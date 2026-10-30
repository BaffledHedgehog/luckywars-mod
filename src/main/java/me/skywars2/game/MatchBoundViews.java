package me.skywars2.game;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import me.skywars2.mixin.BoundViewCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

/** Native special-window links; block/actor contents and offers remain owned by terrain/body images. */
public final class MatchBoundViews {
    public record OfferView(int level,boolean progress,boolean restock) { }
    public interface Publication { OfferView skywars2$offers(int id); }
    private static final Set<Class<?>> KINDS=Set.of(BeaconMenu.class,LecternMenu.class,MerchantMenu.class,HorseInventoryMenu.class,NautilusInventoryMenu.class);
    static boolean supports(AbstractContainerMenu menu){return KINDS.contains(menu.getClass());}
    private final Class<?> kind,actorKind;
    private final ServerLevel level;
    private final BlockPos site;
    private final UUID actor;
    private final int columns,size,offerCount,active,selection,futureXp,removed;
    private final Component title;
    private final ItemStack cursor;
    private final List<ItemStack> local;
    private final OfferView nativeMeta,published;

    MatchBoundViews(ServerPlayer player){
        validate(player,true);var menu=player.containerMenu;kind=menu.getClass();cursor=menu.getCarried().copy();
        var entity=backingActor(menu);actor=entity==null?null:entity.getUUID();actorKind=entity==null?null:entity.getClass();
        var block=backingBlock(menu);level=entity!=null?(ServerLevel)entity.level():(ServerLevel)block.getLevel();site=block==null?null:block.getBlockPos().immutable();
        title=isMount()?null:((MatchPlayerViews.Title)player).skywars2$menuTitle(menu.containerId);
        if(menu instanceof MerchantMenu trade){
            var container=((BoundViewCheckpointAccess.Trade)trade).skywars2$container();var a=(BoundViewCheckpointAccess.TradeBuffer)container;
            var offers=((BoundViewCheckpointAccess.Villager)entity).skywars2$offers();offerCount=offers.size();active=a.skywars2$active()==null?-1:offers.indexOf(a.skywars2$active());
            if(a.skywars2$active()!=null&&active<0)throw new IllegalStateException("Merchant active offer must belong to its native offers");
            local=a.skywars2$items().stream().map(ItemStack::copy).toList();selection=a.skywars2$selection();futureXp=a.skywars2$xp();
            removed=((BoundViewCheckpointAccess.TradeResult)menu.getSlot(2)).skywars2$removed();
            nativeMeta=new OfferView(trade.getTraderLevel(),trade.showProgressBar(),trade.canRestock());published=((Publication)player).skywars2$offers(menu.containerId);
        }else{offerCount=active=selection=futureXp=removed=0;nativeMeta=published=null;
            local=menu instanceof BeaconMenu?List.of(((BoundViewCheckpointAccess.Beacon)menu).skywars2$payment().getItem(0).copy()):List.of();}
        columns=entity instanceof AbstractHorse h?h.getInventoryColumns():entity instanceof AbstractNautilus n?n.getInventoryColumns():0;
        size=isMount()?mountInventory((LivingEntity)entity).getContainerSize():0;
    }
    boolean isMount(){return kind==HorseInventoryMenu.class||kind==NautilusInventoryMenu.class;}
    static Container mountInventory(LivingEntity entity){
        if(entity instanceof AbstractHorse)return ((BoundViewCheckpointAccess.Horse)entity).skywars2$inventory();
        if(entity instanceof AbstractNautilus)return ((BoundViewCheckpointAccess.Nautilus)entity).skywars2$inventory();
        throw new IllegalStateException("Unsupported native inventory mount");
    }
    private static Entity backingActor(AbstractContainerMenu menu){
        if(menu instanceof MerchantMenu){var merchant=((BoundViewCheckpointAccess.Trade)menu).skywars2$trader();
            if(!(merchant instanceof AbstractVillager))throw new IllegalStateException("Nonentity merchant needs its own checkpoint adapter");return (Entity)merchant;}
        if(menu instanceof AbstractMountInventoryMenu)return ((BoundViewCheckpointAccess.Mount)menu).skywars2$mount();
        return null;
    }
    private static BlockEntity backingBlock(AbstractContainerMenu menu){
        if(menu instanceof BeaconMenu){
            return ((BoundViewCheckpointAccess.Beacon)menu).skywars2$access().evaluate((world,pos)->{
                if(!(world instanceof ServerLevel)||!world.hasChunkAt(pos)||!(world.getBlockEntity(pos) instanceof BeaconBlockEntity b))throw new IllegalStateException("Native beacon window requires its current block");return b;
            }).orElseThrow(()->new IllegalStateException("Detached beacon needs its own checkpoint adapter"));
        }
        if(menu instanceof LecternMenu){
            var container=((BoundViewCheckpointAccess.Lectern)menu).skywars2$book();
            if(!(container instanceof BoundViewCheckpointAccess.LecternBook book))throw new IllegalStateException("Native lectern window requires its book owner");return book.skywars2$owner();
        }
        return null;
    }
    static void validate(ServerPlayer player,boolean current){
        var menu=player.containerMenu;if(!supports(menu))throw new IllegalStateException("Unsupported bound window");
        int count=menu instanceof LecternMenu?0:36;
        if(menu.slots.stream().filter(s->s.container==player.getInventory()).count()!=count
                ||menu.slots.stream().anyMatch(s->s.container instanceof net.minecraft.world.entity.player.Inventory&&s.container!=player.getInventory()))throw new IllegalStateException("Bound window has foreign player slots");
        if(!current)return;
        var block=backingBlock(menu);var entity=backingActor(menu);
        if(block!=null&&(block.isRemoved()||!(block.getLevel() instanceof ServerLevel world)||world.getBlockEntity(block.getBlockPos())!=block))throw new IllegalStateException("Bound block window is retired");
        if(menu instanceof BeaconMenu&&((BoundViewCheckpointAccess.Beacon)menu).skywars2$data()!=((BoundViewCheckpointAccess.BeaconBlock)block).skywars2$data())throw new IllegalStateException("Beacon must use current block data");
        if(menu instanceof LecternMenu&&(((BoundViewCheckpointAccess.Lectern)menu).skywars2$book()!=((BoundViewCheckpointAccess.LecternBlock)block).skywars2$book()
                ||((BoundViewCheckpointAccess.Lectern)menu).skywars2$data()!=((BoundViewCheckpointAccess.LecternBlock)block).skywars2$data()))throw new IllegalStateException("Lectern must use current book/data objects");
        if(entity!=null&&(entity.isRemoved()||!(entity.level() instanceof ServerLevel world)||world.getEntity(entity.getUUID())!=entity))throw new IllegalStateException("Bound actor window is retired");
        if(menu instanceof MerchantMenu&&((AbstractVillager)entity).getTradingPlayer()!=player)throw new IllegalStateException("Trade window belongs to a different native customer");
        if(menu instanceof MerchantMenu&&((BoundViewCheckpointAccess.Villager)entity).skywars2$offers()==null)throw new IllegalStateException("Do not generate random merchant offers during capture");
        if(menu instanceof AbstractMountInventoryMenu&&((BoundViewCheckpointAccess.Mount)menu).skywars2$container()!=mountInventory((LivingEntity)entity))throw new IllegalStateException("Mount window must use current native inventory");
    }
    private Entity entity(Function<UUID,Entity> resolve){
        if(actor==null)return null;var e=resolve.apply(actor);
        if(e==null||e.isRemoved()||e.level()!=level||e.getClass()!=actorKind)throw new IllegalStateException("Restore native bound actor before window: "+actor);
        if(isMount()&&(mountInventory((LivingEntity)e).getContainerSize()!=size||(e instanceof AbstractHorse h?h.getInventoryColumns():((AbstractNautilus)e).getInventoryColumns())!=columns))throw new IllegalStateException("Restore native mount inventory shape before window");
        if(kind==MerchantMenu.class){var offers=((BoundViewCheckpointAccess.Villager)e).skywars2$offers();if(offers==null||offers.size()!=offerCount)throw new IllegalStateException("Restore captured native merchant offers before window");}
        return e;
    }
    private BlockEntity block(){
        if(site==null)return null;var block=level.hasChunkAt(site)?level.getBlockEntity(site):null;
        if(block==null||block.isRemoved()||(kind==BeaconMenu.class?block.getClass()!=BeaconBlockEntity.class:block.getClass()!=LecternBlockEntity.class))throw new IllegalStateException("Restore bound block before window: "+site);
        return block;
    }
    void preflight(Function<UUID,Entity> resolve){entity(resolve);block();}
    private boolean matches(ServerPlayer player,Entity actor,BlockEntity block){
        if(player.containerMenu.getClass()!=kind)return false;
        try{validate(player,true);return backingActor(player.containerMenu)==actor&&backingBlock(player.containerMenu)==block
                &&(isMount()||title.equals(((MatchPlayerViews.Title)player).skywars2$menuTitle(player.containerMenu.containerId)));}catch(IllegalStateException bad){return false;}
    }
    /** False means native mount pairing is pending; no mount-ID screen packet may precede pairing. */
    boolean restore(ServerPlayer player,Function<UUID,Entity> resolve,Set<UUID> pending){
        var e=entity(resolve);var block=block();boolean matches=matches(player,e,block);
        if(!matches){
            MatchPlayerViews.closeFutureStorage(player);
            if(isMount()){
                if(!MatchPlayerViews.tracked(player,e)){pending.add(actor);return false;}
                if(e instanceof AbstractHorse h)player.openHorseInventory(h,mountInventory(h));else player.openNautilusInventory((AbstractNautilus)e,mountInventory((LivingEntity)e));
            }else{
                if(e instanceof AbstractVillager v)v.setTradingPlayer(player);
                var opened=player.openMenu(new SimpleMenuProvider((id,inv,p)->{
                    if(e instanceof AbstractVillager v)return new MerchantMenu(id,inv,v);
                    if(block instanceof BeaconBlockEntity b)return new BeaconMenu(id,inv,((BoundViewCheckpointAccess.BeaconBlock)b).skywars2$data(),ContainerLevelAccess.create(level,site));
                    var b=(BoundViewCheckpointAccess.LecternBlock)block;return new LecternMenu(id,b.skywars2$book(),b.skywars2$data());
                },MatchPlayerViews.copyTitle(player,title)));
                if(opened.isEmpty())throw new IllegalStateException("Native bound window reopening failed");
            }
        }
        var menu=player.containerMenu;menu.setCarried(cursor.copy());
        if(menu instanceof BeaconMenu){((SimpleContainer)((BoundViewCheckpointAccess.Beacon)menu).skywars2$payment()).getItems().set(0,local.getFirst().copy());}
        if(menu instanceof MerchantMenu trade){
            var buffer=(BoundViewCheckpointAccess.TradeBuffer)((BoundViewCheckpointAccess.Trade)menu).skywars2$container();
            for(int i=0;i<3;i++)buffer.skywars2$items().set(i,local.get(i).copy());
            buffer.skywars2$selection(selection);buffer.skywars2$xp(futureXp);buffer.skywars2$active(active<0?null:((BoundViewCheckpointAccess.Villager)e).skywars2$offers().get(active));
            ((BoundViewCheckpointAccess.TradeResult)menu.getSlot(2)).skywars2$removed(removed);
            trade.setMerchantLevel(nativeMeta.level);trade.setShowProgressBar(nativeMeta.progress);trade.setCanRestock(nativeMeta.restock);
            var v=(AbstractVillager)e;v.setTradingPlayer(player);
            player.sendMerchantOffers(menu.containerId,((BoundViewCheckpointAccess.Villager)v).skywars2$offers(),published.level,v.getVillagerXp(),published.progress,published.restock);
        }
        return true;
    }
    static void discard(AbstractContainerMenu menu){
        if(menu instanceof BeaconMenu)((SimpleContainer)((BoundViewCheckpointAccess.Beacon)menu).skywars2$payment()).getItems().set(0,ItemStack.EMPTY);
        if(menu instanceof MerchantMenu){var a=(BoundViewCheckpointAccess.TradeBuffer)((BoundViewCheckpointAccess.Trade)menu).skywars2$container();
            for(int i=0;i<3;i++)a.skywars2$items().set(i,ItemStack.EMPTY);a.skywars2$active(null);a.skywars2$xp(0);}
    }
}
