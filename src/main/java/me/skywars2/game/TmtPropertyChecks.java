package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/** Generation-only native checks. Does not enable tokens, acquisition, ability dispatch or TMT pools. */
public final class TmtPropertyChecks {
    private TmtPropertyChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT properties: "+why);}
    private static final net.minecraft.world.item.Item[] BASES={Items.CARROT_ON_A_STICK,Items.BOW,
            Items.SNOWBALL,Items.CROSSBOW,Items.GOLDEN_APPLE};

    public static void run(GameController game){
        var level=game.maps().level();var generator=new LbcTmtProperties(level);var domains=generator.domains();
        require(domains.attributes().size()==level.registryAccess().lookupOrThrow(Registries.ATTRIBUTE).size(),"every current attribute");
        require(domains.enchantments().size()==level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).size(),"every current enchantment");
        require(new HashSet<>(domains.sounds()).size()==domains.sounds().size()&&domains.sounds().containsAll(BuiltInRegistries.SOUND_EVENT.keySet()),"one slot per current/source sound");
        require(new HashSet<>(domains.names()).size()==domains.names().size()&&domains.names().size()>11682,"deduplicated legacy and current text keys");
        require(domains.animations().size()==net.minecraft.world.item.ItemUseAnimation.values().length,"all current use animations");
        require(domains.slots().size()==net.minecraft.world.entity.EquipmentSlotGroup.values().length,"all current equipment slot groups");
        for(int index=0;index<domains.attributes().size();index++){
            var random=new Exact(new int[]{Integer.MAX_VALUE,domains.attributes().size(),151,domains.slots().size(),3},
                    new int[]{0,index,50,0,0},new float[]{.9F,.9F,.9F});
            var entry=generator.attribute(random);random.done();
            require(entry.attribute().equals(domains.attributes().get(index)),"each attribute index reachable exactly once");
        }
        for(int index=0;index<domains.enchantments().size();index++){
            var random=new Exact(new int[]{domains.enchantments().size(),5},new int[]{index,4},new float[]{.9F,.9F});
            var roll=generator.enchantment(random);random.done();
            require(roll.enchantment().equals(domains.enchantments().get(index))&&roll.level()==5,"each enchantment index reachable exactly once");
        }
        var lastEnchant=new Exact(new int[]{domains.enchantments().size(),5,255,15},new int[]{0,4,254,14},new float[]{0,0});
        require(generator.enchantment(lastEnchant).level()==15,"later enchantment overwrite wins over255");lastEnchant.done();
        amount(new int[]{151},new int[]{0},new float[]{.9F,.9F,.9F},-.5);
        amount(new int[]{151,2000000001},new int[]{0,2000000000},new float[]{0,.9F,.9F},10000000);
        amount(new int[]{151,2000000001,1401,501},new int[]{0,0,1400,500},new float[]{0,0,0},4);
        amount(new int[]{151,1401},new int[]{150,0},new float[]{.9F,0,.9F},-2);

        var writes=new ArrayList<CompoundTag>();
        for(int i=0;i<7;i++){var data=new CompoundTag();data.putInt("property_sequence",i);data.putInt("property_"+i,1);writes.add(data);}
        var prototype=new ItemStack(Items.BOW,64);prototype.set(DataComponents.CUSTOM_NAME,Component.literal("old name"));
        var nativeData=new CompoundTag();nativeData.putInt("native_default",9);prototype.set(DataComponents.CUSTOM_DATA,CustomData.of(nativeData));
        var prototypeBefore=prototype.copy();var allMin=new Endpoints(false,1);
        var full=generator.generate(allMin,base->prototype,writes);
        require(ItemStack.matches(prototype,prototypeBefore),"caller prototype never mutated");
        require(full.getCount()==1&&!full.has(DataComponents.CUSTOM_NAME),"one output and item-name rather than custom-name override");
        var pdc=full.get(DataComponents.CUSTOM_DATA).copyTag();
        require(pdc.getIntOr("native_default",0)==9&&pdc.getIntOr("tmt_item",0)==1&&pdc.getIntOr("property_sequence",-1)==6,"base flags, native metadata and ordered duplicate writes");
        for(int i=0;i<7;i++)require(pdc.getIntOr("property_"+i,0)==1,"all seven behavior writes retained");
        writes.getFirst().putInt("property_0",99);
        require(full.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("property_0",0)==1,"later mutation cannot alter generated data");
        require(full.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size()==29,"all source attribute opportunities succeed");
        require(allMin.enchantmentWide==21&&allMin.enchantmentLast==21,"all 21 source enchantment opportunities independently execute both overwrites");
        require(full.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().allMatch(e->e.modifier().amount()==-1),"last successful attribute overwrite wins");
        require(full.get(DataComponents.ENCHANTMENTS).entrySet().size()==1&&full.get(DataComponents.ENCHANTMENTS).entrySet().iterator().next().getIntValue()==1,"duplicate enchantment keys overwrite");
        var name=full.get(DataComponents.ITEM_NAME);
        require(!(name.getContents() instanceof TranslatableContents)&&name.getStyle().isBold()&&name.getStyle().isItalic()&&name.getStyle().isObfuscated()&&name.getStyle().isUnderlined()&&name.getStyle().isStrikethrough(),"source literal/style branch");
        require(full.get(DataComponents.LORE).lines().size()==6,"six independent lore components");

