package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import java.util.*;

/** Persistent authored spell slots live on the native stack; payloads belong to the library owners. */
public final class LbcUniwand {
    public static final Item ITEM=item();
    private final GameController game;
    private final Set<ItemEntity> resident=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcUniwand(GameController game){this.game=game;}
    public static void register(){}
    private static Component text(String key,int color){return Component.translatable(key).withStyle(s->s.withColor(color).withItalic(false));}
    private static Item item(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:uniwand"));
        var model=Identifier.parse("lbc:uniwand");var data=new CompoundTag();
        for(var flag:List.of("transformable","staff","magicwand","uniwand","spellwithcooldown"))data.putInt(flag,1);
        for(var flag:List.of("mana","spellcount","value"))data.putInt(flag,0);
        var lore=new ArrayList<Component>();
        lore.add(text("type",0x55ffff).copy().append(text("staff",0x55ffff)));
        for(var row:List.of("uniwand.lore.1","uniwand.lore.2","uniwand.lore.3"))lore.add(text(row,0xffffff));
        lore.add(manaLore(0));
        var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).durability(25)
                .overrideDescription("uniwand").component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_q"))
                .component(DataComponents.CUSTOM_NAME,text("uniwand",0xffaa00))
                .component(DataComponents.LORE,new ItemLore(lore)).component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("uniwand",0)==1;}
    private static Component manaLore(int mana){return text("mana_usage",0x55ffff).copy().append(Component.literal(Integer.toString(mana)).withStyle(s->s.withColor(0x55ffff).withItalic(false)));}
    public static boolean use(GameController game,ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!matches(stack)||!LbcDimensionItems.ordinary(p)||!LbcLibrarySpells.admitted(game,p,hand)
                ||game.isObserver(p)||!game.playing()||game.items().timeStop().stopped(p.level())
                ||!game.server().tickRateManager().runsNormally())return false;
        var data=LbcFartus.data(stack);int cost=data.getIntOr("mana",0),count=data.getIntOr("spellcount",0);
        if(cost<0||count<0)return false;
        var resource=game.items().mana();
        boolean exceedsMaximum=resource.get(p)<cost&&resource.maximum(p)<cost;
        // User permits exact equality. Source's documented maximum-cost overdraft stays deliberate.
        if(exceedsMaximum){
            // This source spell explicitly permits repeated casts into debt, unlike ordinary wands.
            if(!resource.visible(p)||game.items().magic().suppressed(p)||p.entityTags().contains("time_stopped"))return false;
            resource.set(p,(int)Math.max(Integer.MIN_VALUE,(long)resource.get(p)-cost));
        }else if(!resource.trySpendWand(p,cost,cost))return false;
        if(exceedsMaximum)resource.award(p,"lbc:true_advancements/legends/uniwand");
        for(int slot=1;slot<=count;slot++){
            int id=data.getIntOr("spell"+slot,0);
            if(id>=1&&id<=LbcExtraGrimoires.LAST&&!LbcExtraGrimoires.retired(id))LbcLibrarySpells.cast(game,p,hand,id);
        }
        p.awardStat(Stats.ITEM_USED.get(stack.getItem()));return true;
    }
    public boolean loaded(Entity entity){
        if(entity instanceof ItemEntity item&&(matches(item.getItem())||LbcFartus.data(item.getItem()).getIntOr("libspell",0)==1)){
            if(!game.items().hasStaleItemEpoch(item)){game.items().markCurrentItemEpoch(item);resident.add(item);}return false;
        }return false;
    }
    public void unloaded(Entity entity){resident.remove(entity);}
    public void stop(){resident.clear();}
    private boolean ready(ItemEntity entity){return !entity.isRemoved()&&!entity.getItem().isEmpty()&&entity.getAge()!=-32768
            &&entity.level()==game.maps().level()&&entity.level().hasChunkAt(entity.blockPosition())
            &&game.items().currentEpoch(entity)&&LbcDimensionItems.ordinary(entity.level());}
    public void tick(){
        resident.removeIf(Entity::isRemoved);
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||game.server().getTickCount()%20!=0
                ||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(game.maps().level()))return;
        for(var wand:List.copyOf(resident))if(ready(wand)&&matches(wand.getItem()))compose(wand);
    }
    /** One source-second absorbs the finite nearby stacks, nearest first and one authored slot per unit. */
    private void compose(ItemEntity wand){
        var books=new ArrayList<ItemEntity>();
        for(var other:resident)if(other!=wand&&ready(other)&&other.level()==wand.level()
                &&other.position().distanceToSqr(wand.position())<=1&&LbcFartus.data(other.getItem()).getIntOr("libspell",0)==1)books.add(other);
        if(books.stream().noneMatch(b->b.position().distanceToSqr(wand.position())>=.000001))return;
        books.sort(Comparator.comparingDouble(b->b.position().distanceToSqr(wand.position())));
        var stack=wand.getItem().copy();var data=LbcFartus.data(stack);
        int slots=data.getIntOr("spellcount",0),mana=data.getIntOr("mana",0),value=data.getIntOr("value",0);
        if(slots<0||mana<0)return;
        for(var book:books){
            while(!book.getItem().isEmpty()){
                var spell=LbcFartus.data(book.getItem());int next=slots+1;if(next<=0)return;
                int contribution=spell.getIntOr("mana",0)/Math.max(1,next/5);
                slots=next;mana+=contribution;value+=tierValue(book.getItem());
                data.putInt("spell"+slots,spell.getIntOr("spellid",0));data.putInt("spellcount",slots);data.putInt("mana",mana);data.putInt("value",value);
                stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                stack.set(DataComponents.TOOLTIP_STYLE,Identifier.parse(tier(value)));
                var lore=new ArrayList<>(stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines());
                if(lore.size()>4){lore.set(4,manaLore(mana));stack.set(DataComponents.LORE,new ItemLore(lore));}
                wand.setItem(stack.copy());var remaining=book.getItem().copy();remaining.shrink(1);
                book.setItem(remaining);if(remaining.isEmpty())book.discard();
                // Recursive source continuation requires at least one noncoincident remaining book.
                if(books.stream().noneMatch(b->!b.isRemoved()&&!b.getItem().isEmpty()&&b.position().distanceToSqr(wand.position())>=.000001))return;
            }
        }
    }
    private static int tierValue(ItemStack stack){
        var id=stack.get(DataComponents.TOOLTIP_STYLE);if(id==null)return 0;
        return switch(id.getPath()){
            case "tier_ff"->-99;case "tier_f"->-9;case "tier_d"->1;case "tier_c"->10;
            case "tier_b"->100;case "tier_a"->1000;case "tier_s"->10000;case "tier_ss"->100000;case "tier_x"->1000000;default->0;
        };
    }
    private static String tier(int value){
        if(value<=-11)return "lbc:tier_ff";if(value<0)return "lbc:tier_f";if(value<10)return "lbc:tier_d";
        if(value<100)return "lbc:tier_c";if(value<1000)return "lbc:tier_b";if(value<10000)return "lbc:tier_a";
        if(value<100000)return "lbc:tier_s";if(value<1000000)return "lbc:tier_ss";return "lbc:tier_x";
    }
}
