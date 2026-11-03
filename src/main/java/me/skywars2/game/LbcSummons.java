package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared native summons: ordinary formations, Soul Vex and six Psychovex cultists. */
public final class LbcSummons {
    public static final String PROJECTILE="skywars2.summons.projectile";
    private IdentityHashMap<Entity,UUID> consequences=new IdentityHashMap<>();
    private static final String[] CULTISTS={"strangeness","chaos","luck","omnipresence","obsession","hypocrisy"};
    private final GameController game;
    private IdentityHashMap<Mob,Summon> mobs=new IdentityHashMap<>();
    private Map<String,CompoundTag> presets=Map.of();
    private java.util.concurrent.CompletableFuture<Map<String,CompoundTag>> preparation;
    private final List<StrangeRay> rays=new ArrayList<>();
    private int ticks;
    private boolean advancing;
    private final LbcSummonOrders orders;
    LbcSummonOrders orders(){return orders;}
    boolean command(ServerPlayer owner){return orders.command(owner);}
    public static boolean commandItem(ItemStack stack){
        var data=LbcFartus.data(stack);
        return LbcSummonerItems.kind(stack)!=null||stack.is(LbcWands.SUMMONER_VEXES)||stack.is(LbcWands.PSYCHOVEX)
                ||data.getIntOr("summonervexes",0)==1||data.getIntOr("psychovex_wand",0)==1||LbcKfcRewards.isCultus(stack);
    }
    private record Summon(UUID owner,String epoch,String kind) { }
    private static final class StrangeRay {
        final UUID owner; final String epoch; final ServerLevel level; final Vec3 origin; final int due; final Mob actor;
        Vec3 direction; int step;
        StrangeRay(UUID owner,String epoch,ServerLevel level,Vec3 origin,int due,Mob actor) {
            this.owner=owner; this.epoch=epoch; this.level=level; this.origin=origin; this.due=due; this.actor=actor;
        }
    }
    private final ArrayDeque<StrangeRay> activeRays=new ArrayDeque<>();

    LbcSummons(GameController game,LbcMana mana) {
        this.game=game;
        orders=new LbcSummonOrders(game,this);
        for(var kind:LbcSummonerItems.kinds())mana.score(ScoreHolder.forNameOnly(kind.cost()),"lbc.math").set(kind.price());
    }

