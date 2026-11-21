package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Shot-direction error only; gun recoil and muzzle positions remain independent. */
public final class LbcGunAccuracy {
    static final double HIP_DEGREES=5, MAX_DEGREES=20, FULL_PENALTY_SPEED=.3;
    private LbcGunAccuracy() { }
    /** Native Projectile#getMovementToShoot adds independent bounded triangular XYZ errors. */
    public static double vanillaDegrees(double uncertainty){return additiveDegrees(.0172275*uncertainty,1);}
    /** Conservative angular envelope of an additive XYZ cube, without sampling any RNG. */
    public static double additiveDegrees(double axisError,double speed){
        if(axisError<=0)return 0;
        return Math.toDegrees(Math.asin(Math.clamp(Math.sqrt(3)*axisError/Math.max(1e-12,speed),0,1)));
    }

    /** Real native item classes also cover authored Polymer subclasses and TMT bases. */
    public static boolean nativeWeapon(net.minecraft.world.item.ItemStack stack){
        var item=stack.getItem();return item instanceof net.minecraft.world.item.ProjectileWeaponItem
                ||item instanceof net.minecraft.world.item.SnowballItem||item instanceof net.minecraft.world.item.EggItem
                ||item instanceof net.minecraft.world.item.EnderpearlItem||item instanceof net.minecraft.world.item.WindChargeItem
                ||item instanceof net.minecraft.world.item.ThrowablePotionItem||item instanceof net.minecraft.world.item.TridentItem
                ||item instanceof net.minecraft.world.item.ExperienceBottleItem||item instanceof net.minecraft.world.item.FishingRodItem
                ||stack.is(LbcPursuitItems.BOUNCING_POOP)||stack.is(LbcImbaNativeItems.FIREWORK)
                ||LbcSteampunkRocket.matches(stack)||ItemData.intValue(stack,"metasola",0)==1
                ||ItemData.intValue(stack,"special",0)==1&&(LbcHedgehogItems.spike(stack)!=null||LbcHedgehogSpikes.legacyThrow(stack)>0);
    }
    public static double nativeDegrees(ServerPlayer player,net.minecraft.world.item.ItemStack stack){
        if(stack.isEmpty()||!nativeWeapon(stack))return -1;
        // These source abilities copy the player's rotation, not the discarded snowball's motion.
        if(LbcKapusta.matches(stack)||ItemData.intValue(stack,"govno_raskid",0)==1||LbcBridgeBuilder.recognized(stack)
                ||LbcSteampunkRocket.matches(stack)||stack.is(LbcImbaNativeItems.FIREWORK))return 0;
        var kind=LbcImbaCrossbows.kind(stack);
        if(kind!=null){
            if(kind==LbcImbaCrossbows.Kind.SNIPER)return 0;
            if(kind==LbcImbaCrossbows.Kind.SUPER)return Math.toDegrees(Math.acos(Math.cos(Math.toRadians(30))*Math.cos(Math.toRadians(30))));
            if(kind==LbcImbaCrossbows.Kind.GATLING)return Math.toDegrees(Math.acos(Math.cos(Math.toRadians(30))*Math.cos(Math.toRadians(5))));
            if(kind==LbcImbaCrossbows.Kind.SHOTGUN)return additiveDegrees(.5,5);
            if(kind==LbcImbaCrossbows.Kind.FAST)return additiveDegrees(.4,7);
        }
        double spread=vanillaDegrees(1);
        if(stack.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem){
            var multishot=player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MULTISHOT);
            if(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(multishot,stack)>0)spread+=10;
        }
        return spread;
    }

    public static double movementDegrees(ServerPlayer player) {
        double speed=Math.max(speed(player.getKnownMovement(),player.onGround()),
                speed(player.getDeltaMovement(),player.onGround()));
        return MAX_DEGREES*Math.min(1,speed/FULL_PENALTY_SPEED);
    }
    private static double speed(Vec3 motion,boolean grounded) {
        // Ignore standing gravity/contact jitter, but include jumps, falls and external pushes.
        double vertical=grounded?0:motion.y;
        double speed=Math.sqrt(motion.horizontalDistanceSqr()+vertical*vertical);
        return speed>.01?speed:0;
    }
    /** Half-angle of the next rifle shot; reading presentation state must never sample the RNG. */
    public static double rifleDegrees(GameController game,ServerPlayer player) {
        double degrees=game.items().rifleZoom().scoped(player)?0:HIP_DEGREES;
        return Math.min(MAX_DEGREES,degrees+movementDegrees(player));
    }
    public static double ak47Degrees(ServerPlayer player) {
        return movementDegrees(player);
    }
    /** Source narrowing occurs once per accepted shot, before either angular offset is sampled. */
    public static int minigunNextShotSpread(int currentSpread) {
        return currentSpread>=1100?currentSpread-100:currentSpread;
    }
    /** Minigun offsets form a yaw/pitch square, not the rifle's solid-angle cone. */
    public static float minigunDegrees(int spread) {
        return spread*.001f;
    }
    public static double minigunNextShotDegrees(GameController game,ServerPlayer player) {
        return minigunDegrees(minigunNextShotSpread(game.items().laserMinigun().spread(player)));
    }
    public static Vec3 rifle(GameController game,ServerPlayer player) {
        return sample(player.getLookAngle(),rifleDegrees(game,player),player.getRandom());
    }
    public static Vec3 ak47(ServerPlayer player) {
        return sample(player.getLookAngle(),ak47Degrees(player),player.getRandom());
    }
    public static Vec3 sample(Vec3 forward,double degrees,RandomSource random) {
        if(degrees==0)return forward;
        // Uniform solid-angle cone, with a stable basis even when looking straight up/down.
        var axis=forward.normalize();
        var side=axis.cross(Math.abs(axis.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        var up=side.cross(axis);
        double cosine=1-random.nextDouble()*(1-Math.cos(Math.toRadians(degrees)));
        double sine=Math.sqrt(Math.max(0,1-cosine*cosine)),angle=random.nextDouble()*Math.PI*2;
        return axis.scale(cosine).add(side.scale(sine*Math.cos(angle))).add(up.scale(sine*Math.sin(angle)));
    }
}
