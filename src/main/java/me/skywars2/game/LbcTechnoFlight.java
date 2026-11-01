package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** MK2 boots' owner-local schedule; fuel and native effect clocks remain shared. */
public final class LbcTechnoFlight {
    static final Identifier GRAVITY_ONE=Identifier.parse("minecraft:gravity_mk2armor_fix1");
    static final Identifier GRAVITY_TWO=Identifier.parse("minecraft:gravity_mk2armor_fix2");
    static final String FLIGHT_OWNER="technomk2-flight", LANDING_OWNER="technomk2-landing";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final class Flight {
        final ServerLevel level;
        int delay=2;
        int fuelHalf;
        Flight(ServerLevel level){this.level=level;}
    }
    private final GameController game;
    private final Predicate<ServerPlayer> eligible,admitted;
    private final Map<ServerPlayer,Flight> flights=new IdentityHashMap<>();

    LbcTechnoFlight(GameController game,Predicate<ServerPlayer> eligible,Predicate<ServerPlayer> admitted){
        this.game=game;this.eligible=eligible;this.admitted=admitted;
    }
    private static boolean piece(ServerPlayer player,EquipmentSlot slot,String key){
        var stack=player.getItemBySlot(slot);
        return ItemData.intValue(stack,key,0)==1;
    }
    private int read(ServerPlayer player,String objective){return game.items().mana().hudValue(player,objective);}
    private void write(ServerPlayer player,String objective,int value){game.items().mana().score(player,objective).set(value);}
    private void add(ServerPlayer player,String objective,int value){game.items().mana().score(player,objective).add(value);}
    private static void gravity(ServerPlayer player,Identifier id,Double amount){
        var attribute=player.getAttribute(Attributes.GRAVITY);if(attribute==null)return;
        var previous=attribute.getModifier(id);
        if(amount!=null&&previous!=null&&previous.amount()==amount
                &&previous.operation()==AttributeModifier.Operation.ADD_VALUE)return;
        if(previous!=null)attribute.removeModifier(id);
        if(amount!=null)attribute.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private void removeFlightPhysics(ServerPlayer player){
        gravity(player,GRAVITY_ONE,null);gravity(player,GRAVITY_TWO,null);
        game.items().ownedEffects().withdraw(player,FLIGHT_OWNER);
    }
    private static void sound(ServerPlayer player,String name,float volume,float pitch){
        LbcOverload.sound(player,"minecraft:lbcsounds."+name,volume,pitch,0);
    }
    private void begin(ServerPlayer player){
        flights.put(player,new Flight(player.level()));player.entityTags().add("technomk2fly");
        write(player,"reactivefuelsub",0);sound(player,"technoflyclick",1,1);
    }
    private void end(ServerPlayer player,boolean landing){
        boolean flying=flights.remove(player)!=null||player.entityTags().contains("technomk2fly");
        player.entityTags().remove("technomk2fly");removeFlightPhysics(player);
        game.items().ownedEffects().withdraw(player,LANDING_OWNER);
        if(flying){write(player,"reactivefuelsub",0);sound(player,"technoflyclick",1,1);}
        if(landing&&flying)player.entityTags().add("stopped_flight");
        else player.entityTags().remove("stopped_flight");
    }
    /** Called even outside admitted time so unequip and protected-role cleanup is immediate. */
    public void tick(ServerPlayer player){
        var flight=flights.get(player);
        if(!eligible.test(player)||!piece(player,EquipmentSlot.FEET,"technomk2")
                ||game.items().magic().mechanismSuppressed(player)
                ||flight!=null&&flight.level!=player.level()){
            end(player,false);return;
        }
        if(!admitted.test(player))return;
        if(flight==null){
            if(player.entityTags().contains("technomk2fly"))end(player,false);
            if(!player.onGround()&&player.isShiftKeyDown()&&read(player,"reactivefuel")>0)begin(player);
            else if(player.onGround())regenerate(player);
        }else if(--flight.delay<=0){
            flight.delay=1;run(player,flight);
        }
        if(player.entityTags().contains("stopped_flight"))afterFlight(player);
    }
    private void regenerate(ServerPlayer player){
        add(player,"reactivefuelsub",piece(player,EquipmentSlot.LEGS,"reactive")?2:1);
        if(read(player,"reactivefuelsub")<20)return;
        int fuel=read(player,"reactivefuel");
        if(piece(player,EquipmentSlot.CHEST,"reactive")&&fuel>=10&&fuel<=19)fuel++;
        if(fuel>=0&&fuel<=9)fuel++;
        write(player,"reactivefuel",fuel);write(player,"reactivefuelsub",0);
    }
    private void run(ServerPlayer player,Flight flight){
        int pitch=(int)Math.floor(player.getXRot());
        int cost=0;
        if(pitch>=-90&&pitch<=-75){up(player);cost=10;}
        else if(pitch>=-74&&pitch<=74){hover(player);cost=1;}
        else if(pitch>=75&&pitch<=90)down(player);
        boolean boost=player.isSprinting()&&piece(player,EquipmentSlot.CHEST,"technomk2");
        if(boost){
            var motion=player.getDeltaMovement();var heading=Vec3.directionFromRotation(0,player.getYRot()).scale(.02);
            player.setDeltaMovement(motion.add(heading.x,0,heading.z));player.needsSync=true;
            cost=Math.max(1,cost); // Active horizontal thrust is not a free descent.
        }
        flight.fuelHalf+=cost*(boost?3:2);
        add(player,"reactivefuelsub",flight.fuelHalf/2);flight.fuelHalf%=2;
        if(read(player,"reactivefuelsub")>=40){
            add(player,"reactivefuel",-1);write(player,"reactivefuelsub",boost?read(player,"reactivefuelsub")-40:0);
        }
        if(player.onGround()||read(player,"reactivefuel")==0)end(player,true);
    }
    private void up(ServerPlayer player){
        game.items().ownedEffects().withdraw(player,FLIGHT_OWNER);
        boolean upgraded=piece(player,EquipmentSlot.LEGS,"technomk2");
        if(upgraded&&player.getXRot()==-90f){
            gravity(player,GRAVITY_ONE,null);
            gravity(player,GRAVITY_TWO,(Math.floor((double)player.getXRot()*100)+6420)*.001);
        }else{
            gravity(player,GRAVITY_ONE,null);gravity(player,GRAVITY_TWO,null);
            game.items().ownedEffects().grant(player,FLIGHT_OWNER,
                ItemEffectLevels.create(MobEffects.LEVITATION,20,upgraded?32:21,false,false),player);
        }
        add(player,"rocket_sound",1);
        if(read(player,"rocket_sound")>=1){sound(player,"rocket_power",3,1);write(player,"rocket_sound",0);}
        particles(player,true);
    }
    private static double additiveGravity(ServerPlayer player){
        var attribute=player.getAttribute(Attributes.GRAVITY);double additive=attribute.getBaseValue();
        for(var modifier:attribute.getModifiers())if(modifier.operation()==AttributeModifier.Operation.ADD_VALUE
                &&!modifier.id().equals(GRAVITY_ONE)&&!modifier.id().equals(GRAVITY_TWO))additive+=modifier.amount();
        return additive;
    }
    private void hover(ServerPlayer player){
        game.items().ownedEffects().withdraw(player,FLIGHT_OWNER);
        // Cancel the additive stage, so every native multiplicative stage also remains exactly zero.
        // Remove only our own modifiers on departure; unusual external gravity is left intact.
        gravity(player,GRAVITY_TWO,null);gravity(player,GRAVITY_ONE,-additiveGravity(player));
        var motion=player.getDeltaMovement();player.setDeltaMovement(motion.x,0,motion.z);player.needsSync=true;
        rocket(player,false);particles(player,false);
    }
    private void down(ServerPlayer player){
        game.items().ownedEffects().withdraw(player,FLIGHT_OWNER);gravity(player,GRAVITY_ONE,null);
        // The old shared tmp was stale without MK2 legs. Use the explicit source fallback .1.
        boolean upgraded=piece(player,EquipmentSlot.LEGS,"technomk2");
        gravity(player,GRAVITY_TWO,upgraded?(player.getXRot()==90f?6.3:additiveGravity(player)*.5+.15):.1);
        rocket(player,true);particles(player,false);
    }
    private void rocket(ServerPlayer player,boolean down){
        add(player,"rocket_sound",1);
        if(read(player,"rocket_sound")>=3){sound(player,"rocket",down?.6f:1,down?.8f:1);write(player,"rocket_sound",0);}
    }
    private void afterFlight(ServerPlayer player){
        for(int below=1;below<=5;below++){
            var position=BlockPos.containing(player.getX(),player.getY()-below,player.getZ());
            if(!player.level().hasChunkAt(position)||player.level().getBlockState(position).is(AIRS))continue;
            player.entityTags().remove("stopped_flight");
            game.items().ownedEffects().grant(player,LANDING_OWNER,
                    ItemEffectLevels.create(MobEffects.SLOW_FALLING,20,2,false,false),player);
            sound(player,"rocket_power",.5f,.7f);
            player.level().sendParticles(ParticleTypes.CLOUD,player.getX(),player.getY()-2,player.getZ(),20,.5,1,.5,0);
            return;
        }
    }
    private static Vec3 local(ServerPlayer player,float pitch,double x,double y,double z){
        var forward=Vec3.directionFromRotation(pitch,player.getYRot());
        var up=Vec3.directionFromRotation(pitch-90,player.getYRot());
        var left=forward.cross(up).scale(-1);
        return player.position().add(left.scale(x)).add(up.scale(y)).add(forward.scale(z));
    }
    private static void particles(ServerPlayer player,boolean strong){
        boolean chest=!strong&&piece(player,EquipmentSlot.CHEST,"technomk2");
        for(int side:new int[]{1,-1}){
            var origin=local(player,chest?0:player.getXRot(),.18*side,chest?.8:0,-.1);
            if(strong)player.level().sendParticles(ParticleTypes.CLOUD,true,false,origin.x,origin.y,origin.z,2,0,0,0,0);
            else{
                // Preserve the source command's local/relative delta coordinate interpretation.
                var delta=chest?local(player,0,80000*side,-100000,-50000)
                        :player.position().add(0,-100000,0);
                player.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,true,false,
                        origin.x,origin.y,origin.z,0,delta.x,delta.y,delta.z,chest?.000005:.00001);
            }
        }
    }
    public void inventoryChanged(ServerPlayer player){
        if(!eligible.test(player)||!piece(player,EquipmentSlot.FEET,"technomk2")
                ||game.items().magic().mechanismSuppressed(player))end(player,false);
    }
    public boolean flying(ServerPlayer player){return flights.containsKey(player);}
    public int activeCount(){return flights.size();}
    public void loaded(ServerPlayer player){end(player,false);}
    public void forget(ServerPlayer player){end(player,false);}
    public void stop(){
        for(var player:java.util.List.copyOf(flights.keySet()))end(player,false);
        for(var player:game.server().getPlayerList().getPlayers())end(player,false);
    }

    ResidentCheckpoint residentCheckpoint(MatchSpellCheckpoint.Links links){return new ResidentCheckpoint(links);}
    void prepareResidentFrozen(){flights.clear();}
    final class ResidentCheckpoint {
        private record Image(MatchDamageSources.Ref owner,ServerLevel level,int delay,int fuelHalf){}
        private final java.util.List<Image> saved;
        ResidentCheckpoint(MatchSpellCheckpoint.Links links){saved=flights.entrySet().stream().map(e->new Image(links.key(e.getKey()),e.getValue().level,e.getValue().delay,e.getValue().fuelHalf)).toList();}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var next=new IdentityHashMap<ServerPlayer,Flight>();for(var image:saved){MatchAuthorityCheckpoint.world(game,image.level);
                var flight=new Flight(image.level);flight.delay=image.delay;flight.fuelHalf=image.fuelHalf;next.put((ServerPlayer)links.get(image.owner),flight);}
            return ()->{flights.clear();flights.putAll(next);};
        }
    }

}
