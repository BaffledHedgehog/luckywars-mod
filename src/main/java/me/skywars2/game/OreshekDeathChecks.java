package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Two genuine player deaths; only a synchronous consumed nut explosion announces its symbol. */
public final class OreshekDeathChecks {
    private OreshekDeathChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Oreshek death: "+why);}
    private static Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object read(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static void flatten(Packet<?> packet,List<Packet<?>> result){if(packet instanceof ClientboundBundlePacket bundle)bundle.subPackets().forEach(child->flatten(child,result));else result.add(packet);}
    @SuppressWarnings("unchecked") private static long notices(ShishEncounterChecks.Client client){var packets=new ArrayList<Packet<?>>();for(var packet:(List<Packet<?>>)read(read(client,"wire"),"packets"))flatten(packet,packets);return packets.stream().filter(ClientboundSystemChatPacket.class::isInstance).map(ClientboundSystemChatPacket.class::cast).filter(packet->packet.content().getString().equals("\uE202")).count();}
    public static boolean active(){return running!=null;}
    public static void run(GameController game){require(running==null,"exclusive native death fixture");var f=new Fixture(game);running=f;try{f.begin();}catch(RuntimeException|Error failure){running=null;try{f.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS oreshek-death (actual survivor explosion/later unrelated native death has no symbol; actual consumed nut lethal explosion broadcasts once per audience; two registered replacements and twenty ticks)");}catch(RuntimeException|Error failure){running=null;try{f.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final GameController.Checkpoint core;
        ServerPlayer owner;final ServerPlayer beta;final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final boolean frozen;final Set<Entity> previous=Collections.newSetFromMap(new IdentityHashMap<>());
        final int initialLives,used,killedBy,killed;final long usedCount,explosionCount;final ItemStack nut;
        ShishEncounterChecks.Client client,audience;ServerPlayer retired;Vec3 high;int born,stage,clock;boolean closed;
        Fixture(GameController game){this.game=game;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&!game.developerMode()&&game.canUseGameplayItems(owner)&&beta.level()==level,"ordinary two-player active match");
            initialLives=game.lives(owner.getUUID());require(initialLives>=3,"native fixture starts with three lives for two ordinary deaths");
            require(!game.items().timeStop().stopped(level)&&game.items().oreshek().snapshot().isEmpty(),"exclusive unpaused nut service");
            core=game.checkpointCore();saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,beta);frozen=game.server().tickRateManager().isFrozen();
            used=owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(LbcOreshek.ITEM));killed=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER));killedBy=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER));
            usedCount=game.items().oreshek().consumedCount();explosionCount=game.items().oreshek().explosionCount();
            high=new Vec3(owner.getX(),level.getMaxY()+256,owner.getZ());require(level.hasChunkAt(net.minecraft.core.BlockPos.containing(high)),"resident blast column above world");
            for(var entity:level.getAllEntities()){previous.add(entity);require(entity==owner||entity.position().distanceToSqr(high)>1024,"isolated native15 blast, no other bodies in footprint");}
            nut=game.lbc().rollTable(level,high,owner,"lbc:oreshki_10").stream().filter(LbcOreshek::matches).findFirst().orElseThrow();require(nut.is(LbcOreshek.ITEM),"canonical public nut loot identity");
        }
        void ready(float health){owner.closeContainer();owner.stopUsingItem();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.getAbilities().invulnerable=false;owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);owner.setAbsorptionAmount(0);owner.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);owner.setLastHurtByMob(null);
            owner.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active","skywars2.oreshek.returned"));
            var profile=game.players().get(owner.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);
            var defaults=net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER);for(var holder:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var attribute=owner.getAttribute(holder);if(attribute!=null&&defaults.hasAttribute(holder)){attribute.removeModifiers();attribute.setBaseValue(holder.equals(Attributes.MAX_HEALTH)?health:defaults.getBaseValue(holder));}}
            owner.setHealth(health);owner.setExperienceLevels(0);owner.setExperiencePoints(0);owner.totalExperience=0;owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            owner.teleportTo(level,high.x,high.y,high.z,Set.of(),0,0,true);if(client!=null)client.ack();
            game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();game.items().armorSets().inventoryChanged(owner);
        }
        void eat(){var stack=nut.copyWithCount(1);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);try(var choices=new OreshekChecks.Choices(level,0)){var out=stack.finishUsingItem(level,owner);require(out.isEmpty(),"actual native consumable owns exactly one debit");}}
        void begin(){game.server().tickRateManager().setFrozen(false);ready(1000);client=new ShishEncounterChecks.Client(game,owner);audience=new ShishEncounterChecks.Client(game,beta);client.ack();audience.ack();born=game.server().getTickCount();eat();
            require(owner.isAlive()&&owner.getHealth()<1000&&!game.items().oreshek().pendingDeathMessage(owner),"real highHP nut explosion survives with no persistent death notice");
            owner.entityTags().add("skywars2.oreshek.returned");require(!game.items().oreshek().resetBoundary(owner)&&!owner.entityTags().contains("skywars2.oreshek.returned"),"survivor reset clears obsolete marker silently");require(notices(client)==0&&notices(audience)==0,"survivor and reset emit no symbol");
        }
        void verifyDeath(int deaths,long symbols){require(owner.isDeadOrDying()&&game.awaitingRespawn(owner.getUUID()),"real native player death enters ordinary respawn");require(game.lives(owner.getUUID())==initialLives-deaths,"ordinary life debit is authoritative");require(notices(client)==symbols&&notices(audience)==symbols,"exact symbol count for owner and independent audience");retired=owner;}
        void respawn(){client.close();client=null;owner=game.server().getPlayerList().respawn(retired,false,Entity.RemovalReason.KILLED);require(owner!=retired&&game.server().getPlayerList().getPlayer(owner.getUUID())==owner,"genuine canonical registered ServerPlayer replacement");owner.connection.resetPosition();var acknowledgement=(MatchPlayerCheckpoint.SimulatedAcknowledgement)owner.connection;acknowledgement.skywars2$beginRespawn();acknowledgement.skywars2$acknowledgeRestore();require(owner.isAlive()&&game.allowDamage(owner)&&!game.awaitingRespawn(owner.getUUID()),"native replacement returns to ordinary active participant");client=new ShishEncounterChecks.Client(game,owner);}
        boolean step(){require(game.server().getTickCount()-born<100&&game.playing(),"bounded actual native death fixture");
            // A native-dead player has already left the loaded-client lifecycle. Its
            // captured wire still proves the death notice; respawn owns the next ACK.
            // Never run handleAcceptPlayerLoad against the retired body/listener.
            if(stage==0||stage==3){require(client.listener.player==owner,"active fixture listener belongs to canonical player");client.ack();}
            audience.ack();
            if(stage==0){if(game.server().getTickCount()==born)return false;owner.setInvulnerableTime(0);owner.damageCooldownTime=0;require(owner.hurtServer(level,owner.damageSources().fellOutOfWorld(),10000),"later unrelated native void damage accepted");verifyDeath(1,0);stage=1;return false;}
            if(stage==1){respawn();ready(20);eat();require(game.items().oreshek().lastOutcome()==LbcOreshek.Outcome.EXPLOSION,"actual consumed nut chose synchronous native explosion");verifyDeath(2,1);require(game.items().oreshek().consumedCount()==usedCount+2&&game.items().oreshek().explosionCount()==explosionCount+2,"exactly two real nut consumptions and blasts");stage=2;return false;}
            if(stage==2){require(notices(client)==1&&notices(audience)==1,"death wait cannot duplicate synchronous nut notice");respawn();clock=game.server().getTickCount();stage=3;return false;}
            require(owner.isAlive()&&owner==game.server().getPlayerList().getPlayer(owner.getUUID())&&game.allowDamage(owner),"canonical second replacement remains active");require(notices(client)==0&&notices(audience)==1&&!game.items().oreshek().pendingDeathMessage(owner),"replacement and later ticks inherit no nut notice");require(!game.items().oreshek().resetBoundary(owner)&&notices(audience)==1,"post-respawn boundary never repeats symbol");return game.server().getTickCount()-clock>=20;
        }
        void bind(){try{field(saved,"player").set(saved,owner);field(saved.state,"player").set(saved.state,owner);}catch(IllegalAccessException error){throw new IllegalStateException("Rebind only fixture snapshot body references",error);}}
        public void close(){if(closed)return;closed=true;Throwable failure=null;for(Runnable action:List.<Runnable>of(
            ()->{if(owner.isDeadOrDying()){if(client!=null){client.close();client=null;}retired=owner;owner=game.server().getPlayerList().respawn(retired,false,Entity.RemovalReason.KILLED);owner.connection.resetPosition();var ack=(MatchPlayerCheckpoint.SimulatedAcknowledgement)owner.connection;ack.skywars2$beginRespawn();ack.skywars2$acknowledgeRestore();}},
            ()->{game.server().tickRateManager().setFrozen(true);game.restoreCore(core);bind();},
            ()->{if(client!=null)client.close();if(audience!=null)audience.close();},
            ()->{var added=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!previous.contains(entity)&&!(entity instanceof ServerPlayer))added.add(entity);for(var entity:added){game.items().entityUnloaded(entity);entity.discard();}},
            ()->{game.items().clear(owner);saved.restore(game);otherSaved.restore(game);saved.restoreAttributes();otherSaved.restoreAttributes();saved.restoreUse();otherSaved.restoreUse();owner.getStats().setValue(owner,net.minecraft.stats.Stats.ITEM_USED.get(LbcOreshek.ITEM),used);owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER),killedBy);owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER),killed);},
            ()->{game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(beta);game.items().magic().flushCleansing();game.items().armorSets().inventoryChanged(owner);game.items().armorSets().inventoryChanged(beta);owner.connection.resetPosition();beta.connection.resetPosition();game.server().tickRateManager().setFrozen(frozen);game.players().save();}
        ))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw new IllegalStateException("Oreshek native death cleanup",failure);}
    }
}
