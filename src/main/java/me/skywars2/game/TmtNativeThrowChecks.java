package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Real item-use factories, native projectile ownership and whole-coordinator source-copy replay. */
public final class TmtNativeThrowChecks {
    private TmtNativeThrowChecks(){}
    private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT native throws: "+why);}
    static void run(GameController game){require(running==null,"exclusive native throw scene");var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    static void tick(GameController game){var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();
        SkyWars2.LOGGER.info("[check] PASS tmt-native-throws (all16 wool colors in one family, actual native item-use launches independent wool+bucket source copies/debits, original held properties and real owners, native TimeStop, whole coordinator actor/private-context restoration and continued flight)");
    }catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tmt-native-throws",error);}}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final HashSet<UUID> initial=new HashSet<>();
        final Map<net.minecraft.stats.Stat<Item>,Integer> stats=new LinkedHashMap<>();
        Snowball wool,bucket;ItemStack held;CustomData data;Vec3 woolAt,bucketAt;int woolAge,bucketAge,phase,when,started;boolean restoring,closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,true);for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            var items=new ArrayList<Item>(LbcWoolTrailItems.tokens().values());items.addAll(List.of(LbcThrownUtilityItems.RUCHKA,LbcThrownUtilityItems.HONEW,LbcThrownUtilityItems.THROWABLE_METAL_PIPE,LbcThrownUtilityItems.BUCKET_PROJECTILE,LbcThrownUtilityItems.THROWABLE_PUMPKIN,LbcBridgeBuilderItem.TOKEN));
            for(int i=0;i<5;i++)items.add(LbcTmtItems.prototype(i).getItem());for(var item:items){var stat=net.minecraft.stats.Stats.ITEM_USED.get(item);stats.put(stat,f.owner.getStats().getValue(stat));}}
        LbcTmtAbilities.Ability property(Item item){String id=BuiltInRegistries.ITEM.getKey(item).toString();return game.items().tmt().useProfiles().families().stream().flatMap(row->row.variants().stream())
                .filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readString(LbcTmtAbilities.NATIVE,"").equals(id)).findFirst().orElseThrow();}
        void next(int value){phase=value;when=game.server().getTickCount();}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();f.move(f.owner,f.home.add(0,500,0));
            var ids=new HashSet<String>();for(var item:LbcWoolTrailItems.tokens().values())ids.add(property(item).id());require(ids.size()==1,"all sixteen authored colors share one lottery family");
            var color=property(LbcWoolTrailItems.tokens().get(DyeColor.RED));var helmet=property(LbcThrownUtilityItems.BUCKET_PROJECTILE);require(!color.id().equals(helmet.id()),"two distinct native properties");
            held=LbcTmtItems.prototype(0);held.set(DataComponents.MAX_STACK_SIZE,16);held.setCount(3);var root=held.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(root,List.of(color,helmet));held.set(DataComponents.CUSTOM_DATA,CustomData.of(root));data=held.get(DataComponents.CUSTOM_DATA);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,held);require(f.owner.gameMode.useItem(f.owner,f.level,held,InteractionHand.MAIN_HAND).consumesAction(),"actual used-hand input admits both native factories");
            for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())&&e instanceof Snowball ball){
                f.own(ball);if(game.items().balance().trailColor(ball)==DyeColor.RED){require(wool==null,"one native wool projectile");wool=ball;}
                if(game.items().thrownUtilities().state(ball)!=null&&game.items().thrownUtilities().state(ball).kind()==LbcThrownUtilities.Kind.BUCKET){require(bucket==null,"one native bucket projectile");bucket=ball;}
            }
            require(wool!=null&&bucket!=null&&wool!=bucket&&held.getCount()==1&&f.owner.getMainHandItem()==held,"two native outputs and their two real consumable debits");
            require(held.get(DataComponents.CUSTOM_DATA)==data&&LbcTmtAbilities.read(held).size()==2&&!LbcTmtAbilities.nativePredicatesActive(),"complete outer source and scope cleanup");verify();
            f.pause();woolAt=wool.position();bucketAt=bucket.position();woolAge=wool.tickCount;bucketAge=bucket.tickCount;next(0);}
        void verify(){require(wool.getOwner()==f.owner&&bucket.getOwner()==f.owner,"actual native causal thrower");
            require(game.items().balance().trailColor(wool)==DyeColor.RED&&game.items().thrownUtilities().state(bucket)!=null&&game.items().thrownUtilities().state(bucket).kind()==LbcThrownUtilities.Kind.BUCKET,"independent native source owners");
            require(LbcBalanceExtras.woolColor(wool.getItem())==DyeColor.RED&&LbcThrownUtilities.kind(bucket.getItem())==LbcThrownUtilities.Kind.BUCKET&&LbcThrownUtilities.kind(wool.getItem())==null&&!LbcWoolTrailItems.matches(bucket.getItem()),"captured private source context cannot impersonate its sibling");}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-started<1000,"bounded native factory replay phase="+phase);
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<3)return false;require(wool.tickCount==woolAge&&bucket.tickCount==bucketAge&&wool.position().equals(woolAt)&&bucket.position().equals(bucketAt),"native TimeStop holds both real projectiles");
                require(game.vanity().requestCapture(f.owner,false),"whole coordinator captures native factory outputs");next(1);return false;}
            if(phase==1){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;f.discard(wool);f.discard(bucket);require(game.vanity().requestRestoreForCheck(),"whole coordinator accepts retired source bodies");restoring=true;next(2);return false;}
            if(phase==2){if(game.vanity().busy())return false;require(f.level.getEntity(wool.getUUID())==wool&&f.level.getEntity(bucket.getUUID())==bucket&&!wool.isRemoved()&&!bucket.isRemoved(),"canonical native body identities restored");verify();
                require(wool.tickCount==woolAge&&bucket.tickCount==bucketAge&&wool.position().equals(woolAt)&&bucket.position().equals(bucketAt)&&ItemStack.matches(f.owner.getMainHandItem(),held),"native flight cursor and complete remaining inventory source restored");
                game.vanity().stop();f.thaw();next(3);return false;}
            if(phase==3){if(elapsed<2)return false;verify();require(wool.tickCount>woolAge&&bucket.tickCount>bucketAge,"both native factories continue real flight after thaw");return true;}return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();if(f.stopped)f.thaw();for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(e.getUUID()))f.discard(e);
                stats.forEach((stat,value)->f.owner.getStats().setValue(f.owner,stat,value));
            }finally{f.close();}}
    }
}
