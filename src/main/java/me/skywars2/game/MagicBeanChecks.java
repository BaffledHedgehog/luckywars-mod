package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Source commands execute unchanged; only the bounded fixture controls clocks and native RNG draws. */
public final class MagicBeanChecks {
    private MagicBeanChecks(){}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException("Magic Bean: "+detail);}
    private static Field field(Object object,String name){for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object read(Object object,String name){try{return field(object,name).get(object);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object object,String name,Object value){try{field(object,name).set(object,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static final class Draws implements AutoCloseable {
        final ServerLevel level;final Object previous;final float[] values;int used,total;
        Draws(ServerLevel level,float...values){this.level=level;this.values=values;previous=read(level,"random");
            set(level,"random",Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                if(method.getName().equals("nextFloat")){total++;if(used<this.values.length)return this.values[used++];}
                try{return method.invoke(previous,args);}catch(InvocationTargetException e){throw e.getCause();}
            }));
        }
        @Override public void close(){set(level,"random",previous);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner;final ServerLevel level;final LbcMagicBean service;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final LbcIndirectKills.TerrainProvenance provenance;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final List<Cow> cows=new ArrayList<>();
        final List<LbcStonekivi.Work> previousWork;final Map<String,Object> counters=new LinkedHashMap<>();
        final Vec3 origin=new Vec3(-.25,245.75,-.75);final BlockPos center=BlockPos.containing(origin);ItemStack leaf;Marker zone;
        @SuppressWarnings("unchecked") Fixture(GameController game){
            this.game=game;service=game.items().magicBean();require(service.count()==0,"exclusive empty source service");
            base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;level=owner.level();
            try{
            base.initialize();
            require(!game.items().timeStop().stopped(level),"no ambient stop");
            level.getAllEntities().forEach(before::add);previousWork=new ArrayList<>((Collection<LbcStonekivi.Work>)read(game.items().stonekivi(),"work"));
            for(var key:List.of("lastTick","steps","completions"))counters.put(key,read(service,key));
            // Two separated height bands cover every source write and leave unrelated terrain untouched.
            for(int[] band:new int[][]{{244,248},{298,303}})for(var pos:BlockPos.betweenClosed(new BlockPos(-15,band[0],-15),new BlockPos(14,band[1],14))){
                require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir()&&level.getBlockEntity(pos)==null,"resident air source volume "+pos);
                terrain.put(pos.immutable(),game.maps().snapshot(level,pos));
            }
            provenance=game.items().indirectKills().captureTerrain(level,terrain.keySet());
            var loot=game.lbc().rollTable(level,origin,owner,"lbc:drops/magic_bean");
            require(loot.size()==1,"actual original Magic Bean source leaf result count="+loot.size()+" results="+loot);
            leaf=loot.getFirst();
            require(LbcMagicBean.recognized(leaf),"actual original Magic Bean source leaf item="+leaf+" custom_data="+LbcFartus.data(leaf)+" components="+leaf.getComponents());
            require(leaf.is(Items.REPEATING_COMMAND_BLOCK)&&LbcFartus.data(leaf).getIntOr("drop",0)==1,"raw source carrier/drop flag");
            require(Objects.equals(leaf.get(DataComponents.ITEM_MODEL),net.minecraft.resources.Identifier.parse("lbc:drop_not_trap"))&&Objects.equals(leaf.get(DataComponents.TOOLTIP_STYLE),net.minecraft.resources.Identifier.parse("lbc:tier_u")),"source model and tierU");
            }catch(RuntimeException|Error failure){base.close();throw failure;}
        }
        @SuppressWarnings("unchecked") ArrayDeque<LbcStonekivi.Work> queue(){return (ArrayDeque<LbcStonekivi.Work>)read(game.items().stonekivi(),"work");}
        @SuppressWarnings("unchecked") Object bean(Marker marker){return ((Map<Marker,Object>)read(service,"beans")).get(marker);}
        void allowStep(Marker marker){int now=game.server().getTickCount();set(service,"lastTick",now-1);set(bean(marker),"lastStepTick",now-1);}
        LbcStonekivi.Work enqueue(Marker marker){allowStep(marker);service.tick();var pending=read(bean(marker),"pending");require(pending instanceof LbcStonekivi.Work,"source enqueues actual shared terrain work");return (LbcStonekivi.Work)pending;}
        void execute(LbcStonekivi.Work work){require(work.ready(),"complete native source footprint resident");require(queue().remove(work),"work is owned by actual common scheduler");require(work.step(),"one complete source function per work step");}
        Marker spawn(Vec3 at){
            var token=new ItemEntity(level,at.x,at.y,at.z,leaf.copyWithCount(2));token.setThrower(owner);token.setNoGravity(true);token.setOnGround(true);token.setDeltaMovement(Vec3.ZERO);game.items().markCurrentItemEpoch(token);
            require(level.addFreshEntity(token),"native source token publication");
            require(service.ready(token)&&game.items().specialDrops().spawn(token),"actual shared SpecialDrops admission and dispatch");
            require(token.getItem().getCount()==1&&service.count()==1,"exact one token debit and one marker");token.discard();
            var marker=service.snapshot().getFirst();require(marker.position().equals(at)&&marker.entityTags().containsAll(Set.of("magic","magicbeaner")),"exact fractional source marker position/tags");
            require(service.owner(marker)==owner&&LbcIndirectKills.playerSource(marker)==owner,"shared owner bridge retains causal wearer");
            var cause=(LbcIndirectKills.Owner)read(bean(marker),"cause");require(cause!=null&&cause.player().equals(owner.getUUID())&&cause.friendlyFire(),"unfiltered growth keeps WILD world author");
            return marker;
        }
        void clear(){service.stop();queue().removeIf(work->!previousWork.contains(work));for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}cows.clear();}
        void delayAndMoves(){
            var marker=spawn(origin);long baseline=service.steps();set(service,"lastTick",game.server().getTickCount()-1);service.tick();
            require(read(bean(marker),"pending")==null&&service.steps()==baseline,"new marker cannot grow in its birth server tick");clear();
            for(int mask=0;mask<32;mask++){
                marker=spawn(origin);var work=enqueue(marker);float[] draws=new float[6];for(int bit=0;bit<5;bit++)draws[bit]=(mask&(1<<bit))!=0?.25f:.75f;draws[5]=.5f;
                try(var script=new Draws(level,draws)){
                    execute(work);require(script.used==6&&script.total==6,"five independent50percent draws plus literal endpoint draw "+mask);
                    int x=center.getX()+((mask&1)!=0?1:0)-((mask&2)!=0?1:0),z=center.getZ()+((mask&4)!=0?1:0)-((mask&8)!=0?1:0),y=center.getY()+((mask&16)!=0?1:0);
                    require(marker.position().equals(new Vec3(x,y,z))&&!marker.isRemoved(),"all32 actual source move combinations including negative floors "+mask);
                    for(var cell:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))require(level.getBlockState(cell).is(Blocks.OAK_WOOD),"complete source3x3 oak wood layer");
                    long steps=service.steps();var position=marker.position();service.tick();require(work.step()&&service.steps()==steps&&marker.position().equals(position)&&script.used==6,"same actual tick/canceled work cannot duplicate growth");
                }clear();
            }
        }
        void lazyChest(BlockPos pos){
            require(level.getBlockEntity(pos) instanceof ChestBlockEntity,"native endpoint chest "+pos);var chest=(ChestBlockEntity)level.getBlockEntity(pos);
            var data=chest.saveWithFullMetadata(level.registryAccess());require(data.getStringOr("LootTable","").equals("lbc:lucky_items_1"),"complete original lazy table identity");
            require(!data.contains("Items")||data.getListOrEmpty("Items").isEmpty(),"endpoint reward remains unrolled");
        }
        boolean glass(int x,int z){x=Math.abs(x);z=Math.abs(z);return x<=8&&z<=8||x<=6&&z<=10||x<=4&&z<=12||x<=10&&z<=6||x<=12&&z<=4;}
        void endpoints(){
            var high=new Vec3(-.25,299.75,-.75);var pre=new BlockPos(0,300,0);
            var marker=spawn(high);var work=enqueue(marker);long count=service.completions();
            try(var script=new Draws(level,.25f,.75f,.25f,.75f,.25f,.5f)){execute(work);require(script.used==6,"native height endpoint consumes original six draws");}
            require(marker.isRemoved()&&service.count()==0&&service.completions()==count+1,"height threshold finishes exactly once when random false");
            for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)require(glass(x,z)?level.getBlockState(pre.offset(x,0,z)).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.WHITE)):level.getBlockState(pre.offset(x,0,z)).isAir(),"five exact source rectangles and untouched corners");
            lazyChest(pre.above());require(level.getBlockState(new BlockPos(-1,300,-1)).is(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.WHITE)),"height platform uses moved position");clear();restoreBlocks();
            marker=spawn(high);work=enqueue(marker);count=service.completions();
            try(var script=new Draws(level,.25f,.75f,.25f,.75f,.25f,.0079f)){execute(work);require(script.used==6,"literal.008 random predicate boundary");}
            require(marker.isRemoved()&&service.completions()==count+1,"coincident conditions create only one platform and reward");lazyChest(new BlockPos(0,301,0));clear();restoreBlocks();
            // The strict boundary remains .008 per growth step.
            marker=spawn(origin);work=enqueue(marker);try(var script=new Draws(level,.75f,.75f,.75f,.75f,.75f,.008f)){execute(work);require(!marker.isRemoved()&&script.used==6,"chance exactly.008 does not match strict random condition");}clear();restoreBlocks();
        }
        void causality(){
            var marker=spawn(origin);var cow=new Cow(EntityTypes.COW,level);cow.setPos(center.getX()+.5,center.getY()-1,center.getZ()+.5);cow.setNoAi(true);cow.setNoGravity(true);require(level.addFreshEntity(cow),"native terrain suffocation witness");cows.add(cow);
            var work=enqueue(marker);try(var script=new Draws(level,.75f,.75f,.75f,.75f,.75f,.5f)){execute(work);}
            require(level.getBlockState(BlockPos.containing(cow.getEyePosition())).is(Blocks.OAK_WOOD),"native witness eye is inside own bean wood");
            require(cow.hurtServer(level,cow.damageSources().inWall(),1)&&cow.getKillCredit()==owner,"actual native suffocation inherits bean wearer");clear();restoreBlocks();
        }
        void zone(Vec3 at,boolean tech){zone=new Marker(EntityTypes.MARKER,level);zone.setPos(at);zone.entityTags().add(tech?"stopper":"stopper_magic");require(level.addFreshEntity(zone),"actual jammer marker");game.items().magic().loaded(zone);require(tech?game.items().magic().mechanismSuppressedAt(level,at):game.items().magic().suppressedAt(level,at),"actual shared suppression predicate");}
        void removeZone(){if(zone!=null){game.items().magic().unloaded(zone);zone.discard();zone=null;}}
        void guards(){
            var marker=spawn(origin);zone(origin,true);try{var work=enqueue(marker);try(var script=new Draws(level,.75f,.75f,.75f,.75f,.75f,.5f)){execute(work);require(!marker.isRemoved()&&script.used==6,"TECH does not suppress source magical bean");}}finally{removeZone();clear();restoreBlocks();}
            marker=spawn(origin);zone(origin,false);try{allowStep(marker);try(var script=new Draws(level,.25f)){service.tick();require(marker.isRemoved()&&service.count()==0&&script.used==0,"MAG deletes source marker without growth or RNG");}}finally{removeZone();clear();}
            marker=spawn(origin);long steps=service.steps();allowStep(marker);game.items().timeStop().begin(level,40);
            try(var script=new Draws(level,.25f)){service.tick();require(service.count()==1&&!marker.isRemoved()&&service.steps()==steps&&script.used==0,"real dimension Time Stop retains marker without growth");}finally{game.items().timeStop().end(level);clear();}
            marker=spawn(origin);var work=enqueue(marker);var position=marker.position();service.unloaded(marker);
            try(var script=new Draws(level,.25f)){require(work.ready()&&work.step()&&script.used==0&&marker.position().equals(position),"unload cancels already queued source work");}queue().remove(work);
            service.loaded(marker);require(service.count()==1&&service.owner(marker)==owner,"same-epoch native position/tags rebuild ownership");
            work=enqueue(marker);try(var script=new Draws(level,.75f,.75f,.75f,.75f,.75f,.5f)){execute(work);require(script.used==6,"reloaded marker resumes native source function");}clear();restoreBlocks();
            marker=spawn(origin);work=enqueue(marker);service.stop();steps=service.steps();try(var script=new Draws(level,.25f)){require(work.ready()&&work.step()&&marker.isRemoved()&&service.steps()==steps&&script.used==0,"stop cancels pending geometry without future platform");}queue().remove(work);
            marker=spawn(origin);marker.entityTags().removeIf(t->t.startsWith("skywars2.items."));marker.entityTags().add("skywars2.items.magic-bean-stale");set(service,"lastTick",game.server().getTickCount()-1);service.tick();require(marker.isRemoved()&&service.count()==0,"stale match marker cannot continue growth");
        }
        void restoreBlocks(){
            for(var pos:terrain.keySet())if(level.getBlockEntity(pos) instanceof ChestBlockEntity chest){chest.setLootTable(null);chest.clearContent();}
            terrain.forEach((pos,state)->game.maps().restoreSnapshot(level,pos,state));
        }
        @Override public void close(){try{removeZone();game.items().timeStop().end(level);clear();restoreBlocks();game.items().indirectKills().restoreTerrain(provenance);
                var spawned=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity)&&!(entity instanceof ServerPlayer))spawned.add(entity);spawned.forEach(Entity::discard);
            }finally{queue().clear();queue().addAll(previousWork);counters.forEach((key,value)->set(service,key,value));base.close();}}
    }
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.delayAndMoves();fixture.endpoints();fixture.causality();fixture.guards();}
        SkyWars2.LOGGER.info("[check] PASS magic-bean (actual source leaf/shared single-debit dispatch; five independent RNG draws/all32 moves; strict wood/glass shapes, literal.008/single moved endpoint, lazy full loot; native suffocation author, MAG/TECH/Time Stop, stale/unload/stop cancellation)");
        SkyWars2.LOGGER.info("[check] magic-bean fixture uses scoped clock/RNG controls only; natural scheduling latency and unmodified stochastic duration are not measured");
    }
}
