package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Actual scheduled native hits and connections; no manual gameplay ticks or manufactured clocks. */
public final class PsychoSoundChecks {
    private PsychoSoundChecks() { }
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException("Psycho sound: "+reason);}
    @SuppressWarnings("unchecked") private static List<Packet<?>> packets(RegenerationBeltChecks.ScheduledClient client){
        try{Object connection=client.connection;var f=connection.getClass().getDeclaredField("packets");f.setAccessible(true);return (List<Packet<?>>)f.get(connection);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static List<ClientboundSoundEntityPacket> voices(RegenerationBeltChecks.ScheduledClient client){
        return packets(client).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                .filter(p->p.getSound().value().location().equals(LbcPsycho.BEAM_SOUND)).toList();
    }
    private static long stops(RegenerationBeltChecks.ScheduledClient client){
        return packets(client).stream().filter(ClientboundStopSoundPacket.class::isInstance).map(ClientboundStopSoundPacket.class::cast)
                .filter(p->LbcPsycho.BEAM_SOUND.equals(p.getName())&&p.getSource()==SoundSource.MASTER).count();
    }
    private static boolean empty(LbcPsycho service){
        try{var f=LbcPsycho.class.getDeclaredField("beamAudio");f.setAccessible(true);return ((Map<?,?>)f.get(service)).isEmpty();}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer source,target;final LbcPsycho service;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final SauvojenMeditationChecks.Attribution attribution;
        RegenerationBeltChecks.ScheduledClient client,audience;
        int started,phaseStarted,phase,initialUpdates,pausedRay,observedAt=-1,observedRay;
        int previousStarts,lastStartFrame,afterContactStarts;boolean ownsStop,closed,resetRay;
        int expectedRaySamples;
        BlockPos rayWall;
        BlockState savedRayWall;
        LbcPsycho.Charge trailCharge;
        Vec3 trailStart,trailEnd;
        Fixture(GameController game){
            this.game=game;source=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            target=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&source!=null&&target!=null&&game.canUseGameplayItems(source)&&game.canUseGameplayItems(target)
                    &&game.server().tickRateManager().runsNormally(),"two active native participants and running scheduler");
            service=game.items().psycho();
            require(service.count()==0&&service.effectCount()==0&&service.rayCount(source)==0&&service.rayCount(target)==0&&empty(service)
                    &&!game.items().timeStop().stopped(source.level()),"exclusive Psycho fixture");
            saved=new RegenerationBeltChecks.Snapshot(game,source);otherSaved=new RegenerationBeltChecks.Snapshot(game,target);
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(source,target));
        }
        void aim(boolean hit){
            if(!hit){source.setYRot(180);source.setXRot(0);return;}
            Vec3 d=target.position().add(0,.8,0).subtract(source.getEyePosition());
            source.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));
            source.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
        }
        void begin(){
            Vec3 at=Vec3.atBottomCenterOf(new BlockPos(source.blockPosition().getX(),280,source.blockPosition().getZ()));
            for(var p:BlockPos.betweenClosed(BlockPos.containing(at).offset(-5,-2,-2),BlockPos.containing(at).offset(5,4,12)))
                require(source.level().hasChunkAt(p)&&source.level().getBlockState(p).isAir(),"resident empty ray fixture");
            for(var p:List.of(source,target)){
                p.stopUsingItem();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);
                p.removeAllEffects();p.setNoGravity(true);p.noPhysics=true;p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
                p.setLastClientInput(new net.minecraft.world.entity.player.Input(false,false,false,false,false,false,false));
                for(String tag:List.of("time_stopped","sin_greed_active","nomagic_active","spectator","strict_map_object"))p.entityTags().remove(tag);
                // The later raw-clock replacement must not import an unrelated old affliction score.
                for(String status:List.of("scared","lookback","twitching","piss"))service.clearStatus(p,status);
                game.items().mana().noMagic(p,0);game.items().magic().inventoryChanged(p);
            }
            source.teleportTo(source.level(),at.x,at.y,at.z,Set.of(),0,0,true);
            target.teleportTo(target.level(),at.x,at.y,at.z+6,Set.of(),0,0,true);
            client=new RegenerationBeltChecks.ScheduledClient(game,source);audience=new RegenerationBeltChecks.ScheduledClient(game,target);
            client.acknowledge();audience.acknowledge();initialUpdates=client.updates();
            packets(client).clear();packets(audience).clear();aim(true);
            // End the packet-count probe behind the victim in an already resident
            // chunk. A 99-block ray can otherwise grow as the test chunks arrive.
            rayWall=BlockPos.containing(at.add(0,0,9.5));
            savedRayWall=source.level().getBlockState(rayWall);
            require(savedRayWall.isAir(),"empty native ray terminator");
            source.level().setBlock(rayWall,Blocks.STONE.defaultBlockState(),3);
            Vec3 eye=source.getEyePosition(),direction=source.getLookAngle();
            double length=(rayWall.getZ()-eye.z)/direction.z;
            for(double sample=0;sample<=length;sample+=.33)expectedRaySamples++;
            require(expectedRaySamples>=20,"resident trail extends beyond the struck player");
            // Existing grant API owns the same ray clock used by food and wand casts.
            service.addRayTicks(source,240);require(service.rayCount(source)==240,"one accepted real ray-clock grant");
            started=phaseStarted=game.server().getTickCount();
        }
        String diagnostics(){
            Vec3 offset=target.position().subtract(source.getEyePosition()),look=source.getLookAngle();
            double along=offset.dot(look),miss=offset.subtract(look.scale(along)).lengthSqr();
            return "phase="+phase+" elapsed="+(game.server().getTickCount()-started)+" phaseElapsed="+(game.server().getTickCount()-phaseStarted)
                    +" sourcePos="+source.position()+" targetPos="+target.position()+" sourceVelocity="+source.getDeltaMovement()+" targetVelocity="+target.getDeltaMovement()
                    +" yaw="+source.getYRot()+" pitch="+source.getXRot()+" along="+along+" missSquared="+miss
                    +" remaining="+service.rayCount(source)+" target[scared/lookback/piss]="+service.effectValue(target,"scared")+"/"+service.effectValue(target,"lookback")+"/"+service.effectValue(target,"piss")
                    +" source[scared/lookback/piss]="+service.effectValue(source,"scared")+"/"+service.effectValue(source,"lookback")+"/"+service.effectValue(source,"piss")
                    +" rawSource[scared/lookback/piss]="+game.items().mana().score(source,"scared").get()+"/"+game.items().mana().score(source,"lookback").get()+"/"+game.items().mana().score(source,"piss").get()
                    +" targetAlive="+target.isAlive()+" sourceSuppressed="+game.items().magic().suppressed(source)+" targetSuppressed="+game.items().magic().suppressed(target)
                    +" starts="+voices(audience).size()+" previousStarts="+previousStarts+" lastStartFrame="+lastStartFrame+" resetRay="+resetRay
                    +" sourceUpdates="+(client.updates()-initialUpdates)+" targetConnection="+audience.diagnostics();
        }
        void next(int value){phase=value;phaseStarted=game.server().getTickCount();}
        void audience(int count){
            require(voices(client).isEmpty()&&voices(audience).size()==count,"only impacted target receives voice, expected starts="+count);
            for(var voice:voices(audience))require(voice.getId()==target.getId()&&voice.getSource()==SoundSource.MASTER
                    &&voice.getVolume()==3&&voice.getPitch()==2,"self-bound moving voice retains loud exception and source pitch");
            for(var c:List.of(client,audience))require(packets(c).stream().noneMatch(p->p instanceof ClientboundSoundPacket s
                    &&s.getSound().value().location().toString().equals("minecraft:lbcsounds.scream")),"no legacy caster/area scream packet");
        }
        void rayTraffic(int rayTicks){
            int expectedPackets=game.balanceLegacy()?expectedRaySamples:(expectedRaySamples+2)/3;
            var trail=packets(client).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast)
                    .filter(packet->packet.particle().getType()==ParticleTypes.DUST_COLOR_TRANSITION).toList();
            int particles=trail.stream().mapToInt(ClientboundLevelParticlesPacket::count).sum();
            require(trail.size()==expectedPackets*rayTicks&&particles==expectedRaySamples*rayTicks
                            &&trail.stream().allMatch(packet->packet.overrideLimiter()&&packet.xMaxSpeed()==0
                                    &&packet.yMaxSpeed()==0&&packet.zMaxSpeed()==0),
                    "forced beam retains all source trail particles with zero speed: "+trail.size()+" packets, "
                            +particles+" particles for "+rayTicks+" ticks, expected "+expectedPackets+"/"+expectedRaySamples+" per tick");
            SkyWars2.LOGGER.info("[check] Psycho ray particles: {} packets per tick for {} particles",expectedPackets,expectedRaySamples);
        }
        void clearRayWall(){
            if(rayWall==null)return;
            source.level().setBlock(rayWall,savedRayWall,3);
            rayWall=null;
        }
        void startCharge(int mode) {
            service.stop();
            var origin=source.position().add(-3,3,0);
            var direction=new Vec3(.6,.12,.8).normalize();
            if(mode==0)require(service.emitDoppel(source,origin,direction),"native visible charge launches");
            else service.emitAt(source,mode,origin,direction);
            var shots=service.snapshot();
            require(shots.size()==(mode==4?3:1),"expected native charge formation");
            trailCharge=shots.getFirst();
            trailStart=trailCharge.position();
            trailEnd=trailStart.add(trailCharge.getDeltaMovement());
            packets(client).clear();
        }
        void chargeTraffic(int steps) {
            var particles=packets(client).stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                    .map(ClientboundLevelParticlesPacket.class::cast).filter(packet->
                        packet.particle()==ParticleTypes.SQUID_INK || packet.particle()==ParticleTypes.GLOW_SQUID_INK).toList();
            require(trailCharge.position().distanceToSqr(trailEnd)<1e-10,"charge advances its full native segment");
            if(steps==0) {
                require(particles.isEmpty(),"invisible charge omits both ink trails");
                return;
            }
            int perPacket=game.balanceLegacy()?1:3;
            for(var type:List.of(ParticleTypes.SQUID_INK,ParticleTypes.GLOW_SQUID_INK)) {
                var trail=particles.stream().filter(packet->packet.particle()==type).toList();
                require(trail.size()==(steps+perPacket)/perPacket,"reduced packets for "+type);
                int start=0;
                for(var packet:trail) {
                    int count=Math.min(perPacket,steps+1-start);
                    Vec3 center=trailStart.lerp(trailEnd,(start+(count-1)*.5)/steps);
                    require(packet.count()==count && packet.overrideLimiter()==(type==ParticleTypes.GLOW_SQUID_INK)
                            && packet.xMaxSpeed()==0 && packet.yMaxSpeed()==0 && packet.zMaxSpeed()==0
                            && new Vec3(packet.x(),packet.y(),packet.z()).distanceToSqr(center)<1e-10,
                            "ink retains type, particle count, range flag, group center and zero emission velocity");
                    start+=count;
                }
                require(start==steps+1,"entire section of both ink trails is emitted");
            }
            SkyWars2.LOGGER.info("[check] Psycho charge: {} ink trail packets for {} particles",particles.size(),2*(steps+1));
        }
        boolean step(){
            client.acknowledge();audience.acknowledge();int now=game.server().getTickCount(),elapsed=now-phaseStarted;
            require(now-started<140,"bounded actual scheduled beam fixture");if(elapsed<1)return false;
            switch(phase){
                case 0->{
                    int count=voices(audience).size();audience(count);
                    require(count>=1&&count<=3,"bounded 40-tick cadence, never per-hit packet spam");
                    if(count!=previousStarts){
                        require(count==previousStarts+1,"one target voice per renewal");
                        if(previousStarts>0)require(now-lastStartFrame>=39&&now-lastStartFrame<=41,"native renewals stay forty frames apart");
                        previousStarts=count;lastStartFrame=now;
                    }
                    int remaining=service.rayCount(source);
                    if(observedAt<0){
                        require(remaining<240&&remaining>=238,"real scheduler begins source ray clock");
                        rayTraffic(240-remaining);
                        clearRayWall();
                    }
                    else require(remaining==observedRay-(now-observedAt),"source clock decrements once per actual scheduler frame");
                    observedAt=now;observedRay=remaining;
                    require(service.effectValue(target,"scared")>0&&service.effectValue(target,"lookback")>0&&service.effectValue(target,"piss")>0,"real hit retains all existing target status counters");
                    // Scare's random teleport runs before the next ray, after this frame's aim was chosen.
                    // Verify the real hit first, then clear only that spatial side effect for a stable audio fixture.
                    service.clearStatus(target,"scared");
                    if(!resetRay&&elapsed>=25){
                        game.items().mana().score(source,"screamray").set(240);service.adoptRawFoodStatuses(source);
                        observedRay=240;resetRay=true;
                        require(voices(audience).size()==count,"replacing source remaining counter never restarts target audio cadence");
                    }
                    aim(true);if(elapsed<80)return false;
                    require(client.updates()-initialUpdates>=79&&count>=2&&resetRay&&stops(audience)==0,"four real seconds use only scheduled short-clip renewals despite counter reset");
                    afterContactStarts=count;
                    aim(false);next(1);
                }
                case 1->{if(elapsed<3)return false;audience(afterContactStarts);require(empty(service)&&stops(audience)==0,"aim-away loss retires renewals and leaves unrelated event instances alone");aim(true);next(2);}
                case 2->{audience(afterContactStarts+1);aim(true);pausedRay=service.rayCount(source);game.items().timeStop().begin(source.level(),40);ownsStop=true;next(3);}
                case 3->{
                    require(service.rayCount(source)==pausedRay,"actual Time Stop preserves ray clock");audience(afterContactStarts+1);
                    require(empty(service)&&stops(audience)==0,"always-on maintenance retires future renewals without event-wide stop");
                    if(elapsed<3)return false;game.items().timeStop().end(source.level());ownsStop=false;aim(true);next(4);
                }
                case 4->{audience(afterContactStarts+2);int remaining=service.rayCount(source);service.silenceBeamAudio();
                    require(empty(service)&&stops(audience)==0&&service.rayCount(source)==remaining,"rewind presentation cleanup preserves actual ray clock without broad stop");aim(true);next(5);}
                case 5->{audience(afterContactStarts+3);service.clearEffects(target);require(empty(service)&&stops(audience)==0,"target clear retires renewal lease");aim(true);next(6);}
                case 6->{audience(afterContactStarts+4);service.stop();require(empty(service)&&service.rayCount(source)==0&&stops(audience)==0,"ordinary service stop retires renewal lease");
                    for(var c:List.of(client,audience))require(packets(c).stream().filter(ClientboundStopSoundPacket.class::isInstance)
                            .map(ClientboundStopSoundPacket.class::cast).noneMatch(p->p.getName()==null||LbcPsycho.BEAM_SOUND.equals(p.getName())),"no broad or scream-wide stop affects unrelated uses");
                    startCharge(0);next(7);}
                case 7->{chargeTraffic(8);startCharge(3);next(8);}
                case 8->{chargeTraffic(4);require(service.count()==2,"spray still emits a child after moving");startCharge(4);next(9);}
                case 9->{chargeTraffic(0);return true;}
                default->throw new IllegalStateException("Psycho fixture phase "+phase);
            }return false;
        }
        public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{if(ownsStop){game.items().timeStop().end(source.level());ownsStop=false;}},
                    service::stop,this::clearRayWall,()->{if(client!=null)client.close();if(audience!=null)audience.close();},
                    ()->{saved.restore(game);otherSaved.restore(game);saved.restoreAttributes();otherSaved.restoreAttributes();saved.restoreUse();otherSaved.restoreUse();},
                    attribution::close,()->{game.items().magic().inventoryChanged(source);game.items().magic().inventoryChanged(target);}))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Psycho fixture restoration",failure);
        }
    }
    private static Fixture running;
    public static void start(GameController game){
        require(running==null,"exclusive scheduled Psycho check");var fixture=new Fixture(game);
        try{fixture.begin();running=fixture;SkyWars2.LOGGER.info("[check] START psycho-sound");}
        catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;
            SkyWars2.LOGGER.info("[check] PASS psycho-sound (actual scheduled ray/status/clients; target-only self-bound exempt loud scream; forty-frame renewal across eighty native frames despite source-counter replacement; aim-away loss; actual Time Stop maintenance/resume; rewind/clear/stop retire leases without broad stop; original RP clip only; no sample-perfect continuity claim)");
        }catch(Throwable error){
            try{SkyWars2.LOGGER.error("[check] Psycho sound diagnostics: {}",running.diagnostics());}catch(Throwable diagnostic){error.addSuppressed(diagnostic);}
            try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL psycho-sound",error);
        }
    }
}
