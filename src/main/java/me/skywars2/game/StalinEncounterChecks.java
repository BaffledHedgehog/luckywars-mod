package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native encounters and shared production gun hits; no substitute combat implementation. */
public final class StalinEncounterChecks {
    private StalinEncounterChecks() { }
    private static Fixture running, retained;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Entity> stopActors=List.of();
    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Stalin encounter: "+message);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer owner, other;
        final GameChecks.WandThirdPlayerState ownerState, otherState;
        final Vec3 origin;
        final BlockPos base;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final Set<Entity> initial=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> otherStats=new LinkedHashMap<>();
        final int cowKills, bossKills, skill, trinket, trinketSlot;
        final int[] trinkets;
        ZombifiedPiglin actor;
        Cow target, behind, service;
        int lastTick,warmTicks,phase;
        boolean retain;
        Fixture(GameController game) {
            this.game=game;level=game.maps().level();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && owner!=null && other!=null && game.allowDamage(other),"two active participants");
            require(game.items().stalin().activeCount()==0,"exclusive encounter fixture");
            ownerState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
            otherState=new GameChecks.WandThirdPlayerState(other,game.items().mana());
            origin=new Vec3(Math.floor(owner.getX()/16)*16+8.25,265.25,Math.floor(owner.getZ()/16)*16+8.75);
            base=BlockPos.containing(origin);
            var profile=game.players().get(owner.getUUID());
            skill=profile.lbcSkill;trinket=profile.lbcTrinket;trinketSlot=profile.lbcTrinketSlot;trinkets=profile.trinkets.clone();
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT) {
                var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));otherStats.put(stat,other.getStats().getValue(stat));
            }
            cowKills=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.COW));
            bossKills=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.ZOMBIFIED_PIGLIN));
            level.getAllEntities().forEach(initial::add);
        }
        void set(BlockPos pos,net.minecraft.world.level.block.state.BlockState value) {
            pos=pos.immutable();blocks.computeIfAbsent(pos,p->game.maps().snapshot(level,p));
            if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();
            level.setBlock(pos,value,818);game.items().magic().blockChanged(pos,value);
        }
        void initialize() {
            for(int x=(base.getX()-32)>>4;x<=(base.getX()+32)>>4;x++)for(int z=(base.getZ()-32)>>4;z<=(base.getZ()+32)>>4;z++) {
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);
            }
            for(var p:BlockPos.betweenClosed(base.offset(-3,-1,-3),base.offset(3,5,12)))set(p,Blocks.AIR.defaultBlockState());
            for(var player:List.of(owner,other)) {
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
                player.teleportTo(level,origin.x+24,origin.y+2,origin.z+24,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);
            }
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);
            lastTick=game.server().getTickCount();
        }
        ItemEntity token(int count) {
            var t=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(LbcStalinDropItem.ITEM,count));
            t.setThrower(owner);t.setNoGravity(true);t.setPickUpDelay(1000);t.setDeltaMovement(Vec3.ZERO);
            require(level.addFreshEntity(t),"native dropped token");owned.add(t);return t;
        }
        ZombifiedPiglin activate() {
            var prior=new HashSet<Entity>();game.items().stalin().snapshot().forEach(v->prior.add(v.actor()));
            var t=token(2);
            require(game.items().stalin().ready(t) && game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"production dispatch debits exactly one source unit");
            require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"same-tick replay cannot consume another unit");t.discard();
            var actor=game.items().stalin().snapshot().stream().map(LbcStalin.View::actor).filter(e->!prior.contains(e)).findFirst().orElseThrow();owned.add(actor);return actor;
        }
        Cow cow(Vec3 at) {
            var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);
            require(level.addFreshEntity(c),"native cow");owned.add(c);return c;
        }
        void reset(Cow c) {c.setHealth(10);c.setInvulnerableTime(0);c.damageCooldownTime=0;}
        void shot() {game.items().stalin().shot(actor,new Vec3(0,0,1));}
        void presentation() {
            var stack=new ItemStack(LbcStalinDropItem.ITEM);var flags=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(flags.keySet().equals(Set.of("drop","stalin")) && flags.getIntOr("drop",0)==1 && flags.getIntOr("stalin",0)==1,"exact acquisition flags");
            require(Identifier.parse("lbc:drop_boss").equals(stack.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE))
                    && !stack.getHoverName().getStyle().isItalic() && stack.getHoverName().getStyle().getColor().getValue()==0xffff55,"source boss token presentation");
            var loot=game.lbc().rollTable(level,origin,owner,"lbc:ported/stalin_drop");
            require(loot.size()==1 && loot.getFirst().is(LbcStalinDropItem.ITEM) && ItemStack.isSameItemSameComponents(stack,loot.getFirst()),"real public acquisition leaf");
            var fake=token(3);fake.makeFakeItem();require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount()==3 && game.items().stalin().activeCount()==0,"fake item free rejection");fake.discard();
            actor=activate();
            require(actor.position().equals(origin.add(0,.5,0)) && actor.getHealth()==500 && actor.getMaxHealth()==500
                    && actor.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue()==100 && actor.isSilent() && !actor.isNoAi(),"native500HP actor and exact noncentered source spawn");
            require(actor.hasCustomName() && actor.isCustomNameVisible() && actor.getCustomName().getStyle().getColor().getValue()==0xff5555
                    && actor.entityTags().containsAll(Set.of("stalin","lucky_mob")) && game.items().stalin().owner(actor)==owner,"visible source name/tags and native causal owner");
            require(actor.hasEffect(MobEffects.INVISIBILITY) && actor.hasEffect(MobEffects.FIRE_RESISTANCE),"source long-lived effects");
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.FEET)) {
                var armor=actor.getItemBySlot(slot);var equip=armor.get(DataComponents.EQUIPPABLE);
                require(equip!=null && equip.assetId().isPresent() && equip.assetId().get().identifier().equals(Identifier.parse("lbc:stalin"))
                        && actor.getDropChances().byEquipment(slot)==0,"native source armor asset/zero drop: "+slot);
            }
            require(actor.getItemBySlot(EquipmentSlot.HEAD).has(DataComponents.UNBREAKABLE)
                    && actor.getItemBySlot(EquipmentSlot.LEGS).is(Items.LEATHER_LEGGINGS)
                    && Identifier.parse("lbc:ak47").equals(actor.getMainHandItem().get(DataComponents.ITEM_MODEL)),"source head/legs/held AK model");
            // Native HurtByTargetGoal initializes its timestamp to zero; first tick before first hit.
            level.tickNonPassenger(actor);
            require(actor.hurtServer(level,actor.damageSources().playerAttack(other),1),"native piglin accepts retaliation trigger");
            for(int i=0;i<30 && actor.getTarget()!=other;i++)level.tickNonPassenger(actor);
            require(actor.getLastHurtByMob()==other && actor.getTarget()==other,"native hurt goal acquires actual attacker");
            actor.setHealth(500);actor.setInvulnerableTime(0);actor.damageCooldownTime=0;actor.setPos(origin.add(0,.5,0));actor.setDeltaMovement(Vec3.ZERO);
            actor.setNoAi(true);actor.setNoGravity(true);level.tickNonPassenger(actor);
            require(actor.isInvisible(),"native first metadata synchronization exposes invisibility");
            game.items().stalin().pulse(actor);
            require(game.items().stalin().snapshot().getFirst().cooldown()==null,"uninjured actor retains missing source timer");
            actor.setHealth(495.99f);game.items().stalin().pulse(actor);
            require(game.items().stalin().snapshot().getFirst().cooldown()==15,"source integer health and missing-timer pre2 initializes16 then decrements");
            actor.setHealth(500);
            target=cow(new Vec3(actor.getX(),actor.getEyeY()-.5,actor.getZ()+4));
            behind=cow(new Vec3(actor.getX(),actor.getEyeY()-.5,actor.getZ()+7));
            service=cow(new Vec3(actor.getX(),actor.getEyeY()-.5,actor.getZ()+2));service.entityTags().add("magic_academy");
            game.items().timeStop().begin(level,200);
        }
        void combat() {
            game.items().timeStop().end(level);
            // Exercise real native melee independently of ray direction and aiming policy.
            var melee=cow(actor.position().add(2,0,0));melee.setHealth(1);
            require(actor.doHurtTarget(level,melee) && !melee.isAlive() && melee.getKillCredit()==owner,"actual native lethal piglin melee credits source author");melee.discard();
            require(!actor.doHurtTarget(level,service) && service.getHealth()==10,"service role blocks real native melee");
            require(game.items().stalin().aimTarget(actor)==target,"nearest physical service role is excluded from aim");
            target.entityTags().add("communist");require(game.items().stalin().aimTarget(actor)==behind,"communist tag excluded from aim");
            shot();require(target.getHealth()==1 && behind.getHealth()==10 && service.getHealth()==10,"aim exclusion retains physical first collision and exact9damage");
            require(target.getLastDamageSource().is(DamageTypes.ARROW) && target.getLastDamageSource().getEntity()==actor,"real native arrow damage uses actual NPC source");
            target.entityTags().remove("communist");reset(target);
            var communism=new ItemStack(Items.PAPER);var data=new net.minecraft.nbt.CompoundTag();data.putInt("communism",1);communism.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            other.teleportTo(level,actor.getX()+2,actor.getY(),actor.getZ()+1,Set.of(),0,0,true);other.getInventory().setItem(9,communism);
            require(game.items().stalin().aimTarget(actor)==target,"communism inventory excludes closer player from aim");
            other.getInventory().setItem(9,ItemStack.EMPTY);require(game.items().stalin().aimTarget(actor)==other,"same player becomes native aim target without communism");
            other.teleportTo(level,origin.x+24,origin.y+2,origin.z+24,Set.of(),0,0,true);
            var first=BlockPos.containing(actor.getEyePosition().add(0,0,1));var second=first.south();set(first,Blocks.STONE.defaultBlockState());set(second,Blocks.DIRT.defaultBlockState());
            shot();require(level.getBlockState(first).isAir() && level.getBlockState(second).is(Blocks.DIRT) && target.getHealth()==10,"one shot destroys only first native wall and stops before entity");
            set(second,Blocks.AIR.defaultBlockState());set(first,Blocks.BEDROCK.defaultBlockState());shot();
            require(level.getBlockState(first).is(Blocks.BEDROCK) && target.getHealth()==10,"source indestructible wall blocks damage");set(first,Blocks.AIR.defaultBlockState());
            // Native shield sphere intersects the lane; its player's hitbox is deliberately outside it.
            other.teleportTo(level,actor.getX()+1.2,actor.getEyeY()-.5,actor.getZ()+2,Set.of(),180,0,true);
            other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));other.startUsingItem(InteractionHand.MAIN_HAND);for(int i=0;i<6;i++)other.doTick();
            require(other.isBlocking() && game.items().wands().shieldHit(owner,actor.getEyePosition(),actor.getEyePosition().add(0,0,8))!=null,"actual blocking shield sphere covers lane");
            shot();require(target.getHealth()==1,"AK bypasses magic shield interception and hits actual cow");other.stopUsingItem();
            other.teleportTo(level,origin.x+24,origin.y+2,origin.z+24,Set.of(),0,0,true);
            reset(target);target.setHealth(1);int kills=owner.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));shot();
            require(!target.isAlive(),"native lethal NPC shot kills physical target; health="+target.getHealth());
            require(target.getLastDamageSource().getEntity()==actor,"lethal ray retains actual NPC damage source");
            require(target.getKillCredit()==owner,"lethal ray credits thrower during actual death; credit="+target.getKillCredit());
            require(owner.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS))==kills+1,
                    "lethal ray awards native credited mob kill statistic; before="+kills+" after="+owner.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS)));target.discard();
            target=behind;reset(target);
            actor.setHealth(500);for(int i=0;i<16;i++)game.items().stalin().pulse(actor);
            require(game.items().stalin().snapshot().getFirst().cooldown()==0,"source timer drains to zero even after health recovery");
            shot();require(target.getHealth()==1 && game.items().stalin().snapshot().getFirst().cooldown()==0,"source shot does not reset the exhausted timer");reset(target);
            // Drive the actual five-tick production dispatcher, including random admission,
            // sorted aim, shared spread and the origin-only remaining facing branches.
            var beforePulse=target.position();target.getAttribute(Attributes.SCALE).setBaseValue(3);target.refreshDimensions();
            target.setPos(actor.getX(),actor.getEyeY()-.5,actor.getZ()+4);
            require(game.items().stalin().aimTarget(actor)==target,"actual source pulse nearest target fixture");
            actor.setHealth(495);actor.getRandom().setSeed(4096L);game.items().stalin().pulse(actor);
            require(target.getHealth()==1 && game.items().stalin().snapshot().getFirst().cooldown()==0,
                    "actual admitted source pulse chooses and damages nearest eligible target without resetting timer; hp="+target.getHealth()+" cooldown="+game.items().stalin().snapshot().getFirst().cooldown());
            actor.setHealth(500);target.getAttribute(Attributes.SCALE).setBaseValue(1);target.refreshDimensions();target.setPos(beforePulse);reset(target);
            var beacon=base.west(2);set(beacon,LbcMagic.MECHANISM_BEACON_BLOCK.defaultBlockState());
            require(game.items().magic().mechanismSuppressedAt(level,actor.position()),"native mechanism beacon fixture");shot();require(target.getHealth()==10,"mechanism suppression blocks native gun");set(beacon,Blocks.AIR.defaultBlockState());
            var view=game.items().stalin().snapshot().stream().filter(v->v.actor()==actor).findFirst().orElseThrow();
            game.items().timeStop().begin(level,200);var position=actor.position();int ticks=actor.tickCount,active=view.activeTicks();
            for(int i=0;i<7;i++){level.tickNonPassenger(actor);game.items().stalin().tick();shot();}
            var frozen=game.items().stalin().snapshot().stream().filter(v->v.actor()==actor).findFirst().orElseThrow();
            require(actor.tickCount==ticks && actor.position().equals(position) && frozen.activeTicks()==active && frozen.cooldown().equals(view.cooldown()) && target.getHealth()==10,"native world tick gate freezes actor and source cadence/shots");
            var paused=token(3);require(!game.items().specialDrops().spawn(paused) && paused.getItem().getCount()==3,"frozen acquisition free rejection");paused.discard();game.items().timeStop().end(level);
        }
        void reward() {
            service.discard();target.discard();var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
            actor.setHealth(1);actor.setInvulnerableTime(0);actor.damageCooldownTime=0;
            require(actor.hurtServer(level,actor.damageSources().playerAttack(owner),1000) && !actor.isAlive(),"actual ordinary native boss death");
            var drops=new ArrayList<ItemEntity>();for(var entity:level.getAllEntities())if(!before.contains(entity) && entity instanceof ItemEntity item){owned.add(item);if(LbcAk47.isAk47(item.getItem()))drops.add(item);}
            require(drops.size()==1,"native death emits exactly one usable AK reward");var gun=drops.getFirst().getItem().copy();
            require(gun.is(LbcAk47.ITEM) && !LbcAk47.isFinite(gun) && Identifier.parse("lbc:tier_s").equals(gun.get(DataComponents.TOOLTIP_STYLE))
                    && EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY),gun)==1,"native infinite enchanted source reward identity");
            require(gun.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().keySet().equals(Set.of("transformable","shooter","magicwand","ak47")),"reward preserves all crafting/reforge flags");
            var nativeLoot=game.lbc().rollTable(level,origin,owner,"lbc:stalin");require(nativeLoot.size()==1 && ItemStack.isSameItemSameComponents(gun,nativeLoot.getFirst()),"actual death reward equals complete imported source loot payload");
            owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);owner.setItemInHand(InteractionHand.MAIN_HAND,gun);game.items().ak47().setAmmo(owner,0);
            var victim=cow(new Vec3(owner.getX(),owner.getEyeY()-.5,owner.getZ()+4));
            require(game.items().wands().use(owner,InteractionHand.MAIN_HAND) && victim.getHealth()==1 && game.items().ak47().ammo(owner)==0,"actual death reward fires through normal shared native input without ammunition");
            game.items().stalin().tick();require(game.items().stalin().activeCount()==0,"dead native actor retires encounter");
        }
        boolean step() {
            if(lastTick==game.server().getTickCount())return false;lastTick=game.server().getTickCount();
            require(++warmTicks<=100,"bounded resident-section warmup");
            if(phase==0) {
                if(!level.getEntitiesOfClass(ServerPlayer.class,owner.getBoundingBox().inflate(1)).contains(owner))return false;
                presentation();phase=1;return false;
            }
            combat();reward();return true;
        }
        @Override public void close() {
            game.items().timeStop().end(level);
            if(!retain){for(var e:owned){if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}game.items().stalin().tick();}
            var cleanupEntities=new ArrayList<Entity>();level.getAllEntities().forEach(cleanupEntities::add);
            for(var e:cleanupEntities)if(!initial.contains(e) && !(e instanceof ServerPlayer) && e.position().distanceToSqr(origin)<400 && (e instanceof ItemEntity || e instanceof ExperienceOrb))e.discard();
            for(var entry:blocks.entrySet()){game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());game.items().magic().blockChanged(entry.getKey(),level.getBlockState(entry.getKey()));}
            ownerState.restore(game);otherState.restore(game);game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);
            otherStats.forEach((stat,value)->other.getStats().setValue(other,stat,value));owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.ZOMBIFIED_PIGLIN),bossKills);
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=skill;profile.lbcTrinket=trinket;profile.lbcTrinketSlot=trinketSlot;System.arraycopy(trinkets,0,profile.trinkets,0,trinkets.length);
            if(!retain)for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game) {
        require(running==null && retained==null,"one fixture");running=new Fixture(game);
        try{running.initialize();}catch(RuntimeException|Error error){try{running.close();}finally{running=null;}throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var f=running;
        try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS stalin-encounter");}}
        catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL stalin-encounter",error);}
    }
    public static void stopProbe(GameController game) {
        require(running==null && retained==null,"exclusive stop fixture");var f=new Fixture(game);
        try{f.initialize();f.actor=f.activate();stopActors=List.of(f.actor);stopEpoch=game.items().epoch();f.retain=true;f.close();retained=f;
            game.items().timeStop().begin(f.level,200);stopToken=f.token(3);require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"frozen pending stop token");
            SkyWars2.LOGGER.info("[check] PASS stalin-encounter-stop-probe");}
        catch(RuntimeException|Error error){f.retain=false;f.close();throw error;}
    }
    public static void cleanup(GameController game) {
        require(retained!=null && stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && game.items().stalin().activeCount()==0 && stopActors.stream().allMatch(Entity::isRemoved),"actual stop quietly removes encounter and invalidates epoch");
        require(stopToken!=null && stopToken.getItem().getCount()==3 && stopToken.entityTags().contains(stopEpoch) && !game.items().specialDrops().spawn(stopToken),"stale pending token cannot replay or debit");
        require(retained.level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(retained.base).inflate(8)).stream().noneMatch(e->LbcAk47.isAk47(e.getItem())),"quiet actual stop emits no death reward");
        stopToken.discard();retained.retain=false;retained.close();retained=null;stopToken=null;stopEpoch=null;stopActors=List.of();SkyWars2.LOGGER.info("[check] PASS stalin-encounter-cleanup");
    }
}
