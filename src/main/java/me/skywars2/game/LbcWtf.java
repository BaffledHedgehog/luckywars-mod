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
        int stasis,attackDelay;
        float damage;
        Chef(LbcWtf service,ServerPlayer owner,LivingEntity target){
            super(owner.level());this.service=service;this.owner=owner;this.target=target;
            this.cause=service.game.items().indirectKills().coldCause(owner,true);
            this.brain=new LbcChefBrain(this);
            applyImplicitComponent(DataComponents.PROFILE,CHEF_PROFILE);
            setCustomName(Component.translatable("shef"));setCustomNameVisible(true);
            setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_SWORD));
            setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.COBBLESTONE,64));
            setSpeed(.28f);
        }
        public ServerPlayer getOwner(){return owner;}
        @Override public boolean shouldBeSaved(){return false;}
        boolean paused(){return service!=null&&(stasis>0||service.game.items().timeStop().stopped((ServerLevel)level())
                ||service.game.items().timeStop().frozen(this));}
        @Override public void tick(){if(paused()){super.setDeltaMovement(Vec3.ZERO);return;}super.tick();}
        @Override public void setDeltaMovement(Vec3 value){super.setDeltaMovement(service!=null&&paused()?Vec3.ZERO:value);}
        @Override public void move(net.minecraft.world.entity.MoverType type,Vec3 value){if(service==null||!paused())super.move(type,value);}
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
        void receive(float accepted){damage+=accepted;if(damage>=20){damage=0;stasis=200;halt();brain.reset();}}
        @Override public void setHealth(float health){
            if(service==null){super.setHealth(health);return;}
            if(!paused()&&health<getHealth())receive(getHealth()-health);
        }
        @Override public void die(DamageSource source){} // Infinite HP never runs lethal/drop callbacks.
        @Override public void setPos(double x,double y,double z){if(service==null||!paused())super.setPos(x,y,z);}
        @Override public void snapTo(double x,double y,double z,float yaw,float pitch){if(service==null||!paused())super.snapTo(x,y,z,yaw,pitch);}
        @Override protected void onBelowWorld(){brain.returnFromVoid();}
        @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 point){return InteractionResult.FAIL;}
        @Override public SlotAccess getSlot(int index){return new SlotAccess(){public ItemStack get(){return ItemStack.EMPTY;}public boolean set(ItemStack stack){return false;}};}
        @Override public boolean isEquippableInSlot(ItemStack stack,EquipmentSlot slot){return false;}
        void halt(){xxa=0;zza=0;jumping=false;super.setDeltaMovement(Vec3.ZERO);}
        void pillarJump(){
            xxa=0;zza=0;jumping=true;
            Vec3 velocity=getDeltaMovement();setDeltaMovement(0,velocity.y,0);
        }
        void steer(Vec3 destination,boolean jump){
            Vec3 delta=destination.subtract(position());
            setYRot((float)(Math.atan2(-delta.x,delta.z)*180/Math.PI));setYHeadRot(getYRot());setYBodyRot(getYRot());
            setXRot((float)(-Math.atan2(target.getEyeY()-getEyeY(),Math.max(.01,delta.horizontalDistance()))*180/Math.PI));
            xxa=0;zza=delta.horizontalDistanceSqr()<.0025?0:1;setSpeed(.28f);jumping=jump||isInWater();
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
        if(chef.stasis>0){chef.halt();chef.stasis--;return;}
        if(game.items().actionsBlocked(chef)){chef.halt();return;}
        if(!game.items().wands().target(chef.owner,chef.target)){chef.halt();return;}
        if(!chef.level().hasChunkAt(chef.blockPosition())){chef.halt();return;}
        if(chef.owner.getRandom().nextFloat()<.005f)LbcOverload.sound(chef.owner,chef.position(),SOUNDS[chef.owner.getRandom().nextInt(SOUNDS.length)],2,1,0);
        if(chef.attackDelay>0)chef.attackDelay--;
        if(chef.distanceToSqr(chef.target)<=6.25&&chef.hasLineOfSight(chef.target)){
            chef.halt();chef.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_SWORD));
            if(chef.attackDelay==0){
                chef.attackDelay=20;chef.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
                var level=(ServerLevel)chef.level();
                var source=new DamageSource(level.damageSources().mobAttack(chef).typeHolder(),chef,chef.owner);
                var cause=game.items().indirectKills().coldCause(chef.owner,false);
                game.items().indirectKills().withCause(chef.target,cause,()->chef.target.hurtServer(level,source,8));
            }
            return;
        }
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
    private void remove(Chef chef){chefs.remove(chef);chef.brain.reset();if(!chef.isRemoved())chef.discard();}
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
    List<ItemStack> tools(Chef chef){return chef.brain.tools();}
    List<Chef> snapshot(){return List.copyOf(chefs);}
}
