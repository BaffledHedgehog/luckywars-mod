package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real native actors and existing engines; no fake author, private clocks or copied spell engine. */
public final class ApocalypseActorChecks {
    private ApocalypseActorChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Apocalypse actors: "+why);}
    private static void near(Vec3 a,Vec3 b,String why){require(a.distanceToSqr(b)<1e-8,why+" actual="+a+" expected="+b);}
    private static Object read(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(value);}catch(NoSuchFieldException ignored){}catch(IllegalAccessException error){throw new IllegalStateException(error);}throw new IllegalStateException(name);}
    private static CompoundTag saved(Entity entity){var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,entity.registryAccess());entity.saveWithoutId(out);return out.buildResult();}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner;final ServerLevel level;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final LbcIndirectKills.TerrainProvenance provenance;final SauvojenMeditationChecks.Attribution attribution;
        final LbcApocalypseEvents.Authority authority;final Vec3 origin;boolean closed;
        Fixture(GameController game){
            this.game=game;require(game.items().apocalypseActors().count()==0&&game.items().stonekivi().count()==0,"exclusive empty actor and terrain services");require(game.items().spellSources().current()==null,"no borrowed source scope");
            base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;level=owner.level();
            try{
                require(!game.items().timeStop().stopped(level),"no borrowed TimeStop");
                for(var world:game.server().getAllLevels())world.getAllEntities().forEach(before::add);
                for(var advancement:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(advancement,done);}
                for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
                for(var objective:game.server().getScoreboard().getObjectives()){var value=game.server().getScoreboard().getPlayerScoreInfo(owner,objective);if(value!=null)scores.put(objective,value.value());}
                base.initialize();ack();owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);origin=owner.position().add(4,4,-4);
                for(var cell:BlockPos.betweenClosed(BlockPos.containing(origin.add(-10,-10,-10)),BlockPos.containing(origin.add(10,10,10)))){require(level.hasChunkAt(cell),"resident bounded actor footprint "+cell);terrain.put(cell.immutable(),game.maps().snapshot(level,cell));require(level.getBlockState(cell).isAir(),"exclusive native air footprint "+cell);}
                provenance=game.items().indirectKills().captureTerrain(level,terrain.keySet());attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner));
                authority=new LbcApocalypseEvents.Authority(owner.getUUID(),game.items().indirectKills().wildCause(owner),game.items().epoch());
            }catch(RuntimeException|Error error){base.close();throw error;}
        }
        void ack(){owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(read(owner.connection,"awaitingPositionFromClient")!=null){var position=base.recording.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();owner.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(position.id(),owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot()));}}
        List<Entity> added(){var result=new ArrayList<Entity>();for(var world:game.server().getAllLevels())for(var entity:world.getAllEntities())if(!before.contains(entity)&&!(entity instanceof ServerPlayer))result.add(entity);return result;}
        void clear(){game.items().apocalypseActors().stop();base.clear();for(var entity:added()){game.items().entityUnloaded(entity);if(entity instanceof LivingEntity living)game.items().indirectKills().clear(living);entity.discard();game.items().spellSources().unloaded(entity);}}
        Entity cloud(LbcSauvojen.Kind kind){require(game.items().apocalypseActors().spawnCloud(authority,level,origin,kind),"actual explicit native display "+kind);var actor=game.items().apocalypseActors().snapshot().getFirst();require(actor.getType()==EntityTypes.ITEM_DISPLAY&&actor.entityTags().containsAll(Set.of("savoujen_cloud","magic","sauvojen_"+kind.name().toLowerCase(Locale.ROOT))),"native source cloud identity "+kind);return actor;}
        Projectile shot(LbcSauvojen.Kind kind){return switch(kind){
            case RUBY->game.items().ruby().snapshot().getFirst();case AIR->game.items().air().snapshot().getFirst();case ACID->game.items().clouds().acidSnapshot().getFirst();case TELEPORT->game.items().teleport().snapshot().getFirst();case STONE->game.items().stone().snapshot().getFirst();case BLIND->game.items().blind().snapshot().getFirst();
            case TERRA->(Projectile)added().stream().filter(LbcTerra.TerraProjectile.class::isInstance).findFirst().orElseThrow();case FIRE->(Projectile)added().stream().filter(e->e.getType()==EntityTypes.SMALL_FIREBALL).findFirst().orElseThrow();default->throw new IllegalArgumentException();};}
        void step(LbcSauvojen.Kind kind,Projectile shot){switch(kind){case RUBY->game.items().ruby().tick();case TERRA->game.items().terra().tick();case AIR->game.items().air().tick();case ACID->{level.tickNonPassenger(shot);game.items().clouds().tick();}case TELEPORT->game.items().teleport().tick();case STONE->game.items().stone().tick();case BLIND->game.items().blind().tick();case FIRE->level.tickNonPassenger(shot);case ELECTRO->game.items().wands().tick();}}
        void payloads(){
            var nether=game.server().getLevel(Level.NETHER);require(nether!=null&&owner.teleportTo(nether,origin.x,origin.y,origin.z,Set.of(),0,0,true),"actual causal author changes native dimension");ack();
            try{
                var emitter=cloud(LbcSauvojen.Kind.RUBY);var policy=game.items().spellSources().source(emitter);
                game.items().sauvojen().emitHostile(level,LbcSauvojen.Kind.RUBY,origin,new Vec3(1,0,0),policy);var first=shot(LbcSauvojen.Kind.RUBY);step(LbcSauvojen.Kind.RUBY,first);require(!first.isRemoved()&&first.level()==level&&owner.level()==nether,"accepted shot survives actual author dimension change");clear();
                var lookup=game.server().getPlayerList().getPlayersByUUID();require(lookup.remove(owner.getUUID(),owner),"mask only actual online author resolver, preserving real participant");
                try{
                    require(game.server().getPlayerList().getPlayer(authority.author())==null,"native nullable author resolver");
                    for(var kind:LbcSauvojen.Kind.values()){
                        emitter=cloud(kind);policy=game.items().spellSources().source(emitter);require(policy!=null&&policy.persistent()&&policy.cause().equals(authority.cause()),"persistent actual captured WILD authority "+kind);
                        game.items().sauvojen().emitHostile(level,kind,origin,new Vec3(1,0,0),policy);
                        require(game.items().spellSources().current()==null,"producer restores caller source "+kind);
                        if(kind==LbcSauvojen.Kind.ELECTRO){var traces=game.items().wands().electroSnapshot();require(traces.size()==4,"four actual offline-safe electrical records");for(var trace:traces)require(trace.lifetime==20&&trace.sourcePolicy==policy&&trace.owner==null,"electrical source20 and no fabricated author");step(kind,null);for(var trace:traces){require(trace.age==1,"actual electrical step");near(trace.position,origin.add(3.6,0,0),"source electrical motion");}}
                        else{
                            var shot=shot(kind);require(game.items().spellSources().source(shot)!=null&&game.items().spellSources().source(shot).cause().equals(authority.cause())&&game.items().spellSources().owner(shot)==null,"real charge retains captured UUID cause without online player "+kind);
                            double speed=switch(kind){case AIR->.8;case TELEPORT->1.2;case STONE->1.6;case FIRE->.7;case BLIND->.99;case ACID->.5;default->1.8;};near(shot.position(),origin,"actual source origin "+kind);near(shot.getDeltaMovement(),new Vec3(speed,0,0),"actual source velocity "+kind);
                            switch(kind){case RUBY->require(((LbcRuby.Charge)shot).sourceLifetime==60,"Ruby60");case TERRA->require(((LbcTerra.TerraProjectile)shot).sourceLifetime==60,"Terra60");case AIR->require(((LbcAir.Charge)shot).sourceLifetime==120,"Air120");case ACID->require(LbcClouds.acidLifetime((LbcClouds.AcidProjectile)shot)==300,"Acid300");case TELEPORT->require(((LbcTeleport.Charge)shot).sourceLifetime==60,"Teleport60");case STONE->require(((LbcStone.Charge)shot).lifetime==80,"Stone80");case BLIND->require(((LbcBlind.Charge)shot).lifetime==120,"Blind120");default->{}}
                            step(kind,shot);require(!shot.isRemoved(),"same native source engine survives absent causal author "+kind);if(kind!=LbcSauvojen.Kind.ACID)near(shot.position(),origin.add(kind==LbcSauvojen.Kind.FIRE?(speed+.1)*(double).95F:speed,0,0),"native engine motion "+kind);
                        }
                        require(game.items().spellSources().current()==null,"async engine restores source after null-author branch "+kind);clear();
                    }
                    offlineWitness();
                }finally{require(lookup.put(owner.getUUID(),owner)==null,"restore exact native author resolver entry");}
            }finally{require(owner.teleportTo(level,base.saved.position.x,240,base.saved.position.z,Set.of(),0,0,true),"return actual participant to original dimension");ack();}
        }
        void offlineWitness(){
            var emitter=cloud(LbcSauvojen.Kind.TERRA);var policy=game.items().spellSources().source(emitter);var cow=new Cow(EntityTypes.COW,level);cow.setPos(origin.add(.2,0,.2));cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(level.addFreshEntity(cow),"actual offline source damage witness");
            game.items().sauvojen().emitHostile(level,LbcSauvojen.Kind.TERRA,origin,new Vec3(0,0,1),policy);game.items().terra().tick();
            require(cow.getHealth()==993&&cow.getLastDamageSource()!=null&&cow.getLastDamageSource().is(DamageTypes.CACTUS)&&LbcIndirectKills.isWildCaptured(cow.getLastDamageSource()),"native source7 cactus and captured WILD after author resolution disappears");require(read(cow.getLastDamageSource(),"cause").equals(authority.cause()),"native damage retains exact captured original causal order/UUID");
            try(var scope=game.items().spellSources().scope(policy,level)){var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(beta!=null&&game.items().wands().target(null,beta),"nullable source admits actual player body independent of author dimension");try(var nested=game.items().spellSources().scope((LbcSpellSource.Source)null)){require(game.items().spellSources().current()==null,"ordinary reaction resets source");}require(game.items().spellSources().current()==policy,"reaction restores exact source");}
            require(game.items().spellSources().current()==null,"witness caller scope restored");clear();
        }
        @SuppressWarnings("unchecked") void worm(){
            require(game.items().apocalypseActors().spawnWorm(authority,level,origin),"native worm summon/class initialization");var slime=(Slime)game.items().apocalypseActors().snapshot().getFirst();slime.setNoAi(true);slime.setNoGravity(true);
            require(slime.getSize()==21&&slime.getAttribute(Attributes.MAX_HEALTH).getBaseValue()==441&&saved(slime).getIntOr("Size",-1)==20,"actual native Size20 serialization and size21 health");
            var center=BlockPos.containing(origin.add(0,3,0));for(var cell:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5)))level.setBlock(cell,Blocks.STONE.defaultBlockState(),2|16|32);var outside=center.offset(6,0,0);level.setBlock(outside,Blocks.GOLD_BLOCK.defaultBlockState(),2|16|32);
            for(int i=0;i<5;i++)game.items().apocalypseActors().tick();
            var queue=(Collection<LbcStonekivi.Work>)read(game.items().stonekivi(),"work");require(queue.size()==1,"one actual worm terrain work after five service steps");for(var work:List.copyOf(queue)){require(work.ready()&&work.step(),"native resident strict terrain work");queue.remove(work);}
            int air=0;for(var cell:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5)))if(level.getBlockState(cell).isAir())air++;require(air==1331&&level.getBlockState(outside).is(Blocks.GOLD_BLOCK),"exact native11cube1331 air cells and untouched outside control");
            require(slime.hurtServer(level,slime.damageSources().genericKill(),Float.MAX_VALUE)&&slime.isDeadOrDying(),"actual native slime death");slime.remove(Entity.RemovalReason.KILLED);
            var children=added().stream().filter(e->e!=slime&&!e.isRemoved()&&e instanceof Slime).map(Slime.class::cast).toList();require(children.size()>=2&&children.size()<=4,"actual native split count2..4");for(var child:children){var source=game.items().spellSources().source(child);require(child.getSize()==10&&!child.entityTags().contains("worm")&&!child.entityTags().contains("lucky_mob")&&!LbcApocalypseActors.managed(child),"native halfsize10 split child without duplicated worm authority");require(source!=null&&source.persistent()&&source.cause().equals(authority.cause()),"native conversion preserves accepted WILD causal source");}
            clear();terrain.forEach((cell,state)->game.maps().restoreSnapshot(level,cell,state));game.items().indirectKills().restoreTerrain(provenance);
        }
        void stevari(){
            require(game.items().apocalypseActors().spawnStevari(authority,level,origin),"actual Stevari SNBT class/payload admission");var skeleton=(Skeleton)game.items().apocalypseActors().snapshot().getFirst();require(!skeleton.isNoAi()&&skeleton.getMainHandItem().is(Items.BONE)&&skeleton.getItemBySlot(EquipmentSlot.FEET).is(Items.BONE_MEAL)&&skeleton.getItemBySlot(EquipmentSlot.FEET).getCount()==64&&skeleton.getItemBySlot(EquipmentSlot.LEGS).getCount()==32,"native skeleton melee equipment, no variant alias");
            require(skeleton.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()==.6&&skeleton.getAttribute(Attributes.ARMOR).getBaseValue()==20&&skeleton.getAttribute(Attributes.ATTACK_KNOCKBACK).getBaseValue()==100&&skeleton.getAttribute(Attributes.ARMOR_TOUGHNESS).getBaseValue()==5,"exact Stevari native bases");
            var data=saved(skeleton);require(data.getStringOr("DeathLootTable","").equals("lbc:fatskeleton")&&data.getBooleanOr("PersistenceRequired",false)&&data.getBooleanOr("CustomNameVisible",false),"actual original loot/AI persistence identity");
            var modifiers=skeleton.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();var ids=new HashSet<String>();for(var modifier:modifiers)ids.add(modifier.modifier().id().toString());require(modifiers.size()==4&&ids.contains("minecraft:sdiuirj4938uhf")&&ids.contains("minecraft:idjf9u8weu89"),"exact Stevari weapon modifier IDs differ from other fat-skeleton variants");clear();
        }
        public void close(){if(closed)return;closed=true;try{clear();game.items().stonekivi().stop();if(owner.level()!=level){owner.teleportTo(level,base.saved.position.x,base.saved.position.y,base.saved.position.z,Set.of(),base.saved.yaw,base.saved.pitch,true);ack();}}finally{try{base.close();stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));advancements.forEach((a,done)->{for(var criterion:a.value().criteria().keySet())if(done.contains(criterion))owner.getAdvancements().award(a,criterion);else owner.getAdvancements().revoke(a,criterion);});for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(owner,objective,true).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(owner,objective);}finally{terrain.forEach((cell,state)->game.maps().restoreSnapshot(level,cell,state));game.items().indirectKills().restoreTerrain(provenance);attribution.close();}}}
    }
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.stevari();fixture.worm();fixture.payloads();}SkyWars2.LOGGER.info("[check] PASS apocalypse-actors (native Stevari exact payload; native Size20 worm1331-cell strict cube/split2..4halfsize10 inheritedWILD;9 same native spell engines with absent online resolver and actual foreign-dimension author; actual7 cactus/captured cause; scope restoration; no fake player or private clock rewriting)");SkyWars2.LOGGER.info("[check] PENDING apocalypse-actors: natural server cadence/long lifetimes/full three-event schedules and physical display presentation require separate root qualification; five worm service calls prove native dispatcher payload, not elapsed wall time");}
}
