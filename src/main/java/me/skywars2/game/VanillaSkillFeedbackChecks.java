package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/** Native vanilla-skill tick and accepted player-hit regressions from the November feedback. */
public final class VanillaSkillFeedbackChecks {
    private VanillaSkillFeedbackChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanilla skill feedback: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();f.move(f.owner,f.home.add(0,12,0));f.move(f.other,f.owner.position().add(0,0,2));
            game.config().vanillaSkills=true;game.config().ranked=true;
            game.events().restoreTimeline(new MatchEvents.Timeline(true,-1,-1));
            var profile=game.players().get(f.owner.getUUID());game.players().get(f.other.getUUID()).skill=0;
            var before=new HashSet<UUID>();for(var body:f.level.getAllEntities())before.add(body.getUUID());
            try{
                profile.skill=7;require(game.events().selectedSkill(f.owner)==7,"actual source skill gate accepts Furious");
                for(int health:new int[]{10,15,16}){
                    f.owner.removeAllEffects();f.owner.setHealth(health);game.events().tick();var effect=f.owner.getEffect(MobEffects.STRENGTH);
                    require(health==16?effect==null:effect!=null&&effect.getAmplifier()==(health==10?1:0)&&effect.getDuration()==60,
                            "native Furious preserves10/15HP thresholds and60ticks, with requested Strength II/I: health="+health+", actualHealth="+f.owner.getHealth()+", effect="+effect+", timeline="+game.events().checkpointTimeline()+", alive="+game.alivePlayers().contains(f.owner));
                }
                f.owner.removeAllEffects();f.owner.setHealth(1000);profile.skill=10;
                boolean hit=false;
                for(int attempt=0;attempt<64&&!hit;attempt++){
                    f.other.damageCooldownTime=0;f.other.setInvulnerableTime(0);f.other.setHealth(1000);f.other.setDeltaMovement(Vec3.ZERO);
                    f.owner.attack(f.other);
                    for(var body:f.level.getAllEntities())if(!before.contains(body.getUUID())&&body instanceof LightningBolt bolt){
                        require(bolt.position().distanceToSqr(f.other.position())<1e-8
                                &&bolt.position().distanceToSqr(f.owner.position())>1,"real outgoing Thunderer strike appears at actual damaged target");
                        hit=true;
                    }
                }
                require(hit,"at least one actual native50-percent Thunderer reaction within64 accepted attacks");
            }finally{for(var body:f.level.getAllEntities())if(!before.contains(body.getUUID()))f.own(body);}
        }
        SkyWars2.LOGGER.info("[check] PASS vanilla-skill-feedback (native Furious10/15HP thresholds and StrengthII/I; actual accepted melee Thunderer targets victim)");
    }
}
