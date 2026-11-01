package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.phys.Vec3;

/** Source physical armor fusions sharing one tracked item lifecycle and cadence. */
public final class LbcNyauwuCrafting {
    private static final String[] PARTS={"helmet","chestplate","leggings","boots"};
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private final GameController game;
    // Track all item identities so an in-place component change can become eligible.
    private final Set<ItemEntity> items=Collections.newSetFromMap(new IdentityHashMap<>());
    private int phase;

    public LbcNyauwuCrafting(GameController game){this.game=game;}

    public static int part(ItemStack stack,String family){
        if(stack.isEmpty())return -1;
        var data=LbcFartus.data(stack);
        if(data.getIntOr(family,0)!=1)return -1;
        var equipment=stack.get(DataComponents.EQUIPPABLE);
        if(equipment==null||equipment.assetId().isEmpty()
                ||!equipment.assetId().get().identifier().equals(Identifier.parse("lbc:"+(family.equals("reactive")?"steampunk":family))))return -1;
        for(int i=0;i<PARTS.length;i++)if(data.getIntOr(PARTS[i],0)==1&&equipment.slot()==SLOTS[i])return i;
        return -1;
    }

    private enum Fusion {
        NYAUWU("mage","phantom","neko_set_femboy",.839f,.376f,.933f,2),
        MK2("reactive","techno","neko_set_technomk2",.376f,.729f,.933f,1),
        FLYING("hermes","reactive_boots","neko_set_steampunk",.655f,1,.702f,2);
        final String first,second,output;
        final float red,green,blue,scale;
        Fusion(String first,String second,String output,float red,float green,float blue,float scale){
            this.first=first;this.second=second;this.output=output;
            this.red=red;this.green=green;this.blue=blue;this.scale=scale;
        }
        String table(int part){return "lbc:"+output+"/"+(this==FLYING?"boots_spectral":PARTS[part]);}
    }

    private static int ingredientPart(ItemStack stack,String family){
        if(!family.equals("hermes")&&!family.equals("reactive_boots"))return part(stack,family);
        if(!stack.is(Items.LEATHER_BOOTS))return -1;
        var data=LbcFartus.data(stack);
        var model=stack.get(DataComponents.ITEM_MODEL);
        if(model==null)return -1;
        boolean hermes=family.equals("hermes");
        return data.getIntOr(hermes?"jumpboost":"reactive",0)==1&&data.getIntOr("boots",0)==1
                &&model.equals(Identifier.parse(hermes?"lbc:armors/hermes_boots":"lbc:armors/reactive"))?3:-1;
    }

    /** Exact two occupied slots; matching and preview do not mutate the inputs. */
    private static int craftingPart(CraftingInput input,Fusion fusion){
        if(input.ingredientCount()!=2)return -1;
        int first=-1,second=-1;
        for(var stack:input.items())if(!stack.isEmpty()){
            int a=ingredientPart(stack,fusion.first),b=ingredientPart(stack,fusion.second);
            if(a>=0&&first<0)first=a;
            else if(b>=0&&second<0)second=b;
            else return -1;
        }
        return first>=0&&first==second?first:-1;
    }

    public static int craftingPart(CraftingInput input){return craftingPart(input,Fusion.NYAUWU);}
    public static int mk2CraftingPart(CraftingInput input){return craftingPart(input,Fusion.MK2);}
    public static boolean flyingBootsMatches(CraftingInput input){return craftingPart(input,Fusion.FLYING)==3;}

    public static LbcCraftingRecipe.Kind mk2Kind(CraftingInput input){
        return switch(mk2CraftingPart(input)){
            case 0->LbcCraftingRecipe.Kind.CYBERPUNK_HELMET;
            case 1->LbcCraftingRecipe.Kind.CYBERPUNK_CHESTPLATE;
            case 2->LbcCraftingRecipe.Kind.CYBERPUNK_LEGGINGS;
            case 3->LbcCraftingRecipe.Kind.CYBERPUNK_BOOTS;
            default->null;
        };
    }

