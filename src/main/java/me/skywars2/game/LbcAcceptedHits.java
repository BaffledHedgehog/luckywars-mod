package me.skywars2.game;

import java.util.IdentityHashMap;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Fabric omits AFTER_DAMAGE on death. Both outcomes share one accepted-hit dispatcher. */
public final class LbcAcceptedHits {
    private record Receipt(DamageSource source,float amount){}
    private static final IdentityHashMap<LivingEntity,Receipt> pending=new IdentityHashMap<>();
    private LbcAcceptedHits(){}
    public static void clear(){pending.clear();}
    /** Native CombatTracker supplies the accepted, post-protection HP damage before death. */
    public static void record(LivingEntity victim,DamageSource source,float amount){
        if(amount>0&&Float.isFinite(amount)&&victim.isAlive())pending.put(victim,new Receipt(source,amount));
    }
    public static void lethal(LivingEntity victim,DamageSource source){
        var receipt=pending.remove(victim);
        if(receipt!=null&&receipt.source==source)dispatch(victim,source,receipt.amount,false);
    }
    public static void nonlethal(LivingEntity victim,DamageSource source,float amount,boolean blocked){
        var receipt=pending.get(victim);if(receipt!=null&&receipt.source==source)pending.remove(victim);
        if(!blocked&&amount>0)dispatch(victim,source,amount,true);
        else if(!blocked&&victim instanceof ServerPlayer player&&SkyWars2.game()!=null)SkyWars2.game().hurtForSkill(player);
    }
    private static void dispatch(LivingEntity victim,DamageSource source,float amount,boolean living){
        var game=SkyWars2.game();if(game==null)return;
        game.items().academy().damaged(victim,source);
        // Fatal native credit was settled by acceptedDamage/beforeDeath already.
        if(living)game.items().indirectKills().damaged(victim,source);
        game.items().damaged(victim,source);
        game.items().vexSword().damaged(victim,source,amount);
        if(source.getEntity() instanceof ServerPlayer owner)game.items().tmt().withHeldTraits(source,()->game.items().fartus().melee(owner,victim,source));
        game.items().tmt().withHeldTraits(source,()->game.items().wtf().melee(victim,source));
        if(victim instanceof ServerPlayer player){
            game.hurtForSkill(player);game.items().ak47().receivedDamage(player,amount);game.damaged(player,source,amount);
        }
    }
}
