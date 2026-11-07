package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real inputs and server ticks for the shared Jack flight and nuclear encounter. */
public final class BossProjectileChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Boss/projectile: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scene");var s=new Scene(game);running=s;try{s.begin();}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS boss-projectiles (native Jack input, persistent typed mode/clone and TimeStop; real200HP Crocodildo, active bomb cadence, NPC nuclear self-kill, immediate protected64nukes/upgraded wand, TECH ordinary rewarded death)");}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL boss-projectiles",e);}}
    static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> original=new HashSet<>();final Set<Long> forced=new HashSet<>();
        final NativeMeleeChecks.NativeStats stats;int phase,when,start;boolean closed;Display.BlockDisplay jack;LivingEntity boss;ItemEntity foreign;
        Vec3 jackAt,bossAt;int savedAge;Set<UUID> rewardIds;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcKapustaItem.JACK,LbcCrocodildoItems.NUKE,LbcTmtItems.prototype(0).getItem()));f.level.getAllEntities().forEach(e->original.add(e.getUUID()));}
        void next(int p){phase=p;when=game.server().getTickCount();}
        void force(Vec3 at){var p=BlockPos.containing(at);int x=p.getX()>>4,z=p.getZ()>>4;if(f.level.setChunkForced(x,z,true))forced.add(net.minecraft.world.level.ChunkPos.pack(x,z));f.level.getChunk(x,z);}
        void begin(){
            f.prepareParticipants();require(game.items().crocodildo().bodies().isEmpty()&&game.items().kapusta().actors().isEmpty(),"exclusive native encounter/flight owners");start=game.server().getTickCount();
            var source=game.lbc().rollTable(f.level,f.home,null,"lbc:64_nukes");
            require(source.stream().filter(s->s.is(LbcCrocodildoItems.NUKE)).mapToInt(ItemStack::getCount).sum()==64,"actual source reward count before entity publication: "+source);
            for(var s:source){var test=LbcBossLoot.protect(new ItemEntity(f.level,f.home.x,f.home.y,f.home.z,s.copy()));require(ItemStack.matches(test.getItem(),s),"creation-time protection retains exact stack and count: "+s+" -> "+test.getItem());}
            var stack=f.loot("lbc:item_defs/minecraft/jack_o_lantern__cc18e55635ba").copyWithCount(2);require(stack.is(LbcKapustaItem.JACK)&&LbcKapusta.jack(stack),"canonical native Jack source leaf");
            require(stack.get(DataComponents.ITEM_MODEL).toString().equals("minecraft:jack_o_lantern"),"source vanilla Jack presentation");
            var property=game.items().tmt().useProfiles().families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt("jack_o_lantern",0)==1).findFirst().orElseThrow();
            stack=LbcTmtItems.prototype(0).copyWithCount(2);var payload=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(payload,List.of(property));stack.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(payload));f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==1,"actual Jack input and one debit");
            jack=game.items().kapusta().actors().getFirst();f.own(jack);require(game.items().kapusta().state(jack).jack()&&!game.items().kapusta().state(jack).primary(),"shared typed flight owns Jack mode without cabbage offspring");
            require(game.items().cloneSkills().copyNow(jack,15),"native projectile cloning admits Jack");
            for(var e:game.items().kapusta().actors()){f.own(e);require(game.items().kapusta().state(e).jack(),"clone preserves Jack typed mode");}
            jackAt=f.home.add(0,14,0);for(var e:game.items().kapusta().actors()){e.setPos(jackAt);e.setYRot(0);e.setXRot(0);}savedAge=game.items().kapusta().state(jack).age();f.pause();next(0);
        }
        LivingEntity spawn(){
            var origin=f.home;var inward=new Vec3(-origin.x,0,-origin.z).normalize();if(inward.lengthSqr()==0)inward=new Vec3(0,0,1);force(origin.add(0,80,0).subtract(inward.scale(150)));
            var token=new ItemEntity(f.level,origin.x,origin.y,origin.z,f.loot("lbc:crocodildo").copyWithCount(2));token.setNoGravity(true);game.items().markCurrentItemEpoch(token);f.own(token);require(f.level.addFreshEntity(token),"actual Crocodildo token publication");
            require(game.items().crocodildo().spawn(token)&&token.getItem().getCount()==1,"native dropped trigger spends one token");f.discard(token);
            var result=game.items().crocodildo().bodies().stream().filter(e->e.getType()==EntityTypes.TURTLE&&e.isAlive()).findFirst().orElseThrow();f.own(result);
            require(result.getHealth()==200&&result.getMaxHealth()==200,"real200HP and no hidden sentinel");return result;
        }
        Set<UUID> rewards(){var set=new HashSet<UUID>();for(var e:f.level.getAllEntities())if(LbcBossLoot.protectedReward(e)&&!original.contains(e.getUUID()))set.add(e.getUUID());return set;}
        void verifyReward(Set<UUID> before){var drops=new ArrayList<ItemEntity>();for(var id:rewards())if(!before.contains(id))drops.add((ItemEntity)f.level.getEntity(id));
            require(drops.stream().mapToInt(e->e.getItem().is(LbcCrocodildoItems.NUKE)?e.getItem().getCount():0).sum()==64,"all64 actual usable native nuclear eggs; protected="+drops.stream().map(e->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItem().getItem())+" x"+e.getItem().getCount()).toList());
            require(drops.stream().filter(e->ItemData.intValue(e.getItem(),"exasperato_wand",0)==1&&ItemData.intValue(e.getItem(),"upgraded",0)==1).mapToInt(e->e.getItem().getCount()).sum()==1,"one functional upgraded source Exasperato");
        }
        boolean step(){require(game.server().getTickCount()-start<800,"bounded real-tick scene");int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<3)return false;require(jack.position().equals(jackAt)&&game.items().kapusta().state(jack).age()==savedAge,"whole native Jack flight pauses");f.thaw();next(1);return false;}
            if(phase==1){if(elapsed<3)return false;require(!jack.position().equals(jackAt)&&game.items().kapusta().state(jack).age()>savedAge,"actual shared scheduler resumes Jack");game.items().kapusta().stop();f.owner.getInventory().clearContent();boss=spawn();bossAt=new Vec3(f.home.x,f.level.getMaxY()+250,f.home.z);boss.setPos(f.home.add(0,25,0));boss.setNoGravity(true);savedAge=game.items().crocodildo().age(boss);f.pause();next(2);return false;}
            if(phase==2){if(elapsed<3)return false;require(game.items().crocodildo().age(boss)==savedAge&&game.items().crocodildo().bodies().size()==1,"TimeStop pauses encounter and bombing cadence");require(game.vanity().requestCapture(f.owner,false),"whole coordinator captures actual encounter");next(20);return false;}
            if(phase==20){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;game.items().crocodildo().stop();require(game.vanity().requestRestoreForCheck(),"actual restore after discarded encounter/visual");next(21);return false;}
            if(phase==21){if(game.vanity().busy())return false;require(f.level.getEntity(boss.getUUID())==boss&&!boss.isRemoved()&&game.items().crocodildo().age(boss)==savedAge&&boss.getHealth()==200,"canonical body and typed encounter clock restored");require(boss.getPassengers().size()==1&&LbcCrocodildo.visual(boss.getPassengers().getFirst()),"actual paired visual restored");game.vanity().stop();boss.setPos(bossAt);f.thaw();next(3);return false;}
            if(phase==3){var pig=game.items().crocodildo().bodies().stream().filter(e->e.getType()==EntityTypes.PIG).findFirst().orElse(null);if(pig==null){require(elapsed<110,"native five-second active bombing cadence");return false;}
                // The actual falling-bomb owner handles landing next tick; no parallel blast implementation.
                boss.setPos(bossAt);pig.setPos(bossAt);pig.setNoGravity(true);pig.setOnGround(true);f.own(pig);rewardIds=rewards();foreign=new ItemEntity(f.level,bossAt.x,bossAt.y,bossAt.z,new ItemStack(Items.EMERALD));f.own(foreign);require(f.level.addFreshEntity(foreign),"foreign unprotected item witness");next(4);return false;}
            if(phase==4){if(boss.isAlive()){require(elapsed<5,"first nuclear damage is immediate on landing");return false;}
                require(boss.getLastDamageSource()!=null&&boss.getLastDamageSource().getEntity()==boss,"self-nuke has real NPC cause, never token dropper");verifyReward(rewardIds);rewardIds=rewards();require(!rewardIds.isEmpty(),"immediate reward before later nuclear work");next(5);return false;}
            if(phase==5){if(!game.items().nukeEvent().captureReady()){require(elapsed<300,"shared nuclear waves/terrain/cleanup drain on scheduler");return false;}
                require(rewards().containsAll(rewardIds),"all rewards survive every shared nuclear wave AND explicit final entity cleanup");require(foreign.isRemoved(),"foreign nearby drops were not accidentally protected");
                boss=spawn();boss.setPos(f.home.add(0,25,0));boss.setNoGravity(true);var marker=new Marker(EntityTypes.MARKER,f.level);marker.setPos(boss.position());marker.entityTags().add("stopper");f.own(marker);require(f.level.addFreshEntity(marker),"native TECH marker");game.items().magic().loaded(marker);rewardIds=rewards();next(6);return false;}
            if(phase==6){if(boss.isAlive()){require(elapsed<5,"TECH ordinary death");return false;}verifyReward(rewardIds);
                var stack=f.loot("lbc:nuke").copyWithCount(2);require(stack.is(LbcCrocodildoItems.NUKE),"actual usable reward item");f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var pad=BlockPos.containing(f.home.add(0,1,3));f.set(pad,Blocks.STONE.defaultBlockState());var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atBottomCenterOf(pad.above()),net.minecraft.core.Direction.UP,pad,false);
                require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()&&stack.getCount()==1,"real nuclear egg use-on and one debit");
                require(game.items().nukeEvent().markers().size()==1&&game.items().nukeEvent().markers().getFirst().entityTags().contains("nuke1"),"same native timed nuclear owner, no inert marker or panda");return true;}return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();game.items().kapusta().stop();game.items().crocodildo().stop();game.items().nukeEvent().stop();var fresh=new ArrayList<Entity>();for(var e:f.level.getAllEntities())if(!(e instanceof net.minecraft.server.level.ServerPlayer)&&!original.contains(e.getUUID()))fresh.add(e);for(var e:fresh){game.items().magic().unloaded(e);f.discard(e);}stats.close();}finally{try{f.close();}finally{for(long c:forced)f.level.setChunkForced((int)c,(int)(c>>32),false);}}}
    }
}
