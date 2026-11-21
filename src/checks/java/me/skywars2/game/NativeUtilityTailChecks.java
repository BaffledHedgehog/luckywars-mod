package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Independent vanilla-source oracles and actual armor use for the ready catalog's final carriers. */
public final class NativeUtilityTailChecks {
    private NativeUtilityTailChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Native utility tail: "+why);}
    public static void run(GameController game){try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner,LbcUtilityItems.items())){
        for(var kind:LbcUtilityItems.Kind.values()){
            var generated=new HashSet<ItemAttributeModifiers>();
            for(long seed:kind==LbcUtilityItems.Kind.MALEVICH?new long[]{17,29,41}:new long[]{17}){
                var stack=NativeMeleeChecks.sourceOutcome(game,f,kind.item,kind.carrier,kind.definition,seed);
                var fixed=stack.copyWithCount(1);var defaults=kind.item.getDefaultInstance();
                if(kind==LbcUtilityItems.Kind.MALEVICH){
                    var attrs=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);require(attrs!=null&&attrs.modifiers().size()==1,"one real generated Malevich attribute");
                    double amount=attrs.modifiers().getFirst().modifier().amount();require(amount>=1&&amount<=100,"authored random attribute range");generated.add(attrs);
                    require(defaults.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().isEmpty(),"bootstrap never freezes a per-loot random attribute");if(defaults.has(DataComponents.ATTRIBUTE_MODIFIERS))fixed.set(DataComponents.ATTRIBUTE_MODIFIERS,defaults.get(DataComponents.ATTRIBUTE_MODIFIERS));else fixed.remove(DataComponents.ATTRIBUTE_MODIFIERS);
                    require(!stack.get(DataComponents.TOOLTIP_DISPLAY).shows(DataComponents.ATTRIBUTE_MODIFIERS)&&LbcMalevich.matches(defaults),"source hidden attribute and functional default PDC");
                }
                require(ItemStack.matches(fixed,defaults),"every fixed default component equals independent source "+kind.id);
                for(var tag:kind.carrier.builtInRegistryHolder().tags().toList())require(stack.is(tag),"complete carrier tag graph "+kind.id+" "+tag);
            }
            if(kind==LbcUtilityItems.Kind.MALEVICH)require(generated.size()>1,"canonical rolls vary instead of sharing bootstrap RNG");
            else require(ItemStack.matches(f.leaf(kind.id),kind.item.getDefaultInstance()),"ready alias keeps every source component "+kind.id);
        }
        f.prepareParticipants();
        for(var kind:List.of(LbcUtilityItems.Kind.BUCKET_HEAD,LbcUtilityItems.Kind.MOISEI_BOOTS)){
            var slot=kind==LbcUtilityItems.Kind.BUCKET_HEAD?EquipmentSlot.HEAD:EquipmentSlot.FEET;
            f.owner.setItemSlot(slot,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.MAIN_HAND,kind.item.getDefaultInstance());
            require(kind.item.use(f.level,f.owner,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.getItemBySlot(slot).is(kind.item)&&f.owner.getMainHandItem().isEmpty(),"real native armor equip input "+kind.id);
        }
    }SkyWars2.LOGGER.info("[check] PASS native-utility-tail (3 full vanilla-source/default/tag/Polymer/server-codec oracles; fresh Malevich RNG and hidden attributes, real armor equip inputs)");}
}
