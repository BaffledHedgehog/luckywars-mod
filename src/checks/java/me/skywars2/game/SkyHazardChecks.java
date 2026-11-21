package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Native altitude boundaries, scoped death credit, and one isolated Cosmos execution. */
public final class SkyHazardChecks {
    private SkyHazardChecks() { }
    private static String stopEpoch;
    private static ServerPlayer stopPlayer;
    private static boolean stopDrowning;
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("Sky hazards: " + message);
    }
    private static ServerPlayer participant(GameController game, String name) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name));
        require(game.playing() && player!=null && game.allowDamage(player),"actual fixture participant " + name);
        return player;
    }
    private static void ready(ServerPlayer player, double y) {
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent(); player.removeAllEffects();
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        player.setHealth(1000); player.setAbsorptionAmount(0);
        player.setInvulnerableTime(0); player.damageCooldownTime=0;
        player.setNoGravity(true); player.setPos(player.getX(),y,player.getZ());
        player.setDeltaMovement(Vec3.ZERO);
    }
    private static void pulse(GameController game, ServerPlayer player, double y, float health, int blasts) {
        ready(player,y);
        int before=game.items().exasperato().pendingBlasts();
        game.items().skyHazards().pulse(player);
        require(player.getHealth()==health,"native health at absolute Y="+y+" expected="+health+" actual="+player.getHealth()+" level="+player.level().dimension().identifier());
        require(game.items().exasperato().pendingBlasts()==before+blasts,"Cosmos queue at absolute Y="+y);
    }
    private static void displacementCredit(GameController game, ServerPlayer author, Vec3 position) {
        var level=author.level();
        var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
        cow.setPos(position); cow.setNoAi(true); cow.setNoGravity(true);
        var existing=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.minecraft.world.entity.Entity,Boolean>());
        level.getAllEntities().forEach(existing::add);
        var stat=net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS);
        int kills=author.getStats().getValue(stat);
        try {
            require(level.addFreshEntity(cow),"disposable native altitude-credit victim");
            game.items().indirectKills().displaced(cow,author);
            require(game.items().indirectKills().altitudeAuthor(cow)==author,"displacement retains causal altitude author");
            cow.setHealth(1); cow.setInvulnerableTime(0); cow.damageCooldownTime=0;
            game.items().indirectKills().altitudeDamage(cow,()->cow.hurtServer(level,cow.damageSources().drown(),4));
            require(cow.isDeadOrDying() && cow.getKillCredit()==author && cow.getLastDamageSource()!=null
                    && cow.getLastDamageSource().is(DamageTypes.DROWN)
                    && cow.getLastDamageSource().getEntity()==null,"native DROWN death credits causal displacement author");
            var aquatic=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
            aquatic.setPos(position); aquatic.setNoAi(true); aquatic.setNoGravity(true);
            require(level.addFreshEntity(aquatic),"ordinary drowning comparison victim");
            try {
                game.items().indirectKills().displaced(aquatic,author); aquatic.setHealth(1);
                aquatic.hurtServer(level,aquatic.damageSources().drown(),4);
                require(aquatic.isDeadOrDying() && aquatic.getKillCredit()!=author,"ordinary aquatic drowning does not inherit altitude context");
            } finally {game.items().indirectKills().clear(aquatic); aquatic.discard();}
        } finally {
            game.items().indirectKills().clear(cow); cow.discard();
            var created=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
            level.getAllEntities().forEach(entity->{if(!existing.contains(entity))created.add(entity);});
            created.forEach(net.minecraft.world.entity.Entity::discard);
            author.getStats().setValue(author,stat,kills);
        }
    }
    private static void cosmosExecution(GameController game, ServerPlayer author) {
        var level=author.level();
        // Reuse a ticking column: a freshly forced remote chunk has no query-visible entity sections yet.
        var at=new Vec3(Math.floor(author.getX()/16)*16+8.5,15000,
                Math.floor(author.getZ()/16)*16+8.5);
        var cell=net.minecraft.core.BlockPos.containing(at);
        int chunkX=cell.getX()>>4,chunkZ=cell.getZ()>>4;
        long chunk=net.minecraft.world.level.ChunkPos.pack(chunkX,chunkZ);
        boolean forced=level.getForceLoadedChunks().contains(chunk);
        record Placement(ServerPlayer player,net.minecraft.server.level.ServerLevel level,Vec3 position,
                         Vec3 velocity,float yaw,float pitch) { }
        var moved=new java.util.ArrayList<Placement>();
        var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
        var stat=net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS);
        int kills=author.getStats().getValue(stat);
        try {
            require(game.items().exasperato().pendingBlasts()==0,"empty queue before isolated Cosmos execution");
            for(var player:game.server().getPlayerList().getPlayers()) if(player.level()==level) {
                if(player.position().distanceToSqr(at)<500*500) {
                    moved.add(new Placement(player,player.level(),player.position(),player.getDeltaMovement(),player.getYRot(),player.getXRot()));
                    player.teleportTo(level,at.x+1024,player.getY(),at.z+1024,java.util.Set.of(),player.getYRot(),player.getXRot(),true);
                }
            }
            for(var entity:level.getAllEntities())
                require(entity.position().distanceToSqr(at)>200*200,"empty high-altitude blast area");
            level.setChunkForced(chunkX,chunkZ,true); level.getChunkAt(cell);
            // Keep the disposable victim in the preloaded column, fifty blocks above the blast.
            cow.setPos(at.add(0,50,0)); cow.setNoAi(true); cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000); cow.setHealth(10000);
            float before=cow.getHealth(); require(before==10000 && cow.getMaxHealth()==10000,"durable disposable Cosmos victim");
            require(level.addFreshEntity(cow),"native Cosmos victim admission");
            require(level.getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class,cow.getBoundingBox().inflate(1)).contains(cow),
                    "native Cosmos victim is query-visible before explosion");
            game.items().exasperato().queueCosmos(level,at,author,game.items().epoch());
            require(game.items().exasperato().pendingCosmos()==1,"one isolated Cosmos queued");
            game.items().exasperato().blastTick();
            var damage=cow.getLastDamageSource();
            require(game.items().exasperato().pendingCosmos()==0 && cow.isAlive()
                    && cow.getHealth()>0 && cow.getHealth()<before,"native Cosmos executes and damages surviving disposable victim: pending="
                            +game.items().exasperato().pendingCosmos()+" health="+cow.getHealth()+" before="+before);
            require(damage!=null && damage.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && damage.getEntity()==author
                    && damage.getDirectEntity() instanceof net.minecraft.world.entity.monster.Creeper,
                    "native Cosmos explosion retains transient creeper and causal player");
            var source=damage.getDirectEntity(); var name=source.getCustomName();
            require(name!=null && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().equals("cosmos") && level.getEntity(source.getId())==null
                    && level.getEntity(source.getUUID())==null,"named Cosmos source is never inserted as a persistent actor");
        } finally {
            game.items().exasperato().stop(); game.items().indirectKills().clear(cow); cow.discard();
            for(var placement:moved) {
                placement.player().teleportTo(placement.level(),placement.position().x,placement.position().y,placement.position().z,
                        java.util.Set.of(),placement.yaw(),placement.pitch(),true);
                placement.player().setDeltaMovement(placement.velocity());
            }
            author.getStats().setValue(author,stat,kills);
            if(!forced)level.setChunkForced(chunkX,chunkZ,false);
        }
    }
    public static void run(GameController game) {
        var player=participant(game,"TestBeta");
        var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var level=player.level();
        require(game.items().exasperato().pendingBlasts()==0 && game.items().exasperato().count()==0
                && game.items().exasperato().pendingSearches()==0 && game.items().exasperato().mushroomCount()==0,
                "exclusive native blast service");
        try {
            game.items().antigravity().stop();
            game.items().skyHazards().stop();
            require(game.items().antigravity().stormCount()==0,"no producer required");
            ready(player,1500);
            for(int i=0;i<9;i++)game.items().skyHazards().tick();
            require(player.getHealth()==1000,"global cadence waits nine service ticks");
            game.items().skyHazards().tick();
            require(player.getHealth()==996,"tenth tick reaches native global hazard");
            pulse(game,player,1499.999,1000,0);
            require(!player.entityTags().contains("drowning"),"below absolute lower threshold clears entry");
            pulse(game,player,1500,996,0);
            require(player.entityTags().contains("drowning") && player.getLastDamageSource()!=null
                    && player.getLastDamageSource().is(DamageTypes.DROWN)
                    && player.getLastDamageSource().getEntity()==null,"inclusive lower threshold uses native environmental drown");
            player.setInvulnerableTime(0); player.damageCooldownTime=0;
            game.items().skyHazards().pulse(player);
            require(player.getHealth()==992 && player.entityTags().contains("drowning"),"repeat pulse damages while retaining one entry tag");
            pulse(game,player,14999.99,996,0);
            pulse(game,player,15000,996,1);
            game.items().exasperato().stop();
            pulse(game,player,144990,996,1);
            game.items().exasperato().stop();
            pulse(game,player,144990.01,1000,0);
            require(!player.entityTags().contains("drowning"),"exclusive source-score upper bound clears entry");
            // Horizontal distance and a stopped producer cannot disable a global hazard.
            player.setPos(player.getX()+2048,1500,player.getZ()+2048);
            pulse(game,player,1500,996,0);
            game.items().antigravity().stop();
            pulse(game,player,1500,996,0);
            for (var role:java.util.List.of("spectator","strict_map_object","rcdenyalways","sauvojen_tuntija","magic_academy","transformator")) {
                ready(player,15000); player.entityTags().remove("drowning"); player.entityTags().add(role);
                try {
                    game.items().skyHazards().pulse(player);
                    require(player.getHealth()==1000 && game.items().exasperato().pendingBlasts()==0
                            && !player.entityTags().contains("drowning"),"protected role " + role);
                } finally {player.entityTags().remove(role);}
            }
            // Legacy exclusions name dimensions, not arbitrary entity tags with those names.
            for(var name:java.util.List.of("nexus","imprinted")) {
                player.entityTags().add(name);
                try { pulse(game,player,1500,996,0); }
                finally { player.entityTags().remove(name); }
            }
            ready(player,1500); player.setGameMode(GameType.SPECTATOR);
            game.items().skyHazards().pulse(player);
            require(player.getHealth()==1000 && !player.entityTags().contains("drowning"),"native spectator excluded");
            ready(player,1500); player.setGameMode(GameType.CREATIVE);
            game.items().skyHazards().pulse(player);
            require(player.getHealth()==1000,"creative protection remains native");
            ready(player,15000); player.entityTags().remove("drowning");
            game.items().timeStop().begin(level,200);
            game.items().skyHazards().pulse(player);
            require(player.getHealth()==1000 && game.items().exasperato().pendingBlasts()==0
                    && !player.entityTags().contains("drowning"),"dimension time stop pauses both hazards");
            game.items().timeStop().end(level);
            pulse(game,player,1500,996,0);
            pulse(game,player,1499,1000,0);
            pulse(game,player,1500,996,0);
            require(player.entityTags().contains("drowning"),"leaving and reentering renews entry");
            for(var other:game.server().getAllLevels()) if(other!=level) {
                player.teleportTo(other,saved.position.x,1500,saved.position.z,java.util.Set.of(),0,0,true);
                player.hasChangedDimension();
                player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                try {pulse(game,player,1500,996,0);}
                finally {
                    player.teleportTo(level,saved.position.x,saved.position.y,saved.position.z,java.util.Set.of(),saved.yaw,saved.pitch,true);
                    player.hasChangedDimension();
                    player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                }
                break;
            }
            displacementCredit(game,participant(game,"TestAlpha"),saved.position);
            cosmosExecution(game,participant(game,"TestAlpha"));
            SkyWars2.LOGGER.info("[check] PASS sky-hazards");
        } finally {
            game.items().timeStop().end(level); game.items().exasperato().stop();
            game.items().skyHazards().stop(); game.items().indirectKills().clear(player); saved.restore(game);
        }
    }
    public static void stopProbe(GameController game) {
        require(stopEpoch==null,"one stop probe at a time");
        var player=participant(game,"TestBeta");
        var saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        stopDrowning=player.entityTags().contains("drowning");
        try {
            ready(player,1500); game.items().skyHazards().pulse(player);
            require(player.entityTags().contains("drowning"),"live entry before actual stop");
        } finally {saved.restore(game);}
        player.entityTags().add("drowning");
        stopPlayer=player; stopEpoch=game.items().epoch();
        SkyWars2.LOGGER.info("[check] PASS sky-hazards-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(stopEpoch!=null && !game.items().currentEpoch(stopEpoch)
                && !stopPlayer.entityTags().contains("drowning"),"actual stop clears global altitude entry");
        if(stopDrowning)stopPlayer.entityTags().add("drowning");
        stopEpoch=null; stopPlayer=null;
        SkyWars2.LOGGER.info("[check] PASS sky-hazards-cleanup");
    }
}
