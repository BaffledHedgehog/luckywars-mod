package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Source membership, real native draws and wrapper checks; no histogram completeness claims. */
public final class TypedUniformItemPoolChecks {
    private TypedUniformItemPoolChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Typed uniform pools: "+why);}
    private static JsonObject resource(String path){
        try(var input=TypedUniformItemPoolChecks.class.getResourceAsStream(path)){
            require(input!=null,"packaged resource "+path);return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception error){throw new IllegalStateException("Read typed pool oracle "+path,error);}
    }
    public static void run(GameController game){
        var service=game.items().allItemPools();service.initialize();
        var player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var params=new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN,player.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.COMMAND);
        var manifest=resource("/skywars2/all_item_pools.json");var views=manifest.getAsJsonObject("typedViews");
        require(views.size()==34,"20 exact base, 11 broader and three dedicated source closures");
        var mappings=new HashMap<String,String>();
        for(String key:List.of("typedLegacyTables","functionalLegacyTables"))manifest.getAsJsonObject(key).entrySet().forEach(e->mappings.put(e.getKey(),e.getValue().getAsString()));
        require(mappings.equals(LbcAllItemPools.uniformTables()),"all 46 dedicated caller translations agree with source metadata");
        require(LbcAllItemPools.uniformTable("lbc:transformables_helmet_moisei").equals("lbc:transformables_helmet_moisei"),"set-specific routes retain their own selector");
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
        int checked=0;
        for(var view:views.entrySet()){
            String name=view.getKey();var expected=view.getValue().getAsJsonObject().getAsJsonObject("members").keySet();
            var slots=service.slots(LbcAllItemPools.Domain.CUSTOM,List.of(name));var actual=new HashSet<>(slots);
            require(actual.size()==slots.size()&&actual.containsAll(expected),"one ticket per source identity in "+name);
            if(!name.endsWith("_all"))require(actual.equals(expected),"base/subtype exact source closure, no inferred transport category: "+name);
            require(actual.contains("factory/tmt")==view.getValue().getAsJsonObject().get("tmtFactory").getAsBoolean(),"typed TMT eligibility derives only from selected source closure: "+name);
            var entrySource=resource("/data/lbc/loot_table/uniform/"+name+".json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            var entry=LootPoolEntries.CODEC.parse(ops,entrySource).getOrThrow();
            require(LootPoolEntries.CODEC.parse(ops,LootPoolEntries.CODEC.encodeStart(ops,entry).getOrThrow()).getOrThrow() instanceof LbcAllItemPools.Entry,"typed native MapCodec roundtrip "+name);
            for(int index=0;index<slots.size();index++){
                var context=context(params,new FirstIndexRandom(slots.size(),index));var output=emit(entry,context);
                require(output.size()==1&&output.getFirst().getCount()>=0,"real typed raw draw emits one source callback, including authored zero quantities: "+name+"/"+slots.get(index));
                String slot=slots.get(index);var stack=output.getFirst();
                var exact=new ArrayList<ItemStack>();var exactContext=context(params,new FirstIndexRandom(slots.size(),index));
                exactContext.getRandom().nextInt(slots.size()); // Same initial lottery draw; exact seam itself never falls back.
                service.drawSlot(LbcAllItemPools.Domain.CUSTOM,List.of(name),index,exactContext,exact::add);
                if(!exact.isEmpty()){
                    require(exact.size()==1&&ItemStack.matches(exact.getFirst(),stack),"admitted exact profile equals actual native selector output: "+slot);
                    if(slot.startsWith("custom/")&&!stack.isEmpty()){
                        var row=find(manifest,slot);require(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(net.minecraft.resources.Identifier.parse(row.getAsJsonObject("identity").get("carrier").getAsString())),"selected logical identity keeps its full native carrier: "+slot);
                    }
                }else require(stack.isEmpty()||slots.stream().anyMatch(candidate->candidate.startsWith("custom/")?BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(net.minecraft.resources.Identifier.parse(find(manifest,candidate).getAsJsonObject("identity").get("carrier").getAsString())):candidate.startsWith("native/")&&BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(net.minecraft.resources.Identifier.parse(candidate.substring(7)))),"blocked source falls back within the same typed membership: "+name);
                // An outer native modifier must restore a legitimate zero quantity too.
                var wrapped=entrySource.deepCopy();wrapped.add("modifier",JsonParser.parseString("[{\"type\":\"minecraft:set_count\",\"count\":64},{\"type\":\"minecraft:set_components\",\"components\":{\"minecraft:max_stack_size\":99,\"!minecraft:max_damage\":{}}}]") );
                var positive=emit(LootPoolEntries.CODEC.parse(ops,wrapped).getOrThrow(),context(params,new FirstIndexRandom(slots.size(),index)));
                require(positive.size()==1&&positive.getFirst().getCount()==64&&positive.getFirst().getMaxStackSize()==99&&!positive.getFirst().has(DataComponents.MAX_DAMAGE)&&ItemStack.validateStrict(positive.getFirst()).error().isEmpty(),"typed source components survive native count64/max99 wrapper: "+name+"/"+slot);
                checked++;
            }
            var max99=entrySource.deepCopy();max99.add("modifier",JsonParser.parseString("[{\"type\":\"minecraft:set_count\",\"count\":99},{\"type\":\"minecraft:set_components\",\"components\":{\"minecraft:max_stack_size\":99,\"!minecraft:max_damage\":{}}}]") );
            var output99=emit(LootPoolEntries.CODEC.parse(ops,max99).getOrThrow(),context(params,new FirstIndexRandom(slots.size(),0)));
            require(output99.size()==1&&output99.getFirst().getCount()==99&&ItemStack.validateStrict(output99.getFirst()).error().isEmpty(),"typed count99/max99 native wrapper "+name);
        }
        for(var combination:manifest.getAsJsonArray("typedCombinations")){
            var types=new ArrayList<String>();combination.getAsJsonArray().forEach(e->types.add(e.getAsString()));
            var expected=new TreeSet<String>();types.forEach(type->expected.addAll(service.slots(LbcAllItemPools.Domain.CUSTOM,List.of(type))));
            var slots=service.slots(LbcAllItemPools.Domain.CUSTOM,types);
            require(new TreeSet<>(slots).equals(expected)&&slots.size()==expected.size(),"OR categories share one deduplicated logical index: "+types);
            var duplicated=new ArrayList<>(types);duplicated.addAll(types);Collections.reverse(duplicated);
            require(service.slots(LbcAllItemPools.Domain.CUSTOM,duplicated).equals(slots),"type order and repetition cannot weight tickets");
        }
        for(var mapping:manifest.getAsJsonObject("functionalLegacyTables").entrySet()){
            String name=mapping.getKey().substring(4);var target=resource("/data/lbc/loot_table/uniform/"+name+".json");
            var before=manifest.getAsJsonObject("functionalSources").getAsJsonObject(mapping.getKey()).deepCopy();var after=target.deepCopy();
            for(int poolIndex=0;poolIndex<before.getAsJsonArray("pools").size();poolIndex++){
                var originalEntry=before.getAsJsonArray("pools").get(poolIndex).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().deepCopy();
                var replacementEntry=after.getAsJsonArray("pools").get(poolIndex).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().deepCopy();
                for(String field:List.of("type","name","value","weight","quality","domain","types","item_type")){originalEntry.remove(field);replacementEntry.remove(field);}
                require(originalEntry.equals(replacementEntry),"functional native entry wrapper conditions/modifiers retained in order: "+name);
            }
            for(var pool:before.getAsJsonArray("pools"))pool.getAsJsonObject().remove("entries");
            for(var pool:after.getAsJsonArray("pools"))pool.getAsJsonObject().remove("entries");
            require(before.equals(after),"27-roll caller pool/table functions, conditions and roll providers unchanged: "+name);
            var table=LootTable.DIRECT_CODEC.parse(ops,target).getOrThrow();var output=new ArrayList<ItemStack>();
            table.getRandomItemsRaw(context(params,net.minecraft.util.RandomSource.create(0x5459504544L)),output::add);
            require(output.size()==27,"dedicated native functional wrapper makes exactly 27 logical draws: "+name);
        }
        var vanilla=service.slots(LbcAllItemPools.Domain.VANILLA);var restricted=service.slots(LbcAllItemPools.Domain.VANILLA,List.of("no_recursive_shulker"));
        require(new HashSet<>(restricted).equals(new HashSet<>(vanilla)),"source-compatible vanilla view keeps every ordinary vanilla default, including empty boxes");
        long boxes=vanilla.stream().filter(id->id.endsWith("shulker_box")).count();require(boxes==17,"all 17 source plain vanilla shulker boxes retained");
        var rcb=new ArrayList<ItemStack>();service.drawSlot(LbcAllItemPools.Domain.VANILLA,List.of("no_recursive_shulker"),restricted.indexOf("vanilla/minecraft:repeating_command_block"),context(params,net.minecraft.util.RandomSource.create(1)),rcb::add);
        require(rcb.size()==1&&rcb.getFirst().is(Items.REPEATING_COMMAND_BLOCK)&&!LbcBucksTrader.matches(rcb.getFirst()),"generic source command block remains vanilla, never native currency");
        for(String carrier:List.of("skywars2:bucks","skywars2:academy_token","skywars2:blood_anal_boom","skywars2:mana_gem")){
            var rows=new ArrayList<JsonObject>();for(var value:manifest.getAsJsonArray("custom")){var row=value.getAsJsonObject();if(row.getAsJsonObject("identity").get("carrier").getAsString().equals(carrier))rows.add(row);}
            require(rows.size()==1,"dedicated helper primitive cannot duplicate the complete native item: "+carrier);
            var output=new ArrayList<ItemStack>();var custom=service.slots(LbcAllItemPools.Domain.CUSTOM);service.drawSlot(LbcAllItemPools.Domain.CUSTOM,custom.indexOf(rows.getFirst().get("id").getAsString()),context(params,net.minecraft.util.RandomSource.create(1)),output::add);
            require(output.size()==1&&output.getFirst().has(DataComponents.CUSTOM_NAME)&&output.getFirst().has(DataComponents.ITEM_MODEL)&&output.getFirst().has(DataComponents.CUSTOM_DATA),"full authored wrapper retained for "+carrier);
            if(carrier.endsWith(":bucks"))require(LbcBucksTrader.matches(output.getFirst())&&output.getFirst().getCount()>=1&&output.getFirst().getCount()<=64&&output.getFirst().has(DataComponents.LORE),"currency uses actual full random_bucks quantity and metadata");
        }
        fallback(game,params);
        SkyWars2.LOGGER.info("[check] PASS typed-uniform-item-pools (34 source views, {} exact-index native draws/wrappers, deduplicated OR views, 12 actual 27-roll callers, vanilla source isolation, four full helper authorities and condition fallback)",checked);
    }
    private static JsonObject find(JsonObject manifest,String id){for(var value:manifest.getAsJsonArray("custom"))if(value.getAsJsonObject().get("id").getAsString().equals(id))return value.getAsJsonObject();throw new IllegalStateException(id);}
    private static LootContext context(LootParams params,net.minecraft.util.RandomSource random){return new LootContext.Builder(params).withOptionalRandomSource(random).create(Optional.empty());}
    private static List<ItemStack> emit(LootPoolEntryContainer entry,LootContext context){var expanded=new ArrayList<LootPoolEntry>();entry.expand(context,expanded::add);var output=new ArrayList<ItemStack>();expanded.forEach(e->e.createItemStack(output::add,context));return output;}
    private static void fallback(GameController game,LootParams params){
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
        var denied=LootPoolEntries.CODEC.parse(ops,JsonParser.parseString("{\"type\":\"minecraft:item\",\"name\":\"minecraft:stick\",\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":0}}")).getOrThrow();
        var admitted=LootPoolEntries.CODEC.parse(ops,JsonParser.parseString("{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\",\"modifier\":{\"type\":\"minecraft:set_count\",\"count\":0}}")).getOrThrow();
        var context=context(params,net.minecraft.util.RandomSource.create(0x46414c4c4241434bL));require(emit(denied,context).isEmpty(),"actual native false condition blocks first slot");
        var visited=new HashSet<Integer>();visited.add(0);var reward=new ArrayList<ItemStack>();
        require(LbcAllItemPools.tryRemaining(3,0,context.getRandom(),index->{require(visited.add(index),"fallback never revisits a failed logical slot");var output=emit(index==2?admitted:denied,context);reward.addAll(output);return !output.isEmpty();}),"native condition failure falls back to an admitted remaining ticket");
        require(reward.size()==1&&reward.getFirst().getCount()==0,"an authored zero quantity is successful emission, never rerolled");
        reward.getFirst().setCount(1);require(reward.getFirst().is(Items.DIAMOND),"the raw zero callback retains its actual native item for a later count wrapper");
        visited.clear();visited.add(1);require(!LbcAllItemPools.tryRemaining(7,1,context.getRandom(),index->{require(visited.add(index),"all-blocked fallback visits once");return !emit(denied,context).isEmpty();})&&visited.size()==7,"all blocked ends after each logical slot exactly once");
    }
    private static final class FirstIndexRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        private final int bound,index;private boolean selected;
        FirstIndexRandom(int bound,int index){super(0x5459504544L);this.bound=bound;this.index=index;}
        @Override public int nextInt(int bound){if(!selected){require(bound==this.bound,"native outer sampler uses full combined logical length");selected=true;return index;}return super.nextInt(bound);}
    }
}
