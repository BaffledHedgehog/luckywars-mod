package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Real native chicken conversion, turret acquisition and fireball collision/damage. */
public final class KfcWildChecks {
    private KfcWildChecks() { }
    private static void require(boolean ok,String message) {
        if(!ok) throw new IllegalStateException("KFC wild: "+message);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerPlayer owner,ally;
        final GameChecks.WandThirdPlayerState ownerState,allyState;
        final net.minecraft.world.scores.PlayerTeam team,oldOwnerTeam,oldAllyTeam;
        final Set<Entity> initial=new HashSet<>();
        final ArrayList<Entity> created=new ArrayList<>();
        final Vec3 origin;
        Fixture(GameController game) {
            this.game=game;
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            ally=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && owner!=null && ally!=null && game.allowDamage(owner) && game.allowDamage(ally),"two active participants required");
            ownerState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            allyState=new GameChecks.WandThirdPlayerState(ally,game.items().mana());
            oldOwnerTeam=owner.getTeam(); oldAllyTeam=ally.getTeam();
            var board=game.server().getScoreboard(); team=board.addPlayerTeam("codex_kfc_wild"); team.setAllowFriendlyFire(false);
            board.addPlayerToTeam(owner.getScoreboardName(),team); board.addPlayerToTeam(ally.getScoreboardName(),team);
            origin=new Vec3(Math.floor(owner.getX()/16)*16+8.5,280,Math.floor(owner.getZ()/16)*16+8.5);
            owner.level().getAllEntities().forEach(initial::add);
            game.items().blaze().stop(); game.items().shooters().forget(owner);
            for(var player:new ServerPlayer[]{owner,ally}) {
                player.closeContainer(); player.getInventory().clearContent(); player.removeAllEffects();
                player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setShiftKeyDown(false);
                player.getAbilities().invulnerable=false; player.entityTags().remove("time_stopped");
                game.items().mana().noMagic(player,0); ready(player);
            }
            move(owner,20,0); move(ally,16,0);
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) owner.level().getChunk((int)Math.floor(origin.x/16)+x,(int)Math.floor(origin.z/16)+z);
        }
        Cow cow(double x,double z,boolean allied) {
            var cow=new Cow(EntityTypes.COW,owner.level()); cow.setNoAi(true); cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); cow.setHealth(1000);
            cow.setPos(origin.add(x,0,z)); require(owner.level().addFreshEntity(cow),"cow spawned"); created.add(cow);
            if(allied) game.server().getScoreboard().addPlayerToTeam(cow.getScoreboardName(),team);
            return cow;
        }
        void move(ServerPlayer player,double x,double z) {
            player.teleportTo(owner.level(),origin.x+x,origin.y,origin.z+z,Set.of(),0,0,true); player.hasChangedDimension();
        }
        SmallFireball pulse(LivingEntity target,boolean chicken) {
            Vec3 point=origin.add(0,1,0);
            if(chicken) {
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcShooters.KFC_GUN));
                require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"native gun use accepted");
                var birds=owner.level().getEntitiesOfClass(Chicken.class,owner.getBoundingBox().inflate(4),e->game.items().shooters().owner(e)==owner);
                require(birds.size()==1,"one owned native chicken");
                var bird=birds.getFirst(); bird.setPos(origin);
                require(bird.hurtServer(owner.level(),bird.damageSources().generic(),10) && bird.isRemoved(),"native damage converts chicken to spirit");
            } else game.items().blaze().chickenTurret(owner,point);
            for(int tick=0;tick<20;tick++) game.items().blaze().tick();
            var balls=owner.level().getEntitiesOfClass(SmallFireball.class,new AABB(point,point).inflate(1),LbcBlaze::isKfcSpirit);
            require(balls.size()==1,"one spirit shot on the first native pulse");
            var ball=balls.getFirst(); created.add(ball);
            require(ball.getOwner()==owner && ball.position().distanceTo(point)<.001,"spirit keeps owner and origin");
            Vec3 expected=target.getEyePosition().subtract(point).normalize().scale(1.6);
            require(ball.getDeltaMovement().distanceTo(expected)<.001,"nearest eligible target is "+target.getType());
            ball.discard(); game.items().blaze().forget(owner); return ball;
        }
        SmallFireball hit(LivingEntity victim,boolean wild) { return hit(victim,wild,false); }
        SmallFireball hit(LivingEntity victim,boolean wild,boolean reflected) {
            var ball=new SmallFireball(EntityTypes.SMALL_FIREBALL,owner.level());
            if(wild) ball.entityTags().add(LbcBlaze.KFC_SPIRIT_PROJECTILE);
            game.items().wands().launchFire(owner,ball,victim.position().add(0,.5,-2),new Vec3(0,0,3)); created.add(ball);
            if(reflected) game.items().electro().assignOwner(ball,owner);
            owner.level().tickNonPassenger(ball); return ball;
        }
        @Override public void close() {
            game.items().shooters().forget(owner); game.items().blaze().stop();
            if(game.isObserver(ally)) game.spectators().remove(ally);
            owner.level().getAllEntities().forEach(e->{if(!initial.contains(e)) created.add(e);});
            for(var entity:created) { game.server().getScoreboard().removePlayerFromTeam(entity.getScoreboardName()); entity.discard(); }
            var board=game.server().getScoreboard();
            board.removePlayerFromTeam(owner.getScoreboardName()); board.removePlayerFromTeam(ally.getScoreboardName()); board.removePlayerTeam(team);
            if(oldOwnerTeam!=null) board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);
            if(oldAllyTeam!=null) board.addPlayerToTeam(ally.getScoreboardName(),oldAllyTeam);
            ownerState.restore(game); allyState.restore(game);
        }
    }
    private static void ready(LivingEntity target) {
        target.removeAllEffects(); target.setHealth(target.getMaxHealth()); target.setAbsorptionAmount(0);
        target.setInvulnerableTime(0); target.damageCooldownTime=0; target.setRemainingFireTicks(0);
    }
    public static void run(GameController game) {
        try(var f=new Fixture(game)) {
            var friendly=f.cow(0,2,true); var enemy=f.cow(0,7,false);
            var npc=f.cow(0,.5,true); npc.entityTags().add("magic_academy");
            f.pulse(friendly,true);
            f.move(f.owner,1,0); f.pulse(f.owner,false);
            f.move(f.owner,20,0); f.move(f.ally,0,1); f.pulse(f.ally,false);
            game.spectators().enter(f.ally); f.pulse(friendly,false);
            game.spectators().remove(f.ally); f.ally.setGameMode(GameType.SURVIVAL); f.move(f.ally,10,0);
            // Isolate each physical flight from the other fixture hitboxes.
            friendly.setPos(f.origin.add(0,0,10)); enemy.setPos(f.origin.add(0,0,20)); npc.setPos(f.origin.add(0,0,30));
            f.move(f.owner,20,0);
            require(!f.owner.canHarmPlayer(f.ally),"native scoreboard friendly fire disabled");
            for(var victim:new LivingEntity[]{friendly,f.ally,f.owner}) {
                ready(victim); float before=victim.getHealth(); var ball=f.hit(victim,true);
                require(ball.isRemoved() && victim.getHealth()<before && victim.isOnFire(),"actual native spirit hits and ignites "+victim.getType());
                require(victim.getLastDamageSource()!=null && victim.getLastDamageSource().getEntity()==f.owner,"native fireball retains true owner");
                if(victim!=f.owner) require(victim.getKillCredit()==f.owner,"native owner kill credit");
                float hp=victim.getHealth(); f.hit(victim,true);
                require(victim.getHealth()==hp,"immediate repeated hit respects native hurt cooldown");
                victim.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0); victim.setLastHurtByMob(null);
                victim.setInvulnerableTime(0); victim.damageCooldownTime=0;
                require(victim.hurtServer(f.owner.level(),victim.damageSources().onFire(),1) && victim.getHealth()<hp,"owned ignition retains wild damage");
                if(victim!=f.owner) require(victim.getKillCredit()==f.owner,"later burn keeps original owner");
                victim.setRemainingFireTicks(0);
            }
            ready(f.ally); float before=f.ally.getHealth(); var ordinary=f.hit(f.ally,false);
            require(f.ally.getHealth()==before && !f.ally.isOnFire() && !ordinary.isRemoved(),"ordinary fireball still passes through allied player"); ordinary.discard();
            ready(f.ally); before=f.ally.getHealth(); var reflected=f.hit(f.ally,true,true);
            require(reflected.isRemoved() && f.ally.getHealth()<before && f.ally.getLastDamageSource().getEntity()==f.owner,
                    "owner-transferred spirit remains wild and retains its current owner");
            f.ally.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
            // LivingEntity.tick detects changed equipment and applies its transient attribute modifiers.
            f.ally.doTick(); ready(f.ally);
            double armor=f.ally.getAttributeValue(Attributes.ARMOR);
            require(armor>0,"native equipment tick applies chestplate armor: armor="+armor);
            before=f.ally.getHealth(); var armored=f.hit(f.ally,true);
            float loss=before-f.ally.getHealth();
            require(armored.isRemoved() && loss>0 && loss<5,"wild spirit retains native armor reduction"
                    +" [hp="+before+"->"+f.ally.getHealth()+", loss="+loss+", raw=5.0, armor="+armor
                    +", source="+f.ally.getLastDamageSource()+", projectileRemoved="+armored.isRemoved()+"]");
            ready(npc); f.hit(npc,true);
            require(npc.getHealth()==npc.getMaxHealth() && !npc.isOnFire(),"service NPC survives actual spirit flight");
            ready(f.ally); game.spectators().enter(f.ally); f.hit(f.ally,true);
            require(f.ally.getHealth()==f.ally.getMaxHealth() && !f.ally.isOnFire(),"observer survives actual spirit flight");
        }
        SkyWars2.LOGGER.info("[check] PASS kfc-wild");
    }
}
