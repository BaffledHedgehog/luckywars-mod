package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** An item-only alias places a vanilla block without replacing that block's ordinary item. */
abstract class LbcVanillaBlockAliasItem extends BlockItem implements PolymerItem {
    private final Item vanillaProxy;
    protected LbcVanillaBlockAliasItem(Block block,Properties properties,Item vanillaProxy){
        super(block,properties);this.vanillaProxy=vanillaProxy;
    }
    /** Fabric invokes this virtual hook synchronously when the alias enters the item registry. */
    @Override public final void registerBlocks(Map<Block,Item> map,Item item){map.putIfAbsent(getBlock(),item);}
    @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return vanillaProxy;}
}
