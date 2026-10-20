package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.scores.ScoreHolder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Consuming and its two independent donor scrolls share the existing terrain work queue. */
public final class LbcConsuming {
    static final String TRIAL_ACTOR="skywars2.consuming.trial_actor";
    private static final net.minecraft.core.particles.DustParticleOptions NORMAL_DUST=dust(.424f,.251f,.647f,2);
    private static final net.minecraft.core.particles.DustParticleOptions END_DUST=dust(0,0,0,4);
    private static final net.minecraft.core.particles.DustParticleOptions AREA_DUST=dust(.5f,0,.5f,4);
    private static final net.minecraft.core.particles.DustColorTransitionOptions PART_DUST=
            new net.minecraft.core.particles.DustColorTransitionOptions(rgb(.494f,.282f,.482f),rgb(.294f,.082f,.282f),2);
    private static int rgb(float r,float g,float b) { return net.minecraft.util.ARGB.colorFromFloat(1,r,g,b)&0xffffff; }
    private static net.minecraft.core.particles.DustParticleOptions dust(float r,float g,float b,float scale) {
        return new net.minecraft.core.particles.DustParticleOptions(rgb(r,g,b),scale);
    }
    private static final Identifier MEDITATION=Identifier.parse("lbc:meditation");
    private static final Identifier END_AIR=Identifier.parse("lbc:air11x11");
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private final GameController game;
    private final List<Partition> partitions=new ArrayList<>();
    private final List<Area> areas=new ArrayList<>();
    private final Map<Entity,Owned> summons=new IdentityHashMap<>();
    private final Map<LbcStonekivi.Work,Owned> work=new IdentityHashMap<>();
    private final Map<ServerPlayer,Terrain> terrain=new IdentityHashMap<>();
    private static final class Terrain {
        final BlockAreaWork scan;
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> portals=new java.util.LinkedHashMap<>();
        Terrain(BlockAreaWork scan) { this.scan=scan; }
    }
    private CompletableFuture<Prepared> preparation;
    private Prepared prepared;
    private record Spawn(CompoundTag data,Vec3 offset) { }
    private record Prepared(Map<Identifier,List<MapManager.TemplateSlice>> templates,List<Spawn> spawns) { }
    private static class Owned {
        final ServerPlayer owner; final ServerLevel level; final String epoch;
        boolean cancelled;
        Owned(ServerPlayer owner,String epoch) { this.owner=owner; this.level=owner.level(); this.epoch=epoch; }
    }
    private static final class Partition extends Owned {
        Vec3 point,direction; int age;
        Partition(ServerPlayer owner,String epoch,Vec3 point,Vec3 direction) {
            super(owner,epoch); this.point=point; this.direction=direction;
        }
    }
    private static final class Area extends Owned {
        Vec3 point; int age;
        Area(ServerPlayer owner,String epoch,Vec3 point) { super(owner,epoch); this.point=point; }
    }

