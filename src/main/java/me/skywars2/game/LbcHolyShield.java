package me.skywars2.game;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Native using-item pulses; vanilla still owns shield use, blocking and durability. */
public final class LbcHolyShield {
    private static final TagKey<EntityType<?>> UNHOLY=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:unholy_monsters"));
    private static final DustParticleOptions LIGHT=new DustParticleOptions(0xffff00,1);
    private LbcHolyShield(){}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("holyshield",0)==1;}
    public static void using(GameController game,ServerPlayer player,ItemStack stack){
        if(!matches(stack)||!game.playing()||game.stage()==GameController.Stage.ENDING||!game.canUseGameplayItems(player)
                ||!player.isAlive()||player.isRemoved()||game.items().actionsBlocked(player)
                ||player.entityTags().contains("time_stopped")||game.items().timeStop().stopped(player.level()))return;
        var dimension=player.level().dimension().identifier().toString();
        if(dimension.equals("minecraft:nexus")||dimension.equals("minecraft:imprinted"))return;
        var level=player.level();var at=player.getEyePosition().add(player.getLookAngle().scale(3));
        var cause=game.items().indirectKills().wildCause(player);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(4),e->
                e.getType().builtInRegistryHolder().is(UNHOLY)&&e.position().distanceToSqr(at)<=16&&game.items().wands().validTarget(e))){
            if(target.getType()==EntityTypes.ZOMBIE&&target instanceof Mob mob
                    &&mob.getLootTable().filter(key->key.identifier().toString().equals("lbc:reactive_shish")).isPresent())
                game.items().mana().award(player,"lbc:true_advancements/shields/holy");
            level.sendParticles(LIGHT,target.getX(),target.getY()+1,target.getZ(),10,.3,.7,.3,0);
            // Instant Health I's native formula; retain ordinary magic HurtTime and the real author.
            if(target.isInvertedHealAndHarm())target.hurtServer(level,game.items().indirectKills().captureDamage(
                    new DamageSource(target.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),player),cause),6);
            else target.heal(4);
        }
        // The source kills the display directly, so purification has no terminal curse/explosion.
        game.items().corruptedGlyphs().dispelAt(level,at,4);
    }
}
