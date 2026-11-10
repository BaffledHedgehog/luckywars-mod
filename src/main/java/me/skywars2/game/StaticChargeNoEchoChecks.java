package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Native external hits release charge; a discharge must not trigger its own return wave. */
public final class StaticChargeNoEchoChecks {
    private StaticChargeNoEchoChecks(){}
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Static charge echo: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final LbcStaticCharge service;
        int started,phase;float ownerHp,otherHp;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);service=game.items().legacyEffects().staticCharge();}
        int charge(net.minecraft.world.entity.Entity actor){return game.items().mana().hudValue(actor,LbcStaticCharge.COUNTER);}
        void begin(){f.prepareParticipants();f.move(f.owner,f.home.add(0,30,0));f.move(f.other,f.owner.position().add(4,0,0));
            f.owner.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,200,126),f.other);
            require(charge(f.owner)==10,"actual potion marker gives ten charge");
            hit(f.owner,f.other);ownerHp=f.owner.getHealth();otherHp=f.other.getHealth();started=game.server().getTickCount();}
        void hit(net.minecraft.server.level.ServerPlayer victim,net.minecraft.server.level.ServerPlayer attacker){
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
            require(victim.hurtServer(f.level,victim.damageSources().playerAttack(attacker),1),"native external melee hit");}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-started;require(elapsed<40,"bounded scheduler");
            if(phase==0){if(elapsed<2)return false;
                require(charge(f.owner)==0&&charge(f.other)==9&&f.other.getHealth()==otherHp-6,"first discharge transfers nine and deals exactly six");
                require(f.owner.getHealth()==ownerHp,"no immediate return damage");
                if(elapsed<12)return false;
                hit(f.other,f.owner);ownerHp=f.owner.getHealth();otherHp=f.other.getHealth();phase=1;started=game.server().getTickCount();return false;}
            if(elapsed<2)return false;
            require(charge(f.owner)==8&&charge(f.other)==0&&f.owner.getHealth()==ownerHp-6&&f.other.getHealth()==otherHp,"next external hit releases the transferred charge once");
            return elapsed>=12;
        }
        public void close(){service.stop();f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var s=new Scene(game);try{s.begin();running=s;}catch(Throwable error){s.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var s=running;try{if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS static-charge-no-echo (native potion marker, actual external damage, two twelve-tick no-echo windows, transferred charge retained)");}catch(Throwable error){running=null;try{s.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL static-charge-no-echo",error);}}
}
