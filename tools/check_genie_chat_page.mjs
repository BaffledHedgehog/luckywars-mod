import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import vm from "node:vm";

const source = readFileSync(new URL("./genie_chat_page.js", import.meta.url), "utf8");
const receipt = { ok: true, verifier: "luckywars-script-1.0.0", program_sha256: "test" };
function inspect({ selected = "Thinking effort\n5.6 Medium", chat = "true", work = "false",
    origin = "https://chatgpt.com", path = "/c/owned-test", output = true, fenced = false } = {}) {
    const buttons = [{ innerText: "Chat", getAttribute: () => chat }, { innerText: "Work", getAttribute: () => work }];
    const final = { textContent: fenced ? "JSONCopy code" : JSON.stringify({ receipt }),
        closest: () => ({ getAttribute: () => "owned-reply" }),
        querySelector: selector => fenced && selector === "pre code" ? {textContent: JSON.stringify({ receipt })} : null };
    const main = {
        querySelectorAll: selector => selector === "button" ? buttons :
            selector.startsWith("[data-markdown") ? [final] :
            selector === "pre.text-execution-output" && output ? [{ textContent: "stdout " + JSON.stringify(receipt) + "\nstderr diagnostics" }] : [],
    };
    const document = { querySelector: selector => selector === "main" ? main : { innerText: selected } };
    return vm.runInNewContext(source, { document, location: { origin, pathname: path, href: origin + path } });
}
const normal = inspect();
assert.equal(normal.exactModel, true);
assert.equal(normal.chat, "true");
assert.equal(normal.work, "false");
assert.equal(normal.executions[0].program_sha256, "test");
assert.equal(JSON.parse(normal.replies[0].text).receipt.ok, true);
assert.equal(inspect({ selected: "Thinking effort 5.6 High" }).exactModel, false);
assert.equal(inspect({ selected: "Thinking effort 6 Medium" }).exactModel, false);
assert.equal(inspect({ output: false }).executions.length, 0, "a receipt in prose is not a code-execution witness");
assert.equal(JSON.parse(inspect({ fenced: true }).replies[0].text).receipt.ok, true);
assert.equal(inspect({ chat: "false", work: "true" }).work, "true");
assert.throws(() => inspect({ origin: "https://example.com" }), /Unexpected Chat origin/);
assert.throws(() => inspect({ path: "/g/private-unrelated-space" }), /conversation route/);
console.log("GENIE_CHAT_PAGE_PASS exact model, mode, native execution widgets, prose-only receipt, pending response and route guards");
