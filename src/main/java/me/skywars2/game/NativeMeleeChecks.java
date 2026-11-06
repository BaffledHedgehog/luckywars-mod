package me.skywars2.game;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.*;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Compare native item machinery with independently decoded original-carrier source loot. */
public final class NativeMeleeChecks {
    private NativeMeleeChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Native melee: "+why);}
    static void captureStats(ServerPlayer player,Map<Stat<?>,Integer> stats){for(var kind:LbcMeleeItems.Kind.values())for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN,Stats.ITEM_CRAFTED)){var stat=type.get(kind.item);stats.putIfAbsent(stat,player.getStats().getValue(stat));}}
    static final class NativeStats implements AutoCloseable {
        final ServerPlayer player;final Map<Stat<?>,Integer> before=new LinkedHashMap<>();
        NativeStats(ServerPlayer player){this.player=player;captureStats(player,before);}
        public void close(){before.forEach((stat,value)->player.getStats().setValue(player,stat,value));}
    }
    private static JsonObject source(String resource){try(var input=NativeMeleeChecks.class.getResourceAsStream(resource)){
        return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(input,resource),StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private static ItemStack compare(GameController game,WoolBucketChecks.Fixture f,LbcMeleeItems.Kind kind,String resource){
        var raw=source(resource);var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.owner.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var actual=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow().getRandomItems(params,17);require(actual.size()==1&&actual.getFirst().is(kind.item),"actual native source identity "+resource);
        raw.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(kind.carrier).toString());
        var original=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow().getRandomItems(params,17);require(original.size()==1,"independent original source outcome "+resource);
        var stack=actual.getFirst();var expected=original.getFirst();require(stack.getCount()==expected.getCount()&&stack.getComponents().keySet().equals(expected.getComponents().keySet()),"all original component keys/count "+resource);
        for(var c:expected.getComponents())require(Objects.equals(c.value(),stack.get(c.type())),"original carrier machinery and authored component "+c.type()+" "+resource);
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.owner.connection;
        var view=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());
        require(view.is(kind.carrier)&&ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(view,provider.getPacketContext(),f.owner.registryAccess())),"Polymer carrier and authoritative roundtrip "+resource);
        var nbt=f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);var saved=ItemStack.CODEC.encodeStart(nbt,stack).getOrThrow();require(ItemStack.matches(stack,ItemStack.CODEC.parse(nbt,saved).getOrThrow()),"all native components persist "+resource);
        return stack;
    }
    public static void run(GameController game){try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeStats(f.owner)){
        for(var kind:LbcMeleeItems.Kind.values()){
            var stack=compare(game,f,kind,kind.definition);require(ItemStack.matches(stack,kind.item.getDefaultInstance()),"native /give default equals canonical authored source "+kind.id);
            var leaf=f.leaf(kind.id);
            if(kind==LbcMeleeItems.Kind.FRAGMENT_SWORD){var data=LbcFartus.data(leaf).copy();int type=data.getIntOr("cooltype",-1);require(leaf.is(kind.item)&&type>=0&&type<=16,"public Fragment table selects one of the 17 native states");data.putInt("cooltype",0);leaf=leaf.copy();leaf.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));}
            require(ItemStack.matches(stack,leaf),"ready leaf preserves every canonical component beyond its declared random Fragment state "+kind.id);
            if(kind.carrier==Items.SHIELD)require(kind.item instanceof ShieldItem&&stack.has(DataComponents.BLOCKS_ATTACKS),"actual native shield subtype and blocking machinery");
            else if(kind.carrier!=Items.CARROT_ON_A_STICK)require(stack.is(ItemTags.SWORDS)&&stack.has(DataComponents.TOOL)&&stack.has(DataComponents.WEAPON),"sword mining/attack/tag components "+kind.id);
            if(kind.carrier==Items.GOLDEN_SWORD)require(stack.is(ItemTags.PIGLIN_LOVED),"source golden-sword piglin membership");
            if(kind.carrier==Items.SHIELD||kind.carrier==Items.CARROT_ON_A_STICK)require(stack.is(TagKey.create(Registries.ITEM,Identifier.parse("minecraft:enchantable/durability"))),"native durability enchantment admission "+kind.id);
        }
        for(var resource:FRAGMENT_PROFILES)compare(game,f,LbcMeleeItems.Kind.FRAGMENT_SWORD,resource);
    }SkyWars2.LOGGER.info("[check] PASS native-melee (15 source/default prototypes; all31 exact vanilla carrier component oracles; native sword/tool/shield machinery and tag graph, 17 Fragment states, Polymer/server-codec roundtrips)");}
    private static final List<String> FRAGMENT_PROFILES=List.of(
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__03104ededa2e.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__1ec119fbc67c.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__58a71d5245dc.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__5fe7aecfbe7e.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__7fbc1bc761b4.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__863ea09489be.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__a16daa1c6e69.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__a816236f7fe5.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__b1a94cc71340.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__b5d00275d984.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__be72e67b7068.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__cfc0856e0ad9.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__d3c66a33acb7.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__d67fd4d4abe7.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__e8e6d150db1a.json",
            "/data/lbc/loot_table/item_defs/lbc/fragmentsword__fb9c46618c51.json");
}
