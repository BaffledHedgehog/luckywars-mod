package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Native packet listener, actual tracker/tickets and real actor death; no physical client claim. */
public final class FpvDroneChecks {
    private static final net.minecraft.resources.Identifier HUD=net.minecraft.resources.Identifier.parse("lbc:fpv_drone"),NEIGHBOR=net.minecraft.resources.Identifier.parse("minecraft:invert");
    private FpvDroneChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("FPV drone: "+why);}
    private static Object read(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static ItemStack controller(){var s=new ItemStack(Items.CARROT_ON_A_STICK,2);var d=new CompoundTag();d.putInt("drone_pult",1);d.putInt("raycastspell",1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));return s;}
    private static double angle(Vec3 a,Vec3 b){return Math.toDegrees(Math.acos(net.minecraft.util.Mth.clamp(a.normalize().dot(b.normalize()),-1,1)));}
    private static void flatten(Packet<?> p,List<Packet<?>> out){out.add(p);if(p instanceof ClientboundBundlePacket b)b.subPackets().forEach(x->flatten(x,out));}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer owner,beta;
        final ShishEncounterChecks.State saved,otherSaved;final SauvojenMeditationChecks.Attribution attribution;
        final Set<Long> forced=new HashSet<>();final Vec3 origin;final boolean offhand;ShishEncounterChecks.Client client;
        final List<net.minecraft.resources.Identifier> postEffects;ShishEncounterChecks.Client observerClient;
        LbcFpvDrone.Drone drone;Vec3 priorDirection,priorPosition;int phase,at,born,attachedSeat=-1;boolean stopped,closed;long explosions;
        Fixture(GameController game){this.game=game;level=game.maps().level();owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));saved=new ShishEncounterChecks.State(game,owner);otherSaved=new ShishEncounterChecks.State(game,beta);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,beta));offhand=game.config().offhandWands;
            postEffects=List.copyOf(owner.getPostEffects());origin=new Vec3(0,Math.min(300,level.getMaxY()-24),0);at=game.server().getTickCount();
        }
        @SuppressWarnings("unchecked") List<Packet<?>> packets(){var out=new ArrayList<Packet<?>>();for(var p:(List<Packet<?>>)read(read(client,"wire"),"packets"))flatten(p,out);return out;}
        void init(){require(game.items().fpvDrone().count()==0&&game.items().fpvDrone().ticketCount()==0,"exclusive empty service");game.config().offhandWands=false;
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){long key=ChunkPos.pack(x,z);if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);}
            for(var p:List.of(owner,beta)){p.stopUsingItem();p.removeAllEffects();p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setSprinting(false);p.setShiftKeyDown(false);p.setOnGround(true);
                p.entityTags().removeAll(Set.of("spectator","time_stopped","time_stopped_lol","strict_map_object","rcdenyalways","magic_academy","sin_greed_active","nomagic_active"));
                game.players().get(p.getUUID()).lbcSkill=0;game.players().get(p.getUUID()).lbcTrinket=0;Arrays.fill(game.players().get(p.getUUID()).trinkets,0);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.ARMOR).setBaseValue(0);p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);p.setHealth(1000);}
            owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+35,origin.y,origin.z,Set.of(),0,0,true);client=new ShishEncounterChecks.Client(game,owner);observerClient=new ShishEncounterChecks.Client(game,beta);owner.addPostEffect(NEIGHBOR);
            // Native power8 blast footprint must be all air so this test never mutates map terrain.
            var launch=origin.add(0,7,-48);for(var pos:BlockPos.betweenClosed(BlockPos.containing(launch.add(-20,-20,-20)),BlockPos.containing(launch.add(20,20,40))))require(level.getBlockState(pos).isAir(),"resident isolated blast air "+pos);
            launch();
        }
        void launch(){owner.setItemInHand(InteractionHand.MAIN_HAND,controller());client.ack();var s=owner.getMainHandItem();
            require(owner.gameMode.useItem(owner,level,s,InteractionHand.MAIN_HAND).consumesAction()&&s.getCount()==1,"actual target-free pult input and one debit");
            require(game.items().fpvDrone().count()==1&&game.items().fpvDrone().controlling(owner)&&!owner.isPassenger()&&owner.getCamera()==owner,"real body remains independent of packet-only camera/seat");
            drone=game.items().fpvDrone().snapshot().getFirst();require(drone.getHealth()==20&&drone.getMaxHealth()==20&&!drone.isPushable()&&!drone.canCollideWith(owner),"native shootable20HP nonpushing actor");
            require(drone.position().distanceToSqr(origin.add(0,7,-48))<1e-8,"authored fixed-point spawn further/higher");
            priorDirection=drone.getLookAngle();born=at=game.server().getTickCount();
        }
        boolean tick(){client.ack();int now=game.server().getTickCount();require(now-at<40,"bounded native phase "+phase);
            require(owner.position().distanceToSqr(origin)<1e-6,"controller body never moves into drone");
            if(phase==0){if(now-born<3)return false;
                if(!attached())return false;
                var seen=packets();int spawn=-1,camera=-1;
                for(int n=0;n<seen.size();n++){var p=seen.get(n);if(p instanceof ClientboundAddEntityPacket add&&add.getId()==drone.getId())spawn=n;if(p instanceof ClientboundSetCameraPacket c&&c.getEntity(level)==owner)camera=n;}
                require(spawn>=0&&camera>spawn,"actual native pairing precedes camera attachment");
                attachedPresentation();operatorPresentation();audioViewpoint();quietInvisibility();
                require(level.sendParticles(owner,net.minecraft.core.particles.ParticleTypes.END_ROD,false,false,drone.getX(),drone.getY(),drone.getZ(),1,0,0,0,0),"native particle range uses camera instead of distant body");
                long sounds=packets().stream().filter(ClientboundSoundPacket.class::isInstance).count();
                var sound=net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.parse("minecraft:block.note_block.pling"));
                level.playSound(null,drone.getX(),drone.getY(),drone.getZ(),sound,net.minecraft.sounds.SoundSource.MASTER,1,1);
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).count()==sounds+1,"native sound range follows remote camera");
                LbcOverload.sound(level,drone.position(),"minecraft:block.note_block.pling",1,1,5,0);
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).count()==sounds+2,"shared custom sound range follows remote camera");
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).anyMatch(s->s.getSound().value().location().toString().equals("minecraft:lbcsounds.drone_signal")&&s.getSource()==net.minecraft.sounds.SoundSource.PLAYERS&&new Vec3(s.getX(),s.getY(),s.getZ()).distanceToSqr(origin)<1),"launch sound remains at body before camera attachment");
                require(packets().stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).anyMatch(s->s.getSound().value().location().toString().equals("minecraft:lbcsounds.drone_tick")&&s.getSource()==net.minecraft.sounds.SoundSource.HOSTILE),"authored flight hostile sound category");
                require(packets().stream().noneMatch(p->p instanceof ClientboundSetCameraPacket c&&c.getEntity(level)==drone),"native camera stays on locally mouse-controlled player, not capped drone heading");
                require(packets().stream().anyMatch(p->p instanceof ClientboundSetPassengersPacket c&&c.getVehicle()==attachedSeat&&Arrays.equals(c.getPassengers(),new int[]{owner.getId()})),"only controller receives transient display seat so vanilla client keeps sending free look");
                require(owner.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view&&view.center().equals(drone.chunkPosition()),"actual native chunk stream centers on drone");require(game.items().fpvDrone().ticketCount()==1,"body has independently owned simulation ticket");
                client.listener.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(origin.add(40,20,20),1470,89,false,false));
                require(owner.position().equals(origin)&&owner.getYRot()==0,"movement/look packet cannot move or turn body");
                var session=session();require((Float)read(session,"yaw")==30f&&(Float)read(session,"pitch")==89f,"large free mouse yaw/pitch reaches session immediately without drone turn clamp");
                client.listener.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(true,false,false,false,true,false,true)));
                require(owner.getLastClientInput().equals(Input.EMPTY),"walking and jump input consumed separately");
                require(!owner.gameMode.useItem(owner,level,owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&owner.getMainHandItem().getCount()==1,"item use blocked while piloting");
                priorDirection=drone.getLookAngle();at=now;phase=1;return false;
            }
            if(phase==1){double turned=angle(priorDirection,drone.getLookAngle());require(turned>2.9&&turned<=3.001,"total angular step limited to three degrees: "+turned);priorDirection=drone.getLookAngle();
                require(packets().stream().anyMatch(p->p instanceof ClientboundEntityPositionSyncPacket movement&&movement.id()==attachedSeat),"moving drone publishes transient seat position without client rotation correction");
                if(now-at<5)return false;game.items().timeStop().begin(level,100);stopped=true;priorPosition=drone.position();priorDirection=drone.getLookAngle();at=now;phase=2;return false;}
            if(phase==2){require(drone.position().equals(priorPosition)&&angle(drone.getLookAngle(),priorDirection)<1e-5,"actual Time Stop pauses drone position and steering");if(now-at<3)return false;
                game.items().timeStop().end(level);stopped=false;client.ack();
                client.listener.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(false,false,false,false,false,true,false)));
                require(!game.items().fpvDrone().controlling(owner)&&game.items().fpvDrone().ticketCount()==0&&!drone.isRemoved(),"Shift releases camera/body ticket and preserves flying actor");
                restoredPresentation("Shift");
                require(packets().stream().anyMatch(p->p instanceof ClientboundSetCameraPacket c&&c.getEntity(level)==owner),"return camera packet");client.ack();
                priorDirection=drone.getLookAngle();priorPosition=drone.position();client.listener.handleMovePlayer(new ServerboundMovePlayerPacket.Rot(-90,45,true,false));at=now;phase=3;return false;}
            if(phase==3){require(angle(drone.getLookAngle(),priorDirection)<1e-5,"released drone ignores subsequent player look");if(now-at<3)return false;require(drone.position().distanceToSqr(priorPosition)>4,"released drone keeps flying straight");
                explosions=game.items().fpvDrone().explosions();require(drone.hurtServer(level,drone.damageSources().playerAttack(beta),20),"native20 damage accepted against actual actor");
                require(drone.isRemoved()&&game.items().fpvDrone().count()==0&&game.items().fpvDrone().explosions()==explosions+1,"shot-down actor explodes once without helper creeper");
                launch();at=now;phase=4;return false;}
            if(phase==4){if(now-at<3||!attached())return false;attachedPresentation();game.items().fpvDrone().forget(owner);require(!game.items().fpvDrone().controlling(owner)&&game.items().fpvDrone().ticketCount()==0&&!drone.isRemoved(),"forget/disconnect boundary releases only control");
                restoredPresentation("forget");game.items().fpvDrone().stop();launch();at=now;phase=5;return false;}
            if(phase==5){if(now-at<3||!attached())return false;attachedPresentation();
                shortCircuits();
                sharedElectricalSegments();
                deathMessages();
                game.items().fpvDrone().stop();require(game.items().fpvDrone().count()==0&&game.items().fpvDrone().ticketCount()==0,"exact owned stop cleanup");return true;}
            throw new IllegalStateException("phase");
        }
        Object session(){return ((Map<?,?>)read(game.items().fpvDrone(),"controllers")).get(owner.getUUID());}
        boolean attached(){var session=session();return session!=null&&(Boolean)read(session,"attached");}
        int latestClientMode(){return packets().stream().filter(ClientboundGameEventPacket.class::isInstance).map(ClientboundGameEventPacket.class::cast).filter(p->p.getEvent()==ClientboundGameEventPacket.CHANGE_GAME_MODE).reduce((a,b)->b).map(p->(int)p.getParam()).orElse(-1);}
        void authoritativeBody(){
            require(owner.gameMode()==net.minecraft.world.level.GameType.SURVIVAL&&!owner.isSpectator()&&!owner.getAbilities().invulnerable&&!owner.getAbilities().mayfly&&!owner.getAbilities().flying&&!owner.getAbilities().instabuild,"client presentation never changes authoritative mode/abilities");
            require(packets().stream().filter(ClientboundPlayerInfoUpdatePacket.class::isInstance).map(ClientboundPlayerInfoUpdatePacket.class::cast).flatMap(p->p.entries().stream()).noneMatch(e->e.profileId().equals(owner.getUUID())&&e.gameMode()==net.minecraft.world.level.GameType.SPECTATOR),"client-only hand hiding never changes PlayerInfo mode");
        }
        void attachedPresentation(){
            attachedSeat=game.items().fpvDrone().cameraSeatId(owner);require(attachedSeat>=0&&attachedSeat!=drone.getId()&&level.getEntity(attachedSeat)==null,"camera seat is transient presentation only, without server-world body");
            var wire=packets();int spawn=-1,metadata=-1,mount=-1,camera=-1;for(int i=0;i<wire.size();i++){var packet=wire.get(i);if(packet instanceof ClientboundAddEntityPacket p&&p.getId()==attachedSeat){require(p.getType()==EntityTypes.ITEM_DISPLAY,"free camera seat projects an empty display");spawn=i;}if(packet instanceof ClientboundSetEntityDataPacket p&&p.id()==attachedSeat&&metadata<0)metadata=i;if(packet instanceof ClientboundSetPassengersPacket p&&p.getVehicle()==attachedSeat)mount=i;if(packet instanceof ClientboundSetCameraPacket p&&p.getEntity(level)==owner)camera=i;}require(spawn>=0&&metadata>spawn&&mount>metadata&&camera>=mount,"display seat and metadata are published before passenger and self-camera packets");
            require(latestClientMode()==3,"owner-only client spectator rendering hides hands and hotbar");authoritativeBody();
            var seat=(Entity)read(session(),"seat");var standing=owner.getDimensions(net.minecraft.world.entity.Pose.STANDING);
            double eyeY=seat.getPassengerRidingPosition(owner).y-standing.attachments().getClamped(net.minecraft.world.entity.EntityAttachment.VEHICLE,0,owner.getYRot()).y+standing.eyeHeight();
            require(Math.abs(eyeY-drone.getEyeY())<.00001,"native passenger/self-camera eye height matches drone eyes");
            require(owner.getPostEffects().contains(HUD)&&owner.getPostEffects().contains(NEIGHBOR),"own FPV HUD is composed with unrelated post effect");
            require(!beta.getPostEffects().contains(HUD),"FPV HUD never changes another player's effects");
            var other=new ArrayList<Packet<?>>();for(var packet:(List<Packet<?>>)read(read(observerClient,"wire"),"packets"))flatten(packet,other);
            require(other.stream().noneMatch(p->p instanceof ClientboundGameEventPacket e&&e.getEvent()==ClientboundGameEventPacket.CHANGE_GAME_MODE&&e.getParam()==3),"hand/HUD presentation mode packet is owner-only");
            require(other.stream().noneMatch(p->p instanceof ClientboundAddEntityPacket e&&e.getId()==attachedSeat),"transient seat is never sent to another player");
        }
        void audioViewpoint(){
            var eye=drone.getEyePosition();require(eye.equals(LbcFpvDrone.viewpoint(owner))&&owner.getCamera()==owner,"shared sound audience follows actual private FPV camera eye, independent of native body camera");
            // Audio audience follows the remote eye; gains and source coordinates remain authored.
            var buzz=packets().stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(packet->packet.getSound().value().location().toString().equals("minecraft:lbcsounds.drone_tick")).reduce((a,b)->b).orElseThrow();
            require(new Vec3(buzz.getX(),buzz.getY(),buzz.getZ()).distanceToSqr(eye)<4&&buzz.getVolume()==.15f&&buzz.getPitch()==1&&buzz.getSource()==net.minecraft.sounds.SoundSource.HOSTILE,"authored looping buzz reaches actual FPV listener at full legacy gain, never the remote body/minimum-volume fallback");
        }
        @SuppressWarnings("unchecked") void operatorPresentation(){
            var service=game.items().fpvDrone();var operator=(net.minecraft.world.entity.decoration.Mannequin)read(session(),"operator");
            require(operator!=null&&level.getEntity(operator.getId())==null&&operator.position().equals(owner.position()),"owner-only Mannequin mirrors real body without a second gameplay actor");
            require(operator.getProfile().partialProfile().equals(owner.getGameProfile()),"operator mirror retains exact resolved skin profile");
            require(packets().stream().anyMatch(packet->packet instanceof ClientboundAddEntityPacket add&&add.getId()==operator.getId()&&add.getType()==EntityTypes.MANNEQUIN),"owner receives native Mannequin body appearance");
            require(packets().stream().filter(ClientboundSetEquipmentPacket.class::isInstance).map(ClientboundSetEquipmentPacket.class::cast).anyMatch(packet->packet.getEntity()==operator.getId()&&packet.getSlots().stream().anyMatch(pair->pair.getFirst()==EquipmentSlot.MAINHAND&&ItemStack.matches(pair.getSecond(),owner.getMainHandItem()))),"body mirror shows real remaining controller with all native components");
            var visual=(Entity)((Map<?,?>)read(service,"actors")).get(drone);require(visual.broadcastToPlayer(owner),"actual drone model remains visible to its own controller in F5");
            require(packets().stream().filter(ClientboundSetEntityDataPacket.class::isInstance).map(ClientboundSetEntityDataPacket.class::cast).anyMatch(packet->packet.id()==owner.getId()&&packet.packedItems().stream().anyMatch(value->value.id()==0&&value.value() instanceof Byte flags&&(flags&0x20)!=0)),"owner self metadata hides local player model");
            var before=owner.getMainHandItem().copy();var normal=new ClientboundSetEquipmentPacket(owner.getId(),List.of(com.mojang.datafixers.util.Pair.of(EquipmentSlot.MAINHAND,before)));
            var hidden=(ClientboundSetEquipmentPacket)service.presentation(owner,normal);
            require(hidden.getSlots().getFirst().getSecond().isEmpty()&&ItemStack.matches(owner.getMainHandItem(),before),"self equipment presentation hides rendered item without editing authoritative inventory");
            require(service.presentation(beta,normal)==normal,"other players receive original body equipment");
            var observer=new ArrayList<Packet<?>>();for(var packet:(List<Packet<?>>)read(read(observerClient,"wire"),"packets"))flatten(packet,observer);
            require(observer.stream().noneMatch(packet->packet instanceof ClientboundAddEntityPacket add&&add.getId()==operator.getId()),"body mirror remains owner-only while public real player stays native");
        }
        void restoredPresentation(String boundary){
            require(latestClientMode()==owner.gameMode().getId(),boundary+" restores actual client game mode");authoritativeBody();
            require(packets().stream().filter(ClientboundSetEquipmentPacket.class::isInstance).map(ClientboundSetEquipmentPacket.class::cast).anyMatch(packet->packet.getEntity()==owner.getId()&&packet.getSlots().stream().anyMatch(pair->pair.getFirst()==EquipmentSlot.MAINHAND&&ItemStack.matches(pair.getSecond(),owner.getMainHandItem()))),boundary+" restores actual self equipment presentation");
            require(attachedSeat<0||packets().stream().anyMatch(p->p instanceof ClientboundRemoveEntitiesPacket removed&&removed.entityIds().contains(attachedSeat)),boundary+" removes transient camera seat");
            var packet=packets().stream().filter(ClientboundPlayerAbilitiesPacket.class::isInstance).map(ClientboundPlayerAbilitiesPacket.class::cast).reduce((a,b)->b).orElseThrow();var expected=new ClientboundPlayerAbilitiesPacket(owner.getAbilities());
            require(packet.isInvulnerable()==expected.isInvulnerable()&&packet.isFlying()==expected.isFlying()&&packet.canFly()==expected.canFly()&&packet.canInstabuild()==expected.canInstabuild()&&packet.getFlyingSpeed()==expected.getFlyingSpeed()&&packet.getWalkingSpeed()==expected.getWalkingSpeed(),boundary+" restores latest authoritative abilities packet");
            require(!owner.getPostEffects().contains(HUD)&&owner.getPostEffects().contains(NEIGHBOR),boundary+" removes only own HUD effect");
        }
        @SuppressWarnings("unchecked") void quietInvisibility(){
            drone.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED,100,0,false,true,true),owner);drone.updateDataBeforeSync();
            try{var field=LivingEntity.class.getDeclaredField("DATA_EFFECT_PARTICLES");field.setAccessible(true);var accessor=(net.minecraft.network.syncher.EntityDataAccessor<List<net.minecraft.core.particles.ParticleOptions>>)field.get(null);
                require(drone.isInvisible()&&drone.getEntityData().get(accessor).isEmpty(),"native dirty effect synchronization preserves invisible drone with no particles");
                var packet=new ClientboundSetEntityDataPacket(drone.getId(),drone.getEntityData().getNonDefaultValues());require(packet.packedItems().stream().anyMatch(v->v.id()==0&&v.value() instanceof Byte flags&&(flags&0x20)!=0),"actual native metadata retains invisibility bit after visible effect application");
                drone.removeEffect(net.minecraft.world.effect.MobEffects.SPEED);drone.updateDataBeforeSync();require(drone.isInvisible()&&drone.getEntityData().get(accessor).isEmpty(),"native effect removal also preserves quiet invisibility");
            }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        }
        void shortCircuits(){
            var service=game.items().fpvDrone();
            for(int kind=0;kind<4;kind++){
                if(kind>0)launch();
                var victim=drone;victim.setPermanentlyInvulnerable(true);victim.setInvulnerableTime(100);victim.damageCooldownTime=100;
                victim.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.RESISTANCE,100,4,false,false),owner);
                long before=service.explosions();
                if(kind==0)require(victim.hurtServer(level,victim.damageSources().source(LbcDamage.ELECTRIC,beta,beta),0),"zero-damage electrical contact bypasses immunity/cooldown");
                else if(kind==1)victim.thunderHit(level,new LightningBolt(EntityTypes.LIGHTNING_BOLT,level));
                else if(kind==2)require(victim.hurtServer(level,victim.damageSources().source(LbcDamage.LIGHTNING,beta,beta),.01f),"custom lightning immediately shorts protected drone");
                else require(service.tryShortCircuit(victim),"shared producer electrical contact detonates before ordinary damage admission");
                require(victim.isRemoved()&&service.count()==0&&service.explosions()==before+1&&!service.controlling(owner)&&service.ticketCount()==0,"electrical short circuit explodes once and releases owned control/tickets kind="+kind);
                restoredPresentation("electrical short circuit "+kind);
                require(!service.tryShortCircuit(victim)&&service.explosions()==before+1,"repeated electrical callbacks cannot duplicate blast");
            }
        }
        void sharedElectricalSegments(){
            var service=game.items().fpvDrone();var board=game.server().getScoreboard();var previous=owner.getTeam();
            String name="fpv_el_"+Integer.toHexString(owner.getId());require(board.getPlayerTeam(name)==null,"exclusive electrical alliance fixture");
            var team=board.addPlayerTeam(name);team.setAllowFriendlyFire(false);var allied=new ArrayList<LbcFpvDrone.Drone>();
            try{
                board.addPlayerToTeam(owner.getScoreboardName(),team);
                for(var type:List.of(LbcDamage.ELECTRIC,LbcDamage.YELLOW_BOLT,LbcDamage.YELLOW_SPIN,LbcDamage.YELLOW_CONDUCTION)){
                    launch();var victim=drone;allied.add(victim);board.addPlayerToTeam(victim.getScoreboardName(),team);
                    require(owner.getUUID().equals(victim.author)&&owner.isAlliedTo(victim),"actual own drone belongs to owner's native team");
                    victim.setPermanentlyInvulnerable(true);victim.setInvulnerableTime(100);victim.damageCooldownTime=100;
                    var damage=owner.damageSources().source(type,owner,owner);require(LbcDamage.isElectricalContact(damage),"shared contact classifier includes "+type.identifier());
                    long before=service.explosions();var center=victim.getBoundingBox().getCenter();
                    // This passes through the broad query volume but misses the exact native body.
                    LbcDamage.hurtSegment(level,damage,center.add(-2,0,.8),center.add(2,0,.8),Vec3.ZERO,.2,0,1,entity->false,false);
                    require(!victim.isRemoved()&&service.explosions()==before&&service.controlling(owner),"nearby electrical line misses own allied drone "+type.identifier());
                    // The ordinary allegiance predicate rejects every entity. Electrical contact must
                    // still reach an intersected drone, before immunity, hurt cooldown or damage amount.
                    LbcDamage.hurtSegment(level,damage,center.add(-2,0,0),center.add(2,0,0),Vec3.ZERO,.2,0,1,entity->false,false);
                    require(victim.isRemoved()&&service.explosions()==before+1&&service.count()==0&&!service.controlling(owner)&&service.ticketCount()==0,"exact shared electrical line shorts own allied drone "+type.identifier());
                    require(!service.tryShortCircuit(victim)&&service.explosions()==before+1,"shared line explosion remains idempotent");
                }
            }finally{
                for(var victim:allied)if(victim.getTeam()==team)board.removePlayerFromTeam(victim.getScoreboardName(),team);
                if(owner.getTeam()==team)board.removePlayerFromTeam(owner.getScoreboardName(),team);
                if(previous!=null)board.addPlayerToTeam(owner.getScoreboardName(),previous);board.removePlayerTeam(team);
            }
        }
        void deathMessages(){
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
            try{
                game.items().fpvDrone().stop();launch();
                // Use the real launched actor and native blast/death pipeline, not a fabricated damage receipt.
                var victim=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);
                victim.setNoAi(true);victim.setNoGravity(true);victim.setPos(drone.position().add(2,0,0));
                victim.setCustomName(net.minecraft.network.chat.Component.literal("DroneBlastVictim"));victim.setHealth(1);
                require(level.addFreshEntity(victim),"native blast victim admitted");
                owner.getMainHandItem().set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named controller"));
                game.items().fpvDrone().explode(drone);
                require(victim.isDeadOrDying()&&victim.getLastDamageSource()!=null,"actual native drone explosion kills victim");
                var damage=victim.getLastDamageSource();var message=victim.getCombatTracker().getDeathMessage();
                require(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                        &&text.getKey().equals("death.attack.skywars2.kamikaze_drone"),"actual combat death message uses drone translation key: "+message);
                require(message.getString().contains("DroneBlastVictim")&&message.getString().contains(owner.getGameProfile().name()),"victim and real player author, even with named controller");
                require(message.getString().contains("взорван Камикадзе Дроном игрока"),"Russian fallback text survives UTF-8 encoding");
                require(game.items().indirectKills().policyCause(victim,damage).player().equals(owner.getUUID()),"existing accepted kill author retained");
                var ordinary=new net.minecraft.world.entity.item.PrimedTnt(level,drone.getX(),drone.getY(),drone.getZ(),owner);
                var vanilla=net.minecraft.world.level.Explosion.getDefaultDamageSource(level,ordinary).getLocalizedDeathMessage(victim);
                require(!(vanilla.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text&&text.getKey().startsWith("death.attack.skywars2.kamikaze_drone")),"ordinary TNT keeps its native message");
                var nonBlast=damage.getDirectEntity().damageSources().source(net.minecraft.world.damagesource.DamageTypes.MAGIC,damage.getDirectEntity(),owner);
                require(game.items().fpvDrone().deathMessage(nonBlast,victim)==null,"weapon marker does not rename nonexplosive damage");
            }finally{game.items().fpvDrone().stop();var created=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(!before.contains(e))created.add(e);});created.forEach(Entity::discard);}
        }
        public void close(){if(closed)return;closed=true;try{if(stopped)game.items().timeStop().end(level);game.items().fpvDrone().stop();game.config().offhandWands=offhand;
            saved.restore(game);otherSaved.restore(game);owner.clearPostEffects();for(var effect:postEffects)owner.addPostEffect(effect);if(client!=null)client.ack();attribution.close();}
            finally{if(observerClient!=null)observerClient.close();if(client!=null)client.close();for(long key:forced)level.setChunkForced(ChunkPos.getX(key),ChunkPos.getZ(key),false);}}
    }
    private static Fixture running;
    public static void start(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;throw e;}SkyWars2.LOGGER.info("[check] START fpv-drone");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS fpv-drone (actual target-free item input, camera/seat packets, native remote chunk view/body tickets, look/input interception, bounded steering, Time Stop, Shift straight flight, native20HP shootdown/power8 blast and owned lifecycle; no physical-client qualification)");}
        catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL fpv-drone",e);}}
}
