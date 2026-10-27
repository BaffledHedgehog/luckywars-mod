package me.skywars2.game;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Reusable standalone Pop-It: exactly one source27 draw, separate from Traveller's source20 table. */
public final class LbcPopit {
    private static final String ADVANCEMENT="lbc:true_advancements/accesories/popit";
    private static final List<Holder<MobEffect>> EFFECTS=List.of(MobEffects.INSTANT_DAMAGE,MobEffects.LEVITATION,
            MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.POISON,MobEffects.SLOWNESS,MobEffects.UNLUCK,
            MobEffects.WATER_BREATHING,MobEffects.WEAKNESS,MobEffects.WITHER,MobEffects.BAD_OMEN,
            MobEffects.BLINDNESS,MobEffects.GLOWING,MobEffects.HUNGER,MobEffects.DARKNESS);
    private final GameController game;
    LbcPopit(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("popit",0)==1;}

    public boolean use(ServerPlayer owner,InteractionHand hand){
        if(owner==null||!owner.isAlive()||owner.isRemoved()||owner.level()!=game.maps().level()
                ||!game.canUseGameplayItems(owner)||game.isObserver(owner)||game.stage()==GameController.Stage.ENDING
                ||!game.items().wands().validRoleTarget(owner,false)||!matches(owner.getItemInHand(hand))
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||owner.entityTags().contains("nomagic_active")
                ||game.items().magic().artifactSuppressedAt(owner.level(),owner.position()))return false;
        // Source sound precedes the one random draw; beacons and the personal nomagic timer are not use gates.
        var at=owner.position();
        owner.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(
                Identifier.parse("minecraft:block.bubble_column.bubble_pop"))),SoundSource.MASTER,
                at.x,at.y,at.z,100000,1,owner.getRandom().nextLong()));
        clearOutcome(game,owner,owner.getRandom().nextInt(27)+1);
        return true;
    }

    /** Source outcomes are unconditional clears; missing statuses never cause a second draw or sound. */
    static void clearOutcome(GameController game,ServerPlayer owner,int outcome){
        if(outcome<1||outcome>27)throw new IllegalArgumentException("Pop-It outcome "+outcome);
        if(outcome<=15){
            var type=EFFECTS.get(outcome-1);var active=owner.getEffect(type);
            // Source examines only the active instance, not an infinite layer hidden under a finite stronger one.
            if((outcome==1||outcome==5||outcome==10)&&active!=null&&active.isInfiniteDuration())
                game.items().mana().award(owner,ADVANCEMENT);
            owner.removeEffect(type);
            return;
        }
        switch(outcome){
            case 16->{
                if(game.items().mana().score(owner,"delayed_death").get()>0)game.items().mana().award(owner,ADVANCEMENT);
                game.items().acidCorruption().clear(owner);
            }
            case 17->game.items().mana().score(owner,"dimensional_shifting").set(0);
            case 18->game.items().psycho().clearStatus(owner,"lookback");
            case 19->game.items().mana().noMagic(owner,0);
            case 20->game.items().psycho().clearStatus(owner,"piss");
            case 21->game.items().mana().reverse(owner,0);
            case 22->game.items().psycho().clearStatus(owner,"scared");
            case 23->{game.items().electro().clearEffects(owner);game.items().mana().score(owner,"shock").set(0);}
            // These two curses have native producers and ownership, so raw compatibility-score resets are insufficient.
            case 24->game.items().clearTurnCurse(owner);
            case 25->game.items().homing().clearCurse(owner);
            case 26->game.items().psycho().clearStatus(owner,"twitching");
            case 27->{
                if(owner.entityTags().contains("greed_cursed"))game.items().mana().award(owner,ADVANCEMENT);
                owner.entityTags().remove("greed_cursed");
            }
            default->throw new IllegalArgumentException("Pop-It outcome "+outcome);
        }
    }
}
