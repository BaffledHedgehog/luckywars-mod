package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Independent original-carrier codec oracle for every canonical food, with actual native held use. */
public final class NativeNamedFoodChecks {
    private NativeNamedFoodChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Native named food: "+why);}
    private static String difference(ItemStack actual,ItemStack expected){
        var values=new ArrayList<String>();
        for(var component:actual.getComponents())if(!Objects.equals(component.value(),expected.get(component.type())))
            values.add(component.type()+": "+component.value()+" != "+expected.get(component.type()));
        for(var component:expected.getComponents())if(!actual.has(component.type()))values.add("missing "+component.type());
        return values.toString();
    }
    public static void run(GameController game){
        try(var base=new ElementalGrimoireChecks.Fixture(game,true)){
            base.initialize();var player=base.owner;
            var params=new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN,player.position())
                    .withOptionalParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.CHEST);
            var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)player.connection;
            var unique=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
            require(LbcNamedFoodItems.entries().size()==40,"complete source profile family");
            for(var entry:LbcNamedFoodItems.entries()){
                var items=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/food/"+entry.id());
                require(items.size()==1,"one canonical public item "+entry.id());var actual=items.getFirst();
                require(actual.is(entry.item())&&unique.add(actual.getItem())&&ItemData.intValue(actual,entry.flag(),0)==1,"distinct native identity and exact source flag "+entry.id());
                try(var input=NativeNamedFoodChecks.class.getResourceAsStream("/"+entry.definition())){
                    var original=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input),StandardCharsets.UTF_8)).getAsJsonObject();
                    original.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                            .addProperty("name",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.carrier()).toString());
                    var source=LootTable.DIRECT_CODEC.parse(ops,original).getOrThrow().getRandomItems(params,17);
                    require(source.size()==1,"independent original-carrier table "+entry.id());var expected=source.getFirst();
                    require(LbcFoodLifecycle.countsAsFood(actual)==LbcFoodLifecycle.countsAsFood(expected),"source food counter membership retained "+entry.id());
                    var comparable=new ItemStack(entry.carrier(),actual.getCount());comparable.applyComponents(actual.getComponents());
                    require(ItemStack.matches(comparable,expected),"every authored/inherited native component matches original carrier "+entry.id()+" differences="+difference(comparable,expected));
                    require(actual.getComponents().equals(entry.item().getDefaultInstance().getComponents()),"give defaults equal canonical loot "+entry.id());
                    var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(actual,TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());
                    require(ItemStack.matches(actual,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),player.registryAccess())),"all-component Polymer roundtrip "+entry.id());
                    player.getFoodData().setFoodLevel(1);var held=actual.copyWithCount(2);player.setItemInHand(InteractionHand.MAIN_HAND,held);
                    require(player.gameMode.useItem(player,player.level(),held,InteractionHand.MAIN_HAND).consumesAction()&&player.isUsingItem()
                            &&player.getUseItem()==held&&player.getUseItemRemainingTicks()==expected.getUseDuration(player),"actual native use and source duration "+entry.id());
                    player.stopUsingItem();require(held.getCount()==2,"cancelled use does not consume "+entry.id());
                }catch(java.io.IOException error){throw new IllegalStateException(entry.id(),error);}
            }
            require(unique.size()==40,"all native types registered");
        }
        SkyWars2.LOGGER.info("[check] PASS native-named-food (40 distinct canonical native foods, original-carrier all-component codec oracle, loot/give equality, actual held use/cancel/duration and Polymer roundtrip; effects stay on the existing named-food owner)");
    }
}
