package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Native finger actors and the source reward gear; shared Witch claim remains a separate edge. */
public final class LbcIndolenceFingers {
    private static final String MANAGED="skywars2.witch.indolence_finger";
    private static final String FINGER="{PersistenceRequired:1b,DeathLootTable:\"empty\",Tags:[\"indolence_finger\"],Silent:1b,equipment:{mainhand:{id:\"minecraft:bow\",count:1},feet:{id:\"minecraft:leather_boots\",components:{\"minecraft:unbreakable\":{},\"minecraft:equippable\":{\"slot\":\"feet\",\"asset_id\":\"lbc:finger\"}},count:1},legs:{id:\"minecraft:leather_leggings\",components:{\"minecraft:unbreakable\":{},\"minecraft:equippable\":{\"slot\":\"legs\",\"asset_id\":\"lbc:finger\"}},count:1},chest:{id:\"minecraft:leather_chestplate\",components:{\"minecraft:unbreakable\":{},\"minecraft:equippable\":{\"slot\":\"chest\",\"asset_id\":\"lbc:void\"}},count:1},head:{id:\"minecraft:leather_helmet\",components:{\"minecraft:unbreakable\":{},\"minecraft:equippable\":{\"slot\":\"head\",\"asset_id\":\"lbc:void\"}},count:1}},drop_chances:{head:0.0f,chest:0.0f,legs:0.0f,feet:0.0f,mainhand:0.0f,offhand:0.0f}}";
    private static final Map<EquipmentSlot,String> ARMOR=Map.of(
            EquipmentSlot.HEAD,"minecraft:leather_helmet[minecraft:equippable={slot:\"head\",asset_id:\"lbc:finger\"},minecraft:attribute_modifiers=[{type:\"minecraft:armor\",slot:\"head\",id:\"cumeiruhweqw\",amount:5,operation:\"add_value\"},{type:\"minecraft:movement_speed\",slot:\"head\",id:\"932489wuefiouberf\",amount:0.25,operation:\"add_multiplied_base\"}],tooltip_style=\"lbc:tier_c\"]",
            EquipmentSlot.CHEST,"minecraft:leather_chestplate[minecraft:equippable={slot:\"chest\",asset_id:\"lbc:assasin\"},minecraft:attribute_modifiers=[{type:\"minecraft:armor\",slot:\"chest\",id:\"cumindaerisneiwqd783yr7\",amount:5,operation:\"add_value\"},{type:\"minecraft:movement_speed\",slot:\"chest\",id:\"ferwdfgrreg\",amount:0.25,operation:\"add_multiplied_base\"}],tooltip_style=\"lbc:tier_c\"]",
            EquipmentSlot.LEGS,"minecraft:leather_leggings[minecraft:equippable={slot:\"legs\",asset_id:\"lbc:assasin\"},minecraft:attribute_modifiers=[{type:\"minecraft:armor\",slot:\"legs\",id:\"ffdopldfpoopd\",amount:5,operation:\"add_value\"},{type:\"minecraft:movement_speed\",slot:\"legs\",id:\"giuaosidoaodperfer\",amount:0.25,operation:\"add_multiplied_base\"}],tooltip_style=\"lbc:tier_c\"]",
            EquipmentSlot.FEET,"minecraft:leather_boots[minecraft:equippable={slot:\"feet\",asset_id:\"lbc:assasin\"},minecraft:attribute_modifiers=[{type:\"minecraft:armor\",slot:\"feet\",id:\"jweiuf78qwqqqecvf\",amount:5,operation:\"add_value\"},{type:\"minecraft:movement_speed\",slot:\"feet\",id:\"fflpor3iuwd78\",amount:0.25,operation:\"add_multiplied_base\"}],tooltip_style=\"lbc:tier_c\"]");
    private final GameController game;
    private IdentityHashMap<Entity,LbcSpellSource.Source> actors=new IdentityHashMap<>();
    private final Map<UUID,LbcSpellSource.Source> unloaded=new HashMap<>();
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    private record Rebirth(String epoch,List<net.minecraft.world.effect.MobEffectInstance> effects,Object owned,CompoundTag shadow,
            float absorption,Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,LbcIndirectKills.Owner> causes){}
    private static final class Cost {final UUID finger,player;final ServerLevel level;final int born;int left=5;
        Cost(Entity finger,ServerPlayer player,int born){this.finger=finger.getUUID();this.player=player.getUUID();level=(ServerLevel)finger.level();this.born=born;}
        Cost(UUID finger,UUID player,ServerLevel level,int born,int left){this.finger=finger;this.player=player;this.level=level;this.born=born;this.left=left;}}
    private final Map<UUID,Rebirth> rebirths=new HashMap<>();
    private final Set<UUID> ready=new HashSet<>(),consumed=new HashSet<>();
    private final List<Cost> costs=new ArrayList<>();
    private boolean advancing;
    private int lastTick=Integer.MIN_VALUE;
    LbcIndolenceFingers(GameController game){this.game=game;}
    private boolean live(){return game.playing()&&game.stage()!=GameController.Stage.ENDING;}
    private List<? extends Entity> select(ServerLevel level,Vec3 at,String literal){try{
        var selector=selectors.get(literal);if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();selectors.put(literal,selector);}
        return List.copyOf(selector.findEntities(game.server().createCommandSourceStack().withLevel(level).withPosition(at).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput()));
    }catch(CommandSyntaxException error){throw new IllegalStateException("Indolence actor selector "+literal,error);}}
    private static boolean resident(ServerLevel level,Vec3 at){var pos=BlockPos.containing(at);return level.getWorldBorder().isWithinBounds(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private boolean holder(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&!game.isObserver(p)&&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p)&&p.entityTags().contains("sin_indolence");}
    /** Called by the successful Indolence claim after its shared authority/debit step. */
    List<Skeleton> acquired(ServerPlayer owner){
        if(!live()||!holder(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner))return List.of();
        // The source selector is global, unbounded, and includes markers/items/displays too.
        var positions=select(owner.level(),owner.position(),"@e[type=!minecraft:player,sort=random,limit=20]");
        var created=new ArrayList<Skeleton>();
        for(var target:positions){var mob=spawn(owner,(ServerLevel)target.level(),target.position());if(mob!=null)created.add(mob);}
        // Original podchinenie runs at ALL fingers, including preexisting ones and overlaps.
        for(var finger:fingers(owner.level(),owner.position()))game.items().communism().subjugateAt(owner,(ServerLevel)finger.level(),finger.position(),e->true);
        for(var stack:claws(owner)){var drop=new ItemEntity(owner.level(),owner.getX(),owner.getY(),owner.getZ(),stack);drop.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(drop);owner.level().addFreshEntity(drop);}
        game.items().mana().award(owner,"lbc:true_advancements/legends/sins/indolence");
        return List.copyOf(created);
    }
    private Skeleton spawn(ServerPlayer owner,ServerLevel level,Vec3 at){
        if(!resident(level,at)||!LbcDimensionItems.ordinary(level))return null;
        var skeleton=new Skeleton(EntityTypes.SKELETON,level);
        try{skeleton.load(TagValueInput.create(ProblemReporter.DISCARDING,skeleton.registryAccess(),TagParser.parseCompoundFully(FINGER)));}
        catch(CommandSyntaxException error){throw new IllegalStateException("Source finger payload",error);}
        skeleton.setPos(at);skeleton.entityTags().add(MANAGED);game.items().markCurrentItemEpoch(skeleton);
        var source=new LbcSpellSource.Source(skeleton.getUUID(),owner.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(owner),Set.of("strict_map_object","rcdenyalways","magic_academy"),true);
        actors.put(skeleton,source);game.items().spellSources().bind(skeleton,source);game.items().timeStop().prepareFrozen(skeleton);
        if(level.addFreshEntity(skeleton)&&!skeleton.isRemoved())return skeleton;actors.remove(skeleton);skeleton.discard();return null;
    }
    List<Skeleton> fingers(ServerLevel level,Vec3 at){return select(level,at,"@e[type=minecraft:skeleton,tag=indolence_finger]").stream().filter(Skeleton.class::isInstance).map(Skeleton.class::cast).toList();}
    private List<ItemStack> claws(ServerPlayer p){return game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/indolence_claw");}
    private static void give(ServerPlayer p,ItemStack stack){if(!p.getInventory().add(stack)&&!stack.isEmpty())p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
    void reward(ServerPlayer p){
        for(var entry:ARMOR.entrySet())try{p.setItemSlot(entry.getKey(),new ItemParser(p.registryAccess()).parse(new StringReader(entry.getValue())).createItemStack(1));}
        catch(CommandSyntaxException error){throw new IllegalStateException("Source Indolence armor "+entry.getKey(),error);}
        give(p,new ItemStack(Items.BOW));give(p,new ItemStack(Items.ARROW,64));giveClaw(p);
        p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();game.items().mana().inventoryChanged(p);game.items().magic().inventoryChanged(p);
    }
    void giveClaw(ServerPlayer p){for(var stack:claws(p))give(p,stack);}
    private List<Skeleton> available(ServerPlayer p){return fingers(p.level(),p.position()).stream().filter(e->e.isAlive()&&!e.isRemoved()
            &&!consumed.contains(e.getUUID())&&LbcDimensionItems.ordinary(e.level())).toList();}
    /** Native death still happens; only the existing participant's life debit is waived. */
    boolean prepareDeath(ServerPlayer p){
        if(!live()||game.isObserver(p)||!p.entityTags().contains("sin_indolence")||!LbcDimensionItems.ordinary(p)||available(p).isEmpty()){
            p.entityTags().remove("indolence_active");rebirths.remove(p.getUUID());return false;
        }
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());ShadowAbsorption.save(p,output);
        rebirths.put(p.getUUID(),new Rebirth(game.items().epoch(),p.getActiveEffects().stream().map(EffectLayers::copy).toList(),
                game.items().ownedEffects().suspend(p),output.buildResult(),p.getAbsorptionAmount(),game.items().indirectKills().effectCauses(p)));
        p.entityTags().add("indolence_active");return true;
    }
    boolean fingerSpawn(ServerPlayer p){return pending(p)&&!available(p).isEmpty();}
    private boolean pending(ServerPlayer p){var saved=rebirths.get(p.getUUID());return saved!=null&&game.items().currentEpoch(saved.epoch())&&p.entityTags().contains("sin_indolence");}
    void respawned(ServerPlayer p){
        if(!live()||!game.allowDamage(p))return;
        if(pending(p)){if(paused(p.level()))ready.add(p.getUUID());else reborn(p);}
        else {rebirths.remove(p.getUUID());p.entityTags().remove("indolence_active");if(holder(p))giveClaw(p);}
    }
    private boolean paused(ServerLevel level){return game.items().wands().timeStopped()||game.items().timeStop().stopped(level);}
    private void reborn(ServerPlayer p){
        var saved=rebirths.remove(p.getUUID());ready.remove(p.getUUID());if(saved==null||!game.items().currentEpoch(saved.epoch()))return;
        var choices=available(p);var finger=choices.isEmpty()?null:choices.get(p.level().getRandom().nextInt(choices.size()));
        if(finger!=null){var motion=p.getDeltaMovement();
            try{p.teleportTo((ServerLevel)finger.level(),finger.getX(),finger.getY(),finger.getZ(),TeleportMomentum.positionFlags(Set.of()),finger.getYRot(),finger.getXRot(),true);}
            finally{p.setDeltaMovement(motion);}
        }
        for(var effect:saved.effects())game.items().indirectKills().inheritedEffect(p,EffectLayers.copy(effect),saved.causes().get(effect.getEffect()));
        ShadowAbsorption.beforeLoad(p);ShadowAbsorption.load(p,TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),saved.shadow()));
        game.items().ownedEffects().resume(p,saved.owned());p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(saved.absorption());
        reward(p);p.entityTags().remove("indolence_active");
        if(finger!=null){p.entityTags().add("kill_one_skeleton");costs.add(new Cost(finger,p,game.server().getTickCount()));}
        game.items().invalidateHud(p);
    }
    void forget(ServerPlayer p){rebirths.remove(p.getUUID());ready.remove(p.getUUID());p.entityTags().remove("indolence_active");p.entityTags().remove("kill_one_skeleton");}
    public void tick(){if(advancing)throw new IllegalStateException("Reentrant authority advance");advancing=true;try{
        int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        for(var id:List.copyOf(ready)){var p=game.server().getPlayerList().getPlayer(id);if(p==null||!live()||!pending(p)){ready.remove(id);rebirths.remove(id);}
            else if(!paused(p.level())&&game.allowDamage(p))reborn(p);}
        for(var cost:List.copyOf(costs)){
            if(!live()){costs.remove(cost);continue;}if(now==cost.born||paused(cost.level))continue;
            if(--cost.left>0)continue;costs.remove(cost);consumed.add(cost.finger);
            for(var level:game.server().getAllLevels()){var finger=level.getEntity(cost.finger);if(finger!=null){finger.discard();actors.remove(finger);break;}}
            var p=game.server().getPlayerList().getPlayer(cost.player);if(p!=null&&costs.stream().noneMatch(c->c.player.equals(cost.player)))p.entityTags().remove("kill_one_skeleton");
        }
        for(var e:List.copyOf(actors.keySet())){
            var state=actors.get(e);if(e.isRemoved()){actors.remove(e);continue;}
            if(!live()||!game.items().currentEpoch(state.epoch())){e.discard();actors.remove(e);continue;}
            if(game.items().wands().timeStopped()||game.items().timeStop().stopped((ServerLevel)e.level())||game.items().timeStop().frozen(e)||game.items().polymorph().suspended(e))continue;
            var current=select((ServerLevel)e.level(),e.position(),"@p[tag=sin_indolence,gamemode=!spectator]");
            if(!current.isEmpty()&&current.getFirst() instanceof ServerPlayer p&&holder(p)&&!state.author().equals(p.getUUID())){
                game.items().communism().subjugate(p,e,true,true);var updated=game.items().spellSources().source(e);if(updated!=null)actors.put(e,updated);
            }
        }
    }finally{advancing=false;}}
    public void loaded(Entity e){if(consumed.contains(e.getUUID())){e.discard();return;}if(!e.entityTags().contains(MANAGED))return;var source=actors.get(e);if(source==null)source=unloaded.remove(e.getUUID());
        if(source==null||!live()||!game.items().currentEpoch(source.epoch())){e.discard();return;}actors.put(e,source);game.items().spellSources().bind(e,source);game.items().timeStop().prepareFrozen(e);}
    public void unloaded(Entity e){var source=actors.remove(e);if(source!=null&&e.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(e.getUUID(),source);}
    public void stop(){for(var p:game.server().getPlayerList().getPlayers()){p.entityTags().remove("indolence_active");p.entityTags().remove("kill_one_skeleton");}for(var e:List.copyOf(actors.keySet()))e.discard();actors.clear();unloaded.clear();rebirths.clear();ready.clear();costs.clear();consumed.clear();lastTick=Integer.MIN_VALUE;}
    List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    void checkpointBoundary(){if(advancing)throw new IllegalStateException("Finish finger/rebirth callbacks first");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    private static Rebirth copyRebirth(Rebirth r){return new Rebirth(r.epoch,r.effects.stream().map(EffectLayers::copy).toList(),OwnedEffectLayers.copyCheckpoint(r.owned),r.shadow.copy(),r.absorption,new HashMap<>(r.causes));}
    final class Checkpoint {
        private record Payment(UUID finger,UUID player,ServerLevel level,int born,int left) { }
        private final MatchSpellCheckpoint.IdentityImage<Entity,LbcSpellSource.Source> image;
        private final Map<UUID,LbcSpellSource.Source> asleep=new HashMap<>(unloaded);
        private final Map<UUID,Rebirth> births=new HashMap<>();
        private final Set<UUID> readyImage=Set.copyOf(ready),spent=Set.copyOf(consumed);
        private final List<Payment> payments=costs.stream().map(c->new Payment(c.finger,c.player,c.level,c.born,c.left)).toList();
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        Checkpoint(MatchSpellCheckpoint.Links links){image=new MatchSpellCheckpoint.IdentityImage<>(actors,links);rebirths.forEach((id,r)->births.put(id,copyRebirth(r)));}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=image.restore(links);var records=new HashMap<UUID,Rebirth>();births.forEach((id,r)->records.put(id,copyRebirth(r)));
            var queue=new ArrayList<Cost>();for(var payment:payments){MatchAuthorityCheckpoint.world(game,payment.level);queue.add(new Cost(payment.finger,payment.player,payment.level,clock.time(payment.born,game),payment.left));}
            return ()->{actors=restored;unloaded.clear();unloaded.putAll(asleep);rebirths.clear();rebirths.putAll(records);ready.clear();ready.addAll(readyImage);consumed.clear();consumed.addAll(spent);costs.clear();costs.addAll(queue);lastTick=clock.last(game);};}
    }

    static boolean cloneRecognizes(Entity e){return e.entityTags().contains(MANAGED);}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        var state=actors.get(source);if(state==null)return null;
        var payments=costs.stream().filter(c->c.finger.equals(source.getUUID())).map(c->new Cost(c.finger,c.player,c.level,c.born,c.left)).toList();
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var next=new LbcSpellSource.Source(clone.getUUID(),state.author(),state.epoch(),state.cause(),state.excluded(),state.persistent());
                actors.put(clone,next);game.items().spellSources().bind(clone,next);
                for(var c:payments)costs.add(new Cost(clone.getUUID(),c.player,c.level,c.born,c.left));
            }
            public void rollback(Entity clone){actors.remove(clone);costs.removeIf(c->c.finger.equals(clone.getUUID()));}
        };
    }

}
