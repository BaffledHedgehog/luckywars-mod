package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Synchronous native dispatch boundaries; no claim that explicit calls advance server time. */
public final class ChefChecks {
    private ChefChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Chef: "+why);}
    private static Object copy(Object value){if(value instanceof int[] a)return a.clone();if(value instanceof Set<?> s)return new HashSet<>(s);return value;}
    private static Map<Field,Object> fields(Object target){
        var result=new LinkedHashMap<Field,Object>();
        try{for(var f:target.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))result.put(f,copy(f.get(target)));}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}return result;
    }
    private static void fields(Object target,Map<Field,Object> values){try{for(var e:values.entrySet())e.getKey().set(target,copy(e.getValue()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static Map<Field,Object> attackers(ServerPlayer player){
        var saved=new LinkedHashMap<Field,Object>();
        try{for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime")){
            var field=net.minecraft.world.entity.LivingEntity.class.getDeclaredField(name);field.setAccessible(true);saved.put(field,field.get(player));
        }}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        return saved;
    }
    /** Existing pending combat must belong to its game, never to an operator-run fixture. */
    private static boolean cleanAttribution(GameController game,ServerPlayer player){
        var service=game.items().indirectKills();
        try{for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){
            field.setAccessible(true);if(((Map<?,?>)field.get(service)).containsKey(player))return false;
        }}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        return true;
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final PlayerStore.Profile profile;
        final Map<Field,Object> profileFields,attackerFields;final boolean shift;final int visualRegen,cowKills;final net.minecraft.world.scores.PlayerTeam team;
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Stat<Identifier>,Integer> custom=new LinkedHashMap<>();
        final Map<String,Integer> scores=new LinkedHashMap<>();
        final Map<Stat<Item>,Integer> crafted=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){
            this.player=player;team=player.getTeam();state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());profileFields=fields(profile);attackerFields=attackers(player);
            for(String name:List.of("fartus_sword"))scores.put(name,game.items().mana().score(player,name).get());
            shift=player.isShiftKeyDown();visualRegen=game.items().mana().regeneration(player);cowKills=player.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));
            for(var adv:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);advancements.put(adv,done);}
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);custom.put(stat,player.getStats().getValue(stat));}
            for(var item:BuiltInRegistries.ITEM)for(var type:List.of(Stats.ITEM_CRAFTED,Stats.ITEM_USED,Stats.ITEM_PICKED_UP,Stats.ITEM_DROPPED)){var stat=type.get(item);crafted.put(stat,player.getStats().getValue(stat));}
        }
        void restore(GameController game){
            advancements.forEach((adv,done)->{for(var key:adv.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(adv,key);else player.getAdvancements().revoke(adv,key);});
            var board=game.server().getScoreboard();if(player.getTeam()!=null)board.removePlayerFromTeam(player.getScoreboardName(),player.getTeam());if(team!=null)board.addPlayerToTeam(player.getScoreboardName(),team);fields(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);fields(player,attackerFields);game.items().mana().score(player,"manaregen_visual").set(visualRegen);
            scores.forEach((name,value)->game.items().mana().score(player,name).set(value));
            custom.forEach((stat,value)->player.getStats().setValue(player,stat,value));crafted.forEach((stat,value)->player.getStats().setValue(player,stat,value));
            player.getStats().setValue(player,Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcWtf service;final LbcMana mana;final ServerPlayer owner,other;
        final Saved saved,otherSaved;final Map<Field,Object> config;final Vec3 at;final BlockPos center;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final List<Cow> cows=new ArrayList<>();
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final net.minecraft.world.scores.PlayerTeam team;
        Fixture(GameController game){
            this.game=game;service=game.items().wtf();mana=game.items().mana();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active participants");
            require(service.pendingCount()==0&&!game.items().timeStop().stopped(owner.level()),"no unrelated Chef, fountain or dimension Time Stop work");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&cleanAttribution(game,p)&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0,"closed menus and no pre-existing combat/frost");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            center=owner.blockPosition().offset(0,32,0);at=Vec3.atBottomCenterOf(center);require(center.getY()+12<owner.level().getMaxY(),"bounded clearbox below ceiling");
            team=game.server().getScoreboard().addPlayerTeam("chef_fixture");team.setAllowFriendlyFire(false);
        }
        void init(){
            for(var p:BlockPos.betweenClosed(center.offset(-16,-4,-16),center.offset(16,12,16))){require(owner.level().hasChunkAt(p),"all possible test terrain resident");blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));}
            for(var p:blocks.keySet())block(p,p.getY()==center.getY()-1?Blocks.STONE:Blocks.AIR);
            game.config().ranked=false;game.config().lbcSkills=false;
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                p.entityTags().remove("time_stopped");p.entityTags().remove("sin_greed_active");mana.noMagic(p,0);mana.set(p,0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);
            }
            move(owner,at.add(-12,0,-12));move(other,at.add(12,0,-12));game.items().magic().flushCleansing();
        }
        void block(BlockPos p,Block type){require(blocks.containsKey(p),"fixture writes only snapshotted terrain");owner.level().setBlock(p,type.defaultBlockState(),2);}
        void move(ServerPlayer player,Vec3 pos){player.teleportTo(player.level(),pos.x,pos.y,pos.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        Cow cow(Vec3 pos){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(pos);c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);c.setHealth(20);c.setNoAi(true);c.setNoGravity(true);owner.level().addFreshEntity(c);cows.add(c);return c;}
        LbcWtf.Chef spawn(ServerPlayer author,Cow target){var c=service.spawn(author,target);require(c!=null&&!c.isRemoved(),"native Chef created");require(blocks.containsKey(c.blockPosition()),"spawn inside saved terrain");return c;}
        void clear(){for(var c:service.snapshot())service.unloaded(c);require(service.count()==0,"all fixture Chefs retired");}
        void remove(Cow c){game.items().indirectKills().clear(c);c.discard();}
        void advance(LbcWtf.Chef chef,int ticks){for(int i=0;i<ticks&&!chef.isRemoved();i++){LbcChefBrain.beginTick(-i-1L);service.step(chef);chef.tick();require(blocks.containsKey(chef.blockPosition()),"native pursuit remains in saved resident box");}}
        void nativeMaterialAndOwnership(){
            var material=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/wtf_item").getFirst();owner.setItemInHand(InteractionHand.MAIN_HAND,material.copyWithCount(2));var victim=cow(at.add(0,0,6));var foreign=cow(at.add(5,0,6));
            require(victim.hurtServer(owner.level(),victim.damageSources().playerAttack(owner),1)&&owner.getMainHandItem().getCount()==1&&service.count()==1&&mana.get(owner)==0,"actual accepted native material melee consumes one, no mana");var chef=service.snapshot().getFirst();
            require(chef instanceof net.minecraft.world.entity.decoration.Mannequin&&chef.getType()==EntityTypes.MANNEQUIN&&service.owner(chef)==owner&&chef.target==victim&&!chef.shouldBeSaved(),"actual unsaved Mannequin and fixed hit victim/current owner");require(chef.getProfile()!=null,"native Mannequin profile metadata exists");var tools=new HashSet<Item>();for(var tool:service.tools(chef))tools.add(tool.getItem());require(tools.equals(Set.of(Items.NETHERITE_PICKAXE,Items.NETHERITE_AXE,Items.NETHERITE_SHOVEL,Items.NETHERITE_HOE,Items.SHEARS,Items.NETHERITE_SWORD)),"native tools and sword available");
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;require(victim.hurtServer(owner.level(),victim.damageSources().indirectMagic(owner,owner),1)&&owner.getMainHandItem().getCount()==1&&service.count()==1,"nonmelee native damage never consumes material or creates another Chef");
            require(!game.items().wands().target(owner,chef)&&game.items().wands().projectileTarget(owner,chef),"summoned Chef excluded from owner homing but accepts friendly physical projectiles");
            var second=spawn(other,foreign);require(second.target==foreign&&service.owner(second)==other&&chef.target==victim,"two owners retain independent fixed victims");clear();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);remove(victim);remove(foreign);
        }
        void infiniteHealthAndStasis(){
            var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));chef.setDeltaMovement(Vec3.ZERO);float hp=chef.getHealth();
            require(chef.hurtServer(owner.level(),chef.damageSources().playerAttack(other),7)&&chef.getHealth()==hp&&Math.abs(service.damageCount(chef)-7)<.001&&service.stasis(chef)==0,"actual native7 damage preserves HP and tracks partial threshold");chef.setInvulnerableTime(0);chef.damageCooldownTime=0;
            require(chef.hurtServer(owner.level(),chef.damageSources().playerAttack(other),13)&&chef.getHealth()==hp&&service.stasis(chef)==200,"actual total20 damage enters200tick stasis with infinite HP");var position=chef.position();float targetHp=victim.getHealth();
            game.items().timeStop().begin(owner.level(),40);try{advance(chef,10);require(service.stasis(chef)==200,"dimension Time Stop pauses stasis clock");}finally{game.items().timeStop().end(owner.level());}
            chef.knockback(2,1,0,chef.damageSources().generic(),0);require(chef.getDeltaMovement().lengthSqr()<1e-12,"stasis rejects external native knockback");chef.snapTo(position.x+2,position.y+1,position.z+2,31,17);require(chef.position().distanceToSqr(position)<1e-9,"stasis rejects native snapTo relocation");chef.teleportTo(position.x+3,position.y+1,position.z+3);require(chef.position().distanceToSqr(position)<1e-9,"stasis rejects native teleportTo relocation");
            advance(chef,199);require(service.stasis(chef)==1&&chef.position().distanceToSqr(position)<1e-9&&victim.getHealth()==targetHp,"199 explicit active boundaries keep stasis immobile and harmless");advance(chef,1);require(service.stasis(chef)==0&&chef.getHealth()==hp,"wake exactly at200 active boundaries");advance(chef,20);require(chef.position().distanceToSqr(position)>.01,"native actor resumes movement after wake");clear();remove(victim);
        }
        void directSword(){
            var victim=cow(at.add(0,0,3));var outsider=cow(at.add(.6,0,3));var chef=spawn(owner,victim);chef.setPos(victim.position().add(0,0,-1));chef.setDeltaMovement(Vec3.ZERO);service.step(chef);
            require(victim.getHealth()<20&&victim.getKillCredit()==owner&&outsider.getHealth()==20,"actual native sword hits only fixed victim with summoner attribution");require(!chef.getMainHandItem().isEmpty(),"native held weapon visible in equipment");
            for(int i=0;i<100&&!victim.isDeadOrDying();i++){victim.setInvulnerableTime(0);victim.damageCooldownTime=0;advance(chef,1);}require(victim.isDeadOrDying()&&victim.getKillCredit()==owner,"actual native sword death credits summoner");clear();remove(victim);remove(outsider);
        }
        @Override public void close(){
            clear();service.stop();game.items().timeStop().end(owner.level());for(var c:cows){game.items().indirectKills().clear(c);c.discard();}for(var p:List.of(owner,other)){game.items().frost().clear(p);game.items().indirectKills().clear(p);}
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());fields(game.config(),config);saved.restore(game);otherSaved.restore(game);game.server().getScoreboard().removePlayerTeam(team);
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);game.players().save();game.items().magic().flushCleansing();
        }
        void corridor(boolean gap){
            for(int z=-6;z<=8;z++)for(int x=-1;x<=1;x++)for(int y=0;y<=2;y++)block(center.offset(x,y,z),x!=0||y==2?Blocks.BEDROCK:Blocks.AIR);
            for(int z=-6;z<=8;z++)block(center.offset(0,-1,z),gap&&z>=0&&z<=2?Blocks.AIR:Blocks.STONE);
            block(center.offset(0,0,-6),Blocks.BEDROCK);block(center.offset(0,1,-6),Blocks.BEDROCK);
        }
        void clearCorridor(){for(int z=-6;z<=8;z++)for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++)block(center.offset(x,y,z),y==-1?Blocks.STONE:Blocks.AIR);}
        void boundedTerrain(){
            corridor(false);block(center.offset(0,0,0),Blocks.STONE);block(center.offset(0,1,0),Blocks.OBSIDIAN);block(center.above(2),Blocks.AIR);block(center.above(3),Blocks.AIR);var supportVictim=cow(at.add(0,2,0));var newerVictim=cow(at.add(.02,2,0));supportVictim.setOnGround(true);newerVictim.setOnGround(true);var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));
            for(int i=0;i<700&&!chef.isRemoved()&&!owner.level().getBlockState(center.above()).isAir();i++)advance(chef,1);
            require(owner.level().getBlockState(center.above()).isAir(),"bounded native NPC completes obsidian mining beyond36 stationary ticks; position="+chef.position()+" removed="+chef.isRemoved());game.items().indirectKills().finishSupportRemoval();require(game.items().indirectKills().altitudeAuthor(supportVictim)==owner&&game.items().indirectKills().altitudeAuthor(newerVictim)==owner,"actual NPC mining captures native standing victims support-removal cause; first="+game.items().indirectKills().altitudeAuthor(supportVictim)+" second="+game.items().indirectKills().altitudeAuthor(newerVictim));
            supportVictim.setInvulnerableTime(0);supportVictim.damageCooldownTime=0;require(supportVictim.hurtServer(owner.level(),supportVictim.damageSources().fellOutOfWorld(),1000)&&supportVictim.isDeadOrDying()&&supportVictim.getKillCredit()==owner,"actual later native void death credits mining summoner");newerVictim.setOnGround(false);require(newerVictim.hurtServer(owner.level(),newerVictim.damageSources().playerAttack(other),1),"newer accepted native hit after mined support");newerVictim.setInvulnerableTime(0);newerVictim.damageCooldownTime=0;require(newerVictim.hurtServer(owner.level(),newerVictim.damageSources().fellOutOfWorld(),1000)&&newerVictim.isDeadOrDying()&&newerVictim.getKillCredit()==other,"later real player hit supersedes NPC terrain cause");remove(supportVictim);remove(newerVictim);clear();remove(victim);clearCorridor();
            corridor(true);victim=cow(at.add(0,0,6));chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));boolean built=false;
            for(int i=0;i<700&&!chef.isRemoved()&&!built;i++){advance(chef,1);for(int z=0;z<=2;z++)if(!owner.level().getBlockState(center.offset(0,-1,z)).isAir())built=true;}
            require(built,"bounded native NPC places actual infinite bridge blocks across saved gap");clear();remove(victim);clearCorridor();
            for(int x=-1;x<=1;x++)for(int z=4;z<=6;z++)block(center.offset(x,3,z),Blocks.STONE);victim=cow(at.add(0,4,5));chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));double initial=chef.getY();
            for(int i=0;i<700&&!chef.isRemoved()&&chef.getY()<initial+2;i++)advance(chef,1);require(chef.getY()>=initial+2,"bounded native NPC builds/climbs a path to a higher real platform; position="+chef.position()+" velocity="+chef.getDeltaMovement()+" grounded="+chef.onGround());clear();remove(victim);
            for(int x=-3;x<=3;x++)for(int z=-6;z<=8;z++)for(int y=0;y<=7;y++)block(center.offset(x,y,z),Blocks.AIR);
        }
        void longDetour(){
            for(int x=-12;x<=12;x++)for(int y=-3;y<=12;y++)block(center.offset(x,y,0),Blocks.BEDROCK);
            var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));boolean detoured=false;
            for(int i=0;i<1000&&!chef.isRemoved()&&chef.getZ()<at.z+2;i++){advance(chef,1);if(Math.abs(chef.getX()-at.x)>12)detoured=true;}
            require(detoured&&chef.getZ()>=at.z+2,"native pursuit retains search progress across slices and goes around a long unbreakable wall");clear();remove(victim);
            for(int x=-12;x<=12;x++)for(int y=-3;y<=12;y++)block(center.offset(x,y,0),Blocks.AIR);
        }
        void voidRecovery(){
            var victim=cow(at.add(0,0,5));var chef=spawn(owner,victim);chef.setPos(at.x,owner.level().getMinY()-24,at.z);service.step(chef);
            require(!chef.isRemoved()&&owner.level().hasChunkAt(chef.blockPosition())&&owner.level().getBlockState(chef.blockPosition().below()).isSolidRender()&&chef.target==victim,"void Chef returns to real loaded solid surface retaining fixed victim");if(!blocks.containsKey(chef.blockPosition()))chef.setPos(at.add(0,0,-4));var pos=chef.position();advance(chef,30);require(chef.position().distanceToSqr(pos)>.01||victim.getHealth()<20,"after rescue live fixed-victim pursuit continues inside saved fixture");clear();remove(victim);
        }
        void guards(){
            var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);var pos=chef.position();game.items().timeStop().begin(owner.level(),40);try{advance(chef,10);require(chef.position().distanceToSqr(pos)<1e-9&&victim.getHealth()==20,"actual dimension Time Stop prevents native movement and attacks");}finally{game.items().timeStop().end(owner.level());}clear();remove(victim);
            victim=cow(at.add(0,0,6));victim.entityTags().add("strict_map_object");chef=service.spawn(owner,victim);if(chef!=null)service.step(chef);require(service.count()==0&&victim.getHealth()==20,"technical target rejected or immediately retired");remove(victim);
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);chef=service.spawn(owner,other);if(chef!=null)service.step(chef);require(service.count()==0&&other.getHealth()==20,"allied-player fixed victim rejected");
            game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);move(other,at.add(0,0,4));other.entityTags().add("sin_greed_active");chef=service.spawn(owner,other);if(chef!=null){chef.setPos(other.position().add(0,0,-1));service.step(chef);require(other.getHealth()==20,"Greed victim rejects actual nearby Chef sword attempt");}clear();other.entityTags().remove("sin_greed_active");move(other,at.add(12,0,-12));
            game.spectators().enter(owner);victim=cow(at.add(0,0,6));chef=service.spawn(owner,victim);if(chef!=null)service.step(chef);require(game.isObserver(owner)&&service.count()==0&&victim.getHealth()==20,"observer cannot own active Chef");clear();remove(victim);
        }
    }
    private record Case(String name,java.util.function.Consumer<Fixture> action){}
    public static void run(GameController game){
        var failures=new ArrayList<String>();
        for(var test:List.of(new Case("material/ownership",Fixture::nativeMaterialAndOwnership),new Case("stasis",Fixture::infiniteHealthAndStasis),
                new Case("sword",Fixture::directSword),new Case("terrain",Fixture::boundedTerrain),new Case("long detour",Fixture::longDetour),
                new Case("void",Fixture::voidRecovery),new Case("guards",Fixture::guards))){
            try(var f=new Fixture(game)){f.init();test.action.accept(f);}
            catch(RuntimeException failure){failures.add(test.name+": "+failure.getMessage());}
        }
        require(failures.isEmpty(),String.join("; ",failures));
        SkyWars2.LOGGER.info("[check] PASS chef (native Mannequin, fixed-victim melee, infiniteHP/stasis and bounded obsidian/bridge/elevation/long-detour/void boundaries; explicit ticks)");
    }
    private static Saved stopSaved;private static String stopEpoch;private static LbcWtf.Chef stopChef;private static Cow stopTarget;private static Set<Entity> stopBefore;
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null&&owner!=null&&game.canUseGameplayItems(owner)&&game.items().wtf().pendingCount()==0&&cleanAttribution(game,owner),"exclusive clean real-stop Chef probe");stopSaved=new Saved(game,owner);stopBefore=Collections.newSetFromMap(new IdentityHashMap<>());owner.level().getAllEntities().forEach(stopBefore::add);
        try{stopTarget=new Cow(EntityTypes.COW,owner.level());stopTarget.setPos(owner.position().add(0,4,0));stopTarget.setNoAi(true);stopTarget.setNoGravity(true);stopTarget.entityTags().add("time_stopped");owner.level().addFreshEntity(stopTarget);stopChef=game.items().wtf().spawn(owner,stopTarget);require(stopChef!=null&&!stopChef.isRemoved(),"native Chef staged before real match stop");stopChef.entityTags().add("time_stopped");game.items().wtf().crafted(owner);require(game.items().wtf().count()==1&&game.items().wtf().fountainCount()==1&&game.items().wtf().pendingCount()==2,"native Chef and sixty-tick crafting fountain staged before actual stop");stopEpoch=game.items().epoch();}catch(RuntimeException|Error failure){restoreStop(game);throw failure;}SkyWars2.LOGGER.info("[check] PASS chef-stop-probe");
    }
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().wtf().pendingCount()==0&&stopChef.isRemoved(),"actual match stop retires native Chef and epoch");}finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS chef-cleanup");}
    private static void restoreStop(GameController game){if(stopSaved==null)return;if(stopChef!=null)game.items().wtf().unloaded(stopChef);game.items().wtf().stop();if(stopTarget!=null){game.items().indirectKills().clear(stopTarget);stopTarget.discard();}stopSaved.restore(game);var fresh=new ArrayList<Entity>();stopSaved.player.level().getAllEntities().forEach(e->{if(!stopBefore.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);game.players().save();game.items().magic().flushCleansing();stopSaved=null;stopChef=null;stopTarget=null;stopBefore=null;stopEpoch=null;}
}
