package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Два удерживаемых огненных арканума; обработка блоков общая с Tannerkivi. */
public final class LbcKiuas {
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<CompoundTag> CURSE=
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(Identifier.parse("skywars2:kiuas_curse"),b->b.persistent(CompoundTag.CODEC));
    private static final int FLAGS=Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;
    private static final TagKey<Block> TO_AIR=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:kiuaskivi_to_air"));
    private static final TagKey<Block> TO_FIRE=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:kiuaskivi_to_fire"));
    private static final TagKey<EntityType<?>> PROJECTILES=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:projectiles"));
    private static final CompoundTag KIUAS_FLAG=flag("kiuaskivi"),SUN_FLAG=flag("sun_arcanum");
    public static final Item KIUASKIVI=register("skywars2:kiuaskivi","kiuaskivi","lbc:kiuaskivi",false);
    public static final Item SUN=register("skywars2:sun_arcanum","sun_arcanum","lbc:the_sun",true);
    private final GameController game;
    private final Map<UUID,Aura> auras=new HashMap<>();
    private final Map<Long,Set<Aura>> index=new HashMap<>();
    private final java.util.ArrayDeque<Aura> work=new java.util.ArrayDeque<>();
    private final Set<Aura> scheduled=new HashSet<>();
    private volatile PreparedSunPattern sunPattern;
    private volatile int preparation;
    private int ticks;
    private int checkpointCallbacks;
    private boolean checkpointRestoring;
    private final Map<LivingEntity,LbcIndirectKills.Owner> cursed=new java.util.LinkedHashMap<>();

