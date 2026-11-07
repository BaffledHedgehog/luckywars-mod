package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Raw Rasengan snowball: the throw is native; its immediate spell belongs to the caster. */
public final class LbcRasengan {
    private final GameController game;
    private final Set<Snowball> pending=new LinkedHashSet<>();
    public LbcRasengan(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){
        if(!stack.is(Items.SNOWBALL))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        return data.getIntOr("gadget",0)==1&&data.getIntOr("rasengan",0)==1;
    }
    private boolean active(ServerPlayer p){
        return game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
                &&p.level()==game.maps().level()&&game.canUseGameplayItems(p)&&!game.isObserver(p)
                &&LbcDimensionItems.ordinary(p)&&!game.items().actionsBlocked(p);
    }
    /** Real private TMT input reuses the cast body; it needs no disposable snowball/tick workaround. */
    boolean use(ServerPlayer p,net.minecraft.world.InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!LbcTmtAbilities.active(stack)||ItemData.intValue(stack,"rasengan",0)!=1||ItemData.intValue(stack,"gadget",0)!=1
                ||!active(p)||game.items().wands().inputTimeStopped(p)||game.items().timeStop().frozen(p)
                ||!game.server().tickRateManager().runsNormally()||game.items().magic().rasenganSuppressed(p.level(),p.position()))return false;
        cast(p);stack.shrink(1);p.getInventory().setChanged();return true;
    }
    public void loaded(Entity entity){
        if(entity instanceof Snowball ball&&matches(ball.getItem())&&ball.getOwner() instanceof ServerPlayer p&&active(p)){
            game.items().markCurrentItemEpoch(ball);pending.add(ball);
        }
    }
    /** Entity-load callbacks only enqueue; world mutation belongs to the ordinary item tick. */
    public void tick(){
        for(var ball:List.copyOf(pending)){
            if(ball.isRemoved()||!game.items().currentEpoch(ball)||!(ball.getOwner() instanceof ServerPlayer p)||!active(p)){
                pending.remove(ball);continue;
            }
            if(!game.server().tickRateManager().runsNormally()||game.items().timeStop().frozen(ball)
                    ||game.items().timeStop().frozen(p)||ball.entityTags().contains("time_stopped")||p.entityTags().contains("time_stopped"))continue;
            pending.remove(ball);
            if(!game.items().magic().rasenganSuppressed(p.level(),p.position()))cast(p);
        }
    }
    /** (user) Repeated source cells accumulated knockback; damage occurs once per point. */
    static ExplosionDamageCalculator blastCalculator(int cell){
        if(cell<0||cell>=26)throw new IllegalArgumentException("Rasengan cell "+cell);
        int multiplier=cell<8?1:cell<11?2:6;
        return new ExplosionDamageCalculator(){
            @Override public float getKnockbackMultiplier(Entity target){return multiplier;}
        };
    }
    private void cast(ServerPlayer p){
        var level=p.level();var origin=p.position().add(0,1.62,0);
        var direction=Vec3.directionFromRotation(p.getXRot(),p.getYRot());
        // The source's nearby protection is measured from its positioned eye origin, not feet.
        for(var target:level.players())if(target.distanceToSqr(origin)<=16&&game.items().wands().validRoleTarget(target,false))
            target.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,3,false,false,false),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false,false),p);
        var cause=game.items().indirectKills().wildCause(p);
        var vanilla=p.damageSources().explosion(null,null);
        var named=new BlastDamage(vanilla);
        var damage=game.items().indirectKills().captureDamage(named,cause);
        for(int i=0;i<26;i++){
            var point=origin.add(direction.scale(i*.2));var calculator=blastCalculator(i);
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(point)))continue;
            game.items().indirectKills().trapChanges(level,cause,
                    ()->level.explode(null,damage,calculator,point,1,false,Level.ExplosionInteraction.MOB));
        }
        p.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,0,false,false,false),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,200,0,false,false,false),p);
    }
    private static final class BlastDamage extends DamageSource {
        BlastDamage(DamageSource vanilla){super(vanilla.typeHolder());}
        @Override public Component getLocalizedDeathMessage(LivingEntity target){
            return Component.translatable("death.attack.explosion.player",target.getDisplayName(),
                    Component.translatable("killed_by_rasengan").withStyle(ChatFormatting.AQUA,ChatFormatting.BOLD));
        }
    }
    static boolean blast(DamageSource source){return LbcIndirectKills.originalDamage(source) instanceof BlastDamage;}
    public int pendingCount(){return pending.size();}
    public void unloaded(Entity entity){pending.remove(entity);}
    public void forget(ServerPlayer p){pending.removeIf(ball->ball.getOwner()==p);}
    /** Vanity may cancel this short uncommitted throw, while native snowballs/effects remain canonical. */
    public void stop(){pending.clear();}
}
