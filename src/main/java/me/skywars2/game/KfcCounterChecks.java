package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Synchronous native checks of the food-to-owner counter bridges. */
public final class KfcCounterChecks {
    private KfcCounterChecks() { }
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("KFC counters: "+message);
    }
    private static void checkDurationAttribution(GameController game,ServerPlayer owner) {
        var victim=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,owner.level());
        victim.setPos(owner.getX()+3,owner.getY(),owner.getZ()); victim.setNoGravity(true);
        require(owner.level().addFreshEntity(victim),"status attribution fixture enters native world");
        try {
            for(var type:List.of(MobEffects.POISON,MobEffects.WITHER)) {
                victim.removeAllEffects(); victim.setHealth(victim.getMaxHealth());
                victim.setInvulnerableTime(0); victim.damageCooldownTime=0;
                require(victim.addEffect(new MobEffectInstance(type,200,0,false,false,false),owner),"hostile effect accepted");
                var original=victim.getEffect(type);
                game.items().indirectKills().extendEffectDuration(victim,original);
                require(victim.getEffect(type)==original && original.getDuration()==201,"duration grant retains the actual native effect instance");
                victim.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0); victim.setLastHurtByMob(null);
                require(victim.getKillCredit()==null,"direct combat credit is cleared before native status tick");
                float before=victim.getHealth();
                type.value().applyEffectTick(owner.level(),victim,0);
                require(victim.getHealth()<before && victim.getKillCredit()==owner,"native delayed damage retains the original hostile effect author after extension");
            }
        } finally {
            game.items().indirectKills().clear(victim); victim.discard();
        }
    }
    public static void run(GameController game) {
        ServerPlayer player=game.server().getPlayerList().getPlayers().stream()
                .filter(game::canUseGameplayItems).findFirst().orElseThrow();
        checkDurationAttribution(game,player);
        var items=game.items(); var mana=items.mana();
        var inventory=new ArrayList<ItemStack>();
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var effects=player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var scores=new HashMap<String,Integer>();
        var names=new ArrayList<>(LbcKfcFood.sourceScoreAdditions());
        names.add("dmg_dealt");
        names.addAll(List.of("effect_boost_mana","effect_add_manamax","effect_add_manaregen","effect_add_mana","effect_remove_mana","overload_jump","overload_slam","effect_rtp_1","gigaponos","hitbullet"));
        for(String name:names) scores.put(name,mana.hudValue(player,name));
        try {
            player.closeContainer(); player.getInventory().clearContent();
            items.forget(player); player.removeAllEffects();
            int resource=mana.get(player),max=mana.maximum(player),regen=mana.baseRegen(player);
            long time=player.level().getGameTime(); int tick=game.server().getTickCount(); int id=player.getId();
            player.addEffect(new MobEffectInstance(MobEffects.SPEED,200,2,false,false,false));
            mana.boost(player,30);
            items.frost().add(player,player);
            int cold=items.frost().cold(player),coldCd=items.frost().cooldown(player);
            int shotCount=items.laserMinigun().shots(player),magnumCount=items.magnum().shots(player);
            mana.score(player,"dmg_dealt").set(37);
            items.kfcFood().applyOutcome(player,7);
            require(mana.hudValue(player,"dmg_dealt")==37,"food cannot accumulate melee-only Wrath/Vanity progress");
            require(items.reactive().cooldownTicks(player)==5,"reactive counter adds one original five-tick unit");
            require(items.timeStop().cooldownTicks(player)==1 && items.stand().cooldownTicks(player)==1
                    && items.stand().lightningCooldownTicks(player)==1,"cooldowns keep their native stored increment of one");
            require(items.homing().curseSeconds(player)==1 && items.curseTicks(player)==1,"target and skill_issue update actual owner effects from zero");
            require(items.blind().queued(player)==1,"gatling source grant enrolls exactly one volley");
            require(items.frost().cold(player)==cold+1 && items.frost().cooldown(player)==coldCd+1,"existing frost values update in their stored units");
            require(mana.boost(player)==31,"active mana boost timer increments once");
            var speed=player.getEffect(MobEffects.SPEED);
            require(speed!=null && speed.getDuration()==201 && speed.getAmplifier()==2 && !speed.isVisible() && !speed.showIcon(),"finite native duration extends with flags preserved");
            require(items.laserMinigun().shots(player)==shotCount+1 && items.magnum().shots(player)==magnumCount+1,"current player shot counters increment");
            require(items.shooters().helicopterCooldownRemaining(player)==1 && items.shooters().bossHelicopterCooldownRemaining(player)==1,"both helicopter timer owners receive their source grants");
            require(mana.get(player)==resource && mana.maximum(player)==max && mana.baseRegen(player)==regen,"counter outcome leaves mana resources for outcome nine");
            require(player.level().getGameTime()==time && game.server().getTickCount()==tick && player.getId()==id,"clock and identity stay unchanged");
            items.kfcFood().applyOutcome(player,7);
            require(mana.hudValue(player,"dmg_dealt")==37,"repeated food outcome still cannot add melee damage");
            require(items.reactive().cooldownTicks(player)==10,"repeated reactive counter grants extend the live timer");
            require(items.timeStop().cooldownTicks(player)==2 && items.stand().cooldownTicks(player)==2
                    && items.homing().curseTicks(player)==40 && items.curseTicks(player)==2 && items.blind().queued(player)==2,"repeated grants extend the same live states");
        } finally {
            items.forget(player); player.removeAllEffects();
            for(var entry:scores.entrySet()) mana.score(player,entry.getKey()).set(entry.getValue());
            for(int slot=0;slot<inventory.size();slot++) player.getInventory().setItem(slot,inventory.get(slot));
            for(var effect:effects) player.addEffect(effect);
            items.magic().inventoryChanged(player); mana.inventoryChanged(player);
            player.inventoryMenu.broadcastChanges();
        }
    }
}
