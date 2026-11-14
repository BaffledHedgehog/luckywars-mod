package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;

/** Actual native bow draws, with one unfinished burst per owner and no catch-up debt. */
public final class LbcMachinegunSkill {
    public static final String TAG="skywars2.machinegun";
    public static final int DELIVERY_BUDGET=128;
    private static final long DELIVERY_NANOS=4_000_000;
    public static final int BURSTS_PER_ADDITIONAL_ARROW=20;
    private final GameController game;
    private final Map<UUID,Draw> draws=new HashMap<>();
    private final ArrayDeque<Draw> lanes=new ArrayDeque<>();

    private static final class Draw {
        final ServerPlayer owner;
        final ItemStack bow;
        final InteractionHand hand;
        final int slot;
        int callbackTick=Integer.MIN_VALUE,remaining;
        Draw(ServerPlayer owner,ItemStack bow){
            this.owner=owner;this.bow=bow;hand=owner.getUsedItemHand();slot=owner.getInventory().getSelectedSlot();
        }
    }
    public LbcMachinegunSkill(GameController game){this.game=game;}

    public static int interval(int progress){return progress>=19?1:20-Math.max(0,progress);}
    public static int arrowsPerBurst(int progress){return arrowsPerBurst(progress,BURSTS_PER_ADDITIONAL_ARROW);}
    /** User: slower growth, one extra arrow after twenty saturated completed bursts. */
    public static int arrowsPerBurst(int progress,int burstsPerAdditionalArrow){
        return (int)Math.min(Integer.MAX_VALUE,1L+Math.max(0L,(long)progress-19)/Math.max(1,burstsPerAdditionalArrow));
    }
    /** Preserve the authored late discontinuity instead of silently adding a speed cap. */
    public static double speed(int charge){return (long)charge+200>=10000?10:((long)charge+200)*.005;}
    public static int spread(int charge){return (int)Math.max(0L,Math.min(Integer.MAX_VALUE,2000L-charge));}
    public static double component(double direction,int charge,int jitter){
        return (Math.floor(direction*speed(charge)*10000)+jitter)*.0001;
    }
    int charge(ServerPlayer p){return game.items().mana().score(p,"bow_charge").get();}
    int progress(ServerPlayer p){return game.items().mana().score(p,"bow_charge_sub").get();}
    int pending(ServerPlayer p){var draw=draws.get(p.getUUID());return draw==null?0:draw.remaining;}
    private boolean paused(ServerPlayer p){return game.server().tickRateManager().isFrozen()
            ||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(p.level())
            ||game.items().timeStop().frozen(p)||p.entityTags().contains("time_stopped");}
    private boolean valid(Draw draw){var p=draw.owner;
        return game.lbc().hasSkill(p,14)&&p.isUsingItem()&&p.getUsedItemHand()==draw.hand
                &&p.getUseItem()==draw.bow&&p.getItemInHand(draw.hand)==draw.bow&&!draw.bow.isEmpty()
                &&draw.bow.getItem() instanceof BowItem
                &&(draw.hand!=InteractionHand.MAIN_HAND||p.getInventory().getSelectedSlot()==draw.slot);
    }

    /** Called from LivingEntity.updateUsingItem with the actual used stack, in either hand. */
    public void usingBow(ServerPlayer p,ItemStack bow){
        if(MatchPlayerCheckpoint.restoring(p))return;
        if(!(bow.getItem() instanceof BowItem)||!game.lbc().hasSkill(p,14)){clear(p);return;}
        var draw=draws.get(p.getUUID());
        if(draw!=null&&!valid(draw)){clear(p);draw=null;}
        if(draw==null){draw=new Draw(p,bow);draws.put(p.getUUID(),draw);lanes.addLast(draw);}
        if(!valid(draw)){clear(p);return;}
        int now=game.server().getTickCount();
        if(paused(p)||draw.callbackTick==now)return;
        draw.callbackTick=now;
        var score=game.items().mana().score(p,"bow_charge");
        int q=score.get()==Integer.MAX_VALUE?Integer.MAX_VALUE:Math.max(0,score.get())+1;score.set(q);
        int interval=interval(progress(p));
        if(draw.remaining==0&&Math.floorMod(q,interval)==interval-1&&ammo(p)!=null){
            draw.remaining=arrowsPerBurst(progress(p));
        }
    }

