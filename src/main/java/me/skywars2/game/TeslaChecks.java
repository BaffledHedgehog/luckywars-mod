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

/** Native Tesla input, shared electric geometry, cooperative beams, trial and match lifecycle. */
public final class TeslaChecks {
    private TeslaChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Tesla: "+why);}
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
            for(String key:List.of("lbc:true_advancements/staffs/tesla","lbc:true_advancements/legends/academy/tesla")){
                var id=Identifier.parse(key);var advancement=game.server().getAdvancements().get(id);if(advancement!=null){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(id,done);}
            }
            owner.level().getAllEntities().forEach(before::add);team=game.server().getScoreboard().addPlayerTeam("tesla_fixture");team.setAllowFriendlyFire(false);
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

        List<LbcWands.ElectroTrace> shots(){return game.items().wands().electroSnapshot().stream().filter(t->t.owner==owner&&t.age<t.lifetime).toList();}
        void clear(){for(var t:shots())t.age=t.lifetime;game.items().wands().tick();}
        static final class ProbeCow extends Cow {
            int direct,conducting;
            ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
                if(source.is(LbcDamage.ELECTRO_BOLT))direct++;if(source.is(LbcDamage.ELECTRO_CONDUCTION))conducting++;
                return super.hurtServer(level,source,amount);
            }
        }
        ProbeCow cow(Vec3 point){var c=new ProbeCow(owner.level());c.setPos(point);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void block(BlockPos p,net.minecraft.world.level.block.Block type){blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,type.defaultBlockState(),2);}
        void presentation(){
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            for(boolean upgraded:new boolean[]{false,true}){
                var s=loot(upgraded?"academy_tesla_wand":"tesla_wand");
                require(s.is(upgraded?LbcWands.ACADEMY_TESLA:LbcWands.TESLA)&&flag(s,"lightningwand")==1&&flag(s,"staff")==1&&flag(s,"cyberset")==1,"native Tesla identity/source flags");
                require(Identifier.parse("lbc:lightningwand").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(upgraded?"lbc:tier_a":"lbc:tier_b").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(upgraded?12:11),"B/A source model/lore");
                require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("lightning_wand")&&!s.getHoverName().getStyle().isItalic(),"source translated name/style");
                require(flag(s,"upgraded")== (upgraded?1:0),"source Academy flag");
            }
            game.items().mana().set(owner,1199);require(!LbcTesla.use(game,owner,InteractionHand.MAIN_HAND,loot("tesla_wand"),false)&&game.items().mana().get(owner)==1199,"insufficient mana unchanged");
            var a=loot("tesla_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,a);owner.setItemInHand(InteractionHand.OFF_HAND,a.copy());game.items().mana().set(owner,100000);
            require(owner.gameMode.useItem(owner,owner.level(),a,InteractionHand.MAIN_HAND).consumesAction()&&shots().size()==8&&game.items().mana().get(owner)==97600,"native simultaneous hands, four arcs per hand and2400 mana");
            var lifetimes=shots().stream().map(t->t.lifetime).sorted().toList();require(lifetimes.equals(List.of(30,30,35,35,40,40,45,45)),"source four lifetimes per volley");
            var offsets=shots().stream().map(t->Math.round(t.position.subtract(owner.getEyePosition()).dot(owner.getLookAngle())*100)).sorted().toList();
            require(offsets.equals(List.of(-3L,-2L,-1L,0L,47L,48L,49L,50L)),"ordinary offhand source origin half-block forward");
            clear();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var s=loot("academy_tesla_wand");game.items().mana().set(owner,-10000);owner.setShiftKeyDown(true);
            for(int mode:new int[]{2,3,1})require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"mode")==mode&&game.items().mana().get(owner)==-10000,"free mode changes at negative mana");
            owner.setShiftKeyDown(false);game.items().mana().set(owner,100000);
        }
        void geometry(){
            var center=at.add(0,3,8);var victim=cow(center.add(0,-.6,1));game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);
            other.teleportTo(other.level(),center.x,center.y-.6,center.z+1,Set.of(),0,0,true);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);other.setHealth(1000);other.setAbsorptionAmount(0);
            require(use(loot("tesla_wand"),InteractionHand.OFF_HAND),"actual offhand Tesla volley");var trace=shots().getFirst();shots().stream().filter(t->t!=trace).forEach(t->t.age=t.lifetime);trace.position=center;trace.yaw=0;trace.pitch=0;
            game.items().wands().tick();require(Math.abs(trace.position.distanceTo(center)-2)<1e-6&&victim.getHealth()<1000&&victim.direct>=2&&victim.getKillCredit()==owner&&other.getHealth()==1000,"2-block native sampled arc hits friendly mob repeatedly, protects allied player and credits caster");
            game.items().wands().reflectElectro(other);require(trace.owner==other,"electro shield reflection transfers owner on shared trace");
            trace.owner=owner;trace.position=center;trace.age=29;game.items().wands().tick();require(!shots().contains(trace),"ordinary arc expires at30 source ticks");clear();victim.discard();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            // A real vanilla shield clips Tesla at the same shared sphere as other shield_blockable arcs.
            game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));other.setYRot(180);other.startUsingItem(InteractionHand.MAIN_HAND);for(int i=0;i<6;i++)other.doTick();require(other.isBlocking(),"native shield blocking delay");
            require(use(loot("tesla_wand"),InteractionHand.MAIN_HAND),"shield volley");var blocked=shots().getFirst();blocked.position=center.add(0,0,-2);blocked.yaw=0;blocked.pitch=0;game.items().wands().tick();require(!shots().contains(blocked)&&blocked.position.distanceTo(center.add(0,0,-2))<2,"native shield clips moving Tesla arc");clear();other.stopUsingItem();other.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            other.teleportTo(other.level(),owner.getX()+1,owner.getY(),owner.getZ(),Set.of(),0,0,true);owner.setHealth(1000);other.setHealth(1000);owner.setInvulnerableTime(0);other.setInvulnerableTime(0);
            game.items().wands().teslaConduction(new LbcWands.ElectroTrace(owner,owner.position(),game.items().epoch()),owner.position());
            require(owner.getHealth()==999&&other.getHealth()==999,"conduction reaches caster and allied player even with team friendly fire disabled");
            other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);
        }
        void chain(){
            var s=loot("academy_tesla_wand");flag(s,"mode",3);require(use(s,InteractionHand.MAIN_HAND)&&shots().size()==1,"one native chain carrier");var trace=shots().getFirst();require(trace.teslaChain&&trace.lifetime==150,"source chain flag/lifetime150");
            var center=at.add(0,4,12);trace.position=center;trace.yaw=0;trace.pitch=0;
            var near=cow(center.add(1,0,4));var ring=cow(center.add(7,0,4));
            game.items().wands().tick();require(Math.abs(trace.position.distanceTo(center)-4)<1e-6&&trace.yaw==0,"chain moves four blocks and near target suppresses turn");
            Vec3 p=trace.position;for(int i=0;i<4;i++)game.items().wands().tick();require(trace.position.equals(p),"source flight only every5 ticks");near.discard();var friendly=cow(center.add(2,0,4));game.server().getScoreboard().addPlayerToTeam(friendly.getScoreboardName(),team);
            trace.position=center;trace.yaw=0;trace.pitch=0;other.teleportTo(other.level(),center.x-6,center.y,center.z+4,Set.of(),0,0,true);
            game.items().wands().tick();require(trace.yaw< -80&&trace.yaw> -100,"chain seeks enemy ring and ignores closer allied player/friendly mob");
            ring.discard();friendly.discard();trace.age=149;game.items().wands().tick();require(!shots().contains(trace),"chain expires at150 actual ticks");clear();other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);
        }
        void boundaries(){
            var mark=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());mark.setPos(at);mark.entityTags().add("stopper_magic");owner.level().addFreshEntity(mark);game.items().magic().loaded(mark);
            try{int mana=game.items().mana().get(owner);require(!use(loot("tesla_wand"),InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana,"mana wand activation blocked by real antimagic");
                // Already launched electricity remains nonmagical inside that same beacon.
                game.items().wands().teslaVolley(owner,InteractionHand.MAIN_HAND,loot("tesla_wand"),false);var trace=shots().getFirst();var from=trace.position;game.items().wands().tick();require(trace.position.distanceToSqr(from)>1,"existing arcs keep moving inside antimagic");clear();
            }finally{game.items().magic().unloaded(mark);mark.discard();}
        }
        int waits;ProbeCow instantVictim;
        void beginInstant(){
            owner.setYRot(0);owner.setXRot(0);var s=loot("academy_tesla_wand");flag(s,"mode",2);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);instantVictim=cow(owner.getEyePosition().add(0,-.6,2));
            for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)block(BlockPos.containing(owner.getEyePosition()).offset(x,y,4),Blocks.STONE);
            int before=game.items().stonekivi().count(),mana=game.items().mana().get(owner);require(use(s,InteractionHand.MAIN_HAND)&&game.items().stonekivi().count()==before+1&&game.items().mana().get(owner)==mana-1200,"native instant queues one five-beam work item for1200");
            game.items().timeStop().begin(owner.level(),200);
        }
        void conductionTrial(){
            var center=at.add(0,4,20);var pos=BlockPos.containing(center);block(pos,Blocks.IRON_BLOCK);
            var cows=new ArrayList<ProbeCow>();for(int i=0;i<10;i++){var c=cow(center.add(4,0,(i-5)*.3));cows.add(c);if(i%2==0)game.server().getScoreboard().addPlayerToTeam(c.getScoreboardName(),team);}
            var technical=cow(center.add(-4,0,0));technical.entityTags().add("strict_map_object");cows.getLast().setPermanentlyInvulnerable(true);
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);require(use(trial,InteractionHand.OFF_HAND),"actual trial offhand volley");var trace=shots().getFirst();shots().stream().filter(t->t!=trace).forEach(t->t.age=t.lifetime);trace.position=center;trace.yaw=0;trace.pitch=0;
            game.items().wands().tick();require(flag(trial,"upgrade_available")==0&&cows.stream().limit(9).allMatch(c->c.getHealth()==999)&&technical.getHealth()==1000,"nine accepted conductor targets insufficient, friendly mobs hit and technical protected");
            cows.getLast().setPermanentlyInvulnerable(false);trace.position=center;game.items().wands().tick();require(flag(trial,"upgrade_available")==1&&cows.getLast().getHealth()==999&&cows.getFirst().getKillCredit()==owner,"ten actual conductor hits qualify used offhand stack with native credit");
            clear();cows.forEach(Entity::discard);technical.discard();
        }
        boolean step(){long now=owner.level().getGameTime();if(last==now)return false;last=now;
            if(phase==0){presentation();geometry();chain();boundaries();beginInstant();phase++;return false;}
            if(phase==1){require(game.items().stonekivi().count()>0&&instantVictim.direct==0,"actual Time Stop freezes queued instant work");if(++waits<3)return false;game.items().timeStop().end(owner.level());waits=0;phase++;return false;}
            if(phase==2){if(game.items().stonekivi().count()>0){require(++waits<200,"queued five rays complete");return false;}require(instantVictim.getHealth()<1000&&instantVictim.direct>=5&&instantVictim.getKillCredit()==owner,"all five native instant beams hit and credit real caster");instantVictim.discard();
                panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(at);panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);trial=loot("tesla_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(trial),"actual Tesla Academy admission");phase++;return false;}
            if(phase==3){conductionTrial();phase++;return false;}
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_TESLA)&&Identifier.parse("lbc:tier_a").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real Tesla Academy A promotion");return true;
        }
        public void close(){
            game.items().timeStop().end(owner.level());var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var entry:advancements.entrySet()){var advancement=game.server().getAdvancements().get(entry.getKey());for(String criterion:advancement.value().criteria().keySet())if(!entry.getValue().contains(criterion))owner.getAdvancements().revoke(advancement,criterion);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);for(long k:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(k),net.minecraft.world.level.ChunkPos.getZ(k),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.warm())return;if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS tesla");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tesla",e);}}

    private static GameChecks.WandThirdPlayerState stopSaved;private static String epoch;
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null,"exclusive stop probe");stopSaved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);var s=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_tesla_wand").getFirst();var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("mode",2);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().stonekivi().count()>0,"instant work pending before actual stop");d.putInt("mode",3);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"live chain before stop");epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS tesla-stop-probe");}
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(epoch)&&game.items().stonekivi().count()==0&&game.items().wands().electroSnapshot().isEmpty(),"actual match stop clears instant work and moving Tesla arcs");}finally{if(stopSaved!=null)stopSaved.restore(game);stopSaved=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS tesla-cleanup");}
}
