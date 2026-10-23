package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.*;

/** Native acquisition and the production Electro dispatcher, with only owned fixture cleanup. */
public final class TransformatorChecks {
    private TransformatorChecks() { }
    private static Fixture running, retained;
    private static String stopEpoch;
    private static List<Entity> stopActors = List.of();
    private static ItemEntity stopToken;
    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("Transformator: " + message);
    }
    private static net.minecraft.nbt.CompoundTag saved(Entity entity, ServerLevel level) {
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        entity.saveWithoutId(out);return out.buildResult();
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer owner, other;
        final GameChecks.WandThirdPlayerState ownerState, otherState;
        final Vec3 origin;
        final BlockPos base;
        final Map<BlockPos, MapManager.BlockSnapshot> blocks = new LinkedHashMap<>();
        final BoundingBox bounds, highBounds;
        final List<ScheduledTick<net.minecraft.world.level.block.Block>> blockTicks=new ArrayList<>();
        final List<ScheduledTick<net.minecraft.world.level.material.Fluid>> fluidTicks=new ArrayList<>();
        final Set<Long> forced = new HashSet<>();
        final Set<Entity> owned = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<LbcWands.ElectroTrace> traces = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<net.minecraft.stats.Stat<Identifier>, Integer> ownerStats = new LinkedHashMap<>(), otherStats = new LinkedHashMap<>();
        final int skill, trinket, trinketSlot;
        final int[] trinkets;
        int lastTick, warmTicks, phase;
        boolean retain;
        Zombie actor;
        Cow target, protectedTarget;
        Fixture(GameController game) {
            this.game = game; level = game.maps().level();
            owner = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && owner != null && other != null && game.allowDamage(other), "two active participants");
            require(game.items().transformator().activeCount() == 0, "exclusive fixture service");
            ownerState = new GameChecks.WandThirdPlayerState(owner, game.items().mana());
            otherState = new GameChecks.WandThirdPlayerState(other, game.items().mana());
            origin = new Vec3(Math.floor(owner.getX()/16)*16+8.5, 265.9, Math.floor(owner.getZ()/16)*16+8.5);
            base = BlockPos.containing(origin);
            bounds=new BoundingBox(base.getX()-4,base.getY()-3,base.getZ()-4,base.getX()+4,base.getY()+7,base.getZ()+12);
            highBounds=new BoundingBox(base.getX()-3,297,base.getZ()-3,base.getX()+3,303,base.getZ()+3);
            var profile = game.players().get(owner.getUUID());
            skill = profile.lbcSkill; trinket = profile.lbcTrinket; trinketSlot = profile.lbcTrinketSlot; trinkets = profile.trinkets.clone();
            for (var id : net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT) {
                var stat = net.minecraft.stats.Stats.CUSTOM.get(id);
                ownerStats.put(stat, owner.getStats().getValue(stat)); otherStats.put(stat, other.getStats().getValue(stat));
            }
        }
        void set(BlockPos p, net.minecraft.world.level.block.state.BlockState value) {
            var pos = p.immutable(); blocks.computeIfAbsent(pos, at -> game.maps().snapshot(level, at));
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container c) c.clearContent();
            level.setBlock(pos, value, 818);
        }
        void initialize() {
            for (int x=(base.getX()-32)>>4; x<=(base.getX()+32)>>4; x++)
                for (int z=(base.getZ()-32)>>4; z<=(base.getZ()+32)>>4; z++) {
                    long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                    if (!level.getForceLoadedChunks().contains(key)) { forced.add(key); level.setChunkForced(x,z,true); }
                    var chunk=level.getChunk(x,z);
                    ((LevelChunkTicks<net.minecraft.world.level.block.Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos()) || highBounds.isInside(t.pos())).forEach(blockTicks::add);
                    ((LevelChunkTicks<net.minecraft.world.level.material.Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos()) || highBounds.isInside(t.pos())).forEach(fluidTicks::add);
                }
            for (var p:BlockPos.betweenClosed(base.offset(-4,-3,-4),base.offset(4,7,12))) set(p,Blocks.AIR.defaultBlockState());
            for (var p:List.of(owner,other)) {
                p.closeContainer(); p.getInventory().clearContent(); p.removeAllEffects(); p.setGameMode(GameType.SURVIVAL); p.setNoGravity(true);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.setHealth(1000); p.setAbsorptionAmount(0);
                p.teleportTo(level,origin.x+24,origin.y+2,origin.z+24,Set.of(),0,0,true); p.setDeltaMovement(Vec3.ZERO);
            }
            var profile=game.players().get(owner.getUUID()); profile.lbcSkill=0; profile.lbcTrinket=0; Arrays.fill(profile.trinkets,0);
            lastTick=game.server().getTickCount();
        }
        boolean warm() {
            require(++warmTicks<=100,"bounded entity-section warmup");
            return level.getEntitiesOfClass(ServerPlayer.class,owner.getBoundingBox().inflate(1)).contains(owner)
                    && level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(base.getX()>>4,base.getZ()>>4));
        }
        void capture() {
            for (var v:game.items().transformator().snapshot()) { owned.add(v.actor()); owned.add(v.text()); }
            owned.addAll(game.items().transformator().attackSnapshot());
            for (var trace:game.items().wands().electroSnapshot()) if (trace.npcSource!=null && owned.contains(trace.npcSource)) traces.add(trace);
        }
        ItemEntity token(int count) {
            var t=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(LbcNpcDropItems.TRANSFORMATOR,count));
            t.setThrower(owner); t.setNoGravity(true); t.setDeltaMovement(Vec3.ZERO); t.setPickUpDelay(1000);
            require(level.addFreshEntity(t),"native token insertion"); owned.add(t); return t;
        }
        Zombie activate() {
            var prior=new HashSet<Zombie>();for(var view:game.items().transformator().snapshot())prior.add(view.actor());
            var t=token(2); int before=game.items().transformator().activeCount();
            require(game.items().transformator().ready(t) && game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"production dispatcher consumes one source unit");
            require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"one activation per native tick");
            t.discard(); capture(); require(game.items().transformator().activeCount()==before+1,"exactly one actor/text pair");
            return game.items().transformator().snapshot().stream().map(LbcTransformator.View::actor).filter(e->!prior.contains(e)).findFirst().orElseThrow();
        }
        void presentation() {
            for (boolean academy:List.of(false,true)) {
                var item=academy?LbcNpcDropItems.ACADEMY:LbcNpcDropItems.TRANSFORMATOR; var stack=new ItemStack(item);
                var flags=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(flags.keySet().equals(Set.of("drop",academy?"magic_academy":"transformator")) && flags.getIntOr("drop",0)==1
                        && flags.getIntOr(academy?"magic_academy":"transformator",0)==1,"exact source acquisition flags");
                require(Identifier.parse(academy?"lbc:drop_not_trap":"lbc:drop_trap").equals(stack.get(DataComponents.ITEM_MODEL))
                        && Identifier.parse(academy?"lbc:tier_q":"lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source token model and tier");
                require(!stack.getHoverName().getStyle().isItalic() && stack.getHoverName().getStyle().getColor().getValue()==0xffff55,"yellow nonitalic source name");
                var loot=game.lbc().rollTable(level,origin,owner,academy?"lbc:ported/magic_academy_drop":"lbc:ported/transformator_drop");
                require(loot.size()==1 && loot.getFirst().is(item) && loot.getFirst().getCount()==1
                        && flags.equals(loot.getFirst().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()),"public native leaf payload");
            }
            var fake=token(3); fake.makeFakeItem();
            require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount()==3 && game.items().transformator().activeCount()==0,"give visual never activates or debits"); fake.discard();
            actor=activate();
            var view=game.items().transformator().snapshot().getFirst();
            require(actor.getHealth()==500 && actor.getMaxHealth()==500 && actor.isNoAi() && actor.isSilent() && actor.isInvisible()
                    && actor.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue()==100,"native source actor health/AI/invisibility/resistance: hp="+actor.getHealth()+" max="+actor.getMaxHealth()+" noAI="+actor.isNoAi()+" silent="+actor.isSilent()+" invisible="+actor.isInvisible()+" kbr="+actor.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue());
            require(LbcTransformator.managed(actor) && LbcTransformator.managed(view.text()) && game.items().transformator().owner(actor)==owner,"pair identity and causal owner");
            require(actor.position().equals(new Vec3(base.getX()+.5,base.getY()-.1,base.getZ()+.5))
                    && Math.abs(view.text().getY()-actor.getY()-3)<1e-6,"source floor-centered spawn and own +3 text");
            require(actor.hasCustomName() && actor.isCustomNameVisible() && actor.getCustomName().getString().contains("Nightmare23 Birthday"),"complete source actor name");
            require(actor.getItemBySlot(EquipmentSlot.HEAD).is(Items.REPEATING_COMMAND_BLOCK)
                    && Identifier.parse("lbc:transformator").equals(actor.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.ITEM_MODEL))
                    && actor.getDropChances().byEquipment(EquipmentSlot.HEAD)==0,"real source helmet model and zero drop chance");
            var text=saved(view.text(),level);
            require(text.getStringOr("billboard","").equals("center") && text.getFloatOr("view_range",0)==2
                    && text.getBooleanOr("see_through",false) && (text.getByteOr("text_opacity",(byte)0)==-1),"native centered see-through source display range and opacity");
        }
        Cow cow(Vec3 at, boolean service) {
            var c=new Cow(EntityTypes.COW,level); c.setPos(at); c.setNoAi(true); c.setNoGravity(true);
            c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); c.setHealth(1000);
            if(service)c.entityTags().add("magic_academy");
            require(level.addFreshEntity(c),"native target insertion"); owned.add(c); return c;
        }
        List<LbcWands.ElectroTrace> burst(Zombie source) {
            var before=new HashSet<>(game.items().wands().electroSnapshot()); game.items().transformator().burst(source); capture();
            var out=game.items().wands().electroSnapshot().stream().filter(t->!before.contains(t)).toList();
            require(out.size()==7 && out.stream().allMatch(t->t.mechanism && !t.meditation && t.lifetime==30 && t.age==0 && t.npcSource==source
                    && t.owner==null && t.source()==source && t.level==level),"seven real shared Electro traces, source30 lifetime/mechanism/native cause");
            require(out.stream().allMatch(t->t.yaw==out.getFirst().yaw && t.pitch==out.getFirst().pitch),"all seven aim at the same nearest eligible target"); return out;
        }
        void drain() { for(int i=0;i<31;i++)game.items().wands().tick(); require(game.items().wands().electroSnapshot().stream().noneMatch(traces::contains),"source trace lifetime terminates through shared engine"); }
        void combat() {
            var arcs=burst(actor); var first=arcs.getFirst(); var position=first.position;
            game.items().timeStop().begin(level,200); int age=first.age;
            var view=game.items().transformator().snapshot().stream().filter(v->v.actor()==actor).findFirst().orElseThrow(); int active=view.activeTicks();
            for(int i=0;i<12;i++){game.items().transformator().tick();game.items().wands().tick();}
            require(first.age==age && first.position.equals(position) && game.items().transformator().snapshot().stream().filter(v->v.actor()==actor).findFirst().orElseThrow().activeTicks()==active,"Time Stop freezes actor cadence and shared native arcs");
            var paused=token(3); require(!game.items().specialDrops().spawn(paused) && paused.getItem().getCount()==3,"frozen acquisition preserves all source units");paused.discard();
            game.items().timeStop().end(level); game.items().wands().tick();
            require(target.getHealth()<1000 && protectedTarget.getHealth()==1000,"production shared segment damages eligible native target and protects nearer service role");
            require(target.getLastDamageSource()!=null && target.getLastDamageSource().getEntity()==actor
                    && LbcIndirectKills.playerSource(actor)==owner,"native NPC direct source retains the true thrower"); drain();
            var deaths=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(deaths::add);
            protectedTarget.entityTags().remove("magic_academy");protectedTarget.setHealth(.1f);
            burst(actor);game.items().wands().tick();
            require(!protectedTarget.isAlive() && protectedTarget.getLastDamageSource().getEntity()==actor
                    && protectedTarget.getKillCredit()==owner,"real lethal NPC trace credits the thrower before native death/loot");
            for(var e:level.getAllEntities())if(!deaths.contains(e) && bounds.isInside(e.blockPosition())
                    && (e instanceof ItemEntity || e instanceof ExperienceOrb))owned.add(e);
            drain();
            target.setHealth(1000); target.setInvulnerableTime(0);target.damageCooldownTime=0;
            for(var p:BlockPos.betweenClosed(base.offset(-2,0,1),base.offset(2,4,1)))set(p,Blocks.STONE.defaultBlockState());
            burst(actor);drain(); require(target.getHealth()==1000,"real shared Electro wall collision prevents damage behind solid terrain");
            for(var p:BlockPos.betweenClosed(base.offset(-2,0,1),base.offset(2,4,1)))set(p,Blocks.AIR.defaultBlockState());
            var beacon=base.west(3);set(beacon,LbcMagic.MECHANISM_BEACON_BLOCK.defaultBlockState());game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            int count=game.items().wands().electroSnapshot().size();game.items().transformator().burst(actor);
            require(game.items().wands().electroSnapshot().size()==count,"mechanism suppression blocks production burst");
            set(beacon,Blocks.AIR.defaultBlockState());game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            var reflected=burst(actor);var before=reflected.getFirst();float yaw=before.yaw,pitch=before.pitch;
            other.teleportTo(level,actor.getX()+4,actor.getY(),actor.getZ(),Set.of(),0,0,true);
            game.items().wands().reflectElectro(other);
            require(reflected.stream().allMatch(t->t.owner==other && t.source()==other && t.npcSource==actor && t.age==0)
                    && before.yaw==yaw+180 && before.pitch==-pitch,"production reflection transfers native author and preserves original NPC/age");
            // Reflection reverses pitch as well as yaw: put the target in the mirrored downward path.
            target.setPos(actor.position().add(0,-.5,-1.6));target.setHealth(1000);target.setInvulnerableTime(0);target.damageCooldownTime=0;
            game.items().wands().tick();
            require(target.getHealth()<1000 && target.getLastDamageSource().getEntity()==other && target.getKillCredit()==other,"actual reflected shared trace damage credits reflector");drain();
            set(beacon,LbcMagic.MAGIC_BEACON_BLOCK.defaultBlockState());game.items().magic().blockChanged(beacon,level.getBlockState(beacon));burst(actor);drain();
            set(beacon,Blocks.AIR.defaultBlockState());game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
        }
        void terminal() {
            var second=activate();second.setPos(actor.position().add(2,0,0));
            var secondView=game.items().transformator().snapshot().stream().filter(v->v.actor()==second).findFirst().orElseThrow();
            var firstView=game.items().transformator().snapshot().stream().filter(v->v.actor()==actor).findFirst().orElseThrow();
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
            // 400 deterministically enters both source thresholds, independent of random health-damage rolls.
            // Native fire needs support; an all-air sky fixture would extinguish every cell on placement.
            for(var p:BlockPos.betweenClosed(actor.blockPosition().offset(-1,-2,-1),actor.blockPosition().offset(1,-2,1)))set(p,Blocks.STONE.defaultBlockState());
            actor.setHealth(400);game.items().transformator().pulse(actor);capture();
            for(var e:level.getAllEntities())if(!before.contains(e) && bounds.isInside(e.blockPosition())
                    && (e instanceof ItemEntity || e instanceof ExperienceOrb))owned.add(e);
            require(!actor.hasEffect(net.minecraft.world.effect.MobEffects.INSTANT_HEALTH),"health rolls do not leave an Instant Health effect on the undead actor");
            require(blocks.keySet().stream().anyMatch(p->level.getBlockState(p).is(Blocks.FIRE)),"production low-health pass places real source fire");
            require(actor.isRemoved() && firstView.text().isRemoved() && !second.isRemoved() && !secondView.text().isRemoved()
                    && game.items().transformator().activeCount()==1,"terminal removes exactly its own actor/text, preserving a nearby concurrent pair");
            var balls=game.items().transformator().attackSnapshot().stream().filter(e->e instanceof LargeFireball).map(e->(LargeFireball)e).toList();
            require(balls.size()==1 && balls.getFirst().getDeltaMovement().equals(new Vec3(0,-10,0))
                    && balls.getFirst().getOwner()==null && LbcIndirectKills.playerSource(balls.getFirst())==owner
                    && saved(balls.getFirst(),level).getByteOr("ExplosionPower",(byte)-1)==12
                    && level.getBlockState(actor.blockPosition().below()).is(Blocks.DIRT),"real power12 downward terminal fireball, original NPC owner and dirt support");
            var ball=balls.getFirst();ball.setPos(actor.position().add(0,30,0));double y=ball.getY();ball.tick();
            require(!ball.isRemoved() && ball.getY()<y && LbcIndirectKills.playerSource(ball)==owner,"actual native fireball flies after terminal NPC removal and retains its author");
            var high=new BlockPos(base.getX(),300,base.getZ());
            for(var p:BlockPos.betweenClosed(high.offset(-3,-3,-3),high.offset(3,3,3)))set(p,Blocks.AIR.defaultBlockState());
            set(high,Blocks.TNT.defaultBlockState());
            // Small actual native blast exercises TNT's production wasExploded lineage hook without a power12 terrain footprint.
            level.explode(ball,high.getX()+.5,high.getY()+.5,high.getZ()+.5,1,net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            capture();
            var children=game.items().transformator().attackSnapshot().stream().filter(e->e instanceof net.minecraft.world.entity.item.PrimedTnt)
                    .map(e->(net.minecraft.world.entity.item.PrimedTnt)e).toList();
            require(children.size()==1,"actual native explosion primes one bounded TNT child");var child=children.getFirst();
            require(LbcTransformator.managed(child) && child.entityTags().contains(game.items().epoch())
                    && child.getOwner()==owner && LbcIndirectKills.playerSource(child)==owner,"actual TNT chain inherits managed epoch and native true author");
            int fuse=child.getFuse();game.items().timeStop().begin(level,200);Vec3 at=child.position();level.tickNonPassenger(child);
            require(child.getFuse()==fuse && child.position().equals(at),"Time Stop freezes native inherited TNT movement and fuse");
            game.items().timeStop().end(level);level.tickNonPassenger(child);require(!child.isRemoved() && child.getFuse()==fuse-1,"actual inherited TNT resumes its native fuse");
            child.discard();
            balls.forEach(Entity::discard);
        }
        boolean step() {
            if(game.server().getTickCount()==lastTick)return false;lastTick=game.server().getTickCount();
            if(phase==0) {if(!warm())return false;presentation();actor.setNoGravity(true);target=cow(actor.position().add(0,.5,1.6),false);protectedTarget=cow(actor.position().add(0,.5,.7),true);game.items().timeStop().begin(level,200);phase=1;return false;}
            if(phase==1){game.items().timeStop().end(level);combat();terminal();return true;}return false;
        }
        @Override public void close() {
            game.items().timeStop().end(level);capture();
            if(!retain) {for(var e:owned){if(e instanceof LivingEntity l)game.items().indirectKills().clear(l);e.discard();}game.items().transformator().tick();drain();}
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);level.getBlockTicks().clearArea(highBounds);level.getFluidTicks().clearArea(highBounds);
            for(var e:blocks.entrySet()){if(level.getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(level,e.getKey(),e.getValue());game.items().magic().blockChanged(e.getKey(),level.getBlockState(e.getKey()));}
            level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);level.getBlockTicks().clearArea(highBounds);level.getFluidTicks().clearArea(highBounds);blockTicks.forEach(level.getBlockTicks()::schedule);fluidTicks.forEach(level.getFluidTicks()::schedule);
            ownerState.restore(game);otherState.restore(game);game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            ownerStats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));otherStats.forEach((stat,value)->other.getStats().setValue(other,stat,value));
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=skill;profile.lbcTrinket=trinket;profile.lbcTrinketSlot=trinketSlot;System.arraycopy(trinkets,0,profile.trinkets,0,trinkets.length);
            if(!retain)for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game) {
        require(running==null && retained==null,"one bounded fixture");running=new Fixture(game);
        try{running.initialize();}catch(RuntimeException|Error error){try{running.close();}finally{running=null;}throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var f=running;
        try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS transformator-drops");}}
        catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL transformator-drops",error);}
    }
    public static void stopProbe(GameController game) {
        require(running==null && retained==null,"exclusive actual-stop fixture");var f=new Fixture(game);
        try{f.initialize();f.actor=f.activate();f.capture();stopActors=List.copyOf(f.owned);stopEpoch=game.items().epoch();f.retain=true;f.close();retained=f;
            game.items().timeStop().begin(f.level,200);stopToken=f.token(3);
            require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"frozen pending token retained before actual stop");
            SkyWars2.LOGGER.info("[check] PASS transformator-drops-stop-probe");}
        catch(RuntimeException|Error error){f.retain=false;f.close();throw error;}
    }
    public static void cleanup(GameController game) {
        require(retained!=null && stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && game.items().transformator().activeCount()==0
                && game.items().transformator().attackSnapshot().isEmpty() && stopActors.stream().allMatch(Entity::isRemoved),"actual stop quietly removes native actor, own text and attacks");
        require(stopToken!=null && stopToken.getItem().getCount()==3 && stopToken.entityTags().contains(stopEpoch)
                && !game.items().specialDrops().spawn(stopToken),"actual stop preserves pending source count and rejects stale replay");
        stopToken.discard();retained.retain=false;retained.close();retained=null;stopToken=null;stopEpoch=null;stopActors=List.of();SkyWars2.LOGGER.info("[check] PASS transformator-drops-cleanup");
    }
}
