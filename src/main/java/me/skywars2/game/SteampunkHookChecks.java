package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real hook inputs, scheduled shared rays and retained player attachment, never a helper mount. */
public final class SteampunkHookChecks {
    private SteampunkHookChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Steampunk hook: "+why);}
    private static void near(Vec3 a,Vec3 b,String why){require(a!=null&&b!=null&&a.distanceToSqr(b)<1e-8,why+" actual="+a+" expected="+b);}
    private static State running;
    private static final class State implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final ServerPlayer p;
        final LbcSteampunkHook service;final Vec3 origin;final BlockPos wall;
        final RegenerationBeltChecks.ScheduledClient client;final List<net.minecraft.network.protocol.Packet<?>> packets;
        final List<Marker> fields=new ArrayList<>();final Map<net.minecraft.world.item.Item,Integer> stats=new IdentityHashMap<>();
        ItemStack hook;int phase,started;boolean closed;
        State(GameController game){this.game=game;base=new AllGrimoireChecks.Fixture(game);base.reset();p=base.base.owner;service=game.items().steampunkHook();origin=p.position();
            for(var item:List.of(LbcMechanicalItems.HOOK,Items.CARROT_ON_A_STICK))stats.put(item,p.getStats().getValue(Stats.ITEM_USED.get(item)));
            client=new RegenerationBeltChecks.ScheduledClient(game,p);
            try{Object connection=client.connection;var field=connection.getClass().getDeclaredField("packets");field.setAccessible(true);packets=(List<net.minecraft.network.protocol.Packet<?>>)field.get(connection);}catch(ReflectiveOperationException error){client.close();base.close();throw new IllegalStateException(error);}
            wall=BlockPos.containing(p.getEyePosition().add(0,0,8));
            p.level().setBlock(wall,Blocks.STONE.defaultBlockState(),3);hook=loot();p.setItemInHand(InteractionHand.MAIN_HAND,hook);
            p.setYRot(0);p.setXRot(0);acknowledge();service.reset(p);game.items().mana().score(p,"hook_cooldown").set(119);started=game.server().getTickCount();
            require(!input(InteractionHand.MAIN_HAND)&&service.anchor(p)==null&&service.pending()==0&&hook.getDamageValue()==0,"initial119 seconds cannot launch or debit");
        }
        ItemStack loot(){var result=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/grapping_hook");require(result.size()==1,"singleton source leaf");var s=result.getFirst();
            require(s.is(LbcMechanicalItems.HOOK)&&LbcSteampunkHook.matches(s)&&s.getMaxDamage()==32,"native identity/source durability/flag");
            require(s.get(DataComponents.ITEM_MODEL).toString().equals("lbc:deagle"),"existing authored hook model");return s;}
        int charge(){return game.items().mana().hudValue(p,"hook_cooldown");}
        void acknowledge(){client.acknowledge();}
        boolean input(InteractionHand hand){acknowledge();
            if(!p.getItemInHand(hand).is(LbcMechanicalItems.HOOK))return net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,p.level(),hand).consumesAction();
            return p.gameMode.useItem(p,p.level(),p.getItemInHand(hand),hand).consumesAction();}
        Marker field(String tag){var m=new Marker(EntityTypes.MARKER,p.level());m.setPos(p.position());m.entityTags().add(tag);require(p.level().addFreshEntity(m),"native field admitted");fields.add(m);game.items().magic().loaded(m);return m;}
        void remove(Marker m){game.items().magic().unloaded(m);m.discard();fields.remove(m);}
        void launch(InteractionHand hand,boolean raw){
            service.clear(p);p.setYRot(0);p.setXRot(0);p.setShiftKeyDown(false);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var source=loot();hook=raw?source.transmuteCopy(Items.CARROT_ON_A_STICK):source;
            if(raw){
                // A transmute copies patches, not the native item's default components.
                // Recreate the old carrier with the actual source metadata, not an unflagged carrot.
                hook.set(DataComponents.CUSTOM_DATA,source.get(DataComponents.CUSTOM_DATA));
                hook.set(DataComponents.MAX_DAMAGE,32);hook.set(DataComponents.MAX_STACK_SIZE,1);hook.set(DataComponents.DAMAGE,0);
                hook.set(DataComponents.ITEM_MODEL,source.get(DataComponents.ITEM_MODEL));hook.set(DataComponents.CUSTOM_NAME,source.get(DataComponents.CUSTOM_NAME));hook.set(DataComponents.LORE,source.get(DataComponents.LORE));
            }
            p.setItemInHand(hand,hook);require(LbcSteampunkHook.matches(hook),"actual source flag survives raw carrier construction");
            boolean accepted=input(hand);
            require(accepted&&service.pending()==1,"actual hand input submits one shared ray "+hand+" raw="+raw+" accepted="+accepted+" pending="+service.pending()+" charge="+charge()+" offhand="+game.config().offhandWands);
        }
        void noPosePackets(){require(packets.stream().noneMatch(packet->packet instanceof ClientboundPlayerPositionPacket||packet instanceof ClientboundSetCameraPacket),"pull never sends position correction or camera swap");
            require(packets.stream().anyMatch(ClientboundSetEntityMotionPacket.class::isInstance),"native player velocity packet sent");
            require(!p.isPassenger(),"direct player never mounts helper");
            for(var e:p.level().getAllEntities())if(!base.base.before.contains(e))require(e.getType()!=EntityTypes.PIG,"no source pig carrier");}
        void policies(){
            var anchor=service.anchor(p);var motion=p.getDeltaMovement();int clock=charge();
            p.setYRot(63);p.setXRot(-25);
            String diagnostics=" pendingTeleport="+((LbcAntigravity.PlayerTransport)p.connection).skywars2$hasPendingTeleport()+" tech="+game.items().magic().mechanismSuppressed(p)+" techLine="+game.items().magic().mechanismSuppressionFraction(p.level(),p.getEyePosition(),anchor)+" shift="+p.isShiftKeyDown()+" role="+game.items().wands().validRoleTarget(p,false)+" epoch="+game.items().epoch()+" tags="+p.entityTags();
            service.tick();near(service.anchor(p),anchor,"camera does not move fixed contact anchor"+diagnostics);require(p.getYRot()==63&&p.getXRot()==-25,"free camera retained");
            var rotated=anchor.add(3,2,-1);service.transformOrigins(p.level(),v->v.add(3,2,-1));near(service.anchor(p),rotated,"Boba transforms saved hook anchor");service.transformOrigins(p.level(),v->v.add(-3,-2,1));near(service.anchor(p),anchor,"inverse restores native hook anchor");
            var image=new MatchPlayerCheckpoint(game,p);service.clear(p);require(service.anchor(p)==null,"actual clear retires attachment");
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{require(image.restoreFrozen()==p,"native saved current player restored");}finally{game.server().tickRateManager().setFrozen(frozen);}
            acknowledge();near(service.anchor(p),anchor,"native full player NBT restores sustained hook without private queue adapter");require(charge()==clock,"player restore does not invent a charge debit");
            packets.clear();p.setShiftKeyDown(true);motion=new Vec3(.7,.4,-.3);p.setDeltaMovement(motion);service.tick();
            require(service.anchor(p)==null,"SHIFT exits pull");near(p.getDeltaMovement(),motion,"SHIFT keeps momentum");p.setShiftKeyDown(false);
            noCancelPosePackets();
        }
        void noCancelPosePackets(){require(packets.stream().noneMatch(packet->packet instanceof ClientboundPlayerPositionPacket||packet instanceof ClientboundSetCameraPacket),"cancellation keeps camera/position free");}
        boolean tick(){
            require(game.server().getTickCount()-started<100,"finite native hook sequence");acknowledge();
            switch(phase){
                case 0->{if(charge()<120)return false;require(charge()==120,"one existing global second completes initial120");
                    field("stopper_magic");p.entityTags().add("nomagic_active");require(game.items().magic().suppressed(p),"MAG/artifact positive control");
                    packets.clear();launch(InteractionHand.MAIN_HAND,false);phase=1;return false;}
                case 1->{if(service.pending()>0)return false;require(service.anchor(p)!=null,"real shared ray accepts source wall");
                    var expected=new Vec3(p.getEyePosition().x,p.getEyePosition().y,wall.getZ()-1);near(service.anchor(p),expected,"wall anchor backs one block before first surface");
                    require(hook.getDamageValue()==1&&charge()>=120,"one actual launching-stack debit and reusable charge");
                    noPosePackets();policies();for(var m:List.copyOf(fields))remove(m);p.entityTags().remove("nomagic_active");
                    launch(InteractionHand.OFF_HAND,true);phase=2;return false;}
                case 2->{if(service.pending()>0)return false;require(service.anchor(p)!=null&&hook.getDamageValue()==1&&p.getMainHandItem().isEmpty(),"legacy offhand carrier launches/debits only its own stack");
                    // A distinct ordinary effect source survives withdrawal of this hook's landing layer.
                    game.items().ownedEffects().grant(p,"steampunk-check-other",ItemEffectLevels.create(MobEffects.SLOW_FALLING,100,0,false,false),p);
                    var tech=field("stopper");require(game.items().magic().mechanismSuppressed(p),"TECH actual field positive control");
                    var motion=new Vec3(.4,.6,.8);p.setDeltaMovement(motion);packets.clear();service.tick();
                    require(service.anchor(p)==null&&!input(InteractionHand.OFF_HAND)&&hook.getDamageValue()==1,"TECH cancels active and blocks use");near(p.getDeltaMovement(),motion,"TECH does not zero velocity");noCancelPosePackets();
                    require(p.hasEffect(MobEffects.SLOW_FALLING),"unrelated landing effect retained");game.items().ownedEffects().withdraw(p,"steampunk-check-other");remove(tech);
                    launch(InteractionHand.MAIN_HAND,false);tech=field("stopper");phase=3;return false;}
                case 3->{if(service.pending()>0)return false;require(service.anchor(p)==null&&hook.getDamageValue()==0,"TECH cancels pending ray without use debit");for(var m:List.copyOf(fields))remove(m);
                    int clock=charge();game.items().timeStop().begin(p.level(),20);try{require(!input(InteractionHand.MAIN_HAND),"actual Time Stop input blocks");service.tick();require(charge()==clock,"Time Stop pauses charge");}finally{game.items().timeStop().end(p.level());}
                    boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);try{require(!input(InteractionHand.MAIN_HAND),"global freeze input blocks");}finally{game.server().tickRateManager().setFrozen(frozen);}
                    // Fixed point misses never consume durability, and do not start an orphan flight.
                    p.setYRot(180);p.setXRot(0);require(input(InteractionHand.MAIN_HAND),"miss accepted for finite ray search");phase=4;return false;}
                case 4->{if(service.pending()>0)return false;require(service.anchor(p)==null&&hook.getDamageValue()==0,"range miss has no debit/flight");
                    service.reset(p);require(charge()==0,"death/reset restores initial charging clock");return true;}
                default->throw new IllegalStateException("hook fixture phase");
            }
        }
        @Override public void close(){if(closed)return;closed=true;try{service.clear(p);for(var m:List.copyOf(fields))remove(m);game.items().timeStop().end(p.level());game.items().ownedEffects().withdraw(p,"steampunk-check-other");
            for(var entry:stats.entrySet())p.getStats().setValue(p,Stats.ITEM_USED.get(entry.getKey()),entry.getValue());}finally{try{client.close();}finally{base.close();}}}
    }
    public static void run(GameController game){require(running==null,"exclusive sequence");running=new State(game);SkyWars2.LOGGER.info("[check] START steampunk-hook");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS steampunk-hook (actual initial120 clock/reusable charge; native/raw main/offhand; fixed wall contact; velocity without pose packets/pig; MAG/TECH active+pending and pause gates; SHIFT momentum; unrelated effects; native player NBT checkpoint/Boba transform; miss/reset)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL steampunk-hook",error);}}
}
