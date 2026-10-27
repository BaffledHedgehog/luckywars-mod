# Jump artifacts - complete current/old six-leaf source contract

27 October2026 read-only source preparation. S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`; paths relative to S. Only this note written; no code/build/test/server/RP/git operations. Legacy runtime unobserved. Existing native signed-effect layers, item caps, allied-player/technical-role/Greed guards, Time Stop and owner attribution are authoritative. No WILD allied-player exception is authorized or needed. This complete bounded group is independently implementable, not an implementation claim.

## Genuine native gap and event primitive

Current LbcMagic.artifact():384-395 matches mystical1 DATA only. LbcSkills.artifactIndex:142 maps mystical3 types1..3 to artifact_15/16/17 descriptions; this is Looper lore, not jump behavior. No live current/old mystical3 jump producer exists. Support all six leaves as one group; do not re-port mystical1 or enable partial random artifact pools.

Existing `src/main/java/me/skywars2/mixin/LbcEquipmentMixin.java:16-23` injects LivingEntity.jumpFromGround TAIL for ServerPlayer and dispatches native Halloween/Soulhunter/Overload jump services. Add a dedicated jump-artifact callback there after actual native jump impulse, keeping current independent callbacks. The new boost must affect future jumps; do NOT recompute the just-applied vertical motion using newly granted Jump Boost. Shared signed jump implementation already handles effect strength safely; no direct velocity injection is implied by these sources. Callback should use actual jump occurrence rather than airborne position polling, swimming ascent, flight or statistics commands.

## Source jump score/tick/cleanup ordering

`load.mcfunction:3` creates jump1 criterion minecraft.custom:minecraft.jump. Native jump stat increments feed this score. `tick.mcfunction` -> tick_not_timestopped -> mobs_tick:4 nonspectator !time_stopped players -> players_tick:6 when jump1>=1 -> items/armor. Thus source can coalesce multiple accumulated jumps into ONE armor evaluation, examines inventory at processing time after jump, and pauses entry during Time Stop. This is not player_hurt_entity or a jump-specific advancement reward.

`items/armor.mcfunction:6` calls current when_jump if Inventory contains mystical3, no stopper_magic MARKER within20, no nonspectator nomagic_active PLAYER within20. when_jump independently evaluates types1,2,3 in that order, owner !spectator. Current magical suppression does not prevent old grants later in armor.

Old grants inline `items/armor:11-13` evaluate mystical_old3 types1,2,3 in order after current branches and unrelated source armor actions. They have no local magic/stopper gate. At function tail armor:17 adds falltimer0 (initializes/preserves count, not reset), :18 sets jump1=0 AFTER all armor actions. There is no jump advancement to revoke. Source load_scores_reload:3 removes objective. No artifact-private timer/score or explicit Jump Boost clear on inventory removal/purify is defined; ordinary effects expire/layer and full native match cleanup applies. Native port does not need to create/poll/reset source jump1 stat objective merely to implement event callback.

Inventory predicates inspect source Inventory NBT, covering carried inventory/equipment slots exposed there, including offhand; not menu cursor/crafting containers. Do not assume LbcMagic's wider passive inventory cache automatically defines intended reactive scope. One matching type triggers once regardless of duplicate copies; distinct types current1->2->3 then old1->2->3 can all fire in one jump. Both current/old of same type coexist, effects use shared native layering. Source types are NOT a randomized choice during activation.

## Current mystical3 exact three behaviors

|Type/helper and lines|Exact gameplay|Exact feedback|
|---|---|---|
|1 highspeed:1-5|Self SPEED60 ticks amp5 (Speed VI), visible default particles. Copy owner team to fake *tempt lbcID2. Enemy !spectator entities distance.01..7 get SLOWNESS60 ticks amp2 (Slowness III), visible defaults. Native eligible living targets/ally/role guards replace effect commands on invalid entities.|end_rod30 normal at ownerY1 spread.5/1/.5 speed.2; illusioner.cast_spell master globally volume1/pitch1.|
|2 bigjump:1-4|ALL !spectator entities radius<=7 INCLUDING OWNER receive LEVITATION20 ticks amp10 hidden; owner SLOW_FALLING200 ticks amp0 hidden. Source no team filter; native self explicitly admitted, allied PLAYERS protected, friendly mobs remain eligible. No separate direct damage.|cloud50 normal at ownerfeet spread.3/.1/.3 speed.1; wool.step master globally volume1/pitch.5.|
|3 kombojump:1-12|Ordered active-Jump-Boost ladder below; all normal visible particle/icon grants. No target scan/charge/mana.|wither.shoot MASTER only @s volume.7/pitch1; poof6 normal ownerfeet spread.4/.4/.2 speed.1.|

Native addEffect source must retain caster/owner for levitation/slowness and deferred fall/void consequences. Do not call bigjump WILD or ask for a new friendly-fire exception: established default allied-player protection resolves source teamless selector. Greed/role guards apply to harmful effects through current common policies; owner slow falling/boost follows existing self-effect admission.

## Combo jump ordered ladder and signed layers

`other/artifact/kombojump.mcfunction` uses INDEPENDENT commands with descending exact ACTIVE amplifier matches (not else-if, not hidden nested levels):

|Line|Condition and grant|
|---|---|
|1|active Jump Boost amp32 -> Slow Falling600 ticks amp0|
|2|active amp32 -> Jump Boost160 ticks amp64|
|3|active amp16 -> Slow Falling400 ticks amp0|
|4|active amp16 -> Jump Boost200 ticks amp32|
|5|active amp8 -> Jump Boost200 amp16|
|6|active amp4 -> Jump Boost200 amp8|
|7|active amp2 -> Jump Boost200 amp4|
|8|active amp1 -> Jump Boost200 amp2|
|9|ANY active Jump Boost -> Jump Boost200 amp1|
|10|UNCONDITIONAL Jump Boost200 amp0|

No active boost: only line10 amp0. Next jump active0: lines9+10 gives amp1 plus lower0 layer. Active1/2/4/8 progress2/4/8/16 then lines9/10 add or refresh lower1/0 layers. Active16 grants slowfall20s then32 and lower1/0. Active32 grants slowfall30s then64 for8s and lower1/0 (10s). Active64 has no exact branch, only lower1/0 refresh; stronger64 retains its existing remaining duration, not refreshed8s. Other signed/high/intermediate amplifiers use generic9/10 without normalization; do not clamp -1 to0 or pretend all positive levels double. Native independently ticking layers/longer same-level max duration rules are required. A snapshot of active amplifier can choose the one exact upper branch, but still must perform later generic grants in order, including the absent-effect case distinction.

## Old mystical_old3 three behaviors

Inline armor:11 type1 -> self SLOW_FALLING60 ticks amp3 hidden; :12 type2 -> self LEVITATION20 ticks amp4 hidden; :13 type3 -> self SPEED60 ticks amp4 hidden. No sound/particle feedback is specified for old types. These are different semantics, not aliases of highspeed/bigjump/combo. Old local suppression is absent, though established native match/action/Time Stop/role/Greed policy remains authoritative. Old1 slowfall amplifier3 must retain signed effect semantics rather than silently collapse to0.

No damage helper, carrier, terrain mutation, mana, private cooldown, HUD or recursion path occurs in the six jump-artifact branches. Removing an item does not erase already-granted effects. Ordinary duration layering and existing purify/match reset are enough; family should not clear another item's same effect layer merely because inventory changed.

## Six exact item leaves and acquisition

Both table families contain three fixed typed leaves, not one leaf rerolled on each jump. `loot_table/artifact.json`: repeating_command_block/count1/model lbc:mysticalartifact, tier_u; name array mystical_artifact #C28CFF italicfalse; lore row array type+accessory aqua italicfalse, then mystical_artifact.lore red italicfalse. Custom data transformable1/accessory1/unresolved_artifact1/mystical3/typeN. Weight10 each; qualities type1=5,type2=0,type3=10. No attribute/durability override.

`artifact_old.json` same base/count/model/name/weight/quality, NO tooltip_style; only one lore mystical_artifact.lore red italicfalse, no type row. Flags mystical_old3/typeN only; no transformable/accessory/unresolved. Preserve exact vanilla proxy/item components; no brand-new model/texture/key is needed.

Current artifact table referenced by transformables_accessory:464,485; ordinary accessory transform/reforge routes can supply current artifacts. Legacy_luckyblock:3197 references artifact_old. No dedicated ingredient craft or kit for mystical3 was found by full flag/helper search. Source random artifact pool contains OTHER unimplemented families: importing six leaves does not authorize enabling full pool. Explicit existing catalog/operator leaf routes and individually qualified transform-pool filtering are separate integration choices.

Looper source `swrg_kit_integration/game/skills/looper_explain_artifact.mcfunction:16-18` maps current type1/2/3 to artifact_15/16/17 yellow italicfalse in lore[1], removes unresolved_artifact afterward and writes item back. Existing LbcSkills already maps15..17; reuse exact author explanation/recipient policy, no duplicate gameplay. Old leaves lack unresolved flag/current mystical so do not receive current explanation by aliasing. No mystical3-specific advancement/grant exists in source helper/dispatcher closure; general artifact/Looper achievements remain outside this bounded group.

## Integration checklist without new author blockers

Implement one inventory type-set snapshot per jump and execute deterministic current1/2/3 then old1/2/3. Reuse native addEffect/ItemEffectLevels, owner cause, shared role/Greed/ally guards and Time Stop; read current owner pose for source particles. Current suppression gates are separate from old local gates. Bigjump self included explicitly while allies use default protection. Use existing native jump TAIL so current jump motion is unchanged and effects apply after it. Exact six source leaves and lore reuse suffice; no RP/template edits. This group has no remaining HurtTime target-identity ambiguity or special WILD authorization requirement. Implementation/test qualification remains future work, not inferred from audit.

## Original-byte SHA256 ledger

- `function/items/armor.mcfunction` `9d0141c94439147e77eb3a1184cf00795567ca75655ae59eb537b312d759fac6`
- `function/other/artifact/when_jump.mcfunction` `aad975abe7c18ce874b11f4fe04de63d65e863f8ae04d04397a37862b31a6cac`
- `function/other/artifact/highspeed.mcfunction` `9abb029650c9e05fbef273029d97942e87b21515a7268b67377a0d5ae68bcf1b`
- `function/other/artifact/bigjump.mcfunction` `2a828bd41fce7b12fe5d8fa755e97da4f473f5c6747c375fd4c1c0ce0bc86575`
- `function/other/artifact/kombojump.mcfunction` `19cf6679cd562b77de57adfa89911086fb1da3a1743fd52197f3d09ef72c9610`
- `function/load.mcfunction` `b2b4c8d0f4fd6d8a9464262368188d4a26d71bf29f7f931b423658c7a763f6e1`
- `function/load_scores_reload.mcfunction` `61c7e5ff20e288f9e060f7acad5ae44e17a2fdf5f7f186d19223b1a9998456bb`
- `function/players_tick.mcfunction` `851491714a1ed2831411a19a829d362e051fdc4b2d5b3db581318e01d383c483`
- `function/mobs_tick.mcfunction` `70fa3e4075cb14b3f9607c59b06fcdfaa23f6cd101d1b3b72e7fa49a8dc5d9c8`
- `function/tick.mcfunction` `86e363bdb2197ab7a44dcdd892d719b07259a3772de71dbf03ac12fc46668965`
- `function/tick_not_timestopped.mcfunction` `55faddfdca07e3a486d0f826e9593969ae5fa05260cb2392c162569efa8cf5cc`
- `loot_table/artifact.json` `04783e32c1a8935a3849a493fda7b885967835bf8749068965cb09cfc84052db`
- `loot_table/artifact_old.json` `5e75b1c35d90eacfb847757d6533f4cafc38919923de2d9e8003f2864bf1bbfd`
- `loot_table/transformables_accessory.json` `3792e70b859e718276c7000dcc81bd055462cf8600b5833389efd2a5f53e1524`
- `loot_table/legacy_luckyblock.json` `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- `function/swrg_kit_integration/game/skills/looper_explain_artifact.mcfunction` `87b3470eac2f167161af01e5523ed9f5afb803b2ec477366f0e358a925bdeeaa`

