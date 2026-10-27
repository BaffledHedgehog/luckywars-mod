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
public final class AcidAcademyChecks {
    private AcidAcademyChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Acid Academy: "+why);}
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
            AcidAcademyChecks.restore(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);player.setSprinting(sprint);player.setOnGround(ground);
            scores.forEach((key,value)->game.items().mana().score(player,key).set(value));counters.forEach(Runnable::run);
            if(click==null)clickTimes(game.items().academy()).remove(player.getUUID());else clickTimes(game.items().academy()).put(player.getUUID(),click);
            game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcClouds clouds;final LbcAcademy academy;final LbcMana mana;
        final ServerPlayer owner,other;final Saved saved,otherSaved;final Map<Field,Object> config;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>()),owned=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();
        final BlockPos base;final BoundingBox bounds;final Vec3 at;final int configuredCost;final String epoch;
        final net.minecraft.world.scores.PlayerTeam oldOwnerTeam,oldOtherTeam,team;
        Interaction normal,school;ItemStack trial;int phase;long last=Long.MIN_VALUE;boolean closed;
        Fixture(GameController game){
            this.game=game;clouds=game.items().clouds();academy=game.items().academy();mana=game.items().mana();epoch=game.items().epoch();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&owner.level()==other.level(),"two active same-dimension participants");
            require(game.server().tickRateManager().runsNormally()&&!game.items().timeStop().stopped(owner.level())&&clouds.acidSnapshot().isEmpty()&&academy.ownedCount()==0&&academy.pendingCount()==0,"exclusive empty native services and normal clock");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&cleanAttribution(game,p)&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0,"closed menus and no existing frost/combat");
            require(game.server().getScoreboard().getPlayerTeam("acid_academy_fixture")==null,"exclusive fixture team");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());configuredCost=cost();
            oldOwnerTeam=owner.getTeam();oldOtherTeam=other.getTeam();team=game.server().getScoreboard().addPlayerTeam("acid_academy_fixture");team.setAllowFriendlyFire(false);
            owner.level().getAllEntities().forEach(before::add);
            base=new BlockPos((owner.blockPosition().getX()>>4)*16+2,240,(owner.blockPosition().getZ()>>4)*16+2);
            bounds=new BoundingBox(base.getX(),base.getY(),base.getZ(),base.getX()+11,base.getY()+10,base.getZ()+11);at=Vec3.atLowerCornerOf(base).add(6,2,6);
        }
        int cost(){return mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("stacid_wand_mana_use"),"lbc.math").get();}
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
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("stacid_wand_mana_use"),"lbc.math").set(250);
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
            presentation();inputModes();shots();guards();
        }
        void add(Entity e){require(owner.level().addFreshEntity(e),"native fixture entity admission");owned.add(e);}
        void move(ServerPlayer player,Vec3 pos){player.teleportTo(player.level(),pos.x,pos.y,pos.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(String leaf){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(result.size()==1&&result.getFirst().getCount()==1,"real singleton loot "+leaf);return result.getFirst();}
        void held(ItemStack stack,InteractionHand hand){owner.stopUsingItem();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,stack);owner.setShiftKeyDown(false);}
        boolean input(InteractionHand hand){return owner.gameMode.useItem(owner,owner.level(),owner.getItemInHand(hand),hand).consumesAction();}
        List<LbcClouds.AcidProjectile> acids(){var result=clouds.acidSnapshot();owned.addAll(result);return result;}
        void clearAcids(){for(var shot:acids()){shot.discard();clouds.unloaded(shot);}}
        Cow cow(Vec3 pos){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(pos);c.setNoAi(true);c.setNoGravity(true);add(c);return c;}
        void presentation(){
            var ordinary=loot("acid_wand");var stack=loot("academy_acid_wand");
            require(ordinary.is(LbcWands.ACID_WAND)&&stack.is(LbcWands.ACADEMY_ACID_WAND)&&stack.getItem() instanceof eu.pb4.polymer.core.api.item.SimplePolymerItem,"distinct native ordinary/Academy identities");
            for(var key:List.of("craftusable","transformable","staff","magicwand","stacid"))require(flag(stack,key)==1&&flag(ordinary,key)==1,"source flag "+key);
            require(flag(stack,"upgraded")==1&&flag(stack,"mode")==1&&flag(ordinary,"upgraded")==0,"source ordinary and Academy default modes");
            require(Identifier.parse("lbc:st/acid").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(stack.get(DataComponents.TOOLTIP_STYLE))&&Identifier.parse("lbc:tier_b").equals(ordinary.get(DataComponents.TOOLTIP_STYLE)),"source model and A/B tier");
            var normalLore=ordinary.get(DataComponents.LORE).lines();var lore=stack.get(DataComponents.LORE).lines();
            require(normalLore.size()==7&&lore.size()==8&&lore.subList(0,7).equals(normalLore),"seven source lore rows plus upgrade marker");
        }
        void inputModes(){
            for(var hand:InteractionHand.values())for(boolean legacy:new boolean[]{false,true}){
                var stack=loot("academy_acid_wand");if(legacy)stack=stack.transmuteCopy(Items.CARROT_ON_A_STICK);held(stack,hand);mana.set(owner,0);owner.setShiftKeyDown(true);
                require((legacy?game.items().wands().use(owner,hand):input(hand))&&flag(stack,"mode")==2&&mana.get(owner)==0&&acids().isEmpty(),"free Shift mode2 without shot "+hand+"/"+legacy);
                require((legacy?game.items().wands().use(owner,hand):input(hand))&&flag(stack,"mode")==1&&mana.get(owner)==0&&acids().isEmpty(),"free Shift mode wrap without shot "+hand+"/"+legacy);
            }
            var main=loot("academy_acid_wand");var off=loot("academy_acid_wand");held(main,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,off);owner.setShiftKeyDown(true);mana.set(owner,0);
            require(input(InteractionHand.MAIN_HAND)&&flag(main,"mode")==2&&flag(off,"mode")==2&&acids().isEmpty(),"actual dual-hand free selectors");
            game.config().offhandWands=false;
            require(input(InteractionHand.MAIN_HAND)&&flag(main,"mode")==1&&flag(off,"mode")==2,"disabled offhand preserves independent mode");
            require(!input(InteractionHand.OFF_HAND)&&flag(off,"mode")==2&&acids().isEmpty(),"disabled native offhand admission");game.config().offhandWands=true;
        }
        void aimed(LbcClouds.AcidProjectile shot,int mode){
            require(shot.getType()==LbcClouds.ACID_PROJECTILE&&shot.getOwner()==owner&&shot.position().distanceToSqr(owner.getEyePosition())<1e-10,"one existing native acid from actual eye/owner");
            require(shot.entityTags().containsAll(List.of("skywars2.wand.acid.regular","raycastacidbig","shield_blockable2",epoch))&&!shot.entityTags().contains("magic")&&!shot.entityTags().contains("mecha")&&shot.entityTags().contains("on_fire")== (mode==2),"ordinary big acid source flags and selected ignition");
            var expected=owner.getLookAngle().normalize().scale(.2f);
            float nativeYaw=(float)(Mth.atan2(expected.x,expected.z)*(double)(180F/(float)Math.PI));
            float nativePitch=(float)(Mth.atan2(expected.y,expected.horizontalDistance())*(double)(180F/(float)Math.PI));
            require(Math.abs(shot.getYRot()-nativeYaw)<1e-5&&Math.abs(shot.getXRot()-nativePitch)<1e-5&&shot.getDeltaMovement().distanceToSqr(expected)<1e-12,"Academy exactly aimed source-speed shot with native projectile rotations");
            require(!shot.isNoGravity()&&LbcClouds.acidLifetime(shot)==300&&clouds.acidAge(shot)==1,"shared native gravity and existing 300 budget");
        }
        void shots(){
            owner.setYRot(17.25f);owner.setXRot(-8.5f);owner.setRemainingFireTicks(0);
            for(var hand:InteractionHand.values())for(boolean legacy:new boolean[]{false,true})for(int mode=1;mode<=2;mode++){
                var stack=loot("academy_acid_wand");if(legacy)stack=stack.transmuteCopy(Items.CARROT_ON_A_STICK);flag(stack,"mode",mode);held(stack,hand);
                mana.set(owner,249);require(!(legacy?game.items().wands().use(owner,hand):input(hand))&&mana.get(owner)==249&&acids().isEmpty(),"strict configured250 admission "+hand+"/"+legacy+"/"+mode);
                mana.set(owner,250);require(legacy?game.items().wands().use(owner,hand):input(hand),"actual paid Academy input "+hand+"/"+legacy+"/"+mode);
                require(mana.get(owner)==0&&acids().size()==1,"exactly one250 debit and one shot");aimed(acids().getFirst(),mode);clearAcids();
            }
            var main=loot("academy_acid_wand");var off=loot("academy_acid_wand");flag(off,"mode",2);held(main,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,off);mana.set(owner,500);
            require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&acids().size()==2,"actual dual-hand input independently spends500 for two shots");
            require(acids().stream().filter(s->s.entityTags().contains("on_fire")).count()==1,"independent hand ignition modes");clearAcids();
            boolean spread=false;
            for(int i=0;i<16;i++){
                held(loot("acid_wand"),InteractionHand.MAIN_HAND);mana.set(owner,250);require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&acids().size()==1,"ordinary still spends250 and emits one native big acid");
                var shot=acids().getFirst();var velocity=shot.getDeltaMovement();
                float sourceYaw=(float)Math.toDegrees(Math.atan2(-velocity.x,velocity.z));
                float sourcePitch=(float)-Math.toDegrees(Math.atan2(velocity.y,velocity.horizontalDistance()));
                float yaw=Mth.wrapDegrees(sourceYaw-owner.getYRot()),pitch=sourcePitch-owner.getXRot();
                require(Math.abs(yaw)<=30.02&&Math.abs(pitch)<=20.02,"ordinary source spread bounds");spread|=Math.abs(yaw)>.02||Math.abs(pitch)>.02;clearAcids();
            }
            require(spread,"ordinary retains random spread");
            // Native acid moves through ordinary actors; only Greed participates in entity collision.
            var target=cow(at.add(1,0,0));var shot=clouds.academyWandAcid(owner,false);owned.add(shot);
            require(!shot.canHitEntity(target),"ordinary entity passage remains native");other.entityTags().add("sin_greed_active");
            require(shot.canHitEntity(other),"native Greed collision remains enabled");other.entityTags().remove("sin_greed_active");
            var from=shot.position();var velocity=shot.getDeltaMovement();shot.tick();
            require(!shot.isRemoved()&&shot.position().distanceToSqr(from)>0&&shot.getDeltaMovement().y<velocity.y,"actual native tick applies existing gravity and motion");clearAcids();target.discard();
        }
        void guards(){
            held(loot("academy_acid_wand"),InteractionHand.MAIN_HAND);mana.set(owner,1000);
            mana.noMagic(owner,1);require(input(InteractionHand.MAIN_HAND)&&acids().size()==1&&mana.get(owner)==750,"nomagic regen countdown preserves source paid casts");clearAcids();mana.noMagic(owner,0);mana.set(owner,1000);
            owner.entityTags().add("time_stopped");require(!input(InteractionHand.MAIN_HAND)&&acids().isEmpty(),"manual Time Stop blocks cast");owner.entityTags().remove("time_stopped");
            game.items().timeStop().begin(owner.level(),40);try{require(!input(InteractionHand.MAIN_HAND)&&acids().isEmpty()&&mana.get(owner)==1000,"dimension Time Stop blocks cast");}finally{game.items().timeStop().end(owner.level());}
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");add(marker);game.items().magic().loaded(marker);
            try{require(!input(InteractionHand.MAIN_HAND)&&acids().isEmpty()&&mana.get(owner)==1000,"real antimagic field blocks cast");}finally{game.items().magic().unloaded(marker);marker.discard();game.items().magic().flushCleansing();}
            // Shared native impact qualifies teammate player protection while friendly mobs remain eligible.
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            move(other,at.add(0,0,2));var friend=cow(at.add(1,0,2));var role=cow(at.add(-1,0,2));role.entityTags().add("strict_map_object");
            game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);
            other.removeAllEffects();var shot=clouds.academyWandAcid(owner,false);owned.add(shot);shot.setPos(at.add(0,0,2));clouds.tick();
            require(!other.hasEffect(net.minecraft.world.effect.MobEffects.POISON)&&friend.hasEffect(net.minecraft.world.effect.MobEffects.POISON)&&!role.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"shared friend player, friendly mob and technical-role impact guards");
            require(friend.getEffect(net.minecraft.world.effect.MobEffects.POISON).getDuration()==600,"ordinary source poison duration600");
            int age=clouds.acidAge(shot);game.items().timeStop().begin(owner.level(),40);try{clouds.tick();require(!shot.isRemoved()&&clouds.acidAge(shot)==age,"existing acid age freezes with dimension Time Stop");}finally{game.items().timeStop().end(owner.level());}
            clearAcids();friend.discard();role.discard();
        }
        boolean step(){
            require(game.playing()&&game.items().currentEpoch(epoch),"match and fixture epoch active");long now=owner.level().getGameTime();if(now==last)return false;last=now;
            switch(phase++){
                case 0->{held(loot("acid_wand"),InteractionHand.MAIN_HAND);trial=owner.getMainHandItem();require(academy.interact(owner,InteractionHand.MAIN_HAND,normal)&&LbcAcademy.trial(trial),"real Academy assigns ordinary acid trial");}
                case 1->{trials();held(trial,InteractionHand.MAIN_HAND);owner.setRemainingFireTicks(100);mana.set(owner,250);require(input(InteractionHand.MAIN_HAND)&&mana.get(owner)==0&&flag(trial,"upgrade_available")==1&&acids().size()==1,"actual Academy-assigned burning stack readies while still firing and spending250 once");owner.setRemainingFireTicks(0);clearAcids();}
                case 2->{
                    require(academy.interact(owner,InteractionHand.MAIN_HAND,school),"real native Academy promotion callback");var stack=owner.getMainHandItem();var expected=loot("academy_acid_wand");
                    require(stack.is(LbcWands.ACADEMY_ACID_WAND)&&flag(stack,"upgraded")==1&&flag(stack,"mode")==1&&Identifier.parse("lbc:tier_a").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"real promotion native Academy identity/source default/A tier");
                    require(stack.get(DataComponents.LORE).lines().equals(expected.get(DataComponents.LORE).lines())&&stack.get(DataComponents.ITEM_MODEL).equals(expected.get(DataComponents.ITEM_MODEL)),"promotion source presentation equals real Academy loot");
                    require(school.isRemoved(),"real token-built Academy consumed by upgrade");return true;
                }
                default->throw new IllegalStateException("unexpected phase");
            }return false;
        }
        void trials(){
            for(var hand:InteractionHand.values())for(boolean legacy:new boolean[]{false,true}){
                var stack=trial.copy();if(legacy)stack=stack.transmuteCopy(Items.CARROT_ON_A_STICK);held(stack,hand);owner.setRemainingFireTicks(0);mana.set(owner,250);
                require((legacy?game.items().wands().use(owner,hand):input(hand))&&LbcAcademy.trial(stack)&&flag(stack,"upgrade_available")==0&&mana.get(owner)==0&&acids().size()==1,"nonburning used trial still fires without readiness "+hand+"/"+legacy);clearAcids();
                owner.setRemainingFireTicks(100);mana.set(owner,249);
                require(!(legacy?game.items().wands().use(owner,hand):input(hand))&&LbcAcademy.trial(stack)&&flag(stack,"upgrade_available")==0&&mana.get(owner)==249&&acids().isEmpty(),"burning unpaid trial cannot complete "+hand+"/"+legacy);
                mana.set(owner,250);require(legacy?game.items().wands().use(owner,hand):input(hand),"burning paid used trial input");
                require(flag(stack,"upgrade_available")==1&&flag(stack,"upgradable")==0&&flag(stack,"upgraded")==0&&mana.get(owner)==0&&acids().size()==1,"only actual used burning trial immediately ready, ordinary shot, exact250 "+hand+"/"+legacy);clearAcids();
            }
            var used=trial.copy();held(trial,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,used);owner.setRemainingFireTicks(100);mana.set(owner,250);
            require(input(InteractionHand.OFF_HAND)&&flag(used,"upgrade_available")==1&&LbcAcademy.trial(trial)&&flag(trial,"upgrade_available")==0&&mana.get(owner)==0&&acids().size()==1,"two ordinary trials attribute readiness solely to actual used offhand");clearAcids();
            // An Academy cast cannot complete the unused ordinary stack, even while burning.
            var academyStack=loot("academy_acid_wand");held(trial,InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,academyStack);owner.setRemainingFireTicks(100);mana.set(owner,250);
            require(input(InteractionHand.OFF_HAND)&&LbcAcademy.trial(trial)&&flag(trial,"upgrade_available")==0&&acids().size()==1,"mixed hands never borrow ordinary trial");clearAcids();
            var unrelated=loot("fire_wand");flag(unrelated,"upgradable",1);held(loot("acid_wand"),InteractionHand.MAIN_HAND);owner.setItemInHand(InteractionHand.OFF_HAND,unrelated);mana.set(owner,250);
            require(input(InteractionHand.MAIN_HAND)&&flag(unrelated,"upgrade_available")==0&&LbcAcademy.trial(unrelated),"ordinary acid cannot ready unrelated offhand trial");clearAcids();owner.setRemainingFireTicks(0);
        }
        @Override public void close(){
            if(closed)return;closed=true;owner.stopUsingItem();game.items().timeStop().end(owner.level());clearAcids();academy.stop();
            for(var e:owned){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
            for(var p:List.of(owner,other))game.items().indirectKills().clear(p);
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container container)container.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);blockTicks.forEach(owner.level().getBlockTicks()::schedule);fluidTicks.forEach(owner.level().getFluidTicks()::schedule);
            restore(game.config(),config);mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("stacid_wand_mana_use"),"lbc.math").set(configuredCost);
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
        try{if(fixture.step()){fixture.close();running=null;SkyWars2.LOGGER.info("[check] PASS acid-academy");}}
        catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL acid-academy",error);}
    }
    private static List<LbcClouds.AcidProjectile> stopAcids;
    private static String stopEpoch;
    /** Invoke before retained probes lend pauses to players or dimension. Restores player immediately. */
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var clouds=game.items().clouds();
        require(running==null&&stopAcids==null&&owner!=null&&game.canUseGameplayItems(owner)&&clouds.acidSnapshot().isEmpty()&&cleanAttribution(game,owner),"clean exclusive native acid stop probe");
        require(!game.items().timeStop().stopped(owner.level())&&!owner.entityTags().contains("time_stopped"),"stop probe before retained pauses");var saved=new Saved(game,owner);
        try{
            var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_acid_wand");require(loot.size()==1,"real stop-probe Academy loot");var stack=loot.getFirst();flag(stack,"mode",2);
            owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setShiftKeyDown(false);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);
            require(owner.gameMode.useItem(owner,owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction()&&clouds.acidSnapshot().size()==1,"actual native mode2 cast creates retained acid");
            stopAcids=clouds.acidSnapshot();stopEpoch=game.items().epoch();for(var shot:stopAcids){shot.entityTags().add("time_stopped");shot.entityTags().add("skywars2.acid_stuck");shot.setPos(owner.position().add(0,40,0));shot.setDeltaMovement(Vec3.ZERO);}
        }catch(RuntimeException|Error error){for(var shot:clouds.acidSnapshot()){shot.discard();clouds.unloaded(shot);}stopAcids=null;stopEpoch=null;throw error;}
        finally{saved.restore(game);game.items().magic().flushCleansing();}
        SkyWars2.LOGGER.info("[check] PASS acid-academy-stop-probe");
    }
    public static void cleanup(GameController game){
        var actors=stopAcids;require(actors!=null,"retained acid stop probe exists");
        try{require(!game.items().currentEpoch(stopEpoch)&&game.items().clouds().acidSnapshot().isEmpty()&&actors.stream().allMatch(Entity::isRemoved),"actual match stop invalidates epoch and retires native acid");}
        finally{actors.forEach(Entity::discard);stopAcids=null;stopEpoch=null;}
        SkyWars2.LOGGER.info("[check] PASS acid-academy-cleanup");
    }
}
