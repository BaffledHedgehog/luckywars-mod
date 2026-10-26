package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ordinary Sauvojen Tuntija: one flying producer, with the source's nine payloads. */
public final class LbcSauvojen {
    public static final String DRONE="skywars2.sauvojen.flying";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    public enum Kind {
        RUBY(60),TERRA(20),ELECTRO(20),AIR(60),ACID(20),TELEPORT(80),STONE(60),FIRE(5),BLIND(10);
        final int cadence;
        Kind(int cadence){this.cadence=cadence;}
    }
    /** Native-check inspection is deliberately detached from input admission and random choice. */
    static final class Drone {
        final ServerPlayer owner;
        final InteractionHand hand;
        final ItemStack usedStack;
        final Kind kind;
        final String epoch;
        final Marker carrier;
        final ElementHolder holder;
        final ItemDisplayElement display;
        Vec3 position,direction=Vec3.directionFromRotation(0,0);
        int lifetime;
        Drone(ServerPlayer owner,InteractionHand hand,ItemStack stack,Kind kind,String epoch,Marker carrier) {
            this.owner=owner;this.hand=hand;usedStack=stack.copy();this.kind=kind;this.epoch=epoch;this.carrier=carrier;
            position=carrier.position();
            var model=new ItemStack(Items.CARROT_ON_A_STICK);
            model.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:st/"+kind.name().toLowerCase(java.util.Locale.ROOT)));
            display=new ItemDisplayElement(model);display.setItemDisplayContext(ItemDisplayContext.FIXED);
            display.setTransformation(new Matrix4f()
                    .setRow(0,new Vector4f(0,0,2.5f,0))
                    .setRow(1,new Vector4f(1.7678f,-1.7678f,0,0))
                    .setRow(2,new Vector4f(1.7678f,1.7678f,0,0))
                    .setRow(3,new Vector4f(0,0,0,1)));
            display.setInterpolationDuration(1);display.setTeleportDuration(1);
            display.setShadowRadius(0);display.setShadowStrength(0);
            holder=new ElementHolder();holder.addElement(display);
        }
    }
    private final GameController game;
    private final Map<Marker,Drone> drones=new LinkedHashMap<>();
    public LbcSauvojen(GameController game){this.game=game;}
    public int count(){return drones.size();}
    List<Drone> snapshot(){return List.copyOf(drones.values());}

