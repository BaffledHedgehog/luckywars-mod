package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import me.skywars2.mixin.RaidCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;

/** Native world raid owners and their body graph. Part of the unfinished goal-image experiment. */
public final class MatchRaids {
    private final Map<ServerLevel,Raids> levels=new IdentityHashMap<>();
    private final Map<Raids,Manager> managers=new IdentityHashMap<>();
    private final Map<Raid,Image> raids=new IdentityHashMap<>();
    private final Map<Raider,Body> bodies=new IdentityHashMap<>();
    private final MatchDamageSources sources;
    private boolean linksRestored;
    public MatchRaids(GameController game,Collection<Entity> registered,MatchDamageSources sources){
        this.sources=sources;
        for(var level:game.server().getAllLevels()){
            Raids owner=level.getRaids();levels.put(level,owner);
            var manager=managers.computeIfAbsent(owner,Manager::new);
            for(var raid:manager.entries.values())capture(raid);
        }
        for(var actor:registered)if(actor instanceof Raider raider)capture(raider);
    }
    private void capture(Raider body){
        if(bodies.containsKey(body))return;
        bodies.put(body,new Body(body,sources.dependencyRef(body)));capture(body.getCurrentRaid());
    }
    private void capture(Raid owner){
        if(owner==null||raids.containsKey(owner))return;
        var image=new Image(owner);raids.put(owner,image);
        image.leaders.values().forEach(member->capture(member.original));
        image.groups.values().forEach(group->group.members.forEach(member->capture(member.original)));
    }
    public void validate(){levels.forEach((level,owner)->{if(level.getRaids()!=owner)throw new IllegalStateException("Native raid service owner changed");});}
    public void prepareFrozen(Collection<Entity> current){
        validate();linksRestored=false;var future=Collections.newSetFromMap(new IdentityHashMap<Raid,Boolean>());
        for(var owner:managers.keySet())future.addAll(managerAccess(owner).skywars2$raidMap().values());
        for(var entity:current)if(entity instanceof Raider body&&body.getCurrentRaid()!=null)future.add(body.getCurrentRaid());
        for(var raid:future){var bar=raidAccess(raid).skywars2$raidEvent();bar.setVisible(false);bar.removeAllPlayers();}
        managers.values().forEach(Manager::restore);
        raids.values().forEach(Image::restoreValues);
    }
    public void restoreLinks(Function<UUID,Entity> lookup,Set<UUID> pending){
        validate();
        if(linksRestored){raids.values().forEach(image->image.bar.rebindCurrent(image.event,lookup,pending));return;}
        bodies.values().forEach(Body::restore);
        raids.values().forEach(image->image.restoreLinks(lookup,pending));
        linksRestored=true;
    }
    private static RaidCheckpointAccess.Manager managerAccess(Raids owner) {
        return (RaidCheckpointAccess.Manager)(Object) owner;
    }
    private static RaidCheckpointAccess.State raidAccess(Raid owner) {
        return (RaidCheckpointAccess.State)(Object) owner;
    }

    private static final class Manager {
        final Raids owner;
        final Map<Integer,Raid> original, entries;
        final int nextId, tick;
        Manager(Raids owner) {
            this.owner=owner;
            var access = managerAccess(owner);
            original=access.skywars2$raidMap();
            entries=new LinkedHashMap<>(original);
            nextId=access.skywars2$raidNextId();
            tick=access.skywars2$raidTick();
        }
        void restore() {
            var access = managerAccess(owner);
            requireIdentity(access.skywars2$raidMap(),original,"raidMap");
            original.clear();
            original.putAll(entries);
            access.skywars2$raidNextId(nextId);
            access.skywars2$raidTick(tick);
            // Persist these restored contents over a future disk save.
            owner.setDirty();
        }
    }
    private static final class Body {
        final Raider owner;
        final Raid raid;
        final MatchDamageSources.Ref ref;
        final int wave, ticksOutside;
        final boolean canJoin;

