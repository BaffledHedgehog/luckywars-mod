package me.skywars2.game;

import me.skywars2.SkyWars2;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;

/** Native accepted damage crosses the production callback; no replacement growth runtime. */
public final class EvolutionSwordChecks {
    private EvolutionSwordChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Evolution sword: "+message);}
    private static double amount(ItemStack stack){return stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().getFirst().modifier().amount();}
    private static ItemAttributeModifiers attributes(double amount){return ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.parse("minecraft:base_attack_damage"),amount,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.parse("skywars2:independent_damage"),.125,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED,new AttributeModifier(Identifier.parse("skywars2:independent_speed"),-.7,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();}
    public static void run(GameController game){
        try(var base=new ElementalGrimoireChecks.Fixture(game);var attribution=new SauvojenMeditationChecks.Attribution(game,List.of(base.owner))){
            base.initialize();var p=base.owner;
            var custom=new LinkedHashMap<net.minecraft.stats.Stat<?>,Integer>();
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);custom.put(stat,p.getStats().getValue(stat));}
            var advancement=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,Set<String>>();
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancement.put(a,done);}
            var target=new Cow(EntityTypes.COW,p.level());target.setPos(p.position().add(0,0,2));target.setNoAi(true);target.setNoGravity(true);
            require(p.level().addFreshEntity(target),"real native target");
            try{
                var loot=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/evolution_sword");
                require(loot.size()==1&&loot.getFirst().is(LbcEvolutionSwordItem.ITEM),"canonical native leaf actually rolls");
                var fresh=loot.getFirst();require(amount(fresh)==1&&fresh.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size()==1,"source damage1 only; initial speed unchanged");
                for(boolean raw:new boolean[]{false,true}){
                    var stack=raw?fresh.transmuteCopy(Items.GOLDEN_SWORD):fresh.copy();
                    if(raw){stack.set(DataComponents.CUSTOM_DATA,fresh.get(DataComponents.CUSTOM_DATA));stack.set(DataComponents.ITEM_MODEL,fresh.get(DataComponents.ITEM_MODEL));stack.set(DataComponents.LORE,fresh.get(DataComponents.LORE));stack.set(DataComponents.UNBREAKABLE,fresh.get(DataComponents.UNBREAKABLE));}
                    stack.set(DataComponents.CUSTOM_NAME,Component.literal("kept custom name"));stack.set(DataComponents.REPAIR_COST,17);stack.setDamageValue(7);
                    stack.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),3);
                    var data=LbcFartus.data(stack).copy();data.putString("private_kept","yes");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                    p.setItemInHand(InteractionHand.MAIN_HAND,stack);
                    double[] old={1,3.9216,8.8236,11.7648,14.7059,19.6079,49.0197,98.0393,210000,214748.3647};
                    String[] tiers={"f","d","c","b","a","s","ss","x","x","x"};
                    for(int i=0;i<old.length;i++){
                        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,attributes(old[i]));var expected=stack.copy();
                        target.setHealth(10);target.setInvulnerableTime(0);target.damageCooldownTime=0;
                        require(target.hurtServer(p.level(),p.damageSources().playerAttack(p),1),"actual accepted direct melee "+raw+"/"+i);
                        int score=(int)Math.floor(old[i]*10200d);double next=score*.0001d;
                        require(amount(stack)==next,"quantized source growth and scoreboard cap raw="+raw+" index="+i+" old="+old[i]+" actual="+amount(stack)+" expected="+next);
                        var expectedEntries=new ArrayList<>(expected.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers());
                        expectedEntries.set(0,new ItemAttributeModifiers.Entry(Attributes.ATTACK_DAMAGE,new AttributeModifier(Identifier.parse("minecraft:base_attack_damage"),next,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND));
                        expected.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(expectedEntries));expected.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_"+tiers[i]));
                        require(ItemStack.isSameItemSameComponents(expected,stack),"only growing damage and exact tier changed; all other modifiers/components survive raw="+raw+" index="+i+" actualTier="+stack.get(DataComponents.TOOLTIP_STYLE));
                    }
                    var capped=stack.copy();for(int j=0;j<3;j++){target.setHealth(10);target.setInvulnerableTime(0);target.damageCooldownTime=0;require(target.hurtServer(p.level(),p.damageSources().playerAttack(p),1),"repeated accepted capped hit");}
                    require(amount(stack)==214748.3647&&ItemStack.isSameItemSameComponents(capped,stack),"old scoreboard ceiling stable on repeated native hits");
                    stack.set(DataComponents.ATTRIBUTE_MODIFIERS,attributes(1));
                    for(String tag:List.of("stopper","stopper_magic")){
                        var field=new Marker(EntityTypes.MARKER,p.level());field.setPos(p.position());field.entityTags().add(tag);require(p.level().addFreshEntity(field),"real suppression field");game.items().magic().loaded(field);
                        try{boolean tech=tag.equals("stopper");require(tech?game.items().magic().mechanismSuppressed(p):game.items().magic().suppressed(p),"suppression positive control");target.setHealth(10);target.setInvulnerableTime(0);target.damageCooldownTime=0;var before=amount(stack);require(target.hurtServer(p.level(),p.damageSources().playerAttack(p),1),"native melee inside field");require(amount(stack)==(tech?before:1.02),"TECH alone suppresses; MAG permits growth");}
                        finally{game.items().magic().unloaded(field);field.discard();}
                    }
                    var before=stack.copy();target.setHealth(10);target.setInvulnerableTime(0);target.damageCooldownTime=0;
                    require(target.hurtServer(p.level(),new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),p),1),"accepted attributed spell control");
                    var summon=new Marker(EntityTypes.MARKER,p.level());target.setInvulnerableTime(0);target.damageCooldownTime=0;
                    require(target.hurtServer(p.level(),new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.PLAYER_ATTACK),summon,p),1),"accepted attributed indirect control");
                    target.setPermanentlyInvulnerable(true);require(!target.hurtServer(p.level(),p.damageSources().playerAttack(p),1),"rejected native hit");target.setPermanentlyInvulnerable(false);
                    require(ItemStack.isSameItemSameComponents(before,stack),"attributed spell/summon and rejected hit never grow");
                }
            }finally{target.discard();custom.forEach((stat,value)->p.getStats().setValue(p,stat,value));advancement.forEach((a,done)->{for(var criterion:a.value().criteria().keySet())if(done.contains(criterion))p.getAdvancements().award(a,criterion);else p.getAdvancements().revoke(a,criterion);});}
        }
        SkyWars2.LOGGER.info("[check] PASS evolution-sword (actual native/raw source loot; accepted direct melee; every tier; quantized2percent; stable scoreboard cap; all unrelated stack components/modifiers; TECH only; attributed spell/summon and rejected hit exclusion)");
    }
}
