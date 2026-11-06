package me.skywars2.game;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Native predicate identity stays local to the callback; the real transport is never replaced. */
public final class TmtNativeUseChecks {
    private TmtNativeUseChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT native use: "+why);}
    private static ItemStack item(List<LbcTmtAbilities.Ability> abilities){
        var result=LbcTmtItems.prototype(0);var data=result.get(DataComponents.CUSTOM_DATA).copyTag();
        LbcTmtAbilities.write(data,abilities);result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return result;
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var aliases=new CompoundTag();aliases.putString(LbcTmtAbilities.NATIVE,"skywars2:imba_axe");
            var stack=item(List.of(new LbcTmtAbilities.Ability("native/identity-witness",CustomData.of(aliases))));
            var original=stack.get(DataComponents.CUSTOM_DATA);var transport=stack.getItem();
            var deferred=new java.util.ArrayList<Integer>();
            var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            require(LbcTmtAbilities.invoke(stack,"native/identity-witness",()->{
                require(stack.is(LbcImbaAxeItem.TOKEN),"identity-only native owner recognizes the selected property");
                var holder=LbcImbaAxeItem.TOKEN.builtInRegistryHolder();
                require(stack.is(holder)&&stack.is(holder.key())&&stack.is(net.minecraft.core.HolderSet.direct(holder)),"inherited holder/key/set predicates agree with the selected native property");
                require(stack.is(net.minecraft.tags.ItemTags.AXES)==new ItemStack(LbcImbaAxeItem.TOKEN).is(net.minecraft.tags.ItemTags.AXES),"inherited tag predicate uses the selected native prototype");
                require(stack.getItem()==transport,"actual item type and native client transport remain unchanged");
                var copy=stack.copy();require(copy.is(LbcImbaAxeItem.TOKEN)&&copy.getItem()==transport,"captured source copies have the same temporary predicate context");
                var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,copy).getOrThrow()).getOrThrow();
                require(restored.getItem()==transport&&restored.is(LbcImbaAxeItem.TOKEN),"real codecs retain transport and selected source context");
                LbcTmtAbilities.afterScopes(()->{
                    require(!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"accepted continuation runs after full native state restoration");
                    require(stack.get(DataComponents.CUSTOM_DATA).equals(original),"continuation sees restored outer metadata");
                    deferred.add(1);
                });
                require(deferred.isEmpty(),"native continuation stays deferred during the selected callback");
                return true;
            }),"native identity callback ran");
            require(deferred.equals(List.of(1)),"accepted continuation executes exactly once before returning to native caller");
            require(stack.getItem()==transport&&stack.is(transport)&&!stack.is(LbcImbaAxeItem.TOKEN)
                    &&stack.get(DataComponents.CUSTOM_DATA).equals(original)&&!LbcTmtAbilities.nativePredicatesActive(),"full restoration releases predicate scope and outer metadata");
            var failure=new IllegalStateException("deliberate native identity failure");
            try{LbcTmtAbilities.invoke(stack,"native/identity-witness",()->{throw failure;});throw new AssertionError("failure swallowed");}
            catch(IllegalStateException actual){require(actual==failure,"native failure identity retained");}
            require(!LbcTmtAbilities.nativePredicatesActive()&&!stack.is(LbcImbaAxeItem.TOKEN),"exception releases native predicate context");

            var use=game.items().tmt().useProfiles();
            require(use.families().size()>40&&use.domains().canonicalPrototypes()>800,"current native/default and all canonical typed input domains are loaded");
            var bob=use.families().stream().flatMap(row->row.variants().stream())
                    .filter(a->((ItemData.IntReader)(Object)a.data()).skywars2$readInt("big_bob",0)==1).findFirst().orElseThrow();
            var nativeInput=item(List.of(bob));f.owner.setItemInHand(InteractionHand.MAIN_HAND,nativeInput);
            var before=new java.util.HashSet<java.util.UUID>();for(var entity:f.level.getAllEntities())before.add(entity.getUUID());
            try{
                require(f.owner.gameMode.useItem(f.owner,f.level,nativeInput,InteractionHand.MAIN_HAND).consumesAction(),"real registered input reaches the complete shared native-use route");
                int nuts=0;
                for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID())){
                    f.own(entity);if(entity instanceof ItemEntity drop&&LbcOreshek.matches(drop.getItem()))nuts+=drop.getItem().getCount();
                }
                require(nuts==10&&f.owner.getMainHandItem()==nativeInput&&nativeInput.getCount()==1,"native Big Bob creates exact10 nuts without replacing/debiting the TMT carrier");
                require(LbcTmtAbilities.read(nativeInput).size()==1&&!LbcTmtAbilities.nativePredicatesActive(),"native owner returns with complete private state and identity cleanup");
                var bucket=use.families().stream().flatMap(row->row.variants().stream())
                        .filter(a->((ItemData.IntReader)(Object)a.data()).skywars2$readInt("bucket",0)==1).findFirst().orElseThrow();
                var thrown=item(List.of(bucket));f.other.setItemInHand(InteractionHand.MAIN_HAND,thrown);
                var usedStat=net.minecraft.stats.Stats.ITEM_USED.get(thrown.getItem());int usedBefore=f.other.getStats().getValue(usedStat);
                try{
                    require(f.other.gameMode.useItem(f.other,f.level,thrown,InteractionHand.MAIN_HAND).consumesAction()&&thrown.isEmpty(),"existing ordinary bucket input owns the sole TMT debit");
                    long buckets=0;
                    for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID())&&entity instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball shot
                            &&shot.getOwner()==f.other&&ItemData.intValue(shot.getItem(),"bucket",0)==1)buckets++;
                    require(buckets==1,"one real native throwable keeps its selected payload and actual owner");
                }finally{f.other.getStats().setValue(f.other,usedStat,usedBefore);}
                var meleeSlots=LbcTmtSelfAbilities.catalog().stream().filter(row->List.of("self/feeding","self/vampire",
                        "self/triple_golden_sword","self/blazefire_sword").contains(row.id())).map(LbcTmtSelfAbilities.Profile::ability).toList();
                var melee=item(meleeSlots);f.owner.setItemInHand(InteractionHand.MAIN_HAND,melee);
                var mana=game.items().mana();mana.score(f.owner,"food_count").set(0);
                mana.score(f.owner,"swordcharged8").set(0);mana.score(f.owner,"swordcharged9").set(0);
                f.move(f.other,f.owner.position().add(0,0,1));f.other.setInvulnerableTime(0);f.other.damageCooldownTime=0;
                float victimHealth=f.other.getHealth();f.owner.resetAttackStrengthTicker();
                f.owner.attack(f.other);
                require(f.other.getHealth()<victimHealth,"real native player melee hit is accepted");
                require(mana.score(f.owner,"food_count").get()==1&&f.owner.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"one accepted hit dispatches both private classic melee properties once");
                require(game.items().chargedSwords().tripleCharges(f.owner)==1&&game.items().chargedSwords().blazeCharges(f.owner)==1,"one accepted hit independently increments both private sword charge owners");
                require(f.owner.getMainHandItem()==melee&&LbcTmtAbilities.read(melee).size()==4&&!LbcTmtAbilities.active(melee),"melee restores the real held stack and all four private properties");
            }finally{for(var entity:f.level.getAllEntities())if(!before.contains(entity.getUUID()))f.own(entity);}
            me.skywars2.SkyWars2.LOGGER.info("[check] TMT native use domain families={} variants={} native={} canonical={} rejected={}",
                    use.families().size(),use.domains().variants(),use.domains().registeredItems(),use.domains().canonicalPrototypes(),use.domains().unrecognizedItems().size());
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-native-use (all canonical/native default input domains, real shared dispatcher; scoped identity-only predicate, unchanged carrier/codecs/copies, native Big Bob input and exception cleanup)");
    }
}
