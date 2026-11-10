package me.skywars2.game;

import java.util.IdentityHashMap;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Fabric omits AFTER_DAMAGE on death. Both outcomes share one accepted-hit dispatcher.
 * Death changes delivery timing only: retain the original damage source and every owner's
 * ordinary hit filters. A credited spell kill must never become a synthetic melee attack.
 */
public final class LbcAcceptedHits {
    private static final class Receipt {
        final DamageSource source;final float amount;boolean dispatched,stanceDispatched,nativeDeath;
        Receipt(DamageSource source,float amount){this.source=source;this.amount=amount;}
    }
    private static final IdentityHashMap<LivingEntity,Receipt> pending=new IdentityHashMap<>();
    private LbcAcceptedHits(){}
    public static void clear(){pending.clear();}
    /** Native CombatTracker supplies the accepted, post-protection HP damage before death. */
    public static void record(LivingEntity victim,DamageSource source,float amount){
        if(amount>0&&Float.isFinite(amount)&&victim.isAlive())pending.put(victim,new Receipt(source,amount));
    }
    /** Only hurtServer's native die call has an outgoing hit criterion after it.
     * Direct HP removal also records combat damage but must not invent that criterion.
     */
    public static void beforeNativeDeath(LivingEntity victim,DamageSource source){
        var receipt=pending.get(victim);
        if(receipt!=null&&receipt.source==source)receipt.nativeDeath=true;
    }
    public static void lethal(LivingEntity victim,DamageSource source){
        var receipt=pending.get(victim);
        if(receipt!=null&&receipt.source==source&&!receipt.dispatched){
            receipt.dispatched=true;
            // The native outgoing criterion runs after die(), when player roles have
            // already been reset. Deliver these target-sensitive skill hooks before it.
            if(receipt.nativeDeath&&source.getEntity() instanceof ServerPlayer)stanceHit(victim,source);
            dispatch(victim,source,receipt.amount,false);
        }
    }
    /** The same unblocked native criterion also includes resisted, nonlethal hits. */
    public static void stanceHit(LivingEntity victim,DamageSource source){
        var receipt=pending.get(victim);
        if(receipt!=null&&receipt.source==source){
            if(receipt.stanceDispatched)return;
            receipt.stanceDispatched=true;
        }
        var game=SkyWars2.game();if(game==null)return;
        // Match the native criterion's empty parent-spell scope; keep every source,
        // ownership, team, target, suppression and live-owner filter in the skill owner.
        try(var scope=game.items().spellSources().scope((LbcSpellSource.Source)null)){
            game.items().championSkill().damaged(victim,source);
            game.items().manaVampireSkill().damaged(victim,source);
        }
    }
    public static void finished(LivingEntity victim){pending.remove(victim);}
    public static void nonlethal(LivingEntity victim,DamageSource source,float amount,boolean blocked){
        var receipt=pending.get(victim);if(receipt!=null&&receipt.source==source)pending.remove(victim);
        if(receipt!=null&&receipt.source==source&&receipt.dispatched)return;
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
