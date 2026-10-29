# Legacy effect consumers - 29 October 2026

Staged source implementation, NOT public readiness or full-chain qualification. No build,
server, resource, RP or commit was performed by this agent. Owned files are this note
and `src/main/java/me/skywars2/game/LbcLegacyEffects.java` only.

## Source closure and producer

Original read-only root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.
`lbc/function/random_debuff.mcfunction` draws world RNG uniformly from 1..47,
writes fake `randebuff lbc.math`, adds `have_lucky_effects`,
`have_lucky_effects_1s`, `have_lucky_effects_5t`, then returns after exactly one
selection. Native `randomDebuff(ServerPlayer)` and `applyDebuff(player,1..47)`
share the actual producer. The subsequent Oreshek random draws remain native.

| Selection | Original operation |
| --- | --- |
| 1..17 | blindness, darkness, glowing, hunger, infested, instant_damage, levitation, mining_fatigue, nausea, oozing, poison, slowness, unluck, weakness, weaving, wind_charged, wither |
| 18..27 | +30 cold, cold_cd, delayed_death, effect_big_explosion, effect_evoker, effect_evokerarrow, effect_explosion, effect_firebreak, effect_gravity_curse, effect_hedgehog_boom |
| 28..37 | +30 effect_kiuaskivi, effect_lightarrow, effect_lightning_bolt, effect_nuking, effect_remove_mana, effect_small_explosion, effect_snus, effect_stand_arrow_hit, effect_traps, effect_witch_gens_reset |
| 38..47 | +30 frozen, gigaponos, lookback, nomagic, piss, scared, shock, skill_issue, target, twitching |

Vanilla `/effect give` defaults, verified in mapped 26.3 `EffectCommands`, are
600 ticks/amp0/visible for the sixteen noninstant selections and 1 tick/amp0 for
instant damage. Existing values are added to, never replaced with30 or clamped.
Native shared item-amplitude policy applies through `ItemEffectLevels`.
A raw counter addition immediately calls the central `LbcItems.adoptRawFoodStatuses`.
No duplicate BadWiki/EnderWiki/Psycho random-debuff selector exists: their other
random pools are different source functions.

## Consumers and real shared engines

Original dispatchers: `effects/tick`, `effects/tick_1s`, `effects/tick_5t`, called
by `mobs_tick`, `mobs_1s`, `mobs_5t`. The following actions run every native tick,
except gravity (20 ticks). Every consumed positive score decreases by one even
when its individual jammer condition prevents the action. Greed pauses all listed
branches except `effect_stand_arrow_hit`, whose source dispatcher has no Greed
exclusion. No generic counter is given a second private lifetime.

