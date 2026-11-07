package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** One native scene for the current-content batch; it uses real hands, factories and ordinary ticks. */
public final class TmtCurrentContentChecks {
    private TmtCurrentContentChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT current content: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scene");var scene=new Scene(game);running=scene;try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();SkyWars2.LOGGER.info("[check] PASS tmt-current-content (real selector input/property-only spend, native blade block, ordinary most snowball/trail, authored native firework use-on, accepted Chef-label melee, green wire inventory reward, Liquid Abyss ticks, yellow passive, Infinity intake, immediate Rasengan and attributed End event input; independent properties preserved)");}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-current-content",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        final Set<UUID> original=new HashSet<>();final LbcNovemberLightsabers.Checkpoint faction;
        final Integer oldFaction;final LbcTmtAbilities.Ability sibling;
        int phase,when,started;boolean closed;ItemStack held,expected;Snowball ball;List<BlockPos> footprint;float health;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);faction=game.items().lightsabers().checkpoint();
            var board=game.server().getScoreboard();var objective=board.getObjective("november");var score=objective==null?null:board.getPlayerScoreInfo(f.owner,objective);oldFaction=score==null?null:score.value();
            var items=new ArrayList<Item>();for(int i=0;i<5;i++)items.add(LbcTmtItems.prototype(i).getItem());items.add(LbcNovemberLightsaberItems.JEDI);items.add(LbcImbaNativeItems.FIREWORK);stats=new NativeMeleeChecks.NativeStats(f.owner,items);
            f.level.getAllEntities().forEach(e->original.add(e.getUUID()));sibling=property("zaika");}
        LbcTmtAbilities.Ability property(String flag){return game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow(()->new IllegalStateException("Missing actual current property "+flag));}
        LbcTmtAbilities.Ability firework(){return game.items().tmt().useProfiles().families().stream().flatMap(row->row.variants().stream()).filter(p->LbcTmtComponents.nativeItem(p.data())==LbcImbaNativeItems.FIREWORK).findFirst().orElseThrow();}
        ItemStack mixed(LbcTmtAbilities.Ability property){var stack=LbcTmtItems.prototype(0);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(property,sibling));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        boolean input(ItemStack stack){f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);return f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction();}
        void clear(){f.owner.stopUsingItem();f.owner.getInventory().clearContent();f.owner.removeAllEffects();for(var e:List.copyOf(newActors())){f.own(e);f.discard(e);}}
        List<Entity> newActors(){var result=new ArrayList<Entity>();for(var e:f.level.getAllEntities())if(!original.contains(e.getUUID())&&!e.isRemoved())result.add(e);return result;}
        void changed(){f.owner.getInventory().setChanged();game.items().mana().inventoryChanged(f.owner);game.items().nexusWires().inventoryChanged(f.owner);game.items().shishEncounter().inventoryChanged(f.owner);}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();require(game.items().apocalypseEvents().pendingSnapshot().isEmpty()&&game.items().apocalypseEvents().snapshot().isEmpty()&&game.items().apocalypseEvents().terrainCount()==0,"no preexisting event owned by this fixture");
            f.set(BlockPos.containing(f.home).below(),Blocks.STAINED_GLASS.pick(DyeColor.BLUE).defaultBlockState());game.items().mana().score(f.owner,"november").set(0);
            held=mixed(property("lightsaberrc"));expected=held.copy();require(input(held),"real selector native RMB input");
            require(f.owner.getMainHandItem()==held&&held.getCount()==1&&ItemData.intValue(held,"lightsaberrc",0)==0&&LbcTmtAbilities.read(held).equals(List.of(sibling)),"selector spends only its property and retains physical carrier/sibling");
            require(game.items().lightsabers().faction(f.owner.getUUID())==LbcNovemberLightsabers.Faction.JEDI&&f.owner.getInventory().getNonEquipmentItems().stream().anyMatch(s->s.is(LbcNovemberLightsaberItems.JEDI)),"real faction owner and actual source blade reward");next(0);}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<220,"bounded grouped native scene phase="+phase);int elapsed=game.server().getTickCount()-when;if(elapsed==0)return false;
            if(phase==0){clear();held=mixed(property("lightsaber"));LbcTmtContinuous.apply(held);expected=held.copy();require(input(held)&&f.owner.isUsingItem(),"actual native blade blocking use starts on physical carrot");next(1);return false;}
            if(phase==1){if(elapsed<7)return false;require(f.owner.isBlocking()&&held.get(DataComponents.BLOCKS_ATTACKS).equals(LbcNovemberLightsaberItems.JEDI.getDefaultInstance().get(DataComponents.BLOCKS_ATTACKS)),"native authored blade block delay and component");
                health=f.owner.getHealth();f.move(f.other,f.owner.position().add(0,0,3));f.owner.setYRot(0);f.owner.setXRot(0);f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
                f.owner.hurtServer(f.level,f.owner.damageSources().playerAttack(f.other),4);require(f.owner.getHealth()==health&&LbcTmtAbilities.read(held).equals(LbcTmtAbilities.read(expected)),"real native blocking damage preserves private sibling properties");clear();next(11);return false;}
            if(phase==11){
                held=mixed(property("most"));require(input(held)&&held.isEmpty(),"ordinary bridge reuses actual native snowball input and one physical debit");
                ball=(Snowball)newActors().stream().filter(Snowball.class::isInstance).findFirst().orElseThrow();require(ItemData.intValue(ball.getItem(),"most",0)==1,"native projectile retains selected ordinary source");var at=f.home.add(.2,12,.2);footprint=f.prepare(at,false);f.park(ball,at);f.own(ball);next(2);return false;}
            if(phase==2){f.verifyCells(footprint,DyeColor.LIME,false);clear();var at=BlockPos.containing(f.home).offset(2,-1,0);f.set(at,Blocks.STONE.defaultBlockState());held=mixed(firework());f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);
                var hit=new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),net.minecraft.core.Direction.UP,at,false);
                require(f.owner.gameMode.useItemOn(f.owner,f.level,held,InteractionHand.MAIN_HAND,hit).consumesAction()&&held.isEmpty(),"native firework actual use-on and sole debit");
                var rocket=(FireworkRocketEntity)newActors().stream().filter(FireworkRocketEntity.class::isInstance).findFirst().orElseThrow();require(rocket.getItem().get(DataComponents.FIREWORKS).flightDuration()==127,"authored flight127 reaches actual native rocket");f.own(rocket);f.discard(rocket);next(3);return false;}
            if(phase==3){clear();held=mixed(property("wtf_item"));f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);var target=new Cow(EntityTypes.COW,f.level);target.setNoAi(true);target.setNoGravity(true);target.setPos(f.home.add(0,0,3));require(f.level.addFreshEntity(target),"real label target admitted");f.own(target);
                require(target.hurtServer(f.level,target.damageSources().playerAttack(f.owner),1)&&held.isEmpty(),"actual accepted melee invokes existing Chef label and consumes one");
                require(newActors().stream().anyMatch(LbcWtf.Chef.class::isInstance),"real native Chef body created from private catalyst");clear();held=mixed(property("nexus_wire_blue"));f.owner.getInventory().setItem(9,held);changed();next(4);return false;}
            if(phase==4){if(!held.isEmpty()){require(elapsed<15,"green source inventory scheduler consumes TMT");return false;}
                require(newActors().stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).anyMatch(item->item.getItem().getCount()>0&&item.position().distanceToSqr(f.owner.position())<4),"native green random-item reward spawns at actual player");clear();held=mixed(property("liquidvoid"));f.owner.getInventory().setItem(9,held);game.items().mana().score(f.owner,"void_resistance").set(0);f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;health=f.owner.getHealth();changed();next(5);return false;}
            if(phase==5){if(f.owner.getHealth()==health){require(elapsed<50,"native two-second Liquid Abyss callback");return false;}require(f.owner.getHealth()<health&&held.getCount()==1,"real carried hazard damages without consuming TMT");clear();held=mixed(property("yellow_lightning"));f.owner.getInventory().setItem(0,held);changed();require(f.owner.entityTags().contains("yellow_lightning_active"),"actual private yellow inventory owner admits hotbar");next(6);return false;}
            if(phase==6){if(newActors().stream().noneMatch(e->e.entityTags().contains("piss"))){require(elapsed<12,"native yellow passive produces actual projectile");return false;}clear();held=mixed(property("infinity_rand"));f.owner.getInventory().setItem(9,held);changed();require(InfinityChecks.select(game.items().infinity(),f.owner,1)==1&&held.isEmpty(),"native Infinity instant uses complete private prerequisites and one physical intake");clear();
                // Real cast above empty air: no fixture map blocks can be destroyed by the accepted explosion chain.
                f.move(f.owner,new Vec3(f.home.x,240,f.home.z));held=mixed(property("rasengan"));next(7);return false;}
            if(phase==7){require(input(held)&&held.isEmpty()&&f.owner.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE),"Rasengan source cast runs at native input without a fake snowball/tick");require(newActors().stream().noneMatch(Snowball.class::isInstance),"Rasengan creates no disposable callback projectile");clear();next(8);return false;}
            if(phase==8){held=mixed(property("end_of_everything"));long starts=game.items().apocalypseEvents().startCount();var anchors=new ArrayList<Entity>();for(var level:game.server().getAllLevels())for(var e:level.getAllEntities())if(e.entityTags().remove("swrg.look"))anchors.add(e);
                try{require(input(held)&&held.isEmpty()&&game.items().apocalypseEvents().startCount()==starts+1,"real private End input starts existing native phase owner once");require(game.items().apocalypseEvents().pendingSnapshot().stream().anyMatch(p->p.authority().author().equals(f.owner.getUUID())),"native scheduled event retains real caster");}
                finally{game.items().apocalypseEvents().stop();for(var e:anchors)e.entityTags().add("swrg.look");}return true;}
            throw new IllegalStateException("Unknown grouped native phase "+phase);
        }
        public void close(){if(closed)return;closed=true;try{f.owner.stopUsingItem();for(var e:newActors())f.own(e);faction.prepare(f.level::getEntity).install().run();var objective=game.server().getScoreboard().getObjective("november");if(objective!=null){if(oldFaction==null)game.server().getScoreboard().resetSinglePlayerScore(f.owner,objective);else game.server().getScoreboard().getOrCreatePlayerScore(f.owner,objective,true).set(oldFaction);}game.items().shishEncounter().inventoryChanged(f.owner);}finally{try{f.close();}finally{stats.close();}}}
    }
}
