package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import java.util.function.Consumer;
import me.skywars2.mixin.SynchroRankedMixin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Canonical lucky outcomes are shared; personal bonuses/filters never reweight the base chain. */
final class SynchroLuckyDraws {
    private final SynchroRanked owner;
    private final Map<String,Model> models=new HashMap<>();
    record Draw(String family,long index,String outcome,boolean bonus,boolean skipped){}
    private static final ThreadLocal<Consumer<Draw>> OBSERVER=new ThreadLocal<>();
    record Observation(Consumer<Draw> previous) implements AutoCloseable {public void close(){if(previous==null)OBSERVER.remove();else OBSERVER.set(previous);}}
    static Observation observe(Consumer<Draw> observer){var previous=OBSERVER.get();OBSERVER.set(observer);return new Observation(previous);}
    private void observe(String family,LootContext context,Outcome outcome,boolean bonus,boolean skipped){var observer=OBSERVER.get();if(observer==null)return;var source=context.getOptional(LootContextParams.THIS_ENTITY);long index=source==null?-1:owner.position(source.getUUID(),Identifier.parse("lbc:native_lucky/"+family))-(bonus?0:1);observer.accept(new Draw(family,index,outcome.name,bonus,skipped));}
    private record Outcome(String name,LootPoolEntryContainer admission,LootTable emission,boolean forbidden,LootTable phone){}
    private record Ticket(Outcome outcome,LootPoolEntry ticket){}
    private record Model(List<Outcome> base,List<Outcome> all,Outcome bonus,LootPoolEntryContainer guard,LootPoolEntryContainer phone){}
    SynchroLuckyDraws(SynchroRanked owner){this.owner=owner;}
    void clear(){models.clear();}
    static String family(Identifier id){if(!id.getNamespace().equals("lbc"))return null;return switch(id.getPath()){case "luckyblock"->"normal";case "nekoluckyblock"->"neko";case "loliluckyblock"->"loli";default->null;};}
    boolean draw(LootTable table,LootContext context,Consumer<ItemStack> output){
        if(!owner.admitted(context))return false;var id=owner.tableId(table,context);if(id==null)return false;String family=family(id);if(family==null)return false;
        var visit=LootContext.createVisitedEntry(table);if(!context.pushVisitedElement(visit))return true;
        try{
            var model=models.computeIfAbsent(family,key->compile(key,context));
            if(model.guard!=null&&!admitted(model.guard,context))return true;
            boolean phone=model.phone!=null&&admitted(model.phone,context);
            // A mask result inserts an extra outcome before the next base candidate.
            if(model.bonus!=null&&admitted(model.bonus.admission,context)){
                try(var rng=random(context,owner.next(context,Identifier.parse("lbc:synchro_bonus/"+family)))){
                    var tickets=expand(model.all,context);var chosen=choose(tickets,context.getRandom());
                    if(chosen!=null&&chosen.outcome==model.bonus){observe(family,context,chosen.outcome,true,false);chosen.outcome.emission.getRandomItemsRaw(context,output);return true;}
                }
            }
            // The shipped Loli/Neko admission rates are substantial; bound malformed custom packs.
            for(int attempt=0;attempt<256;attempt++)try(var rng=random(context,owner.next(context,Identifier.parse("lbc:native_lucky/"+family)))){
                var chosen=choose(expand(model.base,context),context.getRandom());if(chosen==null)return true;
                boolean skipped=phone&&(chosen.outcome.forbidden||family.equals("neko")&&chosen.outcome.phone==null);
                observe(family,context,chosen.outcome,false,skipped);
                if(skipped)continue;
                var emission=phone&&chosen.outcome.phone!=null?chosen.outcome.phone:chosen.outcome.emission;
                emission.getRandomItemsRaw(context,output);return true;
            }
            return true;
        }finally{context.popVisitedElement(visit);}
    }
    private static boolean admitted(LootPoolEntryContainer entry,LootContext context){return entry.expand(context,ignored->{});}
    private static List<Ticket> expand(List<Outcome> entries,LootContext context){var result=new ArrayList<Ticket>();for(var entry:entries)entry.admission.expand(context,ticket->{if(ticket.getWeight(0)>0)result.add(new Ticket(entry,ticket));});return result;}
    private static Ticket choose(List<Ticket> tickets,RandomSource random){
        if(tickets.isEmpty())return null;if(tickets.size()==1)return tickets.getFirst();
        int total=0;for(var t:tickets)total=Math.addExact(total,t.ticket.getWeight(0));int pick=random.nextInt(total);
        for(var ticket:tickets){pick-=ticket.ticket.getWeight(0);if(pick<0)return ticket;}throw new IllegalStateException("Native lucky ticket weights changed during selection");
    }
    private record RandomScope(SynchroLootContext state,RandomSource previous,Integer slab) implements AutoCloseable {public void close(){state.skywars2$synchroPrepare(previous);state.skywars2$synchroSlab(slab);}}
    private static RandomScope random(LootContext context,RandomSource random){var state=(SynchroLootContext)context;var prior=new RandomScope(state,state.skywars2$synchroRandom(),state.skywars2$synchroSlab());state.skywars2$synchroPrepare(random);state.skywars2$synchroSlab(null);return prior;}
    private static JsonObject json(String id,LootContext context){var table=context.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));return LootTable.DIRECT_CODEC.encodeStart(context.getLevel().getServer().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE),table).getOrThrow().getAsJsonObject();}
    private static LootTable parse(JsonObject json,LootContext context){return LootTable.DIRECT_CODEC.parse(context.getLevel().getServer().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE),json).getOrThrow();}
    // Only the first pool chooses the primary outcome; emission retains all auxiliary source pools.
    private static List<LootPoolEntryContainer> entries(LootTable table){var pools=((SynchroRankedMixin.Pools)table).skywars2$pools();if(pools.isEmpty())throw new IllegalStateException("Logical lucky family needs a primary source pool");return pools.getFirst().entries;}
    private static JsonArray jsonEntries(JsonObject root){return root.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");}
    private static JsonObject singleton(JsonObject root,JsonObject entry){
        var result=root.deepCopy();var pool=result.getAsJsonArray("pools").get(0).getAsJsonObject();pool.addProperty("rolls",1);pool.remove("bonus_rolls");var one=new JsonArray();one.add(entry.deepCopy());pool.add("entries",one);return result;
    }
    private static LootTable emission(JsonObject root,JsonObject entry,LootContext context){var selected=entry.deepCopy();selected.remove("condition");selected.remove("conditions");return parse(singleton(root,selected),context);}
    private static boolean mentions(JsonElement node,String flag){return node!=null&&node.toString().contains(flag);}
    /** Remove only negative phone guards; retain ranked, mode, seasonal and all other source gates. */
    private static JsonElement ordinary(JsonElement node){
        if(node==null)return null;if(!node.isJsonObject())return node.deepCopy();var o=node.getAsJsonObject();String type=o.has("type")?o.get("type").getAsString():"";
        if(type.equals("minecraft:inverted")&&mentions(o.get("term"),"zaika"))return null;
        var result=o.deepCopy();if(type.equals("minecraft:all_of")){
            var terms=new JsonArray();for(var child:o.getAsJsonArray("terms")){var value=ordinary(child);if(value!=null)terms.add(value);}if(terms.isEmpty())return null;result.add("terms",terms);
        }
        return result;
    }
    private static JsonObject ordinaryEntry(JsonObject entry){var result=entry.deepCopy();if(result.has("condition")){var value=ordinary(result.get("condition"));if(value==null)result.remove("condition");else result.add("condition",value);}return result;}
    private static JsonElement phonePredicate(JsonElement node){
        if(node==null||!node.isJsonObject())return null;var o=node.getAsJsonObject();
        if(o.has("type")&&o.get("type").getAsString().equals("minecraft:any_of")&&mentions(o,"zaika"))return o.deepCopy();
        for(var value:o.entrySet()){if(value.getValue().isJsonArray())for(var child:value.getValue().getAsJsonArray()){var found=phonePredicate(child);if(found!=null)return found;}else{var found=phonePredicate(value.getValue());if(found!=null)return found;}}
        return null;
    }
    private static LootPoolEntryContainer probe(JsonElement condition,LootContext context){
        if(condition==null)return null;var entry=new JsonObject();entry.addProperty("type","minecraft:empty");entry.add("condition",condition.deepCopy());var root=new JsonObject();var pool=new JsonObject();pool.addProperty("rolls",1);var list=new JsonArray();list.add(entry);pool.add("entries",list);var pools=new JsonArray();pools.add(pool);root.add("pools",pools);return entries(parse(root,context)).getFirst();
    }
    private Model compile(String family,LootContext context){
        var nekoRoot=json("nekoluckyblock",context);var ordinaryNeko=jsonEntries(nekoRoot).get(0).getAsJsonObject();
        var phone=probe(phonePredicate(jsonEntries(nekoRoot).get(1).getAsJsonObject().get("condition")),context);
        var source=json(family.equals("normal")?"luckyblock":family.equals("loli")?"loliluckyblock":"nekolucky_items",context);
        var baseline=source.deepCopy();var sourceEntries=jsonEntries(source);var baselineEntries=jsonEntries(baseline);
        for(int i=0;i<baselineEntries.size();i++)baselineEntries.set(i,ordinaryEntry(baselineEntries.get(i).getAsJsonObject()));
        var compiled=entries(parse(baseline,context));var all=new ArrayList<Outcome>();var base=new ArrayList<Outcome>();Outcome bonus=null;
        var full=family.equals("neko")?json("nekolucky_zaika_items",context):null;var fullByName=new HashMap<String,JsonObject>();if(full!=null)for(var entry:jsonEntries(full)){var row=entry.getAsJsonObject();fullByName.put(row.get("value").getAsString(),row);}
        for(int i=0;i<compiled.size();i++){
            var original=sourceEntries.get(i).getAsJsonObject();var neutral=baselineEntries.get(i).getAsJsonObject();LootTable replacement=null;
            if(full!=null&&original.has("value")){var key=original.get("value").getAsString().replaceAll("_pre$","_full");var row=fullByName.get(key);if(row!=null)replacement=emission(full,row,context);}
            String name=original.has("value")?original.get("value").getAsString():family+"/"+i;
            var outcome=new Outcome(name,compiled.get(i),emission(baseline,neutral,context),mentions(original.get("condition"),"zaika"),replacement);all.add(outcome);
            if(family.equals("normal")&&mentions(original.get("condition"),"mellstroy_mask"))bonus=outcome;else base.add(outcome);
        }
        var guard=family.equals("neko")?probe(ordinary(ordinaryNeko.get("condition")),context):null;
        return new Model(List.copyOf(base),List.copyOf(all),bonus,guard,family.equals("normal")?null:phone);
    }
}
