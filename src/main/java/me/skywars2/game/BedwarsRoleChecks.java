package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real placed shop/native admission and registered cloud ticks; root owns mode3 scene and rewind. */
public final class BedwarsRoleChecks {
    private BedwarsRoleChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("BedWars shop role: "+why);}
    public static Probe start(GameController game,ServerPlayer owner,ServerPlayer enemy,Villager shop){
        var probe=new Probe(game,owner,enemy,shop);try{probe.prepare();return probe;}catch(RuntimeException|Error failure){try{probe.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
    /** Root supplies three real enrolled players from its four-player/two-team scene. */
    public static void allies(GameController game,ServerPlayer owner,ServerPlayer teammate,ServerPlayer enemy){
        require(LbcBedwarsHarm.active()&&owner!=teammate&&owner.isAlliedTo(teammate)&&!owner.isAlliedTo(enemy),"actual two-team enrolled ally/enemy roster");
        var level=owner.level();require(teammate.level()==level&&enemy.level()==level,"same real BedWars arena");
        var saved=new GameChecks.WandThirdPlayerState(teammate,game.items().mana());var hostileSaved=new GameChecks.WandThirdPlayerState(enemy,game.items().mana());
        int invulnerable=teammate.getInvulnerableTime(),hostileInvulnerable=enemy.getInvulnerableTime(),cooldown=teammate.damageCooldownTime,hostileCooldown=enemy.damageCooldownTime;
        var board=game.server().getScoreboard();var actors=new ArrayList<Entity>();
        try{
            for(var p:List.of(teammate,enemy)){p.getInventory().clearContent();p.removeAllEffects();p.setHealth(Math.min(20,p.getMaxHealth()));p.setInvulnerableTime(0);p.damageCooldownTime=0;}
            float health=teammate.getHealth();
            require(!teammate.hurtServer(level,teammate.damageSources().playerAttack(owner),1)&&teammate.getHealth()==health,"actual ordinary native teammate melee denied independently of scoreboard FF flag");
            var friend=new Cow(EntityTypes.COW,level);friend.setPos(owner.position().add(3,0,0));friend.setNoAi(true);friend.setNoGravity(true);require(level.addFreshEntity(friend),"actual friendly native mob fixture");actors.add(friend);board.addPlayerToTeam(friend.getStringUUID(),owner.getTeam());
            float friendHealth=friend.getHealth();
            require(!friend.hurtServer(level,friend.damageSources().playerAttack(owner),1)&&friend.getHealth()==friendHealth,"ordinary incoming melee also protects same-team native mobs");
            var attacker=new Zombie(EntityTypes.ZOMBIE,level);attacker.setPos(owner.position().add(4,0,0));attacker.setNoAi(true);require(level.addFreshEntity(attacker),"actual native allied attacker");actors.add(attacker);board.addPlayerToTeam(attacker.getStringUUID(),owner.getTeam());
            require(!attacker.doHurtTarget(level,friend)&&friend.getHealth()==friendHealth,"actual native allied mob attack cannot damage its teammate");
            var arrow=new Arrow(EntityTypes.ARROW,level);arrow.setOwner(owner);arrow.setPos(owner.getEyePosition());actors.add(arrow);
            var hit=(me.skywars2.mixin.BedwarsRoleGuardMixin.ProjectileOwner)(Object)arrow;
            require(!hit.skywars2$canHit(teammate)&&!hit.skywars2$canHit(friend)&&hit.skywars2$canHit(enemy),"actual native ordinary arrow collision excludes allied player/mob before impact payloads");
            require(!teammate.hurtServer(level,teammate.damageSources().arrow(arrow,owner),1)&&teammate.getHealth()==health,"ordinary native arrow damage also rejects a direct/server bypass of collision");
            require(!teammate.addEffect(new MobEffectInstance(MobEffects.POISON,60),owner)&&!teammate.addEffect(new MobEffectInstance(MobEffects.HUNGER,60),owner),"ordinary harmful status/resource grants reject before application");
            require(teammate.addEffect(new MobEffectInstance(MobEffects.REGENERATION,60),owner),"ordinary beneficial teammate aid remains admitted");
            var motion=teammate.getDeltaMovement();teammate.knockback(1,1,0,teammate.damageSources().playerAttack(owner),1,true);
            require(teammate.getDeltaMovement().equals(motion),"ordinary source-bearing knockback rejects without velocity mutation");
            require(enemy.hurtServer(level,enemy.damageSources().playerAttack(owner),1),"actual enemy melee positive control");
            teammate.setInvulnerableTime(0);teammate.damageCooldownTime=0;
            var conduction=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_CONDUCTION),owner);
            float before=teammate.getHealth();require(teammate.hurtServer(level,conduction,1)&&teammate.getHealth()<before,"registered conduction WILD exception still applies real allied native damage");
            var bomb=new net.minecraft.world.entity.item.PrimedTnt(level,owner.getX(),owner.getY(),owner.getZ(),owner);bomb.entityTags().add(LbcDamage.UNPROTECTED_TNT);actors.add(bomb);
            teammate.setInvulnerableTime(0);teammate.damageCooldownTime=0;var blast=teammate.damageSources().explosion(bomb,owner);
            require(!teammate.hurtServer(level,blast,1),"ordinary owned native TNT source cannot harm an ally");
            var wild=game.items().indirectKills().captureDamage(blast,game.items().indirectKills().wildCause(owner));
            before=teammate.getHealth();require(teammate.hurtServer(level,wild,1)&&teammate.getHealth()<before,"explicit accepted WILD bomb cause still applies actual native allied damage");
            boolean[] applied={false};game.items().indirectKills().withCause(teammate,game.items().indirectKills().wildCause(owner),()->applied[0]=teammate.addEffect(new MobEffectInstance(MobEffects.POISON,60),owner));
            require(applied[0],"explicit accepted WILD status policy remains authoritative");
            SkyWars2.LOGGER.info("[check] PASS bedwars-allies (real native ally/enemy melee, mob attack, arrow collision/damage, harmful/resource/beneficial status and knockback; ordinary TNT denied; registered conduction/accepted WILD bomb and status positive controls; no scoreboardFF toggle)");
        }finally{
            for(var actor:actors){if(actor.getTeam()!=null)board.removePlayerFromTeam(actor.getStringUUID(),actor.getTeam());if(actor instanceof LivingEntity living)game.items().indirectKills().clear(living);actor.discard();}
            saved.restore(game);hostileSaved.restore(game);teammate.setInvulnerableTime(invulnerable);teammate.damageCooldownTime=cooldown;enemy.setInvulnerableTime(hostileInvulnerable);enemy.damageCooldownTime=hostileCooldown;
        }
    }
    public static final class Probe implements AutoCloseable {
        final GameController game;final ServerPlayer owner,enemy;final Villager shop;
        final Set<String> tags;final boolean invulnerable;final float health;final int fire;final Vec3 motion;
        final List<Entity> actors=new ArrayList<>();Cow friend,target;Zombie attacker;AreaEffectCloud cloud;int began;boolean closed;
        Probe(GameController game,ServerPlayer owner,ServerPlayer enemy,Villager shop){
            this.game=game;this.owner=owner;this.enemy=enemy;this.shop=shop;
            require(game.config().gameMode==3&&game.playing()&&game.bedwars().arena().active()&&!game.items().timeStop().stopped((ServerLevel)shop.level()),"real active ordinary mode3 world");
            require(owner.getTeam()!=null&&enemy.getTeam()!=null&&owner.getTeam()!=enemy.getTeam(),"two actual native participant teams");
            require(game.bedwars().arena().topology().markers().stream().anyMatch(m->m.kind()==LbcBedwarsArena.MarkerKind.SHOP&&m.id().equals(shop.getUUID())),"actual placed shop UUID in canonical topology");
            require(LbcBedwars.shopRole(shop)&&!shop.hasEffect(MobEffects.REGENERATION)&&!shop.hasEffect(MobEffects.POISON),"clean protected source shop");
            tags=Set.copyOf(shop.entityTags());invulnerable=shop.isPermanentlyInvulnerable();health=shop.getHealth();fire=shop.getRemainingFireTicks();motion=shop.getDeltaMovement();
        }
        <T extends Entity>T add(T actor,Vec3 at){actor.setPos(at);require(shop.level().addFreshEntity(actor)&&!actor.isRemoved(),"real native fixture publication");actors.add(actor);return actor;}
        void prepare(){
            var at=shop.position();var level=(ServerLevel)shop.level();var board=game.server().getScoreboard();
            friend=add(new Cow(EntityTypes.COW,level),at.add(5,0,0));friend.setNoAi(true);friend.setNoGravity(true);board.addPlayerToTeam(friend.getStringUUID(),owner.getTeam());
            target=add(new Cow(EntityTypes.COW,level),at.add(.4,0,0));target.setNoAi(true);target.setNoGravity(true);board.addPlayerToTeam(target.getStringUUID(),enemy.getTeam());
            attacker=add(new Zombie(EntityTypes.ZOMBIE,level),at.add(5,0,2));attacker.setNoAi(true);attacker.setNoGravity(true);
            shop.setPermanentlyInvulnerable(false);shop.setHealth(10);
            // Simulate a lost/changed representation tag. Immutable placed identity still owns the role.
            shop.entityTags().removeAll(Set.of("bedwars_shop",LbcBedwarsRoleGuards.ROLE,"strict_map_object"));
            require(LbcBedwars.shopRole(shop)&&!game.items().wands().validTarget(shop),"durable canonical role survives missing mutable representation tags");
            require(!shop.hurtServer(level,shop.damageSources().playerAttack(owner),3)&&shop.getHealth()==10,"actual native direct damage denied despite invulnerable=false");
            shop.heal(5);require(shop.getHealth()==10,"native beneficial healing is also rejected");
            require(!shop.addEffect(new MobEffectInstance(MobEffects.POISON,100),owner)&&!shop.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100),owner),"both harmful and beneficial native effect grants reject before ownership/modifier payloads");
            require(!shop.isAffectedByPotions()&&!shop.canBeAffected(new MobEffectInstance(MobEffects.REGENERATION,100)),"native potion/cloud selection excludes shop before victims/radius bookkeeping");
            var glow=shop.getEffect(MobEffects.GLOWING);shop.forceAddEffect(new MobEffectInstance(MobEffects.GLOWING,100),owner);require(shop.getEffect(MobEffects.GLOWING)==glow,"native forced grant cannot bypass shop role");
            shop.push(1,1,1);require(shop.getDeltaMovement().equals(motion),"native direct push admission preserves velocity");
            shop.knockback(1,1,0,shop.damageSources().playerAttack(owner),1,true);require(shop.getDeltaMovement().equals(motion),"native source-bearing knockback admission preserves velocity");
            shop.igniteForTicks(100);require(shop.getRemainingFireTicks()==fire,"native ignition rejected without fire/freeze side effects");
            attacker.setTarget(shop);require(attacker.getTargetUnchecked()==null&&!attacker.canAttack(shop),"actual hostile Mob targeting/attack admission rejects protected shop");
            attacker.setTarget(target);require(attacker.getTargetUnchecked()==target&&attacker.canAttack(target),"ordinary native target remains admitted");
            shop.setTarget(target);require(shop.getTargetUnchecked()==null,"shop cannot become a hostile actor source");
            int[] converted={0};var witch=shop.convertTo(EntityTypes.WITCH,ConversionParams.single(shop,false,false),EntitySpawnReason.CONVERSION,mob->converted[0]++);
            require(witch==null&&converted[0]==0&&!shop.isRemoved(),"native conversion rejects before creating/publishing replacement or invoking callback");
            var lightning=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);shop.thunderHit(level,lightning);require(!shop.isRemoved()&&shop.getHealth()==10&&shop.getRemainingFireTicks()==fire,"native lightning rejects before witch/fallback ignition");
            require(!game.items().polymorph().applyTimed(shop,game.items().indirectKills().wildCause(owner),40),"actual custom polymorph factory rejects before a sheep/body publication");
            game.items().mobgun().dispose(shop);require(!shop.isRemoved(),"shared direct erase admission preserves canonical shop");
            int traces=game.items().wands().electroCount();game.items().wands().mechanismElectro(shop,at,new Vec3(0,0,1));
            game.items().wands().chainStatusElectro(shop,owner,at,game.items().indirectKills().wildCause(owner));require(game.items().wands().electroCount()==traces,"actual borrowed NPC/status center rejects before trace publication");
            var policy=game.items().spellSources().hostile(shop,owner,Set.of());require(policy!=null&&!game.items().spellSources().target(policy,target),"typed factory remains total while effect admission rejects shop emitter");
            var shot=new Arrow(EntityTypes.ARROW,level);shot.setOwner(owner);shot.setPos(at);
            try(var scope=game.items().spellSources().scope(policy,level)){require(!level.addFreshEntity(shot)&&level.getEntity(shot.getUUID())==null,"native shared-source publication rejects before registering a borrowed-center body");}finally{shot.discard();}
            require(!game.items().wands().target(owner,friend)&&game.items().wands().target(owner,target),"ordinary typed target predicate distinguishes actual ally/enemy teams");
            var wild=game.items().spellSources().hostile(owner,owner,Set.of());require(game.items().spellSources().target(wild,friend),"existing explicit WILD policy still includes allies; no blanket friendly-fire rewrite");
            shop.entityTags().clear();shop.entityTags().addAll(tags);
            for(var e:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(1.5)))require(e==shop||e==target,"isolated actual cloud radius for resource-use census");
            cloud=add(new AreaEffectCloud(level,at.x,at.y,at.z),at);cloud.setOwner(owner);cloud.setRadius(1.5f);cloud.setRadiusOnUse(-.25f);cloud.setWaitTime(0);cloud.setDuration(100);cloud.setRadiusPerTick(0);
            cloud.setPotionContents(new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.REGENERATION,100)),Optional.empty()));began=game.server().getTickCount();
        }
        /** Call after ordinary registered entity ticks; never ticks the cloud/service artificially. */
        public boolean finish(){
            int elapsed=game.server().getTickCount()-began;require(elapsed<30,"bounded native cloud proof");if(elapsed<6||cloud.tickCount<5)return false;
            require(!shop.hasEffect(MobEffects.REGENERATION)&&shop.getHealth()==10,"actual native cloud leaves shop health/effects unchanged");
            require(target.hasEffect(MobEffects.REGENERATION)&&Math.abs(cloud.getRadius()-1.25f)<1e-5,"ordinary cow receives native cloud effect; radius charges exactly once and never charges shop");
            SkyWars2.LOGGER.info("[check] PASS bedwars-shop-role (actual placed UUID/tag durability; invulnerable-off damage/heal/effect/impulse/AI/conversion/lightning; real custom erase/polymorph/borrowed center; native publication; two-team predicates and WILD control; registered native cloud resource census)");return true;
        }
        public void close(){if(closed)return;closed=true;var board=game.server().getScoreboard();for(var actor:actors){if(actor.getTeam()!=null)board.removePlayerFromTeam(actor.getStringUUID(),actor.getTeam());if(actor instanceof LivingEntity living)game.items().indirectKills().clear(living);actor.discard();}
            shop.entityTags().clear();shop.entityTags().addAll(tags);shop.setPermanentlyInvulnerable(invulnerable);shop.setHealth(health);shop.setRemainingFireTicks(fire);shop.setDeltaMovement(motion);
        }
    }
}