    /** Called after the shared wand admission, mana payment and cast bonuses. */
    public void cast(ServerPlayer owner,InteractionHand hand,ItemStack stack){
        cast(owner,hand,stack,owner.getRandom().nextInt(9));
    }
    void cast(ServerPlayer owner,InteractionHand hand,ItemStack stack,int branch){
        if(branch<0||branch>=Kind.values().length)throw new IllegalArgumentException("Sauvojen branch must be 0..8");
        if(!validOwner(owner))return;
        Vec3 origin=owner.getEyePosition().add(owner.getLookAngle().scale(2));
        if(!owner.level().hasChunkAt(BlockPos.containing(origin)))return;
        var carrier=new Marker(EntityTypes.MARKER,owner.level());carrier.setPos(origin);
        carrier.entityTags().add(DRONE);carrier.entityTags().add("magic");
        game.items().markCurrentItemEpoch(carrier);
        var drone=new Drone(owner,hand,stack,Kind.values()[branch],game.items().epoch(),carrier);
        drones.put(carrier,drone);
        if(!owner.level().addFreshEntity(carrier)||carrier.isRemoved()||!drones.containsKey(carrier)){remove(drone,false);return;}
        var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/sauvojen_tuntija_wand"));
        var data=stack.get(DataComponents.CUSTOM_DATA);
        var flag=new net.minecraft.nbt.CompoundTag();flag.putInt("sauvojen_wand",1);
        if(advancement!=null&&(stack.is(LbcWands.SAUVOJEN)||data!=null&&data.matchedBy(flag)))
            owner.getAdvancements().award(advancement,"wand"+(branch+1));
        LbcOverload.sound(owner,owner.position(),"minecraft:block.enchantment_table.use",2,1,0);
        game.visualTracking().attach(drone.holder,owner.level(),()->drone.position);
        sync(drone);
    }
    public void tick(){
        for(var drone:snapshot()){
            if(!drones.containsKey(drone.carrier))continue;
            if(drone.carrier.isRemoved()||!validOwner(drone.owner)||drone.owner.level()!=drone.carrier.level()
                    ||!game.items().currentEpoch(drone.epoch)||!game.items().currentEpoch(drone.carrier)){
                remove(drone,false);continue;
            }
            var owner=drone.owner;
            if(game.items().wands().timeStopped()||owner.entityTags().contains("time_stopped"))continue;
            BlockPos cell=BlockPos.containing(drone.position);
            if(!owner.level().hasChunkAt(cell)){remove(drone,false);continue;}
            if(game.items().magic().suppressedAt(owner.level(),drone.position)){remove(drone,false);continue;}
            // raycastselector increments first; the helper changes Lifetime=1 to2 before checking modulo.
            if(++drone.lifetime==1)drone.lifetime=2;
            if(drone.lifetime>=200||!owner.level().getBlockState(cell).is(AIRS)){
                remove(drone,true);continue;
            }
            LivingEntity nearest=null;double best=150*150;
            for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(drone.position,drone.position).inflate(150),
                    candidate->game.items().lifetimes().target(owner,candidate)&&game.items().wands().target(owner,candidate))){
                double distance=target.position().distanceToSqr(drone.position);
                if(distance<=best){best=distance;nearest=target;}
            }
            if(nearest!=null){
                Vec3 aim=nearest.getEyePosition().subtract(drone.position);
                if(aim.lengthSqr()>1e-12)drone.direction=aim.normalize();
            }
            if(drone.lifetime%drone.kind.cadence==1)
                emit(owner,drone.hand,drone.usedStack,drone.kind,drone.position,drone.direction);
            if(!drones.containsKey(drone.carrier)||!validOwner(owner))continue;
            owner.level().sendParticles(ParticleTypes.WITCH,drone.position.x,drone.position.y,drone.position.z,2,.5,.5,.5,1);
            // Move away from the owner's eyes without changing the aimed firing rotation.
            Vec3 away=drone.position.subtract(owner.getEyePosition()).normalize().scale(.1);
            drone.position=drone.position.add(away);drone.carrier.setPos(drone.position);
            sync(drone);
        }
    }
    /** A producer API: exact payloads, no player movement, extra charge, or second admission. */
    void emit(ServerPlayer owner,InteractionHand hand,ItemStack stack,Kind kind,Vec3 origin,Vec3 direction){
        if(!validOwner(owner)||!owner.level().hasChunkAt(BlockPos.containing(origin)))return;
        switch(kind){
            case RUBY->game.items().ruby().flying(owner,origin,direction);
            case TERRA->game.items().terra().flying(owner,origin,direction);
            case ELECTRO->game.items().wands().libraryFlyingElectro(owner,origin,direction);
            case AIR->{
                game.items().air().flying(owner,hand,stack,origin,direction);
                LbcOverload.sound(owner,origin,"minecraft:entity.generic.explode",1,1.3f,0);
            }
            case ACID->game.items().clouds().flyingWandAcid(owner,origin,direction);
            case TELEPORT->game.items().teleport().flying(owner,hand,stack,origin,direction);
            case STONE->{
                game.items().stone().flying(owner,origin,direction);
                LbcOverload.sound(owner,origin,"minecraft:block.amethyst_cluster.break",1,1,0);
            }
            case FIRE->{
                var shot=new SmallFireball(owner.level(),owner,direction);
                game.items().wands().launchFire(owner,shot,origin,direction.normalize().scale(2.5));
                LbcOverload.sound(owner,origin,"minecraft:item.firecharge.use",1,1,0);
            }
            case BLIND->{
                game.items().blind().spawn(owner,origin,direction.normalize().scale(1.8),false,100,hand,false);
                LbcOverload.sound(owner,origin,"minecraft:entity.squid.squirt",1,1,0);
            }
        }
    }
    private void sync(Drone drone){
        drone.display.setYaw((float)Math.toDegrees(Math.atan2(-drone.direction.x,drone.direction.z)));
        drone.display.setPitch((float)-Math.toDegrees(Math.atan2(drone.direction.y,drone.direction.horizontalDistance())));
        var connected=game.server().getPlayerList().getPlayers();
        int range=EntityTypes.ITEM_DISPLAY.clientTrackingRange()*16;double rangeSquared=(double)range*range;
        for(var connection:List.copyOf(drone.holder.getWatchingPlayers())){
            var viewer=connection.player;
            if(viewer.connection!=connection||!connected.contains(viewer)||viewer.level()!=drone.owner.level()
                    ||viewer.position().distanceToSqr(drone.position)>=rangeSquared)drone.holder.stopWatching(connection);
        }
        for(var viewer:connected)if(viewer.level()==drone.owner.level()&&viewer.position().distanceToSqr(drone.position)<rangeSquared)
            drone.holder.startWatching(viewer);
        drone.holder.tick();
    }
    private boolean validOwner(ServerPlayer owner){
        return owner!=null&&!owner.isRemoved()&&owner.isAlive()&&owner.level()==game.maps().level()
                &&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&game.stage()!=GameController.Stage.ENDING;
    }
    public void loaded(Entity entity){
        if(entity.entityTags().contains(DRONE)&&(!(entity instanceof Marker marker)||!drones.containsKey(marker)))entity.discard();
    }
    public void unloaded(Entity entity){
        if(entity instanceof Marker marker){var drone=drones.get(marker);if(drone!=null)remove(drone,false);}
    }
    public void forget(ServerPlayer owner){for(var drone:snapshot())if(drone.owner==owner)remove(drone,false);}
    private void remove(Drone drone,boolean effects){
        if(drones.remove(drone.carrier)==null)return;
        drone.holder.destroy();
        if(effects){
            var level=(ServerLevel)drone.carrier.level();
            level.sendParticles(ParticleTypes.WITCH,drone.position.x,drone.position.y,drone.position.z,100,0,0,0,1);
            LbcOverload.sound(drone.owner,drone.position,"minecraft:block.enchantment_table.use",1,1,0);
        }
        if(!drone.carrier.isRemoved())drone.carrier.discard();
    }
    public void stop(){for(var drone:snapshot())remove(drone,false);}
}
