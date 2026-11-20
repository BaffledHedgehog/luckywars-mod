import assert from 'node:assert/strict';
import { chatCookies, probeHostedChat, runHostedWish } from './genie_hosted_chat_worker.mjs';

const origin = 'https://chatgpt.com';
const conversation = origin + '/c/test-wish';
const receipt = { ok: true, verifier: 'test-verifier', program_sha256: 'program', contract_sha256: 'contract' };
const reply = JSON.stringify({ decision: 'grant', program: { large: 'wish'.repeat(7_000) }, receipt });
const request = {
    adapter: '({})',
    prompt: 'Text and "quotes".\nSecond line stays text.',
    files: ['/request with spaces/verify_genie_program.py', '/request with spaces/contract.json',
        '/request with spaces/game_knowledge.json']
};

function scenario(options = {}) {
    let url = 'about:blank';
    let selected = options.controls ? 'GPT-5.6 High' : 'GPT-5.6 Medium';
    let chat = !options.controls;
    let menuOpen = false;
    let sent = false;
    let reloaded = false;
    let now = 0;
    let gaveWrongReceipt = false;
    const events = [];
    const timing = {
        clock: () => now, pause: async ms => { now += ms; }, timeout: 10_000, interval: 1_000, reloadAfter: 3_000
    };
    const upload = {
        evaluate: async () => !options.fileChooser,
        setInputFiles: async files => { events.push(['input files', files]); },
        click: async () => { events.push('native attachment picker'); }
    };
    const textBox = {
        isVisible: async () => !options.hiddenComposer,
        isEditable: async () => !options.readOnlyComposer,
        fill: async text => { events.push(['fill', text]); }
    };
    const page = {
        url: () => url,
        goto: async target => {
            events.push(['navigate', target]);
            url = options.redirect ?? target;
            return { status: () => options.http ?? 200 };
        },
        evaluate: async expression => {
            if (typeof expression === 'function') return options.challenge ?? false;
            if (expression.includes('const button=')) return { active: true };
            if (expression.includes("getAttribute('aria-checked')")) return { selected: false };
            if (expression.includes('document.activeElement')) return { focused: true };
            if (expression.includes('open:document.querySelector')) return { open: menuOpen };
            if (!sent || options.waitForReload && !reloaded) {
                return { url, selected, chat: String(chat), work: String(!chat),
                    exactModel: !options.wrongModel && selected.endsWith('Medium'), replies: [] };
            }
            if (expression.includes('text.slice(')) {
                const [, a, b] = /text\.slice\((\d+),(\d+)\)/.exec(expression);
                return { url, message: 'assistant-1', length: reply.length, text: reply.slice(Number(a), Number(b)) };
            }
            const executions = options.wrongReceiptFirst && !gaveWrongReceipt
                ? [{ ...receipt, program_sha256: 'wrong' }] : [receipt];
            gaveWrongReceipt = true;
            return { url, replies: [{ message: 'assistant-1', length: reply.length, closed: true }], executions };
        },
        locator: selector => ({
            waitFor: async () => { if (options.loggedOut) throw new Error('model is absent'); },
            click: async () => {
                if (selector.includes('Send')) {
                    assert.equal(sent, false, 'the accepted turn is never submitted twice');
                    sent = true;
                    url = conversation;
                    events.push('submit');
                    options.cancelOnSend?.abort();
                } else if (selector.includes('Select ChatGPT model')) {
                    menuOpen = !menuOpen;
                    events.push('model menu');
                } else if (selector.includes('gpt-5-6-thinking')) {
                    events.push('model family');
                } else assert.fail(selector);
            }
        }),
        getByRole: (role, input) => {
            assert.equal(input.exact, true);
            if (role === 'textbox' && input.name === 'Ask ChatGPT') return textBox;
            if (input.name === 'Log in') return { isVisible: async () => !!options.loggedOut };
            if (role === 'button' && input.name === 'Chat') return { click: async () => { chat = true; events.push('chat mode'); } };
            if (role === 'button' && input.name === 'Stop') return { click: async () => { events.push('stop'); } };
            assert.fail(`Unexpected role: ${role}/${input.name}`);
        },
        getByLabel: (label, input) => {
            assert.equal(label, 'Attach files');
            assert.equal(input.exact, true);
            return upload;
        },
        keyboard: { press: async key => {
            assert.equal(key, 'ArrowLeft');
            if (!options.wrongModel) selected = 'GPT-5.6 Medium';
            events.push('medium effort');
        } },
        waitForEvent: async event => {
            assert.equal(event, 'filechooser');
            return { setFiles: async files => { events.push(['chooser files', files]); } };
        },
        reload: async () => {
            assert.equal(sent, true);
            assert.equal(reloaded, false);
            reloaded = true;
            events.push('reload');
            return { status: () => options.reloadHttp ?? 200 };
        },
        close: async () => { events.push('close'); }
    };
    return { events, timing, context: { newPage: async () => { events.push('new page'); return page; } } };
}

