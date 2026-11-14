package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Restored classic area effect: actual native instant-effect ticks, exact four-block sphere and WILD targets. */
final class LiquidAbyssAreaChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Liquid Abyss area: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final java.lang.reflect.Field clock;final int oldPhase;final net.minecraft.world.scores.Objective math;final boolean hadLegacy;final int oldLegacy;
        net.minecraft.world.scores.PlayerTeam team,ownerTeam,otherTeam;
        Cow inside,vertical,outside,diagonal,protectedBody;Skeleton undead;int stage,at;boolean closed;
        Scene(GameController game){
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            try{clock=LbcInventoryHazards.class.getDeclaredField("phase");clock.setAccessible(true);oldPhase=clock.getInt(game.items().inventoryHazards());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            math=game.server().getScoreboard().getObjective("lbc.math");require(math!=null,"existing legacy mode objective");
            var score=game.server().getScoreboard().getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("legacy"),math);
            hadLegacy=score!=null;oldLegacy=score==null?0:score.value();
        }
        ItemStack liquid(){var result=new ItemStack(Items.REPEATING_COMMAND_BLOCK,3);var data=new net.minecraft.nbt.CompoundTag();data.putInt("liquidvoid",1);result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return result;}
        <T extends net.minecraft.world.entity.Mob>T body(T body,Vec3 offset){body.setPos(f.home.add(offset));body.setNoAi(true);body.setNoGravity(true);body.setPersistenceRequired();body.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);body.setHealth(100);require(f.level.addFreshEntity(body),"native receiver publication");f.owned.add(body.getUUID());return body;}
        void setup(){
            f.prepareParticipants();game.config().lbcSkills=true;game.config().ranked=false;
            game.server().getScoreboard().getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("legacy"),math).set(0);
            for(var p:List.of(f.owner,f.other)){game.players().get(p.getUUID()).lbcSkill=0;Arrays.fill(game.players().get(p.getUUID()).trinkets,0);}
            var board=game.server().getScoreboard();ownerTeam=f.owner.getTeam();otherTeam=f.other.getTeam();require(board.getPlayerTeam("liquid_area_qa")==null,"owned team name unused");team=board.addPlayerTeam("liquid_area_qa");team.setAllowFriendlyFire(false);board.addPlayerToTeam(f.owner.getScoreboardName(),team);board.addPlayerToTeam(f.other.getScoreboardName(),team);
            f.move(f.owner,f.home);f.move(f.other,f.home.add(3,0,0));
            inside=body(new Cow(EntityTypes.COW,f.level),new Vec3(0,0,3.5));vertical=body(new Cow(EntityTypes.COW,f.level),new Vec3(0,3.5,0));
            outside=body(new Cow(EntityTypes.COW,f.level),new Vec3(4.1,0,0));diagonal=body(new Cow(EntityTypes.COW,f.level),new Vec3(3,0,3));
            protectedBody=body(new Cow(EntityTypes.COW,f.level),new Vec3(0,0,-1));protectedBody.entityTags().add("strict_map_object");
            undead=body(new Skeleton(EntityTypes.SKELETON,f.level),new Vec3(0,0,-3));reset();f.owner.getInventory().setItem(9,liquid());pulse();at=game.server().getTickCount();
        }
        void reset(){for(var p:List.of(f.owner,f.other)){p.removeAllEffects();p.setHealth(1000);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.setAbsorptionAmount(0);}for(var body:List.of(inside,vertical,outside,diagonal,protectedBody,undead)){body.removeAllEffects();body.setHealth(100);body.setInvulnerableTime(0);body.damageCooldownTime=0;}}
        void pulse(){try{clock.setInt(game.items().inventoryHazards(),39);}catch(IllegalAccessException error){throw new IllegalStateException(error);}game.items().inventoryHazards().tick();}
        void hit(){
            require(f.owner.getHealth()==994&&f.other.getHealth()==994,"raw source6 pulse harms holder and same-team ally: holder="+f.owner.getHealth()+", ally="+f.other.getHealth()+", holderEffect="+f.owner.getEffect(MobEffects.INSTANT_DAMAGE)+", allyEffect="+f.other.getEffect(MobEffects.INSTANT_DAMAGE));
            require(inside.getHealth()==94&&vertical.getHealth()==94&&undead.getHealth()==94,"native magic harms living and undead receivers inside the sphere");
            require(outside.getHealth()==100&&diagonal.getHealth()==100&&protectedBody.getHealth()==100,"exact radius excludes intersecting outside hitbox, cube corner and protected service actor");
            var source=f.other.getLastDamageSource();require(source!=null&&source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)&&source.getEntity()==null&&source.getDirectEntity()==null,"WILD environmental magic has no invented player credit");
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-at;require(elapsed<15,"bounded normally scheduled native effect trial");if(elapsed==0)return false;
            if(stage==0){
                hit();require(f.owner.getInventory().getItem(9).getCount()==3,"area effect never consumes Liquid");
                reset();game.players().get(f.owner.getUUID()).lbcSkill=2;pulse();require(!f.owner.hasEffect(MobEffects.INSTANT_DAMAGE)&&!inside.hasEffect(MobEffects.INSTANT_DAMAGE),"protected backpack prevents emission");stage=1;return false;
            }
            if(stage==1){
                require(f.owner.getHealth()==1000&&inside.getHealth()==100,"protected inventory has no delayed area damage");reset();
                f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,liquid());pulse();stage=2;return false;
            }
            hit();require(f.owner.getMainHandItem().getCount()==3&&f.owner.getInventory().getItem(9).getCount()==3,"held stack bypasses the inventory-only guard; counts and duplicate probes do not multiply the source effect");return true;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            try{var board=game.server().getScoreboard();if(team!=null){board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(f.owner.getScoreboardName(),ownerTeam);if(otherTeam!=null)board.addPlayerToTeam(f.other.getScoreboardName(),otherTeam);}clock.setInt(game.items().inventoryHazards(),oldPhase);var name=net.minecraft.world.scores.ScoreHolder.forNameOnly("legacy");if(hadLegacy)board.getOrCreatePlayerScore(name,math).set(oldLegacy);else board.resetSinglePlayerScore(name,math);}catch(Throwable error){failure=error;}
            try{f.close();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw new IllegalStateException(failure);
        }
    }
    static void run(GameController game){require(running==null,"exclusive scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS liquid-abyss-area (actual source6 instant magic ticks, four-block sphere, holder/ally/undead, exclusions and protected-inventory/held/duplicate-probe behavior)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL liquid-abyss-area",error);}}
}
