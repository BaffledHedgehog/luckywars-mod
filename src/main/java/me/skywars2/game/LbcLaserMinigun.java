package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/** Player spool state and short lived Polymer beams; collision remains in LbcWands. */
public final class LbcLaserMinigun {
    public static final Item ITEM=registerItem();
    private static final CompoundTag FLAG=flag();
    private final GameController game;
    private final Map<UUID,Spool> states=new HashMap<>();
    private final HashSet<UUID> active=new HashSet<>();
    private final ArrayList<Beam> beams=new ArrayList<>();
    private static final class Spool {
        final ServerPlayer owner;
        final String epoch;
        int spin,grace,sound,spread=12000,shots;
        int inventoryTick=Integer.MIN_VALUE;
        boolean revved;
        Spool(ServerPlayer owner,String epoch) { this.owner=owner; this.epoch=epoch; }
    }
    private static final class Beam {
        final ServerPlayer owner;
        final String epoch;
        final ElementHolder holder;
        final ItemDisplayElement display;
        final float lengthScale;
        final Vec3 origin;
        final float yaw,pitch;
        int life=6;
        Beam(ServerPlayer owner,String epoch,ElementHolder holder,ItemDisplayElement display,float lengthScale,
                Vec3 origin,float yaw,float pitch) {
            this.owner=owner; this.epoch=epoch; this.holder=holder; this.display=display; this.lengthScale=lengthScale;
            this.origin=origin; this.yaw=yaw; this.pitch=pitch;
        }
    }
    LbcLaserMinigun(GameController game) { this.game=game; }
    public static void register() { }
    private static CompoundTag flag() { var flag=new CompoundTag(); flag.putInt("laser_minigun",1); return flag; }
    private static Item registerItem() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:laser_minigun"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:laser_minigun/1"); }
        }.setId(key).stacksTo(1).attributes(LbcItems.minigunAttributes()).overrideDescription("laser_minigun")
                .component(DataComponents.INTERACT_ANIMATION,new net.minecraft.world.item.component.SwingAnimation(
                        net.minecraft.world.item.SwingAnimationType.NONE,6)),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isMinigun(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(ITEM) || !stack.isEmpty() && data!=null && data.matchedBy(FLAG);
    }
    /** Source shoot/click: input refreshes six working ticks; starting the motor is free. */
    public boolean use(ServerPlayer owner,InteractionHand hand) {
        if(!valid(owner) || !isMinigun(owner.getItemInHand(hand))
                || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")
                || suppressed(owner)) return false;
        var spool=state(owner);
        // Inventory input ends this trigger press; queued use packets from the same tick cannot restart it.
        if(spool.inventoryTick==game.server().getTickCount()) return false;
        if(spool.grace<=0) sound(owner,"rev_up",1,1);
        spool.grace=6;
        if(!spool.revved) spool.sound=1;
        spool.revved=true;
        active.add(owner.getUUID());
        return true;
    }
    /** Stop animation before a menu moves its stack; never restore a saved hand after the transfer. */
    public boolean interruptInventory(ServerPlayer owner) {
        var spool=states.get(owner.getUUID());
        if(spool==null || !active.contains(owner.getUUID()) || spool.spin<=0 && spool.grace<=0) return false;
        spool.inventoryTick=game.server().getTickCount();
        spool.spin=0; spool.grace=0; spool.sound=0; spool.spread=12000;
        active.remove(owner.getUUID());
        if(spool.revved) { sound(owner,"rev_down",1,1); stopRevUp(owner); }
        spool.revved=false;
        return true;
    }
    public void tick() {
        boolean stopped=game.items().wands().timeStopped();
        // Age existing visuals first: a newly emitted beam retains its full six-tick lifetime.
        tickVisuals(stopped);
        for(var id:List.copyOf(active)) {
            var spool=states.get(id);
            if(spool==null) { active.remove(id); continue; }
            var owner=spool.owner;
            if(!valid(owner) || !game.items().currentEpoch(spool.epoch)) { forget(owner); continue; }
            if(stopped || owner.entityTags().contains("time_stopped")) continue;
            boolean main=isMinigun(owner.getItemInHand(InteractionHand.MAIN_HAND));
            boolean off=game.config().offhandWands && isMinigun(owner.getItemInHand(InteractionHand.OFF_HAND));
            // Removing the last held copy cancels input immediately, while the motor coasts down.
            if(!main && !off) spool.grace=0;
            if(spool.grace>0) { spool.spin++; spool.grace--; }
            if(spool.grace==0) spool.spin=Math.max(0,spool.spin-1);
            if(spool.spin>=30) {
                spool.spin=30;
                if(--spool.sound<=0) { sound(owner,"spin",2,1); spool.sound=5; }
                if(!suppressed(owner)) {
                    if(main) shoot(spool,InteractionHand.MAIN_HAND);
                    // Damage can synchronously end the match, forget this owner, or replace its state.
                    if(current(spool) && !suppressed(owner) && off) shoot(spool,InteractionHand.OFF_HAND);
                }
            }
            if(!current(spool)) continue;
            if(spool.grace==0 && spool.revved) {
                spool.spread=12000; spool.revved=false;
                sound(owner,"rev_down",1,1); stopRevUp(owner);
            }
            if(spool.spin>0) {
                if(main) animate(owner,InteractionHand.MAIN_HAND,false);
                if(off) animate(owner,InteractionHand.OFF_HAND,false);
            } else active.remove(id);
        }
    }
    private boolean current(Spool spool) {
        return states.get(spool.owner.getUUID())==spool && active.contains(spool.owner.getUUID())
                && valid(spool.owner) && game.items().currentEpoch(spool.epoch);
    }
    /** Source full_rev/cast: each held copy pays independently, then narrows spread before tracing. */
    private void shoot(Spool spool,InteractionHand hand) {
        var owner=spool.owner;
        if(!current(spool) || !isMinigun(owner.getItemInHand(hand))
                || !game.items().mana().trySpendWand(owner,100)) return;
        if(spool.spread>=1100) spool.spread-=100;
        spool.shots++;
        if(spool.spread<=1000) game.items().mana().award(owner,"lbc:true_advancements/shooter/laser_minigun");
        sound(owner,"shot",2,0);
        nativeRay(spool,hand);
    }
    private Spool state(ServerPlayer owner) {
        return states.computeIfAbsent(owner.getUUID(),id->new Spool(owner,game.items().epoch()));
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && !owner.isRemoved() && owner.isAlive() && !game.isObserver(owner)
                && owner.level()==game.maps().level() && game.canUseGameplayItems(owner)
                && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean suppressed(ServerPlayer owner) {
        return game.items().mana().noMagic(owner)>0 || game.items().actionsBlocked(owner) || game.items().magic().suppressed(owner)
                || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());
    }
    /** Corrected eight-frame cycle; the optional legacy transition remains available for comparison. */
    static int nextFrame(int frame,boolean sourceSequentialSkip) {
        if(frame<1 || frame>8) frame=1;
        return frame==1 || sourceSequentialSkip && frame==2 ? 8 : frame-1;
    }
    private void animate(ServerPlayer owner,InteractionHand hand,boolean sourceSequentialSkip) {
        var stack=owner.getItemInHand(hand);
        if(isMinigun(stack)) stack.set(DataComponents.ITEM_MODEL,
                Identifier.parse("lbc:laser_minigun/"+nextFrame(frame(owner,hand),sourceSequentialSkip)));
    }
    /** Source ray/cast rotations and muzzle, with the author's native first-hit endpoint policy. */
    private void nativeRay(Spool spool,InteractionHand hand) {
        var owner=spool.owner;
        int spread=spool.spread;
        Vec3 origin=muzzle(owner,hand);
        if(!owner.level().hasChunkAt(BlockPos.containing(origin))) return;
        float yaw=owner.getYRot()+(owner.getRandom().nextInt(spread*2+1)-spread)*.001f;
        float pitch=owner.getXRot()+(owner.getRandom().nextInt(spread*2+1)-spread)*.001f;
        Vec3 forward=Vec3.directionFromRotation(pitch,yaw),end=origin.add(forward.scale(100));
        double suppression=game.items().magic().suppressionFraction(owner.level(),origin,end);
        if(suppression<=1) end=origin.lerp(end,suppression);
        var hit=game.items().wands().firstHit(owner,origin,end);
        if(hit!=null) end=hit.getLocation();
        var shield=game.items().wands().shieldHit(owner,origin,end);
        if(shield!=null) end=shield.getLocation();
        else if(hit instanceof EntityHitResult entityHit) {
            var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.GENERIC),owner);
            // Accepted native damage already feeds the common indirect-kill attribution hook.
            entityHit.getEntity().hurtServer(owner.level(),source,2.5f);
            if(current(spool)) sparks(owner,end,10,false);
        } else if(hit instanceof BlockHitResult wall && owner.level().hasChunkAt(wall.getBlockPos()))
            sparks(owner,end,20,true);
        if(current(spool)) display(owner,origin,yaw,pitch,Math.min(100,origin.distanceTo(end)));
    }
    private void sparks(ServerPlayer owner,Vec3 position,int count,boolean force) {
        owner.level().sendParticles(ParticleTypes.ELECTRIC_SPARK,force,false,
                position.x,position.y,position.z,count,0,0,0,.3);
    }
    /** Legacy main-hand ^-.3 ^-.2 ^.5; the already-correct offhand mirrors only lateral offset. */
    static Vec3 muzzle(ServerPlayer owner) { return muzzle(owner,InteractionHand.MAIN_HAND); }
    static Vec3 muzzle(ServerPlayer owner,InteractionHand hand) {
        // Use the actual command basis, including its native Mth angle quantization at steep pitch.
        return new net.minecraft.commands.arguments.coordinates.LocalCoordinates(
                hand==InteractionHand.MAIN_HAND?-.3:.3,-.2,.5)
                .apply(owner.getEyePosition(),new net.minecraft.world.phys.Vec2(owner.getXRot(),owner.getYRot()));
    }
    private void display(ServerPlayer owner,Vec3 origin,float yaw,float pitch,double length) {
        var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:laser_minigun_ray"));
        var display=new ItemDisplayElement(stack);
        display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setYaw(yaw); display.setPitch(pitch);
        display.setShadowRadius(0); display.setShadowStrength(0);
        display.setInterpolationDuration(0);
        float scale=(float)(length*2);
        display.setScale(new Vector3f(0,0,scale));
        var holder=new ElementHolder(); holder.addElement(display);
        // Rotation, zero initial width and final length exist before the first spawn packet.
        game.visualTracking().attach(holder,owner.level(),()->origin);
        refreshViewers(holder,owner,origin);
        holder.tick();
        beams.add(new Beam(owner,game.items().epoch(),holder,display,scale,origin,yaw,pitch));
    }
    private void sound(ServerPlayer owner,String suffix,float volume,float pitch) {
        LbcOverload.sound(owner,owner.position(),"minecraft:lbcsounds.laser_mg_"+suffix,volume,pitch,0);
    }
    private void stopRevUp(ServerPlayer owner) {
        var packet=new ClientboundStopSoundPacket(Identifier.parse("minecraft:lbcsounds.laser_mg_rev_up"),SoundSource.MASTER);
        for(var viewer:game.server().getPlayerList().getPlayers())
            if(viewer.level()==owner.level() && viewer.position().distanceToSqr(owner.position())<=100)
                viewer.connection.send(packet);
    }
    private void tickVisuals(boolean stopped) {
        for(int i=beams.size()-1;i>=0;i--) {
            var beam=beams.get(i);
            if(!valid(beam.owner) || !game.items().currentEpoch(beam.epoch)) {
                beam.holder.destroy(); beams.remove(i); continue;
            }
            refreshViewers(beam.holder,beam.owner,beam.origin);
            // All beams pause with the dimension, including the free time-stop owner's beams.
            if(stopped || beam.owner.entityTags().contains("time_stopped")) continue;
            if(--beam.life<=0) { beam.holder.destroy(); beams.remove(i); continue; }
            float width=beam.life*.04f;
            beam.display.setScale(new Vector3f(width,width,beam.lengthScale));
            beam.holder.tick();
        }
    }
    /** ManualAttachment never enrolls viewers itself. Keep native tracking range and all viewer roles. */
    private void refreshViewers(ElementHolder holder,ServerPlayer owner,Vec3 origin) {
        var connected=game.server().getPlayerList().getPlayers();
        int range=EntityTypes.ITEM_DISPLAY.clientTrackingRange()*16;
        double rangeSquared=(double)range*range;
        for(var connection:List.copyOf(holder.getWatchingPlayers())) {
            var viewer=connection.player;
            if(viewer.connection!=connection || !connected.contains(viewer) || viewer.level()!=owner.level()
                    || viewer.position().distanceToSqr(origin)>=rangeSquared) holder.stopWatching(connection);
        }
        for(var viewer:connected)
            if(viewer.level()==owner.level() && viewer.position().distanceToSqr(origin)<rangeSquared)
                holder.startWatching(viewer);
    }
    public void purify(ServerPlayer owner) { state(owner).spread=0; }
    /** KFC increments source counters even when there is no currently scheduled spool tick. */
    void addPlayerCounters(ServerPlayer owner) {
        var state=state(owner); state.spin++; state.grace++; state.sound++; state.spread++; state.shots++;
    }
    public int spin(ServerPlayer owner) { var state=states.get(owner.getUUID()); return state==null?0:state.spin; }
    public int inputGrace(ServerPlayer owner) { var state=states.get(owner.getUUID()); return state==null?0:state.grace; }
    public int spread(ServerPlayer owner) { var state=states.get(owner.getUUID()); return state==null?12000:state.spread; }
    public int shots(ServerPlayer owner) { var state=states.get(owner.getUUID()); return state==null?0:state.shots; }
    public int frame(ServerPlayer owner,InteractionHand hand) {
        var stack=owner.getItemInHand(hand);
        if(!isMinigun(stack)) return 0;
        var model=stack.get(DataComponents.ITEM_MODEL);
        if(model==null) return 1;
        String path=model.getPath();
        return model.getNamespace().equals("lbc") && path.startsWith("laser_minigun/")
                && path.length()==15 && path.charAt(14)>='1' && path.charAt(14)<='8' ? path.charAt(14)-'0' : 1;
    }
    public int visualCount() { return beams.size(); }
    record BeamView(Vec3 origin,float yaw,float pitch,double length,int life) { }
    List<BeamView> beamSnapshot() {
        return beams.stream().map(beam->new BeamView(beam.origin,beam.yaw,beam.pitch,beam.lengthScale/2,beam.life)).toList();
    }
    public void forget(ServerPlayer owner) {
        active.remove(owner.getUUID());
        states.remove(owner.getUUID());
        beams.removeIf(beam->{ if(beam.owner!=owner) return false; beam.holder.destroy(); return true; });
    }
    public void stop() {
        active.clear();
        states.clear();
        for(var beam:beams) beam.holder.destroy();
        beams.clear();
    }
}
