package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

/** Real shield/finite cover and retreat intents. Never absorbs damage or modifies an attack. */
public final class LbcBotDefense {
    final ServerPlayer player;final LbcChefBrain.Driver driver;final LbcBotSurvival survival;
    private record ThornGear(Item item,ItemEnchantments enchantments){}
    private final Map<UUID,List<ThornGear>> thorny=new LinkedHashMap<>();
    int shieldUntil, nextThreat, nextCounter;
    public int countered;Vec3 threat;LivingEntity retaliation;
    Projectile incoming;
    private Projectile plannedBlast;
    private Vec3 plannedImpact,blastExit,blastRefuge;
    private boolean plannedBlastInWater;
    private Projectile dodgedShot;
    private Vec3 sidestep;
    private int dodgeUntil;
    BlockPos cover;
    java.util.UUID struck;int struckAt;
    Vec3 retreatPoint,retreatDanger;int retreatUntil,nextCrowd;
    Vec3 escapePoint;int nextEscape;
    public LivingEntity rangedAttacker;Vec3 coverDirection;
    private Vec3 rangedBank,recoilDirection;
    private int bankUntil,nextBankSearch;
    int rangedHits;
    LivingEntity meleeAttacker;int meleeAt,meleeHits;
    final LbcBotShelter shelter;
    private final LbcBotShelter projectileShelter;
    List<net.minecraft.world.entity.Mob> crowd=List.of();
    public String diagnostics(){return "defenseShot="+(incoming==null?null:incoming.getUUID())+" blastExit="+blastExit+" blastRefuge="+blastRefuge+" sidestep="+sidestep+" bank="+rangedBank+" cover="+cover+" "+shelter.diagnostics()+" countered="+countered+" meleeHits="+meleeHits+" rangedHits="+rangedHits+" rangedAttacker="+(rangedAttacker==null?null:rangedAttacker.getMainHandItem())+" nextThreat="+nextThreat+" bodyTick="+player.tickCount;}
    void meleeDamage(LivingEntity attacker){if(meleeAttacker!=attacker)meleeHits=0;meleeAttacker=attacker;meleeAt=player.tickCount;meleeHits++;}
    LbcBotDefense(ServerPlayer player,LbcChefBrain.Driver driver,LbcBotSurvival survival){this.player=player;this.driver=driver;this.survival=survival;shelter=new LbcBotShelter(player,driver,survival);projectileShelter=new LbcBotShelter(player,driver,survival);}
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
    public boolean thorns(LivingEntity target){
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
    boolean retreat(Vec3 danger,double desired,boolean watchThreat){
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
                // During a short melee cooldown, gain room for real knockback before
                // maximizing separation. Urgent long escapes retain their distance priority.
                double marginWeight=watchThreat?3:.85;
                double score=vector.dot(direction)*length+survival.brain.retreatMargin(candidate)*marginWeight+length*.15;
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
    private Vec3 findBlastExit(LbcBotActions.BlastImpact blast){
        var position=player.position();
        var offset=position.subtract(blast.point()).multiply(1,0,1);
        double outside=Math.pow(blast.radius()+.6,2)-offset.lengthSqr();
        if(outside<=0)return survival.brain.retreatMargin(position)>0?position:null;
        var away=offset.lengthSqr()>.001?offset.normalize():new Vec3(1,0,0);
        double travel=Math.min(8,blast.ticks()*(player.isInWater()&&blast.explosions()>1?.11:.23)+.35);
        Vec3 exit=null;double best=Double.POSITIVE_INFINITY;
        for(int turn=0;turn<16;turn++){
            var direction=away.yRot((float)(turn*Math.PI/8));
            double along=offset.dot(direction);
            double length=Math.sqrt(along*along+outside)-along;
            if(length>travel)continue;
            var candidate=position.add(direction.scale(length));
            if(!survival.safeStep(candidate))continue;
            double score=length-survival.brain.retreatMargin(candidate)*.3;
            if(score<best){best=score;exit=candidate;}
        }
        return exit;
    }
    private double blastRunway(Vec3 feet,Vec3 impact,double limit){
        var direction=feet.subtract(impact).multiply(1,0,1).normalize();
        if(direction.lengthSqr()<.001)return 0;
        double length=0;
        for(double step=.75;step<=limit;step+=.75){
            if(!survival.brain.stepSupported(feet.add(direction.scale(step))))break;
            length=step;
        }
        return length;
    }
    public boolean safeFromBurst(Vec3 feet,LbcBotActions.BlastImpact blast){
        double exposureDistance=Math.max(0,1-feet.distanceTo(blast.point())/blast.radius());
        if(exposureDistance==0)return true;
        // Treat the whole impulse as horizontal. Summing native airborne drag
        // bounds travel without relying on when this body will touch down.
        double resistance=player.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        double impulse=exposureDistance*blast.explosions()*Math.max(0,1-resistance);
        double runway=Math.ceil((impulse/.09+.75)/.75)*.75;
        return coveredFromBurst(feet,blast)||blastRunway(feet,blast.point(),runway)>=runway;
    }
    private boolean coveredFromBurst(Vec3 feet,LbcBotActions.BlastImpact blast){
        var body=player.getDimensions(Pose.STANDING).makeBoundingBox(feet);
        // TNT moves down at most one gravity step before its fuse-one detonation.
        // Its detonation point is slightly above its feet.
        var center=blast.point().add(0,.025,0);
        var collision=CollisionContext.of(player);
        double maximumPower=blast.radius()*.5*1.3;
        if(!durableBlastCover(body.getCenter(),center,collision,maximumPower))return false;
        // Match the native exposure sampling of this body. Only cover that can
        // survive the first TNT counts for a burst.
        double dx=1/(body.getXsize()*2+1),dy=1/(body.getYsize()*2+1),dz=1/(body.getZsize()*2+1);
        double offsetX=(1-Math.floor(1/dx)*dx)/2,offsetZ=(1-Math.floor(1/dz)*dz)/2;
        for(double x=0;x<=1;x+=dx)for(double y=0;y<=1;y+=dy)for(double z=0;z<=1;z+=dz){
            var sample=new Vec3(body.minX+x*body.getXsize()+offsetX,body.minY+y*body.getYsize(),
                    body.minZ+z*body.getZsize()+offsetZ);
            if(!durableBlastCover(sample,center,collision,maximumPower))return false;
        }
        return true;
    }
    private boolean durableBlastCover(Vec3 from,Vec3 to,CollisionContext collision,double maximumPower){
        var level=player.level();
        var hit=LbcTerra.shapeHit(level,from,to,(pos,state)->
                (state.getBlock().getExplosionResistance()+.3)*.3>=maximumPower
                        ?state.getCollisionShape(level,pos,collision):Shapes.empty());
        return hit!=null&&level.hasChunkAt(hit.getBlockPos());
    }
    private Vec3 findBlastRefuge(LbcBotActions.BlastImpact blast){
        var feet=player.position();
        boolean wet=player.isInWater();
        boolean burst=blast.explosions()>1;
        if(burst&&survival.brain.grounded()&&!wet&&safeFromBurst(feet,blast))return feet;
        // Reserve time for a bucket click only on dry ground. Wet movement is slower.
        double reach=Math.min(3,(blast.ticks()-(wet?2:5))*(wet?.11:.22));
        if(reach<.75)return null;
        var predictedFeet=feet.add(player.getDeltaMovement().multiply(2,0,2));
        double currentRunway=burst?0:blastRunway(predictedFeet,blast.point(),4.5);
        if(!burst&&currentRunway>=4.5)return null;
        double currentDistance=feet.subtract(blast.point()).horizontalDistance();
        Vec3 best=null;
        double bestScore=0;
        var considered=new HashSet<Vec3>();
        for(int heading=0;heading<16;heading++){
            double angle=heading*Math.PI/8;
            var direction=new Vec3(Math.cos(angle),0,Math.sin(angle));
            for(double length=.75;length<=reach;length+=.75){
                var sample=feet.add(direction.scale(length));
                // Flow can keep the feet above or below the actual walking surface.
                // Aim for a supported cell, away from the exposed edge of the water.
                var point=wet||burst?survival.brain.surfaceAt(sample):sample;
                if(point==null||!considered.add(point))continue;
                double travel=point.subtract(feet).horizontalDistance();
                if(travel<.35||travel>reach+.15)continue;
                if(burst){
                    double score=reach+.2-travel;
                    if(score>bestScore&&survival.safeStep(point)&&safeFromBurst(point,blast)){
                        best=point;bestScore=score;
                    }
                    continue;
                }
                double runway=blastRunway(point,blast.point(),4.5);
                if(runway<currentRunway+1.5)continue;
                double distance=point.subtract(blast.point()).horizontalDistance();
                if(distance<currentDistance-.25)continue;
                double runwayGain=2*(runway-currentRunway);
                double score=wet?runwayGain-travel*1.5:runwayGain+distance-currentDistance-travel*.35;
                if(score<=bestScore||!survival.safeStep(point))continue;
                best=point;bestScore=score;
            }
        }
        return best;
    }
    private boolean escapeBlast(LbcBotActions.BlastImpact blast){
        boolean wet=player.isInWater();
        if(!player.onGround()&&!wet)return false;
        if(plannedBlast!=incoming||plannedImpact.distanceToSqr(blast.point())>.5
                ||wet&&!plannedBlastInWater
                ||blastExit!=null&&!survival.safeStep(blastExit)
                ||blastRefuge!=null&&!survival.safeStep(blastRefuge)){
            plannedBlast=incoming;plannedImpact=blast.point();
            plannedBlastInWater=wet;
            blastExit=findBlastExit(blast);
            blastRefuge=blastExit==null?findBlastRefuge(blast):null;
            projectileShelter.clear();
        }
        if(blastExit!=null){
            if(player.position().subtract(blastExit).horizontalDistanceSqr()>.04){
                player.setSprinting(true);driver.steer(blastExit,false,blastExit.add(0,1.5,0));
            }
            return true;
        }
        // Water may preserve the floor while the blast still knocks us off it.
        if(blastRefuge!=null){
            double stop=blast.explosions()>1?.025:.09;
            if(player.position().subtract(blastRefuge).horizontalDistanceSqr()>stop){
                player.setSprinting(!wet);
                driver.steer(blastRefuge,false,blastRefuge.add(0,1.5,0));
                return true;
            }
            if(blast.explosions()>1){
                player.setSprinting(false);driver.crouch(true);
                return true;
            }
        }
        player.setSprinting(false);driver.crouch(true);
        if(!survival.braceWithWater())projectileShelter.tick(blast.point());
        return true;
    }
    private void updateIncoming(){
        if(player.tickCount>=nextThreat){
            incoming=LbcBotActions.incoming(player);
            nextThreat=player.tickCount+3;
        }
    }
    private Vec3 projectileSidestep(Projectile shot,double arrival){
        var motion=shot.getDeltaMovement();
        var side=new Vec3(-motion.z,0,motion.x).normalize();
        if(side.lengthSqr()<.001)side=new Vec3(1,0,0);
        var from=shot.position();
        var end=from.add(motion.scale(Math.min(20,16/motion.length())));
        var body=LbcBotActions.projectileTarget(shot,player.getBoundingBox());
        var alternate=shot instanceof LbcAir.Charge air?LbcAir.forecastContact(air,end,player):null;
        double reachable=Math.min(2.25,Math.max(0,arrival-1)*.26);
        Vec3 best=null;
        double bestScore=Double.NEGATIVE_INFINITY;
        for(double distance:new double[]{.60,.95,1.35,1.8,2.25}){
            if(distance>reachable)break;
            for(int sign=-1;sign<=1;sign+=2){
                var delta=side.scale(sign*distance);
                var point=player.position().add(delta);
                var moved=body.move(delta);
                if(moved.contains(from)||moved.clip(from,end).isPresent()
                        ||alternate!=null&&moved.distanceToSqr(alternate.getLocation())
                        <Math.pow(LbcBotActions.AIR_BLAST_RADIUS+.6,2)||!survival.safeStep(point))continue;
                double score=.35*survival.brain.retreatMargin(point)-distance;
                if(score>bestScore){best=point;bestScore=score;}
            }
        }
        return best;
    }
    boolean dodgeProjectile(){
        boolean grounded=survival.brain.grounded();
        // Shallow flow can lift the feet off a ledge for several consecutive ticks.
        // Keep forecasting blasts while there is still a foothold within stepping distance.
        if(!grounded&&(!player.isInWater()||!survival.brain.stepSupported(player.position())))return false;
        updateIncoming();
        if(dodgedShot!=null&&(!dodgedShot.isAlive()||player.tickCount>=dodgeUntil
                ||player.position().subtract(dodgedShot.position()).dot(dodgedShot.getDeltaMovement())<-1
                ||!survival.safeStep(sidestep))){
            dodgedShot=null;sidestep=null;
        }
        var shot=incoming;
        var blast=shot instanceof LbcAir.Charge air&&air.emitterStyle?LbcBotActions.blastImpact(player,air):null;
        if(grounded&&shot!=dodgedShot&&(shot instanceof SmallFireball
                ||shot instanceof LbcTerra.TerraProjectile terra&&terra.emitterStyle||blast!=null&&blast.hitsActor())){
            double arrival=blast==null?LbcBotActions.impactTime(player,shot):blast.ticks()-1;
            var point=Double.isFinite(arrival)?projectileSidestep(shot,arrival):null;
            if(point!=null){
                dodgedShot=shot;sidestep=point;
                dodgeUntil=player.tickCount+Math.min(20,(int)Math.ceil(arrival)+5);
                if(shot.getOwner() instanceof LivingEntity attacker&&attacker!=player&&!player.isAlliedTo(attacker))
                    retaliation=attacker;
            }
        }
        if(blast!=null&&(!grounded||!blast.hitsActor()||shot!=dodgedShot)){
            dodgedShot=null;sidestep=null;
            if(player.isUsingItem())player.stopUsingItem();
            driver.halt();
            return escapeBlast(blast);
        }
        if(!grounded||sidestep==null)return false;
        if(player.isUsingItem())player.stopUsingItem();
        driver.halt();
        player.setSprinting(true);
        driver.steer(sidestep,false,sidestep.add(0,1.5,0));
        return true;
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
    boolean moveFromMeleeEdge(LivingEntity enemy){
        if(!groundMelee(enemy)||!player.onGround()||player.distanceToSqr(enemy)>25
                ||!player.hasLineOfSight(enemy))return false;
        var at=player.position();
        var away=at.subtract(enemy.position()).multiply(1,0,1).normalize();
        var risk=at.add(away.scale(1.5)).add(player.getDeltaMovement().multiply(2,0,2));
        if(survival.brain.stepSupported(risk)||!retreat(enemy.position(),6,true))return false;
        survival.cancelCombatClimb();
        return true;
    }
    boolean recoverRangedGround(LivingEntity enemy,boolean aerialThreat){
        boolean recentlyHit=rangedAttacker!=null&&rangedHits>0&&player.tickCount<rangedUntil;
        if(!aerialThreat&&!recentlyHit){finishRangedBank();return false;}
        if(rangedBank!=null&&(player.tickCount>=bankUntil||!survival.brain.supported(rangedBank)
                ||player.onGround()&&player.position().distanceToSqr(rangedBank)<.26))finishRangedBank();
        if(rangedBank==null&&survival.brain.grounded()&&player.tickCount>=nextBankSearch){
            nextBankSearch=player.tickCount+8;
            var danger=recentlyHit?rangedAttacker:enemy;
            var away=recentlyHit&&recoilDirection!=null?recoilDirection:
                    player.position().subtract(danger.position()).multiply(1,0,1).normalize();
            rangedBank=findRangedBank(away);
            if(rangedBank!=null)bankUntil=player.tickCount+34;
        }
        if(rangedBank==null)return false;
        if(player.isUsingItem())player.stopUsingItem();
        survival.destination.accept(rangedBank);
        if(!survival.brain.grounded()||!survival.brain.closeGround(rangedBank))
            survival.brain.tickOnExistingGround();
        return true;
    }
    private void finishRangedBank(){
        if(rangedBank==null)return;
        rangedBank=null;
        survival.brain.reset();
    }
    private Vec3 findRangedBank(Vec3 recoil){
        var brain=survival.brain;
        var from=player.position();
        var drift=player.getDeltaMovement().multiply(2,0,2);
        int margin=rangedFooting(from.add(drift));
        boolean exposed=!brain.stepSupported(from.add(drift).add(recoil.scale(2.1)));
        if(margin==8&&!exposed)return null;
        record Bank(Vec3 at,double score){}
        var candidates=new ArrayList<Bank>();
        for(double radius:new double[]{1,2,3,4})for(int heading=0;heading<8;heading++){
            double angle=heading*Math.PI/4;
            var point=brain.surfaceAt(from.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius));
            if(point==null||point.y<from.y-.2||point.distanceToSqr(from)<.45)continue;
            int safeSides=rangedFooting(point);
            boolean coversRecoil=brain.stepSupported(point.add(recoil.scale(2.1)));
            if(safeSides<3||safeSides<=margin&&(!exposed||!coversRecoil))continue;
            double score=safeSides*2+(coversRecoil?2:0)+(point.y-from.y)*.7-from.distanceTo(point)*.35;
            candidates.add(new Bank(point,score));
        }
        candidates.sort(Comparator.comparingDouble(Bank::score).reversed());
        for(int i=0;i<Math.min(5,candidates.size());i++){
            var route=brain.startSurfaceRoute(candidates.get(i).at());
            if(route!=null)return route;
        }
        return null;
    }
    private int rangedFooting(Vec3 point){
        // A shot can come from a caster or a cloud behind either shoulder.
        // Check the likely travel after a normal hit, including diagonal edges.
        int safe=0;
        for(int heading=0;heading<8;heading++){
            double angle=heading*Math.PI/4;
            if(survival.brain.stepSupported(point.add(Math.cos(angle)*2.1,0,Math.sin(angle)*2.1)))safe++;
        }
        return safe;
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
    private void rememberRanged(LivingEntity attacker){if(rangedAttacker!=attacker){rangedHits=0;recoilDirection=null;}rangedAttacker=attacker;rangedUntil=player.tickCount+60;}
    public void rangedDamage(LivingEntity attacker){rememberRanged(attacker);rangedHits++;}
    void rangedDamage(LivingEntity attacker,Projectile shot){
        rangedDamage(attacker);
        var direction=shot.getDeltaMovement().multiply(1,0,1).normalize();
        recoilDirection=direction.lengthSqr()>.001?direction:null;
    }
    private Boolean shelter(){
        if(!shelterThreat()){shelter.clear();return null;}
        return shelter.tick(rangedAttacker.position());
    }
    boolean tick(LivingEntity enemy){
        updateIncoming();
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
        var blast=actual?LbcBotActions.blastImpact(player,incoming):null;
        if(blast!=null){
            double reach=player.entityInteractionRange();
            boolean canCounter=incoming.getBoundingBox().distanceToSqr(player.getEyePosition())<=reach*reach
                    &&player.hasLineOfSight(incoming)&&incoming.is(net.minecraft.tags.EntityTypeTags.REDIRECTABLE_PROJECTILE);
            if(!canCounter&&escapeBlast(blast))return true;
        }
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
                ||Math.abs(cover.getY()-player.getY())>1.25
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
