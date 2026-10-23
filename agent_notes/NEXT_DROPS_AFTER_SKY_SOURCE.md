# After sky hazards - remaining drop source audit

23 October 2026. Source audit, not runtime qualification. Legacy S =
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`;
Paper P = `E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java`.
Read current root/agent_notes AGENTS and NEXT_DROPS_AFTER_PORTAL_SOURCE.md.
Pisa, Antigravity, altitude hazards and Death Lust are already ported; do not reopen.
No honest group of 2-4 small complete remaining mechanics was established.
Ultraponos is now implemented and qualified as one substantive closed producer;
see [current contract](ULTRAPONOS_PORT.md) for author refinements and runtime
evidence. The source/Paper distinctions below are the audit, not the final
target when superseded by the user's one-item-per-tick/upward-stream requests.
Keep broad acquisition pools gated.

## Ultraponos - exact complete chain

S `function/drops/specialdrops.mcfunction:65` dispatches `{ultraponos:1,drop:1}`
to `drops/ponos`. Normal token `loot_table/traps.json:1095-1138`:
repeating_command_block/count1, weight10 quality10, tier_ff, drop_trap,
yellow nonitalic `trap_ponos`, white nonitalic `trap_desc`. No invented old leaf.

`drops/ponos.mcfunction:1-8` places `lbc:ponos` at token XYZ with no rotation/
mirror; global `minecraft:lbcsounds.ponos` master volume1 pitch1. The actual
`structure/ponos.nbt` exists (430 bytes, DataVersion4189), decoded read-only:
size[1,3,2], no entities, exactly six blocks. Polished deepslate at(0,0,1) and
(0,1,1); bottom/open/powered/north/nonwaterlogged iron trapdoor(0,1,0);
explicit air(0,2,0); floor/powered/east lever(0,2,1); south/enabled hopper(0,0,0),
TransferCooldown0, slots0..4 cocoa_beans64, nether_brick64, melon_seeds64,
frogspawn64, brown_dye64. Ordinary native inventory/items, not custom rewards.
P `spawnPonosStructure:6081-6099` constructs the same six blocks/inventory.
Preserve actual template placement/replacement/container behavior; ordering and
neighbor physics need implementation verification, not inferred equivalence.

Base four creepers are at block-aligned X/Z+.5 and absolute relative Y
-1.5, -2.5, +.5, +1.5; radii6,7,8,9 respectively. All invisible288ticks,
NoAI/NoGravity/Silent/Invulnerable/PersistenceRequired, ignited, Fuse290,
tag govno_creeper, yellow/nonitalic translated megadrist (not visible-name flag).
P `spawnUltraponosCreeper:6101-6117` retains these native values; P caller
`runNativeUltraponosDrop:5441-5460` handles placement/debit.

Emitter source marker is aligned center Y+1.5, lifetime280,
ultraponos/rc_5t/can_deleted/mecha. `raycasters_5t.mcfunction:9` calls
`drops/ultraponos:1-3`: count item ENTITIES with govnoq1 within spherical15,
not total stack count. If count<=200, call `ultraponos_2:1-6`: brown_wool block
particles at markerY+1.5, delta(.3,1,.3),speed5,count300,normal; five independent
`lbc:govno` loot spawns at marker-relative Y0,.5,1,1.5,2. The threshold is an
AUTHORED per-pulse condition, not a new hard cap; one pulse may exceed200.
P `startUltraponosEmitter:6119-6149` instead uses fixed centerY+1.5, 56passes,
first after5ticks/period5; five loot origins reduce Y by1.5 (base through base+2),
and particles at fixed centerY+1.5. P count helper:6151-6167. This is an additional
source/Paper height difference, not merely visual noise.

## Decisions required before claiming fidelity

- Skill9 enabled source `ponos:3` calls `ponos_mega:1-4` (radii12,14,16,18),
  THEN still spawns all four base creepers: EIGHT total. P:5451-5455 replaces
  base radii with enlarged radii: FOUR total. User chose EIGHT total (23 October2026); implemented as additional stronger four then base four.
- Source particles occur only when count<=200; P particles occur every pulse.
- Source loot origins/particle position are above the marker as described;
  P moves loot down1.5 and particles down1.5 relative to source. Choose one
  authoritative version explicitly; don't silently mix them.
- Source marker phase/lifetime pass ordering versus Paper exact56passes needs
  intentional timing selection. Structure native update order also needs checks.

## Reward closure

S `loot_table/govno.json` has one roll, four equal weight1 entries: cocoa_beans,
brown_dye, nether_brick, or `govno_eaten`. First three count1/max_stack_size1,
tier_ff, nonitalic names shit_1/2/3, flags craftusable/govnoq/randomgovno1.
Fourth is count1 cookie, tier_f/max_stack_size1/model lbc:shit/nonitalic shit_4,
flags craftusable/govnoq/randomgovno/eat_shit1. These are native items and an
already implemented native food, not new custom abilities.
`LbcKfcFood.java:75-79,100-113` supplies skywars2:govno_eaten with real cookie
food/consumable and existing self-reproducing eat_shit behavior.
`LbcCraftingRecipe.java` accepts govnoq donors; LbcDamage recognizes govnoq.
No reward may be replaced by bare cocoa/cookie without these components.

Per root follow-up, prepared only `LbcUltraponosDropItem.java` and
`ported/ultraponos_drop.json`. Reuse existing internal `lbc:govno` and
`lbc:govno_eaten`: build.gradle already includes both plus govno_lot/lot_2.
Read current imported JSON: source four equal entries/all source components
are preserved in current26.3 modifier schema, with food redirected to real
skywars2:govno_eaten. No duplicate reward table is necessary. JSON parses;
registration, resource readiness, service/dispatcher/build configuration and
runtime qualification belong to root. No shared code, build, server,
source-server or RP/template modifications here.

## Other searched remaining mechanics - defer

Forest S `drops/forest` -> marker lifetime800/rc_5t -> `forest_loop:1-17`:
12-way tree feature/random baby zombie chain, roll7 persistent30HP reactive_shish
with jump6/fire resistance, no helmet drop. P:6391-6483 uses160steps/5ticks,
TreeType approximations (not exact configured-feature equivalence).
`loot_table/reactive_shish.json` guarantees reactive_shish_item AND 5% chance
end_of_technomagic. The latter is snowball gadget shish_of_everything1,
model lbc:shish_of_everything/tier_k. No corresponding native ability support
was found. Existing reactive_shish recognition alone does not close this drop.

Magic Bean `drops/magicbean` -> scheduled magicbean_loop -> bean -> beanend
creates a strict irregular white-glass platform and chest lbc:lucky_items_1.
That broad unclosed reward pool prevents declaring the short setup closed.
Infinity `drops/infinity` creates immortal glowing no-gravity infinity_rand
instant token, and invokes nearby savoujen_cloud drop_wand continuations;
P:4858-4878,9220+ mirrors the new token/cloud reward chain. These are not
reward-free effects. Prior broad-pool/normal Bob/Greed/TMT exclusions remain.
