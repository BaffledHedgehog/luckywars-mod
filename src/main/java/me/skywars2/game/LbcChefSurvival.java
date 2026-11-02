package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Native Mannequin survival inputs; no player inventory or synthetic food-use clock. */
final class LbcChefSurvival {
    private final LbcWtf.Chef chef;
    private final ServerLevel level;
    private ItemStack bucket=new ItemStack(Items.WATER_BUCKET);
    private ItemStack food=new ItemStack(Items.COOKED_BEEF,64);
    private Vec3 lastPosition;
    private int foodLevel=20,waterAge,settledWaterTicks,waterCooldown;
    private float saturation=5,exhaustion;
    private boolean eating;
    private int eatingCount,eatingNutrition;
    private float eatingSaturation;
    private BlockPos ownedWater,pickupWater;
    private int nextWaterSearch;
    private BlockState ownedWaterState;
    private boolean touchedWater;
    private Vec3 descentLanding,descentLip;
    private int descentStarted,nextDescentSearch;
    private boolean descentAirborne;

    LbcChefSurvival(LbcWtf.Chef chef){this.chef=chef;level=(ServerLevel)chef.level();lastPosition=chef.position();}

    /** One call per active native tick, after lifecycle/knock guards and before combat. */
    boolean tick(){
        chef.service.enterCheckpointCallback();try {
            movementExhaustion();
            settleConsumption();
            if(waterCooldown>0)waterCooldown--;
            observeWater();
            if(chef.isOnFire()||fallDanger()){
                cancelEating();
                if(bucket.is(Items.WATER_BUCKET)&&waterCooldown==0){
                    Landing landing=predictLanding();
                    if(landing!=null){
                        // Start taking out the bucket and looking down while still in flight.
                        // Its use still waits for native reach and the last safe landing ticks.
                        if(descentLanding!=null)chef.steer(descentLanding,false);
                        boolean equipped=chef.equip(bucket),aimed=chef.aimAction(landing.hit.getLocation());
                        if(equipped&&aimed&&(chef.isOnFire()||landing.ticks<=3)&&placeWater(landing))return true;
                        return true;
                    }
                }
            }
            if(recoverWater())return true;
            if(descendToTarget())return true;
            if(ownedWater!=null&&touchedWater&&waterAge<40&&!chef.isOnFire()&&chef.position().distanceToSqr(Vec3.atCenterOf(ownedWater))<6){
                // Let the landing settle before swimming away from the source we need
                // to pick up. Native gravity and water buoyancy remain in control.
                chef.halt();return true;
            }
            if(chef.isInWater()){
                cancelEating();
                swim();
                return true;
            }
            boolean safe=chef.onGround()&&!chef.isOnFire()&&!chef.isInLava()
                    &&chef.distanceToSqr(chef.target)>25;
            if(eating){
                if(!safe){cancelEating();return false;}
                chef.halt();chef.setSprinting(false);chef.lookAt(chef.target.getEyePosition());
                return true;
            }
            if(foodLevel<20&&safe){
                if(food.isEmpty())food=new ItemStack(Items.COOKED_BEEF,64);
                chef.halt();chef.setSprinting(false);
                if(!chef.equip(food))return true;
                food=chef.getMainHandItem();
                var consumable=food.get(DataComponents.CONSUMABLE);
                var properties=food.get(DataComponents.FOOD);
                if(consumable!=null&&properties!=null){
                    eatingCount=food.getCount();
                    eatingNutrition=properties.nutrition();eatingSaturation=properties.saturation();
                    consumable.startConsuming(chef,food,InteractionHand.MAIN_HAND);
                    eating=chef.isUsingItem();
                    return eating;
                }
            }
            return false;
        }finally{chef.service.leaveCheckpointCallback();}
    }

    boolean canSprint(){return foodLevel>6&&!eating;}
    void jumpExhaustion(){addExhaustion(chef.isSprinting()?.2f:.05f);}
    void attackExhaustion(){addExhaustion(.1f);}
    private void addExhaustion(float amount){exhaustion=Math.min(40,exhaustion+amount);}

