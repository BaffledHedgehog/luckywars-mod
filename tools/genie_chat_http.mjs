// Read only the newly submitted owned conversation. Never list account history.
const model = 'gpt-5-6-thinking';
const normalized = text => text.replace(/\r\n/g, '\n').trim();
const plain = message => (message.content?.parts ?? []).filter(p => typeof p === 'string').join('');
const failure = status => Object.assign(new Error(status), { status });

export function conversationState(value, expectedPrompt) {
    if (!value || !value.mapping || typeof value.current_node !== 'string') return { generating: true };
    const path = [], seen = new Set();
    for (let id = value.current_node; id; id = value.mapping[id]?.parent) {
        if (seen.has(id) || seen.size > 2048 || !value.mapping[id]) throw failure('UNAVAILABLE');
        seen.add(id);
        if (value.mapping[id].message) path.unshift(value.mapping[id].message);
    }
    const users = path.filter(m => m.author?.role === 'user');
    if (users.length !== 1 || normalized(plain(users[0])) !== normalized(expectedPrompt)) throw failure('UNAVAILABLE');
    const terminalFailure = path.some(m => ['cancelled', 'failed', 'error'].includes(m.status));
    if (terminalFailure) throw failure('UNAVAILABLE');
    const final = path.findLast(m => m.author?.role === 'assistant' && m.content?.content_type === 'text'
        && (m.channel === 'final' || !m.channel));
    if (!final || final.status !== 'finished_successfully') return { generating: true };
    // The Work model slugs differ. Do not accept a result from a silently substituted model.
    if (final.metadata?.model_slug !== model) throw failure('MODEL_UNAVAILABLE');
    let reply = plain(final).trim();
    const fenced = /^```(?:json)?\s*\n([\s\S]*)\n```$/.exec(reply);
    if (fenced) reply = fenced[1].trim();
    if (Buffer.byteLength(reply) > 4_194_304) throw failure('UNAVAILABLE');
    const executions = [];
    for (const message of path) {
        if (message.author?.role !== 'tool' || !['python', 'python_user_visible'].includes(message.author.name)
                || message.content?.content_type !== 'execution_output') continue;
        const output = message.content.text;
        if (typeof output !== 'string' || output.length > 65536) continue;
        for (const line of output.split(/\r?\n/)) {
            const json = line.trimStart().replace(/^stdout\s*:?\s*/i, '');
            if (!json.startsWith('{')) continue;
            try {
                const receipt = JSON.parse(json);
                if (receipt?.ok === true && typeof receipt.verifier === 'string' && typeof receipt.program_sha256 === 'string'
                        && typeof receipt.contract_sha256 === 'string' && JSON.stringify(receipt).length < 700) executions.push(receipt);
            } catch { /* Tool diagnostics are not receipts. */ }
        }
    }
    return { generating: false, observation: { reply, executions: executions.slice(-32) } };
}

/** Cookie updates remain inside the authenticated browser context. All bodies are disposed promptly. */
export function conversationReader(context, page, expectedPrompt) {
    let conversation, token, refreshed = 0, unauthorized = 0;
    let rejected;
    const responseStatus = response => {
        if (response.request().method() !== 'POST' || !new URL(response.url()).pathname.endsWith('/conversation')) return;
        if (response.status() === 401) rejected = 'AUTH_REQUIRED';
        else if (response.status() === 403) rejected = 'ACCESS_CHALLENGE';
        else if (response.status() === 429) rejected = 'RATE_LIMITED';
        else if (response.status() >= 400) rejected = 'UNAVAILABLE';
    };
    page.on?.('response', responseStatus);
    const json = async (url, headers) => {
        const response = await context.request.get(url, { headers, timeout: 20_000, maxRedirects: 0 });
        try {
            if (response.status() === 401) { token = undefined; if (++unauthorized > 2) throw failure('AUTH_REQUIRED'); return null; }
            if (response.status() === 403) throw failure('ACCESS_CHALLENGE');
            if (response.status() === 404) return null; // The newly submitted conversation may not have persisted yet.
            if (!response.ok()) throw failure('UNAVAILABLE');
            const bytes = await response.body();
            if (bytes.length > 16_777_216) throw failure('UNAVAILABLE');
            return JSON.parse(bytes.toString('utf8'));
        } finally { await response.dispose(); }
    };
    const read = async signal => {
        signal?.throwIfAborted();
        if (rejected) throw failure(rejected);
        const url = new URL(page.url());
        if (url.origin !== 'https://chatgpt.com') throw failure('AUTH_REQUIRED');
        const match = /^\/c\/([A-Za-z0-9_-]+)$/.exec(url.pathname);
        if (!match) return { generating: true };
        if (conversation && conversation !== match[1]) throw failure('UNAVAILABLE');
        conversation = match[1];
        if (!token || Date.now() - refreshed > 60_000) {
            const session = await json('https://chatgpt.com/api/auth/session');
            if (!session) return { generating: true };
            if (typeof session.accessToken !== 'string' || !session.accessToken) throw failure('AUTH_REQUIRED');
            token = session.accessToken; refreshed = Date.now();
        }
        const value = await json('https://chatgpt.com/backend-api/conversation/' + conversation,
            { Authorization: 'Bearer ' + token });
        signal?.throwIfAborted();
        return value ? conversationState(value, expectedPrompt) : { generating: true };
    };
    read.close = () => page.off?.('response', responseStatus);
    return read;
}