    /** DFU выполняется один раз: игровые тики читают только готовые неизменяемые шаблоны. */
    public void prepare() {
        if(preparation!=null) return;
        var fixer=game.server().getFixerUpper();
        preparation=game.maps().computeFromSnapshot(()-> {
        var result=new HashMap<String,CompoundTag>();
        try(var input=LbcSummons.class.getResourceAsStream("/skywars2/summons_presets.json")) {
            if(input==null) throw new IllegalStateException("Missing summons presets");
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            int version=json.get("originalDataVersion").getAsInt();
            for(var entry:json.getAsJsonObject("presets").entrySet()) {
                var tag=TagParser.parseCompoundFully(entry.getValue().getAsString());
                result.put(entry.getKey(),SourceKnockbackBalance.entity((CompoundTag)fixer.update(References.ENTITY,new Dynamic<>(NbtOps.INSTANCE,tag),version,
                        SharedConstants.getCurrentVersion().dataVersion().version()).getValue()));
            }
            var ordinary=LbcSummonerItems.SOURCE;
            for(var entry:ordinary.getAsJsonObject("presets").entrySet()){
                var tag=TagParser.parseCompoundFully(entry.getValue().getAsString());
                result.put(entry.getKey(),SourceKnockbackBalance.entity((CompoundTag)fixer.update(References.ENTITY,new Dynamic<>(NbtOps.INSTANCE,tag),
                        ordinary.get("originalDataVersion").getAsInt(),SharedConstants.getCurrentVersion().dataVersion().version()).getValue()));
            }
        } catch(Exception error) { throw new IllegalStateException("Invalid original summons presets",error); }
        return Map.copyOf(result);
        });
    }
    public boolean ready() {
        if(presets.isEmpty() && preparation!=null && preparation.isDone()) presets=preparation.getNow(Map.of());
        return !presets.isEmpty();
    }
    public int count() { return mobs.size(); }
    List<Mob> snapshot() { return List.copyOf(mobs.keySet()); }
    List<Mob> ordinarySnapshot(String kind){return mobs.entrySet().stream().filter(e->e.getValue().kind.equals("ordinary_"+kind)).map(Map.Entry::getKey).toList();}
    private boolean valid(ServerPlayer owner) { return owner!=null && game.items().mana().visible(owner); }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")
                || game.items().frost().isFrozen(owner) || game.items().magic().suppressed(owner);
    }
    /** После призыва состояние хозяина не замораживает самостоятельный нативный AI. */
    private boolean paused(Mob mob) {
        return game.items().wands().timeStopped() || mob.entityTags().contains("time_stopped")
                || game.items().frost().isFrozen(mob);
    }
    private boolean target(ServerPlayer owner,Entity target) {
        return game.items().wands().target(owner,target)
                && !game.items().magic().suppressedAt(owner.level(),target.position())
                && !target.entityTags().contains("nomagic_active");
    }
    ServerPlayer owner(Entity entity) {
        var subdued=game.items().communism().owner(entity);if(subdued!=null)return subdued;
        if(LbcWtf.owner(entity) instanceof ServerPlayer chefOwner)return chefOwner;
        var consequence=consequences.get(entity);
        if(consequence!=null) return game.items().currentEpoch(entity)?game.server().getPlayerList().getPlayer(consequence):null;
        if(entity instanceof Projectile projectile) return owner(projectile.getOwner());
        if(entity instanceof EvokerFangs fangs) return owner(fangs.getOwner());
        var summon=entity instanceof Mob mob?mobs.get(mob):null;
        return summon!=null && game.items().currentEpoch(summon.epoch)
                ? game.server().getPlayerList().getPlayer(summon.owner):null;
    }
    List<Entity> controlledActors(UUID id){
        var result=new ArrayList<Entity>();
        mobs.forEach((mob,state)->{if(state.owner.equals(id)&&game.items().currentEpoch(state.epoch))result.add(mob);});
        return result;
    }
    boolean friendly(ServerPlayer player,Entity entity) {
        var owner=owner(entity); return owner!=null && (owner==player || player.isAlliedTo(owner));
    }
    void ownerChanged(Entity entity,ServerPlayer owner) {
        orders.changedOwner(entity);
        if(consequences.containsKey(entity)) consequences.put(entity,owner.getUUID());
        if(entity instanceof Mob mob){var state=mobs.get(mob);if(state!=null)mobs.put(mob,new Summon(owner.getUUID(),state.epoch,state.kind));}
    }
    boolean canAttack(Mob mob,LivingEntity target) {
        var owner=owner(mob);
        return valid(owner) && !paused(mob) && mob.level()==owner.level() && target(owner,target);
    }
    public boolean canMelee(Entity actor,Entity target){
        return !(actor instanceof Mob mob)||!mobs.containsKey(mob)||target instanceof LivingEntity living&&canAttack(mob,living);
    }
    public void inheritConversion(Entity original,Entity converted){
        if(!(original instanceof Mob source)||!(converted instanceof Mob mob))return;
        var state=mobs.remove(source);if(state==null)return;
        mobs.put(mob,state);orders.converted(source,mob);mob.entityTags().addAll(source.entityTags());mob.entityTags().add("skywars2.summons.actor");game.items().markCurrentItemEpoch(mob);
    }
    /** Callback общего Mob.setTarget: ванильный AI остаётся владельцем движения и ударов. */
    public void targetChanged(Mob mob,LivingEntity target) {
        if(target!=null && mobs.containsKey(mob) && !canAttack(mob,target)) mob.setTarget(null);
    }

    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean psycho) {
        if(!valid(owner) || paused(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        if(owner.isShiftKeyDown()) return command(owner);
        if(!ready())return false;
        var mana=game.items().mana();
        int price=mana.score(ScoreHolder.forNameOnly(psycho?"psychovex_wand_mana_use":"vex_soul_mana_use"),"lbc.math").get();
        boolean down=owner.getXRot()>=85;
        var destination=psycho && !down?owner.level().players().stream().filter(player->target(owner,player))
                .min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null):null;
        // Исходный допуск 1000 сохраняется и при реальном расходе 3000 возле врага.
        if(!mana.trySpendWand(owner,price,psycho && !down && destination!=null?price*3:price)) return false;
        return castSummon(owner,psycho,down,destination);
    }
    boolean useOrdinary(ServerPlayer owner,InteractionHand hand,LbcSummonerItems.Kind kind){
        if(!valid(owner)||paused(owner)||game.items().actionsBlocked(owner)
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        if(owner.isShiftKeyDown())return command(owner);
        if(!ready())return false;
        int price=game.items().mana().score(ScoreHolder.forNameOnly(kind.cost()),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,price))return false;
        castOrdinary(owner,kind);game.items().trinkets().summoned(owner);return true;
    }
    boolean castOrdinary(ServerPlayer owner,LbcSummonerItems.Kind kind){
        if(!ready())return false;
        String family="ordinary_"+kind.kind();
        // Direct owner/group handles replace the source's32 team selectors; unrelated casts stay intact.
        for(var mob:List.copyOf(mobs.keySet())){var state=mobs.get(mob);
            if(state!=null&&state.kind.equals(family)&&state.owner.equals(owner.getUUID()))remove(mob);
        }
        Vec3 forward=horizontal(owner),left=new Vec3(forward.z,0,-forward.x);
        for(var root:kind.roots()){
            var v=root.offset();var offset=root.local()?left.scale(v.x).add(0,v.y,0).add(forward.scale(v.z)):v;
            var position=owner.position().add(offset);
            if(owner.level().hasChunkAt(BlockPos.containing(position)))ordinaryTree(owner,family,presets.get(family),position,null);
        }
        effects(owner,false);return true;
    }
    private Mob ordinaryTree(ServerPlayer owner,String family,CompoundTag tag,Vec3 at,Mob vehicle){
        Mob mob=switch(tag.getStringOr("id","")){
            case "minecraft:skeleton"->new CultSkeleton(owner.level());
            case "minecraft:ravager"->new SoulRavager(owner.level());
            case "minecraft:ghast"->new SoulGhast(owner.level());
            case "minecraft:blaze"->new SoulBlaze(owner.level());
            case "minecraft:guardian"->new SoulGuardian(owner.level());
            case "minecraft:bat"->new SoulBat(owner.level());
            case "minecraft:skeleton_horse"->new SoulHorse(owner.level());
            default->throw new IllegalArgumentException("Unsupported summon tree "+tag);
        };
        mob.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),tag.copy()));
        mob.setPos(at);mob.entityTags().add("summactivated");mob.entityTags().add("skywars2.summons.actor");game.items().markCurrentItemEpoch(mob);
        mobs.put(mob,new Summon(owner.getUUID(),game.items().epoch(),family));orders.attach(mob);
        if(owner.getTeam()!=null)owner.level().getScoreboard().addPlayerToTeam(mob.getStringUUID(),owner.getTeam());
        if(!owner.level().addFreshEntity(mob)){remove(mob);return null;}
        if(vehicle!=null)mob.startRiding(vehicle,true,true);
        for(var child:tag.getListOrEmpty("Passengers"))if(child instanceof CompoundTag passenger)ordinaryTree(owner,family,passenger,at,mob);
        return mob;
    }
    private boolean castSummon(ServerPlayer owner,boolean psycho,boolean down,ServerPlayer destination){
        if(!psycho) {
            for(var mob:List.copyOf(mobs.keySet())) {
                var state=mobs.get(mob);
                if(state!=null && state.kind.equals("vex") && state.owner.equals(owner.getUUID())) remove(mob);
            }
            for(int[] shift:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
                spawnPreset(owner,"vex",owner.position().add(shift[0],1,shift[1]));
        } else {
            Vec3 point;
            if(down) point=owner.position().add(horizontal(owner).scale(2));
            else {
                var enemy=destination;
                if(enemy==null) { effects(owner,true); game.items().trinkets().summoned(owner); return true; }
                int direction=owner.getRandom().nextInt(4);
                Vec3 forward=horizontal(owner),right=new Vec3(-forward.z,0,forward.x);
                point=enemy.position().add((direction<2?forward:right).scale(direction%2==0?2:-2));
            }
            spawnPreset(owner,CULTISTS[owner.getRandom().nextInt(6)],point);
        }
        effects(owner,psycho); game.items().trinkets().summoned(owner); return true;
    }
    void sauvojenCultist(ServerPlayer owner,Vec3 origin,Vec3 direction){
        var target=owner.level().players().stream().filter(p->p!=owner&&!p.isSpectator()&&!game.isObserver(p)&&p.isAlive())
                .min(Comparator.comparingDouble(p->p.position().distanceToSqr(origin))).orElse(null);
        if(target==null)return;
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        Vec3 forward=Vec3.directionFromRotation(0,yaw),left=new Vec3(forward.z,0,-forward.x);
        int side=owner.getRandom().nextInt(4);
        Vec3 at=target.position().add((side<2?forward:left).scale(side%2==0?2:-2));
        if(spawnPreset(owner,CULTISTS[owner.getRandom().nextInt(6)],at)==null)return;
        for(float pitch:new float[]{2,1})LbcOverload.sound(owner,at,"minecraft:entity.warden.agitated",1,pitch,0);
    }
    private static Vec3 horizontal(LivingEntity entity) {
        double yaw=Math.toRadians(entity.getYRot()); return new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
    }
    private void effects(ServerPlayer owner,boolean psycho) {
        owner.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,owner.getX(),owner.getY()+1,owner.getZ(),300,5,5,5,0);
        LbcOverload.sound(owner,psycho?"minecraft:entity.vex.ambient":"minecraft:entity.wither.spawn",1,1,0);
    }
    Mob spawnPreset(ServerPlayer owner,String kind,Vec3 position) {
        if(!ready() || !owner.level().hasChunkAt(BlockPos.containing(position))) return null;
        Mob mob=switch(kind) {
            case "vex"->new SoulVex(owner.level());
            case "strangeness"->new CultWither(owner.level());
            case "hypocrisy"->new CultZombie(owner.level());
            default->new CultSkeleton(owner.level());
        };
        mob.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),presets.get(kind).copy()));
        // Source assignment adds this after the preset: the shared magic beacon must erase released summons too.
        mob.entityTags().add("summactivated");
        mob.setPos(position); game.items().markCurrentItemEpoch(mob);
        mobs.put(mob,new Summon(owner.getUUID(),game.items().epoch(),kind));orders.attach(mob);
        if(owner.getTeam()!=null) owner.level().getScoreboard().addPlayerToTeam(mob.getStringUUID(),owner.getTeam());
        if(!owner.level().addFreshEntity(mob)) { remove(mob); return null; }
        return mob;
    }
    LivingEntity attackTarget(Mob mob,ServerPlayer owner,Vec3 origin,double radius){
        var selected=orders.assigned(mob);
        return selected!=null ? selected.distanceToSqr(origin)<=radius*radius&&target(owner,selected)?selected:null : nearest(owner,origin,radius);
    }
    private List<LivingEntity> enemies(ServerPlayer owner,Vec3 position,double radius) {
        var area=new AABB(position,position).inflate(radius);
        return owner.level().getEntitiesOfClass(LivingEntity.class,area,
                living->living.distanceToSqr(position)<=radius*radius && target(owner,living));
    }
    private LivingEntity nearest(ServerPlayer owner,Vec3 position,double radius) {
        return enemies(owner,position,radius).stream().min(Comparator.comparingDouble(e->e.distanceToSqr(position))).orElse(null);
    }
    public void tick() {
        if(advancing)throw new IllegalStateException("Reentrant summon advance");advancing=true;try {
        ++ticks;
        orders.tick();
        for(var entity:List.copyOf(consequences.keySet())) {
            var owner=game.server().getPlayerList().getPlayer(consequences.get(entity));
            if(entity.isRemoved()) consequences.remove(entity);
            else if(!valid(owner) || !game.items().currentEpoch(entity)) { consequences.remove(entity); entity.discard(); }
        }
        for(var mob:List.copyOf(mobs.keySet())) {
            if(game.items().polymorph().suspended(mob))continue;
            var state=mobs.get(mob); if(state==null) continue;
            var owner=owner(mob);
            if(!mob.isAlive() || mob.isRemoved() || !valid(owner) || mob.level()!=owner.level()
                    || !game.items().currentEpoch(state.epoch)) { remove(mob); continue; }
            if(paused(mob)) { mob.setTarget(null); continue; }
            if(mob.getTarget()!=null && !canAttack(mob,mob.getTarget())) mob.setTarget(null);
            switch(state.kind) {
                case "obsession" -> {
                    for(var enemy:enemies(owner,mob.position(),2.5))
                        game.items().psycho().afflict(enemy,owner,0,0,mob.entityTags().contains("syringe_buffed")?15:5,0);
                }
                case "luck" -> {
                    if(ticks%5==0) { var enemy=attackTarget(mob,owner,mob.getEyePosition(),12);
                        if(enemy!=null) arrow(owner,mob,mob.getEyePosition(),enemy.getEyePosition().subtract(mob.getEyePosition()),mob.entityTags().contains("syringe_buffed")?1.5:.5,2); }
                }
                case "hypocrisy" -> { if(ticks%40==0) mob.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_DAMAGE,1,0)); }
                case "chaos" -> { if(ticks%40==0 && nearest(owner,mob.position(),50)!=null) chaos(owner,mob); }
                case "strangeness" -> {
                    if(ticks%60==0 && nearest(owner,mob.position(),20)!=null) {
                        rays.add(new StrangeRay(state.owner,state.epoch,owner.level(),mob.position().add(0,2.5,0),ticks+20,mob));
                        owner.level().sendParticles(ParticleTypes.SOUL,mob.getX(),mob.getY()+2.5,mob.getZ(),50,.2,.2,.2,0);
                        LbcOverload.sound(owner,mob.position(),"minecraft:block.beacon.activate",1.5f,2,0);
                    }
                }
                default -> { }
            }
        }
        for(int i=rays.size()-1;i>=0;i--) {
            var ray=rays.get(i); var owner=game.server().getPlayerList().getPlayer(ray.owner);
            if(!valid(owner) || owner.level()!=ray.level || !game.items().currentEpoch(ray.epoch)) { rays.remove(i); continue; }
            if(ticks<ray.due || game.items().wands().timeStopped()) continue;
            rays.remove(i);
            var enemy=attackTarget(ray.actor,owner,ray.origin,20);
            if(enemy!=null) { ray.direction=enemy.getEyePosition().subtract(ray.origin).normalize(); activeRays.addLast(ray); }
        }
        // Принятые лучи выполняются порциями; число призывов и ожидающих лучей не ограничивается.
        long deadline=System.nanoTime()+3_000_000L; int pausedRays=0;
        while(!activeRays.isEmpty() && System.nanoTime()<deadline) {
            var ray=activeRays.removeFirst(); var owner=game.server().getPlayerList().getPlayer(ray.owner);
            if(!valid(owner) || owner.level()!=ray.level || !game.items().currentEpoch(ray.epoch)) continue;
            if(game.items().wands().timeStopped()) { activeRays.addLast(ray); if(++pausedRays>=activeRays.size()) break; continue; }
            pausedRays=0;
            if(strangeStep(owner,ray)) activeRays.addLast(ray);
        }
        }finally{advancing=false;}
    }
    /** AFTER_DAMAGE даёт ровно одну реакцию на настоящее попадание, без NBT/HurtTime polling. */
    public void damaged(LivingEntity victim,DamageSource source) {
        if(!(victim instanceof Mob mob)) return;
        var state=mobs.get(mob); var owner=owner(mob);
        if(state==null || !mob.isAlive() || !valid(owner) || paused(mob)) return;
        if(state.kind.equals("luck")) {
            int amount=mob.entityTags().contains("syringe_buffed")?8:4;
            for(int i=0;i<amount;i++) {
                double yaw=owner.getRandom().nextDouble()*Math.PI*2, pitch=Math.toRadians(owner.getRandom().nextDouble()*52-2);
                arrow(owner,mob,mob.position().add(0,1,0),new Vec3(Math.cos(yaw)*Math.cos(pitch),Math.sin(pitch),Math.sin(yaw)*Math.cos(pitch)),2,.5);
            }
        } else if(state.kind.equals("omnipresence")) {
            var enemy=attackTarget(mob,owner,mob.position(),50);
            if(enemy==null) return;
            for(int attempt=0;attempt<24;attempt++) {
                double x=enemy.getX()+owner.getRandom().nextDouble()*20-10,z=enemy.getZ()+owner.getRandom().nextDouble()*20-10;
                if(!owner.level().hasChunkAt(BlockPos.containing(x,enemy.getY(),z))) continue;
                int surface=owner.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(x),(int)Math.floor(z));
                if(mob.randomTeleport(x,surface+1,z,true,BlockTags.CONSUMABLE_DOES_NOT_TELEPORT_TO)) break;
            }
            arrow(owner,mob,mob.getEyePosition(),enemy.getEyePosition().subtract(mob.getEyePosition()),mob.entityTags().contains("syringe_buffed")?3:1,2);
            LbcOverload.sound(owner,mob.position(),"minecraft:entity.fox.teleport",1,1,0);
        }
    }
    private void arrow(ServerPlayer owner,Mob mob,Vec3 origin,Vec3 direction,double damage,double speed) {
        var arrow=new Arrow(owner.level(),owner,new ItemStack(Items.ARROW),null);
        arrow.setPos(origin); arrow.setBaseDamage(damage); arrow.setDeltaMovement(direction.normalize().scale(speed));
        LbcShooters.orient(arrow); track(owner,arrow); owner.level().addFreshEntity(arrow);
        LbcOverload.sound(owner,origin,"minecraft:entity.arrow.shoot",1,1,0);
    }
    private boolean strangeStep(ServerPlayer owner,StrangeRay ray) {
        Vec3 point=ray.origin.add(ray.direction.scale(ray.step++)); var center=BlockPos.containing(point);
        if(!ray.level.hasChunkAt(center) || game.items().magic().suppressedAt(ray.level,point)) return false;
        var damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.STRANGENESS),owner);
        // Каждая из 21 исходных позиций отдельно наносит 1 урона и стирает свой куб 3×3×3.
        ray.level.sendParticles(new DustParticleOptions(0x1f1030,3),point.x,point.y,point.z,3,1.5,1.5,1.5,0);
        ray.level.sendParticles(new DustParticleOptions(0x4c8596,3),true,false,point.x,point.y,point.z,1,0,0,0,0);
        for(var victim:enemies(owner,point,2.5))
            if(game.items().mana().score(victim,"void_resistance").get()<=0) victim.hurtServer(ray.level,damage,1);
        game.items().indirectKills().worldChanges(ray.level,owner,()-> {
            for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))
                if(ray.level.hasChunkAt(pos) && !ray.level.getBlockState(pos).isAir()) ray.level.setBlock(pos,Blocks.AIR.defaultBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
                        | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
                        | net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE);
        });
        return ray.step<21;
    }
    private void chaos(ServerPlayer owner,Mob mob) {
        if(nearest(owner,mob.position(),50)==null) return;
        chaosCast(owner,mob,owner.getRandom().nextInt(10));
    }
    /** Общий исполнитель выбранной способности; случайный выбор остаётся у обычного импульса. */
    void chaosCast(ServerPlayer owner,Mob mob,int spell) {
        var enemy=attackTarget(mob,owner,mob.position(),50); if(enemy==null) return;
        Vec3 origin=mob.getEyePosition(),direction=enemy.getEyePosition().subtract(origin).normalize();
        chaosEffects(owner,mob,spell);
        switch(spell) {
            case 0 -> mob.addEffect(ItemEffectLevels.create(MobEffects.STRENGTH,160,3));
            case 1 -> mob.addEffect(ItemEffectLevels.create(MobEffects.SPEED,160,4));
            case 2 -> mob.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,160,3));
            case 3 -> game.items().wands().launchFire(owner,new LargeFireball(owner.level(),owner,direction,2),origin.add(direction.scale(.5)),direction.scale(2));
            case 4 -> {
                var damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),mob,owner);
                for(var victim:enemies(owner,mob.position(),30)) victim.hurtServer(owner.level(),damage,7);
            }
            case 5 -> {
                var trident=new ThrownTrident(owner.level(),owner,new ItemStack(Items.TRIDENT));
                // Трезубец ванильно наносит 8: исходное поле damage=0.2 игнорировалось (user).
                trident.setPos(origin.add(direction)); trident.setDeltaMovement(direction.scale(7));
                LbcShooters.orient(trident); track(owner,trident); owner.level().addFreshEntity(trident);
            }
            case 6 -> {
                int count=0;
                for(var victim:enemies(owner,mob.position(),50)) if(victim.distanceToSqr(mob)>=2.25 && count++<10)
                    for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                        var fangs=new EvokerFangs(owner.level(),victim.getX()+x,victim.getY(),victim.getZ()+z,0,0,owner);
                        track(owner,fangs); owner.level().addFreshEntity(fangs);
                    }
            }
            case 7 -> {
                var pos=enemy.blockPosition();
                game.items().indirectKills().trapChanges(owner,()-> {
                    owner.level().setBlock(pos,Blocks.STONE.defaultBlockState(),2|16|32);
                    owner.level().setBlock(pos.above(),Blocks.STONE.defaultBlockState(),2|16|32);
                });
                enemy.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,100,127));
            }
            case 8 -> game.items().terra().castAt(owner,origin,direction,LbcTerra.Kind.REGULAR);
            case 9 -> {
                float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
                float pitch=(float)-Math.toDegrees(Math.asin(direction.y));
                game.items().wands().cultistElectro(owner,origin,yaw,pitch);
            }
            default -> { }
        }
    }
    private void chaosEffects(ServerPlayer owner,Mob mob,int spell) {
        String sound=switch(spell) {
            case 0->"minecraft:entity.ender_dragon.growl"; case 1->"minecraft:block.sculk.charge";
            case 2->"minecraft:entity.iron_golem.repair"; case 3->"minecraft:item.firecharge.use";
            case 4->"minecraft:entity.cat.hiss"; case 5->"minecraft:item.trident.throw";
            case 7->"minecraft:block.amethyst_block.break"; case 8->"minecraft:block.amethyst_cluster.break";
            case 9->"minecraft:entity.firework_rocket.twinkle"; default->null;
        };
        if(sound!=null) LbcOverload.sound(owner,mob.position(),sound,1,1,0);
        var particle=switch(spell) {
            case 0->ParticleTypes.CRIT; case 1->ParticleTypes.FIREWORK; case 3->ParticleTypes.FLAME;
            case 4->ParticleTypes.DAMAGE_INDICATOR; default->null;
        };
        if(particle!=null) owner.level().sendParticles(particle,mob.getX(),mob.getY()+1,mob.getZ(),spell==4?300:spell==0?20:10,
                spell==4?10:.3,spell==4?10:.6,spell==4?10:.3,spell==4?1:0);
        if(spell==2) owner.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.IRON_BLOCK.defaultBlockState()),
                mob.getX(),mob.getY()+1,mob.getZ(),30,.3,.6,.3,0);
    }
    void track(ServerPlayer owner,Entity entity) {
        consequences.put(entity,owner.getUUID()); entity.entityTags().add(PROJECTILE); game.items().markCurrentItemEpoch(entity);
    }
    void loaded(Entity entity) {
        if(entity instanceof Mob mob&&entity.entityTags().contains("skywars2.summons.actor")&&!mobs.containsKey(mob)){entity.discard();return;}
        if(entity.entityTags().contains(PROJECTILE)) {
            if(!game.items().currentEpoch(entity)) { entity.discard(); return; }
            var actor=owner(entity);
            if(actor!=null) consequences.put(entity,actor.getUUID());
        }
        if(entity instanceof Projectile projectile) {
            if(game.items().spellSources().persistent(entity))return;
            var owner=owner(projectile.getOwner());
            if(owner!=null) {
                var pickup=projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow?arrow.pickup:null;
                projectile.setOwner(owner);
                if(projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow)arrow.pickup=pickup;
                track(owner,projectile);
            }
        }
    }
    void unloaded(Entity entity) {
        if(entity instanceof Mob mob && mobs.containsKey(mob)) remove(mob);
        if(consequences.remove(entity)!=null && !entity.isRemoved()) entity.discard();
    }
    private void remove(Mob mob) { orders.removed(mob); mobs.remove(mob); if(!mob.isRemoved()) mob.discard(); }
    public void forget(ServerPlayer player) {
        for(var mob:List.copyOf(mobs.keySet())) if(mobs.get(mob).owner.equals(player.getUUID())) remove(mob);
        rays.removeIf(ray->ray.owner.equals(player.getUUID()));
        activeRays.removeIf(ray->ray.owner.equals(player.getUUID()));
        for(var entity:List.copyOf(consequences.keySet())) if(consequences.get(entity).equals(player.getUUID())) { consequences.remove(entity); entity.discard(); }
    }
    public void stop() { orders.stop(); for(var mob:List.copyOf(mobs.keySet())) remove(mob); rays.clear(); activeRays.clear(); for(var entity:List.copyOf(consequences.keySet())) entity.discard(); consequences.clear(); }

    // Узкие подклассы сохраняют native тип/AI и проверяют роль до настоящего melee удара.
    private final class SoulVex extends Vex {
        SoulVex(ServerLevel level) { super(EntityTypes.VEX,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class CultSkeleton extends Skeleton {
        CultSkeleton(ServerLevel level) { super(EntityTypes.SKELETON,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class CultWither extends WitherSkeleton {
        CultWither(ServerLevel level) { super(EntityTypes.WITHER_SKELETON,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class CultZombie extends Zombie {
        CultZombie(ServerLevel level) { super(EntityTypes.ZOMBIE,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class SoulRavager extends net.minecraft.world.entity.monster.Ravager {
        SoulRavager(ServerLevel level) { super(EntityTypes.RAVAGER,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class SoulGhast extends net.minecraft.world.entity.monster.Ghast {
        SoulGhast(ServerLevel level) { super(EntityTypes.GHAST,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class SoulBlaze extends net.minecraft.world.entity.monster.Blaze {
        SoulBlaze(ServerLevel level) { super(EntityTypes.BLAZE,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class SoulGuardian extends net.minecraft.world.entity.monster.Guardian {
        SoulGuardian(ServerLevel level) { super(EntityTypes.GUARDIAN,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class SoulBat extends net.minecraft.world.entity.ambient.Bat {
        SoulBat(ServerLevel level) { super(EntityTypes.BAT,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    private final class SoulHorse extends net.minecraft.world.entity.animal.equine.SkeletonHorse {
        SoulHorse(ServerLevel level) { super(EntityTypes.SKELETON_HORSE,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean canAttack(LivingEntity target) { return LbcSummons.this.canAttack(this,target) && super.canAttack(target); }
        @Override public boolean doHurtTarget(ServerLevel level,Entity target) { return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(level,target); }
    }
    void checkpointBoundary(){if(advancing||preparation!=null&&!preparation.isDone())throw new IllegalStateException("Finish summon advance/bootstrap before checkpointing");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private record Ray(UUID owner,String epoch,ServerLevel level,Vec3 origin,int due,MatchDamageSources.Ref actor,Vec3 direction,int step) { }
        private final MatchSpellCheckpoint.IdentityImage<Mob,Summon> actors;
        private final MatchSpellCheckpoint.IdentityImage<Entity,UUID> children;
        private final LbcSummonOrders.Checkpoint commands;
        private final List<Ray> waiting,active;private final int cadence=ticks;
        Checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();actors=new MatchSpellCheckpoint.IdentityImage<>(mobs,links);children=new MatchSpellCheckpoint.IdentityImage<>(consequences,links);
            commands=orders.checkpoint(links);var aliases=new IdentityHashMap<StrangeRay,Ray>();
            waiting=rays.stream().map(ray->image(ray,links,aliases)).toList();active=activeRays.stream().map(ray->image(ray,links,aliases)).toList();}
        private Ray image(StrangeRay ray,MatchSpellCheckpoint.Links links,IdentityHashMap<StrangeRay,Ray> aliases){return aliases.computeIfAbsent(ray,r->new Ray(r.owner,r.epoch,r.level,r.origin,r.due,links.key(r.actor),r.direction,r.step));}
        private StrangeRay ray(Ray image,MatchSpellCheckpoint.Links links,IdentityHashMap<Ray,StrangeRay> aliases){return aliases.computeIfAbsent(image,r->{
            if(game.server().getLevel(r.level.dimension())!=r.level||!(links.get(r.actor) instanceof Mob actor))throw new IllegalStateException("Reconcile native ray world/actor before spell replay");
            var result=new StrangeRay(r.owner,r.epoch,r.level,r.origin,r.due,actor);result.direction=r.direction;result.step=r.step;return result;});}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restoredActors=actors.restore(links);var restoredChildren=children.restore(links);var command=commands.prepare(links);
            var aliases=new IdentityHashMap<Ray,StrangeRay>();var queued=new ArrayList<StrangeRay>();var running=new ArrayDeque<StrangeRay>();
            for(var image:waiting)queued.add(ray(image,links,aliases));for(var image:active)running.addLast(ray(image,links,aliases));
            return ()->{mobs=restoredActors;consequences=restoredChildren;rays.clear();rays.addAll(queued);activeRays.clear();activeRays.addAll(running);ticks=cadence;command.run();};}
    }

    static boolean cloneRecognizes(Entity e){return e.entityTags().contains("skywars2.summons.actor")||e.entityTags().contains(PROJECTILE);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var summon=source instanceof Mob mob?mobs.get(mob):null;var cause=consequences.get(source);
        if(summon==null&&cause==null)return null;
        var command=orders.cloneImage(source);
        var waiting=rays.stream().filter(r->r.actor==source).map(CloneRay::new).toList();
        var running=activeRays.stream().filter(r->r.actor==source).map(CloneRay::new).toList();
        return new LbcCloneSkills.ActorImage(){
            public Entity create(Entity original,ServerLevel level){
                if(original instanceof SoulVex)return new SoulVex(level);
                if(original instanceof CultSkeleton)return new CultSkeleton(level);
                if(original instanceof CultWither)return new CultWither(level);
                if(original instanceof CultZombie)return new CultZombie(level);
                if(original instanceof SoulRavager)return new SoulRavager(level);
                if(original instanceof SoulGhast)return new SoulGhast(level);
                if(original instanceof SoulBlaze)return new SoulBlaze(level);
                if(original instanceof SoulGuardian)return new SoulGuardian(level);
                if(original instanceof SoulBat)return new SoulBat(level);
                if(original instanceof SoulHorse)return new SoulHorse(level);
                return LbcCloneSkills.ActorImage.super.create(original,level);
            }
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                if(summon!=null){mobs.put((Mob)clone,new Summon(summon.owner,summon.epoch,summon.kind));orders.attach((Mob)clone);}
                if(cause!=null)consequences.put(clone,cause);
                if(command!=null)command.install(original,clone,family);
                for(var r:waiting)rays.add(r.make((Mob)clone));
                for(var r:running)activeRays.addLast(r.make((Mob)clone));
            }
            public void rollback(Entity clone){mobs.remove(clone);consequences.remove(clone);if(clone instanceof Mob m)orders.removed(m);rays.removeIf(r->r.actor==clone);activeRays.removeIf(r->r.actor==clone);}
        };
    }
    private record CloneRay(UUID owner,String epoch,ServerLevel level,Vec3 origin,int due,Vec3 direction,int step){
        CloneRay(StrangeRay r){this(r.owner,r.epoch,r.level,r.origin,r.due,r.direction,r.step);}
        StrangeRay make(Mob actor){var r=new StrangeRay(owner,epoch,level,origin,due,actor);r.direction=direction;r.step=step;return r;}
    }

}
