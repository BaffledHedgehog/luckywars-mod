package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.PlayerViewCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Actual native menus/clicks/terrain, death/respawn and view packets; no private FPV claims. */
public final class VanityViewChecks {
    private VanityViewChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity views: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final MatchCoreCheckpoint originalCore;
        final ChefChecks.Fixture base;
        final UUID id=GameChecks.offlineUuid("TestGamma");
        final boolean frozen;
        final net.minecraft.server.level.TicketType resident=new net.minecraft.server.level.TicketType(0,
                net.minecraft.server.level.TicketType.FLAG_LOADING|net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
        MatchActorCheckpoint originalActors,actors;
        MatchCoreCheckpoint core;
        List<MatchPlayerCheckpoint> players;
        MatchTerrainCheckpoint terrain;
        MatchTerrainCheckpoint.Restore restore;
        ServerPlayer gamma,retired;
        ElementalGrimoireChecks.Recording wire;
        net.minecraft.world.entity.animal.cow.Cow camera;
        List<BlockPos> positions;
        List<Container> oldBacking;
        net.minecraft.network.chat.MutableComponent titleTail;
        Vec3 at;
        final Vec3 motion=new Vec3(12.25,-13.5,14.75),known=new Vec3(.25,.04,-.3);
        int phase=-1,ticks,clock;
        Set<UUID> lastPending=Set.of();
        boolean owned,closed;
        Fixture(GameController game) {
            this.game=game;frozen=game.server().tickRateManager().isFrozen();
            require(!frozen&&game.playing()&&!game.maps().busy()&&game.server().getPlayerList().getPlayer(id)==null,"exclusive running match and owned Gamma");
            originalCore=new MatchCoreCheckpoint(game);base=new ChefChecks.Fixture(game);
        }
        void init() {
            var nether=game.server().getLevel(net.minecraft.world.level.Level.NETHER);
            nether.getChunkSource().addTicketWithRadius(resident,new net.minecraft.world.level.ChunkPos(0,0),0);nether.getChunk(0,0);
            originalActors=MatchActorCheckpoint.captureRegistered(game);base.init();
            gamma=GameChecks.createBot(game,"TestGamma");owned=true;
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
            SkyWars2.LOGGER.info("[check] START vanity-views");
        }
        void wire() {
            wire=new ElementalGrimoireChecks.Recording(gamma);
            gamma.connection=new ServerGamePacketListenerImpl(game.server(),wire,gamma,
                    new CommonListenerCookie(gamma.getGameProfile(),0,gamma.clientInformation(),false));
            gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        void flatten(Packet<?> p,List<Packet<?>> out){if(p instanceof ClientboundBundlePacket b)for(var child:b.subPackets())flatten(child,out);else out.add(p);}
        List<Packet<?>> packets(){var out=new ArrayList<Packet<?>>();for(var p:wire.packets)flatten(p,out);return out;}
        void ackChunks() {
            // These native memory bots have no client to acknowledge batches (including
            // ones emitted before this fixture attached its recorder). Read ONLY the
            // count of genuinely emitted outstanding batches and use the real handler.
            try {
                var field=net.minecraft.server.network.PlayerChunkSender.class.getDeclaredField("unacknowledgedBatches");field.setAccessible(true);
                for(var player:List.of(base.owner,base.other,gamma)) {
                    if(game.server().getPlayerList().getPlayer(player.getUUID())!=player)continue;
                    int count=field.getInt(player.connection.chunkSender);
                    for(int i=0;i<count;i++)player.connection.handleChunkBatchReceived(new ServerboundChunkBatchReceivedPacket(9));
                }
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void ack() {
            ackChunks();
            gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var positions=packets().stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
            if(!positions.isEmpty())gamma.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(positions.getLast().id(),gamma.getX(),gamma.getY(),gamma.getZ(),gamma.getYRot(),gamma.getXRot()));
        }
        Container container(int n){return (Container)base.owner.level().getBlockEntity(positions.get(n));}
        void open(ServerPlayer player,int kind,Component title) {
            var a=container(kind);var backing=kind==0?new CompoundContainer(a,container(1)):a;
            player.openMenu(new SimpleMenuProvider((number,inventory,p)->switch(kind){
                case 0 -> new ChestMenu(MenuType.GENERIC_9x6,number,inventory,backing,6);
                case 2 -> new HopperMenu(number,inventory,backing);
                case 3 -> new DispenserMenu(number,inventory,backing);
                default -> throw new IllegalArgumentException();
            },title));
        }
        void prepare() {
            require(game.allowDamage(gamma)&&game.lives(id)==1,"actual native enrollment");game.server().tickRateManager().setFrozen(true);
            at=base.at.add(0,1,0);base.move(gamma,at);gamma.setNoGravity(true);wire();
            positions=List.of(base.center.offset(1,0,0),base.center.offset(2,0,0),base.center.offset(-2,0,0),base.center.offset(0,0,2));
            base.block(positions.get(0),Blocks.CHEST);base.block(positions.get(1),Blocks.CHEST);
            base.block(positions.get(2),Blocks.HOPPER);base.block(positions.get(3),Blocks.DROPPER);
            oldBacking=List.of(container(0),container(1),container(2),container(3));
            container(1).setItem(7,new ItemStack(Items.EMERALD,19));container(2).setItem(4,new ItemStack(Items.DIAMOND,13));container(3).setItem(8,new ItemStack(Items.IRON_INGOT,11));
            gamma.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,1200,2));FullInvisibility.effectsChanged(gamma);
            camera=base.cow(at.add(3,1,0));gamma.setCamera(camera);
            gamma.teleportTo(gamma.level(),at.x,at.y,at.z,Set.of(),0,0,false);
            base.owner.setCamera(gamma);var ownerAt=at.add(-1,0,0);
            base.owner.teleportTo(base.owner.level(),ownerAt.x,ownerAt.y,ownerAt.z,Set.of(),0,0,false);base.move(base.other,at.add(0,0,1));
            require(!FullInvisibility.proxyFor(gamma.getId(),base.owner)&&FullInvisibility.proxyFor(gamma.getId(),base.other),"native camera exception and ordinary full-invisibility viewer baseline");
            gamma.getInventory().clearContent();gamma.getInventory().setItem(12,new ItemStack(Items.LAPIS_LAZULI,17));
            gamma.inventoryMenu.getCraftSlots().setItem(0,new ItemStack(Items.APPLE,3));gamma.inventoryMenu.setCarried(new ItemStack(Items.COPPER_INGOT,2));
            titleTail=Component.literal("double chest");
            open(gamma,0,Component.literal("Saved native ").append(titleTail));open(base.owner,2,Component.literal("Saved native hopper"));open(base.other,3,Component.literal("Saved native dropper"));
            var cursor=new ItemStack(Items.GOLD_INGOT,8);cursor.set(DataComponents.CUSTOM_NAME,Component.literal("Saved cursor components"));gamma.containerMenu.setCarried(cursor);
            base.owner.containerMenu.setCarried(new ItemStack(Items.AMETHYST_SHARD,5));base.other.containerMenu.setCarried(new ItemStack(Items.REDSTONE,7));
            gamma.containerMenu.clicked(-999,0,ContainerInput.QUICK_CRAFT,gamma);
            gamma.containerMenu.clicked(0,1,ContainerInput.QUICK_CRAFT,gamma);gamma.containerMenu.clicked(1,1,ContainerInput.QUICK_CRAFT,gamma);
            require(((PlayerViewCheckpointAccess.Drag)gamma.containerMenu).skywars2$slots().size()==2,"real native drag selects two storage slots");
            gamma.setYRot(37.5F);gamma.setXRot(-12.25F);gamma.setDeltaMovement(motion);gamma.setKnownMovement(known);
            var before=gamma.containerMenu;actors=MatchActorCheckpoint.captureRegistered(game);
            players=List.of(new MatchPlayerCheckpoint(game,base.owner),new MatchPlayerCheckpoint(game,base.other),new MatchPlayerCheckpoint(game,gamma));
            require(gamma.containerMenu==before&&before.getCarried().getCount()==8,"capture does not close or mutate the live session");
            terrain=new MatchTerrainCheckpoint(game.server());for(var pos:positions)terrain.include(base.owner.level().getChunkAt(pos));
            core=new MatchCoreCheckpoint(game);clock=game.matchTicks();phase=0;
        }
        void mutate() {
            titleTail.append(" future mutation");
            for(var p:List.of(base.owner,base.other,gamma)){p.containerMenu.setCarried(new ItemStack(Items.NETHERITE_INGOT,31));p.getInventory().setItem(12,new ItemStack(Items.DIRT,64));}
            long drops=cursorDrops();var beforeMotion=gamma.getDeltaMovement();var beforePosition=gamma.position();
            actors.prepareViewsFrozen();
            require(List.of(base.owner,base.other,gamma).stream().allMatch(p->p.containerMenu==p.inventoryMenu),"native future storage sessions closed before terrain");
            require(cursorDrops()==drops,"future storage cursor never becomes a native item drop");
            require(gamma.getCamera()==gamma&&base.owner.getCamera()==base.owner&&gamma.position().equals(beforePosition)&&gamma.getDeltaMovement().equals(beforeMotion),"future camera links retire before actor removal without moving bodies");
            for(var p:List.of(base.owner,base.other,gamma))for(int i=0;i<p.getInventory().getContainerSize();i++)
                require(!p.getInventory().getItem(i).is(Items.NETHERITE_INGOT),"native close never returns the future cursor to inventory");
            for(var pos:positions)base.block(pos,Blocks.STONE);
            camera.discard();gamma.setCamera(gamma);
            var nether=game.server().getLevel(net.minecraft.world.level.Level.NETHER);
            require(gamma.teleportTo(nether,.5,200,.5,Set.of(),0,0,false)&&gamma.level()==nether,"genuine native future dimension/client-world replacement");
            ack();require(!gamma.isChangingDimension(),"native client teleport acknowledgement completes dimension immunity");
            gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;
            require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying(),"genuine native death before restored screen/camera links");
            require(base.owner.isAlive()&&base.other.isAlive(),"borrowed players survive fixture death");
            restore=terrain.restore(game);
        }
        long cursorDrops(){long count=0;for(var entity:base.owner.level().getAllEntities())if(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(Items.NETHERITE_INGOT)&&item.getItem().getCount()==31)count++;return count;}
        void bodies() {
            ackChunks();wire.packets.clear();retired=gamma;core.restoreFrozen();actors.restoreBodiesFrozen();
            for(var image:players)image.restoreFrozen();gamma=game.server().getPlayerList().getPlayer(id);
            require(gamma!=retired,"real native player body replacement");gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            require(gamma.position().equals(at)&&gamma.getDeltaMovement().equals(motion)&&gamma.getKnownMovement().equals(known)&&gamma.getYRot()==37.5F&&gamma.getXRot()==-12.25F,"body phase restores exact saved position/rotation/raw and known movement before view publication");
            ack();
            var position=gamma.position();var velocity=gamma.getDeltaMovement();var input=gamma.getKnownMovement();
            var pending=actors.restoreLinksFrozen();require(Set.of(id,camera.getUUID()).containsAll(pending),"only post-respawn native camera pairing may remain pending");
            require(gamma.position().equals(position)&&gamma.getDeltaMovement().equals(velocity)&&gamma.getKnownMovement().equals(input),"initial view publication preserves current body position and velocities");
            // The body owner already proved the deliberately extreme saved velocity.
            // An idle fixture stays beside the camera while real publication frames run.
            gamma.setDeltaMovement(Vec3.ZERO);gamma.setKnownMovement(Vec3.ZERO);
        }
        boolean finishViews() {
            ackChunks();
            var position=gamma.position();var velocity=gamma.getDeltaMovement();var input=gamma.getKnownMovement();float yaw=gamma.getYRot(),pitch=gamma.getXRot();
            lastPending=actors.finishViewsFrozen();boolean ready=lastPending.isEmpty();
            require(gamma.position().equals(position)&&gamma.getDeltaMovement().equals(velocity)&&gamma.getKnownMovement().equals(input)&&gamma.getYRot()==yaw&&gamma.getXRot()==pitch,"deferred camera phase preserves current position/rotation and both velocities");return ready;
        }
        void verify() {
            var menu=(ChestMenu)gamma.containerMenu;var pair=(PlayerViewCheckpointAccess.Compound)menu.getContainer();
            require(pair.skywars2$first()==container(0)&&pair.skywars2$second()==container(1)&&container(0)!=oldBacking.getFirst(),"new double chest binds restored block entities, not removed instances");
            require(((PlayerViewCheckpointAccess.Hopper)base.owner.containerMenu).skywars2$container()==container(2)
                    &&((PlayerViewCheckpointAccess.Dispenser)base.other.containerMenu).skywars2$container()==container(3),"hopper and dropper native sessions bind restored block entities");
            require(menu.slots.stream().filter(s->s.container==gamma.getInventory()).count()==36&&menu.slots.stream().noneMatch(s->s.container==retired.getInventory()),"all native inventory slots bind the actual current player");
            require(container(1).getItem(7).getCount()==19&&container(2).getItem(4).getCount()==13&&container(3).getItem(8).getCount()==11,"terrain is the sole owner of storage item restoration");
            require(gamma.getInventory().getItem(12).is(Items.LAPIS_LAZULI)&&gamma.getInventory().getItem(12).getCount()==17
                    &&gamma.inventoryMenu.getCraftSlots().getItem(0).getCount()==3&&gamma.inventoryMenu.getCarried().is(Items.COPPER_INGOT),"body owns personal inventory/crafting/cursor independently from storage cursor");
            require(menu.getCarried().is(Items.GOLD_INGOT)&&menu.getCarried().getCount()==8&&menu.getCarried().getHoverName().getString().equals("Saved cursor components")
                    &&base.owner.containerMenu.getCarried().getCount()==5&&base.other.containerMenu.getCarried().getCount()==7,"independent native cursor copies/components for every session");
            var drag=(PlayerViewCheckpointAccess.Drag)menu;
            require(drag.skywars2$type()==0&&drag.skywars2$status()==1&&drag.skywars2$slots().equals(Set.of(menu.getSlot(0),menu.getSlot(1))),"native quick-craft state rebinds NEW slot objects");
            require(ChestBlockEntity.getOpenCount(base.owner.level(),positions.get(0))==1&&ChestBlockEntity.getOpenCount(base.owner.level(),positions.get(1))==1,"native double-chest viewer counts balance close/reopen");
            require(gamma.getCamera()==camera&&base.owner.getCamera()==gamma&&base.other.getCamera()==base.other,"camera UUID binds original actor, actual replaced player and self");
            require(!FullInvisibility.proxyFor(gamma.getId(),base.owner)&&FullInvisibility.proxyFor(gamma.getId(),base.other),"shared full-invisibility camera exception binds actual restored body");
            require(packets().stream().filter(ClientboundOpenScreenPacket.class::isInstance).map(ClientboundOpenScreenPacket.class::cast).anyMatch(p->p.getTitle().getString().equals("Saved native double chest"))
                    &&packets().stream().anyMatch(ClientboundContainerSetContentPacket.class::isInstance)
                    &&packets().stream().filter(ClientboundSetCameraPacket.class::isInstance).map(ClientboundSetCameraPacket.class::cast).anyMatch(p->p.getEntity(gamma.level())==camera),"native titled screen, full content and exact current camera ID packets emitted");
            if(phase!=7) {
                var all=packets();int respawn=-1,paired=-1,attached=-1;
                for(int i=0;i<all.size();i++) {
                    var packet=all.get(i);
                    if(packet instanceof ClientboundRespawnPacket)respawn=i;
                    if(packet instanceof ClientboundAddEntityPacket add&&add.getId()==camera.getId()&&respawn>=0)paired=i;
                    if(packet instanceof ClientboundSetCameraPacket view&&view.getEntity(gamma.level())==camera)attached=i;
                }
                require(respawn>=0&&paired>respawn&&attached>paired,"native camera target is paired AFTER client respawn and BEFORE camera attachment: "+respawn+"/"+paired+"/"+attached);
            }
            require(actors.restoreLinksFrozen().isEmpty()&&gamma.containerMenu.getCarried().getCount()==8&&ChestBlockEntity.getOpenCount(base.owner.level(),positions.get(0))==1,"repeated link replay neither duplicates cursor nor leaks viewers");
            gamma.containerMenu.clicked(-999,2,ContainerInput.QUICK_CRAFT,gamma);
            require(gamma.containerMenu.getCarried().isEmpty()&&container(0).getItem(0).getCount()==4&&container(0).getItem(1).getCount()==4,"restored drag executes native distribution exactly once");
            // Unsupported/plugin storage fails before closing a supported session or touching actors.
            MatchPlayerViews.closeFutureStorage(base.other);
            base.other.openMenu(new SimpleMenuProvider((n,inv,p)->ChestMenu.threeRows(n,inv,new SimpleContainer(27)),Component.literal("Unsupported plugin backing")));
            var unsupported=base.other.containerMenu;boolean rejected=false;
            try{actors.prepareViewsFrozen();}catch(IllegalStateException expected){rejected=true;}
            require(rejected&&base.other.containerMenu==unsupported&&gamma.containerMenu instanceof ChestMenu&&!camera.isRemoved(),"preflight rejects unsupported future menus before any mutation");
            unsupported.setCarried(ItemStack.EMPTY);base.other.closeContainer();require(actors.restoreLinksFrozen().isEmpty(),"supported images remain replayable after failed preflight");
        }
        void waitState() {
            for(var p:List.of(base.owner,base.other,gamma)){p.setCamera(p);MatchPlayerViews.closeFutureStorage(p);p.setDeltaMovement(Vec3.ZERO);}
            base.move(base.owner,at.add(-40,0,-40));base.move(base.other,at.add(40,0,-40));gamma.setKnownMovement(Vec3.ZERO);
        }
        boolean step() {
            require(++ticks<180,"bounded native views phase="+phase+" pending="+lastPending+" pos="+(gamma==null?null:gamma.position())
                    +" chunk="+(gamma==null||camera==null?null:gamma.level().getChunkSource().chunkMap.isChunkTracked(gamma,camera.chunkPosition().x(),camera.chunkPosition().z()))
                    +" tracking="+(gamma!=null&&camera!=null&&net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(camera).contains(gamma))
                    +" loaded="+(gamma!=null&&gamma.connection.hasClientLoaded())+" changing="+(gamma!=null&&gamma.isChangingDimension()));
            if(phase==-1){prepare();return false;}
            if(phase==0){mutate();phase=1;return false;}
            if(phase==1||phase==3){if(!restore.step(32768))return false;restore.close();restore=null;bodies();phase=phase==1?5:6;return false;}
            if(phase==5||phase==6){if(!finishViews())return false;verify();
                if(phase==5){waitState();game.server().tickRateManager().setFrozen(false);phase=2;return false;}
                gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity views offline fixture")));
                actors.restoreBodiesFrozen();require(actors.restoreLinksFrozen().equals(Set.of(id))&&base.owner.getCamera()==base.owner,"offline player and camera remain pending UUID with safe self view");
                gamma=GameChecks.createBot(game,"TestGamma");wire();restore=terrain.restore(game);phase=4;return false;}
            if(phase==4){if(!restore.step(32768))return false;restore.close();restore=null;core.restoreFrozen();actors.restoreBodiesFrozen();
                for(var image:players)image.restoreFrozen();gamma=game.server().getPlayerList().getPlayer(id);
                var pending=actors.restoreLinksFrozen();require(Set.of(id,camera.getUUID()).containsAll(pending),"explicit login replay rebinds native sessions and waits only for camera pairing");phase=7;return false;}
            if(phase==7){if(!finishViews())return false;verify();return true;}
            if(phase==2){if(game.matchTicks()-clock<4)return false;game.server().tickRateManager().setFrozen(true);mutate();phase=3;}
            return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);
            try {
                if(restore!=null)restore.close();
                for(var p:List.of(base.owner,base.other)){p.containerMenu.setCarried(ItemStack.EMPTY);if(p.containerMenu!=p.inventoryMenu)p.closeContainer();p.setCamera(p);}
                var live=game.server().getPlayerList().getPlayer(id);if(owned&&live!=null){live.containerMenu.setCarried(ItemStack.EMPTY);live.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity views cleanup")));}
                if(originalActors!=null){originalActors.restoreBodiesFrozen();originalActors.restoreLinksFrozen();}
                base.close();originalCore.restoreFrozen();
            }finally{game.server().getLevel(net.minecraft.world.level.Level.NETHER).getChunkSource().removeTicketWithRadius(resident,new net.minecraft.world.level.ChunkPos(0,0),0);game.server().tickRateManager().setFrozen(frozen);}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-views (native terrain-backed double chest/hopper/dropper sessions and titles/cursors/drag, actual death/respawn/current inventory links, camera UUIDs without body teleport or velocity loss, viewer balance/idempotence, real ticks and offline/login replay, unsupported preflight; private FPV/full coordinator remain gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-views",error);}}
}
