# Next complete port source assessment — 25 October 2026

## Chef route selection and compact takeoff - QUALIFIED, AUTO INSTALL QUEUED (26 October 2026)

See [behavior, native fixtures and evidence](CHEF_ROUTE_SELECTION_2026_10_26.md).
Chef compares walking, parkour, mining and construction in one frontier;
there is no exhaustive terrain-only pass before building. Supported short
approaches to clear columns beneath airborne victims are validated directly.
Retains forward speed at takeoff; fallback runway is 0.75 blocks. Small rising
landings use ordinary jumps; noisy walk/drop routes can retain sprinting.
Pillars hold actual downward camera/head aim through placement and landing;
vertical action yaw is stable. Strict two-degree action facing, gravity,
main-hand dwell, native sounds, knock, water and fixed-victim ownership remain.

Build 385f9f11 passed 22 recorded suite results in two actual mode1 matches
(98.01 seconds). Each passed all 23 Chef fixture groups, a real-world-tick
chase, sheep/Danmaku/player-rule/Time-Stop regressions and actual-stop cleanup.
New fixtures cover diagonal full-block stairs, noisy partial-block terrain,
short bridge versus a long detour, stable pillar facing, moving takeoff and
prompt construction toward an offset airborne target. Exact 401 sources,
1130 classes and 3138 resources match the tested release. Protected author
files/deletions, live grimoire overrides and the author RP are unchanged.

Ready: build/verification/skywars2-chef-routing-385f9f11.jar; immutable evidence
and live receipt: build/verification/chef-routing-final/. Existing installer 13420
adopted this build, superseding queued a82498d0; all preceding native gear/totem
changes are included. It waits for normal main shutdown/world unlock (port 24566,
RCON disabled), without killing/restarting the server. Main remains 7da14e96.
Retain ready 385f9f11, preceding a82498d0 and installed 7da14e96. Old 7dc0342b
archive retired; cumulative cleanup 317 JARs/11275598510 bytes. Rendered frames,
subjective human likeness, arbitrary map routes and peak TPS are not qualified.

## Hedgehog family and speed-only FOV - QUALIFIED, AUTO INSTALL QUEUED (25 October 2026)

[Family contract](HEDGEHOG_PORT_2026_10_25.md),
[FOV contract](SPEED_FOV_PORT_2026_10_25.md).
Native ordinary/Academy Hedgehog, five helper modes,65-fish WILD walls,
seven consumable spikes, real dimension transitions, God boss and inert
trophy expose ten narrow leaves. Arena is the logical Overworld destination;
real Nether/End/Nexus/Imprinted remain separate worlds. Boss removes all other
silverfish in its own dimension and rejects new ones while alive.

Author rule: ignited Fuse0/1 creepers are datapack workarounds; use native
explosions, never spawn them as instant explosives. Rare numerical1e-13
silverfish sneeze queues32 native power20 MOB explosions, with captured WILD
author, real TNT-chain lineage and shared nuclear shader. Chat key
`skywars2.hedgehog.sneeze` is already author-applied to RU/EN RP.

Speed FOV contribution is multiplied by0.05; full rifle projection remains
0.25. The server cannot force the client's FOV Effects preference to100%.
A shared Greed fix lets allied native arrows contact the deflector and leave
the new owner's hitbox without self-reflecting. Native boss fuse0 lasers
ignore TNT protection and work in their actual encounter dimension.

Build4cd2ef8f passed32 results in two actual mode1 matches (83.49s),
including both hands/modes/payloads, native explosions/roles/Greed/ownership,
actual player/world transitions and real stop cleanup. Helpers and boss stop
probes are in separate matches because the boss forbids helper silverfish.
Exact365 sources/1031 classes/2837 resources match; all281 ready leaves
are packaged. Sound inventory maps281/281 leaves,292 sites,104 owners,
zero unknown native vanilla IDs. Client pixels and peak main TPS not observed.

