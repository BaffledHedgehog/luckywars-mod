package me.skywars2.game;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

/** Explicit active-body images. Never copies a caster's unrelated emitted blast/ray history. */
public final class CloneComplexProjectileStates {
    private CloneComplexProjectileStates() { }
    public static boolean recognizes(Entity e) {
        return LbcBalance.managedRain(e)||LbcBalance.managedTnt(e)||LbcShulkerBow.cloneRecognizes(e)||LbcMadokaBow.cloneRecognizes(e)||LbcTrinketArrows.cloneRecognizes(e)||LbcClouds.cloneRecognizes(e)||e instanceof LbcExasperato.Charge
                ||LbcExoperso.cloneRecognizes(e)||LbcSpecialFireballs.cloneRecognizes(e)
                ||LbcHalloweenWind.cloneRecognizes(e)||LbcGovnomet.cloneRecognizes(e)
                ||e instanceof LbcPrism.Charge||LbcSyringe.cloneRecognizes(e)||LbcNeutral.managed(e);
    }
    public static boolean protectedActor(Entity e) {
        return e.entityTags().contains("skywars2.supernova")
                ||e instanceof net.minecraft.world.entity.Marker&&e.entityTags().contains("nuke_old")
                ||LbcMadokaBow.restoreJob(e)||e instanceof LbcExasperato.Charge explosive&&explosive.academyTrial
                ||e instanceof LbcPrism.Charge prism&&(prism.phase==LbcPrism.Charge.Phase.ACADEMY
                    ||prism.stack!=null&&LbcAcademy.trial(prism.stack));
    }
    /** Returns null for protected trial authority and unsupported managed body variants. */
    public static LbcCloneSkills.ActorImage capture(GameController game,Entity e) {
        if(LbcBalance.managedRain(e))return game.items().balance().cloneRain(e);
        if(LbcBalance.managedTnt(e))return game.items().balance().cloneTnt(e);
        if(LbcShulkerBow.cloneRecognizes(e))return game.items().shulkerBow().cloneImage(e);
        if(LbcMadokaBow.cloneRecognizes(e))return game.items().madokaBow().cloneImage(e);
        if(LbcTrinketArrows.cloneRecognizes(e))return game.items().trinketArrows().cloneImage(e);
        if(LbcClouds.cloneRecognizes(e))return game.items().clouds().cloneImage(e);
        if(e instanceof LbcExasperato.Charge)return game.items().exasperato().cloneImage(e);
        if(LbcExoperso.cloneRecognizes(e))return game.items().exoperso().cloneImage(e);
        if(LbcSpecialFireballs.cloneRecognizes(e))return game.items().specialFireballs().cloneImage(e);
        if(LbcHalloweenWind.cloneRecognizes(e))return game.items().halloweenWind().cloneImage(e);
        if(LbcGovnomet.cloneRecognizes(e))return game.items().govnomet().cloneImage(e);
        if(e instanceof LbcPrism.Charge)return game.items().prism().cloneImage(e);
        if(LbcSyringe.cloneRecognizes(e))return game.items().syringe().cloneImage(e);
        if(LbcNeutral.managed(e))return game.items().neutral().cloneImage(e);
        return null;
    }
    /** Shared mechanics only; every field and owner index is explicitly selected by its owner. */
    static LbcCloneSkills.ActorImage image(Consumer<Entity> install,Consumer<Entity> rollback) {
        return image(null,install,rollback);
    }
    static LbcCloneSkills.ActorImage image(Function<ServerLevel,Entity> factory,Consumer<Entity> install,Consumer<Entity> rollback) {
        return new LbcCloneSkills.ActorImage() {
            public Entity create(Entity original,ServerLevel level) {
                return factory==null?original.getType().create(level,EntitySpawnReason.COMMAND):factory.apply(level);
            }
            public void install(Entity original,Entity clone,Map<Entity,Entity> family) {
                if(family.get(original)!=clone)throw new IllegalArgumentException("Missing clone family binding");
                install.accept(clone);
            }
            public void rollback(Entity clone) { rollback.accept(clone); }
        };
    }
}
