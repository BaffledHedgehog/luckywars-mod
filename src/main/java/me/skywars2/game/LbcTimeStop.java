package me.skywars2.game;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import me.skywars2.mixin.TimeStopNativeMixin;
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
    private final Map<ServerLevel,Trail> trails=new IdentityHashMap<>();
    private static final class Trail {
        final String epoch;
        final java.util.ArrayList<Vec3> points=new java.util.ArrayList<>();
        final Map<BlockPos,java.util.ArrayList<Vec3>> cells=new HashMap<>();
        Trail(String epoch){this.epoch=epoch;}
        void sample(Vec3 point){
            var cell=BlockPos.containing(point);
            // The source selector suppresses a new marker within one block of any player's trail.
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                var nearby=cells.get(cell.offset(x,y,z));
                if(nearby!=null)for(var previous:nearby)if(previous.distanceToSqr(point)<=1)return;
            }
            points.add(point);cells.computeIfAbsent(cell,key->new java.util.ArrayList<>()).add(point);
        }
    }
    private DamageSource replaying;
    private Checkpoint rewinding;
    private final Map<ServerLevel,Map<BlockPos,Contact>> contacts=new IdentityHashMap<>();
    private final Set<Contact> contactLeases=java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final int CONTACT_FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
    private static final class Contact {
        final BlockState previous;
        final net.minecraft.world.level.chunk.LevelChunk chunk;
        final BlockPos pos;
        int holders;
        Contact(BlockState previous,net.minecraft.world.level.chunk.LevelChunk chunk,BlockPos pos) {
            this.previous=previous;this.chunk=chunk;this.pos=pos;
        }
    }
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
        int correctedAt=Integer.MIN_VALUE, presentedAt=Integer.MIN_VALUE;
        Contact contact;
        Frozen(Entity entity) {
            level=(ServerLevel)entity.level(); position=entity.position(); velocity=entity.getDeltaMovement();
            yaw=entity.getYRot(); pitch=entity.getXRot();
            noGravity=entity.isNoGravity();
        }
        Frozen(ServerLevel level,Vec3 position,Vec3 velocity,float yaw,float pitch,boolean noGravity){
            this.level=level;this.position=position;this.velocity=velocity;this.yaw=yaw;this.pitch=pitch;this.noGravity=noGravity;
        }
    }
    private record Hit(LivingEntity target,ServerLevel level,DamageSource source,float amount,String epoch) { }

    LbcTimeStop(GameController game) { this.game=game; }
    public boolean stopped(ServerLevel level) { return stops.containsKey(level); }
    int remaining(ServerLevel level) { return stops.getOrDefault(level,0); }
    int cooldownTicks(ServerPlayer player) {
        var cooldown=cooldowns.get(player.getUUID()); return cooldown==null?0:cooldown.ticks;
    }
    void reduceSourceCooldown(ServerPlayer player,int seconds) {
        var cooldown=cooldowns.get(player.getUUID());if(cooldown==null)return;
        int current=(int)(((long)cooldown.ticks+19)/20);
        cooldown.ticks=(int)Math.max(0,(long)cooldown.ticks-((long)current-seconds)*20);
        if(cooldown.ticks==0)cooldowns.remove(player.getUUID());
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
        if(rewinding!=null)return false;
        exemptions.remove(player);
        if(!LbcStand.isClocks(player.getItemInHand(hand))
                || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                || game.stage()==GameController.Stage.ENDING || cooldownTicks(player)>0
                || game.items().actionsBlocked(player)
                || game.items().magic().suppressedAt(player.level(),player.getEyePosition())) return false;
        cooldowns.put(player.getUUID(),new Cooldown(player.level()));
        begin(player.level(),STOP_TICKS);
        LbcOverload.sound(player,player.position(),"minecraft:lbcsounds.zawardo",1,1,0,.5f);
        return true;
    }

    /** Продление не переснимает нулевой Motion и не нормализует повторно очередь scheduled ticks. */
    void begin(ServerLevel level,int ticks) {
        if(rewinding!=null)throw new IllegalStateException("Finish the Time Stop checkpoint transaction before starting a stop");
        if(ticks<=0) throw new IllegalArgumentException("Time stop duration must be positive");
        boolean first=!stopped(level);
        stops.put(level,ticks); exemptions.clear();
        if(first){
            trails.put(level,new Trail(game.items().epoch()));
            LbcTimeStopSchedules.begin(level);
        }
        for(var entity:level.getAllEntities()) if(frozen(entity)) prepareFrozen(entity);
    }

    public boolean frozen(Entity entity) {
        if(rewinding!=null)return false;
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
        if(state!=null && entity.level()!=state.level) {
            frozen.remove(entity); restore(entity,state); state=null;
        }
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
            acquireContact(entity,state);
        }
        if(entity instanceof PrimedTnt tnt) {
            int tick=game.server().getTickCount();
            if(state.presentedAt==Integer.MIN_VALUE || tick-state.presentedAt>=40) {
                state.presentedAt=tick;
                sendTntFuse(tnt,presentationFuse());
            }
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
    /** Legacy uses the feet cell and replaces airs only. Existing blocks and BEs stay untouched. */
    private void acquireContact(Entity entity,Frozen state) {
        if(!(entity instanceof PrimedTnt) && !(entity instanceof FallingBlockEntity)) return;
        BlockPos pos=BlockPos.containing(state.position);
        var chunk=state.level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        if(chunk==null) return;
        var cells=contacts.computeIfAbsent(state.level,key->new HashMap<>());
        var contact=cells.get(pos);
        if(contact==null || contact.chunk!=chunk) {
            var previous=chunk.getBlockState(pos);
            if(!previous.isAir()) { if(cells.isEmpty()) contacts.remove(state.level); return; }
            contact=new Contact(previous,chunk,pos);
            if(!state.level.setBlock(pos,Blocks.BARRIER.defaultBlockState(),CONTACT_FLAGS)) {
                if(cells.isEmpty()) contacts.remove(state.level);
                return;
            }
            cells.put(pos,contact);contactLeases.add(contact);
        }
        contact.holders++; state.contact=contact;
    }
    private void releaseContact(Frozen state) {
        var contact=state.contact;
        if(contact==null) return;
        if(--contact.holders==0) {
            contactLeases.remove(contact);
            // Retain the actual chunk, so release never asks the level to load an absent chunk.
            if(contact.chunk.getBlockState(contact.pos).is(Blocks.BARRIER)) {
                var resident=state.level.getChunkSource().getChunkNow(contact.pos.getX()>>4,contact.pos.getZ()>>4);
                if(resident==contact.chunk) state.level.setBlock(contact.pos,contact.previous,CONTACT_FLAGS);
                else contact.chunk.setBlockState(contact.pos,contact.previous,CONTACT_FLAGS);
            }
            var cells=contacts.get(state.level);
            if(cells!=null) {
                cells.remove(contact.pos,contact);
                if(cells.isEmpty()) contacts.remove(state.level);
            }
        }
        state.contact=null;
    }
    /** Save authoritative pre-freeze physics without changing the live frozen entity. */
    public Vec3 persistedVelocity(Entity entity,Vec3 original) {
        var state=frozen.get(entity);return state==null?original:state.velocity;
    }
    public boolean persistedNoGravity(Entity entity,boolean original) {
        var state=frozen.get(entity);return state==null?original:state.noGravity;
    }
    /** copyOf owns these detached section copies; temporary collision blocks must never reach disk. */
    public net.minecraft.world.level.chunk.storage.SerializableChunkData persistedChunk(ServerLevel level,
            net.minecraft.world.level.chunk.ChunkAccess chunk,net.minecraft.world.level.chunk.storage.SerializableChunkData data) {
        if(contactLeases.isEmpty()) return data;
        boolean changed=false;
        for(var contact:contactLeases) {
            if(contact.chunk!=chunk) continue;
            for(var section:data.sectionData()) if(section.y()==(contact.pos.getY()>>4) && section.chunkSection()!=null) {
                int x=contact.pos.getX()&15,y=contact.pos.getY()&15,z=contact.pos.getZ()&15;
                if(section.chunkSection().getBlockState(x,y,z).is(Blocks.BARRIER)) {
                    section.chunkSection().setBlockState(x,y,z,contact.previous);changed=true;
                }
                break;
            }
        }
        if(!changed) return data;
        // Native read recomputes omitted heightmaps; invalidate copied lighting after removing solids.
        return new net.minecraft.world.level.chunk.storage.SerializableChunkData(data.containerFactory(),data.chunkPos(),
                data.minSectionY(),data.lastUpdateTime(),data.inhabitedTime(),data.chunkStatus(),data.blendingData(),
                data.belowZeroRetrogen(),data.upgradeData(),Map.of(),data.packedTicks(),data.postProcessingSections(),
                false,data.sectionData(),data.entities(),data.blockEntities(),data.structureData());
    }
    int contactCount() { return contactLeases.size(); }
    private int presentationFuse() { return 32000-Math.floorMod(game.server().getTickCount(),1000); }
    public List<SynchedEntityData.DataValue<?>> tntPresentation(Entity entity,List<SynchedEntityData.DataValue<?>> values) {
        if(!(entity instanceof PrimedTnt) || !frozen(entity)) return values;
        var result=new java.util.ArrayList<SynchedEntityData.DataValue<?>>(values);
        var accessor=TimeStopNativeMixin.TntData.skywars2$fuseData();
        result.removeIf(value->value.id()==accessor.id());
        result.add(SynchedEntityData.DataValue.create(accessor,presentationFuse()));
        return result;
    }
    private void sendTntFuse(PrimedTnt entity,int fuse) {
        var packet=new ClientboundSetEntityDataPacket(entity.getId(),List.of(
                SynchedEntityData.DataValue.create(TimeStopNativeMixin.TntData.skywars2$fuseData(),fuse)));
        ((ServerLevel)entity.level()).getChunkSource().sendToTrackingPlayersAndSelf(entity,packet);
    }
    private void restore(Entity entity,Frozen state) {
        releaseContact(state);
        if(entity.isRemoved() || !entity.isAlive()) return;
        if(entity instanceof PrimedTnt tnt) sendTntFuse(tnt,tnt.getFuse());
        entity.setNoGravity(state.noGravity);
        entity.setDeltaMovement(state.velocity); sendMotion(entity);
        if(entity instanceof ServerPlayer player) {
            player.connection.resetPosition(); player.connection.resetFlyingTicks();
        }
    }
    void end(ServerLevel level) {
        if(stops.remove(level)==null) return;
        var trail=trails.remove(level);
        if(trail!=null&&game.items().currentEpoch(trail.epoch)){
            var dust=new net.minecraft.core.particles.DustColorTransitionOptions(0xffffff,0,3);
            for(var point:trail.points){
                level.sendParticles(dust,true,false,point.x,point.y+1,point.z,2,.3,.6,.3,2);
                LbcOverload.sound(level,point,"minecraft:entity.lightning_bolt.impact",.1f,2,16,0);
            }
        }
        LbcTimeStopSchedules.end(level); exemptions.clear();
        for(var entry:List.copyOf(frozen.entrySet())) if(entry.getValue().level==level) {
            frozen.remove(entry.getKey()); restore(entry.getKey(),entry.getValue());
        }
    }

    /** RAW amount сохраняется до electric/TNT/armor модификаторов, которые выполнятся один раз при replay. */
    public boolean queueDamage(LivingEntity target,ServerLevel level,DamageSource source,float amount) {
        if(replaying(source) || !frozen(target) || !target.isAlive() || !(amount>0)
                || target.level()!=level || !game.items().allowDamage(target,source)) return false;
        source=game.items().indirectKills().inheritDamage(target,source);
        hits.computeIfAbsent(level,key->new ArrayDeque<>())
                .addLast(new Hit(target,level,source,amount,game.items().epoch()));
        return true;
    }
    public boolean replaying(DamageSource source) { return replaying==source; }

    public void tick() {
        if(rewinding!=null || !game.server().tickRateManager().runsNormally()) return;
        exemptions.clear();
        for(var level:stops.keySet()){
            String dimension=level.dimension().identifier().toString();
            if(dimension.equals("minecraft:nexus")||dimension.equals("minecraft:imprinted"))continue;
            var trail=trails.get(level);
            if(trail==null||!game.items().currentEpoch(trail.epoch))continue;
            for(var player:level.players())if(!player.isRemoved()&&!game.isObserver(player)
                    &&!player.entityTags().contains("spectator")&&!frozen(player))trail.sample(player.position());
        }
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
        rewinding=null; // Match/service cancellation must not leave the next stop permanently gated.
        // Match/epoch cancellation restores physics but must not replay an old stop's presentation.
        trails.clear();
        for(var level:List.copyOf(stops.keySet())) end(level);
        cooldowns.clear(); exemptions.clear(); hits.clear(); replaying=null;
        LbcTimeStopSchedules.clear();
    }

    /** Independent private-state image. Body values and native tick payloads stay with canonical owners. */
    Checkpoint checkpoint(MatchDamageSources sources){return new Checkpoint(sources);}
    final class Checkpoint {
        private record Lease(ServerLevel level,BlockPos pos,BlockState previous,int holders) { }
        private record Cell(ServerLevel level,BlockPos pos,int lease) { }
        private record Body(MatchDamageSources.Ref ref,Class<?> type,ServerLevel level,Vec3 position,Vec3 velocity,float yaw,float pitch,boolean noGravity,int corrected,int presented,int lease) { }
        private record Cool(ServerLevel level,int ticks) { }
        private record Exempt(MatchDamageSources.Ref ref,int tick,boolean immune) { }
        private record Damage(MatchDamageSources.Ref ref,ServerLevel level,MatchDamageSources.Image source,float amount,String epoch) { }
        private record Path(String epoch,List<Vec3> points) { }
        private final String epoch=game.items().epoch();
        private final int tick=game.server().getTickCount();
        private final MatchDamageSources sources;
        private final LbcTimeStopSchedules.Checkpoint clocks;
        private final Map<ServerLevel,Integer> stopped=new IdentityHashMap<>();
        private final Map<UUID,Cool> cool=new HashMap<>();
        private final List<Lease> leases;
        private final List<Cell> cells;
        private final List<Body> bodies;
        private final List<Exempt> immune;
        private final Map<ServerLevel,List<Damage>> damage=new IdentityHashMap<>();
        private final Map<ServerLevel,Path> paths=new IdentityHashMap<>();
        Checkpoint(MatchDamageSources sources){
            this.sources=sources;stable(false);if(rewinding!=null||replaying!=null)throw new IllegalStateException("Capture Time Stop outside replay/rewind callbacks");validateContacts();clocks=new LbcTimeStopSchedules.Checkpoint(game.server());
            stopped.putAll(stops);cooldowns.forEach((id,value)->cool.put(id,new Cool(value.level,value.ticks)));
            var leaseIds=new IdentityHashMap<Contact,Integer>();var leaseLevels=new IdentityHashMap<Contact,ServerLevel>();for(var state:frozen.values())if(state.contact!=null)leaseLevels.put(state.contact,state.level);
            var allLeases=new java.util.ArrayList<Lease>();for(var contact:contactLeases){var level=leaseLevels.get(contact);if(level==null||level.getChunkSource().getChunkNow(contact.pos.getX()>>4,contact.pos.getZ()>>4)!=contact.chunk)throw new IllegalStateException("Reconcile loaded Time Stop contact chunks before capture");leaseIds.put(contact,allLeases.size());allLeases.add(new Lease(level,contact.pos.immutable(),contact.previous,contact.holders));}leases=List.copyOf(allLeases);
            var allCells=new java.util.ArrayList<Cell>();contacts.forEach((level,map)->map.forEach((pos,contact)->allCells.add(new Cell(level,pos.immutable(),leaseIds.get(contact)))));cells=List.copyOf(allCells);
            var allBodies=new java.util.ArrayList<Body>();frozen.forEach((entity,state)->allBodies.add(new Body(sources.ref(entity),entity.getClass(),state.level,state.position,state.velocity,state.yaw,state.pitch,state.noGravity,state.correctedAt,state.presentedAt,state.contact==null?-1:leaseIds.get(state.contact))));bodies=List.copyOf(allBodies);
            var allImmune=new java.util.ArrayList<Exempt>();exemptions.forEach((player,value)->allImmune.add(new Exempt(sources.ref(player),value.tick,value.immune)));immune=List.copyOf(allImmune);
            hits.forEach((level,queue)->damage.put(level,queue.stream().map(hit->new Damage(sources.ref(hit.target),hit.level,sources.capture(hit.source),hit.amount,hit.epoch)).toList()));
            trails.forEach((level,path)->paths.put(level,new Path(path.epoch,List.copyOf(path.points))));
        }
        private void stable(boolean requireFrozen){
            if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||requireFrozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Time Stop checkpoints require the same stable match/native thread and a frozen restore");
        }
        /** FIRST phase: before canonical terrain ticks and actor/player bodies. Never end()/thaw/replay. */
        void prepareFrozen(){
            stable(true);if(rewinding!=null||replaying!=null)throw new IllegalStateException("Time Stop checkpoint transaction already active");clocks.validate();validateContacts();
            for(var lease:leases)chunk(lease); // Entire loaded-contact preflight precedes writes.
            rewinding=this;
            for(var state:List.copyOf(frozen.values()))releaseContact(state);
            stops.clear();frozen.clear();cooldowns.clear();exemptions.clear();hits.clear();trails.clear();contacts.clear();contactLeases.clear();
            clocks.restoreFrozen();
        }
        private net.minecraft.world.level.chunk.LevelChunk chunk(Lease lease){
            if(game.server().getLevel(lease.level.dimension())!=lease.level)throw new IllegalStateException("Time Stop contact dimension changed");
            var chunk=lease.level.getChunkSource().getChunkNow(lease.pos.getX()>>4,lease.pos.getZ()>>4);if(chunk==null)throw new IllegalStateException("Restore native contact chunks before Time Stop leases");return chunk;
        }
        private int stamp(int saved){return saved==Integer.MIN_VALUE?saved:saved+(game.server().getTickCount()-tick);}
        /** LAST link phase. Missing player/actor UUIDs defer the WHOLE install without losing hits. */
        Set<UUID> finishFrozen(java.util.function.Function<UUID,Entity> lookup){
            stable(true);if(rewinding!=this)throw new IllegalStateException("Prepare this Time Stop image before restoring its links");clocks.validate();
            var pending=new java.util.LinkedHashSet<UUID>();sources.missing(lookup,pending);
            var resolved=new HashMap<MatchDamageSources.Ref,Entity>();
            for(var body:bodies){var entity=MatchDamageSources.resolve(body.ref,lookup,pending);if(entity!=null){if(entity.getClass()!=body.type||entity.level()!=body.level)throw new IllegalStateException("Restore original frozen bodies before Time Stop links");resolved.put(body.ref,entity);}}
            for(var cache:immune){var entity=MatchDamageSources.resolve(cache.ref,lookup,pending);if(entity!=null){if(!(entity instanceof ServerPlayer))throw new IllegalStateException("Time Stop immunity cache requires its current player body");resolved.put(cache.ref,entity);}}
            for(var queue:damage.values())for(var hit:queue){var entity=MatchDamageSources.resolve(hit.ref,lookup,pending);if(entity!=null){if(!(entity instanceof LivingEntity)||entity.level()!=hit.level)throw new IllegalStateException("Restore original deferred-hit targets before Time Stop links");resolved.put(hit.ref,entity);}}
            if(!pending.isEmpty())return Set.copyOf(pending);
            var nativeChunks=new java.util.ArrayList<net.minecraft.world.level.chunk.LevelChunk>();for(var lease:leases){var chunk=chunk(lease);var state=chunk.getBlockState(lease.pos);if(state!=lease.previous&&!state.is(Blocks.BARRIER))throw new IllegalStateException("Restore canonical terrain before Time Stop contact cells");nativeChunks.add(chunk);}
            clocks.relinkFrozen();sources.restore(lookup,pending);if(!pending.isEmpty())throw new IllegalStateException("Native source graph changed within one frozen link phase");
            var restored=new java.util.ArrayList<Contact>();for(int i=0;i<leases.size();i++){var lease=leases.get(i);var contact=new Contact(lease.previous,nativeChunks.get(i),lease.pos);contact.holders=lease.holders;
                restored.add(contact);contactLeases.add(contact);}
            for(var cell:cells)contacts.computeIfAbsent(cell.level,key->new HashMap<>()).put(cell.pos,restored.get(cell.lease));
            for(var body:bodies){var state=new Frozen(body.level,body.position,body.velocity,body.yaw,body.pitch,body.noGravity);state.correctedAt=stamp(body.corrected);state.presentedAt=stamp(body.presented);if(body.lease>=0)state.contact=restored.get(body.lease);frozen.put(resolved.get(body.ref),state);}
            stopped.forEach(stops::put);cool.forEach((id,value)->{var current=new Cooldown(value.level);current.ticks=value.ticks;cooldowns.put(id,current);});
            for(var cache:immune)exemptions.put((ServerPlayer)resolved.get(cache.ref),new Exemption(stamp(cache.tick),cache.immune));
            damage.forEach((level,queue)->{var current=new ArrayDeque<Hit>();for(var hit:queue)current.add(new Hit((LivingEntity)resolved.get(hit.ref),hit.level,MatchDamageSources.value(hit.source),hit.amount,hit.epoch));hits.put(level,current);});
            paths.forEach((level,image)->{var path=new Trail(image.epoch);for(var point:image.points){path.points.add(point);path.cells.computeIfAbsent(BlockPos.containing(point),key->new java.util.ArrayList<>()).add(point);}trails.put(level,path);});
            // Keep every lease/body/index coherent BEFORE fallible native block writes. A failed
            // install remains quarantined and can still release its leases for a baseline restore.
            validateContacts();
            for(int i=0;i<leases.size();i++){var lease=leases.get(i);if(!nativeChunks.get(i).getBlockState(lease.pos).is(Blocks.BARRIER)&&!lease.level.setBlock(lease.pos,Blocks.BARRIER.defaultBlockState(),CONTACT_FLAGS))throw new IllegalStateException("Native Time Stop contact installation failed");}
            rewinding=null;validateContacts();
            for(var entity:frozen.keySet()){sendMotion(entity);if(entity instanceof PrimedTnt tnt)sendTntFuse(tnt,presentationFuse());}
            return Set.of();
        }
        /** A cancelled partial transaction still needs its caller's trusted baseline restore before resume. */
        void cancelFrozen(){if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel a checkpoint on its frozen native server thread");if(rewinding==this)rewinding=null;}
    }
    private void validateContacts(){
        var holders=new IdentityHashMap<Contact,Integer>();for(var state:frozen.values())if(state.contact!=null)holders.merge(state.contact,1,Integer::sum);
        if(holders.size()!=contactLeases.size()||!contactLeases.containsAll(holders.keySet()))throw new IllegalStateException("Unowned Time Stop contact lease");
        holders.forEach((contact,count)->{if(contact.holders!=count)throw new IllegalStateException("Time Stop contact holder count mismatch");});
        contacts.forEach((level,map)->map.forEach((pos,contact)->{if(!contactLeases.contains(contact)||!pos.equals(contact.pos))throw new IllegalStateException("Invalid active Time Stop contact index");}));
    }
}
