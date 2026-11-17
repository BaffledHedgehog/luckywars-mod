package me.skywars2.game.genie;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.projectile.Projectile;

/** Native factory metadata shared by the constructor, its validator and the model index. */
public final class GenieEntities {
    private GenieEntities(){}
    private static final Map<EntityType<?>,Class<?>> CLASSES=classes();
    private static Map<EntityType<?>,Class<?>> classes(){
        var result=new IdentityHashMap<EntityType<?>,Class<?>>();
        for(var field:EntityTypes.class.getFields())if(Modifier.isStatic(field.getModifiers())
                &&field.getGenericType() instanceof ParameterizedType generic&&generic.getRawType()==EntityType.class
                &&generic.getActualTypeArguments()[0] instanceof Class<?> body){
            try{result.put((EntityType<?>)field.get(null),body);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        }
        return Collections.unmodifiableMap(result);
    }
    public static String category(EntityType<?> type){
        var body=CLASSES.get(type);
        return body!=null&&Projectile.class.isAssignableFrom(body)?"projectile":
                body!=null&&LivingEntity.class.isAssignableFrom(body)?"living":"other";
    }
    public static boolean launchable(EntityType<?> type){
        var body=CLASSES.get(type);
        return type.canSummon()&&body!=null&&(Projectile.class.isAssignableFrom(body)||Mob.class.isAssignableFrom(body));
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
