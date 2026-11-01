# Combat, visuals, FPV and restored Stonekivi Sun

Author decisions and implementation, 1–2 November 2026. Qualification receipt:
`build/verification/combat-visual-flight-2026-11-01/qualification.json`.

## Resource-pack repair

Apply the `assets/` folder from
[`updates/visual_repair_2026_11_01`](../resourcepack_template/updates/visual_repair_2026_11_01/README.md)
to the author's current pack and reload resources. The actual pack is read-only.
This patch is required independently of the server JAR.

The physical client log rejects the complete `minecraft:end_of_frame` chain:
`minecraft:entity_outline` is inaccessible in that chain in 26.3. Black-hole
composition and the Laser Minigun bloom consequently both stop. Move only the
polymorph outline emission into the legal outline chain, restore ordinary bloom
sampling, and preserve the scene's alpha transport byte exactly. Other signed
effects, zones, weapon spread and black-hole/nuclear shaders remain intact.

Radar's two special particle markers had clip depth zero, the far plane under
26.3 reverse depth. Give only those markers near depth `z=w`. Ordinary particle
depth and occlusion remain unchanged. Twenty offscreen GPU checks on RTX 4080
SUPER cover legal targets, actual black-hole horizon/lensing, cyan bloom,
polymorph isolation, all 256 alpha values and both radar marker occlusion cases.
These are GPU contract checks, not a physical Minecraft client test.

## Direct black-hole HP contact

Keep the already approved raw HP loss: exact sphere/native-hitbox contact,
bypassing mitigation/absorption and normal hit cooldown. Reuse shared incoming
admission, effects, criteria, reactive artifacts, death protection and death
listeners. Restore native anger/credit, no-action reset, damage game event,
source food exhaustion, DAMAGE_TAKEN and hurt/death sounds. Player sounds include
the victim explicitly: native Player.playSound omits self and normally relies
on the client hit event, which this damage intentionally does not send to self.

Other tracking clients retain red hit feedback; a camera following the victim
does not receive that damaging-camera event. Existing hurt state and velocity
stay untouched. Native health metadata precedes ordinary self health packets,
including the player whose sheep body receives the HP loss. No second damage
event is manufactured for death, and native totems still work.

## FPV availability and electricity

The target-free FPV implementation already existed; its raw `lbc:drone` root was
missing from public completions. Restore it alongside five individually qualified
roots: `big_bob`, `boba_eblan`, `anal_whip`, `rasengan`, `infinite_lucky_block`.
The earlier complete Chaos dependency gate has been cleared; unfinished kit6,
LuckyWars and deferred TMT gates are not changed. Five other hidden original
names are aliases already represented by ready `ported/` tables.

Drone short circuit precedes native immunity/cooldowns and detonates once for
actual vanilla lightning and electrical contact. Shared exact line tests admit
owned/allied drones too; a nearby miss does not detonate. Include yellow
projectiles/spin/conduction without changing their ordinary combat damage type.
Conduction and the legacy arrow-typed chain-lightning pulse explicitly recognize
the drone. Existing 20 HP, bounded steering, body/camera, Shift exit, input locks,
Time Stop and chunk-ticket lifecycle remain owned by the same FPV service.

## Mana and MK2 flight

Only negative maximum mana digits and negative regeneration minus/digits become
red; source separators/brackets and positive values retain their colors. MIN_VALUE
uses long magnitude. Hover cancels native additive gravity with an owned modifier
and zeros only Y, preserving X/Z and foreign gravity modifiers on exit. Exact
pitch −90/+90 receives the old maximum Wyvern lift/descent; intermediate angles
receive 1.5× baseline. MK2 chest plus sprint adds accumulated horizontal thrust;
its fuel cost is exactly 1.5×, retaining fractional fuel in existing checkpoints.
Without that chest the old horizontal/fuel behavior remains.

Frequent armor/flight/drone flags now use a read-only scalar CustomData mixin
instead of copying the whole compound. No backing mutable NBT escapes and no
cache can become stale after component replacement. This is a narrow allocation
improvement, not a claim that all NBT work or all world scans have been removed.

## Restored Sun: latest author corrections

Return Sun to **Iso Kivi / Stonekivi**, not Fireblaze. Preserve Crystallization as
mode 3 and add Sun as mode 5, cost 2500 mana. New grimoire ID 300 uses the original
`lbc:spelllib/stonestone` model and its own translation keys; merge the two
`updates/sun_2026_11_01.*.json` files into the corresponding language JSONs.

Retain retired geometry: nominal radius 100, centre 148 blocks above impact.
Latest author direction replaces **all lava and all fire with magma**. Core and
shell use magma; outer noise and curved finite tongues mix magma, glowstone,
shroomlight and orange/yellow glass. There are no lava or fire cells anywhere.
Use the existing Stonekivi queue, terrain budget, resident columns, strict quiet
block writes, lighting completion, journals, attribution and checkpoint graph.
No new generator, ticking emitter or persistent chunk ticket is introduced.

Native checks exercise actual both-hand grimoire use and five-mode selection,
then cancel full-size diagnostic jobs before writes; test native placement on a
small radius and 120 ordinary persistence ticks. Full-size geometry is sampled
separately. Full-radius peak TPS and end-to-end fill latency are not measured.

## Retained failures

Initial compilation exposed old mappings for stained-glass constants and the
invulnerability setter; corrected to native 26.3 APIs. First native batch passed
five groups but failed the new DAMAGE_TAKEN assertion: TestAlpha's existing
persisted counter was INT_MAX, so native saturation disagreed with the test's
overflowing `before+6`. Reset only the probe counter to zero and restore its full
original value afterwards; runtime damage/stat logic was not weakened.
The first lava/fire Sun artifact is superseded by the author's magma-only revision.

## Qualified artifacts and deployment

Final test-server JAR: `test-10b7c054.jar`, SHA-256
`10b7c05418edbc2298212f3aa0dbc29dc06c7caf3567badf551bd5907eb90e1c`.
Production: `production-5ed436dd.jar`, SHA-256
`5ed436dd940d8edd2845517b99da0e2214c138e1a840329f2f51790937364b6b`.
Shared entries are byte-identical; production omits all eight map editing classes.
This installation uses the test-server artifact.

Six native groups passed in six actual mode1 matches: black-hole-contact,
combat-feedback, fpv-drone, techno-armor, stonekivi-sun, shaping-grimoires. Actual
loot autocomplete and real item resolution include all six recovered roots and
the new Sun grimoire. 9792 build inputs stayed frozen; all 2472 compiled classes
and 7353 processed resources match the selected artifact. All 226 protected
author paths and 31 live grimoire overrides remain byte-identical. Child shutdown
was normal; player/configuration/OP/level/ticket snapshots restored, no retained
lock, only the allowlisted fixture on port25577 was used.

The normal-stop installer receipt lives next to qualification.json. It waits for
ordinary main-server shutdown, retains the prior JAR and does not restart it.
Do not replace a running main-server JAR. No remote push was performed.

Receipt confirms installation of `10b7c054` while the main server was stopped;
previous `25dcef82` is retained as rollback. `restarted:false`.
