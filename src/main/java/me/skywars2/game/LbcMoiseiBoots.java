package me.skywars2.game;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Source shared sprint clock and full Moisei equipped path, speed ladder and native TNT continuation. */
public final class LbcMoiseiBoots {
    private static final String ACTOR="skywars2.moisei_tnt";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final Identifier SPEED_SPRINT=Identifier.parse("minecraft:c1cda724-0ea2-460a-98b8-9631eafd2515");
    private static final Identifier DAMAGE_SPRINT=Identifier.parse("minecraft:585f98b8-a8b8-4e4c-838a-f2e124831040");
    private static final double[] SAMPLES={0,.25,.5,.75,1,1.5,2,3};
    private final GameController game;
    private final Map<Entity,UUID> actors=new IdentityHashMap<>();
    private final Map<UUID,UUID> unloaded=new HashMap<>();
    private long spawned;
    public LbcMoiseiBoots(GameController game){this.game=game;}
    public static boolean equipped(ServerPlayer owner){var stack=owner.getItemBySlot(EquipmentSlot.FEET);
        return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("moisei",0)==1;}
    private boolean admitted(ServerPlayer owner){
        if(owner==null||!owner.isAlive()||owner.isRemoved()||owner.level()!=game.maps().level()||!game.canUseGameplayItems(owner)
                ||game.isObserver(owner)||game.stage()==GameController.Stage.ENDING||!game.items().wands().validRoleTarget(owner,false)
                ||game.items().wands().inputTimeStopped(owner)||game.items().timeStop().stopped(owner.level()))return false;
        var dimension=owner.level().dimension().identifier();
        return !dimension.equals(Identifier.parse("minecraft:nexus"))&&!dimension.equals(Identifier.parse("minecraft:imprinted"));
    }
    /** Active-input stun/ice do not stop a carried armor passive; native sprint flag is authoritative. */
    public void tick(){
        if(game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        actors.keySet().removeIf(Entity::isRemoved);
        for(var owner:game.server().getPlayerList().getPlayers()){
            if(!admitted(owner))continue;
            var timer=game.items().mana().score(owner,"sprint_timer");
            if(!owner.isSprinting()){
                if(timer.get()>=1){timer.set(0);removeSprintModifiers(owner);owner.entityTags().remove("sprinted");}
                continue;
            }
            // This SECRET clock is intentionally independent of equipment and local magic suppression.
            timer.add(1);
            if(!equipped(owner)||game.items().magic().beaconSuppressed(owner.position()))continue;
            int elapsed=timer.get();
            if(elapsed>=1)owner.addEffect(ItemEffectLevels.create(MobEffects.SPEED,20,speed(elapsed)),owner);
            if(elapsed>=651)explode(owner);
            path(owner,elapsed);
        }
    }
    private static int speed(int elapsed){
        if(elapsed<=400)return (elapsed-1)/40;
        if(elapsed<=450)return 10;if(elapsed<=500)return 20;if(elapsed<=550)return 30;if(elapsed<=600)return 40;
        // Approved continuation keeps the final source50 layer refreshed after650 as TNT continues every tick.
        return 50;
    }
    private static void removeSprintModifiers(ServerPlayer owner){
        var speed=owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(SPEED_SPRINT);
        var damage=owner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);if(damage!=null)damage.removeModifier(DAMAGE_SPRINT);
    }
    private void path(ServerPlayer owner,int elapsed){
        var level=owner.level();var origin=owner.position().add(0,-1,0);var forward=Vec3.directionFromRotation(0,owner.getYRot());
        game.items().indirectKills().trapChanges(level,owner,false,()->{
            for(double sample:SAMPLES)fill(level,origin.add(forward.scale(sample)),origin.add(forward.scale(sample)));
            if(elapsed>=100)segment(level,origin,forward,3.5,4);
            if(elapsed>=200)segment(level,origin,forward,4,5);
            if(elapsed>=300){segment(level,origin,forward,5,6);segment(level,origin,forward,6,7);}
            if(elapsed>=400)segment(level,origin,forward,7,8);
            if(elapsed>=500){segment(level,origin,forward,8,9);segment(level,origin,forward,9,10);}
            if(elapsed>=600)segment(level,origin,forward,10,11);
        });
    }
    private static void segment(ServerLevel level,Vec3 origin,Vec3 direction,double first,double last){fill(level,origin.add(direction.scale(first)),origin.add(direction.scale(last)));}
    private static boolean resident(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getWorldBorder().isWithinBounds(pos)
            &&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    /** Each original STRICT fill is independent: reject an unavailable segment without loading or delaying it. */
    private static void fill(ServerLevel level,Vec3 first,Vec3 last){
        var a=BlockPos.containing(first);var b=BlockPos.containing(last);
        int minX=Math.min(a.getX(),b.getX()),maxX=Math.max(a.getX(),b.getX()),minY=Math.min(a.getY(),b.getY()),maxY=Math.max(a.getY(),b.getY()),minZ=Math.min(a.getZ(),b.getZ()),maxZ=Math.max(a.getZ(),b.getZ());
        long volume=(long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);
        if(volume>level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS)||level.isDebug())return;
        for(var pos:BlockPos.betweenClosed(minX,minY,minZ,maxX,maxY,maxZ))if(!resident(level,pos))return;
        var input=new BlockInput(Blocks.DIRT.defaultBlockState(),Set.of(),null);
        for(var pos:BlockPos.betweenClosed(minX,minY,minZ,maxX,maxY,maxZ))if(level.getBlockState(pos).is(AIRS))input.place(level,pos,818);
    }
    private void explode(ServerPlayer owner){
        var position=owner.position().subtract(owner.getLookAngle().scale(5));if(!resident(owner.level(),BlockPos.containing(position)))return;
        // Ten independent ordinary native explosions per admitted sprint tick: no queue, merge or throttle.
        for(int i=0;i<10;i++){
            var tnt=new PrimedTnt(owner.level(),position.x,position.y,position.z,owner);tnt.setFuse(0);tnt.setDeltaMovement(Vec3.ZERO);
            tnt.entityTags().add(ACTOR);game.items().markCurrentItemEpoch(tnt);actors.put(tnt,owner.getUUID());game.items().timeStop().prepareFrozen(tnt);
            if(owner.level().addFreshEntity(tnt))spawned++;else{actors.remove(tnt);tnt.discard();}
        }
    }
    public static boolean managed(Entity actor){return actor!=null&&actor.entityTags().contains(ACTOR);}
    public ServerPlayer owner(Entity actor){
        if(!actors.containsKey(actor))return null;
        var reflected=game.items().electro().reflectedOwner(actor);if(reflected instanceof ServerPlayer player)return player;
        if(actor instanceof PrimedTnt tnt&&tnt.getOwner() instanceof ServerPlayer player)return player;
        return game.server().getPlayerList().getPlayer(actors.get(actor));
    }
    public boolean canAffect(Entity source,Entity target){
        if(!managed(source))return true;var owner=owner(source);
        return owner!=null&&(target==owner?!LbcGreed.active(target)&&game.items().wands().validRoleTarget(target,false)
                :game.items().wands().projectileTarget(owner,target));
    }
    public boolean explosionTarget(Entity source,Entity target){return canAffect(source,target);}
    public void inheritChild(Entity source,Entity child){
        if(!managed(source)||!actors.containsKey(source)||!(child instanceof PrimedTnt))return;
        var owner=owner(source);
        if(owner==null||!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(source)){child.discard();return;}
        child.entityTags().add(ACTOR);game.items().markCurrentItemEpoch(child);actors.put(child,owner.getUUID());game.items().timeStop().prepareFrozen(child);
    }
    public void loaded(Entity actor){
        if(!managed(actor))return;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(actor)||actor.level()!=game.maps().level()){actor.discard();return;}
        if(!actors.containsKey(actor)){var author=unloaded.remove(actor.getUUID());if(author==null){actor.discard();return;}actors.put(actor,author);}
        game.items().timeStop().prepareFrozen(actor);
    }
    public void unloaded(Entity actor){if(managed(actor)&&actor.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK){var author=actors.remove(actor);if(author!=null)unloaded.put(actor.getUUID(),author);}}
    /** Source purify explicitly zeros this shared clock; ordinary gear removal does not. */
    public void clear(ServerPlayer owner){game.items().mana().score(owner,"sprint_timer").set(0);}
    public void forget(ServerPlayer owner){
        for(var actor:List.copyOf(actors.keySet()))if(owner.getUUID().equals(actors.get(actor))){actors.remove(actor);actor.discard();}
        unloaded.values().removeIf(owner.getUUID()::equals);clear(owner);
    }
    public void stop(){for(var actor:List.copyOf(actors.keySet()))actor.discard();actors.clear();unloaded.clear();}
    List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    long spawned(){return spawned;}
}
