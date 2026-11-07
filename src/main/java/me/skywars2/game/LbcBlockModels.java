package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

/** One startup allocation per opaque cube; Polymer owns vanilla-state remapping and pack generation. */
final class LbcBlockModels {
    /** Predict the exact allocated note state, with the authored item model unchanged. */
    static class CubeItem extends PolymerBlockItem {
        private final BlockItemStateProperties prediction;
        CubeItem(Block block,Item.Properties properties){
            super(block,properties,Items.NOTE_BLOCK,true);
            var state=PolymerBlockUtils.getPolymerBlockState(block.defaultBlockState(),null);
            var value=BlockItemStateProperties.EMPTY;
            for(var property:state.getProperties())value=value.with(property,state);
            prediction=value;
        }
        @Override public void modifyBasePolymerItemStack(ItemStack out,ItemStack source,PacketContext context,HolderLookup.Provider lookup){
            out.set(DataComponents.BLOCK_STATE,prediction);
            out.set(DataComponents.TOOLTIP_DISPLAY,out.getOrDefault(DataComponents.TOOLTIP_DISPLAY,TooltipDisplay.DEFAULT).withHidden(DataComponents.BLOCK_STATE,true));
        }
        // Ordinary/crouched placement is predicted locally. Cube clicks handle their own exception below.
        @Override public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state,ServerPlayer player,
                InteractionHand hand,ItemStack stack,ServerLevel level,BlockHitResult hit){return false;}
    }
    abstract static class Cube extends Block implements PolymerTexturedBlock {
        Cube(Properties properties){super(properties);}
        @Override public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state,ServerPlayer player,
                InteractionHand hand,ItemStack stack,ServerLevel level,BlockHitResult hit){
            // The client's interactive note block consumes a non-crouched click before item prediction.
            return !player.isSecondaryUseActive()&&(stack.getItem() instanceof BlockItem||stack.getItem() instanceof BucketItem);
        }
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
                Player player,InteractionHand hand,BlockHitResult hit){
            if(player instanceof ServerPlayer server&&stack.getItem() instanceof BucketItem)
                return server.gameMode.useItem(server,level,stack,hand);
            return super.useItemOn(stack,state,level,pos,player,hand,hit);
        }
        @Override public void spawnDestroyByEntityParticles(Level level,Entity entity,BlockPos pos,BlockState state){
            // Keep native observer exclusion. Calling super would also run Polymer's unconditional
            // self event, duplicating vanilla's predicted destruction for creative/instant mining.
            level.levelEvent(entity,LevelEvent.PARTICLES_DESTROY_BLOCK,pos,Block.getId(state));
            if(entity instanceof ServerPlayer player&&!player.getAbilities().instabuild
                    &&PolymerBlockUtils.shouldMineServerSide(player,pos,state)){
                var client=PolymerBlockUtils.getPolymerBlockState(state,player.connection.getPacketContext());
                // Slower server-controlled mining never reaches the client's destroyBlock path.
                // A client that could insta-mine already emitted both the sound and the particles.
                if(client.getDestroyProgress(player,level,pos)<1)player.connection.send(new ClientboundLevelEventPacket(
                        LevelEvent.PARTICLES_DESTROY_BLOCK,pos,Block.getId(state),false));
            }
        }
    }
    record Allocation(Identifier serverBlock, Identifier model, BlockState clientState) {}
    private static final Map<Identifier,Allocation> ALLOCATIONS=new LinkedHashMap<>();
    private static final Map<String,String> MODELS=new LinkedHashMap<>();
    private static final Map<String,byte[]> TEXTURES=new LinkedHashMap<>();
    static {
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder->{
            MODELS.forEach(builder::addStringData);
            TEXTURES.forEach(builder::addData);
            var entries=new JsonArray();
            for(var allocation:ALLOCATIONS.values()){
                var entry=describe(allocation);entries.add(entry);
                // Retain the former diagnostic path for manual-pack consumers.
                if(allocation.serverBlock().equals(Identifier.parse("skywars2:consumed_block"))){
                    entry.addProperty("pure_black",true);
                    builder.addStringData("skywars2/consumed_block_allocation.json",entry.toString());
                }
            }
            var metadata=new JsonObject();metadata.addProperty("version",1);metadata.add("allocations",entries);
            metadata.addProperty("instruction","Use the generated Polymer resource pack, including its complete note_block blockstate file. Allocations may change with installed mods; never hardcode these states or overwrite vanilla mushroom blockstates.");
            builder.addStringData("skywars2/block_palette_allocations.json",metadata.toString());
        });
    }
    private LbcBlockModels() {}

    static BlockState fullCube(Identifier serverBlock,Identifier model){
        var previous=ALLOCATIONS.get(serverBlock);
        if(previous!=null){
            if(!previous.model().equals(model))throw new IllegalStateException("Conflicting block model for "+serverBlock);
            return previous.clientState();
        }
        var state=PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK,PolymerBlockModel.of(model));
        // FULL_BLOCK can spill into target/hive states; the common carrier must remain a note block.
        if(state==null||!state.is(Blocks.NOTE_BLOCK))
            throw new IllegalStateException("No Polymer note-block palette slot available for "+serverBlock);
        ALLOCATIONS.put(serverBlock,new Allocation(serverBlock,model,state));
        me.skywars2.SkyWars2.LOGGER.info("Custom cube palette: {} -> {} ({})",serverBlock,state,model);
        return state;
    }
    static List<Allocation> allocations(){return List.copyOf(ALLOCATIONS.values());}
    static void model(Identifier model,String json){
        MODELS.put("assets/"+model.getNamespace()+"/models/"+model.getPath()+".json",json);
    }
    static void texture(Identifier texture,byte[] png){
        TEXTURES.put("assets/"+texture.getNamespace()+"/textures/"+texture.getPath()+".png",png.clone());
    }
    private static JsonObject describe(Allocation allocation){
        var entry=new JsonObject();entry.addProperty("server_block",allocation.serverBlock().toString());
        entry.addProperty("model",allocation.model().toString());
        entry.addProperty("client_block",BuiltInRegistries.BLOCK.getKey(allocation.clientState().getBlock()).toString());
        entry.addProperty("client_state",allocation.clientState().toString());
        var state=BlockState.FULL_CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,allocation.clientState()).getOrThrow().getAsJsonObject();
        entry.add("properties",state.getAsJsonObject("properties"));
        entry.addProperty("merge_into","assets/minecraft/blockstates/note_block.json");
        return entry;
    }
}
