# Glass armor source closure ? 28 October 2026

Read-only audit; no runtime qualification claimed. Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Project instructions and agent_notes/RESOURCE_PACK.md read; original server/RP untouched.

## Trigger and native reuse

`advancement/cocksuckers.json` entity_hurt_player, excludes spectators, minecraft:nexus and minecraft:imprinted; no damage/blocked restriction. Reward `function/items/damaged.mcfunction:9` invokes Glass when ANY armor slot has raw glass:1. Unlike neighboring abilities, Glass has NO stopper/stopper_magic/nomagic gate. No cooldown/random roll. Reuse actual native IncomingArtifactCriterionMixin and pre-hurt fatal admission capture used by LbcNanobotArmor; do not add generic accepted-hurt callback (shield/absorption count). Existing LbcArmorSets has no Glass mechanic. LbcDamage lists glass solely to exclude custom sets from ordinary-metal electric penalty. Existing LbcBolts ReforgeRule already owns glass; do not duplicate it. Existing import handles raw four-piece PvP advancement; verify rather than introduce another award path.

## Exact damage algorithm and gotchas

`function/armor/glass/damaged.mcfunction`: reset tmp2, sequential feet?legs?chest?head. For each matching slot read the item armor modifier amount with data-get scale1000 into #tmp1, add to unused tmp2, replace WHOLE item with corresponding `loot_table/down_armor_<slot>.json`. Replacement uses #tmp1*0.00095 for BOTH armor and toughness, regardless of old toughness. Data-get result uses floor, so next armor = floor(A*1000)*0.00095. Fresh40?38?36.1?34.295 (subject to floating/score provider representation). Full stack replaced with fresh leather base: durability damage, enchants, custom additions and other nonauthored components do not survive. This is not just an attribute mutation.

Then `damaged_full_set.mcfunction` rereads current postreplacement slots. Set tmp=tmp1=0. For every matching slot reset #tmp1=0, read armor*.17 via data-get floor, add to tmp. Count matching slots N in tmp1. Integer score divide tmp /= N (floor semantics); atk=4*N-tmp. Tag wearer atacker, if atk>=1 invoke macro, then remove tag. There is NO N==4 check despite name/lore. Fresh full set gives sum4*floor(38*.17)=24; average6; damage10. Fresh N=1 gives -2 (no damage), N=2 gives2, N=3 gives6. No lower clamp on armor; zero stays zero. N==0 division fails preserving tmp, atk0 skips burst. Initial read failures store0; tmp2 is unused; do not retain stale cross-player scratch as gameplay state.

`damaged_full_set_dmg.mcfunction`: copy wearer team_number to *tempt lbcID2 then execute as #minecraft:mobs with origin distance .001..7 and predicate !lbc:same_team; hurt with integer atk `arrow by @a[tag=atacker,limit=1]`. Origin spherical feet distance, .001 lower bound excludes co-located targets as well as wearer. Same-team predicate is equal-score test; missing target team score fails same_team and therefore allows target. Source mobs tag includes players/passives/hostiles and modern additions; reuse existing source living-entity target selector plus role/Greed/friendship safety. Arrow is actual attributed native damage without helper projectile, preserving downstream arrow distinctions and indirect kills. No custom particles or playsound in any Glass function; native damage presentation only. Snapshot wearer/position/attribution locally; don't reproduce global atacker race. Target rechecks matter during synchronous recursive victim criteria: new Glass armor values must already be equipped before burst.

## Acquisition and source resources

`neko_set_glass.json`: one equally weighted random leaf of helmet/chestplate/leggings/boots. `_pre`: five independent random-piece rolls. `_full`: four separate pools, guaranteed each piece once. All leaves leather, count1, armor40+toughness40 add_value in matching equipment slot; max_damage150, model lbc:armors/glass_<piece>, equippable asset lbc:glass, tooltip lbc:tier_b; raw transformable:1, glass:1 and respective helmet/chestplate/leggings/boots:1. Name white italic=false. Three lore rows aqua italic=false: [type,piece], glass.lore, glass.lore.2. Preserve exact attribute identifiers from resources (they intentionally differ per slot/type); no shared invented replacement ID.

Roots: nekolucky_items?_pre; 6_sets?_full; transformables_<piece>?leaf; normal native bolt reforge uses existing Glass rule, ultra_reforge.mcfunction:41?random root. Do not enable previously disabled unrelated drop pools. build.gradle currently packages random root+four leaves but misses Glass pre/full: add explicit includes like Nanobots. Four down_armor loot tables use same display/raw flags as leaves, replacement score scale .00095. `item_modifier/down_armor_{feet,legs,chest,head}.json` are DIFFERENT unused variants: scale .0009, #tmp1/#tmp2/#tmp3/#tmp4 respectively, only two lore rows and no tooltip_style; do not accidentally use them. Generic item_modifier/down_armor.json uses #tmp1 scale1 and head slot; it is not the called path.

