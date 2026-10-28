package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.*;

/** One real type5 warmup/pulse, followed by normal world ticks without its emitter. */
public final class RadiantMetasolaChecks {
    private RadiantMetasolaChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Radiant Metasola: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner;final ServerLevel level;final GameChecks.WandThirdPlayerState saved;
        final Vec3 at=new Vec3(-176.5,240,-176.5);final BlockPos center=BlockPos.containing(at),origin=center.offset(-10,-10,-10);
        final BoundingBox bounds=new BoundingBox(center.getX()-16,center.getY()-16,center.getZ()-16,center.getX()+16,center.getY()+16,center.getZ()+16);
        final Set<Long> forced=new HashSet<>();final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Map<BlockPos,BlockState> palette=new LinkedHashMap<>();final Map<BlockPos,BlockState> stable=new LinkedHashMap<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        LbcSoulless.Field field;BlockPos airSentinel,untouchedSentinel;int phase,warmTicks;long last=Long.MIN_VALUE,started,pulse;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&owner!=null&&game.canUseGameplayItems(owner),"active native participant");level=owner.level();
            require(level==game.maps().level()&&game.items().soulless().count()==0,"clean field service in actual match dimension");
            require(bounds.minY()>=level.getMinY()&&bounds.maxY()<level.getMaxY(),"complete source and cleanup halo fit build height");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        }
        void force(int x,int z){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);}
        void init(){
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)force(x,z);
            force((center.getX()+40)>>4,center.getZ()>>4);
        }
        boolean warm(){
            require(++warmTicks<600,"forced fixture chunks become entity-resident");
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)
                if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;
            return true;
        }
        void write(BlockPos pos,BlockState state){level.setBlock(pos,state,Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
        void prepare(){
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){
                var chunk=level.getChunkSource().getChunkNow(x,z);require(chunk!=null,"resident cleanup halo");
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            }
            for(var pos:BlockPos.betweenClosed(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()))
                blocks.put(pos.immutable(),game.maps().snapshot(level,pos));
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:light_sphere_21x21")).orElseThrow();
            require(template.getSize().equals(new net.minecraft.core.Vec3i(21,21,21)),"native source volume21 cubed");
            int index=0;int[] counts={2229,739,1977};
            for(var type:List.of(Blocks.FIRE,Blocks.LAVA,Blocks.VOID_AIR)){
                var cells=template.filterBlocks(origin,new StructurePlaceSettings(),type);
                require(cells.size()==counts[index++],"exact native palette count "+type);
                for(var cell:cells)require(palette.put(cell.pos(),cell.state())==null,"unique native source cell");
            }
            require(palette.size()==4945,"complete sparse native source mask");
            for(var pos:BlockPos.betweenClosed(origin,origin.offset(20,20,20))){
                if(!level.getBlockState(pos).isAir())write(pos,Blocks.AIR.defaultBlockState());
                if(untouchedSentinel==null&&!palette.containsKey(pos))untouchedSentinel=pos.immutable();
            }
            airSentinel=palette.entrySet().stream().filter(e->e.getValue().isAir()).map(Map.Entry::getKey).findFirst().orElseThrow();
            require(untouchedSentinel!=null,"sparse template leaves explicit unlisted control cell");
            write(airSentinel,Blocks.STONE.defaultBlockState());write(untouchedSentinel,Blocks.DIAMOND_BLOCK.defaultBlockState());
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);owner.setShiftKeyDown(false);
            game.items().mana().noMagic(owner,0);
            owner.teleportTo(level,at.x+40,at.y,at.z,Set.of(),0,0,true);owner.setDeltaMovement(Vec3.ZERO);
            ItemStack item=game.lbc().rollTable(level,at,owner,"lbc:ported/metasola_5").getFirst();
            require(LbcSoulless.material(item)==5,"real Radiant Metasola item");owner.setItemInHand(InteractionHand.MAIN_HAND,item);
            require(game.items().soulless().use(owner,InteractionHand.MAIN_HAND)&&owner.getMainHandItem().isEmpty(),"native type5 activation consumes one item");
            var fields=game.items().soulless().snapshot();require(fields.size()==1&&fields.getFirst().type==5,"one actual type5 emitter");field=fields.getFirst();
            field.pig.setNoAi(true);field.pig.setNoGravity(true);field.pig.setDeltaMovement(Vec3.ZERO);field.pig.setPos(at);field.display.setPos(at);
            require(field.age==0,"natural warmup starts without counter edits");started=level.getGameTime();phase=1;
        }
        void capturePulse(){
            require(field.age==200&&level.getGameTime()-started>=199,"one pulse after real200tick warmup: age="+field.age);
            for(var entry:palette.entrySet()){
                var actual=level.getBlockState(entry.getKey());var expected=entry.getValue();
                require(expected.isAir()?actual.isAir():actual.equals(expected),"integrity1 strict source state "+entry.getKey()+" expected="+expected+" actual="+actual);
                require(!level.getBlockTicks().hasScheduledTick(entry.getKey(),Blocks.FIRE)&&!level.getFluidTicks().hasScheduledTick(entry.getKey(),Fluids.LAVA),"strict pulse schedules neither fire nor lava update");
            }
            require(level.getBlockState(airSentinel).isAir(),"explicit source void_air clears its sentinel");
            require(level.getBlockState(untouchedSentinel).is(Blocks.DIAMOND_BLOCK),"unlisted source cell preserves existing block");
            int fire=0,lava=0;
            for(var entry:palette.entrySet()){
                BlockPos pos=entry.getKey();BlockState state=entry.getValue();
                if(state.is(Blocks.FIRE)&&!state.canSurvive(level,pos)&&fire<8){stable.put(pos,state);fire++;}
                // The native palette has no lava over air: its97 bottom lava cells float over non-supporting FIRE.
                if(state.is(Blocks.LAVA)&&level.getBlockState(pos.below()).is(Blocks.FIRE)&&lava<8){stable.put(pos,state);lava++;}
            }
            require(fire==8&&lava==8,"representative unsupported fire and floating source lava exist in empty fixture: fire="+fire+"/8 lava="+lava+"/8 (source has97 lava-over-fire candidates)");
            game.items().soulless().unloaded(field.pig);
            require(field.pig.isRemoved()&&field.display.isRemoved()&&game.items().soulless().count()==0,"remove both actual emitter actors before persistence interval");
            pulse=level.getGameTime();phase=2;
        }
        boolean step(){
            long now=level.getGameTime();if(last==now)return false;last=now;
            require(game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(level),"persistence uses normal unpaused world ticks");
            if(phase==0){if(!warm())return false;prepare();return false;}
            if(phase==1){require(now-started<240,"native type5 warmup completes");if(field.age>=200)capturePulse();return false;}
            require(game.items().soulless().count()==0&&field.pig.isRemoved()&&field.display.isRemoved(),"no emitter replenishes the placed source");
            if(now-pulse<120)return false;
            for(var entry:stable.entrySet())require(level.getBlockState(entry.getKey()).equals(entry.getValue()),"unsupported source persists after120 normal ticks without emitter: "+entry.getKey());
            require(level.getBlockState(untouchedSentinel).is(Blocks.DIAMOND_BLOCK),"unlisted control survives persistence interval");
            return true;
        }
        public void close(){
            game.items().soulless().forget(owner);
            if(!blocks.isEmpty()){
                level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
                for(var entry:blocks.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
                level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
                blockTicks.forEach(level.getBlockTicks()::schedule);fluidTicks.forEach(level.getFluidTicks()::schedule);
            }
            saved.restore(game);
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game){
        require(running==null,"exclusive fixture");var fixture=new Fixture(game);running=fixture;
        try{fixture.init();}catch(RuntimeException|Error failure){running=null;fixture.close();throw failure;}
    }
    public static void tick(GameController game){
        var fixture=running;if(fixture==null)return;
        try{if(fixture.step()){running=null;fixture.close();SkyWars2.LOGGER.info("[check] PASS radiant-metasola (native activation, natural200tick warmup, exact full strict sparse source, intentional source air, removed emitter, floating lava/unsupported fire after120 normal ticks)");}}
        catch(Throwable failure){running=null;try{fixture.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL radiant-metasola",failure);}
    }
}
