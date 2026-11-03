package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Dedicated Nexus tools keep their physical actors separate from ordinary wand admission. */
public final class LbcNexusTools {
    static final String TAG="skywars2.nexus_tool";
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final Vec3[] NEIGHBORS={new Vec3(0,1,0),new Vec3(0,-1,0),new Vec3(0,0,1),new Vec3(0,0,-1),new Vec3(1,0,0),new Vec3(-1,0,0)};
    private static final double LOG_FAILURE=Math.log1p(-.002);
    static final class Actor {
        final Entity body;final LbcIndirectKills.Owner cause;final UUID rider;final Vec3 direction;
        int age,born;
        Actor(Entity body,LbcIndirectKills.Owner cause,UUID rider,Vec3 direction,int age,int born){this.body=body;this.cause=cause;this.rider=rider;this.direction=direction;this.age=age;this.born=born;}
        boolean road(){return rider!=null;}
    }
    private record Chunk(ServerLevel level,ChunkPos pos){}
    private record Image(UUID id,LbcIndirectKills.Owner cause,UUID rider,Vec3 direction,int age){}
    private final GameController game;
    private final Map<Entity,Actor> actors=new LinkedHashMap<>();
    private final Deque<Actor> cells=new ArrayDeque<>();
    private final Map<Chunk,CompletableFuture<?>> chunks=new HashMap<>();
    private final Set<Chunk> needed=new HashSet<>();
    private final Set<UUID> casting=new HashSet<>(),cooling=new HashSet<>();
    private int lastTick=Integer.MIN_VALUE;
    private boolean restoring;
    LbcNexusTools(GameController game){this.game=game;}
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:nexus_tools"),TICKET);}
    public static boolean matches(net.minecraft.world.item.ItemStack s){return LbcNexusToolItems.isRoadgun(s)||LbcNexusToolItems.isCorrupter(s);}
    List<Actor> snapshot(){return List.copyOf(actors.values());}
    int count(){return actors.size();}
    int loadingChunks(){return chunks.size();}
    private boolean nexus(ServerLevel level){return level.dimension().equals(LbcHedgehogDimensions.NEXUS);}
    private boolean paused(Entity entity){return entity.entityTags().contains("time_stopped")||game.items().timeStop().stopped((ServerLevel)entity.level());}
    private boolean admitted(ServerPlayer p){return game.playing()&&game.canUseGameplayItems(p)&&p.isAlive()&&!p.isRemoved()&&!game.isObserver(p)&&nexus(p.level())&&!paused(p)&&!game.items().wands().inputTimeStopped(p);}
    public boolean use(ServerPlayer p,InteractionHand hand){
        if(!admitted(p)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var held=p.getItemInHand(hand);var mana=game.items().mana();
        if(LbcNexusToolItems.isRoadgun(held)){
            if(mana.hudValue(p,"nexus_roadgun_cd")>0||actors.values().stream().anyMatch(a->p.getUUID().equals(a.rider)))return false;
            var body=new BlockDisplay(EntityTypes.BLOCK_DISPLAY,p.level());body.setPos(p.position());body.setPosRotInterpolationDuration(1);body.setTransformationInterpolationDuration(1);body.setYRot(p.getYRot());body.setXRot(p.getXRot());
            var actor=new Actor(body,game.items().indirectKills().wildCause(p),p.getUUID(),p.getLookAngle().normalize(),0,game.server().getTickCount());
            install(actor);if(!p.level().addFreshEntity(body)||!p.startRiding(body,true,true)){remove(actor);return false;}
            p.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,300,0,false,false),p);mana.score(p,"nexus_roadgun_cd").set(15);cooling.add(p.getUUID());return true;
        }
        if(!LbcNexusToolItems.isCorrupter(held)||mana.hudValue(p,"nexus_corrupter_cd")>0||!casting.add(p.getUUID()))return false;
        UUID id=p.getUUID();String epoch=game.items().epoch();ServerLevel level=p.level();var cause=game.items().indirectKills().wildCause(p);
        game.items().exasperato().searchNexusTool(p,p.getEyePosition(),p.getLookAngle(),200,(pos,state)->!state.is(AIRS),
                e->e!=p&&e instanceof LivingEntity&&e.isAlive()&&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validRoleTarget(e,false))
            .thenAccept(hit->{casting.remove(id);var owner=game.server().getPlayerList().getPlayer(id);
                if(hit==null||owner==null||owner.level()!=level||!admitted(owner)||!game.items().currentEpoch(epoch))return;
                cube(level,hit instanceof BlockHitResult block?Vec3.atCenterOf(block.getBlockPos()):hit.getLocation(),cause);mana.score(owner,"nexus_corrupter_cd").set(20);cooling.add(id);
            });return true;
    }
    private void install(Actor actor){
        actor.body.entityTags().add(TAG);actor.body.entityTags().add("nexus_rc");actor.body.entityTags().add(actor.road()?"roadgun":"sim_cube");
        actor.body.entityTags().add("rare");game.items().markCurrentItemEpoch(actor.body);actors.put(actor.body,actor);save(actor);if(!actor.road())cells.addLast(actor);
    }
    Actor cube(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(Vec3.atCenterOf(BlockPos.containing(point)));
        var actor=new Actor(marker,cause,null,Vec3.ZERO,0,game.server().getTickCount());install(actor);
        if(!level.addFreshEntity(marker)){remove(actor);return null;}return actor;
    }
    private static void tag(Entity entity,String key,String value){String prefix=TAG+"."+key+".";entity.entityTags().removeIf(t->t.startsWith(prefix));entity.entityTags().add(prefix+value);}
    private static String value(Entity entity,String key,String fallback){String prefix=TAG+"."+key+".";for(var t:entity.entityTags())if(t.startsWith(prefix))return t.substring(prefix.length());return fallback;}
    private void save(Actor a){tag(a.body,"age",Integer.toString(a.age));if(a.cause!=null){tag(a.body,"owner",a.cause.player().toString());tag(a.body,"order",Long.toString(a.cause.order()));}
        if(a.rider!=null){tag(a.body,"rider",a.rider.toString());tag(a.body,"direction",a.direction.x+","+a.direction.y+","+a.direction.z);}}
    void loaded(Entity body){
        if(restoring||actors.containsKey(body)||!body.entityTags().contains(TAG))return;
        try{var owner=value(body,"owner","");var cause=owner.isEmpty()?null:new LbcIndirectKills.Owner(UUID.fromString(owner),Long.parseLong(value(body,"order","0")),true,false);
            var rider=value(body,"rider","");Vec3 direction=Vec3.ZERO;if(!rider.isEmpty()){var xyz=value(body,"direction","0,0,1").split(",");direction=new Vec3(Double.parseDouble(xyz[0]),Double.parseDouble(xyz[1]),Double.parseDouble(xyz[2]));}
            var actor=new Actor(body,cause,rider.isEmpty()?null:UUID.fromString(rider),direction,Integer.parseInt(value(body,"age","0")),-1);actors.put(body,actor);if(!actor.road())cells.addLast(actor);
        }catch(RuntimeException invalid){body.discard();}
    }
    void unloaded(Entity body){actors.remove(body);}
    private void remove(Actor actor){actors.remove(actor.body);actor.body.discard();}
    private boolean resident(ServerLevel level,AABB bounds){
        if(bounds.maxY<level.getMinY()||bounds.minY>=level.getMaxY())return true;
        boolean ready=true;for(int x=(int)Math.floor(bounds.minX/16);x<=(int)Math.floor(bounds.maxX/16);x++)for(int z=(int)Math.floor(bounds.minZ/16);z<=(int)Math.floor(bounds.maxZ/16);z++){
            var key=new Chunk(level,new ChunkPos(x,z));needed.add(key);var f=chunks.computeIfAbsent(key,k->level.getChunkSource().addTicketAndLoadWithRadius(TICKET,k.pos(),0));
            if(!f.isDone()||level.getChunkSource().getChunkNow(x,z)==null)ready=false;
        }return ready;
    }
    private void block(ServerLevel level,BlockPos pos,Block value){if(!level.isOutsideBuildHeight(pos))LbcDropBlocks.setBlock(level,pos,value.defaultBlockState(),true);}
    /** Geometric skipping samples the exact independent .002 Bernoulli cells without visiting343 empty trials. */
    static int nextSparse(net.minecraft.util.RandomSource random){return (int)Math.floor(Math.log1p(-random.nextDouble())/LOG_FAILURE);}
    private void road(Actor a){
        var p=game.server().getPlayerList().getPlayer(a.rider);var level=(ServerLevel)a.body.level();
        if(p==null||p.isRemoved()||!p.isAlive()||p.level()!=level||p.getVehicle()!=a.body||p.isShiftKeyDown()){if(p!=null&&p.getVehicle()==a.body)p.stopRiding();remove(a);return;}
        if(paused(p))return;var from=a.body.position();var end=from.add(a.direction.scale(36));
        if(!resident(level,new AABB(from,end).inflate(5)))return;
        int age=++a.age;
        game.items().indirectKills().capturedWorldChanges(level,a.cause,()->{
            if(age<=36)for(int i=0;i<13;i++){var origin=BlockPos.containing(from.add(a.direction.scale(3*i))).offset(-4,-4,-4);
                for(int index=nextSparse(level.getRandom());index<343;index+=1+nextSparse(level.getRandom()))block(level,origin.offset(index%7,index/49,index/7%7),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE));}
            if(age==39){p.stopRiding();var origin=p.blockPosition().offset(-2,-2,-2);for(int x=0;x<5;x++)for(int z=0;z<5;z++)block(level,origin.offset(x,0,z),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE));
                level.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_3.value(),net.minecraft.sounds.SoundSource.MASTER,2.5f,.5f);}
        });
        if(age<=35){var next=from.add(a.direction.scale(15));a.body.teleportTo(next.x,next.y,next.z);}
        if(age>=39)remove(a);else save(a);
    }
    private void cell(Actor a){var level=(ServerLevel)a.body.level();var point=a.body.position();
        if(!resident(level,new AABB(point,point).inflate(2))) {cells.addLast(a);return;}
        game.items().indirectKills().capturedWorldChanges(level,a.cause,()->{
            block(level,a.body.blockPosition(),Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK));
            for(var delta:NEIGHBORS){var next=point.add(delta);if(!level.getBlockState(BlockPos.containing(next)).is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK))&&level.getRandom().nextFloat()<.5f)cube(level,next,a.cause);}
        });
        for(var e:level.getEntities((Entity)null,new AABB(point,point).inflate(1),e->!e.entityTags().contains("nexus_rc")&&!e.entityTags().contains("nexus_entity")&&e.position().distanceToSqr(point)<=1))
            if(!e.getType().builtInRegistryHolder().is(MOBS))e.kill(level);
        var hit=level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(1),e->e.isAlive()&&e.position().distanceToSqr(point)<=1&&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validRoleTarget(e,false));
        if(!hit.isEmpty())hit.getFirst().hurtServer(level,game.items().indirectKills().captureDamage(level.damageSources().outOfBorder(),a.cause),10);
        // Source chance clears the entire Nexus cohort, including other owners and freshly spawned cells.
        if(level.getRandom().nextFloat()<.0025f)clearCells(level);
        remove(a);
    }
    void clearCells(ServerLevel level){for(var other:List.copyOf(actors.values()))if(!other.road()&&other.body.level()==level)remove(other);cells.removeIf(a->a.body.level()==level);}
    public void inventoryChanged(ServerPlayer p){if(game.items().mana().hudValue(p,"nexus_roadgun_cd")>0||game.items().mana().hudValue(p,"nexus_corrupter_cd")>0)cooling.add(p.getUUID());}
    public int cooldown(ServerPlayer p){String key=LbcNexusToolItems.isRoadgun(p.getMainHandItem())||LbcNexusToolItems.isRoadgun(p.getOffhandItem())?"nexus_roadgun_cd":LbcNexusToolItems.isCorrupter(p.getMainHandItem())||LbcNexusToolItems.isCorrupter(p.getOffhandItem())?"nexus_corrupter_cd":null;return key==null?Integer.MIN_VALUE:game.items().mana().hudValue(p,key);}
    public void tick(){
        int tick=game.server().getTickCount();if(restoring||lastTick==tick)return;lastTick=tick;if(!game.playing()||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;needed.clear();
        for(var a:List.copyOf(actors.values())){if(a.body.isRemoved()||!game.items().currentEpoch(a.body)){remove(a);continue;}if(a.road()&&nexus((ServerLevel)a.body.level())&&!paused(a.body)&&a.born!=tick)road(a);}
        int count=cells.size(),work=0;long until=Math.max(System.nanoTime()+200_000,Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+2_000_000));
        while(count-->0&&!cells.isEmpty()&&work++<256&&System.nanoTime()<until){var a=cells.removeFirst();if(!actors.containsKey(a.body))continue;
            if(!nexus((ServerLevel)a.body.level())||paused(a.body)||a.born==tick){cells.addLast(a);continue;}cell(a);}
        for(var a:cells)if(actors.containsKey(a.body)){var p=a.body.blockPosition();for(int x=(p.getX()-2)>>4;x<=(p.getX()+2)>>4;x++)for(int z=(p.getZ()-2)>>4;z<=(p.getZ()+2)>>4;z++)needed.add(new Chunk((ServerLevel)a.body.level(),new ChunkPos(x,z)));}
        for(var it=chunks.keySet().iterator();it.hasNext();){var key=it.next();if(!needed.contains(key)){key.level().getChunkSource().removeTicketWithRadius(TICKET,key.pos(),0);it.remove();}}
        for(var it=cooling.iterator();it.hasNext();){var p=game.server().getPlayerList().getPlayer(it.next());if(p==null){it.remove();continue;}if(paused(p)||!nexus(p.level()))continue;
            var mana=game.items().mana();int phase=mana.hudValue(p,"nexus_tools_phase")+1;if(phase>=20){phase=0;for(String key:List.of("nexus_roadgun_cd","nexus_corrupter_cd")){int cd=mana.hudValue(p,key);if(cd>0)mana.score(p,key).set(cd-1);}}
            mana.score(p,"nexus_tools_phase").set(phase);if(mana.hudValue(p,"nexus_roadgun_cd")<=0&&mana.hudValue(p,"nexus_corrupter_cd")<=0)it.remove();}
    }
    private void clearDerived(){casting.clear();actors.clear();cells.clear();for(var key:chunks.keySet())key.level().getChunkSource().removeTicketWithRadius(TICKET,key.pos(),0);chunks.clear();needed.clear();cooling.clear();}
    public void stop(){for(var a:List.copyOf(actors.values()))remove(a);clearDerived();restoring=false;
        var board=game.server().getScoreboard();for(String name:List.of("nexus_roadgun_cd","nexus_corrupter_cd","nexus_tools_phase")){
            var objective=board.getObjective(name);if(objective==null)continue;
            for(var holder:List.copyOf(board.getTrackedPlayers()))if(board.getPlayerScoreInfo(holder,objective)!=null)board.getOrCreatePlayerScore(holder,objective).set(0);
        }
    }
    void prepareFrozen(){clearDerived();restoring=true;}
    void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();for(var p:game.server().getPlayerList().getPlayers())inventoryChanged(p);}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final List<Image> images=actors.values().stream().map(a->new Image(a.body.getUUID(),a.cause,a.rider,a.direction,a.age)).toList();
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){var missing=new HashSet<UUID>();var restored=new ArrayList<Actor>();
            for(var image:images){var body=lookup.apply(image.id());if(body==null)missing.add(image.id());else restored.add(new Actor(body,image.cause(),image.rider(),image.direction(),image.age(),-1));}
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{actors.clear();cells.clear();for(var a:restored){actors.put(a.body,a);if(!a.road())cells.addLast(a);}});
        }
    }
}
