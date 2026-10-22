package me.skywars2.game;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Локальная остановка измерения: native tick gates, сохранённая физика и принятые отложенные удары. */
public final class LbcTimeStop {
    private static final int STOP_TICKS=400, COOLDOWN_TICKS=800;
    private final GameController game;
    private final Map<ServerLevel,Integer> stops=new IdentityHashMap<>();
    private final Map<Entity,Frozen> frozen=new IdentityHashMap<>();
    private final Map<UUID,Cooldown> cooldowns=new HashMap<>();
    private final Map<ServerPlayer,Exemption> exemptions=new IdentityHashMap<>();
    private final Map<ServerLevel,ArrayDeque<Hit>> hits=new IdentityHashMap<>();
    private DamageSource replaying;
    private record Exemption(int tick,boolean immune) { }
    private static final class Cooldown {
        final ServerLevel level;
        int ticks=COOLDOWN_TICKS;
        Cooldown(ServerLevel level) { this.level=level; }
    }
    private static final class Frozen {
        final ServerLevel level;
        final Vec3 position, velocity;
        final float yaw,pitch;
        final boolean noGravity;
        int correctedAt=Integer.MIN_VALUE;
        Frozen(Entity entity) {
            level=(ServerLevel)entity.level(); position=entity.position(); velocity=entity.getDeltaMovement();
            yaw=entity.getYRot(); pitch=entity.getXRot();
            noGravity=entity.isNoGravity();
        }
    }
    private record Hit(LivingEntity target,ServerLevel level,DamageSource source,float amount,String epoch) { }

    LbcTimeStop(GameController game) { this.game=game; }
    public boolean stopped(ServerLevel level) { return stops.containsKey(level); }
    int remaining(ServerLevel level) { return stops.getOrDefault(level,0); }
    int cooldownTicks(ServerPlayer player) {
        var cooldown=cooldowns.get(player.getUUID()); return cooldown==null?0:cooldown.ticks;
    }
    int pendingDamageCount() { return hits.values().stream().mapToInt(ArrayDeque::size).sum(); }
    void addPlayerCounters(ServerPlayer player) {
        var cooldown=cooldowns.get(player.getUUID());
        if(cooldown==null) {
            cooldown=new Cooldown(player.level()); cooldown.ticks=1;
            cooldowns.put(player.getUUID(),cooldown);
        } else cooldown.ticks++;
    }

    public boolean useClocks(ServerPlayer player,InteractionHand hand) {
        exemptions.remove(player);
        if(!LbcStand.isClocks(player.getItemInHand(hand))
                || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                || game.stage()==GameController.Stage.ENDING || cooldownTicks(player)>0
                || game.items().actionsBlocked(player)
                || game.items().magic().suppressedAt(player.level(),player.getEyePosition())) return false;
        cooldowns.put(player.getUUID(),new Cooldown(player.level()));
        begin(player.level(),STOP_TICKS);
        LbcOverload.sound(player,player.position(),"minecraft:lbcsounds.zawardo",10,1,0);
        return true;
    }

    /** Продление не переснимает нулевой Motion и не нормализует повторно очередь scheduled ticks. */
    void begin(ServerLevel level,int ticks) {
        if(ticks<=0) throw new IllegalArgumentException("Time stop duration must be positive");
        boolean first=!stopped(level);
        stops.put(level,ticks); exemptions.clear();
        if(first) LbcTimeStopSchedules.begin(level);
        for(var entity:level.getAllEntities()) if(frozen(entity)) prepareFrozen(entity);
    }

    public boolean frozen(Entity entity) {
        if(!(entity.level() instanceof ServerLevel level) || !stopped(level) || entity.isRemoved()) return false;
        if(entity.entityTags().contains("have_stand")) return false;
        if(game.items().stand().movesDuringTimeStop(entity)) return false;
        if(entity instanceof ServerPlayer player) {
            if(game.isObserver(player)) return false;
            int tick=game.server().getTickCount();
            var cached=exemptions.get(player);
            if(cached==null || cached.tick()!=tick) {
                cached=new Exemption(tick,LbcStand.hasClocks(player)); exemptions.put(player,cached);
            }
            return !cached.immune();
        }
        return true;
    }

