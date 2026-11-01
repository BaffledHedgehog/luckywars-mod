package me.skywars2.game;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.scores.ScoreHolder;

/** Frozen native-registry expectations, actual appended loot entries, tags and ordinary PhD result take. */
public final class ContentCatalogChecks {
    private ContentCatalogChecks() { }
    private static void require(boolean ok,String why) { if(!ok) throw new IllegalStateException("Content catalog: "+why); }
    private static JsonObject json(String path) {
        try(var input=ContentCatalogChecks.class.getResourceAsStream(path)) {
            require(input!=null,"packaged source exists "+path);
            return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(java.io.IOException error) { throw new IllegalStateException("Cannot read "+path,error); }
    }
    private static ResourceKey<LootTable> key(String id) { return ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(id)); }
    private static ItemStack singleton(GameController game,LootParams params,String table) {
        var result=game.server().reloadableRegistries().getLootTable(key(table)).getRandomItems(params,268361115L);
        require(result.size()==1,"native singleton leaf "+table+" outputs="+result);
        return result.getFirst();
    }
    private static Set<String> strings(JsonArray rows) {
        var result=new TreeSet<String>(); for(var row:rows) require(result.add(row.getAsString()),"unique expected ID "+row);
        return result;
    }
    public static void run(GameController game) {
        require(game.playing(),"requires an actual match");
        var p=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        require(p.containerMenu==p.inventoryMenu && !p.isUsingItem(),"idle ordinary player inventory fixture");
        var metadata=json("/skywars2/content_catalog_refresh.json");
        var expected=strings(metadata.getAsJsonArray("expectedVanillaItems"));
        var actual=new TreeSet<String>();
        for(var item:BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(item);
            if(id.getNamespace().equals("minecraft") && item!=Items.AIR) actual.add(id.toString());
        }
        require(expected.equals(actual),"frozen native report exactly matches current vanilla item registry (expected="+expected.size()+" actual="+actual.size()+")");
        var parent=json("/data/lbc/loot_table/infinity_no_shulker.json");
        var pools=parent.getAsJsonArray("pools"); require(pools.size()==1,"one author infinity pool");
        var pool=pools.get(0).getAsJsonObject(); require(pool.get("rolls").getAsInt()==26,"author26 rolls retained");
        var entries=pool.getAsJsonArray("entries"); var additions=metadata.getAsJsonArray("addedItems");
        require(entries.size()==metadata.get("originalPoolEntryCount").getAsInt()+additions.size(),"original pool cardinality plus exact additions");
        var params=new LootParams.Builder(p.level()).withParameter(LootContextParams.ORIGIN,p.position())
                .withParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.CHEST);
        var addedIds=new HashSet<String>();
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        for(int index=0;index<additions.size();index++) {
            String item=additions.get(index).getAsJsonObject().get("item").getAsString();
            require(expected.contains(item)&&addedIds.add(item),"new distinct vanilla item "+item);
            var entry=entries.get(metadata.get("originalPoolEntryCount").getAsInt()+index).getAsJsonObject();
            var shape=new JsonObject();shape.addProperty("type","minecraft:item");shape.addProperty("name",item);shape.addProperty("weight",1);
            require(entry.equals(shape),"exact direct item entry in existing pool "+item);
            var singleEntries=new JsonArray();singleEntries.add(entry.deepCopy());
            var singlePool=new JsonObject();singlePool.addProperty("rolls",1);singlePool.add("entries",singleEntries);
            var singlePools=new JsonArray();singlePools.add(singlePool);
            var singleTable=new JsonObject();singleTable.add("pools",singlePools);
            // Decode the ACTUAL parent entry with vanilla's codec; no extra resource tables are created.
            var decoded=LootTable.DIRECT_CODEC.parse(ops,singleTable).getOrThrow();
            var result=decoded.getRandomItems(params,268361115L);
            var type=BuiltInRegistries.ITEM.get(Identifier.parse(item)).orElseThrow().value();
            require(result.size()==1&&ItemStack.matches(result.getFirst(),new ItemStack(type)),"native decoded direct entry exact item/default components "+item+" got="+result);
        }
        var observedNew=new HashSet<String>();
        var actualParent=game.server().reloadableRegistries().getLootTable(key("lbc:infinity_no_shulker"));
        for(long seed=1;seed<=64;seed++)for(var stack:actualParent.getRandomItems(params,seed)){
            var id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();if(addedIds.contains(id))observedNew.add(id);
        }
        require(!observedNew.isEmpty(),"actual packaged parent draws new vanilla rewards");
        for(String tag:List.of("mobs","monsters","magic_mobs"))
            require(EntityTypes.SULFUR_CUBE.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE,Identifier.withDefaultNamespace(tag))),"native sulfur cube entity tag #minecraft:"+tag);
        var bio=TagKey.create(Registries.ITEM,Identifier.parse("minecraft:bioitems"));
        for(var raw:metadata.getAsJsonArray("bioAdded")) {
            var id=Identifier.parse(raw.getAsString());
            require(BuiltInRegistries.ITEM.get(id).orElseThrow().is(bio),"native BIO membership "+id);
        }
        require(LbcRandomMobSpawn.ENTRIES.size()==91 && LbcRandomMobSpawn.ENTRIES.getLast().type().equals("minecraft:sulfur_cube"),"source random-mob producer appends sulfur_cube at91");
        var saved=new RegenerationBeltChecks.Snapshot(game,p);
        var attribution=new SauvojenMeditationChecks.Attribution(game,List.of(p));
        var menu=(AbstractCraftingMenu)p.inventoryMenu;var slots=menu.getInputGridSlots();
        var grid=slots.stream().map(slot->slot.getItem().copy()).toList();
        var cursor=menu.getCarried().copy();var oldResult=menu.getResultSlot().getItem().copy();
        var crafted=Stats.ITEM_CRAFTED.get(LbcPills.PILL);int beforeCrafted=p.getStats().getValue(crafted);
        Entity spawned=null;
        try {
            p.setGameMode(GameType.SURVIVAL);p.getAbilities().instabuild=false;p.setShiftKeyDown(false);
            spawned=LbcRandomMobSpawn.spawn(game,p,LbcRandomMobSpawn.ENTRIES.size());
            require(spawned!=null && spawned.getType()==EntityTypes.SULFUR_CUBE && p.level().getEntity(spawned.getUUID())==spawned,"actual selected-last native Summon producer body");
            spawned.discard();require(spawned.isRemoved(),"only own summoned fixture body removed before clocks tick");spawned=null;
            p.getInventory().clearContent();menu.setCarried(ItemStack.EMPTY);
            var certificate=singleton(game,params,"lbc:ported/phd_certificate");certificate.setCount(2);
            certificate.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("content refresh retained certificate"));
            var retained=certificate.copy();
            var donors=List.of("minecraft:golden_dandelion","minecraft:poplar_log","minecraft:sulfur_cube_spawn_egg");
            var objective=Objects.requireNonNull(game.server().getScoreboard().getObjective("lbc.math"));
            var seed=Objects.requireNonNull(game.server().getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly("#game_seed"),objective));
            int hash=seed.value();require(hash>=1&&hash<=10000000,"native per-match source seed");
            for(String id:donors) for(int i=10;i<Math.min(id.length(),37);i++) {
                char c=id.charAt(i);if(c>='a'&&c<='z')hash+=c-'a'+1;else if(c=='_')hash+=27;
            }
            var pill=singleton(game,params,"lbc:ported/pills/pill_"+Math.floorMod(hash,144));
            slots.getFirst().setByPlayer(certificate);
            for(int i=0;i<3;i++) slots.get(i+1).setByPlayer(new ItemStack(BuiltInRegistries.ITEM.get(Identifier.parse(donors.get(i))).orElseThrow().value(),2));
            menu.slotsChanged(p.getInventory());
            require(ItemStack.matches(menu.getResultSlot().getItem(),pill),"new BIO inputs give canonical pill from independent original character hash");
            require(ItemStack.matches(slots.getFirst().getItem(),retained),"preview preserves reusable certificate");
            GameChecks.inventoryClick(p,0,0,ContainerInput.PICKUP);
            require(ItemStack.matches(menu.getCarried(),pill),"ordinary native2x2 output take delivers exactly canonical pill");
            require(ItemStack.matches(slots.getFirst().getItem(),retained),"result take retains exact certificate count/name/components");
            for(int i=0;i<3;i++) {
                var stack=slots.get(i+1).getItem();
                require(stack.getCount()==1&&BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(donors.get(i)),"exact single BIO debit "+donors.get(i));
            }
            require(p.getStats().getValue(crafted)==beforeCrafted+1,"native crafted stat increments once");
        } finally {
            if(spawned!=null) spawned.discard();
            menu.setCarried(ItemStack.EMPTY);p.inventoryMenu.getCraftSlots().clearContent();
            saved.restore(game);saved.restoreAttributes();saved.restoreUse();
            p.getStats().setValue(p,crafted,beforeCrafted);attribution.close();
            for(int i=0;i<grid.size();i++)slots.get(i).setByPlayer(grid.get(i));
            menu.getResultSlot().set(oldResult);menu.setCarried(cursor);
        }
        SkyWars2.LOGGER.info("[check] PASS content-catalog vanillaItems={} newNativeItemEntries={} bioAdded={} nativeSulfurSummon=true nativePhdTake=true",actual.size(),additions.size(),metadata.getAsJsonArray("bioAdded").size());
    }
}
