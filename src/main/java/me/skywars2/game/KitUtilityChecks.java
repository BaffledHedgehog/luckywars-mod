package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Actual source kit acquisition and registered Polymer prototype checks; behavior has its own groups. */
public final class KitUtilityChecks {
    private KitUtilityChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Kit utilities: "+why);}
    public static void mechanicalMenus(GameController game){
        require(game.inLobby(),"actual lobby catalogue selection");
        var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(p!=null,"native menu participant");
        var saved=new RegenerationBeltChecks.Snapshot(game,p);boolean ranked=game.config().ranked,kits=game.config().lbcKits;
        var progress=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,Map<String,Boolean>>();
        try{
            game.config().ranked=false;game.config().lbcKits=true;game.lbc().syncRanked();
            var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcChallengeKit=0;profile.lbcTrinket=0;
            Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            String[] gates={"lbc:true_advancements/armor/steampunk","lbc:true_advancements/armor/technomk2"};
            for(int n=0;n<2;n++){
                int id=12+n,slot=11+n;String gate=gates[n];
                var advancement=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(gate));require(advancement!=null,"native source gate "+gate);
                var old=new LinkedHashMap<String,Boolean>();var status=p.getAdvancements().getOrStartProgress(advancement);
                for(var criterion:advancement.value().criteria().keySet())old.put(criterion,status.getCriterion(criterion).isDone());progress.put(advancement,old);
                for(var criterion:old.keySet())p.getAdvancements().revoke(advancement,criterion);profile.lbcUnlocks.remove(gate);profile.lbcKit=0;
                require(!game.lbc().select(p,LbcLoadouts.Kind.KITS,9000,slot)&&profile.lbcKit==0,"locked source menu refuses kit"+id);
                for(var criterion:old.keySet())p.getAdvancements().award(advancement,criterion);
                require(game.lbc().select(p,LbcLoadouts.Kind.KITS,9000,slot)&&profile.lbcKit==id&&game.lbc().selected(profile,LbcLoadouts.Kind.KITS,9000,slot),"actual original menu slot selects kit"+id);
                p.getInventory().clearContent();game.lbc().apply(p,false);
                var actual=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)if(!p.getInventory().getItem(i).isEmpty())actual.add(p.getInventory().getItem(i));
                var expected=n==0?List.of(LbcMechanicalItems.BREAKER,LbcMechanicalItems.GLOVE):List.of(LbcMechanicalItems.HOOK);
                require(actual.size()==expected.size(),"original kit contents/disabled rocket pool "+id+" actual="+actual);
                for(var item:expected)require(actual.stream().filter(v->v.is(item)&&v.getCount()==1).count()==1,"selected source grant reaches registered native item "+item);
            }
        }finally{
            game.config().ranked=ranked;game.config().lbcKits=kits;game.lbc().syncRanked();
            for(var a:progress.entrySet())for(var c:a.getValue().entrySet())if(c.getValue())p.getAdvancements().award(a.getKey(),c.getKey());else p.getAdvancements().revoke(a.getKey(),c.getKey());
            saved.restore(game);saved.restoreAttributes();saved.restoreUse();saved.restoreProfile(game);game.players().save();
        }
        SkyWars2.LOGGER.info("[check] PASS mechanical-kits-menu (actual lobby selection, original kit12/13 slots and native gate advancements, locked refusal/unlock, normal loadout grant, native breaker/glove/hook, zero-roll rocket retained)");
    }
    public static void presentation(GameController game){try(var f=new ElementalGrimoireChecks.Fixture(game)){
        f.initialize();var p=f.owner;
        var tablets=game.lbc().rollTable(p.level(),p.position(),p,"lbc:kit/multiply_tablet");
        require(tablets.size()==4,"four independent original kit pools");
        var nativeItems=List.of(LbcKitUtilityItems.DOUBLE,LbcKitUtilityItems.TRIPLE,LbcKitUtilityItems.QUADRUPLE,LbcKitUtilityItems.QUINTUPLE);
        for(int i=0;i<4;i++){
            int number=i+2;Item item=nativeItems.get(i);var leaf=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/multiply_"+number);
            require(leaf.size()==1&&leaf.getFirst().is(item)&&leaf.getFirst().getCount()==1,"native canonical singleton "+number);
            var s=leaf.getFirst();require(tablets.stream().filter(v->v.is(item)&&ItemStack.matches(v,s)).count()==1,"real source kit reaches exact native leaf "+number);
            require(LbcMultiplierTablets.kind(s)==number&&s.get(DataComponents.ITEM_MODEL).toString().equals("lbc:x"+number),"source flag/model "+number);
            require(s.get(DataComponents.CUSTOM_NAME).getContents() instanceof TranslatableContents t&&t.getKey().equals("multiply_by_"+number),"existing author name key "+number);
            require(s.get(DataComponents.LORE).lines().size()==2&&!s.get(DataComponents.CUSTOM_NAME).getStyle().isItalic(),"original lore/format "+number);
        }
        var palm=game.lbc().rollTable(p.level(),p.position(),p,"lbc:kit/palm_with_bananas");
        var leaf=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/banana_palm");
        require(palm.size()==1&&leaf.size()==1&&palm.getFirst().is(LbcKitUtilityItems.PALM)&&ItemStack.matches(palm.getFirst(),leaf.getFirst()),"real palm kit reaches canonical native type");
        var s=palm.getFirst();require(LbcFartus.data(s).getIntOr("banana_palm",0)==1&&s.getMaxDamage()==64&&s.get(DataComponents.ITEM_MODEL).toString().equals("lbc:palm_small"),"original palm flag/durability/model");
        require(s.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size()==2&&s.get(DataComponents.LORE).lines().getFirst().getStyle().getColor().getValue()==0xffffff,"authored palm attributes and white lore");
        var food=game.lbc().rollTable(p.level(),p.position(),p,"lbc:bananas");int count=food.stream().mapToInt(ItemStack::getCount).sum();
        require(count>=3&&count<=6&&food.stream().allMatch(v->LbcFartus.data(v).getIntOr("eat_banana",0)==1&&v.has(DataComponents.CONSUMABLE)&&v.has(DataComponents.FOOD)),"existing source food owner remains the banana consumer");
    }SkyWars2.LOGGER.info("[check] PASS kit-utility-items (five registered native source leaves, actual four-tablet/palm kits, exact source flag/model/name/lore, native banana food dependency)");}
}
