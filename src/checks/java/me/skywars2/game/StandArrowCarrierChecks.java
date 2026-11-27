package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.entities.mobs.summons.LbcStand;
import me.skywars2.game.match.checkpoints.MatchRandomState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;

/** Real player impact must resolve once, without a second queued source transport roll. */
public final class StandArrowCarrierChecks {
    private StandArrowCarrierChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Stand arrow carrier: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();
            var initial=new java.util.HashSet<java.util.UUID>();for(var entity:f.level.getAllEntities())initial.add(entity.getUUID());
            var mana=game.items().mana();var score=mana.score(f.other,"effect_stand_arrow_hit");int previous=score.get();
            var random=MatchRandomState.capture(f.owner.getRandom());
            try{
                score.set(0);
                var ammo=f.leaf("stand_arrow");var arrow=new Arrow(f.level,f.owner,ammo.copy(),null);
                arrow.setPos(f.other.getEyePosition().add(0,0,-3));arrow.setDeltaMovement(0,0,5);arrow.setNoGravity(true);
                require(f.level.addFreshEntity(arrow),"actual Stand tipped arrow in native world");f.own(arrow);
                // Fixed native owner RNG selects a successful source roll; no damage callback is replaced.
                f.owner.getRandom().setSeed(0);arrow.tick();
                require(f.other.isAlive()&&f.other.getHealth()<1000&&f.other.entityTags().contains("have_stand"),
                        "real accepted player impact grants the Stand exactly once");
                require(score.get()==0&&!f.other.hasEffect(MobEffects.RAID_OMEN)&&!LbcStand.nativeArrowEffect(),
                        "native transport leaves no queued retry or effect context after arrow impact");
                long returned=f.level.getEntitiesOfClass(ItemEntity.class,f.other.getBoundingBox().inflate(3),
                        item->LbcStand.isArrow(item.getItem())).stream().mapToLong(item->item.getItem().getCount()).sum();
                require(returned==1,"accepted player impact returns one arrow");
                require(game.items().legacyEffects().acceptCarrier(f.other,new MobEffectInstance(MobEffects.RAID_OMEN,20,119),null)
                        &&score.get()==1,"raw source Stand marker schedules exactly one tick despite a long carrier duration");
                score.set(0);
            }finally{
                score.set(previous);random.restore(f.owner.getRandom());
                var fresh=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
                for(var entity:f.level.getAllEntities())if(!initial.contains(entity.getUUID())&&!(entity instanceof net.minecraft.server.level.ServerPlayer))fresh.add(entity);
                for(var entity:fresh){game.items().entityUnloaded(entity);entity.discard();}
            }
        }
        SkyWars2.LOGGER.info("[check] PASS stand-arrow-carrier (native player arrow hit once, no marker retry, raw marker one tick)");
    }
}
