package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Real final-stack use, source-family payloads and the complete collection trigger. */
public final class ExtraGrimoireChecks {
    private ExtraGrimoireChecks() { }
    private static void require(boolean b,String why){if(!b)throw new IllegalStateException("Extra grimoires: "+why);}
    private static void clear(GameController g){g.items().soulless().stop();g.items().halloweenWind().stop();g.items().fartus().stop();g.items().summons().stop();g.items().reactive().stop();g.items().advancedSnow().stop();}
    private static void payload(AllGrimoireChecks.Fixture f,int id){
        var i=f.base.game.items();
        if(id==243)require(f.base.fire().size()==1,"Tiny Fire creates one native fireball");
        if(id==250)require(i.clouds().acidCount()==6,"Tiny Acid creates the six shared acid charges");
        if(id==251)require(i.wands().electroCount()==1,"Tiny Electro creates one arc");
        if(id==253){int drops=0;for(var e:f.level.getAllEntities())if(!f.base.before.contains(e)&&e instanceof ItemEntity item&&!item.isRemoved()){
            require(Arrays.stream(LbcSoulless.MATERIALS).anyMatch(item.getItem()::is),"actual metasola loot");drops++;
        }require(drops==1,"one generated metasola");}
        if(id==255)require(f.base.owner.getDeltaMovement().lengthSqr()>0,"self-only wind uses native movement");
        if(id==257)require(i.halloweenWind().count()==5,"five native wind charges");
        if(id==258)require(i.fartus().count()==5,"five native bananas/labels");
        if(id==260)require(i.iceWand().count()==1,"one native ice projectile");
        if(id==278){var clouds=i.advancedSnow().snapshot();require(clouds.size()==1,"one source snow cloud");
            var cloud=clouds.getFirst().entity();require(cloud.getOwner()==f.base.owner&&cloud.getRadius()==4&&cloud.getDuration()==250
                    &&cloud.position().distanceToSqr(f.base.owner.position().add(0,2,0))<.0001,"snow source owner/radius/duration/position");}
        if(id==279){for(int tick=0;tick<20&&i.advancedSnow().snapshot().isEmpty();tick++)i.exasperato().tick();
            var clouds=i.advancedSnow().snapshot();require(clouds.size()==1,"one aimed source snow cloud");
            var cloud=clouds.getFirst().entity();require(cloud.getOwner()==f.base.owner&&cloud.getRadius()==4&&cloud.getDuration()==250
                    &&cloud.getZ()>f.at.z+7&&Math.abs(cloud.getY()-(f.base.owner.getEyeY()+3))<.0001,"Academy snow at ray impact+3, not caster position");}
    }
    private static void collection(AllGrimoireChecks.Fixture f){
        var p=f.base.owner;var adv=f.base.game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/legends/all_grimoires"));
        require(adv!=null&&adv.value().criteria().size()==268&&!adv.value().criteria().containsKey("spell_189"),"268 attainable criteria without phantom189");
        var before=new HashSet<String>();p.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(before::add);
        try{
            for(var key:adv.value().criteria().keySet())p.getAdvancements().revoke(adv,key);
            for(var key:adv.value().criteria().keySet())if(Integer.parseInt(key.substring(6))<243)p.getAdvancements().award(adv,key);
            require(!p.getAdvancements().getOrStartProgress(adv).isDone(),"old collection now requires the new books");
            for(var d:LbcLibrarySpells.extraDefinitions()){
                p.getInventory().clearContent();var stack=f.loot(d);p.getInventory().setItem(0,stack);
                CriteriaTriggers.INVENTORY_CHANGED.trigger(p,p.getInventory(),stack);
                require(p.getAdvancements().getOrStartProgress(adv).getCriterion("spell_"+d.spellId()).isDone(),"actual new inventory predicate "+d.id());
                require(p.getAdvancements().getOrStartProgress(adv).isDone()==(d.spellId()==279),"complete only after final new book");
            }
        }finally{for(var key:adv.value().criteria().keySet())if(before.contains(key))p.getAdvancements().award(adv,key);else p.getAdvancements().revoke(adv,key);}
    }
    public static void run(GameController g){
        var stats=new IdentityHashMap<net.minecraft.world.item.Item,Integer>();
        try(var f=new AllGrimoireChecks.Fixture(g)){
            require(LbcLibrarySpells.extraDefinitions().size()==28,"ordinary/Academy books without summoners, seven rainbow colours and both snow positions");
            for(int id=262;id<=270;id++)require(LbcLibrarySpells.extraDefinitions().stream().noneMatch(d->LbcExtraGrimoires.retired(d.spellId()))
                    &&!LbcLibrarySpells.cast(g,f.base.owner,InteractionHand.MAIN_HAND,id),"retired summon book has no playable outcome "+id);
            for(var d:LbcLibrarySpells.extraDefinitions())stats.put(d.item(),f.base.owner.getStats().getValue(Stats.ITEM_USED.get(d.item())));
            try{
                for(var d:LbcLibrarySpells.extraDefinitions()){
                    f.presentation(d);
                    for(var hand:InteractionHand.values())for(boolean shift:new boolean[]{false,true}){
                        clear(g);f.restoreWorld=true;f.reset();var p=f.base.owner;var stack=f.loot(d);
                        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                        p.setItemInHand(hand,stack);p.setShiftKeyDown(shift);
                        f.base.mana.set(p,shift?-500:0);int balance=f.base.mana.get(p),used=p.getStats().getValue(Stats.ITEM_USED.get(d.item()));
                        require(f.base.input(hand)&&stack.isEmpty()&&f.base.mana.get(p)==balance,"real final-book consumption/no mana gate "+d.id()+" "+hand+" shift="+shift);
                        require(p.getStats().getValue(Stats.ITEM_USED.get(d.item()))==used+1,"exactly one use stat "+d.id());payload(f,d.spellId());
                    }
                    clear(g);f.restoreWorld=true;f.reset();var p=f.base.owner;var stack=f.loot(d);stack.setCount(2);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
                    p.entityTags().add("time_stopped");try{require(!f.base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"stopped input preserves "+d.id());}finally{p.entityTags().remove("time_stopped");}
                    f.base.field=new Marker(EntityTypes.MARKER,f.level);f.base.field.setPos(f.at);f.base.field.entityTags().add("stopper_magic");
                    require(f.level.addFreshEntity(f.base.field),"native antimagic fixture");g.items().magic().loaded(f.base.field);
                    try{require(!f.base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"antimagic preserves "+d.id());}finally{f.base.removeField();}
                    g.config().offhandWands=false;p.setItemInHand(InteractionHand.OFF_HAND,stack);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                    try{require(!f.base.input(InteractionHand.OFF_HAND)&&stack.getCount()==2,"disabled offhand preserves "+d.id());}finally{g.config().offhandWands=true;}
                }
                clear(g);f.restoreWorld=true;f.reset();collection(f);
            }finally{clear(g);for(var entry:stats.entrySet())f.base.owner.getStats().setValue(f.base.owner,Stats.ITEM_USED.get(entry.getKey()),entry.getValue());}
        }
        SkyWars2.LOGGER.info("[check] PASS extra-grimoires");
    }
    private static AllGrimoireChecks.Fixture retained;private static String epoch;
    public static void stopProbe(GameController g){
        require(retained==null,"exclusive stop probe");var f=new AllGrimoireChecks.Fixture(g,true);
        try{f.world();for(int id:new int[]{252,257,258,260,271})require(LbcLibrarySpells.cast(g,f.base.owner,InteractionHand.MAIN_HAND,id),"retained extra book "+id);
            epoch=g.items().epoch();f.retain=true;retained=f;
        }finally{f.close();}SkyWars2.LOGGER.info("[check] PASS extra-grimoires-stop-probe");
    }
    public static void cleanup(GameController g){
        require(retained!=null,"retained stop probe");try{
            require(!g.items().currentEpoch(epoch)&&g.items().halloweenWind().count()==0&&g.items().fartus().count()==0&&g.items().iceWand().count()==0&&g.items().summons().count()==0,"actual stop clears new shared payloads");
        }finally{retained.restore();retained=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS extra-grimoires-cleanup");
    }
}
