package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Complete set comparisons and native entry/factory execution, never a histogram completeness claim. */
public final class UniformItemPoolChecks {
    private UniformItemPoolChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Uniform item pools: "+why);}
    public static void run(GameController game){
        var player=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var service=game.items().allItemPools();service.initialize();
        var custom=service.slots(LbcAllItemPools.Domain.CUSTOM);var vanilla=service.slots(LbcAllItemPools.Domain.VANILLA);var union=service.slots(LbcAllItemPools.Domain.UNION);
        require(new HashSet<>(custom).size()==custom.size()&&new HashSet<>(vanilla).size()==vanilla.size()&&new HashSet<>(union).size()==union.size(),"one ticket per named logical slot");
        var expectedVanilla=new TreeSet<String>();
        for(var id:BuiltInRegistries.ITEM.keySet())if(id.getNamespace().equals("minecraft")&&BuiltInRegistries.ITEM.getValue(id)!=Items.AIR)expectedVanilla.add("vanilla/"+id);
        require(expectedVanilla.equals(new TreeSet<>(vanilla)),"exact full current non-AIR vanilla registry membership");
        var expectedUnion=new HashSet<>(custom);expectedUnion.addAll(vanilla);
        require(expectedUnion.equals(new HashSet<>(union))&&union.size()==custom.size()+vanilla.size(),"equal single index over the full union; no fixed branch probability");
        require(Collections.frequency(custom,"factory/tmt")==1&&Collections.frequency(union,"factory/tmt")==1&&!vanilla.contains("factory/tmt"),"exactly one TMT logical ticket across all five forms");
        for(var id:BuiltInRegistries.ITEM.keySet())if(id.getNamespace().equals("skywars2")&&!LbcTmtItems.output(BuiltInRegistries.ITEM.getValue(id))&&!id.toString().equals("skywars2:tmt_token"))
            require(service.representedNativeItems().contains(id)||service.excludedNativeItems().containsKey(id),"every registered native gameplay item represented or explicitly excluded: "+id);
        JsonObject manifest;
        try(var input=UniformItemPoolChecks.class.getResourceAsStream("/skywars2/all_item_pools.json")){
            require(input!=null,"packaged structural membership oracle");manifest=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception error){throw new IllegalStateException("Uniform pool oracle",error);}
        var expectedCustom=new HashSet<String>();
        for(var value:manifest.getAsJsonArray("custom")){
            var row=value.getAsJsonObject();String id=row.get("id").getAsString();
            require(expectedCustom.add(id)&&custom.contains(id),"every authored logical identity has exactly one runtime slot: "+id);
            require(!row.getAsJsonArray("profiles").isEmpty(),"authored slot keeps its contextual source profiles");
            for(var profile:row.getAsJsonArray("profiles"))require(manifest.getAsJsonObject("entryNodes").has(profile.getAsJsonObject().get("entry").getAsString()),"all source profile roots resolve into the interned native graph");
        }
        require(custom.stream().filter(id->id.startsWith("custom/")).allMatch(expectedCustom::contains),"no invented authored identity or silent authored omission");
        for(var value:manifest.getAsJsonArray("accounting")){
            var row=value.getAsJsonArray();String disposition=row.get(2).getAsString();
            require(!disposition.startsWith("custom/")||expectedCustom.contains(disposition),"every accounted source leaf maps to an actual logical slot");
            require(manifest.getAsJsonObject("sourceHashes").has(row.get(0).getAsString())&&manifest.getAsJsonObject("origins").has(row.get(1).getAsString()),"every membership claim retains its exact source closure");
        }
        require(manifest.getAsJsonArray("blocked").isEmpty(),"no silently unsupported source entry/composite/reference");
        var params=new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN,player.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.COMMAND);
        var context=new LootContext.Builder(params).withOptionalRandomSource(net.minecraft.util.RandomSource.create(0x554e49464f524dL)).create(Optional.empty());
        String consumed="native/skywars2:consumed_block";
        require(Collections.frequency(custom,consumed)==1&&Collections.frequency(union,consumed)==1&&!vanilla.contains(consumed),"playable consumed block has exactly one native logical ticket in custom and union");
        require(!service.excludedNativeItems().containsKey(Identifier.parse("skywars2:consumed_block"))
                &&manifest.getAsJsonObject("allowedDefaultNative").has("skywars2:consumed_block"),"real consumed block admission is separate from TMT ability classification");
        var consumedOutput=new ArrayList<ItemStack>();service.drawSlot(LbcAllItemPools.Domain.CUSTOM,custom.indexOf(consumed),context,consumedOutput::add);
        require(consumedOutput.size()==1&&ItemStack.matches(consumedOutput.getFirst(),LbcConsumedBlock.ITEM.getDefaultInstance())
                &&Identifier.parse("lbc:consumed_block").equals(consumedOutput.getFirst().get(DataComponents.ITEM_MODEL))
                &&consumedOutput.getFirst().getItem() instanceof BlockItem blockItem&&blockItem.getBlock()==LbcConsumedBlock.BLOCK,"consumed block output retains the complete native registered model and real placeable block item");
        for(var evidence:manifest.getAsJsonObject("nativeEvidence").entrySet()){
            String nativeId=evidence.getKey();
            var authored=java.util.stream.StreamSupport.stream(manifest.getAsJsonArray("custom").spliterator(),false)
                    .map(JsonElement::getAsJsonObject).filter(row->row.getAsJsonObject("identity").get("carrier").getAsString().equals(nativeId)).toList();
            require(!authored.isEmpty(),"audited native extra has full authored profiles: "+nativeId);
            for(var row:authored){
                var output=new ArrayList<ItemStack>();service.drawSlot(LbcAllItemPools.Domain.CUSTOM,custom.indexOf(row.get("id").getAsString()),context,output::add);
                require(output.size()==1&&BuiltInRegistries.ITEM.getKey(output.getFirst().getItem()).toString().equals(nativeId),"audited native extra uses its exact native transport");
                var stack=output.getFirst();require(stack.has(DataComponents.CUSTOM_NAME)&&stack.has(DataComponents.LORE)&&stack.has(DataComponents.ITEM_MODEL),"full native source presentation, no bare transport: "+nativeId);
                if(nativeId.startsWith("skywars2:academy_"))require(ItemData.intValue(stack,"upgraded",0)==1&&ItemData.intValue(stack,"mode",0)==1&&ItemData.intValue(stack,"upgrade_available",0)==1&&ItemData.intValue(stack,"upgradable",1)==0,"qualified Academy final native flags retained");
                else if(nativeId.startsWith("skywars2:cunny_"))require(LbcCunnyItems.variant(stack)==Integer.parseInt(nativeId.substring("skywars2:cunny_".length())),"exact authored indexed Cunny profile");
            }
        }
        for(String nativeId:List.of("skywars2:mana_potion","skywars2:mana_splash_potion","skywars2:mana_lingering_potion")){
            require(!custom.contains("native/"+nativeId),"bare mana transport cannot add an empty-dose ticket");boolean found=false;
            for(var value:manifest.getAsJsonArray("custom")){
                var row=value.getAsJsonObject();if(!row.getAsJsonObject("identity").get("carrier").getAsString().equals(nativeId))continue;
                var output=new ArrayList<ItemStack>();service.drawSlot(LbcAllItemPools.Domain.CUSTOM,custom.indexOf(row.get("id").getAsString()),context,output::add);
                require(output.size()==1&&output.getFirst().getOrDefault(DataComponents.POTION_CONTENTS,net.minecraft.world.item.alchemy.PotionContents.EMPTY).hasEffects(),"every authored mana logical dose retains its actual payload");found=true;
            }
            require(found,"mana transport represented by its real source dose domain");
        }
        require(service.complete(),"all current registered native IDs have an authored or explicitly audited complete-default admission");
        // Raw authored quantities include legitimate zero outcomes (for example the
        // explosive Magnum round's 0..2 provider). Keep those source functions intact;
        // also materialize every ticket through real positive count/max-stack wrappers.
        for(int i=0;i<custom.size();i++){
            var output=new ArrayList<ItemStack>();service.drawSlot(LbcAllItemPools.Domain.CUSTOM,i,context,output::add);
            require(output.size()==1&&output.getFirst().getCount()>=0,"every raw custom index emits exactly one source stack callback: "+custom.get(i));
            if(!output.getFirst().isEmpty())require(ItemStack.validateStrict(output.getFirst()).error().isEmpty(),"native raw component invariants for "+custom.get(i));
        }
        allCustomWrappers(game,params,custom);
        // The index seam covers every vanilla ticket with real native output.
        for(int i=0;i<vanilla.size();i++){
            var output=new ArrayList<ItemStack>();service.drawSlot(LbcAllItemPools.Domain.VANILLA,i,context,output::add);
            require(output.size()==1&&("vanilla/"+BuiltInRegistries.ITEM.getKey(output.getFirst().getItem())).equals(vanilla.get(i)),"every vanilla index materializes its own native item exactly once");
        }
        var forms=new HashSet<Item>();var tmt=new ArrayList<ItemStack>();int factory=custom.indexOf("factory/tmt");
        for(int i=0;i<128;i++){
            var output=new ArrayList<ItemStack>();service.drawSlot(LbcAllItemPools.Domain.CUSTOM,factory,context,output::add);
            require(output.size()==1&&LbcTmtItems.matches(output.getFirst())&&LbcTmtItems.output(output.getFirst().getItem()),"winning TMT ticket invokes the real fresh runtime generator");
            var stack=output.getFirst();forms.add(stack.getItem());tmt.add(stack);
        }
        require(forms.size()==5,"all five genuine native forms reached by the one seeded factory slot");
        require(tmt.getFirst()!=tmt.get(1)&&!ItemStack.isSameItemSameComponents(tmt.getFirst(),tmt.get(1)),"each winning factory ticket creates fresh private properties");
        var second=tmt.get(1).copy();tmt.getFirst().setCount(0);require(ItemStack.matches(second,tmt.get(1)),"generated outputs do not alias private state");
        wrappers(game,context,params,factory,custom.size());
        SkyWars2.LOGGER.info("[check] PASS uniform-item-pools (exact authored/native/vanilla membership, one union index, source-profile closure, one fresh five-form TMT factory and native count wrappers); complete={} findings={}",service.complete(),service.findings().size());
    }
    private static void allCustomWrappers(GameController game,LootParams params,List<String> custom){
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
        for(int count:List.of(64,99)){
            var source=JsonParser.parseString("{\"type\":\"skywars2:uniform_item\",\"domain\":\"custom\",\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":"+count+"},{\"type\":\"minecraft:set_components\",\"components\":{\"minecraft:max_stack_size\":99,\"!minecraft:max_damage\":{}}},{\"type\":\"minecraft:limit_count\",\"limit\":{\"max\":99}}]}");
            var entry=LootPoolEntries.CODEC.parse(ops,source).getOrThrow();
            for(int index=0;index<custom.size();index++){
                var actual=new LootContext.Builder(params).withOptionalRandomSource(new FirstIndexRandom(custom.size(),index)).create(Optional.empty());
                var expanded=new ArrayList<LootPoolEntry>();require(entry.expand(actual,expanded::add)&&expanded.size()==1,"native full custom wrapper expands once");
                var output=new ArrayList<ItemStack>();expanded.getFirst().createItemStack(output::add,actual);
                require(output.size()==1&&!output.getFirst().isEmpty()&&output.getFirst().getCount()==count
                        &&output.getFirst().getMaxStackSize()==99&&!output.getFirst().has(DataComponents.MAX_DAMAGE),"every custom logical index materializes through count"+count+"/max99: "+custom.get(index));
                require(ItemStack.validateStrict(output.getFirst()).error().isEmpty(),"native wrapped component invariants for "+custom.get(index));
            }
        }
    }
    private static void wrappers(GameController game,LootContext context,LootParams params,int factory,int customSize){
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
        for(int count:List.of(64,99)){
            var source=JsonParser.parseString("{\"type\":\"skywars2:uniform_item\",\"domain\":\"vanilla\",\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":"+count+"},{\"type\":\"minecraft:set_components\",\"components\":{\"minecraft:max_stack_size\":99,\"!minecraft:max_damage\":{}}},{\"type\":\"minecraft:limit_count\",\"limit\":{\"max\":99}}]}");
            var entry=LootPoolEntries.CODEC.parse(ops,source).getOrThrow();var expanded=new ArrayList<LootPoolEntry>();require(entry.expand(context,expanded::add)&&expanded.size()==1,"real native registered uniform entry expands once");
            var output=new ArrayList<ItemStack>();expanded.getFirst().createItemStack(output::add,context);
            require(output.size()==1&&output.getFirst().getCount()==count&&output.getFirst().getMaxStackSize()==99&&!output.getFirst().has(DataComponents.MAX_DAMAGE),"native outer count"+count+"/max99/removal wrappers survive selection");
            var roundtrip=LootPoolEntries.CODEC.parse(ops,LootPoolEntries.CODEC.encodeStart(ops,entry).getOrThrow()).getOrThrow();require(roundtrip instanceof LbcAllItemPools.Entry,"custom entry MapCodec native roundtrip");
            var factorySource=source.deepCopy().getAsJsonObject();factorySource.addProperty("domain","custom");
            var factoryContext=new LootContext.Builder(params).withOptionalRandomSource(new FirstIndexRandom(customSize,factory)).create(Optional.empty());
            var nativeFactory=LootPoolEntries.CODEC.parse(ops,factorySource).getOrThrow();var factoryEntries=new ArrayList<LootPoolEntry>();
            require(nativeFactory.expand(factoryContext,factoryEntries::add)&&factoryEntries.size()==1,"real uniform factory entry expands once");
            var generated=new ArrayList<ItemStack>();factoryEntries.getFirst().createItemStack(generated::add,factoryContext);
            require(generated.size()==1&&LbcTmtItems.matches(generated.getFirst())&&generated.getFirst().getCount()==count
                    &&generated.getFirst().getMaxStackSize()==99&&!LbcTmtAbilities.read(generated.getFirst()).isEmpty()&&LbcTmtAbilities.read(generated.getFirst()).size()<=7,
                    "native count"+count+"/max99 wrappers retain full freshly generated TMT private components");
        }
    }
    private static final class FirstIndexRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        final int bound,index;boolean selected;
        FirstIndexRandom(int bound,int index){super(0x554e49464f524dL);this.bound=bound;this.index=index;}
        @Override public int nextInt(int bound){if(!selected){require(bound==this.bound,"one actual uniform outer-domain bound");selected=true;return index;}return super.nextInt(bound);}
    }
}
