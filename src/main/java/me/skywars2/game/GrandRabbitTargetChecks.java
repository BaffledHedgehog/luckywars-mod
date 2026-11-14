package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Actual native rabbit AI selects and bites passive, hostile, ordinary-rabbit and same-team mobs. */
final class GrandRabbitTargetChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Grand rabbit targets: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int start;final boolean oldTag;
        final Set<UUID> before=new HashSet<>();net.minecraft.world.scores.PlayerTeam team;
        Rabbit rabbit,family;Cow service;Mob victim;int kind,at;float familyHp;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);start=game.server().getTickCount();oldTag=f.owner.entityTags().contains("sin_gluttony");for(var e:game.items().witchGluttony().snapshot())before.add(e.getUUID());}
        void setup(){
            require(before.isEmpty(),"exclusive actor family");f.prepareParticipants();f.owner.entityTags().add("sin_gluttony");
            var board=game.server().getScoreboard();require(board.getPlayerTeam("rabbit_target_qa")==null,"owned team unused");team=board.addPlayerTeam("rabbit_target_qa");board.addPlayerToTeam(f.owner.getScoreboardName(),team);
            var cell=BlockPos.containing(f.home);for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)f.set(cell.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            f.move(f.owner,f.home.add(20,0,0));f.move(f.other,f.home.add(24,0,0));
            rabbit=spawn(f.home);family=spawn(f.home.add(.25,0,0));family.setNoAi(true);family.setNoGravity(true);familyHp=family.getHealth();
            service=(Cow)body(new Cow(EntityTypes.COW,f.level),f.home.add(.5,0,0));service.entityTags().add("strict_map_object");
            game.items().witchGluttony().loaded(rabbit);game.items().witchGluttony().loaded(rabbit);
            require(((me.skywars2.mixin.SummonTargetAccess)rabbit).skywars2$summonTargets().getAvailableGoals().stream().filter(g->g.getGoal().getClass().getSimpleName().equals("GrandRabbitTarget")).count()==1,"publication/reload installs exactly one native target goal");next();
        }
        Rabbit spawn(Vec3 at){var actor=game.items().witchGluttony().spawn(LbcWitchGluttony.Kind.RABBIT,f.level,at,0,0,f.owner.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(f.owner));require(actor instanceof Rabbit,"actual source native rabbit spawn");return (Rabbit)actor;}
        Mob body(Mob mob,Vec3 at){mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);mob.setSilent(true);mob.setPersistenceRequired();mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);mob.setHealth(1000);require(f.level.addFreshEntity(mob),"actual native target publication");f.owned.add(mob.getUUID());return mob;}
        void next(){
            if(victim!=null)f.discard(victim);
            victim=body(switch(kind){case 0->new Cow(EntityTypes.COW,f.level);case 1->new Zombie(EntityTypes.ZOMBIE,f.level);default->new Rabbit(EntityTypes.RABBIT,f.level);},rabbit.position().add(1,0,0));
            if(victim instanceof Zombie)victim.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            if(kind==0){game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);require(victim.getTeam()==rabbit.getTeam(),"actual same-team target is a positive control");}
            at=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-start<170,"bounded native AI pursuit and melee");
            require(family.getHealth()==familyHp,"family was not bitten");require(service.getHealth()==1000,"service role excluded");
            for(var actor:game.items().witchGluttony().snapshot())if(actor instanceof Rabbit r&&r.getTarget()!=null)require(!r.getTarget().entityTags().contains("grand_rabbit"),"all current parents/children exclude their own family");
            if(victim.getHealth()==1000){require(game.server().getTickCount()-at<55,"native target selected and actually bitten, kind="+kind+", target="+rabbit.getTarget()+", actor="+rabbit.position()+", victim="+victim.position());return false;}
            var source=victim.getLastDamageSource();require(source!=null&&source.getDirectEntity() instanceof Rabbit,"HP loss came from actual native rabbit melee, not environment");
            if(++kind==3)return true;next();return false;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.items().witchGluttony().stop();if(!oldTag)f.owner.entityTags().remove("sin_gluttony");if(team!=null)game.server().getScoreboard().removePlayerTeam(team);}finally{f.close();}}
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS grand-rabbit-targets (actual native acquisition and melee against allied passive, hostile and ordinary-rabbit mobs, family/service exclusions and idempotent goal installation)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL grand-rabbit-targets",error);}}
}