## Read-only RP exact text/model evidence

`en_us` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "mystical_artifact": "Mystical Artifact",
  "mystical_artifact.lore": "Unknown effect",
  "artifact_15": "Speed up on jump",
  "artifact_16": "High jump + throws mobs in air on jump",
  "artifact_17": "Jumping combo"
}
```

`ru_ru` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "mystical_artifact": "Мистический Артефакт",
  "mystical_artifact.lore": "Неизвестный эффект",
  "artifact_15": "Прыжок - Скорость",
  "artifact_16": "Высокий прыжок с подкидыванием",
  "artifact_17": "Распрыжка"
}
```

`assets/lbc/items/mysticalartifact.json` SHA256 `de862c559b494df5271be0f9a59a4c69f3804d56a0b574d33298800aa246a562`; {     "model": {         "type": "model",         "model": "lbc:item/mysticalartifact"     } }

`assets/lbc/models/item/mysticalartifact.json` SHA256 `f8bfd2b5ea27191c6f1553cbb596956645298949df83be2c179678f0db37517b`; {   "parent": "minecraft:item/generated",   "textures": {     "layer0": "minecraft:item/mysticalartifact"   } }
## Additional explicit producers outside this release

Whole-source follow-up also finds direct calls to `items/armor` in
`drops/tmt/trigger_smth`, `trigger_smth_not_bow` and `trigger_smth_not_crossbow`.
The native jump service exposes a reusable entry point for those future
producers; this release connects only the actual native jump event. It does
not enable or qualify those TMT trigger items.
