package me.skywars2.game;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Actual shared self/jump owners, with independent siblings and no fabricated accepted hit. */
public final class TmtSelfChecks {
    private TmtSelfChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT self: "+why);}
    private static LbcTmtAbilities.Ability ability(String id,int type){
        return LbcTmtSelfAbilities.catalog().stream().filter(p->p.id().equals(id)
                &&p.flags().copyTag().getIntOr("type",0)==type).findFirst().orElseThrow().ability();
    }
    private static ItemStack item(List<LbcTmtAbilities.Ability> abilities){
        var item=new ItemStack(Items.CARROT_ON_A_STICK);var data=new CompoundTag();data.putInt("tmt_item",1);data.putInt("untouched",73);
        LbcTmtAbilities.write(data,abilities);item.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return item;
    }
    private static double damage(ItemStack item){
        return item.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().getFirst().modifier().amount();
    }
    private static void metadata(){
        require(LbcTmtSelfAbilities.catalog().size()==12,"six self and six current/legacy jump profiles");
        for(var profile:LbcTmtSelfAbilities.catalog()){
            var item=item(List.of(profile.ability()));var data=LbcFartus.data(item);
            require(LbcTmtAbilities.read(item).size()==1,"every declared profile is a real persistent property");
            if(profile.group()==LbcTmtSelfAbilities.Group.JUMP){
                int bit=1<<profile.flags().copyTag().getIntOr("type",0);
                int expected=profile.id().equals("jump/mystical_old")?bit<<4:bit;
                require(LbcTmtSelfAbilities.jumpMasks(data)==expected,"all six native inventory outcomes remain reachable");
            }
        }
        var mixed=item(List.of(ability("self/feeding",0),ability("self/vampire",0),ability("jump/mystical",1),ability("jump/mystical_old",3)));
        var original=mixed.get(DataComponents.CUSTOM_DATA);
        require(LbcTmtAbilities.invoke(mixed,"self/feeding",()->{
            require(ItemData.intValue(mixed,"feeding",0)==1&&!ItemData.contains(mixed,"vampire")
                    &&!ItemData.contains(mixed,"mystical")&&!ItemData.contains(mixed,"mystical_old"),"sibling classifiers cannot impersonate the selected native property");
            require(ItemData.intValue(mixed,"untouched",0)==73,"unrelated item data remains visible");return true;
        }),"selected native property executes");
        require(mixed.get(DataComponents.CUSTOM_DATA).equals(original),"outer aggregate classifiers restored exactly");
        var repeated=item(List.of(ability("jump/mystical",1),ability("jump/mystical",3),ability("jump/mystical_old",2)));
        require(LbcTmtSelfAbilities.jumpMasks(LbcFartus.data(repeated))==(8|(4<<4)),"last type in a family wins; the other family keeps its own type");
    }

