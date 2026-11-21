package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import me.skywars2.SkyWars2;
import me.skywars2.game.entities.projectiles.LbcClouds;
import me.skywars2.game.items.weapons.firearms.LbcAk47;
import me.skywars2.game.match.checkpoints.MatchCoreCheckpoint;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import me.skywars2.game.match.checkpoints.MatchRandomState;
import me.skywars2.game.match.checkpoints.MatchWorldCheckpoint;
import me.skywars2.game.match.checkpoints.MatchWorldRandomCheckpoint;
import me.skywars2.game.match.events.LbcAbsoluteCinema;
import me.skywars2.game.match.events.LbcOreshnik;
import me.skywars2.game.match.events.LbcPozdmanBot;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Scoped native Cinema checks, including one complete nineteen-site parent impact. */
final class CinemaNativeChecks {
    private CinemaNativeChecks(){}
    private static Fixture running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Cinema native: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-4,why+" actual="+actual+" expected="+expected);}
    private static Field field(Object object,String name){for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object get(Object target,String name){try{return field(target,name).get(target);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    static void start(GameController game,boolean rewind){require(running==null,"exclusive fixture");AbsoluteCinemaChecks.sourceRules();var f=new Fixture(game,rewind);running=f;try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    static void tick(GameController game){if(running==null)return;var f=running;try{require(game==f.game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} ({})",f.name(),f.rewind?"TECH/TimeStop, bounded native chunk failures, angled approach, eleven children per parent, native clone and rewind, same-tick nineteen-site explosion":"actual finite AK/8 owner-safe acids/Lenin/Trump native criteria/Pozdman native death wolf/Nahida native death creative");}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+f.name(),error);}}

    private static final class Member {
        ServerPlayer p;final RegenerationBeltChecks.Snapshot saved;RegenerationBeltChecks.ScheduledClient client;final int kills,killedBy;
        Member(GameController game,ServerPlayer p){this.p=p;kills=p.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER));killedBy=p.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER));saved=new RegenerationBeltChecks.Snapshot(game,p);client=new RegenerationBeltChecks.ScheduledClient(game,p);}
        void respawn(GameController game){client.close();var old=p;p=game.server().getPlayerList().respawn(old,false,Entity.RemovalReason.KILLED);require(p!=old&&p.isAlive(),"real native replacement");p.connection.resetPosition();var ack=(MatchPlayerCheckpoint.SimulatedAcknowledgement)p.connection;ack.skywars2$beginRespawn();ack.skywars2$acknowledgeRestore();set(saved,"player",p);set(saved.state,"player",p);client=new RegenerationBeltChecks.ScheduledClient(game,p);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final LbcAbsoluteCinema service;final LbcOreshnik rockets;final ServerLevel level;
        final Member owner,victim,attacker;final List<Member> members;final MatchCoreCheckpoint core;final MatchWorldCheckpoint world;final MatchWorldRandomCheckpoint random;
        final List<MatchRandomState> playerRandom;final Set<UUID> originals=new HashSet<>();final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final boolean frozen;final int started;final Vec3 origin,remote;int stage,when;boolean closed,paused,restoring;Marker jammer;Cow cow;Map<String,Object> picture;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;var alive=game.alivePlayers();require(game.playing()&&!game.bedwars().mode()&&!game.developerMode()&&alive.size()>=3,"three ordinary active native participants");
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(alive.getFirst().level())&&game.items().stonekivi().queuedSnapshot().isEmpty(),"exclusive unpaused coordinator and no accepted work");
            service=game.items().cinema();rockets=service.rockets();require(((Map<?,?>)get(service,"roles")).isEmpty()&&service.trumpRule()==0&&rockets.flightCount()==0,"fresh canonical Cinema owner");
            frozen=game.server().tickRateManager().isFrozen();core=new MatchCoreCheckpoint(game);world=new MatchWorldCheckpoint(game);random=new MatchWorldRandomCheckpoint(game);
            owner=new Member(game,alive.get(0));victim=new Member(game,alive.get(1));attacker=new Member(game,alive.get(2));members=List.of(owner,victim,attacker);level=owner.p.level();
            playerRandom=members.stream().map(m->MatchRandomState.capture(m.p.getRandom())).toList();level.getAllEntities().forEach(e->originals.add(e.getUUID()));origin=owner.p.position();remote=new Vec3(Math.floor(origin.x)+4096.5,Math.min(240,level.getMaxY()-40),Math.floor(origin.z)+4096.5);started=game.server().getTickCount();
        }
        String name(){return rewind?"cinema-rewind":"cinema-native";}
        void next(int value){stage=value;when=game.server().getTickCount();}
        void move(Member member,Vec3 point){require(member.p.teleportTo(level,point.x,point.y,point.z,Set.of(),0,0,true),"native fixture teleport");member.p.setNoGravity(true);member.p.setDeltaMovement(Vec3.ZERO);member.client.acknowledge();}
        void pause(){game.items().timeStop().begin(level,10000);paused=true;}
        void thaw(){game.items().timeStop().end(level);paused=false;}
        List<Entity> fresh(){var result=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!originals.contains(e.getUUID())&&!(e instanceof ServerPlayer))result.add(e);return result;}
        void remove(Entity e){game.items().magic().unloaded(e);game.items().clouds().unloaded(e);game.items().summons().unloaded(e);game.items().communism().removed(e);if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
        void zone(String tag){if(jammer!=null){remove(jammer);jammer=null;}if(tag!=null){jammer=new Marker(EntityTypes.MARKER,level);jammer.setPos(owner.p.position());jammer.entityTags().add(tag);require(level.addFreshEntity(jammer),"native jammer publication");game.items().magic().loaded(jammer);}}
        void begin(){
            game.server().tickRateManager().setFrozen(false);game.config().lbcSkills=false;game.config().lbcKits=false;game.config().lbcLevelups=false;game.config().lbcTrinketLimit=0;game.config().vanillaKits=false;game.config().vanillaSkills=false;
            for(var m:members){game.lbc().clear(m.p);game.players().get(m.p.getUUID()).lbcSkill=0;m.p.closeContainer();m.p.getInventory().clearContent();m.p.removeAllEffects();m.p.entityTags().removeAll(Set.of("skywars2.debug_perks","sin_vanity"));m.p.setGameMode(GameType.SURVIVAL);m.p.setNoGravity(true);m.p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);m.p.setHealth(1000);m.p.setAbsorptionAmount(0);for(var key:List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS)){var attribute=m.p.getAttribute(key);attribute.removeModifiers();attribute.setBaseValue(0);}}
            move(victim,origin.add(8,0,0));move(attacker,origin.add(12,0,0));
            if(rewind){zone("stopper");service.startRoll(owner.p,1,List.of(remote));require(rockets.flightCount()==0,"TECH blocks launch without rerolling Putin");service.forget(owner.p);zone(null);
                coldLoadFailures();
                owner.p.getRandom().setSeed(2);service.startRoll(owner.p,1,List.of(remote));require(rockets.flightCount()==1,"actual source Putin parent accepted");
                var parent=flights().getFirst();var start=(Vec3)get(parent,"at");float pitch=(float)get(parent,"pitch");
                require(get(parent,"target").equals(remote)&&pitch>=10&&pitch<=60,"parent retains selected target and requested pitch bounds");
                near(start.distanceTo(remote),100,"three-dimensional launch distance");
                int split=(int)get(parent,"splitAge");require(split>5,"seed leaves time to clone before the steep-approach split");
                near(Math.floor(start.y+((Vec3)get(parent,"direction")).y*split-remote.y),59,"split crosses the old altitude within one movement step");
                loadedRegion(start,0);next(20);}
            else nativeRoles();
        }
        int count(Item item){int count=0;for(int slot=0;slot<owner.p.getInventory().getContainerSize();slot++){var stack=owner.p.getInventory().getItem(slot);if(stack.is(item))count+=stack.getCount();}return count;}
        String inventory(){var rows=new ArrayList<String>();for(int slot=0;slot<owner.p.getInventory().getContainerSize();slot++){var stack=owner.p.getInventory().getItem(slot);if(!stack.isEmpty())rows.add(slot+"="+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())+" x"+stack.getCount()+" finite="+LbcAk47.isFinite(stack));}return rows.toString();}
        void nativeRoles(){
            service.startRoll(owner.p,401,List.of());require(count(LbcAk47.ITEM)==1,"Stalin grants exactly one canonical AK; inventory="+inventory());for(int slot=0;slot<owner.p.getInventory().getContainerSize();slot++){var stack=owner.p.getInventory().getItem(slot);if(stack.is(LbcAk47.ITEM))require(LbcAk47.isFinite(stack),"Stalin native AK retains finite source flags; inventory="+inventory());}service.bodyReady(owner.p);require(count(LbcAk47.ITEM)==1,"Stalin body refresh never repeats start grant");service.forget(owner.p);owner.p.getInventory().clearContent();
            var spawn=game.maps().spawns().stream().filter(s->s.position().distanceToSqr(owner.p.position())>=25).findFirst().orElseThrow(()->new IllegalStateException("Cinema acid fixture needs a foreign source spawn"));move(victim,spawn.position());
            var team=owner.p.getTeam();require(team!=null,"real owner team");var old=victim.p.getTeam();game.server().getScoreboard().addPlayerToTeam(victim.p.getScoreboardName(),team);
            service.startRoll(owner.p,101,List.of());var shots=fresh().stream().filter(LbcClouds::sourceAcidManaged).filter(e->e.position().distanceToSqr(spawn.position().add(0,2,0))<.01).toList();require(shots.size()==8,"actual occupied foreign spawn produces eight source acids");
            for(var shot:shots)require(!game.items().clouds().sourceAcidWild(shot)&&game.items().clouds().sourceAcidOwner(shot)==owner.p&&!game.items().clouds().sourceAcidCanAffect(shot,owner.p)&&!game.items().clouds().sourceAcidCanAffect(shot,victim.p)&&LbcClouds.acidLifetime((LbcClouds.AcidProjectile)shot)==500,"source500 acid is owned and respects owner/allied team");
            if(old==null)game.server().getScoreboard().removePlayerFromTeam(victim.p.getScoreboardName(),team);else game.server().getScoreboard().addPlayerToTeam(victim.p.getScoreboardName(),old);
            for(var e:fresh())if(LbcClouds.sourceAcidManaged(e))remove(e);service.forget(owner.p);move(victim,origin.add(8,0,0));
            zone("stopper_magic");service.startRoll(owner.p,301,List.of());cow=new Cow(EntityTypes.COW,level);cow.setPos(origin.add(.4,0,.4));cow.setNoAi(true);require(level.addFreshEntity(cow),"actual nearby passive cow");next(0);
        }
        void trump(int wanted){
            // Nearby LegacyRandom seeds have correlated first nextInt(2) values:
            // seeds1..128 all choose rule2. Advance one real canonical stream instead.
            owner.p.getRandom().setSeed(0x43494E454D41L);var observed=new ArrayList<Integer>();
            for(int attempt=0;attempt<128;attempt++){
                service.forget(owner.p);service.startRoll(owner.p,201,List.of());observed.add(service.trumpRule());
                if(service.trumpRule()==wanted)return;
            }
            throw new IllegalStateException("No bounded deterministic source Trump rule "+wanted+"; native outcomes="+observed);
        }
        void hit(Member target,DamageSource source,float amount){for(var m:members){m.p.setInvulnerableTime(0);m.p.damageCooldownTime=0;}require(target.p.hurtServer(level,source,amount),"actual accepted native hit "+source);}
        boolean nativeStep(){
            if(stage==0){require(game.items().communism().owner(cow)==owner.p&&cow.getTeam()==owner.p.getTeam(),"actual Lenin tick subjugates passive neighbor despite MAG");remove(cow);service.forget(owner.p);
                require(!attacker.p.isAlliedTo(victim.p)&&!owner.p.isAlliedTo(victim.p),"hostile native criterion contexts");trump(1);float before=attacker.p.getHealth(),authorHp=owner.p.getHealth();hit(victim,victim.p.damageSources().playerAttack(attacker.p),1);near(attacker.p.getHealth(),before-30,"native melee criterion punishes attacker30");near(owner.p.getHealth(),authorHp,"Cinema retaliation reentry guard preserves author health");
                trump(2);before=attacker.p.getHealth();var magic=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),attacker.p);hit(victim,magic,1);near(attacker.p.getHealth(),before-30,"native non-player-attack magic qualifies without fake arrow-only filter");before=attacker.p.getHealth();hit(victim,victim.p.damageSources().playerAttack(attacker.p),1);near(attacker.p.getHealth(),before,"ranged rule excludes native player-attack tag");service.forget(owner.p);
                service.startRoll(owner.p,501,List.of());require(count(LbcPozdmanBot.ITEM)==1,"Pozdman grants exactly one native bot");game.addRuneLives(victim.p,2-game.lives(victim.p.getUUID()));hit(victim,victim.p.damageSources().playerAttack(owner.p),10000);require(victim.p.isDeadOrDying()&&game.lives(victim.p.getUUID())==1,"actual Pozdman melee player life loss");
                var wolves=fresh().stream().filter(Wolf.class::isInstance).map(Wolf.class::cast).toList();require(wolves.size()==1,"one wolf from the genuine native death boundary");var wolf=wolves.getFirst();require(wolf.getOwner()==owner.p&&game.items().communism().owner(wolf)==owner.p,"actual wolf belongs to killer through native and controller ownership");near(wolf.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),40,"source wolf40 HP");near(wolf.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(),9,"source wolf9 attack");next(1);return false;
            }
            if(stage==1){victim.respawn(game);service.forget(owner.p);service.startRoll(owner.p,601,List.of());game.addRuneLives(owner.p,1-game.lives(owner.p.getUUID()));hit(owner,owner.p.damageSources().playerAttack(attacker.p),10000);require(owner.p.isDeadOrDying()&&game.lives(owner.p.getUUID())==1,"actual last life without bed receives Nahida's single bonus life");next(2);return false;}
            owner.respawn(game);require(owner.p.gameMode()==GameType.CREATIVE&&service.role(owner.p.getUUID())==LbcAbsoluteCinema.Role.NAHIDA,"actual native Nahida body awakens despite MAG");require(!service.beforeLastLifeLost(owner.p,1,false),"awakening cannot repeat");service.forget(owner.p);require(owner.p.gameMode()==GameType.SURVIVAL,"retirement releases installed creative lease");return true;
        }
        @SuppressWarnings("unchecked") List<Object> flights(){return new ArrayList<>((Set<Object>)get(rockets,"flights"));}
        Map<String,Object> image(Object flight){var result=new LinkedHashMap<String,Object>();for(String key:List.of("at","age","closed","child","owner","cause","epoch"))result.put(key,get(flight,key));return result;}
        void loadedRegion(Vec3 point,int reach){var p=BlockPos.containing(point);for(int x=(p.getX()-reach)>>4;x<=(p.getX()+reach)>>4;x++)for(int z=(p.getZ()-reach)>>4;z<=(p.getZ()+reach)>>4;z++)level.getChunk(x,z);}
        @SuppressWarnings("unchecked") Map<ChunkPos,Object> leases(){
            return ((Map<ServerLevel,Map<ChunkPos,Object>>)get(get(rockets,"leases"),"leases")).getOrDefault(level,Map.of());
        }
        void exhaustColdLoad(ChunkPos chunk,Runnable step,BooleanSupplier waiting){
            var lease=leases().get(chunk);
            require(lease!=null&&level.getChunkSource().getChunkNow(chunk.x(),chunk.z())==null,"cold native request exists at "+chunk);
            var requested=List.copyOf(leases().keySet());
            int now=game.server().getTickCount();
            var first=CompletableFuture.completedFuture(ChunkResult.error("controlled Oreshnik first native load failure"));
            set(lease,"load",first);
            step.run();
            require(waiting.getAsBoolean()&&(int)get(lease,"retryTick")==now&&get(lease,"load")!=first,
                    "the first terminal result schedules exactly one real native retry");
            var last=CompletableFuture.completedFuture(ChunkResult.error("controlled Oreshnik retry failure"));
            set(lease,"load",last);
            step.run();
            require(waiting.getAsBoolean()&&get(lease,"load")==last,"same-tick scheduler revisits cannot spend another retry or cancel");
            // Model a later observer tick without letting asynchronous native generation warm the witness.
            set(lease,"retryTick",now-1);
            step.run();
            var storage=(TicketStorage)get(level.getChunkSource(),"ticketStorage");
            var type=(TicketType)get(rockets,"TICKET");
            require(!waiting.getAsBoolean()&&leases().isEmpty()
                            &&requested.stream().noneMatch(pos->storage.getTickets(pos.pack()).stream().anyMatch(ticket->ticket.getType()==type)),
                    "exhausted native load removes the actual work and releases every Oreshnik ticket");
        }
        void coldLoadFailures(){
            var grid=BlockPos.containing(remote);
            // Keep these independent scenes in the same region as the existing remote Cinema fixture.
            int regionX=grid.getX()&~511,regionZ=grid.getZ()&~511;
            var approachTarget=new Vec3(regionX+240.5,remote.y,regionZ+240.5);
            require(rockets.launch(owner.p,approachTarget),"real parent flight accepted for cold approach");
            var parent=flights().getFirst();
            var next=((Vec3)get(parent,"at")).add((Vec3)get(parent,"direction"));
            var approachChunk=ChunkPos.containing(BlockPos.containing(next));
            rockets.tick();
            require((int)get(parent,"age")==0&&rockets.blastCount()==0,"cold flight waits before advancing age");
            exhaustColdLoad(approachChunk,rockets::tick,()->rockets.flightCount()==1);
            require(rockets.flightCount()==0&&rockets.blastCount()==0,"failed approach retires the parent without an impact");

            var target=new Vec3(regionX+400.5,remote.y,regionZ+400.5);
            loadedRegion(target,0);
            var block=BlockPos.containing(target);
            terrain.put(block,game.maps().snapshot(level,block));
            level.setBlock(block,Blocks.STONE.defaultBlockState(),3);
            require(rockets.launch(owner.p,target),"real parent accepted for cold blast tests");
            parent=flights().getFirst();
            set(parent,"age",99);
            set(parent,"at",target.subtract((Vec3)get(parent,"direction")));
            rockets.tick();
            require(rockets.flightCount()==0&&rockets.blastCount()==19&&game.items().stonekivi().queuedSnapshot().size()==1,
                    "native impact is accepted and queues its complete blast behind cold neighbors");
            var neighbor=leases().keySet().stream().filter(pos->level.getChunkSource().getChunkNow(pos.x(),pos.z())==null).findFirst().orElseThrow();
            exhaustColdLoad(neighbor,game.items().stonekivi()::tick,()->rockets.blastCount()==19);
            require(rockets.blastCount()==0&&game.items().stonekivi().queuedSnapshot().isEmpty()&&level.getBlockState(block).is(Blocks.STONE),
                    "failed impact retires the entire queued blast without touching its actual terrain witness");
        }
        void smokePackets(){
            for(var flight:flights()) loadedRegion((Vec3)get(flight,"at"),3);
            rockets.tick();
            var expected=new ArrayList<Vec3>();
            for(var flight:flights()){
                var display=(Display.ItemDisplay)get(flight,"display");require(display!=null&&display.getViewRange()==1000,"all split models published with extended view range before smoke witness");
                var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
                display.saveWithoutId(output);
                var transform=net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult())
                        .read("transformation",com.mojang.math.Transformation.EXTENDED_CODEC).orElseThrow();
                // Independent renderer matrix from the actual native display's serialized transform.
                var point=new org.joml.Matrix4f().rotateY((float)(-Math.PI/180)*display.getYRot()).rotateX((float)(Math.PI/180)*display.getXRot())
                        .mul(transform.getMatrix()).rotateY((float)Math.PI).translate(-.5f,-.5f,-.5f)
                        .transformPosition(new org.joml.Vector3f(.5f,0,1));
                expected.add(display.position().add(point.x,point.y,point.z));
            }
            require(expected.size()==24,"two native parents and twenty-two native split rockets witnessed");
            move(owner,remote.add(0,0,8));owner.client.packets().clear();rockets.tick();
            var packets=owner.client.packets().stream().filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance)
                    .map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast)
                    .filter(p->p.particle()==net.minecraft.core.particles.ParticleTypes.CAMPFIRE_SIGNAL_SMOKE).toList();
            require(packets.size()==expected.size(),"one actual smoke packet per flight for nearby native listener");
            for(var packet:packets){
                var point=new Vec3(packet.x(),packet.y(),packet.z());int matched=-1;
                for(int i=0;i<expected.size();i++)if(expected.get(i).distanceToSqr(point)<1e-8){matched=i;break;}
                require(matched>=0,"smoke originates at rendered nozzle, not entity pivot: "+point);expected.remove(matched);
                require(packet.overrideLimiter()&&!packet.alwaysShow()&&packet.count()==5,"source visibility flags and five particles retained");
                near(packet.xDist(),.3,"source smoke X spread");near(packet.yDist(),.3,"source smoke Y spread");near(packet.zDist(),.3,"source smoke Z spread");
                near(packet.xMaxSpeed(),.05,"source smoke speed");
            }
            require(expected.isEmpty(),"every parent and child nozzle has exactly one emission");
            SkyWars2.LOGGER.info("[check] Cinema smoke: actual24 nozzle-aligned packets, native serialized transforms and all source particle parameters preserved");
        }
        boolean rewindStep(){
            if(stage==20){if(flights().isEmpty()||get(flights().getFirst(),"display")==null)return false;pause();picture=image(flights().getFirst());next(21);return false;}
            if(stage==21){require(image(flights().getFirst()).equals(picture),"TimeStop holds actual Oreshnik flight");if(game.server().getTickCount()-when<3)return false;thaw();var display=(Entity)get(flights().getFirst(),"display");require(game.items().cloneSkills().copyNow(display,15)&&rockets.flightCount()==2,"native x2 reconstructs one causal flight rather than inert display");next(22);return false;}
            if(stage==22){
                require(flights().stream().filter(f->!(boolean)get(f,"child")).count()==2,"both parents reach the split: "+flights().stream().map(this::image).toList());
                if(flights().stream().filter(f->(boolean)get(f,"child")).count()<22){
                    require(game.server().getTickCount()-when<300,"both parents complete the split: "+flights().stream().filter(f->!(boolean)get(f,"child")).map(this::image).toList());return false;
                }
                require(rockets.flightCount()==24,"each actual cloned parent creates eleven children");
                var parentDirection=(Vec3)get(flights().stream().filter(f->!(boolean)get(f,"child")).findFirst().orElseThrow(),"direction");
                for(var child:flights())if((boolean)get(child,"child")){
                    var direction=(Vec3)get(child,"direction");require(direction.y<0&&direction.dot(parentDirection)>.9,"cluster scatter remains close to its inclined parent trajectory");
                }
                smokePackets();rockets.stop();
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,true,game.server());
                var block=BlockPos.containing(remote);var target=Vec3.atCenterOf(block);
                loadedRegion(target,25);terrain.put(block,game.maps().snapshot(level,block));level.setBlock(block,Blocks.STONE.defaultBlockState(),3);
                require(rockets.launch(owner.p,target),"supporting parent kernel launch");
                var parent=flights().getFirst();set(parent,"age",99);set(parent,"at",target.subtract((Vec3)get(parent,"direction")));
                owner.client.packets().clear();rockets.tick();
                var explosions=owner.client.packets().stream().filter(net.minecraft.network.protocol.game.ClientboundExplodePacket.class::isInstance)
                        .map(net.minecraft.network.protocol.game.ClientboundExplodePacket.class::cast).toList();
                require(rockets.flightCount()==0&&rockets.blastCount()==0&&level.getBlockState(block).isAir(),"impact removes the target block and finishes the whole kernel in the same tick");
                require(explosions.size()==19&&explosions.getFirst().center().equals(target),"all native blast packets, starting at the exact target");
                require(rockets.launch(owner.p,remote.add(40,20,0)),"another flight for the native rewind");
                loadedRegion((Vec3)get(flights().getFirst(),"at"),3);rockets.tick();pause();picture=image(flights().getFirst());
                require(game.vanity().requestCapture(owner.p,false),"actual whole coordinator captures the live flight");next(23);return false;
            }
            if(stage==23){require(image(flights().getFirst()).equals(picture),"TimeStop preserves flight during capture");if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;thaw();next(24);return false;}
            if(stage==24){if(image(flights().getFirst()).equals(picture))return false;pause();require(game.vanity().requestRestoreForCheck(),"actual whole coordinator rewinds the flight");restoring=true;next(25);return false;}
            if(game.vanity().busy())return false;require(image(flights().getFirst()).equals(picture)&&service.role(owner.p.getUUID())==LbcAbsoluteCinema.Role.PUTIN,"typed whole rewind restores source role and the angled flight");game.vanity().stop();thaw();restoring=false;return true;
        }
        boolean step(){for(var m:members)if(m.p.isAlive())m.client.acknowledge();require(game.server().getTickCount()-started<1600,"bounded scheduled fixture "+name()+" stage="+stage);if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();return rewind?rewindStep():nativeStep();}
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;for(Runnable action:List.<Runnable>of(
                ()->{game.vanity().stop();if(paused)thaw();service.stop();zone(null);},()->{for(var m:members)if(!m.p.isAlive())m.respawn(game);},
                ()->{for(var e:fresh())remove(e);for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),818);},
                ()->{game.server().tickRateManager().setFrozen(true);core.restoreFrozen();world.restoreFrozen();random.restoreFrozen();},
                ()->{for(int n=0;n<members.size();n++){var m=members.get(n);m.saved.restoreAttributes();m.saved.restore(game);m.saved.restoreUse();m.p.getStats().setValue(m.p,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER),m.kills);m.p.getStats().setValue(m.p,net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER),m.killedBy);playerRandom.get(n).restore(m.p.getRandom());}game.players().save();},
                ()->{for(var m:members)m.client.close();},()->game.server().tickRateManager().setFrozen(frozen)))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw new IllegalStateException("Cinema scoped restoration",failure);}
    }
}
