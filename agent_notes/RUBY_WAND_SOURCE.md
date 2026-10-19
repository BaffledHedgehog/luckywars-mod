# Ruby staff source notes

Scope: ordinary and Academy `struby` staff only. The tiny Ruby wand is a separate item and is omitted.

## Behavior in the datapack

- Base activation costs **2,000 mana** (`config.mcfunction`); `ruby.mcfunction` and `ruby_upgraded.mcfunction` both subtract the same configured value. No per-tick mana cost is shown. Both invoke the four-piece mage armor bonus and the common wand trinket cast hook.
- The ordinary staff directly calls `ruby_preloop`: it creates one `raycastruby` marker at the cast position, copies caster rotation, tags it active, and assigns a 70-tick lifetime. The shared selector routes it to `lbcr:raycast_ruby`. In one handler invocation, `raycast_ruby` performs six 0.3-block moves (Distance 1–6), each with a destroy-fill over a 1.2-block cube (`0.6` each side), then routes Distance 7 to `raycast_end_with_tp`; that function resets Distance and teleports the marker in place, not deleting it. The handler recurs synchronously, so it covers 1.8 blocks per tick, not 2.1 blocks total. The source establishes a 70-tick marker lifetime; effective total travel depends on that lifetime and dispatcher behavior and is not inferred here.
- Academy staff cycles three modes on sneak-use (mainhand and offhand have separate functions): mode 1 “Projectile” runs the ordinary charge; mode 2 “Squeeze” invokes `ruby_hole`; mode 3 “Ray” invokes `ruby_ray`. `ruby_preloop_2` reads mode from the actual used hand and starts the selected mode.
- Squeeze advances **0.5 blocks per iteration** for at most Distance 200 (nominal 100-block path). It triggers upon encountering a non-air block or a non-spectator, non-caster mob, then places the `air_sphere_7x7x7` template centered on the endpoint and sets the endpoint to bedrock. The nearby-mob selector is evaluated at that impact point: eligible mobs within distance 3 are teleported to the impact point, then those without `void_resistance` take **1 outside-border damage** there. Marker Distance is set to 1000 to stop the loop. Because the loop selector may be evaluated again after entities move, preserve its source guards when porting.
- Ray advances **0.2 blocks per iteration** up to Distance 300 (nominal 60-block path). Each iteration destroys blocks in a `0.2` cube (`fill ~.1 ... ~-.1 ... air destroy`), so this is a destructive mining beam, not a damage beam in the inspected function.
- Academy Squeeze trial: while sneaking and casting an item tagged `upgradable`, `ruby_preloop` kills its just-created charge, places `air_sphere_11x11x11` with `0.96` placement integrity, fills the caster's position through one block up with bedrock, teleports entities within 5.5 blocks to the caster, and deals **6 outside-border damage** to the caster unless `void_resistance`; then it calls generic `can_upgrade`, which makes the item upgrade-ready unconditionally. English text describes compressing the staff's energy via Shift+RMB; nearby creatures are drawn in and the caster takes void damage. Actual Academy upgrade swaps mainhand item to tier B (`upgrade/struby`), while the Academy advancement is granted by inventory detection of upgraded staff.

## Sources

- Main behavior: `world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/ruby*.mcfunction`
- Shared charge handler: `world/datapacksdisabled/LBC/data/lbcr/function/raycast_ruby.mcfunction`
- Routing: `lbc/function/raycastselector_tuntija.mcfunction`
- Cost: `lbc/function/config.mcfunction`; staff item data/cost lore: `lbc/loot_table/sauvojen_tuntija_swargia.json`
- Academy interaction: `lbc/function/other/magic_academy/interacted_2.mcfunction`, `can_upgrade/struby.mcfunction`, `upgrade/struby.mcfunction`, and `change_mode_3{,_left}.mcfunction`
- Academy trial implementation: `ruby_preloop.mcfunction` and `ruby_preloop_check_upgrade.mcfunction`; player-facing text: `plugins/luckywars/resourcepack_lang/en_us.json` (`upgrade_condition_struby`)
- Academy unlocked-item predicate: `lbc/advancement/true_advancements/legends/academy/ruby.json`

## Author decisions and migration boundary

- Trial (user): preserve the sphere, bedrock, six self-damage and immediate readiness, but also pull teammates. Observer/NPC role guards remain active. The actual used hand is checked, following the established offhand correction.
- Charge lifetime (user): use smart removal by solid blocks and live targets, rather than the old 70-tick marker lifetime. This is wired to the shared ProjectileLifetimes with terrain=true.
- Ray mode has no entity damage in its source function; the port retains this mining-only behavior.
- Squeeze damage and its created trap preserve the caster's ownership. Its ordinary target guard excludes allies, observers and service NPCs; only meditation pulling receives the explicitly approved ally exception.

For integration, existing project owners are `LbcWands` for wand input, mana and common target/hit helpers; `LbcAcademy` for trials/upgrades; `LbcItems` for tick/lifecycle/cleanup; use `LbcWands.firstHit` and shared `ProjectileLifetimes` where appropriate. Ruby already has an intentional whole-line destruction requirement in `ARCHITECTURE.md`; don't implement it as a first-hit-only beam.
