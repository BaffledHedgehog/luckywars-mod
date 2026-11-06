package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ShulkerBowMixin;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Real native dispenser publication and swept contacts, sharing the existing scoped fixture. */
public final class OwnerlessImbaArrowChecks {
    private static Scene running;
    private OwnerlessImbaArrowChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Ownerless IMBA arrows: "+why);}
    /** Shared real dispenser input for native arrow qualification; no synthetic projectile constructor. */
    static Arrow dispense(WoolBucketChecks.Fixture f,BlockPos dispenser,ItemStack ammo,Vec3 at){
        var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
        var container=(DispenserBlockEntity)f.level.getBlockEntity(dispenser);require(container!=null,"actual dispenser block entity");
        container.clearContent();container.setItem(0,ammo.copyWithCount(2));
        var state=f.level.getBlockState(dispenser);state.tick(f.level,dispenser,f.level.getRandom());
        var published=new ArrayList<Arrow>();for(var e:f.level.getAllEntities())if(e instanceof Arrow a&&!before.contains(a.getUUID()))published.add(a);
        require(published.size()==1&&container.getItem(0).getCount()==1,"native dispenser publishes/debits exactly one arrow: "+ammo.getItem()+" published="+published.size()+" remaining="+container.getItem(0).getCount());
        var a=published.getFirst();require(a.getOwner()==null&&ItemStack.isSameItemSameComponents(a.getPickupItemStackOrigin(),ammo),"true unowned source ammo ");
        f.own(a);a.setPos(at);a.setNoGravity(true);a.setDeltaMovement(Vec3.ZERO);
        return a;
    }
    public static void run(GameController game){
        require(running==null,"exclusive scene");var s=new Scene(game);running=s;
        try{s.begin();}catch(Throwable error){running=null;try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        var s=running;if(s==null)return;
        try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS ownerless-imba-arrows (8 actual native dispenser inputs, swept living/block contacts, null author, shared reactive/nuclear/vacuum/paralysis/electrical payloads, elapsed arc lifetime, TimeStop/TECH/MAG, cloning and owner transfer)");}
        catch(Throwable error){running=null;try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL ownerless-imba-arrows",error);}
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final Set<UUID> initial=new HashSet<>();
        final MatchRandomState worldRandom;final Collection<?> traces;final Vec3 high;final BlockPos dispenser;
        final List<ItemStack> ammo=new ArrayList<>();final List<Arrow> shots=new ArrayList<>();
        int when;boolean closed;Cow electricalTarget;Arrow electric;
        Scene(GameController game){
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);high=f.home.add(0,500,0);dispenser=BlockPos.containing(f.home.add(0,20,0));
            worldRandom=MatchRandomState.capture(f.level.getRandom());for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            traces=(Collection<?>)WoolBucketChecks.read(game.items().wands(),"electroTraces");
            require(traces.isEmpty()&&game.items().nukeEvent().jobCount()==0&&game.items().nukeEvent().markerCount()==0,"exclusive shared owners");
        }
        void begin(){
            f.prepareParticipants();f.move(f.owner,high.add(0,250,0));f.move(f.other,high.add(12,250,0));
            f.owner.getAbilities().invulnerable=true;f.other.getAbilities().invulnerable=true;
            var names=List.of("strong","weak","poison","reactive","explosive","evoker","paralyzing","electric");
            for(var name:names)ammo.add(f.leaf("imba_"+name+"_arrow").copyWithCount(1));
            f.set(dispenser,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.SOUTH));
            for(int i=0;i<ammo.size();i++){var a=dispense(i,high.add(0,i*20,0));shots.add(a);}
            direct();reactive();vacuum();nuclear();lifecycle();electrical();when=game.server().getTickCount();
        }
        Arrow dispense(int kind,Vec3 at){var a=OwnerlessImbaArrowChecks.dispense(f,dispenser,ammo.get(kind),at);require(game.items().quiver().arrows().contains(a),"native load registers ownerless IMBA state "+kind);return a;}
        Cow cow(Vec3 at){var c=EntityTypes.COW.create(f.level,EntitySpawnReason.COMMAND);require(c!=null,"native cow");c.setPos(at);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);require(f.level.addFreshEntity(c),"native target");f.own(c);return c;}
        void strike(Arrow a,Cow c){a.setPos(c.position().add(0,.5,-2));a.setDeltaMovement(0,0,4);a.tick();}
        void direct(){
            var strong=cow(high.add(0,170,0));shots.get(0).setCritArrow(true);strike(shots.get(0),strong);require(!strong.isAlive(),"unowned native high critical damage saturates safely");f.discard(strong);
            require(((ShulkerBowMixin.ArrowAccess)shots.get(1)).skywars2$baseDamage()==1,"unowned weak base damage retained");
            var poison=cow(high.add(0,180,0));strike(shots.get(2),poison);
            require(poison.hasEffect(MobEffects.POISON)&&poison.getEffect(MobEffects.POISON).isInfiniteDuration()&&poison.hasEffect(MobEffects.WITHER)&&!poison.hasEffect(MobEffects.RAID_OMEN),"unowned native poison transport consumed");
            require(game.items().acidCorruption().deathRemaining(poison)==450,"ownerless source delayed death450 seconds");
            require(poison.getLastDamageSource().getEntity()==shots.get(2),"native ownerless arrow remains its own damage source, without fabricated player credit");
            game.items().acidCorruption().clear(poison);f.discard(poison);
            game.items().acidCorruption().arrowDeath(f.other,450,null);game.items().acidCorruption().adoptRawFoodStatuses(f.other);
            var deaths=(Map<?,?>)WoolBucketChecks.read(game.items().acidCorruption(),"deaths");
            require(deaths.containsKey(f.other)&&deaths.get(f.other)==null,"raw-status adoption preserves an explicitly ownerless delayed death");game.items().acidCorruption().clear(f.other);
            var para=cow(high.add(0,190,0));para.setNoAi(false);strike(shots.get(6),para);
            require(para.isNoAi()&&para.isInvulnerable()&&para.isNoGravity()&&para.isSilent()&&para.getActiveEffects().isEmpty(),"unowned nonplayer paralysis");f.discard(para);
        }
        void reactive(){
            var point=high.add(0,210,0);var target=cow(point.add(0,0,8));var a=shots.get(3);a.setPos(point);a.setDeltaMovement(Vec3.ZERO);
            game.items().quiver().beforeArrow(a);var tnts=game.items().quiver().children().stream().filter(e->e instanceof PrimedTnt&&!e.isRemoved()).toList();
            require(tnts.size()==1,"unowned reactive native TNT");var tnt=(PrimedTnt)tnts.getFirst();
            require(tnt.getFuse()==0&&tnt.getOwner()==null&&game.items().quiver().cause(tnt)==null&&tnt.getDeltaMovement().equals(Vec3.ZERO),"native zero-fuse TNT keeps null author");
            require(game.items().quiver().explosionTarget(tnt,target),"ownerless child retains valid-target policy");f.discard(tnt);
            f.pause();try{var p=a.position();a.tick();require(a.position().equals(p)&&game.items().quiver().children().stream().noneMatch(e->!e.isRemoved()),"TimeStop holds ownerless physical body and reactive output");}finally{f.thaw();}
            f.field(true,point);try{game.items().quiver().beforeArrow(a);require(game.items().quiver().children().stream().noneMatch(e->!e.isRemoved()),"TECH blocks reactive output");}finally{f.removeFields();}
            f.discard(a);f.discard(target);
        }
        void vacuum(){
            var point=Vec3.atCenterOf(dispenser).add(0,5,0);var mob=cow(point.add(4,0,0));
            var item=new ItemEntity(f.level,point.x+5,point.y,point.z,new ItemStack(Items.DIAMOND,3));item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);require(f.level.addFreshEntity(item),"loose target");f.own(item);
            var protectedBody=cow(point.add(6,0,0));protectedBody.entityTags().add("strict_map_object");var saved=protectedBody.position();
            var wall=BlockPos.containing(point.add(0,0,2));f.set(wall,Blocks.STONE.defaultBlockState());var a=shots.get(5);a.setPos(point);a.setDeltaMovement(0,0,4);a.tick();
            require(a.isRemoved()&&mob.position().distanceToSqr(item.position())<1e-9&&mob.position().distanceToSqr(point)<5,"actual swept native wall contact activates ownerless WILD pull");
            require(protectedBody.position().equals(saved),"service role protected from ownerless vacuum");
            var terminal=mob.position();game.items().quiver().ground(a);require(mob.position().equals(terminal),"removed terminal cannot replay");
            for(var e:List.of(mob,item,protectedBody))f.discard(e);
        }
        void nuclear(){
            var point=high.add(0,400,0);var a=shots.get(4);a.setPos(point);game.items().quiver().ground(a);
            int jobs=game.items().nukeEvent().jobCount();require(a.isRemoved()&&jobs>0,"ownerless ground terminal enters shared nuclear engine");game.items().quiver().ground(a);require(game.items().nukeEvent().jobCount()==jobs,"no duplicate nuclear terminal");game.items().nukeEvent().stop();
            var victim=cow(point);var hit=dispense(4,point.add(0,.5,-2));strike(hit,victim);
            require(game.items().nukeEvent().jobCount()>0&&victim.getHealth()<990&&!victim.hasEffect(MobEffects.RAID_OMEN),"ownerless nuclear native contact includes immediate first wave");
            require(victim.getLastDamageSource().getEntity()==null,"nuclear damage has no fabricated shooter");game.items().nukeEvent().stop();f.discard(hit);f.discard(victim);
        }
        void lifecycle(){
            var a=dispense(6,high.add(0,460,0));game.items().quiver().beforeArrow(a);var image=game.items().quiver().cloneImage(a);require(image!=null,"ownerless native clone authority image");
            var copy=dispense(6,a.position().add(1,0,0));image.install(a,copy,Map.of(a,copy));require(game.items().quiver().arrows().contains(copy),"ownerless clone state installs without player");
            for(int i=0;i<298;i++)game.items().quiver().beforeArrow(copy);require(!copy.isRemoved(),"clone preserves age through299");game.items().quiver().beforeArrow(copy);require(copy.isRemoved(),"source300 active tick expiry");f.discard(a);
            var reflected=dispense(7,high.add(0,470,0));reflected.setOwner(f.owner);game.items().quiver().beforeArrow(reflected);
            var target=cow(reflected.position().add(0,0,6));game.items().quiver().ground(reflected);require(target.getLastDamageSource()!=null&&target.getLastDamageSource().getEntity()==f.owner,"real ownership transfer replaces null source");traces.clear();f.discard(target);
            var guarded=dispense(5,high.add(0,490,0));var control=cow(guarded.position().add(3,0,0));var old=control.position();
            f.field(true,guarded.position());try{game.items().quiver().ground(guarded);require(guarded.isRemoved()&&control.position().equals(old),"TECH suppresses ownerless terminal");}finally{f.removeFields();}f.discard(control);
        }
        void electrical(){
            var point=new Vec3((dispenser.getX()>>4)*16+8,high.y+520,(dispenser.getZ()>>4)*16+8);electricalTarget=cow(point.add(0,0,3));electric=shots.get(7);electric.setPos(point);electric.setDeltaMovement(Vec3.ZERO);
            f.field(false,point);try{game.items().quiver().ground(electric);require(electric.isRemoved()&&electricalTarget.getHealth()==984,"MAG leaves physical electrical terminal intact");}finally{f.removeFields();}
            require(electricalTarget.getLastDamageSource().getEntity()==null&&traces.size()==3,"ownerless native chain and three accepted arcs");
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int age=game.server().getTickCount()-when;
            require(age<60,"bounded elapsed arc scene");if(age<3)return false;
            if(age==3)require(!traces.isEmpty(),"accepted ownerless arcs survive real ticks after source removal in the resident chunk");
            if(age<13)return false;require(traces.isEmpty(),"accepted arcs end at native ten active ticks");return true;
        }
        public void close(){
            if(closed)return;closed=true;
            try{game.items().nukeEvent().stop();game.items().acidCorruption().clear(f.other);traces.clear();var fresh=new ArrayList<Entity>();for(var e:f.level.getAllEntities())fresh.add(e);
                for(var e:fresh)if(!initial.contains(e.getUUID())){if(e instanceof LivingEntity living)game.items().acidCorruption().clear(living);f.own(e);}}
            finally{try{f.close();}finally{worldRandom.restore(f.level.getRandom());}}
        }
    }
}
