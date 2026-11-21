package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Real scheduled input, shared work, source copies and a nonempty whole-coordinator restore. */
public final class ExpansionSkillChecks {
    private ExpansionSkillChecks(){}
    private static Run running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Expansion: "+why);}
    public static void start(GameController game){
        require(running==null,"exclusive scheduled fixture");var r=new Run(game);
        try{r.init();running=r;}catch(RuntimeException|Error failure){r.close();throw failure;}
        SkyWars2.LOGGER.info("[check] START expansion-skill (registered native skill ticks)");
    }
    public static void tick(GameController game){
        if(running==null)return;var r=running;
        try{if(r.step()){running=null;r.close();SkyWars2.LOGGER.info("[check] PASS expansion-skill (source charge, cooldown, copying, MAG, TimeStop, rewind, native build ceiling and cold chunk scheduling)");}}
        catch(RuntimeException|Error failure){running=null;try{r.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL expansion-skill",failure);}
    }
    private static final class Run implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture f;final LbcExpansionSkill skill;
        RegenerationBeltChecks.ScheduledClient client,audience;
        final Map<BlockPos,MapManager.BlockSnapshot> extra=new LinkedHashMap<>();
        final List<net.minecraft.world.ticks.ScheduledTick<Block>> blockTicks=new ArrayList<>();
        final List<net.minecraft.world.ticks.ScheduledTick<net.minecraft.world.level.material.Fluid>> fluidTicks=new ArrayList<>();
        final Set<Long> savedChunks=new HashSet<>();
        int stage,at,pausedScore,pausedFrontier,cooldown,kind;boolean closed;
        BlockPos origin,beacon;
        ColdFrontier coldFrontier;
        Run(GameController game){this.game=game;f=new ChefChecks.Fixture(game);skill=game.items().expansionSkill();}
        void init(){
            f.init();client=new RegenerationBeltChecks.ScheduledClient(game,f.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,f.other);
            skill.clear(f.owner);origin=f.center.below();beacon=f.center.offset(-6,0,-6);
            f.move(f.owner,f.at);f.move(f.other,f.at.add(12,0,12));
            game.config().lbcSkills=true;game.config().ranked=true;game.players().get(f.owner.getUUID()).lbcSkill=24;
            var entries=skill.paletteForCheck();require(entries.size()>=1106,"all1106 source slots retained");
            var source=entries.subList(0,1106);
            require(source.stream().filter(s->s.is(Blocks.TEST_BLOCK)).count()==2
                    &&source.stream().filter(s->s.is(Blocks.TEST_INSTANCE_BLOCK)).count()==2,"source duplicate slot weighting retained");
            var present=new HashSet<>(entries);
            for(var holder:BuiltInRegistries.BLOCK.listElements().toList())if(holder.key().identifier().getNamespace().equals("minecraft"))
                require(present.contains(holder.value().defaultBlockState()),"all current vanilla default states included: "+holder.key().identifier());
            require(entries.stream().allMatch(s->BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals("minecraft")),"new native custom blocks excluded from random pool");
            var added=entries.subList(1106,entries.size()).stream().map(s->BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString()).toList();
            SkyWars2.LOGGER.info("[check] Expansion palette sourceSlots=1106 total={} addedCount={} addedIds={}",entries.size(),added.size(),added);
            f.block(beacon,LbcMagic.MAGIC_BEACON_BLOCK);require(game.items().magic().suppressed(f.owner),"actual loaded MAG charge control");
            f.owner.setShiftKeyDown(true);at=game.server().getTickCount();
        }
        void chest(){
            for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})f.block(origin.offset(d[0],0,d[1]),Blocks.AIR);
            f.block(origin.below(),Blocks.AIR);f.block(origin.east(),Blocks.OBSIDIAN);f.block(origin,Blocks.CHEST);
            var chest=(ChestBlockEntity)f.owner.level().getBlockEntity(origin);
            var named=new ItemStack(Items.CHEST);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Expansion metadata fixture"));chest.applyComponentsFromItemStack(named);chest.setItem(0,new ItemStack(Items.DIAMOND,64));chest.setChanged();
        }
        void checkChest(BlockPos pos){
            require(f.owner.level().getBlockEntity(pos) instanceof ChestBlockEntity,"native copied chest at "+pos);
            var chest=(ChestBlockEntity)f.owner.level().getBlockEntity(pos);
            require(chest.isEmpty()&&chest.getName().getString().equals("Expansion metadata fixture"),"inventory removed but native custom name preserved at "+pos);
        }
        /** Snapshot only accepted frontier neighborhoods, before the next registered terrain tick.
         * This is test cleanup, never a production scan or an artificial propagation boundary. */
        void protect(){
            for(var pos:skill.frontierPositionsForCheck(f.owner))for(var at:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,1,2))){
                if(f.blocks.containsKey(at)||extra.containsKey(at))continue;
                require(Math.abs(at.getX()-origin.getX())<128&&Math.abs(at.getZ()-origin.getZ())<128,"natural stochastic event stays inside bounded qualification fixture");
                if(f.owner.level().isInsideBuildHeight(at.getY()))rememberExtra(at);
            }
        }
        void rememberExtra(BlockPos pos){
            var level=f.owner.level();
            var chunk=level.getChunk(pos);
            if(savedChunks.add(chunk.getPos().pack())){
                ((net.minecraft.world.ticks.LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().forEach(blockTicks::add);
                ((net.minecraft.world.ticks.LevelChunkTicks<net.minecraft.world.level.material.Fluid>)chunk.getFluidTicks()).getAll().forEach(fluidTicks::add);
            }
            extra.putIfAbsent(pos.immutable(),game.maps().snapshot(level,pos));
        }
        boolean step(){
            int elapsed=game.server().getTickCount()-at;
            if(game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            require(elapsed<1800,"bounded scheduled stage "+stage);protect();
            if(stage==0){
                require(!skill.active(f.owner),"partial charge never activates");if(elapsed<20)return false;
                require(skill.score(f.owner)==0,"MAG blocks owner charging without activating the territory");
                f.owner.setShiftKeyDown(false);f.block(beacon,Blocks.AIR);stage=1;at=game.server().getTickCount();return false;
            }
            if(stage==1){
                if(elapsed<2)return false;require(skill.score(f.owner)==0,"release resets partial charge");chest();
                f.block(beacon,LbcMagic.MECHANISM_BEACON_BLOCK);
                require(game.items().magic().mechanismSuppressed(f.owner)&&!game.items().magic().suppressed(f.owner),"actual TECH positive control, MAG absent");
                f.owner.setShiftKeyDown(true);stage=2;at=game.server().getTickCount();return false;
            }
            if(stage==2){
                if(!skill.active(f.owner)){require(elapsed<=61,"source60tick activation");return false;}
                require(elapsed>=59&&elapsed<=61&&skill.score(f.owner)==60,"actual source60 threshold");
                checkChest(origin);checkChest(origin.west());checkChest(origin.north());checkChest(origin.south());
                require(f.owner.level().getBlockState(origin.east()).is(Blocks.OBSIDIAN),"initial copy cannot replace non-air");
                require(skill.frontier(f.owner)==4,"exact four initial directional main seeds");
                pausedScore=skill.score(f.owner);pausedFrontier=skill.frontier(f.owner);
                game.items().timeStop().begin(f.owner.level(),1000);stage=3;at=game.server().getTickCount();return false;
            }
            if(stage==3){
                require(skill.score(f.owner)==pausedScore&&skill.frontier(f.owner)==pausedFrontier,"TimeStop freezes source input and nonempty virtual frontier");
                if(elapsed<5)return false;
                require(game.vanity().requestCapture(f.owner,false),"whole coordinator capture with actual nonempty frontier and container copies");stage=4;at=game.server().getTickCount();return false;
            }
            if(stage==4){
                if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                skill.clear(f.owner);require(skill.frontier(f.owner)==0,"future frontier actually removed");
                require(game.vanity().requestRestoreForCheck(),"whole coordinator restore accepted");stage=5;at=game.server().getTickCount();return false;
            }
            if(stage==5){
                if(game.vanity().busy())return false;
                require(skill.score(f.owner)==pausedScore&&skill.frontier(f.owner)==pausedFrontier,"coordinator restores exact nonempty frontier and RNG clock");checkChest(origin.west());
                client.acknowledge();audience.acknowledge();game.vanity().stop();game.items().timeStop().end(f.owner.level());stage=6;at=game.server().getTickCount();return false;
            }
            if(stage==6){
                if(skill.score(f.owner)>=62)require(!f.owner.level().getBlockState(origin.below()).isAir(),"main downward copy is unconditional regardless10percent downward marker");
                if(skill.active(f.owner))return false;
                cooldown=skill.score(f.owner);require(cooldown>=80&&skill.frontier(f.owner)==0,"actual stochastic source end starts at80 and clears all owned frontier");
                stage=7;at=game.server().getTickCount();return false;
            }
            if(stage==7){
                require(skill.score(f.owner)==cooldown,"holding sneak freezes source cooldown");if(elapsed<5)return false;
                f.owner.setShiftKeyDown(false);stage=8;at=game.server().getTickCount();return false;
            }
            if(stage==8){
                if(skill.score(f.owner)==cooldown){require(elapsed<100,"50percent release cooldown eventually advances");return false;}
                require(skill.score(f.owner)<cooldown,"non-sneak cooldown debit");kind=0;beginLucky();return false;
            }
            if(stage==9){
                if(!skill.active(f.owner)){require(elapsed<=61,"native lucky case real60tick charge");return false;}
                if(waitingForInitial(elapsed))return false;
                var expected=switch(kind){case 0->Blocks.RED_MUSHROOM_BLOCK;case 1->Blocks.BROWN_MUSHROOM_BLOCK;default->Blocks.MUSHROOM_STEM;};
                for(var pos:List.of(origin,origin.east(),origin.west(),origin.north(),origin.south())){
                    var state=f.owner.level().getBlockState(pos);
                    require(state.is(expected),"native lucky family normalized before initial clones: "+kind+" at "+pos+" got "+state);
                    require(LbcLuckyBlocks.kind(state)==null,"no native lucky family duplication");
                }
                if(kind==0)f.block(beacon,LbcMagic.MAGIC_BEACON_BLOCK);
                else{f.other.getInventory().setItem(0,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(f.other);game.items().magic().flushCleansing();}
                require(game.items().magic().suppressed(f.owner),"actual MAG/artifact field covers live frontier");
                stage=10;at=game.server().getTickCount();return false;
            }
            if(stage==10){
                if(skill.active(f.owner)){require(elapsed<10,"suppressed frontier retired promptly");return false;}
                require(skill.frontier(f.owner)==0&&skill.score(f.owner)>=61&&skill.score(f.owner)<80,"frontier-only jammer end, source cooldown retained");
                f.block(beacon,Blocks.AIR);f.other.getInventory().setItem(0,ItemStack.EMPTY);game.items().magic().inventoryChanged(f.other);game.items().magic().flushCleansing();
                if(++kind<3){beginLucky();return false;}
                game.players().get(f.owner.getUUID()).lbcSkill=0;stage=11;at=game.server().getTickCount();return false;
            }
            if(stage==11){
                if(elapsed<2)return false;
                require(skill.score(f.owner)==0&&skill.frontier(f.owner)==0,"skill loss clears all native ownership");
                var level=f.owner.level();
                origin=new BlockPos(origin.getX(),level.getMaxY(),origin.getZ());
                for(var pos:initialCells()){
                    rememberExtra(pos);
                    level.setBlock(pos,(pos.equals(origin)?Blocks.GOLD_BLOCK:Blocks.AIR).defaultBlockState(),Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                }
                game.players().get(f.owner.getUUID()).lbcSkill=24;
                chargeAt(origin);
                stage=12;return false;
            }
            if(stage==12){
                if(elapsed<61&&!skill.active(f.owner))return false;
                if(skill.active(f.owner)&&waitingForInitial(elapsed))return false;
                require(skill.active(f.owner)&&skill.frontier(f.owner)==4
                                &&initialCells().stream().allMatch(p->f.owner.level().getBlockState(p).is(Blocks.GOLD_BLOCK)),
                        "held sneak writes all four native expansion seeds on the inclusive highest block layer");
                skill.clear(f.owner);
                chargeAt(origin.above());
                stage=13;return false;
            }
            if(stage==13){
                if(elapsed<62)return false;
                require(!skill.active(f.owner)&&skill.frontier(f.owner)==0
                                &&initialCells().stream().allMatch(p->f.owner.level().getBlockState(p).is(Blocks.GOLD_BLOCK)),
                        "a seed one layer above the world retires without changing the legal top layer");
                coldFrontier=new ColdFrontier(this);
                coldFrontier.start();
                stage=14;at=game.server().getTickCount();return false;
            }
            return coldFrontier.resume();
        }
        boolean waitingForInitial(int elapsed){
            var owners=(Map<?,?>)WoolBucketChecks.read(skill,"owners");
            var state=owners.get(f.owner.getUUID());
            boolean waiting=Boolean.TRUE.equals(WoolBucketChecks.read(state,"initial"));
            if(waiting)require(elapsed<100&&skill.score(f.owner)==60,"the accepted initial write is still queued at the 60-tick threshold");
            return waiting;
        }
        List<BlockPos> initialCells(){
            return List.of(origin,origin.east(),origin.west(),origin.north(),origin.south());
        }
        void chargeAt(BlockPos pos){
            f.move(f.owner,net.minecraft.world.phys.Vec3.atBottomCenterOf(pos).add(0,.75,0));
            client.acknowledge();
            f.owner.setShiftKeyDown(true);
            at=game.server().getTickCount();
        }
        void beginLucky(){
            skill.clear(f.owner);f.block(beacon,Blocks.AIR);f.move(f.owner,f.at);
            for(var pos:List.of(origin.east(),origin.west(),origin.north(),origin.south(),origin.below()))f.block(pos,Blocks.AIR);
            f.block(origin,LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.values()[kind]));
            f.owner.setShiftKeyDown(true);stage=9;at=game.server().getTickCount();
        }
        @Override public void close(){
            if(closed)return;closed=true;game.vanity().stop();skill.clear(f.owner);game.items().timeStop().end(f.owner.level());
            if(coldFrontier!=null)coldFrontier.close();
            if(!extra.isEmpty()){
                clearExtraTicks();
                for(var e:extra.entrySet())game.maps().restoreSnapshot(f.owner.level(),e.getKey(),e.getValue());
                clearExtraTicks();
                blockTicks.stream().filter(t->!f.bounds.isInside(t.pos())).forEach(f.owner.level().getBlockTicks()::schedule);
                fluidTicks.stream().filter(t->!f.bounds.isInside(t.pos())).forEach(f.owner.level().getFluidTicks()::schedule);
            }
            if(client!=null)client.close();if(audience!=null)audience.close();f.close();
        }
        void clearExtraTicks(){
            for(long key:savedChunks){var chunk=new net.minecraft.world.level.ChunkPos((int)key,(int)(key>>32));
                var box=new BoundingBox(chunk.getMinBlockX(),f.owner.level().getMinY(),chunk.getMinBlockZ(),chunk.getMaxBlockX(),f.owner.level().getMaxY(),chunk.getMaxBlockZ());
                f.owner.level().getBlockTicks().clearArea(box);f.owner.level().getFluidTicks().clearArea(box);
            }
        }
    }

    /** Drives two real frontier jobs without waiting for a player at their chunk border to warm both chunks. */
    private static final class ColdFrontier implements AutoCloseable {
        final Run fixture;
        final LbcStonekivi terrain;
        final LbcExpansionSkill probe;
        final BlockPos cold,warm;
        LbcStonekivi.Work dispatcher;
        int attempts;

        ColdFrontier(Run fixture){
            this.fixture=fixture;
            terrain=fixture.game.items().stonekivi();
            cold=new BlockPos(8199,fixture.origin.getY()-8,8199);
            warm=fixture.origin.offset(4,0,0);
            probe=new LbcExpansionSkill(fixture.game,(owner,level,position)->{
                if(position.equals(cold))attempts++;
                return true;
            });
        }

        void start(){
            var level=fixture.f.owner.level();
            require(!NativeChunkLoad.resident(level,cold),"cold Expansion head starts without a resident FULL chunk");
            var coldState=arm(fixture.f.owner,cold,false);
            long random=(long)WoolBucketChecks.read(coldState,"random");
            invoke("ensureQueued",new Class<?>[0]);
            dispatcher=(LbcStonekivi.Work)WoolBucketChecks.read(probe,"queued");
            terrain.tick();
            terrain.tick();
            SkyWars2.LOGGER.info("[check] expansion cold attempts in one server tick={}",attempts);
            require(attempts==1&&!dispatcher.ready()&&probe.frontier(fixture.f.owner)==1
                            &&(long)WoolBucketChecks.read(coldState,"random")==random,
                    "a waiting real head is attempted once per server tick without spending RNG or consuming the cell");

            var neighborhood=List.of(warm,warm.east(),warm.west(),warm.north(),warm.south());
            prepare(neighborhood,warm);
            arm(fixture.f.other,warm,true);
            terrain.tick();
            require(attempts==1&&neighborhood.stream().allMatch(p->level.getBlockState(p).is(Blocks.GOLD_BLOCK)),
                    "another owner with resident terrain advances in the very same tick");
            // The native loader keeps its ticket. Take the fixture terrain snapshots before the next write.
            require(work().remove(dispatcher),"waiting dispatcher remains queued before loading completes");
        }

        boolean resume(){
            var level=fixture.f.owner.level();
            if(!NativeChunkLoad.resident(level,cold))return false;
            var neighborhood=List.of(cold,cold.east(),cold.north(),cold.south(),cold.below());
            prepare(neighborhood,cold);
            terrain.queuedWork(dispatcher);
            terrain.tick();
            require(attempts==2&&probe.frontier(fixture.f.owner)>1
                            &&neighborhood.stream().allMatch(p->!level.getBlockState(p).isAir()
                                    &&level.getBlockState(p).equals(level.getBlockState(cold))),
                    "native FULL residency resumes the retained direction and publishes its four copies");
            return true;
        }

        void prepare(List<BlockPos> cells,BlockPos center){
            var level=fixture.f.owner.level();
            for(var pos:cells){
                fixture.rememberExtra(pos);
                level.setBlock(pos,(pos.equals(center)?Blocks.GOLD_BLOCK:Blocks.AIR).defaultBlockState(),
                        Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            }
        }

        @SuppressWarnings("unchecked") Object arm(ServerPlayer owner,BlockPos pos,boolean initial){
            try{
                var state=create("State",new Class<?>[]{ServerPlayer.class},owner);
                for(var name:List.of("active","initial")){
                    var field=state.getClass().getDeclaredField(name);field.setAccessible(true);
                    field.setBoolean(state,name.equals("active")||initial);
                }
                var random=state.getClass().getDeclaredField("random");random.setAccessible(true);random.setLong(state,0);
                var cell=create("Cell",new Class<?>[]{long.class,int.class,boolean.class},pos.asLong(),0,true);
                ((Deque<Object>)WoolBucketChecks.read(state,"current")).add(cell);
                ((Map<UUID,Object>)WoolBucketChecks.read(probe,"owners")).put(owner.getUUID(),state);
                invoke("addIndex",new Class<?>[]{net.minecraft.server.level.ServerLevel.class,long.class},owner.level(),pos.asLong());
                return state;
            }catch(ReflectiveOperationException error){throw new IllegalStateException("prepare native expansion head",error);}
        }

        static Object create(String name,Class<?>[] types,Object... args) throws ReflectiveOperationException{
            var type=Arrays.stream(LbcExpansionSkill.class.getDeclaredClasses())
                    .filter(c->c.getSimpleName().equals(name)).findFirst().orElseThrow();
            var constructor=type.getDeclaredConstructor(types);constructor.setAccessible(true);
            return constructor.newInstance(args);
        }

        void invoke(String name,Class<?>[] types,Object... args){
            try{
                var method=LbcExpansionSkill.class.getDeclaredMethod(name,types);method.setAccessible(true);
                method.invoke(probe,args);
            }catch(ReflectiveOperationException error){throw new IllegalStateException("prepare native expansion dispatcher",error);}
        }

        @SuppressWarnings("unchecked") Deque<LbcStonekivi.Work> work(){
            return (Deque<LbcStonekivi.Work>)WoolBucketChecks.read(terrain,"work");
        }

        @Override public void close(){
            probe.stop();
            if(dispatcher!=null)work().remove(dispatcher);
        }
    }
}