    public static boolean isCurse(MobEffectInstance effect){return effect.is(MobEffects.RAID_OMEN)&&effect.getAmplifier()==111;}
    /** Ordinary Kiuas arrows reuse the ordered source tag conversion without a held-arcanum mana charge. */
    void arrowTerrain(ServerPlayer owner,net.minecraft.world.phys.Vec3 point,boolean ground) {
        checkpointCallbacks++;try {
            var level=owner.level();var center=BlockPos.containing(point);int radius=ground?15:1;
            var cause=game.items().indirectKills().coldCause(owner,true);
            var area=new BlockAreaWork(level,LbcKiuas::kiuEligible,(pos,state)->kiuResult(level,pos,state),FLAGS,false,true);
            area.include(new BlockAreaWork.Bounds(center.getX()-radius,center.getY()-(ground?radius:0),center.getZ()-radius,
                    center.getX()+radius,center.getY()+(ground?radius:1),center.getZ()+radius));
            while(area.hasWork())area.step(game,owner,cause,4096,Long.MAX_VALUE);
            if(ground)game.items().indirectKills().trapChanges(level,cause,()->{
                for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))
                    if(level.hasChunkAt(pos)&&!level.isOutsideBuildHeight(pos))level.setBlock(pos,Blocks.LAVA.defaultBlockState(),3);
            });
            else for(var viewer:level.players())level.sendParticles(viewer,ParticleTypes.FLAME,true,false,
                    point.x,point.y,point.z,3,.2,.2,.2,0);
        }finally{checkpointCallbacks--;}
    }
    /** Source transport marker becomes its existing tick counter, without a vanilla raid. */
    public boolean acceptCurse(LivingEntity target,MobEffectInstance effect,Entity source){
        checkpointCallbacks++;try {
            if(!isCurse(effect)||!game.playing()||target.level()!=game.maps().level()||!game.items().wands().validTarget(target)
                    ||!target.canBeAffected(effect)||target instanceof ServerPlayer p&&game.items().magic().rejectCustomDebuff(p))return false;
            var owner=LbcIndirectKills.playerSource(source);
            var cause=owner==null?null:game.items().indirectKills().coldCause(owner,true);
            cursed.put(target,cause);
            game.items().mana().score(target,"effect_kiuaskivi").add(Math.max(0,effect.getDuration()));saveCurse(target,cause);
            return true;
        }finally{checkpointCallbacks--;}
    }
    private void saveCurse(LivingEntity target,LbcIndirectKills.Owner cause){
        var tag=new CompoundTag();tag.putString("epoch",game.items().epoch());
        if(cause!=null){tag.putString("owner",cause.player().toString());tag.putLong("order",cause.order());tag.putBoolean("projectile",cause.projectile());}
        target.setAttached(CURSE,tag);
    }
    void loaded(Entity entity){
        checkpointCallbacks++;try {
            if(!(entity instanceof LivingEntity target))return;if(LbcParasite.active(target)){clearCurse(target);return;}var tag=target.getAttached(CURSE);if(tag==null)return;
            if(!game.items().currentEpoch(tag.getStringOr("epoch",""))){clearCurse(target);return;}
            LbcIndirectKills.Owner cause=null;
            try{cause=game.items().indirectKills().restoreColdCause(UUID.fromString(tag.getStringOr("owner","")),tag.getLongOr("order",0),tag.getBooleanOr("projectile",false));}catch(IllegalArgumentException ignored){}
            if(game.items().mana().hudValue(target,"effect_kiuaskivi")>0)cursed.put(target,cause);else clearCurse(target);
        }finally{checkpointCallbacks--;}
    }
    void unloaded(Entity entity){
        checkpointCallbacks++;try {    if(entity instanceof LivingEntity target)cursed.remove(target);
        }finally{checkpointCallbacks--;}
    }
    void adoptRawFoodStatuses(ServerPlayer player){
        if(game.items().mana().hudValue(player,"effect_kiuaskivi")<=0||game.items().magic().rejectCustomDebuff(player))return;
        var cause=cursed.computeIfAbsent(player,p->game.items().indirectKills().wildCause(player));saveCurse(player,cause);
    }
    public void clearCurse(LivingEntity target){
        checkpointCallbacks++;try {    cursed.remove(target);target.removeAttached(CURSE);
            if(game.items().mana().hudValue(target,"effect_kiuaskivi")!=0)game.items().mana().score(target,"effect_kiuaskivi").set(0);
        }finally{checkpointCallbacks--;}
    }
    private void tickCurses(){
        for(var target:java.util.List.copyOf(cursed.keySet())){
            if(game.items().polymorph().masked(target))continue;
            int remaining=game.items().mana().hudValue(target,"effect_kiuaskivi");
            if(remaining<=0||!game.items().wands().validTarget(target)||target.level()!=game.maps().level()
                    ||target instanceof ServerPlayer p&&game.items().magic().rejectCustomDebuff(p)){clearCurse(target);continue;}
            if(game.items().timeStop().frozen(target)||game.items().frost().isFrozen(target)||target.entityTags().contains("sin_greed_active"))continue;
            if(!game.items().magic().suppressedAt(game.maps().level(),target.position())){
                game.items().indirectKills().trapChanges(game.maps().level(),cursed.get(target),()->{
                    for(var pos:BlockPos.betweenClosed(target.blockPosition().offset(-1,0,-1),target.blockPosition().offset(1,1,1))){
                        if(!game.maps().level().hasChunkAt(pos))continue;
                        var state=game.maps().level().getBlockState(pos);var next=kiuResult(game.maps().level(),pos,state);
                        if(!next.equals(state)&&game.maps().level().setBlock(pos,next,FLAGS)&&next.is(Blocks.FIRE))game.maps().level().scheduleTick(pos.immutable(),Blocks.FIRE,30);
                    }
                });
                game.maps().level().sendParticles(ParticleTypes.FLAME,target.getX(),target.getY(),target.getZ(),3,.2,.2,.2,0);
            }
            game.items().mana().score(target,"effect_kiuaskivi").set(remaining-1);if(remaining==1)clearCurse(target);
        }
    }

    private record SunShift(int x,int y,int z) { }
    private record PreparedSunPattern(MapManager.BlockPattern pattern,Map<SunShift,java.util.List<BlockPos>> changedByShift) { }

    private static CompoundTag flag(String id) { var tag=new CompoundTag(); tag.putInt(id,1); return tag; }
    private static Item register(String id,String key,String model,boolean sun) {
        var resource=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        var data=flag(key); data.putInt("transformable",1); data.putInt("arcanum",1);
        data.putInt(sun?"arcanum5t":"arcanumt",1);
        if(sun) data.putInt("magicwand",1); else data.putInt("craftusable",1);
        var props=new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse(model); }
        }.setId(resource).overrideDescription(key).component(DataComponents.ITEM_MODEL,Identifier.parse(model))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(sun?"lbc:tier_a":"lbc:tier_d"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        return Registry.register(BuiltInRegistries.ITEM,resource,new SimplePolymerItem(props,Items.REPEATING_COMMAND_BLOCK,true));
    }
    public static void register() { }
    private static int kind(ItemStack stack) {
        if(stack.isEmpty()) return 0;
        if(stack.is(SUN)) return 2;
        if(stack.is(KIUASKIVI)) return 1;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return data==null?0:data.matchedBy(SUN_FLAG)?2:data.matchedBy(KIUAS_FLAG)?1:0;
    }
    public static boolean isKiuaskivi(ItemStack stack) { return kind(stack)==1; }
    LbcKiuas(GameController game,LbcMana mana) {
        this.game=game;
        for(var value:Map.of("kiuaskivi_mana_activate",1000,"kiuaskivi_mana_tick",10,"kiuaskivi_mana_tick_sec",200,
                "sun_arcanum_mana_activate",1500,"sun_arcanum_mana_tick",100,"sun_arcanum_mana_tick_sec",400).entrySet())
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly(value.getKey()),"lbc.math").set(value.getValue());
    }
    public void prepare() {
        int generation=++preparation; sunPattern=null;
        game.maps().prepareBlockPattern(Identifier.parse("lbc:the_sun"))
                .thenCompose(pattern->game.maps().computeFromSnapshot(()->prepareSunPattern(pattern))).whenComplete((value,error)-> {
            if(generation!=preparation) return;
            if(error!=null) SkyWars2.LOGGER.error("Sun arcanum pattern preparation failed",error);
            else sunPattern=value;
        });
    }
    public boolean ready() { return sunPattern!=null; }

    /** Built by MapManager's existing template worker while detached block states are available. */
    private static PreparedSunPattern prepareSunPattern(MapManager.BlockPattern pattern) {
        var shifts=new HashMap<SunShift,java.util.List<BlockPos>>();
        for(int dx=-1;dx<=1;dx++) for(int dy=-1;dy<=1;dy++) for(int dz=-1;dz<=1;dz++) {
            if(dx==0 && dy==0 && dz==0) continue;
            var changed=new java.util.ArrayList<BlockPos>();
            for(int y=0;y<pattern.size.getY();y++) for(int z=0;z<pattern.size.getZ();z++) for(int x=0;x<pattern.size.getX();x++) {
                var next=pattern.state(x,y,z);
                if(next!=null && !next.equals(pattern.state(x+dx,y+dy,z+dz))) changed.add(new BlockPos(x,y,z));
            }
            shifts.put(new SunShift(dx,dy,dz),java.util.List.copyOf(changed));
        }
        return new PreparedSunPattern(pattern,Map.copyOf(shifts));
    }
    public int count() { return auras.size(); }
    int queuedBlockCount() { return auras.values().stream().mapToInt(a->a.small.queuedBlockCount()+a.large.queuedBlockCount()+a.sun.queuedBlockCount()+a.sunChanged.size()).sum(); }

    public void tick() {
        checkpointCallbacks++;try {
            if(checkpointRestoring)return;
            tickCurses();
            boolean sunPass=++ticks%5==0;
            var present=new HashSet<UUID>();
            for(var player:game.server().getPlayerList().getPlayers()) {
                if(!game.items().mana().visible(player)) continue;
                present.add(player.getUUID());
                int main=kind(player.getMainHandItem()),off=kind(player.getOffhandItem());
                var aura=auras.get(player.getUUID());
                if(aura!=null && aura.level!=player.level()) { remove(aura); aura=null; }
                if(aura==null && main==0 && off==0) continue;
                if(aura==null) { aura=new Aura(player); auras.put(player.getUUID(),aura); }
                aura.owner=player;
                if(aura.mainKind!=main) { aura.mainKind=main; aura.mainActive=false; aura.mainSunThroughTick=-1; }
                if(aura.offKind!=off) { aura.offKind=off; aura.offActive=false; aura.offSunThroughTick=-1; }
                if(main!=2 && off!=2) aura.sunChanged.clear();
                aura.paused=game.items().wands().timeStopped() || player.entityTags().contains("time_stopped")
                        || game.items().magic().suppressed(player) || game.items().frost().isFrozen(player);
                if(aura.paused) { aura.sunMaintaining=false; reindex(aura); continue; }
                boolean mainPulse=advance(aura,true,main,sunPass),offPulse=advance(aura,false,off,sunPass);
                // Mana branches stay independent, but identical terrain writes are coalesced.
                int kiuPulses=(mainPulse && main==1?1:0)+(offPulse && off==1?1:0);
                int sunPulses=(mainPulse && main==2?1:0)+(offPulse && off==2?1:0);
                if(kiuPulses>0) kiuPulse(aura,kiuPulses,main==1 && off==1);
                if(sunPulses>0) sunPulse(aura,sunPulses,main==2 && off==2);
                serviceSun(aura,(main==2 && ticks<=aura.mainSunThroughTick) || (off==2 && ticks<=aura.offSunThroughTick));
                reindex(aura);
                if(main==0 && off==0 && !aura.hasWork()) remove(aura);
                else schedule(aura);
            }
            for(var aura:java.util.List.copyOf(auras.values())) if(!present.contains(aura.owner.getUUID())) remove(aura);
            // All holders share this budget. No background world reads or writes and no busy wait on futures.
            long deadline=System.nanoTime()+3_000_000L; int budget=65536;
            while(!work.isEmpty() && budget>0 && System.nanoTime()<deadline) {
                var aura=work.removeFirst(); scheduled.remove(aura);
                if(auras.get(aura.owner.getUUID())!=aura || aura.paused) continue;
                for(int i=0;i<3;i++) {
                    var area=aura.areas[Math.floorMod(aura.cursor++,3)];
                    if(!area.hasWork()) continue;
                    budget-=Math.max(1,area.step(game,aura.owner,Math.min(256,budget),deadline));
                    break;
                }
                schedule(aura);
            }
        }finally{checkpointCallbacks--;}
    }
    private void schedule(Aura aura) {
        if(!aura.paused && aura.hasWork() && scheduled.add(aura)) work.addLast(aura);
    }

    /** The original wrappers charge activation, then test upkeep; unlike Tanner no author exception was requested. */
    private boolean advance(Aura aura,boolean main,int type,boolean sunPass) {
        if(type==0 || type==2 && (!sunPass || !ready())) return false;
        boolean active=main?aura.mainActive:aura.offActive;
        if(!active && game.items().mana().trySpend(aura.owner,type==1?1000:1500)) {
            active=true;
            LbcOverload.sound(aura.owner,"minecraft:item.firecharge.use",type==1?1:4,0,0);
            aura.level.sendParticles(ParticleTypes.LAVA,aura.owner.getX(),aura.owner.getY()+1,aura.owner.getZ(),type==1?10:50,1,1,1,type==1?0:1);
        }
        boolean pulse=active && game.items().mana().trySpend(aura.owner,type==1?10:100);
        if(type==2 && pulse) {
            if(main) aura.mainSunThroughTick=ticks+4; else aura.offSunThroughTick=ticks+4;
        }
        boolean remains=pulse && game.items().mana().get(aura.owner)>=(type==1?10:100);
        if(main) aura.mainActive=remains; else aura.offActive=remains;
        return pulse;
    }

    private void kiuPulse(Aura aura,int pulses,boolean dual) {
        var player=aura.owner;
        player.addEffect(ItemEffectLevels.create(MobEffects.FIRE_RESISTANCE,600,0),player);
        aura.level.sendParticles(ParticleTypes.FLAME,player.getX(),player.getY()+1,player.getZ(),pulses*10,.5,1,.5,0);
        // Execute fill coordinates are relative to the precise position, including fractional block edges.
        var lo=BlockPos.containing(player.getX()-1.5,player.getY()-1.5,player.getZ()-1.5);
        var hi=BlockPos.containing(player.getX()+1.5,player.getY()+2.5,player.getZ()+1.5);
        var small=new BlockAreaWork.Bounds(lo.getX(),lo.getY(),lo.getZ(),hi.getX(),hi.getY(),hi.getZ());
        aura.small.move(small,!aura.kiuasIndexed); aura.smallBounds=small;
        for(int i=0;i<pulses && dual;i++) if(player.getRandom().nextFloat()<.05f) {
            var c=player.blockPosition();
            aura.largeBounds=new BlockAreaWork.Bounds(c.getX()-15,c.getY()-15,c.getZ()-15,c.getX()+15,c.getY()+15,c.getZ()+15);
            aura.large.move(aura.largeBounds,true);
        }
    }

    private void sunPulse(Aura aura,int pulses,boolean dual) {
        var player=aura.owner;
        player.addEffect(ItemEffectLevels.create(MobEffects.FIRE_RESISTANCE,2400,0),player);
        aura.level.sendParticles(ParticleTypes.FLAME,player.getX(),player.getY()+1,player.getZ(),pulses*40,.5,1,.5,2);
        for(int i=0;i<pulses;i++) LbcOverload.sound(player,"minecraft:block.fire.ambient",3,0,0);
        if(dual) for(int i=0;i<pulses;i++)
            for(var target:aura.level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(15),e->
                    game.items().wands().target(player,e) && e.position().distanceToSqr(player.position())<=225
                            && e.position().distanceToSqr(player.position())>=.01))
                target.hurtServer(aura.level,aura.level.damageSources().source(net.minecraft.world.damagesource.DamageTypes.ON_FIRE,player),10);
        for(var entity:aura.level.getEntities(player,player.getBoundingBox().inflate(25),e->
                (e instanceof Projectile || e.getType().builtInRegistryHolder().is(PROJECTILES))
                        && !e.entityTags().contains("magic_academy") && !e.entityTags().contains("transformator"))) {
            double distance=entity.position().distanceToSqr(player.position());
            if(distance<64 || distance>625 || !aura.level.hasChunkAt(entity.blockPosition())) continue;
            var pos=entity.blockPosition().immutable();
            game.items().indirectKills().worldChanges(aura.level,player,()->aura.level.setBlock(pos,Blocks.LAVA.defaultBlockState(),FLAGS));
            aura.level.scheduleTick(pos,net.minecraft.world.level.material.Fluids.LAVA,
                    net.minecraft.world.level.material.Fluids.LAVA.getTickDelay(aura.level));
            entity.discard();
        }
    }

    /** Refresh a paid Sun's structure every held tick, independently of its five-tick mana/effect pulse. */
    private void serviceSun(Aura aura,boolean holdingSun) {
        var data=sunPattern;
        aura.sunMaintaining=holdingSun && data!=null;
        if(!aura.sunMaintaining) return;
        var player=aura.owner;
        var nextOrigin=player.blockPosition().offset(-13,-13,-13);
        var size=data.pattern.size;
        var nextBounds=new BlockAreaWork.Bounds(nextOrigin.getX(),nextOrigin.getY(),nextOrigin.getZ(),
                nextOrigin.getX()+size.getX()-1,nextOrigin.getY()+size.getY()-1,nextOrigin.getZ()+size.getZ()-1);
        if(aura.sunOrigin==null || !aura.sunIndexed) aura.sun.move(nextBounds,true);
        else {
            int dx=nextOrigin.getX()-aura.sunOrigin.getX();
            int dy=nextOrigin.getY()-aura.sunOrigin.getY();
            int dz=nextOrigin.getZ()-aura.sunOrigin.getZ();
            if(dx!=0 || dy!=0 || dz!=0) {
                if(Math.abs(dx)<=1 && Math.abs(dy)<=1 && Math.abs(dz)<=1) {
                    aura.sun.move(nextBounds,false);
                    for(var local:data.changedByShift.get(new SunShift(dx,dy,dz)))
                        aura.sun.add(nextOrigin.offset(local.getX(),local.getY(),local.getZ()));
                } else aura.sun.move(nextBounds,true);
            }
        }
        aura.sunOrigin=nextOrigin;
        aura.sunBounds=nextBounds;
        for(var changed:aura.sunChanged) if(nextBounds.contains(changed)) aura.sun.add(changed);
        aura.sunChanged.clear();
    }

    static BlockState kiuResult(BlockState state) {
        // Preserve both fills in their original order: the second predicate sees the first fill's result.
        var result=state.is(TO_AIR)?Blocks.AIR.defaultBlockState():state;
        return result.is(TO_FIRE)?Blocks.FIRE.defaultBlockState():result;
    }
    /** Preserve the ordered classification, but admit new fire only at a supported live position. */
    static BlockState kiuResult(ServerLevel level,BlockPos pos,BlockState state) {
        var result=kiuResult(state);
        if(!result.is(Blocks.FIRE)||state.is(Blocks.FIRE)||fireSupported(level,pos))return result;
        // The first conversion still removes water/snow even when the second cannot ignite it.
        return state.is(TO_AIR)?Blocks.AIR.defaultBlockState():state;
    }
    static boolean fireSupported(ServerLevel level,BlockPos pos) {
        var below=pos.below();
        if(resident(level,below)&&!level.getBlockState(below).isAir())return true;
        for(var direction:Direction.values())if(direction!=Direction.DOWN){
            var neighbour=pos.relative(direction);if(!resident(level,neighbour))continue;
            var state=level.getBlockState(neighbour);
            if(state.hasProperty(BlockStateProperties.WATERLOGGED)&&state.getValue(BlockStateProperties.WATERLOGGED))continue;
            var flammable=FlammableBlockRegistry.getDefaultInstance().get(state.getBlock());
            if(flammable!=null&&flammable.getIgniteOdds()>0)return true;
        }
        return false;
    }
    private static boolean resident(ServerLevel level,BlockPos pos) {
        return pos.getY()>=level.getMinY()&&pos.getY()<=level.getMaxY()
                &&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    static boolean kiuEligible(BlockState state) { return !kiuResult(state).equals(state); }

    private void reindex(Aura aura) {
        boolean nextKiuas=aura.mainActive && aura.mainKind==1 || aura.offActive && aura.offKind==1;
        boolean nextSun=aura.sunMaintaining;
        var nextSmallBounds=nextKiuas?aura.smallBounds:null;
        var nextSunBounds=nextSun?aura.sunBounds:null;
        if(nextKiuas==aura.kiuasIndexed && nextSun==aura.sunIndexed
                && java.util.Objects.equals(nextSmallBounds,aura.indexedSmallBounds)
                && java.util.Objects.equals(nextSunBounds,aura.indexedSunBounds)) return;
        detach(aura);
        aura.kiuasIndexed=nextKiuas;
        aura.sunIndexed=nextSun;
        aura.indexedSmallBounds=nextSmallBounds;
        aura.indexedSunBounds=nextSunBounds;
        if(aura.kiuasIndexed) index(aura,aura.smallBounds);
        if(aura.sunIndexed) index(aura,aura.sunBounds);
    }
    private void index(Aura aura,BlockAreaWork.Bounds bounds) {
        if(bounds==null) return;
        for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++) for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
            long key=ChunkPos.pack(x,z);
            if(aura.chunks.add(key)) index.computeIfAbsent(key,k->new HashSet<>()).add(aura);
        }
    }
    private void detach(Aura aura) {
        for(long key:aura.chunks) { var set=index.get(key); if(set!=null) { set.remove(aura); if(set.isEmpty()) index.remove(key); } }
        aura.chunks.clear();
    }
    public void blockChanged(BlockPos pos,BlockState state) {
        checkpointCallbacks++;try {
            if(checkpointRestoring||index.isEmpty())return;
            // Support changes can make an already-scanned adjacent air cell ignite.
            enqueueChanged(pos,state);
            var level=game.maps().level();
            for(var direction:Direction.values()){
                var neighbour=pos.relative(direction);
                if(resident(level,neighbour))enqueueChanged(neighbour,level.getBlockState(neighbour));
            }
        }finally{checkpointCallbacks--;}
    }
    private void enqueueChanged(BlockPos pos,BlockState state) {
        var holders=index.get(ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4));
        if(holders==null) return;
        for(var aura:holders) {
            if(aura.smallBounds!=null && aura.smallBounds.contains(pos) && kiuEligible(state)
                    && (aura.mainActive && aura.mainKind==1 || aura.offActive && aura.offKind==1)) aura.small.add(pos);
            if(aura.sunBounds!=null && aura.sunBounds.contains(pos) && aura.sunMaintaining) {
                var expected=aura.sunState(pos);
                if(expected!=null && !expected.equals(state)) aura.sunChanged.add(pos.immutable());
            }
        }
    }
    public void chunkLoaded(LevelChunk chunk) {
        checkpointCallbacks++;try {
            if(checkpointRestoring)return;
            var holders=index.get(ChunkPos.pack(chunk.getPos().x(),chunk.getPos().z()));
            if(holders==null) return;
            int x=chunk.getPos().getMinBlockX(),z=chunk.getPos().getMinBlockZ();
            for(var aura:holders) {
                if(aura.smallBounds!=null) aura.small.add(aura.smallBounds.intersection(x,aura.level.getMinY(),z,x+15,aura.level.getMaxY(),z+15));
                if(aura.sunBounds!=null) aura.sun.add(aura.sunBounds.intersection(x,aura.level.getMinY(),z,x+15,aura.level.getMaxY(),z+15));
            }
        }finally{checkpointCallbacks--;}
    }
    private void remove(Aura aura) { detach(aura); aura.small.clear(); aura.large.clear(); aura.sun.clear(); scheduled.remove(aura); auras.remove(aura.owner.getUUID()); }
    public void stop() {
        checkpointCallbacks++;try {     for(var target:java.util.List.copyOf(cursed.keySet()))clearCurse(target);auras.clear(); index.clear(); work.clear(); scheduled.clear(); ticks=0;checkpointRestoring=false;
        }finally{checkpointCallbacks--;}
    }

    void checkpointBoundary(){if(checkpointCallbacks!=0)throw new IllegalStateException("Finish Kiuas aura callbacks before checkpointing");}
    /** Rollback only: keep curse/native authorities intact while discarding future fill metadata. */
    void prepareAuraFrozen(){
        checkpointBoundary();checkpointRestoring=true;clearFutureTerrain();
    }
    void clearFutureTerrain(){
        for(var aura:auras.values()){
            aura.small.clear();aura.large.clear();aura.sun.clear();aura.sunChanged.clear();aura.chunks.clear();
            aura.kiuasIndexed=false;aura.sunIndexed=false;aura.indexedSmallBounds=null;aura.indexedSunBounds=null;
        }
        index.clear();work.clear();scheduled.clear();
    }
    void publishAuraFrozen(){
        checkpointBoundary();index.clear();
        for(var aura:auras.values()){
            aura.chunks.clear();aura.kiuasIndexed=false;aura.sunIndexed=false;aura.indexedSmallBounds=null;aura.indexedSunBounds=null;reindex(aura);
        }
        checkpointRestoring=false;
    }
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){
        checkpointBoundary();if(queuedBlockCount()!=0)throw new IllegalStateException("Finish Kiuas/Sun terrain before taking aura essentials");
        return new Checkpoint(links);
    }
    final class Checkpoint {
        private record AuraImage(Aura original,UUID owner,ServerLevel ownerLevel,int mainKind,int offKind,boolean mainActive,boolean offActive,
                boolean paused,boolean maintaining,int mainRemaining,int offRemaining,BlockAreaWork.Bounds small,BlockAreaWork.Bounds large,
                BlockAreaWork.Bounds sun,BlockPos origin) { }
        private record CurseImage(UUID player,MatchDamageSources.Ref body,ServerLevel level,LbcIndirectKills.Owner cause) { }
        private final java.util.List<AuraImage> saved;
        private final java.util.List<CurseImage> curses=new java.util.ArrayList<>();
        private final Set<UUID> installed=new HashSet<>(),cursePlayers=new HashSet<>();private boolean baseInstalled;
        private final int cadence=ticks;private final PreparedSunPattern pattern=sunPattern;
        Checkpoint(MatchSpellCheckpoint.Links links){
            saved=auras.values().stream().map(a->new AuraImage(a,a.owner.getUUID(),a.owner.level(),a.mainKind,a.offKind,a.mainActive,a.offActive,a.paused,
                    a.sunMaintaining,a.mainSunThroughTick<0?Integer.MIN_VALUE:a.mainSunThroughTick-ticks,
                    a.offSunThroughTick<0?Integer.MIN_VALUE:a.offSunThroughTick-ticks,a.smallBounds,a.largeBounds,a.sunBounds,
                    a.sunOrigin==null?null:a.sunOrigin.immutable())).toList();
            cursed.forEach((target,cause)->curses.add(new CurseImage(target instanceof ServerPlayer player?player.getUUID():null,
                    target instanceof ServerPlayer?null:links.key(target),(ServerLevel)target.level(),cause)));
            saved.forEach(image->installed.add(image.owner));for(var image:curses)if(image.player!=null)cursePlayers.add(image.player);baseInstalled=true;
        }
        private Checkpoint(Checkpoint current,Checkpoint previous){
            var all=new java.util.ArrayList<>(current.saved);for(var image:previous.saved)if(!previous.installed.contains(image.owner))all.add(image);saved=java.util.List.copyOf(all);
            curses.addAll(current.curses);for(var image:previous.curses)if(image.player!=null&&!previous.cursePlayers.contains(image.player))curses.add(image);
            installed.addAll(current.installed);cursePlayers.addAll(current.cursePlayers);baseInstalled=true;
        }
        Checkpoint withPending(Checkpoint previous){return new Checkpoint(this,previous);}
        void resetReplay(){installed.clear();cursePlayers.clear();baseInstalled=false;}
        Set<UUID> pendingPlayers(){var pending=new HashSet<UUID>();for(var image:saved)if(!installed.contains(image.owner))pending.add(image.owner);
            for(var image:curses)if(image.player!=null&&!cursePlayers.contains(image.player))pending.add(image.player);return Set.copyOf(pending);}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();if(sunPattern!=pattern&&saved.stream().anyMatch(image->image.origin!=null))throw new IllegalStateException("Reconcile the prepared Sun corpus before aura replay");
            var plans=new java.util.ArrayList<Runnable>();
            for(var image:curses){
                if(image.player!=null){if(cursePlayers.contains(image.player))continue;var target=MatchAuraCheckpoint.current(game,image.player,image.level);if(target==null)continue;
                    plans.add(()->{cursed.put(target,image.cause);cursePlayers.add(image.player);});}
                else if(!baseInstalled){var target=(LivingEntity)links.get(image.body);plans.add(()->cursed.put(target,image.cause));}
            }
            for(var image:saved){if(installed.contains(image.owner))continue;var current=MatchAuraCheckpoint.current(game,image.owner,image.ownerLevel);if(current==null)continue;
                MatchAuthorityCheckpoint.world(game,image.original.level);
                plans.add(()->{var a=image.original;a.owner=current;a.mainKind=image.mainKind;a.offKind=image.offKind;
                    a.mainActive=image.mainActive;a.offActive=image.offActive;a.paused=image.paused;a.sunMaintaining=image.maintaining;
                    a.mainSunThroughTick=image.mainRemaining==Integer.MIN_VALUE?-1:ticks+image.mainRemaining;
                    a.offSunThroughTick=image.offRemaining==Integer.MIN_VALUE?-1:ticks+image.offRemaining;
                    a.smallBounds=image.small;a.largeBounds=image.large;a.sunBounds=image.sun;a.sunOrigin=image.origin;a.cursor=0;
                    a.small.resetBounds(image.small);a.large.resetBounds(image.large);a.sun.resetBounds(image.sun);
                    a.chunks.clear();a.sunChanged.clear();a.kiuasIndexed=false;a.sunIndexed=false;a.indexedSmallBounds=null;a.indexedSunBounds=null;
                    auras.put(current.getUUID(),a);installed.add(image.owner);});}
            return ()->{checkpointRestoring=true;if(!baseInstalled){clearFutureTerrain();auras.clear();cursed.clear();ticks=cadence;baseInstalled=true;}plans.forEach(Runnable::run);};
        }
    }

    private final class Aura {
        ServerPlayer owner; final ServerLevel level;
        int mainKind,offKind,cursor,mainSunThroughTick=-1,offSunThroughTick=-1; boolean mainActive,offActive,paused,kiuasIndexed,sunIndexed,sunMaintaining;
        BlockAreaWork.Bounds smallBounds,largeBounds,sunBounds;
        BlockAreaWork.Bounds indexedSmallBounds,indexedSunBounds;
        BlockPos sunOrigin;
        final Set<Long> chunks=new HashSet<>();
        final Set<BlockPos> sunChanged=new HashSet<>();
        final BlockAreaWork small,large,sun;
        final BlockAreaWork[] areas;
        Aura(ServerPlayer owner) {
            this.owner=owner; this.level=owner.level();
            java.util.function.BiFunction<BlockPos,BlockState,BlockState> fire=(pos,state)-> {
                var result=kiuResult(level,pos,state);
                if(result.is(Blocks.FIRE) && !result.equals(state)) level.scheduleTick(pos.immutable(),Blocks.FIRE,30);
                return result;
            };
            small=new BlockAreaWork(level,LbcKiuas::kiuEligible,fire,FLAGS);
            large=new BlockAreaWork(level,LbcKiuas::kiuEligible,fire,FLAGS);
            sun=new BlockAreaWork(level,state->true,(pos,state)-> {
                var expected=sunState(pos);
                if(expected!=null && expected.is(Blocks.LAVA) && !expected.equals(state))
                    level.scheduleTick(pos.immutable(),net.minecraft.world.level.material.Fluids.LAVA,
                            net.minecraft.world.level.material.Fluids.LAVA.getTickDelay(level));
                return expected;
            },FLAGS);
            areas=new BlockAreaWork[]{small,large,sun};
        }
        BlockState sunState(BlockPos pos) {
            var pattern=sunPattern;
            return pattern==null || sunOrigin==null?null:pattern.pattern.state(pos.getX()-sunOrigin.getX(),pos.getY()-sunOrigin.getY(),pos.getZ()-sunOrigin.getZ());
        }
        boolean hasWork() { return small.hasWork() || large.hasWork() || sun.hasWork(); }
    }
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        if(!(source instanceof LivingEntity target)||!cursed.containsKey(target))return null;
        var cause=cursed.get(target);var timer=CloneRemainingStatusStates.scores(game,source,java.util.List.of("effect_kiuaskivi"));
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                cursed.put((LivingEntity)clone,cause);CloneRemainingStatusStates.installScores(game,clone,timer);saveCurse((LivingEntity)clone,cause);
            }
            public void rollback(Entity clone){cursed.remove(clone);}
        };
    }

}
