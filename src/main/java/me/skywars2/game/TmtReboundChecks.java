package me.skywars2.game;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Mixed native payloads, not just one wand exercised through different entry points. */
final class TmtReboundChecks {
    private TmtReboundChecks(){}
    private static int seed=100000;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT rebound: "+why);}
    private static LbcTmtAbilities.Ability find(GameController game,Predicate<CompoundTag> predicate){
        return game.items().tmt().catalog().families().stream().flatMap(f->f.variants().stream())
                .filter(p->predicate.test(p.data().copyTag())).findFirst().orElseThrow();
    }
    private static LbcTmtAbilities.Ability flag(GameController game,String flag){return find(game,d->d.getIntOr(flag,0)==1);}
    private static LbcTmtAbilities.Ability artifact(GameController game,int group,int type){return find(game,d->d.getIntOr("mystical",0)==group&&d.getIntOr("type",0)==type);}
    private static LbcTmtAbilities.Ability bind(LbcTmtAbilities.Ability property,LbcTmtTriggers.Action action){
        var data=property.data().copyTag();var inputs=new CompoundTag();inputs.putString(action.name(),"LEFT_CLICK");data.put(LbcTmtTriggers.KEY,inputs);
        return new LbcTmtAbilities.Ability(property.id(),CustomData.of(data));
    }
    private static ItemStack stack(int carrier,LbcTmtAbilities.Ability... properties){
        var stack=LbcTmtItems.prototype(carrier);var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        data.putInt("tmt_item",1);data.putInt("tmt_texture_seed",++seed);data.putInt(LbcTmtTriggers.SCHEMA,1);
        LbcTmtAbilities.write(data,List.of(properties));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;
    }
    private static void punch(WoolBucketChecks.Fixture f,ItemStack source){
        f.owner.stopUsingItem();f.owner.setShiftKeyDown(false);f.owner.setItemInHand(InteractionHand.MAIN_HAND,source);
        f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        f.owner.connection.handlePunch(net.minecraft.network.protocol.game.ServerboundPunchPacket.INSTANCE);
    }
    static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();f.move(f.owner,f.home.add(0,30,0));f.move(f.other,f.owner.position().add(0,0,2));
            f.owner.setYRot(0);f.owner.setXRot(0);
            var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            var queued=new HashSet<>(game.items().stonekivi().queuedSnapshot());
            var profile=game.players().get(f.owner.getUUID());boolean scroll=profile.modeScroll;
            try{
                profile.modeScroll=true;f.owner.getInventory().setSelectedSlot(0);
                var tesla=find(game,d->d.getStringOr(LbcTmtAbilities.NATIVE,"").equals("skywars2:academy_tesla_wand")&&d.getIntOr("mode",1)==1);
                for(int carrier=0;carrier<5;carrier++){
                    var property=bind(tesla,LbcTmtTriggers.Action.USE);var source=stack(carrier,property);var skin=source.get(DataComponents.ITEM_MODEL);
                    f.owner.setItemInHand(InteractionHand.MAIN_HAND,source);f.owner.setShiftKeyDown(true);int manaBefore=game.items().mana().get(f.owner);
                    require(f.owner.gameMode.useItem(f.owner,f.level,source,InteractionHand.MAIN_HAND).consumesAction(),"Shift RMB can cycle each physical carrier");
                    int mode=ItemData.intValue(LbcTmtComponents.source(game,LbcTmtAbilities.read(source).getFirst()),"mode",1);
                    require(mode==2,"Shift RMB updates the private native Tesla mode");
                    LbcModeScroll.forget(f.owner);f.owner.connection.handleSetCarriedItem(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(4));
                    require(ItemData.intValue(LbcTmtComponents.source(game,LbcTmtAbilities.read(source).getFirst()),"mode",1)==3
                            &&f.owner.getInventory().getSelectedSlot()==0,"four raw wheel slots select four private modes while keeping the same held stack");
                    LbcModeScroll.withSteps(-4,()->f.owner.gameMode.useItem(f.owner,f.level,source,InteractionHand.MAIN_HAND));
                    require(ItemData.intValue(LbcTmtComponents.source(game,LbcTmtAbilities.read(source).getFirst()),"mode",1)==2
                            &&LbcTmtAbilities.read(source).getFirst().inputs().get(LbcTmtTriggers.Action.USE)==LbcTmtTriggers.Input.LEFT_CLICK
                            &&Objects.equals(source.get(DataComponents.ITEM_MODEL),skin)&&game.items().mana().get(f.owner)==manaBefore,"reverse private mode cycle preserves binding/skin and never casts or spends mana");
                    LbcModeScroll.forget(f.owner);f.owner.setShiftKeyDown(false);
                }
                int food=game.items().mana().score(f.owner,"food_count").get();
                punch(f,stack(0,bind(flag(game,"feeding"),LbcTmtTriggers.Action.SELF)));
                require(game.items().mana().score(f.owner,"food_count").get()==food+1,"SELF reuses one native Feeding branch");
                f.owner.removeAllEffects();
                var unrelated=new ItemStack(Items.STICK);var extra=new CompoundTag();extra.putInt("mystical",3);extra.putInt("type",2);
                unrelated.set(DataComponents.CUSTOM_DATA,CustomData.of(extra));f.owner.getInventory().setItem(9,unrelated);
                punch(f,stack(0,bind(artifact(game,3,1),LbcTmtTriggers.Action.JUMP)));
                require(f.owner.hasEffect(MobEffects.SPEED)&&!f.owner.hasEffect(MobEffects.LEVITATION),"JUMP selects its native artifact without replaying unrelated inventory");
                f.owner.getInventory().setItem(9,ItemStack.EMPTY);f.owner.removeAllEffects();
                var edible=stack(0,bind(flag(game,"eat_haste"),LbcTmtTriggers.Action.CONSUME));
                punch(f,edible);
                require(f.owner.hasEffect(MobEffects.HASTE)&&edible.getCount()==1,"CONSUME reuses authored effect without inventing eating or consuming its carrier");
                f.owner.removeAllEffects();game.items().mana().maximum(f.owner,10000);game.items().mana().set(f.owner,1000);
                punch(f,stack(0,bind(artifact(game,2,6),LbcTmtTriggers.Action.MELEE)));
                require(game.items().mana().get(f.owner)==2000,"MELEE executes native inventory artifact payload on the aimed target");
                var axeFamily=game.items().tmt().catalog().families().stream().filter(row->row.variants().stream().anyMatch(a->
                        ((ItemData.IntReader)(Object)a.data()).skywars2$readString(LbcTmtAbilities.NATIVE,"").equals("skywars2:imba_axe"))).findFirst().orElseThrow();
                require(axeFamily.events().contains(LbcTmtCatalog.Event.MINING)&&!axeFamily.events().contains(LbcTmtCatalog.Event.BLOCK),"axe contact belongs to real mining/melee domains, not a placeable-block factory");
                int axeJobs=game.items().imbaAxe().count();punch(f,stack(0,bind(axeFamily.variants().getFirst(),LbcTmtTriggers.Action.MELEE)));
                require(game.items().imbaAxe().count()==axeJobs+1,"rebound tool contact enters the existing finite native axe scheduler exactly once");
                extra=new CompoundTag();extra.putInt("mystical_old",4);extra.putInt("type",4);unrelated.set(DataComponents.CUSTOM_DATA,CustomData.of(extra));
                f.owner.getInventory().setItem(9,unrelated);long criteria=game.items().incomingArtifacts().criterionCount();
                punch(f,stack(0,bind(artifact(game,4,5),LbcTmtTriggers.Action.INCOMING)));
                require(f.owner.hasEffect(MobEffects.STRENGTH)&&!f.owner.hasEffect(MobEffects.SLOWNESS)
                        &&game.items().incomingArtifacts().criterionCount()==criteria,"INCOMING selects one payload without a fake hurt criterion or ordinary inventory reaction");
                f.owner.getInventory().setItem(9,ItemStack.EMPTY);f.owner.removeAllEffects();
                punch(f,stack(0,bind(artifact(game,4,1),LbcTmtTriggers.Action.INCOMING)));
                require(f.owner.hasEffect(MobEffects.RESISTANCE)&&game.items().incomingArtifacts().criterionCount()==criteria,"rebound shield payload does not require fabricating an incoming attack");
                f.owner.removeAllEffects();
                f.move(f.other,f.owner.position().add(15,0,0));
                var bow=stack(1,bind(flag(game,"double_old"),LbcTmtTriggers.Action.SHOT));
                var ammo=new ItemStack(Items.ARROW,5);f.owner.getInventory().setItem(9,ammo);
                long arrows=java.util.stream.StreamSupport.stream(f.level.getAllEntities().spliterator(),false).filter(AbstractArrow.class::isInstance).count();
                punch(f,bow);
                long afterArrows=java.util.stream.StreamSupport.stream(f.level.getAllEntities().spliterator(),false).filter(AbstractArrow.class::isInstance).count();
                require(afterArrows-arrows==2&&ammo.getCount()==4,"SHOT uses true native bow factory, two arrows and one actual ammunition debit");
                f.owner.getInventory().setItem(9,ItemStack.EMPTY);
                var egg=new ItemStack(Items.COW_SPAWN_EGG);var body=new CompoundTag();body.putBoolean("NoAI",true);body.putBoolean("Silent",true);
                egg.set(DataComponents.ENTITY_DATA,TypedEntityData.of(EntityTypes.COW,body));var data=new CompoundTag();
                data.putString(LbcTmtAbilities.NATIVE,BuiltInRegistries.ITEM.getKey(egg.getItem()).toString());LbcTmtComponents.capture(game,egg,data);
                var eggProperty=new LbcTmtAbilities.Ability("fixture/cow",CustomData.of(data));
                var floor=f.owner.blockPosition().offset(0,0,2);f.set(floor,Blocks.STONE.defaultBlockState());f.owner.setXRot(35);
                var block=stack(0,bind(eggProperty,LbcTmtTriggers.Action.BLOCK));punch(f,block);
                var cows=new ArrayList<Cow>();for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID())&&entity instanceof Cow cow)cows.add(cow);
                require(cows.size()==1&&cows.getFirst().isNoAi()&&cows.getFirst().isSilent()&&block.isEmpty(),"BLOCK reuses one authored egg, full entity data and real native debit");
                var root=f.owner.blockPosition().offset(0,0,2);f.set(root,Blocks.STONE.defaultBlockState());
                var pick=stack(0,bind(flag(game,"chaos_hoe"),LbcTmtTriggers.Action.MINING));punch(f,pick);
                require(f.level.getBlockState(root).isAir()&&!game.items().imbaPickaxe().snapshot().isEmpty(),"MINING performs true protected root break then existing connected-mining continuation");
            }finally{
                profile.modeScroll=scroll;LbcModeScroll.forget(f.owner);f.owner.setShiftKeyDown(false);
                for(var work:game.items().stonekivi().queuedSnapshot())if(!queued.contains(work))work.cancel();
                for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID()))f.own(entity);
            }
        }
    }
}
