package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.academy.SauvojenMeditation;
import me.skywars2.game.academy.TrialWarningVisual;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.terrain.EffectTerrain;
import me.skywars2.mixin.combat.CombatCheckpointAccess;
import me.skywars2.validation.mixin.FixtureAccess;
import me.skywars2.validation.mixin.GameCheckAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import me.skywars2.game.entities.mobs.LbcPsycho;
import me.skywars2.game.entities.projectiles.LbcDanmaku;

/** Synchronous native trial checks. Terrain is snapshotted and restored, including explicit template air. */
public final class SauvojenMeditationChecks {
    private SauvojenMeditationChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Sauvojen meditation: "+message);}
    /** Per-participant entries only: teardown cannot erase another actor's ambient attribution. */
    static final class Attribution implements AutoCloseable {
        private record Row(Map<? extends LivingEntity, ?> map, Map<LivingEntity, Object> values) { }
        private final List<Row> saved = new ArrayList<>();
        private final List<? extends LivingEntity> targets;
        private final Map<LivingEntity, CombatLinks> combat = new IdentityHashMap<>();

        Attribution(GameController game, List<? extends LivingEntity> targets) {
            this.targets = List.copyOf(targets);
            for (var map : fields(game.items().indirectKills()).values()) {
                var values = new IdentityHashMap<LivingEntity, Object>();
                for (var target : this.targets) {
                    if (!map.containsKey(target)) continue;
                    var value = map.get(target);
                    values.put(target, value instanceof Map<?, ?> nested ? new HashMap<>(nested) : value);
                }
                saved.add(new Row(map, values));
            }
            for (var target : this.targets) combat.put(target, CombatLinks.capture(target));
        }

        static Map<String, Map<? extends LivingEntity, ?>> fields(LbcIndirectKills ledger) {
            var state = (FixtureAccess.Attribution) (Object) ledger;
            return Map.ofEntries(
                    Map.entry("logoutAbsorption", state.validation$logoutAbsorption()),
                    Map.entry("logoutAttacks", state.validation$logoutAttacks()),
                    Map.entry("lootOwners", state.validation$lootOwners()),
                    Map.entry("falls", state.validation$falls()),
                    Map.entry("statuses", state.validation$statuses()),
                    Map.entry("pendingMana", state.validation$pendingMana()),
                    Map.entry("manaDrains", state.validation$manaDrains()),
                    Map.entry("manaDebts", state.validation$manaDebts()),
                    Map.entry("hungerDebts", state.validation$hungerDebts()),
                    Map.entry("removedSupports", state.validation$removedSupports()),
                    Map.entry("laterAttacks", state.validation$laterAttacks()),
                    Map.entry("burning", state.validation$burning()),
                    Map.entry("arrivals", state.validation$arrivals()));
        }

        private static Object rebound(Object value, LivingEntity target) {
            if (value instanceof Map<?, ?> nested) {
                var result = new HashMap<Object, Object>();
                nested.forEach((key, entry) -> result.put(key, rebound(entry, target)));
                return result;
            }
            if (value instanceof LbcIndirectKills.Status status) {
                var previous = status.effect();
                var current = target.getEffect(previous.getEffect());
                return new LbcIndirectKills.Status(status.owner(), current == null ? previous : current);
            }
            return value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void close() {
            for (var row : saved) {
                // Each row is written back to the same map and with the same participant keys.
                var map = (Map<LivingEntity, Object>) (Map<?, ?>) row.map();
                for (var target : targets) {
                    if (row.values().containsKey(target)) map.put(target, rebound(row.values().get(target), target));
                    else map.remove(target);
                }
            }
            combat.forEach((target, links) -> links.restore(target));
        }
    }

