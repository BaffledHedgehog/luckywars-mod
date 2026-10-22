package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Explicit bounded native fixtures; ordinary mode remains the only enabled mode. */
public final class TntDropChecks {
    private TntDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<PrimedTnt> stopTnt=List.of();
    private static void require(boolean condition,String message) {
        if(!condition) throw new IllegalStateException("TNT drops: "+message);
    }
    private static ItemEntity token(ServerLevel level,Vec3 at,int count) {
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(LbcTntDropItem.TOKEN,count));
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game; final ServerLevel level;
        final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final net.minecraft.world.scores.PlayerTeam alphaTeam,betaTeam;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>(); final List<ItemEntity> tokens=new ArrayList<>();
        final Vec3 origin; final int protection; final double armor;
        final boolean tntExplodes;
        ItemEntity stack,paused,unloaded; PrimedTnt frozen; Vec3 frozenVelocity,frozenPosition; int frozenFuse;
        int phase,lastTick,wait; float baseline;
        State(GameController game) {
            this.game=game;level=game.maps().level();
            alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two actual active match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());
            betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            var board=game.server().getScoreboard();alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());betaTeam=board.getPlayersTeam(beta.getScoreboardName());
            protection=game.config().tntProtection;armor=beta.getAttribute(Attributes.ARMOR).getBaseValue();
            tntExplodes=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,true,game.server());
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);
            // Power-four native blast cannot reach outside this saved, cleared volume.
            var center=BlockPos.containing(origin);
            for(var p:BlockPos.betweenClosed(center.offset(-10,-10,-10),center.offset(10,10,10))) {
                var pos=p.immutable();long key=net.minecraft.world.level.ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4);
                if(!level.getForceLoadedChunks().contains(key)) {forced.add(key);level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}
                level.getChunkAt(pos);blocks.put(pos,game.maps().snapshot(pos));
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            }
            game.items().tntDrops().stop();game.items().specialDrops().stop();
            board.removePlayerFromTeam(alpha.getScoreboardName());board.removePlayerFromTeam(beta.getScoreboardName());
            for(var player:List.of(alpha,beta)) {
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
            }
            beta.getAttribute(Attributes.ARMOR).setBaseValue(0);
            alpha.teleportTo(level,origin.x,origin.y+25,origin.z,Set.of(),0,0,true);
            beta.teleportTo(level,origin.x+25,origin.y,origin.z,Set.of(),0,0,true);
            var prototype=new ItemStack(LbcTntDropItem.TOKEN);
            var data=prototype.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr("explode",0)==1 && data.getIntOr("drop",0)==1,"exact source explode/drop flags");
            require(LbcTntDropItem.matches(prototype),"native token recognition");
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/tnt_drop");
            require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(prototype,loot.getFirst()),"ported loot equals complete native prototype");
            game.command("give TestAlpha skywars2:tnt_drop 1");lastTick=game.server().getTickCount();
        }
        ItemEntity drop(int count) {var result=token(level,origin,count);tokens.add(result);return result;}
        List<PrimedTnt> live() {return game.items().tntDrops().snapshot().stream().filter(t->!t.isRemoved()).toList();}
        void clear() {game.items().tntDrops().stop();}
        PrimedTnt batch(ServerPlayer thrower,ServerPlayer pickup) {
            clear();var item=drop(1);if(thrower!=null)item.setThrower(thrower);if(pickup!=null)item.setTarget(pickup.getUUID());
            require(game.items().tntDrops().spawn(item),"service accepts actual item entity");item.discard();
            var list=live();require(list.size()==13,"exact thirteen native TNT per source call");
            double[][] motion={{0,0},{0,.1},{0,-.1},{.1,0},{.1,.1},{.1,-.1},{-.1,0},{-.1,.1},{-.1,-.1},{.2,0},{-.2,0},{0,.2},{0,-.2}};
            var expectedMotion=new ArrayList<Vec3>();for(var v:motion)expectedMotion.add(new Vec3(v[0],1,v[1]));
            for(int i=0;i<13;i++) {
                var t=list.get(i);require(t.getFuse()==70 && !t.isNoGravity(),"native fuse70 and gravity before first tick");
                require(t.position().distanceTo(origin.add(0,.1,0))<1e-8,"actual fractional token position plus .1Y");
                require(expectedMotion.remove(t.getDeltaMovement()),"exact source velocity multiset "+i);
                require(t.getOwner()==thrower,"causal thrower; pickup recipient and nearest player never substitute");
            }
            for(int i=1;i<list.size();i++)list.get(i).discard();return list.getFirst();
        }
        float blast(int protection,ServerPlayer cause) {
            var t=batch(cause,beta);game.config().tntProtection=protection;
            beta.teleportTo(level,origin.x+1,origin.y,origin.z,Set.of(),0,0,true);beta.setDeltaMovement(Vec3.ZERO);
            beta.setHealth(1000);beta.setAbsorptionAmount(0);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
            t.setDeltaMovement(Vec3.ZERO);t.setNoGravity(true);t.setFuse(1);t.tick();
            require(t.isRemoved(),"native final fuse tick detonates and removes entity");
            float loss=1000-beta.getHealth();
            if(loss>0)require(beta.getLastDamageSource()!=null && beta.getLastDamageSource().getDirectEntity()==t
                    && beta.getLastDamageSource().getEntity()==cause,"real native explosion source retains causal owner");
            return loss;
        }
        net.minecraft.nbt.CompoundTag saved(Entity entity) {
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
            entity.saveWithoutId(output);return output.buildResult();
        }
        void roundTrip(PrimedTnt source,net.minecraft.world.entity.LivingEntity expected) {
            var tag=saved(source);
            require(expected==null ? !tag.contains("owner") : tag.contains("owner"),"native serialized owner presence matches causal payload");
            // Exercise native NBT decoding without replacing a live UUID in the world index.
            var restored=new PrimedTnt(level,origin.x,origin.y,origin.z,null);
            try {
                restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
                require(restored.getOwner()==expected && restored.getFuse()==source.getFuse()
                        && restored.getDeltaMovement().distanceTo(source.getDeltaMovement())<1e-8
                        && LbcTntDrops.managed(restored) && game.items().currentEpoch(restored),"native ownerless/reflected save-load retains owner, fuse, velocity and epoch");
            } finally {restored.discard();}
        }
        void causalRegressions() {
            clear();roundTrip(batch(null,beta),null);
            var reflected=batch(alpha,beta);game.items().electro().assignOwner(reflected,beta);roundTrip(reflected,beta);clear();
            var departed=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
            var input=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(LbcTntDropItem.TOKEN));
            tokens.add(input);
            try {
                departed.setPos(origin.add(0,25,0));departed.setNoGravity(true);
                require(level.addFreshEntity(departed),"real nonparticipant causal fixture admitted");
                input.setThrower(departed);input.setTarget(beta.getUUID());input.setNoGravity(true);input.setPickUpDelay(1000);
                require(level.addFreshEntity(input),"thrower assigned before native ENTITY_LOAD capture");
                departed.discard();
                require(game.items().tntDrops().spawn(input),"deferred activation survives departed actual thrower");input.discard();
                require(live().size()==13 && live().stream().allMatch(t->t.getOwner()==departed),"captured actual departed author retained for all thirteen native TNT");
            } finally {input.discard();departed.discard();clear();}
            var afterLaunch=batch(alpha,beta);
            try {
                game.spectators().enter(alpha);
                beta.teleportTo(level,origin.x+1,origin.y,origin.z,Set.of(),0,0,true);
                beta.setHealth(1000);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;game.config().tntProtection=0;
                afterLaunch.setNoGravity(true);afterLaunch.setDeltaMovement(Vec3.ZERO);afterLaunch.setFuse(1);afterLaunch.tick();
                require(afterLaunch.isRemoved() && beta.getHealth()<1000 && beta.getLastDamageSource()!=null
                        && beta.getLastDamageSource().getEntity()==alpha,"already launched native TNT still damages and credits author after observer transition");
            } finally {
                game.spectators().remove(alpha);alpha.setGameMode(GameType.SURVIVAL);alpha.setNoGravity(true);
                alpha.teleportTo(level,origin.x,origin.y+25,origin.z,Set.of(),0,0,true);clear();
            }
            var parent=batch(alpha,beta);var chainPos=BlockPos.containing(origin).east();
            // One native block inside the existing saved volume exercises the actual explosion callback.
            level.setBlock(chainPos,Blocks.TNT.defaultBlockState(),3);
            parent.setNoGravity(true);parent.setDeltaMovement(Vec3.ZERO);parent.setFuse(1);parent.tick();
            var children=live();
            require(parent.isRemoved() && level.getBlockState(chainPos).isAir() && children.size()==1,"actual native blast primes exactly one adjacent TNT block");
            var child=children.getFirst();
            require(child.getOwner()==alpha && LbcTntDrops.managed(child) && game.items().currentEpoch(child)
                    && child.getFuse()>0 && child.getFuse()<80 && !child.isNoGravity(),"native chain inherits real owner, managed marker and epoch while retaining native delayed fuse/gravity");
            game.items().electro().assignOwner(child,beta);roundTrip(child,beta);
            clear();require(child.isRemoved() && live().isEmpty(),"quiet stop removes native chain child before its delayed blast");
        }
        void causalMerges() {
            var a=drop(2);var b=drop(2);a.setThrower(alpha);b.setThrower(beta);
            // Execute native item physics/merge attempts without advancing the dispatcher.
            for(int i=0;i<80;i++){a.tick();b.tick();}
            require(!a.isRemoved() && !b.isRemoved() && a.getItem().getCount()==2 && b.getItem().getCount()==2,
                    "native merge attempts retain separate TNT trap authors");
            b.setThrower(alpha);
            for(int i=0;i<80 && !a.isRemoved() && !b.isRemoved();i++){a.tick();b.tick();}
            require(a.isRemoved()!=b.isRemoved() && (a.isRemoved()?b:a).getItem().getCount()==4,
                    "same-author TNT tokens still use native stack merging");
            a.discard();b.discard();
        }
        boolean step() {
            require(game.playing(),"fixture never ends match");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase) {
                case 0 -> {
                    require(alpha.getInventory().getItem(0).is(LbcTntDropItem.TOKEN) && live().isEmpty(),"deferred give payload; fake pickup visual cannot activate");phase++;
                }
                case 1 -> {require(live().isEmpty(),"held token cannot activate");alpha.getInventory().clearContent();stack=drop(3);phase++;}
                case 2 -> {require(!stack.isRemoved() && stack.getItem().getCount()==2 && live().size()==13,"stack3 first tick debits one");clear();phase++;}
                case 3 -> {require(!stack.isRemoved() && stack.getItem().getCount()==1 && live().size()==13,"stack2 next tick debits one");clear();phase++;}
                case 4 -> {
                    require(stack.isRemoved() && live().size()==13,"last unit removes token");clear();
                    var owned=batch(alpha,beta);owned.tick();require(owned.getFuse()==69 && owned.getY()>origin.y+.1 && owned.getDeltaMovement().y<1,"ordinary native upward motion, gravity and fuse advance");
                    batch(null,beta);clear();drop(1);drop(1);phase++;
                }
                case 5 -> {
                    require(live().size()==26,"overlapping distinct tokens each activate once");clear();
                    unloaded=drop(1);game.items().specialDrops().unloaded(unloaded);phase++;
                }
                case 6 -> {
                    require(!unloaded.isRemoved() && live().isEmpty(),"unloaded pending token cannot activate");unloaded.discard();
                    frozen=batch(alpha,beta);frozenFuse=frozen.getFuse();frozenVelocity=frozen.getDeltaMovement();frozenPosition=frozen.position();
                    game.items().timeStop().begin(level,200);paused=drop(2);wait=0;phase++;
                }
                case 7 -> {
                    require(!paused.isRemoved() && paused.getItem().getCount()==2,"time stop holds source debit");
                    require(frozen.getFuse()==frozenFuse && frozen.position().distanceTo(frozenPosition)<1e-8,"time stop preserves native fuse and position");
                    if(++wait>=3){game.items().timeStop().end(level);require(frozen.getDeltaMovement().distanceTo(frozenVelocity)<1e-8,"time stop restores exact native velocity");paused.discard();clear();phase++;}
                }
                case 8 -> {
                    baseline=blast(0,alpha);require(baseline>0,"unprotected actual native TNT damages active player");
                    for(int protection=1;protection<=5;protection++)require(Math.abs(blast(protection,alpha)-baseline*(1-protection/5f))<.03f,"native TNT protection level "+protection);
                    require(blast(0,null)>0 && beta.getLastDamageSource().getEntity()==null,"ownerless native final explosion remains ownerless");
                    for(String role:List.of("spectator","magic_academy","sauvojen_tuntija","strict_map_object")) {
                        boolean added=beta.entityTags().add(role);
                        try {require(blast(0,alpha)==0,"native explosion excludes role "+role);}
                        finally {if(added)beta.entityTags().remove(role);}
                    }
                    var board=game.server().getScoreboard();
                    require(board.getPlayerTeam("codex_tnt_drop_probe")==null,"isolated team name available");
                    var team=board.addPlayerTeam("codex_tnt_drop_probe");
                    try {
                        board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                        require(blast(0,alpha)==0,"actual native explosion respects allied participant guard");
                    } finally {board.removePlayerTeam(team);}
                    clear();var t=batch(alpha,beta);
                    beta.teleportTo(level,origin.x+1,origin.y,origin.z,Set.of(),0,0,true);
                    beta.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(LbcElectro.SHIELD));
                    beta.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                    var before=t.getDeltaMovement();game.items().electro().tick();beta.stopUsingItem();
                    require(t.getOwner()==beta && t.getDeltaMovement().distanceTo(before.reverse())<1e-8,"actual Electro shield reverses native TNT and replaces thrower");clear();phase++;
                }
                case 9 -> {
                    causalMerges();
                    causalRegressions();
                    var stale=drop(1);stale.entityTags().add("skywars2.items.stale-fixture");
                    require(!game.items().tntDrops().spawn(stale),"old epoch rejected before native TNT creation");stale.discard();
                    game.items().specialDrops().stop();clear();phase++;
                }
                case 10 -> {require(live().isEmpty(),"dispatcher stop leaves no delayed activation");return true;}
                default -> throw new IllegalStateException("Unknown phase "+phase);
            }
            return false;
        }
        public void close() {
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();clear();
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            game.config().tntProtection=protection;level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,tntExplodes,game.server());
            beta.getAttribute(Attributes.ARMOR).setBaseValue(armor);var board=game.server().getScoreboard();
            if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);
            alphaState.restore(game);betaState.restore(game);
        }
    }
    public static void run(GameController game) {require(running==null,"already running");running=new State(game);}
    public static void tick(GameController game) {
        if(running==null)return;
        try {if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS tnt-drops");}}
        catch(Throwable error){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL tnt-drops",error);}
    }
    public static void stopProbe(GameController game) {
        require(game.playing(),"stop probe requires actual match");var level=game.maps().level();
        var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        game.items().timeStop().end(level);
        stopEpoch=game.items().epoch();stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),3);
        require(game.items().tntDrops().spawn(stopToken),"live native stop-probe batch");stopTnt=game.items().tntDrops().snapshot();
        game.items().timeStop().begin(level,200);
        require(stopTnt.size()==13 && game.items().specialDrops().pendingCount()>0,"live TNT and pending token before real stop");
        SkyWars2.LOGGER.info("[check] PASS tnt-drops-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(!game.items().currentEpoch(stopEpoch) && game.items().tntDrops().trackedCount()==0 && game.items().specialDrops().pendingCount()==0,"real stop invalidates epoch and clears both services");
        require(stopTnt.stream().allMatch(Entity::isRemoved),"native TNT removed quietly before delayed blasts");
        if(stopToken!=null)stopToken.discard();stopToken=null;stopTnt=List.of();stopEpoch=null;
        SkyWars2.LOGGER.info("[check] PASS tnt-drops-cleanup");
    }
}
