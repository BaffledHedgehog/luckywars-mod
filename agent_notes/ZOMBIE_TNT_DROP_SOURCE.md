# Zombie TNT drop - source audit, 22 October 2026

Read-only audit; no gameplay/build/runtime/RP changes or execution qualification.

Subsequent author decisions and implementation status are recorded in
[ZOMBIE_TNT_DROP_PORT.md](ZOMBIE_TNT_DROP_PORT.md). The author disabled carrier
melee completely and selected WILD TNT damage, including thrower/allied players.

## Source contract

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
`function/drops/zombietnt.mcfunction:1..4` summons exactly four native zombies at token-relative (+1,0,0), (0,0,+1), (-1,0,0), (0,0,-1), in that order. Every zombie has exactly `IsBaby:1b`, `Passengers:[{id:"minecraft:tnt",fuse:200}]`, `attributes:[{id:"minecraft:attack_damage",base:-10}]`. No alignment, motion, equipment, health, AI, name, tags, owner, persistence, death table or invulnerability override. No source custom TTL or cleanup on author's death/disconnect.

`function/items_tick_act.mcfunction:1` dispatches tag=drop through `drops/specialdrops`. Its line11 selects custom_data zombietnt:1, after normal/old corrosion and before sethome; the shared function finishes with kill(count1) / count*0.99999 debit. A multi-flag token executes all matching branches before one debit. Separate `shahid:1` near the end summons ONE identical carrier; do not silently merge that family into this four-carrier token.

## Exact token presentation

`loot_table/traps.json:842..884`: item repeating_command_block, count1, weight10, quality-10; components tooltip_style=`lbc:tier_ff`, item_model=`lbc:drop_trap`. Name is one component translate=`trap_tnt_zombies`, italic=false, color=yellow. Lore is one row translate=`trap_desc`, italic=false, color=white, replace_all. Custom data `{zombietnt:1,drop:1}`.

`loot_table/imba/traps.json:385..404`: repeating_command_block/count1, weight0, quality-10, set_components `{}`, custom data `{zombietnt:1,drop:1,imba:1}`; no name/lore/model/tooltip style. It is a zero-weight source entry, not an evidenced available decorated variant. No separate IMBA summon behavior.

Current read-only RP checked at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (explicit current location supersedes old documentation path). `assets/minecraft/lang/{ru_ru,rpr}.json:2604,2609`: `Ловушка: Зомби-Подрывники`; `Активный Дроп. Для активации выбросте предмет.` Preserve source typo. en_us: `Trap: Zombie Bombers`; `Active Drop. Throw the item to activate.` `assets/lbc/items/drop_trap.json` resolves model `lbc:item/drop_trap`; model parent minecraft:item/generated, layer0 minecraft:item/drop_trap. Existing resources suffice; no template change identified. Real-client appearance untested.

## Native 26.3 evidence

Local source archive `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:

- `world/entity/item/PrimedTnt.java:95..160`: gravity .04, native tick motion/drag/bounce, fuse decremented even when riding, discard then native TNT explosion at x,y+height*.0625,z; default power4, no fire, TNT interaction, TNT_EXPLODES gamerule. Fuse save/load short; default80, explicitly set200 here. Owner save/load exists but source supplies none. Avoid constructor's ordinary priming kick as an invented source impulse.
- `world/entity/Entity.java:2430..2467`: rideTick zeros velocity, calls native tick, then vehicle.positionRider if still mounted. Thus fuse continues riding; detonation happens during tick before final rider reposition, so manually pinning TNT each tick changes native timing/position.
- `world/entity/monster/zombie/Zombie.java:91..93,143..149,200..207,408..409`: baby dimensions .49x.98, eye .775, baby movement modifier +50% base; follow35, movement .23, armor2. Baby attachment builder sets VEHICLE .1875; PASSENGER fallback is full baby height .98 (`EntityAttachment.java`, `EntityAttachments.Builder.build`). TNT's vehicle fallback feet0 gives mounted feet nominally zombieY+.98 at default scale. `EntityTypes.java:942..948`: TNT .98x.98, eye .15. Use native dimensions/attachments, not adult zombie riding offset.
- `world/entity/ai/attributes/Attributes.java:17` clamps vanilla attack_damage to0..2048. Mod `src/main/java/me/skywars2/mixin/AttributeLimitsMixin.java` bypasses bounds for finite values, so base-10 stays negative. `Mob.java:1371..1380` passes that through weapon/enchantment calculation into hurtServer; `LivingEntity.java` hurtServer explicitly clamps damage<0 to0. Therefore do NOT claim negative damage heals. Zero-damage attack may still run native hit/cooldown/retaliation/knockback and Zombie's burning-attacker ignition path if hit returns true (`Zombie.java:307..316`); mod hooks require fixture verification.

## Other source interactions and migration gaps

No dedicated tag identifies these zombies/TNT and no additional zombietnt-specific tick function was found. Generic interactions can still affect them: `projectilestick.mcfunction:6` routes native TNT to `projectilestick_tnt` (fuse-zero daleko cleanup, tag-gated special effects); `other/artifact/notnt_old.mcfunction:1,5` converts nearby TNT to item and kills it. `workingitems/fireworktnt.mcfunction:42..45` rewrites nearby TNT motion/owner/team; `workingitems/tntbow1.mcfunction:6,8` globally touches TNT when invoked. Existing global protections, time stop, reflection and chain ownership must apply once through established native hooks; tagged other-zombie handlers (transformator/otchim/muravei) do not select these fresh untagged carriers.

Ordinary native zombie AI, sunlight/fire, targeting, despawn, death drops/XP and drowning conversion are not suppressed in source. Carrier death does not explicitly disarm passenger; native dismount/fuse behavior needs fixture coverage. Native surviving carriers can remain after detonation. No custom reward is declared. Do not add empty death loot, kill-triggered extra TNT, reward, owner lifetime, forced persistence or healing based on assumptions.

Source has no causal thrower ownership. Required user contract in agent_notes/AGENTS.md demands migration attribution for direct and delayed consequences: carrier hit/knockback/fire, mounted/dismounted TNT, chain TNT and terrain/fall/void must retain actual token author, with later reflection/attacker precedence. Quiet epoch/match cleanup must discard both entities without death loot/XP or explosion. Retain cause across unload/conversion where supported; source does not prove restart or unload guarantees. Combat-death native rewards and mechanism suppression must be distinguished from quiet cleanup.

## Questions before implementation

1. Is this a native zero-damage attacking baby zombie (including native knockback/hit/fire side effects), or should melee attacks be completely disabled? Preserve base-10 only after accounting for global unbounded attributes; healing is not native26.3 behavior.
2. Confirm ordinary surviving carriers/death loot/XP, sunlight and drowning conversion, and TNT remaining live after carrier death; source has no disarm/death/reward override.
3. Apply established WILD trap damage to self/allies and ordinary hostile AI targets, while preserving server-role protection? Previous trap decisions suggest this, but there is no zombie-specific author answer in this audit.
4. Should the zero-weight undecorated IMBA placeholder be exposed at all? Do not invent a stronger/decorated variant without an author decision.
