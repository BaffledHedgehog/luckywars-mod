package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.tags.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Native tool properties, stochastic loot and genuine server item/block inputs. */
public final class NativeToolChecks {
    private NativeToolChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Native tools: "+why);}
    private static ItemStack input(WoolBucketChecks.Fixture f,LbcNativeToolItems.Kind kind,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        f.set(pos,state);f.set(pos.above(),Blocks.AIR.defaultBlockState());var stack=kind.item.getDefaultInstance();f.owner.stopUsingItem();f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"genuine native use-on input "+kind.id);return stack;
    }
    public static void run(GameController game){try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner,LbcNativeToolItems.items())){
        for(var kind:LbcNativeToolItems.Kind.values()){
            var outcomes=new HashSet<net.minecraft.world.item.enchantment.ItemEnchantments>();
            for(long seed:kind.random()?new long[]{17,29,41}:new long[]{17}){
                var stack=NativeMeleeChecks.sourceOutcome(game,f,kind.item,kind.carrier,kind.definition,seed);var defaults=kind.item.getDefaultInstance();
                require(stack.getComponents().keySet().equals(defaults.getComponents().keySet()),"static source/default component keys "+kind.id);
                for(var component:stack.getComponents())if(!kind.random()||component.type()!=DataComponents.ENCHANTMENTS)require(Objects.equals(component.value(),defaults.get(component.type())),"canonical default preserves fixed component "+kind.id+" "+component.type());
                if(kind.random()){require(!stack.getEnchantments().isEmpty(),"stochastic source applies real enchantments "+kind.id);outcomes.add(stack.getEnchantments());}
                for(var tag:kind.carrier.builtInRegistryHolder().tags().toList())require(stack.is(tag),"native tool inherits actual vanilla carrier tag "+kind.id+" "+tag);
            }
            if(kind.random())require(outcomes.size()>1,"fresh source rolls vary rather than freezing generated enchantments "+kind.id);
            require(f.leaf(kind.id).is(kind.item),"existing ready leaf has the same native identity "+kind.id);
        }
        var pos=f.owner.blockPosition().offset(0,-1,3);
        input(f,LbcNativeToolItems.Kind.HAMMER,pos,Blocks.OAK_LOG.defaultBlockState());require(f.level.getBlockState(pos).is(Blocks.STRIPPED_OAK_LOG),"native axe retains actual stripping interaction");
        input(f,LbcNativeToolItems.Kind.EFFICIENT_SHOVEL,pos,Blocks.DIRT.defaultBlockState());require(f.level.getBlockState(pos).is(Blocks.DIRT_PATH),"native shovel retains actual path interaction");
        input(f,LbcNativeToolItems.Kind.SICKLE,pos,Blocks.DIRT.defaultBlockState());require(f.level.getBlockState(pos).is(Blocks.FARMLAND),"native hoe retains actual till interaction");
        var flint=input(f,LbcNativeToolItems.Kind.ENHANCED_FLINT,pos,Blocks.STONE.defaultBlockState());require(flint.getItem() instanceof FlintAndSteelItem&&f.level.getBlockState(pos.above()).is(Blocks.FIRE),"real native FlintAndSteelItem ignites fire");
        f.set(pos.above(),Blocks.AIR.defaultBlockState());f.set(pos,Blocks.STONE.defaultBlockState());f.owner.setItemInHand(InteractionHand.MAIN_HAND,LbcNativeToolItems.Kind.SILK_PICKAXE.item.getDefaultInstance());
        var mined=Stats.BLOCK_MINED.get(Blocks.STONE);stats.before.put(mined,f.owner.getStats().getValue(mined));var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
        require(f.owner.gameMode.destroyBlock(pos)&&f.level.getBlockState(pos).isAir(),"real native player block mining");
        var drops=f.level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),item->!before.contains(item.getUUID()));for(var drop:drops)f.own(drop);require(drops.stream().anyMatch(item->item.getItem().is(Items.STONE)),"native Silk TouchX and correct-tool data produce real stone loot");
    }SkyWars2.LOGGER.info("[check] PASS native-tools (11 source prototypes/all carrier components and tags; seeded stochastic enchants, Polymer/server-codec persistence, genuine axe/shovel/hoe/fire inputs and SilkX block mining/drop)");}
}
