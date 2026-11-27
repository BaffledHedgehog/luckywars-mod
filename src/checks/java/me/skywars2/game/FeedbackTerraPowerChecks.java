package me.skywars2.game;

import java.util.List;
import me.skywars2.game.combat.WeaponEnchantments;
import me.skywars2.game.items.wands.LbcTerra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual swept Terra projectile damage after the casting hand is replaced. */
public final class FeedbackTerraPowerChecks {
    private FeedbackTerraPowerChecks() {}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();
            if(game.items().terra().count()!=0)throw new IllegalStateException("Terra Power requires an empty projectile scene");
            Vec3 origin=f.home.add(0,12,0);var block=BlockPos.containing(origin);
            for(var p:BlockPos.betweenClosed(block.offset(-1,-1,-1),block.offset(5,3,1)))f.set(p,Blocks.AIR.defaultBlockState());
            f.move(f.owner,origin.add(-2,0,0));
            var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(origin.add(3,0,0));cow.setNoAi(true);cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);
            if(!f.level.addFreshEntity(cow))throw new IllegalStateException("Terra Power target spawn");f.own(cow);
            var wand=f.leaf("terra_wand");wand.enchant(f.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),1);
            try(var use=WeaponEnchantments.using(f.owner,wand)){
                var shot=game.items().terra().castAt(f.owner,origin.add(0,.8,0),new Vec3(1,0,0),LbcTerra.Kind.REGULAR,20);
                if(shot==null)throw new IllegalStateException("Terra Power cast failed");f.own(shot);
            }
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
            try{for(int i=0;i<4&&cow.getHealth()==1000;i++)game.items().terra().tick();
                if(Math.abs(cow.getHealth()-988f)>.001f)throw new IllegalStateException("Power I Terra expected 12 damage after hand swap, health="+cow.getHealth());
            }finally{game.items().terra().stop();}
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS feedback-terra-power");
    }
}
