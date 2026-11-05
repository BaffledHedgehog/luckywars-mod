package me.skywars2.game;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The source's five ordered operations; ownership, payloads and ability lifetimes stay with their owners. */
public final class LbcPlanetoidSkill {
    @FunctionalInterface public interface DirectionOwner { void redirect(ServerPlayer holder); }
    private static final TagKey<EntityType<?>> MOBS=tag("mobs"),PROJECTILES=tag("projectiles"),RAYCASTERS=tag("raycasters");
    private static final String CAPTURED="affected_by_gravity";
    private final GameController game;
    private List<DirectionOwner> owners;
    public LbcPlanetoidSkill(GameController game){this.game=game;}
    private static TagKey<EntityType<?>> tag(String name){return TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:"+name));}

    public void tick(){
        if(!game.playing()||game.maps().busy()||game.server().tickRateManager().isFrozen()
                ||!game.server().tickRateManager().runsNormally())return;
        // Every admitted holder executes on every active tick, in server player order. No round robin cap.
        for(var owner:game.server().getPlayerList().getPlayers()){
            if(game.lbc().hasSkill(owner,21)&&!game.items().magic().suppressed(owner)
                    &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level()))pulse(owner);
        }
    }

    void pulse(ServerPlayer owner){
        var point=owner.position();
        // A resident spatial query only. Do not index every world entity or serialize entity NBT.
        List<Entity> nearby=owner.level().getEntities((Entity)null,new AABB(point,point).inflate(20),
                e->eligible(e)&&e.position().distanceToSqr(point)<=400);
        for(var body:nearby)if(mob(body)&&body.position().distanceToSqr(point)<=6.25)body.entityTags().add(CAPTURED);
        for(var body:nearby)if(mob(body)&&body.entityTags().contains(CAPTURED)
                &&body.position().distanceToSqr(point)>=225)body.entityTags().remove(CAPTURED);
        for(var body:nearby)if(physical(body)&&annulus(owner,body.position()))
            {motion(body,direction(owner,body.position(),80),.00035);nativeChanged(body);}
        for(var body:nearby)if(mob(body)&&body.entityTags().contains(CAPTURED)
                &&body.position().distanceToSqr(point)<=225)motion(body,direction(owner,body.position(),-87),.0002);
        for(var body:nearby)if(raycaster(body)&&annulus(owner,body.position())){
            var aim=direction(owner,body.position(),82);orient(body,aim);
            // Java spell bodies replace source raycasters whose forward movement is stored as native velocity.
            if(nativeRaycaster(body)){
                body.setDeltaMovement(aim.scale(body.getDeltaMovement().length()));
                if(body instanceof LbcExoperso.Charge charge)charge.aim=aim;
                nativeChanged(body);
            }
            game.items().grimoireBoss().directionChanged(body,aim);
        }
        if(owners==null)owners=List.of(game.items().wands()::planetoid,game.items().electro()::planetoid,
                game.items().homing()::planetoid,game.items().duplicating()::planetoid,
                game.items().sauvojen()::planetoid,game.items().sauvojen().stars()::planetoid,
                game.items().stand()::planetoid,game.items().godHedgehog()::planetoid,
                game.items().voidArcanum()::planetoid,game.items().specialTools()::planetoid,
                game.items().danmaku()::planetoid,game.items().pendingCombat()::planetoid,
                game.items().consuming()::planetoid,game.items().sauvojenBoss()::planetoid,
                game.items().sauvojen().meditation()::planetoid,game.items().gaycasterAcademy()::planetoid,game.items().pumpkinator()::planetoid);
        for(var service:owners)service.redirect(owner);
    }

    private void nativeChanged(Entity actor){
        game.items().iceWand().directionChanged(actor);game.items().fartus().directionChanged(actor);
        game.items().chargedSwords().directionChanged(actor);game.items().fpvDrone().directionChanged(actor);
    }

