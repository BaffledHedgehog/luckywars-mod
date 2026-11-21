package me.skywars2.game;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.entities.mob_ai.LbcRandomMobSpawn;
import me.skywars2.game.items.food.LbcPills;
import me.skywars2.game.items.loot.LbcAllItemPools;
import me.skywars2.validation.mixin.ItemFixtureAccess;
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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.scores.ScoreHolder;

/** Frozen native-registry expectations, exact Infinity lottery membership, tags and native PhD result take. */
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
    /** Select one known index through the actual loot entry; variant draws still use native RNG. */
    private static final class InfinityDraw extends LegacyRandomSource {
        private final int size,index;
        private boolean selected;
        InfinityDraw(int size,int index){super(268361115L);this.size=size;this.index=index;}
        @Override public int nextInt(int bound){
            if(selected)return super.nextInt(bound);
            require(bound==size,"native Infinity chooses uniformly from every vanilla slot");
            selected=true;
            return index;
        }
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
        var entries=pool.getAsJsonArray("entries");var additions=metadata.getAsJsonArray("addedItems");
        require(entries.equals(JsonParser.parseString("""
                [{"type":"skywars2:uniform_item","domain":"vanilla","types":["no_recursive_shulker"]}]
                """)),"one unconditional native vanilla Infinity selector without recursive shulkers");
        var manifest=json("/skywars2/all_item_pools.json");
        var nodes=manifest.getAsJsonObject("entryNodes");
        var members=manifest.getAsJsonObject("vanillaSourceViews").getAsJsonObject("no_recursive_shulker").getAsJsonObject("members");
        var service=game.items().allItemPools();service.initialize();
        var infinitySlots=service.slots(LbcAllItemPools.Domain.VANILLA,List.of("no_recursive_shulker"));
        var indexes=new HashMap<String,Integer>();
        for(int index=0;index<infinitySlots.size();index++){
            String slot=infinitySlots.get(index);
            require(slot.startsWith("vanilla/")&&indexes.put(slot.substring(8),index)==null,
                    "variant profiles never create a second lottery ticket or add custom items: "+slot);
        }
        require(indexes.keySet().equals(expected)&&members.keySet().equals(new HashSet<>(infinitySlots)),
                "Infinity runtime and authored profiles contain every current vanilla registry item exactly once");
        var params=new LootParams.Builder(p.level()).withParameter(LootContextParams.ORIGIN,p.position())
                .withParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.CHEST);
        var actualParent=game.server().reloadableRegistries().getLootTable(key("lbc:infinity_no_shulker"));
        var nativePools=((ItemFixtureAccess.Loot)(Object)actualParent).validation$pools();require(nativePools.size()==1,"one native registry pool");
        var nativeEntries=nativePools.getFirst().entries;
        require(nativeEntries.size()==1&&nativeEntries.getFirst() instanceof LbcAllItemPools.Entry,"loaded Infinity table uses the exact typed entry");
        var expanded=new ArrayList<LootPoolEntry>();
        var context=new LootContext.Builder(params).withOptionalRandomSeed(17).create(Optional.empty());
        nativeEntries.getFirst().expand(context,expanded::add);
        require(expanded.size()==1&&expanded.getFirst().getWeight(0)==1&&expanded.getFirst().getWeight(1000000)==1,
                "native Infinity selector has one unconditional equal-weight ticket independent of luck");
        var selector=expanded.getFirst();
        var addedIds=new HashSet<String>();
        for(var added:additions){
            String item=added.getAsJsonObject().get("item").getAsString();
            require(expected.contains(item)&&addedIds.add(item),"new distinct vanilla item "+item);
            var profiles=members.getAsJsonArray("vanilla/"+item);
            var shape=new JsonObject();shape.addProperty("type","minecraft:item");shape.addProperty("name",item);
            require(profiles.size()==1&&shape.equals(nodes.get(profiles.get(0).getAsString())),"exact direct authored profile "+item);
            var random=new InfinityDraw(infinitySlots.size(),indexes.get(item));
            var roll=new LootContext.Builder(params).withOptionalRandomSource(random).create(Optional.empty());
            var result=new ArrayList<ItemStack>();
            selector.createItemStack(result::add,roll);
            var type=BuiltInRegistries.ITEM.get(Identifier.parse(item)).orElseThrow().value();
            require(random.selected&&result.size()==1&&ItemStack.matches(result.getFirst(),new ItemStack(type)),
                    "actual native Infinity roll preserves exact item/default components "+item+" got="+result);
        }
        var observedNew=new HashSet<String>();
        for(long seed=1;seed<=64;seed++)for(var stack:actualParent.getRandomItems(params,seed)){
            var id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            require(expected.contains(id)&&!stack.has(DataComponents.CONTAINER_LOOT),"actual Infinity rolls reject custom or recursive rewards: "+id);
            if(addedIds.contains(id))observedNew.add(id);
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
