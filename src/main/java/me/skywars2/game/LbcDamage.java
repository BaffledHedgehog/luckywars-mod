package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Общие правила урона; источник атаки по-прежнему определяет допустимые цели. */
public final class LbcDamage {
    /** Fuse=0 в исходнике обозначает взрыв без серверной защиты TNT, а не задержку. */
    public static final String UNPROTECTED_TNT = "skywars2.tnt.unprotected";
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType>
            YELLOW_BOLT = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:yellow_bolt")),
            YELLOW_SPIN = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:yellow_spin")),
            YELLOW_CONDUCTION = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:yellow_conduction"));
    public static final ResourceKey<DamageType> ELECTRIC =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:electric"));
    /** Нативная молния сохраняет электрическую защиту, но соблюдает обычный hurt cooldown. */
    public static final ResourceKey<DamageType> LIGHTNING =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:lightning"));
    public static final ResourceKey<DamageType> ELECTRO_BOLT =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:electro_bolt"));
    public static final ResourceKey<DamageType> ELECTRO_CONDUCTION =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:electro_conduction"));
    public static final ResourceKey<DamageType> ELECTRO_MEDITATION =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:electro_meditation"));
    public static final ResourceKey<DamageType> TERRA_BEAM =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:terra_beam"));
    public static final ResourceKey<DamageType> CONSUMING =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:consuming"));
    public static final ResourceKey<DamageType> DUPLICATING_HOLE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:duplicating_hole"));
    public static final ResourceKey<DamageType> STRANGENESS =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:strangeness"));
    private static final TagKey<DamageType> ELECTRIC_TYPES =
            TagKey.create(Registries.DAMAGE_TYPE, Identifier.parse("skywars2:electric"));
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final CompoundTag TECHNO = flag("techno"), TECHNO_MK2 = flag("technomk2");
    // Маркеры 20 source-сетов neko_set_*. Новый legacy-сет добавляется сюда; native IDs уже исключены.
    private static final CompoundTag[] CUSTOM_SETS = {
            flag("assasin"), flag("berserk"), flag("chaoschestplate"), flag("erosion"),
            flag("flesh"), flag("glass"), flag("govnoq"), flag("gownoleggings"), flag("jumpboost"),
            flag("mage"), flag("multiplying"), flag("nanobots"), flag("nyauwu"), flag("overload"),
            flag("paladin"), flag("phantom"), flag("randchanted"), flag("reactive"), flag("shulkshell"),
            TECHNO, TECHNO_MK2
    };
    private static final Map<EquipmentSlot, Set<Item>> METAL = Map.of(
            EquipmentSlot.HEAD, Set.of(Items.COPPER_HELMET, Items.IRON_HELMET, Items.CHAINMAIL_HELMET,
                    Items.GOLDEN_HELMET, Items.NETHERITE_HELMET),
            EquipmentSlot.CHEST, Set.of(Items.COPPER_CHESTPLATE, Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE,
                    Items.GOLDEN_CHESTPLATE, Items.NETHERITE_CHESTPLATE),
            EquipmentSlot.LEGS, Set.of(Items.COPPER_LEGGINGS, Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS,
                    Items.GOLDEN_LEGGINGS, Items.NETHERITE_LEGGINGS),
            EquipmentSlot.FEET, Set.of(Items.COPPER_BOOTS, Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS,
                    Items.GOLDEN_BOOTS, Items.NETHERITE_BOOTS));

    private LbcDamage() { }
    /** Один spatial query на отрезок; кратность старых шагов вычисляется по пересечению native AABB. */
    static void hurtSegment(net.minecraft.server.level.ServerLevel level, DamageSource damage,
            net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to,
            net.minecraft.world.phys.Vec3 padding, double spacing, float amount, int targetLimit,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> target, boolean includeOwner) {
        hurtSegment(level,damage,from,to,padding,spacing,amount,targetLimit,target,includeOwner,null);
    }
    /** Spell trials/status payloads observe accepted native hits without another entity query. */
    static void hurtSegment(net.minecraft.server.level.ServerLevel level, DamageSource damage,
            net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to,
            net.minecraft.world.phys.Vec3 padding, double spacing, float amount, int targetLimit,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> target, boolean includeOwner,
            java.util.function.Consumer<net.minecraft.world.entity.Entity> acceptedHit) {
        hurtSegment(level,damage,from,to,padding,spacing,amount,targetLimit,target,includeOwner,null,acceptedHit);
    }
    /** Source payloads may apply after a sampled hit even when native damage is rejected. */
    static void hurtSegment(net.minecraft.server.level.ServerLevel level, DamageSource damage,
            net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to,
            net.minecraft.world.phys.Vec3 padding, double spacing, float amount, int targetLimit,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> target, boolean includeOwner,
            java.util.function.Consumer<net.minecraft.world.entity.Entity> sampledHit,
            java.util.function.Consumer<net.minecraft.world.entity.Entity> acceptedHit) {
        hurtSegment(level,damage,from,to,padding,spacing,amount,targetLimit,target,includeOwner,isElectric(damage),sampledHit,acceptedHit);
    }
    private enum Sweep { LINE, ARROW, PHANTOM_ARROW }

