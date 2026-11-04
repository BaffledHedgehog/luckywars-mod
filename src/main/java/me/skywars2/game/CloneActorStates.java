package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.EntityTypes;

/** Reviewed body factories plus independent overlays owned by the original mechanics. */
final class CloneActorStates implements LbcCloneSkills.ActorAdapter {
    private final GameController game;
    CloneActorStates(GameController game){this.game=game;}
    public boolean completeCoverage(){return true;}
    public boolean protectedActor(Entity e){
        return LbcBedwars.shopRole(e)||e.entityTags().contains("skywars2.bedwars.chest_visual")
                ||game.items().danmaku().cloneProtected(e)||CloneSimpleProjectileStates.protectedActor(e)||CloneComplexProjectileStates.protectedActor(e)
                ||CloneMobStates.protectedActor(e)||CloneOtherActorStates.protectedActor(e)
                ||CloneEncounterStates.protectedActor(e)||CloneVirtualAbilityStates.protectedActor(e)
                ||CloneRemainingStatusStates.protectedActor(e)||CloneExtraAbilityStates.protectedActor(e)
                ||CloneFinalAbilityStates.protectedActor(e)||ClonePolymorphStates.protectedActor(e)
                ||e.entityTags().contains("skywars2.ice_display")||e.entityTags().contains("skywars2.ice_crystal")
                ||e.entityTags().contains("skywars2.fartus_display");
    }
    public boolean combatBody(Entity e){
        return e instanceof LbcWtf.Chef||(CloneEncounterStates.recognizes(e)||CloneMobStates.recognizes(e))&&e instanceof net.minecraft.world.entity.LivingEntity;
    }
    public boolean gameplayActor(Entity e){
        return LbcDanmaku.managed(e)&&!game.items().danmaku().cloneProtected(e)||CloneSimpleProjectileStates.recognizes(e)&&!(e instanceof net.minecraft.world.entity.Mob)
                ||CloneComplexProjectileStates.recognizes(e)&&!(e instanceof net.minecraft.world.entity.Mob)
                ||e instanceof LbcFpvDrone.Drone||CloneVirtualAbilityStates.recognizes(e)&&!(e instanceof net.minecraft.world.entity.Mob)
                ||(CloneRemainingStatusStates.recognizesGameplay(e)||CloneExtraAbilityStates.recognizesGameplay(e))&&!(e instanceof net.minecraft.world.entity.Mob)
                ||CloneFinalAbilityStates.recognizesGameplay(e)||ClonePolymorphStates.recognizesGameplay(e);
    }
    public LbcCloneSkills.ActorImage capture(Entity source){
        // A hidden pre-form body is captured only by its own polymorph image's auxiliary transaction.
        var factories=new ArrayList<LbcCloneSkills.ActorImage>();
        if(LbcDanmaku.managed(source)&&!require(factories,game.items().danmaku().cloneImage(source)))return null;
        if(CloneSimpleProjectileStates.recognizes(source)&&!require(factories,CloneSimpleProjectileStates.capture(game,source)))return null;
        if(CloneComplexProjectileStates.recognizes(source)&&!require(factories,CloneComplexProjectileStates.capture(game,source)))return null;
        if(CloneOtherActorStates.recognizes(source)&&!require(factories,CloneOtherActorStates.capture(game,source)))return null;
        if(CloneEncounterStates.recognizes(source)&&!require(factories,CloneEncounterStates.capture(game,source)))return null;
        if(CloneVirtualAbilityStates.recognizes(source)&&!require(factories,CloneVirtualAbilityStates.captureNative(game,source)))return null;
        if(CloneExtraAbilityStates.recognizes(source)&&!require(factories,CloneExtraAbilityStates.capture(game,source)))return null;
        if(CloneFinalAbilityStates.recognizesGameplay(source)&&!require(factories,CloneFinalAbilityStates.captureNative(game,source)))return null;
        if(ClonePolymorphStates.recognizes(source)&&!require(factories,ClonePolymorphStates.capture(game,source)))return null;
        // These also own statuses on ordinary bodies, so they are visited regardless of body type.
        var overlays=new ArrayList<LbcCloneSkills.ActorImage>();
        var mobs=CloneMobStates.capture(game,source);
        if(CloneMobStates.recognizes(source)&&mobs==null)return null;
        if(mobs!=null)factories.add(mobs);
        add(overlays,CloneSimpleProjectileStates.captureStatuses(game,source));
        add(overlays,CloneStatusStates.capture(game,source));
        add(overlays,CloneRemainingStatusStates.capture(game,source));
        var all=new ArrayList<LbcCloneSkills.ActorImage>(factories);all.addAll(overlays);
        var saved=List.copyOf(all);var bodyFactories=List.copyOf(factories);
        return new LbcCloneSkills.ActorImage(){
            public Entity create(Entity original,ServerLevel level){
                for(var owner:bodyFactories){var e=owner.create(original,level);if(e!=null&&e.getClass()==original.getClass())return e;}
                return original instanceof FishingHook?new FishingHook(EntityTypes.FISHING_BOBBER,level)
                        :original.getType().create(level,EntitySpawnReason.COMMAND);
            }
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){for(var owner:saved)owner.install(original,clone,family);}
            public void rollback(Entity clone){
                RuntimeException failure=null;
                for(int i=saved.size()-1;i>=0;i--)try{saved.get(i).rollback(clone);}catch(RuntimeException error){if(failure==null)failure=error;else failure.addSuppressed(error);}
                if(failure!=null)throw failure;
            }
        };
    }
    private static boolean require(List<LbcCloneSkills.ActorImage> images,LbcCloneSkills.ActorImage image){if(image==null)return false;images.add(image);return true;}
    private static void add(List<LbcCloneSkills.ActorImage> images,LbcCloneSkills.ActorImage image){if(image!=null)images.add(image);}
}
