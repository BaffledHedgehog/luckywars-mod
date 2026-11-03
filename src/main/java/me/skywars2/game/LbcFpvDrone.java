package me.skywars2.game;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
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
    static final Identifier HUD=Identifier.parse("lbc:fpv_drone");
    private final GameController game;
    private IdentityHashMap<Drone,Visual> actors=new IdentityHashMap<>();
    private HashMap<UUID,Session> controllers=new HashMap<>();
    private record BodyChunk(ServerLevel level,ChunkPos pos){}
    private final Map<BodyChunk,Integer> tickets=new HashMap<>();
    private int lastTick=Integer.MIN_VALUE;
    private boolean advancing,rewinding;
    private long explosions;

    private static EntityType<Drone> type(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:fpv_drone"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Drone>of(Drone::new,MobCategory.MISC)
                .sized(1.1f,.6f).eyeHeight(.3f).clientTrackingRange(16).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);FabricDefaultAttributeRegistry.register(type,Vex.createAttributes().add(Attributes.MAX_HEALTH,20));return type;
    }
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:fpv_body"),BODY_TICKET);}
    public LbcFpvDrone(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return ItemData.intValue(stack,"drone_pult",0)==1;}
    public static final class Drone extends Vex implements PolymerEntity {
        UUID author;long order;String epoch="";boolean retired;
        Drone(EntityType<? extends Drone> type,Level level){super(type,level);setNoAi(true);setNoGravity(true);setInvisible(true);setSilent(true);setPersistenceRequired();}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.VEX;}
        @Override public boolean isPushable(){return false;}
        @Override public boolean canCollideWith(Entity other){return false;}
        @Override public boolean isPushedByFluid(){return false;}
        // Native effect synchronization otherwise clears constructor invisibility.
        @Override protected void updateInvisibilityStatus(){setInvisible(true);removeEffectParticles();}
        @Override public void tick(){var g=SkyWars2.game();if(g!=null&&g.items().fpvDrone().paused(this))return;
            setDeltaMovement(Vec3.ZERO);super.tick();setDeltaMovement(Vec3.ZERO);}
        @Override public void die(DamageSource damage){var g=SkyWars2.game();if(g!=null)g.items().fpvDrone().explode(this);else discard();}
        @Override public boolean hurtServer(ServerLevel level,DamageSource damage,float amount){
            var game=SkyWars2.game();
            if(game!=null&&LbcDamage.isElectricalContact(damage)
                    &&game.items().fpvDrone().tryShortCircuit(this))return true;
            return super.hurtServer(level,damage,amount);
        }
        @Override public void thunderHit(ServerLevel level,LightningBolt bolt){var game=SkyWars2.game();if(game!=null&&game.items().fpvDrone().tryShortCircuit(this))return;super.thunderHit(level,bolt);}
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
        @Override public boolean broadcastToPlayer(ServerPlayer p){return true;}
    }
    /** Empty, owner-only client entity: no world insertion, ticking actor or saved state. */
    private static final class Seat extends Display.ItemDisplay {
        Seat(ServerLevel level){super(EntityTypes.ITEM_DISPLAY,level);
            try{load(TagValueInput.create(ProblemReporter.DISCARDING,registryAccess(),TagParser.parseCompoundFully("{teleport_duration:1}")));}
            catch(CommandSyntaxException error){throw new IllegalStateException("FPV camera seat",error);}
        }
    }
    /** Owner-only copy of the body missing from its own native entity tracker. Never a gameplay actor. */
    private static final class Operator extends Mannequin {
        final ServerPlayer owner;
        Operator(ServerPlayer owner){super(owner.level());this.owner=owner;
            applyImplicitComponent(net.minecraft.core.component.DataComponents.PROFILE,ResolvableProfile.createResolved(owner.getGameProfile()));
            ((me.skywars2.mixin.ChefMannequinAccess)(Object)this).skywars2$hideDescription(true);
            setMainArm(owner.getMainArm());sync();
        }
        void sync(){setPos(owner.position());setYRot(owner.getYRot());setXRot(owner.getXRot());setYBodyRot(owner.yBodyRot);setYHeadRot(owner.getYHeadRot());setPose(owner.getPose());
            byte layers=0;for(var part:net.minecraft.world.entity.player.PlayerModelPart.values())if(owner.isModelPartShown(part))layers|=(byte)part.getMask();
            getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION,layers);
            setInvisible(owner.isInvisible());
        }
        @Override public boolean shouldBeSaved(){return false;}
    }
    private static final class Session {
        final ServerPlayer player;final Drone drone;int born;float yaw,pitch;boolean attached;BodyChunk body;Seat seat;Operator operator;PlayerTeam hiddenTeam;
        final EnumMap<EquipmentSlot,ItemStack> appearance=new EnumMap<>(EquipmentSlot.class);
        Session(ServerPlayer player,Drone drone,int born){this.player=player;this.drone=drone;this.born=born;yaw=player.getYRot();pitch=player.getXRot();}
    }
    public boolean controlling(ServerPlayer p){var s=controllers.get(p.getUUID());return s!=null&&s.player==p;}
    int cameraSeatId(ServerPlayer p){var s=controllers.get(p.getUUID());return s==null||s.player!=p||s.seat==null?-1:s.seat.getId();}
    /** Position self-camera at the physical drone's eyes, independent of vanilla vehicle eye offsets. */
    private static void positionSeat(Session s){
        var dimensions=s.player.getDimensions(Pose.STANDING);
        double eyeOffset=s.seat.getPassengerRidingPosition(s.player).y-s.seat.getY()
                -dimensions.attachments().getClamped(EntityAttachment.VEHICLE,0,s.player.getYRot()).y+dimensions.eyeHeight();
        s.seat.setPos(s.drone.getEyePosition().add(0,-eyeOffset,0));
    }
    private void attach(Session s){
        s.seat=new Seat(s.player.level());positionSeat(s);
        var seat=s.seat;var p=s.player;
        p.connection.send(new ClientboundAddEntityPacket(seat.getId(),seat.getUUID(),seat.getX(),seat.getY(),seat.getZ(),0,0,EntityTypes.ITEM_DISPLAY,0,Vec3.ZERO,0));
        var data=seat.getEntityData().getNonDefaultValues();
        if(data!=null&&!data.isEmpty())p.connection.send(new ClientboundSetEntityDataPacket(seat.getId(),data));
        p.connection.send(passengers(seat.getId(),p.getId()));
        // Mouse input rotates LocalPlayer directly. Looking through the Vex instead
        // makes its limited flight heading the camera heading as well.
        p.connection.send(new ClientboundSetCameraPacket(p));
        // Presentation only: no server game-mode/abilities/inventory or tab-list change.
        // The client hand renderer and hotbar use this local mode, unlike body gameplay.
        p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE,net.minecraft.world.level.GameType.SPECTATOR.getId()));
        // Local mode selection also sets flying client-side; restore native abilities
        // immediately so hiding hands does not introduce a flight/FOV bonus.
        p.onUpdateAbilities();
        if(p.addPostEffect(HUD))p.sendPostEffects();s.attached=true;
        // The native real body remains public and vulnerable. Only its owner gets this mirror.
        s.operator=new Operator(p);var body=s.operator;
        p.connection.send(new ClientboundAddEntityPacket(body.getId(),body.getUUID(),body.getX(),body.getY(),body.getZ(),body.getXRot(),body.getYRot(),EntityTypes.MANNEQUIN,0,Vec3.ZERO,body.getYHeadRot()));
        p.connection.send(new ClientboundSetEntityDataPacket(body.getId(),body.getEntityData().getNonDefaultValues()));
        s.hiddenTeam=new PlayerTeam(new Scoreboard(),"sw2fpv"+Integer.toHexString(seat.getId()));s.hiddenTeam.setSeeFriendlyInvisibles(false);
        s.hiddenTeam.getPlayers().add(p.getScoreboardName());
        p.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(s.hiddenTeam,true));
        p.connection.send(selfFlags(p,true));
        var empty=new ArrayList<Pair<EquipmentSlot,ItemStack>>();for(var slot:EquipmentSlot.values())empty.add(Pair.of(slot,ItemStack.EMPTY));
        p.connection.send(new ClientboundSetEquipmentPacket(p.getId(),empty));updateOperator(s);
    }
    private static ClientboundSetEntityDataPacket selfFlags(ServerPlayer p,boolean hidden){
        byte flags=0;var values=p.getEntityData().getNonDefaultValues();if(values!=null)for(var value:values)if(value.id()==0)flags=(Byte)value.value();
        if(hidden)flags=(byte)((flags|0x20)&~0x40);
        return new ClientboundSetEntityDataPacket(p.getId(),List.of(new SynchedEntityData.DataValue<>(0,EntityDataSerializers.BYTE,flags)));
    }
    private void updateOperator(Session s){
        var body=s.operator;if(body==null)return;var p=s.player;
        var oldPosition=body.position();float yaw=body.getYRot(),pitch=body.getXRot(),head=body.getYHeadRot();body.sync();
        if(!oldPosition.equals(body.position())||yaw!=body.getYRot()||pitch!=body.getXRot())p.connection.send(ClientboundEntityPositionSyncPacket.of(body));
        if(head!=body.getYHeadRot())p.connection.send(new ClientboundRotateHeadPacket(body,(byte)Mth.floor(body.getYHeadRot()*256/360)));
        var dirty=body.getEntityData().packDirty();if(dirty!=null)p.connection.send(new ClientboundSetEntityDataPacket(body.getId(),dirty));
        // Eight visible native slots only; no inventory/NBT scan or gameplay inventory shell.
        var changed=new ArrayList<Pair<EquipmentSlot,ItemStack>>();
        for(var slot:EquipmentSlot.values()){var actual=p.getItemBySlot(slot);var previous=s.appearance.get(slot);
            if(previous==null||!ItemStack.matches(previous,actual)){var copy=actual.copy();s.appearance.put(slot,copy);changed.add(Pair.of(slot,copy));}}
        if(!changed.isEmpty())p.connection.send(new ClientboundSetEquipmentPacket(body.getId(),changed));
    }
    /** Recipient-only presentation, including later native effect/equipment updates. */
    @SuppressWarnings({"unchecked","rawtypes"})
    public Packet<?> presentation(ServerPlayer p,Packet<?> packet){
        var s=controllers.get(p.getUUID());if(s==null||s.player!=p||!s.attached)return packet;
        if(packet instanceof ClientboundBundlePacket bundle){var packets=new ArrayList<Packet<? super ClientGamePacketListener>>();for(var child:bundle.subPackets())packets.add((Packet)presentation(p,child));return new ClientboundBundlePacket(packets);}
        if(packet instanceof ClientboundSetEntityDataPacket data&&data.id()==p.getId()){
            var fields=new ArrayList<SynchedEntityData.DataValue<?>>();for(var field:data.packedItems())fields.add(field.id()==0?new SynchedEntityData.DataValue<>(0,EntityDataSerializers.BYTE,(byte)(((Byte)field.value()|0x20)&~0x40)):field);
            return new ClientboundSetEntityDataPacket(data.id(),fields);
        }
        if(packet instanceof ClientboundSetEquipmentPacket equipment&&equipment.getEntity()==p.getId())return new ClientboundSetEquipmentPacket(p.getId(),equipment.getSlots().stream().map(pair->Pair.of(pair.getFirst(),ItemStack.EMPTY)).toList());
        if(packet instanceof ClientboundSetPlayerInventoryPacket slot)return new ClientboundSetPlayerInventoryPacket(slot.slot(),ItemStack.EMPTY);
        if(packet instanceof ClientboundContainerSetSlotPacket slot&&slot.getContainerId()==0)return new ClientboundContainerSetSlotPacket(slot.getContainerId(),slot.getStateId(),slot.getSlot(),ItemStack.EMPTY);
        if(packet instanceof ClientboundContainerSetContentPacket contents&&contents.containerId()==0)return new ClientboundContainerSetContentPacket(0,contents.stateId(),Collections.nCopies(contents.items().size(),ItemStack.EMPTY),ItemStack.EMPTY);
        if(packet instanceof ClientboundSetPlayerTeamPacket team&&s.hiddenTeam!=null&&!team.getName().equals(s.hiddenTeam.getName())&&team.getPlayerAction()==ClientboundSetPlayerTeamPacket.Action.ADD&&team.getPlayers().contains(p.getScoreboardName()))
            return new ClientboundBundlePacket(List.of(team,ClientboundSetPlayerTeamPacket.createPlayerPacket(s.hiddenTeam,p.getScoreboardName(),ClientboundSetPlayerTeamPacket.Action.ADD)));
        return packet;
    }
    private void clearHud(ServerPlayer p){if(p.removePostEffect(HUD)&&p.connection!=null&&!p.isRemoved())p.sendPostEffects();}
    private void detach(Session s){
        var p=s.player;clearHud(p);
        if(!s.attached)return;
        s.attached=false;
        s.appearance.clear();
        if(p.isRemoved()||game.server().getPlayerList().getPlayer(p.getUUID())!=p)return;
        if(s.operator!=null)p.connection.send(new ClientboundRemoveEntitiesPacket(s.operator.getId()));
        if(s.hiddenTeam!=null){p.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(s.hiddenTeam));if(p.getTeam()!=null)p.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(p.getTeam(),p.getScoreboardName(),ClientboundSetPlayerTeamPacket.Action.ADD));}
        p.connection.send(selfFlags(p,false));
        var restored=new ArrayList<Pair<EquipmentSlot,ItemStack>>();for(var slot:EquipmentSlot.values())restored.add(Pair.of(slot,p.getItemBySlot(slot).copy()));
        p.connection.send(new ClientboundSetEquipmentPacket(p.getId(),restored));p.containerMenu.sendAllDataToRemote();
        if(s.seat!=null){p.connection.send(passengers(s.seat.getId()));p.connection.send(new ClientboundRemoveEntitiesPacket(s.seat.getId()));}
        p.connection.send(new ClientboundSetCameraPacket(p));
        p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE,p.gameMode.getGameModeForPlayer().getId()));
        p.onUpdateAbilities();
    }
    public static Vec3 viewpoint(ServerPlayer player){var g=SkyWars2.game();if(g==null||g.items().fpvDrone()==null)return player.position();
        var at=audioViewpoint(player);return at==null?player.position():at;}
    /** The private seat deliberately leaves ServerPlayer.getCamera() on its real body. */
    static Vec3 audioViewpoint(ServerPlayer player){var g=SkyWars2.game();if(g==null||g.items().fpvDrone()==null)return null;
        var s=g.items().fpvDrone().controllers.get(player.getUUID());return s!=null&&s.player==player&&s.attached&&s.drone.level()==player.level()?s.drone.getEyePosition():null;}
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
    public void loaded(Entity e){if(e instanceof ServerPlayer p){if(!controlling(p))clearHud(p);return;}if(!(e instanceof Drone d))return;if(!live(d)){d.discard();return;}bind(d);
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
    public void tick(){if(rewinding)return;if(advancing)throw new IllegalStateException("Reentrant FPV advance");advancing=true;try{int now=game.server().getTickCount();if(now==lastTick)return;lastTick=now;
        for(var s:List.copyOf(controllers.values())){
            if(!eligible(s.player)||s.player.level()!=s.drone.level()||!live(s.drone)){release(s.player);continue;}
            bodyTicket(s);if(s.attached)updateOperator(s);
            // A stationary/frozen drone may never cross a section: refresh native pairing
            // while prefetching too, before waiting for the camera attachment condition.
            s.player.level().getChunkSource().move(s.player);s.player.setLastClientInput(Input.EMPTY);
            // Native tracker has published the actor before its ID becomes the client camera/seat.
            if(!s.attached&&now-s.born>=2&&net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(s.drone).contains(s.player))attach(s);
        }
        for(var entry:List.copyOf(actors.entrySet())){var d=entry.getKey();var visual=entry.getValue();
            if(!live(d)){retire(d);continue;}var level=(ServerLevel)d.level();if(paused(d))continue;
            if(game.items().magic().mechanismSuppressedAt(level,d.position())){retire(d);continue;}
            var session=controllers.get(d.author);Vec3 direction=d.getLookAngle();
            if(session!=null&&session.drone==d){direction=turn(direction,Vec3.directionFromRotation(session.pitch,session.yaw),TURN_DEGREES);orientation(d,direction);}
            // Repeated approximate-trig Euler conversion would drift an unpiloted heading.
            var from=d.position();var to=from.add(direction);
            if(!Level.isInSpawnableBounds(BlockPos.containing(to))){retire(d);continue;}
            // Released drones retain the source's unloaded-chunk pause; no infinite chunk generation.
            if(!resident(level,to))continue;
            var wall=LbcTerra.wallHit(level,from,to,state->!state.is(net.minecraft.tags.TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("airs"))));
            var end=wall==null?to:wall.getLocation();
            var hit=ProjectileUtil.getEntityHitResult(level,d,from,end,new AABB(from,end).inflate(1),
                    entity->entity!=visual&&entity instanceof LivingEntity&&game.items().wands().validRoleTarget(entity,false),0);
            d.setPos(hit==null?end:hit.getLocation());visual.setPos(d.position());visual.setYRot(d.getYRot());visual.setXRot(d.getXRot());
            if(session!=null&&session.drone==d){
                if(session.attached){positionSeat(session);session.player.connection.send(ClientboundEntityPositionSyncPacket.of(session.seat));}
                session.player.level().getChunkSource().move(session.player);
            }
            LbcOverload.sound(level,d.position(),"minecraft:lbcsounds.drone_tick",net.minecraft.sounds.SoundSource.HOSTILE,.15f,1,35,.04f);
            if(hit!=null||wall!=null&&level.hasChunkAt(wall.getBlockPos()))explode(d);
        }
        }finally{advancing=false;}
    }
    public void explode(Drone d){if(!live(d))return;d.retired=true;var level=(ServerLevel)d.level();var accepted=source(d);
        try(var scope=game.items().spellSources().scope(accepted,level)){
            explosions++;game.items().exasperato().encounterBlast(d,level,d.position(),8,tnt->game.items().spellSources().bind(tnt,accepted));
        }finally{retire(d);}
    }
    /** Electrical contact is admission, independent of ordinary damage amount, immunity or hurt cooldown. */
    public boolean tryShortCircuit(Entity target){if(!(target instanceof Drone drone)||!live(drone))return false;explode(drone);return true;}
    private void retire(Drone d){d.retired=true;var session=controllers.get(d.author);if(session!=null&&session.drone==d)release(session.player);
        var visual=actors.remove(d);if(visual!=null)visual.discard();if(!d.isRemoved())d.discard();}
    private static ClientboundSetPassengersPacket passengers(int entity,int... ids){var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{buffer.writeVarInt(entity);buffer.writeVarIntArray(ids);return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer);}finally{buffer.release();}}
    public void release(ServerPlayer p){var session=controllers.get(p.getUUID());if(session==null||session.player!=p)return;controllers.remove(p.getUUID());releaseBody(session);
        p.setLastClientInput(Input.EMPTY);p.setShiftKeyDown(false);
        boolean attached=session.attached;detach(session);
        // Respawn replaces ServerPlayer. Never send an obsolete entity ID or re-add its chunk ticket.
        if(p.isRemoved()||game.server().getPlayerList().getPlayer(p.getUUID())!=p)return;
        if(attached)p.connection.teleport(p.getX(),p.getY(),p.getZ(),session.yaw,session.pitch);
        p.level().getChunkSource().move(p);
    }
    private void bodyTicket(Session s){var next=new BodyChunk(s.player.level(),s.player.chunkPosition());if(next.equals(s.body))return;releaseBody(s);
        s.body=next;if(tickets.merge(next,1,Integer::sum)==1)next.level.getChunkSource().addTicketWithRadius(BODY_TICKET,next.pos,3);}
    private void releaseBody(Session s){var key=s.body;if(key==null)return;s.body=null;int count=tickets.getOrDefault(key,1)-1;
        if(count>0)tickets.put(key,count);else{tickets.remove(key);key.level.getChunkSource().removeTicketWithRadius(BODY_TICKET,key.pos,3);}}
    public void forget(ServerPlayer p){var s=controllers.get(p.getUUID());if(s!=null)release(s.player);clearHud(p);}
    public void stop(){for(var s:List.copyOf(controllers.values()))release(s.player);for(var d:List.copyOf(actors.keySet()))retire(d);for(var p:game.server().getPlayerList().getPlayers())clearHud(p);lastTick=Integer.MIN_VALUE;rewinding=false;}
    public int count(){return actors.size();}public long explosions(){return explosions;}public int ticketCount(){return tickets.size();}
    public List<Drone> snapshot(){return List.copyOf(actors.keySet());}
    Checkpoint checkpoint(MatchDamageSources sources){return new Checkpoint(sources);}
    private void checkpointBoundary(){if(advancing)throw new IllegalStateException("Finish FPV callbacks before checkpointing");}
    /** Detach the abandoned client seats and explicit lease type; never use release()'s body teleport. */
    private void prepareCheckpoint(){
        rewinding=true;var old=List.copyOf(controllers.values());controllers.clear();
        for(var key:List.copyOf(tickets.keySet()))key.level.getChunkSource().removeTicketWithRadius(BODY_TICKET,key.pos,3);tickets.clear();
        for(var session:old)detach(session);
    }
    final class Checkpoint {
        private record Actor(MatchDamageSources.Ref drone,MatchDamageSources.Ref visual,boolean retired) { }
        private record Control(UUID key,MatchDamageSources.Ref player,MatchDamageSources.Ref drone,int born,float yaw,float pitch,boolean attached,BodyChunk body) { }
        private final String epoch=game.items().epoch();private final MatchSpellCheckpoint.Links links;
        @SuppressWarnings("unchecked") private final IdentityHashMap<Drone,Visual> traversal=(IdentityHashMap<Drone,Visual>)actors.clone();
        @SuppressWarnings("unchecked") private final HashMap<UUID,Session> controllerTraversal=(HashMap<UUID,Session>)controllers.clone();
        private final List<Actor> bodies=new ArrayList<>();private final List<Control> sessions=new ArrayList<>();private final Map<BodyChunk,Integer> leases=new HashMap<>(tickets);
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);private final long blasts=explosions;
        private boolean prepared,installed;
        private final Set<UUID> omittedControllers=new HashSet<>();
        Checkpoint(MatchDamageSources sources){stable(false);links=new MatchSpellCheckpoint.Links(sources);actors.forEach((drone,visual)->bodies.add(new Actor(links.key(drone),links.key(visual),drone.retired)));
            var counts=new HashMap<BodyChunk,Integer>();controllers.forEach((key,s)->{if(!key.equals(s.player.getUUID()))throw new IllegalStateException("FPV controller UUID mismatch");sessions.add(new Control(key,links.key(s.player),links.key(s.drone),s.born,s.yaw,s.pitch,s.attached,s.body));if(s.body!=null)counts.merge(s.body,1,Integer::sum);});if(!counts.equals(leases))throw new IllegalStateException("FPV body lease counts are incoherent");}
        private void stable(boolean frozen){if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore FPV on its stable frozen match thread");checkpointBoundary();game.items().spellSources().checkpointBoundary();}
        void prepareFrozen(){stable(true);prepareCheckpoint();omittedControllers.clear();prepared=true;installed=false;}
        Set<UUID> finishFrozen(java.util.function.Function<UUID,Entity> lookup){
            stable(true);if(!prepared)throw new IllegalStateException("Detach future FPV state first");var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
            @SuppressWarnings("unchecked") var restored=(IdentityHashMap<Drone,Visual>)traversal.clone();for(var image:bodies){if(!(links.get(image.drone) instanceof Drone drone)||!(links.get(image.visual) instanceof LbcFpvDrone.Visual visual)||visual.drone!=drone)throw new IllegalStateException("Restore canonical FPV actor/display links first");restored.put(drone,visual);}
            @SuppressWarnings("unchecked") var controls=(HashMap<UUID,Session>)controllerTraversal.clone();
            var omitted=new HashSet<UUID>();
            for(var image:sessions){
                if(!(links.get(image.player) instanceof ServerPlayer player)||!(links.get(image.drone) instanceof Drone drone)||!restored.containsKey(drone))
                    throw new IllegalStateException("Restore canonical FPV player/drone bodies first");
                // A retained offline body is private source data, never a live camera/controller or ticket owner.
                if(MatchOfflinePlayers.retained(player)){controls.remove(image.key);omitted.add(image.key);continue;}
                var session=new Session(player,drone,clock.time(image.born,game));session.yaw=image.yaw;session.pitch=image.pitch;session.body=image.body;controls.put(image.key,session);
            }
            var activeLeases=new HashMap<BodyChunk,Integer>();
            for(var session:controls.values())if(session.body!=null)activeLeases.merge(session.body,1,Integer::sum);
            activeLeases.keySet().forEach(key->MatchAuthorityCheckpoint.world(game,key.level));
            actors=restored;controllers=controls;omittedControllers.clear();omittedControllers.addAll(omitted);tickets.clear();tickets.putAll(activeLeases);
            for(var image:bodies)((Drone)links.get(image.drone)).retired=image.retired;explosions=blasts;lastTick=clock.last(game);
            for(var key:activeLeases.keySet())key.level.getChunkSource().addTicketWithRadius(BODY_TICKET,key.pos,3);
            for(var session:controls.values())session.player.level().getChunkSource().move(session.player);installed=true;return Set.of();
        }

        Set<UUID> publishFrozen(){
            stable(true);if(!installed)throw new IllegalStateException("Install FPV state before client presentation");var pending=new LinkedHashSet<UUID>();
            for(var image:sessions){if(omittedControllers.contains(image.key))continue;var s=controllers.get(image.key);if(s==null||game.server().getPlayerList().getPlayer(image.key)!=s.player)throw new IllegalStateException("FPV client body changed during publication");
                if(image.attached&&!s.attached){s.player.level().getChunkSource().move(s.player);if(s.player.isChangingDimension()||!s.player.connection.hasClientLoaded()||!net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(s.drone).contains(s.player)){pending.add(image.key);continue;}
                    attach(s);}
            }
            if(pending.isEmpty()){for(var image:sessions)if(!omittedControllers.contains(image.key))controllers.get(image.key).born=clock.time(image.born,game);lastTick=clock.last(game);rewinding=false;prepared=false;}
            return Set.copyOf(pending);
        }
        void cancelFrozen(){if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel FPV on its frozen native thread");prepareCheckpoint();omittedControllers.clear();rewinding=false;prepared=false;installed=false;}
    }

    static boolean cloneProtected(Entity actor){return actor instanceof LbcFpvDrone.Visual || actor instanceof Seat || actor instanceof Operator;}
    LbcCloneSkills.ActorImage cloneImage(Drone source) {
        if(!live(source))return null;
        var author=source.author;long order=source.order;var epoch=source.epoch;
        return new LbcCloneSkills.ActorImage() {
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var target=(Drone)clone;target.author=author;target.order=order;target.epoch=epoch;target.retired=false;
                // No controller Session is created: loaded regenerates one public visual, then normal autopilot flies straight.
            }
            public void rollback(Entity clone){unloaded(clone);}
        };
    }

    void directionChanged(Entity actor) {
        if(actor instanceof Drone drone){
            drone.setYHeadRot(drone.getYRot());drone.setYBodyRot(drone.getYRot());
            var visual=actors.get(drone);
            if(visual!=null)visual.snapTo(drone.getX(),drone.getY(),drone.getZ(),drone.getYRot(),drone.getXRot());
        }
    }

}