    private void movementExhaustion(){
        Vec3 current=chef.position();
        double distance=current.subtract(lastPosition).horizontalDistance();lastPosition=current;
        // Rescue relocation and external launches are not deliberate locomotion.
        if(distance<=1.5){
            if(chef.isInWater())addExhaustion((float)(distance*.01));
            else if(chef.onGround()&&chef.isSprinting())addExhaustion((float)(distance*.1));
        }
        if(exhaustion>4){
            exhaustion-=4;
            if(saturation>0)saturation=Math.max(0,saturation-1);
            else foodLevel=Math.max(0,foodLevel-1);
        }
    }

    private void settleConsumption(){
        if(!eating)return;
        // Only vanilla finishUsingItem shrinks this exact retained stack. An interrupted
        // native use has no food credit, irrespective of elapsed service calls.
        if(food.getCount()<eatingCount){
            foodLevel=Math.min(20,foodLevel+eatingNutrition);
            saturation=Math.min(foodLevel,saturation+eatingSaturation);
            eating=false;
        }else if(!chef.isUsingItem()||chef.getMainHandItem()!=food)eating=false;
    }

    private void cancelEating(){
        settleConsumption();
        if(eating){chef.stopUsingItem();eating=false;}
    }

    /** Interrupt an action, retaining the bucket's actual placed-source ownership. */
    void cancel(){cancelEating();descentLanding=null;descentLip=null;descentAirborne=false;lastPosition=chef.position();}

    /** Removal/epoch cleanup: forget ownership without deleting world terrain. */
    void clear(){cancel();pickupWater=null;nextWaterSearch=0;ownedWater=null;ownedWaterState=null;touchedWater=false;waterAge=0;settledWaterTicks=0;}

    List<ItemStack> supplies(){return List.of(bucket.copy(),food.copy());}
    int foodLevel(){return foodLevel;}
    float saturation(){return saturation;}
    float exhaustion(){return exhaustion;}
    boolean eating(){return eating;}
    BlockPos ownedWater(){return ownedWater;}

