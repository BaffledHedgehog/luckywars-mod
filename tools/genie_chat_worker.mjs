import { readFile } from 'node:fs/promises';
import { resolve, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { setTimeout as sleep } from 'node:timers/promises';

const origin = 'https://chatgpt.com';
const modelButton = '[aria-label="Select ChatGPT model"]';
const family = '[data-model-selection-label="gpt-5-6-thinking"]';
const slider = '[data-reasoning-slider]';
const chat = 'getByRole("button",{name:"Chat",exact:true})';
const composer = 'getByRole("textbox",{name:"Ask ChatGPT",exact:true})';
const attach = 'getByLabel("Attach files",{exact:true})';
const send = 'main button[aria-label="Send"]';
class ChatStepError extends Error {}

function route(value) {
    const url = new URL(value);
    if (url.origin !== origin || !/^\/(?:c\/[A-Za-z0-9:%_-]+)?\/?$/.test(url.pathname)) {
        throw new Error('unexpected Chat route');
    }
    return url.pathname.replace(/\/$/, '') || '/';
}

function evaluation(output) {
    const json = output.split(/\r?\n/).find(line => line.startsWith('{'));
    if (!json) throw new Error('missing Chat observation');
    return JSON.parse(json);
}

function readyReply(text, executions) {
    let reply;
    try { reply = JSON.parse(text); } catch { return false; }
    if (reply?.decision === 'reject') return Object.keys(reply).length === 1;
    if (reply?.decision !== 'grant' || !reply.receipt || !reply.program) return false;
    const fields = Object.keys(reply.receipt);
    return executions.some(receipt => fields.length === Object.keys(receipt).length
        && fields.every(key => Object.hasOwn(receipt, key) && receipt[key] === reply.receipt[key]));
}

export async function runWish(browser, request, signal, timing = {}) {
    const clock = timing.clock ?? Date.now;
    const pause = timing.pause ?? (milliseconds => sleep(milliseconds, undefined, { signal }));
    const idleTimeout = timing.timeout ?? 210_000;
    const submissionDeadline = clock() + (timing.submitTimeout ?? idleTimeout);
    const pageSource = request.adapter.replace(/\/\*[\s\S]*?\*\//g, '').trim().replace(/\r?\n/g, ' ');
    const projection = `p=>({...p,replies:p.replies?.slice(-1).map(r=>({message:r.message,length:r.text.length,closed:r.text.trimEnd().endsWith('}')})),executions:p.executions?.filter(r=>r?.ok===true&&typeof r.verifier==='string'&&typeof r.program_sha256==='string'&&typeof r.contract_sha256==='string'&&JSON.stringify(r).length<700).slice(-32)})`;
    const step = async (command, stage = command.split(' ', 1)[0]) => {
        signal?.throwIfAborted();
        try { return await browser(command, signal); }
        catch { throw new ChatStepError(`Chat ${stage} failed`); }
    };
    const evaluate = async expression => evaluation(await step(`eval ${expression} --max all`));
    const read = () => evaluate(`(${projection})(${pageSource})`);
    const root = page => {
        if (route(page.url) !== '/' || page.replies?.length) throw new Error('expected fresh Chat');
        return page;
    };
    let ownTab;
    let sent = false;
    let completed = false;
    try {
        const opened = await step('tab new');
        ownTab = /\btab (\d+)\b/.exec(opened)?.[1];
        if (!ownTab) throw new Error('could not identify owned Chat tab');
        await step(`goto ${origin}/?model=gpt-5-6-thinking`);
        await step(`wait ${modelButton}:not(:disabled) --timeout 16000`);

        let page = root(await read());
        if (page.chat !== 'true') {
            await step(`click ${chat}`, 'select Chat mode');
            page = root(await read());
        }
        if (!page.exactModel) {
            await step(`click ${modelButton}`, 'open model menu');
            const choices = await evaluate(`(()=>{const node=document.querySelector('${family}');return {selected:node?.getAttribute('aria-checked')==='true'||node?.getAttribute('data-state')==='checked'}})()`);
            if (!choices.selected) await step(`click ${family}`, 'select GPT-5.6 family');
            page = root(await read());
            if (!/\b5\.6\b/.test(page.selected)) throw new Error('required model is unavailable');
            // Read the selected effort afresh before each adjustment. Never submit in another mode.
            for (let attempts = 0; !page.exactModel && attempts < 3; attempts++) {
                const controls = await evaluate(`(()=>({open:document.querySelector('${modelButton}')?.getAttribute('data-state')==='open'}))()`);
                if (!controls.open) await step(`click ${modelButton}`, 'open reasoning menu');
                await evaluate(`(()=>{const control=document.querySelector('${slider}');control?.focus();return {focused:document.activeElement===control}})()`);
                await step(`press ${page.selected.endsWith('Low') ? 'ArrowRight' : 'ArrowLeft'}`);
                page = root(await read());
            }
            const controls = await evaluate(`(()=>({open:document.querySelector('${modelButton}')?.getAttribute('data-state')==='open'}))()`);
            if (controls.open) await step(`click ${modelButton}`, 'close model menu');
        }
        const ready = root(await read());
        if (!ready.exactModel || ready.chat !== 'true' || ready.work === 'true') throw new Error('GPT-5.6 Medium Chat required');

        // Only fixed paths built by the caller are uploaded. Wish text is a quoted fill argument.
        await step(`upload ${attach} ${request.files.map(path => JSON.stringify(path.replaceAll('\\', '/'))).join(' ')}`);
        await step(`fill ${composer} ${JSON.stringify(request.prompt)}`);
        const submitPage = root(await read());
        if (!submitPage.exactModel || submitPage.chat !== 'true' || submitPage.work === 'true') throw new Error('Chat mode changed');
        for (;;) {
            const button = await evaluate(`(()=>{const button=document.querySelector('${send}');return {active:!!button&&!button.disabled}})()`);
            if (button.active) break;
            if (clock() >= submissionDeadline) throw new ChatStepError('Chat send button timed out');
            await pause(500);
        }
        sent = true; // A timeout after the click cannot authorize a second submission.
        await step(`click ${JSON.stringify(send)}`, 'submit');

        let conversation;
        let reloaded = false;
        let candidate;
        let lastProgress = clock();
        let observedMessage;
        let observedLength;
        for (;;) {
            await pause(timing.interval ?? 750);
            page = await read();
            const now = clock();
            const current = route(page.url);
            if (conversation && current !== conversation) throw new Error('Chat conversation changed');
            const message = page.replies?.[0];
            if (page.generating || !conversation && current !== '/' ||
                    message?.message !== observedMessage || message?.length !== observedLength) {
                lastProgress = now;
            }
            if (current !== '/') conversation = current;
            observedMessage = message?.message;
            observedLength = message?.length;
            if (conversation && message?.closed) {
                if (message.length > 4_194_304) throw new Error('Chat reply exceeds input bounds');
                if (candidate?.message !== message.message || candidate?.reply.length !== message.length) {
                    // A streaming answer can grow while its slices are being read.
                    let reply = '';
                    while (reply.length < message.length) {
                        const chunk = await evaluate(`(()=>{const p=${pageSource};const r=p.replies?.at(-1);return {url:p.url,length:r?.text.length,message:r?.message,text:r?.text.slice(${reply.length},${reply.length + 12000})}})()`);
                        if (route(chunk.url) !== conversation) throw new Error('Chat conversation changed');
                        if (chunk.message !== message.message || chunk.length !== message.length || !chunk.text) break;
                        reply += chunk.text;
                    }
                    candidate = reply.length === message.length ? {message: message.message, reply} : undefined;
                }
                if (candidate && readyReply(candidate.reply, page.executions ?? [])) {
                    completed = true;
                    return { reply: candidate.reply, executions: page.executions ?? [] };
                }
            }
            if (now - lastProgress >= idleTimeout) throw new ChatStepError('Chat stopped before a verified reply');
            if (conversation && !page.generating && !reloaded && now - lastProgress >= (timing.reloadAfter ?? 50_000)) {
                // A background tab can fail to repaint a completed response. Never reload an active turn.
                await step('reload');
                reloaded = true;
            }
        }
    } finally {
        if (ownTab) {
            const cleanup = AbortSignal.timeout(3000);
            if (sent && !completed) {
            try { await browser('click getByRole("button",{name:"Stop",exact:true})', cleanup); } catch { /* already stopped */ }
            }
            try { await browser(`tab close ${ownTab}`, cleanup); } catch { /* disconnected extension */ }
        }
    }
}

async function main() {
    const [mcpRoot, stateDir, port, workspace] = process.argv.slice(2);
    if (!/^[0-9]+$/.test(port) || Number(port) < 1 || Number(port) > 65535) throw new Error('invalid Firefox MCP port');
    const sdk = join(mcpRoot, 'node_modules', '@modelcontextprotocol', 'sdk', 'dist', 'esm', 'client');
    const { Client } = await import(pathToFileURL(join(sdk, 'index.js')));
    const { StdioClientTransport } = await import(pathToFileURL(join(sdk, 'stdio.js')));
    const abort = new AbortController();
    process.stdin.on('data', () => abort.abort());
    process.stdin.resume();
    const client = new Client({ name: 'LuckyWars Genie', version: '1.0.0' }, { capabilities: {} });
    const transport = new StdioClientTransport({ command: process.execPath,
        args: [join(mcpRoot, 'dist', 'server', 'index.js'), '--port', port],
        env: { ...process.env, FIREFOX_MCP_HOME: stateDir }, stderr: 'pipe' });
    try {
        await client.connect(transport, { signal: abort.signal, timeout: 12000 });
        const result = await runWish(async (commands, signal) => {
            const reply = await client.callTool({ name: 'browser', arguments: { commands } }, undefined, { signal, timeout: 35000 });
            if (reply.isError) throw new Error('Firefox rejected Chat operation');
            return reply.content.filter(x => x.type === 'text').map(x => x.text).join('\n');
        }, {
            prompt: await readFile(join(workspace, 'prompt.txt'), 'utf8'),
            adapter: await readFile(join(workspace, 'genie_chat_page.js'), 'utf8'),
            files: ['verify_genie_program.py', 'contract.json', 'game_knowledge.json'].map(name => resolve(workspace, name))
        }, abort.signal);
        process.stdout.write(JSON.stringify(result));
    } finally {
        await client.close();
        process.stdin.pause();
    }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    main().catch(error => {
        process.stderr.write(error instanceof ChatStepError ? `${error.message}\n` : 'Chat worker failure\n');
        process.exitCode = 1;
    });
}
