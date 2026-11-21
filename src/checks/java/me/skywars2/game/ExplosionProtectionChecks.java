package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Actual native explosion/config/recipient-shield boundaries, independent from visual carriers. */
public final class ExplosionProtectionChecks {
    private ExplosionProtectionChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Explosion protection: "+why);}
    private static void near(float a,float b,String why){require(Math.abs(a-b)<.03f,why+" actual="+a+" expected="+b);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture base;final ServerPlayer target;
        final int setting;final Vec3 point;final List<Entity> actors=new ArrayList<>();
        Fixture(GameController game){this.game=game;base=new WoolBucketChecks.Fixture(game,false);target=base.other;setting=game.config().tntProtection;
            base.prepareParticipants();point=base.home.add(0,15,0);base.move(base.owner,point.add(0,70,0));base.move(target,point.add(1,0,0));
            for(var p:List.of(base.owner,target)){p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.ARMOR).setBaseValue(0);p.setHealth(1000);}
        }
        void reset(boolean effects){if(effects)target.removeAllEffects();target.setHealth(1000);target.setAbsorptionAmount(0);target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setDeltaMovement(Vec3.ZERO);}
        float nativeLoss(Entity source,ExplosionItemProtection.Shield shield){
            reset(false);
            if(shield==null)base.level.explode(source,point.x,point.y,point.z,1,Level.ExplosionInteraction.MOB);
            else ExplosionItemProtection.with(source,shield,()->base.level.explode(source,point.x,point.y,point.z,1,Level.ExplosionInteraction.MOB));
            return 1000-target.getHealth();
        }
        PrimedTnt tnt(int fuse){var tnt=new PrimedTnt(base.level,point.x,point.y,point.z,base.owner);tnt.setNoGravity(true);tnt.setDeltaMovement(Vec3.ZERO);tnt.setFuse(fuse);actors.add(tnt);return tnt;}
        float countdown(int fuse){reset(true);var tnt=tnt(fuse);tnt.tick();require(tnt.isRemoved(),"actual native terminal fuse executes once");return 1000-target.getHealth();}
        void check(){
            game.config().tntProtection=0;float timed=countdown(1),immediate=countdown(0);require(timed>0&&immediate>0,"both real native TNT baselines damage player");
            game.config().tntProtection=5;near(countdown(1),0,"ordinary countdown remains configurable after reaching fuse zero");near(countdown(0),immediate,"authored fuse-zero bypasses config");
            var creeper=EntityTypes.CREEPER.create(base.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            var fireball=EntityTypes.FIREBALL.create(base.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(creeper!=null&&fireball!=null,"real native creeper and fireball factories");
            creeper.setPos(point);fireball.setPos(point);actors.add(creeper);actors.add(fireball);
            for(var source:List.of(creeper,fireball)){target.removeAllEffects();game.config().tntProtection=0;float expected=nativeLoss(source,null);require(expected>0,"native non-TNT baseline");
                game.config().tntProtection=5;near(nativeLoss(source,null),expected,"config never reduces creeper or fireball explosion");}
            for(int protection:new int[]{0,5}){
                game.config().tntProtection=protection;reset(true);game.items().exoperso().edibleBurst(base.owner,point);float exoperso=1000-target.getHealth();
                require(exoperso>0,"actual Exoperso first wave bypasses maximum configured protection");game.items().exasperato().stop();game.items().exoperso().stop();
                reset(true);require(game.items().nukeEvent().pointStatusBurst(base.owner,base.level,point),"actual nuclear status admitted");
                require(target.getHealth()<1000,"actual point nuclear first wave bypasses configured protection");game.items().nukeEvent().stop();game.items().nuclearVisuals().stop();
            }
            game.config().tntProtection=5;target.removeAllEffects();float normal=nativeLoss(creeper,null);var shield=ExplosionItemProtection.owner(target.getUUID(),4);
            near(nativeLoss(creeper,shield),normal*.2f,"item ResistanceIV ratio belongs to this explosion, independently of config");
            require(!target.hasEffect(MobEffects.RESISTANCE),"recipient acquires no temporary resistance effect");
            target.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,200,1));near(nativeLoss(creeper,shield),normal*.2f,"item IV supersedes existing II without multiplying it twice");
            require(target.getEffect(MobEffects.RESISTANCE).getAmplifier()==1&&target.getEffect(MobEffects.RESISTANCE).getDuration()==200,"existing native effect remains exact");
            target.removeAllEffects();var delayed=tnt(1);ExplosionItemProtection.stamp(delayed,ExplosionItemProtection.owner(target.getUUID(),5));
            game.config().tntProtection=0;reset(true);delayed.tick();near(1000-target.getHealth(),0,"delayed native TNT retains its activation-time item shield");
            reset(true);require(target.hurtServer(base.level,target.damageSources().generic(),5),"ordinary subsequent damage admitted");near(1000-target.getHealth(),5,"no one-second immunity leaks into unrelated damage");
            nested();nuclearWild();
        }
        void nuclearWild(){
            var board=game.server().getScoreboard();var oldOwner=base.owner.getTeam();var oldTarget=target.getTeam();
            require(board.getPlayerTeam("nuclear_wild_check")==null,"exclusive nuclear alliance fixture");
            var team=board.addPlayerTeam("nuclear_wild_check");team.setAllowFriendlyFire(false);
            try{
                board.addPlayerToTeam(base.owner.getScoreboardName(),team);board.addPlayerToTeam(target.getScoreboardName(),team);
                base.move(base.owner,point.add(-1,0,0));base.move(target,point.add(1,0,0));
                for(var p:List.of(base.owner,target)){p.removeAllEffects();p.setHealth(1000);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;}
                game.config().tntProtection=5;game.items().exasperato().exopersoBurst(base.owner,point,null);
                for(var p:List.of(base.owner,target)){
                    require(p.getHealth()<1000,"real nuclear first wave hits caster and same-team ally: "+p.getScoreboardName());
                    var damage=p.getLastDamageSource();var cause=game.items().indirectKills().babylonCause(damage);
                    require(damage!=null&&damage.getEntity()==null,"no physical caster ownership or thorns retaliation");
                    require(damage.getDirectEntity() instanceof PrimedTnt tnt&&tnt.getOwner()==null,"ownerless native blast carrier");
                    require(cause!=null&&cause.friendlyFire()&&cause.player().equals(base.owner.getUUID()),"WILD statistical caster survives independently");
                }
            }finally{
                game.items().exasperato().stop();game.items().nuclearVisuals().stop();board.removePlayerTeam(team);
                if(oldOwner!=null)board.addPlayerToTeam(base.owner.getScoreboardName(),oldOwner);
                if(oldTarget!=null)board.addPlayerToTeam(target.getScoreboardName(),oldTarget);
            }
        }
        void nested(){
            reset(true);var parent=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,base.level);parent.setPos(point);actors.add(parent);
            var child=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,base.level);child.setPos(point);actors.add(child);
            var witness=new Zombie(EntityTypes.ZOMBIE,base.level){boolean fired;
                @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,DamageSource damage,float amount){
                    if(!fired&&damage.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)){fired=true;level.explode(child,point.x,point.y,point.z,2,Level.ExplosionInteraction.MOB);}
                    return super.hurtServer(level,damage,amount);
                }};
            witness.setPos(point.add(0,0,1));witness.setNoAi(true);witness.setNoGravity(true);witness.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);witness.setHealth(1000);
            require(base.level.addFreshEntity(witness),"native nested-explosion witness");actors.add(witness);
            ExplosionItemProtection.with(parent,ExplosionItemProtection.owner(target.getUUID(),5),()->base.level.explode(parent,point.x,point.y,point.z,1,Level.ExplosionInteraction.MOB));
            require(target.getHealth()<1000,"nested unrelated native explosion cannot borrow the outer item's shield");
        }
        public void close(){game.config().tntProtection=setting;game.items().exasperato().stop();game.items().exoperso().stop();game.items().nukeEvent().stop();game.items().nuclearVisuals().stop();actors.forEach(Entity::discard);base.close();}
    }
    public static void run(GameController game){try(var fixture=new Fixture(game)){fixture.check();}
        SkyWars2.LOGGER.info("[check] PASS explosion-protection (actual native timedTNT/fuse0/creeper/fireball/config boundaries; real nuclear and Exoperso first waves; per-item IV/V, unchanged native effects, delayed TNT and nested/unrelated damage isolation)");}
}
