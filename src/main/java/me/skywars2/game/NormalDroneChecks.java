package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.util.*;

/** Closed normal-drone acquisition, native reward/right-click airstrike and actual-stop fixtures. */
public final class NormalDroneChecks {
    private NormalDroneChecks() { }
    private static State running,stopping,retained;
    private static ItemEntity stopToken;
    private static String stopEpoch;
    private static List<Entity> stopActors=List.of();
    private static Set<Entity> stopDebris=Collections.newSetFromMap(new IdentityHashMap<>());
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Normal drone: "+text);}
    private static CompoundTag saved(Entity e,ServerLevel level){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());e.saveWithoutId(out);return out.buildResult();}
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final Vec3 origin=new Vec3(-280.63,150.7,-280.19);final BlockPos center=BlockPos.containing(origin);
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>()),mine=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ItemEntity> tokens=new ArrayList<>();final Set<Long> forced=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final List<ScheduledTick<net.minecraft.world.level.block.Block>> blockTicks=new ArrayList<>();
        final List<ScheduledTick<net.minecraft.world.level.material.Fluid>> fluidTicks=new ArrayList<>();
        final BoundingBox bounds;final boolean mobDrops,tntExplodes;final int protection,alphaKills,betaKills;
        int cooldownBefore,lifeTicks,deathWait; float firstHealth; LivingEntity bomber; Vec3 bomberStart; int produced; boolean sawHurt; final boolean offhand; final Map<ServerPlayer,Map<net.minecraft.stats.Stat<Identifier>,Integer>> stats=new LinkedHashMap<>(); int phase,lastTick,warmTicks,wait;ItemEntity stack,paused,unloaded;PrimedTnt frozen;MinecartSpawner producer;Vec3 frozenPos;int frozenFuse,frozenDelay;
        State(GameController game){this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(game.playing()&&alpha!=null&&beta!=null&&game.allowDamage(beta),"two actual match participants");alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());level.getAllEntities().forEach(existing::add);bounds=new BoundingBox(center.getX()-12,center.getY()-12,center.getZ()-12,center.getX()+12,center.getY()+34,center.getZ()+12);mobDrops=level.getGameRules().get(GameRules.MOB_DROPS);tntExplodes=level.getGameRules().get(GameRules.TNT_EXPLODES);protection=game.config().tntProtection;alphaKills=kills(alpha);betaKills=kills(beta);offhand=game.config().offhandWands;cooldownBefore=game.items().mana().score(alpha,"coldownhohol").get();for(var p:List.of(alpha,beta)){var map=new LinkedHashMap<net.minecraft.stats.Stat<Identifier>,Integer>();for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);map.put(stat,p.getStats().getValue(stat));}stats.put(p,map);}}
        int kills(ServerPlayer p){return p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));}
        void initialize(){for(int x=(center.getX()-64)>>4;x<=(center.getX()+64)>>4;x++)for(int z=(center.getZ()-64)>>4;z<=(center.getZ()+64)>>4;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}var c=level.getChunkAt(new BlockPos(x*16,center.getY(),z*16));((LevelChunkTicks<net.minecraft.world.level.block.Block>)c.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);((LevelChunkTicks<net.minecraft.world.level.material.Fluid>)c.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);}
            for(var p:BlockPos.betweenClosed(new BlockPos(bounds.minX(),bounds.minY(),bounds.minZ()),new BlockPos(bounds.maxX(),bounds.maxY(),bounds.maxZ())))blocks.put(p.immutable(),game.maps().snapshot(level,p));
            for(var p:List.of(alpha,beta)){p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);reset(p);}level.getGameRules().set(GameRules.MOB_DROPS,true,game.server());level.getGameRules().set(GameRules.TNT_EXPLODES,true,game.server());game.config().tntProtection=0;game.config().offhandWands=true;clearTerrain();place();lastTick=game.server().getTickCount();}
        void place(){alpha.teleportTo(level,origin.x+8,origin.y+20,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+10,origin.y+20,origin.z,Set.of(),0,0,true);}
        boolean warm(){boolean ready=true;for(int x=(center.getX()-64)>>4;x<=(center.getX()+64)>>4;x++)for(int z=(center.getZ()-64)>>4;z<=(center.getZ()+64)>>4;z++)ready&=level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z));boolean visible=level.getEntities((Entity)null,new AABB(origin.add(-16,-5,-16),origin.add(16,30,16)),e->e==beta).contains(beta);require(++warmTicks<=100,"entity residency timeout visible="+visible+" beta="+beta.position()+" level="+beta.level().dimension());return ready&&visible;}
        void reset(LivingEntity e){e.setInvulnerableTime(0);e.damageCooldownTime=0;}
        void capture(){for(var e:game.items().creatureDrops().snapshot())if(!existing.contains(e)&&!e.isRemoved())mine.add(e);for(var e:game.items().droneDrops().snapshot())if(!existing.contains(e)&&!e.isRemoved())mine.add(e);}
        void clear(){for(var guard:List.copyOf(guards))removeGuard(guard);guards.clear();capture();for(var e:new ArrayList<>(mine)){if(e instanceof LivingEntity l)game.items().indirectKills().clear(l);e.discard();}mine.clear();tokens.forEach(Entity::discard);var debris=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e)&&!(e instanceof ServerPlayer)&&bounds.isInside(e.blockPosition()))debris.add(e);debris.forEach(Entity::discard);}
        void clearTerrain(){level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);for(var p:blocks.keySet()){if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),818);}}
        ItemEntity token(boolean bob,int count){var t=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(bob?LbcCreatureDropItem.Kind.OLD_BOB.item():LbcDroneDropItem.NORMAL_ITEM,count));t.setThrower(alpha);t.setTarget(beta.getUUID());t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);t.setPickUpDelay(1000);require(level.addFreshEntity(t),"native token admission");tokens.add(t);return t;}

        MinecartSpawner drone(){clear();var t=token(false,2);require(game.items().droneDrops().ready(t)&&game.items().specialDrops().spawn(t)&&t.getItem().getCount()==1,"drone one shared debit");require(!game.items().specialDrops().spawn(t),"drone same tick dispatch guard");t.discard();capture();var list=mine.stream().filter(e->e instanceof MinecartSpawner).toList();require(list.size()==1,"one native producer cart");producer=(MinecartSpawner)list.getFirst();return producer;}




        Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);require(level.addFreshEntity(c),"native disposable victim");mine.add(c);return c;}
        void lethal(Cow c,net.minecraft.world.damagesource.DamageSource source,ServerPlayer owner){int before=kills(owner);c.setHealth(1);reset(c);require(c.hurtServer(level,source,10000)&&c.isDeadOrDying()&&c.getKillCredit()==owner&&kills(owner)==before+1,"actual native death and author/stat credit "+source);}


        void loadSpawner(MinecartSpawner cart,CompoundTag tag){cart.getSpawner().load(level,cart.blockPosition(),TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));}
        void delay(MinecartSpawner cart,int n){var tag=saved(cart,level);tag.putShort("Delay",(short)n);loadSpawner(cart,tag);}
        List<PrimedTnt> bombs(){capture();return mine.stream().filter(e->e instanceof PrimedTnt&&!e.isRemoved()).map(e->(PrimedTnt)e).toList();}
        PrimedTnt emit(MinecartSpawner cart){var before=new HashSet<>(bombs());delay(cart,0);cart.getSpawner().serverTick(level,cart.blockPosition());var fresh=bombs().stream().filter(t->!before.contains(t)).toList();require(fresh.size()==1,"actual BaseSpawner emits TNT/passenger: fresh="+fresh.size()+" Delay="+saved(cart,level).getIntOr("Delay",-1)+" admitted="+game.items().droneDrops().canTick(cart)+" alphaDist="+alpha.distanceTo(cart));var t=fresh.getFirst();require(t.getFuse()==100&&t.entityTags().contains("groundbam")&&t.getOwner()==alpha&&saved(t,level).contains("owner")&&game.items().droneDrops().owner(t)==alpha&&LbcIndirectKills.playerSource(t)==alpha,"native source fuse/landing tag and persisted augmented causal owner: fuse="+t.getFuse()+" ground="+t.entityTags().contains("groundbam")+" nativeOwner="+t.getOwner()+" savedOwner="+saved(t,level).get("owner")+" serviceOwner="+game.items().droneDrops().owner(t)+" cause="+LbcIndirectKills.playerSource(t));require(t.getPassengers().size()==1&&t.getFirstPassenger().getType()==EntityTypes.PIG&&((LivingEntity)t.getFirstPassenger()).getHealth()==35&&((LivingEntity)t.getFirstPassenger()).getMaxHealth()==35&&saved(t.getFirstPassenger(),level).getStringOr("DeathLootTable","").equals("minecraft:empty"),"native TNT carries source35HP empty-loot pig");for(var actor:List.of(t,t.getFirstPassenger()))require(actor.getCustomName()!=null&&actor.getCustomName().getString().equals("\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430 \u0431i\u043c\u0431\u0430")&&!actor.getCustomName().getStyle().isItalic(),"exact source literal bomb name");return t;}
        void droneConfig(MinecartSpawner cart){require(mine.size()==3&&cart.getVehicle()!=null&&cart.getVehicle().getType()==EntityTypes.BAT&&cart.getPassengers().size()==1&&cart.getFirstPassenger().getType()==EntityTypes.HOGLIN,"native bat/cart/hoglin graph");var bat=(LivingEntity)cart.getVehicle();var hog=(LivingEntity)cart.getFirstPassenger();require(bat.position().distanceTo(origin.add(0,20,0))<1e-8&&bat.getHealth()==40&&bat.getMaxHealth()==40&&bat.isSilent()&&((Mob)bat).isPersistenceRequired(),"native bat source position/health/silent/persistence");for(var actor:List.of(bat,cart,hog))require(actor.getCustomName()!=null&&actor.getCustomName().getString().equals("\u0443\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0438\u0439 \u0431\u0435\u0437\u043f\u0456\u043b\u043e\u0442\u043d\u0438\u043a")&&!actor.getCustomName().getStyle().isItalic(),"exact source literal drone name");var invis=bat.getEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);var weak=hog.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);require(invis!=null&&invis.getAmplifier()==10&&invis.getDuration()==2000000&&!invis.isVisible()&&weak!=null&&weak.getAmplifier()==99&&weak.getDuration()==2000000&&!weak.isVisible(),"source native status payloads");var ht=saved(hog,level);require(hog.getHealth()==30&&hog.getMaxHealth()==30&&((Mob)hog).isPersistenceRequired()&&ht.getBooleanOr("IsImmuneToZombification",false)&&ht.getStringOr("DeathLootTable","").equals("lbc:hohlopitek"),"normal hoglin native health/persistence/immunity/reward loot");var sourceCart=LbcDroneDrops.payload(true).getListOrEmpty("Passengers").getCompoundOrEmpty(0);require(!sourceCart.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity").contains("owner"),"source TNT preset has no authored owner field");var tag=saved(cart,level);for(var e:Map.of("SpawnCount",1,"SpawnRange",0,"Delay",20,"MinSpawnDelay",20,"MaxSpawnDelay",20,"MaxNearbyEntities",1000,"RequiredPlayerRange",50).entrySet())require(tag.getIntOr(e.getKey(),-1)==e.getValue(),"exact native cart config "+e.getKey());require(cart.entityTags().contains("hohlospawner"),"source recurring cart tag");for(var e:mine)require(game.items().droneDrops().owner(e)==alpha&&game.items().currentEpoch(e),"all native passenger author/epoch");((Mob)bat).setNoAi(true);bat.setNoGravity(true);hog.setNoGravity(true);cart.setNoGravity(true);}
        void producer(){var cart=drone();droneConfig(cart);alpha.teleportTo(level,origin.x+80,origin.y+20,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+82,origin.y+20,origin.z,Set.of(),0,0,true);delay(cart,1);cart.getSpawner().serverTick(level,cart.blockPosition());require(saved(cart,level).getIntOr("Delay",-1)==1&&bombs().isEmpty(),"native required-player range prevents countdown");place();cart.getSpawner().serverTick(level,cart.blockPosition());require(saved(cart,level).getIntOr("Delay",-1)==0&&bombs().isEmpty(),"native nearby player permits countdown before emission");var t=emit(cart);require(saved(cart,level).getIntOr("Delay",-1)==20,"native fixed20 delay after real emission");var pig=t.getFirstPassenger();require(saved(pig,level).getStringOr("DeathLootTable","").equals("minecraft:empty"),"native empty-loot override present before passenger death");var beforePig=new HashSet<Entity>();level.getAllEntities().forEach(beforePig::add);pig.stopRiding();pig.hurtServer(level,pig.damageSources().playerAttack(beta),10000);require(((LivingEntity)pig).isDeadOrDying(),"actual source pig death");for(var e:level.getAllEntities())require(beforePig.contains(e)||!(e instanceof ItemEntity),"native empty-loot passenger death emits no item");t.setPos(origin.add(0,5,0));t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);t.setFuse(90);t.setOnGround(false);t.tick();require(t.getFuse()==89&&t.entityTags().contains("groundbam"),"airborne TNT retains native100-fuse countdown");var floor=BlockPos.containing(origin.add(0,4,0));level.setBlock(floor,Blocks.STONE.defaultBlockState(),818);t.setPos(floor.getX()+.5,floor.getY()+2,floor.getZ()+.5);t.setNoGravity(false);t.setDeltaMovement(new Vec3(0,-1.2,0));t.tick();require(t.onGround(),"native falling TNT collides with real floor");t.tick();require(t.getFuse()<=2&&!t.entityTags().contains("groundbam"),"actual on-ground landing accelerates once to fuse2");t.discard();}
        void hostPolicy(){var cart=drone();droneConfig(cart);var host=(LivingEntity)cart.getFirstPassenger();host.stopRiding();host.setPos(cart.position().add(2,0,0));require(game.items().droneDrops().keepAlive(cart)&&game.items().droneDrops().canTick(cart),"living dismounted own hoglin keeps cart active");game.items().droneDrops().unloaded(host);require(game.items().droneDrops().keepAlive(cart),"host unload is not death");game.items().droneDrops().loaded(host);var foreign=EntityTypes.HOGLIN.create(level,EntitySpawnReason.COMMAND);require(foreign!=null,"native foreign hoglin");foreign.setPos(cart.position().add(1,0,0));foreign.setNoAi(true);require(level.addFreshEntity(foreign),"foreign host admission");mine.add(foreign);require(host.hurtServer(level,host.damageSources().playerAttack(beta),100000)&&host.isDeadOrDying(),"own native hoglin death");require(!game.items().droneDrops().keepAlive(cart)&&!game.items().droneDrops().canTick(cart),"own host death disables producer despite live nearby foreign hoglin");delay(cart,0);int count=bombs().size();cart.tick();require(bombs().size()==count&&saved(cart,level).getIntOr("Delay",-1)==0,"actual native cart tick cannot advance producer after own host death");game.items().droneDrops().unloaded(cart);game.items().droneDrops().loaded(cart);require(!game.items().droneDrops().canTick(cart),"own host death state survives service unload/reload");}
        float explosion(int protect,boolean self,String role){var cart=drone();droneConfig(cart);var t=emit(cart);t.getFirstPassenger().discard();t.setPos(origin.add(0,5,0));t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);t.entityTags().remove("groundbam");var victim=self?alpha:beta;alpha.teleportTo(level,origin.x+(self?1:10),origin.y+5,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+(self?10:1),origin.y+5,origin.z,Set.of(),0,0,true);victim.setHealth(1000);victim.setAbsorptionAmount(0);reset(victim);game.config().tntProtection=protect;boolean added=role!=null&&victim.entityTags().add(role);try{t.setFuse(1);t.tick();require(t.isRemoved(),"native final fuse explosion removes TNT");return 1000-victim.getHealth();}finally{if(added)victim.entityTags().remove(role);place();}}
        void explosions(){float base=explosion(0,false,null);require(base>0,"actual ownerless native drone TNT damages player");for(int i:List.of(1,5))require(Math.abs(explosion(i,false,null)-base*(1-i/5f))<.03,"native TNT protection scaling "+i);require(explosion(0,true,null)>0,"native WILD drone self damage");var board=game.server().getScoreboard();var oldA=alpha.getTeam();var oldB=beta.getTeam();var team=board.addPlayerTeam("codex_drone_wild_probe");team.setAllowFriendlyFire(false);try{board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);require(explosion(0,false,null)>0,"actual native WILD drone blast damages ally with friendly fire disabled");}finally{board.removePlayerTeam(team);if(oldA!=null)board.addPlayerToTeam(alpha.getScoreboardName(),oldA);if(oldB!=null)board.addPlayerToTeam(beta.getScoreboardName(),oldB);}for(String role:List.of("spectator","strict_map_object","magic_academy"))require(explosion(0,false,role)==0,"actual role blast guard "+role);game.config().tntProtection=0;var cart=drone();droneConfig(cart);var t=emit(cart);t.getFirstPassenger().discard();t.setPos(origin.add(0,5,0));t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);var c=cow(origin.add(1,5,0));c.setHealth(1);int before=kills(alpha);t.setFuse(1);t.tick();require(c.isDeadOrDying()&&c.getKillCredit()==alpha&&kills(alpha)==before+1,"actual native generated TNT explosion kill author/stat");}




        void observerBlast(){
            var cart=drone();droneConfig(cart);var t=emit(cart);t.getFirstPassenger().discard();
            t.setPos(origin.add(0,5,0));t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);t.entityTags().remove("groundbam");
            beta.teleportTo(level,origin.x+1,origin.y+5,origin.z,Set.of(),0,0,true);beta.setHealth(1000);beta.setAbsorptionAmount(0);reset(beta);
            var victim=cow(origin.add(-1,5,0));victim.setHealth(1);reset(victim);int before=kills(alpha);
            var snapshot=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());game.spectators().enter(alpha);
            try{
                require(game.isObserver(alpha)&&!t.isRemoved()&&game.items().currentEpoch(t)&&t.getOwner()==alpha,"emitted current-epoch native TNT survives its caster becoming observer");
                t.setFuse(1);t.tick();
                require(t.isRemoved()&&beta.getHealth()<1000&&victim.isDeadOrDying(),"actual native TNT explosion still damages active player and mob after caster enters observer");
                require(victim.getKillCredit()==alpha&&kills(alpha)==before+1&&LbcIndirectKills.playerSource(t)==alpha,"independent native TNT retains original observer caster death/stat credit");
            }finally{game.spectators().remove(alpha);snapshot.restore(game);place();}
            clear();
        }

        ItemStack staff;
        final Set<Entity> guards=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<BlockPos> guardCells=new HashSet<>();
        Entity farGuard,sourceMagic; boolean addedAlphaAura,addedBetaAura;
        BlockPos farTarget;
        final Set<Long> farChunks=new HashSet<>();
        int cooldown(){return game.items().mana().score(alpha,"coldownhohol").get();}
        void resetCooldown(){game.items().hohlopitek().clear(alpha);}
        List<LivingEntity> bombers(){capture();return mine.stream().filter(e->e instanceof LivingEntity&&e.entityTags().contains("hohlopitek")&&!e.isRemoved()).map(e->(LivingEntity)e).toList();}
        boolean use(net.minecraft.world.InteractionHand hand){alpha.setItemInHand(hand,staff.copy());game.items().mana().inventoryChanged(alpha);return alpha.gameMode.useItem(alpha,level,alpha.getItemInHand(hand),hand).consumesAction();}
        void presentation(){
            var token=new ItemStack(LbcDroneDropItem.NORMAL_ITEM);var flags=token.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(flags.size()==2&&flags.getIntOr("droppertnt",0)==1&&flags.getIntOr("drop",0)==1&&LbcDroneDropItem.isNormal(token),"exact normal flags and variant identity");
            require(Identifier.parse("lbc:drop_boss").equals(token.get(DataComponents.ITEM_MODEL)),"normal source model");
            var rolled=game.lbc().rollTable(level,origin,alpha,"lbc:ported/tnt_drone_drop");
            require(rolled.size()==1&&ItemStack.isSameItemSameComponents(token,rolled.getFirst()),"complete normal ready leaf");
            var old=LbcDroneDrops.payload(false).getListOrEmpty("Passengers").getCompoundOrEmpty(0).getListOrEmpty("Passengers").getCompoundOrEmpty(0);
            var normal=LbcDroneDrops.payload(true).getListOrEmpty("Passengers").getCompoundOrEmpty(0).getListOrEmpty("Passengers").getCompoundOrEmpty(0);
            require(!old.contains("DeathLootTable")&&normal.getStringOr("DeathLootTable","").equals("lbc:hohlopitek"),"old and normal native reward payloads stay distinct");
        }
        void reward(){
            var cart=drone();droneConfig(cart);var host=(LivingEntity)cart.getFirstPassenger();
            var before=new HashSet<Entity>();level.getAllEntities().forEach(before::add);
            require(host.hurtServer(level,host.damageSources().playerAttack(alpha),10000)&&host.isDeadOrDying(),"actual native normal host player kill");
            var drops=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(!before.contains(e)&&e instanceof ItemEntity i){mine.add(e);drops.add(i);}
            require(drops.size()==1&&drops.getFirst().getItem().getCount()==1&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(drops.getFirst().getItem().getItem()).equals(Identifier.parse("skywars2:hohlopitek")),"one guaranteed complete native staff reward");
            staff=drops.getFirst().getItem().copy();var publicReward=game.lbc().rollTable(level,origin,alpha,"lbc:ported/hohlopitek");
            require(publicReward.size()==1&&ItemStack.isSameItemSameComponents(staff,publicReward.getFirst()),"native reward equals working public weapon leaf");
            require(!game.items().droneDrops().keepAlive(cart),"normal reward death disables only its producer");
            clear();cart=drone();host=(LivingEntity)cart.getFirstPassenger();before.clear();level.getAllEntities().forEach(before::add);
            require(host.hurtServer(level,host.damageSources().generic(),10000)&&host.isDeadOrDying(),"actual environmental normal host death");
            int count=0;for(var e:level.getAllEntities())if(!before.contains(e)&&e instanceof ItemEntity i){mine.add(e);if(ItemStack.isSameItemSameComponents(staff,i.getItem()))count++;}
            require(count==1,"native normal staff reward does not require a player kill");clear();
        }
        void egg(){
            clear();var pad=center.below();level.setBlock(pad,Blocks.STONE.defaultBlockState(),818);
            var stack=game.lbc().rollTable(level,origin,alpha,"lbc:ported/hohol_spawn_egg").getFirst();stack.setCount(2);
            require(Identifier.parse("lbc:tier_f").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"normal drone egg retains source tier_f presentation");
            alpha.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);var before=new HashSet<Entity>();level.getAllEntities().forEach(before::add);
            var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pad).add(0,.5,0),net.minecraft.core.Direction.UP,pad,false);
            require(stack.useOn(new net.minecraft.world.item.context.UseOnContext(alpha,net.minecraft.world.InteractionHand.MAIN_HAND,hit)).consumesAction()&&stack.getCount()==1,"native normal egg right click spends exactly one");
            var fresh=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(!before.contains(e)){mine.add(e);if(e instanceof ItemEntity i)fresh.add(i);}
            require(fresh.size()==1,"native egg emits exactly one token");var token=fresh.getFirst();tokens.add(token);
            require(token.getItem().getCount()==1&&LbcDroneDropItem.isNormal(token.getItem())&&saved(token,level).getIntOr("PickupDelay",0)==32767&&game.items().currentEpoch(token),"native egg retains unpickable normal nested payload and epoch");
            require(Identifier.parse("lbc:drop_boss").equals(token.getItem().get(DataComponents.ITEM_MODEL)),"egg nested token retains exact source drop_boss model");
            require(game.items().specialDrops().spawn(token),"egg token passes actual shared dispatcher");capture();
            require(mine.stream().filter(e->e instanceof MinecartSpawner&&!e.isRemoved()).count()==1,"egg creates one native normal drone");clear();
        }
        void guards(){
            resetCooldown();int mana=game.items().mana().get(alpha);
            game.items().timeStop().begin(level,200);
            try{require(!use(net.minecraft.world.InteractionHand.MAIN_HAND)&&cooldown()==0&&game.items().hohlopitek().pendingRayCount()==0,"frozen native right click declines without cooldown or ray");}finally{game.items().timeStop().end(level);}
            game.config().offhandWands=false;
            try{require(!use(net.minecraft.world.InteractionHand.OFF_HAND)&&cooldown()==0,"disabled offhand declines freely");}finally{game.config().offhandWands=true;}
            var snapshot=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());game.spectators().enter(alpha);
            try{require(!use(net.minecraft.world.InteractionHand.MAIN_HAND)&&cooldown()==0,"observer native weapon use declines freely");}finally{game.spectators().remove(alpha);snapshot.restore(game);}
            require(game.items().mana().get(alpha)==mana&&bombers().isEmpty(),"rejected activation consumes no mana and emits no actor");
        }
        void castMiss(){
            clear();resetCooldown();alpha.teleportTo(level,origin.x,origin.y+3,origin.z,Set.of(),0,-90,true);
            int mana=game.items().mana().get(alpha);
            use(net.minecraft.world.InteractionHand.MAIN_HAND);
            require(cooldown()==200&&game.items().mana().get(alpha)==mana,"air miss starts raw200 cooldown and spends no mana");
            use(net.minecraft.world.InteractionHand.OFF_HAND);require(cooldown()==200&&bombers().isEmpty(),"hands share cooldown and no bomber on rejected second use");wait=0;
        }
        void castHit(){
            clear();resetCooldown();alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);
            var target=BlockPos.containing(alpha.getEyePosition().add(0,0,6));level.setBlock(target,Blocks.STONE.defaultBlockState(),818);
            var sight=level.clip(new net.minecraft.world.level.ClipContext(alpha.getEyePosition(),alpha.getEyePosition().add(alpha.getLookAngle().scale(10)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,alpha));
            require(sight.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&sight.getBlockPos().equals(target),"native target fixture geometry intersects the actual horizontal eye ray");
            int mana=game.items().mana().get(alpha);use(net.minecraft.world.InteractionHand.OFF_HAND);
            require(cooldown()==200&&game.items().mana().get(alpha)==mana,"offhand native right click starts shared raw cooldown for free");wait=0;
        }
        void bomberExplosion(PrimedTnt t){
            t.getFirstPassenger().discard();var floor=BlockPos.containing(origin.add(0,4,0));level.setBlock(floor,Blocks.STONE.defaultBlockState(),818);
            t.setPos(floor.getX()+.5,floor.getY()+2,floor.getZ()+.5);t.setNoGravity(false);t.setDeltaMovement(new Vec3(0,-1.2,0));t.setOnGround(false);
            t.tick();require(t.onGround(),"actual airstrike TNT lands through native block collision");t.tick();require(t.getFuse()<=2&&!t.entityTags().contains("groundbam"),"actual airstrike landing accelerates native fuse once");
            var victim=cow(t.position().add(1,0,0));victim.setHealth(1);reset(victim);int before=kills(alpha);t.setFuse(1);t.tick();
            require(t.isRemoved()&&victim.isDeadOrDying()&&victim.getKillCredit()==alpha&&kills(alpha)==before+1,"actual airstrike native landed TNT explosion keeps caster/stat credit");
        }
        void discardBombs(){for(var t:bombs()){t.getSelfAndPassengers().toList().forEach(Entity::discard);}}
        boolean step(){
            require(game.playing(),"actual match retained");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{if(!warm())return false;presentation();reward();guards();egg();producer();hostPolicy();explosions();observerBlast();clear();castMiss();phase++;}
                case 1->{require(bombers().isEmpty(),"miss never launches a bomber");if(++wait<6)return false;require(wait<=100,"miss ray completes within test budget");if(game.items().hohlopitek().pendingRayCount()>0)return false;require(cooldown()==200-wait/5,"raw cooldown decreases once per five active ticks");require(game.items().hohlopitek().hudValue(alpha)==cooldown(),"HUD uses original raw cooldown units");castHit();phase++;}
                case 2->{var list=bombers();require(++wait<=100,"resident target ray completes within test budget pending="+game.items().hohlopitek().pendingRayCount()+" cooldown="+cooldown()+" eye="+alpha.getEyePosition()+" look="+alpha.getLookAngle());if(list.isEmpty())return false;require(list.size()==1,"one terminal hit launches exactly one bomber");bomber=list.getFirst();
                    require(bomber.getType()==EntityTypes.HOGLIN&&bomber.getMaxHealth()==150&&bomber.getHealth()>0&&bomber.getHealth()<=150&&((Mob)bomber).isNoAi()&&bomber.isNoGravity(),"native source150HP NoAI NoGravity attack hoglin");
                    var effect=bomber.getEffect(net.minecraft.world.effect.MobEffects.INSTANT_DAMAGE);require(effect!=null&&effect.getAmplifier()==0&&!effect.isVisible(),"actual native Instant Damage effect governs lifespan");
                    require(!saved(bomber,level).contains("DeathLootTable")&&game.items().droneDrops().owner(bomber)==alpha&&game.items().currentEpoch(bomber),"bomber has ordinary loot and original caster ownership/epoch");
                    discardBombs();bomberStart=bomber.position();firstHealth=bomber.getHealth();game.items().timeStop().begin(level,200);wait=0;phase++;}
                case 3->{require(bomber.position().distanceTo(bomberStart)<1e-8&&bomber.getHealth()==firstHealth&&bombs().isEmpty(),"actual Time Stop freezes bomber movement, native harmful effect and emissions");if(++wait<3)return false;game.items().timeStop().end(level);lifeTicks=0;produced=0;phase++;}
                case 4->{
                    var fresh=bombs();if(!fresh.isEmpty()){
                        require(fresh.size()==3,"one source global five-tick pulse emits exactly three TNT");produced+=fresh.size();
                        for(var t:fresh){require(t.getFuse()>=99&&t.getFuse()<=100&&t.entityTags().contains("groundbam")&&t.getOwner()==alpha&&LbcIndirectKills.playerSource(t)==alpha,"native bomber TNT fuse/ground flag and causal owner");
                            require(t.getPassengers().size()==1&&t.getFirstPassenger().getType()==EntityTypes.PIG&&((LivingEntity)t.getFirstPassenger()).getHealth()==35&&saved(t.getFirstPassenger(),level).getStringOr("DeathLootTable","").equals("minecraft:empty"),"each native bomber TNT carries one source35HP empty-loot pig");}
                        if(produced==3)bomberExplosion(fresh.getFirst());discardBombs();
                    }
                    sawHurt|=bomber.getHealth()<firstHealth;
                    require(++lifeTicks<=400,"native harmful effect kills bomber within bounded test simulation; no production TTL");
                    if(!bomber.isAlive()||bomber.isRemoved()){
                        require(sawHurt&&produced>0&&bomber.position().distanceTo(bomberStart)>0,"native harmful effect death follows actual movement and repeated TNT emission");wait=0;phase++;
                    }
                }
                case 5->{require(bombs().isEmpty(),"native bomber death stops new TNT pulses");if(++wait<10)return false;require(cooldown()>0,"original cooldown outlives the launched native actor");game.items().hohlopitek().clear(alpha);require(cooldown()==0&&game.items().hohlopitek().hudValue(alpha)==0&&game.items().hohlopitek().pendingRayCount()==0,"shared purifier/death clear resets raw HUD counter and pending ray");clear();prepareFar();wait=0;phase++;}
                case 6->{
                    require(++wait<=100,"far resident strip becomes entity ticking within fixture budget");
                    if(farChunks.stream().anyMatch(key->!level.areEntitiesLoaded(key)))return false;
                    alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);alpha.setDeltaMovement(Vec3.ZERO);beta.setDeltaMovement(Vec3.ZERO);
                    resetCooldown();var source=marker("stopper",alpha.position());
                    require(game.items().magic().mechanismSuppressed(alpha),"actual source mechanism marker is registered");
                    game.items().mana().set(alpha,-1);
                    require(!use(net.minecraft.world.InteractionHand.MAIN_HAND)&&cooldown()==0&&game.items().hohlopitek().pendingRayCount()==0&&game.items().mana().get(alpha)==-1,"source mechanism suppresses technological admission with no cooldown or mana debit");
                    removeGuard(source);
                    farGuard=marker("stopper",alpha.getEyePosition().add(0,0,50));
                    require(game.items().magic().mechanismSuppressedAt(level,farGuard.position())&&!game.items().magic().mechanismSuppressed(alpha),"midray mechanism lies outside source radius");
                    game.items().magic().pulse();
                    require(farGuard.isRemoved()&&level.getBlockState(farGuard.blockPosition()).is(LbcMagic.MECHANISM_BEACON_BLOCK)&&game.items().magic().mechanismSuppressedAt(level,farGuard.position()),"forced native pulse materializes the owned legacy stopper into a real indexed mechanism beacon");
                    require(use(net.minecraft.world.InteractionHand.MAIN_HAND)&&cooldown()==200&&game.items().mana().get(alpha)==-1,"negative mana permits technological ray until remote mechanism boundary");
                    wait=0;phase++;
                }
                case 7->{
                    require(bombers().isEmpty(),"midray mechanism suppression never launches bomber");require(++wait<=100,"remote mechanism ray resolves within fixture budget");
                    if(game.items().hohlopitek().pendingRayCount()>0)return false;
                    removeGuard(farGuard);require(!game.items().magic().mechanismSuppressedAt(level,farGuard.position()),"removing owned migrated beacon clears actual mechanism index before magic control");
                    // Earlier native explosions deliberately impart knockback; teleport alone retains velocity.
                    alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);alpha.setDeltaMovement(Vec3.ZERO);beta.setDeltaMovement(Vec3.ZERO);
                    var sight=level.clip(new net.minecraft.world.level.ClipContext(alpha.getEyePosition(),alpha.getEyePosition().add(alpha.getLookAngle().scale(245)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,alpha));
                    require(sight.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&sight.getBlockPos().equals(farTarget),"far native fixture eye ray intersects exact target: "+farDiagnostics());
                    var spawn=sight.getLocation().add(-28,20,0);var footprint=new AABB(BlockPos.containing(spawn)).inflate(1);
                    for(int x=net.minecraft.util.Mth.floor(footprint.minX)>>4;x<=net.minecraft.util.Mth.floor(footprint.maxX)>>4;x++)for(int z=net.minecraft.util.Mth.floor(footprint.minZ)>>4;z<=net.minecraft.util.Mth.floor(footprint.maxZ)>>4;z++)require(level.getChunkSource().getChunkNow(x,z)!=null,"far authored bomber footprint is resident: "+farDiagnostics());
                    farGuard=marker("stopper_magic",alpha.getEyePosition().add(0,0,50));sourceMagic=marker("stopper_magic",alpha.position());
                    require(game.items().magic().suppressed(alpha)&&!game.items().magic().mechanismSuppressed(alpha),"actual source magic marker is distinct from mechanism control");
                    addedAlphaAura=alpha.entityTags().add("nomagic_active");addedBetaAura=beta.entityTags().add("nomagic_active");resetCooldown();game.items().mana().set(alpha,-1);
                    require(use(net.minecraft.world.InteractionHand.MAIN_HAND)&&cooldown()==200&&game.items().mana().get(alpha)==-1,"technological admission ignores own and nearby antimagic artifact plus source magic marker even at negative mana");
                    wait=0;phase++;
                }
                case 8->{
                    discardBombs();var list=bombers();if(++wait>100)require(false,"unlimited resident target ray resolves within fixture budget: "+farDiagnostics());
                    if(list.isEmpty()){require(game.items().hohlopitek().pendingRayCount()>0,"far hit ray must remain pending until native bomber insertion: "+farDiagnostics());return false;}
                    require(list.size()==1&&game.items().hohlopitek().pendingRayCount()==0,"unlimited real ray has exactly one far terminal and one native bomber");
                    var actor=list.getFirst();require(actor.getMaxHealth()==150&&actor.getY()>alpha.getEyePosition().y+19&&Math.abs(actor.getZ()-farTarget.getZ())<4&&Math.abs(actor.getX()-(alpha.getX()-28))<4&&actor.position().distanceTo(alpha.position())>220,"real resident hit beyond200 launches source offset150HP bomber at its own target");
                    require(game.items().droneDrops().owner(actor)==alpha&&cooldown()>0,"far bomber retains caster and original shared cooldown");
                    if(addedAlphaAura)alpha.entityTags().remove("nomagic_active");if(addedBetaAura)beta.entityTags().remove("nomagic_active");
                    removeGuard(farGuard);removeGuard(sourceMagic);clear();return true;
                }
                default->throw new IllegalStateException("Unknown normal drone fixture phase "+phase);
            }return false;
        }


        String farDiagnostics(){
            var from=alpha.getEyePosition();var end=from.add(alpha.getLookAngle().scale(245));
            var sight=level.clip(new net.minecraft.world.level.ClipContext(from,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,alpha));
            var candidates=level.getEntities((Entity)null,new AABB(from,end).inflate(1),e->e instanceof LivingEntity&&e!=alpha).stream().limit(12).map(e->e.getType()+"@"+e.position()+"/removed="+e.isRemoved()+"/target="+game.items().lifetimes().target(alpha,e)).toList();
            var at=new Vec3(alpha.getX()-28,farTarget.getY()+20,farTarget.getZ());var residency=new ArrayList<String>();
            for(int x=(BlockPos.containing(at).getX()-1)>>4;x<=(BlockPos.containing(at).getX()+1)>>4;x++)for(int z=(BlockPos.containing(at).getZ()-1)>>4;z<=(BlockPos.containing(at).getZ()+1)>>4;z++)residency.add(x+","+z+":"+(level.getChunkSource().getChunkNow(x,z)!=null)+"/entities="+level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)));
            return "pos="+alpha.position()+" eye="+from+" look="+alpha.getLookAngle()+" velocity="+alpha.getDeltaMovement()+" beta="+beta.position()+"/"+beta.getDeltaMovement()+" nativeClip="+sight.getType()+"@"+sight.getBlockPos()+" target="+farTarget+":"+level.getBlockState(farTarget)+" nearbyTargets="+candidates+" expectedBomberChunks="+residency+" unloadedStrip="+farChunks.stream().filter(key->!level.areEntitiesLoaded(key)).count()+" mechanism="+game.items().magic().mechanismSuppressed(alpha)+" magic="+game.items().magic().suppressed(alpha)+" guard="+(farGuard==null?"null":farGuard.position()+"/removed="+farGuard.isRemoved()+"/tags="+farGuard.entityTags())+" sourceMagic="+(sourceMagic==null?"null":sourceMagic.position()+"/removed="+sourceMagic.isRemoved()+"/tags="+sourceMagic.entityTags())+" pending="+game.items().hohlopitek().pendingRayCount()+" cooldown="+cooldown()+" observer="+game.isObserver(alpha)+" actionBlocked="+game.items().actionsBlocked(alpha)+" hp="+alpha.getHealth()+" noGravity="+alpha.isNoGravity();
        }
        void removeGuard(Entity guard){
            guard.discard();game.items().magic().unloaded(guard);var pos=guard.blockPosition();
            if(guardCells.contains(pos)&&level.getBlockState(pos).is(LbcMagic.MECHANISM_BEACON_BLOCK)){
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),818);
            }
            if(guardCells.contains(pos))game.items().magic().blockChanged(pos,level.getBlockState(pos));
        }
        Entity marker(String tag,Vec3 at){
            var pos=BlockPos.containing(at);guardCells.add(pos);blocks.computeIfAbsent(pos,key->game.maps().snapshot(level,key));
            if(tag.equals("stopper"))require(level.getBlockState(pos).isAir(),"owned mechanism migration cell starts as air");
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().add(tag);
            require(level.addFreshEntity(marker),"native suppression marker admission "+tag);mine.add(marker);guards.add(marker);game.items().magic().loaded(marker);return marker;
        }
        void farBlock(BlockPos p,net.minecraft.world.level.block.state.BlockState value){p=p.immutable();blocks.computeIfAbsent(p,key->game.maps().snapshot(level,key));require(level.getBlockEntity(p)==null,"far ray strip contains no native block entity");level.setBlock(p,value,818);}
        void prepareFar(){
            // Test-only resident strip; production rays never request a chunk.
            for(int x=(center.getX()-48)>>4;x<=(center.getX()+48)>>4;x++)for(int z=(center.getZ()-16)>>4;z<=(center.getZ()+272)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);farChunks.add(key);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);
            }
            alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);
            var eyes=BlockPos.containing(alpha.getEyePosition());
            // Remove the previous six-block target and any intervening terrain from this controlled ray.
            for(int z=0;z<=241;z++)farBlock(eyes.offset(0,0,z),Blocks.AIR.defaultBlockState());
            farTarget=eyes.offset(0,0,240);farBlock(farTarget,Blocks.STONE.defaultBlockState());
        }

        void restorePlayers(){alphaState.restore(game);betaState.restore(game);game.items().mana().score(alpha,"coldownhohol").set(cooldownBefore);stats.forEach((p,values)->values.forEach((stat,value)->p.getStats().setValue(p,stat,value)));alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaKills);beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaKills);}
        public void close(){close(true);}
        void close(boolean players){game.items().hohlopitek().clear(alpha);game.items().timeStop().end(level);clear();level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);for(var e:blocks.entrySet()){if(level.getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(level,e.getKey(),e.getValue());}for(var pos:guardCells)game.items().magic().blockChanged(pos,level.getBlockState(pos));level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);blockTicks.forEach(level.getBlockTicks()::schedule);fluidTicks.forEach(level.getFluidTicks()::schedule);for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);level.getGameRules().set(GameRules.MOB_DROPS,mobDrops,game.server());level.getGameRules().set(GameRules.TNT_EXPLODES,tntExplodes,game.server());game.config().tntProtection=protection;game.config().offhandWands=offhand;game.items().indirectKills().clear(alpha);game.items().indirectKills().clear(beta);if(players)restorePlayers();}
    }
    private static void failure(State s,Throwable e){try{s.close();}catch(Throwable cleanup){if(cleanup!=e)e.addSuppressed(cleanup);}running=null;stopping=null;}
    public static void run(GameController game){require(running==null&&stopping==null&&retained==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error e){failure(running,e);throw e;}}
    public static void tick(GameController game){
        if(stopping!=null){var f=stopping;try{
            if(!f.warm())return;
            var cart=f.drone();f.droneConfig(cart);f.emit(cart);
            var bomber=f.game.items().droneDrops().spawnBomber(f.alpha,f.origin,0);
            require(bomber!=null,"native stop bomber");f.capture();stopActors=List.copyOf(f.mine);
            require(stopActors.size()==6,"native stop producer graph plus TNT/pig and bomber");
            stopEpoch=game.items().epoch();stopDebris.clear();f.level.getAllEntities().forEach(stopDebris::add);
            f.restorePlayers();game.items().timeStop().begin(f.level,200);stopToken=f.token(false,3);
            require(!game.items().specialDrops().spawn(stopToken)&&stopToken.getItem().getCount()==3,"pending normal stop token frozen");
            retained=f;stopping=null;SkyWars2.LOGGER.info("[check] PASS normal-drone-stop-probe");
        }catch(Throwable e){failure(f,e);SkyWars2.LOGGER.error("[check] FAIL normal-drone-stop-probe",e);}return;}
        if(running==null)return;var f=running;try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS normal-drone");}}catch(Throwable e){failure(f,e);SkyWars2.LOGGER.error("[check] FAIL normal-drone",e);}
    }
    public static void stopProbe(GameController game){require(game.playing()&&running==null&&stopping==null&&retained==null,"actual stop probe");stopping=new State(game);try{stopping.initialize();}catch(RuntimeException|Error e){failure(stopping,e);throw e;}}
    public static void cleanup(GameController game){require(retained!=null&&stopEpoch!=null&&!game.items().currentEpoch(stopEpoch)&&stopToken!=null&&stopToken.entityTags().contains(stopEpoch)&&stopToken.getItem().getCount()==3&&stopActors.size()==6&&stopActors.stream().allMatch(Entity::isRemoved),"actual stop removes normal producer graph/bomber/TNT/pig and invalidates pending epoch");require(!game.items().specialDrops().spawn(stopToken),"stopped pending token cannot replay");for(var e:retained.level.getAllEntities())require(stopDebris.contains(e)||(!(e instanceof ItemEntity)&&e.getType()!=EntityTypes.EXPERIENCE_ORB)||!retained.bounds.isInside(e.blockPosition())||e==stopToken,"actual stop emits no reward/XP");stopToken.discard();retained.close(false);retained=null;stopToken=null;stopEpoch=null;stopActors=List.of();stopDebris.clear();SkyWars2.LOGGER.info("[check] PASS normal-drone-cleanup");}
}
