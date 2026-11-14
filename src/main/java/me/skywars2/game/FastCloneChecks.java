package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Sixty-four real first-frame wall impacts, two separate aura holders, every physical shooter relation and no post-impact cloning. */
final class FastCloneChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Fast clone intake: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final List<Arrow> originals=new ArrayList<>();
        final List<ItemEntity> bombs=new ArrayList<>();Skeleton npc;
        final String prefix="fast_clone_qa."+UUID.randomUUID();int at,phase;long operations;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        void setup(){
            f.prepareParticipants();game.config().lbcSkills=true;game.config().ranked=true;
            for(var p:List.of(f.owner,f.other))game.players().get(p.getUUID()).lbcSkill=15;
            f.move(f.owner,f.home);f.move(f.other,f.home.add(0,0,24));
            npc=new Skeleton(EntityTypes.SKELETON,f.level);npc.setPos(f.home.add(0,8,0));npc.setNoAi(true);npc.setNoGravity(true);require(f.level.addFreshEntity(npc),"actual NPC shooter publication");f.owned.add(npc.getUUID());
            int serial=0;
            for(var holder:List.of(f.owner,f.other)){
                var center=BlockPos.containing(holder.position().add(0,3,2));
                for(int x=-5;x<=5;x++)for(int y=-1;y<=3;y++)f.set(center.offset(x,y,0),Blocks.STONE.defaultBlockState());
                for(int i=0;i<32;i++){
                    var arrow=new Arrow(EntityTypes.ARROW,f.level);arrow.setOwner(switch(i%4){case 0->f.owner;case 1->f.other;case 2->npc;default->null;});
                    arrow.setPos(holder.position().add((i%9-4)*.35,3,0));arrow.setNoGravity(true);arrow.setDeltaMovement(new Vec3(0,0,3));arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
                    arrow.entityTags().add(prefix);arrow.entityTags().add(prefix+"."+serial++);require(f.level.addFreshEntity(arrow),"real first-flight arrow publication");originals.add(arrow);f.owned.add(arrow.getUUID());
                }
            }
            require(originals.size()==64,"source burst exceeds former48-body budget");operations=game.items().cloneSkills().operationCount();at=game.server().getTickCount();
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-at;if(elapsed==0)return false;require(elapsed==1,"entire native source burst processed in its first physics frame");
            if(phase==1)return bombStep();
            var family=f.level.getEntities((Entity)null,new AABB(f.home,f.home).inflate(50),e->e.entityTags().contains(prefix));for(var e:family)f.owned.add(e.getUUID());
            require(family.size()==128,"both holders get all64 copies exactly once, not48 or one holder's32: "+family.size());
            require(game.items().cloneSkills().operationCount()-operations==64,"one successful native operation per original");
            for(int i=0;i<originals.size();i++){
                var original=originals.get(i);require(((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)original).skywars2$isInGround(),"original already reached its real block before the old end-of-tick scanner");
                String label=prefix+"."+i;var copies=family.stream().filter(e->e.entityTags().contains(label)).toList();require(copies.size()==2,"one exact copied family for source"+i);
                for(var body:copies){require(body.entityTags().contains("doubled"),"completed marker prevents any same-frame repeat");require(body instanceof Arrow arrow&&arrow.getOwner()==original.getOwner(),"self/other/NPC/unowned physical shooter remains unchanged");}
            }
            for(var holder:List.of(f.owner,f.other))for(int i=0;i<4;i++){
                var point=holder.position().add(i*.2,3,0);
                ItemEntity bomb=i%3==0?game.items().kfcRewards().spawnLooseBomb(npc,point,i+1):game.items().kfcRewards().launchRangedBomb(i%2==0?f.owner:f.other,point,new Vec3(.1,0,0),0);
                require(bomb!=null,"actual bow/NPC free mine producer");bomb.entityTags().add(prefix+".bomb");bomb.entityTags().add(prefix+".bomb."+bombs.size());bombs.add(bomb);f.owned.add(bomb.getUUID());
            }
            at=game.server().getTickCount();phase=1;return false;
        }
        boolean bombStep(){
            var family=f.level.getEntities((Entity)null,new AABB(f.home,f.home).inflate(50),e->e.entityTags().contains(prefix+".bomb"));for(var e:family)f.owned.add(e.getUUID());
            require(family.size()==16,"all8 free item-entity weapons are doubled before their first physics frame");
            Map<?,?> states;
            try{var field=LbcKfcRewards.class.getDeclaredField("bombs");field.setAccessible(true);states=(Map<?,?>)field.get(game.items().kfcRewards());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            for(int i=0;i<bombs.size();i++){
                var source=bombs.get(i);String tag=prefix+".bomb."+i;var copies=family.stream().filter(e->e!=source&&e.entityTags().contains(tag)).toList();require(copies.size()==1,"one native mine copy per source");var copy=copies.getFirst();
                require(copy.getClass()==source.getClass()&&copy.entityTags().contains("doubled")&&source.entityTags().contains("doubled"),"private native mine factory and completed markers");
                require(copy instanceof ItemEntity item&&net.minecraft.world.item.ItemStack.matches(item.getItem(),source.getItem()),"full authored payload components retained");
                require(states.get(copy)!=null&&states.get(copy)!=states.get(source)&&states.get(copy).equals(states.get(source)),"independent native mine entry keeps player/NPC author, variant and epoch");
            }
            return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{for(var e:f.level.getEntities((Entity)null,new AABB(f.home,f.home).inflate(50),body->body.entityTags().contains(prefix)||body.entityTags().contains(prefix+".bomb")))f.owned.add(e.getUUID());}finally{f.close();}}
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS fast-clones (all64 first-frame impacts/two holders/self/other/NPC/unowned authors, no48-body cap, plus8 actual bow/NPC item-entity mines with independent payload ownership)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL fast-clones",error);}}
}
