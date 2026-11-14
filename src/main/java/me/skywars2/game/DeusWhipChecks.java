package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual item input and finite observations of the registered server/player scheduler. */
public final class DeusWhipChecks {
    private DeusWhipChecks(){}
    private static State running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Deus whip: "+why);}
    private static void near(double a,double b,String why){require(Math.abs(a-b)<1e-4,why+" actual="+a+" expected="+b);}
    private static final class State implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture f;final ServerPlayer p,beta;final LbcDeus service;
        final RegenerationBeltChecks.Snapshot saved,betaSaved;
        final RegenerationBeltChecks.ScheduledClient client,audience;
        final List<net.minecraft.network.protocol.Packet<?>> packets;
        final List<Cow> cows=new ArrayList<>();final Map<Cow,Vec3> homes=new LinkedHashMap<>();
        final List<Marker> fields=new ArrayList<>();final Map<Marker,MapManager.BlockSnapshot> fieldBlocks=new IdentityHashMap<>();final Map<net.minecraft.world.item.Item,Integer> stats=new IdentityHashMap<>();
        final boolean frozen;int stage=-1,when,started,lastClock,frames,damages,ownerUpdates;boolean closed;
        private static final float[][] ANGLES={{0,0},{90,0},{180,0},{-90,0},{45,-60},{-45,60},{120,89},{-120,-89},{180,45},{0,-45}};
        int packetBoundary,poseIndex;
        List<Vec3> stablePath;Vec3 pose;float yaw,pitch;Cow pullTarget;double ordinaryPull;
        State(GameController game){this.game=game;
            p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(p!=null&&beta!=null,"two actual participants");saved=new RegenerationBeltChecks.Snapshot(game,p);betaSaved=new RegenerationBeltChecks.Snapshot(game,beta);
            f=new AllGrimoireChecks.Fixture(game);service=game.items().deus();f.reset();
            client=new RegenerationBeltChecks.ScheduledClient(game,p);audience=new RegenerationBeltChecks.ScheduledClient(game,beta);
            try{var field=((Object)client.connection).getClass().getDeclaredField("packets");field.setAccessible(true);@SuppressWarnings("unchecked") var list=(List<net.minecraft.network.protocol.Packet<?>>)field.get(client.connection);packets=list;}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            frozen=game.server().tickRateManager().isFrozen();require(!frozen,"live scheduler");
            for(var item:List.of(LbcPursuitItems.DEUS,Items.CARROT_ON_A_STICK))stats.put(item,p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(item)));
            for(var player:List.of(p,beta)){player.getInventory().clearContent();player.removeAllEffects();player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.setKnownMovement(Vec3.ZERO);player.getFoodData().setFoodLevel(8);player.getFoodData().setSaturation(0);var profile=game.players().get(player.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);game.items().mana().baseRegen(player,0);game.items().magic().inventoryChanged(player);}
            position(beta,f.at.add(14,0,0));game.items().magic().flushCleansing();
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(f.at).offset(-12,-2,-12),BlockPos.containing(f.at).offset(12,8,18)))f.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);
            started=when=game.server().getTickCount();ownerUpdates=client.updates();
        }
        void position(ServerPlayer player,Vec3 at){player.teleportTo(f.level,at.x,at.y,at.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);if(player==p)client.acknowledge();else audience.acknowledge();}
        int clock(){return service.cooldown(p);}
        ItemStack loot(){var list=game.lbc().rollTable(f.level,f.at,p,"lbc:ported/deus");require(list.size()==1,"one original source leaf");var s=list.getFirst();require(s.is(LbcPursuitItems.DEUS)&&s.getMaxDamage()==25,"native Deus source identity/durability25");for(var component:List.of(DataComponents.CUSTOM_DATA,DataComponents.ITEM_MODEL,DataComponents.CUSTOM_NAME,DataComponents.LORE))require(Objects.equals(s.get(component),LbcPursuitItems.DEUS.getDefaultInstance().get(component)),"source/default presentation "+component);return s;}
        ItemStack held(InteractionHand hand,boolean raw){p.getInventory().clearContent();var source=loot();var stack=source;if(raw){stack=new ItemStack(Items.CARROT_ON_A_STICK);stack.set(DataComponents.CUSTOM_DATA,source.get(DataComponents.CUSTOM_DATA));stack.set(DataComponents.ITEM_MODEL,source.get(DataComponents.ITEM_MODEL));stack.set(DataComponents.CUSTOM_NAME,source.get(DataComponents.CUSTOM_NAME));stack.set(DataComponents.LORE,source.get(DataComponents.LORE));stack.set(DataComponents.MAX_DAMAGE,25);}p.setItemInHand(hand,stack);return stack;}
        boolean input(InteractionHand hand,boolean raw){client.acknowledge();return raw?net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,f.level,hand).consumesAction():p.gameMode.useItem(p,f.level,p.getItemInHand(hand),hand).consumesAction();}
        void launch(){service.reset(p);p.setYRot(0);p.setXRot(0);held(InteractionHand.OFF_HAND,true);require(input(InteractionHand.OFF_HAND,true)&&clock()==260,"scheduled raw offhand launch starts260");p.getInventory().clearContent();pose=p.position();yaw=p.getYRot();pitch=p.getXRot();packets.clear();packetBoundary=0;lastClock=260;frames=damages=0;}
        void boundaryInputs(){
            for(var mode:List.of(GameType.SURVIVAL,GameType.CREATIVE))for(var hand:InteractionHand.values())for(boolean raw:new boolean[]{false,true}){
                service.reset(p);p.setGameMode(mode);var s=held(hand,raw);var other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;var untouched=loot();p.setItemInHand(other,untouched);game.items().mana().set(p,-500);int stat=p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(s.getItem()));
                require(input(hand,raw)&&clock()==260,"actual native/raw hand activates "+mode+" "+hand+" raw="+raw);require(s.getCount()==1&&s.getDamageValue()==0&&untouched.getCount()==1&&untouched.getDamageValue()==0&&game.items().mana().get(p)==-500,"no mana/item/durability debit at immediate boundary");require(!input(hand,raw)&&clock()==260,"busy use cannot restart260");require(p.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(s.getItem()))>=stat,"input stat remains well formed");
            }
            p.setGameMode(GameType.SURVIVAL);service.reset(p);p.getInventory().clearContent();var wrong=new ItemStack(Items.CARROT_ON_A_STICK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("magicwand",1);wrong.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));p.setItemInHand(InteractionHand.MAIN_HAND,wrong);input(InteractionHand.MAIN_HAND,true);require(clock()==0,"unrelated magicwand never activates Deus");
            held(InteractionHand.MAIN_HAND,false);var tech=field("stopper");require(game.items().magic().mechanismSuppressed(p)&&!input(InteractionHand.MAIN_HAND,false)&&clock()==0,"real TECH blocks admission");remove(tech);
            artifact(true);require(game.items().magic().artifactActive(beta)&&!input(InteractionHand.MAIN_HAND,false)&&clock()==0,"real strong MAG artifact blocks admission");artifact(false);
            var mag=field("stopper_magic");require(game.items().magic().beaconSuppressed(p.position())&&input(InteractionHand.MAIN_HAND,false)&&clock()==260,"ordinary MAG beacon allows Deus admission");remove(mag);service.reset(p);
            var miss=cow(f.at.add(2,0,3),1);var from=f.at.add(0,.7,0);var to=f.at.add(4,.7,4);require(new net.minecraft.world.phys.AABB(from,to).inflate(.001).intersects(miss.getBoundingBox())&&!LbcBeamGeometry.intersects(miss.getBoundingBox(),from,to),"actual cow near miss inside broad segment AABB requires exact native hitbox clip");clearCows();
        }
        Marker field(String tag){var m=new Marker(EntityTypes.MARKER,f.level);m.setPos(p.position());m.entityTags().add(tag);fieldBlocks.put(m,game.maps().snapshot(f.level,m.blockPosition()));require(f.level.addFreshEntity(m),"real field publication");fields.add(m);game.items().magic().loaded(m);return m;}
        void remove(Marker m){game.items().magic().unloaded(m);m.discard();fields.remove(m);
            // A real scheduler pulse migrates legacy TECH markers into persistent beacon blocks.
            var old=fieldBlocks.remove(m);if(old!=null&&f.level.getBlockState(m.blockPosition()).is(LbcMagic.MECHANISM_BEACON_BLOCK))game.maps().restoreSnapshot(f.level,m.blockPosition(),old);
            game.items().magic().blockChanged(m.blockPosition(),f.level.getBlockState(m.blockPosition()));
        }
        void artifact(boolean on){beta.getInventory().setItem(9,on?new ItemStack(LbcMagic.MYSTICAL_ARTIFACT):ItemStack.EMPTY);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();}
        Cow cow(Vec3 at,double resistance){var c=new Cow(EntityTypes.COW,f.level);c.setNoAi(true);c.setNoGravity(true);c.noPhysics=true;c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(resistance);c.setHealth(1000);c.setPos(at);require(f.level.addFreshEntity(c),"actual cow publication");cows.add(c);homes.put(c,at);return c;}
        void clearCows(){cows.forEach(Entity::discard);cows.clear();homes.clear();}
        void pin(){homes.forEach((c,at)->{c.setPos(at);c.setDeltaMovement(Vec3.ZERO);c.setInvulnerableTime(0);c.damageCooldownTime=0;});}
        void next(int n){stage=n;when=game.server().getTickCount();lastClock=clock();}
        void unchanged(String why){require(clock()==lastClock,why+" clock="+clock()+" expected="+lastClock);}
        void pose(){require(p.position().distanceToSqr(pose)<1e-8&&p.getYRot()==yaw&&p.getXRot()==pitch,"whip leaves owner position and camera unchanged");require(packets.stream().noneMatch(packet->packet instanceof ClientboundPlayerPositionPacket||packet instanceof ClientboundSetCameraPacket),"whip sends no owner teleport/camera packets");}
        void geometry(){
            var path=service.snapshotLastPath(p);var hits=service.snapshotLastHits(p);require(path.size()>2&&path.size()<=161,"real sampled curve with bounded work");
            var expected=new net.minecraft.commands.arguments.coordinates.LocalCoordinates(-.3,-.2,.5).apply(p.getEyePosition(),new net.minecraft.world.phys.Vec2(p.getXRot(),p.getYRot()));
            require(path.getFirst().distanceToSqr(expected)<1e-18,"native main-hand muzzle matches approved laser-minigun offset for actual camera pose");
            var previous=p.getLookAngle().normalize();double travel=0;
            for(int i=1;i<path.size();i++){
                var delta=path.get(i).subtract(path.get(i-1));double length=delta.length();require(Double.isFinite(length)&&length<=.200001,"exact collision subsegments remain <=.2 including clamped links");if(length<1e-8)continue;
                var direction=delta.scale(1/length);double turn=Math.acos(Math.clamp(previous.dot(direction),-1,1));require(turn<=Math.toRadians(8)+1e-6,"at most20degrees per half-block, including first segment and target-chain joins; angle="+Math.toDegrees(turn));previous=direction;travel+=length;
            }
            require(travel<=28.000001,"actual arc length respects source reach28");
            var visual=packets.subList(packetBoundary,packets.size()).stream().filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast).filter(packet->packet.particle().getType()==net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK).toList();
            require(!visual.isEmpty()&&visual.size()<=60,"fixed exact-point packet budget per actual active frame");boolean muzzle=false;
            for(var packet:visual){require(packet.count()==1&&packet.xDist()==0&&packet.yDist()==0&&packet.zDist()==0&&packet.xMaxSpeed()==0&&packet.yMaxSpeed()==0&&packet.zMaxSpeed()==0,"native sparks have zero positional and velocity spread");var at=new Vec3(packet.x(),packet.y(),packet.z());require(path.stream().anyMatch(v->v.distanceToSqr(at)<1e-18),"each actual native spark is on the actual collision path");muzzle|=at.distanceToSqr(expected)<1e-18;}
            require(muzzle,"actual native first spark starts at the muzzle");packetBoundary=packets.size();
            for(var id:hits){var c=cows.stream().filter(body->body.getUUID().equals(id)).findFirst().orElseThrow();boolean contact=false;for(int n=1;n<path.size();n++)if(c.getBoundingBox().inflate(1e-9).contains(path.get(n-1))||c.getBoundingBox().inflate(1e-9).clip(path.get(n-1),path.get(n)).isPresent()){contact=true;break;}require(contact,"reported body requires actual segment/hitbox contact, not broadphase AABB "+id);}
        }
        void cameraProbe(){launch();p.setYRot(ANGLES[poseIndex][0]);p.setXRot(ANGLES[poseIndex][1]);yaw=p.getYRot();pitch=p.getXRot();}
        void damageFrame(){var accepted=service.snapshotLastAccepted(p);boolean pulse=(frames-1)%10==0;require(accepted.size()==(pulse?3:0),"native2HP only each10 active ticks, frame="+frames+" accepted="+accepted);if(pulse)damages++;for(var c:cows){near(c.getHealth(),1000-(accepted.contains(c.getUUID())?2:0),"native exact2HP accepted damage");c.setHealth(1000);}}
        boolean step(){client.acknowledge();audience.acknowledge();int elapsed=game.server().getTickCount()-when;require(game.server().getTickCount()-started<650,"bounded scheduled sequence stage="+stage);
            if(stage==-1){var center=BlockPos.containing(f.at);if(client.updates()<=ownerUpdates||!f.level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(center.getX()>>4,center.getZ()>>4)))return false;boundaryInputs();cow(f.at.add(0,0,4),1);cow(f.at.add(1,0,7),1);cow(f.at.add(-1,0,10),1);cow(f.at.add(2,0,13),1);launch();next(0);return false;}
            if(stage==0){
                require(clock()==lastClock-1,"one decrement per scheduled active frame");frames++;var hits=service.snapshotLastHits(p);require(hits.size()==3&&new HashSet<>(hits).size()==3&&hits.stream().allMatch(id->cows.stream().anyMatch(c->c.getUUID().equals(id))),"exactly three unique actual reached cow bodies: "+hits+" end="+service.snapshotLastPath(p).getLast()+" bodies="+homes);
                var path=service.snapshotLastPath(p);require(path.size()>2&&path.stream().anyMatch(v->Math.abs(v.x-p.getEyePosition().x)>.01),"electrowhip has lateral curvature under straight aim");
                if(stablePath==null)stablePath=List.copyOf(path);else require(stablePath.equals(path),"stationary endpoints produce the same curve on successive ticks, without phase jitter");
                require(path.size()<=161,"fixed per-caster curve sample budget");
                for(int i=1;i<path.size();i++)require(path.get(i).distanceTo(path.get(i-1))<=.200001,"bounded exact collision segments");
                geometry();damageFrame();pose();pin();lastClock=clock();
                if(frames<3)return false;field("stopper");next(1);return false;
            }
            if(stage==1){unchanged("TECH pauses entire active clock");if(elapsed<3)return false;for(var m:List.copyOf(fields))remove(m);artifact(true);next(2);return false;}
            if(stage==2){unchanged("strong artifact pauses entire active clock");if(elapsed<3)return false;artifact(false);p.entityTags().add("time_stopped");game.items().timeStop().begin(f.level,100);next(3);return false;}
            if(stage==3){unchanged("TimeStop pauses entire active clock");if(elapsed<3)return false;game.items().timeStop().end(f.level);p.entityTags().remove("time_stopped");game.server().tickRateManager().setFrozen(true);next(4);return false;}
            if(stage==4){unchanged("global freeze pauses entire active clock");if(elapsed<3)return false;game.server().tickRateManager().setFrozen(false);packets.clear();packetBoundary=0;field("stopper_magic");next(5);return false;}
            if(stage==5){require(clock()==lastClock-1,"ordinary MAG beacon advances active/cooldown clock actual="+clock()+" previous="+lastClock+" frozen="+game.server().tickRateManager().isFrozen()+" normal="+game.server().tickRateManager().runsNormally()+" inputStopped="+game.items().wands().inputTimeStopped(p)+" tech="+game.items().magic().mechanismSuppressed(p)+" artifact="+game.items().magic().artifactSuppressedAt(p.level(),p.position()));frames++;geometry();damageFrame();pose();pin();lastClock=clock();if(clock()>=200)return false;require(clock()==199&&frames==61&&damages==7,"source61 active frames and seven exact10tick damage pulses end at199");for(var m:List.copyOf(fields))remove(m);field("stopper");next(6);return false;}
            if(stage==6){unchanged("TECH pauses cooldown199");if(elapsed<3)return false;for(var m:List.copyOf(fields))remove(m);artifact(true);next(7);return false;}
            if(stage==7){unchanged("artifact pauses cooldown199");if(elapsed<3)return false;artifact(false);clearCows();var checkpoint=new MatchPlayerCheckpoint(game,p);var scoreboard=new MatchScoreboardCheckpoint(game.server());game.items().mana().score(p,"deus_cd").set(4);game.server().tickRateManager().setFrozen(true);try{scoreboard.restore(game.server());require(checkpoint.restoreFrozen()==p,"canonical scoreboard/player checkpoint restore");}finally{game.server().tickRateManager().setFrozen(false);}client.acknowledge();require(clock()==199,"shared scoreboard checkpoint restores canonical Deus score without private runtime image");next(8);return false;}
            if(stage==8){require(clock()==Math.max(0,199-elapsed),"real scheduled199 cooldown expiry");if(clock()>0)return false;pullTarget=cow(f.at.add(0,0,4),0);launch();next(9);return false;}
            if(stage==9){var tangent=service.snapshotLastTangents(p).get(pullTarget.getUUID());require(tangent!=null,"actual reached target has incoming curve tangent");var motion=pullTarget.getDeltaMovement();ordinaryPull=motion.dot(tangent.scale(-1));require(ordinaryPull>0&&motion.normalize().dot(tangent.scale(-1))>.98,"target pulled opposite final curve tangent");pin();pullTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(-.5);service.reset(p);launch();next(10);return false;}
            if(stage==10){var tangent=service.snapshotLastTangents(p).get(pullTarget.getUUID());require(tangent!=null,"negative resistance actual target reached");double amplified=pullTarget.getDeltaMovement().dot(tangent.scale(-1));require(amplified>ordinaryPull*1.4,"signed negative knockback resistance amplifies pull: normal="+ordinaryPull+" negative="+amplified);pose();clearCows();position(beta,f.at.add(14,0,18));cow(f.at.add(19,0,0),1);cow(f.at.add(0,0,20),1);launch();next(11);return false;}
            if(stage==11){require(service.snapshotLastHits(p).equals(List.of(cows.getFirst().getUUID())),"smooth curve reaches the first sideways target before considering the next link");near(cows.getFirst().getHealth(),998,"first real segment contact deals2");near(cows.get(1).getHealth(),1000,"remaining path budget cannot jump to a distant second target");var path=service.snapshotLastPath(p);require(path.size()<=161,"sideways target preserves finite sample budget");pose();service.reset(p);require(clock()==0,"death/reset clears native source clock");clearCows();position(beta,f.at.add(60,0,0));poseIndex=0;cameraProbe();next(12);return false;}
            if(stage==12){geometry();pose();var path=service.snapshotLastPath(p);var chord=path.getLast().subtract(path.getFirst());double deviation=0;for(var at:path)deviation=Math.max(deviation,at.subtract(path.getFirst()).cross(chord).length()/chord.length());require(deviation>1,"long empty ray has a visible lateral bow, not a straight ray; deviation="+deviation+" pose="+poseIndex);if(++poseIndex<ANGLES.length){cameraProbe();next(12);return false;}clearCows();cow(f.at.add(0,0,-4),1);launch();next(13);return false;}
            if(stage==13){geometry();pose();require(service.snapshotLastHits(p).contains(cows.getFirst().getUUID()),"bounded spline still reaches a target behind its initial aim");service.reset(p);clearCows();for(int i=0;i<4;i++)cow(f.at.add(0,0,4),1);launch();next(14);return false;}
            if(stage==14){geometry();pose();require(service.snapshotLastHits(p).size()==3&&new HashSet<>(service.snapshotLastHits(p)).size()==3,"overlapping bodies cannot consume turns without traveled distance or exceed three unique contacts");near(cows.getLast().getHealth(),1000,"fourth overlapping target remains untouched");service.reset(p);clearCows();return true;}
            throw new IllegalStateException("Deus stage "+stage);
        }
        @Override public void close(){if(closed)return;closed=true;game.server().tickRateManager().setFrozen(frozen);try{game.items().timeStop().end(f.level);p.entityTags().remove("time_stopped");artifact(false);for(var m:List.copyOf(fields))remove(m);clearCows();service.reset(p);client.close();audience.close();f.close();}finally{saved.restore(game);saved.restoreAttributes();saved.restoreUse();betaSaved.restore(game);betaSaved.restoreAttributes();betaSaved.restoreUse();stats.forEach((item,n)->p.getStats().setValue(p,net.minecraft.stats.Stats.ITEM_USED.get(item),n));game.items().magic().inventoryChanged(p);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();}}
    }
    public static void start(GameController game){require(running==null,"exclusive finite fixture");var s=new State(game);try{running=s;SkyWars2.LOGGER.info("[check] START deus-whip");}catch(Throwable e){s.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var s=running;try{if(!s.step())return;s.close();running=null;SkyWars2.LOGGER.info("[check] PASS deus-whip (native/raw actual hands/source leaf/no debit; real scheduled curve/three unique bodies/native2HP/tangent pull/signed resistance; TECH/artifact/TimeStop/freeze whole-clock pauses, MAG beacon allows; canonical checkpoint/199 cooldown/no held/reset; no owner pose packets)");}catch(Throwable e){try{s.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL deus-whip",e);}}
}
