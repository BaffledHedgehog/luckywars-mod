# Soulless Wand and Soulhunter presentation audit (25 October 2026)

Scope: ordinary Soulless Wand and full Soulhunter Mace/craft. Prepared leaves are presentation payloads; this audit alone does not qualify gameplay, registry integration, loot decoding or client pixels. Academy is a separate stage. Metasola resource closure was subsequently added below; its gameplay belongs to the dedicated core integration. Source server, author RP, template, broad pools, languages and Java were not changed.

## Exact identity and scoped outputs

- `lbc:ported/soulcorruption_wand` uses `skywars2:soulcorruption_wand`; source is entry index1 of `lbc:halloween/transformables_staff`, native carrot-on-a-stick payload. Identity is `soulcorrupter:1`, **not** the ordinary Corruption Wand. Existing name key `soulcorruption_wand` means Soulless Wand / Бездушный Жезл. Model `lbc:soulcorruptwand`, tooltip `lbc:tier_b`, no forced name color, italic=false. Exact custom data `{craftusable:1,transformable:1,staff:1,magicwand:1,soulcorrupter:1}`.
- `lbc:ported/soulhunter_mace` uses `skywars2:soulhunter_mace`; source `lbc:halloween/soulhunter_mace`, native mace payload. Existing name key `soulhunter_mace` means Ghost Hunter / Охотник за призраками; red, italic=false, model `lbc:soulhunter_mace`, tooltip `lbc:tier_ss`. Exact custom data `{transformable:1,shovel:1,soulhunter_mace:1,spellwithcooldown:1}`.

Both leaves use 26.3 entry `modifier` and modifier discriminator `type`, converted from source `functions`/`function`. Source pool-specific weight10/quality9 is omitted from the isolated deterministic wand leaf. No component, name or lore was rewritten.

## Lore and component contract

Wand has14 rows in source order: aqua type+staff; aqua literal `0-6 ` + magic_damage; aqua score `soulcorruption_wand_mana_use` / `lbc.math` + mana_on_activate; aqua literal `15 ` + cast_radius; gold soulcorruption_wand.lore.1; white lore.2 through.8; gold lore.9; yellow craft_with_antichrist_mace. Every component explicitly has italic=false. Source config sets mana score1500. Keep the original score component and use the established scoreboard setup; replacing it with another value or text changes the source payload.

Source lore radius15 conflicts with actual `cast_2` selector radius25 and translated lore.2 radius25. This mismatch is preserved and must be documented rather than silently corrected. Ordinary source lore mentions Metasola even though that separate subsystem is outside this batch; no lore rows are removed.

Mace has6 rows: aqua type+shovel; yellow soulhunter_mace.lore.1 through.4; orange #ff670f halloween.lore. All italic=false. Mainhand attributes: attack_damage +10 add_value / minecraft:base_attack_damage; attack_speed -2.4 add_value / minecraft:base_attack_speed; fall_damage_multiplier -1 add_value / `mhsr5jhrjntyyj` (unqualified source ID). Enchantments wind_burst2, density5. Source consumable animation none, consume_seconds1000000000000, has_consume_particles=false; food nutrition0/saturation0/can_always_eat=true. These are deliberately retained, including the huge duration. Native codec/use behavior still requires parent integration checks.

## Author RP read-only verification

Root: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
Existing `assets/minecraft/lang/ru_ru.json`, `en_us.json`, `rpr.json` contain the original names, source lore and craft key. No key additions or duplicate translations are needed. Existing `assets/lbc/items/soulcorruptwand.json` and `soulhunter_mace.json` reference `lbc:item/soulcorruptwand` and `lbc:item/soulhunter_mace`; corresponding model files use vanilla handheld_rod / handheld_mace parents and `minecraft:item/soulcorruptwand` / `minecraft:item/soulhunter_mace` textures. Both original PNG files exist. Existing tier_b/tier_ss styles are reused. Actual client pixels were not verified.

## Source craft and future-stage boundaries

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
`function/other/craft/inventory_craft_check.mcfunction` line74 requires soulcorrupter + halloween_mace in the four player crafting slots. `other/craft/soulhunter_mace_inv` spawns one result then reduces every matching donor slot, not an exact native recipe. `other/craft_ground.mcfunction` line78 detects a separate Soulless item within0.001..1 of the mace; `other/craft/soulhunter_mace` spawns the result then kills the donor item entities (whole stacks). Neither tests upgradable/upgraded. These source quirks must not be mistaken for an exact native recipe contract.

`other/event/halloween/soulcorruptwand/cast_2` applies ghosting to all #mobs within25 except spectator, with no ally/caster exclusion: source WILD. `apply_ghosting` adds5 effect seconds and the exact attribute modifiers, except Vex invokes effects/ghosting_second_vex. Ordinary cast dispatch can run meditation for upgradable sneaking stacks; `cast_2meditate` summons16 Vex then calls the common can_upgrade function. Upgrade applies tierA through tiers/a. No Academy leaf or readiness claim is included here.

Validation: both generated JSON leaves parse; source presentation is copied structurally with only item identity and native syntax changes. Build/server/git operations were not run.


## Metasola resource closure

Ordinary Soulless ghost/Vex reward closure requires all six Metasola payloads. Added `lbc:ported/metasola_1` through `metasola_6`, each using matching `skywars2:metasola_N`; added narrow mixed `lbc:metasola`. This does not enable broad Halloween acquisition pools. Source is `loot_table/halloween/metasola.json` under the legacy root above. Each source entry is a native snowball. The mixed table copies all six source entries in source order, each implicit weight1/quality0 (equal one-sixth outcomes). Six single-result leaves preserve the corresponding entry. Only native item registry identity and 26.3 `modifier`/`type` syntax change; source unqualified `item_model` component key remains legal/default-minecraft and unchanged.

All six names use `metasola_N`, aqua, italic=false; item model `lbc:metasola_N`. Tiers are B/A/A/A/S/S. Each has six lore rows: aqua type+metasola; white metasola_N.lore.1/.2/.3; red metasola_all.lore; orange #ff670f halloween.lore. Every piece explicitly has italic=false. Exact custom data is `{transformable:1,metasola:1,type:N,gadget:1}`. No other source attribute, enchantment, consumable or food component is present.

Author RP read-only checks found all six existing `assets/lbc/items/metasola_N.json`, corresponding `assets/lbc/models/item/metasola_N.json`, and every model texture reference resolving to an existing PNG. These are authored multi-element models rather than vanilla snowball placeholders. Existing RU/EN/rpr keys cover every name, all three per-kind lore rows, shared metasola and metasola_all.lore; no language/template additions are needed. Seven generated JSON files parse; native registered loot decode and actual client appearance require core integration checks. No Academy leaf is added.


## Integrated qualification (25 October 2026)

Build `fa036823` passed86 recorded results in two actual mode1 matches (151.33s),
including all14 Soulhunter-family cases twice, the existing shared regressions,
real match-stop probes and cleanup. Exact316 source files/881 classes/2812
resources match the tested ready JAR. No new ERROR lines beyond the existing
fake-bot Polymer packet-context pair. Evidence: `build/verification/soulhunter-final/`.
Main installation was refused while its session.lock was held. The user then
requested leaving this build ready, so the main JAR remains `2558af6b`; no restart
or replacement occurred. This supersedes individual task-agent pending statuses.
Native fixture checks include explicit tick boundaries, not real client pixels,
actual disk restart/disconnect or peak-capacity qualification.
