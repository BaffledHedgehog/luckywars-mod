package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Awakened staff input and source rifle counter; shared native acid owns all projectile work. */
final class LbcAwakenedAcid {
    private static final String[] MODES={"mode_default","mode_caust","mode_gatling","mode_rifle"};
    private final GameController game;
    private record Rifle(ItemStack stack,InteractionHand hand,String epoch){}
    private final Map<ServerPlayer,Rifle> rifles=new IdentityHashMap<>();
    private int cadence;
    LbcAwakenedAcid(GameController game){this.game=game;}
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Mth.clamp(data.getIntOr("mode",1),1,4);
        if(academy&&owner.isShiftKeyDown()){
            if(owner.getXRot()>=89&&owner.getXRot()<=90){
                int tier=data.getIntOr("tier",0)==1?2:1;data.putInt("tier",tier);
                owner.sendSystemMessage(Component.translatable(tier==1?"mode_ignited_true":"mode_ignited_false"));
            }else{mode=mode%4+1;data.putInt("mode",mode);owner.sendSystemMessage(Component.translatable(MODES[mode-1]));}
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
        else if(mode==3){
            for(int i=0;i<8;i++){
                float yaw=(Mth.floor(owner.getYRot()*100)+owner.getRandom().nextInt(6001)-3000)*.01f;
                float pitch=(Mth.floor(owner.getXRot()*100)+owner.getRandom().nextInt(4001)-2000)*.01f;
                game.items().clouds().awakenedAcid(owner,owner.getEyePosition(),Vec3.directionFromRotation(pitch,yaw),2,ignited,i==0);
            }
        }else{
            // The source counter belongs to the player and accumulates ten releases per paid cast.
            game.items().mana().score(owner,"acid_rifle").add(10);rifles.put(owner,new Rifle(stack,hand,game.items().epoch()));
        }
    }
    /** Detached helper release: rifle is one shot, never the paid ten-release queue. */
    void emitAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        Vec3 aim=direction.normalize();
        if(mode==3){
            // Gatling explicitly selects the marked owner Rotation before universal_razbros.
            float yaw=owner.getYRot();
            float pitch=owner.getXRot();
            for(int i=0;i<8;i++)game.items().clouds().awakenedAcid(owner,origin,
                    Vec3.directionFromRotation((Mth.floor(pitch*100)+owner.getRandom().nextInt(4001)-2000)*.01f,
                            (Mth.floor(yaw*100)+owner.getRandom().nextInt(6001)-3000)*.01f),2,false,i==0);
        }else{
            game.items().clouds().awakenedAcid(owner,origin,aim,mode==2?1:mode==4?2:0,false,mode!=4);
            if(mode==4)LbcOverload.sound(owner,origin,"minecraft:entity.item.break",1,2,0);
        }
    }
    int pending(ServerPlayer owner){return Math.max(0,game.items().mana().hudValue(owner,"acid_rifle"));}
    void addPlayerCounter(ServerPlayer owner){
        game.items().mana().score(owner,"acid_rifle").add(1);
        rifles.putIfAbsent(owner,new Rifle(ItemStack.EMPTY,InteractionHand.MAIN_HAND,game.items().epoch()));
    }
    /** Raw consumable grants enter the same rifle owner/cadence as ordinary casts. */
    void adoptRawFoodStatuses(ServerPlayer owner){
        if(pending(owner)>0&&!game.items().magic().rejectCustomEffect(owner))
            rifles.putIfAbsent(owner,new Rifle(ItemStack.EMPTY,InteractionHand.MAIN_HAND,game.items().epoch()));
    }

    void tick(){
        boolean release=++cadence%5==0;
        for(var entry:List.copyOf(rifles.entrySet())){
            var owner=entry.getKey();var rifle=entry.getValue();
            if(!owner.isAlive()||owner.isRemoved()||!game.items().currentEpoch(rifle.epoch)||!game.items().mana().visible(owner)
                    ||game.items().magic().rejectCustomEffect(owner)){clear(owner);continue;}
            if(pending(owner)<=0){rifles.remove(owner);continue;}
            if(!release||game.items().timeStop().stopped(owner.level())||game.items().magic().suppressed(owner)
                    ||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())||game.items().actionsBlocked(owner))continue;
            boolean ignited=rifle.stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("tier",0)==1;
            game.items().clouds().awakenedAcid(owner,owner.getEyePosition(),owner.getLookAngle(),2,ignited,false);
            LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:entity.item.break",1,2,0);
            game.items().mana().score(owner,"acid_rifle").add(-1);
            if(pending(owner)<=0)rifles.remove(owner);
        }
    }
    void clear(ServerPlayer owner){rifles.remove(owner);game.items().mana().score(owner,"acid_rifle").set(0);}
    void stop(){for(var owner:List.copyOf(rifles.keySet()))clear(owner);cadence=0;}
}
