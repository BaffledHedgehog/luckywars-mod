package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Real native creative damage and signed mana regeneration, with participant restoration. */
public final class PlayerRuleChecks {
    private PlayerRuleChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Player rules: "+why);}
    private static final class Saved implements AutoCloseable {
        final GameController game;final ServerPlayer player;final GameChecks.WandThirdPlayerState state;
        final Map<Field,Object> profile=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),tracker=new LinkedHashMap<>();
        final java.util.List<net.minecraft.world.damagesource.CombatEntry> trackerEntries;
        final Map<net.minecraft.advancements.AdvancementHolder,java.util.Set<String>> advancements=new LinkedHashMap<>();
        final Map<Stat<Identifier>,Integer> stats=new LinkedHashMap<>();final int visual;
        Saved(GameController game,ServerPlayer player){
            this.game=game;this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());
            visual=game.items().mana().regeneration(player);
            try{
                for(var field:net.minecraft.world.food.FoodData.class.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){field.setAccessible(true);food.put(field,field.get(player.getFoodData()));}
                var p=game.players().get(player.getUUID());for(var field:p.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){
                    profile.put(field,snapshotValue(field.get(p)));
                }
                for(String name:new String[]{"hurtTime","hurtDuration","lastHurt","lastDamageSource","lastDamageStamp","lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp"}){
                    var field=LivingEntity.class.getDeclaredField(name);field.setAccessible(true);combat.put(field,field.get(player));
                }
                for(String name:new String[]{"lastDamageTime","combatStartTime","combatEndTime","inCombat","takingDamage"}){
                    var field=net.minecraft.world.damagesource.CombatTracker.class.getDeclaredField(name);field.setAccessible(true);tracker.put(field,field.get(player.getCombatTracker()));
                }
                trackerEntries=java.util.List.copyOf(combatEntries(player));
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){
                var completed=new java.util.HashSet<String>();player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(completed::add);advancements.put(advancement,completed);
            }
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
        }
        @Override public void close(){
            player.entityTags().remove("sin_greed_active");game.items().clear(player);
            try{var p=game.players().get(player.getUUID());for(var entry:profile.entrySet())entry.getKey().set(p,entry.getValue());state.restore(game);
                for(var entry:combat.entrySet())entry.getKey().set(player,entry.getValue());
                for(var entry:food.entrySet())entry.getKey().set(player.getFoodData(),entry.getValue());
                for(var entry:tracker.entrySet())entry.getKey().set(player.getCombatTracker(),entry.getValue());
                var entries=combatEntries(player);entries.clear();entries.addAll(trackerEntries);
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            game.items().mana().score(player,"manaregen_visual").set(visual);
            stats.forEach((stat,value)->player.getStats().setValue(player,stat,value));game.items().magic().inventoryChanged(player);
            advancements.forEach((advancement,completed)->{
                for(var criterion:advancement.value().criteria().keySet())if(!completed.contains(criterion))player.getAdvancements().revoke(advancement,criterion);
            });
        }
    }
    private static Object snapshotValue(Object value){
        if(value instanceof int[] array)return array.clone();
        if(value instanceof java.util.Set<?> set)return new java.util.LinkedHashSet<>(set);
        if(value instanceof java.util.List<?> list)return new java.util.ArrayList<>(list);
        if(value instanceof Map<?,?> map)return new LinkedHashMap<>(map);
        return value;
    }
    @SuppressWarnings("unchecked")
    private static java.util.List<net.minecraft.world.damagesource.CombatEntry> combatEntries(ServerPlayer player)throws ReflectiveOperationException{
        var field=net.minecraft.world.damagesource.CombatTracker.class.getDeclaredField("entries");field.setAccessible(true);
        return (java.util.List<net.minecraft.world.damagesource.CombatEntry>)field.get(player.getCombatTracker());
    }
    public static void run(GameController game){
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing()&&player!=null&&game.canUseGameplayItems(player),"active native participant");
        var mana=game.items().mana();
        try(var saved=new Saved(game,player)){
            player.getInventory().clearContent();player.inventoryMenu.getCraftSlots().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);
            player.removeAllEffects();player.entityTags().remove("sin_greed_active");game.items().clear(player);
            var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;
            Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);mana.inventoryChanged(player);game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();
            player.setGameMode(GameType.CREATIVE);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);player.setHealth(20);player.setAbsorptionAmount(0);
            player.setInvulnerableTime(0);player.damageCooldownTime=0;
            require(!player.hurtServer(player.level(),player.damageSources().generic(),4)&&player.getHealth()==20,"ordinary damage retains creative immunity");
            for(var type:java.util.List.of(DamageTypes.FELL_OUT_OF_WORLD,DamageTypes.OUTSIDE_BORDER)){
                player.setInvulnerableTime(0);player.damageCooldownTime=0;float before=player.getHealth();
                var source=new net.minecraft.world.damagesource.DamageSource(player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(type));
                require(player.hurtServer(player.level(),source,4)&&player.getHealth()==before-4&&player.isCreative()&&player.getAbilities().invulnerable,"native environmental damage reaches creative: "+type);
            }
            player.setInvulnerableTime(0);player.damageCooldownTime=0;float before=player.getHealth();
            require(player.hurtServer(player.level(),new LbcCorruptedGlyphs.WildSource(player),1)&&player.getHealth()==before-1,"Ahriman outside-border subtype reaches creative through native pipeline");
            player.entityTags().add("sin_greed_active");before=player.getHealth();
            require(!player.hurtServer(player.level(),player.damageSources().fellOutOfWorld(),4)&&player.getHealth()==before,"active Greed remains absolute");player.entityTags().remove("sin_greed_active");
            player.setGameMode(GameType.SURVIVAL);player.setHealth(20);mana.noMagic(player,0);mana.maximum(player,1000);
            for(int base:new int[]{10,-10})for(boolean reversed:new boolean[]{false,true}){
                mana.baseRegen(player,base);mana.reverse(player,reversed?10:0);mana.set(player,500);mana.tickPlayer(player);
                int expected=reversed?-base:base;
                require(mana.get(player)==500+expected&&mana.effectiveRegeneration(player)==expected,"native signed regeneration "+base+" reversed="+reversed);
                String hud=mana.hud(player).getString();require(hud.contains(expected<0?"\u00a7a-":"\u00a7a+")&&!hud.contains("+-"),"HUD displays actual sign without a double sign");
            }
            mana.baseRegen(player,-10);mana.reverse(player,10);mana.set(player,995);mana.tickPlayer(player);require(mana.get(player)==1000,"inverted negative regeneration caps at maximum");
            mana.set(player,1100);mana.tickPlayer(player);require(mana.get(player)==1100,"positive regeneration preserves an existing excess");
            mana.baseRegen(player,10);mana.reverse(player,10);mana.tickPlayer(player);require(mana.get(player)==1090,"plague drains even above maximum");
        }
        SkyWars2.LOGGER.info("[check] PASS player-rules (native creative environmental damage, Greed, signed plague regeneration and HUD)");
    }
}
