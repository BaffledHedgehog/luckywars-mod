package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Native IMBA entry, singleton movie clock and resumable terrain projection. */
public final class LbcBadApple {
    private static final String ACTOR="skywars2.badapple",DISPLAY="skywars2.badapple_display";
    private static final String SOUND="minecraft:lbcsounds.bad_apple";
    private static final Identifier MOVIE=Identifier.parse("lbc:badapple/movie");
    /** Admission and writes remain with the canonical mode/protection/terrain owners. */
    public interface Policy {
        boolean admittedLaunch(ServerPlayer author,ServerLevel level,Vec3 at);
        boolean admittedCell(ServerPlayer author,ServerLevel level,BlockPos at);
        /** False means no mutation was admitted. Accepted work still advances past protected cells. */
        boolean write(ServerPlayer author,ServerLevel level,BlockPos at,BlockState state,LbcIndirectKills.Owner cause);
        /** Restore accepted movie writes, including block-entity metadata, through the terrain owner. */
        boolean restore(ServerPlayer author,ServerLevel level,BlockPos at,MapManager.BlockSnapshot original,LbcIndirectKills.Owner cause);
    }
    public record View(boolean running,int logicalTicks,int desiredFrame,int acceptedFrame,int columnCursor,
                       int appliedFrame,boolean soundStarted,long writes,long coalesced) { }
    private final GameController game;
    private final Policy policy;
    private final LbcBadAppleMedia media;
    private LinkedHashMap<UUID,Actor> actors=new LinkedHashMap<>();
    private LinkedHashSet<Playback> playbacks=new LinkedHashSet<>();
    private Playback current;
    private final LinkedHashMap<UUID,Visual> visuals=new LinkedHashMap<>();
    private int callbacks,lastTick=Integer.MIN_VALUE;
    private boolean restoring;
    private static final class Visual{
        final ServerLevel level;int target,elapsed=10;boolean paused;Identifier control;
        Visual(ServerLevel level){this.level=level;}
        Visual(Visual source){level=source.level;target=source.target;elapsed=source.elapsed;paused=source.paused;}
    }

