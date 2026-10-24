# Tesla staff core: ordinary and Academy

Native `lightningwand` is distinct from Lightning Bolts (`lightning_bolts`) and
Boltbolt. [Resource audit](LIGHTNING_STAFF_RESOURCES.md) records exact source
presentation, acquisition and existing author RP assets. The verification section records the tested artifact.

## User rulings

- Tesla chain: "Сохранить защиту союзных игроков" (user, 24 October 2026).
  Caster/allied players remain protected in direct damage and ring selection.
  Friendly mobs remain physically hittable; homing still chooses only enemies.
- Trial: "так урон от проводимости и так всем наносится без фильтров, какая
  разница? ну давай то что будет быстрее по производительности" (user).
  Count accepted conduction returns in its existing damage loop. No extra entity
  query, pre-count pass or per-target scoreboard operation. Ordinary targets
  include caster and allies; protected technical roles/observers remain excluded.
- Existing policy: wand use consumes mana and is blocked by antimagic. Released
  electricity is nonmagical. Direct electric arcs retain knockback and native
  bypass cooldown; conduction retains velocity and ignores friendship.

## Source mechanics

Old source root is read-only:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Relevant `lbcr/function/` files: `raycast_preloop10{,cd,_wtot}.mcfunction`,
`raycast_preloop10cd_upgraded{,_pre,_mode_chain,_mode_instant}.mcfunction`,
`raycast_preloop10cd_upgraded_mode_instant_loop{,_pre,_razbros,_razbros2}.mcfunction`,
`raycast_molnia{,_chain,_chain_end,_chain_end_check}.mcfunction`,
`raycast_electro_electroprovod{,_damage,_check_upgrade}.mcfunction`.
Chain scheduling/age comes from `lbc/function/raycasters_5t.mcfunction`.

Price is configured `lightning_wand_mana_use`1200. No separate cooldown. The
actual selected hand participates in the existing two-hand dispatcher and pays
independently. Ordinary and Academy default fire four arcs, lifetimes30/35/40/45,
offset by0/.01/.02/.03 behind the eye. Ordinary offhand starts0.5 blocks forward;
Academy offhand retains the eye origin. Existing Electro's2-block/tick flight,
random yaw/pitch±8° refraction, sampled0.5 direct damage, conductivity, native
shield clipping, reflection/Curse ownership and Time Stop/lifecycle are reused.
No new projectile physics, marker entities or global world scan.

Academy Shift+PKM cycles default/instant/chain for free, including negative mana.
Chain emits one carrier for150 real ticks, moving4 blocks every5ticks. It turns
toward the nearest eligible target in5..10 only when no eligible target is
within5; caster/allied players are excluded (user). Friendly mobs remain hittable
but excluded from homing under the existing enemy-only targeting policy.
It keeps the source terminal end-rod particles and three laser sound calls.
Existing conductivity remains WILD, independently of direct targeting.

Instant emits five independent finite rays,401 quarter-block samples each,
starting from the original eye/look. The source5% random±3° bends remain.
Native damage is0.5 per intersecting sample, one target per sample. Local state
replaces stale global Distance and source self-attributed damage. Native electric
damage inherits the actual caster and shared armor/electric/knockback policies.
Source white/purple particles and refraction sounds remain. A non-air block
ends that ray; only water invokes conduction in this mode, as in the source.

The instant work reuses the existing cooperative Stonekivi queue and3ms budget.
One work unit processes up to eight consecutive samples; bends split segments.
Each segment uses one native AABB query and `LbcDamage.hurtSegment` rather than
one selector per sample. Endpoints are disjoint across work units. No capacity
discard; unloaded chunks stop the ray without loading more terrain. Epoch,
participant/death and Time Stop guards apply; match stop cancels the shared queue.

## Academy and presentation

Real Academy admission uses the existing `upgradable` state. A conduction pulse
with at least ten accepted targets marks the actual casting stack/hand ready;
reflection cannot grant the original trial to a different caster. Returning
the ready item to the normal main-hand Academy interaction produces native
`academy_tesla_wand`, tooltip A and one added green `upgraded_item` line.
Ordinary scoped leaf uses the canonical staff-pool B presentation. The distinct
source-of-energy tier C variant is not rewritten or enabled by this batch.
Original model/name/eleven lore lines and existing RU/EN are preserved; no RP or
template changes are required.

Ready scoped leaves after qualification:
`lbc:ported/tesla_wand`, `lbc:ported/academy_tesla_wand`.
Broad staff/LuckyWars pools remain gated. The Sosueneruga continuation craft and
special mushroom-stem/`mom` Yellow Lightning encounter/reward chain are separate
unported dependencies, as previously recorded in [Electro](WANDS_ELECTRO_PORT.md).
This batch qualifies Tesla's ordinary combat and three Academy modes, not those
rare encounter rewards or the complete acquisition pools.

## Verification

Qualified build `8040ad37`: 28 recorded suite results, two actual mode1 matches (114.85s). Tests cover original presentation, native both-hand PKM and exact cost/ordinary offhand origin, direct hits/friendly mobs/allied-player protection, shared shield clipping/reflection, WILD conductor credit including caster/allied player with team friendlyFire=false, chain enemy ring/cadence/lifetime, free negative-mana changes, actual queued five-beam damage/Time Stop, nine versus ten accepted conductor targets, actual offhand trial/A promotion and real match-stop cancellation. AwakenedAcid, full Gaycaster meditation/palette, Prism, CausticOmega, AdvancedSnow, Neutral, Mobgun, Academy and KFC regressions passed. Exact 285 sources / 763 classes / 2772 resources verified. Installed stopped main with rollback `322f2e06`; server not started. Evidence: `build/verification/tesla-final/`. Native fixtures include manual boundary ticks; no client particles/pixels, actual disk restart/unload/disconnect, rare Mom chain or peak-load performance claim.