    LbcConsuming(GameController game) { this.game=game; }
    public void prepare() {
        prepared=null;
        var fixer=game.server().getFixerUpper();
        var spawns=game.maps().computeFromSnapshot(()-> {
            try(var input=LbcConsuming.class.getResourceAsStream("/skywars2/consuming_presets.json")) {
                if(input==null) throw new IllegalStateException("Missing consuming presets");
                var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
                int version=json.get("originalDataVersion").getAsInt();
                var data=new java.util.HashMap<String,CompoundTag>();
                for(var entry:json.getAsJsonObject("presets").entrySet()) {
                    var tag=TagParser.parseCompoundFully(entry.getValue().getAsString());
                    data.put(entry.getKey(),(CompoundTag)fixer.update(References.ENTITY,new Dynamic<>(NbtOps.INSTANCE,tag),version,
                            SharedConstants.getCurrentVersion().dataVersion().version()).getValue());
                }
                var result=new ArrayList<Spawn>();
                for(var entry:json.getAsJsonArray("spawns")) {
                    var spawn=entry.getAsJsonObject(); var offset=spawn.getAsJsonArray("offset");
                    result.add(new Spawn(data.get(spawn.get("preset").getAsString()),new Vec3(
                            offset.get(0).getAsDouble(),offset.get(1).getAsDouble(),offset.get(2).getAsDouble())));
                }
                return List.copyOf(result);
            } catch(Exception error) { throw new IllegalStateException("Invalid consuming presets",error); }
        });
        preparation=game.maps().prepareSpellTemplates(List.of(MEDITATION,END_AIR)).thenCombine(spawns,Prepared::new);
    }
    public boolean ready() {
        if(prepared==null && preparation!=null && preparation.isDone()) {
            try { prepared=preparation.join(); }
            catch(java.util.concurrent.CompletionException error) {
                me.skywars2.SkyWars2.LOGGER.error("Consuming preparation failed",error); preparation=null;
            }
        }
        return prepared!=null;
    }
    private boolean valid(Owned state) {
        return !state.cancelled && game.items().currentEpoch(state.epoch) && valid(state.owner) && state.level==state.owner.level();
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && owner.isAlive() && !owner.isRemoved() && owner.level()==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped");
    }
    private boolean castable(ServerPlayer owner) {
        return valid(owner) && !paused(owner) && !game.items().frost().isFrozen(owner)
                && !game.items().magic().suppressed(owner)
                && !game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());
    }
    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean upgraded) {
        if(!castable(owner) || !ready()) return false;
        int price=game.items().mana().score(ScoreHolder.forNameOnly("consuming_wand_mana_use"),"lbc.math").get();
        int mode=upgraded?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4):1;
        if(upgraded && owner.isShiftKeyDown()) {
            if(!game.items().mana().trySpendWand(owner,price,0)) return false;
            mode=mode%4+1; var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            tag.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            owner.sendSystemMessage(Component.translatable(switch(mode) {
                case 2->"mode_silence"; case 3->"mode_the_end"; case 4->"mode_avarice"; default->"mode_default";
            })); return true;
        }
        if(!game.items().mana().trySpendWand(owner,price)) return false;
        if(!upgraded && owner.isShiftKeyDown() && LbcAcademy.trial(stack)) {
            trial(owner); game.items().academy().ready(owner,hand,stack);
        } else if(mode==2) silence(owner);
        else if(mode==3) theEnd(owner);
        else if(mode==4) avarice(owner);
        else normal(owner);
        flushTerrain();
        game.items().wands().wandCasted(owner); return true;
    }

    private void normal(ServerPlayer owner) {
        Vec3 start=owner.getEyePosition(),direction=owner.getLookAngle().normalize(),end=start;
        double suppression=game.items().magic().suppressionFraction(owner.level(),start,start.add(direction.scale(50)));
        for(int sample=0;sample<51;sample++) {
            Vec3 point=start.add(direction.scale(sample));
            if(!owner.level().hasChunkAt(BlockPos.containing(point)) || sample>0 && sample/50.0>=suppression) break;
            end=point; erase(owner,point,1);
            owner.level().sendParticles(NORMAL_DUST,true,false,point.x,point.y,point.z,2,.05,.05,.05,0);
            LbcOverload.sound(owner,point,"minecraft:entity.wither.shoot",.01f,1,25,.01f);
            if(point.distanceToSqr(owner.position())>49) partitions.add(new Partition(owner,game.items().epoch(),point,
                    Vec3.directionFromRotation(owner.getRandom().nextFloat()*360-180,owner.getRandom().nextFloat()*360-180)));
        }
        hits(owner,start,end,Vec3.ZERO,1,2,2);
    }
    private boolean traversable(ServerPlayer owner,Vec3 point) {
        return owner.level().hasChunkAt(BlockPos.containing(point)) && !game.items().magic().suppressedAt(owner.level(),point);
    }
    private boolean target(ServerPlayer owner,Entity entity) {
        return entity instanceof LivingEntity && game.items().wands().target(owner,entity)
                && !entity.entityTags().contains("strict_map_object")
                && !game.items().magic().suppressedAt(owner.level(),entity.position());
    }
    private void hits(ServerPlayer owner,Vec3 from,Vec3 to,Vec3 padding,double spacing,float damage,int limit) {
        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.CONSUMING),owner);
        var eligible=new IdentityHashMap<Entity,Boolean>();
        LbcDamage.hurtSegment(owner.level(),source,from,to,padding,spacing,damage,limit,
                entity->entity.isAlive() && eligible.computeIfAbsent(entity,candidate->target(owner,candidate)),false);
    }
    private void erase(ServerPlayer owner,Vec3 point,double half) {
        var first=BlockPos.containing(point.x-half,point.y-half,point.z-half);
        var last=BlockPos.containing(point.x+half,point.y+half,point.z+half);
        var batch=terrain.computeIfAbsent(owner,ignored->new Terrain(new BlockAreaWork(owner.level(),state->!state.isAir(),(pos,state)-> {
            game.items().indirectKills().supportRemoved(owner,pos);
            return Blocks.AIR.defaultBlockState();
        },FLAGS,true)));
        var bounds=new BlockAreaWork.Bounds(first.getX(),first.getY(),first.getZ(),last.getX(),last.getY(),last.getZ());
        batch.scan.include(bounds);
        batch.portals.keySet().removeIf(bounds::contains);
    }
    private void flushTerrain() {
        for(var entry:terrain.entrySet()) {
            var owner=entry.getKey(); var batch=entry.getValue(); var scan=batch.scan;
            queue(owner,new LbcStonekivi.Work() {
            public boolean step() {
                try { scan.step(game,owner,512,System.nanoTime()+500_000); }
                finally { game.items().indirectKills().finishSupportRemoval(); }
                if(scan.hasWork()) return false;
                var placements=batch.portals.entrySet().iterator();
                long deadline=System.nanoTime()+500_000;
                for(int count=0;count<32 && placements.hasNext() && System.nanoTime()<deadline;count++) {
                    var placement=placements.next();
                    if(!owner.level().hasChunkAt(placement.getKey())) return false;
                    game.items().indirectKills().trapChanges(owner,()->owner.level().setBlock(placement.getKey(),placement.getValue(),FLAGS));
                    placements.remove();
                }
                return batch.portals.isEmpty();
            }
            public void cancel() { scan.clear(); batch.portals.clear(); }
            });
        }
        terrain.clear();
    }
    private void queue(ServerPlayer owner,LbcStonekivi.Work child) {
        var state=new Owned(owner,game.items().epoch());
        var wrapper=new LbcStonekivi.Work() {
            public boolean step() {
                if(!valid(state)) { cancel(); return true; }
                if(paused(owner)) return false;
                if(child.step()) { work.remove(this); return true; }
                return false;
            }
            public void cancel() { state.cancelled=true; child.cancel(); work.remove(this); }
        };
        work.put(wrapper,state); game.items().stonekivi().queuedWork(wrapper);
    }
    private void template(ServerPlayer owner,Identifier id,BlockPos origin) {
        for(var part:prepared.templates.getOrDefault(id,List.of())) {
            var bounds=part.bounds();
            int minX=origin.getX()+bounds.minX(),maxX=origin.getX()+bounds.maxX();
            int minZ=origin.getZ()+bounds.minZ(),maxZ=origin.getZ()+bounds.maxZ();
            for(int x=minX>>4;x<=maxX>>4;x++) for(int z=minZ>>4;z<=maxZ>>4;z++) {
                var box=new BoundingBox(Math.max(minX,x*16),origin.getY()+bounds.minY(),Math.max(minZ,z*16),
                        Math.min(maxX,x*16+15),origin.getY()+bounds.maxY(),Math.min(maxZ,z*16+15));
                queue(owner,()-> {
                    if(!owner.level().hasChunkAt(new BlockPos(box.minX(),box.minY(),box.minZ()))) return false;
                    game.items().indirectKills().trapChanges(owner,()->game.maps().placeMapSlice(owner.level(),part.template(),origin,
                            new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true).setBoundingBox(box),0));
                    return true;
                });
            }
        }
    }
    private void theEnd(ServerPlayer owner) {
        Vec3 start=owner.getEyePosition().add(owner.getLookAngle().normalize().scale(8));
        for(int angle:new int[]{0,20,-20,-10,10}) {
            Vec3 direction=Vec3.directionFromRotation(owner.getXRot(),owner.getYRot()+angle);
            Vec3 end=start;
            double suppression=game.items().magic().suppressionFraction(owner.level(),start,start.add(direction.scale(400)));
            for(int sample=0;sample<201;sample++) {
                Vec3 point=start.add(direction.scale(sample*2));
                if(!owner.level().hasChunkAt(BlockPos.containing(point)) || sample>0 && sample/200.0>=suppression) break;
                end=point; erase(owner,Vec3.atLowerCornerOf(BlockPos.containing(point)),5);
                owner.level().sendParticles(END_DUST,true,false,point.x,point.y,point.z,4,2,2,2,0);
                LbcOverload.sound(owner,point,"minecraft:entity.wither.shoot",.01f,0,0);
                BlockPos portal=BlockPos.containing(point.subtract(direction.scale(4)));
                boolean nether=owner.getRandom().nextInt(20)==0,ender=owner.getRandom().nextInt(20)==0;
                if(nether || ender) terrain.get(owner).portals.put(portal,(ender?Blocks.END_PORTAL:Blocks.NETHER_PORTAL).defaultBlockState());
            }
            endHits(owner,start,end);
        }
    }
    /** The End's source distance selector is a sphere around each sample, with two attempts per sample. */
    private void endHits(ServerPlayer owner,Vec3 start,Vec3 end) {
        Vec3 delta=end.subtract(start); double length=delta.length();
        Vec3 direction=length>1e-9?delta.scale(1/length):Vec3.ZERO;
        int samples=(int)Math.floor(length/2+1e-6)+1;
        int[] attempts=new int[samples];
        var damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.CONSUMING),owner);
        for(var entity:game.items().lifetimes().loadedTargets()) {
            if(entity.level()!=owner.level() || !target(owner,entity)) continue;
            Vec3 offset=entity.position().subtract(start);
            double projection=offset.dot(direction),perpendicular=offset.lengthSqr()-projection*projection;
            if(perpendicular>25) continue;
            double span=Math.sqrt(Math.max(0,25-perpendicular));
            int first=Math.max(0,(int)Math.ceil((projection-span)/2-1e-6));
            int last=Math.min(samples-1,(int)Math.floor((projection+span)/2+1e-6));
            if(length<1e-9) { first=0; last=offset.lengthSqr()<=25?0:-1; }
            for(int sample=first;sample<=last && entity.isAlive();sample++) if(attempts[sample]<2) {
                attempts[sample]++; entity.hurtServer(owner.level(),damage,6);
            }
        }
    }
    /** Broad source effects include allies and dropped/projectile entities while preserving technical actors. */
    private boolean ordinary(Entity entity) {
        if(entity==null || entity.isRemoved() || entity instanceof ServerPlayer || entity.isSpectator()) return false;
        var tags=entity.entityTags();
        return !tags.contains("spectator") && !tags.contains("strict_map_object") && !tags.contains("core_marker") && !tags.contains("map_object")
                && !tags.contains("rcdenyalways") && !tags.contains("sauvojen_tuntija") && !tags.contains("magic_academy")
                && !tags.contains("transformator")
                && (!(entity instanceof net.minecraft.world.entity.Marker
                    || entity instanceof net.minecraft.world.entity.Display
                    || entity instanceof net.minecraft.world.entity.Interaction
                    || entity instanceof net.minecraft.world.entity.AreaEffectCloud)
                    || tags.stream().noneMatch(tag->tag.startsWith("swrg.")));
    }
    private DamageSource damage(ServerPlayer owner,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type) {
        return new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),owner);
    }
    private void silence(ServerPlayer owner) {
        Vec3 center=owner.getEyePosition();
        owner.addEffect(new MobEffectInstance(MobEffects.LEVITATION,60,30));
        owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,1200));
        var stop=new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(null,null);
        for(var player:game.server().getPlayerList().getPlayers()) player.connection.send(stop);
        var scan=new BlockAreaWork(owner.level(),state->!state.isAir(),(pos,state)-> {
            if(Vec3.atCenterOf(pos).distanceToSqr(center)>48*48) return null;
            game.items().indirectKills().supportRemoved(owner,pos);
            return Blocks.AIR.defaultBlockState();
        },FLAGS,true);
        BlockPos first=BlockPos.containing(center.add(-48,-48,-48)),last=BlockPos.containing(center.add(48,48,48));
        scan.move(new BlockAreaWork.Bounds(first.getX(),first.getY(),first.getZ(),last.getX(),last.getY(),last.getZ()),true);
        queue(owner,new LbcStonekivi.Work() {
            public boolean step() {
                try { scan.step(game,owner,512,System.nanoTime()+500_000); }
                finally { game.items().indirectKills().finishSupportRemoval(); }
                return !scan.hasWork();
            }
            public void cancel() { scan.clear(); }
        });
        partitions.removeIf(part->part.level==owner.level() && part.point.distanceToSqr(center)<=48*48);
        areas.removeIf(area->area.level==owner.level() && area.point.distanceToSqr(center)<=48*48);
        game.items().transformSpellOrigins(owner.level(),point->point.distanceToSqr(center)<=48*48?null:point);
        var doomed=new ArrayList<Entity>(); Entity hedgehog=null;
        for(var entity:owner.level().getEntities((Entity)null,new AABB(center,center).inflate(48))) {
            if(entity.position().distanceToSqr(center)>48*48) continue;
            if(entity.entityTags().contains("god_hedgehog")) { if(hedgehog==null) hedgehog=entity; continue; }
            if(ordinary(entity)) doomed.add(entity);
        }
        var kill=damage(owner,DamageTypes.GENERIC_KILL);
        queue(owner,new LbcStonekivi.Work() {
            int cursor;
            public boolean step() {
                long deadline=System.nanoTime()+500_000;
                for(int count=0;count<32 && cursor<doomed.size() && System.nanoTime()<deadline;count++) {
                    var entity=doomed.get(cursor++);
                    if(entity.level()!=owner.level() || !ordinary(entity)) continue;
                    if(entity instanceof LivingEntity living) {
                        living.hurtServer(owner.level(),kill,Float.MAX_VALUE);
                        // Dragon's damage path starts a long dying phase; native /kill removes it immediately.
                        if(living instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon && !living.isRemoved())
                            living.kill(owner.level());
                    }
                    else entity.kill(owner.level());
                }
                return cursor==doomed.size();
            }
            public void cancel() { doomed.clear(); }
        });
        var outside=damage(owner,DamageTypes.OUTSIDE_BORDER);
        for(var player:owner.level().players()) if(player.position().distanceToSqr(center)<=48*48
                && target(owner,player) && game.items().mana().hudValue(player,"void_resistance")<=0)
            player.hurtServer(owner.level(),outside,20);
        if(hedgehog!=null) owner.hurtServer(owner.level(),new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.OUTSIDE_BORDER),hedgehog),10_000_000_000f);
    }
    private void avarice(ServerPlayer owner) {
        Vec3 start=owner.getEyePosition(),direction=owner.getLookAngle().normalize(),limit=start.add(direction.scale(140));
        double suppression=game.items().magic().suppressionFraction(owner.level(),start,limit);
        if(Double.isFinite(suppression)) limit=start.lerp(limit,suppression);
        var hit=game.items().wands().firstHit(owner,start,limit,entity->entity instanceof LivingEntity
                && (entity instanceof ServerPlayer || ordinary(entity)),true);
        Vec3 end=hit==null?limit:hit.getLocation();
        var particle=dust(.667f,.459f,.941f,1);
        double distance=start.distanceTo(end);
        for(int sample=0;sample<=200 && sample*.7<=distance+1e-6;sample++) {
            Vec3 point=start.add(direction.scale(sample*.7));
            if(!owner.level().hasChunkAt(BlockPos.containing(point))) break;
            owner.level().sendParticles(particle,true,false,point.x,point.y,point.z,1,.05,.05,.05,0);
            LbcOverload.sound(owner,point,"minecraft:block.deepslate.break",.2f,1,25,1);
        }
        if(hit==null || !traversable(owner,end)) return;
        Vec3 impact=hit instanceof BlockHitResult?end.subtract(direction.scale(.7)):end;
        if(!owner.level().hasChunkAt(BlockPos.containing(impact))) return;
        var entities=new ArrayList<Entity>();
        for(var entity:owner.level().getAllEntities()) if(ordinary(entity)) entities.add(entity);
        for(var part:partitions) if(part.level==owner.level()) part.point=impact;
        for(var area:areas) if(area.level==owner.level()) area.point=impact;
        game.items().transformSpellOrigins(owner.level(),point->impact);
        queue(owner,new LbcStonekivi.Work() {
            int cursor;
            public boolean step() {
                if(!owner.level().hasChunkAt(BlockPos.containing(impact))) return false;
                long deadline=System.nanoTime()+500_000;
                for(int count=0;count<32 && cursor<entities.size() && System.nanoTime()<deadline;count++) {
                    var entity=entities.get(cursor++);
                    if(entity.level()!=owner.level() || !ordinary(entity)) continue;
                    entity.teleportTo(impact.x,impact.y,impact.z);
                    if(entity instanceof LivingEntity living) game.items().indirectKills().displaced(living,owner);
                }
                if(cursor<entities.size()) return false;
                var generic=damage(owner,DamageTypes.GENERIC);
                for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(impact,impact).inflate(3),
                        candidate->target(owner,candidate) && candidate.position().distanceToSqr(impact)<=9))
                    target.hurtServer(owner.level(),generic,20);
                return true;
            }
            public void cancel() { entities.clear(); }
        });
    }
    public void trial(ServerPlayer owner) {
        if(!valid(owner) || !ready()) return;
        Vec3 origin=owner.getEyePosition();
        template(owner,MEDITATION,BlockPos.containing(origin).offset(-23,-16,-23));
        for(var spawn:prepared.spawns) {
            var position=origin.add(spawn.offset);
            if(!owner.level().hasChunkAt(BlockPos.containing(position))) continue;
            var mob=new Skeleton(net.minecraft.world.entity.EntityTypes.SKELETON,owner.level());
            mob.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),spawn.data.copy()));
            mob.setPos(position); game.items().markCurrentItemEpoch(mob); mob.entityTags().add(TRIAL_ACTOR);
            summons.put(mob,new Owned(owner,game.items().epoch()));
            if(!owner.level().addFreshEntity(mob)) { summons.remove(mob); mob.discard(); }
        }
        owner.addEffect(new MobEffectInstance(MobEffects.DARKNESS,400,3,false,false));
        owner.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,400,3,false,false));
    }
    public boolean useScroll(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean destroyer) {
        if(!castable(owner)) return false;
        Vec3 start=owner.getEyePosition(),direction=owner.getLookAngle().normalize();
        if(destroyer) {
            double suppression=game.items().magic().suppressionFraction(owner.level(),start,start.add(direction.scale(200)));
            for(int sample=0;sample<201;sample++) {
                Vec3 point=start.add(direction.scale(sample));
                if(!owner.level().hasChunkAt(BlockPos.containing(point)) || sample>0 && sample/200.0>=suppression) break;
                int distance=sample+1;
                double half=distance<=5?.5:distance<=10?1:distance<=20?1.5:distance<=40?2:distance<=80?3:distance<=100?4:5;
                // Source Distance 5 performs both fills; their union is the larger cube.
                if(distance<=200) erase(owner,point,distance==5?1:half);
                owner.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,true,false,point.x,point.y,point.z,2,1,1,1,.01);
            }
        } else {
            Vec3 limit=start.add(direction.scale(200));
            var hit=game.items().wands().firstHit(owner,start,limit);
            if(hit==null || !traversable(owner,hit.getLocation())) return false;
            areas.add(new Area(owner,game.items().epoch(),hit.getLocation().add(0,1,0)));
            double distance=start.distanceTo(hit.getLocation());
            for(int sample=0;sample<=200 && sample<=distance;sample++) {
                Vec3 particle=start.add(direction.scale(sample));
                owner.level().sendParticles(ParticleTypes.LANDING_OBSIDIAN_TEAR,true,false,particle.x,particle.y,particle.z,1,0,0,0,.01);
            }
            Vec3 impact=hit.getLocation();
            owner.level().sendParticles(ParticleTypes.PORTAL,impact.x,impact.y,impact.z,100,1,1,1,2);
            LbcOverload.sound(owner,impact,"minecraft:block.portal.travel",1,0,0);
        }
        flushTerrain(); stack.shrink(1); return true;
    }
    public void tick() {
        partitions.removeIf(part->!valid(part) || game.items().magic().suppressedAt(part.level,part.point));
        areas.removeIf(area->!valid(area) || game.items().magic().suppressedAt(area.level,area.point));
        for(var part:partitions) {
            if(paused(part.owner)) continue;
            if(++part.age<60) continue;
            if(!traversable(part.owner,part.point)) { part.cancelled=true; continue; }
            erase(part.owner,part.point,1); hits(part.owner,part.point,part.point,Vec3.ZERO,1,2,3);
            // Local spatial query avoids visiting every loaded mob for each active partition.
            for(var target:part.level.getEntitiesOfClass(LivingEntity.class,new AABB(part.point,part.point).inflate(8),
                    candidate->part.point.distanceToSqr(candidate.position())<=64
                            && game.items().lifetimes().target(part.owner,candidate) && target(part.owner,candidate))) {
                part.direction=target.getEyePosition().subtract(part.point).normalize(); break;
            }
            part.level.sendParticles(PART_DUST,true,false,part.point.x,part.point.y,part.point.z,1,.1,.1,.1,0);
            part.point=part.point.add(part.direction.scale(.4));
        }
        partitions.removeIf(part->part.cancelled || part.age>=100);
        for(var area:areas) {
            if(paused(area.owner)) continue;
            if(++area.age%5!=0) continue;
            float yaw=area.owner.getRandom().nextFloat()*360-180,pitch=area.owner.getRandom().nextFloat()*180-90;
            Vec3 point=area.point;
            for(int remaining=149;remaining>=0;remaining--) {
                if(!area.level.hasChunkAt(BlockPos.containing(point))) break;
                erase(area.owner,point,.51);
                if(area.owner.getRandom().nextFloat()<.4f)
                    area.level.sendParticles(AREA_DUST,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
                point=point.add(Vec3.directionFromRotation(pitch,yaw).scale(.5));
                if(remaining<=50) { yaw+=10; pitch+=1; }
                else if(remaining<=100) { yaw+=5; pitch-=.2f; }
                else yaw+=1;
            }
            area.level.sendParticles(ParticleTypes.PORTAL,area.point.x,area.point.y,area.point.z,30,1,1,1,3);
        }
        areas.removeIf(area->area.age>=6000);
        for(var entry:List.copyOf(summons.entrySet())) if(entry.getKey().isRemoved() || !valid(entry.getValue())) {
            entry.getKey().discard(); summons.remove(entry.getKey());
        }
        flushTerrain();
    }
    ServerPlayer owner(Entity entity) {
        if(entity instanceof Projectile projectile) return owner(projectile.getOwner());
        var state=summons.get(entity); return state!=null && valid(state)?state.owner:null;
    }
    /** Испытание остаётся враждебным к вызвавшему игроку, но не атакует его союзников и служебные роли. */
    public boolean canTrialTarget(Entity source,Entity target) {
        var caster=owner(source);
        return caster==null || target==null || target==caster || game.items().wands().target(caster,target);
    }
    void loaded(Entity entity) {
        if(!entity.entityTags().contains(TRIAL_ACTOR)) return;
        var state=summons.get(entity);
        if(state==null || !valid(state)) { summons.remove(entity); entity.discard(); }
    }
    void unloaded(Entity entity) {
        // Remove ownership before native discard: unload may re-enter through entity removal.
        if(summons.remove(entity)!=null && !entity.isRemoved()) entity.discard();
    }
    public int count() { return partitions.size()+areas.size()+summons.size()+work.size(); }
    record PartitionView(ServerPlayer owner,String epoch,Vec3 point,int age) { }
    record AreaView(ServerPlayer owner,String epoch,Vec3 point,int age) { }
    List<PartitionView> partitionSnapshot() { return partitions.stream().map(part->new PartitionView(part.owner,part.epoch,part.point,part.age)).toList(); }
    List<AreaView> areaSnapshot() { return areas.stream().map(area->new AreaView(area.owner,area.epoch,area.point,area.age)).toList(); }
    List<Entity> summonSnapshot() { return List.copyOf(summons.keySet()); }
    int pendingWorkCount() { return work.size(); }
    public void forget(ServerPlayer owner) {
        partitions.removeIf(part->part.owner==owner); areas.removeIf(area->area.owner==owner);
        for(var entry:List.copyOf(work.entrySet())) if(entry.getValue().owner==owner) entry.getKey().cancel();
        for(var entry:List.copyOf(summons.entrySet())) if(entry.getValue().owner==owner) {
            entry.getKey().discard(); summons.remove(entry.getKey());
        }
    }
    public void stop() {
        partitions.clear(); areas.clear(); terrain.clear();
        for(var job:List.copyOf(work.keySet())) job.cancel();
        for(var entity:List.copyOf(summons.keySet())) entity.discard(); summons.clear();
    }
}
