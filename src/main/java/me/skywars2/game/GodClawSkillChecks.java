package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Scheduled gameplay checks: real sneak input, owned conversion and pause/replay, never fake tick loops. */
public final class GodClawSkillChecks {
    private GodClawSkillChecks(){}
    private static Run running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("God Claw: "+why);}
    public static void start(GameController game){
        require(running==null,"exclusive scheduled fixture");var r=new Run(game);
        try{r.init();running=r;}catch(RuntimeException|Error failure){r.close();throw failure;}
        SkyWars2.LOGGER.info("[check] START god-claw (registered native skill ticks)");
    }
    public static void tick(GameController game){
        if(running==null)return;var r=running;
        try{if(r.step()){running=null;r.close();SkyWars2.LOGGER.info("[check] PASS god-claw (actual60tick sneak/release, finite terrain and whole-stack conversion, exact native VoidMan owner, protected role, TimeStop/actual nonempty whole-coordinator replay, cooldown sneak freeze, loaded TECH control and skill cleanup)");}}
        catch(RuntimeException|Error failure){running=null;try{r.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL god-claw",failure);}
    }
    private static final class Run implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture f;final LbcGodClawSkill skill;
        final Set<Entity> summons=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<net.minecraft.world.entity.ai.attributes.AttributeInstance,Double> bases=new IdentityHashMap<>();
        ItemEntity input;Cow cow;Marker admin,jammer;UUID liquidId,adminId,chefId;RegenerationBeltChecks.ScheduledClient client,audience;int stage,at,pausedScore,pausedFrontier,cooldown;
        boolean closed;
        Run(GameController game){this.game=game;f=new ChefChecks.Fixture(game);skill=game.items().godClaw();}
        void init(){
            f.init();client=new RegenerationBeltChecks.ScheduledClient(game,f.owner);audience=new RegenerationBeltChecks.ScheduledClient(game,f.other);skill.clear(f.owner);voidBodies().forEach(summons::add);
            for(var attribute:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){
                var instance=f.owner.getAttribute(attribute);if(instance!=null)bases.put(instance,instance.getBaseValue());
            }
            // A finite connected strip. Its perimeter is already the exact consumed state, so no
            // runtime frontier can escape the fixture's snapshotted region into unrelated map terrain.
            for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)
                f.owner.level().setBlock(f.center.offset(x,-1,z),LbcGodClawSkill.CONSUMED,2);
            for(int x=0;x<=8;x++)f.block(f.center.offset(x,-1,0),Blocks.STONE);
            f.move(f.owner,f.at);f.move(f.other,f.at.add(12,0,12));
            game.config().lbcSkills=true;game.config().ranked=true;game.players().get(f.owner.getUUID()).lbcSkill=23;
            cow=f.cow(f.at.add(1,0,0));
            input=new ItemEntity(f.owner.level(),f.at.x,f.at.y,f.at.z,new ItemStack(Items.DIAMOND,64));
            input.setNoGravity(true);input.setDeltaMovement(Vec3.ZERO);input.setPickUpDelay(32767);require(f.owner.level().addFreshEntity(input),"actual full-stack entity");
            admin=new Marker(EntityTypes.MARKER,f.owner.level());admin.setPos(f.at);admin.entityTags().add("strict_map_object");require(f.owner.level().addFreshEntity(admin),"actual protected role actor");
            f.block(f.center.offset(-8,0,-8),LbcMagic.MECHANISM_BEACON_BLOCK);
            require(game.items().magic().mechanismSuppressed(f.owner)&&!game.items().magic().suppressed(f.owner),"loaded TECH positive control, MAG absent");
            f.owner.setShiftKeyDown(true);at=game.server().getTickCount();
        }
        List<net.minecraft.world.entity.monster.skeleton.Skeleton> voidBodies(){return f.owner.level().getEntitiesOfClass(net.minecraft.world.entity.monster.skeleton.Skeleton.class,f.owner.getBoundingBox().inflate(40),e->e.entityTags().contains("skywars2.godclaw.voidman"));}
        boolean step(){
            int elapsed=game.server().getTickCount()-at;
            if(game.vanity().busy())for(var player:game.server().getPlayerList().getPlayers())if(player.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            require(elapsed<1000,"bounded scheduled stage "+stage);
            if(stage==0){
                require(!skill.active(f.owner),"initial partial charge never activates");
                if(elapsed<20)return false;
                require(skill.score(f.owner)>=19&&skill.score(f.owner)<=21,"real initial sneak charging");
                f.owner.setShiftKeyDown(false);stage=1;at=game.server().getTickCount();return false;
            }
            if(stage==1){
                if(elapsed<2)return false;require(skill.score(f.owner)==0,"release resets incomplete charge");
                f.block(f.center.offset(-4,0,-4),LbcMagic.MAGIC_BEACON_BLOCK);require(game.items().magic().suppressed(f.owner),"actual MAG admission field");
                f.owner.setShiftKeyDown(true);stage=10;at=game.server().getTickCount();return false;
            }
            if(stage==10){
                require(skill.score(f.owner)==0&&!skill.active(f.owner),"MAG blocks charge and activation");if(elapsed<5)return false;
                f.block(f.center.offset(-4,0,-4),Blocks.AIR);require(!game.items().magic().suppressed(f.owner),"MAG removal unindexes admission field");stage=2;at=game.server().getTickCount();return false;
            }
            if(stage==2){
                if(!skill.active(f.owner)){require(elapsed<=61,"activation occurs at60 real sneak ticks");return false;}
                require(elapsed>=59&&elapsed<=61&&skill.score(f.owner)==60,"source60 threshold");
                require(skill.frontier(f.owner)>0,"nonempty native next generation");
                require(input.isRemoved()&&!cow.isAlive()&&!admin.isRemoved(),"whole input and mob consumed while protected role survives; item="+input.isRemoved()+", cow="+cow.isAlive()+", admin="+admin.isRemoved()+", itemAt="+input.position()+", owner="+f.owner.position());
                var created=voidBodies().stream().filter(e->!summons.contains(e)).toList();
                require(created.size()==1,"one exact native transformed VoidMan");
                var body=(net.minecraft.world.entity.monster.skeleton.Skeleton)created.getFirst();body.setNoAi(true);body.setNoGravity(true);
                require(body.getMaxHealth()==40&&body.getHealth()==40&&body.entityTags().contains("clawed_entity")
                        &&game.items().spellSources().owner(body)==f.owner&&game.items().spellSources().source(body).persistent(),"source native preset40HP with canonical ownership; hp="+body.getHealth()+", max="+body.getMaxHealth()+", immune="+body.entityTags().contains("clawed_entity")+", owner="+game.items().spellSources().owner(body)+", source="+game.items().spellSources().source(body));
                var outputs=f.owner.level().getEntitiesOfClass(ItemEntity.class,f.owner.getBoundingBox().inflate(3),e->ItemData.contains(e.getItem(),"liquidvoid"));
                require(outputs.size()==1&&outputs.getFirst().getItem().getCount()==1,"64 original items yield exactly one canonical Liquid Abyss");
                var liquid=outputs.getFirst();require(liquid.entityTags().contains("clawed_entity"),"source Liquid Abyss output has permanent contact immunity");liquid.setPickUpDelay(32767);liquid.setNoGravity(true);liquid.setDeltaMovement(Vec3.ZERO);liquidId=liquid.getUUID();adminId=admin.getUUID();
                var chefTarget=f.cow(f.at.add(10,0,0));var chef=game.items().wtf().spawn(f.other,chefTarget);require(chef!=null,"real native Chef for source transformation");chef.setPos(f.at.add(1,0,0));chefId=chef.getUUID();
                require(!game.items().cloneSkills().protectedFamilyActor(chef),"Chef is a real contact body");
                pausedScore=skill.score(f.owner);pausedFrontier=skill.frontier(f.owner);
                game.items().timeStop().begin(f.owner.level(),1000);stage=3;at=game.server().getTickCount();return false;
            }
            if(stage==3){
                require(skill.score(f.owner)==pausedScore&&skill.frontier(f.owner)==pausedFrontier,"TimeStop freezes score and accepted frontier");
                if(elapsed<5)return false;
                require(game.vanity().requestCapture(f.owner,false),"actual whole-coordinator capture with nonempty virtual frontier and native outputs");stage=8;at=game.server().getTickCount();return false;
            }
            if(stage==8){
                if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                skill.clear(f.owner);require(skill.frontier(f.owner)==0,"future frontier genuinely removed");
                require(game.vanity().requestRestoreForCheck(),"actual whole-coordinator restore request");stage=9;at=game.server().getTickCount();return false;
            }
            if(stage==9){
                if(game.vanity().busy())return false;
                require(skill.score(f.owner)==pausedScore&&skill.frontier(f.owner)==pausedFrontier,"whole coordinator restores exact nonempty frontier and clock");
                require(f.owner.level().getEntity(liquidId) instanceof ItemEntity,"whole coordinator restores canonical native output identity");
                admin=(Marker)f.owner.level().getEntity(adminId);require(admin!=null,"protected native role body remains after rewind");
                client.acknowledge();audience.acknowledge();game.vanity().stop();game.items().timeStop().end(f.owner.level());stage=4;at=game.server().getTickCount();return false;
            }
            if(stage==4){
                require(f.owner.level().getEntity(liquidId) instanceof ItemEntity item&&item.entityTags().contains("clawed_entity"),"source output retains identity through subsequent contact generations");
                require(!admin.isRemoved(),"protected admin survives all contact generations");
                if(skill.active(f.owner))return false;
                require(f.owner.level().getEntity(chefId)==null&&game.items().wtf().count()==0,"God Claw permanently removes Chef canonical family instead of knock");
                cooldown=skill.score(f.owner);require(cooldown>60&&cooldown<=90&&skill.frontier(f.owner)==0,"finite source end retains actual accumulated cooldown");
                stage=5;at=game.server().getTickCount();return false;
            }
            if(stage==5){
                require(skill.score(f.owner)==cooldown,"holding sneak freezes source cooldown");if(elapsed<5)return false;
                f.owner.setShiftKeyDown(false);stage=6;at=game.server().getTickCount();return false;
            }
            if(stage==6){
                if(skill.score(f.owner)==cooldown){require(elapsed<160,"stochastic outside-sneak cooldown eventually advances");return false;}
                require(skill.score(f.owner)<cooldown,"only outside-sneak cooldown debit");
                game.players().get(f.owner.getUUID()).lbcSkill=0;stage=7;at=game.server().getTickCount();return false;
            }
            if(elapsed<2)return false;
            require(skill.score(f.owner)==0&&skill.frontier(f.owner)==0,"skill loss retires all native bookkeeping");return true;
        }
        @Override public void close(){
            if(closed)return;closed=true;
            game.vanity().stop();skill.clear(f.owner);game.items().timeStop().end(f.owner.level());
            if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();}
            if(chefId!=null&&f.owner.level().getEntity(chefId)!=null)game.items().wtf().erase(f.owner.level().getEntity(chefId));
            if(admin!=null)admin.discard();if(input!=null)input.discard();
            for(var body:voidBodies())if(!summons.contains(body)){body.discard();game.items().spellSources().unloaded(body);}
            bases.forEach((instance,base)->instance.setBaseValue(base));if(client!=null)client.close();if(audience!=null)audience.close();f.close();
        }
    }
}
