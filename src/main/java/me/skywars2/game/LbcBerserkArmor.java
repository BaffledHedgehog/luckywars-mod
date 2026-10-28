package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;

/** Berserker's native hurt-criterion proc; no cooldown or separate effect lifetime. */
public final class LbcBerserkArmor {
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final SoundEvent GROWL=SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:entity.wolf.growl"));
    private final GameController game;
    private final Predicate<ServerPlayer> admitted;
    private record Attempt(ServerPlayer player,boolean admitted){}
    private final Deque<Attempt> attempts=new ArrayDeque<>();

    LbcBerserkArmor(GameController game,Predicate<ServerPlayer> admitted){this.game=game;this.admitted=admitted;}

    /** Share the existing pre-hurt admission bridge, including native fatal criterion events. */
    public boolean withIncomingAttempt(ServerPlayer player,BooleanSupplier hurt){
        attempts.push(new Attempt(player,admitted.test(player)));
        try{return hurt.getAsBoolean();}finally{attempts.pop();}
    }

    public void incoming(ServerPlayer player){
        var attempt=attempts.peek();
        if(!(attempt!=null&&attempt.player()==player?attempt.admitted():admitted.test(player)))return;
        for(var slot:SLOTS){
            var stack=player.getItemBySlot(slot);
            if(stack.isEmpty()||LbcFartus.data(stack).getIntOr("berserk",0)!=1)return;
        }
        // Author: any HP can proc; only MAG blocks new grants, never existing effects.
        if(game.items().magic().beaconSuppressed(player.position())||player.getRandom().nextFloat()>=.7f)return;
        player.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,1,0),player);
        player.level().sendParticles(ParticleTypes.HEART,player.getX(),player.getY()+1,player.getZ(),3,.5,1,.5,0);
        player.level().playSound(null,player.getX(),player.getY(),player.getZ(),GROWL,SoundSource.MASTER,1,1);
        player.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,100,1),player);
    }
}
