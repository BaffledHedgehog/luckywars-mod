package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.HashMap;

/** Native collision admission and accepted hurt are separate assertions, including native owner departure. */
public final class ProjectileSelfHitChecks {
    private ProjectileSelfHitChecks() { }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Projectile self/friendly hits: "+message);}
    private static boolean canHit(Projectile shot,Entity target){
        try{Class<?> type=shot.getClass();Method method=null;
            while(type!=null){try{method=type.getDeclaredMethod("canHitEntity",Entity.class);break;}catch(NoSuchMethodException ignored){type=type.getSuperclass();}}
            require(method!=null,"native collision method exists");method.setAccessible(true);return (Boolean)method.invoke(shot,target);
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void hit(Projectile shot,LivingEntity target){
        try{var method=Projectile.class.getDeclaredMethod("hitTargetOrDeflectSelf",HitResult.class);method.setAccessible(true);method.invoke(shot,new EntityHitResult(target));}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static void health(LivingEntity target){target.removeAllEffects();target.setHealth(target.getMaxHealth());target.setAbsorptionAmount(0);target.setInvulnerableTime(0);target.damageCooldownTime=0;}
    private static void accepted(Projectile shot,LivingEntity target,String label){
        health(target);require(canHit(shot,target),label+" collision accepts intended target");float before=target.getHealth();hit(shot,target);
        require(target.getHealth()<before,label+" actual native damage accepted, before="+before+" after="+target.getHealth()
                +" armor="+target.getArmorValue()+" effects="+target.getActiveEffects()+" last="+target.getLastDamageSource()
                +" motion="+shot.getDeltaMovement()
                +" permission="+(shot instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow&&SkyWars2.game().items().allowDamage(target,arrow.damageSources().arrow(arrow,arrow.getOwner()))));
    }
    private static Arrow nativeArrow(GameController game,net.minecraft.world.entity.monster.skeleton.Skeleton emitter,
                                     LivingEntity target,List<Entity> actors){
        var before=java.util.Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        var level=(net.minecraft.server.level.ServerLevel)emitter.level();level.getAllEntities().forEach(before::add);
        emitter.performRangedAttack(target,1);
        var added=new ArrayList<Arrow>();
        level.getAllEntities().forEach(e->{if(!before.contains(e)){actors.add(e);if(e instanceof Arrow a)added.add(a);}});
        require(added.size()==1,"actual native skeleton ranged attack publishes one arrow");
        var arrow=added.getFirst();var policy=game.items().spellSources().source(arrow);
        require(arrow.getOwner()==emitter&&policy!=null&&policy==game.items().spellSources().source(emitter),
                "native arrow keeps actual NPC owner and independently inherited accepted source");
        require(!canHit(arrow,emitter),"newborn NPC arrow retains native muzzle immunity");
        arrow.setPos(emitter.position().add(8,1,0));arrow.setDeltaMovement(Vec3.ZERO);arrow.tick();
        arrow.setDeltaMovement(new Vec3(0,0,2));return arrow;
    }
    private static void sourceArrows(GameController game,net.minecraft.server.level.ServerPlayer owner,
                                     net.minecraft.server.level.ServerPlayer ally,Cow cow,List<Entity> actors,Vec3 at){
        require(game.items().shishEncounter().actorCount()==0&&game.items().wands().electroCount()==0,
                "exclusive source NPC and electrical fixture");
        var authority=new LbcApocalypseEvents.Authority(owner.getUUID(),game.items().indirectKills().wildCause(owner),game.items().epoch());
        var emitter=game.items().shishEncounter().spawnCyborg(authority,owner.level(),at.add(12,0,0));
        require(emitter!=null,"actual persistent Shish cyborg producer");actors.add(emitter);emitter.setNoAi(true);emitter.setNoGravity(true);
        emitter.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        var team=owner.getTeam();require(team!=null,"actual closed friendly-fire team");
        game.server().getScoreboard().addPlayerToTeam(emitter.getScoreboardName(),team);
        try{
            for(var target:List.<LivingEntity>of(owner,ally,cow,emitter)){
                var arrow=nativeArrow(game,emitter,ally,actors);
                accepted(arrow,target,"captured native NPC bow "+target.getType());
                var damage=target.getLastDamageSource();
                require(damage!=null&&damage.getDirectEntity()==arrow&&damage.getEntity()==emitter
                        &&LbcIndirectKills.isWildCaptured(damage),"native emitter preserved separately from WILD causal source");
                var resolved=game.items().indirectKills().babylonCause(damage);
                require(authority.cause().equals(resolved),"actual accepted hurt retains original author UUID/order after projectile removal");
                // CyberShish retaliation remains the real shared on-hit engine; clear only this exclusive fixture's traces.
                game.items().wands().stop();
            }
            var reflected=nativeArrow(game,emitter,ally,actors);health(ally);ally.entityTags().add("sin_greed_active");
            try{require(canHit(reflected,ally),"source arrow reaches absolute Greed physical reflection");var before=ally.getHealth();hit(reflected,ally);
                require(ally.getHealth()==before&&reflected.getDeltaMovement().z<0&&game.items().spellSources().source(reflected)==null,
                        "native reflection clears previous source authority without damage");}
            finally{ally.entityTags().remove("sin_greed_active");}
            var ward=nativeArrow(game,emitter,ally,actors);cow.entityTags().add("strict_map_object");
            try{require(!canHit(ward,cow),"source physical missiles retain service-role protection");}
            finally{cow.entityTags().remove("strict_map_object");}
            var lookup=game.server().getPlayerList().getPlayersByUUID();require(lookup.remove(owner.getUUID(),owner),"temporarily hide only actual source author's resolver entry");
            try{var arrow=nativeArrow(game,emitter,ally,actors);accepted(arrow,cow,"captured native arrow with departed author resolver");
                require(authority.cause().equals(game.items().indirectKills().babylonCause(cow.getLastDamageSource())),"offline author keeps original UUID/order");}
            finally{require(lookup.put(owner.getUUID(),owner)==null,"restore exact source author resolver");}
        }finally{
            game.items().wands().stop();game.items().entityUnloaded(emitter);emitter.discard();
            for(var actor:actors)if(actor instanceof Projectile&&game.items().spellSources().source(actor)!=null){game.items().entityUnloaded(actor);actor.discard();}
        }
    }
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var ally=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.playing()&&owner!=null&&ally!=null,"actual native match participants");
        var saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());var otherSaved=new GameChecks.WandThirdPlayerState(ally,game.items().mana());
        var board=game.server().getScoreboard();var oldOwner=owner.getTeam();var oldAlly=ally.getTeam();
        var team=board.addPlayerTeam("self_hit_fixture");team.setAllowFriendlyFire(false);
        var actors=new ArrayList<Entity>();var at=owner.position().add(0,24,0);
        var smoke=game.items().reactiveArtifacts();var spikes=game.items().hedgehogSpikes();
        var attribution=new IdentityHashMap<Map<Object,Object>,Map<Entity,Object>>();
        try{for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){
            field.setAccessible(true);@SuppressWarnings("unchecked")var map=(Map<Object,Object>)field.get(game.items().indirectKills());
            var entries=new HashMap<Entity,Object>();for(var target:List.of(owner,ally))if(map.containsKey(target))entries.put(target,map.get(target));attribution.put(map,entries);
        }}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        try{
            require(spikes.trackedCount()==0&&spikes.pendingCount()==0&&smoke.cloudCount()==0,"exclusive owned projectile/smoke fixture");
            for(var p:List.of(owner,ally)){
                p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);
                p.entityTags().removeAll(Set.of("spectator","time_stopped","sin_greed_active","nomagic_active"));
                board.addPlayerToTeam(p.getScoreboardName(),team);game.items().magic().inventoryChanged(p);
            }
            game.items().magic().flushCleansing();
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);ally.teleportTo(owner.level(),at.x+2,at.y,at.z,Set.of(),0,0,true);
            require(!owner.canHarmPlayer(ally),"native team gate is actually closed before projectile exception");
            var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(at.add(4,0,0));cow.setNoAi(true);cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(owner.level().addFreshEntity(cow),"friendly native mob publication");actors.add(cow);
            board.addPlayerToTeam(cow.getScoreboardName(),team);
            sourceArrows(game,owner,ally,cow,actors,at);
            for(var target:List.<LivingEntity>of(owner,ally,cow)){
                var arrow=new Arrow(owner.level(),owner,new ItemStack(Items.ARROW),null);actors.add(arrow);
                require(!canHit(arrow,owner),"native newborn arrow keeps initial muzzle owner immunity");
                arrow.setPos(at.add(8,1,0));arrow.setDeltaMovement(Vec3.ZERO);arrow.tick();arrow.setDeltaMovement(new Vec3(0,0,2));
                accepted(arrow,target,"native bow arrow "+target.getType());
            }
            for(var target:List.<LivingEntity>of(owner,ally,cow)){
                var arrow=spikes.emit(owner,LbcHedgehogItems.SpikeKind.STANDARD,at.add(8,1,0),Vec3.ZERO);require(arrow!=null,"native Baffled spike emitted");
                arrow.tick();arrow.setDeltaMovement(new Vec3(0,0,2));accepted(arrow,target,"Baffled spike "+target.getType());
            }
            var debuff=spikes.emit(owner,LbcHedgehogItems.SpikeKind.BLIND,at.add(8,1,0),Vec3.ZERO);require(debuff!=null,"real status spike emitted");
            debuff.tick();debuff.setDeltaMovement(new Vec3(0,0,2));accepted(debuff,owner,"self status spike");
            require(game.items().psycho().effectValue(owner,"piss")==6000,"self-hit retains actual spike payload");game.items().psycho().clearStatus(owner,"piss");
            for(var target:List.<LivingEntity>of(owner,ally,cow)){
                var bullet=new ShulkerBullet(owner.level(),owner,cow,net.minecraft.core.Direction.Axis.Y);actors.add(bullet);
                bullet.setPos(at.add(8,1,0));bullet.setDeltaMovement(Vec3.ZERO);bullet.tick();bullet.setDeltaMovement(new Vec3(0,0,2));
                accepted(bullet,target,"native Shulker bullet "+target.getType());require(target.hasEffect(MobEffects.LEVITATION),"self/friendly bullet retains native Levitation");
            }
            var ward=new Arrow(owner.level(),owner,new ItemStack(Items.ARROW),null);actors.add(ward);ward.setPos(at.add(8,1,0));ward.tick();ward.setDeltaMovement(new Vec3(0,0,2));
            cow.entityTags().add("strict_map_object");require(!canHit(ward,cow),"service entity remains protected");cow.entityTags().remove("strict_map_object");
            ally.entityTags().add("spectator");require(!canHit(ward,ally),"observer remains protected");ally.entityTags().remove("spectator");
            health(ally);ally.entityTags().add("sin_greed_active");float hp=ally.getHealth();
            try{require(canHit(ward,ally),"friendly Greed remains a physical reflector");hit(ward,ally);require(ally.getHealth()==hp&&ward.getDeltaMovement().z<0,"absolute Greed reflects without any damage");}
            finally{ally.entityTags().remove("sin_greed_active");}
            for(int variant:new int[]{1,2,3,4,5,6,7,8}){
                String flag=switch(variant){case 1->"confuse";case 2->"blind";case 3->"boom";case 4->"standart";case 5->"wither";case 6->"poison";case 7->"smoke";default->"smoke_old";};
                owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);ally.teleportTo(owner.level(),at.x+2,at.y,at.z,Set.of(),0,0,true);
                var stack=new ItemStack(Items.SNOWBALL,2);var data=new CompoundTag();data.putInt("special",1);data.putInt(flag,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                owner.setItemInHand(InteractionHand.MAIN_HAND,stack);int before=spikes.trackedCount();
                var result=UseItemCallback.EVENT.invoker().interact(owner,owner.level(),InteractionHand.MAIN_HAND);
                require(result.consumesAction()&&stack.getCount()==1&&spikes.trackedCount()==before+1,"actual vanilla carrier callback consumes exactly one "+flag);
                var shot=spikes.projectiles().getLast();shot.setPos(at.add(8,1,0));shot.setDeltaMovement(Vec3.ZERO);shot.tick();shot.setDeltaMovement(new Vec3(0,0,2));
                accepted(shot,variant%2==0?owner:ally,"ordinary throwable "+flag);
                if(variant==3){var spawned=new ArrayList<net.minecraft.world.entity.item.PrimedTnt>();owner.level().getAllEntities().forEach(e->{if(e instanceof net.minecraft.world.entity.item.PrimedTnt tnt&&game.items().babylonEffects().managedSecondary(tnt))spawned.add(tnt);});
                    require(spawned.size()==1&&spawned.getFirst().getFuse()==2&&spawned.getFirst().getOwner()==owner
                            &&game.items().babylonEffects().canAffect(spawned.getFirst(),owner)&&game.items().babylonEffects().canAffect(spawned.getFirst(),ally),"explosive dart uses attributed WILD native TNT fuse2");spawned.forEach(Entity::discard);}
                if(variant==2)require(owner.hasEffect(MobEffects.BLINDNESS)&&owner.getEffect(MobEffects.BLINDNESS).getDuration()==120,"dart native Blindness120");
                if(variant==5)require(ally.hasEffect(MobEffects.WITHER)&&ally.getEffect(MobEffects.WITHER).getAmplifier()==2,"dart native WitherIII");
                if(variant==6)require(owner.hasEffect(MobEffects.POISON)&&owner.getEffect(MobEffects.POISON).getDuration()==100,"dart native Poison100");
            }
            var mob=new Zombie(EntityTypes.ZOMBIE,owner.level());mob.setNoAi(true);mob.setNoGravity(true);mob.setPos(at.add(1,0,1));
            require(owner.level().addFreshEntity(mob),"friendly smoke mob publication");actors.add(mob);board.addPlayerToTeam(mob.getScoreboardName(),team);
            owner.removeAllEffects();ally.removeAllEffects();smoke.tick();
            require(owner.hasEffect(MobEffects.BLINDNESS)&&ally.hasEffect(MobEffects.BLINDNESS)
                    &&owner.getEffect(MobEffects.BLINDNESS).getAmplifier()==1,"grenade smoke is WILD and preserves source BlindnessII");
            require(Math.abs(mob.getAttributeValue(Attributes.FOLLOW_RANGE)-3)<1e-8,"allied mob is blinded inside smoke");
            owner.teleportTo(owner.level(),at.x+12,at.y,at.z,Set.of(),0,0,true);ally.teleportTo(owner.level(),at.x+14,at.y,at.z,Set.of(),0,0,true);mob.setPos(at.add(13,0,1));smoke.tick();
            require(!owner.hasEffect(MobEffects.BLINDNESS)&&!ally.hasEffect(MobEffects.BLINDNESS)&&mob.getAttributeValue(Attributes.FOLLOW_RANGE)>3,"leaving smoke restores players and mobs");
        }finally{
            spikes.stop();smoke.stop();game.items().psycho().clearStatus(owner,"piss");
            for(var actor:actors){if(actor.getTeam()==team)board.removePlayerFromTeam(actor.getScoreboardName(),team);actor.discard();}
            board.removePlayerTeam(team);if(oldOwner!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwner);if(oldAlly!=null)board.addPlayerToTeam(ally.getScoreboardName(),oldAlly);
            saved.restore(game);otherSaved.restore(game);game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(ally);game.items().magic().flushCleansing();
            attribution.forEach((map,entries)->{map.remove(owner);map.remove(ally);map.putAll(entries);});
        }
        SkyWars2.LOGGER.info("[check] PASS projectile-self-hits (native bow/Baffled/Shulker and source-bound NPC arrow self+allies+mob hurt, captured UUID/order, native reflection and absent author resolver, native owner departure, accepted spike payload, Greed/roles, real dart/grenade callback and WILD owned smoke enter/leave)");
    }
}