| Score | Exact reachable original operation / native owner |
| --- | --- |
| effect_big_explosion | `projectiles/arrows/bigexplode`:7 radius10 MOB explosions at center,z+4,z-4,x-4,x+4,y-4,y+4. Raw type2 mystical item in ANY player inventory within12 or TECH stopper within20 suppresses action. `LbcBabylonEffects.bigExplosionAt` reuses native OwnerDamageSource/explosion/causal child engine; no creepers. |
| effect_explosion | Same suppression; ordinary native TNT fuse2. Existing `primeExplosionAt` owns actual native child lifecycle. |
| effect_nuking | Only TECH stopper within20; `other/nuke/nuke1_at` goes to `LbcNukeEvent.pointBurst`. This is NOT bruh or Exasperato:12 air templates (6 at25 integrity.5,6 at60 integrity.25),24 radius25 blasts at horizontal39/18 and vertical0/+40/-40,400-tick nuclear visual,Wither radius45,kill radius14. Root owns exact shared point variant implementation and its qualification. |
| effect_traps | Unjammed source `loot spawn ... lbc:traps`; `chaosTraps.spawnUnjammed` uses same25-entry native chest table + causal shared SpecialDrops activation. No armor MAG gate. Full25 transitive release gate still applies. |
| effect_kiuaskivi | Only MAG stopper20 suppresses the action, not decrement. Existing `LbcKiuas` owns exact 3x2x3 air/fire tagged fills,flame particles,cause,counter; root adds raw adoption. No second terrain engine. |
| effect_remove_mana | Existing `LbcMana.loaded` adopts its score. Existing badPotion draws1..10,always mana-2000,roll<=8 manamax-1000,roll<=6 manaregen-1,then drains once. Source helper has no jammer condition. Root owns existing mana policy/counter adoption. |
| effect_snus | Broadcast green executor display name + translation snus.used; clear exactly17 listed effects; native Speed4000amp15,Jump4000amp15,Nausea5000amp0,Fatigue5000amp2,Hunger8000amp0 then Hunger320amp10,NightVision5000amp0. Native layered Hunger is retained; source snus advancement awarded. No jammer gate. |
| effect_stand_arrow_hit | Only MAG stopper20; `hit_pre` executes hit then returns one source Stand arrow. Shared Stand engine draw1..100:1..10 kill unlessGreed;11..100 addhave_stand+oraora unlessalreadyhave/overheaven. Source subsequent no_stand condition becomes false after adding have_stand. Root bridge reuses existing resolver and native arrow loot. |
| effect_witch_gens_reset | Only MAG stopper20; dmg_dealt=1;food_count,lolidefecation,sleep_time,greed_prestige=0;addhave_mom;removegreed_cursed. No new status engine. |
| effect_hedgehog_boom | Only MAG stopper20; `lbcr:raycast_loop9cd_upgrade`:5 native13-silverfish passenger towers,65total,local side1,2,-1,-2,0 plus forward1. Existing `hedgehog.spawnWall` owns exact payload,loot,broadcast and150-wallboss threshold. |
| effect_gravity_curse | Every20 ticks,at score1 remove minecraft:gravity_curse modifiers from FALL_DAMAGE_MULTIPLIER,GRAVITY,STEP_HEIGHT,SAFE_FALL_DISTANCE,JUMP_STRENGTH,MOVEMENT_SPEED; then decrement and taghave_lucky_effects_1s. Bare +30 does NOT create physical modifiers. Registered PendingCombat curse remains sole owner,root makes its player score authoritative. Generic raw handler skips gravityCurseActive. |
| skill_issue | Existing LbcItems TurnCurse owns rotation history,hurt,blindness,lifetime,advancement. Root adopts raw value instead of adding another turn engine. Existing explicit author decision says this psychic disorder ignores both jammers; preserve that override of players_tick source condition. |

The raw type2 inventory test deliberately reads custom_data directly, including
holders whose artifact behavior is itself suppressed; source NBT condition does
not query an active artifact index. The new generic counters have WILD authors
through the existing common causal helpers. No new damage,blast,raycast,trap or
mob engine is introduced.

The remaining47-producer results stay with existing services: Frost(cold/cold_cd/
frozen),AcidCorruption(delayeddeath),Babylon3/Quiver4 effects,KFC(gigaponos),
Psycho(lookback/piss/scared/twitching),Magic(nomagic),Electro(shock),Blind(target),
plus existing TurnCurse(skill_issue). Root central adoption owns their exact state
bridges and must qualify the complete47 runtime closure independently.

## Admission, lifecycle and unresolved policy

The shared native role/dimension/TimeStop admission applies: live gameplay author,
ordinary dimension,nonobserver,valid role,match active,not ending,no global or
per-entity stopped input. No blanket MAG or TECH gate is added to the producer.
Greed is checked only on source-excluded action branches; Stand branch still
runs underGreed and its own kill explicitly excludesGreed.
`clear/forget/stop` clear only this service's owned scores plus raw gravity;
shared registered engine cleanup stays with its existing owner. `tick` uses the
actual server tick once,never advances a fabricated production clock.

UNRESOLVED for central author-policy resolution/full release gate:
1. Stand shared resolveArrowHit still checks artifact/MAG at eye position; the raw
   effect wrapper source only checks stopper_magic at feet. Root explicitly
   retains existing common admission pending the author-policy decision.
2. Hedgehog shared spawn/valid wall admission includes artifact suppression,
   while this source wrapper only checks stopper_magic. Root likewise retains
   existing family policy instead of adding a duplicate engine or unapproved
   bypass. This affects actual downstream output and must not be called exact
   complete under artifact/eye boundary cases.
3. Existing mana,Kiuas and other adopted status services retain their established
   common role/magic policies. Root owns checking those against source-only
   no-jammer/raw-counter scripts and recording any author overrides.
4. Complete pointBurst blast cascade/terrain/performance and full47 native branch
   closure are not qualified by this source implementation. Existing Nuke event
   bounded warning proof does not qualify this different point variant.

No new resources or RP assets are introduced here. Existing source snus and Stand
advancement/loot packaging is parent-owned. `snus.used` is an existing source key;
no replacement translations or textures were written.
