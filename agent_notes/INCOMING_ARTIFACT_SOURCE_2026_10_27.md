# Incoming-damage artifacts - complete family4 audit

27 October2026 source-only preparation. S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Source paths below relative to S. Only this note written; no native builds/tests/server/git/RP modifications. Legacy engine/runtime unobserved. Current/old mystical4 types1..6 all covered; other artifact families are out of scope.

## Native gaps and source event admission

Current LbcMagic exact artifact DATA supports mystical1 only. LbcSkills artifactIndex maps mystical4 to descriptions18..23 only. No live native family4 item flags/callbacks were found; generic LbcDamage/ArmorCalculation, passive immunity and wolf/explosion infrastructure are not incoming-artifact handlers. Native SkyWars2 AFTER_DAMAGE currently admits !blocked && damage>0 to LbcItems.damaged and game.damaged; AFTER_DEATH separately invokes selected outgoing services. **That positive/unblocked outgoing hook alone cannot reproduce this source incoming contract.** Inspect/add a dedicated incoming callback at appropriate native criterion timing, with explicit fatal/blocked support, without changing all shared outgoing hook semantics.

`lbc/advancement/cocksuckers.json` trigger entity_hurt_player, player not spectator and outside nexus/imprinted. NO damage amount, blocked, type, attacker or environmental exclusion is specified. Thus admitted native criterion events can include blocked/zero HP and environmental damage; it does not mean all rejected damage attempts fire. Native26.3 LivingEntity.hurtServer source :1212-1214 early-return rejected cooldown hits; :1248-1255 death runs BEFORE :1273 ENTITY_HURT_PLAYER.trigger(originalDamage,damage,blocked). Full blocked shield paths can still reach criterion. Legacy1.21.4 behavior not runtime-tested here.

`items/damaged.mcfunction:1` revokes cocksuckers IMMEDIATELY before helpers. This lets nested incoming damage re-enter the advancement; no function-wide recursion tag is present. :4 calls current damaged if container.* contains mystical4 and no stopper_magic marker20. NO nomagic_active player-aura gate. Current inner branches additionally require owner !spectator and Inventory NBT exacttype. Old inline :12-17 matches Inventory NBT and has no local suppression guard. Current container.* outer scope can reject an offhand-only/armor-only current artifact even though inner Inventory would match. Native explicitly reconcile source scopes; don't inherit wider mystical1 cursor/crafting cache as alleged parity.

Distinct types execute in current2->3->4->5->6 order; type1 has separate global criterion below. Then old1->2->3->4->5->6. Duplicates same type trigger once per matching command. No mana/charge/private cooldown/HUD input is added; effects persist normally after inventory removal. Source tail resets blocked_damage, schedules damaged_scheduled2t (sets ALL players damage_taken0); these are shared unrelated damage bookkeeping, not artifact timers.

## Current type1 is a separate entity-attacker shield

A whole-source search finds type1 in `lbc/advancement/damaged_by_player.json`, not damaged dispatcher. Its name is misleading: damage.source_entity.type is **#minecraft:attacks**, not literalplayer. Tag includes player and many mobs/projectile entries; environment with no matching source entity is excluded. Same owner dimensions excluded, inventory mystical4/type1 required. No blocked/amount/spectator condition at criterion level. `items/damaged_by_player:1` then requires nonspectator owner and time_stopped lbc.math NOT>=1 -> artifact/shield; :3 revokes damaged_by_player only AFTER helper. Thus helper has separate reward/revoke ordering from general damaged.

`other/artifact/shield:1-4`: owner Resistance40 ticks amp4 hidden; one forced BARRIER block_marker atY+.5 and one atY+1.5, each zero spread/speed; anvil.use master globally volume2/pitch2. No stopper_magic or nomagic_active check in this path. The grant affects later damage, not retroactively the hit that triggered it. Do not replace type1 with old ResistanceIV or implement it only for player attackers. Preserve native ally/technical-role/default Time Stop policy; owner self buff is not WILD.

## Current types2..6 exact helper closure

