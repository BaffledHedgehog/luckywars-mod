package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** Bounded native activation and payload checks; no private clock or random-field rewriting. */
public final class SauvojenBossChecks {
    private SauvojenBossChecks(){}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException("Sauvojen boss: "+detail);}
    private static void near(Vec3 actual,Vec3 expected,String detail){require(actual.distanceToSqr(expected)<1e-8,detail+" actual="+actual+" expected="+expected);}
    private static CompoundTag saved(Entity entity){var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,entity.registryAccess());entity.saveWithoutId(out);return out.buildResult();}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,beta;
        final GameChecks.WandThirdPlayerState otherSaved;final PlayerTeam oldOwnerTeam,oldBetaTeam,team;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Cow> cows=new ArrayList<>();LivingEntity boss;LbcSpellSource.Source authority;
        Fixture(GameController game){
            this.game=game;require(game.items().sauvojenBoss().count()==0&&game.items().sauvojenBoss().cloudCount()==0,"exclusive empty boss service");
            require(game.items().spellSources().current()==null,"no ambient source scope");
            base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;
            beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            otherSaved=new GameChecks.WandThirdPlayerState(beta,base.mana);oldOwnerTeam=owner.getTeam();oldBetaTeam=beta.getTeam();
            var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_boss_source")==null,"exclusive source fixture team");
            team=board.addPlayerTeam("sw2_boss_source");owner.level().getAllEntities().forEach(before::add);
            base.initialize();board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
            beta.setNoGravity(true);beta.setDeltaMovement(Vec3.ZERO);beta.teleportTo(owner.level(),owner.getX(),owner.getY(),owner.getZ()+4,Set.of(),0,0,true);
        }
        void activation(){
            ItemStack selected=ItemStack.EMPTY;
            for(int attempt=0;attempt<512&&selected.isEmpty();attempt++)for(var stack:game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:traps"))
                if(LbcSauvojenBoss.recognized(stack)){selected=stack.copyWithCount(2);break;}
            require(!selected.isEmpty(),"original native trap table reaches Sauvojen raw branch");
            var token=new ItemEntity(owner.level(),owner.getX(),owner.getY(),owner.getZ(),selected);token.setThrower(owner);token.setOnGround(true);token.setNoGravity(true);
            game.items().markCurrentItemEpoch(token);require(owner.level().addFreshEntity(token),"actual trap token publication");
            require(game.items().sauvojenBoss().ready(token)&&game.items().specialDrops().spawn(token),"actual shared native special-drop dispatch");
            require(token.getItem().getCount()==1&&game.items().sauvojenBoss().count()==1,"one native boss and exactly one token debit");token.discard();
            boss=(LivingEntity)game.items().sauvojenBoss().snapshot().getFirst();((Mob)boss).setNoAi(true);boss.setNoGravity(true);
            require(boss.getType()==EntityTypes.GHAST&&boss.getHealth()==100024&&boss.getMaxHealth()==100024,"source ghast and exact native100024HP");
            require(boss.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue()==256&&boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue()==256,"source follow/knockback bases");
            var data=saved(boss);require(data.getStringOr("DeathLootTable","").equals("lbc:sauvojen_tuntija")&&data.getIntOr("ExplosionPower",0)==3,"native source loot and blast3");
            require(boss.entityTags().containsAll(Set.of("sauvojen_tuntija","rcdenyalways"))&&game.items().sauvojenBoss().owner(boss)==owner,"source identity and independent causal author");
            near(boss.position(),owner.position().add(0,20,0),"source Y+20 spawn");
            authority=game.items().spellSources().source(boss);require(authority!=null&&authority.emitter().equals(boss.getUUID())&&authority.author().equals(owner.getUUID()),"explicit actual emitter binding");
        }
        LbcSpellSource.Source source(LbcSauvojen.Kind kind){
            var excluded=new HashSet<>(authority.excluded());if(kind==LbcSauvojen.Kind.TERRA||kind==LbcSauvojen.Kind.ELECTRO)excluded.add("summoned_cultist");
            return new LbcSpellSource.Source(authority.emitter(),authority.author(),authority.epoch(),authority.cause(),excluded);
        }
        Projectile shot(LbcSauvojen.Kind kind){
            return switch(kind){
                case RUBY->game.items().ruby().snapshot().getFirst();
                case TERRA->{Projectile found=null;for(var e:owner.level().getAllEntities())if(e instanceof LbcTerra.TerraProjectile p&&!p.isRemoved())found=p;yield Objects.requireNonNull(found);}
                case AIR->game.items().air().snapshot().getFirst();case ACID->game.items().clouds().acidSnapshot().getFirst();
                case TELEPORT->game.items().teleport().snapshot().getFirst();case STONE->game.items().stone().snapshot().getFirst();
                case FIRE->base.fire().getFirst();case BLIND->game.items().blind().snapshot().getFirst();default->throw new IllegalArgumentException();
            };
        }
        void payloads(){
            var origin=owner.position().add(4,4,-4);var direction=new Vec3(1,0,0);base.mana.set(owner,12345);
            for(var kind:LbcSauvojen.Kind.values()){
                var policy=source(kind);game.items().sauvojen().emitHostile(owner,kind,origin,direction,policy);
                require(game.items().spellSources().current()==null&&base.mana.get(owner)==12345,"producer restores source scope and never charges mana "+kind);
                if(kind==LbcSauvojen.Kind.ELECTRO){
                    var traces=game.items().wands().electroSnapshot();require(traces.size()==4,"four real hostile electrical records");
                    for(var trace:traces){require(trace.lifetime==20&&trace.age==0&&trace.sourcePolicy==policy&&trace.owner==owner,"four source20-tick electrical records");near(trace.position,origin,"electrical explicit origin");}
                    game.items().wands().tick();for(var trace:traces){require(trace.age==1,"one actual shared electrical engine step");near(trace.position,origin.add(3.6,0,0),"source nineteen-position electrical segment");}
                }else{
                    var shot=shot(kind);require(game.items().currentEpoch(shot)&&game.items().spellSources().owner(shot)==owner,"actual charge keeps causal wearer "+kind);
                    require(game.items().spellSources().source(shot)!=null,"actual charge keeps explicit hostile policy "+kind);near(shot.position(),origin,"native launch origin "+kind);
                    double speed=switch(kind){case AIR->.8;case TELEPORT->1.2;case STONE->1.6;case FIRE->.7;case BLIND->.99;case ACID->.5;default->1.8;};
                    near(shot.getDeltaMovement(),direction.scale(speed),"source velocity "+kind);
                    switch(kind){
                        case RUBY->{require(((LbcRuby.Charge)shot).sourceLifetime==60,"Ruby source60");game.items().ruby().tick();}
                        case TERRA->{require(((LbcTerra.TerraProjectile)shot).sourceLifetime==60,"Terra source60");game.items().terra().tick();}
                        case AIR->{require(((LbcAir.Charge)shot).sourceLifetime==120,"Air source120");game.items().air().tick();}
                        case ACID->{require(LbcClouds.acidLifetime((LbcClouds.AcidProjectile)shot)==300&&shot.getOwner()==null&&shot.entityTags().contains("shield_blockable2"),"source300 acid independent owner/shield");owner.level().tickNonPassenger(shot);game.items().clouds().tick();require(game.items().clouds().acidAge((LbcClouds.AcidProjectile)shot)==1,"actual source acid service age");}
                        case TELEPORT->{require(((LbcTeleport.Charge)shot).sourceLifetime==60,"Teleport source60");game.items().teleport().tick();}
                        case STONE->{require(((LbcStone.Charge)shot).lifetime==80,"Stone source80");game.items().stone().tick();}
                        case FIRE->{require(shot.getOwner()==boss,"native owner immunity belongs to actual emitter");owner.level().tickNonPassenger(shot);}
                        case BLIND->{require(((LbcBlind.Charge)shot).lifetime==120,"Blind source120");game.items().blind().tick();}
                        default->throw new IllegalStateException();
                    }
                    require(!shot.isRemoved(),"actual source charge survives one unobstructed native step "+kind);
                    if(kind!=LbcSauvojen.Kind.ACID)near(shot.position(),origin.add(direction.scale(kind==LbcSauvojen.Kind.FIRE?(speed+.1)*(double).95F:speed)),"actual engine movement "+kind);
                }
                require(game.items().spellSources().current()==null,"async engine restores source scope "+kind);base.clear();
            }
        }
        Cow cow(Vec3 at){var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(owner.level().addFreshEntity(cow),"actual living target");cows.add(cow);return cow;}
        void targeting(){
            require(!game.items().wands().target(owner,owner)&&!game.items().wands().target(owner,beta),"ordinary self/allied exclusions remain");
            try(var scope=game.items().spellSources().scope(authority)){
                require(game.items().wands().target(owner,owner)&&game.items().wands().target(owner,beta),"explicit hostile policy admits wearer and allied player");
                for(var player:List.of(owner,beta)){
                    var at=player.position().add(0,.5,0);var hit=game.items().wands().firstHit(owner,at.add(0,0,-1),at.add(0,0,1));
                    require(hit instanceof EntityHitResult entity&&entity.getEntity()==player,"native swept firstHit includes actual self/allied body");
                }
                try(var reaction=game.items().spellSources().scope((LbcSpellSource.Source)null)){require(!game.items().wands().target(owner,beta),"fresh reaction restores ordinary allegiance");}
                require(game.items().spellSources().current()==authority,"nested reaction restores exact outer source");
            }
            require(game.items().spellSources().current()==null,"source scope is fully restored");
            var origin=owner.position().add(4,2,0);var victim=cow(origin.add(.2,0,.2));var excluded=cow(origin.add(.3,0,.3));excluded.entityTags().add("summoned_cultist");
            game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);
            require(!game.items().wands().target(owner,victim),"actual team-alias nonplayer protected ordinarily");
            game.items().sauvojen().emitHostile(owner,LbcSauvojen.Kind.TERRA,origin,new Vec3(0,0,1),source(LbcSauvojen.Kind.TERRA));game.items().terra().tick();
            require(victim.getHealth()==993&&victim.getLastDamageSource()!=null&&victim.getLastDamageSource().is(DamageTypes.CACTUS)&&victim.getKillCredit()==owner,"actual allied target receives source7 native cactus with wearer credit");
            require(excluded.getHealth()==1000,"source summoned-cultist exclusion survives actual Terra samples");base.clear();victim.discard();excluded.discard();
            var blindVictim=cow(origin.add(.2,0,.2));game.items().sauvojen().emitHostile(owner,LbcSauvojen.Kind.BLIND,origin,new Vec3(0,0,1),source(LbcSauvojen.Kind.BLIND));game.items().blind().tick();
            var blindness=blindVictim.getEffect(MobEffects.BLINDNESS);var wither=blindVictim.getEffect(MobEffects.WITHER);
            require(blindVictim.getHealth()==1000&&blindness!=null&&blindness.getDuration()==80&&wither!=null&&wither.getDuration()==80&&wither.getAmplifier()==1,"hostile Blind source effects have no invented direct damage");base.clear();blindVictim.discard();
        }
        void reflection(){
            var origin=owner.position().add(4,4,-4);game.items().sauvojen().emitHostile(owner,LbcSauvojen.Kind.RUBY,origin,new Vec3(1,0,0),authority);
            var shot=game.items().ruby().snapshot().getFirst();var before=shot.getDeltaMovement();boolean already=beta.entityTags().contains("sin_greed_active");
            beta.entityTags().add("sin_greed_active");try{require(LbcGreed.reflect(shot,beta),"actual shared native Greed reflection callback");}finally{if(!already)beta.entityTags().remove("sin_greed_active");}
            require(shot.getOwner()==beta&&game.items().spellSources().source(shot)==null,"real reflection transfers owner and drops hostile source");near(shot.getDeltaMovement(),before.reverse(),"reflection preserves speed/reverses motion");
            try(var scope=game.items().spellSources().scope(shot)){require(!game.items().wands().target(beta,owner),"reflected ordinary projectile protects allied causal wearer");}
            require(game.items().spellSources().current()==null,"reflection scope restores caller");base.clear();
            game.items().sauvojen().emitHostile(owner,LbcSauvojen.Kind.RUBY,origin,new Vec3(1,0,0),authority);
            var sameAuthor=game.items().ruby().snapshot().getFirst();
            try(var scope=game.items().spellSources().scope(authority)){game.items().electro().assignOwner(sameAuthor,owner);}
            require(sameAuthor.getOwner()==owner&&game.items().spellSources().source(sameAuthor)==null,
                    "same-author reflection epoch stamp cannot rebind the old hostile scope");base.clear();
        }
        @Override public void close(){
            try{for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}game.items().sauvojenBoss().stop();base.clear();
                for(var e:owner.level().getAllEntities())if(!before.contains(e)&&!(e instanceof ServerPlayer))e.discard();
            }finally{var board=game.server().getScoreboard();board.removePlayerTeam(team);if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldBetaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldBetaTeam);otherSaved.restore(game);base.close();}
        }
    }
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.activation();fixture.payloads();fixture.targeting();fixture.reflection();}
        SkyWars2.LOGGER.info("[check] PASS sauvojen-boss (native shared token activation; nine explicit source payloads and actual engine steps; source7 cactus, Blind80/WitherII, self/allied swept targets, actual Greed reflection, nested source restoration)");
        SkyWars2.LOGGER.info("[check] PENDING sauvojen-boss continuation: native random10 coverage/original40-tick cadence and actual player-retaliation scope integration require real server ticks; no private clocks or fake scheduling were used");
    }
}
