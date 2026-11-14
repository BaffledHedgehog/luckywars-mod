package me.skywars2.game;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** One accepted-volley continuation for native bows/crossbows and automatic bow pulses. */
public final class LbcRangedShots {
    private LbcRangedShots(){}
    public static void accepted(GameController game,ServerPlayer player,InteractionHand hand,ItemStack weapon,
            ItemStack firedWeapon,List<Projectile> spawned){
        var fired=firedWeapon;boolean traitsRolled=false;
        if(me.skywars2.game.LbcTmtRanged.onShot(game,player,hand,weapon,fired,List.copyOf(spawned))){
            return;
        }
        me.skywars2.game.LbcLegacyRanged.installRocketAmmo(game,player,fired,List.copyOf(spawned));
        if(me.skywars2.game.ItemData.intValue(fired,"explosive",0)==1){
            var nativeFamily=List.copyOf(spawned);if(!traitsRolled)game.items().trinkets().shot(player,nativeFamily);traitsRolled=true;
            if(game.items().balance().explosiveBow(player,fired,nativeFamily))return;
        }
        if(me.skywars2.game.LbcChargedBows.matches(fired)){
            var nativeFamily=List.copyOf(spawned);if(!traitsRolled)game.items().trinkets().shot(player,nativeFamily);traitsRolled=true;
            game.items().chargedBows().release(player,fired,player.getTicksUsingItem(),nativeFamily);return;
        }
        var imbaFamily=me.skywars2.game.LbcImbaCrossbows.augment(game,player,fired,List.copyOf(spawned));
        if(imbaFamily.isEmpty())return;
        if(me.skywars2.game.LbcImbaCrossbows.isBeam(fired)){
            game.items().trinkets().shot(player,imbaFamily);
            me.skywars2.game.LbcImbaCrossbows.release(game,player,fired,imbaFamily);return;
        }
        var oldFamily=game.items().balance().augment(player,fired,imbaFamily);
        var family=game.items().shulkerBow().augment(player,fired,oldFamily);
        game.items().madokaBow().augment(player,fired,family);
        if(!traitsRolled)game.items().trinkets().shot(player,family);
        me.skywars2.game.LbcLegacyRanged.convert(game,player,fired,family);
        me.skywars2.game.LbcMinigunCrossbow.afterShot(game,player,hand,weapon,family,fired);
        me.skywars2.game.LbcLegacyRanged.afterShot(game,player,hand,weapon,family,fired);
    }
}
