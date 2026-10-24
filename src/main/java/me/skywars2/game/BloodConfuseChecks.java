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
public final class BloodConfuseChecks {
    private BloodConfuseChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Blood/Confuse: "+why);}
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
            team=game.server().getScoreboard().addPlayerTeam("blood_conf_fixture");
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
        void presentation(){for(String name:List.of("blood_wand","confuse_wand"))for(boolean upgraded:new boolean[]{false,true}){
            var s=loot((upgraded?"academy_":"")+name);require(s.getCount()==1&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("skywars2:"+(upgraded?"academy_":"")+name),"native item leaf");
            require(flag(s,"staff")==1&&flag(s,"magicwand")==1&&flag(s,"transformable")==1&&flag(s,name.equals("blood_wand")?"bloodmagic":"foxusing")==1,"source flags");
            require(Identifier.parse(name.equals("blood_wand")?"lbc:killingwand":"lbc:foxuswand").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(upgraded?"lbc:tier_a":"lbc:tier_c").equals(s.get(DataComponents.TOOLTIP_STYLE)),"source model and tier");
            require(s.get(DataComponents.CUSTOM_NAME).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals(name)&&!s.getHoverName().getStyle().isItalic()&&s.get(DataComponents.LORE).lines().size()==(name.equals("blood_wand")?8:7)+(upgraded?1:0),"full original name/lore/style");
        }}
        void blood(){
            var victim=cow(at.add(0,0,5));var friend=cow(at.add(1,0,5));var technical=cow(at.add(-1,0,5));technical.entityTags().add("strict_map_object");var outside=cow(at.add(0,0,11));
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
            var base=loot("blood_wand");resetHealth();game.items().mana().set(owner,10000);require(use(base,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==5000,"actual Blood cast spends5000");
            require(victim.getHealth()==982&&friend.getHealth()==1000&&technical.getHealth()==1000&&outside.getHealth()==1000,"source18 damage, radius and protected targets");
            effect(victim,MobEffects.WITHER,600,1);effect(owner,MobEffects.WITHER,300,1);effect(owner,MobEffects.POISON,300,1);effect(owner,MobEffects.HUNGER,40,255);effect(owner,MobEffects.INSTANT_DAMAGE,1,1);
            owner.removeEffect(MobEffects.WITHER);owner.removeEffect(MobEffects.POISON);owner.removeEffect(MobEffects.HUNGER);MobEffects.INSTANT_DAMAGE.value().applyEffectTick(owner.level(),owner,1);owner.removeEffect(MobEffects.INSTANT_DAMAGE);require(owner.getHealth()==988,"native Instant Damage II costs12 HP, got "+owner.getHealth());
            // Tick the real vanilla Wither effect through its existing causal wrapper.
            victim.setHealth(1);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;MobEffects.WITHER.value().applyEffectTick(owner.level(),victim,1);
            require(!victim.isAlive()&&victim.getKillCredit()==owner,"delayed Wither death credits the wand caster");victim.discard();
            var later=cow(at.add(0,0,6));resetHealth();game.items().mana().set(owner,10000);require(use(loot("academy_blood_wand"),InteractionHand.OFF_HAND),"upgraded offhand Blood");
            require(later.getHealth()==982&&owner.getActiveEffects().isEmpty()&&owner.getHealth()==1000,"Academy version removes all self harm");
            later.setHealth(2);later.setInvulnerableTime(0);later.damageCooldownTime=0;require(later.hurtServer(owner.level(),owner.damageSources().playerAttack(other),1),"later real player hit");later.setInvulnerableTime(0);later.damageCooldownTime=0;
            MobEffects.WITHER.value().applyEffectTick(owner.level(),later,1);require(!later.isAlive()&&later.getKillCredit()==other,"later attacker overrides old wand's Wither credit");later.discard();
            resetHealth();var trial=loot("blood_wand");trial(trial);owner.setShiftKeyDown(true);game.items().mana().set(owner,10000);require(use(trial,InteractionHand.OFF_HAND)&&flag(trial,"upgrade_available")==1,"trial completes on the actual offhand item");owner.setShiftKeyDown(false);
            require(owner.getEffect(MobEffects.POISON).isInfiniteDuration()&&owner.getEffect(MobEffects.POISON).getAmplifier()==4&&owner.getEffect(MobEffects.WITHER).isInfiniteDuration()&&owner.getEffect(MobEffects.WITHER).getAmplifier()==4,"permanent source trial curse V");
            resetHealth();victim=friend;friend.setInvulnerableTime(0);friend.damageCooldownTime=0;game.server().getScoreboard().removePlayerFromTeam(owner.getScoreboardName(),team);
            friend.discard();technical.discard();outside.discard();
        }
        void set(BlockPos p,net.minecraft.world.level.block.state.BlockState state){p=p.immutable();blocks.computeIfAbsent(p,key->game.maps().snapshot(owner.level(),key));if(owner.level().getBlockEntity(p) instanceof net.minecraft.world.Container c)c.clearContent();owner.level().setBlock(p,state,818);}
        void floor(int y,boolean create){for(int x=(int)Math.floor(at.x-10);x<Math.ceil(at.x+10);x++)for(int z=(int)Math.floor(at.z-10);z<Math.ceil(at.z+10);z++)set(new BlockPos(x,y,z),create?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}
        void confuse(){
            resetHealth();owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);var target=cow(at.add(0,0,5));
            var s=loot("academy_confuse_wand");
            for(int m:new int[]{1,3,4}){resetHealth();mode(s,m);game.items().mana().set(owner,5000);require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==4100,"Confuse mode "+m+" cost900");
                if(m==1)effect(owner,MobEffects.INVISIBILITY,200,0);if(m==3){effect(owner,MobEffects.SPEED,200,3);effect(owner,MobEffects.JUMP_BOOST,200,3);}if(m==4){effect(owner,MobEffects.LEVITATION,20,10);effect(owner,MobEffects.SLOW_FALLING,160,0);require(!target.hasEffect(MobEffects.BLINDNESS),"Academy blindness is player-only");}
            }
            // Normal random outcomes eventually supply all six buffs/debuffs; use native random casts.
            var ordinary=loot("confuse_wand");trial(ordinary);floor(239,true);floor(294,true);
            for(int i=0;i<100&&flag(ordinary,"upgrade_available")==0;i++){
                owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);game.items().mana().set(owner,100000);require(use(ordinary,InteractionHand.OFF_HAND),"ordinary random Confuse");
            }
            require(flag(ordinary,"upgrade_available")==1&&target.hasEffect(MobEffects.BLINDNESS)&&target.hasEffect(MobEffects.SLOWNESS),"ordinary trial checks effects at the original cast origin after teleport");
            var award=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/confuse"));require(owner.getAdvancements().getOrStartProgress(award).isDone(),"real upward spread of at least50 blocks awards advancement");
            floor(294,false);owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),23,11,true);resetHealth();mode(s,2);game.items().mana().set(owner,5000);require(use(s,InteractionHand.MAIN_HAND),"surface teleport");
            require(owner.getY()==240&&Math.abs(owner.getX()-at.x)<=10.5&&Math.abs(owner.getZ()-at.z)<=10.5&&owner.getYRot()==23&&owner.getXRot()==11,"safe source square/cell center/yaw/pitch");effect(target,MobEffects.SLOWNESS,60,50);
            var distant=new Vec3(1000000.5,240,1000000.5);var pos=BlockPos.containing(distant);require(!owner.level().hasChunkAt(pos),"unloaded probe premise");require(LbcBloodConfuse.spreadDestination(owner.level(),distant,owner.getRandom())==null&&!owner.level().hasChunkAt(pos),"empty/unloaded columns do not load chunks or loop");
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);owner.setShiftKeyDown(true);game.items().mana().set(owner,-100);mode(s,1);require(use(s,InteractionHand.OFF_HAND)&&flag(s,"mode")==2&&game.items().mana().get(owner)==-100,"free offhand mode change at negative mana");owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            resetHealth();target.discard();
        }
        void alternating(){
            resetHealth();owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);
            var s=loot("academy_confuse_wand");mode(s,4);owner.setShiftKeyDown(true);game.items().mana().set(owner,-100);
            require(use(s,InteractionHand.OFF_HAND)&&flag(s,"mode")==5&&game.items().mana().get(owner)==-100,"free selection of alternating Confuse");
            owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==0,"failed mana never starts Confuse cycle");
            for(int action=1;action<=5;action++){
                owner.removeAllEffects();game.items().mana().set(owner,10000);
                require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==9100&&flag(s,"mode")==5&&flag(s,"alternating_next")==action%4+1,"paid Confuse cycle "+action);
                if(action==1||action==5)effect(owner,MobEffects.INVISIBILITY,200,0);
                if(action==3)effect(owner,MobEffects.SPEED,200,3);
                if(action==4)effect(owner,MobEffects.LEVITATION,20,10);
            }
            game.items().mana().set(owner,900-1);require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==2,"failed mana preserves running Confuse cursor");game.items().mana().set(owner,10000);
            var blocker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());blocker.setPos(owner.position());blocker.entityTags().add("stopper_magic");owner.level().addFreshEntity(blocker);game.items().magic().loaded(blocker);
            try{require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==2&&game.items().mana().get(owner)==10000,"antimagic preserves Confuse cursor and mana");}finally{game.items().magic().unloaded(blocker);blocker.discard();}
            game.items().timeStop().begin(owner.level(),200);
            try{require(!use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==2,"Time Stop preserves Confuse cursor");}finally{game.items().timeStop().end(owner.level());}
            var left=loot("academy_confuse_wand");mode(left,5);owner.setItemInHand(InteractionHand.OFF_HAND,left);game.items().mana().set(owner,10000);
            require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"alternating_next")==3&&flag(left,"alternating_next")==2&&game.items().mana().get(owner)==8200,"two Confuse stacks retain independent cursor and costs");
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setShiftKeyDown(true);require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"mode")==1&&flag(s,"alternating_next")==3,"Confuse selection wraps and preserves cursor");owner.setShiftKeyDown(false);resetHealth();
        }
        void admission(){
            var s=loot("academy_confuse_wand");game.items().mana().set(owner,899);require(!use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==899,"cost admission");
            game.items().mana().set(owner,10000);game.items().timeStop().begin(owner.level(),200);try{require(!use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==10000,"dimension Time Stop rejects without cost");}finally{game.items().timeStop().end(owner.level());}
            var marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);
            try{require(!use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==10000,"antimagic rejects cast");}finally{game.items().magic().unloaded(marker);marker.discard();}
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var left=loot("academy_confuse_wand");mode(left,3);owner.setItemInHand(InteractionHand.OFF_HAND,left);mode(s,1);int old=game.items().mana().get(owner);require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==old-1800&&owner.hasEffect(MobEffects.INVISIBILITY)&&owner.hasEffect(MobEffects.SPEED)&&owner.getOffhandItem()==left,"both hands cast independently without item replacement");
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);resetHealth();
        }
        int phase;long lastTime=Long.MIN_VALUE;net.minecraft.world.entity.Interaction panel;
        boolean academyStep(){long now=owner.level().getGameTime();if(now==lastTime)return false;lastTime=now;
            if(panel==null){panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.entityTags().add("magic_academy");panel.setPos(at);owner.level().addFreshEntity(panel);}
            String name=phase<2?"blood_wand":"confuse_wand";
            if(phase%2==0){var s=loot(name);owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(s),"Academy admits "+name);game.items().academy().ready(owner,InteractionHand.MAIN_HAND,s);}
            else {require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(name.equals("blood_wand")?LbcWands.ACADEMY_BLOOD:LbcWands.ACADEMY_CONFUSE)&&Identifier.parse("lbc:tier_a").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real Academy promotion "+name);}
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
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.checked){if(!f.warm())return;f.presentation();f.blood();f.confuse();f.alternating();f.admission();f.checked=true;}if(f.academyStep()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS blood-confuse");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL blood-confuse",e);}}
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopState==null,"exclusive actual stop");stopState=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        owner.removeAllEffects();owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,10000);var s=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/blood_wand").getFirst();owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&owner.hasEffect(MobEffects.WITHER)&&owner.hasEffect(MobEffects.POISON),"real harmful native effects before actual stop");epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS blood-confuse-stop-probe");}
    public static void cleanup(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));try{require(stopState!=null&&epoch!=null&&!game.items().currentEpoch(epoch)&&!owner.hasEffect(MobEffects.WITHER)&&!owner.hasEffect(MobEffects.POISON)&&!owner.hasEffect(MobEffects.HUNGER),"actual match stop removes harmful effects");}finally{if(stopState!=null)stopState.restore(game);stopState=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS blood-confuse-cleanup");}
}
