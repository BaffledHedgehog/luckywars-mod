package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native family actors. Durable state belongs to native entities and scoreboard, not a checkpoint service. */
public final class LbcFamilyDrops {
    private static final String ACTOR="skywars2.family", CLOCK="skywars2.family_clock.";
    // Central policy switches, pending the author's final suppression decision.
    public static final boolean PARENTS_MAGIC_SUPPRESSED=false, CHILD_MAGIC_SUPPRESSED=true;
    private static final CompoundTag MOM=preset("""
            {Tags:["mom","rc_1sec","active"],CustomNameVisible:1b,ShowArms:1b,Invulnerable:1b,NoBasePlate:1b,DisabledSlots:4144959,Pose:{LeftLeg:[180f,0f,0f],RightLeg:[180f,0f,0f],RightArm:[0f,0f,0f]}}
            """);
    private static final CompoundTag OTCHIM=preset("""
            {Tags:["otchim","lucky_mob","active"],CustomNameVisible:1b,Invulnerable:1b,PersistenceRequired:1b,drop_chances:{head:0f,chest:0f,legs:0f,feet:0f},active_effects:[{id:"minecraft:invisibility",amplifier:1b,duration:20000000}]}
            """);
    private static final CompoundTag CHILD=preset("""
            {Tags:["absurd_child","lucky_mob"],Glowing:1b,CustomNameVisible:1b,PersistenceRequired:1b,Health:100000f,CustomName:{bold:true,color:"gold",italic:false,translate:"absurd_child"},attributes:[{id:"minecraft:max_health",base:100000}],drop_chances:{mainhand:0f}}
            """);
    private final GameController game;
    private final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcFamilyDrops(GameController game){this.game=game;}
    private static CompoundTag preset(String data){try{return TagParser.parseCompoundFully(data);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    public static boolean matches(ItemStack stack){return LbcFamilyDropItems.matches(stack);}
    public static boolean managed(Entity e){return e!=null&&e.entityTags().contains(ACTOR)&&(e instanceof ArmorStand||e instanceof Zombie||e instanceof Allay);}
    private boolean active(Entity e){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&e.level()==game.maps().level()&&game.items().currentEpoch(e);}
    private boolean suppressed(Entity e){return (e instanceof Allay?CHILD_MAGIC_SUPPRESSED:PARENTS_MAGIC_SUPPRESSED)&&game.items().magic().suppressedAt((ServerLevel)e.level(),e.position());}
    public boolean ready(ItemEntity token){
        if(token.isRemoved()||token.getAge()>=5999||!matches(token.getItem())||!game.playing()||game.stage()==GameController.Stage.ENDING||token.level()!=game.maps().level()||!(token.level() instanceof ServerLevel level)||paused(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.")&&!game.items().currentEpoch(tag))return false;
        return level.getChunkSource().getChunkNow(token.blockPosition().getX()>>4,token.blockPosition().getZ()>>4)!=null&&(!PARENTS_MAGIC_SUPPRESSED||!game.items().magic().suppressedAt(level,token.position()));
    }
    public boolean spawn(ItemEntity token){
        if(!ready(token))return false;
        var level=(ServerLevel)token.level();boolean mom=LbcFamilyDropItems.kind(token.getItem())==LbcFamilyDropItems.Kind.MOM;
        Entity actor=mom?EntityTypes.ARMOR_STAND.create(level,EntitySpawnReason.COMMAND):EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        if(actor==null)return false;
        actor.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),(mom?MOM:OTCHIM).copy()));actor.setPos(token.position());
        var nearest=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        if(nearest==null)nearest=game.items().spellSources().owner(token);
        if(nearest==null)nearest=nearestPlayer(level,actor.position());
        var living=(LivingEntity)actor;
        var head=loot(mom?"mom":"otchim",actor,nearest);living.setItemSlot(mom?EquipmentSlot.MAINHAND:EquipmentSlot.HEAD,head);actor.setCustomName(head.get(DataComponents.CUSTOM_NAME));
        prepare(actor,token);
        if(!level.addFreshEntity(actor)){actors.remove(actor);actor.discard();return false;}
        if(nearest!=null){
            if(mom){nearest.entityTags().add("have_mom");var board=game.server().getScoreboard();var objective=board.getObjective("lbcID2");if(objective!=null){var score=board.getPlayerScoreInfo(nearest,objective);if(score!=null)board.getOrCreatePlayerScore(actor,objective).set(score.value());}}
            broadcast(Component.translatable(mom?"mom":"otchim").withStyle(s->s.withColor(0xffffff)).append(nearest.getDisplayName()).append(Component.translatable(mom?"burned":"otchim_2").withStyle(s->s.withColor(0xffffff))));
        }
        return true;
    }
    private ItemStack loot(String id,Entity entity,ServerPlayer nearest){
        var table=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));
        var params=new LootParams.Builder((ServerLevel)entity.level()).withParameter(LootContextParams.ORIGIN,entity.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,nearest).create(LootContextParamSets.COMMAND);
        var items=table.getRandomItems(params);return items.isEmpty()?ItemStack.EMPTY:items.getFirst();
    }
    private void prepare(Entity actor,Entity parent){
        actor.entityTags().add(ACTOR);clock(actor,0);game.items().markCurrentItemEpoch(actor);game.items().spellSources().capture(actor);game.items().spellSources().inherit(parent,actor);
        if(game.items().spellSources().source(actor)==null&&parent instanceof ItemEntity token){
            var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
            if(author!=null)game.items().spellSources().bind(actor,game.items().spellSources().hostile((LivingEntity)actor,author,Set.of()));
        }
        // These independent actors outlive their author's life/session. Keep the accepted
        // causal policy in the shared owner, including inherited emitter and exclusions.
        var source=game.items().spellSources().source(actor);
        if(source!=null&&!source.persistent())game.items().spellSources().bind(actor,new LbcSpellSource.Source(
                source.emitter(),source.author(),source.epoch(),source.cause(),source.excluded(),true));
        actors.add(actor);game.items().timeStop().prepareFrozen(actor);
    }
    private ServerPlayer nearestPlayer(ServerLevel level,Vec3 at){ServerPlayer best=null;double distance=Double.POSITIVE_INFINITY;for(var p:level.players())if(!p.isSpectator()&&!p.isRemoved()){double d=p.position().distanceToSqr(at);if(d<distance){distance=d;best=p;}}return best;}
    private void broadcast(Component message){for(var p:game.server().getPlayerList().getPlayers())p.sendSystemMessage(message);}
    private static int clock(Entity e){for(var tag:e.entityTags())if(tag.startsWith(CLOCK))try{return Integer.parseInt(tag.substring(CLOCK.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void clock(Entity e,int n){e.entityTags().removeIf(t->t.startsWith(CLOCK));e.entityTags().add(CLOCK+n);}
    public void tick(){
        for(var e:List.copyOf(actors)){
            if(e.isRemoved()){actors.remove(e);continue;}if(!active(e)){actors.remove(e);e.discard();continue;}
            var level=(ServerLevel)e.level();if(paused(level))continue;
            int n=(clock(e)+1)%20;clock(e,n);if(suppressed(e))continue;
            try(var scope=game.items().spellSources().scope(e)){
                if(e instanceof ArmorStand stand&&n==0)mom(stand);
                else if(e instanceof Zombie zombie&&n%5==0)birth(zombie);
                else if(e instanceof Allay child&&n==0)child(child);
            }
        }
    }
    private void mom(ArmorStand mom){
        var level=(ServerLevel)mom.level();var target=nearestPlayer(level,mom.position());
        if(target!=null){var delta=target.getEyePosition().subtract(mom.position());mom.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mom.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));}
        // Source setblock deliberately replaces the occupied block, without destroying drops.
        var source=game.items().spellSources().source(mom);var cell=mom.blockPosition();boolean resident=true;
        for(int x=(cell.getX()-1)>>4;x<=(cell.getX()+1)>>4;x++)for(int z=(cell.getZ()-1)>>4;z<=(cell.getZ()+1)>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)resident=false;
        if(resident)game.items().indirectKills().capturedWorldChanges(level,source==null?null:source.cause(),()->LbcDropBlocks.setBlock(level,cell,Blocks.FIRE.defaultBlockState(),false));
        int id=level.getRandom().nextInt(6);var sound=SoundEvent.createVariableRangeEvent(Identifier.parse(switch(id){case 1,3->"minecraft:entity.cow.death";case 4,5->"minecraft:entity.sheep.death";default->"minecraft:entity.pig.death";}));
        var packet=new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound),SoundSource.MASTER,mom.getX(),mom.getY(),mom.getZ(),1,id==2||id==3||id==5?.5f:0f,level.getRandom().nextLong());
        for(var viewer:game.server().getPlayerList().getPlayers())viewer.connection.send(packet);
    }
    private void birth(Zombie dad){
        var level=(ServerLevel)dad.level();var mom=level.getEntitiesOfClass(ArmorStand.class,new AABB(dad.position(),dad.position()).inflate(2.5),e->managed(e)&&e.entityTags().contains("mom")&&!e.isRemoved()).stream().findFirst().orElse(null);if(mom==null)return;
        var child=EntityTypes.ALLAY.create(level,EntitySpawnReason.COMMAND);if(child==null)return;
        child.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),CHILD.copy()));child.setPos(dad.position().add(0,1,0));child.setItemSlot(EquipmentSlot.MAINHAND,loot("luckyblock_block_only",child,null));prepare(child,dad);
        if(!level.addFreshEntity(child)){actors.remove(child);child.discard();return;}
        // Live Paper override uses one harmless visual strike, not the old datapack's global strike.
        var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.COMMAND);if(bolt!=null){bolt.setPos(dad.position());bolt.setVisualOnly(true);level.addFreshEntity(bolt);}
        level.sendParticles(new net.minecraft.core.particles.DustColorTransitionOptions(0x000a64,0x640a00,4),dad.getX(),dad.getY(),dad.getZ(),5000,1,1,1,2);
        for(var p:level.players())if(!p.isSpectator()&&p.distanceToSqr(dad)<=225)game.items().mana().award(p,"lbc:true_advancements/legends/absurd_child");
        broadcast(Component.translatable("absurd_child_born"));mom.discard();dad.discard();
    }
    private void child(Allay child){
        var level=(ServerLevel)child.level();LivingEntity target=null;double distance=400;
        for(var e:level.getEntitiesOfClass(LivingEntity.class,child.getBoundingBox().inflate(20),e->e!=child&&game.items().wands().validTarget(e))){double d=e.distanceToSqr(child);if(d<=distance){distance=d;target=e;}}
        if(target!=null&&distance>25)ray(child,target);
        if(level.getRandom().nextInt(100)<3){var message=Component.translatable("absurd_child").append(Component.translatable("absurd_child_msg_"+(level.getRandom().nextInt(20)+1)));for(var p:level.players())if(!p.isSpectator()&&p.distanceToSqr(child)<=900)p.sendSystemMessage(message);}
        var held=child.getMainHandItem();String table=held.is(Items.RED_MUSHROOM_BLOCK)||held.is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NORMAL))?"luckyblock_neko_block":held.is(Items.BROWN_MUSHROOM_BLOCK)||held.is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.NEKO))?"luckyblock_loli_block":held.is(Items.MUSHROOM_STEM)||held.is(LbcLuckyBlocks.item(LbcLuckyBlocks.Kind.LOLI))?"luckyblock_block_only":null;
        if(table!=null)child.setItemSlot(EquipmentSlot.MAINHAND,loot(table,child,null));
    }
    private void ray(Allay child,LivingEntity target){
        var level=(ServerLevel)child.level();var start=child.getEyePosition();var direction=target.getEyePosition().subtract(start).normalize();var end=start.add(direction.scale(20));
        // Never load a ray's next chunk. Stop at its first unloaded sample.
        for(double d=0;d<=20;d+=.2){var at=start.add(direction.scale(d));if(level.getChunkSource().getChunkNow(((int)Math.floor(at.x))>>4,((int)Math.floor(at.z))>>4)==null){end=start.add(direction.scale(Math.max(0,d-.2)));break;}}
        var block=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,child));end=block.getLocation();double nearest=start.distanceToSqr(end);LivingEntity hit=null;
        for(var e:level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(.2),e->e!=child&&game.items().wands().validTarget(e))){var box=e.getBoundingBox();if(!LbcBeamGeometry.intersects(box,start,end))continue;var contact=box.clip(start,end);if(box.contains(start)){hit=e;nearest=0;end=start;break;}if(contact.isPresent()){double d=start.distanceToSqr(contact.get());if(d<nearest){nearest=d;hit=e;}}}
        double length=Math.sqrt(nearest);for(double d=0;d<=length;d+=.4){var at=start.add(direction.scale(d));level.sendParticles(ParticleTypes.END_ROD,at.x,at.y,at.z,1,0,0,0,0);}
        if(hit!=null){var damage=game.items().spellSources().damage(level.damageSources().mobAttack(child));hit.hurtServer(level,damage,6);}
    }
    public void loaded(Entity e){if(!managed(e))return;if(!active(e)){e.discard();return;}actors.add(e);game.items().timeStop().prepareFrozen(e);}
    /** Rebuild only the derived loaded-body index after canonical frozen entity replay.
     * Vanity deliberately suppresses ENTITY_LOAD during that replay; clocks, equipment,
     * tags and all causal policies have already been restored by their shared owners. */
    void reindexAfterRestore(){
        actors.clear();
        var level=game.maps().level();
        if(level!=null)for(var entity:level.getAllEntities())if(managed(entity)&&!entity.isRemoved()&&active(entity))actors.add(entity);
    }
    private boolean paused(ServerLevel level){return !game.server().tickRateManager().runsNormally()||level.tickRateManager().isFrozen()||game.items().timeStop().stopped(level);}
    public void unloaded(Entity e){actors.remove(e);}
    public void stop(){for(var e:List.copyOf(actors))if(!e.isRemoved())e.discard();actors.clear();}
    public List<Entity> snapshot(){return List.copyOf(actors);}
}
