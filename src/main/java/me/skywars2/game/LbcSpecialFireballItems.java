package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Canonical throwable identities delegate their final native payload to the existing six-way factory. */
public final class LbcSpecialFireballItems {
    record Entry(String id,String definition,Item item){}
    private static final List<Entry> ENTRIES=List.of(
            entry("throwable_small_fireball","smallfireball__5403050a24bc"),
            entry("throwable_large_fireball","bigfireball__58955493346d"),
            entry("throwable_dragon_fireball","dragfireball__9640597422c4"),
            entry("imba_throwable_small_fireball","smallfireball__dff5ed11dc93"),
            entry("imba_throwable_large_fireball","bigfireball__0f6adc3f0b43"),
            entry("imba_throwable_dragon_fireball","dragfireball__409f7dfdb4f7"));
    private LbcSpecialFireballItems(){}
    public static void register(){for(var entry:ENTRIES)net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(entry.item());}
    static List<Entry> entries(){return ENTRIES;}
    private static Entry entry(String id,String definition){
        var resource="data/lbc/loot_table/item_defs/lbc/"+definition+".json";
        var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,"/"+resource,Items.SNOWBALL,16);
        var item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),new NativeSnowball(properties));
        return new Entry(id,resource,item);
    }
    private static final class NativeSnowball extends SnowballItem implements PolymerItem {
        NativeSnowball(Properties properties){super(properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.SNOWBALL;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var game=me.skywars2.SkyWars2.game();
            return game!=null&&player instanceof ServerPlayer owner&&game.items().specialFireballs().use(owner,hand)
                    ?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
    }
}
