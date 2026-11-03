package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Source dropped combinations: event indexes, one real pair, one atomic native debit. */
final class LbcChargedBowCrafting {
    private final GameController game;
    private final Map<UUID,ItemEntity> inputs=new LinkedHashMap<>();
    private int phase;
    LbcChargedBowCrafting(GameController game){this.game=game;}
    static boolean tiny(ItemStack stack){return stack.is(LbcWands.TINY_ELECTRO)||ItemData.intValue(stack,"stelectro_tiny",0)==1;}
    static LbcCraftingRecipe.Kind kind(ItemStack a,ItemStack b){
        if(tiny(a)&&b.getItem() instanceof BowItem&&!LbcChargedBowItems.railgun(b)
                ||tiny(b)&&a.getItem() instanceof BowItem&&!LbcChargedBowItems.railgun(a))return LbcCraftingRecipe.Kind.RAILGUN;
        if(LbcChargedBowItems.railgun(a)&&ItemData.intValue(b,"reactive_shish",0)==1
                ||LbcChargedBowItems.railgun(b)&&ItemData.intValue(a,"reactive_shish",0)==1)return LbcCraftingRecipe.Kind.SHISHGUN;
        return null;
    }
    void loaded(Entity body){if(body instanceof ItemEntity item)itemChanged(item);}
    void unloaded(Entity body){inputs.remove(body.getUUID());}
    void itemChanged(ItemEntity item){if(tiny(item.getItem())||LbcChargedBowItems.railgun(item.getItem()))inputs.put(item.getUUID(),item);else inputs.remove(item.getUUID());}
    private boolean valid(ItemEntity item){return !item.isRemoved()&&item.getAge()<5999&&!item.getItem().isEmpty()
            &&item.level()==game.maps().level()&&!game.items().hasStaleItemEpoch(item)
            &&!game.items().timeStop().frozen(item)&&!game.items().timeStop().stopped((ServerLevel)item.level());}
    void tick(){
        if(!game.playing()||game.supernovaMapTransition()||game.items().timeStop().stopped(game.maps().level()))return;
        if(++phase<20)return;phase=0;
        for(var item:List.copyOf(inputs.values())){
            if(!valid(item)){if(item.isRemoved()||item.getAge()>=5999)inputs.remove(item.getUUID());continue;}
            var partner=((ServerLevel)item.level()).getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(1),other->other!=item
                    &&valid(other)&&other.position().distanceToSqr(item.position())<=1&&kind(item.getItem(),other.getItem())!=null)
                    .stream().min(Comparator.comparingDouble(other->other.position().distanceToSqr(item.position()))).orElse(null);
            if(partner==null)continue;var recipe=kind(item.getItem(),partner.getItem());
            var level=(ServerLevel)item.level();var at=item.position();
            var rolled=game.lbc().rollTable(level,at,null,"lbc:ported/"+(recipe==LbcCraftingRecipe.Kind.RAILGUN?"railgun":"shishgun"));
            if(rolled.isEmpty())continue;var output=new ItemEntity(level,at.x,at.y,at.z,rolled.getFirst().copyWithCount(1));output.setDefaultPickUpDelay();
            if(!level.addFreshEntity(output))continue;
            boolean retain=recipe==LbcCraftingRecipe.Kind.RAILGUN&&level.getRandom().nextFloat()<.6f;
            if(!retain||!tiny(item.getItem()))debit(item);if(!retain||!tiny(partner.getItem()))debit(partner);
            present(level,at,recipe);itemChanged(item);itemChanged(partner);
        }
    }
    private void debit(ItemEntity item){var stack=item.getItem().copy();stack.shrink(1);if(stack.isEmpty()){item.discard();inputs.remove(item.getUUID());}else item.setItem(stack);}
    static void present(ServerLevel level,Vec3 at,LbcCraftingRecipe.Kind recipe){
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,at.x,at.y,at.z,100,1,1,1,1);
        LbcOverload.sound(level,at,"minecraft:block.enchantment_table.use",1,1,0,0);
    }
    void stop(){inputs.clear();phase=0;}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final int clock=phase;final List<UUID> ids=inputs.values().stream().filter(e->!e.isRemoved()).map(Entity::getUUID).toList();
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){var next=new LinkedHashMap<UUID,ItemEntity>();var missing=new LinkedHashSet<UUID>();
            for(var id:ids)if(lookup.apply(id) instanceof ItemEntity item)next.put(id,item);else missing.add(id);
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{inputs.clear();inputs.putAll(next);phase=clock;});}
    }
}