Ready: build/verification/skywars2-hedgehog-4cd2ef8f.jar; evidence:
build/verification/hedgehog-final/. Main disk95c3db2f unchanged at qualification.
User now authorizes automatic qualified installs, superseding READY ONLY.
`tools/install_when_stopped.py` is running: main-install.json records live
status. It waits for normal shutdown/world unlock, validates hashes and keeps
rollback; it never kills/restarts main. Subsequent qualified requests replace
the pending one. Main currently running with RCON disabled.

User files/deletions and author RP are preserved. Empty already-applied
sneeze fragments removed. Obsoleteccb40b6c archive removed after qualification;
retain latest4cd2ef8f, precedingb2f50a60, main95c3db2f and historical fallbacks
until the running JVM's provenance is known. Cumulative cleanup292 JARs,
10,308,156,309 bytes (~9.60GiB). Remaining large families: combined Sauvojen,
unstable spellbook/broad-library outcomes; unfinished pools stay gated.

Read-only research. Only this note written; no source/server/resource-pack mutations, builds or commits. Rules read: root AGENTS.md, agent_notes/AGENTS.md, RESOURCE_PACK.md; current migration-plan relevant remaining-family sections. UTF-8 reads throughout. Legacy paths below relative to E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/.

## Recommendation

Direct consumed Danmaku grimoire **159** is the strongest bounded complete next leaf, combined with the separate sound audit. Detailed exact source/item presentation is already in [DANMAKU_GRIMOIRES_SOURCE.md](DANMAKU_GRIMOIRES_SOURCE.md). Combined Sauvojen and unstable book are not similarly bounded. Do not expose broad spells_library or random-grimoire wrappers for this leaf.

159 is one ordinary grimoire, not an Academy pair. 160 is Swarogia/ruby. Existing LbcDanmaku.queueExternal captures actual hand and ordinary/Academy provenance, adds20 queued ticks and independently continues after consuming the last item. Parent reports the user already authorized continued external emission and captured hand; no repeated question needed on that point.

Only potential decision is accidental source sneak refund: select bypasses the paid staff wrapper but click_2 still refunds1000 while sneaking. Current staff contract has already removed this refund. Recommended target is the existing free bomb behavior, with no refund and no mana gate; do not silently claim that preserves the source bug. If parent judges current authorization covers this consistency fix, no further permission required.

## Direct159 implementation contract

Source load creates used-snowball statistic; players_tick → items/snowballs → projectiles/eggs (gadget) → items/libspells/select. select:1 reads spellid, :5 removes thrown libspell carrier, :71 calls items/wands/danmaku_staff/click_2. No mana admission/debit happens on this consumed route. Stored mana1200 is Looper/universal-wand metadata, not charged resource.

Use one native consumable item with snowball Polymer appearance, model lbc:spelllib/danmaku, tierC tooltip, max stack99. Exact authored name/lore/components/custom data are transcribed in prior note; use that source entry, not copied staff presentation. Preserve unresolved_grimoire1, mana1200, spellid159, transformable/gadget/libspell/spellwithcooldown markers if compatible with native metadata mechanisms. A deterministic narrow ported/danmaku_grimoire leaf is appropriate; do not claim original1/240 broad-table distribution.

Native normal use: observer/action/antimagic/time-stop guards first; queueExternal(p, actualHand, false), consume exactly one actual used stack after successful admission. No staff hooks or mana debit inferred merely from staff integration. Sneak: existing bomb≥50 consumes all charge, no queued volley; below50 should neither queue nor consume under established native failed-action convention. Above1500 should not mark this grimoire as an Academy trial: only actual ordinary staff used-stack has that promotion pathway. Source throw itself consumed even failed sneak, so native failure retention is a conscious convention, not historical source equivalence.

