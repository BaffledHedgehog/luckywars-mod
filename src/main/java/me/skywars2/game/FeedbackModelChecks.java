package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;

/** Real registered defaults, canonical loot and recipient-bound Polymer round trips. */
public final class FeedbackModelChecks {
    private FeedbackModelChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Feedback models: "+why);}
    public static void run(GameController game){
        try(var f=new ElementalGrimoireChecks.Fixture(game,true)){
            f.initialize();
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.recording;
            require(eu.pb4.polymer.common.api.PolymerCommonUtils.getPlayer(provider.getPacketContext())==f.owner,"actual viewer context");
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                for(var book:LbcLibrarySpells.extraDefinitions()){
                    var result=game.lbc().rollTable(f.owner.level(),f.owner.position(),f.owner,"lbc:ported/"+book.id());
                    require(result.size()==1,"canonical book leaf "+book.id());
                    var looted=result.getFirst();var fresh=new ItemStack(book.item());
                    require(Objects.equals(looted.get(DataComponents.ITEM_MODEL),fresh.get(DataComponents.ITEM_MODEL)),"registered model agrees with authored loot "+book.id());
                    verify(f,fresh,looted.get(DataComponents.ITEM_MODEL),provider);
                    verify(f,looted,looted.get(DataComponents.ITEM_MODEL),provider);
                }
                var expectations=Map.of("gneiss_stone","minecraft:stone","dev_tools","minecraft:chest_minecart",
                    "rocket_boots","lbc:armors/rocket_boots","govno_eaten","lbc:shit","win_button","lbc:win_button");
                expectations.forEach((id,model)->verify(f,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("skywars2:"+id))),Identifier.parse(model),provider));
                int[] colors={3672329,16760906,10197915,16777215};int index=0;
                for(var id:List.of("termit","sulfur","phosphin","gremuch")){
                    var fresh=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("skywars2:"+id)));
                    verify(f,fresh,Identifier.withDefaultNamespace("lingering_potion"),provider);
                    require(fresh.getHoverName().getContents() instanceof TranslatableContents text&&text.getKey().equals(id),"literal chemical translation key "+id);
                    var contents=fresh.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
                    require(contents.customColor().orElse(-1)==colors[index]&&LbcItems.chemicalAmplifier(contents)==234+index,"source native potion color and sentinel "+id);index++;
                }
                var nuts=game.lbc().rollTable(f.owner.level(),f.owner.position(),f.owner,"lbc:oreshki_10");
                require(nuts.stream().mapToInt(ItemStack::getCount).sum()==10&&nuts.stream().allMatch(s->s.is(LbcOreshek.ITEM)&&LbcOreshek.matches(s)),"all dispenser nuts use the same native registered item");
                for(var stack:nuts)verify(f,stack,Identifier.parse("lbc:bob"),provider);
                verify(f,new ItemStack(Items.STONE),Identifier.withDefaultNamespace("stone"),provider);
            });
        }
        SkyWars2.LOGGER.info("[check] PASS feedback-models (registered/loot grimoire defaults, all reported carriers, native chemical colors/names, unified nut identity and recipient Polymer round trips)");
    }
    private static void verify(ElementalGrimoireChecks.Fixture f,ItemStack stack,Identifier expected,
            net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider provider){
        require(expected!=null&&expected.equals(stack.get(DataComponents.ITEM_MODEL)),"authoritative authored model "+stack.getItem());
        var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());
        require(expected.equals(proxy.get(DataComponents.ITEM_MODEL)),"actual client model "+stack.getItem());
        require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),f.owner.registryAccess())),"lossless authoritative round trip "+stack.getItem());
    }
}
