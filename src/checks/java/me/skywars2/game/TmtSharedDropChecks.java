package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Genuine mixed dropped-item publication; no synthetic item/use events or manual production ticks. */
public final class TmtSharedDropChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT shared drops: "+why);}
    public static void run(GameController game){require(running==null,"exclusive scene");var s=new Scene(game);running=s;try{s.begin();}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{require(s.game==game,"same controller");if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS tmt-shared-drops (real inventory drop, mixed active Academy/helicopter/Crocodildo native publications/one debit/private siblings, separate owner queues excluded; token air/real landing/one-unit debit, ordinary source completion, TimeStop and author panel position)");}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL tmt-shared-drops",e);}}
    static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        final Set<UUID> initial=new HashSet<>();final Set<Long> forced=new HashSet<>();
        ItemEntity token;ItemStack expected;Vec3 firstAt,singleAt;int phase,when,start,pausedPending;boolean closed;
        LbcTmtAbilities.Ability single;net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcTmtItems.prototype(0).getItem(),LbcTmtItems.prototype(2).getItem()));f.level.getAllEntities().forEach(e->initial.add(e.getUUID()));}
        void force(Vec3 at){var p=BlockPos.containing(at);int x=p.getX()>>4,z=p.getZ()>>4;if(f.level.setChunkForced(x,z,true))forced.add(net.minecraft.world.level.ChunkPos.pack(x,z));f.level.getChunk(x,z);}
        void terrain(Vec3 at){var origin=BlockPos.containing(at).offset(-6,-1,-6);var size=template.getSize();
            for(int x=(origin.getX()-1)>>4;x<=(origin.getX()+size.getX())>>4;x++)for(int z=(origin.getZ()-1)>>4;z<=(origin.getZ()+size.getZ())>>4;z++)force(new Vec3(x*16,at.y,z*16));
            for(var p:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(size.getX(),size.getY()+2,size.getZ())))f.remember(p);
        }
        LbcTmtAbilities.Ability property(Item item){String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();return game.items().tmt().catalog().families().stream()
                .filter(row->row.events().contains(LbcTmtCatalog.Event.DROP)&&row.locations().contains(LbcTmtCatalog.Location.LOOSE_ITEM)).flatMap(row->row.variants().stream())
                .filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readString(LbcTmtAbilities.NATIVE,"").equals(id)).findFirst().orElseThrow(()->new IllegalStateException("Missing actual dropped capability "+id));}
        ItemEntity drop(int carrier,List<LbcTmtAbilities.Ability> properties,Vec3 at){
            var old=new HashSet<UUID>();f.level.getAllEntities().forEach(e->old.add(e.getUUID()));var stack=LbcTmtItems.prototype(carrier).copyWithCount(2);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
            LbcTmtAbilities.write(data,properties);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));expected=stack.copyWithCount(1);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);f.owner.drop(true);
            require(f.owner.getMainHandItem().isEmpty(),"actual selected whole-stack drop");var matches=new ArrayList<ItemEntity>();for(var e:f.level.getAllEntities())if(e instanceof ItemEntity i&&!old.contains(e.getUUID())&&LbcTmtDrops.matches(i.getItem()))matches.add(i);
            require(matches.size()==1,"one real dropped body");var item=matches.getFirst();f.own(item);item.setPos(at);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setOnGround(false);
            require(item.getItem().getCount()==2&&game.items().specialDrops().owner(item)==f.owner,"actual source author and exact stack admitted once");return item;
        }
        void next(int p){phase=p;when=game.server().getTickCount();}
        void begin(){
            f.prepareParticipants();require(game.items().specialDrops().pendingCount()==0&&game.items().academy().pendingCount()==0&&game.items().academy().ownedCount()==0&&game.items().crocodildo().bodies().isEmpty()&&game.items().helicopterEncounter().snapshot().isEmpty(),"exclusive native owners");
            for(var item:List.of(LbcCrocodildoItems.TOKEN,LbcHelicopterTriggerItem.TOKEN)){
                var fresh=new ItemEntity(f.level,f.home.x,f.home.y,f.home.z,item.getDefaultInstance());f.own(fresh);
                require(f.level.addFreshEntity(fresh)&&game.items().currentEpoch(fresh),"ordinary encounter ENTITY_LOAD stamps its actual epoch without manual fixture setup");f.discard(fresh);
            }
            template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:magic_academy")).orElseThrow();start=game.server().getTickCount();
            firstAt=f.home.add(0,18,0);singleAt=f.home.add(32,18,0);terrain(firstAt);terrain(singleAt);
            var academy=property(LbcNpcDropItems.ACADEMY);single=property(LbcAcademy.TOKEN);var helicopter=property(LbcHelicopterTriggerItem.TOKEN);var croc=property(LbcCrocodildoItems.TOKEN);
            token=drop(0,List.of(academy,single,helicopter,croc),firstAt);
            var inward=new Vec3(-firstAt.x,0,-firstAt.z).normalize();if(inward.lengthSqr()==0)inward=new Vec3(0,0,1);force(firstAt.add(0,80,0).subtract(inward.scale(150)));
            force(firstAt.add(Vec3.directionFromRotation(token.getXRot()-90,token.getYRot()).scale(20)).add(Vec3.directionFromRotation(token.getXRot(),token.getYRot()).scale(50)));
            require(!LbcTmtAbilities.invoke(token.getItem(),single.id(),()->game.items().academy().readyDrop(token)),"single-use Academy stays unready in air inside its own private scope");
            require(LbcTmtAbilities.invoke(token.getItem(),academy.id(),()->game.items().academy().readyDrop(token)),"active Academy is immediately ready at same airborne body");next(0);
        }
        void counts(){require(game.items().helicopterEncounter().snapshot().size()==1&&game.items().crocodildo().bodies().size()==1,"one native encounter from each admitted property, no competing standalone dispatch");}
        boolean step(){require(game.server().getTickCount()-start<250,"bounded actual scheduler scene");f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(token.getItem().getCount()==2){require(elapsed<8,"mixed native loose dispatch");return false;}require(!token.isRemoved()&&ItemStack.matches(token.getItem(),expected),"four private properties preserved and exactly one shared physical debit");counts();require(game.items().academy().pendingCount()==1,"only ready active Academy accepted, not airborne single-use sibling");require(!MatchAcademyCheckpoint.captureReady(game),"actual rewind barrier rejects partially placed Academy");pausedPending=game.items().academy().pendingCount();f.pause();next(1);return false;}
            if(phase==1){if(elapsed<3)return false;require(token.getItem().getCount()==1&&game.items().academy().pendingCount()==pausedPending,"TimeStop pauses remaining item and queued acquisition");counts();f.discard(token);f.thaw();next(2);return false;}
            if(phase==2){if(game.items().academy().pendingCount()>0){require(elapsed<25,"actual queued full Academy finishes");return false;}require(game.items().academy().ownedCount()>0&&MatchAcademyCheckpoint.captureReady(game),"actual full source Academy actors created and completed state ready for rewind");counts();token=drop(2,List.of(single),singleAt);next(3);return false;}
            if(phase==3){if(elapsed<3)return false;require(!token.isRemoved()&&token.getItem().getCount()==2&&game.items().academy().pendingCount()==0,"single-use TMT property does not trigger in air");var below=BlockPos.containing(singleAt).below();f.set(below,Blocks.STONE.defaultBlockState());token.setNoGravity(false);next(4);return false;}
            if(phase==4){if(token.getItem().getCount()==2){require(elapsed<25,"actual falling item reaches support");return false;}require(!token.isRemoved()&&ItemStack.matches(token.getItem(),expected),"native landing consumed one TMT unit, never whole token stack");require(game.items().academy().pendingCount()==1,"one real single-use placement accepted");f.pause();f.discard(token);next(5);return false;}
            if(phase==5){if(elapsed<2)return false;require(game.items().academy().pendingCount()==1,"single-use queue paused independently of discarded source body");f.thaw();next(6);return false;}
            if(phase==6){if(game.items().academy().pendingCount()>0){require(elapsed<40,"native single-use rows complete");return false;}
                var origin=BlockPos.containing(token.position()).offset(-6,-1,-6);var labels=new ArrayList<net.minecraft.world.entity.Display.TextDisplay>();for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())&&e instanceof net.minecraft.world.entity.Display.TextDisplay d&&d.entityTags().contains("skywars2.academy.single_panel"))labels.add(d);
                require(labels.stream().anyMatch(d->Math.abs(d.getY()-(origin.getY()+2.25))<.001),"authored single-use title placement is preserved");counts();return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{if(f.stopped)f.thaw();game.items().academy().stop();game.items().helicopterEncounter().stop();game.items().crocodildo().stop();
                var fresh=new ArrayList<Entity>();for(var e:f.level.getAllEntities())if(!(e instanceof net.minecraft.server.level.ServerPlayer)&&!initial.contains(e.getUUID()))fresh.add(e);for(var e:fresh)f.discard(e);stats.close();
            }finally{try{f.close();}finally{for(long c:forced)f.level.setChunkForced((int)c,(int)(c>>32),false);}}}
    }
}
