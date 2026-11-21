// Private hosted-browser queue. Only this dedicated service profile's landing page may be adopted after login.
import { createServer } from 'node:https';
import { timingSafeEqual } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { runHostedWish, probeHostedChat } from './genie_hosted_chat_worker.mjs';
import { conversationReader } from './genie_chat_http.mjs';

const idPattern = /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/;
const issues = new Set(['AUTH_REQUIRED', 'ACCESS_CHALLENGE', 'RATE_LIMITED', 'MODEL_UNAVAILABLE', 'CANCELLED']);
const limit = 8 * 1024 * 1024;

export function gatewayHandler({ token, run, probe, clock = Date.now, lease = 60_000, maximum = 4, report = () => {} }) {
    const jobs = new Map();
    let active;
    let probing = false;
    const authorized = request => {
        const got = Buffer.from(request.headers.authorization ?? '');
        const expected = Buffer.from('Bearer ' + token);
        return got.length === expected.length && timingSafeEqual(got, expected);
    };
    const reply = (response, code, value) => {
        response.writeHead(code, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
        response.end(JSON.stringify(value));
    };
    const forget = job => {
        if (job.state === 'queued' || job.state === 'running') job.abort.abort();
        jobs.delete(job.id);
    };
    const sweep = () => {
        for (const job of jobs.values()) if (clock() - job.touched > lease) forget(job);
    };
    const pump = () => {
        sweep();
        if (active || probing) return;
        const job = [...jobs.values()].find(j => j.state === 'queued');
        if (!job) return;
        active = job;
        job.state = 'running';
        Promise.resolve().then(() => {
            job.abort.signal.throwIfAborted();
            return run(job.input, job.abort.signal);
        }).then(result => { job.result = result; job.state = 'complete'; }, error => {
            job.issue = job.abort.signal.aborted ? 'CANCELLED' : issues.has(error?.status) ? error.status : 'UNAVAILABLE';
            report({ issue: job.issue, step: /^Chat [A-Za-z0-9 .-]{1,80} failed$/.test(error?.message ?? '') ? error.message : undefined });
            job.state = 'failed';
        }).finally(() => {
            job.input = undefined;
            active = undefined;
            pump();
        });
    };
    const handler = async (request, response) => {
        try {
            if (!authorized(request)) return reply(response, 401, { issue: 'UNAUTHORIZED' });
            sweep();
            if (request.method === 'GET' && request.url === '/v1/probe') {
                if (active || probing) return reply(response, 409, { issue: 'BUSY' });
                probing = true;
                const abort = new AbortController();
                response.once('close', () => abort.abort());
                try { return reply(response, 200, await probe(abort.signal)); }
                catch (error) { return reply(response, 503, { issue: issues.has(error?.status) ? error.status : 'UNAVAILABLE' }); }
                finally { probing = false; pump(); }
            }
            const match = /^\/v1\/jobs\/([^/]+)$/.exec(request.url);
            if (!match || !idPattern.test(match[1])) return reply(response, 404, { issue: 'NOT_FOUND' });
            const id = match[1];
            if (request.method === 'PUT') {
                if (jobs.has(id)) { request.resume(); return reply(response, 200, { state: jobs.get(id).state }); }
                if (jobs.size >= maximum) { request.resume(); return reply(response, 429, { issue: 'BUSY' }); }
                const chunks = [];
                let bytes = 0;
                for await (const chunk of request) {
                    bytes += chunk.length;
                    if (bytes > limit) return reply(response, 413, { issue: 'TOO_LARGE' });
                    chunks.push(chunk);
                }
                const input = JSON.parse(Buffer.concat(chunks).toString('utf8'));
                for (const field of ['prompt', 'knowledge', 'contract', 'verifier']) {
                    if (typeof input[field] !== 'string' || !input[field].length) throw new Error('Invalid input');
                }
                if (input.verifier.length % 4 || !/^[A-Za-z0-9+/]*={0,2}$/.test(input.verifier)) throw new Error('Invalid verifier');
                JSON.parse(input.knowledge);
                JSON.parse(input.contract);
                // Recheck after the asynchronous body read: concurrent PUTs are still idempotent.
                if (jobs.has(id)) return reply(response, 200, { state: jobs.get(id).state });
                if (jobs.size >= maximum) return reply(response, 429, { issue: 'BUSY' });
                jobs.set(id, { id, input, touched: clock(), state: 'queued', abort: new AbortController() });
                pump();
                return reply(response, 202, { state: 'queued' });
            }
            const job = jobs.get(id);
            if (request.method === 'DELETE') {
                if (job) forget(job);
                return reply(response, 200, { removed: true });
            }
            if (request.method !== 'GET' || !job) return reply(response, 404, { issue: 'NOT_FOUND' });
            job.touched = clock();
            return reply(response, 200, { state: job.state, result: job.result, issue: job.issue });
        } catch { return reply(response, 400, { issue: 'INVALID_REQUEST' }); }
    };
    return { handler, sweep, close: () => { for (const job of jobs.values()) forget(job); } };
}

async function main() {
    const base = resolve(process.env.GENIE_GATEWAY_DATA ?? '/data');
    const token = (await readFile(join(base, 'token'), 'utf8')).trim();
    if (!/^[A-Za-z0-9_-]{43,128}$/.test(token)) throw new Error('Gateway token missing');
    const { chromium } = await import('playwright-core');
    let browser;
    const context = async () => {
        if (!browser?.isConnected()) browser = await chromium.connectOverCDP('http://127.0.0.1:19222');
        return browser.contexts()[0];
    };
    const adapter = await readFile(new URL('./genie_chat_page.js', import.meta.url), 'utf8');
    let ownedPage, preparation;
    const prepare = () => {
        if (preparation) return preparation;
        preparation = (async () => {
            const actual = await context();
            if (!ownedPage || ownedPage.isClosed()) {
                ownedPage = undefined;
                // This isolated profile belongs solely to the service after manual login.
                // Reuse its authenticated landing page; a restored service draft is cleared
                // below. Do not enter any existing conversation.
                for (const page of actual.pages()) {
                    if (page.url() !== 'https://chatgpt.com/' ||
                            !await page.locator('[aria-label="Select ChatGPT model"]').count()) continue;
                    if (await page.locator('[data-markdown-text-style="assistant-message"]').count()) continue;
                    ownedPage = page;
                    break;
                }
                if (!ownedPage) {
                    ownedPage = await actual.newPage();
                    await ownedPage.bringToFront();
                    await ownedPage.goto('https://chatgpt.com/', { waitUntil: 'domcontentloaded', timeout: 30_000 });
                }
            }
            if (new URL(ownedPage.url()).pathname !== '/') {
                // SPA navigation keeps the already initialized profile; a new chat still
                // has its own conversation ID and no messages from the preceding wish.
                try {
                    await ownedPage.getByRole('button', { name: 'New chat', exact: true }).first()
                        .evaluate(button => button.click(), null, { timeout: 5000 });
                    await ownedPage.waitForURL(url => url.origin === 'https://chatgpt.com' && url.pathname === '/', { timeout: 10_000 });
                } catch {
                    await ownedPage.goto('https://chatgpt.com/', { waitUntil: 'domcontentloaded', timeout: 30_000 });
                }
            }
            await ownedPage.bringToFront();
            // The app restores a failed pre-submit draft in newly opened pages. On
            // this service-owned fresh chat it must be cleared before waiting for
            // the model control, which the site disables while a draft is present.
            const field = ownedPage.getByRole('textbox', { name: 'Ask ChatGPT', exact: true });
            await field.waitFor({ timeout: 120_000 });
            await field.fill('', { timeout: 12_000 });
            const chat = ownedPage.getByRole('button', { name: 'Chat', exact: true });
            if (await chat.getAttribute('aria-pressed') !== 'true') {
                await ownedPage.locator('main button:not(:disabled)').filter({ hasText: /^Chat$/ }).first()
                    .evaluate(button => button.click(), null, { timeout: 120_000 });
            }
            // Cold account initialization can exceed one minute on a small VPS. This
            // wait occurs once while warming, outside the individual UI-action deadline.
            await ownedPage.locator('[aria-label="Select ChatGPT model"]:not(:disabled)').waitFor({ timeout: 120_000 });
            const page = ownedPage;
            return { newPage: async () => new Proxy(page, { get(target, key) {
                if (key === 'goto') return async url => {
                    if (url !== 'https://chatgpt.com/' || target.url() !== url) throw new Error('Unexpected warm-page route');
                    return { status: () => 200 };
                };
                if (key === 'close') return async () => {}; // Keep the owned initialized client between jobs.
                const value = target[key];
                return typeof value === 'function' ? value.bind(target) : value;
            } }) };
        })().finally(() => { preparation = undefined; });
        return preparation;
    };
    const discard = async () => {
        const previous = ownedPage;
        ownedPage = undefined;
        // A failed pre-submit draft otherwise persists into the next page and keeps
        // the site's model picker locked. This page belongs exclusively to the service.
        if (previous && !previous.isClosed()) {
            await previous.getByRole('textbox', { name: 'Ask ChatGPT', exact: true }).fill('', { timeout: 1000 }).catch(() => {});
        }
        await previous?.close().catch(() => {});
    };
    const withPage = async (signal, action, readOnly = false) => {
        const abort = () => {
            if (readOnly) return; // A disconnected status check must not destroy account warmup or human login.
            void (async () => {
                await ownedPage?.locator('main button[data-testid="stop-button"],main button[aria-label="Stop"],main button[aria-label="Stop generating"]')
                    .first().click({ timeout: 1000 }).catch(() => {});
                await discard();
            })();
        };
        signal?.addEventListener('abort', abort, { once: true });
        try {
            signal?.throwIfAborted();
            const ready = await prepare();
            signal?.throwIfAborted();
            return await action(ready);
        } catch (error) { if (!readOnly) await discard(); throw error; }
        finally { signal?.removeEventListener('abort', abort); }
    };
    // Warm once after startup; it sends no text and never enters another conversation.
    void prepare().catch(() => {});
    const queue = gatewayHandler({ token,
        report: value => process.stderr.write('Genie job failure ' + JSON.stringify(value) + '\n'),
        probe: async signal => withPage(signal, ready => probeHostedChat(ready, adapter, signal), true),
        run: async (input, signal) => {
            let lastStep;
            const onStep = stage => {
                if (stage !== lastStep && /^[A-Za-z0-9 .-]{1,80}$/.test(stage ?? '')) {
                    process.stdout.write('Genie step ' + stage + '\n');
                    lastStep = stage;
                }
            };
            // Buffer uploads work even though Chromium and Playwright live in different containers.
            // No wish content or attachments are persisted on the gateway's disk.
            const files = ['verify_genie_program.py', 'contract.json', 'game_knowledge.json'];
            const buffers = [Buffer.from(input.verifier, 'base64'), Buffer.from(input.contract), Buffer.from(input.knowledge)];
            const filePayloads = files.map((name, i) => ({ name, mimeType: name.endsWith('.py') ? 'text/x-python' : 'application/json', buffer: buffers[i] }));
            signal.throwIfAborted();
            onStep('prepare');
            return withPage(signal, async ready => {
                const observeConversation = conversationReader(await context(), ownedPage, input.prompt);
                try {
                    return await runHostedWish(ready, { adapter, files, filePayloads, onStep, observeConversation, prompt: input.prompt }, signal);
                } finally { observeConversation.close(); }
            });
        }
    });
    const server = createServer({ key: await readFile(join(base, 'tls.key')),
        cert: await readFile(join(base, 'tls.crt')), minVersion: 'TLSv1.2' }, queue.handler);
    server.requestTimeout = 30_000;
    server.headersTimeout = 10_000;
    const reaper = setInterval(queue.sweep, 15_000);
    reaper.unref();
    for (const signal of ['SIGTERM', 'SIGINT']) process.once(signal, () => {
        queue.close(); server.close(); clearInterval(reaper);
        setTimeout(() => process.exit(0), 3500).unref();
    });
    server.listen(18443, '0.0.0.0', () => process.stdout.write('Genie private browser gateway ready\n'));
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    main().catch(() => { process.stderr.write('Genie gateway failed to start\n'); process.exitCode = 1; });
}
