package me.skywars2.game;

import java.util.*;
import me.skywars2.game.gui.loadouts.LbcLoadouts;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class TrinketGrantChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Trinket grant: "+why);}
    private static int count(net.minecraft.server.level.ServerPlayer p){int n=0;for(int i=0;i<36;i++)n+=p.getInventory().getItem(i).getCount();return n;}
    static void run(GameController game){
        var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(p!=null&&game.playing(),"active participant");
        var saved=new GameChecks.WandThirdPlayerState(p,game.items().mana());var before=new HashSet<Entity>();p.level().getAllEntities().forEach(before::add);
        try{
            String table=null;int tested=0;
            for(var entry:LbcLoadouts.catalog(LbcLoadouts.Kind.TRINKETS)){
                if(entry.id()==0)continue;p.getInventory().clearContent();game.lbc().grantTable(p,entry.grantTable());
                if(count(p)==0)continue;tested++;if(table==null)table=entry.grantTable();
                for(int i=0;i<9;i++)require(p.getInventory().getItem(i).isEmpty(),"trinket "+entry.id()+" leaves hotbar free");
                require(!p.getInventory().getItem(35).isEmpty(),"trinket "+entry.id()+" starts at slot35");
            }
            require(tested>10&&table!=null,"actual native trinket catalog exercised");
            p.getInventory().clearContent();game.lbc().grantTable(p,table);int one=count(p);
            var first=p.getInventory().getItem(35).copy();game.lbc().grantTable(p,table);
            require(count(p)==one*2,"duplicate grants conserve all items");
            for(int i=0;i<9;i++)require(p.getInventory().getItem(i).isEmpty(),"duplicate trinkets stay outside hotbar");
            p.getInventory().clearContent();p.getInventory().setItem(35,new ItemStack(Items.DIAMOND,17));game.lbc().grantTable(p,table);
            require(p.getInventory().getItem(35).is(Items.DIAMOND)&&p.getInventory().getItem(35).getCount()==17
                    &&ItemStack.isSameItemSameComponents(first,p.getInventory().getItem(34)),"occupied distant slot preserved");
            p.getInventory().clearContent();for(int i=9;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            game.lbc().grantTable(p,table);require(ItemStack.isSameItemSameComponents(first,p.getInventory().getItem(8)),"full main inventory falls back to far hotbar slot8");
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));game.lbc().grantTable(p,table);
            int dropped=0;for(var e:p.level().getAllEntities())if(!before.contains(e)&&e instanceof ItemEntity item&&ItemStack.isSameItemSameComponents(item.getItem(),first))dropped+=item.getItem().getCount();
            require(dropped==one,"full inventory uses native overflow without losing or overwriting trinkets");
            me.skywars2.SkyWars2.LOGGER.info("[check] PASS trinket-grants (all native catalog entries, duplicates, occupied/fill slots, native overflow and count conservation)");
        }finally{var fresh=new ArrayList<Entity>();p.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e instanceof ItemEntity)fresh.add(e);});fresh.forEach(Entity::discard);saved.restore(game);}
    }
}