    public static void run(GameController game){
        metadata();
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var owner=f.owner;var other=f.other;var items=game.items();
            var scores=items.mana();boolean swimming=owner.isSwimming();
            try{
                var self=item(List.of(ability("self/blazefire_sword",0),ability("self/vampire",0),ability("self/triple_golden_sword",0),
                        ability("self/feeding",0),ability("self/evolution",0),ability("self/seaguardian",0)));
                self.set(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Identifier.parse("minecraft:base_attack_damage"),1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build());
                owner.setItemInHand(InteractionHand.MAIN_HAND,self);owner.setSwimming(true);
                scores.score(owner,"food_count").set(0);scores.score(owner,"swordcharged8").set(0);scores.score(owner,"swordcharged9").set(0);
                float health=other.getHealth();var before=self.get(DataComponents.CUSTOM_DATA);
                require(LbcTmtSelfAbilities.swords(game,owner)==6,"all six private source self branches dispatch once");
                require(scores.score(owner,"food_count").get()==1&&items.chargedSwords().tripleCharges(owner)==1
                        &&items.chargedSwords().blazeCharges(owner)==1,"feeding and each shared sword counter update exactly once");
                require(Math.abs(damage(self)-1.02)<.000001,"Evolution uses the real modifier growth once");
                require(owner.getEffect(MobEffects.REGENERATION).getAmplifier()==4&&owner.getEffect(MobEffects.WATER_BREATHING).getAmplifier()==2,"native Vampire and deterministic swimming Guardian self effects");
                require(other.getHealth()==health,"no victim damage is invented for the source self group");
                require(owner.getMainHandItem()==self&&self.get(DataComponents.CUSTOM_DATA).equals(before),"actual held identity and aggregate metadata preserved");
                require(items.chargedSwords().count()==0&&!items.chargedSwords().activeBlaze(owner),"charging is not a fabricated projectile shot");

                owner.removeAllEffects();other.removeAllEffects();owner.setSwimming(false);
                f.field(false,owner.position().add(6,0,0));require(items.magic().suppressed(owner),"native MAG positive control");
                LbcTmtSelfAbilities.swords(game,owner);
                require(scores.score(owner,"food_count").get()==2&&!owner.hasEffect(MobEffects.REGENERATION)
                        &&items.chargedSwords().tripleCharges(owner)==1&&items.chargedSwords().blazeCharges(owner)==1,"native feeding bypass and magic-dependent self/counter suppression retained");
                require(damage(self)>1.02,"MAG cannot replace Evolution's native TECH policy");f.removeFields();
                double grown=damage(self);f.field(true,owner.position().add(6,0,0));require(items.magic().mechanismSuppressed(owner),"native TECH positive control");
                LbcTmtSelfAbilities.swords(game,owner);require(damage(self)==grown,"TECH blocks Evolution through its existing owner");f.removeFields();
                owner.removeAllEffects();other.removeAllEffects();
                f.pause();int food=scores.score(owner,"food_count").get();
                require(LbcTmtSelfAbilities.swords(game,owner)==0&&scores.score(owner,"food_count").get()==food,"stopped input cannot enter source self effects");f.thaw();
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));owner.setItemInHand(InteractionHand.OFF_HAND,self);
                require(LbcTmtSelfAbilities.swords(game,owner)==0,"source SelectedItem does not silently substitute offhand TMT");

                var jump=item(List.of(ability("jump/mystical",1),ability("jump/mystical_old",1)));
                owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.getInventory().setItem(10,jump);owner.getInventory().setItem(11,jump.copy());
                var ops=f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,jump).getOrThrow()).getOrThrow();
                require(ItemStack.matches(jump,restored)&&LbcTmtSelfAbilities.jumpMasks(LbcFartus.data(restored))==34,"real save/load retains separate current/legacy jump types");
                var motion=owner.getDeltaMovement();LbcTmtSelfAbilities.armor(game,owner);
                require(owner.getEffect(MobEffects.SPEED).getAmplifier()==5&&owner.getEffect(MobEffects.SLOW_FALLING).getAmplifier()==3,"actual ordinary jump owner reads both private inventory properties");
                require(owner.getDeltaMovement().equals(motion),"source armor group does not manufacture a jump impulse");
                owner.removeAllEffects();other.removeAllEffects();f.field(false,owner.position().add(6,0,0));LbcTmtSelfAbilities.armor(game,owner);
                require(!owner.hasEffect(MobEffects.SPEED)&&owner.hasEffect(MobEffects.SLOW_FALLING),"current and legacy jump retain independent native MAG policies");f.removeFields();
                owner.removeAllEffects();f.pause();LbcTmtSelfAbilities.armor(game,owner);
                require(!owner.hasEffect(MobEffects.SLOW_FALLING),"source armor helper observes TimeStop admission");f.thaw();
            }finally{owner.setSwimming(swimming);}
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-self (12 source self/jump profiles; sibling classifier isolation, actual shared self/counter/armor owners, no invented hit/jump/shot, native MAG/TECH/TimeStop and codec restoration)");
    }
}
