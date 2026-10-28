package me.skywars2.game;

import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;

/** Nyauwu additions to the shared Mage regeneration and successful wand-cast paths. */
public final class LbcNyauwuArmor {
    static final Identifier SPEED=Identifier.parse("minecraft:916c89e6-ae62-4b11-91d7-10e7e57c726d");
    private static final AttributeModifier SPEED_MODIFIER=new AttributeModifier(SPEED,1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<EntityType<?>> UNHOLY=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:unholy_monsters"));
    private static final TagKey<Block> WATER=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:water"));
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private final GameController game;
    private final Predicate<ServerPlayer> eligible,admitted;

    LbcNyauwuArmor(GameController game,Predicate<ServerPlayer> eligible,Predicate<ServerPlayer> admitted){
        this.game=game;this.eligible=eligible;this.admitted=admitted;
    }
    static boolean piece(ServerPlayer player,EquipmentSlot slot){
        var stack=player.getItemBySlot(slot);
        return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("nyauwu",0)==1;
    }
    static boolean full(ServerPlayer player){for(var slot:SLOTS)if(!piece(player,slot))return false;return true;}
    private boolean suppressed(ServerPlayer player){return game.items().magic().beaconSuppressed(player.position());}

    /** Runs after base, belts and Mage pieces, before boost and plague. */
    public int beforeRegeneration(ServerPlayer player,int visual){
        if(!admitted.test(player)||suppressed(player)||game.items().mana().noMagic(player)>0)return visual;
        var mana=game.items().mana();
        if(piece(player,EquipmentSlot.HEAD)&&!player.level().getEntitiesOfClass(ServerPlayer.class,
                player.getBoundingBox().inflate(10),other->other!=player&&!other.isSpectator()
                        &&!other.entityTags().contains("spectator")&&inProximity(player,other)).isEmpty())
            mana.maximum(player,mana.maximum(player)+20);
        // The original mobs tag includes players; allies also prevent this doubling.
        if(piece(player,EquipmentSlot.LEGS)&&player.level().getEntities(player,
                player.getBoundingBox().inflate(10),other->other.getType().builtInRegistryHolder().is(MOBS)
                        &&!other.entityTags().contains("spectator")&&!other.isSpectator()&&inProximity(player,other)).isEmpty())
            visual*=2;
        if(piece(player,EquipmentSlot.CHEST)&&mana.get(player)>=mana.maximum(player)
                &&player.level().getBlockState(player.blockPosition()).is(WATER))mana.add(player,1);
        return visual;
    }
    private static boolean inProximity(ServerPlayer player,Entity other){
        double distance=player.distanceToSqr(other);return distance>=.0001&&distance<=100;
    }
    private static void speed(ServerPlayer player,boolean wanted){
        var attribute=player.getAttribute(Attributes.MOVEMENT_SPEED);if(attribute==null)return;
        var old=attribute.getModifier(SPEED);
        if(!wanted){if(old!=null)attribute.removeModifier(SPEED);return;}
        if(old!=null&&old.amount()==SPEED_MODIFIER.amount()&&old.operation()==SPEED_MODIFIER.operation())return;
        if(old!=null)attribute.removeModifier(SPEED);
        attribute.addTransientModifier(SPEED_MODIFIER);
    }
    /** Cleanup is immediate under MAG or equipment/role changes, even while time is stopped. */
    public void refreshSpeed(ServerPlayer player){
        if(!eligible.test(player)||!piece(player,EquipmentSlot.FEET)||suppressed(player)){speed(player,false);return;}
        if(!admitted.test(player))return;
        speed(player,game.items().mana().get(player)<game.items().mana().maximum(player));
    }

    /** The author binds this pulse to the existing successful-cast callback. */
    public void wandCasted(ServerPlayer player){
        if(!admitted.test(player)||suppressed(player)||!full(player)||player.getTeam()==null)return;
        var targets=player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(30),
                target->!(target instanceof net.minecraft.world.entity.player.Player)
                        &&target.getType().builtInRegistryHolder().is(MOBS)
                        &&player.distanceToSqr(target)<=900&&game.items().wands().validRoleTarget(target,false));
        // Source heals every ally first, then deals the typed damage to foreign mobs.
        for(var target:targets)if(friendly(player,target)&&targetAllowed(target)){
            player.level().sendParticles(ParticleTypes.HEART,target.getX(),target.getY()+1,target.getZ(),10,.3,.6,.3,0);
            target.addEffect(ItemEffectLevels.create(target.getType().builtInRegistryHolder().is(UNHOLY)
                    ?MobEffects.INSTANT_DAMAGE:MobEffects.INSTANT_HEALTH,1,1),player);
        }
        var damage=player.damageSources().source(DamageTypes.ARROW,player,player);
        for(var target:targets)if(!friendly(player,target)&&targetAllowed(target)&&game.items().magic().magical(target))
            target.hurtServer(player.level(),damage,25);
    }
    /** The chest bonus belongs only to an accepted real melee hit, never attributed pet damage. */
    public void damaged(LivingEntity victim,net.minecraft.world.damagesource.DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer player)||!LbcDamage.isMeleeAttack(source,player)
                ||!admitted.test(player)||suppressed(player)||!piece(player,EquipmentSlot.CHEST)
                ||!game.items().wands().validRoleTarget(victim,true))return;
        game.items().mana().hitRegeneration(player);
    }
    private boolean targetAllowed(LivingEntity target){
        return !game.items().magic().beaconSuppressed(target.position());
    }
    private boolean friendly(ServerPlayer player,LivingEntity target){
        var metasolaOwner=game.items().soulless().owner(target);
        return player.isAlliedTo(target)||game.items().summons().friendly(player,target)
                ||game.items().blaze().friendly(player,target)
                ||metasolaOwner!=null&&(metasolaOwner==player||player.isAlliedTo(metasolaOwner));
    }
    public void tick(){for(var player:game.server().getPlayerList().getPlayers())refreshSpeed(player);}
    public void inventoryChanged(ServerPlayer player){refreshSpeed(player);}
    public void loaded(Entity entity){if(entity instanceof ServerPlayer player)refreshSpeed(player);}
    public void forget(ServerPlayer player){speed(player,false);}
    public void stop(){for(var player:game.server().getPlayerList().getPlayers())speed(player,false);}
}
