package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Synchronous native boundary checks: explicit ticks below do not represent elapsed server time. */
public final class IceWandChecks {
    private IceWandChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Ice Wand: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<.001,why+" actual="+actual+" expected="+expected);}
    private static Object copy(Object value){if(value instanceof int[] array)return array.clone();if(value instanceof Set<?> set)return new HashSet<>(set);return value;}
    private static final class PlayerState {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final PlayerStore.Profile profile;
        final Map<java.lang.reflect.Field,Object> fields=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<net.minecraft.world.item.Item>,Integer> itemStats=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;final int cowKills;final boolean shift;
        PlayerState(GameController game,ServerPlayer player){
            this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());team=player.getTeam();shift=player.isShiftKeyDown();
            try{for(var field:PlayerStore.Profile.class.getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers()))fields.put(field,copy(field.get(profile)));}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
            cowKills=player.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));
            for(var item:List.of(LbcWands.ICE,LbcWands.ACADEMY_ICE)){for(var type:List.of(Stats.ITEM_CRAFTED,Stats.ITEM_USED)){var stat=type.get(item);itemStats.put(stat,player.getStats().getValue(stat));}}
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){var initial=new HashSet<String>();player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(initial::add);advancements.put(advancement,initial);}
        }
        void restore(GameController game){
            game.items().frost().clear(player);game.items().indirectKills().clear(player);
            var board=game.server().getScoreboard();var current=player.getTeam();if(current!=null)board.removePlayerFromTeam(player.getScoreboardName(),current);if(team!=null)board.addPlayerToTeam(player.getScoreboardName(),team);
            stats.forEach((stat,value)->player.getStats().setValue(player,stat,value));itemStats.forEach((stat,value)->player.getStats().setValue(player,stat,value));player.getStats().setValue(player,Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);
            advancements.forEach((adv,initial)->{for(var criterion:adv.value().criteria().keySet())if(initial.contains(criterion))player.getAdvancements().award(adv,criterion);else player.getAdvancements().revoke(adv,criterion);});
            state.restore(game);player.setShiftKeyDown(shift);
            try{for(var field:fields.entrySet())field.getKey().set(profile,copy(field.getValue()));}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            game.items().mana().inventoryChanged(player);game.items().magic().inventoryChanged(player);
        }
    }
    private static final class ProbeCow extends Cow {
        int freeze,arrow;float lastFreeze,lastArrow;boolean noKnockback;Entity author;
        ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,DamageSource source,float amount){
            if(source.is(DamageTypes.FREEZE)){freeze++;lastFreeze=amount;noKnockback=source.is(net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK);author=source.getEntity();}
            if(source.is(DamageTypes.ARROW)){arrow++;lastArrow=amount;author=source.getEntity();}return super.hurtServer(level,source,amount);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;final PlayerState saved,otherSaved;
        final LbcIceWand service;final Vec3 at;final BlockPos origin;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ProbeCow> cows=new ArrayList<>();final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;final boolean offhand,skills;
        Marker stopper;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active participants");
            service=game.items().iceWand();require(service.count()==0&&!game.items().timeStop().stopped(owner.level())&&game.items().frost().cold(owner)==0&&game.items().frost().cold(other)==0&&!game.items().frost().isFrozen(owner)&&!game.items().frost().isFrozen(other),"clean initial actors/frost/Time Stop");
            saved=new PlayerState(game,owner);otherSaved=new PlayerState(game,other);offhand=game.config().offhandWands;skills=game.config().lbcSkills;
            origin=owner.blockPosition().offset(0,7,0);at=Vec3.atBottomCenterOf(origin);owner.level().getAllEntities().forEach(before::add);
            team=game.server().getScoreboard().addPlayerTeam("ice_fixture");team.setAllowFriendlyFire(false);
        }
        void init(){
            for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-2;y<=5;y++){
                var p=origin.offset(x,y,z);require(owner.level().hasChunkAt(p),"fixture stays in resident chunks");block(p,y==-2?Blocks.STONE:Blocks.AIR);
            }
            for(var player:List.of(owner,other)){
                player.closeContainer();player.getInventory().clearContent();player.inventoryMenu.getCraftSlots().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);player.getInventory().setSelectedSlot(0);
                player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setShiftKeyDown(false);player.setDeltaMovement(Vec3.ZERO);
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);player.getFoodData().setFoodLevel(17);
                game.items().frost().clear(player);game.items().mana().noMagic(player,0);game.items().mana().set(player,100000);
                var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
                game.server().getScoreboard().addPlayerToTeam(player.getScoreboardName(),team);
                game.items().magic().inventoryChanged(player);
            }
            game.config().offhandWands=true;game.config().lbcSkills=false;game.items().magic().flushCleansing();home();
        }
        void home(){move(owner,at.add(-6,0,-6));move(other,at.add(6,0,-6));}
        void move(ServerPlayer p,Vec3 v){p.teleportTo(p.level(),v.x,v.y,v.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        void block(BlockPos p,Block type){blocks.putIfAbsent(p.immutable(),game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,type.defaultBlockState(),2);}
        ItemStack loot(String leaf){var result=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+leaf);require(result.size()==1&&result.getFirst().getCount()==1,"live loot "+leaf);return result.getFirst();}
        int flag(ItemStack stack,String key){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
        void flag(ItemStack stack,String key,int value){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt(key,value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
        void clear(){service.forget(owner);service.forget(other);}
        ProbeCow cow(Vec3 point){var cow=new ProbeCow(owner.level());cow.setPos(point);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);owner.level().addFreshEntity(cow);cows.add(cow);return cow;}
        void reset(LivingEntity target){game.items().frost().clear(target);target.removeAllEffects();target.setHealth(1000);target.setAbsorptionAmount(0);target.setInvulnerableTime(0);target.damageCooldownTime=0;target.setDeltaMovement(Vec3.ZERO);}
        LbcIceWand.Crystal crystal(ServerPlayer caster,BlockPos cell){block(cell,Blocks.AIR);block(cell.below(),Blocks.STONE);var crystal=service.spawnCrystal(caster,InteractionHand.MAIN_HAND,loot("ice_wand"),Vec3.atCenterOf(cell));require(crystal!=null,"native crystal spawn");return crystal;}
        void presentation(){
            for(boolean academy:new boolean[]{false,true}){var s=loot(academy?"academy_ice_wand":"ice_wand");require(s.is(academy?LbcWands.ACADEMY_ICE:LbcWands.ICE)&&flag(s,"ice_wand")==1,"native ice item/flag");require(Identifier.parse("lbc:ice_wand").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(academy?"lbc:tier_ss":"lbc:tier_s").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(academy?8:7),"authored S/SS model/lore");}
            craft(false);craft(true);
            for(boolean academy:new boolean[]{false,true})for(boolean shift:new boolean[]{false,true}){
                owner.setShiftKeyDown(shift);var s=loot(academy?"academy_ice_wand":"ice_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,s);owner.setItemInHand(InteractionHand.OFF_HAND,s.copy());game.items().mana().set(owner,100000);
                require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"actual wands both-hand dispatch");near(game.items().mana().get(owner),100000-(shift?400:800),"two hands debit source cost");require(shift?service.count()==0:service.shotSnapshot().size()==2,"both hand action count");clear();
            }
            owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().set(owner,399);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==399,"ordinary strict admission");game.items().mana().set(owner,100000);
        }
        void craft(boolean bench){
            owner.closeContainer();owner.getInventory().clearContent();if(bench){var p=origin.offset(-5,0,-6);block(p,Blocks.CRAFTING_TABLE);owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,pl)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(owner.level(),p)),net.minecraft.network.chat.Component.literal("ice fixture")));}
            var menu=(net.minecraft.world.inventory.AbstractCraftingMenu)owner.containerMenu;var slots=menu.getInputGridSlots();var neutral=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/neutral"));require(neutral!=null&&neutral.value().criteria().containsKey("ice"),"source partial neutral criterion");for(var key:neutral.value().criteria().keySet())owner.getAdvancements().revoke(neutral,key);
            slots.getFirst().setByPlayer(loot(bench?"academy_neutral_wand":"neutral_wand").copyWithCount(2));slots.getLast().setByPlayer(loot("runic_ice_shard").copyWithCount(2));
            require(menu.getResultSlot().getItem().is(LbcWands.ICE)&&!owner.getAdvancements().getOrStartProgress(neutral).getCompletedCriteria().iterator().hasNext(),"native preview has result and no free advancement");
            slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra donor rejected");slots.get(1).setByPlayer(ItemStack.EMPTY);
            GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
            require(menu.getCarried().is(LbcWands.ICE)&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"native ResultSlot take consumes one each in "+(bench?"3x3":"2x2"));
            int balls=0;for(int i=0;i<owner.getInventory().getContainerSize();i++)if(LbcIceMaterials.isNeutralBall(owner.getInventory().getItem(i)))balls+=owner.getInventory().getItem(i).getCount();for(var slot:slots)if(LbcIceMaterials.isNeutralBall(slot.getItem()))balls+=slot.getItem().getCount();
            require(balls==1&&owner.getAdvancements().getOrStartProgress(neutral).getCriterion("ice").isDone(),"exact Neutral Ball remainder and only real-take ice criterion");
            for(var criterion:owner.getAdvancements().getOrStartProgress(neutral).getCompletedCriteria())require(criterion.equals("ice"),"neutral unrelated criteria unchanged");
            menu.setCarried(ItemStack.EMPTY);slots.forEach(slot->slot.setByPlayer(ItemStack.EMPTY));owner.closeContainer();owner.getInventory().clearContent();
        }
        void flight(){
            home();var stack=loot("ice_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setYRot(35);owner.setXRot(-20);
            var shot=service.spawn(owner,InteractionHand.MAIN_HAND,stack);near(shot.getDeltaMovement().length(),1.5,"source launch velocity");require(shot.getOwner()==owner&&shot.display.position().equals(shot.position())&&!shot.display.isRemoved(),"owner and zero-tick real carrier display position");require(Math.abs(shot.display.getYRot()-35)<.01&&Math.abs(shot.display.getXRot()+20)<.01,"zero-tick pose matches launch direction within native trigonometry precision");require(Identifier.parse("lbc:ice_crystal_proj").equals(shot.display.getItemStack().get(DataComponents.ITEM_MODEL)),"native projectile model");
            shot.setPos(at.add(0,2,-4));shot.setDeltaMovement(new Vec3(0,0,1.5));var from=shot.position();shot.tick();require(!shot.isRemoved()&&shot.position().z>from.z+1.4&&shot.position().y<from.y&&shot.getDeltaMovement().y<0&&shot.display.position().equals(shot.position()),"native Snowball movement/gravity and same-tick display sync");clear();
            for(int x=-1;x<=1;x++)for(int y=0;y<=3;y++)block(origin.offset(x,y,0),Blocks.STONE);
            shot=service.spawn(owner,InteractionHand.MAIN_HAND,stack);shot.setPos(at.add(0,1,-2));shot.setDeltaMovement(new Vec3(0,0,1.5));for(int i=0;i<3&&!shot.isRemoved();i++)shot.tick();require(shot.isRemoved()&&shot.display.isRemoved()&&service.snapshot().size()==1,"real native block first contact gives one terminal and one air-side crystal");clear();for(int x=-1;x<=1;x++)for(int y=0;y<=3;y++)block(origin.offset(x,y,0),Blocks.AIR);
            owner.setYRot(0);owner.setXRot(0);
        }
        void impact(){
            var p=at.add(0,0,0);var friend=cow(p);game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);var role=cow(p.add(1,0,0));role.entityTags().add("strict_map_object");move(other,p.add(-1,0,0));reset(other);
            var shot=service.spawn(owner,InteractionHand.MAIN_HAND,loot("ice_wand"));shot.setPos(p);service.impact(shot,p);near(friend.getHealth(),995,"actual5 FREEZE");require(friend.freeze==1&&friend.lastFreeze==5&&friend.noKnockback&&friend.getDeltaMovement().equals(Vec3.ZERO)&&friend.author==owner&&game.items().frost().cold(friend)==10,"single native impact call/no knockback/owner/+10 cold");require(other.getHealth()==1000&&role.freeze==0&&game.items().frost().cold(other)==0,"allied player and technical role protected; friendly mob eligible");clear();
            var doomed=cow(p.add(2,0,0));doomed.setHealth(1);shot=service.spawn(owner,InteractionHand.MAIN_HAND,loot("ice_wand"));shot.setPos(doomed.position());service.impact(shot,doomed.position());require(doomed.isDeadOrDying()&&doomed.getKillCredit()==owner,"native real mob death credits caster");clear();
            friend.discard();role.discard();
            var later=cow(p);shot=service.spawn(owner,InteractionHand.MAIN_HAND,loot("ice_wand"));shot.setPos(p);service.impact(shot,p);clear();later.setInvulnerableTime(0);later.damageCooldownTime=0;require(later.hurtServer(owner.level(),later.damageSources().playerAttack(other),1),"accepted later actual player hit");later.setInvulnerableTime(0);later.damageCooldownTime=0;require(later.hurtServer(owner.level(),later.damageSources().fellOutOfWorld(),10000)&&later.isDeadOrDying()&&later.getKillCredit()==other,"later actual player hit takes priority over prior ice cold cause");home();
        }
        void crystals(){
            var cell=origin.offset(0,0,0);var c=crystal(owner,cell);require(c.owner==owner&&c.center().equals(Vec3.atCenterOf(cell))&&c.hitbox.position().equals(c.center().add(0,-.6,0))&&c.hitbox.getWidth()==1.2f&&c.hitbox.getHeight()==1.2f,"published native crystal ownership/center/Interaction size");near(c.display.getXRot(),0,"floor pose initialized");require(Identifier.parse("lbc:ice_crystal_small").equals(c.display.getItemStack().get(DataComponents.ITEM_MODEL))&&owner.level().getBlockState(cell).is(Blocks.BARRIER),"native model and owned barrier");
            var victim=cow(c.hitbox.position().add(1,0,0));AttackEntityCallback.EVENT.invoker().interact(owner,owner.level(),InteractionHand.MAIN_HAND,c.hitbox,null);require(c.removed&&c.display.isRemoved()&&c.hitbox.isRemoved()&&owner.level().getBlockState(cell).isAir(),"actual AttackEntity callback explodes once and restores air snapshot");near(victim.getHealth(),995,"crystal5 ARROW");require(victim.arrow==1&&victim.author==owner&&game.items().frost().cold(victim)==10,"native crystal owner/cold");victim.discard();
            c=crystal(owner,cell);block(cell,Blocks.GOLD_BLOCK);service.forget(owner);require(owner.level().getBlockState(cell).is(Blocks.GOLD_BLOCK),"foreign block replacement preserved");block(cell,Blocks.STONE);var alternative=service.spawnCrystal(owner,InteractionHand.MAIN_HAND,loot("ice_wand"),Vec3.atCenterOf(cell));require(owner.level().getBlockState(cell).is(Blocks.STONE)&&(alternative==null||!BlockPos.containing(alternative.center()).equals(cell)),"solid original candidate preserved; fallback may use a distinct air cell");clear();block(cell,Blocks.AIR);
            c=crystal(owner,cell);for(int i=0;i<399;i++)service.tick();require(!c.removed&&c.ticks==399,"399 explicit boundary ticks retain crystal");service.tick();require(c.removed,"400th active boundary tick explodes");
            c=crystal(owner,cell);var ice=service.spawn(owner,InteractionHand.MAIN_HAND,loot("ice_wand"));ice.setPos(c.center());service.tick();require(!c.removed,"own IceShot excluded as trigger");ice.discard();
            var arrow=new Arrow(EntityTypes.ARROW,owner.level());try{arrow.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),TagParser.parseCompoundFully("{inGround:1b}")));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}arrow.setPos(c.center());owner.level().addFreshEntity(arrow);service.tick();require(!c.removed,"native grounded arrow excluded");arrow.discard();
            var snow=new Snowball(EntityTypes.SNOWBALL,owner.level());snow.setPos(c.center());owner.level().addFreshEntity(snow);service.tick();require(c.removed,"foreign native snowball proximity triggers");snow.discard();
            var ours=crystal(owner,cell);var foreign=crystal(other,origin.offset(5,0,0));owner.setShiftKeyDown(true);owner.setItemInHand(InteractionHand.MAIN_HAND,loot("ice_wand"));owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&ours.removed&&!foreign.removed,"ordinary Shift shatters only caster-owned crystals within50");owner.setShiftKeyDown(false);clear();
        }
        void trial(){
            home();var p=at.add(0,0,3);move(other,p);reset(other);game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);
            var stack=loot("ice_wand");flag(stack,"upgradable",1);owner.setItemInHand(InteractionHand.MAIN_HAND,loot("ice_wand"));owner.setItemInHand(InteractionHand.OFF_HAND,stack);
            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,8),owner);game.items().frost().add(other,owner);require(game.items().frost().cold(other)==90,"prepare real90 cold");
            require(game.items().wands().use(owner,InteractionHand.OFF_HAND),"actual offhand trial cast");var shot=service.shotSnapshot().getFirst();shot.setPos(p);service.impact(shot,p);require(game.items().frost().isFrozen(other)&&flag(stack,"upgrade_available")==1&&flag(owner.getMainHandItem(),"upgrade_available")==0,"actual impact freezes another player and qualifies only initiating offhand stack");clear();game.items().frost().clear(other);other.removeAllEffects();
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);var panel=new Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(owner.position());panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_ICE)&&Identifier.parse("lbc:tier_ss").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"actual native Academy SS promotion");panel.discard();game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);home();
        }
        void freeze(LivingEntity target){reset(target);target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,9),owner);game.items().frost().add(target,owner);require(game.items().frost().isFrozen(target),"native threshold preparation");target.removeEffect(MobEffects.RESISTANCE);target.setInvulnerableTime(0);target.damageCooldownTime=0;}
        void shatter(){
            home();move(other,at.add(6,0,0));var mob=cow(at.add(0,0,6));var splash=cow(other.position().add(-1,0,0));freeze(owner);freeze(other);freeze(mob);
            var stack=loot("academy_ice_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setShiftKeyDown(true);
            // Frozen casters cannot voluntarily cast: native WILD payload boundary is invoked explicitly.
            service.shatter(owner);owner.setShiftKeyDown(false);
            near(owner.getHealth(),760,"WILD direct caster24percent");near(other.getHealth(),750,"WILD allied player24percent plus native10 splash");near(mob.lastArrow,400,"WILD direct mob40percent native amount");near(mob.getHealth(),590,"native mob40percent plus10 splash");near(splash.getHealth(),990,"allied nearby friendly mob receives10 FREEZE splash");require(!game.items().frost().isFrozen(owner)&&!game.items().frost().isFrozen(other)&&!game.items().frost().isFrozen(mob)&&!other.hasEffect(MobEffects.SLOWNESS),"shatter releases all owned ice and Slowness");mob.discard();splash.discard();reset(owner);reset(other);home();
            freeze(other);owner.setItemInHand(InteractionHand.MAIN_HAND,loot("academy_ice_wand"));owner.setShiftKeyDown(true);game.items().mana().set(owner,100000);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&!game.items().frost().isFrozen(other),"actual Academy Shift dispatch shatters frozen allied player");near(game.items().mana().get(owner),99800,"actual Academy Shift200 mana");near(other.getHealth(),750,"actual Academy Shift native24percent plus10 splash");owner.setShiftKeyDown(false);reset(other);home();
        }
        void boundaries(){
            var c=crystal(owner,origin);var shot=service.spawn(owner,InteractionHand.MAIN_HAND,loot("ice_wand"));var from=shot.position();int ticks=c.ticks;game.items().timeStop().begin(owner.level(),100);
            try{for(int i=0;i<10;i++){shot.tick();service.tick();}require(shot.position().equals(from)&&c.ticks==ticks,"actual native Time Stop pauses flight and crystal active age");}finally{game.items().timeStop().end(owner.level());}clear();
            c=crystal(owner,origin);shot=service.spawn(owner,InteractionHand.MAIN_HAND,loot("ice_wand"));stopper=new Marker(EntityTypes.MARKER,owner.level());stopper.setPos(at);stopper.entityTags().add("stopper_magic");owner.level().addFreshEntity(stopper);game.items().magic().loaded(stopper);service.tick();require(c.removed&&shot.isRemoved()&&owner.level().getBlockState(origin).isAir(),"actual antimagic deletes actors and restores barrier");game.items().magic().unloaded(stopper);stopper.discard();stopper=null;
            c=crystal(owner,origin);shot=service.spawn(owner,InteractionHand.MAIN_HAND,loot("ice_wand"));service.forget(owner);require(c.removed&&shot.isRemoved()&&service.count()==0,"participant forget closes native actors");
        }
        @Override public void close(){
            if(stopper!=null){game.items().magic().unloaded(stopper);stopper.discard();}game.items().timeStop().end(owner.level());clear();
            for(var cow:cows){game.items().frost().clear(cow);game.items().indirectKills().clear(cow);cow.discard();}game.items().frost().clear(owner);game.items().frost().clear(other);
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb||e.entityTags().contains("magic_academy")))fresh.add(e);});fresh.forEach(Entity::discard);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());
            game.server().getScoreboard().removePlayerTeam(team);game.config().offhandWands=offhand;game.config().lbcSkills=skills;saved.restore(game);otherSaved.restore(game);game.players().save();game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.init();f.presentation();f.flight();f.impact();f.crystals();f.trial();f.shatter();f.boundaries();}SkyWars2.LOGGER.info("[check] PASS ice-wand (native actors/input/crafting/trial; explicit manual boundary ticks)");}
    private static PlayerState stopSaved;
    private static String stopEpoch;
    private static LbcIceWand.IceShot stopShot;
    private static LbcIceWand.Crystal stopCrystal;
    private static final Map<BlockPos,MapManager.BlockSnapshot> stopBlocks=new LinkedHashMap<>();
    private static final Set<Entity> stopBefore=Collections.newSetFromMap(new IdentityHashMap<>());
    public static void stopProbe(GameController game){
        require(stopSaved==null,"exclusive actual-stop probe");var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null&&game.canUseGameplayItems(owner),"live stop-probe owner");
        stopSaved=new PlayerState(game,owner);owner.level().getAllEntities().forEach(stopBefore::add);
        try{
            owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);
            var stack=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/ice_wand").getFirst();owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"actual native cast before match stop");stopShot=game.items().iceWand().shotSnapshot().getFirst();
            var cell=owner.blockPosition().offset(2,5,0);require(owner.level().hasChunkAt(cell),"resident stop crystal");
            for(var pos:List.of(cell,cell.below()))stopBlocks.put(pos,game.maps().snapshot(owner.level(),pos));
            owner.level().setBlock(cell,Blocks.AIR.defaultBlockState(),2);owner.level().setBlock(cell.below(),Blocks.STONE.defaultBlockState(),2);
            stopCrystal=game.items().iceWand().spawnCrystal(owner,InteractionHand.MAIN_HAND,stack,Vec3.atCenterOf(cell));
            require(stopCrystal!=null&&!stopShot.isRemoved()&&owner.level().getBlockState(cell).is(Blocks.BARRIER),"live native shot/crystal/owned barrier before actual stop");stopEpoch=game.items().epoch();
        }catch(RuntimeException|Error error){restoreStop(game);throw error;}
        SkyWars2.LOGGER.info("[check] PASS ice-wand-stop-probe");
    }
    public static void cleanup(GameController game){
        try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().iceWand().count()==0&&stopShot.isRemoved()&&stopShot.display.isRemoved()
                &&stopCrystal.removed&&stopCrystal.display.isRemoved()&&stopCrystal.hitbox.isRemoved()&&stopSaved.player.level().getBlockState(BlockPos.containing(stopCrystal.center())).isAir(),"actual match stop clears actors and restores native owned barrier");}
        finally{restoreStop(game);}
        SkyWars2.LOGGER.info("[check] PASS ice-wand-cleanup");
    }
    private static void restoreStop(GameController game){
        if(stopSaved==null)return;var owner=stopSaved.player;game.items().iceWand().forget(owner);
        for(var entry:stopBlocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());stopBlocks.clear();
        var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!stopBefore.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);stopBefore.clear();
        stopSaved.restore(game);game.players().save();stopSaved=null;stopShot=null;stopCrystal=null;stopEpoch=null;
    }
}
