package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayDeque;

/** Blaze-донор и Fireblaze: общий native fireball, без сущностей-маркеров для турелей. */
public final class LbcBlaze {
    private static final DustParticleOptions TURRET_DUST=new DustParticleOptions(0xfff235,4);
    private static final DustParticleOptions CHICKEN_DUST=new DustParticleOptions(0xfda500,4);
    private static final DustParticleOptions CHICKEN_BURST=new DustParticleOptions(0xfff59a,4);
    public static final String TRIAL_PROJECTILE="skywars2.blaze.trial";
    public static final String KFC_SPIRIT_PROJECTILE="skywars2.kfc.spirit";
    public static boolean isKfcSpirit(Entity entity) {
        return entity instanceof SmallFireball && entity.entityTags().contains(KFC_SPIRIT_PROJECTILE);
    }
    private static final String OWNER_TAG="skywars2.blaze.owner.";
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    private final GameController game;
    private final List<Turret> turrets=new ArrayList<>();
    private final Map<UUID,ServerPlayer> heads=new HashMap<>();
    private final Map<Blaze,UUID> blazes=new HashMap<>();
    private final ArrayDeque<Terrain> terrain=new ArrayDeque<>();
    private int ticks;
    private record Turret(ServerPlayer owner,ServerLevel level,Vec3 position,String epoch,int remaining,double speed,DustParticleOptions dust,boolean wild) { }
    private record Terrain(ServerPlayer owner,ServerLevel level,ArrayDeque<BlockAreaWork> areas) { }

