package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Real scheduled native mutation owners acting on complete mixed carriers. */
public final class TmtInventoryMutationChecks {
    private TmtInventoryMutationChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT inventory mutations: "+why);}
    static void run(GameController game){require(running==null,"exclusive scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    static void tick(GameController game){var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
        SkyWars2.LOGGER.info("[check] PASS tmt-inventory-mutations (private colliding PDC aliases, native live Malevich fill/ground conversion, mixed sword melee, scheduled full-carrier curse walk/storage/unpack and partial coordinator replay, protected/MAG/TimeStop and exact native Greedstone debit)");
    }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-inventory-mutations",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> initial=new HashSet<>();
        final LbcCursedPumpkin.Checkpoint curseSaved;final LbcEternalWealth.Checkpoint wealthSaved;
        final int prestige;final List<ItemStack> payload=new ArrayList<>();
        ItemStack male,maleSaved,groundMaleSaved,curse,curseSavedStack,storedVictim,stone;ItemEntity groundMale,donor,unpack;Marker jammer;
        CustomData stoneData;Map<Integer,ItemStack> prefix;int stage,at,started;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            curseSaved=game.items().cursedPumpkin().checkpoint();wealthSaved=game.items().eternalWealth().checkpoint();prestige=game.items().mana().score(f.owner,"greed_prestige").get();
            require(curseSaved.items.isEmpty()&&curseSaved.savedHolders.isEmpty()&&game.items().eternalWealth().count()==0&&game.items().malevich().pendingCount()==0&&game.items().malevich().groundCount()==0,"no unrelated native mutation work");}
        LbcTmtAbilities.Ability property(String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream())
                .filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();}
        ItemStack mixed(String flag,int count){var stack=LbcTmtItems.prototype(2);stack.setCount(count);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            LbcTmtAbilities.write(data,List.of(property(flag),property("attack_damageattribute")));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));stack.set(DataComponents.REPAIR_COST,37);
            require(ItemStack.matches(stack,ItemStackTemplate.fromNonEmptyStack(stack).create()),"valid native template roundtrip for mixed fixture "+flag);return stack;}
        ItemStack specimen(int i){var stack=new ItemStack(Items.DIAMOND,1+i%7);stack.set(DataComponents.CUSTOM_NAME,Component.literal("mixed stored "+i));var tag=new CompoundTag();tag.putInt("tmt_mutation_fixture",i);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;}
        void next(int value){stage=value;at=game.server().getTickCount();}
        void changed(){game.items().malevich().inventoryChanged(f.owner);game.items().cursedPumpkin().inventoryChanged(f.owner);game.items().mana().inventoryChanged(f.owner);}
        void jam(boolean tech){unjam();jammer=new Marker(EntityTypes.MARKER,f.level);jammer.setPos(f.owner.position());jammer.entityTags().add(tech?"stopper":"stopper_magic");f.own(jammer);require(f.level.addFreshEntity(jammer),"real jammer");game.items().magic().loaded(jammer);
            require(tech?game.items().magic().mechanismSuppressed(f.owner):game.items().magic().suppressed(f.owner),"positive suppression witness");}
        void unjam(){if(jammer==null)return;var live=f.level.getEntity(jammer.getUUID());if(live!=null){game.items().magic().unloaded(live);live.discard();}game.items().magic().unloaded(jammer);jammer.discard();jammer=null;}
        ItemEntity drop(ItemStack stack,Vec3 at){var item=new ItemEntity(f.level,at.x,at.y,at.z,stack);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(32767);item.setThrower(f.owner);f.own(item);game.items().markCurrentItemEpoch(item);require(f.level.addFreshEntity(item),"actual native ground stack");return item;}
        void aliases(){for(var ns:List.of("luckywars","lws","lucky_wars")){
            var original=property("malevich");var tag=original.data().copyTag();var values=new CompoundTag();var nested=new CompoundTag();nested.putString(ns+":type","malevich");values.put(ns+":item",nested);tag.put("PublicBukkitValues",values);
            var collision=new CompoundTag();var wrong=new CompoundTag();wrong.putString(ns+":type","different_native_property");values=new CompoundTag();values.put(ns+":item",wrong);collision.put("PublicBukkitValues",values);
            var stack=LbcTmtItems.prototype(0);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(root,List.of(new LbcTmtAbilities.Ability(original.id(),CustomData.of(tag)),new LbcTmtAbilities.Ability("fixture/pdc_collision",CustomData.of(collision))));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            var pattern=new CompoundTag();pattern.put("PublicBukkitValues",tag.getCompoundOrEmpty("PublicBukkitValues"));
            require(!stack.get(DataComponents.CUSTOM_DATA).matchedBy(pattern)&&LbcMalevich.matches(stack),"private alias remains visible behind colliding outer PDC "+ns);
            var ordinary=new ItemStack(Items.STICK);ordinary.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));require(LbcMalevich.matches(ordinary),"ordinary PDC alias "+ns);
        }}
        void melee(){var sword=mixed("pumpkin_king",1);var expected=sword.copy();f.owner.setItemInHand(InteractionHand.MAIN_HAND,sword);f.move(f.other,f.owner.position().add(2,0,0));
            f.other.setInvulnerableTime(0);f.other.damageCooldownTime=0;require(f.other.hurtServer(f.level,f.other.damageSources().playerAttack(f.owner),1),"actual accepted sword melee");
            int count=0;for(int i=0;i<41;i++)if(LbcCursedPumpkinItems.curse(LbcCursedPumpkin.get(f.other,i)))count++;
            require(count==1&&ItemStack.matches(sword,expected),"one victim curse and intact complete mixed sword");f.other.getInventory().clearContent();game.items().cursedPumpkin().inventoryChanged(f.other);f.owner.getInventory().clearContent();}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();game.players().get(f.owner.getUUID()).lbcSkill=0;game.players().get(f.other.getUUID()).lbcSkill=0;f.owner.entityTags().add("obtained_crux");aliases();melee();
            male=mixed("malevich",1);maleSaved=male.copy();f.owner.getInventory().setItem(9,male);changed();next(0);}
        ItemStack moving(){ItemStack found=ItemStack.EMPTY;for(int i=0;i<41;i++){var stack=LbcCursedPumpkin.get(f.owner,i);if(LbcCursedPumpkinItems.curse(stack)){require(found.isEmpty(),"single moving mixed curse");found=stack;}}return found;}
        void prepareCurse(){f.owner.getInventory().clearContent();storedVictim=mixed("armorattribute",3);for(int i=0;i<41;i++)LbcCursedPumpkin.set(f.owner,i,storedVictim.copy());curse=mixed("pumpkin_curse",1);curse.set(LbcCursedPumpkinItems.CONTENTS,List.of(ItemStackTemplate.fromNonEmptyStack(specimen(1000))));curseSavedStack=curse.copy();LbcCursedPumpkin.set(f.owner,35,curse);changed();game.config().lbcSkills=true;game.players().get(f.owner.getUUID()).lbcSkill=2;require(game.items().inventoryHazards().protectedInventory(f.owner),"actual native protected-inventory perk enabled");next(2);}
        Map<Integer,ItemStack> outputs(){var result=new TreeMap<Integer,ItemStack>();for(var e:f.level.getAllEntities())if(e instanceof ItemEntity item&&!initial.contains(e.getUUID())){int id=ItemData.intValue(item.getItem(),"tmt_mutation_fixture",-1);if(id>=0&&id<80){require(result.put(id,item.getItem().copy())==null,"unique output "+id);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(32767);}}return result;}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();if(stage==9&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();int now=game.server().getTickCount(),elapsed=now-at;require(now-started<700,"bounded scheduled stage "+stage);
            if(stage==0){if(f.owner.getOffhandItem().isEmpty()){require(elapsed<15,"live native Malevich fill");return false;}require(ItemStack.matches(male,maleSaved)&&f.owner.getInventory().getItem(9)==male,"fill preserves complete original live carrier");for(int i=0;i<41;i++)require(!f.owner.getInventory().getItem(i).isEmpty(),"native fills all41 slots");f.owner.getInventory().clearContent();changed();
                // Native publication stamps a previously unattributed source with its thrower's lineage.
                groundMaleSaved=male.copy();LbcInventoryHazards.recordProducer(groundMaleSaved,f.owner.getUUID(),game.items().epoch());groundMale=drop(male.copy(),f.home.add(0,6,0));
                require(ItemStack.matches(groundMale.getItem(),groundMaleSaved)&&game.items().currentEpoch(groundMale)&&f.owner.getUUID().equals(game.items().inventoryHazards().producerIdOf(groundMale.getItem())),"native publication adds exactly current producer lineage while preserving every source component/property");
                donor=drop(new ItemStack(Items.EMERALD,5),groundMale.position().add(1,0,0));next(1);return false;}
            if(stage==1){if(!donor.isRemoved()){require(elapsed<25,"actual Malevich ground pulse");return false;}require(!groundMale.isRemoved()&&ItemStack.matches(groundMale.getItem(),groundMaleSaved),"ground conversion retains complete mixed source and native producer lineage");f.discard(groundMale);for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(e instanceof ItemEntity&&!initial.contains(e.getUUID()))f.discard(e);prepareCurse();return false;}
            if(stage==2){require(ItemStack.matches(LbcCursedPumpkin.get(f.owner,35),curseSavedStack),"protected actual curse inventory");if(elapsed<12)return false;game.players().get(f.owner.getUUID()).lbcSkill=0;jam(false);next(3);return false;}
            if(stage==3){require(ItemStack.matches(LbcCursedPumpkin.get(f.owner,35),curseSavedStack),"MAG prevents native curse walk");if(elapsed<12)return false;unjam();f.pause();next(4);return false;}
            if(stage==4){require(ItemStack.matches(LbcCursedPumpkin.get(f.owner,35),curseSavedStack),"real TimeStop prevents native curse walk");if(elapsed<12)return false;
                for(int slot:List.of(26,34,8))require(ItemStack.matches(LbcCursedPumpkin.get(f.owner,slot),storedVictim),"actual live source35 destination victim remains exact before native walk "+slot);
                f.thaw();jam(true);next(5);return false;}
            if(stage==5){var moved=moving();if(moved.getOrDefault(LbcCursedPumpkinItems.CONTENTS,List.of()).size()<2){require(elapsed<12,"native curse walks under TECH");return false;}
                require(moved.get(DataComponents.CUSTOM_DATA)==curseSavedStack.get(DataComponents.CUSTOM_DATA)&&moved.get(DataComponents.REPAIR_COST)==37&&LbcTmtAbilities.read(moved).size()==2,"moving carrier preserves immutable siblings and outer components");
                int location=-1;for(int slot=0;slot<41;slot++)if(LbcCursedPumpkin.get(f.owner,slot)==moved)location=slot;
                require(Set.of(26,34,8).contains(location)&&moved.get(LbcCursedPumpkinItems.CONTENTS).size()==2&&LbcCursedPumpkin.get(f.owner,35).is(Items.PUMPKIN_SEEDS),"one exact source35 native movement appends only its selected live victim");
                require(ItemStack.matches(moved.get(LbcCursedPumpkinItems.CONTENTS).getFirst().create(),specimen(1000)),"preexisting stored template survives native append");
                require(ItemStack.matches(moved.get(LbcCursedPumpkinItems.CONTENTS).get(1).create(),storedVictim),"native storage retains complete swallowed mixed victim/count");
                f.owner.getInventory().clearContent();changed();unjam();for(int i=0;i<80;i++)payload.add(specimen(i));var source=moved.copy();source.set(LbcCursedPumpkinItems.CONTENTS,payload.stream().map(ItemStackTemplate::fromNonEmptyStack).toList());unpack=drop(source,f.home.add(0,6,0));next(6);return false;}
            if(stage==6){var actual=outputs();if(actual.isEmpty()){require(elapsed<25,"native unpack admission");return false;}require(actual.size()<80&&!unpack.isRemoved()&&game.items().currentEpoch(unpack),"actual partial native unpack with source epoch");prefix=actual;f.pause();require(game.vanity().requestCapture(f.owner,false),"full coordinator captures partial mixed unpack");next(7);return false;}
            if(stage==7){require(outputs().keySet().equals(prefix.keySet()),"TimeStop holds real partial output cursor");if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;f.thaw();next(8);return false;}
            if(stage==8){if(outputs().size()<=prefix.size())return false;f.pause();require(game.vanity().requestRestoreForCheck(),"full coordinator restores mixed cursor");next(9);return false;}
            if(stage==9){if(game.vanity().busy())return false;require(outputs().keySet().equals(prefix.keySet()),"full replay restores exact native published prefix");for(var e:prefix.entrySet())require(ItemStack.matches(e.getValue(),outputs().get(e.getKey())),"prefix full component replay");game.vanity().stop();f.thaw();next(10);return false;}
            if(stage==10){var actual=outputs();if(actual.size()<80){require(elapsed<20,"native resumed unpack drain");return false;}require(f.level.getEntity(unpack.getUUID())==null,"ordinary curse intentionally retires full carrier after unpack");for(int i=0;i<80;i++)require(ItemStack.matches(payload.get(i),actual.get(i)),"original stored count/components "+i);
                game.config().lbcSkills=true;game.players().get(f.owner.getUUID()).lbcSkill=2;require(game.items().inventoryHazards().protectedInventory(f.owner),"Greedstone native perk prerequisite positively enabled");stone=mixed("greedstone",2);stoneData=stone.get(DataComponents.CUSTOM_DATA);f.owner.getInventory().setItem(9,stone);changed();next(11);return false;}
            if(stage==11){require(stone.getCount()==2&&game.items().mana().score(f.owner,"greed_prestige").get()==prestige,"actual protected perk blocks carried Greedstone");if(elapsed<4)return false;f.owner.getInventory().setItem(9,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stone);jam(false);changed();next(12);return false;}
            if(stage==12){require(stone.getCount()==2,"MAG blocks mainhand native Greedstone");if(elapsed<4)return false;jam(true);next(13);return false;}
            if(stage==13){if(stone.getCount()==2){require(elapsed<4,"native Greedstone intake resumes under TECH");return false;}require(stone.getCount()==1&&stone.get(DataComponents.CUSTOM_DATA)==stoneData&&LbcTmtAbilities.read(stone).size()==2&&stone.get(DataComponents.REPAIR_COST)==37,"exactly one actual matching mixed token consumed with complete survivor");require(game.items().mana().score(f.owner,"greed_prestige").get()==prestige+1&&game.items().eternalWealth().count()>0,"real native prestige and pending actor");f.owner.getInventory().clearContent();changed();return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();unjam();if(f.stopped)f.thaw();for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(e.getUUID()))f.discard(e);game.items().mana().score(f.owner,"greed_prestige").set(prestige);
                game.items().cursedPumpkin().prepareFrozen();curseSaved.prepare(f.level::getEntity).install().run();game.items().cursedPumpkin().publishFrozen();game.items().eternalWealth().prepareFrozen();wealthSaved.prepare(f.level::getEntity).install().run();game.items().eternalWealth().publishFrozen();
            }finally{f.close();game.items().malevich().inventoryChanged(f.owner);game.items().cursedPumpkin().inventoryChanged(f.owner);}}
    }
}
