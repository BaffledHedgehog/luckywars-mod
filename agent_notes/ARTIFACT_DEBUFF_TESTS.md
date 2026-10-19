# Artifact debuff checks

Status: focused runtime checks passed in 66.81 s against JAR SHA-256
`ad09d832da32467c8e7eafabb9b6e478d4f794fe83bbb0b2c8bb2b33ea72bf36`.
The complete regression also passed: 254.54 s, 13 matches and all 36 runner
groups, against the same JAR. Final evidence and deployment hashes are recorded in
[SERVER_TESTS.md](../SERVER_TESTS.md).

Run `tools/smoke_lbc_loadouts.py --server-dir <isolated-server> --artifact-debuff-only`
after building the mod. The slice loads the artifact source fixtures, starts a
match, and checks type 5 debuff removal and levitation protections, then type 6
cleanup of cold-only frozen state and a turn curse when the player has no native
potion effects. The fixture also verifies that shared cleanup restores temporary
ice and drops its frost tracking state. Additional assertions cover 5+8 and
5+6+8 ordering, preserving the inventory capability mask, native cleansing-potion
callback, overlapping ice ownership and release of the cleansed player's anchor.
The run stops the match and restores the server configuration files.

The first iteration failed in the newly added overlap fixture because it
asserted ice blocks before invoking the native frozen tick that places them.
The fixture was corrected; production cleanup did not need an additional change.
That initial log is preserved as `build/verification/artifact-debuffs-first-fixture.log`.

The legacy type 6/8 passive checks remain in
`tools/smoke_lbc_loadouts.py --artifact-passives-only`; the focused type 5/6
slice does not replace them. A passing source check or Java manifest audit is
not a substitute for the runtime result recorded here. Full mode runs both the
existing passive slice and the new slice, adding one match and three groups to
the previous baseline; focus mode has four groups including reusable bots.
