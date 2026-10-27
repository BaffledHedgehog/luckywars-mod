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
        int academyCadence(){return switch(this){case RUBY,AIR->10;case TERRA->1;case STONE->40;default->5;};}
        int modes(){return switch(this){case RUBY,TERRA,ACID->10;case ELECTRO->6;case AIR->13;case TELEPORT->5;case STONE,BLIND->9;case FIRE->8;};}
        List<String> models(){return switch(this){
            case RUBY->List.of("st/ruby","consuming_wand","dublicating_wand");
            case TERRA->List.of("st/terra","terra_razorpine","terrasword_true","fevgalea_star");
            case ELECTRO->List.of("st/electro","erecto","electroblocker");
            case AIR->List.of("st/air","exasperato","homing");
            case ACID->List.of("st/acid","acidacidwand","acidcorrupt");
            case TELEPORT->List.of("st/teleport","boltbolt_wand","infbolt");
            case STONE->List.of("st/stone","stonekivi","bedrock_wand");
            case FIRE->List.of("st/fire","blazefire","the_sun");
            case BLIND->List.of("st/blind","psychoblind","blindvex");
        };}
    }
    /** Native-check inspection is deliberately detached from input admission and random choice. */
    static final class Drone {
        final ServerPlayer owner;
        final InteractionHand hand;
        final ItemStack usedStack;
        final Kind kind;
        final boolean academy;
        int nextMode;
        final String epoch;
        final Marker carrier;
        final ElementHolder holder;
        final ItemDisplayElement display;
        Vec3 position,direction=Vec3.directionFromRotation(0,0);
        int lifetime;
        Drone(ServerPlayer owner,InteractionHand hand,ItemStack stack,Kind kind,String epoch,Marker carrier) {
            this.owner=owner;this.hand=hand;usedStack=stack.copy();this.kind=kind;
            academy=upgraded(stack);this.epoch=epoch;this.carrier=carrier;
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
    private final SauvojenStars stars;
    private final SauvojenMeditation meditation;
    public LbcSauvojen(GameController game){this.game=game;meditation=new SauvojenMeditation(game);stars=new SauvojenStars(game);}
    public SauvojenMeditation meditation(){return meditation;}
    public SauvojenStars stars(){return stars;}
    static boolean upgraded(ItemStack stack){return stack.is(LbcWands.ACADEMY_SAUVOJEN)
            ||LbcFartus.data(stack).getIntOr("upgraded",0)==1;}
    public void prepare(){game.server().getStructureTemplateManager().get(SauvojenMeditation.STONE);}
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
        meditation.tick();stars.tick();
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
            if(drone.lifetime>=(drone.academy?400:200)||!drone.academy&&!owner.level().getBlockState(cell).is(AIRS)){
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
            if(drone.academy){
                if(nearest!=null&&(drone.kind==Kind.TERRA||drone.lifetime%drone.kind.academyCadence()==1)){
                    int mode=drone.nextMode;drone.nextMode=(mode+1)%drone.kind.modes();
                    emitAcademy(owner,drone.hand,drone.usedStack,drone.kind,mode,drone.position,drone.direction);
                }
                var models=drone.kind.models();
                var model=new ItemStack(Items.CARROT_ON_A_STICK);
                model.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:"+models.get(owner.getRandom().nextInt(models.size()))));
                drone.display.setItem(model);
            }else if(drone.lifetime%drone.kind.cadence==1)
                emit(owner,drone.hand,drone.usedStack,drone.kind,drone.position,drone.direction);
            if(!drones.containsKey(drone.carrier)||!validOwner(owner))continue;
            owner.level().sendParticles(ParticleTypes.WITCH,drone.position.x,drone.position.y,drone.position.z,2,.5,.5,.5,1);
            // Move away from the owner's eyes without changing the aimed firing rotation.
            Vec3 away=drone.position.subtract(owner.getEyePosition());
            if(drone.academy)away=new Vec3(away.x,0,away.z);
            away=away.normalize().scale(.1);
            drone.position=drone.position.add(away).add(0,drone.academy?.04:0,0);drone.carrier.setPos(drone.position);
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
    void emitAcademy(ServerPlayer owner,InteractionHand hand,ItemStack stack,Kind kind,int mode,Vec3 origin,Vec3 direction){
        if(mode<0||mode>=kind.modes())throw new IllegalArgumentException("Sauvojen "+kind+" mode "+mode);
        if(!validOwner(owner)||!owner.level().hasChunkAt(BlockPos.containing(origin)))return;
        switch(kind){
            case RUBY->{
                if(mode<=2)game.items().ruby().emitAt(owner,mode,origin,direction);
                else if(mode<=6)game.items().duplicating().emitAt(owner,switch(mode){case 4->4;case 5->3;case 6->2;default->1;},origin,direction);
                else{
                    game.items().consuming().emitAt(owner,hand,stack,switch(mode){case 7->4;case 8->3;default->2;},origin,direction);
                    if(mode==9){
                        owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.LEVITATION,60,30),owner);
                        owner.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.SLOW_FALLING,1200,0),owner);
                    }
                }
            }
            case TERRA->{
                if(mode>=5)stars.castAt(owner,hand,stack,mode,origin,direction);
                else game.items().terra().castAt(owner,origin,direction,switch(mode){
                    case 1->LbcTerra.Kind.ACADEMY;case 2->LbcTerra.Kind.TRUE_CHARGE;
                    case 3->LbcTerra.Kind.RAZOR;case 4->LbcTerra.Kind.TRUE_BEAM;default->LbcTerra.Kind.REGULAR;
                },switch(mode){case 1,2->20;case 3->10;case 4->50;default->200;});
            }
            case ELECTRO->{
                if(mode<=1)game.items().wands().sauvojenElectro(owner,origin,direction,mode==1);
                else game.items().erecto().emitAt(owner,hand,stack,switch(mode){case 2->1;case 3->2;case 4->4;default->3;},origin,direction);
            }
            case AIR->{
                if(mode<=3)game.items().air().emitAt(owner,hand,stack,mode,origin,direction);
                else if(mode<=7)game.items().homing().emitAt(owner,hand,stack,switch(mode){case 4->1;case 5->2;case 6->3;default->4;},origin,direction);
                else game.items().exasperato().emitAt(owner,hand,stack,mode-8,origin,direction);
            }
            case ACID->{
                if(mode==0)game.items().clouds().sauvojenAcid(owner,origin,direction);
                else if(mode<=5)game.items().awakenedAcid().emitAt(owner,hand,stack,switch(mode){case 2->2;case 4->3;case 5->4;default->1;},origin,direction);
                else game.items().acidCorruption().emitAt(owner,hand,stack,switch(mode){case 6->1;case 7->2;case 8->4;default->3;},origin,direction);
            }
            case TELEPORT->{
                if(mode<=1)game.items().teleport().emitAt(owner,hand,mode==0?0:2,origin,direction);
                else game.items().bolts().emitAt(owner,hand,stack,switch(mode){case 2->1;case 3->3;default->4;},origin,direction);
            }
            case STONE->{
                if(mode<=4)game.items().stone().emitAt(owner,mode,origin,direction);
                else game.items().stonekivi().emitAt(owner,hand,stack,switch(mode){case 5->1;case 6->2;case 7->4;default->3;},origin,direction);
            }
            case FIRE->{
                if(mode<=2)game.items().wands().sauvojenFire(owner,origin,direction,mode);
                else game.items().blaze().emitAt(owner,hand,stack,switch(mode){case 3->0;case 4->4;case 5->2;case 6->3;default->1;},origin,direction);
            }
            case BLIND->{
                if(mode<=2)game.items().blind().emitAt(owner,hand,mode,origin,direction);
                else if(mode==8)game.items().summons().sauvojenCultist(owner,origin,direction);
                else game.items().psycho().emitAt(owner,switch(mode){case 3->0;case 4->1;case 5->4;case 6->2;default->3;},origin,direction);
            }
        }
    }
    public void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform){
        stars.transformOrigins(level,transform);
        for(var drone:snapshot())if(drone.owner.level()==level){
            var point=transform.apply(drone.position);
            if(point==null)remove(drone,false);
            else{drone.position=point;drone.carrier.setPos(point);sync(drone);}
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
        meditation.loaded(entity);stars.loaded(entity);
        if(entity.entityTags().contains(DRONE)&&(!(entity instanceof Marker marker)||!drones.containsKey(marker)))entity.discard();
    }
    public void unloaded(Entity entity){
        meditation.unloaded(entity);stars.unloaded(entity);
        if(entity instanceof Marker marker){var drone=drones.get(marker);if(drone!=null)remove(drone,false);}
    }
    public void forget(ServerPlayer owner){meditation.forget(owner);stars.forget(owner);for(var drone:snapshot())if(drone.owner==owner)remove(drone,false);}
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
    public void stop(){meditation.stop();stars.stop();for(var drone:snapshot())remove(drone,false);}
}
