package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Real native input/loot and Academy callbacks; successive clicks use actual server ticks. */
public final class SheepChecks {
    private SheepChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Sheep: "+why);}
    private static int flag(ItemStack stack,String key){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
    private static void flag(ItemStack stack,String key,int value){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt(key,value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    private static Object copy(Object value){if(value instanceof int[] a)return a.clone();if(value instanceof Set<?> s)return new HashSet<>(s);return value;}
    private static Map<Field,Object> fields(Object value){
        var result=new LinkedHashMap<Field,Object>();
        try{for(var f:value.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))result.put(f,copy(f.get(value)));}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}return result;
    }
    private static void restore(Object value,Map<Field,Object> saved){try{for(var e:saved.entrySet())e.getKey().set(value,copy(e.getValue()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    @SuppressWarnings("unchecked") private static Map<UUID,Long> clickTimes(LbcAcademy academy){try{var f=LbcAcademy.class.getDeclaredField("lastInteractions");f.setAccessible(true);return (Map<UUID,Long>)f.get(academy);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static boolean cleanAttribution(GameController game,ServerPlayer player){
        try{for(var f:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(f.getType())){f.setAccessible(true);if(((Map<?,?>)f.get(game.items().indirectKills())).containsKey(player))return false;}}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}return true;
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final PlayerStore.Profile profile;
        final Map<Field,Object> profileFields;final Map<String,Integer> scores=new LinkedHashMap<>();
        final List<Runnable> counters=new ArrayList<>();final boolean shift,sprint,ground;final net.minecraft.world.entity.player.Input input;final Long click;final Map<net.minecraft.world.scores.Objective,Integer> allScores=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){
            this.player=player;for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(player,objective);if(score!=null)allScores.put(objective,score.value());}state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());profileFields=fields(profile);
            shift=player.isShiftKeyDown();sprint=player.isSprinting();ground=player.onGround();input=player.getLastClientInput();click=clickTimes(game.items().academy()).get(player.getUUID());
            for(var name:List.of("effect_ghosting","void_resistance","manaregen_visual"))scores.put(name,game.items().mana().score(player,name).get());
            for(var id:BuiltInRegistries.CUSTOM_STAT)stat(Stats.CUSTOM.get(id));
            for(var item:BuiltInRegistries.ITEM)for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_DROPPED,Stats.ITEM_PICKED_UP,Stats.ITEM_CRAFTED))stat(type.get(item));
            for(var type:BuiltInRegistries.ENTITY_TYPE){stat(Stats.ENTITY_KILLED.get(type));stat(Stats.ENTITY_KILLED_BY.get(type));}
            for(var adv:game.server().getAdvancements().getAllAdvancements()){
                var completed=new HashSet<String>();player.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(completed::add);
                counters.add(()->{for(var key:adv.value().criteria().keySet())if(completed.contains(key))player.getAdvancements().award(adv,key);else player.getAdvancements().revoke(adv,key);});
            }
        }
        <T> void stat(Stat<T> stat){int value=player.getStats().getValue(stat);counters.add(()->player.getStats().setValue(player,stat,value));}
        void restore(GameController game){
            SheepChecks.restore(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);player.setSprinting(sprint);player.setOnGround(ground);player.setLastClientInput(input);
            scores.forEach((key,value)->game.items().mana().score(player,key).set(value));counters.forEach(Runnable::run);
            if(click==null)clickTimes(game.items().academy()).remove(player.getUUID());else clickTimes(game.items().academy()).put(player.getUUID(),click);
            for(var objective:game.server().getScoreboard().getObjectives())if(allScores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(player,objective,true).set(allScores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(player,objective);
            game.items().magic().inventoryChanged(player);
        }
    }
    private static final class ProbeCow extends Cow {
        Entity author; int hits;
        ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){boolean hit=super.hurtServer(level,source,amount);if(hit){author=source.getEntity();hits++;}return hit;}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game; final LbcSheep sheep; final LbcAcademy academy; final LbcMana mana;
        final ServerPlayer owner,other; final Saved saved,otherSaved; final Map<Field,Object> config;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>()),owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> preexistingForced,forced=new LinkedHashSet<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final BlockPos base;final BoundingBox bounds,holeBounds;final Vec3 at,holeAt;final int configuredCost;final String epoch;
        final net.minecraft.world.scores.PlayerTeam oldOwnerTeam,oldOtherTeam,team;
        Interaction normal,school;ItemStack trial;int phase;long last=Long.MIN_VALUE;boolean closed;
        Fixture(GameController game){
            this.game=game;sheep=game.items().sheep();academy=game.items().academy();mana=game.items().mana();epoch=game.items().epoch();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&owner.level()==other.level(),"two active same-dimension participants");
            require(game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(owner.level())&&sheep.sheepViews().isEmpty()&&sheep.holeViews().isEmpty()&&academy.ownedCount()==0&&academy.pendingCount()==0,"exclusive empty services and normal server clock");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&cleanAttribution(game,p),"closed menus and no pending attribution");
            require(game.server().getScoreboard().getPlayerTeam("sheep_fixture")==null,"exclusive fixture team");
            preexistingForced=new HashSet<>(owner.level().getForceLoadedChunks());
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());configuredCost=cost();
            oldOwnerTeam=owner.getTeam();oldOtherTeam=other.getTeam();team=game.server().getScoreboard().addPlayerTeam("sheep_fixture");team.setAllowFriendlyFire(false);
            owner.level().getAllEntities().forEach(before::add);
            base=new BlockPos((owner.blockPosition().getX()>>4)*16+2,240,(owner.blockPosition().getZ()>>4)*16+2);
            bounds=new BoundingBox(base.getX(),base.getY(),base.getZ(),base.getX()+11,base.getY()+18,base.getZ()+13);
            at=Vec3.atLowerCornerOf(base).add(6,14,6);holeAt=at.add(80,0,0);
            var h=BlockPos.containing(holeAt);holeBounds=new BoundingBox(h.getX()-32,h.getY()-32,h.getZ()-32,h.getX()+32,h.getY()+32,h.getZ()+32);
        }
        int cost(){return mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("sheep_wand_mana_use"),"lbc.math").get();}
        void resident(BoundingBox area){
            for(int x=area.minX()>>4;x<=area.maxX()>>4;x++)for(int z=area.minZ()>>4;z<=area.maxZ()>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!preexistingForced.contains(key)&&forced.add(key))owner.level().setChunkForced(x,z,true);
                var chunk=owner.level().getChunkSource().getChunkNow(x,z);if(chunk==null)chunk=owner.level().getChunk(x,z);require(chunk!=null,"bounded fixture column admission");
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->area.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->area.isInside(t.pos())).forEach(fluidTicks::add);
            }
        }
        void initialize(){
            require(holeBounds.maxY()<owner.level().getMaxY(),"hole sphere below native ceiling");
            require(before.stream().noneMatch(e->!(e instanceof ServerPlayer)&&(e.position().distanceToSqr(at)<55*55||e.position().distanceToSqr(holeAt)<55*55)),"isolated school and destructive-hole regions exclude unrelated entities");
            resident(bounds);resident(holeBounds);
            for(var p:BlockPos.betweenClosed(base,base.offset(11,18,13)))blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));
            for(var p:BlockPos.betweenClosed(new BlockPos(holeBounds.minX(),holeBounds.minY(),holeBounds.minZ()),new BlockPos(holeBounds.maxX(),holeBounds.maxY(),holeBounds.maxZ())))require(owner.level().getBlockState(p).isAir(),"destructive fixture requires empty air sphere and margin at "+p);
            game.config().offhandWands=true;game.config().ranked=false;game.config().lbcSkills=false;mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("sheep_wand_mana_use"),"lbc.math").set(500);
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(p.getMaxHealth());p.setNoGravity(true);p.setShiftKeyDown(false);p.setSprinting(false);p.setOnGround(false);p.removeAllEffects();p.entityTags().removeAll(Set.of("sin_greed_active","time_stopped"));
                mana.noMagic(p,1000);mana.set(p,100000);mana.score(p,"void_resistance").set(0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);
            }
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);move(owner,at);move(other,at.add(8,0,0));game.items().magic().flushCleansing();
            normal=new Interaction(EntityTypes.INTERACTION,owner.level());normal.setPos(at.add(0,-12,1));normal.entityTags().add("magic_academy");add(normal);
            var source=Vec3.atLowerCornerOf(base.offset(6,1,6)).add(.25,.25,.75);var drop=new ItemEntity(owner.level(),source.x,source.y,source.z,loot("academy_token"));drop.setThrower(owner);drop.setOnGround(true);add(drop);academy.tick();
            require(drop.isRemoved()&&academy.pendingCount()==1,"real grounded token starts Academy placement");for(int i=0;i<20&&academy.pendingCount()>0;i++)academy.tick();require(academy.pendingCount()==0,"native school completes");
            for(var e:owner.level().getAllEntities())if(!before.contains(e)&&!owned.contains(e)){owned.add(e);if(e instanceof Interaction interaction&&e.entityTags().contains("magic_academy"))school=interaction;}
            require(school!=null,"real token school interaction");presentation();
        }
        void add(Entity e){require(owner.level().addFreshEntity(e),"native entity admission");owned.add(e);}
        void move(ServerPlayer player,Vec3 p){player.teleportTo(player.level(),p.x,p.y,p.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(String leaf){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(result.size()==1&&result.getFirst().getCount()==1,"singleton native loot "+leaf);return result.getFirst();}
        void held(ItemStack stack,InteractionHand hand){owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,stack);owner.setShiftKeyDown(false);}
        boolean input(InteractionHand hand){return owner.gameMode.useItem(owner,owner.level(),owner.getItemInHand(hand),hand).consumesAction();}
        ProbeCow cow(Vec3 p){var c=new ProbeCow(owner.level());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);add(c);return c;}
        void clear(){sheep.forget(owner.getUUID());sheep.forget(other.getUUID());}
        Entity sheepEntity(){require(sheep.sheepViews().size()==1,"exactly one replacement sheep");var e=owner.level().getEntity(sheep.sheepViews().getFirst().entity());require(e!=null,"resident replacement");owned.add(e);return e;}
        ItemStack potion(){var p=new ItemStack(Items.POTION);for(var key:List.of("transformable","potion","succubus"))flag(p,key,1);return p;}
        void presentation(){
            var a=loot("sheep_wand");var b=loot("academy_sheep_wand");require(a.is(LbcSheep.ITEM)&&b.is(LbcSheep.ACADEMY_ITEM)&&LbcSheep.isWand(a)&&LbcSheep.isAcademy(b)&&!LbcSheep.isAcademy(a),"native ordinary and Academy identities");
            for(var key:List.of("transformable","staff","raycastspell","sheepwand"))require(flag(a,key)==1&&flag(b,key)==1,"authored flag "+key);
            require(Identifier.parse("lbc:sheepwand").equals(a.get(DataComponents.ITEM_MODEL))&&a.get(DataComponents.ITEM_MODEL).equals(b.get(DataComponents.ITEM_MODEL)),"authored model");
            require(Identifier.parse("lbc:tier_a").equals(a.get(DataComponents.TOOLTIP_STYLE))&&Identifier.parse("lbc:tier_x").equals(b.get(DataComponents.TOOLTIP_STYLE)),"source A/X tiers");
            require(a.get(DataComponents.LORE).lines().size()==6&&b.get(DataComponents.LORE).lines().size()==7&&b.get(DataComponents.LORE).lines().subList(0,6).equals(a.get(DataComponents.LORE).lines()),"six source lore rows plus upgrade marker");
        }
        void guardsAndRay(){
            for(var hand:InteractionHand.values())for(boolean upgraded:new boolean[]{false,true}){
                var stack=loot(upgraded?"academy_sheep_wand":"sheep_wand");held(stack,hand);mana.set(owner,499);owner.setShiftKeyDown(true);
                require(!input(hand)&&mana.get(owner)==499&&flag(stack,"mode")== (upgraded?1:0),"no free mode selector or sub500 admission "+hand+"/"+upgraded);
                mana.set(owner,500);owner.entityTags().add("time_stopped");try{require(!input(hand)&&mana.get(owner)==500,"personal Time Stop cast guard");}finally{owner.entityTags().remove("time_stopped");}
                if(hand==InteractionHand.OFF_HAND){game.config().offhandWands=false;require(!input(hand)&&mana.get(owner)==500,"offhand configuration guard");game.config().offhandWands=true;}
                owner.setShiftKeyDown(false);require(input(hand)&&mana.get(owner)==0,"actual native used-hand500 debit "+hand+"/"+upgraded);
            }
            held(loot("sheep_wand"),InteractionHand.MAIN_HAND);mana.set(owner,500);game.items().timeStop().begin(owner.level(),40);
            try{require(!input(InteractionHand.MAIN_HAND)&&mana.get(owner)==500,"dimension Time Stop cast admission");}finally{game.items().timeStop().end(owner.level());}
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");add(marker);game.items().magic().loaded(marker);
            try{require(!input(InteractionHand.MAIN_HAND)&&mana.get(owner)==500,"real antimagic field cast admission");}finally{game.items().magic().unloaded(marker);marker.discard();game.items().magic().flushCleansing();}
            var first=cow(at.add(0,.5,4));var second=cow(at.add(0,.5,8));var sentinel=new ItemEntity(owner.level(),at.x,at.y+1.5,at.z+2,new ItemStack(Items.DIAMOND));sentinel.setNoGravity(true);add(sentinel);
            other.getInventory().setItem(2,new ItemStack(Items.EMERALD,7));move(other,at.add(0,0,2));mana.set(owner,500);owner.setYRot(0);owner.setXRot(0);
            require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&game.items().polymorph().active(first)&&!second.isRemoved(),"first native impact terminates once");
            var replacement=sheepEntity();require(replacement instanceof net.minecraft.world.entity.animal.sheep.Sheep,"actual vanilla sheep replacement");
            var living=(net.minecraft.world.entity.LivingEntity)replacement;
            require(living.getHealth()==8&&living.getMaxHealth()==8&&replacement.getCustomName()!=null&&replacement.getCustomName().getString().equals("jeb_")&&living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).getBaseValue()==20,"source jeb_ health8 and resistance20");
            require(sentinel.isAlive()&&other.isAlive()&&other.getInventory().getItem(2).is(Items.EMERALD)&&other.getInventory().getItem(2).getCount()==7,"player exclusion preserves hotbar and ordinary item selector exclusion");
            require(owner.level().getEntities(owner,new net.minecraft.world.phys.AABB(first.position(),first.position()).inflate(2),e->e instanceof ItemEntity&&!owned.contains(e)).isEmpty(),"original replacement discards without original death loot");
            int remaining=game.items().polymorph().remaining(first);for(int i=0;i<240;i++)sheep.tick();require(!replacement.isRemoved()&&second.isAlive()&&game.items().polymorph().remaining(first)==remaining,"reentrant non-clock calls cannot expire temporary polymorph");
            clear();first.discard();second.discard();sentinel.discard();move(other,at.add(8,0,0));
        }
        void potionTrial(){
            move(owner,holeAt.add(0,0,-10));move(other,holeAt.add(12,0,0));owner.setYRot(0);owner.setXRot(0);
            held(loot("sheep_wand"),InteractionHand.MAIN_HAND);trial=owner.getMainHandItem();require(academy.interact(owner,InteractionHand.MAIN_HAND,normal)&&LbcAcademy.trial(trial),"real normal Academy assigns Sheep trial");held(trial,InteractionHand.OFF_HAND);
            var p=holeAt.add(0,1.5,-5);var malformed=potion();flag(malformed,"succubus",0);var wrong=new ItemEntity(owner.level(),p.x,p.y,p.z,malformed);wrong.setNoGravity(true);add(wrong);
            mana.set(owner,500);require(input(InteractionHand.OFF_HAND)&&wrong.isAlive()&&sheep.holeViews().isEmpty()&&flag(trial,"upgrade_available")==0,"exact potion flags required and items are not sheep targets");wrong.discard();
            var drop=new ItemEntity(owner.level(),p.x,p.y,p.z,potion());drop.setNoGravity(true);add(drop);var debris=new ItemEntity(owner.level(),p.x+3,p.y,p.z,new ItemStack(Items.DIAMOND));debris.setNoGravity(true);add(debris);
            mana.set(owner,500);require(input(InteractionHand.OFF_HAND)&&drop.isRemoved()&&debris.isRemoved()&&sheep.holeViews().size()==1&&flag(trial,"upgrade_available")==1&&flag(trial,"upgradable")==0,"native dropped-potion hole clears all R5 items and readies actual used offhand stack");
            clear();
            held(loot("sheep_wand"),InteractionHand.MAIN_HAND);move(other,holeAt.add(0,0,-5));other.getInventory().setItem(5,potion());owner.getInventory().setItem(5,potion());mana.set(owner,500);
            other.entityTags().add("sin_greed_active");
            try{require(input(InteractionHand.MAIN_HAND)&&sheep.holeViews().isEmpty()&&!other.getInventory().getItem(5).isEmpty()&&!owner.getInventory().getItem(5).isEmpty(),"nearby active Greed cannot trigger or lose its potion through an area inventory scan");}finally{other.entityTags().remove("sin_greed_active");}
            game.config().lbcSkills=true;game.players().get(other.getUUID()).lbcSkill=2;
            var protectedPotion=other.getInventory().getItem(5);protectedPotion.setCount(3);flag(protectedPotion,"inventory_probe",71);var exactPotion=protectedPotion.copy();
            require(!sheep.consumePotion(owner,InteractionHand.MAIN_HAND,owner.getMainHandItem(),other.position())&&sheep.holeViews().isEmpty()
                    &&other.getInventory().getItem(5)==protectedPotion&&ItemStack.matches(protectedPotion,exactPotion),"protected inventory cannot trigger a hole and preserves exact potion components/count/reference");
            var guardedDrop=new ItemEntity(owner.level(),other.getX(),other.getY(),other.getZ(),potion());guardedDrop.setNoGravity(true);add(guardedDrop);
            require(sheep.consumePotion(owner,InteractionHand.MAIN_HAND,owner.getMainHandItem(),other.position())&&guardedDrop.isRemoved()&&sheep.holeViews().size()==1
                    &&ItemStack.matches(protectedPotion,exactPotion),"protected holder leaves dropped-potion trigger and cleanup unchanged");clear();
            game.players().get(other.getUUID()).lbcSkill=0;game.players().get(owner.getUUID()).lbcSkill=2;move(owner,other.position().add(3,0,0));
            var selfPotion=owner.getInventory().getItem(5);var exactSelf=selfPotion.copy();
            require(sheep.consumePotion(owner,InteractionHand.MAIN_HAND,owner.getMainHandItem(),other.position())&&sheep.holeViews().size()==1&&other.getInventory().getItem(5).isEmpty()
                    &&owner.getInventory().getItem(5)==selfPotion&&ItemStack.matches(selfPotion,exactSelf),"unprotected holder still triggers/consumes while protected caster survives radius5 inventory consumption");clear();
            game.config().lbcSkills=false;game.players().get(owner.getUUID()).lbcSkill=0;other.getInventory().setItem(5,potion());move(owner,holeAt.add(0,0,-10));
            mana.set(owner,500);
            require(input(InteractionHand.MAIN_HAND)&&sheep.holeViews().size()==1&&other.getInventory().getItem(5).isEmpty()&&owner.getInventory().getItem(5).isEmpty()&&other.isAlive(),"inventory potion creates one hole and clears matching potions from nearby players without transformation");
            clear();move(owner,at);move(other,at.add(8,0,0));
        }
        void chefPolymorph(){
            require(game.items().wtf().count()==0,"no unrelated Chef before polymorph regression");move(owner,at);move(other,at.add(8,0,0));
            for(boolean academy:new boolean[]{false,true})for(boolean knocked:new boolean[]{false,true}){
                var victim=cow(at.add(8,0,10));var chef=game.items().wtf().spawn(owner,victim);require(chef!=null,"actual owned native Chef");owned.add(chef);
                if(knocked)chef.receive(20);chef.setPos(at.add(0,knocked?1.4:.5,4));chef.setDeltaMovement(Vec3.ZERO);
                held(loot(academy?"academy_sheep_wand":"sheep_wand"),InteractionHand.MAIN_HAND);owner.setYRot(0);owner.setXRot(0);mana.set(owner,500);
                require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&(academy?chef.isRemoved():game.items().polymorph().active(chef))&&sheep.sheepViews().size()==1,"actual ray transforms awake/knocked allied Chef: academy="+academy+" knocked="+knocked);
                clear();game.items().wtf().erase(chef);require(game.items().wtf().count()==0&&game.items().wtf().pinnedChunks()==0,"restored and permanent Chef fixture cleanup");victim.discard();
            }
        }
        void promote(){held(trial,InteractionHand.MAIN_HAND);require(academy.interact(owner,InteractionHand.MAIN_HAND,school),"real token school promotion callback on subsequent server tick");require(owner.getMainHandItem().is(LbcSheep.ACADEMY_ITEM)&&Identifier.parse("lbc:tier_x").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE))&&school.isRemoved(),"real X-tier Academy promotion consumes token school");}
        void academySex(){
            move(owner,holeAt.add(0,0,-10));move(other,at);
            var male=cow(holeAt);male.entityTags().add("male");sheep.transform(owner,male,true);require(male.isRemoved()&&sheep.holeViews().size()==1&&sheep.sheepViews().size()==1,"Academy preassigned male creates unconditional hole and permanent sheep: removed="+male.isRemoved()+", holes="+sheep.holeViews().size()+", sheep="+sheep.sheepViews().size()+", ownerHP="+owner.getHealth()+", playing="+game.playing()+", canUse="+game.canUseGameplayItems(owner)+", stage="+game.stage()+", levelResident="+(owner.level().getChunkSource().getChunkNow(male.blockPosition().getX()>>4,male.blockPosition().getZ()>>4)!=null)+", entityResident="+owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(male.blockPosition().getX()>>4,male.blockPosition().getZ()>>4))+", owner="+owner.position()+", male="+male.position());clear();
            var female=cow(holeAt);female.entityTags().add("female");sheep.transform(owner,female,true);require(female.isRemoved()&&sheep.holeViews().isEmpty()&&sheep.sheepViews().size()==1,"Academy preassigned female preserves assignment without male hole");clear();
            var guarded=cow(holeAt);guarded.entityTags().add("strict_map_object");sheep.transform(owner,guarded,true);require(!guarded.isRemoved()&&sheep.sheepViews().isEmpty()&&sheep.holeViews().isEmpty(),"technical-role transformation guard");guarded.discard();
            int males=0,females=0;for(int i=0;i<48;i++){var unknown=cow(holeAt);sheep.transform(owner,unknown,true);boolean m=unknown.entityTags().contains("male"),f=unknown.entityTags().contains("female");require(m!=f,"unassigned creature receives exactly one sex");if(m)males++;else females++;require(sheep.holeViews().size()==(m?1:0),"only assigned male creates hole");clear();}require(males>0&&females>0,"source random sex branch reaches both assignments across48creatures");
            move(owner,at);move(other,at.add(8,0,0));
        }
        void place(BlockPos p,Block block){blocks.putIfAbsent(p.immutable(),game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,block.defaultBlockState(),2);}
        void physics(){
            move(owner,holeAt.add(0,0,-40));move(other,holeAt.add(28,0,0));
            var center=BlockPos.containing(holeAt);place(center,Blocks.OBSIDIAN);place(center.offset(28,0,0),Blocks.STONE);
            var noisyCells=new ArrayList<BlockPos>();
            for(int x=8;x<=22;x+=2)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
                var cell=center.offset(x,y,z);place(cell,Blocks.STONE);noisyCells.add(cell);
            }
            var nativeHole=sheep.spawnHole(owner,holeAt);require(sheep.holeViews().size()==1,"native sheep hole admission");
            var view=sheep.holeViews().getFirst();require(view.disc()&&Math.abs(view.radius()-Math.sqrt(552))<1e-6,"disc shader radius matches destructive source sphere");require(nativeHole.visual!=null&&nativeHole.visual.disc&&Math.abs(nativeHole.visual.radius*14-LbcSheep.RADIUS)<1e-5,"actual shared shader handle has matching radius and large-disc selection");
            var near=cow(holeAt.add(12,0,0));var far=cow(holeAt.add(28,0,0));var damage=cow(holeAt.add(0,0,2));var role=cow(holeAt.add(1,0,0));role.entityTags().add("strict_map_object");var greed=cow(holeAt.add(-1,0,0));greed.entityTags().add("sin_greed_active");
            var resistant=cow(holeAt.add(0,0,-2));mana.score(resistant,"void_resistance").set(1);
            other.setLastClientInput(new net.minecraft.world.entity.player.Input(true,false,false,false,false,false,false));
            other.setDeltaMovement(0,0,.35);
            var outerChunk=BlockPos.containing(far.position());
            boolean resident=owner.level().getChunkSource().getChunkNow(outerChunk.getX()>>4,outerChunk.getZ()>>4)!=null;
            boolean entityResident=owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(outerChunk.getX()>>4,outerChunk.getZ()>>4));
            boolean queried=owner.level().getEntities((Entity)null,new net.minecraft.world.phys.AABB(holeAt,holeAt).inflate(LbcSheep.INFLUENCE),e->e==far).contains(far);
            require(resident&&queried,"outer pull fixture resident/query admission: block="+resident+", entities="+entityResident+", queried="+queried+", cow="+far.position()+", player="+other.position());
            sheep.tick();
            require(damage.getHealth()==0&&damage.getKillCredit()==owner&&damage.hits==0
                    &&damage.getLastDamageSource()!=null&&damage.getLastDamageSource().getEntity()==owner,
                    "source10 outside_border direct HP death keeps author without native hurt");
            require(role.getHealth()==10&&greed.getHealth()==10&&resistant.getHealth()==10,"technical role, Greed and void resistance preserve HP");
            require(far.getDeltaMovement().x<0,"outer creature receives weak inward pull: far="+far.getDeltaMovement()+", position="+far.position()+", resident="+resident+", entityResident="+entityResident);
            require(near.getDeltaMovement().x<far.getDeltaMovement().x,"inner pull stronger than extended pull: near="+near.getDeltaMovement()+", far="+far.getDeltaMovement()+", hole="+nativeHole.point);
            require(other.getLastClientInput().forward()&&other.getDeltaMovement().equals(new Vec3(0,0,.35)),"actual forward input preserves outer player motion: input="+other.getLastClientInput()+", velocity="+other.getDeltaMovement()+", position="+other.position());
            move(owner,holeAt.add(2,-2,0));move(other,holeAt.add(-2,-2,0));for(var p:List.of(owner,other)){p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);p.damageCooldownTime=0;p.setInvulnerableTime(0);}float ownerHp=owner.getHealth(),otherHp=other.getHealth();sheep.physics(nativeHole,owner);require(owner.getHealth()==ownerHp-10&&other.getHealth()==otherHp-10,"source WILD hole damages caster and teammate for10: owner="+owner.getHealth()+" before="+ownerHp+", teammate="+other.getHealth()+" before="+otherHp);
            require(owner.getDeltaMovement().y>owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY)&&other.getDeltaMovement().y>other.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY),"strong inner pull overcomes gravity and lifts both players despite forward input: owner="+owner.getDeltaMovement()+", teammate="+other.getDeltaMovement());
            move(owner,holeAt.add(0,0,-40));move(other,holeAt.add(28,0,0));
            for(int i=0;i<48;i++)sheep.tick();
            long eroded=noisyCells.stream().filter(cell->owner.level().getBlockState(cell).isAir()).count();
            require(owner.level().getBlockState(center).isAir()&&eroded>0&&eroded<noisyCells.size()&&owner.level().getBlockState(center.offset(28,0,0)).is(Blocks.STONE),"native sheep hole clears its obsidian core and leaves dithered shell remnants while preserving exterior");
            var normal=nativeHole.tilt.transform(new org.joml.Vector3f(0,1,0));require(normal.y<1&&normal.y>=Math.cos(Math.toRadians(18.01)),"actual disc tilts toward motion within18degrees");
            for(var e:owner.level().getEntities((Entity)null,new net.minecraft.world.phys.AABB(damage.position(),damage.position()).inflate(3),e->!before.contains(e)&&(e instanceof ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb)))owned.add(e);
            var retained=sheep.holeViews().getFirst();game.items().timeStop().begin(owner.level(),40);
            try{sheep.tick();var frozen=sheep.holeViews().getFirst();require(frozen.point().equals(retained.point())&&frozen.velocity().equals(retained.velocity()),"Time Stop retains hole with unchanged point and velocity");}finally{game.items().timeStop().end(owner.level());}
            for(var e:List.of(near,far,damage,role,greed,resistant)){game.items().indirectKills().clear(e);e.discard();}mana.score(resistant,"void_resistance").set(0);
            clear();require(sheep.holeViews().isEmpty(),"owner retirement closes hole");
            move(owner,at);move(other,at.add(8,0,0));
        }
        boolean step(){
            require(game.playing()&&game.items().currentEpoch(epoch),"active match epoch");long now=owner.level().getGameTime();if(now==last)return false;last=now;
            switch(phase++){case 0->{guardsAndRay();chefPolymorph();}case 1->potionTrial();case 2->promote();case 3->academySex();case 4->{physics();return true;}default->throw new IllegalStateException("unexpected phase");}return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;try{owner.stopUsingItem();game.items().timeStop().end(owner.level());clear();academy.stop();
            for(var e:owned){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);if(!game.items().wtf().erase(e))e.discard();}
            for(var p:List.of(owner,other))game.items().indirectKills().clear(p);
            for(var area:List.of(bounds,holeBounds)){owner.level().getBlockTicks().clearArea(area);owner.level().getFluidTicks().clearArea(area);}
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            for(var area:List.of(bounds,holeBounds)){owner.level().getBlockTicks().clearArea(area);owner.level().getFluidTicks().clearArea(area);}blockTicks.forEach(owner.level().getBlockTicks()::schedule);fluidTicks.forEach(owner.level().getFluidTicks()::schedule);
            restore(game.config(),config);mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("sheep_wand_mana_use"),"lbc.math").set(configuredCost);
            var board=game.server().getScoreboard();for(var p:List.of(owner,other))if(p.getTeam()==team)board.removePlayerFromTeam(p.getScoreboardName(),team);if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldOtherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOtherTeam);board.removePlayerTeam(team);
            saved.restore(game);otherSaved.restore(game);game.players().save();game.items().magic().flushCleansing();
            }finally{for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);forced.clear();}
        }
    }
    public static void run(GameController game){require(running==null,"one fixture at a time");running=new Fixture(game);try{running.initialize();}catch(RuntimeException|Error error){try{running.close();}finally{running=null;}throw error;}}
    public static void tick(GameController game){if(running==null)return;var fixture=running;try{if(fixture.step()){fixture.close();running=null;SkyWars2.LOGGER.info("[check] PASS sheep");}}catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL sheep",error);}}
    private static List<Entity> stopActors;private static String stopEpoch;
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var sheep=game.items().sheep();
        require(running==null&&stopActors==null&&owner!=null&&game.canUseGameplayItems(owner)&&sheep.sheepViews().isEmpty()&&sheep.holeViews().isEmpty(),"exclusive retained stop probe");
        var saved=new Saved(game,owner);var actors=new ArrayList<Entity>();
        try{
            // Freeze native hole immediately, before any service tick can mutate terrain.
            var c=new Cow(EntityTypes.COW,owner.level());c.setPos(owner.position().add(0,10,0));c.setNoAi(true);c.setNoGravity(true);require(owner.level().addFreshEntity(c),"retained probe cow");actors.add(c);sheep.transform(owner,c,false);
            require(sheep.sheepViews().size()==1,"native retained replacement exists");var replacement=owner.level().getEntity(sheep.sheepViews().getFirst().entity());require(replacement!=null,"retained replacement resident");actors.add(replacement);replacement.entityTags().add("time_stopped");var hole=sheep.spawnHole(owner,owner.position().add(0,12,0));require(hole!=null&&hole.terrain==null,"real retained hole created before terrain work");hole.entity.entityTags().add("time_stopped");actors.add(hole.entity);stopActors=actors;stopEpoch=game.items().epoch();
        }catch(RuntimeException|Error error){sheep.forget(owner.getUUID());actors.forEach(Entity::discard);throw error;}finally{saved.restore(game);game.items().magic().flushCleansing();}
        SkyWars2.LOGGER.info("[check] PASS sheep-stop-probe");
    }
    public static void cleanup(GameController game){var actors=stopActors;require(actors!=null,"retained sheep stop probe exists");try{require(!game.items().currentEpoch(stopEpoch)&&game.items().sheep().sheepViews().isEmpty()&&game.items().sheep().holeViews().isEmpty()&&actors.stream().allMatch(Entity::isRemoved),"actual match stop invalidates epoch and removes retained permanent sheep");}finally{actors.forEach(Entity::discard);stopActors=null;stopEpoch=null;}SkyWars2.LOGGER.info("[check] PASS sheep-cleanup");}
}
