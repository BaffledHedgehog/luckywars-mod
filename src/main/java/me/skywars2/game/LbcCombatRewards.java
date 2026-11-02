package me.skywars2.game;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Exact victim-bound skill16 rewards and skill18 copies of successful native death drops. */
public final class LbcCombatRewards {
    private static final AttributeSupplier PLAYER_BASE = DefaultAttributes.getSupplier(EntityTypes.PLAYER);
    private final GameController game;
    // Entity.equals compares numeric IDs; native respawn reuses that ID for a new body.
    private final Map<LivingEntity, Death> deaths = new IdentityHashMap<>();
    private final Deque<Scope> scopes = new ArrayDeque<>();
    private final List<Reward> pending = new ArrayList<>();
    private boolean includeAttackSpeed=true;
    private boolean publishingCopies;

    private static final class Death {
        final UUID killer;
        final String epoch;
        final boolean soul, loot;
        boolean lootCaptured;
        Death(UUID killer,String epoch,boolean soul,boolean loot) {
            this.killer=killer;this.epoch=epoch;this.soul=soul;this.loot=loot;
        }
    }
    private static final class Scope {
        final LivingEntity victim;
        final ServerLevel level;
        final Set<ItemEntity> drops=Collections.newSetFromMap(new IdentityHashMap<>());
        Scope(LivingEntity victim,ServerLevel level){this.victim=victim;this.level=level;}
    }
    private record Drop(ItemStack stack,Vec3 at) { }
    private record Reward(UUID owner,String epoch,ServerLevel level,boolean soul,
                          Map<Holder<Attribute>,Double> surplus,List<Drop> drops) { }

    public LbcCombatRewards(GameController game){this.game=game;}
    public void includeAttackSpeed(boolean include){includeAttackSpeed=include;}

    /** Called after shared indirect attribution, before native death cleanup changes donor bases. */
    public void beforeDeath(LivingEntity victim,DamageSource source) {
        if(!victim.isDeadOrDying()||victim.isRemoved()||deaths.containsKey(victim))return;
        // Mark even an ineligible death: overlapping native callbacks must never add a reward later.
        deaths.put(victim,new Death(null,game.items().epoch(),false,false));
        if(victim.level()!=game.maps().level()||!game.items().wands().validRoleTarget(victim,true)
                ||!LbcDimensionItems.ordinary(victim.level()))return;
        var owner=attacker(victim,source);
        if(owner==null||owner==victim)return;
        boolean soul=game.lbc().hasSkill(owner,16),loot=game.lbc().hasSkill(owner,18);
        var death=new Death(owner.getUUID(),game.items().epoch(),soul,loot);deaths.put(victim,death);
        if(soul) {
            var surplus=victim instanceof ServerPlayer player?surplus(player,includeAttackSpeed):Map.<Holder<Attribute>,Double>of();
            var reward=new Reward(owner.getUUID(),death.epoch,(ServerLevel)victim.level(),true,surplus,List.of());
            if(paused(owner))pending.add(reward);else apply(reward,owner);
        }
    }
    private ServerPlayer attacker(LivingEntity victim,DamageSource source) {
        var cause=game.items().indirectKills().babylonCause(source);
        if(cause!=null)return game.server().getPlayerList().getPlayer(cause.player());
        var owner=LbcIndirectKills.playerSource(source.getEntity());
        if(owner==null)owner=LbcIndirectKills.playerSource(source.getDirectEntity());
        if(owner==null&&(source.getEntity()==victim||source.getDirectEntity()==victim))return null;
        return owner!=null?owner:LbcIndirectKills.playerSource(victim.getKillCredit());
    }

