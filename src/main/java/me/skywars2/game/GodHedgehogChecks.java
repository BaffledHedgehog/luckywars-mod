package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Native boss and source-age boundary probes, plus201 genuinely elapsed server attack ticks. */
public final class GodHedgehogChecks {
    private GodHedgehogChecks(){}
    private static Fixture running;
    private static List<Entity> retained;
    private static List<TrialWarningVisual> retainedWarnings;
    private static String retainedEpoch;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("God Hedgehog: "+why);}
    private static final class BlastCow extends net.minecraft.world.entity.animal.cow.Cow {
        net.minecraft.world.damagesource.DamageSource received;
        BlastCow(ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){boolean accepted=super.hurtServer(level,source,amount);if(accepted)received=source;return accepted;}
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final Object profile;
        final Map<Field,Object> fields=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final List<Runnable> counters=new ArrayList<>();final boolean shift,sprint,ground;
        Saved(GameController game,ServerPlayer p){
            player=p;state=new GameChecks.WandThirdPlayerState(p,game.items().mana());profile=game.players().get(p.getUUID());
            shift=p.isShiftKeyDown();sprint=p.isSprinting();ground=p.onGround();
            try{for(var f:profile.getClass().getFields())if(!Modifier.isStatic(f.getModifiers())){var value=f.get(profile);fields.put(f,value instanceof Set<?> s?new HashSet<>(s):value instanceof int[] a?a.clone():value);}}
            catch(IllegalAccessException error){throw new IllegalStateException(error);}
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(score!=null)scores.put(objective,score.value());}
            for(var id:BuiltInRegistries.CUSTOM_STAT)stat(Stats.CUSTOM.get(id));
            for(var type:BuiltInRegistries.ENTITY_TYPE){stat(Stats.ENTITY_KILLED.get(type));stat(Stats.ENTITY_KILLED_BY.get(type));}
            for(var item:BuiltInRegistries.ITEM)for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_DROPPED,Stats.ITEM_PICKED_UP))stat(type.get(item));
        }
        <T> void stat(Stat<T> stat){int old=player.getStats().getValue(stat);counters.add(()->player.getStats().setValue(player,stat,old));}
        void restore(GameController game){
            try{for(var e:fields.entrySet())e.getKey().set(profile,e.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            state.restore(game);player.setShiftKeyDown(shift);player.setSprinting(sprint);player.setOnGround(ground);counters.forEach(Runnable::run);
            for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(player,objective).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(player,objective);
            game.items().indirectKills().clear(player);game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcGodHedgehog service;final ServerPlayer owner;final ServerLevel level;
        final List<Saved> saved=new ArrayList<>();final List<Entity> fixtures=new ArrayList<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final Map<BlockPos,BlockState> terrain=new LinkedHashMap<>();
        ServerLevel crossLevel,guestLevel;ServerPlayer guest;Vec3 crossPoint,guestPoint;float guestYaw,guestPitch;Set<Entity> crossBefore;
        final Vec3 at;Silverfish boss;Entity eater,white;List<LivingEntity> victims;int phase,lastTick=-1,startTick,whiteTicks;boolean retain;
        Fixture(GameController game){
            this.game=game;service=game.items().godHedgehog();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(owner!=null&&game.playing()&&game.canUseGameplayItems(owner)&&service.count()==0&&!game.items().timeStop().stopped(owner.level()),"exclusive unpaused real match and participant");
            level=owner.level();at=new Vec3(owner.getX(),Math.min(260,level.getMaxY()-45),owner.getZ());
            for(var p:game.server().getPlayerList().getPlayers())saved.add(new Saved(game,p));level.getAllEntities().forEach(before::add);
            require(before.stream().noneMatch(e->e instanceof Silverfish),"no unrelated live silverfish before deliberate global-removal fixture");
            require(before.stream().noneMatch(e->!e.isRemoved()&&!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<25*25),"isolated native combat volume");
            for(var p:BlockPos.betweenClosed(BlockPos.containing(at.add(-20,-4,-20)),BlockPos.containing(at.add(20,15,20))))require(level.hasChunkAt(p)&&level.getBlockState(p).isAir(),"resident empty fixture air volume");
        }
        void initialize(){
            owner.setGameMode(GameType.SURVIVAL);owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setShiftKeyDown(false);owner.getInventory().clearContent();
            for(var saved:saved)game.items().mana().score(saved.player,"void_resistance").set(1);
            var ordinary=EntityTypes.SILVERFISH.create(level,EntitySpawnReason.COMMAND);require(ordinary!=null,"native initial silverfish");ordinary.setPos(at.add(3,0,0));ordinary.setNoAi(true);ordinary.setNoGravity(true);fixtures.add(ordinary);require(level.addFreshEntity(ordinary),"ordinary pre-boss fish enters level");
            boss=service.summon(level,at.add(0,0,12),owner);require(boss!=null&&ordinary.isRemoved(),"summoning erases ALL existing silverfish, including untagged vanilla fish");fixtures.add(boss);
            require(boss.position().distanceToSqr(at.add(0,20,12))<1e-12,"boss appears exactly20blocks above requested point");
            require(service.summon(level,at.add(0,0,12),owner)==boss&&boss.position().distanceToSqr(at.add(0,20,12))<1e-12,
                    "duplicate summon reuses boss without another height offset");
            boss.setNoAi(true);boss.setNoGravity(true);boss.setDeltaMovement(Vec3.ZERO);
            require(service.blocksSilverfish(level)&&service.isBoss(boss)&&boss.isAlive()&&service.owner(boss)==owner&&service.cause(boss).friendlyFire(),"tracked live identity blocks new silverfish and retains WILD owner");
            for(String tag:List.of("",LbcGodHedgehog.TAG)){
                var fish=EntityTypes.SILVERFISH.create(level,EntitySpawnReason.COMMAND);require(fish!=null,"native rejected fish");fish.setPos(at);if(!tag.isEmpty())fish.entityTags().add(tag);fixtures.add(fish);level.addFreshEntity(fish);
                require(fish.isRemoved()&&!service.isBoss(fish),"all new native silverfish rejected, even forged boss tag: "+tag);
            }
            require(boss.getAttribute(Attributes.MAX_HEALTH).getBaseValue()==2147483647d&&boss.getHealth()==boss.getMaxHealth()&&boss.getHealth()>2_000_000_000f,"authored huge native health without fake invulnerability");
            require(boss.getAttribute(Attributes.SCALE).getBaseValue()==16&&boss.getAttribute(Attributes.ARMOR).getBaseValue()==20&&boss.getAttribute(Attributes.ARMOR_TOUGHNESS).getBaseValue()==20,"native scale16 armor/toughness20");
            boss.removeAllEffects();float hp=boss.getHealth();boss.hurtServer(level,level.damageSources().generic(),1024);require(boss.getHealth()<hp,"ordinary native damage reaches huge-health boss");boss.setHealth(boss.getMaxHealth());boss.entityTags().add("time_stopped");
            var trophy=game.lbc().rollTable(level,at,owner,"lbc:god");require(trophy.size()==1&&trophy.getFirst().is(LbcHedgehogItems.GOD),"source boss trophy acquisition alias");
            var stack=trophy.getFirst();require(Identifier.parse("lbc:god").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_x").equals(stack.get(DataComponents.TOOLTIP_STYLE))&&stack.get(DataComponents.LORE).lines().size()==10,"trophy model/tier/ten authored blank lore rows");
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();require(data.getIntOr("god",0)==1&&data.getIntOr("magicwand",0)==1,"inert trophy source flags");
            sweptRayRegression();
            // Inject source age78. The next two ACTUAL updates exercise grow and detonation boundaries.
            var center=BlockPos.containing(at.add(12,0,0));for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){var p=center.offset(x,y,z);terrain.put(p,level.getBlockState(p));level.setBlock(p,Blocks.STONE.defaultBlockState(),2);}
            eater=service.eaterBoundary(boss,Vec3.atLowerCornerOf(center),new Vec3(0,1,0),78);require(eater!=null,"actual native eater display at declared age78 boundary");phase=1;startTick=game.server().getTickCount();
        }
        LivingEntity cow(Vec3 at,String tag){var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);require(cow!=null,"native cow");cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);if(!tag.isEmpty())cow.entityTags().add(tag);fixtures.add(cow);require(level.addFreshEntity(cow),"native ray victim enters level");return cow;}
        void sweptRayRegression(){
            // Drive the real private actor updates once, so targets beyond this tick's segment stay observable.
            try{
                var actorType=Class.forName(LbcGodHedgehog.class.getName()+"$Actor");
                var actorsField=LbcGodHedgehog.class.getDeclaredField("actors");actorsField.setAccessible(true);
                var displayField=actorType.getDeclaredField("display");displayField.setAccessible(true);
                var kindField=actorType.getDeclaredField("kind");kindField.setAccessible(true);
                var whiteUpdate=LbcGodHedgehog.class.getDeclaredMethod("whiteTick",actorType);whiteUpdate.setAccessible(true);
                var rayUpdate=LbcGodHedgehog.class.getDeclaredMethod("rayTick",actorType);rayUpdate.setAccessible(true);
                var remove=LbcGodHedgehog.class.getDeclaredMethod("remove",actorType);remove.setAccessible(true);
                for(var direction:List.of(new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(1,0,1).normalize())){
                    var from=at.add(-8,5,-8);var to=from.add(direction.scale(2.5));var middle=from.add(direction.scale(1.25));
                    var positive=cow(middle.add(.5,.2,.5),"");var negative=cow(middle.add(-.5,-1.6,-.5),"");
                    require(positive.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(middle,middle.add(1,1,1)))
                            &&negative.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(middle.add(-.99,-.99,-.99),middle.add(.01,.01,.01))),
                            "two separate off-axis native hitboxes satisfy the former paired cubes");
                    var crossed=cow(middle.add(0,-.7,0),"");
                    var behind=cow(from.subtract(direction).add(0,-.7,0),"");var beyond=cow(to.add(direction).add(0,-.7,0),"");
                    var paused=cow(middle.add(0,-.7,0),"time_stopped");
                    var visual=service.whiteBoundary(boss,from,direction,39);require(visual!=null,"native one-tick white conversion regression");
                    var actors=(List<?>)actorsField.get(service);Object whiteActor=null;
                    for(var actor:actors)if(displayField.get(actor)==visual)whiteActor=actor;
                    require(whiteActor!=null,"actual white actor selected");whiteUpdate.invoke(service,whiteActor);
                    Object ray=null;for(var actor:actors)if(kindField.getInt(actor)==2)ray=actor;
                    require(ray!=null,"actual marker ray created");rayUpdate.invoke(service,ray);
                    require(positive.isAlive()&&negative.isAlive(),"off-axis targets cannot combine separate cubes into a remote kill: "+direction);
                    require(!crossed.isAlive()&&crossed.getKillCredit()==owner,"swept native hitbox retains inherited WILD kill credit: "+direction);
                    require(behind.isAlive()&&beyond.isAlive(),"targets before and beyond the finite2.5block segment survive: "+direction);
                    require(paused.isAlive(),"native Time Stop target remains protected: "+direction);
                    remove.invoke(service,ray);
                    for(var victim:List.of(positive,negative,crossed,behind,beyond,paused))victim.discard();
                }
            }catch(ReflectiveOperationException error){throw new IllegalStateException("God Hedgehog native swept-ray regression",error);}
        }
        void killRay(){
            var p=at.add(-12,0,0);victims=new ArrayList<>();victims.add(cow(p,""));victims.add(cow(p,"sin_greed_active"));victims.add(cow(p,""));victims.add(cow(p,"spectator"));victims.add(cow(p,"strict_map_object"));
            game.items().mana().score(victims.get(2),"void_resistance").set(1);
            white=service.whiteBoundary(boss,p,new Vec3(0,0,1),39);require(white!=null,"native white display at explicit source-age39 boundary");
        }
        void silence(){
            for(var player:game.server().getPlayerList().getPlayers())if(player!=owner)player.entityTags().add("spectator");
            owner.removeAllEffects();owner.setAbsorptionAmount(0);owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20_000_000_000d);owner.setHealth(20_000_000_000f);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.getAbilities().invulnerable=false;
            game.items().mana().score(owner,"void_resistance").set(0);game.items().mana().set(owner,2000);
            var stack=new ItemStack(LbcWands.ACADEMY_CONSUMING);var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("mode",2);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(game.items().consuming().use(owner,InteractionHand.MAIN_HAND,stack,true),"actual native Silence activation with real boss present");
            float loss=switch(level.getDifficulty()){case PEACEFUL->0;case EASY->5_000_000_000f;case NORMAL->10_000_000_000f;case HARD->15_000_000_000f;};
            require(owner.isAlive()&&owner.getHealth()==20_000_000_000f-loss,"native boss10-billion raw retaliation retains difficulty scaling");
            require(boss.isAlive()&&!game.items().consuming().eraseLoaded(boss)&&!boss.isRemoved(),"real tracked boss survives active Silence event erasure");
            if(loss>0)require(owner.getLastDamageSource()!=null&&owner.getLastDamageSource().getEntity()==boss,"retaliation identifies actual boss");
            game.items().consuming().forget(owner);owner.removeAllEffects();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        }
        void beginCrossWorld(){
            service.stop();var remote=game.server().getLevel(LbcHedgehogDimensions.IMPRINTED);
            require(remote!=null&&remote!=level,"actual Imprinted world is outside arena");
            var existing=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());remote.getAllEntities().forEach(existing::add);
            require(existing.stream().noneMatch(e->e instanceof Silverfish&&!e.isRemoved()),"crossworld probe does not erase unrelated existing silverfish");
            int cx=BlockPos.containing(at).getX()>>4,cz=BlockPos.containing(at).getZ()>>4;
            var point=new Vec3(cx*16+8.5,Math.min(270,remote.getMaxY()-24),cz*16+8.5);
            // The real participant's ordinary teleport acquires this destination; production actor code never does.
            crossLevel=remote;crossPoint=point;crossBefore=existing;
            guest=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(guest!=null&&guest!=owner,"actual remote chunk-visibility participant");
            guestLevel=guest.level();guestPoint=guest.position();guestYaw=guest.getYRot();guestPitch=guest.getXRot();crossBefore.add(guest);
            guest.hasChangedDimension();guest.teleportTo(remote,point.x,point.y+8,point.z,Set.of(),0,0,true);guest.hasChangedDimension();guest.setNoGravity(true);guest.setDeltaMovement(Vec3.ZERO);
            require(guest.level()==remote,"native participant teleport acquires its actual Imprinted destination chunk");phase=5;startTick=game.server().getTickCount();
        }
        void restoreGuest(){
            if(guest==null||guestLevel==null)return;
            guest.hasChangedDimension();guest.teleportTo(guestLevel,guestPoint.x,guestPoint.y,guestPoint.z,Set.of(),guestYaw,guestPitch,true);guest.hasChangedDimension();guest=null;guestLevel=null;
        }
        void crossWorld(){
            var remote=crossLevel;var point=crossPoint;var existing=crossBefore;
            remote.getAllEntities().forEach(existing::add);
            require(existing.stream().noneMatch(e->e instanceof Silverfish&&!e.isRemoved()),"entity-visible Imprinted world has no unrelated fish before deliberate boss sweep");
            require(existing.stream().noneMatch(e->!e.isRemoved()&&e!=guest&&e.position().distanceToSqr(point)<40*40),"actual visible Imprinted explosion volume remains isolated");
            for(var cell:BlockPos.betweenClosed(BlockPos.containing(point.add(-24,-24,-24)),BlockPos.containing(point.add(24,24,24))))
                if(remote.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)!=null)require(remote.getBlockState(cell).isAir(),"native participant's resident Imprinted explosion region is empty air");
            var probe=BlockPos.containing(point);var old=remote.getBlockState(probe);BlastCow cow=null;
            try{
                remote.setBlock(probe,Blocks.STONE.defaultBlockState(),2);
                var remoteBoss=service.summon(remote,point.add(6,0,0),owner);
                require(remoteBoss!=null&&remoteBoss.level()==remote&&service.blocksSilverfish(remote),"actual native God encounter remains live outside arena");
                require(remoteBoss.position().distanceToSqr(point.add(6,20,0))<1e-12,"outside-arena boss uses the same20block spawn offset");
                remoteBoss.setNoAi(true);remoteBoss.setNoGravity(true);remoteBoss.setDeltaMovement(Vec3.ZERO);
                require(service.whiteBoundary(remoteBoss,point.add(0,8,0),new Vec3(0,0,1),0)!=null,"real outside-arena warning owner");
                var remoteWarning=service.warningSnapshot().getFirst();
                require(remoteWarning.display().getHolder().getAttachment().getWorld()==remote,"warning tracking uses actual encounter dimension");
                var rejected=EntityTypes.SILVERFISH.create(remote,EntitySpawnReason.COMMAND);require(rejected!=null,"crossworld native silverfish fixture");
                rejected.setPos(point.add(0,2,0));remote.addFreshEntity(rejected);require(rejected.isRemoved(),"native Imprinted silverfish load blocked by its local live boss");
                cow=new BlastCow(remote);cow.setPos(point.add(2,0,0));cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);cow.setHealth(10000);
                require(remote.addFreshEntity(cow),"actual Imprinted native blast recipient");
                service.phaseForCheck(441);service.laserBoundary(remoteBoss,Vec3.atCenterOf(probe),new Vec3(0,1,0));
                require(service.pendingBlasts()==101,"crossworld native laser retains full authored101-job series");
                service.tick(); // One bounded native worker slice, not101 unbounded fixture explosions.
                require(!remoteWarning.closed()&&remoteWarning.age()==1,"outside-arena warning remains alive and advances once with its native actor");
                require(remote.getBlockState(probe).isAir()&&service.pendingBlasts()<101,"actual native God laser destroys stone outside arena");
                var damage=cow.received;require(damage!=null&&damage.getDirectEntity() instanceof net.minecraft.world.entity.item.PrimedTnt,"outside-arena explosion reaches actual native entity damage");
                var source=(net.minecraft.world.entity.item.PrimedTnt)damage.getDirectEntity();
                require(source.getFuse()==0&&source.entityTags().contains(LbcDamage.UNPROTECTED_TNT)&&source.entityTags().contains(LbcGodHedgehog.BLAST),"native laser uses source fuse0 and explicit UNPROTECTED permission");
                require(damage.getEntity()==owner&&game.items().indirectKills().babylonCause(damage)==service.cause(remoteBoss)&&LbcDamage.allowsTrapFriendlyFire(damage),"crossworld native blast retains exact captured WILD author/cause");
            }finally{
                service.stop();if(cow!=null)cow.discard();remote.setBlock(probe,old,2);
                var fresh=new ArrayList<Entity>();remote.getAllEntities().forEach(e->{if(!existing.contains(e)&&!(e instanceof ServerPlayer))fresh.add(e);});fresh.forEach(Entity::discard);restoreGuest();
            }
        }
        boolean step(){
            require(game.playing(),"real match remains active");int tick=game.server().getTickCount();if(tick==lastTick)return false;lastTick=tick;
            switch(phase){
                case 1->{if(tick-startTick<2)return false;var center=BlockPos.containing(at.add(12,0,0));int removed=0,kept=0;for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){boolean air=level.getBlockState(center.offset(x,y,z)).isAir();if(x*x+y*y+z*z<=12){require(air,"eater authored integer sphere cell");removed++;}else{require(!air,"eater preserves outer cube corners");kept++;}}require(removed==179&&kept==164,"actual source179-air-cell mask, not radius3/cube");require(!eater.isRemoved(),"eater remains through source-age80 detonation");killRay();phase=2;startTick=tick;}
                case 2->{if(tick-startTick<3)return false;require(white.isRemoved()&&!victims.getFirst().isAlive(),"source-age40 native white conversion and next native marker update kill ordinary victim");for(int i=1;i<victims.size();i++)require(victims.get(i).isAlive(),"native kill ray protects Greed/void resistance/observer/strict role "+i);require(victims.getFirst().getKillCredit()==owner,"lethal ray inherits original WILD summoner credit");require(eater.isRemoved(),"source-age83 retires native eater");for(var item:level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(at,at).inflate(30)))if(!before.contains(item))item.discard();service.stop();boss=service.summon(level,at.add(0,0,12),owner);require(boss!=null,"second real encounter");fixtures.add(boss);boss.setNoAi(true);boss.setNoGravity(true);boss.entityTags().add("time_stopped");service.laserBoundary(boss,at.add(0,60,0),new Vec3(0,1,0));require(service.pendingBlasts()==101,"one source laser enqueues exactly101 native explosions without command/TNT marker entities");require(service.cause(boss).friendlyFire(),"queued laser retains WILD cause");boss.entityTags().remove("time_stopped");service.phaseForCheck(441);phase=3;startTick=tick;}
                case 3->{require(service.pendingBlasts()<101,"bounded native explosion worker makes resident progress");boss.entityTags().add("time_stopped");require(level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(at,at).inflate(30)).isEmpty(),"air-only laser boundary creates no item drops");service.stop();boss=service.summon(level,at.add(0,0,12),owner);fixtures.add(boss);boss.setNoAi(true);boss.setNoGravity(true);boss.setDeltaMovement(Vec3.ZERO);service.phaseForCheck(240);whiteTicks=0;phase=4;startTick=tick;}
                case 4->{whiteTicks++;for(var actor:service.snapshot())if(actor!=boss)actor.entityTags().add("time_stopped");if(whiteTicks<201)return false;require(tick-startTick>=201,"201 source attack updates elapsed across201 actual server ticks");boss.entityTags().add("time_stopped");var warnings=service.warningSnapshot();require(warnings.size()==201,"all201native warning owners have one display line each");
                    for(var w:warnings){require(!w.closed()&&w.age()==1&&w.lifetime()==40,"actor pause freezes its forty-tick warning");require(w.model().toString().equals("lbc:meditation_warning_beam_red")&&Math.abs(w.end().distanceTo(w.origin())-10)<1e-7,"shared red line preserves original10block warning span");}
                    long count=service.snapshot().stream().filter(e->e.entityTags().contains("floatingwhitearrow")).count();require(count==201,"real five white-arrow windows produce41+40+40+40+40 native displays");for(var actor:service.snapshot())if(actor!=boss)actor.discard();silence();boss.entityTags().remove("time_stopped");boss.removeAllEffects();boss.setInvulnerableTime(0);boss.damageCooldownTime=0;boss.hurtServer(level,level.damageSources().genericKill(),Float.MAX_VALUE);require(!boss.isAlive()&&!service.blocksSilverfish(level),"boss genuinely dies and silverfish restriction ends immediately");var drops=level.getEntitiesOfClass(ItemEntity.class,boss.getBoundingBox().inflate(4),e->e.getItem().is(LbcHedgehogItems.GOD));require(drops.size()==1&&drops.getFirst().getItem().getCount()==1,"native boss death produces exactly one functional source trophy");fixtures.addAll(drops);beginCrossWorld();return false;}
                case 5->{if(!crossLevel.isPositionEntityTicking(BlockPos.containing(crossPoint))){require(tick-startTick<80,"native participant teleport makes its Imprinted destination entity-visible within80 actual ticks");return false;}require(tick>startTick,"crossworld explosion waits for a later actual server tick");crossWorld();return true;}
                default->throw new IllegalStateException("unknown God fixture phase");
            }return false;
        }
        @Override public void close(){
            if(!retain)service.stop();restoreGuest();game.items().consuming().forget(owner);for(var e:fixtures)if(!retain||e!=boss)e.discard();
            for(var e:terrain.entrySet())if(level.hasChunkAt(e.getKey()))level.setBlock(e.getKey(),e.getValue(),2);
            for(var e:level.getAllEntities())if(!before.contains(e)&&e instanceof ItemEntity item&&(item.getItem().is(LbcHedgehogItems.GOD)||item.position().distanceToSqr(at)<30*30))item.discard();
            saved.forEach(s->s.restore(game));game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}finally{running=null;}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS god-hedgehog");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL god-hedgehog",error);}}
    public static void stopProbe(GameController game){
        require(retained==null&&running==null,"exclusive actual stop probe");try(var f=new Fixture(game)){
            f.boss=f.service.summon(f.level,f.at.add(0,0,12),f.owner);require(f.boss!=null,"retained real boss");f.boss.setNoAi(true);f.boss.setNoGravity(true);f.boss.entityTags().add("time_stopped");
            f.service.eaterBoundary(f.boss,f.at.add(10,0,0),new Vec3(0,1,0),0);f.service.whiteBoundary(f.boss,f.at.add(-10,0,0),new Vec3(0,1,0),0);f.service.laserBoundary(f.boss,f.at.add(0,60,0),new Vec3(0,1,0));
            retained=f.service.snapshot();retainedWarnings=f.service.warningSnapshot();require(retainedWarnings.size()==1&&!retainedWarnings.getFirst().closed(),"retained real boss warning display");retainedEpoch=game.items().epoch();retained.forEach(e->e.entityTags().add("time_stopped"));require(retained.size()==3&&f.service.pendingBlasts()==101,"real retained boss/eater/white and full pending native laser");f.retain=true;
        }SkyWars2.LOGGER.info("[check] PASS god-hedgehog-stop-probe");
    }
    public static void cleanup(GameController game){require(retained!=null,"retained actual stop probe");try{require(!game.items().currentEpoch(retainedEpoch)&&game.items().godHedgehog().count()==0&&retained.stream().allMatch(Entity::isRemoved)&&retainedWarnings.stream().allMatch(TrialWarningVisual::closed),"actual match stop retires boss, warning displays, native displays, pending101 explosions and epoch");}finally{retained.forEach(Entity::discard);retained=null;retainedWarnings=null;retainedEpoch=null;}SkyWars2.LOGGER.info("[check] PASS god-hedgehog-cleanup");}
}
