package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Closed native acquisition, actual melee hook and native arrow collision fixtures. */
public final class VexFamilyChecks {
    private VexFamilyChecks() { }
    private static Fixture running;
    private static String stopEpoch;
    private static Fixture stopping;
    private static List<Entity> stopActors=List.of();
    private static ItemEntity stopToken;
    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Vex family: "+message);
    }
    private static double arrowDamage(Arrow arrow) {
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,arrow.registryAccess());
        arrow.saveWithoutId(out);return out.buildResult().getDoubleOr("damage",0);
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState state,otherState;
        final Vec3 origin;
        final BlockPos base;
        final Set<Entity> initial=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>(),otherStats=new LinkedHashMap<>();
        final Map<String,Integer> scores=new LinkedHashMap<>();
        final boolean offhand,mobDrops;
        final int skill,trinket,trinketSlot,challenge,challengeProgress,otherChallengeProgress;
        final int[] trinkets;
        final Set<String> unlocks;
        int phase,lastTick,wait;
        Cow target,protectedTarget,lethalTarget;
        ItemStack sword;
        ItemEntity normal,old;
        Arrow pausedArrow;
        Vec3 pausedPosition;
        List<Entity> normalActors=List.of(),oldActors=List.of();
        Fixture(GameController game) {
            this.game=game;level=game.maps().level();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && owner!=null && other!=null && game.allowDamage(other),"two actual active match participants");
            require(game.items().vexSword().activeCount()==0,"exclusive sword fixture");
            state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherState=new GameChecks.WandThirdPlayerState(other,game.items().mana());
            offhand=game.config().offhandWands;mobDrops=level.getGameRules().get(GameRules.MOB_DROPS);
            var p=game.players().get(owner.getUUID());skill=p.lbcSkill;trinket=p.lbcTrinket;trinketSlot=p.lbcTrinketSlot;
            challenge=p.lbcChallenge;challengeProgress=p.lbcChallengeProgress;otherChallengeProgress=game.players().get(other.getUUID()).lbcChallengeProgress;
            trinkets=p.trinkets.clone();unlocks=new HashSet<>(p.lbcUnlocks);
            for(String score:List.of("swordcharging4","swordcharged4"))scores.put(score,game.items().mana().score(owner,score).get());
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT) {
                var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));otherStats.put(stat,other.getStats().getValue(stat));
            }
            origin=new Vec3(Math.floor(owner.getX()/16)*16+8.37,250.7,Math.floor(owner.getZ()/16)*16+8.81);base=BlockPos.containing(origin);
            level.getAllEntities().forEach(initial::add);
        }
        void initialize() {
            for(int x=(base.getX()-35)>>4;x<=(base.getX()+35)>>4;x++)for(int z=(base.getZ()-35)>>4;z<=(base.getZ()+35)>>4;z++) {
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);
            }
            for(var p:BlockPos.betweenClosed(base.offset(-8,-3,-4),base.offset(8,7,15)))set(p,Blocks.AIR.defaultBlockState());
            for(var player:List.of(owner,other)) {
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
            }
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;p.lbcChallenge=0;Arrays.fill(p.trinkets,0);
            game.config().offhandWands=true;game.items().vexSword().clear(owner);place(origin);
            other.teleportTo(level,origin.x+24,origin.y+10,origin.z+24,Set.of(),0,0,true);
            level.getGameRules().set(GameRules.MOB_DROPS,true,game.server());lastTick=game.server().getTickCount();
        }
        void set(BlockPos p,net.minecraft.world.level.block.state.BlockState value) {
            p=p.immutable();blocks.computeIfAbsent(p,pos->game.maps().snapshot(level,pos));
            if(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(p,value,818);
        }
        void place(Vec3 at) {owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);owner.setDeltaMovement(Vec3.ZERO);}
        ItemEntity token(LbcCreatureDropItem.Kind kind,int count) {
            var t=new ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(kind.item(),count));t.setThrower(owner);
            t.setNoGravity(true);t.setDeltaMovement(Vec3.ZERO);t.setPickUpDelay(1000);require(level.addFreshEntity(t),"native token");owned.add(t);return t;
        }
        Cow cow(Vec3 at) {
            var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);
            require(level.addFreshEntity(c),"native cow");owned.add(c);return c;
        }
        void reset(LivingEntity victim) {victim.setHealth(victim.getMaxHealth());victim.setInvulnerableTime(0);victim.damageCooldownTime=0;}
        int mobKills(ServerPlayer player) {return player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));}
        void capture() {
            for(var e:game.items().creatureDrops().snapshot())if(!initial.contains(e))owned.add(e);
            owned.addAll(game.items().vexSword().arrowSnapshot());
        }
        void hand(InteractionHand hand) {
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            owner.setItemInHand(hand,sword.copy());game.items().mana().inventoryChanged(owner);
        }
        boolean nativeUse(InteractionHand hand) {return owner.gameMode.useItem(owner,level,owner.getItemInHand(hand),hand).consumesAction();}
        void presentationAndAdmission() {
            try(var input=VexFamilyChecks.class.getResourceAsStream("/data/lbc/loot_table/vex_sword.json")) {
                require(input!=null,"native10% death wrapper resource");
                var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var pools=json.getAsJsonArray("pools");require(pools.size()==1,"single source death pool");
                var pool=pools.get(0).getAsJsonObject();var entries=pool.getAsJsonArray("entries");require(pool.get("rolls").getAsInt()==1 && entries.size()==1,"one death reward roll");
                var entry=entries.get(0).getAsJsonObject();require(entry.get("value").getAsString().equals("lbc:vexsword"),"original complete source reward table");
                var condition=entry.has("condition")?entry.getAsJsonObject("condition"):pool.has("condition")?pool.getAsJsonObject("condition"):null;
                require(condition!=null && condition.get("type").getAsString().equals("minecraft:random_chance")
                        && condition.get("chance").getAsDouble()==.1 && !pool.has("conditions"),"active native codec condition retains independent source10% chance");
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
            sword=game.lbc().rollTable(level,origin,owner,"lbc:ported/vex_sword").getFirst();
            require(sword.is(LbcVexSword.ITEM) && sword.getCount()==1 && LbcVexSword.isSword(sword),"public leaf yields functional native sword");
            var flags=sword.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(flags.keySet().equals(Set.of("transformable","sword","vexsword","magicwand","swordwithability")),"all source flags, no added flags");
            for(var key:flags.keySet())require(flags.getIntOr(key,0)==1,"source flag "+key);
            require(Identifier.parse("lbc:vex_sword").equals(sword.get(DataComponents.ITEM_MODEL))
                    && Identifier.parse("lbc:tier_b").equals(sword.get(DataComponents.TOOLTIP_STYLE))
                    && sword.get(DataComponents.LORE).lines().size()==5 && !sword.getHoverName().getStyle().isItalic(),"source model/tier/five lore rows/style");
            var direct=game.lbc().rollTable(level,origin,owner,"lbc:vexsword");
            require(direct.size()==1 && ItemStack.isSameItemSameComponents(sword,direct.getFirst()),"public leaf retains complete source reward JSON components");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.doTick();
            double unheldAttack=owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
            hand(InteractionHand.MAIN_HAND);owner.doTick();
            require(Math.abs(owner.getAttributeValue(Attributes.ATTACK_DAMAGE)-unheldAttack-2)<1e-6,
                    "native held source +2 attack damage; unheld="+unheldAttack+" held="+owner.getAttributeValue(Attributes.ATTACK_DAMAGE));
            var publicToken=game.lbc().rollTable(level,origin,owner,"lbc:ported/vex_drop");
            require(publicToken.size()==1 && publicToken.getFirst().is(LbcCreatureDropItem.Kind.VEX.item()),"public normal acquisition leaf");
            var fake=token(LbcCreatureDropItem.Kind.VEX,2);fake.makeFakeItem();
            require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount()==2,"fake source token rejected without debit");fake.discard();
            normal=token(LbcCreatureDropItem.Kind.VEX,2);old=token(LbcCreatureDropItem.Kind.OLD_VEX,2);
            require(game.items().creatureDrops().ready(normal,LbcCreatureDrops.Kind.VEX),"normal loaded admission");
            require(game.items().specialDrops().spawn(normal) && normal.getItem().getCount()==1,"normal native dispatcher one debit");
            require(!game.items().specialDrops().spawn(normal) && normal.getItem().getCount()==1,"same tick normal replay free rejection");
            require(game.items().specialDrops().spawn(old) && old.getItem().getCount()==1,"concurrent old native dispatcher one debit");capture();
            normalActors=game.items().creatureDrops().snapshot().stream().filter(e->!initial.contains(e)&&e.entityTags().contains("skywars2.normal_vex_drop")).toList();
            oldActors=game.items().creatureDrops().snapshot().stream().filter(e->!initial.contains(e)&&!e.entityTags().contains("skywars2.normal_vex_drop")).toList();
            require(normalActors.size()==10 && oldActors.size()==5,"normal ten distinct from old first five");
            for(var e:normalActors) {
                require(e instanceof Vex v && v.getHealth()==1 && v.getMaxHealth()==1 && e.position().equals(origin)
                        && game.items().creatureDrops().owner(e)==owner && game.items().currentEpoch(e),"ten co-located1HP native authored Vex");
                require(((Vex)e).getMainHandItem().isEmpty() && ((Vex)e).getOffhandItem().isEmpty(),"normal LowVex trap has no sword in either hand");
                ((Vex)e).setNoAi(true);e.setNoGravity(true);
            }
        }
        void rewardAndMelee() {
            capture();require(normal.isRemoved() && old.isRemoved(),"second actual dispatcher tick consumes each remaining unit");
            var all=game.items().creatureDrops().snapshot().stream().filter(e->!initial.contains(e)).toList();
            require(all.stream().filter(e->e.entityTags().contains("skywars2.normal_vex_drop")).count()==20
                    && all.stream().filter(e->!e.entityTags().contains("skywars2.normal_vex_drop")).count()==10,"concurrent second native debit keeps normal/old geometry independent");
            var actor=(Vex)normalActors.getFirst();long seed=0;
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:vex_sword")));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,actor)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,actor.damageSources().generic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            for(int i=1;i<=512 && seed==0;i++)if(!table.getRandomItems(params,i*0x9E3779B97F4A7C15L).isEmpty())seed=i*0x9E3779B97F4A7C15L;
            require(seed!=0,"bounded deterministic source chance seed");
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());actor.saveWithoutId(output);
            var tag=output.buildResult();tag.putLong("DeathLootTableSeed",seed);
            actor.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
            require(actor.getLootTableSeed()==seed,"actual native death seed retains all actor tags");
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
            require(actor.hurtServer(level,actor.damageSources().generic(),1000) && !actor.isAlive(),"ordinary native death without player condition");
            var rewards=new ArrayList<ItemEntity>();for(var e:level.getAllEntities())if(!before.contains(e)&&e instanceof ItemEntity item){owned.add(item);rewards.add(item);}
            require(rewards.size()==1 && ItemStack.isSameItemSameComponents(rewards.getFirst().getItem(),sword),"native seeded10% death emits complete working sword");
            all.forEach(Entity::discard);rewards.forEach(Entity::discard);
            target=cow(origin.add(0,0,4));protectedTarget=cow(origin.add(5,0,4));protectedTarget.entityTags().add("magic_academy");
            protectedTarget.setPermanentlyInvulnerable(true);
            lethalTarget=cow(origin.add(6,0,8));
            hand(InteractionHand.MAIN_HAND);game.items().vexSword().clear(owner);
        }
        void meleeAndArrows() {
            for(int i=0;i<10;i++) {
                reset(target);owner.attack(target);
                require(target.getHealth()<10,"real Player.attack accepted damage "+i);
                require(game.items().vexSword().chargeProgress(owner)==(i+1)%10 && game.items().vexSword().charges(owner)==(i+1)/10,"actual AfterDamage hook accumulates tenth hit "+i);
            }
            // Consecutive actual attacks retain vanilla cooldown scaling; this is a lethal accepted hit.
            int ownerKills=mobKills(owner);
            lethalTarget.setHealth(.1f);owner.attack(lethalTarget);
            require(!lethalTarget.isAlive() && game.items().vexSword().chargeProgress(owner)==1
                    && game.items().vexSword().charges(owner)==1 && mobKills(owner)==ownerKills+1,"actual lethal native melee charges after accepted death and credits native MOB_KILLS");
            capture();require(game.items().vexSword().arrowCount()==11,"one native side arrow per accepted melee including lethal victim");
            for(var arrow:game.items().vexSword().arrowSnapshot())arrow.discard();game.items().vexSword().tick();
            int progress=game.items().vexSword().chargeProgress(owner);
            var projectile=EntityTypes.SNOWBALL.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(projectile!=null,"native attributed non-arrow fixture");
            reset(target);target.hurtServer(level,target.damageSources().thrown(projectile,owner),1);
            require(game.items().vexSword().chargeProgress(owner)==progress,"attributed non-melee damage never charges");
            reset(protectedTarget);owner.attack(protectedTarget);
            require(protectedTarget.getHealth()==10 && game.items().vexSword().chargeProgress(owner)==progress,"service target protects native attack and charge");
            require(game.items().vexSword().sideShot(owner,protectedTarget,1)==null,"ability role filter independently rejects service target");
            game.items().vexSword().clear(owner);reset(target);
            for(int side:new int[]{-1,1}) {
                var arrow=game.items().vexSword().sideShot(owner,target,side);require(arrow!=null,"deterministic native side shot");owned.add(arrow);
                require(arrow.getOwner()==owner && arrowDamage(arrow)==2 && arrow.getPierceLevel()==100
                        && arrow.position().distanceTo(owner.getEyePosition().add(side*3,0,1))<1e-6,"side origin/native owner/base damage/pierce");
                require(arrow.getYRot()==arrow.yRotO && arrow.getXRot()==arrow.xRotO,"first-frame previous/current native arrow orientation agrees");
                require(arrow.getDeltaMovement().normalize().distanceTo(target.getEyePosition().subtract(arrow.position()).normalize())<1e-6,"side shot aims once at actual damaged victim");
                float health=target.getHealth();for(int i=0;i<10 && target.getHealth()==health;i++)level.tickNonPassenger(arrow);
                require(target.getHealth()<health,"native flight resolves side arrow collision");arrow.discard();reset(target);
            }
            int reflectedKills=mobKills(other);
            var reflected=game.items().vexSword().sideShot(owner,target,1);owned.add(reflected);reflected.setOwner(other);target.setHealth(1);
            for(int i=0;i<10 && target.isAlive();i++)level.tickNonPassenger(reflected);
            require(!target.isAlive() && target.getKillCredit()==other && mobKills(other)==reflectedKills+1,"native reflected side arrow credits current owner and MOB_KILLS");reflected.discard();target.discard();
            target=cow(origin.add(0,0,6));
            pausedArrow=game.items().vexSword().sideShot(owner,target,-1);owned.add(pausedArrow);pausedPosition=pausedArrow.position();
            game.items().timeStop().begin(level,200);level.tickNonPassenger(pausedArrow);
            require(pausedArrow.position().equals(pausedPosition),"native ServerLevel tick gate pauses arrow");
        }
        void ultimate() {
            require(pausedArrow.position().equals(pausedPosition),"actual world ticks preserve paused arrow");game.items().timeStop().end(level);
            // An authorized arrow is independent of the player's later spectator/death role.
            target.setHealth(1);game.spectators().enter(owner);
            try {
                for(int i=0;i<12 && target.isAlive();i++)level.tickNonPassenger(pausedArrow);
                require(!target.isAlive() && target.getKillCredit()==owner,"already-launched native arrow retains author after source becomes observer");
            } finally { game.spectators().remove(owner);owner.setGameMode(GameType.SURVIVAL);place(origin); }
            target.discard();target=cow(origin.add(0,0,6));
            game.items().vexSword().clear(owner);owner.removeAllEffects();hand(InteractionHand.MAIN_HAND);
            int mana=game.items().mana().get(owner);
            require(!nativeUse(InteractionHand.MAIN_HAND) && game.items().mana().get(owner)==mana && game.items().vexSword().zoneCount()==0,"empty charge native use declines freely");
            game.items().vexSword().addPlayerCounter(owner);require(game.items().vexSword().hudValue(owner)==1,"KFC adds one source charged unit");
            require(nativeUse(InteractionHand.MAIN_HAND) && game.items().vexSword().charges(owner)==0,"normal main hand consumes exactly one charge");
            var invis=owner.getEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
            var lev=owner.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
            var slow=owner.getEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
            require(invis!=null && invis.getDuration()==1200 && invis.getAmplifier()==1 && lev!=null && lev.getDuration()==20 && lev.getAmplifier()==30
                    && slow!=null && slow.getDuration()==400,"native60s invisibilityII/1s Levitation31/20s Slow Falling");
            var zone=game.items().vexSword().zoneSnapshot().getFirst();require(zone.origin.equals(origin) && zone.remaining==70,"source anchored70-active-tick zone");
            place(origin.add(20,10,0));game.items().timeStop().begin(level,200);
            for(int i=0;i<5;i++)game.items().vexSword().tick();require(zone.remaining==70,"dimension Time Stop pauses source zone duration");game.items().timeStop().end(level);
            other.getInventory().setItem(17,sword.copy());other.teleportTo(level,origin.x+2,origin.y,origin.z+6,Set.of(),0,0,true);
            for(int x:new int[]{-4,-2,2,4})cow(origin.add(x,0,10));
            game.items().timeStop().begin(level,200);
        }
        void zoneAndForms() {
            game.items().timeStop().end(level);
            var service=game.items().vexSword();var zone=service.zoneSnapshot().getFirst();int before=service.arrowCount();
            service.tick();capture();
            require(zone.remaining==69 && service.arrowCount()==before+5,"anchored box fires five independently randomized shots at eligible enemies");
            // Keep exactly one eligible enemy: all five shots must still fire with replacement.
            var excluded=new ArrayList<LivingEntity>();
            for(var actor:level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(zone.origin.x-30,zone.origin.y-30,zone.origin.z-30,
                            zone.origin.x+31,zone.origin.y+6,zone.origin.z+31))) {
                if(actor!=target && actor!=protectedTarget && !actor.entityTags().contains("magic_academy")) {
                    actor.entityTags().add("magic_academy");excluded.add(actor);
                }
            }
            try {
                for(var arrow:service.arrowSnapshot())arrow.discard();
                service.tick();capture();
                require(service.arrowCount()==5 && target.isAlive(),
                        "five fresh random shots can choose the same still-living sole eligible enemy");
                for(var arrow:service.arrowSnapshot())require(arrow.position().distanceTo(target.position())>=4.9
                        && arrow.position().distanceTo(target.position())<=5.01,
                        "every sole-target shot spawns around the eligible enemy");
                var second=cow(zone.origin.add(12,0,6));
                owner.getRandom().setSeed(1717L);
                var chosen=new HashSet<LivingEntity>();
                for(int update=0;update<4;update++) {
                    for(var arrow:service.arrowSnapshot())arrow.discard();
                    service.tick();capture();
                    require(service.arrowCount()==5,"every update independently emits five area shots");
                    for(var arrow:service.arrowSnapshot()) {
                        var aim=arrow.position().add(arrow.getDeltaMovement().scale(5));
                        if(aim.distanceTo(target.position())<.00001) chosen.add(target);
                        else if(aim.distanceTo(second.position())<.00001) chosen.add(second);
                        else throw new IllegalStateException("Vex family: field shot selected ineligible target at "+aim);
                    }
                }
                require(chosen.size()==2 && target.isAlive() && second.isAlive(),
                        "fresh shot rolls switch between two living targets without waiting for either death");
                second.discard();
            } finally {for(var actor:excluded)actor.entityTags().remove("magic_academy");}
            for(var arrow:service.arrowSnapshot())arrow.discard();service.tick();
            double[][] form={{5,0,-1,0},{-5,0,1,0},{0,5,0,-1},{0,-5,0,1},{3.5,3.5,-.7,-.7},{-3.5,3.5,.7,-.7},{3.5,-3.5,-.7,.7},{-3.5,-3.5,.7,.7}};
            for(int i=0;i<8;i++) {
                var arrow=service.phantomShot(owner,target,i+1);require(arrow!=null,"native phantom form "+i);owned.add(arrow);
                require(arrow.position().distanceTo(target.position().add(form[i][0],0,form[i][1]))<1e-6
                        && arrow.getDeltaMovement().distanceTo(new Vec3(form[i][2],0,form[i][3]))<1e-6
                        && arrow.getPierceLevel()==100 && arrowDamage(arrow)==2.5 && arrow.entityTags().contains("groundbreaker") && !arrow.entityTags().contains("magic"),"all eight exact source offsets/motion, source tags and user all100 pierce "+i);
                arrow.discard();
            }
            require(service.phantomShot(owner,other,1)==null,"inventory sword grants ultimate targeting immunity");
            var block=BlockPos.containing(target.position().add(3,0,0));set(block,Blocks.STONE.defaultBlockState());set(block.above(),Blocks.STONE.defaultBlockState());
            var breaker=service.phantomShot(owner,target,1);owned.add(breaker);
            for(int i=0;i<5&&!breaker.isRemoved();i++)level.tickNonPassenger(breaker);
            require(breaker.isRemoved() && level.getBlockState(block).isAir(),"actual native block hit invokes groundbreaker terrain destruction and retires arrow");
            for(int i=0;i<63;i++)service.tick();require(service.zoneCount()==0,"zone expires at exactly70 active updates");
            service.clear(owner);place(origin);hand(InteractionHand.OFF_HAND);game.config().offhandWands=false;service.addPlayerCounter(owner);
            require(!nativeUse(InteractionHand.OFF_HAND) && service.charges(owner)==1,"offhand configuration declines without spend");game.config().offhandWands=true;
            require(nativeUse(InteractionHand.OFF_HAND) && service.charges(owner)==0,"normal allowed offhand creates working zone");service.clear(owner);
            hand(InteractionHand.MAIN_HAND);service.addPlayerCounter(owner);owner.entityTags().add("spectator");
            try{require(!service.use(owner,InteractionHand.MAIN_HAND) && service.charges(owner)==1,"role guard declines without spend");}finally{owner.entityTags().remove("spectator");}
            var retired=service.sideShot(owner,target,-1);require(retired!=null,"native unload lifecycle arrow");owned.add(retired);
            require(!retired.shouldBeSaved(),"ephemeral native sword arrow is never persisted");service.unloaded(retired);
            require(retired.isRemoved() && !service.arrowSnapshot().contains(retired),"unload quietly retires owned arrow");service.loaded(retired);
            require(retired.isRemoved() && !service.arrowSnapshot().contains(retired),"retired loaded arrow cannot resurrect");
            service.clear(owner);
        }
        boolean step() {
            int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;require(game.playing(),"actual match retained");
            switch(phase) {
                case 0->{presentationAndAdmission();phase++;}
                case 1->{rewardAndMelee();phase++;}
                case 2->{if(++wait<2)return false;meleeAndArrows();phase++;}
                case 3->{ultimate();phase++;}
                case 4->{if(++wait<4)return false;zoneAndForms();return true;}
                default->throw new IllegalStateException("Unknown Vex fixture phase "+phase);
            }
            return false;
        }
        void close(boolean retain) {
            capture();game.items().timeStop().end(level);if(!retain){game.items().vexSword().clear(owner);owned.forEach(Entity::discard);}
            for(var e:level.getEntities((Entity)null,new net.minecraft.world.phys.AABB(origin.add(-12,-8,-12),origin.add(12,12,20)),e->!initial.contains(e)&&(e instanceof ItemEntity||e.getType()==EntityTypes.EXPERIENCE_ORB)))if(!retain||!owned.contains(e))e.discard();
            for(var entry:blocks.entrySet()){if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());}
            state.restore(game);otherState.restore(game);game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcTrinketSlot=trinketSlot;p.lbcChallenge=challenge;p.lbcChallengeProgress=challengeProgress;
            game.players().get(other.getUUID()).lbcChallengeProgress=otherChallengeProgress;
            System.arraycopy(trinkets,0,p.trinkets,0,trinkets.length);p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);
            stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));otherStats.forEach((stat,value)->other.getStats().setValue(other,stat,value));
            if(!retain)scores.forEach((score,value)->game.items().mana().score(owner,score).set(value));game.config().offhandWands=offhand;
            level.getGameRules().set(GameRules.MOB_DROPS,mobDrops,game.server());game.items().magic().inventoryChanged(owner);
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
        public void close(){close(false);}
    }
    public static void run(GameController game) {
        require(running==null && stopping==null,"one fixture at a time");var f=new Fixture(game);running=f;
        try{f.initialize();}catch(RuntimeException|Error error){running=null;f.close();throw error;}
    }
    public static void tick(GameController game) {
        if(running==null)return;var f=running;
        try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS vex-family");}}
        catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vex-family",error);}
    }
    public static void stopProbe(GameController game) {
        require(running==null && stopping==null,"exclusive actual stop fixture");var f=new Fixture(game);stopping=f;
        try {
            f.initialize();f.sword=game.lbc().rollTable(f.level,f.origin,f.owner,"lbc:ported/vex_sword").getFirst();f.hand(InteractionHand.MAIN_HAND);
            game.items().vexSword().addPlayerCounter(f.owner);require(f.nativeUse(InteractionHand.MAIN_HAND),"actual pre-stop zone");
            var t=f.token(LbcCreatureDropItem.Kind.VEX,1);require(game.items().specialDrops().spawn(t),"normal actors before actual stop");t.discard();f.capture();
            stopActors=f.owned.stream().filter(e->e instanceof Vex && e.entityTags().contains("skywars2.normal_vex_drop")).toList();
            require(stopActors.size()==10,"ten normal actors before actual stop");stopEpoch=game.items().epoch();
            f.close(true);game.items().timeStop().begin(f.level,200);stopToken=f.token(LbcCreatureDropItem.Kind.VEX,3);
            require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"frozen pending normal token");
            SkyWars2.LOGGER.info("[check] PASS vex-family-stop-probe");
        }catch(RuntimeException|Error error){stopping=null;f.close();throw error;}
    }
    public static void cleanup(GameController game) {
        require(stopping!=null && stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && stopToken!=null && stopToken.getItem().getCount()==3
                && stopActors.size()==10 && stopActors.stream().allMatch(Entity::isRemoved)
                && game.items().creatureDrops().snapshot().stream().noneMatch(e->e.entityTags().contains("skywars2.normal_vex_drop"))
                && game.items().vexSword().activeCount()==0,"actual stop quietly closes normal Vex, zone and pending token");
        require(!game.items().specialDrops().spawn(stopToken),"stopped token replay rejected");stopToken.discard();
        var f=stopping;f.scores.forEach((score,value)->game.items().mana().score(f.owner,score).set(value));
        stopping=null;stopEpoch=null;stopToken=null;stopActors=List.of();SkyWars2.LOGGER.info("[check] PASS vex-family-cleanup");
    }
}
