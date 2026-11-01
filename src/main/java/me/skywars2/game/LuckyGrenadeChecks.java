package me.skywars2.game;

import java.lang.reflect.Method;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Focused native use/flight contacts; reuses the ordinary disposable match fixture. */
public final class LuckyGrenadeChecks {
    private LuckyGrenadeChecks(){}
    private static Fixture running;
    private static boolean observerInstalled;
    private static void observe(Entity entity){var fixture=running;if(fixture!=null)fixture.loaded(entity);}
    private static double baseDamage(Arrow arrow){try{var field=net.minecraft.world.entity.projectile.arrow.AbstractArrow.class.getDeclaredField("baseDamage");field.setAccessible(true);return field.getDouble(arrow);}catch(ReflectiveOperationException error){throw new IllegalStateException("native arrow base damage",error);}}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Lucky grenade: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final ServerPlayer p;
        final LbcHedgehogSpikes service;final List<Marker> fields=new ArrayList<>();final Map<Marker,Map<BlockPos,MapManager.BlockSnapshot>> fieldCells=new IdentityHashMap<>();
        final Map<net.minecraft.world.item.Item,Integer> stats=new IdentityHashMap<>();
        final RegenerationBeltChecks.ScheduledClient client;final RegenerationBeltChecks.Snapshot saved;
        final MatchRandomState random,playerRandom;
        Arrow flight;Cow victim;Vec3 contact;int phase,started,observed;boolean collecting,closed,ownerOkay,epochOkay;
        ItemStack observedStack;
        Fixture(GameController game){this.game=game;var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null,"native participant");saved=new RegenerationBeltChecks.Snapshot(game,owner);
            base=new AllGrimoireChecks.Fixture(game);p=base.base.owner;service=game.items().hedgehogSpikes();
            random=MatchRandomState.capture(base.level.getRandom());playerRandom=MatchRandomState.capture(p.getRandom());client=new RegenerationBeltChecks.ScheduledClient(game,p);
            require(service.trackedCount()==0,"exclusive native transport");
            for(var i:List.of(Items.SNOWBALL,LbcGadgetItems.LUCKY_GRENADE))stats.put(i,p.getStats().getValue(Stats.ITEM_USED.get(i)));
        }
        void reset(){collecting=false;for(var m:List.copyOf(fields))remove(m);service.stop();base.reset();client.acknowledge();
            p.getInventory().clearContent();game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}
        Marker field(String tag,Vec3 at){var m=new Marker(EntityTypes.MARKER,base.level);m.setPos(at);m.entityTags().add(tag);var cells=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();for(var pos:List.of(m.blockPosition(),m.blockPosition().above()))cells.put(pos,game.maps().snapshot(base.level,pos));fieldCells.put(m,cells);
            require(base.level.addFreshEntity(m),"native suppression marker");fields.add(m);game.items().magic().loaded(m);return m;}
        void remove(Marker m){game.items().magic().unloaded(m);m.discard();fields.remove(m);var cells=fieldCells.remove(m);if(cells!=null)cells.forEach((pos,snapshot)->{game.maps().restoreSnapshot(base.level,pos,snapshot);game.items().magic().blockChanged(pos,base.level.getBlockState(pos));});}
        ItemStack held(InteractionHand hand,boolean raw){var loot=game.lbc().rollTable(base.level,p.position(),p,"lbc:ported/lucky_grenade");require(loot.size()==1,"canonical singleton");var s=loot.getFirst();
            if(raw){var original=s;s=s.transmuteCopy(Items.SNOWBALL);s.set(DataComponents.CUSTOM_DATA,original.get(DataComponents.CUSTOM_DATA));}
            s.setCount(2);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(hand,s);return s;}
        boolean input(InteractionHand hand,boolean raw){
            client.acknowledge();
            // Raw compatibility is admitted by the real Fabric callback before vanilla snowball use.
            return raw?net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,base.level,hand).consumesAction():base.base.input(hand);
        }
        List<ItemEntity> tokens(){var out=new ArrayList<ItemEntity>();for(var e:base.level.getAllEntities())
            if(!base.base.before.contains(e)&&!e.isRemoved()&&e instanceof ItemEntity token)out.add(token);return out;}
        void loaded(Entity entity){
            if(!collecting||!(entity instanceof ItemEntity token)||token.position().distanceToSqr(contact)>1
                    ||LbcFartus.data(token.getItem()).getIntOr("drop",0)!=1)return;
            observed++;observedStack=token.getItem().copy();ownerOkay=game.items().specialDrops().owner(token)==p;epochOkay=game.items().currentEpoch(token);
            // Observe the native full table output before the shared dispatcher can consume it.
            // Retire this fixture-owned random token at admission, avoiding random boss/terrain work.
            token.discard();
        }
        void tokensOwned(){require(observed==1&&observedStack!=null&&!observedStack.isEmpty(),"exactly one real native full-table trap token admitted");
            require(ownerOkay&&epochOkay,"admitted trap token retains author/epoch before dispatcher");}
        Arrow shot(Vec3 at){var arrow=service.emitLegacy(p,10,at,new Vec3(0,0,2));require(arrow!=null,"native grenade launch");
            require(baseDamage(arrow)==.01,"source .01 native damage");return arrow;}
        void hands(){for(boolean raw:new boolean[]{false,true})for(var hand:InteractionHand.values()){
            reset();var mag=field("stopper_magic",p.position());require(game.items().magic().suppressed(p),"ordinary MAG positive control");
            var s=held(hand,raw);int used=p.getStats().getValue(Stats.ITEM_USED.get(s.getItem()));int mana=game.items().mana().get(p);
            require(LbcHedgehogSpikes.legacyThrow(s)==10&&input(hand,raw)&&s.getCount()==1,"native/raw actual used-hand debit under MAG "+hand+" raw="+raw);
            require(service.trackedCount()==1&&service.projectiles().getFirst().getOwner()==p&&game.items().mana().get(p)==mana
                    &&p.getStats().getValue(Stats.ITEM_USED.get(s.getItem()))==used+1,"one shared native owner arrow/stat and no mana debit");
            var contents=service.projectiles().getFirst().getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS);
            require(contents!=null&&contents.customColor().orElse(-1)==16772608&&contents.customEffects().isEmpty(),"source tint without leaked Raid Omen carrier");remove(mag);
        }}
        void denied(){reset();var s=held(InteractionHand.MAIN_HAND,false);var tech=field("stopper",p.position());
            require(!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2&&service.trackedCount()==0,"TECH launch denies debit");remove(tech);
            p.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            require(game.items().magic().artifactSuppressedAt(base.level,p.position())&&!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2,"strong antimagic launch denies debit");
            p.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            game.items().timeStop().begin(base.level,40);try{require(!input(InteractionHand.MAIN_HAND,false)&&s.getCount()==2,"time stop launch denies debit");}finally{game.items().timeStop().end(base.level);}
        }
        void duplicateBlockCallback(Arrow arrow,Vec3 at){try{Method m=arrow.getClass().getDeclaredMethod("onHitBlock",BlockHitResult.class);m.setAccessible(true);
            m.invoke(arrow,new BlockHitResult(at,Direction.NORTH,BlockPos.containing(at),false));
        }catch(ReflectiveOperationException error){throw new IllegalStateException("native callback overlap probe",error);}}
        void contactStage(int next){
            reset();phase=next;observed=0;observedStack=null;ownerOkay=false;epochOkay=false;
            if(next==0){field("stopper_magic",p.position());victim=new Cow(EntityTypes.COW,base.level);victim.setPos(base.at.add(0,0,4));victim.setNoAi(true);victim.setNoGravity(true);
                require(base.level.addFreshEntity(victim),"real native living target");contact=victim.position();flight=shot(contact.add(0,.5,-1));
            }else{var block=BlockPos.containing(base.at.add(0,1,4));base.level.setBlock(block,Blocks.STONE.defaultBlockState(),2);contact=Vec3.atCenterOf(block);
                flight=shot(new Vec3(block.getX()+.5,block.getY()+.5,block.getZ()-1));
                if(next==2)field("stopper",contact);
                if(next==3){p.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}
            }
            collecting=true;started=game.server().getTickCount();
        }
        void initialize(){hands();denied();contactStage(0);}
        boolean tick(){client.acknowledge();int elapsed=game.server().getTickCount()-started;require(elapsed<=6,"bounded actual scheduled native arrow impact phase="+phase);
            if(!flight.isRemoved())return false;require(elapsed>=1,"impact occurs on a real later server tick");
            if(phase<=1){if(phase==0)require(victim.getHealth()<victim.getMaxHealth()&&!victim.hasEffect(MobEffects.RAID_OMEN),"scheduled accepted physical living hit without Raid");
                tokensOwned();duplicateBlockCallback(flight,contact);tokensOwned();
            }else require(observed==0&&tokens().isEmpty(),"scheduled impact rechecks "+(phase==2?"TECH":"strong antimagic")+" after launch");
            collecting=false;if(phase==3)return true;contactStage(phase+1);return false;
        }
        @Override public void close(){if(closed)return;closed=true;collecting=false;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{for(var m:List.copyOf(fields))remove(m);service.stop();},client::close,base::close,()->saved.restore(game),saved::restoreAttributes,saved::restoreUse,()->{stats.forEach((item,n)->p.getStats().setValue(p,Stats.ITEM_USED.get(item),n));random.restore(base.level.getRandom());playerRandom.restore(p.getRandom());game.items().magic().inventoryChanged(p);game.players().save();}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("lucky grenade scoped restore",failure);
        }
    }
    public static void start(GameController game){require(running==null,"exclusive finite trial");if(!observerInstalled){net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)->observe(entity));observerInstalled=true;}
        var fixture=new Fixture(game);running=fixture;try{fixture.initialize();SkyWars2.LOGGER.info("[check] START lucky-grenade");}catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS lucky-grenade (native/raw hands, MAG/TECH/strong artifact, source .01/tint/no Raid, scheduled native entity+block impacts, one full-table causal token observed before retirement/dispatcher, overlapping callbacks)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL lucky-grenade",error);}}
}
