package me.skywars2.game;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Original raw Boba input; accepted terrain work belongs exclusively to MapManager. */
public final class LbcBoba {
    private static final String[] SOUNDS={"ponos","yametekudasai","dick_1","weewee","spank","woo",
            "suction","vomiting","shitting_1","shitting_2","metal_pipe"};
    private final GameController game;
    public LbcBoba(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()
            &&LbcFartus.data(stack).getIntOr("magicwand",0)==1
            &&LbcFartus.data(stack).getIntOr("boba_eblan",0)==1;}
    private static boolean debitMatches(ItemStack stack){return !stack.isEmpty()
            &&LbcFartus.data(stack).getIntOr("boba_eblan",0)==1;}
    /** Source x/z position scores were integer block coordinates (Pos scale 1). */
    static BlockPos origin(Vec3 at){
        var block=BlockPos.containing(at);
        return block.getX()>=-16000&&block.getX()<=16000&&block.getZ()>=-16000&&block.getZ()<=16000
                ?new BlockPos(0,160,0):block;
    }
    public boolean use(ServerPlayer owner,InteractionHand hand){
        if(owner==null||!owner.isAlive()||owner.isRemoved()||owner.level()!=game.maps().level()
                ||!game.canUseGameplayItems(owner)||game.isObserver(owner)||!game.playing()
                ||game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(owner)
                ||!matches(owner.getItemInHand(hand))||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||!game.server().tickRateManager().runsNormally()||game.items().timeStop().frozen(owner)
                ||game.items().timeStop().stopped(owner.level())||owner.entityTags().contains("time_stopped")
                ||owner.entityTags().contains("nomagic_active")
                ||game.items().magic().suppressed(owner))return false;
        // /clear scans the inventory, not just the clicked hand, and precedes engine admission.
        owner.getInventory().clearOrCountMatchingItems(LbcBoba::debitMatches,false,1,owner.inventoryMenu.getCraftSlots());
        if(game.gameMode()>=3){
            for(var target:List.copyOf(owner.level().players()))
                if(target.distanceToSqr(owner)>=.0001&&game.items().wands().target(owner,target))bedwars(owner,target);
        }else game.maps().rotateMap(owner,origin(owner.position()));
        return true;
    }
    private void bedwars(ServerPlayer caster,ServerPlayer target){
        // Local ^1 is left, independent of pitch; source keeps yaw+90 and looks straight up.
        double angle=Math.toRadians(target.getYRot());var at=target.position().add(Math.cos(angle),0,Math.sin(angle));
        target.teleportTo(target.level(),at.x,at.y,at.z,TeleportMomentum.positionFlags(Set.of()),target.getYRot()+90,-90,true);
        for(int i=0;i<5;i++)LbcOverload.sound(target,target.position(),"minecraft:lbcsounds."+SOUNDS[target.getRandom().nextInt(SOUNDS.length)],1,1,16);
        drop(caster,target,"lbc:govno_lot");
        target.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE,Blocks.DIRT.defaultBlockState()),
                target.getX(),target.getY()+.5,target.getZ(),1000,1,1,1,1);
        target.level().sendParticles(ParticleTypes.SNEEZE,target.getX(),target.getY()+.5,target.getZ(),1000,1,1,1,2);
        drop(caster,target,"lbc:govno_lot_2");
    }
    private void drop(ServerPlayer caster,ServerPlayer target,String table){
        for(var stack:game.lbc().rollTable(target.level(),target.position(),target,table)){
            var item=new ItemEntity(target.level(),target.getX(),target.getY(),target.getZ(),stack);
            item.setThrower(caster);game.items().markCurrentItemEpoch(item);target.level().addFreshEntity(item);
        }
    }
}