    public static double baseline(Holder<Attribute> attribute,int health) {
        if(attribute.equals(Attributes.MAX_HEALTH))return Math.max(1,Math.min(1000,health));
        return PLAYER_BASE.hasAttribute(attribute)?PLAYER_BASE.getBaseValue(attribute):attribute.value().getDefaultValue();
    }
    public static double baseline(Holder<Attribute> attribute,GameConfig config) {
        if(attribute.equals(Attributes.ATTACK_SPEED)&&config.pvpMode==1)return 100000;
        return baseline(attribute,config.health);
    }
    private Map<Holder<Attribute>,Double> surplus(ServerPlayer victim,boolean attackSpeed) {
        var result=new LinkedHashMap<Holder<Attribute>,Double>();
        for(var attribute:BuiltInRegistries.ATTRIBUTE.listElements().toList()) {
            if(!attackSpeed&&attribute.equals(Attributes.ATTACK_SPEED))continue;
            var donor=victim.getAttribute(attribute);if(donor==null)continue;
            double extra=donor.getBaseValue()-baseline(attribute,game.config());
            if(extra>0&&Double.isFinite(extra))result.put(attribute,extra);
        }
        return Map.copyOf(result);
    }
    /** Explicit direct helper; no effective/equipment/modifier values enter the transfer. */
    public void absorb(ServerPlayer killer,ServerPlayer victim,boolean attackSpeed) {
        addSurplus(killer,surplus(victim,attackSpeed));
    }
    private void addSurplus(ServerPlayer killer,Map<Holder<Attribute>,Double> surplus) {
        surplus.forEach((key,extra)->{
            var attribute=killer.getAttribute(key);
            if(attribute!=null)attribute.setBaseValue(attribute.getBaseValue()+extra);
        });
    }

    /** Reuses the same victim's outer die scope, while nested deaths get a separate top frame. */
    public void withDeathLoot(LivingEntity victim,ServerLevel level,DamageSource source,Runnable action) {
        if(!scopes.isEmpty()&&scopes.peek().victim==victim){action.run();return;}
        var scope=new Scope(victim,level);scopes.push(scope);
        boolean completed=false;
        try{action.run();completed=true;}
        finally{scopes.pop();if(completed)finish(scope);}
    }
    /** Called only after addFreshEntity returned true; no world or age search. */
    public void published(ServerLevel level,Entity entity) {
        if(publishingCopies||scopes.isEmpty()||!(entity instanceof ItemEntity item))return;
        var scope=scopes.peek();
        if(scope.level==level)scope.drops.add(item);
    }
    private void finish(Scope scope) {
        var death=deaths.get(scope.victim);
        if(death==null||!death.loot||death.lootCaptured)return;
        death.lootCaptured=true;
        var drops=new ArrayList<Drop>();
        for(var entity:scope.drops)if(!entity.isRemoved()&&!entity.getItem().isEmpty())
            drops.add(new Drop(entity.getItem().copy(),entity.position()));
        if(drops.isEmpty())return;
        var reward=new Reward(death.killer,death.epoch,scope.level,false,Map.of(),List.copyOf(drops));
        var owner=game.server().getPlayerList().getPlayer(death.killer);
        if(owner==null||!game.items().currentEpoch(death.epoch))return;
        if(paused(owner))pending.add(reward);else apply(reward,owner);
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(owner)
                ||game.items().timeStop().stopped(owner.level());
    }
    public void tick() {
        deaths.keySet().removeIf(victim->victim.isAlive()||victim.isRemoved());
        var batch=List.copyOf(pending);pending.clear();
        for(var reward:batch) {
            var owner=game.server().getPlayerList().getPlayer(reward.owner);
            if(!game.items().currentEpoch(reward.epoch)||owner==null||owner.level()!=reward.level
                    ||owner.isRemoved())continue;
            if(paused(owner)){pending.add(reward);continue;}
            apply(reward,owner);
        }
    }
    private void apply(Reward reward,ServerPlayer owner) {
        if(reward.soul) {
            int choice=LbcAttributeGains.roll(owner);addSurplus(owner,reward.surplus);
            if(choice==2) {
                LbcOverload.sound(owner,"minecraft:entity.player.levelup",1,1,0);
                owner.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.IRON_BLOCK.defaultBlockState()),
                        owner.getX(),owner.getY()+1,owner.getZ(),100,.5,.5,.5,0);
            }
            return;
        }
        boolean previous=publishingCopies;publishingCopies=true;
        try {
            for(var drop:reward.drops) {
                int remaining=drop.stack.getCount(),max=Math.max(1,drop.stack.getMaxStackSize());
                while(remaining>0) {
                    int count=Math.min(remaining,max);remaining-=count;
                    var stack=drop.stack.copyWithCount(count);
                    var child=new ItemEntity(reward.level,drop.at.x,drop.at.y,drop.at.z,stack);
                    child.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(child);
                    reward.level.addFreshEntity(child);
                }
            }
        }finally{publishingCopies=previous;}
    }
    int pendingCount(){return pending.size();}
    public void clear(){deaths.clear();pending.clear();}
}
