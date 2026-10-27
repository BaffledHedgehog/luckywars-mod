package me.skywars2.game;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

/** Worn source Shulker armor, with shared projectile, interception and surface-search owners. */
public final class LbcShulkerArmor {
    static final Identifier SPEED=Identifier.parse("minecraft:shulker_armor");
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private final GameController game;
    private final Set<UUID> slowed=new HashSet<>();
    private record Attempt(ServerPlayer player,boolean chest){}
    private final Deque<Attempt> attempts=new ArrayDeque<>();
    private int phase;
    public LbcShulkerArmor(GameController game){this.game=game;}
    private boolean admitted(ServerPlayer p){
        return p!=null&&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()
                &&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(p)
                &&game.items().wands().validRoleTarget(p,false)&&LbcDimensionItems.ordinary(p)
                &&!game.items().wands().inputTimeStopped(p)&&!game.items().timeStop().stopped(p.level());
    }
    private static boolean piece(ServerPlayer p,EquipmentSlot slot){return LbcFartus.data(p.getItemBySlot(slot)).getIntOr("shulkshell",0)==1;}
    static boolean full(ServerPlayer p){for(var slot:SLOTS)if(!piece(p,slot))return false;return true;}
    private boolean magic(ServerPlayer p){return !game.items().magic().beaconSuppressed(p.position());}
    public void tick(){
        if(!game.playing()||game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        phase=(phase+1)%200;
        for(var p:game.server().getPlayerList().getPlayers()){
            validate(p);
            if(!admitted(p))continue;
            boolean helmet=piece(p,EquipmentSlot.HEAD),legs=piece(p,EquipmentSlot.LEGS),complete=full(p);
            // Author correction: the source omitted the stopper gate on helmet/leggings.
            if((!helmet&&!legs&&!complete)||!magic(p))continue;
            if(phase%2==0&&legs)p.removeEffect(MobEffects.LEVITATION);
            if(phase==0&&helmet)shoot(p);
            if(complete){
                p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,1),p);
                var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);
                if(speed!=null&&!speed.hasModifier(SPEED))speed.addTransientModifier(new AttributeModifier(SPEED,-10,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                slowed.add(p.getUUID());
                game.items().magic().eraseArmorArrows(p);
            }
        }
    }
    void shoot(ServerPlayer p){
        if(!admitted(p)||!piece(p,EquipmentSlot.HEAD)||!magic(p))return;
        ServerPlayer nearest=null;double best=Double.POSITIVE_INFINITY;
        for(var target:p.level().players())if(target!=p&&game.items().wands().target(p,target)){
            double distance=target.distanceToSqr(p);if(distance<best){best=distance;nearest=target;}
        }
        game.items().creatureDrops().launchArmorShell(p,nearest);
    }
    public void incoming(ServerPlayer p){
        var attempt=attempts.peek();
        boolean allowed=attempt!=null&&attempt.player()==p?attempt.chest():admitted(p)&&piece(p,EquipmentSlot.CHEST)&&magic(p);
        if(allowed&&p.getRandom().nextInt(5)==0)performChestTeleport(p);
    }
    /** Preserve the actual criterion's pre-death wearer admission, including fatal and shield events. */
    public boolean withIncomingAttempt(ServerPlayer p,BooleanSupplier hurt){
        attempts.push(new Attempt(p,admitted(p)&&piece(p,EquipmentSlot.CHEST)&&magic(p)));
        try{return hurt.getAsBoolean();}finally{attempts.pop();}
    }
    void chestTeleport(ServerPlayer p){
        if(!admitted(p)||!piece(p,EquipmentSlot.CHEST)||!magic(p))return;
        performChestTeleport(p);
    }
    private void performChestTeleport(ServerPlayer p){
        // Both particle commands in the source execute at the original function position.
        var at=p.position();particles(p,at,ParticleTypes.REVERSE_PORTAL);
        LbcOverload.sound(p.level(),at,"minecraft:entity.enderman.teleport",1,1,0,0);
        teleport(p,at,20);
        particles(p,at,ParticleTypes.PORTAL);
        LbcOverload.sound(p.level(),at,"minecraft:entity.enderman.teleport",1,1,0,0);
    }
    private static void particles(ServerPlayer p,Vec3 at,net.minecraft.core.particles.SimpleParticleType type){
        p.level().sendParticles(type,at.x,at.y+1,at.z,100,0,1,0,1);
    }
    public void jump(ServerPlayer p){
        if(admitted(p)&&full(p)&&magic(p))teleport(p,p.position().add(p.getLookAngle().scale(5)),3);
    }
    private static void teleport(ServerPlayer p,Vec3 center,int radius){
        int ceiling=p.level().dimension().equals(Level.NETHER)?120:p.level().getMaxY()+1;
        var destination=LbcSurfaceTeleport.destination(p.level(),center,radius,ceiling,p.getRandom());
        if(destination!=null)p.teleportTo(p.level(),destination.x,destination.y,destination.z,Set.of(),p.getYRot(),p.getXRot(),true);
    }
    /** Source removes its speed modifier when equipment changes; entering a stopper alone does not remove it. */
    private void validate(ServerPlayer p){
        if(!full(p)||!p.isAlive()||p.level()!=game.maps().level()||!game.canUseGameplayItems(p)||!LbcDimensionItems.ordinary(p))forget(p);
    }
    public void inventoryChanged(ServerPlayer p){forget(p);}
    public void forget(ServerPlayer p){
        var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(SPEED);
        slowed.remove(p.getUUID());
    }
    public void stop(){for(var id:Set.copyOf(slowed)){var p=game.server().getPlayerList().getPlayer(id);if(p!=null)forget(p);}slowed.clear();phase=0;}
}
