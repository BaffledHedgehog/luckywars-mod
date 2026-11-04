package me.skywars2.game;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
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
import com.google.gson.JsonObject;

/** Consumed territory has its own identity; ordinary mushroom states remain ordinary terrain. */
public final class LbcConsumedBlock {
    private static final Identifier ID=Identifier.parse("skywars2:consumed_block");
    private static final Identifier MODEL=Identifier.parse("lbc:consumed_block");
    private static final String BLOCK_MODEL="{\"parent\":\"minecraft:block/cube_all\",\"ambientocclusion\":false,\"textures\":{\"all\":\"lbc:block/consumed_block\"}}";
    private static final byte[] BLACK_TEXTURE=java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGNgYGAAAAAEAAH2FzhVAAAAAElFTkSuQmCC");
    private static boolean packRegistered;
    private static final BlockState CLIENT_STATE=clientState();
    private static BlockState clientState(){
        var state=PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK,PolymerBlockModel.of(Identifier.parse("lbc:block/consumed_block")));
        if(state!=null)return state;
        me.skywars2.SkyWars2.LOGGER.warn("No Polymer full-block palette slot available for consumed territory; using native black concrete presentation");
        return Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState();
    }
    public static final Block BLOCK=Registry.register(BuiltInRegistries.BLOCK,ID,new Consumed(
            BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK).setId(ResourceKey.create(Registries.BLOCK,ID))));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,ID,new PolymerBlockItem(BLOCK,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ID)).component(DataComponents.ITEM_MODEL,MODEL),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).asItem(),true));
    private LbcConsumedBlock(){}
    public static void register(){
        if(packRegistered)return;packRegistered=true;
        var block=BuiltInRegistries.BLOCK.getKey(CLIENT_STATE.getBlock());
        var serialized=BlockState.FULL_CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,CLIENT_STATE).getOrThrow().getAsJsonObject();
        var properties=serialized.getAsJsonObject("properties");
        if(!CLIENT_STATE.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK))&&(properties==null||properties.isEmpty()))
            throw new IllegalStateException("A reserved Polymer block requires an exact, nonempty client-state variant");
        String variant=properties==null?"":properties.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .map(e->e.getKey()+"="+e.getValue().getAsString()).collect(java.util.stream.Collectors.joining(","));
        var allocation=new JsonObject();allocation.addProperty("server_block",ID.toString());allocation.addProperty("client_block",block.toString());
        allocation.addProperty("client_state",CLIENT_STATE.toString());allocation.addProperty("pure_black",!CLIENT_STATE.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK)));
        allocation.addProperty("merge_into","assets/"+block.getNamespace()+"/blockstates/"+block.getPath()+".json");
        var model=new JsonObject();model.addProperty("model","lbc:block/consumed_block");var variants=new JsonObject();variants.add(variant,model);var patch=new JsonObject();patch.add("variants",variants);allocation.add("merge_fragment",patch);
        allocation.addProperty("instruction","Prefer the generated Polymer resource pack. For a manual pack, merge only this reserved variant into the existing blockstate JSON; never replace that full file or repurpose natural server blocks. No merge is needed when pure_black is false (palette-exhaustion fallback).");
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder->{
            builder.addStringData("assets/lbc/models/block/consumed_block.json",BLOCK_MODEL);
            builder.addData("assets/lbc/textures/block/consumed_block.png",BLACK_TEXTURE);
            builder.addStringData("skywars2/consumed_block_allocation.json",allocation.toString());
        });
        me.skywars2.SkyWars2.LOGGER.info("Consumed territory native palette allocation: {} (generated pack metadata skywars2/consumed_block_allocation.json)",CLIENT_STATE);
    }
    private static final class Consumed extends Block implements PolymerBlock {
        Consumed(Properties properties){super(properties);}
        @Override public BlockState getPolymerBlockState(BlockState state,PacketContext context){return CLIENT_STATE;}
        // The consumed-state branch in the original mushroom loot explicitly emitted nothing.
        @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){return List.of();}
    }
}
