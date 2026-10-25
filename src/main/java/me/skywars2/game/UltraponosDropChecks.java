package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native fixtures. Service stepping deliberately leaves unrelated world physics alone. */
public final class UltraponosDropChecks {
    private UltraponosDropChecks() { }
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Creeper> stopActors=List.of();
    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Ultraponos drops: "+message);
    }
    private static CompoundTag saved(Entity entity,ServerLevel level) {
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        entity.saveWithoutId(out); return out.buildResult();
    }
    private static ItemEntity token(ServerLevel level,Vec3 at,int count,ServerPlayer author) {
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(LbcUltraponosDropItem.Kind.NORMAL.item(),count));
        item.setThrower(author);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static List<Creeper> actors(GameController game) {
        return game.items().creeperDrops().snapshot().stream()
                .filter(c->c.entityTags().contains(LbcCreeperDrops.ULTRAPONOS) && !c.isRemoved()).toList();
    }
    private static boolean govno(ItemStack item) {
        return item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("govnoq",0)==1;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState savedA,savedB;
        final Vec3 origin;final BlockPos base;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Long> forced=new HashSet<>();
        final boolean skills,griefing;final int skillA,skillB,mobKills;
        boolean retainForStop;
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> customStats=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<net.minecraft.world.item.Item>,Integer> itemStats=new LinkedHashMap<>();
        Fixture(GameController game) {
            this.game=game;level=game.maps().level();
            alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null,"two actual match participants");
            require(game.items().ultraponos().emitterCount()==0 && actors(game).isEmpty(),"exclusive fixture actors");
            savedA=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());
            savedB=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            skills=game.config().lbcSkills;skillA=game.players().get(alpha.getUUID()).lbcSkill;skillB=game.players().get(beta.getUUID()).lbcSkill;
            griefing=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
            mobKills=alpha.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,Math.min(250.7,level.getMaxY()-40.3),Math.floor(alpha.getZ()/16)*16+8.81);
            base=BlockPos.containing(origin);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);customStats.put(stat,alpha.getStats().getValue(stat));}
            for(var stat:List.of(net.minecraft.stats.Stats.ITEM_USED.get(LbcKfcFood.GOVNO_EATEN),net.minecraft.stats.Stats.ITEM_CRAFTED.get(LbcElectro.SHIT_KATANA)))itemStats.put(stat,alpha.getStats().getValue(stat));
        }
        void initialize() {
            // Only the fixture requests residency. Neither admission nor emitter may do so.
            for(int x=(base.getX()-20)>>4;x<=(base.getX()+20)>>4;x++)for(int z=(base.getZ()-20)>>4;z<=(base.getZ()+20)>>4;z++) {
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}
                level.getChunk(x,z);
            }
            for(var p:BlockPos.betweenClosed(base.offset(-2,-3,-2),base.offset(2,4,3))) {
                blocks.put(p.immutable(),game.maps().snapshot(level,p));clearBlock(p);
            }
            game.config().lbcSkills=false;
            for(var player:List.of(alpha,beta)) {
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();
                player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
                player.teleportTo(level,origin.x+35,origin.y+15,origin.z,Set.of(),0,0,true);
            }
        }
        void clearBlock(BlockPos p) {
            if(level.getBlockEntity(p) instanceof Container c)c.clearContent();
            level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
        }
        ItemEntity drop(int count) {var item=token(level,origin,count,alpha);owned.add(item);return item;}
        Set<Entity> entities() {var set=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(set::add);return set;}
        List<ItemEntity> addedItems(Set<Entity> before) {
            var out=new ArrayList<ItemEntity>();
            for(var e:level.getAllEntities())if(!before.contains(e) && e instanceof ItemEntity item && item.position().distanceTo(origin)<60) {owned.add(item);out.add(item);}
            return out;
        }
        void ticks(int count){for(int i=0;i<count;i++)game.items().ultraponos().tick();}
        void clearRewards(){for(var e:owned)if(e instanceof ItemEntity item && govno(item.getItem()))item.discard();}
        void reset() {
            game.items().ultraponos().stop();for(var c:actors(game))c.discard();game.items().creeperDrops().tick();
            owned.forEach(Entity::discard);for(var p:blocks.keySet())clearBlock(p);
        }
        void activate() {
            var t=drop(2);t.setTarget(beta.getUUID());require(game.items().ultraponos().ready(t),"resident service admission");
            require(game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"one shared debit");
            require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"same-tick replay rejected");t.discard();
            owned.addAll(actors(game));require(game.items().ultraponos().emitterCount()==1,"one finite emitter");
        }
        void presentation() {
            var stack=new ItemStack(LbcUltraponosDropItem.Kind.NORMAL.item());var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr("ultraponos",0)==1 && data.getIntOr("drop",0)==1,"exact source token flags");
            require(Identifier.parse("lbc:drop_trap").equals(stack.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"trap model and tier");
            translated(stack.getHoverName(),"trap_ponos",0xffff55);
            var lore=stack.get(DataComponents.LORE);require(lore!=null && lore.lines().size()==1,"one source lore row");translated(lore.lines().getFirst(),"trap_desc",0xffffff);
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/ultraponos_drop");
            require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(stack,loot.getFirst()),"real public acquisition leaf");
            require(!LbcUltraponosDropItem.matches(new ItemStack(Items.REPEATING_COMMAND_BLOCK)),"plain visual lookalike rejected");
            var copied=new ItemStack(Items.PAPER);copied.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            require(LbcUltraponosDropItem.matches(copied),"source copied flags remain recognized");
        }
        void geometry() {
            activate();
            require(level.getBlockState(base.south()).is(Blocks.POLISHED_DEEPSLATE) && level.getBlockState(base.south().above()).is(Blocks.POLISHED_DEEPSLATE),"two source polished deepslate cells");
            var trap=level.getBlockState(base.above());require(trap.is(Blocks.IRON_TRAPDOOR) && trap.getValue(TrapDoorBlock.FACING)==Direction.NORTH
                    && trap.getValue(TrapDoorBlock.OPEN) && trap.getValue(TrapDoorBlock.POWERED) && trap.getValue(TrapDoorBlock.HALF)==Half.BOTTOM,"native powered open north bottom trapdoor");
            require(level.getBlockState(base.above(2)).isAir(),"explicit source air cell");
            var lever=level.getBlockState(base.south().above(2));require(lever.is(Blocks.LEVER) && lever.getValue(LeverBlock.FACE)==AttachFace.FLOOR
                    && lever.getValue(LeverBlock.FACING)==Direction.EAST && lever.getValue(LeverBlock.POWERED),"floor powered east lever");
            var hopper=level.getBlockState(base);require(hopper.is(Blocks.HOPPER) && hopper.getValue(HopperBlock.FACING)==Direction.SOUTH,"native south hopper");
            require(level.getBlockEntity(base) instanceof Container,"native hopper inventory");var inventory=(Container)level.getBlockEntity(base);
            require(level.getBlockEntity(base).saveWithFullMetadata(level.registryAccess()).getIntOr("TransferCooldown",-1)==0,"native source hopper transfer cooldown0");
            var types=List.of(Items.COCOA_BEANS,Items.NETHER_BRICK,Items.MELON_SEEDS,Items.FROGSPAWN,Items.DYE.pick(net.minecraft.world.item.DyeColor.BROWN));
            require(inventory.getContainerSize()==5,"five hopper slots");for(int i=0;i<5;i++)require(inventory.getItem(i).is(types.get(i)) && inventory.getItem(i).getCount()==64,"source hopper slot "+i);
            var live=actors(game);require(live.size()==4,"normal four native delayed creepers");
            var radii=new HashSet<Integer>();double[] heights={-1.5,-2.5,.5,1.5};
            for(var c:live) {
                var tag=saved(c,level);int r=tag.getByteOr("ExplosionRadius",(byte)-1);require(r>=6 && r<=9 && radii.add(r),"unique normal blast radius");
                require(c.position().distanceTo(Vec3.atCenterOf(base).add(0,heights[r-6]-.5,0))<1e-9,"floored source position and height");
                require(c.isNoAi() && c.isNoGravity() && c.isSilent() && c.isInvulnerable() && tag.getShortOr("Fuse",(short)-1)==290
                        && tag.getBooleanOr("ignited",false) && !tag.getBooleanOr("powered",false) && tag.getBooleanOr("PersistenceRequired",false)
                        && c.entityTags().contains("govno_creeper") && !c.isCustomNameVisible(),"native source fuse and actor flags");
                var effect=c.getEffect(MobEffects.INVISIBILITY);require(effect!=null && effect.getDuration()==288 && effect.getAmplifier()==0
                        && effect.isAmbient() && !effect.isVisible() && !effect.showIcon(),"native288 invisible effect");
                translated(c.getCustomName(),"megadrist",0xffff55);
                require(game.items().currentEpoch(c) && game.items().creeperDrops().owner(c)==alpha,"shared actor epoch and captured thrower despite pickup target");
            }
        }
        void cadence() {
            var initial=entities().stream().filter(e->e instanceof ItemEntity item && govno(item.getItem()) && !item.isRemoved()
                    && item.position().distanceTo(origin)<15).toList();owned.addAll(initial);
            require(initial.isEmpty() && game.items().ultraponos().emittedPulses()==0,"no initial item before first active tick");
            double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minZ=Double.POSITIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
            boolean separateAxes=false;
            // Reclaim every checked output so the authored entity threshold cannot mask a missing tick.
            for(int tick=1;tick<=280;tick++) {
                var before=entities();ticks(1);var rewards=addedItems(before);
                require(rewards.size()==1,"one real govno item on active tick "+tick);var reward=rewards.getFirst();var velocity=reward.getDeltaMovement();
                require(govno(reward.getItem()) && reward.getItem().getCount()==1 && reward.getX()==base.getX()+.5 && reward.getZ()==base.getZ()+.5
                        && Math.abs(reward.getY()-base.getY()-1.05)<1e-9,"real single reward centered just above hopper rim");
                require(Math.abs(velocity.y-.8)<1e-9 && velocity.x>=-.2 && velocity.x<=.2 && velocity.z>=-.2 && velocity.z<=.2,
                        "upward .8 velocity with independently bounded horizontal scatter");
                minX=Math.min(minX,velocity.x);maxX=Math.max(maxX,velocity.x);minZ=Math.min(minZ,velocity.z);maxZ=Math.max(maxZ,velocity.z);
                separateAxes|=velocity.x!=velocity.z;reward.discard();
                require(game.items().ultraponos().emitterCount()==(tick<280?1:0),"producer lifetime through active tick "+tick);
            }
            require(minX<0 && maxX>0 && minZ<0 && maxZ>0 && separateAxes,"horizontal scatter varies on both axes rather than sharing one fixed value");
            require(game.items().ultraponos().emittedPulses()==280 && game.items().ultraponos().emitterCount()==0,"exact280 active single-item emissions");
            var before=entities();ticks(50);require(addedItems(before).isEmpty() && game.items().ultraponos().emittedPulses()==280,"finite producer never resumes");
        }
        void particles() {
            reset();activate();
            alpha.teleportTo(level,origin.x+4,origin.y+2,origin.z,Set.of(),0,0,true);
            var previous=alpha.connection;
            var packets=new ArrayList<net.minecraft.network.protocol.Packet<?>>();
            var recorder=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public boolean isMemoryConnection(){return true;}
                @Override public boolean isConnected(){return true;}
                @Override public net.minecraft.network.PacketListener getPacketListener(){return alpha.connection;}
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
                @Override public void flushChannel(){ }
            };
            try {
                alpha.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),recorder,alpha,
                        new net.minecraft.server.network.CommonListenerCookie(alpha.getGameProfile(),0,alpha.clientInformation(),false));
                alpha.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                var before=entities();ticks(1);addedItems(before);
                var emitted=packets.stream().filter(p->p instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket)
                        .map(p->(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket)p).toList();
                require(emitted.size()==1,"one batched stream packet per nearby viewer and tick");
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
                var decoded=new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket[1];
                try {net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(
                        (net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)alpha.connection,()->{
                            net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.STREAM_CODEC.encode(buffer,emitted.getFirst());
                            decoded[0]=net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.STREAM_CODEC.decode(buffer);
                        });
                } finally {buffer.release();}
                var particle=decoded[0];
                require(particle.particle() instanceof net.minecraft.core.particles.BlockParticleOption block
                        && block.getType()==net.minecraft.core.particles.ParticleTypes.DUST_PILLAR
                        && block.getState().is(Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BROWN)),"native same TerrainParticle brown-wool shard appearance");
                require(particle.x()==base.getX()+.5 && Math.abs(particle.y()-base.getY()-1.05)<1e-9 && particle.z()==base.getZ()+.5
                        && particle.xDist()==0 && particle.yDist()==0 && particle.zDist()==0,"stream starts at hopper rim with no area randomization");
                require(particle.count()==60 && particle.xMaxSpeed()==0 && Math.abs(particle.yMaxSpeed()-.8)<1e-7 && particle.zMaxSpeed()==0
                        && particle.randomizationType()==net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.ALTERNATIVE,
                        "item-like mean upward speed retains source particle rate in one native packet");
                dustPillarSourceSemantics(particle);
            } finally {
                alpha.connection=previous;previous.resetPosition();
                alpha.teleportTo(level,origin.x+35,origin.y+15,origin.z,Set.of(),0,0,true);
            }
        }
        /** Exact26.3 TerrainParticle.DustPillarProvider + Particle.tick equations, not real client rendering. */
        void dustPillarSourceSemantics(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet) {
            var random=net.minecraft.util.RandomSource.create(0x4c4243554c545241L);
            double meanY=0,meanX=0,meanZ=0,firstDisplacement=0;int rising=0;int samples=4096;
            for(int i=0;i<samples;i++) {
                // ALTERNATIVE passes the packet axis speeds directly, unlike DEFAULT's signed Gaussian.
                double x=random.nextGaussian()/30.0,y=packet.yMaxSpeed()+random.nextGaussian()/2.0,z=random.nextGaussian()/30.0;
                // DustPillarProvider overwrites the normalized constructor velocities with these values.
                meanX+=x;meanY+=y;meanZ+=z;firstDisplacement+=y-.04;
                if(y-.04>0)rising++;
            }
            meanX/=samples;meanY/=samples;meanZ/=samples;firstDisplacement/=samples;
            require(Math.abs(meanY-.8)<.03 && Math.abs(firstDisplacement-.76)<.03 && rising>samples*.90,
                    "source provider cohort rises with item-like meanY.8 and gravity.04; native jitter permits rare downward shards");
            require(Math.abs(meanX)<.003 && Math.abs(meanZ)<.003,
                    "source provider gives independent centered Gaussian horizontal scatter with sigma1/30");
        }
        void threshold() {
            reset();activate();var center=Vec3.atCenterOf(base).add(0,1,0);var blockers=new ArrayList<ItemEntity>();
            for(int i=0;i<201;i++) {
                var stack=new ItemStack(Items.DIRT,64);var tag=new CompoundTag();tag.putInt("govnoq",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
                var item=new ItemEntity(level,center.x,center.y,center.z,stack);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
                require(level.addFreshEntity(item),"threshold native item");owned.add(item);blockers.add(item);
            }
            var before=entities();ticks(1);require(addedItems(before).isEmpty(),"201 flagged entities suppress the active tick");
            blockers.getLast().discard();before=entities();ticks(1);var emitted=addedItems(before);
            require(emitted.size()==1,"200 entities allow one item despite12800 total stack units");
            before=entities();ticks(2);require(addedItems(before).isEmpty(),"new201st item suppresses subsequent active ticks");
            emitted.getFirst().discard();before=entities();ticks(1);require(addedItems(before).size()==1,"removing201st entity permits exactly one new item");
            reset();activate();
            for(int i=0;i<200;i++) {
                var stack=new ItemStack(Items.DIRT);var tag=new CompoundTag();tag.putInt("govnoq",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
                var item=new ItemEntity(level,center.x,center.y,center.z,stack);item.setNoGravity(true);
                require(level.addFreshEntity(item),"sphere boundary native item");owned.add(item);
            }
            var out=new ItemEntity(level,center.x+14.9,center.y+14.9,center.z,new ItemStack(Items.DIRT,64));
            var flags=new CompoundTag();flags.putInt("govnoq",1);out.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(flags));out.setNoGravity(true);
            require(level.addFreshEntity(out),"outside sphere native item");owned.add(out);
            before=entities();ticks(1);require(addedItems(before).size()==1,"AABB corner outside radius15 cannot count");
        }
        void guardsAndPause() {
            reset();var fake=drop(2);fake.makeFakeItem();require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount()==2,"give visual cannot debit");
            var stale=drop(2);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().ultraponos().ready(stale) && !game.items().specialDrops().spawn(stale) && stale.getItem().getCount()==2,"old epoch cannot debit");
            int x=(base.getX()>>4)+1024,z=(base.getZ()>>4)+1024;while(level.getChunkSource().getChunkNow(x,z)!=null)x+=1024;
            var remote=new ItemEntity(level,x*16+8,origin.y,z*16+8,new ItemStack(LbcUltraponosDropItem.Kind.NORMAL.item(),2));
            int forcedCount=level.getForceLoadedChunks().size();require(!game.items().ultraponos().ready(remote) && !game.items().specialDrops().spawn(remote)
                    && remote.getItem().getCount()==2 && level.getChunkSource().getChunkNow(x,z)==null && forcedCount==level.getForceLoadedChunks().size(),"unresident admission neither loads nor debits");
            activate();var before=entities();ticks(4);require(addedItems(before).size()==4 && game.items().ultraponos().emittedPulses()==4,"four active ticks emit four items before pause");
            clearRewards();var pending=drop(2);before=entities();game.items().timeStop().begin(level,200);
            try {ticks(400);require(game.items().ultraponos().emitterCount()==1 && game.items().ultraponos().emittedPulses()==4 && addedItems(before).isEmpty()
                        && !game.items().specialDrops().spawn(pending) && pending.getItem().getCount()==2,"dimension pause freezes emitter age and pending debit");}
            finally{game.items().timeStop().end(level);}
            ticks(1);require(addedItems(before).size()==1 && game.items().ultraponos().emittedPulses()==5,"thaw resumes with exactly one active-tick item");pending.discard();
        }
        void suppression() {
            reset();activate();var live=List.copyOf(actors(game));
            var suppressor=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,level);
            suppressor.setPos(Vec3.atCenterOf(base).add(0,1,0));suppressor.entityTags().add("stopper");
            require(level.addFreshEntity(suppressor),"native mechanism suppressor");owned.add(suppressor);
            require(game.items().magic().mechanismSuppressedAt(level,suppressor.position()),"registered mechanism suppression fixture");
            ticks(1);require(game.items().ultraponos().emitterCount()==0 && live.stream().noneMatch(Entity::isRemoved),"mechanism suppression permanently cancels only emitter");
            suppressor.discard();var before=entities();ticks(300);require(addedItems(before).isEmpty() && game.items().ultraponos().emitterCount()==0,"removing suppressor cannot restart cancelled emitter");
        }
        void mega() {
            reset();game.config().lbcSkills=true;
            game.players().get(alpha.getUUID()).lbcSkill=0;game.players().get(beta.getUUID()).lbcSkill=9;
            var board=game.server().getScoreboard();var oldA=alpha.getTeam();var oldB=beta.getTeam();
            require(board.getPlayerTeam("codex_ultraponos_probe")==null,"mega fixture team available");var team=board.addPlayerTeam("codex_ultraponos_probe");team.setAllowFriendlyFire(false);
            board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
            beta.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);
            try {
                activate();var live=actors(game);require(live.size()==8,"skill9 nearest eligible ally produces eight source explosions");
                var radii=new HashSet<Integer>();double[] heights={-1.5,-2.5,.5,1.5};
                for(var c:live) {
                    int r=saved(c,level).getByteOr("ExplosionRadius",(byte)-1);require(radii.add(r),"mega unique native radius");
                    int index= r>=12?r/2-6:r-6;require(index>=0 && index<4 && c.position().distanceTo(Vec3.atCenterOf(base).add(0,heights[index]-.5,0))<1e-9,"mega and base source height");
                    require(game.items().creeperDrops().owner(c)==alpha,"nearest skill selector never replaces causal thrower");
                }
                require(radii.equals(Set.of(6,7,8,9,12,14,16,18)),"four amplified plus four retained normal radii");
                reset();beta.entityTags().add("strict_map_object");
                try {activate();require(actors(game).size()==4,"nearest protected-role skill9 cannot amplify trap");}
                finally{beta.entityTags().remove("strict_map_object");}
                reset();game.config().lbcSkills=false;activate();require(actors(game).size()==4,"disabled skills retain four normal creepers beside skill9 player");
            } finally {
                board.removePlayerTeam(team);if(oldA!=null)board.addPlayerToTeam(alpha.getScoreboardName(),oldA);if(oldB!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldB);
                game.config().lbcSkills=false;game.players().get(alpha.getUUID()).lbcSkill=0;game.players().get(beta.getUUID()).lbcSkill=0;
                beta.teleportTo(level,origin.x+35,origin.y+15,origin.z,Set.of(),0,0,true);reset();
            }
        }
        void nativeBlast() {
            reset();activate();var c=actors(game).getFirst();for(var other:actors(game))if(other!=c)other.discard();
            require(game.items().creeperDrops().explosionTarget(c,alpha),"WILD blast includes own thrower");
            var board=game.server().getScoreboard();var oldA=alpha.getTeam();var oldB=beta.getTeam();
            require(board.getPlayerTeam("codex_ultraponos_probe")==null,"fixture team available");var team=board.addPlayerTeam("codex_ultraponos_probe");team.setAllowFriendlyFire(false);
            try {
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                require(game.items().creeperDrops().explosionTarget(c,beta),"WILD includes allied participant");
                var hazard=base.above(3);
                beta.teleportTo(level,hazard.getX()+.5,hazard.getY(),hazard.getZ()+.5,Set.of(),0,0,true);
                game.items().indirectKills().worldChanges(level,c,()->level.setBlock(hazard,Blocks.LAVA.defaultBlockState(),818));
                require(game.items().indirectKills().allowEnvironmentDamage(beta,beta.damageSources().lava()),
                        "entity-write terrain context retains WILD for allied players");
                game.items().indirectKills().environmentDamage(beta,beta.damageSources().lava());
                require(beta.getKillCredit()==alpha,"terrain context retains actual trap author");
                clearBlock(hazard);game.items().indirectKills().clear(beta);
                beta.teleportTo(level,origin.x+35,origin.y+15,origin.z,Set.of(),0,0,true);
            }
            finally{board.removePlayerTeam(team);if(oldA!=null)board.addPlayerToTeam(alpha.getScoreboardName(),oldA);if(oldB!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldB);}
            var cow=new Cow(EntityTypes.COW,level);cow.setPos(Vec3.atCenterOf(base).add(0,10,1));cow.setNoAi(true);cow.setNoGravity(true);cow.setHealth(1);
            require(level.addFreshEntity(cow),"native disposable blast victim");owned.add(cow);
            require(level.getEntitiesOfClass(Cow.class,cow.getBoundingBox().inflate(1),e->e==cow).contains(cow),"native blast victim is present in entity-section query");
            for(String role:List.of("spectator","rcdenyalways","strict_map_object","sauvojen_tuntija","magic_academy","transformator")) {
                cow.entityTags().add(role);try{require(!game.items().creeperDrops().explosionTarget(c,cow),"protected native role "+role);}finally{cow.entityTags().remove(role);}
            }
            c.setPos(Vec3.atCenterOf(base).add(0,10,0));level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,false,game.server());
            var before=entities();for(int i=0;i<289;i++)c.tick();require(!c.isRemoved() && cow.getHealth()==1,"native delayed blast remains pending through289 ticks");
            c.tick();addedItems(before);require(c.isRemoved() && cow.isDeadOrDying() && cow.getKillCredit()==alpha,"native290 fuse explodes and credits captured trap thrower");
        }
        void rewards() {
            reset();ItemStack edible=null,donor=null;var seen=new HashSet<String>();
            for(int i=0;i<256 && seen.size()<4;i++)for(var item:game.lbc().rollTable(level,origin,alpha,"lbc:govno")) {
                require(govno(item) && item.getCount()==1 && item.getMaxStackSize()==1,"genuine full govno reward");
                seen.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
                if(item.is(LbcKfcFood.GOVNO_EATEN))edible=item.copy();else donor=item.copy();
            }
            require(seen.equals(Set.of("minecraft:cocoa_beans","minecraft:brown_dye","minecraft:nether_brick","skywars2:govno_eaten")) && edible!=null && donor!=null,"all four native table branches remain reachable");
            alpha.getFoodData().setFoodLevel(1);var before=entities();var food=edible.copy();var remaining=food.finishUsingItem(level,alpha);
            var returned=addedItems(before);require(remaining.isEmpty() && alpha.getFoodData().getFoodLevel()>1 && returned.stream().anyMatch(i->i.getItem().is(LbcKfcFood.GOVNO_EATEN) && govno(i.getItem())),"native food consumption restores nutrition and emits real reusable edible");
            var menu=alpha.inventoryMenu;var grid=menu.getCraftSlots();grid.clearContent();menu.setCarried(ItemStack.EMPTY);
            grid.setItem(0,new ItemStack(LbcElectro.CYBERKATANA));grid.setItem(3,donor);
            require(menu.getResultSlot().getItem().is(LbcElectro.SHIT_KATANA),"actual reward accepted by govno relic craft chain");
            GameChecks.inventoryClick(alpha,0,0,ContainerInput.PICKUP);
            require(menu.getCarried().is(LbcElectro.SHIT_KATANA) && grid.getItem(0).isEmpty() && grid.getItem(3).isEmpty(),"native craft consumes both inputs exactly once");grid.clearContent();menu.setCarried(ItemStack.EMPTY);
        }
        public void close() {
            if(!retainForStop){game.items().timeStop().end(level);game.items().ultraponos().stop();
                for(var c:actors(game)) {owned.add(c);c.discard();}game.items().creeperDrops().tick();}
            for(var e:owned){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
            for(var entry:blocks.entrySet()){if(level.getBlockEntity(entry.getKey()) instanceof Container c)c.clearContent();game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());}
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,griefing,game.server());
            game.config().lbcSkills=skills;game.players().get(alpha.getUUID()).lbcSkill=skillA;game.players().get(beta.getUUID()).lbcSkill=skillB;
            savedA.restore(game);savedB.restore(game);alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),mobKills);
            customStats.forEach((stat,value)->alpha.getStats().setValue(alpha,stat,value));
            itemStats.forEach((stat,value)->alpha.getStats().setValue(alpha,stat,value));
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    private static void translated(net.minecraft.network.chat.Component text,String key,int color) {
        require(text!=null && text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr && tr.getKey().equals(key)
                && !text.getStyle().isItalic() && text.getStyle().getColor()!=null && text.getStyle().getColor().getValue()==color,"source translated/style "+key);
    }
    public static void run(GameController game) {
        try(var fixture=new Fixture(game)) {
            fixture.initialize();fixture.presentation();fixture.geometry();fixture.cadence();fixture.mega();fixture.threshold();fixture.particles();fixture.guardsAndPause();fixture.suppression();fixture.nativeBlast();fixture.rewards();
            SkyWars2.LOGGER.info("[check] PASS ultraponos-drops");
        }
    }
    public static void stopProbe(GameController game) {
        require(game.playing(),"actual match stop probe");
        try(var f=new Fixture(game)) {
            f.initialize();f.activate();stopActors=List.copyOf(actors(game));
            stopEpoch=game.items().epoch();
            // close() normally owns actors; temporarily detach them for actual GameItems.stop().
            f.owned.removeAll(stopActors);
            game.items().timeStop().begin(f.level,200);stopToken=token(f.level,f.origin.add(0,4,0),3,f.alpha);
            require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"frozen pending token before stop");
            f.retainForStop=true;
            SkyWars2.LOGGER.info("[check] PASS ultraponos-drops-stop-probe");
        }
    }
    public static void cleanup(GameController game) {
        require(stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && stopToken!=null && stopToken.getItem().getCount()==3
                && game.items().ultraponos().emitterCount()==0 && stopActors.size()==4 && stopActors.stream().allMatch(Entity::isRemoved),"actual stop clears emitter and delayed actors, preserves pending debit");
        require(!game.items().ultraponos().ready(stopToken) && !game.items().specialDrops().spawn(stopToken),"off-stage old token cannot replay");
        stopToken.discard();stopToken=null;stopEpoch=null;stopActors=List.of();SkyWars2.LOGGER.info("[check] PASS ultraponos-drops-cleanup");
    }
}
