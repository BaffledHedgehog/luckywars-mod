package me.skywars2.game;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
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
final class MatchRaids {
    private final Map<ServerLevel,Raids> levels=new IdentityHashMap<>();
    private final Map<Raids,Manager> managers=new IdentityHashMap<>();
    private final Map<Raid,Image> raids=new IdentityHashMap<>();
    private final Map<Raider,Body> bodies=new IdentityHashMap<>();
    private final MatchDamageSources sources;
    private boolean linksRestored;
    MatchRaids(GameController game,Collection<Entity> registered,MatchDamageSources sources){
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
    void validate(){levels.forEach((level,owner)->{if(level.getRaids()!=owner)throw new IllegalStateException("Native raid service owner changed");});}
    void prepareFrozen(Collection<Entity> current){
        validate();linksRestored=false;var future=Collections.newSetFromMap(new IdentityHashMap<Raid,Boolean>());
        for(var owner:managers.keySet())future.addAll(MatchRaids.<Raid>map(owner,Raids.class,"raidMap").values());
        for(var entity:current)if(entity instanceof Raider body&&body.getCurrentRaid()!=null)future.add(body.getCurrentRaid());
        for(var raid:future){var bar=(ServerBossEvent)read(raid,Raid.class,"raidEvent");bar.setVisible(false);bar.removeAllPlayers();}
        managers.values().forEach(Manager::restore);
        raids.values().forEach(Image::restoreValues);
    }
    void restoreLinks(Function<UUID,Entity> lookup,Set<UUID> pending){
        validate();
        if(linksRestored){raids.values().forEach(image->image.bar.rebindCurrent(image.event,lookup,pending));return;}
        bodies.values().forEach(Body::restore);
        raids.values().forEach(image->image.restoreLinks(lookup,pending));
        linksRestored=true;
    }
    private static final class Manager {
        final Raids owner;
        final Map<Integer,Raid> original,entries;
        final Map<Field,Object> values;
        Manager(Raids owner){this.owner=owner;original=map(owner,Raids.class,"raidMap");entries=new LinkedHashMap<>(original);values=values(owner,Raids.class,"nextId","tick");}
        void restore(){
            requireIdentity(owner,Raids.class,"raidMap",original);original.clear();original.putAll(entries);restoreValues(owner,values);
            // Even an originally clean image must persist the restored contents over a future disk save.
            owner.setDirty();
        }
    }
    private static final class Body {
        final Raider owner;final Raid raid;final MatchDamageSources.Ref ref;final Map<Field,Object> values;
        Body(Raider owner,MatchDamageSources.Ref ref){this.owner=owner;this.ref=ref;raid=owner.getCurrentRaid();values=values(owner,Raider.class,"wave","canJoinRaid","ticksOutsideRaid");}
        void restore(){
            restoreValues(owner,values);
            boolean omitted=ref.historical()==null&&MatchDamageSources.intentionallyAbsent(ref.id());
            owner.setCurrentRaid(omitted?null:raid);
        }
    }
    private record Member(Raider original,MatchDamageSources.Ref ref,float health) { }
    private record Group(Set<Raider> original,List<Member> members) { }
    private final class Image {
        final Raid owner;
        final Map<Field,Object> values;
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
            values=values(owner,Raid.class,"ticksActive","center","started","totalHealth","raidOmenLevel","active","groupsSpawned",
                    "postRaidTicks","raidCooldownTicks","numGroups","status","celebrationTicks","waveSpawnPos");
            for(var value:values.values()){
                if(value instanceof BlockPos.MutableBlockPos position)positions.put(position,position.immutable());
                if(value instanceof Optional<?> optional&&optional.orElse(null) instanceof BlockPos.MutableBlockPos position)positions.put(position,position.immutable());
            }
            leaderMap=map(owner,Raid.class,"groupToLeaderMap");leaderMap.forEach((wave,body)->leaders.put(wave,member(body)));
            groupMap=map(owner,Raid.class,"groupRaiderMap");groupMap.forEach((wave,members)->groups.put(wave,new Group(members,members.stream().map(this::member).toList())));
            heroes=set(owner,Raid.class,"heroesOfTheVillage");heroValues=Set.copyOf(heroes);
            random=(RandomSource)read(owner,Raid.class,"random");randomState=MatchRandomState.capture(random);
            event=(ServerBossEvent)read(owner,Raid.class,"raidEvent");bar=new Bar(event);
        }
        Member member(Raider body){return new Member(body,sources.dependencyRef(body),body.getHealth());}
        void restoreValues(){
            positions.forEach(BlockPos.MutableBlockPos::set);MatchRaids.restoreValues(owner,values);
            requireIdentity(owner,Raid.class,"random",random);randomState.restore(random);
            requireIdentity(owner,Raid.class,"raidEvent",event);
            requireIdentity(owner,Raid.class,"heroesOfTheVillage",heroes);heroes.clear();heroes.addAll(heroValues);
        }
        void restoreLinks(Function<UUID,Entity> lookup,Set<UUID> pending){
            requireIdentity(owner,Raid.class,"groupToLeaderMap",leaderMap);leaderMap.clear();
            leaders.forEach((wave,member)->{var body=resolve(member,lookup,pending);if(body!=null)leaderMap.put(wave,body);});
            requireIdentity(owner,Raid.class,"groupRaiderMap",groupMap);groupMap.clear();float removedHealth=0;
            for(var entry:groups.entrySet()){
                var group=entry.getValue();group.original.clear();
                for(var member:group.members){
                    var body=resolve(member,lookup,pending);
                    if(body!=null)group.original.add(body);
                    else if(MatchDamageSources.intentionallyAbsent(member.ref.id()))removedHealth+=member.health;
                }
                groupMap.put(entry.getKey(),group.original);
            }
            write(owner,Raid.class,"totalHealth",(Float)values.get(field(Raid.class,"totalHealth"))-removedHealth);
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
    private static Map<Field,Object> values(Object owner,Class<?> type,String... names){
        var values=new LinkedHashMap<Field,Object>();
        for(var name:names){var field=field(type,name);Object value=read(owner,type,name);
            if(!field.getType().isPrimitive()&&!field.getType().isEnum()&&!(value instanceof BlockPos)&&!(value instanceof Optional<?>))throw new IllegalStateException("Review native raid value: "+field);
            values.put(field,value);
        }return values;
    }
    private static void restoreValues(Object owner,Map<Field,Object> values){values.forEach((field,value)->{
        if(Modifier.isFinal(field.getModifiers())){if(!Objects.equals(read(owner,field.getDeclaringClass(),field.getName()),value))throw new IllegalStateException("Final native raid value changed: "+field);}
        else write(owner,field.getDeclaringClass(),field.getName(),value);
    });}
    @SuppressWarnings("unchecked") private static <T> Map<Integer,T> map(Object owner,Class<?> type,String name){return (Map<Integer,T>)read(owner,type,name);}
    @SuppressWarnings("unchecked") private static <T> Set<T> set(Object owner,Class<?> type,String name){return (Set<T>)read(owner,type,name);}
    private static void requireIdentity(Object owner,Class<?> type,String name,Object expected){if(read(owner,type,name)!=expected)throw new IllegalStateException("Native raid container identity changed: "+name);}
    private static Field field(Class<?> type,String name){try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(ReflectiveOperationException error){throw new IllegalStateException("Review native raid field "+type.getName()+"."+name,error);}}
    private static Object read(Object owner,Class<?> type,String name){try{return field(type,name).get(owner);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static void write(Object owner,Class<?> type,String name,Object value){try{field(type,name).set(owner,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
}
