package me.skywars2.game;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Shared armor admission, source scoreboard counters and native technical abilities. */
public final class LbcTechnoArmor {
    static final String REACTIVE_LIFT="reactive-armor-lift", REACTIVE_LANDING="reactive-armor-landing";
    static final String SHIELD="technomk2-shield";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private final GameController game;
    private final Predicate<ServerPlayer> eligible,admitted;
    private final LbcTechnoFlight flight;
    private final Map<ServerPlayer,Integer> phases=new IdentityHashMap<>(),ticks=new IdentityHashMap<>();
    private final Set<LivingEntity> living=Collections.newSetFromMap(new IdentityHashMap<>());
    private record Attempt(ServerPlayer player,boolean admitted){}
    private final Deque<Attempt> attempts=new ArrayDeque<>();

    LbcTechnoArmor(GameController game,Predicate<ServerPlayer> eligible,Predicate<ServerPlayer> admitted){
        this.game=game;this.eligible=eligible;this.admitted=admitted;flight=new LbcTechnoFlight(game,eligible,admitted);
    }
    public LbcTechnoFlight flight(){return flight;}
    static boolean piece(ServerPlayer p,EquipmentSlot slot,String key){return !p.getItemBySlot(slot).isEmpty()&&LbcFartus.data(p.getItemBySlot(slot)).getIntOr(key,0)==1;}
    static int count(ServerPlayer p,String key){int n=0;for(var slot:SLOTS)if(piece(p,slot,key))n++;return n;}
    static boolean ordinarySet(ServerPlayer p){return count(p,"techno")==4&&count(p,"technomk2")!=4;}
    private boolean technical(ServerPlayer p){return !game.items().magic().mechanismSuppressed(p);}
    private int read(ServerPlayer p,String key){return game.items().mana().hudValue(p,key);}
    private void write(ServerPlayer p,String key,int value){game.items().mana().score(p,key).set(value);}
    private void add(ServerPlayer p,String key,int value){game.items().mana().score(p,key).add(value);}

