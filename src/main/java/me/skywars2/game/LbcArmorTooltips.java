package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/** Client tooltip grouping only; native enchantment slots and equipped effects remain authoritative. */
final class LbcArmorTooltips {
    private LbcArmorTooltips(){}

    static boolean aimbobBoots(ItemInstance stack){
        var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        return attributes!=null&&attributes.modifiers().stream()
                .anyMatch(entry->entry.modifier().id().toString().equals("minecraft:aimbob_boots"));
    }

    static void groupFeet(ItemStack projected){
        var lore=projected.get(DataComponents.LORE);
        if(lore==null)return;
        var lines=lore.lines();var rows=new ArrayList<Component>();
        boolean[] grouped=new boolean[lines.size()];int first=lines.size();Component header=null;boolean armor=false;
        for(int i=0;i<lines.size();i++){
            boolean feet=key(lines.get(i),"item.modifiers.feet");
            if(!feet&&!key(lines.get(i),"item.modifiers.armor"))continue;
            if(feet)header=lines.get(i);else armor=true;
            int start=i>0&&lines.get(i-1).getString().isEmpty()?i-1:i;
            first=Math.min(first,start);
            for(int j=start;j<=i;j++)grouped[j]=true;
            while(i+1<lines.size()&&!lines.get(i+1).getString().isEmpty()){
                i++;grouped[i]=true;rows.add(lines.get(i));
            }
        }
        if(!armor||header==null)return;
        var out=new ArrayList<Component>(lines.size());
        for(int i=0;i<lines.size();i++){
            if(i==first){out.add(Component.empty());out.add(header);out.addAll(rows);}
            if(!grouped[i])out.add(lines.get(i));
        }
        projected.set(DataComponents.LORE,new ItemLore(List.copyOf(out)));
    }

    static boolean key(Component line,String key){
        return line.getContents() instanceof TranslatableContents text&&text.getKey().equals(key)
                ||line.getSiblings().stream().anyMatch(child->key(child,key));
    }
}