    /** A virtual mob attack supplies the defensive enchantment callback normally run by native contact. */
    static void mobContactEnchantments(ServerLevel level, Entity target, DamageSource damage) {
        if (damage.getEntity() instanceof Mob attacker && attacker.isAlive())
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, damage, null);
    }

    static void hurtArc(ServerLevel level, DamageSource damage, Vec3 from, Vec3 to,
            double spacing, float amount, int targetLimit, Predicate<Entity> target, boolean includeOwner,
            boolean throughBlocks, ServerPlayer shooter, Consumer<Entity> acceptedHit) {
        hurtSegment(level, damage, from, to, Vec3.ZERO, spacing, amount, targetLimit, target, includeOwner,
            isElectric(damage), null, acceptedHit, throughBlocks ? Sweep.PHANTOM_ARROW : Sweep.ARROW, shooter);
    }

    /** Mechanical crossbows keep their terrain penetration and old centerline sample count. */
    static void hurtPiercingBeam(ServerLevel level, DamageSource damage, Vec3 from, Vec3 to,
            double spacing, float amount, int targetLimit, Predicate<Entity> target, ServerPlayer shooter) {
        hurtSegment(level, damage, from, to, Vec3.ZERO, spacing, amount, targetLimit, target, false,
                true, null, null, Sweep.PHANTOM_ARROW, shooter);
    }

    /** Exact line admission with source unit-cell multiplicity, independent of the native damage type. */
    static void hurtSegment(net.minecraft.server.level.ServerLevel level,DamageSource damage,
            net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to,
            net.minecraft.world.phys.Vec3 padding,double spacing,float amount,int targetLimit,
            java.util.function.Predicate<net.minecraft.world.entity.Entity> target,boolean includeOwner,boolean sourceCells,
            java.util.function.Consumer<net.minecraft.world.entity.Entity> sampledHit,
            java.util.function.Consumer<net.minecraft.world.entity.Entity> acceptedHit) {
        hurtSegment(level, damage, from, to, padding, spacing, amount, targetLimit, target, includeOwner,
            sourceCells, sampledHit, acceptedHit, Sweep.LINE, null);
    }

    private static void hurtSegment(ServerLevel level, DamageSource damage, Vec3 from, Vec3 to, Vec3 padding,
            double spacing, float amount, int targetLimit, Predicate<Entity> target, boolean includeOwner,
            boolean sourceCells, Consumer<Entity> sampledHit, Consumer<Entity> acceptedHit, Sweep sweep, ServerPlayer shooter) {
        double length=from.distanceTo(to);
        boolean shortCircuit=isElectricalContact(damage);
        Predicate<Entity> admitted=shortCircuit
                ?entity->entity instanceof LbcFpvDrone.Drone||target.test(entity):target;
        int maxSample=(int)Math.floor(length/spacing+1e-6);
        int[] perSample=new int[maxSample+1];
        var volume=new AABB(from,to).inflate(padding.x+1,padding.y+1,padding.z+1);
        for(var entity:level.getEntities(includeOwner ? null : damage.getEntity(),volume,admitted)) {
            var box=entity.getBoundingBox().inflate(padding.x,padding.y,padding.z);
            var entry=LbcProjectileSweep.entry(box,from,to);
            boolean graze=entry==null;
            if(graze) {
                if(sweep==Sweep.LINE) continue;
                entry=LbcProjectileSweep.arrowContact(level,damage.getDirectEntity(),box,from,to,sweep==Sweep.PHANTOM_ARROW);
                if(entry==null)continue;
                box=LbcProjectileSweep.arrowBounds(box);
            }
            var game=me.skywars2.SkyWars2.game();
            if(shortCircuit&&game!=null&&game.items().fpvDrone().tryShortCircuit(entity))continue;
            // Keep the authored sample count for line hits. A grazing hit uses only its swept overlap.
            boolean useSourceCells=sourceCells&&!graze;
            if(useSourceCells) {
                box=new AABB(box.minX-1,box.minY-1,box.minZ-1,box.maxX,box.maxY,box.maxZ);
                entry=LbcProjectileSweep.entry(box,from,to);
            }
            double first=from.distanceTo(entry);
            double last=box.contains(to) ? length : box.clip(to,from).map(from::distanceTo).orElse(first);
            int begin=Math.max(0,(int)Math.ceil(first/spacing-1e-6));
            int end=Math.min(maxSample,(int)Math.floor(last/spacing+1e-6));
            if(end<begin) begin=end=Math.min(maxSample,begin);
            boolean notified = false;
            for(int i=begin;i<=end;i++) if(perSample[i]<targetLimit && admitted.test(entity)) {
                if(useSourceCells){
                    var point=from.lerp(to,length>0?i*spacing/length:0);
                    // Native selector cells use strict overlap, including at sample endpoints.
                    if(point.x<=box.minX||point.x>=box.maxX||point.y<=box.minY||point.y>=box.maxY
                            ||point.z<=box.minZ||point.z>=box.maxZ)continue;
                }
                perSample[i]++;
                boolean accepted=entity.hurtServer(level,damage,amount);
                if(sampledHit!=null)sampledHit.accept(entity);
                if (accepted) {
                    // One contact cue per target even when the arc applies several damage samples.
                    if (!notified && shooter != null) {
                        LbcAcceptedHits.projectileFeedback(game, shooter);
                        notified = true;
                    }
                    if (acceptedHit != null) acceptedHit.accept(entity);
                }
            }
        }
    }
    /** (user) Зона снимает HP без hurt pipeline: броня, эффекты, absorption и velocity не участвуют. */
    public static boolean removeHealth(LivingEntity target, float amount, DamageSource source) {
        if (LbcGreed.active(target) || !(amount > 0) || !Float.isFinite(amount) || !target.isAlive()) return false;
        float before = target.getHealth();
        target.getCombatTracker().recordDamage(source, amount);
        target.setHealth(Math.max(0, before - amount));
        if (target.getHealth() >= before) return false;
        if (target.level() instanceof ServerLevel level) level.broadcastDamageEvent(target, source);
        if (target.getHealth() <= 0) target.die(source);
        return true;
    }
    private static CompoundTag flag(String name) {
        var data = new CompoundTag(); data.putInt(name, 1); return data;
    }
    public static boolean isElectric(DamageSource source) { return source.is(ELECTRIC_TYPES); }
    /** Contact identity is broader than metal/resistance balancing of electric damage. */
    public static boolean isElectricalContact(DamageSource source) {
        return isElectric(source)||source.is(DamageTypes.LIGHTNING_BOLT)||source.is(YELLOW_BOLT)
                ||source.is(YELLOW_SPIN)||source.is(YELLOW_CONDUCTION);
    }
    /** Нативный ближний удар игрока, включая sweep; авторство само по себе не означает melee. */
    public static boolean isMeleeAttack(DamageSource source, ServerPlayer owner) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == owner
                && source.getEntity() == owner;
    }
    /** Friendship never filters damage from a real vanilla lightning entity. */
    public static boolean isNativeLightning(DamageSource source) {
        return source.getDirectEntity() instanceof net.minecraft.world.entity.LightningBolt
                || source.is(DamageTypes.LIGHTNING_BOLT);
    }
    /** KFC spirits retain native fireball damage while ignoring friendship. */
    public static boolean allowsKfcFriendlyFire(DamageSource source) {
        return LbcBlaze.isKfcSpirit(source.getDirectEntity());
    }
    public static boolean allowsTrapFriendlyFire(DamageSource source) {
        var game=me.skywars2.SkyWars2.game();
        return LbcSpecialFireballs.managed(source.getDirectEntity()) || LbcNukeEvent.wild(source.getDirectEntity()) || LbcEdibleSpells.wild(source) || LbcIncomingArtifacts.wild(source.getDirectEntity()) || SauvojenMeditation.managed(source.getDirectEntity()) || LbcVexSword.wild(source.getDirectEntity()) || LbcTntDrops.wild(source.getDirectEntity()) || LbcIndirectKills.isWildCaptured(source) || source instanceof LbcBabylonEffects.OwnerDamageSource || source instanceof LbcVoidArcanum.WildDamage || source instanceof LbcIceWand.WildSource || source instanceof LbcCorruptedGlyphs.WildSource || source instanceof LbcCausticOmega.WildSource || source instanceof LbcGaycasterAcademy.TrialDamage || game!=null && (game.items().godHedgehog().cause(source.getDirectEntity())!=null && game.items().godHedgehog().cause(source.getDirectEntity()).friendlyFire()
                || LbcTransformator.managed(source.getDirectEntity()) || LbcStalin.managed(source.getDirectEntity()) || game.items().cageDrops().wild(source.getDirectEntity())
                || game.items().creeperDrops().wild(source.getDirectEntity())
                || game.items().zombieTntDrops().wild(source.getDirectEntity())
                || game.items().potionSpawnerDrops().wild(source.getDirectEntity())
                || game.items().droneDrops().wild(source.getDirectEntity())
                || game.items().antDrops().wild(source.getDirectEntity())
                || game.items().skyHazards().wild(source.getDirectEntity())
                || game.items().clouds().sourceAcidWild(source.getDirectEntity()));
    }
    public static boolean allowsElectricFriendlyFire(DamageSource source) {
        return isNativeLightning(source) || source.is(ELECTRO_CONDUCTION) || source.is(ELECTRO_MEDITATION);
    }
    /** Отдельные типы с исходным arrow-уроном сохраняют исключение arrow_only для атакующих пассивов. */
    public static boolean isArrowDamage(DamageSource source) {
        return source.is(DamageTypes.ARROW) || source.is(ELECTRO_BOLT) || source.is(TERRA_BEAM)
                || source.is(CONSUMING) || source.is(YELLOW_BOLT) || source.is(LbcKfcRewards.CULTUS_ARROW);
    }
    public static boolean allowDamage(LivingEntity victim, DamageSource source) {
        if(!LbcBedwarsRoleGuards.affectable(victim)||!LbcBedwarsHarm.damageAllowed(victim,source))return false;
        if(LbcGreed.active(victim)) return false;
        var game=me.skywars2.SkyWars2.game();
        if(game!=null && game.items().defensiveArcanums().blocksDamage(victim,source)) return false;
        if(game!=null && game.items().summons().alliedMinionHit(victim,source)) return false;
        if(game!=null && !game.items().indirectKills().allowEnvironmentDamage(victim,source)) return false;
        if(game!=null && source.getDirectEntity()!=null && !isNativeLightning(source) && !allowsKfcFriendlyFire(source) && !allowsTrapFriendlyFire(source)) {
            var reflected=game.items().electro().reflectedOwner(source.getDirectEntity());
            boolean admittedSelf=game.items().wands().physicalProjectileTarget(source.getDirectEntity(),victim)
                    ||reflected==victim&&(LbcMoiseiBoots.managed(source.getDirectEntity())
                    ||game.items().quiver().managedSecondary(source.getDirectEntity())
                    ||game.items().babylonEffects().managedSecondary(source.getDirectEntity()));
            if(reflected!=null && !admittedSelf && !game.items().wands().projectileTarget(reflected,victim)) return false;
        }
        if (!isElectric(source)||LbcTechnoArmor.selfShortCircuit(victim,source)) return true;
        // Флаги — серверная идентичность сета; Techno и MK2 можно смешивать в четырёх слотах.
        for (var slot : ARMOR) {
            var data = victim.getItemBySlot(slot).get(DataComponents.CUSTOM_DATA);
            if (data == null || !data.matchedBy(TECHNO) && !data.matchedBy(TECHNO_MK2)) return true;
        }
        return false;
    }
    /** После сложности, до брони/эффектов: Easy добавляет +1, поэтому HEAD ServerPlayer здесь неверен. */
    public static float protectedTntDamage(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                && source.getDirectEntity() instanceof net.minecraft.world.entity.item.PrimedTnt tnt
                && !tnt.entityTags().contains(UNPROTECTED_TNT)) {
            var game = me.skywars2.SkyWars2.game();
            if (game != null) amount *= (5 - Math.clamp(game.config().tntProtection, 0, 5)) / 5f;
        }
        return amount;
    }
    /** Электрическая надбавка к raw amount; вся ванильная защита рассчитывается Minecraft. */
    public static float incomingDamage(ServerPlayer victim, DamageSource source, float amount) {
        if (!isElectric(source)) return amount;
        int metal = 0;
        for (var slot : ARMOR) {
            var stack = victim.getItemBySlot(slot);
            if (!METAL.get(slot).contains(stack.getItem())) continue;
            var data = stack.get(DataComponents.CUSTOM_DATA);
            boolean customSet = false;
            if (data != null && !data.isEmpty()) for (var set : CUSTOM_SETS) if (data.matchedBy(set)) {
                customSet = true; break;
            }
            // Generic metadata вроде night_vision не превращает обычную броню в кастомный сет.
            if (!customSet) metal++;
        }
        return amount * (1 + .25f * metal);
    }
}
