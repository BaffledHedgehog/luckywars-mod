package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** One shared logical lottery for native loot tables, containers and item factories. */
public final class LbcAllItemPools {
    public enum Domain { CUSTOM, VANILLA, UNION;
        static Domain parse(String text){return valueOf(text.toUpperCase(Locale.ROOT));}
    }
    private final GameController game;
    private volatile Catalog catalog;
    public LbcAllItemPools(GameController game){this.game=game;}
    public static void register(){Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE,Identifier.parse("skywars2:uniform_item"),Entry.MAP_CODEC);}
    /** Bind once after item and dynamic registries are ready, before gameplay input is published. */
    public synchronized void initialize(){if(catalog==null)catalog=new Catalog();}
    /** Build privately; a failed reload leaves the previous complete binding usable. */
    public synchronized void reload(){var replacement=new Catalog();catalog=replacement;}
    private Catalog catalog(){if(catalog==null)throw new IllegalStateException("Initialize uniform item pools before publishing gameplay inputs");return catalog;}
    public boolean complete(){return catalog().complete;}
    public List<String> findings(){return catalog().findings;}
    public List<String> slots(Domain domain){return catalog().slots(domain).stream().map(slot->slot.id).toList();}
    public Set<Identifier> representedNativeItems(){return catalog().representedNative;}
    public Map<Identifier,String> excludedNativeItems(){return catalog().excludedNative;}
    public void draw(Domain domain,LootContext context,Consumer<ItemStack> output){
        var slots=catalog().slots(domain);drawSlot(domain,context.getRandom().nextInt(slots.size()),context,output);
    }
    /** Explicit index seam verifies every logical slot without replacing the runtime random sampler. */
    void drawSlot(Domain domain,int index,LootContext context,Consumer<ItemStack> output){
        var slot=catalog().slots(domain).get(index);
        if(slot.tmt){output.accept(game.items().tmt().generate(context.getRandom()));return;}
        if(slot.nativeItem!=null){output.accept(slot.nativeItem.getDefaultInstance().copy());return;}
        // Pick a profile only inside the already selected logical slot. Conditions
        // retain native context semantics and cannot change another item's ticket count.
        var admitted=new ArrayList<Prepared>();
        for(var profile:slot.profiles){var prepared=profile.prepare(context);if(prepared!=null)admitted.add(prepared);}
        if(admitted.isEmpty())return;
        admitted.get(context.getRandom().nextInt(admitted.size())).emit(output,context);
    }
    private record Slot(String id,List<Node> profiles,Item nativeItem,boolean tmt){}
    private record Prepared(LootPoolEntry entry,List<Holder<LootItemFunction>> modifiers){
        void emit(Consumer<ItemStack> output,LootContext context){
            for(var modifier:modifiers)output=LootItemFunction.decorate(Optional.of(modifier),output,context);
            entry.createItemStack(output,context);
        }
    }
    /** Interned native condition/function boundaries; no codec parsing occurs during a draw. */
    private record Node(Node child,LootPoolEntryContainer leaf,Holder<LootItemCondition> condition,Holder<LootItemFunction> modifier){
        Prepared prepare(LootContext context){
            var modifiers=new ArrayList<Holder<LootItemFunction>>();var node=this;
            while(node.child!=null){
                if(node.condition!=null&&!node.condition.value().test(context))return null;
                if(node.modifier!=null)modifiers.add(node.modifier);node=node.child;
            }
            var entries=new ArrayList<LootPoolEntry>();
            if(!node.leaf.expand(context,entries::add)||entries.isEmpty())return null;
            if(entries.size()!=1)throw new IllegalStateException("Uniform profile must resolve one logical leaf");
            return new Prepared(entries.getFirst(),List.copyOf(modifiers));
        }
    }
    private final class Catalog {
        final List<Slot> custom,vanilla,union;
        final Set<Identifier> representedNative;
        final Map<Identifier,String> excludedNative;
        final boolean complete;
        final List<String> findings;
        Catalog(){
            JsonObject manifest;
            try(var stream=LbcAllItemPools.class.getResourceAsStream("/skywars2/all_item_pools.json")){
                if(stream==null)throw new IllegalStateException("Missing uniform item manifest");
                manifest=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            }catch(Exception error){throw new IllegalStateException("Read uniform item catalog",error);}
            if(manifest.get("schema").getAsInt()!=1)throw new IllegalStateException("Unknown uniform pool schema");
            boolean sourceComplete=manifest.get("complete").getAsBoolean();
            var problems=new ArrayList<String>();
            for(String key:List.of("blocked","unparsedSourceResources"))for(var value:manifest.getAsJsonArray(key))problems.add(value.toString());
            var excluded=new LinkedHashMap<Identifier,String>();manifest.getAsJsonObject("excludedNative").entrySet().forEach(e->excluded.put(Identifier.parse(e.getKey()),e.getValue().getAsString()));
            excludedNative=Map.copyOf(excluded);
            var tmtIds=new HashSet<Identifier>();manifest.getAsJsonArray("tmtNative").forEach(e->tmtIds.add(Identifier.parse(e.getAsString())));
            var represented=new HashSet<Identifier>();var nodes=manifest.getAsJsonObject("entryNodes");
            for(var value:nodes.entrySet()){
                var node=value.getValue().getAsJsonObject();
                if(node.get("type").getAsString().equals("minecraft:item")){
                    var id=Identifier.parse(node.get("name").getAsString());
                    if(!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalStateException("Uniform pool missing native item "+id);
                    if(id.getNamespace().equals("skywars2"))represented.add(id);
                }
            }
            var binding=new Binding(nodes);
            var customs=new ArrayList<Slot>();
            for(var value:manifest.getAsJsonArray("custom")){
                var row=value.getAsJsonObject();var profiles=new ArrayList<Node>();
                for(var profile:row.getAsJsonArray("profiles"))profiles.add(binding.node(profile.getAsJsonObject().get("entry").getAsString()));
                if(profiles.isEmpty())throw new IllegalStateException("Uniform custom slot without source profiles");
                customs.add(new Slot(row.get("id").getAsString(),List.copyOf(profiles),null,false));
            }
            // A registered item may be a bare transport requiring a dose/mode or
            // payload from its authored leaf. Only an explicitly audited complete
            // native prototype may add a default ticket; unknown IDs are findings.
            var allowedDefaults=manifest.getAsJsonObject("allowedDefaultNative");boolean nativeComplete=true;
            for(var id:BuiltInRegistries.ITEM.keySet().stream().filter(id->id.getNamespace().equals("skywars2")).sorted().toList()){
                if(represented.contains(id)||excluded.containsKey(id)||tmtIds.contains(id))continue;
                if(!allowedDefaults.has(id.toString())){nativeComplete=false;problems.add("Unresolved native item has no authored profile or complete-default policy: "+id);continue;}
                var item=BuiltInRegistries.ITEM.getValue(id);represented.add(id);
                customs.add(new Slot("native/"+id,List.of(),item,false));
            }
            customs.add(new Slot("factory/tmt",List.of(),null,true));
            var vanillas=new ArrayList<Slot>();var unionVanillas=new ArrayList<Slot>();
            for(var id:BuiltInRegistries.ITEM.keySet().stream().filter(id->id.getNamespace().equals("minecraft")).sorted().toList()){
                var item=BuiltInRegistries.ITEM.getValue(id);if(item==Items.AIR)continue;
                vanillas.add(new Slot("vanilla/"+id,List.of(),item,false));
                var profiles=new ArrayList<Node>();var authored=manifest.getAsJsonObject("pureVanillaSourceProfiles").getAsJsonArray(id.toString());
                if(authored!=null)for(var profile:authored)profiles.add(binding.node(profile.getAsString()));
                unionVanillas.add(new Slot("vanilla/"+id,List.copyOf(profiles),profiles.isEmpty()?item:null,false));
            }
            customs.sort(Comparator.comparing(Slot::id));vanillas.sort(Comparator.comparing(Slot::id));
            custom=List.copyOf(customs);vanilla=List.copyOf(vanillas);var combined=new ArrayList<>(custom);combined.addAll(unionVanillas);union=List.copyOf(combined);
            representedNative=Set.copyOf(represented);findings=List.copyOf(problems);complete=sourceComplete&&nativeComplete;
            requireUnique(custom);requireUnique(vanilla);requireUnique(union);
        }
        List<Slot> slots(Domain domain){return switch(domain){case CUSTOM->custom;case VANILLA->vanilla;case UNION->union;};}
        private void requireUnique(List<Slot> slots){if(slots.stream().map(slot->slot.id).distinct().count()!=slots.size())throw new IllegalStateException("Duplicate logical uniform slot");}
        private final class Binding {
            final JsonObject nodes;
            final Map<String,Node> decoded=new HashMap<>();
            final Map<String,Holder<LootItemCondition>> conditions=new HashMap<>();
            final Map<String,Holder<LootItemFunction>> functions=new HashMap<>();
            final com.mojang.serialization.DynamicOps<JsonElement> ops=game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
            Binding(JsonObject nodes){this.nodes=nodes;}
            Node node(String id){
                var old=decoded.get(id);if(old!=null)return old;
                var row=nodes.getAsJsonObject(id);if(row==null)throw new IllegalStateException("Missing interned uniform profile "+id);
                Node value;
                if(row.get("type").getAsString().equals("minecraft:group")){
                    var children=row.getAsJsonArray("children");if(children.size()!=1)throw new IllegalStateException("Uniform boundary must have exactly one child");
                    Holder<LootItemCondition> condition=row.has("condition")?conditions.computeIfAbsent(row.get("condition").toString(),key->LootItemCondition.CODEC.parse(ops,row.get("condition")).getOrThrow()):null;
                    Holder<LootItemFunction> modifier=row.has("modifier")?functions.computeIfAbsent(row.get("modifier").toString(),key->LootItemFunctions.CODEC.parse(ops,row.get("modifier")).getOrThrow()):null;
                    value=new Node(node(children.get(0).getAsString()),null,condition,modifier);
                }else value=new Node(null,LootPoolEntries.CODEC.parse(ops,row).getOrThrow(),null,null);
                decoded.put(id,value);return value;
            }
        }
    }
    /** Native entry modifier/condition support keeps existing outer count/max-stack wrappers effective. */
    public static final class Entry extends SingleEntryContainerBase {
        public static final MapCodec<Entry> MAP_CODEC=RecordCodecBuilder.mapCodec(i->i.group(
                Codec.STRING.fieldOf("domain").forGetter(e->e.domain.name().toLowerCase(Locale.ROOT))).and(uniformFields(i)).apply(i,Entry::new));
        private final Domain domain;
        private Entry(String domain,int weight,int quality,Optional<Holder<LootItemCondition>> condition,Optional<Holder<LootItemFunction>> modifier){super(weight,quality,condition,modifier);this.domain=Domain.parse(domain);}
        @Override public MapCodec<Entry> codec(){return MAP_CODEC;}
        @Override public void createItemStack(Consumer<ItemStack> output,LootContext context){
            var game=me.skywars2.SkyWars2.game();if(game==null)throw new IllegalStateException("Uniform gameplay pool needs its native game owner");
            game.items().allItemPools().draw(domain,context,output);
        }
    }
}
