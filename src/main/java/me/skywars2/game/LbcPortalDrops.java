package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Independent native gateway pairs. Single-target vanilla spread search is sliced across ticks. */
public final class LbcPortalDrops {
    public static final String DATA="skywars2_random_portal";
    public interface StateAccess { CompoundTag skywars2$portalMetadata(); }
    public static final TicketType PORTAL_TICKET=new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final int MAX_ITERATIONS=10000;
    private static final long TICK_BUDGET=2_000_000L;
    private final GameController game;
    private record Key(ServerLevel level,ChunkPos pos) { }
    public record Plan(BlockPos sourceBase,BlockPos remoteBase,BlockPos sourceExit,BlockPos remoteExit) { }
    private static final class Job {
        final ItemEntity token;
        final ServerLevel level;
        final Vec3 source;
        final LivingEntity author;
        final String epoch;
        final float centerX,centerZ,range;
        final int maxHeight;
        final Set<Key> tickets=new HashSet<>();
        Vec3 candidate;
        Plan prepared;
        int iterations;
        Job(ItemEntity token,ServerLevel level,Vec3 source,LivingEntity author,String epoch,boolean noBorders) {
            this.token=token;this.level=level;this.source=source;this.author=author;this.epoch=epoch;
            // Vec2Argument converts native command coordinates to floats, including relative centers.
            // Native Vec2Argument.vec2() center-corrects absolute integer `0 0` to (.5,.5).
            centerX=noBorders?(float)source.x:.5F;centerZ=noBorders?(float)source.z:.5F;
            range=noBorders?200:170;
            maxHeight=level.dimension()==Level.NETHER?120:level.getMaxY()+1;
        }
    }
    private final ArrayDeque<Job> jobs=new ArrayDeque<>();
    private final Map<ItemEntity,Job> pending=new IdentityHashMap<>();
    private final Map<Key,Integer> tickets=new HashMap<>();
    private final Map<java.util.UUID,ServerPlayer> authors=new HashMap<>();
    private int lastTick=Integer.MIN_VALUE;
    // Native fixtures snapshot only these two small columns before their first write.
    java.util.function.Consumer<Plan> beforePlace;

