package me.skywars2.game;

import java.util.*;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;

/** Real vanilla equipment/mining/damage contracts, independent of legacy selector tags. */
public final class TmtEquipmentChecks {
    private TmtEquipmentChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT native components: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var catalog=game.items().tmt().catalog();var properties=new ArrayList<LbcTmtAbilities.Ability>();
            var ops=game.server().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            for(var property:LbcTmtEquipment.PROPERTIES){
                var family=catalog.families().stream().filter(row->row.id().equals(property.id())).findFirst().orElseThrow();
                var values=new HashSet<net.minecraft.nbt.Tag>();
                for(var variant:family.variants())require(values.add(variant.data().copyTag().get(LbcTmtEquipment.KEY)),"semantic component values deduplicate source aliases: "+property.name());
                for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);var source=new ItemStack(item);
                    if(id.getNamespace().equals("skywars2")&&!LbcTmtItems.output(item)&&property.present(source))
                        require(values.contains(LbcTmtEquipment.capture(game,source,property).get(LbcTmtEquipment.KEY)),"every current registered component value is represented: "+id+"/"+property.name());
                }
                properties.add(family.variants().getFirst());
            }
            var references=new HashMap<String,ItemStack>();
            for(int i=0;i<properties.size();i++){
                var ability=properties.get(i);var reference=new ItemStack(Items.STICK);
                reference.applyComponents(DataComponentPatch.CODEC.parse(ops,ability.data().copyTag().get(LbcTmtEquipment.KEY)).getOrThrow());
                references.put(LbcTmtEquipment.PROPERTIES.get(i).name(),reference);
            }
            int actual=0;
            for(int carrier=0;carrier<5;carrier++)for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                var stack=LbcTmtItems.prototype(carrier);var header=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();LbcTmtAbilities.write(header,properties);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(header));
                var armor=slot==EquipmentSlot.HEAD?Items.LEATHER_HELMET:slot==EquipmentSlot.CHEST?Items.LEATHER_CHESTPLATE:slot==EquipmentSlot.LEGS?Items.LEATHER_LEGGINGS:Items.LEATHER_BOOTS;
                stack.set(DataComponents.EQUIPPABLE,new ItemStack(armor).get(DataComponents.EQUIPPABLE));stack.set(DataComponents.CUSTOM_NAME,Component.literal("physical native component carrier"));
                var before=stack.copy();LbcTmtEquipment.apply(game,stack);
                require(LivingEntity.canGlideUsing(stack,slot),"native GLIDER operates on every generated equipment slot and all five carriers");
                require(!LivingEntity.canGlideUsing(stack,slot==EquipmentSlot.HEAD?EquipmentSlot.CHEST:EquipmentSlot.HEAD),"native wrong-slot restriction is retained");
                for(var state:List.of(Blocks.STONE.defaultBlockState(),Blocks.OAK_LOG.defaultBlockState(),Blocks.IRON_ORE.defaultBlockState(),Blocks.GLASS.defaultBlockState())){
                    var reference=references.get("tool");require(stack.getDestroySpeed(state)==reference.getDestroySpeed(state)&&stack.isCorrectToolForDrops(state)==reference.isCorrectToolForDrops(state),"native TOOL mining speed and correct-drop rules survive source composition");
                }
                var resistant=references.get("damage_resistant");require(stack.canBeHurtBy(f.level.damageSources().lava())==resistant.canBeHurtBy(f.level.damageSources().lava())&&stack.canBeHurtBy(f.level.damageSources().generic())==resistant.canBeHurtBy(f.level.damageSources().generic()),"actual native item damage resistance, never player immunity");
                f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);int damage=stack.getDamageValue();stack.hurtAndBreak(5,f.owner,EquipmentSlot.MAINHAND);require(stack.getDamageValue()==damage&&stack.getCount()==1,"native UNBREAKABLE prevents wear on the physical carrier");
                var roundTrip=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,stack).getOrThrow()).getOrThrow();require(ItemStack.matches(stack,roundTrip),"typed private properties and all native components survive native codec");
                var stripped=stack.copy();for(var property:LbcTmtEquipment.PROPERTIES)stripped.remove(property.type());
                for(var property:LbcTmtEquipment.PROPERTIES)before.remove(property.type());require(ItemStack.matches(stripped,before),"composition preserves exact carrier, equipment asset/slot, food, stats, private sibling data and name");actual++;
            }
            var draws=new ArrayList<>(properties);while(draws.size()<7)draws.add(properties.getFirst());
            var generated=game.items().tmt().generate(RandomSource.create(91),draws);
            require(LivingEntity.canGlideUsing(generated,generated.get(DataComponents.EQUIPPABLE).slot())&&LbcTmtEquipment.PROPERTIES.stream().allMatch(p->generated.has(p.type())),"real generator applies the sampled native component families");
            game.server().sendSystemMessage(Component.literal("[check] PASS tmt-native-components ("+actual+" carrier/slot combinations; every current registered native component value; dedup; native glide/mining/drop rules/item resistance/unbreakable wear; real generator and codec; physical properties preserved)"));
        }
    }
}
