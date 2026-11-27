package me.skywars2.game;

import java.util.List;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.combat.WeaponEnchantments;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.enchantment.Enchantments;

/** Native publication, cloned source, checkpoint restoration and reflected controller. */
public final class FeedbackWandProvenanceChecks {
    private FeedbackWandProvenanceChecks() {}
    private static void trackingBounce(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity actor){
        var manager=(me.skywars2.validation.mixin.NativeFixtureAccess.Sections)
                ((me.skywars2.mixin.entities.ActorCheckpointAccess.LevelEntities)level).skywars2$manager();
        manager.skywars2$validationStopTracking(actor);
        if(actor.isRemoved())throw new IllegalStateException("tracking stop removed actual spell actor");
        manager.skywars2$validationStartTracking(actor);
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();WeaponEnchantments.stopSpellActors();
            var wand=f.leaf("terra_wand");
            var registry=f.owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            wand.enchant(registry.getOrThrow(Enchantments.POWER),1);
            var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(f.home.add(3,4,0));cow.setNoAi(true);cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);
            if(!f.level.addFreshEntity(cow))throw new IllegalStateException("provenance target");f.own(cow);
            var shot=new Snowball(EntityTypes.SNOWBALL,f.level);shot.setOwner(f.owner);shot.setPos(f.home.add(0,4,0));
            try(var scope=WeaponEnchantments.using(f.owner,wand)){
                if(!f.level.addFreshEntity(shot))throw new IllegalStateException("provenance native publication");
            }
            f.own(shot);trackingBounce(f.level,shot);
            if(WeaponEnchantments.actorCause(shot,f.owner)==null)throw new IllegalStateException("tracking bounce lost wand provenance");
            var shotOutput=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess());
            shot.saveWithoutId(shotOutput);var shotId=shot.getUUID();
            shot.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var reloaded=new Snowball(EntityTypes.SNOWBALL,f.level);
            reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),shotOutput.buildResult()));
            if(!reloaded.getUUID().equals(shotId)||!f.level.addFreshEntity(reloaded))throw new IllegalStateException("native unloaded wand actor readmission");
            shot=reloaded;f.own(shot);
            if(WeaponEnchantments.actorCause(shot,f.owner)==null)throw new IllegalStateException("native unloaded wand actor lost provenance");
            var clone=new Snowball(EntityTypes.SNOWBALL,f.level);clone.setOwner(f.owner);clone.setPos(f.home.add(0,4,1));
            if(!f.level.addFreshEntity(clone))throw new IllegalStateException("provenance clone publication");f.own(clone);
            WeaponEnchantments.inherit(shot,clone);
            var links=new MatchSpellCheckpoint.Links(new MatchDamageSources(game,List.of(shot,clone)));
            var checkpoint=WeaponEnchantments.checkpoint(links);
            if(!links.resolve(id->f.level.getEntity(id)).isEmpty())throw new IllegalStateException("provenance native links unresolved");
            WeaponEnchantments.stopSpellActors();checkpoint.prepare(links).run();
            for(var actor:List.of(shot,clone)){
                cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
                var damage=new DamageSource(f.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),actor,f.owner);
                if(!cow.hurtServer(f.level,damage,4)||Math.abs(cow.getHealth()-994f)>.001f)
                    throw new IllegalStateException("captured/clone checkpoint Power mismatch: "+cow.getHealth());
            }
            clone.setOwner(f.other);cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var reflected=new DamageSource(f.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),clone,f.other);
            if(!cow.hurtServer(f.level,reflected,4)||Math.abs(cow.getHealth()-996f)>.001f)
                throw new IllegalStateException("reflected projectile borrowed original owner Power: "+cow.getHealth());
            try(var outer=WeaponEnchantments.using(f.other,wand);var actor=WeaponEnchantments.actorScope(clone,f.other)){
                if(WeaponEnchantments.effectCause(f.other)!=null)throw new IllegalStateException("reflected actor borrowed outer unrelated wand");
            }
            // Native owned-mob shot retains the cast, but retaming cannot transfer it to another controller.
            var minion=new net.minecraft.world.entity.monster.skeleton.Skeleton(EntityTypes.SKELETON,f.level);
            minion.setPos(f.home.add(1,4,3));minion.setNoAi(true);minion.setNoGravity(true);
            try(var use=WeaponEnchantments.using(f.owner,wand)){
                game.items().summons().track(f.owner,minion);
                if(!f.level.addFreshEntity(minion))throw new IllegalStateException("native attributed minion publication");
            }
            f.own(minion);
            var mobShot=new Snowball(EntityTypes.SNOWBALL,f.level);mobShot.setOwner(minion);mobShot.setPos(f.home.add(2,4,3));
            if(!f.level.addFreshEntity(mobShot))throw new IllegalStateException("native minion projectile publication");f.own(mobShot);
            var minionDamage=new DamageSource(f.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),mobShot,minion);
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            if(!cow.hurtServer(f.level,minionDamage,4)||Math.abs(cow.getHealth()-994f)>.001f)
                throw new IllegalStateException("native owned mob shot lost wand Power: "+cow.getHealth());
            game.items().summons().track(f.other,minion);
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            if(!cow.hurtServer(f.level,minionDamage,4)||Math.abs(cow.getHealth()-996f)>.001f)
                throw new IllegalStateException("retamed mob projectile borrowed previous controller Power: "+cow.getHealth());
            f.discard(mobShot);f.discard(minion);
            var book=f.leaf("random_grimoire");book.enchant(registry.getOrThrow(Enchantments.POWER),1);
            var borrowed=new Snowball(EntityTypes.SNOWBALL,f.level);borrowed.setOwner(f.owner);borrowed.setPos(f.home.add(0,4,2));
            try(var outer=WeaponEnchantments.using(f.owner,book);
                var borrowedOrigin=me.skywars2.game.items.wands.WandCastOrigin.scope(f.owner,true);
                var inner=WeaponEnchantments.using(f.owner,wand.copyWithCount(1))){
                // Strip Power from the projected wand: only the actual book may supply it.
                var projected=wand.copy();projected.remove(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
                try(var projection=WeaponEnchantments.using(f.owner,projected)){
                    if(!f.level.addFreshEntity(borrowed))throw new IllegalStateException("borrowed provenance publication");
                }
            }
            f.own(borrowed);cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var borrowedDamage=new DamageSource(f.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),borrowed,f.owner);
            if(!cow.hurtServer(f.level,borrowedDamage,4)||Math.abs(cow.getHealth()-994f)>.001f)
                throw new IllegalStateException("actual enchanted grimoire lost through borrowed wand: "+cow.getHealth());
            var tmt=me.skywars2.game.items.tmt.LbcTmtItems.prototype(0);
            var tag=tmt.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            me.skywars2.game.items.tmt.LbcTmtAbilities.write(tag,List.of(new me.skywars2.game.items.tmt.LbcTmtAbilities.Ability("check/power",net.minecraft.world.item.component.CustomData.of(new net.minecraft.nbt.CompoundTag()))));
            tmt.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            tmt.enchant(registry.getOrThrow(Enchantments.POWER),1);f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tmt);
            var tmtShot=new Snowball(EntityTypes.SNOWBALL,f.level);tmtShot.setOwner(f.owner);tmtShot.setPos(f.home.add(0,4,3));
            boolean invoked=me.skywars2.game.items.tmt.LbcTmtAbilities.invoke(tmt,"check/power",()->{
                try(var use=WeaponEnchantments.using(f.owner,tmt)) {return f.level.addFreshEntity(tmtShot);}
            });
            if(!invoked)throw new IllegalStateException("private TMT projection publication failed");f.own(tmtShot);
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var tmtDamage=new DamageSource(f.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),tmtShot,f.owner);
            if(!cow.hurtServer(f.level,tmtDamage,4)||Math.abs(cow.getHealth()-994f)>.001f)
                throw new IllegalStateException("TMT outer Power lost through private projection: "+cow.getHealth());
            // Exercise real synchronous wand damage and deferred effect healing.
            var vampire=me.skywars2.game.items.wands.LbcWands.LIFESTEALER.getDefaultInstance();vampire.enchant(registry.getOrThrow(Enchantments.POWER),1);
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;f.owner.removeAllEffects();f.owner.setHealth(900);
            try(var use=WeaponEnchantments.using(f.owner,vampire)){
                game.items().lifestealer().cast(f.owner,net.minecraft.world.InteractionHand.MAIN_HAND,vampire,false);
            }
            if(Math.abs(cow.getHealth()-998.5f)>.001f)throw new IllegalStateException("actual Lifestealer wand damage lost Power: "+cow.getHealth());
            var regeneration=net.minecraft.world.effect.MobEffects.REGENERATION;
            var healing=f.owner.getEffect(regeneration);if(healing==null)throw new IllegalStateException("Lifestealer did not grant regeneration");
            for(int i=0;i<20&&f.owner.getHealth()==900;i++)healing.tickServer(f.level,f.owner,()->{});
            if(Math.abs(f.owner.getHealth()-901.5f)>.001f)throw new IllegalStateException("deferred Lifestealer healing lost Power: "+f.owner.getHealth());
            f.owner.removeEffect(regeneration);
            var blood=me.skywars2.game.items.wands.LbcWands.BLOOD.getDefaultInstance();blood.enchant(registry.getOrThrow(Enchantments.POWER),1);
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            try(var use=WeaponEnchantments.using(f.owner,blood)){
                game.items().bloodConfuse().blood(f.owner,net.minecraft.world.InteractionHand.MAIN_HAND,blood,true);
            }
            if(Math.abs(cow.getHealth()-973f)>.001f)throw new IllegalStateException("actual Blood wand damage lost Power: "+cow.getHealth());
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.hurtServer(f.level,f.level.damageSources().wither(),1);
            if(Math.abs(cow.getHealth()-999f)>.001f)throw new IllegalStateException("unrelated wither damage borrowed active status Power");
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.getEffect(net.minecraft.world.effect.MobEffects.WITHER).tickServer(f.level,cow,()->{});
            if(Math.abs(cow.getHealth()-998.5f)>.001f)throw new IllegalStateException("actual native Blood Wither tick lost Power: "+cow.getHealth());
            try(var use=WeaponEnchantments.using(f.owner,blood)){
                cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,600,2),f.owner);
            }
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.getEffect(net.minecraft.world.effect.MobEffects.WITHER).tickServer(f.level,cow,()->{});
            if(Math.abs(cow.getHealth()-999f)>.001f)throw new IllegalStateException("plain potion winner borrowed Blood Power: "+cow.getHealth());
            cow.removeAllEffects();cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var evoker=me.skywars2.game.items.wands.LbcWands.EVOKER.getDefaultInstance();evoker.enchant(registry.getOrThrow(Enchantments.POWER),1);
            net.minecraft.world.entity.projectile.EvokerFangs fang;
            try(var use=WeaponEnchantments.using(f.owner,evoker)){fang=game.items().evokerBass().spawnOwned(f.owner,f.home.add(2,1,0));}
            if(fang==null)throw new IllegalStateException("evoker native fang publication");f.own(fang);
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            game.items().evokerBass().fangHit(fang,cow);
            if(Math.abs(cow.getHealth()-991f)>.001f)throw new IllegalStateException("delayed native fang lost captured Power: "+cow.getHealth());
            f.discard(fang);cow.removeAllEffects();
            // A real queued Library ray survives hand swap and keeps its Wither owner.
            var lane=f.home.add(0,30,0);var cell=net.minecraft.core.BlockPos.containing(lane);
            for(var at:net.minecraft.core.BlockPos.betweenClosed(cell.offset(-1,-1,-1),cell.offset(8,3,1)))f.set(at.immutable(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            f.move(f.owner,lane);f.owner.setYRot(-90);f.owner.setXRot(0);cow.setPos(lane.add(4,.5,0));
            try(var use=WeaponEnchantments.using(f.owner,book)){game.items().libraryRandom().ray(f.owner,18,null,null);}
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            for(int i=0;i<20&&game.items().libraryRandom().pendingRayCount()>0;i++)game.items().exasperato().tick();
            var libraryWither=cow.getEffect(net.minecraft.world.effect.MobEffects.WITHER);
            if(libraryWither==null)throw new IllegalStateException("queued Library Wither did not hit native target; pending="+game.items().libraryRandom().pendingRayCount()+" eye="+f.owner.getEyePosition()+" look="+f.owner.getLookAngle()+" bounds="+cow.getBoundingBox()+" valid="+game.items().wands().validTarget(cow));
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;libraryWither.tickServer(f.level,cow,()->{});
            if(Math.abs(cow.getHealth()-998.5f)>.001f)throw new IllegalStateException("queued Library Wither lost launch Power: "+cow.getHealth());
            cow.removeAllEffects();cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var corruption=me.skywars2.game.items.wands.LbcWands.CORRUPTION.getDefaultInstance();corruption.enchant(registry.getOrThrow(Enchantments.POWER),1);
            try(var use=WeaponEnchantments.using(f.owner,corruption)){
                me.skywars2.game.world.terrain.LbcCorruption.cast(game,f.owner,net.minecraft.world.InteractionHand.MAIN_HAND,corruption,true,4);
            }
            if(Math.abs(cow.getHealth()-988f)>.001f)throw new IllegalStateException("actual Corruption damage lost Power: "+cow.getHealth());
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            try(var use=WeaponEnchantments.using(f.owner,corruption)){
                me.skywars2.game.world.terrain.LbcCorruption.cast(game,f.owner,net.minecraft.world.InteractionHand.MAIN_HAND,corruption,true,1);
            }
            var poison=cow.getEffect(net.minecraft.world.effect.MobEffects.POISON);
            if(poison==null)throw new IllegalStateException("actual Corruption poison missing");
            poison.tickServer(f.level,cow,()->{});
            if(Math.abs(cow.getHealth()-998.5f)>.001f)throw new IllegalStateException("actual Corruption poison lost Power: "+cow.getHealth());
            cow.removeAllEffects();cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var acid=me.skywars2.game.items.wands.LbcWands.ACID_WAND.getDefaultInstance();acid.enchant(registry.getOrThrow(Enchantments.POWER),1);
            me.skywars2.game.entities.projectiles.LbcClouds.AcidProjectile acidShot;
            try(var use=WeaponEnchantments.using(f.owner,acid)){acidShot=game.items().clouds().academyWandAcid(f.owner,false);}
            f.own(acidShot);acidShot.setPos(cow.position());acidShot.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            game.items().clouds().tick();var acidPoison=cow.getEffect(net.minecraft.world.effect.MobEffects.POISON);
            if(acidPoison==null)throw new IllegalStateException("native acid projectile did not apply poison");
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;acidPoison.tickServer(f.level,cow,()->{});
            if(Math.abs(cow.getHealth()-998.5f)>.001f)throw new IllegalStateException("native acid poison lost projectile Power: "+cow.getHealth());
            f.discard(acidShot);cow.removeAllEffects();cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,200,0));
            cow.getEffect(net.minecraft.world.effect.MobEffects.POISON).tickServer(f.level,cow,()->{});
            if(Math.abs(cow.getHealth()-999f)>.001f)throw new IllegalStateException("ordinary poison borrowed acid wand Power");
            cow.removeAllEffects();cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            game.items().psycho().clearEffects(f.owner);
            var psycho=me.skywars2.game.items.wands.LbcWands.PSYCHOBLIND.getDefaultInstance();psycho.enchant(registry.getOrThrow(Enchantments.POWER),1);
            try(var use=WeaponEnchantments.using(f.owner,psycho)){game.items().psycho().psychoblind(f.owner,2);}
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);game.items().psycho().tick();
            if(Math.abs(cow.getHealth()-999.985f)>.001f)throw new IllegalStateException("sustained Psycho ray lost launch Power: "+cow.getHealth());
            game.items().psycho().clearEffects(f.owner);game.items().psycho().clearDebuffs(cow);
            cow.removeAllEffects();cow.setPos(f.home.add(3,4,0));f.move(f.owner,f.home);
            // Native falling-block damage keeps only the explicitly bound Library cast.
            me.skywars2.game.combat.LbcIndirectKills.Owner fallingCause;
            try(var use=WeaponEnchantments.using(f.owner,book)){fallingCause=WeaponEnchantments.effectCause(f.owner);}
            for(boolean bound:new boolean[]{true,false}){
                var block=EntityTypes.FALLING_BLOCK.create(f.level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if(block==null)throw new IllegalStateException("native falling block creation");
                try{block.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),
                        net.minecraft.nbt.TagParser.parseCompoundFully("{BlockState:'minecraft:stone_bricks',Time:1,DropItem:0b,HurtEntities:1b,FallHurtMax:10,FallHurtAmount:10f}")));}
                catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
                block.setPos(cow.position());if(bound)WeaponEnchantments.captureFalling(block,f.owner,fallingCause);
                if(!f.level.addFreshEntity(block))throw new IllegalStateException("native falling block publication");f.own(block);
                cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
                block.causeFallDamage(2,1,f.level.damageSources().fall());
                float expected=bound?985:990;
                if(Math.abs(cow.getHealth()-expected)>.001f)throw new IllegalStateException("falling-block Power bound="+bound+" health="+cow.getHealth());
                f.discard(block);
            }
            // Actual thunderHit reconstructs the native bolt/owner damage source.
            for(boolean powered:new boolean[]{true,false}){
                net.minecraft.world.entity.LightningBolt bolt;
                try(var use=powered?WeaponEnchantments.using(f.owner,book):null){bolt=game.items().electro().lightning(f.owner,cow.position());}
                if(bolt==null)throw new IllegalStateException("native lightning publication");f.own(bolt);
                cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
                cow.thunderHit(f.level,bolt);
                float expected=powered?992.5f:995f;
                if(Math.abs(cow.getHealth()-expected)>.001f)throw new IllegalStateException("native lightning Power="+powered+" health="+cow.getHealth());
                cow.clearFire();f.discard(bolt);
            }
            // Actual native fire block and lava contacts use the exact placed hazard owner.
            var hazardPos=net.minecraft.core.BlockPos.containing(cow.position());
            // Native BaseFireBlock.onPlace removes unsupported fire immediately.
            f.set(hazardPos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            f.set(hazardPos,net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            var fireState=f.level.getBlockState(hazardPos);
            if(!fireState.is(net.minecraft.world.level.block.Blocks.FIRE))
                throw new IllegalStateException("native fire fixture placement failed: position="+hazardPos+" state="+fireState);
            try(var use=WeaponEnchantments.using(f.owner,book)){game.items().indirectKills().hazardPlaced(f.level,hazardPos,f.owner);}
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var contact=new net.minecraft.world.entity.InsideBlockEffectApplier.StepBasedCollector();
            boolean[] fired={false};
            fireState.entityInside(f.level,hazardPos,cow,contact,false);
            contact.runAfter(net.minecraft.world.entity.InsideBlockEffectType.FIRE_IGNITE,entity->fired[0]=true);
            contact.applyAndClear(cow);
            if(Math.abs(cow.getHealth()-998.5f)>.001f)throw new IllegalStateException("native fire block lost wand Power: health="+cow.getHealth()
                    +" callback="+fired[0]+" state="+fireState+" alive="+cow.isAlive()+" immune="+cow.fireImmune()
                    +" fireTicks="+cow.getRemainingFireTicks()+" invulnerableTime="+cow.getInvulnerableTime()+" position="+cow.position());
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.hurtServer(f.level,f.level.damageSources().inFire(),1);
            if(Math.abs(cow.getHealth()-999f)>.001f)throw new IllegalStateException("non-native in-fire damage borrowed hazard Power");
            f.set(hazardPos,net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
            try(var use=WeaponEnchantments.using(f.owner,book)){game.items().indirectKills().hazardPlaced(f.level,hazardPos,f.owner);}
            cow.setHealth(1000);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;cow.lavaHurt();
            if(Math.abs(cow.getHealth()-994f)>.001f)throw new IllegalStateException("native lava contact lost wand Power: "+cow.getHealth());
            cow.clearFire();
            // Native serialization drops server-only fields; checkpoint restores each hidden layer.
            var marked=new net.minecraft.world.effect.MobEffectInstance(regeneration,200,0);
            try(var use=WeaponEnchantments.using(f.owner,wand)) {WeaponEnchantments.markHealingEffect(marked,WeaponEnchantments.effectCause(f.owner));}
            cow.addEffect(marked);
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(regeneration,1,1));
            var effectLinks=new MatchSpellCheckpoint.Links(new MatchDamageSources(game,List.of(cow)));
            var effectImage=WeaponEnchantments.effectCheckpoint(game,effectLinks);
            var ops=f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            var encoded=net.minecraft.world.effect.MobEffectInstance.CODEC.encodeStart(ops,cow.getEffect(regeneration)).getOrThrow();
            var decoded=net.minecraft.world.effect.MobEffectInstance.CODEC.parse(ops,encoded).getOrThrow();
            var rejoined=new Cow(EntityTypes.COW,f.level);rejoined.addEffect(net.minecraft.world.effect.MobEffectInstance.CODEC.parse(ops,encoded).getOrThrow());
            WeaponEnchantments.transferHealingEffects(cow,rejoined);
            var rejoinedTop=(me.skywars2.game.combat.WandPowerEffectAccess)(Object)rejoined.getEffect(regeneration);
            var rejoinedHidden=(me.skywars2.game.combat.WandPowerEffectAccess)(Object)rejoinedTop.skywars2$hiddenEffect();
            if(rejoinedTop.skywars2$powerCause()!=null||rejoinedHidden==null||rejoinedHidden.skywars2$powerCause()==null)
                throw new IllegalStateException("login effect provenance transfer failed");
            cow.removeEffect(regeneration);cow.addEffect(decoded);
            if(!effectLinks.resolve(id->f.level.getEntity(id)).isEmpty())throw new IllegalStateException("effect checkpoint links unresolved");
            effectImage.prepare(effectLinks).run();
            var top=(me.skywars2.game.combat.WandPowerEffectAccess)(Object)cow.getEffect(regeneration);
            var hidden=(me.skywars2.game.combat.WandPowerEffectAccess)(Object)top.skywars2$hiddenEffect();
            if(top.skywars2$powerCause()!=null||hidden==null||hidden.skywars2$powerCause()==null)
                throw new IllegalStateException("native serialized hidden effect provenance was not restored");
            cow.getEffect(regeneration).tickServer(f.level,cow,()->{});
            if(((me.skywars2.game.combat.WandPowerEffectAccess)(Object)cow.getEffect(regeneration)).skywars2$powerCause()==null)
                throw new IllegalStateException("hidden effect promotion lost wand provenance");
            cow.removeEffect(regeneration);
            WeaponEnchantments.stopSpellActors();
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS feedback-wand-provenance");
    }
}
