package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native instant casts, harmful-effect attribution, resident surface selection and real Academy/stop fixtures. */
public final class CausticOmegaChecks {
    private CausticOmegaChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Caustic/Omega: "+why);}
    private static Fixture running;
    private static GameChecks.WandThirdPlayerState stopState;
    private static String epoch;
    private static final class Fixture implements AutoCloseable {
        boolean checked;int warmTicks;
        boolean warm(){require(++warmTicks<=200,"resident fixture becomes entity ticking");for(int x=-18;x<=-12;x++)for(int z=-18;z<=-12;z++)if(!owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)))return false;return true;}
        final GameController game; final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();
        final boolean offhand;
        final int skill,trinket,challengeProgress;
        final Set<String> unlocks;
        final Set<Identifier> advancements=new HashSet<>();
        final Map<Identifier,Set<String>> completed=new HashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;
            var profile=game.players().get(owner.getUUID());skill=profile.lbcSkill;trinket=profile.lbcTrinket;challengeProgress=profile.lbcChallengeProgress;unlocks=new HashSet<>(profile.lbcUnlocks);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            for(String id:List.of("lbc:true_advancements/staffs/blood","lbc:true_advancements/staffs/confuse")){
                var key=Identifier.parse(id);var a=game.server().getAdvancements().get(key);if(a!=null){advancements.add(key);var prior=new HashSet<String>();owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(prior::add);completed.put(key,prior);}
            }
            owner.level().getAllEntities().forEach(before::add);
            team=game.server().getScoreboard().addPlayerTeam("caustic_omega_fixture");
        }
        void init(){
            for(int x=-18;x<=-12;x++)for(int z=-18;z<=-12;z++){long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(owner.level(),at.x+90,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;
            game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);game.config().offhandWands=true;
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);
        }
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one loot item "+leaf);return list.getFirst();}
        void mode(ItemStack stack,int mode){var d=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack stack,InteractionHand hand){owner.setItemInHand(hand,stack);return owner.gameMode.useItem(owner,owner.level(),stack,hand).consumesAction();}
        Cow cow(Vec3 p){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        int flag(ItemStack s,String key){return s.get(DataComponents.CUSTOM_DATA).copyTag().getIntOr(key,0);}
        void trial(ItemStack s){var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("upgradable",1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        void resetHealth(){owner.removeAllEffects();owner.setHealth(1000);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        void effect(LivingEntity target,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int ticks,int amplifier){require(target.hasEffect(type)&&target.getEffect(type).getDuration()==ticks&&target.getEffect(type).getAmplifier()==amplifier,"exact native "+type.getRegisteredName());}
        void clearAcid(){for(var shot:game.items().clouds().acidSnapshot())if(shot.getOwner()==owner)shot.discard();}
        void reset(LivingEntity target){target.removeAllEffects();target.setHealth(target.getMaxHealth());target.setInvulnerableTime(0);target.damageCooldownTime=0;}
        void presentation(){for(String name:List.of("caustic_wand","omega_flag"))for(boolean upgraded:new boolean[]{false,true}){
            var s=loot((upgraded?"academy_":"")+name);String id=(upgraded?"academy_":"")+name;
            require(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("skywars2:"+id),"native leaf identity "+id);
            require(flag(s,name.equals("caustic_wand")?"acidwand":"omegaflag")==1&&flag(s,"staff")==1&&flag(s,"transformable")==1,"source flags");
            require(Identifier.parse(name.equals("caustic_wand")?"lbc:acidwand":"lbc:omegaflag").equals(s.get(DataComponents.ITEM_MODEL)),"source models");
            require(s.get(DataComponents.CUSTOM_NAME).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals(name.equals("caustic_wand")?"acid_wand":"omega_flag")&&!s.getHoverName().getStyle().isItalic(),"source names/styles");
            require(flag(s,"upgraded")== (upgraded?1:0)&&s.get(DataComponents.LORE).lines().size()==(name.equals("caustic_wand")?10:6)+(upgraded?1:0),"source lore and Academy flags");
        }}
        void acid(){
            resetHealth();owner.setShiftKeyDown(false);game.items().mana().set(owner,10000);
            var s=loot("caustic_wand");trial(s);owner.setRemainingFireTicks(100);
            require(use(s,InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==8800&&flag(s,"upgrade_available")==1,"native offhand6 volley, cost1200 and actual used-stack fire trial");
            owner.clearFire();var shots=game.items().clouds().acidSnapshot().stream().filter(p->p.getOwner()==owner&&!p.isRemoved()).toList();
            require(shots.size()==6,"six native acid charges");
            for(var p:shots)require(game.items().clouds().acidLifetime(p)==300&&p.getDeltaMovement().length()>.49&&p.getDeltaMovement().length()<.51&&p.entityTags().contains("shield_blockable2")&&!p.entityTags().contains("on_fire"),"shared physics, shield, lifetime and unignited launch");
            var p=shots.getFirst();shots.subList(1,shots.size()).forEach(Entity::discard);
            p.setPos(at.add(0,3,6));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            var victim=cow(p.position());var friend=cow(p.position().add(.5,0,0));var technical=cow(p.position().add(-.5,0,0));technical.entityTags().add("strict_map_object");
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
            other.teleportTo(other.level(),p.getX(),p.getY(),p.getZ(),Set.of(),0,0,true);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);other.removeAllEffects();
            game.items().clouds().tick();effect(victim,MobEffects.POISON,300,0);effect(friend,MobEffects.POISON,300,0);
            require(!technical.hasEffect(MobEffects.POISON)&&!other.hasEffect(MobEffects.POISON),"friendly mobs hit, allied player/technical protected");
            MobEffects.POISON.value().applyEffectTick(owner.level(),victim,0);require(victim.getKillCredit()==owner,"native acid poison tick inherits owner");
            // On block contact the same native projectile remains alive and immobile until its own age expires.
            var wall=BlockPos.containing(at.add(0,3,9));blocks.putIfAbsent(wall,game.maps().snapshot(owner.level(),wall));owner.level().setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            p.setPos(wall.getX()+.5,wall.getY()+.2,wall.getZ()-.2);p.setDeltaMovement(0,0,.5);p.tick();var stuck=p.position();
            require(!p.isRemoved()&&p.entityTags().contains("skywars2.acid_stuck")&&p.getDeltaMovement().equals(Vec3.ZERO),"native wall contact sticks");p.tick();require(p.position().equals(stuck),"sticky acid stays on surface");
            int age=game.items().clouds().acidAge(p);for(int i=age;i<299;i++)game.items().clouds().tick();require(!p.isRemoved(),"alive before age300");game.items().clouds().tick();require(p.isRemoved(),"removed at source300");
            clearAcid();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.clearFire();resetHealth();
            var academy=loot("academy_caustic_wand");mode(academy,2);game.items().mana().set(owner,5000);require(use(academy,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==3800,"Academy price1200");
            shots=game.items().clouds().acidSnapshot().stream().filter(q->q.getOwner()==owner&&!q.isRemoved()).toList();require(shots.size()==8&&shots.stream().allMatch(q->q.entityTags().contains("on_fire")),"eight freshly ignited charges only");
            var flaming=shots.getFirst();shots.subList(1,shots.size()).forEach(Entity::discard);flaming.setPos(victim.position());reset(victim);game.items().clouds().tick();require(victim.getHealth()==997&&victim.getKillCredit()==owner,"source3 native fire damage and cause");
            clearAcid();game.items().mana().set(owner,5000);require(use(loot("caustic_wand"),InteractionHand.MAIN_HAND),"unrelated ordinary cast");
            var old=game.items().clouds().acidSnapshot().stream().filter(q->q.getOwner()==owner&&!q.isRemoved()).toList();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(use(academy,InteractionHand.MAIN_HAND),"new Academy cast");require(old.stream().noneMatch(q->q.entityTags().contains("on_fire")),"does not ignite earlier nearby charges");
            clearAcid();owner.setShiftKeyDown(true);game.items().mana().set(owner,-100000);require(use(academy,InteractionHand.MAIN_HAND)&&flag(academy,"mode")==1&&game.items().mana().get(owner)==-100000,"free negative-mana mode switch");owner.setShiftKeyDown(false);
            require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"actual ability still checks mana");
            victim.discard();friend.discard();technical.discard();other.teleportTo(other.level(),at.x+50,at.y,at.z,Set.of(),0,0,true);
        }
        void omega(){
            resetHealth();owner.setShiftKeyDown(false);reset(other);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);other.setHealth(1000);other.setAbsorptionAmount(0);
            var victim=cow(at.add(0,70,0));var friend=cow(at.add(0,70,1));var technical=cow(at.add(0,70,-1));technical.entityTags().add("strict_map_object");
            game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);team.setAllowFriendlyFire(false);
            var s=loot("omega_flag");game.items().mana().set(owner,10000);require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==7000,"ordinary global spell costs3000");
            require(owner.getHealth()==994&&other.getHealth()==994&&victim.getHealth()==994&&friend.getHealth()==994&&technical.getHealth()==1000,"WILD reaches high distant mob, caster and same-team player with friendlyFire=false");
            require(victim.getKillCredit()==owner&&other.getKillCredit()==owner,"native direct damage credits actual caster");
            resetHealth();reset(other);reset(victim);reset(friend);var academy=loot("academy_omega_flag");game.items().mana().set(owner,10000);require(use(academy,InteractionHand.OFF_HAND),"Academy global spell offhand");
            require(owner.getHealth()==1000&&other.getHealth()==1000&&friend.getHealth()==1000&&victim.getHealth()==994,"Academy protects owner and ALL allies, enemies hit");
            resetHealth();s=loot("omega_flag");trial(s);owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RESISTANCE,100,4));game.items().mana().set(owner,10000);require(use(s,InteractionHand.OFF_HAND)&&flag(s,"upgrade_available")==1,"offhand trial when self damage fully resisted");
            victim.discard();friend.discard();technical.discard();resetHealth();reset(other);
        }
        int phase;long lastTime=Long.MIN_VALUE;net.minecraft.world.entity.Interaction panel;
        boolean academyStep(){long now=owner.level().getGameTime();if(now==lastTime)return false;lastTime=now;
            if(panel==null){panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.entityTags().add("magic_academy");panel.setPos(at);owner.level().addFreshEntity(panel);}
            String name=phase<2?"caustic_wand":"omega_flag";
            if(phase%2==0){var s=loot(name);owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(s),"Academy admits "+name);game.items().academy().ready(owner,InteractionHand.MAIN_HAND,s);}
            else require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(name.equals("caustic_wand")?LbcWands.ACADEMY_CAUSTIC:LbcWands.ACADEMY_OMEGA),"real Academy promotion "+name);
            return ++phase==4;
        }
        public void close(){
            game.items().timeStop().end(owner.level());var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var key:advancements){var a=game.server().getAdvancements().get(key);for(String c:a.value().criteria().keySet())if(!completed.get(key).contains(c))owner.getAdvancements().revoke(a,c);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error error){running=null;f.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.checked){if(!f.warm())return;f.presentation();f.acid();f.omega();f.checked=true;}if(f.academyStep()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS caustic-omega");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL caustic-omega",e);}}
    private static java.util.List<LbcClouds.AcidProjectile> stopped=List.of();
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopState==null,"exclusive actual stop");stopState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,10000);var s=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/caustic_wand").getFirst();owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"actual live cast before stop");stopped=game.items().clouds().acidSnapshot().stream().filter(p->p.getOwner()==owner&&!p.isRemoved()).toList();require(stopped.size()==6,"six live stop probes");epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS caustic-omega-stop-probe");}
    public static void cleanup(GameController game){try{require(stopState!=null&&epoch!=null&&!game.items().currentEpoch(epoch)&&stopped.stream().allMatch(Entity::isRemoved),"actual match stop removes native acid");}finally{if(stopState!=null)stopState.restore(game);stopState=null;epoch=null;stopped=List.of();}SkyWars2.LOGGER.info("[check] PASS caustic-omega-cleanup");}
}
