package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.ScoreHolder;

/** Internal TMT common-wand composition. Existing owners, real held stack and real native projectiles. */
public final class TmtWandChecks {
    private TmtWandChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT wands: "+why);}
    private static LbcWands.WandAbility profile(String id){
        return LbcWands.tmtAbilities().stream().filter(p->p.nativeItem().equals(Identifier.parse(id))).findFirst().orElseThrow();
    }
    private static ItemStack item(List<LbcTmtAbilities.Ability> abilities){
        var stack=new ItemStack(Items.CARROT_ON_A_STICK);var data=new CompoundTag();
        data.putInt("tmt_item",1);data.putInt("tmt_texture_seed",56123);data.putInt("unrelated",73);
        LbcTmtAbilities.write(data,abilities);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        stack.set(DataComponents.ITEM_NAME,Component.literal("TMT identity witness"));
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:tmt/tmt_1"));return stack;
    }
    private static CompoundTag payload(ItemStack stack,String id){
        return LbcTmtAbilities.read(stack).stream().filter(a->a.id().equals(id)).findFirst().orElseThrow().data().copyTag();
    }
    private static List<Entity> fresh(WoolBucketChecks.Fixture f,java.util.Set<java.util.UUID> before){
        var result=new ArrayList<Entity>();
        for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID())&&!entity.isRemoved()){f.own(entity);result.add(entity);}
        return result;
    }
    private static void metadata(){
        var profiles=LbcWands.tmtAbilities();var families=new HashSet<String>();var flags=new HashSet<CustomData>();
        require(profiles.size()==103,"all current common-wand profiles");
        for(var profile:profiles){
            families.add(profile.family());require(flags.add(profile.flags()),"no duplicate property payload slots");
            var stack=item(List.of(profile.inMode(2)));var plan=LbcWands.tmtPlan(stack);
            require(plan.size()==1&&plan.getFirst().nativeItem().equals(profile.nativeItem()),"each ordinary/Academy/Tiny profile resolves exactly once "+profile.nativeItem());
        }
        require(families.size()==58,"all current common-wand families");
        var regular=profile("skywars2:fire_wand");var academic=profile("skywars2:academy_fire_wand");
        var tiny=profile("skywars2:tiny_fire_wand");var air=profile("skywars2:academy_air_wand");
        var mix=item(List.of(tiny.inMode(0),regular.inMode(2),air.inMode(1)));
        var plan=LbcWands.tmtPlan(mix);
        require(plan.stream().map(LbcWands.WandAbility::family).toList().equals(List.of("stair","stfire","stfire_tiny")),"source order rather than draw order");
        require(plan.get(1).nativeItem().equals(regular.nativeItem())&&plan.get(0).nativeItem().equals(air.nativeItem()),"upgrade is private to its property; Tiny survives");
        require(!ItemData.contains(mix,"upgraded")&&!ItemData.contains(mix,"mode")&&!ItemData.contains(mix,"type"),"no shared version/mode/type parameters");
        var repeated=item(List.of(regular.inMode(1),tiny.inMode(0),academic.inMode(3)));
        require(LbcTmtAbilities.read(repeated).size()==2&&LbcWands.tmtPlan(repeated).size()==2
                &&payload(repeated,"wand/stfire").getIntOr("mode",0)==3,"last duplicate family wins without extra executions");
        var exception=new IllegalStateException("deliberate native-owner failure");
        try{
            LbcTmtAbilities.invoke(mix,"wand/stfire",()->{
                require(ItemData.intValue(mix,"mode",0)==2&&ItemData.intValue(mix,"upgraded",-1)==0,"native owner sees its own ordinary parameters");
                require(!LbcTmtAbilities.invoke(mix,"wand/stair",()->{throw new AssertionError("nested scope executed");}),"reentrant scope is rejected");
                var data=mix.get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("mode",3);data.putInt("counter",19);
                mix.set(DataComponents.CUSTOM_DATA,CustomData.of(data));throw exception;
            });
            throw new AssertionError("owner failure was swallowed");
        }catch(IllegalStateException actual){require(actual==exception,"original owner exception preserved");}
        require(payload(mix,"wand/stfire").getIntOr("mode",0)==3&&payload(mix,"wand/stfire").getIntOr("counter",0)==19
                &&payload(mix,"wand/stair").getIntOr("mode",0)==1,"per-property state retained even when native owner throws");
        require(!ItemData.contains(mix,LbcTmtAbilities.ACTIVE)&&!ItemData.contains(mix,"mode")&&ItemData.intValue(mix,"unrelated",0)==73,"scope fully restored and unrelated metadata retained");
        require(!LbcTmtAbilities.active(mix),"owner exception releases the runtime scope");
        require(!LbcTmtAbilities.invoke(mix,"wand/missing",()->{throw new AssertionError("missing property executed");}),"unknown property has no fallback execution");
    }

    public static void run(GameController game){
        metadata();
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();game.config().lbcTrinketLimit=0;
            var mana=game.items().mana();var wands=game.items().wands();
            var fireCost=mana.score(ScoreHolder.forNameOnly("stfire_wand_mana_use"),"lbc.math");
            var tinyCost=mana.score(ScoreHolder.forNameOnly("tiny_fire_wand_mana_use"),"lbc.math");
            var sauvojenCost=mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math");
            int savedFire=fireCost.get(),savedTiny=tinyCost.get(),savedSauvojen=sauvojenCost.get();
            var regular=profile("skywars2:fire_wand");var tiny=profile("skywars2:tiny_fire_wand");var air=profile("skywars2:academy_air_wand");
            var before=new HashSet<java.util.UUID>();for(var entity:f.level.getAllEntities())before.add(entity.getUUID());
            try{
                fireCost.set(100);tinyCost.set(25);mana.maximum(f.owner,10000);mana.set(f.owner,1000);
                var stack=item(List.of(tiny.inMode(0),regular.inMode(2),air.inMode(2)));
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                f.owner.setShiftKeyDown(true);
                require(wands.castTmtSpells(f.owner,InteractionHand.MAIN_HAND)==3,"three existing native owners execute in one continuation");
                require(mana.get(f.owner)==875,"two paid spells and free Academy mode selection keep native payment");
                var actors=fresh(f,before);
                require(actors.size()==4&&actors.stream().allMatch(e->e instanceof SmallFireball)
                        &&actors.stream().noneMatch(e->e instanceof LargeFireball),"ordinary Fire remains three small balls despite its mode2 and neighboring Academy ability; Tiny adds one");
                for(var entity:actors)require(((SmallFireball)entity).getOwner()==f.owner&&game.items().wands().isFireProjectile(entity),"true owner and existing native projectile classification");
                require(f.owner.getMainHandItem()==stack&&stack.getCount()==1
                        &&stack.get(DataComponents.ITEM_NAME).getString().equals("TMT identity witness"),"actual held object, item identity, count and presentation preserved");
                require(payload(stack,"wand/stair").getIntOr("mode",0)==3&&payload(stack,"wand/stfire").getIntOr("mode",0)==2
                        &&payload(stack,"wand/stfire_tiny").getIntOr("upgraded",-1)==0,"native mode write persists only to its own property");
                require(!ItemData.contains(stack,LbcTmtAbilities.ACTIVE)&&!ItemData.contains(stack,"mode")&&!ItemData.contains(stack,"upgraded"),"temporary native parameter projection is gone after input");
                var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,stack).getOrThrow()).getOrThrow();
                require(ItemStack.matches(stack,restored)&&payload(restored,"wand/stair").getIntOr("mode",0)==3,"native save/load retains private versions and changed modes");
                for(var entity:actors)f.discard(entity);
                f.owner.setShiftKeyDown(false);

                // A real long-lived producer captures its own version/data before a sibling changes mode.
                require(game.items().sauvojen().snapshot().isEmpty(),"quiet existing producer owner");
                var longLived=item(List.of(profile("skywars2:academy_sauvojen_wand").inMode(0),profile("skywars2:academy_fire_wand").inMode(1)));
                sauvojenCost.set(2500);mana.set(f.owner,10000);f.owner.setItemInHand(InteractionHand.MAIN_HAND,longLived);
                require(wands.castTmtSpells(f.owner,InteractionHand.MAIN_HAND)==2&&game.items().sauvojen().snapshot().size()==1,"native long-lived producer and Fire both start");
                var producer=game.items().sauvojen().snapshot().getFirst();var captured=producer.usedStack.copy();
                require(producer.academy&&ItemData.intValue(producer.usedStack,"upgraded",0)==1
                        &&ItemData.stringValue(producer.usedStack,LbcTmtAbilities.ACTIVE,"").equals("wand/sauvojen_wand"),"producer captures its own Academy property context");
                f.owner.setShiftKeyDown(true);
                require(wands.castTmtSpells(f.owner,InteractionHand.MAIN_HAND)==2&&game.items().sauvojen().snapshot().size()==2,"a later input uses existing repeat-cast policy");
                require(payload(longLived,"wand/stfire").getIntOr("mode",0)==2
                        &&payload(longLived,"wand/sauvojen_wand").getIntOr("mode",-1)==0
                        &&ItemStack.matches(captured,producer.usedStack),"changing Fire mode cannot rewrite an already-running producer's captured data");
                var returned=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,captured).getOrThrow()).getOrThrow();
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,returned);
                require(wands.castTmtSpells(f.owner,InteractionHand.MAIN_HAND)==2&&game.items().sauvojen().snapshot().size()==3,
                        "native saved/restored source copies keep their context without becoming permanently cast-locked");
                for(var entity:fresh(f,before))f.discard(entity);
                require(game.items().sauvojen().snapshot().isEmpty(),"native entity unload cleans both producer owners");f.owner.setShiftKeyDown(false);

                // Source-order first cast fails, but the later Tiny family can still spend its own25.
                var poor=item(List.of(tiny.inMode(0),regular.inMode(1)));f.owner.setItemInHand(InteractionHand.MAIN_HAND,poor);
                mana.set(f.owner,25);
                require(wands.castTmtSpells(f.owner,InteractionHand.MAIN_HAND)==1&&mana.get(f.owner)==0,"failed expensive family does not suppress affordable later family");
                var produced=fresh(f,before);
                require(produced.size()==1&&produced.getFirst() instanceof SmallFireball,"only affordable Tiny emits");for(var entity:produced)f.discard(entity);

                mana.set(f.owner,1000);f.owner.setItemInHand(InteractionHand.OFF_HAND,poor);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));
                game.config().offhandWands=false;
                require(wands.castTmtSpells(f.owner,InteractionHand.OFF_HAND)==0&&mana.get(f.owner)==1000,"offhand setting blocks before native payment");
                game.config().offhandWands=true;
                require(wands.castTmtSpells(f.owner,InteractionHand.OFF_HAND)==2&&mana.get(f.owner)==875
                        &&f.owner.getMainHandItem().is(Items.STICK)&&f.owner.getMainHandItem().getCount()==7,"exact used offhand, independent of opposite hand");
                for(var entity:fresh(f,before))f.discard(entity);

                f.pause();int value=mana.get(f.owner);
                require(wands.castTmtSpells(f.owner,InteractionHand.OFF_HAND)==0&&mana.get(f.owner)==value,"native stopped input cannot enter any property");f.thaw();
                f.field(false,f.owner.position().add(6,0,0));require(game.items().magic().suppressed(f.owner),"actual MAG positive control");
                require(wands.castTmtSpells(f.owner,InteractionHand.OFF_HAND)==0&&mana.get(f.owner)==value,"shared native suppression prevents every magical payment");f.removeFields();
                require(wands.castTmtSpells(f.owner,InteractionHand.MAIN_HAND)==0,"ordinary item cannot enter TMT continuation");
            }finally{
                fireCost.set(savedFire);tinyCost.set(savedTiny);sauvojenCost.set(savedSauvojen);fresh(f,before);
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-wands (103 profiles/58 families; private version/mode, native held-item scopes, source order, real Fire/Tiny/Academy owner composition, mana, offhand, TimeStop/MAG and codec restoration)");
    }
}
