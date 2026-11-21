package me.skywars2.game;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Actual loot outputs must not inherit another consumer's defaults through a shared primitive. */
public final class SharedLootPrimitiveChecks {
    private SharedLootPrimitiveChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Shared loot primitives: "+why);}
    private static List<ItemStack> roll(GameController game,ServerPlayer owner,String table){
        return game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:"+table);
    }
    private static ItemStack one(GameController game,ServerPlayer owner,String table,Item item){
        var result=roll(game,owner,table);require(result.size()==1&&result.getFirst().is(item),"real native identity for "+table);
        return result.getFirst();
    }
    public static void run(GameController game){
        var owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        var worldRandom=MatchRandomState.capture(owner.level().getRandom());var playerRandom=MatchRandomState.capture(owner.getRandom());
        try{
            var raw=one(game,owner,"item_defs/minecraft/repeating_command_block__6711510709f0",Items.REPEATING_COMMAND_BLOCK);
            require(raw.getCount()==1&&!LbcBucksTrader.matches(raw)&&!raw.has(DataComponents.CUSTOM_NAME),"generic primitive stays ordinary vanilla, not native money");
            var amounts=new HashSet<Integer>();
            for(int i=0;i<64;i++)for(var table:List.of("imba/random_bucks","ported/bucks")){
                var money=one(game,owner,table,LbcBucksTrader.TOKEN);amounts.add(money.getCount());
                require(LbcBucksTrader.matches(money)&&money.getCount()>=1&&money.getCount()<=64
                        &&money.has(DataComponents.CUSTOM_NAME)&&money.has(DataComponents.LORE)
                        &&money.has(DataComponents.ITEM_MODEL)&&money.has(DataComponents.TOOLTIP_STYLE),"full currency payload and source count provider for "+table);
                require(ItemStack.validateStrict(money).error().isEmpty(),"native currency components remain valid");
            }
            require(amounts.size()>1&&amounts.stream().anyMatch(count->count>1),"authored uniform1..64 count is active rather than an ignored functions field");
            var token=one(game,owner,"kit/academy_token",LbcAcademy.TOKEN);
            require(ItemData.intValue(token,"academy_token",0)==1&&!LbcBucksTrader.matches(token)
                    &&token.has(DataComponents.CUSTOM_NAME)&&token.has(DataComponents.LORE),"actual academy token retains its own native metadata");
            for(var name:List.of("blood_anal_boom","mana_gem")){
                var item=name.equals("mana_gem")?LbcTrinkets.MANA_GEM:LbcTrinkets.BLOOD_ANAL_BOOM;
                var stack=one(game,owner,"trinket/"+name,item);
                require(ItemData.stringValue(stack,"trinket","").equals(name)&&!LbcBucksTrader.matches(stack)
                        &&stack.has(DataComponents.CUSTOM_NAME)&&stack.has(DataComponents.LORE),"actual native trinket retains its own selector and presentation: "+name);
            }
            var head=one(game,owner,"otchim",Items.REPEATING_COMMAND_BLOCK);
            require(!LbcBucksTrader.matches(head)&&Identifier.parse("lbc:otchim").equals(head.get(DataComponents.ITEM_MODEL)),"visual Otchim head uses the generic primitive and its own model");
            var tmt=one(game,owner,"tmtrainer_drop",Items.REPEATING_COMMAND_BLOCK);
            require(ItemData.intValue(tmt,"tmt",0)==1&&ItemData.intValue(tmt,"drop",0)==1&&!LbcBucksTrader.matches(tmt),"existing unreleased TMT token keeps its own payload without currency defaults");
            var lucky=roll(game,owner,"luckyblock_block");
            require(lucky.size()==2&&lucky.stream().noneMatch(LbcBucksTrader::matches)
                    &&lucky.stream().anyMatch(stack->stack.is(Items.REPEATING_COMMAND_BLOCK)&&ItemData.intValue(stack,"adv_check_lbc",0)==1),"lucky block and its source achievement marker remain independent of currency");
            me.skywars2.SkyWars2.LOGGER.info("[check] PASS shared-loot-primitives (generic vanilla RCB, actual currency counts/full ready table, native academy/trinkets, visual head and independent lucky/TMT markers)");
        }finally{worldRandom.restore(owner.level().getRandom());playerRandom.restore(owner.getRandom());}
    }
}
