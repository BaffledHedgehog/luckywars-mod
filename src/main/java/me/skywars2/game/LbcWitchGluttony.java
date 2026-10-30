package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Gluttony source actors. Native AI, shared rays/terrain/effects/control; accepted causes survive the holder. */
public final class LbcWitchGluttony {
    private static final String MANAGED="skywars2.witch.gluttony_actor";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> TANNER=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:tannerkivi"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    enum Kind { RABBIT, SNAKE, FRAGMENT, WHALE, CLONE, FOG }
    private static final class Actor {final Kind kind;LbcSpellSource.Source source;int ticks;Actor(Kind kind,LbcSpellSource.Source source){this.kind=kind;this.source=source;}}
    private final GameController game;
    private final Map<Entity,Actor> actors=new LinkedHashMap<>();
    private final Map<UUID,Actor> unloaded=new HashMap<>();
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    private final Set<Kind> pending=EnumSet.noneOf(Kind.class);
    private final Set<Terrain> terrain=new LinkedHashSet<>();
    private int lastTick=Integer.MIN_VALUE,phase;
    private boolean advancing;
    LbcWitchGluttony(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){var data=LbcFartus.data(stack);return !stack.isEmpty()&&(data.getIntOr("rabbit",0)==1||data.getIntOr("snake",0)==1||data.getIntOr("whale",0)==1);}
    private boolean live(String epoch,ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch)&&LbcDimensionItems.ordinary(level);}
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    private boolean suppressed(Entity entity){return entity.entityTags().contains("nomagic_active")||game.items().magic().suppressedAt((ServerLevel)entity.level(),entity.position());}
    private List<? extends Entity> select(ServerLevel level,Vec3 at,String literal){try{
        var selector=selectors.get(literal);if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();selectors.put(literal,selector);}
        return List.copyOf(selector.findEntities(game.server().createCommandSourceStack().withLevel(level).withPosition(at).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput()));
    }catch(CommandSyntaxException error){throw new IllegalStateException("Gluttony source selector "+literal,error);}}
    private int count(ServerLevel level,Vec3 at,Kind kind){return select(level,at,switch(kind){case RABBIT->"@e[type=minecraft:rabbit,tag=grand_rabbit]";case WHALE->"@e[type=minecraft:ghast,tag=white_whale]";default->"@e[type=minecraft:marker,tag=black_snake]";}).size();}
    private boolean eligibleHolder(ServerPlayer p){return p.entityTags().contains("sin_gluttony")&&p.isAlive()&&!p.isRemoved()&&!game.isObserver(p)&&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p);}
    private ServerPlayer holder(Entity actor){return select((ServerLevel)actor.level(),actor.position(),"@a[tag=sin_gluttony,gamemode=!spectator,sort=nearest]").stream().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).filter(this::eligibleHolder).findFirst().orElse(null);}
    public boolean use(ServerPlayer owner,InteractionHand hand){
        if(!matches(owner.getItemInHand(hand))||!eligibleHolder(owner)||game.items().actionsBlocked(owner)||paused(owner.level())||game.items().wands().inputTimeStopped(owner)||suppressed(owner)
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var data=LbcFartus.data(owner.getItemInHand(hand));Kind kind=data.getIntOr("whale",0)==1?Kind.WHALE:data.getIntOr("rabbit",0)==1?Kind.RABBIT:Kind.SNAKE;
        var level=owner.level();var direction=owner.getLookAngle();float yaw=owner.getYRot(),pitch=owner.getXRot();var from=owner.getEyePosition().add(direction.scale(hand==InteractionHand.OFF_HAND?.5:0));
        if(game.items().magic().suppressedAt(level,from)||pending.contains(kind)||count(level,from,kind)>0)return false;
        var cause=game.items().indirectKills().wildCause(owner);var epoch=game.items().epoch();
        if(kind==Kind.WHALE){var whale=spawn(kind,level,owner.getEyePosition().add(owner.getLookAngle().scale(10)),owner.getYRot(),owner.getXRot(),owner.getUUID(),epoch,cause);
            if(whale==null)return false;velocity(whale,owner.getLookAngle(),7);sounds(level,false);return true;}
        pending.add(kind);
        game.items().exasperato().replayableSpell(owner,from,direction,150,new Cast(kind,level,direction,yaw,pitch,owner.getUUID(),epoch,cause));
        return true;
    }
    /** Immutable accepted cast data; completion is repeatable after restoring its scheduler and owner. */
    private final class Cast implements LbcExasperato.ReplayableRay {
        final Kind kind;final ServerLevel level;final Vec3 direction;final float yaw,pitch;final UUID author;final String epoch;final LbcIndirectKills.Owner cause;
        Cast(Kind kind,ServerLevel level,Vec3 direction,float yaw,float pitch,UUID author,String epoch,LbcIndirectKills.Owner cause){this.kind=kind;this.level=level;this.direction=direction;this.yaw=yaw;this.pitch=pitch;this.author=author;this.epoch=epoch;this.cause=cause;}
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return !state.is(AIRS);}
        public boolean target(Entity entity){return entity.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validRoleTarget(entity,false);}
        public void trail(Vec3 from,Vec3 to){for(double d=0;d<from.distanceTo(to);d+=.5){var p=from.add(to.subtract(from).normalize().scale(d));if(kind==Kind.RABBIT)level.sendParticles(ParticleTypes.CLOUD,p.x,p.y,p.z,1,.1,.1,.1,.2);else level.sendParticles(new DustParticleOptions(0x000000,4),p.x,p.y,p.z,1,.1,.1,.1,.2);}}
        public void complete(net.minecraft.world.phys.HitResult hit){pending.remove(kind);if(hit==null||!live(epoch,level))return;
            var at=hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?hit.getLocation().subtract(direction):hit.getLocation();spawn(kind,level,at,kind==Kind.SNAKE?0:yaw,kind==Kind.SNAKE?0:pitch,author,epoch,cause);}
    }
    private static CompoundTag parse(String value){try{return TagParser.parseCompoundFully(value);}catch(CommandSyntaxException error){throw new IllegalStateException(error);}}
    Entity spawn(Kind kind,ServerLevel level,Vec3 at,float yaw,float pitch,UUID author,String epoch,LbcIndirectKills.Owner cause){
        if(!live(epoch,level)||!Level.isInSpawnableBounds(BlockPos.containing(at))||level.getChunkSource().getChunkNow(Mth.floor(at.x)>>4,Mth.floor(at.z)>>4)==null)return null;
        Entity entity;String nbt;
        switch(kind){
            case RABBIT->{entity=EntityTypes.RABBIT.create(level,EntitySpawnReason.COMMAND);nbt="{Tags:[\"grand_rabbit\"],PersistenceRequired:1b,RabbitType:99,attributes:[{id:\"minecraft:follow_range\",base:100},{id:\"minecraft:movement_speed\",base:1}]}";}
            case WHALE,CLONE->{entity=EntityTypes.GHAST.create(level,EntitySpawnReason.COMMAND);boolean clone=kind==Kind.CLONE;nbt="{Tags:[\""+(clone?"white_whale_clone":"white_whale")+"\",\"lucky_mob\"],PersistenceRequired:1b,Health:"+(clone?15000:5400)+"f,ExplosionPower:30,CustomName:{italic:false,translate:\"white_whale\"},attributes:[{id:\"minecraft:max_health\",base:"+(clone?15000:54000)+"},{id:\"minecraft:follow_range\",base:200}]}";}
            default->{entity=new Marker(EntityTypes.MARKER,level);nbt="{Tags:[\"slowraycast\",\"magic\",\"other\""+(kind==Kind.FOG?",\"actived\"":"")+",\""+(kind==Kind.FOG?"whale_fog":"black_snake")+"\""+(kind==Kind.FRAGMENT?",\"fragm\"":"")+"]}";}
        }
        if(entity==null)return null;entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),parse(nbt)));
        entity.setPos(at);entity.setYRot(yaw);entity.setXRot(pitch);entity.entityTags().add(MANAGED);game.items().markCurrentItemEpoch(entity);
        var exclusions=new HashSet<>(Set.of("strict_map_object","rcdenyalways","magic_academy"));
        if(kind==Kind.SNAKE||kind==Kind.FRAGMENT)exclusions.add("sin_gluttony");
        var source=new LbcSpellSource.Source(entity.getUUID(),author,epoch,cause,exclusions,true);
        actors.put(entity,new Actor(kind,source));game.items().spellSources().bind(entity,source);game.items().timeStop().prepareFrozen(entity);
        var controller=game.server().getPlayerList().getPlayer(author);if(controller!=null)game.items().communism().subjugate(controller,entity,true,true);
        if(level.addFreshEntity(entity))return entity;actors.remove(entity);entity.discard();return null;
    }
    private static void velocity(Entity actor,Vec3 direction,double speed){var v=direction.scale(speed);actor.setDeltaMovement(Math.floor(v.x*1000)*.001,Math.floor(v.y*1000)*.001,Math.floor(v.z*1000)*.001);actor.needsSync=true;}
    private void forward(Entity actor,double step){actor.setPos(actor.position().add(actor.getLookAngle().scale(step)));}
    private void face(Entity actor,Entity target){var dir=target.getEyePosition().subtract(actor.position());actor.setYRot((float)Math.toDegrees(Math.atan2(-dir.x,dir.z)));actor.setXRot((float)-Math.toDegrees(Math.atan2(dir.y,dir.horizontalDistance())));}
    private Entity randomTarget(Entity actor,boolean wrath){var targets=select((ServerLevel)actor.level(),actor.position(),"@e[type=#minecraft:mobs,tag=!spectator,tag=!strict_map_object,tag=!sin_gluttony,tag=!white_whale,tag=!white_whale_clone"+(wrath?",tag=!sin_wrath":"")+"]").stream().filter(e->game.items().wands().validRoleTarget(e,false)&&!LbcGreed.active(e)).toList();return targets.isEmpty()?null:targets.get(((ServerLevel)actor.level()).getRandom().nextInt(targets.size()));}
    Entity randomSnakeTarget(Entity actor){var targets=select((ServerLevel)actor.level(),actor.position(),"@e[type=#minecraft:mobs,tag=!spectator,tag=!strict_map_object,tag=!sin_gluttony]").stream().filter(e->game.items().wands().validRoleTarget(e,false)&&!LbcGreed.active(e)).toList();return targets.isEmpty()?null:targets.get(((ServerLevel)actor.level()).getRandom().nextInt(targets.size()));}
    public void tick(){if(advancing)throw new IllegalStateException("Reentrant Gluttony advance");advancing=true;try{int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        boolean second=++phase%20==0;
        for(var entry:List.copyOf(actors.entrySet())){
            var actor=entry.getKey();var state=entry.getValue();var level=(ServerLevel)actor.level();
            if(actor.isRemoved()||!actor.isAlive()){actors.remove(actor);continue;}
            if(!live(state.source.epoch(),level)){actor.discard();continue;}if(paused(level)||game.items().timeStop().frozen(actor))continue;
            var controller=holder(actor);if(controller!=null&&game.items().communism().owner(actor)!=controller)game.items().communism().subjugate(controller,actor,true,true);
            var source=game.items().spellSources().source(actor);if(source!=null)state.source=source;
            state.ticks++;
            switch(state.kind){
                case RABBIT->{if(state.ticks%20==0&&!suppressed(actor)&&count(level,actor.position(),Kind.RABBIT)<=99)spawn(Kind.RABBIT,level,actor.position(),0,0,state.source.author(),state.source.epoch(),state.source.cause());}
                case SNAKE,FRAGMENT->{if(suppressed(actor)){actor.discard();continue;}snake(actor,state);}
                case WHALE,CLONE->whale((Ghast)actor,state);
                case FOG->{if(suppressed(actor)){actor.discard();continue;}fog(actor,state);}
            }
        }
        if(second)sculkContact();
        }finally{advancing=false;}
    }
    private void snake(Entity actor,Actor state){var level=(ServerLevel)actor.level();var random=level.getRandom();
        fill(actor,state,actor.position(),3,true,false);fill(actor,state,actor.position(),1,false,false);
        if(random.nextFloat()<.06f)fragment(actor);
        if(state.kind==Kind.FRAGMENT){int y=Mth.floor(actor.getY());if(y<0||y>319){actor.discard();return;}forward(actor,1);}
        else{
            var side=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("snake_storona"),"lbc.math");
            if(random.nextFloat()<.01f)side.set(1);if(random.nextFloat()<.01f)side.set(2);
            int y=Mth.floor(actor.getY());if(y<=10){actor.setPos(actor.position().add(0,30,0));actor.setXRot(actor.getXRot()-5);}if(y>=310){actor.setPos(actor.position().add(0,-30,0));actor.setXRot(actor.getXRot()+5);}
            if(side.get()==1||side.get()==2){forward(actor,.5);actor.setYRot(actor.getYRot()+(side.get()==1?2:-2));}
            fill(actor,state,actor.position(),4,false,false);fill(actor,state,actor.position(),7,true,false);
            if(random.nextFloat()<.0025f){var target=randomSnakeTarget(actor);if(target!=null){face(actor,target);forward(actor,.5);}}
        }
        for(var e:select(level,actor.position(),"@e[type=#minecraft:mobs,distance=..5,tag=!sin_gluttony]"))if(e instanceof LivingEntity living&&game.items().spellSources().target(state.source,living))
            living.hurtServer(level,game.items().indirectKills().captureDamage(actor.damageSources().source(DamageTypes.GENERIC,actor),state.source.cause()),.5f);
        if(state.kind==Kind.FRAGMENT&&state.ticks>=20)actor.discard();
    }
    /** (user) Preserve the first fragment's activation of its still-unactivated parent/head. */
    Entity fragment(Entity parent){var state=actors.get(parent);if(state==null||(state.kind!=Kind.SNAKE&&state.kind!=Kind.FRAGMENT)||count((ServerLevel)parent.level(),parent.position(),Kind.SNAKE)>78)return null;
        var child=spawn(Kind.FRAGMENT,(ServerLevel)parent.level(),parent.position(),0,0,state.source.author(),state.source.epoch(),state.source.cause());if(child==null)return null;
        var activated=parent.entityTags().contains("actived")?child:parent;var random=((ServerLevel)parent.level()).getRandom();activated.setYRot(random.nextInt(721)-360);activated.setXRot(random.nextInt(181)-90);activated.entityTags().add("actived");return child;
    }
    private void whale(Ghast actor,Actor state){var level=(ServerLevel)actor.level();boolean clone=state.kind==Kind.CLONE;
        fill(actor,state,actor.position(),5,false,true);
        for(var e:select(level,actor.position(),"@e[type=minecraft:item,dx=6,dy=6,dz=6]"))e.discard();
        particle(level,actor.position(),Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE),6,10);
        for(int i=1;i<=3;i++)if(level.getRandom().nextFloat()<.01f)sound(level,clone,i);
        if(state.ticks%20!=0)return;
        if(clone){var mains=select(level,actor.position(),"@e[type=minecraft:ghast,tag=white_whale,limit=1]");
            if(mains.isEmpty()){actor.hurtServer(level,actor.damageSources().genericKill(),Float.MAX_VALUE);return;}
            if(mains.getFirst() instanceof LivingEntity main)actor.setHealth(main.getHealth());
            if(!suppressed(actor)){if(level.getRandom().nextFloat()<.03f)charge(actor,false);if(level.getRandom().nextFloat()<.03f)emitFog(actor,state,false);}
        }else{int hp=Mth.floor(actor.getHealth());
            if(hp>=3001&&hp<=5000&&level.getRandom().nextFloat()<.05f)charge(actor,true);
            if(hp>=3001&&hp<=4000&&level.getRandom().nextFloat()<.1f)emitFog(actor,state,true);
            if(hp<=3000&&!actor.entityTags().contains("cloned")){
                actor.entityTags().add("cloned");for(int i=0;i<3;i++)spawn(Kind.CLONE,level,actor.position(),0,0,state.source.author(),state.source.epoch(),state.source.cause());
                actor.teleportTo(0,150,0);actor.setDeltaMovement(0,10,0);actor.needsSync=true;sounds(level,true);
            }
        }
    }
    private void charge(Entity actor,boolean wrath){var target=randomTarget(actor,wrath);if(target!=null){face(actor,target);velocity(actor,actor.getLookAngle(),7);}}
    private void emitFog(Entity actor,Actor state,boolean wrath){var target=randomTarget(actor,wrath);if(target!=null){face(actor,target);spawn(Kind.FOG,(ServerLevel)actor.level(),actor.position(),actor.getYRot(),actor.getXRot(),state.source.author(),state.source.epoch(),state.source.cause());}}
    private static void sound(ServerLevel level,boolean clone,int number){LbcOverload.sound(level,new Vec3(0,1000000,0),"minecraft:lbcsounds.whale_"+(clone?"clone_":"")+number,1,1,0,1);}
    private static void sounds(ServerLevel level,boolean clone){for(int i=1;i<=3;i++)sound(level,clone,i);}
    private static void particle(ServerLevel level,Vec3 at,Block block,double spread,int count){var particle=new BlockParticleOption(ParticleTypes.BLOCK_MARKER,block.defaultBlockState());for(var viewer:level.players())level.sendParticles(viewer,particle,true,false,at.x,at.y,at.z,count,spread,spread,spread,0);}
    private void fog(Entity actor,Actor state){forward(actor,1);var level=(ServerLevel)actor.level();particle(level,actor.position(),Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY),2,10);fogKill(actor,state,2);
        var cell=BlockPos.containing(actor.position());if(level.getChunkSource().getChunkNow(cell.getX()>>4,cell.getZ()>>4)!=null&&!level.getBlockState(cell).is(AIRS)){particle(level,actor.position(),Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY),10,1000);fogKill(actor,state,5);}
        if(state.ticks>=100)actor.discard();
    }
    private void fogKill(Entity actor,Actor state,int radius){for(var target:select((ServerLevel)actor.level(),actor.position(),"@e[distance=.."+radius+",tag=!white_whale,tag=!white_whale_clone,tag=!whale_fog,tag=!sin_gluttony,tag=!sin_wrath,tag=!spectator,tag=!sin_greed_active,tag=!strict_map_object]"))
        if(game.items().spellSources().target(state.source,target)&&!actor.isAlliedTo(target)&&!sameTeam(actor,target)&&game.items().mana().hudValue(target,"void_resistance")<=0){
            if(target instanceof LivingEntity living)game.items().indirectKills().inheritedExecution(living,state.source.cause());else target.kill((ServerLevel)target.level());}}
    private boolean sameTeam(Entity a,Entity b){if(LbcWtf.owner(b) instanceof ServerPlayer owner){if(a.isAlliedTo(owner))return true;b=owner;}var board=game.server().getScoreboard();var objective=board.getObjective("team_number");if(objective==null)return false;var x=board.getPlayerScoreInfo(a,objective);var y=board.getPlayerScoreInfo(b,objective);return x!=null&&y!=null&&x.value()==y.value();}
    private void sculkContact(){Entity snake=null;for(var world:game.server().getAllLevels()){var found=select(world,Vec3.ZERO,"@e[type=minecraft:marker,tag=black_snake,limit=1]");if(!found.isEmpty()){snake=found.getFirst();break;}}
        if(snake==null)return;var state=actors.get(snake);if(state==null)return;
        for(var target:select((ServerLevel)snake.level(),snake.position(),"@e[type=#minecraft:mobs,tag=!spectator,tag=!sin_gluttony,tag=!sin_greed_active]"))if(target instanceof LivingEntity living&&LbcDimensionItems.ordinary(target.level())&&!paused((ServerLevel)target.level())&&game.items().spellSources().target(state.source,target)){
            var world=(ServerLevel)target.level();for(double y:new double[]{-.5,1})for(var offset:List.of(new Vec3(.3,y,0),new Vec3(-.3,y,0),new Vec3(0,y,.3),new Vec3(0,y,-.3))){var pos=BlockPos.containing(target.position().add(offset));if(world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null&&world.getBlockState(pos).is(Blocks.SCULK))curse(living,state.source.cause());}
        }
    }
    private void curse(LivingEntity target,LbcIndirectKills.Owner cause){List<Holder<MobEffect>> types=List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.HUNGER,MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.POISON,MobEffects.SLOWNESS,MobEffects.UNLUCK,MobEffects.WEAKNESS,MobEffects.WITHER);int[] amps={1,2,4,8,16,32,64,127,127,127};for(int i=0;i<types.size();i++)game.items().indirectKills().inheritedEffect(target,ItemEffectLevels.create(types.get(i),(i+1)*200,amps[i]),cause);}
    private void fill(Entity actor,Actor state,Vec3 at,int radius,boolean tagged,boolean air){
        var level=(ServerLevel)actor.level();Terrain work=null;for(var candidate:terrain)if(candidate.actor.equals(actor.getUUID())&&candidate.tagged==tagged&&candidate.air==air&&candidate.source==state.source){work=candidate;break;}
        if(work==null){work=new Terrain(actor.getUUID(),level,state.source,tagged,air);terrain.add(work);game.items().stonekivi().queuedWork(work);}
        var pos=BlockPos.containing(at);work.area.include(new BlockAreaWork.Bounds(pos.getX()-radius,pos.getY()-radius,pos.getZ()-radius,pos.getX()+radius,pos.getY()+radius,pos.getZ()+radius));
    }
    private final class Terrain implements LbcStonekivi.Work {
        final UUID actor;final ServerLevel level;final LbcSpellSource.Source source;final boolean tagged,air;final BlockAreaWork area;boolean cancelled;
        Terrain(UUID actor,ServerLevel level,LbcSpellSource.Source source,boolean tagged,boolean air){this.actor=actor;this.level=level;this.source=source;this.tagged=tagged;this.air=air;area=new BlockAreaWork(level,s->air?!s.isAir():!s.is(Blocks.SCULK)&&(!tagged||s.is(TANNER)),(p,s)->air?Blocks.AIR.defaultBlockState():Blocks.SCULK.defaultBlockState(),818);}
        public boolean ready(){return cancelled||!live(source.epoch(),level)||!paused(level);}
        public boolean step(){if(cancelled||!live(source.epoch(),level)){cancel();return true;}area.step(game,null,source.cause(),4096,System.nanoTime()+500_000);if(!area.hasWork()){cancel();return true;}return false;}
        public void cancel(){cancelled=true;area.clear();terrain.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){var cells=area.checkpoint();boolean dead=cancelled;return links->{MatchAuthorityCheckpoint.world(game,level);return ()->{cells.restore();cancelled=dead;};};}
    }
    public void loaded(Entity entity){if(!entity.entityTags().contains(MANAGED))return;var state=actors.get(entity);if(state==null)state=unloaded.remove(entity.getUUID());if(state==null||!live(state.source.epoch(),(ServerLevel)entity.level())){entity.discard();return;}actors.put(entity,state);game.items().spellSources().bind(entity,state.source);game.items().timeStop().prepareFrozen(entity);}
    public void unloaded(Entity entity){var state=actors.remove(entity);if(state!=null&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(entity.getUUID(),state);}
    List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    public void stop(){pending.clear();for(var actor:List.copyOf(actors.keySet()))actor.discard();actors.clear();unloaded.clear();for(var work:List.copyOf(terrain))work.cancel();phase=0;lastTick=Integer.MIN_VALUE;}
    void checkpointBoundary(){if(advancing)throw new IllegalStateException("Finish Gluttony callbacks before checkpointing");}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){return new Checkpoint(graph);}
    final class Checkpoint {
        private record Image(Kind kind,LbcSpellSource.Source source,int ticks) { }
        private final Map<MatchDamageSources.Ref,Image> alive=new LinkedHashMap<>();private final Map<UUID,Image> asleep=new HashMap<>();
        private final Set<Kind> waiting=Set.copyOf(pending);private final List<Terrain> jobs=List.copyOf(terrain);private final int cadence=phase;
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){var aliases=new IdentityHashMap<Actor,Image>();actors.forEach((actor,state)->alive.put(graph.links.key(actor),image(state,aliases)));unloaded.forEach((id,state)->asleep.put(id,image(state,aliases)));jobs.forEach(graph::capture);}
        private Image image(Actor actor,IdentityHashMap<Actor,Image> aliases){return aliases.computeIfAbsent(actor,a->new Image(a.kind,a.source,a.ticks));}
        private Actor actor(Image image,IdentityHashMap<Image,Actor> aliases){return aliases.computeIfAbsent(image,i->{var actor=new Actor(i.kind,i.source);actor.ticks=i.ticks;return actor;});}
        Runnable prepare(MatchSpellCheckpoint.Links links){var aliases=new IdentityHashMap<Image,Actor>();var active=new LinkedHashMap<Entity,Actor>();var dormant=new HashMap<UUID,Actor>();
            alive.forEach((ref,image)->active.put(links.get(ref),actor(image,aliases)));asleep.forEach((id,image)->dormant.put(id,actor(image,aliases)));jobs.forEach(j->MatchAuthorityCheckpoint.world(game,j.level));
            return ()->{actors.clear();actors.putAll(active);unloaded.clear();unloaded.putAll(dormant);pending.clear();pending.addAll(waiting);terrain.clear();terrain.addAll(jobs);phase=cadence;lastTick=clock.last(game);};}
    }
}
