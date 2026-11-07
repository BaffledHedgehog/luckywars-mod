# TMT golden-apple input audit — 7 November 2026

Read-only source investigation; only this note was added. No Java/resource
changes, compiler, JVM, JDT/index, server or installation. `OLD` below means
`E:/servers_mew/cringe1_21_4_skywars`; `L` means
`OLD/world/datapacksdisabled/LBC/data/lbc`; `S` means
`OLD/world/datapacksdisabled/swarogskywars/data/swrg`.

**Finding:** ordinary TMT apple eating has a real USING self/charging boundary
and property-specific consumption callbacks. Neither boundary generically
dispatches spells/direct-use properties. Do not claim an Apple with a Clock USE
property casts it merely by eating. An initial-RMB spell adapter exists in the
Paper source, but its actual checked registration/configuration is disabled.

## Actual datapack producers and dispatch

- `L/function/drops/tmt_at.mcfunction:1-6` chooses five forms, including
  `set_gapple`; `set_gapple.mcfunction:1` creates golden_apple with food and a
  two-second consumable. `set_basics.mcfunction:1` supplies tmt_item and the
  common classification flags. Classification alone does not create input.
- `L/advancement/using_item.json:4,7,39` listens to actual minecraft:using_item
  for tmt_item (excluding Nexus/Imprinted), rewarding `lbc:items/tmt_item`.
  `function/items/tmt_item.mcfunction:1,3` calls trigger_smth then revokes the
  advancement. `function/drops/tmt/trigger_smth.mcfunction:1-4` marks tmticked,
  runs items/swords, items/armor and items/crossbow_charging. It never calls
  items/spells. Charging increments crossbow_charge (`items/crossbow_charging:1`).
- Direct generic spell input is a different producer: `load.mcfunction:8`
  defines RC_Raycast as minecraft.used:minecraft.carrot_on_a_stick;
  `players_tick.mcfunction:19` calls items/spells when positive. An apple's
  native eating does not award carrot-use. `players_tick:1` resets tmticked.
- Accepted bow/crossbow paths explicitly add spells: `items/bows:15` calls
  trigger_smth_not_bow, whose line6 calls items/spells; `items/crossbows:25`
  calls trigger_smth_not_crossbow, whose line6 calls items/spells. Their USING
  event alone is not this accepted-shot event.
- Entity interaction is separately real: `interaction_rightclickable.json:4,11,45`
  listens to interaction with an rc_able entity and calls items/spells. The
  concrete rc_able producer found is ice crystal spawn/final:1. This can be
  another external spell trigger; it does not make ordinary apple eating one.
- No generic tmt_item consume advancement exists in either namespace. LBC
  consume advancements select actual private food properties; for example
  `eat_book.json:4,8,41` invokes other/eatables/book, not generic spells.
  `S/advancement/core/food.json` invokes game/player/skills/food; that function
  grants skill4 effects and revokes its advancement, with no TMT/spell dispatch.
  Property-specific side effects remain distinct: eat_kfc_chick -> kfc_chick:8
  can select all_up_scores_no_mana, whose line6 increments RC_Raycast, causing
  that separate next player-tick dispatch. This is not a generic finish hook.

## Paper branch: implemented but conditionally unregistered

`OLD/plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java:69-76`
registers LegacyFunctionRuntime and SwrgNativeService only if legacy.enabled.
Both actual `plugins/luckywars/config.yml:3-4` and source resources/config.yml
set it false. Therefore the broad handler is not registered under the checked
configuration; no runtime execution proof supersedes that gate.

If enabled, SwrgNativeService.register:913-917 registers its listener, and
onSpellCastInteract:1840-1856 handles any non-spectator RIGHT_CLICK_AIR/BLOCK,
deduplicates per player/tick and calls runtime.executeFunction(lbc:items/spells).
LegacyFunctionRuntime:166-216 loads embedded .lwfn/.mcfunction bodies;
executeFunction:292-338 resolves overrides or executes their lines;
executeLine:405-418 rewrites execute-run-function calls to legacyfunction and
dispatchVanilla:477-488 invokes the native command dispatcher. Thus this is an
implemented optional path, not an unconditional input contract. It has no
material/ammo/consumption gate and would be initial RMB, never USING or finish.
Paper's independent always-registered UsableItemManager registers only TerraWand
(registerBuiltin); it supplies no common Clock/TMT spell dispatch.

## Existing native boundaries

NativeApple.onUseTick -> LbcTmtRuntime.usingItem:49-52 runs continuous traits,
selfGroups and charging through the existing once-per-tick coordinator. This
matches the source USING group. ConsumableSkillMixin invokes GameController.consumed
at the real ConsumeItemTrigger, before the native debit; withConsumedTraits:114-121
replays private food owners and finishes the ordinary food lifecycle. It does
not invoke spells. Preserve both paths. A future explicitly chosen broad RMB
compatibility policy would need its own initial-use boundary and proof; moving
spells to every eating tick or finish would invent a different contract.
