package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Source one-block samples, through mobs until terrain, with native AI and explicit ownership. */
public final class LbcCommunism {
    private static final TagKey<net.minecraft.world.level.block.Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    public static final Item ITEM=create();
    private final GameController game;
    private final Map<Entity,UUID> owners=new WeakHashMap<>();
    private final Map<UUID,Set<Entity>> controlled=new HashMap<>();
    private final Map<String,Entity> resident=new HashMap<>();
    private final IdentityHashMap<Entity,Integer> ownershipScores=new IdentityHashMap<>();
    private final Map<Integer,Set<Entity>> scoreCohorts=new HashMap<>();
    private static Set<Entity> identitySet(){return Collections.newSetFromMap(new IdentityHashMap<>());}
    private void removeOwner(Entity actor){
        var prior=owners.remove(actor);if(prior==null)return;
        var bucket=controlled.get(prior);if(bucket!=null){bucket.remove(actor);if(bucket.isEmpty())controlled.remove(prior);}
    }
    private void putOwner(Entity actor,UUID id){removeOwner(actor);owners.put(actor,id);controlled.computeIfAbsent(id,key->identitySet()).add(actor);}
    public void loaded(Entity actor){
        if(!(actor instanceof net.minecraft.world.entity.LivingEntity)||actor.isRemoved())return;
        var old=resident.put(actor.getScoreboardName(),actor);if(old!=null&&old!=actor)removeScore(old);
        var objective=game.server().getScoreboard().getObjective("lbcID2");
        var score=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(actor,objective);
        ownershipScoreChanged(actor.getScoreboardName(),score==null?null:score.value());
    }
    private void removeScore(Entity actor){
        var prior=ownershipScores.remove(actor);if(prior==null)return;
        var bucket=scoreCohorts.get(prior);if(bucket!=null){bucket.remove(actor);if(bucket.isEmpty())scoreCohorts.remove(prior);}
    }
    public void ownershipScoreChanged(String name,Integer value){
        var actor=resident.get(name);if(actor==null)return;removeScore(actor);
        if(value!=null){ownershipScores.put(actor,value);scoreCohorts.computeIfAbsent(value,key->identitySet()).add(actor);}
    }
    public void ownershipObjectiveRemoved(){ownershipScores.clear();scoreCohorts.clear();}
    /** Canonical score ownership plus native controller/summon authority, independent of distance. */
    List<Entity> controlledActors(UUID id){
        var result=identitySet();result.addAll(controlled.getOrDefault(id,Set.of()));
        var player=game.server().getPlayerList().getPlayer(id);
        var objective=game.server().getScoreboard().getObjective("lbcID2");
        var score=player==null||objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(player,objective);
        if(score!=null)result.addAll(scoreCohorts.getOrDefault(score.value(),Set.of()));
        result.addAll(game.items().summons().controlledActors(id));
        // A newer native controller wins over historical lbcID2 retained by the Communism ray.
        result.removeIf(actor->{var current=owners.get(actor);return current!=null&&!current.equals(id);});
        return List.copyOf(result);
    }
    void reindexOwnership(Collection<Entity> bodies){
        resident.clear();ownershipScores.clear();scoreCohorts.clear();controlled.clear();
        owners.forEach((actor,id)->controlled.computeIfAbsent(id,key->identitySet()).add(actor));
        for(var actor:bodies)loaded(actor);
    }

