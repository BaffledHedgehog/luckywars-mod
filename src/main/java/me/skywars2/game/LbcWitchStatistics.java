package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Mutable Witch progress: actual melee HP loss and native mushroom mining, never lifetime totals. */
public final class LbcWitchStatistics {
    private LbcWitchStatistics(){}
    /** Proof travels with the real native impact through deferred Time Stop damage; no per-tick hit ledger. */
    private static final class Melee extends net.minecraft.world.damagesource.DamageSource {
        final net.minecraft.world.damagesource.DamageSource original;final java.util.UUID victim;
        Melee(net.minecraft.world.damagesource.DamageSource source,net.minecraft.world.entity.Entity target){
            super(source.typeHolder(),source.getDirectEntity(),source.getEntity());original=source;victim=target.getUUID();
        }
        @Override public net.minecraft.world.phys.Vec3 getSourcePosition(){return original.getSourcePosition();}
        @Override public net.minecraft.world.phys.Vec3 sourcePositionRaw(){return original.sourcePositionRaw();}
        @Override public boolean scalesWithDifficulty(){return original.scalesWithDifficulty();}
        @Override public net.minecraft.world.item.ItemStack getWeaponItem(){return original.getWeaponItem();}
        @Override public net.minecraft.network.chat.Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity target){return original.getLocalizedDeathMessage(target);}
    }
    /** Only the native attack/stab/sweep hurt call creates this proof, never a spell's player_attack alias. */
    public static net.minecraft.world.damagesource.DamageSource nativeMelee(net.minecraft.world.entity.player.Player attacker,
            net.minecraft.world.entity.Entity victim,net.minecraft.world.damagesource.DamageSource source){
        return attacker instanceof ServerPlayer player&&source.getEntity()==player&&source.getDirectEntity()==player?new Melee(source,victim):source;
    }
    /** The native weapon/death-message delegate shares the accepted source checkpoint graph. */
    static net.minecraft.world.damagesource.DamageSource checkpointWrappedSource(net.minecraft.world.damagesource.DamageSource source){return source instanceof Melee melee?melee.original:null;}
    public static ObjectiveCriteria criterion(String name){return switch(name){
        case "dmg_dealt"->ObjectiveCriteria.DUMMY;
        case "lolidefecation"->Stats.BLOCK_MINED.get(Blocks.MUSHROOM_STEM);
        case "lucky_block_count"->Stats.BLOCK_MINED.get(Blocks.RED_MUSHROOM_BLOCK);
        default->ObjectiveCriteria.DUMMY;
    };}
    private static String name(Stat<?> stat){
        if(stat==Stats.CUSTOM.get(Stats.DAMAGE_DEALT))return "dmg_dealt";
        if(stat==Stats.BLOCK_MINED.get(Blocks.MUSHROOM_STEM)||stat==Stats.BLOCK_MINED.get(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI)))return "lolidefecation";
        if(stat==Stats.BLOCK_MINED.get(Blocks.RED_MUSHROOM_BLOCK)||stat==Stats.BLOCK_MINED.get(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL)))return "lucky_block_count";
        return null;
    }
    /** Create before vanilla forAllObjectives, so the first native award is counted too. */
    public static void prepare(ServerPlayer player,Stat<?> stat){
        var game=SkyWars2.game();var name=name(stat);if(game!=null&&name!=null)game.items().mana().score(player,name);
    }
    /** Worlds created by older mod versions have dummy objectives. Preserve them and add only this event. */
    public static void awarded(ServerPlayer player,Stat<?> stat,int count){
        // DAMAGE_DEALT also has non-melee producers, and its attack total includes item side effects.
        // The combat tracker below owns this one objective; vanilla statistics stay untouched.
        if(stat==Stats.CUSTOM.get(Stats.DAMAGE_DEALT))return;
        var game=SkyWars2.game();var name=name(stat);if(game==null||name==null)return;
        var objective=player.level().getScoreboard().getObjective(name);
        if(objective!=null&&(objective.getCriteria()==ObjectiveCriteria.DUMMY
                ||stat==Stats.BLOCK_MINED.get(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI))
                ||stat==Stats.BLOCK_MINED.get(LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL))))game.items().mana().score(player,name).add(count);
    }
    public static void reset(ServerPlayer player,Stat<?> stat){
        var game=SkyWars2.game();var name=name(stat);if(game==null||name==null)return;
        var objective=player.level().getScoreboard().getObjective(name);
        if(objective!=null&&objective.getCriteria()==ObjectiveCriteria.DUMMY)game.items().mana().score(player,name).set(0);
    }
    /** Called before native HP subtraction, after armor, effects and absorption. Also covers sweeps/death. */
    public static void meleeDamage(net.minecraft.world.entity.LivingEntity victim,
                                   net.minecraft.world.damagesource.DamageSource source,float damage){
        var game=SkyWars2.game();
        if(game==null||!game.playing()||!(source.getEntity() instanceof ServerPlayer player)
                ||source.getDirectEntity()!=player||!game.canUseGameplayItems(player)
                ||game.isObserver(player)||!(damage>0)||!Float.isFinite(damage))return;
        if(!(LbcIndirectKills.originalDamage(source) instanceof Melee melee)||!melee.victim.equals(victim.getUUID()))return;
        int units=Math.round(Math.min(damage,Math.max(0,victim.getHealth()))*10f);
        if(units>0){var score=game.items().mana().score(player,"dmg_dealt");
            score.set((int)Math.min(Integer.MAX_VALUE,(long)score.get()+units));}
    }
}
