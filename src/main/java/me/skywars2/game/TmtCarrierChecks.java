package me.skywars2.game;

import java.util.HashSet;
import java.util.List;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.ScoreHolder;

/** Native registered transports, real input/debit/food and player-tick fanout guard. */
public final class TmtCarrierChecks {
    private TmtCarrierChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT carriers: "+why);}
    private static List<LbcTmtAbilities.Ability> draws(){
        var fire=LbcWands.tmtAbilities().stream().filter(p->p.nativeItem().toString().equals("skywars2:fire_wand")).findFirst().orElseThrow().inMode(0);
        var tiny=LbcWands.tmtAbilities().stream().filter(p->p.nativeItem().toString().equals("skywars2:tiny_fire_wand")).findFirst().orElseThrow().inMode(0);
        var feed=LbcTmtSelfAbilities.catalog().stream().filter(p->p.id().equals("self/feeding")).findFirst().orElseThrow().ability();
        return List.of(fire,tiny,feed,fire,tiny,feed,fire);
    }
    private static ItemStack nativeItem(int index){
        var result=LbcTmtItems.prototype(index);var data=result.get(DataComponents.CUSTOM_DATA).copyTag();
        LbcTmtAbilities.write(data,draws());result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return result;
    }
    private static long seed(int carrier){for(long n=0;n<1000;n++)if(RandomSource.create(n).nextInt(5)==carrier)return n;throw new AssertionError("base RNG domain");}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var owner=f.owner;var runtime=game.items().tmt();var mana=game.items().mana();
            var expected=List.of(Items.CARROT_ON_A_STICK,Items.BOW,Items.SNOWBALL,Items.CROSSBOW,Items.GOLDEN_APPLE);
            var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            var fireCost=mana.score(ScoreHolder.forNameOnly("stfire_wand_mana_use"),"lbc.math");
            var tinyCost=mana.score(ScoreHolder.forNameOnly("tiny_fire_wand_mana_use"),"lbc.math");int oldFire=fireCost.get(),oldTiny=tinyCost.get();
            var otherStat=Stats.ITEM_USED.get(LbcTmtItems.prototype(0).getItem());int otherUsed=f.other.getStats().getValue(otherStat);
            var before=new HashSet<java.util.UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            try{
                for(int i=0;i<5;i++){
                    var generated=runtime.generate(RandomSource.create(seed(i)),draws());
                    require(generated.is(LbcTmtItems.prototype(i).getItem()),"all five registered transport choices");
                    require(((PolymerItem)generated.getItem()).getPolymerItem(generated,PacketContext.get())==expected.get(i),"native client base agrees with server input family");
                    require(generated.getCount()==1&&LbcTmtAbilities.read(generated).size()==3,"one generated item and last-draw duplicate family semantics");
                    require(generated.get(DataComponents.ITEM_MODEL).toString().equals("lbc:tmt/procedural")
                            &&(generated.get(DataComponents.CUSTOM_MODEL_DATA).colors().getFirst()>>>16)==250,"same explicit procedural atlas transport for all carriers");
                    var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,generated).getOrThrow()).getOrThrow();
                    require(ItemStack.matches(restored,generated),"registered transport, parameters and visual seed survive real item codec");
                    var stat=Stats.ITEM_USED.get(generated.getItem());f.usedStats.put(stat,owner.getStats().getValue(stat));
                }
                fireCost.set(100);tinyCost.set(25);mana.maximum(owner,10000);mana.set(owner,1000);mana.score(owner,"food_count").set(0);
                var carrot=nativeItem(0);owner.setItemInHand(InteractionHand.MAIN_HAND,carrot);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                require(owner.gameMode.useItem(owner,f.level,carrot,InteractionHand.MAIN_HAND).consumesAction(),"actual registered carrot input reaches TMT coordinator");
                require(mana.get(owner)==875&&mana.score(owner,"food_count").get()==1,"native three-family fanout pays each spell once and feeds once");
                owner.gameMode.useItem(owner,f.level,carrot,InteractionHand.MAIN_HAND);
                require(mana.get(owner)==875&&mana.score(owner,"food_count").get()==1,"same player/tick cannot repeat the ability fanout");
                long fireballs=0;for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())){f.own(e);if(e instanceof SmallFireball)fireballs++;}
                require(fireballs==4,"actual native Fire3 plus Tiny1 and no duplicate second input");

                var other=nativeItem(0);f.other.setItemInHand(InteractionHand.MAIN_HAND,other);mana.maximum(f.other,10000);mana.set(f.other,1000);
                require(f.other.hurtServer(f.level,f.other.damageSources().generic(),1),"actual positive native incoming damage");
                f.other.gameMode.useItem(f.other,f.level,other,InteractionHand.MAIN_HAND);
                require(mana.get(f.other)==1000,"native damaged continuation marks guard without activating abilities");

                var snow=nativeItem(2);owner.setItemInHand(InteractionHand.MAIN_HAND,snow);
                require(owner.gameMode.useItem(owner,f.level,snow,InteractionHand.MAIN_HAND).consumesAction()&&snow.isEmpty(),"native snowball debit remains separate from once-per-tick ability guard");
                Snowball ball=null;for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID())){f.own(e);if(e instanceof Snowball b)ball=b;}
                require(ball!=null&&ball.getOwner()==owner&&LbcTmtItems.matches(ball.getItem()),"real vanilla Snowball retains TMT payload and actual owner");

                var bow=nativeItem(1);owner.setItemInHand(InteractionHand.MAIN_HAND,bow);owner.getInventory().setItem(9,new ItemStack(Items.ARROW));
                require(owner.gameMode.useItem(owner,f.level,bow,InteractionHand.MAIN_HAND).consumesAction()&&owner.getUseItem()==bow,"registered BowItem uses real native charging state");owner.stopUsingItem();
                var apple=runtime.generate(RandomSource.create(seed(4)),draws());owner.getFoodData().setFoodLevel(2);
                int foodCount=mana.score(owner,"food_count").get();
                apple.finishUsingItem(f.level,owner);
                require(apple.isEmpty()&&owner.getFoodData().getFoodLevel()==20,"native consumable spends one apple and applies authored20 nutrition");
                require(mana.score(owner,"food_count").get()==foodCount+1,"native TMT apple counts as one consumed food");
                var infinite=LbcTmtItems.prototype(4);var infiniteData=infinite.get(DataComponents.CUSTOM_DATA).copyTag();
                var foodAbilities=new java.util.ArrayList<LbcTmtAbilities.Ability>();
                for(var flag:List.of("inf","armorattribute","attack_damageattribute")){
                    var payload=new CompoundTag();payload.putInt(flag,1);
                    foodAbilities.add(new LbcTmtAbilities.Ability("food/"+flag,CustomData.of(payload)));
                }
                LbcTmtAbilities.write(infiniteData,foodAbilities);infinite.set(DataComponents.CUSTOM_DATA,CustomData.of(infiniteData));
                owner.setItemInHand(InteractionHand.MAIN_HAND,infinite);owner.getFoodData().setFoodLevel(2);
                var armor=owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                var damage=owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                double originalArmor=armor.getBaseValue(),originalDamage=damage.getBaseValue();
                int beforeInfinite=mana.score(owner,"food_count").get();
                try{
                    var replacement=infinite.finishUsingItem(f.level,owner);
                    require(replacement.is(LbcTmtItems.prototype(4).getItem())&&replacement.getCount()==1
                            &&LbcTmtAbilities.read(replacement).size()==3,"infinite food restores the same TMT carrier and all private properties after native debit");
                    require(armor.getBaseValue()==originalArmor+2&&damage.getBaseValue()==originalDamage+1,"both actual native attribute-food owners apply once");
                    require(mana.score(owner,"food_count").get()==beforeInfinite+1,"three food properties still count as one meal");
                }finally{armor.setBaseValue(originalArmor);damage.setBaseValue(originalDamage);}
            }finally{
                owner.stopUsingItem();fireCost.set(oldFire);tinyCost.set(oldTiny);
                f.other.getStats().setValue(f.other,otherStat,otherUsed);
                for(var e:f.level.getAllEntities())if(!before.contains(e.getUUID()))f.own(e);
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-carriers (five registered Polymer/native transports, procedural seed and codecs; actual carrot spells, native damaged tick guard, snowball debit, bow use and20-nutrition consumption)");
    }
}
