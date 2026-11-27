package me.skywars2.game.items.weapons.ranged;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

import me.skywars2.game.GameController;
import me.skywars2.game.academy.LbcAcademy;
import me.skywars2.game.combat.CrossbowBlastBlocks;
import me.skywars2.game.combat.LbcBeamAmmo;
import me.skywars2.game.combat.LbcDamage;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.effects.ItemEffectLevels;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.entities.drones.LbcFpvDrone;
import me.skywars2.game.items.gadgets.LbcModeScroll;
import me.skywars2.game.items.loot.LbcLuckyBlocks;
import me.skywars2.game.items.wands.LbcStonekivi;
import me.skywars2.game.items.wands.LbcTerra;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.match.checkpoints.MatchAuthorityCheckpoint;
import me.skywars2.game.match.checkpoints.MatchQueuedWorkCheckpoint;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.presentation.projectiles.LbcParticleBatch;
import me.skywars2.game.presentation.projectiles.LbcParticleBatch.Group;
import me.skywars2.game.presentation.projectiles.LbcParticleBatch.NearbyRuns;
import me.skywars2.game.skills.clone.CloneVirtualAbilityStates;
import me.skywars2.game.world.maps.NativeChunkLoad;
import me.skywars2.game.effects.magic.LbcMana;
import me.skywars2.game.items.drops.LbcFamilyDrops;

/** Higher electricity shares Electro traces and the terrain work queue. */
public final class LbcErecto {
    public static final int CENTRAL=0,SIDE=1,PHANTOM=2,SNIPER=3,CROSSBOW=4;
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> CONDUCTIVE=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:electroprovod"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final DustColorTransitionOptions DUST=new DustColorTransitionOptions(0x00ccff,0x003333,.8f);
    private static final DustColorTransitionOptions SNIPER_DUST=new DustColorTransitionOptions(0x00ccff,0xe5e5ff,.8f);
    private static final DustColorTransitionOptions CROSSBOW_DUST=new DustColorTransitionOptions(0xffccff,0xccccff,.9f);
    private static final int CROSSBOW_BLAST_LIMIT=8;
    private static final int CROSSBOW_PATHS_PER_PLAYER=128,CROSSBOW_PATHS_TOTAL=512;
    // Backpressure starts here; paths already in flight can all still impact.
    private static final int CROSSBOW_JOB_WATERMARK_PER_PLAYER=64,CROSSBOW_JOB_WATERMARK_TOTAL=256;
    private static final long CROSSBOW_BLAST_BUDGET_NANOS=4_000_000;
    private static final int CROSSBOW_BLAST_RADIUS=20;
    private static final int CROSSBOW_BLAST_REACH=2*CROSSBOW_BLAST_RADIUS+2;
    /** Merge a tick of sniper effects without altering any of the sampled paths. */
    public static final class SniperEffects implements AutoCloseable {
        private static final Holder<SoundEvent> RIPTIDE=Holder.direct(
                SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("item.trident.riptide_3")));
        private static final float VOLUME=.1f;
        private static final double SOUND_RANGE_SQUARED=Math.pow(RIPTIDE.value().getRange(VOLUME),2);
        private final Map<ServerLevel,WorldEffects> worlds=new LinkedHashMap<>();

        private record SoundSample(Vec3 point,long seed,double distanceSquared){}
        private static final class Listener {
            final ServerPlayer player;
            final Vec3 viewpoint;
            final Map<ServerPlayer,SoundSample> nearest=new LinkedHashMap<>();

            Listener(ServerPlayer player){
                this.player=player;
                viewpoint=LbcFpvDrone.viewpoint(player);
            }

            void offer(ServerPlayer owner,Vec3 point,long seed){
                double distance=viewpoint.distanceToSqr(point);
                if(distance>SOUND_RANGE_SQUARED)return;
                var current=nearest.get(owner);
                if(current==null||distance<current.distanceSquared)nearest.put(owner,new SoundSample(point,seed,distance));
            }
        }
        private static final class WorldEffects {
            final ServerLevel level;
            final Map<BlockPos,LbcParticleBatch.Group> souls=new LinkedHashMap<>();
            final Map<SuperVolley,LbcParticleBatch.NearbyRuns> superDust=new LinkedHashMap<>();
            final ArrayList<Listener> listeners=new ArrayList<>();

            WorldEffects(ServerLevel level){
                this.level=level;
                for(var player:level.getServer().getPlayerList().getPlayers())
                    if(player.level()==level)listeners.add(new Listener(player));
            }
        }

        void sound(ServerLevel level,ServerPlayer owner,Vec3 point,long seed){
            for(var listener:worlds.computeIfAbsent(level,WorldEffects::new).listeners)listener.offer(owner,point,seed);
        }

        void soul(ServerLevel level,Vec3 point){
            // A half-block cell bounds the variance of overlapping Gaussian clouds.
            var cell=BlockPos.containing(point.x*2,point.y*2,point.z*2);
            var world=worlds.computeIfAbsent(level,WorldEffects::new);
            world.souls.computeIfAbsent(cell,key->new LbcParticleBatch.Group(level,
                    ParticleTypes.SCULK_SOUL,.1,0,true,.87,8)).add(point,3);
        }

        LbcParticleBatch.NearbyRuns superDust(ServerLevel level,SuperVolley volley){
            return worlds.computeIfAbsent(level,WorldEffects::new).superDust.computeIfAbsent(volley,
                    key->new LbcParticleBatch.NearbyRuns(level,CROSSBOW_DUST,.1,true,.25));
        }

