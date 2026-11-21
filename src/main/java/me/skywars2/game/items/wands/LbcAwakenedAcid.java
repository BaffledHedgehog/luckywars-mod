package me.skywars2.game.items.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import me.skywars2.game.GameController;
import me.skywars2.game.academy.LbcAcademy;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.items.gadgets.LbcModeScroll;

/** Awakened staff input and source rifle counter; shared native acid owns all projectile work. */
public final class LbcAwakenedAcid {
    private static final String[] MODES={"mode_default","mode_caust","mode_gatling","mode_rifle"};
    private final GameController game;
    private record Rifle(ItemStack stack,InteractionHand hand,String epoch){}
    private final Map<ServerPlayer,Rifle> rifles=new IdentityHashMap<>();
    private final Map<ServerPlayer,ArrayDeque<DirectedRifle>> directedRifles=new IdentityHashMap<>();

    private static final class DirectedRifle {
        final ServerLevel level;
        final String epoch;
        final Vec3 origin, aim;
        final ItemStack weapon;
        final boolean ignited;
        int remaining=10;

        DirectedRifle(ServerLevel level, String epoch, Vec3 origin, Vec3 aim, ItemStack weapon, boolean ignited) {
            this.level=level;
            this.epoch=epoch;
            this.origin=origin;
            this.aim=aim;
            this.weapon=weapon.copyWithCount(1);
            this.ignited=ignited;
        }
    }
    private int cadence;
    public LbcAwakenedAcid(GameController game){this.game=game;}
    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Mth.clamp(data.getIntOr("mode",1),1,4);
        if(academy&&owner.isShiftKeyDown()){
            if(!LbcModeScroll.scrolling()&&owner.getXRot()>=89&&owner.getXRot()<=90){
                int tier=data.getIntOr("tier",0)==1?2:1;data.putInt("tier",tier);
                owner.sendSystemMessage(Component.translatable(tier==1?"mode_ignited_true":"mode_ignited_false"));
            }else{mode=LbcModeScroll.next(mode,4);data.putInt("mode",mode);owner.sendSystemMessage(Component.translatable(MODES[mode-1]));}
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("acidacid_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        boolean ignited=academy&&data.getIntOr("tier",0)==1;
        castMode(owner,hand,stack,academy?mode:1,ignited);
        if(!academy&&owner.isOnFire()&&LbcAcademy.trial(stack))game.items().academy().ready(owner,hand,stack);
        game.items().wands().wandCasted(owner);return true;
    }
    void castMode(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,boolean ignited){
        if(mode<=2)game.items().clouds().awakenedAcid(owner,owner.getEyePosition(),owner.getLookAngle(),mode==2?1:0,ignited);
        else if(mode==3) gatling(owner,owner.getEyePosition(),owner.getYRot(),owner.getXRot(),ignited,ItemStack.EMPTY);
        else{
            // The source counter belongs to the player and accumulates ten releases per paid cast.
            game.items().mana().score(owner,"acid_rifle").add(10);rifles.put(owner,new Rifle(stack,hand,game.items().epoch()));
        }
    }
    /** Detached helper release: rifle is one shot, never the paid ten-release queue. */
    void emitAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        Vec3 aim=direction.normalize();
        if(mode==3){
            // Sauvojen's Gatling selects the marked owner's rotation.
            float yaw=owner.getYRot();
            float pitch=owner.getXRot();
            for(int i=0;i<8;i++)game.items().clouds().awakenedAcid(owner,origin,
                    Vec3.directionFromRotation((Mth.floor(pitch*100)+owner.getRandom().nextInt(4001)-2000)*.01f,
                            (Mth.floor(yaw*100)+owner.getRandom().nextInt(6001)-3000)*.01f),2,false,i==0);
        }else if(mode==4) rifleShot(owner,origin,aim,false,ItemStack.EMPTY);
        else game.items().clouds().awakenedAcid(owner,origin,aim,mode==2?1:0,false);
    }

    private boolean gatling(ServerPlayer owner, Vec3 origin, float yaw, float pitch, boolean ignited, ItemStack weapon) {
        boolean released=false;
        for(int i=0;i<8;i++) {
            float spreadYaw=(Mth.floor(yaw*100)+owner.getRandom().nextInt(6001)-3000)*.01f;
            float spreadPitch=(Mth.floor(pitch*100)+owner.getRandom().nextInt(4001)-2000)*.01f;
            var shot=game.items().clouds().awakenedAcid(owner,origin,
                    Vec3.directionFromRotation(spreadPitch,spreadYaw),2,ignited,i==0,weapon);
            released|=!shot.isRemoved();
        }
        return released;
    }

