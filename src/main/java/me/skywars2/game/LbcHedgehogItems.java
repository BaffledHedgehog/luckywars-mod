package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Native identities; authored names/lore remain supplied by the narrow source loot leaves. */
public final class LbcHedgehogItems {
    public static final Item WAND=wand(false), ACADEMY=wand(true);
    public static final Item GOD=create("god","god","god","x",flags("god","magicwand"),Items.CARROT_ON_A_STICK,1,false);
    public enum SpikeKind {
        CONFUSE("ender_spike","spike_ender","x","spike_confuse"),
        BLIND("blind_spike","spike_blind","a","spike_blind"),
        BOOM("explosive_spike","spike_explosive","x","spike_boom"),
        STANDARD("spike","spike","a","spike_standart"),
        WITHER("wither_spike","spike_wither","a","spike_wither"),
        POISON("poison_spike","spike_poison","b","spike_poison"),
        IMPULSE("impulse_spike","spike_impulse","s","spike_impulse");
        private final Item item;
        private final String flag;
        SpikeKind(String id,String model,String tier,String flag){
            this.flag=flag;
            item=create(id,model,id,tier,flags("special",flag),Items.SNOWBALL,16,true);
        }
        public Item item(){return item;}
    }
    private LbcHedgehogItems(){}
    public static void register(){SpikeKind.values();}
    private static CompoundTag flags(String... flags){
        var data=new CompoundTag();for(var flag:flags)data.putInt(flag,1);return data;
    }
    private static Item wand(boolean academy){
        var data=flags("transformable","staff","raycastspell","baffledhedgehog");
        if(academy){data.putInt("upgradable",0);data.putInt("upgrade_available",1);data.putInt("upgraded",1);data.putInt("mode",1);}
        return create(academy?"academy_hedgehog_wand":"hedgehog_wand","hedgehogwand","hedgehog_wand",
                academy?"s":"b",data,Items.CARROT_ON_A_STICK,1,true);
    }
    private static Item create(String id,String modelName,String name,String tier,CompoundTag data,Item donor,int count,boolean usable){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        var model=Identifier.parse("lbc:"+modelName);
        var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(count)
                .overrideDescription(name).component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_"+tier));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,donor,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                if(!usable)return InteractionResult.PASS;
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer owner&&game.items().wands().use(owner,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isWand(ItemStack stack){
        return !stack.isEmpty()&&(stack.is(WAND)||stack.is(ACADEMY)||LbcFartus.data(stack).getIntOr("baffledhedgehog",0)==1);
    }
    public static boolean isAcademy(ItemStack stack){
        return isWand(stack)&&(stack.is(ACADEMY)||LbcFartus.data(stack).getIntOr("upgraded",0)==1);
    }
    public static SpikeKind spike(ItemStack stack){
        if(stack.isEmpty())return null;
        var data=LbcFartus.data(stack);
        for(var kind:SpikeKind.values())if(stack.is(kind.item)||data.getIntOr(kind.flag,0)==1)return kind;
        return null;
    }
}
