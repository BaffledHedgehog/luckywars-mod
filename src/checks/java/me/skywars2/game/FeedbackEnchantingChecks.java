package me.skywars2.game;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import me.skywars2.game.combat.EnchantmentRules;

/** Native item tags and actual enchantment eligibility, including vanilla axe asymmetry. */
public final class FeedbackEnchantingChecks {
    private FeedbackEnchantingChecks() {}
    private static void require(boolean condition,String message){
        if(!condition)throw new IllegalStateException("Feedback enchanting: "+message);
    }
    private static void wandSource(ServerPlayer player,ItemStack stack,net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> power,boolean abilitySword){
        require(stack.has(net.minecraft.core.component.DataComponents.ENCHANTABLE),"published wand has ENCHANTABLE: "+stack.getItem());
        var unenchanted=stack.copy();unenchanted.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        require((abilitySword||EnchantmentRules.spellSource(unenchanted)) && unenchanted.has(net.minecraft.core.component.DataComponents.ENCHANTABLE)
                && unenchanted.isEnchantable(),"unenchanted wand admits native enchanting table: "+stack.getItem());
        require(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentCost(net.minecraft.util.RandomSource.create(19),2,15,unenchanted)>0,
                "actual native enchantment table has positive cost: "+stack.getItem());
        var enchanted=unenchanted.copy();enchanted.enchant(power,1);
        try(var use=abilitySword?me.skywars2.game.combat.WeaponEnchantments.usingSpell(player,enchanted):me.skywars2.game.combat.WeaponEnchantments.using(player,enchanted)){
            var cause=me.skywars2.game.combat.WeaponEnchantments.effectCause(player);
            require(cause!=null && cause.weaponAction() && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(power,cause.enchantmentWeapon())==1,
                    "immutable cast provenance retains this exact wand's Power: "+stack.getItem());
        }
    }
    public static void run(ServerPlayer player){
        var registry=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var knockback=registry.getOrThrow(Enchantments.KNOCKBACK).value();
        var sharpness=registry.getOrThrow(Enchantments.SHARPNESS).value();
        for(var item:List.of(Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.IRON_SHOVEL,Items.IRON_HOE,Items.IRON_AXE)){
            var stack=new ItemStack(item);
            require(!EnchantmentRules.weapon(stack),"ordinary tool must not be classified by attack damage: "+item);
            require(!knockback.isPrimaryItem(stack)&&!knockback.isSupportedItem(stack),"sword knockback rejected for tool: "+item);
        }
        int vanillaTools=0;
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){
            if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))continue;
            var stack=new ItemStack(item);
            if(!(stack.is(net.minecraft.tags.ItemTags.PICKAXES)||stack.is(net.minecraft.tags.ItemTags.AXES)
                    ||stack.is(net.minecraft.tags.ItemTags.SHOVELS)||stack.is(net.minecraft.tags.ItemTags.HOES)))continue;
            vanillaTools++;
            require(!EnchantmentRules.weapon(stack)&&!knockback.isPrimaryItem(stack)&&!knockback.isSupportedItem(stack),
                    "all registered native tools retain their native non-sword enchantment eligibility: "+item);
            if(stack.is(net.minecraft.tags.ItemTags.AXES))
                require(!sharpness.isPrimaryItem(stack)&&sharpness.isSupportedItem(stack),"vanilla axe anvil/table asymmetry: "+item);
        }
        require(vanillaTools>0,"vanilla tool registry tags are loaded");
        var axe=new ItemStack(Items.IRON_AXE);
        require(!sharpness.isPrimaryItem(axe)&&sharpness.isSupportedItem(axe),"native axe supports book sharpness but not table sharpness");
        require(knockback.isPrimaryItem(new ItemStack(Items.IRON_SWORD)),"native sword retains table knockback");
        // Every actual common spell dispatcher item must work even without loot-applied custom data.
        var checked=new java.util.HashSet<net.minecraft.resources.Identifier>();
        for(var route:me.skywars2.game.items.wands.LbcWands.tmtAbilities())if(checked.add(route.nativeItem())){
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(route.nativeItem()).orElseThrow().value();
            var stack=item.getDefaultInstance();
            boolean abilitySword=route.nativeItem().toString().equals("skywars2:true_terra_sword");
            require((abilitySword||EnchantmentRules.wand(stack)) && registry.getOrThrow(Enchantments.POWER).value().isPrimaryItem(stack)
                    && registry.getOrThrow(Enchantments.POWER).value().isSupportedItem(stack),"native default wand Power eligibility "+route.nativeItem());
            wandSource(player,stack,registry.getOrThrow(Enchantments.POWER),abilitySword);
        }
        require(!checked.isEmpty(),"native common wand dispatcher has registered items");
        for(var item:java.util.List.of(me.skywars2.game.items.wands.LbcMusicStaff.ITEM,me.skywars2.game.items.wands.LbcMusicStaff.ACADEMY,
                me.skywars2.game.entities.projectiles.LbcDanmaku.ITEM,me.skywars2.game.entities.projectiles.LbcDanmaku.ACADEMY,
                me.skywars2.game.entities.mobs.LbcSheep.ITEM,me.skywars2.game.entities.mobs.LbcSheep.ACADEMY_ITEM,
                me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehogItems.WAND,me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehogItems.ACADEMY,
                me.skywars2.game.items.wands.LbcFartus.ITEM,me.skywars2.game.items.wands.LbcFartus.ACADEMY,
                me.skywars2.game.items.wands.LbcUniwand.ITEM)){
            var stack=item.getDefaultInstance();
            require(EnchantmentRules.wand(stack)&&registry.getOrThrow(Enchantments.POWER).value().isPrimaryItem(stack)
                    &&registry.getOrThrow(Enchantments.POWER).value().isSupportedItem(stack),
                    "native staff Power eligibility "+item);
            wandSource(player,stack,registry.getOrThrow(Enchantments.POWER),false);
        }
        // Enumerate actual custom staff lottery entries, preserving their authored components.
        var game=me.skywars2.SkyWars2.game();var pools=game.items().allItemPools();
        var domain=me.skywars2.game.items.loot.LbcAllItemPools.Domain.CUSTOM;var types=List.of("staff");
        var slots=pools.slots(domain,types);var power=registry.getOrThrow(Enchantments.POWER).value();
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(player.level())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,player.position())
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,player)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
        int wands=0;
        for(int i=0;i<slots.size();i++){
            var output=new java.util.ArrayList<ItemStack>();
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(java.util.Optional.empty());
            pools.drawSlot(domain,types,i,context,output::add);
            for(var stack:output)if(EnchantmentRules.wand(stack)){
                wands++;
                wandSource(player,stack,registry.getOrThrow(Enchantments.POWER),false);
                require(power.isPrimaryItem(stack)&&power.isSupportedItem(stack),"Power table/anvil eligibility for real staff loot "+slots.get(i)+" item="+stack.getItem());
            }
        }
        require(wands>0,"real custom staff catalog must yield wand items");
        me.skywars2.SkyWars2.LOGGER.info("[check] feedback-enchanting native staff outputs={} catalog slots={}",wands,slots.size());
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS feedback-enchanting");
    }
}