    public LbcBadApple(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);media=LbcBadAppleMedia.instance();}
    public static boolean recognized(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"bad_apple_imba",0)==1;}
    public static boolean managed(Entity entity){return entity!=null&&(entity.entityTags().contains(ACTOR)||entity.entityTags().contains(DISPLAY));}
    private ServerPlayer player(LivingEntity entity){return entity instanceof ServerPlayer p?p:null;}
    public boolean ready(ItemEntity token){
        if(restoring||token.isRemoved()||!recognized(token.getItem())||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||!(token.level() instanceof ServerLevel level)||!LbcDimensionItems.ordinary(level)
                ||level!=game.maps().level()&&!game.items().spellSources().persistent(token)
                ||game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.")&&!game.items().currentEpoch(tag))return false;
        return policy.admittedLaunch(player(game.items().specialDrops().owner(token)),level,token.position());
    }
    /** Claims the source whole dropped stack. The dispatcher must not debit or discard it. */
    public boolean spawn(ItemEntity token){
        if(actors.containsKey(token.getUUID()))return true;
        callbacks++;try{
            if(!ready(token))return false;
            var author=game.items().specialDrops().owner(token);var cause=game.items().specialDrops().cause(token);
            var actor=new Actor(token,author,cause,game.items().epoch(),0,false);
            actors.put(token.getUUID(),actor);token.entityTags().add(ACTOR);token.entityTags().add("working");
            game.items().markCurrentItemEpoch(token);token.setPickUpDelay(32000);
            game.items().timeStop().prepareFrozen(token);
            sound((ServerLevel)token.level(),token.position(),"minecraft:lbcsounds.energy_2",1,1);
            ((ServerLevel)token.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,token.getX(),token.getY(),token.getZ(),20,.3,.3,.3,1);
            return true;
        }finally{callbacks--;}
    }
    private final class Actor {
        Entity body;LivingEntity author;final LbcIndirectKills.Owner cause;final String epoch;
        int age;boolean display;
        Actor(Entity body,LivingEntity author,LbcIndirectKills.Owner cause,String epoch,int age,boolean display){
            this.body=body;this.author=author;this.cause=cause;this.epoch=epoch;this.age=age;this.display=display;
        }
    }
    public LivingEntity owner(Entity entity){var actor=actors.get(entity.getUUID());return actor==null?null:actor.author;}
    public int count(){return actors.size();}
    List<Entity> snapshot(){return actors.values().stream().map(a->a.body).toList();}
    public View view(){var p=current;return p==null?new View(false,0,0,-1,0,-1,false,0,0):
            new View(!p.closed&&!p.retiring,p.logical,p.nextDesired,p.accepted,p.cursor,p.appliedIndex,p.soundStarted,p.writes,p.coalesced);}
    public void tick(){
        if(restoring)return;callbacks++;try{
            int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
            for(var actor:List.copyOf(actors.values())){
                var body=actor.body;
                if(!game.playing()||!game.items().currentEpoch(actor.epoch)){remove(actor,true);continue;}
                if(body.isRemoved()){
                    if(body.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)continue;
                    remove(actor,false);continue;
                }
                var level=(ServerLevel)body.level();
                if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(body))continue;
                if(actor.display){
                    if(start(player(actor.author),level,body.position(),actor.cause)){
                        sound(level,body.position(),"minecraft:lbcsounds.energy_1",.1f,2);
                        level.sendParticles(ParticleTypes.SQUID_INK,body.getX(),body.getY(),body.getZ(),100,1,1,1,1);
                        remove(actor,true);
                    }
                    continue;
                }
                actor.age++;
                if(actor.age==10){body.setNoGravity(true);body.setGlowingTag(true);body.setDeltaMovement(0,.1,0);}
                level.sendParticles(ParticleTypes.FIREWORK,body.getX(),body.getY(),body.getZ(),3,0,0,0,2);
                if(actor.age>=60)display(actor);
            }
            var p=current;
            if(p!=null&&!p.closed&&!p.retiring&&p.predecessorDone()&&!game.items().timeStop().stopped(p.level)){
                if(!p.baselined)p.baseline();
                else{
                    p.logical++;
                    p.nextDesired=Math.min(media.frameCount()-1,p.logical/2);
                    // The full finite clip is retained (219.1s); the old 219s schedule cut its tail.
                    if(p.logical>=media.frameCount()*2){p.retiring=true;p.finalFrame=true;}
                }
            }
            tickVisuals();
        }finally{callbacks--;}
    }
    private void display(Actor actor){
        var old=actor.body;var level=(ServerLevel)old.level();var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);
        try{var tag=TagParser.parseCompoundFully("{item_display:'fixed',interpolation_duration:1,teleport_duration:1,item:{id:'minecraft:acacia_boat',count:1,components:{'minecraft:item_model':'lbc:badapple'}}}");
            display.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
        display.setPos(old.position());display.setNoGravity(true);display.entityTags().add(DISPLAY);
        var nearest=level.getNearestPlayer(old,Double.MAX_VALUE);
        if(nearest!=null){var direction=nearest.getEyePosition().subtract(old.position());display.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));display.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));}
        game.items().spellSources().inherit(old,display);game.items().markCurrentItemEpoch(display);game.items().timeStop().prepareFrozen(display);
        // ENTITY_LOAD is synchronous: publish the private association before insertion.
        actors.remove(old.getUUID());actor.body=display;actor.display=true;actors.put(display.getUUID(),actor);
        if(!level.addFreshEntity(display)){
            actors.remove(display.getUUID());actor.body=old;actor.display=false;actors.put(old.getUUID(),actor);display.discard();return;
        }
        old.discard();
        sound(level,display.position(),"minecraft:lbcsounds.energy_3",1,1);
    }
    private void remove(Actor actor,boolean discard){actors.remove(actor.body.getUUID());if(discard&&!actor.body.isRemoved())actor.body.discard();}
    public void loaded(Entity entity){
        if(entity instanceof ServerPlayer p&&!restoring&&!MatchPlayerCheckpoint.restoring(p))clearVisual(p);
        if(restoring||MatchActorCheckpoint.restoring(entity)||!managed(entity))return;
        var actor=actors.get(entity.getUUID());
        if(actor==null||!game.items().currentEpoch(actor.epoch)){entity.discard();return;}
        actor.body=entity;game.items().timeStop().prepareFrozen(entity);
    }
    public void unloaded(Entity entity){/* Keep the admitted scalar state; load only rebinds this UUID. */}
    public void removed(Entity entity){if(restoring||MatchActorCheckpoint.restoring(entity))return;if(entity instanceof ServerPlayer p)clearVisual(p);var actor=actors.get(entity.getUUID());if(actor!=null&&entity.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK)remove(actor,false);}
    /** A replacement waits for the prior accepted frame, so two movies never race paint. */
    public boolean start(ServerPlayer author,ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause){
        if(restoring||!game.playing()||!policy.admittedLaunch(author,level,at))return false;
        callbacks++;try{
            Playback previous=current;
            if(previous!=null){previous.retiring=true;previous.finalFrame=false;}
            stopSound();
            var playback=new Playback(author,level,at,cause,previous);current=playback;playbacks.add(playback);
            game.items().stonekivi().queuedWork(playback);return true;
        }finally{callbacks--;}
    }
    public void requestStop(){callbacks++;try{if(current!=null){current.retiring=true;current.finalFrame=false;}stopSound();}finally{callbacks--;}}
    /** Only epoch teardown/whole-coordinator detachment cancels accepted work. Ordinary stop drains. */
    public void stop(){callbacks++;try{clearVisuals();for(var actor:List.copyOf(actors.values()))remove(actor,true);for(var p:List.copyOf(playbacks))p.cancel();playbacks.clear();current=null;lastTick=Integer.MIN_VALUE;restoring=false;stopSound();}finally{callbacks--;}}
    private int visualTarget(ServerPlayer player){
        var p=current;
        if(p==null||!p.baselined||p.retiring||p.closed||!p.predecessorDone()||player.level()!=p.level||!player.onGround())return 0;
        var support=BlockPos.containing(player.getX(),player.getY()-.01,player.getZ());var painted=p.painted.get(support.asLong());
        if(painted==null||!p.level.getBlockState(support).equals(painted))return 0;
        return painted.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK))?2:1;
    }
    void tickVisuals(){
        var online=new HashSet<UUID>();
        for(var player:game.server().getPlayerList().getPlayers()){
            var id=player.getUUID();online.add(id);var visual=visuals.get(id);
            if(!game.playing()||game.stage()==GameController.Stage.ENDING||!player.isAlive()||player.isRemoved()||game.isObserver(player)||!game.canUseGameplayItems(player)
                    ||visual!=null&&visual.level!=player.level()){clearVisual(player);continue;}
            int target=visualTarget(player);if(visual==null){if(target==0)continue;visual=new Visual(player.level());visuals.put(id,visual);}
            boolean paused=game.items().timeStop().stopped(player.level())||game.items().timeStop().frozen(player);
            if(!paused){if(visual.target!=target){visual.target=target;visual.elapsed=0;}else if(visual.elapsed<10)visual.elapsed++;}
            visual.paused=paused;
            if(visual.target==0&&visual.elapsed>=10){clearVisual(player);continue;}
            synchronizeVisual(player,visual);
        }
        visuals.keySet().retainAll(online);
    }
    private void synchronizeVisual(ServerPlayer player,Visual visual){
        SignedEffectVisuals.movieNightVision(player,visual.target!=0);
        var control=Identifier.parse("lbc:badapple/control_"+visual.target+(visual.paused?"_paused":""));
        if(control.equals(visual.control)&&player.getPostEffects().contains(MOVIE))return;
        for(var effect:List.copyOf(player.getPostEffects()))if(visualEffect(effect))player.removePostEffect(effect);
        player.addPostEffect(control);player.addPostEffect(MOVIE);visual.control=control;
    }
    private static boolean visualEffect(Identifier effect){return effect.getNamespace().equals("lbc")&&effect.getPath().startsWith("badapple/");}
    private void clearVisual(ServerPlayer player){
        visuals.remove(player.getUUID());SignedEffectVisuals.movieNightVision(player,false);
        for(var effect:List.copyOf(player.getPostEffects()))if(visualEffect(effect))player.removePostEffect(effect);
    }
    private void clearVisuals(){for(var player:game.server().getPlayerList().getPlayers())clearVisual(player);visuals.clear();}
    private void sound(ServerLevel level,Vec3 at,String id,float volume,float pitch){LbcOverload.sound(level,at,id,volume,pitch,0,0);}
    private void playSound(){
        var sound=net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse(SOUND)));
        for(var player:game.server().getPlayerList().getPlayers()){
            var at=LbcFpvDrone.viewpoint(player);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound,net.minecraft.sounds.SoundSource.MASTER,at.x,at.y,at.z,100000,1,0));
        }
    }
    private void stopSound(){var packet=new ClientboundStopSoundPacket(Identifier.parse(SOUND),null);for(var player:game.server().getPlayerList().getPlayers())player.connection.send(packet);}

    private final class Playback implements LbcStonekivi.Work {
        ServerPlayer author;final ServerLevel level;final String epoch;final LbcIndirectKills.Owner cause;
        final int minX,minZ;final Playback predecessor;
        byte[] applied,desired;
        final LinkedHashMap<Long,MapManager.BlockSnapshot> originals=new LinkedHashMap<>();
        final LinkedHashMap<Long,BlockState> painted=new LinkedHashMap<>();
        final ArrayList<Long> restoreOrder=new ArrayList<>();
        int restoreCursor;
        int logical,nextDesired,desiredIndex,accepted=-1,cursor,appliedIndex=-1;
        boolean baselined,soundStarted,retiring,finalFrame,closed;
        long writes,coalesced;
        Playback(ServerPlayer author,ServerLevel level,Vec3 at,LbcIndirectKills.Owner cause,Playback predecessor){
            this.author=author;this.level=level;this.cause=cause;this.predecessor=predecessor;epoch=game.items().epoch();
            int x=BlockPos.containing(at).getX(),z=BlockPos.containing(at).getZ();
            if(x>=-192&&x<=192&&z>=-192&&z<=192){x=0;z=0;}
            minX=x-media.width()/2;minZ=z-media.height()/2;
            applied=new byte[media.frameBytes()];desired=new byte[media.frameBytes()];
        }
        boolean predecessorDone(){return predecessor==null||predecessor.closed;}
        void baseline(){
            // Actual Paper semantics: frame zero establishes a baseline, without initial terrain fill.
            media.decodeFrame(0,applied);System.arraycopy(applied,0,desired,0,applied.length);
            baselined=true;appliedIndex=desiredIndex=0;
            if(!soundStarted){soundStarted=true;playSound();}
        }
        public boolean ready(){return !restoring&&(closed||!game.items().timeStop().stopped(level)&&predecessorDone()&&(retiring||baselined&&nextDesired>appliedIndex));}
        /** Every quantum has a complete typed adapter; a movie is safe to capture while partial. */
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(restoring)return false;callbacks++;game.items().stonekivi().beginNativeWork();
            try{
                if(closed)return true;
                if(!game.playing()||!game.items().currentEpoch(epoch)){cancel();return true;}
                if(!ready())return false;
                if(accepted<0){
                    if(retiring&&(!finalFrame||appliedIndex>=media.frameCount()-1))return restore();
                    int target=nextDesired;
                    if(target<=appliedIndex)return false;
                    // Pure CPU decoding is bounded too. Unaccepted targets may follow the latest clock.
                    int decoded=0;while(desiredIndex<target&&decoded++<8)media.applyDelta(++desiredIndex,desired);
                    if(desiredIndex<target)return false;
                    accepted=target;cursor=0;coalesced+=Math.max(0,target-appliedIndex-1);
                }
                long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
                int visits=0,columns=0;
                while(cursor<media.pixelCount()&&visits++<512&&columns<8){
                    if((visits&15)==0&&System.nanoTime()>=deadline)break;
                    int index=cursor;
                    boolean white=media.white(desired,index);
                    if(white==media.white(applied,index)){cursor++;continue;}
                    int x=minX+index%media.width(),z=minZ+index/media.width();
                    // Request native manager tickets, then yield without advancing accepted pixels.
                    if(level.getChunkSource().getChunkNow(x>>4,z>>4)==null){game.maps().prepareChunk(level,new ChunkPos(x>>4,z>>4));return false;}
                    paint(x,z,white);set(applied,index,white);cursor++;columns++;
                }
                if(cursor==media.pixelCount()){
                    appliedIndex=accepted;accepted=-1;cursor=0;
                    if(retiring&&(!finalFrame||appliedIndex==media.frameCount()-1))return restore();
                }
                return false;
            }finally{game.items().stonekivi().endNativeWork();callbacks--;}
        }
        private void paint(int x,int z,boolean white){
            int top=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;
            if(top<level.getMinY())return;
            var state=Blocks.CONCRETE.pick(white?net.minecraft.world.item.DyeColor.WHITE:net.minecraft.world.item.DyeColor.BLACK).defaultBlockState();
            // No cached height/floors: every admitted changed column observes canonical terrain writes.
            // At most 64 lower cells and four surfaces are touched within this atomic column quantum.
            write(new BlockPos(x,top,z),state);
            int found=0,min=Math.max(level.getMinY(),top-64);
            boolean aboveSolid=level.getBlockState(new BlockPos(x,top-1,z)).isSolid();
            for(int y=top-2;y>=min&&found<3;y--){
                var pos=new BlockPos(x,y,z);boolean solid=level.getBlockState(pos).isSolid();
                if(solid&&!aboveSolid){write(pos,state);found++;}
                aboveSolid=solid;
            }
        }
        private void write(BlockPos pos,BlockState state){
            if(!policy.admittedCell(author,level,pos)||level.getBlockState(pos).equals(state))return;
            long key=pos.asLong();var before=originals.containsKey(key)?null:game.maps().snapshot(level,pos);
            if(!policy.write(author,level,pos,state,cause))return;
            if(before!=null){originals.put(key,before);restoreOrder.add(key);}
            painted.put(key,state);writes++;
        }
        private boolean restore(){
            long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);int visits=0;
            while(restoreCursor<restoreOrder.size()&&visits++<32){
                if((visits&7)==0&&System.nanoTime()>=deadline)return false;
                long key=restoreOrder.get(restoreCursor);var pos=BlockPos.of(key);
                if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null){game.maps().prepareChunk(level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;}
                if(!policy.restore(author,level,pos,copy(originals.get(key)),cause))return false;
                painted.remove(key);restoreCursor++;
            }
            if(restoreCursor<restoreOrder.size())return false;
            finish();return true;
        }
        private void finish(){closed=true;originals.clear();painted.clear();restoreOrder.clear();playbacks.remove(this);if(current==this){current=null;stopSound();}}
        public void cancel(){closed=true;playbacks.remove(this);if(current==this)current=null;}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var owner=graph.links.key(author);
            byte[] savedApplied=applied.clone(),savedDesired=desired.clone();
            int savedLogical=logical,savedNext=nextDesired,savedDesiredIndex=desiredIndex,savedAccepted=accepted,savedCursor=cursor,savedAppliedIndex=appliedIndex;
            boolean savedBaseline=baselined,savedSound=soundStarted,savedRetiring=retiring,savedFinal=finalFrame,savedClosed=closed;
            long savedWrites=writes,savedCoalesced=coalesced;
            var savedOriginals=new LinkedHashMap<Long,MapManager.BlockSnapshot>();originals.forEach((key,value)->savedOriginals.put(key,copy(value)));
            var savedPainted=new LinkedHashMap<>(painted);var savedOrder=List.copyOf(restoreOrder);int savedRestore=restoreCursor;
            return links->{checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);var rebound=(ServerPlayer)links.get(owner);
                return ()->{author=rebound;applied=savedApplied.clone();desired=savedDesired.clone();logical=savedLogical;nextDesired=savedNext;desiredIndex=savedDesiredIndex;
                    accepted=savedAccepted;cursor=savedCursor;appliedIndex=savedAppliedIndex;baselined=savedBaseline;soundStarted=savedSound;retiring=savedRetiring;finalFrame=savedFinal;closed=savedClosed;writes=savedWrites;coalesced=savedCoalesced;
                    originals.clear();savedOriginals.forEach((key,value)->originals.put(key,copy(value)));painted.clear();painted.putAll(savedPainted);restoreOrder.clear();restoreOrder.addAll(savedOrder);restoreCursor=savedRestore;};};
        }
    }
    private static MapManager.BlockSnapshot copy(MapManager.BlockSnapshot snapshot){return new MapManager.BlockSnapshot(snapshot.state(),snapshot.entity()==null?null:snapshot.entity().copy());}
    private static void set(byte[] bits,int index,boolean value){int mask=1<<(index&7);if(value)bits[index>>>3]|=(byte)mask;else bits[index>>>3]&=(byte)~mask;}

    void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Bad Apple native quantum before capture/replay");}
    void prepareFrozen(){checkpointBoundary();restoring=true;clearVisuals();stopSound();actors=new LinkedHashMap<>();playbacks=new LinkedHashSet<>();current=null;}
    void publishFrozen(){
        checkpointBoundary();boolean publish=restoring;restoring=false;lastTick=game.server().getTickCount();
        // Vanilla has no seek packet. A restored active track starts at zero once; the saved movie
        // clock/cursor stays exact. Capturing alone never stops or replays presentation.
        if(publish&&current!=null&&!current.closed&&!current.retiring&&current.soundStarted)playSound();
        if(publish)for(var player:game.server().getPlayerList().getPlayers()){var visual=visuals.get(player.getUUID());if(visual!=null)synchronizeVisual(player,visual);}
    }
    void cancelFrozen(){checkpointBoundary();restoring=false;}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    final class Checkpoint {
        private record ActorImage(MatchDamageSources.Ref body,MatchDamageSources.Ref author,LbcIndirectKills.Owner cause,String epoch,int age,boolean display){}
        private final List<ActorImage> savedActors=new ArrayList<>();
        private final LinkedHashSet<Playback> savedPlaybacks=new LinkedHashSet<>(playbacks);
        private final Playback savedCurrent=current;
        private final LinkedHashMap<UUID,Visual> savedVisuals=new LinkedHashMap<>();
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            visuals.forEach((id,value)->savedVisuals.put(id,new Visual(value)));
            for(var actor:actors.values())savedActors.add(new ActorImage(graph.links.key(actor.body),graph.links.key(actor.author),actor.cause,actor.epoch,actor.age,actor.display));
            savedPlaybacks.forEach(graph::capture);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var next=new LinkedHashMap<UUID,Actor>();
            for(var image:savedActors){var body=links.get(image.body);var author=(LivingEntity)links.get(image.author);next.put(body.getUUID(),new Actor(body,author,image.cause,image.epoch,image.age,image.display));}
            return ()->{actors=next;playbacks=new LinkedHashSet<>(savedPlaybacks);current=savedCurrent;visuals.clear();savedVisuals.forEach((id,value)->visuals.put(id,new Visual(value)));lastTick=clock.last(game);};
        }
    }
    boolean cloneRecognizes(Entity entity){return actors.containsKey(entity.getUUID());}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var original=actors.get(source.getUUID());if(original==null)return null;
        int age=original.age;boolean display=original.display;var cause=original.cause;var author=original.author;var epoch=original.epoch;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity ignored,Entity clone,Map<Entity,Entity> family){
                var linked=family.get(author);var currentAuthor=linked instanceof LivingEntity living?living:author;
                actors.put(clone.getUUID(),new Actor(clone,currentAuthor,cause,epoch,age,display));
            }
            public void rollback(Entity clone){var actor=actors.get(clone.getUUID());if(actor!=null)remove(actor,true);}
        };
    }
}
