package me.skywars2.game;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

/** Item-specific legacy blast shields. No temporary effects and no protection from unrelated damage. */
public final class ExplosionItemProtection {
    private ExplosionItemProtection(){}
    private static final String MARK="skywars2.item_blast_shield",ENTRY="skywars2.item_blast_shield.";
    public record Shield(Map<UUID,Integer> levels){
        public Shield{levels=Map.copyOf(levels);}
        public Shield and(UUID player,int level){var copy=new HashMap<>(levels);copy.merge(player,level,Math::max);return new Shield(copy);}
    }
    private record Pending(Entity emitter,DamageSource damage,Shield shield){}
    private record Active(DamageSource damage,Shield shield){}
    private static final ThreadLocal<Pending> PENDING=new ThreadLocal<>();
    private static final ThreadLocal<Active> ACTIVE=new ThreadLocal<>();
    public static Shield owner(UUID owner,int level){return new Shield(owner==null?Map.of():Map.of(owner,level));}
    public static Shield area(GameController game,ServerLevel world,Vec3 origin,double radius,int level){
        var selected=new HashMap<UUID,Integer>();
        for(var player:world.players())if(player.distanceToSqr(origin)<=radius*radius&&game.items().wands().validRoleTarget(player,false))selected.put(player.getUUID(),level);
        return new Shield(selected);
    }
    /** Durable native tags keep the activation-time recipients through delayed TNT, cloning and rewind. */
    public static void stamp(Entity actor,Shield shield){
        if(shield.levels.isEmpty())return;actor.entityTags().add(MARK);
        shield.levels.forEach((player,level)->actor.entityTags().add(ENTRY+level+"."+player));
    }
    private static Shield stamped(Entity actor){
        if(actor==null||!actor.entityTags().contains(MARK))return null;
        var selected=new HashMap<UUID,Integer>();
        for(var tag:actor.entityTags())if(tag.startsWith(ENTRY))try{
            int delimiter=tag.indexOf('.',ENTRY.length());int level=Integer.parseInt(tag.substring(ENTRY.length(),delimiter));
            if(level>0)selected.merge(UUID.fromString(tag.substring(delimiter+1)),level,Math::max);
        }catch(IllegalArgumentException|IndexOutOfBoundsException ignored){}
        return new Shield(selected);
    }
    public static void with(Entity emitter,Shield shield,Runnable blast){with(new Pending(emitter,null,shield),blast);}
    public static void with(DamageSource damage,Shield shield,Runnable blast){with(new Pending(null,damage,shield),blast);}
    private static void with(Pending next,Runnable blast){var previous=PENDING.get();PENDING.set(next);try{blast.run();}finally{restore(PENDING,previous);}}
    private static <T> void restore(ThreadLocal<T> local,T previous){if(previous==null)local.remove();else local.set(previous);}
    private static boolean contains(DamageSource source,DamageSource expected){
        for(var next=source;next!=null;next=LbcIndirectKills.checkpointWrappedSource(next))if(next==expected)return true;
        return false;
    }
    /** Exact native hit loop; nested unrelated explosions cannot borrow the outer item's shield. */
    public static void scope(ServerExplosion explosion,Runnable hits){
        var pending=PENDING.get();var previous=ACTIVE.get();Shield selected=stamped(explosion.getDirectSourceEntity());
        if(pending!=null&&(pending.emitter!=null?pending.emitter==explosion.getDirectSourceEntity():contains(explosion.getDamageSource(),pending.damage)))selected=pending.shield;
        PENDING.remove();if(selected==null)ACTIVE.remove();else ACTIVE.set(new Active(explosion.getDamageSource(),selected));
        try{hits.run();}finally{restore(ACTIVE,previous);restore(PENDING,pending);}
    }
    public static int level(LivingEntity recipient,DamageSource damage){
        var active=ACTIVE.get();
        return active!=null&&damage.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)&&contains(damage,active.damage)
                ?active.shield.levels.getOrDefault(recipient.getUUID(),0):0;
    }
}
