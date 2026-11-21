package me.skywars2.game.match.events;

import java.util.*;

import me.skywars2.game.GameController;
import me.skywars2.game.GameController.Stage;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.items.wands.LbcStonekivi;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.skills.clone.LbcCloneSkills;
import me.skywars2.game.world.maps.NativeChunkLoad;
import me.skywars2.game.world.maps.NativeChunkLoad.Residency;
import me.skywars2.game.world.maps.NativeChunkLoad.Leases.Area;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** One guided parent and eleven children, with each rocket's full blast applied at impact. */
public final class LbcOreshnik {
    private static final String TAG="skywars2.oreshnik";
    private static final int APPROACH_TICKS=100;
    private static final int PARENT_BLAST_REACH=25, CHILD_BLAST_REACH=14;
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final TagKey<net.minecraft.world.level.block.Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("airs"));
    private static final String MODEL="{item_display:'fixed',teleport_duration:1,interpolation_duration:1,brightness:{block:15,sky:15},item:{id:'minecraft:carrot_on_a_stick',count:1,components:{'minecraft:item_model':'lbc:oreshnik'}},transformation:{translation:[0f,4.5f,0f],scale:[%sf,%sf,%sf],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}";
    private final GameController game;
    private Set<Flight> flights=new LinkedHashSet<>();private Set<Blasts> blasts=new LinkedHashSet<>();
    private final NativeChunkLoad.Leases leases=new NativeChunkLoad.Leases(TICKET);
    private boolean restoring;private int callbacks;
    public LbcOreshnik(GameController game){this.game=game;}
    public static void registerTicket(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:oreshnik"),TICKET);}
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}
    public ServerPlayer owner(Entity entity){for(var f:flights)if(f.display==entity)return game.server().getPlayerList().getPlayer(f.owner);return null;}
    public LbcIndirectKills.Owner cause(Entity entity){for(var f:flights)if(f.display==entity)return f.cause;return null;}
    /** The native actor clone adapter supplies its already-copied display; no new start/split/random draw. */
    public boolean copyActor(Entity original,Entity copy){
        if(!(copy instanceof Display.ItemDisplay display))return false;
        for(var f:List.copyOf(flights))if(f.display==original){
            var next=new Flight((ServerLevel)display.level(),f.owner,f.cause,f.epoch,display.position(),f.yaw,f.pitch,
                    f.age,f.child,f.translatedTarget(display.position()),f.splitAge);
            next.display=display;flights.add(next);return true;
        }
        return false;
    }
    public LbcCloneSkills.ActorImage cloneImage(Entity source){
        for(var f:flights)if(f.display==source){
            var owner=f.owner;var cause=f.cause;var epoch=f.epoch;boolean child=f.child;int age=f.age;
            var origin=f.at;var target=f.target;int splitAge=f.splitAge;float yaw=f.yaw,pitch=f.pitch;
            return new LbcCloneSkills.ActorImage(){
                public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                    if(!(clone instanceof Display.ItemDisplay display))throw new IllegalStateException("Oreshnik clone display type");
                    var next=new Flight((ServerLevel)clone.level(),owner,cause,epoch,clone.position(),yaw,pitch,age,child,
                            target==null?null:target.add(clone.position().subtract(origin)),splitAge);
                    next.display=display;flights.add(next);
                }
                public void rollback(Entity clone){for(var f:List.copyOf(flights))if(f.display==clone)f.close();}
            };
        }
        return null;
    }
    public int flightCount(){return flights.size();}public int blastCount(){return blasts.stream().mapToInt(b->b.offsets.size()-b.cursor).sum();}
    private boolean live(String epoch){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch);}
    public boolean launch(ServerPlayer author,Vec3 point){
        if(restoring||!live(game.items().epoch())||game.items().magic().mechanismSuppressed(author)||game.items().magic().mechanismSuppressedAt(author.level(),point))return false;
        float yaw=author.getRandom().nextFloat()*360-180;
        float pitch=10+author.getRandom().nextFloat()*50;
        var direction=Vec3.directionFromRotation(pitch,yaw).normalize();
        var origin=point.subtract(direction.scale(APPROACH_TICKS));
        // The old cluster opened 60 blocks above the target. A shallow approach
        // starts below that altitude, so it opens as soon as it enters flight.
        int splitAge=Math.clamp((int)Math.ceil(APPROACH_TICKS-60/-direction.y),1,APPROACH_TICKS-1);
        flights.add(new Flight(author.level(),author.getUUID(),game.items().indirectKills().wildCause(author),
                game.items().epoch(),origin,yaw,pitch,0,false,point,splitAge));
        return true;
    }
    private void addChild(ServerLevel level,UUID owner,LbcIndirectKills.Owner cause,String epoch,Vec3 point,float yaw,float pitch){
        flights.add(new Flight(level,owner,cause,epoch,point,yaw,pitch,41,true,null,0));
    }
    public void tick(){
        if(restoring)return;callbacks++;
        try{
            if(!flights.isEmpty())game.items().cloneSkills().beforeOwnedStep(flights.stream().map(f->f.display).filter(Objects::nonNull).toList());
            for(var flight:List.copyOf(flights))flight.tick();
        }finally{callbacks--;}
    }
    public static List<Vec3> offsets(boolean child){
        var result=new ArrayList<Vec3>();result.add(Vec3.ZERO);
        for(int radius:new int[]{1,3})for(var axis:List.of(new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),new Vec3(-1,0,0),new Vec3(0,-1,0),new Vec3(0,0,-1)))result.add(axis.scale(radius));
        if(!child)result.addAll(List.of(new Vec3(4,4,0),new Vec3(4,0,4),new Vec3(0,4,4),new Vec3(-4,-4,0),new Vec3(-4,0,-4),new Vec3(0,-4,-4)));
        return List.copyOf(result);
    }
    static Vec3 exhaustOffset(float yaw,float pitch,boolean child){
        float scale=child?5:8;
        // Authored nozzle center (8,0,16): item centering, ItemDisplay's Y half-turn,
        // display scale/translation, then FIXED billboard orientation, in renderer order.
        var offset=new org.joml.Vector3f(0,4.5f-scale*.5f,-scale*.5f)
                .rotate(new org.joml.Quaternionf().rotationYXZ((float)(-Math.PI/180)*yaw,(float)(Math.PI/180)*pitch,0));
        return new Vec3(offset.x,offset.y,offset.z);
    }
    private final class Flight {
        final ServerLevel level;final UUID owner;final LbcIndirectKills.Owner cause;final String epoch;final boolean child;final float yaw,pitch;
        final Area tickets,impactTickets;final Vec3 exhaust,direction,target;final int splitAge;
        Vec3 at;int age;Display.ItemDisplay display;boolean closed;
        Flight(ServerLevel level,UUID owner,LbcIndirectKills.Owner cause,String epoch,Vec3 at,float yaw,float pitch,int age,boolean child,Vec3 target,int splitAge){
            this.level=level;this.owner=owner;this.cause=cause;this.epoch=epoch;this.at=at;this.yaw=yaw;this.pitch=pitch;this.age=age;this.child=child;tickets=leases.area(level);exhaust=exhaustOffset(yaw,pitch,child);
            this.direction=Vec3.directionFromRotation(pitch,yaw).normalize();this.target=target;this.splitAge=splitAge;
            impactTickets=child?null:leases.area(level);
        }
        Vec3 translatedTarget(Vec3 copy){return target==null?null:target.add(copy.subtract(at));}
        void model(){
            if(display!=null&&!display.isRemoved())return;
            display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);int scale=child?5:8;
            try{display.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),TagParser.parseCompoundFully(MODEL.formatted(scale,scale,scale))));}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
            display.setPos(at);display.setYRot(yaw);display.setXRot(pitch);display.setNoGravity(true);display.entityTags().addAll(List.of(TAG,"oreshnik","mecha","rare","slowraycast",epoch));
            if(child)display.entityTags().add("oreshnik_splitted");
            game.items().markCurrentItemEpoch(display);game.items().timeStop().prepareFrozen(display);
            if(!level.addFreshEntity(display))throw new IllegalStateException("Oreshnik display publication");
        }
        void tick(){
            if(closed)return;if(!live(epoch)||display!=null&&display.isRemoved()){close();return;}
            if(game.items().timeStop().stopped(level))return;
            if(game.items().magic().mechanismSuppressedAt(level,at)){close();return;}
            boolean arrives=!child&&age+1>=APPROACH_TICKS;
            var next=arrives?target:at.add(direction);
            if(impactTickets!=null&&impactTickets.empty()){
                if(impactTickets.resident(target,PARENT_BLAST_REACH)==Residency.FAILED){close();return;}
                // Keep the full approach resident while the display crosses chunk boundaries.
                for(int i=0;i<=APPROACH_TICKS;i++){
                    var cell=BlockPos.containing(at.lerp(target,i/(double)APPROACH_TICKS));
                    impactTickets.hold(new ChunkPos(cell.getX()>>4,cell.getZ()>>4));
                }
            }
            var ready=tickets.resident(next,0);
            if(ready==Residency.FAILED){close();return;}
            if(ready!=Residency.READY)return;
            boolean birth=display==null;model();
            if(birth)game.items().cloneSkills().beforeOwnedStep(List.of(display));
            age++;
            var smoke=at.add(exhaust);
            for(var viewer:game.server().getPlayerList().getPlayers())if(viewer.level()==level)level.sendParticles(viewer,ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,true,false,smoke.x,smoke.y,smoke.z,5,.3,.3,.3,.05);
            LbcOverload.sound(level,at,"minecraft:lbcsounds.rocket_power",net.minecraft.sounds.SoundSource.MASTER,1,child?1.5f:1,0,child?.08f:.5f);
            at=next;display.setPos(at);
            if(!child&&age==splitAge)for(int n=0;n<11;n++){
                float childYaw=(level.getRandom().nextInt(360001)-180000)*.001f,childPitch=(level.getRandom().nextInt(30001)+60000)*.001f;
                addChild(level,owner,cause,epoch,at,childYaw,childPitch);
            }
            if(arrives||child&&!level.getBlockState(BlockPos.containing(at)).is(AIRS)){
                var work=new Blasts(level,owner,cause,epoch,at,child);
                blasts.add(work);
                if(!work.step())game.items().stonekivi().queuedWork(work);
                close();
            }else if(age>=180)close();
        }
        void releaseTickets(){tickets.close();if(impactTickets!=null)impactTickets.close();}
        void close(){closed=true;releaseTickets();if(display!=null)display.discard();flights.remove(this);}
    }
    private final class Blasts implements LbcStonekivi.Work {
        final ServerLevel level;final UUID owner;final LbcIndirectKills.Owner cause;final String epoch;final Vec3 at;final boolean child;final List<Vec3> offsets;final Area tickets;
        int cursor;boolean closed;
        Blasts(ServerLevel level,UUID owner,LbcIndirectKills.Owner cause,String epoch,Vec3 at,boolean child){this.level=level;this.owner=owner;this.cause=cause;this.epoch=epoch;this.at=at;this.child=child;offsets=offsets(child);tickets=leases.area(level);}
        @Override public boolean ready(){return closed||!live(epoch)||!game.items().timeStop().stopped(level);}
        @Override public boolean snapshotIdle(){return true;}
        @Override public boolean step(){
            callbacks++;try{
                if(closed||!live(epoch)){cancel();return true;}
                var ready=tickets.resident(at,child?CHILD_BLAST_REACH:PARENT_BLAST_REACH);
                if(ready==Residency.FAILED){cancel();return true;}
                if(ready!=Residency.READY)return false;
                float power=child?5:10;
                var direct=new Marker(EntityTypes.MARKER,level);direct.setPos(at);direct.entityTags().addAll(List.of(TAG,epoch));direct.setCustomName(Component.translatable("oreshnik"));
                var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.EXPLOSION),direct,game.server().getPlayerList().getPlayer(owner));
                var captured=game.items().indirectKills().captureDamage(damage,cause);
                try {
                    game.items().indirectKills().trapChanges(level,cause,()->{
                        while(cursor<offsets.size()){
                            var point=at.add(offsets.get(cursor++));
                            direct.setPos(point);
                            level.explode(direct,captured,null,point.x,point.y,point.z,power,false,Level.ExplosionInteraction.MOB);
                        }
                    });
                }
                finally{direct.discard();}
                cancel();return true;
            }finally{callbacks--;}
        }
        @Override public void cancel(){closed=true;tickets.close();blasts.remove(this);}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            int savedCursor=cursor;boolean savedClosed=closed;
            return links->()->{tickets.close();cursor=savedCursor;closed=savedClosed;};
        }
    }
    public void stop(){for(var flight:List.copyOf(flights))flight.close();for(var work:List.copyOf(blasts))work.cancel();}
    void boundary(){if(callbacks!=0)throw new IllegalStateException("Finish Oreshnik native quantum before capture");}
    void prepareFrozen(){boundary();restoring=true;for(var flight:flights)flight.releaseTickets();for(var work:blasts)work.tickets.close();}
    void publishFrozen(){boundary();restoring=false;}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){boundary();return new Checkpoint(graph);}
    final class Checkpoint {
        record Image(Flight original,Vec3 at,int age,boolean closed,MatchDamageSources.Ref display){}
        final List<Image> image;final Set<Blasts> accepted;
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            image=flights.stream().map(f->new Image(f,f.at,f.age,f.closed,f.display==null?null:graph.links.key(f.display))).toList();
            accepted=new LinkedHashSet<>(blasts);accepted.forEach(graph::capture);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var displays=image.stream().map(i->i.display==null?null:(Display.ItemDisplay)links.get(i.display)).toList();
            return ()->{
                for(var flight:flights)flight.releaseTickets();for(var work:blasts)work.tickets.close();
                flights=new LinkedHashSet<>();for(int n=0;n<image.size();n++){var i=image.get(n);var f=i.original;f.at=i.at;f.age=i.age;f.closed=i.closed;f.display=displays.get(n);flights.add(f);}
                blasts=new LinkedHashSet<>(accepted);
            };
        }
    }
}
