package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/** Direct consumed elemental library160..187 and204..215, sharing native ordinary/Academy payloads. */
public final class LbcElementalGrimoires {
    public enum Kind {
        RUBY(160,"ruby","swarogia",1200,"c","ruby",1,2,3),
        TERRA(163,"terra","terra",1200,"c","terra",2),
        ELECTRO(165,"electro","sosuenerugi",900,"c","electro",2,3),
        AIR(167,"air","explode",1000,"c","air",2),
        ACID(171,"acid","lahazor",200,"c","acid",2,3),
        TELEPORT(173,"teleport","arkontas",2000,"b","teleport",2),
        STONE(176,"stone","stone",2000,"c","stone",2),
        FIRE(181,"fire","fire",200,"c","fire",2),
        BLIND(184,"blind","blind",300,"d","blind",2),
        RUBY_RAY(161,"ruby_ray","swarogia",1200,"c","libspell_swarogia_1","powerful",ChatFormatting.DARK_PURPLE,"libspell_swarogia_1.lore.1","libspell_swarogia_1.lore.2"),
        RUBY_HOLE(162,"ruby_hole","swarogia",1200,"b","libspell_swarogia_2","powerful",ChatFormatting.DARK_PURPLE,"libspell_swarogia_2.lore.1","libspell_swarogia_2.lore.2"),
        TERRA_FAST(164,"terra_fast","terra",1200,"a","libspell_terra_1","powerful",ChatFormatting.DARK_PURPLE,"libspell_terra_1.lore"),
        ELECTRO_RAY(166,"electro_ray","sosuenerugi",900,"d","libspell_sosuenerugi_1","universal",ChatFormatting.YELLOW,"libspell_sosuenerugi_1.lore"),
        AIR_BURST(168,"air_burst","explode",1000,"c","libspell_explode_1","universal",ChatFormatting.YELLOW,"libspell_explode_1.lore"),
        AIR_FAST(169,"air_fast","explode",1000,"a","libspell_explode_2","universal",ChatFormatting.YELLOW,"libspell_explode_2.lore"),
        AIR_HOMING(170,"air_homing","explode",1000,"b","libspell_explode_3","universal",ChatFormatting.YELLOW,"libspell_explode_3.lore"),
        ACID_BURNING(172,"acid_burning","lahazor",200,"b","libspell_lahazor_1","enhanced",ChatFormatting.GRAY,"libspell_lahazor_1.lore"),
        TELEPORT_INVERTED(174,"teleport_inverted","arkontas",2000,"b","libspell_arkontas_1","enhanced",ChatFormatting.GRAY,"libspell_arkontas_1.lore"),
        TELEPORT_CLOUD(175,"teleport_cloud","arkontas",2000,"b","libspell_arkontas_2","smart",ChatFormatting.AQUA,"libspell_arkontas_2.lore"),
        STONE_RAY(177,"stone_ray","stone",2000,"b","libspell_stone_1","universal",ChatFormatting.YELLOW,"libspell_stone_1.lore"),
        STONE_SPIKES(178,"stone_spikes","stone",2000,"b","libspell_stone_2","powerful",ChatFormatting.DARK_PURPLE,"libspell_stone_2.lore"),
        STONE_BRIDGE(179,"stone_bridge","stone",2000,"b","libspell_stone_3","smart",ChatFormatting.AQUA,"libspell_stone_3.lore"),
        STONE_SHIELD(180,"stone_shield","stone",2000,"b","libspell_stone_4","smart",ChatFormatting.AQUA,"libspell_stone_4.lore"),
        FIRE_BIG(182,"fire_big","fire",200,"b","libspell_fire_1","powerful",ChatFormatting.DARK_PURPLE,"libspell_fire_1.lore"),
        FIRE_RAY(183,"fire_ray","fire",200,"b","libspell_fire_2","universal",ChatFormatting.YELLOW,"libspell_fire_2.lore"),
        BLIND_GATLING(185,"blind_gatling","blind",300,"c","libspell_blind_1","enhanced",ChatFormatting.GRAY,"libspell_blind_1.lore"),
        BLIND_PHANTOM(186,"blind_phantom","blind",300,"d","libspell_blind_2","universal",ChatFormatting.YELLOW,"libspell_blind_2.lore"),
        BLIND_RAIN(187,"blind_rain","blind",300,"c","libspell_blind_3","smart",ChatFormatting.AQUA,"libspell_blind_3.lore"),
        EXAHOMING(204,"exahoming","homing",2000,"a","libspell_homing",null,null,"libspell_homing.lore"),
        EXAHOMING_CURSE(205,"exahoming_curse","homing",2000,"x","libspell_homing_1","smart",ChatFormatting.AQUA,"libspell_homing_1.lore"),
        EXAHOMING_DESTINY(206,"exahoming_destiny","homing",2000,"x","libspell_homing_2","unreal",ChatFormatting.RED,"libspell_homing_2.lore"),
        EXAHOMING_SISYPHUS(207,"exahoming_sisyphus","homing",2000,"x","libspell_homing_3","powerful",ChatFormatting.DARK_PURPLE,"libspell_homing_3.lore"),
        PSYCHOBLIND(208,"psychoblind","psychoblind",2000,"b","libspell_psychoblind",null,null,"libspell_psychoblind.lore"),
        PSYCHOBLIND_INVISIBLE(209,"psychoblind_invisible","psychoblind",2000,"b","libspell_psychoblind_1","enhanced",ChatFormatting.GRAY,"libspell_psychoblind_1.lore"),
        PSYCHOBLIND_RAY(210,"psychoblind_ray","psychoblind",2000,"a","libspell_psychoblind_2","smart",ChatFormatting.AQUA,"libspell_psychoblind_2.lore"),
        PSYCHOBLIND_SPRAY(211,"psychoblind_spray","psychoblind",2000,"s","libspell_psychoblind_3","universal",ChatFormatting.YELLOW,"libspell_psychoblind_3.lore"),
        BOLTBOLT(212,"boltbolt","boltbolt",2000,"a","libspell_boltbolt",null,null,"libspell_boltbolt.lore"),
        BOLTBOLT_METEOR(213,"boltbolt_meteor","boltbolt",2000,"a","libspell_boltbolt_1","smart",ChatFormatting.AQUA,"libspell_boltbolt_1.lore"),
        BOLTBOLT_WATERING(214,"boltbolt_watering","boltbolt",2000,"b","libspell_boltbolt_2","smart",ChatFormatting.AQUA,"libspell_boltbolt_2.lore"),
        BOLTBOLT_RACER(215,"boltbolt_racer","boltbolt",2000,"s","libspell_boltbolt_3","smart",ChatFormatting.AQUA,"libspell_boltbolt_3.lore"),
        ERECTO(216,"erecto","erecto",1000,"a","libspell_erecto",null,null,"libspell_erecto.lore"),
        ERECTO_RAY(217,"erecto_ray","erecto",1000,"a","libspell_erecto_1","universal",ChatFormatting.YELLOW,"libspell_erecto_1.lore"),
        ERECTO_SNIPER(218,"erecto_sniper","erecto",1000,"ss","libspell_erecto_2","powerful",ChatFormatting.DARK_PURPLE,"libspell_erecto_2.lore"),
        ERECTO_SPHERE(219,"erecto_sphere","erecto",1000,"b","libspell_erecto_3","enhanced",ChatFormatting.GRAY,"libspell_erecto_3.lore"),
        FIREBLAZE(220,"fireblaze","fireblaze",100,"d","libspell_fireblaze",null,null,"libspell_fireblaze.lore"),
        FIREBLAZE_KIUAS(221,"fireblaze_kiuas","fireblaze",100,"c","libspell_fireblaze_1","powerful",ChatFormatting.DARK_PURPLE,"libspell_fireblaze_1.lore"),
        FIREBLAZE_MEGA(222,"fireblaze_mega","fireblaze",100,"b","libspell_fireblaze_2","powerful",ChatFormatting.DARK_PURPLE,"libspell_fireblaze_2.lore"),
        FIREBLAZE_PILLARS(223,"fireblaze_pillars","fireblaze",100,"d","libspell_fireblaze_3","universal",ChatFormatting.YELLOW,"libspell_fireblaze_3.lore"),
        FIREBLAZE_WORLD(224,"fireblaze_world","fireblaze",100,"d","libspell_fireblaze_4","overhauled",ChatFormatting.GOLD,"libspell_fireblaze_4.lore"),
        STONEKIVI(233,"stonekivi","stonestone",2500,"s","libspell_stonestone",null,null,"libspell_stonestone.lore"),
        STONEKIVI_EARTH(234,"stonekivi_earth","stonestone",2500,"s","libspell_stonestone_1","powerful",ChatFormatting.DARK_PURPLE,"libspell_stonestone_1.lore"),
        STONEKIVI_SUN(235,"stonekivi_sun","stonestone",2500,"k","libspell_stonestone_2","unreal",ChatFormatting.RED,"libspell_stonestone_2.lore"),
        STONEKIVI_COMET(236,"stonekivi_comet","stonestone",2500,"ss","libspell_stonestone_3","overhauled",ChatFormatting.GOLD,"libspell_stonestone_3.lore");


