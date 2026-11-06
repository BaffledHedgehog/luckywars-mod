package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

/** Actual imported loot, shared refill/restoration routes and native lucky-block placement. */
public final class LuckyChestChecks {
    private LuckyChestChecks(){}
    private static final Identifier COUNT=Identifier.parse("minecraft:count");
    private static final List<String> CATEGORIES=List.of("default","midle","center");
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Lucky chests: "+why);}
    private static ResourceKey<LootTable> key(String id){return ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(id));}
    private static JsonObject source(String id){var split=id.split(":",2);var resource="/data/"+split[0]+"/loot_table/"+split[1]+".json";
        try(var input=LuckyChestChecks.class.getResourceAsStream(resource)){
            require(input!=null,"packaged source "+id);return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(java.io.IOException error){throw new IllegalStateException(error);}}
    private static void closure(GameController game,String id,Set<String> seen,List<String> quantities){
        if(!seen.add(id))return;require(game.server().reloadableRegistries().getLootTable(key(id))!=LootTable.EMPTY,"loaded dependency "+id);walk(game,source(id),id,seen,quantities);
    }
    private static void walk(GameController game,JsonElement node,String id,Set<String> seen,List<String> quantities){
        if(node.isJsonArray()){for(var child:node.getAsJsonArray())walk(game,child,id,seen,quantities);return;}
        if(!node.isJsonObject())return;var row=node.getAsJsonObject();String type=row.has("type")?row.get("type").getAsString():"";
        if(type.equals("minecraft:loot_table"))closure(game,row.get("value").getAsString(),seen,quantities);
        if(type.equals("minecraft:set_attributes")){require(row.getAsJsonArray("modifiers").size()==1&&row.getAsJsonArray("modifiers").get(0).getAsJsonObject().get("id").getAsString().equals("minecraft:count"),"explicit quantity marker "+id);quantities.add(id);}
        require(!type.equals("nbt"),"no uninitialized language-storage text "+id);
        for(var child:row.entrySet())walk(game,child.getValue(),id,seen,quantities);
    }
    private static LootParams params(WoolBucketChecks.Fixture f){return new LootParams.Builder(f.level).withParameter(LootContextParams.ORIGIN,f.home).withOptionalParameter(LootContextParams.THIS_ENTITY,f.owner).create(LootContextParamSets.CHEST);}
    private static ItemStack roll(GameController game,WoolBucketChecks.Fixture f,String id,long seed){var result=game.server().reloadableRegistries().getLootTable(key(id)).getRandomItems(params(f),seed);require(result.size()==1,"one source profile "+id);return result.getFirst();}
    private static void profiles(GameController game,WoolBucketChecks.Fixture f){
        var seen=new HashSet<String>();var quantities=new ArrayList<String>();for(var category:CATEGORIES)closure(game,"swrg:chests/"+category+"_lb",seen,quantities);
        require(seen.size()==62&&quantities.size()==23,"complete finite source closure");
        for(var id:quantities){var values=new HashSet<Integer>();var definition=source(id);var function=definition.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("modifier").asList().stream().map(JsonElement::getAsJsonObject).filter(v->v.get("type").getAsString().equals("minecraft:set_attributes")).findFirst().orElseThrow();var provider=function.getAsJsonArray("modifiers").get(0).getAsJsonObject().get("amount");
            for(int sample=1;sample<=32;sample++){long seed=0x9e3779b97f4a7c15L*sample;
                var raw=roll(game,f,id,seed);var attrs=raw.get(DataComponents.ATTRIBUTE_MODIFIERS);var marker=attrs.modifiers().stream().filter(e->e.modifier().id().equals(COUNT)).findFirst().orElseThrow();int amount=(int)marker.modifier().amount();
                require(raw.getCount()==1&&amount>0,"source one occupied slot before post-fill count "+id);values.add(amount);
                var container=new SimpleContainer(raw.copy());MapManager.normalizeLootCounts(container);var fixed=container.getItem(0);require(fixed.getCount()==amount&&!fixed.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().anyMatch(e->e.modifier().id().equals(COUNT)),"native count matches actual old post-fill amount and removes only metadata "+id);
                for(var component:raw.getComponents())if(component.type()!=DataComponents.ATTRIBUTE_MODIFIERS)require(Objects.equals(component.value(),fixed.get(component.type())),"count preserves authored component "+component.type());
                var actualOthers=fixed.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(actualOthers.equals(attrs.modifiers().stream().filter(e->!e.modifier().id().equals(COUNT)).toList()),"legitimate attributes retained "+id);
                if(raw.is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NORMAL)))blockOracle(game,f,id,definition,raw,seed);
            }
            require(!provider.isJsonObject()||provider.getAsJsonObject().get("max").getAsDouble()-provider.getAsJsonObject().get("min").getAsDouble()<=1||values.size()>1,"random quantities with multiple possible integer counts roll afresh "+id);
        }
        var locator=roll(game,f,"lbc:item_defs/minecraft/compass__77ff6dac04ff",17);require(locator.is(Items.COMPASS)&&locator.get(DataComponents.CUSTOM_NAME).equals(Component.translatable("swrg.item.compass").withStyle(ChatFormatting.YELLOW).withStyle(s->s.withItalic(false))),"source yellow localized locator without storage");
    }
    private static void blockOracle(GameController game,WoolBucketChecks.Fixture f,String id,JsonObject definition,ItemStack actual,long seed){
        var original=definition.deepCopy();original.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("name","minecraft:red_mushroom_block");
        var ops=game.server().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);var old=LootTable.DIRECT_CODEC.parse(ops,original).getOrThrow().getRandomItems(params(f),seed).getFirst();
        require(actual.getCount()==old.getCount()&&actual.getComponents().keySet().equals(old.getComponents().keySet()),"complete original block profile/count "+id+" native="+actual.getComponents().keySet()+" original="+old.getComponents().keySet());
        for(var component:old.getComponents())if(component.type()!=DataComponents.ITEM_NAME)require(Objects.equals(component.value(),actual.get(component.type())),"every original authored/native carrier component "+component.type()+" "+id);
        require(Objects.equals(actual.get(DataComponents.ITEM_NAME),actual.getItem().getDefaultInstance().get(DataComponents.ITEM_NAME)),"native block identity supplies its base item name; authored visible name remains exact");
        var context=((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.owner.connection).getPacketContext();var view=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(actual,TooltipFlag.NORMAL,context,f.owner.registryAccess());require(view.is(Items.RED_MUSHROOM_BLOCK)&&ItemStack.matches(actual,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(view,context,f.owner.registryAccess())),"Polymer exact profile roundtrip");
        var nbt=f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);require(ItemStack.matches(actual,ItemStack.CODEC.parse(nbt,ItemStack.CODEC.encodeStart(nbt,actual).getOrThrow()).getOrThrow()),"native profile disk codec");
    }
    private static Marker marker(WoolBucketChecks.Fixture f,BlockPos at,String category){var actor=new Marker(EntityTypes.MARKER,f.level);actor.setPos(Vec3.atCenterOf(at));actor.entityTags().add("swrg."+category);f.own(actor);require(f.level.addFreshEntity(actor),"native category marker");return actor;}
    private static ItemStack sentinel(){return new ItemStack(Items.ECHO_SHARD,13);}
    private static RandomizableContainerBlockEntity chest(WoolBucketChecks.Fixture f,BlockPos at){return (RandomizableContainerBlockEntity)f.level.getBlockEntity(at);}
    private static void content(WoolBucketChecks.Fixture f,BlockPos at,int category,boolean upgrade,boolean lucky){
        int blocks=0;var container=chest(f,at);for(int i=0;i<container.getContainerSize();i++){var stack=container.getItem(i);if(stack.isEmpty())continue;require(!stack.is(Items.ECHO_SHARD),"refill replaces previous inventory");var attrs=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);require(attrs==null||attrs.modifiers().stream().noneMatch(e->e.modifier().id().equals(COUNT)),"all actual chest quantities normalized");
            if(stack.is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NORMAL))){blocks++;int min=upgrade||category==2?5:category==1?6:4;int max=upgrade||category==2?11:category==1?8:4;require(stack.getCount()>=min&&stack.getCount()<=max,"category/upgrade quantity from real source pool");}
            require(!stack.is(Items.RED_MUSHROOM_BLOCK),"functional Lucky blocks use real native block identity");}
        require(blocks==(lucky?1:0),"exactly one lucky profile only in Lucky family");
    }
    public static void run(GameController game){try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NORMAL)))){
        int savedMode=game.config().gameMode,activeMode=game.gameMode();var markerTags=new HashMap<Entity,Set<String>>();try{
            for(var kind:LbcLuckyBlocks.Kind.values()){var carrier=switch(kind){case NORMAL->Items.RED_MUSHROOM_BLOCK;case NEKO->Items.BROWN_MUSHROOM_BLOCK;case LOLI->Items.MUSHROOM_STEM;};require(Objects.equals(LbcLuckyBlocks.item(kind).getDefaultInstance().get(DataComponents.COMPOSTABLE),carrier.getDefaultInstance().get(DataComponents.COMPOSTABLE)),"exact original composting component "+kind);}
            profiles(game,f);f.prepareParticipants();
            // Snapshot every real map chest touched by the shared all-arena refill, not just test anchors.
            for(var entity:f.level.getAllEntities())if(entity instanceof Marker&&entity.entityTags().stream().anyMatch(t->List.of("swrg.default","swrg.midle","swrg.center").contains(t))){f.remember(entity.blockPosition());markerTags.put(entity,Set.copyOf(entity.entityTags()));}
            var origin=f.owner.blockPosition().offset(0,8,5);var positions=new ArrayList<BlockPos>();
            for(int i=0;i<3;i++){var at=origin.offset(i*3,0,0);positions.add(at);f.set(at,Blocks.BARREL.defaultBlockState());marker(f,at,CATEGORIES.get(i));}
            var control=origin.offset(0,0,24);f.set(control,Blocks.BARREL.defaultBlockState());marker(f,control,"default");
            var fakeAt=origin.offset(0,0,5);f.set(fakeAt,Blocks.AIR.defaultBlockState());var fake=new ItemEntity(f.level,fakeAt.getX()+.5,fakeAt.getY(),fakeAt.getZ()+.5,new ItemStack(Items.COPPER_INGOT));fake.entityTags().add("swrg.center");f.own(fake);require(f.level.addFreshEntity(fake),"tagged non-marker witness");
            for(int mode:new int[]{1,2,2}){game.config().gameMode=mode;game.beginMatchMode(game.config().gameMode);for(boolean upgrade:new boolean[]{false,true}){
                for(var at:positions)chest(f,at).setItem(0,sentinel());game.maps().fillChests(upgrade);
                for(int i=0;i<3;i++)content(f,positions.get(i),i,upgrade,mode==2);
                require(f.level.getBlockState(fakeAt).isAir()&&!fake.isRemoved(),"tagged transport never becomes a block chest");
            }}
            game.config().gameMode=2;game.beginMatchMode(game.config().gameMode);game.events().stageOne(4);for(int i=0;i<3;i++)content(f,positions.get(i),i,false,true);
            game.config().gameMode=2;game.beginMatchMode(game.config().gameMode);chest(f,control).clearContent();chest(f,control).setItem(0,sentinel());for(var at:positions){chest(f,at).clearContent();chest(f,at).setItem(0,sentinel());}
            game.maps().restoreRespawnIsland(new MapManager.Spawn(Vec3.atCenterOf(origin),0,0));for(int i=0;i<3;i++)content(f,positions.get(i),i,false,true);require(ItemStack.matches(chest(f,control).getItem(0),sentinel()),"island radius leaves distant chest unchanged");
            for(var pos:BlockPos.betweenClosed(origin.offset(-5,-5,-5),origin.offset(5,5,5)))f.remember(pos.immutable());
            require(game.maps().restoreMadokaRegion(f.level,Vec3.atCenterOf(origin)),"actual shared Madoka region restore");for(int i=0;i<3;i++)content(f,positions.get(i),i,false,true);require(ItemStack.matches(chest(f,control).getItem(0),sentinel()),"Madoka radius leaves distant chest unchanged");
            game.events().stageOne(7);for(int i=0;i<3;i++)content(f,positions.get(i),i,true,true);game.maps().fillChests(false);for(int i=0;i<3;i++)content(f,positions.get(i),i,true,true);
            for(int mode:new int[]{3,4}){game.config().gameMode=mode;game.beginMatchMode(game.config().gameMode);for(var at:positions){chest(f,at).clearContent();chest(f,at).setItem(0,sentinel());}game.maps().fillChests(true);for(var at:positions)require(ItemStack.matches(chest(f,at).getItem(0),sentinel()),"BedWars never receives SkyWars refill");}
            game.config().gameMode=2;game.beginMatchMode(game.config().gameMode);var base=origin.offset(0,-4,-4);for(var profile:List.of("variant_e98680e387b5","variant_e5d5f79f4617","variant_cebd527e2a9c")){
                f.set(base,Blocks.STONE.defaultBlockState());f.set(base.above(),Blocks.AIR.defaultBlockState());var stack=roll(game,f,"lbc:item_defs/lbc/lucky_block__e196d020d84e/"+profile,17);var box=new SimpleContainer(stack);MapManager.normalizeLootCounts(box);int count=stack.getCount();f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(base).add(0,.5,0),Direction.UP,base,false)).consumesAction(),"actual profile placement input");require(f.level.getBlockState(base.above()).is(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL))&&stack.getCount()==count-1,"real native Lucky block placement and exact one-item debit");
            }
            require(MapManager.chestLootTable(1,"default",false).identifier().toString().equals("swrg:chests/default_0")&&MapManager.chestLootTable(2,"default",false).identifier().toString().equals("swrg:chests/default_lb")&&MapManager.chestLootTable(2,"default",true).identifier().toString().equals("swrg:chests/center_lb")&&MapManager.chestLootTable(3,"center",true)==null&&MapManager.chestLootTable(4,"center",false)==null,"explicit authored mode/category/upgrade policy");
        }finally{game.config().gameMode=savedMode;game.beginMatchMode(activeMode);markerTags.forEach((entity,tags)->{entity.entityTags().clear();entity.entityTags().addAll(tags);});}
    }SkyWars2.LOGGER.info("[check] PASS lucky-chests (62 real loaded dependencies;23 seeded quantity profiles, three native/Polymer/codec/placement profiles, localized locator; actual initial/repeated/upgrade/island/Madoka routes, BedWars isolation and native scoped restoration)");}
}
