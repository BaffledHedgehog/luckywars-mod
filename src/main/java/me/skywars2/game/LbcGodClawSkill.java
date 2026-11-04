package me.skywars2.game;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Finite skill23. Virtual markers retain source generations without per-cell entities or world scans. */
public final class LbcGodClawSkill {
    /** Admission policies are distinct: suppressing a frontier need not suppress charging or cooldown.
     * removeFamily returns true when the canonical owner handled the complete family (including a
     * deliberately surviving Chef); false asks for ordinary /kill semantics. summonVoidMan must use
     * the existing native Consuming preset/ownership and return true only after successful admission.
     * No policy may start asynchronous removal: an accepted contact is one native transaction. */
    public interface Policy {
        boolean owner(ServerPlayer owner);
        boolean cell(ServerPlayer owner,ServerLevel level,BlockPos position);
        boolean target(ServerPlayer owner,Entity target);
        boolean removeFamily(ServerPlayer owner,Entity target);
        boolean summonVoidMan(ServerPlayer owner,Vec3 position);
    }
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs_only"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    public static void register(){net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:god_claw"),TICKET);}
    static final BlockState CONSUMED=Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()
            .setValue(HugeMushroomBlock.DOWN,false).setValue(HugeMushroomBlock.EAST,false)
            .setValue(HugeMushroomBlock.NORTH,false).setValue(HugeMushroomBlock.SOUTH,false)
            .setValue(HugeMushroomBlock.UP,true).setValue(HugeMushroomBlock.WEST,false);
    private static final List<Holder<Attribute>> ATTRIBUTES=List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,
            Attributes.ATTACK_DAMAGE,Attributes.ATTACK_KNOCKBACK,Attributes.ATTACK_SPEED,Attributes.BURNING_TIME,
            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.JUMP_STRENGTH,
            Attributes.KNOCKBACK_RESISTANCE,Attributes.LUCK,Attributes.MAX_HEALTH,Attributes.MOVEMENT_EFFICIENCY,
            Attributes.MOVEMENT_SPEED,Attributes.OXYGEN_BONUS,Attributes.SAFE_FALL_DISTANCE,Attributes.SCALE,
            Attributes.STEP_HEIGHT,Attributes.WATER_MOVEMENT_EFFICIENCY,Attributes.BLOCK_BREAK_SPEED,
            Attributes.BLOCK_INTERACTION_RANGE,Attributes.ENTITY_INTERACTION_RANGE,Attributes.MINING_EFFICIENCY,
            Attributes.SNEAKING_SPEED,Attributes.SUBMERGED_MINING_SPEED,Attributes.SWEEPING_DAMAGE_RATIO);
    private static final int[] SCALE={100,100,1000,1000,10000,10000,100000,100000,10000,100000,
            5000,1000,100,100000,100,100,10000,500,10000,1000,10000,10000,100,1000,100,1000};
    private final GameController game;
    private final Policy policy;
    private final LinkedHashMap<UUID,State> owners=new LinkedHashMap<>();
    private final Map<ServerLevel,Long2ObjectOpenHashMap<State>> positions=new IdentityHashMap<>();
    private final Map<Chunk,CompletableFuture<?>> loading=new LinkedHashMap<>();
    private final Set<Chunk> neededLoading=new HashSet<>();
    private boolean restoring;
    private int lastTick=Integer.MIN_VALUE,turn;
    private record Chunk(ServerLevel level,ChunkPos pos){}
    private static final class Cell {
        final long position;
        int neighbor;
        boolean written;
        Cell(long position){this.position=position;}
        Cell copy(){var result=new Cell(position);result.neighbor=neighbor;result.written=written;return result;}
    }
    private static final class State {
        final UUID owner;final ServerLevel level;
        int score;boolean active,cooling;
        long random;
        ArrayDeque<Cell> current=new ArrayDeque<>(),next=new ArrayDeque<>();
        State(ServerPlayer p){owner=p.getUUID();level=p.level();random=p.getRandom().nextLong();}
        State(State s){owner=s.owner;level=s.level;score=s.score;active=s.active;cooling=s.cooling;random=s.random;
            s.current.forEach(c->current.add(c.copy()));s.next.forEach(c->next.add(c.copy()));}
        int random(int bound){
            // Checkpointed SplitMix64, rejection sampling preserves uniform discrete outcomes.
            long u,r;
            do{random+=0x9e3779b97f4a7c15L;long z=random;z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;
                z=(z^(z>>>27))*0x94d049bb133111ebL;u=(z^(z>>>31))>>>1;r=u%bound;
            }while(u-r+(bound-1)<0);return (int)r;
        }
    }
    public LbcGodClawSkill(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    private boolean paused(ServerPlayer p){return game.items().timeStop().stopped(p.level())
            ||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p);}
    private boolean admitted(ServerPlayer p){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!game.isObserver(p)
            &&game.canUseGameplayItems(p)&&LbcDimensionItems.ordinary(p.level())&&game.lbc().hasSkill(p,23);}
    public void tick(){
        int tick=game.server().getTickCount();
        if(restoring||lastTick==tick||!game.playing()||game.maps().busy()||!game.server().tickRateManager().runsNormally())return;
        lastTick=tick;
        neededLoading.clear();
        for(var id:List.copyOf(owners.keySet())){
            var p=game.server().getPlayerList().getPlayer(id);var state=owners.get(id);
            if(p!=null&&(!admitted(p)||p.level()!=state.level))clear(id);
        }
        for(var p:game.server().getPlayerList().getPlayers()){
            if(!admitted(p)||paused(p))continue;
            var s=owners.computeIfAbsent(p.getUUID(),id->new State(p));
            if(!policy.owner(p)){end(s);continue;}
            if(!p.isShiftKeyDown()){
                if(s.cooling&&s.score>0&&s.random(5)==0)s.score--;
                if(s.active)end(s);
                if(!s.cooling)s.score=0;
                if(s.score==0)s.cooling=false;
                continue;
            }
            if(s.active){
                // A generation is a source marker selector snapshot. Accepted work is never discarded
                // to meet a tick budget, and deferred CPU work never grants additional attribute rolls.
                if(!s.current.isEmpty())continue;
                var q=s.current;s.current=s.next;s.next=q;
                int count=s.current.size();
                s.score++;
                if(count==0)end(s);
                int choice=s.random(26),scale=SCALE[choice];
                LbcAttributeChanges.change(p,ATTRIBUTES.get(choice),scale,(choice==5||choice==7)?-count:count,1d/scale);
                p.level().sendParticles(ParticleTypes.PORTAL,p.getX(),p.getY()+1,p.getZ(),30,.5,.5,.5,0);
                if(s.score>=90)end(s);
            }else if(!s.cooling&&++s.score==60){
                s.active=s.cooling=true;
                claim(s,BlockPos.containing(p.getX(),p.getY()-.5,p.getZ()),s.current);
            }
        }
        var active=new ArrayList<>(owners.values());
        long deadline=EffectTerrain.deadlineNanos();int budget=1024,rounds=0;
        while(!active.isEmpty()&&budget>0&&System.nanoTime()<deadline){
            var s=active.get(Math.floorMod(turn++,active.size()));
            var p=game.server().getPlayerList().getPlayer(s.owner);
            if(!s.active||s.current.isEmpty()||p==null||paused(p)){
                if(++rounds>=active.size())break;continue;
            }
            rounds=0;int quantum=Math.min(16,budget);
            while(quantum-->0&&!s.current.isEmpty()&&System.nanoTime()<deadline){
                var cell=s.current.peek();
                budget--;
                boolean complete;
                try(var scope=game.items().spellSources().scope(game.items().spellSources().hostile(p,p,Set.of()),s.level)){
                    complete=consume(p,s,cell);
                }
                if(!complete)break;
                s.current.remove();index(s.level).remove(cell.position,s);
            }
            // Loading one frontier must not spin until the entire shared deadline is spent.
            if(++rounds>=active.size()&&active.stream().allMatch(a->a.current.isEmpty()
                    ||!a.level.hasChunkAt(BlockPos.of(a.current.peek().position))))break;
        }
        releaseUnusedTickets();
    }
    private Long2ObjectOpenHashMap<State> index(ServerLevel level){return positions.computeIfAbsent(level,k->new Long2ObjectOpenHashMap<>());}
    private void claim(State s,BlockPos p,ArrayDeque<Cell> queue){
        if(p.getY()<s.level.getMinY()||p.getY()>=s.level.getMaxY())return;
        if(index(s.level).putIfAbsent(p.asLong(),s)==null)queue.add(new Cell(p.asLong()));
    }
    private boolean resident(ServerLevel level,BlockPos p){
        if(level.hasChunkAt(p))return true;
        var key=new Chunk(level,new ChunkPos(p.getX()>>4,p.getZ()>>4));var old=loading.get(key);
        neededLoading.add(key);
        if(old!=null&&(old.isCancelled()||old.isCompletedExceptionally())){
            level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);loading.remove(key);
        }
        if(loading.size()<8)loading.computeIfAbsent(key,k->level.getChunkSource().addTicketAndLoadWithRadius(TICKET,k.pos,0));
        return false;
    }
    private boolean consume(ServerPlayer p,State s,Cell cell){
        var pos=BlockPos.of(cell.position);
        if(!resident(s.level,pos))return false;
        if(!policy.cell(p,s.level,pos))return true;
        if(!cell.written){
            game.items().indirectKills().supportRemoved(s.level,p,pos);
            s.level.setBlock(pos,CONSUMED,Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            cell.written=true;
        }
        // Persist the cursor before any asynchronous chunk load. Previously accepted children retain
        // their claim; a repeated resume cannot reroll a neighbor or duplicate conversion output.
        while(cell.neighbor<26){
            int n=cell.neighbor,dx=n%3-1,dy=n/3%3-1,dz=n/9-1;
            // Enumerate the 27 cube offsets except the center, preserving all26 exactly once.
            int actual=n>=13?n+1:n;dx=actual%3-1;dy=actual/3%3-1;dz=actual/9-1;
            var target=pos.offset(dx,dy,dz);
            if(target.getY()<s.level.getMinY()||target.getY()>=s.level.getMaxY()){cell.neighbor++;continue;}
            if(!resident(s.level,target))return false;
            cell.neighbor++;
            if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)!=1&&s.random(5)>=2)continue;
            var state=s.level.getBlockState(target);
            if(!state.is(AIRS)&&!state.isAir()&&!state.equals(CONSUMED))claim(s,target,s.next);
        }
        Vec3 center=Vec3.atCenterOf(pos);
        var contacts=s.level.getEntities((Entity)null,new AABB(center,center).inflate(2),e->eligible(e)
                &&e.position().distanceToSqr(center)<=4&&policy.target(p,e));
        for(var e:contacts){
            if(e.isRemoved())continue;
            if(e instanceof ItemEntity){
                var outputs=game.lbc().rollTable(s.level,e.position(),p,"lbc:item_defs/lbc/liquidabyss__6e31baa5f9d5");
                if(outputs.size()!=1||outputs.getFirst().isEmpty())throw new IllegalStateException("Missing canonical Liquid Abyss definition");
                var output=new ItemEntity(s.level,e.getX(),e.getY(),e.getZ(),outputs.getFirst().copyWithCount(1));
                output.setDefaultPickUpDelay();output.setThrower(p);game.items().markCurrentItemEpoch(output);
                game.items().spellSources().capture(output);
                // Source outputs are permanently immune to subsequent frontier contacts.
                output.entityTags().add("clawed_entity");
                if(!s.level.addFreshEntity(output))continue;
            }else if((e instanceof Mob||e.getType().builtInRegistryHolder().is(MOBS))
                    &&!(e instanceof LbcWtf.Chef)&&!policy.summonVoidMan(p,e.position()))continue;
            if(!policy.removeFamily(p,e)){
                if(e instanceof LivingEntity living)living.hurtServer(s.level,game.items().spellSources().damage(p.damageSources().genericKill()),Float.MAX_VALUE);
                else e.kill(s.level);
            }
        }
        return true;
    }
    private boolean eligible(Entity e){return !(e instanceof Player)&&!e.isRemoved()&&!e.isSpectator()
            &&!e.entityTags().contains("clawed_entity")&&!e.entityTags().contains("strict_map_object")
            &&!e.entityTags().contains("spectator")&&!e.entityTags().contains("magic_academy")
            &&!game.items().timeStop().frozen(e)&&game.items().wands().validRoleTarget(e,false);}
    private void end(State s){
        if(!s.active)return;s.active=false;s.cooling=true;
        for(var cell:s.current)index(s.level).remove(cell.position,s);
        for(var cell:s.next)index(s.level).remove(cell.position,s);
        s.current.clear();s.next.clear();
    }
    public int score(ServerPlayer p){var s=owners.get(p.getUUID());return s==null?0:s.score;}
    public void clear(ServerPlayer p){if(restoring||MatchPlayerCheckpoint.restoring(p))return;clear(p.getUUID());releaseUnusedTickets();}
    private void clear(UUID id){var s=owners.remove(id);if(s!=null)end(s);}
    private void releaseUnusedTickets(){
        for(var key:List.copyOf(loading.keySet())){
            if(!neededLoading.contains(key)){key.level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);loading.remove(key);}
        }
    }
    private void releaseTickets(){for(var key:loading.keySet())key.level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);loading.clear();neededLoading.clear();}
    public void stop(){releaseTickets();owners.clear();positions.clear();turn=0;restoring=false;lastTick=Integer.MIN_VALUE;}
    public void prepareFrozen(){restoring=true;releaseTickets();owners.clear();positions.clear();}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final List<State> saved=owners.values().stream().map(State::new).toList();final int savedTurn=turn;
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var restored=new LinkedHashMap<UUID,State>();
            for(var s:saved)restored.put(s.owner,new State(s));
            return new MatchFieldCheckpoint.Plan(Set.of(),()->{
                owners.clear();owners.putAll(restored);positions.clear();turn=savedTurn;
                for(var s:owners.values()){
                    for(var c:s.current)index(s.level).put(c.position,s);
                    for(var c:s.next)index(s.level).put(c.position,s);
                }
            });
        }
    }
    int frontier(ServerPlayer p){var s=owners.get(p.getUUID());return s==null?0:s.current.size()+s.next.size();}
    boolean active(ServerPlayer p){var s=owners.get(p.getUUID());return s!=null&&s.active;}
}
