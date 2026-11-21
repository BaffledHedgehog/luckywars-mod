import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { gatewayHandler } from './genie_chat_gateway.mjs';

const token = 'test-token-only';
const ids = ['00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003'];
let now = 0;
const started = [], canceled = [], releases = [];
const result = { reply: '{"decision":"reject"}', executions: [] };
const queue = gatewayHandler({ token, maximum: 2, clock: () => now, lease: 100,
    probe: async () => ({ ready: true }),
    run: async (input, signal) => {
        started.push(input.prompt);
        await new Promise((resolve, reject) => {
            releases.push(resolve);
            signal.addEventListener('abort', () => { canceled.push(input.prompt); reject(signal.reason); });
        });
        return result;
    }
});
const server = createServer(queue.handler);
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const base = 'http://127.0.0.1:' + server.address().port;
const input = prompt => JSON.stringify({ prompt, knowledge: '{}', contract: '{}', verifier: 'eA==' });
const request = (path, method = 'GET', body, auth = token) => fetch(base + '/v1/' + path,
    { method, body, headers: { Authorization: 'Bearer ' + auth } });
try {
    assert.equal((await request('probe', 'GET', undefined, 'wrong')).status, 401);
    assert.deepEqual(await (await request('probe')).json(), { ready: true });
    assert.equal((await request('jobs/../token')).status, 404);
    assert.equal((await request('jobs/' + ids[0], 'PUT', '{}')).status, 400);
    assert.equal((await request('jobs/' + ids[0], 'PUT', input('first'))).status, 202);
    assert.equal((await request('jobs/' + ids[0], 'PUT', input('duplicate'))).status, 200);
    assert.equal((await request('jobs/' + ids[1], 'PUT', input('second'))).status, 202);
    assert.equal((await request('jobs/' + ids[2], 'PUT', input('overflow'))).status, 429);
    assert.equal((await request('probe')).status, 409);
    assert.deepEqual(started, ['first']);
    releases[0]();
    await new Promise(resolve => setTimeout(resolve, 10));
    assert.deepEqual(started, ['first', 'second']);
    assert.deepEqual((await (await request('jobs/' + ids[0])).json()).result, result);
    await request('jobs/' + ids[1], 'DELETE');
    await new Promise(resolve => setTimeout(resolve, 10));
    assert.deepEqual(canceled, ['second']);
    assert.equal((await request('jobs/' + ids[1])).status, 404);
    now = 90;
    await request('jobs/' + ids[0]); // Touch extends the lease, including completed results.
    now = 150;
    queue.sweep();
    assert.equal((await request('jobs/' + ids[0])).status, 200);
    now = 300;
    queue.sweep();
    assert.equal((await request('jobs/' + ids[0])).status, 404);
    assert.equal((await request('jobs/' + ids[2], 'PUT', input('orphan'))).status, 202);
    now = 500;
    queue.sweep();
    await new Promise(resolve => setTimeout(resolve, 10));
    assert.deepEqual(canceled, ['second', 'orphan']);
    console.log('PASS: authentication, independent IDs, serialized browser, bounded queue, idempotence, results, cancellation and leases');
} finally {
    queue.close(); server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
}
