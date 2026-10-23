package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native piglin health, retaliation, movement, melee and death; source five-tick gun dispatcher. */
public final class LbcStalin {
    private static final String ACTOR="skywars2.stalin.actor";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final CompoundTag COMMUNISM=communism();
    private static CompoundTag communism() {var tag=new CompoundTag();tag.putInt("communism",1);return tag;}
    private final GameController game;
    private final class Actor extends ZombifiedPiglin {
        Actor(ServerLevel level) {super(EntityTypes.ZOMBIFIED_PIGLIN,level);}
        @Override public boolean shouldBeSaved() {return false;}
        @Override public boolean canAttack(LivingEntity target) {
            return game.items().wands().validTarget(target) && super.canAttack(target);
        }
    }
    private static final class State {
        final ZombifiedPiglin actor;final String epoch;int ticks;Integer cooldown;
        State(ZombifiedPiglin actor,String epoch) {this.actor=actor;this.epoch=epoch;}
    }
    public record View(ZombifiedPiglin actor,int activeTicks,Integer cooldown) { }
    private final Map<ZombifiedPiglin,State> actors=new LinkedHashMap<>();
    // Retain causes through the synchronous native death callback and terrain consequences.
    private final Map<Entity,LivingEntity> authors=new WeakHashMap<>();
    LbcStalin(GameController game) {this.game=game;}
    public static boolean managed(Entity entity) {return entity!=null && entity.entityTags().contains(ACTOR);}
    public LivingEntity owner(Entity entity) {return authors.get(entity);}
    private static boolean resident(ServerLevel level,BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    boolean ready(ItemEntity token) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcStalinDropItem.isTrigger(token.getItem())
                || !(token.level() instanceof ServerLevel level) || level!=game.maps().level()
                || !game.playing() || game.stage()==GameController.Stage.ENDING) return false;
        var at=token.position().add(0,.5,0);
        if(level.isOutsideBuildHeight(BlockPos.containing(at)) || level.isOutsideBuildHeight(BlockPos.containing(at.add(0,2,0)))) return false;
        var box=new AABB(at,at.add(0,2,0)).inflate(.5);
        for(int x=net.minecraft.util.Mth.floor(box.minX)>>4;x<=net.minecraft.util.Mth.floor(box.maxX)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(box.minZ)>>4;z<=net.minecraft.util.Mth.floor(box.maxZ)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
    /** Dispatcher owns debit; source summon keeps fractional token XYZ and adds only .5Y. */
    void spawn(ItemEntity token) {
        if(!ready(token)) return;
        var level=(ServerLevel)token.level();var actor=new Actor(level);
        try {
            actor.load(TagValueInput.create(ProblemReporter.DISCARDING,actor.registryAccess(),TagParser.parseCompoundFully(
                    "{Silent:1b,CustomNameVisible:1b,PersistenceRequired:1b,Health:500f,CustomName:{color:'red',italic:false,translate:'stalin'},equipment:{mainhand:{id:'minecraft:carrot_on_a_stick',components:{'minecraft:item_model':'lbc:ak47'},count:1},feet:{id:'minecraft:leather_boots',components:{'minecraft:equippable':{slot:'feet',asset_id:'lbc:stalin'}},count:1},legs:{id:'minecraft:leather_leggings',count:1},chest:{id:'minecraft:leather_chestplate',components:{'minecraft:equippable':{slot:'chest',asset_id:'lbc:stalin'}},count:1},head:{id:'minecraft:leather_helmet',components:{'minecraft:unbreakable':{},'minecraft:equippable':{slot:'head',asset_id:'lbc:stalin'}},count:1}},active_effects:[{id:'minecraft:fire_resistance',duration:2000000},{id:'minecraft:invisibility',duration:2000000}],attributes:[{id:'minecraft:max_health',base:500d},{id:'minecraft:follow_range',base:100d}],drop_chances:{chest:0f,feet:0f,legs:0f,head:0f,mainhand:0f},DeathLootTable:'lbc:stalin'}")));
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) {throw new IllegalStateException(error);}
        actor.setInvisible(true);actor.setPos(token.position().add(0,.5,0));
        adoptEgg(actor,game.items().specialDrops().owner(token));
        if(!level.addFreshEntity(actor)) {terminal(actor);return;}
        game.items().timeStop().prepareFrozen(actor);
    }
    /** Attach only encounter state; native egg placement and its distinct equipment remain intact. */
    void adoptEgg(ZombifiedPiglin actor,LivingEntity author) {
        actor.entityTags().addAll(List.of(ACTOR,"stalin","lucky_mob"));
        game.items().markCurrentItemEpoch(actor);
        if(author!=null) authors.put(actor,author);
        actors.put(actor,new State(actor,game.items().epoch()));
        game.items().timeStop().prepareFrozen(actor);
    }
    void tick() {
        for(var state:List.copyOf(actors.values())) {
            var actor=state.actor;var level=(ServerLevel)actor.level();
            if(!game.items().currentEpoch(state.epoch) || !game.playing() || game.stage()==GameController.Stage.ENDING
                    || actor.isRemoved() || !actor.isAlive() || !resident(level,actor.blockPosition())) {terminal(actor);continue;}
            if(game.items().timeStop().stopped(level)) continue;
            if(++state.ticks%5==0) pulse(actor);
        }
    }
    private boolean active(ZombifiedPiglin actor) {
        return actors.containsKey(actor) && actor.isAlive() && !actor.isRemoved()
                && game.items().currentEpoch(actor) && game.playing() && game.stage()!=GameController.Stage.ENDING
                && !game.items().timeStop().stopped((ServerLevel)actor.level())
                && !game.items().magic().mechanismSuppressedAt((ServerLevel)actor.level(),actor.position());
    }
    /** Missing timer is distinct from zero: source pre2 initializes it only once injured. */
    void pulse(ZombifiedPiglin actor) {
        if(!active(actor)) return;
        var state=actors.get(actor);
        if((int)actor.getHealth()<=495) {
            if(Objects.equals(state.cooldown,0) && actor.getRandom().nextFloat()<.5f && nearbyMob(actor)) shot(actor);
            if(state.cooldown==null || state.cooldown<0 || state.cooldown>16) {
                LbcAk47.sound(actor,actor.position(),"minecraft:lbcsounds.rasstrelat",2,1,0,0);
                state.cooldown=16;
            }
        }
        if(state.cooldown!=null && state.cooldown>=1 && state.cooldown<=16) state.cooldown--;
    }
    private boolean mob(Entity target) {
        return target instanceof LivingEntity && target.getType().builtInRegistryHolder().is(MOBS)
                && game.items().wands().validTarget(target);
    }
    private boolean nearbyMob(ZombifiedPiglin actor) {
        for(var target:actor.level().getEntities(actor,actor.getBoundingBox().inflate(40),this::mob))
            if(!target.entityTags().contains("stalin") && actor.position().distanceToSqr(target.position())<=1600) return true;
        return false;
    }
    static boolean communism(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(COMMUNISM);
    }
    private boolean aimAllowed(Entity target) {
        if(!mob(target) || target.entityTags().contains("stalin") || target.entityTags().contains("communist")) return false;
        // Source Inventory NBT is a player's complete Inventory, including armor and offhand.
        if(target instanceof ServerPlayer player) for(int i=0;i<player.getInventory().getContainerSize();i++)
            if(communism(player.getInventory().getItem(i))) return false;
        if(target instanceof net.minecraft.world.entity.npc.InventoryCarrier carrier)
            for(int i=0;i<carrier.getInventory().getContainerSize();i++)
                if(communism(carrier.getInventory().getItem(i))) return false;
        return true;
    }
    List<LivingEntity> aimTargets(ZombifiedPiglin actor) {
        var targets=new ArrayList<LivingEntity>();
        // Source facing forks all sorted eligible targets; discovery itself has no range.
        for(var entity:((ServerLevel)actor.level()).getAllEntities())
            if(entity!=actor && aimAllowed(entity)) targets.add((LivingEntity)entity);
        targets.sort(Comparator.comparingDouble(entity->entity.position().distanceToSqr(actor.position())));
        return targets;
    }
    LivingEntity aimTarget(ZombifiedPiglin actor) {
        var targets=aimTargets(actor);return targets.isEmpty()?null:targets.getFirst();
    }
    void shot(ZombifiedPiglin actor) {
        if(!active(actor)) return;
        int yawSpread=actor.getRandom().nextInt(9)-4,pitchSpread=actor.getRandom().nextInt(9)-4;
        var targets=aimTargets(actor);
        boolean first=true;
        for(var target:targets) {
            if(!active(actor)) break;
            var look=target.getEyePosition().subtract(actor.getEyePosition());
            float yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z))+yawSpread;
            float pitch=(float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance()))+pitchSpread;
            // Shared source Distance was reset BEFORE execute-facing fork, never per branch.
            // First ray exhausts it; subsequent branches emit just their initial origin sample.
            game.items().ak47().nativeRay(actor,Vec3.directionFromRotation(pitch,yaw),this::mob,first?160:0);
            first=false;
        }
        LbcAk47.sound(actor,actor.position(),"minecraft:lbcsounds.shot",.5f,1,200,.05f);
    }
    /** Testable deterministic direction; null retains source no-aim/no-ray. */
    void shot(ZombifiedPiglin actor,Vec3 direction) {
        if(!active(actor)) return;
        if(direction!=null) game.items().ak47().nativeRay(actor,direction,this::mob);
        LbcAk47.sound(actor,actor.position(),"minecraft:lbcsounds.shot",.5f,1,200,.05f);
    }
    public boolean canAffect(Entity source,Entity target) {
        return !managed(source) || game.items().wands().validTarget(target);
    }
    private void terminal(ZombifiedPiglin actor) {
        actors.remove(actor);if(!actor.isRemoved()) actor.discard();
    }
    public void loaded(Entity entity) {
        if(!managed(entity)) return;
        // Custom actors are transient; any disk reconstruction is stale rather than a second encounter.
        if(!(entity instanceof ZombifiedPiglin actor) || !actors.containsKey(actor)
                || !game.items().currentEpoch(entity) || !game.playing() || game.stage()==GameController.Stage.ENDING) entity.discard();
    }
    public void unloaded(Entity entity) {
        if(managed(entity) && entity instanceof ZombifiedPiglin actor) terminal(actor);
    }
    void stop() {for(var actor:List.copyOf(actors.keySet())) terminal(actor);authors.clear();}
    List<View> snapshot() {return actors.values().stream().map(s->new View(s.actor,s.ticks,s.cooldown)).toList();}
    int activeCount() {return actors.size();}
}
