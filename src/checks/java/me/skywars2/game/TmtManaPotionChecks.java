package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
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
            SkyWars2.LOGGER.info("[check] PASS tmt-mana-potions (35 authored mana doses plus4 actual catalog chemical234..237 profiles/8 legal main-offhand throws, inherited WATER-default negative; native source color/scale/effect/CODEC, clean projectile/native impact and existing cloud-owner first quantum; empty transport rejection, source finish listener/one real apple debit, complete Succubus, native splash/lingering and cloud delivery)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-mana-potions",error);}}
    public static void cancel(GameController game){var scene=running;if(scene==null)return;require(scene.game==game,"same cancel owner");running=null;scene.close();}

    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        final MatchRandomState worldRandom;final Set<UUID> initialActors=new HashSet<>();
        final List<LbcTmtAbilities.Ability> chemicals=new ArrayList<>();
        List<LbcTmtAbilities.Ability> doses;Cow witness;AreaEffectCloud cloud,retiredChemical;
        int start,when,phase,beforeMaximum,chemicalIndex,chemicalLife,particleStart;boolean closed;
        Object chemicalState;Vec3 chemicalOrigin;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            var items=new ArrayList<Item>(List.of(LbcMana.MANA_POTION,LbcMana.MANA_SPLASH_POTION,LbcMana.MANA_LINGERING_POTION,Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION));
            items.addAll(List.of(LbcItems.TERMIT,LbcItems.SULFUR,LbcItems.PHOSPHIN,LbcItems.GREMUCH));
            for(int i=0;i<5;i++)items.add(LbcTmtItems.prototype(i).getItem());stats=new NativeMeleeChecks.NativeStats(f.owner,items);
            worldRandom=MatchRandomState.capture(f.level.getRandom());f.level.getAllEntities().forEach(actor->initialActors.add(actor.getUUID()));}
        ItemStack source(LbcTmtAbilities.Ability property){return LbcTmtComponents.source(game,property);}
        LbcTmtAbilities.Ability dose(Item nativeItem,int amplifier,int duration){
            return doses.stream().filter(property->{var source=source(property);if(source.getItem()!=nativeItem)return false;
                for(var effect:source.get(DataComponents.POTION_CONTENTS).getAllEffects())if(effect.is(MobEffects.RAID_OMEN)&&effect.getAmplifier()==amplifier&&effect.getDuration()==duration)return true;return false;
            }).findFirst().orElseThrow(()->new IllegalStateException("Missing actual authored dose "+nativeItem+"/"+amplifier+"/"+duration));
        }
        ItemStack carrier(int index,LbcTmtAbilities.Ability... properties){return carrier(index,2,properties);}
        ItemStack carrier(int index,int count,LbcTmtAbilities.Ability... properties){var stack=LbcTmtItems.prototype(index);
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(properties));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));stack.setCount(count);return stack;}
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
                        "real native ChemicalPotionItem registered payload and source factory: item="+item+" delivery="+delivery+" amplifier="+amplifier+" expected="+chemical+" contents="+source.get(DataComponents.POTION_CONTENTS));
                var creative=item.getDefaultInstance();require(LbcItems.chemicalAmplifier(creative.get(DataComponents.POTION_CONTENTS))==0
                        &&new PotionContents(net.minecraft.world.item.alchemy.Potions.WATER).equals(creative.get(DataComponents.POTION_CONTENTS)),"inherited PotionItem creative getter actually replaces registered chemical dose with effectless WATER");
                var property=doses.stream().filter(row->source(row).getItem()==item
                        &&java.util.Objects.equals(source(row).get(DataComponents.POTION_CONTENTS),source.get(DataComponents.POTION_CONTENTS))
                        &&java.util.Objects.equals(source(row).get(DataComponents.POTION_DURATION_SCALE),source.get(DataComponents.POTION_DURATION_SCALE))).findFirst()
                        .orElseThrow(()->new IllegalStateException("Catalog lost actual native chemical dose "+item+": "+source.get(DataComponents.POTION_CONTENTS)));
                var reconstructed=source(property);var contents=reconstructed.get(DataComponents.POTION_CONTENTS);
                require(LbcItems.chemicalAmplifier(contents)==chemical&&contents.customColor().equals(source.get(DataComponents.POTION_CONTENTS).customColor())
                        &&Float.valueOf(.25f).equals(reconstructed.get(DataComponents.POTION_DURATION_SCALE)),"actual catalog privately reconstructs native chemical color/amplifier/scale");
                var effects=new ArrayList<net.minecraft.world.effect.MobEffectInstance>();contents.getAllEffects().forEach(effects::add);
                require(effects.size()==1&&effects.getFirst().is(MobEffects.RAID_OMEN)&&effects.getFirst().getDuration()==20&&effects.getFirst().getAmplifier()==chemical,"actual catalog chemical keeps exact single authored20tick selector effect");
                var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);var decoded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,reconstructed).getOrThrow()).getOrThrow();
                require(ItemStack.matches(decoded,reconstructed),"complete private chemical reconstruction persists through native codec");
                String itemId=BuiltInRegistries.ITEM.getKey(item).toString();require(game.items().tmt().catalog().domains().findings().stream()
                        .noneMatch(row->row.item().equals(itemId)&&row.source().equals("native default")
                                &&row.disposition()!=LbcTmtCatalog.Disposition.EXCLUDED_SERVICE&&row.disposition()!=LbcTmtCatalog.Disposition.EXCLUDED_INERT_KEY),
                        "registered chemical prototype has no false native-default pending finding: "+itemId);
                chemicals.add(property);chemical++;}
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
        AbstractThrownPotion launch(LbcTmtAbilities.Ability property,InteractionHand hand){return launch(property,hand,false);}
        AbstractThrownPotion launch(LbcTmtAbilities.Ability property,InteractionHand hand,boolean legalChemical){
            var expected=source(property);var stack=carrier(0,legalChemical?1:2,property,dose(LbcMana.MANA_POTION,17,1));var original=stack.get(DataComponents.CUSTOM_DATA);
            if(legalChemical){require(stack.getCount()==stack.getMaxStackSize(),"new chemical input uses one legal carrot carrier, no max-stack override");
                stack.set(DataComponents.CUSTOM_NAME,Component.literal("independent chemical carrier"));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("retained private sibling snapshot"))));
                stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3))));
                var snapshot=stack.copy();var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                require(ItemStack.matches(snapshot,ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,snapshot).getOrThrow()).getOrThrow())&&LbcTmtAbilities.read(snapshot).size()==2,"complete legal input/sibling snapshot persists before genuine consumption");}
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);f.owner.setItemInHand(hand,stack);
            var before=new HashSet<AbstractThrownPotion>(f.level.getEntitiesOfClass(AbstractThrownPotion.class,f.owner.getBoundingBox().inflate(20)));
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,hand).consumesAction(),"actual native used-hand entry accepts selected potion factory "+hand);
            var created=f.level.getEntitiesOfClass(AbstractThrownPotion.class,f.owner.getBoundingBox().inflate(20),actor->!before.contains(actor));
            require(created.size()==1&&(legalChemical?f.owner.getItemInHand(hand).isEmpty():stack.getCount()==1&&f.owner.getItemInHand(hand)==stack),"one real selected native potion and one exact physical debit; legal chemical carrier is exhausted");
            var shot=created.getFirst();f.own(shot);var payload=shot.getItem();
            require(shot.getOwner()==f.owner&&payload.getItem()==expected.getItem()&&payload.get(DataComponents.POTION_CONTENTS).equals(expected.get(DataComponents.POTION_CONTENTS))
                    &&java.util.Objects.equals(payload.get(DataComponents.POTION_DURATION_SCALE),expected.get(DataComponents.POTION_DURATION_SCALE)),"native projectile retains complete private source type, dose, flags/color/scale and caster");
            require(!ItemData.contains(payload,LbcTmtAbilities.SLOTS)&&!ItemData.contains(payload,LbcTmtAbilities.ACTIVE)&&!LbcTmtItems.matches(payload),"producer snapshot has no borrowed outer TMT/sibling state");
            var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);var decoded=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,payload).getOrThrow()).getOrThrow();
            require(ItemStack.matches(payload,decoded)&&!LbcTmtAbilities.nativePredicatesActive()
                    &&(legalChemical||stack.get(DataComponents.CUSTOM_DATA)==original&&LbcTmtAbilities.read(stack).size()==2),"native source survives real codec roundtrip; retained traits or legally exhausted source leave no active scope");
            if(legalChemical)require(!payload.has(DataComponents.CONTAINER)&&!payload.getHoverName().getString().equals("independent chemical carrier")
                    &&ItemData.intValue(payload,"potion",0)==ItemData.intValue(expected,"potion",0),"native chemical projectile excludes independent outer components and mana-potion sibling");return shot;
        }
        void splash(InteractionHand hand){var target=cow();var shot=launch(dose(LbcMana.MANA_SPLASH_POTION,18,3600),hand);
            require(shot instanceof ThrownSplashPotion,"real splash source factory class");shot.setPos(target.position());
            ((ThrownSplashPotion)shot).onHitAsPotion(f.level,shot.getItem(),new EntityHitResult(target));
            require(target.hasEffect(MobEffects.RAID_OMEN)&&target.getEffect(MobEffects.RAID_OMEN).getAmplifier()==18&&target.getEffect(MobEffects.RAID_OMEN).getDuration()==3600,"native direct-hit splash applies exact source duration before ordinary mana conversion");
            f.discard(shot);game.server().getScoreboard().resetAllPlayerScores(target);f.discard(target);
        }
        @SuppressWarnings("unchecked") List<Packet<?>> packets(){return (List<Packet<?>>)WoolBucketChecks.read(f.client.connection,"packets");}
        @SuppressWarnings("unchecked") Map<AreaEffectCloud,?> chemicalStates(){return (Map<AreaEffectCloud,?>)WoolBucketChecks.read(game.items().clouds(),"clouds");}
        void chemicalBegin(){
            if(witness!=null){game.server().getScoreboard().resetAllPlayerScores(witness);f.discard(witness);witness=null;}if(cloud!=null){f.discard(cloud);cloud=null;}
            chemicalOrigin=new Vec3(f.home.x,f.level.getMaxY()+24,f.home.z);
            // Both real packet audiences stay within32, but outside all chemical target/blast radii.
            f.move(f.owner,chemicalOrigin.add(-16,0,0));f.move(f.other,chemicalOrigin.add(16,0,0));chemicalThrow();
        }
        void chemicalThrow(){
            var property=chemicals.get(chemicalIndex/2);var expected=source(property);int kind=234+chemicalIndex/2;
            var hand=chemicalIndex%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
            var shot=launch(property,hand,true);require(shot instanceof ThrownLingeringPotion,"actual ChemicalPotionItem native lingering factory kind="+kind+" hand="+hand);
            shot.setPos(chemicalOrigin);var prior=new HashSet<>(f.level.getEntitiesOfClass(AreaEffectCloud.class,shot.getBoundingBox().inflate(4)));
            ((ThrownLingeringPotion)shot).onHitAsPotion(f.level,shot.getItem(),new BlockHitResult(chemicalOrigin,Direction.UP,BlockPos.containing(chemicalOrigin),false));
            var created=f.level.getEntitiesOfClass(AreaEffectCloud.class,shot.getBoundingBox().inflate(4),actor->!prior.contains(actor));
            require(created.size()==1,"real native chemical impact emits exactly one cloud");cloud=created.getFirst();f.own(cloud);
            require(cloud.getOwner()==f.owner&&cloud.get(DataComponents.POTION_CONTENTS).equals(expected.get(DataComponents.POTION_CONTENTS))
                    &&java.util.Objects.equals(cloud.get(DataComponents.POTION_DURATION_SCALE),expected.get(DataComponents.POTION_DURATION_SCALE))
                    &&LbcItems.chemicalAmplifier(cloud.get(DataComponents.POTION_CONTENTS))==kind,"native cloud preserves exact source color/scale/amplifier/duration and real caster");
            require(!cloud.isRemoved()&&chemicalStates().containsKey(cloud),"real ENTITY_LOAD admits native chemical into existing LbcClouds owner");
            chemicalState=chemicalStates().get(cloud);require(((Integer)WoolBucketChecks.read(chemicalState,"kind"))==kind,"actual native owner selected exact chemical kind");
            chemicalLife=(Integer)WoolBucketChecks.read(chemicalState,"lifetime");require(chemicalLife==(kind==234?100:0)+cloud.tickCount,"native chemical initial lifetime is source-owned");
            f.discard(shot);
            if(kind==236){witness=cow();witness.setPos(chemicalOrigin.add(1,0,0));}
            particleStart=packets().size();next(4);
        }
        void chemicalQuantum(){int kind=234+chemicalIndex/2;
            require(((Integer)WoolBucketChecks.read(chemicalState,"lifetime"))==chemicalLife+1,"exactly one real LbcItems/LbcClouds scheduler quantum, no manual tick kind="+kind);
            if(kind==234||kind==235){var type=kind==234?ParticleTypes.FLAME:ParticleTypes.SNEEZE;int count=kind==234?5:10;
                require(packets().subList(particleStart,packets().size()).stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                        .anyMatch(packet->packet.particle().getType()==type&&packet.count()==count&&packet.x()==chemicalOrigin.x&&packet.z()==chemicalOrigin.z),"existing chemical branch emits its actual first-quantum native particle feedback kind="+kind);}
            if(kind==236)for(var effect:List.of(MobEffects.POISON,MobEffects.WITHER,MobEffects.WEAKNESS,MobEffects.SLOWNESS))
                require(witness.hasEffect(effect)&&witness.getEffect(effect).getDuration()==200&&witness.getEffect(effect).getAmplifier()==0,"real phosphine owner applies exact native200tick effects to isolated above-build-height living witness");
            if(kind==237)require(cloud.isRemoved(),"actual fulminate owner consumes native cloud in its first protected blast quantum");
            else require(!cloud.isRemoved()&&chemicalStates().get(cloud)==chemicalState,"existing native chemical state remains canonical after real quantum");
            if(witness!=null){game.server().getScoreboard().resetAllPlayerScores(witness);f.discard(witness);witness=null;}
            if(cloud!=null){retiredChemical=cloud;f.discard(cloud);cloud=null;}cleanupChemicalChildren();
        }
        void cleanupChemicalChildren(){if(chemicalOrigin==null)return;
            // Sulfur's authored3% branch may create real native acid during this same quantum.
            var actors=new ArrayList<net.minecraft.world.entity.Entity>();for(var actor:f.level.getAllEntities())
                if(!initialActors.contains(actor.getUUID())&&actor instanceof LbcClouds.AcidProjectile acid&&acid.getOwner()==f.owner&&actor.position().distanceToSqr(chemicalOrigin)<32*32)actors.add(actor);
            for(var actor:actors){f.own(actor);f.discard(actor);}
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
                require(timer>=850&&timer<=900,"ordinary native cloud scheduler delivers source quarter-duration potion to real generic LivingEntity");chemicalBegin();return false;}
            if(phase==4){if(elapsed<1)return false;require(elapsed==1,"observe only the first real native chemical quantum");chemicalQuantum();chemicalIndex++;next(5);return false;}
            // discard at END marks the body immediately; native ENTITY_UNLOAD runs next world frame.
            if(phase==5){if(elapsed<1)return false;require(!chemicalStates().containsKey(retiredChemical),"native entity removal releases the chemical owner index without a manual unload call");retiredChemical=null;if(chemicalIndex==8)return true;chemicalThrow();return false;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.items().mana().clearEffects(f.owner);if(witness!=null){game.server().getScoreboard().resetAllPlayerScores(witness);f.discard(witness);}if(cloud!=null)f.discard(cloud);cleanupChemicalChildren();}
            finally{try{f.close();}finally{stats.close();worldRandom.restore(f.level.getRandom());}}}
    }
}