    public static LbcCraftingRecipe.Kind kind(CraftingInput input){
        return switch(craftingPart(input)){
            case 0->LbcCraftingRecipe.Kind.NYAUWU_HELMET;
            case 1->LbcCraftingRecipe.Kind.NYAUWU_CHESTPLATE;
            case 2->LbcCraftingRecipe.Kind.NYAUWU_LEGGINGS;
            case 3->LbcCraftingRecipe.Kind.NYAUWU_BOOTS;
            default->null;
        };
    }

    public static ItemStack assemble(GameController game,CraftingInput input){return assemble(game,input,Fusion.NYAUWU);}
    public static ItemStack assembleMk2(GameController game,CraftingInput input){return assemble(game,input,Fusion.MK2);}
    public static ItemStack assembleFlyingBoots(GameController game,CraftingInput input){return assemble(game,input,Fusion.FLYING);}

    private static ItemStack assemble(GameController game,CraftingInput input,Fusion fusion){
        int part=craftingPart(input,fusion);if(part<0)return ItemStack.EMPTY;
        var result=game.lbc().rollTable(game.maps().level(),Vec3.ZERO,null,fusion.table(part));
        return result.size()==1&&result.getFirst().getCount()==1?result.getFirst().copy():ItemStack.EMPTY;
    }

    public static void crafted(ServerLevel level,Vec3 at){crafted(level,at,Fusion.NYAUWU);}
    public static void craftedMk2(ServerLevel level,Vec3 at){crafted(level,at,Fusion.MK2);}
    public static void craftedFlyingBoots(ServerLevel level,Vec3 at){crafted(level,at,Fusion.FLYING);}