        @Override public void close(){
            for(var world:worlds.values()){
                for(var dust:world.superDust.values())dust.close();
                for(var souls:world.souls.values())souls.close();
                for(var listener:world.listeners){
                    if(listener.player.level()!=world.level||listener.player.isRemoved())continue;
                    for(var sample:listener.nearest.values()){
                        var point=sample.point;
                        listener.player.connection.send(new ClientboundSoundPacket(RIPTIDE,
                                SoundSource.MASTER,point.x,point.y,point.z,VOLUME,2,sample.seed));
                    }
                }
            }
        }
    }

    public static final class SuperVolley {}

    public static final class Path {
        public final int kind,focusSteps;public final ServerPlayer caster;public final InteractionHand hand;public final ItemStack stack;
        public Vec3 origin,focus;public int side;public LbcIndirectKills.Owner terrainCause;public LbcBeamAmmo ammunition;
        public CrossbowBlasts pendingBlasts;
        public SuperVolley superVolley;
        public Path(int kind,Vec3 origin,Vec3 focus,int focusSteps,int side,ServerPlayer caster,InteractionHand hand,ItemStack stack){
            this.kind=kind;this.origin=origin;this.focus=focus;this.focusSteps=focusSteps;this.side=side;this.caster=caster;this.hand=hand;this.stack=stack;
        }
    }
    public static final class Sphere {
        public final ServerPlayer owner;final ServerLevel level;public Vec3 position;public final String epoch;public int age;
        final LbcIndirectKills.Owner cause;
        int cloneMask;
        Sphere(ServerPlayer owner,Vec3 position,String epoch,LbcIndirectKills.Owner cause){this.owner=owner;level=owner.level();this.position=position;this.epoch=epoch;this.cause=cause;}
    }
    private final GameController game;
    private final Set<Sphere> spheres=new LinkedHashSet<>();
    private final Map<ServerPlayer,String> rays=new IdentityHashMap<>();
    private final Map<ServerPlayer,LbcIndirectKills.Owner> rayCauses=new IdentityHashMap<>();
    private static final Identifier SNIPER_WINDUP=Identifier.parse("skywars2:erecto_sniper_windup");
    private final Map<ServerPlayer,Integer> sniperWindups=new IdentityHashMap<>();
    private int blastTick=Integer.MIN_VALUE,blastsThisTick;
    private long blastNanos;
    private final Set<CrossbowBlasts> crossbowWork=new HashSet<>();
    private final NativeChunkLoad.ColdRequests crossbowColdRequests=new NativeChunkLoad.ColdRequests(8,32);
    private final Map<UUID,Integer> firingThisTick=new HashMap<>();
    private int firingTick=Integer.MIN_VALUE,pathsFiredThisTick;
    public LbcErecto(GameController game){this.game=game;}
    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int mode=Mth.clamp(data.getIntOr("mode",1),1,4);
        if(academy&&owner.isShiftKeyDown()){
            mode=LbcModeScroll.next(mode,4);data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();
            owner.sendSystemMessage(Component.translatable(switch(mode){case 2->"mode_ray";case 3->"mode_sphere";case 4->"mode_sniper";default->"mode_default";}));return true;
        }
        var mana=game.items().mana();int cost=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("erecto_wand_mana_use"),"lbc.math").get();
        if(!mana.trySpendWand(owner,cost))return false;
        if(!academy||mode!=4)castMode(owner,hand,stack,academy?mode:1);
        else if(owner.getXRot()>=89){
            // Looking straight down is source aim assistance, not a paid sniper discharge.
            mana.set(owner,mana.get(owner)+cost);LivingEntity farthest=null;double distance=-1;
            for(var entity:owner.level().getAllEntities())if(entity!=owner&&entity instanceof LivingEntity living
                    &&entity.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().target(owner,entity)){
                double d=entity.distanceToSqr(owner);if(d>distance){distance=d;farthest=living;}
            }
            if(farthest!=null)rotate(owner,farthest.getEyePosition().subtract(owner.getEyePosition()),0);
        }else{
            mana.set(owner,mana.get(owner)-3900);sniper(owner,hand,stack);
        }
        game.items().wands().wandCasted(owner);return true;
    }
    public void castMode(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode) {
        switch(mode) {
            case 1 -> volley(owner,hand,stack);
            case 2 -> {game.items().mana().score(owner,"ray").add(40);rays.put(owner,game.items().epoch());rayCauses.put(owner,me.skywars2.game.combat.WeaponEnchantments.effectCause(owner));}
            case 3 -> {var hit=first(owner,150,true);if(hit!=null)spheres.add(new Sphere(owner,hit.getLocation().subtract(owner.getLookAngle()),game.items().epoch(),me.skywars2.game.combat.WeaponEnchantments.effectCause(owner)));}
            case 4 -> sniper(owner,hand,stack);
            default -> throw new IllegalArgumentException("Erecto mode "+mode);
        }
    }
    public void emitAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        switch(mode){
            case 1->volleyAt(owner,hand,ItemStack.EMPTY,origin,direction);
            case 2->rayAt(owner,origin,direction);
            case 3->{var hit=firstAt(owner,origin,direction,150,true);if(hit!=null)spheres.add(new Sphere(owner,hit.getLocation().subtract(direction),game.items().epoch(),me.skywars2.game.combat.WeaponEnchantments.effectCause(owner)));}
            case 4->{
                add(owner,origin,yaw,pitch,200,new Path(SNIPER,origin,null,0,0,owner,hand,ItemStack.EMPTY));
                LbcOverload.sound(owner,origin,"minecraft:item.trident.return",3,1.3f,0);
                owner.level().sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xff00ffff),true,false,origin.x,origin.y,origin.z,1,0,0,0,0);
            }
            default->throw new IllegalArgumentException("Erecto producer mode "+mode);
        }
    }
    void phantomBurst(ServerPlayer owner,Vec3 origin,LbcIndirectKills.Owner cause){
        for(int i=0;i<16;i++){
            var trace=new LbcWands.ElectroTrace(owner,origin,game.items().epoch(),8);
            trace.yaw=owner.getRandom().nextFloat()*360-180;trace.pitch=owner.getRandom().nextFloat()*180-90;trace.cause=cause;
            trace.erecto=new Path(PHANTOM,origin,null,0,0,owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            game.items().wands().higherElectro(trace);
        }
    }
    private void sniper(ServerPlayer owner,InteractionHand hand,ItemStack stack) {
        add(owner,owner.getEyePosition(),owner.getYRot(),owner.getXRot(),200,
                    new Path(SNIPER,owner.getEyePosition(),null,0,0,owner,hand,stack));
            // Source Slowness CI immobilizes for one second. The uncapped attribute
            // contract would turn its -1515% modifier into negative movement speed.
            // Multiply the current speed by zero without changing unrelated effects.
            owner.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(
                    new AttributeModifier(SNIPER_WINDUP,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            sniperWindups.put(owner,20);
            rotate(owner,owner.getLookAngle(),-15);LbcOverload.sound(owner,owner.getEyePosition(),"minecraft:item.trident.return",3,1.3f,0);
            owner.level().sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xff00ffff),true,false,owner.getEyePosition().x,owner.getEyePosition().y,owner.getEyePosition().z,1,0,0,0,0);
    }
    private static void rotate(ServerPlayer owner,Vec3 direction,float recoil){
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));float pitch=Mth.clamp((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()))+recoil,-90,90);
        owner.forceSetRotation(yaw,false,pitch,false);
    }
    /** Shared resident DDA and native entity boxes resolve a cast-local focus without allocating a marker. */
    private HitResult first(ServerPlayer owner,double range,boolean entities){return firstAt(owner,owner.getEyePosition(),owner.getLookAngle(),range,entities);}
    private HitResult firstAt(ServerPlayer owner,Vec3 from,Vec3 direction,double range,boolean entities){
        Vec3 to=from.add(direction.scale(range));var wall=LbcTerra.wallHit(owner.level(),from,to);
        if(wall!=null)to=wall.getLocation();
        var entity=entities?ProjectileUtil.getEntityHitResult(owner,from,to,new AABB(from,to).inflate(1),e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,e),from.distanceToSqr(to)):null;
        if(entity!=null)return entity;
        return wall!=null&&owner.level().hasChunkAt(BlockPos.containing(wall.getLocation()))?wall:null;
    }
    private void volley(ServerPlayer owner,InteractionHand hand,ItemStack stack){volleyAt(owner,hand,stack,owner.getEyePosition(),owner.getLookAngle());}
    private void volleyAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,Vec3 origin,Vec3 direction){
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        var hit=firstAt(owner,origin,direction,200,true);Vec3 focus=hit==null?null:hit.getLocation();int steps=hit==null?401:Math.min(401,1+Mth.ceil(origin.distanceTo(focus)*2));
        add(owner,origin,yaw,pitch,200,new Path(CENTRAL,origin,focus,steps,0,owner,hand,stack));
        for(int side:new int[]{1,-1,2,-2,3,-3,4,-4})add(owner,origin,yaw+side*10,pitch,200,new Path(SIDE,origin,focus,steps,side,owner,hand,stack));
    }
    private void add(ServerPlayer owner,Vec3 at,float yaw,float pitch,int lifetime,Path path){
        add(owner,at,yaw,pitch,lifetime,path,me.skywars2.game.combat.WeaponEnchantments.effectCause(owner));
    }
    private void add(ServerPlayer owner,Vec3 at,float yaw,float pitch,int lifetime,Path path,LbcIndirectKills.Owner cause){
        var trace=new LbcWands.ElectroTrace(owner,at,game.items().epoch(),lifetime);trace.yaw=yaw;trace.pitch=pitch;trace.erecto=path;trace.cause=cause;game.items().wands().higherElectro(trace);
    }
    /**
     * Includes jobs whose trace has retired. Per-tick debits cover launches whose
     * TMT continuation has yet to publish its paths, including nested shots.
     */
    public boolean admitCrossbow(ServerPlayer owner,int count,boolean reserve){
        int tick=game.server().getTickCount();
        if(firingTick!=tick){
            firingTick=tick;
            firingThisTick.clear();
            pathsFiredThisTick=0;
        }
        var id=owner.getUUID();
        int total=pathsFiredThisTick,personal=firingThisTick.getOrDefault(id,0);
        var wands=game.items().wands();
        for(var trace:wands.activeElectroTraces())if(trace.erecto!=null&&trace.erecto.kind==CROSSBOW){
            total++;
            if(trace.erecto.caster.getUUID().equals(id))personal++;
        }
        int jobs=0,personalJobs=0;
        for(var work:crossbowWork)if(!work.retired()){
            boolean sameOwner=work.path.caster.getUUID().equals(id);
            jobs++;
            if(sameOwner)personalJobs++;
            if(!wands.hasElectroTrace(work.trace)){
                total++;
                if(sameOwner)personal++;
            }
        }
        if(total+count>CROSSBOW_PATHS_TOTAL||personal+count>CROSSBOW_PATHS_PER_PLAYER
                ||jobs>=CROSSBOW_JOB_WATERMARK_TOTAL||personalJobs>=CROSSBOW_JOB_WATERMARK_PER_PLAYER)return false;
        if(reserve){
            firingThisTick.merge(id,count,Integer::sum);
            pathsFiredThisTick+=count;
        }
        return true;
    }

    void releaseShotPaths(ServerPlayer owner,int count){
        if(firingTick!=game.server().getTickCount())return;
        var id=owner.getUUID();
        int reserved=firingThisTick.getOrDefault(id,0);
        int released=Math.min(reserved,count);
        if(reserved==released)firingThisTick.remove(id);
        else firingThisTick.put(id,reserved-released);
        pathsFiredThisTick-=released;
    }

    /** The source mechanical sniper has its own impact payload; wand sniper behavior stays shared above. */
    void crossbow(ServerPlayer owner,ItemStack weapon,List<net.minecraft.world.entity.projectile.Projectile> family,LbcImbaCrossbows.Kind kind){
        int count=kind.beamPaths();
        var superVolley=kind==LbcImbaCrossbows.Kind.SUPER?new SuperVolley():null;
        var origin=kind==LbcImbaCrossbows.Kind.GATLING?owner.getEyePosition().add(owner.getLookAngle()):owner.position();
        for(int i=0;i<count;i++){
            float yaw=owner.getYRot(),pitch=owner.getXRot();
            if(count>1){yaw+=(owner.getRandom().nextInt(6001)-3000)*.01f;pitch+=(owner.getRandom().nextInt(kind==LbcImbaCrossbows.Kind.GATLING?1001:6001)-(kind==LbcImbaCrossbows.Kind.GATLING?500:3000))*.01f;}
            var path=new Path(CROSSBOW,origin,null,0,0,owner,InteractionHand.MAIN_HAND,weapon.copy());
            path.superVolley=superVolley;
            path.ammunition=(LbcBeamAmmo)LbcBeamAmmo.capture(game,owner,weapon,family);if(path.ammunition!=null)path.ammunition.precise=true;
            var trace=new LbcWands.ElectroTrace(owner,origin,game.items().epoch(),count==1?200:count==32?50:(i+1)*10);
            trace.yaw=yaw;trace.pitch=pitch;trace.erecto=path;trace.cause=game.items().indirectKills().coldCause(owner,true);
            game.items().wands().higherElectro(trace);
        }
        releaseShotPaths(owner,count);
        LbcOverload.sound(owner,owner.position(),"minecraft:item.trident.return",3,1.3f,0);
        owner.level().sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xff00ffff),true,false,origin.x,origin.y,origin.z,1,0,0,0,0);
        if(kind==LbcImbaCrossbows.Kind.SNIPER)rotate(owner,owner.getLookAngle(),-15);
    }
    public boolean step(LbcWands.ElectroTrace trace){
        try(var effects=new SniperEffects()){return step(trace,effects);}
    }
    public boolean step(LbcWands.ElectroTrace trace,SniperEffects effects){
        Path p=trace.erecto;if(p.kind==SNIPER||p.kind==CROSSBOW)return sniper(trace,effects);
        double spacing=p.kind==SIDE?.25:p.kind==PHANTOM?.2:.5;int samples=p.kind==SIDE?9:p.kind==PHANTOM?10:4;
        Vec3 from=trace.position,direction=Vec3.directionFromRotation(trace.pitch,trace.yaw),end=from;boolean clear=true;
        var shield=p.kind==PHANTOM?game.items().wands().shieldHit(trace.owner,from,from.add(direction.scale(samples*spacing))):null;
        double shieldDistance=shield==null?Double.POSITIVE_INFINITY:from.distanceTo(shield.getLocation());boolean emitted=false;
        double mechanismDistance=game.items().magic().mechanismSuppressionFraction(trace.level,from,from.add(direction.scale(samples*spacing)))*samples*spacing;
        boolean jammed=false;
        BlockPos struckBlock=null;
        int batchSize=game.balanceLegacy()?1:3;
        try (var dust = new LbcParticleBatch.Group(trace.level,DUST,.1,.1,.1,0,true,.55,batchSize);
             var spark = new LbcParticleBatch.Group(trace.level,ParticleTypes.ELECTRIC_SPARK,.1,.1,.1,1,false,.55,batchSize)) {
            for(int i=0;i<=samples;i++){
                Vec3 point=from.add(direction.scale(i*spacing));if(i*spacing>=Math.min(mechanismDistance,shieldDistance)){
                    jammed=mechanismDistance<=shieldDistance;end=jammed?from.add(direction.scale(mechanismDistance)):shield.getLocation();clear=false;break;
                }
                var pos=BlockPos.containing(point);if(!trace.level.hasChunkAt(pos)){clear=false;break;}
                emitted=true;end=point;
                if(p.kind!=PHANTOM||trace.level.getRandom().nextFloat()<.2f){
                    dust.add(point);
                    spark.add(point);
                    LbcOverload.sound(trace.owner,point,"minecraft:entity.firework_rocket.twinkle",p.kind==CENTRAL?.03f:.01f,2,0);
                }
                var state=trace.level.getBlockState(pos);
                if (p.kind == PHANTOM ? LbcLuckyBlocks.kind(state) == LbcLuckyBlocks.Kind.LOLI : !state.is(AIRS)) {
                    struckBlock=pos;clear=false;break;
                }
            }
        }
        var contact = game.items().familyDrops().reactToArc(trace.level, from, end, struckBlock, trace.owner);
        if (contact != null) {
            hurt(trace, from, contact, spacing, p.kind==PHANTOM?.5f:1, 2);
            trace.position=contact;
            return false;
        }
        if(emitted)hurt(trace,from,end,spacing,p.kind==PHANTOM?.5f:1,2);
        trace.position=end;
        if(!clear){if(!jammed&&(shield==null||from.distanceTo(end)<shieldDistance-1e-6))impact(trace,end);return false;}
        if(p.kind==SIDE&&p.side!=0){
            if(trace.age+1>p.focusSteps/7){
                if(p.focus!=null)aim(trace,p.focus.subtract(end));p.side=0;
            }else{int bend=((400-p.focusSteps)/11)*3;trace.yaw-=Math.signum(p.side)*Math.abs(p.side)*bend*bend*.0001f;}
        }
        if(p.kind==PHANTOM){
            trace.yaw+=(trace.level.getRandom().nextInt(1601)-800)*.01f;trace.pitch+=(trace.level.getRandom().nextInt(1601)-800)*.01f;
            if(trace.level.getBlockState(BlockPos.containing(end)).is(CONDUCTIVE))game.items().wands().teslaConduction(trace,end);
        }
        return true;
    }
    private static void aim(LbcWands.ElectroTrace t,Vec3 direction){t.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));t.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));}
    private void hurt(LbcWands.ElectroTrace t,Vec3 from,Vec3 to,double spacing,float amount,int limit){
        var damage=game.items().indirectKills().captureDamage(new DamageSource(t.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),t.owner),t.cause);
        LbcDamage.hurtArc(t.level,damage,from,to,spacing,amount,limit,e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(t.owner,e)
                && (t.erecto.kind!=CENTRAL&&t.erecto.kind!=SNIPER||!e.entityTags().contains("transformator")),false,t.erecto.kind==PHANTOM,t.owner,e->{
            var p=t.erecto;
            if(p.kind==SIDE&&t.owner==p.caster&&e.position().distanceToSqr(p.origin)>4900&&LbcAcademy.trial(p.stack))game.items().academy().ready(p.caster,p.hand,p.stack);
        });
    }
    private void impact(LbcWands.ElectroTrace trace,Vec3 point){
        var level=trace.level;if(!level.hasChunkAt(BlockPos.containing(point)))return;
        Vec3 particles=point.subtract(Vec3.directionFromRotation(trace.pitch,trace.yaw).scale(.5));level.sendParticles(ParticleTypes.END_ROD,particles.x,particles.y,particles.z,25,.1,.1,.1,.1);
        if(!level.getBlockState(BlockPos.containing(point)).is(CONDUCTIVE))return;
        var damage=game.items().indirectKills().captureDamage(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_CONDUCTION),trace.owner),trace.cause);
        for(var e:level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(15),e->e.position().distanceToSqr(point)<=225&&game.items().wands().projectileTarget(trace.owner,e)&&!e.entityTags().contains("transformator"))){
            e.hurtServer(level,damage,8);level.sendParticles(ParticleTypes.END_ROD,e.getX(),e.getY()+1,e.getZ(),3,.3,.6,.3,.2);
        }
    }
    private boolean sniper(LbcWands.ElectroTrace trace,SniperEffects effects){
        boolean crossbow=trace.erecto.kind==CROSSBOW;
        if(crossbow&&trace.erecto.pendingBlasts!=null)return true;
        Vec3 from=trace.position,direction=Vec3.directionFromRotation(trace.pitch,trace.yaw),end=from;boolean alive=true,emitted=false;
        double mechanismDistance=game.items().magic().mechanismSuppressionFraction(trace.level,from,from.add(direction.scale(18)))*18;
        var family=game.items().familyDrops();
        Vec3 familyLimit=from.add(direction.scale(Math.min(18,mechanismDistance)));
        var loli=LbcTerra.wallHit(trace.level,from,familyLimit,state->LbcLuckyBlocks.kind(state)==LbcLuckyBlocks.Kind.LOLI);
        var familyHit=family.firstArcContact(trace.level,from,loli==null?familyLimit:loli.getLocation(),loli==null?null:loli.getBlockPos());
        double familyDistance=familyHit==null?Double.POSITIVE_INFINITY:from.distanceTo(familyHit.position())/direction.length();
        var particle=crossbow?CROSSBOW_DUST:SNIPER_DUST;
        double yawRadians=Math.toRadians(trace.yaw);
        var right=new Vec3(Math.cos(yawRadians),0,Math.sin(yawRadians));
        ArrayList<Vec3> centers=null;
        boolean legacy=game.balanceLegacy();
        var sharedDust=trace.age==0&&trace.erecto.superVolley!=null&&!legacy
                ?effects.superDust(trace.level,trace.erecto.superVolley):null;
        // Only the first six triplets of a volley are close enough to benefit.
        var dustRun=sharedDust==null?null:new Vec3[3];
        int dustPoints=0;
        try (var dust = new LbcParticleBatch.Group(trace.level,particle,.1,.1,.1,0,true,.31,legacy?1:3)) {
            for(int i=0;i<=60;i++){
                if(familyDistance<mechanismDistance-1e-7&&i*.3+1e-7>=familyDistance){
                    end=familyHit.position();alive=false;emitted=true;
                    family.reactToArc(trace.level,familyHit,trace.owner,true);
                    break;
                }
                if(i*.3>=mechanismDistance){end=from.add(direction.scale(mechanismDistance));alive=false;break;}
                Vec3 point=from.add(direction.scale(i*.3));var pos=BlockPos.containing(point);if(!trace.level.hasChunkAt(pos)){alive=false;break;}
                emitted=true;end=point;
                if(sharedDust!=null&&i<18){
                    dustRun[dustPoints++]=point;
                    if(dustPoints==dustRun.length){
                        sharedDust.add(dustRun,dustPoints);
                        dustPoints=0;
                    }
                }else dust.add(point);
                // Keep both original RNG draws in the trace loop, even when the
                // sample has no listener or is represented by a nearer sound.
                if(trace.level.getRandom().nextBoolean())
                    effects.sound(trace.level,trace.owner,point,trace.owner.getRandom().nextLong());
                if((i+1)%12==0){
                    effects.soul(trace.level,point.subtract(right));
                    effects.soul(trace.level,point.add(right));
                }
                if(!trace.level.getBlockState(pos).is(AIRS)){
                    if(crossbow){
                        if(trace.erecto.ammunition!=null)trace.erecto.ammunition.blockContact(trace.level,point,trace.cause);
                        if(centers==null)centers=new ArrayList<>();
                        centers.add(point);
                        continue;
                    }
                    impact(trace,point);var p=trace.erecto;
                    if(p.terrainCause==null||!p.terrainCause.player().equals(trace.owner.getUUID()))p.terrainCause=game.items().indirectKills().coldCause(trace.owner,true);
                    game.items().indirectKills().trapChanges(trace.level,p.terrainCause,()->{
                        for(var block:BlockPos.betweenClosed(BlockPos.containing(point.add(-.5,-.5,-.5)),BlockPos.containing(point.add(.5,.5,.5))))
                            if(trace.level.hasChunkAt(block)&&!trace.level.getBlockState(block).isAir())trace.level.destroyBlock(block,true,trace.owner);
                    });
                    if(trace.level.getRandom().nextFloat()<.2f){alive=false;break;}
                }
            }
        }
        if(dustPoints>0)sharedDust.add(dustRun,dustPoints);
        if(emitted&&crossbow){
            if(centers!=null){
                var work=new CrossbowBlasts(trace,centers);
                trace.erecto.pendingBlasts=work;
                crossbowWork.add(work);
                game.items().stonekivi().queuedWork(work);
            }
            if(trace.erecto.ammunition!=null)trace.erecto.ammunition.lineSegment(trace.level,from,end,trace.cause);
            var damage=game.items().indirectKills().captureDamage(new DamageSource(trace.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.ARROW),trace.owner),trace.cause);
            LbcDamage.hurtPiercingBeam(trace.level,damage,from,end,.3,20,2,
                    e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(trace.owner,e)
                            &&!e.entityTags().contains("transformator"),trace.owner);
        }else if(emitted)hurt(trace,from,end,.3,20,2);trace.position=end;return alive;
    }

    private boolean crossbowBlastBudget(int tick){
        if(blastTick!=tick){
            blastTick=tick;
            blastsThisTick=0;
            blastNanos=0;
        }
        return blastsThisTick<CROSSBOW_BLAST_LIMIT&&blastNanos<CROSSBOW_BLAST_BUDGET_NANOS;
    }

    /** Legacy schedules the whole segment before its first blast can change the sampled terrain. */
    public final class CrossbowBlasts implements LbcStonekivi.Work {
        public final LbcWands.ElectroTrace trace;
        public final Path path;
        public final List<Vec3> centers;
        public final ServerLevel level;
        final String epoch;
        final DamageSource damage;
        final LbcIndirectKills.Owner cause;
        public final Vec3 footprint;
        public final int reach;
        NativeChunkLoad.Leases.Area lease;
        public int next;
        int waitingTick=Integer.MIN_VALUE;
        boolean resident;

        CrossbowBlasts(LbcWands.ElectroTrace trace,List<Vec3> samples){
            this.trace=trace;
            path=trace.erecto;
            centers=List.copyOf(samples);
            level=trace.level;
            epoch=trace.epoch;
            cause=trace.cause;
            damage=game.items().indirectKills().captureDamage(level.damageSources().explosion(null,trace.owner),cause);
            var first=centers.getFirst();var last=centers.getLast();
            footprint=first.lerp(last,.5);
            reach=CROSSBOW_BLAST_REACH+(int)Math.ceil(Math.max(Math.abs(last.x-first.x),Math.abs(last.z-first.z))*.5);
        }

        private boolean retired(){return path.pendingBlasts!=this||!game.items().currentEpoch(epoch)||!game.playing();}
        @Override public boolean usesArenaClock(){return false;}
        @Override public boolean ready(){
            if(retired())return true;
            int tick=game.server().getTickCount();
            if(waitingTick==tick||!crossbowBlastBudget(tick)||game.items().timeStop().stopped(level))return false;
            return !(damage.getEntity() instanceof ServerPlayer player&&!player.isRemoved()
                    &&game.items().wands().personalTimeStopped(player));
        }
        @Override public boolean snapshotIdle(){return true;}
        @Override public boolean step(){
            if(retired()){cancel();return true;}
            if(!ready())return false;
            if(!resident){
                if(lease==null)lease=game.items().exasperato().impactArea(level);
                switch(lease.resident(footprint,reach,crossbowColdRequests)){
                    case WAITING->{waitingTick=game.server().getTickCount();return false;}
                    case FAILED->{cancel();return true;}
                    case READY->resident=true;
                }
            }
            var point=centers.get(next++);
            long started=System.nanoTime();
            blastsThisTick++;
            try{
                game.items().indirectKills().trapChanges(level,cause,
                        ()->CrossbowBlastBlocks.scoped(level,point,damage,
                                ()->level.explode(null,damage,null,point,CROSSBOW_BLAST_RADIUS,false,Level.ExplosionInteraction.MOB)));
            }finally{
                blastNanos+=System.nanoTime()-started;
            }
            if(next<centers.size())return false;
            cancel();
            return true;
        }
        @Override public void cancel(){
            if(lease!=null){lease.close();lease=null;}
            resident=false;
            if(path.pendingBlasts==this)path.pendingBlasts=null;
            crossbowWork.remove(this);
        }
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            var image=graph.links.damage(damage);
            int cursor=next;
            return links->{
                MatchAuthorityCheckpoint.world(game,level);
                if(MatchDamageSources.value(image)!=damage)throw new IllegalStateException("Restore accepted crossbow explosion damage");
                return ()->{cancel();next=cursor;waitingTick=Integer.MIN_VALUE;path.pendingBlasts=this;crossbowWork.add(this);};
            };
        }
    }
    public int rayPending(ServerPlayer owner){return Math.max(0,game.items().mana().hudValue(owner,"ray"));}
    public void addPlayerCounter(ServerPlayer owner){game.items().mana().score(owner,"ray").add(1);rays.put(owner,game.items().epoch());rayCauses.put(owner,me.skywars2.game.combat.WeaponEnchantments.effectCause(owner));}
    /** A source food can set ray directly; it must not create a second ray clock or another debit. */
    public void adoptRawFoodStatuses(ServerPlayer owner){
        if(rayPending(owner)>0&&!game.items().magic().rejectCustomEffect(owner))rays.put(owner,game.items().epoch());
    }

    public int sphereCount(){return spheres.size();}
    public List<Sphere> sphereSnapshot(){return List.copyOf(spheres);}
    /** The source sphere is a live marker; accepted ray samples and world writes remain fixed. */
    public void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform){
        spheres.removeIf(sphere->{
            if(sphere.level!=level||sphere.age>=300||!active(sphere.owner,sphere.epoch))return false;
            var next=transform.apply(sphere.position);if(next==null)return true;sphere.position=next;return false;
        });
    }
    public void tick(){
        for(var entry:List.copyOf(sniperWindups.entrySet())){
            var owner=entry.getKey();
            if(!owner.isAlive()||owner.isRemoved()||owner.level()!=game.maps().level()||!game.canUseGameplayItems(owner))clearSniperWindup(owner);
            else if(!game.items().timeStop().stopped(owner.level())){
                int remaining=entry.getValue()-1;
                if(remaining<=0)clearSniperWindup(owner);else sniperWindups.put(owner,remaining);
            }
        }
        for(var entry:List.copyOf(rays.entrySet())){
            var owner=entry.getKey();if(!active(owner,entry.getValue())||game.items().magic().rejectCustomEffect(owner)){clearRay(owner);continue;}
            if(rayPending(owner)<=0){rays.remove(owner);rayCauses.remove(owner);continue;}if(game.items().timeStop().stopped(owner.level()))continue;
            rayAt(owner,owner.getEyePosition(),owner.getLookAngle(),rayCauses.get(owner));game.items().mana().score(owner,"ray").add(-1);if(rayPending(owner)<=0){rays.remove(owner);rayCauses.remove(owner);}
        }
        for(var sphere:List.copyOf(spheres)){
            if(!active(sphere.owner,sphere.epoch)||sphere.owner.level()!=sphere.level||!sphere.level.hasChunkAt(BlockPos.containing(sphere.position))){spheres.remove(sphere);continue;}
            if(game.items().timeStop().stopped(sphere.level))continue;
            if(++sphere.age%5==0)for(int i=0;i<16;i++)add(sphere.owner,sphere.position,sphere.owner.getRandom().nextFloat()*360-180,sphere.owner.getRandom().nextFloat()*180-90,8,
                    new Path(PHANTOM,sphere.position,null,0,0,sphere.owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY),sphere.cause);
            if(sphere.age>=300)spheres.remove(sphere);
        }
    }
    private boolean active(ServerPlayer owner,String epoch){return owner.isAlive()&&!owner.isRemoved()&&game.items().currentEpoch(epoch)&&game.canUseGameplayItems(owner);}
    private void ray(ServerPlayer owner){rayAt(owner,owner.getEyePosition(),owner.getLookAngle());}
    private void rayAt(ServerPlayer owner, Vec3 from, Vec3 direction) {
        rayAt(owner,from,direction,me.skywars2.game.combat.WeaponEnchantments.effectCause(owner));
    }
    private void rayAt(ServerPlayer owner, Vec3 from, Vec3 direction,LbcIndirectKills.Owner cause) {
        var level = owner.level();
        Vec3 end = from.add(direction.scale(50));
        double boundary = game.items().magic().mechanismSuppressionFraction(level, from, end);
        if (Double.isFinite(boundary)) end = from.lerp(end, Math.max(0, Math.nextDown(boundary)));
        var wall = LbcTerra.wallHit(level, from, end);
        if (wall != null) end = wall.getLocation();

        var family = game.items().familyDrops();
        var contact = family.firstArcContact(level, from, end, wall == null ? null : wall.getBlockPos());
        if (contact != null) {
            end = contact.position();
            family.reactToArc(level, contact, owner, false);
        }
        if (from.distanceToSqr(end) < 1e-12) return;

        double length = from.distanceTo(end);
        try (var rayDust = new LbcParticleBatch.Group(level, DUST, .1, 0, true);
             var raySpark = new LbcParticleBatch.Group(level, ParticleTypes.ELECTRIC_SPARK, .1, 1, false)) {
            for (int i = 0; i <= 100 && i * .5 <= length + 1e-7; i++) {
                if (owner.getRandom().nextFloat() < .35f) {
                    var point = from.add(direction.scale(i * .5));
                    rayDust.add(point);
                    raySpark.add(point);
                }
            }
        }
        LbcOverload.sound(owner, from.lerp(end, .5), "minecraft:entity.firework_rocket.twinkle", .03f, 2, 0);
        var damage = game.items().indirectKills().captureDamage(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(LbcDamage.ELECTRO_BOLT), owner),cause);
        var affected = Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());
        LbcDamage.hurtSegment(level, damage, from, end, Vec3.ZERO, .5, .5f, 1,
                e -> e.getType().builtInRegistryHolder().is(MOBS) && game.items().wands().projectileTarget(owner, e),
                false, e -> {
                    if (affected.add(e) && e instanceof LivingEntity living) {
                        living.addEffect(ItemEffectLevels.create(MobEffects.GLOWING, 20, 0), owner);
                        living.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS, 20, 100), owner);
                    }
                }, null);
    }
    public void clearRay(ServerPlayer owner){rays.remove(owner);rayCauses.remove(owner);game.items().mana().score(owner,"ray").set(0);clearSniperWindup(owner);}
    public int sniperWindupTicks(ServerPlayer owner){return sniperWindups.getOrDefault(owner,0);}
    private void clearSniperWindup(ServerPlayer owner){sniperWindups.remove(owner);owner.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SNIPER_WINDUP);}
    public void forget(ServerPlayer owner){clearRay(owner);clearSniperWindup(owner);spheres.removeIf(s->s.owner==owner);}
    public void stop(){for(var owner:List.copyOf(rays.keySet()))clearRay(owner);for(var owner:List.copyOf(sniperWindups.keySet()))clearSniperWindup(owner);spheres.clear();}

    int cloneNearby(ServerPlayer holder,int skill,int budget,long deadline) {
        int made=0,count=CloneVirtualAbilityStates.count(skill),bit=CloneVirtualAbilityStates.bit(skill);
        for(var sphere:List.copyOf(spheres)){
            if(budget-made<count||System.nanoTime()>=deadline)break;
            if(sphere.age>=300||!active(sphere.owner,sphere.epoch)||sphere.owner.level()!=sphere.level
                    ||!CloneVirtualAbilityStates.near(game,holder,sphere.level,sphere.position,sphere.cloneMask,skill))continue;
            var points=CloneVirtualAbilityStates.positions(holder,sphere.level,sphere.position,skill);if(points==null)continue;
            for(var point:points){var copy=new Sphere(sphere.owner,point,sphere.epoch,sphere.cause);copy.age=sphere.age;
                copy.cloneMask=sphere.cloneMask|bit;spheres.add(copy);}
            sphere.cloneMask|=bit;made+=count;
        }
        return made;
    }
    public SphereCheckpoint checkpointSpheres(MatchSpellCheckpoint.Links links){return new SphereCheckpoint(links);}
    public final class SphereCheckpoint {
        private record Image(MatchDamageSources.Ref owner,ServerLevel level,Vec3 position,String epoch,int age,int mask,LbcIndirectKills.Owner cause){}
        private record RayImage(MatchDamageSources.Ref owner,String epoch,LbcIndirectKills.Owner cause){}
        private final List<Image> images;
        private final List<RayImage> rayImages;
        SphereCheckpoint(MatchSpellCheckpoint.Links links){images=spheres.stream().map(s->new Image(links.key(s.owner),s.level,s.position,s.epoch,s.age,s.cloneMask,s.cause)).toList();rayImages=rays.entrySet().stream().map(e->new RayImage(links.key(e.getKey()),e.getValue(),rayCauses.get(e.getKey()))).toList();}
        public Runnable prepare(MatchSpellCheckpoint.Links links){var values=new java.util.ArrayList<Sphere>();
            for(var image:images){var owner=(ServerPlayer)links.get(image.owner);MatchAuthorityCheckpoint.world(game,image.level);
                if(owner.level()!=image.level)throw new IllegalStateException("Restore sphere's original author dimension");
                var sphere=new Sphere(owner,image.position,image.epoch,image.cause);sphere.age=image.age;sphere.cloneMask=image.mask;values.add(sphere);}
            var rayOwners=new IdentityHashMap<ServerPlayer,RayImage>();for(var image:rayImages)rayOwners.put((ServerPlayer)links.get(image.owner),image);
            return ()->{spheres.clear();spheres.addAll(values);rays.clear();rayCauses.clear();rayOwners.forEach((owner,image)->{rays.put(owner,image.epoch);rayCauses.put(owner,image.cause);});};
        }
    }

}
