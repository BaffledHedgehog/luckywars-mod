package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Actual inventory scheduler and survival recipe taking; native rewards/debits remain authoritative. */
public final class TmtIntakeChecks {
    private TmtIntakeChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT intake: "+why);}
    static void run(GameController game){require(running==null,"exclusive native intake scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    static void tick(GameController game){var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
        SkyWars2.LOGGER.info("[check] PASS tmt-intake (three private Touhou rewards, one actual inventory debit and no repeats, real TimeStop, native PhD preview/result take/full catalyst remainder, protected Nexus intake/main hand, combined red+black single debit and shared native rewards)");
    }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-intake",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcTouhouCollectibles.Checkpoint touhou;
        final LbcNexusWires.Checkpoint wires;final int skill,oldRegen,oldPower,crafted;
        final net.minecraft.stats.Stat<net.minecraft.world.item.Item> craftStat;
        int phase,when,started,xp;boolean closed;ItemStack intake,wire;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);touhou=game.items().touhouCollectibles().checkpoint();wires=game.items().nexusWires().checkpoint();
            require(game.items().nexusWires().captureReady()&&game.items().nexusWires().loadingChunks()==0,"exclusive empty native gateway queue");
            skill=game.players().get(f.owner.getUUID()).lbcSkill;oldRegen=game.items().mana().baseRegen(f.owner);oldPower=game.items().touhouCollectibles().counters(f.owner).power();
            craftStat=net.minecraft.stats.Stats.ITEM_CRAFTED.get(LbcPills.PILL);crafted=f.owner.getStats().getValue(craftStat);}
        LbcTmtAbilities.Ability property(String family){return game.items().tmt().catalog().families().stream().filter(f->f.id().equals("native/"+family)).findFirst().orElseThrow().variants().getFirst();}
        ItemStack mixed(int count,LbcTmtAbilities.Ability... properties){var stack=LbcTmtItems.prototype(0);stack.set(DataComponents.MAX_STACK_SIZE,64);stack.setCount(count);
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(properties));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void changed(){f.owner.getInventory().setChanged();game.items().mana().inventoryChanged(f.owner);game.items().touhouCollectibles().inventoryChanged(f.owner);game.items().nexusWires().inventoryChanged(f.owner);}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();f.pause();
            intake=mixed(2,property("touhou/1"),property("touhou/2"),property("touhou/9"));f.owner.getInventory().setItem(9,intake);changed();next(0);}
        void craft(){
            var wand=game.items().tmt().catalog().families().stream().filter(f->f.events().contains(LbcTmtCatalog.Event.WAND_CAST)).findFirst().orElseThrow().variants().getFirst();
            var certificate=mixed(2,property("phd"),wand);var saved=certificate.copy();require(LbcPills.certificate(certificate),"real private catalyst classifier");
            var menu=(AbstractCraftingMenu)f.owner.inventoryMenu;var slots=menu.getInputGridSlots();
            var donors=List.of(new ItemStack(Items.BONE_MEAL,2),new ItemStack(Items.APPLE,2),new ItemStack(Items.STICK,2));
            slots.getFirst().setByPlayer(certificate);for(int i=1;i<4;i++)slots.get(i).setByPlayer(donors.get(i-1));
            var preview=menu.getResultSlot().getItem().copy();require(preview.is(LbcPills.PILL)&&preview.getCount()==1,"native survival result preview");
            GameChecks.inventoryClick(f.owner,0,0,ContainerInput.PICKUP);
            require(ItemStack.matches(menu.getCarried(),preview)&&ItemStack.matches(slots.getFirst().getItem(),saved),"actual result take keeps full mixed catalyst count and every component");
            for(var donor:donors)require(donor.getCount()==1,"native result owner consumes one of each biological donor");
            require(f.owner.getStats().getValue(craftStat)==crafted+1,"one native crafted result stat");
            menu.setCarried(ItemStack.EMPTY);for(var slot:slots)slot.setByPlayer(ItemStack.EMPTY);
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<120,"bounded native intake phases "+phase);int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<3)return false;require(f.owner.getInventory().getItem(9)==intake&&intake.getCount()==2&&game.items().touhouCollectibles().counters(f.owner).power()==oldPower,"TimeStop defers every private intake reward");f.thaw();next(1);return false;}
            if(phase==1){if(!f.owner.getInventory().getItem(9).isEmpty()){require(elapsed<20,"scheduled native intake drains TMT");return false;}
                require(game.items().touhouCollectibles().counters(f.owner).power()==Math.min(150,oldPower+22)&&game.items().mana().baseRegen(f.owner)==oldRegen+2,"all three native property rewards use original stack quantity");next(2);return false;}
            if(phase==2){if(elapsed<3)return false;require(game.items().mana().baseRegen(f.owner)==oldRegen+2,"consumed private reward never repeats");craft();f.pause();
                game.config().lbcSkills=true;game.players().get(f.owner.getUUID()).lbcSkill=2;require(game.items().inventoryHazards().protectedInventory(f.owner),"actual protected-inventory positive control");
                wire=mixed(2,property("nexus_wire_red"),property("nexus_wire_black"));f.owner.getInventory().setItem(9,wire);changed();next(3);return false;}
            if(phase==3){if(elapsed<3)return false;require(wire.getCount()==2,"TimeStop holds mixed wires");f.thaw();next(4);return false;}
            if(phase==4){if(elapsed<3)return false;require(f.owner.getInventory().getItem(9)==wire&&wire.getCount()==2,"protected inventory holds TMT outside main hand");
                f.owner.getInventory().setItem(9,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.MAIN_HAND,wire);f.owner.getInventory().setItem(10,new ItemStack(LbcNexusWireItems.RED));
                game.items().mana().baseRegen(f.owner,0);game.items().mana().maximum(f.owner,20000);game.items().mana().set(f.owner,100);xp=f.owner.totalExperience;changed();next(5);return false;}
            if(phase==5){if(wire.getCount()==2){require(elapsed<20,"mixed wire starts via native carrier scheduler");return false;}
                require(wire.getCount()==1&&f.owner.totalExperience==xp+20&&game.items().mana().get(f.owner)==5100,"one mixed debit and exact native red resources");
                require(f.owner.getInventory().getItem(10).getCount()==1&&game.items().nexusWires().pendingGateways().size()==1,"ordinary red does not duplicate same-tick grant; native black queue receives one gateway");return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.server().tickRateManager().setFrozen(true);game.items().nexusWires().prepareFrozen();var wirePlan=wires.prepare(f.level::getEntity);require(wirePlan.pending().isEmpty(),"original native wire links available");wirePlan.install().run();
                game.items().touhouCollectibles().prepareFrozen();touhou.prepare(f.level::getEntity).install().run();game.items().touhouCollectibles().publishFrozen();game.items().nexusWires().publishFrozen();
                f.owner.getStats().setValue(f.owner,craftStat,crafted);game.players().get(f.owner.getUUID()).lbcSkill=skill;
            }finally{f.close();}}
    }
}
