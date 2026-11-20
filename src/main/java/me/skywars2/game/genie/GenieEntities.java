package me.skywars2.game.genie;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/** Native factory metadata shared by the constructor, its validator and the model index. */
public final class GenieEntities {
    private GenieEntities(){}
    private static final Set<EntityType<?>> PROJECTILES = Set.of(
            EntityTypes.ARROW, EntityTypes.BREEZE_WIND_CHARGE, EntityTypes.DRAGON_FIREBALL, EntityTypes.EGG,
            EntityTypes.ENDER_PEARL, EntityTypes.EXPERIENCE_BOTTLE, EntityTypes.FIREBALL, EntityTypes.FIREWORK_ROCKET,
            EntityTypes.FISHING_BOBBER, EntityTypes.LINGERING_POTION, EntityTypes.LLAMA_SPIT, EntityTypes.SHULKER_BULLET,
            EntityTypes.SMALL_FIREBALL, EntityTypes.SNOWBALL, EntityTypes.SPECTRAL_ARROW, EntityTypes.SPLASH_POTION,
            EntityTypes.TRIDENT, EntityTypes.WIND_CHARGE, EntityTypes.WITHER_SKULL);
    private static final Set<EntityType<?>> NON_MOBS = Set.of(EntityTypes.ARMOR_STAND, EntityTypes.MANNEQUIN, EntityTypes.PLAYER);

    private static boolean vanillaLiving(EntityType<?> type) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && id.getNamespace().equals("minecraft") && DefaultAttributes.hasSupplier(type);
    }
    public static String category(EntityType<?> type){
        return PROJECTILES.contains(type) ? "projectile" : vanillaLiving(type) ? "living" : "other";
    }
    public static boolean launchable(EntityType<?> type){
        return type.canSummon() && (PROJECTILES.contains(type) || vanillaLiving(type) && !NON_MOBS.contains(type));
    }
    /** Larger native bodies pay for their actual health, damage and simultaneous population. */
    public static double power(EntityType<?> type){
        if(!DefaultAttributes.hasSupplier(type))return 1;
        @SuppressWarnings("unchecked") var living=(EntityType<? extends LivingEntity>)type;
        var attributes=DefaultAttributes.getSupplier(living);
        double health=attributes.hasAttribute(Attributes.MAX_HEALTH)?attributes.getValue(Attributes.MAX_HEALTH):20;
        double attack=attributes.hasAttribute(Attributes.ATTACK_DAMAGE)?attributes.getValue(Attributes.ATTACK_DAMAGE):0;
        return Math.max(1,Math.max(health/20,attack/4));
    }
    public static int minimumCooldown(EntityType<?> type){return Math.max(40,(int)Math.ceil(20*power(type)));}
    public static double populationCost(EntityType<?> type,int lifetime,int cooldown){
        return 2*power(type)*Math.max(1,(lifetime==0?200:lifetime)/(double)cooldown);
    }
}
