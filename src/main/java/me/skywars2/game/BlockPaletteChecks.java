package me.skywars2.game;

import com.google.gson.*;
import eu.pb4.polymer.core.api.block.BlockMapper;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipFile;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Actual installed packet mapper, native placement and generated resource-pack integration. */
public final class BlockPaletteChecks {
    private BlockPaletteChecks() {}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Block palette: "+why);}
    public static void run(GameController game){
        ServerPlayer player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var level=player.level();var pos=player.blockPosition().above(4);require(level.hasChunkAt(pos),"resident native fixture cell");
        var before=game.maps().snapshot(level,pos);
        var covered=new int[2];
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)player.connection;
        try{
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                var context=provider.getPacketContext();var installed=BlockMapper.get(player.connection);var configured=BlockMapper.getDefault(context);
                var expected=Map.of(
                        "skywars2:lucky_block","minecraft:block/lucky_block",
                        "skywars2:lucky_block_neko","minecraft:block/neko_lucky_block",
                        "skywars2:lucky_block_loli","minecraft:block/lucky_block_loli",
                        "skywars2:consumed_block","lbc:block/consumed_block");
                var allocations=LbcBlockModels.allocations();require(allocations.size()==expected.size(),"only three Lucky variants and consumed full-cube allocations");
                var reserved=new HashSet<BlockState>();var servers=new HashSet<Identifier>();
                var full=new net.minecraft.world.phys.AABB(0,0,0,1,1,1);
                for(var allocation:allocations){
                    require(servers.add(allocation.serverBlock())&&Objects.equals(expected.get(allocation.serverBlock().toString()),allocation.model().toString()),"exact independent source model "+allocation);
                    require(allocation.clientState().is(Blocks.NOTE_BLOCK)&&reserved.add(allocation.clientState()),"distinct common NOTE_BLOCK palette state "+allocation);
                    var block=BuiltInRegistries.BLOCK.getValue(allocation.serverBlock());require(block instanceof PolymerBlock,"native Polymer block registered "+allocation.serverBlock());
                    var state=block.defaultBlockState();require(level.setBlock(pos,state,2|16|32)&&level.getBlockState(pos)==state,"real server placement retains custom identity "+allocation.serverBlock());
                    require(state.getCollisionShape(level,pos).bounds().equals(full)&&allocation.clientState().getCollisionShape(level,pos).bounds().equals(full),"server and client keep full-cube collision "+allocation.serverBlock());
                    require(eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment.get(level,pos)==null,"custom model has no virtual holder "+allocation.serverBlock());
                    require(((PolymerBlock)block).getPolymerBlockState(state,context)==allocation.clientState(),"native projection uses allocated model "+allocation.serverBlock());
                    require(installed.toClientSideState(state,context)==allocation.clientState()&&configured.toClientSideState(state,context)==allocation.clientState(),"actual installed/default packet mapper preserves textured allocation "+allocation.serverBlock());
                }
                int notes=0,mushrooms=0;
                for(var state:Blocks.NOTE_BLOCK.getStateDefinition().getPossibleStates()){
                    level.setBlock(pos,state,2|16|32);require(level.getBlockState(pos)==state,"every real vanilla note state remains native "+state);
                    var client=installed.toClientSideState(state,context);
                    require(client.is(Blocks.NOTE_BLOCK)&&!reserved.contains(client)&&configured.toClientSideState(state,context)==client,"vanilla note mapping cannot alias any custom model "+state+" -> "+client);notes++;
                }
                for(var block:List.of(Blocks.RED_MUSHROOM_BLOCK,Blocks.BROWN_MUSHROOM_BLOCK,Blocks.MUSHROOM_STEM))for(var state:block.getStateDefinition().getPossibleStates()){
                    require(installed.toClientSideState(state,context)==state&&configured.toClientSideState(state,context)==state,"natural mushroom state keeps its exact client identity "+state);mushrooms++;
                }
                generatedPack(allocations);
                covered[0]=notes;covered[1]=mushrooms;
            });
        }finally{game.maps().restoreSnapshot(level,pos,before,2|16|32);game.items().magic().blockChanged(pos,level.getBlockState(pos));}
        SkyWars2.LOGGER.info("[check] PASS block-palette (four distinct actual textured packet mappings, full native/client collision, no holders, {} real vanilla note states, {} natural mushroom states, exact generated model references and owned assets)",covered[0],covered[1]);
    }
    private static <T extends Comparable<T>> String value(BlockState state,Property<T> property){return property.getName(state.getValue(property));}
    private static Map<String,String> properties(BlockState state){var result=new TreeMap<String,String>();for(var property:state.getProperties())result.put(property.getName(),value(state,property));return result;}
    private static boolean valueMatches(String actual,String allowed){return actual!=null&&Arrays.asList(allowed.split("\\|",-1)).contains(actual);}
    private static boolean when(JsonElement raw,Map<String,String> properties){
        if(raw==null||raw.isJsonNull())return true;var condition=raw.getAsJsonObject();
        for(var entry:condition.entrySet()){
            if(entry.getKey().equals("OR")){boolean found=false;for(var part:entry.getValue().getAsJsonArray())found|=when(part,properties);if(!found)return false;}
            else if(entry.getKey().equals("AND")){for(var part:entry.getValue().getAsJsonArray())if(!when(part,properties))return false;}
            else if(!valueMatches(properties.get(entry.getKey()),entry.getValue().getAsString()))return false;
        }return true;
    }
    private static void models(JsonElement raw,Set<Identifier> result){
        if(raw.isJsonArray()){for(var part:raw.getAsJsonArray())models(part,result);return;}
        var model=raw.getAsJsonObject();require(model.has("model"),"generated blockstate model reference");result.add(Identifier.parse(model.get("model").getAsString()));
    }
    private static Set<Identifier> modelsFor(JsonObject root,BlockState state){
        var properties=properties(state);var result=new HashSet<Identifier>();
        if(root.has("variants"))for(var entry:root.getAsJsonObject("variants").entrySet()){
            boolean matches=true;if(!entry.getKey().isEmpty())for(var pair:entry.getKey().split(",")){var split=pair.split("=",2);require(split.length==2,"generated variant key");matches&=valueMatches(properties.get(split[0]),split[1]);}
            if(matches)models(entry.getValue(),result);
        }
        if(root.has("multipart"))for(var part:root.getAsJsonArray("multipart")){var row=part.getAsJsonObject();if(when(row.get("when"),properties))models(row.get("apply"),result);}
        return result;
    }
    private static JsonObject json(ZipFile zip,String path)throws java.io.IOException{
        var entry=zip.getEntry(path);require(entry!=null,"generated pack entry "+path);
        try(var input=zip.getInputStream(entry);var reader=new java.io.InputStreamReader(input,StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject();}
    }
    private static void generatedPack(List<LbcBlockModels.Allocation> allocations){
        var path=eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils.getMainPath();require(java.nio.file.Files.isRegularFile(path),"existing generated Polymer pack");
        try(var zip=new ZipFile(path.toFile())){
            var states=json(zip,"assets/minecraft/blockstates/note_block.json");
            for(var allocation:allocations)for(var member:Blocks.NOTE_BLOCK.getStateDefinition().getPossibleStates())
                if(member.getValue(net.minecraft.world.level.block.NoteBlock.NOTE).equals(allocation.clientState().getValue(net.minecraft.world.level.block.NoteBlock.NOTE)))
                    require(modelsFor(states,member).equals(Set.of(allocation.model())),"generated prediction-safe model for every instrument/powered variant "+allocation+" "+member);
            require(zip.getEntry("assets/skywars2/models/block/greedstone_cursed_block.json")==null,"no fabricated Greedstone terrain model is generated");
            var consumed=json(zip,"assets/lbc/models/block/consumed_block.json");require(consumed.getAsJsonObject("textures").get("all").getAsString().equals("lbc:block/consumed_block")&&zip.getEntry("assets/lbc/textures/block/consumed_block.png")!=null,"consumed model and owned texture remain in generated pack");
        }catch(java.io.IOException error){throw new IllegalStateException("Read existing generated block palette pack",error);}
    }
}
