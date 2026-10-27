package me.skywars2.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Current Belt's Paper item consumer, with live physical inventory and native self healing. */
public final class LbcRegenerationBelt {
    private static final String[] PREFIXES={"luckywars:","lws:","lucky_wars:","minecraft:"};
    private final GameController game;
    private int phase;
    private static final class Group {
        final String operation;final double value;final boolean stackable;long count;
        Group(String operation,double value,boolean stackable,long count){this.operation=operation;this.value=value;this.stackable=stackable;this.count=count;}
    }
    public LbcRegenerationBelt(GameController game){this.game=game;}
    private boolean admitted(ServerPlayer p){
        String dimension=p.level().dimension().identifier().toString();
        return !dimension.equals("minecraft:imprinted")&&!dimension.equals("minecraft:nexus")
                &&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&game.canUseGameplayItems(p)
                &&game.items().wands().validRoleTarget(p,false)&&game.stage()!=GameController.Stage.ENDING
                &&!game.items().wands().inputTimeStopped(p)&&!game.items().timeStop().stopped(p.level());
    }
    private static CompoundTag payload(ItemStack stack){
        if(stack.isEmpty())return null;var flags=LbcFartus.data(stack);
        if(flags.getIntOr("heall",0)!=1)return null;
        var root=flags.getCompound("PublicBukkitValues").orElse(null);if(root==null)return null;
        // Paper checks its plugin namespace, lws and lucky_wars for the nested container.
        for(int i=0;i<3;i++){var result=root.getCompound(PREFIXES[i]+"regen").orElse(null);if(result!=null)return result;}
        return null;
    }
    private static String string(CompoundTag tag,String name){for(var prefix:PREFIXES)if(tag.get(prefix+name) instanceof StringTag value)return value.value();return null;}
    private static Double number(CompoundTag tag,String name){for(var prefix:PREFIXES)if(tag.get(prefix+name) instanceof DoubleTag value)return value.value();return null;}
    private static boolean stackable(CompoundTag tag){for(var prefix:PREFIXES)if(tag.get(prefix+"stackable") instanceof ByteTag value)return value.value()!=0;return false;}
    private static void gather(ItemStack stack,String slot,Map<String,Group> groups){
        var tag=payload(stack);if(tag==null)return;
        var value=number(tag,"value");var operation=string(tag,"operation");var active=string(tag,"slot");var id=string(tag,"id");
        if(value==null||operation==null||active==null||id==null||id.isEmpty())return;
        active=active.toLowerCase(Locale.ROOT);operation=operation.toLowerCase(Locale.ROOT);
        if(!active.equals("inventory")&&!active.equals(slot)&&!(active.equals("armor")&&(slot.equals("head")||slot.equals("chest")||slot.equals("legs")||slot.equals("feet"))))return;
        boolean stackable=stackable(tag);var group=groups.get(id);
        if(group==null)groups.put(id,new Group(operation,value,stackable,stackable?stack.getCount():1));
        else if(stackable)group.count+=stack.getCount();
    }
    private static double apply(double initial,Map<String,Group> groups){
        double result=initial;
        for(var g:groups.values())switch(g.operation){
            case "set"->result=g.value;
            case "multiple","mult"->result*=g.stackable?Math.pow(g.value,g.count):g.value;
            default->result+=g.stackable?g.value*g.count:g.value;
        }
        return result;
    }
    /** Each native physical slot0..40 is read once; no cursor/craft/body/saddle aliases. */
    public double rate(ServerPlayer p){
        var nonArmor=new LinkedHashMap<String,Group>();var armor=new LinkedHashMap<String,Group>();
        int selected=p.getInventory().getSelectedSlot();
        gather(p.getInventory().getItem(selected),"mainhand",nonArmor);
        gather(p.getInventory().getItem(40),"offhand",nonArmor);
        for(int slot=0;slot<36;slot++)if(slot!=selected)gather(p.getInventory().getItem(slot),"inventory",nonArmor);
        int[] slots={39,38,37,36};String[] keys={"head","chest","legs","feet"};
        for(int i=0;i<slots.length;i++)gather(p.getInventory().getItem(slots[i]),keys[i],armor);
        return apply(apply(0,nonArmor),armor);
    }
    private boolean taskTrigger(ServerPlayer p){
        for(int slot=0;slot<36;slot++)if(!p.getInventory().getItem(slot).isEmpty()&&LbcFartus.data(p.getInventory().getItem(slot)).getIntOr("heall",0)==1)return true;
        return !p.getOffhandItem().isEmpty()&&LbcFartus.data(p.getOffhandItem()).getIntOr("heall",0)==1;
    }
    private void collection(ServerPlayer p){
        if(!taskTrigger(p)||game.items().magic().beaconSuppressed(p.position()))return;
        var board=game.server().getScoreboard();var objective=board.getObjective("y");if(objective==null)return;
        var score=board.getPlayerScoreInfo(p,objective);if(score==null||score.value()>-100000)return;
        int count=p.getInventory().clearOrCountMatchingItems(stack->!stack.isEmpty()&&LbcFartus.data(stack).getIntOr("heall",0)==1,true,0,p.inventoryMenu.getCraftSlots());
        if(count>=10)game.items().mana().award(p,"lbc:true_advancements/accesories/regeneration_belt");
    }
    public void tick(){
        if(game.stage()==GameController.Stage.ENDING||game.maps().level()==null||game.items().wands().timeStopped())return;
        phase=(phase+1)%100;
        for(var p:game.server().getPlayerList().getPlayers()){
            // A forbidden-dimension player never returns from the whole server task.
            if(!admitted(p))continue;
            double amount=rate(p);
            if(amount>0&&!game.items().magic().beaconSuppressedAabb(p.level(),p.position(),20))
                // Native heal reaches ShadowAbsorption's visible/shadow ledger and self-Greed rules.
                p.heal((float)amount);
            if(phase==0)collection(p);
        }
    }
    public void forget(ServerPlayer p){}
    public void stop(){phase=0;}
}