|Type/helper|Gameplay|Feedback|
|---|---|---|
|2 explode:1-3|ALL !spectator players radius7 includingowner Resistance20 ticks amp4 DEFAULT visible; owner Slow Falling100 ticks amp0 hidden; TNT atowner fuse0. Source no team/owner or magic tags.|Native TNT blast sound/particles, no extra helper sound.|
|3 corrosion:1-3|place template lbc:air11x11 atowner(-5,-8,-5), rotationnone/mirrornone, integrity.5; native source random template-placement processor.|reverse_portal30 normal atowner spread1/1/1 speed1; portal.trigger masterglobally1/2.|
|4 weakall:1-3|ALL !spectator entities radius7 INCLUDINGowner weakness60 ticks amp9 visible; source no team filter.|happy_villager count0(!), spread5/5/5 speed30 normal; count0 native directional particle semantics, not no particles. villager.trade masterglobally1/1.|
|5 speed:1-8|Ordered conditional progression below.|sweep_attack2 normal ownerY1 spread1/1/1 speed0; ender_dragon.growl masterglobally.5/2.|
|6 wolf:1-5|Spawn wolf atowner with DeathLootTable empty, Sittingfalse, CollarColor14, follow_rangeBASE200, movement_speedBASE.5, attack_damageBASE5, temporary summoned tag; nearest global summoned entity Owner UUID copied from owner, tagremoved.|wolf.growl masterglobally1/1; gray dust RGB.6/.6/.6 scale1 count10 normal ownerfeet spread.5/.5/.5 speed.02.|

Source explosion is TNT default power4, not a radius1 creeper or instant fixed damage. Native implementation should use actual native TNT-equivalent blast/policy/owner cause directly or proper native TNT primitive, no fuse1 creeper stand-in. Source owner/team missing is a required attribution repair. Default allied-player protection remains authoritative; no WILD exception authorized. Resistance is source player buff, not unconditional removal of explosion damage; normal native resistance/type rules apply.

Corrosion template parsed read-only: size11x11x11, paletteonly minecraft:air,1331 block entries,0 entities. Footprint ownerX/Z-5..+5, Y-8..+2. Integrity.5 probabilistically places AIR, does NOT mean exact halfcount, radial damage, tick lifetime or delayed restoration. Native block/container/drop/update semantics of template placement and resident guards must be reused; no destroy mode in source. Capture original owner for supportfall/void and any downstream damage. Must preserve block placement RNG/integrity semantics rather than invent a deterministic checkerboard. No template edits required.

Weakall source affects allies/self; self explicitly remains eligible. Default allied PLAYERS protected, friendly mobs eligible, technical roles/Greed guarded by platform. Harmful weakness amplifier9 must preserve signed/native layering; don't silently cap to normal vanilla potion levels. Native wolf should have originalowner and established summon friendly-mob/role attack policy, epoch/forget/stop cleanup. Source wolf has no custom duration, health override, forced target, disappearance task or despawn lifetime. Default entity health/AI/owner behavior apply; no invented300/600 tick expiry. Source nearest-global summoned selector can cross-owner alias; use direct new wolf reference, not legacy scratch tags.

### Type5 speed progression - all independent checks

speed:1 active fire_resistance(anyamp) -> Regeneration200ticks amp1; :2 active resistance -> FireResistance800ticks amp0; :3 active resistance -> FireResistance200ticks amp1; :4 active speed -> Resistance100ticks amp0; :5 active strength -> Speed100ticks amp2; :6 UNCONDITIONAL Strength100ticks amp0. All visible/default effect flags.

Order matters: newly granted Fire Resistance at2 does NOT retroactively trigger line1 in this event; newly granted Resistance at4 does NOT retroactively trigger2/3. New Speed at5 does not run4 until a later hit. Native current active-effect presence includes signed/high amplitudes; don't force0..N ladder or snapshot all six grants indiscriminately. FireResistance800/0 +200/1 must retain independent lower layer, normal layered timing. Removing item does not clear produced native effects.

## Old mystical_old4 six inline grants and self-harm recursion

items/damaged:12-17 respectively: type1 Resistance20ticks amp3 hidden; type2 INSTANT_DAMAGE durationargument1 amp0 hidden; type3 Speed200ticks amp1 hidden; type4 Slowness200ticks amp2 hidden; type5 Strength200ticks amp1 hidden; type6 Weakness200ticks amp0 hidden. All self-target, no own particles/sounds. Old2 is native HarmI with normal living base6HP before effect/native modifiers (undead inversion applies), duration **1TICK**, not20 and not genericdamage. Source addEffect command has no attacker ownership; native attribution must retain owner/cause per established policy without accidental new recursion.

