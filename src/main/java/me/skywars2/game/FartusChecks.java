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
public final class FartusChecks {
    private FartusChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Fartus: "+why);}
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
        final GameController game;final LbcFartus service;final LbcMana mana;final ServerPlayer owner,other;
        final Saved saved,otherSaved;final Map<Field,Object> config;final Vec3 at;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Entity> actors=new ArrayList<>();final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;final int oldCost;
        Fixture(GameController game){
            this.game=game;service=game.items().fartus();mana=game.items().mana();
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active native participants");
            require(service.count()==0&&game.items().wtf().pendingCount()==0&&!game.items().timeStop().stopped(owner.level()),"clean family, crafting fountains and dimension time");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0&&cleanAttribution(game,p),"closed menu, no frost or pending combat before fixture");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            at=Vec3.atCenterOf(owner.blockPosition().offset(0,32,0));require(at.y+8<owner.level().getMaxY(),"fixture under build ceiling");
            team=game.server().getScoreboard().addPlayerTeam("fartus_fixture");team.setAllowFriendlyFire(false);
            oldCost=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("fartus_nadristailo_mana_use"),"lbc.math").get();
        }
        void init(){
            var center=BlockPos.containing(at);for(var p:BlockPos.betweenClosed(center.offset(-8,-3,-8),center.offset(8,7,8))){require(owner.level().hasChunkAt(p),"resident saved clearbox");blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));}
            for(var p:blocks.keySet())block(p,Blocks.AIR);
            game.config().ranked=false;game.config().lbcSkills=false;game.config().offhandWands=true;
            for(var p:List.of(owner,other)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;
                p.entityTags().remove("time_stopped");p.entityTags().remove("sin_greed_active");mana.noMagic(p,0);mana.set(p,100000);mana.score(p,"fartus_sword").set(0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);
            }
            move(owner,at);move(other,at.add(7,0,0));game.items().magic().flushCleansing();
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("fartus_nadristailo_mana_use"),"lbc.math").set(1250);
        }
        void block(BlockPos p,Block type){require(blocks.containsKey(p),"writes remain inside saved clearbox");owner.level().setBlock(p,type.defaultBlockState(),2);}
        void move(ServerPlayer p,Vec3 v){p.teleportTo(p.level(),v.x,v.y,v.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        ItemStack loot(String leaf){var results=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(results.size()==1&&results.getFirst().getCount()==1,"exact singleton loot "+leaf);return results.getFirst();}
        void clear(){for(var shot:service.snapshot())service.unloaded(shot);require(service.count()==0,"all fixture projectiles retired");}
        void remove(Entity e){game.items().shooters().clearStun((net.minecraft.world.entity.LivingEntity)e);game.items().indirectKills().clear((net.minecraft.world.entity.LivingEntity)e);e.discard();}
        Cow cow(Vec3 position){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(position);c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);c.setHealth(20);c.setNoAi(true);c.setNoGravity(true);owner.level().addFreshEntity(c);actors.add(c);return c;}
        LbcFartus.Shot shot(LbcFartus.Kind kind,Vec3 pos,Vec3 motion){var result=service.spawn(owner,kind,pos,motion);require(result!=null&&!result.isRemoved(),"native projectile staged");return result;}
        void held(String leaf,InteractionHand hand){clear();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,loot(leaf));mana.set(owner,100000);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}
        void presentationAndInputs(){
            for(boolean academy:new boolean[]{false,true}){
                var s=loot(academy?"academy_fartus_wand":"fartus_wand");require(s.is(academy?LbcFartus.ACADEMY:LbcFartus.ITEM)&&LbcFartus.isFartus(s),"native family identity");
                require(Identifier.parse(academy?"lbc:palma":"lbc:fartus").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(academy?"lbc:tier_x":"lbc:tier_s").equals(s.get(DataComponents.TOOLTIP_STYLE)),"exact source model/tier");
                require(s.get(DataComponents.LORE)!=null&&s.get(DataComponents.LORE).lines().size()==6,"six authored lore rows");
                for(String flag:List.of("transformable","staff","magicwand","fartus_nadristailo","swordwithability"))require(LbcFartus.data(s).getIntOr(flag,0)==1,"authored flag "+flag);
                require(LbcFartus.data(s).getIntOr("upgraded",0)==(academy?1:0),"Academy upgraded flag");
            }
            require(loot("wtf_item").is(LbcFartus.MATERIAL)&&LbcFartus.MATERIAL.getDefaultMaxStackSize()==64,"native stackable craft donor");
            for(var hand:InteractionHand.values()){
                held("fartus_wand",hand);mana.set(owner,1249);require(!game.items().wands().use(owner,hand)&&mana.get(owner)==1249&&service.count()==0,"strict1250 admission "+hand);
                mana.set(owner,1250);require(game.items().wands().use(owner,hand)&&mana.get(owner)==0&&service.count()==5,"actual paid five-shot input "+hand);
                var kind=service.snapshot().getFirst().kind;require(kind!=LbcFartus.Kind.HOMING&&service.snapshot().stream().allMatch(s->s.kind==kind&&s.getOwner()==owner&&s.position().distanceToSqr(owner.getEyePosition())<1e-9&&s.display.position().distanceToSqr(s.position())<1e-9),"one random branch, initial eye/display position and owner");
                held("academy_fartus_wand",hand);mana.set(owner,-1);mana.score(owner,"fartus_sword").set(2);require(!game.items().wands().use(owner,hand)&&service.count()==0,"two charges insufficient");mana.score(owner,"fartus_sword").set(3);
                require(game.items().wands().use(owner,hand)&&service.count()==5&&service.charges(owner)==0&&mana.get(owner)==-1&&service.snapshot().stream().allMatch(s->s.kind==LbcFartus.Kind.HOMING),"actual Academy both-hand charge3, no mana debit");
            }
            clear();owner.setYRot(0);owner.setXRot(0);service.cast(owner,2);var fan=service.snapshot();for(int i=0;i<5;i++)require(fan.get(i).getDeltaMovement().distanceToSqr(Vec3.directionFromRotation(0,30-i*15).scale(.4))<1e-12,"five exact label fan angles");clear();
        }
        void recipes(){
            for(boolean bench:new boolean[]{false,true})for(boolean academy:new boolean[]{false,true}){
                owner.closeContainer();owner.getInventory().clearContent();if(bench){var pos=BlockPos.containing(at).offset(-6,0,-6);block(pos,Blocks.CRAFTING_TABLE);owner.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,pl)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(owner.level(),pos)),net.minecraft.network.chat.Component.literal("Fartus fixture")));}
                var menu=(net.minecraft.world.inventory.AbstractCraftingMenu)owner.containerMenu;var grid=menu.getInputGridSlots();int fountains=game.items().wtf().fountainCount();
                grid.getFirst().setByPlayer(loot(academy?"academy_terra_wand":"terra_wand").copyWithCount(2));grid.getLast().setByPlayer(loot("wtf_item").copyWithCount(2));
                require(menu.getResultSlot().getItem().is(LbcFartus.ITEM),"native exact Terra+material preview "+bench+"/"+academy);
                grid.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra ingredient rejects");grid.get(1).setByPlayer(ItemStack.EMPTY);
                for(int i=0;i<36;i++)owner.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
                GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);require(grid.getFirst().getItem().getCount()==2&&grid.getLast().getItem().getCount()==2,"full inventory shift take cannot consume donors");
                owner.getInventory().clearContent();GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.PICKUP);
                require(menu.getCarried().is(LbcFartus.ITEM)&&menu.getCarried().getCount()==1&&grid.getFirst().getItem().getCount()==1&&grid.getLast().getItem().getCount()==1,"native take consumes exactly one donor each");require(game.items().wtf().fountainCount()==fountains+1,"only actual successful craft creates fountain");menu.setCarried(ItemStack.EMPTY);
                GameChecks.inventoryClick(owner,0,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);int results=0;for(int i=0;i<36;i++)if(owner.getInventory().getItem(i).is(LbcFartus.ITEM))results+=owner.getInventory().getItem(i).getCount();require(results==1&&grid.getFirst().getItem().isEmpty()&&grid.getLast().getItem().isEmpty(),"native shift take transfers exactly remaining craft");owner.closeContainer();owner.getInventory().clearContent();
            }
        }
        void displayPacket(LbcFartus.Shot shot,float yaw,float pitch,org.joml.Quaternionf left,float scale){
            var display=shot.display;var raw=io.netty.buffer.Unpooled.buffer();
            try{
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(raw,owner.registryAccess());
                var spawn=new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(display.getId(),display.getUUID(),display.getX(),display.getY(),display.getZ(),display.getXRot(),display.getYRot(),display.getType(),0,Vec3.ZERO,0);
                net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer,spawn);
                var decodedSpawn=net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.decode(buffer);
                require(Math.abs(net.minecraft.util.Mth.wrapDegrees(decodedSpawn.getYRot()-yaw))<1.5&&Math.abs(decodedSpawn.getXRot()-pitch)<1.5,"native display spawn packet retains source entity yaw/pitch convention");
                var metadata=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(display.getId(),display.getEntityData().getNonDefaultValues());
                net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,metadata);
                var decoded=net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                var received=new net.minecraft.world.entity.Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());received.getEntityData().assignValues(decoded.packedItems());
                var getter=net.minecraft.world.entity.Display.class.getDeclaredMethod("createTransformation",net.minecraft.network.syncher.SynchedEntityData.class);getter.setAccessible(true);
                var transform=(com.mojang.math.Transformation)getter.invoke(null,received.getEntityData());
                var actual=transform.leftRotation();
                require(Math.abs(actual.x()-left.x())<1e-5&&Math.abs(actual.y()-left.y())<1e-5&&Math.abs(actual.z()-left.z())<1e-5&&Math.abs(actual.w()-left.w())<1e-5,"native metadata round trip retains source local left rotation kind="+shot.kind+" actual="+actual+" expected="+left+" scale="+transform.scale());
                require(transform.scale().equals(new org.joml.Vector3f(scale))&&transform.translation().equals(new org.joml.Vector3f())&&transform.rightRotation().equals(new org.joml.Quaternionf()),"source display scale/translation/right identity survive native metadata");
                require(received.getItemTransform()==net.minecraft.world.item.ItemDisplayContext.FIXED&&Identifier.parse(shot.kind==LbcFartus.Kind.LABEL?"lbc:banan":"lbc:banana").equals(received.getItemStack().get(DataComponents.ITEM_MODEL))&&received.isNoGravity()&&raw.readableBytes()==0,"fixed source model and gravity-free display survive native wire encoding");
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Fartus native display transformation",error);}finally{raw.release();}
        }
        void visuals(){
            held("fartus_wand",InteractionHand.MAIN_HAND);owner.setYRot(71);owner.setXRot(-23);
            for(var kind:LbcFartus.Kind.values()){
                var motion=new Vec3(.3,.2,.4);var shot=shot(kind,at.add(0,5,0),motion);
                float yaw=kind==LbcFartus.Kind.BANANA?71:shot.getYRot(),pitch=kind==LbcFartus.Kind.BANANA?-23:shot.getXRot();
                var left=kind==LbcFartus.Kind.LABEL?new org.joml.Quaternionf(1,0,0,1).normalize():new org.joml.Quaternionf();
                displayPacket(shot,yaw,pitch,left,kind==LbcFartus.Kind.LABEL?.5f:1);
                service.flight(shot);require(!shot.isRemoved(),"display fixture has one unobstructed active frame");
                if(kind==LbcFartus.Kind.BANANA)left=new org.joml.Quaternionf().rotateY((float)Math.toRadians(-shot.getYRot())).rotateX((float)Math.toRadians(shot.getXRot()));
                else{yaw=shot.getYRot();pitch=shot.getXRot();}
                displayPacket(shot,yaw,pitch,left,kind==LbcFartus.Kind.LABEL?.5f:1);clear();
            }
            owner.setYRot(0);owner.setXRot(0);
        }
        void nativeContacts(){
            held("fartus_wand",InteractionHand.MAIN_HAND);
            var c=cow(at.add(0,0,4));var p=shot(LbcFartus.Kind.BANANA,c.position().add(0,.6,-2),new Vec3(0,0,3));service.flight(p);require(c.isDeadOrDying()&&c.getKillCredit()==owner&&p.isRemoved()&&p.display.isRemoved(),"actual native banana collision48 damage and direct real death owner");remove(c);
            c=cow(at.add(0,0,4));p=shot(LbcFartus.Kind.LABEL,c.position().add(0,.6,-.7),new Vec3(0,0,.4));service.flight(p);
            require(p.isRemoved()&&c.getHealth()==20&&c.getEffect(MobEffects.WITHER).getDuration()==300&&c.getEffect(MobEffects.WITHER).getAmplifier()==1&&c.getEffect(MobEffects.POISON).getAmplifier()==2&&c.getEffect(MobEffects.SLOWNESS).getAmplifier()==0,"actual first label contact exact native payload without direct damage");
            c.removeEffect(MobEffects.POISON);c.setHealth(1);for(int i=0;i<50&&!c.isDeadOrDying();i++){c.setInvulnerableTime(0);c.damageCooldownTime=0;c.tick();}require(c.isDeadOrDying()&&c.getKillCredit()==owner,"actual native delayed Wither death inherits label caster");remove(c);
            c=cow(at.add(0,0,4));p=shot(LbcFartus.Kind.LABEL,c.position().add(0,.6,-.7),new Vec3(0,0,.4));p.setOwner(other);service.flight(p);require(c.hasEffect(MobEffects.WITHER),"reflected/current owner label native contact");c.removeEffect(MobEffects.POISON);c.setHealth(2);c.setInvulnerableTime(0);c.damageCooldownTime=0;require(c.hurtServer(owner.level(),c.damageSources().playerAttack(owner),1),"later real accepted player hit");for(int i=0;i<50&&!c.isDeadOrDying();i++){c.setInvulnerableTime(0);c.damageCooldownTime=0;c.tick();}require(c.isDeadOrDying()&&c.getKillCredit()==owner,"newer real player hit keeps priority over reflected label cause");remove(c);
            c=cow(at.add(0,0,4));p=shot(LbcFartus.Kind.BANANA,c.position().add(0,.6,-2),new Vec3(0,0,3));p.setOwner(other);service.flight(p);require(c.isDeadOrDying()&&c.getKillCredit()==other,"current projectile owner determines actual banana kill");remove(c);
            c=cow(at.add(0,0,4));c.addEffect(new MobEffectInstance(MobEffects.WITHER,400,3),other);p=shot(LbcFartus.Kind.LABEL,c.position().add(0,.6,-.7),new Vec3(0,0,.4));service.flight(p);require(c.getEffect(MobEffects.WITHER).getAmplifier()==3,"native ignored weaker Wither preserves existing stronger effect");c.removeEffect(MobEffects.POISON);c.setHealth(1);for(int i=0;i<50&&!c.isDeadOrDying();i++){c.setInvulnerableTime(0);c.damageCooldownTime=0;c.tick();}require(c.isDeadOrDying()&&c.getKillCredit()==other,"ignored weaker label effect cannot steal older stronger Wither cause");remove(c);
            var undead=new net.minecraft.world.entity.monster.zombie.Zombie(EntityTypes.ZOMBIE,owner.level());undead.setPos(at.add(0,0,4));undead.setNoAi(true);undead.setNoGravity(true);undead.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);undead.setHealth(10);owner.level().addFreshEntity(undead);actors.add(undead);p=shot(LbcFartus.Kind.BANANA,undead.position().add(0,.6,-2),new Vec3(0,0,3));service.flight(p);require(undead.getHealth()==42,"native InstantDamageIV inverts to32 healing for undead");remove(undead);
            var overlapA=cow(at.add(0,0,4));var overlapB=cow(at.add(.1,0,4));p=shot(LbcFartus.Kind.BANANA,overlapA.position().add(0,.6,-2),new Vec3(0,0,3));service.flight(p);require(overlapA.isDeadOrDying()&&overlapB.isDeadOrDying()&&overlapA.getKillCredit()==owner&&overlapB.getKillCredit()==owner,"physical banana applies source cube payload to all overlapping native victims");remove(overlapA);remove(overlapB);
            var tough=cow(at.add(0,0,4));tough.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);tough.setHealth(100);p=shot(LbcFartus.Kind.BANANA,tough.position().add(0,.6,-2),new Vec3(0,0,3));service.flight(p);require(tough.getHealth()==52,"source InstantDamageIV48");for(var type:List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.WEAKNESS)){var effect=tough.getEffect(type);require(effect!=null&&effect.getDuration()==40&&effect.getAmplifier()==99&&!effect.isVisible(),"source banana debuff40/99");}require(game.items().shooters().stunTicks(tough)==40&&game.items().actionsBlocked(tough)&&tough.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)>=0,"banana impact uses shared40tick full action stun without negative movement");remove(tough);
        }
        void movement(){
            var p=shot(LbcFartus.Kind.BANANA,at.add(0,4,0),new Vec3(.3,.2,.4));service.flight(p);require(p.position().distanceToSqr(at.add(.3,4.12,.4))<1e-9&&p.getDeltaMovement().distanceToSqr(new Vec3(.3,.12,.4))<1e-9,"native source gravity.08 first step");service.unloaded(p);
            for(var kind:LbcFartus.Kind.values()){
                p=shot(kind,at.add(0,4,0),new Vec3(0,0,.4));if(kind==LbcFartus.Kind.LABEL){service.flight(p);require(p.position().distanceToSqr(at.add(0,4,1.2))<1e-8,"three source.4 substeps");}
                p.age=kind==LbcFartus.Kind.LABEL?59:99;service.flight(p);require(p.isRemoved()&&p.display.isRemoved(),"source60/100 active-tick lifetime "+kind);service.loaded(p);require(service.count()==0,"retired projectile cannot resurrect");
            }
            var target=cow(at.add(4,4,4));p=shot(LbcFartus.Kind.HOMING,at.add(0,4,0),new Vec3(0,0,.4));service.flight(p);require(!p.isRemoved()&&p.getX()>at.x&&p.getZ()>at.z+1.1,"native homing bends through three source.05 blended substeps toward loaded enemy");clear();remove(target);
            // Exclude all loaded living targets, then exercise native entity tick plus service
            // boundaries so no implicit gravity/drag can hide behind manual movement calls.
            var excluded=new ArrayList<Entity>();for(var entity:owner.level().getAllEntities())if(entity!=owner&&entity instanceof net.minecraft.world.entity.LivingEntity&&!entity.entityTags().contains("strict_map_object")){entity.entityTags().add("strict_map_object");excluded.add(entity);}
            try{var start=at.add(0,12,0);p=shot(LbcFartus.Kind.HOMING,start,new Vec3(0,0,.4));for(int i=0;i<8;i++){p.tick();p.display.tick();service.flight(p);require(Math.abs(p.getY()-start.y)<1e-9&&p.getDeltaMovement().y==0,"every native homing frame retains altitude without target/gravity");}require(!p.isRemoved()&&p.isNoGravity()&&!p.entityTags().contains("physical_object")&&p.position().distanceToSqr(start.add(0,0,9.6))<1e-8&&p.getDeltaMovement().distanceToSqr(new Vec3(0,0,.4))<1e-12,"Academy homing flies straight1.2/tick without target, native gravity, drag or ordinary-banana physics");clear();
                var levelTarget=cow(start.add(4,0,18));levelTarget.setPos(levelTarget.getX(),start.y-levelTarget.getEyeHeight(),levelTarget.getZ());
                try{p=shot(LbcFartus.Kind.HOMING,start,new Vec3(0,0,.4));for(int i=0;i<8;i++){p.tick();p.display.tick();service.flight(p);require(Math.abs(p.getY()-start.y)<1e-8&&Math.abs(p.getDeltaMovement().y)<1e-9,"native homing frames steer toward level eyes without gravity/downward drift");}require(p.getX()>start.x,"gravity-free native flight still homes toward the loaded enemy");}finally{clear();remove(levelTarget);}
                var homingWall=BlockPos.containing(at).offset(0,4,2);block(homingWall,Blocks.STONE);
                try{p=shot(LbcFartus.Kind.HOMING,Vec3.atCenterOf(homingWall).add(0,0,-1),new Vec3(0,0,.4));p.tick();service.flight(p);require(!p.isRemoved()&&!p.display.isRemoved()&&p.getDeltaMovement().distanceToSqr(new Vec3(0,0,-.4))<1e-12&&p.getZ()<homingWall.getZ()&&Math.abs(p.getY()-(homingWall.getY()+.5))<1e-9,"Academy homing retains source wall reversal without downward gravity drift");}finally{block(homingWall,Blocks.AIR);}
            }finally{excluded.forEach(e->e.entityTags().remove("strict_map_object"));clear();}
            var wall=BlockPos.containing(at).offset(0,4,2);block(wall,Blocks.STONE);p=shot(LbcFartus.Kind.LABEL,Vec3.atCenterOf(wall).add(0,0,-1),new Vec3(0,0,.4));service.flight(p);require(p.isRemoved(),"label terminates at actual wall");p=shot(LbcFartus.Kind.BANANA,Vec3.atCenterOf(wall).add(0,0,-1.1),new Vec3(0,0,.4));service.flight(p);require(!p.isRemoved()&&Math.abs(p.getDeltaMovement().z+.36)<1e-9,"native physical cube clips wall and rebounds with source.9 coefficient");clear();block(wall,Blocks.AIR);
        }
        void trialAndSword(){
            held("fartus_wand",InteractionHand.MAIN_HAND);var panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(at);panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);actors.add(panel);
            require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(owner.getMainHandItem()),"actual Academy trial admission");var trial=owner.getMainHandItem();var unrelated=loot("fartus_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,unrelated);owner.setItemInHand(InteractionHand.OFF_HAND,trial);
            for(int i=0;i<256&&LbcFartus.data(trial).getIntOr("upgrade_available",0)==0;i++){clear();mana.set(owner,100000);int previous=LbcFartus.data(trial).getIntOr("prev",0),score=LbcFartus.data(trial).getIntOr("score",0);require(game.items().wands().use(owner,InteractionHand.OFF_HAND),"actual used offhand trial release");int branch=service.snapshot().getFirst().kind==LbcFartus.Kind.BANANA?1:2;int expected=switch(score){case 1->branch==previous?2:1;case 2->branch!=previous?3:1;case 3->branch==previous?4:1;default->1;};require(LbcFartus.data(trial).getIntOr("score",0)==expected&&LbcFartus.data(trial).getIntOr("prev",0)==branch,"actual release follows exact AABB/reset-to1 source state machine");require(LbcFartus.data(unrelated).getIntOr("score",0)==0,"unused mainhand never advances offhand trial");}
            require(LbcFartus.data(trial).getIntOr("upgrade_available",0)==1&&LbcFartus.data(trial).getIntOr("score",0)>=4,"actual random AABB Academy success");clear();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,trial);
            // Keep the real same-frame double-click guard; trial progress belongs to the item, so another actor can promote it.
            require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem()==trial,"same-frame Academy duplicate does not promote");
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);other.setItemInHand(InteractionHand.MAIN_HAND,trial);
            require(game.items().academy().interact(other,InteractionHand.MAIN_HAND,panel)&&other.getMainHandItem().is(LbcFartus.ACADEMY)&&Identifier.parse("lbc:tier_x").equals(other.getMainHandItem().get(DataComponents.TOOLTIP_STYLE))&&Identifier.parse("lbc:palma").equals(other.getMainHandItem().get(DataComponents.ITEM_MODEL)),"actual transferred ready-item Academy X palm promotion");
            owner.setItemInHand(InteractionHand.MAIN_HAND,other.getMainHandItem());other.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            require(owner.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS).equals(LbcFartus.attributes(19)),"promoted exact source attributes19/-3.5/+10");
            var c=cow(at.add(0,0,3));
            for(var effect:List.of(MobEffects.SLOWNESS,MobEffects.WEAKNESS,MobEffects.MINING_FATIGUE))c.addEffect(new MobEffectInstance(effect,200,0),other);
            var preexisting=new ArrayList<MobEffectInstance>(c.getActiveEffects());double previousSpeed=c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            mana.score(owner,"fartus_sword").set(0);require(c.hurtServer(owner.level(),c.damageSources().playerAttack(owner),1)&&c.getHealth()==19&&c.getKillCredit()==owner&&service.charges(owner)==1&&game.items().shooters().stunTicks(c)==40&&game.items().actionsBlocked(c),"accepted actual Academy melee retains one damage and native owner, charges sword and starts full40tick action stun");
            c.setDeltaMovement(Vec3.ZERO);c.knockback(.4,1,0,c.damageSources().playerAttack(owner),1);var externalVelocity=c.getDeltaMovement();require(externalVelocity.lengthSqr()>0,"native external knockback produces velocity while stunned");game.items().shooters().prepareStunnedInput(c);require(c.getDeltaMovement().equals(externalVelocity)&&c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)==0,"full melee stun preserves native external velocity and safely zeroes voluntary movement");
            game.items().shooters().clearStun(c);require(Math.abs(c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-previousSpeed)<1e-9&&preexisting.stream().allMatch(effect->c.getEffect(effect.getEffect())==effect&&effect.getAmplifier()==0&&effect.getDuration()==200),"generic melee stun cleanup restores movement and preserves all preexisting potion instances without unsafe SLR status residue");c.setInvulnerableTime(0);c.damageCooldownTime=0;require(c.hurtServer(owner.level(),c.damageSources().indirectMagic(owner,owner),1)&&service.charges(owner)==1&&!game.items().shooters().stunned(c),"nonmelee native damage grants neither charge nor melee stun");
            require(service.hud(owner).getString().contains("\uE103")&&service.hud(owner).getString().endsWith("1"),"source sword charge HUD");remove(c);
            var poop=new ItemStack(Items.DIRT,3);var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("govnoq",1);poop.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));owner.getInventory().setItem(9,poop);owner.setItemInHand(InteractionHand.OFF_HAND,poop.copyWithCount(2));service.updateSword(owner);require(owner.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS).equals(LbcFartus.attributes(69)),"full carried plus offhand govnoq count5 gives69 base bonus");owner.getInventory().setItem(9,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);service.updateSword(owner);require(owner.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS).equals(LbcFartus.attributes(19)),"removing govnoq restores source19");panel.discard();
        }
        void boundaries(){
            held("fartus_wand",InteractionHand.MAIN_HAND);mana.noMagic(owner,1);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&service.count()==0,"noMagic input rejects");mana.noMagic(owner,0);
            owner.entityTags().add("time_stopped");require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"entity time stop rejects input");owner.entityTags().remove("time_stopped");
            for(int i=0;i<10;i++)game.items().frost().add(owner,null);require(game.items().frost().isFrozen(owner)&&!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"real frost action gate");game.items().frost().clear(owner);owner.removeAllEffects();
            var p=shot(LbcFartus.Kind.LABEL,at.add(0,4,0),new Vec3(0,0,.4));var pos=p.position();game.items().timeStop().begin(owner.level(),40);try{service.flight(p);require(p.age==0&&p.position().equals(pos)&&!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"actual dimension Time Stop freezes projectile and input");}finally{game.items().timeStop().end(owner.level());}clear();
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().add("stopper_magic");owner.level().addFreshEntity(marker);game.items().magic().loaded(marker);try{int value=mana.get(owner);require(!game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&mana.get(owner)==value,"real antimagic prevents activation without charge");p=shot(LbcFartus.Kind.LABEL,at.add(0,4,0),new Vec3(0,0,.4));service.flight(p);require(p.isRemoved(),"released magic label suppressed");p=shot(LbcFartus.Kind.BANANA,at.add(0,4,0),new Vec3(0,0,.4));service.flight(p);require(!p.isRemoved(),"released physical banana continues under antimagic");clear();}finally{game.items().magic().unloaded(marker);marker.discard();}
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            move(other,at.add(0,0,3));p=shot(LbcFartus.Kind.BANANA,other.position().add(0,.6,-2),new Vec3(0,0,3));service.flight(p);require(other.getHealth()==20&&other.getActiveEffects().isEmpty()&&!p.isRemoved(),"allied player actual physical projectile safe");clear();move(other,at.add(7,0,0));
            var friend=cow(at.add(0,0,3));game.server().getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);p=shot(LbcFartus.Kind.BANANA,friend.position().add(0,.6,-2),new Vec3(0,0,3));service.flight(p);require(friend.isDeadOrDying(),"friendly mob remains actual physical target");remove(friend);
            var technical=cow(at.add(0,0,3));technical.entityTags().add("strict_map_object");p=shot(LbcFartus.Kind.BANANA,technical.position().add(0,.6,-2),new Vec3(0,0,3));service.flight(p);require(technical.getHealth()==20&&technical.getActiveEffects().isEmpty(),"technical role ignores actual physical projectile");clear();remove(technical);
            game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);move(other,at.add(0,0,3));other.entityTags().add("sin_greed_active");p=shot(LbcFartus.Kind.BANANA,other.position().add(0,.6,-2),new Vec3(0,0,3));try{service.flight(p);require(other.getHealth()==20&&other.getActiveEffects().isEmpty()&&p.getOwner()==other&&p.getDeltaMovement().z<0,"actual Greed reflection transfers current owner and rejects all payload");}finally{other.entityTags().remove("sin_greed_active");clear();move(other,at.add(7,0,0));}
            game.spectators().enter(owner);require(game.isObserver(owner)&&!game.items().wands().use(owner,InteractionHand.MAIN_HAND),"observer input gate");
        }
        @Override public void close(){
            clear();game.items().wtf().stop();game.items().timeStop().end(owner.level());
            for(var e:actors){if(e instanceof net.minecraft.world.entity.LivingEntity living){game.items().shooters().clearStun(living);game.items().indirectKills().clear(living);}e.discard();}
            for(var p:List.of(owner,other)){game.items().frost().clear(p);game.items().indirectKills().clear(p);}
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());fields(game.config(),config);saved.restore(game);otherSaved.restore(game);game.server().getScoreboard().removePlayerTeam(team);
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("fartus_nadristailo_mana_use"),"lbc.math").set(oldCost);
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);
            game.players().save();mana.inventoryChanged(owner);mana.inventoryChanged(other);game.items().magic().flushCleansing();
        }
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.init();CombatVisualChecks.piss(game,f.owner,f.other);f.owner.removeAllEffects();f.other.removeAllEffects();f.move(f.owner,f.at);f.move(f.other,f.at.add(7,0,0));f.presentationAndInputs();f.recipes();f.visuals();f.nativeContacts();f.movement();f.trialAndSword();f.boundaries();}SkyWars2.LOGGER.info("[check] PASS fartus (native loot/input/craft, used-hand trial/X promotion, actual source projectile impacts/deaths/reflection, full melee stun, camera particle wire payload and lifecycle; explicit boundary ticks)");}
    private static Saved stopSaved;private static String stopEpoch;private static LbcFartus.Shot stopShot;private static Set<Entity> stopBefore;
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null&&owner!=null&&game.canUseGameplayItems(owner)&&game.items().fartus().count()==0&&cleanAttribution(game,owner),"clean exclusive actual-stop probe");stopSaved=new Saved(game,owner);stopBefore=Collections.newSetFromMap(new IdentityHashMap<>());owner.level().getAllEntities().forEach(stopBefore::add);
        try{stopShot=game.items().fartus().spawn(owner,LbcFartus.Kind.BANANA,owner.getEyePosition().add(0,4,0),Vec3.ZERO);require(stopShot!=null&&!stopShot.isRemoved(),"native projectile before actual stop");stopShot.entityTags().add("time_stopped");stopShot.display.entityTags().add("time_stopped");stopEpoch=game.items().epoch();}catch(RuntimeException|Error failure){restoreStop(game);throw failure;}SkyWars2.LOGGER.info("[check] PASS fartus-stop-probe");
    }
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().fartus().count()==0&&stopShot.isRemoved()&&stopShot.display.isRemoved(),"actual match stop clears shot/display and invalidates epoch");}finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS fartus-cleanup");}
    private static void restoreStop(GameController game){if(stopSaved==null)return;if(stopShot!=null)game.items().fartus().unloaded(stopShot);stopSaved.restore(game);var fresh=new ArrayList<Entity>();stopSaved.player.level().getAllEntities().forEach(e->{if(!stopBefore.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);game.players().save();game.items().magic().flushCleansing();stopSaved=null;stopShot=null;stopBefore=null;stopEpoch=null;}
}

