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
public final class HalloweenWindChecks {
    private HalloweenWindChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("HalloweenWind: "+why);}
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
            for(String name:List.of("cooldown_wind_charge"))scores.put(name,game.items().mana().score(player,name).get());
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
        final GameController game;final LbcHalloweenWind service;final LbcMana mana;final ServerPlayer owner,other;
        final Saved saved,otherSaved;final Map<Field,Object> config;final BlockPos center;final Vec3 at;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Entity> actors=new ArrayList<>();final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;final int oldCost;final Map<ServerPlayer,Integer> cadence;
        Fixture(GameController game){
            this.game=game;service=game.items().halloweenWind();mana=game.items().mana();cadence=new HashMap<>(cadence(service));
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&owner.level()==other.level()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active same-dimension native participants");
            require(game.server().tickRateManager().runsNormally()&&service.count()==0&&!game.items().timeStop().stopped(owner.level()),"no unrelated native charges or dimension Time Stop");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0&&cleanAttribution(game,p),"closed menu and no pending frost/combat");
            require(game.server().getScoreboard().getPlayerTeam("wind_fixture")==null,"exclusive fixture team");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            center=owner.blockPosition().offset(0,32,0);at=Vec3.atBottomCenterOf(center);require(center.getY()+16<owner.level().getMaxY(),"saved region below build ceiling");
            team=game.server().getScoreboard().addPlayerTeam("wind_fixture");team.setAllowFriendlyFire(false);
            oldCost=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("vex_wand_mana_use"),"lbc.math").get();
        }
        void init(){
            require(owner.level().getEntities(owner,new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(center.offset(-14,-5,-14)),Vec3.atLowerCornerOf(center.offset(15,17,15))),e->e!=other).isEmpty(),"no unrelated entities inside native fixture bounds");
            for(var p:BlockPos.betweenClosed(center.offset(-14,-5,-14),center.offset(14,16,14))){require(owner.level().hasChunkAt(p),"resident bounded terrain");blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));}
            for(var p:blocks.keySet())block(p,Blocks.AIR);
            game.config().ranked=false;game.config().lbcSkills=false;game.config().offhandWands=true;
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                p.entityTags().remove("time_stopped");p.entityTags().remove("sin_greed_active");mana.noMagic(p,0);mana.set(p,100000);mana.score(p,"cooldown_wind_charge").set(0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);
            }
            move(owner,at);move(other,at.add(13,0,0));game.items().magic().flushCleansing();
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("vex_wand_mana_use"),"lbc.math").set(700);
        }
        void block(BlockPos p,Block type){require(blocks.containsKey(p),"all terrain writes inside saved region");owner.level().setBlock(p,type.defaultBlockState(),2);}
        void move(ServerPlayer p,Vec3 v){p.teleportTo(p.level(),v.x,v.y,v.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(String leaf){var result=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(result.size()==1&&result.getFirst().getCount()==1,"singleton native leaf "+leaf);return result.getFirst();}
        void clear(){for(var shot:service.snapshot())service.unloaded(shot);require(service.count()==0,"fixture charges retired");}
        void remove(Entity e){if(e instanceof net.minecraft.world.entity.LivingEntity l)game.items().indirectKills().clear(l);e.discard();}
        Cow cow(Vec3 v){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(v);c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);c.setHealth(20);c.setNoAi(true);c.setNoGravity(true);owner.level().addFreshEntity(c);actors.add(c);return c;}
        void held(String leaf,InteractionHand hand){clear();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,loot(leaf));mana.set(owner,100000);owner.setShiftKeyDown(false);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}
        static void data(ItemStack stack,String key,int value){var n=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();n.putInt(key,value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
        static int data(ItemStack stack,String key){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
        void presentation(){
            for(boolean academy:new boolean[]{false,true}){
                var stack=loot(academy?"academy_vex_wind_wand":"vex_wind_wand");
                require(Identifier.parse("lbc:vex_wand").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse(academy?"lbc:tier_a":"lbc:tier_b").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"exact source Wind model/tier");
                require(stack.get(DataComponents.LORE)!=null&&stack.get(DataComponents.LORE).lines().size()==(academy?6:5),"exact authored Wind lore row count");
                for(String flag:List.of("transformable","staff","magicwand","vex_wand"))require(data(stack,flag)==1,"source wand flag "+flag);
                require(data(stack,"upgraded")== (academy?1:0),"native Academy upgrade identity");
            }
            var mace=loot("halloween_mace");require(Identifier.parse("lbc:halloween_mace").equals(mace.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_s").equals(mace.get(DataComponents.TOOLTIP_STYLE))&&mace.get(DataComponents.LORE).lines().size()==6,"source mace model/S tier/six lore rows");
            for(String flag:List.of("transformable","shovel","halloween_mace","spellwithcooldown","craftusable"))require(data(mace,flag)==1,"source mace flag "+flag);
            require(!mace.has(DataComponents.UNBREAKABLE),"source mace durability component");
        }
        void inputsAndModes(){
            require(loot("vex_wind_wand").is(LbcHalloweenWind.WIND)&&loot("academy_vex_wind_wand").is(LbcHalloweenWind.ACADEMY_WIND)&&loot("halloween_mace").is(LbcHalloweenWind.MACE),"three native loot identities");
            for(var hand:InteractionHand.values()){
                held("vex_wind_wand",hand);mana.set(owner,699);require(!game.items().wands().use(owner,hand)&&mana.get(owner)==699,"strict700 admission "+hand);
                mana.set(owner,700);owner.setDeltaMovement(new Vec3(.2,.3,.4));var look=owner.getLookAngle();require(game.items().wands().use(owner,hand)&&mana.get(owner)==0&&owner.getDeltaMovement().distanceToSqr(new Vec3(.2,.3,.4).add(look))<1e-10,"paid actual hand adds look1 including caster "+hand);
                held("academy_vex_wind_wand",hand);var stack=owner.getItemInHand(hand);data(stack,"mode",1);owner.setShiftKeyDown(true);mana.set(owner,0);
                for(int expected:new int[]{2,3,4,1}){require(game.items().wands().use(owner,hand)&&data(stack,"mode")==expected&&mana.get(owner)==0&&service.count()==0,"four free zero-mana mode changes on actual "+hand);}
                owner.setShiftKeyDown(false);data(stack,"mode",2);mana.set(owner,700);owner.setDeltaMovement(Vec3.ZERO);move(other,at.add(0,0,3));require(game.items().wands().use(owner,hand)&&owner.getDeltaMovement().distanceToSqr(owner.getLookAngle())<1e-10&&other.getDeltaMovement().lengthSqr()==0,"mode2 affects only caster");
                data(stack,"mode",3);mana.set(owner,700);owner.setDeltaMovement(Vec3.ZERO);require(game.items().wands().use(owner,hand)&&owner.getDeltaMovement().lengthSqr()==0&&other.getDeltaMovement().distanceToSqr(owner.getLookAngle())<1e-10,"mode3 excludes only caster");move(other,at.add(13,0,0));
            }
        }
        void wildAndGuards(){
            held("vex_wind_wand",InteractionHand.MAIN_HAND);game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);move(other,at.add(0,0,3));
            var inside=cow(owner.getEyePosition().add(0,0,4.9));var outside=cow(owner.getEyePosition().add(0,0,5.1));var role=cow(at.add(2,0,0));role.entityTags().add("strict_map_object");
            require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&other.getDeltaMovement().distanceToSqr(owner.getLookAngle())<1e-10&&inside.getDeltaMovement().distanceToSqr(owner.getLookAngle())<1e-10&&outside.getDeltaMovement().lengthSqr()==0&&role.getDeltaMovement().lengthSqr()==0,"WILD ally/self radius5 with technical role protection");
            other.setDeltaMovement(Vec3.ZERO);other.entityTags().add("sin_greed_active");mana.set(owner,700);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&other.getDeltaMovement().lengthSqr()==0,"Greed rejects WILD impulse");other.entityTags().remove("sin_greed_active");other.entityTags().add("time_stopped");mana.set(owner,700);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&other.getDeltaMovement().lengthSqr()==0,"Time Stop target rejects impulse");other.entityTags().remove("time_stopped");
            mana.noMagic(owner,1);int value=mana.get(owner);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&mana.get(owner)==value,"noMagic input guard");mana.noMagic(owner,0);
            game.items().timeStop().begin(owner.level(),40);try{require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"dimension Time Stop input guard");}finally{game.items().timeStop().end(owner.level());}
            game.spectators().enter(owner);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"observer input guard");
        }
        void academyMeditation(){
            held("vex_wind_wand",InteractionHand.MAIN_HAND);var panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(at);panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);actors.add(panel);
            require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(owner.getMainHandItem()),"real Academy trial admission");var trial=owner.getMainHandItem();var unused=loot("vex_wind_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,unused);owner.setItemInHand(InteractionHand.OFF_HAND,trial);owner.setShiftKeyDown(true);
            var template=game.server().getStructureTemplateManager().get(Identifier.parse("lbc:air_sphere_11x11x11")).orElseThrow();var origin=BlockPos.containing(owner.getEyePosition().add(-5,-1,-5));var expected=new HashSet<BlockPos>();
            for(var b:template.filterBlocks(origin,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),Blocks.AIR))expected.add(b.pos());require(!expected.isEmpty(),"source sphere contains actual air cells");
            for(var b:BlockPos.betweenClosed(origin,origin.offset(10,10,10)))block(b,Blocks.STONE);
            var support=expected.stream().filter(b->expected.contains(b.above())&&expected.contains(b.above(2))).findFirst().orElseThrow();var standing=cow(Vec3.atBottomCenterOf(support.above()));standing.setOnGround(true);var near=cow(owner.position().add(0,0,10.9));var far=cow(owner.position().add(0,0,11.1));
            require(game.items().wands().use(owner,InteractionHand.OFF_HAND)&&data(trial,"upgrade_available")==1&&data(unused,"upgrade_available")==0,"actual offhand meditation immediately readies exactly used trial stack");
            for(var b:BlockPos.betweenClosed(origin,origin.offset(10,10,10)))require(owner.level().getBlockState(b).isAir()==expected.contains(b),"exact source sphere mask "+b);
            require(near.getDeltaMovement().distanceToSqr(near.getEyePosition().subtract(owner.position()).normalize().scale(10))<1e-10&&far.getDeltaMovement().lengthSqr()==0&&owner.getDeltaMovement().distanceToSqr(new Vec3(0,10,0))<1e-10,"source feet-radius11 outward/upward SET10, including selfUP10");
            game.items().indirectKills().finishSupportRemoval();require(standing.hurtServer(owner.level(),standing.damageSources().fellOutOfWorld(),1000)&&standing.isDeadOrDying()&&standing.getKillCredit()==owner,"actual source sphere support removal/owned displacement inherit terminal native void death");remove(standing);
            require(game.items().academy().interact(owner,InteractionHand.OFF_HAND,panel)&&owner.getOffhandItem()==trial,"production same-gameTime duplicate guard remains");
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);other.setItemInHand(InteractionHand.MAIN_HAND,trial);
            require(game.items().academy().interact(other,InteractionHand.MAIN_HAND,panel)&&other.getMainHandItem().is(LbcHalloweenWind.ACADEMY_WIND),"second actor promotes transferred ready item through real Academy");
        }
        void nativeCharges(){
            held("academy_vex_wind_wand",InteractionHand.MAIN_HAND);data(owner.getMainHandItem(),"mode",4);owner.setYRot(0);owner.setXRot(0);mana.set(owner,700);
            require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&service.count()==5&&mana.get(owner)==0,"actual mode4 creates exactly five native WindCharges");
            var shots=service.snapshot();for(var p:shots){var v=p.getDeltaMovement();require(p.getType()==EntityTypes.WIND_CHARGE&&p.getOwner()==owner&&p.position().distanceToSqr(owner.getEyePosition())<1e-10&&Math.abs(v.x)<=.10001&&Math.abs(v.y)<=.02001&&v.z>=.8999&&v.z<=1.1001,"exact native type/owner/eye and source independent spread bounds");}
            for(var p:shots.subList(1,shots.size()))service.unloaded(p);var p=shots.getFirst();var wall=center.offset(0,3,4);block(wall,Blocks.STONE);var bystander=cow(Vec3.atBottomCenterOf(wall).add(1,0,0));p.setPos(Vec3.atCenterOf(wall).add(0,0,-.8));p.setDeltaMovement(new Vec3(0,0,1));p.tick();require(p.isRemoved(),"native first block collision must retire charge; pos="+p.position()+" removed="+p.isRemoved()+" wall="+owner.level().getBlockState(wall)+" bystanderHP="+bystander.getHealth());require(bystander.getHealth()==20,"native WindCharge blast must add no mod splash HP damage; pos="+p.position()+" removed="+p.isRemoved()+" bystanderHP="+bystander.getHealth()+" cowPos="+bystander.position());remove(bystander);clear();block(wall,Blocks.AIR);
            service.cast(owner,4);p=service.snapshot().getFirst();for(var q:service.snapshot())if(q!=p)service.unloaded(q);var victim=cow(at.add(0,3,4));victim.setHealth(1);p.setPos(victim.position().add(0,.6,-.6));p.setDeltaMovement(new Vec3(0,0,1));p.tick();require(p.isRemoved()&&victim.isDeadOrDying()&&victim.getKillCredit()==owner,"actual native direct WindCharge death retains caster without extra mod impact");remove(victim);clear();
        }
        void chargeGuardsAndLifecycle(){
            service.cast(owner,4);var p=service.snapshot().getFirst();for(var q:service.snapshot())if(q!=p)service.unloaded(q);var pos=p.position();
            game.items().timeStop().begin(owner.level(),40);try{p.tick();require(!p.isRemoved()&&p.position().equals(pos),"native charge fully pauses during dimension Time Stop");}finally{game.items().timeStop().end(owner.level());}
            service.unloaded(p);require(p.isRemoved()&&service.count()==0,"native unload retires charge");service.loaded(p);require(service.count()==0,"retired native charge cannot resurrect");
            service.cast(owner,4);p=service.snapshot().getFirst();for(var q:service.snapshot())if(q!=p)service.unloaded(q);move(other,at.add(0,0,3));other.entityTags().add("sin_greed_active");p.setPos(other.position().add(0,.8,-.7));p.setDeltaMovement(new Vec3(0,0,1));p.tick();
            require(!p.isRemoved()&&p.getOwner()==other&&p.getDeltaMovement().z<0&&other.getHealth()==20&&other.getDeltaMovement().lengthSqr()==0,"actual Greed contact reflects native charge and transfers owner without impact");other.entityTags().remove("sin_greed_active");move(other,at.add(13,0,0));
            var victim=cow(at.add(0,3,4));victim.setHealth(1);p.setPos(victim.position().add(0,.6,-.6));p.setDeltaMovement(new Vec3(0,0,1));p.tick();require(victim.isDeadOrDying()&&victim.getKillCredit()==other,"reflected current native owner receives actual direct death");remove(victim);clear();
            service.cast(owner,4);var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(owner.getEyePosition());marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);actors.add(marker);game.items().magic().loaded(marker);
            try{for(var q:service.snapshot())q.tick();require(service.count()==0,"released native charges retire inside real antimagic");}finally{game.items().magic().unloaded(marker);marker.discard();}
            var stale=new net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge(EntityTypes.WIND_CHARGE,owner.level());stale.entityTags().add(LbcHalloweenWind.PROJECTILE);stale.setPos(at);owner.level().addFreshEntity(stale);actors.add(stale);require(service.loaded(stale)&&stale.isRemoved()&&service.count()==0,"plain reloaded tagged charge safely retired");
        }
        void impulseInheritance(){
            var first=cow(at.add(0,2,3));var newer=cow(at.add(1,2,3));service.push(owner,first,new Vec3(0,1,0),false);service.push(owner,newer,new Vec3(0,1,0),false);
            require(first.hurtServer(owner.level(),first.damageSources().fellOutOfWorld(),1000)&&first.isDeadOrDying()&&first.getKillCredit()==owner,"owned native wind impulse inherits terminal void death");
            require(newer.hurtServer(owner.level(),newer.damageSources().playerAttack(other),1),"later accepted real hit");newer.setInvulnerableTime(0);newer.damageCooldownTime=0;require(newer.hurtServer(owner.level(),newer.damageSources().fellOutOfWorld(),1000)&&newer.isDeadOrDying()&&newer.getKillCredit()==other,"newer native hit supersedes wind altitude cause");
        }
        void mace(){
            for(var hand:InteractionHand.values()){
                held("halloween_mace",hand);mana.set(owner,0);mana.score(owner,"cooldown_wind_charge").set(0);owner.setXRot(90);owner.setDeltaMovement(new Vec3(.2,.3,.4));service.tick();require(owner.getDeltaMovement().distanceToSqr(new Vec3(.2,.3,.4))<1e-10,"lookdown without native jump cannot activate mace");
                require(service.hud(owner).getString().startsWith("    \uE104 "),"source ready glyph/prefix");
                owner.setXRot(0);owner.jumpFromGround();require(mana.score(owner,"cooldown_wind_charge").get()==0,"actual jump requires source lookdown");
                owner.setXRot(90);owner.setDeltaMovement(new Vec3(.2,0,.4));owner.jumpFromGround();require(Math.abs(owner.getDeltaMovement().x-.2)<1e-10&&Math.abs(owner.getDeltaMovement().y-1.42)<1e-6&&Math.abs(owner.getDeltaMovement().z-.4)<1e-10&&mana.score(owner,"cooldown_wind_charge").get()==5&&mana.get(owner)==0,"actual anyhand jump adds source Y1 after native.42, cooldown5, no mana");
                require(service.hud(owner).getString().equals("    \uE105 5"),"source positive-cooldown exact HUD");owner.setDeltaMovement(new Vec3(.2,0,.4));owner.jumpFromGround();require(Math.abs(owner.getDeltaMovement().y-.42)<1e-6,"native jump during cooldown adds no source Y1");
                game.items().timeStop().begin(owner.level(),40);try{service.second(owner);require(mana.score(owner,"cooldown_wind_charge").get()==5,"dimension Time Stop pauses second cooldown boundary");}finally{game.items().timeStop().end(owner.level());}
                for(int remaining=4;remaining>=0;remaining--){service.second(owner);require(mana.score(owner,"cooldown_wind_charge").get()==remaining,"explicit source second boundary "+remaining);}
                owner.setDeltaMovement(new Vec3(.2,0,.4));owner.jumpFromGround();require(mana.score(owner,"cooldown_wind_charge").get()==5&&Math.abs(owner.getDeltaMovement().y-1.42)<1e-6,"native jump rearms after five explicit second boundaries");service.purify(owner);require(mana.score(owner,"cooldown_wind_charge").get()==0,"purify clears source cooldown");
            }
        }
        @Override public void close(){
            clear();game.items().timeStop().end(owner.level());for(var e:actors)remove(e);for(var p:List.of(owner,other)){game.items().frost().clear(p);game.items().indirectKills().clear(p);}
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());fields(game.config(),config);saved.restore(game);otherSaved.restore(game);game.server().getScoreboard().removePlayerTeam(team);
            cadence(service).clear();cadence(service).putAll(cadence);mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("vex_wand_mana_use"),"lbc.math").set(oldCost);freshItems(owner,before);game.players().save();game.items().magic().flushCleansing();
        }
    }
    @SuppressWarnings("unchecked") private static Map<ServerPlayer,Integer> cadence(LbcHalloweenWind service){try{var field=LbcHalloweenWind.class.getDeclaredField("seconds");field.setAccessible(true);return (Map<ServerPlayer,Integer>)field.get(service);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    private static void freshItems(ServerPlayer owner,Set<Entity> before){var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);}
    private record Case(String name,java.util.function.Consumer<Fixture> action){}
    public static void run(GameController game){
        var failures=new ArrayList<String>();for(var test:List.of(new Case("presentation",Fixture::presentation),new Case("inputs/modes",Fixture::inputsAndModes),new Case("WILD/guards",Fixture::wildAndGuards),new Case("Academy/sphere",Fixture::academyMeditation),new Case("native charges",Fixture::nativeCharges),new Case("charge guards/lifecycle",Fixture::chargeGuardsAndLifecycle),new Case("impulse attribution",Fixture::impulseInheritance),new Case("mace",Fixture::mace))){
            try(var f=new Fixture(game)){f.init();test.action.accept(f);}catch(RuntimeException failure){failures.add(test.name+": "+failure.getMessage());}
        }require(failures.isEmpty(),String.join("; ",failures));SkyWars2.LOGGER.info("[check] PASS halloween-wind (native actual-hand admission/modes/Academy, WILD guards, source sphere/impulse, native charge impacts, attribution and mace; explicit boundaries)");
    }
    private static Saved stopSaved;private static String stopEpoch;private static List<net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge> stopShots;private static Set<Entity> stopBefore;
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null&&owner!=null&&game.canUseGameplayItems(owner)&&game.items().halloweenWind().count()==0&&cleanAttribution(game,owner),"clean exclusive actual-stop probe");stopSaved=new Saved(game,owner);stopBefore=Collections.newSetFromMap(new IdentityHashMap<>());owner.level().getAllEntities().forEach(stopBefore::add);
        try{game.items().halloweenWind().cast(owner,4);stopShots=game.items().halloweenWind().snapshot();require(stopShots.size()==5,"five native charges before actual stop");for(var p:stopShots)p.entityTags().add("time_stopped");stopEpoch=game.items().epoch();}catch(RuntimeException|Error failure){restoreStop(game);throw failure;}SkyWars2.LOGGER.info("[check] PASS halloween-wind-stop-probe");
    }
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().halloweenWind().count()==0&&stopShots.stream().allMatch(Entity::isRemoved),"actual stop removes every native charge and invalidates epoch");}finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS halloween-wind-cleanup");}
    private static void restoreStop(GameController game){if(stopSaved==null)return;if(stopShots!=null)for(var p:stopShots)game.items().halloweenWind().unloaded(p);stopSaved.restore(game);freshItems(stopSaved.player,stopBefore);game.players().save();game.items().magic().flushCleansing();stopSaved=null;stopShots=null;stopBefore=null;stopEpoch=null;}
}
