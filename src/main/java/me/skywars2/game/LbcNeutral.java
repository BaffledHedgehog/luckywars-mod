package me.skywars2.game;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Five original native projectile families; no replacement movement or synthetic entity clock. */
public final class LbcNeutral {
    public static final String PROJECTILE="skywars2.wand.neutral";
    private static final String[] MODES={"cast_wither_skulls","cast_small_fireballs","cast_big_fireball","cast_fireworks","cast_potions"};
    private static final CompoundTag[] ROCKETS=new CompoundTag[3];
    private static final CompoundTag POTION;
    private static final CompoundTag BIG_BALL=parse("{ExplosionPower:2}");
    static {
        try(var input=LbcNeutral.class.getResourceAsStream("/skywars2/neutral_wand_presets.json")){
            if(input==null)throw new IllegalStateException("Missing neutral native presets");
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(int i=0;i<3;i++)ROCKETS[i]=TagParser.parseCompoundFully(json.getAsJsonArray("rockets").get(i).getAsString());
            POTION=TagParser.parseCompoundFully(json.get("potion").getAsString());
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    private final GameController game;
    private final Set<Projectile> projectiles=new LinkedHashSet<>();
    LbcNeutral(GameController game){this.game=game;}
    public static boolean managed(Entity e){return e instanceof Projectile&&e.entityTags().contains(PROJECTILE);}
    List<Projectile> snapshot(){return List.copyOf(projectiles);}
    private boolean active(ServerPlayer p){return game.items().mana().visible(p)&&!game.items().actionsBlocked(p)
            &&!p.entityTags().contains("time_stopped")&&!game.items().timeStop().stopped(p.level())
            &&!game.items().magic().suppressed(p)&&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());}
    boolean use(ServerPlayer p,InteractionHand hand,ItemStack stack,boolean academy){
        if(!active(p))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Math.max(1,Math.min(6,data.getIntOr("mode",1)));
        if(academy&&p.isShiftKeyDown()){
            mode=mode%6+1;data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.getInventory().setChanged();
            p.sendSystemMessage(mode==6?Component.translatableWithFallback("mode_alternating","Чередующийся"):Component.translatable(MODES[mode-1]));return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("neutral_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(p,cost))return false;
        boolean alternating=academy&&mode==6;
        if(alternating)mode=Math.max(1,Math.min(5,data.getIntOr("alternating_next",1)));
        if(!academy)mode=p.getRandom().nextInt(5)+1;
        cast(p,mode);
        if(alternating){
            var updated=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            updated.putInt("alternating_next",mode%5+1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(updated));p.getInventory().setChanged();
        }
        if(!academy&&LbcAcademy.trial(stack)){
            int combo=data.getIntOr("type_combo",-1)==mode?data.getIntOr("score",0)+1:0;
            data.putInt("type_combo",mode);data.putInt("score",combo);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.getInventory().setChanged();
            if(combo>=2)game.items().academy().ready(p,hand,stack);
        }
        // Legacy sound targets only the caster, unlike ordinary nearby wand sound broadcasts.
        librarySound(p);
        game.items().wands().wandCasted(p);return true;
    }
    void librarySound(ServerPlayer p){
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.WITHER_SHOOT),net.minecraft.sounds.SoundSource.MASTER,
                p.getX(),p.getY(),p.getZ(),1,1,p.getRandom().nextLong()));
    }
    boolean feather(ServerPlayer p){
        if(!active(p))return false;
        p.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,1200,0),p);
        LbcOverload.sound(p,p.position(),"minecraft:entity.ender_dragon.flap",1,2,0);
        p.level().sendParticles(new DustParticleOptions(0xFFFFFF,1),p.getX(),p.getY(),p.getZ(),10,.3,.1,.3,0);
        // Its source function has neither mana debit nor cast_wand/mage hooks.
        return true;
    }
    void cast(ServerPlayer p,int mode){
        int[] sides=mode==3?new int[]{0}:mode==4?new int[]{-1,0,1}:new int[]{0,1,-1};
        Vec3 forward=p.getLookAngle(),left=Vec3.directionFromRotation(0,p.getYRot()-90);
        for(int i=0;i<sides.length;i++){
            int side=sides[i];Projectile shot=switch(mode){
                case 1->new WitherSkull(EntityTypes.WITHER_SKULL,p.level());
                case 2->new SmallFireball(EntityTypes.SMALL_FIREBALL,p.level());
                case 3->{var ball=new LargeFireball(EntityTypes.FIREBALL,p.level());load(ball,BIG_BALL);yield ball;}
                case 4->{var rocket=new FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET,p.level());load(rocket,ROCKETS[i]);yield rocket;}
                case 5->{var potion=new ThrownSplashPotion(EntityTypes.SPLASH_POTION,p.level());load(potion,POTION);yield potion;}
                default->throw new IllegalArgumentException("Neutral mode "+mode);
            };
            Vec3 pos=p.getEyePosition().add(forward).add(left.scale(side*.1));
            if(!p.level().hasChunkAt(BlockPos.containing(pos)))continue;
            double factor=mode==1||mode==5?500:mode==4?100:300;
            Vec3 marker=forward.scale(10).add(left.scale(side));
            shot.setPos(pos);shot.setDeltaMovement(new Vec3(Mth.floor(marker.x*factor)*.001,Mth.floor(marker.y*factor)*.001,Mth.floor(marker.z*factor)*.001));
            shot.setOwner(p);LbcShooters.orient(shot);shot.entityTags().add(PROJECTILE);shot.entityTags().add("magic");shot.entityTags().add("activated");
            if(mode==2){shot.entityTags().add("can_deleted");game.items().wands().adoptFire(p,shot);}
            game.items().markCurrentItemEpoch(shot);
            if(p.level().addFreshEntity(shot))projectiles.add(shot);
        }
    }
    private static CompoundTag parse(String value){try{return TagParser.parseCompoundFully(value);}catch(Exception e){throw new IllegalStateException(e);}}
    private static void load(Entity entity,CompoundTag tag){entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),tag));}
    public boolean canAffect(Projectile shot,Entity target){return shot.getOwner() instanceof ServerPlayer p&&game.items().currentEpoch(shot)&&game.items().wands().projectileTarget(p,target);}
    void loaded(Entity e){if(managed(e)){if(game.playing()&&game.items().currentEpoch(e))projectiles.add((Projectile)e);else e.discard();}}
    void unloaded(Entity e){if(e instanceof Projectile p&&projectiles.remove(p)&&!p.isRemoved())p.discard();}
    void forget(ServerPlayer p){for(var shot:List.copyOf(projectiles))if(shot.getOwner()==p)unloaded(shot);}
    void tick(){for(var shot:List.copyOf(projectiles))if(shot.isRemoved()||!game.playing()||!game.items().currentEpoch(shot)||!shot.level().hasChunkAt(shot.blockPosition()))unloaded(shot);}
    void stop(){for(var shot:List.copyOf(projectiles))unloaded(shot);}

    LbcCloneSkills.ActorImage cloneImage(Entity source) {
        if(!managed(source))return null;
        return CloneComplexProjectileStates.image(e->projectiles.add((Projectile)e),e->projectiles.remove(e));
    }

}
