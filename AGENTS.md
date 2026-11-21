# Working instructions

Read `agent_notes/CURRENT_GOAL_STATE.md` first when it exists. It contains local
handoff state, active processes and verification evidence. Do not restart an old
task from a historical `RUNNING` or `CURRENT` heading. The latest user request and
session instructions take precedence over older notes.

## Source navigation and changes

- Read the codebase MCP `initial_instructions` before coding. Use Java symbol
  definitions and references for navigation; use `rg` for exact text and resources.
- Use semantic Java moves through `tools/java_refactor.py` in a clean worktree.
  Review non-Java references too: Fabric/Mixin metadata, string targets and tools.
  Run `tools/check_java_layout.py` after package changes.
- Main sources belong in `src/main/java`; native checks and map authoring belong
  in `src/checks/java`. Checks must not enter the production JAR.
- Keep registry IDs, translation keys, saved-data keys and protocol identities
  stable during reorganization. Use complete literal translation keys, so exact
  searches find their uses. Explain non-obvious lifecycle decisions in comments.
- Do not introduce reflection into production code. Keep native APIs, typed
  accessors and shared engines rather than duplicate item-specific mechanics.

## Builds, verification and safety

- Use Java 25. Keep owned builds, language servers and native fixtures at low CPU
  and disk priority with bounded heaps. Do not cap threads, processor count or
  affinity unless the user changes that preference. Run one heavy owned JVM at
  a time; leave the user's IDE, game and language servers alone.
- Do not run `gradlew clean`: ignored `build/` also contains verification worlds,
  sealed artifacts, worktrees and deployment evidence. Remove only explicitly
  checked reproducible output directories when necessary.
- Verify changes with the relevant native scenarios and production/test archive
  separation. Report actual results and limits; compilation alone is not native
  proof. Preserve fixture worlds and restore their snapshots on exit.
- Preserve user edits, worlds, credentials, authored assets and legacy evidence.
  Never print or commit cookies, API keys, deployment credentials or private data.
  Delete only confirmed generated junk. Do not add blanket ignore rules for source.
- Commit completed, verified stages separately. Publish or deploy only within the
  user's current authorization. Do not start a server in the background merely
  because a build completed.
- For performance work measure tick cost, cast-to-result delay and queue growth.
  Do not disguise slower gameplay or an expanding queue as an optimization.
  Never wait for external work on the game thread; use bounded work and snapshots.

## Gameplay contracts and documentation

See [ARCHITECTURE.md](ARCHITECTURE.md) for the current source boundaries and
[SERVER_TESTS.md](SERVER_TESTS.md) for artifact checks. Before changing a gameplay
family, consult its existing notes and the relevant user contracts in
[the preserved development reference](docs/history/AGENTS_2026_11_21.md).
That document contains item-specific requirements, including Overload mana,
failed island copies, TNT drone host lifetime, and ant/acid collision behavior.
Its dated build/deployment statuses describe past evidence, not current state.

User-facing messages and GUI labels use translatable components. Preserve authored
item text and resource-pack designs; merge additions instead of replacing unrelated
shaders or assets. Follow current session authorization for editing the live pack.
When the user is away and requests file-only questions, collect them in the active
questions file under `agent_notes/` and continue independent work.

For explicitly authorized delegation, give agents bounded ownership and concise
context. Use the requested cheaper model for narrow searches/resources where
applicable; the primary agent owns integration and final validation.
