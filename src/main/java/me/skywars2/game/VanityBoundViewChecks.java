package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.BoundViewCheckpointAccess;
import me.skywars2.mixin.WorkstationCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec3;

/** Native actor/block-backed UI, real respawn, deferred pairing, trade/payment/book/equipment operations. */
public final class VanityBoundViewChecks {
    private VanityBoundViewChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity bound views: "+why);}
    private record State(Class<?> kind,List<ItemStack> items,List<Integer> data,ItemStack cursor,int xp){
        static State of(ServerPlayer p){return new State(p.containerMenu.getClass(),p.containerMenu.slots.stream().map(s->s.getItem().copy()).toList(),
                ((WorkstationCheckpointAccess.Data)p.containerMenu).skywars2$data().stream().map(DataSlot::get).toList(),p.containerMenu.getCarried().copy(),p.experienceLevel);}
        void verify(ServerPlayer p,String why){var n=of(p);require(n.kind==kind&&n.items.size()==items.size()&&n.data.equals(data)&&n.xp==xp&&ItemStack.matches(n.cursor,cursor),why+" shape/data/XP/cursor");
            for(int i=0;i<items.size();i++)require(ItemStack.matches(items.get(i),n.items.get(i)),why+" slot "+i);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final MatchCoreCheckpoint originalCore;
        final MatchBlockEvents originalEvents=new MatchBlockEvents();final UUID id=GameChecks.offlineUuid("TestGamma");final boolean frozen;
        final net.minecraft.server.level.TicketType resident=new net.minecraft.server.level.TicketType(0,net.minecraft.server.level.TicketType.FLAG_LOADING|net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
        MatchActorCheckpoint originalActors,actors;MatchCoreCheckpoint core;List<MatchPlayerCheckpoint> players;
        MatchTerrainCheckpoint terrain;MatchTerrainCheckpoint.Restore restore;
        ServerPlayer gamma,retired;Entity actor;ElementalGrimoireChecks.Recording wire;
        BlockPos beacon,lectern,table;BlockEntity oldBeacon,oldLectern;State saved,peer,consumed;
        MerchantOffer oldOffer;int round,phase=-1,ticks,consumedUses,consumedXp;boolean owned,closed;
        final String[] kinds={null,null,"villager","wandering_trader","horse","donkey","llama","nautilus","chest_minecart","hopper_minecart","oak_chest_boat"};
        Fixture(GameController game){this.game=game;frozen=game.server().tickRateManager().isFrozen();require(!frozen&&game.playing()&&!game.maps().busy()&&game.server().getPlayerList().getPlayer(id)==null,"exclusive actual native match");originalCore=new MatchCoreCheckpoint(game);base=new ChefChecks.Fixture(game);}
        void init(){
            var nether=game.server().getLevel(Level.NETHER);nether.getChunkSource().addTicketWithRadius(resident,new net.minecraft.world.level.ChunkPos(0,0),0);nether.getChunk(0,0);
            originalActors=MatchActorCheckpoint.captureRegistered(game);for(int x=base.bounds.minX()>>4;x<=base.bounds.maxX()>>4;x++)for(int z=base.bounds.minZ()>>4;z<=base.bounds.maxZ()>>4;z++)originalEvents.include(base.owner.level(),new net.minecraft.world.level.ChunkPos(x,z));
            base.init();gamma=GameChecks.createBot(game,"TestGamma");owned=true;game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");SkyWars2.LOGGER.info("[check] START vanity-bound-views");
        }
        void connect(){wire=new ElementalGrimoireChecks.Recording(gamma);gamma.connection=new ServerGamePacketListenerImpl(game.server(),wire,gamma,new CommonListenerCookie(gamma.getGameProfile(),0,gamma.clientInformation(),false));gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());}
        List<ServerPlayer> live(){return List.of(gamma,base.owner,base.other);}
        void flatten(Packet<?> p,List<Packet<?>> out){if(p instanceof ClientboundBundlePacket bundle)for(var q:bundle.subPackets())flatten(q,out);else out.add(p);}
        List<Packet<?>> packets(){var all=new ArrayList<Packet<?>>();for(var p:wire.packets)flatten(p,all);return all;}
        void ack(){
            try{var f=net.minecraft.server.network.PlayerChunkSender.class.getDeclaredField("unacknowledgedBatches");f.setAccessible(true);
                for(var p:live())if(game.server().getPlayerList().getPlayer(p.getUUID())==p){int n=f.getInt(p.connection.chunkSender);for(int i=0;i<n;i++)p.connection.handleChunkBatchReceived(new ServerboundChunkBatchReceivedPacket(9));}
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var teleports=packets().stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();
            if(!teleports.isEmpty())gamma.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(teleports.getLast().id(),gamma.getX(),gamma.getY(),gamma.getZ(),gamma.getYRot(),gamma.getXRot()));
        }
        ContainerData beaconData(){return ((BoundViewCheckpointAccess.BeaconBlock)base.owner.level().getBlockEntity(beacon)).skywars2$data();}
        ContainerData lecternData(){return ((BoundViewCheckpointAccess.LecternBlock)base.owner.level().getBlockEntity(lectern)).skywars2$data();}
        MerchantOffers offers(){return ((BoundViewCheckpointAccess.Villager)actor).skywars2$offers();}
        Container mount(){return MatchBoundViews.mountInventory((LivingEntity)actor);}
        void beacon(ServerPlayer p){p.openMenu(new SimpleMenuProvider((n,inv,pl)->new BeaconMenu(n,inv,beaconData(),ContainerLevelAccess.create(p.level(),beacon)),Component.literal("Bound beacon")));p.containerMenu.getSlot(0).set(new ItemStack(p==gamma?Items.DIAMOND:Items.EMERALD));}
        void open(ServerPlayer p){
            if(round==0){beacon(p);return;}
            if(round==1){var b=(BoundViewCheckpointAccess.LecternBlock)p.level().getBlockEntity(lectern);p.openMenu(new SimpleMenuProvider((n,inv,pl)->new LecternMenu(n,b.skywars2$book(),b.skywars2$data()),Component.literal("Bound lectern")));return;}
            if(actor instanceof AbstractVillager v){v.setTradingPlayer(p);v.openTradingScreen(p,Component.literal("Bound trader"),4);var m=(MerchantMenu)p.containerMenu;m.setMerchantLevel(3);m.setShowProgressBar(false);m.setCanRestock(true);m.setSelectionHint(1);
                m.getSlot(0).set(new ItemStack(Items.EMERALD,20));m.getSlot(1).set(new ItemStack(Items.DIAMOND,2));return;}
            if(actor instanceof AbstractHorse h){p.openHorseInventory(h,mount());return;}
            if(actor instanceof AbstractNautilus n){p.openNautilusInventory(n,mount());return;}
            p.openMenu((net.minecraft.world.MenuProvider)actor);
        }
        void setup(){game.server().tickRateManager().setFrozen(true);connect();require(game.allowDamage(gamma)&&game.lives(id)==1,"actual one-life native participant");beacon=base.center.offset(-2,0,-2);lectern=base.center.offset(2,0,-2);table=base.center.offset(0,0,-2);prepare();}
        void prepare(){
            for(var p:live()){MatchPlayerViews.closeFutureStorage(p);base.move(p,base.at.add(0,1,0));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);p.experienceLevel=40;}
            if(actor!=null&&!actor.isRemoved())actor.setRemoved(Entity.RemovalReason.DISCARDED);actor=null;
            base.block(beacon,Blocks.BEACON);base.block(lectern,Blocks.LECTERN);base.block(table,Blocks.CRAFTING_TABLE);
            base.owner.level().setBlock(lectern,Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.HAS_BOOK,true),Block.UPDATE_CLIENTS);
            beaconData().set(0,4);beaconData().set(1,BeaconMenu.encodeEffect(MobEffects.SPEED));beaconData().set(2,BeaconMenu.encodeEffect(MobEffects.REGENERATION));
            var book=new ItemStack(Items.WRITABLE_BOOK);book.set(DataComponents.WRITABLE_BOOK_CONTENT,new WritableBookContent(List.of(Filterable.passThrough("One"),Filterable.passThrough("Two"),Filterable.passThrough("Three"),Filterable.passThrough("Four"))));
            ((LecternBlockEntity)base.owner.level().getBlockEntity(lectern)).setBook(book);lecternData().set(0,1);
            if(round>=2){actor=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:"+kinds[round])).create(base.owner.level(),EntitySpawnReason.COMMAND);require(actor!=null,"native actor factory "+round);actor.setPos(base.at.add(1,1,0));actor.setNoGravity(true);
                if(actor instanceof net.minecraft.world.entity.Mob mob)mob.setNoAi(true);
                if(actor instanceof net.minecraft.world.entity.animal.equine.Llama l)((BoundViewCheckpointAccess.LlamaStrength)l).skywars2$strength(3);
                if(actor instanceof AbstractChestedHorse h){h.setChest(true);((BoundViewCheckpointAccess.Horse)h).skywars2$resize();}
                if(actor instanceof LivingEntity living&&(actor instanceof AbstractHorse||actor instanceof AbstractNautilus))living.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
                if(actor instanceof net.minecraft.world.entity.vehicle.ContainerEntity c)c.setItem(0,new ItemStack(Items.DIAMOND,13));
                if(actor instanceof AbstractHorse||actor instanceof AbstractNautilus){var c=mount();if(c.getContainerSize()>0)c.setItem(0,new ItemStack(Items.EMERALD,17));}
                if(actor instanceof AbstractVillager v){var o=new MerchantOffers();o.add(new MerchantOffer(new ItemCost(Items.EMERALD,1),new ItemStack(Items.DIAMOND),10,1,0));
                    var chosen=new MerchantOffer(new ItemCost(Items.EMERALD,3),Optional.of(new ItemCost(Items.DIAMOND,1)),new ItemStack(Items.GOLDEN_APPLE,2),3,19,4,.2f,2);chosen.addToSpecialPriceDiff(-1);o.add(chosen);((BoundViewCheckpointAccess.Villager)v).skywars2$offers(o);v.overrideXp(17);oldOffer=chosen;}
                require(base.owner.level().addFreshEntity(actor),"native actor registration");
            }
            open(gamma);if(round<=1||round>=8)open(base.owner);else beacon(base.owner);
            gamma.inventoryMenu.getCraftSlots().setItem(0,new ItemStack(Items.APPLE,2));gamma.inventoryMenu.setCarried(new ItemStack(Items.COPPER_INGOT,3));
            for(var p:List.of(gamma,base.owner)){var cursor=new ItemStack(Items.GOLD_INGOT,p==gamma?8:9);cursor.set(DataComponents.CUSTOM_NAME,Component.literal(p==gamma?"Bound Gamma":"Bound Alpha"));p.containerMenu.setCarried(cursor);}
            saved=State.of(gamma);peer=State.of(base.owner);oldBeacon=base.owner.level().getBlockEntity(beacon);oldLectern=base.owner.level().getBlockEntity(lectern);
            actors=MatchActorCheckpoint.captureRegistered(game);players=live().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();core=new MatchCoreCheckpoint(game);
            terrain=new MatchTerrainCheckpoint(game.server());for(var pos:List.of(beacon,lectern,table))terrain.include(base.owner.level().getChunkAt(pos));phase=0;
        }
        void mutate(){
            gamma.containerMenu.setCarried(new ItemStack(Items.NETHERITE_INGOT,31));
            if(round<=1)gamma.containerMenu.getSlot(0).set(new ItemStack(Items.ROTTEN_FLESH));
            if(actor instanceof AbstractVillager){offers().get(1).increaseUses();gamma.containerMenu.getSlot(0).set(new ItemStack(Items.ROTTEN_FLESH));}
            if(actor instanceof LivingEntity living&&(actor instanceof AbstractHorse||actor instanceof AbstractNautilus))living.setItemSlot(EquipmentSlot.SADDLE,ItemStack.EMPTY);
            if(actor instanceof net.minecraft.world.entity.vehicle.ContainerEntity c)c.setItem(0,new ItemStack(Items.ROTTEN_FLESH,64));
            actors.prepareViewsFrozen();require(live().stream().allMatch(p->p.getInventory().isEmpty()),"abandoned local payments/inputs/cursor are not returned into inventory");
            base.block(beacon,Blocks.STONE);base.block(lectern,Blocks.STONE);if(actor!=null)actor.setRemoved(Entity.RemovalReason.DISCARDED);
            if(round==4){require(gamma.teleportTo(game.server().getLevel(Level.NETHER),.5,200,.5,Set.of(),0,0,true),"native client world replacement before mount restore");ack();}
            gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying(),"real native death");restore=terrain.restore(game);phase=1;
        }
        void bodies(boolean offline){
            retired=gamma;wire.packets.clear();core.restoreFrozen();actors.restoreBodiesFrozen();for(var image:players)if(game.server().getPlayerList().getPlayer(image.id())!=null)image.restoreFrozen();
            if(!offline){gamma=game.server().getPlayerList().getPlayer(id);ack();
                if(round>=8){gamma.openMenu(new SimpleMenuProvider((n,inv,p)->new CraftingMenu(n,inv,ContainerLevelAccess.create(p.level(),table)),Component.literal("Future workstation family")));gamma.containerMenu.getSlot(1).set(new ItemStack(Items.DIRT,64));}}
            var pending=actors.restoreLinksFrozen();require(offline?pending.equals(Set.of(id)):pending.isEmpty()||actor!=null&&pending.equals(Set.of(actor.getUUID())),"only explicit offline UUID/native mount pairing may remain pending: "+pending);
        }
        boolean finish(){ack();return actors.finishViewsFrozen().isEmpty();}
        void verify(){
            saved.verify(gamma,"saved Gamma round "+round);peer.verify(base.owner,"saved Alpha round "+round);
            require(base.owner.level().getBlockEntity(beacon)!=oldBeacon&&base.owner.level().getBlockEntity(lectern)!=oldLectern,"current restored native BE objects");
            require(beaconData().get(0)==4&&lecternData().get(0)==1,"canonical terrain restored beacon Levels and lectern Page");
            require(gamma.inventoryMenu.getCraftSlots().getItem(0).getCount()==2&&gamma.inventoryMenu.getCarried().is(Items.COPPER_INGOT),"personal buffer isolation");
            int inventoryCount=round==1?0:36;require(gamma.containerMenu.slots.stream().filter(s->s.container==gamma.getInventory()).count()==inventoryCount&&(retired==gamma||gamma.containerMenu.slots.stream().noneMatch(s->s.container==retired.getInventory())),"current player inventory binding");
            if(actor instanceof AbstractVillager v){var a=(BoundViewCheckpointAccess.TradeBuffer)((BoundViewCheckpointAccess.Trade)gamma.containerMenu).skywars2$container();
                require(v.getTradingPlayer()==gamma&&a.skywars2$active()==offers().get(1)&&a.skywars2$active()!=oldOffer&&offers().get(1).getUses()==3,"trade active offer/customer bind current native objects without increasing uses");
                require(((MerchantMenu)gamma.containerMenu).getTraderLevel()==3&&!((MerchantMenu)gamma.containerMenu).showProgressBar()&&((MerchantMenu)gamma.containerMenu).canRestock(),"native merchant fields independent of published UI");
                var published=((MatchBoundViews.Publication)gamma).skywars2$offers(gamma.containerMenu.containerId);require(published.level()==4&&packets().stream().anyMatch(ClientboundMerchantOffersPacket.class::isInstance),"native published offers metadata packet restored");}
            if(actor instanceof AbstractHorse||actor instanceof AbstractNautilus){require(((BoundViewCheckpointAccess.Mount)gamma.containerMenu).skywars2$container()==mount(),"mount binds current resized inventory");
                var all=packets();int paired=-1,opened=-1,respawn=-1;for(int i=0;i<all.size();i++){var p=all.get(i);if(p instanceof ClientboundRespawnPacket)respawn=i;if(p instanceof ClientboundAddEntityPacket add&&add.getId()==actor.getId())paired=i;if(p instanceof ClientboundMountScreenOpenPacket mount&&mount.getEntityId()==actor.getId())opened=i;}
                require(paired>=0&&opened>paired&&(round!=4||paired>respawn),"native actor pairing precedes mount ID packet, including real client world replacement: "+paired+"/"+opened+"/"+respawn);}
            var menu=gamma.containerMenu;wire.packets.clear();require(actors.restoreLinksFrozen().isEmpty()&&gamma.containerMenu==menu,"same native session reused");
            require(packets().stream().noneMatch(p->p instanceof ClientboundOpenScreenPacket||p instanceof ClientboundMountScreenOpenPacket),"idempotent replay emits no extra window open");saved.verify(gamma,"repeated view image "+round);
        }
        void action(){
            if(round==0){require(((BeaconMenu)gamma.containerMenu).updateEffects(Optional.of(MobEffects.HASTE),Optional.of(MobEffects.REGENERATION))&&!((BeaconMenu)gamma.containerMenu).hasPayment(),"native beacon consumes only Gamma payment");require(base.owner.containerMenu.getSlot(0).getItem().is(Items.EMERALD),"independent peer payment");}
            else if(round==1){require(gamma.containerMenu.clickMenuButton(gamma,2)&&((LecternMenu)gamma.containerMenu).getPage()==2,"native shared page navigation");require(gamma.containerMenu.clickMenuButton(gamma,3)&&gamma.getInventory().contains(s->s.is(Items.WRITABLE_BOOK)),"native lectern book extraction");}
            else {int slot=actor instanceof AbstractVillager?2:0;require(gamma.containerMenu.getSlot(slot).mayPickup(gamma)&&!gamma.containerMenu.quickMoveStack(gamma,slot).isEmpty(),"native restored trade/equipment/storage transfer "+round);
                if(actor instanceof AbstractVillager){require(offers().get(1).getUses()==4&&gamma.getInventory().contains(s->s.is(Items.GOLDEN_APPLE)),"trade charges current native active offer exactly once");}}
        }
        boolean step(){
            require(++ticks<600,"bounded fixture round="+round+" phase="+phase);
            if(phase==-1){setup();return false;}if(phase==0){mutate();return false;}
            if(phase==1||phase==3||phase==5||phase==6){if(!restore.step(32768))return false;restore.close();restore=null;int previous=phase;
                if(previous==5){bodies(true);gamma=GameChecks.createBot(game,"TestGamma");connect();actors.prepareViewsFrozen();restore=terrain.restore(game);phase=6;return false;}
                bodies(false);phase=previous==1?2:previous==3?4:7;return false;}
            if(phase==2||phase==4||phase==7){if(!finish())return false;verify();
                if(phase==2){require(gamma!=retired,"actual native replacement player");action();consumed=State.of(gamma);if(actor instanceof AbstractVillager v){consumedUses=offers().get(1).getUses();consumedXp=v.getVillagerXp();}actors.prepareViewsFrozen();restore=terrain.restore(game);phase=3;return false;}
                if(phase==4){action();consumed.verify(gamma,"repeat native consumption "+round);if(actor instanceof AbstractVillager v)require(offers().get(1).getUses()==consumedUses&&v.getVillagerXp()==consumedXp,"repeat trade actor counters");
                    if(round==2||round==5){gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Bound view offline fixture")));actors.prepareViewsFrozen();restore=terrain.restore(game);phase=5;return false;}}
                if(++round==kinds.length)return true;prepare();return false;}
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);
            try{if(restore!=null)restore.close();for(var p:List.of(base.owner,base.other))MatchPlayerViews.closeFutureStorage(p);var p=game.server().getPlayerList().getPlayer(id);if(owned&&p!=null){MatchPlayerViews.closeFutureStorage(p);p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Bound views cleanup")));}
                if(originalActors!=null){originalActors.restoreBodiesFrozen();originalActors.restoreLinksFrozen();}base.close();originalEvents.restore();originalCore.restoreFrozen();
            }finally{game.server().getLevel(Level.NETHER).getChunkSource().removeTicketWithRadius(resident,new net.minecraft.world.level.ChunkPos(0,0),0);game.server().tickRateManager().setFrozen(frozen);}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-bound-views (native beacon/lectern/trade/horse/nautilus and storage vehicles; canonical current block data/actor inventories/offers, native customer/active offer and UI publication; deferred tracked mount-ID pairing after respawn/world replacement, offline trade/mount replay; actual payments/book/trade/equipment/storage operations and repeated resources/counters; cross-family future-menu transition; full coordinator remains gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-bound-views",error);}}
}
