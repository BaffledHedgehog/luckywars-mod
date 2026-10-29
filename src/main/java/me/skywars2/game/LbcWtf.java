package me.skywars2.game;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Consumed Label melee summon: real native mannequin, with server-side terrain planning. */
public final class LbcWtf {
    public static final net.minecraft.world.item.Item SWORD=registerSword();
    private static net.minecraft.world.item.Item registerSword(){
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.parse("skywars2:oguzok_sword"));
        var model=net.minecraft.resources.Identifier.parse("lbc:oguzok_sword");
        var properties=new net.minecraft.world.item.Item.Properties(){
            @Override public net.minecraft.resources.Identifier effectiveModel(){return model;}
        }.setId(key).sword(net.minecraft.world.item.ToolMaterial.WOOD,3,-2.4f)
                .component(DataComponents.ITEM_MODEL,model);
        return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,key,
                new eu.pb4.polymer.core.api.item.SimplePolymerItem(properties,Items.WOODEN_SWORD,true));
    }
    static ItemStack sword(){return new ItemStack(SWORD);}
    private static final ResolvableProfile CHEF_PROFILE=chefProfile();
    private static ResolvableProfile chefProfile(){
        // A static native skin patch resolves from the server pack without a Mojang lookup.
        var profile=ResolvableProfile.createResolved(new GameProfile(
                java.util.UUID.fromString("2853d7b5-a618-4c73-98b6-ff37a92157f8"),"Shef",PropertyMap.EMPTY));
        var tag=(CompoundTag)ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE,profile).getOrThrow();
        tag.putString("texture","skywars2:entity/chef");tag.putString("model","wide");
        return ResolvableProfile.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();
    }
    private static final String[] SOUNDS={"minecraft:lbcsounds.oluh_1","minecraft:lbcsounds.oluh_2",
            "minecraft:lbcsounds.oluh_3","minecraft:lbcsounds.oluh_4","minecraft:lbcsounds.oluh_5","minecraft:lbcsounds.oluh_6"};
    private static final net.minecraft.server.level.TicketType CHEF_TICKET=new net.minecraft.server.level.TicketType(
            net.minecraft.server.level.TicketType.NO_TIMEOUT,net.minecraft.server.level.TicketType.FLAG_LOADING
            |net.minecraft.server.level.TicketType.FLAG_SIMULATION|net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    // Radius three keeps this chunk and its neighbours entity-ticking across a crossing.
    private record ChunkKey(ServerLevel level,net.minecraft.world.level.ChunkPos pos){}
    private final java.util.Map<ChunkKey,Integer> tickets=new java.util.HashMap<>();
    private final GameController game;
    private final Set<Chef> chefs=new LinkedHashSet<>();
    private final List<Fountain> fountains=new ArrayList<>();
    private int firstChef;
    private static final class Fountain {
        final ServerLevel level;final Vec3 position;final String epoch;int age;
        Fountain(ServerLevel level,Vec3 position,String epoch){this.level=level;this.position=position;this.epoch=epoch;}
    }
    LbcWtf(GameController game){this.game=game;}
    LbcItems gameItems(){return game.items();}
    public static void register(){
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,
                net.minecraft.resources.Identifier.parse("skywars2:chef"),CHEF_TICKET);
    } // Vanilla MANNEQUIN type/attributes/metadata: no custom client entity.
    public static boolean managed(Entity entity){return entity instanceof Chef;}
    public static ServerPlayer owner(Entity entity){return entity instanceof Chef chef?chef.owner:null;}
    public static final class Chef extends Mannequin {
        final LivingEntity target;
        final ServerPlayer owner;
        final LbcWtf service;
        final LbcIndirectKills.Owner cause;
        final LbcChefBrain brain;
        final LbcChefSurvival survival;
        int stasis,attackDelay,combatJumpWait;
        private ChunkKey ticket;
        private Vec3 previousTarget;
        static final int HAND_INTERVAL=6;
        private int handChanged=-HAND_INTERVAL,aimUntil=-1;
        private Vec3 actionAim;
        private int sentAimYaw=256,sentAimPitch=256,sentAimHead=256;
        Vec3 lastActionPoint;
        double lastActionError,lastHeadActionError;
        int actionSerial;
        float damage;
        Chef(LbcWtf service,ServerPlayer owner,LivingEntity target){
            super(owner.level());this.service=service;this.owner=owner;this.target=target;
            this.cause=service.game.items().indirectKills().coldCause(owner,true);
            this.brain=new LbcChefBrain(this);
            this.survival=new LbcChefSurvival(this);
            // Add combat attributes only to this mannequin, leaving decorative mannequins unchanged.
            addCombatAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,1);
            addCombatAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,4);
            getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(.1);
            getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT).setBaseValue(.6);
            ((me.skywars2.mixin.ChefMannequinAccess)(Object)this).skywars2$hideDescription(true);
            applyImplicitComponent(DataComponents.PROFILE,CHEF_PROFILE);
            setCustomName(Component.translatable("shef"));setCustomNameVisible(true);
            equipCombat();
            setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            setSpeed(.1f);
        }
        public ServerPlayer getOwner(){return owner;}
        @Override public boolean shouldBeSaved(){return false;}
        boolean timeFrozen(){return service!=null&&(service.game.items().timeStop().stopped((ServerLevel)level())
                ||service.game.items().timeStop().frozen(this));}
        boolean paused(){return stasis>0||timeFrozen();}
        private void addCombatAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> type,double base){
            var map=getAttributes();
            var instance=new net.minecraft.world.entity.ai.attributes.AttributeInstance(type,a->{
                map.getAttributesToUpdate().add(a);
                if(a.getAttribute().value().isClientSyncable())map.getAttributesToSync().add(a);
            });
            ((me.skywars2.mixin.GlyphAiMixin.AttributeAccess)map).skywars2$glyphAttributes().put(type,instance);
            instance.setBaseValue(base);
        }
        boolean metadataDescriptionHidden(){return getDescription()==null;}
        ItemStack combatWeapon(){return sword();}
        boolean equip(ItemStack stack){
            if(ItemStack.isSameItemSameComponents(getMainHandItem(),stack))return true;
            if(tickCount-handChanged<HAND_INTERVAL)return false;
            setItemSlot(EquipmentSlot.MAINHAND,stack);handChanged=tickCount;
            return true;
        }
        boolean equipCombat(){
            var weapon=combatWeapon();if(!equip(weapon))return false;
            // An action may switch back from a tool before the next equipment tick.
            weapon.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{
                var instance=getAttribute(attribute);
                if(instance!=null){instance.removeModifier(modifier.id());instance.addTransientModifier(modifier);}
            });
            return true;
        }
        @Override public void setSprinting(boolean sprint){
            super.setSprinting(sprint&&(survival==null||survival.canSprint())&&stasis==0);
        }
        @Override protected float getFlyingSpeed(){return isSprinting()?.025999999f:.02f;}
        @Override public void tick(){
            service.pin(this,position());
            if(timeFrozen())return;
            if(tickCount>aimUntil){sentAimYaw=256;sentAimPitch=256;sentAimHead=256;}
            setNoGravity(false);
            if(stasis>0){halt();super.setDeltaMovement(0,getDeltaMovement().y,0);setPose(net.minecraft.world.entity.Pose.SWIMMING);}
            super.tick();
        }
        @Override public void setDeltaMovement(Vec3 value){if(service==null||!timeFrozen())super.setDeltaMovement(value);}
        @Override public void move(net.minecraft.world.entity.MoverType type,Vec3 value){
            if(service!=null&&timeFrozen())return;
            if(service!=null)service.pin(this,position().add(value));
            super.move(type,value);
            if(service!=null)service.pin(this,position());
        }
        @Override public boolean isPushable(){return !paused()&&super.isPushable();}
        @Override public boolean isPushedByFluid(){return !paused()&&super.isPushedByFluid();}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            if(service.game.items().polymorph().active(this))return service.game.items().polymorph().redirectDamage(this,level,source,amount);
            if(paused()||!(amount>0)||!service.game.items().allowDamage(this,source))return false;
            return super.hurtServer(level,source,amount);
        }
        @Override protected void actuallyHurt(ServerLevel level,DamageSource source,float amount){
            float accepted=getDamageAfterMagicAbsorb(source,getDamageAfterArmorAbsorb(source,amount));
            if(!(accepted>0))return;
            receive(accepted);
            // Infinite HP does not decrease, so the generic Fabric damage delta is
            // not a reliable accepted-hit callback for this native actor.
            service.attachLabel(this,source);
            // Keep vanilla hurt cooldown, animation, sound and responsible attacker, but never enter death.
        }
        void receive(float accepted){damage+=accepted;if(damage>=20){
            damage=0;stasis=200;halt();survival.cancel();brain.reset();setNoGravity(false);
            setPose(net.minecraft.world.entity.Pose.SWIMMING);
        }}
        @Override public void setHealth(float health){
            if(service==null){super.setHealth(health);return;}
            if(!paused()&&health<getHealth())receive(getHealth()-health);
        }
        @Override public void die(DamageSource source){} // Infinite HP never runs lethal/drop callbacks.
        @Override public void setPos(double x,double y,double z){
            if(service!=null&&timeFrozen())return;
            if(service!=null)service.pin(this,new Vec3(x,y,z));
            super.setPos(x,y,z);
        }
        @Override public void snapTo(double x,double y,double z,float yaw,float pitch){if(service==null||!timeFrozen())super.snapTo(x,y,z,yaw,pitch);}
        @Override protected void onBelowWorld(){brain.returnFromVoid();}
        @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 point){return InteractionResult.FAIL;}
        @Override public SlotAccess getSlot(int index){return new SlotAccess(){public ItemStack get(){return ItemStack.EMPTY;}public boolean set(ItemStack stack){return false;}};}
        @Override public boolean isEquippableInSlot(ItemStack stack,EquipmentSlot slot){return false;}
        void halt(){xxa=0;zza=0;yya=0;jumping=false;setSprinting(false);}
        void rescue(Vec3 at){service.pin(this,at);super.setPos(at.x,at.y,at.z);super.setDeltaMovement(Vec3.ZERO);fallDistance=0;}
        void restorePose(){
            if(stasis>0){setPose(net.minecraft.world.entity.Pose.SWIMMING);return;}
            var standing=getDefaultDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(position());
            if(level().noCollision(this,standing))setPose(net.minecraft.world.entity.Pose.STANDING);
            else if(level().noCollision(this,getDefaultDimensions(net.minecraft.world.entity.Pose.CROUCHING).makeBoundingBox(position())))setPose(net.minecraft.world.entity.Pose.CROUCHING);
            else setPose(net.minecraft.world.entity.Pose.SWIMMING);
        }
        @Override public void jumpFromGround(){
            if(paused())return;
            super.jumpFromGround();
            // Bridging looks at a support face while movement is strafed. Keep the
            // native sprint impulse in that movement direction, not backwards into the gap.
            if(isSprinting()&&xxa*xxa+zza*zza>.01){
                double yaw=Math.toRadians(getYRot());
                Vec3 input=new Vec3(xxa*Math.cos(yaw)-zza*Math.sin(yaw),0,xxa*Math.sin(yaw)+zza*Math.cos(yaw)).normalize();
                setDeltaMovement(getDeltaMovement().add(Math.sin(yaw)*.2,0,-Math.cos(yaw)*.2).add(input.scale(.2)));
            }
            // Mannequin has no LocalPlayer input controller to enter airborne
            // travel on takeoff. Ground drag here would consume the sprint impulse.
            if(getDeltaMovement().y>0)setOnGround(false);
            survival.jumpExhaustion();
        }
        void pillarJump(){halt();jumping=true;}
        void lookAt(Vec3 at){
            if(actionAim!=null&&tickCount<=aimUntil)at=actionAim;
            turnLook(at,15,8);
        }
        private void turnLook(Vec3 at,float yawStep,float pitchStep){
            Vec3 delta=at.subtract(getEyePosition());
            float yaw=delta.horizontalDistanceSqr()<1.0e-8?getYRot():(float)(Math.atan2(-delta.x,delta.z)*180/Math.PI);
            float pitch=(float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
            float head=net.minecraft.util.Mth.approachDegrees(getYHeadRot(),yaw,yawStep);
            head=getYRot()+net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(head-getYRot()),-75,75);
            setYHeadRot(head);setXRot(net.minecraft.util.Mth.approachDegrees(getXRot(),pitch,pitchStep));
        }
        void face(Vec3 at){
            Vec3 delta=at.subtract(getEyePosition());
            float desired=(float)(Math.atan2(-delta.x,delta.z)*180/Math.PI);
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(),desired,20));
            setYBodyRot(net.minecraft.util.Mth.approachDegrees(yBodyRot,getYRot(),15));
            lookAt(at);
        }
        /** An action waits for the actual camera ray, rather than merely requesting a turn. */
        boolean aimAction(Vec3 at){
            actionAim=at;aimUntil=tickCount+2;
            Vec3 offset=at.subtract(getEyePosition());
            float yaw=offset.horizontalDistanceSqr()<1.0e-8?getYRot():(float)Math.toDegrees(Math.atan2(-offset.x,offset.z));
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(),yaw,25));
            setYBodyRot(net.minecraft.util.Mth.approachDegrees(yBodyRot,getYRot(),20));
            turnLook(at,25,20);
            byte packedYaw=net.minecraft.util.Mth.packDegrees(getYRot()),packedPitch=net.minecraft.util.Mth.packDegrees(getXRot());
            byte packedHead=net.minecraft.util.Mth.packDegrees(getYHeadRot());
            // Action feedback can happen between ordinary entity-tracker updates.
            // Send its camera rotation first, without teleporting or moving the actor.
            var chunks=((ServerLevel)level()).getChunkSource();
            if(sentAimYaw!=packedYaw||sentAimPitch!=packedPitch){
                chunks.sendToTrackingPlayers(this,new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Rot(getId(),packedYaw,packedPitch,onGround()));
                sentAimYaw=packedYaw;sentAimPitch=packedPitch;
            }
            if(sentAimHead!=packedHead){
                chunks.sendToTrackingPlayers(this,new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(this,packedHead));sentAimHead=packedHead;
            }
            Vec3 delta=at.subtract(getEyePosition()).normalize();
            return getLookAngle().dot(delta)>=Math.cos(Math.toRadians(2))
                    &&Vec3.directionFromRotation(getXRot(),getYHeadRot()).dot(delta)>=Math.cos(Math.toRadians(2));
        }
        void acted(Vec3 at){
            lastActionPoint=at;lastActionError=Math.toDegrees(Math.acos(net.minecraft.util.Mth.clamp(
                    getLookAngle().dot(at.subtract(getEyePosition()).normalize()),-1,1)));
            lastHeadActionError=Math.toDegrees(Math.acos(net.minecraft.util.Mth.clamp(
                    Vec3.directionFromRotation(getXRot(),getYHeadRot()).dot(at.subtract(getEyePosition()).normalize()),-1,1)));
            actionSerial++;
        }
        void steer(Vec3 destination,boolean jump){
            steer(destination,jump,null);
        }
        /** Backpedal/air-place in world space without turning away from the action or landing. */
        void steer(Vec3 destination,boolean jump,Vec3 facing){
            Vec3 delta=destination.subtract(position());
            double distance=delta.horizontalDistance();
            if(distance<.035){xxa=0;zza=0;jumping=jump;return;}
            Vec3 look=facing==null?delta:facing.subtract(position());
            float desiredYaw=(float)(Math.atan2(-look.x,look.z)*180/Math.PI);
            if(actionAim==null||tickCount>aimUntil)setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(),desiredYaw,20));
            if(actionAim==null||tickCount>aimUntil)setYBodyRot(net.minecraft.util.Mth.approachDegrees(yBodyRot,getYRot(),15));
            // Direction is independent of bounded body rotation: corners cannot make a circling actor.
            double yaw=Math.toRadians(getYRot()),dx=delta.x/distance,dz=delta.z/distance;
            float throttle=(float)(!onGround()?1:Math.min(1,distance*4));
            xxa=(float)(dx*Math.cos(yaw)+dz*Math.sin(yaw))*throttle;
            zza=(float)(-dx*Math.sin(yaw)+dz*Math.cos(yaw))*throttle;
            setSpeed((float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
            jumping=jump;lookAt(facing==null?target.getEyePosition():facing);
        }

    }
    public void melee(LivingEntity victim,DamageSource source){
        // The native Chef invokes this at its own accepted-damage boundary exactly once.
        if(victim instanceof Chef)return;
        attachLabel(victim,source);
    }
    private boolean labelTarget(ServerPlayer owner,LivingEntity target){
        // A deliberate label hit may select one's own Chef; automatic homing stays protected.
        return target instanceof Chef?game.items().wands().projectileTarget(owner,target):game.items().wands().target(owner,target);
    }
    private void attachLabel(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)
                ||!LbcFartus.isMaterial(owner.getMainHandItem())||!eligible(owner)||!labelTarget(owner,victim))return;
        Chef chef=spawn(owner,victim);
        if(chef!=null){
            owner.getMainHandItem().shrink(1);owner.getInventory().setChanged();
            if(victim instanceof Chef)game.items().mana().award(owner,"lbc:true_advancements/legends/chef_label");
        }
    }
    private boolean livingOwner(ServerPlayer owner){return owner.isAlive()&&game.canUseGameplayItems(owner)
            &&owner.level()==game.maps().level();}
    private boolean eligible(ServerPlayer owner){return livingOwner(owner)&&!game.items().actionsBlocked(owner);}
    Chef spawn(ServerPlayer owner,LivingEntity target){
        if(!eligible(owner)||target==null||!target.isAlive()||target.level()!=owner.level()
                ||!labelTarget(owner,target))return null;
        Vec3 at=target.position().subtract(target.getLookAngle().multiply(1,0,1).normalize().scale(10));
        if(!owner.level().hasChunkAt(BlockPos.containing(at)))return null;
        Chef chef=new Chef(this,owner,target);chef.setPos(at);
        game.items().markCurrentItemEpoch(chef);chefs.add(chef);pin(chef,chef.position());
        game.items().timeStop().prepareFrozen(chef);
        if(!owner.level().addFreshEntity(chef)||chef.isRemoved()){remove(chef);return null;}
        chef.brain.findSpawnSurface();
        return chef;
    }
    void step(Chef chef){
        if(game.items().polymorph().suspended(chef))return;
        if(!game.items().currentEpoch(chef)||chef.isRemoved()||!livingOwner(chef.owner)
                ||!chef.target.isAlive()||chef.target.isRemoved()||chef.target.level()!=chef.level()) {remove(chef);return;}
        if(game.items().timeStop().stopped((ServerLevel)chef.level())||game.items().timeStop().frozen(chef))return;
        if(chef.getY()<chef.level().getMinY()-8){chef.brain.returnFromVoid();}
        if(chef.stasis>0){chef.halt();chef.survival.cancel();chef.stasis--;chef.restorePose();return;}
        chef.restorePose();
        if(game.items().actionsBlocked(chef)){chef.halt();chef.survival.cancel();return;}
        if(!labelTarget(chef.owner,chef.target)){chef.halt();chef.survival.cancel();return;}
        if(!chef.level().hasChunkAt(chef.blockPosition())){chef.halt();return;}
        if(chef.owner.getRandom().nextFloat()<.005f)LbcOverload.sound(chef.owner,chef.position(),SOUNDS[chef.owner.getRandom().nextInt(SOUNDS.length)],2,1,0);
        if(chef.attackDelay>0)chef.attackDelay--;
        if(chef.brain.freeBody())return;
        if(chef.survival.tick())return;
        boolean movingTarget=chef.previousTarget!=null&&chef.target.position().distanceToSqr(chef.previousTarget)>.0004;
        chef.previousTarget=chef.target.position();
        if(chef.distanceToSqr(chef.target)<=6.25&&chef.hasLineOfSight(chef.target)){
            boolean chase=movingTarget||chef.position().subtract(chef.target.position()).horizontalDistanceSqr()>1.44;
            if(chase){chef.setSprinting(true);chef.steer(chef.target.position(),false);}
            else chef.halt();
            boolean ready=chef.aimAction(chef.target.getEyePosition())&&chef.equipCombat();
            boolean crit=!chef.onGround()&&chef.getDeltaMovement().y<-.05&&!chef.isInWater()&&!chef.onClimbable()&&!chef.isPassenger();
            if(chef.attackDelay==0&&ready){
                boolean safeJump=chef.onGround()&&!chef.isInWater()&&chef.level().noCollision(chef,chef.getBoundingBox().move(0,1.25,0));
                if(safeJump&&!movingTarget&&chef.combatJumpWait==0){
                    if(chase)chef.steer(chef.target.position(),true);else chef.pillarJump();
                    chef.combatJumpWait=12;return;
                }
                if(chef.combatJumpWait>0&&!crit&&!movingTarget){chef.combatJumpWait--;if(chef.combatJumpWait>0)return;}
                chef.combatJumpWait=0;
                // Combat equip has synchronized the held sword modifiers for this action.
                double speed=chef.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
                chef.attackDelay=(int)Math.ceil(20/Math.max(.1,speed));
                chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
                chef.acted(chef.target.getEyePosition());
                var level=(ServerLevel)chef.level();
                var source=new DamageSource(level.damageSources().mobAttack(chef).typeHolder(),chef,chef.owner);
                float hit=(float)chef.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*(crit?1.5f:1);
                var cause=game.items().indirectKills().coldCause(chef.owner,false);
                boolean[] accepted={false};game.items().indirectKills().withCause(chef.target,cause,()->accepted[0]=chef.target.hurtServer(level,source,hit));
                level.playSound(null,chef.getX(),chef.getY(),chef.getZ(),accepted[0]
                        ?(crit?net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT:net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG)
                        :net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
                level.sendParticles(crit?ParticleTypes.CRIT:ParticleTypes.SWEEP_ATTACK,
                        chef.target.getX(),chef.target.getY()+.9,chef.target.getZ(),crit?8:1,.2,.3,.2,.1);
                if(accepted[0])chef.survival.attackExhaustion();
            }
            return;
        }
        if(chef.combatJumpWait>0)chef.combatJumpWait--;
        chef.brain.tick();
    }
    public void tick(){
        if(!game.server().tickRateManager().runsNormally())return;
        LbcChefBrain.beginTick(game.server().getTickCount());
        var batch=List.copyOf(chefs);
        if(!batch.isEmpty()){int start=Math.floorMod(firstChef++,batch.size());for(int i=0;i<batch.size();i++)step(batch.get((start+i)%batch.size()));}
        fountains.removeIf(f->{
            if(!game.items().currentEpoch(f.epoch)||!game.playing())return true;
            if(game.items().timeStop().stopped(f.level))return false;
            f.level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.YELLOW)),true,false,
                    f.position.x,f.position.y,f.position.z,0,100,70000,0,.00001);
            f.level.playSound(null,f.position.x,f.position.y,f.position.z,net.minecraft.sounds.SoundEvents.GRASS_STEP,net.minecraft.sounds.SoundSource.BLOCKS,.5f,2);
            return ++f.age>=60;
        });
    }
    public void crafted(ServerPlayer owner){
        var base=owner.blockPosition();fountains.add(new Fountain(owner.level(),new Vec3(base.getX()+.5,base.getY()+1,base.getZ()+.5),game.items().epoch()));
        LbcOverload.sound(owner,owner.position(),"minecraft:lbcsounds.icum",1,1,0);
    }
    private void pin(Chef chef,Vec3 at){
        if(!chefs.contains(chef)||chef.isRemoved())return;
        var key=new ChunkKey((ServerLevel)chef.level(),net.minecraft.world.level.ChunkPos.containing(BlockPos.containing(at)));
        if(key.equals(chef.ticket))return;
        // TicketStorage coalesces equal type/level tickets, so shared chunks need ownership counts.
        if(tickets.merge(key,1,Integer::sum)==1)key.level.getChunkSource().addTicketWithRadius(CHEF_TICKET,key.pos,3);
        ChunkKey old=chef.ticket;chef.ticket=key;unpin(old);
    }
    private void unpin(ChunkKey key){
        if(key==null)return;
        Integer count=tickets.get(key);
        if(count==null)return;
        if(count>1)tickets.put(key,count-1);
        else {tickets.remove(key);key.level.getChunkSource().removeTicketWithRadius(CHEF_TICKET,key.pos,3);}
    }
    int pinnedChunks(){return tickets.size();}
    /** Explicit erasure/transformation bypasses infinite HP, knock and all death/drop callbacks. */
    boolean erase(Entity entity){if(!(entity instanceof Chef chef)||chef.service!=this)return false;remove(chef);return true;}
    private void remove(Chef chef){unpin(chef.ticket);chef.ticket=null;chefs.remove(chef);chef.brain.reset();chef.survival.clear();if(!chef.isRemoved())chef.discard();}
    public void loaded(Entity entity){
        if(entity instanceof Chef chef){if(!chef.isRemoved()&&chef.service==this&&game.items().currentEpoch(chef))chefs.add(chef);else chef.discard();}
        else if(entity.entityTags().contains("skywars2.label_chef_display"))entity.discard();
    }
    public void unloaded(Entity entity){
        if(entity instanceof Chef chef){
            // Visibility/accessibility loss is not destruction. Retire only the removed
            // actor; a still-live pursuer retains its service state and simulation ticket.
            if(chef.isRemoved()||chef.service!=this||!game.items().currentEpoch(chef))remove(chef);
            else pin(chef,chef.position());
        }
    }
    public void forget(ServerPlayer owner){for(var chef:List.copyOf(chefs))if(chef.owner==owner||chef.target==owner)remove(chef);}
    public void stop(){for(var chef:List.copyOf(chefs))remove(chef);fountains.clear();}
    public int count(){return chefs.size();}
    public int fountainCount(){return fountains.size();}
    public int pendingCount(){return chefs.size()+fountains.size();}
    int stasis(Chef chef){return chef.stasis;}
    float damageCount(Chef chef){return chef.damage;}
    List<ItemStack> tools(Chef chef){var tools=new ArrayList<>(chef.brain.tools());tools.addAll(chef.survival.supplies());return List.copyOf(tools);}
    List<Chef> snapshot(){return List.copyOf(chefs);}
}
