package me.skywars2.game;

import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Perception and ordinary survival inputs. Every block/item action still belongs to its native owner. */
final class LbcBotSurvival {
    final ServerPlayer player;final LbcChefBrain.Driver driver;final LbcChefBrain brain;final Consumer<Vec3> destination;
    Vec3 safe,climbGoal,landing;BlockPos cage,water;Direction exit;int waterAt,cageAt;String decision="idle";boolean cageCovered,cageRain,escapeWater,cageSwimming,cageRoofAbandoned,hasSafe;
    private record EscapeWarning(Object source,Vec3 center,double radius){}
    int nextLandingSearch,nextWarning,surfaceWarnings,combatClimbUntil;EscapeWarning warning;Vec3 warningGoal;
    final LbcBotShelter blastShelter;boolean placementWaiting;
    String diagnostics(){
        if(cage==null||exit==null)return "cage=none water="+water+" climb="+climbGoal+" landing="+landing+" warning="+warningGoal+" surfaceWarnings="+surfaceWarnings+" blastCover="+blastShelter.diagnostics()+" wet="+player.isInWater()+" burning="+player.isOnFire();
        var upper=cage.above().relative(exit);var lower=cage.relative(exit);
        var ray=player.level().clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),Vec3.atCenterOf(upper),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        var actual=LbcBotActions.ray(player,player.blockInteractionRange());
        return "cage="+cage+" exit="+exit+" decision="+decision+" roof="+player.level().getBlockState(cage.above(cageRain?3:2))+" upper="+player.level().getBlockState(upper)+" lower="+player.level().getBlockState(lower)+" upperRay="+ray.getType()+"/"+ray.getBlockPos()+" actualRay="+actual.getType()+"/"+actual.getBlockPos()+" water="+water+" rain="+cageRain+" covered="+cageCovered+" swimExit="+escapeWater+" swimming="+player.isSwimming();
    }
    LbcBotSurvival(ServerPlayer player,LbcChefBrain.Driver driver,LbcChefBrain brain,Consumer<Vec3> destination){this.player=player;this.driver=driver;this.brain=brain;this.destination=destination;safe=player.position();hasSafe=brain.grounded();blastShelter=new LbcBotShelter(player,driver,this);}
    ItemStack find(Item wanted){for(int i=0;i<36;i++){var s=player.getInventory().getItem(i);if(s.is(wanted))return s;}return ItemStack.EMPTY;}
    boolean aimUse(ItemStack item,Vec3 aim){
        return aimUse(item,aim,null);
    }
    boolean aimUse(ItemStack item,Vec3 aim,java.util.function.Consumer<net.minecraft.world.phys.BlockHitResult> accepted){
        if(item.isEmpty()||!driver.equip(item)||!driver.aimAction(aim))return false;
        var hit=accepted==null?null:LbcBotActions.ray(player,player.blockInteractionRange());
        boolean used=player.gameMode.useItem(player,player.level(),player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction();
        if(used&&hit!=null&&hit.getType()==HitResult.Type.BLOCK)accepted.accept(hit);
        return used;
    }
    boolean place(BlockPos target){
        return place(target,null);
    }
    boolean place(BlockPos target,ItemStack building){
        placementWaiting=false;
        var level=player.level();if(!level.hasChunkAt(target)||!level.getBlockState(target).canBeReplaced())return !level.getBlockState(target).getCollisionShape(level,target).isEmpty();
        if(!driver.equip(building==null?new ItemStack(Items.COBBLESTONE):building)){placementWaiting=true;return false;}
        for(Direction side:Direction.values()){
            var neighbor=target.relative(side);if(!level.hasChunkAt(neighbor))continue;
            for(var shape:level.getBlockState(neighbor).getShape(level,neighbor).toAabbs()){
                double x=(shape.minX+shape.maxX)*.5,y=(shape.minY+shape.maxY)*.5,z=(shape.minZ+shape.maxZ)*.5;
                switch(side){case DOWN->y=shape.maxY;case UP->y=shape.minY;case WEST->x=shape.maxX;case EAST->x=shape.minX;case NORTH->z=shape.maxZ;case SOUTH->z=shape.minZ;}
                var at=new Vec3(neighbor.getX()+x,neighbor.getY()+y,neighbor.getZ()+z);
                if(player.getEyePosition().distanceToSqr(at)>player.blockInteractionRange()*player.blockInteractionRange())continue;
                var visible=level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),at.add(side.getStepX()*.01,side.getStepY()*.01,side.getStepZ()*.01),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                if(visible.getType()!=HitResult.Type.BLOCK||!visible.getBlockPos().equals(neighbor)||visible.getDirection()!=side.getOpposite())continue;
                if(!driver.aimAction(at)){placementWaiting=true;return false;}
                var hit=LbcBotActions.placement(player,target,player.blockInteractionRange());
                return hit!=null&&(building==null?driver.place(target,hit):LbcBotActions.placeHeld(player,target));
            }
        }
        return false;
    }
    boolean water(){
        var bucket=find(Items.WATER_BUCKET);if(bucket.isEmpty())return false;
        var at=player.blockPosition();
        // Click the web itself from above/inside, so the actual bucket places water
        // above it and the flowing water clears it. A floor below the web is too low.
        for(var web:java.util.List.of(at,at.above()))if(player.level().getBlockState(web).is(Blocks.COBWEB)){
            var aim=Vec3.atBottomCenterOf(web);
            if(aimUse(bucket,aim,this::rememberWater))return true;
            if(driver.equip(bucket)){driver.aimAction(aim);return true;}
        }
        for(var face:Direction.values())for(int distance=1;distance<=(face==Direction.DOWN&&player.fallDistance>5?40:3);distance++){
            var solid=at.relative(face,distance);if(!player.level().hasChunkAt(solid)||player.level().getBlockState(solid).getCollisionShape(player.level(),solid).isEmpty())continue;
            var aim=Vec3.atCenterOf(solid).add(-face.getStepX()*.5,-face.getStepY()*.5,-face.getStepZ()*.5);
            // Aim while the ground is still far away; native bucket use only succeeds
            // once its real ray is in range. Waiting until three blocks is too late.
            if(aimUse(bucket,aim,this::rememberWater))return true;
            if(driver.equip(bucket)){driver.aimAction(aim);return true;}
        }
        return false;
    }
    private void rememberWater(net.minecraft.world.phys.BlockHitResult hit){
        water=null;
        for(var pos:java.util.List.of(hit.getBlockPos().relative(hit.getDirection()),hit.getBlockPos())){
            var fluid=player.level().getFluidState(pos);
            if(fluid.is(FluidTags.WATER)&&fluid.isSource()){water=pos.immutable();waterAt=player.tickCount;break;}
        }
    }
    boolean safeStep(Vec3 point){
        // The shared shape/support check already rejects lava. Shallow water is a usable escape.
        return brain.safeWalk(point);
    }
    boolean urgent(){
        var at=player.blockPosition();
        return cage!=null||player.isOnFire()||player.isInLava()||player.fallDistance>5||player.getY()<safe.y-5
                ||player.level().getBlockState(at).is(Blocks.COBWEB)||player.level().getBlockState(at.above()).is(Blocks.COBWEB);
    }
    /** The ordinary shared pillar route, using the actor's finite block supply. */
    boolean takeHighGround(){
        if(climbGoal!=null||!player.onGround()||!driver.hasBlocks())return false;
        var center=Vec3.atBottomCenterOf(player.blockPosition());
        if(!brain.supported(center))return false;
        for(int rise=5;rise>=3;rise--){
            // The shared navigator stands one block below the column destination.
            if(!player.level().noCollision(player,new AABB(center.x-.299,center.y+.05,center.z-.299,
                    center.x+.299,center.y+rise+.8,center.z+.299)))continue;
            climbGoal=center.add(0,rise,0);combatClimbUntil=player.tickCount+160;
            brain.reset();destination.accept(climbGoal);brain.tick();return true;
        }
        // A low, breakable roof can be opened by the same native mining/column
        // planner. The tactical deadline also bounds an unbreakable obstruction.
        climbGoal=center.add(0,5,0);combatClimbUntil=player.tickCount+160;
        brain.reset();destination.accept(climbGoal);brain.tick();return true;
    }
    /** Keep native air control available while preparing a real bucket click. */
    private boolean landing(){
        if(cage!=null||!hasSafe)return false;
        if(brain.grounded()){landing=null;return false;}
        var motion=player.getDeltaMovement();var source=player.getLastDamageSource();
        boolean knocked=source!=null&&(source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                ||source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile
                ||source.getEntity() instanceof net.minecraft.world.entity.LivingEntity)
                &&(motion.y>.5||motion.horizontalDistanceSqr()>.25);
        boolean lifted=player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)&&player.getY()>safe.y+1.2;
        if(landing!=null&&(!brain.supported(landing)||landing.y>player.getY()+.1&&motion.y<0))landing=null;
        if(landing==null&&player.tickCount>=nextLandingSearch&&(lifted||knocked||player.fallDistance>3)){
            nextLandingSearch=player.tickCount+10;
            landing=brain.supported(safe)&&safe.y<=player.getY()+.1?safe:brain.landingSurface(safe,player.getY()+.1);
            if(landing==null)landing=brain.landingSurface(player.position(),player.getY()+.1);
        }
        if(landing==null)return false;
        if(player.position().subtract(landing).horizontalDistanceSqr()<=.16&&motion.horizontalDistanceSqr()<=.0016)return false;
        player.setSprinting(false);
        // Counter current drift with ordinary directional input; no motion/position correction.
        driver.steer(new Vec3(landing.x-motion.x*4,player.getY(),landing.z-motion.z*4),false,landing);
        return true;
    }
    private boolean fleeWarning(){
        if(player.tickCount>=nextWarning){
            var eruption=driver.items().ultraponos().warning(player.level(),player.position());nextWarning=player.tickCount+5;
            EscapeWarning current=eruption==null?null:new EscapeWarning(eruption.center(),eruption.center(),eruption.radius());
            if(current==null){
                // Start an actual route while the native fuse still leaves time
                // to cross or bridge a gap. A last-second local retreat cannot
                // escape a small island and only circles its remaining ground.
                var explosive=warning!=null&&warning.source() instanceof net.minecraft.world.entity.item.PrimedTnt tnt
                        &&tnt.isAlive()&&tnt.level()==player.level()&&player.distanceToSqr(tnt)<24*24?tnt:null;
                if(explosive==null)explosive=player.level().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
                        player.getBoundingBox().inflate(16),tnt->tnt.isAlive()&&tnt.getFuse()<=80).stream()
                        .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                if(explosive!=null){
                    // Retain the same escape anchor as this visible TNT moves;
                    // rebuilding a route every five ticks would prevent departure.
                    var center=warning!=null&&warning.source()==explosive?warning.center():
                            new Vec3(explosive.getX(),player.getY(),explosive.getZ());
                    current=new EscapeWarning(explosive,center,8);
                }
            }
            if(current==null){warning=null;warningGoal=null;return false;}
            if(warning==null||!warning.source().equals(current.source())||warning.radius()!=current.radius()){
                var away=player.position().subtract(current.center()).multiply(1,0,1);
                if(away.lengthSqr()<1)away=driver.destination().subtract(player.position()).multiply(1,0,1);
                if(away.lengthSqr()<1)away=player.position().multiply(-1,0,-1);
                if(away.lengthSqr()<1)away=new Vec3(1,0,0);
                warningGoal=current.center().add(away.normalize().scale(current.radius()+6));
                if(current.source() instanceof net.minecraft.world.entity.item.PrimedTnt){
                    var supported=supportedBlastRetreat();
                    if(supported!=null){warningGoal=supported;surfaceWarnings++;}
                }
                brain.reset();
            }
            warning=current;
        }
        if(warningGoal==null){blastShelter.clear();return false;}
        if(player.isUsingItem())player.stopUsingItem();
        if(warning.source() instanceof net.minecraft.world.entity.item.PrimedTnt explosive&&explosive.isAlive()
                &&player.distanceToSqr(explosive)<12*12){
            // Water preserves real ground and damps native blast movement. Do
            // this before leaving a small island where the fuse cannot allow
            // enough legal bridge placements to clear the whole formation.
            if(water==null&&!find(Items.WATER_BUCKET).isEmpty()&&brain.grounded()&&water())return true;
            if(water!=null&&player.level().getFluidState(water).is(FluidTags.WATER)){
                var covered=blastShelter.tick(explosive.position());if(Boolean.TRUE.equals(covered))return true;
                var anchor=Vec3.atBottomCenterOf(water);var motion=player.getDeltaMovement();
                player.setSprinting(false);
                driver.steer(new Vec3(anchor.x-motion.x*4,player.getY(),anchor.z-motion.z*4),
                        player.isInWater()&&player.getY()<anchor.y-.25,anchor);return true;
            }
        }
        destination.accept(warningGoal);if(!brain.closeGround(warningGoal))brain.tick();return true;
    }
    /** Do not begin a bridge during a fuse when existing connected ground offers a refuge. */
    private Vec3 supportedBlastRetreat(){
        var explosives=player.level().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
                player.getBoundingBox().inflate(24),tnt->tnt.isAlive()&&tnt.getFuse()<=80);
        if(explosives.isEmpty())return null;
        record Refuge(Vec3 at,double score){}
        var candidates=new java.util.ArrayList<Refuge>();
        for(double length:new double[]{14,12,10,8,6,4,2})for(int direction=0;direction<16;direction++){
            double angle=direction*Math.PI/8;
            var point=brain.surfaceAt(player.position().add(Math.cos(angle)*length,0,Math.sin(angle)*length));
            if(point==null)continue;
            double nearest=Double.POSITIVE_INFINITY;
            for(var explosive:explosives)nearest=Math.min(nearest,point.distanceToSqr(explosive.position()));
            if(nearest<81)continue; // A nearby supported tile is not a refuge from an8-block blast.
            double value=Math.min(196,nearest)+brain.retreatMargin(point)*3-length*.1;
            candidates.add(new Refuge(point,value));
        }
        candidates.sort(java.util.Comparator.comparingDouble(Refuge::score).reversed());
        for(int i=0;i<Math.min(8,candidates.size());i++){
            var point=candidates.get(i).at;var reachable=brain.safeWalk(point)?point:brain.reachableGround(point);
            if(reachable!=null&&explosives.stream().allMatch(tnt->reachable.distanceToSqr(tnt.position())>=81))return reachable;
        }
        return null;
    }
    boolean tick(){
        return tick(false);
    }
    boolean tick(boolean combat){
        var level=player.level();var feet=player.blockPosition();
        boolean web=level.getBlockState(feet).is(Blocks.COBWEB)||level.getBlockState(feet.above()).is(Blocks.COBWEB);
        if(climbGoal==null&&brain.grounded()&&!player.isInWater()&&!player.isOnFire()&&!web
                &&(!hasSafe||player.getY()>safe.y-3||brain.plannedGround())){safe=player.position();hasSafe=true;}
        boolean flying=landing();
        boolean watering=(player.isOnFire()||web||player.isInLava()||player.fallDistance>5)&&water();
        if(flying||watering)return true;
        if(water!=null&&(player.tickCount-waterAt>120||player.position().distanceToSqr(Vec3.atCenterOf(water))>25))water=null;
        if(water!=null&&cage==null&&warning==null&&!combat&&!player.isUsingItem()&&!player.isOnFire()&&!web&&player.tickCount-waterAt>8
                &&(!escapeWater||player.position().distanceToSqr(Vec3.atCenterOf(water))>16)){
            if(!level.getFluidState(water).is(FluidTags.WATER)){water=null;}
            else {var bucket=find(Items.BUCKET);if(!bucket.isEmpty()){driver.halt();if(aimUse(bucket,Vec3.atCenterOf(water))&&player.getMainHandItem().is(Items.WATER_BUCKET))water=null;return true;}}
        }
        if(cage==null){int walls=0;for(var side:Direction.Plane.HORIZONTAL){var b=level.getBlockState(feet.relative(side));var h=level.getBlockState(feet.above().relative(side));if(b.is(Blocks.IRON_BARS)||h.is(Blocks.GLASS)&&b.is(Blocks.OBSIDIAN))walls++;}if(walls>=3){
            cage=feet.immutable();cageAt=player.tickCount;cageCovered=false;escapeWater=false;cageSwimming=false;cageRoofAbandoned=false;exit=Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());
            var above=new AABB(cage.getX()-1,cage.getY()+3,cage.getZ()-1,cage.getX()+2,cage.getY()+80,cage.getZ()+2);
            cageRain=!level.getEntitiesOfClass(net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion.class,above).isEmpty();
        }}
        if(cage!=null){
            if(player.position().subtract(Vec3.atBottomCenterOf(cage)).horizontalDistanceSqr()>2.5){cage=null;exit=null;brain.reset();}
            else {
                boolean wet=level.getBlockState(cage.relative(exit)).is(Blocks.OBSIDIAN)||escapeWater||player.isInWater();
                if(wet)escapeWater=true;
                var roof=cage.above(cageRain?3:2);
                if(!wet&&!cageCovered&&!cageRoofAbandoned){
                    if(!level.getBlockState(roof).getCollisionShape(level,roof).isEmpty())cageCovered=true;
                    else if(!driver.hasBlocks()||player.tickCount-cageAt>=32)cageRoofAbandoned=true;
                    else {decision="roof";driver.halt();cageCovered=place(roof);return true;}
                }
                // A fireball may consume that roof and its attachment faces. Do not abandon
                // the real exit to attempt an impossible floating replacement indefinitely.
                var upper=cage.above().relative(exit);var lower=cage.relative(exit);
                if(!level.getBlockState(upper).canBeReplaced()){decision="upper";driver.mine(upper);return true;}
                if(cageRain&&!wet&&!find(Items.WATER_BUCKET).isEmpty()){
                    decision="swim exit";
                    if(aimUse(find(Items.WATER_BUCKET),Vec3.atBottomCenterOf(roof))){escapeWater=true;water=roof.below();waterAt=player.tickCount;}
                    return true;
                }
                if(!wet&&!level.getBlockState(lower).canBeReplaced()){decision="lower";driver.mine(lower);return true;}
                var escape=Vec3.atBottomCenterOf(cage.relative(exit,3)).add(0,wet?1.1:0,0);
                if(player.isSwimming())cageSwimming=true;
                if(wet&&!cageSwimming&&(cageRain&&player.getY()<cage.getY()+1.05||!player.isUnderWater())){
                    // Rise inside the water column first. Moving toward the wall before
                    // the eyes are submerged can strand a standing body on the lower bars.
                    decision="swim center";
                    var center=Vec3.atBottomCenterOf(cage);
                    player.setSprinting(true);driver.steer(center,true,escape.add(0,3,0));return true;
                }
                boolean rise=wet&&player.getY()<cage.getY()+1.05;
                // Native sprint swimming follows the camera pitch. Looking down through
                // the exit while still below its sill pushes the body into the lower bars.
                decision=rise?"swim rise":"exit";
                var facing=rise?escape.add(0,3,0):escape.add(0,wet?.3:1,0);
                player.setSprinting(true);driver.steer(escape,rise,facing);return true;
            }
        }
        if(fleeWarning())return true;
        if(combatClimbUntil!=0&&player.tickCount>=combatClimbUntil){combatClimbUntil=0;climbGoal=null;brain.reset();}
        if(climbGoal!=null||hasSafe&&player.getY()<safe.y-5){
            if(climbGoal==null)climbGoal=new Vec3(Math.floor(player.getX())+.5,safe.y+1,Math.floor(player.getZ())+.5);
            if(player.onGround()&&player.getY()>=climbGoal.y-1.6){climbGoal=null;combatClimbUntil=0;brain.reset();return false;}
            // Keep the column fixed while climbing. Moving its destination under the
            // current foot every tick makes the shared navigator chase sideways ramps.
            destination.accept(climbGoal);brain.tick();return true;
        }
        return false;
    }
}
