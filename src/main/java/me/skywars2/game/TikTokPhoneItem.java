package me.skywars2.game;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** The real phone/passive remains authoritative; moving pixels exist only in recipient projection. */
final class TikTokPhoneItem extends SimplePolymerItem {
    TikTokPhoneItem(Properties properties){super(properties,Items.CARROT_ON_A_STICK,true);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        var game=SkyWars2.game();var playback=TikTokAccounts.playback();
        if(game==null||playback==null||!(player instanceof ServerPlayer owner)||game.items().actionsBlocked(owner))return InteractionResult.FAIL;
        return playback.use(owner)?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    @Override public ItemStack getPolymerItemStack(ItemStack stack,TooltipFlag flag,PacketContext context,HolderLookup.Provider lookup){
        var result=super.getPolymerItemStack(stack,flag,context,lookup);
        var player=PolymerCommonUtils.getPlayer(context);var playback=TikTokAccounts.playback();
        var screen=player==null||playback==null?null:playback.screen(player);
        if(screen!=null){
            result.set(DataComponents.ITEM_MODEL,screen.enhanced()?TikTokPlayback.CLIENT_MODEL:TikTokPlayback.VIDEO_MODEL);
            if(screen.enhanced())result.remove(DataComponents.CUSTOM_MODEL_DATA);else result.set(DataComponents.CUSTOM_MODEL_DATA,screen.colors());
        }
        return result;
    }
}
