package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.*;

/** The two placed legacy slabs. These are unrelated to the copper melee/Kara fan. */
public final class LbcLegacySlabs {
    public static final Item BLOOD=registerItem("blood_slab",Blocks.ACACIA_SLAB,Items.ACACIA_SLAB,"acacia_slab__c3f3f3e17aae",Kind.BLOOD);
    public static final Item CUM=registerItem("cum_slab",Blocks.BIRCH_SLAB,Items.BIRCH_SLAB,"birch_slab__b39738876363",Kind.DOWN);
    private static final String ACTOR="skywars2.legacy_slab";
    private static final TagKey<Block> BLOODED=TagKey.create(Registries.BLOCK,Identifier.parse("lbc:blood_slabbed"));
    private static final TagKey<EntityType<?>> IMMUNE=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("lbc:bloody_slabby"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    // Source 1..32 order, including the intentional second nether_bricks weight.
    private static final Block[] PALETTE={Blocks.AIR,Blocks.CAVE_AIR,Blocks.VOID_AIR,Blocks.NETHERRACK,Blocks.LAVA,Blocks.ACACIA_PLANKS,Blocks.FIRE,Blocks.NETHER_BRICKS,
            Blocks.WOOL.pick(DyeColor.RED),Blocks.RED_NETHER_BRICKS,Blocks.RED_SANDSTONE,Blocks.RED_SAND,Blocks.STAINED_GLASS.pick(DyeColor.RED),Blocks.REDSTONE_BLOCK,Blocks.TNT,Blocks.CONCRETE.pick(DyeColor.RED),
            Blocks.CONCRETE_POWDER.pick(DyeColor.RED),Blocks.GLAZED_TERRACOTTA.pick(DyeColor.RED),BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:red_terracotta")),Blocks.NETHER_BRICKS,Blocks.MAGMA_BLOCK,Blocks.NETHER_WART_BLOCK,Blocks.BASALT,Blocks.SMOOTH_BASALT,
            Blocks.POLISHED_BASALT,Blocks.SOUL_SAND,Blocks.SOUL_SOIL,Blocks.BONE_BLOCK,Blocks.BLACKSTONE,Blocks.NETHER_GOLD_ORE,Blocks.NETHER_QUARTZ_ORE,Blocks.FIRE_CORAL_BLOCK};
    private static final int[][] SIDES={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}},SIX={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,1,0},{0,-1,0}};
    private static final EntityType<?>[] CREATURES={EntityTypes.PIGLIN,EntityTypes.PIGLIN_BRUTE,EntityTypes.MAGMA_CUBE,EntityTypes.GHAST,EntityTypes.PIGLIN,EntityTypes.WITHER_SKELETON,EntityTypes.BLAZE,EntityTypes.HOGLIN,EntityTypes.ZOMBIFIED_PIGLIN};
    enum Kind { BLOOD, DOWN, CENTRAL }
    private final GameController game;
    private LinkedHashMap<UUID,Actor> actors=new LinkedHashMap<>();
    private Map<ServerLevel,Integer> sourceLevels=new IdentityHashMap<>();
    private Set<Ray> rays=Collections.newSetFromMap(new IdentityHashMap<>());
    private Frame pending;private int callbacks,lastTick=Integer.MIN_VALUE;private boolean restoring;
    private static final class Actor {
        Marker body;final Kind kind;final LbcSpellSource.Source source;
        Actor(Marker body,Kind kind,LbcSpellSource.Source source){this.body=body;this.kind=kind;this.source=source;}
    }
    public LbcLegacySlabs(GameController game){this.game=game;}
    public static void register(){}
    private static Item registerItem(String id,Block block,Item proxy,String leaf,Kind kind){
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),new Slab(block,proxy,kind,
                LbcUtilityItemPrototypes.properties("skywars2:"+id,"/data/lbc/loot_table/item_defs/minecraft/"+leaf+".json",proxy,64)));
    }
    /** Blood's advancement only checks its flag; white additionally requires the birch carrier. */
    public static Item canonical(ItemStack stack){
        if(stack.isEmpty())return null;
        var selected=LbcTmtAbilities.nativePredicatesActive()?LbcTmtAbilities.nativePredicateItem(stack):stack.getItem();
        // An unrelated aggregate outer flag may remain in the projected custom data.
        // The selected typed native source owns dispatch inside its callback.
        if(LbcTmtAbilities.nativePredicatesActive()){
            if(selected==BLOOD)return ItemData.intValue(stack,"blood_slab",0)==1?BLOOD:null;
            if(selected==CUM)return ItemData.intValue(stack,"cum_slab",0)==1?CUM:null;
        }
        if(ItemData.intValue(stack,"blood_slab",0)==1)return BLOOD;
        return ItemData.intValue(stack,"cum_slab",0)==1&&(selected==Items.BIRCH_SLAB||selected==CUM)?CUM:null;
    }
    public static ItemStack normalize(ItemStack stack){var target=canonical(stack);if(target==null||stack.is(target))return stack;var result=stack.transmuteCopy(target);result.applyComponents(stack.getComponents());return result;}
    public static InteractionResult legacyUseOn(UseOnContext context){var stack=context.getItemInHand();if(LbcTmtItems.matches(stack))return InteractionResult.PASS;var item=canonical(stack);return item!=null&&!stack.is(item)?item.useOn(context):InteractionResult.PASS;}
    private static final class Slab extends LbcVanillaBlockAliasItem {
        final Kind kind;
        Slab(Block block,Item proxy,Kind kind,Properties properties){super(block,properties,proxy);this.kind=kind;}
        @Override public InteractionResult place(BlockPlaceContext context){
            if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;
            var game=SkyWars2.game();if(game==null)return InteractionResult.FAIL;var owner=game.items().legacySlabs();
            if(canonical(context.getItemInHand())!=(kind==Kind.BLOOD?BLOOD:CUM)||!owner.allowed(player,kind))return InteractionResult.FAIL;
            // Geometry, replaceability, permission, waterlogging, statistics and sole hand debit stay vanilla.
            var source=owner.authority(player);boolean nativeAuthority=game.items().spellSources().current()==null;
            var origin=player.getEyePosition();var direction=player.getLookAngle();
            var result=super.place(context);if(result.consumesAction())owner.placed(player,kind,origin,direction,source,nativeAuthority);return result;
        }
    }
    private boolean allowed(ServerPlayer player,Kind kind){
        return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()&&player.isAlive()&&!player.isRemoved()
                &&!player.isSpectator()&&!game.isObserver(player)&&game.canUseGameplayItems(player)&&LbcDimensionItems.ordinary(player.level())
                &&LbcBedwarsRoleGuards.publication(player.level())&&!game.items().actionsBlocked(player)&&!game.items().wands().inputTimeStopped(player);
    }
    private LbcSpellSource.Source authority(ServerPlayer player){
        var current=game.items().spellSources().current();if(current!=null)return current;
        return new LbcSpellSource.Source(player.getUUID(),player.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(player),Set.of(),true);
    }
    private void placed(ServerPlayer player,Kind kind,Vec3 origin,Vec3 direction,LbcSpellSource.Source source,boolean nativeAuthority){
        callbacks++;try{game.items().exasperato().replayableSamples(player,origin,direction,999,new Ray(player.level(),kind,source,nativeAuthority));}finally{callbacks--;}
    }
    private final class Ray implements LbcExasperato.ReplayableRay {
        final ServerLevel level;final Kind kind;final LbcSpellSource.Source source;final boolean nativeAuthority;LbcExasperato.RayTicket ticket;
        Ray(ServerLevel level,Kind kind,LbcSpellSource.Source source,boolean nativeAuthority){this.level=level;this.kind=kind;this.source=source;this.nativeAuthority=nativeAuthority;}
        public double sampleStep(){return .025;}
        public boolean block(BlockPos at,BlockState state){return !state.isAir();}
        public boolean target(Entity entity){return false;}
        public void trail(Vec3 from,Vec3 to){}
        public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;rays.add(this);}
        public HitResult sampleAt(Vec3 point){
            var at=BlockPos.containing(point);
            if(restoring||!game.items().currentEpoch(source.epoch())||!level.isInValidBounds(at))return BlockHitResult.miss(point,Direction.UP,at);
            if(level.getBlockState(at).isAir())return null;
            callbacks++;try{add(level,at,kind,source,false,nativeAuthority);}finally{callbacks--;}
            return new BlockHitResult(point,Direction.UP,at,false);
        }
        public void complete(HitResult hit){rays.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){return resolved->()->{rays.add(this);};}
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}
    private boolean resident(ServerLevel level,BlockPos at){return level.isInValidBounds(at)&&level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)!=null;}
    private Actor add(ServerLevel level,BlockPos at,Kind kind,LbcSpellSource.Source source,boolean unique){
        return add(level,at,kind,source,unique,false);
    }
    private Actor add(ServerLevel level,BlockPos at,Kind kind,LbcSpellSource.Source source,boolean unique,boolean nativeAuthority){
        if(!resident(level,at)||!LbcBedwarsRoleGuards.canEmit(source,level))return null;
        if(unique&&!level.getEntitiesOfClass(Marker.class,new AABB(Vec3.atCenterOf(at),Vec3.atCenterOf(at)).inflate(.8),e->e.entityTags().contains(kind==Kind.BLOOD?"blood_slab":"cum_slab")&&e.position().distanceToSqr(Vec3.atCenterOf(at))<=.64).isEmpty())return null;
        var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(Vec3.atCenterOf(at));
        // WILD terrain/area hazards include the creator. Their first physical marker is the
        // emitter; an inherited explicit source keeps its original exclusions and authority.
        if(nativeAuthority)source=new LbcSpellSource.Source(marker.getUUID(),source.author(),source.epoch(),source.cause(),source.excluded(),source.persistent());
        marker.entityTags().addAll(List.of(ACTOR,"slowraycast","imba_rc",kind==Kind.BLOOD?"blood_slab":"cum_slab",source.epoch()));
        marker.entityTags().add(kind==Kind.BLOOD?"magic":kind==Kind.DOWN?"down":"central");
        var actor=new Actor(marker,kind,source);index(actor);game.items().spellSources().bind(marker,source);game.items().timeStop().prepareFrozen(marker);
        if(!level.addFreshEntity(marker)){remove(actor);return null;}return actor;
    }
    /** A single shared fair-terrain frame owns the admitted selector snapshot; children wait for the next frame. */
    public void tick(){
        if(restoring||!nativeRunning()||lastTick==game.server().getTickCount())return;lastTick=game.server().getTickCount();
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        // Consult the small world index before copying any producer set or drawing a frame seed.
        if(!sourcesRunning(sourceLevels.keySet()))return;
        if(pending==null&&!actors.isEmpty()){pending=new Frame(List.copyOf(actors.values()));game.items().stonekivi().queuedWork(pending);}
    }
    private boolean nativeRunning(){return !game.server().tickRateManager().isFrozen()&&game.server().tickRateManager().runsNormally();}
    private boolean sourcesRunning(Collection<ServerLevel> levels){for(var level:levels)if(!game.items().timeStop().stopped(level))return true;return false;}
    private record Move(Actor actor,boolean accelerate){}
    private final class Frame implements LbcStonekivi.Work {
        ArrayDeque<Move> todo=new ArrayDeque<>();final RandomSource random;final Set<ServerLevel> levels=new HashSet<>();boolean closed;
        Frame(List<Actor> active){random=RandomSource.create(active.getFirst().body.getRandom().nextLong());for(var actor:active){todo.add(new Move(actor,true));levels.add((ServerLevel)actor.body.level());}}
        public boolean ready(){return !restoring&&(closed||nativeRunning()&&sourcesRunning(levels));}
        public boolean snapshotIdle(){return true;}
        public boolean step(){
            if(!ready())return false;callbacks++;game.items().stonekivi().beginNativeWork();try{
                if(closed)return true;long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);int paused=0;
                for(int i=0;i<16&&!todo.isEmpty()&&System.nanoTime()<deadline;i++){
                    var move=todo.removeFirst();var actor=move.actor;var marker=actor.body;var level=(ServerLevel)marker.level();
                    if(!game.playing()||!game.items().currentEpoch(actor.source.epoch())){remove(actor);continue;}
                    if(marker.isRemoved())continue;
                    if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(marker)){
                        // Retain this exact move and its phase; no next-frame reseed on thaw.
                        todo.addLast(move);if(++paused>=todo.size())break;continue;
                    }
                    paused=0;
                    if(!resident(level,marker.blockPosition()))continue;
                    if(actor.kind==Kind.BLOOD&&game.items().magic().suppressedAt(level,marker.position())){remove(actor);continue;}
                    if(actor.kind==Kind.BLOOD&&move.accelerate){
                        todo.addFirst(new Move(actor,false));
                        var score=game.server().getScoreboard().getObjective("team_number");
                        var mine=score==null?null:game.server().getScoreboard().getPlayerScoreInfo(marker,score);
                        for(var other:level.getEntitiesOfClass(Marker.class,marker.getBoundingBox().inflate(16),e->e!=marker&&e.entityTags().contains("blood_slab")&&e.distanceToSqr(marker)>=.01&&e.distanceToSqr(marker)<=256)){
                            var theirs=score==null?null:game.server().getScoreboard().getPlayerScoreInfo(other,score);
                            var found=actors.get(other.getUUID());if(found!=null&&(mine==null||theirs==null||mine.value()!=theirs.value())&&random.nextFloat()<.3f)todo.addFirst(new Move(found,false));
                        }continue;
                    }
                    try(var scope=game.items().spellSources().scope(actor.source,level)){if(actor.kind==Kind.BLOOD)blood(actor,random);else white(actor);}
                    remove(actor);
                }
                if(todo.isEmpty()){closed=true;if(pending==this)pending=null;}return closed;
            }finally{game.items().stonekivi().endNativeWork();callbacks--;}
        }
        public void cancel(){closed=true;todo.clear();if(pending==this)pending=null;}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var saved=List.copyOf(todo);var rng=MatchRandomState.capture(random);boolean done=closed;
            return links->{checkpointBoundary();return ()->{todo=new ArrayDeque<>(saved);rng.restore(random);closed=done;};};
        }
    }
    private boolean write(Actor actor,BlockPos at,BlockState state){
        var level=(ServerLevel)actor.body.level();if(!resident(level,at)||level.getBlockState(at).equals(state))return false;
        return game.items().writeOwnedTerrain(level,at,state,actor.source.cause());
    }
    private void white(Actor actor){
        var level=(ServerLevel)actor.body.level();var at=actor.body.blockPosition();
        boolean changed=write(actor,at,Blocks.BIRCH_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,actor.kind==Kind.DOWN?SlabType.DOUBLE:SlabType.BOTTOM));
        if(actor.kind==Kind.DOWN&&!changed)return;
        if(!resident(level,at.below()))return;
        if(level.getBlockState(at.below()).isAir()){add(level,at.below(),Kind.DOWN,actor.source,true);return;}
        for(var d:SIDES){var side=at.offset(d[0],0,d[2]);if(resident(level,side)&&level.getBlockState(side).isAir())add(level,side,Kind.CENTRAL,actor.source,true);}
        // afterdown unconditionally places all four double slabs, even on occupied cells.
        if(actor.kind==Kind.DOWN)for(var d:SIDES)write(actor,at.offset(d[0],0,d[2]),Blocks.BIRCH_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE));
    }
    private void blood(Actor actor,RandomSource random){
        var level=(ServerLevel)actor.body.level();var at=actor.body.blockPosition();
        for(var d:SIX){var next=at.offset(d[0],d[1],d[2]);if(resident(level,next)&&!level.getBlockState(next).is(BLOODED))add(level,next,Kind.BLOOD,actor.source,true);}
        write(actor,at,PALETTE[random.nextInt(32)].defaultBlockState());
        if(random.nextFloat()>=.05f)return;
        for(int y=1;y<=5;y++)if(!resident(level,at.above(y))||!level.getBlockState(at.above(y)).is(Blocks.AIR))return;
        int roll=random.nextInt(10);
        if(roll==9){
            // Reuse the actual Mom family owner, equipment loot, native AI and lifecycle.
            var token=new ItemEntity(level,at.getX()+.5,at.getY()+2.5,at.getZ()+.5,LbcFamilyDropItems.Kind.MOM.item().getDefaultInstance());
            game.items().markCurrentItemEpoch(token);game.items().spellSources().bind(token,actor.source);game.items().familyDrops().spawn(token);game.items().spellSources().unloaded(token);token.discard();
        }else{
            var mob=CREATURES[roll].create(level,EntitySpawnReason.COMMAND);
            if(mob!=null){mob.setPos(Vec3.atCenterOf(at).add(0,roll==3?6:roll==2?3:2,0));
                if(mob instanceof Mob living)living.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),EntitySpawnReason.COMMAND,null);
                game.items().markCurrentItemEpoch(mob);game.items().spellSources().bind(mob,actor.source);game.items().timeStop().prepareFrozen(mob);if(!level.addFreshEntity(mob))mob.discard();}
        }
        var origin=Vec3.atCenterOf(at);LbcOverload.sound(level,origin,"minecraft:entity.generic.explode",1.1f,1.5f,0,0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,origin.x,origin.y+1.5,origin.z,30,.2,2,.2,.5);
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),origin);
        damage=game.items().indirectKills().captureDamage(damage,actor.source.cause());
        for(var victim:level.getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(4),e->e.position().distanceToSqr(origin)<=16&&e.getType().builtInRegistryHolder().is(MOBS)&&!e.getType().builtInRegistryHolder().is(IMMUNE)&&game.items().spellSources().target(actor.source,e)))victim.hurtServer(level,damage,20);
        for(int x:new int[]{1,-1})for(int y:new int[]{1,-1})for(int z:new int[]{1,-1})add(level,at.offset(x,y,z),Kind.BLOOD,actor.source,true);
    }
    private void index(Actor actor){actors.put(actor.body.getUUID(),actor);sourceLevels.merge((ServerLevel)actor.body.level(),1,Integer::sum);}
    private void unindex(Actor actor){if(actors.remove(actor.body.getUUID(),actor))sourceLevels.computeIfPresent((ServerLevel)actor.body.level(),(level,count)->count==1?null:count-1);}
    private void remove(Actor actor){unindex(actor);if(!actor.body.isRemoved())actor.body.discard();}
    public int count(){return actors.size();}
    public List<Entity> snapshot(){return actors.values().stream().map(a->(Entity)a.body).toList();}
    LivingEntity owner(Entity entity){var actor=entity==null?null:actors.get(entity.getUUID());return actor==null||actor.source.author()==null?null:game.server().getPlayerList().getPlayer(actor.source.author());}
    LbcIndirectKills.Owner cause(Entity entity){var actor=entity==null?null:actors.get(entity.getUUID());return actor==null?null:actor.source.cause();}
    record View(UUID body,Kind kind,Vec3 position,LbcSpellSource.Source source){}
    List<View> views(){return actors.values().stream().map(a->new View(a.body.getUUID(),a.kind,a.body.position(),a.source)).toList();}
    static List<Block> palette(){return List.of(PALETTE);}
    public void loaded(Entity entity){
        if(restoring||MatchActorCheckpoint.restoring(entity)||!managed(entity))return;
        var actor=actors.get(entity.getUUID());if(actor==null||!(entity instanceof Marker marker)||!game.items().currentEpoch(actor.source.epoch())){entity.discard();return;}
        actor.body=marker;game.items().spellSources().bind(marker,actor.source);game.items().timeStop().prepareFrozen(marker);
    }
    public void unloaded(Entity entity){}
    public void removed(Entity entity){if(restoring||MatchActorCheckpoint.restoring(entity))return;var reason=entity.getRemovalReason();if(reason!=null&&reason.shouldDestroy()){var actor=actors.get(entity.getUUID());if(actor!=null)unindex(actor);}}
    boolean cloneRecognizes(Entity entity){return actors.containsKey(entity.getUUID());}
    LbcCloneSkills.ActorImage cloneImage(Entity entity){
        var actor=actors.get(entity.getUUID());if(actor==null)return null;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){index(new Actor((Marker)clone,actor.kind,actor.source));game.items().spellSources().bind(clone,actor.source);}
            public void rollback(Entity clone){var found=actors.get(clone.getUUID());if(found!=null)remove(found);}
        };
    }
    public void stop(){checkpointBoundary();if(pending!=null)pending.cancel();for(var ray:List.copyOf(rays))if(ray.ticket!=null)ray.ticket.cancel();rays.clear();for(var actor:List.copyOf(actors.values()))remove(actor);lastTick=Integer.MIN_VALUE;}
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish native legacy slab quantum before checkpoint");}
    public void prepareFrozen(){checkpointBoundary();restoring=true;actors=new LinkedHashMap<>();sourceLevels=new IdentityHashMap<>();rays=Collections.newSetFromMap(new IdentityHashMap<>());pending=null;}
    public void publishFrozen(){checkpointBoundary();restoring=false;lastTick=game.server().getTickCount();}
    public void cancelFrozen(){checkpointBoundary();restoring=false;}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    final class Checkpoint {
        private record Image(Actor actor,MatchDamageSources.Ref body){}
        final List<Image> images;final Set<Ray> accepted=Set.copyOf(rays);final Frame work=pending;
        final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){images=actors.values().stream().map(a->new Image(a,graph.links.key(a.body))).toList();if(work!=null)graph.capture(work);}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restored=new LinkedHashMap<UUID,Actor>();var plans=new ArrayList<Runnable>();
            for(var image:images){var body=(Marker)links.get(image.body);MatchAuthorityCheckpoint.world(game,(ServerLevel)body.level());restored.put(body.getUUID(),image.actor);plans.add(()->image.actor.body=body);}
            return ()->{plans.forEach(Runnable::run);actors=restored;sourceLevels=new IdentityHashMap<>();for(var actor:actors.values())sourceLevels.merge((ServerLevel)actor.body.level(),1,Integer::sum);rays=Collections.newSetFromMap(new IdentityHashMap<>());rays.addAll(accepted);pending=work;lastTick=clock.last(game);};
        }
    }
}
