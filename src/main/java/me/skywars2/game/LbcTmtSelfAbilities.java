package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.CustomData;

/** Source self/jump properties. No fake hit, function interpreter or extra tick owner. */
final class LbcTmtSelfAbilities {
    private LbcTmtSelfAbilities(){}
    enum Group { CLASSIC, CHARGED, JUMP }
    record Profile(String id,Group group,CustomData flags) {
        LbcTmtAbilities.Ability ability(){return new LbcTmtAbilities.Ability(id,flags);}
    }
    private static Profile profile(String id,Group group,String flag,int value,int type){
        var data=new CompoundTag();data.putInt(flag,value);if(type!=0)data.putInt("type",type);
        return new Profile(id,group,CustomData.of(data));
    }
    private static final List<Profile> PROFILES=profiles();
    private static List<Profile> profiles(){
        var all=new ArrayList<Profile>();
        // This is the source swords-group order, independent of the random draw order.
        for(var flag:List.of("feeding","vampire","seaguardian","evolution"))
            all.add(profile("self/"+flag,Group.CLASSIC,flag,1,0));
        for(var flag:List.of("triple_golden_sword","blazefire_sword"))
            all.add(profile("self/"+flag,Group.CHARGED,flag,1,0));
        for(var flag:List.of("mystical","mystical_old"))for(int type=1;type<=3;type++)
            all.add(profile("jump/"+flag,Group.JUMP,flag,3,type));
        return List.copyOf(all);
    }
    static List<Profile> catalog(){return PROFILES;}

    private static boolean admitted(GameController game,ServerPlayer owner){
        return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
                &&LbcDimensionItems.ordinary(owner)&&game.canUseGameplayItems(owner)&&!game.isObserver(owner)
                &&game.stage()!=GameController.Stage.ENDING&&!game.items().actionsBlocked(owner)
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level());
    }

    /** The source group reads SelectedItem, even if the use event came from the other hand. */
    static int swords(GameController game,ServerPlayer owner){
        if(!admitted(game,owner))return 0;
        var stack=owner.getMainHandItem();if(LbcTmtAbilities.active(stack))return 0;
        var slots=LbcTmtTriggers.effective(stack);int dispatched=0;
        var ordered=new ArrayList<>(slots);
        ordered.sort(java.util.Comparator.comparingInt(LbcTmtSelfAbilities::order));
        for(var slot:ordered){
            boolean classic=false,charged=false;
            for(var profile:PROFILES)if(slot.data().matchedBy(profile.flags.copyTag())){
                classic|=profile.group==Group.CLASSIC;charged|=profile.group==Group.CHARGED;
            }
            if(!classic&&!charged)continue;
            if(owner.getMainHandItem()!=stack||stack.isEmpty()||!admitted(game,owner))break;
            final boolean runClassic=classic,runCharged=charged;
            if(LbcTmtAbilities.invoke(stack,slot.id(),()->{
                if(runClassic)game.items().classicMelee().sourceSelf(owner);
                if(runCharged)game.items().chargedSwords().sourceSelf(owner);
                return true;
            }))dispatched++;
        }
        return dispatched;
    }
    private static int order(LbcTmtAbilities.Ability slot){
        for(int i=0;i<PROFILES.size();i++)if(PROFILES.get(i).group!=Group.JUMP&&slot.data().matchedBy(PROFILES.get(i).flags.copyTag()))return i;
        return PROFILES.size();
    }

    /** Called once per source armor group. Each ordinary owner retains equipment and jammer checks. */
    static void armor(GameController game,ServerPlayer owner){
        if(!admitted(game,owner))return;
        var items=game.items();
        items.armorSets().techno().jump(owner);
        items.jumpArtifacts().jump(owner);
        items.shulkerArmor().jump(owner);
        items.armorSets().chaos().activate(owner);
        items.mana().overload().armor().jump(owner);
        items.halloweenWind().jump(owner);
        items.soulhunter().jump(owner);
    }

    /** Add private TMT types to the existing inventory union; repeated items never replay a jump. */
    static int jumpMasks(CompoundTag root){
        if(root.getIntOr("tmt_item",0)!=1)return 0;
        int masks=LbcTmtInventory.mysticalTypes(root,3);
        return (masks&14)|((masks>>>16)&14)<<4;
    }
}
