package me.skywars2.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Authored ordinary summon items and exact world/local formation coordinates. */
final class LbcSummonerItems {
    record Root(boolean local,Vec3 offset) { }
    record Kind(String kind,String id,String flag,String cost,int price,Item item,List<Root> roots) { }
    private static final List<Kind> KINDS=new ArrayList<>();
    static final JsonObject SOURCE;
    static {
        try(var input=LbcSummonerItems.class.getResourceAsStream("/skywars2/ordinary_summoners.json")){
            if(input==null)throw new IllegalStateException("Missing ordinary summoner source");
            SOURCE=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var element:SOURCE.getAsJsonArray("items")){
                var row=element.getAsJsonObject();String id=row.get("id").getAsString(),kind=row.get("kind").getAsString();
                var model=Identifier.parse(row.get("model").getAsString());
                var name=ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,row.get("name")).getOrThrow();
                var lore=new ArrayList<net.minecraft.network.chat.Component>();
                for(var line:row.getAsJsonArray("lore"))lore.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,line).getOrThrow());
                var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
                var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                        .setId(key).durability(25).component(DataComponents.CUSTOM_NAME,name)
                        .component(DataComponents.LORE,new ItemLore(lore)).component(DataComponents.ITEM_MODEL,model)
                        .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(row.get("tooltip").getAsString()))
                        .component(DataComponents.CUSTOM_DATA,CustomData.of(TagParser.parseCompoundFully(row.get("data").getAsString())));
                var item=Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.CARROT_ON_A_STICK,true){
                    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                        var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
                    }
                });
                var roots=new ArrayList<Root>();for(var entry:SOURCE.getAsJsonObject("formations").getAsJsonArray(kind)){
                    var r=entry.getAsJsonObject();var offset=r.getAsJsonArray("offset");
                    roots.add(new Root(r.get("local").getAsBoolean(),new Vec3(offset.get(0).getAsDouble(),offset.get(1).getAsDouble(),offset.get(2).getAsDouble())));
                }
                KINDS.add(new Kind(kind,id,row.get("flag").getAsString(),row.get("cost").getAsString(),row.get("price").getAsInt(),item,List.copyOf(roots)));
            }
            if(KINDS.size()!=7)throw new IllegalStateException("Incomplete ordinary summoners");
        }catch(Exception error){throw new ExceptionInInitializerError(error);}
    }
    private LbcSummonerItems() { }
    static void register(){ }
    static List<Kind> kinds(){return List.copyOf(KINDS);}
    static Kind kind(ItemStack stack){
        if(stack.isEmpty())return null;var data=LbcFartus.data(stack);
        return KINDS.stream().filter(k->stack.is(k.item())||data.getIntOr(k.flag(),0)==1).findFirst().orElse(null);
    }
}
