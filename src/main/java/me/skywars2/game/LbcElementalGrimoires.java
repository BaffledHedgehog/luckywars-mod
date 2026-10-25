package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/** Direct consumed library160/163/165/167/171/173/176/181/184, sharing ordinary wand payloads. */
public final class LbcElementalGrimoires {
    public enum Kind {
        RUBY(160,"ruby","swarogia",1200,"c","ruby",1,2,3),
        TERRA(163,"terra","terra",1200,"c","terra",2),
        ELECTRO(165,"electro","sosuenerugi",900,"c","electro",2,3),
        AIR(167,"air","explode",1000,"c","air",2),
        ACID(171,"acid","lahazor",200,"c","acid",2,3),
        TELEPORT(173,"teleport","arkontas",2000,"b","teleport",2),
        STONE(176,"stone","stone",2000,"c","stone",2),
        FIRE(181,"fire","fire",200,"c","fire",2),
        BLIND(184,"blind","blind",300,"d","blind",2);

        final int spellId,metadataMana;
        final String id,model,name,tier;
        final List<String> lore;
        final Item item;
        Kind(int spellId,String id,String model,int metadataMana,String tier,String loreStem,int... rows) {
            this.spellId=spellId;this.id=id+"_grimoire";this.model="lbc:spelllib/"+model;
            this.name="libspell_"+model;this.metadataMana=metadataMana;this.tier="lbc:tier_"+tier;
            this.lore=java.util.Arrays.stream(rows).mapToObj(row->"sauvojen_wand_"+loreStem+".lore."+row).toList();
            this.item=create(this);
        }
    }

    private LbcElementalGrimoires() { }
    public static void register() { Kind.values(); }

    private static Component text(String key,ChatFormatting color) {
        return Component.translatable(key).withStyle(color).withStyle(style->style.withItalic(false));
    }
    private static Item create(Kind kind) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+kind.id));
        var model=Identifier.parse(kind.model);
        var tag=new CompoundTag();
        for(var flag:List.of("transformable","gadget","libspell","unresolved_grimoire"))tag.putInt(flag,1);
        tag.putInt("mana",kind.metadataMana);tag.putInt("spellid",kind.spellId);
        var lore=new ArrayList<Component>();
        lore.add(text("type",ChatFormatting.AQUA).copy().append(text("libspell",ChatFormatting.AQUA)));
        lore.add(Component.empty());
        for(var row:kind.lore)lore.add(text(row,ChatFormatting.GOLD));
        lore.add(text("rarity",ChatFormatting.WHITE).copy().append(text("epic",ChatFormatting.DARK_PURPLE)));
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).stacksTo(99).overrideDescription(kind.name)
                .component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(kind.tier))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(tag))
                .component(DataComponents.CUSTOM_NAME,text(kind.name,ChatFormatting.WHITE))
                .component(DataComponents.LORE,new ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.SNOWBALL,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }

    static Kind kind(ItemStack stack) {
        if(stack.isEmpty())return null;
        for(var kind:Kind.values())if(stack.is(kind.item))return kind;
        var data=LbcFartus.data(stack);
        if(data.getIntOr("libspell",0)!=1)return null;
        int id=data.getIntOr("spellid",0);
        for(var kind:Kind.values())if(id==kind.spellId)return kind;
        return null;
    }

    /** Metadata mana belongs to library crafting; direct consumption has no mana price/admission. */
    static boolean use(GameController game,ServerPlayer player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);var kind=kind(stack);
        if(kind==null||player.isRemoved()||!player.isAlive()||game.stage()==GameController.Stage.ENDING
                ||!game.canUseGameplayItems(player)||player.level()!=game.maps().level()
                ||!player.level().hasChunkAt(BlockPos.containing(player.getEyePosition()))
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||game.items().actionsBlocked(player)||game.items().wands().inputTimeStopped(player)
                ||game.items().magic().suppressed(player)
                ||game.items().magic().suppressedAt(player.level(),player.getEyePosition()))return false;
        switch(kind) {
            case RUBY -> game.items().ruby().cast(player,0);
            case TERRA -> game.items().terra().cast(player);
            case ELECTRO -> game.items().wands().electroVolley(player);
            case AIR -> game.items().air().cast(player,hand,0,ItemStack.EMPTY);
            case ACID -> {
                game.items().clouds().ordinaryWandAcid(player);
                LbcOverload.sound(player,player.getEyePosition(),"minecraft:entity.zombie_villager.cure",1,2,0);
            }
            case TELEPORT -> game.items().teleport().cast(player,hand,0,ItemStack.EMPTY);
            case STONE -> game.items().stone().cast(player,0);
            case FIRE -> game.items().wands().fireVolley(player);
            case BLIND -> game.items().blind().cast(player,hand,0,ItemStack.EMPTY);
        }
        player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL,.5f,.4f/(player.level().getRandom().nextFloat()*.4f+.8f));
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        stack.consume(1,player);
        return true;
    }
}
