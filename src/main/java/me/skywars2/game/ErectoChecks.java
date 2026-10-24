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

/** Native higher-electricity craft/casts, source payloads, real timers, Academy and lifecycle. */
public final class ErectoChecks {
    private ErectoChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Erecto: "+why);}
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
        final Set<String> unlocks;final boolean offhand;final int skill,trinket,challengeProgress;
        final net.minecraft.world.scores.PlayerTeam team;
        boolean warmed;int warmTicks,phase;long last=Long.MIN_VALUE;
        ItemStack trial;net.minecraft.world.entity.Interaction panel;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;
            var p=game.players().get(owner.getUUID());skill=p.lbcSkill;trinket=p.lbcTrinket;challengeProgress=p.lbcChallengeProgress;unlocks=new HashSet<>(p.lbcUnlocks);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(s,owner.getStats().getValue(s));}
            for(String key:List.of("lbc:true_advancements/legends/tuntija_wands/erecto","lbc:true_advancements/legends/academy/erecto")){
                var id=Identifier.parse(key);var advancement=game.server().getAdvancements().get(id);if(advancement!=null){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(id,done);}
            }
            owner.level().getAllEntities().forEach(before::add);team=game.server().getScoreboard().addPlayerTeam("erecto_fixture");team.setAllowFriendlyFire(false);
        }
        void init(){
            for(int x=-16;x<=-14;x++)for(int z=-16;z<=-8;z++){long k=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(k)){forced.add(k);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);owner.setShiftKeyDown(false);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;game.config().offhandWands=true;
            game.items().mana().set(owner,100000);game.items().mana().noMagic(owner,0);
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);owner.getFoodData().setFoodLevel(17);
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
        }
        boolean warm(){if(warmed)return true;require(++warmTicks<=600,"resident entity ticking fixture");for(long k:forced)if(!owner.level().areEntitiesLoaded(k))return false;warmed=true;return true;}
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one item "+leaf);return list.getFirst();}
        int flag(ItemStack s,String key){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
        void flag(ItemStack s,String key,int value){var d=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt(key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack s,InteractionHand hand){owner.setItemInHand(hand,s);return owner.gameMode.useItem(owner,owner.level(),s,hand).consumesAction();}


        List<LbcWands.ElectroTrace> traces(){return game.items().wands().electroSnapshot().stream().filter(t->t.erecto!=null&&t.erecto.caster==owner&&t.age<t.lifetime).toList();}
        void clearTraces(){for(var t:traces())t.age=t.lifetime;game.items().wands().tick();}
        static final class ProbeCow extends Cow {
            int direct;boolean reject;ProbeCow(net.minecraft.server.level.ServerLevel l){super(EntityTypes.COW,l);}
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel l,net.minecraft.world.damagesource.DamageSource s,float n){if(s.is(LbcDamage.ELECTRO_BOLT)){direct++;if(reject)return false;}return super.hurtServer(l,s,n);}
        }
        ProbeCow cow(Vec3 p){var c=new ProbeCow(owner.level());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void block(BlockPos p,net.minecraft.world.level.block.Block type){blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,type.defaultBlockState(),2);}
        void craft(boolean workbench){
            owner.closeContainer();owner.getInventory().clearContent();var pos=owner.blockPosition().east();if(workbench){block(pos,Blocks.CRAFTING_TABLE);owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(owner.level(),pos)),net.minecraft.network.chat.Component.literal("test")));}
            var menu=(net.minecraft.world.inventory.AbstractCraftingMenu)owner.containerMenu;var slots=menu.getInputGridSlots();var first=new ItemStack(workbench?LbcWands.ACADEMY_ELECTRO:LbcWands.ELECTRO,2);var second=loot(workbench?"academy_tesla_wand":"tesla_wand").copyWithCount(2);slots.getFirst().setByPlayer(first);slots.getLast().setByPlayer(second);
            require(menu.getResultSlot().getItem().is(LbcWands.ERECTO),"native "+(workbench?"3x3":"2x2")+" output");slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra ingredient rejected");slots.get(1).setByPlayer(ItemStack.EMPTY);
            for(int i=0;i<36;i++)owner.getInventory().setItem(i,new ItemStack(Items.STICK,64));GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);require(slots.getFirst().getItem().getCount()==2&&slots.getLast().getItem().getCount()==2,"full inventory retains donors");
            GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);require(menu.getCarried().is(LbcWands.ERECTO)&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"one result consumes exactly one of each donor");menu.setCarried(ItemStack.EMPTY);slots.forEach(s->s.setByPlayer(ItemStack.EMPTY));owner.closeContainer();owner.getInventory().clearContent();
        }
        void presentation(){
            game.items().erecto().forget(owner);
            for(boolean academy:new boolean[]{false,true}){var s=loot(academy?"academy_erecto_wand":"erecto_wand");require(s.is(academy?LbcWands.ACADEMY_ERECTO:LbcWands.ERECTO)&&flag(s,"erecto_wand")==1&&flag(s,"magicwand")==1,"native source identity");require(Identifier.parse("lbc:erecto").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(academy?"lbc:tier_ss":"lbc:tier_s").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(academy?4:3),"S/SS source presentation");require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("erecto_wand")&&!s.getHoverName().getStyle().isItalic(),"original source name/style");}
            craft(false);craft(true);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().set(owner,1099);require(!game.items().erecto().use(owner,InteractionHand.MAIN_HAND,loot("erecto_wand"),false)&&game.items().mana().get(owner)==1099,"strict ordinary admission1099 fails");
            var s=loot("erecto_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,s);owner.setItemInHand(InteractionHand.OFF_HAND,s.copy());game.items().mana().set(owner,100000);require(owner.gameMode.useItem(owner,owner.level(),s,InteractionHand.MAIN_HAND).consumesAction()&&traces().size()==18&&game.items().mana().get(owner)==97800,"native dual hands: two9-arc volleys for2200");require(traces().stream().filter(t->t.erecto.kind==LbcErecto.SIDE).count()==16&&traces().stream().allMatch(t->t.lifetime==200),"eight side arcs per volley, source lifetime200");clearTraces();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            s=loot("academy_erecto_wand");game.items().mana().set(owner,-10000);owner.setShiftKeyDown(true);for(int mode:new int[]{2,3,4,1})require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"mode")==mode&&game.items().mana().get(owner)==-10000,"free negative-mana mode cycling");owner.setShiftKeyDown(false);game.items().mana().set(owner,100000);
        }
        void geometry(){
            var point=at.add(0,4,12);var friend=cow(point.add(0,-.6,1));friend.getAttribute(Attributes.SCALE).setBaseValue(2);friend.refreshDimensions();game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);other.setHealth(1000);other.setAbsorptionAmount(0);other.teleportTo(other.level(),point.x,point.y-.6,point.z+1,Set.of(),0,0,true);
            require(use(loot("erecto_wand"),InteractionHand.OFF_HAND),"actual offhand ordinary cast");var center=traces().stream().filter(t->t.erecto.kind==LbcErecto.CENTRAL).findFirst().orElseThrow();for(var t:traces())if(t!=center)t.age=t.lifetime;center.position=point;center.yaw=0;center.pitch=0;game.items().wands().tick();require(Math.abs(center.position.distanceTo(point)-2)<1e-6&&friend.getHealth()<1000&&friend.direct>=2&&friend.getKillCredit()==owner&&other.getHealth()==1000,"central moves2 each tick, repeated native hits credit owner; friendly mob hit, allied player protected: movement="+center.position.distanceTo(point)+" hp="+friend.getHealth()+" calls="+friend.direct+" credit="+(friend.getKillCredit()==owner)+" allyHp="+other.getHealth());
            game.items().wands().reflectElectro(other);require(center.owner==other,"higher electric arc reflection changes owner");center.owner=owner;clearTraces();friend.discard();
            var iron=BlockPos.containing(point.add(0,0,10));block(iron,Blocks.IRON_BLOCK);var victim=cow(Vec3.atLowerCornerOf(iron).add(4,0,0));game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);var technical=cow(Vec3.atLowerCornerOf(iron).add(-4,0,0));technical.entityTags().add("strict_map_object");other.teleportTo(other.level(),iron.getX()+3,iron.getY(),iron.getZ(),Set.of(),0,0,true);other.setHealth(1000);
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(use(loot("erecto_wand"),InteractionHand.MAIN_HAND),"conductor cast");center=traces().stream().filter(t->t.erecto.kind==LbcErecto.CENTRAL).findFirst().orElseThrow();for(var t:traces())if(t!=center)t.age=t.lifetime;center.position=Vec3.atCenterOf(iron);center.yaw=0;center.pitch=0;game.items().wands().tick();require(victim.getHealth()==992&&victim.getKillCredit()==owner&&other.getHealth()==1000&&technical.getHealth()==1000&&!traces().contains(center),"first conductive wall8 damage radius15, owner credit and role/player protection");clearTraces();victim.discard();technical.discard();other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);
            var focus=BlockPos.containing(owner.getEyePosition().add(0,0,20));block(focus,Blocks.STONE);owner.setYRot(0);require(use(loot("erecto_wand"),InteractionHand.MAIN_HAND),"focused volley");var side=traces().stream().filter(t->t.erecto.side==1).findFirst().orElseThrow();var original=side.erecto.focus;require(original!=null,"focus resolved at first actual wall");owner.setYRot(90);require(use(loot("erecto_wand"),InteractionHand.MAIN_HAND)&&side.erecto.focus.equals(original),"another cast cannot replace first cast focus");for(var t:traces())if(t!=side)t.age=t.lifetime;var start=side.position;float yaw=side.yaw;game.items().wands().tick();require(Math.abs(side.position.distanceTo(start)-2.25)<1e-6&&side.yaw<yaw,"side arc advances2.25 and bends by its source distance");side.age=side.erecto.focusSteps/7;game.items().wands().tick();require(side.erecto.side==0,"side switches once toward its own focus");clearTraces();block(focus,Blocks.AIR);owner.setYRot(0);
        }
        void ray(){
            var s=loot("academy_erecto_wand");flag(s,"mode",2);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setYRot(0);owner.setXRot(0);var target=cow(owner.getEyePosition().add(0,-.6,3));game.items().mana().set(owner,100000);
            require(use(s,InteractionHand.MAIN_HAND)&&use(s,InteractionHand.MAIN_HAND)&&game.items().erecto().rayPending(owner)==80&&game.items().mana().get(owner)==97800,"ray accumulates40 releases per1100-mana cast");owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);require(game.items().hud(owner).getString().contains("\uE117 80"),"source raw ray count in actionbar");game.items().erecto().tick();require(game.items().erecto().rayPending(owner)==79&&target.direct>0&&target.getKillCredit()==owner&&target.hasEffect(MobEffects.GLOWING)&&target.getEffect(MobEffects.SLOWNESS).getAmplifier()==100,"ray emits after unequip and applies owned damage/glowing/slow");
            target.removeAllEffects();target.reject=true;float hp=target.getHealth();game.items().erecto().tick();require(target.getHealth()==hp&&target.hasEffect(MobEffects.GLOWING)&&target.hasEffect(MobEffects.SLOWNESS),"source ray statuses apply even when native damage is rejected");target.reject=false;
            var mark=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());mark.setPos(at);mark.entityTags().add("stopper_magic");owner.level().addFreshEntity(mark);game.items().magic().loaded(mark);try{require(!use(s,InteractionHand.MAIN_HAND),"real antimagic blocks wand activation");game.items().erecto().tick();require(game.items().erecto().rayPending(owner)==77,"already cast electricity continues inside antimagic");}finally{game.items().magic().unloaded(mark);mark.discard();}
            game.items().timeStop().begin(owner.level(),200);game.items().erecto().tick();require(game.items().erecto().rayPending(owner)==77,"Time Stop pauses existing ray releases");game.items().timeStop().end(owner.level());game.items().clear(owner);require(game.items().erecto().rayPending(owner)==0,"full purify clears source ray counter");game.items().erecto().addPlayerCounter(owner);game.items().erecto().tick();require(game.items().erecto().rayPending(owner)==0&&target.direct>1,"KFC +1 bridges one live ray");target.discard();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        }
        void sniper(){
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setXRot(0);owner.setYRot(0);var s=loot("academy_erecto_wand");flag(s,"mode",4);game.items().mana().set(owner,1100);double movement=owner.getAttributeValue(Attributes.MOVEMENT_SPEED);require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==-3900,"source1100 admission/5000 actual cost");var shot=traces().getFirst();require(shot.erecto.kind==LbcErecto.SNIPER&&shot.pitch==0&&owner.getXRot()==-15&&game.items().erecto().sniperWindupTicks(owner)==20&&owner.getAttributeValue(Attributes.MOVEMENT_SPEED)==0&&!owner.hasEffect(MobEffects.SLOWNESS),"sniper heading precedes native recoil and safe20tick zero movement without negative speed");
            CombatVisualChecks.movementPacket(owner,0);for(int i=0;i<19;i++)game.items().erecto().tick();require(owner.getAttributeValue(Attributes.MOVEMENT_SPEED)==0&&game.items().erecto().sniperWindupTicks(owner)==1,"safe sniper windup lasts19 active boundaries");game.items().erecto().tick();require(game.items().erecto().sniperWindupTicks(owner)==0&&Math.abs(owner.getAttributeValue(Attributes.MOVEMENT_SPEED)-movement)<1e-9,"20th active boundary restores prior movement attribute");CombatVisualChecks.movementPacket(owner,movement);for(var t:traces())if(t!=shot)t.age=t.lifetime;
            var start=at.add(0,5,35);var victim=cow(start.add(0,-.6,2));shot.position=start;shot.yaw=0;shot.pitch=0;game.items().wands().tick();require(Math.abs(shot.position.distanceTo(start)-18)<1e-6&&victim.getHealth()<1000&&victim.getKillCredit()==owner,"sniper advances18 each tick and owns repeated20 hits");victim.discard();clearTraces();
            boolean stopped=false,continued=false;var wall=BlockPos.containing(start);
            for(int seed=0;seed<64&&!(stopped&&continued);seed++){
                block(wall,Blocks.STONE);owner.setXRot(0);game.items().mana().set(owner,1100);require(use(s,InteractionHand.MAIN_HAND),"repeat native sniper for collision branch");shot=traces().getFirst();shot.position=Vec3.atCenterOf(wall);shot.yaw=0;shot.pitch=0;owner.level().getRandom().setSeed(seed);game.items().wands().tick();boolean alive=traces().contains(shot);continued|=alive;stopped|=!alive;require(owner.level().getBlockState(wall).isAir(),"sniper removes collision blocks even on final impact");clearTraces();
            }
            require(stopped&&continued,"both source20-percent stop and continued wall penetration exercised");
            owner.setXRot(90);game.items().mana().set(owner,1100);var enemy=cow(at.add(0,5,80));require(use(s,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==1100&&traces().isEmpty()&&owner.getXRot()<89,"downward sniper use is free aim assistance without a shot");enemy.discard();owner.setXRot(0);owner.setYRot(0);owner.removeAllEffects();game.items().mana().set(owner,100000);
        }
        LbcErecto.Sphere sphere;int waits,peak;
        void beginSphere(){
            var wall=BlockPos.containing(owner.getEyePosition().add(0,0,5));block(wall,Blocks.STONE);var s=loot("academy_erecto_wand");flag(s,"mode",3);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(use(s,InteractionHand.MAIN_HAND)&&game.items().erecto().sphereCount()==1,"native sphere first wall acquisition");sphere=game.items().erecto().sphereSnapshot().getFirst();for(int i=0;i<5;i++)game.items().erecto().tick();require(traces().size()==16&&traces().stream().allMatch(t->t.erecto.kind==LbcErecto.PHANTOM&&t.lifetime==8),"16 source phantom charges at5 ticks, each lifetime8");
            var phantom=traces().getFirst();var point=at.add(0,5,40);block(BlockPos.containing(point.add(0,0,1)),Blocks.STONE);phantom.position=point;phantom.yaw=0;phantom.pitch=0;game.items().wands().tick();require(traces().contains(phantom)&&Math.abs(phantom.position.distanceTo(point)-2)<1e-6,"phantom arc flies through solid wall");
            game.items().timeStop().begin(owner.level(),200);waits=0;
        }
        void farTrial(){
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setYRot(0);owner.setXRot(0);game.items().mana().set(owner,100000);require(use(trial,InteractionHand.OFF_HAND),"actual used-hand trial volley");var side=traces().stream().filter(t->t.erecto.kind==LbcErecto.SIDE).findFirst().orElseThrow();for(var t:traces())if(t!=side)t.age=t.lifetime;side.erecto.side=0;side.yaw=0;side.pitch=0;side.age=40;side.position=side.erecto.origin.add(0,0,65);var near=cow(side.position.add(0,-.6,1));game.items().wands().tick();require(flag(trial,"upgrade_available")==0,"old age40 near hit does not qualify");near.discard();side.age=1;side.position=side.erecto.origin.add(0,0,80);side.yaw=0;side.pitch=0;var far=cow(side.position.add(0,-.6,1));game.items().wands().tick();require(far.getHealth()<1000&&flag(trial,"upgrade_available")==1,"accepted real side hit beyond70 qualifies despite age1");far.discard();clearTraces();
        }
        boolean step(){long now=owner.level().getGameTime();if(last==now)return false;last=now;
            if(phase==0){presentation();geometry();ray();sniper();beginSphere();phase++;return false;}
            if(phase==1){require(sphere.age==5,"actual Time Stop freezes sphere and its schedule");if(++waits<3)return false;game.items().timeStop().end(owner.level());waits=0;phase++;return false;}
            if(phase==2){peak=Math.max(peak,traces().size());if(game.items().erecto().sphereCount()>0){require(++waits<320,"natural sphere expires in300 ticks");return false;}require(sphere.age==300&&peak<=32,"15-second source sphere completes without unbounded active arc accumulation");clearTraces();
                panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(at);panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);trial=loot("erecto_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(trial),"real Erecto Academy admission");phase++;return false;}
            if(phase==3){farTrial();phase++;return false;}
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_ERECTO)&&Identifier.parse("lbc:tier_ss").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real SS Academy promotion");return true;
        }
        public void close(){
            game.items().timeStop().end(owner.level());game.items().erecto().forget(owner);clearTraces();var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var entry:advancements.entrySet()){var advancement=game.server().getAdvancements().get(entry.getKey());for(String criterion:advancement.value().criteria().keySet())if(!entry.getValue().contains(criterion))owner.getAdvancements().revoke(advancement,criterion);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);for(long k:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(k),net.minecraft.world.level.ChunkPos.getZ(k),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.warm())return;if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS erecto");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL erecto",e);}}


    private static GameChecks.WandThirdPlayerState stopSaved;private static String epoch;private static Cow stopTarget;
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null,"exclusive stop probe");stopSaved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setXRot(0);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);var s=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_erecto_wand").getFirst();owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"live default charge before stop");var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("mode",2);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().erecto().rayPending(owner)==40,"live source ray queue before stop");stopTarget=new Cow(EntityTypes.COW,owner.level());stopTarget.setPos(owner.getEyePosition().add(owner.getLookAngle().scale(3)).add(0,-.6,0));stopTarget.setNoGravity(true);owner.level().addFreshEntity(stopTarget);d.putInt("mode",3);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().erecto().sphereCount()>0,"live native sphere before stop");epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS erecto-stop-probe");}
    public static void cleanup(GameController game){try{var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved!=null&&!game.items().currentEpoch(epoch)&&game.items().erecto().rayPending(owner)==0&&game.items().erecto().sphereCount()==0&&game.items().wands().electroSnapshot().stream().noneMatch(t->t.erecto!=null),"actual match stop clears all higher electric state");}finally{if(stopTarget!=null)stopTarget.discard();stopTarget=null;if(stopSaved!=null)stopSaved.restore(game);stopSaved=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS erecto-cleanup");}
}
