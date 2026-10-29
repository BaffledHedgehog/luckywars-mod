package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Original Oreshek consumer, independent return latch and real shared native rare-event producers. */
public final class LbcOreshek {
    private static final String LATCH="skywars2.oreshek.returned",ACTOR="skywars2.oreshek.actor";
    private static final String AUTHOR="skywars2.oreshek.author.",ORDER="skywars2.oreshek.order.";
    private static final CompoundTag PIGLIN=parse("""
{equipment:{head:{"id":"netherite_helmet","count":1,"components":{"equippable":{"slot":head,"asset_id":"lbc:zelensky"}}},chest:{"id":"netherite_chestplate","count":1,"components":{"equippable":{"slot":chest,"asset_id":"lbc:zelensky"}}},legs:{"id":"netherite_leggings","count":1,"components":{"equippable":{"slot":legs,"asset_id":"lbc:zelensky"}}},feet:{"id":"netherite_boots","count":1,"components":{"equippable":{"slot":feet,"asset_id":"lbc:zelensky"}}}},drop_chances:{head:0,chest:0,legs:0,feet:0},CustomName:[{"text":"Володiмiр","color":"aqua"},{"text":" Zelensky","color":"yellow"}],CustomNameVisible:true}
            """);
    private final GameController game;
    private final Set<Entity> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private ServerLevel executingLevel;private LbcIndirectKills.Owner executingCause;private Entity executingSource;
    private long consumed,explosions,transfers,zelenskys;private Outcome last=Outcome.NONE;
    public enum Outcome { NONE, EXPLOSION, NEXUS, ZELENSKY }
    public LbcOreshek(GameController game){this.game=game;}
    private static CompoundTag parse(String value){try{return TagParser.parseCompoundFully(value);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("eat_oreshek",0)==1;}
    private boolean admitted(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&LbcDimensionItems.ordinary(p)
            &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)
            &&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(p)&&!game.items().timeStop().stopped(p.level());}
    /** Native consume owns the only item debit. A completed early-return root blocks later nut scripts until reset. */
    public void consumed(ServerPlayer p,ItemStack stack){
        if(!matches(stack)||!admitted(p)||pendingDeathMessage(p))return;
        // Source consume_item completes its hidden advancement BEFORE its reward runs, including a potentially lethal debuff.
        p.entityTags().add(LATCH);consumed++;last=Outcome.NONE;
        var level=p.level();var origin=p.position();var accepted=game.items().indirectKills().wildCause(p);
        game.items().legacyEffects().randomDebuff(p);
        if(level.getRandom().nextFloat()<.1f){last=Outcome.EXPLOSION;explode(p,level,origin,accepted);return;}
        LbcOverload.sound(level,origin,"minecraft:lbcsounds.ukraine",8,1,0,0);
        if(level.getRandom().nextFloat()<.01f){last=Outcome.NEXUS;transfers++;game.items().hedgehogDimensions().nexus(p,p,level.getRandom());return;}
        if(level.getRandom().nextFloat()<.0001f){last=Outcome.ZELENSKY;zelenskys++;zelensky(p,level,origin,accepted);return;}
        // Only the ordinary fallthrough revokes source eat_oreshek. Early paths keep its return/death marker.
        p.entityTags().remove(LATCH);
    }
    private void explode(ServerPlayer p,ServerLevel level,Vec3 origin,LbcIndirectKills.Owner accepted){
        var previousLevel=executingLevel;var previousCause=executingCause;var previousSource=executingSource;
        final PrimedTnt[] direct={null};executingLevel=level;executingCause=accepted;
        try{game.items().exasperato().encounterBlast(p,level,origin,15,tnt->{direct[0]=tnt;stamp(tnt,accepted);executingSource=tnt;});explosions++;}
        finally{executingLevel=previousLevel;executingCause=previousCause;executingSource=previousSource;if(direct[0]!=null)direct[0].discard();}
        LbcOverload.sound(level,origin,"minecraft:lbcsounds.donbass",4,1,0,0);
    }
    private void zelensky(ServerPlayer p,ServerLevel level,Vec3 origin,LbcIndirectKills.Owner accepted){
        game.items().nukeEvent().start(p,origin,true);
        var payload=PIGLIN.copy();payload.putString("id","minecraft:piglin");
        var piglin=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),payload),level,EntitySpawnReason.COMMAND,
                entity->{entity.snapTo(origin.x,origin.y,origin.z,0,0);stamp(entity,accepted);return entity;});
        if(piglin!=null&&!level.tryAddFreshEntityWithPassengers(piglin))piglin.getSelfAndPassengers().forEach(entity->{tracked.remove(entity);entity.discard();});
        // Shared bomber's native spawn formula adds90 degrees; compensate to retain source relative45/-45/135/-135.
        for(int relative:new int[]{45,-45,135,-135})game.items().droneDrops().spawnBomber(p,origin,p.getYRot()+relative-90);
    }
    private void stamp(Entity entity,LbcIndirectKills.Owner accepted){
        entity.entityTags().removeIf(t->t.startsWith(AUTHOR)||t.startsWith(ORDER));
        entity.entityTags().addAll(List.of(ACTOR,AUTHOR+accepted.player(),ORDER+accepted.order()));game.items().markCurrentItemEpoch(entity);
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(ACTOR);}
    public static boolean wild(Entity entity){return managed(entity);}
    /** Immutable lineage on the actual entity also works after its native unpublished TNT source was discarded. */
    public LbcIndirectKills.Owner cause(Entity entity){
        if(!managed(entity))return null;UUID owner=null;long order=0;
        for(var tag:entity.entityTags())try{if(tag.startsWith(AUTHOR))owner=UUID.fromString(tag.substring(AUTHOR.length()));
            else if(tag.startsWith(ORDER))order=Long.parseLong(tag.substring(ORDER.length()));}catch(IllegalArgumentException ignored){}
        return owner==null||order<=0?null:new LbcIndirectKills.Owner(owner,order,true,false);
    }
    public ServerPlayer owner(Entity source){var accepted=cause(source);return accepted==null?null:game.server().getPlayerList().getPlayer(accepted.player());}
    public boolean canAffect(Entity source,Entity target){return target!=null&&cause(source)!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.items().currentEpoch(source)&&game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target);}
    public boolean canTarget(Entity source,Entity target){return canAffect(source,target);}
    public void inherit(Entity parent,Entity child){var accepted=cause(parent);if(accepted!=null&&game.items().currentEpoch(parent))stamp(child,accepted);}
    public void inheritChild(Entity child){if(executingCause!=null&&child.level()==executingLevel)stamp(child,executingCause);}
    public boolean executing(ServerLevel level){return executingCause!=null&&executingLevel==level;}
    public Entity currentEmitter(){return executingSource;}
    public void loaded(Entity entity){
        if(!managed(entity))return;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(entity)||cause(entity)==null){entity.discard();return;}
        tracked.add(entity);game.items().timeStop().prepareFrozen(entity);
    }
    public void unloaded(Entity entity){tracked.remove(entity);}
    /** Physical accepted children follow their own native lifetime and do not vanish when their author forgets an item. */
    public void forget(ServerPlayer player){}
    public void tick(){for(var entity:List.copyOf(tracked))if(entity.isRemoved())tracked.remove(entity);
        else if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(entity)){tracked.remove(entity);entity.discard();}}
    public void stop(){for(var entity:List.copyOf(tracked))entity.discard();tracked.clear();}
    public boolean pendingDeathMessage(ServerPlayer p){return p!=null&&p.entityTags().contains(LATCH);}
    /** Source leave_death is also called by active leave and explicit match/player resets. Idempotent across duplicate boundaries. */
    public boolean died(ServerPlayer p){if(!pendingDeathMessage(p))return false;
        for(var viewer:game.server().getPlayerList().getPlayers())viewer.sendSystemMessage(Component.literal("\uE202"));
        p.entityTags().remove(LATCH);return true;
    }
    public boolean resetBoundary(ServerPlayer p){return died(p);}
    public List<Entity> snapshot(){return List.copyOf(tracked);}
    public long consumedCount(){return consumed;}public long explosionCount(){return explosions;}public long transferCount(){return transfers;}
    public long zelenskyCount(){return zelenskys;}public Outcome lastOutcome(){return last;}
    /** Exact source payload oracle for native resource/component verification, independent of added epoch/author tags. */
    public static CompoundTag piglinPayload(){return PIGLIN.copy();}
}