    LbcBlaze(GameController game) { this.game=game; }
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform) {
        for(int i=turrets.size()-1;i>=0;i--) {
            var turret=turrets.get(i);
            if(turret.level!=level) continue;
            Vec3 next=transform.apply(turret.position);
            if(next==null) turrets.remove(i);
            else if(!next.equals(turret.position))
                turrets.set(i,new Turret(turret.owner,turret.level,next,turret.epoch,turret.remaining,turret.speed,turret.dust,turret.wild));
        }
    }

    public int count() { return turrets.size()+heads.size()+terrain.size()+blazes.size(); }
    int turretCount() { return turrets.size(); }
    int terrainCount() { return terrain.size(); }
    int headCharges(ServerPlayer owner) { return game.items().mana().hudValue(owner,"blaze"); }
    /** Общая точка выдачи ресурса для будущих еды/предметов; прямой scoreboard grant не запускает статус. */
    public void addHeadCharges(ServerPlayer owner,int amount) {
        if(amount<=0 || !valid(owner) || game.items().magic().rejectCustomEffect(owner)) return;
        heads.put(owner.getUUID(),owner); game.items().mana().score(owner,"blaze").add(amount);
    }
    private boolean valid(ServerPlayer owner) { return game.items().mana().visible(owner); }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")
                || game.items().frost().isFrozen(owner) || game.items().magic().suppressed(owner);
    }

    /** Используемую руку обрабатывает прежний LbcWands dispatch, бонус каста выдаётся ровно один раз. */
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean blaze,boolean upgraded) {
        if(!valid(owner) || paused(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        var mana=game.items().mana();
        if(upgraded && owner.isShiftKeyDown()) {
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=Math.floorMod(tag.getIntOr("mode",1),blaze?2:4)+1;
            tag.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            owner.sendSystemMessage(Component.translatable(blaze?(mode==1?"mode_default":"mode_blaze_head"):
                    switch(mode) { case 2->"mode_mega"; case 3->"mode_pillars"; case 4->"mode_kiuas"; default->"mode_world"; }));
            return true;
        }
        int cost=mana.score(ScoreHolder.forNameOnly(blaze?"blaze_wand_mana_use":"fireblaze_wand_mana_use"),"lbc.math").get();
        if(!mana.trySpendWand(owner,cost)) return false;
        int mode=upgraded?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()
                .getIntOr("mode",1),1,blaze?2:4):0;
        if(blaze) {
            if(mode==2 && !game.items().magic().rejectCustomEffect(owner)) {
                addHeadCharges(owner,7);
                LbcOverload.sound(owner,"minecraft:entity.blaze.hurt",1,1,0);
            } else if(mode!=2) turret(owner,cost);
            if(!upgraded && owner.isShiftKeyDown() && LbcAcademy.trial(stack)) {
                // (user) Это настоящая враждебная волна, не приручение и не временные мобы.
                for(int i=0;i<20;i++) {
                    var mob=new Blaze(EntityTypes.BLAZE,owner.level()); mob.setPos(owner.position());
                    mob.finalizeSpawn(owner.level(),owner.level().getCurrentDifficultyAt(mob.blockPosition()),
                            net.minecraft.world.entity.EntitySpawnReason.COMMAND,null);
                    mob.entityTags().add(TRIAL_PROJECTILE); own(mob,owner); owner.level().addFreshEntity(mob);
                }
                game.items().academy().ready(owner,hand,stack);
            }
        } else {
            if(mode==0) volley(owner,30,false);
            else if(mode==2) volley(owner,100,true);
            else global(owner,mode);
            if(!upgraded && owner.isShiftKeyDown() && LbcAcademy.trial(stack)) {
                // (user) Все 14 шаров и исходные мощности сохраняются, владельца не теряем.
                for(int i=1;i<=14;i++) {
                    int axis=(i-1)%6; double distance=10+i*10;
                    Vec3 offset=switch(axis) {
                        case 0->new Vec3(0,distance,0); case 1->new Vec3(0,0,distance);
                        case 2->new Vec3(distance,0,0); case 3->new Vec3(0,-distance,0);
                        case 4->new Vec3(0,0,-distance); default->new Vec3(-distance,0,0);
                    };
                    Vec3 velocity=offset.normalize().scale(-1);
                    var shot=new LargeFireball(owner.level(),owner,velocity,i); shot.entityTags().add(TRIAL_PROJECTILE);
                    game.items().wands().launchFire(owner,shot,owner.position().add(offset),velocity);
                }
                game.items().academy().ready(owner,hand,stack);
            }
        }
        game.items().wands().wandCasted(owner);
        return true;
    }

    /** Первое пересечение блок/хитбокс заменяет старую рекурсию из 200 отдельных команд. */
    private void turret(ServerPlayer owner,int cost) {
        Vec3 from=owner.getEyePosition(),to=from.add(owner.getLookAngle().scale(200));
        double stop=game.items().magic().suppressionFraction(owner.level(),from,to);
        if(Double.isFinite(stop)) to=from.lerp(to,stop);
        var hit=game.items().wands().firstHit(owner,from,to);
        Vec3 end=hit==null?to:hit.getLocation();
        double length=from.distanceTo(end);
        for(double d=0;d<=length;d+=1) {
            Vec3 p=from.add(owner.getLookAngle().scale(d));
            owner.level().sendParticles(ParticleTypes.FLAME,p.x,p.y,p.z,2,.5,.5,.5,0);
        }
        if(hit==null || hit instanceof BlockHitResult wall && !owner.level().hasChunkAt(wall.getBlockPos())
                || Double.isFinite(stop) && end.distanceToSqr(from)>=to.distanceToSqr(from)-1e-6) return;
        // Как в исходнике: приручение возвращает цену, но не отменяет создание огненной турели.
        var blazes=owner.level().getEntitiesOfClass(Blaze.class,new AABB(end,end).inflate(5),
                e->e.isAlive() && e.position().distanceToSqr(end)<=25);
        if(!blazes.isEmpty()) {
            var board=game.server().getScoreboard();
            for(var blaze:blazes) {
                board.removePlayerFromTeam(blaze.getScoreboardName());
                if(owner.getTeam()!=null) board.addPlayerToTeam(blaze.getScoreboardName(),owner.getTeam());
                blaze.entityTags().remove(TRIAL_PROJECTILE); own(blaze,owner);
                blaze.setTarget(null);
            }
            game.items().mana().add(owner,cost);
            game.items().mana().award(owner,"lbc:true_advancements/staffs/blaze");
            LbcOverload.sound(owner,end,"minecraft:entity.villager.trade",1,1,0);
        }
        turrets.add(new Turret(owner,owner.level(),end.add(0,1,0),game.items().epoch(),5,.7,TURRET_DUST,false));
        LbcOverload.sound(owner,end,"minecraft:entity.blaze.ambient",1,1.3f,0);
    }

    /** KFC uses the same one-second pulse and owner/fire lifecycle, with three native shots at speed 1.6. */
    void chickenTurret(ServerPlayer owner,Vec3 position) {
        turrets.add(new Turret(owner,owner.level(),position,game.items().epoch(),3,1.6,CHICKEN_DUST,true));
        owner.level().sendParticles(CHICKEN_BURST,true,false,position.x,position.y-1,position.z,10,.2,.5,.2,0);
    }

    private boolean target(ServerPlayer owner,LivingEntity entity) {
        return game.items().wands().target(owner,entity) && !entity.entityTags().contains("strict_map_object")
                && !entity.entityTags().contains("chick");
    }
    private LivingEntity nearest(ServerPlayer owner,Vec3 point,boolean turret,boolean wild,List<LivingEntity> candidates) {
        LivingEntity nearest=null; double distance=Double.POSITIVE_INFINITY;
        // Раз в 10/20 тиков на активную способность, а не каждый тик на каждую сущность мира.
        for(var living:candidates) if(living.level()==owner.level() && (wild ? game.items().wands().validTarget(living) : target(owner,living))
                && (!turret || !living.entityTags().contains("nomagic_active"))) {
            double d=living.position().distanceToSqr(point);
            if(d<distance) { distance=d; nearest=living; }
        }
        return nearest;
    }
    private void volley(ServerPlayer owner,int radius,boolean mega) {
        for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(radius),
                e->target(owner,e) && e.position().distanceToSqr(owner.position())<=radius*(double)radius))
            randomFire(owner,target.position().add(0,1,0),mega);
        LbcOverload.sound(owner,"minecraft:item.firecharge.use",1,1,0);
    }
    private void randomFire(ServerPlayer owner,Vec3 point,boolean mega) {
        int direction=owner.getRandom().nextInt(9);
        Vec3 offset=switch(direction) {
            case 0->new Vec3(6,0,0); case 1->new Vec3(-6,0,0);
            case 2->new Vec3(0,0,6); case 3->new Vec3(0,0,-6);
            case 4->new Vec3(4,0,4); case 5->new Vec3(-4,0,-4);
            case 6->new Vec3(4,0,-4); case 7->new Vec3(-4,0,4); default->new Vec3(0,6,0);
        };
        Vec3 velocity=offset.scale(direction>=4 && direction<=7?-.5:-.25);
        Projectile projectile=mega?new LargeFireball(owner.level(),owner,velocity,5):new SmallFireball(EntityTypes.SMALL_FIREBALL,owner.level());
        game.items().wands().launchFire(owner,projectile,point.add(offset),velocity);
        LbcOverload.sound(owner,point.add(offset),"minecraft:item.firecharge.use",1,1,0);
    }

    /** Глобальные режимы выбирают живые цели один раз на каст; отдельного тикового обхода мира нет. */
    private void global(ServerPlayer owner,int mode) {
        var areas=new ArrayDeque<BlockAreaWork>();
        var targets=new ArrayList<LivingEntity>();
        for(var entity:owner.level().getAllEntities()) if(entity instanceof LivingEntity living && target(owner,living)) targets.add(living);
        for(var living:targets) {
            if(mode==1) randomFire(owner,living.position().add(0,1,0),false);
            else if(mode==3) {
                var center=BlockPos.containing(living.position().add(0,1,0));
                center=center.offset(owner.getRandom().nextInt(61)-30,0,owner.getRandom().nextInt(61)-30);
                enqueue(owner,areas,new BlockAreaWork.Bounds(center.getX(),0,center.getZ(),center.getX(),319,center.getZ()),true);
            }
        }
        if(mode==4) {
            // Сначала все маленькие области, затем до десяти пар большой огонь -> лава: исходный порядок fill.
            for(int i=0;i<Math.min(100,targets.size());i++) {
                var center=targets.get(i).blockPosition();
                enqueue(owner,areas,new BlockAreaWork.Bounds(center.getX()-1,center.getY(),center.getZ()-1,
                        center.getX()+1,center.getY()+1,center.getZ()+1),false);
                var position=targets.get(i).position();
                owner.level().sendParticles(ParticleTypes.FLAME,true,false,position.x,position.y,position.z,3,.2,.2,.2,0);
            }
            for(int i=0;i<Math.min(10,targets.size());i++) {
                var living=targets.get(i);
                var center=living.blockPosition();
                enqueue(owner,areas,new BlockAreaWork.Bounds(center.getX()-15,center.getY()-15,center.getZ()-15,
                        center.getX()+15,center.getY()+15,center.getZ()+15),false);
                enqueue(owner,areas,new BlockAreaWork.Bounds(center.getX()-1,center.getY()-1,center.getZ()-1,
                        center.getX()+1,center.getY()+1,center.getZ()+1),true);
            }
        }
        if(!areas.isEmpty()) terrain.add(new Terrain(owner,owner.level(),areas));
        if(mode==3 || mode==4) owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,-1,0));
        if(mode!=4) LbcOverload.sound(owner,mode==3?"minecraft:block.lava.ambient":"minecraft:item.firecharge.use",1,1,0);
    }

    /** Magnum's exact 3x3x3 incendiary fill, using the shared queued fire conversion. */
    void incendiaryCube(ServerPlayer owner,Vec3 impact) {
        var center=BlockPos.containing(impact);
        var areas=new ArrayDeque<BlockAreaWork>();
        enqueue(owner,areas,new BlockAreaWork.Bounds(center.getX()-1,center.getY()-1,center.getZ()-1,
                center.getX()+1,center.getY()+1,center.getZ()+1),false);
        terrain.add(new Terrain(owner,owner.level(),areas));
    }

    private void enqueue(ServerPlayer owner,ArrayDeque<BlockAreaWork> areas,BlockAreaWork.Bounds bounds,boolean lava) {
        var level=owner.level();
        var work=new BlockAreaWork(level,lava?s->!s.equals(Blocks.LAVA.defaultBlockState()):LbcKiuas::kiuEligible,(pos,state)-> {
            var result=lava?Blocks.LAVA.defaultBlockState():LbcKiuas.kiuResult(level,pos,state);
            if(result.is(Blocks.LAVA)) level.scheduleTick(pos.immutable(),net.minecraft.world.level.material.Fluids.LAVA,
                    net.minecraft.world.level.material.Fluids.LAVA.getTickDelay(level));
            else if(result.is(Blocks.FIRE)) level.scheduleTick(pos.immutable(),Blocks.FIRE,30);
            return result;
        },FLAGS,true);
        work.move(bounds,true); areas.add(work);
    }

    private void own(Blaze mob,ServerPlayer owner) {
        mob.entityTags().removeIf(tag->tag.startsWith(OWNER_TAG)); mob.entityTags().add(OWNER_TAG+owner.getUUID());
        game.items().markCurrentItemEpoch(mob); blazes.put(mob,owner.getUUID());
    }
    /** Nether Plug использует уже существующие AI-фильтры и владельца подчинённого ифрита. */
    void summonFriend(ServerPlayer owner) {
        var mob=new Blaze(EntityTypes.BLAZE,owner.level());
        var tag=new net.minecraft.nbt.CompoundTag(); tag.putString("DeathLootTable","minecraft:empty");
        mob.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess(),tag));
        mob.setPos(owner.position().add(0,2,0));
        mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(200);
        mob.setHealth(20); mob.entityTags().add("blaze_friend"); own(mob,owner);
        owner.level().addFreshEntity(mob);
    }
    ServerPlayer owner(Entity entity) {
        var id=entity instanceof Blaze blaze?blazes.get(blaze):null;
        return id==null?null:game.server().getPlayerList().getPlayer(id);
    }
    boolean friendly(ServerPlayer player,Entity entity) {
        return entity instanceof Blaze mob && !mob.entityTags().contains(TRIAL_PROJECTILE)
                && player.getUUID().equals(blazes.get(mob));
    }
    /** Подчинённый ифрит не выбирает хозяина/союзника; враждебная испытательная волна остаётся враждебной. */
    public boolean canTarget(Blaze mob,LivingEntity target) {
        var owner=owner(mob);
        if(target==null || owner==null) return true;
        boolean hostile=mob.entityTags().contains(TRIAL_PROJECTILE);
        return hostile && target==owner ? valid(owner) : game.items().wands().target(owner,target,hostile);
    }
    /** Нативные ифриты сохраняют свой AI; их снарядам передаётся реальный автор до регистрации трекера. */
    void loaded(Entity entity) {
        if(entity instanceof Blaze blaze) for(String tag:entity.entityTags()) if(tag.startsWith(OWNER_TAG)) {
            if(!game.items().currentEpoch(entity)) { entity.discard(); return; }
            try { blazes.put(blaze,UUID.fromString(tag.substring(OWNER_TAG.length()))); } catch(IllegalArgumentException ignored) { }
            break;
        }
        if(entity instanceof Projectile shot && shot.getOwner() instanceof Blaze mob) {
            var owner=owner(mob);
            if(owner!=null) {
                if(mob.entityTags().contains(TRIAL_PROJECTILE)) shot.entityTags().add(TRIAL_PROJECTILE);
                game.items().wands().adoptFire(owner,shot);
            }
        }
    }
    void unloaded(Entity entity) { if(entity instanceof Blaze blaze) blazes.remove(blaze); }

    /** Время способности останавливается вместе с общей магией; выгруженные чанки не загружаются. */
    public void tick() {
        int now=++ticks;
        // Один снимок живых кандидатов на общий импульс, независимо от числа турелей и снарядов.
        List<LivingEntity> candidates=now%10==0 && (!turrets.isEmpty() || !heads.isEmpty())
                ? game.items().lifetimes().loadedTargets() : List.of();
        if(now%20==0) for(int i=turrets.size()-1;i>=0;i--) {
            var turret=turrets.get(i); var owner=turret.owner;
            if(!game.items().currentEpoch(turret.epoch) || !valid(owner) || owner.level()!=turret.level
                    || !turret.level.hasChunkAt(BlockPos.containing(turret.position))
                    || game.items().magic().suppressedAt(turret.level,turret.position)) { turrets.remove(i); continue; }
            if(paused(owner)) continue;
            var target=nearest(owner,turret.position,true,turret.wild,candidates);
            if(target!=null) {
                var projectile=new SmallFireball(EntityTypes.SMALL_FIREBALL,turret.level);
                if(turret.wild) projectile.entityTags().add(KFC_SPIRIT_PROJECTILE);
                Vec3 velocity=target.getEyePosition().subtract(turret.position).normalize().scale(turret.speed);
                game.items().wands().launchFire(owner,projectile,turret.position,velocity);
                LbcOverload.sound(owner,turret.position,"minecraft:entity.blaze.shoot",1,1,0);
            }
            turret.level.sendParticles(turret.dust,true,false,turret.position.x,turret.position.y,turret.position.z,10,.2,.5,.2,0);
            if(turret.remaining<=1) turrets.remove(i);
            else turrets.set(i,new Turret(owner,turret.level,turret.position,turret.epoch,turret.remaining-1,turret.speed,turret.dust,turret.wild));
        }
        if(now%10==0) for(var owner:List.copyOf(heads.values())) {
            if(!valid(owner) || headCharges(owner)<=0 || game.items().magic().rejectCustomEffect(owner)) { clearEffects(owner); continue; }
            if(paused(owner)) continue;
            var target=nearest(owner,owner.getEyePosition(),false,false,candidates);
            Vec3 velocity=target==null?owner.getLookAngle().scale(.7):target.getEyePosition().subtract(owner.getEyePosition()).normalize().scale(.7);
            game.items().wands().launchFire(owner,new SmallFireball(EntityTypes.SMALL_FIREBALL,owner.level()),owner.getEyePosition(),velocity);
            owner.level().sendParticles(ParticleTypes.FLAME,owner.getX(),owner.getEyeY(),owner.getZ(),5,.4,.4,.4,0);
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,40,0,false,false));
            game.items().mana().score(owner,"blaze").add(-1);
            LbcOverload.sound(owner,"minecraft:entity.blaze.shoot",1,1,0);
            if(headCharges(owner)<=0) heads.remove(owner.getUUID());
        }
        long deadline=System.nanoTime()+3_000_000L; int left=65536,stalledJobs=0;
        // Оплаченные области не теряются. Порядок fill сохраняется: завершающая лава идёт после огня,
        // иначе незаконченный большой куб мог бы позже превратить её обратно в fire.
        while(left>0 && !terrain.isEmpty() && System.nanoTime()<deadline) {
            var job=terrain.removeFirst();
            if(!valid(job.owner) || job.owner.level()!=job.level) continue;
            if(paused(job.owner)) {
                terrain.addLast(job); if(++stalledJobs>=terrain.size()) break; continue;
            }
            var area=job.areas.getFirst();
            int[] processed={0};
            int allowance=Math.min(256,left);
            game.items().indirectKills().trapChanges(job.owner,()->processed[0]=area.step(game,job.owner,allowance,deadline));
            int used=processed[0]; left-=Math.max(1,used);
            if(!area.hasWork()) job.areas.removeFirst();
            if(!job.areas.isEmpty()) terrain.addLast(job);
            if(used==0) { if(++stalledJobs>=Math.max(1,terrain.size())) break; }
            else stalledJobs=0;
        }
    }
    public void clearEffects(ServerPlayer owner) { heads.remove(owner.getUUID()); game.items().mana().score(owner,"blaze").set(0); }
    /** Cancel pending shared turrets before a respawn can make the owner valid at the next pulse. */
    public void forget(ServerPlayer owner) {
        turrets.removeIf(turret->turret.owner.getUUID().equals(owner.getUUID()));
        terrain.removeIf(job->job.owner.getUUID().equals(owner.getUUID()));
        clearEffects(owner);
    }
    public void stop() {
        for(var owner:List.copyOf(heads.values())) clearEffects(owner);
        turrets.clear(); terrain.clear();
        var pending=List.copyOf(blazes.keySet()); blazes.clear(); for(var mob:pending) mob.discard();
        ticks=0;
    }
}
