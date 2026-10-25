package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Постройка Academy и событийные испытания уже перенесённых жезлов. */
public final class LbcAcademy {
    private static final Identifier TEMPLATE = Identifier.parse("lbc:magic_academy");
    private static final String ENTITY_TAG = "skywars2.academy";
    private static final String BUILDING_TAG = "skywars2.academy.building";
    public static final String SINGLE_USE_WELCOME = "welcome_to_single_use_academy";
    private static final String SINGLE_PANEL = "skywars2.academy.single_panel";
    private static final CompoundTag TOKEN_DATA = flag("academy_token"), BUILDING_DATA = flag("magic_academy");
    public static final Item TOKEN = registerToken();
    private enum Wand {
        HEDGEHOG("baffledhedgehog",LbcHedgehogItems.WAND,LbcHedgehogItems.ACADEMY,"s","upgrade_condition_baffledhedgehog","upgrade_succesful_baffledhedgehog"),
        SHEEP("sheepwand",LbcSheep.ITEM,LbcSheep.ACADEMY_ITEM,"x","upgrade_condition_sheepwand","upgrade_succesful_sheepwand"),
        DANMAKU("danmaku_staff",LbcDanmaku.ITEM,LbcDanmaku.ACADEMY,"s","upgrade_condition_danmaku_staff","upgrade_succesful_danmaku_staff"),
        ACID("stacid",LbcWands.ACID_WAND,LbcWands.ACADEMY_ACID_WAND,"a","upgrade_condition_stacid","upgrade_succesful_stacid"),
        SOULLESS("soulcorrupter",LbcSoulless.WAND,LbcSoulless.ACADEMY_WAND,"a","upgrade_condition_soulcorrupter","upgrade_succesful_soulcorrupter"),
        VEX_WIND("vex_wand",LbcHalloweenWind.WIND,LbcHalloweenWind.ACADEMY_WIND,"a","upgrade_condition_vex","upgrade_succesful_vex"),
        FARTUS("fartus_nadristailo",LbcFartus.ITEM,LbcFartus.ACADEMY,"x","upgrade_condition_fartus_nadristailo","upgrade_succesful_fartus_nadristailo"),
        ICE("ice_wand",LbcWands.ICE,LbcWands.ACADEMY_ICE,"ss","upgrade_condition_ice","upgrade_succesful_ice"),
        CORRUPTION("corrupter",LbcWands.CORRUPTION,LbcWands.ACADEMY_CORRUPTION,"c","upgrade_condition_corrupter","upgrade_succesful_corrupter"),
        ACID_CORRUPTION("acidcorrupt_wand",LbcWands.ACID_CORRUPTION,LbcWands.ACADEMY_ACID_CORRUPTION,"ss","upgrade_condition_acidcorrupt","upgrade_succesful_acidcorrupt"),
        ERECTO("erecto_wand",LbcWands.ERECTO,LbcWands.ACADEMY_ERECTO,"ss","upgrade_condition_erecto","upgrade_succesful_erecto"),
        TESLA("lightningwand",LbcWands.TESLA,LbcWands.ACADEMY_TESLA,"a","upgrade_condition_lightningwand","upgrade_succesful_lightningwand"),
        AWAKENED_ACID("acidacid_wand", LbcWands.AWAKENED_ACID, LbcWands.ACADEMY_AWAKENED_ACID, "ss", "upgrade_condition_acidacid", "upgrade_succesful_acidacid"),
        GAYCASTER("gaycaster", LbcWands.GAYCASTER, LbcWands.ACADEMY_GAYCASTER, "s", "upgrade_condition_gaycaster", "upgrade_succesful_gaycaster"),
        PRISM("prisma", LbcWands.PRISM, LbcWands.ACADEMY_PRISM, "ss", "upgrade_condition_prisma", "upgrade_succesful_prisma"),
        CAUSTIC("acidwand", LbcWands.CAUSTIC, LbcWands.ACADEMY_CAUSTIC, "c", "upgrade_condition_acid", "upgrade_succesful_acid"),
        OMEGA("omegaflag", LbcWands.OMEGA, LbcWands.ACADEMY_OMEGA, "b", "upgrade_condition_omegaflag", "upgrade_succesful_omegaflag"),
        ADVANCED_ENDER("advancedteleport", LbcWands.ADVANCED_ENDER, LbcWands.ACADEMY_ADVANCED_ENDER, "s", "upgrade_condition_advancedteleport", "upgrade_succesful_advancedteleport"),
        SNOW("snow_wand", LbcWands.SNOW, LbcWands.ACADEMY_SNOW, "b", "upgrade_condition_snow", "upgrade_succesful_snow"),
        ENDER("enderwand", LbcWands.ENDER, LbcWands.ACADEMY_ENDER, "a", "upgrade_condition_enderwand", "upgrade_succesful_enderwand"),
        BAD_WIKI("badwiki", LbcWands.BAD_WIKI, LbcWands.ACADEMY_BAD_WIKI, "a", "upgrade_condition_badwiki", "upgrade_succesful_badwiki"),
        NEUTRAL("neutralmage", LbcWands.NEUTRAL, LbcWands.ACADEMY_NEUTRAL, "a", "upgrade_condition_neutralmage", "upgrade_succesful_neutralmage"),
        BLOOD("bloodmagic", LbcWands.BLOOD, LbcWands.ACADEMY_BLOOD, "a", "upgrade_condition_bloodmagic", "upgrade_succesful_bloodmagic"),
        CONFUSE("foxusing", LbcWands.CONFUSE, LbcWands.ACADEMY_CONFUSE, "a", "upgrade_condition_foxusing", "upgrade_succesful_foxusing"),
        EVOKER("evocator", LbcWands.EVOKER, LbcWands.ACADEMY_EVOKER, "b", "upgrade_condition_evocator", "upgrade_succesful_evocator"),
        BASS("bassboost", LbcWands.BASS, LbcWands.ACADEMY_BASS, "b", "upgrade_condition_bassboost", "upgrade_succesful_bassboost"),
        LIFESTEALER("lifestealer", LbcWands.LIFESTEALER, LbcWands.ACADEMY_LIFESTEALER, "a", "upgrade_condition_lifestealer", "upgrade_succesful_lifestealer"),
        FIRE("stfire", LbcWands.FIRE_WAND, LbcWands.ACADEMY_FIRE, "a", "upgrade_condition_stfire", "upgrade_succesful_stfire"),
        BLAZE("fallingstars", LbcWands.BLAZE_WAND, LbcWands.ACADEMY_BLAZE, "a", "upgrade_condition_fallingstars", "upgrade_succesful_fallingstars"),
        FIREBLAZE("fireblaze_wand", LbcWands.FIREBLAZE, LbcWands.ACADEMY_FIREBLAZE, "s", "upgrade_condition_fireblaze", "upgrade_succesful_fireblaze"),
        FALLING_STARS("truefallingstars", LbcWands.FALLING_STARS_WAND, LbcWands.ACADEMY_FALLING_STARS, "a", "upgrade_condition_truefallingstars", "upgrade_succesful_truefallingstars"),
        LIGHTNING_BOLTS("lightning_bolts", LbcWands.LIGHTNING_BOLTS_WAND, LbcWands.ACADEMY_LIGHTNING_BOLTS, "s", "upgrade_condition_lightning_bolt", "upgrade_succesful_lightning_bolt"),
        BOLTBOLT("boltbolt_wand", LbcWands.BOLTBOLT, LbcWands.ACADEMY_BOLTBOLT, "ss", "upgrade_condition_boltbolt", "upgrade_succesful_boltbolt"),
        CONSUMING("consuming_wand", LbcWands.CONSUMING, LbcWands.ACADEMY_CONSUMING, "x", "upgrade_condition_consuming", "upgrade_succesful_consuming"),
        DUPLICATING("dublicating_wand", LbcWands.DUPLICATING, LbcWands.ACADEMY_DUPLICATING, "ss", "upgrade_condition_dublicating", "upgrade_succesful_dublicating"),
        EXAHOMING("exahoming_wand", LbcWands.EXAHOMING, LbcWands.ACADEMY_EXAHOMING, "x", "upgrade_condition_exahoming", "upgrade_succesful_exahoming"),
        STONE("ststone", LbcWands.STONE_WAND, LbcWands.ACADEMY_STONE, "a", "upgrade_condition_ststone", "upgrade_succesful_ststone"),
        BEDROCK("bedrock_wand", LbcWands.BEDROCK, LbcWands.ACADEMY_BEDROCK, "ss", "upgrade_condition_bedrock", "upgrade_succesful_bedrock"),
        WALL("wall", LbcWands.WALL, LbcWands.ACADEMY_WALL, "a", "upgrade_condition_wall", "upgrade_succesful_wall"),
        STONEKIVI("stonekivi_wand", LbcWands.STONEKIVI, LbcWands.ACADEMY_STONEKIVI, "x", "upgrade_condition_stonekivi", "upgrade_succesful_stonekivi"),
        PSYCHOWAND("psychowand", LbcWands.PSYCHOWAND, LbcWands.ACADEMY_PSYCHOWAND, "b", "upgrade_condition_psychowand", "upgrade_succesful_psychowand"),
        PSYCHOBLIND("psychoblind_wand", LbcWands.PSYCHOBLIND, LbcWands.ACADEMY_PSYCHOBLIND, "ss", "upgrade_condition_psychoblind", "upgrade_succesful_psychoblind"),
        BLIND("stblind", LbcWands.BLIND_WAND, LbcWands.ACADEMY_BLIND, "b", "upgrade_condition_stblind", "upgrade_succesful_stblind"),
        AIR("stair", LbcWands.AIR_WAND, LbcWands.ACADEMY_AIR, "s", "upgrade_condition_stair", "upgrade_succesful_stair"),
        TELEPORT("stteleport", LbcWands.TELEPORT_WAND, LbcWands.ACADEMY_TELEPORT, "a", "upgrade_condition_stteleport", "upgrade_succesful_stteleport"),
        RUBY("struby", LbcWands.RUBY_WAND, LbcWands.ACADEMY_RUBY, "b", "upgrade_condition_struby", "upgrade_succesful_struby"),
        EXOPERSO("exoperso", LbcWands.EXOPERSO, LbcWands.ACADEMY_EXOPERSO, "s", "upgrade_condition_exoperso", "upgrade_succesful_exoperso"),
        TERRA("stterra", LbcWands.TERRA, LbcWands.ACADEMY_TERRA, "a", "upgrade_condition_stterra", "upgrade_succesful_stterra"),
        ELECTRO("stelectro", LbcWands.ELECTRO, LbcWands.ACADEMY_ELECTRO, "a", "upgrade_condition_stelectro", "upgrade_succesful_stelectro"),
        EXASPERATO("exasperato", LbcWands.EXASPERATO, LbcWands.ACADEMY_EXASPERATO, "x", "upgrade_condition_exasperato", "upgrade_succesful_exasperato");
        final String key, flag, tier, conditionKey, successKey;
        final Item base, upgraded;
        Wand(String key, Item base, Item upgraded, String tier, String conditionKey, String successKey) {
            this.key = key; this.flag = key.equals("exasperato") ? "exasperato_wand" : key;
            this.base = base; this.upgraded = upgraded; this.tier = tier;
            this.conditionKey=conditionKey; this.successKey=successKey;
        }
    }
    private static Item registerToken() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:academy_token"));
        var data = new CompoundTag(); data.putInt("academy_token", 1);
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(
                new Item.Properties().setId(key).overrideDescription("academy_token")
                        .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:advancement_icon/academy"))
                        .component(DataComponents.CUSTOM_DATA, CustomData.of(data)), Items.PAPER, true));
    }
    static void register() { }
    private static CompoundTag flag(String name) { var tag = new CompoundTag(); tag.putInt(name, 1); return tag; }
    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    private static Wand wand(ItemStack stack) {
        var tag = data(stack);
        for (var wand : Wand.values())
            if (stack.is(wand.base) || stack.is(wand.upgraded) || tag.getIntOr(wand.flag, 0) == 1) return wand;
        return null;
    }
    static boolean trial(ItemStack stack) {
        var tag = data(stack);
        return tag.getIntOr("upgradable", 0) == 1 && tag.getIntOr("upgraded", 0) != 1;
    }
    private final GameController game;
    private final Set<ItemEntity> drops = new LinkedHashSet<>();
    private final Set<Entity> entities = new HashSet<>();
    private final Map<UUID, Long> lastInteractions = new HashMap<>();
    private final ArrayDeque<Placement> placements = new ArrayDeque<>();
    private final Map<UUID,Placement> singleUse = new HashMap<>();
    private final Map<GlobalPos,Placement> panelOwners = new HashMap<>();
    private final Set<GlobalPos> pendingWhite = new HashSet<>();
    private boolean wasStopped;
    // Источники одной серии ссылаются на один счётчик: 85 TNT не дают 85 очков за одну цель.
    private final Map<Entity, BlastTrial> blastSources = new IdentityHashMap<>();
    private Placement placing;
    private static final class Placement {
        final StructureTemplate template;
        final ServerLevel level;
        final BlockPos origin;
        final Vec3 source;
        final String epoch;
        final boolean token;
        final Set<Entity> spawned = new HashSet<>();
        final Set<GlobalPos> panels = new HashSet<>();
        final List<BlockPos> background;
        boolean spent;
        final long seed;
        int row;
        Placement(StructureTemplate template, ServerLevel level, BlockPos origin, Vec3 source,
                  String epoch, boolean token, long seed) {
            this.template = template; this.level = level; this.origin = origin; this.source = source;
            this.epoch = epoch; this.token = token; this.seed = seed;
            this.background = template.filterBlocks(origin,new StructurePlaceSettings(),
                    Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE)).stream().map(block -> block.pos().immutable()).toList();
        }
    }
    static final class BlastTrial {
        final UUID owner;
        final Wand wand;
        final InteractionHand hand;
        final Vec3 origin;
        final Set<UUID> targets = new HashSet<>();
        boolean completed;
        BlastTrial(ServerPlayer owner) { this(owner,Wand.EXASPERATO,InteractionHand.MAIN_HAND,null); }
        BlastTrial(ServerPlayer owner,Wand wand,InteractionHand hand,Vec3 origin) {
            this.owner=owner.getUUID(); this.wand=wand; this.hand=hand; this.origin=origin;
        }
    }
    LbcAcademy(GameController game) { this.game = game; }
    /** Разбор старой NBT/DFU выполняем при загрузке ресурсов, до первого токена в бою. */
    public void prepare() { game.server().getStructureTemplateManager().get(TEMPLATE); }

    /** Съедаем также offhand callback: нажатие по NPC не должно одновременно кастовать жезл. */
    public boolean interact(ServerPlayer player, InteractionHand hand, Entity target) {
        if (!target.entityTags().contains("magic_academy")) return false;
        var academy = singleUse.get(target.getUUID());
        if (target.isRemoved() || !target.isAlive() || academy != null && academy.spent) return true;
        if (target.entityTags().contains(BUILDING_TAG)) return true;
        if (hand != InteractionHand.MAIN_HAND || !active(player) || target.level() != player.level()
                || game.items().frost().isFrozen(player) || player.entityTags().contains("time_stopped")
                || game.items().timeStop().stopped(player.level())) return true;
        String dimension = player.level().dimension().identifier().toString();
        if (dimension.equals("minecraft:nexus") || dimension.equals("minecraft:imprinted")) return true;
        // interaction_at + interaction одного клиентского клика могут прийти в одном тике.
        long now = player.level().getGameTime();
        if (lastInteractions.getOrDefault(player.getUUID(), Long.MIN_VALUE) == now) return true;
        lastInteractions.put(player.getUUID(), now);
        var stack = player.getMainHandItem(); var wand = wand(stack);
        if (wand == null) return true; // Остальным жезлам пока не обещаем несуществующие улучшения.
        var tag = data(stack);
        if (tag.getIntOr("pissed_by_madoka_sob_sob_sob_sob_sob", 0) == 1) return true;
        player.sendSystemMessage(Component.translatable("magic_academy_root").withStyle(ChatFormatting.GREEN));
        if (tag.getIntOr("upgraded", 0) == 1 || stack.is(wand.upgraded)) {
            player.sendSystemMessage(Component.translatable("already_upgraded"));
        } else if (tag.getIntOr("upgrade_available", 0) == 1) {
            tag.putInt("upgraded", 1); tag.putInt("mode", 1);
            var upgraded = stack.transmuteCopy(wand.upgraded);
            upgraded.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            upgraded.set(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_" + wand.tier));
            if(wand==Wand.FARTUS){
                var presentations=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/academy_fartus_wand");
                if(presentations.isEmpty())throw new IllegalStateException("Missing qualified Fartus presentation");
                var presentation=presentations.getFirst();
                upgraded.set(DataComponents.LORE,presentation.getOrDefault(DataComponents.LORE,ItemLore.EMPTY));
                upgraded.set(DataComponents.ITEM_MODEL,presentation.get(DataComponents.ITEM_MODEL));
                upgraded.set(DataComponents.ATTRIBUTE_MODIFIERS,presentation.get(DataComponents.ATTRIBUTE_MODIFIERS));
            }else{
                upgraded.set(DataComponents.LORE, stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
                        .withLineAdded(Component.translatable("upgraded_item").withStyle(style ->
                                style.withColor(ChatFormatting.GREEN).withItalic(false))));
            }
            player.setItemInHand(hand, upgraded);
            if (academy != null) consume(academy);
            game.lbc().academyPromoted(player);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.translatable(wand.successKey).withStyle(ChatFormatting.YELLOW));
        } else if (tag.getIntOr("upgradable", 0) == 1) {
            player.sendSystemMessage(Component.translatable("no_conditions"));
        } else {
            tag.putInt("upgradable", 1); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.getInventory().setChanged();
            player.sendSystemMessage(Component.translatable(wand.conditionKey).withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    public void ready(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (!active(player) || player.getItemInHand(hand) != stack || !trial(stack)) return;
        var tag = data(stack); tag.putInt("upgrade_available", 1); tag.putInt("upgradable", 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); player.getInventory().setChanged();
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP),
                net.minecraft.sounds.SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2, 1,
                player.getRandom().nextLong()));
        player.sendSystemMessage(Component.translatable("can_upgrade").withStyle(ChatFormatting.AQUA));
    }
    public boolean terraTrial(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (!player.isShiftKeyDown() || !trial(stack)) return false;
        // В исходнике концентрация заменяет обычный выстрел, но сохраняет расход маны.
        if (player.level().getBlockState(BlockPos.containing(player.position().add(0, -1, 0))).is(Blocks.GRASS_BLOCK)) {
            LbcTerra.growVegetation(player.level(), player.blockPosition(), LbcTerra.Kind.ACADEMY);
            player.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY()+1,
                    player.getZ(), 10, .3, .6, .3, 0);
            if (player.getRandom().nextFloat() < .2f) ready(player, hand, stack);
        } else player.sendSystemMessage(Component.translatable("not_on_grass"));
        return true;
    }
    /** One cast must affect at least fifteen eligible mobs; use the casting hand. */
    public void lifestealerCast(ServerPlayer player, InteractionHand hand, ItemStack stack, int targets) {
        if (targets >= 15 && wand(stack) == Wand.LIFESTEALER && trial(stack)) ready(player, hand, stack);
    }
    public void electroTrial(ServerPlayer player, InteractionHand hand, ItemStack stack) { ready(player, hand, stack); }
    /** Пять запусков с заданием дают готовность Exoperso; счётчик принадлежит используемому жезлу. */
    public void exopersoTrial(ServerPlayer player,InteractionHand hand,ItemStack stack) {
        if(!trial(stack)) return;
        var tag=data(stack); int progress=tag.getIntOr("score",0)+1;
        tag.putInt("score",progress); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        player.getInventory().setChanged();
        if(progress>=5) ready(player,hand,stack);
    }
    /** Медитация Ruby заменяет выстрел; союзников притягивает, но служебные роли остаются исключены (user). */
    public boolean rubyTrial(ServerPlayer player,InteractionHand hand,ItemStack stack) {
        if(!player.isShiftKeyDown() || !trial(stack)) return false;
        var level=player.level(); var origin=player.getEyePosition();
        LbcOverload.sound(player,origin,"minecraft:block.amethyst_cluster.break",1,1,0);
        var center=BlockPos.containing(origin);
        var targets=level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(5.5),
                target->target.position().distanceToSqr(origin)<=5.5*5.5
                        && !target.entityTags().contains("strict_map_object") && game.items().wands().target(player,target,true));
        game.items().indirectKills().trapChanges(player,()-> {
            game.maps().placeLoadedTemplate(player,LbcRuby.MEDITATION,center.offset(-5,-5,-5),.96f);
            for(var pos:BlockPos.betweenClosed(center,center.above()))
                if(level.hasChunkAt(pos)) level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                                |net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        });
        for(var target:targets) {
            target.teleportTo(player.getX(),player.getY(),player.getZ());
            target.setYRot(player.getYRot()); target.setXRot(player.getXRot());
            game.items().indirectKills().displaced(target,player);
        }
        if(game.items().mana().hudValue(player,"void_resistance")<=0)
            player.hurtServer(level,new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(net.minecraft.world.damagesource.DamageTypes.OUTSIDE_BORDER)),6);
        ready(player,hand,stack);
        return true;
    }
    /** Медитация телепорта тратит обычную ману и добавляет пять импульсов shock самому владельцу. */
    public void teleportTrial(ServerPlayer player,InteractionHand hand,ItemStack stack) {
        if(!player.isShiftKeyDown() || !trial(stack)) return;
        game.items().electro().shock(player,player,5);
        if(player.getRandom().nextFloat()<.35f) ready(player,hand,stack);
    }
    /** (user) Испытание засчитывает реальное попадание взрыва дальше 80 блоков, а не возраст ракеты. */
    BlastTrial airTrial(ServerPlayer player,LbcAir.Charge charge) {
        if(!charge.trial || charge.trialOwner!=player || charge.trialHand==null || charge.origin==null) return null;
        var stack=player.getItemInHand(charge.trialHand);
        return active(player) && wand(stack)==Wand.AIR && trial(stack)
                ? new BlastTrial(player,Wand.AIR,charge.trialHand,charge.origin) : null;
    }
    /** Испытание слепоты: шанс 3% за попадание в ещё не ослеплённую цель, у используемой руки. */
    public void blindTrial(ServerPlayer player,InteractionHand hand,boolean previouslyBlind) {
        var stack=player.getItemInHand(hand);
        if(!previouslyBlind && wand(stack)==Wand.BLIND && trial(stack) && player.getRandom().nextFloat()<.03f)
            ready(player,hand,stack);
    }
    /** (user) Постоянный бедрок и шанс 20% сохраняются как часть каменной медитации. */
    public void stoneTrial(ServerPlayer player,InteractionHand hand,ItemStack stack) {
        if(!player.isShiftKeyDown() || !trial(stack)) return;
        var center=BlockPos.containing(player.getEyePosition());
        game.items().indirectKills().fillTrap(player,center.offset(-1,-1,-1),center.offset(1,2,1),Blocks.BEDROCK.defaultBlockState());
        if(player.getRandom().nextFloat()<.2f) ready(player,hand,stack);
    }
    /** Бедроковое испытание (user): готовность сразу и сплошной куб 21³; проверяется используемая рука. */
    public boolean bedrockTrial(ServerPlayer player,InteractionHand hand,ItemStack stack) {
        if(!player.isShiftKeyDown() || !trial(stack)) return false;
        ready(player,hand,stack);
        var center=player.blockPosition();
        game.items().indirectKills().fillTrap(player,center.offset(-10,-10,-10),center.offset(10,10,10),Blocks.BEDROCK.defaultBlockState());
        return true;
    }
    /** Испытание Wall проверяет четыре кирпичные стороны вокруг глаз, а не число кастов. */
    void wallTrial(ServerPlayer player,InteractionHand hand,ItemStack stack) {
        if(!trial(stack)) return;
        Vec3 from=player.getEyePosition().add(Vec3.directionFromRotation(0,player.getYRot()));
        BlockPos start=BlockPos.containing(from);
        var walls=new HashSet<BlockPos>();
        for(int yaw:new int[]{0,90,180,270}) {
            Vec3 direction=Vec3.directionFromRotation(0,yaw);
            boolean found=false;
            for(int step=0;step<=10;step++) {
                var pos=BlockPos.containing(from.add(direction.scale(step*.9)));
                if(!player.level().hasChunkAt(pos)) break;
                // (user) Общий старт внутри кирпича не заменяет четыре разные стороны.
                if(!pos.equals(start) && player.level().getBlockState(pos).is(Blocks.BRICKS) && walls.add(pos)) {
                    found=true; break;
                }
            }
            if(!found) return;
        }
        ready(player,hand,stack);
    }
    /** Испытание завершается после постановки слоёв, если живой противник действительно оказался внутри блока. */
    void stonekiviTrial(ServerPlayer owner,InteractionHand hand,ItemStack stack,BlockPos impact) {
        if(!trial(stack)) return;
        var airs=net.minecraft.tags.TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
        var center=Vec3.atCenterOf(impact);
        var targets=owner.level().getEntitiesOfClass(ServerPlayer.class,new net.minecraft.world.phys.AABB(center,center).inflate(10),
                player->game.items().wands().target(owner,player) && player.position().distanceToSqr(center)<=100
                        && !player.level().getBlockState(player.blockPosition()).is(airs));
        if(!targets.isEmpty()) ready(owner,hand,stack);
    }
    /** Медитация огня сохраняет исходные лаву 3×3 и шанс 5% на один Shift+ПКМ. */
    public void fireTrial(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if(!player.isShiftKeyDown() || !trial(stack)) return;
        var level=player.level();
        var center=BlockPos.containing(player.getEyePosition());
        game.items().indirectKills().worldChanges(level,player,()-> {
            for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))
                if(level.hasChunkAt(pos)) level.setBlockAndUpdate(pos,Blocks.LAVA.defaultBlockState());
        });
        if(player.getRandom().nextFloat()<.05f) ready(player,hand,stack);
    }
    BlastTrial blastTrial(ServerPlayer owner, boolean enabled) {
        return enabled && active(owner) && trial(owner.getMainHandItem()) && wand(owner.getMainHandItem()) == Wand.EXASPERATO
                ? new BlastTrial(owner) : null;
    }
    void blastSource(Entity source, BlastTrial trial) { if (source != null && trial != null) blastSources.put(source, trial); }
    /** Мгновенный взрыв не попадает в ENTITY_UNLOAD: его временный источник освобождается явно. */
    void blastFinished(Entity source) { blastSources.remove(source); }
    public void inheritBlast(Entity source, Entity spawned) { blastSource(spawned, blastSources.get(source)); }
    /** Роль и расстояние фиксируются до урона; положительный результат проверяется в AFTER_DAMAGE/AFTER_DEATH. */
    public void beforeDamage(LivingEntity victim,DamageSource damage) {
        var trial=blastSources.get(damage.getDirectEntity());
        if(trial==null || trial.completed || trial.wand!=Wand.AIR) return;
        var owner=game.server().getPlayerList().getPlayer(trial.owner);
        if(owner!=null && victim.position().distanceToSqr(trial.origin)>80*80
                && game.items().lifetimes().target(owner,victim)) trial.targets.add(victim.getUUID());
    }
    public void damaged(LivingEntity victim, DamageSource damage) {
        var trial = blastSources.get(damage.getDirectEntity());
        if (trial == null || trial.completed) return;
        var owner = game.server().getPlayerList().getPlayer(trial.owner);
        if(owner==null) return;
        if(trial.wand==Wand.AIR) {
            if(!trial.targets.remove(victim.getUUID())) return;
        } else if(!trial.targets.add(victim.getUUID()) || trial.targets.size()<10) return;
        var stack=owner.getItemInHand(trial.hand);
        if (wand(stack)==trial.wand && trial(stack)) {
            ready(owner, trial.hand, stack);
            trial.completed = true;
        }
    }

    public boolean loaded(Entity entity) {
        if (stale(entity)) { entity.discard(); return true; }
        var academy = singleUse.get(entity.getUUID());
        if (academy != null) {
            if (academy.spent) { entity.discard(); return true; }
            academy.spawned.removeIf(previous -> previous.getUUID().equals(entity.getUUID()));
            academy.spawned.add(entity);
        }
        if (placing != null) {
            entity.entityTags().add(ENTITY_TAG); entity.entityTags().add(BUILDING_TAG);
            if (placing.token) {
                singleUse.put(entity.getUUID(),placing);
                if (entity.entityTags().contains("magic_academy")) entity.entityTags().add(SINGLE_PANEL);
                if (entity instanceof net.minecraft.world.entity.Display.TextDisplay label) {
                    label.setText(Component.translatableWithFallback(SINGLE_USE_WELCOME,"Single-use Academy of Magic")
                            .withStyle(label.getText().getStyle()));
                    // A short title belongs at the panel's centre, just in front of its inner face.
                    // The ordinary welcome keeps its source position and multi-line layout.
                    label.setPos(placing.origin.getX()+6,placing.origin.getY()+3,placing.origin.getZ()+9.97);
                    entity.entityTags().add(SINGLE_PANEL);
                }
            }
            game.items().markCurrentItemEpoch(entity); entities.add(entity); placing.spawned.add(entity); return true;
        }
        if (entity.entityTags().contains(ENTITY_TAG)) {
            for (var previous : Set.copyOf(entities)) if (previous.getUUID().equals(entity.getUUID())) {
                if (!previous.entityTags().contains(BUILDING_TAG)) entity.entityTags().remove(BUILDING_TAG);
                entities.remove(previous);
            }
            for (var part : placements) {
                boolean owned = part.spawned.removeIf(previous -> previous.getUUID().equals(entity.getUUID()));
                if (owned) part.spawned.add(entity);
            }
            entities.add(entity); return true;
        }
        if (entity instanceof ItemEntity item && isDrop(item.getItem()) && item.level() == game.maps().level()) {
            game.items().markCurrentItemEpoch(item);
            drops.add(item); return true;
        }
        return false;
    }
    private static boolean isDrop(ItemStack stack) {
        var tag = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(TOKEN) || tag != null && (tag.matchedBy(TOKEN_DATA) || tag.matchedBy(BUILDING_DATA));
    }
    public void unloaded(Entity entity) {
        drops.remove(entity);
        // Keep the small ownership record through chunk unload: completion and stop must still close it.
        if (entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) entities.remove(entity);
    }
    private boolean active(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && player.level() == game.maps().level() && game.canUseGameplayItems(player);
    }
    public void tick() {
        boolean stopped = game.items().timeStop().stopped(game.maps().level());
        if (wasStopped && !stopped) for (var cell : Set.copyOf(pendingWhite)) repaint(cell);
        wasStopped = stopped;
        // Удалённый TNT взрывается до следующего END_SERVER_TICK: очищаем только после damage callbacks.
        blastSources.keySet().removeIf(Entity::isRemoved);
        for (var it = drops.iterator(); it.hasNext();) {
            var item = it.next();
            if (item.isRemoved() || !isDrop(item.getItem())) { it.remove(); continue; }
            if (stale(item)) { item.discard(); it.remove(); continue; }
            if (!item.onGround() || item.level() != game.maps().level() || game.maps().busy()
                    || game.stage() == GameController.Stage.ENDING
                    || game.items().timeStop().stopped(game.maps().level())) continue;
            if (!game.playing() && !(item.getOwner() instanceof ServerPlayer p && active(p))) continue;
            var template = game.server().getStructureTemplateManager().get(TEMPLATE).orElse(null);
            if (template == null || template.getSize().getY() == 0) continue;
            boolean token = item.getItem().is(TOKEN) || data(item.getItem()).getIntOr("academy_token", 0) == 1;
            var origin = item.blockPosition().offset(-6, token ? -1 : 0, -6);
            var part = new Placement(template, game.maps().level(), origin, item.position(),
                    game.items().epoch(), token, item.getRandom().nextLong());
            if (!resident(part)) continue; // Reject before debit; never request a chunk for acquisition.
            placements.addLast(part);
            it.remove();
            if (token) {
                game.maps().level().sendParticles(ParticleTypes.WITCH, item.getX(), item.getY(), item.getZ(), 350, 5, 5, 5, 1);
                item.level().playSound(null, item.getX(), item.getY(), item.getZ(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                        net.minecraft.sounds.SoundSource.MASTER, 1, 1);
                item.discard();
            } else {
                var remaining = item.getItem().copy(); remaining.shrink(1);
                if (remaining.isEmpty()) item.discard();
                else { item.setItem(remaining); drops.add(item); }
            }
            break; // Не разворачивать несколько зданий за один тик.
        }
        if (placements.isEmpty() || game.maps().busy()) return;
        var part = placements.removeFirst(); var size = part.template.getSize(); var origin = part.origin;
        if (!game.items().currentEpoch(part.epoch) || part.level != game.maps().level()
                || game.stage() == GameController.Stage.ENDING) {
            part.spawned.forEach(Entity::discard); return;
        }
        if (game.items().timeStop().stopped(part.level) || !resident(part)) { placements.addLast(part); return; }
        var bounds = new BoundingBox(origin.getX(), origin.getY()+part.row, origin.getZ(),
                origin.getX()+size.getX()-1, origin.getY()+part.row, origin.getZ()+size.getZ()-1);
        placing = part;
        try {
            game.maps().placeMapSlice(part.level, part.template, origin,
                    new StructurePlaceSettings().setBoundingBox(bounds), part.seed);
            // Включает developer-проверки в лобби, которые обычный arena block callback не журналирует.
            // Журнал группирует точки по чанкам: двух диагональных углов недостаточно для четырёх чанков.
            for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++)
                for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                    game.maps().recordBlockChange(new BlockPos(Math.max(bounds.minX(), x*16), bounds.minY(),
                            Math.max(bounds.minZ(), z*16)));
                    game.maps().recordBlockChange(new BlockPos(Math.min(bounds.maxX(), x*16+15), bounds.maxY(),
                            Math.min(bounds.maxZ(), z*16+15)));
                }
        } finally { placing = null; }
        // Claim a background row when it is written, including during overlapping construction.
        for (var pos : part.background) if (pos.getY() == origin.getY()+part.row
                && part.level.getBlockState(pos).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE))) {
            var cell = GlobalPos.of(part.level.dimension(),pos);
            if (part.token) { part.panels.add(cell); panelOwners.put(cell,part); }
            else panelOwners.remove(cell);
            pendingWhite.remove(cell);
        }
        if (++part.row < size.getY()) placements.addLast(part);
        else {
            part.spawned.forEach(entity -> entity.entityTags().remove(BUILDING_TAG));
            // Native placement can fail to create an actor. Never send a player into that incomplete acquisition.
            if (!part.token && part.spawned.stream().anyMatch(entity -> !entity.isRemoved()
                    && entity.entityTags().contains("magic_academy"))) {
                var nearest = part.level.getNearestPlayer(part.source.x, part.source.y, part.source.z, 12,
                        p -> p instanceof ServerPlayer player && active(player) && !game.isObserver(player)
                                && game.items().wands().validTarget(player));
                if (nearest instanceof ServerPlayer player)
                    player.teleportTo(part.level, part.source.x, part.source.y+1, part.source.z-2,
                            Set.of(), player.getYRot(), player.getXRot(), false);
            }
        }
    }
    /** Commit before removing entities: another player's click must see the same spent state. */
    private void consume(Placement academy) {
        academy.spent = true;
        for (var entity : Set.copyOf(academy.spawned)) if (entity.entityTags().contains(SINGLE_PANEL)
                && !entity.isRemoved()) entity.discard();
        for (var cell : academy.panels) if (panelOwners.get(cell) == academy) repaint(cell);
    }
    private void repaint(GlobalPos cell) {
        var academy = panelOwners.get(cell);
        if (academy == null || !academy.spent || !game.items().currentEpoch(academy.epoch)) { pendingWhite.remove(cell); return; }
        var level = academy.level; var pos = cell.pos();
        if (game.items().timeStop().stopped(level) || level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null) {
            pendingWhite.add(cell); return;
        }
        if (level.getBlockState(pos).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE))) level.setBlock(pos,Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
        pendingWhite.remove(cell); panelOwners.remove(cell);
    }
    public void chunkLoaded(net.minecraft.world.level.chunk.LevelChunk chunk) {
        if (pendingWhite.isEmpty()) return;
        for (var cell : Set.copyOf(pendingWhite)) if (cell.dimension().equals(chunk.getLevel().dimension())
                && chunk.getPos().equals(new net.minecraft.world.level.ChunkPos(cell.pos().getX()>>4,cell.pos().getZ()>>4))) repaint(cell);
    }
    private boolean stale(Entity entity) {
        for (var tag : entity.entityTags())
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return true;
        return false;
    }
    private boolean resident(Placement part) {
        var size = part.template.getSize(); var origin = part.origin;
        if (size.getX()<=0 || size.getY()<=0 || size.getZ()<=0 || origin.getY()<part.level.getMinY()
                || (long)origin.getY()+size.getY()-1>part.level.getMaxY()) return false;
        // Native block updates can inspect neighbours just beyond the template boundary.
        for (int x=(origin.getX()-1)>>4; x<=(origin.getX()+size.getX())>>4; x++)
            for (int z=(origin.getZ()-1)>>4; z<=(origin.getZ()+size.getZ())>>4; z++)
                if (part.level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    int pendingCount() { return placements.size(); }
    int ownedCount() { return entities.size(); }
    int pendingRow() { return placements.isEmpty() ? -1 : placements.peekFirst().row; }
    public void stop() {
        drops.clear(); placements.clear(); lastInteractions.clear(); blastSources.clear();
        singleUse.clear(); panelOwners.clear(); pendingWhite.clear(); wasStopped = false;
        var old = Set.copyOf(entities); entities.clear(); old.forEach(Entity::discard);
    }
}
