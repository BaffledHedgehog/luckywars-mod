package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native corruption input, Academy, crafting, owned acid actors and bounded lifecycle probes. */
public final class CorruptionChecks {
    private CorruptionChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Corruption: "+why);}
    private static Fixture running;
    /** Native mob kills can award unlocks and challenge progress to either credited participant. */
    private static final class ProfileState {
        final PlayerStore.Profile target;final int coins,memcoins,games,wins,kills,deaths,skill,trinket,challenge,progress,challengeKit;
        final Set<String> unlocks;
        ProfileState(PlayerStore.Profile p){target=p;coins=p.coins;memcoins=p.memcoins;games=p.games;wins=p.wins;kills=p.kills;deaths=p.deaths;skill=p.lbcSkill;trinket=p.lbcTrinket;challenge=p.lbcChallenge;progress=p.lbcChallengeProgress;challengeKit=p.lbcChallengeKit;unlocks=new HashSet<>(p.lbcUnlocks);}
        void restore(){target.coins=coins;target.memcoins=memcoins;target.games=games;target.wins=wins;target.kills=kills;target.deaths=deaths;target.lbcSkill=skill;target.lbcTrinket=trinket;target.lbcChallenge=challenge;target.lbcChallengeProgress=progress;target.lbcChallengeKit=challengeKit;target.lbcUnlocks.clear();target.lbcUnlocks.addAll(unlocks);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final ProfileState ownerProfile,otherProfile;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Long> forced=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> otherStats=new LinkedHashMap<>();
        final Map<Identifier,Set<String>> advancements=new LinkedHashMap<>();
        final net.minecraft.stats.Stat<net.minecraft.world.entity.EntityType<?>> cowStat=net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.COW);
        final int ownerCowKills,otherCowKills;
        final Set<String> unlocks;final boolean offhand;final int skill,trinket,challengeProgress,ownerDeath,otherDeath;
        final net.minecraft.world.scores.PlayerTeam team;
        boolean warmed;int warmTicks,phase,waits;long last=Long.MIN_VALUE;
        ItemStack trial;net.minecraft.world.entity.Interaction panel;LbcAcidCorruption.Charge frozen;Cow doomed,later,frozenDeath;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;
            ownerCowKills=owner.getStats().getValue(cowStat);otherCowKills=other.getStats().getValue(cowStat);
            ownerProfile=new ProfileState(game.players().get(owner.getUUID()));otherProfile=new ProfileState(game.players().get(other.getUUID()));
            var p=game.players().get(owner.getUUID());skill=p.lbcSkill;trinket=p.lbcTrinket;challengeProgress=p.lbcChallengeProgress;unlocks=new HashSet<>(p.lbcUnlocks);
            ownerDeath=game.items().mana().score(owner,"delayed_death").get();otherDeath=game.items().mana().score(other,"delayed_death").get();
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(s,owner.getStats().getValue(s));otherStats.put(s,other.getStats().getValue(s));}
            for(String key:List.of("lbc:true_advancements/staffs/corruption","lbc:true_advancements/legends/academy/corruption","lbc:true_advancements/legends/tuntija_wands/acidcorrupt","lbc:true_advancements/legends/academy/acidcorrupt")){
                var id=Identifier.parse(key);var advancement=game.server().getAdvancements().get(id);if(advancement!=null){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(id,done);}
            }
            owner.level().getAllEntities().forEach(before::add);team=game.server().getScoreboard().addPlayerTeam("corruption_fixture");team.setAllowFriendlyFire(false);
        }
        void init(){
            for(int x=-16;x<=-9;x++)for(int z=-16;z<=-14;z++){long k=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(k)){forced.add(k);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);owner.setShiftKeyDown(false);
            other.closeContainer();other.getInventory().clearContent();other.removeAllEffects();other.setGameMode(GameType.SURVIVAL);other.setNoGravity(true);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;game.config().offhandWands=true;
            for(var player:List.of(owner,other)){game.items().mana().set(player,1000000);game.items().mana().noMagic(player,0);game.items().mana().reverse(player,0);game.items().acidCorruption().clear(player);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);}
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
        }
        boolean warm(){if(warmed)return true;require(++warmTicks<=600,"resident fixture chunks");for(long k:forced)if(!owner.level().areEntitiesLoaded(k))return false;warmed=true;return true;}
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one item "+leaf);return list.getFirst();}
        int flag(ItemStack s,String key){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
        void flag(ItemStack s,String key,int value){var d=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt(key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack s,InteractionHand hand){owner.setItemInHand(hand,s);return owner.gameMode.useItem(owner,owner.level(),s,hand).consumesAction();}
        ItemStack mode(int mode){var s=loot("academy_acid_corruption_wand");flag(s,"mode",mode);return s;}
        Cow cow(Vec3 point){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(point);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void block(BlockPos p,net.minecraft.world.level.block.Block type){blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,type.defaultBlockState(),2);}
        void remove(LivingEntity e){game.items().acidCorruption().clear(e);game.items().indirectKills().clear(e);e.discard();}
        void clearActors(){for(var actor:List.copyOf(game.items().acidCorruption().snapshot()))actor.discard();}
        void craft(boolean workbench){
            owner.closeContainer();owner.getInventory().clearContent();var pos=owner.blockPosition().east();if(workbench){block(pos,Blocks.CRAFTING_TABLE);owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(owner.level(),pos)),net.minecraft.network.chat.Component.literal("test")));}
            var menu=(net.minecraft.world.inventory.AbstractCraftingMenu)owner.containerMenu;var slots=menu.getInputGridSlots();slots.getFirst().setByPlayer(new ItemStack(LbcWands.ACID_WAND,2));slots.getLast().setByPlayer(loot("corruption_wand").copyWithCount(2));
            require(menu.getResultSlot().getItem().is(LbcWands.ACID_CORRUPTION),"native "+(workbench?"3x3":"2x2")+" Acid + Corruption output");slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra ingredient rejected");slots.get(1).setByPlayer(ItemStack.EMPTY);
            for(int i=0;i<36;i++)owner.getInventory().setItem(i,new ItemStack(Items.STICK,64));GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);require(slots.getFirst().getItem().getCount()==2&&slots.getLast().getItem().getCount()==2,"full inventory retains both donors");
            GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);require(menu.getCarried().is(LbcWands.ACID_CORRUPTION)&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"one native result consumes one of each donor");menu.setCarried(ItemStack.EMPTY);slots.forEach(s->s.setByPlayer(ItemStack.EMPTY));owner.closeContainer();owner.getInventory().clearContent();
        }
        void presentation(){
            for(boolean academy:new boolean[]{false,true})for(boolean acid:new boolean[]{false,true}){
                var s=loot((academy?"academy_":"")+(acid?"acid_corruption_wand":"corruption_wand"));
                require(s.is(acid?(academy?LbcWands.ACADEMY_ACID_CORRUPTION:LbcWands.ACID_CORRUPTION):(academy?LbcWands.ACADEMY_CORRUPTION:LbcWands.CORRUPTION))&&flag(s,acid?"acidcorrupt_wand":"corrupter")==1,"native source identities/flags");
                require(Identifier.parse(acid?"lbc:acidcorrupt":"lbc:corruptwand").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(acid?(academy?"lbc:tier_ss":"lbc:tier_s"):"lbc:tier_c").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(acid?4:9)+(academy?1:0),"original models, C/C and S/SS tiers, exact lore sizes");
                require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals(acid?"acidcorrupt_wand":"corruption_wand")&&!s.getHoverName().getStyle().isItalic(),"original translated names/style");
            }
            craft(false);craft(true);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            game.items().mana().set(owner,1799);require(!LbcCorruption.use(game,owner,InteractionHand.MAIN_HAND,loot("corruption_wand"),false)&&game.items().mana().get(owner)==1799,"donor1800 admission");
            game.items().mana().set(owner,1209);require(!game.items().acidCorruption().use(owner,InteractionHand.MAIN_HAND,loot("acid_corruption_wand"),false)&&game.items().mana().get(owner)==1209,"continuation1210 admission");
            for(String leaf:List.of("academy_corruption_wand","academy_acid_corruption_wand")){
                var s=loot(leaf);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);game.items().mana().set(owner,-10000);owner.setShiftKeyDown(true);
                for(int mode:new int[]{2,3,4,1})require(use(s,InteractionHand.OFF_HAND)&&flag(s,"mode")==mode&&game.items().mana().get(owner)==-10000,"free four-mode cycle on actual offhand at negative mana");owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            }
            owner.setShiftKeyDown(false);game.items().mana().set(owner,1000000);
        }
        void donor(){
            var victim=cow(at.add(3,0,0));var friend=cow(at.add(-3,0,0));game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);var technical=cow(at.add(0,0,-3));technical.entityTags().add("strict_map_object");
            var undead=new net.minecraft.world.entity.monster.zombie.Zombie(EntityTypes.ZOMBIE,owner.level());undead.setPos(at.add(4,0,0));undead.setNoAi(true);undead.setNoGravity(true);owner.level().addFreshEntity(undead);
            var s=loot("academy_corruption_wand");
            for(int mode=1;mode<=4;mode++){flag(s,"mode",mode);int mana=game.items().mana().get(owner);require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana-1800,"actual Academy donor mode/cost");
                if(mode==1)require(victim.hasEffect(MobEffects.POISON)&&victim.getEffect(MobEffects.POISON).getDuration()==200&&!victim.getEffect(MobEffects.POISON).isVisible()&&undead.hasEffect(MobEffects.WITHER)&&undead.getEffect(MobEffects.WITHER).getAmplifier()==1,"source poison200/I and unholy wither200/II");
                if(mode==2)require(victim.getEffect(MobEffects.WITHER).getDuration()==140&&victim.getEffect(MobEffects.WITHER).getAmplifier()==1,"source wither140/II");
                if(mode==3)require(victim.getEffect(MobEffects.HUNGER).getDuration()==40&&victim.getEffect(MobEffects.HUNGER).getAmplifier()==255,"source hunger40/256");
                if(mode==4)require(victim.getHealth()==992&&victim.getKillCredit()==owner,"native8 cactus and caster credit");
            }
            require(friend.getHealth()==1000&&friend.getActiveEffects().isEmpty()&&technical.getHealth()==1000&&technical.getActiveEffects().isEmpty(),"enemy-only donor protects friendly mobs and technical roles");remove(victim);remove(friend);remove(technical);remove(undead);
            panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(at);panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);trial=loot("corruption_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,trial);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(trial),"native donor Academy admission");
        }
        void donorTrial(){
            var victim=cow(at.add(3,0,0));for(var effect:List.of(MobEffects.POISON,MobEffects.WITHER,MobEffects.HUNGER))victim.addEffect(new MobEffectInstance(effect,600,0),owner);
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.getRandom().setSeed(8023);
            for(int i=0;i<64&&flag(trial,"upgrade_available")==0;i++){victim.setInvulnerableTime(0);victim.setHealth(1000);require(use(trial,InteractionHand.OFF_HAND),"actual ordinary trial cast");}
            require(flag(trial,"upgrade_available")==1,"ordinary random damage branch qualifies actual offhand trial");remove(victim);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        }
        void acidModes(){
            owner.setYRot(0);owner.setXRot(0);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var victim=cow(owner.getEyePosition().add(0,-.6,3));var nearby=cow(owner.getEyePosition().add(3,-.6,3));game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);
            require(use(mode(3),InteractionHand.MAIN_HAND)&&game.items().acidCorruption().deathRemaining(victim)==120&&game.items().acidCorruption().deathRemaining(nearby)==0,"mode3 direct friendly entity curse120 without area application");
            require(use(mode(3),InteractionHand.MAIN_HAND)&&game.items().acidCorruption().deathRemaining(victim)==240,"each direct curse adds120 seconds");remove(victim);remove(nearby);
            var wall=BlockPos.containing(owner.getEyePosition().add(0,0,4));block(wall,Blocks.STONE);nearby=cow(Vec3.atCenterOf(wall).add(2,-.6,0));require(use(mode(3),InteractionHand.MAIN_HAND)&&game.items().acidCorruption().deathRemaining(nearby)==0,"mode3 block hit never curses neighbours");remove(nearby);
            other.teleportTo(other.level(),wall.getX()+2,at.y,wall.getZ(),Set.of(),0,0,true);game.items().mana().reverse(owner,0);game.items().mana().reverse(other,0);
            require(use(mode(4),InteractionHand.MAIN_HAND)&&game.items().mana().reverse(owner)==30&&game.items().mana().reverse(other)==30,"mode4 WILD radius10 includes caster and allied player");
            require(use(mode(4),InteractionHand.MAIN_HAND)&&game.items().mana().reverse(other)==60,"mode4 extends reverse mana by30");
            game.items().clearEffects(owner);game.items().clearEffects(other);require(game.items().mana().reverse(owner)==0&&game.items().mana().reverse(other)==0,"shared purify clears reverse mana");other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);block(wall,Blocks.AIR);
            // A real reverse regeneration creates debt; the native damage context proves its author without killing a participant.
            game.items().mana().applyReverse(owner,other,30);game.items().mana().baseRegen(other,10);game.items().mana().set(other,-100);game.items().mana().tickPlayer(other);
            require(game.items().mana().get(other)<-100,"actual reverse regeneration drains allied player mana");other.setInvulnerableTime(0);other.damageCooldownTime=0;
            game.items().indirectKills().manaDamage(other,()->{require(other.hurtServer(other.level(),other.damageSources().fellOutOfWorld(),1),"accepted native nonlethal overload boundary");game.items().indirectKills().dying(other,other.damageSources().fellOutOfWorld());});
            require(other.getKillCredit()==owner&&other.isAlive(),"reversed native regeneration debt credits caster across WILD allied-player boundary");game.items().clearEffects(other);game.items().mana().set(other,1000000);game.items().mana().baseRegen(other,0);
            game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);other.setLastHurtByPlayer((ServerPlayer)null,0);other.setLastHurtByMob(null);other.getFoodData().setFoodLevel(0);
            other.addEffect(new MobEffectInstance(MobEffects.HUNGER,20,255),owner);game.items().indirectKills().environmentDamage(other,other.damageSources().starve());require(other.getKillCredit()==owner,"native hunger starvation cause credits caster");
            game.items().clearEffects(other);other.setLastHurtByPlayer((ServerPlayer)null,0);other.setLastHurtByMob(null);game.items().indirectKills().environmentDamage(other,other.damageSources().starve());require(other.getKillCredit()==null,"full purify clears hunger causal debt");other.getFoodData().setFoodLevel(17);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            var friend=cow(at.add(80,0,0));friend.setHealth(99);game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);var protectedMob=cow(at.add(70,0,0));protectedMob.entityTags().add("strict_map_object");game.server().getScoreboard().addPlayerToTeam(protectedMob.getScoreboardName(),team);var enemy=cow(at.add(60,0,0));
            require(use(mode(2),InteractionHand.MAIN_HAND)&&!friend.isAlive()&&protectedMob.isAlive()&&enemy.isAlive()&&other.isAlive(),"reverse globally sacrifices friendly loaded mobs and protects players/technical/enemies");
            var resistance=owner.getEffect(MobEffects.RESISTANCE);require(resistance!=null&&resistance.getAmplifier()==4&&resistance.getDuration()==180,"sum floor friendly HP/10 grants9 seconds ResistanceV");remove(friend);remove(protectedMob);remove(enemy);owner.removeAllEffects();clearActors();
        }
        void actors(){
            var service=game.items().acidCorruption();var wall=BlockPos.containing(owner.getEyePosition().add(0,0,4));block(wall,Blocks.STONE);
            require(use(loot("acid_corruption_wand"),InteractionHand.MAIN_HAND)&&service.snapshot().stream().anyMatch(c->c.ball&&c.getOwner()==owner),"actual first wall creates owned native corruption ball");clearActors();block(wall,Blocks.AIR);
            var victim=cow(owner.getEyePosition().add(0,-.6,3));require(use(loot("acid_corruption_wand"),InteractionHand.MAIN_HAND)&&service.snapshot().stream().anyMatch(c->c.ball&&c.getOwner()==owner),"actual first entity creates native ball");clearActors();remove(victim);
            var center=at.add(0,5,12);var ball=service.spawn(owner,center,true);require(ball.getOwner()==owner&&ball.ball,"native ball author");owner.level().getRandom().setSeed(1924);
            for(int i=0;i<64&&service.snapshot().stream().noneMatch(c->!c.ball);i++)service.flight(ball);
            var drop=service.snapshot().stream().filter(c->!c.ball).findFirst().orElseThrow();require(drop.getOwner()==owner,"native emitted falling drop inherits caster");
            drop.setPos(center);drop.setDeltaMovement(new Vec3(0,-1,0));victim=cow(center.add(0,-1,0));victim.setInvulnerableTime(0);service.flight(drop);require(victim.getHealth()<1000&&victim.getKillCredit()==owner,"native falling drop cactus impact credits caster");remove(victim);
            var area=cow(center.add(5,0,0));var outside=cow(center.add(7,0,0));var role=cow(center.add(-3,0,0));role.entityTags().add("strict_map_object");
            for(int i=0;i<64&&area.getActiveEffects().isEmpty();i++)service.flight(ball);
            require(!area.getActiveEffects().isEmpty()&&outside.getActiveEffects().isEmpty()&&role.getActiveEffects().isEmpty(),"ball35-percent pulses reach radius6 and protect outside/technical targets");remove(area);remove(outside);remove(role);
            ball.age=399;service.flight(ball);require(ball.isRemoved(),"ball boundary expires at400 active ticks");clearActors();var falling=service.spawn(owner,center,false);falling.age=299;service.flight(falling);require(falling.isRemoved(),"falling drop boundary expires at300 active ticks");
            var erode=BlockPos.containing(center.add(3,0,0));for(int x=-1;x<=1;x++)for(int y=-1;y<=2;y++)for(int z=-1;z<=1;z++)block(erode.offset(x,y,z),Blocks.AIR);block(erode,Blocks.STONE);
            falling=service.spawn(owner,Vec3.atCenterOf(erode).add(0,1,0),false);falling.setDeltaMovement(new Vec3(0,-2,0));service.flight(falling);require(falling.isRemoved()&&owner.level().getBlockState(erode).isAir(),"falling drop terminates once at wall and erodes source .3 cube");
            var instant=cow(at.add(10,0,0));instant.setHealth(3);instant.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE,1,0),owner);instant.tick();require(instant.isDeadOrDying()&&instant.getKillCredit()==owner,"actual one-tick native instant damage inherits caster");remove(instant);
            instant=cow(at.add(11,0,0));instant.setHealth(4);instant.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE,1,0),owner);require(instant.hurtServer(owner.level(),owner.damageSources().playerAttack(other),1),"later real hit before native instant damage");instant.tick();require(instant.isDeadOrDying()&&instant.getKillCredit()==other,"later accepted player hit overrides one-tick native effect author");remove(instant);
            var technical=cow(at.add(4,0,0));technical.entityTags().add("strict_map_object");service.applyDeath(owner,technical,120);require(service.deathRemaining(technical)==0,"technical roles reject death status");remove(technical);
            var marked=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());marked.setPos(at);marked.entityTags().add("stopper_magic");owner.level().addFreshEntity(marked);game.items().magic().loaded(marked);
            try{int mana=game.items().mana().get(owner);require(!use(loot("corruption_wand"),InteractionHand.MAIN_HAND)&&!use(loot("acid_corruption_wand"),InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana,"real antimagic blocks both families without cost");}finally{game.items().magic().unloaded(marked);marked.discard();}
            frozen=service.spawn(owner,center,true);frozenDeath=cow(at.add(8,0,0));service.applyDeath(owner,frozenDeath,1);game.items().timeStop().begin(owner.level(),200);waits=0;
        }
        boolean step(){long now=owner.level().getGameTime();if(last==now)return false;last=now;
            if(phase==0){presentation();donor();phase++;return false;}
            if(phase==1){donorTrial();phase++;return false;}
            if(phase==2){owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_CORRUPTION)&&Identifier.parse("lbc:tier_c").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"actual native C donor promotion");phase++;return false;}
            if(phase==3){acidModes();actors();phase++;return false;}
            if(phase==4){require(frozen.age==0&&game.items().acidCorruption().deathRemaining(frozenDeath)==1&&frozenDeath.isAlive(),"actual Time Stop freezes native ball and death countdown");if(++waits<3)return false;game.items().timeStop().end(owner.level());clearActors();doomed=cow(at.add(6,0,0));later=cow(at.add(7,0,0));game.items().acidCorruption().applyDeath(owner,doomed,1);game.items().acidCorruption().applyDeath(owner,later,1);require(later.hurtServer(owner.level(),owner.damageSources().playerAttack(other),1),"later real second-player hit accepted");waits=0;phase++;return false;}
            if(phase==5){if(doomed.isAlive()||later.isAlive()||frozenDeath.isAlive()){require(++waits<25,"one-second natural delayed deaths complete");return false;}require(doomed.getKillCredit()==owner&&later.getKillCredit()==other&&frozenDeath.getKillCredit()==owner,"native delayed death credits caster, later real hit takes precedence, paused curse resumes");remove(doomed);remove(later);remove(frozenDeath);trial=loot("acid_corruption_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,trial);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(trial),"actual continuation Academy admission");phase++;return false;}
            if(phase==6){owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setShiftKeyDown(true);require(use(trial,InteractionHand.OFF_HAND),"actual offhand meditation");owner.setShiftKeyDown(false);var resistance=owner.getEffect(MobEffects.RESISTANCE);require(resistance!=null&&resistance.getAmplifier()==4&&resistance.getDuration()==600&&flag(trial,"upgrade_available")==1,"meditation immediately opens used-hand trial with ResistanceV600");require(owner.getActiveEffects().stream().filter(e->e.getDuration()==MobEffectInstance.INFINITE_DURATION).count()==9&&owner.hasEffect(MobEffects.INSTANT_DAMAGE),"all nine infinite native meditation negatives including InstantDamageC");game.items().clearEffects(owner);require(owner.getActiveEffects().isEmpty(),"full native meditation purify");owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);phase++;return false;}
            owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_ACID_CORRUPTION)&&Identifier.parse("lbc:tier_ss").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"actual native SS continuation promotion");return true;
        }
        public void close(){
            game.items().timeStop().end(owner.level());clearActors();game.items().acidCorruption().clear(owner);game.items().acidCorruption().clear(other);var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});for(var e:fresh){if(e instanceof LivingEntity living){game.items().acidCorruption().clear(living);game.items().indirectKills().clear(living);}e.discard();}
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));otherStats.forEach((s,v)->other.getStats().setValue(other,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            owner.getStats().setValue(owner,cowStat,ownerCowKills);other.getStats().setValue(other,cowStat,otherCowKills);
            ownerProfile.restore();otherProfile.restore();game.players().save();
            for(var entry:advancements.entrySet()){var advancement=game.server().getAdvancements().get(entry.getKey());for(String criterion:advancement.value().criteria().keySet())if(!entry.getValue().contains(criterion))owner.getAdvancements().revoke(advancement,criterion);}
            game.items().mana().score(owner,"delayed_death").set(ownerDeath);game.items().mana().score(other,"delayed_death").set(otherDeath);game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);for(long k:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(k),net.minecraft.world.level.ChunkPos.getZ(k),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.warm())return;if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS corruption");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL corruption",e);}}
    private static GameChecks.WandThirdPlayerState stopSaved;private static String epoch;private static Cow stopTarget;private static List<LbcAcidCorruption.Charge> stopActors=List.of();private static net.minecraft.world.entity.Display.ItemDisplay stopGlyph;
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null,"exclusive stop probe");stopSaved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);game.items().acidCorruption().spawn(owner,owner.getEyePosition().add(0,5,0),true);game.items().acidCorruption().spawn(owner,owner.getEyePosition().add(0,7,0),false);stopTarget=new Cow(EntityTypes.COW,owner.level());stopTarget.setPos(owner.position().add(3,0,0));stopTarget.setNoAi(true);stopTarget.setNoGravity(true);owner.level().addFreshEntity(stopTarget);game.items().acidCorruption().applyDeath(owner,stopTarget,120);stopActors=List.copyOf(game.items().acidCorruption().snapshot());require(stopActors.size()>=2&&game.items().acidCorruption().deathRemaining(stopTarget)==120,"live native ball/drop/death status before match stop");var glyph=game.items().corruptedGlyphs().spawn(owner,owner.getEyePosition().add(0,9,0),LbcCorruptedGlyphs.Kind.BELIAL);require(glyph!=null,"live glyph before match stop");stopGlyph=glyph.display;epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS corruption-stop-probe");}
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(epoch)&&game.items().acidCorruption().snapshot().isEmpty()&&stopActors.stream().allMatch(Entity::isRemoved)&&game.items().acidCorruption().deathRemaining(stopTarget)==0&&game.items().corruptedGlyphs().count()==0&&stopGlyph!=null&&stopGlyph.isRemoved(),"actual match stop clears native balls/drops and delayed curse");}finally{if(stopTarget!=null)stopTarget.discard();stopTarget=null;stopGlyph=null;stopActors=List.of();if(stopSaved!=null)stopSaved.restore(game);stopSaved=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS corruption-cleanup");}
}
