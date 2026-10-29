package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Genuine native identity/death/dimension/manager/tracker paths, not deserialized lookalikes. */
public final class VanityActorChecks {
    private VanityActorChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity actors: "+why);}
    private static final class UnsavedCow extends Cow {
        final Object token=new Object();
        UnsavedCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final MatchCoreCheckpoint originalCore;
        final ChefChecks.Fixture base;
        final UUID id=GameChecks.offlineUuid("TestGamma");
        final boolean frozen;
        final ServerGamePacketListenerImpl oldWire;
        final ChunkTrackingView oldView;
        MatchActorCheckpoint originalActors,actors;
        MatchCoreCheckpoint core;
        MatchPlayerCheckpoint player;
        ServerPlayer gamma;
        ElementalGrimoireChecks.Recording wire;
        Cow cow;
        Cow tethered;
        net.minecraft.world.entity.decoration.LeashFenceKnotEntity knot;
        UnsavedCow rider;
        Pig vehicle;
        Skeleton skeleton;
        Snowball shot;
        PrimedTnt tnt;
        ItemEntity item;
        Entity alias,future,futureSlimeSource;
        Object token;
        List<UUID> passengerOrder;
        net.minecraft.world.scores.Objective objective;
        net.minecraft.world.scores.ScoreAccess cached;
        Vec3 at;
        final Vec3 motion=new Vec3(11.25,12.5,-13.75);
        int phase=-1,ticks,clock;
        boolean owned,closed;
        Fixture(GameController game) {
            this.game=game;frozen=game.server().tickRateManager().isFrozen();
            require(!frozen&&game.playing()&&!game.maps().busy()&&game.server().getPlayerList().getPlayer(id)==null,"exclusive real match and owned Gamma");
            originalCore=new MatchCoreCheckpoint(game);base=new ChefChecks.Fixture(game);oldWire=base.owner.connection;oldView=base.owner.getChunkTrackingView();
        }
        void init() {
            // Capture borrowed entities AFTER the deliberate native warm load.
            game.server().getLevel(Level.NETHER).getChunk(0,0);
            originalActors=MatchActorCheckpoint.captureRegistered(game);
            base.init();gamma=GameChecks.createBot(game,"TestGamma");owned=true;
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
            SkyWars2.LOGGER.info("[check] START vanity-actors");
        }
        <T extends Entity>T add(T entity,Vec3 position){entity.setPos(position);entity.setNoGravity(true);if(entity instanceof net.minecraft.world.entity.Mob mob){mob.setNoAi(true);mob.setPersistenceRequired();}
            require(base.owner.level().addFreshEntity(entity)&&!entity.isRemoved(),"native fixture actor registered");return entity;}
        void flatten(Packet<?> packet,List<Packet<?>> out){if(packet instanceof ClientboundBundlePacket b)for(var child:b.subPackets())flatten(child,out);else out.add(packet);}
        List<Packet<?>> packets(){var result=new ArrayList<Packet<?>>();for(var packet:wire.packets)flatten(packet,result);return result;}
        void ack(ServerPlayer p) {
            p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            // The owned Gamma uses the normal native memory wire; resetPosition remains native.
        }
        void prepare() {
            require(game.allowDamage(gamma)&&game.lives(id)==1,"actual operator enrollment");game.server().tickRateManager().setFrozen(true);
            base.move(gamma,base.at);gamma.setNoGravity(true);at=base.at.add(0,2,0);
            wire=new ElementalGrimoireChecks.Recording(base.owner);base.owner.connection=new ServerGamePacketListenerImpl(game.server(),wire,base.owner,
                    new CommonListenerCookie(base.owner.getGameProfile(),0,base.owner.clientInformation(),false));
            ack(base.owner);base.owner.setChunkTrackingView(ChunkTrackingView.of(base.owner.chunkPosition(),2));base.owner.level().getChunkSource().move(base.owner);
            cow=add(new Cow(EntityTypes.COW,base.owner.level()),at.add(2,0,0));
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);cow.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(
                    net.minecraft.resources.Identifier.parse("skywars2:actor_transient"),20,AttributeModifier.Operation.ADD_VALUE));cow.setHealth(55);
            cow.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND,13));cow.entityTags().add("actor.before");cow.setRemainingFireTicks(50000);
            cow.addEffect(new MobEffectInstance(MobEffects.SPEED,600,-2));cow.addEffect(new MobEffectInstance(MobEffects.SPEED,300,1));
            cow.addEffect(new MobEffectInstance(MobEffects.HASTE,1000,0));game.items().ownedEffects().grant(cow,"actor:a",new MobEffectInstance(MobEffects.HASTE,700,2),gamma);
            cow.setLeashedTo(gamma,true);cow.getLeashData().angularMomentum=.37;
            var fence=base.center.offset(5,2,3);base.owner.level().setBlockAndUpdate(fence,net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState());
            knot=net.minecraft.world.entity.decoration.LeashFenceKnotEntity.getOrCreateKnot(base.owner.level(),fence);
            tethered=add(new Cow(EntityTypes.COW,base.owner.level()),at.add(4,0,3));tethered.setLeashedTo(knot,true);tethered.getLeashData().angularMomentum=.19;
            vehicle=add(new Pig(EntityTypes.PIG,base.owner.level()),at.add(-3,0,0));
            rider=add(new UnsavedCow(base.owner.level()),at.add(-3,1,0));token=rider.token;
            require(rider.startRiding(vehicle,true,false)&&gamma.startRiding(vehicle,true,false),"ordered native mob/player riders");
            passengerOrder=vehicle.getPassengers().stream().map(Entity::getUUID).toList();
            skeleton=add(new Skeleton(EntityTypes.SKELETON,base.owner.level()),at.add(0,0,5));skeleton.setTarget(gamma);
            shot=add(new Snowball(EntityTypes.SNOWBALL,base.owner.level()),at.add(0,1,2));shot.setOwner(gamma);shot.setDeltaMovement(motion);
            tnt=add(new PrimedTnt(base.owner.level(),at.x+6,at.y,at.z,null),at.add(6,0,0));tnt.setFuse(120);tnt.setDeltaMovement(Vec3.ZERO);
            var stack=new ItemStack(Items.EMERALD,23);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Saved actor item"));
            item=add(new ItemEntity(base.owner.level(),at.x-6,at.y,at.z,stack),at.add(-6,0,0));item.setDeltaMovement(Vec3.ZERO);
            var board=game.server().getScoreboard();board.addPlayerToTeam(shot.getScoreboardName(),base.team);
            objective=board.addObjective("vanity_actor",net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,Component.literal("Actors"),net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,true,null);
            cached=board.getOrCreatePlayerScore(shot,objective);cached.set(101);
            actors=MatchActorCheckpoint.captureRegistered(game);player=new MatchPlayerCheckpoint(game,gamma);core=new MatchCoreCheckpoint(game);clock=game.matchTicks();phase=0;
        }
        void mutate() {
            gamma.stopRiding();rider.stopRiding();cow.removeLeash();knot.discard();
            cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            require(cow.hurtServer(base.owner.level(),cow.damageSources().genericKill(),10000)&&cow.isDeadOrDying(),"actual lethal native cow damage");cow.remove(Entity.RemovalReason.KILLED);
            rider.setHealth(1);rider.entityTags().add("actor.future");rider.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIRT,64));
            skeleton.setTarget(base.other);tnt.setFuse(3);item.getItem().setCount(1);item.discard();
            cached.set(999);
            alias=shot.teleport(new TeleportTransition(game.server().getLevel(Level.NETHER),new Vec3(.5,200,.5),Vec3.ZERO,0,0,Set.of(),TeleportTransition.DO_NOTHING));
            require(alias!=null&&alias!=shot&&shot.isRemoved()&&alias.getUUID().equals(shot.getUUID()),"genuine native cross-dimension replacement shares UUID");
            future=add(new ItemEntity(base.owner.level(),at.x,at.y,at.z,new ItemStack(Items.NETHERITE_INGOT,9)),at);
            var slimeSource=add(new Cow(EntityTypes.COW,base.owner.level()),at.add(7,0,0));slimeSource.addEffect(new MobEffectInstance(MobEffects.OOZING,1000));futureSlimeSource=slimeSource;
            gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;
            require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying()&&game.lives(id)==0,"actual owner death before UUID relationship rebind");
        }
        void restore() {
            var old=gamma;core.restoreFrozen();wire.packets.clear();actors.restoreBodiesFrozen();gamma=player.restoreFrozen();ack(gamma);
            require(old!=gamma&&game.server().getPlayerList().getPlayer(id)==gamma,"real native owner respawn replacement between actor body/link phases");
            require(actors.restoreLinksFrozen().isEmpty(),"all native relationship UUIDs resolved");
        }
        void verify() {
            var level=base.owner.level();
            for(var e:List.of(cow,tethered,knot,rider,vehicle,skeleton,shot,tnt,item))require(!e.isRemoved()&&level.getEntity(e.getUUID())==e&&level.getEntity(e.getId())==e,"same original instance in native UUID and ID registries: "+e.getType());
            require(cow.isAlive()&&cow.getHealth()==55&&cow.getAttributeValue(Attributes.MAX_HEALTH)==60&&cow.getItemBySlot(EquipmentSlot.MAINHAND).getCount()==13
                    &&cow.getRemainingFireTicks()==50000&&cow.entityTags().contains("actor.before"),"dead actor returns health/transient attributes/equipment/full fire timer/tags");
            require(cow.getEffect(MobEffects.SPEED).getAmplifier()==1&&((EffectLayers.Access)cow.getEffect(MobEffects.SPEED)).skywars2$layerHidden().getAmplifier()==-2,"native signed hidden layers restored");
            game.items().ownedEffects().withdraw(cow,"actor:a");require(cow.getEffect(MobEffects.HASTE).getAmplifier()==0&&cow.getEffect(MobEffects.HASTE).getDuration()==1000,"actor owned lease leaves captured external baseline");
            require(rider.token==token&&!rider.shouldBeSaved()&&rider.getHealth()==10&&!rider.entityTags().contains("actor.future")&&rider.getMainHandItem().isEmpty(),"unsaved custom subclass retains original identity and restores native values");
            require(vehicle.getPassengers().stream().map(Entity::getUUID).toList().equals(passengerOrder)&&vehicle.getPassengers().contains(gamma)
                    &&rider.getVehicle()==vehicle&&gamma.getVehicle()==vehicle,"ordered native passenger graph uses current player body: "+vehicle.getPassengers());
            require(skeleton.getTargetUnchecked()==gamma&&shot.getOwner()==gamma&&cow.getLeashHolder()==gamma&&cow.getLeashData().angularMomentum==.37,"mob target/projectile owner/leash resolve current UUID session");
            require(tethered.getLeashHolder()==knot&&tethered.getLeashData().angularMomentum==.19,"native fence-pos leash codec rebinds original knot immediately, without manufacturing another knot");
            require(shot.position().equals(at.add(0,1,2))&&shot.getDeltaMovement().equals(motion)&&shot.level()==level&&alias.isRemoved()
                    &&game.server().getLevel(Level.NETHER).getEntity(shot.getUUID())==null,"native dimension alias removed, original projectile and raw motion restored: phase="+phase+", pos="+shot.position()+", motion="+shot.getDeltaMovement()+", aliasRemoved="+alias.isRemoved());
            require(shot.getTeam()==base.team&&cached.get()==101&&game.server().getScoreboard().getOrCreatePlayerScore(shot,objective).get()==101,"dimension alias retirement preserves restored native team/score and cached score handle");
            require(tnt.getFuse()==120&&item.getItem().getCount()==23&&item.getItem().getHoverName().getString().equals("Saved actor item"),"native TNT fuse and item components restored");
            require(future.isRemoved()&&futureSlimeSource.isRemoved()&&MatchActorCheckpoint.captureRegistered(game).size()==actors.size(),"future items/death drops/oozing actor removed without death-effect children");
            require(packets().stream().filter(ClientboundAddEntityPacket.class::isInstance).map(ClientboundAddEntityPacket.class::cast).anyMatch(p->p.getId()==shot.getId())
                    &&packets().stream().filter(ClientboundSetPassengersPacket.class::isInstance).map(ClientboundSetPassengersPacket.class::cast).anyMatch(p->p.getVehicle()==vehicle.getId()),"actual native tracker publishes restored entity/passengers to connected viewer");
            require(actors.restoreLinksFrozen().isEmpty()&&vehicle.getPassengers().stream().map(Entity::getUUID).toList().equals(passengerOrder),"repeated link phase is idempotent");
        }
        boolean step() {
            require(++ticks<140,"bounded native actor fixture phase="+phase);
            if(phase==-1){prepare();return false;}
            if(phase==0){mutate();restore();verify();shot.setDeltaMovement(Vec3.ZERO);cow.clearFire();game.server().tickRateManager().setFrozen(false);phase=1;return false;}
            if(phase==1){if(game.matchTicks()-clock<4)return false;game.server().tickRateManager().setFrozen(true);mutate();restore();verify();
                gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity actor offline link fixture")));
                // Native PlayerList.remove unloads a sole player's whole vehicle tree.
                // The coordinator's body phase precedes pending-link reconciliation.
                actors.restoreBodiesFrozen();
                require(actors.restoreLinksFrozen().equals(Set.of(id))&&skeleton.getTargetUnchecked()==null,"offline native target/owner/leash/rider remains explicit pending UUID");
                gamma=GameChecks.createBot(game,"TestGamma");core.restoreFrozen();actors.restoreBodiesFrozen();gamma=player.restoreFrozen();ack(gamma);
                require(actors.restoreLinksFrozen().isEmpty(),"explicit link replay after actual reconnect resolves new native UUID session");verify();return true;}
            return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);
            try{
                if(owned){var p=game.server().getPlayerList().getPlayer(id);if(p!=null)p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity actor cleanup")));}
                if(originalActors!=null){originalActors.restoreBodiesFrozen();originalActors.restoreLinksFrozen();}
                base.owner.connection=oldWire;base.owner.setChunkTrackingView(oldView);base.close();originalCore.restoreFrozen();
            }finally{game.server().tickRateManager().setFrozen(frozen);}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-actors (same native body identities after real death/dimension replacement, raw motion/transient attributes/hidden effects/owned leases, current-player owner/target/leash and ordered riders, item/TNT state, future removal without death-effect spawning, native registry/tracker packets, repeated restoration across real ticks; private AI/service/world coordinator remains gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-actors",error);}}
}
