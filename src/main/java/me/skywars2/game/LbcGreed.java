package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/** Absolute active-Greed guard; acquisition and the Greed activation loop are separate ports. */
public final class LbcGreed {
    public static final String REFLECTED="skywars2.greed.reflected";
    private LbcGreed(){}

    public static boolean active(Entity entity){return entity!=null&&entity.entityTags().contains("sin_greed_active");}

    /** Manual spell actors move in family services, outside vanilla projectile collision. */
    public static void tick(GameController game) {
        for(var level:game.server().getAllLevels()) {
            if(level.players().stream().noneMatch(LbcGreed::active)) continue;
            for(var entity:level.getAllEntities()) if(entity instanceof Projectile shot && ProjectileLifetimes.protectedWandProjectile(shot)) {
                var hit=firstHit(level,shot.position(),shot.position().add(shot.getDeltaMovement()),shot.getBbWidth()/2);
                if(hit!=null && (shot.getOwner()!=hit.getEntity() || !hit.getEntity().getBoundingBox().inflate(.2).contains(shot.position()))) reflect(shot,hit.getEntity());
            }
        }
    }
    static net.minecraft.world.phys.EntityHitResult firstHit(net.minecraft.server.level.ServerLevel level,net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to,double padding) {
        net.minecraft.world.phys.EntityHitResult nearest=null; double distance=Double.POSITIVE_INFINITY;
        for(var player:level.players()) if(active(player) && player.isAlive()) {
            var box=player.getBoundingBox().inflate(padding);
            var contact=box.clip(from,to);
            if(contact.isPresent() && from.distanceToSqr(contact.get())<distance) { distance=from.distanceToSqr(contact.get());nearest=new net.minecraft.world.phys.EntityHitResult(player,contact.get()); }
        }
        return nearest;
    }

    /** Called before native impact, so arrows, potions, rockets and their hit side effects cannot activate. */
    public static boolean reflect(Projectile projectile,Entity target){
        if(!active(target)||projectile.isRemoved()||projectile.level().isClientSide())return false;
        // Preserve speed: native REVERSE deliberately halves velocity and adds random rotation.
        projectile.setDeltaMovement(projectile.getDeltaMovement().reverse());
        projectile.setOwner(target);
        var game=SkyWars2.game();
        if(game!=null&&target instanceof ServerPlayer player)game.items().electro().assignOwner(projectile,player);
        LbcShooters.orient(projectile);
        projectile.needsSync=true;
        projectile.entityTags().add(REFLECTED);
        return true;
    }
}
