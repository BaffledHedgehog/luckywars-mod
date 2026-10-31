package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/** The original native jigsaw structure, placed through the existing accepted terrain dispatcher. */
public final class LbcGrimoireDungeon {
    static final String PENDING="skywars2.grimoire_pending";
    static final Identifier STRUCTURE=Identifier.parse("lbc:grimoire_dungeon");
    private static final List<Identifier> TEMPLATES=List.of("balcony","center_1","dead_end","road_1","room_1","room_2","tower_1","tower_2","tower_3","tower_4","tower_5","tower_6","tower_middle","tower_stairs","tower_top","wall")
            .stream().map(n->Identifier.parse("lbc:grimoire_dungeon/"+n)).toList();
    private final GameController game;
    private Job placing;
    private int pending;
    LbcGrimoireDungeon(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){if(stack.isEmpty())return false;var data=LbcFartus.data(stack);return data.getIntOr("grimoire_dungeon",0)==1&&data.getIntOr("drop",0)==1;}
    public boolean ready(ItemEntity token){return matches(token.getItem())&&!token.isRemoved()&&token.level()==game.maps().level()&&game.playing()
            &&!game.items().hasStaleItemEpoch(token)&&!game.items().timeStop().stopped((ServerLevel)token.level());}
    int pending(){return pending;}
    /** Invoked from the native entity-add callback while a native structure slice is being placed. */
    public void capture(Entity actor){if(placing!=null&&actor.level()==placing.level){
        actor.entityTags().add(PENDING);placing.entities.add(actor);game.items().spellSources().capture(actor);
    }}
    public boolean spawn(ItemEntity token){
        if(!ready(token))return false;
        var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        if(author==null)author=game.items().cageDrops().target(token);
        LbcSpellSource.Source source=author==null?null:new LbcSpellSource.Source(token.getUUID(),author.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(author),Set.of("dungeon_mob"),true);
        var job=new Job((ServerLevel)token.level(),BlockPos.containing(token.position()),source);pending++;
        game.items().stonekivi().queuedWork(job);return true;
    }
    private final class Job implements LbcStonekivi.Work {
        final ServerLevel level;final BlockPos origin;final LbcSpellSource.Source source;final String epoch;
        final Set<Entity> entities=Collections.newSetFromMap(new IdentityHashMap<>());
        final CompletableFuture<?> preparation;
        final MapLighting.Update lighting;
        StructureStart start;List<ChunkPos> chunks;int cursor;boolean closed,writesDone;
        Job(ServerLevel level,BlockPos origin,LbcSpellSource.Source source){
            this.level=level;this.origin=origin;this.source=source;epoch=game.items().epoch();lighting=new MapLighting.Update(level);
            var futures=new ArrayList<CompletableFuture<?>>();futures.add(game.maps().prepareSpellTemplates(TEMPLATES));
            // max_distance_from_center=128, plus one chunk for native processors/jigsaw junctions.
            int x=origin.getX()>>4,z=origin.getZ()>>4;
            for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)futures.add(game.maps().prepareChunk(level,new ChunkPos(x+dx,z+dz)));
            preparation=CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
        @Override public boolean ready(){return preparation.isDone();}
        private void generate(){
            var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(STRUCTURE).orElseThrow(()->new IllegalStateException("Missing native dungeon structure"));
            var generator=level.getChunkSource().getGenerator();var randomState=level.getChunkSource().randomState();
            start=holder.value().generate(holder,level.dimension(),level.registryAccess(),generator,generator.getBiomeSource(),randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED),randomState,
                    level.getStructureTemplateManager(),level.getSeed(),new ChunkPos(origin.getX()>>4,origin.getZ()>>4),0,level,biome->true);
            if(!start.isValid())throw new IllegalStateException("Native dungeon jigsaw generation failed");
            var bounds=start.getBoundingBox();chunks=ChunkPos.rangeClosed(new ChunkPos(bounds.minX()>>4,bounds.minZ()>>4),new ChunkPos(bounds.maxX()>>4,bounds.maxZ()>>4)).toList();
            for(var pos:chunks)if(level.getChunkSource().getChunkNow(pos.x(),pos.z())==null)throw new IllegalStateException("Native dungeon exceeds prepared footprint");
        }
        @Override public boolean step(){
            if(closed)return true;
            if(!game.playing()||!game.items().currentEpoch(epoch)){cancel();return true;}
            if(game.items().timeStop().stopped(level))return false;
            long deadline=Math.max(EffectTerrain.deadlineNanos(),System.nanoTime()+1_000_000);
            try{
                preparation.join();if(start==null)generate();
                while(cursor<chunks.size()){
                    var pos=chunks.get(cursor++);var min=new BlockPos(pos.x()*16,level.getMinY(),pos.z()*16);lighting.changed(min);
                    var box=new BoundingBox(pos.x()*16,level.getMinY(),pos.z()*16,pos.x()*16+15,level.getMaxY()+1,pos.z()*16+15);
                    placing=this;
                    try(var scope=game.items().spellSources().scope(source,level)){
                        Runnable nativePlace=()->start.placeInChunk(level,level.structureManager(),level.getChunkSource().getGenerator(),level.getRandom(),box,pos);
                        if(source!=null)game.items().indirectKills().trapChanges(level,source.cause(),nativePlace);else nativePlace.run();
                    }finally{placing=null;}
                    if(System.nanoTime()>=deadline)return false;
                }
                if(!writesDone){lighting.finishWrites();writesDone=true;}
                if(!lighting.tick(deadline))return false;
                for(var actor:entities)if(!actor.isRemoved()){
                    actor.entityTags().remove(PENDING);game.items().grimoireRooms().acceptedMarker(actor);
                }
                for(var player:game.server().getPlayerList().getPlayers())player.sendSystemMessage(Component.translatable("shadow_citadel").withStyle(net.minecraft.ChatFormatting.DARK_RED,net.minecraft.ChatFormatting.BOLD));
                for(float[] sound:new float[][]{{100,1},{10,.5f},{1,1.5f}})LbcOverload.sound(level,net.minecraft.world.phys.Vec3.atLowerCornerOf(origin),"minecraft:ambient.cave",sound[0],sound[1],0,0);
                SkyWars2.LOGGER.info("Native grimoire dungeon placed {} pieces in {} chunks",start.getPieces().size(),chunks.size());
                finish();return true;
            }catch(RuntimeException error){SkyWars2.LOGGER.error("Native grimoire dungeon placement failed",error);cancel();return true;}
        }
        private void finish(){closed=true;entities.clear();pending--;}
        @Override public void cancel(){if(closed)return;lighting.cancel();for(var actor:entities)if(!actor.isRemoved())actor.discard();finish();}
    }
}
