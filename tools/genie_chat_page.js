/* Read only the owned Chat tab's rendered controls, assistant reply and real tool-output widgets.
   No cookies, page internals, sidebars, history, network interception or other tabs.
   The orchestrator must additionally bind the expected conversation URL and request identity. */
(() => {
    if (location.origin !== "https://chatgpt.com" ||
        !/^\/(?:c\/[A-Za-z0-9:%_-]+)?\/?$/.test(location.pathname)) {
        throw new Error("Unexpected Chat origin or conversation route");
    }
    const main = document.querySelector("main");
    if (!main) return {ready: false};
    const model = document.querySelector('[aria-label="Select ChatGPT model"]');
    const effort = model?.getAttribute?.("data-selected-reasoning-effort");
    const modernModelPicker = effort != null;
    const modelChoices = modernModelPicker ? [...document.querySelectorAll('[role="menuitemradio"]')].filter(node =>
        node.textContent.trim() === "GPT-5.6 Sol") : [];
    const solSelected = modelChoices.some(node =>
        node.textContent.trim() === "GPT-5.6 Sol" && node.getAttribute("aria-checked") === "true");
    const renderedModel = model?.innerText.replace(/\s+/g, " ").trim() ?? "";
    const explicitFamilyAndEffort = /\b5\.6 Medium$/.test(renderedModel);
    const selected = modernModelPicker ? `${solSelected ? "GPT-5.6 Sol" : "Other"} ${effort}`
        : model?.innerText.replace(/\s+/g, " ").trim() ?? "";
    const mode = (label) => {
        const button = [...main.querySelectorAll("button")].find(e => e.innerText.trim() === label);
        return button ? button.getAttribute("aria-pressed") : null;
    };
    const stopControls = main.querySelectorAll(
        'button[data-testid="stop-button"],button[aria-label="Stop"],button[aria-label="Stop generating"]');
    const generating = [...stopControls].some(button => !button.disabled
        && button.getAttribute("aria-disabled") !== "true" && button.getClientRects().length > 0);
    const replies = [...main.querySelectorAll('[data-markdown-text-style="assistant-message"]')].map(e => ({
        message: e.closest("[data-response-body]")?.getAttribute("data-response-body") ?? null,
        text: e.querySelector?.("pre code")?.textContent ?? e.textContent
    }));
    const executions = [];
    for (const node of main.querySelectorAll("pre.text-execution-output")) {
        /* A model-authored code block does not have this native tool widget. */
        const raw = node.textContent;
        if (raw.length > 65536) continue;
        for (const line of raw.split(/\r?\n/)) {
            const value = line.startsWith("stdout ") ? line.slice(7) : line;
            if (!value.startsWith("{")) continue;
            try {
                const result = JSON.parse(value);
                if (result && typeof result === "object") executions.push(result);
            } catch (_) { /* Runtime diagnostics are not a verification receipt. */ }
        }
    }
    return {
        ready: Boolean(model),
        url: location.href,
        selected,
        exactModel: modernModelPicker ? (solSelected || explicitFamilyAndEffort) && effort === "medium" : explicitFamilyAndEffort,
        modernModelPicker,
        modelMenuOpen: model?.getAttribute?.("data-state") === "open",
        selectedEffort: effort,
        solSelected,
        modelChoicesVisible: modelChoices.some(node => node.getClientRects().length > 0),
        chat: mode("Chat"),
        work: mode("Work"),
        generating,
        replies,
        executions
    };
})()
