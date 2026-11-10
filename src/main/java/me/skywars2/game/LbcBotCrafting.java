package me.skywars2.game;

import java.util.List;
import java.util.BitSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/** Bounded discovery through the shared recipes, then actual player-grid clicks. No private recipes/debits. */
final class LbcBotCrafting {
    final ServerPlayer player;
    final LbcBotToolCrafting tools;
    int first,second=1,next,stage=-1,a,b,crafted,pairCursor;
    final ItemStack[] observed=new ItemStack[36];
    final BitSet changed=new BitSet(36),pairs=new BitSet(36*36);
    boolean searched;
    LbcBotCrafting(ServerPlayer player,LbcChefBrain.Driver driver,LbcBotSurvival survival){this.player=player;tools=new LbcBotToolCrafting(player,driver,survival);}
    boolean active(){return stage>=0||tools.active();}
    boolean prepared(){return tools.prepared();}
    private int inventorySlot(int index){
        for(int i=0;i<player.inventoryMenu.slots.size();i++){
            var slot=player.inventoryMenu.slots.get(i);
            if(slot.container==player.getInventory()&&slot.getContainerSlot()==index)return i;
        }
        return -1;
    }
    private void refreshPairs(){
        for(int i=0;i<36;i++){
            var current=player.getInventory().getItem(i);
            if(observed[i]==null||!ItemStack.matches(observed[i],current)){observed[i]=current.copy();changed.set(i);}
        }
        for(int i=changed.nextSetBit(0);i>=0;i=changed.nextSetBit(i+1)){
            if(player.getInventory().getItem(i).isEmpty())continue;
            for(int j=0;j<36;j++)if(i!=j&&!player.getInventory().getItem(j).isEmpty())pairs.set(Math.min(i,j)*36+Math.max(i,j));
        }
        changed.clear();searched=pairs.isEmpty();
    }
    private int nextPair(){
        int pair=pairs.nextSetBit(pairCursor);if(pair<0)pair=pairs.nextSetBit(0);
        if(pair>=0){pairs.clear(pair);pairCursor=pair+1;}return pair;
    }
    static boolean special(ItemStack item){
        var model=item.get(DataComponents.ITEM_MODEL);
        return item.has(DataComponents.CUSTOM_DATA)||model!=null&&!model.getNamespace().equals("minecraft");
    }
    void cancel(){
        if(active()){
            if(stage>=0){changed.set(a);changed.set(b);}
            player.closeContainer();tools.cancel();stage=-1;searched=false;first=0;second=1;next=player.tickCount+20;
        }
    }
    boolean tick(boolean interrupted){
        if(interrupted){cancel();return false;}
        if(player.isUsingItem())return false;
        if(tools.navigate())return true;
        if(player.tickCount<next)return active();
        if(stage<0&&tools.tick()){next=player.tickCount+5;return true;}
        if(player.containerMenu!=player.inventoryMenu)return false;
        if(!active()){
            if(!player.inventoryMenu.getCarried().isEmpty()||player.getInventory().getFreeSlot()<0)return false;
            for(int slot=1;slot<=4;slot++)if(player.inventoryMenu.getSlot(slot).hasItem())return false;
            refreshPairs();
            if(searched){next=player.tickCount+5;return false;}
            // Empty/common pairs are cheap; bound recipe queries separately and retain round-robin fairness.
            for(int probes=0,queries=0;probes<72&&queries<4;probes++){
                int pair=nextPair();if(pair<0){searched=true;next=player.tickCount+5;return false;}
                int x=pair/36,y=pair%36;first=x;second=y;
                var left=player.getInventory().getItem(x);var right=player.getInventory().getItem(y);
                if(left.isEmpty()||right.isEmpty()||!special(left)&&!special(right))continue;
                queries++;
                var input=CraftingInput.of(2,2,List.of(left,right,ItemStack.EMPTY,ItemStack.EMPTY));
                var kind=LbcCraftingRecipe.craftingKind(input);
                if(kind==null||kind==LbcCraftingRecipe.Kind.TINY_REFORGE||kind==LbcCraftingRecipe.Kind.REROLL_MACHINE)continue;
                a=x;b=y;stage=0;break;
            }
            next=player.tickCount+5;return active();
        }
        var menu=player.inventoryMenu;
        switch(stage){
            case 0->menu.clicked(inventorySlot(a),0,ContainerInput.PICKUP,player);
            case 1->menu.clicked(1,0,ContainerInput.PICKUP,player);
            case 2->menu.clicked(inventorySlot(b),0,ContainerInput.PICKUP,player);
            case 3->menu.clicked(2,0,ContainerInput.PICKUP,player);
            case 4->{
                if(!menu.getSlot(0).hasItem()){cancel();return false;}
                menu.clicked(0,0,ContainerInput.PICKUP,player);crafted++;
            }
            case 5->{
                if(!menu.getCarried().isEmpty()){
                    int slot=player.getInventory().getFreeSlot();if(slot<0){cancel();return false;}
                    menu.clicked(inventorySlot(slot),0,ContainerInput.PICKUP,player);
                }
            }
            default->{
                for(int slot=1;slot<=4;slot++)if(menu.getSlot(slot).hasItem()){
                    menu.clicked(slot,0,ContainerInput.QUICK_MOVE,player);menu.broadcastChanges();next=player.tickCount+5;return true;
                }
                stage=-1;searched=false;next=player.tickCount+20;return false;
            }
        }
        stage++;next=player.tickCount+5;menu.broadcastChanges();return true;
    }
}
