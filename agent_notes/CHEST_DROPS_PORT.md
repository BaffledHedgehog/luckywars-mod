# Five native loot-chest drops — 22 October 2026

## Author decision (user)

Question: when a drop-created chest replaces an existing container, what should
happen to its contents?

> Выбрасывать прежнее содержимое предметами (рекомендую)

This supersedes any source command's incidental content-loss behavior. Materialize
an old lazy loot table once, eject only the replaced block entity's inventory,
and retain complete item components. A double chest's other half stays intact.

## Implemented scope

Five separately testable Polymer items reuse the ordinary source token design.
They activate when dropped during an active arena match; holding or using them
does not place a chest. Existing flag-bearing legacy stacks use the same path.
No LuckyWars mode or broad unqualified loot pool is enabled by this batch.

| Native item / ported loot suffix | Source flag | Lazy vanilla chest loot table |
| --- | --- | --- |
| chest_drop_bastion | bastion | minecraft:chests/bastion_treasure |
| chest_drop_end_city | end_city | minecraft:chests/end_city_treasure |
| chest_drop_treasure | treasure | minecraft:chests/buried_treasure |
| chest_drop_village | village | minecraft:chests/village/village_weaponsmith |
| chest_drop_bonus | bonus_chest | minecraft:chests/spawn_bonus_chest |

Each native ID uses namespace `skywars2`; each test table uses `lbc:ported/`.
For example: `/loot give @s loot lbc:ported/chest_drop_bastion`, then drop the
item in a match. The registered item and loot result have identical components.
Names are the original yellow nonitalic drop_bastion/drop_end_city/drop_treasure/
drop_village/drop_bonus; lore is one white nonitalic trap_desc row. Existing
model lbc:drop_not_trap and tooltip lbc:tier_d are retained. PAPER is only the
Polymer carrier; the legacy command-block costume has no placement behavior.
No new resource-pack assets or template changes are required.

Native /setblock-style chest shape resolution is retained, including compatible
adjacent chests. Placement floors the actual item coordinates, with no nearest
player dependency or artificial X/Z bound. The new vanilla loot table and seed0
remain unresolved until ordinary native opening or later container replacement.
Same-state chest replacement uses a fresh block entity, clearing old inventory,
name, lock, components and seed before installing the new table.

## Scheduling and reuse

`LbcChestDropItem` follows the existing Helicopter Polymer prototype/component
event pattern. `LbcChestDrops` indexes dropped inputs through entity load/unload
events, defers effects outside ENTITY_LOAD, ignores native /give fake pickup
visuals, and captures the match epoch. It performs one attempt and one unit debit
per token per server tick. Matching source flags dispatch in source order, with
one final debit even if an executed placement fails (the source policy).

Per-dimension time stop and unloaded target/neighbour chunks defer without debit.
Only unique resident chunks in the one-block shape border are queried; no chunk
is loaded for an effect. Stop, unload and stale epochs cancel pending work.
The token is nonmagical; no antimagic or mechanism-beacon restriction is added.
There is no inventory scan, command-string execution, eager loot roll per tick,
or off-thread world mutation. Native BlockInput, Containers.dropContents and
RandomizableContainer APIs own placement, inventory splitting and loot context;
normal arena mutation hooks retain next-map cleanup coverage.

## Verification — qualified

Build and two native isolated matches passed on SHA256
1b6710c5df7b387ad8da5a2dfe14bb1f4beb893835493465e6f8b6077ae7ac02.
The runner completed12 suite results in52.43 seconds: chest drops, prior
Helicopter encounter, both services' stop probes and real match cleanup.
Lobby bot/catalog checks also passed. `ChestDropChecks` verifies all five exact
prototype/ported-loot/native-give payloads, held/fake items, stack3 progression,
concurrent inputs, real vanilla opening, per-dimension time stop, unload
cancellation, failed-placement debit, exact7-diamond old inventory ejection,
same-seed native old-lazy-loot bundle equality and adjacent13-diamond retention.

Archive: build/verification/skywars2-chest-drops-1b6710c5.jar.
Evidence: build/verification/chest-drops-final/ (build/native/server logs,
results.json, qualification.json). All140 current Java files match the sources
JAR;2648 processed resource files match the release, with Loom-transformed
Fabric metadata and bundled jars checked separately. CRC passed. Isolated
runner locks/world lock are released and saved test configuration restored.
Main JAR remains69de8601, not installed; original RP/template unchanged.

This is native fake-player gameplay qualification, not a new real-client visual
inspection. The existing Polymer missing chunk-packet-context diagnostic on bot
join remains present, as in the previous qualified runs; no fix is claimed here.

## Sources and next work

Exact legacy dispatch, payloads and active resource-pack references are in
[NEXT_SPECIALDROPS_SOURCE.md](NEXT_SPECIALDROPS_SOURCE.md). The ordinary source
variants are ported; the different undecorated imba pool is not published as a
new ready pool. This does not qualify the entire Lucky Block outcome dispatcher.

Next bounded unit: the 13-TNT drop, then harmful creeper/cage outcomes with source
attribution and shared explosion/target policies. Crocodildo is separately audited
in [CROCODILDO_ENCOUNTER_SOURCE.md](CROCODILDO_ENCOUNTER_SOURCE.md): initial HP,
destructive payload, time-stop/suppression and missing native nuke-egg reward
need resolution before implementation. Muravei still needs NPC acid ownership
and reinforcement behavior audited; do not silently reimplement its acid physics.
