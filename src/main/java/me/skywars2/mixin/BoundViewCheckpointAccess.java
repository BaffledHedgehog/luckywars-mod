package me.skywars2.mixin;

import me.skywars2.game.MatchBoundViews;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native block/actor backing links. Container contents retain their terrain/actor owner. */
public final class BoundViewCheckpointAccess {
    private BoundViewCheckpointAccess() { }
    @Mixin(BeaconMenu.class) public interface Beacon {
        @Accessor("beacon") Container skywars2$payment();
        @Accessor("access") ContainerLevelAccess skywars2$access();
        @Accessor("beaconData") ContainerData skywars2$data();
    }
    @Mixin(BeaconBlockEntity.class) public interface BeaconBlock {
        @Accessor("dataAccess") ContainerData skywars2$data();
    }
    @Mixin(LecternMenu.class) public interface Lectern {
        @Accessor("lectern") Container skywars2$book();
        @Accessor("lecternData") ContainerData skywars2$data();
    }
    @Mixin(LecternBlockEntity.class) public interface LecternBlock {
        @Accessor("bookAccess") Container skywars2$book();
        @Accessor("dataAccess") ContainerData skywars2$data();
    }
    @Mixin(targets="net.minecraft.world.level.block.entity.LecternBlockEntity$1") public interface LecternBook {
        @Accessor("this$0") LecternBlockEntity skywars2$owner();
    }
    @Mixin(AbstractMountInventoryMenu.class) public interface Mount {
        @Accessor("mount") LivingEntity skywars2$mount();
        @Accessor("mountContainer") Container skywars2$container();
    }
    @Mixin(AbstractHorse.class) public interface Horse {
        @Accessor("inventory") SimpleContainer skywars2$inventory();
        @Invoker("createInventory") void skywars2$resize();
    }
    @Mixin(AbstractNautilus.class) public interface Nautilus {
        @Accessor("inventory") SimpleContainer skywars2$inventory();
    }
    @Mixin(Llama.class) public interface LlamaStrength {
        @Invoker("setStrength") void skywars2$strength(int value);
    }
    @Mixin(MerchantMenu.class) public interface Trade {
        @Accessor("trader") Merchant skywars2$trader();
        @Accessor("tradeContainer") MerchantContainer skywars2$container();
    }
    @Mixin(MerchantContainer.class) public interface TradeBuffer {
        @Accessor("itemStacks") NonNullList<ItemStack> skywars2$items();
        @Accessor("selectionHint") int skywars2$selection();
        @Accessor("selectionHint") void skywars2$selection(int value);
        @Accessor("futureXp") int skywars2$xp();
        @Accessor("futureXp") void skywars2$xp(int value);
        @Accessor("activeOffer") MerchantOffer skywars2$active();
        @Accessor("activeOffer") void skywars2$active(MerchantOffer value);
    }
    @Mixin(AbstractVillager.class) public interface Villager {
        @Accessor("offers") MerchantOffers skywars2$offers();
        @Accessor("offers") void skywars2$offers(MerchantOffers value);
    }
    @Mixin(MerchantResultSlot.class) public interface TradeResult {
        @Accessor("removeCount") int skywars2$removed();
        @Accessor("removeCount") void skywars2$removed(int value);
    }
    @Mixin(ServerPlayer.class) public abstract static class Publication implements MatchBoundViews.Publication {
        @Unique private int skywars2$tradeId=-1;
        @Unique private MatchBoundViews.OfferView skywars2$offerView;
        @Inject(method="sendMerchantOffers",at=@At("HEAD"))
        private void skywars2$remember(int id,MerchantOffers offers,int level,int xp,boolean progress,boolean restock,CallbackInfo ci){
            skywars2$tradeId=id;skywars2$offerView=new MatchBoundViews.OfferView(level,progress,restock);
        }
        @Override public MatchBoundViews.OfferView skywars2$offers(int id){
            if(id!=skywars2$tradeId||skywars2$offerView==null)throw new IllegalStateException("Native merchant screen needs its published offer metadata");
            return skywars2$offerView;
        }
    }
}
