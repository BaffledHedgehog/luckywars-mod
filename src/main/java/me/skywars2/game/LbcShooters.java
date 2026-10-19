package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Очереди стрелкового оружия; физика, попадания и автор урона остаются у ванильных стрел. */
public final class LbcShooters {
    private static final String PROJECTILE_TAG = "skywars2.razorpine";
    private static final CompoundTag PINE_FLAG = new CompoundTag();
    private static final TagKey<Item> AMMO = TagKey.create(Registries.ITEM, Identifier.parse("minecraft:razor_ammo"));
    private static final TagKey<Item> BEST_AMMO = TagKey.create(Registries.ITEM, Identifier.parse("minecraft:razor_ammo_best"));
    public static final Item PINE = registerPine();
    static { PINE_FLAG.putInt("razorpine", 1); }

    private final GameController game;
    private final Map<UUID, Burst> bursts = new HashMap<>();
    private final Set<Arrow> arrows = new LinkedHashSet<>();
    private static final class Burst {
        final ServerPlayer owner;
        final boolean best;
        int volleys = 6, delay = 2;
        Burst(ServerPlayer owner, boolean best) { this.owner = owner; this.best = best; }
    }

    LbcShooters(GameController game) { this.game = game; }
    static void register() { }
    private static Item registerPine() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:razorpine"));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(
                new Item.Properties().setId(key).durability(25).overrideDescription("razorpine")
                        .component(DataComponents.ITEM_MODEL, Identifier.parse("lbc:razorpine")),
                Items.CARROT_ON_A_STICK, true) {
            @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                var controller = me.skywars2.SkyWars2.game();
                return controller != null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
    }
    public static boolean isPine(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(PINE) || data != null && data.matchedBy(PINE_FLAG);
    }
    private boolean active(ServerPlayer player) {
        return game.canUseGameplayItems(player) && player.isAlive() && !player.isRemoved()
                && player.level() == game.maps().level() && game.stage() != GameController.Stage.ENDING;
    }
    private boolean canFire(ServerPlayer player) {
        return active(player) && !game.items().frost().isFrozen(player)
                && !player.entityTags().contains("time_stopped") && !game.items().wands().timeStopped()
                && !game.items().magic().suppressed(player);
    }
    private int hands(ServerPlayer player) {
        return (isPine(player.getMainHandItem()) ? 1 : 0)
                + (game.config().offhandWands && isPine(player.getOffhandItem()) ? 1 : 0);
    }
    public boolean use(ServerPlayer player, InteractionHand hand) {
        if (!isPine(player.getItemInHand(hand)) || hand == InteractionHand.OFF_HAND && !game.config().offhandWands
                || bursts.containsKey(player.getUUID()) || !canFire(player)) return false;
        // Как source /clear: сначала еловые боеприпасы, затем обычные; ровно одна единица на всю очередь.
        boolean best = consume(player, BEST_AMMO);
        if (!best && !consume(player, AMMO)) return false;
        bursts.put(player.getUUID(), new Burst(player, best));
        if (best) {
            var advancement = game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/shooter/razorpine"));
            if (advancement != null) for (var criterion : advancement.value().criteria().keySet())
                player.getAdvancements().award(advancement, criterion);
        }
        return true;
    }
    private static boolean consume(ServerPlayer player, TagKey<Item> tag) {
        return player.getInventory().clearOrCountMatchingItems(stack -> stack.is(tag), false, 1,
                player.inventoryMenu.getCraftSlots()) == 1;
    }
    public void tick() {
        if (bursts.isEmpty()) return;
        boolean stopped = game.items().wands().timeStopped();
        for (var burst : List.copyOf(bursts.values())) {
            var owner = burst.owner;
            if (!active(owner)) { forget(owner); continue; }
            if (stopped || owner.entityTags().contains("time_stopped")) continue;
            if (--burst.delay > 0) continue;
            burst.delay = 2;
            if (canFire(owner)) {
                // (user) Два хвоемёта удваивают очередь без второго расхода боеприпаса.
                for (int hand = 0, held = hands(owner); hand < held; hand++) volley(owner, burst.best);
            }
            if (--burst.volleys == 0) bursts.remove(owner.getUUID());
        }
    }
    private void volley(ServerPlayer owner, boolean best) {
        var direction = owner.getLookAngle();
        var origin = owner.getEyePosition().subtract(direction.scale(.2));
        int speed = best ? 5000 : 2000;
        for (int shot = 0; shot < (best ? 2 : 3); shot++) {
            var arrow = new Arrow(EntityTypes.ARROW, owner.level());
            arrow.setOwner(owner);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setBaseDamage(best ? 1.5 : .5);
            arrow.setSoundEvent(SoundEvents.AZALEA_LEAVES_BREAK);
            arrow.setPos(origin);
            arrow.setDeltaMovement(new Vec3(component(owner, direction.x, speed),
                    component(owner, direction.y, speed), component(owner, direction.z, speed)));
            // Arrow использует углы скорости, а не углы взгляда игрока; задаём их до spawn-пакета.
            orient(arrow);
            arrow.entityTags().add(PROJECTILE_TAG);
            owner.level().addFreshEntity(arrow);
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.MASTER, 1, 2);
    }
    static void orient(Projectile projectile) {
        var motion=projectile.getDeltaMovement();
        projectile.setYRot((float)(net.minecraft.util.Mth.atan2(motion.x,motion.z)*180/Math.PI));
        projectile.setXRot((float)(net.minecraft.util.Mth.atan2(motion.y,motion.horizontalDistance())*180/Math.PI));
        projectile.setOldRot();
    }
    private static double component(ServerPlayer owner, double direction, int speed) {
        return (Math.floor(direction * speed) + owner.getRandom().nextInt(601) - 300) * .001;
    }
    boolean loaded(Entity entity) {
        if (!(entity instanceof Arrow arrow) || !entity.entityTags().contains(PROJECTILE_TAG)) return false;
        if (entity.level() == game.maps().level() && (game.playing() || game.items().currentEpoch(entity)
                || arrow.getOwner() instanceof ServerPlayer owner && active(owner))) arrows.add(arrow);
        else arrow.discard();
        return true;
    }
    void unloaded(Entity entity) { arrows.remove(entity); }
    public boolean canHit(Projectile projectile, Entity target) {
        return !projectile.entityTags().contains(PROJECTILE_TAG)
                || projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && game.items().wands().target(owner, target);
    }
    /** Source prein удаляет хвою в земле; перехватываем событие вместо чтения NBT каждого снаряда в тике. */
    public void hit(Projectile projectile, HitResult hit) {
        if (hit.getType() == HitResult.Type.BLOCK && projectile.entityTags().contains(PROJECTILE_TAG)) projectile.discard();
    }
    public int burstCount() { return bursts.size(); }
    public int arrowCount() { return arrows.size(); }
    public void forget(ServerPlayer player) { bursts.remove(player.getUUID()); }
    void stop() {
        bursts.clear();
        var removed = List.copyOf(arrows); arrows.clear();
        for (var arrow : removed) arrow.discard();
    }
}
