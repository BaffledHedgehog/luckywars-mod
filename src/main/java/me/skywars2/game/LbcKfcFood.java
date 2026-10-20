package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.food.Foods;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Native KFC consumption; only eaters with an active finite gigaponos timer are ticked. */
public final class LbcKfcFood {
    public static final Item KFC_CHICK=registerFood();
    public static final Item GOVNO_EATEN=registerCookie();
    private static final CompoundTag FLAG=new CompoundTag();
    static { FLAG.putInt("kfc_chick",1); }
    private static final String[] MESSAGES={"yum_xp","yum_xp_2","yum_xp_3","yum_remove","yum_healing",
            "yum_strength","yum_scores","yum_attributes","yum_mana","yum_ponos","yum_blevota","yum_tasty",
            "yum_gigaponos","yum_gigaponos","yum_jesus","yum_win_button","yum_overfeed","yum_satan","yum_chick_alive","yum_yumyum"};
    // Exact source order, including the duplicate reverse_mana and excluding commented nexus_compass_cd.
    private static final String[] SOURCE_SCORE_ADDITIONS={
            "bow","jump1","Distance","RC_Raycast","coldown1","coldown8","coldown13",
            "coldown18","coldown18b","coldown16","coldown17","coldown25","bow_charge","nitro",
            "snowball","foodlvl","hplvl","totem","reactivefuel","sprint_timer","end_damage",
            "piss","laser_minigun_spread","MaxHealth","lookback","scared","attack_damage","simpleTimer",
            "shock","supplyFall","swordcharged1","swordcharging1","swordcharged2","swordcharging2","swordcharged3",
            "swordcharging3","swordcharged4","swordcharging4","swordcharged5","swordcharging5","swordcharged6","swordcharging6",
            "swordcharged7","swordcharging7","swordcharged8","luck_amount","falltimer","twitching","Lifetime",
            "coldownshish","coldownhohol","coldownMagnum","cyborgBossFly","divider_cd","lust_cd","lust_cd_small",
            "food_count","indolence_cd","sleep_time","Distance_const","dmg_dealt","lolidefecation","lucky_block_count",
            "rocket_sound","reactivefuelsub","reverse_mana","technomk2shield","nanobots_coldown","nanobots_tier","gens_type",
            "greed_prestige","x","y","z","nomagic","gatling_fovos","november",
            "ray","acid_rifle","screamray","laser_minigun_spin","laser_minigun_cd","laser_minigun_sound","blaze",
            "coldownoraora","fevgalea_charge","h","s","v","hueDelta","h+",
            "color","const.rgb","target","reverse_mana","void_resistance","dimensional_shifting","all_seeing_eye",
            "living_legend","skill_issue","coldowntimestand","coldownlightning","cd_helicopter","metal_divider_cd","nexus_roadgun_cd",
            "nexus_corrupter_cd","MagnumNyauwu","speed","danmaku","danmaku_score","pulsar_cd","adv_game_minigun_shooted"
    };
    private final GameController game;
    private final Map<UUID,Integer> gigaponos=new HashMap<>();
    LbcKfcFood(GameController game) { this.game=game; }
    static void register() { }
    private static Item registerCookie() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:govno_eaten"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties().setId(key).overrideDescription("shit_4")
                        .food(Foods.COOKIE,Consumables.DEFAULT_FOOD)
                        .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:shit")),Items.COOKIE,true));
    }
    private static Item registerFood() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:kfc_chick"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties().setId(key).overrideDescription("kfc_chick")
                        .food(Foods.COOKED_CHICKEN,Consumables.DEFAULT_FOOD)
                        .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:kfc_yum")),Items.COOKED_CHICKEN,true));
    }
    public static boolean isKfcFood(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && (stack.is(KFC_CHICK) || data!=null && data.matchedBy(FLAG));
    }
    private boolean active(ServerPlayer player) {
        return game.playing() && game.stage()!=GameController.Stage.ENDING && game.canUseGameplayItems(player)
                && player.isAlive() && !player.isRemoved() && player.level()==game.maps().level();
    }
    /** Called before native stack consumption and normal cooked chicken nutrition. */
    public void consumed(ServerPlayer player,ItemStack stack) {
        if(!active(player)) return;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(stack.is(GOVNO_EATEN) || data.getIntOr("eat_shit",0)==1) {
            // This edible is reachable from KFC's random drops and reproduces itself at the eyes.
            for(var result:game.lbc().rollTable(player.level(),player.position(),player,"lbc:govno_eaten")) {
                var item=new ItemEntity(player.level(),player.getX(),player.getEyeY(),player.getZ(),result);
                item.setDefaultPickUpDelay(); player.level().addFreshEntity(item);
            }
            for(float pitch:new float[]{0,1,2}) sound(player,"minecraft:lbcsounds.vomiting",1,pitch);
            sound(player,"minecraft:lbcsounds.shitting_1",1,1);
            if(game.config().lbcSkills && game.players().get(player.getUUID()).lbcSkill==9) {
                effect(player,MobEffects.REGENERATION,10,2,false);
                effect(player,MobEffects.INSTANT_HEALTH,1,0,false);
            }
            return;
        }
        if(!isKfcFood(stack)) return;
        applyOutcome(player,player.getRandom().nextInt(20)+1);
        game.items().mana().award(player,"lbc:true_advancements/shooter/kfc_gun");
    }
    /** Deterministic outcome seam; randomness remains only in the original random-slot branch. */
    void applyOutcome(ServerPlayer p,int outcome) {
        if(outcome<1 || outcome>20) throw new IllegalArgumentException("KFC outcome must be 1..20");
        if(!active(p)) return;
        var message=Component.translatable(MESSAGES[outcome-1]);
        if(outcome==14) message.withStyle(ChatFormatting.DARK_RED);
        p.sendSystemMessage(message);
        switch(outcome) {
            case 1 -> p.giveExperienceLevels(1);
            case 2 -> p.giveExperienceLevels(5);
            case 3 -> p.giveExperienceLevels(15);
            case 4 -> { sound(p,"minecraft:lbcsounds.shitting_1",1,2); replaceRandomSlot(p); }
            case 5 -> effect(p,MobEffects.INSTANT_HEALTH,1,99,false);
            case 6 -> { effect(p,MobEffects.STRENGTH,20,3,false); sound(p,"minecraft:entity.lightning_bolt.thunder",1,1); }
            case 7 -> addExactSourceScores(p);
            case 8 -> improveAttributes(p);
            case 9 -> {
                var mana=game.items().mana(); mana.add(p,1);
                mana.baseRegen(p,mana.baseRegen(p)+1); mana.maximum(p,mana.maximum(p)+1);
            }
            case 10 -> {
                sound(p,"minecraft:lbcsounds.shitting_1",1,0); effect(p,MobEffects.HUNGER,5,255,false);
                particle(p,ParticleTypes.SNEEZE,1,4000,.4,.4,.4,.4);
            }
            case 11 -> {
                sound(p,"minecraft:lbcsounds.vomiting",1,1); effect(p,MobEffects.NAUSEA,20,4,false);
                particle(p,ParticleTypes.FALLING_HONEY,1.62,100,.3,.3,.3,1);
            }
            case 12 -> effect(p,MobEffects.SATURATION,1,100,false);
            case 13 -> {
                sound(p,"minecraft:lbcsounds.shitting_1",1,1); effect(p,MobEffects.HUNGER,10,255,true);
                if(game.config().lbcSkills && game.players().get(p.getUUID()).lbcSkill==9) mega(p);
                drop(p,"lbc:govno_lot");
            }
            case 14 -> {
                sound(p,"minecraft:lbcsounds.shitting_2",20,1);
                effect(p,MobEffects.HUNGER,20,255,true); effect(p,MobEffects.LEVITATION,20,10,false);
                if(!game.items().magic().rejectEffect(p,MobEffects.HUNGER)) setGigaponos(p,gigaponosRemaining(p)+400);
            }
            case 15 -> {
                effect(p,MobEffects.LEVITATION,10,0,true); effect(p,MobEffects.GLOWING,10,0,true);
                sound(p,"minecraft:lbcsounds.angelic",1,1);
                p.level().dimensionType().defaultClock().ifPresent(clock -> game.server().clockManager().moveToTimeMarker(clock,ClockTimeMarkers.NOON));
            }
            case 16 -> drop(p,"lbc:win_button");
            case 17 -> { effect(p,MobEffects.SLOWNESS,30,3,true); effect(p,MobEffects.SATURATION,600,20,true); }
            case 18 -> { sound(p,"minecraft:entity.illusioner.cast_spell",1,1); drop(p,"lbc:penis_cultus"); }
            case 19 -> {
                sound(p,"minecraft:entity.chicken.hurt",1,1); sound(p,"minecraft:entity.chicken.ambient",1,1);
                sound(p,"minecraft:entity.chicken.ambient",1,2); sound(p,"minecraft:entity.chicken.ambient",1,0);
                var chicken=new Chicken(EntityTypes.CHICKEN,p.level()); chicken.setPos(p.getX(),p.getY()+1.62,p.getZ());
                p.level().addFreshEntity(chicken);
            }
            case 20 -> { }
            default -> throw new IllegalStateException();
        }
    }
    /** Source ledger plus the current owners of ported player counters. */
    private void addExactSourceScores(ServerPlayer p) {
        var mana=game.items().mana();
        for(String name:SOURCE_SCORE_ADDITIONS) {
            switch(name) {
                case "reverse_mana" -> mana.reverse(p,mana.reverse(p)+1);
                case "nomagic" -> mana.noMagic(p,mana.noMagic(p)+1);
                case "blaze" -> game.items().blaze().addHeadCharges(p,1);
                default -> mana.score(p,name).add(1);
            }
        }
        game.items().psycho().afflict(p,p,1,1,1,1);
        game.items().psycho().addRayTicks(p,1);
        game.items().electro().shock(p,p,1);
        game.items().electro().addPlayerCounters(p,1);
        game.items().shooters().addPlayerCounters(p);
        game.items().terra().addPlayerCounter(p);
        p.entityTags().add("have_lucky_effects");
    }
    private void effect(ServerPlayer p,Holder<MobEffect> type,int seconds,int amplifier,boolean hidden) {
        // Matches EffectCommands: instantaneous durations use ticks, all other durations use seconds.
        p.addEffect(new MobEffectInstance(type,type.value().isInstantaneous()?seconds:seconds*20,amplifier,false,!hidden),p);
    }
    private void mega(ServerPlayer p) {
        effect(p,MobEffects.REGENERATION,10,10,false); effect(p,MobEffects.INSTANT_HEALTH,10,10,false);
        effect(p,MobEffects.FIRE_RESISTANCE,10,0,false); effect(p,MobEffects.HASTE,10,10,false);
        effect(p,MobEffects.RESISTANCE,10,4,false); effect(p,MobEffects.STRENGTH,10,2,false);
        effect(p,MobEffects.SPEED,10,9,false);
    }
    private void replaceRandomSlot(ServerPlayer p) {
        int roll=p.getRandom().nextInt(38);
        var stacks=game.lbc().rollTable(p.level(),p.position(),p,"lbc:govno_lot_2");
        var replacement=stacks.isEmpty()?ItemStack.EMPTY:stacks.getFirst();
        if(roll<27) p.getInventory().setItem(roll+9,replacement);
        else if(roll<36) p.getInventory().setItem(roll-27,replacement);
        else p.setItemSlot(roll==36?EquipmentSlot.HEAD:EquipmentSlot.OFFHAND,replacement);
        p.inventoryMenu.broadcastChanges();
    }
    private void drop(ServerPlayer p,String table) {
        for(var stack:game.lbc().rollTable(p.level(),p.position(),p,table)) {
            var item=new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),stack);
            item.setDefaultPickUpDelay(); p.level().addFreshEntity(item);
        }
    }
    private void improveAttributes(ServerPlayer p) {
        attribute(p,Attributes.ARMOR,1,1); attribute(p,Attributes.ARMOR_TOUGHNESS,1,1);
        attribute(p,Attributes.ATTACK_DAMAGE,1,1); attribute(p,Attributes.ATTACK_SPEED,5,1);
        attribute(p,Attributes.KNOCKBACK_RESISTANCE,10,1); attribute(p,Attributes.LUCK,1,1);
        attribute(p,Attributes.MAX_HEALTH,1,1); attribute(p,Attributes.MOVEMENT_SPEED,100000,2000);
        attributeFeedback(p,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.IRON_BLOCK.defaultBlockState()),100,0,"lbc:armor_toughnessattr");
        attributeFeedback(p,ParticleTypes.SWEEP_ATTACK,100,0,"lbc:attack_damageattr");
        attributeFeedback(p,ParticleTypes.CRIT,100,0,"lbc:attack_speedattr");
        attributeFeedback(p,ParticleTypes.SMOKE,100,0,"lbc:knockback_resistanceattr");
        attributeFeedback(p,ParticleTypes.HAPPY_VILLAGER,100,0,"lbc:luckattr");
        attributeFeedback(p,ParticleTypes.END_ROD,10,1,"lbc:movement_speedattr");
    }
    private static void attribute(ServerPlayer p,Holder<Attribute> key,int scale,int increase) {
        var attribute=p.getAttribute(key);
        if(attribute!=null) attribute.setBaseValue((Math.floor(attribute.getBaseValue()*scale)+increase)/(double)scale);
    }
    private void attributeFeedback(ServerPlayer p,ParticleOptions type,int count,double speed,String revoke) {
        sound(p,"minecraft:entity.player.levelup",1,1); particle(p,type,1,count,.5,.5,.5,speed);
        var advancement=game.server().getAdvancements().get(Identifier.parse(revoke));
        if(advancement!=null) for(String criterion:advancement.value().criteria().keySet()) p.getAdvancements().revoke(advancement,criterion);
    }
    private static void particle(ServerPlayer p,ParticleOptions type,double height,int count,double x,double y,double z,double speed) {
        p.level().sendParticles(type,p.getX(),p.getY()+height,p.getZ(),count,x,y,z,speed);
    }
    private static void sound(ServerPlayer p,String id,float volume,float pitch) { LbcOverload.sound(p,id,volume,pitch,0); }
    int gigaponosRemaining(ServerPlayer p) { return gigaponos.getOrDefault(p.getUUID(),0); }
    private void setGigaponos(ServerPlayer p,int ticks) {
        if(ticks>0) gigaponos.put(p.getUUID(),ticks); else gigaponos.remove(p.getUUID());
        game.items().mana().score(p,"gigaponos").set(ticks);
    }
    public void tick() {
        for(var entry:List.copyOf(gigaponos.entrySet())) {
            var p=game.server().getPlayerList().getPlayer(entry.getKey());
            if(p==null) { gigaponos.remove(entry.getKey()); continue; }
            if(!active(p) || game.items().magic().rejectEffect(p,MobEffects.HUNGER)) { clear(p); continue; }
            int remaining=entry.getValue();
            particle(p,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DIRT.defaultBlockState()),.7,5,.1,.1,.1,0);
            particle(p,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState()),.7,5,.1,.1,.1,0);
            LbcOverload.sound(p,p.position(),"minecraft:lbcsounds.rocket_power",.3f,1,50,.3f);
            drop(p,"lbc:govno");
            if(remaining==300) { sound(p,"minecraft:lbcsounds.ponos",15,1); sound(p,"minecraft:lbcsounds.shitting_2",20,.5f); }
            if(remaining==200) sound(p,"minecraft:lbcsounds.shitting_2",20,1.5f);
            if(remaining==350 || remaining==100) sound(p,"minecraft:lbcsounds.shitting_1",20,1);
            if(remaining==250) sound(p,"minecraft:lbcsounds.shitting_1",20,1.5f);
            if(remaining==150) sound(p,"minecraft:lbcsounds.shitting_1",20,.5f);
            setGigaponos(p,remaining-1);
        }
    }
    public void clear(ServerPlayer p) { if(gigaponos.containsKey(p.getUUID())) setGigaponos(p,0); }
    public void forget(ServerPlayer p) { clear(p); }
    public void stop() {
        for(UUID id:List.copyOf(gigaponos.keySet())) {
            var p=game.server().getPlayerList().getPlayer(id); if(p!=null) clear(p);
        }
        gigaponos.clear();
    }
}
