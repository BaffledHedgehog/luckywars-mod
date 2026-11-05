package me.skywars2.game;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Central source-specific admission and terminal intent, shared by both territory conversions. */
final class LbcImbaFollowup {
    private LbcImbaFollowup(){}
    static boolean admitted(GameController game,Entity target){
        return game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target)
                &&!(target instanceof Display)&&!(target instanceof Interaction)
                &&(LbcWtf.managed(target)||!game.items().cloneSkills().protectedFamilyActor(target));
    }
    static boolean erase(GameController game,Entity target,LbcIndirectKills.Owner cause){
        if(!admitted(game,target))return false;
        if(target instanceof LivingEntity living){game.items().indirectKills().inheritedExecution(living,cause);return !living.isAlive()||living.isRemoved();}
        target.discard();return true;
    }
    static boolean midas(GameController game,ServerPlayer player,InteractionHand hand,Vec3 origin){
        var stack=player.getItemInHand(hand);var at=BlockPos.containing(origin);var level=player.level();
        if(!LbcImbaFollowupItems.midas(stack)||!game.playing()||game.stage()==GameController.Stage.ENDING||game.maps().busy()
                ||!game.canUseGameplayItems(player)||game.isObserver(player)||!player.isAlive()||player.isRemoved()||player.isSpectator()
                ||!LbcDimensionItems.ordinary(level)||!LbcBedwarsRoleGuards.publication(level)||game.items().actionsBlocked(player)
                ||game.items().wands().inputTimeStopped(player)||game.items().timeStop().stopped(level)||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||!level.hasChunkAt(at)||!level.isInValidBounds(at)||game.items().magic().suppressed(player)||game.items().magic().suppressedAt(level,origin))return false;
        var cause=game.items().indirectKills().wildCause(player);
        if(!game.items().writeOwnedTerrain(level,at,Blocks.RAW_GOLD_BLOCK.defaultBlockState(),cause))return false;
        game.items().cosmetics().midas().launch(player,level,at,cause,true,-1);stack.consume(1,player);player.inventoryMenu.broadcastChanges();return true;
    }
    static void adopt(GameController game,Entity entity,LivingEntity user,ItemStack stack){
        if(!(user instanceof ServerPlayer owner)||!LbcFusedTnt.managed(entity)&&!stack.is(LbcImbaFollowupItems.SUPER_GHAST)
                &&!(entity instanceof net.minecraft.world.entity.monster.Ghast&&ItemData.intValue(stack,"imba",0)==1))return;
        game.items().spellSources().bind(entity,new LbcSpellSource.Source(entity.getUUID(),owner.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(owner),Set.of(),true));
    }
}
