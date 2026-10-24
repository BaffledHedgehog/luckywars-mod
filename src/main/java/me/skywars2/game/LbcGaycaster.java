package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.List;

/** Ordinary source ray: local colour state, shared resident CPU queue and existing effect owners. */
final class LbcGaycaster {
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> FIRE_AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs_to_fire"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=
            TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String[] CRITERIA={"r","o","y","g","c","b","p"};
    private LbcGaycaster(){}
    static boolean target(GameController game,ServerPlayer owner,Entity entity){
        return entity instanceof LivingEntity&&entity.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,entity)
                &&!game.items().magic().suppressedAt(owner.level(),entity.position())
                &&!entity.entityTags().contains("nomagic_active");
    }
    static boolean use(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack stack){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        // Academy meditation and its upgraded palette are deliberately not advertised yet.
        var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(data.getIntOr("upgraded",0)==1||owner.isShiftKeyDown()&&data.getIntOr("upgradable",0)==1)return false;
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("gaycaster_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        Vec3 origin=owner.getEyePosition(),direction=owner.getLookAngle();
        LbcOverload.sound(owner,origin,"minecraft:lbcsounds.laser_mg_shot",1,0,0);
        LbcOverload.sound(owner,origin,"minecraft:item.trident.return",1,0,0);
        // Sample cursor belongs to this cast, including simultaneous hands and different casters.
        double[] next={0};
        game.items().exasperato().searchSpell(owner,origin,direction,125,false,(p,s)->!s.is(AIRS),
                e->target(game,owner,e),(from,to)->{
                    double end=origin.distanceTo(to);
                    while(next[0]<=end&&next[0]<125){
                        Vec3 point=origin.add(direction.scale(next[0]));
                        owner.level().sendParticles(dust(hue(next[0]),1.5f),true,false,point.x,point.y,point.z,1,0,0,0,0);next[0]+=.5;
                    }
                }).thenAccept(hit->{
                    if(hit==null)return;
                    Vec3 point=hit.getLocation();int hue=hue(origin.distanceTo(point));
                    if(hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity living)
                        impact(game,owner,point,direction,hue,living);
                    else impact(game,owner,point,direction,hue,null);
                });
        game.items().wands().wandCasted(owner);return true;
    }
    static int hue(double distance){return (-20+5*Mth.floor((distance+1e-6)*2))%360;}
    private static DustParticleOptions dust(int hue,float size){return new DustParticleOptions(Mth.hsvToRgb(Math.floorMod(hue,360)/360f,.75f,1),size);}
    /** Source boundary overlaps are retained: block red320..329 and blue/purple278. */
    static boolean band(int hue,int color,boolean block){
        return switch(color){case 0->hue>=0&&hue<=20||hue>=(block?320:330)&&hue<=359;
            case 1->hue>=21&&hue<=72;case 2->hue>=73&&hue<=124;case 3->hue>=125&&hue<=175;
            case 4->hue>=176&&hue<=226;case 5->hue>=227&&hue<=278;case 6->hue>=278&&hue<=329;default->false;};
    }
    static void impact(GameController game,ServerPlayer owner,Vec3 point,Vec3 direction,int hue,LivingEntity direct){
        if(game.items().magic().suppressedAt(owner.level(),point))return;
        var nearby=direct==null?owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(1.5),
                e->e.position().distanceToSqr(point)<=2.25&&target(game,owner,e)):
                target(game,owner,direct)?List.of(direct):List.<LivingEntity>of();
        boolean block=direct==null;
        for(int color=0;color<7;color++)if(band(hue,color,block)){
            boolean accepted=false;
            switch(color){
                case 0->{
                    boolean heal=false;
                    for(var e:nearby){boolean hit=e.hurtServer(owner.level(),damage(owner,DamageTypes.MAGIC),6);accepted=hit||accepted;heal=hit;}
                    // Preserve the source distinction pending the author's answer: entity healing is
                    // unconditional; the block helper overwrites its success with the last target.
                    if(!block&&!nearby.isEmpty())heal=true;
                    if(heal){owner.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH,1,1,false,false,false),owner);
                        particles(owner,ParticleTypes.HEART,owner.position(),5,.3,.5,.3,0);}
                    particles(owner,ParticleTypes.DAMAGE_INDICATOR,point,block?10:5,block?.5:.3,.5,block?.5:.3,0);
                }
                case 1->{
                    for(var e:nearby)accepted=game.items().kiuas().acceptCurse(e,new MobEffectInstance(MobEffects.RAID_OMEN,100,111,false,false,false),owner)||accepted;
                    if(block)game.items().indirectKills().trapChanges(owner.level(),owner,()->{
                        for(var p:BlockPos.betweenClosed(BlockPos.containing(point.add(-.5,-.5,-.5)),BlockPos.containing(point.add(.5,.5,.5))))
                            if(owner.level().hasChunkAt(p)&&owner.level().getBlockState(p).is(FIRE_AIRS))owner.level().setBlock(p,Blocks.FIRE.defaultBlockState(),3);
                    });
                    particles(owner,ParticleTypes.LAVA,point,block?10:100,0,0,0,.3);
                }
                case 2->{
                    for(var e:nearby)accepted=game.items().psycho().yellow(e,owner)||accepted;
                    particles(owner,new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:yellow_stained_glass"))))),point,40,0,0,0,.3);
                }
                case 3->{game.items().clouds().volleyAcid(owner,block?point.subtract(direction.scale(2)):point,new Vec3(0,1,0),6,false,false);accepted=!nearby.isEmpty();}
                case 4->{
                    if(block){Vec3 from=point.subtract(direction.scale(.5));
                        for(var e:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,from).inflate(2),
                                e->e.position().distanceToSqr(from)<=4&&target(game,owner,e))){game.items().wands().shortElectro(owner,from,e.getEyePosition(),false);accepted=true;}
                    }else if(!nearby.isEmpty()){
                        Vec3 from=owner.getEyePosition().subtract(direction);game.items().wands().shortElectro(owner,from,from.add(direction),false);accepted=true;
                    }
                    particles(owner,ParticleTypes.ELECTRIC_SPARK,point,20,block?.5:.3,.5,block?.5:.3,0);
                }
                case 5->{
                    for(var e:nearby){accepted=e.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,20,1),owner)||accepted;
                        LbcOverload.sound(owner,e.position(),"minecraft:lbcsounds.yametekudasai",block?2:3,1,0);}
                    if(block)particles(owner,ParticleTypes.SPLASH,point,30,.4,.4,.4,0);
                }
                case 6->{
                    for(var e:nearby)if(game.items().mana().hudValue(e,"void_resistance")<=0)
                        accepted=e.hurtServer(owner.level(),damage(owner,DamageTypes.FELL_OUT_OF_WORLD),5)||accepted;
                    if(block)game.items().indirectKills().trapChanges(owner.level(),owner,()->{
                        for(var p:BlockPos.betweenClosed(BlockPos.containing(point.add(-.5,-.5,-.5)),BlockPos.containing(point.add(.5,.5,.5))))
                            if(owner.level().hasChunkAt(p))owner.level().destroyBlock(p,true,owner);
                    });
                    particles(owner,ParticleTypes.SQUID_INK,point,block?30:20,block?.6:.3,block?.6:.4,block?.6:.3,0);
                }
            }
            if(!nearby.isEmpty()&&(color!=0||hue<320||hue>329)){var a=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/gaycaster"));
                if(a!=null)owner.getAdvancements().award(a,CRITERIA[color]);}
        }
        owner.level().sendParticles(dust(hue,2),true,false,point.x,point.y,point.z,20,.3,.3,.3,0);
    }
    private static DamageSource damage(ServerPlayer owner,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type){
        return new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),owner);
    }
    private static void particles(ServerPlayer owner,net.minecraft.core.particles.ParticleOptions particle,Vec3 point,int count,double x,double y,double z,double speed){
        owner.level().sendParticles(particle,true,false,point.x,point.y,point.z,count,x,y,z,speed);
    }
}