    public LbcPortalDrops(GameController game) { this.game=game; }
    public static void registerTicket() {
        Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:random_portal"),PORTAL_TICKET);
    }
    public int pendingCount() { return pending.size(); }
    public boolean idle() { return pending.isEmpty() && jobs.isEmpty() && tickets.isEmpty(); }
    public static Plan plan(Vec3 source,Vec3 remote) {
        // /data get doubles returns floor, including negative/fractional coordinates.
        var a=BlockPos.containing(source);var b=BlockPos.containing(remote);
        return new Plan(a,b,b,a);
    }
    private boolean admitted(ItemEntity token) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcPortalDropItem.matches(token.getItem())
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        return level.getChunkSource().getChunkNow(token.blockPosition().getX()>>4,token.blockPosition().getZ()>>4)!=null;
    }
    /** Prepare first; the common dispatcher waits before running any flag or its single debit. */
    public boolean ready(ItemEntity token) {
        if(!admitted(token)) { unloaded(token);return false; }
        var job=pending.get(token);
        if(job==null) {
            job=new Job(token,(ServerLevel)token.level(),token.position(),game.items().specialDrops().owner(token),
                    game.items().epoch(),game.config().noBorders);
            pending.put(token,job);jobs.addLast(job);
        }
        return job.prepared!=null;
    }
    public void spawn(ItemEntity token) {
        if(!ready(token))return;
        var job=pending.remove(token);
        place(job.level,job.prepared,job.author);
        release(job);
    }
    public void unloaded(Entity entity) {
        if(entity instanceof ItemEntity token) {
            var job=pending.remove(token);
            if(job!=null) { jobs.remove(job);release(job); }
        }
    }
    public void tick() {
        int now=game.server().getTickCount();if(now==lastTick)return;lastTick=now;
        for(var job:java.util.List.copyOf(pending.values())) {
            if(job.token.isRemoved() || job.token.getAge()>=5999 || !LbcPortalDropItem.matches(job.token.getItem())
                    || !game.playing() || game.stage()==GameController.Stage.ENDING
                    || job.token.level()!=job.level || !game.items().currentEpoch(job.epoch)
                    || job.token.entityTags().stream().anyMatch(tag->tag.startsWith("skywars2.items.")
                        && !game.items().currentEpoch(tag)))unloaded(job.token);
        }
        long deadline=System.nanoTime()+TICK_BUDGET;
        int steps=32;
        while(steps-->0 && !jobs.isEmpty() && System.nanoTime()<deadline) {
            var job=jobs.removeFirst();
            if(!game.playing() || game.stage()==GameController.Stage.ENDING
                    || !game.items().currentEpoch(job.epoch) || job.level!=game.maps().level()) { pending.remove(job.token);release(job);continue; }
            if(game.items().timeStop().stopped(job.level)) { jobs.addLast(job);continue; }
            if(!advance(job))jobs.addLast(job);
        }
    }
    private boolean advance(Job job) {
        if(job.candidate==null) {
            double x=Mth.nextDouble(job.level.getRandom(),(double)(job.centerX-job.range),(double)(job.centerX+job.range));
            double z=Mth.nextDouble(job.level.getRandom(),(double)(job.centerZ-job.range),(double)(job.centerZ+job.range));
            job.candidate=new Vec3(Mth.floor(x)+.5,0,Mth.floor(z)+.5);
            request(job,BlockPos.containing(job.candidate));
            // Source writes also need their neighbour chunks; never wait forever at a loaded edge.
            request(job,BlockPos.containing(job.source));
        }
        for(var key:job.tickets)if(key.level.getChunkSource().getChunkNow(key.pos.x(),key.pos.z())==null)return false;
        int y=spawnY(job.level,BlockPos.containing(job.candidate),job.maxHeight);
        var below=BlockPos.containing(job.candidate.x,y-1,job.candidate.z);
        var state=job.level.getBlockState(below);
        boolean safe=below.getY()<job.maxHeight && !state.liquid() && state.is(BlockTags.ENTITIES_CAN_TELEPORT_TO);
        job.iterations++;
        // Vanilla throws after iteration10000 even when the last inspected candidate is safe.
        if(job.maxHeight<job.level.getMinY() || job.iterations>=MAX_ITERATIONS) {
            job.prepared=plan(job.source,job.source);return true;
        }
        if(safe) {
            job.prepared=plan(job.source,new Vec3(job.candidate.x,y,job.candidate.z));return true;
        }
        release(job);job.candidate=null;return false;
    }
    /** Exact SpreadPlayersCommand.Position.getSpawnY, operating only on a resident column. */
    static int spawnY(ServerLevel level,BlockPos column,int maxHeight) {
        var pos=new BlockPos.MutableBlockPos(column.getX(),maxHeight+1,column.getZ());
        boolean air2=level.getBlockState(pos).isAir();pos.move(0,-1,0);
        boolean air1=level.getBlockState(pos).isAir();
        while(pos.getY()>level.getMinY()) {
            pos.move(0,-1,0);boolean air=level.getBlockState(pos).isAir();
            if(!air && air1 && air2)return pos.getY()+1;
            air2=air1;air1=air;
        }
        return maxHeight+1;
    }
    private void request(Job job,BlockPos pos) {
        for(int x=(pos.getX()-1)>>4;x<=(pos.getX()+1)>>4;x++)
            for(int z=(pos.getZ()-1)>>4;z<=(pos.getZ()+1)>>4;z++) {
                var key=new Key(job.level,new ChunkPos(x,z));
                if(!job.tickets.add(key))continue;
                if(tickets.merge(key,1,Integer::sum)==1)job.level.getChunkSource().addTicketWithRadius(PORTAL_TICKET,key.pos,0);
            }
    }
    private void release(Job job) {
        for(var key:job.tickets) {
            int count=tickets.getOrDefault(key,0);
            if(count<=1) { tickets.remove(key);key.level.getChunkSource().removeTicketWithRadius(PORTAL_TICKET,key.pos,0); }
            else tickets.put(key,count-1);
        }
        job.tickets.clear();
    }
    /** Pair preflight avoids partial build-height writes; source supports and arrival Y stay unchanged. */
    void place(ServerLevel level,Plan plan,LivingEntity author) {
        if(level.isDebug() || !fits(level,plan.sourceBase) || !fits(level,plan.remoteBase))return;
        if(beforePlace!=null)beforePlace.accept(plan);
        Runnable writes=()-> {
            column(level,plan.remoteBase,plan.remoteExit,author);
            column(level,plan.sourceBase,plan.sourceExit,author);
        };
        var player=LbcIndirectKills.playerSource(author);
        if(player!=null)game.items().indirectKills().trapChanges(level,player,true,writes);
        else game.items().indirectKills().worldChanges(level,author,true,writes);
    }
    private static boolean fits(ServerLevel level,BlockPos base) {
        return !level.isOutsideBuildHeight(base.below()) && !level.isOutsideBuildHeight(base.above(4))
                && Level.isInSpawnableBounds(base) && Level.isInSpawnableBounds(base.above(4));
    }
    private void column(ServerLevel level,BlockPos base,BlockPos exit,LivingEntity author) {
        LbcDropBlocks.setBlock(level,base.below(),Blocks.BEDROCK.defaultBlockState(),false);
        LbcDropBlocks.setBlock(level,base.above(4),Blocks.BEDROCK.defaultBlockState(),false);
        var portal=base.above(3);
        LbcDropBlocks.setBlock(level,portal,Blocks.END_GATEWAY.defaultBlockState(),false);
        if(!level.isOutsideBuildHeight(portal) && level.getBlockEntity(portal) instanceof TheEndGatewayBlockEntity be) {
            be.setExitPosition(exit,true);
            var metadata=((StateAccess)be).skywars2$portalMetadata();metadata.remove("owner");
            metadata.putString("epoch",game.items().epoch());
            var player=LbcIndirectKills.playerSource(author);
            if(player!=null) {
                metadata.putString("owner",player.getUUID().toString());
                authors.put(player.getUUID(),player);
            }
            be.setChanged();
            level.sendBlockUpdated(portal,be.getBlockState(),be.getBlockState(),Block.UPDATE_CLIENTS);
        }
    }
    public boolean canUseGateway(ServerLevel level,Entity actor,BlockPos pos) {
        if(!(level.getBlockEntity(pos) instanceof StateAccess state) || state.skywars2$portalMetadata().isEmpty())return true;
        var data=state.skywars2$portalMetadata();
        return game.playing() && game.stage()!=GameController.Stage.ENDING && level==game.maps().level()
                && game.items().currentEpoch(data.getStringOr("epoch",""))
                && !game.items().timeStop().stopped(level) && game.items().wands().validTarget(actor)
                && passengersAllowed(actor);
    }
    private boolean passengersAllowed(Entity actor) {
        for (var passenger : actor.getIndirectPassengers())
            if (!game.items().wands().validTarget(passenger)) return false;
        return true;
    }
    public void arrived(Entity actor,CompoundTag data) {
        if(data.isEmpty() || !game.playing() || game.stage()==GameController.Stage.ENDING
                || !game.items().currentEpoch(data.getStringOr("epoch","")) || actor.level()!=game.maps().level()
                || !(actor.level() instanceof ServerLevel level) || game.items().timeStop().stopped(level)
                || !game.items().wands().validTarget(actor)
                || !(actor instanceof LivingEntity living))return;
        try {
            var id=java.util.UUID.fromString(data.getStringOr("owner",""));
            var owner=game.server().getPlayerList().getPlayer(id);
            if(owner==null)owner=authors.get(id);
            if(owner!=null)game.items().indirectKills().teleported(living,owner,true);
        } catch(IllegalArgumentException absentOwner) { }
    }
    public void stop() { for(var job:pending.values())release(job);pending.clear();jobs.clear();authors.clear(); }
}