    /** Fair owner lanes bound CPU and entity publication; unfinished bursts suppress new debt. */
    public void tick(){
        int budget=DELIVERY_BUDGET,visits=lanes.size();long start=System.nanoTime();
        while(budget>0&&visits-->0&&!lanes.isEmpty()){
            var draw=lanes.removeFirst();
            if(draws.get(draw.owner.getUUID())!=draw)continue;
            if(!valid(draw)){clear(draw.owner);continue;}
            lanes.addLast(draw);
            if(!paused(draw.owner)&&draw.remaining>0){
                if(shoot(draw.owner)){
                    draw.remaining--;budget--;
                    if(draw.remaining==0){var s=game.items().mana().score(draw.owner,"bow_charge_sub");
                        if(s.get()<Integer.MAX_VALUE)s.set(Math.max(0,s.get())+1);}
                }else draw.remaining=0;
            }
            if(visits==0&&budget>0&&lanes.stream().anyMatch(d->d.remaining>0&&!paused(d.owner)))visits=lanes.size();
            if(System.nanoTime()-start>=DELIVERY_NANOS)break;
        }
    }
    private ItemStack weapon(ServerPlayer p){
        if(p.isUsingItem()&&p.getUseItem().getItem() instanceof BowItem)return p.getUseItem();
        for(var hand:InteractionHand.values())if(p.getItemInHand(hand).getItem() instanceof BowItem)return p.getItemInHand(hand);
        return ItemStack.EMPTY;
    }
    private ItemStack ammo(ServerPlayer p){return ammo(p,weapon(p));}
    private ItemStack ammo(ServerPlayer p,ItemStack weapon){
        if(!(weapon.getItem() instanceof BowItem bow))return null;
        // Native held-first selection, without the creative fallback's fabricated normal arrow.
        var held=ProjectileWeaponItem.getHeldProjectile(p,bow.getSupportedHeldProjectiles());
        if(!held.isEmpty()&&held.getItem() instanceof ArrowItem)return held;
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++){
            var stack=p.getInventory().getItem(slot);
            if(!stack.isEmpty()&&stack.getItem() instanceof ArrowItem&&bow.getAllSupportedProjectiles().test(stack))return stack;
        }
        return null;
    }
    private boolean shoot(ServerPlayer p){
        var weapon=weapon(p);var ammo=ammo(p,weapon);if(ammo==null)return false;
        var fired=weapon.copy();
        int q=charge(p),spread=spread(q);var random=p.getRandom();var direction=p.getLookAngle();
        // q is nonnegative for real draws: spread is at most 2000 and nextInt cannot overflow.
        var motion=new Vec3(component(direction.x,q,random.nextInt(spread*2+1)-spread),
                component(direction.y,q,random.nextInt(spread*2+1)-spread),
                component(direction.z,q,random.nextInt(spread*2+1)-spread));
        var arrow=((ArrowItem)ammo.getItem()).createArrow(p.level(),ammo,p,fired);double damage=2;
        arrow.setPos(p.getEyePosition());arrow.entityTags().add(TAG);arrow.entityTags().add("killedarrow");
        arrow.setBaseDamage(damage);arrow.setDeltaMovement(motion);
        game.items().projectileLevelups().beforePublish(p,arrow,LbcProjectileLevelups.Launch.MACHINEGUN);
        LbcShooters.orient(arrow);game.items().markCurrentItemEpoch(arrow);
        game.items().timeStop().prepareFrozen(arrow);
        if(!p.level().addFreshEntity(arrow))return false;
        game.items().lifetimes().track(arrow);
        if(random.nextBoolean()){ammo.shrink(1);p.getInventory().setChanged();}
        boolean traitsRolled=false;
        if(ItemData.intValue(fired,"explosive",0)==1){
            var ballistic=java.util.List.<net.minecraft.world.entity.projectile.Projectile>of(arrow);game.items().trinkets().shot(p,ballistic);traitsRolled=true;
            if(game.items().balance().explosiveBow(p,fired,ballistic))return true;
        }
        if(LbcChargedBows.matches(fired)){
            var ballistic=java.util.List.<net.minecraft.world.entity.projectile.Projectile>of(arrow);if(!traitsRolled)game.items().trinkets().shot(p,ballistic);
            game.items().chargedBows().release(p,fired,p.getTicksUsingItem(),ballistic);return true;
        }
        var oldFamily=game.items().balance().augment(p,fired,java.util.List.of(arrow));
        var family=game.items().shulkerBow().augment(p,fired,oldFamily);
        game.items().madokaBow().augment(p,fired,family);
        if(!traitsRolled)game.items().trinkets().shot(p,family);
        return true;
    }
    public static boolean managed(Entity actor){return actor!=null&&actor.entityTags().contains(TAG);}
    public boolean loaded(Entity actor){
        if(!managed(actor))return false;
        if(!game.playing()||actor.level()!=game.maps().level()||!game.items().currentEpoch(actor)){actor.discard();return true;}
        if(actor instanceof AbstractArrow arrow)game.items().lifetimes().track(arrow);
        game.items().timeStop().prepareFrozen(actor);return true;
    }
    /** Narrow native-arrow tick hook also covers arrows restored by full match rewind. */
    public void arrowTick(AbstractArrow arrow){
        if(!managed(arrow)||arrow.isRemoved())return;
        if(!game.playing()||!game.items().currentEpoch(arrow)||arrow.level()!=game.maps().level()){arrow.discard();return;}
        if(game.items().timeStop().stopped((net.minecraft.server.level.ServerLevel)arrow.level())
                ||game.items().timeStop().frozen(arrow))return;
        if(!((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$isInGround())game.items().lifetimes().advance(arrow,arrow.tickCount);
    }
    public void clear(ServerPlayer p){
        if(MatchPlayerCheckpoint.restoring(p))return;
        var draw=draws.remove(p.getUUID());
        if(draw==null&&!game.lbc().hasSkill(p,14))return;
        if(draw!=null)lanes.remove(draw);
        game.items().mana().score(p,"bow_charge").set(0);game.items().mana().score(p,"bow_charge_sub").set(0);
    }
    public void clear(){for(var draw:java.util.List.copyOf(draws.values()))clear(draw.owner);}
    /** Core restores canonical scoreboard phase; short unfinished emissions may be canceled. */
    public void clearTransient(){draws.clear();lanes.clear();}
}