Tests: actual use in each hand at mana0 and negative mana; exact decrement including final stack, no residual thrown snowball;40/35 ordinary cards per20 ticks, no Academy pattern; queue continues after switching hand/item; suppression/time stop pause and resume; repeat additions remain independent of held staff state; bomb succeeds at50/1500 and leaves mana unchanged; low bomb admission does not spend; observer/Greed/team/roles protections; source card credit; match stop clears queued casts/cards/bombs. Validate copied leaf converts to native item and item roundtrip retains exact components. Real match native checks plus artifact/source snapshot matching remain needed; no runtime claims from this audit.

## Why not full combined Sauvojen now

items/wands/sauvojen_wand/cast[_upgraded] delegates cast_2 then debits configured sauvojen_wand_mana_use. cast_2 chooses uniform1..9 displays at eyes+2: ruby,terra,electro,air,acid,teleport,stone,fire,blind; owner/team copied; fixed display transform matrix, scale2.5. Models lbc:st/<element>. Ordinary grants nine advancement criteria separately.

flying_wand_tick: Lifetime starts2 if1; retires at200 or nonair block; faces nearest enemy≤150; moves0.1 away from owner, maintaining facing. Ordinary cadence ruby60/blind10/stone60/air60/acid20/electro20/teleport80/terra20/fire5, each modulo equal1. Corresponding ordinary payload helper files summon existing elemental carriers with owner/team as source states. This is much more than simply random-casting nine items.

Upgraded tick lasts400, omits ordinary solid-block retirement, rises0.04 and moves0.1 away in horizontal owner direction. Cadences ruby10/blind5/stone40/air10/acid5/electro5/teleport5/terra every tick/fire5. Every element rotates models every tick and invokes a cold-counter cycle of ordinary, Academy and awakened helpers, sometimes as owner instead of display. Dependencies include Avarice/Consuming/Silence, Razorterra/true terra/Fevgalea Star, Ultra Electro, Exahoming/Exasperato, Acidacid/Acidcorrupt, Stonekivi, Boltbolt, Fireblaze and Psychoblind/Psychovex. Must inspect each context-sensitive producer before reuse: item admission/spend methods are not interchangeable with source payload helpers.

Ruby upgraded helper copies counter, increments, copies again, increments again before modulo10: starting0 produces indices1,3,5,7,9, with optional additional increment after8. This source bug excludes even indices under usual initial state. Blind upgraded helper contains a condition without run (previous audit). Preserve or fix requires deliberate decision.

Full ordinary upgrade trial is meditate/start setting620 ticks, then tick checks held mainhand upgradable Sauvojen and decrements timer. tick_ok stages: ruby620; terra550..600; electro520/500/480/460/440 and final420; moving air383..419; stone320; acid300 and200..290; levitation160; darkness140, fear40..140 with random delays40..120; fire20; ready1. It has its own summoned helpers and stop/fail/damage graph. Fire is exact destructive mask: fill(-15,0,-15)..(15,-20,15) AIR strict; fill(-15,-21,-15)..(15,-50,15) LAVA; plane(-60,-21,-60)..(60,-21,60) LAVA. Stone places stone_cylinder at(-23,-4,-23). Need decode actual structure and agree current bounded terrain behavior before qualification; merely enum promotion would be incomplete.

## Other remaining dependencies

Unstable book random_spell rolls1..55 and delegates these systems, including still missing Hedgehog and combined Sauvojen. Progress check tracks only53 and Academy selector cycles53 while announcing54/55; this needs a clear intended contract. Cannot claim full implementation before dependencies are complete.

Hedgehog remains the full helper/wall/spike/god-boss family documented in [SHEEP_HEDGEHOG_SOURCE.md](SHEEP_HEDGEHOG_SOURCE.md); no narrow wand-only approximation qualifies.

Paper/Ignite search for libspell/grimoire/Sauvojen finds extensive dungeon/boss native overrides in Paper SwrgNativeService; those are separate dungeon behavior and not evidence of a registered159 consumed handler. No Java consumed159 override was established in this targeted search. Full global extension audit was not repeated.
