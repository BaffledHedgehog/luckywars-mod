package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.network.HashedStack;
import net.minecraft.world.phys.*;

/** Staged checks inside an actual mode3 match; normal server ticks advance every owner. */
public final class BedwarsAuxiliaryChecks {
    private BedwarsAuxiliaryChecks() { }
    private static Run running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("BedWars auxiliaries: "+why);}
    public static void start(GameController game){
        Run r=null;
        try{require(running==null,"exclusive fixture");r=new Run(game);r.init();running=r;SkyWars2.LOGGER.info("[check] START bedwars-auxiliary");}
        catch(RuntimeException|Error error){if(r!=null)try{r.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL bedwars-auxiliary",error);}
    }
    public static void tick(GameController game){
        if(running==null)return;var r=running;
        try{if(r.step()){running=null;r.close();SkyWars2.LOGGER.info("[check] PASS bedwars-auxiliary (actual mode3/native five cadences/duplicates/overflow, shared54-slot native packets, zombie/team/owner, TimeStop/nonempty whole-coordinator replay)");}}
        catch(RuntimeException|Error error){running=null;try{r.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL bedwars-auxiliary",error);}
    }
    private static final class Run implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture f;final LbcBedwarsAuxiliaries aux;
        final MatchSpellCheckpoint.Links oldLinks;final LbcBedwarsAuxiliaries.Checkpoint original;final LbcBedwarsAuxiliaries.StorageImage oldStorage;
        final List<Entity> markers=new ArrayList<>();final List<LbcBedwarsAuxiliaries.Generator> gens=new ArrayList<>();
        RegenerationBeltChecks.ScheduledClient client,audience,teammateClient;net.minecraft.server.level.ServerPlayer teammate,enemy;int stage,when,sequence,team,beacon;boolean closed;
        BlockPos chest;Zombie zombie;LbcBedwarsAuxiliaries.SharedContainer root;int queue;UUID zombieId;
        Run(GameController game){
            this.game=game;require(game.config().gameMode==3&&game.playing(),"run in genuine admitted mode3 match");aux=game.bedwars().auxiliaries();
            oldLinks=new MatchSpellCheckpoint.Links(new MatchDamageSources(game,List.of()));original=aux.checkpoint(oldLinks);oldStorage=aux.storageImage();
            require(original.bodies.isEmpty()&&original.queued.isEmpty()&&original.buffs.isEmpty()&&aux.containers().isEmpty(),"fresh auxiliary scene with no unrelated owned bodies/storage/rewards");f=new ChefChecks.Fixture(game);
        }
        void init(){
            f.init();client=new RegenerationBeltChecks.ScheduledClient(game,f.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,f.other);team=aux.team(f.owner);require(team>=1&&team<=16,"actual source team membership");
            teammate=game.server().getPlayerList().getPlayers().stream().filter(p->p!=f.owner&&aux.team(p)==team&&!game.isObserver(p)&&game.canUseGameplayItems(p)).findFirst().orElseThrow(()->new IllegalStateException("Actual mode3 scene needs two teammates and two enemy teammates"));
            enemy=game.server().getPlayerList().getPlayers().stream().filter(p->aux.team(p)!=team&&aux.team(p)>0&&!game.isObserver(p)&&game.canUseGameplayItems(p)).findFirst().orElseThrow();
            require(teammate.containerMenu==teammate.inventoryMenu&&teammate.inventoryMenu.getCarried().isEmpty(),"teammate native window initially closed");if(teammate!=f.other)teammateClient=new RegenerationBeltChecks.ScheduledClient(game,teammate);
            for(var kind:LbcBedwarsAuxiliaries.Kind.values())marker(kind,f.at.add(-10+kind.ordinal()*5,0,8));
            marker(LbcBedwarsAuxiliaries.Kind.GOLD,gens.get(2).at());aux.initialize(gens);
            f.move(f.owner,f.at.add(-12,0,-12));f.move(f.other,f.at.add(12,0,-12));client.acknowledge();audience.acknowledge();when=game.server().getTickCount();
        }
        void marker(LbcBedwarsAuxiliaries.Kind kind,Vec3 at){var m=new Marker(EntityTypes.MARKER,f.owner.level());m.setPos(at);m.entityTags().add(switch(kind){case COPPER->"copper_generator";case IRON->"iron_generator";case GOLD->"gold_generator";case DIAMOND->"diamond_generator";case STAR->"star_generator";});m.entityTags().add("strict_map_object");require(f.owner.level().addFreshEntity(m),"actual indexed source generator marker");markers.add(m);gens.add(new LbcBedwarsAuxiliaries.Generator(m.getUUID(),f.owner.level(),at,kind));}
        List<Entity> resident(){var result=new ArrayList<Entity>();f.owner.level().getAllEntities().forEach(result::add);return result;}
        long physical(Item type){long count=0;for(var e:f.owner.level().getAllEntities())if(e instanceof ItemEntity item&&!item.isRemoved()&&!f.before.contains(e)&&item.getItem().is(type)&&e.position().distanceToSqr(f.at)<40*40)count+=item.getItem().getCount();return count;}
        void blockPacket(BlockPos pos,Direction face){client.acknowledge();var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(.5)),face,pos,false);f.owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,hit,++sequence));f.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);}
        void begin(int next){stage=next;when=game.server().getTickCount();}
        boolean step(){
            client.acknowledge();audience.acknowledge();if(teammateClient!=null)teammateClient.acknowledge();int elapsed=game.server().getTickCount()-when;require(elapsed<900,"bounded native stage"+stage);
            if(game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement a)a.skywars2$acknowledgeRestore();
            if(stage==0){
                if(elapsed<301)return false;
                require(physical(Items.COPPER_INGOT)==30&&physical(Items.IRON_INGOT)==3&&physical(Items.GOLD_INGOT)==2&&physical(Items.DIAMOND)==1&&physical(Items.NETHER_STAR)==1,"all five exact source cadences and duplicate gold emits twice then retires one");
                require(aux.generators().size()==5,"no-player-only duplicate pruning after real output");
                for(var e:resident())if(e instanceof ItemEntity item&&!f.before.contains(e)&&item.getItem().is(Items.COPPER_INGOT))e.discard();
                f.move(f.owner,gens.getFirst().at());f.move(f.other,gens.getFirst().at().add(1.4,0,0));client.acknowledge();audience.acknowledge();f.owner.getInventory().clearContent();f.other.getInventory().clearContent();for(int i=0;i<36;i++)f.owner.getInventory().setItem(i,new ItemStack(Items.STONE,64));begin(1);return false;
            }
            if(stage==1){
                if(elapsed<11)return false;long inventory=0;for(int i=0;i<f.other.getInventory().getContainerSize();i++)if(f.other.getInventory().getItem(i).is(Items.COPPER_INGOT))inventory+=f.other.getInventory().getItem(i).getCount();require(inventory>=1&&inventory+physical(Items.COPPER_INGOT)==2,"one physical currency for EACH nearby player, full-inventory overflow retained without loss");
                aux.beacon(f.owner,LbcBedwarsShop.Beacon.JUMP2,300);beacon=aux.beaconSeconds(team,LbcBedwarsShop.Beacon.JUMP2);game.items().timeStop().begin(f.owner.level(),1000);queue=aux.pendingOutputs();begin(2);return false;
            }
            if(stage==2){
                require(aux.beaconSeconds(team,LbcBedwarsShop.Beacon.JUMP2)==beacon&&aux.pendingOutputs()==queue,"native TimeStop retains generator work/beacon clock");if(elapsed<25)return false;game.items().timeStop().end(f.owner.level());
                f.owner.getInventory().clearContent();f.move(f.owner,f.at.add(.5,0,-2));client.acknowledge();chest=f.center;f.owner.setItemInHand(InteractionHand.MAIN_HAND,aux.teamChest(f.owner).copyWithCount(2));blockPacket(chest.below(),Direction.UP);begin(3);return false;
            }
            if(stage==3){
                if(elapsed<2)return false;require(aux.chest(f.owner.level(),chest)&&f.owner.getMainHandItem().getCount()==1,"actual native placement records semantic chest and debits used hand once");blockPacket(chest,Direction.NORTH);require(f.owner.containerMenu instanceof ChestMenu menu&&menu.getContainer()==aux.container(team),"native UseBlock opens canonical54-slot root");
                root=aux.container(team);aux.open(teammate,InteractionHand.MAIN_HAND,f.owner.level(),chest);require(teammate.containerMenu instanceof ChestMenu shared&&shared.getContainer()==root,"two actual same-team native windows share one54slot root");
                var denied=enemy.containerMenu;require(aux.open(enemy,InteractionHand.MAIN_HAND,f.owner.level(),chest)&&enemy.containerMenu==denied,"enemy interaction denied without opening personal ender inventory");
                root.setItem(7,new ItemStack(Items.DIAMOND,17));var menu=f.owner.containerMenu;f.owner.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),(short)7,(byte)0,ContainerInput.PICKUP,new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(),HashedStack.EMPTY));
                require(root.getItem(7).isEmpty()&&menu.getCarried().is(Items.DIAMOND)&&menu.getCarried().getCount()==17,"native inventory packet moves shared root to actual cursor once");menu.setCarried(ItemStack.EMPTY);root.setItem(7,new ItemStack(Items.DIAMOND,17));f.owner.closeContainer();
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,aux.teamZombie(f.owner).copyWithCount(2));f.owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,++sequence,0,0));f.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);begin(4);return false;
            }
            if(stage==4){
                if(elapsed<2)return false;for(var e:f.owner.level().getAllEntities())if(e instanceof Zombie z&&aux.owner(z)==f.owner){zombie=z;break;}
                require(zombie!=null&&zombie.getTeam()==f.owner.getTeam()&&f.owner.getMainHandItem().getCount()==1,"real native egg input emits one same-team causal zombie and debits one");
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND))require(zombie.getItemBySlot(slot).has(net.minecraft.core.component.DataComponents.UNBREAKABLE),"all five native source equipment stacks unbreakable");
                require(!aux.canAttack(zombie,f.owner)&&!aux.canAttack(zombie,teammate),"zombie rejects its owner through shared ally target policy");zombieId=zombie.getUUID();aux.open(f.owner,InteractionHand.MAIN_HAND,f.owner.level(),chest);require(f.owner.containerMenu instanceof ChestMenu,"real chest window remains open during capture");game.items().timeStop().begin(f.owner.level(),1000);require(game.vanity().requestCapture(f.owner,false),"nonempty actual whole-coordinator capture");begin(5);return false;
            }
            if(stage==5){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;root.setItem(7,new ItemStack(Items.ROTTEN_FLESH,2));f.owner.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT,9));aux.beacon(f.owner,LbcBedwarsShop.Beacon.JUMP2,120);require(game.vanity().requestRestoreForCheck(),"actual whole-coordinator restore");begin(6);return false;}
            if(stage==6){if(game.vanity().busy())return false;require(aux.container(team)==root&&root.getItem(7).is(Items.DIAMOND)&&root.getItem(7).getCount()==17&&f.owner.containerMenu instanceof ChestMenu menu&&menu.getContainer()==root&&menu.getCarried().isEmpty(),"same native root, item values, open window and cursor restored through canonical owners");require(f.owner.level().getEntity(zombieId) instanceof Zombie z&&aux.owner(z)==f.owner,"restored real zombie resolves causal native body");require(teammate.containerMenu instanceof ChestMenu shared&&shared.getContainer()==root,"second restored viewer remains bound to identical canonical root");
                game.vanity().stop();game.items().timeStop().end(f.owner.level());
                // Destroy one physical site without producing or clearing shared contents.
                var beforeBreak=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());f.owner.level().getAllEntities().forEach(beforeBreak::add);
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.NETHERITE_PICKAXE));
                require(f.owner.gameMode.destroyBlock(chest),"native player team chest material removed with correct tool");
                int marked=0;for(var e:f.owner.level().getAllEntities())if(!beforeBreak.contains(e)&&e instanceof ItemEntity item){require(LbcBedwarsAuxiliaries.chestItem(item.getItem()),"owned player break suppresses vanilla obsidian/ordinary ender chest drops");marked+=item.getItem().getCount();}
                require(marked==1,"actual player mutation publishes exactly one marked chest immediately");
                require(!aux.chest(f.owner.level(),chest)&&root.getItem(7).getCount()==17,"destroyed association never drops or clears shared inventory");return true;}
            return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->game.vanity().stop(),()->game.items().timeStop().end(f.owner.level()),aux::stop,
                    ()->{for(var m:markers)m.discard();for(var e:resident())if(!f.before.contains(e)&&(e instanceof ItemEntity||e instanceof Zombie||e instanceof Display.BlockDisplay)&&e.position().distanceToSqr(f.at)<40*40)e.discard();},
                    ()->{original.prepare(oldLinks).run();oldStorage.restoreStorageFrozen();aux.publishFrozen();},
                    ()->{if(client!=null)client.close();},()->{if(audience!=null)audience.close();},()->{if(teammateClient!=null)teammateClient.close();},f::close))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("BedWars auxiliary fixture cleanup",failure);
        }
    }
}
