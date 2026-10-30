package me.skywars2.game;

import me.skywars2.mixin.CombatCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Native causal probes; full player/actor/terrain cleanup belongs to the enclosing fixture. */
final class VanityAttributionScene {
    final GameController game;final ChefChecks.Fixture base;final LbcIndirectKills service;
    final Cow effect,environment,support,arrival;final ServerPlayer old;
    final BlockPos wall,hot,arrivalWall,supportCell;final long next;
    VanityAttributionScene(GameController game,ChefChecks.Fixture base,ServerPlayer gamma){
        this.game=game;this.base=base;service=game.items().indirectKills();
        effect=cow(-8,3,-8);environment=cow(-8,3,8);support=cow(8,1,-8);arrival=cow(8,3,8);
        old=new ServerPlayer(game.server(),base.owner.level(),gamma.getGameProfile(),net.minecraft.server.level.ClientInformation.createDefault());game.server().getPlayerList().getPlayerAdvancements(gamma);old.setRemainingFireTicks(600);service.displaced(old,base.owner);service.ignited(old,base.owner);
        var beta=service.wildCause(base.other);service.inheritedEffect(gamma,new MobEffectInstance(MobEffects.BLINDNESS,1200),beta);gamma.addEffect(new MobEffectInstance(MobEffects.HUNGER,1200),base.other);
        gamma.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,1200,20),base.other);service.manaPotionConsumed(gamma);service.manaLost(gamma);gamma.setRemainingFireTicks(600);service.ignited(gamma,base.other);
        gamma.addEffect(new MobEffectInstance(MobEffects.POISON,1200),base.other);service.effectChanged(gamma,new MobEffectInstance(MobEffects.POISON,1200),base.owner); // Deliberately inactive old pointer, never a current native poison.
        service.inheritedEffect(effect,new MobEffectInstance(MobEffects.POISON,1200),service.wildCause(base.owner));service.inheritedEffect(effect,new MobEffectInstance(MobEffects.WITHER,1200),service.wildCause(base.other));effect.addEffect(new MobEffectInstance(MobEffects.LEVITATION,1200),base.owner);
        wall=BlockPos.containing(environment.getEyePosition());hot=environment.blockPosition().below();write(wall,Blocks.STONE);service.stonePlaced(base.owner.level(),wall,base.owner);write(hot,Blocks.MAGMA_BLOCK);service.hazardPlaced(base.owner.level(),hot,base.owner);environment.setRemainingFireTicks(600);service.ignited(environment,base.other);
        arrivalWall=BlockPos.containing(arrival.getEyePosition());write(arrivalWall,Blocks.STONE);service.teleported(arrival,base.owner,false);
        supportCell=support.blockPosition().below();write(supportCell,Blocks.STONE);support.setOnGround(true);service.supportRemoved(base.owner.level(),base.other,supportCell);write(supportCell,Blocks.AIR);
        next=service.wildCause(base.owner).order()+1;
    }
    private Cow cow(int x,int y,int z){var c=new Cow(EntityTypes.COW,base.owner.level());c.setNoAi(true);c.setNoGravity(true);c.entityTags().add("have_stand");c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);c.setHealth(200);c.setPos(base.at.add(x,y,z));require(base.owner.level().addFreshEntity(c),"native causal witness registration");return c;}
    private void write(BlockPos pos,Block block){require(base.blocks.containsKey(pos),"causal terrain remains in the enclosing snapshot");base.owner.level().setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity attribution: "+why);}
    private static void reset(LivingEntity target){var a=(CombatCheckpointAccess.Living)target;a.skywars2$player(null);a.skywars2$mob(null);target.setInvulnerableTime(0);target.damageCooldownTime=0;}
    private void credited(LivingEntity target,ServerPlayer author,Runnable action,String why){reset(target);action.run();require(target.getLastHurtByPlayer()==author&&target.getLastHurtByMob()==author,why);}
    void verify(ServerPlayer gamma){
        require(service.wildCause(base.owner).order()==next,"global causal sequence rewinds exactly; no fresh owners synthesized by restore");
        credited(gamma,base.other,()->service.statusDamage(gamma,MobEffects.BLINDNESS,()->true),"blindness binds to the CURRENT native effect/player body");
        credited(gamma,base.other,()->service.environmentDamage(gamma,gamma.damageSources().starve()),"hunger debt survives body replacement");
        credited(gamma,base.other,()->service.manaDamage(gamma,()->service.dying(gamma,gamma.damageSources().genericKill())),"saved mana debt is used by the native pre-death attribution context");
        service.manaPotionConsumed(gamma);service.manaLost(gamma);credited(gamma,base.other,()->service.manaDamage(gamma,()->service.dying(gamma,gamma.damageSources().genericKill())),"active pending potion links to current effect and regenerates the saved drain/debt author");
        credited(gamma,base.other,()->service.environmentDamage(gamma,gamma.damageSources().onFire()),"burning on the live body remains separate from an older body with the SAME UUID");
        credited(old,base.owner,()->service.environmentDamage(old,old.damageSources().onFire()),"older body ledger key remains historical, never overwriting the live victim");
        reset(gamma);service.statusDamage(gamma,MobEffects.POISON,()->true);require(gamma.getLastHurtByPlayer()==null,"inactive old effect pointer never attaches to a current effect by type alone");
        credited(effect,base.owner,()->service.statusDamage(effect,MobEffects.POISON,()->effect.hurtServer(base.owner.level(),effect.damageSources().magic(),1)),"actual native poison-context hit retains Alpha");
        credited(effect,base.other,()->service.statusDamage(effect,MobEffects.WITHER,()->effect.hurtServer(base.owner.level(),effect.damageSources().wither(),1)),"actual native wither-context hit retains Beta");
        require(service.altitudeAuthor(effect)==base.owner,"levitation/fall causal metadata restored");service.finishSupportRemoval();require(service.altitudeAuthor(support)==base.other,"captured pending support removal still promotes to the original fall cause");
        credited(environment,base.owner,()->service.environmentDamage(environment,environment.damageSources().inWall()),"placed stone provenance restored after terrain callbacks");
        credited(environment,base.owner,()->service.environmentDamage(environment,environment.damageSources().hotFloor()),"magma hazard provenance restored after terrain callbacks");
        credited(environment,base.other,()->service.environmentDamage(environment,environment.damageSources().onFire()),"burning cause remains independent of nearby terrain ownership");
        credited(arrival,base.owner,()->service.environmentDamage(arrival,arrival.damageSources().inWall()),"arrival cells and later-action precedence restored");
        gamma.removeEffect(MobEffects.RAID_OMEN);gamma.removeEffect(MobEffects.POISON);gamma.setRemainingFireTicks(0);environment.setRemainingFireTicks(0); // Prevent the probe potion/fire from changing the enclosing HP control.
    }
    void mutate(){service.stop();for(int i=0;i<100;i++)service.wildCause(base.other);service.displaced(effect,base.other);old.setRemainingFireTicks(600);service.ignited(old,base.other);environment.setRemainingFireTicks(600);service.ignited(environment,base.owner);write(wall,Blocks.AIR);write(hot,Blocks.STONE);write(arrivalWall,Blocks.AIR);}
}
