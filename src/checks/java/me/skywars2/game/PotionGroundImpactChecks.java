package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class PotionGroundImpactChecks {
    private PotionGroundImpactChecks() { }
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Potion ground impact: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final Set<UUID> bombs=new HashSet<>();Cow target;Arrow arrow;BlockPos center;int phase,when;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        ItemStack ammo(int amp){var stack=new ItemStack(Items.TIPPED_ARROW);stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(16743168),List.of(new MobEffectInstance(MobEffects.RAID_OMEN,20,amp)),Optional.empty()));return stack;}
        void begin(){f.prepareParticipants();var at=f.home.add(0,50,0);f.move(f.owner,at.add(0,0,-10));f.move(f.other,at.add(20,0,0));
            require(game.items().babylonEffects().pendingCount()==0,"exclusive carrier fixture");
            for(int amp:new int[]{101,102,103,104,107,108,109,110,111,118,121,122,124,125}){
                var probe=new Arrow(f.level,f.owner,ammo(amp),null);require(LbcQuiver.custom(probe),"ordinary tipped-arrow ground marker admitted: "+amp);
            }
            target=new Cow(EntityTypes.COW,f.level);target.setPos(at);target.setNoAi(true);target.setNoGravity(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
            require(f.level.addFreshEntity(target),"real living potion carrier");f.owned.add(target.getUUID());
            target.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,20,104),f.owner);
            require(game.items().babylonEffects().queued(target,104)==1,"even old twenty-tick potion data queues only a single explosion pulse");when=game.server().getTickCount();
        }
        void ground(){
            game.items().babylonEffects().clear(target);f.discard(target);
            for(var id:List.copyOf(bombs)){var body=f.level.getEntity(id);if(body!=null)f.discard(body);}bombs.clear();
            center=BlockPos.containing(f.home.add(0,50,5));
            for(var pos:BlockPos.betweenClosed(center.offset(-7,-7,-7),center.offset(7,7,7)))f.remember(pos);
            for(var pos:BlockPos.betweenClosed(center.offset(-2,-1,-1),center.offset(2,1,1)))f.set(pos,Blocks.STONE.defaultBlockState());
            var potion=f.loot("lbc:unstable_reactive");var stack=new ItemStack(Items.TIPPED_ARROW);stack.set(DataComponents.POTION_CONTENTS,potion.get(DataComponents.POTION_CONTENTS));
            arrow=new Arrow(f.level,f.owner,stack,new ItemStack(Items.BOW));arrow.setPos(Vec3.atCenterOf(center).add(0,0,-3));arrow.setNoGravity(true);arrow.setDeltaMovement(0,0,2);
            require(f.level.addFreshEntity(arrow),"native potion arrow with exact source effect components");f.owned.add(arrow.getUUID());phase=1;when=game.server().getTickCount();
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-when;require(elapsed<25,"bounded single-pulse/native-impact scheduler");
            for(var body:f.level.getAllEntities())if(body instanceof PrimedTnt tnt&&game.items().babylonEffects().managedSecondary(tnt)&&bombs.add(tnt.getUUID())){
                f.owned.add(tnt.getUUID());require(tnt.getOwner()==f.owner&&game.items().currentEpoch(tnt),"single native TNT keeps real author and match epoch");if(phase==0)tnt.setFuse(20);
            }
            if(phase==0){if(elapsed<5)return false;require(bombs.size()==1&&game.items().babylonEffects().queued(target,104)==0,"one application yields exactly one TNT, not a duration-sized burst");ground();return false;}
            if(elapsed<7)return false;
            require(arrow.isRemoved()&&bombs.size()==1,"native in-ground arrow fires once and retires");
            boolean destroyed=false;for(var pos:BlockPos.betweenClosed(center.offset(-2,-1,-1),center.offset(2,1,1)))if(f.level.getBlockState(pos).isAir())destroyed=true;
            require(destroyed,"actual two-tick native TNT destroys impact terrain");
            SkyWars2.LOGGER.info("[check] PASS potion-ground-impact (single living-target pulse, fourteen source ground markers, actual native arrow collision, exactly one owned TNT and real terrain destruction)");return true;
        }
        public void close(){if(target!=null)game.items().babylonEffects().clear(target);f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.begin();}catch(Throwable e){running.close();running=null;throw e;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL potion-ground-impact",e);}}
}
