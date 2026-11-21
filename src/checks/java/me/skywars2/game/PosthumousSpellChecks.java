package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Delayed native spell contact after the actual hostile mob's body has left the world. */
public final class PosthumousSpellChecks {
    private PosthumousSpellChecks(){}
    private static Scene running;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Posthumous spell: "+detail);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final SauvojenBossChecks.Fixture f;
        Projectile shot;Cow target;Vec3 parking;
        int phase,when,started,alphaKills,betaKills;boolean closed;
        Scene(GameController game){this.game=game;f=new SauvojenBossChecks.Fixture(game);}
        void begin(){
            alphaKills=f.owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(net.minecraft.world.entity.EntityTypes.COW));
            betaKills=f.beta.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(net.minecraft.world.entity.EntityTypes.COW));
            f.activation();started=when=game.server().getTickCount();parking=f.owner.position().add(0,50,0);
            target=f.cow(parking.add(0,0,6));
            require(target.hurtServer(f.owner.level(),target.damageSources().playerAttack(f.owner),1),"independent real prior player hit");
            require(target.getKillCredit()==f.owner,"positive control: actual earlier player owns combat credit");
            game.items().sauvojen().emitHostile(f.owner.level(),LbcSauvojen.Kind.TERRA,parking,new Vec3(1,0,0),f.source(LbcSauvojen.Kind.TERRA));
            shot=f.shot(LbcSauvojen.Kind.TERRA);require(shot!=null&&game.items().spellSources().nativeMobSource(shot)==f.boss,"accepted real charge resolves its living NPC emitter");
            require(f.boss.hurtServer(f.owner.level(),f.boss.damageSources().genericKill(),Float.MAX_VALUE)&&!f.boss.isAlive(),"real native boss death");
        }
        boolean step(){
            require(game.playing()&&game.server().getTickCount()-started<55,"bounded native flight/death scene");
            if(phase==0){
                require(!shot.isRemoved(),"already emitted persistent native charge survives emitter death");shot.setPos(parking);
                if(f.owner.level().getEntity(f.boss.getUUID())!=null){require(game.server().getTickCount()-when<30,"actual corpse eventually leaves native world");return false;}
                shot.setPos(target.position().add(-.2,0,-.2));shot.setDeltaMovement(1.8,0,0);target.setInvulnerableTime(0);target.damageCooldownTime=0;phase=1;when=game.server().getTickCount();return false;
            }
            if(phase==1&&target.getHealth()==999){require(game.server().getTickCount()-when<3,"actual existing charge makes delayed native contact");return false;}
            if(phase==2&&target.isAlive()){require(game.server().getTickCount()-when<3,"same accepted charge makes a real final killing contact");return false;}
            var damage=target.getLastDamageSource();require(damage!=null&&damage.getDirectEntity()==shot&&LbcIndirectKills.npcSource(damage),"real native charge and explicit NPC damage wrapper retained");
            require(damage.getEntity()==f.boss&&target.getKillCredit()==f.boss,"removed emitter retains native damage and kill credit, replacing earlier player cause: source="+damage.getEntity()+" credit="+target.getKillCredit()+" expected="+f.boss);
            if(phase==1){
                target.setInvulnerableTime(0);target.damageCooldownTime=0;
                require(target.hurtServer(f.owner.level(),target.damageSources().playerAttack(f.beta),1)&&target.getKillCredit()==f.beta,"a newer real player hit supersedes the removed NPC");
                target.setHealth(7);target.setInvulnerableTime(0);target.damageCooldownTime=0;shot.setPos(target.position().add(-.2,0,-.2));shot.setDeltaMovement(1.8,0,0);
                phase=2;when=game.server().getTickCount();return false;
            }
            require(!target.isAlive()&&f.owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(net.minecraft.world.entity.EntityTypes.COW))==alphaKills
                    &&f.beta.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(net.minecraft.world.entity.EntityTypes.COW))==betaKills,"actual NPC death gives neither former player a kill");return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{f.close();}finally{
            f.owner.getStats().setValue(f.owner,net.minecraft.stats.Stats.ENTITY_KILLED.get(net.minecraft.world.entity.EntityTypes.COW),alphaKills);
            f.beta.getStats().setValue(f.beta,net.minecraft.stats.Stats.ENTITY_KILLED.get(net.minecraft.world.entity.EntityTypes.COW),betaKills);
        }}
    }
    public static void run(GameController game){require(running==null,"exclusive scene");running=new Scene(game);try{running.begin();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){var scene=running;if(scene==null)return;try{if(!scene.step())return;scene.close();running=null;SkyWars2.LOGGER.info("[check] PASS posthumous-spell (real earlier PvP hit, native hostile boss death/removal, persistent accepted Terra flight and delayed native NPC damage/kill credit, newer-player override and real NPC final death/no borrowed player reward)");}catch(Throwable error){try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL posthumous-spell",error);}}
}
