package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Bounded real wall-time stalls on the disposable native fixture, never production players. */
public final class TpsCompensationChecks {
    private TpsCompensationChecks() { }
    private static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException("TPS compensation: " + why);
    }
    private static Field field(Object object, String name) throws ReflectiveOperationException {
        for (Class<?> type=object.getClass();type!=null;type=type.getSuperclass()) {
            try { var f=type.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static void waitMillis(long millis) throws InterruptedException { Thread.sleep(millis); }
    private static void near(double actual, double expected, String why) {
        require(Math.abs(actual-expected)<1e-5,why+": "+actual+" != "+expected);
    }
    public static void run(GameController game) {
        require(game.server().getPort()==25576
                && java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
                    .endsWith(java.nio.file.Path.of("build/verification/key-reuse-server")),
                "bounded stalls are restricted to the isolated native fixture");
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing() && player!=null && game.allowDamage(player),"native participant");
        require(game.server().getTickCount()>30,"nonzero absolute native mining clock");
        var saved=new RegenerationBeltChecks.Snapshot(game,player);
        var mode=player.gameMode;
        var nativeFields=new LinkedHashMap<Field,Object>();
        var level=player.level();
        var pos=new BlockPos((int)Math.floor(player.getX()/16)*16+8,
                Math.min(265,level.getMaxY()-10),(int)Math.floor(player.getZ()/16)*16+8);
        var old=game.maps().snapshot(level,pos);
        var initial=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        level.getAllEntities().forEach(initial::add);
        var manager=game.server().tickRateManager();
        float tickrate=manager.tickrate(); boolean frozen=manager.isFrozen();
        int sequence=9400;
        try {
            for (Class<?> type=mode.getClass();type!=Object.class;type=type.getSuperclass())
                for(var f:type.getDeclaredFields()) if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        && !java.lang.reflect.Modifier.isFinal(f.getModifiers())) { f.setAccessible(true);nativeFields.put(f,f.get(mode)); }
            require(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"resident block");
            player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);player.teleportTo(level,pos.getX()+.5,pos.getY()-1,pos.getZ()-2,Set.of(),0,0,true);
            player.setDeltaMovement(Vec3.ZERO);player.setOnGround(true);
            ((TpsCompensation.Tracked)player).skywars2$compensationTick();
            require(TpsCompensation.enabled(player),"normal20TPS compensation enabled");

            // Synthetic intervals test fractional18/19TPS accounting without changing global tick rate.
            near(TpsCompensation.interval(1_000_000_000L/18)*18,20,"18TPS one second");
            near(TpsCompensation.interval(1_000_000_000L/19)*19,20,"19TPS one second");
            near(TpsCompensation.interval(50_000_000),1,"normal20TPS interval");
            near(TpsCompensation.interval(5_000_000_000L),5,"long pause credits at most250ms");
            near(TpsCompensation.interval(-1),0,"nonpositive clock interval");
            var clock=new TpsCompensation.Clock();clock.reset(0);clock.elapsed(player,0);
            waitMillis(110);double before=clock.elapsed(player,0);double caught=clock.elapsed(player,3);
            require(before>=2 && before<3 && caught==3,"real stalled time and catch-up ticks use max, never sum");

            var attack=player.getAttribute(Attributes.ATTACK_SPEED);attack.removeModifiers();attack.setBaseValue(4);
            player.resetAttackStrengthTicker();
            require(player.getAttackStrengthScale(0)<.05,"fresh attack has no idle credit");
            waitMillis(200);
            float strength=player.getAttackStrengthScale(0);
            require(strength>=.75 && strength<1,"native cooldown advances through real200ms stall: "+strength);
            require(field(player,"attackStrengthTicker").getInt(player)==0,"native ticker and synchronized attribute untouched");
            near(attack.getValue(),4,"attack attribute unchanged");
            var charged=new ItemStack(Items.IRON_SWORD);charged.set(DataComponents.MINIMUM_ATTACK_CHARGE,.7f);
            require(!player.cannotAttackWithItem(charged,0),"minimum charge uses same compensated clock");
            waitMillis(70);require(player.getItemSwapScale(0)==1,"swap cooldown also catches up");
            player.onAttack();
            require(player.getAttackStrengthScale(0)<.05 && player.getItemSwapScale(0)==1,"attack resets only its own clock");
            player.resetAttackStrengthTicker();require(player.getItemSwapScale(0)<.05,"item reset clears both clocks");

            manager.setTickRate(10);TpsCompensation.beginTick(game.server());player.resetAttackStrengthTicker();
            waitMillis(110);require(player.getAttackStrengthScale(0)==0,"intentional tick-rate change retains native simulation");
            manager.setTickRate(20);manager.setFrozen(true);TpsCompensation.beginTick(game.server());
            waitMillis(110);manager.setFrozen(false);TpsCompensation.beginTick(game.server());
            require(player.getAttackStrengthScale(0)<.05,"unfreeze never grants pause credit");

            for(var attribute:List.of(Attributes.BLOCK_BREAK_SPEED,Attributes.MINING_EFFICIENCY,Attributes.SUBMERGED_MINING_SPEED))
                player.getAttribute(attribute).removeModifiers();
            player.getAttribute(Attributes.BLOCK_BREAK_SPEED).setBaseValue(3);
            player.getAttribute(Attributes.MINING_EFFICIENCY).setBaseValue(0);
            player.getAttribute(Attributes.SUBMERGED_MINING_SPEED).setBaseValue(1);
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)container.clearContent();
            level.setBlock(pos,Blocks.DIRT.defaultBlockState(),818);
            mode.handleBlockBreakAction(pos,Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(field(mode,"isDestroyingBlock").getBoolean(mode),"native accepted START");
            waitMillis(200);
            mode.handleBlockBreakAction(pos,Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(level.getBlockState(pos).isAir(),"client's normally timed STOP completes despite zero intervening server ticks");
            near(player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED),3,"mining attribute unchanged");

            level.setBlock(pos,Blocks.DIRT.defaultBlockState(),818);
            mode.handleBlockBreakAction(pos,Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            mode.handleBlockBreakAction(pos,Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(!level.getBlockState(pos).isAir() && field(mode,"hasDelayedDestroy").getBoolean(mode),"early STOP retains native delayed validation");
            var increment=mode.getClass().getDeclaredMethod("incrementDestroyProgress",BlockState.class,BlockPos.class,int.class);
            increment.setAccessible(true);
            int absolute=field(mode,"delayedTickStart").getInt(mode);
            float early=(float)increment.invoke(mode,level.getBlockState(pos),pos,absolute);
            require(early<.7,"absolute delayed start is not mistaken for elapsed ticks: "+early);
            waitMillis(200);
            float delayed=(float)increment.invoke(mode,level.getBlockState(pos),pos,absolute);
            require(delayed>=.9,"native delayed progress accrues real elapsed time: "+delayed);
            // A new START must not reset the older delayed block's independent clock.
            field(mode,"hasDelayedDestroy").setBoolean(mode,false);
            mode.handleBlockBreakAction(pos,Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            field(mode,"hasDelayedDestroy").setBoolean(mode,true);
            require((float)increment.invoke(mode,level.getBlockState(pos),pos,absolute)>=delayed,"active START preserves delayed clock");
            field(mode,"hasDelayedDestroy").setBoolean(mode,false);
            mode.handleBlockBreakAction(pos,Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            waitMillis(200);
            mode.handleBlockBreakAction(pos,Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(!level.getBlockState(pos).isAir(),"stale STOP after ABORT receives no compensation credit");
            require((float)increment.invoke(mode,level.getBlockState(pos),pos,
                    field(mode,"delayedTickStart").getInt(mode))<.7,
                    "stale STOP cannot move abandoned compensation into delayed progress");

            player.getAttribute(Attributes.BLOCK_BREAK_SPEED).setBaseValue(1000);
            field(mode,"hasDelayedDestroy").setBoolean(mode,false);
            mode.handleBlockBreakAction(pos,Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxY(),sequence++);
            require(level.getBlockState(pos).isAir(),"native instant-break path retained");
        } catch (ReflectiveOperationException|InterruptedException error) {
            if(error instanceof InterruptedException)Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        } finally {
            manager.setTickRate(tickrate);manager.setFrozen(frozen);TpsCompensation.beginTick(game.server());
            try { for(var entry:nativeFields.entrySet())entry.getKey().set(mode,entry.getValue()); }
            catch(IllegalAccessException error){throw new IllegalStateException(error);}
            var added=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(!initial.contains(e))added.add(e);});added.forEach(Entity::discard);
            level.destroyBlockProgress(player.getId(),pos,-1);game.maps().restoreSnapshot(level,pos,old);
            player.resetAttackStrengthTicker();saved.restore(game);
        }
        SkyWars2.LOGGER.info("[check] PASS tps-compensation (real bounded stalls, native attack/reset/minimum-charge and mining START/STOP/delayed/ABORT/instant;18/19TPS math, freeze/rate opt-out, no attribute mutation)");
    }
}
