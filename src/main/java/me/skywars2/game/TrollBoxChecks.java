package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Genuine source/private inputs and native shared queues, bodies, worlds and rollback coordinator. */
public final class TrollBoxChecks {
    private TrollBoxChecks(){}
    private static Scene running;private static Boolean requested;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Troll Box: "+why);}
    private static void set(Object object,String name,Object value){for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);f.set(object,value);return;}catch(NoSuchFieldException ignored){}catch(IllegalAccessException e){throw new IllegalStateException(e);}throw new IllegalStateException(name);}
    /** Control only this source's lottery/coordinates; all unrelated native RNG calls remain native. */
    static final class WorldRandom implements AutoCloseable {
        final ServerLevel level;final Object saved;
        WorldRandom(ServerLevel level){this.level=level;saved=WoolBucketChecks.read(level,"random");set(level,"random",Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
            boolean own=StackWalker.getInstance().walk(frames->frames.filter(f->!f.getClassName().equals(WorldRandom.class.getName())&&!f.getClassName().startsWith("java.")&&!f.getClassName().startsWith("jdk.")).findFirst().map(f->f.getClassName().equals(LbcTrollBox.class.getName())).orElse(false));
            if(own&&method.getName().equals("nextFloat"))return 0f;
            if(own&&method.getName().equals("nextInt")&&args!=null&&args.length==1)return (Integer)args[0]==201?100:0;
            try{return method.invoke(saved,args);}catch(InvocationTargetException e){throw e.getCause();}
        }));}
        public void close(){set(level,"random",saved);}
    }
    public static void run(GameController game,boolean rewind){require(running==null&&requested==null,"exclusive native scene");requested=rewind;}
    public static void tick(GameController game){
        if(requested!=null){boolean rewind=requested;requested=null;var f=new Scene(game,rewind);running=f;try{f.begin();}catch(Throwable e){running=null;try{f.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL "+(rewind?"troll-box-rewind":"troll-box-native"),e);}return;}
        var f=running;if(f==null)return;try{f.ack();if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} (actual source/private inputs; coupled native events, terrain, mask beam and causal actors; scoped cleanup)",f.rewind?"troll-box-rewind":"troll-box-native");}catch(Throwable e){running=null;try{f.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL "+(f.rewind?"troll-box-rewind":"troll-box-native"),e);}
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;final boolean rewind;final WoolBucketChecks.Fixture f;final LbcTrollBox service;final WorldRandom rng;
        final Vec3 origin=new Vec3(8.5,220,8.5);final int started;final Set<UUID> initial=new HashSet<>();
        int phase,when,number=1,variant;boolean closed;Map<BlockPos,BlockState> savedTerrain,futureWalls;LbcTrollBox.View savedView;
        Map<UUID,Vec3> savedBodies;Map<UUID,Integer> savedAges;int savedCell;UUID powerId;LivingEntity witness;float witnessHealth;
        Scene(GameController game,boolean rewind){this.game=game;this.rewind=rewind;f=new WoolBucketChecks.Fixture(game,true);service=game.items().trollBox();rng=new WorldRandom(f.level);started=game.server().getTickCount();for(var e:f.level.getAllEntities())initial.add(e.getUUID());}
        void next(int n){phase=n;when=game.server().getTickCount();}
        void ack(){f.client.acknowledge();f.audience.acknowledge();}
        void room(){
            // Full authored25-template union and both source cages, including their cleared shaft.
            for(var at:BlockPos.betweenClosed(new BlockPos(-19,219,-19),new BlockPos(48,240,36)))f.remember(at);
            for(var p:List.of(f.owner,f.other)){var base=p.blockPosition();for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=base.getY();y<f.level.getMaxY();y++)f.remember(new BlockPos(base.getX()+x,y,base.getZ()+z));}
            for(var at:BlockPos.betweenClosed(new BlockPos(-4,304,-4),new BlockPos(4,314,4)))f.remember(at);
        }
        void begin(){f.prepareParticipants();f.move(f.owner,origin);f.move(f.other,origin.add(12,0,0));require(service.ready(),"all actual authored templates prepared from packaged resources");require(game.items().stonekivi().count()==0&&service.snapshot().isEmpty()&&game.items().cageDrops().snapshot().isEmpty(),"exclusive source queues/bodies");room();use(1,0);next(0);}
        ItemStack privateItem(){
            var a=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(p->LbcTmtComponents.nativeItem(p.data())==LbcTrollBox.TOKEN).findFirst().orElseThrow();
            var sibling=game.items().tmt().catalog().families().stream().flatMap(row->row.variants().stream()).filter(p->((ItemData.IntReader)(Object)p.data()).skywars2$readInt("zaika",0)==1).findFirst().orElseThrow();
            var stack=LbcTmtItems.prototype(0);var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();LbcTmtAbilities.write(data,List.of(a,sibling));data.putInt("trollge",0);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));stack.setCount(2);return stack;
        }
        long seed(int result){for(long s=0;s<10000;s++)if(RandomSource.create(s).nextInt(6)+1==result)return s;throw new IllegalStateException("lottery seed");}
        void use(int result,int kind){
            f.owner.stopUsingItem();var hand=kind==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;var stack=kind==0?new ItemStack(LbcTrollBox.TOKEN,2):privateItem();var copy=stack.copyWithCount(1);f.owner.setItemInHand(hand,stack);f.owner.getRandom().setSeed(seed(result));
            require(f.owner.gameMode.useItem(f.owner,f.owner.level(),stack,hand).consumesAction(),"real source/private native input outcome="+result+" variant="+kind);
            require(ItemStack.matches(stack,copy)&&!LbcTmtAbilities.active(stack),"one physical debit preserves private sibling and carrier");
            require(f.owner.getInventory().contains(s->LbcTrollBox.mask(s)==result),"actual authored mask reward "+result);
        }
        Map<UUID,Integer> ages(){var out=new LinkedHashMap<UUID,Integer>();var powers=(Map<?,?>)WoolBucketChecks.read(service,"powers");for(var e:powers.entrySet())out.put((UUID)e.getKey(),(Integer)WoolBucketChecks.read(e.getValue(),"age"));return out;}
        Map<UUID,Vec3> bodies(){var out=new LinkedHashMap<UUID,Vec3>();for(var e:service.snapshot())out.put(e.getUUID(),e.position());return out;}
        Map<BlockPos,BlockState> terrain(){var out=new LinkedHashMap<BlockPos,BlockState>();for(var at:f.terrain.keySet())out.put(at,f.level.getBlockState(at));return out;}
        Map<BlockPos,BlockState> walls(){var out=new LinkedHashMap<BlockPos,BlockState>();for(var at:f.terrain.keySet())if(at.getY()>=219&&at.getY()<=240)out.put(at,f.level.getBlockState(at));return out;}
        void cleanupEntities(){var remove=new ArrayList<Entity>();for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())&&!(e instanceof ServerPlayer))remove.add(e);for(var e:remove)f.discard(e);}
        void clear(){service.stop();if(f.stopped)f.thaw();f.owner.removeAllEffects();f.other.removeAllEffects();f.owner.getInventory().clearContent();f.other.getInventory().clearContent();cleanupEntities();f.terrain.forEach((at,state)->game.maps().restoreSnapshot(f.level,at,state,2|16|32));f.move(f.owner,origin);f.move(f.other,origin.add(12,0,0));}
        void nextOutcome(){clear();if(++variant==2){variant=0;number++;}if(number<=6)use(number,variant);next(10);}
        Entity power(){return service.snapshot().stream().filter(e->e.entityTags().contains("movingpower")).findFirst().orElse(null);}
        void hunter(){var body=service.snapshot().stream().filter(WitherSkeleton.class::isInstance).map(WitherSkeleton.class::cast).findFirst().orElseThrow();require(body.isSilent()&&body.getMaxHealth()==20&&body.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)==8&&body.hasEffect(MobEffects.INVISIBILITY)&&body.hasEffect(MobEffects.SPEED),"actual native hunter attributes/equipment/status");require(service.owner(body)==f.owner&&game.items().spellSources().source(body).author().equals(f.owner.getUUID()),"causal hunter owner");}
        void stones(){for(var at:BlockPos.betweenClosed(new BlockPos(-3,307,-3),new BlockPos(3,313,3)))f.set(at,Blocks.STONE.defaultBlockState());}
        boolean step(){
            require(!game.vanity().status().startsWith("FAILED"),"native coordinator failed: "+game.vanity().status());
            require(game.server().getTickCount()-started<2200,"bounded native scene phase="+phase+" result="+number+" variant="+variant+" state="+service.view()+" vanity="+game.vanity().status());int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(service.view().hunters()==0)return false;hunter();if(rewind){use(2,0);f.move(f.owner,origin);f.move(f.other,origin.add(12,0,0));use(6,0);use(3,1);stones();next(20);}else{number=2;variant=0;clear();use(2,0);next(10);}return false;}
            if(phase==10){
                if(number>6)return true;
                if(number==2){require(f.owner.level().dimension().equals(LbcHedgehogDimensions.IMPRINTED)&&f.other.level()==f.owner.level(),"darkness moves both actual participants");if(elapsed<101)return false;require(service.view().gravityLeft()<=0&&!f.owner.getAttribute(Attributes.GRAVITY).hasModifier(Identifier.withDefaultNamespace("imprinted")),"native five-second gravity cleanup");require(game.items().mana().hudValue(f.owner,LbcHedgehogDimensions.RETURN_COUNTER)>0,"general personal residence active");nextOutcome();return false;}
                if(number==3){if(power()==null)return false;require(service.owner(power())==f.owner,"source/private power causal author");
                    if(witness==null){var cow=EntityTypes.COW.create(f.level,EntitySpawnReason.COMMAND);cow.setPos(power().position());cow.setNoGravity(true);cow.setNoAi(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);f.own(cow);require(f.level.addFreshEntity(cow),"real falling-power status target");witness=cow;next(10);return false;}
                    if(elapsed<2)return false;require(ages().values().stream().anyMatch(v->v>0),"actual falling power native age/terrain frame");
                    for(var entry:Map.of(MobEffects.SLOWNESS,100,MobEffects.WEAKNESS,100,MobEffects.WITHER,5,MobEffects.BLINDNESS,2).entrySet())require(witness.hasEffect(entry.getKey())&&witness.getEffect(entry.getKey()).getAmplifier()==entry.getValue(),"native saturated-rate power WILD status "+entry+" actual="+witness.getEffect(entry.getKey()));witness=null;nextOutcome();return false;}
                if(number==4){if(service.view().placements()!=0)return false;require(f.level.getBlockState(BlockPos.containing(origin).below()).is(Blocks.STONE_BRICKS)&&game.items().cageDrops().snapshot().size()==4,"both real empty cages plus two authored anvils per victim");for(var e:game.items().cageDrops().snapshot())require(game.items().cageDrops().owner(e)==f.owner,"cage causal author");nextOutcome();return false;}
                if(number==5){if(service.view().placements()!=0)return false;long changed=walls().values().stream().filter(s->!s.isAir()).count();require(changed>10000,"full functional25-template walls, not a reward-only mask: "+changed);nextOutcome();return false;}
                if(number==6){require(service.view().abyss(),"real abyss event");f.move(f.other,new Vec3(origin.x,-1,origin.z));next(11);return false;}
            }
            if(phase==11){require(f.other.getY()>=320,"native abyss return from source block-zero column");f.move(f.other,origin.add(12,0,0));
                var cow=net.minecraft.world.entity.EntityTypes.COW.create(f.level,EntitySpawnReason.COMMAND);cow.setPos(origin.add(0,1,4));cow.setNoGravity(true);cow.setNoAi(true);f.own(cow);require(f.level.addFreshEntity(cow),"real mask beam target");witness=cow;witnessHealth=cow.getHealth();
                f.owner.setItemSlot(EquipmentSlot.HEAD,service.maskPrototype(6));f.owner.setYRot(0);f.owner.setXRot(0);f.set(BlockPos.containing(origin.add(0,1,6)),Blocks.STONE.defaultBlockState());next(12);return false;}
            if(phase==12){if(elapsed<2)return false;require(witness.getHealth()<witnessHealth,"worn mask6 invokes actual shared sampled beam");f.owner.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);nextOutcome();return false;}
            if(phase==20){if(power()==null)return false;powerId=power().getUUID();use(5,1);use(4,0);f.pause();next(21);return false;}
            if(phase==21){
                var frame=game.items().stonekivi().queuedSnapshot().stream().filter(w->w.getClass().getSimpleName().equals("PowerFrame")).findFirst().orElse(null);if(frame==null)return false;
                // Fault-injected between-step boundary, then the real coordinator/scheduler owns replay.
                var todo=(Collection<?>)WoolBucketChecks.read(frame,"todo");int initialCell=(Integer)WoolBucketChecks.read(todo.iterator().next(),"cell");
                if(initialCell==0){f.thaw();frame.step();f.pause();}savedCell=(Integer)WoolBucketChecks.read(todo.iterator().next(),"cell");require(savedCell>0&&savedCell<343,"actual partially accepted power cell batch: "+savedCell);
                savedTerrain=terrain();savedView=service.view();savedBodies=bodies();savedAges=ages();rng.close();require(savedView.placements()==4,"both native cage and wall jobs pending under TimeStop");require(game.vanity().requestCapture(f.owner,false),"whole coordinator accepts paused partial source queues");next(22);return false;
            }
            if(phase==22){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;require(terrain().equals(savedTerrain)&&bodies().equals(savedBodies)&&ages().equals(savedAges),"capture retains paused native cells/bodies/age");f.thaw();next(23);return false;}
            if(phase==23){if(service.view().placements()!=0||elapsed<80)return false;futureWalls=walls();require(!terrain().equals(savedTerrain)&&ages().get(powerId)>savedAges.get(powerId),"real future completes builders and active powers advance");f.pause();for(var e:service.snapshot())f.discard(e);require(game.vanity().requestRestoreForCheck(),"whole native terrain/body/queue rollback");next(24);return false;}
            if(phase==24){if(game.vanity().busy())return false;require(service.view().equals(savedView)&&bodies().equals(savedBodies)&&ages().equals(savedAges)&&terrain().equals(savedTerrain),"exact source clocks, canonical replacement bodies, queues and terrain restored");
                var frame=game.items().stonekivi().queuedSnapshot().stream().filter(w->w.getClass().getSimpleName().equals("PowerFrame")).findFirst().orElseThrow();var todo=(Collection<?>)WoolBucketChecks.read(frame,"todo");require((Integer)WoolBucketChecks.read(todo.iterator().next(),"cell")==savedCell,"partial cell cursor rebound to canonical restored power");
                require(game.items().timeStop().stopped(f.level),"native captured TimeStop restored");game.vanity().stop();f.thaw();next(25);return false;}
            if(phase==25){if(service.view().placements()!=0||elapsed<80)return false;require(walls().equals(futureWalls),"both restored cages and all25 seeded wall templates finish identically");require(ages().get(powerId)>savedAges.get(powerId),"restored canonical power resumes");hunter();return true;}
            return false;
        }
        public void close(){if(closed)return;closed=true;try{game.vanity().stop();clear();}finally{try{rng.close();}finally{f.close();}}}
    }
}
