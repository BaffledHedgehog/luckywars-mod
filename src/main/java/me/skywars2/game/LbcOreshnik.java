package me.skywars2.game;

import java.util.*;
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

/** Source parent plus eleven children. Accepted native blasts drain separately through the shared fair queue. */
public final class LbcOreshnik {
    private static final String TAG="skywars2.oreshnik";
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final TagKey<net.minecraft.world.level.block.Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("airs"));
    private static final String MODEL="{item_display:'fixed',teleport_duration:1,interpolation_duration:1,brightness:{block:15,sky:15},item:{id:'minecraft:carrot_on_a_stick',count:1,components:{'minecraft:item_model':'lbc:oreshnik'}},transformation:{translation:[0f,4.5f,0f],scale:[%sf,%sf,%sf],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}";
    private final GameController game;
    private Set<Flight> flights=new LinkedHashSet<>();private Set<Blasts> blasts=new LinkedHashSet<>();
    private final Map<ServerLevel,Map<ChunkPos,Integer>> leases=new IdentityHashMap<>();
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
            var next=new Flight((ServerLevel)display.level(),f.owner,f.cause,f.epoch,display.position(),display.getYRot(),display.getXRot(),f.age,f.child);
            next.display=display;flights.add(next);return true;
        }
        return false;
    }
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        for(var f:flights)if(f.display==source){
            var owner=f.owner;var cause=f.cause;var epoch=f.epoch;boolean child=f.child;int age=f.age;
            return new LbcCloneSkills.ActorImage(){
                public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                    if(!(clone instanceof Display.ItemDisplay display))throw new IllegalStateException("Oreshnik clone display type");
                    var next=new Flight((ServerLevel)clone.level(),owner,cause,epoch,clone.position(),clone.getYRot(),clone.getXRot(),age,child);
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
        add(author.level(),author.getUUID(),game.items().indirectKills().wildCause(author),game.items().epoch(),point,0,90,0,false);return true;
    }
    private void add(ServerLevel level,UUID owner,LbcIndirectKills.Owner cause,String epoch,Vec3 point,float yaw,float pitch,int age,boolean child){
        var flight=new Flight(level,owner,cause,epoch,point,yaw,pitch,age,child);flights.add(flight);
    }
    public void tick(){
        if(restoring)return;callbacks++;
        try{for(var flight:List.copyOf(flights))flight.tick();}finally{callbacks--;}
    }
    static List<Vec3> offsets(boolean child){
        var result=new ArrayList<Vec3>();result.add(Vec3.ZERO);
        for(int radius:new int[]{1,3})for(var axis:List.of(new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),new Vec3(-1,0,0),new Vec3(0,-1,0),new Vec3(0,0,-1)))result.add(axis.scale(radius));
        if(!child)result.addAll(List.of(new Vec3(4,4,0),new Vec3(4,0,4),new Vec3(0,4,4),new Vec3(-4,-4,0),new Vec3(-4,0,-4),new Vec3(0,-4,-4)));
        return List.copyOf(result);
    }
    private final class Tickets {
        final ServerLevel level;final Set<ChunkPos> held=new LinkedHashSet<>();Tickets(ServerLevel level){this.level=level;}
        boolean resident(Vec3 point,int reach){
            var p=BlockPos.containing(point);boolean ready=true;var wanted=new LinkedHashSet<ChunkPos>();
            for(int x=(p.getX()-reach)>>4;x<=(p.getX()+reach)>>4;x++)for(int z=(p.getZ()-reach)>>4;z<=(p.getZ()+reach)>>4;z++){
                var chunk=new ChunkPos(x,z);wanted.add(chunk);
                if(held.add(chunk)){
                    var counts=leases.computeIfAbsent(level,ignored->new LinkedHashMap<>());int count=counts.getOrDefault(chunk,0);
                    counts.put(chunk,count+1);if(count==0)level.getChunkSource().addTicketAndLoadWithRadius(TICKET,chunk,0);
                }
                if(level.getChunkSource().getChunkNow(x,z)==null)ready=false;
            }
            for(var previous:List.copyOf(held))if(!wanted.contains(previous))release(previous);
            return ready;
        }
        void release(ChunkPos chunk){
            if(!held.remove(chunk))return;var counts=leases.get(level);int count=counts.getOrDefault(chunk,0);
            if(count<=1){counts.remove(chunk);level.getChunkSource().removeTicketWithRadius(TICKET,chunk,0);}else counts.put(chunk,count-1);
            if(counts.isEmpty())leases.remove(level);
        }
        void close(){for(var chunk:List.copyOf(held))release(chunk);}
    }
    private final class Flight {
        final ServerLevel level;final UUID owner;final LbcIndirectKills.Owner cause;final String epoch;final boolean child;final float yaw,pitch;final Tickets tickets;
        Vec3 at;int age;Display.ItemDisplay display;boolean closed;
        Flight(ServerLevel level,UUID owner,LbcIndirectKills.Owner cause,String epoch,Vec3 at,float yaw,float pitch,int age,boolean child){
            this.level=level;this.owner=owner;this.cause=cause;this.epoch=epoch;this.at=at;this.yaw=yaw;this.pitch=pitch;this.age=age;this.child=child;tickets=new Tickets(level);
        }
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
            var next=at.add(Vec3.directionFromRotation(pitch,yaw));if(!tickets.resident(next,0))return;
            model();age++;
            for(var viewer:game.server().getPlayerList().getPlayers())if(viewer.level()==level)level.sendParticles(viewer,ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,true,false,at.x,at.y,at.z,5,.3,.3,.3,.05);
            LbcOverload.sound(level,at,"minecraft:lbcsounds.rocket_power",net.minecraft.sounds.SoundSource.MASTER,1,child?1.5f:1,0,child?.08f:.5f);
            at=next;display.setPos(at);
            if(!child&&age==40)for(int n=0;n<11;n++){
                float childYaw=(level.getRandom().nextInt(360001)-180000)*.001f,childPitch=(level.getRandom().nextInt(30001)+60000)*.001f;
                add(level,owner,cause,epoch,at,childYaw,childPitch,41,true);
            }
            if(!level.getBlockState(BlockPos.containing(at)).is(AIRS)){
                var work=new Blasts(level,owner,cause,epoch,at,child);blasts.add(work);game.items().stonekivi().queuedWork(work);close();
            }else if(age>=180)close();
        }
        void close(){closed=true;tickets.close();if(display!=null)display.discard();flights.remove(this);}
    }
    private final class Blasts implements LbcStonekivi.Work {
        final ServerLevel level;final UUID owner;final LbcIndirectKills.Owner cause;final String epoch;final Vec3 at;final boolean child;final List<Vec3> offsets;final Tickets tickets;
        int cursor;boolean closed;
        Blasts(ServerLevel level,UUID owner,LbcIndirectKills.Owner cause,String epoch,Vec3 at,boolean child){this.level=level;this.owner=owner;this.cause=cause;this.epoch=epoch;this.at=at;this.child=child;offsets=offsets(child);tickets=new Tickets(level);}
        @Override public boolean ready(){return closed||!live(epoch)||!game.items().timeStop().stopped(level);}
        @Override public boolean snapshotIdle(){return true;}
        @Override public boolean step(){
            callbacks++;try{
                if(closed||!live(epoch)){cancel();return true;}
                if(cursor==offsets.size()){cancel();return true;}
                var point=at.add(offsets.get(cursor));float power=child?5:10;
                if(!tickets.resident(point,(int)(power*2)+1))return false;
                var direct=new Marker(EntityTypes.MARKER,level);direct.setPos(point);direct.entityTags().addAll(List.of(TAG,epoch));direct.setCustomName(Component.translatable("oreshnik"));
                var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.EXPLOSION),direct,game.server().getPlayerList().getPlayer(owner));
                var captured=game.items().indirectKills().captureDamage(damage,cause);
                try{game.items().indirectKills().trapChanges(level,cause,()->level.explode(direct,captured,null,point.x,point.y,point.z,power,false,Level.ExplosionInteraction.MOB));}
                finally{direct.discard();}
                cursor++;if(cursor==offsets.size()){cancel();return true;}return false;
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
    void prepareFrozen(){boundary();restoring=true;for(var flight:flights)flight.tickets.close();for(var work:blasts)work.tickets.close();}
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
                for(var flight:flights)flight.tickets.close();for(var work:blasts)work.tickets.close();
                flights=new LinkedHashSet<>();for(int n=0;n<image.size();n++){var i=image.get(n);var f=i.original;f.at=i.at;f.age=i.age;f.closed=i.closed;f.display=displays.get(n);flights.add(f);}
                blasts=new LinkedHashSet<>(accepted);
            };
        }
    }
}
