package me.skywars2.game;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Артефакты и настоящие Polymer-маяки: события, пространственный индекс и ограниченная очистка. */
public final class LbcMagic {
    private static final CompoundTag ARTIFACT_DATA = artifactData(1);
    private static final CompoundTag EXPLOSIONS_DATA = artifactData(2);
    private static final CompoundTag LIQUIDS_DATA = artifactData(3);
    private static final CompoundTag ARROWS_DATA = artifactData(4);
    private static final CompoundTag DEBUFF_DATA = artifactData(5), CLEAR_DATA = artifactData(6), COLORS_DATA = artifactData(7), METABOLISM_DATA = artifactData(8);
    private static final CompoundTag REACTIVE = flagData("reactive"), TECHNO_MK2 = flagData("technomk2"),
            TECHNO = flagData("techno"), REACTIVE_ROD = flagData("reactive_shish");
    private static final int ANTIMAGIC = 1 << 1, NO_EXPLOSIONS = 1 << 2, NO_LIQUIDS = 1 << 3, NO_ARROWS = 1 << 4,
            CLEAR_DEBUFFS = 1 << 5, CLEAR_EFFECTS = 1 << 6, COLORS = 1 << 7, METABOLISM = 1 << 8;
    private static final int SUPPORTED_ARTIFACTS = ANTIMAGIC | NO_EXPLOSIONS | NO_LIQUIDS | NO_ARROWS | CLEAR_DEBUFFS | CLEAR_EFFECTS | COLORS | METABOLISM;
    private static final TagKey<EntityType<?>> ARROWS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:arrows"));
    private static final TagKey<EntityType<?>> UNSTABLE = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:unstable"));
    private static final Predicate<Entity> IS_ARROW = entity -> entity.getType().builtInRegistryHolder().is(ARROWS);
    private static final Predicate<Entity> IS_UNSTABLE = entity -> entity.getType().builtInRegistryHolder().is(UNSTABLE);
    private static final Predicate<Entity> IS_FALLING = entity -> entity instanceof FallingBlockEntity;
    private static final Predicate<BlockState> IS_LIQUID = state -> liquidReplacement(state) != null;
    private static final int LIQUID_RADIUS = 16;
    private static final int[][] LIQUID_WIDTHS = liquidWidths();
    private static int[][] liquidWidths() {
        var result = new int[2*LIQUID_RADIUS+1][2*LIQUID_RADIUS+1];
        for (int y=-LIQUID_RADIUS;y<=LIQUID_RADIUS;y++) for (int z=-LIQUID_RADIUS;z<=LIQUID_RADIUS;z++) {
            int squared=LIQUID_RADIUS*LIQUID_RADIUS-y*y-z*z;
            result[y+LIQUID_RADIUS][z+LIQUID_RADIUS]=squared<0 ? -1 : (int)Math.sqrt(squared);
        }
        return result;
    }
    private static final List<Block> LIQUID_SOURCE = List.of(Blocks.LAVA, Blocks.WATER, Blocks.POWDER_SNOW);
    private static final Map<Block, Block> FALLING_REPLACEMENTS = fallingReplacements();
    private static Map<Block, Block> fallingReplacements() {
        var result = new HashMap<Block, Block>(Map.ofEntries(
            Map.entry(Blocks.SAND,Blocks.SANDSTONE), Map.entry(Blocks.GRAVEL,Blocks.COBBLESTONE),
            Map.entry(Blocks.RED_SAND,Blocks.RED_SANDSTONE), Map.entry(Blocks.STONE_BRICKS,Blocks.STONE_BRICKS),
            Map.entry(Blocks.NETHERITE_BLOCK,Blocks.NETHERITE_BLOCK), Map.entry(Blocks.ANVIL,Blocks.IRON_BLOCK),
            Map.entry(Blocks.CHIPPED_ANVIL,Blocks.IRON_BLOCK), Map.entry(Blocks.DAMAGED_ANVIL,Blocks.IRON_BLOCK)));
        // В 26.3 цветные блоки представлены нативной ColorCollection; выбираем ровно исходные 16 цветов.
        for (var color : net.minecraft.world.item.DyeColor.values())
            result.put(Blocks.CONCRETE_POWDER.pick(color),Blocks.CONCRETE.pick(color));
        return Map.copyOf(result);
    }
    private static final EntityTypeTest<Entity, Entity> ENTITIES = EntityTypeTest.forClass(Entity.class);
    private static final EntityTypeTest<Entity, ServerPlayer> PLAYERS = EntityTypeTest.forClass(ServerPlayer.class);
    public static final Item MYSTICAL_ARTIFACT = registerArtifact();
    public static final Block MAGIC_BEACON_BLOCK = registerBlock(false);
    public static final Block MECHANISM_BEACON_BLOCK = registerBlock(true);
    public static final BlockEntityType<BeaconData> BEACON_DATA = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.parse("skywars2:magic_beacon"), new BlockEntityType<>(BeaconData::new, Set.of(MAGIC_BEACON_BLOCK, MECHANISM_BEACON_BLOCK)));
    public static final Item MAGIC_BEACON = registerBeacon(false);
    public static final Item MECHANISM_BEACON = registerBeacon(true);
    private static final TagKey<Block> MECHANISM_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:mecha"));
    private static final TagKey<EntityType<?>> MECHANISM_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mecha"));
    private static final TagKey<Block> MAGIC_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:magic"));
    private static final TagKey<EntityType<?>> MAGIC_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:magic_entities"));
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final int STRICT_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;
    private static final int RADIUS = 20, RADIUS_SQUARED = RADIUS * RADIUS, BLOCK_BUDGET = 2048;
    // Геометрия вычисляется один раз: серверный тик читает только позиции внутри сферы.
    private static final List<BlockPos> SPHERE = sphere();
    private static final List<BlockPos> MECHANISM_CUBE = mechanismCube();
    private static List<BlockPos> mechanismCube() {
        var offsets = new ArrayList<BlockPos>();
        for (int x=-15;x<=15;x++) for (int y=-15;y<=15;y++) for (int z=-15;z<=15;z++) offsets.add(new BlockPos(x,y,z));
        return List.copyOf(offsets);
    }

    private static final class Beacon {
        final BlockPos pos;
        final Vec3 center;
        final boolean mechanism;
        BeaconData data;
        boolean queued;
        int cursor;
        Beacon(BeaconData data) {
            this.data = data; pos = data.getBlockPos().immutable(); center = Vec3.atCenterOf(pos);
            mechanism = data.getBlockState().is(MECHANISM_BEACON_BLOCK);
        }
    }
    /** Только включённые области; подавленный маяком носитель остаётся в наборе артефактов для возобновления. */
    private static final class LiquidAura {
        BlockPos center;
        LiquidAura(BlockPos center) { this.center = center; }
    }
    private final GameController game;
    private final LbcColors colors;
    private final Set<UUID> dirtyInventories = new LinkedHashSet<>();
    // Guard может прочитать dirty-инвентарь раньше тика; отметка сохраняет очистку старых статусов.
    private final Set<UUID> pendingCleansing = new LinkedHashSet<>();
    private final Set<UUID> activeArtifacts = new java.util.HashSet<>();
    private final Set<UUID> arrowArtifacts = new java.util.HashSet<>();
    private final Set<UUID> explosionArtifacts = new java.util.HashSet<>();
    private final Set<UUID> liquidArtifacts = new java.util.HashSet<>();
    private final Set<Entity> arrows = new LinkedHashSet<>();
    private final Set<Entity> unstable = new LinkedHashSet<>();
    private final Set<Entity> falling = new LinkedHashSet<>();
    // Перехватчики работают последовательно в серверном потоке и переиспользуют один буфер.
    private final ArrayList<Entity> interceptionBatch = new ArrayList<>();
    private final Map<UUID, LiquidAura> liquidAuras = new LinkedHashMap<>();
    private final Map<Long, Set<LiquidAura>> liquidRegions = new HashMap<>();
    private final Set<BlockPos> dirtyLiquids = new LinkedHashSet<>();
    private final Set<Long> dirtyLiquidChunks = new LinkedHashSet<>();
    private final Map<BlockPos, BlockState> liquidUpdates = new LinkedHashMap<>();
    private final Map<UUID, Integer> artifactTypes = new LinkedHashMap<>();
    private final Map<BlockPos, Beacon> beacons = new LinkedHashMap<>();
    private final Set<Beacon> loadedBeacons = new LinkedHashSet<>();
    private final Map<Long, Set<Beacon>> regions = new HashMap<>();
    private final Set<BlockPos> dirtyBlocks = new LinkedHashSet<>();
    private final ArrayDeque<Beacon> scans = new ArrayDeque<>();
    private final Set<Marker> legacyMarkers = new LinkedHashSet<>();
    private final BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
    private boolean stopping;

    LbcMagic(GameController game) { this.game = game; colors = new LbcColors(game); }
    public LbcColors colors() { return colors; }
    static void register() { PolymerBlockUtils.registerBlockEntity(BEACON_DATA); }

    private static CompoundTag artifactData(int type) {
        var tag = new CompoundTag(); tag.putInt("mystical", 1); tag.putInt("type", type); return tag;
    }
    private static CompoundTag flagData(String name) {
        var tag = new CompoundTag(); tag.putInt(name, 1); return tag;
    }
    private static List<BlockPos> sphere() {
        var offsets = new ArrayList<BlockPos>();
        for (int x = -RADIUS; x <= RADIUS; x++) for (int y = -RADIUS; y <= RADIUS; y++)
            for (int z = -RADIUS; z <= RADIUS; z++) if (x*x+y*y+z*z <= RADIUS_SQUARED) offsets.add(new BlockPos(x,y,z));
        return List.copyOf(offsets);
    }
    private static Item registerArtifact() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:mystical_artifact"));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(new Item.Properties().setId(key)
                .overrideDescription("mystical_artifact").component(DataComponents.CUSTOM_DATA, CustomData.of(ARTIFACT_DATA))
                .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:mysticalartifact")), Items.REPEATING_COMMAND_BLOCK, true) {
            @Override public void modifyClientTooltip(List<net.minecraft.network.chat.Component> tooltip, ItemStack stack, PacketContext context) {
                LbcSkills.artifactTooltip(tooltip, stack, context);
            }
        });
    }
    private static Block registerBlock(boolean mechanism) {
        var key = ResourceKey.create(Registries.BLOCK, Identifier.parse(mechanism ? "skywars2:mechanism_beacon" : "skywars2:magic_beacon"));
        return Registry.register(BuiltInRegistries.BLOCK, key, new BeaconBlock(BlockBehaviour.Properties
                .ofFullCopy(Blocks.LIGHTNING_ROD.weathering().unaffected()).setId(key).overrideDescription(mechanism ? "mecha_breaker" : "magic_breaker"), mechanism));
    }
    private static Item registerBeacon(boolean mechanism) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse(mechanism ? "skywars2:mechanism_beacon" : "skywars2:magic_beacon"));
        // Старый компонент остаётся частью исходного стека/tooltip, но больше не создаёт сущность.
        var data = new CompoundTag(); var tags = new ListTag();
        for (String tag : List.of(mechanism ? "stopper" : "stopper_magic", "rc_1sec", "marker_2t")) tags.add(StringTag.valueOf(tag));
        data.put("Tags", tags);
        var properties = new Item.Properties().setId(key).overrideDescription(mechanism ? "mecha_breaker" : "magic_breaker")
                .component(DataComponents.ITEM_MODEL, Identifier.parse(mechanism ? "lbc:stopper" : "lbc:stopper_magic"))
                .component(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityTypes.MARKER, data));
        return Registry.register(BuiltInRegistries.ITEM, key, new PolymerBlockItem(mechanism ? MECHANISM_BEACON_BLOCK : MAGIC_BEACON_BLOCK, properties, Items.PANDA_SPAWN_EGG, true) {
            @Override public InteractionResult useOn(UseOnContext context) {
                var game = SkyWars2.game();
                if (!(context.getPlayer() instanceof ServerPlayer player) || game == null
                        || !game.canUseGameplayItems(player) || context.getLevel() != game.maps().level()) return InteractionResult.FAIL;
                return super.useOn(context);
            }
        });
    }

    /** Род — только клиентское представление; сервер хранит отдельный тип блока и его нативный loot. */
    private static final class BeaconBlock extends LightningRodBlock implements PolymerBlock, EntityBlock {
        final boolean mechanism;
        BeaconBlock(BlockBehaviour.Properties properties, boolean mechanism) { super(properties); this.mechanism = mechanism; }
        private String lootTable() { return mechanism ? "lbc:ported/mechanism_beacon" : "lbc:ported/magic_beacon"; }
        @Override public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level,pos,state,placer,stack);
            var entityData = stack.get(DataComponents.ENTITY_DATA);
            if (level.getBlockEntity(pos) instanceof BeaconData data) {
                data.placer = placer instanceof ServerPlayer player ? player.getUUID() : null;
                data.notBlock = entityData != null && entityData.copyTagWithoutId().getListOrEmpty("Tags").contains(StringTag.valueOf("not_block"));
                data.setChanged();
            }
        }
        @Override public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
            return Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState()
                    .setValue(FACING, state.getValue(FACING)).setValue(POWERED, state.getValue(POWERED))
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            // Прежний маяк всегда стоял вертикально, независимо от грани установки.
            return super.getStateForPlacement(context).setValue(FACING, net.minecraft.core.Direction.UP);
        }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BeaconData(pos, state); }
        @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            var game = SkyWars2.game();
            return game == null ? List.of() : game.lbc().rollTable(params.getLevel(), params.getParameter(LootContextParams.ORIGIN),
                    params.getOptionalParameter(LootContextParams.THIS_ENTITY), lootTable());
        }
        @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            var game = SkyWars2.game();
            if (game != null && level == game.maps().level()) {
                var drops = game.lbc().rollTable(game.maps().level(), Vec3.atCenterOf(pos), null, lootTable());
                if (!drops.isEmpty()) return drops.getFirst();
            }
            return new ItemStack(mechanism ? MECHANISM_BEACON : MAGIC_BEACON);
        }
    }

    /** Только сохранение эпохи матча. Тикера и сетевых BE-пакетов нет; используется общая эпоха LbcItems. */
    public static final class BeaconData extends BlockEntity {
        private String epoch;
        private boolean notBlock;
        private UUID placer;
        public BeaconData(BlockPos pos, BlockState state) {
            super(BEACON_DATA, pos, state);
            var game = SkyWars2.game(); epoch = game == null ? "" : game.items().epoch();
        }
        @Override protected void loadAdditional(ValueInput input) { super.loadAdditional(input); epoch = input.getStringOr("epoch", ""); notBlock = input.getBooleanOr("not_block",false);
            String owner=input.getStringOr("placer", "");
            try { placer=owner.isEmpty() ? null : UUID.fromString(owner); } catch (IllegalArgumentException invalid) { placer=null; }
        }
        @Override protected void saveAdditional(ValueOutput output) { super.saveAdditional(output); output.putString("epoch", epoch); output.putBoolean("not_block",notBlock);
            if(placer!=null) output.putString("placer",placer.toString());
        }
    }

    public void inventoryChanged(ServerPlayer player) {
        dirtyInventories.add(player.getUUID()); pendingCleansing.add(player.getUUID());
        game.items().mana().inventoryChanged(player);
        game.items().shulkerArmor().inventoryChanged(player);
        game.items().armorSets().inventoryChanged(player);game.items().shishEncounter().inventoryChanged(player);
    }
    public void clear(ServerPlayer player) {
        game.items().electro().inventory(player,false);
        pendingCleansing.remove(player.getUUID());
        dirtyInventories.remove(player.getUUID()); activeArtifacts.remove(player.getUUID()); arrowArtifacts.remove(player.getUUID());
        explosionArtifacts.remove(player.getUUID());
        liquidArtifacts.remove(player.getUUID()); removeLiquidAura(player.getUUID());
        artifactTypes.remove(player.getUUID()); player.entityTags().remove("nomagic_active");
    }
    public boolean artifactActive(ServerPlayer player) { return activeArtifacts.contains(player.getUUID()); }
    /** Биты поддержанных типов mystical:1; дубликаты одного типа не умножают эффект. */
    public int artifactTypes(ServerPlayer player) { return artifactTypes.getOrDefault(player.getUUID(), 0); }

    /** Только snapshot/gates: canBeAffected не должен удалять эффекты во время чужого native iterator. */
    private int cleansingTypes(ServerPlayer player) {
        if (dirtyInventories.remove(player.getUUID())) refresh(player);
        int types = artifactTypes(player) & (CLEAR_DEBUFFS | CLEAR_EFFECTS);
        if (types == 0 || stopping || game.stage() == GameController.Stage.ENDING
                || !game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                || player.level() != game.maps().level()) return 0;
        // Сохраняем beacon-only подавление, без исключений для Creative/developer и time_stopped.
        return beaconSuppressed(player.position()) ? 0 : types;
    }

    public boolean rejectEffect(ServerPlayer player, Holder<MobEffect> effect) {
        int types = cleansingTypes(player);
        return (types & CLEAR_EFFECTS) != 0 || (types & CLEAR_DEBUFFS) != 0 && cleansedDebuff(player, effect);
    }

    /** Общая точка допуска будущих кастомных статусов; type 5 сохраняет исходный native-перечень. */
    public boolean rejectCustomEffect(ServerPlayer player) { return LbcGreed.active(player) || (cleansingTypes(player) & CLEAR_EFFECTS) != 0; }

    /** До native tickEffects, чтобы уже существующий poison не получил даже один лечебный/уронный шаг. */
    public void beforeEffects(ServerPlayer player) {
        if(LbcGreed.active(player)) {
            for(var effect:List.copyOf(player.getActiveEffects()))
                if(effect.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL
                        || effect.is(MobEffects.LEVITATION) || effect.is(MobEffects.SLOW_FALLING)) player.removeEffect(effect.getEffect());
        }
        if (player.getActiveEffects().isEmpty() && !pendingCleansing.contains(player.getUUID())) return;
        pendingCleansing.remove(player.getUUID());
        cleanse(player);
    }

    private void cleanse(ServerPlayer player) {
        int types = cleansingTypes(player);
        if ((types & CLEAR_EFFECTS) != 0) game.items().clearEffects(player);
        else if ((types & CLEAR_DEBUFFS) != 0) clearDebuffs(player);
    }

    /** Один пересчёт dirty-снимка; очистка после клика выполняется до маны/кастомных эффектов этого тика. */
    void flushCleansing() {
        if (!dirtyInventories.isEmpty()) {
            var ids = List.copyOf(dirtyInventories); dirtyInventories.clear();
            for (UUID id : ids) { var player = game.server().getPlayerList().getPlayer(id); if (player != null) refresh(player); }
        }
        if (!pendingCleansing.isEmpty()) {
            var ids = List.copyOf(pendingCleansing); pendingCleansing.clear();
            for (UUID id : ids) { var player = game.server().getPlayerList().getPlayer(id); if (player != null) cleanse(player); }
        }
    }
    private void refresh(ServerPlayer player) {
        int found = 0;
        boolean blocker = false;
        if (game.canUseGameplayItems(player) && player.isAlive() && !player.isRemoved() && player.level() == game.maps().level()) {
            for (int slot = 0; slot < 36; slot++) {
                var stack=player.getInventory().getItem(slot);
                found |= artifact(stack); blocker |= LbcElectro.isBlocker(stack);
            }
            found |= artifact(player.getInventory().getItem(40));
            for(int slot=36;slot<=40;slot++) blocker |= LbcElectro.isBlocker(player.getInventory().getItem(slot));
            found |= artifact(player.containerMenu.getCarried());
            for (var slot : player.containerMenu.slots)
                if (slot.container instanceof CraftingContainer) found |= artifact(slot.getItem());
        }
        game.items().electro().inventory(player,blocker);
        if ((found & ANTIMAGIC) != 0) activeArtifacts.add(player.getUUID()); else activeArtifacts.remove(player.getUUID());
        if ((found & NO_ARROWS) != 0) arrowArtifacts.add(player.getUUID()); else arrowArtifacts.remove(player.getUUID());
        if ((found & NO_EXPLOSIONS) != 0) explosionArtifacts.add(player.getUUID()); else explosionArtifacts.remove(player.getUUID());
        if ((found & NO_LIQUIDS) != 0) liquidArtifacts.add(player.getUUID());
        else { liquidArtifacts.remove(player.getUUID()); removeLiquidAura(player.getUUID()); }
        if (found != 0) artifactTypes.put(player.getUUID(), found); else artifactTypes.remove(player.getUUID());
        player.entityTags().remove("nomagic_active");
    }
    private static int artifact(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        if (data.matchedBy(ARTIFACT_DATA)) return ANTIMAGIC;
        if (data.matchedBy(EXPLOSIONS_DATA)) return NO_EXPLOSIONS;
        if (data.matchedBy(LIQUIDS_DATA)) return NO_LIQUIDS;
        if (data.matchedBy(ARROWS_DATA)) return NO_ARROWS;
        if (data.matchedBy(DEBUFF_DATA)) return CLEAR_DEBUFFS;
        if (data.matchedBy(CLEAR_DATA)) return CLEAR_EFFECTS;
        if (data.matchedBy(COLORS_DATA)) return COLORS;
        return data.matchedBy(METABOLISM_DATA) ? METABOLISM : 0;
    }

    /** Источники-игроки используют UUID-набор, блоки — индекс чанков; старые marker пока поддерживаются для совместимости. */
    boolean luckyEffectWarded(ServerLevel level,Vec3 at) {
        for(var id:explosionArtifacts) {
            var player=game.server().getPlayerList().getPlayer(id);
            if(player!=null && player.level()==level && interceptorHolder(player,NO_EXPLOSIONS) && player.distanceToSqr(at)<=144) return true;
        }
        return false;
    }
    public boolean suppressed(ServerPlayer owner) {
        return suppressedAt(owner.level(),owner.position());
    }
    /** Тот же фильтр для отложенных лучей без marker: артефакт и маяк проверяются в точке луча. */
    boolean suppressedAt(ServerLevel level,Vec3 position) {
        return artifactSuppressedAt(level,position) || level==game.maps().level() && beaconSuppressed(position);
    }
    /** Free source accessories can use artifact-only admission without acquiring a new beacon gate. */
    boolean artifactSuppressedAt(ServerLevel level,Vec3 position) {
        var area = new AABB(position, position).inflate(RADIUS + 1e-6);
        return !activeArtifacts.isEmpty() && !level.getEntitiesOfClass(ServerPlayer.class, area, player -> game.canUseGameplayItems(player)
                && artifactActive(player) && position.distanceToSqr(player.position()) <= RADIUS_SQUARED).isEmpty();
    }
    /** Home markers use the source artifact's 25-block deletion radius; beacon suppression stays 20. */
    boolean homeMarkerSuppressedAt(ServerLevel level, Vec3 position) {
        if (level == game.maps().level() && beaconSuppressed(position)) return true;
        for (var id : activeArtifacts) {
            var holder = game.server().getPlayerList().getPlayer(id);
            if (holder != null && holder.level() == level && holder.isAlive() && !holder.isRemoved()
                    && game.canUseGameplayItems(holder) && holder.distanceToSqr(position) <= 625) return true;
        }
        return false;
    }
    /** Ближайшая граница подавления на луче: один проход по известным источникам вместо опроса каждого шага. */
    double suppressionFraction(ServerLevel level,Vec3 from,Vec3 to) {
        double nearest=Double.POSITIVE_INFINITY;
        for(var id:activeArtifacts) {
            var player=game.server().getPlayerList().getPlayer(id);
            if(player!=null && player.level()==level && game.canUseGameplayItems(player) && artifactActive(player))
                nearest=Math.min(nearest,sphereEntry(from,to,player.position()));
        }
        if(level==game.maps().level()) {
            for(var beacon:loadedBeacons) if(!beacon.mechanism) nearest=Math.min(nearest,sphereEntry(from,to,beacon.center));
            for(var marker:legacyMarkers) if(!marker.isRemoved() && marker.entityTags().contains("stopper_magic"))
                nearest=Math.min(nearest,sphereEntry(from,to,marker.position()));
        }
        return nearest;
    }
    private static double sphereEntry(Vec3 from,Vec3 to,Vec3 center) {
        Vec3 offset=from.subtract(center),delta=to.subtract(from);
        double c=offset.lengthSqr()-RADIUS_SQUARED;
        if(c<=0) return 0;
        double a=delta.lengthSqr();
        if(a<1e-12) return Double.POSITIVE_INFINITY;
        double b=offset.dot(delta),discriminant=b*b-a*c;
        if(discriminant<0) return Double.POSITIVE_INFINITY;
        double t=(-b-Math.sqrt(discriminant))/a;
        return t>=0 && t<=1 ? t : Double.POSITIVE_INFINITY;
    }
    public boolean mechanismSuppressed(ServerPlayer owner) { return mechanismSuppressedAt(owner.level(),owner.position()); }
    public boolean mechanismSuppressedAt(ServerLevel level, Vec3 position) {
        return level == game.maps().level() && beaconSuppressed(position,true);
    }
    public double mechanismSuppressionFraction(ServerLevel level, Vec3 from, Vec3 to) {
        double nearest = Double.POSITIVE_INFINITY;
        if (level != game.maps().level()) return nearest;
        for (var beacon : loadedBeacons) if (beacon.mechanism) nearest=Math.min(nearest,sphereEntry(from,to,beacon.center));
        for (var marker : legacyMarkers) if (!marker.isRemoved() && marker.entityTags().contains("stopper"))
            nearest=Math.min(nearest,sphereEntry(from,to,marker.position()));
        return nearest;
    }
    public boolean beaconSuppressed(Vec3 position) { return beaconSuppressed(position,false); }
    /** Paper getNearbyEntities radius: axis-aligned box, not the ordinary source sphere. */
    public boolean beaconSuppressedAabb(ServerLevel level, Vec3 position, double radius) {
        if(level!=game.maps().level()) return false;
        for(var beacon:loadedBeacons) if(!beacon.mechanism && withinBox(beacon.center,position,radius)) return true;
        var box=new AABB(position.x-radius,position.y-radius,position.z-radius,
                position.x+radius,position.y+radius,position.z+radius);
        for(var marker:legacyMarkers) if(!marker.isRemoved() && marker.level()==level
                && marker.entityTags().contains("stopper_magic") && marker.getBoundingBox().intersects(box)) return true;
        return false;
    }
    private static boolean withinBox(Vec3 a,Vec3 b,double radius) {
        return Math.abs(a.x-b.x)<radius && Math.abs(a.y-b.y)<radius && Math.abs(a.z-b.z)<radius;
    }
    private boolean beaconSuppressed(Vec3 position, boolean mechanism) {
        var nearby = regions.get(ChunkPos.pack(((int)Math.floor(position.x))>>4, ((int)Math.floor(position.z))>>4));
        if (nearby != null) for (var state : nearby)
            if (state.mechanism == mechanism && state.center.distanceToSqr(position) <= RADIUS_SQUARED) return true;
        for (var marker : legacyMarkers) if (!marker.isRemoved()
                && marker.entityTags().contains(mechanism ? "stopper" : "stopper_magic")
                && marker.position().distanceToSqr(position) <= RADIUS_SQUARED) return true;
        return false;
    }
    /** Комната теряет метку целиком, если сфера маяка касается её объёма (user). */
    boolean beaconIntersects(AABB bounds) {
        for(var beacon:loadedBeacons) if(!beacon.mechanism && bounds.distanceToSqr(beacon.center)<=RADIUS_SQUARED) return true;
        for(var marker:legacyMarkers) if(!marker.isRemoved() && marker.entityTags().contains("stopper_magic") && bounds.distanceToSqr(marker.position())<=RADIUS_SQUARED) return true;
        return false;
    }
    public boolean loaded(Entity entity) {
        if (entity instanceof ServerPlayer player) { inventoryChanged(player); game.items().mana().effectsChanged(player); }
        if (entity.isAlive() && entity.level() == game.maps().level()) {
            if(entity instanceof net.minecraft.world.entity.LivingEntity living) game.items().mana().loaded(living);
            colors.loaded(entity);
            if (entity.getType().builtInRegistryHolder().is(ARROWS)) arrows.add(entity);
            if (entity.getType().builtInRegistryHolder().is(UNSTABLE)) unstable.add(entity);
            if (entity instanceof FallingBlockEntity) falling.add(entity);
        }
        if (!(entity instanceof Marker marker) || !(marker.entityTags().contains("stopper_magic") || marker.entityTags().contains("stopper"))) return false;
        if (stopping || !game.playing() || entity.level() != game.maps().level()) marker.discard();
        else {
            legacyMarkers.add(marker);
            if (marker.entityTags().contains("stopper_magic")) game.items().bedrock().beaconPlaced(marker.position(),RADIUS);
        }
        return true;
    }
    public void unloaded(Entity entity) {
        if(entity instanceof net.minecraft.world.entity.LivingEntity living) game.items().mana().unloaded(living);
        colors.unloaded(entity);
        arrows.remove(entity);
        unstable.remove(entity);
        falling.remove(entity);
        legacyMarkers.remove(entity);
        if (entity instanceof ServerPlayer player) clear(player);
    }
    /** Shared magical/summoned classification used by suppression and armor pulses. */
    public boolean magical(Entity entity) {
        return !entity.entityTags().contains("spectator") && (!(entity instanceof ServerPlayer player) || game.allowDamage(player))
                && (entity.getType().builtInRegistryHolder().is(MAGIC_ENTITIES) || entity.entityTags().contains("magic")
                || entity.entityTags().contains("summactivated") && entity.getType().builtInRegistryHolder().is(MOBS)
                || LbcSoulless.managed(entity) && game.items().soulless().owner(entity)!=null);
    }
    private static boolean inside(Beacon state, BlockPos pos) {
        long x = (long)state.pos.getX()-pos.getX(), y = (long)state.pos.getY()-pos.getY(), z = (long)state.pos.getZ()-pos.getZ();
        return state.mechanism ? Math.abs(x)<=15 && Math.abs(y)<=15 && Math.abs(z)<=15 && !state.data.notBlock
                : x*x+y*y+z*z <= RADIUS_SQUARED;
    }
    private void index(Beacon state) {
        if (!loadedBeacons.add(state)) return;
        for (int x = (state.pos.getX()-RADIUS)>>4; x <= (state.pos.getX()+RADIUS)>>4; x++)
            for (int z = (state.pos.getZ()-RADIUS)>>4; z <= (state.pos.getZ()+RADIUS)>>4; z++)
                regions.computeIfAbsent(ChunkPos.pack(x,z), ignored -> new LinkedHashSet<>()).add(state);
    }
    private void unindex(Beacon state) {
        loadedBeacons.remove(state);
        for (int x = (state.pos.getX()-RADIUS)>>4; x <= (state.pos.getX()+RADIUS)>>4; x++)
            for (int z = (state.pos.getZ()-RADIUS)>>4; z <= (state.pos.getZ()+RADIUS)>>4; z++) {
                long key = ChunkPos.pack(x,z); var entries = regions.get(key);
                if (entries != null) { entries.remove(state); if (entries.isEmpty()) regions.remove(key); }
            }
    }
    private void loadedBlock(BeaconData data) {
        var level = game.maps().level(); var pos = data.getBlockPos();
        if (stopping || !game.items().currentEpoch(data.epoch)) {
            game.maps().recordBlockChange(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), STRICT_FLAGS); return;
        }
        var previous = beacons.get(pos);
        if (previous != null && previous.mechanism != data.getBlockState().is(MECHANISM_BEACON_BLOCK)) {
            beacons.remove(pos); unindex(previous); previous.data = null;
        }
        var state = beacons.computeIfAbsent(pos.immutable(), ignored -> new Beacon(data));
        if (state.data == data && loadedBeacons.contains(state)) return;
        state.data = data; index(state); schedule(state);
        // Событие размещения действует даже при удалении маяка до следующего тика.
        if (!state.mechanism) game.items().bedrock().beaconPlaced(state.center,RADIUS);
    }
    private boolean covers(BlockPos pos, boolean mechanism) {
        var nearby = regions.get(ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4));
        if (nearby != null) for (var state : nearby) if (state.mechanism == mechanism && state.data != null && inside(state,pos)) return true;
        return false;
    }
    public void blockChanged(BlockPos pos, BlockState state) {
        if (stopping) return;
        if (state.is(MAGIC_BEACON_BLOCK) || state.is(MECHANISM_BEACON_BLOCK)) {
            if (game.maps().level().getBlockEntity(pos) instanceof BeaconData data) loadedBlock(data);
        } else {
            var removed = beacons.remove(pos);
            if (removed != null) { unindex(removed); removed.data = null; }
        }
        if (state.is(MAGIC_BLOCKS) && covers(pos,false) || state.is(MECHANISM_BLOCKS) && covers(pos,true)) dirtyBlocks.add(pos.immutable());
        // Очередь хранит только новые жидкости внутри активных областей, а не все изменения мира.
        if (!liquidAuras.isEmpty() && IS_LIQUID.test(state) && liquidCovered(pos)) dirtyLiquids.add(pos.immutable());
    }
    private void schedule(Beacon state) {
        state.cursor = 0;
        if (!state.queued && state.data != null) { state.queued = true; scans.addLast(state); }
    }
    public void chunkLoaded(LevelChunk chunk) {
        // Штатный список BE включает упакованный NBT: нет обхода всех блоков или синхронной загрузки соседей.
        for (var pos : chunk.getBlockEntitiesPos())
            if ((chunk.getBlockState(pos).is(MAGIC_BEACON_BLOCK) || chunk.getBlockState(pos).is(MECHANISM_BEACON_BLOCK)) && chunk.getBlockEntity(pos) instanceof BeaconData data) loadedBlock(data);
        var nearby = regions.get(ChunkPos.pack(chunk.getPos().x(),chunk.getPos().z()));
        if (nearby != null) for (var state : nearby) schedule(state);
        long chunkKey = ChunkPos.pack(chunk.getPos().x(),chunk.getPos().z());
        // Полный чанк может ещё не быть виден getChunkNow внутри load callback; читаем на следующем pulse.
        if (liquidRegions.containsKey(chunkKey)) dirtyLiquidChunks.add(chunkKey);
    }
    public void chunkUnloaded(LevelChunk chunk) {
        dirtyLiquidChunks.remove(ChunkPos.pack(chunk.getPos().x(),chunk.getPos().z()));
        var nearby = regions.get(ChunkPos.pack(chunk.getPos().x(),chunk.getPos().z()));
        if (nearby == null) return;
        for (var state : List.copyOf(nearby)) if ((state.pos.getX()>>4) == chunk.getPos().x() && (state.pos.getZ()>>4) == chunk.getPos().z()) {
            unindex(state); state.data = null;
        }
    }
    public void tick() {
        flushCleansing();
        if (stopping || game.stage() == GameController.Stage.ENDING) return;
        tickArrows();
        if (game.server().getTickCount()%2 == 0) tickExplosions();
        if (game.server().getTickCount()%5 == 0) { colors.tickWater(); tickLiquids(); tickColors(); tickPassives(); colors.tickTeleports(); }
        if (game.server().getTickCount()%20 == 0) second();
        if (game.server().getTickCount()%2 == 0) pulse();
        int budget = BLOCK_BUDGET;
        var changed = dirtyBlocks.iterator();
        while (budget > 0 && changed.hasNext()) {
            var pos = changed.next(); changed.remove(); budget--;
            if (covers(pos,false)) erase(pos,false);
            if (covers(pos,true)) erase(pos,true);
        }
        while (budget > 0 && !scans.isEmpty()) {
            var state = scans.removeFirst(); state.queued = false;
            if (state.data == null || state.data.isRemoved() || beacons.get(state.pos) != state) continue;
            var offsets = state.mechanism ? MECHANISM_CUBE : SPHERE;
            if (state.mechanism && state.data.notBlock) continue;
            int work = Math.min(Math.min(64,budget), offsets.size()-state.cursor); budget -= work;
            for (int end = state.cursor+work; state.cursor < end; state.cursor++) {
                var offset = offsets.get(state.cursor);
                erase(scanPos.setWithOffset(state.pos,offset.getX(),offset.getY(),offset.getZ()),state.mechanism,beaconOwner(state));
            }
            if (state.cursor < offsets.size()) { state.queued = true; scans.addLast(state); }
        }
    }
    void second() {
        var level = game.maps().level();
        for (var state : loadedBeacons) if (!state.mechanism)
            level.sendParticles(ParticleTypes.WITCH,state.center.x,state.center.y,state.center.z,10,.2,.4,.2,0);
    }
    /** Однократная сверка после старта/перезагрузки тегов: события могли пройти до создания контроллера. */
    public void refreshEntityIndexes() {
        arrows.clear(); unstable.clear(); falling.clear();
        var level = game.maps().level();
        if (level == null) return;
        for (var entity : level.getAllEntities()) if (entity.isAlive()) {
            if (entity instanceof Marker marker && (marker.entityTags().contains("stopper_magic") || marker.entityTags().contains("stopper"))) loaded(marker);
            if(entity instanceof net.minecraft.world.entity.LivingEntity living) game.items().mana().loaded(living);
            colors.loaded(entity);
            if (entity.getType().builtInRegistryHolder().is(ARROWS)) arrows.add(entity);
            if (entity.getType().builtInRegistryHolder().is(UNSTABLE)) unstable.add(entity);
            if (entity instanceof FallingBlockEntity) falling.add(entity);
        }
    }
    public void refreshArrowIndex() { refreshEntityIndexes(); }
    public int trackedArrowCount() { return arrows.size(); }
    public int trackedUnstableCount() { return unstable.size(); }
    public int trackedFallingCount() { return falling.size(); }

    /** Обходим меньший набор: редкие стрелы либо локальные области носителей, используя индекс Minecraft. */
    void tickArrows() {
        intercept(IS_ARROW, arrowArtifacts, arrows, 16, NO_ARROWS);
    }
    /** Full Shulker armor invokes the same noarrows operation every active tick. */
    void eraseArmorArrows(ServerPlayer player) {
        if(stopping || arrows.isEmpty() || !interceptorHolder(player,NO_ARROWS))return;
        var at=player.position();
        for(var arrow:player.level().getEntities((Entity)null,new AABB(at,at).inflate(16),
                entity->IS_ARROW.test(entity)&&entity.isAlive()&&entity.distanceToSqr(at)<=256)){
            eraseIntercepted(arrow,NO_ARROWS);
            if(!arrow.isAlive())arrows.remove(arrow);
        }
    }
    /** Исходный players_2t: очищаем всю группу unstable, включая криперов и фейерверки. */
    void tickExplosions() {
        intercept(IS_UNSTABLE, explosionArtifacts, unstable, 24, NO_EXPLOSIONS);
    }
    private void intercept(Predicate<Entity> matches, Set<UUID> holders, Set<Entity> indexed, int radius, int ability) {
        if (stopping || game.stage() == GameController.Stage.ENDING || holders.isEmpty() || indexed.isEmpty()) return;
        var level = game.maps().level();
        int squared = radius*radius;
        try {
            if (indexed.size() > holders.size()) {
                for (var id : List.copyOf(holders)) {
                    var player = game.server().getPlayerList().getPlayer(id);
                    if (player == null || !interceptorHolder(player, ability)) continue;
                    var pos = player.position();
                    level.getEntities(ENTITIES, new AABB(pos,pos).inflate(radius+1e-6),
                            entity -> matches.test(entity) && entity.isAlive()
                                    && entity.distanceToSqr(pos) <= squared, interceptionBatch);
                    for (var entity : interceptionBatch) {
                        if (stopping || game.stage() == GameController.Stage.ENDING) return;
                        eraseIntercepted(entity, ability);
                        if (!entity.isAlive()) indexed.remove(entity);
                    }
                    // Уже удалённые сущности отсутствуют в следующем запросе пересекающейся области.
                    interceptionBatch.clear();
                }
                return;
            }
            // kill вызывает ENTITY_UNLOAD и меняет индекс, поэтому обход идёт по переиспользуемому снимку.
            interceptionBatch.addAll(indexed);
            for (var entity : interceptionBatch) {
                if (stopping || game.stage() == GameController.Stage.ENDING) return;
                if (!entity.isAlive() || entity.level() != level || !matches.test(entity)) {
                    indexed.remove(entity); continue;
                }
                var pos = entity.position();
                if (!level.hasEntities(PLAYERS, new AABB(pos,pos).inflate(radius+1e-6), player ->
                        holders.contains(player.getUUID()) && interceptorHolder(player, ability) && player.distanceToSqr(pos) <= squared)) continue;
                eraseIntercepted(entity, ability);
                if (!entity.isAlive()) indexed.remove(entity);
            }
        } finally { interceptionBatch.clear(); }
    }
    private boolean interceptorHolder(ServerPlayer player, int ability) {
        return game.canUseGameplayItems(player) && player.isAlive() && !player.isRemoved()
                && player.level() == game.maps().level()
                // players_2t не имеет фильтра времени; time_stopped проверяется только в ветке стрел.
                && (ability != NO_ARROWS || !player.entityTags().contains("time_stopped"));
    }
    private void eraseIntercepted(Entity entity, int ability) {
        // @e исключает мёртвых: у крипера после /kill ещё идёт нативная анимация смерти и выпадение loot.
        if (!entity.isAlive()) return;
        var level = game.maps().level(); var pos = entity.position();
        if (ability == NO_LIQUIDS) {
            var replacement = fallingReplacement(((FallingBlockEntity)entity).getBlockState());
            // Y сущности — низ блока: смещение выбирает ближайшую клетку без скачка вниз при начале падения.
            var target = BlockPos.containing(pos.x, pos.y + .5, pos.z);
            if (replacement != null && level.getChunkSource().getChunkNow(target.getX()>>4,
                    target.getZ()>>4) != null) {
                var old = level.getBlockState(target);
                if (level.setBlock(target,replacement.defaultBlockState(),Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
                    level.updateNeighboursOnBlockSet(target,old);
            }
        }
        boolean explosion = ability == NO_EXPLOSIONS, hardening = ability == NO_LIQUIDS;
        // Владельцы, команды, состояние снаряда и антимагические маяки не ограничивают исходные селекторы.
        level.sendParticles(explosion ? ParticleTypes.EXPLOSION : hardening ? ParticleTypes.SMOKE : ParticleTypes.CLOUD,
                pos.x,pos.y,pos.z,explosion ? 1 : 20,hardening ? 1 : 0,hardening ? 1 : 0,hardening ? 1 : 0,
                explosion ? 1 : hardening ? .1 : .3);
        long soundSeed = level.getRandom().nextLong();
        var sound = new ClientboundSoundPacket(explosion ? SoundEvents.GENERIC_EXPLODE : net.minecraft.core.Holder.direct(SoundEvents.FIRE_EXTINGUISH),
                SoundSource.MASTER,pos.x,pos.y,pos.z,1,1,soundSeed);
        // Исходный /playsound @a с minVolume=0 отправляет звук лишь в том же мире и в пределах 16.
        for (var viewer : level.players()) if (viewer.distanceToSqr(pos) <= 256) viewer.connection.send(sound);
        entity.kill(level);
    }

    private static Block liquidReplacement(BlockState state) {
        if (state.is(Blocks.LAVA)) return Blocks.BASALT;
        if (state.is(Blocks.WATER)) return Blocks.FROSTED_ICE;
        return state.is(Blocks.POWDER_SNOW) ? Blocks.SNOW_BLOCK : null;
    }
    private static Block fallingReplacement(BlockState state) { return FALLING_REPLACEMENTS.get(state.getBlock()); }
    public int liquidAuraCount() { return liquidAuras.size(); }
    public int pendingLiquidCount() { return dirtyLiquids.size(); }

    /** Источник выполнял три fill каждые пять тиков; (user) теперь область сферическая. */
    void tickLiquids() {
        if (stopping || game.stage() == GameController.Stage.ENDING || liquidArtifacts.isEmpty()) return;
        var level = game.maps().level();
        for (var id : List.copyOf(liquidArtifacts)) {
            var player = game.server().getPlayerList().getPlayer(id);
            if (player == null) { liquidArtifacts.remove(id); removeLiquidAura(id); continue; }
            if (!interceptorHolder(player, NO_LIQUIDS) || beaconSuppressed(player.position())) {
                removeLiquidAura(id); continue;
            }
            var center = player.blockPosition().immutable();
            var aura = liquidAuras.get(id);
            if (aura != null && aura.center.equals(center)) continue;
            BlockPos old = aura == null ? null : aura.center;
            if (aura == null) { aura = new LiquidAura(center); liquidAuras.put(id,aura); }
            else indexLiquidAura(aura,false);
            aura.center = center; indexLiquidAura(aura,true);
            findNewLiquids(center,old);
        }
        for (long key : List.copyOf(dirtyLiquidChunks)) {
            var auras = liquidRegions.get(key);
            if (auras == null) { dirtyLiquidChunks.remove(key); continue; }
            int x = ChunkPos.getX(key), z = ChunkPos.getZ(key);
            if (level.getChunkSource().getChunkNow(x,z) == null) continue;
            dirtyLiquidChunks.remove(key);
            for (var aura : auras) {
                var c = aura.center;
                findLiquids(Math.max(c.getX()-LIQUID_RADIUS,x<<4),c.getY()-LIQUID_RADIUS,Math.max(c.getZ()-LIQUID_RADIUS,z<<4),
                        Math.min(c.getX()+LIQUID_RADIUS,(x<<4)+15),c.getY()+LIQUID_RADIUS,Math.min(c.getZ()+LIQUID_RADIUS,(z<<4)+15),c,null);
            }
        }
        var changed = List.copyOf(dirtyLiquids); dirtyLiquids.clear();
        try {
            // Сохраняем порядок lava → water → powder snow и пакетные neighbor updates как у трёх fill.
            for (var source : LIQUID_SOURCE) {
                for (var pos : changed) {
                    if (!liquidCovered(pos)) continue;
                    var chunk = level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                    if (chunk == null) continue;
                    var old = chunk.getBlockState(pos);
                    if (!old.is(source)) continue;
                    if (level.setBlock(pos,liquidReplacement(old).defaultBlockState(),Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
                        liquidUpdates.put(pos,old);
                }
                for (var entry : liquidUpdates.entrySet()) level.updateNeighboursOnBlockSet(entry.getKey(),entry.getValue());
                liquidUpdates.clear();
            }
        } finally { liquidUpdates.clear(); }
        intercept(IS_FALLING,liquidAuras.keySet(),falling,LIQUID_RADIUS,NO_LIQUIDS);
    }
    private boolean liquidCovered(BlockPos pos) {
        var nearby = liquidRegions.get(ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4));
        if (nearby != null) for (var aura : nearby) {
            var c = aura.center;
            long x=(long)pos.getX()-c.getX(),y=(long)pos.getY()-c.getY(),z=(long)pos.getZ()-c.getZ();
            if (x*x+y*y+z*z <= LIQUID_RADIUS*LIQUID_RADIUS) return true;
        }
        return false;
    }
    private void indexLiquidAura(LiquidAura aura, boolean add) {
        var c = aura.center;
        for (int x = (c.getX()-LIQUID_RADIUS)>>4; x <= (c.getX()+LIQUID_RADIUS)>>4; x++)
            for (int z = (c.getZ()-LIQUID_RADIUS)>>4; z <= (c.getZ()+LIQUID_RADIUS)>>4; z++) {
                long key = ChunkPos.pack(x,z);
                if (add) liquidRegions.computeIfAbsent(key,ignored -> new LinkedHashSet<>()).add(aura);
                else {
                    var auras = liquidRegions.get(key);
                    if (auras != null) { auras.remove(aura); if (auras.isEmpty()) liquidRegions.remove(key); }
                }
            }
    }
    private void removeLiquidAura(UUID id) {
        var aura = liquidAuras.remove(id);
        if (aura != null) indexLiquidAura(aura,false);
        if (liquidAuras.isEmpty()) { dirtyLiquids.clear(); dirtyLiquidChunks.clear(); }
    }
    /** Читаем новые части сферы по строкам, включая участки внутри пересечения старого/нового bounding box. */
    private void findNewLiquids(BlockPos center, BlockPos old) {
        int x0=center.getX()-LIQUID_RADIUS,y0=center.getY()-LIQUID_RADIUS,z0=center.getZ()-LIQUID_RADIUS;
        int x1=center.getX()+LIQUID_RADIUS,y1=center.getY()+LIQUID_RADIUS,z1=center.getZ()+LIQUID_RADIUS;
        findLiquids(x0,y0,z0,x1,y1,z1,center,old);
    }
    /** Читаем только уже видимые полные чанки; палитра позволяет пропустить секции без нужных состояний. */
    private void findLiquids(int x0,int y0,int z0,int x1,int y1,int z1,BlockPos center,BlockPos old) {
        var level = game.maps().level();
        y0=Math.max(y0,level.getMinY()); y1=Math.min(y1,level.getMaxY());
        if (x0>x1 || y0>y1 || z0>z1) return;
        for (int cx=x0>>4;cx<=x1>>4;cx++) for (int cz=z0>>4;cz<=z1>>4;cz++) {
            var chunk = level.getChunkSource().getChunkNow(cx,cz);
            if (chunk == null) continue;
            int minX=Math.max(x0,cx<<4),maxX=Math.min(x1,(cx<<4)+15);
            int minZ=Math.max(z0,cz<<4),maxZ=Math.min(z1,(cz<<4)+15);
            for (int sy=y0>>4;sy<=y1>>4;sy++) {
                var section = chunk.getSections()[chunk.getSectionIndexFromSectionY(sy)];
                if (section.hasOnlyAir() || !section.maybeHas(IS_LIQUID)) continue;
                int minY=Math.max(y0,sy<<4),maxY=Math.min(y1,(sy<<4)+15);
                for (int z=minZ;z<=maxZ;z++) for (int y=minY;y<=maxY;y++) {
                    int width=liquidWidth(y-center.getY(),z-center.getZ());
                    if (width<0) continue;
                    int left=Math.max(minX,center.getX()-width),right=Math.min(maxX,center.getX()+width);
                    int previous=old==null ? -1 : liquidWidth(y-old.getY(),z-old.getZ());
                    if (previous<0) collectLiquids(section,left,right,y,z);
                    else {
                        collectLiquids(section,left,Math.min(right,old.getX()-previous-1),y,z);
                        collectLiquids(section,Math.max(left,old.getX()+previous+1),right,y,z);
                    }
                }
            }
        }
    }
    private static int liquidWidth(int y,int z) {
        return Math.abs(y)>LIQUID_RADIUS || Math.abs(z)>LIQUID_RADIUS ? -1 : LIQUID_WIDTHS[y+LIQUID_RADIUS][z+LIQUID_RADIUS];
    }
    private void collectLiquids(net.minecraft.world.level.chunk.LevelChunkSection section,int left,int right,int y,int z) {
        for (int x=left;x<=right;x++)
            if (IS_LIQUID.test(section.getBlockState(x&15,y&15,z&15))) dirtyLiquids.add(new BlockPos(x,y,z));
    }

    /** Цветной блок читается только для учтённых носителей, два раза за пульс без обхода области. */
    void tickColors() {
        if (stopping || game.stage() == GameController.Stage.ENDING) return;
        for (var entry : List.copyOf(artifactTypes.entrySet())) {
            if ((entry.getValue() & COLORS) == 0) continue;
            var player = game.server().getPlayerList().getPlayer(entry.getKey());
            if (player == null) { artifactTypes.remove(entry.getKey()); continue; }
            if (!game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                    || player.level() != game.maps().level()) { clear(player); continue; }
            if (!beaconSuppressed(player.position())) colors.pulse(player);
        }
    }
    /** Пять тиков исходного dispatcher: посещаем только учтённых носителей, не сканируем их инвентарь повторно. */
    void tickPassives() {
        if (stopping || game.stage() == GameController.Stage.ENDING || artifactTypes.isEmpty()) return;
        for (var entry : List.copyOf(artifactTypes.entrySet())) {
            int types = entry.getValue();
            if ((types & (METABOLISM | CLEAR_DEBUFFS | CLEAR_EFFECTS)) == 0) continue;
            var player = game.server().getPlayerList().getPlayer(entry.getKey());
            if (player == null) { artifactTypes.remove(entry.getKey()); activeArtifacts.remove(entry.getKey()); continue; }
            if (!game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                    || player.level() != game.maps().level()) { clear(player); continue; }
            // Source запрещает эти пассивы только возле маяка, а не возле другого антимагического артефакта.
            if (beaconSuppressed(player.position())) continue;
            // HEALTH objective округляет HP + absorption вверх, /attribute get усекает MaxHealth.
            // Сохраняем это условие напрямую, без scoreboard-записей и их задержки обновления.
            if ((types & METABOLISM) != 0 && Mth.ceil(player.getHealth()+player.getAbsorptionAmount())
                    < (int)player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)) {
                player.addEffect(ItemEffectLevels.create(MobEffects.HUNGER,20,2));
                sustainRegeneration(player);
            }
            // Исходный порядок 8 → 5 → 6: выборочная очистка убирает Hunger, полная — также Regen.
            if ((types & CLEAR_DEBUFFS) != 0) clearDebuffs(player);
            // Кастомный cold/frozen существует и без potion effects, поэтому проверка их списка здесь неверна.
            if ((types & CLEAR_EFFECTS) != 0) game.items().clearEffects(player);
        }
    }
    private void clearDebuffs(ServerPlayer player) {
        game.items().quiver().clear(player);
        game.items().babylonEffects().clear(player);
        game.items().kiuas().clearCurse(player);
        game.items().kfcFood().clear(player);
        game.items().electro().clearEffects(player);
        game.items().shooters().clearStun(player);
        if (player.getActiveEffects().isEmpty()) return;
        // Snapshot нужен: native onEffectsRemoved синхронно запускает другие обработчики.
        for (var effect : List.copyOf(player.getActiveEffects()))
            if (cleansedDebuff(player, effect.getEffect())) player.removeEffect(effect.getEffect());
    }
    private static boolean cleansedDebuff(ServerPlayer player, Holder<MobEffect> effect) {
        return effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.SLOWNESS) || effect.is(MobEffects.WEAKNESS)
                || effect.is(MobEffects.MINING_FATIGUE) || effect.is(MobEffects.GLOWING) || effect.is(MobEffects.HUNGER)
                || effect.is(MobEffects.INSTANT_DAMAGE) || effect.is(MobEffects.NAUSEA) || effect.is(MobEffects.POISON)
                || effect.is(MobEffects.UNLUCK) || effect.is(MobEffects.WITHER)
                || effect.is(MobEffects.LEVITATION) && !protectsLevitation(player);
    }
    private static boolean protectsLevitation(ServerPlayer player) {
        var feet = player.getItemBySlot(EquipmentSlot.FEET);
        var legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (matches(feet, REACTIVE) || matches(feet, TECHNO_MK2)
                || matches(legs, TECHNO_MK2) || matches(legs, TECHNO)) return true;
        for (int slot = 0; slot < 36; slot++) if (matches(player.getInventory().getItem(slot), REACTIVE_ROD)) return true;
        return matches(player.getOffhandItem(), REACTIVE_ROD);
    }
    private static boolean matches(ItemStack stack, CompoundTag pattern) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.matchedBy(pattern);
    }
    /** (user) Регенерация должна лечить. Сдвиг на полный период сохраняет фазу нативного эффекта. */
    private static void sustainRegeneration(ServerPlayer player) {
        var current = player.getEffect(MobEffects.REGENERATION);
        int duration = 45;
        if (current != null && current.getAmplifier() == 1 && !current.isInfiniteDuration()) {
            duration = current.getDuration();
            if (duration <= 20) duration += 25;
        }
        // Нативное слияние сохраняет приоритет сильного эффекта и скрытые более слабые эффекты.
        player.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,duration,1));
    }
    void pulse() {
        var level = game.maps().level();
        // Legacy eggs are migrated once to the same persistent native carrier and shared scan/index.
        for (var marker : List.copyOf(legacyMarkers)) {
            if (marker.isRemoved()) { legacyMarkers.remove(marker); continue; }
            if (!marker.entityTags().contains("stopper")) continue;
            var pos = marker.blockPosition();
            var old = level.getBlockState(pos);
            if (old.is(MECHANISM_BEACON_BLOCK) && level.getBlockEntity(pos) instanceof BeaconData existing) {
                existing.notBlock = marker.entityTags().contains("not_block"); existing.setChanged();
                loadedBlock(existing); marker.discard(); continue;
            }
            if (marker.entityTags().contains("activated") && !old.is(Blocks.LIGHTNING_ROD.weathering().unaffected())
                    && !old.is(MECHANISM_BEACON_BLOCK)) {
                marker.discard(); continue;
            }
            if (!old.isAir() && !old.is(Blocks.LIGHTNING_ROD.weathering().unaffected()) && !old.is(MECHANISM_BEACON_BLOCK)) pos=pos.above();
            if (level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null) continue;
            if (level.setBlock(pos,MECHANISM_BEACON_BLOCK.defaultBlockState(),Block.UPDATE_ALL)
                    && level.getBlockEntity(pos) instanceof BeaconData data) {
                data.notBlock = marker.entityTags().contains("not_block"); data.setChanged();
                loadedBlock(data); marker.discard();
            }
        }
        for (var state : List.copyOf(loadedBeacons)) {
            var area = new AABB(state.center,state.center).inflate(RADIUS+1e-6);
            for (Entity entity : level.getEntities((Entity)null,area,entity -> !entity.isRemoved() && entity.isAlive()
                    && (state.mechanism ? mechanical(entity) : magical(entity))
                    && state.center.distanceToSqr(entity.position()) <= RADIUS_SQUARED)) {
                if (stopping || game.stage() == GameController.Stage.ENDING) return;
                if (entity.isRemoved() || !(state.mechanism ? mechanical(entity) : magical(entity))) continue;
                if (!state.mechanism || entity.getType().builtInRegistryHolder().is(MECHANISM_ENTITIES))
                    level.sendParticles(state.mechanism ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.WITCH,
                            entity.getX(),entity.getY(),entity.getZ(),20,.3,.3,.3,.1);
                var owner=state.mechanism ? beaconOwner(state) : null;
                if (state.mechanism && entity.entityTags().contains("nuke")) cleanLegacyNuke(entity.blockPosition(),owner);
                if (state.mechanism && game.items().helicopterEncounter().mechanismRemoved(entity)) continue;
                if (owner!=null && entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    var damage=new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL),owner);
                    living.hurtServer(level,damage,Float.MAX_VALUE);
                } else entity.kill(level);
            }
            if (state.mechanism) for (var player : level.getEntitiesOfClass(ServerPlayer.class,area,
                    player -> !player.isSpectator() && player.position().distanceToSqr(state.center)<=RADIUS_SQUARED)) {
                removeLegacyModifier(player,net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,"c1cda724-0ea2-460a-98b8-9631eafd2515");
                removeLegacyModifier(player,net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,"585f98b8-a8b8-4e4c-838a-f2e124831040");
            }
        }
    }
    private boolean mechanical(Entity entity) {
        var tags=entity.entityTags();
        return !(entity instanceof ServerPlayer) && !entity.isSpectator() && !tags.contains("spectator")
                && !tags.contains("strict_map_object") && !tags.contains("core_marker") && !tags.contains("map_object")
                && !tags.contains("rcdenyalways") && !tags.contains("sauvojen_tuntija")
                && !tags.contains("magic_academy") && !tags.contains("transformator")
                && (!(entity instanceof Marker || entity instanceof net.minecraft.world.entity.Display
                    || entity instanceof net.minecraft.world.entity.Interaction || entity instanceof net.minecraft.world.entity.AreaEffectCloud)
                    || tags.stream().noneMatch(tag -> tag.startsWith("swrg.")))
                && (entity.getType().builtInRegistryHolder().is(MECHANISM_ENTITIES) || tags.contains("mecha")
                    || entity.getType()==EntityTypes.SKELETON && tags.contains("cyborg_boss"));
    }
    private static void removeLegacyModifier(ServerPlayer player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String id) {
        var instance=player.getAttribute(attribute);
        if(instance!=null) instance.removeModifier(Identifier.parse("minecraft:"+id));
    }
    private void cleanLegacyNuke(BlockPos center, ServerPlayer owner) {
        var level=game.maps().level();
        for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++) for(int z=-1;z<=1;z++) {
            var pos=center.offset(x,y,z);
            if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null) continue;
            var old=level.getBlockState(pos);
            if(old.is(Blocks.NETHERITE_BLOCK) || old.is(Blocks.BEACON)) eraseOwnedBlock(pos,owner);
        }
        var top=center.above(2);
        if(level.getChunkSource().getChunkNow(top.getX()>>4,top.getZ()>>4)!=null) eraseOwnedBlock(top,owner);
    }
    private ServerPlayer beaconOwner(Beacon state) {
        return state.data == null || state.data.placer == null ? null : game.server().getPlayerList().getPlayer(state.data.placer);
    }
    private void erase(BlockPos pos, boolean mechanism) {
        ServerPlayer owner=null;
        if(mechanism) {
            var nearby=regions.get(ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4));
            if(nearby!=null) for(var state:nearby) if(state.mechanism && state.data!=null && inside(state,pos)) {
                owner=beaconOwner(state); if(owner!=null) break;
            }
        }
        erase(pos,mechanism,owner);
    }
    private void erase(BlockPos pos, boolean mechanism, ServerPlayer owner) {
        var level = game.maps().level();
        if (level.hasChunkAt(pos) && level.getBlockState(pos).is(mechanism ? MECHANISM_BLOCKS : MAGIC_BLOCKS))
            eraseOwnedBlock(pos,mechanism ? owner : null);
    }
    private void eraseOwnedBlock(BlockPos pos, ServerPlayer owner) {
        var level=game.maps().level();
        if(owner==null) { level.setBlock(pos,Blocks.AIR.defaultBlockState(),STRICT_FLAGS); return; }
        game.items().indirectKills().supportRemoved(level,owner,pos);
        // worldChanges also finishes this bounded support-removal batch in its finally block.
        game.items().indirectKills().worldChanges(level,owner,()->level.setBlock(pos,Blocks.AIR.defaultBlockState(),STRICT_FLAGS));
    }
    public int beaconCount() { return beacons.size(); }
    public int pendingBlockCount() { return dirtyBlocks.size()+scans.size(); }
    public void stop() {
        stopping = true;
        colors.stop();
        var states = List.copyOf(beacons.values()); beacons.clear(); loadedBeacons.clear(); regions.clear();
        dirtyBlocks.clear(); scans.clear(); dirtyInventories.clear(); pendingCleansing.clear(); activeArtifacts.clear(); arrowArtifacts.clear(); explosionArtifacts.clear();
        liquidArtifacts.clear(); liquidAuras.clear(); liquidRegions.clear(); dirtyLiquids.clear(); dirtyLiquidChunks.clear(); liquidUpdates.clear(); artifactTypes.clear();
        for (var marker : List.copyOf(legacyMarkers)) marker.discard();
        legacyMarkers.clear();
        for (var state : states) {
            // Выгруженные блоки уберёт существующий журнал/очередь MapManager, без принудительной загрузки чанков.
            game.maps().recordBlockChange(state.pos);
            if (game.maps().level().hasChunkAt(state.pos) && (game.maps().level().getBlockState(state.pos).is(MAGIC_BEACON_BLOCK) || game.maps().level().getBlockState(state.pos).is(MECHANISM_BEACON_BLOCK)))
                game.maps().level().setBlock(state.pos,Blocks.AIR.defaultBlockState(),STRICT_FLAGS);
        }
        for (var player : game.server().getPlayerList().getPlayers()) player.entityTags().remove("nomagic_active");
        stopping = false;
    }
}
