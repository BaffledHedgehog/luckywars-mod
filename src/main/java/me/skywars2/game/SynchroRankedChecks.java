package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Real registered loot graphs, independent recipients and a delayed second reader. */
public final class SynchroRankedChecks {
    private SynchroRankedChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Synchro ranked: "+why);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.begin();}catch(Throwable e){running=null;f.close();throw e;}}
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(f.game==game,"same game");if(game.server().getTickCount()-f.began<26)return;f.finish();running=null;f.close();SkyWars2.LOGGER.info("[check] PASS synchro-ranked (all10 native legacy ranked gates, independent delayed native lucky/uniform/TMT/slab sequences, nested single cursors, island chest contents/slots, core rewind, config latch/off/new-match reset)");}
        catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL synchro-ranked",e);}
    }
    private static Object member(Object owner,String name){for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(NoSuchFieldException ignored){}catch(IllegalAccessException e){throw new IllegalStateException(e);}throw new IllegalStateException(name);}
    private static final class Fixture implements AutoCloseable {
        static final long SEED=0x5231a2bc98490L;
        static final List<String> POOLS=List.of("lbc:native_lucky/normal","lbc:native_lucky/neko","lbc:native_lucky/loli","lbc:uniform/vanilla","lbc:uniform/union","lbc:items/tmt","lbc:items/anime_slab");
        final GameController game;final WoolBucketChecks.Fixture f;final SynchroRanked sync;final SynchroRanked.State old;
        final Map<String,List<List<ItemStack>>> expected=new LinkedHashMap<>();final int began;boolean closed;
        Fixture(GameController game){this.game=game;old=game.synchroRanked().checkpoint();f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();sync=game.synchroRanked();began=game.server().getTickCount();}
        LootTable table(String id){var t=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(id)));require(t!=LootTable.EMPTY,"registered native table "+id);return t;}
        LootParams params(ServerPlayer p,String pool){var kind=pool.endsWith("/neko")?LbcLuckyBlocks.Kind.NEKO:pool.endsWith("/loli")?LbcLuckyBlocks.Kind.LOLI:LbcLuckyBlocks.Kind.NORMAL;return new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,p.position()).withParameter(LootContextParams.THIS_ENTITY,p).withParameter(LootContextParams.BLOCK_STATE,LbcLuckyBlocks.legacyState(kind)).withParameter(LootContextParams.TOOL,ItemStack.EMPTY).create(LootContextParamSets.BLOCK);}
        List<ItemStack> roll(ServerPlayer p,String pool,long seed){return List.copyOf(table(pool).getRandomItems(params(p,pool),seed));}
        void same(List<ItemStack> a,List<ItemStack> b,String why){require(a.size()==b.size(),why+" size "+a.size()+"/"+b.size());for(int i=0;i<a.size();i++)require(ItemStack.matches(a.get(i),b.get(i)),why+" slot="+i+" first="+a.get(i)+" second="+b.get(i));}
        @SuppressWarnings("unchecked") LootPoolEntryContainer entry(String name,int... path){var pools=(List<LootPool>)member(table("lbc:"+name),"pools");var entry=pools.getFirst().entries.get(path[0]);for(int i=1;i<path.length;i++)entry=((List<LootPoolEntryContainer>)member(entry,"children")).get(path[i]);return entry;}
        void gate(boolean ranked,String name,int index,boolean admitted,int weight){var ctx=new LootContext.Builder(params(f.owner,name)).create(Optional.empty());var tickets=new ArrayList<LootPoolEntry>();entry(name,index).expand(ctx,tickets::add);require(!tickets.isEmpty()==admitted,"actual ranked="+ranked+" gate "+name+"/"+index);if(admitted)require(tickets.size()==1&&tickets.getFirst().getWeight(0)==weight,"legacy ranked native ticket weight "+name+"/"+index);}
        void rankedGates(){
            for(boolean ranked:new boolean[]{false,true}){
                game.config().ranked=ranked;game.lbc().syncRanked();
                gate(ranked,"barriersnottraps",20,ranked,1);gate(ranked,"barriersnottraps",21,!ranked,10);
                gate(ranked,"traps",21,!ranked,10);gate(ranked,"traps",22,ranked,1);
                gate(ranked,"loliluckyblock",19,!ranked,10);gate(ranked,"loliluckyblock",20,ranked,1);gate(ranked,"loliluckyblock",26,!ranked,10);gate(ranked,"loliluckyblock",27,!ranked,10);
                gate(ranked,"lucky_items_high_quality",2,!ranked,10);
                var tickets=new ArrayList<LootPoolEntry>();entry("lucky_items",0,7,25).expand(new LootContext.Builder(params(f.owner,"lucky_items")).create(Optional.empty()),tickets::add);require(!tickets.isEmpty()==!ranked,"ranked exclusion in nested lucky-items graph");
            }
        }
        void begin(){
            rankedGates();sameInputs();
            boolean before=sync.enabled();game.configureLobbySetting(f.owner,"synchroRanked","true");require(game.config().synchroRanked&&sync.enabled()==before,"persistent setting does not reorder active match");
            sync.begin(true,SEED);
            for(var pool:POOLS){var rows=new ArrayList<List<ItemStack>>();for(int i=0;i<24;i++)rows.add(roll(f.owner,pool,71+i));expected.put(pool,rows);require(sync.position(f.owner.getUUID(),Identifier.parse(pool))==24,"one cursor per outer roll despite native nested tables "+pool);}
            require(sync.position(f.other.getUUID(),Identifier.parse(POOLS.getFirst()))==0,"another player's cursor stays at beginning");
        }
        void finish(){
            sameInputs(); // HP/attack score providers are personal inputs, not loot randomness.
            for(var pool:POOLS){for(int i=0;i<24;i++)same(expected.get(pool).get(i),roll(f.other,pool,981-i),"delayed independent matching native pool "+pool+" #"+i);}
            chestMirrors();
            reusedContext();
            var core=game.checkpointCore();var future=roll(f.owner,POOLS.getFirst(),1);game.server().tickRateManager().setFrozen(true);try{game.restoreCore(core);}finally{game.server().tickRateManager().setFrozen(false);}same(future,roll(f.owner,POOLS.getFirst(),2),"real frozen core rewind restores next drop");
            game.configureLobbySetting(f.owner,"synchroRanked","false");require(!game.config().synchroRanked&&sync.enabled(),"mid-match toggle waits for next match");
            sync.begin();require(!sync.enabled()&&sync.counterCount()==0,"next match applies disabled setting and releases cursors");
            same(roll(f.owner,"lbc:uniform/vanilla",123),roll(f.other,"lbc:uniform/vanilla",123),"disabled mode retains exact native explicit-seed behavior");require(sync.counterCount()==0,"off mode allocates no per-player counters");
            sync.begin(true,SEED);same(expected.get(POOLS.getFirst()).getFirst(),roll(f.owner,POOLS.getFirst(),999),"new match begins at first ticket");
            sync.begin(true,SEED+1);boolean differs=false;for(int i=0;i<24;i++){var row=roll(f.owner,"lbc:uniform/vanilla",i);var old=expected.get("lbc:uniform/vanilla").get(i);if(row.size()!=old.size()||!ItemStack.matches(row.getFirst(),old.getFirst()))differs=true;}require(differs,"new random match seed changes sequence");
            sync.clear();require(!sync.enabled()&&sync.counterCount()==0,"lobby cleanup releases all sequence cursors");
        }
        void sameInputs(){for(var player:List.of(f.owner,f.other)){game.items().mana().score(player,"luck_amount").set(1);game.items().mana().score(player,"hplvl").set(1000);game.items().mana().score(player,"attack_damage").set(1);}}
        void reusedContext(){
            var id="lbc:uniform/vanilla";var context=new LootContext.Builder(params(f.owner,id)).create(Optional.empty());
            for(int i=0;i<3;i++){var actual=new ArrayList<ItemStack>();table(id).getRandomItems(context,actual::add);same(actual,roll(f.other,id,1),"reused root LootContext advances one independent ticket per call");}
        }
        void chestMirrors(){
            var spawns=List.of(new MapManager.Spawn(f.home,0,0),new MapManager.Spawn(f.home.add(40,0,0),0,0));
            for(int index=0;index<3;index++){
                var a=new SimpleContainer(27);var b=new SimpleContainer(27);var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.home).create(LootContextParamSets.CHEST);var table=table("swrg:chests/default_lb");
                try(var scope=sync.island(spawns.getFirst().position(),spawns,false)){table.fill(a,params,99);}
                try(var scope=sync.island(spawns.getLast().position(),spawns,false)){table.fill(b,params,987);}
                MapManager.normalizeLootCounts(a);MapManager.normalizeLootCounts(b);for(int i=0;i<27;i++)require(ItemStack.matches(a.getItem(i),b.getItem(i)),"starter chest actual native contents, counts and shuffled slots match across islands #"+index+" slot"+i);
            }
        }
        @Override public void close(){if(closed)return;closed=true;try{f.close();}finally{sync.restore(old);game.lbc().syncRanked();}}
    }
}
