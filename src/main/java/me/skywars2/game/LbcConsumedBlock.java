package me.skywars2.game;

import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** Consumed territory has its own identity; ordinary mushroom states remain ordinary terrain. */
public final class LbcConsumedBlock {
    private static final Identifier ID=Identifier.parse("skywars2:consumed_block");
    private static final Identifier MODEL=Identifier.parse("lbc:consumed_block");
    private static final String BLOCK_MODEL="{\"parent\":\"minecraft:block/cube_all\",\"ambientocclusion\":false,\"textures\":{\"all\":\"lbc:block/consumed_block\"}}";
    private static final byte[] BLACK_TEXTURE=java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGNgYGAAAAAEAAH2FzhVAAAAAElFTkSuQmCC");
    private static final BlockState CLIENT_STATE=LbcBlockModels.fullCube(ID,Identifier.parse("lbc:block/consumed_block"));
    public static final Block BLOCK=Registry.register(BuiltInRegistries.BLOCK,ID,new Consumed(
            BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK).setId(ResourceKey.create(Registries.BLOCK,ID))));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,ID,new PolymerBlockItem(BLOCK,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ID)).component(DataComponents.ITEM_MODEL,MODEL),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).asItem(),true));
    private LbcConsumedBlock(){}
    public static void register(){
        LbcBlockModels.model(Identifier.parse("lbc:block/consumed_block"),BLOCK_MODEL);
        LbcBlockModels.texture(Identifier.parse("lbc:block/consumed_block"),BLACK_TEXTURE);
    }
    private static final class Consumed extends Block implements PolymerTexturedBlock {
        Consumed(Properties properties){super(properties);}
        @Override public BlockState getPolymerBlockState(BlockState state,PacketContext context){return CLIENT_STATE;}
        // The consumed-state branch in the original mushroom loot explicitly emitted nothing.
        @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){return List.of();}
    }
}
