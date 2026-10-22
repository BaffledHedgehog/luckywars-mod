package me.skywars2.game;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Match-scoped, transferable mirror destinations, with independently owned chunk tickets. */
public final class LbcHomes {
    public static final String MANAGED = "skywars2.home";
    public static final TicketType HOME_TICKET = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    // TicketStorage uses FULL(33)-radius. Radius two therefore keeps the center entity ticking (31).
    private static final int TICKET_RADIUS = 2;
    private record ChunkKey(ServerLevel level, ChunkPos pos) { }
    private static final class Home {
        Marker marker;
        final ServerPlayer owner;
        final String epoch;
        ChunkKey ticket;
        boolean attached;
        int ticks;
        Home(Marker marker, ServerPlayer owner, String epoch) {
            this.marker=marker; this.owner=owner; this.epoch=epoch;
        }
    }
    private final GameController game;
    private final Map<UUID,Home> homes = new LinkedHashMap<>();
    private final Map<ChunkKey,Integer> tickets = new HashMap<>();
    private int lastTick = Integer.MIN_VALUE;

    public static void registerTicket() {
        Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.parse("skywars2:home"), HOME_TICKET);
    }
    public LbcHomes(GameController game) { this.game=game; }

    public boolean ready(ItemEntity token) {
        for (var tag:token.entityTags())
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        return !token.isRemoved() && token.getAge()<5999 && LbcHomeItems.matchesDrop(token.getItem())
                && game.playing() && token.level()==game.maps().level()
                && token.level() instanceof ServerLevel level && !game.items().timeStop().stopped(level)
                && resident(level, token.blockPosition());
    }

    /** The shared dispatcher owns the single token debit. All outputs use explicit entity handles. */
    public void spawn(ItemEntity token) {
        if (!ready(token)) return;
        var level=(ServerLevel)token.level();
        var pos=token.blockPosition();
        var marker=new Marker(EntityTypes.MARKER,level);
        marker.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        marker.entityTags().addAll(Set.of(MANAGED,"hometp.id","rc_1sec","magic"));
        game.items().markCurrentItemEpoch(marker);
        var thrower=game.items().specialDrops().owner(token);
        var home=new Home(marker,thrower instanceof ServerPlayer player ? player : null,game.items().epoch());
        homes.put(marker.getUUID(),home);
        if (!level.addFreshEntity(marker)) { forget(home); return; }
        // ENTITY_LOAD may have already attached this exact record.
        attach(home);
        var mirror=new ItemEntity(level,token.getX(),token.getY(),token.getZ(),LbcHomeItems.mirror(marker.getUUID()));
        mirror.setDefaultPickUpDelay();
        game.items().markCurrentItemEpoch(mirror);
        if (!level.addFreshEntity(mirror)) { forget(home); marker.discard(); }
    }

    public boolean use(ServerPlayer player, InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if (!LbcHomeItems.isMirror(stack)) return false;
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()
                || !game.canUseGameplayItems(player) || game.items().actionsBlocked(player)
                || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || game.items().timeStop().stopped(player.level()) || game.items().magic().suppressed(player)) return true;
        var origin=player.position();
        player.level().sendParticles(ParticleTypes.PORTAL,true,false,origin.x,origin.y+.81,origin.z,50,0,.5,0,.5);
        LbcOverload.sound(player,origin,"minecraft:entity.enderman.teleport",1,1,0);
        var data=stack.get(DataComponents.CUSTOM_DATA);
        if (data==null) return true;
        UUID id;
        try { id=UUID.fromString(data.copyTag().getStringOr("skywars2_home","")); }
        catch (IllegalArgumentException invalid) { return true; }
        var home=homes.get(id);
        if (home==null || !home.attached || !valid(home) || home.marker.level()!=player.level()
                || !resident(player.level(),home.marker.blockPosition())) return true;
        if (game.items().magic().homeMarkerSuppressedAt(player.level(),home.marker.position())) {
            forget(home); home.marker.discard(); return true;
        }
        var target=home.marker.position().add(0,1,0);
        player.teleportTo(target.x,target.y,target.z);
        return true;
    }

    public void loaded(Entity entity) {
        if (!(entity instanceof Marker marker) || !marker.entityTags().contains(MANAGED)) return;
        if (!game.items().currentEpoch(marker) || !game.playing() || marker.level()!=game.maps().level()) {
            marker.discard(); return;
        }
        var home=homes.get(marker.getUUID());
        if (home==null) {
            // Unknown current-epoch entities cannot reconstruct a retained causal author.
            marker.discard(); return;
        }
        home.marker=marker;
        attach(home);
    }

    public void unloaded(Entity entity) {
        var home=homes.get(entity.getUUID());
        if (home==null || home.marker!=entity) return;
        release(home);
        home.attached=false;
        // Ordinary chunk unload retains the same-match destination/author, but owns no ticket.
        if (entity.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK) homes.remove(entity.getUUID());
    }

    public void tick() {
        int now=game.server().getTickCount();
        if (lastTick==now) return;
        lastTick=now;
        for (var home:List.copyOf(homes.values())) {
            if (!game.playing() || !game.items().currentEpoch(home.epoch)) {
                forget(home); if (!home.marker.isRemoved()) home.marker.discard(); continue;
            }
            if (!home.attached) continue;
            if (!valid(home)) { forget(home); continue; }
            var level=(ServerLevel)home.marker.level();
            if (game.items().timeStop().stopped(level)) continue;
            if (game.items().magic().homeMarkerSuppressedAt(level,home.marker.position())) {
                forget(home); home.marker.discard(); continue;
            }
            attach(home); // A moved marker transfers only its own reference.
            if (++home.ticks<20) continue;
            if (!resident(level,home.marker.blockPosition())) { home.ticks=20; continue; }
            home.ticks=0;
            var pos=home.marker.blockPosition();
            level.sendParticles(ParticleTypes.END_ROD,true,false,home.marker.getX(),home.marker.getY()+1,
                    home.marker.getZ(),5,0,1,0,0);
            Runnable writes=()-> {
                if (!level.isOutsideBuildHeight(pos.below()) && !level.isDebug())
                    new BlockInput(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE).defaultBlockState(),Set.of(),null)
                            .place(level,pos.below(),818);
                LbcDropBlocks.fill(level,pos.above(),pos.above(2),Blocks.AIR,true);
            };
            if (home.owner!=null) game.items().indirectKills().trapChanges(level,home.owner,true,writes);
            else writes.run();
        }
    }

    private boolean valid(Home home) {
        return game.playing() && game.items().currentEpoch(home.epoch)
                && !home.marker.isRemoved() && game.items().currentEpoch(home.marker)
                && home.marker.entityTags().contains(MANAGED) && home.marker.entityTags().contains("hometp.id")
                && home.marker.level()==game.maps().level();
    }
    private static boolean resident(ServerLevel level, BlockPos pos) {
        // Native shape resolution can read the immediate neighbour across a chunk boundary.
        for (int x=(pos.getX()-1)>>4; x<=(pos.getX()+1)>>4; x++)
            for (int z=(pos.getZ()-1)>>4; z<=(pos.getZ()+1)>>4; z++)
                if (level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    private void attach(Home home) {
        var key=new ChunkKey((ServerLevel)home.marker.level(),new ChunkPos(home.marker.blockPosition().getX() >> 4,home.marker.blockPosition().getZ() >> 4));
        home.attached=true;
        if (key.equals(home.ticket)) return;
        release(home);
        if (tickets.merge(key,1,Integer::sum)==1)
            key.level().getChunkSource().addTicketWithRadius(HOME_TICKET,key.pos(),TICKET_RADIUS);
        home.ticket=key;
    }
    private void release(Home home) {
        var key=home.ticket;
        if (key==null) return;
        home.ticket=null;
        int remaining=tickets.getOrDefault(key,1)-1;
        if (remaining>0) tickets.put(key,remaining);
        else {
            tickets.remove(key);
            key.level().getChunkSource().removeTicketWithRadius(HOME_TICKET,key.pos(),TICKET_RADIUS);
        }
    }
    private void forget(Home home) {
        release(home); home.attached=false; homes.remove(home.marker.getUUID());
    }
    public void stop() {
        for (var home:List.copyOf(homes.values())) {
            forget(home); if (!home.marker.isRemoved()) home.marker.discard();
        }
        lastTick=Integer.MIN_VALUE;
    }
    int count() { return (int)homes.values().stream().filter(h->h.attached).count(); }
    List<Marker> snapshot() { return homes.values().stream().filter(h->h.attached).map(h->h.marker).toList(); }
    Marker marker(UUID id) { var home=homes.get(id); return home==null || !home.attached ? null : home.marker; }
    int ticketCount() { return tickets.size(); }
    int ticketReferences(ServerLevel level, ChunkPos pos) { return tickets.getOrDefault(new ChunkKey(level,pos),0); }
}
