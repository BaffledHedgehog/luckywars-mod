package me.skywars2.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Native player velocity grapple. Its anchor/flight data lives in the player's saved attachment. */
public final class LbcSteampunkHook {
    private static final double RANGE=100*.33;
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final String LANDING="steampunk-hook-landing";
    private static final AttachmentType<CompoundTag> FLIGHT=AttachmentRegistry.create(
            Identifier.parse("skywars2:steampunk_hook"),b->b.persistent(CompoundTag.CODEC));
    private final GameController game;
    // Short ray tickets only. Sustained state is canonical native player NBT, not this derived index.
    private final Map<UUID,LbcExasperato.RayTicket> requests=new HashMap<>();
    public LbcSteampunkHook(GameController game){this.game=game;}
    public static void register(){}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("grapping_hook",0)==1;}
    private boolean eligible(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&!game.awaitingRespawn(p.getUUID())
            &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&LbcDimensionItems.ordinary(p)
            &&game.items().wands().validRoleTarget(p,false);}
    private boolean paused(ServerPlayer p){return game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().stopped(p.level())||game.items().wands().inputTimeStopped(p)
            ||game.items().actionsBlocked(p);}
    private boolean admitted(ServerPlayer p){return eligible(p)&&!paused(p)&&!game.items().magic().mechanismSuppressed(p);}
    private static boolean teleportPending(ServerPlayer p){return ((LbcAntigravity.PlayerTransport)p.connection).skywars2$hasPendingTeleport();}
    private static Vec3 vector(CompoundTag d,String key){return new Vec3(d.getDoubleOr(key+"X",0),d.getDoubleOr(key+"Y",0),d.getDoubleOr(key+"Z",0));}
    private static void vector(CompoundTag d,String key,Vec3 v){d.putDouble(key+"X",v.x);d.putDouble(key+"Y",v.y);d.putDouble(key+"Z",v.z);}
    public Vec3 anchor(ServerPlayer p){var d=p.getAttached(FLIGHT);return d==null?null:vector(d,"anchor");}
    public int pending(){return requests.size();}
    public boolean use(ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!matches(stack)||!admitted(player))return false;
        if(game.items().mana().hudValue(player,"hook_cooldown")<120){player.sendSystemMessage(Component.translatable("uncharged_hook"));return false;}
        clear(player);var from=player.getEyePosition();var direction=player.getLookAngle();var epoch=game.items().epoch();
        var cause=game.items().indirectKills().coldCause(player,false);UUID author=player.getUUID();
        game.items().exasperato().replayableTool(player,from,direction,RANGE,false,new LbcExasperato.ReplayableRay(){
            LbcExasperato.RayTicket ticket;
            public boolean block(BlockPos pos,BlockState state){return !state.is(AIRS);}
            public boolean target(net.minecraft.world.entity.Entity entity){return !entity.getUUID().equals(author)&&entity instanceof LivingEntity
                    &&game.items().wands().validRoleTarget(entity,false);}
            public void trail(Vec3 a,Vec3 b){}
            public void submitted(LbcExasperato.RayTicket value){ticket=value;requests.put(author,value);}
            public void complete(HitResult hit){
                if(!requests.remove(author,ticket)||hit==null||hit.getType()==HitResult.Type.MISS)return;
                var current=game.server().getPlayerList().getPlayer(author);
                if(!game.items().currentEpoch(epoch)||!admitted(current)||current.level()!=player.level()||current.getItemInHand(hand)!=stack)return;
                // Source backs one block away from a wall; body hits keep their actual fixed contact point.
                var point=hit instanceof BlockHitResult?hit.getLocation().subtract(direction):hit.getLocation();
                if(!current.level().hasChunkAt(BlockPos.containing(point)))return;
                var data=new CompoundTag();data.putString("epoch",epoch);data.putString("dimension",current.level().dimension().identifier().toString());
                data.putLong("order",cause.order());vector(data,"anchor",point);current.setAttached(FLIGHT,data);
                current.stopRiding();var motion=current.getDeltaMovement();
                impulse(current,new Vec3(direction.x*2,motion.y,direction.z*2));
                current.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                // Debit exactly the actual launching stack; a second hand cannot silently damage the first.
                stack.hurtAndBreak(1,current,hand==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            }
        },false,true);
        return true;
    }
    public void tick(){
        for(var player:game.server().getPlayerList().getPlayers()){
            if(!eligible(player)){clear(player);continue;}
            var data=player.getAttached(FLIGHT);
            if(data!=null&&(!game.items().currentEpoch(data.getStringOr("epoch",""))
                    ||!player.level().dimension().identifier().toString().equals(data.getStringOr("dimension",""))
                    ||player.isShiftKeyDown()||teleportPending(player)||game.items().magic().mechanismSuppressed(player)
                    ||Double.isFinite(game.items().magic().mechanismSuppressionFraction(player.level(),player.getEyePosition(),vector(data,"anchor"))))){
                clear(player);data=null;
            }
            if(paused(player))continue;
            if(game.server().getTickCount()%20==0){var clock=game.items().mana().score(player,"hook_cooldown");if(clock.get()<Integer.MAX_VALUE)clock.add(1);}
            if(data==null)continue;
            var to=vector(data,"anchor");if(!player.level().hasChunkAt(BlockPos.containing(to))){clear(player);continue;}
            var from=player.getEyePosition();var delta=to.subtract(from);var motion=player.getDeltaMovement();var known=player.getKnownMovement();
            // Observed native movement includes the player's own input. A socketless body uses its actual Motion instead.
            var last=vector(data,"last");boolean external=motion.subtract(last).lengthSqr()>1e-10;
            if(known.lengthSqr()>1e-10&&(!external||known.lengthSqr()>motion.lengthSqr()))
                motion=new Vec3(known.x*.91,known.y*.98,known.z*.91);
            boolean ceiling=false;
            for(int y=1;y<=5;y++){var pos=BlockPos.containing(from.add(0,y,0));if(player.level().hasChunkAt(pos)&&!player.level().getBlockState(pos).is(AIRS)){ceiling=true;break;}}
            var next=pullVelocity(motion,delta,player.getLookAngle(),ceiling);impulse(player,next);player.resetFallDistance();
            var updated=data.copy();vector(updated,"last",next);player.setAttached(FLIGHT,updated);
            if(delta.lengthSqr()<=100)game.items().ownedEffects().grant(player,LANDING,ItemEffectLevels.create(MobEffects.SLOW_FALLING,20,1,false,false),player);
            else game.items().ownedEffects().withdraw(player,LANDING);
            double distance=delta.length();int samples=Math.min(128,(int)Math.ceil(distance/.33));
            for(int n=0;n<samples;n++){var at=from.lerp(to,(double)n/Math.max(1,samples));player.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,1,0,0,0,0);}
            game.items().indirectKills().displaced(player,new LbcIndirectKills.Owner(player.getUUID(),data.getLongOr("order",0),false,false));
        }
    }
    /** The source's upward-biased steering and finite vector damping, with no pose/position operation. */
    static Vec3 pullVelocity(Vec3 motion,Vec3 towards,Vec3 look,boolean ceiling){
        var unit=towards.normalize();double horizontal=Math.hypot(unit.x,unit.z);
        float yaw=(float)Math.toDegrees(Math.atan2(-unit.x,unit.z));float pitch=(float)-Math.toDegrees(Math.atan2(unit.y,horizontal))-20;
        var pull=Vec3.directionFromRotation(pitch,yaw);
        var value=motion.add(pull.x*.1,pull.y*.5,pull.z*.1);
        if(ceiling)value=new Vec3(value.x,-1,value.z);
        while(Math.abs(value.x)>10||value.y<-.5||value.y>10||Math.abs(value.z)>10)value=value.scale(.75);
        value=value.add(look.x*.1,look.y>=-.01?look.y*.1:0,look.z*.1);
        while(Math.abs(value.x)>10||Math.abs(value.y)>10||Math.abs(value.z)>10)value=value.scale(.75);
        return value;
    }
    private static void impulse(ServerPlayer p,Vec3 motion){p.setDeltaMovement(motion);p.needsSync=true;p.syncVelocity=true;p.connection.send(new ClientboundSetEntityMotionPacket(p.getId(),motion));}
    public void clear(ServerPlayer p){var ticket=requests.remove(p.getUUID());if(ticket!=null)ticket.cancel();
        if(p.getAttached(FLIGHT)!=null){p.removeAttached(FLIGHT);game.items().ownedEffects().withdraw(p,LANDING);}}
    public void reset(ServerPlayer p){clear(p);game.items().mana().score(p,"hook_cooldown").set(0);}
    void transformOrigins(net.minecraft.server.level.ServerLevel level,java.util.function.UnaryOperator<Vec3> transform){
        for(var p:game.server().getPlayerList().getPlayers())if(p.level()==level){
            var d=p.getAttached(FLIGHT);if(d==null)continue;
            var next=d.copy();vector(next,"anchor",transform.apply(vector(d,"anchor")));
            vector(next,"last",transform.apply(vector(d,"last")).subtract(transform.apply(Vec3.ZERO)));p.setAttached(FLIGHT,next);
        }
    }
    public void stop(){for(var ticket:List.copyOf(requests.values()))ticket.cancel();requests.clear();for(var p:game.server().getPlayerList().getPlayers())reset(p);}
}
