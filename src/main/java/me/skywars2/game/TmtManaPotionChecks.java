package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Source payloads, actual native use/finish/impact callbacks, and ordinary native cloud delivery. */
public final class TmtManaPotionChecks {
    private TmtManaPotionChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT mana potions: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scene");var next=new Scene(game);running=next;
        try{next.begin();}catch(Throwable error){running=null;try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-mana-potions (35 authored mana doses, empty transport rejection, source PotionContents finish listener and actual mixed golden-apple one debit/food count, complete Succubus effects, real main/offhand splash and lingering factories, private native source persistence and ordinary native cloud delivery)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-mana-potions",error);}}
    public static void cancel(GameController game){var scene=running;if(scene==null)return;require(scene.game==game,"same cancel owner");running=null;scene.close();}

    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        List<LbcTmtAbilities.Ability> doses;Cow witness;AreaEffectCloud cloud;
        int start,when,phase,beforeMaximum;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            var items=new ArrayList<Item>(List.of(LbcMana.MANA_POTION,LbcMana.MANA_SPLASH_POTION,LbcMana.MANA_LINGERING_POTION,Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION));
            for(int i=0;i<5;i++)items.add(LbcTmtItems.prototype(i).getItem());stats=new NativeMeleeChecks.NativeStats(f.owner,items);}
        ItemStack source(LbcTmtAbilities.Ability property){return LbcTmtComponents.source(game,property);}
        LbcTmtAbilities.Ability dose(Item nativeItem,int amplifier,int duration){
            return doses.stream().filter(property->{var source=source(property);if(source.getItem()!=nativeItem)return false;
                for(var effect:source.get(DataComponents.POTION_CONTENTS).getAllEffects())if(effect.is(MobEffects.RAID_OMEN)&&effect.getAmplifier()==amplifier&&effect.getDuration()==duration)return true;return false;
            }).findFirst().orElseThrow(()->new IllegalStateException("Missing actual authored dose "+nativeItem+"/"+amplifier+"/"+duration));
        }
        ItemStack carrier(int index,LbcTmtAbilities.Ability... properties){var stack=LbcTmtItems.prototype(index);
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(properties));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));stack.setCount(2);return stack;}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void enumerate(){
            doses=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                    .filter(property->LbcTmtManaPotions.delivery(source(property))!=null).toList();
            int[] counts=new int[3];var unique=new HashSet<String>();
            for(var property:doses){var source=source(property);var item=source.getItem();
                if(item!=LbcMana.MANA_POTION&&item!=LbcMana.MANA_SPLASH_POTION&&item!=LbcMana.MANA_LINGERING_POTION)continue;
                String key=item+"/"+source.get(DataComponents.POTION_CONTENTS)+"/"+source.get(DataComponents.POTION_DURATION_SCALE);
                if(unique.add(key))counts[LbcTmtManaPotions.delivery(source).ordinal()]++;
            }
            require(counts[0]==14&&counts[1]==10&&counts[2]==11,"all35 authored native mana dose profiles retained: "+java.util.Arrays.toString(counts));
            for(var item:List.of(LbcMana.MANA_POTION,LbcMana.MANA_SPLASH_POTION,LbcMana.MANA_LINGERING_POTION))
                require(LbcTmtManaPotions.delivery(item.getDefaultInstance())==null,"empty default transport is not a sampled dose");
            int chemical=234;for(var item:List.of(LbcItems.TERMIT,LbcItems.SULFUR,LbcItems.PHOSPHIN,LbcItems.GREMUCH)){
                // PotionItem.getDefaultInstance intentionally substitutes WATER; use the registered
                // physical components, as the qualified chemical source/model checks do.
                var source=new ItemStack(item);var delivery=LbcTmtManaPotions.delivery(source);
                int amplifier=LbcItems.chemicalAmplifier(source.get(DataComponents.POTION_CONTENTS));
                require(delivery==LbcTmtManaPotions.Delivery.LINGERING&&amplifier==chemical,
                        "real native ChemicalPotionItem registered payload and source factory: item="+item+" delivery="+delivery+" amplifier="+amplifier+" expected="+chemical+" contents="+source.get(DataComponents.POTION_CONTENTS));chemical++;}
            var raw=f.loot("lbc:succubus_potion");require(LbcTmtManaPotions.projectileSource(game,raw)==raw,"ordinary native source is never rewritten");
        }
        ItemStack finish(ItemStack stack){f.owner.stopUsingItem();f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.isUsingItem(),"real native use accepts its actual consuming carrier");
            var result=stack.finishUsingItem(f.level,f.owner);f.owner.stopUsingItem();f.owner.setItemInHand(InteractionHand.MAIN_HAND,result);return result;}
        void begin(){
            f.prepareParticipants();enumerate();start=game.server().getTickCount();game.items().mana().baseRegen(f.owner,0);
            var nativeDrink=f.loot("lbc:item_defs/skywars2/mana_potion__4005b410f6fd");
            require(finish(nativeDrink).is(Items.GLASS_BOTTLE)&&game.items().mana().score(f.owner,"effect_add_manamax").get()==1,"stock native source finish transfers its exact one-tick dose and one bottle remainder");
            game.items().mana().clearEffects(f.owner);f.owner.removeAllEffects();
            var drink=dose(LbcMana.MANA_POTION,17,1);var mixed=carrier(4,drink);var original=mixed.get(DataComponents.CUSTOM_DATA);
            int food=game.items().mana().score(f.owner,"food_count").get();beforeMaximum=game.items().mana().maximum(f.owner);
            require(finish(mixed)==mixed&&mixed.getCount()==1,"real TMT golden-apple finish debits exactly one physical item, with no private bottle replacement");
            require(game.items().mana().score(f.owner,"effect_add_manamax").get()==1&&game.items().mana().score(f.owner,"food_count").get()==food+1,"same native potion listener transfers exact dose while the sole real food event counts once");
            require(mixed.get(DataComponents.CUSTOM_DATA)==original&&!mixed.has(DataComponents.POTION_CONTENTS)&&!LbcTmtAbilities.nativePredicatesActive(),"private drink payload restores outer components/slots and releases every source scope");next(0);
        }
        void succubus(){
            game.items().mana().clearEffects(f.owner);f.owner.removeAllEffects();f.owner.getFoodData().setFoodLevel(1);f.owner.getFoodData().setSaturation(0);
            var raw=f.loot("lbc:succubus_potion");finish(raw);int originalFood=f.owner.getFoodData().getFoodLevel();int originalRegen=f.owner.getEffect(MobEffects.REGENERATION).getDuration();
            f.owner.removeAllEffects();f.owner.getFoodData().setFoodLevel(1);f.owner.getFoodData().setSaturation(0);
            var property=doses.stream().filter(row->ItemData.intValue(source(row),"succubus",0)==1).findFirst().orElseThrow();
            var mixed=carrier(4,property);int nutrition=mixed.get(DataComponents.FOOD).nutrition();finish(mixed);
            require(mixed.getCount()==1&&f.owner.getEffect(MobEffects.REGENERATION).getAmplifier()==1&&f.owner.getEffect(MobEffects.REGENERATION).getDuration()==originalRegen
                    &&f.owner.getFoodData().getFoodLevel()==Math.min(20,originalFood+nutrition),"complete source Succubus regeneration and saturation pass through the native listener, alongside one base apple nutrition");
            require(LbcSheep.potion(mixed),"private source flags retain the actual native Succubus ingredient predicate");
            f.owner.removeAllEffects();f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        }
        Cow cow(){var actor=new Cow(EntityTypes.COW,f.level);actor.setPos(f.home.add(5,2,0));actor.setNoAi(true);actor.setNoGravity(true);actor.setSilent(true);
            require(f.level.addFreshEntity(actor),"native isolated potion witness inserted");f.own(actor);return actor;}
        AbstractThrownPotion launch(LbcTmtAbilities.Ability property,InteractionHand hand){
            var expected=source(property);var stack=carrier(0,property,dose(LbcMana.MANA_POTION,17,1));var original=stack.get(DataComponents.CUSTOM_DATA);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);f.owner.setItemInHand(hand,stack);
            var before=new HashSet<AbstractThrownPotion>(f.level.getEntitiesOfClass(AbstractThrownPotion.class,f.owner.getBoundingBox().inflate(20)));
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,hand).consumesAction(),"actual native used-hand entry accepts selected potion factory "+hand);
            var created=f.level.getEntitiesOfClass(AbstractThrownPotion.class,f.owner.getBoundingBox().inflate(20),actor->!before.contains(actor));
            require(created.size()==1&&stack.getCount()==1&&f.owner.getItemInHand(hand)==stack,"one real selected native potion and one exact physical debit, retained mixed carrier");
            var shot=created.getFirst();f.own(shot);var payload=shot.getItem();
            require(shot.getOwner()==f.owner&&payload.getItem()==expected.getItem()&&payload.get(DataComponents.POTION_CONTENTS).equals(expected.get(DataComponents.POTION_CONTENTS))
                    &&java.util.Objects.equals(payload.get(DataComponents.POTION_DURATION_SCALE),expected.get(DataComponents.POTION_DURATION_SCALE)),"native projectile retains complete private source type, dose, flags/color/scale and caster");
            require(!ItemData.contains(payload,LbcTmtAbilities.SLOTS)&&!ItemData.contains(payload,LbcTmtAbilities.ACTIVE)&&!LbcTmtItems.matches(payload),"producer snapshot has no borrowed outer TMT/sibling state");
            var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);var decoded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,payload).getOrThrow()).getOrThrow();
            require(ItemStack.matches(payload,decoded)&&stack.get(DataComponents.CUSTOM_DATA)==original&&LbcTmtAbilities.read(stack).size()==2,"native source survives real codec roundtrip while both physical private traits remain unchanged");return shot;
        }
        void splash(InteractionHand hand){var target=cow();var shot=launch(dose(LbcMana.MANA_SPLASH_POTION,18,3600),hand);
            require(shot instanceof ThrownSplashPotion,"real splash source factory class");shot.setPos(target.position());
            ((ThrownSplashPotion)shot).onHitAsPotion(f.level,shot.getItem(),new EntityHitResult(target));
            require(target.hasEffect(MobEffects.RAID_OMEN)&&target.getEffect(MobEffects.RAID_OMEN).getAmplifier()==18&&target.getEffect(MobEffects.RAID_OMEN).getDuration()==3600,"native direct-hit splash applies exact source duration before ordinary mana conversion");
            f.discard(shot);game.server().getScoreboard().resetAllPlayerScores(target);f.discard(target);
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-start<100,"bounded ordinary native potion scene");int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<1)return false;require(game.items().mana().maximum(f.owner)==beforeMaximum+2500&&game.items().mana().score(f.owner,"effect_add_manamax").get()==0,"ordinary native mana owner processes exactly one accepted stat dose");succubus();splash(InteractionHand.MAIN_HAND);next(1);return false;}
            if(phase==1){if(elapsed<1)return false;splash(InteractionHand.OFF_HAND);next(2);return false;}
            if(phase==2){if(elapsed<1)return false;witness=cow();var shot=launch(dose(LbcMana.MANA_LINGERING_POTION,16,3600),InteractionHand.OFF_HAND);
                require(shot instanceof ThrownLingeringPotion,"real lingering source factory class");shot.setPos(witness.position());
                var before=new HashSet<AreaEffectCloud>(f.level.getEntitiesOfClass(AreaEffectCloud.class,witness.getBoundingBox().inflate(2)));
                ((ThrownLingeringPotion)shot).onHitAsPotion(f.level,shot.getItem(),new BlockHitResult(witness.position(),Direction.UP,BlockPos.containing(witness.position()),false));
                var created=f.level.getEntitiesOfClass(AreaEffectCloud.class,witness.getBoundingBox().inflate(2),actor->!before.contains(actor));
                require(created.size()==1&&created.getFirst().getOwner()==f.owner&&created.getFirst().get(DataComponents.POTION_CONTENTS).equals(shot.getItem().get(DataComponents.POTION_CONTENTS)),"native lingering impact creates one owned cloud with complete source payload");
                cloud=created.getFirst();f.own(cloud);f.discard(shot);next(3);return false;}
            // Observe the first native cloud application, before its20tick reapplication can add a second dose.
            if(phase==3){if(elapsed<24)return false;int timer=game.items().mana().score(witness,"effect_boost_mana").get();
                require(timer>=850&&timer<=900,"ordinary native cloud scheduler delivers source quarter-duration potion to real generic LivingEntity");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.items().mana().clearEffects(f.owner);if(witness!=null){game.server().getScoreboard().resetAllPlayerScores(witness);f.discard(witness);}if(cloud!=null)f.discard(cloud);}
            finally{try{f.close();}finally{stats.close();}}}
    }
}