Literal loop analysis: cocksuckers is revoked before old2 grant and condition has no source/amount/blocked gate, so if instant self-harm reaches ENTITY_HURT_PLAYER it CAN re-enter items/damaged and all possessed branches. There is no explicit recursion guard. However it is **not proven infinite**: legacy imported bypasses_cooldown tag includes arrow/generic/etc but NOT magic; native26.3 hurtServer rejects a second same-or-lower incoming hit while cooldown>10, so ordinary repeated6-damage magic normally stops through hurt cooldown. Depending originaldamage, an initial extra accepted higher selfhit can recurse once, while fullyblocked/other native modifiers may change admission. Source runtime1.21.4 and complete mixed-branch interactions unobserved. Do not assert infinite stack overflow, erase source selfharm, or silently enforce one-proc-one-harm without recording choice. Parent should defer precise reentrant-callback policy in question file; native shared producer guard can bound recursion while preserving actual first Harm grant if authorized.

Current2 TNT and old2 selfharm can create new incoming damage events and recursively produce summons/terrain/effects; native callback needs deliberate no-extra-payment/owner lifecycle and actual incoming event policy. A global outgoing !blockedpositive callback is not a safe replacement.

## Twelve leaves, pool/Looper/presentation/advancement

Current artifact.json types1..6: repeating_command_block/count1/model lbc:mysticalartifact/tier_u; name mystical_artifact #C28CFF italicfalse; lore firstrow type+accessory aqua italicfalse then mystical_artifact.lore red italicfalse. Flags transformable1/accessory1/unresolved_artifact1/mystical4/typeN. Weight10 each; qualities [10,-10,5,-10,10,-10]. Old artifact_old.json samebase/model/name/weight/quality, NO tier/type row, one red mysterious lore row; only mystical_old4/typeN. No durability/attribute rewrite. Exact current pool raw flags positions and loot indices recorded below automatically.

Current table accessed via accessory transform/reforge references; legacy luckyblock references artifact_old. No dedicated recipe/kit/helper craft found for family4 flags. Whole artifact pools contain other unresolved families and must not be activated wholesale from individual leaf support. Explicit current/old leaf acquisition is a separate integration task.

Looper source explanation: helper :19-24 currenttype1..6 -> artifact_18..23 yellow italicfalse lore[1], removes unresolved flag, writes back. Existing LbcSkills maps18..23; description availability is not gameplay. No direct family4-specific achievement grant in helperclosure. General mystical_artifact inventory collection includes family4 criteria; impossible shield dispatch advancement is technical trigger, not a displayed collectible task. Reuse exact existing resourcekeys/models, no RP updates.

## Independent next group and choices

Incoming event semantics and old2 recursion policy need explicit resolution for complete12-leaf release. Source type1 global criterion was found and fully closed, so it must NOT remain unimplemented merely because no damaged-dispatch line. Currenttype1 shield and type5 progression, old1/3/4/5/6 straightforward selfeffects are independent implementation prep; currenttype3 needs exact native template integrity and attribution, current2 real nativeblast, current6 owner-scoped wolf lifecycle. Established default ally/role/Greed/Time Stop policy resolves source indiscriminate harmful areas without new WILD block. Do not enable partial family variants or claim runtime qualification from this audit.

## Source original-byte SHA256

