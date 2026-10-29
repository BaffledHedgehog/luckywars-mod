package me.skywars2.game;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.netty.buffer.Unpooled;
import java.util.*;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A shootable native actor, packet-only FPV seat and native remote chunk/entity tracking. */
public final class LbcFpvDrone {
    public static final String TAG="skywars2.fpv_drone";
    public static final EntityType<Drone> TYPE=type();
    private static final TicketType BODY_TICKET=new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING|TicketType.FLAG_SIMULATION|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    static final double TURN_DEGREES=3;
    private final GameController game;
    private final Map<Drone,Visual> actors=new IdentityHashMap<>();
    private final Map<UUID,Session> controllers=new HashMap<>();
    private record BodyChunk(ServerLevel level,ChunkPos pos){}
    private final Map<BodyChunk,Integer> tickets=new HashMap<>();
    private int lastTick=Integer.MIN_VALUE;
    private long explosions;

    private static EntityType<Drone> type(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:fpv_drone"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Drone>of(Drone::new,MobCategory.MISC)
                .sized(1.1f,.6f).eyeHeight(.3f).clientTrackingRange(16).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);FabricDefaultAttributeRegistry.register(type,Vex.createAttributes().add(Attributes.MAX_HEALTH,20));return type;
    }
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:fpv_body"),BODY_TICKET);}
    public LbcFpvDrone(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("drone_pult",0)==1;}
    public static final class Drone extends Vex implements PolymerEntity {
        UUID author;long order;String epoch="";boolean retired;
        Drone(EntityType<? extends Drone> type,Level level){super(type,level);setNoAi(true);setNoGravity(true);setInvisible(true);setSilent(true);setPersistenceRequired();}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.VEX;}
        @Override public boolean isPushable(){return false;}
        @Override public boolean canCollideWith(Entity other){return false;}
        @Override public boolean isPushedByFluid(){return false;}
        @Override public void tick(){var g=SkyWars2.game();if(g!=null&&g.items().fpvDrone().paused(this))return;
            setDeltaMovement(Vec3.ZERO);super.tick();setDeltaMovement(Vec3.ZERO);}
        @Override public void die(DamageSource damage){var g=SkyWars2.game();if(g!=null)g.items().fpvDrone().explode(this);else discard();}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);
            if(author!=null)out.putString("FpvAuthor",author.toString());out.putLong("FpvOrder",order);out.putString("FpvEpoch",epoch);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);
            try{author=UUID.fromString(in.getStringOr("FpvAuthor",""));}catch(IllegalArgumentException ignored){author=null;}
            order=in.getLongOr("FpvOrder",0);epoch=in.getStringOr("FpvEpoch","");setNoAi(true);setNoGravity(true);setInvisible(true);}
    }
    private final class Visual extends Display.ItemDisplay {
        final Drone drone;
        Visual(Drone drone){super(EntityTypes.ITEM_DISPLAY,drone.level());this.drone=drone;
            try{load(TagValueInput.create(ProblemReporter.DISCARDING,registryAccess(),TagParser.parseCompoundFully("""
{interpolation_duration:1,shadow_radius:0.0f,shadow_strength:0.0f,teleport_duration:1,item_display:"fixed",transformation:[-1.7678f,1.7678f,0f,0f,0f,0f,-2.5f,0.75f,-1.7678f,-1.7678f,0f,0f,0f,0f,0f,1f],item:{id:"minecraft:golden_sword",count:1,components:{item_model:"lbc:drone"}}}
                    """)));}catch(CommandSyntaxException error){throw new IllegalStateException("Drone source model",error);}
            entityTags().add("skywars2.fpv_visual");game.items().markCurrentItemEpoch(this);}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean broadcastToPlayer(ServerPlayer p){return !controlling(p)||controllers.get(p.getUUID()).drone!=drone;}
    }
    private static final class Session {
        final ServerPlayer player;final Drone drone;final int born;float yaw,pitch;boolean attached;BodyChunk body;
        Session(ServerPlayer player,Drone drone,int born){this.player=player;this.drone=drone;this.born=born;yaw=player.getYRot();pitch=player.getXRot();}
    }
    public boolean controlling(ServerPlayer p){var s=controllers.get(p.getUUID());return s!=null&&s.player==p;}
    public static Vec3 viewpoint(ServerPlayer player){var g=SkyWars2.game();if(g==null||g.items().fpvDrone()==null)return player.position();
        var s=g.items().fpvDrone().controllers.get(player.getUUID());return s!=null&&s.attached?trackingPosition(player):player.position();}
    /** Prefetch around the drone before switching the visible camera to its published entity. */
    public static Vec3 trackingPosition(ServerPlayer player){var g=SkyWars2.game();if(g==null||g.items().fpvDrone()==null)return player.position();
        var s=g.items().fpvDrone().controllers.get(player.getUUID());return s!=null&&s.player==player&&s.drone.level()==player.level()?s.drone.position():player.position();}
    public boolean look(ServerPlayer p,ServerboundMovePlayerPacket packet){var s=controllers.get(p.getUUID());if(s==null||s.player!=p)return false;
        float yaw=packet.getYRot(s.yaw),pitch=packet.getXRot(s.pitch);
        if(Float.isFinite(yaw)&&Float.isFinite(pitch)){s.yaw=Mth.wrapDegrees(yaw);s.pitch=Mth.clamp(pitch,-90,90);}return true;}
    public boolean input(ServerPlayer p,Input input){if(!controlling(p))return false;
        p.setLastClientInput(Input.EMPTY);p.setShiftKeyDown(false);p.setSprinting(false);
        if(input.shift())release(p);return true;}
    private boolean eligible(ServerPlayer p){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
            &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&LbcDimensionItems.ordinary(p)&&game.items().wands().validRoleTarget(p,false);}
    public boolean use(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);if(!matches(stack)||!eligible(p)||controlling(p)||p.isPassenger()
                ||game.items().actionsBlocked(p)||game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(p)
                ||game.items().timeStop().stopped(p.level())||game.items().magic().mechanismSuppressed(p))return false;
        // Keep the authored world-point launch reference, moved another3 blocks back and2 up.
        Vec3 horizontal=new Vec3(-p.getX(),0,-p.getZ()).normalize();if(horizontal.lengthSqr()<1e-8)horizontal=new Vec3(0,0,1);
        var at=p.position().add(horizontal.scale(-48)).add(0,7,0);
        if(!Level.isInSpawnableBounds(BlockPos.containing(at))||!resident(p.level(),at)||game.items().magic().mechanismSuppressedAt(p.level(),at))return false;
        var drone=new Drone(TYPE,p.level());drone.setPos(at);orientation(drone,horizontal);drone.setHealth(20);
        var cause=game.items().indirectKills().wildCause(p);drone.author=p.getUUID();drone.order=cause.order();drone.epoch=game.items().epoch();
        drone.entityTags().addAll(List.of(TAG,"homing_drone","slowraycast","mecha",drone.epoch));bind(drone);
        if(!p.level().addFreshEntity(drone)){drone.discard();return false;}
        loaded(drone);var session=new Session(p,drone,game.server().getTickCount());controllers.put(p.getUUID(),session);bodyTicket(session);
        p.stopUsingItem();p.setLastClientInput(Input.EMPTY);p.setShiftKeyDown(false);p.setSprinting(false);
        stack.consume(1,p);p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        LbcOverload.sound(p.level(),p.position(),"minecraft:lbcsounds.drone_signal",net.minecraft.sounds.SoundSource.PLAYERS,1,1,50,.3f);return true;
    }
    private static boolean resident(ServerLevel level,Vec3 at){var b=BlockPos.containing(at);return level.getChunkSource().getChunkNow(b.getX()>>4,b.getZ()>>4)!=null;}
    private boolean live(Drone d){return d.author!=null&&!d.retired&&!d.isRemoved()&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(d.epoch);}
    boolean paused(Drone d){return !LbcDimensionItems.ordinary(d.level())||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped((ServerLevel)d.level())
            ||game.items().wands().timeStopped()||game.items().timeStop().frozen(d)||d.entityTags().contains("time_stopped");}
    private LbcSpellSource.Source source(Drone d){return new LbcSpellSource.Source(d.getUUID(),d.author,d.epoch,new LbcIndirectKills.Owner(d.author,d.order,true,false),Set.of(),true);}
    private void bind(Drone d){game.items().spellSources().bind(d,source(d));}
    public void loaded(Entity e){if(!(e instanceof Drone d))return;if(!live(d)){d.discard();return;}bind(d);
        if(!actors.containsKey(d)){var visual=new Visual(d);visual.setPos(d.position());visual.setYRot(d.getYRot());visual.setXRot(d.getXRot());actors.put(d,visual);
            if(!((ServerLevel)d.level()).addFreshEntity(visual)){actors.remove(d);visual.discard();d.discard();}}
        game.items().timeStop().prepareFrozen(d);
    }
    public void unloaded(Entity e){if(!(e instanceof Drone d))return;var visual=actors.remove(d);if(visual!=null)visual.discard();
        var s=controllers.get(d.author);if(s!=null&&s.drone==d)release(s.player);}
    private static void orientation(Drone d,Vec3 direction){d.setYRot((float)(Mth.atan2(-direction.x,direction.z)*180/Math.PI));d.setXRot((float)(-Mth.atan2(direction.y,direction.horizontalDistance())*180/Math.PI));d.setYHeadRot(d.getYRot());d.setYBodyRot(d.getYRot());}
    static Vec3 turn(Vec3 from,Vec3 target,double degrees){from=from.normalize();target=target.normalize();double dot=Mth.clamp(from.dot(target),-1,1),angle=Math.acos(dot),limit=Math.toRadians(degrees);
        if(angle<=limit)return target;var tangent=target.subtract(from.scale(dot));if(tangent.lengthSqr()<1e-12)tangent=from.cross(Math.abs(from.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0));
        return from.scale(Math.cos(limit)).add(tangent.normalize().scale(Math.sin(limit))).normalize();}
    public void tick(){int now=game.server().getTickCount();if(now==lastTick)return;lastTick=now;
        for(var s:List.copyOf(controllers.values())){
            if(!eligible(s.player)||s.player.level()!=s.drone.level()||!live(s.drone)){release(s.player);continue;}
            bodyTicket(s);s.player.setLastClientInput(Input.EMPTY);
            // Native tracker has published the actor before its ID becomes the client camera/seat.
            if(!s.attached&&now-s.born>=2&&net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(s.drone).contains(s.player)){s.player.level().getChunkSource().move(s.player);s.player.connection.send(passengers(s.drone.getId(),s.player.getId()));
                s.player.connection.send(new ClientboundSetCameraPacket(s.drone));s.attached=true;}
        }
        for(var entry:List.copyOf(actors.entrySet())){var d=entry.getKey();var visual=entry.getValue();
            if(!live(d)){retire(d);continue;}var level=(ServerLevel)d.level();if(paused(d))continue;
            if(game.items().magic().mechanismSuppressedAt(level,d.position())){retire(d);continue;}
            var session=controllers.get(d.author);Vec3 direction=d.getLookAngle();
            if(session!=null&&session.drone==d)direction=turn(direction,Vec3.directionFromRotation(session.pitch,session.yaw),TURN_DEGREES);
            orientation(d,direction);var from=d.position();var to=from.add(direction);
            if(!Level.isInSpawnableBounds(BlockPos.containing(to))){retire(d);continue;}
            // Released drones retain the source's unloaded-chunk pause; no infinite chunk generation.
            if(!resident(level,to))continue;
            var wall=LbcTerra.wallHit(level,from,to,state->!state.is(net.minecraft.tags.TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("airs"))));
            var end=wall==null?to:wall.getLocation();
            var hit=ProjectileUtil.getEntityHitResult(level,d,from,end,new AABB(from,end).inflate(1),
                    entity->entity!=visual&&entity instanceof LivingEntity&&game.items().wands().validRoleTarget(entity,false),0);
            d.setPos(hit==null?end:hit.getLocation());visual.setPos(d.position());visual.setYRot(d.getYRot());visual.setXRot(d.getXRot());
            if(session!=null&&session.drone==d)session.player.level().getChunkSource().move(session.player);
            LbcOverload.sound(level,d.position(),"minecraft:lbcsounds.drone_tick",net.minecraft.sounds.SoundSource.HOSTILE,.15f,1,35,.04f);
            if(hit!=null||wall!=null&&level.hasChunkAt(wall.getBlockPos()))explode(d);
        }
    }
    public void explode(Drone d){if(!live(d))return;d.retired=true;var level=(ServerLevel)d.level();var accepted=source(d);
        try(var scope=game.items().spellSources().scope(accepted,level)){
            explosions++;game.items().exasperato().encounterBlast(d,level,d.position(),8,tnt->game.items().spellSources().bind(tnt,accepted));
        }finally{retire(d);}
    }
    private void retire(Drone d){d.retired=true;var session=controllers.get(d.author);if(session!=null&&session.drone==d)release(session.player);
        var visual=actors.remove(d);if(visual!=null)visual.discard();if(!d.isRemoved())d.discard();}
    private static ClientboundSetPassengersPacket passengers(int entity,int... ids){var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{buffer.writeVarInt(entity);buffer.writeVarIntArray(ids);return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer);}finally{buffer.release();}}
    public void release(ServerPlayer p){var session=controllers.get(p.getUUID());if(session==null||session.player!=p)return;controllers.remove(p.getUUID());releaseBody(session);
        p.setLastClientInput(Input.EMPTY);p.setShiftKeyDown(false);
        // Respawn replaces ServerPlayer. Never send an obsolete entity ID or re-add its chunk ticket.
        if(p.isRemoved()||game.server().getPlayerList().getPlayer(p.getUUID())!=p)return;
        if(session.attached){p.connection.send(passengers(session.drone.getId()));p.connection.send(new ClientboundSetCameraPacket(p));
            p.connection.teleport(p.getX(),p.getY(),p.getZ(),session.yaw,session.pitch);}
        p.level().getChunkSource().move(p);
    }
    private void bodyTicket(Session s){var next=new BodyChunk(s.player.level(),s.player.chunkPosition());if(next.equals(s.body))return;releaseBody(s);
        s.body=next;if(tickets.merge(next,1,Integer::sum)==1)next.level.getChunkSource().addTicketWithRadius(BODY_TICKET,next.pos,3);}
    private void releaseBody(Session s){var key=s.body;if(key==null)return;s.body=null;int count=tickets.getOrDefault(key,1)-1;
        if(count>0)tickets.put(key,count);else{tickets.remove(key);key.level.getChunkSource().removeTicketWithRadius(BODY_TICKET,key.pos,3);}}
    public void forget(ServerPlayer p){var s=controllers.get(p.getUUID());if(s!=null)release(s.player);}
    public void stop(){for(var s:List.copyOf(controllers.values()))release(s.player);for(var d:List.copyOf(actors.keySet()))retire(d);lastTick=Integer.MIN_VALUE;}
    public int count(){return actors.size();}public long explosions(){return explosions;}public int ticketCount(){return tickets.size();}
    public List<Drone> snapshot(){return List.copyOf(actors.keySet());}
}
