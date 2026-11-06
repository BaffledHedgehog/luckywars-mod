package me.skywars2.game;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Native criterion admission and inventory-event cache for the remaining source trinkets. */
public final class LbcReactiveTrinkets {
    private static final Item[] OFFENSE={LbcTrinkets.DROJJI,LbcTrinkets.MANDOAHUET,LbcTrinkets.PSYCHO_SOUL,LbcTrinkets.INTERCAL,LbcTrinkets.SNOW_GEM};
    private static final String[] OFFENSE_IDS={"drojji","mandoahuet","psycho_soul","intercal","snow_pearl"};
    private static final Item[] DEFENSE={LbcTrinkets.BANANGAY,LbcTrinkets.HEDGEHOG_GAUNTLET,LbcTrinkets.ICE_GEM,LbcTrinkets.GNEISS_STONE};
    private static final String[] DEFENSE_IDS={"banangay","hedgehog_gauntlet","ice_gem","gneiss_stone"};
    private static final Item[] PERIODIC={LbcTrinkets.TWILIGHT_SPARKLE,LbcTrinkets.POLY_GEM,LbcTrinkets.GLASS_SHARD};
    private static final String[] PERIODIC_IDS={"twilight_sparkle","poly_gem","sunprism"};
    private final GameController game;
    private final LbcReactiveOffense offense;
    private final LbcReactiveDefense defense;
    private final LinkedHashSet<UUID> dirty=new LinkedHashSet<>();
    private final LinkedHashMap<UUID,Periodic> periodic=new LinkedHashMap<>();
    private final Map<UUID,ArrayDeque<Incoming>> attempts=new HashMap<>();
    private boolean restoring;
    private record Incoming(int banana,int gauntlet,int ice,int stone){}
    private record Periodic(int horse,int poly,int prism,long y,int phase){}
    public LbcReactiveTrinkets(GameController game){
        this.game=game;offense=new LbcReactiveOffense(game);defense=new LbcReactiveDefense(game,this::admission);
    }
    public LbcReactiveOffense offense(){return offense;}
    public LbcReactiveDefense defense(){return defense;}
    private boolean admission(ServerPlayer p){
        return !restoring&&game.playing()&&game.config().lbcTrinketLimit>0&&p.isAlive()&&!p.isRemoved()
                &&LbcDimensionItems.ordinary(p)&&game.canUseGameplayItems(p)&&!game.isObserver(p);
    }
    private boolean paused(ServerPlayer p){return game.items().timeStop().stopped(p.level())
            ||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p);}
    static int count(ServerPlayer player,Item item,String identity){
        if(!LbcDimensionItems.ordinary(player))return 0;
        return player.getInventory().clearOrCountMatchingItems(s->matches(s,item,identity),true,0,player.inventoryMenu.getCraftSlots());
    }
    private static boolean matches(ItemStack s,Item item,String identity){
        return !s.isEmpty()&&(s.is(item)||LbcTmtInventory.string(s,"trinket",identity));
    }
    private static int[] counts(ServerPlayer p,Item[] types,String[] ids){
        int[] result=new int[types.length];
        p.getInventory().clearOrCountMatchingItems(s->{
            if(s.isEmpty())return false;
            String id=ItemData.stringValue(s,"trinket","");
            var privateIds=LbcTmtItems.matches(s)?LbcTmtInventory.strings(s,"trinket"):null;
            for(int i=0;i<types.length;i++)if(s.is(types[i])||id.equals(ids[i])||privateIds!=null&&privateIds.contains(ids[i]))result[i]=add(result[i],s.getCount());
            return false;
        },true,0,p.inventoryMenu.getCraftSlots());
        return result;
    }
    private static Incoming incomingCounts(ServerPlayer p){var n=counts(p,DEFENSE,DEFENSE_IDS);return new Incoming(n[0],n[1],n[2],n[3]);}
    public boolean withIncomingAttempt(ServerPlayer p,BooleanSupplier action){
        if(!admission(p)||paused(p))return action.getAsBoolean();
        var stack=attempts.computeIfAbsent(p.getUUID(),id->new ArrayDeque<>());
        stack.push(incomingCounts(p));
        try{return action.getAsBoolean();}finally{stack.pop();if(stack.isEmpty())attempts.remove(p.getUUID());}
    }
    public void incoming(ServerPlayer p){
        if(restoring||!game.playing()||game.config().lbcTrinketLimit==0||!LbcDimensionItems.ordinary(p)||paused(p))return;
        var stack=attempts.get(p.getUUID());
        var frame=stack==null||stack.isEmpty()?null:stack.peek();
        if(frame==null){if(!admission(p))return;frame=incomingCounts(p);}
        // Counts belong to the native pre-hit attempt; a fatal hit must never revive the owner.
        defense.received(p,frame.banana,frame.gauntlet);
        game.items().trinkets().incoming(p,frame.ice,frame.stone);
    }
    public void outgoing(ServerPlayer p,LivingEntity actual,DamageSource source,boolean blocked){
        if(!admission(p)||paused(p)||blocked||LbcDamage.isArrowDamage(source))return;
        var n=counts(p,OFFENSE,OFFENSE_IDS);offense.outgoing(p,actual,source,false,n[0],n[1],n[2],
                n[3]>0||n[4]>0?()->game.items().trinkets().outgoing(p,source,n[3],n[4]):null);
    }
    public void inventoryChanged(ServerPlayer p){if(!restoring)dirty.add(p.getUUID());}
    private void refresh(ServerPlayer p){
        // One borrowed scalar read per stack, only after inventory events; never copy CUSTOM_DATA.
        var n=counts(p,PERIODIC,PERIODIC_IDS);
        var old=periodic.get(p.getUUID());
        if(n[0]+(long)n[1]+n[2]==0)periodic.remove(p.getUUID());
        else periodic.put(p.getUUID(),new Periodic(n[0],n[1],n[2],old==null?scaledY(p):old.y,old==null?0:old.phase));
    }
    private static int add(int a,int b){return (int)Math.min(Integer.MAX_VALUE,(long)a+b);}
    private static long scaledY(ServerPlayer p){return (long)Math.floor(p.getY()*100);}
    public void tick(){
        if(restoring||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        int budget=Math.min(32,dirty.size());
        while(budget-->0){var id=dirty.removeFirst();var p=game.server().getPlayerList().getPlayer(id);
            if(p==null){periodic.remove(id);continue;}if(paused(p)){dirty.add(id);continue;}refresh(p);}
        // Only enrolled owners are visited. Snapshot prevents callbacks from invalidating traversal.
        for(var id:List.copyOf(periodic.keySet())){
            var p=game.server().getPlayerList().getPlayer(id);var value=periodic.get(id);
            if(p==null){periodic.remove(id);continue;}if(value==null||!admission(p)||paused(p))continue;
            long y=scaledY(p),delta=y-value.y;int phase=(value.phase+1)%20;
            periodic.put(id,new Periodic(value.horse,value.poly,value.prism,y,phase));
            if(game.items().magic().suppressed(p))continue;
            if(phase==0&&value.horse>0&&!p.onGround())defense.fall(p,value.horse,(int)Math.clamp(delta,Integer.MIN_VALUE,Integer.MAX_VALUE));
            if(value.poly>0&&p.getRandom().nextFloat()<(.001+.001*value.poly))game.items().sheep().trinket(p);
            if(value.prism>0&&p.getRandom().nextFloat()<(.0004+.0001*value.prism))game.items().prism().trinket(p);
        }
        offense.tick();defense.tick();
    }
    public void loaded(Entity e){defense.loaded(e);if(e instanceof ServerPlayer p)inventoryChanged(p);}
    public void unloaded(Entity e){defense.unloaded(e);}
    public void forget(ServerPlayer p){if(restoring||MatchPlayerCheckpoint.restoring(p))return;dirty.remove(p.getUUID());periodic.remove(p.getUUID());offense.forget(p);defense.forget(p);}
    public void stop(){dirty.clear();periodic.clear();attempts.clear();offense.stop();defense.stop();}
    void prepareFrozen(){restoring=true;dirty.clear();periodic.clear();}
    void publishFrozen(){restoring=false;}
    Checkpoint checkpoint(){if(!attempts.isEmpty())throw new IllegalStateException("Capture trinkets outside native damage callbacks");return new Checkpoint();}
    final class Checkpoint{
        private final Map<UUID,Periodic> saved=new LinkedHashMap<>(periodic);
        private final List<UUID> pending=List.copyOf(dirty);
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){return new MatchFieldCheckpoint.Plan(Set.of(),()->{
            periodic.clear();periodic.putAll(saved);dirty.clear();dirty.addAll(pending);
        });}
    }
}
