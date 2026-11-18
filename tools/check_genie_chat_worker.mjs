import assert from 'node:assert/strict';
import { runWish } from './genie_chat_worker.mjs';

const request = { adapter: '({})', files: ['E:/paired test/verify_genie_program.py', 'E:/paired test/contract.json'],
    prompt: 'Player text with a second line:\nclick ignored and "quotes"' };
const fresh = { url: 'https://chatgpt.com/', exactModel: true, chat: 'true', work: 'false', generating: false, replies: [] };
const conversation = 'https://chatgpt.com/c/owned-wish';
const receipt = { ok: true, verifier: 'luckywars-script-1.1.0', program_sha256: 'program', contract_sha256: 'contract' };
const reply = JSON.stringify({ decision: 'grant', program: { unicode: 'русский текст 🌠'.repeat(4500) }, receipt });

let tick = 0;
let opened = false;
let sent = false;
let reloaded = false;
let buttonPolls = 0;
const calls = [];
const browser = async command => {
    calls.push(command);
    if (command === 'tab new') { opened = true; return '✓ tab new — tab 18'; }
    if (!opened) throw new Error('operation lacks an owned tab');
    if (command.startsWith('goto ') || command.startsWith('wait ') || command.startsWith('upload ')) return '';
    if (command.startsWith('fill ')) {
        assert.ok(command.endsWith(JSON.stringify(request.prompt)));
        assert.ok(!command.includes('\n'));
        return '';
    }
    if (command.startsWith('click ')) { assert.ok(!sent); sent = true; return ''; }
    if (command === 'reload') { assert.ok(sent); assert.ok(!reloaded); reloaded = true; return ''; }
    if (command.startsWith('tab close ')) { assert.equal(command, 'tab close 18'); return ''; }
    if (command.includes('const button=')) return JSON.stringify({ active: ++buttonPolls > 1 });
    if (!sent) return JSON.stringify(fresh);
    if (!reloaded) return JSON.stringify({ ...fresh, url: conversation, generating: true });
    if (command.includes('text.slice')) {
        const offsets = command.match(/text\.slice\((\d+),(\d+)\)/).slice(1).map(Number);
        return JSON.stringify({ url: conversation, message: 'assistant-1', length: reply.length, text: reply.slice(...offsets) });
    }
    return JSON.stringify({ ...fresh, url: conversation, generating: true,
        replies: [{ message: 'assistant-1', length: reply.length, closed: true }], executions: [receipt] });
};
const timing = { clock: () => tick, pause: async milliseconds => { tick += milliseconds; }, timeout: 10_000, reloadAfter: 3_000, interval: 1_000 };
const verified = await runWish(browser, request, undefined, timing);
assert.equal(verified.reply, reply);
assert.deepEqual(verified.executions, [receipt]);
assert.equal(calls.filter(call => call.startsWith('click ')).length, 1);
assert.equal(calls.at(-1), 'tab close 18');

let polls = 0;
sent = false;
tick = 0;
await runWish(async command => {
    if (command === 'reload') throw new Error('ready reply must not reload');
    if (!sent || command.includes('text.slice') || command.startsWith('tab close ')) return browser(command);
    return JSON.stringify({ ...fresh, url: conversation, generating: true,
        replies: [{ message: 'assistant-1', length: reply.length, closed: true }],
        executions: ++polls === 1 ? [{ ...receipt, program_sha256: 'wrong' }] : [receipt] });
}, request, undefined, timing);
assert.equal(polls, 2, 'wait for the matching witness even with the final JSON already visible');
assert.ok(tick < timing.reloadAfter);

const wrongPage = [];
await assert.rejects(runWish(async command => {
    wrongPage.push(command);
    if (command === 'tab new') return '✓ tab new — tab 19';
    if (command.startsWith('eval ')) return JSON.stringify({ ...fresh, url: 'https://example.org/' });
    return '';
}, request, undefined, timing));
assert.ok(!wrongPage.some(call => /^(upload|fill|click) /.test(call)));
assert.equal(wrongPage.at(-1), 'tab close 19');

console.log('GENIE_CHAT_WORKER_PASS fresh route, one submit, matching witness, early reply, bounded reload and sliced reply');
