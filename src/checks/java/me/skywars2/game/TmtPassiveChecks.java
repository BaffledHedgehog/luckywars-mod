package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Native passive owners observe the actual combined held/equipped item on ordinary server ticks. */
public final class TmtPassiveChecks {
    private TmtPassiveChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT passives: "+why);}
    static void run(GameController game){require(running==null,"exclusive native passive scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    static void tick(GameController game){var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
        SkyWars2.LOGGER.info("[check] PASS tmt-passives (real shared One Punch HP/absorption refresh with mixed Hylian aura and retained attributes/slots, native shard and neutral-ball exchange, mixed Mage/Overload equipment masks, Clown effect prevention and Rocket boots classifier; no substitute passive scheduler)");
    }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-passives",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        int phase,when,started;boolean closed;ItemStack held;CustomData heldData;Zombie witness;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            require(game.items().frost().cold(f.owner)==0&&!game.items().frost().isFrozen(f.owner),"no pre-existing owner frost state");}
        LbcTmtAbilities.Ability property(String name){return game.items().tmt().catalog().families().stream().filter(row->row.id().equals("native/"+name)).findFirst().orElseThrow().variants().getFirst();}
        ItemStack mixed(String... names){var stack=LbcTmtItems.prototype(0);var values=new ArrayList<LbcTmtAbilities.Ability>();for(var name:names)values.add(property(name));
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,values);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();game.items().mana().baseRegen(f.owner,0);
            held=mixed("one_punch","hylian");heldData=held.get(DataComponents.CUSTOM_DATA);f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);f.owner.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(1000);f.owner.setHealth(37.25f);f.owner.setAbsorptionAmount(4.5f);
            require(f.owner.getHealth()==37.25f&&f.owner.getAbsorptionAmount()==4.5f,"actual native HP/absorption positive control");
            witness=EntityTypes.ZOMBIE.create(f.level,EntitySpawnReason.COMMAND);require(witness!=null,"native monster witness");witness.setNoAi(true);witness.setNoGravity(true);witness.setSilent(true);witness.setPos(f.owner.position().add(3,0,0));require(f.level.addFreshEntity(witness),"native witness registered");f.own(witness);next(0);}
        double damage(){return held.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().filter(entry->entry.attribute().equals(Attributes.ATTACK_DAMAGE)&&entry.modifier().id().equals(Identifier.withDefaultNamespace("base_attack_damage"))).findFirst().orElseThrow().modifier().amount();}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<100,"bounded native passive phases "+phase);int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<6)return false;require(damage()==42&&f.owner.hasEffect(MobEffects.STRENGTH)&&f.owner.getEffect(MobEffects.STRENGTH).getAmplifier()==2,"native One Punch and Hylian outcomes on one held item: damage="+damage()+", HP="+f.owner.getHealth()+", absorption="+f.owner.getAbsorptionAmount()+", strength="+f.owner.getEffect(MobEffects.STRENGTH));
                require(held.get(DataComponents.CUSTOM_DATA)==heldData&&LbcTmtAbilities.read(held).size()==2,"held refresh keeps both private properties and immutable custom data");
                f.owner.setHealth(20);f.owner.setAbsorptionAmount(1);next(1);return false;}
            if(phase==1){if(elapsed<2)return false;require(damage()==21,"native health refresh follows changed HP and absorption");f.discard(witness);f.owner.setItemInHand(InteractionHand.MAIN_HAND,mixed("runic_ice_shard","neutral_ball"));
                game.items().mana().maximum(f.owner,20000);game.items().mana().set(f.owner,5000);game.items().mana().baseRegen(f.owner,0);game.items().mana().inventoryChanged(f.owner);next(2);return false;}
            if(phase==2){if(game.items().frost().cold(f.owner)==0){require(elapsed<12,"native ten-tick shard pulse");return false;}
                require(game.items().frost().cold(f.owner)>=2&&game.items().mana().baseRegen(f.owner)==1&&game.items().mana().get(f.owner)<100,"selected-container plus hand shard pulse and native neutral-ball exchange compose");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);game.items().frost().clear(f.owner);
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                    var stack=slot==EquipmentSlot.HEAD?mixed("mage","overload","clown_mask"):slot==EquipmentSlot.FEET?mixed("mage","overload","explosive_feets"):mixed("mage","overload");f.owner.setItemSlot(slot,stack);
                }
                game.items().mana().inventoryChanged(f.owner);next(3);return false;}
            if(phase==3){if(elapsed<2)return false;require(game.items().mana().mageFullSet(f.owner)&&game.items().mana().overloadArmor(f.owner)==15,"existing mana equipment cache sees both complete native sets");
                require(LbcItems.isRocketBoots(f.owner.getItemBySlot(EquipmentSlot.FEET)),"actual native rocket-boot eligibility");
                require(LbcClownKit.mask(f.owner),"actual native head-slot mask eligibility");f.owner.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100));f.owner.addEffect(new MobEffectInstance(MobEffects.DARKNESS,100));
                require(!f.owner.hasEffect(MobEffects.BLINDNESS)&&!f.owner.hasEffect(MobEffects.DARKNESS),"native effect-change handler prevents both mask debuffs");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.items().frost().clear(f.owner);if(witness!=null)f.discard(witness);}finally{f.close();}}
    }
}
