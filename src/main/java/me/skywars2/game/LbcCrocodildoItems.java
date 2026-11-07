package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

final class LbcCrocodildoItems {
    static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:crocodildo_encounter"),new SimplePolymerItem(
            LbcUtilityItemPrototypes.properties("skywars2:crocodildo_encounter","/data/lbc/loot_table/crocodildo.json",Items.PAPER,64),Items.PAPER,true));
    static final Item NUKE=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:nuke"),new SimplePolymerItem(
            LbcUtilityItemPrototypes.properties("skywars2:nuke","/data/lbc/loot_table/nuke.json",Items.PANDA_SPAWN_EGG,64,java.util.Set.of("minecraft:entity_data")),Items.PANDA_SPAWN_EGG,true){
        @Override public InteractionResult useOn(UseOnContext context){
            var game=me.skywars2.SkyWars2.game();
            if(game==null||!(context.getPlayer() instanceof ServerPlayer p)||!game.items().nukeEvent().startPlaced(p,context.getClickLocation().relative(context.getClickedFace(),.01)))return InteractionResult.FAIL;
            if(!p.getAbilities().instabuild)context.getItemInHand().shrink(1);
            p.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));return InteractionResult.SUCCESS;
        }
    });
    static void register(){}
}
