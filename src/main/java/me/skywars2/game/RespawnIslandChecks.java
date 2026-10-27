package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.util.*;

/** Explicit native match fixture. Preparation precedes the genuine match-start capture. */
public final class RespawnIslandChecks {
    private RespawnIslandChecks() { }
    private static Fixture fixture;
    private static final List<Fixture> preparedFixtures=new ArrayList<>();
    private static void require(boolean ok,String why) {
        if(!ok)throw new IllegalStateException("Respawn island: "+why);
    }
    private static Field field(Class<?> type,String name) {
        try {var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static Object read(Object owner,String name) {
        try{return field(owner.getClass(),name).get(owner);}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static void write(Object owner,String name,Object value) {
        try{field(owner.getClass(),name).set(owner,value);}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    public static void prepare(GameController game) {
        require(fixture==null&&preparedFixtures.isEmpty()&&!game.playing()&&!game.maps().busy()&&!game.maps().spawns().isEmpty(),"prepared map before playing, no earlier fixture");
        try {
            for(var spawn:game.maps().spawns()) {
                var prepared=new Fixture(game,spawn);preparedFixtures.add(prepared);prepared.prepare();
            }
        } catch(RuntimeException|Error failure){close(game);throw failure;}
        SkyWars2.LOGGER.info("[check] PASS respawn-island-prepare");
    }
    @SuppressWarnings("unchecked") public static void run(GameController game) {
        require(fixture==null&&!preparedFixtures.isEmpty()&&game.playing(),"prepare first, then start a real match");
        try {
            var participants=(Map<UUID,Object>)read(game,"participants");
            for(var entry:participants.entrySet()) {
                var player=game.server().getPlayerList().getPlayer(entry.getKey());
                if(player==null||!game.allowDamage(player))continue;
                var spawn=(MapManager.Spawn)read(entry.getValue(),"spawn");
                for(var prepared:preparedFixtures)if(prepared.spawn.equals(spawn)){fixture=prepared;break;}
                if(fixture!=null)break;
            }
            require(fixture!=null,"an actually assigned participant spawn was prepared");
            fixture.begin();
        } catch(RuntimeException|Error failure){close(game);throw failure;}
    }
    public static void tick(GameController game) {
        if(fixture==null||!fixture.running)return;
        try {
            if(fixture.step()) {
                close(game);
                SkyWars2.LOGGER.info("[check] PASS respawn-island (native deaths and PERFORM_RESPAWN; cached terrain/NBT; masked air; three chest categories; surviving landing chest recovery and zero-life exclusion)");
            }
        } catch(Throwable failure) {
            try{close(game);}catch(Throwable cleanup){failure.addSuppressed(cleanup);}
            SkyWars2.LOGGER.error("[check] FAIL respawn-island",failure);
        }
    }
    public static void cleanup(GameController game) {
        close(game);
        SkyWars2.LOGGER.info("[check] PASS respawn-island-cleanup");
    }
    private static void close(GameController game) {
        if(fixture==null&&preparedFixtures.isEmpty()) {
            if(game.inLobby())require(((Map<?,?>)read(game.maps(),"respawnIslands")).isEmpty(),"normal match stop cleared cached islands");
            return;
        }
        var old=fixture;fixture=null;
        var all=new ArrayList<>(preparedFixtures);preparedFixtures.clear();
        try{if(old!=null)old.close();}
        finally {
            for(int i=all.size()-1;i>=0;i--) {
                var prepared=all.get(i);
                for(var marker:prepared.markers)marker.discard();
                prepared.prepared.forEach(prepared.maps::restoreSnapshot);
            }
        }
    }
    private static final class Fixture {
        final GameController game;
        final MapManager maps;
        final MapManager.Spawn spawn;
        final BlockPos center,barrel,far;
        final List<BlockPos> near;
        final Map<BlockPos,MapManager.BlockSnapshot> prepared=new LinkedHashMap<>(),before=new LinkedHashMap<>();
        final List<Marker> markers=new ArrayList<>();
        Map<BlockPos,MapManager.BlockSnapshot> original;
        MapManager.BlockSnapshot originalBarrel;
        GameChecks.WandThirdPlayerState saved;
        Object participant;
        UUID victim;
        int lives,deaths,round,phase,ticks,wait;
        boolean awaiting,disconnected,running;
        BlockPos air;
        Fixture(GameController game,MapManager.Spawn spawn) {
            this.game=game;maps=game.maps();this.spawn=spawn;
            center=BlockPos.containing(spawn.position());barrel=center.offset(7,-4,7);
            near=List.of(center.offset(7,-3,0),center.offset(-7,-3,0),center.offset(0,-3,7));
            far=center.offset(25,-3,0);
        }
        void remember(BlockPos pos,Map<BlockPos,MapManager.BlockSnapshot> into) {
            into.computeIfAbsent(pos.immutable(),maps::snapshot);
        }
        RandomizableContainerBlockEntity container(BlockPos pos) {
            require(maps.level().getBlockEntity(pos) instanceof RandomizableContainerBlockEntity,"native container at "+pos);
            return (RandomizableContainerBlockEntity)maps.level().getBlockEntity(pos);
        }
        void set(BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
            maps.restoreSnapshot(pos,new MapManager.BlockSnapshot(state,null));
        }
        void prepare() {
            for(var pos:near)remember(pos,prepared);remember(far,prepared);remember(barrel,prepared);
            set(barrel,Blocks.BARREL.defaultBlockState());
            var gift=new ItemStack(Items.DIAMOND,13);
            gift.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Respawn cached NBT"));
            container(barrel).setItem(4,gift);container(barrel).setChanged();
            originalBarrel=maps.snapshot(barrel);
            String[] categories={"swrg.default","swrg.midle","swrg.center","swrg.default"};
            var positions=new ArrayList<>(near);positions.add(far);
            for(int i=0;i<positions.size();i++) {
                var pos=positions.get(i);set(pos,Blocks.CHEST.defaultBlockState());
                var marker=new Marker(EntityTypes.MARKER,maps.level());
                marker.setPos(Vec3.atCenterOf(pos));marker.entityTags().add(categories[i]);
                require(maps.level().addFreshEntity(marker),"native chest marker created");markers.add(marker);
            }
        }
        @SuppressWarnings("unchecked") void begin() {
            require(maps.spawns().contains(spawn),"prepared map remains selected");
            var participants=(Map<UUID,Object>)read(game,"participants");
            for(var entry:participants.entrySet())if(spawn.equals(read(entry.getValue(),"spawn"))) {
                var p=game.server().getPlayerList().getPlayer(entry.getKey());
                if(p!=null&&game.allowDamage(p)){participant=entry.getValue();victim=entry.getKey();break;}
            }
            require(participant!=null,"prepared original spawn assigned to a live participant");
            var cache=(Map<BlockPos,Map<BlockPos,MapManager.BlockSnapshot>>)read(maps,"respawnIslands");
            original=cache.get(center);
            require(original!=null&&originalBarrel.equals(original.get(barrel)),"genuine start capture retains deliberate unmarked barrel NBT");
            require(maps.hasRespawnLanding(spawn),"original map provides a real landing");
            for(var p:BlockPos.betweenClosed(center.offset(-10,-20,-10),center.offset(10,10,10)))remember(p,before);
            remember(far,before);
            for(var entity:maps.level().getAllEntities())if(entity instanceof Marker&&entity.position().distanceToSqr(spawn.position())<=18*18)
                remember(BlockPos.containing(entity.position()),before);
            for(var p:before.keySet())if(Math.abs(p.getX()-center.getX())<=10&&Math.abs(p.getZ()-center.getZ())<=10
                    &&!original.containsKey(p)&&maps.level().getBlockState(p).isAir()&&p.getY()>center.getY()+3){air=p;break;}
            require(air!=null,"source-air cell available inside captured volume");
            saved=new GameChecks.WandThirdPlayerState(player(),game.items().mana());
            lives=(Integer)read(participant,"lives");awaiting=(Boolean)read(participant,"awaitingRespawn");
            disconnected=(Boolean)read(participant,"disconnected");deaths=game.players().get(victim).deaths;
            write(participant,"lives",3);running=true;phase=-1;
        }
        ServerPlayer player(){return game.server().getPlayerList().getPlayer(victim);}
        void sentinel(BlockPos pos){var c=container(pos);c.clearContent();c.setItem(0,new ItemStack(Items.BARRIER,19));c.setChanged();}
        void damage() {
            var p=player();require(p!=null&&p.isAlive()&&game.allowDamage(p),"active player before native death");
            p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setAbsorptionAmount(0);
            p.getAbilities().invulnerable=false;p.setInvulnerableTime(0);p.damageCooldownTime=0;
            require(game.command("damage "+p.getScoreboardName()+" 100000 minecraft:generic_kill"),"native damage command accepted");
            phase=0;wait=0;
        }
        void destroy() {
            for(var pos:original.keySet())set(pos,Blocks.AIR.defaultBlockState());
            require(!maps.hasRespawnLanding(spawn),"actual original landing removed");
            set(air,Blocks.GOLD_BLOCK.defaultBlockState());
        }
        boolean step() {
            require(game.playing()&&maps.level()==player().level(),"fixture remains in actual playing map");
            require(++ticks<160,"bounded native respawn callback timeout");
            if(phase==-1){damage();return false;}
            if(phase==0) {
                if(!player().isDeadOrDying()||!game.awaitingRespawn(victim))return false;
                require(game.lives(victim)==2-round,"real death decrements exactly one life");
                if(round!=1)destroy();
                for(var pos:near) {
                    if(round!=1)set(pos,Blocks.CHEST.defaultBlockState());
                    sentinel(pos);
                }
                if(round==1)set(near.getFirst(),Blocks.AIR.defaultBlockState());
                sentinel(far);
                var old=player();
                old.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                old.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                require(player()!=old&&player()!=null&&player().isAlive(),"native PlayerList replacement created by packet");
                phase=1;return false;
            }
            if(++wait<2||game.awaitingRespawn(victim))return false;
            var p=player();
            require(spawn.equals(read(participant,"spawn")),"participant assigned spawn remains unchanged");
            require(container(far).getItem(0).is(Items.BARRIER)&&container(far).getItem(0).getCount()==19,"marked chest outside18 remains untouched");
            if(round==0) {
                double dx=p.getX()-spawn.position().x,dz=p.getZ()-spawn.position().z;
                require(p.gameMode()==GameType.SURVIVAL&&dx*dx+dz*dz<.01
                        &&p.getY()<=spawn.position().y+.01&&p.getY()>=spawn.position().y-2,"remaining life teleports to assigned native spawn with ordinary gravity");
                require(maps.hasRespawnLanding(spawn),"landing restored before continuation");
                for(var entry:original.entrySet())if(!near.contains(entry.getKey()))
                    require(maps.level().getBlockState(entry.getKey()).equals(entry.getValue().state()),"cached nonair state restored at "+entry.getKey());
                require(originalBarrel.equals(maps.snapshot(barrel)),"original non-chest block entity NBT restored exactly");
                require(maps.level().getBlockState(air).is(Blocks.GOLD_BLOCK),"masked clone preserves new block in original air");
                requireRefilled();
                SkyWars2.LOGGER.info("[check] PASS respawn-island-restored");
            } else if(round==1) {
                require(maps.hasRespawnLanding(spawn)&&p.gameMode()==GameType.SURVIVAL,"surviving landing supports remaining life");
                require(maps.level().getBlockState(near.getFirst()).is(Blocks.CHEST),"surviving floor still restores missing marked chest block");
                requireRefilled();
                require(container(barrel).getItem(4).is(Items.EMERALD),"surviving floor skips terrain/NBT rollback");
                SkyWars2.LOGGER.info("[check] PASS respawn-island-surviving-floor");
            } else {
                require(game.lives(victim)==0&&game.isObserver(p)&&p.gameMode()==GameType.ADVENTURE,"zero-life native respawn becomes observer");
                require(!maps.hasRespawnLanding(spawn)&&maps.level().getBlockState(barrel).isAir(),"zero-life branch leaves destroyed island absent");
                for(var pos:near)require(container(pos).getItem(0).is(Items.BARRIER),"zero-life branch skips local chest refill");
                SkyWars2.LOGGER.info("[check] PASS respawn-island-zero-life");return true;
            }
            round++;
            if(round==1){container(barrel).setItem(4,new ItemStack(Items.EMERALD,7));container(barrel).setChanged();}
            damage();return false;
        }
        void requireRefilled() {
            for(var pos:near) {
                var c=container(pos);boolean nonempty=false;
                for(int i=0;i<c.getContainerSize();i++) {
                    var stack=c.getItem(i);if(stack.isEmpty())continue;nonempty=true;
                    require(!stack.is(Items.BARRIER),"local marked chest old contents cleared");
                    require(stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().stream()
                            .noneMatch(e->e.modifier().id().toString().equals("minecraft:count")),"ordinary loot synthetic count normalized");
                }
                require(nonempty,"local category chest generated ordinary loot");
            }
        }
        void close() {
            before.forEach(maps::restoreSnapshot);
            if(saved!=null) {
                var current=player();
                if(current!=null&&current.isDeadOrDying()) {
                    current.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                    current.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());current=player();
                }
                write(participant,"lives",lives);write(participant,"awaitingRespawn",awaiting);write(participant,"disconnected",disconnected);
                game.players().get(victim).deaths=deaths;game.players().save();
                if(current!=null)restorePlayer(saved,current,game);
            }
        }
    }
    /** The native respawn replaces the instance; apply the saved values to the replacement. */
    private static void restorePlayer(GameChecks.WandThirdPlayerState s,ServerPlayer p,GameController game) {
        if(game.isObserver(p))game.spectators().remove(p);
        p.closeContainer();var max=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if(max!=null)max.setBaseValue(s.maxHealth);p.setGameMode(s.mode);
        p.getInventory().clearContent();for(int i=0;i<s.inventory.length;i++)p.getInventory().setItem(i,s.inventory[i].copy());
        p.getInventory().setSelectedSlot(s.selectedSlot);
        var slots=p.inventoryMenu.getCraftSlots();for(int i=0;i<s.craft.length;i++)slots.setItem(i,s.craft[i].copy());
        p.containerMenu.setCarried(s.cursor.copy());p.entityTags().clear();p.entityTags().addAll(s.tags);
        p.removeAllEffects();for(var effect:s.effects)p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
        p.teleportTo(p.level(),s.position.x,s.position.y,s.position.z,Set.of(),s.yaw,s.pitch,true);
        p.setDeltaMovement(s.velocity);p.fallDistance=s.fallDistance;p.setNoGravity(s.noGravity);p.setInvisible(s.invisible);p.noPhysics=s.noPhysics;
        p.setHealth(Math.min(p.getMaxHealth(),s.health));p.setAbsorptionAmount(s.absorption);p.setRemainingFireTicks(s.fire);
        p.setInvulnerableTime(s.invulnerable);p.damageCooldownTime=s.damageCooldown;p.setTicksFrozen(s.frozen);p.setAirSupply(s.air);
        p.getFoodData().setFoodLevel(s.food);p.getFoodData().setSaturation(s.saturation);
        p.setExperienceLevels(s.experienceLevel);p.setExperiencePoints(s.experiencePoints);p.totalExperience=s.experiencePoints;p.experienceProgress=s.experienceProgress;
        var a=p.getAbilities();a.mayfly=s.mayFly;a.flying=s.flying;a.invulnerable=s.abilitiesInvulnerable;a.instabuild=s.instabuild;a.mayBuild=s.mayBuild;p.onUpdateAbilities();
        var mana=game.items().mana();mana.inventoryChanged(p);mana.maximum(p,s.manaMaximum);mana.baseRegen(p,s.baseRegen);mana.set(p,s.manaValue);mana.noMagic(p,s.noMagic);
        s.scoreValues.forEach((name,value)->mana.score(p,name).set(value));p.inventoryMenu.sendAllDataToRemote();
    }
}
