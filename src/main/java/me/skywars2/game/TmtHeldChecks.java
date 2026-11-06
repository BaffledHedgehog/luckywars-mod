package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Real native held/using ticks, location changes and full checkpoint of mixed Kiuas/Sun latches. */
public final class TmtHeldChecks {
    private TmtHeldChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT held: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scheduled scene");var next=new Scene(game);running=next;
        try{next.begin();}catch(Throwable error){running=null;try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var next=running;if(next==null)return;
        try{require(next.game==game,"same native controller");if(!next.step())return;running=null;next.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-held (seven native held owners, real inventory unequip, independent Kiuas/Sun upkeep and whole coordinator replay, all five native carriers Holy/Nahida sustained input/blocking/pulses/stop, component-owned parse cache)");
        }catch(Throwable error){running=null;try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-held",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final HashSet<UUID> initial=new HashSet<>();
        final int began;int phase,when,carrier;boolean restoring,closed;
        Skeleton summon;Zombie unholy;ItemStack current;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);began=game.server().getTickCount();for(var e:f.level.getAllEntities())initial.add(e.getUUID());}
        ItemStack item(String... flags){return item(0,flags);}
        ItemStack item(int base,String... flags){var result=LbcTmtItems.prototype(base);var root=result.get(DataComponents.CUSTOM_DATA).copyTag();var properties=new ArrayList<LbcTmtAbilities.Ability>();
            for(var flag:flags){var family=game.items().tmt().catalog().families().stream().filter(row->row.name().equals(flag)).findFirst().orElseThrow();properties.add(family.variants().getFirst());}
            LbcTmtAbilities.write(root,properties);result.set(DataComponents.CUSTOM_DATA,CustomData.of(root));LbcTmtContinuous.apply(result);return result;}
        void equip(ItemStack stack){current=stack;f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void funded(){game.items().mana().maximum(f.owner,1000000);game.items().mana().set(f.owner,1000000);}
        void begin(){
            require(game.items().kiuas().ready()&&game.items().kiuas().count()==0&&game.items().tanner().count()==0&&game.items().babylon().count()==0
                    &&game.items().defensiveArcanums().count()==0&&game.items().voidArcanum().count()==0&&game.items().syringe().count()==0,"exclusive prepared native held services");
            f.prepareParticipants();f.move(f.owner,f.home.add(0,500,0));f.move(f.other,f.home.add(12,500,0));funded();f.owner.setHealth(900);
            summon=new Skeleton(EntityTypes.SKELETON,f.level);summon.setPos(f.owner.position().add(4,0,0));summon.setNoAi(true);summon.setNoGravity(true);
            game.items().summons().track(f.owner,summon);require(f.level.addFreshEntity(summon),"actual owned syringe recipient");f.own(summon);
            equip(item("healing_arcanum","barrier_arcanum","tannerkivi","gate_of_babylon","void_curse","siggewinne_syringe","kiuaskivi"));
            var holy=item("holyshield","nahida_dakimakura");var draws=new ArrayList<LbcTmtAbilities.Ability>();var choices=LbcTmtAbilities.read(holy);for(int i=0;i<7;i++)draws.add(choices.get(i%2));
            for(int seed=0;seed<5;seed++){var generated=game.items().tmt().generate(net.minecraft.util.RandomSource.create(seed),draws);require(generated.has(DataComponents.BLOCKS_ATTACKS)&&generated.has(DataComponents.CONSUMABLE)&&!generated.get(DataComponents.EQUIPPABLE).swappable(),"actual generator retains native sustained prerequisites and wearable asset without intercepting use for equipment swap");}
            var parsed=LbcTmtAbilities.read(current);require(parsed.size()==7&&parsed==LbcTmtAbilities.read(current),"same immutable component reuses its bounded parsed property list");
            var copy=current.copy();require(parsed==LbcTmtAbilities.read(copy),"native stack copy shares the immutable component cache");
            var changed=current.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(changed,parsed.subList(0,1));copy.set(DataComponents.CUSTOM_DATA,CustomData.of(changed));
            require(LbcTmtAbilities.read(copy).size()==1&&LbcTmtAbilities.read(current)==parsed,"new component has independent cache; old source remains untouched");next(0);
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<1500,"bounded scene phase="+phase);
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<6)return false;
                require(f.owner.getHealth()>900&&game.items().defensiveArcanums().activeHands(f.owner)==1,"native healing and barrier both work in one real held item");
                require(game.items().tanner().count()==1&&game.items().voidArcanum().count()>0&&game.items().syringe().buffed(summon)
                        &&game.items().kiuas().activeKinds(f.owner)==1,"all seven native held owners actually activated: tanner="+game.items().tanner().count()+", babylon="+game.items().babylon().count()+", void="+game.items().voidArcanum().count()+", syringe="+game.items().syringe().buffed(summon)+", kiuas="+game.items().kiuas().activeKinds(f.owner)+", mana="+game.items().mana().get(f.owner));
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.getInventory().setItem(9,current);next(1);return false;}
            if(phase==1){if(elapsed<3)return false;
                require(game.items().defensiveArcanums().activeHands(f.owner)==0&&game.items().tanner().count()==0&&game.items().voidArcanum().count()==0
                        &&!game.items().syringe().buffed(summon)&&game.items().kiuas().activeKinds(f.owner)==0,"inventory-only item cannot impersonate either held hand");
                equip(item("gate_of_babylon"));funded();next(10);return false;}
            if(phase==10){if(elapsed<4)return false;require(game.items().babylon().count()>0,"native Babylon output survives after removing projectile-transmuting Tanner");f.owner.getInventory().setItem(9,ItemStack.EMPTY);game.items().babylon().stop();funded();equip(item("kiuaskivi","sun_arcanum"));next(2);return false;}
            if(phase==2){if(elapsed<6)return false;require(game.items().kiuas().activeKinds(f.owner)==3,"both independent Kiuas and Sun activation/upkeep latches remain funded");
                f.pause();require(game.vanity().requestCapture(f.owner,false),"whole coordinator accepts both native aura latches");next(3);return false;}
            if(phase==3){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;require(game.items().kiuas().activeKinds(f.owner)==3,"capture preserves both paused latches");f.thaw();f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);next(4);return false;}
            if(phase==4){if(elapsed<2)return false;require(game.items().kiuas().activeKinds(f.owner)==0,"future unequip clears both aura latches");f.pause();require(game.vanity().requestRestoreForCheck(),"whole coordinator restores native aura state");restoring=true;next(5);return false;}
            if(phase==5){if(game.vanity().busy())return false;require(game.items().kiuas().activeKinds(f.owner)==3&&game.items().timeStop().stopped(f.level),"whole replay restores both independent paused latches");
                game.vanity().stop();f.thaw();f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);next(6);return false;}
            if(phase==6){if(elapsed<2)return false;
                if(unholy!=null)f.discard(unholy);unholy=new Zombie(EntityTypes.ZOMBIE,f.level);unholy.setNoAi(true);unholy.setNoGravity(true);unholy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);unholy.setHealth(1000);
                unholy.setPos(f.owner.getEyePosition().add(f.owner.getLookAngle().scale(3)));unholy.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,-1,0));require(f.level.addFreshEntity(unholy),"real unholy shield target");f.own(unholy);
                equip(item(carrier,"holyshield","nahida_dakimakura"));if(carrier==1||carrier==3)f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.ARROW,2));require(current.has(DataComponents.BLOCKS_ATTACKS)&&current.has(DataComponents.CONSUMABLE),"real native sustained prerequisites attached");
                require((carrier!=0&&carrier!=2)||((eu.pb4.polymer.core.api.item.PolymerItem)current.getItem()).getPolymerItem(current,null)==Items.STICK,"client receives generic component-driven sustained input with same model");
                require(f.owner.gameMode.useItem(f.owner,f.level,current,InteractionHand.MAIN_HAND).consumesAction()&&f.owner.isUsingItem(),"actual TMT carrier input starts continuous native use: "+carrier);next(7);return false;}
            if(phase==7){if(elapsed<7)return false;
                require(f.owner.getUseItem()==current&&f.owner.getItemBlockingWith()==current&&f.owner.entityTags().contains("plapper")&&unholy.getHealth()<1000,"native shield delay/blocking, Holy pulses and Nahida owner all run during actual sustained use");
                f.owner.stopUsingItem();next(8);return false;}
            if(phase==8){if(elapsed<2)return false;require(!f.owner.entityTags().contains("plapper")&&f.owner.getItemBlockingWith()==null,"native release clears Nahida modifiers/state and blocking");if(++carrier<5){next(6);return false;}return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();f.owner.stopUsingItem();game.items().cunny().forget(f.owner);if(f.stopped)f.thaw();
                game.items().syringe().stop();game.items().babylon().stop();game.items().defensiveArcanums().stop();game.items().voidArcanum().stop();game.items().tanner().stop();game.items().kiuas().stop();
                for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())){f.own(e);game.items().entityUnloaded(e);e.discard();}
            }finally{f.close();}}
    }
}
