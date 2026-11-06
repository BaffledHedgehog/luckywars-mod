package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.item.*;

/** The complete native carrier/default/codec oracle also covers the eight related shared-factory users. */
public final class NativeProjectileWeaponChecks {
    private NativeProjectileWeaponChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Native projectile weapons: "+why);}
    private static ItemStack check(GameController game,WoolBucketChecks.Fixture f,Item item,Item carrier,String definition){
        var stack=NativeMeleeChecks.sourceOutcome(game,f,item,carrier,definition,17);
        require(ItemStack.matches(stack.copyWithCount(1),item.getDefaultInstance()),"complete default equals original-carrier source "+definition);
        require(carrier==Items.BOW?item instanceof BowItem:carrier==Items.CROSSBOW?item instanceof CrossbowItem:item instanceof TridentItem,"real native subtype "+definition);
        for(var tag:carrier.builtInRegistryHolder().tags().toList())require(stack.is(tag),"complete vanilla/Fabric/project tag graph "+definition+" "+tag);
        return stack;
    }
    public static void run(GameController game){try(var f=new WoolBucketChecks.Fixture(game,false)){
        for(var kind:LbcProjectileWeaponItems.Kind.values()){
            var stack=check(game,f,kind.item,kind.carrier,kind.definition);
            require(ItemStack.matches(stack,f.leaf(kind.id)),"ready aliases retain canonical count/components "+kind.id);
        }
        for(var kind:LbcImbaCrossbows.Kind.values())check(game,f,kind.item,Items.CROSSBOW,kind.definition);
        check(game,f,LbcChargedBowItems.RAILGUN,Items.BOW,"/data/lbc/loot_table/item_defs/lbc/railgun.json");
        check(game,f,LbcChargedBowItems.SHISHGUN,Items.BOW,"/data/lbc/loot_table/item_defs/lbc/shishgun.json");
    }SkyWars2.LOGGER.info("[check] PASS native-projectile-weapons (5 native ready weapons and8 shared-factory users; all13 independent original-carrier/default/Polymer/server-codec oracles, native subclasses and complete tag graph)");}
}
