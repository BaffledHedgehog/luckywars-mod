package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import me.skywars2.mixin.WandProjectileMixin;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Permafrost's native snowball, paired crystals and Academy shatter. */
final class LbcIceWand {
    static final EntityType<IceShot> TYPE=registerType();
    static final float IMPACT_DAMAGE=5,CRYSTAL_DAMAGE=5,SHATTER_SPLASH_DAMAGE=10;
    static final float PLAYER_SHATTER_RATIO=.24f,MOB_SHATTER_RATIO=.4f;
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final Direction[] FACES={Direction.DOWN,Direction.UP,Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH};
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    private static final String SHOT_MODEL="lbc:ice_crystal_proj",CRYSTAL_MODEL="lbc:ice_crystal_small";
    static void register(){}
    private static EntityType<IceShot> registerType(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:ice_wand_projectile"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<IceShot>of(IceShot::new,MobCategory.MISC)
                .sized(.25f,.25f).clientTrackingRange(6).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);return type;
    }
    /** Adds only the native no-knockback tag, retaining FREEZE and its normal hurt cooldown. */
    static final class ImpactSource extends DamageSource {
        ImpactSource(Holder<DamageType> type,Entity direct,ServerPlayer owner){super(type,direct,owner);}
        @Override public boolean is(TagKey<DamageType> tag){return tag.equals(DamageTypeTags.NO_KNOCKBACK)||super.is(tag);}
    }
    /** The Academy selector is explicitly WILD; the shared damage guard recognizes this source. */
    static final class WildSource extends DamageSource {
        WildSource(Holder<DamageType> type,Entity direct,ServerPlayer owner){super(type,direct,owner);}
    }
    static final class IceShot extends Snowball implements PolymerEntity {
        LbcIceWand service;Display.ItemDisplay display;InteractionHand hand;ItemStack initiatingStack;int activeTicks;
        IceShot(EntityType<? extends IceShot> type,Level level){super(type,level);}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushedByFluid(){return false;}
        @Override public boolean isPushable(){return false;}
        @Override public void tick(){
            if(service==null||!service.valid(this)){if(service!=null)service.remove(this);else discard();return;}
            if(service.game.items().timeStop().frozen(this)||service.game.items().timeStop().stopped((ServerLevel)level()))return;
            if(LbcTerra.wallHit((ServerLevel)level(),position(),position().add(getDeltaMovement()),state->false)!=null){service.remove(this);return;}
            activeTicks++;
            super.tick();
            if(!isRemoved())service.sync(this);
        }
        // Do not call Snowball.onHit: it performs another native zero/Blaze damage hit.
        @Override protected void onHit(HitResult result){if(service!=null)service.impact(this,result);else discard();}
    }
    static final class Crystal {
        final ServerPlayer owner;final InteractionHand hand;final ItemStack initiatingStack;
        final Display.ItemDisplay display;final Interaction hitbox;final String epoch;final BlockKey block;
        int ticks;boolean removed;
        Crystal(ServerPlayer owner,InteractionHand hand,ItemStack stack,Display.ItemDisplay display,Interaction hitbox,String epoch,BlockKey block){
            this.owner=owner;this.hand=hand;this.initiatingStack=stack;this.display=display;this.hitbox=hitbox;this.epoch=epoch;this.block=block;
        }
        Vec3 center(){return display.position();}
    }
    private record BlockKey(ServerLevel level,BlockPos pos){}
    private static final class Barrier {
        final MapManager.BlockSnapshot before;final Set<Crystal> owners=new HashSet<>();
        Barrier(MapManager.BlockSnapshot before){this.before=before;}
    }
    private final GameController game;
    private final Set<IceShot> shots=new LinkedHashSet<>();
    private final Set<Crystal> crystals=new LinkedHashSet<>();
    private final Map<Entity,Crystal> actors=new IdentityHashMap<>();
    private final Map<BlockKey,Barrier> barriers=new HashMap<>();
    LbcIceWand(GameController game){this.game=game;}
    int count(){return shots.size()+crystals.size();}
    List<Crystal> snapshot(){return List.copyOf(crystals);}
    List<IceShot> shotSnapshot(){return List.copyOf(shots);}
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        boolean shift=owner.isShiftKeyDown();
        int cost=game.items().mana().score(ScoreHolder.forNameOnly(shift?"ice_wand_shift_mana_use":"ice_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        if(shift){
            for(var crystal:List.copyOf(crystals))if(crystal.owner==owner&&crystal.display.level()==owner.level()&&crystal.center().distanceToSqr(owner.position())<=2500)explode(crystal);
            if(academy)shatter(owner);
            particles(owner.level(),owner.position(),400,2);
            LbcOverload.sound(owner,owner.position(),"minecraft:lbcsounds.ice_wand_shatter",2,1,0);
        }else{
            spawn(owner,hand,stack);
            LbcOverload.sound(owner,owner.position(),"minecraft:lbcsounds.ice_wand",1,.7f,0);
        }
        game.items().wands().wandCasted(owner);return true;
    }
    IceShot spawn(ServerPlayer owner,InteractionHand hand,ItemStack stack){
        var shot=new IceShot(TYPE,owner.level());shot.service=this;shot.hand=hand;shot.initiatingStack=stack;shot.setOwner(owner);
        shot.setPos(owner.getEyePosition());shot.setDeltaMovement(owner.getLookAngle().scale(1.5));shot.setItem(model(SHOT_MODEL));
        shot.entityTags().addAll(List.of("magic","ice_projectile","skywars2.ice_shot"));game.items().markCurrentItemEpoch(shot);
        shot.display=display(owner,shot.position(),SHOT_MODEL,0,0,-.04f);
        shot.display.entityTags().add("ice_projectile");
        sync(shot);shots.add(shot);game.items().timeStop().prepareFrozen(shot);game.items().timeStop().prepareFrozen(shot.display);
        if(!owner.level().addFreshEntity(shot)||shot.isRemoved()||!owner.level().addFreshEntity(shot.display))remove(shot);
        return shot;
    }
    private static ItemStack model(String name){var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);stack.set(DataComponents.ITEM_MODEL,Identifier.parse(name));return stack;}
    private Display.ItemDisplay display(ServerPlayer owner,Vec3 at,String model,float yaw,float pitch,float offset){
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());
        try{
            var preset=TagParser.parseCompoundFully("{teleport_duration:1,shadow_radius:0f,shadow_strength:0f,transformation:{translation:[0f,"+offset+"f,0f]},item:{id:'minecraft:repeating_command_block',count:1,components:{'minecraft:item_model':'"+model+"'}}}");
            display.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),preset));
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new IllegalStateException(ex);}
        display.snapTo(at.x,at.y,at.z,yaw,pitch);display.entityTags().addAll(List.of("magic","skywars2.ice_display"));
        game.items().markCurrentItemEpoch(display);return display;
    }
    private void sync(IceShot shot){
        var motion=shot.getDeltaMovement();
        float yaw=(float)-Math.toDegrees(Math.atan2(motion.x,motion.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()));
        shot.setYRot(yaw);shot.setXRot(pitch);
        if(shot.display!=null)shot.display.snapTo(shot.getX(),shot.getY(),shot.getZ(),yaw,pitch);
        var at=shot.position();var level=(ServerLevel)shot.level();
        level.sendParticles(new DustParticleOptions(0x68b0eb,1),true,false,at.x,at.y,at.z,1,.1,.1,.1,0);
        level.sendParticles(ParticleTypes.SNOWFLAKE,true,false,at.x,at.y,at.z,1,.1,.1,.1,0);
    }
    private boolean valid(IceShot shot){
        return !shot.isRemoved()&&shot.getOwner() instanceof ServerPlayer owner&&validOwner(owner,shot.level())
                &&game.items().currentEpoch(shot)&&((ServerLevel)shot.level()).hasChunkAt(shot.blockPosition())
                &&!game.items().magic().suppressedAt((ServerLevel)shot.level(),shot.position())
                &&shot.display!=null&&!shot.display.isRemoved();
    }
    private boolean validOwner(ServerPlayer owner,Level level){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&owner.isAlive()
            &&!owner.isRemoved()&&game.canUseGameplayItems(owner)&&owner.level()==level&&level==game.maps().level();}
    private void impact(IceShot shot,HitResult result){
        var at=result.getLocation();
        var crystalAt=result instanceof net.minecraft.world.phys.BlockHitResult wall?at.add(wall.getDirection().getUnitVec3().scale(.01)):at;
        impact(shot,at,crystalAt);
    }
    void impact(IceShot shot,Vec3 at){impact(shot,at,at);}
    private void impact(IceShot shot,Vec3 at,Vec3 crystalAt){
        if(!shots.contains(shot)||!(shot.getOwner() instanceof ServerPlayer owner))return;
        if(valid(shot)&&((ServerLevel)shot.level()).hasChunkAt(BlockPos.containing(at))
                &&!game.items().magic().suppressedAt((ServerLevel)shot.level(),at)){
            var level=(ServerLevel)shot.level();
            var damage=new ImpactSource(holder(level,DamageTypes.FREEZE),shot,owner);
            for(var target:targets(level,at,3))if(target(owner,target)){
                target.hurtServer(level,damage,IMPACT_DAMAGE);cold(owner,shot.hand,shot.initiatingStack,target);
            }
            spawnCrystal(owner,shot.hand,shot.initiatingStack,crystalAt);
            particles(level,at,30,.1);LbcOverload.sound(owner,at,"minecraft:entity.player.hurt_freeze",1,0,0);
        }
        remove(shot);
    }
    private static Holder<DamageType> holder(ServerLevel level,ResourceKey<DamageType> type){return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type);}
    private List<LivingEntity> targets(ServerLevel level,Vec3 at,double radius){return level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),e->e.position().distanceToSqr(at)<=radius*radius&&game.items().wands().validTarget(e));}
    private boolean target(ServerPlayer owner,LivingEntity target){return game.items().wands().projectileTarget(owner,target)&&!game.items().magic().suppressedAt((ServerLevel)target.level(),target.position());}
    private void cold(ServerPlayer owner,InteractionHand hand,ItemStack stack,LivingEntity target){
        game.items().frost().iceHit(target,owner);
        if(target instanceof ServerPlayer&&target!=owner&&game.items().frost().isFrozen(target)
                &&owner.getItemInHand(hand)==stack&&LbcAcademy.trial(stack))game.items().academy().ready(owner,hand,stack);
    }
    /** Source face priority is deterministic, including its second-distance fallback. */
    Crystal spawnCrystal(ServerPlayer owner,InteractionHand hand,ItemStack stack,Vec3 impact){
        var level=owner.level();var start=BlockPos.containing(impact);BlockPos candidate=null;Direction face=null;
        for(int distance=1;distance<=2&&candidate==null;distance++)for(var direction:FACES){
            var cell=distance==1?start:start.relative(direction);
            var support=start.relative(direction,distance);
            if(level.hasChunkAt(cell)&&level.hasChunkAt(support)&&level.getBlockState(cell).is(AIRS)&&!level.getBlockState(support).is(AIRS)){
                candidate=cell;face=direction;break;
            }
        }
        if(candidate==null)return null;
        var at=Vec3.atCenterOf(candidate);
        if(game.items().magic().suppressedAt(level,at))return null;
        float yaw=switch(face){case EAST->90;case WEST->-90;case SOUTH->180;default->0;};
        float pitch=face==Direction.DOWN?0:face==Direction.UP?180:90;
        var display=display(owner,at,CRYSTAL_MODEL,yaw,pitch,0);display.entityTags().add("ice_crystal");
        var hitbox=new Interaction(EntityTypes.INTERACTION,level);hitbox.setWidth(1.2f);hitbox.setHeight(1.2f);
        hitbox.setPos(at.add(0,-.6,0));hitbox.entityTags().addAll(List.of("magic","ice_crystal","skywars2.ice_crystal","rc_able"));game.items().markCurrentItemEpoch(hitbox);
        var key=new BlockKey(level,candidate.immutable());var crystal=new Crystal(owner,hand,stack,display,hitbox,game.items().epoch(),key);
        crystals.add(crystal);actors.put(display,crystal);actors.put(hitbox,crystal);
        barriers.computeIfAbsent(key,k->new Barrier(game.maps().snapshot(level,k.pos))).owners.add(crystal);
        level.setBlock(candidate,Blocks.BARRIER.defaultBlockState(),FLAGS);
        game.items().timeStop().prepareFrozen(display);game.items().timeStop().prepareFrozen(hitbox);
        if(!level.addFreshEntity(display)||display.isRemoved()||!level.addFreshEntity(hitbox)||hitbox.isRemoved()){remove(crystal);return null;}
        return crystal;
    }
    boolean attack(ServerPlayer attacker,Entity target){
        var crystal=actors.get(target);if(crystal==null||target!=crystal.hitbox)return false;
        if(!game.items().actionsBlocked(attacker)&&!game.items().wands().inputTimeStopped(attacker))explode(crystal);return true;
    }
    void explode(Crystal crystal){
        if(crystal.removed||!crystals.contains(crystal))return;
        var level=crystal.block.level;var at=crystal.hitbox.position();var owner=crystal.owner;
        remove(crystal);
        if(!validOwner(owner,level)||!game.items().currentEpoch(crystal.epoch)||game.items().magic().suppressedAt(level,at))return;
        var damage=new DamageSource(holder(level,DamageTypes.ARROW),crystal.hitbox,owner);
        for(var target:targets(level,at,3))if(target(owner,target)){
            target.hurtServer(level,damage,CRYSTAL_DAMAGE);cold(owner,crystal.hand,crystal.initiatingStack,target);
        }
        particles(level,at,50,0);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(model(CRYSTAL_MODEL))),true,false,at.x,at.y+.5,at.z,200,0,0,0,.3);
        LbcOverload.sound(owner,at,"minecraft:block.glass.break",1,0,0);LbcOverload.sound(owner,at,"minecraft:block.deepslate.break",1,1,0);LbcOverload.sound(owner,at,"minecraft:entity.player.hurt_freeze",1,0,0);
        for(var nearby:crystals)if(nearby.display.level()==level&&nearby.hitbox.position().distanceToSqr(crystal.hitbox.position())<=9)nearby.ticks=Math.max(nearby.ticks,398);
    }
    void shatter(ServerPlayer owner){
        var level=owner.level();
        var victims=targets(level,owner.position(),50).stream().filter(e->game.items().frost().isFrozen(e)).toList();
        for(var victim:victims){
            float amount=victim.getHealth()*(victim instanceof ServerPlayer?PLAYER_SHATTER_RATIO:MOB_SHATTER_RATIO);
            var at=victim.position();game.items().frost().shatter(victim);
            var splash=new WildSource(holder(level,DamageTypes.FREEZE),owner,owner);
            for(var target:targets(level,at,4))if(target!=owner)target.hurtServer(level,splash,SHATTER_SPLASH_DAMAGE);
            victim.hurtServer(level,new WildSource(holder(level,DamageTypes.ARROW),owner,owner),amount);
            particles(level,at.add(0,.61,0),50,.5);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.ICE))),true,false,at.x,at.y+.61,at.z,100,0,0,0,.5);
        }
    }
    private static void particles(ServerLevel level,Vec3 at,int count,double speed){level.sendParticles(ParticleTypes.SNOWFLAKE,true,false,at.x,at.y,at.z,count,0,0,0,speed);}
    void tick(){
        for(var shot:List.copyOf(shots))if(!valid(shot))remove(shot);
        for(var crystal:List.copyOf(crystals)){
            var level=crystal.block.level;
            if(crystal.display.isRemoved()||crystal.hitbox.isRemoved()||!validOwner(crystal.owner,level)||!game.items().currentEpoch(crystal.epoch)
                    ||!level.hasChunkAt(crystal.block.pos)||game.items().magic().suppressedAt(level,crystal.center())){remove(crystal);continue;}
            if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(crystal.hitbox))continue;
            var center=crystal.hitbox.position().add(0,.6,0);
            boolean projectile=!level.getEntities((Entity)null,new AABB(center,center).inflate(1.5),e->e instanceof Projectile&&!(e instanceof IceShot)
                    &&!e.entityTags().contains("ice_projectile")&&!(e instanceof AbstractArrow arrow&&((WandProjectileMixin.ArrowAccess)arrow).skywars2$isInGround())&&e.position().distanceToSqr(center)<=2.25).isEmpty();
            if(projectile||crystal.hitbox.getLastAttacker()!=null||++crystal.ticks>=400){explode(crystal);continue;}
            particles(level,crystal.center(),1,0);
        }
    }
    private void remove(IceShot shot){shots.remove(shot);if(shot.display!=null)shot.display.discard();shot.discard();}
    private void remove(Crystal crystal){
        if(crystal.removed)return;crystal.removed=true;crystals.remove(crystal);actors.remove(crystal.display);actors.remove(crystal.hitbox);
        var barrier=barriers.get(crystal.block);
        if(barrier!=null){barrier.owners.remove(crystal);if(barrier.owners.isEmpty()){
            barriers.remove(crystal.block);
            if(crystal.block.level.hasChunkAt(crystal.block.pos)&&crystal.block.level.getBlockState(crystal.block.pos).is(Blocks.BARRIER))
                game.maps().restoreSnapshot(crystal.block.level,crystal.block.pos,barrier.before,FLAGS);
        }}
        crystal.display.discard();crystal.hitbox.discard();
    }
    void loaded(Entity entity){
        if(entity instanceof IceShot shot){if(shot.service==this&&game.items().currentEpoch(shot))shots.add(shot);else shot.discard();}
        else if(entity.entityTags().contains("skywars2.ice_crystal")&&!actors.containsKey(entity))entity.discard();
        else if(entity.entityTags().contains("skywars2.ice_display")&&!actors.containsKey(entity)&&shots.stream().noneMatch(s->s.display==entity))entity.discard();
    }
    void unloaded(Entity entity){if(entity instanceof IceShot shot)remove(shot);else {var crystal=actors.get(entity);if(crystal!=null)remove(crystal);else for(var shot:List.copyOf(shots))if(shot.display==entity)remove(shot);}}
    void forget(ServerPlayer owner){for(var shot:List.copyOf(shots))if(shot.getOwner()==owner)remove(shot);for(var crystal:List.copyOf(crystals))if(crystal.owner==owner)remove(crystal);}
    void stop(){for(var shot:List.copyOf(shots))remove(shot);for(var crystal:List.copyOf(crystals))remove(crystal);barriers.clear();}
}
