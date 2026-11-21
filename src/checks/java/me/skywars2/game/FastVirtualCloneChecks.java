package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Real queued flight ticks, two aura holders and bursts larger than the old global cloning budget. */
final class FastVirtualCloneChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Fast virtual clones: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final List<LbcWands.ElectroTrace> originals=new ArrayList<>();
        int at,phase=-1;long bodies;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        void skill(int value){for(var p:List.of(f.owner,f.other))game.players().get(p.getUUID()).lbcSkill=value;}
        void launch(int lifetime){
            require(game.items().wands().electroCount()==0,"exclusive flight queue");originals.clear();
            for(var holder:List.of(f.owner,f.other))for(int i=0;i<32;i++){
                var trace=new LbcWands.ElectroTrace(holder,holder.position().add((i%9-4)*.15,3,0),game.items().epoch(),lifetime);
                trace.yaw=0;trace.pitch=0;originals.add(trace);game.items().wands().higherElectro(trace);
            }
            require(game.items().wands().electroCount()==64,"actual accepted64-source burst");
            at=game.server().getTickCount();bodies=game.items().cloneSkills().bodyCount();
        }
        void setup(){
            f.prepareParticipants();game.config().lbcSkills=true;game.config().ranked=true;skill(0);
            f.move(f.owner,f.home.add(0,60,0));f.move(f.other,f.owner.position().add(0,0,24));
            require(!game.lbc().hasSkill(f.owner,15)&&!game.lbc().hasSkill(f.other,15),"control has no active doubling aura");
        }
        void copied(int age){
            var traces=game.items().wands().electroSnapshot();require(traces.size()==128,"all64 copied for both holders before their own flight step: "+traces.size());
            require(game.items().cloneSkills().bodyCount()-bodies==64,"exact64 new logical bodies");
            for(var original:originals)require(traces.contains(original)&&original.age==age&&original.cloneMask==1,"original retains its actual remaining lifetime and completed marker");
            var copies=traces.stream().filter(t->!originals.contains(t)).toList();
            require(copies.size()==64&&copies.stream().allMatch(t->t.age==age&&t.cloneMask==1),"fresh clones advance exactly once, without recasting or resetting age");
            for(var holder:List.of(f.owner,f.other))require(copies.stream().filter(t->t.owner==holder).count()==32,"original physical author retained independently of aura holder");
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();
            // Command dispatch can occur before this frame's item tick. Publish at the observed end boundary.
            if(phase==-1){launch(3);phase=0;return false;}
            int elapsed=game.server().getTickCount()-at;if(elapsed==0)return false;
            require(elapsed==1,"one actual server frame between observations");at=game.server().getTickCount();
            if(phase==0){
                var traces=game.items().wands().electroSnapshot();require(traces.size()==64&&traces.stream().allMatch(t->t.age==1&&t.cloneMask==0),"no aura means ordinary unmodified flight: count="+traces.size()+" ages="+traces.stream().map(t->t.age).distinct().toList());
                skill(15);phase=1;return false;
            }
            if(phase==1){copied(2);phase=2;return false;}
            if(phase==2){require(game.items().wands().electroCount()==0,"aged originals and copies expire together at native lifetime3");launch(2);phase=3;return false;}
            if(phase==3){copied(1);phase=4;return false;}
            if(phase==4){
                require(game.items().wands().electroCount()==0,"first-frame clones also expire with original lifetime2");
                require(game.items().stand().fistCount()==0,"exclusive ordinary Stand burst");
                for(var holder:List.of(f.owner,f.other)){
                    holder.setYRot(0);holder.setXRot(0);holder.entityTags().add("have_stand");holder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcStand.ORAORA));
                    require(game.items().stand().use(holder,InteractionHand.MAIN_HAND),"actual Oraora schedules new fists inside its later ability tick");
                }
                phase=5;return false;
            }
            if(phase==5){require(game.items().stand().fistCount()==0,"ordinary two-tick burst delay preserved");phase=6;return false;}
            var fists=f.level.getEntities((Entity)null,new AABB(f.owner.position(),f.other.position()).inflate(10),LbcStand::cloneRecognizes);
            require(game.items().stand().fistCount()==4&&fists.size()==4,"both newborn ability-driven fists doubled in their first movement frame");
            require(fists.stream().allMatch(e->e.entityTags().contains("doubled")),"manual carrier birth shares ordinary completed marker");return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{game.items().wands().stop();game.items().stand().stop();}finally{f.close();}}
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS fast-virtual-clones (two holders, all64 queued flights, real positive-age and first-frame duplication, preserved authors, exact original expiry and same-frame native Stand births)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL fast-virtual-clones",error);}}
}
