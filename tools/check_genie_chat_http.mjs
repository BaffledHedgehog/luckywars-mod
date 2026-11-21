import assert from 'node:assert/strict';
import { conversationState, conversationReader } from './genie_chat_http.mjs';
const receipt = { ok: true, verifier: 'native', program_sha256: 'program', contract_sha256: 'contract' };
const reply = JSON.stringify({ decision: 'grant', program: {}, receipt });
const fixture = {
    current_node: 'final', mapping: {
        root: { parent: null, message: null },
        user: { parent: 'root', message: { author: { role: 'user' }, content: { parts: ['owned\nwish'] } } },
        python: { parent: 'user', message: { author: { role: 'tool', name: 'python' }, status: 'finished_successfully',
            content: { content_type: 'execution_output', text: 'returncode 0\n' + JSON.stringify(receipt) + '\nUnrelated runtime warmup warning' } } },
        final: { parent: 'python', message: { author: { role: 'assistant' }, channel: 'final', status: 'finished_successfully',
            metadata: { model_slug: 'gpt-5-6-thinking' }, content: { content_type: 'text', parts: [reply] } } }
    }
};
assert.deepEqual(conversationState(fixture, 'owned\nwish'), { generating: false, observation: { reply, executions: [receipt] } });
const labeled = structuredClone(fixture);
labeled.mapping.python.message.content.text = 'STDOUT: ' + JSON.stringify(receipt) + '\nSTDERR: unrelated startup diagnostic';
assert.deepEqual(conversationState(labeled, 'owned\nwish').observation.executions, [receipt]);
assert.throws(() => conversationState(fixture, 'other wish'), { status: 'UNAVAILABLE' });
let changed = structuredClone(fixture);
changed.mapping.python.message.author.role = 'assistant';
assert.equal(conversationState(changed, 'owned\nwish').observation.executions.length, 0, 'assistant prose cannot supply a tool witness');
changed = structuredClone(fixture); changed.mapping.python.message.author.name = 'api_tool';
assert.equal(conversationState(changed, 'owned\nwish').observation.executions.length, 0, 'file contents cannot impersonate Python output');
changed = structuredClone(fixture); changed.mapping.final.message.metadata.model_slug = 'gpt-6-thinking';
assert.throws(() => conversationState(changed, 'owned\nwish'), { status: 'MODEL_UNAVAILABLE' });
changed = structuredClone(fixture); changed.mapping.final.message.status = 'in_progress';
assert.equal(conversationState(changed, 'owned\nwish').generating, true);
changed = structuredClone(fixture); changed.mapping.final.parent = 'user';
assert.equal(conversationState(changed, 'owned\nwish').observation.executions.length, 0, 'receipts from another branch are excluded');
changed.mapping.root.parent = 'final';
assert.throws(() => conversationState(changed, 'owned\nwish'), { status: 'UNAVAILABLE' });
let disposed = 0, calls = 0;
const context = { request: { get: async url => {
    calls++;
    return { status: () => 200, ok: () => true, body: async () => Buffer.from(JSON.stringify(url.endsWith('/session')
        ? { accessToken: 'test-only' } : fixture)), dispose: async () => { disposed++; } };
} } };
let path = 'https://chatgpt.com/';
const reader = conversationReader(context, { url: () => path }, 'owned\nwish');
assert.equal((await reader()).generating, true); assert.equal(calls, 0);
path = 'https://chatgpt.com/c/owned';
assert.equal((await reader()).observation.reply, reply);
assert.equal(disposed, 2);
assert.equal((await reader()).observation.reply, reply); assert.equal(calls, 3, 'reuse the session token without repeated account fetches');
path = 'https://chatgpt.com/c/other';
await assert.rejects(reader(), { status: 'UNAVAILABLE' });
console.log('PASS: own turn binding, active branch, exact model, genuine Python output, no prose/file witnesses, pending state, token reuse and body disposal');
