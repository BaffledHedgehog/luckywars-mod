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
public final class SoullessAcademyChecks {
    private SoullessAcademyChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Soulless Academy: "+why);}
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
        final List<Runnable> counters=new ArrayList<>();final boolean shift,sprint,ground;final Long click;
        Saved(GameController game,ServerPlayer player){
            this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());profileFields=fields(profile);
            shift=player.isShiftKeyDown();sprint=player.isSprinting();ground=player.onGround();click=clickTimes(game.items().academy()).get(player.getUUID());
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
            SoullessAcademyChecks.restore(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);player.setSprinting(sprint);player.setOnGround(ground);
            scores.forEach((key,value)->game.items().mana().score(player,key).set(value));counters.forEach(Runnable::run);
            if(click==null)clickTimes(game.items().academy()).remove(player.getUUID());else clickTimes(game.items().academy()).put(player.getUUID(),click);
            game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcSoulless souls;final LbcAcademy academy;final LbcMana mana;
        final ServerPlayer owner,other;final Saved saved,otherSaved;final Map<Field,Object> config;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>()),owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final BlockPos base;final BoundingBox bounds;final Vec3 at;final int configuredCost;final String epoch;
        final net.minecraft.world.scores.PlayerTeam oldOwnerTeam,oldOtherTeam,team;
        Interaction normal,school;ItemStack trial;int phase;long last=Long.MIN_VALUE;boolean closed;
        Fixture(GameController game){
            this.game=game;souls=game.items().soulless();academy=game.items().academy();mana=game.items().mana();epoch=game.items().epoch();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&owner.level()==other.level(),"two active same-dimension participants");
            require(game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(owner.level())&&souls.count()==0&&souls.ghostCount()==0&&souls.vexSnapshot().isEmpty()&&academy.ownedCount()==0&&academy.pendingCount()==0,"exclusive empty native services and normal clock");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&cleanAttribution(game,p)&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0,"closed menus and no existing frost/combat");
            require(game.server().getScoreboard().getPlayerTeam("soul_academy_fixture")==null,"exclusive fixture team");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());configuredCost=cost();
            oldOwnerTeam=owner.getTeam();oldOtherTeam=other.getTeam();team=game.server().getScoreboard().addPlayerTeam("soul_academy_fixture");team.setAllowFriendlyFire(false);
            owner.level().getAllEntities().forEach(before::add);
            base=new BlockPos((owner.blockPosition().getX()>>4)*16+2,240,(owner.blockPosition().getZ()>>4)*16+2);
            bounds=new BoundingBox(base.getX(),base.getY(),base.getZ(),base.getX()+11,base.getY()+10,base.getZ()+11);at=Vec3.atLowerCornerOf(base).add(6,2,6);
        }
        int cost(){return mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("soulcorruption_wand_mana_use"),"lbc.math").get();}
        void initialize(){
            require(base.getY()+11<owner.level().getMaxY(),"school below native ceiling");
            require(before.stream().noneMatch(e->!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<32*32),"fixture sphere has no unrelated entities");
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){
                var chunk=owner.level().getChunkSource().getChunkNow(x,z);require(chunk!=null&&owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)),"resident school columns");
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            }
            for(var p:BlockPos.betweenClosed(base,base.offset(11,10,11)))blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));
            game.config().offhandWands=true;game.config().ranked=false;game.config().lbcSkills=false;
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("soulcorruption_wand_mana_use"),"lbc.math").set(1500);
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.setSprinting(false);p.setOnGround(false);p.removeAllEffects();p.entityTags().removeAll(Set.of("sin_greed_active","time_stopped"));
                mana.noMagic(p,0);mana.set(p,100000);mana.score(p,"effect_ghosting").set(0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);
            }
            move(owner,at);move(other,at.add(0,0,3));game.items().magic().flushCleansing();
            normal=new Interaction(EntityTypes.INTERACTION,owner.level());normal.setPos(at.add(0,0,1));normal.entityTags().add("magic_academy");add(normal);
            var loot=loot("academy_token");var source=Vec3.atLowerCornerOf(base.offset(6,1,6)).add(.25,.25,.75);
            var drop=new ItemEntity(owner.level(),source.x,source.y,source.z,loot);drop.setThrower(owner);drop.setOnGround(true);add(drop);academy.tick();
            require(drop.isRemoved()&&academy.pendingCount()==1,"real grounded token starts Academy placement");
            for(int i=0;i<20&&academy.pendingCount()>0;i++)academy.tick();require(academy.pendingCount()==0,"native school completed");
            for(var e:owner.level().getAllEntities())if(!before.contains(e)&&e!=normal){owned.add(e);if(e instanceof Interaction interaction&&e.entityTags().contains("magic_academy"))school=interaction;}
            require(school!=null&&academy.ownedCount()>0,"real token school Interaction");
            presentation();inputModes();modeOne();generationGuards();
        }
        void add(Entity e){require(owner.level().addFreshEntity(e),"native fixture entity admission");owned.add(e);}
        void move(ServerPlayer player,Vec3 pos){player.teleportTo(player.level(),pos.x,pos.y,pos.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(String leaf){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(result.size()==1&&result.getFirst().getCount()==1,"real singleton loot "+leaf);return result.getFirst();}
        void held(ItemStack stack,InteractionHand hand){owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,stack);owner.setShiftKeyDown(false);}
        boolean input(InteractionHand hand){return owner.gameMode.useItem(owner,owner.level(),owner.getItemInHand(hand),hand).consumesAction();}
        List<ItemEntity> rewards(){var result=new ArrayList<ItemEntity>();for(var e:owner.level().getAllEntities())if(!before.contains(e)&&e instanceof ItemEntity drop&&!drop.isRemoved()&&LbcSoulless.material(drop.getItem())>=1){owned.add(drop);result.add(drop);}return result;}
        void clearRewards(){rewards().forEach(Entity::discard);}
        void clearSouls(){souls.forget(owner);souls.forget(other);for(var e:owned)if(e instanceof net.minecraft.world.entity.LivingEntity living)souls.unloaded(living);}
        Cow cow(Vec3 pos){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(pos);c.setNoAi(true);c.setNoGravity(true);add(c);return c;}
        void presentation(){
            var stack=loot("academy_soulcorruption_wand");require(stack.is(LbcSoulless.ACADEMY_WAND)&&LbcSoulless.isWand(stack)&&LbcSoulless.isAcademy(stack)&&stack.getItem() instanceof eu.pb4.polymer.core.api.item.SimplePolymerItem,"native Academy class/identity");
            for(var key:List.of("craftusable","transformable","staff","magicwand","soulcorrupter","upgraded"))require(flag(stack,key)==1,"source flag "+key);
            require(flag(stack,"mode")==1&&Identifier.parse("lbc:soulcorruptwand").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source default mode/model/A tier");
            require(stack.get(DataComponents.LORE)!=null&&stack.get(DataComponents.LORE).lines().size()==15,"14 authored source rows plus Academy upgrade marker");
            require(!LbcSoulless.isAcademy(loot("soulcorruption_wand")),"ordinary prerequisite remains ordinary");
        }
        void inputModes(){
            for(var hand:InteractionHand.values())for(boolean legacy:new boolean[]{false,true}){
                var stack=loot("academy_soulcorruption_wand");if(legacy)stack=stack.transmuteCopy(Items.CARROT_ON_A_STICK);held(stack,hand);mana.set(owner,0);owner.setShiftKeyDown(true);
                require((legacy?game.items().wands().use(owner,hand):input(hand))&&flag(stack,"mode")==2&&mana.get(owner)==0,"free zero-mana mode2 actual used hand/native-or-legacy "+hand+"/"+legacy);
                require((legacy?game.items().wands().use(owner,hand):input(hand))&&flag(stack,"mode")==1&&mana.get(owner)==0,"two-mode wrap "+hand+"/"+legacy);
                owner.setShiftKeyDown(false);require(!game.items().wands().use(owner,hand)&&mana.get(owner)==0,"mode1 no mana admission "+hand);
            }
            var main=loot("academy_soulcorruption_wand");var off=loot("academy_soulcorruption_wand");held(main,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,off);owner.setShiftKeyDown(true);mana.set(owner,0);
            require(input(InteractionHand.MAIN_HAND)&&flag(main,"mode")==2&&flag(off,"mode")==2,"actual two-native-hand mode selection");
            game.config().offhandWands=false;require(input(InteractionHand.MAIN_HAND)&&flag(main,"mode")==1&&flag(off,"mode")==2,"offhand disabled preserves its independent selector");
            require(!input(InteractionHand.OFF_HAND)&&flag(off,"mode")==2,"native offhand disabled admission");game.config().offhandWands=true;
        }
        void modeOne(){
            held(loot("academy_soulcorruption_wand"),InteractionHand.OFF_HAND);mana.set(owner,3000);other.entityTags().add("sin_greed_active");
            var role=cow(at.add(2,0,3));role.entityTags().add("strict_map_object");var target=cow(at.add(-2,0,3));
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            require(input(InteractionHand.OFF_HAND)&&mana.get(owner)==1500&&mana.score(owner,"effect_ghosting").get()==5&&mana.score(target,"effect_ghosting").get()==5&&mana.score(other,"effect_ghosting").get()==0&&mana.score(role,"effect_ghosting").get()==0,"Academy mode1 shared WILD ghosting, Greed and technical-role guards");
            other.entityTags().remove("sin_greed_active");require(input(InteractionHand.OFF_HAND)&&mana.get(owner)==0&&mana.score(owner,"effect_ghosting").get()==10&&mana.score(other,"effect_ghosting").get()==5,"mode1 additive duration and teammate inclusion");
            clearSouls();role.discard();target.discard();
        }
        void generationGuards(){
            var stack=loot("academy_soulcorruption_wand");flag(stack,"mode",2);held(stack,InteractionHand.MAIN_HAND);mana.set(owner,10000);
            int before=mana.get(owner);mana.noMagic(owner,1);require(!input(InteractionHand.MAIN_HAND)&&rewards().isEmpty()&&mana.get(owner)==before,"personal antimagic blocks generation");mana.noMagic(owner,0);
            owner.entityTags().add("time_stopped");require(!input(InteractionHand.MAIN_HAND)&&rewards().isEmpty(),"manual Time Stop blocks generation");owner.entityTags().remove("time_stopped");
            game.items().timeStop().begin(owner.level(),40);try{require(!input(InteractionHand.MAIN_HAND)&&rewards().isEmpty()&&mana.get(owner)==before,"dimension Time Stop blocks generation");}finally{game.items().timeStop().end(owner.level());}
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");add(marker);game.items().magic().loaded(marker);
            try{require(!input(InteractionHand.MAIN_HAND)&&rewards().isEmpty()&&mana.get(owner)==before,"real antimagic field blocks generation");}finally{game.items().magic().unloaded(marker);marker.discard();game.items().magic().flushCleansing();}
        }
        boolean step(){
            require(game.playing()&&game.items().currentEpoch(epoch),"match and fixture epoch remain active");long now=owner.level().getGameTime();if(now==last)return false;last=now;
            switch(phase++){
                case 0->{held(loot("soulcorruption_wand"),InteractionHand.MAIN_HAND);trial=owner.getMainHandItem();require(academy.interact(owner,InteractionHand.MAIN_HAND,normal)&&LbcAcademy.trial(trial),"normal native Academy assigns ordinary Soulless trial");}
                case 1->{trialAndGeneration();}
                case 2->{
                    require(academy.interact(owner,InteractionHand.MAIN_HAND,school),"native token school upgrade callback");var upgraded=owner.getMainHandItem();
                    require(upgraded.is(LbcSoulless.ACADEMY_WAND)&&flag(upgraded,"upgraded")==1&&flag(upgraded,"mode")==1&&Identifier.parse("lbc:tier_a").equals(upgraded.get(DataComponents.TOOLTIP_STYLE)),"real Soulless item promotion/source mode1 and A tier");
                    require(school.isRemoved(),"single-use Academy consumed by real Soulless upgrade");
                    require(upgraded.get(DataComponents.LORE).lines().equals(loot("academy_soulcorruption_wand").get(DataComponents.LORE).lines()),"promotion receives14 source rows plus upgrade marker");return true;
                }
                default->throw new IllegalStateException("unexpected phase");
            }return false;
        }
        void trialAndGeneration(){
            require(LbcAcademy.trial(trial)&&flag(trial,"upgrade_available")==0,"real trial remains pending before paid meditation");
            var actualTrial=trial;
            for(var hand:InteractionHand.values())for(boolean legacy:new boolean[]{false,true}){
                var stack=loot("academy_soulcorruption_wand");if(legacy)stack=stack.transmuteCopy(Items.CARROT_ON_A_STICK);flag(stack,"mode",2);held(stack,hand);
                mana.set(owner,1499);require(!(legacy?game.items().wands().use(owner,hand):input(hand))&&mana.get(owner)==1499&&rewards().isEmpty(),"mode2 strict configured1500 admission "+hand+"/"+legacy);
                mana.set(owner,1500);require(legacy?game.items().wands().use(owner,hand):input(hand),"mode2 used-hand cast "+hand+"/"+legacy);
                var drops=rewards();require(mana.get(owner)==0&&drops.size()==1&&drops.getFirst().getItem().getCount()==1&&drops.getFirst().position().distanceToSqr(owner.position())<1e-10&&souls.count()==0&&souls.ghostCount()==0,"mode2 pays1500 without refund and drops one undeployed metasola at feet");
                var material=LbcSoulless.material(drops.getFirst().getItem());require(material>=1&&material<=6&&ItemStack.isSameItemSameComponents(drops.getFirst().getItem(),loot("metasola_"+material)),"random reward is one existing qualified metasola leaf");clearRewards();
            }
            var main=loot("academy_soulcorruption_wand");var off=loot("academy_soulcorruption_wand");flag(main,"mode",2);flag(off,"mode",2);held(main,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,off);mana.set(owner,3000);
            require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&rewards().size()==2&&souls.count()==0,"real two-hand input independently pays3000 for two undeployed rewards");clearRewards();
            // An ordinary trial in the other hand cannot turn this offhand Academy cast into meditation.
            held(actualTrial,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,off);mana.set(owner,1500);
            require(input(InteractionHand.OFF_HAND)&&LbcAcademy.trial(actualTrial)&&souls.trialSnapshot().isEmpty()&&rewards().size()==1,"mixed hands keep trial attribution on the actual used stack");clearRewards();
            for(var hand:InteractionHand.values())for(boolean legacy:new boolean[]{false,true}){
                var stack=actualTrial.copy();if(legacy)stack=stack.transmuteCopy(Items.CARROT_ON_A_STICK);held(stack,hand);owner.setShiftKeyDown(true);mana.set(owner,1499);
                require(!(legacy?game.items().wands().use(owner,hand):input(hand))&&LbcAcademy.trial(stack)&&souls.trialSnapshot().isEmpty()&&mana.get(owner)==1499,"unpaid meditation never readies or summons "+hand+"/"+legacy);
                mana.set(owner,3000);var origin=owner.position().add(0,1.6,0);
                require(legacy?game.items().wands().use(owner,hand):input(hand),"paid native/legacy meditation "+hand+"/"+legacy);
                var vexes=souls.trialSnapshot();owned.addAll(vexes);
                require(vexes.size()==16&&mana.get(owner)==1500&&flag(stack,"upgrade_available")==1&&flag(stack,"upgradable")==0&&!LbcSoulless.isAcademy(stack),"one1500 debit,16Vex and immediate readiness without promotion "+hand+"/"+legacy);
                require(souls.ghostCount()==0&&souls.vexSnapshot().isEmpty()&&rewards().isEmpty(),"meditation creates neither ghosting nor ghost-conversion rewards");
                for(var vex:vexes)require(vex.getType()==EntityTypes.VEX&&!vex.isNoAi()&&vex.position().distanceToSqr(origin)<1e-10&&vex.canAttack(owner)&&vex.canAttack(other),"source native hostile Vex at+1.6Y remains WILD for caster and allies");
                require(owner.level().getEntities(owner,new net.minecraft.world.phys.AABB(origin,origin).inflate(3),e->!before.contains(e)&&(e.getType()==EntityTypes.WIND_CHARGE||e instanceof ItemEntity drop&&drop.getItem().is(Items.WIND_CHARGE))).isEmpty(),"ordinary Soulless trial never emits WindCharges");
                // A tracked trial Vex unloads quietly, unlike the on-hit ghost Vex reward path.
                souls.unloaded(vexes.getFirst());require(vexes.getFirst().isRemoved()&&souls.trialSnapshot().size()==15&&rewards().isEmpty(),"trial-Vex unload quiet retirement");
                souls.forget(owner);require(souls.trialSnapshot().isEmpty()&&vexes.stream().allMatch(Entity::isRemoved)&&rewards().isEmpty(),"owner invalidation quietly closes all trial helpers");
            }
            held(actualTrial,InteractionHand.MAIN_HAND);owner.setShiftKeyDown(true);mana.set(owner,1500);require(input(InteractionHand.MAIN_HAND),"real Academy-assigned stack completes own native trial");
            owned.addAll(souls.trialSnapshot());require(flag(actualTrial,"upgrade_available")==1&&souls.trialSnapshot().size()==16,"real trial ready for subsequent native Academy promotion");
            game.items().timeStop().begin(owner.level(),40);try{souls.tick();require(souls.trialSnapshot().size()==16,"Time Stop retains existing trial helpers while freezing their native activity");}finally{game.items().timeStop().end(owner.level());}
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add("stopper_magic");add(marker);game.items().magic().loaded(marker);
            try{souls.tick();require(souls.trialSnapshot().size()==16&&rewards().isEmpty(),"existing ordinary trial Vex survive a later antimagic field");}finally{game.items().magic().unloaded(marker);marker.discard();game.items().magic().flushCleansing();}
            souls.forget(owner);require(souls.trialSnapshot().isEmpty()&&rewards().isEmpty(),"ready item survives quiet owner-helper retirement");
            trial=actualTrial;owner.setShiftKeyDown(false);
        }
        @Override public void close(){
            if(closed)return;closed=true;owner.stopUsingItem();game.items().timeStop().end(owner.level());clearSouls();academy.stop();
            for(var e:owned){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
            for(var p:List.of(owner,other))game.items().indirectKills().clear(p);
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container container)container.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);blockTicks.forEach(owner.level().getBlockTicks()::schedule);fluidTicks.forEach(owner.level().getFluidTicks()::schedule);
            restore(game.config(),config);mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("soulcorruption_wand_mana_use"),"lbc.math").set(configuredCost);
            var board=game.server().getScoreboard();for(var p:List.of(owner,other))if(p.getTeam()==team)board.removePlayerFromTeam(p.getScoreboardName(),team);
            if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldOtherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOtherTeam);board.removePlayerTeam(team);
            saved.restore(game);otherSaved.restore(game);game.players().save();game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){
        require(running==null,"one fixture at a time");running=new Fixture(game);
        try{running.initialize();}catch(RuntimeException|Error error){try{running.close();}finally{running=null;}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var fixture=running;
        try{if(fixture.step()){fixture.close();running=null;SkyWars2.LOGGER.info("[check] PASS soulless-academy");}}
        catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL soulless-academy",error);}
    }
    private static List<? extends net.minecraft.world.entity.monster.Vex> stopVexes;
    private static String stopEpoch;
    /** Invoke before other retained probes lend pauses to the player or dimension. */
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var souls=game.items().soulless();
        require(running==null&&stopVexes==null&&owner!=null&&game.canUseGameplayItems(owner)&&souls.trialSnapshot().isEmpty()&&souls.count()==0&&souls.ghostCount()==0&&souls.vexSnapshot().isEmpty()&&cleanAttribution(game,owner),"clean exclusive trial actual-stop probe");
        require(!game.items().timeStop().stopped(owner.level())&&!owner.entityTags().contains("time_stopped"),"trial stop probe runs before retained pauses");
        var saved=new Saved(game,owner);
        try{
            var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/soulcorruption_wand");require(loot.size()==1,"real stop-probe ordinary wand");var stack=loot.getFirst();flag(stack,"upgradable",1);
            owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setShiftKeyDown(true);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);
            require(owner.gameMode.useItem(owner,owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction()&&flag(stack,"upgrade_available")==1&&souls.trialSnapshot().size()==16,"actual native meditation creates16 retained trial Vex");
            stopVexes=souls.trialSnapshot();stopEpoch=game.items().epoch();stopVexes.forEach(v->v.entityTags().add("time_stopped"));
        }catch(RuntimeException|Error error){souls.forget(owner);stopVexes=null;stopEpoch=null;throw error;}
        finally{saved.restore(game);game.items().magic().flushCleansing();}
        SkyWars2.LOGGER.info("[check] PASS soulless-academy-stop-probe");
    }
    public static void cleanup(GameController game){
        var actors=stopVexes;require(actors!=null,"retained trial stop probe exists");
        try{require(!game.items().currentEpoch(stopEpoch)&&game.items().soulless().trialSnapshot().isEmpty()&&actors.stream().allMatch(Entity::isRemoved),"actual match stop invalidates epoch and quietly retires all16 native trial Vex");}
        finally{actors.forEach(Entity::discard);stopVexes=null;stopEpoch=null;}
        SkyWars2.LOGGER.info("[check] PASS soulless-academy-cleanup");
    }
}
