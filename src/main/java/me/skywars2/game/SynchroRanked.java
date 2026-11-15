package me.skywars2.game;

import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** One match seed; each recipient advances independently through each logical loot table. */
public final class SynchroRanked {
    private static final UUID WORLD=new UUID(0,0);
    private static final ThreadLocal<UUID> RECIPIENT=new ThreadLocal<>();
    private final GameController game;
    private boolean enabled;
    private long seed;
    private final Map<UUID,Map<Identifier,Long>> positions=new HashMap<>();
    private HolderGetter.Provider resolver;
    private final Map<LootTable,Identifier> tableIds=new IdentityHashMap<>();
    record State(boolean enabled,long seed,Map<UUID,Map<Identifier,Long>> positions){}
    SynchroRanked(GameController game){this.game=game;}
    void begin(){begin(game.config().synchroRanked,java.util.concurrent.ThreadLocalRandom.current().nextLong());}
    void begin(boolean enabled,long seed){this.enabled=enabled;this.seed=seed;positions.clear();}
    void clear(){enabled=false;seed=0;positions.clear();}
    public boolean enabled(){return enabled;}
    State checkpoint(){var copy=new HashMap<UUID,Map<Identifier,Long>>();positions.forEach((id,counters)->copy.put(id,Map.copyOf(counters)));return new State(enabled,seed,Map.copyOf(copy));}
    void restore(State state){enabled=state.enabled;seed=state.seed;positions.clear();state.positions.forEach((id,counters)->positions.put(id,new HashMap<>(counters)));}
    long position(UUID player,Identifier pool){return positions.getOrDefault(player,Map.of()).getOrDefault(pool,0L);}
    int counterCount(){return positions.values().stream().mapToInt(Map::size).sum();}
    public static void prepare(LootTable table,LootContext context){
        var state=(SynchroLootContext)context;if(state.skywars2$synchroPrepared())return;
        var game=SkyWars2.game();RandomSource random=null;
        if(game!=null&&game.server()==context.getLevel().getServer())random=game.synchroRanked().random(table,context);
        state.skywars2$synchroPrepare(random);
    }
    private RandomSource random(LootTable table,LootContext context){
        if(!enabled||table==LootTable.EMPTY||!game.server().isSameThread()||game.stage()==GameController.Stage.LOBBY||game.stage()==GameController.Stage.LOADING)return null;
        if(resolver!=context.getResolver()){
            resolver=context.getResolver();tableIds.clear();
            game.server().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElements().forEach(holder->tableIds.put(holder.value(),holder.key().identifier()));
        }
        var id=tableIds.get(table);if(id==null||id.getPath().startsWith("gui/")||id.getPath().contains("/gui/"))return null;
        id=Identifier.parse(LbcAllItemPools.uniformTable(id.toString()));
        UUID recipient=RECIPIENT.get();
        if(recipient==null){
            var self=context.getOptional(LootContextParams.THIS_ENTITY);var killer=context.getOptional(LootContextParams.LAST_DAMAGE_PLAYER);
            var attacker=context.getOptional(LootContextParams.ATTACKING_ENTITY);
            recipient=self instanceof ServerPlayer p?p.getUUID():killer!=null?killer.getUUID():attacker instanceof ServerPlayer p?p.getUUID():null;
            if(recipient==null){var damage=context.getOptional(LootContextParams.DAMAGE_SOURCE);var cause=self!=null&&damage!=null?game.items().indirectKills().policyCause(self,damage):null;if(cause!=null)recipient=cause.player();}
            if(recipient==null)recipient=WORLD;
        }
        var counters=positions.computeIfAbsent(recipient,ignored->new HashMap<>());long index=counters.getOrDefault(id,0L);counters.put(id,index+1);
        long hash=0xcbf29ce484222325L;for(byte b:id.toString().getBytes(StandardCharsets.UTF_8)){hash^=b&255;hash*=0x100000001b3L;}
        return RandomSource.create(mix(seed^hash^mix(index)));
    }
    private static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    public record Scope(UUID previous) implements AutoCloseable {public void close(){if(previous==null)RECIPIENT.remove();else RECIPIENT.set(previous);}}
    private Scope recipient(UUID id){var scope=new Scope(RECIPIENT.get());RECIPIENT.set(id);return scope;}
    /** Forced lazy starter chest evaluation has no player yet: mirror ordinal draws across islands. */
    public Scope island(Vec3 chest,List<MapManager.Spawn> spawns,boolean center){
        if(!enabled)return null;
        int nearest=-1;double distance=Double.POSITIVE_INFINITY;
        if(!center)for(int i=0;i<spawns.size();i++){double d=spawns.get(i).position().distanceToSqr(chest);if(d<distance){distance=d;nearest=i;}}
        return recipient(nearest<0?WORLD:UUID.nameUUIDFromBytes(("synchro-island:"+nearest).getBytes(StandardCharsets.UTF_8)));
    }
}
