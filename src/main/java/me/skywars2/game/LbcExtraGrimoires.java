package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Books for wand families without an authored library entry. All payloads are shared. */
final class LbcExtraGrimoires {
    static final int FIRST=243, LAST=263, COUNT=LAST-FIRST+1;
    private LbcExtraGrimoires() { }
    static boolean cast(GameController game,ServerPlayer owner,InteractionHand hand,int id){
        if(id>=243&&id<=251){game.items().wands().extraTiny(owner,id-FIRST);return true;}
        switch(id){
            case 252 -> game.items().soulless().cast(owner);
            case 253 -> {return game.items().soulless().generate(owner);}
            case 254,255,256,257 -> game.items().halloweenWind().cast(owner,id-253);
            case 258 -> {
                game.items().fartus().cast(owner,owner.getRandom().nextInt(2)+1);
                LbcOverload.sound(owner,owner.position(),"minecraft:entity.snowball.throw",1,1,0);
            }
            case 259 -> LbcGaycaster.cast(game,owner);
            case 260 -> {
                game.items().iceWand().spawn(owner,hand,ItemStack.EMPTY);
                LbcOverload.sound(owner,owner.position(),"minecraft:lbcsounds.ice_wand",1,.7f,0);
            }
            case 261 -> {return game.items().reactive().castWall(owner,false);}
            case 262,263 -> {return game.items().summons().grimoire(owner,id==263);}
            default -> throw new IllegalArgumentException("Unknown extra grimoire "+id);
        }
        return true;
    }
}
