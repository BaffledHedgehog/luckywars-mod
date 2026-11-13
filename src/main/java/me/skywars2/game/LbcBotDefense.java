package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Real shield/finite cover and retreat intents. Never absorbs damage or modifies an attack. */
final class LbcBotDefense {
    final ServerPlayer player;final LbcChefBrain.Driver driver;final LbcBotSurvival survival;
    private record ThornGear(Item item,ItemEnchantments enchantments){}
    private final Map<UUID,List<ThornGear>> thorny=new LinkedHashMap<>();
    int shieldUntil,nextThreat,nextCounter,countered;Vec3 threat;LivingEntity retaliation;
    net.minecraft.world.entity.projectile.Projectile incoming;
    BlockPos cover;
    java.util.UUID struck;int struckAt;
    Vec3 retreatPoint,retreatDanger;int retreatUntil,nextCrowd;
    Vec3 escapePoint;int nextEscape;
    LivingEntity rangedAttacker;Vec3 coverDirection;
    int rangedHits;
    LivingEntity meleeAttacker;int meleeAt,meleeHits;
    final LbcBotShelter shelter;
    List<net.minecraft.world.entity.Mob> crowd=List.of();
    String diagnostics(){return "defenseShot="+(incoming==null?null:incoming.getUUID())+" cover="+cover+" "+shelter.diagnostics()+" countered="+countered+" meleeHits="+meleeHits+" rangedHits="+rangedHits+" rangedAttacker="+(rangedAttacker==null?null:rangedAttacker.getMainHandItem())+" nextThreat="+nextThreat+" bodyTick="+player.tickCount;}
    void meleeDamage(LivingEntity attacker){if(meleeAttacker!=attacker)meleeHits=0;meleeAttacker=attacker;meleeAt=player.tickCount;meleeHits++;}
    LbcBotDefense(ServerPlayer player,LbcChefBrain.Driver driver,LbcBotSurvival survival){this.player=player;this.driver=driver;this.survival=survival;shelter=new LbcBotShelter(player,driver,survival);}
    void observedHit(LivingEntity target,float healthBefore){
        struck=target.getUUID();struckAt=player.tickCount;
        var source=player.getLastDamageSource();
        if(player.getHealth()+player.getAbsorptionAmount()<healthBefore&&source!=null&&source.is(DamageTypes.THORNS)){
            rememberThorns(target);
        }
    }
    private static List<ThornGear> thornGear(LivingEntity target){
        return Arrays.stream(EquipmentSlot.values()).map(slot->{var item=target.getItemBySlot(slot);
            return new ThornGear(item.getItem(),item.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY));}).toList();
    }
    void rememberThorns(LivingEntity target){
        thorny.remove(target.getUUID());thorny.put(target.getUUID(),thornGear(target));
        if(thorny.size()>64)thorny.remove(thorny.keySet().iterator().next());
    }
    boolean thorns(LivingEntity target){
        if(target==null)return false;var gear=thorny.get(target.getUUID());if(gear==null)return false;
        if(gear.equals(thornGear(target)))return true;
        thorny.remove(target.getUUID());return false;
    }
    boolean interruptUse(LivingEntity enemy){
        if(enemy!=null&&(thorns(enemy)||player.distanceToSqr(enemy)<9
                ||enemy instanceof Creeper creeper&&creeper.getSwellDir()>0))return true;
        return !player.level().getEntitiesOfClass(PrimedTnt.class,player.getBoundingBox().inflate(9),e->e.getFuse()<=45).isEmpty()
                ||LbcBotActions.incoming(player)!=null;
    }
    boolean holdingShield(){
        if(!player.getUseItem().has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS))return false;
        if(player.tickCount>=shieldUntil){player.stopUsingItem();return false;}
        if(threat!=null)driver.aimAction(threat);return true;
    }
    boolean retreat(Vec3 danger,double desired){
        return retreat(danger,desired,false);
    }
    private boolean retreat(Vec3 danger,double desired,boolean watchThreat){
        var away=player.position().subtract(danger).multiply(1,0,1);
        if(away.horizontalDistanceSqr()>=desired*desired)return false;
        if(away.lengthSqr()<.001)away=new Vec3(1,0,0);
        Vec3 point=retreatPoint;
        if(point==null||player.tickCount>=retreatUntil||retreatDanger.distanceToSqr(danger)>4
                ||point.subtract(player.position()).horizontalDistanceSqr()<.36||!survival.safeStep(point)){
            point=null;double best=-Double.MAX_VALUE;var direction=away.normalize();
            for(double length:new double[]{2.5,1.25,.65})for(int turn=0;turn<8;turn++){
                var vector=direction.yRot((float)(turn*Math.PI/4));var candidate=player.position().add(vector.scale(length));
                if(!survival.safeStep(candidate))continue;
                double score=vector.dot(direction)*length+survival.brain.retreatMargin(candidate)*.85+length*.15;
                if(score>best){best=score;point=candidate;}
            }
            if(point==null)return false;
            retreatPoint=point;retreatDanger=danger;retreatUntil=player.tickCount+5;
        }
        // A short melee cooldown step is ordinary backpedalling, not a repeated
        // 180-degree turn away from the opponent. Long urgent escapes still face forward.
        player.setSprinting(!watchThreat);
        driver.steer(point,false,watchThreat?danger.add(0,player.getEyeHeight(),0):point.add(0,1.5,0));return true;
    }
    /** Turn around an obstruction on existing connected ground before trying to stand and build. */
    private boolean escapeMelee(Vec3 danger){
        if(player.position().distanceToSqr(danger)>=100){escapePoint=null;return false;}
        if(retreat(danger,8))return true;
        if(escapePoint!=null&&(!survival.brain.supported(escapePoint)||escapePoint.distanceToSqr(player.position())<.36
                ||escapePoint.distanceToSqr(danger)<player.position().distanceToSqr(danger)+1))escapePoint=null;
        if(escapePoint==null&&player.tickCount>=nextEscape){
            nextEscape=player.tickCount+20;
            record Candidate(Vec3 at,double score){}
            var candidates=new ArrayList<Candidate>();
            double current=player.position().distanceToSqr(danger);
            for(int direction=0;direction<16;direction++)for(double radius:new double[]{4,7,10}){
                double angle=direction*Math.PI/8;var at=survival.brain.surfaceAt(player.position().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius));
                if(at==null||at.distanceToSqr(danger)<current+4)continue;
                candidates.add(new Candidate(at,Math.min(144,at.distanceToSqr(danger))+survival.brain.retreatMargin(at)*8-radius));
            }
            candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());
            for(int i=0;i<Math.min(6,candidates.size());i++){
                var at=candidates.get(i).at();var end=survival.brain.safeWalk(at)?at:survival.brain.reachableGround(at);
                if(end!=null&&end.distanceToSqr(danger)>current+4){escapePoint=end;survival.brain.reset();break;}
            }
        }
        if(escapePoint==null)return false;
        survival.destination.accept(escapePoint);
        if(!survival.brain.closeGround(escapePoint))survival.brain.tick();return true;
    }
    private boolean groundMelee(LivingEntity enemy){
        return enemy instanceof net.minecraft.world.entity.Mob mob
                &&mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation
                &&!(mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.WallClimberNavigation)
                &&!(mob instanceof Creeper)&&mob.getType()!=net.minecraft.world.entity.EntityTypes.WITCH
                &&!(enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem)
                &&!(enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem);
    }
    boolean holdsHighGround(LivingEntity enemy){
        return groundMelee(enemy)&&player.onGround()&&player.getY()>enemy.getBoundingBox().maxY+.1
                &&player.position().subtract(enemy.position()).horizontalDistanceSqr()<144;
    }
    private boolean retreatThreats(LivingEntity enemy){
        if(holdsHighGround(enemy))return false;
        // Start before the first heavy hit; the ordinary navigator uses owned blocks
        // and visible faces, and abandons an impossible tactical climb after160ticks.
        if(groundMelee(enemy)&&player.distanceToSqr(enemy)<100&&player.hasLineOfSight(enemy)
                &&survival.takeHighGround())return true;
        if(player.tickCount>=nextCrowd){
            crowd=player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,player.getBoundingBox().inflate(8),
                    mob->mob.isAlive()&&mob.getTarget()==player&&!player.isAlliedTo(mob));nextCrowd=player.tickCount+5;
        }
        boolean overwhelmed=crowd.size()>=3||crowd.size()>=2&&player.getHealth()<player.getMaxHealth()*.7;
        boolean losingMelee=enemy!=null&&enemy==meleeAttacker&&player.tickCount-meleeAt<100&&player.getHealth()<player.getMaxHealth()*.4;
        if((losingMelee||thorns(enemy))&&enemy instanceof net.minecraft.world.entity.Mob&&enemy.onGround()
                &&player.distanceToSqr(enemy)<64
                &&!(enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem)
                &&!(enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)){
            if(shelter.at==null&&escapeMelee(enemy.position()))return true;
            var covered=shelter.tick(enemy.position());if(Boolean.TRUE.equals(covered))return true;
        }
        if(overwhelmed&&enemy instanceof net.minecraft.world.entity.Mob&&enemy.onGround()
                &&player.distanceToSqr(enemy)<49&&player.getY()<enemy.getBoundingBox().maxY+1.2
                &&!(enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem)
                &&!(enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)
                &&survival.takeHighGround())return true;
        if(overwhelmed){
            var danger=Vec3.ZERO;for(var mob:crowd)danger=danger.add(mob.position());
            if(retreat(danger.scale(1.0/crowd.size()),12))return true;
        }
        if(thorns(enemy)&&retreat(enemy.position(),7))return true;
        if(enemy instanceof Creeper creeper&&creeper.getSwellDir()>0&&retreat(enemy.position(),8))return true;
        for(var tnt:player.level().getEntitiesOfClass(PrimedTnt.class,player.getBoundingBox().inflate(9),e->e.getFuse()<=45))
            if(retreat(tnt.position(),9))return true;
        if(enemy!=null&&enemy.getUUID().equals(struck)&&player.tickCount-struckAt<30
                &&player.getAttackStrengthScale(.5f)<.88&&retreat(enemy.position(),3.8,true))return true;
        return false;
    }
    private boolean shelterThreat(){
        return rangedAttacker instanceof net.minecraft.world.entity.Mob&&rangedAttacker.isAlive()
                &&(rangedAttacker.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem
                ||rangedAttacker.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)
                &&(shelter.at!=null||rangedHits>=1||player.getHealth()<player.getMaxHealth()*.9&&thorns(rangedAttacker));
    }
    boolean shelterUrgent(){return shelterThreat()&&shelter.urgent();}
    boolean sheltered(LivingEntity enemy){return enemy==rangedAttacker&&shelter.at!=null&&!shelter.urgent()
            &&player.position().subtract(Vec3.atBottomCenterOf(shelter.at)).horizontalDistanceSqr()<2;}
    int rangedUntil;
    private void rememberRanged(LivingEntity attacker){if(rangedAttacker!=attacker)rangedHits=0;rangedAttacker=attacker;rangedUntil=player.tickCount+60;}
    void rangedDamage(LivingEntity attacker){rememberRanged(attacker);rangedHits++;}
    private Boolean shelter(){
        if(!shelterThreat()){shelter.clear();return null;}
        return shelter.tick(rangedAttacker.position());
    }
    boolean tick(LivingEntity enemy){
        if(player.tickCount>=nextThreat){incoming=LbcBotActions.incoming(player);nextThreat=player.tickCount+3;}
        double arrival=incoming==null?Double.POSITIVE_INFINITY:LbcBotActions.impactTime(player,incoming);
        boolean actual=Double.isFinite(arrival);
        if(actual&&incoming.getOwner() instanceof LivingEntity attacker&&attacker!=player&&!player.isAlliedTo(attacker)){
            retaliation=attacker;rememberRanged(attacker);
        }
        // A visible hostile archer telegraphs its attack before the first fast arrow reaches us.
        if(enemy!=null&&(enemy instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()==player||enemy.isUsingItem())
                &&(enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem
                ||enemy.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)
                &&player.hasLineOfSight(enemy))rememberRanged(enemy);
        if(!actual&&player.tickCount>=rangedUntil){
            rangedAttacker=null;rangedHits=0;cover=null;coverDirection=null;shelter.clear();
        }
        if(!actual){
            if(rangedAttacker==null||!rangedAttacker.isAlive()||rangedAttacker.level()!=player.level()
                    ||!shelterThreat()&&(player.distanceToSqr(rangedAttacker)>32*32||player.distanceToSqr(rangedAttacker)<16)
                    ||!player.hasLineOfSight(rangedAttacker)&&!shelterThreat()&&cover==null)return retreatThreats(enemy);
            threat=rangedAttacker.getEyePosition();arrival=20;
        }else threat=incoming.position();
        driver.halt();
        for(int i=0;i<36;i++){
            var item=player.getInventory().getItem(i);
            if(!item.has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS))continue;
            driver.halt();
            if(!driver.equip(item)||!driver.aimAction(threat))return true;
            if(player.gameMode.useItem(player,player.level(),player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction())shieldUntil=player.tickCount+Math.max(12,(int)Math.ceil(arrival)+4);
            return true;
        }
        if(actual&&(incoming instanceof net.minecraft.world.entity.projectile.ShulkerBullet||incoming.is(net.minecraft.tags.EntityTypeTags.REDIRECTABLE_PROJECTILE))
                &&player.distanceToSqr(incoming)<64){
            if(!driver.aimAction(incoming.getBoundingBox().getCenter()))return true;
            if(player.tickCount>=nextCounter&&LbcBotActions.melee(player,incoming,player.entityInteractionRange())){
                player.attack(incoming);player.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
                if(incoming.isRemoved()||incoming.getOwner()==player)countered++;
                nextCounter=player.tickCount+5;
            }
            return true;
        }
        var sheltered=shelter();if(sheltered!=null)return sheltered;
        var toward=threat.subtract(player.position()).multiply(1,0,1).normalize();
        if(cover!=null&&(player.position().distanceToSqr(Vec3.atBottomCenterOf(cover))>16
                ||coverDirection!=null&&coverDirection.dot(toward)<.7))cover=null;
        if(cover==null){
            var drift=player.getDeltaMovement().multiply(6,0,6);
            if(drift.lengthSqr()>4)drift=drift.normalize().scale(2);
            var candidate=BlockPos.containing(player.position().add(drift).add(toward.scale(1.5)));
            for(int down=1;down<=4;down++){
                var ground=candidate.below(down);
                if(player.level().hasChunkAt(ground)&&!player.level().getBlockState(ground).getCollisionShape(player.level(),ground).isEmpty()){
                    cover=ground.above().immutable();coverDirection=toward;break;
                }
            }
            if(cover==null)return retreatThreats(enemy)||retreat(threat,5);
        }
        var support=cover;
        // Build from an existing visible ground face; a floating eye-height cube has no legal face.
        if(driver.hasBlocks()&&player.level().getBlockState(support).canBeReplaced()){
            driver.aimAction(Vec3.atBottomCenterOf(support));
            survival.place(support);return true;
        }
        var wall=support.above();
        if(survival.place(wall))return true;
        return retreat(threat,5);
    }
}