    private boolean eligible(Entity e){
        return !(e instanceof Player)&&!e.isRemoved()&&e.level() instanceof ServerLevel level&&level.hasChunkAt(e.blockPosition())
                &&!game.items().timeStop().stopped(level)&&!game.items().timeStop().frozen(e)
                &&!e.entityTags().contains("time_stopped")&&!e.isSpectator()
                &&!e.entityTags().contains("spectator")&&!e.entityTags().contains("strict_map_object")
                &&!e.entityTags().contains("magic_academy")
                &&!game.items().polymorph().concealed(e)&&!familyChild(e);
    }
    private static boolean familyChild(Entity e){
        return LbcStand.cloneRecognizes(e)||LbcGodHedgehog.cloneActorRecognizes(e)
                ||e.entityTags().contains("skywars2.pending_chaos")||LbcDanmaku.managed(e)
                ||e.entityTags().contains("skywars2.gaycaster.trial")
                ||e.entityTags().contains("skywars2.sauvojen_boss.cloud")
                ||CloneVirtualAbilityStates.protectedActor(e)||CloneFinalAbilityStates.protectedActor(e)
                ||LbcSoulless.cloneProtected(e)
                ||LbcHelicopterEncounter.cloneProtected(e)||LbcShishEncounter.cloneProtected(e)
                ||e.entityTags().contains("grimoire_boss_visual")||e.entityTags().contains("spinning_red_penis")
                ||CloneOtherActorStates.protectedActor(e)
                ||e.entityTags().contains("skywars2.ice_display")||e.entityTags().contains("skywars2.ice_crystal")
                ||e.entityTags().contains("skywars2.fartus_display");
    }
    private static boolean mob(Entity e){return !nativeRaycaster(e)&&(e instanceof net.minecraft.world.entity.Mob
            ||e.getType().builtInRegistryHolder().is(MOBS)||e instanceof LbcWtf.Chef);}
    private static boolean physical(Entity e){return !nativeRaycaster(e)&&(e instanceof net.minecraft.world.entity.projectile.Projectile
            ||e.getType().builtInRegistryHolder().is(PROJECTILES));}
    private static boolean raycaster(Entity e){return nativeRaycaster(e)||e.getType().builtInRegistryHolder().is(RAYCASTERS);}
    private static boolean nativeRaycaster(Entity e){
        // Protection against incidental knockback does not identify the original source entity type.
        // Neutral skulls/fireballs and Halloween wind charges remain source-native projectiles.
        return e instanceof LbcAir.Charge||e instanceof LbcBlind.Charge||e instanceof LbcRuby.Charge
                ||e instanceof LbcStone.Charge||e instanceof LbcTeleport.Charge||e instanceof LbcLifestealer.Charge
                ||e instanceof LbcAcidCorruption.Charge||e instanceof LbcPsycho.Charge
                ||e instanceof LbcChargedSwords.ThrownSword||e instanceof LbcFpvDrone.Drone
                ||e instanceof LbcTerra.TerraProjectile||e instanceof LbcIceWand.IceShot
                ||e instanceof LbcExasperato.Charge||e instanceof LbcExoperso.Charge
                ||e instanceof LbcGovnomet.Charge||e instanceof LbcPrism.Charge||LbcFartus.magic(e);
    }
    static boolean annulus(ServerPlayer holder,Vec3 point){double d=point.distanceToSqr(holder.position());return d>=4&&d<=225;}
    static boolean near(GameController game,ServerPlayer holder,ServerLevel level,Vec3 point){
        return holder.level()==level&&annulus(holder,point)&&level.hasChunkAt(BlockPos.containing(point))
                &&!game.items().timeStop().stopped(level);
    }
    static Vec3 direction(ServerPlayer holder,Vec3 origin,float yawOffset){
        Vec3 delta=holder.getEyePosition().subtract(origin);
        float yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90+yawOffset;
        float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));
        return Vec3.directionFromRotation(pitch,yaw);
    }
    static void orient(Entity actor,Vec3 aim){
        actor.setYRot((float)Math.toDegrees(Math.atan2(-aim.x,aim.z)));
        actor.setXRot((float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance())));actor.needsSync=true;
    }
    static void motion(Entity actor,Vec3 aim,double scale){
        // /data get Pos 10000 floors each component BEFORE /execute store applies its double scale.
        actor.setDeltaMovement(new Vec3(Math.floor(aim.x*10000)*scale,Math.floor(aim.y*10000)*scale,Math.floor(aim.z*10000)*scale));
        actor.needsSync=true;
    }
}
