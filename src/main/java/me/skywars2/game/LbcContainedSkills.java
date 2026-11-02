package me.skywars2.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

/** Effect conversion and one-server-tick VIP damage receipts; no inventory or NBT polling. */
public final class LbcContainedSkills {
    private final GameController game;
    private final Set<ServerPlayer> converting=new HashSet<>();
    private final Map<ServerPlayer,Damage> damage=new HashMap<>();
    private record Damage(int tick,double amount) { }
    public LbcContainedSkills(GameController game){this.game=game;}

    /** Useful reversed Slowness speeds the player up and is not a slowdown to convert. */
    public boolean convertsSlowness(ServerPlayer player,MobEffectInstance effect){
        return effect.is(MobEffects.SLOWNESS)&&effect.getAmplifier()>=0&&EffectLayers.live(effect)
                &&game.lbc().hasSkill(player,1);
    }
    /** True means the slowdown was consumed, including when stronger existing resistance wins. */
    public boolean acceptSlowness(ServerPlayer player,MobEffectInstance effect,Entity source){
        if(converting.contains(player)||!convertsSlowness(player,effect))return false;
        converting.add(player);
        try{
            MobEffectInstance kept=null,tail=null;
            for(var node=effect;node!=null;node=((EffectLayers.Access)node).skywars2$layerHidden()){
                if(node.getAmplifier()>=0&&EffectLayers.live(node))convert(player,node,source);
                else if(EffectLayers.live(node)){
                    var copy=new MobEffectInstance(node);((EffectLayers.Access)copy).skywars2$layerHidden(null);
                    if(kept==null)kept=copy;else ((EffectLayers.Access)tail).skywars2$layerHidden(copy);tail=copy;
                }
            }
            if(kept!=null)player.addEffect(kept,source);
        }finally{converting.remove(player);}
        return true;
    }
    private void convert(ServerPlayer player,MobEffectInstance effect,Entity source){
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,effect.getDuration(),
                Math.min(3,effect.getAmplifier()),effect.isAmbient(),effect.isVisible(),effect.showIcon()),source);
    }
    /** Loaded/forced native chains and selection are reconciled before the first effect action. */
    public void beforeEffects(ServerPlayer player){
        if(converting.contains(player)||!game.lbc().hasSkill(player,1))return;
        var head=player.getEffect(MobEffects.SLOWNESS);if(head==null)return;
        boolean found=false;
        for(var node=head;node!=null;node=((EffectLayers.Access)node).skywars2$layerHidden())
            if(node.getAmplifier()>=0&&EffectLayers.live(node)){found=true;break;}
        if(!found)return;
        converting.add(player);
        try{
            MobEffectInstance kept=null,tail=null;
            for(var node=head;node!=null;node=((EffectLayers.Access)node).skywars2$layerHidden()){
                if(node.getAmplifier()>=0&&EffectLayers.live(node))convert(player,node,player);
                else{
                    var copy=new MobEffectInstance(node);((EffectLayers.Access)copy).skywars2$layerHidden(null);
                    if(kept==null)kept=copy;else ((EffectLayers.Access)tail).skywars2$layerHidden(copy);tail=copy;
                }
            }
            var owners=game.items().ownedEffects();owners.purgePositiveSlowness(player);
            var saved=owners.suspend(player);
            try{player.removeEffect(MobEffects.SLOWNESS);if(kept!=null)player.addEffect(kept,player);}
            finally{owners.resume(player,saved);}
        }finally{converting.remove(player);}
    }

    private java.util.UUID attacker(ServerPlayer victim,DamageSource source){
        var inherited=game.items().indirectKills().babylonCause(source);
        if(inherited!=null)return inherited.player();
        var owner=LbcIndirectKills.playerSource(source.getEntity());
        if(owner==null)owner=LbcIndirectKills.playerSource(source.getDirectEntity());
        // Explicit self damage wins over an older opponent's credit. Otherwise preserve
        // native last-attacker attribution, including a fall caused by an earlier hit.
        if(owner==null&&(source.getEntity()==victim||source.getDirectEntity()==victim))return victim.getUUID();
        if(owner==null)owner=LbcIndirectKills.playerSource(victim.getKillCredit());
        return owner==null?null:owner.getUUID();
    }
    /** Native CombatTracker records the final, unclamped HP damage before death dispatch. */
    public void recordDamage(ServerPlayer player,DamageSource source,float amount){
        if(!(amount>0)||!game.lbc().hasSkill(player,12))return;
        var owner=attacker(player,source);if(owner==null||owner.equals(player.getUUID()))return;
        int now=game.server().getTickCount();var old=damage.get(player);
        double sum=old!=null&&old.tick==now?old.amount:0;
        damage.put(player,new Damage(now,Math.min(Double.MAX_VALUE,sum+amount)));
    }
    /** Refund the ordinary death debit once. The native death, drops and respawn remain intact. */
    public boolean protectsDeath(ServerPlayer player,DamageSource source){
        var receipt=damage.remove(player);
        if(receipt==null||receipt.tick!=game.server().getTickCount()||receipt.amount<player.getMaxHealth()
                ||!game.config().lbcSkills||game.players().get(player.getUUID()).lbcSkill!=12
                ||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||player.level()!=game.maps().level()||!LbcDimensionItems.ordinary(player))return false;
        var owner=attacker(player,source);return owner!=null&&!owner.equals(player.getUUID());
    }
    double damageThisTick(ServerPlayer player){
        var receipt=damage.get(player);
        return receipt!=null&&receipt.tick==game.server().getTickCount()?receipt.amount:0;
    }
    public void tick(){
        int now=game.server().getTickCount();
        damage.entrySet().removeIf(entry->entry.getValue().tick!=now||entry.getKey().isRemoved());
    }
    public void clear(ServerPlayer player){damage.remove(player);converting.remove(player);}
    public void stop(){damage.clear();converting.clear();}
}
