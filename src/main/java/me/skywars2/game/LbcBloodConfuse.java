package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.List;
import java.util.Set;

/** Instant area spells; native effects own their duration, cleansing and causal damage. */
public final class LbcBloodConfuse {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    LbcBloodConfuse(GameController game){this.game=game;}

    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean blood,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||owner.entityTags().contains("time_stopped")
                ||game.items().timeStop().stopped(owner.level())||game.items().magic().suppressed(owner)
                ||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Math.max(1,Math.min(5,data.getIntOr("mode",1)));
        if(!blood&&academy&&owner.isShiftKeyDown()){
            mode=LbcModeScroll.next(mode,5);data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();
            owner.sendSystemMessage(mode==5?Component.translatableWithFallback("mode_alternating","Чередующийся")
                    :Component.translatable(switch(mode){case 1->"mode_invisibility";case 2->"mode_teleport";case 3->"mode_speed";default->"mode_levitate";}));return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly(blood?"blood_wand_mana_use":"confuse_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        boolean alternating=!blood&&academy&&mode==5;
        int action=alternating?Math.max(1,Math.min(4,data.getIntOr("alternating_next",1))):mode;
        if(blood)blood(owner,hand,stack,academy);else confuse(owner,hand,stack,academy,academy?action:owner.getRandom().nextInt(4)+1);
        if(alternating){
            var updated=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            updated.putInt("alternating_next",action%4+1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(updated));owner.getInventory().setChanged();
        }
        game.items().wands().wandCasted(owner);return true;
    }
    private List<LivingEntity> nearby(ServerPlayer owner,Vec3 center,double radius){
        return owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),
                e->e.position().distanceToSqr(center)<=radius*radius&&game.items().wands().target(owner,e)
                        &&!game.items().magic().suppressedAt(owner.level(),e.position()));
    }
    private void effect(LivingEntity target,ServerPlayer owner,Holder<MobEffect> type,int ticks,int amplifier,boolean visible){
        target.addEffect(ItemEffectLevels.create(type,ticks,amplifier,false,visible),owner);
    }
    private void particles(ServerPlayer owner,Vec3 at,ParticleOptions type,int count,double dx,double dy,double dz,double speed){
        owner.level().sendParticles(type,true,false,at.x,at.y+1,at.z,count,dx,dy,dz,speed);
    }
    void blood(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        Vec3 at=owner.position();
        LbcOverload.sound(owner,at,"minecraft:entity.ender_dragon.growl",1,2,0);
        particles(owner,at,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.REDSTONE_BLOCK.defaultBlockState()),1500,8,8,8,0);
        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),owner);
        for(var target:nearby(owner,at,10)){
            if(target.position().distanceToSqr(at)<.0001)continue;
            if(target.getType().builtInRegistryHolder().is(MOBS))target.hurtServer(owner.level(),source,18);
            if(!target.isAlive())continue;
            if(target instanceof ServerPlayer)effect(target,owner,MobEffects.HUNGER,400,255,true);
            effect(target,owner,MobEffects.WITHER,600,1,true);
            if(target instanceof ServerPlayer p)p.sendSystemMessage(Component.translatable("player_blooding").withStyle(ChatFormatting.DARK_RED));
        }
        if(!academy){
            // EffectCommands uses one tick for Instant Damage, seconds for the other effects.
            effect(owner,owner,MobEffects.INSTANT_DAMAGE,1,1,true);
            effect(owner,owner,MobEffects.WITHER,300,1,true);
            effect(owner,owner,MobEffects.POISON,300,1,true);
            effect(owner,owner,MobEffects.HUNGER,40,255,true);
            var resistance=owner.getEffect(MobEffects.RESISTANCE);
            if(resistance!=null&&resistance.getAmplifier()==4)game.items().mana().award(owner,"lbc:true_advancements/staffs/blood");
            if(owner.isShiftKeyDown()&&LbcAcademy.trial(stack)){
                effect(owner,owner,MobEffects.POISON,MobEffectInstance.INFINITE_DURATION,4,true);
                effect(owner,owner,MobEffects.WITHER,MobEffectInstance.INFINITE_DURATION,4,true);
                game.items().academy().ready(owner,hand,stack);
            }
        }
    }
    void confuse(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy,int mode){
        Vec3 at=owner.position();int before=Mth.floor(at.y);
        switch(mode){
            case 1->{effect(owner,owner,MobEffects.INVISIBILITY,200,0,false);
                LbcOverload.sound(owner,at,"minecraft:entity.illusioner.prepare_mirror",1,1,0);
                particles(owner,at,ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,100,1,1,1,.075);}
            case 2->{
                for(var target:nearby(owner,at,10))if(target.position().distanceToSqr(at)>=.01){
                    effect(target,owner,MobEffects.SLOWNESS,60,50,false);
                    if(target instanceof ServerPlayer p)p.sendSystemMessage(academy?Component.translatable("player_stopped").withStyle(ChatFormatting.AQUA)
                            :Component.translatable("player_teleported").withStyle(ChatFormatting.LIGHT_PURPLE).append(" ").append(Component.translatable("player_stopped").withStyle(ChatFormatting.AQUA)));
                }
                LbcOverload.sound(owner,at,"minecraft:entity.illusioner.mirror_move",1,0,0);
                particles(owner,at,ParticleTypes.LARGE_SMOKE,80,1,1,1,.075);
                Vec3 destination=spreadDestination(owner.level(),at,owner.getRandom());
                if(destination!=null)owner.teleportTo(owner.level(),destination.x,destination.y,destination.z,Set.of(),owner.getYRot(),owner.getXRot(),true);
                // A function's execution origin does not move when its executor teleports.
                particles(owner,at,ParticleTypes.LARGE_SMOKE,80,1,1,1,.075);
            }
            case 3->{effect(owner,owner,MobEffects.SPEED,200,3,false);effect(owner,owner,MobEffects.JUMP_BOOST,200,3,false);
                particles(owner,at,ParticleTypes.END_ROD,30,1,1,1,.05);LbcOverload.sound(owner,at,"minecraft:entity.illusioner.cast_spell",1,0,0);}
            case 4->{
                for(var target:nearby(owner,at,10))if(target.position().distanceToSqr(at)>=.0001&&(!academy||target instanceof ServerPlayer)){
                    effect(target,owner,MobEffects.BLINDNESS,120,0,false);
                    if(target instanceof ServerPlayer p)p.sendSystemMessage(Component.translatable("player_blinded").withStyle(ChatFormatting.GRAY));
                }
                effect(owner,owner,MobEffects.LEVITATION,20,10,false);effect(owner,owner,MobEffects.SLOW_FALLING,160,0,false);
                LbcOverload.sound(owner,at,"minecraft:entity.illusioner.prepare_blindness",1,1,0);
                particles(owner,at,ParticleTypes.POOF,1000,5,1,5,0);
            }
            default->throw new IllegalArgumentException("Confuse mode "+mode);
        }
        if(Mth.floor(owner.getY())-before>=50)game.items().mana().award(owner,"lbc:true_advancements/staffs/confuse");
        if(mode==2&&LbcAcademy.trial(stack)&&owner.hasEffect(MobEffects.SLOW_FALLING)&&owner.hasEffect(MobEffects.SPEED)
                &&owner.hasEffect(MobEffects.JUMP_BOOST)&&owner.hasEffect(MobEffects.INVISIBILITY)
                &&owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(15),e->e!=owner
                    &&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validTarget(e)&&e.position().distanceToSqr(at)>=.0001
                    &&e.position().distanceToSqr(at)<=225&&e.hasEffect(MobEffects.BLINDNESS)&&e.hasEffect(MobEffects.SLOWNESS)).size()>0)
            game.items().academy().ready(owner,hand,stack);
    }
    /** Conditional distribution of vanilla's single-target spread, without repeated void scans/chunk loads. */
    static Vec3 spreadDestination(ServerLevel level,Vec3 center,net.minecraft.util.RandomSource random){
        int max=level.dimension()==Level.NETHER?120:level.getMaxY()+1;
        double weight=0;Vec3 chosen=null;
        for(int x=Mth.floor(center.x-10);x<Math.ceil(center.x+10);x++)for(int z=Mth.floor(center.z-10);z<Math.ceil(center.z+10);z++){
            var column=new BlockPos(x,0,z);if(!level.hasChunkAt(column))continue;
            int surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
            if(surface<=level.getMinY())continue;
            int y=LbcPortalDrops.spawnY(level,column,Math.min(max,surface+1));
            var below=new BlockPos(x,y-1,z);var state=level.getBlockState(below);
            if(below.getY()>=max||state.liquid()||!state.is(BlockTags.ENTITIES_CAN_TELEPORT_TO))continue;
            double part=(Math.min(x+1,center.x+10)-Math.max(x,center.x-10))*(Math.min(z+1,center.z+10)-Math.max(z,center.z-10));
            weight+=part;if(random.nextDouble()*weight<part)chosen=new Vec3(x+.5,y,z+.5);
        }
        return chosen;
    }
}
