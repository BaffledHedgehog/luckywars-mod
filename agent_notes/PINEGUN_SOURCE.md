# Razor Pine and Razor Terra source audit

Read-only audit of the old disabled LBC datapack and current Terra implementation. Source presence does not prove a live old-server runtime. The base weapon is `razorpine`; the upgraded recipe result is `razorterra` (not merely the base weapon with a new model).

This audit predates the ordinary weapon port. Current implementation and
verification: [PINEGUN_PORT.md](PINEGUN_PORT.md). The later exact source audit
corrects two projectile details below: best ammo fires two arrows per volley,
and `killedarrow` is removed only on the grounded `prein` path, not during
flight. It also confirms that legacy `_left` function names refer to the
offhand, not the left mouse button. See [PINEGUN_RUNTIME_SOURCE.md](PINEGUN_RUNTIME_SOURCE.md).

## Base Razor Pine provider and recipe donor

- Old `data/lbc/loot_table/razorpine_with_leaves.json` creates one carrot-on-a-stick with model `lbc:razorpine`, tier A, existing translated name/lore (`razorpine`, `razorpine.lore.2/.3/.4/.1`), custom data `craftusable,transformable,shooter,magicwand,razorpine`; a second pool gives 16 wheat seeds.
- `data/lbc/loot_table/transformables_shooter.json` contains the weighted `razorpine_with_leaves` result. No separate direct loot invocation/provider was found in the old LBC source. The weapon is a confirmed donor in the Terra Razor craft, not a confirmed natural drop.
- `data/minecraft/tags/item/razor_ammo_best.json`: spruce leaves and saplings. `razor_ammo.json`: broad foliage/seeds/flowers and related items. Use only when retaining base Razor Pine combat; upgraded Razor Terra's attack below has no ammo check.
- Craft source: `data/lbc/function/other/craft_ground.mcfunction`, `other/craft/inventory_craft_check.mcfunction`, `other/sauvojen_tuntija/crafted_razorterra*.mcfunction`; output `data/lbc/loot_table/tuntijaa_terra_razor.json` has model `lbc:terra_razorpine`, custom flag `razorterra`. The current project records `stterra + razorpine -> razorterra` in [TERRA_RECIPES_SOURCE.md](TERRA_RECIPES_SOURCE.md).

## Base weapon controls and projectile baseline

- Base weapon activation: `items/changed_inventory/sel.mcfunction` sets `razorpine_active`; `players_2t.mcfunction` calls `other/workingitems/razorpine2t.mcfunction` while active. Mainhand RMB uses `workingitems/spell1_default.mcfunction`; offhand RMB uses `workingitems/spell1_default_left.mcfunction` when the left-hand-wands setting permits it. Both invoke `razorpine_ammo_check.mcfunction` only under their item/cooldown guards. These are hand routes, not separate mouse-button routes.
- Ammo check consumes one best-ammo item first, otherwise one regular-ammo item; it sets the corresponding `coldown18b` or `coldown18` score to 20. No mana cost is present.
- The repeat wrapper checks `matches -2..`, meaning score >= -2, then decrements the score by 4. Starting at 20, the check matches scores 20, 16, 12, 8, 4, and 0; each invocation fires a three-arrow volley before subtracting 4. The next score is -4 and no longer matches. Thus one ammo unit primes six volleys (18 arrows), not one volley. Neither shot function contains a cooldown guard or reset.
- Each volley is three vanilla arrows from eye-relative position with copied owner UUID and random per-axis velocity offset (`-300..300`). `razorpine.mcfunction` uses base velocity scale 2 and `damage:0.5d`; `razorpine2.mcfunction` uses scale 5 and `damage:1.5d`. The respective ammo scores select the two paths. Both set `killedarrow`; `projectilestick.mcfunction` sends arrows through `projectiles/pre.mcfunction`, which removes that tag next projectile tick. Thus native arrow motion, collision, damage, and vanilla shield blocking apply briefly; no pine-specific hit hook was found.

## Actual upgraded Razor Terra attack

- Mainhand RMB is direct: `spell1_default.mcfunction` line 57 invokes `workingitems/awaken_tuntija_wands/terra/razorterra.mcfunction` when the selected item has `razorterra:1`. Offhand RMB reaches the same function through `spell1_default_left.mcfunction` line 47 when the left-hand-wands setting permits it. Both gate on a nearby `stopper_magic`; neither gate checks mana or consumes ammo. The `_left` path is offhand dispatch, not a left-mouse trigger.
- `razorterra.mcfunction` summons a `marker` with `data.lifetime:10`, tags `fast_terra,rare,slowraycast,magic`, copies player rotation, applies random yaw and pitch offsets of ±3 degrees in `razorterra_rand_rot.mcfunction`, copies `lbcID2` and `team_number`, then activates it.
- `raycastselector_rare.mcfunction` dispatches `fast_terra` to `lbcr:raycast_terra_fast`. That function runs once per server tick through `tick_not_timestopped` → `other/areas` → `raycastselector`; it advances 0.3 blocks per recursive call over positions 0 through 7.5 blocks (26 samples), emits green dust, and calls `dmg_bypl_11` when a mob is in the current block/within 15-block selector distance. `dmg_bypl_11` applies 17 cactus damage through `dmgwithid_un_playeronly`, using the shooter lookup marker, while excluding same-team players. On non-air collision it calls `placeflowers`, which places several flower/grass features then kills the ray marker. Otherwise it ends after the 26th sample. The spawn sets `data.lifetime:10`, but does not set `marker_2t`; only `tick2t.mcfunction` advances Lifetime for `marker_2t` raycasters, and `raycastselector` only checks Lifetime without incrementing it. Thus no functioning time-to-live is evidenced for this marker; the configured 10 value does not expire it. No `shield_blockable` tag is set on this Razor Terra marker, so the old source has no explicit shield interception. The requested target behavior is to have shields block Razor Terra like other supported projectiles; treat that as a new requirement rather than source parity.
- It has no vanilla arrow/entity projectile: movement is a per-tick raycast marker. Range, damage, collision, and owner attribution come from those raycast/damage helpers. Don't transfer the base Razor Pine's ammo or arrow semantics to this upgraded attack.

## Reuse and contract difference in current Terra code

- `src/main/java/me/skywars2/game/LbcTerra.java` already renders `Kind.TRUE_BEAM` as `lbc:terrasword_true_beam` and supplies the shared display path: 1-tick interpolation/teleport, visual lead, native entity hitboxes, shield check, owner-attributed damage, terminal display detach and two-tick retirement.
- The existing `TRUE_BEAM` gameplay contract differs from old `razorterra`: speed 1.32, damage 5, 50-tick max lifetime, three-line beam hitbox/particles, wall and active-shield termination. Old `razorterra` is a randomized ±3° 7.5-block raycast with 17 cactus damage and flower placement at walls; configured `data.lifetime:10` is ineffective in the observed call graph because the marker lacks `marker_2t` and its Lifetime score never advances. No mana/ammo cost. Only reuse shared model/display/native entity machinery as appropriate; the beam parameters and hit geometry do not reproduce the old upgraded gun.

## Clarification for the implementation contract

The implementation preserves the upgraded cast's free cost, three-degree spread,
7.5-block movement per working tick and 17 damage, with the requested beam model.
Shield blocking is required by the user's clarification. Lifetime choice (user):

> 10 тиков — исправить отсчёт по значению исходника, до 75 блоков

Thus the ineffective old marker lifetime is repaired, not carried over as an
unbounded projectile. Current implementation/testing: [TERRA_CRAFTING_PORT.md](TERRA_CRAFTING_PORT.md).




