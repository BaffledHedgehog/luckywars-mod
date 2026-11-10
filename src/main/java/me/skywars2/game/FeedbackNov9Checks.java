package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native maximum-fall target admission, arbitrary TNT fuse names and actual terrain-selected airdrops. */
public final class FeedbackNov9Checks {
    private FeedbackNov9Checks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("November9 feedback: "+why);}
    private static Fixture running;
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture base;
        final Map<UUID,Double> fallStart=new HashMap<>();
        final Set<UUID> before=new HashSet<>();PrimedTnt tnt;int phase,began,when,frozenFuse;String frozenText;
        Fixture(GameController game){this.game=game;base=new WoolBucketChecks.Fixture(game,false);}
        void setup(){
            base.prepareParticipants();began=when=game.server().getTickCount();heavy();timerFormats();terrain();gravitas();
            require(((Map<?,?>)WoolBucketChecks.read(game.events(),"pendingAirdrops")).isEmpty(),"exclusive pending airdrop fixture");
            for(var e:base.level.getAllEntities())before.add(e.getUUID());
            game.events().stageTwo(8);
            tnt=new PrimedTnt(EntityTypes.TNT,base.level);tnt.setPos(base.owner.getEyePosition().add(0,20,0));tnt.setNoGravity(true);tnt.setFuse(20);
            require(base.level.addFreshEntity(tnt),"ordinary unmanaged TNT published");base.owned.add(tnt.getUUID());
        }
        void heavy(){
            game.config().vanillaSkills=true;game.config().ranked=true;game.players().get(base.owner.getUUID()).skill=2;
            var cow=new Cow(EntityTypes.COW,base.level);cow.setPos(base.owner.position().add(2,0,0));cow.setNoGravity(true);cow.setNoAi(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);
            var loot=new ItemEntity(base.level,base.owner.getX()+1,base.owner.getY(),base.owner.getZ(),new ItemStack(Items.DIAMOND,7));loot.setNoGravity(true);loot.setPickUpDelay(32767);
            require(base.level.addFreshEntity(cow)&&base.level.addFreshEntity(loot),"actual radial mob and vulnerable dropped-item witnesses");base.owned.add(cow.getUUID());base.owned.add(loot.getUUID());
            require(game.events().selectedSkill(base.owner)==2,"native fixture unlocks Heavyweight");
            base.owner.causeFallDamage(40,0,base.owner.damageSources().fall());
            require(!loot.isRemoved()&&loot.getItem().is(Items.DIAMOND)&&loot.getItem().getCount()==7,"maximum Heavyweight landing does not touch vulnerable dropped loot");
            require(Math.abs(cow.getHealth()-(1000-3999*.00915))<.001,"same native landing retains exact radial mob damage: health="+cow.getHealth()+", selected="+game.events().selectedSkill(base.owner));
            cow.discard();loot.discard();game.config().vanillaSkills=false;
        }
        void timerFormats(){
            var control=new PrimedTnt(EntityTypes.TNT,base.level);
            require(control.isCustomNameVisible()&&control.getCustomName().getString().equals("4"),"native default TNT starts with timer, no special tags needed");
            int[] fuses={2000000,Integer.MAX_VALUE,40,39,21,20,19,10,1,0,-1};
            String[] names={"100000","107374182","2","1","1","1","1.0","0.5","0.1","0.0","\u221e"};
            for(int i=0;i<fuses.length;i++){
                control.setFuse(fuses[i]);require(control.getFuse()==fuses[i]&&control.isCustomNameVisible()&&control.getCustomName().getString().equals(names[i]),"native arbitrary signed/int fuse label "+fuses[i]);
            }
            control.discard();
        }
        void gravitas(){
            game.config().lbcLevelups=true;game.config().ranked=false;
            var profile=game.players().get(base.owner.getUUID());Arrays.fill(profile.levelups,0);profile.levelups[0]=30;
            profile.lbcKit=profile.lbcSkill=profile.lbcChallengeKit=0;
            game.items().mana().award(base.owner,"lbc:true_advancements/bows/shulker");
            game.lbc().apply(base.owner,false);
            require(game.lbc().hasLevelup(base.owner,30),"actual resolved Gravitas is active during the airdrop event");
            var shot=EntityTypes.SNOWBALL.create(base.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(shot!=null,"native Gravitas control factory");shot.setOwner(base.owner);shot.setPos(base.owner.getEyePosition());
            require(base.level.addFreshEntity(shot)&&shot.isNoGravity(),"native owned projectile really receives Gravitas; chicken control is not testing a disabled perk");
            base.owned.add(shot.getUUID());shot.discard();
        }
        void terrain(){
            var center=base.owner.blockPosition();int x=center.getX()+3,z=center.getZ()+3;
            for(int y=base.level.getMinY();y<base.level.getMaxY();y++)base.set(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            require(game.events().airdropSurface(x,z)==null,"empty resident column cannot receive a void airdrop");
            var ground=new BlockPos(x,Math.min(base.level.getMaxY()-2,center.getY()+30),z);
            base.set(ground,Blocks.STONE.defaultBlockState());
            require(ground.equals(game.events().airdropSurface(x,z)),"actual current elevated island surface selected rather than fixed Y110");
            base.set(ground.above(),Blocks.WATER.defaultBlockState());require(game.events().airdropSurface(x,z)==null,"fluid-only top is not treated as solid landing terrain");
            base.set(ground.above(),Blocks.AIR.defaultBlockState());
            base.set(ground,Blocks.STONE_SLAB.defaultBlockState());require(ground.equals(game.events().airdropSurface(x,z)),"partial-height real support is eligible");
        }
        boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            require(now-began<120,"bounded native fuse/airdrop publication");
            for(var e:base.level.getAllEntities())if(!before.contains(e.getUUID())&&e.entityTags().contains("skywars2.airdrop"))base.owned.add(e.getUUID());
            if(phase==0){
                if(tnt.getFuse()==20)return false;
                require(!tnt.isRemoved()&&tnt.getFuse()<20&&tnt.getCustomName().getString().equals(LbcTntTimer.label(tnt.getFuse()).getString()),"actual native fuse tick publishes fractional timer");
                frozenFuse=tnt.getFuse();frozenText=tnt.getCustomName().getString();game.items().timeStop().begin(base.level,100);when=now;phase=1;return false;
            }
            if(phase==1){
                if(now-when<3)return false;
                require(tnt.getFuse()==frozenFuse&&tnt.getCustomName().getString().equals(frozenText),"TimeStop freezes the authoritative fuse and its displayed timer together");
                game.items().timeStop().end(base.level);phase=2;return false;
            }
            require(tnt.getFuse()<frozenFuse,"native fuse resumes after TimeStop");
            var chickens=new ArrayList<Chicken>();var carts=new ArrayList<MinecartChest>();
            for(var id:base.owned){var e=base.level.getEntity(id);if(e instanceof Chicken c&&c.entityTags().contains("skywars2.airdrop"))chickens.add(c);if(e instanceof MinecartChest c&&c.entityTags().contains("skywars2.airdrop"))carts.add(c);}
            if(chickens.size()<10)return false;
            require(chickens.size()==10&&carts.size()==10,"actual native stage-two event publishes ten complete airdrops");
            for(var chicken:chickens){
                var floor=game.events().airdropSurface(chicken.blockPosition().getX(),chicken.blockPosition().getZ());
                require(floor!=null&&chicken.getY()>floor.getY()+1&&!chicken.isNoAi()&&!chicken.isNoGravity(),"airdrop remains an ordinary chicken with native AI, gravity and travel");
                require(chicken.getPassengers().size()==1&&chicken.getPassengers().getFirst() instanceof MinecartChest,"source native mounted chest preserved");
            }
            if(fallStart.isEmpty()){for(var chicken:chickens)fallStart.put(chicken.getUUID(),chicken.getY());base.move(base.owner,chickens.getFirst().position().add(1,0,0));when=now;}
            if(now-when<25)return false;
            for(var chicken:chickens)require(chicken.getY()<fallStart.get(chicken.getUUID())-1,"actual native loaded carrier descends instead of merely having a gravity flag");
            return true;
        }
        public void close(){game.items().timeStop().end(base.level);base.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.setup();}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS feedback-nov9 (actual Heavyweight radial damage/dropped-loot exclusion; native arbitrary int fuse labels/fractional tick/TimeStop; empty, fluid and slab terrain controls; ten ordinary AI chickens actually descend with mounted chests beside an active Gravitas owner)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL feedback-nov9",error);}}
}
