import { open, readFile, realpath, stat } from 'node:fs/promises';
import { isAbsolute, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { runWish } from './genie_chat_worker.mjs';

const origin = 'https://chatgpt.com';
const entry = origin + '/?model=gpt-5-6-thinking';
const model = '[aria-label="Select ChatGPT model"]';
const family = '[data-model-selection-label="gpt-5-6-thinking"]';
const send = 'main button[aria-label="Send"]';
const attach = 'getByLabel("Attach files",{exact:true})';
const composer = 'getByRole("textbox",{name:"Ask ChatGPT",exact:true})';
const chat = 'getByRole("button",{name:"Chat",exact:true})';
const stop = 'getByRole("button",{name:"Stop",exact:true})';
const requestFiles = ['verify_genie_program.py', 'contract.json', 'game_knowledge.json'];
const actionTimeout = { timeout: 12_000 };

class HostedChatError extends Error {
    constructor(status) {
        super(status);
        this.status = status;
    }
}

function chatUrl(page) {
    const url = new URL(page.url());
    if (url.origin === 'https://auth.openai.com' ||
        (url.origin === origin && /^\/(?:auth|login)(?:\/|$)/i.test(url.pathname))) {
        throw new HostedChatError('AUTH_REQUIRED');
    }
    if (url.origin !== origin || !/^\/(?:c\/[A-Za-z0-9:%_-]+)?\/?$/.test(url.pathname)) {
        throw new HostedChatError('UNEXPECTED_PAGE');
    }
}

async function checkInterruption(page, response) {
    chatUrl(page);
    const status = response?.status();
    if (status === 401) throw new HostedChatError('AUTH_REQUIRED');
    if (status === 403) throw new HostedChatError('ACCESS_CHALLENGE');
    if (status === 429) throw new HostedChatError('RATE_LIMITED');
    if (status >= 400) throw new HostedChatError('UNAVAILABLE');
    const challenge = await page.evaluate(() =>
        /just a moment|attention required/i.test(document.title) ||
        !!document.querySelector('iframe[src*="challenges.cloudflare.com"], #challenge-running'));
    if (challenge) throw new HostedChatError('ACCESS_CHALLENGE');
}

async function loginVisible(page) {
    const name = { name: 'Log in', exact: true };
    return await page.getByRole('button', name).isVisible() || await page.getByRole('link', name).isVisible();
}

async function interruptible(signal, action) {
    const boundary = signal
        ? AbortSignal.any([signal, AbortSignal.timeout(30_000)])
        : AbortSignal.timeout(30_000);
    boundary.throwIfAborted();
    let abort;
    const interrupted = new Promise((_, reject) => {
        abort = () => reject(boundary.reason);
        boundary.addEventListener('abort', abort, { once: true });
    });
    try {
        return await Promise.race([action(boundary), interrupted]);
    } finally {
        boundary.removeEventListener('abort', abort);
    }
}

async function upload(page, files) {
    const target = page.getByLabel('Attach files', { exact: true });
    const isFileInput = await target.evaluate(node => node.localName === 'input' && node.type === 'file');
    if (isFileInput) {
        await target.setInputFiles(files, actionTimeout);
    } else {
        const [chooser] = await Promise.all([
            page.waitForEvent('filechooser', actionTimeout), target.click(actionTimeout)
        ]);
        await chooser.setFiles(files, actionTimeout);
    }
}

/** Own one newly opened page in an already provisioned browser context. */
export async function runHostedWish(context, request, signal, timing) {
    return runHosted(context, request, false, signal, timing);
}

/** Check the same exact authorized Chat composer, stopping before upload or text entry. */
export function probeHostedChat(context, adapter, signal, timing) {
    return runHosted(context, { adapter, files: [], prompt: '' }, true, signal, timing);
}

async function runHosted(context, request, probe, signal, timing) {
    const uploadCommand = `upload ${attach} ${request.files.map(file => JSON.stringify(file.replaceAll('\\', '/'))).join(' ')}`;
    const fillCommand = `fill ${composer} ${JSON.stringify(request.prompt)}`;
    let page;
    let failure;
    let submitted = false;
    const browser = async (command, actionSignal) => {
        try {
            return await interruptible(actionSignal, async boundary => {
                if (command === 'tab new' && !page) {
                    // Keep ownership even if cancellation arrives during the native page creation.
                    const opened = await context.newPage();
                    if (boundary.aborted) {
                        await opened.close();
                        boundary.throwIfAborted();
                    }
                    page = opened;
                    return 'tab 1';
                }
                if (!page) throw new HostedChatError('UNAVAILABLE');
                if (command === 'tab close 1') {
                    await page.close();
                    page = undefined;
                    return '';
                }
                if (command === `goto ${entry}`) {
                    const response = await page.goto(entry, { waitUntil: 'domcontentloaded', timeout: 20_000 });
                    await checkInterruption(page, response);
                    return '';
                }
                if (command === `wait ${model}:not(:disabled) --timeout 16000`) {
                    try {
                        await page.locator(`${model}:not(:disabled)`).waitFor({ timeout: 16_000 });
                    } catch (error) {
                        await checkInterruption(page);
                        if (await loginVisible(page)) throw new HostedChatError('AUTH_REQUIRED');
                        throw error;
                    }
                    return '';
                }
                chatUrl(page);
                if (command === uploadCommand) {
                    await checkInterruption(page);
                    if (await loginVisible(page)) throw new HostedChatError('AUTH_REQUIRED');
                    if (probe) {
                        const field = page.getByRole('textbox', { name: 'Ask ChatGPT', exact: true });
                        if (!(await field.isVisible()) || !(await field.isEditable(actionTimeout))) {
                            throw new HostedChatError('UNAVAILABLE');
                        }
                        throw new HostedChatError('PROBE_READY');
                    }
                    await upload(page, request.files);
                } else if (command === fillCommand) await page.getByRole('textbox', { name: 'Ask ChatGPT', exact: true })
                    .fill(request.prompt, actionTimeout);
                else if (command === `click ${chat}`) await page.getByRole('button', { name: 'Chat', exact: true }).click(actionTimeout);
                else if (command === `click ${stop}`) await page.getByRole('button', { name: 'Stop', exact: true }).click(actionTimeout);
                else if (command === `click ${JSON.stringify(send)}`) {
                    submitted = true;
                    await page.locator(send).click(actionTimeout);
                } else if (command === `click ${model}`) await page.locator(model).click(actionTimeout);
                else if (command === `click ${family}`) await page.locator(family).click(actionTimeout);
                else if (command === 'press ArrowLeft' || command === 'press ArrowRight') await page.keyboard.press(command.slice(6));
                else if (command === 'reload') {
                    const response = await page.reload({ waitUntil: 'domcontentloaded', timeout: 20_000 });
                    await checkInterruption(page, response);
                } else if (command.startsWith('eval ') && command.endsWith(' --max all')) {
                    // Expressions are built by the existing trusted Chat worker and its bundled DOM adapter.
                    return JSON.stringify(await page.evaluate(command.slice(5, -10)));
                } else throw new HostedChatError('UNAVAILABLE');
                return '';
            });
        } catch (error) {
            if (error instanceof HostedChatError && !failure) {
                // A rate limit observed while reading an already sent turn cannot authorize resubmission.
                failure = submitted && error.status === 'RATE_LIMITED' ? new HostedChatError('UNAVAILABLE') : error;
            }
            throw error;
        }
    };
    try {
        return await runWish(browser, request, signal, timing);
    } catch (error) {
        if (signal?.aborted) throw new HostedChatError('CANCELLED');
        if (probe && failure?.status === 'PROBE_READY') return { ready: true };
        throw failure ?? error;
    } finally {
        if (page) await page.close().catch(() => {});
    }
}

const exportedSameSite = new Map([
    ['strict', 'Strict'], ['lax', 'Lax'], ['none', 'None'], ['no_restriction', 'None'], ['unspecified', 'Lax'],
]);

/** Accept cookie-only Playwright state or a browser extension's cookie JSON array. */
export function chatCookies(state) {
    const exported = Array.isArray(state);
    const cookies = exported ? state : state?.cookies;
    if (!Array.isArray(cookies) || !cookies.length || cookies.length > 256 ||
        !exported && state.origins !== undefined && (!Array.isArray(state.origins) || state.origins.length !== 0)) {
        throw new HostedChatError('INVALID_BROWSER_STATE');
    }
    return cookies.map(cookie => {
        if (!cookie || !['chatgpt.com', '.chatgpt.com'].includes(cookie.domain) ||
            typeof cookie.name !== 'string' || !cookie.name || cookie.name.length > 256 ||
            typeof cookie.value !== 'string' || cookie.value.length > 16384 ||
            typeof cookie.path !== 'string' || !cookie.path.startsWith('/') ||
            typeof cookie.httpOnly !== 'boolean' || typeof cookie.secure !== 'boolean') {
            throw new HostedChatError('INVALID_BROWSER_STATE');
        }
        let { expires, sameSite } = cookie;
        if (exported) {
            if (cookie.session !== undefined && typeof cookie.session !== 'boolean' ||
                cookie.expirationDate !== undefined && !Number.isFinite(cookie.expirationDate) ||
                sameSite != null && typeof sameSite !== 'string') {
                throw new HostedChatError('INVALID_BROWSER_STATE');
            }
            expires = cookie.session === true ? -1 : cookie.expirationDate ?? -1;
            sameSite = exportedSameSite.get((sameSite ?? 'unspecified').toLowerCase());
        }
        if (!Number.isFinite(expires) || !['Strict', 'Lax', 'None'].includes(sameSite)) {
            throw new HostedChatError('INVALID_BROWSER_STATE');
        }
        const { name, value, domain, path, httpOnly, secure } = cookie;
        return { name, value, domain, path, expires, httpOnly, secure, sameSite };
    });
}

async function importBrowserState(context) {
    try {
        const path = process.env.SKYWARS2_CHATGPT_BROWSER_STATE;
        if (!path || !isAbsolute(path)) throw new HostedChatError('INVALID_BROWSER_STATE');
        const file = await open(path);
        try {
            const limit = 262_144;
            const info = await file.stat();
            if (!info.isFile() || info.size > limit) throw new HostedChatError('INVALID_BROWSER_STATE');
            const payload = await file.readFile('utf8');
            if (Buffer.byteLength(payload) > limit) throw new HostedChatError('INVALID_BROWSER_STATE');
            await context.addCookies(chatCookies(JSON.parse(payload)));
        } finally {
            await file.close();
        }
    } catch {
        // Cookie parser and driver errors can refer to the original private input.
        throw new HostedChatError('INVALID_BROWSER_STATE');
    }
}

async function options() {
    const module = process.env.SKYWARS2_PLAYWRIGHT_MODULE?.trim();
    const executablePath = process.env.SKYWARS2_BROWSER_EXECUTABLE?.trim() || undefined;
    if (module && !isAbsolute(module) || executablePath && !isAbsolute(executablePath)) {
        throw new HostedChatError('UNAVAILABLE');
    }
    const { chromium } = await import(module ? pathToFileURL(module).href : 'playwright');
    if (!chromium) throw new HostedChatError('UNAVAILABLE');
    return { chromium, executablePath };
}

async function main() {
    if (process.argv.length !== 4) throw new HostedChatError('UNAVAILABLE');
    const action = process.argv[2];
    const probe = action === '--probe' || action === '--import-probe';
    const workspace = probe ? undefined : await realpath(action);
    const profile = await realpath(process.argv[3]).catch(() => { throw new HostedChatError('PROFILE_REQUIRED'); });
    if (workspace === profile || !(await stat(profile)).isDirectory()) throw new HostedChatError('PROFILE_REQUIRED');
    const adapter = await readFile(new URL('./genie_chat_page.js', import.meta.url), 'utf8');
    const request = probe ? undefined : {
        adapter,
        prompt: await readFile(join(workspace, 'prompt.txt'), 'utf8'),
        files: requestFiles.map(name => join(workspace, name))
    };
    const { chromium, executablePath } = await options();
    const abort = new AbortController();
    const cancel = () => abort.abort();
    process.stdin.on('data', cancel);
    process.stdin.resume();
    process.once('SIGTERM', cancel);
    let context;
    try {
        context = await chromium.launchPersistentContext(profile, {
            headless: process.env.SKYWARS2_BROWSER_HEADED !== '1',
            locale: 'en-US', executablePath, timeout: 20_000
        });
        if (action === '--import-probe') await importBrowserState(context);
        const answer = JSON.stringify(probe
            ? await probeHostedChat(context, adapter, abort.signal)
            : await runHostedWish(context, request, abort.signal));
        if (Buffer.byteLength(answer) > 4_194_304) throw new HostedChatError('UNAVAILABLE');
        process.stdout.write(answer);
    } finally {
        process.stdin.off('data', cancel);
        process.stdin.pause();
        process.removeListener('SIGTERM', cancel);
        await context?.close();
    }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    main().catch(error => {
        process.stderr.write(`GENIE_HOSTED_${error instanceof HostedChatError ? error.status : 'UNAVAILABLE'}\n`);
        process.exitCode = 1;
    });
}