        var ops=level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for(int base=0;base<5;base++)for(boolean high:new boolean[]{false,true}){
            var result=generator.generate(new Endpoints(high,base),i->new ItemStack(BASES[i]),writes);
            require(result.is(BASES[base]),"all five carriers reachable without extra engine/registration");
            var encoded=ItemStack.CODEC.encodeStart(ops,result).getOrThrow();
            var restored=ItemStack.CODEC.parse(ops,encoded).getOrThrow();
            require(ItemStack.matches(result,restored),"native component/NBT round trip retains seed, names, lore, gear, modifiers and enchantments");
            require(result.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr("tmt_texture_seed",-1)==(high?Integer.MAX_VALUE-1:0),"persisted visual seed full domain");
            var attrs=result.get(DataComponents.ATTRIBUTE_MODIFIERS);
            require(attrs.modifiers().size()==(high?1:29),"guaranteed attribute with independent optional tails");
            require(result.get(DataComponents.ITEM_MODEL).toString().equals(high?"lbc:tmt/tmt_12758":"lbc:tmt/tmt_1"),"authored visual endpoints remain intact");
            var itemName=result.get(DataComponents.ITEM_NAME);
            require((itemName.getContents() instanceof TranslatableContents)==high,"literal/translate branches and both text endpoints");
            require(itemName.getStyle().getColor().getValue()==(high?0xFFFFFF:0),"six independent color digits");
            if(base==4){
                var food=result.get(DataComponents.FOOD);var use=result.get(DataComponents.CONSUMABLE);
                require(food.nutrition()==20&&food.saturation()==20&&food.canAlwaysEat()&&use.consumeSeconds()==2&&use.hasConsumeParticles()&&use.onConsumeEffects().isEmpty(),"source apple replaces consumable, retains 20/20 always-edible two-second food");
            }
        }
        for(int i=0;i<64;i++){
            long seed=7L+31L*i;
            var a=generator.generate(net.minecraft.util.RandomSource.create(seed),n->new ItemStack(BASES[n]),writes);
            var b=generator.generate(net.minecraft.util.RandomSource.create(seed),n->new ItemStack(BASES[n]),writes);
            require(ItemStack.matches(a,b),"explicit RNG is reproducible without world-global scratch state");
            require(a.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().size()>=1,"at least one attribute");
            require(a.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines().size()==6,"persistent six-line name/lore metadata");
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-properties (native component codecs, non-ability source property families, deterministic branches; attributes={} enchantments={} names={} sounds={} animations={} slots={})",domains.attributes().size(),domains.enchantments().size(),domains.names().size(),domains.sounds().size(),domains.animations().size(),domains.slots().size());
    }

    private static void amount(int[] bounds,int[] values,float[] floats,double expected){
        var random=new Exact(bounds,values,floats);require(Math.abs(LbcTmtProperties.attributeAmount(random)-expected)<1e-8,"independent source amount branch/extreme");random.done();
    }
    private static final class Exact extends LegacyRandomSource{
        final int[] bounds,values;final float[] floats;int integer,floating;
        Exact(int[] bounds,int[] values,float[] floats){super(0);this.bounds=bounds;this.values=values;this.floats=floats;}
        @Override public int nextInt(int bound){require(integer<bounds.length&&bounds[integer]==bound,"expected uniform domain bound");return values[integer++];}
        @Override public float nextFloat(){require(floating<floats.length,"expected independent probability draw");return floats[floating++];}
        void done(){require(integer==bounds.length&&floating==floats.length,"all expected independent draws consumed");}
    }
    private static final class Endpoints extends LegacyRandomSource{
        final boolean high;final int base;int count,enchantmentWide,enchantmentLast;
        Endpoints(boolean high,int base){super(0);this.high=high;this.base=base;}
        @Override public int nextInt(int bound){if(count++==0){require(bound==5,"five source carriers");return base;}if(bound==255)enchantmentWide++;if(bound==15)enchantmentLast++;return high?bound-1:0;}
        @Override public float nextFloat(){return high?.999F:0;}
    }
}
