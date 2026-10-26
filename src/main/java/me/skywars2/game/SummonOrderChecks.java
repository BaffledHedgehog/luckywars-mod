package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual free inputs, native target-goal persistence and real Guardian beams against nonplayers. */
public final class SummonOrderChecks {
    private SummonOrderChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Summon orders: "+why);}
    private static void reset(AllGrimoireChecks.Fixture f){f.base.game.items().syringe().stop();f.restoreWorld=true;f.reset();f.base.owner.getInventory().clearContent();f.beta.setPos(f.at.add(12,500,0));f.beta.setNoGravity(true);}
    private static Cow cow(AllGrimoireChecks.Fixture f,Vec3 at){var cow=new Cow(EntityTypes.COW,f.level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(at);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(f.level.addFreshEntity(cow),"native enemy");return cow;}
    private static void aim(AllGrimoireChecks.Fixture f,Vec3 at){var delta=at.subtract(f.base.owner.getEyePosition());f.base.owner.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));f.base.owner.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));}
    private static boolean fighter(Mob mob){return !(mob instanceof net.minecraft.world.entity.ambient.Bat)&&!(mob instanceof net.minecraft.world.entity.animal.equine.SkeletonHorse);}
    private static List<Item> items(){var result=new ArrayList<Item>();for(var kind:LbcSummonerItems.kinds())result.add(kind.item());result.addAll(List.of(LbcWands.SUMMONER_VEXES,LbcWands.PSYCHOVEX,LbcKfcRewards.PENIS_CULTUS));return result;}
    private static void order(AllGrimoireChecks.Fixture f,Item item,InteractionHand hand){var p=f.base.owner;p.getInventory().clearContent();var stack=new ItemStack(item);p.setItemInHand(hand,stack);p.setShiftKeyDown(true);f.base.mana.set(p,-10000);int count=f.base.game.items().summons().count();require(f.base.input(hand)&&stack.getCount()==1&&f.base.mana.get(p)==-10000&&f.base.game.items().summons().count()==count,"free native command, no mana admission/consumption/new summon "+item+"/"+hand);}
    private static void focus(AllGrimoireChecks.Fixture f){reset(f);var g=f.base.game;var service=g.items().summons();var orders=service.orders();
        for(var kind:LbcSummonerItems.kinds())require(service.castOrdinary(f.base.owner,kind),"source formation");
        for(String kind:List.of("vex","strangeness","chaos","luck","omnipresence","obsession","hypocrisy"))require(service.spawnPreset(f.base.owner,kind,f.at.add(1,1,0))!=null,"source helper "+kind);
        var troops=service.snapshot();troops.forEach(m->{m.setNoAi(true);m.setNoGravity(true);});
        var far=troops.stream().filter(SummonOrderChecks::fighter).findFirst().orElseThrow();far.setPos(f.at.add(28,1,0));f.base.owner.setPos(f.at.add(-8,0,0));require(far.distanceToSqr(f.base.owner)>900,"old30-block owner restriction would exclude this soldier");
        var ally=cow(f,f.at.add(-4,.5,6));service.track(f.base.owner,ally);
        var selected=cow(f,f.at.add(0,.5,12));var closer=cow(f,f.at.add(4,.5,3));var distant=cow(f,f.at.add(0,.5,18));aim(f,selected.getBoundingBox().getCenter());
        require(orders.lookedAt(f.base.owner)==selected,"actual enemy hitbox under crosshair");
        var foreign=service.spawnPreset(f.beta,"vex",f.at.add(3,2,0));require(foreign!=null,"other owner's helper");foreign.setNoAi(true);foreign.setTarget(closer);
        for(var item:items())for(var hand:InteractionHand.values()){
            order(f,item,hand);
            for(var mob:troops)if(fighter(mob))require(mob.getTarget()==selected&&orders.assigned(mob)==selected,"every owned combat family focuses same aimed enemy including Guardian/Ghast");
            require(foreign.getTarget()==closer&&orders.assigned(foreign)==null,"does not commandeer another player's summons");
            require(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(f.base.owner,f.level,hand,selected,null).consumesAction()
                    &&f.base.mana.get(f.base.owner)==-10000&&service.count()==troops.size()+1,"real entity-interaction order overrides mob UI without mana or summons "+hand);

        }
        require(orders.assigned(far)==selected,"command reaches owned actors beyond30 blocks");
        var eye=f.base.owner.getEyePosition();var direction=selected.getBoundingBox().getCenter().subtract(eye).normalize();var wall=BlockPos.containing(eye.add(direction.scale(5)));f.level.setBlock(wall,Blocks.STONE.defaultBlockState(),2|16|32);require(orders.lookedAt(f.base.owner)==null,"terrain occludes crosshair selection");f.level.setBlock(wall,Blocks.AIR.defaultBlockState(),2|16|32);
        selected.discard();require(orders.lookedAt(f.base.owner)==distant||orders.lookedAt(f.base.owner)==null,"removed target never remains a focus candidate");
        // The source ability's extra shots obey the same chosen target, not a nearer unrelated actor.
        selected=cow(f,f.at.add(0,.5,10));aim(f,selected.getBoundingBox().getCenter());order(f,LbcWands.PSYCHOVEX,InteractionHand.MAIN_HAND);
        var focused=selected;var luck=troops.stream().filter(m->m.entityTags().contains("summoned_cultist")&&service.attackTarget(m,f.base.owner,m.position(),200)==focused).findFirst().orElse(null);
        require(luck!=null&&service.attackTarget(luck,f.base.owner,luck.position(),200)==selected,"special cultist target helper honors focus");reset(f);
    }
    private static void spreading(AllGrimoireChecks.Fixture f){reset(f);var service=f.base.game.items().summons();require(service.castOrdinary(f.base.owner,LbcSummonerItems.kinds().getFirst()),"twenty soldiers");var troops=service.snapshot();troops.forEach(m->{m.setNoAi(true);m.setNoGravity(true);m.setPos(f.at.add(0,1,0));});
        var left=cow(f,f.at.add(-6,0,0));var right=cow(f,f.at.add(6,0,0));f.base.owner.setYRot(180);f.base.owner.setXRot(0);require(service.orders().lookedAt(f.base.owner)==null,"empty crosshair triggers spreading");order(f,LbcSummonerItems.kinds().getFirst().item(),InteractionHand.MAIN_HAND);
        long a=troops.stream().filter(m->m.getTarget()==left).count(),b=troops.stream().filter(m->m.getTarget()==right).count();require(a==10&&b==10,"equal nearby enemies split20 soldiers evenly: "+a+"/"+b);
        // Proximity is relative to each summon, rather than the player's position.
        for(int n=0;n<troops.size();n++)troops.get(n).setPos(f.at.add(n<10?-20:20,1,0));left.setPos(f.at.add(-22,0,0));right.setPos(f.at.add(22,0,0));order(f,LbcWands.SUMMONER_VEXES,InteractionHand.OFF_HAND);
        for(int n=0;n<troops.size();n++)require(troops.get(n).getTarget()==(n<10?left:right),"nearby enemy measured from each soldier");
        left.discard();for(int tick=0;tick<20;tick++)service.tick();require(troops.stream().allMatch(m->service.orders().assigned(m)==right),"dead targets automatically release and reassign");
        right.entityTags().add("strict_map_object");for(int tick=0;tick<20;tick++)service.tick();require(troops.stream().allMatch(m->service.orders().assigned(m)==null),"protected role is never retained as an enemy");reset(f);
    }
    private static void guardians(AllGrimoireChecks.Fixture f){reset(f);var service=f.base.game.items().summons();require(service.castOrdinary(f.base.owner,LbcSummonerItems.kinds().get(6)),"four native Guardians");
        var guardian=service.ordinarySnapshot("guardians").getFirst();var at=guardian.position();for(var other:service.snapshot())if(other!=guardian)other.setNoAi(true);
        var chosen=cow(f,at.add(0,0,12));aim(f,chosen.getBoundingBox().getCenter());order(f,LbcSummonerItems.kinds().get(6).item(),InteractionHand.MAIN_HAND);
        require(guardian.getTarget()==chosen,"Shift assigns nonplayer enemy to Guardian");
        f.beta.setPos(at.add(5,0,0));f.beta.getInventory().clearContent();f.beta.removeAllEffects();f.beta.setHealth(1000);float betaHp=f.beta.getHealth();
        for(int tick=0;tick<200;tick++)f.level.tickNonPassenger(guardian);
        require(chosen.getHealth()<1000&&f.beta.getHealth()==betaHp&&service.orders().assigned(guardian)==chosen,"real native beam damages Cow, retaining commanded focus despite closer native player candidate");
        chosen.discard();f.beta.setPos(f.at.add(12,500,0));var automatic=cow(f,at.add(0,0,10));for(int tick=0;tick<20;tick++)service.tick();
        for(int tick=0;tick<130;tick++)f.level.tickNonPassenger(guardian);
        require(automatic.getHealth()<1000&&service.orders().assigned(guardian)==automatic,"Guardian automatically acquires and attacks ordinary mob after previous target dies");
        var old=service.orders().assigned(guardian);guardian.entityTags().add("time_stopped");require(service.orders().assigned(guardian)==null,"frozen summons retain no active attack");guardian.entityTags().remove("time_stopped");require(service.orders().assigned(guardian)==old,"order resumes without discarding target");reset(f);
    }
    public static void run(GameController g){try(var f=new AllGrimoireChecks.Fixture(g)){focus(f);spreading(f);guardians(f);}SkyWars2.LOGGER.info("[check] PASS summon-orders");}
    private static AllGrimoireChecks.Fixture retained;private static List<Mob> retainedActors;
    public static void stopProbe(GameController g){require(retained==null,"exclusive stop fixture");var f=new AllGrimoireChecks.Fixture(g,true);try{reset(f);for(var k:LbcSummonerItems.kinds())g.items().summons().castOrdinary(f.base.owner,k);
        var target=cow(f,f.at.add(0,.5,12));aim(f,target.getBoundingBox().getCenter());order(f,LbcWands.SUMMONER_VEXES,InteractionHand.MAIN_HAND);require(g.items().summons().orders().count()==40,"all40 native combat actors have retained commands");
        f.base.owner.setShiftKeyDown(false);f.base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcSyringe.ITEM));g.items().syringe().refresh();require(g.items().syringe().count()==3,"three actual syringe buffs retained");retainedActors=g.items().summons().snapshot();retained=f;
    }catch(Throwable error){g.items().summons().stop();g.items().syringe().stop();f.close();throw error;}SkyWars2.LOGGER.info("[check] PASS summon-orders-stop-probe");}
    public static void cleanup(GameController g){require(retained!=null,"actual-stop fixture");try{require(g.items().summons().orders().count()==0&&g.items().summons().count()==0&&g.items().syringe().count()==0&&retainedActors.stream().allMatch(Entity::isRemoved),"actual stop clears all actor goals, commands, targets and buffs");}finally{retained.close();retained=null;retainedActors=null;}SkyWars2.LOGGER.info("[check] PASS summon-orders-cleanup");}
}
