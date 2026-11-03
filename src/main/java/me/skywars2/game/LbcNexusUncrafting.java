package me.skywars2.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Fixed source selectors and bounded native, per-unit disassembly transactions. */
final class LbcNexusUncrafting {
    static final int UNIT_BUDGET=64, VISIT_BUDGET=256;
    static final long NANOS_BUDGET=1_000_000;
    private final GameController game;
    private Map<Identifier,LootTable> items=Map.of(),effects=Map.of();
    private final ArrayDeque<Target> pending=new ArrayDeque<>();
    private boolean frozen,loaded;
    private int lastTick=Integer.MIN_VALUE;

    private static final class Target {
        final ServerLevel level;
        final ItemEntity entity;
        final ServerPlayer owner;
        final ItemStack original;
        final Vec3 position;
        final List<LootTable> tables;
        final boolean xp;
        int remaining;
        Target(ServerLevel level,ItemEntity entity,ServerPlayer owner,List<LootTable> tables,boolean xp){
            this.level=level;this.entity=entity;this.owner=owner;this.tables=tables;this.xp=xp;
            original=entity.getItem().copy();position=entity.position();remaining=original.getCount();
        }
        boolean valid(){return remaining>0&&entity.isAlive()&&entity.level()==level
                &&!entity.getItem().isEmpty()&&ItemStack.isSameItemSameComponents(original,entity.getItem());}
    }

    LbcNexusUncrafting(GameController game){this.game=game;}

    /** Resolve the whole mapping before publishing it; a rejected reload preserves the old cache. */
    void reload(){
        try(var input=LbcNexusUncrafting.class.getResourceAsStream("/skywars2/nexus_uncrafting.json")){
            if(input==null)throw new IllegalStateException("Missing /skywars2/nexus_uncrafting.json");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var resolved=new HashMap<Identifier,LootTable>();
            var nextItems=load(root.getAsJsonObject("items"),659,"items",resolved);
            var nextEffects=load(root.getAsJsonObject("suspicious_stew_effects"),9,"suspicious_stew_effects",resolved);
            items=Map.copyOf(nextItems);effects=Map.copyOf(nextEffects);loaded=true;
        }catch(Exception error){throw new IllegalStateException("Cannot load Nexus uncrafting selectors; previous cache retained: "+error.getMessage(),error);}
    }

