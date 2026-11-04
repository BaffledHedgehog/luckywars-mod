package me.skywars2.game;

import com.google.gson.JsonParser;
import eu.pb4.polymer.core.api.item.PolymerItem;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.Level;

/** Sixteen authored snowballs; vanilla use owns aim, sound, real item-used stat and the sole debit. */
public final class LbcWoolTrailItems {
    private static final Map<DyeColor,Item> TOKENS=create();
    private LbcWoolTrailItems(){}
    public static void register(){LbcBalanceExtras.register();}
    public static Map<DyeColor,Item> tokens(){return TOKENS;}
    public static boolean matches(ItemStack stack){
        return !stack.isEmpty()&&ItemData.intValue(stack,"wool",0)==1&&ItemData.intValue(stack,"projegadget",0)==1;
    }
    /** Raw legacy snowballs pass through the same gate before their normal vanilla use callback. */
    public static boolean allowed(GameController game,ServerPlayer owner,InteractionHand hand){
        return game!=null&&matches(owner.getItemInHand(hand))&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&!game.maps().busy()&&owner.level()==game.maps().level()&&owner.isAlive()&&!owner.isRemoved()
                &&!owner.isSpectator()&&!game.isObserver(owner)&&!game.awaitingRespawn(owner.getUUID())
                &&game.canUseGameplayItems(owner)&&game.items().wands().validRoleTarget(owner,false)
                &&LbcDimensionItems.ordinary(owner)&&LbcBedwarsRoleGuards.publication(owner.level())
                &&!game.items().actionsBlocked(owner)&&!game.items().wands().inputTimeStopped(owner)
                &&!game.items().timeStop().stopped(owner.level())&&!game.items().timeStop().frozen(owner)
                &&!game.server().tickRateManager().isFrozen()&&game.server().tickRateManager().runsNormally()
                &&game.items().balance().woolAllowed(owner,hand);
    }
    private static Map<DyeColor,Item> create(){
        var result=new LinkedHashMap<DyeColor,Item>();
        try(var input=LbcWoolTrailItems.class.getResourceAsStream("/data/lbc/loot_table/imba/wool.json")){
            if(input==null)throw new IllegalStateException("Missing authored wool group");
            var entries=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
            for(var value:entries){
                String reference=value.getAsJsonObject().get("value").getAsString();
                String name=reference.substring(reference.lastIndexOf('/')+1,reference.indexOf("_wool__"));
                var color=DyeColor.valueOf(name.toUpperCase(Locale.ROOT));
                String id="skywars2:wool_trail_"+name;
                String resource="/data/"+reference.replace(':','/').replace("/item_defs/","/loot_table/item_defs/")+".json";
                var item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse(id),
                        new NativeSnowball(LbcUtilityItemPrototypes.properties(id,resource,Items.SNOWBALL,16)));
                if(result.put(color,item)!=null)throw new IllegalStateException("Duplicate authored wool color");
            }
            if(result.size()!=16)throw new IllegalStateException("Expected all sixteen authored wool colors");
            return Collections.unmodifiableMap(result);
        }catch(Exception error){throw new IllegalStateException("Invalid authored wool group",error);}
    }
    private static final class NativeSnowball extends SnowballItem implements PolymerItem {
        NativeSnowball(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.SNOWBALL;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            return player instanceof ServerPlayer owner&&allowed(me.skywars2.SkyWars2.game(),owner,hand)
                    ?super.use(level,player,hand):InteractionResult.FAIL;
        }
    }
}
