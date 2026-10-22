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
        stand.onAcceptedDamage(victim,source);
        summons.damaged(victim,source);
        shooters.damaged(victim,source);
        if (!game.playing() && !debugItemUseWindow() || !inArena(victim) || LbcDamage.isArrowDamage(source)
                || !(source.getEntity() instanceof ServerPlayer owner) || !active(owner)
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
    public static void register() { LbcSkills.register(); LbcClouds.register(); LbcTrinkets.register(); LbcFrost.register(); LbcMagic.register(); LbcMana.register(); LbcWands.register(); LbcLifestealer.register(); LbcMobgun.register(); LbcTerra.register(); LbcShooters.register(); LbcLaserMinigun.register(); LbcAk47.register(); LbcMagnum.register(); LbcGovnomet.register(); LbcStand.register(); LbcKfcFood.register(); LbcKfcRewards.register(); LbcReforgeMachineItem.register(); LbcHelicopterTriggerItem.register(); LbcChestDropItem.register(); LbcTntDropItem.register(); LbcCreeperDropItem.register(); LbcCageDropItem.register(); LbcHoleDropItem.register(); LbcCorrosionDropItem.register(); LbcZombieTntDropItem.register(); LbcFallingNetheriteDropItem.register(); LbcHomeItems.register(); LbcScareDropItem.register(); LbcLuckyPigDropItem.register(); LbcLevitationDropItem.register(); LbcElectro.register(); LbcExasperato.register(); LbcExoperso.register(); LbcStone.register(); LbcBlind.register(); LbcPsycho.register(); LbcAir.register(); LbcTeleport.register(); LbcRuby.register(); LbcBolts.register(); LbcAcademy.register(); LbcTanner.register(); LbcKiuas.register(); }

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
    private final LbcLifestealer lifestealer;
    private final LbcMobgun mobgun;
    private final LbcStonekivi stonekivi;
    private final LbcTanner tanner;
    private final LbcKiuas kiuas;
    private final LbcBlaze blaze;
    private final LbcPsycho psycho;
    private final LbcSummons summons;
    private final LbcHoming homing;
    private final LbcBolts bolts;
    private final LbcLightningAcquisition lightningAcquisition;
    private final LbcShooters shooters;
    private final LbcLaserMinigun laserMinigun;
    private final LbcAk47 ak47;
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
    private record HudState(UUID source,int mana,int max,int regeneration,int jumps,int cooldown,
                            int cold,int coldCooldown,int frozen,int swordCharges,int noMagic,int reverse,int curse,
                            int addMana,int addMax,int addRegen,int removeMana,int boost,int teleport,
                            int katanaCharges,int katanaCharging,int yellowCharges,int yellowCharging,int shock,int blindQueue,int blazeHead,int lookback,int piss,int scared,int screamray,int twitching,int targetCurse,int divider,int helicopter,int bossHelicopter,int akAmmo,int sentAt) { }
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
    /** Complete native counterpart of the supported portion of source ui/get_data. */
    MutableComponent hud(ServerPlayer source) {
        var component=Component.empty().append(mana.hud(source));
        int shock=electro.shocks(source);
        if(shock>0) component.append(Component.literal("  \uE121 "))
                .append(Component.literal(Integer.toString(shock)).withStyle(ChatFormatting.RED));
        int cold=frost.cold(source), coldCooldown=frost.cooldown(source), frozen=frost.frozenSeconds(source);
        if(cold>0) component.append(Component.literal("  \uE137 §fx")).append(Component.literal(Integer.toString(cold)).withStyle(ChatFormatting.RED));
        if(coldCooldown>0) component.append(Component.literal(" §f(").withStyle(ChatFormatting.RED))
                .append(Component.literal(Integer.toString(coldCooldown)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("§fs)").withStyle(ChatFormatting.RED));
        if(frozen>0) component.append(Component.literal("  \uE138 ")).append(Component.literal(Integer.toString(frozen)).withStyle(ChatFormatting.RED));
        int katanaCharges=electro.hudCharges(source);
        if(katanaCharges>=0) component.append(Component.literal("    \uE103 §fx"))
                .append(Component.literal(Integer.toString(katanaCharges)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" [")).append(Component.literal(Integer.toString(electro.charging(source))).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("/§b10§f]"));
        int swordCharges=terra.hudCharges(source);
        if(swordCharges>=0) component.append(Component.literal("    \uE103 §fx"))
                .append(Component.literal(Integer.toString(swordCharges)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" [")).append(Component.literal("0").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("/§a1§f]"));
        int yellowCharges=electro.yellowHudCharges(source);
        if(yellowCharges>=0) component.append(Component.literal("    \uE103 §fx"))
                .append(Component.literal(Integer.toString(yellowCharges)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" ["))
                .append(Component.literal(Integer.toString(electro.yellowCharging(source))).withColor(0xA55E50))
                .append(Component.literal("/"))
                .append(Component.literal("7§f]").withColor(0xA55E50));
        int blindQueue=blind.queued(source);
        int helicopter=helicopterHudValue(source);
        if(helicopter>=0) component.append(Component.literal(helicopter>0?"    \uE105 ":"    \uE104 "))
                .append(helicopter>0?Component.literal(Integer.toString(helicopter)).withStyle(ChatFormatting.WHITE)
                        :Component.translatable("ready").withStyle(ChatFormatting.GREEN));
        int bossHelicopter=bossHelicopterHudValue(source);
        if(bossHelicopter>=0) component.append(Component.literal(bossHelicopter>0?"    \uE105 ":"    \uE104 "))
                .append(bossHelicopter>0?Component.literal(Integer.toString(bossHelicopter)).withStyle(ChatFormatting.WHITE)
                        :Component.translatable("ready").withStyle(ChatFormatting.GREEN));
        int divider=dividerHudValue(source);
        if(divider>=0) component.append(Component.literal(divider>0?"    \uE105 ":"    \uE104 "))
                .append(divider>0?Component.literal(Integer.toString(divider)).withStyle(ChatFormatting.WHITE)
                        :Component.translatable("ready").withStyle(ChatFormatting.GREEN));
        if(blindQueue>0) component.append(Component.literal("  \uE112 ")).append(Component.literal(Integer.toString(blindQueue)).withStyle(ChatFormatting.GREEN));
        mana.appendHudEarlyStatuses(source,component);
        int blazeHead=blaze.headCharges(source);
        if(blazeHead>0) component.append(Component.literal("  \uE110 "))
                .append(Component.literal(Integer.toString(blazeHead)).withStyle(ChatFormatting.GREEN));
        int lookback=psycho.hudValue(source,"lookback"),piss=psycho.hudValue(source,"piss");
        int scared=psycho.hudValue(source,"scared"),screamray=psycho.hudValue(source,"screamray"),twitching=psycho.hudValue(source,"twitching");
        if(lookback>0) component.append(Component.literal("  \uE113 ")).append(Component.literal(Integer.toString(lookback)).withStyle(ChatFormatting.RED));
        if(piss>0) component.append(Component.literal("  \uE116 ")).append(Component.literal(Integer.toString(piss)).withStyle(ChatFormatting.RED));
        if(scared>0) component.append(Component.literal("  \uE119 ")).append(Component.literal(Integer.toString(scared)).withStyle(ChatFormatting.RED));
        if(screamray>0) component.append(Component.literal("  \uE120 ")).append(Component.literal(Integer.toString(screamray)).withStyle(ChatFormatting.GREEN));
        if(twitching>0) component.append(Component.literal(" \uE123 ")).append(Component.literal(Integer.toString(twitching)).withStyle(ChatFormatting.RED));
        int targetCurse=homing.curseSeconds(source);
        if(targetCurse>0) component.append(Component.literal("  \uE122 ")).append(Component.literal(Integer.toString(targetCurse)).withStyle(ChatFormatting.RED));
        int akAmmo=akAmmoHudValue(source);
        if(akAmmo>=0) component.append(Component.literal("  \uE206 ")).append(Component.literal(Integer.toString(akAmmo)).withStyle(ChatFormatting.WHITE));
        int curse=curseTicks(source);
        if(curse>0) component.append(Component.literal("  \uE129 ")).append(Component.literal(Integer.toString(curse)).withStyle(ChatFormatting.RED));
        mana.appendHudManaStatuses(source,component);
        int teleport=mana.hudValue(source,"effect_rtp_1");
        if(teleport>0) component.append(Component.literal("  \uE142 ")).append(Component.literal(Integer.toString(teleport)).withStyle(ChatFormatting.WHITE));
        var rocket=rockets.get(source.getUUID());
        if(rocket!=null) component.append(rocketHudComponent(rocket));
        return component;
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
        var rocket=rockets.get(source.getUUID()); int jumps=rocket==null ? -1 : rocket.jumps;
        int cooldown=rocket==null ? 0 : (rocketCooldownTicks(source)+19)/20;
        int current=mana.hudValue(source,"mana"), max=mana.hudValue(source,"manamax"), regeneration=mana.hudValue(source,"manaregen_visual");
        int cold=frost.cold(source), coldCooldown=frost.cooldown(source), frozen=frost.frozenSeconds(source);
        int swordCharges=terra.hudCharges(source);
        int katanaCharges=electro.hudCharges(source),katanaCharging=electro.charging(source),shock=electro.shocks(source);
        int yellowCharges=electro.yellowHudCharges(source),yellowCharging=electro.yellowCharging(source);
        int blindQueue=blind.queued(source),blazeHead=blaze.headCharges(source);
        int noMagic=mana.hudValue(source,"nomagic"), reverse=mana.hudValue(source,"reverse_mana"), curse=curseTicks(source);
        int addMana=mana.hudValue(source,"effect_add_mana"), addMax=mana.hudValue(source,"effect_add_manamax");
        int addRegen=mana.hudValue(source,"effect_add_manaregen"), removeMana=mana.hudValue(source,"effect_remove_mana");
        int boost=mana.hudValue(source,"effect_boost_mana"), teleport=mana.hudValue(source,"effect_rtp_1");
        int lookback=psycho.hudValue(source,"lookback"),piss=psycho.hudValue(source,"piss");
        int scared=psycho.hudValue(source,"scared"),screamray=psycho.hudValue(source,"screamray"),twitching=psycho.hudValue(source,"twitching");
        int targetCurse=homing.curseSeconds(source),divider=dividerHudValue(source),helicopter=helicopterHudValue(source);
        int bossHelicopter=bossHelicopterHudValue(source);
        int akAmmo=akAmmoHudValue(source);
        var old=hudStates.get(viewerId); int now=game.server().getTickCount(); UUID sourceId=source.getUUID();
        if(old!=null && old.source().equals(sourceId) && old.mana()==current && old.max()==max && old.regeneration()==regeneration
                && old.jumps()==jumps && old.cooldown()==cooldown && old.cold()==cold && old.coldCooldown()==coldCooldown
                && old.frozen()==frozen && old.noMagic()==noMagic && old.reverse()==reverse && old.curse()==curse
                && old.swordCharges()==swordCharges
                && old.katanaCharges()==katanaCharges && old.katanaCharging()==katanaCharging && old.shock()==shock
                && old.yellowCharges()==yellowCharges && old.yellowCharging()==yellowCharging && old.blindQueue()==blindQueue && old.blazeHead()==blazeHead
                && old.lookback()==lookback && old.piss()==piss && old.scared()==scared && old.screamray()==screamray && old.twitching()==twitching && old.targetCurse()==targetCurse
                && old.addMana()==addMana && old.addMax()==addMax && old.addRegen()==addRegen && old.removeMana()==removeMana
                && old.boost()==boost && old.teleport()==teleport && old.divider()==divider && old.helicopter()==helicopter && old.bossHelicopter()==bossHelicopter && old.akAmmo()==akAmmo && now-old.sentAt()<20) return;
        viewer.connection.send(new ClientboundSetActionBarTextPacket(hud(source)));
        hudStates.put(viewerId,new HudState(sourceId,current,max,regeneration,jumps,cooldown,cold,coldCooldown,frozen,
                swordCharges,noMagic,reverse,curse,addMana,addMax,addRegen,removeMana,boost,teleport,
                katanaCharges,katanaCharging,yellowCharges,yellowCharging,shock,blindQueue,blazeHead,lookback,piss,scared,screamray,twitching,targetCurse,divider,helicopter,bossHelicopter,akAmmo,now));
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
        this.summons = new LbcSummons(game);
        this.homing = new LbcHoming(game);
        this.bolts = new LbcBolts(game);
        this.lightningAcquisition = new LbcLightningAcquisition(game);
        this.summons.prepare();
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
    public LbcHoming homing() { return homing; }
    public LbcBolts bolts() { return bolts; }
    public LbcLightningAcquisition lightningAcquisition() { return lightningAcquisition; }
    public LbcTerra terra() { return terra; }
    public LbcShooters shooters() { return shooters; }
    public LbcLaserMinigun laserMinigun() { return laserMinigun; }
    public LbcAk47 ak47() { return ak47; }
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
                    || (LbcTntDrops.managed(source.getDirectEntity()) || LbcCreeperDrops.managed(source.getDirectEntity())
                        || LbcCageDrops.managed(source.getDirectEntity()))
                        && currentEpoch(source.getDirectEntity()))
                && (!(victim instanceof ServerPlayer player) || game.allowDamage(player))
                && wands.allowDamage(victim,source) && kfcRewards.allowDamage(victim,source)
                && helicopterEncounter.allowDamage(victim,source) && tntDrops.allowDamage(victim,source) && creeperDrops.allowDamage(victim,source) && zombieTntDrops.canAffect(source.getDirectEntity(),victim)
                && luckyPigs.canAffect(source.getDirectEntity(),victim);
    }
    public LbcKfcFood kfcFood() { return kfcFood; }
    public LbcKfcRewards kfcRewards() { return kfcRewards; }
    public LbcHelicopterEncounter helicopterEncounter() { return helicopterEncounter; }
    public LbcSpecialDrops chestDrops() { return specialDrops; }
    public LbcSpecialDrops specialDrops() { return specialDrops; }
    public LbcLuckyPigs luckyPigs() { return luckyPigs; }
    public LbcEnzanDrop enzanDrop() { return enzanDrop; }
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
        if (consuming.eraseLoaded(entity)) return;
        mobgun.claimChild(entity);
        frost.loaded(entity);
        if (hasStaleItemEpoch(entity)) {
            entity.discard();
            return;
        }
        helicopterEncounter.loaded(entity);
        specialDrops.loaded(entity);
        luckyPigs.loaded(entity);
        tntDrops.loaded(entity);
        creeperDrops.loaded(entity);
        cageDrops.loaded(entity);
        zombieTntDrops.loaded(entity);
        fallingNetheriteDrops.loaded(entity);
        homes.loaded(entity);
        lightningAcquisition.loaded(entity);
        terra.loaded(entity);
        lifetimes.loaded(entity);
        exasperato.loaded(entity);
        exoperso.loaded(entity);
        stone.loaded(entity);
        blind.loaded(entity);
        air.loaded(entity);
        teleport.loaded(entity);
        ruby.loaded(entity);
        consuming.loaded(entity);
        lifestealer.loaded(entity);
        govnomet.loaded(entity);
        mobgun.loaded(entity);
        if (academy.loaded(entity)) return;
        if (shooters.loaded(entity)) { markCurrentItemEpoch(entity); return; }
        if (magic.loaded(entity)) { markCurrentItemEpoch(entity); return; }
        if (clouds.loaded(entity)) { markCurrentItemEpoch(entity); return; }
        blaze.loaded(entity);
        psycho.loaded(entity);
        summons.loaded(entity);
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
        electro.transformOrigins(level,transform);
        bolts.transformOrigins(level,transform);
        blaze.transformOrigins(level,transform);
        blind.transformOrigins(level,transform);
        homing.transformOrigins(level,transform);
        duplicating.transformOrigins(level,transform);
        stand.transformOrigins(level,transform);
    }
    public void entityUnloaded(Entity entity) {
        specialDrops.unloaded(entity);
        luckyPigs.unloaded(entity);
        tntDrops.unloaded(entity);
        creeperDrops.unloaded(entity);
        cageDrops.unloaded(entity);
        zombieTntDrops.unloaded(entity);
        fallingNetheriteDrops.unloaded(entity);
        homes.unloaded(entity);
        lightningAcquisition.unloaded(entity);
        govnomet.unloaded(entity);
        academy.unloaded(entity);
        lifetimes.unloaded(entity);
        exasperato.unloaded(entity);
        exoperso.unloaded(entity);
        stone.unloaded(entity);
        blind.unloaded(entity);
        air.unloaded(entity);
        teleport.unloaded(entity);
        ruby.unloaded(entity);
        consuming.unloaded(entity);
        lifestealer.unloaded(entity);
        mobgun.unloaded(entity);
        terra.unloaded(entity);
        shooters.unloaded(entity);
        stand.unloaded(entity);
        helicopterEncounter.entityUnloaded(entity);
        kfcRewards.entityUnloaded(entity);
        magic.unloaded(entity);
        frost.unloaded(entity);
        clouds.unloaded(entity);
        trinkets.unloaded(entity);
        blaze.unloaded(entity);
        psycho.unloaded(entity);
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

    private boolean hasStaleItemEpoch(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !tag.equals(entityEpochTag)) return true;
        }
        return false;
    }

    void markCurrentItemEpoch(Entity entity) { entity.entityTags().add(entityEpochTag); }

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
        // Сначала снимаем источник повторного наложения и привязку ко льду, затем нативные эффекты.
        // Экипировка, выбранные перки, заряды способностей и постоянные атрибуты не являются статусами.
        clear(player);
        player.entityTags().remove("lightningist");
        player.entityTags().remove("greed_cursed");
        if (!player.getActiveEffects().isEmpty()) player.removeAllEffects();
    }

    /** Native used_totem runs before vanilla death-protection effects clear all potions. */
    public void usedTotem(ServerPlayer player, ItemStack used) {
        var data = used.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.matchedBy(BROKEN_TOTEM) || !active(player)) return;
        screamer(player,null);
        if (!magic.rejectCustomEffect(player))
            turnCurses.computeIfAbsent(player.getUUID(), ignored -> new TurnCurse(player)).remaining += 100;
    }

    /** Один исходный скример для сломанного тотема и психических жезлов; проклятие тотема отдельно. */
    public void screamer(LivingEntity target, ServerPlayer owner) {
        screamer(target,owner,false);
    }

    /** The dropped legacy variant keeps its separate sound volume, smoke and duration. */
    public void screamer(LivingEntity target, ServerPlayer owner, boolean old) {
        int duration=old ? 200 : 120;
        if (!(target instanceof ServerPlayer player)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,duration,10,false,false),owner);
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
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 10, false, false),owner);
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
        timeStop.tick();
        lightningAcquisition.tick();
        nuclearVisuals.tick();
        rifleZoom.tick();
        if(game.maps().level()!=null && timeStop.stopped(game.maps().level())) {
            // Исключения Stand: кулаки двигаются, ножи лишь выпускаются и ждут native thaw.
            blackHoleVisuals.tick();
            stand.tick(); shooters.tickPhantomBursts();
            return;
        }
        academy.tick();
        indirectKills.tick();
        magic.flushCleansing();
        mana.tick();
        magic.tick();
        frost.tick();
        clouds.tick();
        electro.tick();
        wands.tick();
        terra.tick();
        exasperato.tick();
        exoperso.tick();
        stone.tick();
        bedrock.tick();
        stonekivi.tick();
        tanner.tick();
        kiuas.tick();
        blaze.tick();
        psycho.tick();
        summons.tick();
        homing.tick();
        bolts.tick();
        blind.tick();
        air.tick();
        teleport.tick();
        ruby.tick();
        consuming.tick();
        lifestealer.tick();
        mobgun.tick();
        duplicating.tick();
        blackHoleVisuals.tick();
        lifetimes.tick();
        shooters.tick();
        laserMinigun.tick();
        magnum.tick();
        govnomet.tick();
        stand.tick();
        kfcFood.tick();
        helicopterEncounter.tick();
        specialDrops.tick();
        luckyPigs.tick();
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
        for(var player:game.server().getPlayerList().getPlayers()) refreshHud(player);
        if (turnCurses.isEmpty()) return;
        // Reuse a bounded snapshot: damage can synchronously remove an owner through AFTER_DEATH.
        tickBatch.addAll(turnCurses.values());
        for (TurnCurse curse : tickBatch) {
            ServerPlayer owner = curse.owner;
            UUID id = owner.getUUID();
            if (turnCurses.get(id) != curse) continue;
            if (!active(owner) || !owner.isAlive() || owner.isRemoved()) { clear(owner); continue; }
            if (magic.rejectCustomEffect(owner)) { clearEffects(owner); continue; }
            if (magic.beaconSuppressed(owner.position()) || !owner.level().getEntitiesOfClass(Marker.class, owner.getBoundingBox().inflate(20), marker ->
                    owner.distanceToSqr(marker) <= 400
                            && (marker.entityTags().contains("stopper_magic") || marker.entityTags().contains("stopper"))).isEmpty()) continue;
            Rotation current = new Rotation(Mth.floor(owner.getYRot()), Mth.floor(owner.getXRot()));
            Rotation previous = rotations.getOrDefault(id, ZERO_ROTATION);
            int yaw = current.yaw - previous.yaw, pitch = current.pitch - previous.pitch;
            // Original commands exclude yaw jumps >=180; pitch has no such exclusion.
            if (Math.abs((long) yaw) > 7 && Math.abs((long) yaw) < 180) punish(owner);
            if (Math.abs((long) pitch) > 7) punish(owner);
            rotations.put(id, current);
            if (game.players().get(id).lbcUnlocks.add("lbc:true_advancements/potions/effect_skill_issue")) game.players().save();
            if (--curse.remaining <= 0) turnCurses.remove(id, curse);
        }
        tickBatch.clear();
    }

    private static void punish(ServerPlayer player) {
        player.hurtServer(player.level(), player.damageSources().generic(), 1);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
    }

    /** Only gameplay counts/deadlines; published HUD caches and tick clocks are never incremented. */
    void addPlayerCounters(ServerPlayer player) {
        if(!magic.rejectCustomEffect(player))
            turnCurses.computeIfAbsent(player.getUUID(),ignored->new TurnCurse(player)).remaining++;
        var rocket=rockets.get(player.getUUID());
        if(rocket!=null) {
            rocket.jumps++;
            if(rocket.nextCharge!=0) rocket.nextCharge++;
            syncRocketGlider(player.getItemBySlot(EquipmentSlot.FEET),rocket.jumps>0);
            rocketHud(rocket);
        }
    }

    public int curseTicks(ServerPlayer player) {
        var curse = turnCurses.get(player.getUUID());
        return curse == null ? 0 : curse.remaining;
    }

    public void clear(ServerPlayer player) { shooters.resetBossHelicopterCooldown(player); magnum.purify(player); turnCurses.remove(player.getUUID()); kfcFood.clear(player); frost.clear(player); shooters.clearStun(player); magic.colors().clearEffects(player); mana.clearEffects(player); electro.clearEffects(player); blaze.clearEffects(player); psycho.clearEffects(player); homing.clearEffects(player); bolts.clearEffects(player); }
    public void forget(ServerPlayer player) {
        blind.forget(player);
        psycho.forget(player);
        summons.forget(player);
        homing.forget(player);
        bolts.forget(player);
        consuming.forget(player);
        lifestealer.forget(player);
        mobgun.forget(player);
        duplicating.forget(player);
        blackHoleVisuals.forget(player);
        teleport.forget(player);
        electro.forget(player);
        shooters.forget(player);
        laserMinigun.forget(player);
        ak47.forget(player);
        magnum.forget(player);
        rifleZoom.forget(player);
        govnomet.forget(player);
        LbcMinigunMovement.clear(player);
        stand.forget(player);
        timeStop.forget(player);
        blaze.forget(player);
        kfcFood.forget(player);
        kfcRewards.clear(player);
        indirectKills.clear(player);
        mana.forget(player);
        magic.clear(player);
        resetRocket(player);
        clear(player); rotations.remove(player.getUUID()); resetAttributes(player);
        if(hudStates.remove(player.getUUID())!=null) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
    }
    public void stop() {
        timeStop.stop();
        academy.stop();
        indirectKills.stop();
        magic.stop();
        mana.stop();
        frost.stop();
        clouds.stop();
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
        psycho.stop();
        summons.stop();
        homing.stop();
        bolts.stop();
        lightningAcquisition.stop();
        blind.stop();
        air.stop();
        teleport.stop();
        ruby.stop();
        consuming.stop();
        lifestealer.stop();
        mobgun.stop();
        duplicating.stop();
        blackHoleVisuals.stop();
        lifetimes.stop();
        shooters.stop();
        laserMinigun.stop();
        ak47.stop();
        magnum.stop();
        rifleZoom.stop();
        govnomet.stop();
        for(var player:game.server().getPlayerList().getPlayers()) LbcMinigunMovement.clear(player);
        stand.stop();
        kfcFood.stop();
        helicopterEncounter.stop();
        specialDrops.stop();
        luckyPigs.stop();
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
