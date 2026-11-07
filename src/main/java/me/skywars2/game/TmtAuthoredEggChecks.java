package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real canonical vanilla ENTITY_DATA and existing native egg compose through the same use-on factory. */
public final class TmtAuthoredEggChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT authored eggs: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcTmtItems.prototype(0).getItem()))){
            f.prepareParticipants();var familyIds=new HashSet<String>();LbcTmtAbilities.Ability illusioner=null,vex=null;
            for(var family:game.items().tmt().useProfiles().families())for(var property:family.variants()){
                var item=LbcTmtComponents.nativeItem(property.data());
                if(item==LbcSpecialEggs.LOW_VEX&&vex==null)vex=property;
                if(!(item instanceof SpawnEggItem)||!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))continue;
                var source=LbcTmtComponents.source(game,property);require(LbcTmtBlockUses.authoredEgg(source,item),"only genuinely authored vanilla egg payloads admitted");
                if(source.get(DataComponents.ENTITY_DATA).type()==EntityTypes.ILLUSIONER){familyIds.add(family.id());if(illusioner==null)illusioner=property;}
            }
            require(illusioner!=null&&vex!=null&&familyIds.size()==1,"Illusioner aliases share one semantic family; existing native Vex retained; illusioner="+(illusioner!=null)+" vex="+(vex!=null)+" families="+familyIds);
            for(var item:List.of(Items.BAT_SPAWN_EGG,Items.ZOMBIE_SPAWN_EGG,Items.CREEPER_SPAWN_EGG)){
                var bare=new ItemStack(item);require(!LbcTmtBlockUses.airCandidate(bare)&&LbcTmtBlockUses.canonical(bare)==null,"bare vanilla egg is not a new gameplay property");
            }
            var first=quiet(game,illusioner);var second=quiet(game,vex);var stack=LbcTmtItems.prototype(0).copyWithCount(3);
            var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(root,List.of(first,second));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            stack.set(DataComponents.ENTITY_DATA,TypedEntityData.of(EntityTypes.COW,new net.minecraft.nbt.CompoundTag()));var expected=stack.copyWithCount(1);
            var ops=f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);stack=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,stack).getOrThrow()).getOrThrow();
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);var other=new ItemStack(Items.STICK,7);f.owner.setItemInHand(InteractionHand.OFF_HAND,other);
            var at=BlockPos.containing(f.home.add(0,0,3));for(int y=0;y<4;y++)f.set(at.above(y),y==0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            var prior=new HashSet<UUID>();f.level.getAllEntities().forEach(e->prior.add(e.getUUID()));
            var hit=new BlockHitResult(Vec3.atBottomCenterOf(at.above()),Direction.UP,at,false);
            require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"actual native private use-on");
            var fresh=new ArrayList<Mob>();for(var e:f.level.getAllEntities())if(!prior.contains(e.getUUID())){f.own(e);if(e instanceof Mob mob)fresh.add(mob);}
            require(fresh.size()==2&&fresh.stream().anyMatch(e->e.getType()==EntityTypes.ILLUSIONER)&&fresh.stream().anyMatch(e->e.getType()==EntityTypes.VEX),"real vanilla Illusioner and native Vex, no outer Cow payload leakage");
            require(fresh.stream().allMatch(Mob::isNoAi),"private authored entity data survives native loading");
            require(stack.getCount()==1&&ItemStack.matches(stack,expected)&&f.owner.getOffhandItem()==other&&other.getCount()==7,"each accepted native factory debits once; full outer/sibling state and other hand retained");
            require(!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"native projection closes after both factories");
        }
    }
    private static LbcTmtAbilities.Ability quiet(GameController game,LbcTmtAbilities.Ability property){
        var source=LbcTmtComponents.source(game,property);var value=source.get(DataComponents.ENTITY_DATA);var tag=value.copyTagWithoutId();tag.putBoolean("NoAI",true);
        source.set(DataComponents.ENTITY_DATA,TypedEntityData.of(value.type(),tag));var data=property.data().copyTag();LbcTmtComponents.capture(game,source,data);return new LbcTmtAbilities.Ability(property.id(),CustomData.of(data));
    }
}
