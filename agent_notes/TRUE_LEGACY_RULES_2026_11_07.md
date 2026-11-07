# True legacy / Lost Balance: runtime boundary

7 November 2026. This implements the author's clarification of the old `legacy lbc.math` overlay. It extends the qualified full-source Lucky modes build `6bf1ff2c`; the existing Supernova, Balance map transition, old items and old loot owners remain in use.

## Behavior

- Actual Balance start and legacy respawn grant the eligible selected ordinary kit and skill through `MatchEvents`. Kits end at Half Slabs (ID21); Vampire11 and Phasmatic13 are excluded, Heavyweight12 remains. Saved selections are retained.
- Super kits, super skills, challenges/rewards, trinkets, selected kill/win effects and prefix/suffix presentation are inactive. The ordinary ending fireworks remain. The shared skill admission also covers direct raw-profile readers; no second skill engine was introduced.
- Mana processing, the custom actionbar and Attribute HUD stop during the overlay. Entering clears already displayed UI and refreshes knowledge/artifact/nameplate presentation. Client team packet projection strips decorations without modifying canonical server teams.
- Vanilla damage type definitions and tag memberships are frozen from the local unmodified Minecraft26.3 JAR. `tools/build_vanilla_damage_tags.py` records its hash and resolves nested tags. The immutable runtime snapshot contains 36 tags and 51 definitions; reads during damage use cached maps/sets, not NBT or disk. There is no global registry reload or client registry change.
- While the overlay is active, damage queries use that vanilla snapshot and extended armor, multiplicative weakness and signed resistance overrides defer to native behavior. Arrow hits obey native hurt cooldown. Exiting the match clears the overlay; the same existing DamageSource immediately observes the ordinary server policy again.
- The existing `native_lucky/normal` legacy branch still selects `legacy_luckyblock`; no neko/loli producer was introduced. All 5,932 current and old literal loot tables are byte-identical to the qualified parent, including old presentation. Existing Balance runtime owners are byte-identical too.

The old item source audit remains [Balance runtime source](BALANCE_LEGACY_RUNTIME_SOURCE_2026_11_03.md) and [reward transition](BALANCE_REWARD_SOURCE_2026_11_03.md). Historical absence verdicts there are superseded by the later implementation and this evidence. This is the old gameplay overlay on the current Minecraft runtime, not an old Minecraft binary; no claim that every historical numerical edge case was exhaustively tested.

## Gallery

The actual native catalog reports 18 identities with explicitly authored barrier models. They are old active-drop carriers (holes, bees, old drone, creeper and similar), activated by the existing dropped-item machinery. The author confirmed these are expected. No gallery appearance, visibility or admission behavior was changed; the check only logs the identities. This finding is not a blanket statement about every possible barrier from arbitrary datapacks.

## Build and checks

Both archives are ordinary full-source Gradle outputs (`productionJar testServerJar`), staged separately. No component overlay JAR is used.

| Artifact | SHA-256 |
| --- | --- |
| test | `4966406c1e8871e12e6d5058427af5f46fd3520e21a0e399cf14f61e6c98384e` |
| production | `121f21c3432394c7f8e78e227637a40fb6d290f74a9d4459930b5ee89937dfc9` |

Receipts and archives: `build/verification/true-legacy-2026-11-07/qualification.json`, `package-proof.json`, `artifacts.json`, `full-build.log` and the two `4966406c-*-results.json`/log pairs.

Eight fresh actual mode1 matches pass on the same final test archive:

1. Gallery, then an actual Supernova -> owner Balance reward -> completed old-map transition with `supernova-chain`, `true-legacy-rules`, `balance-runtime`, followed by observed `true-legacy-exit` after real stop. The old runtime suite exercises eight old wands, native callbacks/fangs, no mana, 81 Wiki outcomes, scrolls, TNT rays, old bows/shulkers/containers and compressed/old nuclear eggs with terrain restoration.
2. Six ordinary regressions: `attribute-hud`, `damage-cooldown`, `inventory-skills`, `cosmetics-native`, `nameplates-native`, `lucky-loadouts`. The last uses actual native menu selection and match start, including the modern 16/1/1 kit and protected-inventory skill.

The initial diagnostic failure is retained: its Cow fixture expected 20 HP but the native Cow maximum is 10. The fixture now explicitly sets its maximum to20; no gameplay damage implementation was altered for that correction. Final acceptance uses only the final artifact's successful receipts.

All 10,501 frozen source inputs and 281 protected author paths match, including the three unrelated Java WIP files, HUD/RP edits and author deletions. Archive CRC/JSON checks pass; production contains zero map recording/deletion classes, test contains eight. One Gradle JVM (768MiB, one worker), then one fixture JVM (1280MiB) ran sequentially. Both fixture runs shut down normally and restored their exact snapshots without retained locks. These are native server checks; physical Minecraft visuals, arbitrary maps and peak TPS were not measured.

## Delivery

Qualified archives remain `test-4966406c.jar` and `production-121f21c3.jar` in that verification directory. `build/libs` and Main were not replaced, no installer was queued, and Main was not restarted. Main currently contains the previous qualified **production** archive `299e61e8`; this supersedes the older note that a test archive was pending. Respect the author's choice of flavor and the standing request to hold all fixes for the next stable patch. The author resource pack remains untouched.
