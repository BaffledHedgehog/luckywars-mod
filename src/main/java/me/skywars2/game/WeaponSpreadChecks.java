package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.*;

/** Native telemetry contract: no shots/resources/RNG, private wire geometry and real scope lifecycle. */
public final class WeaponSpreadChecks {
    private WeaponSpreadChecks() { }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Weapon spread: "+why);}
    private static List<Packet<?>> flatten(List<Packet<?>> packets){
        var result=new ArrayList<Packet<?>>();for(var packet:packets)append(packet,result);return result;
    }
    private static void append(Packet<?> packet,List<Packet<?>> result){
        if(packet instanceof ClientboundBundlePacket bundle)bundle.subPackets().forEach(p->append(p,result));else result.add(packet);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ElementalGrimoireChecks.Fixture base;
        final ServerPlayer owner,beta;
        final GameChecks.WandThirdPlayerState betaSaved;
        final net.minecraft.server.network.ServerGamePacketListenerImpl betaPrevious;
        final ElementalGrimoireChecks.Recording betaRecording;
        final Vec3 known;
        Fixture(GameController game){
            this.game=game;base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;known=owner.getKnownMovement();
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(beta!=null,"second recipient");
            betaSaved=new GameChecks.WandThirdPlayerState(beta,base.mana);betaPrevious=beta.connection;
            betaRecording=new ElementalGrimoireChecks.Recording(beta);
            beta.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),betaRecording,beta,
                    new net.minecraft.server.network.CommonListenerCookie(beta.getGameProfile(),0,beta.clientInformation(),false));
            beta.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            base.initialize();owner.removeAllEffects();owner.setOnGround(true);owner.setKnownMovement(Vec3.ZERO);
            game.items().rifleZoom().forget(owner);game.items().laserMinigun().forget(owner);
            WeaponSpreadVisuals.forget(owner);WeaponSpreadVisuals.forget(beta);
        }
        ItemStack loot(String leaf){
            var result=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+leaf);
            require(result.size()==1,"actual singleton loot "+leaf);return result.getFirst();
        }
        void held(ItemStack main,ItemStack off){
            game.items().rifleZoom().forget(owner);owner.setItemInHand(InteractionHand.MAIN_HAND,main);owner.setItemInHand(InteractionHand.OFF_HAND,off);
        }
        WeaponSpreadVisuals.Aim aim(float degrees,int mode,InteractionHand hand){
            var aim=WeaponSpreadVisuals.sample(game,owner);
            require(aim!=null&&Math.abs(aim.degrees()-degrees)<.00001&&aim.mode()==mode&&aim.hand()==hand,
                    "next-shot geometry/hand "+degrees+" mode="+mode+" hand="+hand+" actual="+aim);return aim;
        }
        ItemDisplayElement display(){
            WeaponSpreadVisuals.synchronize(game,owner);var displays=WeaponSpreadVisuals.displays(owner);
            require(displays.size()==1,"one carrier per connection");return displays.getFirst();
        }
        void removed(ItemDisplayElement display,String why){
            var holder=display.getHolder();
            require(WeaponSpreadVisuals.displays(owner).isEmpty()&&(holder==null
                    ||holder.getAttachment()==null&&holder.getWatchingPlayers().isEmpty()),why+" destroys holder attachment and recipients");
            require(flatten(base.recording.packets).stream().anyMatch(p->p instanceof ClientboundRemoveEntitiesPacket remove
                    &&remove.entityIds().contains(display.getEntityId())),why+" sends native entity removal");
        }
        void weapons(){
            for(String leaf:List.of("slr","magnum","ak47","ak47_finite")){
                var stack=loot(leaf);boolean ak=leaf.startsWith("ak47");
                if(ak)require(LbcAk47.isAk47(stack)&&LbcAk47.isFinite(stack)==leaf.endsWith("finite"),"both real AK flag variants");
                for(var hand:InteractionHand.values()){
                    held(hand==InteractionHand.MAIN_HAND?stack:ItemStack.EMPTY,hand==InteractionHand.OFF_HAND?stack:ItemStack.EMPTY);
                    if(!ak&&hand==InteractionHand.OFF_HAND){require(WeaponSpreadVisuals.sample(game,owner)==null,"main-only rifle");continue;}
                    double last=-1;
                    for(double speed:new double[]{0,.075,.15,.3,1.5}){
                        owner.setKnownMovement(new Vec3(speed,0,0));owner.setDeltaMovement(Vec3.ZERO);
                        double expected=ak?LbcGunAccuracy.ak47Degrees(owner):LbcGunAccuracy.rifleDegrees(game,owner);
                        aim((float)expected,WeaponSpreadVisuals.CONE,hand);
                        require(expected>=last&&expected<=20,"monotone movement expansion bounded by20");last=expected;
                        if(speed==0)require(expected==(ak?0:5),"stationary source base spread");
                        if(speed>=.3)require(expected==20,"full movement penalty cap");
                        var old=WeaponSpreadVisuals.sample(game,owner);owner.setYRot(owner.getYRot()+137);owner.setXRot(-89);
                        require(old.equals(WeaponSpreadVisuals.sample(game,owner)),"camera rotation does not change accuracy");
                    }
                }
            }
            owner.setKnownMovement(Vec3.ZERO);owner.setDeltaMovement(Vec3.ZERO);
            var ak=loot("ak47_finite");var mini=loot("laser_minigun");var rifle=loot("slr");
            held(rifle,mini);aim(5,WeaponSpreadVisuals.CONE,InteractionHand.MAIN_HAND);
            held(ak,mini);aim(0,WeaponSpreadVisuals.CONE,InteractionHand.MAIN_HAND);
            held(mini,ak);aim(11900*.001f,WeaponSpreadVisuals.EULER,InteractionHand.MAIN_HAND);
            for(var off:List.of(ak,mini)){
                held(new ItemStack(Items.STICK),off);game.config().offhandWands=true;
                require(WeaponSpreadVisuals.sample(game,owner).hand()==InteractionHand.OFF_HAND,"eligible offhand fallback");
                game.config().offhandWands=false;require(WeaponSpreadVisuals.sample(game,owner)==null,"disabled offhand config");
            }
            game.config().offhandWands=true;
        }
        void readOnly(){
            held(loot("laser_minigun"),ItemStack.EMPTY);var gun=game.items().laserMinigun();gun.forget(owner);
            var random=MatchRandomState.capture(owner.getRandom());
            long expected=owner.getRandom().nextLong();random.restore(owner.getRandom());
            var stateField=field(LbcLaserMinigun.class,"states");
            try{
                var states=(Map<?,?>)stateField.get(gun);require(!states.containsKey(owner.getUUID()),"fresh native spool absent");
                var stack=owner.getMainHandItem().copy();int mana=base.mana.get(owner);
                for(int n=0;n<25;n++){aim(11900*.001f,WeaponSpreadVisuals.EULER,InteractionHand.MAIN_HAND);display();}
                require(!states.containsKey(owner.getUUID())&&gun.shots(owner)==0&&gun.spin(owner)==0&&gun.inputGrace(owner)==0
                        &&gun.spread(owner)==12000&&base.mana.get(owner)==mana&&ItemStack.matches(stack,owner.getMainHandItem()),
                        "preview does not initialize spool, fire, debit mana or animate stack");
                require(owner.getRandom().nextLong()==expected,"preview consumes no owner RNG");
                gun.purify(owner);aim(0,WeaponSpreadVisuals.EULER,InteractionHand.MAIN_HAND);
                require(display().getScale().x()==0&&gun.spread(owner)==0,"purified next-shot spread stays exactly zero");
            }catch(IllegalAccessException error){throw new IllegalStateException(error);}finally{random.restore(owner.getRandom());gun.forget(owner);}
        }
        void wire(){
            held(loot("magnum"),ItemStack.EMPTY);WeaponSpreadVisuals.forget(owner);
            base.recording.packets.clear();betaRecording.packets.clear();var display=display();
            require(display.getGlowColorOverride()==0xD57600&&display.isGlowing()&&display.getScale().equals(new Vector3f(5,1,1))
                    &&display.getItemDisplayContext()==ItemDisplayContext.NONE&&display.getInterpolationDuration()==0&&display.getTeleportDuration()==0,
                    "private marker and immediate cone telemetry");
            var holder=display.getHolder();require(holder.getWatchingPlayers().size()==1&&holder.getWatchingPlayers().contains(owner.connection),"only owner watches carrier");
            require(holder.getAttachment().getWorld()==owner.level()
                    &&holder.getAttachment().getPos().distanceTo(owner.getEyePosition().add(owner.getLookAngle().scale(.5)))<1e-8,"owner camera anchor");
            var packets=flatten(base.recording.packets);
            require(packets.stream().anyMatch(p->p instanceof ClientboundAddEntityPacket add&&add.getId()==display.getEntityId()),"actual native owner spawn");
            require(flatten(betaRecording.packets).stream().noneMatch(p->p instanceof ClientboundAddEntityPacket add&&add.getId()==display.getEntityId()
                    ||p instanceof ClientboundSetEntityDataPacket data&&data.id()==display.getEntityId()),"other participant receives no private spawn/metadata");
            var received=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());boolean metadata=false,marker=false;
            for(var packet:packets)if(packet instanceof ClientboundSetEntityDataPacket data&&data.id()==display.getEntityId()){
                var raw=io.netty.buffer.Unpooled.buffer();try{
                    var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(raw,owner.registryAccess());
                    ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,data);
                    var decoded=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);require(raw.readableBytes()==0,"complete native metadata codec");
                    received.getEntityData().assignValues(decoded.packedItems());metadata=true;
                    marker|=decoded.packedItems().stream().anyMatch(v->Integer.valueOf(0xD57600).equals(v.value()));
                }finally{raw.release();}
            }
            require(metadata&&marker&&Identifier.parse("lbc:black_hole_small").equals(received.getItemStack().get(DataComponents.ITEM_MODEL))
                    &&received.getItemTransform()==ItemDisplayContext.NONE,"actual wire carries private marker and invisible reference model");
            try{
                var getter=Display.class.getDeclaredMethod("createTransformation",net.minecraft.network.syncher.SynchedEntityData.class);getter.setAccessible(true);
                var transform=(com.mojang.math.Transformation)getter.invoke(null,received.getEntityData());
                require(transform.scale().equals(new Vector3f(5,1,1)),"client decoded angle/mode/validity vector");
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Weapon spread native transform",error);}
            owner.setKnownMovement(new Vec3(.15,0,0));require(display()==display&&display.getScale().x()==15,"movement updates same carrier without interpolation lag");
            owner.setKnownMovement(Vec3.ZERO);
        }
        void lifecycle(){
            var zoom=game.items().rifleZoom();held(loot("slr"),ItemStack.EMPTY);var old=display();int tick=game.server().getTickCount();
            base.recording.packets.clear();zoom.punch(owner,tick);require(zoom.scoped(owner),"actual native scope entry");removed(old,"scope entry immediately");
            zoom.punch(owner,tick+2);require(!zoom.scoped(owner)&&WeaponSpreadVisuals.displays(owner).size()==1,"actual scope exit immediately restores hip indicator");
            old=display();held(new ItemStack(Items.STICK),ItemStack.EMPTY);base.recording.packets.clear();WeaponSpreadVisuals.synchronize(game,owner);removed(old,"unsupported held item");
            held(loot("ak47"),ItemStack.EMPTY);old=display();base.recording.packets.clear();owner.setGameMode(GameType.SPECTATOR);WeaponSpreadVisuals.synchronize(game,owner);removed(old,"spectator");owner.setGameMode(GameType.SURVIVAL);
            old=display();base.recording.packets.clear();float health=owner.getHealth();
            try{owner.setHealth(0);WeaponSpreadVisuals.synchronize(game,owner);removed(old,"nonliving owner");}finally{owner.setHealth(health);}
            old=display();base.recording.packets.clear();WeaponSpreadVisuals.forget(owner);removed(old,"owner lifecycle forget");
            old=display();var oldHolder=old.getHolder();var origin=owner.level();var at=owner.position();
            var remote=game.server().getLevel(net.minecraft.world.level.Level.NETHER);require(remote!=null&&remote!=origin,"other native dimension");
            try{
                owner.teleportTo(remote,at.x,at.y,at.z,Set.of(),0,0,true);var replacement=display();
                require(replacement!=old&&oldHolder.getAttachment()==null&&oldHolder.getWatchingPlayers().isEmpty()
                        &&replacement.getHolder().getAttachment().getWorld()==remote,"dimension transfer retires old holder and recreates in current world");
            }finally{WeaponSpreadVisuals.forget(owner);owner.teleportTo(origin,at.x,at.y,at.z,Set.of(),0,0,true);owner.hasChangedDimension();owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());}
            old=display();base.recording.packets.clear();WeaponSpreadVisuals.stop();removed(old,"global stop");
            require(display()!=old,"fresh lifecycle recreates carrier");
        }
        @Override public void close(){
            WeaponSpreadVisuals.forget(owner);WeaponSpreadVisuals.forget(beta);game.items().rifleZoom().forget(owner);game.items().laserMinigun().forget(owner);
            betaSaved.restore(game);beta.connection=betaPrevious;betaPrevious.resetPosition();base.close();owner.setKnownMovement(known);
        }
    }
    private static java.lang.reflect.Field field(Class<?> type,String name){
        try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    public static void run(GameController game){
        try(var f=new Fixture(game)){f.weapons();f.readOnly();f.wire();f.lifecycle();}
        SkyWars2.LOGGER.info("[check] PASS weapon-spread (actual loot/AK flags, shared next-shot geometry and camera independence, minigun no-init/no-RNG, private native wire, scope and dimension/lifecycle cleanup)");
    }
}