    /** A supported approach to a nearby lip and a resident, open shaft to a lower bank.
     * No action search is replaced: if this conservative shortcut is unavailable the
     * ordinary brain retains mining/bridging responsibility. */
    private boolean descendToTarget(){
        if(descentLanding!=null){
            if(chef.isInWater()||descentAirborne&&chef.onGround()){
                descentLanding=null;descentLip=null;descentAirborne=false;return false;
            }
            if(!chef.onGround())descentAirborne=true;
            if(chef.tickCount-descentStarted>100||!safeDescentBody(descentLanding)
                    ||!descentFloor(descentLanding)){
                descentLanding=null;descentLip=null;descentAirborne=false;return false;
            }
            cancelEating();
            if(!descentAirborne){
                // Revalidate the remaining supported runway before committing to its lip.
                Vec3 remaining=descentLip.subtract(chef.position());
                Vec3 outward=descentLanding.subtract(descentLip).multiply(1,0,1);
                boolean crossingLip=remaining.multiply(1,0,1).dot(outward)<=0
                        &&remaining.horizontalDistanceSqr()<1;
                if(!crossingLip&&!descentRunway(chef.position(),descentLip)){
                    descentLanding=null;descentLip=null;chef.halt();return true;
                }
                if(!chef.equip(bucket)){chef.halt();return true;}
            }
            chef.setSprinting(false);
            // Aim down before departure; steer computes world input independently of yaw.
            chef.aimAction(descentLanding);
            chef.steer(descentLanding,false);
            return true;
        }
        if(!chef.onGround()||chef.isInWater()||chef.isOnFire()||ownedWater!=null
                ||!bucket.is(Items.WATER_BUCKET)||waterCooldown>0||level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,chef.blockPosition())
                ||chef.getDeltaMovement().horizontalDistanceSqr()>.1296
                ||chef.getY()-chef.target.getY()<4||chef.tickCount<nextDescentSearch)return false;
        nextDescentSearch=chef.tickCount+20;
        Vec3 origin=chef.position(),best=null,lip=null;
        double score=Double.POSITIVE_INFINITY;
        // Quarter-block runway probes find thin edges without requiring block-centred starts.
        for(int direction=0;direction<8;direction++){
            double angle=direction*Math.PI/4;
            Vec3 heading=new Vec3(Math.cos(angle),0,Math.sin(angle));
            Vec3 last=origin;
            for(int step=1;step<=16;step++){
                Vec3 at=origin.add(heading.scale(step*.25));
                if(!safeDescentBody(at))break;
                if(descentFloor(at)){last=at;continue;}
                // The complete footprint must clear the upper ledge before falling.
                Vec3 column=at.add(heading.scale(.65));
                BlockHitResult floor=descentLandingHit(column);
                if(floor==null||floor.getDirection()!=Direction.UP)break;
                Vec3 landing=new Vec3(column.x,floor.getLocation().y,column.z);
                double drop=origin.y-landing.y;
                if(drop<4||drop>126||Math.abs(landing.y-chef.target.getY())>3
                        ||!safeDescentBody(landing)||!descentFloor(landing))break;
                // Allow a bank margin for native residual momentum and bucket aim waits.
                boolean clear=true;
                for(Direction side:Direction.Plane.HORIZONTAL){
                    Vec3 margin=landing.add(side.getStepX()*.5,0,side.getStepZ()*.5);
                    if(!safeDescentBody(margin)||!descentFloor(margin)){clear=false;break;}
                }
                // Validate every body-sized vertical segment, including the upper lip.
                for(double y=landing.y;y<=origin.y+.01;y+=.5)
                    if(!safeDescentBody(new Vec3(column.x,y,column.z))){clear=false;break;}
                double candidate=landing.distanceToSqr(chef.target.position())+origin.distanceToSqr(last);
                for(int sweep=1;clear&&sweep<=4;sweep++)
                    clear=safeDescentBody(last.lerp(new Vec3(column.x,origin.y,column.z),sweep/4.0));
                if(clear&&candidate<score&&descentRunway(origin,last)){
                    score=candidate;best=landing;lip=last;
                }
                break;
            }
        }
        if(best==null)return false;
        descentLanding=best;descentLip=lip;descentStarted=chef.tickCount;descentAirborne=false;
        return descendToTarget();
    }

    private BlockHitResult descentLandingHit(Vec3 column){
        Vec3 from=column.add(0,.05,0);
        // Reuse the resident bounded native ray in short segments, including cliffs
        // much higher than one ray. Never skip an unloaded segment or first surface.
        for(int i=0;i<4;i++){
            Vec3 to=new Vec3(from.x,Math.max(level.getMinY()+.01,from.y-31.5),from.z);
            if(to.y>=from.y||!residentBox(from,to))return null;
            BlockHitResult hit=clip(from,to,ClipContext.Fluid.NONE);
            if(hit!=null)return hit;
            from=to;
        }
        return null;
    }

    private boolean descentRunway(Vec3 from,Vec3 to){
        int samples=Math.max(1,(int)Math.ceil(from.distanceTo(to)*4));
        for(int i=0;i<=samples;i++){
            Vec3 p=from.lerp(to,i/(double)samples);
            if(!safeDescentBody(p)||!descentFloor(p))return false;
        }
        return true;
    }
    private boolean descentFloor(Vec3 p){
        BlockHitResult floor=clip(p.add(0,.05,0),p.add(0,-.16,0),ClipContext.Fluid.NONE);
        return floor!=null&&floor.getDirection()==Direction.UP&&!descentHazard(floor.getBlockPos());
    }
    private boolean safeDescentBody(Vec3 p){
        AABB box=chef.getBoundingBox().move(p.subtract(chef.position())).deflate(.005);
        for(BlockPos b:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),
                BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(!loaded(b)||!level.getWorldBorder().isWithinBounds(b)||descentHazard(b))return false;
        return level.noCollision(chef,box);
    }
    private boolean descentHazard(BlockPos p){
        if(!loaded(p))return true;
        BlockState s=level.getBlockState(p);
        return level.getFluidState(p).is(FluidTags.LAVA)||s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)
                ||s.is(Blocks.MAGMA_BLOCK)||s.is(Blocks.CACTUS)||s.is(Blocks.SWEET_BERRY_BUSH)
                ||s.is(Blocks.WITHER_ROSE)||s.is(Blocks.POWDER_SNOW)||s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE);
    }

    private boolean fallDanger(){return !chef.onGround()&&!chef.isInWater()
            &&chef.getDeltaMovement().y<-.25&&chef.fallDistance>2.5;}

    private record Landing(BlockHitResult hit,BlockPos water,int ticks){}

    /** Eight native-sized ballistic segments, five feet rays each; no absent chunks. */
    private Landing predictLanding(){
        Vec3 p=chef.position(),velocity=chef.getDeltaMovement();
        if(chef.onGround()){
            BlockHitResult floor=clip(p.add(0,.12,0),p.add(0,-1.2,0),ClipContext.Fluid.NONE);
            return floor!=null&&floor.getDirection()==Direction.UP
                    ?new Landing(floor,floor.getBlockPos().above(),0):null;
        }
        double width=Math.max(.05,chef.getBbWidth()*.5-.02);
        double[][] offsets={{0,0},{-width,-width},{-width,width},{width,-width},{width,width}};
        for(int tick=1;tick<=8;tick++){
            Vec3 next=p.add(velocity);
            if(!residentBox(p,next))return null;
            BlockHitResult earliest=null;double nearest=Double.POSITIVE_INFINITY;
            for(double[] offset:offsets){
                Vec3 from=p.add(offset[0],.02,offset[1]),to=next.add(offset[0],.02,offset[1]);
                BlockHitResult hit=clip(from,to,ClipContext.Fluid.NONE);
                if(hit!=null&&hit.getDirection()==Direction.UP){
                    double distance=from.distanceToSqr(hit.getLocation());
                    if(distance<nearest){nearest=distance;earliest=hit;}
                }
            }
            if(earliest!=null){
                BlockPos water=earliest.getBlockPos().above();
                return new Landing(earliest,water,tick);
            }
            p=next;
            double gravity=chef.getGravity();
            if(velocity.y<=0&&chef.hasEffect(MobEffects.SLOW_FALLING))gravity=Math.min(gravity,.01);
            velocity=new Vec3(velocity.x*.91,(velocity.y-gravity)*.98,velocity.z*.91);
        }
        return null;
    }

    private boolean placeWater(Landing landing){
        BlockPos pos=landing.water;
        if(!loaded(pos)||!level.getWorldBorder().isWithinBounds(pos)
                ||!level.getFluidState(pos).isEmpty()||!level.getBlockState(pos).canBeReplaced(Fluids.WATER)
                ||chef.getEyePosition().distanceToSqr(landing.hit.getLocation())>20.25)return false;
        // Verify the real clicked support face is visible from the hand's eye ray.
        BlockHitResult visible=clip(chef.getEyePosition(),landing.hit.getLocation().add(0,-.015,0),ClipContext.Fluid.NONE);
        if(visible==null||!visible.getBlockPos().equals(landing.hit.getBlockPos())||visible.getDirection()!=Direction.UP)return false;
        // Continue native horizontal input throughout the clutch, including equipment/aim waits.
        if(descentLanding!=null)chef.steer(descentLanding,false);
        if(!chef.equip(bucket)||!chef.aimAction(landing.hit.getLocation()))return false;
        chef.acted(landing.hit.getLocation());
        boolean[] emptied={false};
        chef.service.gameItems().indirectKills().trapChanges(level,
                chef.service.gameItems().indirectKills().coldCause(chef.owner,false),
                ()->emptied[0]=((BucketItem)Items.WATER_BUCKET).emptyContents(chef,level,pos,null));
        waterCooldown=5;
        if(!emptied[0])return false;
        bucket=new ItemStack(Items.BUCKET);
        chef.setItemSlot(EquipmentSlot.MAINHAND,bucket);
        chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        // Evaporation empties a real bucket, but is not a successful rescue/source lease.
        if(level.getFluidState(pos).is(FluidTags.WATER)&&level.getFluidState(pos).isSource()){
            ownedWater=pos.immutable();ownedWaterState=level.getBlockState(pos);
            touchedWater=false;waterAge=0;settledWaterTicks=0;
        }
        return true;
    }

    private void observeWater(){
        if(ownedWater==null)return;
        waterAge++;
        if(!loaded(ownedWater))return;
        if(!level.getBlockState(ownedWater).equals(ownedWaterState)
                ||!level.getFluidState(ownedWater).is(FluidTags.WATER)
                ||!level.getFluidState(ownedWater).isSource()){
            ownedWater=null;ownedWaterState=null;return;
        }
        if(chef.isInWater()&&chef.position().distanceToSqr(Vec3.atCenterOf(ownedWater))<9)touchedWater=true;
        if(touchedWater&&!chef.isOnFire()&&Math.abs(chef.getDeltaMovement().y)<.15&&floorClose())settledWaterTicks++;
        else settledWaterTicks=0;
    }

    private boolean recoverWater(){
        if(!bucket.is(Items.BUCKET)||chef.isOnFire()||fallDanger())return false;
        boolean ownReady=ownedWater!=null&&waterAge>=10&&touchedWater&&settledWaterTicks>=4;
        if(ownedWater!=null&&!ownReady&&waterAge<40
                &&chef.position().distanceToSqr(Vec3.atCenterOf(ownedWater))<9)return false;
        if(ownReady&&reachableSource(ownedWater))pickupWater=ownedWater;
        if(pickupWater!=null&&!reachableSource(pickupWater))pickupWater=null;
        // A bucket can collect any visible native source, including waterlogging.
        // Do not remove the rescue source before the actual landing has settled.
        if(pickupWater==null&&chef.tickCount>=nextWaterSearch&&(chef.onGround()||floorClose())){
            nextWaterSearch=chef.tickCount+10;double nearest=Double.POSITIVE_INFINITY;
            BlockPos center=chef.blockPosition();
            for(BlockPos p:BlockPos.betweenClosed(center.offset(-4,-2,-4),center.offset(4,3,4))){
                if(p.equals(ownedWater)&&!ownReady||!reachableSource(p))continue;
                double distance=Vec3.atCenterOf(p).distanceToSqr(chef.getEyePosition());
                if(distance<nearest){nearest=distance;pickupWater=p.immutable();}
            }
        }
        if(pickupWater==null)return false;
        BlockPos source=pickupWater;Vec3 aim=Vec3.atCenterOf(source);
        BlockState state=level.getBlockState(source);
        if(!(state.getBlock() instanceof BucketPickup pickup)){pickupWater=null;return false;}
        chef.halt();chef.setSprinting(false);
        // Evaluate both independently so the camera can turn during hand dwell.
        boolean equipped=chef.equip(bucket),aimed=chef.aimAction(aim);
        if(!equipped||!aimed)return true;
        chef.acted(aim);
        ItemStack[] recovered={ItemStack.EMPTY};
        chef.service.gameItems().indirectKills().trapChanges(level,
                chef.service.gameItems().indirectKills().coldCause(chef.owner,false),
                ()->recovered[0]=pickup.pickupBlock(chef,level,source,state));
        pickupWater=null;
        if(!recovered[0].is(Items.WATER_BUCKET))return false;
        bucket=recovered[0];chef.setItemSlot(EquipmentSlot.MAINHAND,bucket);
        pickup.getPickupSound().ifPresent(sound->level.playSound(null,chef.blockPosition(),sound,SoundSource.BLOCKS,1,1));
        level.gameEvent(chef,GameEvent.FLUID_PICKUP,source);
        chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        if(source.equals(ownedWater)){ownedWater=null;ownedWaterState=null;touchedWater=false;}
        return true;
    }
    private boolean reachableSource(BlockPos source){
        if(!loaded(source)||!level.getFluidState(source).is(FluidTags.WATER)||!level.getFluidState(source).isSource()
                ||!(level.getBlockState(source).getBlock() instanceof BucketPickup))return false;
        Vec3 aim=Vec3.atCenterOf(source);
        if(chef.getEyePosition().distanceToSqr(aim)>20.25)return false;
        BlockHitResult hit=clip(chef.getEyePosition(),aim,ClipContext.Fluid.SOURCE_ONLY);
        return hit!=null&&hit.getBlockPos().equals(source);
    }

    private boolean floorClose(){
        Vec3 feet=chef.position();
        BlockHitResult hit=clip(feet.add(0,.05,0),feet.add(0,-1.2,0),ClipContext.Fluid.NONE);
        return hit!=null&&hit.getDirection()==Direction.UP;
    }

    private void swim(){
        chef.setSprinting(false);
        Vec3 from=chef.position(),destination=chef.target.position();
        // Find an actual nearby bank with a clear standing body before defaulting to
        // surface paddling toward the fixed victim. Search and rays stay bounded.
        double best=Double.POSITIVE_INFINITY;
        for(int radius=1;radius<=3;radius++)for(Direction dir:Direction.Plane.HORIZONTAL){
            BlockPos column=chef.blockPosition().relative(dir,radius);
            for(int dy=1;dy>=-1;dy--){
                BlockPos feet=column.offset(0,dy,0);
                if(!loaded(feet)||!loaded(feet.below())||!level.getFluidState(feet).isEmpty())continue;
                BlockHitResult floor=clip(Vec3.atBottomCenterOf(feet).add(0,.5,0),Vec3.atBottomCenterOf(feet).add(0,-1,0),ClipContext.Fluid.NONE);
                if(floor==null||floor.getDirection()!=Direction.UP)continue;
                Vec3 at=new Vec3(feet.getX()+.5,floor.getLocation().y,feet.getZ()+.5);
                var box=chef.getBoundingBox().move(at.subtract(from));
                if(!residentBox(at,at.add(0,chef.getBbHeight(),0))||!level.noCollision(chef,box))continue;
                double score=at.distanceToSqr(from)+at.distanceToSqr(chef.target.position())*.1;
                if(score<best){best=score;destination=at;}
            }
        }
        chef.steer(destination,true);
        chef.lookAt(destination.add(0,.6,0));
    }

    private boolean loaded(BlockPos pos){return level.isInsideBuildHeight(pos.getY())&&level.hasChunkAt(pos);}

    private boolean residentBox(Vec3 from,Vec3 to){
        if(from.distanceToSqr(to)>1024)return false;
        int minX=(int)Math.floor(Math.min(from.x,to.x)-.35),maxX=(int)Math.floor(Math.max(from.x,to.x)+.35);
        int minZ=(int)Math.floor(Math.min(from.z,to.z)-.35),maxZ=(int)Math.floor(Math.max(from.z,to.z)+.35);
        int y=(int)Math.floor(Math.min(from.y,to.y));
        if(!level.isInsideBuildHeight(y)||!level.isInsideBuildHeight((int)Math.floor(Math.max(from.y,to.y))))return false;
        for(int x=minX;x<=maxX;x+=16)for(int z=minZ;z<=maxZ;z+=16)
            if(!level.hasChunkAt(new BlockPos(x,y,z)))return false;
        return level.hasChunkAt(new BlockPos(maxX,y,minZ))&&level.hasChunkAt(new BlockPos(minX,y,maxZ))
                &&level.hasChunkAt(new BlockPos(maxX,y,maxZ));
    }

    private BlockHitResult clip(Vec3 from,Vec3 to,ClipContext.Fluid fluid){
        if(!residentBox(from,to))return null;
        BlockHitResult hit=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,fluid,chef));
        return hit.getType()==HitResult.Type.BLOCK?hit:null;
    }
    /** Native equipment/use-item NBT is canonical; only retained supplies and survival authority are private. */
    Checkpoint checkpoint(){return new Checkpoint();}
    private record Supply(ItemStack value,EquipmentSlot slot){
        static Supply capture(LbcWtf.Chef chef,ItemStack stack){
            EquipmentSlot alias=null;for(var slot:EquipmentSlot.values())if(!stack.isEmpty()&&chef.getItemBySlot(slot)==stack){
                if(alias!=null)throw new IllegalStateException("Chef supply aliases distinct native equipment slots");alias=slot;}
            return new Supply(stack.copy(),alias);
        }
        ItemStack resolve(LbcWtf.Chef current){
            if(slot==null)return value.copy();var nativeStack=current.getItemBySlot(slot);
            if(!ItemStack.matches(value,nativeStack))throw new IllegalStateException("Restore the canonical Chef supply equipment before survival metadata");
            return nativeStack;
        }
    }
    final class Checkpoint {
        private final Supply savedBucket=Supply.capture(chef,bucket),savedFood=Supply.capture(chef,food);
        private final Vec3 position=lastPosition;
        private final int nutrition=foodLevel,waterTicks=waterAge,settled=settledWaterTicks,cooldown=waterCooldown;
        private final float foodSaturation=saturation,spent=exhaustion;
        private final boolean consuming=eating,touched=touchedWater;
        private final int count=eatingCount,meal=eatingNutrition;
        private final float mealSaturation=eatingSaturation;
        private final BlockPos water=ownedWater==null?null:ownedWater.immutable();
        private final BlockState waterState=ownedWaterState;
        Runnable prepare(){
            var currentBucket=savedBucket.resolve(chef);var currentFood=savedFood.resolve(chef);
            return ()->{bucket=currentBucket;food=currentFood;lastPosition=position;foodLevel=nutrition;saturation=foodSaturation;exhaustion=spent;
                pickupWater=null;nextWaterSearch=0;waterAge=waterTicks;settledWaterTicks=settled;waterCooldown=cooldown;ownedWater=water;ownedWaterState=waterState;touchedWater=touched;
                eating=consuming;eatingCount=count;eatingNutrition=meal;eatingSaturation=mealSaturation;
                descentLanding=null;descentLip=null;descentAirborne=false;descentStarted=0;nextDescentSearch=chef.tickCount;};
        }
    }


    /** Per-body supplies/nutrition only; cloned actors receive no authority over existing world water. */
    java.util.function.Consumer<LbcChefSurvival> cloneState() {
        var savedBucket=Supply.capture(chef,bucket);var savedFood=Supply.capture(chef,food);
        int nutrition=foodLevel,cooldown=waterCooldown,count=eatingCount,meal=eatingNutrition;
        float savedSaturation=saturation,spent=exhaustion,mealSaturation=eatingSaturation;
        boolean consuming=eating;
        return target->{
            target.bucket=savedBucket.resolve(target.chef);target.food=savedFood.resolve(target.chef);
            target.lastPosition=target.chef.position();target.foodLevel=nutrition;target.waterCooldown=cooldown;
            target.saturation=savedSaturation;target.exhaustion=spent;
            target.eating=consuming;target.eatingCount=count;target.eatingNutrition=meal;target.eatingSaturation=mealSaturation;
            target.ownedWater=null;target.ownedWaterState=null;target.pickupWater=null;target.nextWaterSearch=0;
            target.waterAge=0;target.settledWaterTicks=0;target.touchedWater=false;
            target.descentLanding=null;target.descentLip=null;target.descentAirborne=false;
            target.descentStarted=0;target.nextDescentSearch=target.chef.tickCount;
        };
    }
}
