package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.TrapezoidInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Полёт Terra и его Polymer-модель; расход и бонусы каста остаются в общем LbcWands. */
public final class LbcTerra {
    public static final EntityType<TerraProjectile> TYPE = registerType();
    public static final Item SWORD = registerSword("skywars2:terra_sword", "terra_sword", "lbc:terrasword", 25);
    public static final Item TRUE_SWORD = registerSword("skywars2:true_terra_sword", "true_terra", "lbc:terrasword_true", 9000);
    public static final Item RAZOR = registerSword("skywars2:terra_razorpine", "razorterra", "lbc:terra_razorpine", 25);
    private static final CompoundTag SWORD_FLAG = flag("terrasword"), TRUE_SWORD_FLAG = flag("true_terra");
    private static final TagKey<Block> AIRS = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final DustColorTransitionOptions DUST = new DustColorTransitionOptions(0x00ff00, 0x006600, 1);
    // Native RGB codec упаковывает исходные float-каналы в 8 бит (включая source green=2).
    private static final DustColorTransitionOptions ACADEMY_DUST = new DustColorTransitionOptions(0xd5ffae, 0xeffee1, 1);
    private static final DustParticleOptions BEAM_DUST = new DustParticleOptions(0x81ff81, 1);
    private static final double[] BEAM_SIDES = { 0, .2, .4, .6, .75, -.2, -.4, -.6, -.75 };
    private static final double[] BEAM_BACK = { 0, .07, .15, .2, .25, .07, .15, .2, .25 };
    private static final double BEAM_STEP = .33, BEAM_HIT_PADDING = .3;
    private static final double[] BEAM_DAMAGE_SIDES = { 0, .6, -.6 };
    /** Варианты задают отличия способности, не размножая обработчики движения и collision. */
    public enum Kind {
        REGULAR("lbc:terra_charge",1.8,8,7,0),
        ACADEMY("lbc:terrasword_true_charge",7.5,17,26,0),
        SWORD("lbc:terrasword_charge",1.8,8,7,40),
        TRUE_CHARGE("lbc:terrasword_true_charge",7.5,17,26,20),
        TRUE_BEAM("lbc:terrasword_true_beam",1.32,1,3,50),
        RAZOR("lbc:terrasword_true_beam",7.5,17,26,10);
        final Identifier model;
        final double speed;
        final float damage;
        final int samples;
        final int lifetime;
        Kind(String model,double speed,float damage,int samples,int lifetime) {
            this.model=Identifier.parse(model); this.speed=speed; this.damage=damage;
            this.samples=samples; this.lifetime=lifetime;
        }
    }
    private final GameController game;
    private final Set<TerraProjectile> projectiles = new LinkedHashSet<>();
    private final ArrayList<TerraProjectile> batch = new ArrayList<>();
    private final Map<UUID, Integer> swordCharges = new HashMap<>();
    // Только визуальный хвост: после попадания gameplay-сущность уже удалена и урон не повторяется.
    private record FinishingModel(ElementHolder holder, int removeAt) { }
    private final ArrayDeque<FinishingModel> finishingModels = new ArrayDeque<>();
    private int modelTicks;

