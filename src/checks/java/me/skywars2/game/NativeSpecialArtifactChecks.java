package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** Independent source/default/carrier oracles and real native death protection in both hands. */
public final class NativeSpecialArtifactChecks {
    private NativeSpecialArtifactChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Native special artifacts: "+why);}
    public static void run(GameController game){try(var f=new WoolBucketChecks.Fixture(game,false);
            var stats=new NativeMeleeChecks.NativeStats(f.owner,LbcSpecialArtifactItems.items())){
        for(var kind:LbcSpecialArtifactItems.Kind.values()){
            var source=NativeMeleeChecks.sourceOutcome(game,f,kind.item,kind.carrier,kind.definition,17);
            require(ItemStack.matches(source.copyWithCount(1),kind.item.getDefaultInstance()),"all static native defaults match original source "+kind.id);
            require(ItemStack.matches(source,f.leaf(kind.id)),"canonical ready wrapper preserves counts/functions "+kind.id);
            for(var tag:kind.carrier.builtInRegistryHolder().tags().toList())require(source.is(tag),"all vanilla/Fabric/project carrier tags "+kind.id+" "+tag);
            if(kind.carrier==Items.TOTEM_OF_UNDYING)require(source.get(DataComponents.DEATH_PROTECTION).equals(Items.TOTEM_OF_UNDYING.components().get(DataComponents.DEATH_PROTECTION)),"real vanilla mitigation/restoration component "+kind.id);
        }
        var service=game.items().totems();require(service.actors()==0&&service.work()==0&&service.pendingRefunds()==0,"no borrowed totem work");
        f.prepareParticipants();var player=f.owner;
        for(var hand:InteractionHand.values()){
            player.getInventory().clearContent();player.removeAllEffects();player.setHealth(20);player.setAbsorptionAmount(0);player.setInvulnerableTime(0);player.damageCooldownTime=0;
            var stack=LbcSpecialArtifactItems.Kind.INFINITE_TOTEM.item.getDefaultInstance();var original=stack.copy();player.setItemInHand(hand,stack);
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());f.level.getAllEntities().forEach(before::add);
            require(player.hurtServer(f.level,f.level.damageSources().generic(),1000)&&player.isAlive()&&player.getHealth()==1&&stack.isEmpty(),"actual vanilla lethal hit consumes native protection exactly once "+hand);
            var refunds=new ArrayList<ItemEntity>();
            for(var entity:f.level.getAllEntities())if(!before.contains(entity)&&entity instanceof ItemEntity item&&!item.isRemoved()){f.own(item);refunds.add(item);}
            require(refunds.size()==1&&ItemStack.matches(refunds.getFirst().getItem(),original)&&service.pendingRefunds()==0,"one complete same-identity source refund "+hand);
            require(player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)&&player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)&&player.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION),"all native protection effects "+hand);
            refunds.forEach(Entity::discard);
        }
        service.stop();
    }SkyWars2.LOGGER.info("[check] PASS native-special-artifacts (6 complete original-carrier/default/ready/Polymer/codec oracles; full vanilla/Fabric/project tags, authored counts and both-hand native lethal protection/refund/effects)");}
}
