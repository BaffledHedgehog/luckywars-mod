package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.PlayerViewCheckpointAccess;
import me.skywars2.mixin.TerrainCheckpointEventsAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec3;

/** Native storage/processing widgets compose with canonical body/terrain and ordered block events. */
public final class VanityStorageChecks {
    private VanityStorageChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity storage: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ChefChecks.Fixture base;
        final MatchCoreCheckpoint originalCore;
        final MatchBlockEvents originalEvents=new MatchBlockEvents();
        final UUID id=GameChecks.offlineUuid("TestGamma");
        final boolean frozen;
        MatchActorCheckpoint originalActors,actors;
        MatchCoreCheckpoint core;
        List<MatchPlayerCheckpoint> players;
        MatchTerrainCheckpoint terrain;
        MatchTerrainCheckpoint.Restore restore;
        List<BlockPos> positions;
        List<BlockEntity> oldBlocks;
        List<BlockEventData> events,deferred;
        Set<Long> chunks;
        BlockEventData outside;
        ServerPlayer gamma,retired;
        ElementalGrimoireChecks.Recording wire;
        int phase=-1,round,ticks,clock;
        boolean owned,closed;
        final int[][] selection={{0,1,2},{3,3,4},{6,5,0}}; // Gamma/Alpha/Beta; two owners share one ender block.
        final int[][] data={{157,160,37,200},{111,13,400,20},{97,100,29,100},{77,80,13,100}};
        Fixture(GameController game) {
            this.game=game;frozen=game.server().tickRateManager().isFrozen();
            require(!frozen&&game.playing()&&!game.maps().busy()&&game.server().getPlayerList().getPlayer(id)==null,"exclusive native match and owned Gamma");
            originalCore=new MatchCoreCheckpoint(game);base=new ChefChecks.Fixture(game);
        }
        void init() {
            originalActors=MatchActorCheckpoint.captureRegistered(game);
            for(int x=base.bounds.minX()>>4;x<=base.bounds.maxX()>>4;x++)for(int z=base.bounds.minZ()>>4;z<=base.bounds.maxZ()>>4;z++)originalEvents.include(base.owner.level(),new ChunkPos(x,z));
            base.init();gamma=GameChecks.createBot(game,"TestGamma");owned=true;
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
            SkyWars2.LOGGER.info("[check] START vanity-storage");
        }
        void connect() {
            wire=new ElementalGrimoireChecks.Recording(gamma);
            gamma.connection=new ServerGamePacketListenerImpl(game.server(),wire,gamma,new CommonListenerCookie(gamma.getGameProfile(),0,gamma.clientInformation(),false));
            gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        List<ServerPlayer> live(){return List.of(gamma,base.owner,base.other);}
        BlockEntity block(int n){return base.owner.level().getBlockEntity(positions.get(n));}
        Container container(int n){return (Container)block(n);}
        ContainerData values(int n){return MatchPlayerViews.blockData(container(n));}
        void setup() {
            game.server().tickRateManager().setFrozen(true);require(game.allowDamage(gamma)&&game.lives(id)==1,"actual operator-enrolled participant");
            connect();positions=List.of(base.center.offset(-2,0,-2),base.center.offset(0,0,-2),base.center.offset(2,0,-2),
                    base.center.offset(-2,0,0),base.center.offset(2,0,0),base.center.offset(-2,0,2),base.center.offset(0,0,2));
            var kinds=List.of(Blocks.SHULKER_BOX,Blocks.FURNACE,Blocks.BREWING_STAND,Blocks.ENDER_CHEST,Blocks.BLAST_FURNACE,Blocks.SMOKER,Blocks.CRAFTER);
            for(int i=0;i<kinds.size();i++)base.block(positions.get(i),kinds.get(i));
            container(0).setItem(4,new ItemStack(Items.DIAMOND,17));
            for(int n:List.of(1,4,5)){container(n).setItem(0,new ItemStack(n==5?Items.BEEF:Items.RAW_IRON,8));container(n).setItem(1,new ItemStack(Items.COAL,7));container(n).setItem(2,new ItemStack(n==5?Items.COOKED_BEEF:Items.IRON_INGOT,3));}
            for(int n=0;n<3;n++)container(2).setItem(n,PotionContents.createItemStack(Items.POTION,Potions.WATER));
            container(2).setItem(3,new ItemStack(Items.NETHER_WART,5));container(2).setItem(4,new ItemStack(Items.BLAZE_POWDER,7));
            for(int i=0;i<4;i++){var target=values(new int[]{1,2,4,5}[i]);for(int j=0;j<4;j++)target.set(j,data[i][j]);}
            container(6).setItem(0,new ItemStack(Items.OAK_LOG,2));((CrafterBlockEntity)block(6)).setSlotState(8,false);values(6).set(9,1);
            chunks=positions.stream().map(ChunkPos::pack).collect(java.util.stream.Collectors.toSet());
            prepare();
        }
        void open(ServerPlayer p,int n) {
            if(n==3)p.getEnderChestInventory().setActiveChest((EnderChestBlockEntity)block(3));
            p.openMenu(new SimpleMenuProvider((number,inventory,player)->switch(n){
                case 0 -> new ShulkerBoxMenu(number,inventory,container(0));
                case 1 -> new FurnaceMenu(number,inventory,container(1),values(1));
                case 2 -> new BrewingStandMenu(number,inventory,container(2),values(2));
                case 3 -> ChestMenu.threeRows(number,inventory,p.getEnderChestInventory());
                case 4 -> new BlastFurnaceMenu(number,inventory,container(4),values(4));
                case 5 -> new SmokerMenu(number,inventory,container(5),values(5));
                case 6 -> new CrafterMenu(number,inventory,(CrafterBlockEntity)block(6),values(6));
                default -> throw new IllegalArgumentException();
            },Component.literal("Native storage "+n)));
        }
        void prepare() {
            for(var p:live()){
                MatchPlayerViews.closeFutureStorage(p);base.move(p,base.at.add(0,1,0));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
                p.getInventory().clearContent();p.getEnderChestInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);
            }
            gamma.getEnderChestInventory().setItem(7,new ItemStack(Items.DIAMOND,19));base.owner.getEnderChestInventory().setItem(7,new ItemStack(Items.EMERALD,23));
            gamma.inventoryMenu.getCraftSlots().setItem(0,new ItemStack(Items.APPLE,2));gamma.inventoryMenu.setCarried(new ItemStack(Items.COPPER_INGOT,3));
            for(int i=0;i<3;i++){
                var p=live().get(i);open(p,selection[round][i]);var cursor=new ItemStack(Items.GOLD_INGOT,11+i);
                cursor.set(DataComponents.CUSTOM_NAME,Component.literal("Storage cursor "+i));p.containerMenu.setCarried(cursor);
            }
            oldBlocks=positions.stream().map(p->base.owner.level().getBlockEntity(p)).toList();
            actors=MatchActorCheckpoint.captureRegistered(game);players=live().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();core=new MatchCoreCheckpoint(game);
            terrain=new MatchTerrainCheckpoint(game.server());for(var pos:positions)terrain.include(base.owner.level().getChunkAt(pos));
            var access=(TerrainCheckpointEventsAccess)base.owner.level();events=owned(access.skywars2$events());deferred=owned(access.skywars2$deferred());
            clock=game.matchTicks();phase=0;
        }
        List<BlockEventData> owned(java.util.Collection<BlockEventData> all){return all.stream().filter(e->chunks.contains(ChunkPos.pack(e.pos()))).toList();}
        void mutate() {
            for(var p:live()){p.containerMenu.setCarried(new ItemStack(Items.NETHERITE_INGOT,31));p.getEnderChestInventory().setItem(7,new ItemStack(Items.ROTTEN_FLESH,64));}
            actors.prepareViewsFrozen();for(var pos:positions)base.block(pos,Blocks.STONE);
            var level=base.owner.level();level.blockEvent(positions.getFirst(),Blocks.SHULKER_BOX,1,37);
            ((TerrainCheckpointEventsAccess)level).skywars2$deferred().add(new BlockEventData(positions.getFirst(),Blocks.SHULKER_BOX,1,37));
            outside=new BlockEventData(base.center.offset(256,0,256),Blocks.NOTE_BLOCK,7,91);((TerrainCheckpointEventsAccess)level).skywars2$events().add(outside);
            gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;
            require(gamma.hurtServer(level,gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying(),"real native death before storage rebind");
            require(base.owner.isAlive()&&base.other.isAlive(),"borrowed bodies remain alive");restore=terrain.restore(game);phase=1;
        }
        void restoreBodies(boolean died,boolean offline) {
            retired=gamma;wire.packets.clear();core.restoreFrozen();actors.restoreBodiesFrozen();
            for(var image:players)if(game.server().getPlayerList().getPlayer(image.id())!=null)image.restoreFrozen();
            if(!offline){gamma=game.server().getPlayerList().getPlayer(id);gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(died)require(gamma!=retired,"native player respawn replaced actual inventory/ender owner");}
            require(actors.restoreLinksFrozen().equals(offline?Set.of(id):Set.of()),"native storage owner UUID resolution");
        }
        Object field(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        int viewers(){return ((ContainerOpenersCounter)field(block(3),"openersCounter")).getOpenerCount();}
        Container backing(AbstractContainerMenu menu){
            if(menu instanceof ChestMenu chest)return chest.getContainer();
            if(menu instanceof ShulkerBoxMenu)return ((PlayerViewCheckpointAccess.Shulker)menu).skywars2$container();
            if(menu instanceof AbstractFurnaceMenu)return ((PlayerViewCheckpointAccess.Furnace)menu).skywars2$container();
            if(menu instanceof BrewingStandMenu)return ((PlayerViewCheckpointAccess.Brewing)menu).skywars2$container();
            return ((PlayerViewCheckpointAccess.Crafter)menu).skywars2$container();
        }
        ContainerData menuData(AbstractContainerMenu menu){
            if(menu instanceof AbstractFurnaceMenu)return ((PlayerViewCheckpointAccess.Furnace)menu).skywars2$data();
            if(menu instanceof BrewingStandMenu)return ((PlayerViewCheckpointAccess.Brewing)menu).skywars2$data();
            return ((PlayerViewCheckpointAccess.Crafter)menu).skywars2$data();
        }
        void flatten(Packet<?> p,List<Packet<?>> out){if(p instanceof ClientboundBundlePacket b)for(var child:b.subPackets())flatten(child,out);else out.add(p);}
        List<Packet<?>> packets(){var out=new ArrayList<Packet<?>>();for(var p:wire.packets)flatten(p,out);return out;}
        void verify() {
            var access=(TerrainCheckpointEventsAccess)base.owner.level();
            require(owned(access.skywars2$events()).equals(events)&&owned(access.skywars2$deferred()).equals(deferred),"ordered native owned queued/deferred events restored without future close/count37");
            require(access.skywars2$events().contains(outside),"partial terrain restore preserves unowned future events");access.skywars2$events().remove(outside);
            for(int i=0;i<positions.size();i++)require(block(i)!=oldBlocks.get(i)&&!block(i).isRemoved(),"terrain recreated native block entity "+i);
            for(int i=0;i<3;i++) {
                var p=live().get(i);int n=selection[round][i];var menu=p.containerMenu;
                require(backing(menu)==(n==3?p.getEnderChestInventory():container(n)),"native menu binds current block/player inventory "+n);
                require(menu.slots.stream().filter(s->s.container==p.getInventory()).count()==36
                        &&(retired==gamma||menu.slots.stream().noneMatch(s->s.container==retired.getInventory())),"current native player inventory slots "+n);
                require(menu.getCarried().getCount()==11+i&&menu.getCarried().getHoverName().getString().equals("Storage cursor "+i),"cursor components restored "+n);
                if(n==3)require(((PlayerViewCheckpointAccess.Ender)p.getEnderChestInventory()).skywars2$active()==block(3),"ender active pointer binds restored block");
                if(n==1||n==2||n==4||n==5||n==6)require(menuData(menu)==values(n),"progress/disabled slots use actual restored native data, not a copied array");
            }
            require(gamma.getEnderChestInventory().getItem(7).is(Items.DIAMOND)&&gamma.getEnderChestInventory().getItem(7).getCount()==19
                    &&base.owner.getEnderChestInventory().getItem(7).is(Items.EMERALD)&&base.owner.getEnderChestInventory().getItem(7).getCount()==23,"two owners retain distinct ender inventories");
            require(gamma.inventoryMenu.getCraftSlots().getItem(0).getCount()==2&&gamma.inventoryMenu.getCarried().is(Items.COPPER_INGOT),"personal buffers remain independent of active storage");
            for(int i=0;i<4;i++)for(int j=0;j<4;j++)require(values(new int[]{1,2,4,5}[i]).get(j)==data[i][j],"native terrain restored processing field "+i+"/"+j);
            require(viewers()==(round==1?2:0)&&(int)field(block(0),"openCount")==((round==0||round==2)?1:0),"native ender/shulker viewer counts");
            var menu=gamma.containerMenu;int menuId=menu.containerId;int packetsBefore=packets().size();var eventBefore=List.copyOf(access.skywars2$events());
            require(actors.restoreLinksFrozen().isEmpty()&&gamma.containerMenu==menu&&menu.containerId==menuId
                    &&List.copyOf(access.skywars2$events()).equals(eventBefore),"idempotent replay retains valid menu and emits no extra block events");
            var after=packets();require(after.subList(packetsBefore,after.size()).stream().noneMatch(ClientboundOpenScreenPacket.class::isInstance),"same valid session is not reopened");
            if(round==0){require(!gamma.containerMenu.getSlot(0).mayPlace(new ItemStack(Items.SHULKER_BOX)),"restored native shulker slot forbids nested shulkers");}
            if(round==1)require(viewers()==2,"shared restored ender block tracks both native viewers exactly once");
            if(round==2){var crafter=(CrafterMenu)gamma.containerMenu;require(crafter.isSlotDisabled(8)&&crafter.isPowered()
                    &&crafter.getSlot(45).getItem().is(Items.OAK_PLANKS)&&crafter.getSlot(45).getItem().getCount()==4&&container(6).getItem(0).getCount()==2,
                    "native crafter reconstructs disabled/powered data and recipe preview without consuming inputs");
                require(packets().stream().filter(ClientboundContainerSetDataPacket.class::isInstance).map(ClientboundContainerSetDataPacket.class::cast)
                        .anyMatch(p->p.getContainerId()==crafter.containerId&&p.getId()==8&&p.getValue()==1),"native crafter disabled state is published");}
        }
        boolean step() {
            require(++ticks<240,"bounded native storage phase="+phase+" round="+round);
            if(phase==-1){setup();return false;}
            if(phase==0){mutate();return false;}
            if(phase==1||phase==3||phase==4||phase==5){if(!restore.step(32768))return false;restore.close();restore=null;
                if(phase==4){restoreBodies(false,true);require(viewers()==1&&((PlayerViewCheckpointAccess.Ender)base.owner.getEnderChestInventory()).skywars2$active()==block(3),"offline ender owner stays pending while current owner binds restored block");
                    gamma=GameChecks.createBot(game,"TestGamma");connect();actors.prepareViewsFrozen();((TerrainCheckpointEventsAccess)base.owner.level()).skywars2$events().add(outside);restore=terrain.restore(game);phase=5;return false;}
                int previous=phase;restoreBodies(previous==1||previous==3,false);verify();
                if(round==0&&previous==1){for(var p:live())p.setDeltaMovement(Vec3.ZERO);game.server().tickRateManager().setFrozen(false);phase=2;return false;}
                if(round==1&&previous==1){gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity ender offline fixture")));actors.prepareViewsFrozen();restore=terrain.restore(game);phase=4;return false;}
                if(++round==3)return true;prepare();return false;}
            if(phase==2){if(game.matchTicks()-clock<4)return false;game.server().tickRateManager().setFrozen(true);
                require(values(1).get(0)<data[0][0]&&menuData(base.owner.containerMenu).get(0)==values(1).get(0),"real native furnace ticks update restored menu data");
                require((int)field(block(0),"openCount")==1,"old queued close did not override restored shulker after real world ticks");
                mutate();phase=3;}
            return false;
        }
        @Override public void close() {
            if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);
            try {
                if(restore!=null)restore.close();
                for(var p:List.of(base.owner,base.other)){p.containerMenu.setCarried(ItemStack.EMPTY);p.closeContainer();}
                var p=game.server().getPlayerList().getPlayer(id);if(owned&&p!=null){p.containerMenu.setCarried(ItemStack.EMPTY);p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity storage cleanup")));}
                if(originalActors!=null){originalActors.restoreBodiesFrozen();originalActors.restoreLinksFrozen();}
                base.close();originalEvents.restore();originalCore.restoreFrozen();
                if(outside!=null){var access=(TerrainCheckpointEventsAccess)base.owner.level();access.skywars2$events().remove(outside);access.skywars2$deferred().remove(outside);}
            }finally{game.server().tickRateManager().setFrozen(frozen);}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-storage (shulker/ender/furnace/blast/smoker/brewing/crafter native menus, current inventory/BE/data bindings, independent ender owners and offline replay, real respawn, progress and native recipe preview, ordered queued/deferred block events and unowned isolation, event-free idempotent sessions, real world ticks; full coordinator remains gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-storage",error);}}
}
