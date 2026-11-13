package me.skywars2.game;

import net.minecraft.world.entity.Entity;
import java.util.Map;
import java.util.function.Consumer;

/** Explicit operation-local body images. Native state and causal ownership are installed by the caller. */
final class CloneSimpleProjectileStates {
    private CloneSimpleProjectileStates() { }
    static boolean recognizes(Entity actor) {
        return actor instanceof LbcAir.Charge
                || actor instanceof LbcBlind.Charge
                || actor instanceof LbcRuby.Charge
                || actor instanceof LbcStone.Charge
                || actor instanceof LbcTeleport.Charge
                || actor instanceof LbcLifestealer.Charge
                || actor instanceof LbcAcidCorruption.Charge
                || actor instanceof LbcPsycho.Charge
                || actor instanceof LbcChargedSwords.ThrownSword
                || actor instanceof LbcTerra.TerraProjectile || actor instanceof LbcIceWand.IceShot
                || actor instanceof LbcFartus.Shot || LbcPsycho.cloneVex(actor);
    }
    static boolean protectedActor(Entity actor) {
        return actor instanceof LbcAir.Charge air && air.trial
                || actor instanceof LbcBlind.Charge blind && blind.trial
                || actor instanceof LbcIceWand.IceShot ice && ice.initiatingStack!=null && LbcAcademy.trial(ice.initiatingStack);
    }
    static LbcCloneSkills.ActorImage capture(GameController game,Entity actor) {
        if(protectedActor(actor)) return null;
        if(actor instanceof LbcAir.Charge source) {
            var age=source.age;
            var sourceLifetime=source.sourceLifetime;
            var flyingStyle=source.flyingStyle;
            var emitterStyle=source.emitterStyle;
            var mode=source.mode;
            var origin=source.origin;
            return image(clone->{var target=(LbcAir.Charge)clone;
                target.age=age;
                target.sourceLifetime=sourceLifetime;
                target.flyingStyle=flyingStyle;
                target.emitterStyle=emitterStyle;
                target.mode=mode;
                target.origin=origin;
            },clone->game.items().air().unloaded(clone));
        }
        if(actor instanceof LbcBlind.Charge source) {
            var rainDrop=source.rainDrop;
            var age=source.age;
            var lifetime=source.lifetime;
            var phantom=source.phantom;
            var emitterStyle=source.emitterStyle;
            return image(clone->{var target=(LbcBlind.Charge)clone;
                target.rainDrop=rainDrop;
                target.age=age;
                target.lifetime=lifetime;
                target.phantom=phantom;
                target.emitterStyle=emitterStyle;
            },clone->game.items().blind().unloaded(clone));
        }
        if(actor instanceof LbcRuby.Charge source) {
            var age=source.age;
            var sourceLifetime=source.sourceLifetime;
            return image(clone->{var target=(LbcRuby.Charge)clone;
                target.age=age;
                target.sourceLifetime=sourceLifetime;
            },clone->game.items().ruby().unloaded(clone));
        }
        if(actor instanceof LbcStone.Charge source) {
            var age=source.age;
            var lifetime=source.lifetime;
            return image(clone->{var target=(LbcStone.Charge)clone;
                target.age=age;
                target.lifetime=lifetime;
            },clone->game.items().stone().unloaded(clone));
        }
        if(actor instanceof LbcTeleport.Charge source) {
            var age=source.age;
            var sourceLifetime=source.sourceLifetime;
            var flyingStyle=source.flyingStyle;
            var mode=source.mode;
            return image(clone->{var target=(LbcTeleport.Charge)clone;
                target.age=age;
                target.sourceLifetime=sourceLifetime;
                target.flyingStyle=flyingStyle;
                target.mode=mode;
            },clone->game.items().teleport().unloaded(clone));
        }
        if(actor instanceof LbcLifestealer.Charge source) {
            var age=source.age;
            var lifetime=source.lifetime;
            return image(clone->{var target=(LbcLifestealer.Charge)clone;
                target.age=age;
                target.lifetime=lifetime;
            },clone->game.items().lifestealer().unloaded(clone));
        }
        if(actor instanceof LbcAcidCorruption.Charge source) {
            var ball=source.ball;
            var age=source.age;
            var terrainCause=source.terrainCause;
            return image(clone->{var target=(LbcAcidCorruption.Charge)clone;
                target.ball=ball;
                target.age=age;
                target.terrainCause=terrainCause;
            },clone->game.items().acidCorruption().unloaded(clone));
        }
        if(actor instanceof LbcPsycho.Charge source) {
            var age=source.age;
            var lifetime=source.lifetime;
            var invisible=source.invisible;
            var spray=source.spray;
            var noStack=source.noStack;
            return image(clone->{var target=(LbcPsycho.Charge)clone;
                target.age=age;
                target.lifetime=lifetime;
                target.invisible=invisible;
                target.spray=spray;
                target.noStack=noStack;
            },clone->game.items().psycho().unloaded(clone));
        }
        if(actor instanceof LbcChargedSwords.ThrownSword source) {
            var age=source.age;
            var grounded=source.grounded;
            var embeddedBlock=source.embeddedBlock;
            return image(clone->{var target=(LbcChargedSwords.ThrownSword)clone;
                target.age=age;
                target.grounded=grounded;
                target.embeddedBlock=embeddedBlock;
            },clone->game.items().chargedSwords().unloaded(clone));
        }
        if(actor instanceof LbcTerra.TerraProjectile terra) return game.items().terra().cloneImage(terra);
        if(actor instanceof LbcIceWand.IceShot ice) return game.items().iceWand().cloneImage(ice);
        if(actor instanceof LbcFartus.Shot fartus) return game.items().fartus().cloneImage(fartus);
        return game.items().psycho().cloneVexImage(actor);
    }
    static LbcCloneSkills.ActorImage captureStatuses(GameController game,Entity actor) {
        var psycho=game.items().psycho().cloneStatusImage(actor);
        var death=game.items().acidCorruption().cloneStatusImage(actor);
        if(psycho==null)return death;
        if(death==null)return psycho;
        return new LbcCloneSkills.ActorImage() {
            public void install(Entity original,Entity clone,Map<Entity,Entity> family) {
                psycho.install(original,clone,family);death.install(original,clone,family);
            }
            public void rollback(Entity clone) {
                try{death.rollback(clone);}finally{psycho.rollback(clone);}
            }
        };
    }
    private static LbcCloneSkills.ActorImage image(Consumer<Entity> install,Consumer<Entity> rollback) {
        return new LbcCloneSkills.ActorImage() {
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){install.accept(clone);}
            public void rollback(Entity clone){rollback.accept(clone);}
        };
    }
}
