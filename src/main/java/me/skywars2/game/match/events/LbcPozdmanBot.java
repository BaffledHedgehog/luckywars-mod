package me.skywars2.game.match.events;

import com.mojang.math.Transformation;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.game.GameController;
import me.skywars2.game.combat.LbcDamage;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.LbcSpellSource;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.skills.clone.LbcCloneSkills;
import me.skywars2.game.world.terrain.LbcPhysicalObjects;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Predicate;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import me.skywars2.game.effects.magic.LbcMagic;
import me.skywars2.game.players.teams.LbcTeams;

/** Standalone bot and its complete rare native dolphin/physical bitcoin result. */
public final class LbcPozdmanBot {
    static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    public static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    public static final EntityType<Coin> TYPE=registerType();
    public static final Item ITEM=registerItem();
    private final GameController game;
    private final Predicate<ServerPlayer> allowed;
    private int callbacks;
    private boolean restoring;
    private IdentityHashMap<Entity,Actor> actors=new IdentityHashMap<>();
    private IdentityHashMap<ServerPlayer,Boolean> waiting=new IdentityHashMap<>();
    private final Map<UUID,Actor> dormant=new HashMap<>();
    private final Set<UUID> retiredDormant=new HashSet<>();
    private final Map<ServerLevel,Integer> phases=new IdentityHashMap<>();
    public record Actor(UUID owner, String epoch, int age, float yaw, Display.ItemDisplay display,
                 LbcIndirectKills.Owner cause) {
        Actor frame(int age, float yaw) { return new Actor(owner, epoch, age, yaw, display, cause); }
        Actor visual(Display.ItemDisplay display) { return new Actor(owner, epoch, age, yaw, display, cause); }
    }
    public LbcPozdmanBot(GameController game,Predicate<ServerPlayer> allowed){this.game=game;this.allowed=Objects.requireNonNull(allowed);}
    public static void register(){}
    private static EntityType<Coin> registerType(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:pozdman_bitcoin"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Coin>of(Coin::new,MobCategory.MISC).sized(2,2).clientTrackingRange(10).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);return type;
    }
    private static Item registerItem(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:pozdman_bot"));var model=Identifier.parse("lbc:pozdman_bot");
        var data=new CompoundTag();data.putInt("magicwand",1);data.putInt("pozdman_bot",1);
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).stacksTo(1).overrideDescription("pozdman_bot").component(DataComponents.ITEM_MODEL,model).component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("pozdman_bot").withStyle(s->s.withColor(0xffffff).withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("pozdman_bot.lore.1").withStyle(s->s.withColor(0xffffff).withItalic(false)),Component.translatable("pozdman_bot.lore.2").withStyle(s->s.withColor(0xffffff).withItalic(false))))),Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer owner&&game.items().pozdmanBot().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean matches(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("pozdman_bot",0)!=0;}
    public static final class Coin extends Projectile implements PolymerEntity {
        Coin(EntityType<? extends Coin> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean isPushedByFluid(){return false;}
        @Override public boolean isPushable(){return false;}
    }
    private boolean admitted(ServerPlayer player,InteractionHand hand){
        return !restoring&&matches(player.getItemInHand(hand))&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(player)
                &&player.isAlive()&&!player.isRemoved()&&!game.items().actionsBlocked(player)&&!game.items().wands().inputTimeStopped(player)
                &&!waiting.containsKey(player)&&allowed.test(player);
    }
    public boolean use(ServerPlayer player,InteractionHand hand){return admitted(player,hand)&&use(player,hand,player.getRandom().nextInt(100)+1);}
    public boolean use(ServerPlayer player,InteractionHand hand,int roll){
        if(!admitted(player,hand))return false;
        if(roll<1||roll>100)throw new IllegalArgumentException("Bot roll outside source 1..100");
        waiting.put(player,true);player.entityTags().add("pozdman_wait");
        if(roll<100)player.sendSystemMessage(Component.translatable("pozdman_girl_"+roll));else spawnDolphin(player);
        return true;
    }
    Entity spawnDolphin(ServerPlayer player) {
        return spawnDolphinAt(player, player.getEyePosition(), null);
    }

    public net.minecraft.world.entity.Mob scriptDolphin(ServerPlayer player, Vec3 at, ItemStack weapon) {
        var level = player.level();
        var magic = game.items().magic();
        if (restoring || !allowed.test(player) || magic.mechanismSuppressedAt(level, at)
                || magic.artifactSuppressedAt(level, at)) return null;
        return spawnDolphinAt(player, at, game.items().indirectKills().itemCause(player, false, false, weapon));
    }

    private net.minecraft.world.entity.Mob spawnDolphinAt(ServerPlayer player, Vec3 at, LbcIndirectKills.Owner cause) {
        var level=player.level();if(!level.hasChunkAt(BlockPos.containing(at)))return null;
        var dolphin=EntityTypes.DOLPHIN.create(level,EntitySpawnReason.COMMAND);if(dolphin==null)return null;
        dolphin.snapTo(at.x,at.y,at.z,player.getYRot(),player.getXRot());dolphin.setNoGravity(true);
        // The source pursuit owns its heading; swim/look goals and land flops must not overwrite it.
        dolphin.setNoAi(true);
        dolphin.getAttribute(Attributes.SCALE).setBaseValue(2);dolphin.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50);dolphin.setHealth(50);
        dolphin.setCustomName(Component.translatable("akyla_devid"));dolphin.entityTags().add("akyla_devid");dolphin.entityTags().add("lucky_mob");
        game.items().markCurrentItemEpoch(dolphin);actors.put(dolphin,new Actor(player.getUUID(),game.items().epoch(),0,player.getYRot(),null,cause));
        game.items().summons().track(player,dolphin);game.items().communism().subjugate(player,dolphin,true,true);game.items().timeStop().prepareFrozen(dolphin);
        if(!level.addFreshEntity(dolphin)){remove(dolphin);return null;}
        player.sendSystemMessage(Component.translatable("akyla_privet"));return dolphin;
    }
    private ServerPlayer controller(Entity dolphin,Actor actor){var owner=game.items().communism().owner(dolphin);return owner!=null?owner:game.server().getPlayerList().getPlayer(actor.owner);}
    private boolean eligible(ServerLevel level, Entity teamSource, LivingEntity target) {
        return target.level() == level && target.getType().builtInRegistryHolder().is(MOBS)
                && game.items().wands().validTarget(target) && !LbcTeams.allied(game, teamSource, target);
    }
    public LivingEntity nearest(Entity dolphin){
        LivingEntity result=null;double distance=40000;
        for(var target:((ServerLevel)dolphin.level()).getEntitiesOfClass(LivingEntity.class,dolphin.getBoundingBox().inflate(200),e->eligible((ServerLevel)dolphin.level(),dolphin,e))){double d=dolphin.distanceToSqr(target);if(d>=1&&d<=distance){result=target;distance=d;}}
        return result;
    }
    /** Source mob actions run before the native dolphin's own travel/flop tick. */
    public void nativeDolphinTick(Entity dolphin){
        if(restoring||dolphin.getType()!=EntityTypes.DOLPHIN||!(dolphin.level() instanceof ServerLevel level)
                ||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        var actor=actors.get(dolphin);if(actor==null)return;
        if(dolphin instanceof Mob mob&&!mob.isNoAi())mob.setNoAi(true);
        if(dolphin.isRemoved()||!game.items().currentEpoch(actor.epoch)){remove(dolphin);return;}
        if(game.stage()==GameController.Stage.ENDING)return;
        if(!game.playing()||!dolphin.isAlive()){remove(dolphin);return;}
        if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(dolphin)||dolphin.entityTags().contains("time_stopped"))return;
        callbacks++;try{actors.put(dolphin,actor.frame(actor.age+1,dolphin.getYRot()));dolphinTick(dolphin);}finally{callbacks--;}
    }
    public void dolphinTick(Entity dolphin){
        var actor=actors.get(dolphin);if(actor==null)return;var owner=controller(dolphin,actor);if(owner==null)return;
        // A datapack function retains its command source position after tp moves @s.
        // Its later block test and child function execute from this entry origin.
        var origin=dolphin.position();
        var target=nearest(dolphin);
        if(target!=null){
            // Original command builds a point .1 toward the enemy plus one current look vector,
            // then turns toward that point and flattens pitch before advancing exactly one block.
            var toward=target.getEyePosition().subtract(dolphin.position()).normalize();var heading=dolphin.getLookAngle().add(toward.scale(.1)).multiply(1,0,1).normalize();
            if(heading.lengthSqr()>0){float yaw=(float)Math.toDegrees(Math.atan2(-heading.x,heading.z));dolphin.snapTo(dolphin.getX()+heading.x,dolphin.getY(),dolphin.getZ()+heading.z,yaw,0);dolphin.setYHeadRot(yaw);dolphin.setYBodyRot(yaw);}
        }
        var level=(ServerLevel)dolphin.level();
        if (level.getBlockState(BlockPos.containing(origin)).is(SOLID)) {
            var cause = borrowedAttack(owner, actor, true);
            if (cause == null) cause = game.items().indirectKills().wildCause(owner);
            var source = new LbcSpellSource.Source(dolphin.getUUID(), owner.getUUID(), actor.epoch, cause, Set.of());
            game.items().tntDrops().spawnWildActor(level, origin, Vec3.ZERO, 2, source);
        }
        if(owner.getRandom().nextInt(10)<3)spawnCoin(dolphin,origin);
    }
    public Coin spawnCoin(Entity dolphin){return spawnCoin(dolphin,dolphin.position());}
    Coin spawnCoin(Entity dolphin,Vec3 origin){
        var parent=actors.get(dolphin);if(parent==null)return null;var owner=controller(dolphin,parent);if(owner==null)return null;
        var level=(ServerLevel)dolphin.level();if(!level.hasChunkAt(BlockPos.containing(origin)))return null;
        var coin=new Coin(TYPE,level);coin.setOwner(dolphin);coin.setPos(origin);coin.setNoGravity(true);
        coin.setDeltaMovement(new Vec3((owner.getRandom().nextInt(101)-50)*.01,0,(owner.getRandom().nextInt(101)-50)*.01));
        float yaw=(owner.getRandom().nextInt(3601)-1800)*.1f;
        for(var tag:List.of("bitcoin","mecha","slowraycast","player","physical_object"))coin.entityTags().add(tag);
        game.items().markCurrentItemEpoch(coin);
        var display = display(level);
        var cause = borrowedAttack(owner, parent, false);
        if (cause == null) cause = game.items().indirectKills().coldCause(owner, false);
        var state = new Actor(owner.getUUID(), game.items().epoch(), 0, yaw, display, cause);
        actors.put(coin, state);
        orient(coin, state);
        game.items().timeStop().prepareFrozen(coin);game.items().timeStop().prepareFrozen(display);
        if(!level.addFreshEntity(coin)||!level.addFreshEntity(display)){remove(coin);return null;}return coin;
    }
    private LbcIndirectKills.Owner borrowedAttack(ServerPlayer owner, Actor dolphin, boolean wild) {
        var source = dolphin.cause;
        return source != null && source.player().equals(owner.getUUID())
                ? game.items().indirectKills().itemCause(owner, wild, false, source.enchantmentWeapon()) : null;
    }

    public void ownerChanged(Entity entity, ServerPlayer reflector) {
        if (!(entity instanceof Coin coin)) return;
        var state = actors.get(coin);
        if (state == null) return;
        var cause = game.items().indirectKills().coldCause(reflector, false);
        actors.put(coin, new Actor(reflector.getUUID(), state.epoch, state.age, state.yaw, state.display, cause));
    }
    private Display.ItemDisplay display(ServerLevel level){
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);
        try{var preset=TagParser.parseCompoundFully("{item_display:'fixed',brightness:{block:15,sky:15},teleport_duration:1,interpolation_duration:1,transformation:{scale:[2f,2f,2f],translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},item:{id:'minecraft:carrot_on_a_stick',count:1,components:{'minecraft:item_model':'lbc:bitcoin'}}}");
            display.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),preset));
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
        display.setNoGravity(true);display.entityTags().add("skywars2.pozdman_display");game.items().markCurrentItemEpoch(display);return display;
    }
    private void orient(Coin coin,Actor state){
        var display=state.display;if(display==null||display.isRemoved())return;
        // bitcoin_tick spins the display's entity while legacy physical-object motion
        // only tilts its model through the left transformation quaternion.
        display.snapTo(coin.getX(),coin.getY(),coin.getZ(),state.yaw,state.age==0?0:90);
        var motion=coin.getDeltaMovement();
        if(state.age>0&&motion.lengthSqr()>1e-6){
            float yaw=(float)Math.atan2(-motion.x,motion.z);
            float pitch=(float)-Math.atan2(motion.y,Math.hypot(motion.x,motion.z));
            display.setTransformation(new Transformation(new Vector3f(),new Quaternionf().rotateY(-yaw).rotateX(pitch),
                    new Vector3f(2),new Quaternionf()));
        }
    }
    public boolean coinContact(Coin coin) {
        return coinContact(coin,coin.position(),coin.position(),coinTargets(coin,Vec3.ZERO),new HashSet<>());
    }
    private List<LivingEntity> coinTargets(Coin coin,Vec3 movement){
        var state = actors.get(coin);
        if(state==null)return List.of();
        var level = (ServerLevel)coin.level();
        var author = game.server().getPlayerList().getPlayer(state.owner);
        Entity teamSource = author == null ? coin : author;
        // Include both directions of an axis: a block can bounce the coin back during this tick.
        return level.getEntitiesOfClass(LivingEntity.class,coin.getBoundingBox().inflate(Math.abs(movement.x)+2,Math.abs(movement.y)+2,Math.abs(movement.z)+2),
                target -> !target.entityTags().contains("akyla_devid")
                        && eligible(level, teamSource, target));
    }
    private static boolean coinTouches(Entity target,Vec3 from,Vec3 to){
        double dx=to.x-from.x,dy=to.y-from.y,dz=to.z-from.z;
        double x=target.getX()-from.x,y=target.getY()-from.y,z=target.getZ()-from.z;
        double length=dx*dx+dy*dy+dz*dz;
        double t=length==0?0:Math.clamp((x*dx+y*dy+z*dz)/length,0,1);
        x-=dx*t;y-=dy*t;z-=dz*t;
        return x*x+y*y+z*z<=4;
    }
    private boolean coinContact(Coin coin,Vec3 from,Vec3 to,List<LivingEntity> candidates,Set<LivingEntity> attempted){
        var state=actors.get(coin);if(state==null)return false;
        var level=(ServerLevel)coin.level();var shooter=coin.getOwner();
        var author=game.server().getPlayerList().getPlayer(state.owner);Entity teamSource=author==null?coin:author;
        DamageSource damage=null;
        boolean success = false;
        for (var target : candidates) {
            if(!coinTouches(target,from,to)||!eligible(level,teamSource,target)||!attempted.add(target))continue;
            if(damage==null){
                var type=level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FALLING_BLOCK);
                damage=game.items().indirectKills().captureDamage(new DamageSource(type,coin,shooter==null?coin:shooter),state.cause);
            }
            if (target.hurtServer(level, damage, 15)) {
                success = true;
                LbcDamage.mobContactEnchantments(level, target, damage);
            }
        }
        if (success) remove(coin);
        return success;
    }
    public void coinTick(Coin coin){
        var state=actors.get(coin);if(state==null)return;
        if(state.display==null){remove(coin);return;}
        if(state.display.isRemoved()){
            // Display and body can cross neighboring chunk unload boundaries separately.
            // Keep the association dormant until the native display's load callback replaces it.
            if(state.display.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)return;
            remove(coin);return;
        }
        if(state.age>=100){remove(coin);return;}
        var candidates=coinTargets(coin,coin.getDeltaMovement().add(0,-.08,0));
        Set<LivingEntity> attempted=new HashSet<>();
        LbcPhysicalObjects.step(coin,1,1,.8,1,(from,to)->false,
                (from,to)->coinContact(coin,from,to,candidates,attempted));
        if(coin.isRemoved())return;
        state=state.frame(state.age+1,state.yaw+20);actors.put(coin,state);orient(coin,state);
        if(coinContact(coin,coin.position(),coin.position(),candidates,attempted))return;
        if(!coin.level().getBlockState(BlockPos.containing(coin.position().add(0,1,0))).is(AIRS))breakCoin(coin);
    }
    void breakCoin(Coin coin){LbcOverload.sound((ServerLevel)coin.level(),coin.position(),"minecraft:lbcsounds.bonk",.5f,1,0,0);remove(coin);}
    public void tick(){
        if(restoring||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        if(waiting.isEmpty()&&actors.isEmpty()){phases.clear();return;}
        var worlds=Collections.newSetFromMap(new IdentityHashMap<ServerLevel,Boolean>());
        for(var player:waiting.keySet())worlds.add(player.level());for(var entity:actors.keySet())worlds.add((ServerLevel)entity.level());
        phases.keySet().retainAll(worlds);
        for(var level:worlds)if(!game.items().timeStop().stopped(level)){
            int phase=(phases.getOrDefault(level,Math.floorMod(game.server().getTickCount()-1,20))+1)%20;phases.put(level,phase);
            if(phase==0)for(var player:List.copyOf(waiting.keySet()))if(player.level()==level){waiting.remove(player);player.entityTags().remove("pozdman_wait");}
        }
        for(var entity:List.copyOf(actors.keySet())){
            var actor=actors.get(entity);if(actor==null)continue;
            if(entity.isRemoved()||!game.items().currentEpoch(actor.epoch)){remove(entity);continue;}
            if(game.stage()==GameController.Stage.ENDING)continue;
            if(!game.playing()){remove(entity);continue;}
            if(game.items().timeStop().stopped((ServerLevel)entity.level())||game.items().timeStop().frozen(entity))continue;
            // Dolphin.tick HEAD owns its one source invocation. END owns only coin flight.
            callbacks++;try{if(entity instanceof Coin coin)coinTick(coin);}finally{callbacks--;}
        }
    }
    public void loaded(Entity entity){
        if(restoring)return;
        if(retiredDormant.remove(entity.getUUID())){entity.discard();return;}
        var state=dormant.remove(entity.getUUID());if(state!=null&&game.items().currentEpoch(state.epoch))actors.put(entity,state);
        if(entity instanceof Display.ItemDisplay display){
            for(var key:List.copyOf(actors.keySet())){var a=actors.get(key);if(a.display!=null&&a.display.getUUID().equals(display.getUUID()))actors.put(key,a.visual(display));}
            dormant.replaceAll((id,a)->a.display!=null&&a.display.getUUID().equals(display.getUUID())?a.visual(display):a);
        }
    }
    public void unloaded(Entity entity){
        if(restoring)return;
        if(keepsDuringCelebration(entity))return;
        var actor=actors.remove(entity);
        if(actor!=null&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)dormant.put(entity.getUUID(),actor);
        else if(actor!=null&&actor.display!=null&&!actor.display.isRemoved())actor.display.discard();
    }
    public void removed(Entity entity){if(restoring)return;if(actors.containsKey(entity))remove(entity);dormant.remove(entity.getUUID());}
    private void remove(Entity entity){var actor=actors.remove(entity);dormant.remove(entity.getUUID());if(actor!=null&&actor.display!=null){if(actor.display.isRemoved()&&actor.display.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)retiredDormant.add(actor.display.getUUID());actor.display.discard();}if(!entity.isRemoved())entity.discard();}
    private void retireDormant(Predicate<Actor> condition){
        for(var entry:List.copyOf(dormant.entrySet()))if(condition.test(entry.getValue())){retiredDormant.add(entry.getKey());if(entry.getValue().display!=null)retiredDormant.add(entry.getValue().display.getUUID());dormant.remove(entry.getKey());}
    }
    public boolean keepsDuringCelebration(Entity entity){
        if(game.stage()!=GameController.Stage.ENDING||entity.isRemoved())return false;
        var actor=actors.get(entity);
        return actor!=null&&game.items().currentEpoch(actor.epoch);
    }
    public void forget(ServerPlayer player){
        if(restoring)return;
        waiting.remove(player);player.entityTags().remove("pozdman_wait");
        if(game.stage()==GameController.Stage.ENDING)return;
        for(var entity:List.copyOf(actors.keySet()))if(actors.get(entity).owner.equals(player.getUUID()))remove(entity);
        retireDormant(a->a.owner.equals(player.getUUID()));
    }
    public void stop(){for(var entity:List.copyOf(actors.keySet()))remove(entity);for(var player:waiting.keySet())player.entityTags().remove("pozdman_wait");waiting.clear();retireDormant(a->true);phases.clear();restoring=false;}
    public int count(){return actors.size();}
    public boolean waiting(ServerPlayer player){return waiting.containsKey(player);}
    public Actor state(Entity entity){return actors.get(entity);}
    public List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    public void checkpointBoundary(){if(callbacks!=0)throw new IllegalStateException("Finish Pozdman native callbacks before checkpointing");}
    /** Native actor replay owns the existing bodies. Detach derived associations without discarding them. */
    public void prepareFrozen(){checkpointBoundary();restoring=true;actors=new IdentityHashMap<>();waiting=new IdentityHashMap<>();dormant.clear();retiredDormant.clear();phases.clear();}
    public void publishFrozen(){checkpointBoundary();restoring=false;}
    void cancelFrozen(){checkpointBoundary();restoring=false;}
    public Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    public final class Checkpoint {
        record Image(MatchDamageSources.Ref actor,UUID owner,String epoch,int age,float yaw,MatchDamageSources.Ref display,LbcIndirectKills.Owner cause){}
        private final List<Image> images=new ArrayList<>();
        private final MatchSpellCheckpoint.IdentityImage<ServerPlayer,Boolean> savedWait;
        private final Map<ServerLevel,Integer> clocks=new IdentityHashMap<>(phases);
        private final Map<UUID,Actor> asleep=new HashMap<>(dormant);
        private final Set<UUID> retired=new HashSet<>(retiredDormant);
        Checkpoint(MatchSpellCheckpoint.Links links){savedWait=new MatchSpellCheckpoint.IdentityImage<>(waiting,links);
            for(var entry:actors.entrySet()){var a=entry.getValue();images.add(new Image(links.key(entry.getKey()),a.owner,a.epoch,a.age,a.yaw,a.display==null?null:links.key(a.display),a.cause));}}
        public Runnable prepare(MatchSpellCheckpoint.Links links){checkpointBoundary();var restored=new IdentityHashMap<Entity,Actor>();
            for(var image:images){var entity=links.get(image.actor);var display=image.display==null?null:(Display.ItemDisplay)links.get(image.display);restored.put(entity,new Actor(image.owner,image.epoch,image.age,image.yaw,display,image.cause));}
            var w=savedWait.restore(links);
            return ()->{actors=restored;waiting=w;phases.clear();phases.putAll(clocks);dormant.clear();dormant.putAll(asleep);retiredDormant.clear();retiredDormant.addAll(retired);};}
    }
    public boolean cloneRecognizes(Entity entity){return actors.containsKey(entity);}
    public LbcCloneSkills.ActorImage cloneImage(Entity source){
        var state=actors.get(source);if(state==null)return null;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var visual=clone instanceof Coin?display((ServerLevel)clone.level()):null;
                var next=state.visual(visual);actors.put(clone,next);
                if(clone instanceof Coin coin){
                    orient(coin,next);
                    if(state.display!=null){
                        var registries=visual.registryAccess();
                        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,registries);
                        state.display.saveWithoutId(saved);
                        TagValueInput.create(ProblemReporter.DISCARDING,registries,saved.buildResult())
                                .read("transformation",Transformation.EXTENDED_CODEC).ifPresent(visual::setTransformation);
                    }
                    game.items().timeStop().prepareFrozen(visual);
                    if(!clone.level().addFreshEntity(visual))throw new IllegalStateException("Bitcoin clone visual publication");
                }
            }
            public void rollback(Entity clone){remove(clone);}
        };
    }
}