    private void rifleShot(ServerPlayer owner, Vec3 origin, Vec3 aim, boolean ignited, ItemStack weapon) {
        game.items().clouds().awakenedAcid(owner,origin,aim,2,ignited,false,weapon);
        LbcOverload.sound(owner,origin,"minecraft:entity.item.break",1,2,0);
    }

    public boolean scriptCast(ServerPlayer owner, ItemStack weapon, Vec3 origin, Vec3 aim, int mode, boolean ignited) {
        if (mode==4) {
            directedRifles.computeIfAbsent(owner,ignored->new ArrayDeque<>())
                    .addLast(new DirectedRifle(owner.level(),game.items().epoch(),origin,aim,weapon,ignited));
            return true;
        }
        if (mode==3) {
            float yaw=aim.horizontalDistanceSqr()<1e-12?owner.getYRot():(float)-Math.toDegrees(Math.atan2(aim.x,aim.z));
            float pitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));
            return gatling(owner,origin,yaw,pitch,ignited,weapon);
        }
        return !game.items().clouds().awakenedAcid(owner,origin,aim,mode==2?1:0,ignited,true,weapon).isRemoved();
    }

    public int pending(ServerPlayer owner){return Math.max(0,game.items().mana().hudValue(owner,"acid_rifle"));}
    public void addPlayerCounter(ServerPlayer owner){
        game.items().mana().score(owner,"acid_rifle").add(1);
        rifles.putIfAbsent(owner,new Rifle(ItemStack.EMPTY,InteractionHand.MAIN_HAND,game.items().epoch()));
    }
    /** Raw consumable grants enter the same rifle owner/cadence as ordinary casts. */
    public void adoptRawFoodStatuses(ServerPlayer owner){
        if(pending(owner)>0&&!game.items().magic().rejectCustomEffect(owner))
            rifles.putIfAbsent(owner,new Rifle(ItemStack.EMPTY,InteractionHand.MAIN_HAND,game.items().epoch()));
    }

    public void tick(){
        boolean release=++cadence%5==0;
        for(var entry:List.copyOf(rifles.entrySet())){
            var owner=entry.getKey();var rifle=entry.getValue();
            if(!owner.isAlive()||owner.isRemoved()||!game.items().currentEpoch(rifle.epoch)||!game.items().mana().visible(owner)
                    ||game.items().magic().rejectCustomEffect(owner)){clear(owner);continue;}
            if(pending(owner)<=0){rifles.remove(owner);continue;}
            if(!release||game.items().timeStop().stopped(owner.level())||game.items().magic().suppressed(owner)
                    ||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())||game.items().actionsBlocked(owner))continue;
            boolean ignited=rifle.stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("tier",0)==1;
            rifleShot(owner,owner.getEyePosition(),owner.getLookAngle(),ignited,ItemStack.EMPTY);
            game.items().mana().score(owner,"acid_rifle").add(-1);
            if(pending(owner)<=0)rifles.remove(owner);
        }
        if (release) releaseDirectedRifles();
    }

    private void releaseDirectedRifles() {
        for (var entry=directedRifles.entrySet().iterator();entry.hasNext();) {
            var next=entry.next();
            var owner=next.getKey();
            var queue=next.getValue();
            var rifle=queue.getFirst();
            if (!owner.isAlive() || owner.isRemoved() || owner.level()!=rifle.level
                    || !game.items().currentEpoch(rifle.epoch) || !game.canUseGameplayItems(owner)
                    || game.stage()==GameController.Stage.ENDING || game.items().magic().rejectCustomEffect(owner)) {
                entry.remove();
                continue;
            }
            if (game.items().wands().inputTimeStopped(owner) || game.items().actionsBlocked(owner)
                    || game.items().magic().suppressed(owner)
                    || game.items().magic().suppressedAt(rifle.level,owner.getEyePosition())
                    || game.items().magic().suppressedAt(rifle.level,rifle.origin)
                    || !rifle.level.hasChunkAt(BlockPos.containing(rifle.origin))) continue;
            rifleShot(owner,rifle.origin,rifle.aim,rifle.ignited,rifle.weapon);
            if (--rifle.remaining==0) queue.removeFirst();
            if (queue.isEmpty()) entry.remove();
        }
    }

    public void cancelDirectedRifles(){directedRifles.clear();}
    public void clear(ServerPlayer owner){rifles.remove(owner);directedRifles.remove(owner);game.items().mana().score(owner,"acid_rifle").set(0);}
    public void stop(){for(var owner:List.copyOf(rifles.keySet()))clear(owner);cancelDirectedRifles();cadence=0;}
}
