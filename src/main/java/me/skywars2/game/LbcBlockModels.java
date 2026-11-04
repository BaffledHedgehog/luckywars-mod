package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** One startup allocation per opaque cube; Polymer owns vanilla-state remapping and pack generation. */
final class LbcBlockModels {
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