    public void tickPlayer(ServerPlayer p){
        boolean active=eligible.test(p),running=active&&admitted.test(p);
        // Cleanup precedes time admission so a jammer also stops already-owned flight.
        cleanupReactive(p);
        flight.tick(p);
        if(!active){phases.remove(p);ticks.remove(p);return;}
        if(!running)return;
        int phase=(phases.getOrDefault(p,0)+1)%20;phases.put(p,phase);
        int elapsed=ticks.getOrDefault(p,0)+1;ticks.put(p,elapsed);
        if(elapsed%200==0&&read(p,"nitro")<4)add(p,"nitro",1);
        pulse(p,phase);
    }
    /** The same source two/five/ten/twenty-tick phases, with independent player pause admission. */
    void pulse(ServerPlayer p,int phase){
        if(!admitted.test(p))return;
        // Author: this is a physical air filter and neither jammer disables cleansing.
        if(phase%2==0&&piece(p,EquipmentSlot.HEAD,"reactive")){
            p.removeEffect(MobEffects.BLINDNESS);p.removeEffect(MobEffects.DARKNESS);p.removeEffect(MobEffects.NAUSEA);
        }
        if(phase==0&&read(p,"coldown8")>0)add(p,"coldown8",-1);
        if(!technical(p)){
            cleanupReactive(p);return;
        }
        if(piece(p,EquipmentSlot.HEAD,"techno"))helmet(p);
        if(phase%2==0&&piece(p,EquipmentSlot.FEET,"reactive")&&!piece(p,EquipmentSlot.FEET,"technomk2"))reactive(p);
        if(phase%5==0&&piece(p,EquipmentSlot.FEET,"techno"))p.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.getX(),p.getY(),p.getZ(),2,.3,.1,.3,.01);
        if(phase%10==0&&ordinarySet(p)&&p.level().getBlockState(p.blockPosition()).is(Blocks.WATER))water(p);
        if(phase==0){
            if(piece(p,EquipmentSlot.HEAD,"technomk2"))p.addEffect(ItemEffectLevels.create(MobEffects.NIGHT_VISION,300,0,false,false),p);
            if(piece(p,EquipmentSlot.HEAD,"techno"))directions(p);
        }
        if(count(p,"technomk2")==4){
            if(read(p,"technomk2shield")>0)add(p,"technomk2shield",-1);
            if(read(p,"technomk2shield")==1)sound(p,"minecraft:block.beacon.activate",2,.7f);
            if(read(p,"technomk2shield")==0){
                dust(p,0xffa42e,1,1,.3,.4,.3);
                game.items().ownedEffects().grant(p,SHIELD,ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),p);
            }
        }
    }
    private void reactive(ServerPlayer p){
        if(read(p,"coldownshish")<=20)game.items().ownedEffects().withdraw(p,REACTIVE_LIFT);
        if(!p.onGround()&&p.isShiftKeyDown()){
            game.items().ownedEffects().grant(p,REACTIVE_LANDING,ItemEffectLevels.create(MobEffects.SLOW_FALLING,200,0),p);
            p.level().sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY()-.2,p.getZ(),2,.2,.02,.2,.03);
            if(read(p,"reactivefuel")>0){
                sound(p,"minecraft:block.fire.extinguish",1,2);
                p.level().sendParticles(ParticleTypes.FLAME,true,false,p.getX(),p.getY()-.2,p.getZ(),100,.2,.1,.2,.1);
                game.items().ownedEffects().grant(p,REACTIVE_LIFT,ItemEffectLevels.create(MobEffects.LEVITATION,20,count(p,"reactive")==4?30:15,false,false),p);
                add(p,"reactivefuel",-1);
            }
        }
        if(p.onGround()){
            if(read(p,"reactivefuel")<=9)add(p,"reactivefuel",1);
            if(piece(p,EquipmentSlot.LEGS,"reactive")&&read(p,"reactivefuel")<=9)add(p,"reactivefuel",1);
            if(piece(p,EquipmentSlot.CHEST,"reactive")&&read(p,"reactivefuel")>=10&&read(p,"reactivefuel")<=19)add(p,"reactivefuel",1);
            if(piece(p,EquipmentSlot.LEGS,"reactive")&&piece(p,EquipmentSlot.CHEST,"reactive")&&read(p,"reactivefuel")>=10&&read(p,"reactivefuel")<=19)add(p,"reactivefuel",1);
        }
    }
    private void helmet(ServerPlayer p){
        living.removeIf(Entity::isRemoved);
        var visible=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());visible.addAll(living);visible.addAll(game.server().getPlayerList().getPlayers());
        for(var mob:visible){
            if(mob==p||mob.level()!=p.level()||!mob.isAlive()||mob.isSpectator()||mob.entityTags().contains("spectator")||!mob.getType().builtInRegistryHolder().is(MOBS))continue;
            if(mob instanceof ServerPlayer viewer&&(viewer.distanceToSqr(p)<.0001||game.items().magic().mechanismSuppressed(viewer)))continue;
            var particle=ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,mob instanceof ServerPlayer?0xff000200:0xff000100);
            p.level().sendParticles(p,particle,true,false,mob.getX(),mob.getY(),mob.getZ(),1,0,0,0,0);
        }
    }
    private void directions(ServerPlayer p){
        for(var other:p.level().players()){
            if(other==p||other.isSpectator()||other.distanceToSqr(p)<1||game.items().magic().mechanismSuppressed(other))continue;
            var at=p.position().add(0,3,0);var direction=other.getEyePosition().subtract(at).normalize();
            // Source's nearest viewer is the wearer under normal admission; keep this private.
            directionParticles(p,at,direction,ParticleTypes.FLAME);
        }
    }
    /** The helmet and All-Seeing Eye share the exact eight-point directional glyph. */
    static void directionParticles(ServerPlayer viewer,Vec3 origin,Vec3 direction,ParticleOptions particle){
        for(double offset:new double[]{.1,.5,.9,1.3,1.7,2.1,2.6,3.2}){
            var point=origin.add(direction.scale(offset));
            viewer.level().sendParticles(viewer,particle,false,false,point.x,point.y,point.z,1,0,0,0,0);
        }
    }
    static final class SelfWaterDamage extends DamageSource{
        SelfWaterDamage(ServerPlayer p){super(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRIC),p,p);}
    }
    static boolean selfShortCircuit(LivingEntity victim,DamageSource damage){
        var original=LbcIndirectKills.originalDamage(damage);return original instanceof SelfWaterDamage&&original.getEntity()==victim;
    }
    void water(ServerPlayer p){
        var kills=game.items().indirectKills();var cause=kills.wildCause(p);
        for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),e->
                e.getType().builtInRegistryHolder().is(MOBS)&&e.distanceToSqr(p)<=100&&game.items().wands().validRoleTarget(e,false)
                        &&p.level().getBlockState(e.blockPosition()).is(Blocks.WATER))){
            var nativeDamage=target==p?new SelfWaterDamage(p):p.damageSources().source(LbcDamage.ELECTRIC,p,p);
            target.hurtServer(p.level(),kills.captureDamage(nativeDamage,cause),6);
            p.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,target.getX(),target.getY()+1,target.getZ(),10,.5,1,.5,0);
            LbcOverload.sound(p.level(),target.position(),"minecraft:entity.firework_rocket.twinkle",1,1,0,0);
        }
    }
    public boolean withIncomingAttempt(ServerPlayer p,BooleanSupplier action){attempts.push(new Attempt(p,admitted.test(p)));try{return action.getAsBoolean();}finally{attempts.pop();}}
    public void incoming(ServerPlayer p){
        var attempt=attempts.peek();if(!(attempt!=null&&attempt.player()==p?attempt.admitted():admitted.test(p))||!technical(p))return;
        if(count(p,"technomk2")==4&&read(p,"technomk2shield")==0){
            sound(p,"minecraft:block.beacon.deactivate",1,.7f);dust(p,0xffc830,1.3f,30,.4,.5,.4);write(p,"technomk2shield",100);
        }
        if(ordinarySet(p))game.items().wands().technoRetaliation(p);
    }
    /** Invoked by the existing real jump callback, also available to shared artifact activation. */
    public void jump(ServerPlayer p){
        if(!admitted.test(p)||!technical(p))return;
        if(p.isShiftKeyDown()&&piece(p,EquipmentSlot.LEGS,"techno")&&!piece(p,EquipmentSlot.FEET,"technomk2")){
            if(read(p,"coldown8")>0)p.sendSystemMessage(Component.translatable("wyvern_chestplate.cooldown").withStyle(ChatFormatting.DARK_PURPLE)
                    .append(Component.translatable("ability_recharge").withStyle(ChatFormatting.WHITE)).append(Component.literal(Integer.toString(read(p,"coldown8"))).withStyle(ChatFormatting.WHITE)).append(Component.translatable("second.lbc").withStyle(ChatFormatting.WHITE)));
            else{
                p.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,127,false,false),p);p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,180,4,false,false),p);
                for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),e->e!=p&&e.distanceToSqr(p)>=.0001&&e.distanceToSqr(p)<=100&&game.items().wands().target(p,e))){
                    target.hurtServer(p.level(),p.damageSources().source(net.minecraft.world.damagesource.DamageTypes.CACTUS,p,p),8);
                    if(target instanceof ServerPlayer&&!game.items().magic().mechanismSuppressedAt(p.level(),target.position()))target.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,100,0,false,false),p);
                }
                sound(p,"minecraft:entity.ender_dragon.growl",4,1);p.level().sendParticles(ParticleTypes.CLOUD,true,false,p.getX(),p.getY()+5,p.getZ(),1000,1,5,1,.2);write(p,"coldown8",40);
            }
        }
        if(piece(p,EquipmentSlot.FEET,"techno")&&read(p,"nitro")>0&&p.getXRot()>=89&&p.getXRot()<=90){
            p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),p);
            var at=p.position().add(0,1.2,0).subtract(net.minecraft.world.phys.Vec3.directionFromRotation(0,p.getYRot()));
            var kills=game.items().indirectKills();var cause=kills.wildCause(p);var damage=kills.captureDamage(p.damageSources().explosion(null,p),cause);
            for(int i=0;i<10;i++)kills.trapChanges(p.level(),cause,()->p.level().explode(null,damage,null,at.x,at.y,at.z,1,false,Level.ExplosionInteraction.MOB));
            add(p,"nitro",-1);
        }
    }
    private static void sound(ServerPlayer p,String id,float volume,float pitch){LbcOverload.sound(p,p.position(),id,volume,pitch,0);}
    private static void dust(ServerPlayer p,int color,float size,int count,double x,double y,double z){p.level().sendParticles(new DustParticleOptions(color,size),p.getX(),p.getY()+.9,p.getZ(),count,x,y,z,0);}
    public void appendHud(ServerPlayer p,MutableComponent out){
        if(piece(p,EquipmentSlot.FEET,"techno"))out.append("  \uE101 ").append(Component.literal(Integer.toString(read(p,"nitro"))).withStyle(ChatFormatting.WHITE));
        if(piece(p,EquipmentSlot.FEET,"reactive")||piece(p,EquipmentSlot.FEET,"technomk2"))out.append("  \uE102 ").append(Component.literal(Integer.toString(read(p,"reactivefuel"))).withStyle(ChatFormatting.YELLOW));
    }
    public void reset(ServerPlayer p){forget(p);write(p,"reactivefuel",10);write(p,"nitro",4);for(String key:List.of("reactivefuelsub","technomk2shield","rocket_sound","coldown8","speedy"))write(p,key,0);}
    public void purify(ServerPlayer p){forget(p);for(String key:List.of("reactivefuel","nitro","reactivefuelsub","technomk2shield","coldown8"))write(p,key,0);}
    private void cleanupReactive(ServerPlayer p){
        if(eligible.test(p)&&technical(p)&&piece(p,EquipmentSlot.FEET,"reactive"))return;
        game.items().ownedEffects().withdraw(p,REACTIVE_LIFT);
        game.items().ownedEffects().withdraw(p,REACTIVE_LANDING);
    }
    /** Global time stop bypasses the ordinary armor tick; only withdraw invalid owners here. */
    public void pausedMaintenance(){
        for(var p:game.server().getPlayerList().getPlayers())inventoryChanged(p);
    }
    public void inventoryChanged(ServerPlayer p){flight.inventoryChanged(p);cleanupReactive(p);}
    public void loaded(Entity e){if(e instanceof LivingEntity l)living.add(l);if(e instanceof ServerPlayer p)flight.loaded(p);}
    public void unloaded(Entity e){living.remove(e);if(e instanceof ServerPlayer p)forget(p);}
    public void forget(ServerPlayer p){flight.forget(p);phases.remove(p);ticks.remove(p);for(var key:List.of(REACTIVE_LIFT,REACTIVE_LANDING,SHIELD))game.items().ownedEffects().withdraw(p,key);}
    public void stop(){flight.stop();for(var p:game.server().getPlayerList().getPlayers())forget(p);phases.clear();ticks.clear();living.removeIf(Entity::isRemoved);}
}
