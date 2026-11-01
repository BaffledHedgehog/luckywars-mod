package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.*;

/** Pure full-size geometry plus bounded native placement and120 normal ticks of unsupported magma/glass. */
public final class StonekiviSunChecks {
    private StonekiviSunChecks() { }
    private static Fixture running;
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException("Stonekivi SUN: "+message);
    }
    public static void run(GameController game) {
        require(running==null,"exclusive fixture");
        pureGeometry();
        nativeDispatch(game);
        var fixture=new Fixture(game);running=fixture;
        try {fixture.start();}catch(RuntimeException|Error error){running=null;fixture.close();throw error;}
    }
    public static void tick(GameController game) {
        Fixture fixture=running;if(fixture==null)return;
        try {
            if(fixture.tick()) {
                running=null;fixture.close();
                SkyWars2.LOGGER.info("[check] PASS stonekivi-sun (radius100/source148 offset geometry, bounded shared queue, no generation, strict floating magma/glass120 normal ticks)");
            }
        }catch(RuntimeException|Error error){running=null;fixture.close();throw error;}
    }
    /** Real native loot/use/selector path, cancelled before any full-size terrain step. */
    private static void nativeDispatch(GameController game) {
        require(game.items().stonekivi().count()==0,"exclusive shared terrain queue before dispatch fixture");
        try(var base=new ElementalGrimoireChecks.Fixture(game)) {
            base.initialize();
            BlockPos wall=BlockPos.containing(base.owner.getEyePosition().add(0,0,8));
            var original=game.maps().snapshot(base.owner.level(),wall);
            try {
                game.maps().restoreSnapshot(base.owner.level(),wall,new MapManager.BlockSnapshot(Blocks.STONE.defaultBlockState(),null),LbcStonekiviSun.FLAGS);
                var kind=LbcElementalGrimoires.Kind.STONEKIVI_TRUE_SUN;
                require(kind.spellId==300&&kind.metadataMana==2500&&LbcElementalGrimoires.shaping().contains(kind),"native original-wand SUN metadata and catalog membership");
                require(kind.item.getDefaultInstance().get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:spelllib/stonestone")),"original Stonekivi grimoire texture");
                base.presentation(kind);
                for(var hand:InteractionHand.values()) {
                    var book=base.held(kind,hand,2);base.owner.setYRot(0);base.owner.setXRot(0);base.owner.setShiftKeyDown(false);
                    base.mana.set(base.owner,-500);int mana=base.mana.get(base.owner);
                    require(base.input(hand)&&book.getCount()==1&&base.mana.get(base.owner)==mana,"native SUN grimoire consumes one book without mana debit "+hand);
                    var jobs=game.items().stonekivi().queuedSnapshot();
                    require(jobs.size()==1&&jobs.getFirst() instanceof LbcStonekiviSun.SunWork sun
                            &&sun.radius==100&&sun.center.equals(wall.above(148))&&sun.writes()==0,"actual grimoire dispatch selects original-size SUN before world writes");
                    game.items().stonekivi().stop();
                }
                require(game.items().stonekivi().ready(),"existing Stonekivi templates ready for old-mode/selector comparison");
                var crystal=base.held(LbcElementalGrimoires.Kind.STONEKIVI_SUN,InteractionHand.MAIN_HAND,2);
                require(base.input(InteractionHand.MAIN_HAND)&&crystal.getCount()==1,"existing Crystallization grimoire remains usable");
                var jobs=game.items().stonekivi().queuedSnapshot();
                require(jobs.size()==1&&jobs.getFirst().getClass().getName().equals("me.skywars2.game.LbcMagmify$Work"),"existing mode3 retains native Crystallization");
                game.items().stonekivi().stop();
                var wand=new ItemStack(LbcWands.ACADEMY_STONEKIVI);var data=LbcFartus.data(wand);data.putInt("mode",4);
                wand.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                base.owner.setItemInHand(InteractionHand.MAIN_HAND,wand);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                base.owner.setShiftKeyDown(true);base.mana.set(base.owner,0);
                for(int expected:new int[]{5,1,2,3,4}) {
                    require(base.input(InteractionHand.MAIN_HAND)&&LbcFartus.data(wand).getIntOr("mode",0)==expected
                            &&base.mana.get(base.owner)==0&&game.items().stonekivi().count()==0,"native five-mode selector is free and creates no terrain work");
                }
            }finally {
                game.items().stonekivi().stop();
                game.maps().restoreSnapshot(base.owner.level(),wall,original,LbcStonekiviSun.FLAGS);
            }
        }
    }

    private static void pureGeometry() {
        require(LbcStonekiviSun.RADIUS==100&&LbcStonekiviSun.CENTER_HEIGHT==148,"retired source size and vertical offset");
        require(LbcStonekiviSun.material(66,0,0,100,17).is(Blocks.MAGMA_BLOCK),"inner two-thirds magma");
        require(LbcStonekiviSun.material(67,0,0,100,17).is(Blocks.MAGMA_BLOCK)
                &&LbcStonekiviSun.material(85,0,0,100,17).is(Blocks.MAGMA_BLOCK),"continuous magma shell");
        Set<BlockState> palette=new HashSet<>();int tongues=0;
        for(int x=-116;x<=116;x+=4)for(int y=-116;y<=116;y+=4)for(int z=-116;z<=116;z+=4) {
            BlockState state=LbcStonekiviSun.material(x,y,z,100,424242);
            require(Objects.equals(state,LbcStonekiviSun.material(x,y,z,100,424242)),"seeded coordinate material is deterministic");
            if(state!=null){require(!state.is(Blocks.LAVA)&&!state.is(Blocks.FIRE),"author: no lava/fire anywhere in SUN");palette.add(state);if((long)x*x+(long)y*y+(long)z*z>103L*103)tongues++;}
        }
        for(var block:List.of(Blocks.MAGMA_BLOCK,Blocks.GLOWSTONE,Blocks.SHROOMLIGHT,
                Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.ORANGE),Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.YELLOW)))
            require(palette.contains(block.defaultBlockState()),"full-size palette includes "+block);
        require(tongues>0&&LbcStonekiviSun.material(121,0,0,100,424242)==null,"finite photospheric flame ribbons extend beyond sphere");
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner;final ServerLevel level;final BlockPos center;
        final BoundingBox bounds;
        final Map<BlockPos,MapManager.BlockSnapshot> originals=new LinkedHashMap<>();
        final Map<BlockPos,BlockState> stable=new LinkedHashMap<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();
        final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final it.unimi.dsi.fastutil.longs.LongOpenHashSet forced;
        LbcStonekiviSun.SunWork work;long started,finished;boolean placed;
        Fixture(GameController game) {
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&owner!=null&&game.canUseGameplayItems(owner),"actual active match participant");
            level=owner.level();require(level==game.maps().level(),"native arena world");
            center=new BlockPos(owner.getBlockX(),Math.min(owner.getBlockY()+48,level.getMaxY()-19),owner.getBlockZ());
            bounds=new BoundingBox(center.getX()-17,center.getY()-17,center.getZ()-17,
                    center.getX()+17,center.getY()+17,center.getZ()+17);
            require(bounds.minY()>=level.getMinY()&&bounds.maxY()<level.getMaxY(),"fixture and halo fit world height");
            forced=new it.unimi.dsi.fastutil.longs.LongOpenHashSet(level.getForceLoadedChunks());
        }
        void start() {
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
                var chunk=level.getChunkSource().getChunkNow(x,z);require(chunk!=null,"fixture requires already resident chunks");
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            }
            for(var pos:BlockPos.betweenClosed(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()))
                originals.put(pos.immutable(),game.maps().snapshot(level,pos));
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            for(var pos:originals.keySet())game.maps().restoreSnapshot(level,pos,MapManager.BlockSnapshot.air(),LbcStonekiviSun.FLAGS);
            var originalSize=(LbcStonekiviSun.SunWork)LbcStonekiviSun.create(game,owner,center.below(148));
            require(originalSize.radius==100&&originalSize.center.equals(center),"actual public factory retains original geometry");
            originalSize.cancel();
            int remoteX=owner.getBlockX()+16384,remoteZ=owner.getBlockZ()+16384;
            for(int cx=(remoteX-15)>>4;cx<=(remoteX+15)>>4;cx++)for(int cz=(remoteZ-15)>>4;cz<=(remoteZ+15)>>4;cz++)
                require(level.getChunkSource().getChunkNow(cx,cz)==null,"unresident control columns");
            var remote=new LbcStonekiviSun.SunWork(game,owner,new BlockPos(remoteX,center.getY(),remoteZ),8,424242);
            require(remote.step()&&remote.writes()==0&&remote.visits()==0,"unresident work finishes without terrain reads/writes or generation");
            require(level.getChunkSource().getChunkNow(remoteX>>4,remoteZ>>4)==null,"unresident control remains absent");
            work=new LbcStonekiviSun.SunWork(game,owner,center,8,424242);
            game.items().stonekivi().queuedWork(work);started=level.getGameTime();
        }
        boolean tick() {
            require(game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(level),"normal unpaused persistence interval");
            if(!placed) {
                require(level.getGameTime()-started<400,"native shared work completes");
                if(!work.finished())return false;
                require(work.writes()>0&&forced.equals(level.getForceLoadedChunks()),"bounded native writes preserve forced chunks");
                int floatingMagma=0;
                for(var pos:originals.keySet()) {
                    BlockState expected=LbcStonekiviSun.material(pos.getX()-center.getX(),pos.getY()-center.getY(),pos.getZ()-center.getZ(),8,424242);
                    if(expected==null)continue;
                    require(level.getBlockState(pos).equals(expected),"native coordinate palette at "+pos);
                    require(!level.getBlockTicks().hasScheduledTick(pos,Blocks.FIRE)
                            &&!level.getFluidTicks().hasScheduledTick(pos,Fluids.LAVA),"quiet writes schedule neither fire nor lava");
                    require(!expected.is(Blocks.LAVA)&&!expected.is(Blocks.FIRE),"native SUN contains no lava/fire");
                    if(expected.is(Blocks.MAGMA_BLOCK)&&level.getBlockState(pos.below()).isAir()&&floatingMagma<8){stable.put(pos,expected);floatingMagma++;}
                }
                require(floatingMagma>0,"unsupported magma exists in native ribbons");
                finished=level.getGameTime();placed=true;return false;
            }
            if(level.getGameTime()-finished<120)return false;
            for(var entry:stable.entrySet())require(level.getBlockState(entry.getKey()).equals(entry.getValue()),"floating state persists without emitter "+entry.getKey());
            require(forced.equals(level.getForceLoadedChunks()),"no permanent chunk forcing");
            return true;
        }
        public void close() {
            if(work!=null)work.cancel();
            if(originals.isEmpty())return;
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            for(var entry:originals.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),LbcStonekiviSun.FLAGS);
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
            blockTicks.forEach(level.getBlockTicks()::schedule);fluidTicks.forEach(level.getFluidTicks()::schedule);
        }
    }
}