        final int spellId,metadataMana;
        final String id,model,name,tier;
        final List<String> lore;
        final String modifier;
        final ChatFormatting modifierColor;
        final Item item;
        Kind(int spellId,String id,String model,int metadataMana,String tier,String loreStem,int... rows) {
            this.spellId=spellId;this.id=id+"_grimoire";this.model="lbc:spelllib/"+model;
            this.name="libspell_"+model;this.metadataMana=metadataMana;this.tier="lbc:tier_"+tier;
            this.lore=java.util.Arrays.stream(rows).mapToObj(row->"sauvojen_wand_"+loreStem+".lore."+row).toList();
            this.modifier=null;this.modifierColor=null;
            this.item=create(this);
        }
        Kind(int spellId,String id,String model,int metadataMana,String tier,String name,
             String modifier,ChatFormatting modifierColor,String... lore) {
            this.spellId=spellId;this.id=id+"_grimoire";this.model="lbc:spelllib/"+model;
            this.name=name;this.metadataMana=metadataMana;this.tier="lbc:tier_"+tier;
            this.lore=List.of(lore);this.modifier=modifier;this.modifierColor=modifierColor;
            this.item=create(this);
        }
    }

    private LbcElementalGrimoires() { }
    public static void register() { Kind.values(); }
    static List<Kind> ordinary() { return List.of(Kind.RUBY,Kind.TERRA,Kind.ELECTRO,Kind.AIR,Kind.ACID,Kind.TELEPORT,Kind.STONE,Kind.FIRE,Kind.BLIND); }
    static List<Kind> variants() { return java.util.Arrays.stream(Kind.values()).filter(kind->kind.modifier!=null&&kind.spellId<=187).toList(); }

