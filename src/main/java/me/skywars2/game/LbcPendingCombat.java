package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Author-answered Piston, forty-copy Chaos and temporary seven-attribute Gravity curse. */
public final class LbcPendingCombat {
    private static final String ACTOR="skywars2.pending_chaos";
    private static final Identifier CURSE=Identifier.parse("minecraft:gravity_curse");
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final List<Holder<Attribute>> ATTRIBUTES=List.of(Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.GRAVITY,Attributes.STEP_HEIGHT,
            Attributes.SAFE_FALL_DISTANCE,Attributes.JUMP_STRENGTH,Attributes.MOVEMENT_SPEED,Attributes.KNOCKBACK_RESISTANCE);
    private static final int[][] RANGES={{-100,400},{-120,200},{-400,100},{-100,100},{-400,400},{-100,100}};
    private static final int[][] OFFSETS={{0,15,0},{11,14,0},{0,13,11},{-11,12,0},{0,10,-11},{0,9,0},{11,8,11},{-11,7,11},{-11,6,-11},{11,5,-11},
        {0,16,0},{11,17,0},{0,18,11},{-11,19,0},{0,20,-11},{0,21,0},{11,22,11},{-11,23,11},{-11,24,-11},{11,25,-11},
        {0,26,0},{11,27,0},{0,28,11},{-11,29,0},{0,30,-11},{0,31,0},{11,32,11},{-11,33,11},{-11,34,-11},{11,35,-11},
        {0,36,0},{11,37,0},{0,38,11},{-11,39,0},{0,40,-11},{0,41,0},{11,42,11},{-11,43,11},{-11,44,-11},{11,5,-11}};
    private final GameController game;
    private final List<Copy> copies=new ArrayList<>();
    private final Map<LivingEntity,Curse> curses=new IdentityHashMap<>();
    private int phase;
    private long pistonWrites;
    long pistonWrites(){return pistonWrites;}
    static final class Copy {
        final ServerPlayer owner;final Display.ItemDisplay display;final String epoch;final LbcIndirectKills.Owner cause;
        Vec3 direction;boolean grounded;
        Copy(ServerPlayer owner,Display.ItemDisplay display,String epoch,LbcIndirectKills.Owner cause,Vec3 direction){this.owner=owner;this.display=display;this.epoch=epoch;this.cause=cause;this.direction=direction;}
    }
    private static final class Curse {
        final ServerPlayer owner;final String epoch;final LbcIndirectKills.Owner cause;int seconds=10;
        Curse(ServerPlayer owner,String epoch,LbcIndirectKills.Owner cause){this.owner=owner;this.epoch=epoch;this.cause=cause;}
    }
    public LbcPendingCombat(GameController game){this.game=game;}
    public static boolean isPiston(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("piston_sword",0)==1;}
    private boolean valid(ServerPlayer owner){return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
        &&game.canUseGameplayItems(owner)&&!game.isObserver(owner)&&game.stage()!=GameController.Stage.ENDING;}
    private boolean paused(ServerPlayer owner){return game.items().wands().inputTimeStopped(owner)||game.items().timeStop().stopped(owner.level());}
    private boolean admitted(ServerPlayer owner){return valid(owner)&&!paused(owner)&&!game.items().actionsBlocked(owner);}
    private boolean magic(ServerPlayer owner){return !owner.entityTags().contains("nomagic_active")&&!game.items().magic().suppressed(owner)
        &&!game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());}
    /** Shared accepted-damage callback supplies unblocked, positive and fatal native hits. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)||!admitted(owner)||source.is(ARROW_ONLY)||!game.items().wands().validRoleTarget(victim,true))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        var data=LbcFartus.data(owner.getMainHandItem());
        // Chaos deliberately has no branch-local antimagic admission: native magic actors can be purged later.
        if(data.getIntOr("chaos_swo",0)==1)volley(owner);
        if(data.getIntOr("gravitationsword",0)==1&&magic(owner))gravity(owner);
    }
    private void gravity(ServerPlayer owner){
        double[] values=new double[7];
        for(int i=0;i<6;i++)values[i]=(owner.getRandom().nextInt(RANGES[i][1]-RANGES[i][0]+1)+RANGES[i][0])/100.0;
        // Author changed only the amount: preserve the original add_multiplied_total operation.
        values[6]=-3+3*owner.getRandom().nextDouble();
        LbcOverload.sound(owner,"minecraft:entity.phantom.flap",1,2,0);
        var origin=owner.position();var cause=game.items().indirectKills().coldCause(owner,false);
        for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(8),e->{
            double distance=e.position().distanceToSqr(origin);return distance>=.0001&&distance<=64&&e.hurtTime==10
                &&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().target(owner,e);
        })){
            if(target instanceof ServerPlayer player&&game.items().magic().rejectCustomEffect(player))continue;
            clearEffects(target);
            var slam=target.getAttribute(Attributes.GRAVITY);if(slam!=null)slam.removeModifier(Identifier.parse("minecraft:slamslamslaaaaaaamslaaam"));
            for(int i=0;i<7;i++){var attribute=target.getAttribute(ATTRIBUTES.get(i));if(attribute!=null)
                attribute.addTransientModifier(new AttributeModifier(CURSE,values[i],AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
            curses.put(target,new Curse(owner,game.items().epoch(),cause));
            if(target instanceof ServerPlayer player)game.items().mana().score(player,"effect_gravity_curse").set(10);
            game.items().indirectKills().displaced(target,cause);
        }
    }
    public boolean gravityCurseActive(LivingEntity target){return curses.containsKey(target);}
    public void clearEffects(LivingEntity target){
        curses.remove(target);for(var type:ATTRIBUTES){var attribute=target.getAttribute(type);if(attribute!=null)attribute.removeModifier(CURSE);}
        if(target instanceof ServerPlayer player)game.items().mana().score(player,"effect_gravity_curse").set(0);
    }
    private void volley(ServerPlayer owner){
        var cause=game.items().indirectKills().coldCause(owner,true);
        for(var offset:OFFSETS){
            var at=owner.position().add(offset[0],offset[1],offset[2]);if(!resident(owner.level(),BlockPos.containing(at)))continue;
            var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());
            try{display.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),TagParser.parseCompoundFully(
                "{interpolation_duration:1,teleport_duration:1,shadow_radius:0f,shadow_strength:0f,item_display:'fixed',transformation:[0f,0f,2.5f,0f,1.7678f,-1.7678f,0f,0f,1.7678f,1.7678f,0f,0f,0f,0f,0f,3f],item:{id:'minecraft:golden_sword',count:1}}")));}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
            float yaw=(owner.getRandom().nextInt(360001)-180000)/1000f,pitch=(owner.getRandom().nextInt(180001)-90000)/1000f;
            display.snapTo(at.x,at.y,at.z,yaw,pitch);display.entityTags().addAll(List.of(ACTOR,"homing_sword","slowraycast","imba_rc","magic"));
            game.items().markCurrentItemEpoch(display);var copy=new Copy(owner,display,game.items().epoch(),cause,Vec3.directionFromRotation(pitch,yaw));
            copies.add(copy);if(!owner.level().addFreshEntity(display)||display.isRemoved())remove(copy);
        }
    }
    private static boolean resident(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getWorldBorder().isWithinBounds(pos)
        &&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    public boolean use(ServerPlayer owner,InteractionHand hand){
        if(!isPiston(owner.getItemInHand(hand))||!admitted(owner)||!magic(owner)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        LbcOverload.sound(owner,"minecraft:block.piston.extend",1,1,0);
        var direction=owner.getLookAngle();var at=owner.getEyePosition();
        for(int sample=0;sample<=200;sample++,at=at.add(direction.scale(.33))){
            var pos=BlockPos.containing(at);if(!resident(owner.level(),pos))return true;
            if(owner.getRandom().nextFloat()<.15)owner.level().sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,Blocks.REDSTONE_BLOCK.defaultBlockState()),at.x,at.y,at.z,1,0,0,0,0);
            if(!owner.level().getBlockState(pos).is(Blocks.AIR)){shift(owner,pos,direction);return true;}
        }
        return true;
    }
    /** One forward pass over the digital path; never loads chunks, caps chain length or delays a shift. */
    boolean shift(ServerPlayer owner,BlockPos first,Vec3 direction){return shift(owner,first,direction,null);}
    // A writer boundary also permits native verification of a rejected write after preceding cells changed.
    boolean shift(ServerPlayer owner,BlockPos first,Vec3 direction,java.util.function.BiConsumer<BlockPos,MapManager.BlockSnapshot> writer){
        var level=owner.level();var positions=new ArrayList<BlockPos>();var before=new ArrayList<MapManager.BlockSnapshot>();
        var current=first;
        while(true){
            if(!resident(level,current))return false;
            var snapshot=game.maps().snapshot(level,current);positions.add(current);before.add(snapshot);
            if(snapshot.state().is(Blocks.AIR))break;
            // Native clone cannot change BARRIER to BARRIER during its staging step; reject the whole operation.
            if(snapshot.state().is(Blocks.BARRIER))return false;
            var next=BlockPos.containing(Vec3.atCenterOf(current).add(direction));if(next.equals(current))return false;current=next;
        }
        if(positions.size()<2)return false;
        int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE|Block.UPDATE_SUPPRESS_DROPS;
        boolean[] success={true};
        var provenance=game.items().indirectKills().captureTerrain(level,positions);
        game.items().indirectKills().trapChanges(owner,()->{
            try{
                for(int i=positions.size()-1;i>=0;i--){var expected=i==0?MapManager.BlockSnapshot.air():before.get(i-1);
                    pistonWrites++;if(writer==null)game.maps().restoreSnapshot(level,positions.get(i),expected,flags);else writer.accept(positions.get(i),expected);
                    var actual=game.maps().snapshot(level,positions.get(i));
                    if(!Objects.equals(expected.state(),actual.state())||!sameBlockEntity(expected.entity(),actual.entity())){success[0]=false;break;}
                }
            }catch(RuntimeException error){success[0]=false;}
            if(!success[0])for(int i=0;i<positions.size();i++){pistonWrites++;game.maps().restoreSnapshot(level,positions.get(i),before.get(i),flags);}
        });
        if(!success[0])game.items().indirectKills().restoreTerrain(provenance);
        else{
            // Ordinary clone stages BARRIER then performs final placement with flags2. Defer its native
            // onPlace until commit: liquids/falling blocks must schedule themselves, while rollback stays inert.
            for(int i=positions.size()-1;i>0;i--){
                var expected=before.get(i-1).state();var pos=positions.get(i);
                if(level.getBlockState(pos)==expected)expected.onPlace(level,pos,Blocks.BARRIER.defaultBlockState(),false);
            }
            for(int i=0;i<positions.size();i++)level.updateNeighboursOnBlockSet(positions.get(i),before.get(i).state());
            copyBlockTicks(level,positions);
        }
        return success[0];
    }
    /** Native clone copies block ticks only; sparse digital diagonals must not scan their enclosing rectangle. */
    private static void copyBlockTicks(ServerLevel level,List<BlockPos> path){
        var sources=path.subList(0,path.size()-1);var offset=path.get(1).subtract(path.getFirst());
        int moving=(offset.getX()==0?0:1)+(offset.getY()==0?0:1)+(offset.getZ()==0?0:1);
        if(moving==1){copyTickArea(level,sources,offset);return;}
        var chunks=new LinkedHashMap<Long,List<BlockPos>>();
        for(var pos:sources)chunks.computeIfAbsent(net.minecraft.world.level.ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4),ignored->new ArrayList<>()).add(pos);
        // Each actual source chunk queue is scanned once, regardless of chain length within that chunk.
        var groups=new ArrayList<>(chunks.values());
        // Copy farthest first: this same-level operation must never recopy a newly created destination tick.
        for(int i=groups.size()-1;i>=0;i--)copyTickArea(level,groups.get(i),offset);
    }
    private static void copyTickArea(ServerLevel level,List<BlockPos> cells,BlockPos offset){
        var bounds=net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(cells).orElseThrow();
        var exact=Set.copyOf(cells);
        var area=new net.minecraft.world.level.levelgen.structure.BoundingBox(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()){
            @Override public boolean isInside(net.minecraft.core.Vec3i pos){return exact.contains(new BlockPos(pos.getX(),pos.getY(),pos.getZ()));}
            @Override public boolean isInside(int x,int y,int z){return exact.contains(new BlockPos(x,y,z));}
        };
        level.getBlockTicks().copyAreaFrom(level.getBlockTicks(),area,offset);
    }
    private static boolean sameBlockEntity(net.minecraft.nbt.CompoundTag expected,net.minecraft.nbt.CompoundTag actual){
        if(expected==null||actual==null)return expected==actual;
        var first=expected.copy();var second=actual.copy();
        for(var key:List.of("x","y","z")){first.remove(key);second.remove(key);}
        return first.equals(second);
    }
    private void remove(Copy copy){copies.remove(copy);if(!copy.display.isRemoved())copy.display.discard();}
    private void broken(Copy copy){var at=copy.display.position();LbcOverload.sound(copy.owner,at,"minecraft:entity.item.break",.5f,1,0);
        copy.owner.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.GOLDEN_SWORD),at.x,at.y,at.z,10,.2,.2,.2,0);remove(copy);}
    private boolean target(Copy copy,Entity target){return target!=copy.owner&&target.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(copy.owner,target);}
    private void step(Copy copy,List<LivingEntity> candidates){
        var display=copy.display;var level=copy.owner.level();var from=display.position();var cell=BlockPos.containing(from);
        if(!resident(level,cell)||game.items().magic().suppressedAt(level,from)){remove(copy);return;}
        // Shared source skill30 rotates first, before Chaos applies its own ordinary steering.
        var profile=game.players().get(copy.owner.getUUID());
        if(game.config().lbcSkills&&profile!=null&&profile.lbcSkill==30&&copy.owner.getRandom().nextBoolean()
            &&level.players().stream().noneMatch(player->!player.isSpectator()&&player.position().distanceToSqr(from)<=400)){
            LivingEntity aimed=null;double best=10000;
            for(var candidate:candidates)if(target(copy,candidate)&&!candidate.entityTags().contains("strict_map_object")){
                double squared=candidate.position().distanceToSqr(from);if(squared<=best){best=squared;aimed=candidate;}}
            if(aimed!=null)copy.direction=copy.direction.add(aimed.getEyePosition().subtract(from).normalize().scale(.1)).normalize();
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xff0000,2),true,false,from.x,from.y,from.z,3,.1,.1,.1,0);
        }
        if(copy.grounded){var ahead=BlockPos.containing(from.add(copy.direction.scale(.5)));
            if(!resident(level,ahead)){remove(copy);return;}
            if(level.getBlockState(ahead).isAir()||copy.owner.getRandom().nextInt(100)==0)broken(copy);return;}
        if(!level.getBlockState(cell).isAir()){
            // Traverse the original backward ray to the first literal air sample, with resident/world bounds as the only limit.
            var backed=from;do{backed=backed.subtract(copy.direction.scale(.1));if(!resident(level,BlockPos.containing(backed))){remove(copy);return;}}
            while(!level.getBlockState(BlockPos.containing(backed)).isAir());
            display.setPos(backed.add(copy.direction.scale(.5)));copy.grounded=true;
            LbcOverload.sound(copy.owner,display.position(),"minecraft:block.copper.break",1.5f,1,0);return;
        }
        LivingEntity nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(var candidate:candidates)if(target(copy,candidate)&&!candidate.entityTags().contains("strict_map_object")){
            double squared=candidate.position().distanceToSqr(from);if(squared<distance){distance=squared;nearest=candidate;}}
        if(nearest!=null){copy.direction=copy.direction.add(nearest.getEyePosition().subtract(from).normalize().scale(.1)).normalize();
            var to=from.add(copy.direction);if(!resident(level,BlockPos.containing(to))){remove(copy);return;}
            display.snapTo(to.x,to.y,to.z,(float)Math.toDegrees(Math.atan2(-copy.direction.x,copy.direction.z)),(float)-Math.toDegrees(Math.asin(copy.direction.y)));}
        // Source uses the retained pre-teleport execution position, a one-block selector volume and no swept collision.
        var box=new AABB(from,from.add(1,1,1));
        for(var victim:level.getEntitiesOfClass(LivingEntity.class,box,e->target(copy,e)&&e.position().distanceToSqr(from)<=64)){
            var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),display,copy.owner);
            victim.hurtServer(level,game.items().indirectKills().captureDamage(damage,copy.cause),4);
            level.sendParticles(ParticleTypes.CRIT,from.x,from.y,from.z,8,.1,.1,.1,.5);
            LbcOverload.sound(copy.owner,from,"minecraft:entity.player.attack.strong",1,1,0);remove(copy);break;
        }
    }
    public void tick(){
        if(game.items().wands().timeStopped())return;
        phase=(phase+1)%20;
        for(var entry:List.copyOf(curses.entrySet())){var target=entry.getKey();var curse=entry.getValue();
            if(game.items().polymorph().masked(target))continue;
            if(!target.isAlive()||target.isRemoved()||!valid(curse.owner)||target.level()!=curse.owner.level()||!game.items().currentEpoch(curse.epoch)
                ||target instanceof ServerPlayer player&&game.items().magic().rejectCustomEffect(player)){clearEffects(target);continue;}
            if(paused(curse.owner)||LbcGreed.active(target))continue;
            game.items().indirectKills().displaced(target,curse.cause);
            if(phase==0){
                if(target instanceof ServerPlayer player)curse.seconds=Math.max(0,game.items().mana().hudValue(player,"effect_gravity_curse"));
                if(curse.seconds<=1)clearEffects(target);else{curse.seconds--;if(target instanceof ServerPlayer player)game.items().mana().score(player,"effect_gravity_curse").set(curse.seconds);}
            }
        }
        var candidates=new IdentityHashMap<ServerPlayer,List<LivingEntity>>();
        for(var copy:List.copyOf(copies)){
            if(copy.display.isRemoved()||!valid(copy.owner)||!game.items().currentEpoch(copy.epoch)){remove(copy);continue;}
            if(paused(copy.owner))continue;
            var list=candidates.computeIfAbsent(copy.owner,owner->{var found=new ArrayList<LivingEntity>();for(var entity:owner.level().getAllEntities())if(entity instanceof LivingEntity living&&living.isAlive()&&living.getType().builtInRegistryHolder().is(MOBS))found.add(living);return found;});
            step(copy,list);
        }
    }
    public void loaded(Entity entity){if(entity.entityTags().contains(ACTOR)&&copies.stream().noneMatch(copy->copy.display==entity))entity.discard();}
    public void unloaded(Entity entity){for(var copy:List.copyOf(copies))if(copy.display==entity)remove(copy);if(entity instanceof LivingEntity target)clearEffects(target);}
    public void forget(ServerPlayer owner){for(var copy:List.copyOf(copies))if(copy.owner==owner)remove(copy);for(var entry:List.copyOf(curses.entrySet()))if(entry.getKey()==owner||entry.getValue().owner==owner)clearEffects(entry.getKey());}
    public void stop(){for(var copy:List.copyOf(copies))remove(copy);for(var target:List.copyOf(curses.keySet()))clearEffects(target);phase=0;}
    List<Copy> snapshot(){return List.copyOf(copies);}
    int curseSeconds(LivingEntity target){var curse=curses.get(target);return curse==null?0:curse.seconds;}
}
