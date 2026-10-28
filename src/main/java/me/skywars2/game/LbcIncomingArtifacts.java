package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Source incoming criterion artifacts and both independent Sand Amulet variants. */
public final class LbcIncomingArtifacts {
    private static final String ACTOR="skywars2.incoming_artifact.actor";
    private static final String WILD="skywars2.incoming_artifact.wild";
    private static final TagKey<EntityType<?>> ATTACKS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:attacks"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    private final GameController game;
    private final Map<Entity,UUID> actors=new IdentityHashMap<>();
    private final Map<UUID,UUID> unloaded=new HashMap<>();
    private final List<Column> columns=new ArrayList<>();
    private final Set<Terrain> terrain=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Deque<Admission> attempts=new ArrayDeque<>();
    private int phase;
    private long criteria;
    private record Admission(ServerPlayer owner,boolean allowed,int current,int old,boolean currentContainer){}
    public LbcIncomingArtifacts(GameController game){this.game=game;}

    private boolean admitted(ServerPlayer p,boolean active){
        if(p==null||!p.isAlive()||p.isRemoved()||p.level()!=game.maps().level()||!game.canUseGameplayItems(p)
                ||game.stage()==GameController.Stage.ENDING||!game.items().wands().validRoleTarget(p,false)
                ||game.items().wands().inputTimeStopped(p)||game.items().timeStop().stopped(p.level()))return false;
        var dimension=p.level().dimension().identifier();
        return !dimension.equals(Identifier.parse("minecraft:nexus"))&&!dimension.equals(Identifier.parse("minecraft:imprinted"))
                &&(!active||!game.items().actionsBlocked(p));
    }
    private Admission admission(ServerPlayer owner){
        int current=0,old=0;boolean container=false;
        for(int i=0;i<=40;i++){
            var stack=owner.getInventory().getItem(i);if(stack.isEmpty())continue;
            var d=LbcFartus.data(stack);int type=d.getIntOr("type",0);
            if(d.getIntOr("mystical",0)==4){if(i<36)container=true;if(type>=1&&type<=6)current|=1<<type;}
            if(d.getIntOr("mystical_old",0)==4&&type>=1&&type<=6)old|=1<<type;
        }
        return new Admission(owner,admitted(owner,false),current,old,container);
    }
    /** Carries pre-death role admission to the real criterion; rejected native attempts never dispatch. */
    public boolean withIncomingAttempt(ServerPlayer owner,BooleanSupplier hurt){
        attempts.push(admission(owner));
        try{return hurt.getAsBoolean();}finally{attempts.pop();}
    }
    public long criterionCount(){return criteria;}
    /** Exact native ENTITY_HURT_PLAYER arguments, before advancement listeners dispatch. */
    public void incoming(ServerPlayer owner,DamageSource source,float originalDamage,float actualDamage,boolean blocked){
        var a=!attempts.isEmpty()&&attempts.peek().owner==owner?attempts.peek():admission(owner);
        if(!a.allowed)return;
        criteria++;
        // The separate shield criterion admits the source entity's original tag, without a magic gate.
        if((a.current&2)!=0&&source.getEntity()!=null&&source.getEntity().getType().builtInRegistryHolder().is(ATTACKS))shield(owner);
        // container.* outer predicate is narrower than Inventory NBT; armor/offhand alone do not open it.
        if(a.currentContainer&&!game.items().magic().beaconSuppressed(owner.position())){
            if((a.current&4)!=0)explode(owner);
            if((a.current&8)!=0)corrosion(owner);
            if((a.current&16)!=0)weakall(owner);
            if((a.current&32)!=0)speed(owner);
            if((a.current&64)!=0)wolf(owner);
        }
        if((a.old&2)!=0)effect(owner,MobEffects.RESISTANCE,20,3,true);
        // No artificial reentry guard: native instant-effect clock and hurt cooldown own acceptance.
        if((a.old&4)!=0)effect(owner,MobEffects.INSTANT_DAMAGE,1,0,true);
        if((a.old&8)!=0)effect(owner,MobEffects.SPEED,200,1,true);
        if((a.old&16)!=0)effect(owner,MobEffects.SLOWNESS,200,2,true);
        if((a.old&32)!=0)effect(owner,MobEffects.STRENGTH,200,1,true);
        if((a.old&64)!=0)effect(owner,MobEffects.WEAKNESS,200,0,true);
    }
    private static void effect(ServerPlayer p,Holder<MobEffect> type,int ticks,int amplifier,boolean hidden){
        p.addEffect(ItemEffectLevels.create(type,ticks,amplifier,false,!hidden,!hidden),p);
    }
    private void shield(ServerPlayer p){
        effect(p,MobEffects.RESISTANCE,40,4,true);
        var option=new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.BARRIER.defaultBlockState());
        for(double y:new double[]{.5,1.5})p.level().sendParticles(option,true,false,p.getX(),p.getY()+y,p.getZ(),1,0,0,0,0);
        LbcOverload.sound(p,"minecraft:block.anvil.use",2,2,0);
    }
    private void explode(ServerPlayer p){
        for(var target:p.level().getEntitiesOfClass(ServerPlayer.class,p.getBoundingBox().inflate(7),q->q.distanceToSqr(p)<=49
                &&game.items().wands().validRoleTarget(q,false)))effect(target,MobEffects.RESISTANCE,20,4,false);
        effect(p,MobEffects.SLOW_FALLING,100,0,true);
        var tnt=new PrimedTnt(p.level(),p.getX(),p.getY(),p.getZ(),p);tnt.setFuse(0);tnt.setDeltaMovement(Vec3.ZERO);track(p,tnt);
    }
    private void corrosion(ServerPlayer p){
        var level=p.level();var origin=p.blockPosition().offset(-5,-8,-5);
        queue(p,origin,origin.offset(11,11,11),()->{
            var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:air11x11")).orElseThrow();
            template.placeInWorld(level,origin,origin,new StructurePlaceSettings().setIgnoreEntities(true)
                    .addProcessor(new BlockRotProcessor(.5f))
                    .setRandom(net.minecraft.world.level.block.entity.StructureBlockEntity.createRandom(0)),
                    net.minecraft.world.level.block.entity.StructureBlockEntity.createRandom(0),2);
        });
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,p.getX(),p.getY(),p.getZ(),30,1,1,1,1);
        LbcOverload.sound(p,"minecraft:block.portal.trigger",1,2,0);
    }
    private void weakall(ServerPlayer p){
        for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(7),q->q.distanceToSqr(p)<=49
                &&(q==p?!LbcGreed.active(q):game.items().wands().projectileTarget(p,q))))
            target.addEffect(ItemEffectLevels.create(MobEffects.WEAKNESS,60,9),p);
        p.level().sendParticles(ParticleTypes.HAPPY_VILLAGER,p.getX(),p.getY(),p.getZ(),0,5,5,5,30);
        LbcOverload.sound(p,"minecraft:entity.villager.trade",1,1,0);
    }
    private static void speed(ServerPlayer p){
        if(p.hasEffect(MobEffects.FIRE_RESISTANCE))effect(p,MobEffects.REGENERATION,200,1,false);
        if(p.hasEffect(MobEffects.RESISTANCE))effect(p,MobEffects.FIRE_RESISTANCE,800,0,false);
        if(p.hasEffect(MobEffects.RESISTANCE))effect(p,MobEffects.FIRE_RESISTANCE,200,1,false);
        if(p.hasEffect(MobEffects.SPEED))effect(p,MobEffects.RESISTANCE,100,0,false);
        if(p.hasEffect(MobEffects.STRENGTH))effect(p,MobEffects.SPEED,100,2,false);
        effect(p,MobEffects.STRENGTH,100,0,false);
        p.level().sendParticles(ParticleTypes.SWEEP_ATTACK,p.getX(),p.getY()+1,p.getZ(),2,1,1,1,0);
        LbcOverload.sound(p,"minecraft:entity.ender_dragon.growl",.5f,2,0);
    }
    private void wolf(ServerPlayer p){
        var wolf=EntityTypes.WOLF.create(p.level(),EntitySpawnReason.COMMAND);
        if(wolf!=null){
            load(wolf,"{DeathLootTable:\"minecraft:empty\",Sitting:false,CollarColor:14}");
            wolf.setPos(p.position());wolf.setOwner(p);wolf.setTame(true,false);wolf.setOrderedToSit(false);
            wolf.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(200);
            wolf.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.5);
            wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);track(p,wolf);
        }
        p.level().sendParticles(new DustParticleOptions(0x999999,1),p.getX(),p.getY(),p.getZ(),10,.5,.5,.5,.02);
        LbcOverload.sound(p,"minecraft:entity.wolf.growl",1,1,0);
    }
    private boolean inventory(ServerPlayer p,String flag,boolean passive){
        for(int i=0;i<=40;i++)if((!passive||i<36||i==40)&&!p.getInventory().getItem(i).isEmpty()
                &&LbcFartus.data(p.getInventory().getItem(i)).getIntOr(flag,0)==1)return true;
        return false;
    }
    private boolean magicHit(ServerPlayer p){
        return !p.entityTags().contains("nomagic_active")&&!game.items().magic().suppressed(p)
                &&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());
    }
    /** Existing source swords accepted event supplies the actual victim, never a HurtTime rescan. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer p)||source.is(TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only")))
                ||!admitted(p,true)||!game.items().wands().validRoleTarget(victim,true))return;
        if(inventory(p,"gaara",false)&&magicHit(p)&&p.distanceToSqr(victim)<=100)sand(p,p.getRandom().nextInt(3)+1);
        if(inventory(p,"gaara_old",false))volley(p,true);
    }
    void sand(ServerPlayer p,int branch){
        switch(branch){case 1->volley(p,false);case 2->wall(p);case 3->cages(p);default->throw new IllegalArgumentException("sand branch");}
        for(int i=0;i<2;i++)LbcOverload.sound(p,"minecraft:block.sand.place",1,1,0);
    }
    private static Vec3 local(ServerPlayer p,Vec3 origin,double left,double up,double forward,boolean flat){
        var ahead=Vec3.directionFromRotation(flat?0:p.getXRot(),p.getYRot());
        var leftVector=Vec3.directionFromRotation(0,p.getYRot()-90);
        var above=ahead.cross(leftVector);
        return origin.add(leftVector.scale(left)).add(above.scale(up)).add(ahead.scale(forward));
    }
    private void volley(ServerPlayer p,boolean old){
        // Native execute positioned ~ ~6 ~ resets the caller's eyes anchor to FEET.
        var origin=old?p.position():p.position().add(0,6,0);
        for(int row=0;row<(old?4:3);row++){
            int width=old?row:row+1;double forward=old?row+1.5:row+2.5;
            // Preserve the source center-first/descending lateral order, including its missing current first center.
            var offsets=new ArrayList<Integer>();
            if(old&&row==1){offsets.add(0);offsets.add(1);offsets.add(-1);}
            else for(int left=width;left>=-width;left--)if(old||row!=0||left!=0)offsets.add(left);
            for(int left:offsets){
                var point=local(p,origin,left,old?5:0,forward,!old);
                var actor=EntityTypes.FALLING_BLOCK.create(p.level(),EntitySpawnReason.COMMAND);if(actor==null)continue;
                load(actor,"{BlockState:\"minecraft:sand\",Time:1,DropItem:false,HurtEntities:true,FallHurtMax:5,fall_distance:5.0f,FallHurtAmount:"
                        +(old?"2.0f":"5.0f")+",Motion:[0.0d,"+(old?"0.0d":"-5.0d")+",0.0d]}");
                actor.setPos(point);track(p,actor);
            }
        }
        if(old)for(int i=0;i<7;i++)LbcOverload.sound(p,"minecraft:block.sand.place",10,1,15);
    }
    private void wall(ServerPlayer p){
        var level=p.level();var center=p.blockPosition();
        queue(p,center.offset(-8,-1,-8),center.offset(8,4,8),()->{
            LbcDropBlocks.fill(level,center.offset(-8,-1,-8),center.offset(8,4,8),Blocks.SANDSTONE,true);
            LbcDropBlocks.fill(level,center.offset(-7,0,-7),center.offset(7,4,7),Blocks.AIR,true);
        });
        if(!level.getEntitiesOfClass(ServerPlayer.class,p.getBoundingBox().inflate(8),q->q!=p&&q.distanceToSqr(p)>=.0001
                &&q.distanceToSqr(p)<=64&&game.items().wands().target(p,q)).isEmpty())
            game.items().mana().award(p,"lbc:true_advancements/accesories/sand_amulet");
    }
    private void cages(ServerPlayer p){
        var nearby=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),q->q.getType().builtInRegistryHolder().is(MOBS)
                &&q.distanceToSqr(p)>=.0001&&q.distanceToSqr(p)<=100&&game.items().wands().projectileTarget(p,q));
        for(var q:nearby)if(q.distanceToSqr(p)>=2.2*2.2)column(p,q.position());
        if(nearby.stream().anyMatch(q->q.distanceToSqr(p)<=2.2*2.2)){
            var center=p.blockPosition();var level=p.level();
            queue(p,center.offset(-4,-4,-4),center.offset(4,4,4),()->{
                solidFill(level,center.offset(-4,-4,-4),center.offset(4,4,4),Blocks.RED_SANDSTONE.defaultBlockState());
                solidFill(level,center.offset(-2,0,-2),center.offset(2,3,2),Blocks.RED_SAND.defaultBlockState());
            });
            for(var target:nearby)if(target.distanceToSqr(p)<=25){
                target.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,100,0),p);
                target.hurtServer(level,new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(net.minecraft.world.damagesource.DamageTypes.ARROW),p),5);
                LbcOverload.sound(p,"minecraft:block.sand.break",1,1,0);
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST,Blocks.SAND.defaultBlockState()),p.getX(),p.getY()+1,p.getZ(),100,.5,.5,.5,.5);
        }
    }
    private static void solidFill(ServerLevel level,BlockPos from,BlockPos to,BlockState state){
        long volume=(long)(to.getX()-from.getX()+1)*(to.getY()-from.getY()+1)*(to.getZ()-from.getZ()+1);
        if(level.isOutsideBuildHeight(from)||level.isOutsideBuildHeight(to)||volume>level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS))return;
        for(var pos:BlockPos.betweenClosed(from,to))if(level.getBlockState(pos).is(SOLID))LbcDropBlocks.setBlock(level,pos,state,true);
    }
    private void column(ServerPlayer p,Vec3 point){
        var center=BlockPos.containing(point);var level=p.level();queue(p,center.below(),center.above(3),()->{
            var column=new Column(p,level,point);columns.add(column);
            if(!level.isOutsideBuildHeight(center)&&!level.isOutsideBuildHeight(center.above(3))
                    &&level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS)>=4)
                for(int y=0;y<=3;y++)column.place(center.above(y),Blocks.RED_SAND.defaultBlockState());
            if(level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS)>=1)
                column.place(center.below(),Blocks.CHISELED_RED_SANDSTONE.defaultBlockState());
        });
    }
    private final class Column {
        final UUID author;final ServerLevel level;final BlockPos center;final Vec3 point;final String epoch=game.items().epoch();
        final Map<BlockPos,BlockState> written=new LinkedHashMap<>();int remaining=110;
        Column(ServerPlayer p,ServerLevel level,Vec3 point){author=p.getUUID();this.level=level;this.point=point;center=BlockPos.containing(point);}
        void place(BlockPos pos,BlockState state){
            if(level.isOutsideBuildHeight(pos)||level.isDebug())return;
            level.destroyBlock(pos,true);LbcDropBlocks.setBlock(level,pos,state,false);
            if(level.getBlockState(pos)==state)written.put(pos.immutable(),state);
        }
        void clear(){
            var p=game.server().getPlayerList().getPlayer(author);
            game.items().indirectKills().trapChanges(level,p,false,()->{
                // Only this source column's unchanged red sand, never the world column or a foreign replacement.
                for(var entry:written.entrySet())if(entry.getValue().is(Blocks.RED_SAND)&&level.getBlockState(entry.getKey())==entry.getValue())
                    level.destroyBlock(entry.getKey(),true);
            });
            if(p!=null)shards(p,level,point,null);
        }
    }
    private void shards(ServerPlayer p,ServerLevel level,Vec3 at,Entity source){
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK),source==null?p:source,p);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(1),q->q.position().distanceToSqr(at)<=1
                &&(q==p?!LbcGreed.active(q)&&game.items().wands().validRoleTarget(q,false):game.items().wands().projectileTarget(p,q))))target.hurtServer(level,damage,4);
    }
    private static void load(Entity actor,String snbt){
        try{actor.load(TagValueInput.create(ProblemReporter.DISCARDING,actor.registryAccess(),TagParser.parseCompoundFully(snbt)));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
    }
    void trackWild(ServerPlayer p,Entity actor){actor.entityTags().add(WILD);track(p,actor);}
    public static boolean wild(Entity actor){return managed(actor)&&actor.entityTags().contains(WILD);}
    void track(ServerPlayer p,Entity actor){
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||actor.level()!=game.maps().level()){actor.discard();return;}
        actor.entityTags().add(ACTOR);game.items().markCurrentItemEpoch(actor);actors.put(actor,p.getUUID());
        game.items().timeStop().prepareFrozen(actor);if(!p.level().addFreshEntity(actor))actors.remove(actor);
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}
    public ServerPlayer owner(Entity entity){
        if(!actors.containsKey(entity))return null;
        var reflected=game.items().electro().reflectedOwner(entity);if(reflected instanceof ServerPlayer p)return p;
        if(entity instanceof PrimedTnt tnt&&tnt.getOwner() instanceof ServerPlayer p)return p;
        var id=actors.get(entity);return id==null?null:game.server().getPlayerList().getPlayer(id);
    }
    public void inheritChild(Entity source,Entity child){
        if(!managed(source)||!(child instanceof PrimedTnt)||!actors.containsKey(source))return;
        var p=owner(source);if(p==null||!game.playing()||game.stage()==GameController.Stage.ENDING){child.discard();return;}
        child.entityTags().add(ACTOR);if(wild(source))child.entityTags().add(WILD);game.items().markCurrentItemEpoch(child);actors.put(child,p.getUUID());game.items().timeStop().prepareFrozen(child);
    }
    public boolean explosionTarget(Entity source,Entity target){return !managed(source)||canAffect(source,target);}
    public boolean canAffect(Entity source,Entity target){
        var p=owner(source);if(p!=null&&wild(source))return !LbcGreed.active(target)&&game.items().wands().validRoleTarget(target,false);
        return p!=null&&(target==p&&!(source instanceof Wolf)
                ?!LbcGreed.active(target)&&game.items().wands().validRoleTarget(target,false):game.items().wands().projectileTarget(p,target));
    }
    public void targetChanged(Mob actor,LivingEntity target){if(managed(actor)&&target!=null&&!canAffect(actor,target))actor.setTarget(null);}
    public void withSandTick(FallingBlockEntity actor,Runnable nativeTick){
        var p=owner(actor);
        if(p==null){actor.discard();return;}
        game.items().indirectKills().trapChanges((ServerLevel)actor.level(),p,false,nativeTick);
        if(actor.onGround()&&actor.entityTags().add("skywars2.incoming_artifact.shards"))shards(p,(ServerLevel)actor.level(),actor.position(),actor);
    }
    public int count(){return actors.size()+columns.size()+terrain.size();}
    public void loaded(Entity actor){
        if(!managed(actor))return;
        if(!game.items().currentEpoch(actor)||!game.playing()||actor.level()!=game.maps().level()){actor.discard();return;}
        if(!actors.containsKey(actor)){
            var id=unloaded.remove(actor.getUUID());if(id==null){actor.discard();return;}actors.put(actor,id);
        }
        game.items().timeStop().prepareFrozen(actor);
    }
    public void unloaded(Entity actor){
        if(managed(actor)&&actor.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK){var id=actors.remove(actor);if(id!=null)unloaded.put(actor.getUUID(),id);}
    }
    public void forget(ServerPlayer p){
        for(var actor:List.copyOf(actors.keySet()))if(p.getUUID().equals(actors.get(actor))){actors.remove(actor);actor.discard();}
        unloaded.values().removeIf(p.getUUID()::equals);columns.removeIf(c->c.author.equals(p.getUUID()));
        for(var job:List.copyOf(terrain))if(job.author.equals(p.getUUID()))job.cancel();
    }
    public void stop(){
        for(var actor:List.copyOf(actors.keySet()))actor.discard();actors.clear();unloaded.clear();columns.clear();
        for(var job:List.copyOf(terrain))job.cancel();phase=0;
    }
    public void tick(){
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        actors.keySet().removeIf(Entity::isRemoved);
        var level=game.maps().level();if(level==null||game.items().timeStop().stopped(level))return;
        for(var actor:List.copyOf(actors.keySet()))if(actor instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow
                &&actor.entityTags().contains("killedarrow")&&((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$isInGround())actor.discard();
        for(var column:List.copyOf(columns)){
            if(!game.items().currentEpoch(column.epoch)){columns.remove(column);continue;}
            if(!resident(column.level,column.center,column.center.above(3)))continue;
            if(--column.remaining<=0){column.clear();columns.remove(column);}
        }
        phase=(phase+1)%20;if(phase!=0)return;
        for(var p:game.server().getPlayerList().getPlayers())if(admitted(p,false)&&inventory(p,"gaara",true)
                &&!game.items().magic().beaconSuppressed(p.position())){
            effect(p,MobEffects.RESISTANCE,40,0,true);
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST,Blocks.SAND.defaultBlockState()),true,false,
                    p.getX(),p.getY()+.81,p.getZ(),5,.3,.5,.3,1);
        }
    }
    private static boolean resident(ServerLevel level,BlockPos from,BlockPos to){
        for(int x=from.getX()>>4;x<=to.getX()>>4;x++)for(int z=from.getZ()>>4;z<=to.getZ()>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private void queue(ServerPlayer owner,BlockPos from,BlockPos to,Runnable action){
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||owner.level()!=game.maps().level())return;
        var task=new Terrain(owner,from,to,action);terrain.add(task);game.items().stonekivi().queuedWork(task);
    }
    private final class Terrain implements LbcStonekivi.Work {
        final ServerLevel level;final UUID author;final String epoch=game.items().epoch();final BlockPos from,to;final Runnable action;boolean cancelled;
        Terrain(ServerPlayer p,BlockPos from,BlockPos to,Runnable action){level=p.level();author=p.getUUID();this.from=from;this.to=to;this.action=action;}
        boolean live(){return !cancelled&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch);}
        @Override public boolean ready(){return !live()||!game.items().timeStop().stopped(level)&&resident(level,from,to);}
        @Override public boolean step(){
            if(!live()){cancel();return true;}
            var p=game.server().getPlayerList().getPlayer(author);
            if(p!=null)game.items().indirectKills().trapChanges(level,p,false,action);
            cancel();return true;
        }
        @Override public void cancel(){cancelled=true;terrain.remove(this);}
    }
}