    static void register() { }
    private static CompoundTag flag(String name) {
        var tag = new CompoundTag(); tag.putInt(name, 1); return tag;
    }
    private static boolean matches(ItemStack stack, Item item, CompoundTag flag) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(item) || data != null && data.matchedBy(flag);
    }
    public static boolean isSword(ItemStack stack) {
        return matches(stack, SWORD, SWORD_FLAG) || matches(stack, TRUE_SWORD, TRUE_SWORD_FLAG);
    }
    private static boolean flagged(ItemStack stack,String name) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && data!=null && data.copyTag().getIntOr(name,0)==1;
    }
    public static boolean isRazor(ItemStack stack) { return stack.is(RAZOR) || flagged(stack,"razorterra"); }

    /** Каждый занятый слот — один ингредиент; остатки стопок расходует сам ResultSlot, без поиска доноров. */
    public static LbcCraftingRecipe.Kind craftingKind(net.minecraft.world.item.crafting.CraftingInput input) {
        if(input.ingredientCount()!=2) return null;
        int tiny=0,wand=0,sword=0,razor=0;
        for(var stack:input.items()) {
            if(stack.isEmpty()) continue;
            if(stack.is(LbcWands.TINY_TERRA) || flagged(stack,"stterra_tiny")) tiny++;
            else if(stack.is(LbcWands.TERRA) || stack.is(LbcWands.ACADEMY_TERRA) || flagged(stack,"stterra")) wand++;
            else if(stack.is(SWORD) || flagged(stack,"terrasword")) sword++;
            else if(flagged(stack,"razorpine")) razor++;
            else return null;
        }
        if(tiny==2) return LbcCraftingRecipe.Kind.TERRA;
        if(wand==1 && sword==1) return LbcCraftingRecipe.Kind.SWORD;
        return wand==1 && razor==1 ? LbcCraftingRecipe.Kind.RAZOR : null;
    }
    private static Item registerSword(String id, String name, String model, int durability) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
        var properties = new Item.Properties().setId(key).durability(durability).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL, Identifier.parse(model));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, Items.CARROT_ON_A_STICK, true) {
            @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                var game = me.skywars2.SkyWars2.game();
                return game != null && player instanceof ServerPlayer owner
                        && (isRazor(owner.getItemInHand(hand)) ? game.items().terra().useRazor(owner,hand) : game.items().terra().useSword(owner, hand))
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
    }

    public boolean useRazor(ServerPlayer player,InteractionHand hand) {
        boolean applied=castRazor(player,hand);
        if(hand==InteractionHand.MAIN_HAND && game.config().offhandWands)
            applied=(isRazor(player.getOffhandItem()) ? castRazor(player,InteractionHand.OFF_HAND)
                    : game.items().wands().use(player,InteractionHand.OFF_HAND)) || applied;
        return applied;
    }
    private boolean castRazor(ServerPlayer player,InteractionHand hand) {
        if(!isRazor(player.getItemInHand(hand)) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                || player.level()!=game.maps().level() || game.items().frost().isFrozen(player)
                || player.entityTags().contains("time_stopped") || game.items().magic().suppressed(player)) return false;
        cast(player,Kind.RAZOR);
        return true;
    }
    public int charges(ServerPlayer player) { return swordCharges.getOrDefault(player.getUUID(), 0); }
    void resetCharges() { swordCharges.clear(); }
    /** (user) Только melee мечом или собственный заряд этого меча; прочий приписанный урон не подходит. */
    void damaged(ServerPlayer owner, DamageSource source) {
        var held = owner.getMainHandItem();
        boolean melee = LbcDamage.isMeleeAttack(source, owner);
        Kind charge = source.getDirectEntity() instanceof TerraProjectile projectile && projectile.getOwner() == owner
                ? projectile.kind : null;
        if (matches(held, SWORD, SWORD_FLAG) && (melee || charge == Kind.SWORD))
            swordCharges.put(owner.getUUID(), Math.min(4, charges(owner) + 1));
        if (matches(held, TRUE_SWORD, TRUE_SWORD_FLAG) && (melee || charge == Kind.TRUE_CHARGE)) cast(owner, Kind.TRUE_BEAM);
    }
    int hudCharges(ServerPlayer player) {
        return matches(player.getMainHandItem(), SWORD, SWORD_FLAG)
                || matches(player.getOffhandItem(), SWORD, SWORD_FLAG) ? charges(player) : -1;
    }

    /** Заряды обычного меча принадлежат игроку; улучшенный использует общий wand cast и его бонусы. */
    public boolean useSword(ServerPlayer player, InteractionHand hand) {
        if (!game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                || player.level() != game.maps().level() || game.items().frost().isFrozen(player)
                || player.entityTags().contains("time_stopped") || game.items().magic().suppressed(player)) return false;
        var stack = player.getItemInHand(hand);
        if (matches(stack, TRUE_SWORD, TRUE_SWORD_FLAG)) return game.items().wands().use(player, hand);
        if (hand != InteractionHand.MAIN_HAND || !matches(stack, SWORD, SWORD_FLAG)) return false;
        boolean applied = charges(player) > 0;
        if (applied) {
            swordCharges.put(player.getUUID(), charges(player) - 1);
            cast(player, Kind.SWORD);
        }
        // Старый items/spells обслуживал разрешённый offhand-жезл даже при пустом заряде основного меча.
        return game.items().wands().use(player, InteractionHand.OFF_HAND) || applied;
    }
    private static EntityType<TerraProjectile> registerType() {
        var key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("skywars2:terra_projectile"));
        var type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                EntityType.Builder.<TerraProjectile>of(TerraProjectile::new, MobCategory.MISC)
                        .sized(.1f, .1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }

    public static final class TerraProjectile extends Projectile implements PolymerEntity {
        private ElementHolder holder;
        private ItemDisplayElement display;
        private Kind kind = Kind.REGULAR;
        private int age;
        private boolean modelPaused;
        TerraProjectile(EntityType<? extends TerraProjectile> type, Level level) { super(type, level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
        ItemDisplayElement display() { return display; }

        private void attachModel() {
            var stack = new ItemStack(Items.STICK);
            stack.set(DataComponents.ITEM_MODEL, kind.model);
            display = new ItemDisplayElement(stack);
            display.setItemDisplayContext(ItemDisplayContext.FIXED);
            // Заряды летят лезвием вперёд, дуга развёрнута горизонтально и поперёк направления полёта.
            display.setTransformation(kind == Kind.TRUE_BEAM || kind == Kind.RAZOR
                    ? new Matrix4f().rotateX((float)Math.PI / 2).rotateZ((float)Math.PI / 4)
                    : new Matrix4f()
                    .setRow(0, new Vector4f(0, 0, 2.5f, 0))
                    .setRow(1, new Vector4f(1.7678f, -1.7678f, 0, 0))
                    .setRow(2, new Vector4f(1.7678f, 1.7678f, 0, 0))
                    .setRow(3, new Vector4f(0, 0, 0, 3)));
            display.setInterpolationDuration(1);
            display.setTeleportDuration(1);
            display.setShadowRadius(0);
            display.setShadowStrength(0);
            display.setScale(new Vector3f(1.5f));
            holder = new ElementHolder();
            holder.addElement(display);
            // Attachment сразу подключает зрителей и отправляет spawn: поворот нужен ДО него.
            // До attachment holder.tick() ничего не отправляет, но параметры display уже готовы.
            syncModel(position());
            new EntityAttachment(holder, this, false);
        }
        private void syncModel(Vec3 visualPosition) {
            if (holder == null) return;
            var motion = getDeltaMovement();
            if (motion.lengthSqr() > 1e-12) {
                setYRot((float)-Math.toDegrees(Math.atan2(motion.x, motion.z)));
                setXRot((float)-Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())));
            }
            display.setYaw(getYRot());
            display.setPitch(getXRot());
            // Во время /tick freeze клиент не тикает display: lerp мог бы ждать следующего step.
            // При пошаговой отладке применяем позицию сразу; обычный полёт остаётся плавным.
            boolean stepped = ((ServerLevel) level()).getServer().tickRateManager().isFrozen();
            display.setTeleportDuration(stepped ? 0 : 1);
            display.setAlwaysSyncAbsolutePosition(stepped);
            // Клиент догоняет target за teleport_duration=1: отправляем следующий видимый конец пути.
            display.setOffset(visualPosition.subtract(position()));
            // Ручной tick нужен и в FULL-чанке, который сейчас не entity-ticking.
            holder.tick();
        }
        private void destroyModel() {
            if (holder != null) { holder.destroy(); holder = null; }
        }
    }

    LbcTerra(GameController game) {
        this.game = game;
    }
    public int count() { return projectiles.size(); }
    int finishingModels() { return finishingModels.size(); }

    void loaded(Entity entity) {
        if (entity.level() != game.maps().level()) return;
        if (entity instanceof TerraProjectile projectile) {
            if (!game.items().currentEpoch(projectile) || !(projectile.getOwner() instanceof ServerPlayer)) projectile.discard();
            else projectiles.add(projectile);
        }
    }
    void unloaded(Entity entity) {
        if (entity instanceof TerraProjectile projectile) {
            projectiles.remove(projectile);
            projectile.destroyModel();
        }
    }

    public void cast(ServerPlayer owner) {
        cast(owner,Kind.REGULAR);
    }
    public void cast(ServerPlayer owner,Kind kind) {
        castAt(owner,owner.getEyePosition(),owner.getLookAngle(),kind);
    }
    /** Общий запуск жезла и призванного культиста: модель, попадания и lifecycle совпадают. */
    void castAt(ServerPlayer owner,Vec3 origin,Vec3 direction,Kind kind) {
        var level = owner.level();
        if (level != game.maps().level() || !game.canUseGameplayItems(owner)) return;
        var projectile = new TerraProjectile(TYPE, level);
        projectile.kind = kind;
        projectile.setOwner(owner);
        projectile.setPos(origin);
        projectile.setDeltaMovement(direction.normalize().scale(kind.speed));
        LbcShooters.orient(projectile);
        if(kind==Kind.RAZOR) {
            float yaw=owner.getYRot()+(level.getRandom().nextInt(601)-300)*.01f;
            float pitch=owner.getXRot()+(level.getRandom().nextInt(601)-300)*.01f;
            projectile.setYRot(yaw); projectile.setXRot(pitch);
            projectile.setDeltaMovement(Vec3.directionFromRotation(pitch,yaw).scale(kind.speed));
        }
        projectile.entityTags().add("magic");
        projectile.entityTags().add(game.items().epoch());
        if (level.addFreshEntity(projectile)) {
            projectiles.add(projectile);
            if (kind.lifetime == 0) game.items().lifetimes().track(projectile);
            projectile.attachModel();
            projectile.syncModel(visualEnd(projectile, owner));
            if (kind == Kind.TRUE_BEAM)
                LbcOverload.sound(owner, owner.getEyePosition(), "minecraft:block.beacon.activate", .5f, 2, 0);
            else if (kind == Kind.TRUE_CHARGE || kind == Kind.RAZOR)
                LbcOverload.sound(owner, owner.getEyePosition(), "minecraft:block.amethyst_block.break", 1, 1, 0);
            else if (kind == Kind.SWORD)
                LbcOverload.sound(owner, owner.getEyePosition(), "minecraft:block.amethyst_cluster.break", 1, 0, 0);
            else LbcOverload.sound(owner, owner.getEyePosition(), "minecraft:block.amethyst_cluster.break", 1, 1, 0);
        }
    }

    /** Один swept отрезок на рабочий tick; мир никогда не читается из фонового потока. */
    public void tick() {
        modelTicks++;
        while (!finishingModels.isEmpty() && finishingModels.peekFirst().removeAt() <= modelTicks)
            finishingModels.removeFirst().holder().destroy();
        if (projectiles.isEmpty()) return;
        boolean stopped = game.items().wands().timeStopped();
        batch.addAll(projectiles);
        try {
            for (var projectile : batch) {
                if (!projectiles.contains(projectile)) continue;
                if (projectile.isRemoved() || !(projectile.getOwner() instanceof ServerPlayer owner)
                        || owner.isRemoved() || !owner.isAlive() || owner.level() != projectile.level()
                        || projectile.level() != game.maps().level() || !game.items().currentEpoch(projectile)
                        || !game.canUseGameplayItems(owner) || game.stage() == GameController.Stage.ENDING) {
                    remove(projectile); continue;
                }
                if (stopped || owner.entityTags().contains("time_stopped")) {
                    if (!projectile.modelPaused) projectile.syncModel(projectile.position());
                    projectile.modelPaused = true;
                    continue;
                }
                projectile.modelPaused = false;
                if (!step(projectile, owner)) { remove(projectile); continue; }
                projectile.age++;
                if (projectile.kind.lifetime > 0) {
                    if (projectile.age >= projectile.kind.lifetime) {
                        finishModel(projectile);
                        remove(projectile);
                    }
                } else game.items().lifetimes().advance(projectile, projectile.age);
            }
        } finally { batch.clear(); }
    }

    private boolean step(TerraProjectile projectile, ServerPlayer owner) {
        var level = owner.level();
        Vec3 from = projectile.position(), motion = projectile.getDeltaMovement(), to = from.add(motion);
        if (!level.hasChunkAt(BlockPos.containing(from)) || motion.lengthSqr() < 1e-12
                || game.items().magic().suppressedAt(level, from)) return false;
        // Один native DDA останавливается на стене ИЛИ первом недоступном чанке.
        // hasChunkAt вызывается до чтения каждой клетки: путь не подгружает мир,
        // а попадания до границы чанка сохраняются даже у быстрого заряда.
        BlockHitResult wall = wallHit((ServerLevel) level, from, to);
        boolean unavailable = wall != null && !level.hasChunkAt(wall.getBlockPos());
        var shield = game.items().wands().shieldHit(owner, from, wall == null ? to : wall.getLocation());
        double wallDistance = wall == null ? Double.POSITIVE_INFINITY : from.distanceToSqr(wall.getLocation());
        boolean shieldFirst = shield != null && from.distanceToSqr(shield.getLocation()) <= wallDistance;
        Vec3 end = shieldFirst ? shield.getLocation() : wall == null ? to : wall.getLocation();
        var hits = projectile.kind == Kind.TRUE_BEAM ? beamHits(projectile, owner, from, end)
                : ProjectileUtil.getManyEntityHitResult(level, owner, from, end, new AABB(from, end).inflate(1),
                    entity -> target(owner, entity), 0, ClipContext.Block.COLLIDER, true, false);
        var damage = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(projectile.kind == Kind.TRUE_BEAM ? LbcDamage.TERRA_BEAM : DamageTypes.CACTUS), projectile, owner);
        for (var hit : hits) {
            // Дуга сохраняет отдельные попадания шагов и боковых полос; остальные заряды — одно на цель.
            hit.getEntity().hurtServer(level, damage, projectile.kind.damage);
            if (!projectiles.contains(projectile) || projectile.isRemoved()) return false;
        }
        Vec3 visualFrom = projectile.display == null ? from : projectile.display.getCurrentPos();
        projectile.setPos(end);
        boolean terminal = shieldFirst || wall != null;
        boolean lastStep = projectile.kind.lifetime > 0 && projectile.age + 1 >= projectile.kind.lifetime;
        Vec3 visualTo = terminal || lastStep ? end : visualEnd(projectile, owner);
        projectile.syncModel(visualTo);
        // Модель и частицы проходят один визуальный отрезок. Попадания выше используют
        // только настоящий from..end, а прогноз не наносит урона и не меняет дальность.
        renderTrail(projectile, visualFrom, visualTo);
        if (shieldFirst) {
            LbcOverload.sound(owner, shield.getEntity().position(), "minecraft:item.shield.block", 1, 1, 0);
            finishModel(projectile);
            return false;
        }
        if (wall != null) {
            if (!unavailable && projectile.kind != Kind.TRUE_BEAM)
                growVegetation(level, BlockPos.containing(end.subtract(motion.normalize().scale(.3))),projectile.kind);
            finishModel(projectile);
            return false;
        }
        return true;
    }

    private void renderTrail(TerraProjectile projectile, Vec3 from, Vec3 end) {
        var level = (ServerLevel) projectile.level();
        var motion = projectile.getDeltaMovement();
        Vec3 travelled = end.subtract(from);
        double distance = travelled.length();
        Vec3 forward = motion.normalize(), right = projectile.kind == Kind.TRUE_BEAM ? beamRight(projectile) : Vec3.ZERO;
        int samples = Math.max(1, Math.min(projectile.kind.samples, (int)Math.floor(distance / .3 + 1e-7) + 1));
        for (int i = 0; i < samples; i++) {
            Vec3 point = projectile.kind == Kind.TRUE_BEAM
                    ? from.add(forward.scale(Math.min(distance, i * .33)))
                    : from.add(travelled.scale(samples == 1 ? 0 : (double)i / (samples - 1)));
            if (projectile.kind == Kind.TRUE_BEAM) {
                // Исходные девять точек дуги; визуальные samples не запускают повторный поиск целей.
                for (int offset = 0; offset < BEAM_SIDES.length; offset++) {
                    Vec3 dot = point.add(right.scale(BEAM_SIDES[offset])).subtract(forward.scale(BEAM_BACK[offset]));
                    level.sendParticles(BEAM_DUST, false, false, dot.x, dot.y, dot.z, 1, 0, 0, 0, 0);
                }
            } else level.sendParticles(projectile.kind == Kind.ACADEMY || projectile.kind == Kind.TRUE_CHARGE || projectile.kind == Kind.RAZOR ? ACADEMY_DUST : DUST,
                    true, false, point.x, point.y, point.z, 1,
                    projectile.kind.lifetime > 0 ? .1 : .05,
                    projectile.kind.lifetime > 0 ? .1 : .05,
                    projectile.kind.lifetime > 0 ? .1 : .05, 0);
        }
    }

    static BlockHitResult wallHit(ServerLevel level, Vec3 from, Vec3 to) {
        return wallHit(level,from,to,state->!state.is(AIRS));
    }
    /** Та же DDA/граница чанка для лучей, которые ищут только выбранный тип блока. */
    static BlockHitResult wallHit(ServerLevel level, Vec3 from, Vec3 to,
            java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> blocks) {
        return BlockGetter.traverseBlocks(from, to, level,
                (world, pos) -> world.hasChunkAt(pos) && !blocks.test(world.getBlockState(pos))
                        ? null : Shapes.block().clip(from, to, pos), world -> null);
    }

    /** Прогноз только картинки: без поиска целей, урона, роста растений или загрузки чанков. */
    private Vec3 visualEnd(TerraProjectile projectile, ServerPlayer owner) {
        if (game.server().tickRateManager().isFrozen()) return projectile.position();
        Vec3 from = projectile.position(), to = from.add(projectile.getDeltaMovement());
        var wall = wallHit(owner.level(), from, to);
        Vec3 end = wall == null ? to : wall.getLocation();
        var shield = game.items().wands().shieldHit(owner, from, end);
        return shield == null ? end : shield.getLocation();
    }

    private void finishModel(TerraProjectile projectile) {
        var holder = projectile.holder;
        if (holder == null) return;
        var previous = holder.getAttachment();
        Vec3 impact = projectile.position();
        // Отцепляем от сущности до discard: EntityAttachment иначе удалит картинку вместе с ней.
        new ManualAttachment(holder, (ServerLevel) projectile.level(), () -> impact);
        if (previous != null) previous.destroy();
        projectile.holder = null;
        // Полный тик интерполяции + тик для кадра в конечной точке, без новых gameplay-тиков.
        finishingModels.addLast(new FinishingModel(holder, modelTicks + 2));
    }

    private boolean target(ServerPlayer owner, Entity entity) {
        return entity instanceof LivingEntity && entity.level() == owner.level()
                && entity.getType().builtInRegistryHolder().is(MOBS) && game.items().wands().target(owner, entity);
    }

    /** Один запрос целей на тик дуги. Девять линий её видимой ширины делят четыре шага урона. */
    private List<EntityHitResult> beamHits(TerraProjectile projectile, ServerPlayer owner, Vec3 from, Vec3 to) {
        var result = new ArrayList<EntityHitResult>();
        double distance = Math.min(from.distanceTo(to), 3 * BEAM_STEP);
        if (distance < 1e-8) return result;
        int steps = Math.max(1, Math.min(3, (int)Math.ceil(distance / BEAM_STEP - 1e-7)));
        Vec3 right = beamRight(projectile), forward = projectile.getDeltaMovement().normalize();
        to = from.add(forward.scale(distance));
        for (Entity candidate : owner.level().getEntities(owner, new AABB(from, to).inflate(1), entity -> target(owner, entity))) {
            var bounds = candidate.getBoundingBox().inflate(BEAM_HIT_PADDING);
            var hit = new EntityHitResult(candidate);
            // Legacy: три рабочих шага и три независимые полосы; широкая цель получает повторные удары.
            // Четвёртый шаг только перемещает дугу. Урон уменьшен пользователем до 1 за попадание.
            for (double side : BEAM_DAMAGE_SIDES) {
                Vec3 offset = right.scale(side).subtract(forward.scale(side == 0 ? 0 : .25));
                Vec3 start = from.add(offset), end = to.add(offset);
                var entry = bounds.contains(start) ? java.util.Optional.of(start) : bounds.clip(start, end);
                if (entry.isEmpty()) continue;
                double first = start.distanceTo(entry.get());
                double last = bounds.contains(end) ? distance : bounds.clip(end, start).map(start::distanceTo).orElse(first);
                int begin = Math.clamp((int)Math.floor(first / BEAM_STEP + 1e-7), 0, steps - 1);
                int finish = Math.clamp((int)Math.ceil(last / BEAM_STEP - 1e-7) - 1, begin, steps - 1);
                for (int step = begin; step <= finish; step++) result.add(hit);
            }
        }
        return result;
    }
    private static Vec3 beamRight(TerraProjectile projectile) {
        double yaw = Math.toRadians(projectile.getYRot());
        return new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
    }

    static void growVegetation(ServerLevel level, BlockPos origin,Kind kind) {
        // Нативный /place feature тоже требует соседние чанки. Ради заклинания их не загружаем.
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++)
            if (!level.hasChunk(x, z)) return;
        var registry = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        for (var patch : kind == Kind.ACADEMY || kind == Kind.TRUE_CHARGE || kind == Kind.RAZOR ? FLOWERS : GRASS)
            registry.get(ResourceKey.create(Registries.FEATURE, Identifier.parse(patch.id)))
                    .ifPresent(feature -> {
                        var predicate = patch.id.equals("minecraft:grass_jungle")
                                ? BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                    BlockPredicate.not(BlockPredicate.matchesBlocks(new Vec3i(0, -1, 0), List.of(Blocks.PODZOL))))
                                : BlockPredicate.ONLY_IN_AIR_PREDICATE;
                        // В 26.3 patches стали одиночными features. Возвращаем исходные
                        // попытки и распределение nextInt(n)-nextInt(n), не worldgen biome/heightmap.
                        new PlacedFeature(feature, List.of(CountPlacement.of(patch.tries),
                                OffsetPlacement.of(TrapezoidInt.of(-patch.xz, patch.xz, 0), TrapezoidInt.of(-patch.y, patch.y, 0)),
                                BlockPredicateFilter.forPredicate(predicate)))
                                .place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin);
                    });
    }

    private record Patch(String id,int tries,int xz,int y) { }
    private static final List<Patch> GRASS = List.of(new Patch("minecraft:grass",32,7,3),
            new Patch("minecraft:tall_grass",96,7,3),new Patch("minecraft:taiga_grass",32,7,3),
            new Patch("minecraft:grass_jungle",32,7,3));
    private static final List<Patch> FLOWERS = List.of(new Patch("minecraft:grass_jungle",32,7,3),
            new Patch("minecraft:flower_cherry",96,6,2),new Patch("minecraft:flower_flower_forest",96,6,2),
            new Patch("minecraft:flower_swamp",64,6,2),new Patch("minecraft:flower_meadow",96,6,2),
            new Patch("minecraft:flower_default",64,7,3),new Patch("minecraft:flower_plain",64,6,2));

    private void remove(TerraProjectile projectile) {
        projectiles.remove(projectile);
        projectile.destroyModel();
        projectile.discard();
    }
    public void stop() {
        var discarded = List.copyOf(projectiles);
        projectiles.clear();
        swordCharges.clear();
        for (var projectile : discarded) { projectile.destroyModel(); projectile.discard(); }
        for (var model : finishingModels) model.holder().destroy();
        finishingModels.clear();
    }
}
