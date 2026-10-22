package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native fixtures; all actor mutations below belong exclusively to tests. */
public final class CreeperDropChecks {
    private CreeperDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Creeper> stopActors=List.of();
    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Creeper drops: "+message);
    }
    private static net.minecraft.nbt.CompoundTag saved(Entity entity,ServerLevel level) {
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        entity.saveWithoutId(output);return output.buildResult();
    }
    private static ItemEntity token(ServerLevel level,Vec3 at,ItemStack stack,net.minecraft.world.entity.LivingEntity author) {
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);
        if(author!=null)item.setThrower(author); // Before ENTITY_LOAD, as in real native player drops.
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native item admission");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game; final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final net.minecraft.world.scores.PlayerTeam alphaTeam,betaTeam;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();final List<ItemEntity> tokens=new ArrayList<>();
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final Vec3 origin;final boolean griefing,tntExplodes;final double armor;
        ItemEntity stack,paused,unloaded;Creeper frozen;Vec3 frozenPosition,frozenVelocity;
        int phase,lastTick,wait;float frozenSwell;long rewardSeed;
        State(GameController game) {
            this.game=game;level=game.maps().level();
            alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two actual match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());
            betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            var board=game.server().getScoreboard();alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());betaTeam=board.getPlayersTeam(beta.getScoreboardName());
            armor=beta.getAttribute(Attributes.ARMOR).getBaseValue();
            griefing=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
            tntExplodes=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES);
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);
            for(var entity:level.getAllEntities())existing.add(entity);
        }
        void initialize() {
            var board=game.server().getScoreboard();
            // Radius two and local-forward four fit inside this saved cube, including block entities.
            var center=BlockPos.containing(origin);
            for(var p:BlockPos.betweenClosed(center.offset(-10,-10,-10),center.offset(10,10,10))) {
                var pos=p.immutable();long key=net.minecraft.world.level.ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}
                level.getChunkAt(pos);blocks.put(pos,game.maps().snapshot(pos));
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();
                level.setBlock(pos,pos.getY()==center.getY()-2?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            }
            game.items().creeperDrops().stop();game.items().specialDrops().stop();
            board.removePlayerFromTeam(alpha.getScoreboardName());board.removePlayerFromTeam(beta.getScoreboardName());
            for(var player:List.of(alpha,beta)) {
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
            }
            beta.getAttribute(Attributes.ARMOR).setBaseValue(0);
            alpha.teleportTo(level,origin.x,origin.y+25,origin.z,Set.of(),0,0,true);
            beta.teleportTo(level,origin.x+25,origin.y,origin.z,Set.of(),0,0,true);
            for(var kind:LbcCreeperDropItem.Kind.values()) {
                var prototype=new ItemStack(kind.item());
                var data=prototype.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(data.size()==2 && data.getIntOr(kind.flag(),0)==1 && data.getIntOr("drop",0)==1,"exact source flags "+kind);
                require(LbcCreeperDropItem.matches(prototype,kind),"native recognition "+kind);
                var loot=game.lbc().rollTable(level,origin,alpha,kind==LbcCreeperDropItem.Kind.NORMAL?"lbc:ported/creeper_drop":"lbc:ported/creeper_drop_old");
                require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(prototype,loot.getFirst()),"complete native/ported equality "+kind);
            }
            rewardTable();game.command("give TestAlpha skywars2:creeper_drop 1");lastTick=game.server().getTickCount();
        }
        ItemEntity drop(LbcCreeperDropItem.Kind kind,int count,net.minecraft.world.entity.LivingEntity author) {
            var item=token(level,origin,new ItemStack(kind.item(),count),author);tokens.add(item);return item;
        }
        List<Creeper> live(){return game.items().creeperDrops().snapshot().stream().filter(c->!c.isRemoved()).toList();}
        void clear(){game.items().creeperDrops().stop();}
        List<Creeper> batch(LbcCreeperDropItem.Kind kind,net.minecraft.world.entity.LivingEntity author,boolean pickup) {
            clear();var item=drop(kind,1,author);if(pickup)item.setTarget(beta.getUUID());
            item.setYRot(37);item.setXRot(23);
            require(game.items().creeperDrops().spawn(item,kind),"native service accepts "+kind);
            var actors=live();int count=kind==LbcCreeperDropItem.Kind.NORMAL?20:16;
            require(actors.size()==count,"exact source actor count "+kind);
            double[][] offsets={{2,0},{-2,0},{0,2},{0,-2},{2,-1},{-1,2},{-2,-1},{-1,-2},{-2,-2},{2,2},
                    {2,0},{-2,0},{0,2},{0,-2},{2,1},{1,2},{-2,1},{1,-2},{-2,-2},{2,2}};
            var expected=new ArrayList<Vec3>();var radii=new ArrayList<Integer>();
            for(int i=0;i<count;i++) {
                expected.add(kind==LbcCreeperDropItem.Kind.NORMAL?origin.add(offsets[i][0],0,offsets[i][1]):
                        new net.minecraft.commands.arguments.coordinates.LocalCoordinates(0,0,4).apply(origin,new net.minecraft.world.phys.Vec2(23,37+i*20)));
                radii.add(i%2+1);
            }
            // snapshot order is deliberately unspecified; compare the full position/radius multiset immediately, before native falling.
            for(var c:actors) {
                var tag=saved(c,level);int radius=tag.getByteOr("ExplosionRadius",(byte)-1);int found=-1;
                for(int j=0;j<expected.size();j++)if(c.position().distanceTo(expected.get(j))<1e-6 && radius==radii.get(j)){found=j;break;}
                require(found>=0,"exact fractional source position/radius "+kind);expected.remove(found);radii.remove(found);
                require(!c.isNoAi() && !c.isNoGravity() && c.getHealth()==20 && c.getMaxHealth()==20,"native AI/gravity/default health before test freezing");
                require(tag.getShortOr("Fuse",(short)-1)==30 && !tag.getBooleanOr("powered",false) && !tag.getBooleanOr("ignited",false),"native fuse30/unpowered/unignited");
                require(kind==LbcCreeperDropItem.Kind.NORMAL?tag.getStringOr("DeathLootTable","").equals("lbc:creeper_wand"):!tag.contains("DeathLootTable"),"normal override / old vanilla death loot");
                require(game.items().creeperDrops().owner(c)==author,"actual thrower retained, pickup target never substitutes");
                c.setNoAi(true);c.setNoGravity(true);c.setDeltaMovement(Vec3.ZERO);
            }
            require(item.getYRot()==(kind==LbcCreeperDropItem.Kind.OLD?337:37) && item.getXRot()==23,"old fifteen rotations retain pitch; normal never rotates");
            item.discard();return actors;
        }
        void rewardTable() {
            try(var input=CreeperDropChecks.class.getResourceAsStream("/data/lbc/loot_table/creeper_wand.json")) {
                require(input!=null,"native reward table resource");
                var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var pools=json.getAsJsonArray("pools");require(pools.size()==1,"one reward pool");
                var pool=pools.get(0).getAsJsonObject();require(pool.get("rolls").getAsInt()==1,"one reward roll");
                var entries=pool.getAsJsonArray("entries");require(entries.size()==1,"one reward entry");
                var entry=entries.get(0).getAsJsonObject();var condition=entry.getAsJsonObject("condition");
                require(entry.get("value").getAsString().equals("lbc:ported/air_wand") && condition!=null && !entry.has("conditions"),"qualified native Air leaf and active singular condition; no killed-by-player gate");
                require(condition.get("type").getAsString().equals("minecraft:random_chance") && condition.get("chance").getAsDouble()==.05,"exact source independent five percent condition in native codec form");
            }catch(java.io.IOException e){throw new IllegalStateException(e);}
            var air=game.lbc().rollTable(level,origin,alpha,"lbc:ported/air_wand");
            require(air.size()==1 && air.getFirst().is(LbcWands.AIR_WAND),"native Air reward");
            var expected=air.getFirst();require(expected.get(DataComponents.LORE)!=null && expected.get(DataComponents.LORE).lines().size()==6,"all six source Air lore rows");
            int rewards=0;
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:creeper_wand")));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,alpha)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,alpha.damageSources().generic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            // Fixed seeds exercise the real ENTITY loot table without writing hundreds of entities.
            for(int i=1;i<=512;i++)for(var item:table.getRandomItems(params,i*0x9E3779B97F4A7C15L)) {
                rewards++;if(rewardSeed==0)rewardSeed=i*0x9E3779B97F4A7C15L;
                require(item.getCount()==1 && ItemStack.isSameItemSameComponents(item,expected),"complete reward components equal qualified native Air");
            }
            require(rewards>0 && rewards<80,"native random chance produces sparse rewards");
        }
        void combatReward() {
            var actors=batch(LbcCreeperDropItem.Kind.NORMAL,alpha,false);var actor=actors.getFirst();
            for(var other:actors)if(other!=actor)other.discard();
            var tag=saved(actor,level);tag.putLong("DeathLootTableSeed",rewardSeed);
            actor.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
            require(actor.getLootTableSeed()==rewardSeed,"deterministic native death loot seed");
            var before=new HashSet<Entity>();for(var entity:level.getAllEntities())before.add(entity);
            require(actor.hurtServer(level,actor.damageSources().generic(),1000) && actor.getHealth()==0,"ordinary nonplayer combat death uses native loot path");
            var expected=game.lbc().rollTable(level,origin,alpha,"lbc:ported/air_wand").getFirst();int rewards=0;
            for(var entity:level.getAllEntities())if(!before.contains(entity) && entity instanceof ItemEntity item) {
                require(ItemStack.isSameItemSameComponents(item.getItem(),expected) && item.getItem().getCount()==1,"native combat death produces complete qualified Air only");
                rewards++;item.discard();
            }
            require(rewards==1,"seeded five-percent success emits exactly one Air without player kill gate");clear();
        }
        float blast(boolean grief) {
            var actors=batch(LbcCreeperDropItem.Kind.NORMAL,alpha,true);var c=actors.stream().filter(actor->saved(actor,level).getByteOr("ExplosionRadius",(byte)0)==2).findFirst().orElseThrow();
            for(var other:actors)if(other!=c)other.discard();
            c.setPos(origin);c.setTarget(null);c.ignite();
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,grief,game.server());
            beta.teleportTo(level,origin.x+1,origin.y,origin.z,Set.of(),0,0,true);beta.setDeltaMovement(Vec3.ZERO);
            beta.setHealth(1000);beta.setAbsorptionAmount(0);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
            var block=BlockPos.containing(origin).west();level.setBlock(block,Blocks.DIRT.defaultBlockState(),3);
            var before=new HashSet<Entity>();for(var entity:level.getAllEntities())before.add(entity);
            for(int i=0;i<31 && !c.isRemoved();i++)c.tick();
            require(c.isRemoved(),"native ignition follows native fuse to explosion");
            require(grief?level.getBlockState(block).isAir():level.getBlockState(block).is(Blocks.DIRT),"native MOB_GRIEFING block policy "+grief);
            for(var entity:level.getAllEntities())if(!before.contains(entity) && entity instanceof ItemEntity item)
                require(!item.getItem().is(LbcWands.AIR_WAND),"native self explosion never rolls ordinary death reward");
            float loss=1000-beta.getHealth();
            if(loss>0)require(beta.getLastDamageSource()!=null && beta.getLastDamageSource().getDirectEntity()==c && beta.getLastDamageSource().getEntity()==alpha,"native creeper blast credits actual throwing player");
            return loss;
        }
        void consequentialChildren() {
            var actors=batch(LbcCreeperDropItem.Kind.NORMAL,alpha,false);
            var parent=actors.stream().filter(actor->saved(actor,level).getByteOr("ExplosionRadius",(byte)0)==2).findFirst().orElseThrow();
            for(var other:actors)if(other!=parent)other.discard();
            parent.setPos(origin);parent.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,6000));
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,true,game.server());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,true,game.server());
            var first=BlockPos.containing(origin).west();var second=BlockPos.containing(origin).north();
            level.setBlock(first,Blocks.TNT.defaultBlockState(),3);level.setBlock(second,Blocks.TNT.defaultBlockState(),3);
            beta.teleportTo(level,origin.x+25,origin.y,origin.z,Set.of(),0,0,true);
            var before=new HashSet<Entity>();for(var entity:level.getAllEntities())before.add(entity);
            parent.ignite();for(int i=0;i<31 && !parent.isRemoved();i++)parent.tick();
            var clouds=new ArrayList<net.minecraft.world.entity.AreaEffectCloud>();
            var children=new ArrayList<net.minecraft.world.entity.item.PrimedTnt>();
            for(var entity:level.getAllEntities())if(!before.contains(entity) && !entity.isRemoved()) {
                if(entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud)clouds.add(cloud);
                if(entity instanceof net.minecraft.world.entity.item.PrimedTnt child)children.add(child);
            }
            require(parent.isRemoved() && clouds.size()==1 && children.size()==2 && level.getBlockState(first).isAir() && level.getBlockState(second).isAir(),"actual native potion creeper creates one cloud and primes both TNT blocks");
            var cloud=clouds.getFirst();
            require(cloud.getOwner()==alpha && game.items().creeperDrops().owner(cloud)==alpha && LbcCreeperDrops.managed(cloud) && game.items().currentEpoch(cloud),"native lingering cloud retains actual thrower, managed marker and epoch");
            require(cloud.getRadius()==2.5f && cloud.getDuration()==300 && cloud.getRadiusOnUse()==-.5f,"native creeper cloud geometry/duration unchanged");
            for(var child:children)require(child.getOwner()==alpha && game.items().creeperDrops().owner(child)==alpha && LbcCreeperDrops.managed(child) && game.items().currentEpoch(child) && child.getFuse()>0 && child.getFuse()<80 && !child.isNoGravity(),"native chain TNT inherits actual thrower and epoch with native delayed fuse/gravity");
            var board=game.server().getScoreboard();require(board.getPlayerTeam("codex_creeper_child_probe")==null,"child fixture team available");
            var team=board.addPlayerTeam("codex_creeper_child_probe");
            try {
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                beta.teleportTo(level,origin.x+1,origin.y,origin.z,Set.of(),0,0,true);beta.setHealth(1000);beta.setAbsorptionAmount(0);beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
                var child=children.getFirst();child.setPos(origin);child.setNoGravity(true);child.setDeltaMovement(Vec3.ZERO);child.setFuse(1);child.tick();
                require(child.isRemoved() && beta.getHealth()==1000,"actual native chain TNT explosion respects throwing player's allied guard");
            }finally{board.removePlayerTeam(team);}
            var pending=children.get(1);require(!pending.isRemoved() && !cloud.isRemoved(),"delayed native consequences remain live before quiet stop");
            clear();require(pending.isRemoved() && cloud.isRemoved(),"quiet service stop removes native cloud and unexpired child TNT without effects");
        }
        boolean step() {
            require(game.playing(),"fixture preserves actual match");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase) {
                case 0 -> {require(alpha.getInventory().getItem(0).is(LbcCreeperDropItem.Kind.NORMAL.item()) && live().isEmpty(),"native give/fake pickup cannot activate");phase++;}
                case 1 -> {require(live().isEmpty(),"held token cannot activate");alpha.getInventory().clearContent();stack=drop(LbcCreeperDropItem.Kind.NORMAL,3,alpha);phase++;}
                case 2 -> {require(!stack.isRemoved() && stack.getItem().getCount()==2 && live().size()==20,"stack3 single tick debit");clear();phase++;}
                case 3 -> {require(!stack.isRemoved() && stack.getItem().getCount()==1 && live().size()==20,"stack2 next tick debit");clear();phase++;}
                case 4 -> {
                    require(stack.isRemoved() && live().size()==20,"last unit removes source token");clear();
                    batch(LbcCreeperDropItem.Kind.NORMAL,alpha,true);batch(LbcCreeperDropItem.Kind.OLD,null,true);clear();
                    var combined=drop(LbcCreeperDropItem.Kind.NORMAL,1,alpha);var data=combined.getItem().get(DataComponents.CUSTOM_DATA).copyTag();
                    data.putInt("creepers_old",1);combined.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(data));phase++;
                }
                case 5 -> {
                    require(live().size()==36,"both source flags execute and share one unit debit");clear();
                    unloaded=drop(LbcCreeperDropItem.Kind.OLD,1,alpha);game.items().specialDrops().unloaded(unloaded);phase++;
                }
                case 6 -> {
                    require(!unloaded.isRemoved() && live().isEmpty(),"pending unload cancels source activation");unloaded.discard();
                    var actors=batch(LbcCreeperDropItem.Kind.NORMAL,alpha,false);frozen=actors.getFirst();
                    frozen.setNoGravity(false);frozen.setDeltaMovement(.1,.2,.1);frozen.ignite();frozen.tick();
                    require(frozen.getSwelling(1)>0 && frozen.position().distanceTo(origin)>0,"native ticking advances fuse and physical movement");
                    frozenPosition=frozen.position();frozenVelocity=frozen.getDeltaMovement();frozenSwell=frozen.getSwelling(1);game.items().timeStop().begin(level,200);
                    paused=drop(LbcCreeperDropItem.Kind.OLD,2,alpha);wait=0;phase++;
                }
                case 7 -> {
                    require(!paused.isRemoved() && paused.getItem().getCount()==2,"time stop preserves pending stack");
                    require(!frozen.isRemoved() && frozen.getSwelling(1)==frozenSwell && frozen.position().distanceTo(frozenPosition)<1e-8,"time stop preserves native actor position and fuse progress");
                    if(++wait>=3){game.items().timeStop().end(level);require(frozen.getDeltaMovement().distanceTo(frozenVelocity)<1e-8,"time stop restores actor motion");paused.discard();clear();phase++;}
                }
                case 8 -> {
                    combatReward();require(blast(false)>0,"native creeper blast harms ordinary active participant");
                    for(String role:List.of("spectator","magic_academy","sauvojen_tuntija","strict_map_object")) {
                        boolean added=beta.entityTags().add(role);try{require(blast(false)==0,"native explosion excludes role "+role);}finally{if(added)beta.entityTags().remove(role);}
                    }
                    var board=game.server().getScoreboard();
                    require(board.getPlayerTeam("codex_creeper_drop_probe")==null,"fixture team available");
                    var team=board.addPlayerTeam("codex_creeper_drop_probe");
                    try {
                        board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                        require(blast(false)==0,"thrower attribution preserves allied blast protection");
                    }finally{board.removePlayerTeam(team);}
                    require(blast(true)>0,"native creeper remains damaging with griefing enabled");clear();consequentialChildren();phase++;
                }
                case 9 -> {
                    var departed=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
                    try {
                        departed.setPos(origin.add(0,25,0));departed.setNoGravity(true);require(level.addFreshEntity(departed),"departing native thrower");
                        var item=drop(LbcCreeperDropItem.Kind.NORMAL,1,departed);item.setTarget(beta.getUUID());departed.discard();
                        require(game.items().creeperDrops().spawn(item,LbcCreeperDropItem.Kind.NORMAL),"activation retains departed actual thrower");item.discard();
                        require(live().size()==20 && live().stream().allMatch(c->game.items().creeperDrops().owner(c)==departed),"captured author survives native ENTITY_LOAD and departure");
                    }finally{departed.discard();clear();}
                    var stale=drop(LbcCreeperDropItem.Kind.OLD,1,alpha);stale.entityTags().add("skywars2.items.stale-fixture");
                    require(!game.items().creeperDrops().spawn(stale,LbcCreeperDropItem.Kind.OLD),"stale epoch rejected");stale.discard();
                    var quiet=batch(LbcCreeperDropItem.Kind.OLD,alpha,false);clear();
                    require(quiet.stream().allMatch(c->c.isRemoved() && c.getHealth()==20),"quiet cleanup discards living actors without combat death");
                    game.items().specialDrops().stop();phase++;
                }
                case 10 -> {require(live().isEmpty(),"no delayed activation after dispatcher cleanup");return true;}
                default -> throw new IllegalStateException("Unknown Creeper phase "+phase);
            }
            return false;
        }
        public void close() {
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();clear();
            // Only fixture-created loose items/clouds inside the saved cube are reclaimed.
            var bounds=new net.minecraft.world.phys.AABB(origin,origin).inflate(10);
            for(var entity:level.getEntities((Entity)null,bounds))if(!existing.contains(entity) && !(entity instanceof ServerPlayer))entity.discard();
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,griefing,game.server());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,tntExplodes,game.server());
            beta.getAttribute(Attributes.ARMOR).setBaseValue(armor);var board=game.server().getScoreboard();
            if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);
            alphaState.restore(game);betaState.restore(game);
        }
    }
    public static void run(GameController game){
        require(running==null,"already running");running=new State(game);
        try{running.initialize();}
        catch(RuntimeException|Error error){try{running.close();}finally{running=null;}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS creeper-drops");}}
        catch(Throwable error){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL creeper-drops",error);}
    }
    public static void stopProbe(GameController game){
        require(game.playing(),"stop probe requires actual match");var level=game.maps().level();
        var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));game.items().timeStop().end(level);
        stopEpoch=game.items().epoch();stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),new ItemStack(LbcCreeperDropItem.Kind.NORMAL.item(),3),alpha);
        require(game.items().creeperDrops().spawn(stopToken,LbcCreeperDropItem.Kind.NORMAL),"live real stop-probe actors");stopActors=game.items().creeperDrops().snapshot();
        require(stopActors.size()==20,"live normal source count before real stop");for(var c:stopActors){c.setNoAi(true);c.setNoGravity(true);}
        game.items().timeStop().begin(level,200);SkyWars2.LOGGER.info("[check] PASS creeper-drops-stop-probe");
    }
    public static void cleanup(GameController game){
        require(!game.items().currentEpoch(stopEpoch) && game.items().creeperDrops().trackedCount()==0 && game.items().specialDrops().pendingCount()==0,"real stop clears epoch and both queues");
        require(stopActors.stream().allMatch(c->c.isRemoved() && c.getHealth()==20),"actual match stop quietly removes living creepers");
        if(stopToken!=null)stopToken.discard();stopToken=null;stopActors=List.of();stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS creeper-drops-cleanup");
    }
}
