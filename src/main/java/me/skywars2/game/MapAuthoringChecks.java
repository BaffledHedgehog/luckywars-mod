package me.skywars2.game;

import com.google.gson.JsonParser;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Small asynchronous real compressed-template capture and native block/entity round trip. */
public final class MapAuthoringChecks {
    private static Fixture running;
    private MapAuthoringChecks(){}
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Map authoring: "+text);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.start();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.tick()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS map-authoring");}}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    private static final class Fixture {
        final GameController game;final ServerPlayer owner;final GameChecks.WandThirdPlayerState state;final MapAuthoring authoring;
        final BlockPos source,destination;final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final List<Entity> actors=new ArrayList<>();final Set<Entity> originalEntities=Collections.newSetFromMap(new IdentityHashMap<>());
        final boolean enabled;final String prefix="_check_native_"+UUID.randomUUID();final List<String> published=new ArrayList<>();
        int phase,ticks;String pending;net.minecraft.world.scores.PlayerTeam team;boolean createdTeam;
        Fixture(GameController game){this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null&&GameController.operator(owner),"actual fixture operator");require(!game.maps().busy()&&!game.developerTools().authoring().busy(),"no competing map operation");state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());authoring=new MapAuthoring(game);enabled=game.developerMode();source=owner.blockPosition().above(10);destination=source.offset(0,0,6);owner.level().getAllEntities().forEach(originalEntities::add);}
        void start(){
            game.config().developerMode=true;
            for(var origin:List.of(source,destination))for(var pos:BlockPos.betweenClosed(origin,origin.offset(2,1,2))){require(owner.level().hasChunkAt(pos),"fixture uses already loaded chunks");blocks.put(pos.immutable(),game.maps().snapshot(owner.level(),pos));owner.level().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
            owner.level().setBlockAndUpdate(source,Blocks.STONE.defaultBlockState());owner.level().setBlockAndUpdate(source.east(),Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity)owner.level().getBlockEntity(source.east())).setItem(0,new ItemStack(Items.DIAMOND,2));
            var board=game.server().getScoreboard();team=board.getPlayerTeam("1");if(team==null){team=board.addPlayerTeam("1");createdTeam=true;}
            for(var tag:List.of("swrg.spawn","swrg.look")){var actor=new Marker(EntityTypes.MARKER,owner.level());actor.setPos(Vec3.atCenterOf(source.offset(tag.equals("swrg.look")?2:0,1,1)));actor.setYRot(67);actor.entityTags().add(tag);require(owner.level().addFreshEntity(actor),"native map marker fixture");board.addPlayerToTeam(actor.getScoreboardName(),team);actors.add(actor);}
            move(source);authoring.select(owner,0);move(source.offset(2,1,2));authoring.select(owner,1);require(authoring.selectedBounds(owner).getXsize()==3,"inclusive selection bounds");
            save(false);
        }
        void move(BlockPos pos){owner.teleportTo(owner.level(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),0,0,false);}
        void save(boolean bedwars){pending=game.maps().nextLocalId(bedwars);require(game.maps().catalog().stream().noneMatch(info->info.id().equals(pending)),"new ID is vacant");authoring.save(owner,bedwars?"bedwars":"skywars",prefix+(bedwars?" BW":" SW"));require(authoring.busy(),"native asynchronous capture admitted");}
        boolean tick(){
            if(++ticks>600)throw new IllegalStateException("Map authoring: save timeout");authoring.tick();if(authoring.busy())return false;
            verify(phase==1);published.add(pending);
            if(phase++==0){save(true);return false;}
            var restarted=new MapManager(game.server());
            try{for(var id:published)require(restarted.catalog().stream().anyMatch(info->info.id().equals(id)&&info.name().startsWith(prefix)),"restart discovers actual published map "+id);require(restarted.catalog(false).stream().noneMatch(info->info.id().equals(published.get(1)))&&restarted.catalog(true).stream().anyMatch(info->info.id().equals(published.get(1))),"restart mode catalogs stay separated");}
            finally{restarted.shutdown();}
            SkyWars2.LOGGER.info("[check] map-authoring-owned-artifacts prefix={} ids={} root={}",prefix,published,game.maps().localMapRoot());return true;
        }
        void verify(boolean bedwars){
            var directory=game.maps().localMapRoot().resolve(pending);
            try{
                require(Files.isRegularFile(directory.resolve("map.nbt")),"actual compressed template on disk");
                var manifest=JsonParser.parseString(Files.readString(directory.resolve("manifest.json"))).getAsJsonObject();
                require(manifest.get("name").getAsString().equals(prefix+(bedwars?" BW":" SW"))&&manifest.get("mode").getAsString().equals(bedwars?"bedwars":"skywars")&&manifest.get("playerCapacity").getAsInt()==1,"literal name, mode and actual marker count");
                var origin=manifest.getAsJsonArray("placements").get(0).getAsJsonObject().getAsJsonArray("origin");
                require(origin.get(0).getAsInt()==-1&&origin.get(2).getAsInt()==-1&&origin.get(1).getAsInt()==source.getY(),"centered X/Z and original authored Y");
                var nbt=NbtIo.readCompressed(directory.resolve("map.nbt"),NbtAccounter.create(8*1024*1024));
                require(nbt.getListOrEmpty("blocks").size()==2,"air excluded from native snapshot");require(nbt.getListOrEmpty("entities").size()==2,"both actual marker entities saved");
                boolean chest=false;for(var tag:nbt.getListOrEmpty("blocks"))if(tag instanceof CompoundTag block&&block.getCompoundOrEmpty("nbt").getListOrEmpty("Items").size()==1)chest=true;require(chest,"native chest inventory NBT retained");
                for(var tag:nbt.getListOrEmpty("entities")){var actor=((CompoundTag)tag).getCompoundOrEmpty("nbt");require(actor.getStringOr("Team","").equals("1"),"actual numeric BedWars team survives capture");}
                if(!bedwars){
                    var template=new StructureTemplate();template.load(owner.registryAccess().lookupOrThrow(Registries.BLOCK),nbt);
                    require(template.placeInWorld(owner.level(),destination,destination,new StructurePlaceSettings(),owner.getRandom(),2),"actual native StructureTemplate placement");
                    require(owner.level().getBlockState(destination).is(Blocks.STONE),"native block round trip");
                    require(owner.level().getBlockEntity(destination.east()) instanceof ChestBlockEntity restored&&restored.getItem(0).is(Items.DIAMOND)&&restored.getItem(0).getCount()==2,"native container inventory round trip");
                    int count=0;for(var entity:owner.level().getAllEntities())if(!originalEntities.contains(entity)&&!actors.contains(entity)&&entity instanceof Marker&&entity.position().distanceToSqr(Vec3.atCenterOf(destination))<25){actors.add(entity);count++;}require(count==2,"native map-marker entity round trip");
                }
                require(game.maps().catalog(bedwars).stream().anyMatch(info->info.id().equals(pending)&&info.custom()),"published map immediately enters correct native catalog");
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        void close(){authoring.close();for(var actor:actors){if(actor.getTeam()!=null)game.server().getScoreboard().removePlayerFromTeam(actor.getScoreboardName(),actor.getTeam());actor.discard();}if(createdTeam&&team.getPlayers().isEmpty())game.server().getScoreboard().removePlayerTeam(team);for(var entry:blocks.entrySet()){if(owner.level().getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container container)container.clearContent();game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());}game.config().developerMode=enabled;state.restore(game);}
    }
}