    static List<Kind> advanced() { return java.util.Arrays.stream(Kind.values()).filter(kind->kind.spellId>=204&&kind.spellId<=215).toList(); }

    static List<Kind> shaping() { return java.util.Arrays.stream(Kind.values()).filter(kind->kind.spellId>=216&&kind.spellId<=224||kind.spellId>=233&&kind.spellId<=236).toList(); }

    private static Component text(String key,ChatFormatting color) {
        return Component.translatable(key).withStyle(color).withStyle(style->style.withItalic(false));
    }
    private static Item create(Kind kind) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+kind.id));
        var model=Identifier.parse(kind.model);
        var tag=new CompoundTag();
        for(var flag:List.of("transformable","gadget","libspell","unresolved_grimoire"))tag.putInt(flag,1);
        tag.putInt("mana",kind.metadataMana);tag.putInt("spellid",kind.spellId);
        var lore=new ArrayList<Component>();
        lore.add(text("type",ChatFormatting.AQUA).copy().append(text("libspell",ChatFormatting.AQUA)));
        lore.add(Component.empty());
        for(var row:kind.lore)lore.add(text(row,ChatFormatting.GOLD));
        // These three source library families use Legendary, independently of their tooltip tier.
        boolean legendary=kind.spellId>=204;
        var rarity=text("rarity",ChatFormatting.WHITE).copy().append(text(legendary?"legendary":"epic",legendary?ChatFormatting.RED:ChatFormatting.DARK_PURPLE));
        if(kind.modifier!=null)rarity.append(text(kind.modifier,kind.modifierColor));
        lore.add(rarity);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).stacksTo(99).overrideDescription(kind.name)
                .component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(kind.tier))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(tag))
                .component(DataComponents.CUSTOM_NAME,text(kind.name,ChatFormatting.WHITE))
                .component(DataComponents.LORE,new ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.SNOWBALL,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }

    static Kind kind(ItemStack stack) {
        if(stack.isEmpty())return null;
        for(var kind:Kind.values())if(stack.is(kind.item))return kind;
        var data=LbcFartus.data(stack);
        if(data.getIntOr("libspell",0)!=1)return null;
        int id=data.getIntOr("spellid",0);
        for(var kind:Kind.values())if(id==kind.spellId)return kind;
        return null;
    }

    /** Metadata mana belongs to library crafting; direct consumption has no mana price/admission. */
    static boolean use(GameController game,ServerPlayer player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);var kind=kind(stack);
        if(kind==null||player.isRemoved()||!player.isAlive()||game.stage()==GameController.Stage.ENDING
                ||!game.canUseGameplayItems(player)||player.level()!=game.maps().level()
                ||!player.level().hasChunkAt(BlockPos.containing(player.getEyePosition()))
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||game.items().actionsBlocked(player)||game.items().wands().inputTimeStopped(player)
                ||game.items().magic().suppressed(player)
                ||game.items().magic().suppressedAt(player.level(),player.getEyePosition()))return false;
        switch(kind) {
            case ERECTO,ERECTO_RAY,ERECTO_SNIPER,ERECTO_SPHERE -> game.items().erecto().castMode(player,hand,ItemStack.EMPTY,
                    switch(kind){case ERECTO_RAY -> 2;case ERECTO_SNIPER -> 4;case ERECTO_SPHERE -> 3;default -> 1;});
            case FIREBLAZE,FIREBLAZE_KIUAS,FIREBLAZE_MEGA,FIREBLAZE_PILLARS,FIREBLAZE_WORLD -> game.items().blaze().fireblaze(player,
                    switch(kind){case FIREBLAZE_KIUAS -> 4;case FIREBLAZE_MEGA -> 2;case FIREBLAZE_PILLARS -> 3;case FIREBLAZE_WORLD -> 1;default -> 0;});
            case STONEKIVI,STONEKIVI_EARTH,STONEKIVI_SUN,STONEKIVI_COMET -> {
                if(!game.items().stonekivi().ready())return false;
                game.items().stonekivi().castMode(player,hand,ItemStack.EMPTY,
                        switch(kind){case STONEKIVI_EARTH -> 2;case STONEKIVI_SUN -> 3;case STONEKIVI_COMET -> 4;default -> 1;});
            }
            case EXAHOMING -> game.items().homing().grimoire(player,1);
            case EXAHOMING_CURSE -> game.items().homing().grimoire(player,4);
            case EXAHOMING_DESTINY -> game.items().homing().grimoire(player,3);
            case EXAHOMING_SISYPHUS -> game.items().homing().grimoire(player,2);
            case PSYCHOBLIND -> game.items().psycho().psychoblind(player,1);
            case PSYCHOBLIND_INVISIBLE -> game.items().psycho().psychoblind(player,4);
            case PSYCHOBLIND_RAY -> game.items().psycho().psychoblind(player,2);
            case PSYCHOBLIND_SPRAY -> game.items().psycho().psychoblind(player,3);
            case BOLTBOLT,BOLTBOLT_METEOR,BOLTBOLT_WATERING,BOLTBOLT_RACER -> {
                int mode=switch(kind){case BOLTBOLT_METEOR -> 3;case BOLTBOLT_WATERING -> 4;case BOLTBOLT_RACER -> 2;default -> 1;};
                if(!game.items().bolts().grimoire(player,mode))return false;
            }
            case RUBY -> game.items().ruby().cast(player,0);
            case TERRA -> game.items().terra().cast(player);
            case ELECTRO -> game.items().wands().electroVolley(player);
            case AIR -> game.items().air().cast(player,hand,0,ItemStack.EMPTY);
            case ACID -> {
                game.items().clouds().ordinaryWandAcid(player);
                LbcOverload.sound(player,player.getEyePosition(),"minecraft:entity.zombie_villager.cure",1,2,0);
            }
            case TELEPORT -> game.items().teleport().cast(player,hand,0,ItemStack.EMPTY);
            case STONE -> game.items().stone().cast(player,0);
            case FIRE -> game.items().wands().fireVolley(player);
            case BLIND -> game.items().blind().cast(player,hand,0,ItemStack.EMPTY);
            case RUBY_RAY -> game.items().ruby().cast(player,3);
            case RUBY_HOLE -> game.items().ruby().cast(player,2);
            case TERRA_FAST -> game.items().terra().cast(player,LbcTerra.Kind.ACADEMY);
            case ELECTRO_RAY -> game.items().wands().electroRay(player);
            case AIR_BURST -> game.items().air().cast(player,hand,1,ItemStack.EMPTY);
            case AIR_FAST -> game.items().air().cast(player,hand,2,ItemStack.EMPTY);
            case AIR_HOMING -> game.items().air().cast(player,hand,3,ItemStack.EMPTY);
            case ACID_BURNING -> {
                game.items().clouds().academyWandAcid(player,true);
                LbcOverload.sound(player,player.getEyePosition(),"minecraft:entity.zombie_villager.cure",1,2,0);
            }
            case TELEPORT_INVERTED -> game.items().teleport().cast(player,hand,2,ItemStack.EMPTY);
            case TELEPORT_CLOUD -> game.items().teleport().cast(player,hand,3,ItemStack.EMPTY);
            case STONE_RAY -> game.items().stone().ray(player,false);
            case STONE_SPIKES -> game.items().stone().ray(player,true);
            case STONE_BRIDGE -> game.items().stone().bridge(player);
            case STONE_SHIELD -> game.items().stone().shield(player);
            case FIRE_BIG -> game.items().wands().bigFire(player);
            case FIRE_RAY -> game.items().wands().concentratedFire(player);
            case BLIND_GATLING -> game.items().blind().queueGatling(player);
            case BLIND_PHANTOM -> game.items().blind().cast(player,hand,2,ItemStack.EMPTY);
            case BLIND_RAIN -> game.items().blind().cast(player,hand,3,ItemStack.EMPTY);
        }
        player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL,.5f,.4f/(player.level().getRandom().nextFloat()*.4f+.8f));
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        stack.consume(1,player);
        return true;
    }
}
