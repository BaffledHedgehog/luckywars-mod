package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Reviewed native transient fields, not recursive cloning of server/network objects. */
final class MatchEntityRuntime {
    private final Map<Field,Object> values=new LinkedHashMap<>();
    private final Map<Field,Object> swing=new LinkedHashMap<>();
    private final long worldTime;
    private final boolean using;
    private final net.minecraft.world.InteractionHand usedHand;

    MatchEntityRuntime(Entity entity) {
        worldTime=entity.level().getGameTime();using=entity instanceof LivingEntity living&&living.isUsingItem();
        usedHand=entity instanceof LivingEntity living?living.getUsedItemHand():net.minecraft.world.InteractionHand.MAIN_HAND;
        capture(Entity.class,entity,"tickCount","remainingFireTicks","invulnerableTime","portalCooldown","fallDistance",
                "onGround","onGroundNoBlocks","noPhysics","firstTick","xRotO","yRotO","moveDist","flyDist","nextStep","hasVisualFire","boardingCooldown");
        if(entity instanceof LivingEntity) {
            capture(LivingEntity.class,entity,"attackStrengthTicker","itemSwapTicker","hurtTime","hurtDuration","deathTime","damageCooldownTime",
                "dead","noActionTime","lastHurt","jumping","xxa","yya","zza","speed","noJumpDelay","fallFlyTicks","useItemRemaining",
                "autoSpinAttackTicks","autoSpinAttackDmg","lastHurtByPlayerMemoryTime","lastHurtByMobTimestamp","lastHurtMobTimestamp",
                "yBodyRot","yBodyRotO","yHeadRot","yHeadRotO","lastDamageStamp","lastKineticHitFeedbackTime");
        }
        if(entity instanceof Player)capture(Player.class,entity,"sleepCounter","jumpTriggerTime");
        if(entity instanceof ServerPlayer)capture(ServerPlayer.class,entity,"levitationStartPos","levitationStartTime","startingToFallPosition","enteredLavaOnVehiclePosition","timeEntitySatOnShoulder","lastKnownClientMovement");
        if(entity instanceof LivingEntity) {
            Object state=read(entity,LivingEntity.class,"swingState");
            for(var field:state.getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            field.setAccessible(true);try{swing.put(field,field.get(state));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        }
        if(entity instanceof net.minecraft.world.entity.projectile.Projectile)capture(net.minecraft.world.entity.projectile.Projectile.class,entity,"leftOwnerChecked");
    }

    private void capture(Class<?> type,Object source,String...names) {
        for(var name:names) {
            var field=field(type,name);
            try{values.put(field,field.get(source));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
    }
    private static Field field(Class<?> type,String name) {
        try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new IllegalStateException("Review native entity checkpoint field "+type.getName()+"."+name,e);}
    }
    private static Object read(Object target,Class<?> type,String name) {
        try{return field(type,name).get(target);}catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    void restore(Entity entity) {
        try {
            for(var entry:values.entrySet()) {
                Object value=entry.getValue();String name=entry.getKey().getName();
                if((name.equals("lastDamageStamp")||name.equals("lastKineticHitFeedbackTime")||name.equals("timeEntitySatOnShoulder"))
                        &&value instanceof Long stamp&&stamp>=0)value=Math.addExact(stamp,entity.level().getGameTime()-worldTime);
                entry.getKey().set(entity,value);
            }
            if(entity instanceof LivingEntity living) {
                Object state=read(entity,LivingEntity.class,"swingState");
                for(var entry:swing.entrySet())entry.getKey().set(state,entry.getValue());
                field(LivingEntity.class,"useItem").set(entity,using?living.getItemInHand(usedHand):ItemStack.EMPTY);
            }
        }catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
}
