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

/** Real native ordnance, source payloads, current ownership and quiet lifecycle checks. */
public final class NeutralWandChecks {
    private NeutralWandChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Neutral: "+why);}
    private static Fixture running;
    private static GameChecks.WandThirdPlayerState stopState;
    private static List<net.minecraft.world.entity.projectile.Projectile> stopped=List.of();
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
            owner.level().getAllEntities().forEach(before::add);
            team=game.server().getScoreboard().addPlayerTeam("neutral_fixture");
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
        net.minecraft.nbt.CompoundTag data(Entity e){var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,e.registryAccess());e.saveWithoutId(out);return out.buildResult();}
        void clear(){for(var e:game.items().neutral().snapshot())e.discard();game.items().neutral().tick();}
        ItemStack academy(int m){var s=loot("academy_neutral_wand");mode(s,m);return s;}
        List<net.minecraft.world.entity.projectile.Projectile> cast(int m){owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().set(owner,100000);require(use(academy(m),InteractionHand.MAIN_HAND),"native cast mode "+m);return game.items().neutral().snapshot();}
        void presentation(){for(boolean upgraded:new boolean[]{false,true}){
            var s=loot(upgraded?"academy_neutral_wand":"neutral_wand");require(s.is(upgraded?LbcWands.ACADEMY_NEUTRAL:LbcWands.NEUTRAL)&&s.getCount()==1,"native public wand");
            require(flag(s,"neutralmage")==1&&flag(s,"magicwand")==1&&flag(s,"staff")==1&&flag(s,"transformable")==1&&flag(s,"craftusable")==1,"source flags");
            require(Identifier.parse("lbc:neutralwand").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(upgraded?"lbc:tier_a":"lbc:tier_b").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.CUSTOM_NAME).getStyle().getColor().getValue()==0x5555FF&&!s.get(DataComponents.CUSTOM_NAME).getStyle().isItalic()&&s.get(DataComponents.LORE).lines().size()==11+(upgraded?1:0),"full source blue name/model/tier/lore");
        }
            var feather=loot("feather_greed");require(feather.is(LbcWands.FEATHER_GREED)&&flag(feather,"feather_greed")==1&&flag(feather,"magicwand")==1&&feather.get(DataComponents.CUSTOM_DATA).copyTag().size()==2&&Identifier.parse("minecraft:feather").equals(feather.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_d").equals(feather.get(DataComponents.TOOLTIP_STYLE))&&Boolean.TRUE.equals(feather.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))&&feather.get(DataComponents.LORE).lines().size()==1,"complete original feather payload");
        }
        void payloads(){
            for(int m=1;m<=5;m++){
                var shots=cast(m);require(shots.size()==(m==3?1:3)&&game.items().mana().get(owner)==98800,"source count/cost mode "+m);
                int index=0;for(var shot:shots){require(shot.getOwner()==owner&&game.items().currentEpoch(shot)&&ProjectileLifetimes.protectedWandProjectile(shot),"real owner/epoch/no incidental physics");
                    int side=m==3?0:m==4?index-1:new int[]{0,1,-1}[index];double forward=m==1||m==5?5:m==4?1:3;
                    require(shot.position().distanceTo(owner.getEyePosition().add(side*.1,0,1))<1e-6&&shot.getDeltaMovement().distanceTo(new Vec3(side*forward*.1,0,forward))<1e-6,"exact native local origin/Motion mode "+m);
                    require(Math.abs(shot.getYRot())<7&&Math.abs(shot.getXRot())<1e-4,"initial spawn rotation follows flight");
                    if(m==1)require(shot instanceof net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull,"three native skulls");
                    if(m==2)require(shot instanceof net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball&&game.items().wands().isFireProjectile(shot),"three native fireballs with existing ignition cause");
                    if(m==3)require(shot instanceof net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball&&data(shot).getByteOr("ExplosionPower",(byte)0)==2,"one power2 native fireball");
                    if(m==4){var r=(net.minecraft.world.entity.projectile.FireworkRocketEntity)shot;var fireworks=r.getItem().get(DataComponents.FIREWORKS);int color=new int[]{16777215,4607,16711680}[index];require(data(r).getIntOr("LifeTime",0)==100&&data(r).getBooleanOr("ShotAtAngle",false)&&fireworks.explosions().size()==4&&fireworks.explosions().stream().allMatch(e->e.colors().getInt(0)==color&&e.fadeColors().getInt(0)==color&&e.hasTrail()&&e.hasTwinkle()),"native lifetime100 and four exact colored/trailing/twinkling shapes");}
                    if(m==5){var p=(net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion)shot;var potion=p.getItem().get(DataComponents.POTION_CONTENTS);require(Boolean.TRUE.equals(p.getItem().get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))&&potion.customColor().orElse(0)==16719841&&potion.customEffects().size()==4&&potion.customEffects().stream().allMatch(e->e.getAmplifier()==3&&e.getDuration()==60),"exact four levelIV native potion effects/color/glint");}
                    require(!game.items().neutral().canAffect(shot,owner),"caster protection");index++;
                }clear();
            }
        }
        static final class ProbeCow extends Cow {
            final List<Float> magic=new ArrayList<>();
            ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){if(source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC))magic.add(amount);return super.hurtServer(level,source,amount);}
        }
        ProbeCow victim(){var c=new ProbeCow(owner.level());c.setPos(at.add(0,1,8));c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void collide(net.minecraft.world.entity.projectile.Projectile shot,Cow c){shot.setPos(c.getX(),c.getY()+.5,c.getZ()-1.2);shot.setDeltaMovement(new Vec3(0,0,2));c.setInvulnerableTime(0);c.damageCooldownTime=0;shot.tick();}
        void combat(){
            var c=victim();var skull=cast(1).getFirst();for(var p:game.items().neutral().snapshot())if(p!=skull)p.discard();
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(c.getScoreboardName(),team);
            owner.setHealth(500);c.setHealth(5);collide(skull,c);require(skull.isRemoved()&&!c.isAlive()&&c.magic.equals(List.of(5f))&&c.getKillCredit()==owner&&owner.getHealth()==500,"real native skull5 damage, friendly-mob kill credit and no added healing");c.discard();clear();
            game.server().getScoreboard().removePlayerFromTeam(owner.getScoreboardName(),team);
            // Reflection changes actual owner; all subsequent direct/status/area effects follow it.
            c=victim();skull=cast(1).getFirst();for(var p:game.items().neutral().snapshot())if(p!=skull)p.discard();
            require(skull.deflect(net.minecraft.world.entity.projectile.ProjectileDeflection.REVERSE,other,net.minecraft.world.entity.EntityReference.of(other),false,1.0),"explicit native reflection");require(skull.getOwner()==other,"native current owner");c.setHealth(5);collide(skull,c);require(!c.isAlive()&&c.getKillCredit()==other&&c.magic.equals(List.of(5f)),"reflected skull keeps source damage and credits reflector");c.discard();clear();
            for(int m:new int[]{2,3,4}){c=victim();var p=cast(m).getFirst();for(var extra:game.items().neutral().snapshot())if(extra!=p)extra.discard();c.setHealth(1);collide(p,c);require(p.isRemoved()&&!c.isAlive()&&c.getKillCredit()==owner,"native impact/explosion credits caster mode "+m);c.discard();clear();}
            c=victim();var potion=cast(5).getFirst();for(var extra:game.items().neutral().snapshot())if(extra!=potion)extra.discard();collide(potion,c);
            require(potion.isRemoved()&&c.hasEffect(MobEffects.WEAKNESS)&&c.hasEffect(MobEffects.POISON)&&c.hasEffect(MobEffects.WITHER)&&c.hasEffect(MobEffects.GLOWING),"actual native splash applies all four source effects");
            c.setHealth(1);c.setInvulnerableTime(0);c.damageCooldownTime=0;MobEffects.WITHER.value().applyEffectTick(owner.level(),c,3);require(!c.isAlive()&&c.getKillCredit()==owner,"potion's delayed Wither credits caster");c.discard();clear();
        }
        void trialAndHands(){
            var first=loot("neutral_wand");var second=loot("neutral_wand");trial(first);trial(second);
            for(int i=0;i<2;i++){owner.getRandom().setSeed(1234);game.items().mana().set(owner,10000);require(use(first,InteractionHand.OFF_HAND),"random admitted offhand cast");clear();}
            require(flag(first,"score")==1&&flag(first,"upgrade_available")==0,"first two identical results only build combo");
            owner.getRandom().setSeed(1234);use(second,InteractionHand.OFF_HAND);clear();require(flag(second,"score")==0,"separate stack owns its own trial combo");
            owner.getRandom().setSeed(1234);use(first,InteractionHand.OFF_HAND);clear();require(flag(first,"upgrade_available")==1&&flag(first,"score")==2&&flag(second,"score")==0,"third identical result completes only the actually used stack");
            var s=academy(1);owner.setShiftKeyDown(true);game.items().mana().set(owner,-100);require(use(s,InteractionHand.OFF_HAND)&&flag(s,"mode")==2&&game.items().mana().get(owner)==-100,"free negative-mana mode switch");owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            game.items().mana().set(owner,1199);require(!use(s,InteractionHand.MAIN_HAND),"cost admission");game.items().mana().set(owner,10000);
            game.items().timeStop().begin(owner.level(),200);try{require(!use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==10000,"Time Stop cast admission");}finally{game.items().timeStop().end(owner.level());}
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);
            try{require(!use(s,InteractionHand.MAIN_HAND)&&!use(loot("feather_greed"),InteractionHand.OFF_HAND)&&game.items().mana().get(owner)==10000,"antimagic blocks wand and feather");}finally{game.items().magic().unloaded(marker);marker.discard();}
            owner.setItemInHand(InteractionHand.OFF_HAND,academy(3));int mana=game.items().mana().get(owner);require(use(academy(2),InteractionHand.MAIN_HAND)&&game.items().neutral().snapshot().size()==4&&game.items().mana().get(owner)==mana-2400,"independent parallel native casts in both hands");clear();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        }
        void alternating(){
            clear();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var s=academy(5);owner.setShiftKeyDown(true);game.items().mana().set(owner,-100);
            require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"mode")==6&&game.items().mana().get(owner)==-100,"free alternating Neutral selection");owner.setShiftKeyDown(false);
            require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==0,"failed initial mana preserves Neutral cursor");
            var types=List.of(EntityTypes.WITHER_SKULL,EntityTypes.SMALL_FIREBALL,EntityTypes.FIREBALL,EntityTypes.FIREWORK_ROCKET,EntityTypes.SPLASH_POTION);
            for(int action=1;action<=6;action++){
                clear();game.items().mana().set(owner,10000);int index=(action-1)%5;
                require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==8800&&flag(s,"mode")==6&&flag(s,"alternating_next")==action%5+1,"paid Neutral alternating cycle "+action);
                var shots=game.items().neutral().snapshot();require(shots.size()==(index==2?1:3)&&shots.stream().allMatch(shot->shot.getType()==types.get(index)&&shot.getOwner()==owner),"alternating exact native projectile family/count/owner");
            }
            clear();game.items().mana().set(owner,1199);require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==2,"failed running mana preserves Neutral cursor");game.items().mana().set(owner,10000);
            game.items().timeStop().begin(owner.level(),200);try{require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==2&&game.items().neutral().snapshot().isEmpty(),"Time Stop preserves Neutral cursor");}finally{game.items().timeStop().end(owner.level());}
            var blocker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());blocker.setPos(owner.position());blocker.entityTags().add("stopper_magic");owner.level().addFreshEntity(blocker);game.items().magic().loaded(blocker);
            try{require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==2&&game.items().mana().get(owner)==10000,"antimagic preserves Neutral cursor and debit");}finally{game.items().magic().unloaded(blocker);blocker.discard();}
            var left=academy(6);owner.setItemInHand(InteractionHand.OFF_HAND,left);game.items().mana().set(owner,10000);
            require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==3&&flag(left,"alternating_next")==2&&game.items().mana().get(owner)==7600,"independent two-hand Neutral cursors/costs");
            require(game.items().neutral().snapshot().stream().filter(shot->shot.getType()==EntityTypes.WITHER_SKULL).count()==3&&game.items().neutral().snapshot().stream().filter(shot->shot.getType()==EntityTypes.SMALL_FIREBALL).count()==3,"distinct hand cycles produce skulls and fireballs");
            clear();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setShiftKeyDown(true);require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"mode")==1&&flag(s,"alternating_next")==3,"Neutral mode selection wraps preserving cursor");owner.setShiftKeyDown(false);
        }
        void feather(){var s=loot("feather_greed");game.items().mana().set(owner,-100);
            var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};
            var items=new net.minecraft.world.item.Item[]{Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS};
            for(int i=0;i<4;i++){var armor=new ItemStack(items[i]);var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("mage",1);armor.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));owner.setItemSlot(slots[i],armor);}
            require(game.items().mana().mageFullSet(owner),"full mage hook premise");int maximum=game.items().mana().maximum(owner);require(use(s,InteractionHand.MAIN_HAND)&&s.getCount()==1&&owner.getMainHandItem()==s&&owner.getEffect(MobEffects.SLOW_FALLING).getDuration()==1200&&owner.getEffect(MobEffects.SLOW_FALLING).getAmplifier()==0&&owner.getEffect(MobEffects.SLOW_FALLING).isVisible()&&game.items().mana().get(owner)==-100&&game.items().mana().maximum(owner)==maximum,"native60s visible slow-falling, no cost/consumption/mage cast bonus");
            for(var slot:slots)owner.setItemSlot(slot,ItemStack.EMPTY);owner.removeAllEffects();
        }
        int phase,wait;long lastTime=Long.MIN_VALUE;net.minecraft.world.entity.Interaction panel;
        final Map<Entity,Vec3> frozen=new IdentityHashMap<>();
        boolean step(){long now=owner.level().getGameTime();if(now==lastTime)return false;lastTime=now;
            if(phase==0){panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.entityTags().add("magic_academy");panel.setPos(at);owner.level().addFreshEntity(panel);var s=loot("neutral_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(s),"real Academy admission");game.items().academy().ready(owner,InteractionHand.MAIN_HAND,s);phase++;return false;}
            if(phase==1){require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_NEUTRAL)&&Identifier.parse("lbc:tier_a").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real Academy promotion");panel.discard();
                for(int m=1;m<=5;m++){var s=academy(m);game.items().mana().set(owner,10000);require(use(s,InteractionHand.MAIN_HAND),"freeze setup");}require(game.items().neutral().snapshot().size()==13,"all native types coexist");for(var p:game.items().neutral().snapshot())frozen.put(p,p.position());game.items().timeStop().begin(owner.level(),200);phase++;return false;}
            require(frozen.size()==13&&frozen.entrySet().stream().allMatch(e->!e.getKey().isRemoved()&&e.getKey().position().distanceTo(e.getValue())<1e-8),"actual dimension Time Stop freezes all five native families");if(++wait<3)return false;game.items().timeStop().end(owner.level());return true;
        }
        public void close(){game.items().timeStop().end(owner.level());clear();var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=skill;profile.lbcTrinket=trinket;profile.lbcChallengeProgress=challengeProgress;profile.lbcUnlocks.clear();profile.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.checked){if(!f.warm())return;f.presentation();f.payloads();f.combat();f.trialAndHands();f.alternating();f.feather();f.checked=true;}if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS neutral-wand");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL neutral-wand",e);}}
    public static void stopProbe(GameController game){var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopState==null,"exclusive actual stop");stopState=new GameChecks.WandThirdPlayerState(p,game.items().mana());p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(p,0);
        for(int m=1;m<=5;m++){var s=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/academy_neutral_wand").getFirst();var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("mode",m);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));p.setItemInHand(InteractionHand.MAIN_HAND,s);game.items().mana().set(p,10000);require(game.items().wands().use(p,InteractionHand.MAIN_HAND),"actual pre-stop native cast");}
        stopped=game.items().neutral().snapshot();require(stopped.size()==13,"thirteen projectiles before actual stop");epoch=game.items().epoch();game.items().timeStop().begin(p.level(),200);SkyWars2.LOGGER.info("[check] PASS neutral-wand-stop-probe");}
    public static void cleanup(GameController game){try{require(stopState!=null&&epoch!=null&&!game.items().currentEpoch(epoch)&&stopped.size()==13&&stopped.stream().allMatch(Entity::isRemoved)&&game.items().neutral().snapshot().isEmpty(),"actual match stop quietly removes all native ordnance");}finally{if(stopState!=null)stopState.restore(game);stopState=null;epoch=null;stopped=List.of();}SkyWars2.LOGGER.info("[check] PASS neutral-wand-cleanup");}
}
