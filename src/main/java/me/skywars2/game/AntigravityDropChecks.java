package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native finite Paper-storm fixtures. Service-only stepping avoids accelerating real player physics. */
public final class AntigravityDropChecks {
    private AntigravityDropChecks() { }
    private static State running, stopping;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Display.BlockDisplay> stopDisplays = List.of();
    private static void require(boolean ok, String text) {
        if (!ok) throw new IllegalStateException("Antigravity drops: " + text);
    }
    private static CompoundTag saved(Entity entity, ServerLevel level) {
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(out);
        return out.buildResult();
    }
    private static ItemEntity token(ServerLevel level, Vec3 at, int count, ServerPlayer author) {
        var item = new ItemEntity(level, at.x, at.y, at.z,
                new ItemStack(LbcAntigravityDropItem.Kind.NORMAL.item(), count));
        item.setThrower(author);
        item.setNoGravity(true);
        item.setDeltaMovement(Vec3.ZERO);
        item.setPickUpDelay(1000);
        require(level.addFreshEntity(item), "native token admission");
        return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer alpha, beta;
        final GameChecks.WandThirdPlayerState alphaState, betaState;
        final Vec3 origin;
        final BlockPos center;
        final Map<BlockPos, MapManager.BlockSnapshot> blocks = new LinkedHashMap<>();
        final Set<Long> forced = new HashSet<>();
        final Set<Entity> existing = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ItemEntity> tokens = new ArrayList<>();
        final List<Cow> cows = new ArrayList<>();
        final Set<Entity> debris = Collections.newSetFromMap(new IdentityHashMap<>());
        final int alphaKills, betaKills;
        ItemEntity stack, paused, unloaded;
        Map<Display.BlockDisplay, Vec3> frozen;
        int phase, lastTick, wait;

        State(GameController game) {
            this.game = game;
            level = game.maps().level();
            alpha = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha != null && beta != null && game.allowDamage(beta), "actual participants");
            alphaState = new GameChecks.WandThirdPlayerState(alpha, game.items().mana());
            betaState = new GameChecks.WandThirdPlayerState(beta, game.items().mana());
            origin = new Vec3(Math.floor(alpha.getX()/16)*16+8.37,
                    Math.min(270.7, level.getMaxY()-40.3), Math.floor(alpha.getZ()/16)*16+8.81);
            center = BlockPos.containing(origin);
            alphaKills = kills(alpha); betaKills = kills(beta);
            level.getAllEntities().forEach(existing::add);
        }
        int kills(ServerPlayer player) {
            return player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));
        }
        void initialize() {
            require(game.items().antigravity().stormCount()==0 && game.items().antigravity().displayCount()==0,
                    "exclusive fixture service");
            for (int x=(center.getX()-18)>>4; x<=(center.getX()+18)>>4; x++)
                for (int z=(center.getZ()-18)>>4; z<=(center.getZ()+18)>>4; z++) {
                    long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                    if (!level.getForceLoadedChunks().contains(key)) {
                        forced.add(key); level.setChunkForced(x,z,true);
                    }
                    level.getChunkAt(new BlockPos(x*16,center.getY(),z*16));
                }
            // Includes the entire floor(random[-16,16))+[-1,1] footprint and update halo.
            for (var p : BlockPos.betweenClosed(center.offset(-18,-2,-18),center.offset(18,2,18))) {
                blocks.put(p.immutable(),game.maps().snapshot(level,p));
                set(p,Blocks.AIR.defaultBlockState());
            }
            for (var p : List.of(alpha,beta)) {
                p.closeContainer(); p.getInventory().clearContent(); p.removeAllEffects();
                p.setGameMode(GameType.SURVIVAL); p.setNoGravity(true);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.setHealth(1000);
                p.setAbsorptionAmount(0); p.setInvulnerableTime(0); p.damageCooldownTime=0;
            }
            place(alpha,10,0); place(beta,12,0);
            lastTick=game.server().getTickCount();
        }
        void set(BlockPos p, BlockState state) {
            if (level.getBlockEntity(p) instanceof net.minecraft.world.Container c) c.clearContent();
            level.setBlock(p,state,818);
        }
        void place(ServerPlayer p, double x, double y) {
            p.teleportTo(level,center.getX()+x,center.getY()+y,center.getZ()+.5,Set.of(),0,0,true);
            p.setDeltaMovement(Vec3.ZERO);
        }
        void reset() {
            game.items().antigravity().stop();
            tokens.forEach(Entity::discard);
            for (var c:cows) { game.items().indirectKills().clear(c); c.discard(); }
            cows.clear();
            for (var p:blocks.keySet()) set(p,Blocks.AIR.defaultBlockState());
            for (var p:List.of(alpha,beta)) {
                p.removeAllEffects(); p.setHealth(1000); p.setInvulnerableTime(0); p.damageCooldownTime=0;
                game.items().indirectKills().clear(p);
            }
            place(alpha,10,0); place(beta,12,0);
        }
        ItemEntity drop(int count) {
            var item=token(level,origin,count,alpha); tokens.add(item); return item;
        }
        void activate() {
            var item=drop(2); item.setTarget(beta.getUUID());
            require(game.items().antigravity().ready(item),"resident native admission");
            require(game.items().specialDrops().spawn(item) && item.getItem().getCount()==1,"shared one-unit debit");
            require(!game.items().specialDrops().spawn(item) && item.getItem().getCount()==1,"same-tick replay rejected");
            item.discard();
        }
        void ticks(int count) { for (int i=0;i<count;i++) game.items().antigravity().tick(); }
        Cow cow(Vec3 at) {
            var cow=new Cow(EntityTypes.COW,level); cow.setPos(at); cow.setNoAi(true); cow.setNoGravity(true);
            require(level.addFreshEntity(cow),"native disposable victim"); cows.add(cow); return cow;
        }
        void presentation() {
            var stack=new ItemStack(LbcAntigravityDropItem.Kind.NORMAL.item());
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr("antigravity",0)==1 && data.getIntOr("drop",0)==1,"exact source flags");
            require(Identifier.parse("lbc:drop_trap").equals(stack.get(DataComponents.ITEM_MODEL))
                    && Identifier.parse("lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source trap model and tier");
            var name=stack.getHoverName();
            require(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr
                    && tr.getKey().equals("drop_antigravity") && !name.getStyle().isItalic()
                    && name.getStyle().getColor()!=null && name.getStyle().getColor().getValue()==0xffff55,"yellow nonitalic source name");
            var lore=stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines();
            require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr
                    && tr.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic()
                    && lore.getFirst().getStyle().getColor()!=null && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"white nonitalic source lore");
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/antigravity_drop");
            require(loot.size()==1 && loot.getFirst().getCount()==1
                    && ItemStack.isSameItemSameComponents(stack,loot.getFirst()),"actual ready leaf exact token");
            for (var vanilla:List.of(Items.REPEATING_COMMAND_BLOCK,Items.BARRIER,Items.PAPER))
                require(!LbcAntigravityDropItem.matches(new ItemStack(vanilla)),"unflagged lookalike rejected");
        }
        void geometry() {
            reset();
            var expected=new HashMap<BlockPos,BlockState>();
            for (var p:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {
                var state=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                        p.getX()%2==0?Direction.Axis.X:Direction.Axis.Z);
                set(p,state); expected.put(p.immutable(),state);
            }
            activate();
            var displays=game.items().antigravity().snapshot();
            require(displays.size()==27 && game.items().antigravity().stormCount()==1,"initial complete 3-cube and one finite storm");
            var seen=new HashSet<BlockPos>();
            for (var d:displays) {
                var p=BlockPos.containing(d.getX()+.5,d.getY(),d.getZ()+.5);
                require(expected.containsKey(p) && seen.add(p) && d.getBlockState().equals(expected.get(p)),"exact native oriented state");
                require(d.position().distanceTo(new Vec3(p.getX()-.5,p.getY(),p.getZ()-.5))<1e-9
                        && d.getYRot()==0 && d.getXRot()==0 && level.getBlockState(p).isAir(),"floored source offset and actual terrain removal");
                var nbt=saved(d,level);
                require(nbt.getFloatOr("view_range",0)==128 && nbt.getIntOr("interpolation_duration",0)==1
                        && nbt.getIntOr("teleport_duration",0)==1 && nbt.getFloatOr("shadow_radius",-1)==0
                        && nbt.getFloatOr("shadow_strength",-1)==0 && !nbt.contains("brightness")
                        && nbt.getStringOr("billboard","").equals("fixed"),"source native display settings, normal light");
            }
            var first=new HashSet<>(displays);
            for(var p:BlockPos.betweenClosed(center.offset(-17,-1,-17),center.offset(16,1,16)))
                set(p,Blocks.STONE.defaultBlockState());
            ticks(9);
            require(game.items().antigravity().displayCount()==27,"no random cube before first ten-tick pulse");
            ticks(1);
            var pulse=game.items().antigravity().snapshot().stream().filter(d->!first.contains(d)).toList();
            require(pulse.size()==27,"first random pulse launches a complete native cube");
            int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
            var pulseCells=new HashSet<BlockPos>();
            for(var d:pulse) {
                var p=BlockPos.containing(d.getX()+.5,d.getY(),d.getZ()+.5);
                require(pulseCells.add(p) && p.getY()>=center.getY()-1 && p.getY()<=center.getY()+1
                        && d.getBlockState().is(Blocks.STONE) && level.getBlockState(p).isAir(),"pulse fixed source Y and real captured/cleared block");
                minX=Math.min(minX,p.getX());maxX=Math.max(maxX,p.getX());
                minZ=Math.min(minZ,p.getZ());maxZ=Math.max(maxZ,p.getZ());
            }
            require(maxX-minX==2 && maxZ-minZ==2 && minX>=center.getX()-17 && maxX<=center.getX()+16
                    && minZ>=center.getZ()-17 && maxZ<=center.getZ()+16,"continuous [-16,16) floored offsets produce one 3-cube within exact footprint");
            reset();
            set(center,Blocks.CHEST.defaultBlockState());
            var chest=(ChestBlockEntity)level.getBlockEntity(center);
            require(chest!=null,"native source chest"); chest.setItem(0,new ItemStack(Items.DIAMOND,7));
            set(center.above(),Blocks.WATER.defaultBlockState());
            set(center.below(),Blocks.SHORT_GRASS.defaultBlockState());
            activate();
            require(game.items().antigravity().displayCount()==1 && level.getBlockEntity(center)==null
                    && level.getBlockState(center).isAir() && level.getBlockState(center.above()).is(Blocks.WATER)
                    && level.getBlockState(center.below()).is(Blocks.SHORT_GRASS),"solid-only selection, native container clearing, nonsolids retained");
            for (var e:level.getAllEntities()) if (!existing.contains(e) && e instanceof ItemEntity item)
                require(!item.getItem().is(Items.DIAMOND) && !item.getItem().is(Items.CHEST),"source does not eject inventory or block rewards");
        }
        void motionAndCredit() {
            reset(); set(center,Blocks.STONE.defaultBlockState()); activate();
            var display=game.items().antigravity().snapshot().getFirst();
            var start=display.position();
            var victim=cow(start.add(0,.3,0)); victim.setDeltaMovement(.12,-.8,-.13);
            var fast=cow(start.add(.1,.3,.1)); fast.setDeltaMovement(.2,2,.3);
            var blocked=new ArrayList<Cow>();
            for (var tag:List.of("spectator","rcdenyalways","strict_map_object","sauvojen_tuntija","magic_academy","transformator")) {
                var c=cow(start.add(0,.3,0)); c.entityTags().add(tag); blocked.add(c);
            }
            ticks(1);
            require(display.position().distanceTo(start.add(0,.04,0))<1e-9
                    && victim.getDeltaMovement().distanceTo(new Vec3(.12,.24,-.13))<1e-9
                    && fast.getDeltaMovement().equals(new Vec3(.2,2,.3)),"first .04 acceleration, max lift and horizontal velocity preservation");
            require(blocked.stream().allMatch(c->c.getDeltaMovement().equals(Vec3.ZERO)),"display lift excludes service and observer roles");
            ticks(2);
            require(display.position().distanceTo(start.add(0,.24,0))<1e-9,"per-active-tick acceleration .04/.08/.12");
            victim.setHealth(1); victim.setInvulnerableTime(0); victim.damageCooldownTime=0;
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
            level.getAllEntities().forEach(before::add);
            victim.hurtServer(level,victim.damageSources().fellOutOfWorld(),1000);
            for(var e:level.getAllEntities())if(!before.contains(e))debris.add(e);
            require(victim.isDeadOrDying() && victim.getKillCredit()==alpha
                    && victim.getLastDamageSource().is(DamageTypes.FELL_OUT_OF_WORLD),"native lifted victim void death credits thrower rather than pickup target");
        }
        void pulsesAndDuration() {
            reset(); activate();
            var board=game.server().getScoreboard();
            var oldA=alpha.getTeam(); var oldB=beta.getTeam();
            require(board.getPlayerTeam("codex_antigravity_probe")==null,"fixture team available");
            var team=board.addPlayerTeam("codex_antigravity_probe"); team.setAllowFriendlyFire(false);
            try {
                board.addPlayerToTeam(alpha.getScoreboardName(),team); board.addPlayerToTeam(beta.getScoreboardName(),team);
                place(alpha,10,30); place(beta,12,30);
                alpha.setDeltaMovement(.1,-.1,.2); beta.setDeltaMovement(-.2,2,.1);
                var normal=cow(new Vec3(center.getX()+8,center.getY()+2,center.getZ()+.5));
                var outside=cow(new Vec3(center.getX()+19,center.getY()+2,center.getZ()+.5));
                var role=cow(normal.position()); role.entityTags().add("strict_map_object");
                ticks(9);
                require(alpha.getHealth()==1000 && beta.getHealth()==1000 && !normal.hasEffect(MobEffects.SLOW_FALLING),"no pulse before tick10");
                ticks(1);
                require(alpha.getHealth()==1000 && beta.getHealth()==1000 && alpha.getDeltaMovement().equals(new Vec3(.1,.55,.2))
                        && beta.getDeltaMovement().equals(new Vec3(-.2,2,.1)),"fixed-origin pulse lifts author and ally without local altitude damage");
                var effect=normal.getEffect(MobEffects.SLOW_FALLING);
                require(effect!=null && effect.getDuration()==30 && effect.getAmplifier()==0 && effect.isAmbient()
                        && !effect.isVisible() && !effect.showIcon() && normal.getDeltaMovement().y==.55
                        && !outside.hasEffect(MobEffects.SLOW_FALLING) && !role.hasEffect(MobEffects.SLOW_FALLING)
                        && role.getDeltaMovement().equals(Vec3.ZERO),"native pulse30 slowfall, fixed area and service exclusions");
                ticks(9); require(alpha.getHealth()==1000 && beta.getHealth()==1000,"local storm never applies altitude damage");
                ticks(1); require(alpha.getHealth()==1000 && beta.getHealth()==1000,"later pulse only lifts and grants slowfall");
                place(alpha,10,29.99); place(beta,12,29.99); ticks(10);
                require(alpha.getHealth()==1000 && beta.getHealth()==1000,"relative height does not control global altitude hazard");
                ticks(269); require(game.items().antigravity().stormCount()==1,"finite storm retained through active tick299");
                ticks(1); require(game.items().antigravity().stormCount()==0 && alpha.getHealth()==1000 && beta.getHealth()==1000,"exact30 pulses end at active tick300 without local damage");
                normal.removeAllEffects(); normal.setDeltaMovement(Vec3.ZERO); ticks(20);
                require(!normal.hasEffect(MobEffects.SLOW_FALLING) && normal.getDeltaMovement().equals(Vec3.ZERO),"no legacy perpetual marker or extra pulses");
            } finally {
                board.removePlayerTeam(team);
                if(oldA!=null)board.addPlayerToTeam(alpha.getScoreboardName(),oldA);
                if(oldB!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldB);
            }
        }
        void staleAndLifecycle() {
            reset(); set(center,Blocks.STONE.defaultBlockState()); activate();
            var d=game.items().antigravity().snapshot().getFirst();
            var start=d.position(); double speed=0,distance=0;
            for(int i=0;i<320;i++) {
                alpha.setPos(d.position().add(5,0,0));
                if(speed<3)speed+=.04; distance+=speed;
                ticks(1);
            }
            require(!d.isRemoved() && game.items().antigravity().stormCount()==0
                    && d.position().distanceTo(start.add(0,distance,0))<1e-7,
                    "accompanied display outlives finite storm/build height without extra TTL, follows source .04/<3 loop");
            place(alpha,200,0); place(beta,202,0);
            ticks(19); require(!d.isRemoved(),"nineteen unaccompanied ticks retain display");
            ticks(1); require(d.isRemoved() && game.items().antigravity().displayCount()==0,"twenty consecutive stale ticks remove display");
            reset(); set(center,Blocks.STONE.defaultBlockState()); activate();
            d=game.items().antigravity().snapshot().getFirst();
            game.items().antigravity().unloaded(d);
            require(!game.items().antigravity().snapshot().contains(d),"unload callback detaches tracking");
            game.items().antigravity().loaded(d);
            require(d.isRemoved() && game.items().antigravity().displayCount()==0,"unknown managed loaded display discarded quietly");
            var stale=new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY,level); stale.setPos(origin);
            stale.entityTags().add(LbcAntigravity.ACTOR); stale.entityTags().add("skywars2.items.stale-fixture");
            require(level.addFreshEntity(stale),"native stale display admission");
            game.items().antigravity().loaded(stale); require(stale.isRemoved(),"old epoch display cannot resume");
        }
        void guards() {
            reset();
            var stale=drop(2); stale.entityTags().add("skywars2.items.stale-fixture");
            require(!game.items().specialDrops().spawn(stale) && stale.getItem().getCount()==2,"stale token no debit"); stale.discard();
            var fake=drop(2); fake.makeFakeItem(); require(!game.items().specialDrops().spawn(fake),"give visual rejected"); fake.discard();
            var remote=drop(2);
            int x=(center.getX()>>4)+128,z=(center.getZ()>>4)+128;
            while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;
            remote.setPos(x*16+8,origin.y,z*16+8);
            require(!game.items().specialDrops().spawn(remote) && remote.getItem().getCount()==2
                    && level.getChunkSource().getChunkNow(x,z)==null,"unresident admission never loads chunks or debits"); remote.discard();
        }
        boolean step() {
            require(game.playing(),"actual match retained");
            int now=game.server().getTickCount(); if(now==lastTick)return false; lastTick=now;
            switch(phase) {
                case 0 -> {
                    // Wait for native entity-section admission after the fixture teleport.
                    if (!level.getEntities((Entity)null,new AABB(center).inflate(20),e->e==beta).contains(beta)) {
                        require(++wait<100,"native player-section warmup"); return false;
                    }
                    presentation(); guards(); geometry(); motionAndCredit(); pulsesAndDuration(); staleAndLifecycle();
                    reset(); set(center,Blocks.STONE.defaultBlockState()); stack=drop(2); phase++;
                }
                case 1 -> {
                    require(stack.getItem().getCount()==1 && game.items().antigravity().stormCount()==1,"actual first tick debit"); phase++;
                }
                case 2 -> {
                    require(stack.isRemoved() && game.items().antigravity().stormCount()==2,"actual next tick final unit debit");
                    frozen=new IdentityHashMap<>(); for(var d:game.items().antigravity().snapshot())frozen.put(d,d.position());
                    game.items().timeStop().begin(level,200); paused=drop(2); wait=0;
                    ticks(350);
                    require(game.items().antigravity().stormCount()==2,"dimension pause freezes storm age past nominal duration"); phase++;
                }
                case 3 -> {
                    require(paused.getItem().getCount()==2 && frozen.entrySet().stream().allMatch(e->
                            !e.getKey().isRemoved() && e.getKey().position().distanceTo(e.getValue())<1e-9),"pause freezes native displays and shared debit");
                    if(++wait>=3) {
                        paused.discard(); game.items().timeStop().end(level); reset();
                        unloaded=drop(2); game.items().specialDrops().unloaded(unloaded); phase++;
                    }
                }
                case 4 -> {
                    require(!unloaded.isRemoved() && unloaded.getItem().getCount()==2,"unloaded token cannot replay"); unloaded.discard(); return true;
                }
                default -> throw new IllegalStateException("Unknown antigravity phase "+phase);
            }
            return false;
        }
        public void close() { close(false); }
        void close(boolean keepDisplays) {
            game.items().timeStop().end(level);
            if(!keepDisplays)game.items().antigravity().stop();
            tokens.forEach(Entity::discard);
            debris.forEach(Entity::discard);
            for(var c:cows) {game.items().indirectKills().clear(c);c.discard();}
            for(var e:blocks.entrySet()) {
                if(level.getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();
                game.maps().restoreSnapshot(level,e.getKey(),e.getValue());
            }
            game.items().indirectKills().clear(alpha); game.items().indirectKills().clear(beta);
            alphaState.restore(game); betaState.restore(game);
            alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaKills);
            beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaKills);
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    private static void failure(State state, Throwable error) {
        try { state.close(); } catch(Throwable cleanup) {if(error!=cleanup)error.addSuppressed(cleanup);}
        finally {running=null;stopping=null;}
    }
    public static void run(GameController game) {
        require(running==null && stopping==null,"already running"); running=new State(game);
        try {running.initialize();} catch(RuntimeException|Error error){failure(running,error);throw error;}
    }
    public static void tick(GameController game) {
        if(stopping!=null) {
            var s=stopping; if(game.server().getTickCount()==s.lastTick)return;
            try {
                s.set(s.center,Blocks.STONE.defaultBlockState()); s.activate();
                stopDisplays=List.copyOf(game.items().antigravity().snapshot());
                require(stopDisplays.size()==1 && game.items().antigravity().stormCount()==1,"native storm/display before actual stop");
                stopEpoch=game.items().epoch(); s.close(true);
                game.items().timeStop().begin(s.level,200);
                stopToken=token(s.level,s.origin.add(0,4,0),3,s.alpha);
                require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"paused pending token before stop");
                stopping=null; SkyWars2.LOGGER.info("[check] PASS antigravity-drops-stop-probe");
            } catch(Throwable error) {failure(s,error);SkyWars2.LOGGER.error("[check] FAIL antigravity-drops-stop-probe",error);}
            return;
        }
        if(running==null)return;
        var s=running;
        try {if(s.step()){s.close();running=null;SkyWars2.LOGGER.info("[check] PASS antigravity-drops");}}
        catch(Throwable error){failure(s,error);SkyWars2.LOGGER.error("[check] FAIL antigravity-drops",error);}
    }
    public static void stopProbe(GameController game) {
        require(game.playing() && running==null && stopping==null,"actual stop probe"); stopping=new State(game);
        try {stopping.initialize();}catch(RuntimeException|Error error){failure(stopping,error);throw error;}
    }
    public static void cleanup(GameController game) {
        require(stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && stopToken!=null
                && stopToken.getItem().getCount()==3 && game.items().antigravity().stormCount()==0
                && game.items().antigravity().displayCount()==0 && stopDisplays.size()==1
                && stopDisplays.stream().allMatch(Entity::isRemoved),"actual stop clears finite storm and display quietly");
        require(!game.items().specialDrops().spawn(stopToken),"stopped token cannot replay");
        stopToken.discard();stopToken=null;stopEpoch=null;stopDisplays=List.of();
        SkyWars2.LOGGER.info("[check] PASS antigravity-drops-cleanup");
    }
}
