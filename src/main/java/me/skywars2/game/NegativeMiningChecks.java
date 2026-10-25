package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real native START/tick/STOP/ABORT paths exercise signed progress and independent BE copies. */
public final class NegativeMiningChecks {
    private NegativeMiningChecks() { }
    private static void require(boolean ok,String text) {if(!ok)throw new IllegalStateException("Negative mining: "+text);}
    public static void run(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing() && player!=null && game.allowDamage(player),"actual participant");
        var level=game.maps().level();
        var prior=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var speed=player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        var water=player.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
        var efficiency=player.getAttribute(Attributes.MINING_EFFICIENCY);
        double oldSpeed=speed.getBaseValue(),oldWater=water.getBaseValue(),oldEfficiency=efficiency.getBaseValue();
        var source=new BlockPos((int)Math.floor(player.getX()/16)*16+8,Math.min(270,level.getMaxY()-10),(int)Math.floor(player.getZ()/16)*16+8);
        var target=source.north();
        var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        var initial=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(initial::add);
        int sequence=741;
        try {
            for(var mutable:BlockPos.betweenClosed(source.offset(-2,-3,-5),source.offset(2,3,2))) {
                var pos=mutable.immutable();
                require(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"resident fixture");
                blocks.put(pos,game.maps().snapshot(level,pos));
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),818);
            }
            player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
            player.teleportTo(level,source.getX()+.5,source.getY()-1,source.getZ()-3.5,Set.of(),0,0,true);
            player.setDeltaMovement(Vec3.ZERO);player.setOnGround(true);
            var log=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
            level.setBlock(source,log,818);speed.setBaseValue(-1);water.setBaseValue(1);efficiency.setBaseValue(0);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_AXE));
            double rate=-log.getDestroyProgress(player,level,source);
            require(rate>0 && rate<1,"negative unbounded native effective progress");
            int period=(int)Math.ceil(1/rate);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            for(int i=1;i<period;i++)player.gameMode.tick();
            require(level.getBlockState(target).isAir() && level.getBlockState(source)==log,"fractional progress waits without source damage");
            player.gameMode.tick();
            require(level.getBlockState(target)==log && level.getBlockState(source)==log,"exact threshold creates adjacent toward-player oriented copy");
            level.setBlock(target,Blocks.GLASS.defaultBlockState(),818);
            for(int i=0;i<period*2;i++)player.gameMode.tick();
            require(level.getBlockState(target).is(Blocks.GLASS) && level.getBlockState(target.north()).isAir(),"occupied neighbor never overwritten or extended into a line");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);for(int i=0;i<period*2;i++)player.gameMode.tick();
            require(level.getBlockState(target).isAir() && level.getBlockState(source)==log,"STOP cancels both custom hold and native delayed destruction");
            speed.setBaseValue(-1000);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();require(level.getBlockState(target)==log,"greater magnitude copies on first held tick");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"ABORT stops repeat copies");
            // Effective negative efficiency uses the same native route without a second formula.
            speed.setBaseValue(1);efficiency.setBaseValue(-1000);
            require(log.getDestroyProgress(player,level,source)<0,"negative efficiency makes native progress negative with an effective tool");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();require(level.getBlockState(target)==log,"negative efficiency clones through the shared effective-rate path");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);efficiency.setBaseValue(0);water.setBaseValue(-1000);
            var eye=BlockPos.containing(player.getEyePosition());level.setBlock(eye,Blocks.WATER.defaultBlockState(),818);player.baseTick();player.setOnGround(true);
            require(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && log.getDestroyProgress(player,level,source)<0,"native eye-water multiplier produces negative progress");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();require(level.getBlockState(target)==log,"negative submerged mining clones while eyes are in real source water");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            level.setBlock(eye,Blocks.AIR.defaultBlockState(),818);player.baseTick();player.setOnGround(true);water.setBaseValue(1);
            level.setBlock(target,Blocks.AIR.defaultBlockState(),818);speed.setBaseValue(-1000);
            // Each guard ends the held action; it cannot silently resume when protection ends.
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            game.items().timeStop().begin(level,200);player.gameMode.tick();game.items().timeStop().end(level);player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"Time Stop cancels hold without delayed replay");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.entityTags().add("sin_greed_active");player.gameMode.tick();player.entityTags().remove("sin_greed_active");player.gameMode.tick();
            require(level.getBlockState(target).isAir(),"active Greed rejects and clears held cloning");
            player.setGameMode(GameType.CREATIVE);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(level.getBlockState(source)==log,"negative creative START preserves source");player.gameMode.tick();
            require(level.getBlockState(target)==log,"creative negative progress uses same held clone");
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.setGameMode(GameType.SURVIVAL);level.setBlock(target,Blocks.AIR.defaultBlockState(),818);
            // Copy complete independently owned native block-entity state, including intentional inventory duplicates.
            level.setBlock(source,Blocks.CHEST.defaultBlockState(),818);
            var chest=(ChestBlockEntity)level.getBlockEntity(source);require(chest!=null,"native source chest");
            var diamonds=new ItemStack(Items.DIAMOND,13);chest.setItem(0,diamonds);chest.setChanged();
            var beforeCopy=game.maps().snapshot(level,source);
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            player.gameMode.tick();
            var copy=(ChestBlockEntity)level.getBlockEntity(target);
            require(copy!=null && copy!=chest && copy.getBlockPos().equals(target)
                    && ItemStack.isSameItemSameComponents(copy.getItem(0),diamonds) && copy.getItem(0).getCount()==13
                    && chest.getItem(0).getCount()==13,"independent native chest copy intentionally retains complete contents and destination position");
            var sourceData=beforeCopy.entity().copy();var destinationData=game.maps().snapshot(level,target).entity().copy();
            for(var key:List.of("x","y","z")){sourceData.remove(key);destinationData.remove(key);}
            require(sourceData.equals(destinationData),"full block entity metadata/components copied except destination coordinates");
            copy.getItem(0).shrink(5);require(chest.getItem(0).getCount()==13,"copied inventory is deeply independent from source");
            require(level.getBlockState(source)==beforeCopy.state(),"container source state remains unchanged");
            for(var e:level.getAllEntities())if(!initial.contains(e) && e instanceof ItemEntity)require(e.isRemoved(),"cloning emits no block or inventory drops");
        } finally {
            player.gameMode.handleBlockBreakAction(source,ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence);
            game.items().timeStop().end(level);player.entityTags().remove("sin_greed_active");
            for(var entry:blocks.entrySet()) {
                if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();
                game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
            }
            speed.setBaseValue(oldSpeed);water.setBaseValue(oldWater);efficiency.setBaseValue(oldEfficiency);
            prior.restore(game);player.baseTick();
            var cleanup=new ArrayList<Entity>();for(var actor:level.getAllEntities())if(!initial.contains(actor))cleanup.add(actor);
            cleanup.forEach(Entity::discard);
        }
        SkyWars2.LOGGER.info("[check] PASS negative-mining");
    }
}
