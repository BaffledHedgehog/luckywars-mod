package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Independent canonical/vanilla oracle, actual native marker-egg placement and shared owner checks. */
public final class NativeUtilityInputChecks {
    private record Source(String id,String resource,Item item,Item carrier){}
    private static List<Source> sources(){var out=new ArrayList<Source>();
        for(var k:LbcRewardToolItems.Kind.values())out.add(new Source(k.id,k.definition,k.item,Items.CARROT_ON_A_STICK));
        out.add(new Source("imba_nuclear_bomb","data/lbc/loot_table/item_defs/lbc/nuke__c507f2f53fda.json",LbcImbaPlaceableItems.NUCLEAR_BOMB,Items.PANDA_SPAWN_EGG));
        out.add(new Source("bridge_builder_imba","data/lbc/loot_table/item_defs/lbc/bridgebuilder__d20c5b8711ad.json",LbcBridgeBuilderItem.TOKEN,Items.SNOWBALL));
        out.add(new Source("bad_apple_imba","data/lbc/loot_table/item_defs/lbc/badapple1__53d62d9b2e05.json",LbcBadAppleItem.TOKEN,Items.REPEATING_COMMAND_BLOCK));return out;}
    private NativeUtilityInputChecks(){}
    private static void require(boolean v,String why){if(!v)throw new IllegalStateException("Native utility input: "+why);}
    public static void run(GameController game){try(var s=new Scene(game)){s.begin();}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> before=new HashSet<>();final int used;boolean touchedNuke;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);used=f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB));for(var e:f.level.getAllEntities())before.add(e.getUUID());require(game.items().nukeEvent().markerCount()==0&&game.items().nukeEvent().jobCount()==0,"exclusive nuclear fixture");}
        void begin(){f.prepareParticipants();presentation();nuclear();}
        void presentation(){var params=new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.home).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)f.owner.connection;
            for(var source:sources()){var stack=f.leaf(source.id);require(stack.is(source.item)&&stack.getCount()==1,"canonical identity/quantity "+source.id);var defaults=new ItemStack(source.item);
                for(var c:stack.getComponents())require(Objects.equals(c.value(),defaults.get(c.type())),"canonical/default component "+source.id+" "+c.type());
                try(var stream=NativeUtilityInputChecks.class.getResourceAsStream("/"+source.resource)){
                    var raw=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();raw.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name",BuiltInRegistries.ITEM.getKey(source.carrier).toString());
                    var result=LootTable.DIRECT_CODEC.parse(ops,raw).getOrThrow().getRandomItems(params,17);require(result.size()==1,"one independent source output");var expected=result.getFirst();var actual=new ItemStack(source.carrier);actual.applyComponents(stack.getComponents());
                    // Native item's intrinsic fallback title is stable; the authored visible custom name is compared normally.
                    actual.set(DataComponents.ITEM_NAME,expected.get(DataComponents.ITEM_NAME));require(ItemStack.matches(actual.copyWithCount(1),expected.copyWithCount(1)),"all original source components and vanilla intrinsic properties "+source.id);
                }catch(java.io.IOException error){throw new IllegalStateException(error);}
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),f.owner.registryAccess())),"recipient Polymer all-component roundtrip "+source.id);
            }
            require(LbcWands.isBoltMaterial(new ItemStack(LbcRewardToolItems.Kind.LIGHTNING.item)),"native lightning remains a craft material");
        }
        ItemStack held(InteractionHand hand){var stack=f.leaf("imba_nuclear_bomb").copyWithCount(3);f.owner.setItemInHand(hand,stack);f.owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));return stack;}
        BlockHitResult pad(){var p=BlockPos.containing(f.home.add(0,0,2)).below();f.set(p,Blocks.STONE.defaultBlockState());f.set(p.above(),Blocks.AIR.defaultBlockState());f.set(p.above(2),Blocks.AIR.defaultBlockState());return new BlockHitResult(new Vec3(p.getX()+.5,p.getY()+1,p.getZ()+.5),Direction.UP,p,false);}
        void placed(Vec3 point){var service=game.items().nukeEvent();require(service.markerCount()==1&&service.jobCount()==0,"one existing nuclear warning actor, no premature detonation");var marker=service.markers().getFirst();f.own(marker);require(service.owner(marker)==f.owner&&marker.position().equals(point)&&marker.entityTags().containsAll(List.of("nuke_bruh","rc_1sec","marker_2t","mecha")),"actual owner, source placement and full nuke-bruh root");service.stop();require(marker.isRemoved()&&service.markerCount()==0,"shared owner retires source actor");}
        void nuclear(){touchedNuke=true;var hit=pad();var expected=hit.getLocation();int count=used;
            for(var hand:InteractionHand.values()){var stack=held(hand);require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,hand,hit).consumesAction()&&stack.getCount()==2,"real native used-hand nuke debit "+hand);require(f.owner.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).getCount()==7,"other hand is preserved");require(f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB))==++count,"one native use statistic");placed(expected);}
            f.pause();try{for(var hand:InteractionHand.values()){var stack=held(hand);f.owner.gameMode.useItemOn(f.owner,f.level,stack,hand,hit);require(stack.getCount()==3&&game.items().nukeEvent().markerCount()==0,"TimeStop rejects without debit");}}finally{f.thaw();}
            f.field(false,expected);try{var stack=held(InteractionHand.OFF_HAND);require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.OFF_HAND,hit).consumesAction()&&stack.getCount()==2,"MAG does not prevent physical nuclear input");count++;placed(expected);}finally{f.removeFields();}
            var water=BlockPos.containing(f.owner.getEyePosition().add(0,0,3));f.set(water,Blocks.WATER.defaultBlockState());var stack=held(InteractionHand.MAIN_HAND);require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==2,"source-fluid native use");count++;placed(new Vec3(water.getX()+.5,water.getY(),water.getZ()+.5));f.set(water,Blocks.AIR.defaultBlockState());
            f.owner.setGameMode(GameType.CREATIVE);stack=held(InteractionHand.MAIN_HAND);require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()&&stack.getCount()==3,"creative native placement is free");count++;placed(expected);
            require(f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB))==count,"successful input statistics only");
        }
        public void close(){try{if(touchedNuke)game.items().nukeEvent().stop();for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID()))f.own(e);f.owner.getStats().setValue(f.owner,net.minecraft.stats.Stats.ITEM_USED.get(LbcImbaPlaceableItems.NUCLEAR_BOMB),used);}finally{f.close();}}
    }
}
