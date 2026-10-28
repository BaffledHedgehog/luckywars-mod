package me.skywars2.game;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.phys.Vec3;

/** Physical source Mage + Phantom fusion, using existing entity lifecycle notifications. */
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
                ||!equipment.assetId().get().identifier().equals(Identifier.parse("lbc:"+family)))return -1;
        for(int i=0;i<PARTS.length;i++)if(data.getIntOr(PARTS[i],0)==1&&equipment.slot()==SLOTS[i])return i;
        return -1;
    }

    /** Exact two occupied slots; preview does not consume ingredients or emit effects. */
    public static int craftingPart(CraftingInput input){
        if(input.ingredientCount()!=2)return -1;
        int mage=-1,phantom=-1;
        for(var stack:input.items())if(!stack.isEmpty()){
            int m=part(stack,"mage"),p=part(stack,"phantom");
            if(m>=0&&mage<0)mage=m;
            else if(p>=0&&phantom<0)phantom=p;
            else return -1;
        }
        return mage>=0&&mage==phantom?mage:-1;
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

    public static ItemStack assemble(GameController game,CraftingInput input){
        int part=craftingPart(input);if(part<0)return ItemStack.EMPTY;
        var result=game.lbc().rollTable(game.maps().level(),Vec3.ZERO,null,table(part));
        return result.size()==1&&result.getFirst().getCount()==1?result.getFirst().copy():ItemStack.EMPTY;
    }

    private static String table(int part){return "lbc:neko_set_femboy/"+PARTS[part];}

    public static void crafted(ServerLevel level,Vec3 at){
        level.sendParticles(new DustParticleOptions(net.minecraft.util.ARGB.colorFromFloat(1,.839f,.376f,.933f)&0xffffff,2),
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

    public void tick(){
        items.removeIf(item->item.isRemoved()||item.level()!=game.maps().level());
        var level=game.maps().level();
        if(level==null||!game.playing()||game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(level)
                ||game.items().timeStop().stopped(level)||game.items().wands().timeStopped())return;
        phase=(phase+1)%20;if(phase!=0)return;
        for(var mage:List.copyOf(items)){
            if(!eligible(mage))continue;
            int part=part(mage.getItem(),"mage");
            if(part<0||LbcFartus.data(mage.getItem()).getIntOr("craftusable",0)!=1)continue;
            for(var phantom:level.getEntitiesOfClass(ItemEntity.class,mage.getBoundingBox().inflate(1),other->
                    other!=mage&&eligible(other)&&other.position().distanceToSqr(mage.position())>=.000001
                            &&other.position().distanceToSqr(mage.position())<=1&&part(other.getItem(),"phantom")==part)){
                Vec3 at=mage.position();
                LivingEntity owner=mage.getOwner() instanceof LivingEntity living?living:null;
                var rolls=game.lbc().rollTable(level,at,owner,table(part));
                if(rolls.size()!=1||rolls.getFirst().isEmpty()||rolls.getFirst().getCount()!=1)break;
                var output=new ItemEntity(level,at.x,at.y+.1,at.z,rolls.getFirst().copy());
                output.setDefaultPickUpDelay();if(owner!=null)output.setThrower(owner);
                game.items().markCurrentItemEpoch(output);
                // Publish first: rejected insertion cannot debit either input.
                if(!level.addFreshEntity(output))break;
                consumeOne(mage);consumeOne(phantom);crafted(level,at);break;
            }
        }
    }
}
