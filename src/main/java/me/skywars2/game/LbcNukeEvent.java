package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Source bruh nuclear event: one replaceable global schedule, native selectors and queued native terrain/blasts. */
public final class LbcNukeEvent {
    private static final String TAG="skywars2.nuke_event", CHILD="skywars2.nuke_event.child";
    private static final net.minecraft.server.level.TicketType LEGACY_TICKET=new net.minecraft.server.level.TicketType(0,net.minecraft.server.level.TicketType.FLAG_LOADING|net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    public static void registerLegacyTicket(){net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:legacy_nuclear"),LEGACY_TICKET);}

    private static final EntitySelector ARMED=selector("@e[tag=nuke1,tag=nuke_bruh]");
    private static final EntitySelector POINT_ARMED=selector("@e[tag=nuke1]");
    private static final EntitySelector RANDOM=selector("@e[tag=!spectator,sort=random,limit=16]");
    private static final EntitySelector NEAREST=selector("@p[gamemode=!spectator]");
    private static final Block[] BASE={Blocks.NETHERITE_BLOCK,Blocks.DIAMOND_BLOCK,Blocks.EMERALD_BLOCK,Blocks.GOLD_BLOCK,Blocks.IRON_BLOCK};
    private static final Block[] PANES={Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.WHITE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.RED),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BLACK),
            Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BLUE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.MAGENTA),
            Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.PURPLE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.YELLOW),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.PINK),
            Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.BROWN),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.ORANGE),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.GREEN),
            Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIME),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.CYAN),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.GRAY),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY)};
    private static final String[] MUSIC={"nuke","ponos","backrooms","burning_balance","penis_bossfight","kazahstan"};
    private record Cause(UUID author,LbcIndirectKills.Owner accepted,String epoch){}
    /** Unpublished source survives in captured DamageSource records, without a service-map entry. */
    private static final class BlastSource extends Marker {
        final Cause cause;BlastSource(ServerLevel level,Cause cause){super(EntityTypes.MARKER,level);this.cause=cause;}
    }
    private static final class State {
        final Cause cause;final boolean point;int lifetime;boolean activated;
        State(Cause cause){this(cause,false);}
        State(Cause cause,boolean point){this.cause=cause;this.point=point;activated=point;}
    }
    private record Site(ServerLevel level,Vec3 at,float yaw,Cause cause){}
    private record Visual(Marker marker,Cause cause,int age){}
    private record SavedChild(Cause cause,int visualAge){}
    private record Burst(Site site,long after,float power){}
    private record Terrain(Site site,NuclearTerrain.Shape shape){}
    private final GameController game;
    private IdentityHashMap<Marker,State> actors=new IdentityHashMap<>();
    private final Map<UUID,State> unloaded=new HashMap<>();
    // Native DamageSource/deferred Time Stop records keep their direct source alive as needed.
    private final Map<Entity,Cause> sources=new WeakHashMap<>();
    private final Map<UUID,SavedChild> unloadedChildren=new HashMap<>();
    private final List<Visual> visuals=new ArrayList<>();
    private final Set<Job> jobs=Collections.newSetFromMap(new IdentityHashMap<>());
    private final IdentityHashMap<LivingEntity,Job> statusBursts=new IdentityHashMap<>();
    private volatile List<MapManager.TemplateSlice> air=List.of();
    private int generation,lastTick=Integer.MIN_VALUE,pending=-1;
    private int callbackDepth,cadenceOffset,preparationToken;private boolean rewinding;private volatile boolean preparing;
    private MatchAuthorityCheckpoint.Clock stagedClock;private int stagedPhase;
    private String pendingEpoch;
    private int pendingEoe=-1;private Cause pendingEoeCause;
    private Cause executingCause;private Entity executingEmitter;private ServerLevel executingLevel;
    private long warnings,schedules,detonations,centers,blasts,beacons;
    public LbcNukeEvent(GameController game){this.game=game;}

    private static EntitySelector selector(String input){try{return new EntitySelectorParser(new StringReader(input),true).parse();}
        catch(CommandSyntaxException error){throw new ExceptionInInitializerError(error);}}
    private List<? extends Entity> select(EntitySelector selector,Site at){try{return selector.findEntities(game.server().createCommandSourceStack()
            .withLevel(at.level).withPosition(at.at).withRotation(new Vec2(0,at.yaw)).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput());}
        catch(CommandSyntaxException error){throw new IllegalStateException("Native nuclear selector failed",error);}}
    public void prepare(){preparationToken++;generation++;preparing=false;air=List.of();}
    public boolean prepared(){return generation>0;}
    private boolean live(String epoch){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch);}
    private boolean paused(ServerLevel level){return game.items().timeStop().stopped(level);}
    public static boolean managed(Entity entity){return entity!=null&&(entity.entityTags().contains(TAG)||entity.entityTags().contains(CHILD));}
    public static boolean wild(Entity entity){return managed(entity);}
    private Cause lineage(Entity entity){return entity instanceof BlastSource blast?blast.cause:sources.get(entity);}
    public ServerPlayer owner(Entity entity){var cause=lineage(entity);return cause==null||cause.author==null?null:game.server().getPlayerList().getPlayer(cause.author);}
    public LbcIndirectKills.Owner cause(Entity entity){var cause=lineage(entity);return cause==null?null:cause.accepted;}
    public boolean canAffect(Entity source,Entity target){var cause=lineage(source);return cause!=null&&live(cause.epoch)
            &&game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target);}
    /** Only entry admission uses the living author; accepted consequences outlive their author. */
    public boolean start(ServerPlayer author,Vec3 origin,boolean zelensky){
        if(rewinding)return false;
        callbackDepth++;try{
        if(author==null||origin==null||!Double.isFinite(origin.x)||!Double.isFinite(origin.y)||!Double.isFinite(origin.z)
                ||!game.canUseGameplayItems(author)||game.isObserver(author)||!game.items().wands().validRoleTarget(author,false)
                ||!author.isAlive()||author.isRemoved()||!LbcDimensionItems.ordinary(author)
                ||!live(game.items().epoch())||paused(author.level())||!author.level().hasChunkAt(BlockPos.containing(origin)))return false;
        if(generation==0)prepare();
        var marker=EntityTypes.MARKER.create(author.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);if(marker==null)return false;
        marker.setPos(origin);marker.entityTags().addAll(List.of(TAG,"nuke_bruh","rc_1sec","marker_2t","mecha"));if(zelensky)marker.entityTags().add("zelensky");
        game.items().markCurrentItemEpoch(marker);var state=new State(new Cause(author.getUUID(),game.items().indirectKills().wildCause(author),game.items().epoch()));
        actors.put(marker,state);sources.put(marker,state.cause);
        if(!author.level().addFreshEntity(marker)){actors.remove(marker);sources.remove(marker);return false;}
        game.items().timeStop().prepareFrozen(marker);return true;

        }finally{callbackDepth--;}
    }
    /** Source nuke_eoe: immediate strict beacon and a separate globally REPLACE'd nuke1 deadline. */
    public boolean startEoe(ServerPlayer author,ServerLevel level,Vec3 at,LbcIndirectKills.Owner accepted){
        if(rewinding)return false;
        callbackDepth++;try{
        if(accepted==null||!live(game.items().epoch())||!LbcDimensionItems.ordinary(level)||paused(level)
                ||!level.hasChunkAt(BlockPos.containing(at)))return false;
        if(generation==0)prepare();
        var cause=new Cause(accepted.player(),accepted,game.items().epoch());var site=new Site(level,at,0,cause);
        var origin=BlockPos.containing(at);var job=new Job(cause.epoch);
        job.add(level,origin.offset(-1,0,-1),origin.offset(1,0,1),cause,
                ()->LbcDropBlocks.fill(level,origin.offset(-1,0,-1),origin.offset(1,0,1),Blocks.NETHERITE_BLOCK,true));
        for(int ceiling:new int[]{319,255})job.add(level,origin.above(3),new BlockPos(origin.getX(),ceiling,origin.getZ()),cause,
                ()->LbcDropBlocks.fill(level,origin.above(3),new BlockPos(origin.getX(),ceiling,origin.getZ()),Blocks.AIR,true));
        job.add(level,origin.above(),origin.above(2),cause,()->{
            LbcDropBlocks.setBlock(level,origin.above(),Blocks.BEACON.defaultBlockState(),true);
            LbcDropBlocks.setBlock(level,origin.above(2),Blocks.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(),true);
        });job.submit();
        LbcOverload.sound(level,at,"minecraft:lbcsounds.nuke",.05f,1,0,.05f);
        var marker=EntityTypes.MARKER.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if(marker!=null){marker.setPos(at);marker.entityTags().addAll(List.of(TAG,"nuke","nuke1","rc_1sec","marker_2t","mecha"));
            game.items().markCurrentItemEpoch(marker);var state=new State(cause,true);actors.put(marker,state);sources.put(marker,cause);
            if(!level.addFreshEntity(marker)){actors.remove(marker);sources.remove(marker);}else game.items().timeStop().prepareFrozen(marker);}
        pendingEoe=400;pendingEoeCause=cause;schedules++;return true;

        }finally{callbackDepth--;}
    }
    private static boolean resident(ServerLevel level,BlockPos from,BlockPos to){
        for(int x=Math.min(from.getX(),to.getX())>>4;x<=Math.max(from.getX(),to.getX())>>4;x++)
            for(int z=Math.min(from.getZ(),to.getZ())>>4;z<=Math.max(from.getZ(),to.getZ())>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private boolean stooper(Marker marker){return !((ServerLevel)marker.level()).getEntities((Entity)null,marker.getBoundingBox().inflate(10),
            e->e.isAlive()&&e.entityTags().contains("stooper")&&e.position().distanceToSqr(marker.position())<=100).isEmpty();}
    private void warning(Marker marker,State state){
        var at=new Site((ServerLevel)marker.level(),marker.position(),marker.getYRot(),state.cause);
        Component name;
        if(marker.entityTags().contains("zelensky"))name=Component.literal("\u0412\u043e\u043b\u043e\u0434\u0438\u043c\u0438\u0440 \u0417\u0435\u043b\u0435\u043d\u0441\u044c\u043a\u0438\u0439 ");
        else {var players=select(NEAREST,at);name=players.isEmpty()?Component.empty():players.getFirst().getDisplayName().copy();}
        var subtitle=name.copy().withStyle(ChatFormatting.RED).withStyle(s->s.withItalic(false))
                .append(Component.translatable("nuke_lol_activated").withStyle(ChatFormatting.DARK_RED));
        for(var player:game.server().getPlayerList().getPlayers()){
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
        state.activated=true;marker.entityTags().add("nuke1");warnings++;
        for(float pitch:new float[]{1,.5f,1.5f,2})sound(at,"nuke",pitch);
        for(float pitch:new float[]{1,1.5f,2,.5f})sound(at,"ponos",pitch);
        for(float pitch:new float[]{1,2,0})for(String music:List.of("backrooms","burning_balance","penis_bossfight","kazahstan"))sound(at,music,pitch);
        if(!stooper(marker)){pending=700;pendingEpoch=state.cause.epoch;schedules++;}
    }
    private void sound(Site site,String id,float pitch){LbcOverload.sound(site.level,site.at,"minecraft:lbcsounds."+id,.5f,pitch,0,.5f);}
    public void tick(){
        if(rewinding)return;
        callbackDepth++;try{
        int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        // Schedule functions are global; common Time Stop pauses the one global active-tick deadline.
        boolean globalPause=game.items().wands().timeStopped();
        if(pending>=0&&!globalPause){if(!game.items().currentEpoch(pendingEpoch)){pending=-1;pendingEpoch=null;}
            else if(--pending<=0){pending=-1;pendingEpoch=null;detonate();}}
        if(pendingEoe>=0&&!globalPause){if(pendingEoeCause==null||!live(pendingEoeCause.epoch)){pendingEoe=-1;pendingEoeCause=null;}
            else if(--pendingEoe<=0){var accepted=pendingEoeCause;pendingEoe=-1;pendingEoeCause=null;
                var base=new Site(game.server().overworld(),Vec3.ZERO,0,accepted);
                var sites=new ArrayList<Site>();for(var entity:select(POINT_ARMED,base)){
                    var state=entity instanceof Marker marker?actors.get(marker):null;
                    sites.add(new Site((ServerLevel)entity.level(),entity.position(),entity.getYRot(),state==null?accepted:state.cause));}
                if(!sites.isEmpty())detonations++;for(var site:sites)pointBurst(site);
            }}
        for(var marker:List.copyOf(actors.keySet())){
            var state=actors.get(marker);var level=(ServerLevel)marker.level();
            if(marker.isRemoved()||!live(state.cause.epoch)){actors.remove(marker);continue;}
            if(state.point||paused(level)||!LbcDimensionItems.ordinary(level)||!resident(level,marker.blockPosition(),marker.blockPosition()))continue;
            if(Math.floorMod(now+cadenceOffset,2)==0){state.lifetime+=2;if(!state.activated)warning(marker,state);}
            if(Math.floorMod(now+cadenceOffset,20)==0){state.lifetime+=20;if(!stooper(marker))beacon(marker,state);}
        }
        for(var visual:List.copyOf(visuals)){
            var marker=visual.marker;var level=(ServerLevel)marker.level();
            if(marker.isRemoved()||!live(visual.cause.epoch)){visuals.remove(visual);continue;}
            if(paused(level)||!LbcDimensionItems.ordinary(level))continue;
            visuals.remove(visual);
            if(visual.age+1>=400)marker.discard();else visuals.add(new Visual(marker,visual.cause,visual.age+1));
        }

        }finally{callbackDepth--;}
    }
    private void beacon(Marker marker,State state){
        var origin=marker.blockPosition();var site=new Site((ServerLevel)marker.level(),marker.position(),marker.getYRot(),state.cause);
        var job=new Job(state.cause.epoch);beacons++;
        for(int i=0;i<5;i++){int layer=i,r=i+1;job.add(site.level,origin.offset(-r,-layer,-r),origin.offset(r,-layer,r),state.cause,
                ()->LbcDropBlocks.fill(site.level,origin.offset(-r,-layer,-r),origin.offset(r,-layer,r),BASE[layer],true));}
        if(state.lifetime>=1&&state.lifetime<=20)job.add(site.level,origin.above(3),new BlockPos(origin.getX(),319,origin.getZ()),state.cause,
                ()->LbcDropBlocks.fill(site.level,origin.above(3),new BlockPos(origin.getX(),319,origin.getZ()),Blocks.AIR,true));
        job.add(site.level,origin.above(),origin.above(),state.cause,()->LbcDropBlocks.setBlock(site.level,origin.above(),Blocks.BEACON.defaultBlockState(),false));
        job.add(site.level,origin.above(2),origin.above(2),state.cause,()->{
            int selected=site.level.getRandom().nextInt(16);LbcDropBlocks.setBlock(site.level,origin.above(2),PANES[selected].defaultBlockState(),false);
        });job.submit();
    }
    private void detonate(){
        var base=new Site(game.server().overworld(),Vec3.ZERO,0,null);var selected=select(ARMED,base);
        var sites=new ArrayList<Site>();
        for(var entity:selected)if(entity instanceof Marker marker&&actors.containsKey(marker)){
            var state=actors.get(marker);sites.add(new Site((ServerLevel)marker.level(),marker.position(),marker.getYRot(),state.cause));
        }
        if(sites.isEmpty())return;detonations++;
        var job=new Job(game.items().epoch());
        // Each execute-at fork snapshots its position; later native kills cannot move later contexts.
        for(var site:sites)job.add(site.level,BlockPos.containing(site.at),BlockPos.containing(site.at),site.cause,()->appendMarker(job,site));
        job.submit();
    }
    private void appendMarker(Job job,Site marker){
        if(LbcExplosionSuppression.suppressed(game,marker.level,marker.at))return;
        var selected=select(RANDOM,marker);var sites=new ArrayList<Site>();
        for(var entity:selected)sites.add(new Site((ServerLevel)entity.level(),entity.position(),entity.getYRot(),marker.cause));
        var additions=new ArrayDeque<Operation>();
        for(var site:sites){
            if(LbcExplosionSuppression.suppressed(game,site.level,site.at))continue;
            centers++;
            job.terrain.add(new Terrain(site,NuclearTerrain.point(site.at)));
            additions.add(new Operation(site.level,BlockPos.containing(site.at),BlockPos.containing(site.at),site.cause,()->wither(site)));
            // Creeper fuse1 was a deferred explosion carrier. Queue exact24 native MOB blasts after the synchronous kill phase.
            job.bursts.add(new Burst(site,game.server().getTickCount(),16));
            blast(site,site.at.add(18,0,0),16);
            additions.add(killOperation(site));
        }
        additions.add(new Operation(marker.level,BlockPos.containing(marker.at),BlockPos.containing(marker.at),marker.cause,()->{
            for(var player:game.server().getPlayerList().getPlayers()){
                player.connection.send(new ClientboundStopSoundPacket(Identifier.parse("minecraft:lbcsounds.nuke"),SoundSource.MASTER));
                for(String music:MUSIC)player.connection.send(new ClientboundStopSoundPacket(Identifier.parse("minecraft:lbcsounds."+music),SoundSource.MASTER));
            }
            visual(marker);
        }));
        additions.add(killOperation(marker));
        while(!additions.isEmpty())job.operations.addFirst(additions.removeLast());
    }
    /** Point nuclear status shares terrain/visual lifecycle, retaining its own source angles and power. */
    public boolean pointBurst(ServerPlayer author,ServerLevel level,Vec3 at){
        return pointBurst(author,level,at,false);
    }
    /** Only a repeating body status joins its own pending payload; independent bombs remain independent. */
    public boolean pointStatusBurst(ServerPlayer author,ServerLevel level,Vec3 at){
        if(statusBursts.containsKey(author))return false;
        return pointBurst(author,level,at,true);
    }
    public boolean pendingStatusBurst(ServerPlayer author){return statusBursts.containsKey(author);}
    /** A projectile credits its true shooter while coalescing repeated pulses by the actual victim body. */
    public boolean pointStatusBurst(LivingEntity body,LbcIndirectKills.Owner accepted){
        var level=(ServerLevel)body.level();
        if(statusBursts.containsKey(body)||!body.isAlive()||!live(game.items().epoch())||!LbcDimensionItems.ordinary(level)||paused(level)
                ||!level.hasChunkAt(BlockPos.containing(body.position()))||LbcExplosionSuppression.suppressed(game,level,body.position()))return false;
        if(generation==0)prepare();
        pointBurst(new Site(level,body.position(),body.getYRot(),new Cause(accepted==null?null:accepted.player(),accepted,game.items().epoch())),body);return true;
    }

    /** Native ammo already owns admission and may legitimately have no player author. */
    boolean arrowBurst(ServerLevel level,Vec3 at,float yaw,LbcIndirectKills.Owner accepted){
        if(!live(game.items().epoch())||!LbcDimensionItems.ordinary(level)||paused(level)
                ||!level.hasChunkAt(BlockPos.containing(at))||LbcExplosionSuppression.suppressed(game,level,at))return false;
        if(generation==0)prepare();
        var wild=accepted==null?null:new LbcIndirectKills.Owner(accepted.player(),accepted.order(),true,true);
        var cause=new Cause(accepted==null?null:accepted.player(),wild,game.items().epoch());
        pointBurst(new Site(level,at,yaw,cause));return true;
    }
    private boolean pointBurst(ServerPlayer author,ServerLevel level,Vec3 at,boolean status){
        if(author==null||!live(game.items().epoch())||!LbcDimensionItems.ordinary(level)||paused(level)
                ||!level.hasChunkAt(BlockPos.containing(at))||LbcExplosionSuppression.suppressed(game,level,at))return false;
        if(generation==0)prepare();
        var site=new Site(level,at,author.getYRot(),new Cause(author.getUUID(),game.items().indirectKills().wildCause(author),game.items().epoch()));
        pointBurst(site,status?author:null);return true;
    }
    /** Original old-mode nuclear payload, using this same native damage/job owner. */
    public boolean legacyBurst(ServerLevel level,Vec3 at,LbcIndirectKills.Owner accepted){
        if(accepted==null||!live(game.items().epoch())||paused(level)||LbcExplosionSuppression.suppressed(game,level,at))return false;
        if(generation==0)prepare();
        var cause=new Cause(accepted.player(),accepted,game.items().epoch());var site=new Site(level,at,0,cause);
        var center=BlockPos.containing(at);var job=new Job(cause.epoch);job.legacyClear=new LegacyClear(site);
        job.add(level,center,center,cause,()->{
            var coverage=LbcExplosionSuppression.coverage(game,level);if(coverage.contains(at))return;
            for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(45),e->e.position().distanceToSqr(at)<=2025
                    &&game.items().wands().validRoleTarget(e,false)&&!coverage.targets(e)))
                game.items().indirectKills().wildEffect(target,ItemEffectLevels.create(MobEffects.WITHER,400,30,false,false),accepted);
        });
        for(var offset:List.of(new Vec3(10,0,0),new Vec3(-10,0,0),new Vec3(0,0,10),new Vec3(0,0,-10),new Vec3(0,10,0),new Vec3(-10,-15,0),new Vec3(10,-15,0),new Vec3(0,-15,-10),new Vec3(0,-15,10))){
            var point=at.add(offset);var pos=BlockPos.containing(point);
            job.add(level,pos.offset(-61,-61,-61),pos.offset(61,61,61),cause,()->blast(site,point,30,false));
        }
        job.operations.add(killOperation(site));job.submit();return true;
    }
    private void pointBurst(Site site){pointBurst(site,null);}
    private void pointBurst(Site site,LivingEntity statusBody){
        if(LbcExplosionSuppression.suppressed(game,site.level,site.at))return;
        var level=site.level;var at=site.at;var job=new Job(site.cause.epoch);var center=BlockPos.containing(at);
        visual(site);
        job.terrain.add(new Terrain(site,NuclearTerrain.point(at)));
        job.add(level,center,center,site.cause,()->wither(site));
        job.bursts.add(new Burst(site,game.server().getTickCount(),25));
        job.add(level,center,center,site.cause,()->{
            for(var player:game.server().getPlayerList().getPlayers())player.connection.send(new ClientboundStopSoundPacket(Identifier.parse("minecraft:lbcsounds.nuke"),SoundSource.MASTER));
        });
        job.operations.add(killOperation(site));job.statusBody=statusBody;job.submit();
        if(statusBody!=null)statusBursts.put(statusBody,job);
        // The first wave is centered on the actual detonation body, so enclosure cannot
        // postpone the owner's native lethal hit. Other targets retain native wall exposure.
        blast(site,at,25);
    }
    private void wither(Site site){
        var coverage=LbcExplosionSuppression.coverage(game,site.level);if(coverage.contains(site.at))return;
        for(var entity:site.level.getEntities((Entity)null,new AABB(site.at,site.at).inflate(45),
                e->e.isAlive()&&!e.entityTags().contains("spectator")&&e.position().distanceToSqr(site.at)<=2025&&!coverage.targets(e)))
            if(entity instanceof LivingEntity living)game.items().indirectKills().wildEffect(living,ItemEffectLevels.create(MobEffects.WITHER,400,30,false,false,false),site.cause.accepted);
    }
    private Operation killOperation(Site site){return new Operation(site.level,BlockPos.containing(site.at),BlockPos.containing(site.at),site.cause,()->{
        var coverage=LbcExplosionSuppression.coverage(game,site.level);if(coverage.contains(site.at))return;
        for(var entity:List.copyOf(site.level.getEntities((Entity)null,new AABB(site.at,site.at).inflate(14),e->e.isAlive()&&e.getType()!=EntityTypes.CREEPER
                &&e.getType()!=EntityTypes.PLAYER&&!e.entityTags().contains("slowparticlesexplode_2")&&!e.entityTags().contains("strict_map_object")
                &&e.position().distanceToSqr(site.at)<=196&&!coverage.targets(e)))){
            if(entity instanceof LivingEntity living)game.items().indirectKills().inheritedKill(living,site.cause.accepted);else entity.kill(site.level);
        }
    });}
    private void visual(Site site){
        if(LbcExplosionSuppression.suppressed(game,site.level,site.at))return;
        var marker=EntityTypes.MARKER.create(site.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);if(marker==null)return;
        marker.setPos(site.at);marker.entityTags().addAll(List.of(CHILD,"slowraycast","slowparticlesexplode_2","player","magic"));
        game.items().markCurrentItemEpoch(marker);sources.put(marker,site.cause);
        if(site.level.addFreshEntity(marker)){
            visuals.add(new Visual(marker,site.cause,0));game.items().timeStop().prepareFrozen(marker);
            // Existing approved per-client shader replaces the legacy block-marker mushroom packets.
            game.items().nuclearVisuals().show(site.level,site.at,12,true);
        }else sources.remove(marker);
    }
    private static final class NuclearDamage extends DamageSource {
        final Vec3 position;
        NuclearDamage(ServerLevel level,Entity direct,ServerPlayer author,Vec3 at){super(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.PLAYER_EXPLOSION),direct,author);position=at;}
        @Override public Vec3 getSourcePosition(){return position;}
        @Override public boolean scalesWithDifficulty(){return true;}
        @Override public Component getLocalizedDeathMessage(LivingEntity victim){return Component.translatable("death.attack.explosion.player",victim.getDisplayName(),Component.translatable("killed_by_nuke"));}
    }
    private void blast(Site site,Vec3 at,float power){blast(site,at,power,true);}
    private void blast(Site site,Vec3 at,float power,boolean damageOnly){
        if(LbcExplosionSuppression.suppressed(game,site.level,at))return;
        callbackDepth++;try{
        var direct=new BlastSource(site.level,site.cause);
        direct.setPos(at);direct.entityTags().add(CHILD);direct.setCustomName(Component.translatable("killed_by_nuke"));game.items().markCurrentItemEpoch(direct);
        var previousCause=executingCause;var previousLevel=executingLevel;var previousEmitter=executingEmitter;
        executingCause=site.cause;executingLevel=site.level;executingEmitter=direct;
        try{
            var damage=game.items().indirectKills().captureDamage(new NuclearDamage(site.level,direct,site.cause.author==null?null:game.server().getPlayerList().getPlayer(site.cause.author),at),site.cause.accepted);
            Runnable action=()->game.items().indirectKills().trapChanges(site.level,site.cause.accepted,
                    ()->site.level.explode(direct,damage,null,at.x,at.y,at.z,power,false,Level.ExplosionInteraction.MOB));
            if(damageOnly||at.y-power*2>site.level.getMaxY()||at.y+power*2<site.level.getMinY())NuclearTerrain.damageWave(direct,action);else action.run();blasts++;
        }finally{executingCause=previousCause;executingLevel=previousLevel;executingEmitter=previousEmitter;direct.discard();}

        }finally{callbackDepth--;}
    }
    public boolean executing(ServerLevel level){return executingCause!=null&&executingLevel==level;}
    public Entity currentEmitter(){return executingEmitter;}
    public void inherit(Entity parent,Entity child){var cause=lineage(parent);if(cause!=null)inherit(child,cause);}
    public void inheritChild(Entity child){if(executingCause!=null&&child.level()==executingLevel)inherit(child,executingCause);}
    private void inherit(Entity child,Cause cause){child.entityTags().add(CHILD);game.items().markCurrentItemEpoch(child);sources.put(child,cause);game.items().timeStop().prepareFrozen(child);}
    private final class Operation {
        final ServerLevel level;final BlockPos from,to;final Cause cause;final Runnable action;
        Operation(ServerLevel level,BlockPos from,BlockPos to,Cause cause,Runnable action){this.level=level;this.from=from;this.to=to;this.cause=cause;this.action=action;}
        boolean ready(){return !paused(level);}
        void run(){if(resident(level,from,to))game.items().indirectKills().trapChanges(level,cause.accepted,action);}
    }
    /** Three strict source fills retain native height/volume admission, then load each blast footprint asynchronously. */
    private final class LegacyClear {
        final Site site;final BlockPos center;final int[][] bands={{15,45},{-15,15},{-45,-15}};
        final Map<net.minecraft.world.level.ChunkPos,java.util.concurrent.CompletableFuture<?>> tickets=new LinkedHashMap<>();
        int band,cursor;boolean done;
        LegacyClear(Site site){this.site=site;center=BlockPos.containing(site.at);}
        boolean resident(BlockPos from,BlockPos to){
            if(from.getY()>site.level.getMaxY()||to.getY()<site.level.getMinY())return true;
            boolean ready=true;
            for(int x=from.getX()>>4;x<=to.getX()>>4;x++)for(int z=from.getZ()>>4;z<=to.getZ()>>4;z++){
                if(site.level.getChunkSource().getChunkNow(x,z)!=null)continue;
                var chunk=new net.minecraft.world.level.ChunkPos(x,z);tickets.computeIfAbsent(chunk,key->site.level.getChunkSource().addTicketAndLoadWithRadius(LEGACY_TICKET,key,0));ready=false;
            }
            return ready;
        }
        boolean step(){
            var level=site.level;
            var coverage=LbcExplosionSuppression.coverage(game,level);if(coverage.contains(site.at)){done=true;return true;}
            for(int budget=4096;budget>0&&band<bands.length;){
                var range=bands[band];int low=center.getY()+range[0],high=center.getY()+range[1];
                if(level.isOutsideBuildHeight(low)||level.isOutsideBuildHeight(high)||29791>level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS)){band++;cursor=0;continue;}
                if(cursor>=29791){band++;cursor=0;continue;}
                var pos=center.offset(cursor%31-15,range[0]+cursor/(31*31),cursor/31%31-15);
                if(!resident(pos,pos))return false;
                cursor++;budget--;
                if(!coverage.blocks(pos)&&!level.getBlockState(pos).isAir())game.items().indirectKills().capturedWorldChanges(level,site.cause.accepted,()->LbcDropBlocks.setBlock(level,pos,Blocks.AIR.defaultBlockState(),true));
            }
            done=band>=bands.length;return done;
        }
        void close(){for(var chunk:tickets.keySet())site.level.getChunkSource().removeTicketWithRadius(LEGACY_TICKET,chunk,0);tickets.clear();}
    }
    private final class Job implements LbcStonekivi.Work {
        LegacyClear legacyClear;
        final String epoch;final ArrayDeque<Operation> operations=new ArrayDeque<>();final ArrayDeque<Burst> bursts=new ArrayDeque<>();
        final ArrayDeque<Terrain> terrain=new ArrayDeque<>();boolean cancelled;
        LivingEntity statusBody;
        Job(String epoch){this.epoch=epoch;}
        void add(ServerLevel level,BlockPos from,BlockPos to,Cause cause,Runnable action){operations.add(new Operation(level,from,to,cause,action));}
        void submit(){jobs.add(this);game.items().stonekivi().queuedWork(this);}
        @Override public boolean ready(){if(cancelled||!live(epoch))return true;
            if(legacyClear!=null&&!legacyClear.done)return !paused(legacyClear.site.level);
            if(!operations.isEmpty()){var operation=operations.peekFirst();return prepared()&&operation.ready()&&(legacyClear==null||legacyClear.resident(operation.from,operation.to));}
            if(!terrain.isEmpty())return !paused(terrain.peekFirst().site.level);
            if(bursts.isEmpty())return true;var burst=bursts.peekFirst();return game.server().getTickCount()>burst.after&&!paused(burst.site.level);}
        @Override public boolean step(){
        callbackDepth++;try{
            if(cancelled||!live(epoch)){cancel();return true;}
            if(legacyClear!=null&&!legacyClear.done){legacyClear.step();return false;}
            if(!operations.isEmpty()){
                var operation=operations.removeFirst();
                if(legacyClear!=null)game.items().indirectKills().trapChanges(operation.level,operation.cause.accepted,operation.action);else operation.run();
                return false;
            }
            if(!terrain.isEmpty()){var task=terrain.peekFirst();if(task.shape.step(game,task.site.level,task.site.cause.accepted))terrain.removeFirst();return false;}
            if(!bursts.isEmpty()){
                var burst=bursts.removeFirst();var site=burst.site;
                // Source order: outer12(radius39), then inner12(radius18), cardinal4 per Y layer.
                var offsets=new ArrayDeque<Operation>();
                for(int radius:new int[]{39,18})for(int y:new int[]{0,40,-40})for(var offset:List.of(new Vec3(radius,y,0),new Vec3(-radius,y,0),new Vec3(0,y,radius),new Vec3(0,y,-radius))){
                    if(radius==18&&y==0&&offset.x==18)continue; // already dispatched synchronously
                    var point=site.at.add(offset);var p=BlockPos.containing(point);int reach=(int)(burst.power*2)+1;offsets.add(new Operation(site.level,p.offset(-reach,-reach,-reach),p.offset(reach,reach,reach),site.cause,()->blast(site,point,burst.power)));
                }
                operations.addAll(offsets);return false;
            }
            cancel();return true;

        }finally{callbackDepth--;}
    }
        @Override public void cancel(){
        callbackDepth++;try{cancelled=true;if(legacyClear!=null)legacyClear.close();operations.clear();bursts.clear();terrain.clear();jobs.remove(this);
            if(statusBody!=null&&statusBursts.get(statusBody)==this)statusBursts.remove(statusBody);
        }finally{callbackDepth--;}
    }
    }
    public void loaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;
        if(entity.entityTags().contains(CHILD)){
            if(sources.containsKey(entity))return;
            var saved=unloadedChildren.remove(entity.getUUID());
            if(saved==null||!live(saved.cause.epoch)||!game.items().currentEpoch(entity)){entity.discard();return;}
            sources.put(entity,saved.cause);
            if(saved.visualAge>=0&&entity instanceof Marker marker)visuals.add(new Visual(marker,saved.cause,saved.visualAge));
            game.items().timeStop().prepareFrozen(entity);return;
        }
        if(!(entity instanceof Marker marker)||!entity.entityTags().contains(TAG))return;
        if(!live(game.items().epoch())||!game.items().currentEpoch(entity)){entity.discard();return;}
        if(!actors.containsKey(marker)){var state=unloaded.remove(entity.getUUID());if(state==null){entity.discard();return;}actors.put(marker,state);sources.put(marker,state.cause);}
        game.items().timeStop().prepareFrozen(marker);

        }finally{callbackDepth--;}
    }
    public void unloaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;
        if(entity.entityTags().contains(CHILD)){
            var cause=sources.get(entity);int age=-1;
            for(var visual:List.copyOf(visuals))if(visual.marker==entity){age=visual.age;visuals.remove(visual);}
            if(cause!=null&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloadedChildren.put(entity.getUUID(),new SavedChild(cause,age));
            sources.remove(entity);
        }
        if(entity instanceof Marker marker&&entity.entityTags().contains(TAG)){
            var state=actors.remove(marker);if(state!=null&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(entity.getUUID(),state);
        }

        }finally{callbackDepth--;}
    }
    /** Accepted nuclear consequences persist across the causal author's death/disconnect, like native armed explosives. */
    public void forget(ServerPlayer player){}
    public void stop(){
        callbackDepth++;try{rewinding=false;stagedClock=null;cadenceOffset=0;pending=-1;pendingEpoch=null;pendingEoe=-1;pendingEoeCause=null;for(var job:List.copyOf(jobs))job.cancel();
        for(var entity:List.copyOf(actors.keySet()))entity.discard();actors.clear();unloaded.clear();
        for(var visual:List.copyOf(visuals))visual.marker.discard();visuals.clear();for(var entity:List.copyOf(sources.keySet()))entity.discard();sources.clear();unloadedChildren.clear();
        }finally{callbackDepth--;}
    }
    public int pointPendingTicks(){return pendingEoe;}
    public int markerCount(){return actors.size();}public int pendingTicks(){return pending;}public int jobCount(){return jobs.size();}
    public long warnings(){return warnings;}public long schedules(){return schedules;}public long detonations(){return detonations;}
    public long centers(){return centers;}public long blasts(){return blasts;}public long beacons(){return beacons;}
    public List<Marker> markers(){return List.copyOf(actors.keySet());}
    public record View(UUID marker,UUID author,String dimension,Vec3 origin,int lifetime,boolean armed,String epoch){}
    public List<View> snapshot(){return actors.entrySet().stream().map(e->new View(e.getKey().getUUID(),e.getValue().cause.author,
            e.getKey().level().dimension().identifier().toString(),e.getKey().position(),e.getValue().lifetime,e.getValue().activated,e.getValue().cause.epoch)).toList();}

    boolean captureReady(){return callbackDepth==0&&executingCause==null&&!preparing&&jobs.isEmpty()&&!rewinding;}
    void checkpointBoundary(){if(callbackDepth!=0||executingCause!=null)throw new IllegalStateException("Finish nuclear callbacks/preparation before checkpointing");}
    void cancelFutureTerrainForRollback(){checkpointBoundary();preparationToken++;preparing=false;for(var job:List.copyOf(jobs))job.cancel();}
    void prepareFrozen(){checkpointBoundary();cancelFutureTerrainForRollback();rewinding=true;stagedClock=null;}
    void publishFrozen(){checkpointBoundary();if(stagedClock!=null){lastTick=stagedClock.last(game);cadenceOffset=stagedPhase-game.server().getTickCount();}stagedClock=null;rewinding=false;}
    void cancelFrozen(){checkpointBoundary();stagedClock=null;rewinding=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();if(!captureReady())throw new IllegalStateException("Complete nuclear terrain/bursts before capture");return new Checkpoint(links);}
    final class Checkpoint {
        private record Value(State original,int lifetime,boolean activated){}
        private record Source(MatchDamageSources.Ref entity,Cause cause){}
        private record Picture(MatchDamageSources.Ref marker,Cause cause,int age){}
        private final MatchSpellCheckpoint.IdentityImage<Marker,State> registry;
        private final Map<UUID,State> dormant=new HashMap<>(unloaded);
        private final Map<UUID,SavedChild> children=new HashMap<>(unloadedChildren);
        private final List<Value> values=new ArrayList<>();private final List<Source> lineage=new ArrayList<>();private final List<Picture> pictures=new ArrayList<>();
        private final List<MapManager.TemplateSlice> preparedAir=air;private final int version=generation,deadline=pending,eoe=pendingEoe;
        private final String deadlineEpoch=pendingEpoch;private final Cause eoeCause=pendingEoeCause;
        private final long savedWarnings=warnings,savedSchedules=schedules,savedDetonations=detonations,savedCenters=centers,savedBlasts=blasts,savedBeacons=beacons;
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        private final int phase=Math.floorMod(game.server().getTickCount()+cadenceOffset,20);
        Checkpoint(MatchSpellCheckpoint.Links links){
            for(var entity:actors.keySet())if(links.key(entity).historical()!=null)throw new IllegalStateException("Capture live event actor bodies");
            registry=new MatchSpellCheckpoint.IdentityImage<>(actors,links);var seen=Collections.newSetFromMap(new IdentityHashMap<State,Boolean>());
            for(var value:actors.values())if(seen.add(value))values.add(new Value(value,value.lifetime,value.activated));
            for(var value:unloaded.values())if(seen.add(value))values.add(new Value(value,value.lifetime,value.activated));
            sources.forEach((entity,cause)->lineage.add(new Source(links.key(entity),cause)));
            for(var visual:visuals)pictures.add(new Picture(links.key(visual.marker),visual.cause,visual.age));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restored=registry.restore(links);var nextSources=new ArrayList<Map.Entry<Entity,Cause>>();for(var source:lineage)nextSources.add(Map.entry(links.get(source.entity),source.cause));
            var nextPictures=new ArrayList<Visual>();for(var picture:pictures)nextPictures.add(new Visual((Marker)links.get(picture.marker),picture.cause,picture.age));
            return ()->{for(var value:values){value.original.lifetime=value.lifetime;value.original.activated=value.activated;}
                actors=restored;unloaded.clear();unloaded.putAll(dormant);unloadedChildren.clear();unloadedChildren.putAll(children);
                sources.clear();for(var source:nextSources)sources.put(source.getKey(),source.getValue());visuals.clear();visuals.addAll(nextPictures);
                air=preparedAir;generation=version;preparationToken++;preparing=false;pending=deadline;pendingEpoch=deadlineEpoch;pendingEoe=eoe;pendingEoeCause=eoeCause;
                warnings=savedWarnings;schedules=savedSchedules;detonations=savedDetonations;centers=savedCenters;blasts=savedBlasts;beacons=savedBeacons;stagedClock=clock;stagedPhase=phase;};
        }
    }
}
