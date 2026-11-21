package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Native scheduled shared queue checks; never invokes a production tick or replaces its scheduler. */
public final class RayQueueFairnessChecks {
    private RayQueueFairnessChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Ray queue fairness: "+why);}
    private static Trial running;
    private static final class Probe implements LbcExasperato.ReplayableRay {
        final int id;final List<Integer> order;final boolean sampled;
        LbcExasperato.RayTicket ticket;int samples,trails,completions;HitResult hit;Vec3 sampleOrigin;
        Probe(int id,List<Integer> order,boolean sampled){this.id=id;this.order=order;this.sampled=sampled;}
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return !state.isAir();}
        public boolean target(Entity entity){return false;}
        public void trail(Vec3 from,Vec3 to){trails++;order.add(id);}
        public void complete(HitResult hit){require(++completions==1,"one terminal callback for request "+id);this.hit=hit;}
        public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;}
        public double sampleStep(){return sampled?.001:0;}
        public HitResult sampleAt(Vec3 point){
            if(sampleOrigin==null)sampleOrigin=point;
            require(point.distanceToSqr(sampleOrigin.add(0,samples*.001,0))<1e-16,"exact ordered source sample coordinate for request "+id);
            samples++;order.add(id);return null;
        }
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
            int savedSamples=samples,savedTrails=trails,savedCompletions=completions;var savedHit=hit;var savedOrigin=sampleOrigin;
            return ignored->()->{sampleOrigin=savedOrigin;samples=savedSamples;trails=savedTrails;completions=savedCompletions;hit=savedHit;};
        }
    }
    private static final class Trial implements AutoCloseable {
        final GameController game;final ServerPlayer alpha,beta;final LbcExasperato queue;
        final RegenerationBeltChecks.Snapshot alphaSaved,betaSaved;
        final RegenerationBeltChecks.ScheduledClient alphaClient,betaClient;
        final Vec3 origin;final BlockPos wall;final MapManager.BlockSnapshot wallSaved;
        final Set<Long> forced;final List<Integer> order=new ArrayList<>();final List<Probe> probes=new ArrayList<>();
        Probe near,frozen,cancelled,unloaded,latest;int phase,elapsed,sequence,oldBefore,checkpointOffset,missingX,missingZ;
        MatchSpellCheckpoint.Links links;LbcExasperato.SearchCheckpoint checkpoint;List<Integer> expected;
        Trial(GameController game){
            this.game=game;alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(alpha!=null&&beta!=null&&game.playing()&&game.canUseGameplayItems(alpha)&&game.canUseGameplayItems(beta),"two active native participants");
            queue=game.items().exasperato();require(queue.pendingSearches()==0,"exclusive empty shared ray queue");
            require(!game.items().wands().timeStopped()&&!game.items().timeStop().stopped(alpha.level()),"no ambient global freeze");
            require(!game.items().magic().suppressed(alpha)&&!game.items().magic().suppressed(beta),"native caster admission is open");
            require(!alpha.entityTags().contains("time_stopped")&&!beta.entityTags().contains("time_stopped"),"no ambient author freeze");
            origin=new Vec3(alpha.getX(),Math.min(240,alpha.level().getMaxY()-8)+.25,alpha.getZ());wall=BlockPos.containing(origin.add(0,3,0));
            require(alpha.level().hasChunkAt(wall)&&alpha.level().getBlockState(wall).isAir(),"resident empty native near-hit fixture");
            wallSaved=game.maps().snapshot(alpha.level(),wall);forced=new HashSet<>(alpha.level().getForceLoadedChunks());
            alphaSaved=new RegenerationBeltChecks.Snapshot(game,alpha);betaSaved=new RegenerationBeltChecks.Snapshot(game,beta);
            alphaClient=new RegenerationBeltChecks.ScheduledClient(game,alpha);
            try{betaClient=new RegenerationBeltChecks.ScheduledClient(game,beta);}catch(Throwable error){alphaClient.close();throw error;}
        }
        Probe samples(ServerPlayer owner,int advances){var p=new Probe(sequence++,order,true);probes.add(p);queue.replayableSamples(owner,origin,new Vec3(0,1,0),advances,p);return p;}
        Probe near(ServerPlayer owner){var p=new Probe(sequence++,order,false);probes.add(p);queue.replayableTool(owner,origin,new Vec3(0,1,0),8,true,p,false);return p;}
        void initialize(){
            alpha.level().setBlock(wall,Blocks.STONE.defaultBlockState(),818);
            for(var p:List.of(alpha,beta)){
                p.stopUsingItem();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
                var point=origin.add(p==alpha?2:-2,0,0);
                require(alpha.level().hasChunkAt(BlockPos.containing(point)),"resident author body fixture");
                p.teleportTo(alpha.level(),point.x,point.y,point.z,Set.of(),0,0,true);
            }
            alphaClient.acknowledge();betaClient.acknowledge();
            boolean found=false;missingZ=wall.getZ()>>4;
            for(int distance=64;distance<=4096;distance*=2){missingX=(wall.getX()>>4)+distance;if(alpha.level().getChunkSource().getChunkNow(missingX,missingZ)==null){found=true;break;}}
            require(found,"native absent column fixture found without loading");
            for(int i=0;i<192;i++)samples(alpha,64);
            latest=samples(alpha,64);near=near(beta);
        }
        int oldSamples(){int count=0;for(int i=0;i<192;i++)count+=probes.get(i).samples;return count;}
        boolean drained(){return probes.stream().allMatch(p->p.completions==1)&&queue.pendingSearches()==0;}
        void next(int phase){this.phase=phase;elapsed=0;}
        boolean step(){
            elapsed++;alphaClient.acknowledge();betaClient.acknowledge();
            require(elapsed<600,"bounded eventual completion on actual server ticks, phase="+phase);
            if(phase==0){
                if(near.completions==0){require(elapsed<=2,"second author first work within two real ticks despite 192 long rays");return false;}
                require(elapsed<=2,"second author near hit completes within two real ticks");
                require(near.hit!=null&&near.hit.getType()==HitResult.Type.BLOCK&&((net.minecraft.world.phys.BlockHitResult)near.hit).getBlockPos().equals(wall),"Beta receives exact native near wall hit");
                int first=order.indexOf(near.id);require(first>=0&&first<=1,"second author receives its quantum by the second admitted work item");
                require(latest.samples>0&&order.indexOf(latest.id)==0,"latest same-author ray starts before its 192 unstarted predecessors");
                oldBefore=oldSamples();next(1);
            }else if(phase==1){
                if(elapsed<=12){for(int i=0;i<8;i++)samples(alpha,0);return false;}
                require(oldSamples()>oldBefore,"accepted older rays progress during continuous fresh same-author arrivals");
                int oldest=order.indexOf(probes.getFirst().id);
                require(oldest>=0&&oldest<=5,"oldest unstarted ray receives first work within three author turns despite newest-first priority");
                require(latest.samples>1,"resumed lane progresses alongside newest and oldest first-work lanes");next(2);
            }else if(phase==2){
                if(!drained())return false;
                for(int i=0;i<192;i++)require(probes.get(i).samples==65,"every original sample cursor finishes without skipped or duplicated samples");
                alpha.entityTags().add("time_stopped");frozen=samples(alpha,8);near=near(beta);cancelled=samples(alpha,8);cancelled.ticket.cancel();cancelled.ticket.cancel();
                unloaded=new Probe(sequence++,order,false);probes.add(unloaded);
                queue.replayableTool(beta,new Vec3(missingX*16+8,origin.y,missingZ*16+8),new Vec3(0,1,0),8,true,unloaded,false);next(3);
            }else if(phase==3){
                require(frozen.samples==0&&frozen.completions==0,"author freeze preserves accepted cursor and terminal");
                require(cancelled.completions==1&&cancelled.samples==0&&cancelled.ticket.done(),"ticket cancellation is immediate and terminal exactly once");
                if(elapsed<3)return false;
                require(near.completions==1&&near.hit!=null,"frozen author cannot block another author's native near hit");
                require(unloaded.completions==1&&unloaded.hit==null&&unloaded.trails==0&&alpha.level().getChunkSource().getChunkNow(missingX,missingZ)==null,"absent chunk cancels before reads/trails and remains unloaded");
                alpha.entityTags().remove("time_stopped");next(4);
            }else if(phase==4){
                if(!drained())return false;require(frozen.samples==9,"frozen cursor resumes exact source samples");
                for(int i=0;i<4;i++){samples(alpha,128);samples(beta,128);}next(5);
            }else if(phase==5){
                // Capture mixed fresh/resumed lanes at an actual tick boundary, retaining original tickets.
                samples(alpha,128);samples(beta,128);
                links=new MatchSpellCheckpoint.Links(new MatchDamageSources(game,List.of(alpha,beta)));
                queue.searchBoundary();checkpoint=queue.checkpointSearches(links);checkpointOffset=order.size();next(6);
            }else if(phase==6){
                if(order.size()-checkpointOffset<128)return false;
                expected=List.copyOf(order.subList(checkpointOffset,checkpointOffset+128));
                require(links.resolve(id->game.server().getPlayerList().getPlayer(id)).isEmpty(),"canonical author links resolve");
                queue.searchBoundary();queue.prepareSearchCheckpoint();checkpoint.prepare(links).run();order.subList(checkpointOffset,order.size()).clear();next(7);
            }else if(phase==7){
                if(order.size()-checkpointOffset<128)return false;
                require(expected.equals(order.subList(checkpointOffset,checkpointOffset+128)),"checkpoint restores exact owner rotation, lane turn and per-ray cursor identity/order");next(8);
            }else if(phase==8){
                if(!drained())return false;
                require(probes.stream().allMatch(p->p.completions==1&&p.ticket.done()),"every accepted original ticket reaches one terminal after checkpoint replay");
                require(forced.equals(alpha.level().getForceLoadedChunks()),"shared ray work adds no forced chunk tickets");return true;
            }
            return false;
        }
        public void close(){
            Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->alpha.entityTags().remove("time_stopped"),
                    ()->{for(var p:probes)if(p.ticket!=null&&!p.ticket.done())p.ticket.cancel();},
                    ()->game.maps().restoreSnapshot(alpha.level(),wall,wallSaved),
                    alphaClient::close,betaClient::close,
                    ()->alphaSaved.restore(game),()->betaSaved.restore(game),
                    alphaSaved::restoreAttributes,betaSaved::restoreAttributes,alphaSaved::restoreUse,betaSaved::restoreUse)){
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            }
            if(failure!=null)throw new IllegalStateException("Ray queue native fixture restoration",failure);
        }
    }
    public static void run(GameController game){
        require(running==null,"exclusive native scheduled fixture");running=new Trial(game);
        try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS ray-queue-fairness (actual shared native ticks; 192 long Alpha rays, Beta exact near hit within two ticks, newest/oldest/resumed progress, one terminal, author freeze/cancellation, exact checkpoint owner/lane/cursor replay; no forced chunks)");}}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL ray-queue-fairness",error);}
    }
}
