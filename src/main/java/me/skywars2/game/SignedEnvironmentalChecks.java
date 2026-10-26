package me.skywars2.game;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.cubemob.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Root suite entry: native effect guards/damage/air, plus full signed endpoint math. */
public final class SignedEnvironmentalChecks {
    private SignedEnvironmentalChecks() {}
    private static void require(boolean ok,String detail){if(!ok)throw new IllegalStateException("Signed environmental: "+detail);}
    public static String verify(GameController game) {
        try{return verifyNative(game);}catch(ReflectiveOperationException exception){throw new IllegalStateException("Signed environmental native reflection",exception);}
    }
    private static String verifyNative(GameController game) throws ReflectiveOperationException {
        require(SignedEnvironmentalEffects.level(Integer.MIN_VALUE)==-2147483647L,"minimum amplifier long level");
        require(SignedEnvironmentalEffects.level(Integer.MAX_VALUE)==2147483648L,"maximum amplifier long level");
        require(SignedEnvironmentalEffects.infestationCount(Integer.MAX_VALUE,0)==214748365L,"exact guaranteed/fractional infestation count");
        require(SignedEnvironmentalEffects.infestationCount(-1,0)==0,"zero infestation inert");
        require(SignedEnvironmentalEffects.radius(2147483648L,1,15)==15,"explicit block radius cap");
        require(SignedEnvironmentalEffects.omenAmplifier(Integer.MAX_VALUE)==4,"omen mechanics cap V");
        require(SignedEnvironmentalEffects.drainAir(300,2147483648L)==-20,"large air loss reaches native drown boundary");
        var level=game.maps().level();var mob=EntityTypes.COW.create(level,EntitySpawnReason.TRIGGERED);
        require(mob!=null,"native cow creation");
        try {
            mob.setNoAi(true);mob.setHealth(mob.getMaxHealth());
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100,-2));
            float before=mob.getHealth();boolean accepted=mob.hurtServer(level,level.damageSources().onFire(),2);
            require(accepted&&Math.abs((before-mob.getHealth())-2.4)<.001,"negative fire resistance amplifies actual fire damage");
            mob.removeEffect(MobEffects.FIRE_RESISTANCE);mob.setInvulnerableTime(0);mob.damageCooldownTime=0;
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100,0));
            require(!mob.hurtServer(level,level.damageSources().onFire(),2),"positive native fire immunity");
            mob.removeEffect(MobEffects.FIRE_RESISTANCE);
            for(var effect:java.util.List.of(MobEffects.WATER_BREATHING,MobEffects.CONDUIT_POWER,MobEffects.BREATH_OF_THE_NAUTILUS)) {
                mob.addEffect(new MobEffectInstance(effect,100,-2));require(!MobEffectUtil.hasWaterBreathing(mob),"negative breathing guard "+effect);
                var decrease=LivingEntity.class.getDeclaredMethod("decreaseAirSupply",int.class);decrease.setAccessible(true);
                require((int)decrease.invoke(mob,300)==298,"negative effect adds native air loss "+effect);
                var oxygen=mob.getAttribute(Attributes.OXYGEN_BONUS);
                if(oxygen!=null){double old=oxygen.getBaseValue();try{oxygen.setBaseValue(-2);require((int)decrease.invoke(mob,300)==296,"signed effect composes with negative oxygen attribute");}finally{oxygen.setBaseValue(old);}}
                mob.removeEffect(effect);mob.addEffect(new MobEffectInstance(effect,100,0));
                require(MobEffectUtil.hasWaterBreathing(mob)&&MobEffectUtil.shouldEffectsRefillAirsupply(mob),"positive breathing/refill "+effect);
                mob.removeEffect(effect);
            }
            mob.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,100,2));mob.setAirSupply(300);
            SignedEnvironmentalEffects.airTick(mob);require(mob.getAirSupply()==299,"water breathing III outside one air/tick");
            require(mob.getEffect(MobEffects.WATER_BREATHING).getAmplifier()==2,"stored amplifier retained");
            deathAndInfestation(game);
            return "native signed fire/air, death wind, weaving blocks, ooze offspring and infestation callbacks passed";
        } finally {mob.discard();}
    }
    private static Cow cow(ServerLevel level,Vec3 at,boolean add) {
        var mob=EntityTypes.COW.create(level,EntitySpawnReason.TRIGGERED);require(mob!=null,"native fixture cow");
        mob.setNoAi(true);mob.setNoGravity(true);mob.snapTo(at.x,at.y,at.z,0,0);
        if(add)require(level.addFreshEntity(mob),"fixture cow resident");return mob;
    }
    private static void killed(Cow source,Holder<MobEffect> effect,int amplifier) {
        source.addEffect(new MobEffectInstance(effect,200,amplifier));
        source.setHealth(0);source.remove(Entity.RemovalReason.KILLED);
        require(source.isRemoved(),"actual native KILLED removal completed");
    }
    private static void deathAndInfestation(GameController game) {
        var level=game.maps().level();require(!level.players().isEmpty(),"resident player for isolated test chunk");
        var player=level.players().getFirst();
        var at=new Vec3(player.getX(),Math.min(level.getMaxY()-10,player.getY()+70),player.getZ());
        var center=BlockPos.containing(at);at=Vec3.atCenterOf(center);
        var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
        var velocities=new IdentityHashMap<Entity,Vec3>();for(var e:before)velocities.put(e,e.getDeltaMovement());
        var blocks=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++) {
            var pos=center.offset(x,y,z);require(level.hasChunkAt(pos),"fixture block chunk resident");blocks.put(pos,level.getBlockState(pos));level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        try {
            var target=cow(level,at.add(2,0,0),true);
            var source=cow(level,at,false);killed(source,MobEffects.WIND_CHARGED,0);
            require(target.getDeltaMovement().x>0,"positive native wind death pushes resident target");
            target.setDeltaMovement(Vec3.ZERO);source=cow(level,at,false);killed(source,MobEffects.WIND_CHARGED,-2);
            require(target.getDeltaMovement().x<0,"negative native wind death pulls resident target");target.discard();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setBlock(center.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            source=cow(level,at,false);killed(source,MobEffects.WEAVING,0);SignedEnvironmentalEffects.tick();
            int webs=0;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(level.getBlockState(center.offset(x,y,z)).is(Blocks.COBWEB))webs++;
            require(webs==2,"native positive weaving death writes two real cobwebs");
            source=cow(level,at,false);killed(source,MobEffects.WEAVING,-2);SignedEnvironmentalEffects.tick();
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)require(!level.getBlockState(center.offset(x,y,z)).is(Blocks.COBWEB),"negative weaving death removes actual webs");
            for(int amplifier:new int[]{2,-4}) {
                var old=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(old::add);
                source=cow(level,at,false);killed(source,MobEffects.OOZING,amplifier);
                var offspring=new ArrayList<AbstractCubeMob>();for(var entity:level.getAllEntities())if(!old.contains(entity)&&entity instanceof AbstractCubeMob cube)offspring.add(cube);
                require(offspring.size()==2,"actual native ooze death makes two offspring");
                for(var cube:offspring){require(amplifier<0?cube instanceof MagmaCube:cube instanceof Slime,"signed ooze native offspring type");require(cube.getSize()==3,"native ooze magnitude-three size");cube.discard();}
            }
            require(SignedEnvironmentalEffects.queuedSpawns()==0,"empty infestation queue before fixture");
            source=cow(level,at,false);source.getRandom().setSeed(734231L);source.addEffect(new MobEffectInstance(MobEffects.INFESTED,200,0));
            boolean one=false,two=false;
            for(int i=0;i<200&&(!one||!two);i++) {
                source.setHealth(source.getMaxHealth());source.setInvulnerableTime(0);source.damageCooldownTime=0;
                long prior=SignedEnvironmentalEffects.queuedSpawns();source.hurtServer(level,level.damageSources().generic(),1);
                long count=SignedEnvironmentalEffects.queuedSpawns()-prior;require(count>=0&&count<=2,"native infestation I bounded1–2 fractional result");one|=count==1;two|=count==2;
            }
            require(one&&two,"native infestation I preserves both1 and2 mob outcomes");SignedEnvironmentalEffects.tick();source.discard();
            for(int amplifier:new int[]{19,-21}) {
                var old=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(old::add);
                source=cow(level,at,false);source.addEffect(new MobEffectInstance(MobEffects.INFESTED,200,amplifier));source.hurtServer(level,level.damageSources().generic(),1);
                require(SignedEnvironmentalEffects.queuedSpawns()==2,"each full100% infestation grants exact guaranteed extra mob");SignedEnvironmentalEffects.tick();
                int count=0;for(var entity:level.getAllEntities())if(!old.contains(entity)){require(entity.getType()==(amplifier<0?EntityTypes.ENDERMITE:EntityTypes.SILVERFISH),"signed actual infestation offspring type");count++;}
                require(count==2,"two guaranteed native infestation offspring admitted");source.discard();
            }
            // Native BaseFireBlock.onPlace immediately removes unsupported fire, including /fill-style writes.
            // Give the radial assertion a real nonflammable support while keeping the outer corner empty.
            level.setBlock(center.offset(2,-1,0),Blocks.STONE.defaultBlockState(),2);
            level.setBlock(center.offset(2,1,2),Blocks.STONE.defaultBlockState(),2);
            source=cow(level,at,false);source.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,200,3));source.baseTick();
            require(level.getBlockState(center.offset(2,0,0)).is(Blocks.FIRE),"native fire IV sphere reaches radial boundary");
            require(level.getBlockState(center.offset(2,2,2)).isAir(),"extended fire fill excludes cube corner outside sphere");source.discard();
            for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)level.setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
            source=cow(level,at,false);source.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,200,3));
            game.items().magic().colors().tickWater();
            require(level.getBlockState(center).is(Blocks.WATER),"enhanced conduit nearby water remains immediate");
            SignedEnvironmentalEffects.tick();
            require(level.getBlockState(center.offset(2,0,0)).is(Blocks.WATER),"native indexed conduit reaches spherical boundary through bounded queue");
            require(level.getBlockState(center.offset(2,2,2)).isAir(),"enhanced conduit excludes off-sphere cube corner");source.discard();
        } finally {
            var added=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity))added.add(entity);added.forEach(Entity::discard);
            for(var item:blocks.entrySet())level.setBlock(item.getKey(),item.getValue(),2);
            for(var item:velocities.entrySet())if(!item.getKey().isRemoved())item.getKey().setDeltaMovement(item.getValue());
        }
    }
}
