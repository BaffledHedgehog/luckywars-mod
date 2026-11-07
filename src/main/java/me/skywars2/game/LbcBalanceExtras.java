package me.skywars2.game;

import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** The four additional old-pool leaves have concrete event intake, native bodies and source geometry. */
final class LbcBalanceExtras {
    private final GameController game;
    private record Trail(boolean most,DyeColor color,LbcIndirectKills.Owner cause){}
    private static final List<DyeColor> SOURCE_COLORS=Arrays.stream(DyeColor.values())
            .sorted(Comparator.comparing(color->color.name().toLowerCase(Locale.ROOT))).toList();
    private static final AttachmentType<CompoundTag> TRAIL_CAUSE=AttachmentRegistry.create(
            Identifier.parse("skywars2:wool_trail_cause"),builder->builder.persistent(CompoundTag.CODEC));
    private final Map<Snowball,Trail> bridges=new IdentityHashMap<>();
    private LbcBalance.WoolPolicy woolPolicy=new LbcBalance.WoolPolicy(){};
    private final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Entity> luckyMarkers=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerLevel,Integer> phases=new IdentityHashMap<>();
    LbcBalanceExtras(GameController game){this.game=game;}
    static void register(){}
    void woolPolicy(LbcBalance.WoolPolicy policy){woolPolicy=Objects.requireNonNull(policy);}
    boolean woolAllowed(ServerPlayer owner,InteractionHand hand){return woolPolicy.allowsLaunch(owner,hand);}
    List<Snowball> trailActors(){return List.copyOf(bridges.keySet());}
    DyeColor trailColor(Entity actor){var state=bridges.get(actor);return state==null?null:state.color;}
    LbcIndirectKills.Owner trailCause(Entity actor){var state=bridges.get(actor);return state==null?null:state.cause;}
    static boolean isDrop(ItemStack stack){return LbcBalanceItems.flag(stack,"drop")&&LbcBalanceItems.flag(stack,"luckyspawner_old");}
    void damaged(ServerPlayer owner){
        if(!game.playing()||!game.canUseGameplayItems(owner)||game.isObserver(owner)||!game.items().wands().validRoleTarget(owner,true)
                ||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)||game.items().timeStop().stopped(owner.level())
                ||!LbcBalanceItems.flag(owner.getItemBySlot(EquipmentSlot.CHEST),"dynamite")||game.items().magic().mechanismSuppressedAt(owner.level(),owner.position()))return;
        var body=new PrimedTnt(owner.level(),owner.getX(),owner.getY()+.2,owner.getZ(),owner);body.setFuse(80);insert(owner,body);
    }
    /** Native BaseSpawner remains the sole timing/position/population owner. */
    boolean canDrop(ItemEntity token){return isDrop(token.getItem())&&token.level() instanceof ServerLevel level&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&level==game.maps().level()&&!game.items().timeStop().stopped(level)&&level.hasChunkAt(token.blockPosition());}
    boolean drop(ItemEntity token){
        if(!canDrop(token))return false;var level=(ServerLevel)token.level();
        try{
            var tag=TagParser.parseCompoundFully("{SpawnCount:1s,SpawnRange:3s,Delay:0s,MinSpawnDelay:100s,MaxSpawnDelay:101s,MaxNearbyEntities:1000s,RequiredPlayerRange:100s,SpawnData:{entity:{id:'minecraft:marker',Tags:['futureluckyblock','rc_1sec','skywars2.balance.lucky_marker']}}}");
            var cause=game.items().specialDrops().owner(token);var author=LbcIndirectKills.playerSource(cause);var pos=token.blockPosition();
            game.items().indirectKills().worldChanges(level,cause,true,()->new BlockInput(Blocks.SPAWNER.defaultBlockState(),Set.of(),tag).place(level,pos,258));
            var id=author==null?new UUID(0,0):author.getUUID();var authority=author==null?new LbcIndirectKills.Owner(id,0,true,false):game.items().indirectKills().wildCause(author);
            game.items().potionSpawnerDrops().admitSourceSpawner(level,pos,new LbcSpellSource.Source(UUID.randomUUID(),id,game.items().epoch(),authority,Set.of(),true));
            return true;
        }catch(Exception error){throw new IllegalStateException("Invalid fixed old lucky spawner",error);}
    }
    List<Projectile> augment(ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        boolean doubled=LbcBalanceItems.flag(weapon,"double_old"),shulker=LbcBalanceItems.flag(weapon,"shulker")&&!game.items().magic().suppressed(owner);
        if(!doubled&&!shulker)return family;var result=new ArrayList<Projectile>();
        for(var projectile:family){if(!(projectile instanceof AbstractArrow arrow)||arrow.isRemoved()||arrow.getOwner()!=owner){result.add(projectile);continue;}
            if(doubled){var left=Vec3.directionFromRotation(0,owner.getYRot()-90);for(int side:new int[]{-1,1}){
                var duplicate=arrow.getType().create(owner.level(),EntitySpawnReason.TRIGGERED);if(!(duplicate instanceof AbstractArrow copy))continue;
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,owner.registryAccess());arrow.saveWithoutId(output);var tag=output.buildResult();tag.remove("UUID");
                copy.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),tag));copy.setOwner(owner);copy.setPos(owner.getEyePosition().add(left.scale(side)).add(owner.getLookAngle().scale(.3)));copy.setDeltaMovement(game.items().timeStop().persistedVelocity(arrow,arrow.getDeltaMovement()));copy.setNoGravity(game.items().timeStop().persistedNoGravity(arrow,arrow.isNoGravity()));copy.setYRot(arrow.getYRot());copy.setXRot(arrow.getXRot());
                LbcTmtAmmoPayloads.copyPayloads(arrow,copy);copy.entityTags().add("doubel");copy.entityTags().add("active");insert(owner,copy);result.add(copy);
            }arrow.discard();}
            else result.add(arrow);
        }
        if(shulker){var accepted=weapon.copy();var data=accepted.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();data.putInt("shulker_new",1);accepted.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));return game.items().shulkerBow().augment(owner,accepted,result);}
        return result;
    }
    private void insert(ServerPlayer owner,Entity body){game.items().summons().track(owner,body);game.items().timeStop().prepareFrozen(body);actors.add(body);if(!owner.level().addFreshEntity(body)){actors.remove(body);body.discard();}}
    void loaded(Entity body){
        if(body instanceof Snowball snowball&&!bridges.containsKey(snowball)){
            var data=LbcFartus.data(snowball.getItem());
            if(data.getIntOr("projegadget",0)==1){
                boolean most=data.getIntOr("most",0)==1;DyeColor color=null;
                if(data.getIntOr("wool",0)==1)for(var candidate:SOURCE_COLORS)
                    if(data.getIntOr(candidate.name().toLowerCase(Locale.ROOT),0)==1){color=candidate;break;}
                if(most||color!=null){
                    LbcIndirectKills.Owner cause=null;var saved=snowball.getAttached(TRAIL_CAUSE);
                    if(saved!=null){try{cause=game.items().indirectKills().restoreColdCause(UUID.fromString(saved.getStringOr("author","")),saved.getLongOr("order",0),true);}catch(IllegalArgumentException invalid){return;}}
                    else if(snowball.getOwner() instanceof ServerPlayer owner){
                        cause=game.items().indirectKills().coldCause(owner,true);var tag=new CompoundTag();
                        tag.putString("author",cause.player().toString());tag.putLong("order",cause.order());snowball.setAttached(TRAIL_CAUSE,tag);
                    }
                    if(!game.items().currentEpoch(body)&&game.items().hasStaleItemEpoch(body))return;
                    game.items().markCurrentItemEpoch(body);bridges.put(snowball,new Trail(most,color,cause));
                }
            }
        }
        if(body.entityTags().contains("skywars2.balance.lucky_marker"))luckyMarkers.add(body);
    }
    void unloaded(Entity body){bridges.remove(body);luckyMarkers.remove(body);}
    void tick(){
        actors.removeIf(Entity::isRemoved);
        for(var ball:List.copyOf(bridges.keySet())){
            if(ball.isRemoved()||!game.items().currentEpoch(ball)){bridges.remove(ball);continue;}
            var level=(ServerLevel)ball.level();if(!game.playing()||game.stage()==GameController.Stage.ENDING||game.maps().busy()
                    ||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen()
                    ||level!=game.maps().level()||!LbcDimensionItems.ordinary(level)||!LbcBedwarsRoleGuards.publication(level)
                    ||game.items().timeStop().stopped(level)||game.items().timeStop().frozen(ball)||ball.entityTags().contains("time_stopped"))continue;
            var trail=bridges.get(ball);
            // Source runs Most first; later color fills can only occupy cells which are still actual AIR.
            var color=trail.most&&!game.items().magic().mechanismSuppressedAt(level,ball.position(),20)?DyeColor.LIME
                    :trail.color!=null&&woolPolicy.allowsField(level,ball.position())?trail.color:null;
            if(color==null)continue;
            var min=BlockPos.containing(ball.position().add(-.5,-2.5,-.5));var max=BlockPos.containing(ball.position().add(.5,-2.5,.5));
            for(var pos:BlockPos.betweenClosed(min,max))if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(Blocks.AIR))
                game.items().writeOwnedTerrain(level,pos.immutable(),Blocks.WOOL.pick(color).defaultBlockState(),trail.cause);
        }
        var worlds=Collections.newSetFromMap(new IdentityHashMap<ServerLevel,Boolean>());
        for(var marker:List.copyOf(luckyMarkers)){
            if(marker.isRemoved()||!game.items().currentEpoch(marker)){luckyMarkers.remove(marker);continue;}
            var level=(ServerLevel)marker.level();if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(marker))continue;worlds.add(level);
            if(phases.getOrDefault(level,0)%20!=0)continue;
            var pos=BlockPos.containing(marker.position().add(0,.5,0));if(!level.hasChunkAt(pos))continue;
            level.sendParticles(new DustParticleOptions(0xffff00,2),marker.getX(),marker.getY()+.5,marker.getZ(),10,.5,.5,.5,0);
            level.setBlock(pos,LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.NORMAL).defaultBlockState(),3);marker.discard();luckyMarkers.remove(marker);
        }
        for(var level:worlds)phases.put(level,phases.getOrDefault(level,0)+1);
    }
    void stop(){var bodies=new ArrayList<Entity>(actors);bodies.addAll(bridges.keySet());bodies.addAll(luckyMarkers);actors.clear();bridges.clear();luckyMarkers.clear();phases.clear();bodies.forEach(Entity::discard);}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        var savedActors=actors.stream().map(links::key).toList();var savedBridges=new LinkedHashMap<MatchDamageSources.Ref,Trail>();bridges.forEach((ball,trail)->savedBridges.put(links.key(ball),trail));var savedMarkers=luckyMarkers.stream().map(links::key).toList();var savedPhases=new IdentityHashMap<>(phases);
        return current->{var bodies=savedActors.stream().map(current::get).toList();var balls=new IdentityHashMap<Snowball,Trail>();savedBridges.forEach((ref,trail)->balls.put((Snowball)current.get(ref),trail));var markers=savedMarkers.stream().map(current::get).toList();return ()->{actors.clear();actors.addAll(bodies);bridges.clear();bridges.putAll(balls);luckyMarkers.clear();luckyMarkers.addAll(markers);phases.clear();phases.putAll(savedPhases);};};
    }
}
