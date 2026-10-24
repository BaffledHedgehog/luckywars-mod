package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual native ray/menu use plus deterministic payload, causal hazard and lifecycle boundaries. */
public final class GaycasterChecks {
    private GaycasterChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Gaycaster: "+why);}
    private static Fixture running;
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Long> forced=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<String> completed=new HashSet<>(),unlocks;
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final boolean offhand;final int skill,trinket,challengeProgress;
        final net.minecraft.world.scores.PlayerTeam team;
        Cow victim;int phase,color=-1,waits,warmTicks;boolean rayReady,emptyDone;double emptyDistance;long last=Long.MIN_VALUE;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;
            var p=game.players().get(owner.getUUID());skill=p.lbcSkill;trinket=p.lbcTrinket;challengeProgress=p.lbcChallengeProgress;unlocks=new HashSet<>(p.lbcUnlocks);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            var a=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/gaycaster"));require(a!=null,"source advancement packaged");owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(completed::add);
            owner.level().getAllEntities().forEach(before::add);team=game.server().getScoreboard().addPlayerTeam("gaycaster_fixture");
        }
        void init(){
            for(int x=-16;x<=-14;x++)for(int z=-16;z<=-6;z++){long k=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(k)){forced.add(k);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);owner.setShiftKeyDown(false);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;game.config().offhandWands=true;
            game.items().mana().set(owner,100000);game.items().mana().noMagic(owner,0);owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(900);
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
        }
        boolean warm(){require(++warmTicks<=200,"resident fixture ready");for(long k:forced)if(!owner.level().areEntitiesLoaded(k))return false;return true;}
        ItemStack loot(String leaf){var l=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(l.size()==1,"one item "+leaf);return l.getFirst();}
        boolean use(InteractionHand hand){var s=loot("gaycaster");owner.setItemInHand(hand,s);return owner.gameMode.useItem(owner,owner.level(),s,hand).consumesAction();}
        void block(BlockPos p,net.minecraft.world.level.block.Block b){blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,b.defaultBlockState(),2);}
        void capture(Vec3 point){var p=BlockPos.containing(point);for(var q:BlockPos.betweenClosed(p.offset(-3,-2,-3),p.offset(3,4,3)))blocks.putIfAbsent(q.immutable(),game.maps().snapshot(owner.level(),q));}
        Cow cow(Vec3 pos){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(pos);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void restoreBlocks(){for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}blocks.clear();}
        void clearActors(){if(victim!=null){game.items().kiuas().clearCurse(victim);victim.discard();victim=null;}for(var a:game.items().clouds().acidSnapshot())if(a.getOwner()==owner)a.discard();for(var t:game.items().wands().electroSnapshot())if(t.owner==owner)t.age=t.lifetime;owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        void craft(boolean workbench){
            owner.closeContainer();owner.getInventory().clearContent();var pos=owner.blockPosition().offset(1,0,0);
            if(workbench){block(pos,Blocks.CRAFTING_TABLE);owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(owner.level(),pos)),net.minecraft.network.chat.Component.literal("test")));}
            var menu=(net.minecraft.world.inventory.AbstractCraftingMenu)owner.containerMenu;
            var slots=menu.getInputGridSlots();var first=loot("unstable_prism").copyWithCount(2);var second=loot("omega_flag").copyWithCount(2);
            slots.getFirst().setByPlayer(first);slots.getLast().setByPlayer(second);
            require(menu.getResultSlot().getItem().is(LbcWands.GAYCASTER),"native "+(workbench?"3x3":"2x2")+" preview");
            slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra ingredient rejects recipe");slots.get(1).setByPlayer(ItemStack.EMPTY);
            for(int i=0;i<36;i++)owner.getInventory().setItem(i,new ItemStack(Items.STICK,64));
            GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
            require(slots.getFirst().getItem().getCount()==2&&slots.getLast().getItem().getCount()==2,"full inventory consumes nothing");
            // Ordinary pickup consumes exactly one of each, without a speculative preview side effect.
            GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
            require(menu.getCarried().is(LbcWands.GAYCASTER)&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"one result / one pair on actual native click");
            menu.setCarried(ItemStack.EMPTY);slots.forEach(s->s.setByPlayer(ItemStack.EMPTY));owner.getInventory().clearContent();owner.closeContainer();
        }
        void presentation(){
            var s=loot("gaycaster");var d=s.get(DataComponents.CUSTOM_DATA).copyTag();
            require(s.is(LbcWands.GAYCASTER)&&d.getIntOr("gaycaster",0)==1&&d.getIntOr("magicwand",0)==1&&d.getIntOr("staff",0)==1&&d.getIntOr("transformable",0)==1,"ready identity and source flags");
            require(Identifier.parse("lbc:gaycaster").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==11,"original full presentation");
            require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("gaycaster")&&!s.getHoverName().getStyle().isItalic(),"source translatable name/style");
            craft(false);craft(true);game.items().mana().set(owner,1499);require(!use(InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==1499,"insufficient mana untouched");
            game.items().mana().set(owner,100000);
        }
        void launch(){
            clearActors();double distance=3+5*color;Vec3 p=owner.getEyePosition().add(0,-.6,distance);capture(p);victim=cow(p);
            // Every other source colour physically hits a friendly mob.
            if(color%2==0)game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);
            require(LbcGaycaster.target(game,owner,victim)&&!LbcGaycaster.target(game,owner,other),"friendly mobs accepted, allied player protected");
            victim.entityTags().add("strict_map_object");require(!LbcGaycaster.target(game,owner,victim),"technical role protected");victim.entityTags().remove("strict_map_object");
            int mana=game.items().mana().get(owner);require(use(color%2==0?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana-1500,"actual native PKM1500");
            waits=0;rayReady=false;
        }
        void verifyColor(){
            switch(color){
                case 0->{owner.doTick();require(victim.getHealth()==994&&victim.getKillCredit()==owner&&owner.getHealth()>900,"red6/native accepted healing/owner: hp="+victim.getHealth()+" credit="+victim.getKillCredit()+" casterHP="+owner.getHealth());}
                case 1->require(game.items().mana().hudValue(victim,"effect_kiuaskivi")>0&&game.items().mana().hudValue(victim,"effect_kiuaskivi")<=100&&!victim.hasEffect(MobEffects.RAID_OMEN),"orange100 tick native transport conversion");
                case 2->{require(game.items().psycho().hudValue(victim,"piss")>0,"yellow existing affliction");require(game.items().psycho().yellow(victim,owner)&&game.items().psycho().hudValue(victim,"piss")==100,"yellow sets100 instead of stacking");}
                case 3->require(game.items().clouds().acidSnapshot().stream().filter(a->a.getOwner()==owner&&!a.isRemoved()).count()==6,"green six native owned acids");
                case 4->{var arcs=game.items().wands().electroSnapshot().stream().filter(a->a.owner==owner).toList();require(arcs.size()==4&&arcs.stream().allMatch(a->!a.mechanism)&&arcs.stream().map(a->a.lifetime).sorted().toList().equals(List.of(5,7,9,11)),"cyan four short magic arcs");}
                case 5->{require(victim.hasEffect(MobEffects.CONDUIT_POWER)&&victim.getEffect(MobEffects.CONDUIT_POWER).getAmplifier()==1,"blue exact native enhanced conduit");
                    game.items().magic().colors().tickWater();require(owner.level().getBlockState(victim.blockPosition()).is(Blocks.WATER),"native indexed enhanced water");
                    victim.setInvulnerableTime(0);victim.damageCooldownTime=0;require(victim.hurtServer(owner.level(),owner.level().damageSources().drown(),1)&&victim.getKillCredit()==owner,"blue water native drowning inherits caster");}
                case 6->require(victim.getHealth()==995&&victim.getKillCredit()==owner&&victim.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD),"purple5 native void/owner");
            }
        }
        void redHealing(){
            clearActors();Vec3 p=at.add(10,0,0);victim=cow(p);var second=cow(p.add(.1,0,0));
            try{
                var targets=owner.level().getEntitiesOfClass(Cow.class,new net.minecraft.world.phys.AABB(p,p).inflate(1.5),
                        e->e.position().distanceToSqr(p)<=2.25&&LbcGaycaster.target(game,owner,e));
                require(targets.size()==2,"native mixed-red target order");
                var first=targets.getFirst();var last=targets.getLast();
                owner.removeEffect(MobEffects.INSTANT_HEALTH);owner.setHealth(900);victim.setPermanentlyInvulnerable(true);
                LbcGaycaster.impact(game,owner,p,new Vec3(0,0,1),350,victim);
                require(victim.getHealth()==1000&&!owner.hasEffect(MobEffects.INSTANT_HEALTH),"rejected direct red hit never heals");
                first.setPermanentlyInvulnerable(true);last.setPermanentlyInvulnerable(true);
                LbcGaycaster.impact(game,owner,p,new Vec3(0,0,1),350,null);
                require(!owner.hasEffect(MobEffects.INSTANT_HEALTH),"all rejected block targets never heal");
                for(int order=0;order<3;order++){
                    for(var e:targets){e.setPermanentlyInvulnerable(false);e.setInvulnerableTime(0);e.damageCooldownTime=0;e.setHealth(1000);}
                    if(order==0)last.setPermanentlyInvulnerable(true);else if(order==1)first.setPermanentlyInvulnerable(true);
                    owner.removeEffect(MobEffects.INSTANT_HEALTH);owner.setHealth(900);
                    LbcGaycaster.impact(game,owner,p,new Vec3(0,0,1),350,null);
                    require(first.getHealth()==(order==1?1000:994)&&last.getHealth()==(order==0?1000:994),"every target processed independently of accepted hit order");
                    require(owner.hasEffect(MobEffects.INSTANT_HEALTH)&&owner.getEffect(MobEffects.INSTANT_HEALTH).getAmplifier()==1,"any accepted block hit grants native healII");
                    owner.doTick();require(owner.getHealth()==908,"one8HP heal regardless of target count/order");
                }
            }finally{second.discard();clearActors();}
        }
        void boundaries(){
            redHealing();
            clearActors();victim=cow(at.add(10,0,0));capture(victim.position());var q=victim.blockPosition();block(q,Blocks.CRAFTING_TABLE);block(q.above(),Blocks.ICE);block(q.east(),Blocks.STONE);
            require(victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RAID_OMEN,100,111),owner),"native raid111 ingress");
            game.items().kiuas().unloaded(victim);game.items().kiuas().loaded(victim);
            game.items().kiuas().tick();require(owner.level().getBlockState(q).is(Blocks.FIRE)&&owner.level().getBlockState(q.east()).is(Blocks.STONE)&&game.items().mana().hudValue(victim,"effect_kiuaskivi")==99,"resumed curse burns exact source tags and decrements one tick: state="+owner.level().getBlockState(q)+" ticks="+game.items().mana().hudValue(victim,"effect_kiuaskivi"));
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;require(victim.hurtServer(owner.level(),owner.level().damageSources().inFire(),1)&&victim.getKillCredit()==owner,"native curse fire inherits owner");
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.hurtServer(owner.level(),owner.level().damageSources().playerAttack(other),1);
            victim.setPos(victim.position().add(2,0,0));capture(victim.position());block(victim.blockPosition(),Blocks.CRAFTING_TABLE);game.items().kiuas().tick();
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.hurtServer(owner.level(),owner.level().damageSources().inFire(),1);
            require(victim.getKillCredit()==other,"continuing old curse does not overwrite later real hit");
            game.items().kiuas().clearCurse(victim);require(game.items().mana().hudValue(victim,"effect_kiuaskivi")==0,"purify curse removes owner/counter");
            var mark=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());mark.setPos(at);mark.entityTags().add("stopper_magic");owner.level().addFreshEntity(mark);game.items().magic().loaded(mark);
            try{int mana=game.items().mana().get(owner);require(!use(InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana,"real antimagic cast gate");}finally{game.items().magic().unloaded(mark);mark.discard();}
            clearActors();victim=cow(owner.getEyePosition().add(0,-.6,3));game.items().mana().set(owner,100000);require(use(InteractionHand.MAIN_HAND),"queued ray");
            game.items().timeStop().begin(owner.level(),200);waits=0;
        }
        boolean step(){
            long now=owner.level().getGameTime();if(last==now)return false;last=now;
            if(phase==0){presentation();color=0;launch();phase=1;return false;}
            if(phase==1){
                if(game.items().exasperato().pendingSearches()>0){require(++waits<200,"shared ray completes");return false;}
                if(!rayReady){rayReady=true;return false;}
                verifyColor();restoreBlocks();if(++color<7){launch();return false;}boundaries();phase=2;return false;
            }
            if(phase==2){require(victim.getHealth()==1000&&game.items().exasperato().pendingSearches()>0,"actual Time Stop freezes pending ray");if(++waits<3)return false;game.items().timeStop().end(owner.level());phase=3;waits=0;return false;}
            if(phase==3){
                if(game.items().exasperato().pendingSearches()>0){require(++waits<200,"queued ray resumes");return false;}
                require(victim.getHealth()==994,"actual resume applies one impact");clearActors();
                owner.setItemInHand(InteractionHand.OFF_HAND,loot("gaycaster"));game.items().mana().set(owner,100000);
                require(use(InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==97000,"actual two-hand PKM pays1500 per hand");
                Vec3 from=new Vec3(owner.getX(),400,owner.getZ());
                game.items().exasperato().searchSpell(owner,from,new Vec3(0,1,0),125,false,(p,s)->false,e->false,
                        (a,b)->emptyDistance=from.distanceTo(b)).thenAccept(hit->{require(hit==null,"empty sky miss");emptyDone=true;});
                phase=4;waits=0;return false;
            }
            if(phase==4){
                if(game.items().exasperato().pendingSearches()>0){require(++waits<200,"finite sky and dual casts finish");return false;}
                require(emptyDone&&Math.abs(emptyDistance-125)<1e-5,"finite source range draws full125 in empty sky without targets");
                return true;
            }
            throw new IllegalStateException("phase");
        }
        public void close(){
            game.items().timeStop().end(owner.level());clearActors();game.items().kiuas().clearCurse(owner);game.items().kiuas().clearCurse(other);
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            var a=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/gaycaster"));for(String c:a.value().criteria().keySet())if(!completed.contains(c))owner.getAdvancements().revoke(a,c);
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);
            for(long k:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(k),net.minecraft.world.level.ChunkPos.getZ(k),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.warm())return;if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS gaycaster");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL gaycaster",e);}}
    private static GameChecks.WandThirdPlayerState stopSaved;
    private static String epoch;
    public static void stopProbe(GameController game){
        var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null,"exclusive stop");stopSaved=new GameChecks.WandThirdPlayerState(p,game.items().mana());
        p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().set(p,100000);game.items().mana().noMagic(p,0);
        var s=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/gaycaster").getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,s);
        require(game.items().wands().use(p,InteractionHand.MAIN_HAND)&&game.items().exasperato().pendingSearches()>0,"actual pending source ray before stop");
        require(p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RAID_OMEN,100,111),p),"live curse before stop");
        epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS gaycaster-stop-probe");
    }
    public static void cleanup(GameController game){try{var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved!=null&&!game.items().currentEpoch(epoch)&&game.items().exasperato().pendingSearches()==0&&game.items().mana().hudValue(p,"effect_kiuaskivi")==0,"actual match stop clears queue/curse");}finally{if(stopSaved!=null)stopSaved.restore(game);stopSaved=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS gaycaster-cleanup");}
}
