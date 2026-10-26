package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Books for wand families without an authored library entry. All payloads are shared. */
final class LbcExtraGrimoires {
    static final int FIRST=243, LAST=279, COUNT=28;
    static boolean retired(int id){return id>=262&&id<=270;}
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
            case 271,272,273,274,275,276,277 -> game.items().gaycasterAcademy().cast(owner,id-270);
            case 278 -> game.items().advancedSnow().grimoire(owner,hand,true,false,1);
            case 279 -> game.items().advancedSnow().grimoire(owner,hand,true,true,1);
            case 262,263,264,265,266,267,268,269,270 -> {return false;}
            default -> throw new IllegalArgumentException("Unknown extra grimoire "+id);
        }
        return true;
    }
}
