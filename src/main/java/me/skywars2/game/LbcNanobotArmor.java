package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;

/** Source Nanobots counters; the shared armor owner supplies the twenty-active-tick pulse. */
public final class LbcNanobotArmor {
    static final String TIER="nanobots_tier",COOLDOWN="nanobots_coldown";
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final int[] GAINS={2,4,3,1};
    private static final DustParticleOptions[] DUST={dust(.82f,.275f,.275f),dust(.82f,.584f,.275f),
            dust(1f,.933f,0f),dust(.427f,.82f,.275f),dust(0f,.969f,.969f)};
    private final GameController game;
    private record Attempt(ServerPlayer player,boolean admitted){}
    private final Deque<Attempt> attempts=new ArrayDeque<>();

    public LbcNanobotArmor(GameController game){this.game=game;}

    private static DustParticleOptions dust(float red,float green,float blue){
        return new DustParticleOptions(((int)(red*255)<<16)|((int)(green*255)<<8)|(int)(blue*255),1f);
    }
    private boolean admitted(ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
                &&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(player)
                &&!game.isObserver(player)&&game.items().wands().validRoleTarget(player,false)
                &&LbcDimensionItems.ordinary(player)&&!game.items().wands().inputTimeStopped(player)
                &&!game.items().wands().timeStopped()&&!game.items().timeStop().stopped(player.level());
    }
    static boolean piece(ServerPlayer player,EquipmentSlot slot){
        var stack=player.getItemBySlot(slot);
        return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("nanobots",0)==1;
    }
    static boolean worn(ServerPlayer player){for(var slot:SLOTS)if(piece(player,slot))return true;return false;}

    // Author: TECH suppresses accumulation and resistance refresh; existing effects keep their native clock.
    boolean chargeAllowed(ServerPlayer player){return !game.items().magic().mechanismSuppressed(player);}
    boolean resistanceAllowed(ServerPlayer player){return !game.items().magic().mechanismSuppressed(player);}

    public int tier(ServerPlayer player){return game.items().mana().score(player,TIER).get();}
    public int cooldown(ServerPlayer player){return game.items().mana().score(player,COOLDOWN).get();}

    /** Apply resistance before gains, then full-set acceleration, then the positive-only global decay. */
    public void pulse(ServerPlayer player){
        if(!admitted(player))return;
        resist(player);
        if(worn(player)&&chargeAllowed(player)){
            boolean full=true;
            for(int i=0;i<SLOTS.length;i++){
                boolean present=piece(player,SLOTS[i]);full&=present;
                if(present&&cooldown(player)<=0&&tier(player)<=119)game.items().mana().score(player,TIER).add(GAINS[i]);
            }
            if(full)game.items().mana().score(player,COOLDOWN).add(-4);
        }
        if(cooldown(player)>0)game.items().mana().score(player,COOLDOWN).add(-1);
    }

    /** Capture pre-hurt eligibility so the actual fatal criterion can still execute its source callback. */
    public boolean withIncomingAttempt(ServerPlayer player,BooleanSupplier hurt){
        attempts.push(new Attempt(player,admitted(player)));
        try{return hurt.getAsBoolean();}finally{attempts.pop();}
    }

    /** Source cocksuckers advancement includes shield/absorption criterion events, with tier >= 1. */
    public void incoming(ServerPlayer player){
        var attempt=attempts.peek();
        boolean allowed=attempt!=null&&attempt.player()==player?attempt.admitted():admitted(player);
        if(!allowed||tier(player)<1)return;
        var tier=game.items().mana().score(player,TIER);
        if(tier.get()<=4)tier.set(0);else tier.add(-5);
        if(cooldown(player)<=20)game.items().mana().score(player,COOLDOWN).add(5);
        resist(player);
    }

    private void resist(ServerPlayer player){
        if(!resistanceAllowed(player))return;
        int value=tier(player),band=band(value);
        if(band<0)return;
        // Hidden source effect; dust remains visible and uses normal-distance particle delivery.
        player.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,40,band,false,false),player);
        player.level().sendParticles(DUST[band],player.getX(),player.getY()+1,player.getZ(),5,.3,.6,.3,0);
    }
    static int band(int value){return value<4?-1:value<12?0:value<24?1:value<40?2:value<60?3:4;}

    /** Source reset lifecycle; intentionally does not remove an unrelated native Resistance effect. */
    public void reset(ServerPlayer player){
        game.items().mana().score(player,TIER).set(0);
        game.items().mana().score(player,COOLDOWN).set(0);
    }
    /** No retained per-player state: disconnect/reload preserves the native scoreboard. */
    public void forget(ServerPlayer player){}
    public void stop(){for(var player:game.server().getPlayerList().getPlayers())reset(player);}
}
