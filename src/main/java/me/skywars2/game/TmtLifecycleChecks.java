package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual native chunk IO and steady source cadence, not hand-stepped production clocks. */
public final class TmtLifecycleChecks {
    private static Scene running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT lifecycle: "+why);}
    public static void run(GameController game,boolean rocket){require(running==null,"exclusive lifecycle scene");var s=new Scene(game,rocket);running=s;try{s.begin();}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}}
    public static void tick(GameController game){var s=running;if(s==null)return;try{require(game==s.game,"same controller");if(!s.step())return;running=null;s.close();SkyWars2.LOGGER.info("[check] PASS {} (actual private input and native ticks; chunk-unload/reload/source continuation or800-active-tick source power lifetime/steady cadence; scoped restoration)",s.rocketCase?"tmt-rocket-unload":"troll-box-load");}catch(Throwable e){running=null;try{s.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL "+(s.rocketCase?"tmt-rocket-unload":"troll-box-load"),e);}}
    static final class Scene implements AutoCloseable {
        final GameController game;final boolean rocketCase;final WoolBucketChecks.Fixture f;final NativeMeleeChecks.NativeStats stats;
        final Set<UUID> original=new HashSet<>();final net.minecraft.stats.Stat<net.minecraft.resources.Identifier> totalDrop=net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DROP);final int oldDrop;
        TrollBoxChecks.WorldRandom rng;Arrow rocket,oldRocket;Entity firstPower;UUID rocketId;int phase,when,start,age,active,bornAt,firstAge,peak;Vec3 savedPosition;boolean closed,remoteWasForced,remoteTouched;
        final Vec3 remote=new Vec3(1544,384,1544);static final int REMOTE_X=96,REMOTE_Z=96;
        Scene(GameController game,boolean rocketCase){this.game=game;this.rocketCase=rocketCase;f=new WoolBucketChecks.Fixture(game,false);stats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcTmtItems.prototype(0).getItem(),LbcSteampunkRocket.TOKEN,LbcTrollBox.TOKEN));oldDrop=f.owner.getStats().getValue(totalDrop);f.level.getAllEntities().forEach(e->original.add(e.getUUID()));}
        void next(int value){phase=value;when=game.server().getTickCount();}
        ItemStack privateItem(String flag){var property=game.items().tmt().useProfiles().families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt(flag,0)==1).findFirst().orElseThrow();
            var stack=LbcTmtItems.prototype(0).copyWithCount(2);var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,List.of(property));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
        long seed(int result){for(long s=0;s<10000;s++)if(net.minecraft.util.RandomSource.create(s).nextInt(6)+1==result)return s;throw new IllegalStateException("source seed");}
        void begin(){
            f.prepareParticipants();start=game.server().getTickCount();require(game.items().steampunkRocket().count()==0&&game.items().trollBox().snapshot().isEmpty()&&game.items().stonekivi().count()==0,"exclusive native lifecycle owners/work");
            if(rocketCase){
                f.owner.setXRot(-90);var stack=privateItem("steampunk_rocket");f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==1,"actual private rocket input and sole debit");
                rocket=game.items().steampunkRocket().snapshot().getFirst();f.own(rocket);rocketId=rocket.getUUID();require(f.owner.getVehicle()==rocket,"actual native mount");next(0);
            }else{
                rng=new TrollBoxChecks.WorldRandom(f.level);f.move(f.owner,new Vec3(20,340,0));f.move(f.other,new Vec3(24,340,0));
                // Source coordinates are controlled only by the existing narrow RNG fixture.
                // One real global emitter reaches its legitimate steady forty actors; no fake actors.
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=f.level.getMinY();y<=f.level.getMaxY();y++)f.set(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
                var stack=privateItem("trollge");f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);f.owner.getRandom().setSeed(seed(3));
                require(f.owner.gameMode.useItem(f.owner,f.level,stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==1&&game.items().trollBox().view().powerfall(),"actual private source powerfall activation");next(10);
            }
        }
        int powerAge(Entity entity){var map=(Map<?,?>)WoolBucketChecks.read(game.items().trollBox(),"powers");var power=map.get(entity.getUUID());return power==null?-1:(Integer)WoolBucketChecks.read(power,"age");}
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-start<(rocketCase?600:1050),"bounded actual lifecycle ticks phase="+phase);return rocketCase?rocketStep():powerStep();}
        boolean rocketStep(){int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(elapsed<2)return false;age=game.items().steampunkRocket().age(rocket);require(age==2,"actual pre-unload native flight age");
                f.owner.stopRiding();remoteWasForced=f.level.getForceLoadedChunks().contains(net.minecraft.world.level.ChunkPos.pack(REMOTE_X,REMOTE_Z));require(!remoteWasForced,"exclusive disposable remote fixture chunk");
                remoteTouched=true;f.level.setChunkForced(REMOTE_X,REMOTE_Z,true);f.level.getChunk(REMOTE_X,REMOTE_Z);rocket.setPos(remote);rocket.setDeltaMovement(Vec3.ZERO);f.pause();savedPosition=rocket.position();
                f.level.setChunkForced(REMOTE_X,REMOTE_Z,false);oldRocket=rocket;next(1);return false;}
            if(phase==1){
                require(game.items().steampunkRocket().count()==1&&game.items().steampunkRocket().age(oldRocket)==age,"pending unload keeps accepted source and active age; count="+game.items().steampunkRocket().count()+" age="+game.items().steampunkRocket().age(oldRocket)+" reason="+oldRocket.getRemovalReason());
                if(f.level.getChunkSource().getChunkNow(REMOTE_X,REMOTE_Z)!=null||!oldRocket.isRemoved()){require(elapsed<400,"actual remote chunk unload completes");return false;}
                require(oldRocket.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK&&f.level.getEntity(rocketId)==null&&game.items().steampunkRocket().snapshot().isEmpty(),"actual native entity unload, not a synthetic owner callback");
                f.level.setChunkForced(REMOTE_X,REMOTE_Z,true);f.level.getChunk(REMOTE_X,REMOTE_Z);next(2);return false;
            }
            if(phase==2){var body=f.level.getEntity(rocketId);if(body==null){require(elapsed<100,"actual saved entity reload completes");return false;}
                require(body instanceof Arrow&&body!=oldRocket,"new native Arrow instance from saved chunk entity data");rocket=(Arrow)body;f.own(rocket);
                require(game.items().steampunkRocket().snapshot().contains(rocket)&&game.items().steampunkRocket().age(rocket)==age&&rocket.position().equals(savedPosition),"UUID source, active age and physical position rebound without resetting");
                require(rocket.getOwner()==f.owner&&game.items().spellSources().source(rocket)!=null,"true player and accepted spell source survive actual chunk IO");f.thaw();next(3);return false;
            }
            if(phase==3){if(!rocket.isRemoved()){require(elapsed<42,"native resumed rocket times out without a reset lifetime; age="+game.items().steampunkRocket().age(rocket)+" ticks="+rocket.tickCount+" frozen="+game.items().timeStop().frozen(rocket)+" stop="+game.items().timeStop().stopped(f.level)+" entityTicking="+f.level.isPositionEntityTicking(rocket.blockPosition())+" force="+f.level.getForceLoadedChunks().contains(net.minecraft.world.level.ChunkPos.pack(REMOTE_X,REMOTE_Z))+" tags="+rocket.entityTags());return false;}
                require(game.items().steampunkRocket().count()==0&&elapsed>=40-age-1&&elapsed<=40-age+1,"remaining native active lifetime used exactly once after reload");return true;}
            throw new IllegalStateException("rocket phase "+phase);
        }
        boolean powerStep(){int elapsed=game.server().getTickCount()-when;
            if(phase==10){var powers=game.items().trollBox().snapshot().stream().filter(e->e.entityTags().contains("movingpower")).toList();if(powers.isEmpty()){require(elapsed<22,"actual twenty-tick source emission");return false;}
                firstPower=powers.getFirst();firstAge=powerAge(firstPower);bornAt=active=0;peak=powers.size();next(11);return false;}
            if(phase==12){require(powerAge(firstPower)==age&&firstPower.position().equals(savedPosition)&&game.items().trollBox().view().powers()==peak,"TimeStop holds active lifetime, physics and emission under established load");if(elapsed<5)return false;f.thaw();next(11);return false;}
            if(phase==11){active++;var view=game.items().trollBox().view();peak=Math.max(peak,view.powers());
                if(active==100){age=powerAge(firstPower);savedPosition=firstPower.position();peak=view.powers();f.pause();next(12);return false;}
                if(!firstPower.isRemoved()){int actual=powerAge(firstPower);require(actual>=0&&firstPower.getY()==310-actual*.5,"actual source age/movement agreement");
                    require(active<800-firstAge+2,"first power exceeded800 active native ticks: active="+active+" age="+actual+" live="+view.powers()+" queued="+game.items().stonekivi().count());return false;}
                require(active>=800-firstAge-1&&active<=800-firstAge+1,"full800-active-tick lifetime, independent of the number of later powers; active="+active+" firstAge="+firstAge);
                require(peak>=39&&peak<=41,"real twenty-tick emission reaches steady forty powers without runaway lifetime growth: "+peak);return true;}
            throw new IllegalStateException("power phase "+phase);
        }
        public void close(){if(closed)return;closed=true;try{game.items().steampunkRocket().stop();game.items().trollBox().stop();if(f.stopped)f.thaw();
                var fresh=new ArrayList<Entity>();for(var e:f.level.getAllEntities())if(!(e instanceof net.minecraft.server.level.ServerPlayer)&&!original.contains(e.getUUID()))fresh.add(e);for(var e:fresh)f.discard(e);
                if(rng!=null)rng.close();stats.close();f.owner.getStats().setValue(f.owner,totalDrop,oldDrop);
            }finally{try{f.close();}finally{if(remoteTouched&&!remoteWasForced)f.level.setChunkForced(REMOTE_X,REMOTE_Z,false);}}}
    }
}
