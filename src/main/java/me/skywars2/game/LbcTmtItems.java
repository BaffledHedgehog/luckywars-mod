package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Five genuine vanilla transports; native ammunition, use ticks and consumption remain authoritative. */
public final class LbcTmtItems {
    private LbcTmtItems(){}
    public static final Item TOKEN=token();
    private static final List<Item> OUTPUTS=outputs();
    public static void register(){}
    public static boolean output(Item item){return OUTPUTS.contains(item);}
    static ItemStack prototype(int index){return new ItemStack(OUTPUTS.get(index));}
    public static boolean matches(ItemStack stack){return ItemData.intValue(stack,"tmt_item",0)==1;}
    public static boolean active(ItemStack stack){return LbcTmtAbilities.active(stack);}
    private static Item.Properties properties(String name){
        var data=new CompoundTag();data.putInt("tmt_item",1);
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Identifier.parse(name))).component(DataComponents.CUSTOM_DATA,CustomData.of(data));
    }
    private static Item token(){
        var data=new CompoundTag();data.putInt("tmt",1);data.putInt("drop",1);
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:tmt_token"),new SimplePolymerItem(properties("skywars2:tmt_token")
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data)).component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:drop_trap"))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_u"))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("trap_tmt").withStyle(s->s.withColor(0xffff55).withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("trap_desc").withStyle(s->s.withColor(0xffffff).withItalic(false))))),Items.REPEATING_COMMAND_BLOCK,true));
    }
    private static List<Item> outputs(){
        var carrot=new NativeCarrot(properties("skywars2:tmt_carrot_on_a_stick").durability(25));
        var bow=new NativeBow(properties("skywars2:tmt_bow").durability(384).enchantable(1));
        var snow=new NativeSnowball(properties("skywars2:tmt_snowball").stacksTo(16));
        var cross=new NativeCrossbow(properties("skywars2:tmt_crossbow").durability(465).enchantable(1).component(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.EMPTY));
        var apple=new NativeApple(properties("skywars2:tmt_golden_apple").food(net.minecraft.world.food.Foods.GOLDEN_APPLE,Consumables.GOLDEN_APPLE));
        var result=List.<Item>of(carrot,bow,snow,cross,apple);
        var ids=List.of("skywars2:tmt_carrot_on_a_stick","skywars2:tmt_bow","skywars2:tmt_snowball","skywars2:tmt_crossbow","skywars2:tmt_golden_apple");
        for(int i=0;i<result.size();i++)Registry.register(BuiltInRegistries.ITEM,Identifier.parse(ids.get(i)),result.get(i));
        return result;
    }
    private static LbcTmtRuntime runtime(){var game=SkyWars2.game();return game==null?null:game.items().tmt();}
    static Item vanillaCarrier(ItemStack stack){
        var item=stack.getItem();
        if(item instanceof NativeApple)return Items.GOLDEN_APPLE;
        if(item instanceof NativeCarrot)return Items.CARROT_ON_A_STICK;
        if(item instanceof NativeBow)return Items.BOW;
        if(item instanceof NativeCrossbow)return Items.CROSSBOW;
        if(item instanceof NativeSnowball)return Items.SNOWBALL;
        return item;
    }
    private static void using(LivingEntity entity,ItemStack stack){var runtime=runtime();if(runtime!=null&&entity instanceof net.minecraft.server.level.ServerPlayer owner)runtime.usingItem(owner,owner.getUsedItemHand(),stack);}
    private static final class NativeCarrot extends FoodOnAStickItem<Pig> implements PolymerItem {
        NativeCarrot(Properties properties){super(EntityTypes.PIG,7,properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return LbcTmtContinuous.clientCarrier(stack,Items.CARROT_ON_A_STICK);}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){super.onUseTick(level,entity,stack,remaining);using(entity,stack);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var runtime=runtime();if(runtime!=null&&player instanceof net.minecraft.server.level.ServerPlayer owner){
                if(!runtime.admitted(owner))return InteractionResult.FAIL;
                boolean cast=runtime.rightClick(owner,hand);
                if(owner.getItemInHand(hand).isEmpty())return InteractionResult.SUCCESS;
                var sustained=LbcTmtContinuous.use(SkyWars2.game(),owner,hand);if(sustained!=InteractionResult.PASS)return sustained;
                if(cast)return InteractionResult.SUCCESS;
            }
            return super.use(level,player,hand);
        }
    }
    private static final class NativeBow extends BowItem implements PolymerItem {
        NativeBow(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.BOW;}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){super.onUseTick(level,entity,stack,remaining);using(entity,stack);}
    }
    private static final class NativeCrossbow extends CrossbowItem implements PolymerItem {
        NativeCrossbow(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.CROSSBOW;}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){super.onUseTick(level,entity,stack,remaining);using(entity,stack);}
    }
    private static final class NativeSnowball extends SnowballItem implements PolymerItem {
        NativeSnowball(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return LbcTmtContinuous.clientCarrier(stack,Items.SNOWBALL);}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){super.onUseTick(level,entity,stack,remaining);using(entity,stack);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var runtime=runtime();if(runtime!=null&&player instanceof net.minecraft.server.level.ServerPlayer owner){
                if(!runtime.admitted(owner))return InteractionResult.FAIL;
                runtime.rightClick(owner,hand);
                if(player.getItemInHand(hand).isEmpty())return InteractionResult.SUCCESS;
                var sustained=LbcTmtContinuous.use(SkyWars2.game(),owner,hand);if(sustained!=InteractionResult.PASS)return sustained;
            }
            return super.use(level,player,hand);
        }
    }
    private static final class NativeApple extends Item implements PolymerItem {
        NativeApple(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.GOLDEN_APPLE;}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){super.onUseTick(level,entity,stack,remaining);using(entity,stack);}
    }
}
