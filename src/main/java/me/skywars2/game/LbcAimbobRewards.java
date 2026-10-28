package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Original Aimbob reward helmet and apple, attached to native equipment/consumption. */
public final class LbcAimbobRewards {
    private final GameController game;
    private final Map<ServerPlayer,Integer> phases=new IdentityHashMap<>();
    private String epoch;
    public LbcAimbobRewards(GameController game){this.game=game;}
    private boolean eligible(ServerPlayer p){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&game.canUseGameplayItems(p)
            &&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)&&LbcDimensionItems.ordinary(p);}
    private boolean admitted(ServerPlayer p){return eligible(p)&&!game.items().timeStop().stopped(p.level())
            &&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(p);}
    private static boolean flag(ItemStack stack,String name){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr(name,0)==1;}
    public void tickPlayer(ServerPlayer p){
        if(!game.items().currentEpoch(epoch)){phases.clear();epoch=game.items().epoch();}
        if(!eligible(p)){forget(p);return;}if(!admitted(p))return;
        int phase=(phases.getOrDefault(p,0)+1)%20;phases.put(p,phase);
        // (user) TECH prevents new grants/cleansing; already granted night vision keeps its duration.
        if(phase!=0||!flag(p.getItemBySlot(EquipmentSlot.HEAD),"night_vision")||game.items().magic().mechanismSuppressed(p))return;
        p.addEffect(ItemEffectLevels.create(MobEffects.NIGHT_VISION,300,0),p);
        p.removeEffect(MobEffects.DARKNESS);p.removeEffect(MobEffects.BLINDNESS);p.removeEffect(MobEffects.NAUSEA);
    }
    /** Called after vanilla consume_item, before its own ordinary nutrition/effects/debit. */
    public void consumed(ServerPlayer p,ItemStack stack){
        if(!admitted(p)||!flag(stack,"aimbobapple"))return;
        p.addEffect(ItemEffectLevels.create(MobEffects.HEALTH_BOOST,6000,9),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,60,127),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,100,4),p);
        // /effect give uses one native tick for instantaneous effects, not twenty ticks.
        p.addEffect(ItemEffectLevels.create(MobEffects.SATURATION,1,127),p);
        // Native consumption already repeats per item: the legacy hidden advancement/revoke loop is unnecessary.
    }
    public void inventoryChanged(ServerPlayer p){if(!eligible(p))forget(p);}
    public void forget(ServerPlayer p){phases.remove(p);}
    public void stop(){phases.clear();epoch=null;}
}