    record CombatLinks(EntityReference<LivingEntity> mob, int mobTime, EntityReference<Player> player,
                       int playerMemory, LivingEntity attacked, int attackTime) {
        static CombatLinks capture(LivingEntity target) {
            var living = (CombatCheckpointAccess.Living) target;
            var checks = (GameCheckAccess.CombatLinks) target;
            return new CombatLinks(living.skywars2$mob(), checks.validation$mobTime(), living.skywars2$player(),
                    living.skywars2$memory(), checks.validation$attacked(), checks.validation$attackTime());
        }

        void restore(LivingEntity target) {
            var living = (CombatCheckpointAccess.Living) target;
            living.skywars2$mob(mob);
            living.skywars2$mobTime(mobTime);
            living.skywars2$player(player);
            living.skywars2$memory(playerMemory);
            living.skywars2$attacked(attacked);
            living.skywars2$attackTime(attackTime);
        }
    }
    private static final class Recording extends net.minecraft.network.Connection {
        final net.minecraft.server.level.ServerPlayer player;
        final List<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket> particles=new ArrayList<>();
        final List<net.minecraft.network.protocol.game.ClientboundSoundPacket> sounds=new ArrayList<>();
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        Recording(net.minecraft.server.level.ServerPlayer player){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public net.minecraft.network.PacketListener getPacketListener(){return player.connection;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){
            if(packet instanceof net.minecraft.network.protocol.game.ClientboundBundlePacket bundle){bundle.subPackets().forEach(p->send(p,null,false));return;}
            packets.add(packet);
            if(packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket particle)particles.add(particle);
            else if(packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound)sounds.add(sound);
        }
        @Override public void flushChannel(){}
    }
    private static final class Swap implements AutoCloseable {
        final net.minecraft.server.level.ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous;final Recording recording;
        Swap(GameController game,net.minecraft.server.level.ServerPlayer player){this.player=player;previous=player.connection;recording=new Recording(player);
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),recording,player,
                    new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));}
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final SauvojenMeditation service;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final List<Entity> spawned=new ArrayList<>();
        final Set<net.minecraft.world.level.ChunkPos> forced=new LinkedHashSet<>();
        final int savedCounter,savedFear;
        final Attribution attribution;
        Fixture(GameController game){this.game=game;service=game.items().sauvojen().meditation();
            require(service.count()==0,"exclusive actual meditation lifecycle service");base=new ElementalGrimoireChecks.Fixture(game);
            savedCounter=base.mana.score(base.owner,"sauvojen_wand_upgrade").get();savedFear=base.mana.score(base.owner,"scared").get();
            attribution=new Attribution(game,List.of(base.owner));base.initialize();
            try{warmFootprint();}catch(RuntimeException|Error error){close();throw error;}}
        void warmFootprint(){
            var center=base.owner.blockPosition();var level=base.owner.level();
            // The full source121x121 lava plane and native neighbour guards require these chunks.
            // These are test-only force tickets. Production admission still never acquires terrain.
            for(int x=(center.getX()-61)>>4;x<=(center.getX()+61)>>4;x++)
                for(int z=(center.getZ()-61)>>4;z<=(center.getZ()+61)>>4;z++){
                    if(level.setChunkForced(x,z,true))forced.add(new net.minecraft.world.level.ChunkPos(x,z));
                    level.getChunkAt(new BlockPos(x*16,center.getY(),z*16));
                    require(level.getChunkSource().getChunkNow(x,z)!=null,"native warmed full meditation chunk "+x+","+z);
                }
        }
        ItemStack held(){
            var loot=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/sauvojen_wand");
            require(loot.size()==1,"ordinary source loot");var stack=loot.getFirst();
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("upgradable",1);tag.putInt("upgraded",0);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));base.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            base.owner.setShiftKeyDown(true);return stack;
        }
        SauvojenMeditation.Trial admit(){service.stop();var stack=held();require(service.start(base.owner,InteractionHand.MAIN_HAND,stack),"native mainhand admission");return service.trial(base.owner);}
        void save(BlockPos lo,BlockPos hi){
            for(var p:BlockPos.betweenClosed(lo,hi)){
                require(base.owner.level().getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null,"resident full footprint "+p);
                var cell=p.immutable();terrain.computeIfAbsent(cell,key->game.maps().snapshot(base.owner.level(),key));
            }
        }
        void restore(){terrain.forEach((p,state)->game.maps().restoreSnapshot(base.owner.level(),p,state));terrain.clear();}
        boolean complete(SauvojenMeditation.Trial t){
            long started=System.nanoTime();int turns=0;
            do{EffectTerrain.beginTick();if(service.phase(t)){
                SkyWars2.LOGGER.info("[check] Sauvojen terrain phase{} turns{} totalCPUms{} maxTurnCPUms{}",t.remaining,turns+1,
                        (System.nanoTime()-started)/1_000_000d,t.maxTerrainNanos/1_000_000d);return true;
            }}while(++turns<128);return false;
        }
        void admission(){
            var t=admit();require(service.remaining(base.owner)==620,"620 initial countdown");
            require(!service.start(base.owner,InteractionHand.OFF_HAND,t.stack),"offhand cannot admit");
            require(service.start(base.owner,InteractionHand.MAIN_HAND,t.stack)&&service.count()==1,"held repeat is idempotent");
            // A component-identical replacement is a different participating stack.
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,t.stack.copy());service.tick();require(service.count()==0,"exact stack holding guard");
            t=admit();t.remaining=160;service.tick();
            require(base.owner.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION).getAmplifier()==20,"native levitation amplifier20");
            require(service.remaining(base.owner)==159,"one manual active tick");
            service.forget(base.owner);require(service.count()==0,"UUID player lifecycle cleanup");
            t=admit();t.remaining=1;var exact=t.stack;service.tick();
            var data=exact.get(DataComponents.CUSTOM_DATA).copyTag();
            require(base.owner.getMainHandItem()==exact&&data.getIntOr("upgrade_available",0)==1&&data.getIntOr("upgradable",1)==0,
                    "readiness mutates actual participating stack without granting upgraded item");
            require(service.count()==0,"ready closes trial");
            t=admit();t.remaining=310;base.owner.entityTags().add("time_stopped");
            try{service.tick();require(service.remaining(base.owner)==310,"owner Time Stop pauses countdown");}
            finally{base.owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(base.owner.level(),40);
            try{service.tick();require(service.remaining(base.owner)==310,"dimension Time Stop pauses countdown");}
            finally{game.items().timeStop().end(base.owner.level());}
            service.stop();
        }
        void ruby(){
            var t=admit();var c=base.owner.blockPosition();save(c.offset(-11,0,-11),c.offset(11,24,11));
            try{
                // Interior edge, excluded corner and destroy drops use real world writes.
                base.owner.level().setBlock(c.offset(10,24,3),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
                base.owner.level().setBlock(c.offset(10,24,4),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
                base.owner.level().setBlock(c.offset(0,2,0),Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                base.owner.level().setBlock(c.offset(1,2,0),Blocks.LAVA.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                base.owner.level().setBlock(c.offset(-1,2,0),Blocks.OAK_FENCE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                require(complete(t),"ruby resident phase");
                for(int x=-1;x<=1;x++)require(base.owner.level().getBlockState(c.offset(x,2,0)).isAir(),"ruby clears source fluids and the fluid left by a waterlogged block");
                require(base.owner.level().getBlockState(c.offset(10,24,3)).isAir(),"ruby inclusive upper narrow edge");
                require(base.owner.level().getBlockState(c.offset(10,24,4)).is(Blocks.STONE),"ruby excluded corner retained");
                int cells=0;for(int x=-10;x<=10;x++)cells+=(SauvojenMeditation.rubyHalfWidth(x)*2+1)*25;
                require(cells==8725,"source ruby 349×25 cells");
            }finally{service.stop();restore();}
        }
        void stone(){
            var t=admit();var c=base.owner.blockPosition();save(c.offset(-23,-4,-23),c.offset(23,14,23));
            try{
                var template=game.server().getStructureTemplateManager().get(SauvojenMeditation.STONE).orElseThrow(()->
                        new IllegalStateException("Candidate JAR must package data/lbc/structure/stone_cylinder.nbt"));
                require(template.getSize().getX()==47&&template.getSize().getY()==19&&template.getSize().getZ()==47,"full source stone template dimensions");
                // Fill the volume so explicit air and absent template entries remain distinguishable.
                for(var p:terrain.keySet())base.owner.level().setBlock(p,Blocks.GLASS.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                t.remaining=320;require(complete(t),"native template placement");
                int stone=0,air=0,retained=0;
                for(var p:terrain.keySet()){
                    var state=base.owner.level().getBlockState(p);if(state.is(Blocks.STONE))stone++;else if(state.isAir())air++;else if(state.is(Blocks.GLASS))retained++;
                }
                require(stone==6964&&air==26115&&retained==8892,"exact source explicit stone/air and untouched absent cells "+stone+"/"+air+"/"+retained);
            }finally{service.stop();restore();}
        }
        void fire(){
            var t=admit();var c=base.owner.blockPosition();
            save(c.offset(-15,-50,-15),c.offset(15,0,15));save(c.offset(-60,-21,-60),c.offset(60,-21,60));
            try{
                t.remaining=20;require(complete(t),"full resident fire phase");int lava=0,air=0;
                for(var p:terrain.keySet()){
                    var state=base.owner.level().getBlockState(p);if(state.is(Blocks.LAVA))lava++;else if(state.isAir())air++;
                }
                require(lava==42510&&air==20181,"full source lava union and air pit "+lava+"/"+air);
                require(base.owner.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION)==null,"fire removes levitation");
                require(base.owner.getEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING).getDuration()==80,"source four-second slow falling");
            }finally{service.stop();restore();}
        }
        void hazards(){
            var t=admit();t.remaining=601;service.tick();t.remaining=600;require(service.phase(t),"terra phase");
            require(service.terraTicker()==1,"native non-stopped global angular clock advances without legacy scoreboard writer");
            var rods=service.snapshot(base.owner);
            require(rods.stream().map(h->h.at).distinct().count()==3,"three distinct terra positions");
            for(var rod:rods)require(rod.warning!=null&&!rod.warning.closed()&&t.warnings.contains(rod.warning)
                    &&rod.warning.lifetime()==40,"each real terra emitter owns its forty-tick warning display");
            for(int i=0;i<3;i++)require(rods.get(i).at.distanceToSqr(base.owner.position().add(0,1,0).add(Vec3.directionFromRotation(0,i+1).scale(10)))<1e-10,"source ticker multiplied by1/2/3 exact coordinates");
            var prior=rods.getFirst().at;t.remaining=599;service.tick();
            require(service.snapshot(base.owner).stream().filter(h->h.kind==SauvojenMeditation.Kind.TERRA).map(h->h.at).anyMatch(p->p.distanceToSqr(prior)>1e-4),"advancing ring has different emission coordinates");
            for(var h:List.copyOf(t.hazards))if(!rods.contains(h)){t.hazards.remove(h);if(h.actor!=null)h.actor.discard();}
            require(service.snapshot(base.owner).size()==3,"three terra emitters");
            for(var h:service.snapshot(base.owner))require(Math.abs(h.at.subtract(base.owner.position()).horizontalDistance()-10)<1e-5,
                    "native radius10 source geometry");
            var actors=service.snapshot(base.owner).stream().map(h->h.actor).toList();service.forget(base.owner);
            require(actors.stream().allMatch(Entity::isRemoved),"scoped actual helper cleanup");
            t=admit();t.remaining=419;require(service.phase(t),"first air column");
            require(service.snapshot(base.owner).getFirst().at.distanceToSqr(base.owner.position().add(Vec3.directionFromRotation(0,base.owner.getYRot()).scale(-18)))<1e-8,
                    "first air column at exact local-18");service.stop();
            // Source damage fails an active trial even if native damage is rejected.
            t=admit();boolean invulnerable=base.owner.getAbilities().invulnerable;base.owner.getAbilities().invulnerable=true;
            try{service.hurt(t,base.owner,8,DamageTypes.MAGIC,false);require(service.count()==0,"rejected native damage still fails trial");}
            finally{base.owner.getAbilities().invulnerable=invulnerable;}
            t=admit();var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(base.owner.position().add(0,0,5));cow.setNoAi(true);
            base.owner.level().addFreshEntity(cow);spawned.add(cow);float before=cow.getHealth();
            service.hurt(t,cow,4,DamageTypes.IN_FIRE,true);
            require(cow.getHealth()<before&&cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"native acid in_fire and poison");
            var acid=cow.getLastDamageSource();
            require(acid!=null&&acid.is(DamageTypes.IN_FIRE)&&acid.getEntity()==null&&acid.getDirectEntity()==null,
                    "native acid has no physical attacker or carrier");
            require(cow.getEffect(net.minecraft.world.effect.MobEffects.POISON).getAmplifier()==0&&cow.getEffect(net.minecraft.world.effect.MobEffects.POISON).getDuration()==200,
                    "source poison10 seconds amplifier0");require(service.count()==1,"foreign victim does not end author's trial");
            cow.setHealth(1);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            service.hurt(t,cow,8,DamageTypes.MAGIC,false);
            var magic=cow.getLastDamageSource();
            require(cow.isDeadOrDying()&&magic!=null&&magic.is(DamageTypes.MAGIC)
                    &&magic.getEntity()==null&&magic.getDirectEntity()==null,"lethal native trial magic has no physical attacker or carrier");
            require(cow.getKillCredit()==base.owner&&t.cause.equals(game.items().indirectKills().babylonCause(magic)),
                    "lethal trial magic retains the separate player kill credit and exact WILD cause");
            var source=new SauvojenMeditation.TrialTnt(t,base.owner.position());
            require(SauvojenMeditation.managed(source)&&SauvojenMeditation.cause(source)==t.cause&&source.getOwner()==null,
                    "ownerless native TNT preserves exact WILD cause");
            service.stop();require(service.count()==0,"service stop closes all trials");
        }
        void independent(){
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(other!=null&&game.canUseGameplayItems(other),"second actual participant for concurrent trial ownership");
            var saved=new GameChecks.WandThirdPlayerState(other,base.mana);var indirect=new Attribution(game,List.of(other));boolean shift=other.isShiftKeyDown();
            int counter=base.mana.score(other,"sauvojen_wand_upgrade").get();
            try{
                var first=admit();first.remaining=600;service.phase(first);var firstWarnings=List.copyOf(first.warnings);
                other.setItemInHand(InteractionHand.MAIN_HAND,first.stack.copy());other.setShiftKeyDown(true);
                require(service.start(other,InteractionHand.MAIN_HAND,other.getMainHandItem()),"second independent admission");
                var second=service.trial(other);second.remaining=600;service.phase(second);
                var secondActors=service.snapshot(other).stream().map(h->h.actor).toList();var secondWarnings=List.copyOf(second.warnings);
                require(service.count()==2&&secondActors.size()==3,"simultaneous native helper families");
                require(secondActors.stream().noneMatch(Entity::isRemoved),"second native helper family is live before foreign cleanup: "+
                        secondActors.stream().map(e->e.getUUID()+" removed="+e.isRemoved()+" reason="+e.getRemovalReason()).toList());
                service.forget(base.owner);
                require(!firstWarnings.isEmpty()&&firstWarnings.stream().allMatch(TrialWarningVisual::closed)
                        &&!secondWarnings.isEmpty()&&secondWarnings.stream().noneMatch(TrialWarningVisual::closed),
                        "one owner cleanup closes only that owner's warning displays");
                require(service.count()==1&&service.remaining(other)==600&&secondActors.stream().noneMatch(Entity::isRemoved),
                        "first cleanup leaves other trial counters and all native actors intact: trials="+service.count()+
                                " remaining="+service.remaining(other)+" actors="+secondActors.stream().map(e->e.getUUID()+" removed="+e.isRemoved()+" reason="+e.getRemovalReason()).toList());
                service.stop();require(secondActors.stream().allMatch(Entity::isRemoved),"stop closes remaining exact helpers");
            }finally{service.forget(other);saved.restore(game);indirect.close();other.setShiftKeyDown(shift);base.mana.score(other,"sauvojen_wand_upgrade").set(counter);}
        }
        void webGraph(){
            var t=admit();var center=base.owner.position().add(0,.5,12);
            for(var offset:List.of(new Vec3(-4,0,-4),new Vec3(4,0,-4),new Vec3(4,0,4),new Vec3(-4,0,4)))
                t.hazards.add(new SauvojenMeditation.Hazard(SauvojenMeditation.Kind.WEB,center.add(offset),Vec3.ZERO));
            require(service.webLines(t,false)==6,"four native web nodes issue six warning rays");
            var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(center.add(0,-.5,0));cow.setNoAi(true);cow.setNoGravity(true);
            require(base.owner.level().addFreshEntity(cow),"actual diagonal web victim");spawned.add(cow);float health=cow.getHealth();
            require(service.webLines(t,true)==6&&cow.getHealth()<health,"complete web graph hits central victim absent from nearest-node tree");
            t.hazards.clear();
            for(int i=0;i<16;i++)t.hazards.add(new SauvojenMeditation.Hazard(SauvojenMeditation.Kind.WEB,
                    center.add(Math.cos(i*Math.PI/8)*8,0,Math.sin(i*Math.PI/8)*8),Vec3.ZERO));
            try(var packets=new Swap(game,base.owner)){
                require(service.webLines(t,false)==120,"sixteen native web nodes issue120warning rays");
                require(t.webQueries==0&&t.webEdges==120&&t.webSamples>120,"warning traces complete graph without world entity queries");
                require(packets.recording.particles.isEmpty(),"warning graph emits no dust particles");
                var beams=t.warnings.stream().filter(w->!w.closed()&&w.kind()==TrialWarningVisual.Kind.BEAM).toList();
                // Earlier four-node graph may still be fading; verify the newest complete graph.
                beams=beams.subList(beams.size()-120,beams.size());
                int edge=0;
                for(int i=1;i<t.hazards.size();i++)for(int j=0;j<i;j++){
                    var from=t.hazards.get(i).at;var to=t.hazards.get(j).at;var beam=beams.get(edge++);
                    require(beam.origin().distanceToSqr(from)<1e-10,"warning beam starts at exact source node");
                    var direction=to.subtract(from).normalize();var actual=beam.end().subtract(from);
                    require(actual.dot(direction)>=0&&actual.subtract(direction.scale(actual.dot(direction))).lengthSqr()<1e-8
                            &&actual.length()<=from.distanceTo(to)+1e-5,"warning endpoint follows exact clipped source ray");
                    require(Math.abs(actual.length()/.2-Math.rint(actual.length()/.2))<1e-4,"warning retains native .2 sample endpoint");
                    warningGeometry(beam);
                }
                packets.recording.particles.clear();require(service.webLines(t,true)==120,"full damage graph retains all120 edges");
                require(t.webQueries==1&&t.webEdges==120&&t.webSamples>120,"full damage graph uses one native spatial query for all source collision samples");
                require(packets.recording.particles.size()<=1600&&packets.recording.particles.stream().mapToInt(p->p.count()).sum()<=4000,"bounded actual full-graph damage packets and client particle instances");
                var dust=packets.recording.particles.stream()
                        .filter(packet->packet.particle().getType()==net.minecraft.core.particles.ParticleTypes.DUST_COLOR_TRANSITION).toList();
                var sparks=packets.recording.particles.stream()
                        .filter(packet->packet.particle().getType()==net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK).toList();
                int samples=dust.stream().mapToInt(packet->packet.count()).sum();
                require(sparks.stream().mapToInt(packet->packet.count()).sum()==4*samples,
                        "web still emits four sparks for every dust instance");
                int unbatched=2*samples;
                int actual=packets.recording.particles.size();
                require(unbatched-actual==2*(120-15),"one exact shared start per source node instead of 120 repeated starts");
                var lastNode=t.hazards.getLast().at;
                require(dust.stream().filter(packet->new Vec3(packet.x(),packet.y(),packet.z()).distanceToSqr(lastNode)<1e-12)
                                .mapToInt(packet->packet.count()).sum()==15,
                        "all fifteen outgoing starts remain at the exact native node coordinate");
                SkyWars2.LOGGER.info("[check] Sauvojen web: {} particle instances in {} packets vs {} original",5*samples,actual,unbatched);
            }
            t.hazards.clear();cow.setHealth(cow.getMaxHealth());cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var coincident=cow.position().add(0,.5,0);
            for(int i=0;i<2;i++)t.hazards.add(new SauvojenMeditation.Hazard(SauvojenMeditation.Kind.WEB,coincident,Vec3.ZERO));
            try(var packets=new Swap(game,base.owner)){
                require(service.webLines(t,false)==1&&t.webSamples==1&&packets.recording.particles.isEmpty(),"coincident nodes retain warning source sample without dust");
                var warning=t.warnings.getLast();
                require(warning.origin().distanceToSqr(coincident)<1e-12&&warning.end().distanceToSqr(coincident)<1e-12,
                        "coincident warning retains exact shared coordinate");
                require(Math.abs(warning.display().getScale().z()-.001)<1e-6,"coincident source gets visible tiny beam");
                packets.recording.particles.clear();float before=cow.getHealth();
                require(service.webLines(t,true)==1&&t.webSamples==1&&cow.getHealth()<before,"coincident native nodes retain initial exact collision and damage");
                require(packets.recording.particles.size()==2&&packets.recording.particles.stream().allMatch(packet->new Vec3(packet.x(),packet.y(),packet.z()).distanceToSqr(coincident)<1e-12),"coincident damage retains both native particle types at shared coordinate");
            }
            cow.setHealth(cow.getMaxHealth());cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var retained=List.copyOf(t.warnings);
            try(var packets=new Swap(game,base.owner)){
                service.webDamage(t);
                var twinkles=packets.recording.sounds.stream().filter(sound->sound.getSound().value().location().toString()
                        .equals("minecraft:entity.firework_rocket.twinkle")).toList();
                require(twinkles.size()==1,"actual webDamage sends one source twinkle to owner irrespective of edge count");
                var twinkle=twinkles.getFirst();
                require(new Vec3(twinkle.getX(),twinkle.getY(),twinkle.getZ()).distanceTo(base.owner.position())<.22,
                        "web discharge sound originates at source owner");
                require(t.hazards.stream().noneMatch(h->h.kind==SauvojenMeditation.Kind.WEB),"real discharge retires exact graph nodes");
                require(retained.stream().allMatch(TrialWarningVisual::closed),"real discharge closes all retired warning graph displays");
            }
            service.stop();require(t.warnings.isEmpty(),"stop releases warning ownership list");
        }
        void warningGeometry(TrialWarningVisual beam){
            var display=beam.display();var scale=display.getScale();
            require(beam.model().equals(display.getItem().get(DataComponents.ITEM_MODEL))
                    &&beam.model().toString().equals("lbc:meditation_warning_beam_red"),"native display uses authored red warning beam model");
            require(display.getHolder()!=null&&display.getHolder().getAttachment()!=null
                    &&display.getHolder().getAttachment().getWorld()==base.owner.level(),"warning is attached to actual native dimension");
            double length=beam.origin().distanceTo(beam.end());
            require(Math.abs(scale.x()-1)<1e-6&&Math.abs(scale.y()-1)<1e-6&&Math.abs(scale.z()-Math.max(.001,length))<1e-4,
                    "warning display scale preserves exact beam length and cross section");
            // Authored beam bounds z=-8..8, vanilla centered item and native renderer Y-pi rotation.
            var pose=new org.joml.Matrix4f().rotateYXZ((float)Math.toRadians(-display.getYaw()),
                    (float)Math.toRadians(display.getPitch()),0).scale(scale).rotateY((float)Math.PI).translate(-.5f,-.5f,-.5f);
            var start=pose.transformPosition(new org.joml.Vector3f(.5f,.5f,.5f));
            var end=pose.transformPosition(new org.joml.Vector3f(.5f,.5f,-.5f));
            var expected=beam.end().subtract(beam.origin());
            require(start.length()<1e-5&&end.distance(new org.joml.Vector3f((float)expected.x,(float)expected.y,(float)expected.z))<2e-4,
                    "native display rotations place authored beam from warning origin toward exact clipped endpoint");
        }
        void warningLifetime(){
            try(var packets=new Swap(game,base.owner)){
            var t=admit();t.remaining=299;
            var origin=base.owner.position().add(0,3,8);
            var beam=TrialWarningVisual.beam(game,t.level,t.epoch,origin,origin.add(3,4,5),false,6);t.warnings.add(beam);
            warningGeometry(beam);
            int id=beam.display().getEntityId();
            require(packets.recording.packets.stream().anyMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket add
                    &&add.getId()==id&&add.getType()==EntityTypes.ITEM_DISPLAY),"warning sends actual Polymer item display spawn");
            require(packets.recording.particles.isEmpty(),"warning helpers spawn without dust packets");
            base.owner.entityTags().add("time_stopped");
            try{service.tick();require(beam.age()==0&&!beam.closed(),"owner Time Stop freezes warning age");}
            finally{base.owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(t.level,40);
            try{service.tick();require(beam.age()==0&&!beam.closed(),"dimension Time Stop freezes warning age");}
            finally{game.items().timeStop().end(t.level);}
            for(int age=1;age<6;age++){service.tick();require(beam.age()==age&&!beam.closed(),"one warning age step per active game tick");}
            service.tick();require(beam.closed()&&!t.warnings.contains(beam),"sixth active tick closes and releases exact warning display");
            require(packets.recording.packets.stream().anyMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket remove
                    &&remove.entityIds().contains(id)),"lifetime expiration sends actual native display removal");
            var green=TrialWarningVisual.beam(game,t.level,t.epoch,origin,origin.add(0,-9,0),true,40);t.warnings.add(green);
            require(green.model().toString().equals("lbc:meditation_warning_beam_green"),"acid warning uses green beam model");
            var wall=TrialWarningVisual.wall(game,t.level,t.epoch,origin,new Vec3(1,0,0),20,2,40);t.warnings.add(wall);
            var cube=TrialWarningVisual.cube(game,t.level,t.epoch,origin,origin.add(2,3,4),40);t.warnings.add(cube);
            var sphere=TrialWarningVisual.sphere(game,t.level,t.epoch,origin,7,40);t.warnings.add(sphere);
            var ring=TrialWarningVisual.ring(game,t.level,t.epoch,origin,10,40);t.warnings.add(ring);
            require(wall.display().getScale().x()==20&&wall.display().getScale().y()==2,"terra warning wall spans full source ray");
            require(cube.origin().distanceToSqr(origin.add(1,1.5,2))<1e-10&&cube.display().getScale().z()==4,"air warning cube matches world bounds");
            service.tick();require(sphere.display().getScale().x()>0&&sphere.radius()==7,"air sphere expands toward source blast radius");
            require(ring.display().getScale().x()==20&&ring.radius()==10,"acid annulus preserves outer diameter20");
            var warnings=List.copyOf(t.warnings);service.forget(base.owner);
            require(warnings.stream().allMatch(TrialWarningVisual::closed)&&t.warnings.isEmpty(),"owner cleanup closes every warning shape and ownership list");
            t=admit();t.remaining=299;var failed=TrialWarningVisual.ring(game,t.level,t.epoch,origin,10,40);t.warnings.add(failed);
            service.hurt(t,base.owner,0,net.minecraft.world.damagesource.DamageTypes.MAGIC,false);
            require(failed.closed()&&t.warnings.isEmpty(),"trial failure closes live warning displays");
            }
        }
        void fear(){
            var psycho=game.items().psycho();
            require(psycho.effectValue(base.owner,"scared")==0,"fixture starts with clean canonical fear");
            var t=admit();t.remaining=160;float health=base.owner.getHealth();require(service.phase(t),"initial levitation phase");
            require(base.owner.getHealth()==health&&service.count()==1&&psycho.effectValue(base.owner,"scared")==0,"initial levitation neither damages nor afflicts fear");
            var picture=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,base.owner.level());
            picture.setWidth(4);picture.setHeight(4);picture.entityTags().add("scare_picture");
            picture.setPos(base.owner.getEyePosition().add(base.owner.getLookAngle().scale(5)).add(0,-2,0));
            require(base.owner.level().addFreshEntity(picture),"actual looked-at scare interaction");spawned.add(picture);
            try{
                require(service.looking(t),"actual native scare sight ray");
                base.mana.score(base.owner,"scared").set(10000);
                service.blind(t);require(psycho.effectValue(base.owner,"scared")==10&&base.owner.getHealth()==health&&service.count()==1,"first sight uses canonical fear10 despite obsolete raw score");
                for(int i=0;i<5;i++)psycho.tick();
                require(psycho.effectValue(base.owner,"scared")==5,"real Psycho five-tick decay applies to meditation fear");
                // Scared movement is native and random: restore only this fixture's actual sight geometry.
                picture.setPos(base.owner.getEyePosition().add(base.owner.getLookAngle().scale(5)).add(0,-2,0));
                psycho.afflict(base.owner,base.owner,90,7,0,0);
                service.blind(t);require(psycho.effectValue(base.owner,"scared")==105&&base.owner.getHealth()==health&&service.count()==1,"inclusive105 boundary remains harmless");
                base.owner.entityTags().add("sin_greed_active");
                try{service.blind(t);require(psycho.effectValue(base.owner,"scared")==105&&base.owner.getHealth()==health&&service.count()==1,"Greed rejects fear and threshold damage");}
                finally{base.owner.entityTags().remove("sin_greed_active");}
                boolean invulnerable=base.owner.getAbilities().invulnerable;base.owner.getAbilities().invulnerable=true;
                try{service.blind(t);require(psycho.effectValue(base.owner,"scared")==115&&service.count()==0,"only above105 source damage fails trial even when native hit is rejected");}
                finally{base.owner.getAbilities().invulnerable=invulnerable;}
                require(psycho.effectValue(base.owner,"lookback")==7,"fear lifecycle preserves unrelated custom status");
            }finally{picture.discard();psycho.clearStatus(base.owner,"scared");psycho.clearStatus(base.owner,"lookback");service.stop();}
        }
        void bombPurge(){
            var owner=base.owner;
            var danmaku=game.items().danmaku();
            var psycho=game.items().psycho();
            int savedCharge=base.mana.score(owner,"danmaku_score").get();
            int savedVolley=base.mana.score(owner,"danmaku").get();
            var failures=new ArrayList<String>();
            try{
                for(boolean fast:new boolean[]{false,true}){
                    String source=fast?"Hakurei bomb":"Gohai spellcard";
                    require(danmaku.bombs().isEmpty(),"exclusive "+source+" publication");
                    var t=admit();
                    var center=owner.position();
                    t.remaining=520;
                    require(service.phase(t),"actual first lightning-network phase");
                    var webs=t.hazards.stream().filter(h->h.kind==SauvojenMeditation.Kind.WEB).toList();
                    require(webs.size()==4,"real first phase publishes four network beacons");
                    t.warnings.forEach(TrialWarningVisual::close);
                    t.warnings.clear();
                    var line=center.add(0,1.9,0);
                    relocate(webs.get(0),line.add(-6,0,0));
                    relocate(webs.get(1),line);
                    relocate(webs.get(2),line.add(6,0,0));
                    relocate(webs.get(3),line.add(6,0,10));
                    require(service.webLines(t,false)==6,"actual controlled positions warn six native links");
                    var cow=new Cow(EntityTypes.COW,t.level);
                    cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(line.add(-3,-.4,0));
                    require(t.level.addFreshEntity(cow),"native victim outside both spheres");
                    spawned.add(cow);
                    float health=cow.getHealth();
                    require(service.webLines(t,true)==6&&cow.getHealth()<health&&service.count()==1,
                            "the live middle beacon can damage the native witness without ending the trial");
                    cow.setHealth(health);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;

                    t.remaining=300;
                    require(service.phase(t),"actual authored acid ring phase");
                    var ring=t.hazards.stream().filter(h->h.kind==SauvojenMeditation.Kind.ACID_RING).findFirst().orElseThrow();
                    relocate(ring,center.add(0,0,-1));
                    owner.setYRot(0);owner.setXRot(-90);
                    service.blind(t);
                    var nearScare=t.hazards.stream().filter(h->h.kind==SauvojenMeditation.Kind.SCARE).toList();
                    require(nearScare.size()==2,"phase publishes native face and gaze interaction");
                    for(var h:nearScare)relocate(h,center.add(0,0,1.25));
                    service.blind(t);
                    var farScare=t.hazards.stream().filter(h->h.kind==SauvojenMeditation.Kind.SCARE&&!nearScare.contains(h)).toList();
                    require(farScare.size()==2,"independent live face pair");
                    for(var h:farScare)relocate(h,center.add(0,0,-8));
                    var face=nearScare.stream().map(h->h.actor).filter(net.minecraft.world.entity.Display.class::isInstance).findFirst().orElseThrow();
                    var gaze=nearScare.stream().map(h->h.actor).filter(net.minecraft.world.entity.Interaction.class::isInstance).findFirst().orElseThrow();
                    var gazePosition=center.add(0,0,-3);
                    owner.setPos(gazePosition);owner.setXRot(0);
                    var eye=owner.getEyePosition();
                    require(!gaze.getBoundingBox().contains(eye)
                                    &&gaze.getBoundingBox().clip(eye,eye.add(owner.getLookAngle().scale(100))).isPresent()
                                    &&service.looking(t),"native gaze enters the first actual scare hitbox from outside");
                    owner.setPos(center);
                    var farBodies=farScare.stream().map(h->h.actor).toList();
                    var beacon=webs.get(1).actor;
                    var outside=List.of(webs.get(0).actor,webs.get(2).actor,webs.get(3).actor);
                    var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,t.level);
                    arrow.setOwner(owner);arrow.setPos(center);
                    var interaction=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,t.level);
                    interaction.setPos(center.add(1,0,0));
                    require(t.level.addFreshEntity(arrow)&&t.level.addFreshEntity(interaction),"native own projectile and unrelated Interaction controls");
                    spawned.add(arrow);spawned.add(interaction);

                    var item=game.lbc().rollTable(t.level,center,owner,fast?"lbc:bomb_item":"lbc:ported/danmaku_staff").getFirst();
                    owner.setItemInHand(InteractionHand.OFF_HAND,item);
                    if(!fast)base.mana.score(owner,"danmaku_score").set(100);
                    require(owner.gameMode.useItem(owner,t.level,item,InteractionHand.OFF_HAND).consumesAction(),
                            "actual native held "+source+" while original Keeper remains in main hand");
                    var bomb=danmaku.bombs().getFirst();
                    require(bomb.fast==fast&&bomb.owner==owner&&bomb.display.position().equals(center)&&owner.getMainHandItem()==t.stack,
                            "source-specific actual sphere and original trial identity");
                    owner.setPos(gazePosition);
                    for(int tick=0;tick<10&&bomb.radius()<2;tick++)danmaku.tick();
                    require(bomb.radius()==2&&beacon.isRemoved()&&face.isRemoved(),"actual "+source+" removes both nearby visible hazards");
                    require(outside.stream().noneMatch(Entity::isRemoved)&&farBodies.stream().noneMatch(Entity::isRemoved)
                                    &&!arrow.isRemoved()&&!interaction.isRemoved(),"outside actors, own projectile and unrelated interaction survive");
                    int fear=psycho.effectValue(owner,"scared");
                    boolean staleGaze=service.looking(t);
                    service.blind(t);
                    if(!gaze.isRemoved()||staleGaze||psycho.effectValue(owner,"scared")!=fear)
                        failures.add(source+" left gaze/fear: gazeRemoved="+gaze.isRemoved()+" visible="+staleGaze
                                +" fear="+fear+"->"+psycho.effectValue(owner,"scared"));
                    if(t.hazards.contains(ring))failures.add(source+" left the actorless acid ring");
                    owner.setPos(center);
                    t.remaining=420;
                    service.tick(); // Same game tick as the sphere, before the generic ENTITY_UNLOAD queue drains.
                    if(cow.getHealth()!=health)failures.add(source+" discharged a removed or undeclared lightning line: "
                            +health+"->"+cow.getHealth());
                    require(service.count()==1,"discharging the remaining network keeps the trial");
                    danmaku.forget(owner);service.stop();psycho.clearEffects(owner);
                    cow.discard();arrow.discard();interaction.discard();
                }
                require(failures.isEmpty(),"sphere and same-tick trial cleanup: "+failures);
            }finally{
                danmaku.forget(owner);service.stop();psycho.clearEffects(owner);
                owner.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                owner.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
                owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                base.mana.score(owner,"danmaku_score").set(savedCharge);
                base.mana.score(owner,"danmaku").set(savedVolley);
            }
        }
        void relocate(SauvojenMeditation.Hazard hazard,Vec3 at){
            hazard.at=at;
            if(hazard.actor!=null)hazard.actor.setPos(at);
        }
        void alliedBlast(){
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null,"actual allied native blast participant");
            var state=new GameChecks.WandThirdPlayerState(other,base.mana);var indirect=new Attribution(game,List.of(other));
            var board=game.server().getScoreboard();var oldOwnerTeam=base.owner.getTeam();var oldOtherTeam=other.getTeam();
            int savedProtection=game.config().tntProtection;
            require(board.getPlayerTeam("sw2_medit_tnt")==null,"exclusive native TNT team");var team=board.addPlayerTeam("sw2_medit_tnt");team.setAllowFriendlyFire(false);
            try{
                var t=admit();var at=base.owner.position().add(12,0,0);var c=BlockPos.containing(at);save(c.offset(-9,-9,-9),c.offset(9,9,9));
                board.addPlayerToTeam(base.owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
                other.getInventory().clearContent();other.removeAllEffects();other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                other.getAbilities().invulnerable=false;other.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
                other.setHealth(200);other.setAbsorptionAmount(0);other.setInvulnerableTime(0);other.damageCooldownTime=0;other.setNoGravity(true);
                other.teleportTo(other.level(),at.x+1,at.y,at.z,Set.of(),0,0,true);
                require(!base.owner.canHarmPlayer(other),"ordinary native team damage is disabled before trial TNT");
                game.config().tntProtection=5;
                float health=other.getHealth();service.blast(t,at);
                require(other.getHealth()<health,"actual native TrialTnt damages allied player with maximum ordinary TNT protection");
                var damage=other.getLastDamageSource();
                require(damage!=null&&damage.getEntity()==null
                        &&damage.getDirectEntity() instanceof SauvojenMeditation.TrialTnt tnt&&tnt.getOwner()==null,
                        "native explosion keeps the ownerless TNT as its direct source");
                require(t.cause.equals(game.items().indirectKills().babylonCause(damage)),
                        "native allied TNT retains the exact WILD cause apart from its physical source");
                var ledger = (FixtureAccess.Attribution) (Object) game.items().indirectKills();
                require(t.cause.equals(ledger.validation$laterAttacks().get(other)),
                        "actual allied TNT retains exact captured WILD indirect fall author/order");
            }finally{
                game.config().tntProtection=savedProtection;
                service.stop();restore();board.removePlayerTeam(team);
                if(oldOwnerTeam!=null)board.addPlayerToTeam(base.owner.getScoreboardName(),oldOwnerTeam);
                if(oldOtherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOtherTeam);
                state.restore(game);indirect.close();
            }
        }
        @Override public void close(){
            try{service.stop();restore();for(var e:spawned){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
                base.mana.score(base.owner,"sauvojen_wand_upgrade").set(savedCounter);base.mana.score(base.owner,"scared").set(savedFear);
            }finally{try{base.close();}finally{try{attribution.close();}finally{
                for(var chunk:forced)base.owner.level().setChunkForced(chunk.x(),chunk.z(),false);forced.clear();
            }}}
        }
    }
    public static void run(GameController game){
        try(var f=new Fixture(game)){f.admission();f.ruby();f.stone();f.fire();f.hazards();f.independent();f.webGraph();f.warningLifetime();f.fear();f.bombPurge();f.alliedBlast();}
        SkyWars2.LOGGER.info("[check] PASS sauvojen-meditation (native stack/readiness,8725ruby,47x19x47 explicit stone,42510lava/20181air, complete6/120warning displays+diagonal damage+bounded damage packets, native warning geometry/lifetime/TimeStop/cleanup, one source twinkle, canonical fear/decay/105 threshold/Greed, ownerless WILD damage and TNT with separate kill credit, per-participant state and cleanup)");
    }
}
