package me.skywars2.game;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Selected-hand Steampunk glove. The shared sprint_timer belongs to LbcMoiseiBoots. */
public final class LbcSteampunkGlove {
    static final Identifier SPEED=Identifier.parse("minecraft:c1cda724-0ea2-460a-98b8-9631eafd2515");
    static final Identifier DAMAGE=Identifier.parse("minecraft:585f98b8-a8b8-4e4c-838a-f2e124831040");
    private final GameController game;
    public LbcSteampunkGlove(GameController game){this.game=game;}
    public static boolean selected(ServerPlayer player){var stack=player.getMainHandItem();return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("steampunk_sword",0)==1;}
    private boolean eligible(ServerPlayer p){
        return p!=null&&p.isAlive()&&!p.isRemoved()&&game.canUseGameplayItems(p)&&!game.awaitingRespawn(p.getUUID())
                &&!game.isObserver(p)&&game.stage()!=GameController.Stage.ENDING&&game.items().wands().validRoleTarget(p,false);
    }
    private boolean paused(ServerPlayer p){
        String dimension=p.level().dimension().identifier().toString();
        return dimension.equals("minecraft:nexus")||dimension.equals("minecraft:imprinted")||game.items().wands().timeStopped()
                ||game.items().timeStop().stopped(p.level())||game.items().wands().inputTimeStopped(p);
    }
    /** Integrate immediately AFTER moiseiBoots.tick(), which increments/reset the shared clock once. */
    public void tick(){for(var player:game.server().getPlayerList().getPlayers())tickPlayer(player);}
    public void tickPlayer(ServerPlayer p){
        if(!eligible(p)){if(p!=null)reset(p);return;}
        if(paused(p))return;
        var timer=game.items().mana().score(p,"sprint_timer");
        int elapsed=timer.get();
        if(!p.isSprinting()){
            removeModifiers(p);p.entityTags().remove("sprinted");
        }else if(game.items().magic().mechanismSuppressed(p)){
            // User: TECH cancels the owned bonus even after switching away; it cannot resume on leaving the field.
            removeModifiers(p);p.entityTags().remove("sprinted");
        }else if(selected(p)&&!game.items().actionsBlocked(p)){
                var cooldown=game.items().mana().score(p,"sprint_cooldown");
                if(cooldown.get()<=0){p.entityTags().add("sprinted");timer.set(1);elapsed=1;}
                if(p.isShiftKeyDown())p.entityTags().remove("sprinted");
                if(p.entityTags().contains("sprinted")&&elapsed<=200){
                    cooldown.set(5);
                    removeModifiers(p);
                    // Commands add permanent native attributes; their original IDs also serve native stop cleanup.
                    p.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier(SPEED,.05*elapsed,AttributeModifier.Operation.ADD_VALUE));
                    p.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(DAMAGE,1.25*elapsed,AttributeModifier.Operation.ADD_VALUE));
                    Vec3 at=p.position().add(Vec3.directionFromRotation(0,p.getYRot()-90).scale(.4))
                            .add(Vec3.directionFromRotation(p.getXRot()-90,p.getYRot()));
                    p.level().sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,1,.1,.1,.1,.2);
                    LbcOverload.sound(p,p.position(),"minecraft:lbcsounds.rocket",.6f,.8f,0);
                }
        }
        if(game.server().getTickCount()%20==0)secondPlayer(p,elapsed);
    }
    /** Shared global one-second scheduler; never recharge during any sprint, including empty-handed. */
    void secondPlayer(ServerPlayer p){
        if(!eligible(p)||paused(p))return;
        secondPlayer(p,game.items().mana().score(p,"sprint_timer").get());
    }
    private void secondPlayer(ServerPlayer p,int elapsed){
        var cooldown=game.items().mana().score(p,"sprint_cooldown");
        if(cooldown.get()>0&&elapsed<=0){
            cooldown.add(-1);if(cooldown.get()==0)p.entityTags().remove("sprinted");
        }
    }
    private static void removeModifiers(ServerPlayer p){
        var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(SPEED);
        var damage=p.getAttribute(Attributes.ATTACK_DAMAGE);if(damage!=null)damage.removeModifier(DAMAGE);
    }
    /** Purify matches source: zero sprint_timer, but do not invent a cooldown refund. */
    public void clear(ServerPlayer p){game.items().mana().score(p,"sprint_timer").set(0);removeModifiers(p);p.entityTags().remove("sprinted");}
    public void reset(ServerPlayer p){clear(p);game.items().mana().score(p,"sprint_cooldown").set(0);}
    public void stop(){for(var p:game.server().getPlayerList().getPlayers())reset(p);}
}
