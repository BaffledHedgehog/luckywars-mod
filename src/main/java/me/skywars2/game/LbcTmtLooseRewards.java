package me.skywars2.game;

import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Read-only private admission and retained dispatch into the existing physical loose-item owner. */
final class LbcTmtLooseRewards {
    private LbcTmtLooseRewards(){}
    static final String COPIER_FAMILY="native/loose/copier",MULTIPLY_FAMILY="native/loose/multiply";
    static final int ALL=0,TABLETS=1,COPIERS=2;

    static boolean outer(ItemStack stack){return LbcTmtItems.matches(stack)&&!LbcTmtAbilities.active(stack);}
    /** Scalar native source selector; a physical TMT's aggregate selector never chooses a mode. */
    static int kind(ItemStack source){if(outer(source))return 0;int kind=LbcMultiplierTablets.kind(source);return kind>=2&&kind<=4?kind:0;}
    /** Catalog boundary: native copier1 or one genuine tablet2/3/4, before generic service keys. */
    static int prototypeKind(ItemStack source){if(outer(source))return 0;return ItemData.intValue(source,"copypaster",0)==1?1:kind(source);}
    private static ItemData.IntReader data(LbcTmtAbilities.Ability property){return (ItemData.IntReader)(Object)property.data();}
    private static boolean copier(LbcTmtAbilities.Ability property){return data(property).skywars2$readInt("copypaster",0)==1;}
    private static int kind(LbcTmtAbilities.Ability property){int kind=data(property).skywars2$readInt("multiply",0);return kind>=2&&kind<=4?kind:0;}
    static boolean property(LbcTmtAbilities.Ability property){return copier(property)||kind(property)!=0;}
    /** Also the copier donor exclusion: real private copier sources survive forged outer flag0. */
    static boolean copier(ItemStack source){
        if(!outer(source))return ItemData.intValue(source,"copypaster",0)==1;
        for(var property:LbcTmtAbilities.read(source))if(copier(property))return true;
        return false;
    }
    /** Tablet donors exclude presence, including native unknown/zero modes, exactly as ordinary items. */
    static boolean multiplyPresent(ItemStack donor){
        if(!outer(donor))return ItemData.contains(donor,"multiply");
        for(var property:LbcTmtAbilities.read(donor))if(data(property).skywars2$contains("multiply"))return true;
        return false;
    }
    /** Source/catalog and ENTITY_LOAD admission, with no projection, mutation or NBT copy. */
    static boolean present(ItemStack source){
        if(!outer(source))return copier(source)||kind(source)!=0;
        for(var property:LbcTmtAbilities.read(source))if(property(property))return true;
        return false;
    }
    /** Inspection API; real work iterates the immutable component's cached slots without filtering copies. */
    static List<LbcTmtAbilities.Ability> properties(ItemStack host){return LbcTmtAbilities.read(host).stream().filter(LbcTmtLooseRewards::property).toList();}

    static boolean work(LbcLooseRewards owner,ItemEntity host,int mode){
        var properties=LbcTmtAbilities.read(host.getItem());boolean changed=false;
        // Legacy items_tick invokes multiply in items_tick_act, then the separate copier body.
        // Each successful native property owns its normal one-unit debit; no-op properties retain it.
        for(int phase=0;phase<2;phase++){
            if(phase==0&&mode==COPIERS||phase==1&&mode==TABLETS)continue;
            boolean tablet=phase==0;
            for(var property:properties){
                if(host.isRemoved()||host.getItem().isEmpty())return changed;
                if(tablet?kind(property)==0:!copier(property))continue;
                var stack=host.getItem();
                changed|=LbcTmtAbilities.invoke(stack,property.id(),()->phaseWork(owner,host,tablet));
            }
        }
        return changed;
    }
    private static boolean phaseWork(LbcLooseRewards owner,ItemEntity host,boolean tablet){return tablet?owner.tablet(host):owner.copy(host);}
}
