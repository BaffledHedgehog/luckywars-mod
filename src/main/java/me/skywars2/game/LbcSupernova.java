package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One arena-wide Supernova. Terrain movement remains owned by the qualified chunk engine. */
public final class LbcSupernova {
    public interface GlobalActions {
        boolean canBegin(ServerLevel level, UUID owner);
        default void preparingMap(UUID event, UUID owner, LbcIndirectKills.Owner cause) { }
        void replaceMap(UUID event, UUID owner, LbcIndirectKills.Owner cause);
        void finished(UUID event, UUID owner, boolean success);
    }
    public enum Phase { IDLE, RUNNING, DRAINING, DELAY, MAP }
    private static final String TAG="skywars2.supernova", BENEFIT="skywars2.supernova.benefit";
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final CompoundTag GUN_FLAG=new CompoundTag();
    private static final TagKey<Block> AIRS_ONLY=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs_only"));
    static { GUN_FLAG.putInt("shish_gun",1); }
    public static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:supernova"),TICKET);}
    private final GameController game;
    private final GlobalActions actions;
    private final BooleanSupplier acceptNewWork;
    private final LbcNexusClone clones;
    private final Map<UUID,ItemEntity> katanas=new LinkedHashMap<>();
    private final Set<UUID> craftDirty=new LinkedHashSet<>();
    private final Map<ChunkPos,CompletableFuture<?>> loading=new HashMap<>();
    private final List<Point> points=new ArrayList<>(5);
    private final Set<UUID> retired=new HashSet<>();
    private final Map<UUID,BenefitLease> deferredBenefits=new LinkedHashMap<>();
    private Phase phase=Phase.IDLE;
    private UUID event,owner,root;
    private ServerLevel level;
    private Vec3 origin;
    private LbcIndirectKills.Owner cause;
    private Marker core;
    private boolean blockDrops,entityDrops,mobDrops,restoring,internalAbsorption,mapIntent;
    private int clock,delay,lastTick=Integer.MIN_VALUE;
    private MobEffectInstance absorptionBaseline;
    private Boolean pendingCompletion;
    /** Native ownership image, never a replacement absorption/health ledger. */
    private record BenefitLease(Object effects,MobEffectInstance absorption,boolean cleanup) {
        BenefitLease copy(){return new BenefitLease(OwnedEffectLayers.copyCheckpoint(effects),absorption==null?null:EffectLayers.copy(absorption),cleanup);}
    }
    private static final class Point {
        final UUID id; Vec3 position,candidate; Marker body;
        Point(UUID id,Vec3 position){this.id=id;this.position=position;}
    }
    private record PointImage(UUID id,Vec3 position,boolean loaded){}
    public LbcSupernova(GameController game,GlobalActions actions,BooleanSupplier acceptNewWork){
        this.game=game;this.actions=actions;this.acceptNewWork=acceptNewWork;
        clones=new LbcNexusClone(game,(destination,tx,ty,tz,centers,trees,author)->{
            game.items().nexusCompass().appendedIsland(tx,ty,tz);
            var ecology=game.items().nexusAcquisition().ecology();
            for(var p:centers)ecology.center(destination,p,author);
            for(var p:trees)ecology.tree(destination,p,author);
        });
    }
    public Phase phase(){return phase;}
    public String status(){return phase+" clock="+clock+" clones="+clones.pending()+" tickets="+loading.size();}
    public UUID eventId(){return event;}
    public UUID owner(){return owner;}
    public ServerLevel level(){return level;}
    public int activeTicks(){return clock;}
    public int pendingClones(){return clones.pending();}
    public List<Vec3> points(){return points.stream().map(p->p.position).toList();}
    private boolean running(){return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.server().tickRateManager().runsNormally()&&!game.items().wands().legacyTimeStopped()
            &&!game.items().timeStop().stopped(level==null?game.maps().level():level);}
    private boolean eligible(ServerPlayer player){return player!=null&&player.isAlive()&&!player.isRemoved()
            &&!player.isSpectator()&&!game.isObserver(player)&&game.canUseGameplayItems(player)
            &&!game.items().actionsBlocked(player)&&player.level()==game.maps().level();}
    private static boolean gun(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);return !stack.isEmpty()&&data!=null&&data.matchedBy(GUN_FLAG);}
    public void craftChanged(ServerPlayer player){if(!restoring)craftDirty.add(player.getUUID());}
    public void loaded(Entity body){
        if(restoring||body.isRemoved())return;
        if(body instanceof ServerPlayer player){joined(player);return;}
        if(retired.remove(body.getUUID())){body.discard();return;}
        if(body instanceof ItemEntity item){itemChanged(item);return;}
        if(body instanceof Marker marker){if(body.getUUID().equals(root))core=marker;for(var p:points)if(p.id.equals(body.getUUID()))p.body=marker;}
    }
    public void unloaded(Entity body){katanas.remove(body.getUUID());if(core==body)core=null;for(var p:points)if(p.body==body)p.body=null;}
    public void itemChanged(ItemEntity item){if(restoring)return;if(!item.isRemoved()&&item.level()==game.maps().level()&&LbcElectro.isShitKatana(item.getItem()))katanas.put(item.getUUID(),item);else katanas.remove(item.getUUID());}
    /** Invoke before the shared unload owner forgets its native leases. No world mutation. */
    public void leaving(ServerPlayer player){
        if(restoring||phase==Phase.IDLE||!Objects.equals(owner,player.getUUID()))return;
        var image=game.items().ownedEffects().checkpoint(player);
        var previous=deferredBenefits.get(player.getUUID());
        if(image==null&&previous!=null)image=previous.effects();
        deferredBenefits.put(player.getUUID(),new BenefitLease(image,absorptionBaseline==null?null:EffectLayers.copy(absorptionBaseline),false));
    }
    /** Invoke immediately after login restoration, before gameplay systems grant new effects. */
    public void joined(ServerPlayer player){
        if(restoring)return;var lease=deferredBenefits.remove(player.getUUID());if(lease==null)return;
        if(lease.effects()!=null)game.items().ownedEffects().restoreCheckpoint(player,lease.effects());
        if(lease.cleanup())withdrawBenefits(player,lease.absorption());
    }
    private boolean preflight(ServerPlayer player){return phase==Phase.IDLE&&running()&&acceptNewWork.getAsBoolean()&&eligible(player)&&actions.canBegin(player.level(),player.getUUID());}
    private void inventory(ServerPlayer player){
        if(!preflight(player))return;
        var grid=player.inventoryMenu.getCraftSlots();int katana=-1,gun=-1;
        for(int slot=0;slot<Math.min(4,grid.getContainerSize());slot++){
            var stack=grid.getItem(slot);if(katana<0&&LbcElectro.isShitKatana(stack))katana=slot;
            else if(gun<0&&gun(stack))gun=slot;
        }
        if(katana<0||gun<0)return;
        // Both native slots have been validated before either exact unit is debited.
        grid.getItem(katana).shrink(1);grid.getItem(gun).shrink(1);grid.setChanged();
        start(player,player.position());player.inventoryMenu.broadcastChanges();
    }
    private void dropped(){
        for(var katana:List.copyOf(katanas.values())){
            if(katana.isRemoved()||!LbcElectro.isShitKatana(katana.getItem())){katanas.remove(katana.getUUID());continue;}
            if(katana.entityTags().contains("time_stopped"))continue;
            var nearby=katana.level().getEntitiesOfClass(ItemEntity.class,katana.getBoundingBox().inflate(1),i->i!=katana&&!i.isRemoved()
                    &&i.position().distanceToSqr(katana.position())<=1&&i.position().distanceToSqr(katana.position())>=.000001&&gun(i.getItem())&&!i.entityTags().contains("time_stopped"));
            if(nearby.isEmpty())continue;
            var gun=nearby.getFirst();ServerPlayer player=null;
            if(katana.getOwner() instanceof ServerPlayer actual)player=actual;
            else if(gun.getOwner() instanceof ServerPlayer actual)player=actual;
            else for(var p:game.server().getPlayerList().getPlayers())if(p.level()==katana.level()&&eligible(p)
                    &&(player==null||p.distanceToSqr(katana)<player.distanceToSqr(katana)))player=p;
            if(!preflight(player))continue;
            katana.getItem().shrink(1);gun.getItem().shrink(1);
            if(katana.getItem().isEmpty())katana.discard();else katana.setItem(katana.getItem());
            if(gun.getItem().isEmpty())gun.discard();else gun.setItem(gun.getItem());
            start(player,katana.position());return;
        }
    }
    private Marker marker(Vec3 at){var body=new Marker(EntityTypes.MARKER,level);body.setPos(at);body.entityTags().add(TAG);game.items().markCurrentItemEpoch(body);level.addFreshEntity(body);return body;}
    private void start(ServerPlayer player,Vec3 at){
        event=UUID.randomUUID();owner=player.getUUID();cause=game.items().indirectKills().wildCause(player);level=player.level();origin=at;
        clock=0;delay=0;mapIntent=false;pendingCompletion=null;phase=Phase.RUNNING;
        var rules=level.getGameRules();blockDrops=rules.get(GameRules.BLOCK_DROPS);entityDrops=rules.get(GameRules.ENTITY_DROPS);mobDrops=rules.get(GameRules.MOB_DROPS);
        drops(false,false,false);core=marker(at);root=core.getUUID();
        for(int i=0;i<5;i++){var body=marker(at);var p=new Point(body.getUUID(),at);p.body=body;points.add(p);}
        game.items().ownedEffects().grant(player,BENEFIT,new MobEffectInstance(MobEffects.LEVITATION,200000,0),player);
        var old=ShadowAbsorption.state(player).entries.get(30);
        if(old==null)for(var effect=player.getEffect(MobEffects.ABSORPTION);effect!=null;effect=((EffectLayers.Access)effect).skywars2$layerHidden())if(effect.getAmplifier()==30){old=effect;break;}
        absorptionBaseline=old==null?null:EffectLayers.copy(old);
        internalAbsorption=true;try{player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200000,30),player);}finally{internalAbsorption=false;}
        present(player);
    }
    private void drops(boolean block,boolean entity,boolean mob){var rules=level.getGameRules();rules.set(GameRules.BLOCK_DROPS,block,game.server());rules.set(GameRules.ENTITY_DROPS,entity,game.server());rules.set(GameRules.MOB_DROPS,mob,game.server());}
    /** Successful native external absorption grants keep the same-amplifier baseline independent. */
    public void absorptionChanged(ServerPlayer player,MobEffectInstance effect){
        if(internalAbsorption||phase==Phase.IDLE||!player.getUUID().equals(owner)||!effect.is(MobEffects.ABSORPTION)||effect.getAmplifier()!=30)return;
        if(absorptionBaseline==null)absorptionBaseline=new MobEffectInstance(effect);else absorptionBaseline.update(effect);
    }
    /** Native external cleansing/depletion also consumes the hidden foreign baseline. */
    public void absorptionCleared(ServerPlayer player){
        if(!internalAbsorption&&phase!=Phase.IDLE&&Objects.equals(owner,player.getUUID()))absorptionBaseline=null;
    }
    private void lightning(Vec3 at){var bolt=new LightningBolt(EntityTypes.LIGHTNING_BOLT,level);bolt.setPos(at);bolt.setCause(game.server().getPlayerList().getPlayer(owner));game.items().markCurrentItemEpoch(bolt);level.addFreshEntity(bolt);}
    private void dust(Vec3 at,int color,int count,double x,double y,double z,double speed){for(var p:game.server().getPlayerList().getPlayers())if(p.level()==level)level.sendParticles(p,new DustParticleOptions(color,4),true,true,at.x,at.y,at.z,count,x,y,z,speed);}
    private void present(ServerPlayer player){
        dust(origin.add(0,1,0),0xffffff,1000,1,1,1,2);dust(origin.add(0,1,0),0x010001,1000,1,1,1,2);
        var damage=new LbcBabylonEffects.OwnerDamageSource(level,null,player,cause,game.items().epoch());
        game.items().indirectKills().trapChanges(level,cause,()->level.explode(null,damage,null,origin,4,false,Level.ExplosionInteraction.TNT));lightning(origin);
        for(var p:game.server().getPlayerList().getPlayers()){
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("end_begins")));
            p.level().sendParticles(p,net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xffffffff),true,true,p.getX(),p.getY(),p.getZ(),100,10,10,10,0);
        }
        level.playSound(null,origin.x,origin.y,origin.z,SoundEvents.WITHER_DEATH,SoundSource.MASTER,10000,0);
        level.playSound(null,origin.x,origin.y,origin.z,SoundEvents.WITHER_DEATH,SoundSource.MASTER,10000,2);
        level.playSound(null,origin.x,origin.y,origin.z,SoundEvents.END_PORTAL_SPAWN,SoundSource.MASTER,10000,2);
        level.playSound(null,origin.x,origin.y,origin.z,SoundEvents.END_PORTAL_SPAWN,SoundSource.MASTER,10000,1);
    }
    private boolean scatter(Point point){
        if(point.candidate==null){var random=level.getRandom();point.candidate=new Vec3(point.position.x+(random.nextDouble()*100-50),0,point.position.z+(random.nextDouble()*100-50));}
        var pos=BlockPos.containing(point.candidate);var chunkPos=new ChunkPos(pos.getX()>>4,pos.getZ()>>4);
        var chunk=level.getChunkSource().getChunkNow(chunkPos.x(),chunkPos.z());
        if(chunk==null){
            var future=loading.get(chunkPos);
            if(future!=null&&(future.isCompletedExceptionally()||future.isCancelled())){release(chunkPos);point.candidate=null;return false;}
            if(loading.size()<5)loading.computeIfAbsent(chunkPos,k->level.getChunkSource().addTicketAndLoadWithRadius(TICKET,k,0));return false;
        }
        int y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,pos.getX()&15,pos.getZ()&15)+1;
        var ground=chunk.getBlockState(new BlockPos(pos.getX(),y-1,pos.getZ()));
        if(y>0&&(!ground.getFluidState().isEmpty()||ground.is(Blocks.FIRE)||ground.is(Blocks.SOUL_FIRE))){point.candidate=null;release(chunkPos);return false;}
        point.position=new Vec3(Math.floor(point.candidate.x)+.5,y,Math.floor(point.candidate.z)+.5);point.candidate=null;
        if(point.body!=null&&!point.body.isRemoved())point.body.setPos(point.position);
        release(chunkPos);return true;
    }
    private void release(ChunkPos chunk){if(loading.remove(chunk)!=null)level.getChunkSource().removeTicketWithRadius(TICKET,chunk,0);}
    private void releaseTickets(){for(var chunk:List.copyOf(loading.keySet()))release(chunk);}
    private boolean unsupported(Point point){if(point.position.y<=0)return true;var pos=BlockPos.containing(point.position.add(0,-.6,0));
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk!=null&&chunk.getBlockState(pos).is(AIRS_ONLY);}
    public void tick(){
        int tick=game.server().getTickCount();if(lastTick==tick)return;lastTick=tick;
        if(!running())return;
        var beneficiary=owner==null?null:game.server().getPlayerList().getPlayer(owner);
        if(beneficiary!=null&&phase!=Phase.IDLE&&!ShadowAbsorption.state(beneficiary).entries.containsKey(30))absorptionBaseline=null;
        if(beneficiary!=null&&!game.items().timeStop().frozen(beneficiary)&&absorptionBaseline!=null&&!absorptionBaseline.isInfiniteDuration()){
            int remaining=absorptionBaseline.getDuration()-1;absorptionBaseline=remaining<=0?null:new MobEffectInstance(MobEffects.ABSORPTION,remaining,30,absorptionBaseline.isAmbient(),absorptionBaseline.isVisible(),absorptionBaseline.showIcon());
        }
        if(phase==Phase.IDLE){if(!acceptNewWork.getAsBoolean())return;
            for(var id:List.copyOf(craftDirty)){var player=game.server().getPlayerList().getPlayer(id);
                if(player==null){craftDirty.remove(id);continue;}if(!preflight(player))continue;
                craftDirty.remove(id);inventory(player);if(phase!=Phase.IDLE)return;}
            if(++clock%20==0)dropped();return;
        }
        clock++;
        if(phase==Phase.MAP&&pendingCompletion!=null){finish(pendingCompletion);return;}
        if(phase==Phase.RUNNING){
            if(!acceptNewWork.getAsBoolean())return;
            boolean settled=true;
            for(var point:points){if(!scatter(point)){settled=false;continue;}
                if(!unsupported(point)){clones.request(level,point.position,cause);lightning(point.position);dust(point.position,0xffffff,50,.5,20,.5,1);}
                lightning(point.position);
            }
            if(settled&&points.stream().allMatch(this::unsupported)){phase=Phase.DRAINING;releaseTickets();removeBodies();}
        }
        if(phase==Phase.DRAINING&&clones.captureReady()){phase=Phase.DELAY;delay=0;actions.preparingMap(event,owner,cause);}
        else if(phase==Phase.DELAY&&++delay>=60){phase=Phase.MAP;mapIntent=true;actions.replaceMap(event,owner,cause);}
    }
    public void completed(UUID id,boolean success){if(phase!=Phase.MAP||!Objects.equals(event,id))return;pendingCompletion=success;if(running())finish(success);}
    private void benefits(){
        var player=owner==null?null:game.server().getPlayerList().getPlayer(owner);
        if(player==null){var lease=deferredBenefits.get(owner);
            deferredBenefits.put(owner,new BenefitLease(lease==null?null:OwnedEffectLayers.copyCheckpoint(lease.effects()),absorptionBaseline==null?null:EffectLayers.copy(absorptionBaseline),true));return;}
        joined(player);withdrawBenefits(player,absorptionBaseline);
    }
    private void withdrawBenefits(ServerPlayer player,MobEffectInstance baseline){
        game.items().ownedEffects().withdraw(player,BENEFIT);
        var state=ShadowAbsorption.state(player);state.entries.remove(30);
        if(baseline!=null)state.entries.put(30,EffectLayers.copy(baseline));
        ShadowAbsorption.reconcile(player,false);state.internal=true;
        try{if(state.entries.isEmpty())player.removeEffect(MobEffects.ABSORPTION);else player.forceAddEffect(new MobEffectInstance(state.entries.values().iterator().next()),null);}finally{state.internal=false;}
    }
    private void removeBodies(){if(root!=null){if(core!=null&&!core.isRemoved())core.discard();else retired.add(root);}core=null;
        for(var p:points){if(p.body!=null&&!p.body.isRemoved())p.body.discard();else retired.add(p.id);p.body=null;}}
    private void finish(boolean success){var id=event;var recipient=owner;removeBodies();releaseTickets();benefits();drops(success||blockDrops,success||entityDrops,success||mobDrops);
        phase=Phase.IDLE;points.clear();event=owner=root=null;cause=null;origin=null;level=null;absorptionBaseline=null;pendingCompletion=null;mapIntent=false;actions.finished(id,recipient,success);}
    public void stop(){clones.stop();if(phase!=Phase.IDLE)finish(false);releaseTickets();craftDirty.clear();katanas.clear();restoring=false;}
    /** Capture only waits for accepted atomic terrain writes, never for the whole event. */
    public boolean captureReady(){return clones.captureReady()&&loading.isEmpty()&&phase!=Phase.MAP;}
    public void settle(){if(!acceptNewWork.getAsBoolean()){releaseTickets();for(var p:points)p.candidate=null;}}
    public void prepareFrozen(){releaseTickets();points.clear();katanas.clear();craftDirty.clear();retired.clear();deferredBenefits.clear();core=null;restoring=true;}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(){if(!captureReady())throw new IllegalStateException("Drain accepted Supernova terrain before capture");return new Checkpoint();}
    final class Checkpoint {
        final Phase savedPhase=phase;final UUID savedEvent=event,savedOwner=owner,savedRoot=root;
        final ServerLevel savedLevel=level;final Vec3 savedOrigin=origin;final LbcIndirectKills.Owner savedCause=cause;
        final int savedClock=clock,savedDelay=delay;final boolean savedIntent=mapIntent,savedBlock=blockDrops,savedEntity=entityDrops,savedMob=mobDrops,rootLoaded=core!=null&&!core.isRemoved();
        final MobEffectInstance baseline=absorptionBaseline==null?null:EffectLayers.copy(absorptionBaseline);
        final List<PointImage> actors=points.stream().map(p->new PointImage(p.id,p.position,p.body!=null&&!p.body.isRemoved())).toList();
        final List<UUID> dropped=katanas.values().stream().filter(i->!i.isRemoved()).map(Entity::getUUID).toList(),dirty=List.copyOf(craftDirty);
        final Set<UUID> cancelled=Set.copyOf(retired);
        final Map<UUID,BenefitLease> leases=new LinkedHashMap<>();
        { deferredBenefits.forEach((id,lease)->leases.put(id,lease.copy())); }
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var missing=new LinkedHashSet<UUID>();var restored=new ArrayList<Point>();
            Marker body=savedRoot!=null&&lookup.apply(savedRoot) instanceof Marker m?m:null;if(rootLoaded&&body==null)missing.add(savedRoot);
            for(var image:actors){var p=new Point(image.id(),image.position());if(lookup.apply(p.id) instanceof Marker m)p.body=m;else if(image.loaded())missing.add(p.id);restored.add(p);}
            var items=new LinkedHashMap<UUID,ItemEntity>();for(var id:dropped)if(lookup.apply(id) instanceof ItemEntity item)items.put(id,item);else missing.add(id);
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{phase=savedPhase;event=savedEvent;owner=savedOwner;root=savedRoot;level=savedLevel;origin=savedOrigin;cause=savedCause;clock=savedClock;delay=savedDelay;mapIntent=savedIntent;blockDrops=savedBlock;entityDrops=savedEntity;mobDrops=savedMob;core=body;points.clear();points.addAll(restored);katanas.clear();katanas.putAll(items);craftDirty.clear();craftDirty.addAll(dirty);retired.clear();retired.addAll(cancelled);deferredBenefits.clear();leases.forEach((id,lease)->deferredBenefits.put(id,lease.copy()));absorptionBaseline=baseline==null?null:EffectLayers.copy(baseline);});
        }
    }
}