        Body(Raider owner, MatchDamageSources.Ref ref) {
            this.owner=owner;
            this.ref=ref;
            raid=owner.getCurrentRaid();
            var access = (RaidCheckpointAccess.Body)(Object) owner;
            wave=access.skywars2$raidWave();
            canJoin=access.skywars2$raidCanJoin();
            ticksOutside=access.skywars2$raidOutside();
        }
        void restore() {
            var access = (RaidCheckpointAccess.Body)(Object) owner;
            access.skywars2$raidWave(wave);
            access.skywars2$raidCanJoin(canJoin);
            access.skywars2$raidOutside(ticksOutside);
            boolean omitted=ref.historical()==null && MatchDamageSources.intentionallyAbsent(ref.id());
            owner.setCurrentRaid(omitted?null:raid);
        }
    }

    private record RaidValues(long ticksActive, BlockPos center, boolean started, float totalHealth, int raidOmenLevel,
                              boolean active, int groupsSpawned, int postRaidTicks, int raidCooldownTicks, int numGroups,
                              Raid.RaidStatus status, int celebrationTicks, Optional<BlockPos> waveSpawnPos) {
        RaidValues(RaidCheckpointAccess.State access) {
            this(access.skywars2$raidTicksActive(),access.skywars2$raidCenter(),access.skywars2$raidStarted(),
                    access.skywars2$raidTotalHealth(),access.skywars2$raidOmenLevel(),access.skywars2$raidActive(),
                    access.skywars2$raidGroupsSpawned(),access.skywars2$raidPostRaidTicks(),access.skywars2$raidCooldownTicks(),
                    access.skywars2$raidNumGroups(),access.skywars2$raidStatus(),access.skywars2$raidCelebrationTicks(),
                    access.skywars2$raidWaveSpawnPos());
        }
        void restore(RaidCheckpointAccess.State access) {
            if (access.skywars2$raidNumGroups() != numGroups) throw new IllegalStateException("Final native raid group count changed");
            access.skywars2$raidTicksActive(ticksActive);
            access.skywars2$raidCenter(center);
            access.skywars2$raidStarted(started);
            access.skywars2$raidTotalHealth(totalHealth);
            access.skywars2$raidOmenLevel(raidOmenLevel);
            access.skywars2$raidActive(active);
            access.skywars2$raidGroupsSpawned(groupsSpawned);
            access.skywars2$raidPostRaidTicks(postRaidTicks);
            access.skywars2$raidCooldownTicks(raidCooldownTicks);
            access.skywars2$raidStatus(status);
            access.skywars2$raidCelebrationTicks(celebrationTicks);
            access.skywars2$raidWaveSpawnPos(waveSpawnPos);
        }
    }
    private record Member(Raider original,MatchDamageSources.Ref ref,float health) { }
    private record Group(Set<Raider> original,List<Member> members) { }
    private final class Image {
        final Raid owner;
        final RaidValues values;
        final Map<Integer,Raider> leaderMap;
        final Map<Integer,Set<Raider>> groupMap;
        final Map<Integer,Member> leaders=new LinkedHashMap<>();
        final Map<Integer,Group> groups=new LinkedHashMap<>();
        final Set<UUID> heroes,heroValues;
        final Map<BlockPos.MutableBlockPos,BlockPos> positions=new IdentityHashMap<>();
        final RandomSource random;final MatchRandomState randomState;
        final ServerBossEvent event;final Bar bar;
        Image(Raid owner){
            this.owner=owner;
            var access = raidAccess(owner);
            values = new RaidValues(access);
            if (values.center() instanceof BlockPos.MutableBlockPos position) positions.put(position,position.immutable());
            if (values.waveSpawnPos().orElse(null) instanceof BlockPos.MutableBlockPos position) positions.put(position,position.immutable());
            leaderMap = access.skywars2$raidLeaders();
            leaderMap.forEach((wave,body)->leaders.put(wave,member(body)));
            groupMap = access.skywars2$raidGroups();
            groupMap.forEach((wave,members)->groups.put(wave,new Group(members,members.stream().map(this::member).toList())));
            heroes = access.skywars2$raidHeroes();
            heroValues = Set.copyOf(heroes);
            random = access.skywars2$raidRandom();
            randomState = MatchRandomState.capture(random);
            event = access.skywars2$raidEvent();
            bar = new Bar(event);
        }
        Member member(Raider body){return new Member(body,sources.dependencyRef(body),body.getHealth());}
        void restoreValues(){
            var access = raidAccess(owner);
            positions.forEach(BlockPos.MutableBlockPos::set);
            values.restore(access);
            requireIdentity(access.skywars2$raidRandom(),random,"random");
            randomState.restore(random);
            requireIdentity(access.skywars2$raidEvent(),event,"raidEvent");
            requireIdentity(access.skywars2$raidHeroes(),heroes,"heroesOfTheVillage");
            heroes.clear();
            heroes.addAll(heroValues);
        }
        void restoreLinks(Function<UUID,Entity> lookup,Set<UUID> pending){
            var access = raidAccess(owner);
            requireIdentity(access.skywars2$raidLeaders(),leaderMap,"groupToLeaderMap");leaderMap.clear();
            leaders.forEach((wave,member)->{var body=resolve(member,lookup,pending);if(body!=null)leaderMap.put(wave,body);});
            requireIdentity(access.skywars2$raidGroups(),groupMap,"groupRaiderMap");groupMap.clear();float removedHealth=0;
            for(var entry:groups.entrySet()){
                var group=entry.getValue();group.original.clear();
                for(var member:group.members){
                    var body=resolve(member,lookup,pending);
                    if(body!=null)group.original.add(body);
                    else if(MatchDamageSources.intentionallyAbsent(member.ref.id()))removedHealth+=member.health;
                }
                groupMap.put(entry.getKey(),group.original);
            }
            access.skywars2$raidTotalHealth(values.totalHealth()-removedHealth);
            bar.restore(event,lookup,pending);
            // Omitted bodies leave as native removeFromRaid(...,true), without lifecycle callbacks.
            if(removedHealth!=0)owner.updateBossbar();
        }
    }
    private static Raider resolve(Member member,Function<UUID,Entity> lookup,Set<UUID> pending){
        Entity body=MatchDamageSources.resolve(member.ref,lookup,pending);
        if(body!=null&&!(body instanceof Raider))throw new IllegalStateException("Native raid member resolved to another body type");
        return (Raider)body;
    }
    private record Bar(Component name,float progress,BossEvent.BossBarColor color,BossEvent.BossBarOverlay overlay,
                       boolean dark,boolean music,boolean fog,boolean visible,Set<UUID> players){
        Bar(ServerBossEvent owner){this(owner.getName().copy(),owner.getProgress(),owner.getColor(),owner.getOverlay(),owner.shouldDarkenScreen(),
                owner.shouldPlayBossMusic(),owner.shouldCreateWorldFog(),owner.isVisible(),owner.getPlayers().stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toUnmodifiableSet()));}
        void restore(ServerBossEvent owner,Function<UUID,Entity> lookup,Set<UUID> pending){
            owner.setVisible(false);owner.removeAllPlayers();owner.setName(name.copy());owner.setProgress(progress);owner.setColor(color);owner.setOverlay(overlay);
            owner.setDarkenScreen(dark);owner.setPlayBossMusic(music);owner.setCreateWorldFog(fog);
            for(var id:players){var player=lookup.apply(id);if(player instanceof ServerPlayer online)owner.addPlayer(online);else if(!MatchDamageSources.intentionallyAbsent(id))pending.add(id);}
            owner.setVisible(visible);
        }
        void rebindCurrent(ServerBossEvent owner,Function<UUID,Entity> lookup,Set<UUID> pending){
            // Repeating the link phase must not rewind native progress or resurrect a stopped raid's bar.
            // Raid.updatePlayers owns later audience changes, including arrivals after an offline restore.
            for(var previous:List.copyOf(owner.getPlayers())){
                var current=lookup.apply(previous.getUUID());
                if(current==previous)continue;
                owner.removePlayer(previous);
                if(current instanceof ServerPlayer player)owner.addPlayer(player);
                else if(!MatchDamageSources.intentionallyAbsent(previous.getUUID()))pending.add(previous.getUUID());
            }
        }
    }
    private static void requireIdentity(Object value, Object expected, String label) {
        if (value != expected) throw new IllegalStateException("Native raid container identity changed: " + label);
    }
}
