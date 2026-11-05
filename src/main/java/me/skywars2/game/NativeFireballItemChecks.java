package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.hurtingprojectile.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Actual native input publishes only final actors before returning; old raw aliases use the same factory. */
public final class NativeFireballItemChecks {
    private NativeFireballItemChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Native fireball items: "+why);}
    static ItemStack comparable(ItemStack item){var result=new ItemStack(Items.SNOWBALL,item.getCount());result.applyComponents(item.getComponents());return result;}
    static void presentation(GameController game,SpecialFireballChecks.Fixture f){
        var params=new LootParams.Builder(f.owner.level()).withParameter(LootContextParams.ORIGIN,f.at)
                .withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)f.owner.connection;
        int index=0;
        for(var entry:LbcSpecialFireballItems.entries()){
            require(entry.item() instanceof SnowballItem&&LbcSpecialFireballs.variant(entry.item().getDefaultInstance())==++index,"native type and source variant "+entry.id());
            try(var stream=NativeFireballItemChecks.class.getResourceAsStream("/"+entry.definition())){
                var original=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();
                original.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name","minecraft:snowball");
                var oracle=LootTable.DIRECT_CODEC.parse(ops,original).getOrThrow();
                var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+entry.definition().substring("data/lbc/loot_table/".length()).replace(".json","")));
                var table=game.server().reloadableRegistries().getLootTable(key);boolean seen=false;
                for(int seed=1;seed<=24;seed++){
                    var actual=table.getRandomItems(params,seed);var expected=oracle.getRandomItems(params,seed);
                    require(actual.size()==expected.size(),"source zero-count outcomes preserved "+entry.id());
                    for(int i=0;i<actual.size();i++){
                        var stack=actual.get(i);require(stack.isEmpty()==expected.get(i).isEmpty(),"source zero-count stack preserved "+entry.id());
                        if(stack.isEmpty())continue;seen=true;
                        require(stack.is(entry.item())&&ItemStack.matches(comparable(stack),expected.get(i)),"independent original-carrier all-component/count oracle "+entry.id()+" seed="+seed+" actual="+stack+" expected="+expected.get(i));
                        require(stack.getComponents().equals(entry.item().getDefaultInstance().getComponents()),"loot and give prototypes agree "+entry.id());
                    }
                }
                require(seen,"registered canonical source table "+entry.id());
                var publicTable=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:ported/"+entry.id())));
                boolean publicSeen=false;for(int seed=1;seed<=16;seed++)for(var stack:publicTable.getRandomItems(params,seed))if(!stack.isEmpty()){publicSeen=true;require(stack.is(entry.item()),"ready wrapper references same canonical item "+entry.id());}require(publicSeen,"ready wrapper registered "+entry.id());
                var stack=entry.item().getDefaultInstance();
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),f.owner.registryAccess());
                require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),f.owner.registryAccess())),"complete Polymer roundtrip "+entry.id());
            }catch(java.io.IOException e){throw new IllegalStateException(entry.id(),e);}
        }
    }
    static void launches(GameController game,SpecialFireballChecks.Fixture f){
        int index=0;
        for(var entry:LbcSpecialFireballItems.entries()){
            int kind=index++;
            for(var hand:InteractionHand.values()){
                f.clear();var stack=new ItemStack(entry.item(),2);var other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
                f.owner.setItemInHand(hand,stack);f.owner.setItemInHand(other,new ItemStack(Items.STICK,7));
                int stat=f.owner.getStats().getValue(Stats.ITEM_USED.get(entry.item()));int tick=game.server().getTickCount();
                require(f.owner.gameMode.useItem(f.owner,f.owner.level(),stack,hand).consumesAction(),"actual native use "+entry.id()+" "+hand);
                var actors=f.service.snapshot();
                require(game.server().getTickCount()==tick&&f.service.pendingCount()==0&&actors.size()==(kind==3?33:kind==5||kind==6?17:1),"all final actors published synchronously, no pending conversion "+entry.id());
                for(var e:f.owner.level().getAllEntities())require(f.before.contains(e)||!(e instanceof Snowball)||e.isRemoved(),"no intermediate snowball enters the world");
                require(stack.getCount()==1&&f.owner.getItemInHand(other).is(Items.STICK)&&f.owner.getItemInHand(other).getCount()==7
                        &&f.owner.getStats().getValue(Stats.ITEM_USED.get(entry.item()))==stat+1,"one real used-hand debit/stat "+entry.id());
                require(actors.stream().allMatch(e->f.service.owner(e)==f.owner&&game.items().currentEpoch(e)&&e.tickCount==0),"actual creator/epoch before first actor tick "+entry.id());
                require(actors.stream().allMatch(e->kind==0?e instanceof SmallFireball:kind==3?e instanceof PrimedTnt:kind==1||kind==4?e instanceof LargeFireball:kind==6?e instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl:e instanceof DragonFireball),"exact native body family "+entry.id());
                if(kind==3)require(actors.stream().allMatch(e->((PrimedTnt)e).getFuse()==100),"source TNT fuse starts at100 before any tick");
            }
            f.clear();var held=new ItemStack(entry.item(),2);f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
            f.owner.entityTags().add("time_stopped");try{require(!f.owner.gameMode.useItem(f.owner,f.owner.level(),held,InteractionHand.MAIN_HAND).consumesAction()&&held.getCount()==2&&f.service.trackedCount()==0,"frozen input retains item and emits nothing "+entry.id());}finally{f.owner.entityTags().remove("time_stopped");}
        }
        f.clear();f.owner.setGameMode(GameType.CREATIVE);var free=new ItemStack(LbcSpecialFireballItems.entries().getFirst().item(),2);f.owner.setItemInHand(InteractionHand.MAIN_HAND,free);
        try{require(f.owner.gameMode.useItem(f.owner,f.owner.level(),free,InteractionHand.MAIN_HAND).consumesAction()&&free.getCount()==2&&f.service.trackedCount()==1,"native creative use retains ammunition");}finally{f.owner.setGameMode(GameType.SURVIVAL);f.clear();}
    }
    public static void run(GameController game){
        boolean levelups=game.config().lbcLevelups;var stats=new LinkedHashMap<Item,Integer>();
        try(var f=new SpecialFireballChecks.Fixture(game)){
            game.config().lbcLevelups=false;f.initialize();for(var entry:LbcSpecialFireballItems.entries())stats.put(entry.item(),f.owner.getStats().getValue(Stats.ITEM_USED.get(entry.item())));
            try{presentation(game,f);launches(game,f);}finally{for(var entry:stats.entrySet())f.owner.getStats().setValue(f.owner,Stats.ITEM_USED.get(entry.getKey()),entry.getValue());}
        }finally{game.config().lbcLevelups=levelups;}
        ImbaPearlChecks.run(game);
        SkyWars2.LOGGER.info("[check] PASS native-fireball-items (seven canonical native prototypes, seeded source counts/all components, ready wrappers/Polymer, both actual hands, immediate1/33/17 final bodies/no snowball or pending tick, creator/epoch/fuse/pearl ledger, sole debit/stat, frozen and creative input)");
    }
}
