package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Genuine selected-inventory drop, normal shared scheduler, once-only debit and whole-world continuation. */
public final class TmtDropChecks {
    private TmtDropChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT drops: "+why);}
    public static void run(GameController game){require(running==null,"exclusive native scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;
        try{require(scene.game==game,"same native controller");if(!scene.step())return;running=null;scene.close();
            SkyWars2.LOGGER.info("[check] PASS tmt-drops (actual selected inventory drop, five native Vex plus Skeleton from one shared debit, real thrower and admission, same-frame guard, TimeStop, whole coordinator pending-item restoration and continued native dispatch, read-only scope/cache retention)");
        }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-drops",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final HashSet<UUID> initial=new HashSet<>();final int began;
        final net.minecraft.stats.Stat<Item> dropStat;final net.minecraft.stats.Stat<net.minecraft.resources.Identifier> totalDropStat;
        final net.minecraft.stats.Stat<Item> ordinaryDropStat;final int ordinaryDropped;
        final int dropped,totalDropped;int phase,when;boolean restoring,closed;
        ItemEntity token,ordinary;ItemStack savedItem;Vec3 at;int age;float yaw;final List<Entity> firstActors=new ArrayList<>();
        LbcTmtAbilities.Ability vex;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);began=game.server().getTickCount();for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            dropStat=net.minecraft.stats.Stats.ITEM_DROPPED.get(LbcTmtItems.prototype(2).getItem());totalDropStat=net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DROP);
            dropped=f.owner.getStats().getValue(dropStat);totalDropped=f.owner.getStats().getValue(totalDropStat);
            ordinaryDropStat=net.minecraft.stats.Stats.ITEM_DROPPED.get(LbcCreatureDropItem.Kind.OLD_SKELETON.item());ordinaryDropped=f.owner.getStats().getValue(ordinaryDropStat);}
        LbcTmtAbilities.Ability property(Item item){var id=BuiltInRegistries.ITEM.getKey(item).toString();return game.items().tmt().catalog().families().stream()
                .filter(family->family.events().contains(LbcTmtCatalog.Event.DROP)&&family.locations().contains(LbcTmtCatalog.Location.LOOSE_ITEM))
                .flatMap(family->family.variants().stream()).filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readString(LbcTmtAbilities.NATIVE,"").equals(id)).findFirst().orElseThrow();}
        void begin(){require(game.items().specialDrops().pendingCount()==0,"exclusive ordinary drop dispatcher");f.prepareParticipants();
            vex=property(LbcCreatureDropItem.Kind.OLD_VEX.item());var skeleton=property(LbcCreatureDropItem.Kind.OLD_SKELETON.item());
            var stack=LbcTmtItems.prototype(2);stack.setCount(2);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(root,List.of(vex,skeleton));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            require(LbcTmtDrops.matches(stack)&&LbcTmtDrops.properties(stack).size()==2,"actual sampler supplies both marked native drop properties");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);f.owner.drop(true);
            require(f.owner.getMainHandItem().isEmpty(),"real selected-inventory drop removes its stack");
            for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())&&e instanceof ItemEntity item&&LbcTmtDrops.matches(item.getItem())){require(token==null,"one physical dropped TMT body");token=item;}
            require(token!=null&&token.getItem().getCount()==2&&game.items().specialDrops().owner(token)==f.owner,"actual thrower retained by shared native admission");f.own(token);next(0);
        }
        void next(int value){phase=value;when=game.server().getTickCount();}
        List<Entity> actors(){var actors=new ArrayList<Entity>();for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())&&!e.isRemoved()&&(e instanceof Vex||e instanceof Skeleton))actors.add(e);return actors;}
        void verifyActors(int batches){verifyActors(batches,0);}
        void verifyActors(int batches,int extraSkeletons){var actors=actors();long vexes=actors.stream().filter(e->e instanceof Vex).count(),skeletons=actors.stream().filter(e->e instanceof Skeleton).count();
            require(vexes==5L*batches&&skeletons==batches+extraSkeletons,"native source actor counts: vex="+vexes+", skeleton="+skeletons+", batches="+batches);for(var e:actors)f.own(e);}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<1200,"bounded native drop scene phase="+phase);
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(token.getItem().getCount()==2){require(elapsed<40,"ready native drop starts without a ground wait");return false;}
                require(!token.isRemoved()&&token.getItem().getCount()==1&&!token.onGround(),"two source effects debit one real airborne dropped item");verifyActors(1);firstActors.addAll(actors());
                require(!game.items().specialDrops().spawn(token)&&token.getItem().getCount()==1,"same native frame cannot debit or dispatch again");
                var stack=token.getItem();var data=stack.get(DataComponents.CUSTOM_DATA);var parsed=LbcTmtAbilities.read(stack);
                require(LbcTmtAbilities.invoke(stack,vex.id(),()->game.items().creatureDrops().ready(token,LbcCreatureDrops.Kind.VEX_OLD)),"real shared readiness callback admits its selected native property");
                require(stack.get(DataComponents.CUSTOM_DATA)==data&&LbcTmtAbilities.read(stack)==parsed,"read-only native callback retains original immutable component and parsed slots");
                f.pause();at=token.position();age=token.getAge();yaw=token.getYRot();savedItem=token.getItem().copy();
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcCreatureDropItem.Kind.OLD_SKELETON.item()));f.owner.drop(true);
                for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())&&e instanceof ItemEntity item&&item!=token&&LbcCreatureDropItem.matches(item.getItem(),LbcCreatureDropItem.Kind.OLD_SKELETON)){require(ordinary==null,"one ordinary source body");ordinary=item;}
                require(ordinary!=null&&game.items().specialDrops().owner(ordinary)==f.owner,"ordinary native token admitted during TimeStop");f.own(ordinary);next(1);return false;}
            if(phase==1){if(elapsed<3)return false;require(token.getItem().getCount()==1&&token.getAge()==age&&token.position().equals(at),"TimeStop holds remaining physical drop and dispatch");verifyActors(1);
                require(game.vanity().requestCapture(f.owner,false),"whole coordinator captures pending mixed native drop");next(2);return false;}
            if(phase==2){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;f.discard(token);f.discard(ordinary);for(var actor:firstActors)f.discard(actor);
                require(game.vanity().requestRestoreForCheck(),"whole restore accepts consumed/unloaded source bodies");restoring=true;next(3);return false;}
            if(phase==3){if(game.vanity().busy())return false;
                require(f.level.getEntity(token.getUUID())==token&&!token.isRemoved()&&ItemStack.matches(token.getItem(),savedItem)&&token.getAge()==age&&token.position().equals(at)&&token.getYRot()==yaw,"same native drop instance/UUID, item/private properties and physical cursor restored");
                require(game.items().specialDrops().owner(token)==f.owner&&game.items().timeStop().stopped(f.level),"real thrower and paused native dispatcher restored");
                require(f.level.getEntity(ordinary.getUUID())==ordinary&&!ordinary.isRemoved()&&ordinary.getItem().getCount()==1&&game.items().specialDrops().pendingCount()==2,"restored mixed and ordinary tokens rejoin the shared derived admission index");
                for(var actor:firstActors)require(f.level.getEntity(actor.getUUID())==actor&&!actor.isRemoved(),"prior native outputs restored independently of remaining item");verifyActors(1);
                game.vanity().stop();f.thaw();next(4);return false;}
            if(phase==4){if(!token.isRemoved()||!ordinary.isRemoved()){require(elapsed<40,"restored shared dispatcher resumes; pending="+game.items().specialDrops().pendingCount()+", stopped="+game.items().timeStop().stopped(f.level)+", frozen="+game.server().tickRateManager().isFrozen()+", stage="+game.stage()+", matched="+LbcTmtDrops.matches(token.getItem())+", age="+token.getAge()+", tags="+token.entityTags());return false;}verifyActors(2,1);return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();if(f.stopped)f.thaw();
                for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(e.getUUID())){f.own(e);game.items().entityUnloaded(e);e.discard();}
                f.owner.getStats().setValue(f.owner,dropStat,dropped);f.owner.getStats().setValue(f.owner,totalDropStat,totalDropped);
                f.owner.getStats().setValue(f.owner,ordinaryDropStat,ordinaryDropped);
            }finally{f.close();}}
    }
}
