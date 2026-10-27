package me.skywars2.game;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.List;

/** Current Traveller armor; native effects and the canonical airborne score own all player state. */
public final class LbcTraveller {
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final List<Holder<MobEffect>> CLEANSABLE=List.of(MobEffects.INSTANT_DAMAGE,MobEffects.LEVITATION,
            MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.POISON,MobEffects.SLOWNESS,MobEffects.UNLUCK,
            MobEffects.WATER_BREATHING,MobEffects.WEAKNESS,MobEffects.WITHER,MobEffects.BAD_OMEN,
            MobEffects.BLINDNESS,MobEffects.GLOWING,MobEffects.HUNGER,MobEffects.DARKNESS);
    private final GameController game;
    // One shared active cadence, not a private per-player resource or replacement fall timer.
    private int phase;
    public LbcTraveller(GameController game){this.game=game;}
    private boolean admitted(ServerPlayer owner){
        return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
                &&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&game.stage()!=GameController.Stage.ENDING
                // Stun and ice block active input, while equipped armor keeps its passive effects.
                &&game.items().wands().validRoleTarget(owner,false)
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level());
    }
    private static boolean piece(ServerPlayer owner,EquipmentSlot slot){
        var stack=owner.getItemBySlot(slot);return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("jumpboost",0)==1;
    }
    private static boolean fullSet(ServerPlayer owner){for(var slot:ARMOR)if(!piece(owner,slot))return false;return true;}
    /** Integration calls this before EnderWiki increments/resets the canonical falltimer score. */
    public void tick(){
        var level=game.maps().level();
        if(level==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        phase=(phase+1)%600;
        boolean tenSeconds=phase%200==0,fifteenSeconds=phase%300==0;
        for(var owner:game.server().getPlayerList().getPlayers()){
            if(!admitted(owner))continue;
            if(!owner.onGround()&&piece(owner,EquipmentSlot.CHEST)&&game.items().mana().score(owner,"falltimer").get()>=20
                    &&!game.items().magic().beaconSuppressed(owner.position()))
                owner.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,20,0),owner);
            // Author classifies the full-set cleanse and buff as magic, like the individual passives.
            if(tenSeconds&&fullSet(owner)&&!game.items().magic().beaconSuppressed(owner.position())){
                for(int draw=0;draw<6;draw++)cleanse(game,owner,owner.getRandom().nextInt(20)+1);
                buff(owner,owner.getRandom().nextInt(11)+1);
            }
            if(fifteenSeconds&&piece(owner,EquipmentSlot.HEAD)&&!game.items().magic().beaconSuppressed(owner.position())){
                owner.addEffect(ItemEffectLevels.create(MobEffects.SATURATION,1,1),owner);
                game.items().mana().score(owner,"food_count").add(1);
            }
        }
    }
    /** One source popit_2 result; clears the whole chosen effect, including every hidden layer. */
    static void cleanse(GameController game,ServerPlayer owner,int outcome){
        if(outcome<1||outcome>20)throw new IllegalArgumentException("Popit cleanse outcome "+outcome);
        // Source deliberately sends this raw loud MASTER sound only to its owner, once per draw (six for Traveller).
        var at=owner.position();
        owner.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(
                Identifier.parse("minecraft:block.bubble_column.bubble_pop"))),SoundSource.MASTER,
                at.x,at.y,at.z,100000,1,owner.getRandom().nextLong()));
        if(outcome<=15){owner.removeEffect(CLEANSABLE.get(outcome-1));return;}
        switch(outcome){
            case 16->game.items().psycho().clearStatus(owner,"lookback");
            case 17->game.items().psycho().clearStatus(owner,"piss");
            case 18->game.items().psycho().clearStatus(owner,"scared");
            case 19->{game.items().electro().clearEffects(owner);game.items().mana().score(owner,"shock").set(0);}
            case 20->game.items().psycho().clearStatus(owner,"twitching");
            default->throw new IllegalArgumentException("Popit cleanse outcome "+outcome);
        }
    }
    /** The seventh independent source draw; no bonus feedback or synthetic direct heal. */
    static void buff(ServerPlayer owner,int outcome){
        var effect=switch(outcome){
            case 1->ItemEffectLevels.create(MobEffects.ABSORPTION,1200,4);
            case 2->ItemEffectLevels.create(MobEffects.FIRE_RESISTANCE,1200,0);
            case 3->ItemEffectLevels.create(MobEffects.HASTE,600,1);
            case 4->ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,1,1);
            case 5->ItemEffectLevels.create(MobEffects.INVISIBILITY,600,1,false,false,false);
            case 6->ItemEffectLevels.create(MobEffects.LUCK,200,0);
            case 7->ItemEffectLevels.create(MobEffects.NIGHT_VISION,2400,0);
            case 8->ItemEffectLevels.create(MobEffects.REGENERATION,200,1);
            case 9->ItemEffectLevels.create(MobEffects.RESISTANCE,400,1);
            case 10->ItemEffectLevels.create(MobEffects.STRENGTH,200,0);
            case 11->ItemEffectLevels.create(MobEffects.WATER_BREATHING,600,0);
            default->throw new IllegalArgumentException("Traveller buff outcome "+outcome);
        };
        owner.addEffect(effect,owner);
    }
    public void stop(){phase=0;}
}
