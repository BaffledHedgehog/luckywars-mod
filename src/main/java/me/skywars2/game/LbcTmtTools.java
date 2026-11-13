package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Real tool contacts/breaks share the existing action owners and their captured source stacks. */
public final class LbcTmtTools {
    private LbcTmtTools(){}
    public static boolean blockCandidate(ItemStack stack){
        if(LbcTmtTriggers.modern(stack))return true;
        if(!LbcTmtItems.matches(stack))return LbcSteampunkBreaker.matches(stack)||LbcImbaAxe.recognized(stack)
                ||LbcImbaShovel.matches(stack)||LbcImbaHoe.matches(stack)||LbcImbaPickaxe.matches(stack);
        for(var property:LbcTmtTriggers.effective(stack)){
            var data=(ItemData.IntReader)(Object)property.data();
            if(data.skywars2$readInt("chaos_sho",0)==1||data.skywars2$readInt("chaos_pic",0)==1
                    ||data.skywars2$readInt("chaos_hoe",0)==1||data.skywars2$readInt("steampunk_pickaxe",0)==1
                    ||data.skywars2$readString(LbcTmtAbilities.NATIVE,"").equals("skywars2:imba_axe"))return true;
        }
        return false;
    }
    public static void contact(GameController game,ServerPlayer owner){
        var tool=owner.getMainHandItem();
        if(!LbcTmtItems.matches(tool)){contactOwners(game,owner,tool,LbcImbaAxe.Trigger.REAL_MELEE);return;}
        for(var property:LbcTmtTriggers.effective(tool)){
            if(tool.isEmpty()||owner.getMainHandItem()!=tool)break;
            LbcTmtAbilities.invoke(tool,property.id(),()->{
                contactOwners(game,owner,tool,LbcImbaAxe.Trigger.REAL_MELEE);return true;
            });
        }
    }
    /** Called only after the real destroyBlock succeeds; source was captured before final durability debit. */
    public static void broken(GameController game,ServerPlayer owner,ItemStack source,BlockPos pos,BlockState before){
        LbcTmtTriggers.captured(owner,LbcTmtTriggers.Input.MINING,source,net.minecraft.world.InteractionHand.MAIN_HAND,null,pos,before);
        if(!LbcTmtItems.matches(source)){brokenOwners(game,owner,source,pos,before);return;}
        for(var property:LbcTmtTriggers.effective(source))LbcTmtAbilities.invoke(source,property.id(),()->{
            brokenOwners(game,owner,source,pos,before);return true;
        });
    }
    private static void brokenOwners(GameController game,ServerPlayer owner,ItemStack tool,BlockPos pos,BlockState before){
        game.items().imbaPickaxe().mined(owner,tool,pos,before);
        contactOwners(game,owner,tool,LbcImbaAxe.Trigger.BLOCK_BREAK);
    }
    static void activate(GameController game,ServerPlayer owner,ItemStack tool,BlockPos pos,BlockState before){brokenOwners(game,owner,tool,pos,before);}
    static void propertyContact(GameController game,ServerPlayer owner,ItemStack tool){contactOwners(game,owner,tool,LbcImbaAxe.Trigger.REAL_MELEE);}
    private static void contactOwners(GameController game,ServerPlayer owner,ItemStack tool,LbcImbaAxe.Trigger trigger){
        game.items().imbaShovel().used(owner,tool);game.items().imbaHoe().used(owner,tool);
        game.items().steampunkBreaker().used(owner,tool);game.items().imbaAxe().activate(owner,tool,trigger);
    }
}
