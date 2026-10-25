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
    public static void register(){} // Vanilla MANNEQUIN type/attributes/metadata: no custom client entity.
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
            setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.COBBLESTONE,64));
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
        void equipCombat(){
            var weapon=combatWeapon();setItemSlot(EquipmentSlot.MAINHAND,weapon);
            // An action may switch back from a tool before the next equipment tick.
            weapon.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{
                var instance=getAttribute(attribute);
                if(instance!=null){instance.removeModifier(modifier.id());instance.addTransientModifier(modifier);}
            });
        }
        @Override public void setSprinting(boolean sprint){
            super.setSprinting(sprint&&(survival==null||survival.canSprint())&&stasis==0);
        }
        @Override public void tick(){
            if(timeFrozen())return;
            setNoGravity(false);
            if(stasis>0){halt();super.setDeltaMovement(0,getDeltaMovement().y,0);setPose(net.minecraft.world.entity.Pose.SWIMMING);}
            super.tick();
        }
        @Override public void setDeltaMovement(Vec3 value){if(service==null||!timeFrozen())super.setDeltaMovement(value);}
        @Override public void move(net.minecraft.world.entity.MoverType type,Vec3 value){if(service==null||!timeFrozen())super.move(type,value);}
        @Override public boolean isPushable(){return !paused()&&super.isPushable();}
        @Override public boolean isPushedByFluid(){return !paused()&&super.isPushedByFluid();}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            if(paused()||!(amount>0)||!service.game.items().allowDamage(this,source))return false;
            return super.hurtServer(level,source,amount);
        }
        @Override protected void actuallyHurt(ServerLevel level,DamageSource source,float amount){
            float accepted=getDamageAfterMagicAbsorb(source,getDamageAfterArmorAbsorb(source,amount));
            if(!(accepted>0))return;
            receive(accepted);
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
        @Override public void setPos(double x,double y,double z){if(service==null||!timeFrozen())super.setPos(x,y,z);}
        @Override public void snapTo(double x,double y,double z,float yaw,float pitch){if(service==null||!timeFrozen())super.snapTo(x,y,z,yaw,pitch);}
        @Override protected void onBelowWorld(){brain.returnFromVoid();}
        @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 point){return InteractionResult.FAIL;}
        @Override public SlotAccess getSlot(int index){return new SlotAccess(){public ItemStack get(){return ItemStack.EMPTY;}public boolean set(ItemStack stack){return false;}};}
        @Override public boolean isEquippableInSlot(ItemStack stack,EquipmentSlot slot){return false;}
        void halt(){xxa=0;zza=0;yya=0;jumping=false;setSprinting(false);}
        void rescue(Vec3 at){super.setPos(at.x,at.y,at.z);super.setDeltaMovement(Vec3.ZERO);fallDistance=0;}
        void restorePose(){
            if(stasis>0){setPose(net.minecraft.world.entity.Pose.SWIMMING);return;}
            var standing=getDefaultDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(position());
            if(level().noCollision(this,standing))setPose(net.minecraft.world.entity.Pose.STANDING);
            else if(level().noCollision(this,getDefaultDimensions(net.minecraft.world.entity.Pose.CROUCHING).makeBoundingBox(position())))setPose(net.minecraft.world.entity.Pose.CROUCHING);
            else setPose(net.minecraft.world.entity.Pose.SWIMMING);
        }
        @Override public void jumpFromGround(){
            if(paused())return;
            super.jumpFromGround();survival.jumpExhaustion();
        }
        void pillarJump(){halt();jumping=true;}
        void lookAt(Vec3 at){
            Vec3 delta=at.subtract(getEyePosition());
            float yaw=(float)(Math.atan2(-delta.x,delta.z)*180/Math.PI);
            float pitch=(float)(-Math.atan2(delta.y,Math.max(.01,delta.horizontalDistance()))*180/Math.PI);
            float head=net.minecraft.util.Mth.approachDegrees(getYHeadRot(),yaw,15);
            head=getYRot()+net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(head-getYRot()),-75,75);
            setYHeadRot(head);setXRot(net.minecraft.util.Mth.approachDegrees(getXRot(),pitch,8));
        }
        void face(Vec3 at){
            Vec3 delta=at.subtract(getEyePosition());
            float desired=(float)(Math.atan2(-delta.x,delta.z)*180/Math.PI);
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(),desired,20));
            setYBodyRot(net.minecraft.util.Mth.approachDegrees(yBodyRot,getYRot(),15));
            lookAt(at);
        }
        void steer(Vec3 destination,boolean jump){
            Vec3 delta=destination.subtract(position());
            double distance=delta.horizontalDistance();
            if(distance<.035){xxa=0;zza=0;jumping=jump;return;}
            float desiredYaw=(float)(Math.atan2(-delta.x,delta.z)*180/Math.PI);
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(),desiredYaw,20));
            setYBodyRot(net.minecraft.util.Mth.approachDegrees(yBodyRot,getYRot(),15));
            // Direction is independent of bounded body rotation: corners cannot make a circling actor.
            double yaw=Math.toRadians(getYRot()),dx=delta.x/distance,dz=delta.z/distance;
            float throttle=(float)Math.min(1,distance*2.5);
            xxa=(float)(dx*Math.cos(yaw)+dz*Math.sin(yaw))*throttle;
            zza=(float)(-dx*Math.sin(yaw)+dz*Math.cos(yaw))*throttle;
            setSpeed((float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
            jumping=jump;lookAt(target.getEyePosition());
        }

    }
    public void melee(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)
                ||!LbcFartus.isMaterial(owner.getMainHandItem())||!eligible(owner)||!game.items().wands().target(owner,victim))return;
        Chef chef=spawn(owner,victim);
        if(chef!=null){owner.getMainHandItem().shrink(1);owner.getInventory().setChanged();}
    }
    private boolean livingOwner(ServerPlayer owner){return owner.isAlive()&&game.canUseGameplayItems(owner)
            &&owner.level()==game.maps().level();}
    private boolean eligible(ServerPlayer owner){return livingOwner(owner)&&!game.items().actionsBlocked(owner);}
    Chef spawn(ServerPlayer owner,LivingEntity target){
        if(!eligible(owner)||target==null||!target.isAlive()||target.level()!=owner.level()
                ||!game.items().wands().target(owner,target))return null;
        Vec3 at=target.position().subtract(target.getLookAngle().multiply(1,0,1).normalize().scale(10));
        if(!owner.level().hasChunkAt(BlockPos.containing(at)))return null;
        Chef chef=new Chef(this,owner,target);chef.setPos(at);
        game.items().markCurrentItemEpoch(chef);chefs.add(chef);
        game.items().timeStop().prepareFrozen(chef);
        if(!owner.level().addFreshEntity(chef)||chef.isRemoved()){remove(chef);return null;}
        chef.brain.findSpawnSurface();
        return chef;
    }
    void step(Chef chef){
        if(!game.items().currentEpoch(chef)||chef.isRemoved()||!livingOwner(chef.owner)
                ||!chef.target.isAlive()||chef.target.isRemoved()||chef.target.level()!=chef.level()) {remove(chef);return;}
        if(game.items().timeStop().stopped((ServerLevel)chef.level())||game.items().timeStop().frozen(chef))return;
        if(chef.getY()<chef.level().getMinY()-8){chef.brain.returnFromVoid();}
        if(chef.stasis>0){chef.halt();chef.survival.cancel();chef.stasis--;chef.restorePose();return;}
        chef.restorePose();
        if(game.items().actionsBlocked(chef)){chef.halt();chef.survival.cancel();return;}
        if(!game.items().wands().target(chef.owner,chef.target)){chef.halt();chef.survival.cancel();return;}
        if(!chef.level().hasChunkAt(chef.blockPosition())){chef.halt();return;}
        if(chef.owner.getRandom().nextFloat()<.005f)LbcOverload.sound(chef.owner,chef.position(),SOUNDS[chef.owner.getRandom().nextInt(SOUNDS.length)],2,1,0);
        if(chef.attackDelay>0)chef.attackDelay--;
        if(chef.survival.tick())return;
        if(chef.distanceToSqr(chef.target)<=6.25&&chef.hasLineOfSight(chef.target)){
            chef.halt();chef.face(chef.target.getEyePosition());chef.equipCombat();
            boolean crit=!chef.onGround()&&chef.getDeltaMovement().y<-.05&&!chef.isInWater()&&!chef.onClimbable()&&!chef.isPassenger();
            if(chef.attackDelay==0){
                boolean safeJump=chef.onGround()&&!chef.isInWater()&&chef.level().noCollision(chef,chef.getBoundingBox().move(0,1.25,0));
                if(safeJump&&chef.combatJumpWait==0){chef.pillarJump();chef.combatJumpWait=12;return;}
                if(chef.combatJumpWait>0&&!crit){chef.combatJumpWait--;if(chef.combatJumpWait>0)return;}
                chef.combatJumpWait=0;
                // Combat equip has synchronized the held sword modifiers for this action.
                double speed=chef.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
                chef.attackDelay=(int)Math.ceil(20/Math.max(.1,speed));
                chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
                var level=(ServerLevel)chef.level();
                var source=new DamageSource(level.damageSources().mobAttack(chef).typeHolder(),chef,chef.owner);
                float hit=(float)chef.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*(crit?1.5f:1);
                var cause=game.items().indirectKills().coldCause(chef.owner,false);
                boolean[] accepted={false};game.items().indirectKills().withCause(chef.target,cause,()->accepted[0]=chef.target.hurtServer(level,source,hit));
                if(accepted[0]){chef.survival.attackExhaustion();if(crit)level.sendParticles(ParticleTypes.CRIT,chef.target.getX(),chef.target.getY()+.9,chef.target.getZ(),8,.2,.3,.2,.1);}
            }
            return;
        }
        chef.combatJumpWait=0;
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
    private void remove(Chef chef){chefs.remove(chef);chef.brain.reset();chef.survival.clear();if(!chef.isRemoved())chef.discard();}
    public void loaded(Entity entity){
        if(entity instanceof Chef chef){if(!chef.isRemoved()&&chef.service==this&&game.items().currentEpoch(chef))chefs.add(chef);else chef.discard();}
        else if(entity.entityTags().contains("skywars2.label_chef_display"))entity.discard();
    }
    public void unloaded(Entity entity){if(entity instanceof Chef chef)remove(chef);}
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
