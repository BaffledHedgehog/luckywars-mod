package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.WorkstationCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real native workspaces, recipes, pickup, death/offline replacement and repeated terrain/body/view replay. */
public final class VanityWorkstationChecks {
    private VanityWorkstationChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity workstations: "+why);}
    private record State(Class<?> kind,List<ItemStack> slots,List<Integer> data,ItemStack cursor,int xp) {
        static State of(ServerPlayer p){return new State(p.containerMenu.getClass(),p.containerMenu.slots.stream().map(s->s.getItem().copy()).toList(),
                ((WorkstationCheckpointAccess.Data)p.containerMenu).skywars2$data().stream().map(DataSlot::get).toList(),p.containerMenu.getCarried().copy(),p.experienceLevel);}
        void verify(ServerPlayer p,String why){verify(p,why,false);}
        void verify(ServerPlayer p,String why,boolean nextEnchantSeed){
            var now=of(p);require(now.kind==kind&&now.data.size()==data.size()&&now.xp==xp&&ItemStack.matches(cursor,now.cursor)&&slots.size()==now.slots.size(),why+" shape/XP/cursor");
            for(int i=0;i<data.size();i++)if(!(nextEnchantSeed&&kind==EnchantmentMenu.class&&i==3))require(now.data.get(i).equals(data.get(i)),why+" native data "+i);
            for(int i=0;i<slots.size();i++)require(ItemStack.matches(slots.get(i),now.slots.get(i)),why+" native slot "+i);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final MatchCoreCheckpoint originalCore;
        final ChefChecks.Fixture base;
        final MatchBlockEvents originalEvents=new MatchBlockEvents();
        final UUID id=GameChecks.offlineUuid("TestGamma");
        final boolean frozen;
        MatchActorCheckpoint originalActors,actors;
        MatchCoreCheckpoint core;
        List<MatchPlayerCheckpoint> players;
        MatchTerrainCheckpoint terrain;
        MatchTerrainCheckpoint.Restore restore;
        ServerPlayer gamma,retired;
        ElementalGrimoireChecks.Recording wire;
        BlockPos site;
        State gammaState,alphaState,afterPickup;
        ItemStack map;
        int phase=-1,round,ticks;
        boolean owned,closed;
        Fixture(GameController game){
            this.game=game;frozen=game.server().tickRateManager().isFrozen();
            require(!frozen&&game.playing()&&!game.maps().busy()&&game.server().getPlayerList().getPlayer(id)==null,"exclusive actual match");
            originalCore=new MatchCoreCheckpoint(game);base=new ChefChecks.Fixture(game);
        }
        void init(){
            originalActors=MatchActorCheckpoint.captureRegistered(game);
            for(int x=base.bounds.minX()>>4;x<=base.bounds.maxX()>>4;x++)for(int z=base.bounds.minZ()>>4;z<=base.bounds.maxZ()>>4;z++)originalEvents.include(base.owner.level(),new net.minecraft.world.level.ChunkPos(x,z));
            base.init();gamma=GameChecks.createBot(game,"TestGamma");owned=true;
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
            SkyWars2.LOGGER.info("[check] START vanity-workstations");
        }
        void connect(){
            wire=new ElementalGrimoireChecks.Recording(gamma);gamma.connection=new ServerGamePacketListenerImpl(game.server(),wire,gamma,new CommonListenerCookie(gamma.getGameProfile(),0,gamma.clientInformation(),false));
            gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        List<ServerPlayer> live(){return List.of(gamma,base.owner,base.other);}
        AbstractContainerMenu create(int id,net.minecraft.world.entity.player.Inventory inv){
            var a=ContainerLevelAccess.create(base.owner.level(),site);
            return switch(round){case 0->new CraftingMenu(id,inv,a);case 1->new AnvilMenu(id,inv,a);case 2->new SmithingMenu(id,inv,a);case 3->new GrindstoneMenu(id,inv,a);
                case 4->new StonecutterMenu(id,inv,a);case 5->new CartographyTableMenu(id,inv,a);case 6->new LoomMenu(id,inv,a);case 7->new EnchantmentMenu(id,inv,a);default->throw new IllegalStateException();};
        }
        void input(ServerPlayer p,int count){
            p.openMenu(new SimpleMenuProvider((id,inv,player)->create(id,inv),Component.literal("Vanity workspace "+round)));
            var m=p.containerMenu;
            switch(round){
                case 0->m.getSlot(1).set(new ItemStack(Items.OAK_LOG,count));
                case 1->{var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(600);m.getSlot(0).set(sword);m.getSlot(1).set(new ItemStack(Items.DIAMOND,count));((AnvilMenu)m).setItemName("Checkpoint sword "+count);}
                case 2->{m.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,count));m.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));m.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT,count));}
                case 3->{var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(900);m.getSlot(0).set(sword);var second=sword.copy();second.setDamageValue(800);m.getSlot(1).set(second);}
                case 4->{m.getSlot(0).set(new ItemStack(Items.STONE,count));require(m.clickMenuButton(p,1),"native stonecutting selection");}
                case 5->{m.getSlot(0).set(map.copy());m.getSlot(1).set(new ItemStack(Items.MAP,count));}
                case 6->{m.getSlot(0).set(new ItemStack(Items.BANNER.pick(net.minecraft.world.item.DyeColor.WHITE),count));m.getSlot(1).set(new ItemStack(Items.DYE.pick(net.minecraft.world.item.DyeColor.RED),count));require(m.clickMenuButton(p,3),"native banner pattern selection");}
                case 7->{m.getSlot(0).set(new ItemStack(Items.DIAMOND_CHESTPLATE));m.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,count));require(((EnchantmentMenu)m).costs[0]>0,"native enchanting offers");}
            }
            var cursor=new ItemStack(Items.GOLD_INGOT,count+11);cursor.set(DataComponents.CUSTOM_NAME,Component.literal("Workspace cursor "+count));m.setCarried(cursor);
            if(round<7)require(!m.getSlot(result()).getItem().isEmpty(),"real native result exists for "+round);
            require(m.stillValid(p),"native workstation site is valid");
        }
        int result(){return new int[]{0,2,3,2,1,2,3,-1}[round];}
        void setup(){
            game.server().tickRateManager().setFrozen(true);connect();site=base.center.offset(0,0,-2);
            require(game.allowDamage(gamma)&&game.lives(id)==1,"real enrolled one-life participant");
            map=MapItem.create(base.owner.level(),base.center.getX(),base.center.getZ(),(byte)0,false,false);prepare();
        }
        void prepare(){
            base.block(site,new net.minecraft.world.level.block.Block[]{Blocks.CRAFTING_TABLE,Blocks.ANVIL,Blocks.SMITHING_TABLE,Blocks.GRINDSTONE,Blocks.STONECUTTER,Blocks.CARTOGRAPHY_TABLE,Blocks.LOOM,Blocks.ENCHANTING_TABLE}[round]);
            for(var p:live()){
                MatchPlayerViews.closeFutureStorage(p);base.move(p,base.at.add(0,1,0));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);
                p.experienceLevel=100;p.totalExperience=10000;p.experienceProgress=.25f;
            }
            gamma.inventoryMenu.getCraftSlots().setItem(0,new ItemStack(Items.APPLE,2));gamma.inventoryMenu.setCarried(new ItemStack(Items.COPPER_INGOT,3));
            input(gamma,5);input(base.owner,7);
            gammaState=State.of(gamma);alphaState=State.of(base.owner);
            actors=MatchActorCheckpoint.captureRegistered(game);players=live().stream().map(p->new MatchPlayerCheckpoint(game,p)).toList();core=new MatchCoreCheckpoint(game);
            terrain=new MatchTerrainCheckpoint(game.server());terrain.include(base.owner.level().getChunkAt(site));phase=0;
        }
        void mutate(){
            for(var p:List.of(gamma,base.owner)){
                for(var s:p.containerMenu.slots)if(s.container!=p.getInventory())s.set(new ItemStack(Items.ROTTEN_FLESH,63));
                p.containerMenu.setCarried(new ItemStack(Items.NETHERITE_INGOT,31));
            }
            actors.prepareViewsFrozen();
            for(var p:live())require(p.getInventory().isEmpty(),"discard future native workspace inputs instead of returning/dropping them");
            base.block(site,Blocks.STONE);gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;
            require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying(),"actual native player death");
            restore=terrain.restore(game);phase=1;
        }
        void restoreBodies(boolean offline){
            retired=gamma;core.restoreFrozen();actors.restoreBodiesFrozen();
            for(var p:players)if(game.server().getPlayerList().getPlayer(p.id())!=null)p.restoreFrozen();
            if(!offline){gamma=game.server().getPlayerList().getPlayer(id);gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());}
            require(actors.restoreLinksFrozen().equals(offline?Set.of(id):Set.of()),"workspace owner UUID resolution");
        }
        void verify(){
            gammaState.verify(gamma,"saved Gamma "+round);alphaState.verify(base.owner,"independent Alpha "+round);
            for(var p:List.of(gamma,base.owner))require(p.containerMenu.slots.stream().filter(s->s.container==p.getInventory()).count()==36
                    &&(retired==gamma||p.containerMenu.slots.stream().noneMatch(s->s.container==retired.getInventory())),"workspace binds current body inventory");
            require(gamma.inventoryMenu.getCraftSlots().getItem(0).getCount()==2&&gamma.inventoryMenu.getCarried().is(Items.COPPER_INGOT),"personal buffers stay independent");
            var before=gamma.containerMenu;wire.packets.clear();require(actors.restoreLinksFrozen().isEmpty()&&gamma.containerMenu==before,"idempotent workspace retains native window");
            require(wire.packets.stream().noneMatch(ClientboundOpenScreenPacket.class::isInstance),"no extra screen reopen on valid workspace");
            gammaState.verify(gamma,"idempotent saved Gamma "+round);
        }
        void pickup(){
            if(round==7)require(gamma.containerMenu.clickMenuButton(gamma,0)&&!gamma.containerMenu.getSlot(0).getItem().getEnchantments().isEmpty(),"native restored enchanting succeeds");
            else {
                var m=gamma.containerMenu;var expected=m.getSlot(result()).getItem().copy();
                require(m.getSlot(result()).mayPickup(gamma)&&!m.quickMoveStack(gamma,result()).isEmpty(),"native restored result pickup succeeds "+round);
                require(gamma.getInventory().contains(s->s.is(expected.getItem())),"native output reaches current player inventory "+round);
            }
            alphaState.verify(base.owner,"other workspace remains unchanged after pickup "+round);
        }
        boolean step(){
            require(++ticks<480,"bounded phase="+phase+" round="+round);
            if(phase==-1){setup();return false;}
            if(phase==0){mutate();return false;}
            if(phase==1||phase==2||phase==3||phase==4){
                if(!restore.step(32768))return false;restore.close();restore=null;int previous=phase;
                if(previous==3){restoreBodies(true);alphaState.verify(base.owner,"offline player's independent peer workspace");
                    gamma=GameChecks.createBot(game,"TestGamma");connect();actors.prepareViewsFrozen();restore=terrain.restore(game);phase=4;return false;}
                restoreBodies(false);verify();
                if(previous==1){require(gamma!=retired,"genuine native replacement player");pickup();afterPickup=State.of(gamma);actors.prepareViewsFrozen();restore=terrain.restore(game);phase=2;return false;}
                if(previous==2){pickup();afterPickup.verify(gamma,"repeat native pickup has identical inventory/resources/XP "+round,true);
                    if(round==1){gamma.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity workspace offline fixture")));actors.prepareViewsFrozen();restore=terrain.restore(game);phase=3;return false;}}
                if(++round==8)return true;prepare();return false;
            }
            return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);
            try{if(restore!=null)restore.close();for(var p:List.of(base.owner,base.other))MatchPlayerViews.closeFutureStorage(p);
                var p=game.server().getPlayerList().getPlayer(id);if(owned&&p!=null){MatchPlayerViews.closeFutureStorage(p);p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Vanity workspace cleanup")));}
                if(originalActors!=null){originalActors.restoreBodiesFrozen();originalActors.restoreLinksFrozen();}
                base.close();originalEvents.restore();originalCore.restoreFrozen();
            }finally{game.server().tickRateManager().setFrozen(frozen);}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-workstations (eight actual native workspaces; independent local inputs/results/recipe/data/name/cursor; callback-free replay; real native result pickup/enchanting and repeated resource/XP equivalence; death/replacement and offline reconnect; personal buffer isolation and current player bindings; full coordinator remains gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-workstations",error);}}
}