    private Map<Identifier,LootTable> load(JsonObject source,int count,String section,Map<Identifier,LootTable> resolved){
        if(source==null||source.size()!=count)throw new IllegalStateException("Expected "+count+" entries in "+section);
        var result=new HashMap<Identifier,LootTable>();
        var registry=game.server().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE);
        for(var entry:source.entrySet()){
            var input=Identifier.parse(entry.getKey());
            var output=Identifier.parse(entry.getValue().getAsString());
            if(!output.getNamespace().equals("lbc")||!output.getPath().startsWith("uncrafted/"))
                throw new IllegalStateException("Unexpected Nexus table "+output+" for "+input);
            var table=resolved.computeIfAbsent(output,id->registry.get(ResourceKey.create(Registries.LOOT_TABLE,id))
                    .orElseThrow(()->new IllegalStateException("Missing native loot table "+id+" selected by "+section+"/"+input)).value());
            if(table==LootTable.EMPTY)throw new IllegalStateException("Empty native loot table "+output+" selected by "+input);
            result.put(input,table);
        }
        return result;
    }

    /** Snapshot the complete initial cohort once; outputs can never enter this request. */
    int request(ServerLevel level,Vec3 point,ServerPlayer owner,boolean allowCustom,boolean stewFallbackXp){
        if(frozen)return 0;
        if(!loaded)throw new IllegalStateException("Nexus uncrafting cache has not been loaded");
        int count=0;
        for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(point,point).inflate(2),
                item->item.isAlive()&&!item.getItem().isEmpty()&&item.position().distanceToSqr(point)<=4)){
            var stack=entity.getItem();
            if(!allowCustom&&LbcDimensionItems.custom(stack))continue;
            var base=LbcPhdCrafting.legacyItem(stack);
            var selected=new ArrayList<LootTable>();
            boolean xp;
            if(base==Items.SUSPICIOUS_STEW){
                // Source checks each effect predicate once, even if a component repeats it.
                var matched=new LinkedHashSet<Identifier>();
                for(var effect:stack.getOrDefault(DataComponents.SUSPICIOUS_STEW_EFFECTS,SuspiciousStewEffects.EMPTY).effects()){
                    var id=BuiltInRegistries.MOB_EFFECT.getKey(effect.effect().value());
                    if(effects.containsKey(id))matched.add(id);
                }
                for(var id:matched)selected.add(effects.get(id));
                xp=stewFallbackXp||selected.isEmpty();
            }else{
                var table=items.get(BuiltInRegistries.ITEM.getKey(base));
                if(table!=null)selected.add(table);
                xp=table==null;
            }
            pending.addLast(new Target(level,entity,owner,List.copyOf(selected),xp));count++;
        }
        return count;
    }

    private boolean paused(Target target){return game.items().timeStop().stopped(target.level)
            ||target.entity.entityTags().contains("time_stopped");}

    void tick(){
        int tick=game.server().getTickCount();
        if(frozen||lastTick==tick)return;
        lastTick=tick;
        if(!game.server().tickRateManager().runsNormally()||game.items().wands().legacyTimeStopped())return;
        long deadline=System.nanoTime()+NANOS_BUDGET;
        int units=0,visits=0,paused=0;
        while(!pending.isEmpty()&&units<UNIT_BUDGET&&visits++<VISIT_BUDGET&&System.nanoTime()<deadline){
            var target=pending.removeFirst();
            if(!target.valid()){paused=0;continue;}
            if(paused(target)){
                pending.addLast(target);
                if(++paused>=pending.size())break;
                continue;
            }
            paused=0;
            commit(target);units++;
            if(target.valid())pending.addLast(target);
        }
    }

    /** No yield or checkpoint boundary may split debit from its complete native outputs. */
    private void commit(Target target){
        var params=new LootParams.Builder(target.level).withParameter(LootContextParams.ORIGIN,target.position)
                .withParameter(LootContextParams.THIS_ENTITY,target.entity).create(LootContextParamSets.COMMAND);
        var outputs=new ArrayList<ItemStack>();
        for(var table:target.tables)outputs.addAll(table.getRandomItems(params));
        int xp=target.xp?1+target.level.getRandom().nextInt(3):0;
        var remaining=target.entity.getItem().copy();remaining.shrink(1);
        target.remaining--;
        if(remaining.isEmpty())target.entity.discard();else target.entity.setItem(remaining);
        for(var stack:outputs)if(!stack.isEmpty()){
            var at=target.position;
            var output=new ItemEntity(target.level,at.x,at.y,at.z,stack);
            output.setDefaultPickUpDelay();
            if(target.owner!=null)output.setThrower(target.owner);
            game.items().markCurrentItemEpoch(output);
            target.level.addFreshEntity(output);
        }
        // The existing Nexus XP producer intercepts these real orbs before vanilla merging.
        if(xp>0)target.level.addFreshEntity(new ExperienceOrb(target.level,target.position.x,target.position.y,target.position.z,xp));
        target.level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,target.position.x,target.position.y+.76,target.position.z,1,0,0,0,.1);
    }

    int pending(){return pending.size();}
    int pendingUnits(){int units=0;for(var target:pending)units+=target.remaining;return units;}
    int tableCount(){return items.size();}
    int effectTableCount(){return effects.size();}
    void stop(){pending.clear();frozen=false;lastTick=Integer.MIN_VALUE;}
    /** Uncommitted work is cancelled; committed bodies belong to the native world checkpoint. */
    void prepareFrozen(){stop();frozen=true;}
    void publishFrozen(){frozen=false;lastTick=game.server().getTickCount();}
}
