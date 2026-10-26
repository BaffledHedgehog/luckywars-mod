# Seven ordinary summoners — 26 October 2026

The continuation batch closes the seven remaining entries of ordinary
`lbc:transformables_summons`, alongside the already ported Soul Vex.
The author's latest correction excludes all summoners from grimoires. All nine
summoning books are removed, including the previously released Vex/Psychovex.
IDs262..270 are retired. Seven Academy Gaycaster books use IDs271..277, one
per rainbow color. The requested ordinary/Academy Snow books use278/279,
tier D/B and Rare. Their source caster/aimed cloud origins stay distinct.
The collection now has268 attainable books; other existing
criterion IDs remain unchanged.

## Exact source

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Entry points: `function/workingitems/spellsummoner{,_left}.mcfunction` and
`function/workingitems/summoner/{skeletons,razoritel,eliteskeletons,ghostrider,ghast,blaze,guardians}.mcfunction`.
Costs: `function/config.mcfunction:54..61`; presentation/weights/components:
`loot_table/transformables_summons.json`. Existing RP keys/models read from the
author pack in `RESOURCE_PACK.md`; that pack remains read-only.

| Native item | Native source formation | Mana | Tier |
| --- | --- | --- | --- |
| summoner_skeletons |20 skeletons,2HP,golden swords/chainmail helmets; absolute offsets up to3 |3000 |B |
| summoner_ravager |1 ravager,40HP,flat forward4 regardless of pitch |5000 |B |
| summoner_elite_skeletons |2 skeletons,diamond gear,flat local left/right1 |4000 |B |
| summoner_ghost_rider |bat atY+2 -> invisible tame skeleton horse -> enchanted bow skeleton |4000 |D |
| summoner_ghasts |9 ghasts,world offsetsX/Z−10,0,10,Y+20 |3000 |D |
| summoner_blazes |3 stacked passenger blazes atY+5 |3000 |D |
| summoner_guardians |4 no-gravity guardians atY+1,cardinal offsets3 |3000 |B |

Importer preserves raw SNBT including nested passengers, native health/defaults,
equipment/enchantments/drop chances and follow range200. ENTITY DFU runs once
asynchronously; gameplay uses prepared presets. Ravager deliberately starts
with40 current HP and native max HP, as authored. No custom visual/entity engine,
projectile pacing, new lifespan or active count cap is added.

## Native ownership and input

`LbcSummonerItems` registers exact source components, model/tier/name/lore and
legacy flags. Ordinary summoners share `LbcWands` actual both-hand dispatch and
`LbcSummons` lifecycle. Each hand pays independently via `trySpendWand`; full
Overload bypasses admission but retains the real debit/debt. Shift commands
existing summons without spending or creating a new formation. Commands use
native targets; no fake damage/retaliation trick is carried from the datapack.

The source32 team selectors and nearest/unactivated matching are replaced by
direct owner/family handles, consistent with existing native Soul Vex. A recast
replaces that player's own previous formation of the same family, including all
passengers. Other families/players remain intact. Source structures and all
ordinary mob AI/movement/attacks stay native. Narrow native subclasses guard
real melee/target selection and prevent persistence of match-only actors.

Existing native projectile admission transfers summon projectile ownership to
the player and shared effect/kill routing. The original arrow pickup setting is
retained across this transfer, so bow skeletons do not generate pickup ammo.
Native arrows/fireballs inherit the existing friendly-player/role guards and
friendly-mob projectile rules. Match stop, owner retirement and epoch removal
clean roots/passengers/projectiles through the shared service.

## Acquisition and resource pack

All eight ordinary source entries now resolve to registered playable items with
unchanged source weights/components. Public `/loot` includes the ordinary
`lbc:transformables_summons` pool and the seven `lbc:ported/summoner_*` leaves.
The expanded `_all` pool and whole Lucky pools remain gated: their other rewards
are separate incomplete families. Forest was audited as a candidate; its rare
End of Technomagic reward reaches supply drops/cyborg encounters and cannot be
declared complete by spawning trees alone. It remains unavailable.

Seven color books share Academy Gaycaster's exact native producers without
mana/trials/cast hooks, using its tier and Legendary/enhanced presentation.
Cyan retains electricity and blue retains water, as explicitly agreed earlier.
Template adds seven model definitions, generated models, placeholder PNGs and
fourteen keys per language. Other retained extra-book translations are preserved;
keys/assets already applied by the author are read from the pack, not duplicated.
Retired summoning-book leaves, collection criteria and template assets are removed.
Merge instructions: `resourcepack_template/EXTRA_GRIMOIRES_README.md`.

## Verification

Evidence directory: `build/verification/summoners-final/`. Build41395417 passed
24 groups in two actual mode1 matches (114.91 seconds). Exact385 sources,
1094 classes and3112 resources match the tested artifacts. Installer8624
installed it over081a2f7a with a verified rollback and no restart. The nine
retired summoning-book leaves are absent from the final JAR. Oldestb4aa411f
archive was removed; current41395417 and two recent rollback artifacts remain.

New checks
cover actual item inputs in both hands, exact formations/HP/gear/offsets and
passenger links, mana/Overload/Shift/guards, real skeleton melee kill attribution,
enchanted native rider arrows/no-pickup, naturally ticking ghast/blaze shooting,
the complete ordinary pool, new consumed books/native collection predicates and
actual-stop cleanup. Native boundary loops are explicit test steps, not claims
of observed real-client rendering/audio or peak TPS.