`advancement/true_advancements/armor/glass.json`: player_killed_entity requires nonspectator killer wearing all four raw Glass flags, victim type player; no damage-source restriction. Display goal, toast/chat true, hidden false, parent traveller, source translation keys; gain/levelup reward. This is separate from incoming proc.

## RP

Read original equipment/items/language assets; all exist. Equipment humanoid texture lbc:glass, humanoid_leggings lbc:glass_leggings. Four item definitions reference lbc:item/armors/glass_<piece>. No new RP/template required. Preserve original key/component text exactly. Exact relevant original translations follow (JSON escaped Unicode retained losslessly):

### en_us

```json
{
  "glass_helmet": "Glass Helmet",
  "glass_chestplate": "Glass Chestplate",
  "glass_leggings": "Glass Leggings",
  "glass_boots": "Glass boots",
  "glass.lore": "Gradually breaks when damaged.",
  "glass.lore.2": "Full set - deals damage with flying glass!",
  "lbc.adv.armor.glass": "Frag grenade",
  "lbc.adv.armor.glass.lore": "Equip full set of Glass Armor and kill another player!"
}
```

### ru_ru

```json
{
  "glass_helmet": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0439 \u0448\u043b\u0435\u043c",
  "glass_chestplate": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0439 \u043d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a",
  "glass_leggings": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0435 \u043f\u043e\u043d\u043e\u0436\u0438",
  "glass_boots": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0435 \u0431\u043e\u0442\u0438\u043d\u043a\u0438",
  "glass.lore": "\u041f\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u043e \u043b\u043e\u043c\u0430\u044e\u0442\u0441\u044f \u043f\u0440\u0438 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u0438\u0438 \u0443\u0440\u043e\u043d\u0430.",
  "glass.lore.2": "\u041f\u043e\u043b\u043d\u044b\u0439 \u0441\u0435\u0442 - \u043d\u0430\u043d\u043e\u0441\u0438\u0442 \u0443\u0440\u043e\u043d \u0440\u0430\u0437\u043b\u0435\u0442\u0430\u044e\u0449\u0438\u043c\u0441\u044f \u0441\u0442\u0435\u043a\u043b\u043e\u043c!",
  "lbc.adv.armor.glass": "\u041e\u0441\u043a\u043e\u043b\u043e\u0447\u043d\u0430\u044f \u0433\u0440\u0430\u043d\u0430\u0442\u0430",
  "lbc.adv.armor.glass.lore": "\u042d\u043a\u0438\u043f\u0438\u0440\u0443\u0439\u0442\u0435 \u043f\u043e\u043b\u043d\u044b\u0439 \u0441\u0435\u0442 \u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u043e\u0439 \u0431\u0440\u043e\u043d\u0438 \u0438 \u0443\u0431\u0435\u0439\u0442\u0435 \u0432 \u043d\u0435\u0439 \u0438\u0433\u0440\u043e\u043a\u0430!"
}
```

### rpr

```json
{
  "glass_helmet": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0439 \u0448\u043b\u0435\u043c",
  "glass_chestplate": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0439 \u043d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a",
  "glass_leggings": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0435 \u043f\u043e\u043d\u043e\u0436\u0438",
  "glass_boots": "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0435 \u0431\u043e\u0442\u0438\u043d\u043a\u0438",
  "glass.lore": "\u041f\u043e\u0441\u0442\u0435\u043f\u0435\u043d\u043d\u043e \u043b\u043e\u043c\u0430\u044e\u0442\u0441\u044f \u043f\u0440\u0438 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u0438\u0438 \u0443\u0440\u043e\u043d\u0430.",
  "glass.lore.2": "\u041f\u043e\u043b\u043d\u044b\u0439 \u0441\u0435\u0442 - \u043d\u0430\u043d\u043e\u0441\u0438\u0442 \u0443\u0440\u043e\u043d \u0440\u0430\u0437\u043b\u0435\u0442\u0430\u044e\u0449\u0438\u043c\u0441\u044f \u0441\u0442\u0435\u043a\u043b\u043e\u043c!",
  "lbc.adv.armor.glass": "\u041e\u0441\u043a\u043e\u043b\u043e\u0447\u043d\u0430\u044f \u0433\u0440\u0430\u043d\u0430\u0442\u0430",
  "lbc.adv.armor.glass.lore": "\u042d\u043a\u0438\u043f\u0438\u0440\u0443\u0439\u0442\u0435 \u043f\u043e\u043b\u043d\u044b\u0439 \u0441\u0435\u0442 \u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u043e\u0439 \u0431\u0440\u043e\u043d\u0438 \u0438 \u0443\u0431\u0435\u0439\u0442\u0435 \u0432 \u043d\u0435\u0439 \u0438\u0433\u0440\u043e\u043a\u0430!"
}
```

## Author question

Source never suppresses Glass. Ask whether TECH, MAG, both, or neither suppress radial shards; independently confirm degradation continues (physical wear) under suppression. Source intent otherwise complete: do not request permission for arithmetic, partial-set proc, whole-item replacement or missing visual effects merely because lore/function names differ.
