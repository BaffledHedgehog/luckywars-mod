# Ready TMT acquisition, actual dropped tokens and gallery regeneration

Component verified, **not installed**. Full TMT goal remains active. Complete parent
is `tmt-firework-ammo-2026-11-07/component-verification.json` (`92762114`/`4db43bdd`).

Complete internal candidates:

- Test `823335942c882e2eb6c90fbcfdc38080ef12e71851487eaa5af3c9683e4ebbee`.
- Production `757cdfe061c2342ca548dd74af05ae8fa7c6f874860736f7bd273918e7c7c021`.

Ten explicit Java families/five resources plus build/generator admission changes.
Qualification: `build/verification/tmt-acquisition-2026-11-07/component-verification.json`.
All six suites pass on the same final artifact: tmt-acquisition, content-gallery,
tmt-catalog, uniform-item-pools, typed-uniform-item-pools, tmt-shared-drops.

The original `lbc:tmtrainer_drop` still had a raw repeating-command-block carrier
that the new shared dispatcher could not execute. Its entry now uses the registered
native token, retaining original weight/count/style/name/lore/custom-data wrappers.
The shared scheduler creates exactly one new generated item per physical token unit,
immediately while airborne. It keeps TimeStop, epoch, ownership and residency policy;
no MAG/TECH checks. Runtime.generate is the same factory used by the uniform single
TMT slot and the new native `skywars2:tmt` loot entry. No function interpreter.

Catalog acquisition exclusion now precedes dropped-ability classification. Otherwise
the newly functional token could enter its own random property catalog recursively.
The direct generated-item table is `lbc:ported/tmt`; the token table is public too.
For the future stable build:

```
/loot give @s loot lbc:ported/tmt
/loot give @s loot lbc:tmtrainer_drop
```

The latter returns a token to drop, following the existing token input. The former
generates immediately on every loot roll. No extra source item implementations.
Gallery traverses this native factory entry, displays its actual populated output
and retains its original LootTable for reroll. Bare TMT transport defaults remain
hidden. Native gallery proof rolls the preserved table with distinct deterministic
seeds and verifies fresh full properties instead of a copied preview stack.

Actual native token test drops two units through ServerPlayer.drop, holds them for
three TimeStop ticks, then observes one fresh output/unit on each real shared tick.
Neither token reaches the ground. Both native jammer fields are present. Output
true author/current epoch/private slots/procedural model+marker/native codec are
verified. The source's remaining unit is retained after the first activation.

Regeneration of canonical manifests also refreshes four formerly frozen transports
(Gayradar/Troll Box/Jack/Crocodildo) and adds the real authored nuclear egg profile.
The first six-suite candidate passed five suites, but full uniform admission found
the new steampunk rocket had no authored leaf or audited default. Its real registered
prototype is complete (flag/model/name/lore; no deferred dose/mode/payload), so it now
has one explicit complete-default admission. Final full uniform proof is complete=true:
all native gameplay IDs accounted, one TMT ticket, all raw custom/vanilla slots and
native count wrappers. Typed proof covers34 views/1061 draws/12 actual27-roll callers.
Manifest factory descriptor is unchanged; custom source slots1953->1954, with no
blocked source closures. Only known empty excluded reroll_9999/material_block remains
an informational parse finding. Fresh TMT catalog1001families/1883variants,124 findings.

10489 guarded inputs and281 author paths exact, three unrelated source WIP/HUD/RP/Main
unchanged. Sequential512MiB compiler then1280MiB fixture; normal exit/exact fixture and
ticket restoration/no retained locks. Main83b0ea27 unchanged; no installer, restart or
actual author-RP edit. Hold all changes until stable. Prepared atlas overlay is still
required in the author pack; no physical client rendering is claimed.

The component/class audit found a **real remaining gap**, not a generic audit label:
authored ENTITY_DATA on vanilla spawn eggs is currently excluded despite meaningful
spawn behavior. Examples are bat_spawn_egg__8c00247f684a and__5224815bd6c2 (Illusioner).
Use the existing LbcTmtBlockUses factory/projection for these, conditioned on actual
authored entity payload; don't invent another spawn engine or admit category-only
bare vanilla eggs. Then finish capability/service audit and stable integration.