const native = scenario({ controls: true, fileChooser: true, wrongReceiptFirst: true });
assert.deepEqual(await runHostedWish(native.context, request, undefined, native.timing), { reply, executions: [receipt] });
assert.ok(native.events.includes('chat mode') && native.events.includes('medium effort'));
assert.ok(native.events.includes('native attachment picker'));
assert.deepEqual(native.events.find(e => e[0] === 'chooser files'), ['chooser files', request.files]);
assert.deepEqual(native.events.find(e => e[0] === 'fill'), ['fill', request.prompt]);
assert.equal(native.events.filter(e => e === 'submit').length, 1);
assert.equal(native.events.at(-1), 'close');

const direct = scenario({ waitForReload: true });
assert.equal((await runHostedWish(direct.context, request, undefined, direct.timing)).reply, reply);
assert.deepEqual(direct.events.find(e => e[0] === 'input files'), ['input files', request.files]);
assert.equal(direct.events.filter(e => e === 'reload').length, 1);
assert.equal(direct.events.filter(e => e === 'submit').length, 1);

const probe = scenario({ controls: true });
assert.deepEqual(await probeHostedChat(probe.context, request.adapter, undefined, probe.timing), { ready: true });
assert.ok(probe.events.includes('medium effort'));
assert.equal(probe.events.at(-1), 'close');
assert.ok(!probe.events.some(e => ['submit', 'fill', 'input files', 'chooser files'].includes(Array.isArray(e) ? e[0] : e)));

for (const [options, status] of [
    [{ redirect: 'https://auth.openai.com/log-in' }, 'AUTH_REQUIRED'],
    [{ loggedOut: true }, 'AUTH_REQUIRED'],
    [{ http: 403 }, 'ACCESS_CHALLENGE'],
    [{ challenge: true }, 'ACCESS_CHALLENGE'],
    [{ http: 429 }, 'RATE_LIMITED'],
    [{ redirect: 'https://chatgpt.com.example/' }, 'UNEXPECTED_PAGE'],
    [{ hiddenComposer: true }, 'UNAVAILABLE']
]) {
    const failed = scenario(options);
    await assert.rejects(probeHostedChat(failed.context, request.adapter, undefined, failed.timing), { status });
    assert.equal(failed.events.at(-1), 'close');
    assert.ok(!failed.events.includes('submit'));
}

const mismatched = scenario({ wrongModel: true });
await assert.rejects(probeHostedChat(mismatched.context, request.adapter, undefined, mismatched.timing));
assert.ok(!mismatched.events.includes('submit'));

const interrupted = new AbortController();
const cancelled = scenario({ cancelOnSend: interrupted });
await assert.rejects(runHostedWish(cancelled.context, request, interrupted.signal, cancelled.timing), { status: 'CANCELLED' });
assert.equal(cancelled.events.filter(e => e === 'submit').length, 1);
assert.equal(cancelled.events.filter(e => e === 'stop').length, 1);
assert.equal(cancelled.events.at(-1), 'close');

const afterSendLimit = scenario({ waitForReload: true, reloadHttp: 429 });
await assert.rejects(runHostedWish(afterSendLimit.context, request, undefined, afterSendLimit.timing), { status: 'UNAVAILABLE' });
assert.equal(afterSendLimit.events.filter(e => e === 'submit').length, 1);
assert.equal(afterSendLimit.events.at(-1), 'close');

const cookie = { domain: '.chatgpt.com', path: '/', expires: -1, name: 'fixture', value: 'synthetic',
    secure: true, httpOnly: true, sameSite: 'Lax' };
assert.deepEqual(chatCookies({ cookies: [cookie], origins: [] }), [cookie]);
assert.throws(() => chatCookies({ cookies: [cookie, { ...cookie, domain: 'chatgpt.com.example' }] }),
    { status: 'INVALID_BROWSER_STATE' });
assert.throws(() => chatCookies({ cookies: [cookie], origins: [{ origin }] }), { status: 'INVALID_BROWSER_STATE' });

console.log('GENIE_HOSTED_CHAT_WORKER_PASS owned tab, exact UI, attachments, verifier witness, no-submit probe, challenges, cancellation and cookie scope');
