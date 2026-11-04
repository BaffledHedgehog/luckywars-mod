package me.skywars2.game;

import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.BedwarsRoleGuardMixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;

/** Active ordinary BedWars allegiance admission; existing typed WILD policy remains authoritative. */
public final class LbcBedwarsHarm {
    private LbcBedwarsHarm(){}
    public static boolean active(){var game=SkyWars2.game();return game!=null&&game.bedwars()!=null&&game.bedwars().active()&&game.bedwars().arena().active();}
    private static LbcSpellSource.Source policy(Entity actor){var game=SkyWars2.game();return game==null?null:game.items().spellSources().source(actor);}
    private static boolean wild(LbcSpellSource.Source source){return source!=null&&(source.cause()==null||source.cause().friendlyFire());}
    private static Entity nativeOwner(Entity actor){return actor instanceof Projectile shot?shot.getOwner():actor instanceof AreaEffectCloud cloud?cloud.getOwner():actor instanceof PrimedTnt tnt?tnt.getOwner():actor;}
    private static UUID ownerId(Entity actor){
        if(actor instanceof Projectile shot){var ref=((BedwarsRoleGuardMixin.ProjectileOwner)(Object)shot).skywars2$owner();if(ref!=null)return ref.getUUID();}
        if(actor instanceof AreaEffectCloud cloud){var ref=((BedwarsRoleGuardMixin.CloudOwner)(Object)cloud).skywars2$owner();if(ref!=null)return ref.getUUID();}
        if(actor instanceof PrimedTnt tnt){var ref=((BedwarsRoleGuardMixin.TntOwner)(Object)tnt).skywars2$owner();if(ref!=null)return ref.getUUID();}
        var player=LbcIndirectKills.playerSource(actor);return player==null?null:player.getUUID();
    }
    private static int team(Entity actor){
        if(actor==null)return 0;var game=SkyWars2.game();
        if(actor instanceof ServerPlayer player)return game.bedwars().team(player);
        var nativeTeam=actor.getTeam();if(nativeTeam!=null)try{int number=Integer.parseInt(nativeTeam.getName());if(number>=1&&number<=16)return number;}catch(NumberFormatException ignored){}
        var player=LbcIndirectKills.playerSource(actor);return player==null?0:game.bedwars().team(player);
    }
    private static boolean allied(Entity victim,Entity actor,UUID causalOwner){
        if(actor!=null&&actor.getUUID().equals(victim.getUUID()))return false;
        if(actor==null&&victim.getUUID().equals(causalOwner))return false;
        int target=team(victim);if(target==0)return actor!=null&&actor.isAlliedTo(victim);
        int author=causalOwner==null?0:SkyWars2.game().bedwars().assignedTeam(causalOwner);
        if(author==0&&actor!=null&&actor.isAlliedTo(victim))return true;
        if(author==0)author=team(actor);
        return author!=0&&author==target;
    }
    public static boolean damageAllowed(Entity victim,DamageSource source){
        if(!active()||victim==null||source==null)return true;
        if(LbcDamage.allowsElectricFriendlyFire(source)||LbcDamage.allowsKfcFriendlyFire(source)||LbcDamage.allowsTrapFriendlyFire(source))return true;
        var game=SkyWars2.game();var accepted=game.items().indirectKills().policyCause(victim,source);
        if(accepted!=null&&accepted.friendlyFire())return true;
        Entity actor=source.getEntity();if(actor==null)actor=nativeOwner(source.getDirectEntity());
        var typed=policy(source.getDirectEntity());
        if(typed==null&&accepted==null){var current=game.items().spellSources().current();var author=accepted!=null?accepted.player():ownerId(actor);
            if(current!=null&&(author==null||author.equals(current.author())))typed=current;}
        if(accepted==null&&wild(typed))return true;
        UUID owner=accepted!=null?accepted.player():typed!=null?typed.author():ownerId(source.getDirectEntity());
        return !allied(victim,actor,owner);
    }
    public static boolean projectileAllowed(Entity projectile,Entity target){
        if(!active()||target==null)return true;
        var owner=nativeOwner(projectile);var source=target.damageSources().thrown(projectile,owner);
        return damageAllowed(target,source);
    }
    public static boolean sourceAllowed(LbcSpellSource.Source source,Entity target){
        if(!active()||source==null||target==null||wild(source))return true;
        var level=target.level();var actor=level instanceof net.minecraft.server.level.ServerLevel server?server.getEntity(source.emitter()):null;
        return !allied(target,actor,source.author());
    }
    public static boolean harmful(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int amplifier,LivingEntity target){
        long level=(long)amplifier+1;if(level==0)return false;
        if(type.is(MobEffects.INSTANT_HEALTH)||type.is(MobEffects.INSTANT_DAMAGE)){
            boolean harm=type.is(MobEffects.INSTANT_DAMAGE);return (harm==target.isInvertedHealAndHarm())!=(level>0);
        }
        if(type.is(MobEffects.REGENERATION))return level<0;
        if(type.is(MobEffects.POISON)||type.is(MobEffects.WITHER)||type.is(MobEffects.SLOWNESS)||type.is(MobEffects.WEAKNESS)||type.is(MobEffects.HUNGER)||type.is(MobEffects.UNLUCK))return level>0;
        return type.value().getCategory()!=MobEffectCategory.BENEFICIAL||level<0;
    }
    public static boolean effectAllowed(LivingEntity target,MobEffectInstance effect,Entity source){
        return effectAllowed(target,effect.getEffect(),effect.getAmplifier(),source,null);
    }
    public static boolean effectAllowed(LivingEntity target,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int amplifier,Entity source,Entity explicitOwner){
        if(!active()||!harmful(type,amplifier,target))return true;
        var actor=explicitOwner!=null?explicitOwner:nativeOwner(source);
        return damageAllowed(target,target.damageSources().thrown(source,actor));
    }
}
