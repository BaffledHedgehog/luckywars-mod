# Reroll Machine native crafting

## Author decision (user)

> Сохранить списание с уходом в минус

Keep the source price of 5000 mana for each transformed donor. Zero and negative
balances do not block the operation. The source item lore already describes this
behavior; no wand affordability or full armor set exception is involved.

## Qualified native contract

The Reroll Machine is a native Polymer item, skywars2:reroll_machine. Its paper
proxy uses the existing lbc:reroll_machine model. A command block was only the
datapack carrier; this tool cannot place one. Preserve source lbckit.24 title,
both gray nonitalic lore lines and reroll_machine/craftusable flags. The native
ITEM_MODEL default is applied through Fabric DefaultItemComponentEvents after
vanilla property finalization, which otherwise overwrites the requested model. This was confirmed from the
local Minecraft 26.3 Item.Properties finalizer and Fabric item API
14.7.0+cf6bc2db5d sources matching the installed Fabric API 0.161.0+26.3.
Native give,
lbc:kit/reroll_machine and lbc:ported/reroll_machine use the same components.

Kit 25 is selectable on page 9100, slot 0, with the source Academy Lightning
achievement. Existing locked GUI design and ranked access rules are reused.
The catalog grants the native machine at start/respawn through the existing
LbcLoadouts path; no separate kit/inventory polling is added.

Both inventory 2x2 and workbench 3x3 accept exactly one occupied catalyst slot
and supported transformable donors in the other occupied slots. One committed
take transforms one unit per donor slot: up to three/eight donors. Unrelated
extras, unmapped donors and multiple catalyst slots reject the recipe. The
machine is always retained. Each empty donor roll preserves that donor without
charging mana; successful other slots still commit. All-empty rolls change
neither inputs nor mana. These preservation rules follow the existing native
no-result policy rather than legacy destructive failures.

LbcReforgeCrafting owns a single pre-transfer click/commit/delivery path shared
with Tiny Teleport. A stable opaque preview never rolls loot. Valid pickup,
quick move, empty hotbar/offhand swap and throw commit one operation. All reward
stacks are delivered, with guaranteed ground overflow; no capacity refusal is
possible after seeing a random result. Unsupported actions cannot acquire the
preview. Creative uses the same costs and consumption. Existing fixed-result
recipes and Tiny pair precedence remain.

Reroll reuses the normal Reforge category mapper and the shared LbcBolts roller,
with player THIS_ENTITY and source origin one block above the player. Tiny keeps
its detached donor ItemEntity context and Ultra tables. Independent matching
categories generate their full bundles for one consumed unit. Successful Reroll
awards lightning_smith; Tiny keeps lightning_smith_ultra.

No new magic/mechanism suppression behavior is invented. Existing match, role,
dimension, menu validity, input snapshot, epoch and actual-world time-stop guards
precede random work. The transaction runs on the server thread, only on result
interaction. Recipe matching checks catalyst presence before constructing donor
category maps, so ordinary crafting avoids those allocations.

## Source and translation handoff

[Source audit](REROLL_MACHINE_SOURCE.md) records the old per-tick player-grid
implementation, exact mapper order, price, player context and kit gate. This
native result-taking route follows the established ordinary crafting/forced
randomization requirements and extends it to workbenches.

Reuse existing source item translations without copying them into the template.
Merge only the new skywars2.crafting.reroll_machine.lore preview key per locale.
An English fallback exists before merging. No original resource pack changes.

## Evidence

RerollMachineChecks uses real result clicks, SGui network clicks, native loot
context predicates, negative mana, multi-donor batches, empty tables, overflow,
Polymer encoding, source component equality and kit application. The isolated
runner also covers the existing Tiny/fixed crafting routes and full lightning/
Infbolt acquisition regression on the final artifact. Final build and four native matches passed in 102.93 seconds: two crafting/kit
matches and two full ambient lightning/Infbolt regression matches, on SHA256
b60d496938a42347ed498315b6b6d971e8b96e93aec77751996e7ba87046d6c4.
The runner switches temporary loot resources only after a clean isolated-server
stop. It does not wait for a new paste when the same map is already ready.
The overflow recapture fixture checks only transformable rewards; ordinary Razor
seeds retain native lightning damage. No general item immunity was added.

Ready archive: build/verification/skywars2-reroll-machine-b60d4969.jar.
Evidence: build/verification/reroll-machine-final/ contains build/native logs,
results and qualification. CRC, 2642 processed resources and Fabric/SGUI metadata
verified. The isolated server is stopped and its world lock released. Main JAR
still has SHA prefix 332415d6 and was not replaced. Original resource pack is
unchanged; one new preview lore key per template locale. No graphical client or
stress-test claim. Other direct-give item prototypes were not audited by this fix.

Next bounded source review: [LuckyWars luck/config owner](LUCKYWARS_LUCK_SOURCE.md).
Its normal-mode applicability/reset policy remains a separate author decision;
this batch does not enable unfinished game modes or broader loot pools.