- `lbc/advancement/cocksuckers.json` `4bc90b59cc6438ffbf7c42984770e2cefb4f2fb170875edb70b735480b1de9e2`
- `lbc/advancement/damaged_by_player.json` `d8fe45e84680753bcc421d01fd6f5b62102987bb7eece8939ac04bba34e1f4cf`
- `lbc/function/items/damaged.mcfunction` `82d8bcedd5d091ea5af146077080987f03be1a4a1211191504280f057a906308`
- `lbc/function/items/damaged_by_player.mcfunction` `162cd4f4c125bebe7db91a5f2d313868d34db3dbe741b2a186ad1ef896124a5c`
- `lbc/function/items/damaged_scheduled.mcfunction` `cf285b080dc7fbab95d5981a35a9bae76060e44362c2819f8fa0111e72cef235`
- `lbc/function/other/artifact/damaged.mcfunction` `1e2d8899b8f2cdc6c544cb2a9e9576f29f40ef6f9300267d4c6c451f7fe0d703`
- `lbc/function/other/artifact/shield.mcfunction` `9f63a2593325f5ddcf4a0cf89f9c599285d064bb2c9a0b6798ae97fbdb9fedf8`
- `lbc/function/other/artifact/explode.mcfunction` `b88025f4a2d0d84a6112649c20e9a3d40b601eacbcbcc54b76e0042313f3ed18`
- `lbc/function/other/artifact/corrosion.mcfunction` `1b41c2983532554c5d2273f06218ce9d4b3135c199124adcf957cf98a90e7430`
- `lbc/function/other/artifact/weakall.mcfunction` `ff6bf4eda605ea6657f89dfd995aba608e02e04de2f46ff7cbc9243b30f4b28e`
- `lbc/function/other/artifact/speed.mcfunction` `dabdce0cacbce24d712e7ccc573b5fc816da56e7b2c5ebd4e4a21c6651af2e41`
- `lbc/function/other/artifact/wolf.mcfunction` `3590fb685a0b7d330c9759050ee04b54ecb8e1a74cc7b788095661e32f3d7f2e`
- `lbc/structure/air11x11.nbt` `5b947b9adf49f0fde30f1807a9fe78c51d06ce32925c23d02d7c20ccbafa2926`
- `lbc/loot_table/artifact.json` `04783e32c1a8935a3849a493fda7b885967835bf8749068965cb09cfc84052db`
- `lbc/loot_table/artifact_old.json` `5e75b1c35d90eacfb847757d6533f4cafc38919923de2d9e8003f2864bf1bbfd`
- `minecraft/tags/entity_type/attacks.json` `a4f417d72492ceac77d4484cab2a7e4f6b7f7868fa8b34ff9c92241e47ef36ab`
- `minecraft/tags/damage_type/bypasses_cooldown.json` `219f71eda49d72c2e6c2a2c29add7215d47a9a33ff38e8be6fcf3a5b3e7bc17b`

Exact pool indices (zero-based entries in source pools):

- `artifact.json` pool1/entry17: `{transformable:1,accessory:1,unresolved_artifact:1,mystical:4,type:1}`
- `artifact.json` pool1/entry18: `{transformable:1,accessory:1,unresolved_artifact:1,mystical:4,type:2}`
- `artifact.json` pool1/entry19: `{transformable:1,accessory:1,unresolved_artifact:1,mystical:4,type:3}`
- `artifact.json` pool1/entry20: `{transformable:1,accessory:1,unresolved_artifact:1,mystical:4,type:4}`
- `artifact.json` pool1/entry21: `{transformable:1,accessory:1,unresolved_artifact:1,mystical:4,type:5}`
- `artifact.json` pool1/entry22: `{transformable:1,accessory:1,unresolved_artifact:1,mystical:4,type:6}`
- `artifact_old.json` pool0/entry15: `{mystical_old:4,type:1}`
- `artifact_old.json` pool0/entry16: `{mystical_old:4,type:2}`
- `artifact_old.json` pool0/entry17: `{mystical_old:4,type:3}`
- `artifact_old.json` pool0/entry18: `{mystical_old:4,type:4}`
- `artifact_old.json` pool0/entry19: `{mystical_old:4,type:5}`
- `artifact_old.json` pool0/entry20: `{mystical_old:4,type:6}`

## Read-only RP text/model

`en_us` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "mystical_artifact": "Mystical Artifact",
  "mystical_artifact.lore": "Unknown effect",
  "artifact_18": "Invulnerability when damaged",
  "artifact_19": "Explosion when damaged",
  "artifact_20": "Block corrosion when damaged",
  "artifact_21": "All pacifists when damaged",
  "artifact_22": "Berserker buffs when damaged",
  "artifact_23": "Spawn wolf when damaged"
}
```

`ru_ru` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "mystical_artifact": "Мистический Артефакт",
  "mystical_artifact.lore": "Неизвестный эффект",
  "artifact_18": "Неуязвимость после получения урона",
  "artifact_19": "Взрыв при получении урона",
  "artifact_20": "Коррозия блоков при получении урона",
  "artifact_21": "Слабость всем при получении урона",
  "artifact_22": "Усиление игрока при получении урона",
  "artifact_23": "Призыв волка при получении урона"
}
```

`assets/lbc/items/mysticalartifact.json` SHA256 `de862c559b494df5271be0f9a59a4c69f3804d56a0b574d33298800aa246a562`

`assets/lbc/models/item/mysticalartifact.json` SHA256 `f8bfd2b5ea27191c6f1553cbb596956645298949df83be2c179678f0db37517b`

Native26.3 engine source inspected from existing loom sources archive; this evidence establishes current code event ordering/ordinary cooldown guard, not an observed legacy server run. Legacy Paper source search found no family4 dispatch override; absence search does not establish old runtime behavior.
