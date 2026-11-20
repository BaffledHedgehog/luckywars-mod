package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;

/** Real lobby requests supersede both decoding and partially applied native map work. */
public final class MapReplacementChecks {
    private static Scene active;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Map replacement: "+message);}
    private static Object field(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    public static void run(GameController game){require(active==null&&game.inLobby()&&!game.maps().busy(),"exclusive ready lobby");active=new Scene(game);active.start();}

    private static void checkStoredTerrain() {
        for(var block:List.of(Blocks.AIR,Blocks.CAVE_AIR,Blocks.VOID_AIR)) {
            var nativeState=BlockState.CODEC.encodeStart(NbtOps.INSTANCE,block.defaultBlockState()).getOrThrow();
            require(!MapManager.storedTerrain(chunkWithPalette(nativeState)),"native singleton air palette needs no full load for "+block);
        }
        var stone=BlockState.CODEC.encodeStart(NbtOps.INSTANCE,Blocks.STONE.defaultBlockState()).getOrThrow();
        require(MapManager.storedTerrain(chunkWithPalette(stone)),"native singleton stone palette must be loaded");
        var legacyAir=new CompoundTag();
        legacyAir.putString("Name","minecraft:air");
        require(!MapManager.storedTerrain(chunkWithPalette(legacyAir)),"legacy singleton Name air palette needs no full load");
        var compoundAir=new CompoundTag();
        compoundAir.putString("id","minecraft:air");
        require(!MapManager.storedTerrain(chunkWithPalette(compoundAir)),"current expanded air palette needs no full load");
        require(MapManager.storedTerrain(chunkWithPalette(new CompoundTag())),"unknown palette still falls back to a full load");
    }

    private static CompoundTag chunkWithPalette(Tag state) {
        var palette=new ListTag();
        palette.add(state);
        var blockStates=new CompoundTag();
        blockStates.put("palette",palette);
        var section=new CompoundTag();
        section.put("block_states",blockStates);
        var sections=new ListTag();
        sections.add(section);
        var chunk=new CompoundTag();
        chunk.put("sections",sections);
        return chunk;
    }

    private static final class Scene {
        final GameController game;final ServerPlayer owner;final String oldMap;final boolean oldBroken;final int started;
        final int oldMode;
        final BlockPos untracked, outside, lobby;
        final MapManager.BlockSnapshot previousOutside,lobbyBlock;
        ChunkPos outsideTicket;
        boolean addedOutsideTicket;
        net.minecraft.world.entity.item.ItemEntity outsideItem;
        final List<String> heightMaps=new ArrayList<>();
        final BlockPos topBlock;
        Path heightFixture;
        int phase;final List<net.minecraft.world.entity.item.ItemEntity> debris=new ArrayList<>();
        Scene(GameController game){
            checkStoredTerrain();
            this.game=game;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            oldMap=game.config().mapId;oldBroken=game.config().broken;oldMode=game.config().gameMode;started=game.server().getTickCount();
            require((int)field(game.maps(),"sweptMode")==oldMode,"the first startup map applied a full sweep");
            var level=game.maps().level();int edge=MapManager.OUTER_ARENA_LIMIT;
            untracked=new BlockPos(edge,level.getMaxY(),edge);outside=untracked.east();
            topBlock=new BlockPos(1,level.getMaxY(),0);
            var cells=(Map<?,?>)field(game.maps(),"lobbyVisible");
            lobby=cells.entrySet().stream().filter(entry->entry.getValue() instanceof MapManager.BlockSnapshot snapshot&&!snapshot.state().isAir())
                    .map(entry->(BlockPos)entry.getKey()).findFirst().orElseThrow();
            lobbyBlock=game.maps().snapshot(lobby);previousOutside=game.maps().snapshot(outside);
            var chunk=level.getChunk(untracked.getX()>>4,untracked.getZ()>>4);
            chunk.setBlockState(untracked,Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            chunk.setBlockState(outside,Blocks.EMERALD_BLOCK.defaultBlockState(),Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            require(!game.maps().arenaTerrainAt(untracked),"direct-written witness is outside the tracked map bounds");
        }
        void select(String id){require(game.selectMap(owner,id,false),"actual native selection accepted "+id);}
        void start(){select("60");var first=field(game.maps(),"preparation");select("61");require(first!=field(game.maps(),"preparation"),"second click replaces the in-flight preparation immediately");select("1");require(game.maps().busy()&&!game.selectedMapReady(),"latest request is the sole active operation");}
        void prepareHeightFixture() throws IOException {
            heightFixture=Files.createTempDirectory(game.server().getWorldPath(LevelResource.ROOT),".map-height-check-");
            var template=new CompoundTag();
            template.put("size",tags(IntTag.valueOf(2),IntTag.valueOf(2),IntTag.valueOf(1)));
            template.put("palette",tags(NbtUtils.writeBlockState(Blocks.DIAMOND_BLOCK.defaultBlockState())));
            var block=new CompoundTag();
            block.put("pos",tags(IntTag.valueOf(1),IntTag.valueOf(1),IntTag.valueOf(0)));
            block.putInt("state",0);
            template.put("blocks",tags(block));
            var marker=new CompoundTag();
            marker.putString("id","minecraft:marker");
            marker.put("Tags",tags(StringTag.valueOf("swrg.spawn")));
            var entity=new CompoundTag();
            entity.put("pos",tags(DoubleTag.valueOf(.5),DoubleTag.valueOf(.5),DoubleTag.valueOf(.5)));
            entity.put("blockPos",tags(IntTag.valueOf(0),IntTag.valueOf(0),IntTag.valueOf(0)));
            entity.put("nbt",marker);
            template.put("entities",tags(entity));
            NbtIo.writeCompressed(NbtUtils.addCurrentDataVersion(template),heightFixture.resolve("map.nbt"));
            registerHeightMap(topBlock.getY()-1);
            registerHeightMap(topBlock.getY());
        }
        void registerHeightMap(int originY) {
            String id=game.maps().nextLocalId(false);
            var manifest=new JsonObject();
            manifest.addProperty("id",id);
            manifest.addProperty("name","Map height check "+originY);
            manifest.addProperty("mode","skywars");
            manifest.addProperty("playerCapacity",1);
            manifest.addProperty("available",true);
            manifest.addProperty("reason","");
            var placement=new JsonObject();
            placement.addProperty("template","luckywars:local/"+id);
            placement.addProperty("integrity",1);
            placement.addProperty("seed",0);
            var origin=new JsonArray();origin.add(0);origin.add(originY);origin.add(0);
            placement.add("origin",origin);
            var placements=new JsonArray();placements.add(placement);manifest.add("placements",placements);
            game.maps().registerLocalMap(manifest,heightFixture);
            heightMaps.add(id);
        }
        private static ListTag tags(Tag... entries) {
            var list=new ListTag();
            for(var entry:entries)list.add(entry);
            return list;
        }
        void cleanupHeightFixture() throws IOException {
            for(var id:heightMaps)game.maps().unregisterMap(id);
            if(heightFixture!=null){
                Files.deleteIfExists(heightFixture.resolve("map.nbt"));
                Files.deleteIfExists(heightFixture);
                heightFixture=null;
            }
        }
        boolean tick() throws com.mojang.brigadier.exceptions.CommandSyntaxException, IOException {
            require(game.inLobby()&&game.server().getTickCount()-started<3600,"bounded lobby scene");
            if(phase==0){
                if(game.maps().streamingChangedChunks().isEmpty())return false;
                require(game.maps().busy(),"cancel after real native blocks were changed, before completion");
                select("61");phase=1;return false;
            }
            if(phase==2&&debris.isEmpty()&&game.maps().busy()
                    &&!game.maps().streamingChangedChunks().isEmpty()
                    &&((Collection<?>)field(game.maps(),"work")).isEmpty()
                    &&field(game.maps(),"journalWait")!=null){
                var at=game.maps().spawns().getFirst().position();
                for(var world:List.of(game.maps().level(),Objects.requireNonNull(game.server().getLevel(LbcHedgehogDimensions.IMPRINTED)))){
                    var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Cubes load debris"));
                    var item=new net.minecraft.world.entity.item.ItemEntity(world,at.x,at.y,at.z,stack);
                    item.setNoGravity(true);item.setPickUpDelay(100);require(world.addFreshEntity(item),"real debris after the last native paste, before map readiness");debris.add(item);
                }
            }
            if(phase==3){
                if(game.maps().busy())return false;
                require(!game.selectedMapReady()&&game.maps().spawns().isEmpty(),"manual clear keeps the arena empty until a new selection");
                require(game.maps().level().getForceLoadedChunks().contains(ChunkPos.pack(outsideTicket.x(),outsideTicket.z())),
                        "manual clear preserves independent force-loaded chunks outside the arena");
                require(outsideItem!=null&&!outsideItem.isRemoved(),"manual clear preserves loose items outside the arena: "+outsideItem.getRemovalReason());
                restoreOutsideEntity();
                checkSweep();
                restoreOutside();
                prepareHeightFixture();
                select(heightMaps.getFirst());phase=4;return false;
            }
            if(phase==4){
                if(game.maps().busy())return false;
                require(game.selectedMapReady()&&game.config().mapId.equals(heightMaps.getFirst())
                                &&game.maps().level().getBlockState(topBlock).is(Blocks.DIAMOND_BLOCK)&&game.maps().spawns().size()==1,
                        "a real saved map pastes its highest legal layer and becomes ready: "+field(game,"mapFailure"));
                select(heightMaps.getLast());phase=5;return false;
            }
            if(phase==5){
                if(game.maps().busy())return false;
                var failure=(Component)field(game,"mapFailure");
                require(!game.selectedMapReady()&&failure.getContents() instanceof TranslatableContents reason
                                &&reason.getKey().equals("skywars2.map.error.height")
                                &&((Number)reason.getArgs()[2]).intValue()==topBlock.getY()+1
                                &&((Number)reason.getArgs()[4]).intValue()==topBlock.getY(),
                        "the actual loader rejects maxY+1 with the correct inclusive limit: "+failure);
                require(game.maps().level().getBlockState(topBlock).is(Blocks.DIAMOND_BLOCK),"height rejection leaves the previous map intact");
                game.configureLobbySetting(null,"gameMode",Integer.toString(oldMode));
                require(game.selectMap(owner,oldMap,oldBroken),"restore actual original selection");phase=6;return false;
            }
            if(!game.selectedMapReady())return false;
            if(phase==1){
                require(game.config().mapId.equals("61"),"last requested map is the only ready selection");
                require(game.maps().level().getBlockState(untracked).is(Blocks.DIAMOND_BLOCK),"same-mode selections do not run the full sweep");
                imprinted(game);
                game.configureLobbySetting(null,"gameMode",oldMode==1?"2":"1");
                select("8");phase=2;return false;
            }
            if(phase==2){
                require(game.config().mapId.equals("8")&&debris.size()==2&&debris.stream().allMatch(net.minecraft.world.entity.Entity::isRemoved),"Cubes8 readiness follows removal of actual overworld and Imprinted debris");
                for(var world:List.of(game.maps().level(),Objects.requireNonNull(game.server().getLevel(LbcHedgehogDimensions.IMPRINTED)))){
                    for(var entity:world.getAllEntities())require(!(entity instanceof net.minecraft.world.entity.item.ItemEntity),"no loose items after map loading");
                    require(game.itemQueue().active(world)==0&&game.itemQueue().dormant(world)==0,"no archived debris can revive after readiness");
                }
                checkSweep();
                var level=game.maps().level();
                level.getChunk(untracked.getX()>>4,untracked.getZ()>>4)
                        .setBlockState(untracked,Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                var itemPos=outside.east(32);
                require(!game.maps().arenaTerrainAt(itemPos),"loose-item control is outside tracked and fallback cleanup");
                outsideTicket=ChunkPos.containing(itemPos);
                addedOutsideTicket=level.setChunkForced(outsideTicket.x(),outsideTicket.z(),true);
                level.getChunk(outsideTicket.x(),outsideTicket.z());
                outsideItem=new net.minecraft.world.entity.item.ItemEntity(level,itemPos.getX()+.5,itemPos.getY(),itemPos.getZ()+.5,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER));
                outsideItem.setNoGravity(true);
                outsideItem.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                outsideItem.entityTags().add("map_object");
                require(level.addFreshEntity(outsideItem),"independent loose item just outside active arena");
                require(game.server().getCommands().getDispatcher().execute("skywars clearmap",game.server().createCommandSourceStack())==1
                        &&game.maps().busy(),"operator command starts a manual full sweep");
                phase=3;return false;
            }
            require(game.config().mapId.equals(oldMap)&&game.config().broken==oldBroken,"original selection restored through the public loader");
            cleanupHeightFixture();
            require(heightMaps.stream().noneMatch(game.maps()::isLocal),"temporary height maps are removed from the catalog");
            return true;
        }
        void checkSweep(){
            require(game.maps().level().getBlockState(untracked).isAir(),"full rectangle clears an untracked block on the outer boundary");
            require(game.maps().level().getBlockState(outside).is(Blocks.EMERALD_BLOCK),"the neighboring cell outside the boundary survives");
            require(game.maps().snapshot(lobby).equals(lobbyBlock),"visible lobby block and data survive the sweep");
        }
        void restoreOutside(){game.maps().restoreSnapshot(game.maps().level(),outside,previousOutside,Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
        void restoreOutsideEntity(){
            if(outsideItem!=null&&!outsideItem.isRemoved())outsideItem.discard();
            if(addedOutsideTicket)game.maps().level().setChunkForced(outsideTicket.x(),outsideTicket.z(),false);
            addedOutsideTicket=false;
        }
    }
    public static void tick(GameController game){if(active==null)return;try{if(!active.tick())return;active=null;SkyWars2.LOGGER.info("[check] PASS map-replacement (actual rapid selections replace decoding and partial native writes; startup and mode sweeps, manual cleanup boundaries, Cubes8 loose-item cleanup in both worlds, Imprinted mirror, native maxY acceptance/maxY+1 rejection, and original selection restored)");}catch(Throwable error){if(active!=null){active.restoreOutsideEntity();for(var item:active.debris)if(!item.isRemoved())item.discard();try{active.cleanupHeightFixture();}catch(IOException cleanup){error.addSuppressed(cleanup);}}active=null;SkyWars2.LOGGER.error("[check] FAIL map-replacement",error);}}
    @SuppressWarnings("unchecked") public static void imprinted(GameController game){
        var level=game.maps().level();var target=Objects.requireNonNull(game.server().getLevel(LbcHedgehogDimensions.IMPRINTED));
        require(!game.maps().busy(),"copy completes before ready callback");
        var bounds=(List<BoundingBox>)field(game.maps(),"mapBounds");int retained=0,missing=0,other=0,sampled=0;
        search:for(var box:bounds)for(int y=box.maxY();y>=box.minY();y--)for(int x=box.minX();x<=box.maxX();x++)for(int z=box.minZ();z<=box.maxZ();z++){
            var at=new BlockPos(x,y,z);var original=level.getBlockState(at);if(original.isAir())continue;
            // Starting play releases temporary loading tickets. Read the actual persisted native
            // chunk when it has already unloaded; an absent copy still fails the block comparison.
            var chunk=target.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)chunk=target.getChunk(x>>4,z>>4);
            var copy=chunk.getBlockState(at);if(copy.isAir())missing++;else if(copy.getBlock()==original.getBlock())retained++;else other++;
            if(++sampled==5000)break search;
        }
        require(sampled>=250&&retained>sampled*.5&&missing>sampled*.1&&missing<sampled*.5&&other<sampled*.15,"current native map reproduced with broken integrity: sampled="+sampled+",retained="+retained+",missing="+missing+",other="+other);
        SkyWars2.LOGGER.info("[check] PASS map-imprinted (actual current map in Imprinted, native block comparison: sampled={}, retained={}, missing={}, other={})",sampled,retained,missing,other);
    }
}
