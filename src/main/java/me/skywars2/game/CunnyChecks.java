package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.Vec3;

/** Bounded actual-hand, native owner and whole-coordinator scenes; disposable server only. */
public final class CunnyChecks {
    private CunnyChecks() {}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Cunny: "+why);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){require(running==null,"exclusive fixture");var f=new Fixture(game,rewind);running=f;try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{require(f.game==game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} (actual used hands, source prototypes, finite native outcomes and typed fair terrain/causal state)",f.rewind?"cunny-rewind":"cunny-native");}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+(f.rewind?"cunny-rewind":"cunny-native"),error);}}
    private static Object get(Object object,String name){for(var c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}throw new IllegalStateException(name);}
    private static Object call(Object object,String name,Object...args){for(var c=object.getClass();c!=null;c=c.getSuperclass())for(var m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{m.setAccessible(true);return m.invoke(object,args);}catch(InvocationTargetException error){throw new IllegalStateException(error.getCause());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}throw new IllegalStateException(name);}
    private static Object op(String name,Object...args){try{for(var c:LbcCunnyTerrain.class.getDeclaredClasses())if(c.getSimpleName().equals(name)){var constructor=c.getDeclaredConstructors()[0];constructor.setAccessible(true);return constructor.newInstance(args);}}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}throw new IllegalStateException(name);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final ServerPlayer owner,other;final ServerLevel level;final Vec3 origin;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final Map<ServerPlayer,RegenerationBeltChecks.Snapshot> additionalSaved=new LinkedHashMap<>();
        final Map<ServerPlayer,LbcCloneSkills.ActorImage> psychoSaved=new LinkedHashMap<>();
        final List<ServerPlayer> affected;
        final SauvojenMeditationChecks.Attribution attribution;final LbcCunny service;final LbcCunny.State original;
        final Set<UUID> existing=new HashSet<>();final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Map<UUID,Map<BlockPos,MapManager.BlockSnapshot>> fieldTerrain=new HashMap<>();
        final boolean frozen,offhand;final int originalSkill;final String ownerTeam,otherTeam;
        net.minecraft.world.scores.PlayerTeam temporaryTeam;boolean lbcSkills,stopped,closed,restoring;int started,stage,when;
        LbcCunny.State captured;LbcCunnyTerrain.View capturedTerrain;List<Double> expectedAttrs;int expectedCoins;long expectedDraw;
        long replayOwnerStarts,replayOtherStarts;
        Object terrainJob;BlockPos platform,sign,replayBlock,treeAt,growthGround;MapManager.BlockSnapshot replayExpected;Cow witness;List<Object> terrainTail;Marker growthField;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"two actual ordinary participants and exclusive coordinator");
            require(!game.items().timeStop().stopped(level)&&game.items().cunnyTerrain().view().jobs()==0,"exclusive unpaused terrain owner");
            service=game.items().cunny();original=service.state();require(original.pairs().isEmpty()&&original.nahida().isEmpty()&&original.emissions().isEmpty(),"exclusive finite owner");
            affected=List.copyOf(game.server().getPlayerList().getPlayers());
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            for(var p:affected){if(p!=owner&&p!=other)additionalSaved.put(p,new RegenerationBeltChecks.Snapshot(game,p));var status=game.items().psycho().cloneStatusImage(p);if(status!=null)psychoSaved.put(p,status);}
            attribution=new SauvojenMeditationChecks.Attribution(game,affected);
            level.getAllEntities().forEach(e->existing.add(e.getUUID()));frozen=game.server().tickRateManager().isFrozen();offhand=game.config().offhandWands;lbcSkills=game.config().lbcSkills;originalSkill=game.players().get(other.getUUID()).lbcSkill;
            ownerTeam=owner.getTeam()==null?null:owner.getTeam().getName();otherTeam=other.getTeam()==null?null:other.getTeam().getName();
            origin=new Vec3(owner.getX(),Math.min(220,level.getMaxY()-70),owner.getZ());client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);started=game.server().getTickCount();
        }
        void next(int value){stage=value;when=game.server().getTickCount();}
        void move(ServerPlayer p,Vec3 at){require(p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual native teleport");p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);if(p==owner)client.acknowledge();else audience.acknowledge();}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        void remember(BlockPos at){terrain.putIfAbsent(at.immutable(),game.maps().snapshot(level,at));}
        void set(BlockPos at,net.minecraft.world.level.block.state.BlockState state){remember(at);level.setBlock(at,state,2);}
        void empty(){owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);}
        void begin(){
            game.server().tickRateManager().setFrozen(false);game.config().offhandWands=true;
            for(var p:List.of(owner,other)){p.closeContainer();p.stopUsingItem();game.lbc().clear(p);p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;game.items().mana().noMagic(p,0);game.items().magic().inventoryChanged(p);}
            move(owner,origin);move(other,origin.add(12,0,0));game.items().magic().flushCleansing();
            for(var p:List.of(owner,other)){var at=p.blockPosition().below();set(at,Blocks.STONE.defaultBlockState());}
            witness=new Cow(EntityTypes.COW,level);witness.setPos(origin.add(4,0,4));witness.setNoAi(true);witness.setNoGravity(true);require(level.addFreshEntity(witness),"actual grounded source witness");
            presentation();
            if(rewind){beginReplay();return;}
            finite();beginGrowth();
        }
        ItemStack variant(int n,int count){return new ItemStack(LbcCunnyItems.SLABS.get(n),count);}
        void use(int n,InteractionHand hand){
            owner.stopUsingItem();var unused=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            owner.setItemInHand(unused,variant(n,5));owner.setItemInHand(hand,variant(n,3));long accepted=service.state().accepted();
            require(owner.getItemInHand(hand).getItem().use(level,owner,hand).consumesAction(),"real item use accepts outcome"+n+" "+hand);
            require(owner.getItemInHand(hand).getCount()==2&&owner.getItemInHand(unused).getCount()==5&&service.state().accepted()==accepted+1,"only actual used hand debited once outcome"+n);
        }
        void presentation(){
            var source=LbcCunnyItems.SOURCE.getAsJsonObject("imba/cunny_slabs").getAsJsonArray("pools").get(0).getAsJsonObject();
            require(source.getAsJsonArray("entries").size()==22&&source.getAsJsonObject("rolls").get("max").getAsInt()==8&&source.getAsJsonObject("bonus_rolls").get("max").getAsInt()==8,"exact22 equal source entries and1..8 plus0..8 bundle");
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)client.connection;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                for(int n=0;n<23;n++){var stack=n==22?new ItemStack(LbcCunnyItems.NAHIDA):variant(n,1);require(n==22?LbcCunnyItems.nahida(stack):LbcCunnyItems.variant(stack)==n,"canonical source identity"+n);
                    require(stack.get(DataComponents.ITEM_MODEL)!=null&&stack.get(DataComponents.CUSTOM_NAME)!=null&&stack.get(DataComponents.LORE)!=null,"complete source presentation"+n);
                    if(n<22)require(stack.getMaxStackSize()==99&&!stack.isDamageableItem(),"source slab99 stack and no damage"+n);
                    var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),owner.registryAccess());
                    require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),owner.registryAccess())),"Polymer all components roundtrip"+n);
                }
            });
        }
        List<Double> attrs(){var result=new ArrayList<Double>();for(var holder:BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=owner.getAttribute(holder);if(a!=null)result.add(a.getBaseValue());}return List.copyOf(result);}
        void seed(long value){var s=service.state();service.restore(new LbcCunny.State(s.pairs(),s.nahida(),s.emissions(),s.phase(),MatchRandomState.capture(RandomSource.create(value)),s.accepted(),s.kills(),s.summons()));}
        void pair(InteractionHand hand){
            // Preserve the real uniform runtime sampler and actual candidate order with any lobby size.
            var candidates=level.players().stream().filter(p->(Boolean)call(service,"hostile",owner,p)&&!p.isSpectator()).toList();int index=candidates.indexOf(other);
            require(index>=0,"chosen pair witness belongs to actual eligible source pool");long choice=-1;
            for(long s=0;s<100000&&choice<0;s++)if(RandomSource.create(s).nextInt(candidates.size())==index)choice=s;
            require(choice>=0,"bounded deterministic source pair seed");seed(choice);int before=service.state().pairs().size();use(6,hand);
            require(service.state().pairs().size()==before+1&&service.state().pairs().getLast().seeker().equals(owner.getUUID())&&service.state().pairs().getLast().target().equals(other.getUUID()),"actual enemy pair from real participant pool");
        }
        void enemies(){var board=game.server().getScoreboard();board.removePlayerFromTeam(owner.getScoreboardName());board.removePlayerFromTeam(other.getScoreboardName());}
        void allies(){enemies();var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_cunny_fixture")==null,"exclusive scoped ally team");temporaryTeam=board.addPlayerTeam("sw2_cunny_fixture");temporaryTeam.setAllowFriendlyFire(false);board.addPlayerToTeam(owner.getScoreboardName(),temporaryTeam);board.addPlayerToTeam(other.getScoreboardName(),temporaryTeam);}
        void armor(){for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))other.setItemSlot(slot,new ItemStack(Items.LEATHER_BOOTS));}
        boolean armored(){return !other.getItemBySlot(EquipmentSlot.CHEST).isEmpty();}
        Marker field(boolean tech,Vec3 at){var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().add(tech?"stopper":"stopper_magic");var local=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();for(var pos:List.of(marker.blockPosition(),marker.blockPosition().above())){remember(pos);local.put(pos,game.maps().snapshot(level,pos));}fieldTerrain.put(marker.getUUID(),local);require(level.addFreshEntity(marker),"native suppression field");game.items().magic().loaded(marker);return marker;}
        void removeField(Marker marker){
            game.items().magic().unloaded(marker);marker.discard();var local=fieldTerrain.remove(marker.getUUID());
            // TECH legacy markers become real native beacon blocks on pulse. Remove only that
            // carrier using its immediate placement image; unrelated intervening growth survives.
            if(local!=null)for(var entry:local.entrySet())if(level.getBlockState(entry.getKey()).is(LbcMagic.MECHANISM_BEACON_BLOCK)||level.getBlockState(entry.getKey()).is(LbcMagic.MAGIC_BEACON_BLOCK)){
                game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),2);game.items().magic().blockChanged(entry.getKey(),level.getBlockState(entry.getKey()));
            }
        }
        void suppression(){
            var mag=field(false,owner.position());require(game.items().magic().suppressed(owner),"actual MAG field positive control");owner.setItemInHand(InteractionHand.MAIN_HAND,variant(11,3));long accepted=service.state().accepted();
            require(!owner.getMainHandItem().getItem().use(level,owner,InteractionHand.MAIN_HAND).consumesAction()&&owner.getMainHandItem().getCount()==3&&service.state().accepted()==accepted,"MAG rejects slab activation without debit");
            nahida();removeField(mag);
            var tech=field(true,owner.position());require(game.items().magic().mechanismSuppressed(owner),"actual TECH field positive control");nahida();removeField(tech);
        }
        void finite(){
            enemies();armor();use(1,InteractionHand.OFF_HAND);require(!armored(),"source hostile armor removed");
            armor();game.config().lbcSkills=true;game.players().get(other.getUUID()).lbcSkill=2;
            require(game.items().inventoryHazards().protectedInventory(other),"canonical ProtectedInventory positive witness");use(1,InteractionHand.MAIN_HAND);require(armored(),"ProtectedInventory preserves armor");game.players().get(other.getUUID()).lbcSkill=originalSkill;game.config().lbcSkills=lbcSkills;
            empty();use(2,InteractionHand.MAIN_HAND);var buff=Identifier.parse("minecraft:cunny2buffs");require(owner.getAttribute(Attributes.ARMOR).getModifier(buff).amount()==6&&owner.getAttribute(Attributes.SCALE).getModifier(buff).amount()==-.5,"exact authored permanent buff");
            var before=owner.getAttributeValue(Attributes.ARMOR);use(2,InteractionHand.OFF_HAND);require(owner.getAttributeValue(Attributes.ARMOR)==before,"named permanent buff idempotence");
            for(int n=11;n<=20;n++){var old=service.state();var terrainBefore=game.items().cunnyTerrain().view();use(n,n%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);var now=service.state();require(now.kills()==old.kills()&&now.summons()==old.summons()&&now.pairs().equals(old.pairs())&&now.nahida().equals(old.nahida())&&now.emissions().equals(old.emissions())&&game.items().cunnyTerrain().view().equals(terrainBefore),"source inert terminal"+n);}
            use(3,InteractionHand.MAIN_HAND);require(owner.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)&&game.items().cunnyTerrain().view().grass()>0,"infinite regeneration and real frontier");game.items().cunnyTerrain().stop();
            var prior=new HashSet<>(game.items().summons().snapshot());long count=service.state().summons();use(4,InteractionHand.OFF_HAND);
            var added=game.items().summons().snapshot().stream().filter(m->!prior.contains(m)).toList();require(added.size()==20&&service.state().summons()==count+20,"exact20 native summons including repeated offsets");
            for(var mob:added){var kinds=List.of("strangeness","chaos","luck","omnipresence","obsession","hypocrisy");int kind=-1;for(int i=0;i<kinds.size();i++)if(mob.entityTags().contains(kinds.get(i)))kind=i;require(kind>=0&&mob.entityTags().contains("summactivated"),"native source cultist identity");preset(mob,kind);game.items().summons().unloaded(mob);mob.discard();}
            for(int n=1;n<=6;n++){var mob=game.items().summons().cunnyCultist(owner,origin.add(3,0,3),n);preset(mob,n-1);game.items().summons().unloaded(mob);mob.discard();}
            empty();use(5,InteractionHand.MAIN_HAND);require(java.util.stream.IntStream.range(0,owner.getInventory().getContainerSize()).anyMatch(i->LbcCunnyItems.nahida(owner.getInventory().getItem(i))),"canonical Nahida reward");
            pair(InteractionHand.OFF_HAND);
            other.setOnGround(false);other.setDeltaMovement(Vec3.ZERO);service.tick();require(other.getDeltaMovement().length()>.09&&game.items().indirectKills().altitudeAuthor(other)==owner,"airborne pair motion and causal fall author");
            var targetMag=field(false,other.position().add(19.5,0,0));require(game.items().magic().suppressedAt(level,other.position())&&!game.items().magic().suppressed(owner),"MAG protects target outside owner field");other.setDeltaMovement(Vec3.ZERO);service.tick();require(other.getDeltaMovement().equals(Vec3.ZERO),"MAG target pauses pair displacement");removeField(targetMag);
            pause();var pairState=service.state();var motion=other.getDeltaMovement();service.tick();require(service.state().phase()==pairState.phase()&&other.getDeltaMovement().equals(motion),"TimeStop pauses pair native clock and motion");thaw();other.setOnGround(true);
            use(7,InteractionHand.MAIN_HAND);require(java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).filter(net.minecraft.world.entity.item.ItemEntity.class::isInstance).map(net.minecraft.world.entity.item.ItemEntity.class::cast).anyMatch(e->LbcFartus.data(e.getItem()).getStringOr("skywars2.whip_target","").equals(other.getUUID().toString())),"canonical full UUID whip target");
            allies();use(8,InteractionHand.OFF_HAND);require(service.state().emissions().containsKey(other.getUUID())&&game.items().mana().hudValue(other,"twitching")>=60,"global WILD effect includes ordinary teammate");enemies();
            for(var p:affected)if((Boolean)call(service,"active",p))require(service.state().emissions().containsKey(p.getUUID()),"global WILD effect includes actual participant "+p.getScoreboardName());
            nahida();suppression();
            empty();var candidates=new ArrayList<Entity>();for(var e:level.getAllEntities())if((Boolean)call(service,"killCandidate",owner,e))candidates.add(e);
            long chosen=-1;for(long s=0;s<10000&&chosen<0;s++){var random=RandomSource.create(s);Entity target=null;int seen=0;for(var e:candidates)if(random.nextInt(++seen)==0)target=e;if(target==witness)chosen=s;}
            require(chosen>=0,"deterministic native random selection seed");seed(chosen);use(10,InteractionHand.MAIN_HAND);require(!witness.isAlive()||witness.isRemoved(),"actual random entity kill kernel");
            var protectedActor=new Marker(EntityTypes.MARKER,level);protectedActor.setPos(origin);protectedActor.entityTags().add("strict_map_object");require(level.addFreshEntity(protectedActor),"native protected service actor");require(!service.randomKill(owner,protectedActor)&&!protectedActor.isRemoved(),"protected service actor exclusion");protectedActor.discard();
            var target=new Cow(EntityTypes.COW,level);target.setPos(origin.add(2,0,2));target.setNoAi(true);require(level.addFreshEntity(target),"Chef target body");var chef=game.items().wtf().spawn(owner,target);require(chef!=null,"actual native Chef publication");require(service.randomKill(owner,chef)&&chef.isRemoved()&&!game.items().wtf().snapshot().contains(chef),"Cunny permanently erases Chef authority and body");target.discard();
            count=service.state().summons();use(21,InteractionHand.OFF_HAND);require(service.state().summons()>count&&service.state().summons()<=count+64,"bounded actual21 reservoir summon");
            for(var mob:List.copyOf(game.items().summons().snapshot()))if(!existing.contains(mob.getUUID())){game.items().summons().unloaded(mob);mob.discard();}
            clearAffectedStatuses();owner.stopUsingItem();
            // Every preceding outcome has already been checked. Retire its scoped bodies/loot before
            // testing proximity cloning; a native killed cow remains a selectable corpse this tick.
            removeScopedActors();
            cloneTerrain();
        }
        void cloneTerrain(){
            var terrain=game.items().cunnyTerrain();var clones=game.items().cloneSkills();int own=game.players().get(owner.getUUID()).lbcSkill,holder=game.players().get(other.getUUID()).lbcSkill;boolean ranked=game.config().ranked,skills=game.config().lbcSkills;
            var counters=clones.checkpoint();
            try{
                move(other,origin.add(3,0,0));game.config().ranked=true;game.config().lbcSkills=true;game.players().get(owner.getUUID()).lbcSkill=0;game.players().get(other.getUUID()).lbcSkill=15;
                require(terrain.grass(owner),"one actual indexed grass marker");require(terrain.actorViews(16).size()==1&&game.lbc().hasSkill(other,15),"one virtual candidate and admitted actual x2 holder");
                long bodies=(Long)get(clones,"bodies");for(int n=0;n<16&&terrain.actorViews(16).size()<2;n++)clones.tick();require(terrain.actorViews(16).size()==2&&(Long)get(clones,"bodies")==bodies+1,"canonical shared scanner x2 counter and actual marker");
                game.players().get(other.getUUID()).lbcSkill=17;for(int n=0;n<16&&terrain.actorViews(16).size()<8;n++)clones.tick();var eight=terrain.actorViews(16);
                require(eight.size()==8&&(Long)get(clones,"bodies")==bodies+7,"canonical shared scanner x4 counters and both source markers: markers="+eight+", "+cloneDiagnostic(bodies));
                require(eight.stream().allMatch(a->a.author().equals(owner.getUUID())&&a.cause().player().equals(owner.getUUID())&&a.mask()==3),"clones retain original causal author and both flags");
                clones.tick();game.players().get(other.getUUID()).lbcSkill=15;clones.tick();require(terrain.actorViews(16).size()==8&&(Long)get(clones,"bodies")==bodies+7,"repeat shared scans cannot clone twice");
            }finally{terrain.stop();game.config().ranked=ranked;game.config().lbcSkills=skills;game.players().get(owner.getUUID()).lbcSkill=own;game.players().get(other.getUUID()).lbcSkill=holder;move(other,origin.add(12,0,0));counters.prepare(null).run();}
        }
        String cloneDiagnostic(long baseline){
            var clones=game.items().cloneSkills();var rows=new ArrayList<String>();
            for(var e:level.getAllEntities())if(!(e instanceof ServerPlayer)&&e.position().distanceToSqr(other.position())<=256&&rows.size()<32)
                rows.add(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())+"/"+e.getUUID()+" alive="+e.isAlive()+" removed="+e.isRemoved()+" at="+e.position());
            return "bodiesDelta="+((Long)get(clones,"bodies")-baseline)+", scanned="+get(clones,"scanned")+", operations="+get(clones,"operations")+", unsupported="+get(clones,"unsupported")+", failed="+get(clones,"failed")+", nearby="+rows;
        }
        void removeScopedActors(){
            var remove=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e.getUUID())&&!(e instanceof ServerPlayer))remove.add(e);
            for(var e:remove){game.server().getScoreboard().resetAllPlayerScores(e);game.server().getScoreboard().removePlayerFromTeam(e.getScoreboardName());game.items().summons().unloaded(e);game.items().wtf().unloaded(e);game.items().magic().unloaded(e);if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
        }
        void beginGrowth(){startGrowth(false);}
        void startGrowth(boolean tech){
            growthGround=owner.blockPosition().below();set(growthGround,Blocks.DIRT.defaultBlockState());set(growthGround.above(),Blocks.AIR.defaultBlockState());set(growthGround.above(2),Blocks.AIR.defaultBlockState());
            var fieldAt=owner.blockPosition().offset(10,0,0);set(fieldAt,Blocks.AIR.defaultBlockState());set(fieldAt.above(),Blocks.AIR.defaultBlockState());
            require(game.items().cunnyTerrain().grass(owner),"accepted real autonomous grass");growthField=field(tech,Vec3.atCenterOf(fieldAt));
            require(level.getBlockState(growthGround).is(Blocks.DIRT)&&level.getBlockState(growthGround.above()).isAir()&&level.getBlockState(growthGround.above(2)).isAir(),"native field loading preserves independent grass test surface");next(tech?-4:-5);
        }
        String growthDiagnostic(){
            var jobRows=new ArrayList<String>();for(var job:(Set<?>)get(game.items().cunnyTerrain(),"jobs")){if(jobRows.size()==4)break;var ops=(Deque<?>)get(job,"ops");jobRows.add("logical="+get(job,"logicalTick")+", last="+get(job,"lastStepTick")+", closed="+get(job,"closed")+", blocked="+get(job,"blocked")+", tickets="+((Map<?,?>)get(job,"tickets")).size()+", next="+ops.peek());}
            var airs=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse("minecraft:airs"));var blocks=new ArrayList<String>();
            for(var at:List.of(growthGround,growthGround.above(),growthGround.above(2),owner.blockPosition()))blocks.add(at+"="+level.getBlockState(at)+" AIRS="+level.getBlockState(at).is(airs));
            return "stage="+stage+", tick="+game.server().getTickCount()+", owner="+owner.position()+", origin="+origin+", noGravity="+owner.isNoGravity()+", stopped="+stopped+"/"+game.items().timeStop().stopped(level)+", frozenOwner="+game.items().timeStop().frozen(owner)+", legacyStop="+game.items().wands().timeStopped()+", serverFrozen="+game.server().tickRateManager().isFrozen()+", runsNormally="+game.server().tickRateManager().runsNormally()+", view="+game.items().cunnyTerrain().view()+", actors="+game.items().cunnyTerrain().actorViews(8)+", sharedWork="+((Collection<?>)get(game.items().stonekivi(),"work")).size()+", deadlineRemainingNanos="+(EffectTerrain.deadlineNanos()-System.nanoTime())+", blocks="+blocks+", jobs="+jobRows;
        }
        void preset(Mob mob,int n){
            String[] kinds={"strangeness","chaos","luck","omnipresence","obsession","hypocrisy"};
            String[] assets={"black","chaos","luck","white","femboy","hypo"};
            Item[] weapons={Items.BOW,Items.HEART_OF_THE_SEA,Items.TOTEM_OF_UNDYING,Items.BOW,Items.DIAMOND_AXE,Items.IRON_SWORD};
            double[] health={30,50,40,40,10,60},scale={.7,.55,.65,.65,.6,.6};
            require(mob!=null&&mob.getMaxHealth()==health[n]&&mob.getHealth()==health[n]
                    &&Math.abs(mob.getAttributeValue(Attributes.SCALE)-scale[n])<1e-6
                    &&mob.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue()==100
                    &&mob.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER).getBaseValue()==0,"authored health/scale/follow/fall attributes "+kinds[n]);
            require(mob.getType()==(n==0?EntityTypes.WITHER_SKELETON:n==5?EntityTypes.ZOMBIE:EntityTypes.SKELETON)
                    &&mob.entityTags().contains(kinds[n])&&mob.isSilent()&&game.items().summons().owner(mob)==owner,"authored native body/tags/silence/owner "+kinds[n]);
            require(mob.getMainHandItem().is(weapons[n])&&mob.getMainHandItem().getCount()==1
                    &&(n==5?mob.getOffhandItem().is(Items.IRON_SWORD):mob.getOffhandItem().isEmpty()),"authored main/offhand weapons "+kinds[n]);
            var slots=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
            var armor=List.of(Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS);
            for(int i=0;i<slots.size();i++){
                var stack=mob.getItemBySlot(slots.get(i));var equipment=stack.get(DataComponents.EQUIPPABLE);
                require(stack.is(armor.get(i))&&stack.getCount()==1&&stack.has(DataComponents.UNBREAKABLE)&&equipment!=null&&equipment.slot()==slots.get(i)
                        &&equipment.assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:cultus_"+assets[n]+"_loli")),"authored armor item/asset/slot/unbreakable "+kinds[n]+" "+slots.get(i));
            }
            if(n==2){
                require(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue()==10&&mob.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).getBaseValue()==100,"source Luck resistance base values");
                var protection=owner.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BLAST_PROTECTION);
                require(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(protection,mob.getItemBySlot(EquipmentSlot.FEET))==99,"source Luck boot blast protection99");
            }
            if(n==4)require(mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==11&&mob.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()==.25,"source Obsession attack11 and speed.25");
            if(n==5)require(mob.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()==.2,"source Hypocrisy speed.2");
        }
        void nahida(){
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcCunnyItems.NAHIDA));var baseline=attrs();
            long starts=sounds(client,false),audienceStarts=sounds(audience,false),stops=sounds(client,true),audienceStops=sounds(audience,true);
            require(owner.getMainHandItem().getItem().use(level,owner,InteractionHand.MAIN_HAND).consumesAction()&&owner.isUsingItem()&&owner.getUseItemRemainingTicks()>0,"actual sustained Nahida native use");
            owner.getUseItem().getItem().onUseTick(level,owner,owner.getUseItem(),owner.getUseItemRemainingTicks());
            require(owner.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(Identifier.parse("minecraft:plapper_debuff"))&&!attrs().equals(baseline),"transient negative layer and actual permanent random base gain");
            require(sounds(client,false)==starts+1&&sounds(audience,false)==audienceStarts+1,"first maintained Nahida tick sends one source global MASTER bafled start");
            owner.getUseItem().getItem().onUseTick(level,owner,owner.getUseItem(),owner.getUseItemRemainingTicks());
            require(sounds(client,false)==starts+1&&sounds(audience,false)==audienceStarts+1,"continued maintained use does not repeat source audio start");
            pause();var before=attrs();owner.getUseItem().getItem().onUseTick(level,owner,owner.getUseItem(),owner.getUseItemRemainingTicks());require(attrs().equals(before),"TimeStop pauses Nahida gains");thaw();owner.stopUsingItem();service.tick();
            require(!owner.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(Identifier.parse("minecraft:plapper_debuff"))&&attrs().equals(before),"release removes transient layer and retains permanent base gain");
            require(sounds(client,true)==stops+1&&sounds(audience,true)==audienceStops+1,"release sends one global MASTER bafled stop packet");
        }
        long sounds(RegenerationBeltChecks.ScheduledClient listener,boolean stop){
            var id=Identifier.parse("minecraft:lbcsounds.bafled");long count=0;
            for(var packet:(List<?>)get(listener.connection,"packets")){
                if(!stop&&packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound&&sound.getSound().value().location().equals(id)){
                    require(sound.getSource()==net.minecraft.sounds.SoundSource.MASTER&&sound.getVolume()==2&&sound.getPitch()==1,"exact source Nahida sound category/volume/pitch");count++;
                }
                if(stop&&packet instanceof net.minecraft.network.protocol.game.ClientboundStopSoundPacket sound&&id.equals(sound.getName())){
                    require(sound.getSource()==net.minecraft.sounds.SoundSource.MASTER,"exact source Nahida stop category");count++;
                }
            }return count;
        }
        @SuppressWarnings("unchecked") Object job(Object...ops){var result=call(game.items().cunnyTerrain(),"start",owner);var queue=(Deque<Object>)get(result,"ops");for(var op:ops)queue.add(op);return result;}
        @SuppressWarnings("unchecked") void rememberTemplate(BlockPos at,int kind){var rows=(List<List<Object>>)get(game.items().cunnyTerrain(),"templates");for(var cell:rows.get(kind))remember(at.offset((BlockPos)call(cell,"offset")));}
        void beginTerrain(){
            require(game.items().cunnyTerrain().ready(),"prepared four source templates and100 sign faces");
            platform=new BlockPos(owner.blockPosition().getX()+48,100,owner.blockPosition().getZ());for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)remember(platform.offset(x,0,z));
            sign=owner.blockPosition().offset(8,0,8);set(sign,Blocks.AIR.defaultBlockState());set(sign.below(),Blocks.AIR.defaultBlockState());
            var template=owner.blockPosition().offset(16,0,16);rememberTemplate(template,0);
            treeAt=owner.blockPosition().offset(-8,0,-8);for(int x=0;x<2;x++)for(int z=0;z<2;z++){set(treeAt.offset(x,-1,z),Blocks.DIRT.defaultBlockState());set(treeAt.offset(x,0,z),Blocks.AIR.defaultBlockState());}
            terrainJob=job();terrainTail=List.of(op("Teleport",platform.getX(),platform.getZ()),op("Corrector",sign,0,3,0,1,0),op("Template",template,0,net.minecraft.world.level.block.Rotation.NONE,0));pause();next(-2);
        }
        void beginReplay(){
            enemies();pair(InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcCunnyItems.NAHIDA));owner.getMainHandItem().getItem().use(level,owner,InteractionHand.MAIN_HAND);service.nahidaUseTick(owner);
            require(game.items().cunnyTerrain().grass(owner),"nonempty accepted grass frontier");var at=owner.blockPosition().offset(16,0,16);rememberTemplate(at,0);terrainJob=job(op("Template",at,0,net.minecraft.world.level.block.Rotation.NONE,0));
            // The normal shared scheduler must publish the partial template in an active tick.
            next(9);
        }
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<1000,"bounded native fixture state="+stage);
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(rewind)return replayStep();
            if(stage==-5||stage==-4){
                if(!level.getBlockState(growthGround).is(Blocks.GRASS_BLOCK)){if(game.server().getTickCount()-when>=80)require(false,"native grass advances under suppression: "+growthDiagnostic());return false;}
                if(stage==-4&&!level.getBlockState(growthField.blockPosition()).is(LbcMagic.MECHANISM_BEACON_BLOCK)){require(game.server().getTickCount()-when<80,"TECH field migrates to actual native carrier: "+growthDiagnostic());return false;}
                require(stage==-5?game.items().magic().suppressed(owner):game.items().magic().mechanismSuppressed(owner),"actual field remains during grass publication");
                game.items().cunnyTerrain().stop();removeField(growthField);growthField=null;
                if(stage==-5){startGrowth(true);return false;}
                beginTerrain();return false;
            }
            if(stage==-2){
                boolean resident=true;for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)resident&=(Boolean)call(terrainJob,"resident",treeAt.offset(x*16,0,z*16));
                if(!resident){var tickets=(Map<?,?>)get(terrainJob,"tickets");if(tickets.values().stream().allMatch(f->((java.util.concurrent.CompletableFuture<?>)f).isDone()))call(terrainJob,"release");return false;}
                call(terrainJob,"execute",op("Tree",treeAt));var queue=(Deque<?>)get(terrainJob,"ops");require(queue.stream().anyMatch(o->o.getClass().getSimpleName().equals("Emit"))&&queue.stream().allMatch(o->o.getClass().getSimpleName().equals("Emit")||o.getClass().getSimpleName().equals("Schedule")),"real configured mega_jungle_tree creates native queued overlay");
                for(var emit:queue)if(emit.getClass().getSimpleName().equals("Emit"))remember((BlockPos)call(emit,"at"));require(game.items().cunnyTerrain().view().writes()==0&&level.getBlockState(treeAt).isAir(),"native feature planning publishes no blocks before shared budget");
                @SuppressWarnings("unchecked") var mutable=(Deque<Object>)queue;mutable.addAll(terrainTail);next(0);return false;
            }
            if(stage==0){if(game.server().getTickCount()-when<5)return false;require(game.items().cunnyTerrain().view().writes()==0,"TimeStop holds actual source terrain queue");thaw();next(1);return false;}
            if(stage==1){if(game.items().cunnyTerrain().view().jobs()!=0)return false;
                require(owner.getY()>300&&owner.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"source9 actualY320 teleport and1200tick slowfall");
                require(level.getBlockState(platform.offset(-16,0,-16)).is(Blocks.QUARTZ_BLOCK)&&level.getBlockState(platform.offset(16,0,16)).is(Blocks.QUARTZ_BLOCK),"complete33x33 platform through shared work");
                require(level.getBlockState(treeAt).is(Blocks.JUNGLE_LOG),"actual complete native mega jungle tree published through shared writes");
                require(level.getBlockState(sign.below()).is(Blocks.BARREL)&&level.getBlockEntity(sign) instanceof SignBlockEntity s&&s.isWaxed()&&s.getText(SignTextSlot.FRONT).hasGlowingText()&&s.getText(SignTextSlot.BACK).hasGlowingText(),"native source corrector barrel and waxed glowing two-face sign");return true;}
            return false;
        }
        long draw(){var rng=RandomSource.create();service.state().random().restore(rng);return rng.nextLong();}
        boolean replayStep(){
            if(stage==9){
                require(!game.items().timeStop().stopped(level),"partial source writes require running world time");
                if((Long)get(terrainJob,"writes")==0){require(game.server().getTickCount()-when<80,"normal shared scheduler reaches template work");return false;}
                require(!((Deque<?>)get(terrainJob,"ops")).isEmpty()&&game.items().cunnyTerrain().view().grass()>0,"actual partial source template and nonempty frontier");
                pause();require(game.vanity().requestCapture(owner,false),"whole coordinator accepts pair/Nahida/RNG and partial native terrain");next(10);return false;
            }
            if(stage==10){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                captured=service.state();capturedTerrain=game.items().cunnyTerrain().view();require(!captured.pairs().isEmpty()&&!captured.nahida().isEmpty()&&capturedTerrain.pending()>0,"nonempty whole coordinator image");
                replayBlock=owner.blockPosition().below();replayExpected=game.maps().snapshot(level,replayBlock);
                thaw();owner.startUsingItem(InteractionHand.MAIN_HAND);service.nahidaUseTick(owner);expectedAttrs=attrs();expectedDraw=draw();expectedCoins=game.players().get(owner.getUUID()).coins;
                pause();service.stop();game.items().cunnyTerrain().stop();game.players().get(owner.getUUID()).coins=777777;level.setBlock(replayBlock,Blocks.GOLD_BLOCK.defaultBlockState(),2);
                replayOwnerStarts=sounds(client,false);replayOtherStarts=sounds(audience,false);
                require(game.vanity().requestRestoreForCheck(),"actual whole coordinator restore");restoring=true;next(11);return false;
            }
            if(game.vanity().busy())return false;
            require(service.state().pairs().equals(captured.pairs())&&service.state().nahida().equals(captured.nahida())&&service.state().accepted()==captured.accepted(),"pair/Nahida/accepted cursor restored");
            require(game.items().cunnyTerrain().view().equals(capturedTerrain),"exact partial template/frontier visits writes restored");
            require(level.getBlockState(replayBlock).equals(replayExpected.state()),"actual captured terrain block restored after future mutation");
            require(game.items().timeStop().stopped(level),"native TimeStop image restored");
            require(sounds(client,false)==replayOwnerStarts+1&&sounds(audience,false)==replayOtherStarts+1,"whole restore publishes active Nahida audio once from start on both listeners");
            game.vanity().stop();thaw();owner.startUsingItem(InteractionHand.MAIN_HAND);service.nahidaUseTick(owner);
            require(attrs().equals(expectedAttrs)&&draw()==expectedDraw&&game.players().get(owner.getUUID()).coins==expectedCoins,"same restored random permanent gain and canonical balance");return true;
        }
        void restoreTeams(){var board=game.server().getScoreboard();board.removePlayerFromTeam(owner.getScoreboardName());board.removePlayerFromTeam(other.getScoreboardName());if(temporaryTeam!=null)board.removePlayerTeam(temporaryTeam);if(ownerTeam!=null&&board.getPlayerTeam(ownerTeam)!=null)board.addPlayerToTeam(owner.getScoreboardName(),board.getPlayerTeam(ownerTeam));if(otherTeam!=null&&board.getPlayerTeam(otherTeam)!=null)board.addPlayerToTeam(other.getScoreboardName(),board.getPlayerTeam(otherTeam));}
        void clearAffectedStatuses(){for(var p:affected){call(game.items().psycho(),"removeEffect",p);service.forget(p);}}
        void restorePlayers(){
            clearAffectedStatuses();owner.stopUsingItem();other.stopUsingItem();
            saved.restoreAttributes();otherSaved.restoreAttributes();additionalSaved.values().forEach(RegenerationBeltChecks.Snapshot::restoreAttributes);
            saved.restore(game);otherSaved.restore(game);additionalSaved.values().forEach(s->s.restore(game));
            saved.restoreUse();otherSaved.restoreUse();additionalSaved.values().forEach(RegenerationBeltChecks.Snapshot::restoreUse);
            service.restore(original);game.config().offhandWands=offhand;game.config().lbcSkills=lbcSkills;
            for(var p:affected)game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            psychoSaved.forEach((p,image)->image.install(p,p,Map.of(p,p)));game.players().save();
        }
        public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->{if(rewind)game.vanity().stop();service.stop();game.items().cunnyTerrain().stop();if(stopped)thaw();},
                    this::removeScopedActors,
                    ()->terrain.forEach((p,s)->{game.maps().restoreSnapshot(level,p,s,2);game.items().magic().blockChanged(p,level.getBlockState(p));}),
                    this::restorePlayers,this::restoreTeams,client::close,audience::close,attribution::close,
                    ()->game.server().tickRateManager().setFrozen(frozen)))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Cunny scoped cleanup",failure);
        }
    }
}
