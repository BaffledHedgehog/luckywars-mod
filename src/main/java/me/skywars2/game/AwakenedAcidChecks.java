package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual native menus/casts, shared acid payloads, rifle counter and lifecycle boundaries. */
public final class AwakenedAcidChecks {
    private AwakenedAcidChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Awakened acid: "+why);}
    private static Fixture running;
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Long> forced=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Map<Identifier,Set<String>> advancements=new LinkedHashMap<>();
        final Set<String> unlocks;final boolean offhand;final int skill,trinket,challengeProgress,rifle;
        final net.minecraft.world.scores.PlayerTeam team;
        boolean warmed;int warmTicks,phase;long last=Long.MIN_VALUE;
        ItemStack trial;net.minecraft.world.entity.Interaction panel;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;
            var p=game.players().get(owner.getUUID());skill=p.lbcSkill;trinket=p.lbcTrinket;challengeProgress=p.lbcChallengeProgress;unlocks=new HashSet<>(p.lbcUnlocks);rifle=game.items().mana().hudValue(owner,"acid_rifle");
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(s,owner.getStats().getValue(s));}
            for(String key:List.of("lbc:true_advancements/legends/tuntija_wands/acidacid","lbc:true_advancements/legends/academy/acidacid")){
                var id=Identifier.parse(key);var advancement=game.server().getAdvancements().get(id);if(advancement!=null){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(id,done);}
            }
            owner.level().getAllEntities().forEach(before::add);team=game.server().getScoreboard().addPlayerTeam("awakened_acid_fixture");
        }
        void init(){
            for(int x=-16;x<=-14;x++)for(int z=-16;z<=-14;z++){long k=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(k)){forced.add(k);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);owner.setShiftKeyDown(false);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;game.config().offhandWands=true;
            game.items().awakenedAcid().clear(owner);game.items().mana().set(owner,100000);game.items().mana().noMagic(owner,0);
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);owner.getFoodData().setFoodLevel(17);
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
        }
        boolean warm(){if(warmed)return true;require(++warmTicks<=600,"resident entity ticking fixture");for(long k:forced)if(!owner.level().areEntitiesLoaded(k))return false;warmed=true;return true;}
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one item "+leaf);return list.getFirst();}
        int flag(ItemStack s,String key){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
        void flag(ItemStack s,String key,int value){var d=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt(key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack s,InteractionHand hand){owner.setItemInHand(hand,s);return owner.gameMode.useItem(owner,owner.level(),s,hand).consumesAction();}
        List<LbcClouds.AcidProjectile> shots(){return game.items().clouds().acidSnapshot().stream().filter(p->p.getOwner()==owner&&!p.isRemoved()).toList();}
        void clearAcid(){shots().forEach(Entity::discard);}
        static final class HitCow extends Cow {
            int fireCalls;
            HitCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){if(source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE))fireCalls++;return super.hurtServer(level,source,amount);}
        }
        HitCow cow(Vec3 p){var c=new HitCow(owner.level());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void reset(Cow c){c.removeAllEffects();c.clearFire();c.setHealth(1000);c.setInvulnerableTime(0);c.damageCooldownTime=0;}
        void block(BlockPos p,net.minecraft.world.level.block.Block b){blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,b.defaultBlockState(),2);}
        void effect(Cow c,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type,int ticks,int level){require(c.hasEffect(type)&&c.getEffect(type).getDuration()==ticks&&c.getEffect(type).getAmplifier()==level,"exact "+type.getRegisteredName());}
        void craft(boolean workbench){
            owner.closeContainer();owner.getInventory().clearContent();var pos=owner.blockPosition().east();
            if(workbench){block(pos,Blocks.CRAFTING_TABLE);owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(owner.level(),pos)),net.minecraft.network.chat.Component.literal("test")));}
            var menu=(net.minecraft.world.inventory.AbstractCraftingMenu)owner.containerMenu;var slots=menu.getInputGridSlots();
            var basic=new ItemStack(LbcWands.ACID_WAND,2);var caust=loot(workbench?"academy_caustic_wand":"caustic_wand").copyWithCount(2);
            slots.getFirst().setByPlayer(basic);slots.getLast().setByPlayer(caust);
            require(menu.getResultSlot().getItem().is(LbcWands.AWAKENED_ACID),"native "+(workbench?"3x3":"2x2")+" preview");
            slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra slot rejects");slots.get(1).setByPlayer(ItemStack.EMPTY);
            for(int i=0;i<36;i++)owner.getInventory().setItem(i,new ItemStack(Items.STICK,64));GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
            require(slots.getFirst().getItem().getCount()==2&&slots.getLast().getItem().getCount()==2,"full inventory keeps donors");
            GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
            require(menu.getCarried().is(LbcWands.AWAKENED_ACID)&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"one pair consumed for one item");
            menu.setCarried(ItemStack.EMPTY);slots.forEach(s->s.setByPlayer(ItemStack.EMPTY));owner.getInventory().clearContent();owner.closeContainer();
        }
        void presentation(){
            for(boolean upgraded:new boolean[]{false,true}){var s=loot(upgraded?"academy_awakened_acid_wand":"awakened_acid_wand");
                require(s.is(upgraded?LbcWands.ACADEMY_AWAKENED_ACID:LbcWands.AWAKENED_ACID)&&flag(s,"acidacid_wand")==1&&flag(s,"staff")==1&&flag(s,"transformable")==1,"native source flags");
                require(Identifier.parse("lbc:acidacidwand").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(upgraded?"lbc:tier_ss":"lbc:tier_s").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(upgraded?5:4),"S/SS original model/lore");
                require(flag(s,"tier")==0&&flag(s,"upgraded")== (upgraded?1:0),"rarity never implies ignition");
                require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("acidacid_wand")&&!s.getHoverName().getStyle().isItalic(),"source name/style");
            }
            craft(false);craft(true);game.items().mana().set(owner,1449);require(!game.items().awakenedAcid().use(owner,InteractionHand.MAIN_HAND,loot("awakened_acid_wand"),false)&&game.items().mana().get(owner)==1449,"insufficient mana unchanged");
            game.items().mana().set(owner,100000);var a=loot("awakened_acid_wand");var b=a.copy();owner.setItemInHand(InteractionHand.MAIN_HAND,a);owner.setItemInHand(InteractionHand.OFF_HAND,b);
            require(owner.gameMode.useItem(owner,owner.level(),a,InteractionHand.MAIN_HAND).consumesAction()&&shots().size()==2&&game.items().mana().get(owner)==97100,"actual dual hand cost2900 and two seeds");
            for(var p:shots())require(LbcClouds.acidLifetime(p)==500&&Math.abs(p.getDeltaMovement().length()-.2)<1e-6&&!p.entityTags().contains("on_fire")&&!p.entityTags().contains("shield_blockable2"),"source seed physics/lifetime/shield");
            clearAcid();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var s=loot("academy_awakened_acid_wand");game.items().mana().set(owner,-10000);owner.setShiftKeyDown(true);owner.setXRot(0);
            for(int mode:new int[]{2,3,4,1})require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"mode")==mode&&game.items().mana().get(owner)==-10000,"free negative-mana mode cycle");
            owner.setXRot(90);require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"tier")==1,"missing ignition -> on");require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"tier")==2,"ignition -> off");
            owner.setShiftKeyDown(false);owner.setXRot(0);game.items().mana().set(owner,100000);
        }
        void payloads(){
            var center=at.add(0,3,8);var victim=cow(center);var friend=cow(center.add(1,0,0));var technical=cow(center.add(-1,0,0));technical.entityTags().add("strict_map_object");game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
            other.teleportTo(other.level(),center.x,center.y,center.z,Set.of(),0,0,true);other.removeAllEffects();
            var s=loot("academy_awakened_acid_wand");flag(s,"mode",2);require(use(s,InteractionHand.OFF_HAND),"native caust cast");var p=shots().getFirst();p.setPos(center);p.setDeltaMovement(Vec3.ZERO);
            require(LbcClouds.awakenedVariant(p)==1&&LbcClouds.acidLifetime(p)==500,"caust shared lifetime");game.items().clouds().tick();effect(victim,MobEffects.POISON,2400,4);effect(friend,MobEffects.POISON,2400,4);
            require(!technical.hasEffect(MobEffects.POISON)&&!other.hasEffect(MobEffects.POISON),"allied mobs hit, allied player and technical protected");
            MobEffects.POISON.value().applyEffectTick(owner.level(),victim,10);require(victim.getKillCredit()==owner,"native poison owner");
            reset(victim);p.entityTags().add("on_fire");game.items().clouds().tick();require(victim.getHealth()==985&&victim.getKillCredit()==owner,"caust15 ON_FIRE damage");int age=game.items().clouds().acidAge(p);game.items().clouds().tick();require(victim.getHealth()==985&&game.items().clouds().acidAge(p)==age+1,"native fire hurt cooldown retained and caust age one per tick");
            var wall=BlockPos.containing(center.add(0,0,4));block(wall,Blocks.STONE);p.setPos(wall.getX()+.5,wall.getY()+.2,wall.getZ()-.1);p.setDeltaMovement(0,0,.2);p.tick();var stuck=p.position();require(p.entityTags().contains("skywars2.acid_stuck")&&p.getDeltaMovement().equals(Vec3.ZERO),"native first wall sticks without restored velocity");p.tick();require(p.position().equals(stuck),"sticky charge stationary");clearAcid();
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);friend.discard();technical.discard();reset(victim);
            flag(s,"mode",3);require(use(s,InteractionHand.MAIN_HAND)&&shots().size()==8,"native eight-charge Gatling");var burst=shots().getFirst();shots().stream().filter(q->q!=burst).forEach(Entity::discard);
            require(LbcClouds.awakenedVariant(burst)==2&&LbcClouds.acidLifetime(burst)==200&&Math.abs(burst.getDeltaMovement().length()-3)<1e-6,"burst speed3/lifetime200");
            burst.setPos(center.add(0,0,-1.5));burst.setDeltaMovement(0,0,3);burst.tick();game.items().clouds().tick();effect(victim,MobEffects.POISON,1000,4);effect(victim,MobEffects.WITHER,200,5);
            MobEffects.WITHER.value().applyEffectTick(owner.level(),victim,10);require(victim.getKillCredit()==owner,"native wither cause");
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.hurtServer(owner.level(),owner.level().damageSources().playerAttack(other),1);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;MobEffects.WITHER.value().applyEffectTick(owner.level(),victim,10);require(victim.getKillCredit()==other,"later real player hit wins old wither");
            reset(victim);victim.fireCalls=0;burst.setPos(center.add(0,0,-1.5));burst.setDeltaMovement(0,0,3);burst.entityTags().add("on_fire");burst.tick();game.items().clouds().tick();require(victim.getHealth()==975&&victim.fireCalls==1,"one25 fire call per target per tick");game.items().clouds().tick();require(victim.getHealth()==975&&victim.fireCalls==2,"exactly one call per tick and normal fire cooldown");
            clearAcid();victim.discard();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        }
        void rifle(){
            var service=game.items().awakenedAcid();var s=loot("academy_awakened_acid_wand");flag(s,"mode",4);flag(s,"tier",1);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().set(owner,100000);
            require(use(s,InteractionHand.MAIN_HAND)&&use(s,InteractionHand.MAIN_HAND)&&service.pending(owner)==20&&game.items().mana().get(owner)==97100,"two casts accumulate20 releases for2900 mana");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setYRot(90);for(int i=0;i<5;i++)service.tick();require(service.pending(owner)==19&&shots().size()==1,"one release per5 ticks after unequip");
            var p=shots().getFirst();require(p.entityTags().contains("on_fire")&&p.getDeltaMovement().normalize().dot(owner.getLookAngle())>.999,"last used stack ignition and current aim");
            require(game.items().hud(owner).getString().contains("\uE109 19"),"raw queued shots in shared actionbar");clearAcid();
            var mark=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());mark.setPos(owner.position());mark.entityTags().add("stopper_magic");owner.level().addFreshEntity(mark);game.items().magic().loaded(mark);
            try{for(int i=0;i<10;i++)service.tick();require(service.pending(owner)==19&&shots().isEmpty(),"antimagic pauses release without losing queue");require(!use(s,InteractionHand.MAIN_HAND),"antimagic blocks activation");}finally{game.items().magic().unloaded(mark);mark.discard();}
            game.items().timeStop().begin(owner.level(),200);for(int i=0;i<10;i++)service.tick();require(service.pending(owner)==19&&shots().isEmpty(),"Time Stop pauses queue");game.items().timeStop().end(owner.level());
            for(int i=0;i<5;i++)service.tick();require(service.pending(owner)==18&&shots().size()==1,"release resumes");clearAcid();game.items().clear(owner);require(service.pending(owner)==0,"full purify clears queue");
            service.addPlayerCounter(owner);require(service.pending(owner)==1,"KFC +1 registers live source counter");for(int i=0;i<5;i++)service.tick();require(service.pending(owner)==0&&shots().size()==1,"counter-only KFC shot emitted");clearAcid();
            require(use(s,InteractionHand.MAIN_HAND),"fresh rifle before forget");game.items().forget(owner);require(service.pending(owner)==0,"death/disconnect common forget clears queue");owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setYRot(0);
        }
        boolean step(){long now=owner.level().getGameTime();if(last==now)return false;last=now;
            if(phase==0){presentation();payloads();rifle();clearAcid();game.items().mana().set(owner,100000);panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(at);panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);trial=loot("awakened_acid_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(trial),"actual Academy admission");phase++;return false;}
            if(phase==1){owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setRemainingFireTicks(100);require(use(trial,InteractionHand.OFF_HAND)&&flag(trial,"upgrade_available")==1,"burning actual offhand cast qualifies used stack");owner.clearFire();clearAcid();phase++;return false;}
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,trial);
            require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_AWAKENED_ACID)&&Identifier.parse("lbc:tier_ss").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real Academy promotion SS");return true;
        }
        public void close(){
            game.items().timeStop().end(owner.level());game.items().awakenedAcid().clear(owner);var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));game.items().mana().score(owner,"acid_rifle").set(rifle);
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var entry:advancements.entrySet()){var advancement=game.server().getAdvancements().get(entry.getKey());for(String criterion:advancement.value().criteria().keySet())if(!entry.getValue().contains(criterion))owner.getAdvancements().revoke(advancement,criterion);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);for(long k:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(k),net.minecraft.world.level.ChunkPos.getZ(k),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.warm())return;if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS awakened-acid");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL awakened-acid",e);}}
    private static GameChecks.WandThirdPlayerState stopSaved;private static String epoch;private static List<LbcClouds.AcidProjectile> stopped=List.of();
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null,"exclusive stop probe");stopSaved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);var s=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_awakened_acid_wand").getFirst();var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("mode",4);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().awakenedAcid().pending(owner)==10,"queued rifle before actual match stop");game.items().clouds().awakenedAcid(owner,owner.getEyePosition(),owner.getLookAngle(),1,false);stopped=game.items().clouds().acidSnapshot().stream().filter(p->p.getOwner()==owner&&!p.isRemoved()).toList();epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS awakened-acid-stop-probe");}
    public static void cleanup(GameController game){try{var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved!=null&&!game.items().currentEpoch(epoch)&&game.items().awakenedAcid().pending(owner)==0&&stopped.stream().allMatch(Entity::isRemoved),"actual match stop clears queue and native actors");}finally{if(stopSaved!=null)stopSaved.restore(game);stopSaved=null;epoch=null;stopped=List.of();}SkyWars2.LOGGER.info("[check] PASS awakened-acid-cleanup");}
}
