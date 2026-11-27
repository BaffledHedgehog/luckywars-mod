package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.*;

/** Actual native manager + actual entities, with deliberately reentrant lifecycle callbacks. */
final class EntitySectionVisibilityChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Entity section visibility: "+why);}
    private static final class Scene implements AutoCloseable,LevelCallback<Marker>{
        final GameController game;final PersistentEntitySectionManager<Marker> manager;
        final List<Marker> actors=new ArrayList<>();final Map<Marker,Integer> tracked=new IdentityHashMap<>(),untracked=new IdentityHashMap<>(),ticking=new IdentityHashMap<>(),unticking=new IdentityHashMap<>();
        final ChunkPos chunk=new ChunkPos(0,0);java.util.function.Consumer<Marker> begin=e->{},end=e->{},endTick=e->{};
        Scene(GameController game){this.game=game;manager=new PersistentEntitySectionManager<>(Marker.class,this,new EntityPersistentStorage<Marker>(){
            public CompletableFuture<ChunkEntities<Marker>> loadEntities(ChunkPos pos){return CompletableFuture.completedFuture(new ChunkEntities<>(pos,List.of()));}
            public void storeEntities(ChunkEntities<Marker> entities){}public void flush(boolean full){}
        });}
        Marker add(int y){var e=new Marker(EntityTypes.MARKER,game.maps().level());e.setPos(.5,y,.5);actors.add(e);require(manager.addNewEntity(e),"native add");return e;}
        public void onCreated(Marker e){}public void onDestroyed(Marker e){}public void onSectionChange(Marker e){}
        public void onTrackingStart(Marker e){tracked.merge(e,1,Integer::sum);begin.accept(e);}
        public void onTrackingEnd(Marker e){untracked.merge(e,1,Integer::sum);end.accept(e);}
        public void onTickingStart(Marker e){ticking.merge(e,1,Integer::sum);}
        public void onTickingEnd(Marker e){unticking.merge(e,1,Integer::sum);endTick.accept(e);}
        public void close(){begin=e->{};end=e->{};endTick=e->{};for(var e:List.copyOf(actors))if(!e.isRemoved())e.discard();manager.tick();try{manager.close();}catch(java.io.IOException error){throw new IllegalStateException(error);}}
    }
    static void run(GameController game){
        try(var s=new Scene(game)){
            var first=s.add(80);var sibling=s.add(81);var anotherSection=s.add(100);
            s.manager.updateChunkStatus(s.chunk,Visibility.TICKING);
            s.endTick=actor->{if(actor==first){sibling.discard();anotherSection.discard();}};
            s.manager.updateChunkStatus(s.chunk,Visibility.HIDDEN);
            for(var actor:List.of(first,sibling,anotherSection)){
                require(s.manager.getEntityGetter().get(actor.getUUID())==null,"no stale visible actor after removal in stop-ticking");
                require(s.untracked.getOrDefault(actor,0)==1&&s.unticking.getOrDefault(actor,0)==1,"all old tracking/ticking registrations end exactly once");
            }
        }
        try(var s=new Scene(game)){
            var first=s.add(80);var sibling=s.add(81);
            s.begin=actor->{if(actor==first){sibling.discard();s.add(82);}};
            s.manager.updateChunkStatus(s.chunk,Visibility.TICKING);
            require(s.tracked.getOrDefault(sibling,0)==0&&s.ticking.getOrDefault(sibling,0)==0,"removed sibling never starts later");
            for(var actor:s.actors)if(!actor.isRemoved())require(s.tracked.getOrDefault(actor,0)==1&&s.ticking.getOrDefault(actor,0)==1,"new actor starts once despite reentrant native add");
        }
        try(var s=new Scene(game)){
            var first=s.add(80);s.add(81);s.manager.updateChunkStatus(s.chunk,Visibility.TRACKED);
            s.end=actor->{if(actor==first)s.add(82);};
            s.manager.updateChunkStatus(s.chunk,Visibility.HIDDEN);
            for(var actor:s.actors)require(s.manager.getEntityGetter().get(actor.getUUID())==null,"new hidden sibling not published during unregister");
        }
        SkyWars2.LOGGER.info("[check] PASS entity-section-visibility (native reentrant sibling removal/addition across section visibility, exact tracking cleanup, no resurrection)");
    }
}
