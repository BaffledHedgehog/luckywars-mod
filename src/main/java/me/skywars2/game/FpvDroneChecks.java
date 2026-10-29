package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Native packet listener, actual tracker/tickets and real actor death; no physical client claim. */
public final class FpvDroneChecks {
    private FpvDroneChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("FPV drone: "+why);}
    private static Object read(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static ItemStack controller(){var s=new ItemStack(Items.CARROT_ON_A_STICK,2);var d=new CompoundTag();d.putInt("drone_pult",1);d.putInt("raycastspell",1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));return s;}
    private static double angle(Vec3 a,Vec3 b){return Math.toDegrees(Math.acos(net.minecraft.util.Mth.clamp(a.normalize().dot(b.normalize()),-1,1)));}
    private static void flatten(Packet<?> p,List<Packet<?>> out){out.add(p);if(p instanceof ClientboundBundlePacket b)b.subPackets().forEach(x->flatten(x,out));}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer owner,beta;
        final ShishEncounterChecks.State saved,otherSaved;final SauvojenMeditationChecks.Attribution attribution;
        final Set<Long> forced=new HashSet<>();final Vec3 origin;final boolean offhand;ShishEncounterChecks.Client client;
        LbcFpvDrone.Drone drone;Vec3 priorDirection,priorPosition;int phase,at,born;boolean stopped,closed;long explosions;
        Fixture(GameController game){this.game=game;level=game.maps().level();owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));saved=new ShishEncounterChecks.State(game,owner);otherSaved=new ShishEncounterChecks.State(game,beta);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta));offhand=game.config().offhandWands;
            origin=new Vec3(0,Math.min(300,level.getMaxY()-24),0);at=game.server().getTickCount();
        }
        @SuppressWarnings("unchecked") List<Packet<?>> packets(){var out=new ArrayList<Packet<?>>();for(var p:(List<Packet<?>>)read(read(client,"wire"),"packets"))flatten(p,out);return out;}
        void init(){require(game.items().fpvDrone().count()==0&&game.items().fpvDrone().ticketCount()==0,"exclusive empty service");game.config().offhandWands=false;
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){long key=ChunkPos.pack(x,z);if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);}
            for(var p:List.of(owner,beta)){p.stopUsingItem();p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setSprinting(false);p.setShiftKeyDown(false);p.setOnGround(true);
                p.entityTags().removeAll(Set.of("spectator","time_stopped","time_stopped_lol","strict_map_object","rcdenyalways","magic_academy","sin_greed_active","nomagic_active"));
                game.players().get(p.getUUID()).lbcSkill=0;game.players().get(p.getUUID()).lbcTrinket=0;Arrays.fill(game.players().get(p.getUUID()).trinkets,0);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.ARMOR).setBaseValue(0);p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);p.setHealth(1000);}
            owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+35,origin.y,origin.z,Set.of(),0,0,true);client=new ShishEncounterChecks.Client(game,owner);
            // Native power8 blast footprint must be all air so this test never mutates map terrain.
            var launch=origin.add(0,7,-48);for(var pos:BlockPos.betweenClosed(BlockPos.containing(launch.add(-20,-20,-20)),BlockPos.containing(launch.add(20,20,40))))require(level.getBlockState(pos).isAir(),"resident isolated blast air "+pos);
            launch();
        }
        void launch(){owner.setItemInHand(InteractionHand.MAIN_HAND,controller());client.ack();var s=owner.getMainHandItem();
            require(owner.gameMode.useItem(owner,level,s,InteractionHand.MAIN_HAND).consumesAction()&&s.getCount()==1,"actual target-free pult input and one debit");
            require(game.items().fpvDrone().count()==1&&game.items().fpvDrone().controlling(owner)&&!owner.isPassenger()&&owner.getCamera()==owner,"real body remains independent of packet-only camera/seat");
            drone=game.items().fpvDrone().snapshot().getFirst();require(drone.getHealth()==20&&drone.getMaxHealth()==20&&!drone.isPushable()&&!drone.canCollideWith(owner),"native shootable20HP nonpushing actor");
            require(drone.position().distanceToSqr(origin.add(0,7,-48))<1e-8,"authored fixed-point spawn further/higher");
            priorDirection=drone.getLookAngle();born=at=game.server().getTickCount();
        }
        boolean tick(){client.ack();int now=game.server().getTickCount();require(now-at<40,"bounded native phase "+phase);
            require(owner.position().distanceToSqr(origin)<1e-6,"controller body never moves into drone");
            if(phase==0){if(now-born<3)return false;
                if(packets().stream().noneMatch(p->p instanceof ClientboundSetCameraPacket c&&c.getEntity(level)==drone))return false;
                var seen=packets();int spawn=-1,camera=-1;
                for(int n=0;n<seen.size();n++){var p=seen.get(n);if(p instanceof ClientboundAddEntityPacket add&&add.getId()==drone.getId())spawn=n;if(p instanceof ClientboundSetCameraPacket c&&c.getEntity(level)==drone)camera=n;}
                require(spawn>=0&&camera>spawn,"actual native pairing precedes camera attachment");
                require(level.sendParticles(owner,net.minecraft.core.particles.ParticleTypes.END_ROD,false,false,drone.getX(),drone.getY(),drone.getZ(),1,0,0,0,0),"native particle range uses camera instead of distant body");
                long sounds=packets().stream().filter(ClientboundSoundPacket.class::isInstance).count();
                var sound=net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.parse("minecraft:block.note_block.pling"));
                level.playSound(null,drone.getX(),drone.getY(),drone.getZ(),sound,net.minecraft.sounds.SoundSource.MASTER,1,1);
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).count()==sounds+1,"native sound range follows remote camera");
                LbcOverload.sound(level,drone.position(),"minecraft:block.note_block.pling",1,1,5,0);
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).count()==sounds+2,"shared custom sound range follows remote camera");
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).anyMatch(s->s.getSound().value().location().toString().equals("minecraft:lbcsounds.drone_signal")&&s.getSource()==net.minecraft.sounds.SoundSource.PLAYERS&&new Vec3(s.getX(),s.getY(),s.getZ()).distanceToSqr(origin)<1),"launch sound remains at body before camera attachment");
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).anyMatch(s->s.getSound().value().location().toString().equals("minecraft:lbcsounds.drone_tick")&&s.getSource()==net.minecraft.sounds.SoundSource.HOSTILE),"authored flight hostile sound category");
                require(packets().stream().anyMatch(p->p instanceof ClientboundSetCameraPacket c&&c.getEntity(level)==drone),"native camera packet references published drone");
                require(packets().stream().anyMatch(p->p instanceof ClientboundSetPassengersPacket c&&c.getVehicle()==drone.getId()&&Arrays.equals(c.getPassengers(),new int[]{owner.getId()})),"only controller receives synthetic seat so vanilla client keeps sending look");
                require(owner.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view&&view.center().equals(drone.chunkPosition()),"actual native chunk stream centers on drone");require(game.items().fpvDrone().ticketCount()==1,"body has independently owned simulation ticket");
                client.listener.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(origin.add(40,20,20),90,-30,false,false));
                require(owner.position().equals(origin)&&owner.getYRot()==0,"movement/look packet cannot move or turn body");
                client.listener.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(true,false,false,false,true,false,true)));
                require(owner.getLastClientInput().equals(Input.EMPTY),"walking and jump input consumed separately");
                require(!owner.gameMode.useItem(owner,level,owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&owner.getMainHandItem().getCount()==1,"item use blocked while piloting");
                priorDirection=drone.getLookAngle();at=now;phase=1;return false;
            }
            if(phase==1){double turned=angle(priorDirection,drone.getLookAngle());require(turned>2.9&&turned<=3.001,"total angular step limited to three degrees: "+turned);priorDirection=drone.getLookAngle();
                if(now-at<5)return false;game.items().timeStop().begin(level,100);stopped=true;priorPosition=drone.position();priorDirection=drone.getLookAngle();at=now;phase=2;return false;}
            if(phase==2){require(drone.position().equals(priorPosition)&&angle(drone.getLookAngle(),priorDirection)<1e-5,"actual Time Stop pauses drone position and steering");if(now-at<3)return false;
                game.items().timeStop().end(level);stopped=false;client.ack();
                client.listener.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(false,false,false,false,false,true,false)));
                require(!game.items().fpvDrone().controlling(owner)&&game.items().fpvDrone().ticketCount()==0&&!drone.isRemoved(),"Shift releases camera/body ticket and preserves flying actor");
                require(packets().stream().anyMatch(p->p instanceof ClientboundSetCameraPacket c&&c.getEntity(level)==owner),"return camera packet");client.ack();
                priorDirection=drone.getLookAngle();priorPosition=drone.position();client.listener.handleMovePlayer(new ServerboundMovePlayerPacket.Rot(-90,45,true,false));at=now;phase=3;return false;}
            if(phase==3){require(angle(drone.getLookAngle(),priorDirection)<1e-5,"released drone ignores subsequent player look");if(now-at<3)return false;require(drone.position().distanceToSqr(priorPosition)>4,"released drone keeps flying straight");
                explosions=game.items().fpvDrone().explosions();require(drone.hurtServer(level,drone.damageSources().playerAttack(beta),20),"native20 damage accepted against actual actor");
                require(drone.isRemoved()&&game.items().fpvDrone().count()==0&&game.items().fpvDrone().explosions()==explosions+1,"shot-down actor explodes once without helper creeper");
                launch();at=now;phase=4;return false;}
            if(phase==4){if(now-at<3)return false;game.items().fpvDrone().forget(owner);require(!game.items().fpvDrone().controlling(owner)&&game.items().fpvDrone().ticketCount()==0&&!drone.isRemoved(),"forget/disconnect boundary releases only control");
                game.items().fpvDrone().stop();require(game.items().fpvDrone().count()==0&&game.items().fpvDrone().ticketCount()==0,"exact owned stop cleanup");return true;}
            throw new IllegalStateException("phase");
        }
        public void close(){if(closed)return;closed=true;try{if(stopped)game.items().timeStop().end(level);game.items().fpvDrone().stop();game.config().offhandWands=offhand;
            saved.restore(game);otherSaved.restore(game);if(client!=null)client.ack();attribution.close();}
            finally{if(client!=null)client.close();for(long key:forced)level.setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false);}}
    }
    private static Fixture running;
    public static void start(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;throw e;}SkyWars2.LOGGER.info("[check] START fpv-drone");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS fpv-drone (actual target-free item input, camera/seat packets, native remote chunk view/body tickets, look/input interception, bounded steering, Time Stop, Shift straight flight, native20HP shootdown/power8 blast and owned lifecycle; no physical-client qualification)");}
        catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL fpv-drone",e);}}
}