    /** AI/NBT не переписываются; синхронный gravity-флаг удерживает также клиентский полёт. */
    public void prepareFrozen(Entity entity) {
        if(!frozen(entity)) return;
        var state=frozen.get(entity);
        if(state==null) {
            state=new Frozen(entity); frozen.put(entity,state);
            // У смешанной связки транспорта tick родителя иначе двигал бы замершего пассажира
            // или пропускал бы свободного пассажира. Разделяем только такую связку на переходе.
            if(entity.getVehicle()!=null && !frozen(entity.getVehicle())) entity.stopRiding();
            for(var passenger:List.copyOf(entity.getPassengers())) if(!frozen(passenger)) passenger.stopRiding();
            if(entity instanceof ServerPlayer player) {
                player.setLastClientInput(Input.EMPTY);
                player.gameMode.tick(); // существующий actionsBlocked-hook отменяет начатое копание
            }
            entity.setDeltaMovement(Vec3.ZERO);
            if(!(entity instanceof ServerPlayer)) entity.setNoGravity(true);
            sendMotion(entity);
        }
        entity.setPos(state.position); entity.setYRot(state.yaw); entity.setXRot(state.pitch);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /** Connection продолжает keepalive/teleport ACK, а клиенту возвращается авторитетная неподвижная поза. */
    public void handleFrozenPlayer(ServerPlayer player) {
        prepareFrozen(player);
        var state=frozen.get(player);
        if(state==null) return;
        int tick=game.server().getTickCount();
        if(state.correctedAt==tick) return;
        state.correctedAt=tick;
        player.connection.teleport(state.position.x,state.position.y,state.position.z,state.yaw,state.pitch);
        player.setDeltaMovement(Vec3.ZERO);
        player.connection.resetPosition(); player.connection.resetFlyingTicks();
    }

    private void sendMotion(Entity entity) {
        if(entity.level() instanceof ServerLevel level) {
            var packet=new ClientboundSetEntityMotionPacket(entity);
            level.getChunkSource().sendToTrackingPlayersAndSelf(entity,packet);
        }
    }
    private void restore(Entity entity,Frozen state) {
        if(entity.isRemoved() || !entity.isAlive() || entity.level()!=state.level) return;
        entity.setNoGravity(state.noGravity);
        entity.setDeltaMovement(state.velocity); sendMotion(entity);
        if(entity instanceof ServerPlayer player) {
            player.connection.resetPosition(); player.connection.resetFlyingTicks();
        }
    }
    void end(ServerLevel level) {
        if(stops.remove(level)==null) return;
        LbcTimeStopSchedules.end(level); exemptions.clear();
        for(var entry:List.copyOf(frozen.entrySet())) if(entry.getValue().level==level) {
            frozen.remove(entry.getKey()); restore(entry.getKey(),entry.getValue());
        }
    }

    /** RAW amount сохраняется до electric/TNT/armor модификаторов, которые выполнятся один раз при replay. */
    public boolean queueDamage(LivingEntity target,ServerLevel level,DamageSource source,float amount) {
        if(replaying(source) || !frozen(target) || !target.isAlive() || !(amount>0)
                || target.level()!=level || !game.items().allowDamage(target,source)) return false;
        hits.computeIfAbsent(level,key->new ArrayDeque<>())
                .addLast(new Hit(target,level,source,amount,game.items().epoch()));
        return true;
    }
    public boolean replaying(DamageSource source) { return replaying==source; }

    public void tick() {
        if(!game.server().tickRateManager().runsNormally()) return;
        exemptions.clear();
        Set<ServerLevel> pausedAtStart=new HashSet<>(stops.keySet());
        for(var level:List.copyOf(stops.keySet())) {
            int left=stops.get(level)-1;
            if(left<=0) end(level); else stops.put(level,left);
        }
        for(var entry:List.copyOf(frozen.entrySet())) {
            if(!frozen(entry.getKey())) { frozen.remove(entry.getKey()); restore(entry.getKey(),entry.getValue()); }
            else prepareFrozen(entry.getKey());
        }
        cooldowns.entrySet().removeIf(entry->!pausedAtStart.contains(entry.getValue().level) && --entry.getValue().ticks<=0);
        // Все принятые попадания сохраняются. Короткий бюджет лишь переносит оставшиеся на следующий тик.
        long deadline=System.nanoTime()+3_000_000L;
        for(var entry:List.copyOf(hits.entrySet())) {
            // Не обходим накопленные удары каждый тик неподвижного мира.
            if(stopped(entry.getKey())) continue;
            var queue=entry.getValue();
            // Урон может синхронно завершить матч и очистить владеющий map.
            while(hits.get(entry.getKey())==queue && !queue.isEmpty()) {
                var hit=queue.removeFirst();
                if(hit.target.isAlive() && !hit.target.isRemoved() && hit.target.level()==hit.level
                        && game.items().currentEpoch(hit.epoch)) {
                    replaying=hit.source;
                    try { hit.target.hurtServer(hit.level,hit.source,hit.amount); }
                    finally { replaying=null; }
                }
                if(System.nanoTime()>=deadline) break;
            }
            if(queue.isEmpty()) hits.remove(entry.getKey(),queue);
            if(System.nanoTime()>=deadline) break;
        }
    }
    void forget(ServerPlayer player) {
        var state=frozen.remove(player); if(state!=null) restore(player,state);
        exemptions.remove(player); cooldowns.remove(player.getUUID());
    }
    void stop() {
        for(var level:List.copyOf(stops.keySet())) end(level);
        cooldowns.clear(); exemptions.clear(); hits.clear(); replaying=null;
        LbcTimeStopSchedules.clear();
    }
}
