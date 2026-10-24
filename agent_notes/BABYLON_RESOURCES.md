# Babylon Gate resource and pool audit (2026-10-24)

The scoped leaf `src/main/resources/data/lbc/loot_table/ported/babylon_gate.json` copies the sixth entry of `transformables_arcanum.json`. Its sole intentional difference is item registry name `minecraft:repeating_command_block` ? `skywars2:babylon_gate`. One pool, one roll, one item entry; entry weight 10 and quality 10 remain unchanged. The `modifier` array (not a newly normalized `functions` array) is copied exactly, including function `type` fields.

Presentation: count 1, `lbc:tier_a`, `lbc:babylongate`, gold nonitalic `gate_of_babylon` name. Four lore lines retain aqua/nonitalic styles: type+arcanum; score `babylon_gate_mana_tick_sec` in `lbc.math` + mana_on_use; gate_of_babylon.lore.1; gate_of_babylon.lore.2. Custom data remains exactly `{transformable:1,arcanum:1,arcanum2t:1,gate_of_babylon:1}`. The score must be initialized/refreshed by native runtime; intended default 300 mana each two ticks gives 3000/sec. Coordinates and projectiles are audited separately by the implementation agent.

Author resource pack was read only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. All six translation keys exist in every language file containing these keys (three files below). Exact values remain in the author pack and are not copied into mod/template translations. Item chain verified: `assets/lbc/items/babylongate.json` ? `lbc:item/babylongate` ? `assets/lbc/models/item/babylongate.json` ? generated vanilla parent and `minecraft:item/babylongate` ? existing `assets/minecraft/textures/item/babylongate.png`. Existing `assets/lbc/textures/gui/sprites/tooltip/tier_a_background.png` and `tier_a_frame.png`, both with mcmeta, provide tier styling. No resource template is necessary. This validates references/data, not a rendered graphical client comparison.

Language values and SHA-256 at audit (JSON escaping preserves exact Unicode):

- `assets/minecraft/lang/en_us.json` SHA-256 `97bb2816aa4365b900bd3c95fcb2052f8c43661dab55a1aee0129387be82be81`

```json
{
  "gate_of_babylon": "Gate of Babylon",
  "type": "Item type: ",
  "arcanum": "Arcanum",
  "mana_on_use": " Mana cost to maintain (per second)",
  "gate_of_babylon.lore.1": "Creates gates around you that shoots",
  "gate_of_babylon.lore.2": "many beautiful weapons."
}
```

- `assets/minecraft/lang/rpr.json` SHA-256 `d6976e0779f458511485596879f46ee0e91de68504a5303964b4d0f4f76e338a`

```json
{
  "gate_of_babylon": "\u0412\u0440\u0430\u0442\u0430 \u0412\u0430\u0432\u0438\u043b\u043e\u043d\u0430",
  "type": "\u0422\u0438\u043f \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430: ",
  "arcanum": "\u0410\u0440\u043a\u0430\u043d\u0443\u043c",
  "mana_on_use": " \u0420\u0430\u0441\u0445\u043e\u0434 \u043c\u0430\u043d\u044b \u0434\u043b\u044f \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0430\u043d\u0438\u044f (\u0432 \u0441\u0435\u043a\u0443\u043d\u0434\u0443)",
  "gate_of_babylon.lore.1": "\u0421\u043e\u0437\u0434\u0430\u0451\u0442 \u0432\u0440\u0430\u0442\u0430 \u0432\u043e\u043a\u0440\u0443\u0433 \u0432\u0430\u0441, \u0441\u0442\u0440\u0435\u043b\u044f\u044e\u0449\u0438\u0435",
  "gate_of_babylon.lore.2": "\u043c\u043d\u043e\u0436\u0435\u0441\u0442\u0432\u043e\u043c \u043f\u0440\u0435\u043a\u0440\u0430\u0441\u043d\u044b\u0445 \u043e\u0440\u0443\u0436\u0438\u0439."
}
```

- `assets/minecraft/lang/ru_ru.json` SHA-256 `dbb07bd214bf009807d4579f89a16de12a15e4a80ae47e14a08c05b5b4827ed1`

```json
{
  "gate_of_babylon": "\u0412\u0440\u0430\u0442\u0430 \u0412\u0430\u0432\u0438\u043b\u043e\u043d\u0430",
  "type": "\u0422\u0438\u043f \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430: ",
  "arcanum": "\u0410\u0440\u043a\u0430\u043d\u0443\u043c",
  "mana_on_use": " \u0420\u0430\u0441\u0445\u043e\u0434 \u043c\u0430\u043d\u044b \u0434\u043b\u044f \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0430\u043d\u0438\u044f (\u0432 \u0441\u0435\u043a\u0443\u043d\u0434\u0443)",
  "gate_of_babylon.lore.1": "\u0421\u043e\u0437\u0434\u0430\u0451\u0442 \u0432\u0440\u0430\u0442\u0430 \u0432\u043e\u043a\u0440\u0443\u0433 \u0432\u0430\u0441, \u0441\u0442\u0440\u0435\u043b\u044f\u044e\u0449\u0438\u0435",
  "gate_of_babylon.lore.2": "\u043c\u043d\u043e\u0436\u0435\u0441\u0442\u0432\u043e\u043c \u043f\u0440\u0435\u043a\u0440\u0430\u0441\u043d\u044b\u0445 \u043e\u0440\u0443\u0436\u0438\u0439."
}
```

Ordinary pool closure is now routed: `transformables_arcanum` retains one roll and six equal weight=10/quality=10 entries in source order, referring to `lbc:ported/void_arcanum`, `barrier_arcanum`, `healing_arcanum`, `kiuaskivi`, `tannerkivi`, and `babylon_gate`. Each scoped leaf owns the item modifiers; the pool no longer duplicates presentation. This intentionally uses the qualified Barrier leaf, whose obsolete 100-mana activation lore was removed in its earlier qualified batch. No scoped leaf modifiers were changed by pool closure.

All-pool closure is now routed: `transformables_arcanum_all` retains source weights 6:1:1 and references ordinary `lbc:transformables_arcanum`, `lbc:ported/sun_arcanum`, and `lbc:ported/neutral_ball`. There are no further nested dependencies in the Sun and Neutral Ball leaves. Each of the six ordinary outcomes has probability 1/8 at default luck, as do Sun and Neutral Ball. Equal ordinary quality values preserve equal ordinary probabilities for any luck where entries remain selectable. Sun uses `sun_arcanum` + `arcanum5t` + `magicwand`; Neutral Ball uses `neutral_ball` + `arcanumt`. They remain separate from the six ordinary outcomes.

Validation: parsed both modified pools, verified six ordinary references, source entry order and weight/quality 10/10, outer weights 6/1/1, every referenced scoped leaf exists and resolves to the expected native `skywars2:` item. Runtime validation belongs to the parent integration task; no build was run here. Only these two pool tables and this note were changed in the closure step. Catalog/readiness/generator changes are owned by the parent.

## Qualified batch evidence

Build `0fb48f1a` passed62 recorded suite results across two actual mode1 matches (144.06s). Exact 306 sources / 837 classes / 2793 resources match the tested artifact. Installed stopped main with rollback `daa2f866`; main was not started. Shared lifecycle, source transport and active-Greed guards are integrated. Three protected user sources are unchanged/excluded. Evidence: `build/verification/babylon-final/`. Boundary fixtures include manual ticks; no real client pixels, actual disk restart/disconnect or peak-capacity performance claim.
