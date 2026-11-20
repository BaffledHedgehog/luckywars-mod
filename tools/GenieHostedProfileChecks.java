package me.skywars2.game.genie;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Runs the packaged Node worker with a local Playwright double and no browser or network. */
public final class GenieHostedProfileChecks {
    private static final String COOKIE_EDITOR = """
            [{"domain":"chatgpt.com","name":"fixture","value":"synthetic","path":"/",
              "expirationDate":1900000000.125,"session":false,"httpOnly":true,"secure":true,
              "sameSite":"no_restriction"}]
            """;
    private static final String PLAYWRIGHT = """
            import assert from 'node:assert/strict';
            import { stat, writeFile } from 'node:fs/promises';
            import { join } from 'node:path';

            export const chromium = {
                async launchPersistentContext(profile) {
                    const directory = await stat(profile);
                    assert(directory.isDirectory());
                    if (process.platform !== 'win32') assert.equal(directory.mode & 0o777, 0o700);
                    await writeFile(join(profile, 'probe-witness'), 'ready');
                    return {
                        async addCookies(cookies) {
                            assert.deepEqual(cookies, [{
                                domain: 'chatgpt.com', name: 'fixture', value: 'synthetic', path: '/',
                                expires: 1900000000.125, httpOnly: true, secure: true, sameSite: 'None'
                            }]);
                            await writeFile(join(profile, 'import-witness'), 'normalized');
                        },
                        async newPage() {
                            let url = 'about:blank';
                            return {
                                url: () => url,
                                goto: async target => {
                                    assert.equal(new URL(target).origin, 'https://chatgpt.com');
                                    url = target;
                                    return { status: () => 200 };
                                },
                                evaluate: async expression => typeof expression === 'function' ? false : ({
                                    url, selected: 'GPT-5.6 Medium', chat: 'true', work: 'false', exactModel: true,
                                    replies: []
                                }),
                                locator: () => ({ waitFor: async () => {} }),
                                getByRole: (role, options) => ({
                                    isVisible: async () => role === 'textbox' && options.name === 'Ask ChatGPT',
                                    isEditable: async () => true
                                }),
                                close: async () => {}
                            };
                        },
                        close: async () => {}
                    };
                }
            };
            """;

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "pass the absolute Node executable path");
        Path node = Path.of(args[0]).toAbsolutePath().normalize();
        Path temp = Files.createTempDirectory("genie-hosted-profile-check");
        try {
            Path playwright = Files.writeString(temp.resolve("playwright.mjs"), PLAYWRIGHT);
            Path profile = temp.resolve("state/profile");
            Path requests = temp.resolve("state/requests");
            var transport = new GenieHostedChatTransport(node, playwright, node, profile, requests);
            require(Files.notExists(profile), "probe starts without a browser profile");
            transport.connect(null);
            require(Files.readString(profile.resolve("probe-witness")).equals("ready"),
                    "the real worker opens the fresh persistent profile");
            requireEmpty(requests);

            Path established = Files.writeString(profile.resolve("established-witness"), "keep");
            try {
                transport.connect("{\"cookies\":[]}");
                throw new AssertionError("invalid browser state was accepted");
            } catch (GenieHostedChatTransport.Unavailable rejected) {
                require(rejected.issue == GenieHostedChatTransport.Issue.INVALID_BROWSER_STATE,
                        "browser-state validation is reached after the isolated profile is created: " + rejected.issue);
            }
            require(Files.readString(established).equals("keep"), "failed import retains the established profile");
            requireEmpty(requests);

            transport.connect(COOKIE_EDITOR);
            require(Files.notExists(established)
                            && Files.readString(profile.resolve("probe-witness")).equals("ready")
                            && Files.readString(profile.resolve("import-witness")).equals("normalized"),
                    "successfully probed CookieEditor state replaces the established profile");
            require(Files.notExists(profile.resolveSibling("chatgpt-profile-previous")),
                    "previous profile is retired after atomic promotion");
            requireEmpty(requests);
            System.out.println("GENIE_HOSTED_PROFILE_PASS");
        } finally {
            try (var entries = Files.walk(temp)) {
                for (var path : entries.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void requireEmpty(Path directory) throws Exception {
        try (var entries = Files.list(directory)) {
            require(entries.findAny().isEmpty(), "worker output and request-owned browser state are removed");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
