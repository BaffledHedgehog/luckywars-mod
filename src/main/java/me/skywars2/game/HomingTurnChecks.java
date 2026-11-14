package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.Vec3;

/** Actual native flights near a player, a logical card and invariant checks of the shared angular step. */
final class HomingTurnChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Homing turn: "+why);}
    private static double angle(Vec3 a,Vec3 b){return Math.toDegrees(Math.atan2(a.cross(b).length(),a.dot(b)));}
    private static Vec3 heading(double degrees){return new Vec3(Math.sin(Math.toRadians(degrees)),0,Math.cos(Math.toRadians(degrees)));}
    private static void geometry(){
        for(double degrees:new double[]{-180,-90,-1.1,-.5,0,.5,1.1,90,180}){
            var goal=heading(degrees);var motion=new Vec3(0,0,7.25);double remaining=angle(motion,goal);
            for(int i=0;i<181;i++){
                var next=ProjectileSteering.turn(motion,goal,Math.PI/180);double after=angle(next,goal);
                require(Math.abs(next.length()-7.25)<1e-10,"angular step preserves physical speed");
                require(angle(motion,next)<=1.00000001&&after<=remaining+1e-8,"bounded monotonic turn without overshoot");
                if(remaining<1)require(after<1e-8,"sub-degree remainder aligns exactly");
                motion=next;remaining=after;
            }
            require(remaining<1e-8,"even opposite direction reaches its target within180 angular steps");
        }
        require(ProjectileSteering.turn(Vec3.ZERO,new Vec3(0,1,0),1).equals(Vec3.ZERO),"stationary projectile never invents motion");
        require(ProjectileSteering.turn(new Vec3(1,2,3),Vec3.ZERO,1).equals(new Vec3(1,2,3)),"coincident target keeps finite original motion");
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final List<Projectile> shots=new ArrayList<>();
        final List<Vec3> initial=new ArrayList<>();LivingEntity target;LbcDanmaku.Card card;Vec3 cardStart,cardGoal;
        int at,phase=-1;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        <T extends Projectile>T shot(T body,Vec3 point,Vec3 direction,net.minecraft.server.level.ServerPlayer author){
            body.setOwner(author);body.setPos(point);body.setDeltaMovement(direction);body.setNoGravity(true);
            require(f.level.addFreshEntity(body),"real native projectile publication");f.owned.add(body.getUUID());shots.add(body);initial.add(direction);return body;
        }
        void setup(){
            geometry();f.prepareParticipants();game.config().lbcSkills=true;game.config().ranked=true;
            game.players().get(f.owner.getUUID()).lbcSkill=30;game.players().get(f.other.getUUID()).lbcSkill=0;
            require(game.lbc().hasSkill(f.owner,30)&&!game.lbc().hasSkill(f.other,30),"actual personal skill admission");
            f.move(f.owner,f.home.add(0,60,0));f.move(f.other,f.owner.position().add(30,0,0));
            var cow=new Cow(EntityTypes.COW,f.level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(f.owner.position().add(0,4,15));
            require(f.level.addFreshEntity(cow),"native target publication");target=cow;f.owned.add(cow.getUUID());
        }
        void launch(){
            var at=target.getEyePosition().add(0,0,-15);
            shot(new Snowball(EntityTypes.SNOWBALL,f.level),at,heading(.4),f.owner);
            shot(new Arrow(EntityTypes.ARROW,f.level),at,heading(15),f.owner);
            shot(new Snowball(EntityTypes.SNOWBALL,f.level),at,heading(15),f.other);
            shot(new Snowball(EntityTypes.SNOWBALL,f.level),at,heading(15),null);
            require(f.owner.position().distanceToSqr(at)<225,"native flights inside former blanket player exclusion");
            cardStart=at.add(0,2,0);cardGoal=target.getEyePosition().subtract(cardStart);card=game.items().danmaku().card(f.owner,cardStart,cardGoal.normalize().add(.001,0,0),false);
            require(card!=null,"actual shared logical card producer");f.owned.add(card.display.getUUID());this.at=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();
            if(phase==-1){launch();phase=0;return false;}
            int elapsed=game.server().getTickCount()-at;if(elapsed==0)return false;require(elapsed==1,"one native physics frame observed");
            var fine=shots.get(0);var coarse=shots.get(1);require(!fine.isRemoved()&&!coarse.isRemoved(),"own native flights remain physical");
            require(angle(fine.getDeltaMovement(),target.getEyePosition().subtract(fine.position()))<1e-6,"native near-aligned throwable reaches exact eye direction in one frame");
            require(Math.abs(angle(initial.get(1),coarse.getDeltaMovement())-1)<1e-6,"native arrow turns exactly one degree, independently of speed");
            require(coarse.getDeltaMovement().length()>1.4,"original arrow acceleration remains active");
            for(int i=2;i<4;i++)require(!shots.get(i).isRemoved()&&angle(initial.get(i),shots.get(i).getDeltaMovement())<1e-7,"foreign and unowned projectiles keep original direction");
            require(!card.display.isRemoved()&&card.age==1&&angle(card.direction,cardGoal)<1e-6,"logical card shares exact sub-degree alignment and one real active step");
            require(fine.getOwner()==f.owner&&coarse.getOwner()==f.owner&&shots.get(2).getOwner()==f.other&&shots.get(3).getOwner()==null,"physical ownership remains independent of nearby skill user");
            return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.items().danmaku().stop();}finally{f.close();}}
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS homing-turn (native one-degree arrow turn with source acceleration, exact final throwable/card alignment near players, foreign/null ownership and bounded antipodal geometry)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL homing-turn",error);}}
}
