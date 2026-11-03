package me.skywars2.game;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Accepted Nexus tools share the ordinary-world route without opening other Nexus item input. */
public final class LbcNexusCombat {
    static final int[] PULSAR_DELAYS = {0,40,60,70,75};
    private static final TagKey<Block> AIRS = TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> AIRS_ONLY = TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs_only"));
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final DustColorTransitionOptions ORB_DUST = new DustColorTransitionOptions(0xee326d,0x4c0943,2);
    private final GameController game;
    private final LbcNexusPulsar pulsar;
    private final LbcNexusUncrafting uncrafting;
    private final Set<UUID> casting = new HashSet<>();
    private long bursts;

    LbcNexusCombat(GameController game) {
        this.game=game;
        // Explicit author rule: neither new tool, including an accepted wave, is suppressible.
        pulsar=new LbcNexusPulsar(game,(level,pos)->true);
        uncrafting=new LbcNexusUncrafting(game);
    }
    public static boolean matches(ItemStack stack){return LbcNexusToolItems.isOrbGun(stack)||LbcNexusToolItems.isPulsar(stack);}
    LbcNexusPulsar pulsar(){return pulsar;}
    LbcNexusUncrafting uncrafting(){return uncrafting;}
    long bursts(){return bursts;}
    int pending(){return casting.size();}
    public void reload(){uncrafting.reload();}
    private boolean admitted(ServerPlayer p){return game.playing()&&game.canUseGameplayItems(p)&&p.isAlive()&&!p.isRemoved()
            &&!p.level().dimension().equals(LbcHedgehogDimensions.IMPRINTED)&&!game.items().actionsBlocked(p)
            &&!game.items().wands().inputTimeStopped(p)&&!game.items().timeStop().stopped(p.level());}

    public boolean use(ServerPlayer player,InteractionHand hand){
        if(!admitted(player)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var held=player.getItemInHand(hand);boolean pulse=LbcNexusToolItems.isPulsar(held);
        if(!pulse&&!LbcNexusToolItems.isOrbGun(held)||pulse&&game.items().mana().hudValue(player,"pulsar_cd")>0)return false;
        UUID id=player.getUUID();if(!casting.add(id))return false;
        ServerLevel level=player.level();String epoch=game.items().epoch();var cause=game.items().indirectKills().wildCause(player);
        boolean unpack=!pulse&&player.isShiftKeyDown();int damage=0;
        if(pulse){game.items().mana().score(player,"pulsar_cd").set(15);game.items().nexusTools().inventoryChanged(player);}
        else if(!unpack){
            // This deliberately uses the native command's float multiplication and current-level progress.
            // The author retained progress+1 debit, including the failed shot at zero progress.
            int progress=Mth.floor(player.experienceProgress*player.getXpNeededForNextLevel());
            player.giveExperiencePoints(-(progress+1));damage=progress/2;
            if(progress<1){casting.remove(id);player.sendSystemMessage(Component.translatable("not_enough_xp"));return true;}
            game.items().wands().wandCasted(player,true);
        }
        int amount=damage;double range=pulse?200:unpack?8:198;
        var trail=pulse?null:trail(level,unpack);
        game.items().exasperato().searchNexusTool(player,player.getEyePosition(),player.getLookAngle(),range,
                (pos,state)->!state.is(pulse?AIRS_ONLY:AIRS),
                e->!pulse&&e!=player&&!e.isRemoved()&&!e.isSpectator()&&!e.entityTags().contains("strict_map_object")
                        &&(!unpack||e instanceof ItemEntity),trail)
            .thenAccept(hit->{casting.remove(id);
                if(hit==null||!game.items().currentEpoch(epoch)||player.level()!=level||!admitted(player))return;
                if(pulse){var pos=hit instanceof BlockHitResult b?b.getBlockPos():BlockPos.containing(hit.getLocation());pulsar.start(level,pos,cause,PULSAR_DELAYS);game.items().wands().wandCasted(player,true);}
                else if(unpack){if(uncrafting.request(level,hit.getLocation(),player,true,true)>0)game.items().wands().wandCasted(player,true);}
                else burst(level,hit.getLocation(),player,cause,amount);
            });
        return true;
    }

    /** Particle spacing belongs to the original ray, independent of the CPU queue's segment size. */
    private BiConsumer<Vec3,Vec3> trail(ServerLevel level,boolean unpack){
        return new BiConsumer<>(){double travelled,next;
            @Override public void accept(Vec3 from,Vec3 to){double length=from.distanceTo(to);var direction=length==0?Vec3.ZERO:to.subtract(from).scale(1/length);
                while(next<=travelled+length){var point=from.add(direction.scale(Math.max(0,next-travelled)));
                    if(!unpack)level.sendParticles(ParticleTypes.ENCHANT,true,false,point.x,point.y,point.z,1,.01,.01,.01,0);
                    else if(level.getRandom().nextFloat()<.03f)level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,point.x,point.y,point.z,1,0,0,0,0);
                    next+=unpack?.1:.33;
                }travelled+=length;
            }
        };
    }
    private void burst(ServerLevel level,Vec3 at,ServerPlayer owner,LbcIndirectKills.Owner cause,int damage){
        bursts++;
        var source=game.items().indirectKills().captureDamage(level.damageSources().outOfBorder(),cause);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),
                e->e!=owner&&e.isAlive()&&e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(at)<=100
                        &&game.items().wands().validRoleTarget(e,false)))target.hurtServer(level,source,damage);
        level.playSound(null,at.x,at.y,at.z,SoundEvents.WARDEN_SONIC_BOOM,SoundSource.MASTER,5,1);
        // Source thirty curved tentacles are visual only; the area damage is executed exactly once.
        for(int tentacle=0;tentacle<30;tentacle++){
            float yaw=(level.getRandom().nextInt(36001)-18000)*.01f,pitch=(level.getRandom().nextInt(8001)-4000)*.01f;var point=at;
            for(int sample=0;sample<=30;sample++){
                level.sendParticles(ORB_DUST,true,false,point.x,point.y,point.z,1,0,0,0,0);
                yaw+=5;point=point.add(Vec3.directionFromRotation(pitch,yaw).scale(.5));
            }
        }
    }
    void tick(){pulsar.tick();uncrafting.tick();}
    void stop(){casting.clear();pulsar.stop();uncrafting.stop();}
    void prepareFrozen(){casting.clear();pulsar.prepareFrozen();uncrafting.prepareFrozen();}
    void publishFrozen(){pulsar.publishFrozen();uncrafting.publishFrozen();}
}
