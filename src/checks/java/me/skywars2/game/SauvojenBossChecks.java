package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.validation.mixin.NativeFixtureAccess;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.LbcSpellSource;
import me.skywars2.game.entities.mobs.LbcBossLoot;
import me.skywars2.game.entities.mobs.LbcSauvojenBoss;
import me.skywars2.game.entities.projectiles.LbcClouds;
import me.skywars2.game.items.trinkets.LbcInfinity;
import me.skywars2.game.items.wands.LbcAir;
import me.skywars2.game.items.wands.LbcBlind;
import me.skywars2.game.items.wands.LbcRuby;
import me.skywars2.game.items.wands.LbcSauvojen;
import me.skywars2.game.items.wands.LbcStone;
import me.skywars2.game.items.wands.LbcTeleport;
import me.skywars2.game.items.wands.LbcTerra;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.skills.witch.LbcGreed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** Bounded real-frame activation/payload checks; a typed healthy-MSPT scope isolates functional admission. */
public final class SauvojenBossChecks {
    private SauvojenBossChecks(){}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException("Sauvojen boss: "+detail);}
    private static void near(Vec3 actual,Vec3 expected,String detail){require(actual.distanceToSqr(expected)<1e-8,detail+" actual="+actual+" expected="+expected);}
    private static CompoundTag saved(Entity entity){var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,entity.registryAccess());entity.saveWithoutId(out);return out.buildResult();}
    static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,beta;
        final GameChecks.WandThirdPlayerState otherSaved;final PlayerTeam oldOwnerTeam,oldBetaTeam,team;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Cow> cows=new ArrayList<>();LivingEntity boss;LbcSpellSource.Source authority;
        Fixture(GameController game){
            this.game=game;require(game.items().sauvojenBoss().count()==0&&game.items().sauvojenBoss().cloudCount()==0,"exclusive empty boss service");
            require(game.items().spellSources().current()==null,"no ambient source scope");
            base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            otherSaved=new GameChecks.WandThirdPlayerState(beta,base.mana);oldOwnerTeam=owner.getTeam();oldBetaTeam=beta.getTeam();
            var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_boss_source")==null,"exclusive source fixture team");
            team=board.addPlayerTeam("sw2_boss_source");owner.level().getAllEntities().forEach(before::add);
            base.initialize();board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
            beta.setNoGravity(true);beta.setDeltaMovement(Vec3.ZERO);beta.teleportTo(owner.level(),owner.getX(),owner.getY(),owner.getZ()+4,Set.of(),0,0,true);
        }
        void activation(){activation(owner);}
        void ownerlessActivation(boolean restored){
            ItemStack selected=ItemStack.EMPTY;
            for(int attempt=0;attempt<512&&selected.isEmpty();attempt++)for(var stack:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:traps"))
                if(LbcSauvojenBoss.recognized(stack)){selected=stack.copyWithCount(1);break;}
            require(!selected.isEmpty()&&selected.is(Items.REPEATING_COMMAND_BLOCK),"actual raw trap matches the captured repeating-command-block carrier");
            {
                var level=owner.level();var at=owner.position();
                var token=new ItemEntity(level,at.x,at.y,at.z,selected.copy());
                token.setOnGround(true);token.setNoGravity(true);token.setPickUpDelay(0);
                token.entityTags().add(LbcSpellSource.WILD_LOOT);game.items().markCurrentItemEpoch(token);
                if(restored){
                    // Same relevant native NBT as the report: one raw trigger, Age1528,
                    // zero pickup delay and no Thrower/owner. Do not invent a nearby owner.
                    var tag=saved(token);tag.putString("id","minecraft:item");tag.putShort("Age",(short)1528);
                    token=(ItemEntity)EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag),
                            level,EntitySpawnReason.LOAD,entity->entity);
                    require(token!=null&&token.getAge()==1528,"native saved-body recreation retains reported age");
                }
                require(level.addFreshEntity(token)&&token.getOwner()==null&&game.items().specialDrops().owner(token)==null,
                        "published WILD trap has no player owner or borrowed attribution");
                long started=System.nanoTime();token.playerTouch(owner);
                SkyWars2.LOGGER.info("[check] Sauvojen ownerless restored={} nativeTick={} pickupMs={} nativeAverageMs={}",restored,game.server().getTickCount(),(System.nanoTime()-started)/1e6,game.server().getAverageTickTimeNanos()/1e6);
                require(token.isRemoved()&&game.items().sauvojenBoss().count()==1,
                        "ownerless captured trap activates during native pickup admission instead of waiting forever; restored="+restored);
                var spawned=game.items().sauvojenBoss().snapshot().getFirst();
                near(spawned.position(),at.add(0,20,0),"ownerless boss spawns above the token in its own level");
                require(spawned.level()==level&&game.items().sauvojenBoss().owner(spawned)==null,
                        "no fabricated player owner for raw or restored trap");
                var source=game.items().spellSources().source(spawned);
                require(source!=null&&source.author()==null&&source.unownedMobs()&&source.persistent()
                        &&game.items().sauvojenBoss().canAffect(spawned,owner)&&game.items().sauvojenBoss().canAffect(spawned,beta),
                        "ownerless boss remains WILD against both allied players");
                require(!game.items().specialDrops().spawn(token)&&game.items().sauvojenBoss().count()==1,
                        "consumed single token cannot create a second boss");
                game.items().sauvojenBoss().stop();
            }
        }
        void activation(ServerPlayer creator){
            ItemStack selected=ItemStack.EMPTY;
            for(int attempt=0;attempt<512&&selected.isEmpty();attempt++)for(var stack:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:traps"))
                if(LbcSauvojenBoss.recognized(stack)){selected=stack.copyWithCount(2);break;}
            require(!selected.isEmpty(),"original native trap table reaches Sauvojen raw branch");
            var token=new ItemEntity(owner.level(),owner.getX(),owner.getY(),owner.getZ(),selected);token.setThrower(creator);token.setOnGround(true);token.setNoGravity(true);
            game.items().markCurrentItemEpoch(token);require(owner.level().addFreshEntity(token),"actual trap token publication");
            require(game.items().sauvojenBoss().ready(token)&&game.items().specialDrops().spawn(token),"actual shared native special-drop dispatch");
            require(token.getItem().getCount()==1&&game.items().sauvojenBoss().count()==1,"one native boss and exactly one token debit");token.discard();
            boss=(LivingEntity)game.items().sauvojenBoss().snapshot().getFirst();((Mob)boss).setNoAi(true);boss.setNoGravity(true);
            require(boss.getType()==EntityTypes.GHAST&&boss.getHealth()==124&&boss.getMaxHealth()==124,"source ghast with real124HP and no loot-survival sentinel");
            require(boss.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue()==256&&boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue()==256,"source follow/knockback bases");
            var data=saved(boss);require(data.getStringOr("DeathLootTable","").equals("minecraft:empty")&&data.getIntOr("ExplosionPower",0)==3,"native guarded death owns reward without a delayed duplicate token; blast3");
            require(boss.entityTags().containsAll(Set.of("sauvojen_tuntija","rcdenyalways"))&&game.items().sauvojenBoss().owner(boss)==null,"WILD boss never adopts the trigger's player as its owner");
            near(boss.position(),owner.position().add(0,20,0),"source Y+20 spawn");
            authority=game.items().spellSources().source(boss);require(authority!=null&&authority.emitter().equals(boss.getUUID())&&authority.author()==null&&authority.persistent()&&authority.unownedMobs(),"persistent actual NPC emitter without borrowed player authority");
        }
        LbcSpellSource.Source source(LbcSauvojen.Kind kind){
            var excluded=new HashSet<>(authority.excluded());if(kind==LbcSauvojen.Kind.TERRA||kind==LbcSauvojen.Kind.ELECTRO)excluded.add("summoned_cultist");
            return new LbcSpellSource.Source(authority.emitter(),authority.author(),authority.epoch(),authority.cause(),excluded,authority.persistent(),authority.unownedMobs(),authority.nativeEmitter(),authority.helmetCredit());
        }
        Projectile shot(LbcSauvojen.Kind kind){
            return switch(kind){
                case RUBY->game.items().ruby().snapshot().getFirst();
                case TERRA->{Projectile found=null;for(var e:owner.level().getAllEntities())if(e instanceof LbcTerra.TerraProjectile p&&!p.isRemoved())found=p;yield Objects.requireNonNull(found);}
                case AIR->game.items().air().snapshot().getFirst();case ACID->game.items().clouds().acidSnapshot().getFirst();
                case TELEPORT->game.items().teleport().snapshot().getFirst();case STONE->game.items().stone().snapshot().getFirst();
                case FIRE->base.fire().getFirst();case BLIND->game.items().blind().snapshot().getFirst();default->throw new IllegalArgumentException();
            };
        }
        void payloads(){
            var origin=owner.position().add(4,4,-4);var direction=new Vec3(1,0,0);base.mana.set(owner,12345);
            for(var kind:LbcSauvojen.Kind.values()){
                var policy=source(kind);game.items().sauvojen().emitHostile(owner.level(),kind,origin,direction,policy);
                require(game.items().spellSources().current()==null&&base.mana.get(owner)==12345,"producer restores source scope and never charges mana "+kind);
                if(kind==LbcSauvojen.Kind.ELECTRO){
                    var traces=game.items().wands().electroSnapshot();require(traces.size()==4,"four real hostile electrical records");
                    for(var trace:traces){require(trace.lifetime==20&&trace.age==0&&trace.sourcePolicy==policy&&trace.owner==null&&trace.explicitEmitter==boss,"four source20-tick electrical records");near(trace.position,origin,"electrical explicit origin");}
                    game.items().wands().tick();for(var trace:traces){require(trace.age==1,"one actual shared electrical engine step");near(trace.position,origin.add(3.6,0,0),"source nineteen-position electrical segment");}
                }else{
                    var shot=shot(kind);require(game.items().currentEpoch(shot)&&game.items().spellSources().owner(shot)==null,"actual charge has no player author "+kind);
                    require(game.items().spellSources().source(shot)!=null,"actual charge keeps explicit hostile policy "+kind);near(shot.position(),origin,"native launch origin "+kind);
                    double speed=switch(kind){case AIR->.8;case TELEPORT->1.2;case STONE->1.6;case FIRE->.7;case BLIND->.99;case ACID->.5;default->1.8;};
                    near(shot.getDeltaMovement(),direction.scale(speed),"source velocity "+kind);
                    switch(kind){
                        case RUBY->{require(((LbcRuby.Charge)shot).sourceLifetime==60,"Ruby source60");game.items().ruby().tick();}
                        case TERRA->{require(((LbcTerra.TerraProjectile)shot).sourceLifetime==60,"Terra source60");game.items().terra().tick();}
                        case AIR->{require(((LbcAir.Charge)shot).sourceLifetime==120,"Air source120");game.items().air().tick();}
                        case ACID->{require(LbcClouds.acidLifetime((LbcClouds.AcidProjectile)shot)==300&&shot.getOwner()==null&&shot.entityTags().contains("shield_blockable2"),"source300 acid independent owner/shield");owner.level().tickNonPassenger(shot);game.items().clouds().tick();require(game.items().clouds().acidAge((LbcClouds.AcidProjectile)shot)==1,"actual source acid service age");}
                        case TELEPORT->{require(((LbcTeleport.Charge)shot).sourceLifetime==60,"Teleport source60");game.items().teleport().tick();}
                        case STONE->{require(((LbcStone.Charge)shot).lifetime==80,"Stone source80");game.items().stone().tick();}
                        case FIRE->{require(shot.getOwner()==boss,"native owner immunity belongs to actual emitter");owner.level().tickNonPassenger(shot);}
                        case BLIND->{require(((LbcBlind.Charge)shot).lifetime==120,"Blind source120");game.items().blind().tick();}
                        default->throw new IllegalStateException();
                    }
                    require(!shot.isRemoved(),"actual source charge survives one unobstructed native step "+kind);
                    if(kind!=LbcSauvojen.Kind.ACID)near(shot.position(),origin.add(direction.scale(kind==LbcSauvojen.Kind.FIRE?(speed+.1)*(double).95F:speed)),"actual engine movement "+kind);
                }
                require(game.items().spellSources().current()==null,"async engine restores source scope "+kind);base.clear();
            }
        }
        Cow cow(Vec3 at){var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(owner.level().addFreshEntity(cow),"actual living target");cows.add(cow);return cow;}
        void incomingHits(){
            var level=owner.level();var at=boss.position();float health=boss.getHealth();
            var held=owner.getMainHandItem().copy();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            try{boss.removeEffect(MobEffects.RESISTANCE);boss.setInvulnerableTime(0);boss.damageCooldownTime=0;owner.attack(boss);
                require(boss.getHealth()<health,"actual ordinary native player melee damages managed Sauvojen");}
            finally{owner.setItemInHand(InteractionHand.MAIN_HAND,held);boss.setHealth(health);boss.setInvulnerableTime(0);boss.damageCooldownTime=0;}
            var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(level,owner,new ItemStack(Items.ARROW),null);
            arrow.setPos(at.add(0,2,-5));arrow.setDeltaMovement(0,0,6);arrow.setNoGravity(true);
            require(game.items().wands().canHit(arrow,boss)&&level.addFreshEntity(arrow),"native projectile policy admits boss despite source ray-denial tags");
            try{arrow.tick();require(boss.getHealth()<health,"real ordinary arrow flight damages Sauvojen");}
            finally{arrow.discard();boss.setHealth(health);boss.setInvulnerableTime(0);boss.damageCooldownTime=0;}
            var helicopters=game.items().helicopterEncounter();require(helicopters.activeCount()==0,"exclusive helicopter fixture");
            var token=new ItemEntity(level,owner.getX(),owner.getY(),owner.getZ(),new ItemStack(me.skywars2.game.entities.mobs.LbcHelicopterTriggerItem.TOKEN));
            token.setNoGravity(true);require(level.addFreshEntity(token)&&helicopters.spawn(token),"actual hostile helicopter trigger");
            try{
                // Its native muzzle starts8.5 blocks forward. Keep the boss beyond it.
                var pair=helicopters.snapshot().getFirst();pair.display().setPos(at.add(0,2,-20));pair.hitbox().setPos(at.add(0,-1,-20));
                for(int tick=0;tick<5&&helicopters.attackSnapshot().isEmpty();tick++)helicopters.tick();
                var shot=helicopters.attackSnapshot().stream().filter(net.minecraft.world.entity.projectile.arrow.Arrow.class::isInstance)
                        .map(net.minecraft.world.entity.projectile.arrow.Arrow.class::cast).findFirst().orElseThrow();
                require(helicopters.canHit(shot,boss)&&shot.getDeltaMovement().normalize().dot(boss.getEyePosition().subtract(shot.position()).normalize())>.9,
                        "real helicopter AI shoots toward the nearby Sauvojen and its bullet admits the boss");
            }finally{helicopters.stop();token.discard();}
        }

        void targeting(){
            require(!game.items().wands().target(owner,owner)&&!game.items().wands().target(owner,beta),"ordinary self/allied exclusions remain");
            try(var scope=game.items().spellSources().scope(authority,owner.level())){
                require(game.items().wands().target(null,owner)&&game.items().wands().target(null,beta),"explicit hostile policy admits wearer and allied player");
                for(var player:List.of(owner,beta)){
                    var at=player.position().add(0,.5,0);var hit=game.items().wands().firstHit((ServerPlayer)null,at.add(0,0,-1),at.add(0,0,1));
                    require(hit instanceof EntityHitResult entity&&entity.getEntity()==player,"native swept firstHit includes actual self/allied body");
                }
                try(var reaction=game.items().spellSources().scope((LbcSpellSource.Source)null)){require(!game.items().wands().target(owner,beta),"fresh reaction restores ordinary allegiance");}
                require(game.items().spellSources().current()==authority,"nested reaction restores exact outer source");
            }
            require(game.items().spellSources().current()==null,"source scope is fully restored");
            var origin=owner.position().add(4,2,0);var victim=cow(origin.add(.2,0,.2));var excluded=cow(origin.add(.3,0,.3));excluded.entityTags().add("summoned_cultist");
            game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);
            require(!game.items().wands().target(owner,victim),"actual team-alias nonplayer protected ordinarily");
            game.items().sauvojen().emitHostile(owner.level(),LbcSauvojen.Kind.TERRA,origin,new Vec3(0,0,1),source(LbcSauvojen.Kind.TERRA));game.items().terra().tick();
            require(victim.getHealth()==993&&victim.getLastDamageSource()!=null&&victim.getLastDamageSource().is(DamageTypes.CACTUS)&&victim.getLastDamageSource().getEntity()==boss&&victim.getKillCredit()==boss&&LbcIndirectKills.npcSource(victim.getLastDamageSource()),"actual allied target receives source7 native cactus with real NPC credit");
            require(excluded.getHealth()==1000,"source summoned-cultist exclusion survives actual Terra samples");base.clear();victim.discard();excluded.discard();
            var blindVictim=cow(origin.add(.2,0,.2));game.items().sauvojen().emitHostile(owner.level(),LbcSauvojen.Kind.BLIND,origin,new Vec3(0,0,1),source(LbcSauvojen.Kind.BLIND));game.items().blind().tick();
            var blindness=blindVictim.getEffect(MobEffects.BLINDNESS);var wither=blindVictim.getEffect(MobEffects.WITHER);
            require(blindVictim.getHealth()==1000&&blindness!=null&&blindness.getDuration()==80&&wither!=null&&wither.getDuration()==80&&wither.getAmplifier()==1,"hostile Blind source effects have no invented direct damage");base.clear();blindVictim.discard();
        }
        void reflection(){
            var origin=owner.position().add(4,4,-4);game.items().sauvojen().emitHostile(owner.level(),LbcSauvojen.Kind.RUBY,origin,new Vec3(1,0,0),authority);
            var shot=game.items().ruby().snapshot().getFirst();var before=shot.getDeltaMovement();boolean already=beta.entityTags().contains("sin_greed_active");
            beta.entityTags().add("sin_greed_active");try{require(LbcGreed.reflect(shot,beta),"actual shared native Greed reflection callback");}finally{if(!already)beta.entityTags().remove("sin_greed_active");}
            require(shot.getOwner()==beta&&game.items().spellSources().source(shot)==null,"real reflection transfers owner and drops hostile source");near(shot.getDeltaMovement(),before.reverse(),"reflection preserves speed/reverses motion");
            try(var scope=game.items().spellSources().scope(shot)){require(!game.items().wands().target(beta,owner),"reflected ordinary projectile protects allied causal wearer");}
            require(game.items().spellSources().current()==null,"reflection scope restores caller");base.clear();
            game.items().sauvojen().emitHostile(owner.level(),LbcSauvojen.Kind.RUBY,origin,new Vec3(1,0,0),authority);
            var sameAuthor=game.items().ruby().snapshot().getFirst();
            try(var scope=game.items().spellSources().scope(authority,owner.level())){game.items().electro().assignOwner(sameAuthor,owner);}
            require(sameAuthor.getOwner()==owner&&game.items().spellSources().source(sameAuthor)==null,
                    "same-author reflection epoch stamp cannot rebind the old hostile scope");base.clear();
        }
        void creatorCleanup(){
            game.items().sauvojenBoss().forget(owner);
            require(!boss.isRemoved()&&game.items().sauvojenBoss().count()==1,"creator cleanup does not delete independent native boss");
            var at=owner.position().add(4,4,-4);
            game.items().sauvojen().emitHostile(owner.level(),LbcSauvojen.Kind.RUBY,at,new Vec3(1,0,0),authority);
            var shot=game.items().ruby().snapshot().getFirst();
            require(shot.getOwner()==boss&&game.items().spellSources().owner(shot)==null,"post-cleanup emission still belongs to real NPC");
            game.items().ruby().tick();require(!shot.isRemoved(),"post-cleanup native emission advances");base.clear();
        }
        void death(){
            var level=owner.level();var at=boss.position();long before=game.items().sauvojenBoss().rewards();
            boss.setInvulnerableTime(0);boss.damageCooldownTime=0;
            require(boss.hurtServer(level,boss.damageSources().genericKill(),Float.MAX_VALUE)&&!boss.isAlive(),"actual lethal native hit without threshold polling");
            require(game.items().sauvojenBoss().rewards()==before+1&&boss.position().equals(at),"immediate one reward at actual death position, no teleport");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at,at).inflate(2),LbcBossLoot::protectedReward);
            require(!drops.isEmpty(),"actual immediately protected Infinity reward");
            for(var drop:drops){var nbt=saved(drop);require(nbt.getBooleanOr("Invulnerable",false)&&nbt.getBooleanOr("NoGravity",false)&&nbt.getBooleanOr("Glowing",false)&&nbt.getIntOr("Age",0)==-32768,"legacy reward lifetime/visibility/flight plus real invulnerability");
                require(!drop.hurtServer(level,new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.EXPLOSION)),10000)&&!drop.isRemoved(),"published reward survives actual native explosion damage");}
            game.items().sauvojenBoss().died(boss);require(game.items().sauvojenBoss().rewards()==before+1,"reentrant death cannot duplicate reward");
        }
        ItemStack keeperEgg(){
            var result=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:item_defs/minecraft/ghast_spawn_egg__7f286bc392ee");
            require(result.size()==1&&result.getFirst().is(Items.GHAST_SPAWN_EGG),"canonical Keeper source egg");
            return result.getFirst().copyWithCount(2);
        }
        void useKeeperEgg(BlockPos pad){
            var egg=keeperEgg();
            owner.setItemInHand(InteractionHand.MAIN_HAND,egg);
            var hit=new BlockHitResult(Vec3.atCenterOf(pad).add(0,.5,0),Direction.UP,pad,false);
            require(!owner.getAbilities().instabuild&&owner.gameMode.useItemOn(owner,owner.level(),egg,InteractionHand.MAIN_HAND,hit).consumesAction()
                    &&egg.getCount()==1,"a survival player uses the actual Keeper egg and spends exactly one");
        }
        void eggEncounter(){
            var level=owner.level();
            var service=game.items().sauvojenBoss();
            var pad=owner.blockPosition().offset(3,-1,0);
            var old=level.getBlockState(pad);
            try{
                service.stop();
                level.setBlock(pad,Blocks.STONE.defaultBlockState(),818);
                useKeeperEgg(pad);
                require(service.count()==1&&service.snapshot().getFirst() instanceof Ghast,"native egg ghast enters the hostile Keeper engine");
                var eggBoss=(Ghast)service.snapshot().getFirst();
                eggBoss.setNoAi(true);eggBoss.setNoGravity(true);
                require(level.getEntity(eggBoss.getUUID())==eggBoss&&eggBoss.getHealth()==124&&eggBoss.getMaxHealth()==124
                        &&eggBoss.getName().getString().equals("Sauvojen Tuntija")&&eggBoss.getName().getStyle().isBold()
                        &&eggBoss.getName().getStyle().getColor().getValue()==0x5555ff,
                        "actual ghast preserves the name and effective legacy health");
                var nbt=saved(eggBoss);
                require(nbt.getIntOr("ExplosionPower",0)==3&&nbt.getBooleanOr("PersistenceRequired",false)
                        &&nbt.getStringOr("DeathLootTable","").equals("minecraft:empty")
                        &&Math.abs(eggBoss.getX()-pad.getX()-.5)<1e-6&&Math.abs(eggBoss.getZ()-pad.getZ()-.5)<1e-6,
                        "egg keeps authored blast and native clicked placement with a single reward producer");
                var cause=game.items().spellSources().source(eggBoss);
                require(service.owner(eggBoss)==null&&cause!=null&&cause.emitter().equals(eggBoss.getUUID())
                        &&cause.author()==null&&cause.unownedMobs()&&service.canAffect(eggBoss,owner)&&service.canAffect(eggBoss,beta),
                        "egg boss has native physical authority and hostile rather than allied targeting");
                long seed=0;
                while(RandomSource.create(seed).nextInt(10)!=LbcSauvojen.Kind.RUBY.ordinal())seed++;
                eggBoss.getRandom().setSeed(seed);
                long choices=service.selections(),emits=service.emissions(),rewards=service.rewards();
                for(int tick=0;tick<60;tick++)service.tick();
                require(service.selections()==choices+1&&service.cloudCount()==1&&service.emissions()==emits+1,
                        "normal boss timing creates a Ruby wand and fires from the native egg actor");
                var arrow=game.items().ruby().snapshot().getFirst();
                require(arrow.getOwner()==eggBoss&&game.items().spellSources().owner(arrow)==null,"scheduled wand releases with the actual hostile ghast owner");
                require(eggBoss.hurtServer(level,eggBoss.damageSources().genericKill(),Float.MAX_VALUE)
                        &&service.rewards()==rewards+1&&service.cloudCount()==0,"egg boss death produces one Infinity and converts the real wand");
                var drops=level.getEntitiesOfClass(ItemEntity.class,eggBoss.getBoundingBox().inflate(5));
                require(drops.stream().anyMatch(item->item.getItem().is(LbcWands.RUBY_WAND)&&LbcBossLoot.protectedReward(item))
                        &&drops.stream().noneMatch(item->LbcInfinity.recognized(item.getItem())),"native death exposes the Ruby reward without a duplicate delayed token");
                base.clear();

                level.setBlock(pad,Blocks.SPAWNER.defaultBlockState(),818);
                useKeeperEgg(pad);
                var spawner=(SpawnerBlockEntity)level.getBlockEntity(pad);
                var source=spawner.saveWithFullMetadata(level.registryAccess()).getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity");
                require(source.getStringOr("id","").equals("minecraft:ghast")
                        &&source.getStringOr("DeathLootTable","").equals("lbc:sauvojen_tuntija"),
                        "real spawner egg use keeps the original authored spawn data");
                var body=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),source),
                        level,EntitySpawnReason.SPAWNER,entity->{entity.setPos(Vec3.atCenterOf(pad).add(0,3,0));return entity;});
                require(body instanceof Ghast&&level.tryAddFreshEntityWithPassengers(body)&&service.count()==1
                        &&LbcSauvojenBoss.managed(body)&&service.owner(body)==null,
                        "native spawner-decoded ghast reaches the same entity-load registration");
                var ordinary=Objects.requireNonNull(EntityTypes.GHAST.create(level,EntitySpawnReason.COMMAND));
                ordinary.setPos(owner.position().add(-4,0,0));
                require(level.addFreshEntity(ordinary)&&!LbcSauvojenBoss.managed(ordinary)&&service.count()==1,
                        "an ordinary native ghast never enters the Keeper engine");
                ordinary.discard();body.discard();
            }finally{service.stop();level.setBlock(pad,old,818);}
        }
        @Override public void close(){
            try{for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}game.items().sauvojenBoss().stop();base.clear();
                var fresh=new ArrayList<Entity>();for(var e:owner.level().getAllEntities())if(!before.contains(e)&&!(e instanceof ServerPlayer))fresh.add(e);fresh.forEach(Entity::discard);
            }finally{var board=game.server().getScoreboard();board.removePlayerTeam(team);if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldBetaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBetaTeam);otherSaved.restore(game);base.close();}
        }
    }
    private static Fixture running;private static int phase,started,lastFrame;
    public static void run(GameController game){
        require(running==null,"exclusive continuation");running=new Fixture(game);phase=0;started=game.server().getTickCount();lastFrame=started;
    }
    public static void tick(GameController game){
        if(running==null)return;int now=game.server().getTickCount();if(now==lastFrame)return;lastFrame=now;
        var fixture=running;var clock=(NativeFixtureAccess.Server)game.server();long average=clock.skywars2$validationAggregatedTickTime();
        try{
            require(now-started<20,"bounded real-frame native admission continuation");
            // Overload behavior is independently checked by drop-overload. Do not reset
            // the real frame, production activation count, or cumulative4ms budget here.
            clock.skywars2$validationAggregatedTickTime(0);
            switch(phase++){
                case 0->fixture.ownerlessActivation(false);
                case 1->fixture.ownerlessActivation(true);
                case 2->fixture.activation();
                case 3->fixture.incomingHits();
                case 4->{
                    fixture.payloads();fixture.targeting();fixture.reflection();fixture.creatorCleanup();fixture.death();fixture.eggEncounter();
                    fixture.close();running=null;
                    SkyWars2.LOGGER.info("[check] PASS sauvojen-boss (ownerless raw/restored Age1528 trap on distinct native frames; player-thrown entirely unowned WILD boss; native melee/arrow/helicopter; Keeper egg/spawner; nine payloads, allied targeting, reflection and sources)");
                    SkyWars2.LOGGER.info("[check] PENDING sauvojen-boss continuation: random branch sampling and lifecycle cadence use the separate sauvojen-lifecycle fixture");
                }
                default->throw new IllegalStateException("Unknown native admission phase");
            }
        }catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL sauvojen-boss",error);}
        finally{clock.skywars2$validationAggregatedTickTime(average);}
    }
}
