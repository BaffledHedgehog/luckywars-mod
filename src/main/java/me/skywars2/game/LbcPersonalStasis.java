package me.skywars2.game;

import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/** Source time_stopped_lol is a personal pose anchor, independent of world time stopping. */
public final class LbcPersonalStasis {
    public static final String TAG="time_stopped_lol";
    // Source default: this personal anchor lasts until the current life ends.
    public static final int DEFAULT_DURATION_TICKS=-1;
    private static final AttachmentType<CompoundTag> DATA=AttachmentRegistry.create(
            Identifier.parse("skywars2:personal_stasis"),b->b.persistent(CompoundTag.CODEC));
    private final GameController game;
    public LbcPersonalStasis(GameController game){this.game=game;}
    public static void register(){}
    public boolean active(ServerPlayer p){return p.getAttached(DATA)!=null;}
    public int remaining(ServerPlayer p){var data=p.getAttached(DATA);return data==null?0:data.getIntOr("ticks",-1);}
    public void start(ServerPlayer p,int ticks){
        if(ticks==0||ticks< -1)throw new IllegalArgumentException("stasis ticks");
        if(!p.isAlive()||p.isRemoved()||LbcParasite.active(p))return;
        var data=new CompoundTag();data.putString("epoch",game.items().epoch());
        data.putString("dimension",p.level().dimension().identifier().toString());
        data.putDouble("x",p.getX());data.putDouble("y",p.getY());data.putDouble("z",p.getZ());
        data.putFloat("yaw",p.getYRot());data.putFloat("pitch",p.getXRot());data.putInt("ticks",ticks);
        p.setAttached(DATA,data);p.entityTags().add(TAG);
        // The source initial application clears effects before its first recurring frame.
        p.removeAllEffects();
    }
    public void clear(ServerPlayer p){p.removeAttached(DATA);p.entityTags().remove(TAG);}
    public void tick(ServerPlayer p){
        var data=p.getAttached(DATA);if(data==null)return;
        if(LbcParasite.active(p)){clear(p);return;}
        if(!p.isAlive()||p.isRemoved()||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||!game.items().currentEpoch(data.getStringOr("epoch",""))){clear(p);return;}
        // Freeze pauses the existing anchor; it neither moves the pose nor spends its remaining duration.
        if(!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(p.level())||game.items().wands().timeStopped()
                ||game.items().wands().inputTimeStopped(p)||game.items().polymorph().masked(p))return;
        if(p.level()!=game.maps().level()||!p.level().dimension().identifier().toString().equals(data.getStringOr("dimension","")))return;
        if(!game.canUseGameplayItems(p)||game.isObserver(p)||!game.items().wands().validRoleTarget(p,false))return;
        p.entityTags().add(TAG);
        p.removeAllEffects();
        p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.WEAKNESS,20,127,false,false),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,20,127,false,false),p);
        double x=data.getDoubleOr("x",p.getX()),y=data.getDoubleOr("y",p.getY()),z=data.getDoubleOr("z",p.getZ());
        float yaw=data.getFloatOr("yaw",p.getYRot()),pitch=data.getFloatOr("pitch",p.getXRot());
        if(p.getX()!=x||p.getY()!=y||p.getZ()!=z||p.getYRot()!=yaw||p.getXRot()!=pitch){
            // The shared TeleportMomentum mixin retains velocity on both sides of this correction.
            p.teleportTo(p.level(),x,y,z,Set.of(),yaw,pitch,true);
        }
        int ticks=data.getIntOr("ticks",-1);
        if(ticks>0){if(ticks==1)clear(p);else{var next=data.copy();next.putInt("ticks",ticks-1);p.setAttached(DATA,next);}}
    }
}