    private static void crafted(ServerLevel level,Vec3 at,Fusion fusion){
        level.sendParticles(new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1,fusion.red,fusion.green,fusion.blue)&0xffffff,fusion.scale),
                true,false,at.x,at.y+1,at.z,100,1,1,1,1);
        LbcOverload.sound(level,at,"minecraft:block.enchantment_table.use",1,1,0,0);
    }

    public void loaded(Entity entity){
        if(entity instanceof ItemEntity item&&item.level()==game.maps().level())items.add(item);
    }
    public void unloaded(Entity entity){items.remove(entity);}
    public void stop(){items.clear();phase=0;}

    private boolean eligible(ItemEntity item){
        return !item.isRemoved()&&item.level()==game.maps().level()&&!item.getItem().isEmpty()
                &&!game.items().hasStaleItemEpoch(item)&&!item.entityTags().contains("time_stopped");
    }
    private static void consumeOne(ItemEntity item){
        if(item.getItem().getCount()==1)item.discard();
        else item.setItem(item.getItem().copyWithCount(item.getItem().getCount()-1));
    }

    /** Shulker upgrades share the established tracked ground cadence and atomic publication. */
    private boolean tryInfiniteShulker(ItemEntity first){
        if(!LbcInfiniteShulker.isBase(first.getItem())&&!LbcInfiniteShulker.isUpgraded(first.getItem()))return false;
        var level=game.maps().level();
        for(var second:level.getEntitiesOfClass(ItemEntity.class,first.getBoundingBox().inflate(1),other->
                other!=first&&eligible(other)&&other.position().distanceToSqr(first.position())>=.000001
                        &&other.position().distanceToSqr(first.position())<=1)){
            var kind=LbcInfiniteShulker.pairKind(first.getItem(),second.getItem());if(kind==null)continue;
            var stack=LbcInfiniteShulker.result(game,kind);if(stack.isEmpty())return false;
            var at=first.position();var output=new ItemEntity(level,at.x,at.y+.1,at.z,stack);
            output.setDefaultPickUpDelay();if(first.getOwner() instanceof LivingEntity owner)output.setThrower(owner);
            game.items().markCurrentItemEpoch(output);
            if(!level.addFreshEntity(output))return false;
            consumeOne(first);consumeOne(second);LbcInfiniteShulker.crafted(level,at,kind);return true;
        }
        return false;
    }

    /** Source chance40 predicate: exactly forty percent, with no draw during matching. */
    public static boolean technoConsumesWand(float roll){return roll<.4f;}

    /** Ground-only source recipe. Five-item native publication is transactional before debit. */
    private boolean tryTechnoSet(ItemEntity wand){
        if(!eligible(wand)||LbcFartus.data(wand.getItem()).getIntOr("stelectro_tiny",0)!=1)return false;
        var level=game.maps().level();
        var iron=new ItemEntity[4];
        var ironItems=List.of(Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS);
        // One bounded lookup around the already tracked donor; source matches native iron item IDs.
        var nearby=level.getEntitiesOfClass(ItemEntity.class,wand.getBoundingBox().inflate(1),other->
                other!=wand&&eligible(other)&&other.position().distanceToSqr(wand.position())>=.000001
                        &&other.position().distanceToSqr(wand.position())<=1);
        for(var item:nearby)for(int part=0;part<iron.length;part++)
            if(iron[part]==null&&item.getItem().is(ironItems.get(part)))iron[part]=item;
        for(var item:iron)if(item==null)return false;
        Vec3 at=wand.position();
        LivingEntity owner=wand.getOwner() instanceof LivingEntity living?living:null;
        var rolls=game.lbc().rollTable(level,at,owner,"lbc:neko_set_techno_full");
        if(rolls.size()!=5||rolls.stream().anyMatch(stack->stack.isEmpty()||stack.getCount()!=1))return false;
        var published=new ArrayList<ItemEntity>(5);
        boolean complete=false;
        try{
            for(var stack:rolls){
                var output=new ItemEntity(level,at.x,at.y,at.z,stack.copy());
                output.setDefaultPickUpDelay();if(owner!=null)output.setThrower(owner);
                game.items().markCurrentItemEpoch(output);
                if(!level.addFreshEntity(output))return false;
                published.add(output);
            }
            complete=true;
        }finally{
            // No item can be picked up between these synchronous insertions.
            if(!complete)for(var output:published)output.discard();
        }
        for(var item:iron)consumeOne(item);
        if(technoConsumesWand(level.getRandom().nextFloat()))consumeOne(wand);
        level.sendParticles(ParticleTypes.ENCHANT,true,false,at.x,at.y+.2,at.z,100,1,1,1,1);
        LbcOverload.sound(level,at,"minecraft:block.enchantment_table.use",1,1,0,0);
        return true;
    }

    public void tick(){
        items.removeIf(item->item.isRemoved()||item.level()!=game.maps().level());
        var level=game.maps().level();
        if(level==null||!game.playing()||game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(level)
                ||game.items().timeStop().stopped(level)||game.items().wands().timeStopped())return;
        phase=(phase+1)%20;if(phase!=0)return;
        for(var first:List.copyOf(items)){
            if(!eligible(first)||LbcFartus.data(first.getItem()).getIntOr("craftusable",0)!=1)continue;
            if(tryInfiniteShulker(first)||tryTechnoSet(first))continue;
            for(var fusion:Fusion.values()){
                if(!eligible(first))break;
                int part=ingredientPart(first.getItem(),fusion.first);
                if(part<0)continue;
                for(var second:level.getEntitiesOfClass(ItemEntity.class,first.getBoundingBox().inflate(1),other->
                        other!=first&&eligible(other)&&other.position().distanceToSqr(first.position())>=.000001
                                &&other.position().distanceToSqr(first.position())<=1
                                &&ingredientPart(other.getItem(),fusion.second)==part)){
                    Vec3 at=first.position();
                    LivingEntity owner=first.getOwner() instanceof LivingEntity living?living:null;
                    var rolls=game.lbc().rollTable(level,at,owner,fusion.table(part));
                    if(rolls.size()!=1||rolls.getFirst().isEmpty()||rolls.getFirst().getCount()!=1)break;
                    var output=new ItemEntity(level,at.x,at.y+(fusion==Fusion.FLYING?0:.1),at.z,rolls.getFirst().copy());
                    output.setDefaultPickUpDelay();if(owner!=null)output.setThrower(owner);
                    game.items().markCurrentItemEpoch(output);
                    // Publish first: rejected insertion cannot debit either input.
                    if(!level.addFreshEntity(output))break;
                    consumeOne(first);consumeOne(second);crafted(level,at,fusion);break;
                }
            }
        }
    }
}
