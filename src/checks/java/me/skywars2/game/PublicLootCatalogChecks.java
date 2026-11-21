package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.brigadier.suggestion.Suggestion;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Complete public logical coverage plus actual server completions/native table execution. */
final class PublicLootCatalogChecks {
    private PublicLootCatalogChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Public loot catalog: "+why);}
    static void verify(GameController game,ServerPlayer owner,CommandSourceStack source,List<Suggestion> completions){
        com.google.gson.JsonObject catalog;
        try(var stream=PublicLootCatalogChecks.class.getResourceAsStream("/skywars2/loot_test_catalog.json")){
            require(stream!=null,"catalog is packaged");catalog=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
        var suggested=new HashSet<String>();for(var completion:completions)require(suggested.add(completion.getText()),"no duplicate native suggestion ID");
        var expected=new HashSet<>(game.items().allItemPools().slots(LbcAllItemPools.Domain.CUSTOM));
        var seen=new HashSet<String>();var itemTables=new HashSet<String>();String poop=null,tmt=null;
        int outputs=0;
        for(var value:catalog.getAsJsonArray("items")){
            var row=value.getAsJsonObject();String table=row.get("table").getAsString(),slot=row.get("slot").getAsString(),carrier=row.get("carrier").getAsString();
            require(seen.add(slot)&&itemTables.add(table),"one public table per logical item: "+table);
            require(suggested.contains(table),"every logical item is present in actual native completion: "+table);
            var stacks=game.lbc().rollTable(owner.level(),owner.position(),owner,table);
            require(!stacks.isEmpty(),"every public item table produces real native output: "+table+" slot="+slot);
            for(var stack:stacks){
                require(!stack.isEmpty()&&ItemStack.validateStrict(stack).error().isEmpty(),"strict native stack/components: "+table);
                if(slot.equals("factory/tmt"))require(LbcTmtItems.matches(stack)&&LbcTmtItems.output(stack.getItem())&&!LbcTmtAbilities.read(stack).isEmpty(),"one TMT entry invokes the complete runtime factory");
                else require(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(net.minecraft.resources.Identifier.parse(carrier)),"public slot retains its actual native carrier: "+table+" got "+BuiltInRegistries.ITEM.getKey(stack.getItem())+" expected "+carrier);
                if(row.get("generated").getAsBoolean())require(stack.getCount()==1,"public singleton overrides raw zero/overstack quantities only: "+table);
                outputs++;
            }
            if(carrier.equals("skywars2:bouncing_poop"))poop=table;
            if(slot.equals("factory/tmt"))tmt=table;
        }
        require(seen.equals(expected),"public catalog covers the entire existing custom domain, native defaults and one TMT factory");
        require("lbc:items/govno_raskid".equals(poop)&&"lbc:items/tmt".equals(tmt),"requested projectile and TMT have discoverable singleton IDs");
        for(var alias:catalog.getAsJsonObject("hidden_aliases").entrySet()){
            require(!suggested.contains(alias.getKey())&&suggested.contains(alias.getValue().getAsString()),"aliases disappear while their canonical item stays visible: "+alias.getKey());
        }
        require(suggested.contains("lbc:shock_potion")&&!suggested.contains("lbc:ported/electrostatic_potion")&&!suggested.contains("lbc:ported/shock_potion"),"electrostatic potion has exactly one public canonical entry");
        var inventory=new ArrayList<ItemStack>();for(int n=0;n<owner.getInventory().getContainerSize();n++)inventory.add(owner.getInventory().getItem(n).copy());
        try{
            for(String table:List.of(poop,tmt,"lbc:shock_potion","lbc:ported/electrostatic_potion")){
                owner.getInventory().clearContent();
                require(game.server().getCommands().getDispatcher().execute("loot give TestAlpha loot "+table,source)>0,"actual /loot command gives canonical items and still resolves explicit internal aliases: "+table);
                require(!owner.getInventory().isEmpty(),"actual command commits its generated items: "+table);
            }
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
        finally{owner.getInventory().clearContent();for(int n=0;n<inventory.size();n++)owner.getInventory().setItem(n,inventory.get(n));}
        // The public slot seam cannot change the probability domain of existing lotteries.
        require(new HashSet<>(game.items().allItemPools().slots(LbcAllItemPools.Domain.CUSTOM)).equals(expected),"public materialization never adds tickets or changes TMT multiplicity");
        SkyWars2.LOGGER.info("[check] public loot coverage: {} logical items, {} real native stack outputs, {} hidden aliases",seen.size(),outputs,catalog.getAsJsonObject("hidden_aliases").size());
    }
}
