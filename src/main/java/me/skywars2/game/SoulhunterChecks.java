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
public final class SoulhunterChecks {
    private SoulhunterChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Soulhunter: "+why);}
    private static Object copy(Object value){if(value instanceof int[] a)return a.clone();if(value instanceof Set<?> s)return new HashSet<>(s);return value;}
    private static Map<Field,Object> fields(Object target){
        var result=new LinkedHashMap<Field,Object>();
        try{for(var f:target.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))result.put(f,copy(f.get(target)));}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}return result;
    }
    private static void fields(Object target,Map<Field,Object> values){try{for(var e:values.entrySet())e.getKey().set(target,copy(e.getValue()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static Map<Field,Object> attackers(ServerPlayer player){
        var saved=new LinkedHashMap<Field,Object>();
        try{for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp")){
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
        final Map<Field,Object> profileFields,attackerFields;final Integer maceSecond;final boolean shift,sprinting,ground;final int visualRegen,cowKills;final net.minecraft.world.scores.PlayerTeam team;
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<Stat<Identifier>,Integer> custom=new LinkedHashMap<>();
        final Map<String,Integer> scores=new LinkedHashMap<>();
        final Map<Stat<Item>,Integer> crafted=new LinkedHashMap<>();
        Saved(GameController game,ServerPlayer player){
            this.player=player;maceSecond=cadence(game.items().soulhunter()).get(player);team=player.getTeam();state=new GameChecks.WandThirdPlayerState(player,game.items().mana());profile=game.players().get(player.getUUID());profileFields=fields(profile);attackerFields=attackers(player);
            for(String name:List.of("mace_1","mace_2","mace_3","mace_4","effect_ghosting","void_resistance"))scores.put(name,game.items().mana().score(player,name).get());
            shift=player.isShiftKeyDown();sprinting=player.isSprinting();ground=player.onGround();visualRegen=game.items().mana().regeneration(player);cowKills=player.getStats().getValue(Stats.ENTITY_KILLED.get(EntityTypes.COW));
            for(var adv:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);advancements.put(adv,done);}
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);custom.put(stat,player.getStats().getValue(stat));}
            for(var item:BuiltInRegistries.ITEM)for(var type:List.of(Stats.ITEM_CRAFTED,Stats.ITEM_USED,Stats.ITEM_PICKED_UP,Stats.ITEM_DROPPED)){var stat=type.get(item);crafted.put(stat,player.getStats().getValue(stat));}
        }
        void restore(GameController game){
            advancements.forEach((adv,done)->{for(var key:adv.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(adv,key);else player.getAdvancements().revoke(adv,key);});
            var board=game.server().getScoreboard();if(player.getTeam()!=null)board.removePlayerFromTeam(player.getScoreboardName(),player.getTeam());if(team!=null)board.addPlayerToTeam(player.getScoreboardName(),team);fields(profile,profileFields);state.restore(game);player.setShiftKeyDown(shift);player.setSprinting(sprinting);player.setOnGround(ground);fields(player,attackerFields);game.items().mana().score(player,"manaregen_visual").set(visualRegen);
            scores.forEach((name,value)->game.items().mana().score(player,name).set(value));if(maceSecond==null)cadence(game.items().soulhunter()).remove(player);else cadence(game.items().soulhunter()).put(player,maceSecond);
            custom.forEach((stat,value)->player.getStats().setValue(player,stat,value));crafted.forEach((stat,value)->player.getStats().setValue(player,stat,value));
            player.getStats().setValue(player,Stats.ENTITY_KILLED.get(EntityTypes.COW),cowKills);game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcSoulhunter mace;final LbcSoulless souls;final LbcMana mana;final ServerPlayer owner,other;
        final Saved saved,otherSaved;final Map<Field,Object> config;final BlockPos center;final Vec3 at;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Entity> actors=new ArrayList<>();final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;
        Fixture(GameController game){
            this.game=game;mace=game.items().soulhunter();souls=game.items().soulless();mana=game.items().mana();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&owner.level()==other.level()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active same-dimension participants");
            require(game.server().tickRateManager().runsNormally()&&souls.count()==0&&souls.ghostCount()==0&&souls.vexSnapshot().isEmpty()&&!game.items().timeStop().stopped(owner.level()),"exclusive empty Soul state and normal clock");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0&&cleanAttribution(game,p),"closed menu, no pending frost/combat");
            require(game.server().getScoreboard().getPlayerTeam("soul_fixture")==null,"exclusive fixture team");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            center=owner.blockPosition().offset(0,32,0);at=Vec3.atBottomCenterOf(center);require(center.getY()+14<owner.level().getMaxY(),"region below ceiling");
            team=game.server().getScoreboard().addPlayerTeam("soul_fixture");team.setAllowFriendlyFire(false);
        }
        void init(){
            var box=new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(center.offset(-12,-12,-12)),Vec3.atLowerCornerOf(center.offset(13,14,13)));
            require(owner.level().getEntities(owner,box,e->e!=other).isEmpty(),"no unrelated entities in terrain fixture");
            for(var p:BlockPos.betweenClosed(center.offset(-12,-12,-12),center.offset(12,13,12))){require(owner.level().hasChunkAt(p),"resident region");blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));}
            for(var p:blocks.keySet())block(p,Blocks.AIR);
            game.config().ranked=false;game.config().lbcSkills=false;game.config().offhandWands=true;
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.setSprinting(false);p.setOnGround(false);p.removeAllEffects();p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                p.entityTags().removeAll(Set.of("time_stopped","sin_greed_active","unstamined"));mana.noMagic(p,0);mana.set(p,100000);
                for(String score:List.of("mace_1","mace_2","mace_3","mace_4","effect_ghosting","void_resistance"))mana.score(p,score).set(0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);
            }
            move(owner,at);move(other,at.add(11,0,0));game.items().magic().flushCleansing();
        }
        void block(BlockPos p,Block b){require(blocks.containsKey(p),"terrain write within saved region "+p);owner.level().setBlock(p,b.defaultBlockState(),2);}
        void move(ServerPlayer p,Vec3 v){p.teleportTo(p.level(),v.x,v.y,v.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1&&list.getFirst().getCount()==1,"singleton leaf "+leaf);return list.getFirst();}
        void held(String leaf,InteractionHand hand){owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,loot(leaf));owner.setShiftKeyDown(false);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}
        Cow cow(Vec3 v){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(v);c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);c.setHealth(20);c.setNoAi(true);c.setNoGravity(true);owner.level().addFreshEntity(c);actors.add(c);return c;}
        static int data(ItemStack stack,String key){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
        void presentation(){
            var stack=loot("soulhunter_mace");require(stack.is(LbcSoulhunter.MACE)&&LbcSoulhunter.isMace(stack)&&stack.getItem() instanceof net.minecraft.world.item.MaceItem,"native mace class/identity");
            require(Identifier.parse("lbc:soulhunter_mace").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_ss").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source SS mace model");
            require(stack.has(DataComponents.ENCHANTMENTS)&&stack.has(DataComponents.ATTRIBUTE_MODIFIERS),"native authored enchantments/attributes");
            var wand=loot("soulcorruption_wand");require(LbcSoulless.isWand(wand)&&Identifier.parse("lbc:soulcorruptwand").equals(wand.get(DataComponents.ITEM_MODEL)),"ordinary prerequisite identity/model");
            for(int i=1;i<=6;i++){var reward=loot("metasola_"+i);require(LbcSoulless.material(reward)==i&&Identifier.parse("lbc:metasola_"+i).equals(reward.get(DataComponents.ITEM_MODEL))&&data(reward,"gadget")==1,"functional reward identity/model/gadget "+i);}
        }
        void craft(){
            var table=center.offset(0,-1,0);block(table,Blocks.CRAFTING_TABLE);
            for(boolean bench:new boolean[]{false,true}){
                owner.closeContainer();owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();
                if(bench)owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(owner.level(),table)),net.minecraft.network.chat.Component.literal("soul fixture")));
                var menu=(net.minecraft.world.inventory.AbstractCraftingMenu)owner.containerMenu;var slots=menu.getInputGridSlots();
                slots.getFirst().setByPlayer(loot("halloween_mace").copyWithCount(2));slots.getLast().setByPlayer(loot("soulcorruption_wand").copyWithCount(2));
                for(int i=0;i<3;i++)menu.slotsChanged(owner.getInventory());
                require(menu.getResultSlot().getItem().is(LbcSoulhunter.MACE)&&slots.getFirst().getItem().getCount()==2&&slots.getLast().getItem().getCount()==2&&menu.getCarried().isEmpty(),"native preview no donor consumption "+bench);
                slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"reject extra ingredient");slots.get(1).setByPlayer(ItemStack.EMPTY);
                var donor=slots.getLast().getItem().copy();slots.get(1).setByPlayer(donor.copy());require(menu.getResultSlot().getItem().isEmpty(),"reject duplicate donor");slots.get(1).setByPlayer(ItemStack.EMPTY);
                GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
                require(menu.getCarried().is(LbcSoulhunter.MACE)&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"native ResultSlot consumes one each "+bench);
                menu.setCarried(ItemStack.EMPTY);slots.forEach(slot->slot.setByPlayer(ItemStack.EMPTY));owner.closeContainer();
            }
        }
        void movement(){
            for(var hand:InteractionHand.values()){
                held("soulhunter_mace",hand);mana.set(owner,0);mana.score(owner,"mace_1").set(2);owner.setOnGround(false);owner.setSprinting(true);owner.setYRot(0);owner.setXRot(30);owner.setDeltaMovement(Vec3.ZERO);
                var look=owner.getLookAngle();mace.tickPlayer(owner);require(owner.getDeltaMovement().distanceToSqr(look.scale(.2))<1e-10&&mana.score(owner,"mace_1").get()==1,"airborne sprint retains pitch and spends one "+hand);
                mace.tickPlayer(owner);require(mana.score(owner,"mace_1").get()==0&&owner.entityTags().contains("unstamined"),"zero stamina exhausts");var stopped=owner.getDeltaMovement();mace.tickPlayer(owner);require(owner.getDeltaMovement().equals(stopped),"exhausted airborne sprint blocked");
                owner.setSprinting(false);owner.setOnGround(true);owner.setItemInHand(hand,ItemStack.EMPTY);for(int i=0;i<59;i++)mace.tickPlayer(owner);require(mana.score(owner,"mace_1").get()==59&&owner.entityTags().contains("unstamined"),"unequipped grounded recharge stays exhausted at59");mace.tickPlayer(owner);require(mana.score(owner,"mace_1").get()==60&&!owner.entityTags().contains("unstamined"),"recharge60 clears exhaustion");mace.tickPlayer(owner);require(mana.score(owner,"mace_1").get()==60,"recharge capped60");
                held("soulhunter_mace",hand);owner.setSprinting(false);owner.setXRot(90);owner.setDeltaMovement(Vec3.ZERO);mace.tickPlayer(owner);require(mana.score(owner,"mace_3").get()==0,"looking down does not jump");
                owner.jumpFromGround();require(Math.abs(owner.getDeltaMovement().y-1.62)<1e-6&&mana.score(owner,"mace_3").get()==5,"actual native jump hook adds1.2 and cooldown5");
                require(mace.hud(owner).getString().contains("\uE197")&&mace.hud(owner).getString().contains("\uE199")&&mace.hud(owner).getString().contains("5"),"native HUD stamina/jump cooldown");
                owner.setDeltaMovement(Vec3.ZERO);owner.jumpFromGround();require(Math.abs(owner.getDeltaMovement().y-.42)<1e-6,"cooldown prevents repeated boost");
                game.items().timeStop().begin(owner.level(),40);try{mace.second(owner);require(mana.score(owner,"mace_3").get()==5,"Time Stop pauses cooldown");}finally{game.items().timeStop().end(owner.level());}
                for(int i=4;i>=0;i--){mace.second(owner);require(mana.score(owner,"mace_3").get()==i,"explicit cooldown second "+i);}
            }
        }
        void nativeMelee(){
            held("soulhunter_mace",InteractionHand.MAIN_HAND);var victim=cow(at.add(0,0,2));victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);victim.setHealth(200);
            try{var counter=net.minecraft.world.entity.LivingEntity.class.getDeclaredField("attackStrengthTicker");var swap=net.minecraft.world.entity.LivingEntity.class.getDeclaredField("itemSwapTicker");counter.setAccessible(true);swap.setAccessible(true);int previous=counter.getInt(owner),previousSwap=swap.getInt(owner);
                try{owner.fallDistance=0;counter.setInt(owner,100);swap.setInt(owner,100);owner.attack(victim);float grounded=200-victim.getHealth();require(grounded>0,"native first melee hit lands");
                    victim.setHealth(200);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;owner.fallDistance=5;owner.setOnGround(false);counter.setInt(owner,100);swap.setInt(owner,100);owner.attack(victim);
                    require(200-victim.getHealth()>grounded&&victim.getKillCredit()==owner,"native MaceItem falling smash exceeds grounded strike and owns hit");
                }finally{counter.setInt(owner,previous);swap.setInt(owner,previousSwap);}
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void dash(){
            held("soulhunter_mace",InteractionHand.MAIN_HAND);mana.set(owner,0);owner.setXRot(20);owner.setYRot(35);owner.setDeltaMovement(new Vec3(.3,.4,.5));
            owner.getMainHandItem().getItem().use(owner.level(),owner,InteractionHand.MAIN_HAND);
            require(owner.getDeltaMovement().distanceToSqr(owner.getLookAngle().scale(3))<1e-10&&mana.score(owner,"mace_2").get()==5&&mana.score(owner,"mace_4").get()==0&&mana.get(owner)==0,"native RMB dash replaces velocity; separate5s cooldown");
            var velocity=owner.getDeltaMovement();var stack=owner.getMainHandItem();stack.getItem().onUseTick(owner.level(),owner,stack,Integer.MAX_VALUE-1);require(owner.getDeltaMovement().equals(velocity)&&mana.score(owner,"mace_4").get()==0,"native held-use dash cooldown cannot splash");
            for(int i=0;i<5;i++)mace.second(owner);owner.setDeltaMovement(Vec3.ZERO);stack.getItem().onUseTick(owner.level(),owner,stack,Integer.MAX_VALUE-101);require(owner.getDeltaMovement().distanceToSqr(owner.getLookAngle().scale(3))<1e-10&&mana.score(owner,"mace_2").get()==5,"actual native maintained-use rearms after five second boundaries");
            owner.stopUsingItem();mace.purify(owner);require(mana.score(owner,"mace_2").get()==0,"purify dash cooldown");
        }
        void splash(){
            held("soulhunter_mace",InteractionHand.MAIN_HAND);owner.setShiftKeyDown(true);var enemy=cow(at.add(0,0,4));var role=cow(at.add(1,0,4));role.entityTags().add("strict_map_object");
            move(other,at.add(0,0,3));game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            for(var b:BlockPos.betweenClosed(center.offset(-11,-1,-11),center.offset(11,6,11)))block(b,Blocks.STONE);
            // A real chest verifies native destroy-with-drops; its position lies inside the source mask.
            var chest=center.offset(2,0,0);block(chest,Blocks.CHEST);((ChestBlockEntity)owner.level().getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,3));
            require(mace.use(owner,InteractionHand.MAIN_HAND)&&mana.score(owner,"mace_4").get()==20&&mana.score(owner,"mace_2").get()==0,"Shift RMB uses only20s splash");
            require(enemy.getHealth()==5&&enemy.getKillCredit()==owner&&other.getHealth()==20&&role.getHealth()==20&&owner.getHealth()==20,"splash15 enemy with self/ally/role guards");
            for(var b:BlockPos.betweenClosed(center.offset(-11,-1,-11),center.offset(11,6,11))){int x=Math.abs(b.getX()-center.getX()),z=Math.abs(b.getZ()-center.getZ()),y=b.getY()-center.getY();boolean mask=y>=0&&y<=5&&((x<=3&&z<=10)||(x<=5&&z<=9)||(x<=6&&z<=8)||(x<=7&&z<=7)||(x<=8&&z<=6)||(x<=9&&z<=5)||(x<=10&&z<=3));require(owner.level().getBlockState(b).isAir()==mask,"exact source seven-rectangle mask "+b);}
            boolean diamonds=false;for(var e:owner.level().getAllEntities())if(e instanceof net.minecraft.world.entity.item.ItemEntity drop&&drop.getItem().is(Items.DIAMOND)&&drop.getItem().getCount()==3)diamonds=true;require(diamonds,"native chest contents survive destructive source fill as drops");
            enemy.setInvulnerableTime(0);enemy.damageCooldownTime=0;mace.use(owner,InteractionHand.MAIN_HAND);require(enemy.getHealth()==5,"held splash cooldown blocks repeated damage");
        }
        void supportAttribution(){
            held("soulhunter_mace",InteractionHand.MAIN_HAND);owner.setShiftKeyDown(true);
            var first=cow(Vec3.atBottomCenterOf(center.offset(0,1,3)));var newer=cow(Vec3.atBottomCenterOf(center.offset(1,1,3)));first.setOnGround(true);newer.setOnGround(true);block(center.offset(0,0,3),Blocks.STONE);block(center.offset(1,0,3),Blocks.STONE);
            move(other,at.add(0,0,4));other.entityTags().add("sin_greed_active");require(mace.use(owner,InteractionHand.MAIN_HAND)&&other.getHealth()==20,"splash native Greed damage guard");other.entityTags().remove("sin_greed_active");game.items().indirectKills().finishSupportRemoval();
            first.setInvulnerableTime(0);first.damageCooldownTime=0;require(first.hurtServer(owner.level(),first.damageSources().fellOutOfWorld(),1000)&&first.isDeadOrDying()&&first.getKillCredit()==owner,"source splash/support inherits terminal void death");
            newer.setInvulnerableTime(0);newer.damageCooldownTime=0;require(newer.hurtServer(owner.level(),newer.damageSources().playerAttack(other),1),"newer accepted native player hit");newer.setInvulnerableTime(0);newer.damageCooldownTime=0;require(newer.hurtServer(owner.level(),newer.damageSources().fellOutOfWorld(),1000)&&newer.isDeadOrDying()&&newer.getKillCredit()==other,"newer real-hit priority supersedes source support cause");
        }
        void ghosting(){
            held("soulcorruption_wand",InteractionHand.OFF_HAND);var inside=cow(owner.getEyePosition().add(0,0,24.9));var outside=cow(owner.getEyePosition().add(0,0,25.1));var role=cow(at.add(0,0,3));role.entityTags().add("strict_map_object");
            move(other,at.add(0,0,4));game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            mana.set(owner,1499);require(!game.items().wands().use(owner,InteractionHand.OFF_HAND)&&mana.get(owner)==1499&&souls.ghostCount()==0,"strict1500 offhand admission");
            mana.set(owner,3000);require(game.items().wands().use(owner,InteractionHand.OFF_HAND)&&mana.get(owner)==1500&&mana.score(owner,"effect_ghosting").get()==5&&mana.score(other,"effect_ghosting").get()==5&&mana.score(inside,"effect_ghosting").get()==5&&mana.score(outside,"effect_ghosting").get()==0&&mana.score(role,"effect_ghosting").get()==0,"WILD eye radius25 caster/ally and role guard");
            require(game.items().wands().use(owner,InteractionHand.OFF_HAND)&&mana.get(owner)==0&&mana.score(other,"effect_ghosting").get()==10,"repeat adds5 rather than resetting");
            var gravity=other.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);require(Math.abs(gravity.getValue()-gravity.getBaseValue()*.15)<1e-8,"native ghost gravity multiplied .15");
            other.setSprinting(true);other.setOnGround(false);other.setXRot(80);other.setYRot(0);other.setDeltaMovement(Vec3.ZERO);souls.tick();require(other.hasEffect(MobEffects.INVISIBILITY)&&other.getDeltaMovement().distanceToSqr(new Vec3(0,0,.2))<1e-10,"ghost sprint horizontal pitch-independent .2");other.setSprinting(false);
            var v=other.getDeltaMovement();other.entityTags().add("sin_greed_active");other.setSprinting(true);souls.tick();require(other.getDeltaMovement().equals(v),"active Greed pauses incoming ghost velocity");other.entityTags().remove("sin_greed_active");other.setSprinting(false);
            game.items().timeStop().begin(owner.level(),40);try{for(int i=0;i<20;i++)souls.tick();require(mana.score(other,"effect_ghosting").get()==10,"Time Stop pauses ghost duration");}finally{game.items().timeStop().end(owner.level());}
            // Ten source seconds are explicitly dispatched; these calls do not advance native time.
            for(int i=0;i<199;i++)souls.tick();require(mana.score(other,"effect_ghosting").get()==0&&Math.abs(gravity.getValue()-gravity.getBaseValue())<1e-8,"ten active seconds expire and remove owned modifier");
            mana.noMagic(owner,1);mana.set(owner,1500);require(!game.items().wands().use(owner,InteractionHand.OFF_HAND)&&mana.get(owner)==1500,"personal noMagic admission");mana.noMagic(owner,0);
        }
        void vexReward(){
            var victim=cow(at.add(0,0,3));souls.apply(owner,victim);require(victim.hurtServer(owner.level(),victim.damageSources().playerAttack(other),1)&&victim.hurtTime==10,"real native first hit HurtTime10");souls.tick();
            require(souls.vexSnapshot().size()==1,"first accepted ghost hit emits one native Vex");var vex=souls.vexSnapshot().getFirst();require(vex.getType()==EntityTypes.VEX&&souls.owner(vex)==owner,"native Vex owned by ghost caster");
            victim.hurtTime=9;souls.tick();require(souls.vexSnapshot().size()==1,"HurtTime9 adds no Vex");
            souls.apply(other,vex);require(vex.isDeadOrDying()||vex.isRemoved(),"ordinary Vex conversion actually kills Vex");
            var rewards=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();for(var e:owner.level().getAllEntities())if(!before.contains(e)&&e instanceof net.minecraft.world.entity.item.ItemEntity drop&&LbcSoulless.material(drop.getItem())>=1)rewards.add(drop);
            require(rewards.size()==1&&rewards.getFirst().getItem().getCount()==1&&rewards.getFirst().isNoGravity()&&rewards.getFirst().getDeltaMovement().distanceToSqr(new Vec3(0,-.06,0))<1e-10,"one six-way native unowned reward with source falling motion");
            var reward=rewards.getFirst().getItem().copy();other.setItemInHand(InteractionHand.MAIN_HAND,reward);require(souls.use(other,InteractionHand.MAIN_HAND)&&souls.snapshot().getFirst().owner==other,"reward ownership begins with actual deploying holder");
        }
        void hosts(){
            int[] hp={25,50,20,15,500,40};var initial=new Vec3(.12345,.23456,-.34567);
            for(int type=1;type<=6;type++){
                var field=souls.spawn(owner,at.add(0,3,0),initial,type);require(field!=null&&field.owner==owner&&souls.owner(field.pig)==owner&&souls.owner(field.display)==owner,"owned paired host/display "+type);
                require(field.pig.getHealth()==hp[type-1]&&field.pig.getMaxHealth()==hp[type-1]&&!field.pig.isNoGravity()&&!field.pig.isNoAi(),"source vulnerable full HP native-physics host "+type);
                var expected=new Vec3(Math.floor(initial.x*2000)*.001,Math.floor(initial.y*2000)*.001,Math.floor(initial.z*2000)*.001);require(field.pig.getDeltaMovement().distanceToSqr(expected)<1e-10,"source truncated item motion*2 "+type);
                var pos=field.pig.position();field.pig.tick();require(!field.pig.position().equals(pos)&&field.pig.getDeltaMovement().y<expected.y,"actual native host physics/gravity "+type);
                souls.tickField(field);require(field.display.position().distanceToSqr(field.pig.position())<1e-10,"display follows its exact host "+type);
                int age=field.age;var frozen=field.pig.position();game.items().timeStop().begin(owner.level(),40);try{souls.tickField(field);field.pig.tick();require(field.age==age&&field.pig.position().equals(frozen),"Time Stop freezes paired lifetime/physics "+type);}finally{game.items().timeStop().end(owner.level());}
                souls.unloaded(field.display);require(field.pig.isRemoved()&&field.display.isRemoved(),"one-half unload retires pair "+type);
            }
            require(souls.count()==0,"all pairs retired");
        }
        void fieldActions(){
            // These probes advance the public field dispatch rather than writing private ages.
            var damage=souls.spawn(owner,at,Vec3.ZERO,1);var enemy=cow(at.add(0,0,3));for(int i=0;i<99;i++)souls.tickField(damage);require(enemy.getHealth()==20,"damage warmup99 ticks / age179 harmless");souls.tickField(damage);require(enemy.getHealth()==16&&enemy.getKillCredit()==owner,"type1 native magic4 at boundary200");souls.unloaded(damage.pig);
            var manaField=souls.spawn(owner,at,Vec3.ZERO,4);mana.set(owner,100);for(int i=0;i<39;i++)souls.tickField(manaField);require(mana.get(owner)==100,"mana warmup39 harmless");souls.tickField(manaField);require(mana.get(owner)==500,"mana field sets500 at40");mana.set(owner,600);souls.tickField(manaField);require(mana.get(owner)==600,"mana field leaves above500");souls.unloaded(manaField.pig);
            var lift=souls.spawn(owner,at,Vec3.ZERO,3);owner.setDeltaMovement(Vec3.ZERO);for(int i=0;i<99;i++)souls.tickField(lift);require(owner.getDeltaMovement().lengthSqr()==0,"lift warmup99 harmless");souls.tickField(lift);require(Math.abs(owner.getDeltaMovement().y-.08)<1e-10&&owner.getDeltaMovement().horizontalDistanceSqr()>0,"type3 WILD applies to caster with lateral/up/forward motion");souls.unloaded(lift.pig);
            owner.removeAllEffects();var shield=souls.spawn(owner,at,Vec3.ZERO,6);for(int i=0;i<99;i++)souls.tickField(shield);require(!owner.hasEffect(MobEffects.RESISTANCE),"shield warmup99 harmless");souls.tickField(shield);require(owner.hasEffect(MobEffects.RESISTANCE)&&owner.getEffect(MobEffects.RESISTANCE).getAmplifier()==4&&mana.score(owner,"void_resistance").get()>=2,"type6 native ResistanceV and void floor at100");souls.unloaded(shield.pig);
            owner.setHealth(1);var heal=souls.spawn(owner,at,Vec3.ZERO,2);for(int i=0;i<39;i++)souls.tickField(heal);require(owner.getHealth()==1,"healing warmup39 ticks / age59 harmless");souls.tickField(heal);require(owner.getHealth()>1,"native Instant HealthIII at80");souls.unloaded(heal.pig);
        }
        void fireField(){
            var source=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:light_sphere_21x21")).orElseThrow();var origin=center.offset(-10,-10,-10);
            var palette=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
            for(var type:List.of(Blocks.FIRE,Blocks.LAVA,Blocks.VOID_AIR))for(var cell:source.filterBlocks(origin,settings,type))palette.put(cell.pos(),cell.state());
            require(palette.size()==4945,"source type5 exact sparse template retained");
            for(var pos:BlockPos.betweenClosed(origin,origin.offset(20,20,20)))block(pos,Blocks.STONE);
            var enemy=cow(at.add(0,0,3));var field=souls.spawn(owner,at,Vec3.ZERO,5);
            for(int i=0;i<199;i++)souls.tickField(field);require(enemy.getHealth()==20,"type5 warmup199 harmless");souls.tickField(field);
            require(enemy.isDeadOrDying()&&enemy.getKillCredit()==owner,"type5 WILD native in_fire20 inherits field owner");
            require(Math.abs(field.pig.getY()-(at.y-.2))<1e-8,"type5 source host lowering .2");
            int changed=0,retained=0;for(var pos:BlockPos.betweenClosed(origin,origin.offset(20,20,20))){var state=owner.level().getBlockState(pos);if(state.is(Blocks.STONE)){retained++;continue;}changed++;var expected=palette.get(pos);require(expected!=null&&(state.getBlock()==expected.getBlock()||expected.is(Blocks.FIRE)&&state.is(Blocks.AIR)),"fractional placement changes only sparse source palette cells "+pos+" actual="+state+" expected="+expected);}
            require(changed>0&&retained>0,"one native integrity.1 placement both selects and omits cells");souls.unloaded(field.pig);
        }
        void voidProtection(){
            mana.score(owner,"void_resistance").set(2);move(owner,new Vec3(at.x,-1,at.z));
            game.items().timeStop().begin(owner.level(),40);try{mana.voidProtectionSecond(owner);require(owner.getY()==-1&&mana.score(owner,"void_resistance").get()==2,"Time Stop pauses void protection teleport and counter");}finally{game.items().timeStop().end(owner.level());}
            mana.secondPlayer(owner);
            require(owner.getY()==320&&mana.score(owner,"void_resistance").get()==1,"actual mana second returns y-1 to320 then decrements2->1");
            mana.secondPlayer(owner);require(owner.getY()==320&&mana.score(owner,"void_resistance").get()==0,"second void protection boundary1->0");
            move(owner,new Vec3(at.x,-1,at.z));mana.secondPlayer(owner);require(owner.getY()==-1&&mana.score(owner,"void_resistance").get()==0,"expired void protection cannot teleport");
            move(owner,at);
        }
        void fieldGuards(){
            var field=souls.spawn(owner,at,Vec3.ZERO,1);move(other,at.add(0,0,3));other.entityTags().add("sin_greed_active");for(int i=0;i<100;i++)souls.tickField(field);require(other.getHealth()==20,"Greed rejects periodic field damage");other.entityTags().remove("sin_greed_active");
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);actors.add(marker);game.items().magic().loaded(marker);
            try{souls.tickField(field);require(field.pig.isRemoved()&&field.display.isRemoved()&&souls.count()==0,"real antimagic retires both field entities");}finally{game.items().magic().unloaded(marker);marker.discard();}
            var foreign=souls.spawn(other,at,Vec3.ZERO,4);var owned=souls.spawn(owner,at,Vec3.ZERO,4);souls.forget(owner);require(owned.pig.isRemoved()&&owned.display.isRemoved()&&!foreign.pig.isRemoved()&&souls.count()==1,"owner cleanup preserves foreign pair");souls.unloaded(foreign.pig);
        }
        @Override public void close(){
            owner.stopUsingItem();souls.stop();mace.purify(owner);mace.purify(other);game.items().timeStop().end(owner.level());
            if(owner.containerMenu instanceof net.minecraft.world.inventory.AbstractCraftingMenu menu){menu.setCarried(ItemStack.EMPTY);menu.getInputGridSlots().forEach(slot->slot.setByPlayer(ItemStack.EMPTY));}owner.closeContainer();
            for(var e:actors){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
            for(var p:List.of(owner,other)){game.items().frost().clear(p);game.items().indirectKills().clear(p);}
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());fields(game.config(),config);saved.restore(game);otherSaved.restore(game);game.server().getScoreboard().removePlayerTeam(team);
            freshItems(owner,before);game.players().save();game.items().magic().flushCleansing();
        }
    }
    @SuppressWarnings("unchecked") private static Map<ServerPlayer,Integer> cadence(LbcSoulhunter service){try{var field=LbcSoulhunter.class.getDeclaredField("seconds");field.setAccessible(true);return (Map<ServerPlayer,Integer>)field.get(service);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void freshItems(ServerPlayer player,Set<Entity> before){var fresh=new ArrayList<Entity>();player.level().getAllEntities().forEach(e->{if(!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);}
    private record Case(String name,java.util.function.Consumer<Fixture> action){}
    public static void run(GameController game){
        var failures=new ArrayList<String>();for(var test:List.of(new Case("presentation",Fixture::presentation),new Case("native crafts",Fixture::craft),new Case("native movement",Fixture::movement),new Case("native first melee/smash",Fixture::nativeMelee),new Case("native dash",Fixture::dash),new Case("splash/mask",Fixture::splash),new Case("support attribution",Fixture::supportAttribution),new Case("WILD ghosting",Fixture::ghosting),new Case("native Vex reward",Fixture::vexReward),new Case("six native hosts",Fixture::hosts),new Case("field boundaries",Fixture::fieldActions),new Case("type5 source template",Fixture::fireField),new Case("void protection",Fixture::voidProtection),new Case("field guards",Fixture::fieldGuards))){try(var f=new Fixture(game)){f.init();test.action.accept(f);}catch(RuntimeException error){failures.add(test.name+": "+error.getMessage());}}
        require(failures.isEmpty(),String.join("; ",failures));SkyWars2.LOGGER.info("[check] PASS soulhunter (native menus/jump/item use, source mask/drop, WILD ghosting/Vex rewards, six hosts/physics and explicit field boundaries)");
    }
    private static Saved stopSaved;private static String stopEpoch;private static List<LbcSoulless.Field> stopFields;private static Set<Entity> stopBefore;
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));var souls=game.items().soulless();require(stopSaved==null&&owner!=null&&game.canUseGameplayItems(owner)&&souls.count()==0&&souls.ghostCount()==0&&souls.vexSnapshot().isEmpty()&&cleanAttribution(game,owner),"clean exclusive actual-stop probe");
        stopSaved=new Saved(game,owner);stopBefore=Collections.newSetFromMap(new IdentityHashMap<>());owner.level().getAllEntities().forEach(stopBefore::add);
        try{for(int i=1;i<=6;i++)require(souls.spawn(owner,owner.getEyePosition(),Vec3.ZERO,i)!=null,"stop probe spawn"+i);souls.apply(owner,owner);stopFields=souls.snapshot();require(stopFields.size()==6&&souls.ghostCount()==1,"six pairs and ghost before actual stop");for(var field:stopFields){field.pig.entityTags().add("time_stopped");field.display.entityTags().add("time_stopped");}stopEpoch=game.items().epoch();}catch(RuntimeException|Error failure){restoreStop(game);throw failure;}
        SkyWars2.LOGGER.info("[check] PASS soulhunter-stop-probe");
    }
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().soulless().count()==0&&game.items().soulless().ghostCount()==0&&stopFields.stream().allMatch(f->f.pig.isRemoved()&&f.display.isRemoved()),"actual stop invalidates epoch/removes all twelve entities and ghosting");}finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS soulhunter-cleanup");}
    private static void restoreStop(GameController game){if(stopSaved==null)return;game.items().soulless().stop();stopSaved.restore(game);freshItems(stopSaved.player,stopBefore);game.players().save();game.items().magic().flushCleansing();stopSaved=null;stopFields=null;stopBefore=null;stopEpoch=null;}
}