    LbcCommunism(GameController game){this.game=game;}
    private static Item create(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:communism"));
        var flags=new CompoundTag();flags.putInt("communism",1);flags.putInt("raycastspell",1);
        var props=new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse("lbc:communism");}}
                .setId(key).stacksTo(1).overrideDescription("socializm")
                .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:communism"))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_s"))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("socializm").withStyle(s->s.withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("socializm.lore").withStyle(s->s.withColor(0xffffff).withItalic(false)))))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(flags));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var g=SkyWars2.game();return g!=null&&player instanceof ServerPlayer p&&g.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static void register(){}
    static boolean matches(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||LbcFartus.data(stack).getIntOr("communism",0)==1);}
    ServerPlayer owner(Entity entity){var id=owners.get(entity);return id==null?null:game.server().getPlayerList().getPlayer(id);}
    void removed(Entity entity){removeOwner(entity);removeScore(entity);resident.remove(entity.getScoreboardName(),entity);}
    public void converted(Entity original,Entity converted){var id=owners.get(original);removed(original);loaded(converted);if(id!=null)putOwner(converted,id);}
    void stop(){owners.clear();controlled.clear();resident.clear();ownershipScores.clear();scoreCohorts.clear();}
    /** One native team/AI owner bridge shared by the ray and authority subjugation. */
    private void control(ServerPlayer p,Entity target,boolean leave){
        var board=game.server().getScoreboard();
        if(leave)board.removePlayerFromTeam(target.getScoreboardName());
        if(p.getTeam()!=null)board.addPlayerToTeam(target.getScoreboardName(),p.getTeam());
        // A converted player is still the author of their own attacks, never an owned summon.
        if(!(target instanceof Player)){
            putOwner(target,p.getUUID());game.items().summons().ownerChanged(target,p);
            if(target instanceof Mob mob){mob.setTarget(null);mob.setLastHurtByMob(null);}
        }
        if(target.entityTags().contains("sauvojen_tuntija"))p.entityTags().add("friendly_tuntija");
    }
    /** lbcr:podchinenie keeps its distinct counter payload; historical projectile causes are not rebound. */
    void subjugate(ServerPlayer p,Entity target,boolean leave,boolean copyId){
        if(target instanceof ServerPlayer player&&game.items().magic().rejectCustomDebuff(player))return;
        control(p,target,leave);copyScore(p,target,"team_number");if(copyId)copyScore(p,target,"lbcID2");
        if(!(target instanceof Player))game.items().spellSources().controllerChanged(target,p);
    }
    /** Exact nonplayer lbcr:podchinenie cell, reused by authority actor producers. */
    void subjugateAt(ServerPlayer p,net.minecraft.server.level.ServerLevel level,Vec3 at,java.util.function.Predicate<Entity> allowed){
        var cell=new AABB(at,at.add(1,1,1));
        for(var e:level.getEntities((Entity)null,cell,e->!(e instanceof Player)&&e.getType().builtInRegistryHolder().is(MOBS)
                &&e.position().distanceToSqr(at)<=225&&game.items().wands().validRoleTarget(e,false,true)&&!LbcGreed.active(e)
                &&!game.items().polymorph().concealed(e)&&allowed.test(e)))subjugate(p,e,true,true);
        LbcOverload.sound(level,at,"minecraft:entity.villager.trade",1,1,0,0);
    }
    private void copyScore(Entity from,Entity to,String name){
        var board=game.server().getScoreboard();var objective=board.getObjective(name);
        var value=objective==null?null:board.getPlayerScoreInfo(from,objective);
        if(value!=null)board.getOrCreatePlayerScore(to,objective,true).set(value.value());
    }
    boolean use(ServerPlayer p,InteractionHand hand){
        if(!matches(p.getItemInHand(hand))||!game.items().mana().visible(p)||!game.items().wands().validTarget(p)
                ||game.items().actionsBlocked(p)||game.items().magic().suppressed(p)||game.items().timeStop().stopped(p.level())
                ||p.entityTags().contains("nomagic_active")||p.entityTags().contains("time_stopped")||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var level=p.level();var direction=p.getLookAngle();var start=p.getEyePosition().add(direction.scale(hand==InteractionHand.OFF_HAND?.5:0));
        if(game.items().magic().suppressedAt(level,start))return false;
        var visited=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        // Legacy Distance starts at zero and tests <=200 after increment: exactly201 samples.
        // Only resident chunks are read; the ray never loads terrain or delays movement over ticks.
        for(int step=0;step<=200;step++){
            var at=start.add(direction.scale(step));var block=BlockPos.containing(at);
            if(level.isOutsideBuildHeight(block)||level.getChunkSource().getChunkNow(block.getX()>>4,block.getZ()>>4)==null)break;
            var box=new AABB(at.x,at.y,at.z,at.x+1,at.y+1,at.z+1);
            var targets=level.getEntities(p,box,e->!(e instanceof Player)&&e.getType().builtInRegistryHolder().is(MOBS)
                    &&game.items().wands().validTarget(e)&&!LbcGreed.active(e));
            for(var target:targets){
                if(!visited.add(target))continue;
                control(p,target,true);target.entityTags().add("communist");
            }
            if(!targets.isEmpty())LbcOverload.sound(p,at,"minecraft:entity.villager.trade",1,1,0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,at.x,at.y,at.z,1,.1,.1,.1,.2);
            if(!level.getBlockState(block).is(AIRS))break;
        }
        return true;
    }
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private final Map<MatchDamageSources.Ref,UUID> image=new LinkedHashMap<>();
        Checkpoint(MatchSpellCheckpoint.Links links){owners.forEach((actor,id)->image.put(links.key(actor),id));}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=new WeakHashMap<Entity,UUID>();image.forEach((ref,id)->restored.put(links.get(ref),id));
            return ()->{owners.clear();controlled.clear();restored.forEach((actor,id)->putOwner(actor,id));};}
    }

    LbcCloneSkills.ActorImage cloneImage(Entity source){var owner=owners.get(source);if(owner==null)return null;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){putOwner(clone,owner);}
            public void rollback(Entity clone){removeOwner(clone);}
        };
    }

}
