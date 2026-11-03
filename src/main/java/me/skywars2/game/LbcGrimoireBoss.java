package me.skywars2.game;

import com.mojang.math.Transformation;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Dungeon encounter. Native entity attachments own all clocks, identities and causal state. */
public final class LbcGrimoireBoss {
    public static void register(){}
    public static final String TAG="skywars2.grimoire_boss";
    private static final String MUSIC="minecraft:lbcsounds.penis_bossfight";
    private static final Set<String> EXCLUDED=Set.of("dungeon_mob","strict_map_object");
    private static final net.minecraft.tags.TagKey<EntityType<?>> MOBS=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final AttachmentType<CompoundTag> DATA=AttachmentRegistry.create(
            Identifier.parse("skywars2:grimoire_boss"),b->b.persistent(CompoundTag.CODEC));
    private final GameController game;
    // Derived loaded-body indexes/presentation only; no encounter state is stored here.
    private final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID,ServerBossEvent> bars=new HashMap<>();
    private final Map<Entity,Warning> warnings=new IdentityHashMap<>();
    private record Warning(TrialWarningVisual visual,int phase,int age,Vec3 from,Vec3 to){}
    public LbcGrimoireBoss(GameController game){this.game=game;}
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}
    private static CompoundTag data(Entity entity){return entity.getAttached(DATA);}
    private static void save(Entity entity,CompoundTag state){entity.setAttached(DATA,state);}
    private static int integer(CompoundTag state,String key){return state.getIntOr(key,0);}
    private static UUID id(CompoundTag state,String key){try{return UUID.fromString(state.getStringOr(key,""));}catch(IllegalArgumentException e){return null;}}
    private static Vec3 vector(CompoundTag state,String key){return new Vec3(state.getDoubleOr(key+"X",0),state.getDoubleOr(key+"Y",0),state.getDoubleOr(key+"Z",0));}
    private static void vector(CompoundTag state,String key,Vec3 v){state.putDouble(key+"X",v.x);state.putDouble(key+"Y",v.y);state.putDouble(key+"Z",v.z);}
    private LbcSpellSource.Source source(Entity entity){
        var d=data(entity);if(d==null)return null;var author=id(d,"author");var emitter=id(d,"boss");if(author==null||emitter==null)return null;
        return new LbcSpellSource.Source(emitter,author,d.getStringOr("epoch",""),
                new LbcIndirectKills.Owner(author,d.getLongOr("order",0),true,false),EXCLUDED,true);
    }
    private CompoundTag childState(Warden boss,int kind){var d=data(boss).copy();d.putInt("kind",kind);d.putInt("age",0);d.remove("music");return d;}
    /** The supplied position is the real stationary hitbox's feet; yaw is the source visual's yaw. */
    public Warden spawn(ServerLevel level,Vec3 at,float yaw,ServerPlayer author){
        if(author==null)return null;
        return spawn(level,at,yaw,new LbcSpellSource.Source(author.getUUID(),author.getUUID(),game.items().epoch(),
                game.items().indirectKills().wildCause(author),EXCLUDED,true));
    }
    /** Accepted dungeon continuations retain their original author/cause without resolving a player body. */
    public Warden spawn(ServerLevel level,Vec3 at,float yaw,LbcSpellSource.Source authority){
        if(authority==null||authority.author()==null||authority.cause()==null||!game.items().currentEpoch(authority.epoch())
                ||!game.playing()||game.stage()==GameController.Stage.ENDING||game.items().timeStop().stopped(level)||!resident(level,at))return null;
        var boss=EntityTypes.WARDEN.create(level,EntitySpawnReason.COMMAND);if(boss==null)return null;
        load(boss,"{Silent:true,PersistenceRequired:true,NoAI:true,CanPickUpLoot:false,DeathLootTable:\"lbc:grimoire_boss\"}");
        boss.setPos(at);boss.setYRot(yaw);boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);boss.setHealth(1000);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(1);boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(1);
        boss.setCustomName(Component.translatable("grimoire_boss").withStyle(ChatFormatting.DARK_RED).withStyle(s->s.withItalic(false)));
        boss.addEffect(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.INVISIBILITY,-1,1,false,false));
        boss.entityTags().addAll(List.of(TAG,"grimoire_boss_hitbox","dungeon_mob"));game.items().markCurrentItemEpoch(boss);
        var d=new CompoundTag();d.putString("author",authority.author().toString());d.putString("boss",boss.getStringUUID());d.putString("epoch",authority.epoch());
        d.putLong("order",authority.cause().order());d.putInt("kind",0);d.putInt("phase",1);d.putInt("age",0);d.putInt("total",0);
        vector(d,"anchor",at);vector(d,"aim",Vec3.directionFromRotation(0,yaw));save(boss,d);
        if(!add(level,boss)){boss.discard();return null;}
        var visual=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);visual.setPos(at.add(0,6,0));visual.setYRot(yaw);
        load(visual,"{view_range:10f,shadow_radius:0f,shadow_strength:0f,item_display:\"fixed\",teleport_duration:1,interpolation_duration:1}");
        var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:grimoire_boss"));visual.setItemStack(stack);
        visual.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(4),new Quaternionf()));
        visual.entityTags().addAll(List.of("grimoire_boss_visual","dungeon_mob","mob"));save(visual,childState(boss,1));add(level,visual);
        redCore(boss);level.sendParticles(new net.minecraft.core.particles.ShriekParticleOption(5),at.x,at.y+1,at.z,20,1,1,1,0);sound(level,at,"minecraft:block.sculk_shrieker.shriek",2,1);
        return boss;
    }
    public Warden spawn(ServerLevel level,Vec3 at,ServerPlayer author){return spawn(level,at,-90,author);}
    private boolean add(ServerLevel level,Entity entity){
        entity.entityTags().add(TAG);game.items().markCurrentItemEpoch(entity);game.items().spellSources().bind(entity,source(entity));game.items().timeStop().prepareFrozen(entity);
        actors.add(entity);if(!level.addFreshEntity(entity)){actors.remove(entity);return false;}return true;
    }
    public void loaded(Entity entity){
        if(!managed(entity))return;var d=data(entity);
        if(d==null||!game.items().currentEpoch(d.getStringOr("epoch",""))||!game.playing()){if(!MatchActorCheckpoint.restoring(entity))entity.discard();return;}
        actors.add(entity);game.items().spellSources().bind(entity,source(entity));game.items().timeStop().prepareFrozen(entity);
    }
    public void unloaded(Entity entity){actors.remove(entity);closeWarning(entity);if(entity instanceof Warden)closeBar(entity.getUUID(),(ServerLevel)entity.level());}
    public void tick(){
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        // Boss first: child clocks then advance once, including children born this update.
        for(var actor:List.copyOf(actors))if(actor instanceof Warden)tickBoss((Warden)actor);
        for(var actor:List.copyOf(actors))if(!(actor instanceof Warden))tickChild(actor);
    }
    private boolean stale(Entity actor){var d=data(actor);return d==null||!game.items().currentEpoch(d.getStringOr("epoch",""));}
    private boolean paused(Entity actor){return game.items().timeStop().stopped((ServerLevel)actor.level())||actor.entityTags().contains("time_stopped")||!resident((ServerLevel)actor.level(),actor.position());}
    private void tickBoss(Warden boss){
        if(stale(boss)){retire(boss);return;}
        if(boss.isRemoved()||!boss.isAlive()){retire(boss);return;}
        var level=(ServerLevel)boss.level();bar(boss);if(paused(boss))return;
        var d=data(boss).copy();int total=integer(d,"total")+1;d.putInt("total",total);
        if(total==40){stopMusic(level);sound(level,boss.position().add(0,6,0),MUSIC,.5f,1);}
        if(game.items().magic().suppressedAt(level,boss.position())||game.items().magic().suppressedAt(level,origin(boss))){save(boss,d);closeWarning(boss);return;}
        int age=integer(d,"age")+1,phase=integer(d,"phase");d.putInt("age",age);
        if(boss.hurtTime==10){var at=origin(boss);level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,Blocks.TUFF_BRICKS.defaultBlockState()),at.x,at.y+2,at.z,40,1,1.6,1,0);sound(level,at,"minecraft:entity.iron_golem.hurt",1,0);}
        if(phase==0&&boss.getRandom().nextInt(100)==0){phase=1+boss.getRandom().nextInt(3);d.putInt("phase",phase);d.putInt("age",0);age=0;if(phase==1)redCore(boss);}
        save(boss,d);
        if(phase==1)laser(boss,age);
        else if(phase==2&&age>=30&&boss.getRandom().nextBoolean()){
            if(age<=200)meteors(boss);if(age>=250)idle(boss);
        }else if(phase==3&&age>=20&&boss.getRandom().nextInt(100)<30){
            danmaku(boss);if(age>=200){for(int i=0;i<24;i++)danmaku(boss);idle(boss);}
        }
    }
    private void idle(Warden boss){var d=data(boss).copy();d.putInt("phase",0);save(boss,d);closeWarning(boss);}
    private Vec3 origin(Warden boss){return boss.position().add(0,6,0);}
    private void laser(Warden boss,int age){
        var level=(ServerLevel)boss.level();var from=origin(boss);var d=data(boss).copy();var direction=vector(d,"aim");
        var targets=targets(boss,from,40,1);if(!targets.isEmpty()&&(age==1||age>=71)){
            var wanted=targets.getFirst().getEyePosition().subtract(from).normalize();
            // Source smooth tracking: point .15 toward target from current unit look, then normalize.
            direction=age<=71?wanted:direction.add(wanted.scale(.15)).normalize();vector(d,"aim",direction);save(boss,d);
        }
        var to=clipAir(level,from,direction,60);
        if(age<=70){warning(boss,1,age,from,to,71-age);if(age==1)sound(level,from,"minecraft:block.beacon.activate",2,0);if(age==45)sound(level,from,"minecraft:lbcsounds.ui_beam",3,1);return;}
        closeWarning(boss);
        if(age>=71&&!game.items().magic().suppressedAt(level,from)){
            var damage=damage(boss,DamageTypes.MAGIC);
            // Shared exact line/body admission and source .3-step multiplicity; warning is visual only.
            LbcDamage.hurtSegment(level,damage,from,to,Vec3.ZERO,.3,10,2,e->target(boss,e),true);
            for(double n=0;n<=from.distanceTo(to);n+=.3){var p=from.add(direction.scale(n));level.sendParticles(new net.minecraft.core.particles.DustColorTransitionOptions(0xffee00,0xff8800,3),p.x,p.y,p.z,1,0,0,0,.15);}
        }
        if(age>=250){for(var e:List.copyOf(actors))if(childOf(e,boss)&&data(e)!=null&&integer(data(e),"kind")==4)remove(e);var next=data(boss).copy();next.putInt("age",0);save(boss,next);idle(boss);}
    }
    private Vec3 clipAir(ServerLevel level,Vec3 from,Vec3 direction,double length){
        // Legacy continues only through air at each .3step. A full voxel clip stops exactly at the first non-air face.
        var to=from.add(direction.scale(length));
        double fraction=game.items().magic().suppressionFraction(level,from,to);if(Double.isFinite(fraction))to=from.lerp(to,fraction);
        final var endpoint=to;
        var hit=net.minecraft.world.level.BlockGetter.traverseBlocks(from,endpoint,level,(world,pos)->{
            if(world.hasChunkAt(pos)&&world.getBlockState(pos).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse("minecraft:airs"))))return null;
            return net.minecraft.world.phys.shapes.Shapes.block().clip(from,endpoint,pos);
        },world->null);
        return hit==null?to:hit.getLocation();
    }
    private void warning(Entity actor,int phase,int age,Vec3 from,Vec3 to,int life){
        var previous=warnings.get(actor);
        if(previous!=null&&previous.phase()==phase&&previous.age()+1==age&&previous.from().distanceToSqr(from)<1e-12&&previous.to().distanceToSqr(to)<1e-12){
            previous.visual().tick();warnings.put(actor,new Warning(previous.visual(),phase,age,from,to));return;
        }
        closeWarning(actor);var visual=TrialWarningVisual.worldBeam(game,(ServerLevel)actor.level(),data(actor).getStringOr("epoch",""),from,to,false,Math.max(1,life));
        warnings.put(actor,new Warning(visual,phase,age,from,to));
    }
    private void closeWarning(Entity entity){var v=warnings.remove(entity);if(v!=null)v.visual().close();}
    private void redCore(Warden boss){
        var level=(ServerLevel)boss.level();var e=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level);e.setPos(origin(boss).add(-.5,-.5,-.5));
        load(e,"{block_state:{Name:\"minecraft:redstone_block\"},view_range:10f,shadow_radius:0f,shadow_strength:0f,interpolation_duration:1,teleport_duration:1,brightness:{sky:15,block:15}}");
        e.entityTags().add("spinning_red_penis");save(e,childState(boss,4));add(level,e);
    }
    private void meteors(Warden boss){
        var level=(ServerLevel)boss.level();var center=origin(boss).add(Vec3.directionFromRotation(0,boss.getYRot()).scale(7)).add(0,20,0);
        // Native numeric command results floor positions before inclusive random offsets.
        int x=(int)Math.floor(center.x)+boss.getRandom().nextInt(31)-15,z=(int)Math.floor(center.z)+boss.getRandom().nextInt(31)-15;
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(boss.getRandom().nextInt(100)<40){
            var e=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level);e.setPos(x+dx,Math.floor(center.y),z+dz);
            load(e,"{block_state:{Name:\"minecraft:chiseled_tuff_bricks\"},view_range:100f,shadow_radius:0f,shadow_strength:0f,teleport_duration:1,interpolation_duration:1}");
            e.entityTags().add("falling_tuff");save(e,childState(boss,3));add(level,e);
        }
    }
    private void danmaku(Warden boss){
        var d=data(boss).copy();int rot=integer(d,"rotation");
        for(var target:targets(boss,origin(boss),30,3)){
            rot=boss.getRandom().nextInt(7)==0?boss.getRandom().nextInt(361)-180:rot+93;
            var direction=Vec3.directionFromRotation(0,rot);var at=target.position().add(0,1,0).subtract(direction.scale(10));
            var e=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,boss.level());e.setPos(at);e.setYRot(rot);
            load(e,"{shadow_radius:0f,shadow_strength:0f,teleport_duration:1,interpolation_duration:1,item_display:\"fixed\"}");
            var stack=new ItemStack(Items.CARROT_ON_A_STICK);stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:prism"));e.setItemStack(stack);
            e.setTransformation(new Transformation(new Vector3f(),new Quaternionf().rotateY((float)(Math.PI/2)),new Vector3f(1),new Quaternionf()));
            e.entityTags().addAll(List.of("boss_prism","magic"));var child=childState(boss,2);vector(child,"aim",direction);save(e,child);add((ServerLevel)boss.level(),e);
        }
        d.putInt("rotation",rot);save(boss,d);
    }
    private void tickChild(Entity actor){
        if(actor.isRemoved()){actors.remove(actor);closeWarning(actor);return;}if(stale(actor)){remove(actor);return;}if(paused(actor))return;
        var d=data(actor).copy();var level=(ServerLevel)actor.level();var parent=level.getEntity(id(d,"boss"));int kind=integer(d,"kind");
        if(kind!=3){
            if(parent instanceof LivingEntity living&&!living.isAlive()){remove(actor);return;}
            if(parent==null){if(resident(level,vector(d,"anchor")))remove(actor);return;}
        }
        int age=integer(d,"age")+1;d.putInt("age",age);save(actor,d);
        if(kind==1){actor.setPos(parent.position().add(0,6,0));return;}
        if(kind==4){actor.setPos(parent.position().add(-.5,5.5,-.5));return;}
        if(kind==2){
            if(game.items().magic().suppressedAt(level,actor.position())){remove(actor);return;}
            var from=actor.position();var direction=Vec3.directionFromRotation(actor.getXRot(),actor.getYRot());var to=from.add(direction.scale(27));double boundary=game.items().magic().suppressionFraction(level,from,to);if(Double.isFinite(boundary))to=from.lerp(to,boundary);
            if(age<31){warning(actor,2,age,from,to,31-age);if(age==1||age==11||age==21)sound(level,from,"minecraft:block.beacon.activate",1,2);}
            else{closeWarning(actor);var damage=damage(actor,DamageTypes.ARROW);var hit=new HashSet<UUID>();
                LbcDamage.hurtSegment(level,damage,from,to,Vec3.ZERO,.3,7,1,e->target(actor,e)&&!hit.contains(e.getUUID()),true,e->hit.add(e.getUUID()),null);
                for(int n=0;n<=90;n++){var p=from.add(direction.scale(n*.3));level.sendParticles(ParticleTypes.FIREWORK,p.x,p.y,p.z,1,.01,.01,.01,.01);}
                sound(level,from,"minecraft:lbcsounds.laser_attack",1.5f,1);remove(actor);
            }
        }else if(kind==3){
            actor.setPos(actor.position().add(0,-1,0));var at=actor.position().add(.5,0,.5);int hit=0;
            for(var e:level.getEntities((Entity)null,new AABB(at,at).inflate(.71),v->target(actor,v)))if(e.position().distanceToSqr(at)<=.71*.71&&hit++<2)e.hurtServer(level,damage(actor,DamageTypes.FALLING_BLOCK),10);
            var pos=actor.blockPosition().above();if(!level.getBlockState(pos).isAir()){
                if(!level.getBlockState(pos).is(Blocks.CHISELED_TUFF_BRICKS))game.items().indirectKills().trapChanges(level,source(actor).cause(),()->level.destroyBlock(pos,true));
                sound(level,actor.position(),"minecraft:lbcsounds.bonk",.5f,1);remove(actor);
            }else if(age>=100)remove(actor);
        }
    }
    private boolean target(Entity actor,Entity candidate){return candidate instanceof LivingEntity&&candidate.getType().builtInRegistryHolder().is(MOBS)&&game.items().spellSources().target(source(actor),candidate);}
    private List<LivingEntity> targets(Entity actor,Vec3 point,double radius,int limit){
        var level=(ServerLevel)actor.level();return level.getEntities((Entity)null,new AABB(point,point).inflate(radius),e->target(actor,e)&&e.position().distanceToSqr(point)<=radius*radius)
                .stream().map(e->(LivingEntity)e).sorted(Comparator.comparingDouble(e->e.position().distanceToSqr(point))).limit(limit).toList();
    }
    private DamageSource damage(Entity actor,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type){
        return game.items().indirectKills().captureDamage(actor.damageSources().source(type,actor),source(actor).cause());
    }
    private boolean childOf(Entity actor,Entity boss){var d=data(actor);return actor!=boss&&d!=null&&boss.getUUID().equals(id(d,"boss"));}
    private void retire(Warden boss){for(var child:List.copyOf(actors))if(childOf(child,boss)&&integer(data(child),"kind")!=3)remove(child);actors.remove(boss);closeWarning(boss);closeBar(boss.getUUID(),(ServerLevel)boss.level());if(!boss.isRemoved()&&boss.isAlive())boss.discard();}
    private void remove(Entity actor){actors.remove(actor);closeWarning(actor);actor.discard();}
    private void bar(Warden boss){
        var bar=bars.computeIfAbsent(boss.getUUID(),u->new ServerBossEvent(u,Component.translatable("grimoire_boss").withStyle(ChatFormatting.RED),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS));
        bar.setProgress(Math.clamp(boss.getHealth()/1000f,0,1));
        for(var p:List.copyOf(bar.getPlayers()))if(p.level()!=boss.level()||p.hasDisconnected())bar.removePlayer(p);
        for(var p:((ServerLevel)boss.level()).players())bar.addPlayer(p);
    }
    private void closeBar(UUID boss,ServerLevel level){var bar=bars.remove(boss);if(bar!=null){bar.removeAllPlayers();if(bars.isEmpty())stopMusic(level);}}
    private void stopMusic(ServerLevel level){for(var p:level.players())p.connection.send(new ClientboundStopSoundPacket(Identifier.parse(MUSIC),(SoundSource)null));}
    private static void sound(ServerLevel level,Vec3 at,String sound,float volume,float pitch){LbcOverload.sound(level,at,sound,volume,pitch,0,0);}
    private static boolean resident(ServerLevel level,Vec3 at){return level.getChunkSource().getChunkNow(BlockPos.containing(at).getX()>>4,BlockPos.containing(at).getZ()>>4)!=null;}
    private static void load(Entity entity,String nbt){
        // These are configuration presets: native load defaults absent Pos/Rotation/Motion to zero.
        var position=entity.position();var motion=entity.getDeltaMovement();float yaw=entity.getYRot(),pitch=entity.getXRot();
        try{entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),TagParser.parseCompoundFully(nbt)));
            entity.setPos(position);entity.setDeltaMovement(motion);entity.setYRot(yaw);entity.setXRot(pitch);
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
    }
    public void stop(){for(var actor:List.copyOf(actors)){if(actor instanceof Warden boss)retire(boss);else remove(actor);}for(var bar:bars.values())bar.removeAllPlayers();bars.clear();warnings.values().forEach(w->w.visual().close());warnings.clear();}
    public int count(){return actors.size();}
    List<Entity> snapshot(){return List.copyOf(actors);}
    List<TrialWarningVisual> warningSnapshot(){return warnings.values().stream().map(Warning::visual).toList();}

    static boolean cloneProtected(Entity e){var d=data(e);if(!managed(e)||d==null)return false;
        int kind=integer(d,"kind");return kind==1||kind==4;}
    private record CloneChild(CloneEncounterStates.Presentation nativeImage,CompoundTag state){}
    LbcCloneSkills.ActorImage cloneImage(Entity original) {
        var state=data(original);if(state==null||cloneProtected(original))return null;
        var saved=state.copy();UUID originalId=original.getUUID();var originalAt=original.position();
        var children=new ArrayList<CloneChild>();
        if(original instanceof Warden)for(var child:List.copyOf(actors))if(childOf(child,original)){
            int kind=integer(data(child),"kind");if(kind==1||kind==4)
                children.add(new CloneChild(new CloneEncounterStates.Presentation(child),data(child).copy()));
        }
        var visuals=List.copyOf(children);var warning=warnings.get(original);
        int warningPhase=warning==null?0:warning.phase(),warningAge=warning==null?0:warning.age();
        int visualAge=warning==null?0:warning.visual().age(),visualLife=warning==null?0:warning.visual().lifetime();
        Vec3 warningFrom=warning==null?null:warning.from(),warningTo=warning==null?null:warning.to();
        return CloneComplexProjectileStates.image(clone->{
            var level=(ServerLevel)clone.level();var offset=clone.position().subtract(originalAt);var copy=saved.copy();
            if(originalId.equals(id(copy,"boss"))){copy.putString("boss",clone.getStringUUID());
                vector(copy,"anchor",vector(copy,"anchor").add(offset));}
            save(clone,copy);actors.add(clone);game.items().spellSources().bind(clone,source(clone));
            for(var child:visuals){var body=child.nativeImage().create(level);
                body.setPos(child.nativeImage().position.add(offset));var childData=child.state().copy();
                childData.putString("boss",clone.getStringUUID());vector(childData,"anchor",vector(childData,"anchor").add(offset));
                save(body,childData);body.entityTags().add(TAG);game.items().markCurrentItemEpoch(body);
                actors.add(body);game.items().spellSources().bind(body,source(body));game.items().timeStop().prepareFrozen(body);
                if(!level.addFreshEntity(body)||body.isRemoved())throw new IllegalStateException("Grimoire clone presentation rejected");
            }
            if(warningFrom!=null){var from=warningFrom.add(offset);var to=warningTo.add(offset);
                var beam=TrialWarningVisual.worldBeam(game,level,copy.getStringOr("epoch",""),from,to,false,visualLife);
                warnings.put(clone,new Warning(beam,warningPhase,warningAge,from,to));
                for(int i=0;i<visualAge;i++)beam.tick();
            }
            if(clone instanceof Warden boss)bar(boss);
        },clone->{
            for(var child:List.copyOf(actors))if(childOf(child,clone)){
                actors.remove(child);closeWarning(child);game.items().spellSources().reflected(child);child.discard();}
            actors.remove(clone);closeWarning(clone);var bar=bars.remove(clone.getUUID());if(bar!=null)bar.removeAllPlayers();
        });
    }


    /** DATA owns phase/causality; rebuild only derived indexes after native ENTITY_LOAD was suppressed. */
    void reindexRestored(Iterable<Entity> restored){
        for(var warning:List.copyOf(warnings.values()))warning.visual().close();warnings.clear();
        for(var bar:bars.values())bar.removeAllPlayers();bars.clear();actors.clear();
        for(var actor:restored){
            if(actor.isRemoved()||!managed(actor))continue;var state=data(actor);
            if(state==null||!game.playing()||!game.items().currentEpoch(state.getStringOr("epoch","")))continue;
            actors.add(actor);game.items().spellSources().bind(actor,source(actor));
        }
        // The next ordinary tick derives warning geometry/remaining life and bars from DATA.
        // Never call laser/tickChild here: those paths also emit damage and accepted attacks.
    }


    void directionChanged(Entity actor,Vec3 aim) {
        var warning=warnings.get(actor);if(warning==null)return;
        // Use the same native Rotation and suppression clipping as the next tickChild pass.
        // Keep metadata coherent too: warning() otherwise recreates the still-live holder.
        var from=actor.position();
        var direction=Vec3.directionFromRotation(actor.getXRot(),actor.getYRot());
        var to=from.add(direction.scale(27));
        double boundary=game.items().magic().suppressionFraction((ServerLevel)actor.level(),from,to);
        if(Double.isFinite(boundary))to=from.lerp(to,boundary);
        warning.visual().retargetBeam(from,to);
        warnings.put(actor,new Warning(warning.visual(),warning.phase(),warning.age(),from,to));
    }

}
