# Actionbar, firing inventory transfers and normal Fat Skeleton

22 October 2026. Qualified and installed with rollback; see artifact evidence below.
[HUD source coverage](ACTIONBAR_COVERAGE_AUDIT.md),
[normal skeleton source contract](FAT_SKELETON_PORT.md).

## User requests and observed duplication

The user requests missing cooldown/effect durations in the actionbar, a fix for
an old-build minigun duplication during firing plus inventory Shift-click, and
continuation of migration. They confirmed the duplicate was persistent, both
copies usable, and the player was in Survival. Preserve this report as a real
reported bug; do not reclassify it as a visual ghost without evidence.

The current minigun updates ITEM_MODEL on the live held stack in place. Native
Survival clicks operate on server stacks; client prediction hashes do not insert
authoritative items. Independent static review found no concrete old-build
duplication cause. The user deferred further old-build investigation for a
repeat test and will describe it if it recurs (user):

> ладно пока забей я ещё раз затесчу, если повторится - более детально все опишу. Но это вероятно при подмене модели для обновления кадра вращения происходило - предмет с новым кадром независимо от наличия минигана в слоте вставлялся в этот слот

The new menu transaction hook stops the motor, input grace and frame animation
before the native click. Same-tick queued use cannot restart it. The actual
native move/swap/drop/cursor operation remains allowed, then the resulting
slots and cursor are sent as a complete authoritative snapshot. No saved hand
is restored, no inventory is locked, and no item-count clamp or UUID deduplication
is introduced. Ordinary later use can restart the motor. This removes active
animation/transfer overlap; until reproduced, it is not proof of the reported
old build's exact root cause. Native direct drop/offhand-swap/hotbar packets are
separate endpoints and are not mislabeled as menu click coverage.

## HUD and migration

Missing source-defined native HUD rows now cover Mobgun, ORAORA, Homing Lightning,
Stand Clocks, Overheaven matches and KFC Gigaponos duration. Beacon icons and
native arrow count are included. Source glyphs, colors, order and mixed tick/
second units remain. Generic compatibility scoreboard names alone do not imply
an implemented timer. See the complete source-to-native matrix.

HUD caching compares the completed Component and camera source, preserving the
20-tick heartbeat and ignorance filtering. Newly added values/styles/order do
not need a second manually maintained numeric cache schema.

Normal Fat Skeleton has its own exact source actor/token and complete native
three-pool rewards: one of nine already working wands, five bone-material rolls,
and the eleven-modifier relic. Existing source author, roles, conversion and
quiet lifecycle cleanup are reused. Full acquisition pools remain unexposed.
Actual RP/template and the user's Reroll source are unchanged.

## Installation instruction (user)

> а и не забывай билдить обновы на сервак

The earlier ready-only preference is superseded. Install the qualified artifact
with a rollback copy only while the main Fabric server is fully stopped.
The user installed prior ready artifact08f09e8f before this batch; it is the
current pre-install baseline, rather than the earlier96ee50a7 baseline.

## Qualification

Offline Gradle build and the combined native runner passed:142 recorded suite
results across two actual mode1 matches,116.1s, retaining all136 earlier
regression results. Ready artifact: `build/verification/skywars2-ui-weapon-batch-68387e1d.jar`.
SHA256: `68387e1d85a101c51abde1da72b2107d6565df28183759b1f9176e3ba56ef083`.
All222 Java sources match the source JAR;621 compiled classes and
2700 processed resources match the tested release byte-for-byte.
Evidence/manifest: `build/verification/ui-weapon-batch-final/`. The isolated server
is stopped, original files restored and fixture locks released. Only the known
fake-player Polymer packet-context warnings remain; no check failures.

The UI fixture drives native incoming use plus client-tick-end packets through
ServerGamePacketListenerImpl until paid shots and real frame changes occur.
Five Survival menu scenarios exercise Shift-click, offhand swap, throw, cursor
pickup/placement and stale-state hotbar swap. Native server/menu/world counts,
separately serialized Inventory/equipment counts and replayed outgoing slot/cursor
state stay consistent. The replay applies the cursor prediction declared in each
request before server corrections; it is not a real graphical Minecraft client.
Motor/grace cancellation, no stale hand restore, same-tick input rejection and
later native restart are checked. Actual match stop closes pending minigun input.

HUD checks capture source and observer-camera packets through active/ready cache
transitions for all four added cooldowns. They check non-destructive counting of
inventory/crafting/cursor arrows, retained Overheaven matches, actual KFC400->399,
placed native beacon glyphs/styles and clearing for the ignorance viewer.
Normal skeleton fixtures cover exact native equipment/attributes/drop chances,
role/ally guards, actual melee credit, death rewards, all nine real wand mappings,
relic components, native conversion and actual quiet stop. They reuse prior wand
ability qualification rather than claim to requalify all nine abilities here.

Retained development evidence: compile-attempt1 corrected the26.3 teleport ACK's
six-field constructor. Runtime attempt1 supplied a missing native client-tick-end
boundary required by Polymer; attempt2 included separately serialized equipment
in persistence counts; attempt3 modeled the exact cursor prediction already
reported by its request before replaying corrections. Attempt4 settled the empty-
hand equipment baseline and added diagnostics; attempt5 revealed a plain bone
stack was selected because its default lore component exists but is empty. Final
selection requires the named relic with nonempty lore, validates all eleven
modifiers, then tests actual native equipment effects. These fixture corrections
retain native dispatch and all conservation assertions; they are not reports of
production duplication bugs.

Installed main JAR: `E:\servers_mew\skywars2_last\mods\skywars2.jar`; its hash equals the qualified artifact.
Rollback: `E:\servers_mew\skywars2_last\luckywars-mod\build\verification\main-rollback\skywars2-before-ui-weapon-08f09e8f.jar` with SHA256 `08f09e8f5961282946c48254a7e76045ddab6374c322cb826c0b3b747a82429d`.
Main server was fully stopped at replacement and was not started by the agent.
The user's Reroll source remains6ef80947 unchanged. Actual RP/template untouched.

Limits: no old-build duplication reproduction (user deferred), real client pixels/
input/latency, actual disk restart/chunk unload/disconnect, long-running natural
mob AI, offline coin framework or peak capacity benchmark. Saved payload count
inspection is not disk reload. Packet recording/replay is not a real client
render test. Elapsed suite time includes boot/maps/regressions, not an MSPT figure.
