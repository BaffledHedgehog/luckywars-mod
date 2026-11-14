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
    private static final List<String> BASE_TYPES=List.of("accessory","axe","bow","hoe","pickaxe","potion","shield","shooter","shovel","staff","sword","totem","spells","summons","trident","arcanum","helmet","chestplate","leggings","boots");
    private static final List<String> ALL_TYPES=List.of("accessory","arcanum","axe","boots","bow","hoe","shooter","shovel","staff","summons","sword");
    private static final List<String> FUNCTIONAL_TABLES=List.of("27_acessories","27_bows","27_grimoires","27_instruments","27_potions","27_random_items","27_rofls","27_shooters","27_slabs","27_staffs","27_summons","27_swords");
    private static final Map<String,String> LEGACY_TABLES=legacyTables();
    private static final Set<Filter> REQUESTED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public enum Domain { CUSTOM, VANILLA, UNION;
        static Domain parse(String text){return valueOf(text.toUpperCase(Locale.ROOT));}
    }
    private record Filter(Domain domain,List<String> types){Filter{types=normalizeTypes(types);}}
    private static List<String> normalizeTypes(Collection<String> types){var normalized=new TreeSet<String>();for(var type:types)normalized.add(switch(type){case "spell","spellitem"->"spells";case "summoner"->"summons";default->type;});return List.copyOf(normalized);}
    private static Map<String,String> legacyTables(){var result=new LinkedHashMap<String,String>();for(var type:BASE_TYPES)result.put("lbc:transformables_"+type,"lbc:uniform/"+type);for(var type:ALL_TYPES)result.put("lbc:transformables_"+type+"_all","lbc:uniform/"+type+"_all");result.put("lbc:rofls","lbc:uniform/rofl");result.put("lbc:slabs","lbc:uniform/slab");result.put("lbc:spells_library","lbc:uniform/grimoire");for(var table:FUNCTIONAL_TABLES)result.put("lbc:"+table,"lbc:uniform/"+table);return Map.copyOf(result);}
    /** Translate only designated uniform consumers; weighted/set-specific callers keep their original ID. */
    public static String uniformTable(String legacy){return LEGACY_TABLES.getOrDefault(legacy,legacy);}
    public static Map<String,String> uniformTables(){return LEGACY_TABLES;}
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
    /** Resolve arbitrary category unions once at caller/bootstrap setup, never by scanning item data during a draw. */
    public void prepareTypes(Domain domain,Collection<String> types){catalog().prepare(new Filter(domain,List.copyOf(types)));}
    public Set<String> types(){return catalog().typedCustom.keySet();}
    public List<String> slots(Domain domain,Collection<String> types){var filter=new Filter(domain,List.copyOf(types));return catalog().prepare(filter).stream().map(slot->slot.id).toList();}
    public Set<Identifier> representedNativeItems(){return catalog().representedNative;}
    public Map<Identifier,String> excludedNativeItems(){return catalog().excludedNative;}
    public void draw(Domain domain,LootContext context,Consumer<ItemStack> output){
        draw(catalog().slots(domain),context,output);
    }
    public void draw(Domain domain,Collection<String> types,LootContext context,Consumer<ItemStack> output){draw(catalog().prepared(new Filter(domain,List.copyOf(types))),context,output);}
    private void draw(List<Slot> slots,LootContext context,Consumer<ItemStack> output){
        if(slots.isEmpty())return;int size=slots.size(),chosen=context.getRandom().nextInt(size);
        if(materialize(slots.get(chosen),context,output))return;
        // Only a failed native condition needs allocation. A random permutation
        // without replacement gives every eligible logical item one equal chance.
        tryRemaining(size,chosen,context.getRandom(),index->materialize(slots.get(index),context,output));
    }
    static boolean tryRemaining(int size,int chosen,net.minecraft.util.RandomSource random,java.util.function.IntPredicate attempt){
        var swaps=new HashMap<Integer,Integer>();int remaining=size-1;if(chosen!=remaining)swaps.put(chosen,remaining);
        while(remaining>0){int pick=random.nextInt(remaining),index=swaps.getOrDefault(pick,pick);remaining--;
            int tail=swaps.getOrDefault(remaining,remaining);if(pick!=remaining)swaps.put(pick,tail);swaps.remove(remaining);
            if(attempt.test(index))return true;
        }
        return false;
    }
    /** Explicit index seam verifies every logical slot without replacing the runtime random sampler. */
    void drawSlot(Domain domain,int index,LootContext context,Consumer<ItemStack> output){
        materialize(catalog().slots(domain).get(index),context,output);
    }
    void drawSlot(Domain domain,Collection<String> types,int index,LootContext context,Consumer<ItemStack> output){materialize(catalog().prepared(new Filter(domain,List.copyOf(types))).get(index),context,output);}
    private boolean materialize(Slot slot,LootContext context,Consumer<ItemStack> output){
        if(slot.tmt){output.accept(game.items().tmt().generate(context.getRandom()));return true;}
        if(slot.nativeItem!=null){output.accept(slot.nativeItem.getDefaultInstance().copy());return true;}
        // Pick a profile only inside the already selected logical slot. Conditions
        // retain native context semantics and cannot change another item's ticket count.
        var admitted=new ArrayList<Prepared>();
        for(var profile:slot.profiles){var prepared=profile.prepare(context);if(prepared!=null)admitted.add(prepared);}
        if(admitted.isEmpty())return false;
        admitted.get(context.getRandom().nextInt(admitted.size())).emit(output,context);return true;
    }
    private record Slot(String id,List<Node> profiles,Item nativeItem,boolean tmt){}
    private static Set<String> nativeTypes(Item item){
        var result=new HashSet<String>();var holder=item.builtInRegistryHolder();
        if(holder.is(net.minecraft.tags.ItemTags.SWORDS))result.add("sword");
        if(holder.is(net.minecraft.tags.ItemTags.AXES))result.add("axe");
        if(holder.is(net.minecraft.tags.ItemTags.HOES))result.add("hoe");
        if(holder.is(net.minecraft.tags.ItemTags.PICKAXES))result.add("pickaxe");
        if(holder.is(net.minecraft.tags.ItemTags.SHOVELS))result.add("shovel");
        if(holder.is(net.minecraft.tags.ItemTags.HEAD_ARMOR))result.add("helmet");
        if(holder.is(net.minecraft.tags.ItemTags.CHEST_ARMOR))result.add("chestplate");
        if(holder.is(net.minecraft.tags.ItemTags.LEG_ARMOR))result.add("leggings");
        if(holder.is(net.minecraft.tags.ItemTags.FOOT_ARMOR))result.add("boots");
        if(item instanceof BowItem)result.add("bow");if(item instanceof CrossbowItem)result.add("shooter");
        if(item instanceof ShieldItem)result.add("shield");if(item instanceof TridentItem)result.add("trident");
        if(item instanceof PotionItem||item instanceof SplashPotionItem||item instanceof LingeringPotionItem)result.add("potion");
        if(item instanceof SpawnEggItem)result.add("summons");
        if(item.getDefaultInstance().has(net.minecraft.core.component.DataComponents.DEATH_PROTECTION))result.add("totem");
        return result;
    }
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
        final Map<String,Slot> logical;
        final Map<String,List<Slot>> typedCustom,typedVanilla,typedUnion;
        final Map<Filter,List<Slot>> vectors=new java.util.concurrent.ConcurrentHashMap<>();
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
            var sourceVanilla=new HashMap<String,Slot>();
            for(var member:manifest.getAsJsonObject("vanillaSourceViews").getAsJsonObject("no_recursive_shulker").getAsJsonObject("members").entrySet()){
                var profiles=new ArrayList<Node>();for(var profile:member.getValue().getAsJsonArray())profiles.add(binding.node(profile.getAsString()));
                sourceVanilla.put(member.getKey(),new Slot(member.getKey(),List.copyOf(profiles),null,false));
            }
            var byId=new HashMap<String,Slot>();union.forEach(slot->byId.put(slot.id,slot));logical=Map.copyOf(byId);
            var categories=new TreeMap<String,List<Slot>>();
            for(var value:manifest.getAsJsonObject("typedViews").entrySet()){
                var row=value.getValue().getAsJsonObject();var members=new ArrayList<Slot>();var original=new HashSet<String>();row.getAsJsonArray("sourceMembers").forEach(id->original.add(id.getAsString()));
                for(var member:row.getAsJsonObject("members").entrySet()){
                    var base=byId.get(member.getKey());if(base==null)throw new IllegalStateException("Typed view refers to missing logical slot "+member.getKey());
                    var profiles=new ArrayList<Node>();for(var profile:member.getValue().getAsJsonArray())profiles.add(binding.node(profile.getAsString()));
                    members.add(new Slot(base.id,List.copyOf(profiles),profiles.isEmpty()?base.nativeItem:null,base.tmt));
                }
                // Native semantic tags/classes augment only broader views. They
                // never reinterpret a carrot/stick transport as a custom sword.
                if(value.getKey().endsWith("_all")){
                    String category=value.getKey().substring(0,value.getKey().length()-4);var present=new HashSet<String>();members.forEach(slot->present.add(slot.id));
                    for(var source:manifest.getAsJsonArray("custom")){
                        var authored=source.getAsJsonObject();String id=authored.get("id").getAsString();var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(authored.getAsJsonObject("identity").get("carrier").getAsString()));
                        if(!present.contains(id)&&nativeTypes(item).contains(category)){members.add(byId.get(id));present.add(id);}
                    }
                }
                members.sort(Comparator.comparing(Slot::id));categories.put(value.getKey(),List.copyOf(members));
            }
            categories.put("no_recursive_shulker",List.of());typedCustom=Map.copyOf(categories);
            var nativeCategories=new TreeMap<String,List<Slot>>();var combinedCategories=new TreeMap<String,List<Slot>>();
            for(var category:categories.keySet()){
                String semantic=category.endsWith("_all")?category.substring(0,category.length()-4):category;var eligible=new ArrayList<Slot>();var unionEligible=new ArrayList<Slot>();
                for(int i=0;i<vanilla.size();i++){
                    var slot=vanilla.get(i);boolean admitted=category.equals("no_recursive_shulker")?!slot.nativeItem.getDefaultInstance().has(net.minecraft.core.component.DataComponents.CONTAINER_LOOT):nativeTypes(slot.nativeItem).contains(semantic);
                    if(admitted){eligible.add(category.equals("no_recursive_shulker")?sourceVanilla.getOrDefault(slot.id,slot):slot);unionEligible.add(category.equals("no_recursive_shulker")?sourceVanilla.getOrDefault(slot.id,slot):unionVanillas.get(i));}
                }
                nativeCategories.put(category,List.copyOf(eligible));var combinedView=new LinkedHashMap<String,Slot>();categories.get(category).forEach(slot->combinedView.put(slot.id,slot));unionEligible.forEach(slot->combinedView.put(slot.id,slot));combinedCategories.put(category,List.copyOf(combinedView.values()));
            }
            typedVanilla=Map.copyOf(nativeCategories);typedUnion=Map.copyOf(combinedCategories);
            for(var domain:Domain.values()){
                vectors.put(new Filter(domain,List.of()),slots(domain));
                for(var category:categories.keySet())prepare(new Filter(domain,List.of(category)));
                for(var combination:manifest.getAsJsonArray("typedCombinations")){var names=new ArrayList<String>();combination.getAsJsonArray().forEach(name->names.add(name.getAsString()));prepare(new Filter(domain,names));}
            }
            for(var filter:REQUESTED)prepare(filter);
            representedNative=Set.copyOf(represented);findings=List.copyOf(problems);complete=sourceComplete&&nativeComplete;
            requireUnique(custom);requireUnique(vanilla);requireUnique(union);
        }
        List<Slot> slots(Domain domain){return switch(domain){case CUSTOM->custom;case VANILLA->vanilla;case UNION->union;};}
        List<Slot> prepared(Filter filter){var result=vectors.get(filter);if(result==null)throw new IllegalStateException("Prepare typed uniform selection before drawing: "+filter);return result;}
        List<Slot> prepare(Filter filter){return vectors.computeIfAbsent(filter,key->{
            if(key.types.isEmpty())return slots(key.domain);
            var source=switch(key.domain){case CUSTOM->typedCustom;case VANILLA->typedVanilla;case UNION->typedUnion;};var merged=new TreeMap<String,Slot>();
            for(var type:key.types){var members=source.get(type);if(members==null)throw new IllegalArgumentException("Unknown uniform item type "+type);
                for(var slot:members)merged.merge(slot.id,slot,(first,second)->{if(first.nativeItem!=null||second.nativeItem!=null||first.tmt)return first;var profiles=new LinkedHashSet<Node>(first.profiles);profiles.addAll(second.profiles);return new Slot(first.id,List.copyOf(profiles),null,false);});
            }
            return List.copyOf(merged.values());
        });}
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
                Codec.STRING.fieldOf("domain").forGetter(e->e.domain.name().toLowerCase(Locale.ROOT)),
                Codec.STRING.optionalFieldOf("item_type").forGetter(e->Optional.<String>empty()),
                Codec.STRING.listOf().optionalFieldOf("types",List.of()).forGetter(e->e.types),
                Codec.STRING.optionalFieldOf("slot").forGetter(e->e.slot)).and(uniformFields(i)).apply(i,Entry::new));
        private final Domain domain;
        private final List<String> types;
        private final Filter filter;
        private final Optional<String> slot;
        private Entry(String domain,Optional<String> type,List<String> types,Optional<String> slot,int weight,int quality,Optional<Holder<LootItemCondition>> condition,Optional<Holder<LootItemFunction>> modifier){
            super(weight,quality,condition,modifier);this.domain=Domain.parse(domain);var names=new ArrayList<>(types);type.ifPresent(names::add);this.types=normalizeTypes(names);
            this.filter=new Filter(this.domain,this.types);
            this.slot=slot;if(slot.isPresent()&&(this.domain!=Domain.CUSTOM||!this.types.isEmpty()))throw new IllegalArgumentException("An explicit custom slot cannot also select a type/domain lottery");
            var game=me.skywars2.SkyWars2.game();var pools=game==null||game.items()==null?null:game.items().allItemPools();if(pools!=null&&pools.catalog!=null)pools.catalog.prepare(filter);REQUESTED.add(filter);
        }
        @Override public MapCodec<Entry> codec(){return MAP_CODEC;}
        @Override public void createItemStack(Consumer<ItemStack> output,LootContext context){
            var game=me.skywars2.SkyWars2.game();if(game==null)throw new IllegalStateException("Uniform gameplay pool needs its native game owner");
            var pools=game.items().allItemPools();
            if(slot.isPresent()){
                var selected=pools.catalog().logical.get(slot.get());
                if(selected==null||selected.id.startsWith("vanilla/"))throw new IllegalArgumentException("Unknown custom logical slot "+slot.get());
                pools.materialize(selected,context,output);
            }else pools.draw(pools.catalog().prepared(filter),context,output);
        }
    }
}
