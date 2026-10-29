package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native item callbacks and temporary item state, independent of the selected kit. */
public final class LbcItems {
    public static final Item ROCKET_BOOTS = registerRocketBoots();
    public static final Item ICE_SWORD = registerIceSword();
    public static final Item TERMIT = registerChemicalPotion("skywars2:termit", "termit");
    public static final Item SULFUR = registerChemicalPotion("skywars2:sulfur", "sulfur");
    public static final Item PHOSPHIN = registerChemicalPotion("skywars2:phosphin", "phosphin");
    public static final Item GREMUCH = registerChemicalPotion("skywars2:gremuch", "gremuch");

    /** Native superclass сохраняет бросок, расход, статистику, звук и создание lingering cloud. */
    private static final class ChemicalPotionItem extends LingeringPotionItem implements PolymerItem {
        ChemicalPotionItem(Properties properties) { super(properties); }
        @Override public Item getPolymerItem(ItemStack stack, PacketContext context) { return Items.LINGERING_POTION; }
    }

    private static Item registerChemicalPotion(String id, String nameKey) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
        var properties = new Item.Properties().setId(key).stacksTo(1).overrideDescription(nameKey)
                .component(DataComponents.ITEM_MODEL, Identifier.parse("minecraft:lingering_potion"))
                .component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .component(DataComponents.POTION_DURATION_SCALE, 0.25f);
        return Registry.register(BuiltInRegistries.ITEM, key, new ChemicalPotionItem(properties));
    }

    public static boolean isChemicalPotion(ItemStack stack) {
        if (stack.getItem() instanceof ChemicalPotionItem) return true;
        return chemicalAmplifier(stack.get(DataComponents.POTION_CONTENTS)) != 0;
    }

    public static int chemicalAmplifier(PotionContents contents) {
        if (contents != null) for (var effect : contents.getAllEffects()) {
            if (effect.is(MobEffects.RAID_OMEN) && effect.getAmplifier() >= 234 && effect.getAmplifier() <= 237)
                return effect.getAmplifier();
        }
        return 0;
    }

    private static Item registerRocketBoots() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:rocket_boots"));
        // Базовые свойства прежних кольчужных ботинок; точное оформление выдачи остаётся в исходной loot table.
        var properties = new Item.Properties().setId(key)
                .humanoidArmor(ArmorMaterials.CHAINMAIL, ArmorType.BOOTS).rarity(Rarity.UNCOMMON)
                .overrideDescription("lbckit.21")
                .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:armors/rocket_boots"));
        // Третий аргумент обязателен: двухаргументный конструктор SimplePolymerItem скрывает custom item_model.
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, Items.CHAINMAIL_BOOTS, true));
    }

    private static Item registerIceSword() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:ice_sword"));
        // Обычные свойства железного меча остаются нативными; имя/lore выдаёт исходная loot table.
        var properties = new Item.Properties().setId(key).sword(ToolMaterial.IRON, 3, -2.4f)
                .overrideDescription("ice_sword")
                .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:icesword"));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, Items.IRON_SWORD, true));
    }

    public static boolean isIceSword(ItemStack stack) {
        // Перенесённая способность узнаётся по custom_data и сохраняет тип/оформление получателя.
        if (stack.is(ICE_SWORD)) return true;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.matchedBy(ICE_SWORD_DATA);
    }

    /** Мечи используют свои триггеры; немагическая киберкатана работает и под антимагией. */
    public void damaged(LivingEntity victim, DamageSource source) {
        witchPassives.damaged(victim,source);
        shishEncounter.damaged(victim,source);shishEncounter.incoming(victim);
        armorSets.phantom().damaged(victim,source);
        armorSets.nyauwu().damaged(victim,source);
        stand.onAcceptedDamage(victim,source);
        summons.damaged(victim,source);
        syringe.damaged(victim,source);
        specialTools.melee(victim,source);
        shooters.damaged(victim,source);
        sauvojen.meditation().damaged(victim,source);
        fevgaleaStar.damaged(victim,source);
        chargedSwords.damaged(victim,source);
        karaSlabs.damaged(victim,source);
        classicMelee.damaged(victim,source);
        metalPipe.damaged(victim,source);
        devilSword.damaged(victim,source);
        pendingCombat.damaged(victim,source);
        pendingSwords.damaged(victim,source);
        incomingArtifacts.damaged(victim,source);
        reactiveArtifacts.damaged(victim,source);
        if (!game.playing() && !debugItemUseWindow() || !inArena(victim) || LbcDamage.isArrowDamage(source)
                || !(source.getEntity() instanceof ServerPlayer owner) || !LbcDamage.isMeleeAttack(source,owner) || !active(owner)
                || !owner.isAlive() || !inArena(owner)) return;
        var held = owner.getMainHandItem();
        boolean iceSword = isIceSword(held), terraSword = LbcTerra.isSword(held), katana=LbcElectro.isAnyKatana(held);
        // Дешёвый отбор предмета предшествует пространственной проверке подавления магии.
        if (!iceSword && !terraSword && !katana) return;
        if (katana) { electro.damaged(owner, source); return; }
        if (magic.suppressed(owner)) return;
        if (iceSword) frost.hitTargets(owner);
        if (terraSword) terra.damaged(owner, source);
    }

    /** Загружаем регистрацию до freeze реестров и чтения loot tables, а не при первом обращении к предмету. */
    public static void register() { EnchantmentRules.register(); LbcSkills.register(); LbcClouds.register(); LbcPrism.register(); LbcAcidCorruption.register(); LbcIceWand.register(); LbcVoidArcanum.register(); LbcDefensiveArcanums.register(); LbcBabylon.register(); LbcFartus.register(); LbcHalloweenWind.register(); LbcSoulhunter.register(); LbcSoulless.register(); LbcDanmaku.register(); LbcElementalGrimoires.register(); LbcLibrarySpells.register(); LbcSummonerItems.register(); LbcSyringe.register(); LbcCommunism.register(); LbcSpecialTools.register(); LbcSheep.register(); LbcHedgehogItems.register(); LbcWtf.register(); LbcIceMaterials.register(); LbcCorruptedGlyphs.register(); LbcTrinkets.register(); LbcFrost.register(); LbcMagic.register(); LbcMana.register(); LbcWands.register(); LbcLifestealer.register(); LbcMobgun.register(); LbcTerra.register(); LbcFevgaleaStar.register(); LbcChargedSwords.register(); LbcKaraSlabs.register(); LbcShooters.register(); LbcLaserMinigun.register(); LbcAk47.register(); LbcVexSword.register(); LbcSpecialEggs.register(); LbcHohlopitek.register(); LbcMagnum.register(); LbcGovnomet.register(); LbcStand.register(); LbcKfcFood.register(); LbcKfcRewards.register(); LbcReforgeMachineItem.register(); LbcHelicopterTriggerItem.register(); LbcChestDropItem.register(); LbcTntDropItem.register(); LbcCreeperDropItem.register(); LbcCageDropItem.register(); LbcHoleDropItem.register(); LbcCorrosionDropItem.register(); LbcZombieTntDropItem.register(); LbcFallingNetheriteDropItem.register(); LbcHomeItems.register(); LbcScareDropItem.register(); LbcLuckyPigDropItem.register(); LbcLevitationDropItem.register(); LbcLibraryDropItem.register(); LbcBeeDropItem.register(); LbcOpAnvilDropItem.register(); LbcSlabDropItem.register(); LbcFeedbackDropItem.register(); LbcCreatureDropItem.register(); LbcPotionSpawnerDropItem.register(); LbcIslandDropItem.register(); LbcDroneDropItem.register(); LbcAntDropItem.register(); LbcElectro.register(); LbcExasperato.register(); LbcExoperso.register(); LbcStone.register(); LbcBlind.register(); LbcPsycho.register(); LbcAir.register(); LbcTeleport.register(); LbcRuby.register(); LbcBolts.register(); LbcAcademy.register(); LbcTanner.register(); LbcKiuas.register(); }

    private static final CompoundTag BROKEN_TOTEM = new CompoundTag();
    private static final CompoundTag DYNAMITE = new CompoundTag();
    private static final CompoundTag ICE_SWORD_DATA = new CompoundTag();
    private record NanoAttributes(double maxHealth, double armor, double toughness) {}
    static {
        BROKEN_TOTEM.putInt("broken_totem", 1);
        DYNAMITE.putInt("special", 1);
        DYNAMITE.putInt("dynamite", 1);
        ICE_SWORD_DATA.putInt("icesword", 1);
    }
    private record Rotation(int yaw, int pitch) {}
    private static final Rotation ZERO_ROTATION = new Rotation(0, 0);
    private static final class TurnCurse {
        final ServerPlayer owner;
        int remaining;
        TurnCurse(ServerPlayer owner) { this.owner = owner; }
    }
    private final GameController game;
    private final LbcClouds clouds;
    private final LbcTrinkets trinkets;
    private final LbcFrost frost;
    private final LbcMagic magic;
    private final LbcMana mana;
    private final LbcTerra terra;
    private final LbcWands wands;
    private final LbcPrism prism;
    private final LbcAwakenedAcid awakenedAcid;
    private final LbcErecto erecto;
    private final LbcDefensiveArcanums defensiveArcanums;
    private final LbcBabylon babylon;
    private final LbcFartus fartus;
    private final LbcHalloweenWind halloweenWind;
    private final LbcSoulhunter soulhunter;
    private final LbcSoulless soulless;
    private final LbcDanmaku danmaku;
    private final LbcSauvojen sauvojen;
    private final LbcFevgaleaStar fevgaleaStar;
    private final LbcChargedSwords chargedSwords;
    private final LbcKaraSlabs karaSlabs;
    private final LbcClassicMelee classicMelee;
    private final LbcDevilSword devilSword;
    private final LbcPendingCombat pendingCombat;
    private final LbcPendingSwords pendingSwords;
    private final LbcIncomingArtifacts incomingArtifacts;
    private final LbcReactiveArtifacts reactiveArtifacts;
    private final LbcJumpArtifacts jumpArtifacts;
    private final LbcTraveller traveller;
    private final LbcPopit popit;
    private final LbcOrdinaryAccessories ordinaryAccessories;
    private final LbcArmorSets armorSets;
    private final LbcChaosTraps chaosTraps;
    private final LbcSpellSource spellSources;
    private final LbcApocalypseEvents apocalypseEvents;
    private final LbcApocalypseActors apocalypseActors;
    private final LbcShishEncounter shishEncounter;
    private final LbcWitchPassives witchPassives;
    private final LbcChaosTrapActors chaosTrapActors;
    private final LbcSauvojenBoss sauvojenBoss;
    private final LbcInfinity infinity;
    private final LbcAimbobRewards aimbobRewards;
    private final LbcMagicBean magicBean;
    private final LbcAttributeFood attributeFood;
    private final LbcEdibleSpells edibleSpells;
    private final LbcFoodStatuses foodStatuses;
    private final LbcNukeEvent nukeEvent;
    private final LbcNamedFoods namedFoods;
    private final LbcFoodLifecycle foodLifecycle;
    private final LbcOreshek oreshek;
    private final LbcLegacyEffects legacyEffects;
    private final OwnedEffectLayers ownedEffects=new OwnedEffectLayers();
    private final LbcShulkerArmor shulkerArmor;
    private final LbcInventoryHazards inventoryHazards;
    private final LbcCannedFood cannedFood;
    private final LbcDeathCompass deathCompass;
    private final LbcMalevich malevich;
    private final LbcMetalPipe metalPipe;
    private final LbcMoiseiBoots moiseiBoots;
    private final LbcRegenerationBelt regenerationBelt;
    private final LbcQuiver quiver;
    private final LbcSheep sheep;
    private final LbcHedgehog hedgehog;
    private final LbcGodHedgehog godHedgehog;
    private final LbcHedgehogSpikes hedgehogSpikes;
    private final LbcHedgehogDimensions hedgehogDimensions;
    private final LbcWtf wtf;
    private final LbcBabylonEffects babylonEffects;
    private final LbcVoidArcanum voidArcanum;
    private final LbcIceWand iceWand;
    private final LbcIceMaterials iceMaterials;
    private final LbcAcidCorruption acidCorruption;
    private final LbcCorruptedGlyphs corruptedGlyphs;
    private final LbcGaycasterAcademy gaycasterAcademy;
    private final LbcAdvancedSnow advancedSnow;
    private final LbcEnderWiki enderWiki;
    private final LbcLibraryRandom libraryRandom;
    private final LbcNeutral neutral;
    private final LbcNyauwuCrafting nyauwuCrafting;
    private final LbcBloodConfuse bloodConfuse;
    private final LbcEvokerBass evokerBass;
    private final LbcLifestealer lifestealer;
    private final LbcMobgun mobgun;
    private final LbcStonekivi stonekivi;
    private final LbcTanner tanner;
    private final LbcKiuas kiuas;
    private final LbcBlaze blaze;
    private final LbcPsycho psycho;
    private final LbcSummons summons;
    private final LbcSyringe syringe;
    private final LbcCommunism communism;
    private final LbcSpecialTools specialTools;
    private final LbcTotems totems;
    private final LbcHoming homing;
    private final LbcBolts bolts;
    private final LbcLightningAcquisition lightningAcquisition;
    private final LbcShooters shooters;
    private final LbcLaserMinigun laserMinigun;
    private final LbcAk47 ak47;
    private final LbcVexSword vexSword;
    private final LbcMagnum magnum;
    private final LbcRifleZoom rifleZoom;
    private final LbcGovnomet govnomet;
    private final LbcStand stand;
    private final LbcTimeStop timeStop;
    private final NuclearVisuals nuclearVisuals;
    private final BlackHoleVisuals blackHoleVisuals;
    private final LbcKfcFood kfcFood;
    private final LbcKfcRewards kfcRewards;
    private final LbcHelicopterEncounter helicopterEncounter;
    private final LbcSpecialDrops specialDrops;
    private final LbcLuckyPigs luckyPigs;
    private final LbcEnzanDrop enzanDrop;
    private final LbcLibraryDrop libraryDrop;
    private final LbcBeeDrop beeDrops;
    private final LbcOpAnvilDrop opAnvilDrops;
    private final LbcSlabDrops slabDrops;
    private final LbcFeedbackDrops feedbackDrops;
    private final LbcCreatureDrops creatureDrops;
    private final LbcPotionSpawnerDrops potionSpawnerDrops;
    private final LbcIslandDrops islandDrops;
    private final LbcPortalDrops portalDrops;
    private final LbcLuckyRoomDrops luckyRoomDrops;
    private final LbcPisaDrops pisaDrops;
    private final LbcAntigravity antigravity;
    private final LbcUltraponos ultraponos;
    private final LbcReactive reactive;
    private final LbcTransformator transformator;
    private final LbcStalin stalin;
    private final LbcSkyHazards skyHazards;
    private final LbcDroneDrops droneDrops;
    private final LbcHohlopitek hohlopitek;
    private final LbcAntDrops antDrops;
    private final LbcTntDrops tntDrops;
    private final LbcCreeperDrops creeperDrops;
    private final LbcCageDrops cageDrops;
    private final LbcHoleDrops holeDrops;
    private final LbcCorrosionDrops corrosionDrops;
    private final LbcZombieTntDrops zombieTntDrops;
    private final LbcFallingNetheriteDrops fallingNetheriteDrops;
    private final LbcHomes homes;
    private final LbcElectro electro;
    private final ProjectileLifetimes lifetimes;
    private final LbcExasperato exasperato;
    private final LbcExoperso exoperso;
    private final LbcStone stone;
    private final LbcBedrock bedrock;
    private final LbcBlind blind;
    private final LbcAir air;
    private final LbcTeleport teleport;
    private final LbcRuby ruby;
    private final LbcConsuming consuming;
    private final LbcDuplicating duplicating;
    private final LbcAcademy academy;
    private final LbcIndirectKills indirectKills;
    private record HudState(UUID source,Component content,int sentAt) { }
    private final Map<UUID,HudState> hudStates=new HashMap<>();
    private String entityEpochTag = newEntityEpochTag();
    private final Map<UUID, TurnCurse> turnCurses = new HashMap<>();
    // Source rotation storage survives curse expiry/death; bound its lifetime to this match.
    private final Map<UUID, Rotation> rotations = new HashMap<>();
    private final ArrayList<TurnCurse> tickBatch = new ArrayList<>();
    private final Set<Snowball> pendingThrows = new LinkedHashSet<>();
    private final Set<PrimedTnt> dynamites = new LinkedHashSet<>();
    private final Set<Marker> nanoTraps = new LinkedHashSet<>();
    private final Map<UUID, NanoAttributes> originalNanoAttributes = new HashMap<>();
    private final ArrayList<Entity> entityBatch = new ArrayList<>();
    private final ArrayList<Marker> markerBatch = new ArrayList<>();
    private final Set<Marker> nearbyTraps = new LinkedHashSet<>();
    private static final class RocketState {
        final ServerPlayer owner;
        int jumps = 3;
        int nextCharge;
        RocketState(ServerPlayer owner) { this.owner = owner; }
    }
    private final Map<UUID, RocketState> rockets = new HashMap<>();
    private final Set<RocketState> rechargingRockets = new LinkedHashSet<>();
    private final ArrayList<RocketState> rocketBatch = new ArrayList<>();
    private final Set<ItemEntity> rocketCraftDrops = new LinkedHashSet<>();

    /** Сохраняем распознавание старого Paper PDC; обычный glider не становится ракетной способностью. */
    public static boolean isRocketBoots(ItemStack stack) {
        if (stack.is(ROCKET_BOOTS)) return true;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (stack.isEmpty() || data == null) return false;
        CompoundTag tag = data.copyTag();
        if (tag.getIntOr("explosive_feets", 0) == 1) return true;
        CompoundTag pdc = tag.getCompoundOrEmpty("PublicBukkitValues");
        return rocketPdc(pdc, "lws:item", "lws:type", "lws:additionals", "lws:glider")
                || rocketPdc(pdc, "luckywars:item", "luckywars:type", "luckywars:additionals", "luckywars:glider")
                || rocketPdc(pdc, "lucky_wars:item", "lucky_wars:type", "lucky_wars:additionals", "lucky_wars:glider");
    }

    private static boolean rocketPdc(CompoundTag pdc, String item, String type, String additional, String glider) {
        if (pdc.getCompound(glider).isPresent()) return true;
        CompoundTag nested = pdc.getCompoundOrEmpty(item);
        if (rocketType(nested.getStringOr(type, ""))) return true;
        for (String key : List.of(additional, "minecraft:additionals")) {
            if (rocketType(nested.getStringOr(key, ""))) return true;
            var values = nested.getListOrEmpty(key);
            for (int index = 0; index < values.size(); index++) {
                if (rocketType(values.getStringOr(index, ""))) return true;
            }
        }
        return false;
    }

    private static boolean rocketType(String type) {
        return type.equalsIgnoreCase("rocket_armor") || type.equalsIgnoreCase("rocketarmor") || type.equalsIgnoreCase("rocket");
    }

    /** onEquipItem бывает вызван до записи слота: изменяем переданный новый стек, не читаем старые ноги. */
    public void rocketEquipment(ServerPlayer player, ItemStack boots) {
        if (!active(player) || !isRocketBoots(boots)) { resetRocket(player); return; }
        RocketState state = rockets.computeIfAbsent(player.getUUID(), ignored -> new RocketState(player));
        syncRocketGlider(boots, state.jumps > 0);
        rocketHud(state);
    }

    private static void syncRocketGlider(ItemStack boots, boolean available) {
        if (available && !boots.has(DataComponents.GLIDER)) boots.set(DataComponents.GLIDER, Unit.INSTANCE);
        else if (!available && boots.has(DataComponents.GLIDER)) boots.remove(DataComponents.GLIDER);
    }

    /** Вызывается перед native startFallFlying, когда vanilla уже проверила воду, землю и levitation. */
    public boolean rocketGlide(ServerPlayer player) {
        if (game.isObserver(player)) return true;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!isRocketBoots(boots)) return false;
        boolean elytra = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        if (!active(player)) return !elytra;
        RocketState state = rockets.computeIfAbsent(player.getUUID(), ignored -> new RocketState(player));
        // Native mechanism beacons use a sphere; legacy markers retain the Paper cube check.
        boolean stopped = magic.mechanismSuppressedAt(player.level(),player.position())
                || !player.level().getEntitiesOfClass(Marker.class,
                new AABB(player.position(), player.position()).inflate(20), marker -> marker.entityTags().contains("stopper")).isEmpty();
        if (stopped || state.jumps == 0) return !elytra;
        state.jumps--;
        if (state.nextCharge == 0) {
            // В исходнике сначала decrement до нуля, затем восстановление на следующем тике: 301 тик.
            state.nextCharge = game.server().getTickCount() + 301;
            rechargingRockets.add(state);
        }
        syncRocketGlider(boots, state.jumps > 0);
        Vec3 velocity = player.getDeltaMovement(), direction = player.getLookAngle();
        double y = velocity.y > 0 ? velocity.y + 0.5 : direction.y < 0 ? 0.5 : velocity.y;
        player.setDeltaMovement(new Vec3(velocity.x, y, velocity.z).add(direction));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        // Paper World overload без extra использует 1.0, включая исходные частицы импульса.
        player.level().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 1);
        player.level().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 10, .5, .5, .5, 1);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5F, 2F);
        rocketHud(state);
        return !elytra;
    }

    public int rocketJumps(ServerPlayer player) {
        var state = rockets.get(player.getUUID());
        return state == null ? 0 : state.jumps;
    }

    public int rocketCooldownTicks(ServerPlayer player) {
        var state = rockets.get(player.getUUID());
        return state == null || state.nextCharge == 0 ? 0 : Math.max(0, state.nextCharge - game.server().getTickCount() - 1);
    }

    private void rocketHud(RocketState state) {
        refreshHud(state.owner);
    }
    private Component rocketHudComponent(RocketState state) {
        // Исходная строка CoreUiElements: четыре пробела, glyph E173, белые числа, без нового текста.
        return Component.literal("    \uE173 ").withStyle(style -> style.withItalic(false))
                .append(Component.literal(Integer.toString(state.jumps)).withStyle(ChatFormatting.WHITE))
                .append(" - ").append(Component.literal(Integer.toString((rocketCooldownTicks(state.owner) + 19) / 20))
                        .withStyle(ChatFormatting.WHITE));
    }
    /** Ported entries follow ui/get_data, charging and cooldown order; read native counters only. */
    MutableComponent hud(ServerPlayer source) {
        var component=Component.empty().append(mana.hud(source));
        if(magic.beaconSuppressed(source.position()))
            component.append(Component.literal(" \uE175").withColor(0x4e5c24));
        if(magic.mechanismSuppressed(source))
            component.append(Component.literal(" \uE176").withColor(0x4e5c24));
        int cold=frost.cold(source), coldCooldown=frost.cooldown(source), frozen=frost.frozenSeconds(source);
        if(cold>0) component.append(Component.literal("  \uE137 \u00a7fx")).append(Component.literal(Integer.toString(cold)).withStyle(ChatFormatting.RED));
        if(coldCooldown>0) component.append(Component.literal(" \u00a7f(").withStyle(ChatFormatting.RED))
                .append(Component.literal(Integer.toString(coldCooldown)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\u00a7fs)").withStyle(ChatFormatting.RED));
        hudPositive(component,"  \uE138 ",frozen,ChatFormatting.RED);
        hudPositive(component,"  \uE139 ",babylonEffects.queued(source,110),ChatFormatting.RED);
        hudPositive(component,"  \uE144 ",quiver.queued(source,102),ChatFormatting.RED);
        hudPositive(component,"  \uE145 ",quiver.queued(source,103),ChatFormatting.RED);
        hudPositive(component,"  \uE147 ",quiver.queued(source,112),ChatFormatting.RED);
        hudPositive(component,"  \uE148 ",quiver.queued(source,113),ChatFormatting.RED);
        component.append(fartus.hud(source));
        component.append(halloweenWind.hud(source));
        component.append(soulhunter.hud(source));
        component.append(specialTools.hud(source));
        component.append(soulless.hud(source));
        component.append(danmaku.hud(source));
        hudPositive(component,"  \uE163 ",babylonEffects.queued(source,101),ChatFormatting.RED);
        hudPositive(component,"  \uE164 ",babylonEffects.queued(source,109),ChatFormatting.RED);
        boolean bow=heldVanillaBow(source);
        if(bow || LbcShooters.isSlr(source.getMainHandItem()) || LbcShooters.isSlr(source.getOffhandItem())) {
            // The source uses /clear ... 0: native counting-only includes crafting slots and cursor, without mutation.
            // SLR consumes normal arrows, including custom/infinite variants, rather than the whole bow arrow tag.
            int arrows=source.getInventory().clearOrCountMatchingItems(stack->bow ? stack.is(net.minecraft.tags.ItemTags.ARROWS) : stack.is(Items.ARROW),true,0,source.inventoryMenu.getCraftSlots());
            component.append(Component.literal("    \uE108 "))
                    .append(Component.literal(Integer.toString(arrows)).withStyle(ChatFormatting.WHITE));
        }
        int katanaCharges=electro.hudCharges(source);
        if(katanaCharges>=0) component.append(Component.literal("    \uE103 \u00a7fx"))
                .append(Component.literal(Integer.toString(katanaCharges)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" [")).append(Component.literal(Integer.toString(electro.charging(source))).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("/\u00a7b10\u00a7f]"));
        if(LbcVexSword.isSword(source.getMainHandItem()) || LbcVexSword.isSword(source.getOffhandItem()))
            component.append(Component.literal("    \uE103 \u00a7fx"))
                    .append(Component.literal(Integer.toString(vexSword.charges(source))).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" [")).append(Component.literal(Integer.toString(vexSword.chargeProgress(source))).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("/\u00a7710\u00a7f]"));
        if(LbcFevgaleaStar.isSword(source.getMainHandItem())||LbcFevgaleaStar.isSword(source.getOffhandItem()))
            component.append(Component.literal("    \uE103 \u00a7fx"))
                    .append(Component.literal(Integer.toString(fevgaleaStar.charges(source))).withColor(0x41BBE0));
        if(LbcChargedSwords.isTriple(source.getMainHandItem())||LbcChargedSwords.isTriple(source.getOffhandItem()))
            component.append(Component.literal("    \uE103 \u00a7fx")).append(Component.literal(Integer.toString(chargedSwords.tripleCharges(source))).withStyle(ChatFormatting.GOLD));
        if(LbcChargedSwords.isBlaze(source.getMainHandItem())||LbcChargedSwords.isBlaze(source.getOffhandItem()))
            component.append(Component.literal("    \uE103 \u00a7fx")).append(Component.literal(Integer.toString(chargedSwords.blazeCharges(source))).withStyle(ChatFormatting.GOLD));
        int swordCharges=terra.hudCharges(source);
        if(swordCharges>=0) component.append(Component.literal("    \uE103 \u00a7fx"))
                .append(Component.literal(Integer.toString(swordCharges)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" [")).append(Component.literal("0").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("/\u00a7a1\u00a7f]"));
        int yellowCharges=electro.yellowHudCharges(source);
        if(yellowCharges>=0) component.append(Component.literal("    \uE103 \u00a7fx"))
                .append(Component.literal(Integer.toString(yellowCharges)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" ["))
                .append(Component.literal(Integer.toString(electro.yellowCharging(source))).withColor(0xA55E50))
                .append(Component.literal("/"))
                .append(Component.literal("7\u00a7f]").withColor(0xA55E50));
        // Mobgun is a source tick counter; Stand ability counters are source seconds.
        if(LbcMobgun.isMobgun(source.getMainHandItem()) || LbcMobgun.isMobgun(source.getOffhandItem()))
            hudCooldown(component,mobgun.cooldown(source));
        hudCooldown(component,helicopterHudValue(source));
        if(LbcReactiveItems.isReactive(source.getMainHandItem()) || LbcReactiveItems.isReactive(source.getOffhandItem()))
            hudCooldown(component,reactive.hudValue(source));
        hudCooldown(component,dividerHudValue(source));
        if(LbcMetalPipe.divider(source.getMainHandItem())||LbcMetalPipe.divider(source.getOffhandItem()))
            hudCooldown(component,hudSeconds(metalPipe.cooldownTicks(source)));
        if(LbcStand.isOraora(source.getMainHandItem()) || LbcStand.isOraora(source.getOffhandItem()))
            hudCooldown(component,hudSeconds(stand.cooldownTicks(source)));
        if(LbcStand.isHomingLightning(source.getMainHandItem()) || LbcStand.isHomingLightning(source.getOffhandItem()))
            hudCooldown(component,hudSeconds(stand.lightningCooldownTicks(source)));
        hudCooldown(component,bossHelicopterHudValue(source));
        if(LbcHohlopitek.isItem(source.getMainHandItem()) || LbcHohlopitek.isItem(source.getOffhandItem()))
            hudCooldown(component,hohlopitek.hudValue(source));
        if(LbcStand.isClocks(source.getMainHandItem()) || LbcStand.isClocks(source.getOffhandItem()))
            hudCooldown(component,hudSeconds(timeStop.cooldownTicks(source)));
        hudPositive(component,"  \uE110 ",blaze.headCharges(source),ChatFormatting.GREEN);
        hudPositive(component,"  \uE109 ",awakenedAcid.pending(source),ChatFormatting.GREEN);
        hudPositive(component,"  \uE112 ",blind.queued(source),ChatFormatting.GREEN);
        hudPositive(component,"  \uE113 ",psycho.hudValue(source,"lookback"),ChatFormatting.RED);
        int nanobotTier=mana.hudValue(source,"nanobots_tier");
        if(nanobotTier>=4)hudPositive(component,"  \uE114 ",nanobotTier,
                nanobotTier<12?ChatFormatting.RED:nanobotTier<24?ChatFormatting.GOLD:
                nanobotTier<40?ChatFormatting.YELLOW:nanobotTier<60?ChatFormatting.GREEN:ChatFormatting.AQUA);
        hudPositive(component,"  \uE115 ",mana.hudValue(source,"nomagic"),ChatFormatting.RED);
        hudPositive(component,"  \uE116 ",psycho.hudValue(source,"piss"),ChatFormatting.RED);
        hudPositive(component,"  \uE146 ",mana.hudValue(source,"effect_kiuaskivi"),ChatFormatting.RED);
        hudPositive(component,"  \uE118 ",mana.hudValue(source,"reverse_mana"),ChatFormatting.RED);
        hudPositive(component,"  \uE119 ",psycho.hudValue(source,"scared"),ChatFormatting.RED);
        hudPositive(component,"  \uE111 ",acidCorruption.deathRemaining(source),ChatFormatting.RED);
        hudPositive(component,"  \uE117 ",erecto.rayPending(source),ChatFormatting.GREEN);
        hudPositive(component,"  \uE120 ",psycho.hudValue(source,"screamray"),ChatFormatting.GREEN);
        hudPositive(component,"  \uE121 ",electro.shocks(source),ChatFormatting.RED);
        hudPositive(component,"  \uE122 ",homing.curseSeconds(source),ChatFormatting.RED);
        hudPositive(component," \uE123 ",psycho.hudValue(source,"twitching"),ChatFormatting.RED);
        hudPositive(component,"  \uE126 ",mana.hudValue(source,"void_resistance"),ChatFormatting.GREEN);
        hudPositive(component,"  \uE129 ",curseTicks(source),ChatFormatting.RED);
        hudPositive(component,"  \uE162 ",pendingCombat.curseSeconds(source),ChatFormatting.RED);
        // Overheaven is authored as matches remaining, not an elapsed-time estimate.
        hudPositive(component,"  \uE131 ",mana.hudValue(source,"overheaven"),ChatFormatting.GREEN);
        hudPositive(component,"  \uE135 ",kfcFood.gigaponosRemaining(source),ChatFormatting.WHITE);
        mana.appendHudManaStatuses(source,component);
        armorSets.techno().appendHud(source,component);component.append(shishEncounter.hud(source));
        hudPositive(component,"  \uE142 ",mana.hudValue(source,"effect_rtp_1"),ChatFormatting.WHITE);
        hudPositive(component,"  \uE142 ",mana.hudValue(source,"effect_rtp_2"),ChatFormatting.WHITE);
        hudPositive(component,"  \uE142 ",mana.hudValue(source,"effect_rtp_3"),ChatFormatting.WHITE);
        var rocket=rockets.get(source.getUUID());
        if(rocket!=null) component.append(rocketHudComponent(rocket));
        int akAmmo=akAmmoHudValue(source);
        if(akAmmo>=0) component.append(Component.literal("  \uE206 ")).append(Component.literal(Integer.toString(akAmmo)).withStyle(ChatFormatting.WHITE));
        return component;
    }
    private static boolean heldVanillaBow(ServerPlayer source) {
        return source.getMainHandItem().is(Items.BOW) || source.getMainHandItem().is(Items.CROSSBOW)
                || source.getOffhandItem().is(Items.BOW) || source.getOffhandItem().is(Items.CROSSBOW);
    }
    private static int hudSeconds(int ticks) { return ticks<=0 ? 0 : (int)(((long)ticks+19)/20); }
    private static void hudPositive(MutableComponent hud,String prefix,int value,ChatFormatting color) {
        if(value>0) hud.append(Component.literal(prefix)).append(Component.literal(Integer.toString(value)).withStyle(color));
    }
    private static void hudCooldown(MutableComponent hud,int remaining) {
        if(remaining<0) return;
        hud.append(Component.literal(remaining>0 ? "    \uE105 " : "    \uE104 "))
                .append(remaining>0 ? Component.literal(Integer.toString(remaining)).withStyle(ChatFormatting.WHITE)
                        : Component.translatable("ready").withStyle(ChatFormatting.GREEN));
    }
    /** One shared actionbar; observers with a player camera see that target's live HUD. */
    void invalidateHud(ServerPlayer viewer) { hudStates.remove(viewer.getUUID()); }

    void refreshHud(ServerPlayer viewer) {
        if(game.lbc().hidesKnowledge(viewer)) { clearHud(viewer); return; }
        ServerPlayer source=viewer;
        if(game.isObserver(viewer)) {
            if(viewer.getCamera() instanceof ServerPlayer target && target!=viewer) source=target;
            else { clearHud(viewer); return; }
        }
        if(!mana.visible(source)) { clearHud(viewer); return; }
        UUID viewerId=viewer.getUUID();
        // Compare the actual row so a newly added status cannot be omitted from cache invalidation.
        var content=hud(source);
        var old=hudStates.get(viewerId); int now=game.server().getTickCount(); UUID sourceId=source.getUUID();
        if(old!=null && old.source().equals(sourceId) && old.content().equals(content) && now-old.sentAt()<20) return;
        viewer.connection.send(new ClientboundSetActionBarTextPacket(content));
        hudStates.put(viewerId,new HudState(sourceId,content,now));
    }
    private int akAmmoHudValue(ServerPlayer player) {
        return LbcAk47.isFinite(player.getMainHandItem()) || LbcAk47.isFinite(player.getOffhandItem())
                ? mana.hudValue(player,"hitbullet") : -1;
    }
    private int bossHelicopterHudValue(ServerPlayer player) {
        return LbcShooters.isBossHelicopter(player.getMainHandItem()) || LbcShooters.isBossHelicopter(player.getOffhandItem())
                ? Math.max(0,shooters.bossHelicopterCooldownRemaining(player)) : -1;
    }
    private int helicopterHudValue(ServerPlayer player) {
        return LbcShooters.isHelicopter(player.getMainHandItem()) || LbcShooters.isHelicopter(player.getOffhandItem())
                ? Math.max(0,shooters.helicopterCooldownRemaining(player)) : -1;
    }
    private int dividerHudValue(ServerPlayer player) {
        return LbcWands.isDivider(player.getMainHandItem()) || LbcWands.isDivider(player.getOffhandItem())
                ? Math.max(0,mana.hudValue(player,"divider_cd")) : -1;
    }
    private void clearHud(ServerPlayer viewer) {
        if(hudStates.remove(viewer.getUUID())!=null) viewer.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
    }
    UUID hudSource(ServerPlayer viewer) {
        var state=hudStates.get(viewer.getUUID());
        return state==null ? null : state.source();
    }

    public void resetRocket(ServerPlayer player) {
        RocketState state = rockets.remove(player.getUUID());
        if (state != null) {
            rechargingRockets.remove(state);
            refreshHud(player);
        }
    }

    private void tickRockets() {
        int now = game.server().getTickCount();
        rocketBatch.addAll(rechargingRockets);
        for (RocketState state : rocketBatch) {
            ItemStack boots = state.owner.getItemBySlot(EquipmentSlot.FEET);
            if (!active(state.owner) || !state.owner.isAlive() || !isRocketBoots(boots)) { resetRocket(state.owner); continue; }
            if (now < state.nextCharge) continue;
            state.jumps++;
            state.nextCharge = state.jumps < 3 ? now + 301 : 0;
            if (state.jumps == 3) rechargingRockets.remove(state);
            syncRocketGlider(boots, true);
            rocketHud(state);
        }
        rocketBatch.clear();
        if (now % 20 == 0) {
            rocketBatch.addAll(rockets.values());
            for (RocketState state : rocketBatch) {
                if (active(state.owner) && isRocketBoots(state.owner.getItemBySlot(EquipmentSlot.FEET))) rocketHud(state);
                else resetRocket(state.owner);
            }
            rocketBatch.clear();
        }
    }

    /** Только два ингредиента: native ResultSlot расходует по одному из каждой занятой клетки. */
    public static ItemStack rocketCraftTarget(CraftingInput input) {
        if (input.ingredientCount() != 2) return ItemStack.EMPTY;
        boolean donor = false;
        ItemStack target = ItemStack.EMPTY;
        for (ItemStack stack : input.items()) {
            if (rocketIngredient(stack)) donor = true;
            else if (rocketRecipient(stack)) target = stack;
        }
        return donor ? target : ItemStack.EMPTY;
    }

    public ItemStack rocketCraftResult(CraftingInput input) {
        ItemStack target = rocketCraftTarget(input);
        return target.isEmpty() ? ItemStack.EMPTY : addRocketAbility(game.maps().level(), Vec3.ZERO, target.copyWithCount(1));
    }

    /** Preview не имеет побочных эффектов; частицы и звуки запускаются только при взятии результата. */
    public void rocketCraftTaken(ServerPlayer player, CraftingInput input) {
        if (active(player) && !rocketCraftTarget(input).isEmpty()) rocketCraftEffects(player);
    }

    public static boolean rocketIngredient(ItemStack stack) {
        if (stack.is(ROCKET_BOOTS)) return true;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (stack.isEmpty() || data == null) return false;
        var tag = data.copyTag();
        return tag.getIntOr("explosive_feets", 0) == 1 && tag.getIntOr("craftusable", 0) == 1;
    }

    private static boolean rocketRecipient(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (stack.isEmpty() || data == null || isRocketBoots(stack)) return false;
        var tag = data.copyTag();
        return tag.getIntOr("transformable", 0) == 1 && tag.getIntOr("boots", 0) == 1;
    }

    private ItemStack addRocketAbility(Entity source, ItemStack target) {
        return addRocketAbility((ServerLevel) source.level(), source.position(), target);
    }

    private ItemStack addRocketAbility(ServerLevel level, Vec3 origin, ItemStack target) {
        var key = ResourceKey.create(Registries.ITEM_MODIFIER, Identifier.parse("lbc:add_lore_rocket"));
        var modifier = game.server().reloadableRegistries().lookup().lookupOrThrow(Registries.ITEM_MODIFIER).getOrThrow(key).value();
        // Исходный modifier полностью статический: preview рецепта не требует игрока/THIS_ENTITY.
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.COMMAND);
        var context = new LootContext.Builder(params).create(java.util.Optional.empty());
        context.pushVisitedElement(LootContext.createVisitedEntry(modifier));
        return modifier.apply(target.copy(), context);
    }

    private static void rocketCraftEffects(Entity source) {
        var level = (ServerLevel) source.level();
        level.sendParticles(ParticleTypes.FLAME, true, false, source.getX(), source.getY() + 1, source.getZ(), 10, .2, .2, .2, .1);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_BLOCK), true, false,
                source.getX(), source.getY() + 1, source.getZ(), 10, .2, .2, .2, 1);
        // В datapack playsound отправляется @a, сохраняя master и исходные volume/pitch.
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            viewer.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(SoundEvents.FIRECHARGE_USE),
                    SoundSource.MASTER, source.getX(), source.getY(), source.getZ(), 1F, 1F, source.getRandom().nextLong()));
            viewer.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(SoundEvents.ANVIL_USE),
                    SoundSource.MASTER, source.getX(), source.getY(), source.getZ(), .5F, 1.3F, source.getRandom().nextLong()));
        }
    }

    private void tickRocketCrafts() {
        if (game.server().getTickCount() % 20 != 0 || rocketCraftDrops.isEmpty()) return;
        entityBatch.addAll(rocketCraftDrops);
        for (Entity entity : entityBatch) {
            var donor = (ItemEntity) entity;
            if (donor.isRemoved() || !game.playing() || !inArena(donor) || !rocketIngredient(donor.getItem())) {
                rocketCraftDrops.remove(donor); continue;
            }
            var dimension = donor.level().dimension().identifier().toString();
            if (dimension.equals("minecraft:nexus") || dimension.equals("minecraft:imprinted")) continue;
            var targets = donor.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(donor.position(), donor.position()).inflate(1.000001), target -> target != donor
                            && donor.distanceToSqr(target) >= .000001 && donor.distanceToSqr(target) <= 1 && rocketRecipient(target.getItem()));
            if (targets.isEmpty()) continue;
            var target = targets.getFirst();
            target.setItem(addRocketAbility(donor, target.getItem()));
            // Получатель может сохранить craftusable и затем сам стать донором без повторной загрузки entity.
            if (rocketIngredient(target.getItem())) rocketCraftDrops.add(target);
            rocketCraftEffects(donor);
            donor.getItem().shrink(1);
            if (donor.getItem().isEmpty()) donor.discard();
        }
        entityBatch.clear();
    }

    public LbcItems(GameController game) {
        this.game = game; this.clouds = new LbcClouds(game); this.trinkets = new LbcTrinkets(game); this.frost = new LbcFrost(game);
        this.mana = new LbcMana(game); this.magic = new LbcMagic(game); this.wands = new LbcWands(game,mana);
        this.terra = new LbcTerra(game);
        this.shooters = new LbcShooters(game);
        this.laserMinigun = new LbcLaserMinigun(game);
        this.ak47 = new LbcAk47(game);
        this.vexSword = new LbcVexSword(game,mana);
        this.magnum = new LbcMagnum(game);
        this.rifleZoom = new LbcRifleZoom(game);
        this.govnomet = new LbcGovnomet(game);
        this.stand = new LbcStand(game);
        this.timeStop = new LbcTimeStop(game);
        this.nuclearVisuals = new NuclearVisuals(game);
        this.blackHoleVisuals = new BlackHoleVisuals(game);
        this.kfcFood = new LbcKfcFood(game);
        this.kfcRewards = new LbcKfcRewards(game);
        this.helicopterEncounter = new LbcHelicopterEncounter(game);
        this.specialDrops = new LbcSpecialDrops(game);
        this.luckyPigs = new LbcLuckyPigs(game);
        this.enzanDrop = new LbcEnzanDrop(game);
        this.libraryDrop = new LbcLibraryDrop(game);
        this.beeDrops = new LbcBeeDrop(game);
        this.opAnvilDrops = new LbcOpAnvilDrop(game);
        this.slabDrops = new LbcSlabDrops(game);
        this.feedbackDrops = new LbcFeedbackDrops(game);
        this.creatureDrops = new LbcCreatureDrops(game);
        this.potionSpawnerDrops = new LbcPotionSpawnerDrops(game);
        this.islandDrops = new LbcIslandDrops(game);
        this.portalDrops = new LbcPortalDrops(game);
        this.luckyRoomDrops = new LbcLuckyRoomDrops(game);
        this.pisaDrops = new LbcPisaDrops(game);
        this.antigravity = new LbcAntigravity(game);
        this.ultraponos = new LbcUltraponos(game);
        this.reactive = new LbcReactive(game,mana);
        this.transformator = new LbcTransformator(game);
        this.stalin = new LbcStalin(game);
        this.skyHazards = new LbcSkyHazards(game);
        this.droneDrops = new LbcDroneDrops(game);
        this.hohlopitek = new LbcHohlopitek(game,mana);
        this.antDrops = new LbcAntDrops(game);
        this.tntDrops = new LbcTntDrops(game);
        this.creeperDrops = new LbcCreeperDrops(game);
        this.cageDrops = new LbcCageDrops(game);
        this.holeDrops = new LbcHoleDrops(game);
        this.corrosionDrops = new LbcCorrosionDrops(game);
        this.zombieTntDrops = new LbcZombieTntDrops(game);
        this.fallingNetheriteDrops = new LbcFallingNetheriteDrops(game);
        this.homes = new LbcHomes(game);
        this.electro = new LbcElectro(game);
        this.lifetimes = new ProjectileLifetimes(game);
        this.exasperato = new LbcExasperato(game);
        this.exoperso = new LbcExoperso(game);
        this.stone = new LbcStone(game);
        this.bedrock = new LbcBedrock(game);
        this.blind = new LbcBlind(game);
        this.air = new LbcAir(game);
        this.teleport = new LbcTeleport(game);
        this.ruby = new LbcRuby(game);
        this.consuming = new LbcConsuming(game);
        this.prism = new LbcPrism(game);
        this.awakenedAcid = new LbcAwakenedAcid(game);
        this.erecto = new LbcErecto(game);
        this.defensiveArcanums = new LbcDefensiveArcanums(game,mana);
        this.babylon = new LbcBabylon(game,mana);
        this.fartus = new LbcFartus(game);
        this.halloweenWind = new LbcHalloweenWind(game,mana);
        this.soulhunter = new LbcSoulhunter(game,mana);
        this.soulless = new LbcSoulless(game,mana);
        this.danmaku = new LbcDanmaku(game,mana);
        this.sauvojen = new LbcSauvojen(game);
        this.fevgaleaStar = new LbcFevgaleaStar(game);
        this.chargedSwords = new LbcChargedSwords(game);
        this.karaSlabs = new LbcKaraSlabs(game);
        this.classicMelee = new LbcClassicMelee(game);
        this.devilSword = new LbcDevilSword(game);
        this.pendingCombat = new LbcPendingCombat(game);
        this.pendingSwords = new LbcPendingSwords(game);
        this.incomingArtifacts = new LbcIncomingArtifacts(game);
        this.reactiveArtifacts = new LbcReactiveArtifacts(game);
        this.jumpArtifacts = new LbcJumpArtifacts(game);
        this.traveller = new LbcTraveller(game);
        this.popit = new LbcPopit(game);
        this.ordinaryAccessories = new LbcOrdinaryAccessories(game);
        this.chaosTraps = new LbcChaosTraps(game);
        this.spellSources = new LbcSpellSource(game);
        this.apocalypseEvents = new LbcApocalypseEvents(game);
        this.apocalypseActors = new LbcApocalypseActors(game);
        this.shishEncounter = new LbcShishEncounter(game);
        this.witchPassives = new LbcWitchPassives(game);
        this.chaosTrapActors = new LbcChaosTrapActors(game);
        this.sauvojenBoss = new LbcSauvojenBoss(game);
        this.infinity = new LbcInfinity(game);
        this.aimbobRewards = new LbcAimbobRewards(game);
        this.magicBean = new LbcMagicBean(game);
        this.attributeFood = new LbcAttributeFood(game);
        this.edibleSpells = new LbcEdibleSpells(game);
        this.foodStatuses = new LbcFoodStatuses(game);
        this.nukeEvent = new LbcNukeEvent(game);
        this.namedFoods = new LbcNamedFoods(game);
        this.foodLifecycle = new LbcFoodLifecycle(game);
        this.oreshek = new LbcOreshek(game);
        this.legacyEffects = new LbcLegacyEffects(game);
        this.armorSets = new LbcArmorSets(game);
        this.shulkerArmor = new LbcShulkerArmor(game);
        this.inventoryHazards = new LbcInventoryHazards(game);
        this.cannedFood = new LbcCannedFood(game);
        this.deathCompass = new LbcDeathCompass(game);
        this.malevich = new LbcMalevich(game);
        this.metalPipe = new LbcMetalPipe(game);
        this.moiseiBoots = new LbcMoiseiBoots(game);
        this.regenerationBelt = new LbcRegenerationBelt(game);
        this.quiver = new LbcQuiver(game);
        this.sheep = new LbcSheep(game,mana);
        this.hedgehog = new LbcHedgehog(game,mana);
        this.godHedgehog = new LbcGodHedgehog(game);
        this.hedgehogSpikes = new LbcHedgehogSpikes(game);
        this.hedgehogDimensions = new LbcHedgehogDimensions(game);
        mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("soulcorruption_wand_mana_use"),"lbc.math").set(1500);
        mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("vex_wand_mana_use"),"lbc.math").set(700);
        this.wtf = new LbcWtf(game);
        mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("fartus_nadristailo_mana_use"),"lbc.math").set(1250);
        this.babylonEffects = new LbcBabylonEffects(game,mana);
        this.voidArcanum = new LbcVoidArcanum(game);
        mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("void_arcanum_mana_activate"),"lbc.math").set(4000);
        mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("void_arcanum_mana_tick"),"lbc.math").set(11);
        mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("void_arcanum_mana_tick_sec"),"lbc.math").set(220);
        this.iceWand = new LbcIceWand(game);
        this.iceMaterials = new LbcIceMaterials(game,mana);
        mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("ice_wand_shift_mana_use"),"lbc.math").set(200);
        this.acidCorruption = new LbcAcidCorruption(game);
        this.corruptedGlyphs = new LbcCorruptedGlyphs(game);
        this.gaycasterAcademy = new LbcGaycasterAcademy(game);
        this.advancedSnow = new LbcAdvancedSnow(game);
        this.enderWiki = new LbcEnderWiki(game);
        this.libraryRandom = new LbcLibraryRandom(game);
        this.neutral = new LbcNeutral(game);
        this.nyauwuCrafting = new LbcNyauwuCrafting(game);
        this.bloodConfuse = new LbcBloodConfuse(game);
        this.evokerBass = new LbcEvokerBass(game);
        this.lifestealer = new LbcLifestealer(game);
        this.mobgun = new LbcMobgun(game);
        this.duplicating = new LbcDuplicating(game);
        this.academy = new LbcAcademy(game);
        this.indirectKills = new LbcIndirectKills(game);
        this.stonekivi = new LbcStonekivi(game);
        this.tanner = new LbcTanner(game);
        this.kiuas = new LbcKiuas(game,mana);
        this.blaze = new LbcBlaze(game);
        this.psycho = new LbcPsycho(game);
        this.summons = new LbcSummons(game,mana);
        this.syringe = new LbcSyringe(game);
        this.communism = new LbcCommunism(game);
        this.specialTools = new LbcSpecialTools(game);
        this.totems = new LbcTotems(game);
        this.homing = new LbcHoming(game);
        this.bolts = new LbcBolts(game);
        this.lightningAcquisition = new LbcLightningAcquisition(game);
        this.summons.prepare();
        this.nukeEvent.prepare();apocalypseEvents.prepare();
    }

    public String epoch() { return entityEpochTag; }
    public boolean currentEpoch(String value) { return entityEpochTag.equals(value); }
    public boolean currentEpoch(Entity entity) { return entity.entityTags().contains(entityEpochTag); }
    boolean debugItemUseWindow() {
        return game.inLobby() || game.stage() == GameController.Stage.LOADING;
    }
    boolean debugItemUseWindowOrCountdown() {
        return debugItemUseWindow() || game.stage() == GameController.Stage.COUNTDOWN;
    }
    public LbcClouds clouds() { return clouds; }
    public LbcTrinkets trinkets() { return trinkets; }
    public LbcFrost frost() { return frost; }
    public LbcMagic magic() { return magic; }
    public LbcMana mana() { return mana; }
    public LbcWands wands() { return wands; }
    public LbcStonekivi stonekivi() { return stonekivi; }
    public LbcTanner tanner() { return tanner; }
    public LbcKiuas kiuas() { return kiuas; }
    public LbcBlaze blaze() { return blaze; }
    public LbcPsycho psycho() { return psycho; }
    public LbcSummons summons() { return summons; }
    public LbcSyringe syringe() { return syringe; }
    public LbcCommunism communism() { return communism; }
    public LbcSpecialTools specialTools() { return specialTools; }
    public LbcTotems totems() { return totems; }
    public LbcHoming homing() { return homing; }
    public LbcBolts bolts() { return bolts; }
    public LbcLightningAcquisition lightningAcquisition() { return lightningAcquisition; }
    public LbcTerra terra() { return terra; }
    public LbcShooters shooters() { return shooters; }
    public LbcLaserMinigun laserMinigun() { return laserMinigun; }
    public LbcAk47 ak47() { return ak47; }
    public LbcVexSword vexSword() { return vexSword; }
    public LbcMagnum magnum() { return magnum; }
    public LbcRifleZoom rifleZoom() { return rifleZoom; }
    public LbcGovnomet govnomet() { return govnomet; }
    public static net.minecraft.world.item.component.ItemAttributeModifiers minigunAttributes() {
        return LbcMinigunMovement.attributes();
    }
    public void refreshMinigunMovement(ServerPlayer player) { LbcMinigunMovement.refresh(player); }
    public LbcStand stand() { return stand; }
    public LbcTimeStop timeStop() { return timeStop; }
    public NuclearVisuals nuclearVisuals() { return nuclearVisuals; }
    public BlackHoleVisuals blackHoleVisuals() { return blackHoleVisuals; }
    /** Common role/team/source guard is also checked before accepting a deferred hit. */
    public boolean allowDamage(LivingEntity victim,DamageSource source) {
        return LbcDamage.allowDamage(victim,source)
                && (!(source.getEntity() instanceof ServerPlayer attacker) || !game.isObserver(attacker) || timeStop.replaying(source)
                    || babylonEffects.currentSource(source)
                    || (LbcTntDrops.managed(source.getDirectEntity()) || LbcCreeperDrops.managed(source.getDirectEntity())
                        || LbcIncomingArtifacts.managed(source.getDirectEntity()) || LbcEdibleSpells.managed(source.getDirectEntity()) || LbcNukeEvent.managed(source.getDirectEntity())
                        || spellSources.wild(source.getDirectEntity()) || LbcChaosTrapActors.managed(source.getDirectEntity())
                        || LbcMoiseiBoots.managed(source.getDirectEntity()) || quiver.managedSecondary(source.getDirectEntity()) || babylonEffects.managedSecondary(source.getDirectEntity())
                        || source.getDirectEntity()!=null&&source.getDirectEntity().entityTags().contains("skywars2.quiver.fang")
                        || LbcCageDrops.managed(source.getDirectEntity()) || LbcPotionSpawnerDrops.managed(source.getDirectEntity())
                        || LbcTransformator.managed(source.getDirectEntity()) || LbcVexSword.managed(source.getDirectEntity())
                        || LbcDroneDrops.managed(source.getDirectEntity()) || LbcGodHedgehog.managed(source.getDirectEntity())
                        || LbcHedgehog.managed(source.getDirectEntity()) || SauvojenMeditation.managed(source.getDirectEntity()))
                        && currentEpoch(source.getDirectEntity()))
                && (!(victim instanceof ServerPlayer player) || game.allowDamage(player))
                && wands.allowDamage(victim,source) && kfcRewards.allowDamage(victim,source)
                && inventoryHazards.allowDamage(victim,source)
                && moiseiBoots.canAffect(source.getDirectEntity(),victim) && quiver.canAffect(source.getDirectEntity(),victim) && babylonEffects.canAffect(source.getDirectEntity(),victim)
                && helicopterEncounter.allowDamage(victim,source) && tntDrops.allowDamage(victim,source) && creeperDrops.allowDamage(victim,source) && zombieTntDrops.canAffect(source.getDirectEntity(),victim)
                && transformator.canAffect(source.getDirectEntity(),victim)
                && godHedgehog.canAffect(source.getDirectEntity(),victim)
                && stalin.canAffect(source.getDirectEntity(),victim)
                && luckyPigs.canAffect(source.getDirectEntity(),victim)
                && beeDrops.canAffect(source.getDirectEntity(),victim)
                && slabDrops.canAffect(source.getDirectEntity(),victim)
                && creatureDrops.canAffect(source.getDirectEntity(),victim)
                && potionSpawnerDrops.canAffect(source.getDirectEntity(),victim)
                && droneDrops.canAffect(source.getDirectEntity(),victim)
                && antDrops.canAffect(source.getDirectEntity(),victim)
                && chaosTrapActors.canAffect(source.getDirectEntity(),victim)
                && clouds.sourceAcidCanAffect(source.getDirectEntity(),victim);
    }
    public LbcKfcFood kfcFood() { return kfcFood; }
    public LbcKfcRewards kfcRewards() { return kfcRewards; }
    public LbcHelicopterEncounter helicopterEncounter() { return helicopterEncounter; }
    public LbcSpecialDrops chestDrops() { return specialDrops; }
    public LbcSpecialDrops specialDrops() { return specialDrops; }
    public LbcLuckyPigs luckyPigs() { return luckyPigs; }
    public LbcEnzanDrop enzanDrop() { return enzanDrop; }
    public LbcLibraryDrop libraryDrop() { return libraryDrop; }
    public LbcBeeDrop beeDrops() { return beeDrops; }
    public LbcOpAnvilDrop opAnvilDrops() { return opAnvilDrops; }
    public LbcSlabDrops slabDrops() { return slabDrops; }
    public LbcFeedbackDrops feedbackDrops() { return feedbackDrops; }
    public LbcCreatureDrops creatureDrops() { return creatureDrops; }
    public LbcPotionSpawnerDrops potionSpawnerDrops() { return potionSpawnerDrops; }
    public LbcIslandDrops islandDrops() { return islandDrops; }
    public LbcPortalDrops portalDrops() { return portalDrops; }
    public LbcLuckyRoomDrops luckyRoomDrops() { return luckyRoomDrops; }
    public LbcPisaDrops pisaDrops() { return pisaDrops; }
    public LbcAntigravity antigravity() { return antigravity; }
    public LbcUltraponos ultraponos() { return ultraponos; }
    public LbcReactive reactive() { return reactive; }
    public LbcTransformator transformator() { return transformator; }
    public LbcStalin stalin() { return stalin; }
    public LbcSkyHazards skyHazards() { return skyHazards; }
    public LbcDroneDrops droneDrops() { return droneDrops; }
    public LbcHohlopitek hohlopitek() { return hohlopitek; }
    public LbcAntDrops antDrops() { return antDrops; }
    public LbcTntDrops tntDrops() { return tntDrops; }
    public LbcCreeperDrops creeperDrops() { return creeperDrops; }
    public LbcCageDrops cageDrops() { return cageDrops; }
    public LbcHoleDrops holeDrops() { return holeDrops; }
    public LbcCorrosionDrops corrosionDrops() { return corrosionDrops; }
    public LbcZombieTntDrops zombieTntDrops() { return zombieTntDrops; }
    public LbcFallingNetheriteDrops fallingNetheriteDrops() { return fallingNetheriteDrops; }
    public LbcHomes homes() { return homes; }
    /** Запрет действий общий, но физика льда и электрического оглушения различается. */
    public boolean actionsBlocked(net.minecraft.world.entity.LivingEntity entity) {
        return frost.isFrozen(entity) || shooters.stunned(entity) || timeStop.frozen(entity);
    }
    public LbcElectro electro() { return electro; }
    public ProjectileLifetimes lifetimes() { return lifetimes; }
    public LbcExasperato exasperato() { return exasperato; }
    public LbcExoperso exoperso() { return exoperso; }
    public LbcStone stone() { return stone; }
    public LbcBedrock bedrock() { return bedrock; }
    public LbcBlind blind() { return blind; }
    public LbcAir air() { return air; }
    public LbcTeleport teleport() { return teleport; }
    public LbcRuby ruby() { return ruby; }
    public LbcConsuming consuming() { return consuming; }
    public LbcPrism prism() { return prism; }
    LbcAwakenedAcid awakenedAcid() { return awakenedAcid; }
    LbcErecto erecto() { return erecto; }
    public LbcFartus fartus() { return fartus; }
    public LbcHalloweenWind halloweenWind() { return halloweenWind; }
    public LbcSoulhunter soulhunter() { return soulhunter; }
    public LbcSoulless soulless() { return soulless; }
    public LbcDanmaku danmaku() { return danmaku; }
    public LbcSauvojen sauvojen() { return sauvojen; }
    public LbcFevgaleaStar fevgaleaStar() { return fevgaleaStar; }
    public LbcChargedSwords chargedSwords() { return chargedSwords; }
    public LbcKaraSlabs karaSlabs() { return karaSlabs; }
    public LbcClassicMelee classicMelee() { return classicMelee; }
    public LbcDevilSword devilSword() { return devilSword; }
    public LbcPendingCombat pendingCombat() { return pendingCombat; }
    public LbcPendingSwords pendingSwords() { return pendingSwords; }
    public LbcIncomingArtifacts incomingArtifacts() { return incomingArtifacts; }
    public LbcReactiveArtifacts reactiveArtifacts() { return reactiveArtifacts; }
    public LbcJumpArtifacts jumpArtifacts() { return jumpArtifacts; }
    public LbcTraveller traveller() { return traveller; }
    public LbcPopit popit() { return popit; }
    public LbcOrdinaryAccessories ordinaryAccessories() { return ordinaryAccessories; }
    public LbcArmorSets armorSets() { return armorSets; }
    public LbcChaosTraps chaosTraps() { return chaosTraps; }
    public LbcSpellSource spellSources() { return spellSources; }
    public LbcApocalypseEvents apocalypseEvents(){return apocalypseEvents;}
    public LbcApocalypseActors apocalypseActors(){return apocalypseActors;}
    public LbcShishEncounter shishEncounter(){return shishEncounter;}
    public LbcWitchPassives witchPassives(){return witchPassives;}
    public LbcChaosTrapActors chaosTrapActors() { return chaosTrapActors; }
    public LbcSauvojenBoss sauvojenBoss() { return sauvojenBoss; }
    public LbcInfinity infinity() { return infinity; }
    public LbcAimbobRewards aimbobRewards() { return aimbobRewards; }
    public LbcMagicBean magicBean() { return magicBean; }
    public LbcAttributeFood attributeFood() { return attributeFood; }
    public LbcEdibleSpells edibleSpells() { return edibleSpells; }
    public LbcFoodStatuses foodStatuses() { return foodStatuses; }
    public LbcNukeEvent nukeEvent() { return nukeEvent; }
    public LbcNamedFoods namedFoods(){return namedFoods;}
    public LbcFoodLifecycle foodLifecycle(){return foodLifecycle;}
    public LbcOreshek oreshek(){return oreshek;}
    public LbcLegacyEffects legacyEffects(){return legacyEffects;}
    /** Attach raw food score writes to their existing consumers without adding another dose. */
    public void adoptRawFoodStatuses(ServerPlayer player){
        blaze.adoptRawFoodStatuses(player);awakenedAcid.adoptRawFoodStatuses(player);erecto.adoptRawFoodStatuses(player);
        psycho.adoptRawFoodStatuses(player);electro.adoptRawFoodStatuses(player);frost.adoptRawFoodStatuses(player);
        acidCorruption.adoptRawFoodStatuses(player);kfcFood.adoptRawFoodStatuses(player);blind.adoptRawFoodStatuses(player);
        babylonEffects.adoptRawFoodStatuses(player);quiver.adoptRawFoodStatuses(player);kiuas.adoptRawFoodStatuses(player);
        mana.loaded(player);homing.adoptRawFoodStatuses(player);legacyEffects.adoptRawFoodStatuses(player);
        int skill=mana.hudValue(player,"skill_issue");if(skill>0&&!magic.rejectCustomEffect(player))
            turnCurses.computeIfAbsent(player.getUUID(),p->new TurnCurse(player)).remaining=skill;
    }
    public OwnedEffectLayers ownedEffects() { return ownedEffects; }
    public LbcShulkerArmor shulkerArmor() { return shulkerArmor; }
    public LbcInventoryHazards inventoryHazards() { return inventoryHazards; }
    public LbcCannedFood cannedFood() { return cannedFood; }
    public LbcDeathCompass deathCompass() { return deathCompass; }
    public LbcMalevich malevich() { return malevich; }
    public LbcMetalPipe metalPipe() { return metalPipe; }
    public LbcMoiseiBoots moiseiBoots() { return moiseiBoots; }
    public LbcRegenerationBelt regenerationBelt() { return regenerationBelt; }
    public LbcQuiver quiver() { return quiver; }
    public LbcSheep sheep() { return sheep; }
    public LbcHedgehog hedgehog() { return hedgehog; }
    public LbcGodHedgehog godHedgehog() { return godHedgehog; }
    public LbcHedgehogSpikes hedgehogSpikes() { return hedgehogSpikes; }
    public LbcHedgehogDimensions hedgehogDimensions() { return hedgehogDimensions; }
    public LbcWtf wtf() { return wtf; }
    public LbcBabylon babylon() { return babylon; }
    public LbcBabylonEffects babylonEffects() { return babylonEffects; }
    LbcDefensiveArcanums defensiveArcanums() { return defensiveArcanums; }
    LbcVoidArcanum voidArcanum() { return voidArcanum; }
    public void voidArcanumHandsSwapped(ServerPlayer player) { voidArcanum.handSwapped(player); }
    LbcIceWand iceWand() { return iceWand; }
    LbcIceMaterials iceMaterials() { return iceMaterials; }
    public boolean attackIceCrystal(ServerPlayer player,Entity target) { return iceWand.attack(player,target); }
    LbcAcidCorruption acidCorruption() { return acidCorruption; }
    public LbcCorruptedGlyphs corruptedGlyphs() { return corruptedGlyphs; }
    LbcGaycasterAcademy gaycasterAcademy() { return gaycasterAcademy; }
    public LbcAdvancedSnow advancedSnow() { return advancedSnow; }
    public LbcEnderWiki enderWiki() { return enderWiki; }
    LbcLibraryRandom libraryRandom() { return libraryRandom; }
    public LbcNeutral neutral() { return neutral; }
    public LbcBloodConfuse bloodConfuse() { return bloodConfuse; }
    public LbcEvokerBass evokerBass() { return evokerBass; }
    public LbcLifestealer lifestealer() { return lifestealer; }
    public LbcMobgun mobgun() { return mobgun; }
    public LbcDuplicating duplicating() { return duplicating; }
    public LbcAcademy academy() { return academy; }
    public LbcIndirectKills indirectKills() { return indirectKills; }

    public static boolean isDynamite(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(Items.SNOWBALL) && data != null && data.matchedBy(DYNAMITE);
    }

    /** The vanilla spawn egg reads its entity type and marker tags directly from ENTITY_DATA. */
    public static boolean isNanobot(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.ENTITY_DATA);
        if (data == null || data.type() != EntityTypes.MARKER) return false;
        var tags = data.copyTagWithoutId().getListOrEmpty("Tags");
        for (int index = 0; index < tags.size(); index++) {
            if (tags.getStringOr(index, "").equals("nano_trapik")) return true;
        }
        return false;
    }

    /** Native snowball use retains consumption, statistics, throw sound and projectile velocity. */
    public void entityLoaded(Entity entity) {
        spellSources.capture(entity);apocalypseEvents.inheritChild(entity);shishEncounter.inheritChild(entity);
        if(entity instanceof Snowball ball&&ball.getOwner() instanceof ServerPlayer player&&!LbcApocalypseEvents.kinds(ball.getItem()).isEmpty()){
            if(apocalypseEvents.thrown(player,ball.getItem())&&LbcFartus.data(ball.getItem()).getIntOr("tmt_item",0)!=1){ball.discard();return;}
        }
        nukeEvent.inheritChild(entity);foodLifecycle.inheritChild(entity);oreshek.inheritChild(entity);
        if (entity instanceof net.minecraft.world.entity.monster.Silverfish && entity.level() instanceof ServerLevel level
                && godHedgehog.blocksSilverfish(level) && !godHedgehog.isBoss(entity)) { entity.discard(); return; }
        if (consuming.eraseLoaded(entity)) return;
        mobgun.claimChild(entity);
        frost.loaded(entity);
        pisaDrops.loaded(entity);
        antigravity.loaded(entity);
        if (entity.isRemoved()) return;
        if (hasStaleItemEpoch(entity)) {
            entity.discard();
            return;
        }
        helicopterEncounter.loaded(entity);
        transformator.loaded(entity);
        stalin.loaded(entity);
        indirectKills.supportEntityLoaded(entity);
        specialDrops.loaded(entity);
        spellSources.loaded(entity);apocalypseEvents.loaded(entity);apocalypseActors.loaded(entity);shishEncounter.loaded(entity);
        chaosTrapActors.loaded(entity);
        sauvojenBoss.loaded(entity);
        magicBean.loaded(entity);
        edibleSpells.loaded(entity);
        nukeEvent.loaded(entity);foodLifecycle.loaded(entity);oreshek.loaded(entity);
        luckyPigs.loaded(entity);
        libraryDrop.loaded(entity);
        beeDrops.loaded(entity);
        opAnvilDrops.loaded(entity);
        slabDrops.loaded(entity);
        creatureDrops.loaded(entity);
        armorSets.loaded(entity);
        if(entity instanceof Projectile shot)armorSets.phantom().projectile(shot);
        potionSpawnerDrops.loaded(entity);
        droneDrops.loaded(entity);
        antDrops.loaded(entity);
        tntDrops.loaded(entity);
        inventoryHazards.loaded(entity);
        malevich.loaded(entity);
        metalPipe.loaded(entity);
        pendingCombat.loaded(entity);
        pendingSwords.loaded(entity);
        incomingArtifacts.loaded(entity);
        moiseiBoots.loaded(entity);
        quiver.loaded(entity);
        reactiveArtifacts.loaded(entity);
        creeperDrops.loaded(entity);
        cageDrops.loaded(entity);
        zombieTntDrops.loaded(entity);
        fallingNetheriteDrops.loaded(entity);
        homes.loaded(entity);
        lightningAcquisition.loaded(entity);
        terra.loaded(entity);
        totems.loaded(entity);
        lifetimes.loaded(entity);
        exasperato.loaded(entity);
        exoperso.loaded(entity);
        stone.loaded(entity);
        blind.loaded(entity);
        air.loaded(entity);
        teleport.loaded(entity);
        ruby.loaded(entity);
        consuming.loaded(entity);
        prism.loaded(entity);
        iceWand.loaded(entity);
        voidArcanum.loaded(entity);
        babylon.loaded(entity);
        fartus.loaded(entity);
        halloweenWind.loaded(entity);
        soulless.loaded(entity);
        danmaku.loaded(entity);
        sauvojen.loaded(entity);
        chargedSwords.loaded(entity);
        karaSlabs.loaded(entity);
        sheep.loaded(entity);
        hedgehog.loaded(entity);
        godHedgehog.loaded(entity);
        hedgehogSpikes.loaded(entity);
        wtf.loaded(entity);
        babylonEffects.loaded(entity);
        acidCorruption.loaded(entity);
        corruptedGlyphs.loaded(entity);
        gaycasterAcademy.loaded(entity);
        kiuas.loaded(entity);
        advancedSnow.loaded(entity);
        neutral.loaded(entity);
        nyauwuCrafting.loaded(entity);
        evokerBass.loaded(entity);
        lifestealer.loaded(entity);
        govnomet.loaded(entity);
        mobgun.loaded(entity);
        if (academy.loaded(entity)) return;
        if (vexSword.loaded(entity)) return;
        if (shooters.loaded(entity)) { markCurrentItemEpoch(entity); return; }
        if (magic.loaded(entity)) { markCurrentItemEpoch(entity); return; }
        if (clouds.loaded(entity)) { markCurrentItemEpoch(entity); return; }
        blaze.loaded(entity);
        psycho.loaded(entity);
        summons.loaded(entity);
        syringe.loaded(entity);
        homing.loaded(entity);
        bolts.loaded(entity);
        stand.loaded(entity);
        if (trinkets.loaded(entity)) { markCurrentItemEpoch(entity); return; }
        if (entity instanceof ItemEntity item && game.playing() && inArena(item) && rocketIngredient(item.getItem())) {
            rocketCraftDrops.add(item);
        }
        if (entity instanceof Marker marker && marker.entityTags().contains("nano_trapik")) {
            if ((game.playing() || debugItemUseWindow()) && inArena(marker)) {
                markCurrentItemEpoch(marker);
                nanoTraps.add(marker);
            } else marker.discard();
            return;
        }
        if (entity instanceof PrimedTnt tnt && tnt.entityTags().contains(entityEpochTag)
                && !tnt.entityTags().contains(LbcHelicopterEncounter.ATTACK)) {
            if (inArena(tnt)) dynamites.add(tnt);
            else tnt.discard();
            return;
        }
        if (!(entity instanceof Snowball snowball) || !isDynamite(snowball.getItem())) return;
        if (snowball.getOwner() instanceof ServerPlayer owner && active(owner) && inArena(snowball)) {
            markCurrentItemEpoch(snowball);
            pendingThrows.add(snowball);
        } else snowball.discard();
    }

    /** Массовые заклинания затрагивают и лёгкие снаряды, у которых нет отдельной native entity. */
    void transformSpellOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform) {
        wands.transformOrigins(level,transform);
        sauvojen.transformOrigins(level,transform);
        electro.transformOrigins(level,transform);
        bolts.transformOrigins(level,transform);
        blaze.transformOrigins(level,transform);
        blind.transformOrigins(level,transform);
        homing.transformOrigins(level,transform);
        duplicating.transformOrigins(level,transform);
        stand.transformOrigins(level,transform);
    }
    public void entityUnloaded(Entity entity) {
        if(entity instanceof ServerPlayer player)armorSets.phantom().forget(player);
        if(entity instanceof LivingEntity living)ownedEffects.forget(living);
        specialDrops.unloaded(entity);
        spellSources.unloaded(entity);apocalypseEvents.unloaded(entity);apocalypseActors.unloaded(entity);shishEncounter.unloaded(entity);
        chaosTrapActors.unloaded(entity);
        sauvojenBoss.unloaded(entity);
        magicBean.unloaded(entity);
        edibleSpells.unloaded(entity);
        nukeEvent.unloaded(entity);foodLifecycle.unloaded(entity);oreshek.unloaded(entity);
        pisaDrops.unloaded(entity);
        antigravity.unloaded(entity);
        portalDrops.unloaded(entity);
        luckyPigs.unloaded(entity);
        libraryDrop.unloaded(entity);
        beeDrops.unloaded(entity);
        opAnvilDrops.unloaded(entity);
        slabDrops.unloaded(entity);
        creatureDrops.unloaded(entity);
        potionSpawnerDrops.unloaded(entity);
        droneDrops.unloaded(entity);
        antDrops.unloaded(entity);
        tntDrops.unloaded(entity);
        creeperDrops.unloaded(entity);
        cageDrops.unloaded(entity);
        zombieTntDrops.unloaded(entity);
        fallingNetheriteDrops.unloaded(entity);
        homes.unloaded(entity);
        lightningAcquisition.unloaded(entity);
        govnomet.unloaded(entity);
        academy.unloaded(entity);
        totems.unloaded(entity);
        lifetimes.unloaded(entity);
        exasperato.unloaded(entity);
        exoperso.unloaded(entity);
        stone.unloaded(entity);
        blind.unloaded(entity);
        air.unloaded(entity);
        teleport.unloaded(entity);
        ruby.unloaded(entity);
        consuming.unloaded(entity);
        prism.unloaded(entity);
        iceWand.unloaded(entity);
        voidArcanum.unloaded(entity);
        babylon.unloaded(entity);
        fartus.unloaded(entity);
        halloweenWind.unloaded(entity);
        soulless.unloaded(entity);
        danmaku.unloaded(entity);
        sauvojen.unloaded(entity);
        chargedSwords.unloaded(entity);
        karaSlabs.unloaded(entity);
        sheep.unloaded(entity);
        hedgehog.unloaded(entity);
        godHedgehog.unloaded(entity);
        hedgehogSpikes.unloaded(entity);
        wtf.unloaded(entity);
        babylonEffects.unloaded(entity);
        acidCorruption.unloaded(entity);
        corruptedGlyphs.unloaded(entity);
        gaycasterAcademy.unloaded(entity);
        kiuas.unloaded(entity);
        advancedSnow.unloaded(entity);
        armorSets.unloaded(entity);
        neutral.unloaded(entity);
        nyauwuCrafting.unloaded(entity);
        evokerBass.unloaded(entity);
        lifestealer.unloaded(entity);
        mobgun.unloaded(entity);
        terra.unloaded(entity);
        shooters.unloaded(entity);
        vexSword.unloaded(entity);
        stand.unloaded(entity);
        helicopterEncounter.entityUnloaded(entity);
        transformator.unloaded(entity);
        stalin.unloaded(entity);
        kfcRewards.entityUnloaded(entity);
        inventoryHazards.unloaded(entity);
        malevich.unloaded(entity);
        metalPipe.unloaded(entity);
        pendingCombat.unloaded(entity);
        pendingSwords.unloaded(entity);
        incomingArtifacts.unloaded(entity);
        moiseiBoots.unloaded(entity);
        quiver.unloaded(entity);
        reactiveArtifacts.unloaded(entity);
        magic.unloaded(entity);
        frost.unloaded(entity);
        clouds.unloaded(entity);
        trinkets.unloaded(entity);
        blaze.unloaded(entity);
        psycho.unloaded(entity);
        syringe.unloaded(entity);
        communism.removed(entity);
        summons.unloaded(entity);
        homing.unloaded(entity);
        bolts.unloaded(entity);
        rocketCraftDrops.remove(entity);
        pendingThrows.remove(entity);
        dynamites.remove(entity);
        if (entity instanceof Marker marker) nanoTraps.remove(marker);
    }

    /** Only tracked item projectiles are visited; native TNT handles physics, explosion and damage. */
    private void tickDynamites() {
        // Урон может завершить матч и очистить общий буфер прямо внутри взрыва.
        for (var tnt : List.copyOf(dynamites)) {
            if (tnt.isRemoved()) { dynamites.remove(tnt); continue; }
            boolean explode=tnt.onGround() && tnt.entityTags().remove("groundbam");
            Vec3 motion = tnt.getDeltaMovement();
            // Это исходное условие по ЛЮБОЙ оси, а не проверка полной остановки вектора.
            if (tnt.entityTags().contains("dynamite") && (Mth.floor(motion.x * 100000) == 0
                    || Mth.floor(motion.y * 100000) == 0 || Mth.floor(motion.z * 100000) == 0)) {
                explode=true;
            }
            if(explode) {
                // Летящий TNT остаётся настоящей сущностью до контакта, короткий запал заменён прямым взрывом.
                dynamites.remove(tnt); tnt.discard();
                game.items().exasperato().protectedBlast(tnt.getOwner(),(ServerLevel)tnt.level(),tnt.position(),4,null);
            }
        }
        entityBatch.clear();
        // Не меняем entity manager из ENTITY_LOAD: преобразование выполняется в обычном игровом тике.
        entityBatch.addAll(pendingThrows); pendingThrows.clear();
        for (Entity entity : entityBatch) {
            var snowball = (Snowball) entity;
            if (snowball.isRemoved()) continue;
            Vec3 position = snowball.position().add(Vec3.directionFromRotation(snowball.getXRot() - 90, snowball.getYRot()));
            Vec3 motion = snowball.getDeltaMovement();
            var owner = snowball.getOwner() instanceof LivingEntity living ? living : null;
            var tnt = new PrimedTnt(snowball.level(), position.x, position.y, position.z, owner);
            tnt.setFuse(1000);
            tnt.setDeltaMovement(Mth.floor(motion.x * 10000) * 0.0001,
                    Mth.floor(motion.y * 10000) * 0.0001, Mth.floor(motion.z * 10000) * 0.0001);
            tnt.entityTags().addAll(List.of("can_deleted", "groundbam", "dynamite", entityEpochTag));
            if (snowball.level().addFreshEntity(tnt)) dynamites.add(tnt);
            snowball.discard();
        }
        entityBatch.clear();
    }

    public int dynamiteCount() { return dynamites.size(); }
    public int pendingThrowCount() { return pendingThrows.size(); }

    /** Cast-тринкет использует тот же native TNT и трекер, что уже перенесённый динамит. */
    void castWandDynamite(ServerPlayer player) {
        // Высота тринкета не зависит от origin конкретного жезла или наклона взгляда.
        Vec3 position=player.getEyePosition().add(0,.5,0), direction=player.getLookAngle();
        var tnt=new PrimedTnt(player.level(),position.x,position.y,position.z,player);
        tnt.setFuse(1000);
        double[] motion={direction.x,direction.y,direction.z};
        for(int i=0;i<3;i++) {
            double coarse=Mth.floor(motion[i]*10000)*.0002;
            motion[i]=(Mth.floor(coarse*10000)+player.getRandom().nextInt(201)-100)*.0001;
        }
        tnt.setDeltaMovement(motion[0],motion[1],motion[2]);
        tnt.entityTags().addAll(List.of("can_deleted","meowmurnya","dynamite","groundbam",entityEpochTag));
        if(player.level().addFreshEntity(tnt)) dynamites.add(tnt);
    }
    public int trapCount() { return nanoTraps.size(); }

    private boolean inArena(Entity entity) {
        var arena = game.maps().level();
        return arena != null && entity.level().dimension().equals(arena.dimension());
    }

    private static String newEntityEpochTag() { return "skywars2.items." + UUID.randomUUID(); }

    boolean hasStaleItemEpoch(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !tag.equals(entityEpochTag)) return true;
        }
        return false;
    }

    void markCurrentItemEpoch(Entity entity) {
        entity.entityTags().add(entityEpochTag);
        spellSources.capture(entity);
    }

    private void tickNanoTraps() {
        if (nanoTraps.isEmpty()) return;
        if (!game.playing()) {
            markerBatch.addAll(nanoTraps);
            for (Marker marker : markerBatch) {
                if (marker.isRemoved() || !currentEpoch(marker)) {
                    nanoTraps.remove(marker);
                    if (!marker.isRemoved()) marker.discard();
                }
            }
            markerBatch.clear();
            return;
        }
        // Поиск зависит от числа живых игроков, а не от количества разложенных неактивных мин.
        // Несколько игроков могут найти одну мину: срабатывание остаётся единым для всех её целей.
        for (ServerPlayer player : game.alivePlayers()) {
            var area = new AABB(player.position(), player.position()).inflate(1.000001);
            nearbyTraps.addAll(player.level().getEntitiesOfClass(Marker.class, area, marker ->
                    nanoTraps.contains(marker) && player.distanceToSqr(marker) <= 1.0));
        }
        markerBatch.addAll(nearbyTraps); nearbyTraps.clear();
        for (Marker marker : markerBatch) {
            if (!nanoTraps.contains(marker)) continue;
            if (marker.isRemoved() || !inArena(marker)) {
                nanoTraps.remove(marker);
                if (!marker.isRemoved()) marker.discard();
                continue;
            }
            if (!(marker.level() instanceof ServerLevel level)) continue;

            // Broad phase includes the exact distance=1 boundary; the predicate uses the source radius.
            var nearby = new AABB(marker.position(), marker.position()).inflate(1.000001);
            List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class, nearby, player ->
                    player.level() == marker.level() && player.distanceToSqr(marker) <= 1.0
                            && game.allowDamage(player) && !game.isObserver(player));
            if (targets.isEmpty()) continue;

            // Source executes `as @a[distance=..1]`: every overlapping active player is debuffed once.
            for (ServerPlayer target : targets) applyNanoPenalty(target);
            level.sendParticles(new DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1.0F, 0.357F, 0.937F, 0.98F), 1.0F),
                    marker.getX(), marker.getY() + 1, marker.getZ(), 30, 0, 1, 0, 0);
            playNanoSound(marker);
            nanoTraps.remove(marker);
            marker.discard();
        }
        markerBatch.clear();
    }

    private void applyNanoPenalty(ServerPlayer player) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        var armor = player.getAttribute(Attributes.ARMOR);
        var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (maxHealth == null || armor == null || toughness == null) return;
        originalNanoAttributes.computeIfAbsent(player.getUUID(), ignored ->
                new NanoAttributes(maxHealth.getBaseValue(), armor.getBaseValue(), toughness.getBaseValue()));
        // `attribute base get` truncates toward zero; `data get Motion` uses floor instead.
        maxHealth.setBaseValue((int) maxHealth.getBaseValue() - 2);
        armor.setBaseValue((int) armor.getBaseValue() - 2);
        // (user) Исправляем source-ошибку: третий штраф меняет toughness, а не armor повторно.
        toughness.setBaseValue((int) toughness.getBaseValue() - 2);
    }

    /** Restore source attributes before lobby, leave, or respawn resets. */
    public void resetAttributes(ServerPlayer player) {
        NanoAttributes original = originalNanoAttributes.remove(player.getUUID());
        if (original == null) return;
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        var armor = player.getAttribute(Attributes.ARMOR);
        var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (maxHealth != null) maxHealth.setBaseValue(original.maxHealth());
        if (armor != null) armor.setBaseValue(original.armor());
        if (toughness != null) toughness.setBaseValue(original.toughness());
    }

    private void discardNanoTraps() {
        markerBatch.addAll(nanoTraps);
        nanoTraps.clear();
        for (Marker marker : markerBatch) if (!marker.isRemoved()) marker.discard();
        markerBatch.clear();
    }

    private void playNanoSound(Marker marker) {
        // Source sends the positional sound to @a; clients apply normal distance attenuation.
        for (ServerPlayer viewer : game.server().getPlayerList().getPlayers()) {
            viewer.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(SoundEvents.SNIFFER_SNIFFING),
                    SoundSource.MASTER, marker.getX(), marker.getY(), marker.getZ(), 0.6F, 2.0F,
                    viewer.getRandom().nextLong()));
        }
    }

    private boolean active(ServerPlayer player) {
        String dimension = player.level().dimension().identifier().toString();
        return game.canUseGameplayItems(player) && !game.awaitingRespawn(player.getUUID())
                && !dimension.equals("minecraft:nexus") && !dimension.equals("minecraft:imprinted");
    }

    /** Same point as the source effects_changed advancement; includes splash/cloud/commands. */
    public void effectsChanged(ServerPlayer player) {
        mana.effectsChanged(player);
        // Очищающее зелье снимает эффекты и проклятие предмета; выбор перков и бонусы атрибутов сохраняются.
        var effect = player.getEffect(MobEffects.RAID_OMEN);
        if (effect == null || effect.getAmplifier() != 43 || !active(player)) return;
        clearEffects(player);
    }

    /** Общая полная очистка для зелья и артефакта: новые кастомные статусы подключаются здесь. */
    public void clearEffects(ServerPlayer player) {
        indirectKills.clearHunger(player);
        // Сначала снимаем источник повторного наложения и привязку ко льду, затем нативные эффекты.
        // Экипировка, выбранные перки, заряды способностей и постоянные атрибуты не являются статусами.
        clear(player);
        player.entityTags().remove("lightningist");
        player.entityTags().remove("greed_cursed");
        if (!player.getActiveEffects().isEmpty()) player.removeAllEffects();
    }

    /** Native used_totem runs before vanilla death-protection effects clear all potions. */
    public void usedTotem(ServerPlayer player, ItemStack used) {
        totems.used(player,used);
        var data = used.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.matchedBy(BROKEN_TOTEM) || !active(player)) return;
        screamer(player,null);
        if (!magic.rejectCustomEffect(player)){
            var curse=turnCurses.computeIfAbsent(player.getUUID(),ignored->new TurnCurse(player));curse.remaining+=100;
            mana.score(player,"skill_issue").set(curse.remaining);
        }
    }

    /** Один исходный скример для сломанного тотема и психических жезлов; проклятие тотема отдельно. */
    public void screamer(LivingEntity target, ServerPlayer owner) {
        screamer(target,owner,false);
    }

    /** The dropped legacy variant keeps its separate sound volume, smoke and duration. */
    public void screamer(LivingEntity target, ServerPlayer owner, boolean old) {
        int duration=old ? 200 : 120;
        if (!(target instanceof ServerPlayer player)) {
            target.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,duration,10,false,false),owner);
            return;
        }
        float volume=old ? 100000 : 1; // Explicitly retained by the author for the old trap.
        for (int repeat = 0; repeat < 3; repeat++) {
            sound(player, SoundEvents.GHAST_HURT, 0,volume);
            sound(player, SoundEvents.GHAST_HURT, 1,volume);
            sound(player, SoundEvents.AMBIENT_CAVE.value(), 0,volume);
            sound(player, SoundEvents.AMBIENT_CAVE.value(), 1,volume);
            sound(player, SoundEvents.AMBIENT_CAVE.value(), 2,volume);
        }
        if (!old) sound(player, SoundEvents.WITHER_SPAWN, 0);
        player.level().sendParticles(player, old ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE, old, false,
                player.getX(), player.getY() + 1.62, player.getZ(), 5000, 0.5, 0.3, 0.5, 0.05);
        player.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS, duration, 10, false, false),owner);
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float pitch) {
        sound(player,sound,pitch,1);
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        player.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound), SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }

    /** Twitching имитирует обычный одиночный клик, сохраняя Fabric hooks и расход предметов.
     * Пакеты с выдуманным sequence отправлять нельзя: они сдвигают ACK реального клиента. */
    public void mouseAction(ServerPlayer player, boolean attack) {
        if(!game.server().isSameThread()) throw new IllegalStateException("Mouse actions require the server thread");
        if (!game.canUseGameplayItems(player) || !player.isAlive() || game.isObserver(player)
                || frost.isFrozen(player) || player.entityTags().contains("time_stopped")
                || wands.timeStopped() || player.isUsingItem()) return;
        var level=player.level();
        var held=player.getMainHandItem();
        if(attack && held.has(DataComponents.PIERCING_WEAPON)) {
            if(held.isItemEnabled(level.enabledFeatures()) && !player.cannotAttackWithItem(held,5))
                held.get(DataComponents.PIERCING_WEAPON).attack(player,EquipmentSlot.MAINHAND);
            player.connection.handlePunch(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE);
            return;
        }
        double entityRange=attack ? (player.isCreative()
                ? player.getAttackRangeWith(held).maxCreativeReach() : player.getAttackRangeWith(held).maxReach())
                : player.entityInteractionRange();
        double blockRange=player.blockInteractionRange(),range=Math.max(entityRange,blockRange);
        if (!Double.isFinite(range) || range<=0) return;
        Vec3 from=player.getEyePosition(),to=from.add(player.getLookAngle().scale(range));
        // OUTLINE как у клиентского прицела; граница выгруженного чанка прерывает поиск без его загрузки.
        var wall=net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,level,(world,pos)-> {
            if(!world.hasChunkAt(pos)) return net.minecraft.world.phys.shapes.Shapes.block().clip(from,to,pos);
            return world.getBlockState(pos).getShape(world,pos,
                    net.minecraft.world.phys.shapes.CollisionContext.of(player)).clip(from,to,pos);
        },world->null);
        Vec3 end=wall==null ? to : wall.getLocation();
        var entity=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player,from,end,
                new AABB(from,end).inflate(1),candidate->candidate!=player && candidate.isPickable()
                        && !candidate.isSpectator(),Math.min(entityRange*entityRange,from.distanceToSqr(end)));
        player.resetLastActionTime();
        if(attack) {
            if(entity!=null) {
                var target=entity.getEntity();
                if(!(target instanceof ItemEntity) && !(target instanceof net.minecraft.world.entity.ExperienceOrb)
                        && !(target instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow && !arrow.isAttackable())
                        && level.getWorldBorder().isWithinBounds(target.blockPosition())
                        && player.isWithinAttackRange(held,target.getBoundingBox(),0)
                        && held.isItemEnabled(level.enabledFeatures()) && !player.cannotAttackWithItem(held,5))
                    player.attack(target);
            } else if(wall!=null && from.distanceToSqr(wall.getLocation())<=blockRange*blockRange
                    && level.hasChunkAt(wall.getBlockPos()) && level.mayInteract(player,wall.getBlockPos())
                    && level.getWorldBorder().isWithinBounds(wall.getBlockPos())) {
                // Один клик, без удерживания кнопки и без обхода твёрдости блока.
                player.gameMode.handleBlockBreakAction(wall.getBlockPos(),
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                        wall.getDirection(),level.getMaxY(),0);
                player.gameMode.handleBlockBreakAction(wall.getBlockPos(),
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                        wall.getDirection(),level.getMaxY(),0);
            }
            player.connection.handlePunch(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE);
        } else for(var hand:InteractionHand.values()) {
            var stack=player.getItemInHand(hand);
            if(!stack.isItemEnabled(level.enabledFeatures())) break;
            net.minecraft.world.InteractionResult result=net.minecraft.world.InteractionResult.PASS;
            if(entity!=null && player.isWithinEntityInteractionRange(entity.getEntity(),0)
                    && level.getWorldBorder().isWithinBounds(entity.getEntity().blockPosition())) {
                // UseEntityCallback находится в packet handler; у прямого interactOn его нет.
                result=net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker()
                        .interact(player,level,hand,entity.getEntity(),entity);
                if(result!=net.minecraft.world.InteractionResult.PASS) break;
                if(result==net.minecraft.world.InteractionResult.PASS)
                    result=player.interactOn(entity.getEntity(),hand,entity.getLocation().subtract(entity.getEntity().position()));
            } else if(wall!=null && from.distanceToSqr(wall.getLocation())<=blockRange*blockRange
                    && level.hasChunkAt(wall.getBlockPos()) && level.mayInteract(player,wall.getBlockPos())
                    && level.getWorldBorder().isWithinBounds(wall.getBlockPos())
                    && wall.getBlockPos().getY()>=level.getMinY() && wall.getBlockPos().getY()<=level.getMaxY()) {
                if(game.server().isUnderSpawnProtection(level,wall.getBlockPos(),player)) break;
                result=player.gameMode.useItemOn(player,level,stack,hand,wall);
                if(result.consumesAction()) net.minecraft.advancements.triggers.CriteriaTriggers.ANY_BLOCK_USE.trigger(player,wall.getBlockPos(),stack);
                if(result instanceof net.minecraft.world.InteractionResult.Fail) break;
            }
            mouseSwing(player,hand,stack,result);
            if(result.consumesAction()) break;
            if(!stack.isEmpty()) result=player.gameMode.useItem(player,level,stack,hand);
            mouseSwing(player,hand,stack,result);
            if(result.consumesAction()) break;
        }
        player.containerMenu.broadcastChanges();
    }

    private static void mouseSwing(ServerPlayer player,InteractionHand hand,ItemStack stack,net.minecraft.world.InteractionResult result) {
        if(result instanceof net.minecraft.world.InteractionResult.Success success && success.shouldSwing())
            player.swingAndResetAttackStrength(hand,stack.getInteractAnimation(),true);
    }

    /** Only cursed owners are visited. Local stopper queries run only while a curse is active. */
    public void tick() {
        timeStop.tick();witchPassives.tick();apocalypseEvents.tick();apocalypseActors.tick();shishEncounter.tick();
        nukeEvent.tick();foodLifecycle.tick();oreshek.tick();legacyEffects.tick();
        skyHazards.tick();
        lightningAcquisition.tick();
        nuclearVisuals.tick();
        rifleZoom.tick();
        if(game.maps().level()!=null && timeStop.stopped(game.maps().level())) {
            armorSets.techno().pausedMaintenance();
            armorSets.chaos().pausedMaintenance();
            exasperato.blastTick();
            // Исключения Stand: кулаки двигаются, ножи лишь выпускаются и ждут native thaw.
            blackHoleVisuals.tick();
            stand.tick(); shooters.tickPhantomBursts();
            return;
        }
        academy.tick();
        indirectKills.tick();
        magic.flushCleansing();
        for(var player:game.server().getPlayerList().getPlayers())iceMaterials.tickPlayer(player);
        mana.tick();
        magic.tick();
        frost.tick();
        clouds.tick();
        electro.tick();
        corruptedGlyphs.tick();
        iceWand.tick();
        voidArcanum.tick();
        defensiveArcanums.tick();
        LbcGreed.tick(game);
        babylonEffects.tick();
        babylon.tick();
        fartus.tick();
        halloweenWind.tick();
        soulhunter.tick();
        soulless.tick();
        danmaku.tick();
        sauvojen.tick();
        chargedSwords.tick();
        karaSlabs.tick();
        sheep.tick();
        hedgehog.tick();
        godHedgehog.tick();
        hedgehogSpikes.tick();
        wtf.tick();
        acidCorruption.tick();
        erecto.tick();
        wands.tick();
        terra.tick();
        exasperato.tick();
        exoperso.tick();
        stone.tick();
        bedrock.tick();
        magicBean.tick();
        edibleSpells.tick();
        foodStatuses.tick();
        stonekivi.tick();
        tanner.tick();
        kiuas.tick();
        blaze.tick();
        psycho.tick();
        summons.tick();
        syringe.tick();
        homing.tick();
        libraryRandom.tick();
        bolts.tick();
        blind.tick();
        air.tick();
        teleport.tick();
        ruby.tick();
        consuming.tick();
        traveller.tick();
        ordinaryAccessories.tick();
        armorSets.tick();
        spellSources.tick();
        chaosTrapActors.tick();
        sauvojenBoss.tick();
        infinity.tick();
        shulkerArmor.tick();
        classicMelee.tick();
        pendingCombat.tick();
        pendingSwords.tick();
        incomingArtifacts.tick();
        reactiveArtifacts.tick();
        moiseiBoots.tick();
        regenerationBelt.tick();
        quiver.tick();
        inventoryHazards.tick();
        deathCompass.tick();
        malevich.tick();
        metalPipe.tick();
        enderWiki.tick();
        prism.tick();
        awakenedAcid.tick();
        gaycasterAcademy.tick();
        advancedSnow.tick();
        neutral.tick();
        nyauwuCrafting.tick();
        evokerBass.tick();
        lifestealer.tick();
        mobgun.tick();
        duplicating.tick();
        blackHoleVisuals.tick();
        lifetimes.tick();
        shooters.tick();
        vexSword.tick();
        specialTools.tick();
        totems.tick();
        laserMinigun.tick();
        magnum.tick();
        govnomet.tick();
        stand.tick();
        kfcFood.tick();
        helicopterEncounter.tick();
        antigravity.tick();
        ultraponos.tick();
        reactive.tick();
        hohlopitek.tick();
        transformator.tick();
        stalin.tick();
        specialDrops.tick();
        portalDrops.tick();
        luckyPigs.tick();
        libraryDrop.tick();
        beeDrops.tick();
        opAnvilDrops.tick();
        slabDrops.tick();
        creatureDrops.tick();
        potionSpawnerDrops.tick();
        droneDrops.tick();
        antDrops.tick();
        tntDrops.tick();
        creeperDrops.tick();
        cageDrops.tick();
        zombieTntDrops.tick();
        fallingNetheriteDrops.tick();
        homes.tick();
        kfcRewards.tick();
        tickDynamites();
        tickNanoTraps();
        tickRockets();
        tickRocketCrafts();
        for(var player:game.server().getPlayerList().getPlayers()) { aimbobRewards.tickPlayer(player);refreshHud(player); }
        if (turnCurses.isEmpty()) return;
        // Reuse a bounded snapshot: damage can synchronously remove an owner through AFTER_DEATH.
        tickBatch.addAll(turnCurses.values());
        for (TurnCurse curse : tickBatch) {
            ServerPlayer owner = curse.owner;
            UUID id = owner.getUUID();
            if (turnCurses.get(id) != curse) continue;
            if (!active(owner) || !owner.isAlive() || owner.isRemoved()) { clear(owner); continue; }
            if (magic.rejectCustomEffect(owner)) { clearEffects(owner); continue; }
            // Author: this psychic disorder is unaffected by either jammer.
            Rotation current = new Rotation(Mth.floor(owner.getYRot()), Mth.floor(owner.getXRot()));
            Rotation previous = rotations.getOrDefault(id, ZERO_ROTATION);
            int yaw = current.yaw - previous.yaw, pitch = current.pitch - previous.pitch;
            // Original commands exclude yaw jumps >=180; pitch has no such exclusion.
            if (Math.abs((long) yaw) > 7 && Math.abs((long) yaw) < 180) punish(owner);
            if (Math.abs((long) pitch) > 7) punish(owner);
            rotations.put(id, current);
            if (game.players().get(id).lbcUnlocks.add("lbc:true_advancements/potions/effect_skill_issue")) game.players().save();
            if (--curse.remaining <= 0) turnCurses.remove(id, curse);
            mana.score(owner,"skill_issue").set(Math.max(0,curse.remaining));
        }
        tickBatch.clear();
    }

    private static void punish(ServerPlayer player) {
        player.hurtServer(player.level(), player.damageSources().generic(), 1);
        player.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS, 40, 0));
    }

    /** Only gameplay counts/deadlines; published HUD caches and tick clocks are never incremented. */
    void addPlayerCounters(ServerPlayer player) {
        if(!magic.rejectCustomEffect(player)){
            var curse=turnCurses.computeIfAbsent(player.getUUID(),ignored->new TurnCurse(player));curse.remaining++;
            mana.score(player,"skill_issue").set(curse.remaining);
        }
        var rocket=rockets.get(player.getUUID());
        if(rocket!=null) {
            rocket.jumps++;
            if(rocket.nextCharge!=0) rocket.nextCharge++;
            syncRocketGlider(player.getItemBySlot(EquipmentSlot.FEET),rocket.jumps>0);
            rocketHud(rocket);
        }
    }

    /** Pop-It removes only the turn curse; rotation history retains its existing match lifetime. */
    void clearTurnCurse(ServerPlayer player) {
        turnCurses.remove(player.getUUID());
        mana.score(player,"skill_issue").set(0);
    }
    public int curseTicks(ServerPlayer player) {
        var curse = turnCurses.get(player.getUUID());
        return curse == null ? 0 : curse.remaining;
    }

    public void clear(ServerPlayer player) { clear(player,true); }
    // A connection unload is not source purify: keep authoritative Nanobots scoreboard state.
    private void clear(ServerPlayer player,boolean resetNanobots) { legacyEffects.clear(player); if(resetNanobots){armorSets.nanobots().reset(player);armorSets.techno().purify(player);} moiseiBoots.clear(player); quiver.clear(player); pendingCombat.clearEffects(player); reactiveArtifacts.clearEffects(player); chargedSwords.clear(player); fevgaleaStar.clear(player); specialTools.clear(player); totems.clear(player); halloweenWind.purify(player); soulhunter.purify(player); soulless.purify(player); fartus.purify(player); babylonEffects.clear(player); acidCorruption.clear(player); erecto.clearRay(player); awakenedAcid.clear(player); kiuas.clearCurse(player); hohlopitek.clear(player); vexSword.purify(player); reactive.clear(player); shooters.resetBossHelicopterCooldown(player); magnum.purify(player); clearTurnCurse(player); kfcFood.clear(player); foodStatuses.clear(player); frost.clear(player); shooters.clearStun(player); magic.colors().clearEffects(player); mana.clearEffects(player); electro.clearEffects(player); blaze.clearEffects(player); psycho.clearEffects(player); homing.clearEffects(player); bolts.clearEffects(player); }
    public void forget(ServerPlayer player) {
        oreshek.resetBoundary(player);
        corruptedGlyphs.forget(player);
        iceWand.forget(player);
        voidArcanum.forget(player);
        babylon.forget(player);
        fartus.forget(player);
        halloweenWind.forget(player);
        soulhunter.forget(player);
        soulless.forget(player);
        danmaku.forget(player);
        sauvojen.forget(player);
        fevgaleaStar.forget(player);
        chargedSwords.forget(player);
        karaSlabs.forget(player);
        sheep.forget(player.getUUID());
        hedgehog.forget(player);
        hedgehogSpikes.forget(player);
        wtf.forget(player);
        babylonEffects.forget(player);
        defensiveArcanums.forget(player);
        acidCorruption.forget(player);
        erecto.forget(player);
        awakenedAcid.clear(player);
        hohlopitek.forget(player);
        blind.forget(player);
        psycho.forget(player);
        syringe.forget(player);
        summons.forget(player);
        homing.forget(player);
        libraryRandom.forget(player);
        bolts.forget(player);
        consuming.forget(player);
        prism.forget(player);
        gaycasterAcademy.forget(player);
        kiuas.clearCurse(player);
        advancedSnow.forget(player);
        neutral.forget(player);
        evokerBass.forget(player);
        lifestealer.forget(player);
        mobgun.forget(player);
        duplicating.forget(player);
        blackHoleVisuals.forget(player);
        teleport.forget(player);
        electro.forget(player);
        shooters.forget(player);
        laserMinigun.forget(player);
        ak47.forget(player);
        vexSword.forget(player);
        totems.clear(player);
        magnum.forget(player);
        rifleZoom.forget(player);
        govnomet.forget(player);
        LbcMinigunMovement.clear(player);
        stand.forget(player);
        timeStop.forget(player);
        blaze.forget(player);
        kfcFood.forget(player);
        kfcRewards.clear(player);
        inventoryHazards.forget(player);
        malevich.forget(player);
        metalPipe.forget(player);
        pendingCombat.forget(player);
        pendingSwords.forget(player);
        incomingArtifacts.forget(player);
        shulkerArmor.forget(player);
        armorSets.forget(player);
        witchPassives.forget(player);spellSources.forget(player);apocalypseEvents.forget(player);apocalypseActors.forget(player);shishEncounter.forget(player);
        sauvojenBoss.forget(player);
        infinity.forget(player);
        aimbobRewards.forget(player);
        edibleSpells.forget(player);
        nukeEvent.forget(player);foodLifecycle.forget(player);legacyEffects.forget(player);
        foodStatuses.forget(player);
        ownedEffects.forget(player);
        moiseiBoots.forget(player);
        regenerationBelt.forget(player);
        quiver.forget(player);
        reactiveArtifacts.forget(player);
        indirectKills.clear(player);
        mana.forget(player);
        magic.clear(player);
        resetRocket(player);
        clear(player,false); rotations.remove(player.getUUID()); resetAttributes(player);
        if(hudStates.remove(player.getUUID())!=null) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
    }
    public void stop() {
        SignedEnvironmentalEffects.clear();
        timeStop.stop();
        academy.stop();
        indirectKills.stop();
        magic.stop();
        mana.stop();
        frost.stop();
        clouds.stop();
        corruptedGlyphs.stop();
        iceWand.stop();
        voidArcanum.stop();
        babylon.stop();
        fartus.stop();
        halloweenWind.stop();
        soulhunter.stop();
        soulless.stop();
        danmaku.stop();
        sauvojen.stop();
        fevgaleaStar.stop();
        chargedSwords.stop();
        karaSlabs.stop();
        sheep.stop();
        hedgehog.stop();
        godHedgehog.stop();
        hedgehogSpikes.stop();
        wtf.stop();
        babylonEffects.stop();
        defensiveArcanums.stop();
        acidCorruption.stop();
        erecto.stop();
        wands.stop();
        terra.stop();
        exasperato.stop();
        exoperso.stop();
        stone.stop();
        bedrock.stop();
        stonekivi.stop();
        tanner.stop();
        kiuas.stop();
        blaze.stop();
        traveller.stop();
        ordinaryAccessories.stop();
        armorSets.stop();
        ownedEffects.stop();
        shulkerArmor.stop();
        classicMelee.stop();
        pendingCombat.stop();
        pendingSwords.stop();
        incomingArtifacts.stop();
        moiseiBoots.stop();
        regenerationBelt.stop();
        quiver.stop();
        reactiveArtifacts.stop();
        inventoryHazards.stop();
        deathCompass.stop();
        malevich.stop();
        metalPipe.stop();
        psycho.stop();
        syringe.stop();
        communism.stop();
        specialTools.stop();
        totems.stop();
        summons.stop();
        homing.stop();
        libraryRandom.stop();
        bolts.stop();
        lightningAcquisition.stop();
        blind.stop();
        air.stop();
        teleport.stop();
        ruby.stop();
        consuming.stop();
        prism.stop();
        awakenedAcid.stop();
        gaycasterAcademy.stop();
        advancedSnow.stop();
        neutral.stop();
        nyauwuCrafting.stop();
        evokerBass.stop();
        lifestealer.stop();
        mobgun.stop();
        duplicating.stop();
        blackHoleVisuals.stop();
        lifetimes.stop();
        shooters.stop();
        laserMinigun.stop();
        ak47.stop();
        vexSword.stop();
        magnum.stop();
        rifleZoom.stop();
        govnomet.stop();
        for(var player:game.server().getPlayerList().getPlayers()) LbcMinigunMovement.clear(player);
        stand.stop();
        kfcFood.stop();
        helicopterEncounter.stop();
        specialDrops.stop();
        chaosTrapActors.stop();
        sauvojenBoss.stop();
        infinity.stop();
        aimbobRewards.stop();
        magicBean.stop();
        edibleSpells.stop();
        nukeEvent.stop();foodLifecycle.stop();oreshek.stop();legacyEffects.stop();
        foodStatuses.stop();
        witchPassives.stop();apocalypseEvents.stop();apocalypseActors.stop();shishEncounter.stop();spellSources.stop();
        pisaDrops.stop();
        antigravity.stop();
        ultraponos.stop();
        reactive.stop();
        hohlopitek.stop();
        transformator.stop();
        stalin.stop();
        skyHazards.stop();
        portalDrops.stop();
        luckyPigs.stop();
        libraryDrop.stop();
        beeDrops.stop();
        opAnvilDrops.stop();
        slabDrops.stop();
        creatureDrops.stop();
        potionSpawnerDrops.stop();
        droneDrops.stop();
        antDrops.stop();
        corrosionDrops.stop();
        tntDrops.stop();
        creeperDrops.stop();
        cageDrops.stop();
        zombieTntDrops.stop();
        fallingNetheriteDrops.stop();
        homes.stop();
        kfcRewards.stop();
        electro.stop();
        trinkets.stop();
        for (RocketState state : List.copyOf(rockets.values())) resetRocket(state.owner);
        rockets.clear(); rechargingRockets.clear(); rocketCraftDrops.clear();
        for(var player:game.server().getPlayerList().getPlayers())
            if(hudStates.remove(player.getUUID())!=null) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
        turnCurses.clear(); rotations.clear();
        // Удаление вызывает ENTITY_UNLOAD; очищаем коллекции до discard, чтобы обход был устойчивым.
        entityBatch.addAll(pendingThrows); entityBatch.addAll(dynamites);
        pendingThrows.clear(); dynamites.clear();
        for (Entity entity : entityBatch) entity.discard();
        entityBatch.clear();
        discardNanoTraps();
        for (UUID id : List.copyOf(originalNanoAttributes.keySet())) {
            ServerPlayer player = game.server().getPlayerList().getPlayer(id);
            if (player != null) resetAttributes(player);
            else originalNanoAttributes.remove(id);
        }
        entityEpochTag = newEntityEpochTag();
    }
}
