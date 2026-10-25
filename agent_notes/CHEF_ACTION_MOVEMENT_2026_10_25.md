# Chef action facing and continuous pursuit вЂ” 25 October 2026

Status: QUALIFIED AND INSTALLED on the main server, without restart. Supersedes the movement,
equipment and action presentation details of [the previous rework](CHEF_REWORK_PORT.md).

## Author requirements

> С€РµС„ С‡Р°СЃС‚Рѕ РЅРµ СЃРјРѕС‚СЂРёС‚ РєР°РјРµСЂРѕР№ РІ РјРµСЃС‚Рѕ РІ РєРѕС‚РѕСЂРѕРј РґРµР»Р°РµС‚ РґРµР№СЃС‚РІРёРµ - СЌС‚Рѕ РѕС‡РµРЅСЊ СЃС‚СЂРѕРіРѕРµ С‚СЂРµР±РѕРІР°РЅРёРµ.

> РµС‰С‘ РЅРµ РЅР°РґРѕ РµРјСѓ 2 СЂСѓРєРё РїСЂРµРґРјРµС‚Р°РјРё Р·Р°Р±РёРІР°С‚СЊ - РїСѓСЃС‚СЊ РёСЃРїРѕР»СЊР·СѓРµС‚ С‚РѕР»СЊРєРѕ РѕСЃРЅРѕРІРЅСѓСЋ СЂСѓРєСѓ, РїРµСЂРµРєР»СЋС‡Р°СЏ РїСЂРµРґРјРµС‚ РІ РЅРµР№

> РµС‰С‘ РЅР°РґРѕ СЃРґРµР»Р°С‚СЊ РµРјСѓ РјРёРЅРёРјР°Р»СЊРЅРѕРµ РІСЂРµРјСЏ РјРµР¶РґСѓ РїРµСЂРµРєР»СЋС‡РµРЅРёРµРј РїСЂРµРґРјРµС‚РѕРІ, С‡С‚РѕР±С‹ РѕРЅ РЅРµ РјРѕРі РєР°Р¶РґС‹Р№ С‚РёРє СЃРІР°РїР°С‚СЊ РјРµС‡ РєРёСЂРєСѓ РёР»Рё Р±СѓР»С‹Р¶РЅРёРє, СЌС‚Рѕ РѕС‡РµРЅСЊ РЅРµ РЅР°С‚СѓСЂР°Р»СЊРЅРѕ РІС‹РіР»СЏРґРёС‚

The remainder of this batch addresses the requested placement/attack sounds,
rejected-hit animation and particles, faster construction toward airborne victims,
horizontal/vertical/mixed diagonal building, sustained straight sprint jumping,
3вЂ“4-block gaps, partial-block stairs and indirect routes instead of stationary hops.

## Implementation

`LbcWtf.Chef.aimAction` turns actual body/camera/head before an action is eligible.
Camera ray and head yaw must align within two degrees; merely requesting a turn
does not permit placement, mining progress, melee or bucket use. A short attention
lease prevents steering from replacing the action aim with the victim's eyes.
Changed native rotation packets precede action feedback, independently of the
ordinary entity-tracker interval. There is no movement teleport in these packets.

The offhand starts empty. `equip` centralizes deliberate main-hand changes with
a six-active-tick minimum dwell. Repeated movement/placement does not insert a
fresh sword each tick. Real bucket empty/fill results remain immediate native use
transformations, rather than being treated as hotbar swaps. Native timed food use
retains the exact consumed stack. Mining sounds use the block sound type; actual
placement emits its place sound and BLOCK_PLACE event. Every scheduled melee
attempt swings and sends attack particles and accepted/no-damage sound, even when
the target rejects damage. Existing sword damage/cooldown, crit predicate,
fixed-victim binding and summoner attribution remain.

`LbcChefBrain` keeps running-jump landing intent while airborne, retains inputs
when a route slice is pending in the air and follows a supported direct corridor
when available. Five-cell jump edges represent real four-block gaps; an existing
valid run-up is preserved and long launches wait for the supported edge. Chef
uses the native Player sprint air-control coefficient (0.026, otherwise 0.02).
A successful jump enters airborne travel before friction calculation so a
Mannequin's stale ground state cannot consume the launch impulse. Its normal
gravity, collision and drag continue. A strafed build jump directs the existing
sprint impulse along requested movement rather than into the support-face yaw.

The terrain-only search still tries walking, partial-block steps and parkour
first. Edit search retains jump edges. Horizontal diagonal bridge edges and rising
diagonal RAMP edges add bounded, visible, face-connected intermediate supports;
they do not create unsupported corner blocks. Placements keep reach, world-border,
collision and native loaded-chunk guards, with at least two ticks between writes.
If Chef overlaps a future step, it first backs onto its valid launch cell.
Pillars prepare the block hand and downward aim before jumping and need less
unnecessary centering/settling. A flat terrain search for an elevated target
switches to construction earlier; edit heuristics account for missing height.
The node/probe budgets and fair global scheduling remain bounded.

Stuck edges get a short exclusion rather than immediately retrying the same
impossible action. A separate projected-progress watchdog detects repeated jumps
without progress, excluding genuine mining and deliberate runway repositioning.
Water survival takes out/aims the bucket in advance, then uses native reach and
timing; after landing it settles near its own source before pickup instead of
swimming away during the turn. Knock, prone gravity, void rescue, true Time Stop,
hunger, skin, custom sword, lifecycle and match cleanup retain their contracts.

## Verification

`ChefChecks` now has seventeen independent fixture groups. In addition to the
previous native combat, ownership, knock, rescue, shapes, mining, bridge, detour,
hunger, water and guard coverage, it exercises three/four-block gaps, a 3/4/3
gap chain, a continuous half-step staircase with a right-angle turn, horizontal
and rising diagonal bridges, a pillar toward an airborne target, main-hand dwell,
empty offhand and attack sound/particles/swing against an invulnerable target.
Every observed placement/mining/melee/bucket action checks its actual aim error.
`chef-natural` remains a separately elapsed real-world-tick chase, including
prone falling knock, wake, smooth bounded rotations, slabs/stairs, a required
ceiling bridge and an attributed accepted hit. Stop probes use actual match end.

Final Gradle build `16e097ca4cee31dcafbf1304162433e009fd936935154d1d7d085ab78f29e4b2`
passed ten recorded suite results in two fresh mode1 matches (88.48 seconds,
including boot/map preparation). Both Chef invocations passed all seventeen
fixture groups, followed by two naturally ticking chases and actual-stop cleanup.
The airborne-target fixture starts construction at tick11 and can hit after
three support blocks plus its native jump; it need not waste a fourth block.
Exact371 Java sources,1044 classes and2837 resources match the complete artifact.
User files/deletions and the author's resource pack remain unchanged.

Evidence: `build/verification/chef-motion-final/qualification.json`, the native
log/results, immutable `qualification-16e097ca.json` and `main-install.json`.
Ready artifact: `build/verification/skywars2-chef-motion-16e097ca.jar`.
Main baseline71d12d26 remained unchanged through qualification. The installer
confirmed the world lock and port were closed, installed16e097ca and retained
71d12d26 as rollback; it did not restart the server. Development class overlays were diagnostic only;
the qualified artifact was produced by the full Gradle build. Rendered client
frames, perceived human likeness and peak-load TPS were not directly observed.
