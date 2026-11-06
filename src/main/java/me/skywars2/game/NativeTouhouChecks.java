package me.skywars2.game;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Compare independent original-carrier outcomes and the complete weighted reward graph. */
public final class NativeTouhouChecks {
    private NativeTouhouChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Native Touhou: "+why);}
    private static JsonObject source(String resource){try(var input=NativeTouhouChecks.class.getResourceAsStream(resource)){
        return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input,resource),StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(java.io.IOException error){throw new IllegalStateException(error);}}
    public static void run(GameController game){try(var f=new WoolBucketChecks.Fixture(game,false)){
        for(var kind:LbcTouhouItems.Kind.values()){
            var stack=NativeMeleeChecks.sourceOutcome(game,f,kind.item,kind.carrier,kind.definition,17);
            require(ItemStack.matches(stack,kind.item.getDefaultInstance()),"/give default retains every authored component "+kind.id);
            require(ItemStack.matches(stack,f.leaf(kind.id)),"ready singleton retains full source "+kind.id);
            for(var tag:kind.carrier.builtInRegistryHolder().tags().toList())require(stack.is(tag),"complete carrier tag graph "+kind.id+" "+tag);
            require(LootTestCatalog.publicNativeItem(kind.item),"public native gallery admission "+kind.id);
        }
        var raw=source("/data/lbc/loot_table/touhou_item.json");var entries=raw.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        int[] ids={0,1,2,3,4,10,5,6,7,8,9},weights={800,100,3,1000,1,10,200,100,10,10,1000};
        require(entries.size()==11,"all weighted outcomes remain");
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.owner.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);
        for(int n=0;n<ids.length;n++){
            var entry=entries.get(n).getAsJsonObject();require(entry.get("weight").getAsInt()==weights[n],"exact weight and branch order "+n);
            var kind=LbcTouhouItems.Kind.values()[ids[n]];
            require(entry.get("value").getAsString().equals(ids[n]==10?"lbc:bomb_item":"lbc:ported/"+kind.id),"one source definition per reward "+n);
            // Force each real branch, including the rare 1/3234 life reward, through the native resolver.
            var single=raw.deepCopy();var only=new JsonArray();only.add(entry.deepCopy());single.getAsJsonArray("pools").get(0).getAsJsonObject().add("entries",only);
            var rolled=LootTable.DIRECT_CODEC.parse(ops,single).getOrThrow().getRandomItems(params,29);
            require(rolled.size()==1&&ItemStack.matches(rolled.getFirst(),kind.item.getDefaultInstance()),"weighted reference resolves exact prototype "+kind.id);
        }
        // Build an independent original-carrier oracle for all ten source definitions and the bomb.
        var expected=raw.deepCopy();var originals=expected.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        for(int n=0;n<ids.length;n++){
            var kind=LbcTouhouItems.Kind.values()[ids[n]];var original=source(kind.definition).getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            original.addProperty("name",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(kind.carrier).toString());original.addProperty("weight",weights[n]);originals.set(n,original);
        }
        var table=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow();var oracle=LootTable.DIRECT_CODEC.parse(ops,expected).getOrThrow();
        for(long seed=1;seed<=64;seed++){
            var actual=table.getRandomItems(params,seed);var original=oracle.getRandomItems(params,seed);require(actual.size()==original.size(),"seeded weighted count "+seed);
            for(int i=0;i<actual.size();i++){
                var stack=actual.get(i);var wanted=original.get(i);
                require(stack.getCount()==wanted.getCount()&&stack.getComponents().keySet().equals(wanted.getComponents().keySet()),"seeded pool count/keys "+seed);
                for(var component:wanted.getComponents())require(Objects.equals(component.value(),stack.get(component.type())),"seeded pool full source projection "+seed+" "+component.type());
            }
        }
    }SkyWars2.LOGGER.info("[check] PASS native-touhou (12 exact original-carrier/default/ready/Polymer/codec prototypes; full carrier tag graph; all11 weighted branches and64 seeded pool outcomes)");}
}
