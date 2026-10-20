package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Тринкеты с реакцией на выстрел/урон: количество читается в момент события, без опроса по тикам. */
public final class LbcTrinkets {
    public static final Item INTERCAL = register("skywars2:intercal", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.9", "lbc:trinket_9", false);
    public static final Item GNEISS_STONE = register("skywars2:gneiss_stone", Items.STONE, "lbctrinket.15", "minecraft:stone", true);
    public static final Item BLAZE = register("skywars2:blaze_trinket", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.0", "lbc:trinket_0", false);
    public static final Item GHAST = register("skywars2:ghast_trinket", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.1", "lbc:trinket_1", false);
    public static final Item SNOW_GEM = register("skywars2:snow_gem", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.17", "lbc:snow_pearl", false);
    public static final Item ICE_GEM = register("skywars2:ice_gem", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.19", "lbc:snow_gem", false);
    public static final Item MANA_GEM = register("skywars2:mana_gem", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.20", "lbc:mana_gem", false);
    public static final Item BLOOD_ANAL_BOOM = register("skywars2:blood_anal_boom", Items.REPEATING_COMMAND_BLOCK, "lbctrinket.4", "lbc:blood_anal_boom", false);
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final CompoundTag INTERCAL_DATA = identity("intercal");
    private static final CompoundTag GNEISS_DATA = identity("gneiss_stone");
    private static final CompoundTag BLAZE_DATA = identity("blaze");
    private static final CompoundTag GHAST_DATA = identity("ghast");
    private static final CompoundTag SNOW_DATA = identity("snow_pearl");
    private static final CompoundTag ICE_DATA = identity("ice_gem");
    private static final CompoundTag MANA_GEM_DATA = identity("mana_gem");
    private static final CompoundTag BLOOD_ANAL_BOOM_DATA = identity("blood_anal_boom");
    private static final CompoundTag NETHER_PLUG_DATA = identity("nether_plug");
    private static final CompoundTag GHAST_ROCKET = readRocket();
    private static final String PROJECTILE_TAG = "skywars2.trinket.projectile";
    private final GameController game;
    private final Set<Projectile> projectiles = new LinkedHashSet<>();

    public LbcTrinkets(GameController game) { this.game = game; }
    public static void register() { }

    private static CompoundTag readRocket() {
        try (var input = LbcTrinkets.class.getResourceAsStream("/skywars2/ghast_rocket.snbt")) {
            if (input == null) throw new IllegalStateException("Missing Ghast rocket preset");
            return TagParser.parseCompoundFully(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception failure) { throw new IllegalStateException("Invalid source Ghast rocket preset", failure); }
    }

    private static CompoundTag identity(String id) {
        var tag = new CompoundTag(); tag.putString("trinket", id); return tag;
    }

    private static Item register(String id, Item proxy, String name, String model, boolean placeable) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
        var properties = new Item.Properties().setId(key).stacksTo(99).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL, Identifier.parse(model));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, proxy, true) {
            @Override public InteractionResult useOn(UseOnContext context) {
                // Гнейс в исходнике — обычный камень; размещение и расход оставляем нативному BlockItem.
                return placeable ? Items.STONE.useOn(context) : super.useOn(context);
            }
        });
    }

    private static int count(ServerPlayer player, Item item, CompoundTag identity) {
        // Как /clear ... 0: инвентарь, личная сетка крафта и предмет на курсоре, без изменения стаков.
        return player.getInventory().clearOrCountMatchingItems(stack -> {
            var data = stack.get(DataComponents.CUSTOM_DATA);
            return stack.is(item) || data != null && data.matchedBy(identity);
        }, true, 0, player.inventoryMenu.getCraftSlots());
    }

    public void shot(ServerPlayer player) {
        if (!game.canUseGameplayItems(player) || player.level() != game.maps().level() || game.config().lbcTrinketLimit == 0) return;
        int copies = count(player, BLAZE, BLAZE_DATA);
        // Сохраняем отдельные source-ветки для одной копии и гарантированного количества.
        if (copies > 0 && (copies >= 11 || player.getRandom().nextFloat() < (copies == 1 ? .15f : (175 + 75 * copies) * .001f))) {
            var fireball = new SmallFireball(EntityTypes.SMALL_FIREBALL, player.level());
            // (user) Исправляем ownerless source: стрелок защищён при запуске, урон сохраняет авторство.
            fireball.setOwner(player);
            fireball.setPos(player.getEyePosition());
            fireball.setDeltaMovement(aim(player, 1000, .0016));
            fireball.entityTags().add("can_deleted");
            spawn(fireball);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.MASTER, 1, 1);
        }
        copies = count(player, GHAST, GHAST_DATA);
        if (copies > 0 && (copies >= 16 || player.getRandom().nextFloat() < (copies == 1 ? .25f : (100 + 50 * copies) * .001f))) {
            var rocket = new FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET, player.level());
            // Нативная загрузка сохраняет source LifeTime=100, ShotAtAngle и точные пять burst-взрывов без accessor к private полям.
            rocket.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), GHAST_ROCKET.copy()));
            rocket.setOwner(player);
            rocket.setPos(player.getEyePosition());
            rocket.setDeltaMovement(aim(player, 10000, .0002));
            spawn(rocket);
        }
    }

    private static Vec3 aim(ServerPlayer player, int precision, double scale) {
        var direction = player.getLookAngle();
        return new Vec3(Mth.floor(direction.x * precision) * scale, Mth.floor(direction.y * precision) * scale,
                Mth.floor(direction.z * precision) * scale);
    }

    void spawn(Projectile projectile) {
        projectile.entityTags().add(PROJECTILE_TAG);
        game.maps().level().addFreshEntity(projectile);
    }

    /** Общий on_summon: 50% для первой копии Nether Plug, затем +10% за каждую. */
    public void summoned(ServerPlayer player) {
        if(!game.canUseGameplayItems(player) || player.level()!=game.maps().level() || game.config().lbcTrinketLimit==0) return;
        int copies=count(player,Items.AIR,NETHER_PLUG_DATA);
        if(copies>0 && player.getRandom().nextFloat()<.5f+.1f*(copies-1)) game.items().blaze().summonFriend(player);
    }
    public void castWand(ServerPlayer player) {
        if (!game.canUseGameplayItems(player) || player.level() != game.maps().level()
                || game.config().lbcTrinketLimit == 0) return;
        int copies = count(player, BLOOD_ANAL_BOOM, BLOOD_ANAL_BOOM_DATA);
        if (copies > 0 && (copies >= 16 || player.getRandom().nextFloat() < (copies == 1 ? .25f : .2f + .05f * copies))) {
            game.items().castWandDynamite(player);
        }
        copies = count(player, MANA_GEM, MANA_GEM_DATA);
        if (copies > 0) game.items().mana().add(player, 60 + 15 * (copies - 1));
    }

    boolean loaded(Entity entity) {
        if (!(entity instanceof Projectile projectile)
                || !entity.entityTags().contains(PROJECTILE_TAG) && !game.items().wands().isFireProjectile(entity)) return false;
        entity.entityTags().add(PROJECTILE_TAG);
        ServerPlayer owner = projectile.getOwner() instanceof ServerPlayer player ? player : null;
        boolean ownerCanUse = owner != null && game.canUseGameplayItems(owner);
        boolean currentEpoch = game.items().currentEpoch(entity);
        if (owner != null && game.isObserver(owner)) entity.discard();
        else if (entity.level() == game.maps().level()
                && (game.playing() && (owner == null || ownerCanUse)
                    || game.items().debugItemUseWindow() && ownerCanUse || currentEpoch)) projectiles.add(projectile);
        else entity.discard();
        return true;
    }

    void unloaded(Entity entity) { projectiles.remove(entity); }
    public int projectileCount() { return projectiles.size(); }

    void stop() {
        // ENTITY_UNLOAD меняет коллекцию синхронно; очищаем её до discard и сохраняем единую эпоху LbcItems.
        var pending = List.copyOf(projectiles); projectiles.clear();
        for (var projectile : pending) projectile.discard();
    }

    public void damaged(LivingEntity victim, DamageSource source) {
        if ((!game.playing() && !game.items().debugItemUseWindow()) || game.config().lbcTrinketLimit == 0) return;
        if (victim instanceof ServerPlayer player && game.allowDamage(player) && player.isAlive()) {
            int ice = count(player, ICE_GEM, ICE_DATA);
            if (ice > 0 && player.getRandom().nextFloat() < (90 + 10 * ice) * .001f) iceTargets(player);
            int copies = count(player, GNEISS_STONE, GNEISS_DATA);
            if (copies > 0 && player.getRandom().nextFloat() < (80 + 20 * copies) * .001f) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1, 1);
                player.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                        player.getX(), player.getY() + 1, player.getZ(), 30, .3, .6, .3, 0);
            }
        }
        // Source arrow_only также покрывает отдельный тип Electro bolt; остальные снаряды не исключаем.
        if (LbcDamage.isArrowDamage(source) || !(source.getEntity() instanceof ServerPlayer attacker)
                || !game.allowDamage(attacker) && !game.canUseGameplayItems(attacker)) return;
        int snow = count(attacker, SNOW_GEM, SNOW_DATA);
        if (snow > 0 && attacker.getRandom().nextFloat() < (80 + 20 * snow) * .001f) game.items().frost().hitTargets(attacker);
        int copies = count(attacker, INTERCAL, INTERCAL_DATA);
        if (copies == 0 || attacker.getRandom().nextFloat() >= (100 + 50 * copies) * .001f) return;
        boolean upgraded = game.config().lbcSkills && game.players().get(attacker.getUUID()).lbcSkill == 15;
        for (var target : attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(10), entity ->
                entity.isAlive() && entity.getType().builtInRegistryHolder().is(MOBS)
                        && !entity.entityTags().contains("spectator") && !attacker.isAlliedTo(entity)
                        && (!(entity instanceof ServerPlayer player) || game.allowDamage(player)))) {
            double distance = attacker.distanceToSqr(target);
            if (distance < .000001 || distance > 100) continue;
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0), attacker);
            // Как две исходные effect give: короткий IV скрывает I, затем нативная цепочка возвращает I.
            if (upgraded) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 3), attacker);
        }
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.WITHER_AMBIENT, SoundSource.MASTER, 1, 1);
    }

    private void iceTargets(ServerPlayer owner) {
        // Ice Gem ищет всех подходящих мобов в окружении владельца; Snow Pearl использует общую область меча.
        for (var target : owner.level().getEntitiesOfClass(LivingEntity.class, new AABB(owner.position(), owner.position()).inflate(12), entity ->
                entity.isAlive() && !entity.entityTags().contains("spectator") && !owner.isAlliedTo(entity)
                        && entity.getType().builtInRegistryHolder().is(MOBS)
                        && (!(entity instanceof ServerPlayer player) || game.allowDamage(player)))) {
            double distance = owner.distanceToSqr(target);
            if (distance >= .000001 && distance <= 144) game.items().frost().add(target, owner);
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1, 1);
        owner.level().sendParticles(ParticleTypes.SNOWFLAKE, owner.getX(), owner.getY()+.5, owner.getZ(), 30, .5, .5, .5, 1);
    }
}
