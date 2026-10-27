package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native producer/impact tests against real indexed beacon blocks, with isolated terrain. */
public final class JammerSpellChecks {
    private JammerSpellChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Jammer spells: "+why);}
    private static java.lang.reflect.Field field(Object o,String name){for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object read(Object o,String n){try{return field(o,n).get(o);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object o,String n,Object v){try{field(o,n).set(o,v);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}

    /** Broader than the standard wand snapshot because actual player ticks mutate scores and stats. */
    private static final class State {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState basic;final net.minecraft.server.level.ServerLevel level;
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>(),tickers=new LinkedHashMap<>();
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final net.minecraft.nbt.CompoundTag food;final boolean ground,sprint,shift,swimming;final Vec3 knownMovement;final net.minecraft.world.entity.Pose pose;
        State(GameController game,ServerPlayer p){player=p;level=p.level();basic=new GameChecks.WandThirdPlayerState(p,game.items().mana());ground=p.onGround();sprint=p.isSprinting();shift=p.isShiftKeyDown();swimming=p.isSwimming();knownMovement=p.getKnownMovement();pose=p.getPose();
            for(var a:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var value=p.getAttribute(a);if(value!=null)attributes.getInstance(a).replaceFrom(value);}
            for(var objective:game.server().getScoreboard().getObjectives()){var value=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(value!=null)scores.put(objective,value.value());}
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,p.getStats().getValue(stat));}
            for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET)){var stat=type.get(item);stats.put(stat,p.getStats().getValue(stat));}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            try{var pr=game.players().get(p.getUUID());for(var f:pr.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(pr);profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> s?new HashSet<>(s):v);}for(var n:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO")){var f=field(p,n);tickers.put(f,f.get(p));}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess());p.getFoodData().addAdditionalSaveData(out);food=out.buildResult();
        }
        void restore(GameController game){if(player.level()!=level)player.teleportTo(level,basic.position.x,basic.position.y,basic.position.z,Set.of(),basic.yaw,basic.pitch,true);basic.restore(game);for(var a:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var target=player.getAttribute(a);var saved=attributes.getInstance(a);if(target!=null&&saved!=null)target.replaceFrom(saved);}player.setHealth(basic.health);player.setAbsorptionAmount(basic.absorption);
            for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(player,objective).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(player,objective);
            stats.forEach((s,v)->player.getStats().setValue(player,s,v));advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            try{for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());for(var e:tickers.entrySet())e.getKey().set(player,e.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
            player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setSwimming(swimming);player.setKnownMovement(knownMovement);player.setPose(pose);player.getFoodData().readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess(),food));
        }
    }

    private static final class Straight implements AutoCloseable {
        final ServerPlayer player;final Object original;
        Straight(ServerPlayer p){player=p;original=read(p,"random");set(p,"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,m,args)->{if(m.getName().equals("nextFloat"))return .5f;try{return m.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}}));}
        @Override public void close(){set(player,"random",original);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,beta;final State saved,betaSaved;
        final SauvojenMeditationChecks.Attribution attribution;final Vec3 at,eye;final BlockPos beacon;
        final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();final Set<Long> forced=new HashSet<>(),wanted=new LinkedHashSet<>();
        final List<Cow> actors=new ArrayList<>();int warm;boolean closed;
        Fixture(GameController game){this.game=game;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));saved=new State(game,owner);beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));betaSaved=new State(game,beta);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta));base=new ElementalGrimoireChecks.Fixture(game);base.initialize();at=owner.position();owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),-90,0,true);eye=owner.getEyePosition();beacon=BlockPos.containing(eye.add(25,0,0));owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);owner.setHealth(10000);owner.removeAllEffects();game.items().mana().maximum(owner,1_000_000);game.items().mana().baseRegen(owner,0);game.items().mana().set(owner,1_000_000);
            require(game.items().acidCorruption().snapshot().isEmpty()&&game.items().erecto().sphereCount()==0,"exclusive acid/higher-electric payload services");
            for(int x=((int)Math.floor(at.x)-2)>>4;x<=((int)Math.floor(at.x)+58)>>4;x++)for(int z=((int)Math.floor(at.z)-8)>>4;z<=((int)Math.floor(at.z)+8)>>4;z++){long k=net.minecraft.world.level.ChunkPos.pack(x,z);wanted.add(k);if(!owner.level().getForceLoadedChunks().contains(k)){forced.add(k);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
        }
        boolean ready(){require(++warm<=200,"bounded native entity-chunk readiness");return wanted.stream().allMatch(k->owner.level().areEntitiesLoaded(k));}
        void block(BlockPos p,BlockState state){blocks.computeIfAbsent(p,point->{var old=owner.level().getBlockState(point);require(old.isAir(),"resident air fixture cell "+point);return old;});owner.level().setBlock(p,state,3);}
        void zone(boolean tech){removeZone();block(beacon,(tech?LbcMagic.MECHANISM_BEACON_BLOCK:LbcMagic.MAGIC_BEACON_BLOCK).defaultBlockState());game.items().magic().blockChanged(beacon,owner.level().getBlockState(beacon));require(tech?game.items().magic().mechanismSuppressedAt(owner.level(),eye.add(8,0,0)):game.items().magic().suppressedAt(owner.level(),eye.add(8,0,0)),"actual indexed "+(tech?"TECH":"MAG")+" field contains target");require(!game.items().magic().suppressed(owner)&&!game.items().magic().mechanismSuppressed(owner),"caster remains outside both field spheres");}
        void removeZone(){if(owner.level().getBlockState(beacon).is(LbcMagic.MAGIC_BEACON_BLOCK)||owner.level().getBlockState(beacon).is(LbcMagic.MECHANISM_BEACON_BLOCK)){owner.level().setBlock(beacon,Blocks.AIR.defaultBlockState(),3);game.items().magic().blockChanged(beacon,owner.level().getBlockState(beacon));}}
        Cow cow(double x){var c=new Cow(EntityTypes.COW,owner.level());c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);c.setPos(eye.add(x,-.5,0));require(owner.level().addFreshEntity(c),"real native target publication");actors.add(c);return c;}
        void resetTargets(){for(var c:actors){game.items().indirectKills().clear(c);for(var o:game.server().getScoreboard().getObjectives())game.server().getScoreboard().resetSinglePlayerScore(c,o);c.discard();}actors.clear();}
        void clear(){base.clear();var debris=new ArrayList<Entity>();for(var e:owner.level().getAllEntities())if(!base.before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity))debris.add(e);debris.forEach(Entity::discard);game.items().erecto().forget(owner);game.items().acidCorruption().forget(owner);resetTargets();owner.removeAllEffects();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        ItemStack loot(String id){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+id);require(result.size()==1,"actual source singleton "+id);return result.getFirst();}
        void tinyAndInstant(){
            for(int kind:new int[]{1,2,3,4,5,6}){
                clear();zone(false);var wall=BlockPos.containing(eye.add(8,0,0));block(wall,Blocks.STONE.defaultBlockState());var target=cow(8.5);
                BlockPos front=BlockPos.containing(eye.add(3,0,0));if(kind==5)block(front,Blocks.STONE.defaultBlockState());
                game.items().wands().extraTiny(owner,kind);
                require(owner.level().getBlockState(wall).is(Blocks.STONE)&&target.getHealth()==1000&&target.getActiveEffects().isEmpty(),"tiny shared ray has no terminal or target effect beyond MAG boundary kind="+kind);
                require(game.items().clouds().acidCount()==0,"tiny acid cannot create terminal actors across MAG boundary");
                if(kind==5)require(owner.level().getBlockState(front).isAir(),"tiny Ruby still cuts terrain before the first boundary");
                owner.level().setBlock(wall,Blocks.AIR.defaultBlockState(),3);
                if(kind==5)owner.level().setBlock(front,Blocks.AIR.defaultBlockState(),3);
            }
            clear();zone(false);var before=cow(3);var after=cow(8);game.items().wands().extraTiny(owner,6);require(before.getHealth()==995&&after.getHealth()==1000,"tiny Terra native penetrating hit before boundary, protected hit behind boundary");
            for(int mode:new int[]{0,1,2,3}){clear();zone(false);var wall=BlockPos.containing(eye.add(8,0,0));block(wall,Blocks.STONE.defaultBlockState());
                if(mode==0)game.items().ruby().cast(owner,2);else if(mode==1)game.items().stone().ray(owner,false);else if(mode==2)game.items().stone().ray(owner,true);else game.items().wands().concentratedFire(owner);
                require(owner.level().getBlockState(wall).is(Blocks.STONE),"instant hole/stone/spikes/fire cannot terminal inside MAG mode="+mode);
                for(var p:BlockPos.betweenClosed(wall.offset(-2,-2,-2),wall.offset(2,2,2)))require(p.equals(wall)?owner.level().getBlockState(p).is(Blocks.STONE):owner.level().getBlockState(p).isAir(),"suppression contact produces no terrain terminal mode="+mode+" at="+p);
                owner.level().setBlock(wall,Blocks.AIR.defaultBlockState(),3);
            }
            clear();zone(false);var front=BlockPos.containing(eye.add(3,0,0));var back=BlockPos.containing(eye.add(8,0,0));block(front,Blocks.BEDROCK.defaultBlockState());block(back,Blocks.BEDROCK.defaultBlockState());game.items().ruby().cast(owner,3);require(owner.level().getBlockState(front).isAir()&&owner.level().getBlockState(back).is(Blocks.BEDROCK),"Academy Ruby cuts indestructible front cell but clips at MAG");owner.level().setBlock(back,Blocks.AIR.defaultBlockState(),3);clear();removeZone();
        }
        void bloodAndConfuse(){
            for(boolean tech:List.of(false,true)){clear();zone(tech);var target=cow(8);game.items().bloodConfuse().blood(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,true);require((target.getHealth()==982)==tech&&target.hasEffect(MobEffects.WITHER)==tech,"Blood target inside MAG protected, TECH permits native damage/Wither");target.removeAllEffects();game.items().bloodConfuse().confuse(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,false,4);require(target.hasEffect(MobEffects.BLINDNESS)==tech,"Confuse shares target-position MAG protection without TECH immunity");}
            clear();removeZone();var target=cow(8);game.items().bloodConfuse().blood(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,true);require(target.getHealth()==982&&target.getLastHurtByMob()==owner,"outside fields actual Blood damage retains owner");clear();
        }
        void electrical(){
            clear();zone(true);try{beta.getInventory().clearContent();game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();beta.teleportTo(owner.level(),eye.x+6.3,at.y,eye.z,Set.of(),90,0,true);beta.entityTags().add("sin_greed_active");var origin=eye.add(4.7,0,0);game.items().wands().shortElectro(owner,origin,origin.add(1,0,0),false);game.items().wands().tick();require(game.items().wands().electroCount()==0,"earlier TECH boundary destroys arc before later native Greed reflection");}finally{betaSaved.restore(game);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();}
            for(boolean tech:List.of(false,true)){clear();zone(tech);var target=cow(8);var origin=eye.add(4.7,0,0);game.items().wands().shortElectro(owner,origin,origin.add(1,0,0),false);game.items().wands().tick();require((game.items().wands().electroCount()==0)==tech,"all released short arcs ignore MAG but retire at TECH even former magic-producer flag");if(!tech)require(game.items().wands().electroSnapshot().stream().allMatch(t->t.position.x>eye.x+6),"native electrical segment crosses MAG boundary");require(target.getHealth()==1000,"target beyond2block first segment not accidentally hit");
                clear();zone(tech);target=cow(8);game.items().wands().electroRay(owner);require((target.getHealth()<1000)==!tech,"Academy instantaneous electric ray is MAG-immune, TECH-clipped");
                clear();zone(tech);target=cow(8);boolean done=false;try(var straight=new Straight(owner)){var instant=new LbcTesla.Instant(game,owner);for(int n=0;n<500&&!done;n++)done=instant.step();}require(done,"bounded shared Tesla native instant work");require((target.getHealth()<1000)==!tech,"Tesla queued instant ray crosses MAG and stops before TECH target");
                clear();zone(tech);target=cow(8);game.items().erecto().emitAt(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,2,eye,new Vec3(1,0,0));require((target.getHealth()<1000)==!tech,"higher repeating electrical ray uses same TECH/MAG distinction");
                for(int mode:new int[]{1,4}){clear();zone(tech);target=cow(8);game.items().erecto().emitAt(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,mode,eye.add(4.7,0,0),new Vec3(1,0,0));game.items().wands().tick();require((game.items().wands().electroCount()==0)==tech,"higher electrical moving/sniper release retires under TECH mode="+mode);}
                clear();zone(tech);cow(8);game.items().erecto().emitAt(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,3,eye,new Vec3(1,0,0));require(game.items().erecto().sphereCount()==1,"native higher-electric sphere is a retained producer");for(int n=0;n<5;n++)game.items().erecto().tick();game.items().wands().tick();require((game.items().wands().electroCount()==0)==tech,"released sphere phantom arcs share TECH retirement and MAG immunity");
            }clear();removeZone();
        }
        void pulseControl(boolean tech){
            if(tech){var control=new Arrow(EntityTypes.ARROW,owner.level());control.entityTags().add("mecha");control.setPos(eye.add(9,0,3));control.setNoGravity(true);require(owner.level().addFreshEntity(control),"native TECH purge positive control");game.items().magic().pulse();require(control.isRemoved(),"actual central TECH pulse executed");}
            else{var control=new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(EntityTypes.SMALL_FIREBALL,owner.level());control.setPos(eye.add(9,0,3));require(owner.level().addFreshEntity(control),"native MAGIC type-tag positive control");game.items().magic().pulse();require(control.isRemoved(),"actual central MAGIC type-tag purge executed");}
        }
        void acids(){
            for(boolean tech:List.of(false,true))for(int variant=0;variant<8;variant++){
                clear();zone(tech);var target=cow(8.5);LbcClouds.AcidProjectile shot;
                if(variant==0)shot=game.items().clouds().academyWandAcid(owner,false);
                else if(variant==1)shot=game.items().clouds().ordinaryWandAcid(owner);
                else if(variant==2)shot=game.items().clouds().sourceAcid(owner.level(),eye,0,0,500,false,owner);
                else if(variant==7){game.items().wands().extraTiny(owner,7);require(game.items().clouds().acidCount()==6,"tiny acid guide crosses field and releases its physical terminal");shot=game.items().clouds().acidSnapshot().getFirst();}
                else if(variant==6){game.items().clouds().volleyAcid(owner,eye,new Vec3(1,0,0),1,false,false);shot=game.items().clouds().acidSnapshot().getFirst();}
                else shot=game.items().clouds().awakenedAcid(owner,eye,new Vec3(1,0,0),variant-3,false,false);
                shot.setPos(eye);shot.setNoGravity(true);shot.setDeltaMovement(8,0,0);shot.tick();
                require(!shot.isRemoved()&&shot.position().x>eye.x+7,"native acid projectile physically crosses field first entry variant="+variant+" TECH="+tech);
                pulseControl(tech);require(!shot.isRemoved(),"all released acid variants survive actual central pulse variant="+variant+" TECH="+tech);
                game.items().clouds().tick();require(target.hasEffect(MobEffects.POISON),"shared native acid target impact remains effective inside field variant="+variant+" TECH="+tech);
                if(!tech){shot.entityTags().add("magic");shot.entityTags().add("mecha");game.items().clouds().unloaded(shot);game.items().clouds().loaded(shot);game.items().magic().pulse();require(!shot.isRemoved(),"legacy saved acid tags are removed by actual service re-admission");}
            }
            for(boolean tech:List.of(false,true)){clear();zone(tech);var target=cow(8);var drop=game.items().acidCorruption().spawn(owner,eye.add(3,0,0),false);drop.setDeltaMovement(8,0,0);drop.tick();pulseControl(tech);require(!drop.isRemoved()&&drop.position().x>eye.x+10&&target.getHealth()<1000,"native hard-acid swept damage crosses and persists inside field TECH="+tech);}
            for(boolean tech:List.of(false,true)){clear();zone(tech);cow(8);game.items().acidCorruption().emitAt(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,1,eye,new Vec3(1,0,0));var active=game.items().acidCorruption().snapshot().stream().filter(c->!c.isRemoved()).toList();require(active.size()==1,"physical hard-acid seed guide crosses field after cast admission TECH="+tech+" live="+active.size()+" registered="+game.items().acidCorruption().snapshot().size());var seed=active.getFirst();pulseControl(tech);require(!seed.isRemoved(),"hard-acid ball seed survives central field pulse");}
            clear();zone(true);var overlap=cow(8);float hp=overlap.getHealth();game.items().erecto().emitAt(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,2,eye.add(8,0,0),new Vec3(1,0,0));require(overlap.getHealth()==hp&&overlap.getActiveEffects().isEmpty(),"TECH t=0 electrical ray cannot apply a zero-length native hit/status");
            clear();removeZone();var stack=loot("acid_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,stack);game.items().mana().set(owner,10000);block(BlockPos.containing(at),LbcMagic.MAGIC_BEACON_BLOCK.defaultBlockState());game.items().magic().blockChanged(BlockPos.containing(at),owner.level().getBlockState(BlockPos.containing(at)));int mana=game.items().mana().get(owner);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().clouds().acidCount()==0&&game.items().mana().get(owner)==mana,"actual paid acid cast remains MAG-gated before spending or release");owner.level().setBlock(BlockPos.containing(at),Blocks.AIR.defaultBlockState(),3);game.items().magic().blockChanged(BlockPos.containing(at),owner.level().getBlockState(BlockPos.containing(at)));clear();
        }
        void exercise(){tinyAndInstant();bloodAndConfuse();electrical();acids();}
        @Override public void close(){if(closed)return;closed=true;try{removeZone();clear();var debris=new ArrayList<Entity>();for(var e:owner.level().getAllEntities())if(!base.before.contains(e)&&e!=owner)debris.add(e);debris.forEach(Entity::discard);for(var e:blocks.entrySet()){owner.level().setBlock(e.getKey(),e.getValue(),3);game.items().magic().blockChanged(e.getKey(),e.getValue());}for(long k:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(k),net.minecraft.world.level.ChunkPos.getZ(k),false);base.retain=true;base.close();saved.restore(game);betaSaved.restore(game);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();attribution.close();}finally{owner.connection.resetPosition();beta.connection.resetPosition();}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);}
    public static void tick(GameController game){if(running==null)return;try{if(!running.ready())return;running.exercise();running.close();running=null;SkyWars2.LOGGER.info("[check] PASS jammer-spells (native MAG first-boundary spell terminals; Blood/Confuse protected targets; released electrical TECH interception/MAG immunity; all shared/hard-acid native crossing/impact and actual central MAG/TECH purge; paid acid cast still gated; scoped restore)");}catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL jammer-spells",failure);}}
}
