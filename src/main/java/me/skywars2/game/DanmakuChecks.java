package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Real native input/loot and Academy callbacks; successive clicks use actual server ticks. */
public final class DanmakuChecks {
    private DanmakuChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Danmaku: "+why);}
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
            for(var name:List.of("effect_ghosting","void_resistance","manaregen_visual","danmaku","danmaku_score"))scores.put(name,game.items().mana().score(player,name).get());
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
            DanmakuChecks.restore(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);player.setSprinting(sprint);player.setOnGround(ground);
            scores.forEach((key,value)->game.items().mana().score(player,key).set(value));counters.forEach(Runnable::run);
            if(click==null)clickTimes(game.items().academy()).remove(player.getUUID());else clickTimes(game.items().academy()).put(player.getUUID(),click);
            game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcDanmaku danmaku;final LbcAcademy academy;final LbcMana mana;
        final ServerPlayer owner,other;final Saved saved,otherSaved;final Map<Field,Object> config;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>()),owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final BlockPos base;final BoundingBox bounds;final Vec3 at;final int configuredCost;final String epoch;
        final net.minecraft.world.scores.PlayerTeam oldOwnerTeam,oldOtherTeam,team;
        Interaction normal,school;ItemStack trial;int phase;long last=Long.MIN_VALUE;boolean closed;
        Fixture(GameController game){
            this.game=game;danmaku=game.items().danmaku();academy=game.items().academy();mana=game.items().mana();epoch=game.items().epoch();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&owner.level()==other.level(),"two active same-dimension participants");
            require(game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(owner.level())&&danmaku.count()==0&&danmaku.remaining(owner)==0&&danmaku.remaining(other)==0&&academy.ownedCount()==0&&academy.pendingCount()==0,"exclusive empty native services and normal clock");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&cleanAttribution(game,p)&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0,"closed menus and no existing frost/combat");
            require(game.server().getScoreboard().getPlayerTeam("danmaku_fixture")==null,"exclusive fixture team");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());configuredCost=cost();
            oldOwnerTeam=owner.getTeam();oldOtherTeam=other.getTeam();team=game.server().getScoreboard().addPlayerTeam("danmaku_fixture");team.setAllowFriendlyFire(false);
            owner.level().getAllEntities().forEach(before::add);
            base=new BlockPos((owner.blockPosition().getX()>>4)*16+2,240,(owner.blockPosition().getZ()>>4)*16+2);
            bounds=new BoundingBox(base.getX(),base.getY(),base.getZ(),base.getX()+11,base.getY()+18,base.getZ()+13);at=Vec3.atLowerCornerOf(base).add(6,2,6);
        }
        int cost(){return mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("danmaku_staff_mana_use"),"lbc.math").get();}
        void initialize(){
            require(base.getY()+19<owner.level().getMaxY(),"school below native ceiling");
            require(before.stream().noneMatch(e->!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<96*96),"96block fixture sphere excludes unrelated entities before uncapped-radius probes");
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){
                var chunk=owner.level().getChunkSource().getChunkNow(x,z);require(chunk!=null&&owner.level().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x,z)),"resident school columns");
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            }
            for(var p:BlockPos.betweenClosed(base,base.offset(11,18,13)))blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));
            game.config().offhandWands=true;game.config().ranked=false;game.config().lbcSkills=false;
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("danmaku_staff_mana_use"),"lbc.math").set(1000);
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.setSprinting(false);p.setOnGround(false);p.removeAllEffects();p.entityTags().removeAll(Set.of("sin_greed_active","time_stopped"));
                mana.noMagic(p,0);mana.set(p,100000);mana.score(p,"effect_ghosting").set(0);mana.score(p,"danmaku").set(0);mana.score(p,"danmaku_score").set(0);
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
            for(var pos:BlockPos.betweenClosed(base.offset(2,11,4),base.offset(10,18,13)))require(owner.level().getBlockState(pos).isAir(),"explicit upper flight fixture requires air at "+pos+" actual="+owner.level().getBlockState(pos));
            presentation();guards();
        }
        void add(Entity e){require(owner.level().addFreshEntity(e),"native fixture entity admission");owned.add(e);}
        void move(ServerPlayer player,Vec3 pos){player.teleportTo(player.level(),pos.x,pos.y,pos.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(String leaf){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(result.size()==1&&result.getFirst().getCount()==1,"real singleton loot "+leaf);return result.getFirst();}
        void held(ItemStack stack,InteractionHand hand){owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,stack);owner.setShiftKeyDown(false);}
        boolean input(InteractionHand hand){return owner.gameMode.useItem(owner,owner.level(),owner.getItemInHand(hand),hand).consumesAction();}
        void presentation(){
            var ordinary=loot("danmaku_staff");var upgraded=loot("academy_danmaku_staff");
            require(ordinary.is(LbcDanmaku.ITEM)&&upgraded.is(LbcDanmaku.ACADEMY)&&LbcDanmaku.isItem(ordinary)&&LbcDanmaku.isAcademy(upgraded)&&!LbcDanmaku.isAcademy(ordinary),"distinct native ordinary and Academy identity");
            require(ordinary.getItem() instanceof eu.pb4.polymer.core.api.item.SimplePolymerItem,"native Polymer carrier");
            for(var key:List.of("transformable","spellwithcooldown","staff","magicwand","danmaku_staff"))require(flag(ordinary,key)==1&&flag(upgraded,key)==1,"source flag "+key);
            require(flag(upgraded,"upgraded")==1&&flag(upgraded,"upgrade_available")==1&&flag(upgraded,"mode")==1,"source Academy additions");
            require(Identifier.parse("lbc:danmaku").equals(ordinary.get(DataComponents.ITEM_MODEL))&&ordinary.get(DataComponents.ITEM_MODEL).equals(upgraded.get(DataComponents.ITEM_MODEL)),"authored staff model");
            require(Identifier.parse("lbc:tier_a").equals(ordinary.get(DataComponents.TOOLTIP_STYLE))&&Identifier.parse("lbc:tier_s").equals(upgraded.get(DataComponents.TOOLTIP_STYLE)),"ordinary A and promoted S tiers");
            var lore=ordinary.get(DataComponents.LORE).lines();var schoolLore=upgraded.get(DataComponents.LORE).lines();
            require(lore.size()==9&&schoolLore.size()==10&&schoolLore.subList(0,9).equals(lore),"nine source lore rows preserved before upgrade marker");
            require(ordinary.get(DataComponents.FOOD)==null&&ordinary.get(DataComponents.CONSUMABLE)==null,"actual staff is the non-edible transformable entry");
            require(ordinary.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(upgraded.get(DataComponents.ATTRIBUTE_MODIFIERS)),"Academy preserves source offhand movement attribute");
        }
        void displayPacket(net.minecraft.world.entity.Display.ItemDisplay display,String model,float scale,boolean card){
            var raw=io.netty.buffer.Unpooled.buffer();
            try{
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(raw,owner.registryAccess());
                var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(display.getId(),display.getEntityData().getNonDefaultValues());
                net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,packet);
                var decoded=net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                var received=new net.minecraft.world.entity.Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());received.getEntityData().assignValues(decoded.packedItems());
                var getter=net.minecraft.world.entity.Display.class.getDeclaredMethod("createTransformation",net.minecraft.network.syncher.SynchedEntityData.class);getter.setAccessible(true);
                var transform=(com.mojang.math.Transformation)getter.invoke(null,received.getEntityData());
                require(transform.translation().equals(new org.joml.Vector3f())&&transform.scale().equals(new org.joml.Vector3f(scale))&&transform.rightRotation().equals(new org.joml.Quaternionf()),"wire source translation, signed scale and right rotation");
                var expected=card?new org.joml.Quaternionf(1,0,0,1).normalize():new org.joml.Quaternionf();var left=transform.leftRotation();
                require(Math.abs(left.x()-expected.x())<1e-5&&Math.abs(left.y()-expected.y())<1e-5&&Math.abs(left.z()-expected.z())<1e-5&&Math.abs(left.w()-expected.w())<1e-5,"wire source left rotation");
                require(received.getItemTransform()==(card?net.minecraft.world.item.ItemDisplayContext.FIXED:net.minecraft.world.item.ItemDisplayContext.NONE)&&raw.readableBytes()==0,"native fixed context and complete metadata stream");
                if(card)require(received.getItemStack().is(Items.REPEATING_COMMAND_BLOCK)&&Identifier.parse(model).equals(received.getItemStack().get(DataComponents.ITEM_MODEL)),"authored card carrier and model survive wire");
                else require(received.getItemStack().is(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:red_stained_glass")))&&Objects.equals(received.getItemStack().get(DataComponents.ITEM_MODEL),new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:red_stained_glass"))).get(DataComponents.ITEM_MODEL)),"authored red glass shell without model override");
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Danmaku native display metadata",error);}finally{raw.release();}
        }
        void guards(){
            held(loot("danmaku_staff"),InteractionHand.MAIN_HAND);mana.set(owner,999);
            require(!input(InteractionHand.MAIN_HAND)&&mana.get(owner)==999&&danmaku.count()==0,"strict ordinary1000 admission");
            mana.set(owner,1000);mana.noMagic(owner,5);
            require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0,"source nomagic regeneration pause still allows1000 cast");danmaku.forget(owner);mana.noMagic(owner,0);mana.set(owner,1000);
            owner.entityTags().add("time_stopped");try{require(!input(InteractionHand.MAIN_HAND)&&mana.get(owner)==1000&&danmaku.count()==0,"personal Time Stop suppresses new cast");}finally{owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(owner.level(),40);try{require(!input(InteractionHand.MAIN_HAND)&&mana.get(owner)==1000&&danmaku.count()==0,"dimension Time Stop suppresses new cast");}finally{game.items().timeStop().end(owner.level());}
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");add(marker);game.items().magic().loaded(marker);
            try{require(!input(InteractionHand.MAIN_HAND)&&mana.get(owner)==1000&&danmaku.count()==0,"real antimagic field suppresses new cast");}finally{game.items().magic().unloaded(marker);marker.discard();game.items().magic().flushCleansing();}
            held(loot("danmaku_staff"),InteractionHand.OFF_HAND);game.config().offhandWands=false;
            try{require(!input(InteractionHand.OFF_HAND)&&mana.get(owner)==1000&&danmaku.count()==0,"offhand configuration prevents native cast");}finally{game.config().offhandWands=true;}
        }
        Cow cow(Vec3 pos){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(pos);c.setNoAi(true);c.setNoGravity(true);add(c);return c;}
        void clear(){danmaku.forget(owner);danmaku.forget(other);}
        LbcDanmaku.Card card(Vec3 pos,Vec3 direction,boolean homing){var card=danmaku.card(owner,pos,direction,homing);owned.add(card.display);return card;}
        void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-5,why+" actual="+actual+" expected="+expected);}
        final Set<LbcWands.ElectroTrace> fixtureTraces=Collections.newSetFromMap(new IdentityHashMap<>());
        @SuppressWarnings("unchecked") Set<LbcWands.ElectroTrace> traceStore(){
            try{var field=LbcWands.class.getDeclaredField("electroTraces");field.setAccessible(true);return (Set<LbcWands.ElectroTrace>)field.get(game.items().wands());}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void curse(net.minecraft.world.entity.LivingEntity victim){
            var stack=loot("academy_exahoming_wand");flag(stack,"mode",4);held(stack,InteractionHand.MAIN_HAND);owner.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,victim.getEyePosition());
            int cost=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("exahoming_wand_mana_use"),"lbc.math").get();mana.set(owner,cost);
            require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0,"native Academy Exahoming mode4 curse input");
            for(int i=0;i<100&&game.items().homing().pendingRayCount()>0;i++)game.items().exasperato().tick();
            require(game.items().homing().pendingRayCount()==0&&game.items().homing().curseSeconds(victim)==60,"actual native curse ray drains and binds intended target");
        }
        void bridges(){
            var homing=game.items().homing();var wands=game.items().wands();require(homing.count()==0&&homing.pendingRayCount()==0&&wands.electroSnapshot().isEmpty(),"exclusive empty Curse and virtual electro services");
            clear();move(owner,at.add(0,11,0));move(other,at.add(0,25,0));var origin=owner.position().add(2,1,0);var victim=cow(owner.position().add(0,0,5));
            try{
                curse(victim);var foreign=danmaku.card(other,origin,new Vec3(1,0,0),false);owned.add(foreign.display);homing.redirect(foreign);
                require(foreign.owner==other&&foreign.direction.dot(victim.getEyePosition().subtract(foreign.display.position()).normalize())>.999,"Curse redirects foreign player's Danmaku card while preserving its original owner");
                homing.clear(victim);victim.discard();clear();
                var board=game.server().getScoreboard();if(other.getTeam()==team)board.removePlayerFromTeam(other.getScoreboardName(),team);
                move(other,owner.position().add(0,0,5));curse(other);var own=danmaku.card(other,origin,new Vec3(1,0,0),false);owned.add(own.display);homing.redirect(own);
                require(own.owner==owner&&own.direction.dot(other.getEyePosition().subtract(own.display.position()).normalize())>.999,"Curse adopts cursed shooter's Danmaku card and redirects toward that victim");
                homing.clear(other);clear();
                var enemy=new LbcWands.ElectroTrace(other,origin,epoch);var friendly=new LbcWands.ElectroTrace(owner,origin,epoch);var outside=new LbcWands.ElectroTrace(other,origin.add(4,0,0),epoch);
                fixtureTraces.addAll(List.of(enemy,friendly,outside));for(var trace:fixtureTraces)wands.higherElectro(trace);
                require(wands.electroSnapshot().containsAll(fixtureTraces)&&wands.clearDanmakuRays(owner,origin,1)==1,"Danmaku clears one enemy nonentity electro trace inside native bomb radius");
                require(!wands.electroSnapshot().contains(enemy)&&wands.electroSnapshot().containsAll(List.of(friendly,outside)),"electro bridge preserves own trace and foreign trace beyond bomb radius");
            }finally{
                homing.clear(victim);homing.clear(other);victim.discard();clear();traceStore().removeAll(fixtureTraces);fixtureTraces.clear();
                game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);move(owner,at);move(other,at.add(0,20,0));
            }
        }
        void projectiles(){
            clear();held(ItemStack.EMPTY,InteractionHand.MAIN_HAND);move(other,at.add(0,20,0));
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            var origin=at.add(0,11,0);var shot=card(origin,new Vec3(0,0,1),false);
            require(shot.owner==owner&&!shot.homing&&danmaku.owner(shot.display)==owner&&danmaku.managed(shot.display),"managed card owner and direct source kind");
            displayPacket(shot.display,"lbc:card_1",.5f,true);danmaku.tick();near(shot.display.position().distanceTo(origin),1.2,"three native0.4 substeps per tick removed="+shot.display.isRemoved()+" age="+shot.age+" ownerValid="+game.canUseGameplayItems(owner)+" fromBlock="+owner.level().getBlockState(BlockPos.containing(origin))+" currentBlock="+owner.level().getBlockState(shot.display.blockPosition()));require(shot.age==1,"one source lifetime increment per tick");
            var paused=shot.display.position();int age=shot.age;game.items().timeStop().begin(owner.level(),40);
            try{danmaku.tick();require(shot.age==age&&shot.display.position().equals(paused),"Time Stop freezes card motion and age");}finally{game.items().timeStop().end(owner.level());}
            clear();shot=card(origin,new Vec3(0,0,1),true);displayPacket(shot.display,"lbc:card_2",.5f,true);clear();
            shot=card(origin,new Vec3(0,0,1),false);shot.age=59;danmaku.tick();require(shot.display.isRemoved()&&danmaku.cards().isEmpty(),"native card lifetime60 retires display");
            var wall=base.offset(6,14,7);owner.level().setBlock(wall.below(),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),3);owner.level().setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            shot=card(Vec3.atLowerCornerOf(wall).add(.2,.2,-.3),new Vec3(0,0,1),false);danmaku.tick();require(shot.display.isRemoved(),"solid block intercepts three-substep ray");
            owner.level().setBlock(wall,net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState(),3);
            shot=card(Vec3.atLowerCornerOf(wall).add(.2,.2,-.3),new Vec3(0,0,1),false);danmaku.tick();require(shot.display.isRemoved(),"non-air replaceable grass still intercepts source card");
            owner.level().setBlock(wall,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);owner.level().setBlock(wall.below(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);clear();
            var target=cow(origin.add(0,0,2));game.server().getScoreboard().addPlayerToTeam(target.getScoreboardName(),team);mana.score(owner,"danmaku_score").set(0);
            shot=card(target.position().add(-.3,.3,-.5),new Vec3(0,0,1),false);float health=target.getHealth();var movement=target.getDeltaMovement();danmaku.tick();near(health-target.getHealth(),.65,"friendly mob receives generic0.65 source damage");
            require(shot.display.isRemoved()&&mana.score(owner,"danmaku_score").get()==3,"single accepted collision consumes card and scores3");
            require(target.getDeltaMovement().equals(movement),"attackerless source card preserves motion without an added knockback impulse");
            require(target.damageCooldownTime>10&&target.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN),"first native card establishes a real hurt cooldown and uses imported generic cooldown bypass");
            health=target.getHealth();shot=card(target.position().add(-.3,.3,-.5),new Vec3(0,0,1),false);danmaku.tick();near(health-target.getHealth(),.65,"imported generic damage tag bypasses native hurt invulnerability");require(mana.score(owner,"danmaku_score").get()==6,"each accepted generic hit adds source score3 despite existing hurt cooldown");
            target.setHealth(.5f);var existing=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());owner.level().getAllEntities().forEach(existing::add);
            card(target.position().add(-.3,.3,-.5),new Vec3(0,0,1),false);danmaku.tick();
            for(var e:owner.level().getAllEntities())if(!existing.contains(e))owned.add(e);
            require(target.isDeadOrDying()&&target.getKillCredit()==owner,"lethal attackerless card inherits its caster's actual native kill credit");
            target.discard();clear();
            for(var tag:List.of("strict_map_object","sin_greed_active")){
                var protectedMob=cow(origin.add(0,0,2));protectedMob.entityTags().add(tag);health=protectedMob.getHealth();card(protectedMob.position().add(-.3,.3,-.5),new Vec3(0,0,1),false);danmaku.tick();near(protectedMob.getHealth(),health,"protected native mob "+tag);protectedMob.discard();clear();
            }
            move(other,origin.add(0,0,2));health=other.getHealth();card(other.position().add(-.3,.3,-.5),new Vec3(0,0,1),false);danmaku.tick();near(other.getHealth(),health,"allied player protected from direct source card");clear();move(other,at.add(0,20,0));
            var friend=cow(origin.add(2,0,3));game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);var enemy=cow(origin.add(-3,0,6));
            shot=card(origin,new Vec3(0,0,1),true);danmaku.tick();require(shot.direction.x<0,"homing chooses enemy rather than closer allied mob");friend.discard();enemy.discard();clear();
            shot=card(origin,new Vec3(0,0,1),false);var display=shot.display;danmaku.unloaded(display);require(danmaku.cards().isEmpty()&&display.isRemoved(),"native unload retires owned card");
            shot=card(origin,new Vec3(0,0,1),false);display=shot.display;danmaku.forget(owner);require(danmaku.cards().isEmpty()&&display.isRemoved(),"owner retirement clears own card");
        }
        void bombs(){
            clear();held(loot("danmaku_staff"),InteractionHand.MAIN_HAND);owner.setShiftKeyDown(true);mana.set(owner,0);mana.score(owner,"danmaku_score").set(49);
            require(!input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&mana.score(owner,"danmaku_score").get()==49&&danmaku.bombs().isEmpty(),"free bomb still requires charge50");
            mana.score(owner,"danmaku_score").set(50);require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&mana.score(owner,"danmaku_score").get()==0&&danmaku.bombs().size()==1,"zero-mana bomb captures all50 charge and consumes no mana");
            var bomb=danmaku.bombs().getFirst();owned.add(bomb.positive);owned.add(bomb.negative);
            require(bomb.owner==owner&&bomb.charge==50,"stable bomb owner and captured source charge");displayPacket(bomb.positive,null,1,false);displayPacket(bomb.negative,null,-1,false);
            danmaku.tick();require(bomb.age==4&&bomb.radius()==0,"bomb advances four source age units and integer radius");displayPacket(bomb.positive,null,.8f,false);displayPacket(bomb.negative,null,-.8f,false);
            var fixed=bomb.positive.position();move(owner,at.add(3,0,0));danmaku.tick();require(bomb.positive.position().equals(fixed)&&bomb.negative.position().equals(fixed),"bomb stays at cast position as owner moves");
            var center=bomb.positive.position();var victim=cow(center);var role=cow(center.add(.1,0,0));role.entityTags().add("strict_map_object");var greed=cow(center.add(-.1,0,0));greed.entityTags().add("sin_greed_active");
            move(other,center);float allyHealth=other.getHealth(),victimHealth=victim.getHealth(),roleHealth=role.getHealth(),greedHealth=greed.getHealth();
            var hostile=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,owner.level());hostile.setOwner(victim);hostile.setPos(center);add(hostile);
            var friendly=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,owner.level());friendly.setOwner(owner);friendly.setPos(center);add(friendly);
            bomb.age=16;danmaku.tick();near(victim.getHealth(),victimHealth-1,"source bomb native magic1 impact");require(victim.getLastHurtByMob()==owner,"bomb attributes real damage to stable owner");
            near(role.getHealth(),roleHealth,"bomb protects technical mob");near(greed.getHealth(),greedHealth,"bomb protects active Greed");near(other.getHealth(),allyHealth,"bomb protects allied player");
            require(hostile.isRemoved()&&!friendly.isRemoved(),"bomb clears hostile native projectile and preserves owner's projectile");
            danmaku.tick();near(victim.getHealth(),victimHealth-1,"bomb repeats respect native hurt invulnerability");
            require(!victim.entityTags().contains("nodmg")&&!victim.entityTags().contains("nodmg1"),"bomb does not leak legacy global no-damage tags to mobs");
            victim.discard();role.discard();greed.discard();friendly.discard();move(other,at.add(0,20,0));
            int age=bomb.age;game.items().timeStop().begin(owner.level(),40);try{danmaku.tick();require(bomb.age==age,"Time Stop freezes bomb captured lifetime");}finally{game.items().timeStop().end(owner.level());}
            bomb.age=48;danmaku.tick();require(bomb.positive.isRemoved()&&bomb.negative.isRemoved()&&danmaku.bombs().isEmpty(),"bomb expires only once age exceeds captured charge");clear();move(owner,at);
            mana.score(owner,"danmaku_score").set(1500);require(input(InteractionHand.MAIN_HAND),"large source bomb is uncapped");bomb=danmaku.bombs().getFirst();owned.add(bomb.positive);owned.add(bomb.negative);bomb.age=996;danmaku.tick();require(bomb.age==1000&&bomb.radius()==60,"large bomb nested floor radius at source age1000");clear();owner.setShiftKeyDown(false);
        }
        final Set<UUID> emitted=new HashSet<>();int pattern=-1;boolean patternStarted,geometry;int elapsed,homingEmitted;
        void startPattern(){
            clear();emitted.clear();elapsed=0;homingEmitted=0;geometry=false;patternStarted=true;move(owner,at.add(0,11,0));move(other,at.add(0,24,0));owner.setYRot(0);owner.setXRot(0);
            // Actual volley ticks run at zero mana. Pause unrelated regeneration
            // and its empty-mana Crux drop; this state does not block staff input.
            mana.noMagic(owner,100000);
            var hand=pattern%2==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;boolean upgraded=pattern==2||pattern==3||pattern==5;
            held(loot(upgraded?"academy_danmaku_staff":"danmaku_staff"),hand);if(pattern>=4){hand=InteractionHand.MAIN_HAND;held(loot(upgraded?"academy_danmaku_staff":"danmaku_staff"),hand);owner.setItemInHand(InteractionHand.OFF_HAND,loot(upgraded?"academy_danmaku_staff":"danmaku_staff"));}mana.set(owner,pattern>=4?2000:1000);require(input(hand)&&mana.get(owner)==0&&danmaku.remaining(owner)==(pattern>=4?40:20),"actual native hand input queues20 per paid staff "+pattern);
        }
        boolean patternTick(){
            var fresh=new ArrayList<LbcDanmaku.Card>();for(var c:danmaku.cards()){if(emitted.add(c.display.getUUID())){fresh.add(c);if(c.homing)homingEmitted++;}owned.add(c.display);}if(!geometry&&!fresh.isEmpty()&&pattern<4){firstGeometry(fresh);geometry=true;}
            if(danmaku.remaining(owner)>0){require(++elapsed<=(pattern>=4?43:23),"bounded actual-tick source volley");return false;}
            int expected=switch(pattern){case 0->40;case 1->35;case 2->89;case 3->60;case 4->150;default->298;};int homing=switch(pattern){case 0->8;case 1->12;case 2->72;case 3->0;case 4->40;default->144;};require(homingEmitted==homing,"full source homing/straight composition "+pattern+" actual="+homingEmitted);require(emitted.size()==expected,"full authored20tick volley "+pattern+" emitted="+emitted.size()+" expected="+expected);clear();patternStarted=false;return true;
        }
        void firstGeometry(List<LbcDanmaku.Card> batch){
            if(pattern==0){
                var direct=batch.stream().filter(c->!c.homing).toList();require(batch.size()==9&&direct.size()==5,"source main phase1 emits five straight and four homing");
                for(int yaw:new int[]{30,15,0,-15,-30})require(direct.stream().anyMatch(c->c.direction.distanceToSqr(Vec3.directionFromRotation(0,yaw))<1e-10),"source main phase1 authored yaw "+yaw);
            }else if(pattern==1){
                require(batch.size()==2&&batch.stream().noneMatch(c->c.homing),"source offhand phase1 two straight cards");
                for(double x:new double[]{-.3,.3})require(batch.stream().anyMatch(c->Math.abs(c.display.getX()-owner.getX()-x)<1e-5&&Math.abs(c.display.getY()-(owner.getEyeY()-.5))<1e-5),"source ordinary offhand local offset "+x);
            }else if(pattern==2)require(batch.size()==8&&batch.stream().allMatch(c->c.homing),"source Academy main phase1 eight homing cards");
            else{
                require(batch.size()==6&&batch.stream().noneMatch(c->c.homing),"source Academy offhand six-card ring");
                double[][] offsets={{0,.547723},{.474342,.273861},{.474342,-.273861},{0,-.547723},{-.474342,-.273861},{-.474342,.273861}};
                for(var offset:offsets)require(batch.stream().anyMatch(c->Math.abs(c.display.getX()-owner.getX()-offset[0])<1e-5&&Math.abs(c.display.getY()-owner.getEyeY()-offset[1])<1e-5),"source Academy offhand authored ring geometry "+Arrays.toString(offset));
            }
        }
        void counters(){
            clear();var main=loot("danmaku_staff");var off=loot("danmaku_staff");held(main,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,off);mana.set(owner,2000);
            require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&danmaku.remaining(owner)==40,"native dual-hand input spends2000 and adds40 shared ticks");clear();
            held(main,InteractionHand.MAIN_HAND);mana.set(owner,2000);require(input(InteractionHand.MAIN_HAND)&&input(InteractionHand.MAIN_HAND)&&danmaku.remaining(owner)==40&&mana.get(owner)==0,"successive paid casts add shared duration");
            held(ItemStack.EMPTY,InteractionHand.MAIN_HAND);danmaku.tick();require(danmaku.remaining(owner)==39&&danmaku.cards().isEmpty(),"put-away staff counter decays without inventory fallback shots");
            owner.setItemInHand(InteractionHand.MAIN_HAND,main);danmaku.tick();danmaku.tick();require(!danmaku.cards().isEmpty(),"returning held staff resumes surviving shared timer");clear();
            held(main,InteractionHand.MAIN_HAND);mana.set(owner,1000);require(input(InteractionHand.MAIN_HAND),"counter pause real native input");int ticks=danmaku.remaining(owner);
            game.items().timeStop().begin(owner.level(),40);try{danmaku.tick();require(danmaku.remaining(owner)==ticks&&danmaku.cards().isEmpty(),"dimension Time Stop freezes pending shared volley");}finally{game.items().timeStop().end(owner.level());}
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.position());marker.entityTags().add("stopper_magic");add(marker);game.items().magic().loaded(marker);
            try{danmaku.tick();require(danmaku.remaining(owner)==ticks&&danmaku.cards().isEmpty(),"real antimagic pauses pending shared volley rather than spending timer");}finally{game.items().magic().unloaded(marker);marker.discard();game.items().magic().flushCleansing();}
            danmaku.tick();require(danmaku.remaining(owner)==ticks-1&&!danmaku.cards().isEmpty(),"pending volley resumes after real antimagic leaves");clear();

        }
        void trials(){
            for(var hand:InteractionHand.values())for(boolean legacy:new boolean[]{false,true})for(int charge:new int[]{1499,1500}){
                var used=trial.copy();if(legacy)used=used.transmuteCopy(Items.CARROT_ON_A_STICK);held(used,hand);owner.setShiftKeyDown(true);mana.set(owner,0);mana.score(owner,"danmaku_score").set(charge);
                require(legacy?game.items().wands().use(owner,hand):input(hand),"free actual used trial bomb "+hand+"/"+legacy+"/"+charge);
                require(flag(used,"upgrade_available")== (charge==1500?1:0)&&flag(used,"upgraded")==0,"inclusive1500 readiness only on ordinary used stack");clear();
            }
            held(trial,InteractionHand.MAIN_HAND);var otherTrial=trial.copy();owner.setItemInHand(InteractionHand.OFF_HAND,otherTrial);owner.setShiftKeyDown(true);mana.score(owner,"danmaku_score").set(1500);
            require(input(InteractionHand.OFF_HAND)&&flag(otherTrial,"upgrade_available")==1&&flag(trial,"upgrade_available")==0,"offhand trial does not ready unused ordinary mainhand");clear();
            held(trial,InteractionHand.MAIN_HAND);owner.setShiftKeyDown(true);mana.score(owner,"danmaku_score").set(1500);require(input(InteractionHand.MAIN_HAND)&&flag(trial,"upgrade_available")==1,"actual Academy-assigned trial readies at inclusive1500");clear();owner.setShiftKeyDown(false);
        }
        boolean step(){
            require(game.playing()&&game.items().currentEpoch(epoch),"active fixture match and epoch");long now=owner.level().getGameTime();if(now==last)return false;last=now;
            if(phase==0){projectiles();bridges();bombs();counters();phase=1;pattern=0;startPattern();return false;}
            if(phase==1){if(!patternTick())return false;if(++pattern<6){startPattern();return false;}phase=2;move(owner,at);held(loot("danmaku_staff"),InteractionHand.MAIN_HAND);trial=owner.getMainHandItem();require(academy.interact(owner,InteractionHand.MAIN_HAND,normal)&&LbcAcademy.trial(trial),"real Academy assigns ordinary Danmaku trial");return false;}
            if(phase==2){trials();phase=3;return false;}
            require(phase==3&&academy.interact(owner,InteractionHand.MAIN_HAND,school),"native token-school promotion callback");var stack=owner.getMainHandItem();var expected=loot("academy_danmaku_staff");
            require(stack.is(LbcDanmaku.ACADEMY)&&flag(stack,"upgraded")==1&&Identifier.parse("lbc:tier_s").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"native Academy promotion produces source S tier");
            require(stack.get(DataComponents.LORE).lines().equals(expected.get(DataComponents.LORE).lines())&&stack.get(DataComponents.ITEM_MODEL).equals(expected.get(DataComponents.ITEM_MODEL))&&school.isRemoved(),"source promotion presentation equals real loot and consumes token-school");return true;
        }
        @Override public void close(){
            if(closed)return;closed=true;traceStore().removeAll(fixtureTraces);fixtureTraces.clear();owner.stopUsingItem();game.items().timeStop().end(owner.level());danmaku.forget(owner);danmaku.forget(other);academy.stop();
            for(var e:owned){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
            for(var p:List.of(owner,other))game.items().indirectKills().clear(p);
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container container)container.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);blockTicks.forEach(owner.level().getBlockTicks()::schedule);fluidTicks.forEach(owner.level().getFluidTicks()::schedule);
            restore(game.config(),config);mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("danmaku_staff_mana_use"),"lbc.math").set(configuredCost);
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
        try{if(fixture.step()){fixture.close();running=null;SkyWars2.LOGGER.info("[check] PASS danmaku");}}
        catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL danmaku",error);}
    }
    private static List<Entity> retained;
    private static String retainedEpoch;
    /** Create immediately before other retained pause probes; restore the participant before returning. */
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var service=game.items().danmaku();
        require(running==null&&retained==null&&owner!=null&&game.canUseGameplayItems(owner)&&service.count()==0&&service.remaining(owner)==0&&cleanAttribution(game,owner),"exclusive native retained stop probe");
        require(!game.items().timeStop().stopped(owner.level())&&!owner.entityTags().contains("time_stopped"),"retained probe runs before pause fixtures");var saved=new Saved(game,owner);
        try{
            var result=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/danmaku_staff");require(result.size()==1,"real retained ordinary loot");var stack=result.getFirst();
            owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setShiftKeyDown(false);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,1000);
            require(owner.gameMode.useItem(owner,owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction(),"actual native retained volley input");service.tick();require(!service.cards().isEmpty(),"actual service tick emitted retained cards");
            owner.setShiftKeyDown(true);game.items().mana().score(owner,"danmaku_score").set(1500);require(owner.gameMode.useItem(owner,owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction()&&service.bombs().size()==1,"native zero-mana retained bomb input");
            var actors=new ArrayList<Entity>();for(var card:service.cards()){actors.add(card.display);card.display.entityTags().add("time_stopped");card.display.setPos(owner.position().add(0,40,0));}
            for(var bomb:service.bombs()){actors.add(bomb.positive);actors.add(bomb.negative);bomb.positive.entityTags().add("time_stopped");bomb.negative.entityTags().add("time_stopped");}
            retained=List.copyOf(actors);retainedEpoch=game.items().epoch();
        }catch(RuntimeException|Error error){service.forget(owner);retained=null;retainedEpoch=null;throw error;}
        finally{saved.restore(game);game.items().magic().flushCleansing();}
        SkyWars2.LOGGER.info("[check] PASS danmaku-stop-probe");
    }
    public static void cleanup(GameController game){
        var actors=retained;require(actors!=null&&!actors.isEmpty(),"retained native cards and bomb exist");
        try{require(!game.items().currentEpoch(retainedEpoch)&&game.items().danmaku().count()==0&&actors.stream().allMatch(Entity::isRemoved),"actual match stop changes epoch and discards every retained native card and shell");}
        finally{actors.forEach(Entity::discard);retained=null;retainedEpoch=null;}
        SkyWars2.LOGGER.info("[check] PASS danmaku-cleanup");
    }
}
