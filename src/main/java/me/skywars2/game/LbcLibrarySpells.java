package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Remaining authored library entries; component data is imported, spell engines are shared. */
public final class LbcLibrarySpells {
    record Definition(int spellId,String id,Item item) { }
    private static final Map<Integer,Definition> BOOKS=new LinkedHashMap<>();
    private static final Map<Integer,LbcEnderWiki.Effect> WIKI=new HashMap<>();
    static {
        try(var input=LbcLibrarySpells.class.getResourceAsStream("/skywars2/remaining_grimoires.json")){
            if(input==null)throw new IllegalStateException("Missing remaining library presets");
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var row:json.getAsJsonArray("items")){
                var d=row.getAsJsonObject();int id=d.get("spellId").getAsInt();String name=d.get("id").getAsString();
                var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+name));
                var model=Identifier.parse(d.get("model").getAsString());
                var title=ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,d.get("name")).getOrThrow();
                var lore=new ArrayList<net.minecraft.network.chat.Component>();
                for(var line:d.getAsJsonArray("lore"))lore.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,line).getOrThrow());
                var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                        .setId(key).stacksTo(d.get("count").getAsInt()).overrideDescription(title.getString())
                        .component(DataComponents.ITEM_MODEL,model)
                        .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(d.get("tooltip").getAsString()))
                        .component(DataComponents.CUSTOM_NAME,title).component(DataComponents.LORE,new ItemLore(lore))
                        .component(DataComponents.CUSTOM_DATA,CustomData.of(TagParser.parseCompoundFully(d.get("data").getAsString())));
                var item=Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.SNOWBALL,true){
                    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                        var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
                    }
                });
                if(BOOKS.put(id,new Definition(id,name,item))!=null)throw new IllegalStateException("Duplicate library ID "+id);
            }
            for(var entry:json.getAsJsonObject("wiki").entrySet()){
                var d=entry.getValue().getAsJsonObject();WIKI.put(Integer.parseInt(entry.getKey()),new LbcEnderWiki.Effect(d.get("id").getAsString(),d.get("ticks").getAsInt(),d.get("amplifier").getAsInt(),d.get("hidden").getAsBoolean()));
            }
            if(BOOKS.size()!=167+LbcExtraGrimoires.COUNT||WIKI.size()!=84)throw new IllegalStateException("Incomplete library presets");
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    public static void register(){ }
    static List<Definition> definitions(){return BOOKS.values().stream().filter(d->d.spellId()<LbcExtraGrimoires.FIRST).toList();}
    static List<Definition> extraDefinitions(){return BOOKS.values().stream().filter(d->d.spellId()>=LbcExtraGrimoires.FIRST).toList();}
    static LbcEnderWiki.Effect fixedEffect(int id){return WIKI.get(id);}
    static Definition kind(ItemStack stack){
        if(stack.isEmpty())return null;var data=LbcFartus.data(stack);
        var d=BOOKS.get(data.getIntOr("spellid",0));
        if(d!=null&&(stack.is(d.item)||data.getIntOr("libspell",0)==1))return d;
        for(var entry:BOOKS.values())if(stack.is(entry.item))return entry;return null;
    }
    static boolean admitted(GameController game,ServerPlayer p,InteractionHand hand){
        return p.isAlive()&&!p.isRemoved()&&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(p)
                &&p.level()==game.maps().level()&&p.level().hasChunkAt(BlockPos.containing(p.getEyePosition()))
                &&(hand!=InteractionHand.OFF_HAND||game.config().offhandWands)&&!game.items().actionsBlocked(p)
                &&!game.items().wands().inputTimeStopped(p)&&!game.items().magic().suppressed(p)
                &&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());
    }
    static boolean use(GameController game,ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);var kind=kind(stack);
        if(kind==null||!admitted(game,p,hand)||!cast(game,p,hand,kind.spellId))return false;
        p.level().playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.SNOWBALL_THROW,
                net.minecraft.sounds.SoundSource.NEUTRAL,.5f,.4f/(p.getRandom().nextFloat()*.4f+.8f));
        p.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,p);return true;
    }
    static boolean cast(GameController g,ServerPlayer p,InteractionHand hand,int id){
        if(id>=LbcExtraGrimoires.FIRST&&id<=LbcExtraGrimoires.LAST)return LbcExtraGrimoires.cast(g,p,hand,id);
        for(var kind:LbcElementalGrimoires.Kind.values())if(kind.spellId==id)return LbcElementalGrimoires.cast(g,p,hand,kind);
        if(id>=28&&id<=111){g.items().enderWiki().libraryEffect(p,WIKI.get(id));return true;}
        var empty=ItemStack.EMPTY;
        switch(id){
            case 1,2,3,4 -> g.items().evokerBass().evoker(p,hand,empty,id==1?p.getRandom().nextInt(3)+1:id-1);
            case 5,6,7,8,9 -> g.items().evokerBass().bass(p,hand,empty,id==5?p.getRandom().nextInt(4)+1:id-5,id!=5);
            case 10,11,12,13,14 -> LbcCorruption.cast(g,p,hand,empty,id!=10,id==10?p.getRandom().nextInt(4)+1:id-10);
            case 15,16 -> g.items().enderWiki().ender(p,hand,empty,id==16);
            case 17,18 -> g.items().lifestealer().cast(p,hand,empty,id==18);
            case 19 -> g.items().lifestealer().spawn(p);
            case 20,21,22,23,24 -> g.items().bloodConfuse().confuse(p,hand,empty,id!=20,id==20?p.getRandom().nextInt(4)+1:id-20);
            case 25,26 -> g.items().bloodConfuse().blood(p,hand,empty,id==26);
            case 27 -> {var e=LbcEnderWiki.RANDOM[p.getRandom().nextInt(84)];g.items().enderWiki().libraryEffect(p,e);}
            case 112,113,114,115 -> LbcWall.cast(g,p,InteractionHand.MAIN_HAND,empty,id!=112,switch(id){case 113->2;case 114->4;case 115->3;default->1;});
            case 116,117,118,119 -> g.items().advancedSnow().grimoire(p,hand,false,id!=116,id==118?2:id==119?3:1);
            case 120,121,122 -> g.items().homing().libraryStars(p,id==121?3:id==122?2:1);
            case 123,124 -> g.items().prism().cast(p,InteractionHand.MAIN_HAND,empty,id==124);
            case 125 -> g.items().blaze().turret(p,0);
            case 126 -> g.items().blaze().libraryHead(p);
            case 127 -> LbcLibraryRandom.castRandom(g,p,hand,p.getRandom().nextInt(53)+1);
            case 128 -> {
                int selected=128;for(int attempt=0;attempt<5&&selected==128;attempt++)selected=p.getRandom().nextInt(LbcExtraGrimoires.LAST)+1;
                if(selected!=128)return cast(g,p,hand,selected);
            }
            case 129,130,131,132,133 -> {
                if(g.items().godHedgehog().blocksSilverfish(p.level()))return false;
                g.items().hedgehog().cast(p,switch(id){case 130->LbcHedgehog.Kind.SNIPER;case 131->LbcHedgehog.Kind.LASER;case 132->LbcHedgehog.Kind.TANK;case 133->LbcHedgehog.Kind.HEALER;default->LbcHedgehog.Kind.DEFAULT;});
            }
            case 134,135 -> g.items().sheep().cast(p,hand,empty,id==135);
            case 136,137,138,139,140 -> g.items().psycho().psycho(p,hand,empty,id==136?0:id-136);
            case 141,142 -> g.items().wands().teslaVolley(p,InteractionHand.MAIN_HAND,empty,id==142);
            case 143 -> g.items().stonekivi().queuedWork(new LbcTesla.Instant(g,p));
            case 144,145 -> g.items().clouds().volleyAcid(p,p.getEyePosition(),p.getLookAngle(),8,false,id==145);
            case 146,147 -> LbcCausticOmega.omega(g,p,hand,empty,id==147);
            case 148,149,150,151,152,153 -> {g.items().neutral().cast(p,id==148?p.getRandom().nextInt(5)+1:id-148);g.items().neutral().librarySound(p);}
            case 154,155,156 -> g.items().bolts().libraryDonor(p,id==155?3:id==156?2:1);
            case 157,158 -> {if(!g.items().exoperso().cast(p,id==158))return false;}
            case 159 -> g.items().danmaku().queueExternal(p,hand,false);
            case 195,197,198 -> {g.items().acidCorruption().cast(p,id==195?1:id==197?3:4);LbcOverload.sound(p,p.position(),"minecraft:entity.zombie_villager.cure",1,2,0);}
            case 196 -> g.items().acidCorruption().sacrifice(p);
            case 237,238,239,240,241 -> {return g.items().bedrock().grimoire(p,switch(id){case 238->4;case 239->5;case 240->2;case 241->3;default->1;});}
            case 189 -> { } // Explicitly commented out in the source random-all selector; no direct book exists.
            case 242 -> LbcLibraryRandom.flyingWand(g,p);
            default -> throw new IllegalArgumentException("Unsupported library spell "+id);
        }
        return true;
    }
}
