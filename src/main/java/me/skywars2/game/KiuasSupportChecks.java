package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.*;

/** Actual loaded-world support decisions and the three shared fire producers. */
public final class KiuasSupportChecks {
    private KiuasSupportChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Kiuas support: "+why);}
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(owner!=null&&game.playing()&&game.canUseGameplayItems(owner),"active native owner");
        var level=owner.level();var service=game.items().kiuas();var blaze=game.items().blaze();
        require(service.count()==0&&service.queuedBlockCount()==0&&blaze.terrainCount()==0,"exclusive native terrain services");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var center=new BlockPos(Math.floorDiv(owner.blockPosition().getX(),16)*16+8,240,Math.floorDiv(owner.blockPosition().getZ(),16)*16+8);
        var bounds=new BoundingBox(center.getX()-4,center.getY()-4,center.getZ()-4,center.getX()+4,center.getY()+4,center.getZ()+4);
        var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();var ticks=new ArrayList<ScheduledTick<Block>>();
        Cow cow=null;
        try{
            var chunk=level.getChunkSource().getChunkNow(center.getX()>>4,center.getZ()>>4);require(chunk!=null,"resident fixture only");
            ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(ticks::add);
            for(var p:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))blocks.put(p.immutable(),game.maps().snapshot(level,p));
            owner.closeContainer();owner.getInventory().clearContent();owner.setNoGravity(true);
            owner.teleportTo(level,center.getX()+.5,center.getY(),center.getZ()+.5,Set.of(),0,0,true);
            Runnable clear=()->{level.getBlockTicks().clearArea(bounds);for(var p:blocks.keySet())level.setBlock(p,Blocks.AIR.defaultBlockState(),818);};
            clear.run();
            require(LbcKiuas.kiuResult(Blocks.WATER.defaultBlockState()).is(Blocks.FIRE),"pure ordered classification retained");
            require(!LbcKiuas.fireSupported(level,center)&&LbcKiuas.kiuResult(level,center,Blocks.AIR.defaultBlockState()).isAir()
                    &&LbcKiuas.kiuResult(level,center,Blocks.OAK_PLANKS.defaultBlockState()).is(Blocks.OAK_PLANKS)
                    &&LbcKiuas.kiuResult(level,center,Blocks.WATER.defaultBlockState()).isAir(),"unsupported air/wood cannot ignite; water still clears");
            for(var direction:Direction.values()){
                clear.run();level.setBlock(center.relative(direction),Blocks.STONE.defaultBlockState(),818);
                require(LbcKiuas.fireSupported(level,center)==(direction==Direction.DOWN),"nonflammable support only below: "+direction);
                level.setBlock(center.relative(direction),Blocks.OAK_PLANKS.defaultBlockState(),818);
                require(LbcKiuas.fireSupported(level,center)&&LbcKiuas.kiuResult(level,center,Blocks.AIR.defaultBlockState()).is(Blocks.FIRE),"flammable support on each native side: "+direction);
            }
            clear.run();level.setBlock(center.below(),Blocks.WATER.defaultBlockState(),818);
            require(LbcKiuas.fireSupported(level,center),"any nonair below, including fluid");
            clear.run();level.setBlock(center.east(),Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),818);
            require(!LbcKiuas.fireSupported(level,center),"waterlogged side retains native nonflammability");
            clear.run();owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcKiuas.KIUASKIVI));game.items().mana().set(owner,10000);
            service.tick();for(int i=0;i<30&&service.queuedBlockCount()>0;i++)service.tick();
            require(level.getBlockState(center).isAir(),"actual aura leaves unsupported sky empty");
            level.setBlock(center.below(),Blocks.STONE.defaultBlockState(),818);service.blockChanged(center.below(),Blocks.STONE.defaultBlockState());
            for(int i=0;i<30;i++){service.tick();if(level.getBlockState(center).is(Blocks.FIRE))break;}
            require(level.getBlockState(center).is(Blocks.FIRE),"actual aura reacts to newly placed support");
            service.stop();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);clear.run();
            cow=new Cow(EntityTypes.COW,level);cow.setPos(Vec3.atLowerCornerOf(center));cow.setNoGravity(true);cow.setNoAi(true);require(level.addFreshEntity(cow),"native curse target");
            require(service.acceptCurse(cow,new MobEffectInstance(MobEffects.RAID_OMEN,20,111),owner),"native owned Kiuas curse admission");
            service.tick();require(level.getBlockState(center).isAir(),"actual curse rejects unsupported fire");
            level.setBlock(center.below(),Blocks.STONE.defaultBlockState(),818);service.tick();require(level.getBlockState(center).is(Blocks.FIRE),"actual curse admits supported fire");
            service.clearCurse(cow);cow.discard();cow=null;clear.run();
            blaze.incendiaryCube(owner,Vec3.atCenterOf(center));for(int i=0;i<30&&blaze.terrainCount()>0;i++)blaze.tick();
            require(blaze.terrainCount()==0&&level.getBlockState(center).isAir(),"shared Blaze Kiuas conversion rejects unsupported sky");
            level.setBlock(center.below(),Blocks.STONE.defaultBlockState(),818);blaze.incendiaryCube(owner,Vec3.atCenterOf(center));
            for(int i=0;i<30&&blaze.terrainCount()>0;i++)blaze.tick();require(blaze.terrainCount()==0&&level.getBlockState(center).is(Blocks.FIRE),"shared Blaze Kiuas conversion admits floor support");
        }finally{
            if(cow!=null){service.clearCurse(cow);cow.discard();}service.stop();blaze.forget(owner);
            level.getBlockTicks().clearArea(bounds);for(var e:blocks.entrySet())game.maps().restoreSnapshot(level,e.getKey(),e.getValue());
            level.getBlockTicks().clearArea(bounds);ticks.forEach(level.getBlockTicks()::schedule);saved.restore(game);game.items().magic().inventoryChanged(owner);
        }
        SkyWars2.LOGGER.info("[check] PASS kiuas-support (native six-side support, ordered classification, aura/reactivity, curse and shared Blaze conversion)");
    }
}
