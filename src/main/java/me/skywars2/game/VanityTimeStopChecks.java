package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

/** Actual Time Stop, native contacts, canonical terrain/body/source phases and deferred-hit replay. */
public final class VanityTimeStopChecks {
    private VanityTimeStopChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity Time Stop: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcTimeStop service;final ChefChecks.Fixture base;final boolean frozen;
        final MatchCoreCheckpoint originalCore;final MatchWorldCheckpoint originalWorld;final MatchWorldRandomCheckpoint originalRandom;
        final boolean attribution,spells;final MatchSpellCheckpoint originalSpells;MatchSpellCheckpoint spellImage;VanitySpellScene spellScene;final String suite;final LbcIndirectKills.Checkpoint originalLedger;LbcIndirectKills.Checkpoint ledger;VanityAttributionScene scene;
        final MatchActorCheckpoint originalActors;final List<MatchPlayerCheckpoint> originalPlayers;final LbcTimeStop.Checkpoint originalStop;
        final UUID id=GameChecks.offlineUuid("TestGamma");final ServerGamePacketListenerImpl alphaConnection;
        final List<LevelChunk> chunks=new ArrayList<>();final Vec3 momentum=new Vec3(.23,.17,-.31);
        ElementalGrimoireChecks.Recording alphaWire,gammaWire;ServerPlayer gamma,retired;Cow cow,control;Arrow arrow;PrimedTnt tnt,overlap;FallingBlockEntity falling;
        DamageSource raw,source,historical;ServerPlayer past;MatchActorCheckpoint actors;MatchCoreCheckpoint core;MatchWorldCheckpoint world;List<MatchPlayerCheckpoint> players;
        MatchTerrainCheckpoint terrain;MatchTerrainCheckpoint.Restore terrainWork;LbcTimeStop.Checkpoint image;
        BlockPos contact,fallContact,blockTick,fluidTick;int phase=-1,ticks,start,remaining,cooldown,round;long anchor,blockTrigger,fluidTrigger;
        float cowHealth;boolean owned,closed;
        Fixture(GameController game,boolean attribution){this(game,attribution,false);}
        Fixture(GameController game,boolean attribution,boolean spells){this.game=game;this.attribution=attribution;this.spells=spells;suite=spells?"vanity-spells":attribution?"vanity-attribution":"vanity-time-stop";service=game.items().timeStop();frozen=game.server().tickRateManager().isFrozen();require(!frozen&&game.playing()&&!game.maps().busy()&&game.server().getPlayerList().getPlayer(id)==null,"exclusive stable native match");
            for(var level:game.server().getAllLevels())require(!service.stopped(level),"no unrelated Time Stop");require(service.pendingDamageCount()==0&&service.contactCount()==0,"empty deferred/contact services");
            originalCore=new MatchCoreCheckpoint(game);originalWorld=new MatchWorldCheckpoint(game);originalRandom=new MatchWorldRandomCheckpoint(game);originalActors=MatchActorCheckpoint.captureRegistered(game);originalPlayers=game.server().getPlayerList().getPlayers().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();originalStop=service.checkpoint(originalActors.damageSources());originalLedger=game.items().indirectKills().checkpoint(originalActors.damageSources());originalSpells=new MatchSpellCheckpoint(game,originalActors.damageSources());base=new ChefChecks.Fixture(game);alphaConnection=base.owner.connection;}
        void init(){base.init();alphaWire=wire(base.owner);gamma=GameChecks.createBot(game,"TestGamma");owned=true;game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");gammaWire=wire(gamma);SkyWars2.LOGGER.info("[check] START {}",suite);}
        ElementalGrimoireChecks.Recording wire(ServerPlayer p){var wire=new ElementalGrimoireChecks.Recording(p);p.connection=new ServerGamePacketListenerImpl(game.server(),wire,p,new CommonListenerCookie(p.getGameProfile(),0,p.clientInformation(),false));p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());return wire;}
        void ack(){gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());var sent=gammaWire.packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();if(!sent.isEmpty())gamma.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(sent.getLast().id(),gamma.getX(),gamma.getY(),gamma.getZ(),gamma.getYRot(),gamma.getXRot()));require(!gamma.isChangingDimension()&&gamma.connection.hasClientLoaded(),"current native player completed the actual teleport/load handshake");}
        void block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){base.owner.level().setBlock(pos,state,Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
        Cow cow(Vec3 at,boolean stand){var c=new Cow(EntityTypes.COW,base.owner.level());c.setNoAi(true);c.setNoGravity(stand);if(stand)c.entityTags().add("have_stand");c.setPos(at);require(base.owner.level().addFreshEntity(c),"native Cow registration");return c;}
        PrimedTnt tnt(BlockPos pos,int fuse){var t=new PrimedTnt(base.owner.level(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,null);t.setFuse(fuse);t.setDeltaMovement(momentum);require(base.owner.level().addFreshEntity(t),"native TNT registration");return t;}
        void setup(){
            game.server().tickRateManager().setFrozen(true);require(game.allowDamage(gamma)&&game.lives(id)==1,"actual one-life native participant");
            for(var p:List.of(base.owner,base.other,gamma)){base.move(p,base.at.add(p==base.owner?0:p==base.other?-5:5,1,0));p.getInventory().clearContent();p.removeAllEffects();p.setDeltaMovement(Vec3.ZERO);p.setNoGravity(p!=gamma);p.setShiftKeyDown(false);}
            gamma.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);gamma.setHealth(20);gamma.setAbsorptionAmount(0);gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;gamma.setDeltaMovement(momentum);gamma.setKnownMovement(momentum);for(var p:List.of(base.owner,base.other))p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcStand.CLOCKS));
            contact=base.center.offset(0,2,5);fallContact=contact.east(3);blockTick=base.center.offset(6,2,5);fluidTick=base.center.offset(7,2,5);block(contact.below(),Blocks.STONE.defaultBlockState());block(contact,Blocks.CAVE_AIR.defaultBlockState());block(fallContact,Blocks.AIR.defaultBlockState());block(blockTick,Blocks.STONE.defaultBlockState());block(fluidTick,Blocks.WATER.defaultBlockState());
            tnt=tnt(contact,93);overlap=tnt(contact,47);falling=new FallingBlockEntity(EntityTypes.FALLING_BLOCK,base.owner.level());falling.setPos(Vec3.atBottomCenterOf(fallContact));falling.setDeltaMovement(momentum);require(base.owner.level().addFreshEntity(falling),"native falling payload registration");
            cow=cow(base.at.add(6,2,0),false);cowHealth=cow.getHealth();require(cowHealth>5,"native control has enough HP for two nonfatal hits");cow.setDeltaMovement(momentum);control=cow(base.at.add(-6,2,0),true);arrow=new Arrow(base.owner.level(),base.at.x,base.at.y+3,base.at.z,new ItemStack(Items.ARROW),null);arrow.setOwner(gamma);arrow.setDeltaMovement(momentum);require(base.owner.level().addFreshEntity(arrow),"native author projectile registration");
            raw=new DamageSource(base.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRIC),arrow,gamma);source=game.items().indirectKills().captureDamage(raw,game.items().indirectKills().wildCause(gamma));
            control.setInvulnerableTime(0);control.damageCooldownTime=0;require(control.hurtServer(base.owner.level(),source,2),"native unpaused control accepts first wrapped electric hit");control.setInvulnerableTime(0);control.damageCooldownTime=0;require(control.hurtServer(base.owner.level(),source,3),"native unpaused control accepts second wrapped electric hit");control.setDeltaMovement(Vec3.ZERO);
            long now=base.owner.level().getGameTime();base.owner.level().getBlockTicks().schedule(new ScheduledTick<>(Blocks.STONE,blockTick,now+137,TickPriority.HIGH,-721));base.owner.level().getFluidTicks().schedule(new ScheduledTick<>(Fluids.WATER,fluidTick,now+83,TickPriority.LOW,722));
            if(spells)spellScene=new VanitySpellScene(game,base,gamma);
            require(service.useClocks(base.owner,InteractionHand.MAIN_HAND),"real clocks activate native Time Stop and cooldown");require(service.frozen(gamma)&&service.frozen(cow)&&!service.frozen(base.owner),"native frozen/exempt admissions");
            boolean first=cow.hurtServer(base.owner.level(),source,2);int afterFirst=service.pendingDamageCount();var playerSource=gamma.damageSources().playerAttack(base.owner);boolean player=gamma.hurtServer(gamma.level(),playerSource,4);int afterPlayer=service.pendingDamageCount();boolean second=cow.hurtServer(base.owner.level(),source,3);
            require(first&&player&&second&&service.pendingDamageCount()==3&&cow.getHealth()==cowHealth&&gamma.getHealth()==20,"three raw native accepted hits remain deferred in original order: first="+first+"/"+afterFirst+", player="+player+"/"+afterPlayer+", second="+second+"/"+service.pendingDamageCount()+", HP="+cow.getHealth()+"/"+gamma.getHealth()+", playerAllowed="+game.items().allowDamage(gamma,playerSource)+", playerTags="+gamma.entityTags());
            start=game.matchTicks();game.server().tickRateManager().setFrozen(false);phase=0;
        }
        long blockTrigger(){return ((LevelChunkTicks<Block>)base.owner.level().getChunkAt(blockTick).getBlockTicks()).getAll().filter(t->t.pos().equals(blockTick)).findFirst().orElseThrow().triggerTick();}
        long fluidTrigger(){return ((LevelChunkTicks<net.minecraft.world.level.material.Fluid>)base.owner.level().getChunkAt(fluidTick).getFluidTicks()).getAll().filter(t->t.pos().equals(fluidTick)).findFirst().orElseThrow().triggerTick();}
        long anchor(){return LbcTimeStopSchedules.containerClock((LevelChunkTicks<Block>)base.owner.level().getChunkAt(blockTick).getBlockTicks(),base.owner.level().getGameTime());}
        void capture(){
            require(service.contactCount()==2&&base.owner.level().getBlockState(contact).is(Blocks.BARRIER)&&base.owner.level().getBlockState(fallContact).is(Blocks.BARRIER),"overlapping TNT shares one lease; falling block owns another");
            if(attribution)scene=new VanityAttributionScene(game,base,gamma);
            actors=MatchActorCheckpoint.captureRegistered(game);past=new ServerPlayer(game.server(),base.owner.level(),new com.mojang.authlib.GameProfile(GameChecks.offlineUuid("VanityPast"),"VanityPast"),net.minecraft.server.level.ClientInformation.createDefault());historical=new DamageSource(base.owner.damageSources().generic().typeHolder(),past,past);actors.damageSources().capture(historical);players=List.of(base.owner,base.other,gamma).stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();core=new MatchCoreCheckpoint(game);world=new MatchWorldCheckpoint(game);image=service.checkpoint(actors.damageSources());if(attribution){boolean[] rejected={false};game.items().indirectKills().withCause(gamma,null,()->{try{game.items().indirectKills().checkpoint(actors.damageSources());}catch(IllegalStateException expected){rejected[0]=true;}});require(rejected[0],"reentrant causal checkpoint rejected without mutating a callback");ledger=game.items().indirectKills().checkpoint(actors.damageSources());}if(spells){boolean rejected=false;try(var ignored=game.items().spellSources().scope((LbcSpellSource.Source)null)){try{new MatchSpellCheckpoint(game,actors.damageSources());}catch(IllegalStateException expected){rejected=true;}}require(rejected,"even a null spell scope cannot be captured reentrantly");spellImage=new MatchSpellCheckpoint(game,actors.damageSources());}
            terrain=new MatchTerrainCheckpoint(game.server());for(int x=base.bounds.minX()>>4;x<=base.bounds.maxX()>>4;x++)for(int z=base.bounds.minZ()>>4;z<=base.bounds.maxZ()>>4;z++){var chunk=base.owner.level().getChunkSource().getChunkNow(x,z);require(chunk!=null,"all explicit native terrain chunks resident");chunks.add(chunk);terrain.include(chunk);}
            remaining=service.remaining(base.owner.level());cooldown=service.cooldownTicks(base.owner);anchor=anchor();blockTrigger=blockTrigger();fluidTrigger=fluidTrigger();require(base.owner.level().getGameTime()-anchor>=4&&cooldown==800,"capture a genuinely elapsed stopped scheduler, with cooldown paused");verifyStopped();base.move(base.owner,base.at.add(11,1,0));futureFrame();thaw(4);
        }
        void verifyStopped(){
            if(attribution)scene.verify(gamma);if(spells)spellScene.verify(gamma);
            require(service.remaining(base.owner.level())==remaining&&service.cooldownTicks(base.owner)==cooldown&&service.pendingDamageCount()==3&&service.contactCount()==2,"private duration/cooldown/ordered hits/contact holders restored");
            require(anchor()==anchor&&blockTrigger()==blockTrigger&&fluidTrigger()==fluidTrigger,"original paused clock plus canonical native block/fluid queue timestamps restored without double shifting");
            require(service.frozen(gamma)&&service.frozen(cow)&&!service.frozen(base.owner)&&gamma.getDeltaMovement().equals(Vec3.ZERO)&&cow.getDeltaMovement().equals(Vec3.ZERO),"current player/mob bodies resume frozen, preserving immunity inventory");
            require(historical.getEntity()==past&&historical.getDirectEntity()==past&&game.server().getPlayerList().getPlayer(past.getUUID())==null,"already-detached historical player sources remain historical; they never require a future login or resurrect that player");
            require(source.getEntity()==gamma&&source.getDirectEntity()==arrow&&raw.getEntity()==gamma&&raw.getDirectEntity()==arrow&&LbcIndirectKills.isWildCaptured(source)&&LbcIndirectKills.originalDamage(source)==raw&&arrow.getOwner()==gamma,"one canonical graph restores outer/inner accepted-source classes, WILD policy and current author/projectile bodies");
            require(service.persistedVelocity(tnt,Vec3.ZERO).equals(momentum)&&!service.persistedNoGravity(tnt,true)&&tnt.isNoGravity()&&tnt.getDeltaMovement().equals(Vec3.ZERO)&&tnt.getFuse()==93&&overlap.getFuse()==47,"pre-freeze physics and true TNT fuses remain separate from the live frozen body/presentation");
            require(cow.getHealth()==cowHealth&&gamma.getHealth()==20,"restoration never applies a deferred hit");
            var rider=new Cow(EntityTypes.COW,base.owner.level());rider.setNoAi(true);rider.entityTags().add("have_stand");rider.setPos(contact.getX()+.5,contact.getY()+2,contact.getZ()+.5);rider.move(MoverType.SELF,new Vec3(0,-2,0));require(rider.onGround()&&rider.getY()==contact.getY()+1,"actual native support collision uses the restored contact lease");
        }
        void futureFrame(){game.server().tickRateManager().setFrozen(false);try{service.tick();}finally{game.server().tickRateManager().setFrozen(true);}}
        void thaw(){thaw(3);}
        void thaw(int expectedTrails){
            alphaWire.packets.clear();service.end(base.owner.level());
            long trails=alphaWire.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle() instanceof net.minecraft.core.particles.DustColorTransitionOptions).count();require(trails==expectedTrails,"thaw publishes precisely the expected independent trail image, with no future samples or replay during restoration: "+trails+" expected="+expectedTrails);
            require(service.contactCount()==0&&base.owner.level().getBlockState(contact).is(Blocks.CAVE_AIR)&&base.owner.level().getBlockState(fallContact).is(Blocks.AIR),"shared contact holders release once and restore exact cave/ordinary air");
            require(tnt.getDeltaMovement().equals(momentum)&&!tnt.isNoGravity()&&falling.getDeltaMovement().equals(momentum)&&!falling.isNoGravity()&&gamma.getDeltaMovement().equals(momentum)&&!gamma.isNoGravity(),"actual thaw restores captured launch/player momentum and original gravity");
            require(blockTrigger()-base.owner.level().getGameTime()==137&&fluidTrigger()-base.owner.level().getGameTime()==83,"thaw preserves full remaining delays after the genuinely elapsed stopped clock");
            if(spells)spellScene.afterThaw(gamma);start=game.matchTicks();game.server().tickRateManager().setFrozen(false);phase=2;
        }
        void verifyThawed(){require(service.pendingDamageCount()==0&&Math.abs(cow.getHealth()-control.getHealth())<.001&&gamma.getHealth()==16,"actual native replay matches the unstopped wrapped-electric control and player damage, discarding no accepted hit");require(cow.getLastHurtByPlayer()==gamma&&cow.getLastHurtByMob()==gamma,"replayed hits attribute to the actual CURRENT native author after respawn/login");}
        void mutateAndRestore(){
            retired=gamma;gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying(),"genuine native one-life death after the checkpoint");
            if(round==1)gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity Time Stop offline fixture")));
            service.begin(base.owner.level(),20);require(cow.hurtServer(base.owner.level(),cow.damageSources().generic(),99)&&service.pendingDamageCount()==1,"unwanted future stop/hit before rewind");futureFrame();block(contact,Blocks.CHEST.defaultBlockState());((net.minecraft.world.level.block.entity.ChestBlockEntity)base.owner.level().getBlockEntity(contact)).setItem(0,new ItemStack(Items.DIAMOND,9));
            if(attribution)scene.mutate();if(spells)spellScene.mutate();
            world.restoreFrozen();alphaWire.packets.clear();image.prepareFrozen();require(service.pendingDamageCount()==0&&service.contactCount()==0&&!service.stopped(base.owner.level()),"future stop metadata detached without thawing bodies or replaying future damage");
            require(alphaWire.packets.stream().noneMatch(p->p instanceof ClientboundLevelParticlesPacket),"prepare emits no old/future trail presentation");terrainWork=terrain.restore(game);phase=3;
        }
        void finishRestore(){
            core.restoreFrozen();actors.restoreBodiesFrozen();for(var player:players)if(game.server().getPlayerList().getPlayer(player.id())!=null)player.restoreFrozen();
            if(round==1){if(spells){require(spellImage.restoreFrozen(actors::resolve).equals(Set.of(id)),"shared spell state defers ALL tables while its saved target is offline");spellScene.pending();}require(actors.restoreLinksFrozen().equals(Set.of(id)),"missing author/player UUID remains pending in the canonical source graph");if(attribution)require(ledger.restoreFrozen(actors::resolve).equals(Set.of(id)),"causal ledgers defer the complete restore for an offline saved victim");require(image.finishFrozen(actors::resolve).equals(Set.of(id))&&service.pendingDamageCount()==0&&service.contactCount()==0,"offline native player defers the whole private install without losing/partially replaying accepted hits");gamma=GameChecks.createBot(game,"TestGamma");gammaWire=wire(gamma);core.restoreFrozen();players.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow().restoreFrozen();}
            gamma=game.server().getPlayerList().getPlayer(id);ack();require(gamma!=retired,"actual native replacement player body");require(actors.restoreLinksFrozen().isEmpty(),"canonical restored native relationships and source graph complete");if(spells)require(spellImage.restoreFrozen(actors::resolve).isEmpty(),"shared policies/controllers/summons/commands resolve current native bodies together");if(attribution)require(ledger.restoreFrozen(actors::resolve).isEmpty(),"global causal ledgers rebind after native bodies/effects/terrain");require(image.finishFrozen(actors::resolve).isEmpty(),"private Time Stop links install against current bodies");verifyStopped();round++;thaw();
        }
        boolean step(){require(++ticks<240,"bounded async native fixture phase="+phase);if(phase==-1){setup();return false;}
            if(phase==0){if(game.matchTicks()-start<2)return false;game.server().tickRateManager().setFrozen(true);base.move(base.owner,base.at.add(3,1,0));start=game.matchTicks();game.server().tickRateManager().setFrozen(false);phase=1;return false;}
            if(phase==1){if(game.matchTicks()-start<2)return false;game.server().tickRateManager().setFrozen(true);capture();return false;}
            if(phase==2){if(game.matchTicks()-start<2||service.pendingDamageCount()!=0)return false;game.server().tickRateManager().setFrozen(true);verifyThawed();if(round==2){image.prepareFrozen();service.stop();boolean rejected=false;try{image.finishFrozen(actors::resolve);}catch(IllegalStateException expected){rejected=true;}require(rejected&&!service.stopped(base.owner.level())&&service.pendingDamageCount()==0&&service.contactCount()==0&&!LbcTimeStopSchedules.stopped(base.owner.level().getBlockTicks()),"service cancellation invalidates its pending image and scheduler gate");require(service.useClocks(base.owner,InteractionHand.MAIN_HAND),"new clocks remain usable after cancelling a partial checkpoint");service.end(base.owner.level());return true;}mutateAndRestore();return false;}
            if(phase==3){if(!terrainWork.step(32768))return false;terrainWork.close();terrainWork=null;finishRestore();return false;}return false;
        }
        @Override public void close(){if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);try{if(terrainWork!=null){terrainWork.close();terrainWork=null;}if(image!=null)image.cancelFrozen();var p=game.server().getPlayerList().getPlayer(id);if(owned&&p!=null)p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity Time Stop cleanup")));
                originalWorld.restoreFrozen();originalStop.prepareFrozen();originalActors.restoreBodiesFrozen();base.owner.connection=alphaConnection;base.close();originalCore.restoreFrozen();for(var image:originalPlayers)image.restoreFrozen();require(originalActors.restoreLinksFrozen().isEmpty(),"original native body graph restored during cleanup");require(originalStop.finishFrozen(originalActors::resolve).isEmpty(),"original Time Stop service restored during cleanup");require(originalLedger.restoreFrozen(originalActors::resolve).isEmpty(),"original causal ledgers restored during cleanup");require(originalSpells.restoreFrozen(originalActors::resolve).isEmpty(),"original shared spell/control/summon state restored during cleanup");originalRandom.restoreFrozen();
            }finally{game.server().tickRateManager().setFrozen(frozen);}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game,false);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void runAttribution(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game,true);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void runSpells(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game,true,true);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;var suite=running.suite;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS "+suite+" (actual clocks/cooldown and elapsed native paused scheduler; canonical terrain block/fluid queues and shared TNT/falling contact leases; three saved trail points; pre-freeze versus live physics/fuses; canonical wrapped WILD damage/source graph through real death/respawn and explicit offline/login deferral; accepted ordered hits replay twice through native mitigation with current authors, future queue discarded; full coordinator remains gated)");}catch(Throwable error){var suite=running.suite;try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL "+suite,error);}}
}
