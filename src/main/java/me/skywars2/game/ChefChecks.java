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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
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
        final BoundingBox bounds;final List<ScheduledTick<Block>> blockTicks=new ArrayList<>();final List<ScheduledTick<Fluid>> fluidTicks=new ArrayList<>();boolean closed;
        Fixture(GameController game){
            this.game=game;service=game.items().wtf();mana=game.items().mana();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two active participants");
            require(service.pendingCount()==0&&!game.items().timeStop().stopped(owner.level()),"no unrelated Chef, fountain or dimension Time Stop work");
            for(var p:List.of(owner,other))require(p.containerMenu==p.inventoryMenu&&cleanAttribution(game,p)&&!game.items().frost().isFrozen(p)&&game.items().frost().cold(p)==0,"closed menus and no pre-existing combat/frost");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);config=fields(game.config());owner.level().getAllEntities().forEach(before::add);
            center=owner.blockPosition().offset(0,32,0);at=Vec3.atBottomCenterOf(center);require(center.getY()+12<owner.level().getMaxY(),"bounded clearbox below ceiling");
            bounds=new BoundingBox(center.getX()-16,center.getY()-4,center.getZ()-16,center.getX()+16,center.getY()+12,center.getZ()+16);
            team=game.server().getScoreboard().addPlayerTeam("chef_fixture");team.setAllowFriendlyFire(false);
        }
        void init(){
            for(var p:BlockPos.betweenClosed(center.offset(-16,-4,-16),center.offset(16,12,16))){require(owner.level().hasChunkAt(p),"all possible test terrain resident");blocks.put(p.immutable(),game.maps().snapshot(owner.level(),p));}
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){
                var chunk=owner.level().getChunk(x,z);
                ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(blockTicks::add);
                ((LevelChunkTicks<Fluid>)chunk.getFluidTicks()).getAll().filter(t->bounds.isInside(t.pos())).forEach(fluidTicks::add);
            }
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);
            for(var p:blocks.keySet()){
                boolean floor=p.getY()==center.getY()-1||p.getY()==bounds.minY();
                boolean lowerRim=p.getY()<center.getY()-1&&(p.getX()==bounds.minX()||p.getX()==bounds.maxX()||p.getZ()==bounds.minZ()||p.getZ()==bounds.maxZ());
                block(p,floor?Blocks.STONE:lowerRim?Blocks.BEDROCK:Blocks.AIR);
            }
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
        long fixtureTick;
        void advance(LbcWtf.Chef chef,int ticks){for(int i=0;i<ticks&&!chef.isRemoved();i++){LbcChefBrain.beginTick(--fixtureTick);service.step(chef);
            require(chef.getOffhandItem().isEmpty(),"all Chef actions leave the offhand empty");
            require(chef.lastActionError<=2.001,"every accepted world/combat/bucket action faces its exact point: "+chef.lastActionError);
            chef.commonTick();chef.tick();require(blocks.containsKey(chef.blockPosition()),"native pursuit remains in saved resident box: "+chef.position());}}
        void nativeMaterialAndOwnership(){
            var material=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/wtf_item").getFirst();owner.setItemInHand(InteractionHand.MAIN_HAND,material.copyWithCount(2));var victim=cow(at.add(0,0,6));var foreign=cow(at.add(5,0,6));
            require(victim.hurtServer(owner.level(),victim.damageSources().playerAttack(owner),1)&&owner.getMainHandItem().getCount()==1&&service.count()==1&&mana.get(owner)==0,"actual accepted native material melee consumes one, no mana");var chef=service.snapshot().getFirst();
            require(chef instanceof net.minecraft.world.entity.decoration.Mannequin&&chef.getType()==EntityTypes.MANNEQUIN&&service.owner(chef)==owner&&chef.target==victim&&!chef.shouldBeSaved(),"actual unsaved Mannequin and fixed hit victim/current owner");require(chef.getProfile()!=null,"native Mannequin profile metadata exists");require(chef.metadataDescriptionHidden(),"native description metadata hides Mannequin subtitle");var tools=new HashSet<Item>();for(var tool:service.tools(chef))tools.add(tool.getItem());require(tools.equals(Set.of(Items.NETHERITE_PICKAXE,Items.NETHERITE_AXE,Items.NETHERITE_SHOVEL,Items.NETHERITE_HOE,Items.SHEARS,LbcWtf.SWORD,Items.WATER_BUCKET,Items.COOKED_BEEF)),"native mining tools, custom sword and survival supplies available");
            var profile=chef.getProfile();var skin=profile.skinPatch();
            require(profile instanceof net.minecraft.world.item.component.ResolvableProfile.Static
                    &&skin.body().orElseThrow().texturePath().equals(Identifier.parse("skywars2:textures/entity/chef.png"))
                    &&skin.model().orElseThrow()==net.minecraft.world.entity.player.PlayerModelType.WIDE,"local wide Chef skin without remote profile lookup");
            var buffer=io.netty.buffer.Unpooled.buffer();
            try{net.minecraft.world.item.component.ResolvableProfile.STREAM_CODEC.encode(buffer,profile);
                require(net.minecraft.world.item.component.ResolvableProfile.STREAM_CODEC.decode(buffer).equals(profile)&&!buffer.isReadable(),"native client profile metadata preserves local skin");
            }finally{buffer.release();}
            victim.setInvulnerableTime(0);victim.damageCooldownTime=0;require(victim.hurtServer(owner.level(),victim.damageSources().indirectMagic(owner,owner),1)&&owner.getMainHandItem().getCount()==1&&service.count()==1,"nonmelee native damage never consumes material or creates another Chef");
            require(!game.items().wands().target(owner,chef)&&game.items().wands().projectileTarget(owner,chef),"summoned Chef excluded from owner homing but accepts friendly physical projectiles");
            var second=spawn(other,foreign);require(second.target==foreign&&service.owner(second)==other&&chef.target==victim,"two owners retain independent fixed victims");clear();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);remove(victim);remove(foreign);
        }
        void infiniteHealthAndStasis(){
            var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,5,-4));chef.setDeltaMovement(Vec3.ZERO);float hp=chef.getHealth();
            require(chef.hurtServer(owner.level(),chef.damageSources().playerAttack(other),7)&&chef.getHealth()==hp&&Math.abs(service.damageCount(chef)-7)<.001&&service.stasis(chef)==0,"actual native7 damage preserves HP and tracks partial threshold");chef.setInvulnerableTime(0);chef.damageCooldownTime=0;
            require(chef.hurtServer(owner.level(),chef.damageSources().playerAttack(other),13)&&chef.getHealth()==hp&&service.stasis(chef)==200,"actual total20 damage enters200 active tick knock with infinite HP");
            require(chef.getPose()==Pose.SWIMMING&&Math.abs(chef.getBoundingBox().getYsize()-.6)<.001&&!chef.isNoGravity(),"knock uses native prone .6 high box with gravity");
            var position=chef.position();float targetHp=victim.getHealth();var terrain=terrain();
            game.items().timeStop().begin(owner.level(),40);try{advance(chef,10);require(service.stasis(chef)==200&&chef.position().distanceToSqr(position)<1e-9,"dimension Time Stop pauses knock clock and physics");}finally{game.items().timeStop().end(owner.level());}
            require(!chef.hurtServer(owner.level(),chef.damageSources().playerAttack(other),20)&&chef.getHealth()==hp,"knocked actor rejects additional damage");
            advance(chef,20);require(service.stasis(chef)==180&&chef.getY()<position.y-1&&chef.getPose()==Pose.SWIMMING,"knocked airborne actor falls through native gravity");
            require(victim.getHealth()==targetHp&&terrain().equals(terrain)&&!chef.isUsingItem(),"knock performs no attacks, mining, construction or item use");
            advance(chef,179);require(service.stasis(chef)==1&&victim.getHealth()==targetHp&&chef.getPose()==Pose.SWIMMING,"199 active ticks retain prone harmless knock");
            advance(chef,1);require(service.stasis(chef)==0&&chef.getHealth()==hp,"wake exactly at200 active ticks");var landed=chef.position();advance(chef,30);require(chef.position().distanceToSqr(landed)>.01||victim.getHealth()<targetHp,"native pursuit resumes after wake");clear();remove(victim);
        }
        Map<BlockPos,net.minecraft.world.level.block.state.BlockState> terrain(){var result=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();for(var p:blocks.keySet())result.put(p,owner.level().getBlockState(p));return result;}
        void directSword(){
            var victim=cow(at.add(0,0,3));var outsider=cow(at.add(1.5,0,3));var chef=spawn(owner,victim);chef.setPos(victim.position().add(0,0,-1.5));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
            var sword=chef.combatWeapon();require(sword.is(LbcWtf.SWORD)&&Identifier.parse("lbc:oguzok_sword").equals(sword.get(DataComponents.ITEM_MODEL)),"custom sword retains authored client model");
            var modifiers=sword.get(DataComponents.ATTRIBUTE_MODIFIERS);require(modifiers!=null&&modifiers.modifiers().size()==2,"sword has exactly two native attribute modifiers");
            for(var entry:modifiers.modifiers())require(entry.slot()==EquipmentSlotGroup.MAINHAND&&entry.modifier().operation()==AttributeModifier.Operation.ADD_VALUE&&((entry.attribute().equals(Attributes.ATTACK_DAMAGE)&&entry.modifier().amount()==3)||(entry.attribute().equals(Attributes.ATTACK_SPEED)&&Math.abs(entry.modifier().amount()+2.4)<.00001)),"native wooden sword +3 damage and -2.4 speed");
            boolean fallingCrit=false;int hits=0,lastHit=-100;float health=victim.getHealth();
            for(int tick=0;tick<150&&!victim.isDeadOrDying();tick++){
                victim.setInvulnerableTime(0);victim.damageCooldownTime=0;boolean falling=!chef.onGround()&&chef.getDeltaMovement().y<0;
                advance(chef,1);float damage=health-victim.getHealth();
                if(damage>0){require(Math.abs(damage-Math.min(health,4))<.001||Math.abs(damage-Math.min(health,6))<.001,"actual sword damage is4 normal or6 critical capped by remaining HP; tick="+tick+" beforeHP="+health+" removedHP="+damage+" nativeAttack="+chef.getAttributeValue(Attributes.ATTACK_DAMAGE)+" nativeSpeed="+chef.getAttributeValue(Attributes.ATTACK_SPEED));if(damage>5){require(falling,"critical strike occurs only during native falling phase");fallingCrit=true;}if(hits++>0)require(tick-lastHit>=12,"actual attack cadence respects1.6 attacks/sec sword speed");lastHit=tick;health=victim.getHealth();}
            }
            require(fallingCrit&&hits>=3&&victim.isDeadOrDying()&&victim.getKillCredit()==owner&&outsider.getHealth()==20,"native falling crit kills only fixed victim with summoner attribution");require(chef.getMainHandItem().is(LbcWtf.SWORD),"authored sword remains actual combat equipment");clear();remove(victim);remove(outsider);
        }
        @Override public void close(){
            if(closed)return;closed=true;
            clear();service.stop();game.items().timeStop().end(owner.level());for(var c:cows){game.items().indirectKills().clear(c);c.discard();}for(var p:List.of(owner,other)){game.items().frost().clear(p);game.items().indirectKills().clear(p);}
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());
            owner.level().getBlockTicks().clearArea(bounds);owner.level().getFluidTicks().clearArea(bounds);blockTicks.forEach(owner.level().getBlockTicks()::schedule);fluidTicks.forEach(owner.level().getFluidTicks()::schedule);
            fields(game.config(),config);saved.restore(game);otherSaved.restore(game);game.server().getScoreboard().removePlayerTeam(team);
            var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);game.players().save();game.items().magic().flushCleansing();
        }
        void corridor(boolean gap){
            for(int z=-6;z<=8;z++)for(int x=-1;x<=1;x++)for(int y=0;y<=2;y++)block(center.offset(x,y,z),x!=0||y==2?Blocks.BEDROCK:Blocks.AIR);
            for(int z=-6;z<=8;z++)block(center.offset(0,-1,z),gap&&z>=0&&z<=2?Blocks.AIR:Blocks.STONE);
            block(center.offset(0,0,-6),Blocks.BEDROCK);block(center.offset(0,1,-6),Blocks.BEDROCK);
        }
        void clearCorridor(){for(int z=-6;z<=8;z++)for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++)block(center.offset(x,y,z),y==-1?Blocks.STONE:Blocks.AIR);}
        void shapeTraversal(){
            for(String kind:List.of("slabs","stairs","low ceiling","short jump")){
                corridor(false);
                if(!kind.equals("low ceiling"))for(int z=-5;z<=8;z++)block(center.offset(0,2,z),Blocks.AIR);
                if(kind.equals("slabs"))for(int z=-1;z<=1;z++)block(center.offset(0,0,z),Blocks.STONE_SLAB);
                if(kind.equals("stairs")){block(center,Blocks.STONE_STAIRS);block(center.offset(0,0,1),Blocks.STONE);block(center.offset(0,0,2),Blocks.STONE_SLAB);}
                if(kind.equals("short jump"))block(center.below(),Blocks.AIR);
                var expected=terrain();var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
                for(int tick=0;tick<700&&!chef.isRemoved()&&chef.getZ()<at.z+3;tick++)advance(chef,1);
                require(chef.getZ()>=at.z+3&&terrain().equals(expected),"actual "+kind+" traversal reaches far side without mining or placing; position="+chef.position()+" "+chef.brain.diagnostics());
                clear();remove(victim);clearCorridor();
            }
            for(Block obstacle:List.of(Blocks.OAK_FENCE,Blocks.COBBLESTONE_WALL)){
                block(center,obstacle);block(center.offset(1,0,0),obstacle);var expected=terrain();var victim=cow(at.add(3,0,5));var chef=spawn(owner,victim);chef.setPos(at.add(-3,0,-4));chef.setDeltaMovement(Vec3.ZERO);
                for(int tick=0;tick<700&&!chef.isRemoved()&&chef.distanceToSqr(victim)>6.25;tick++)advance(chef,1);
                require(chef.distanceToSqr(victim)<=6.25&&terrain().equals(expected),"shape-aware diagonal detour around "+obstacle+" reaches victim without edits; position="+chef.position()+" "+chef.brain.diagnostics());clear();remove(victim);block(center,Blocks.AIR);block(center.offset(1,0,0),Blocks.AIR);
            }
        }
        void boundedTerrain(){
            corridor(false);block(center.offset(0,0,0),Blocks.STONE);block(center.offset(0,1,0),Blocks.OBSIDIAN);block(center.above(2),Blocks.AIR);block(center.above(3),Blocks.AIR);var supportVictim=cow(at.add(0,2,0));var newerVictim=cow(at.add(.02,2,0));supportVictim.setOnGround(true);newerVictim.setOnGround(true);var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));
            for(int i=0;i<700&&!chef.isRemoved()&&!owner.level().getBlockState(center.above()).isAir();i++)advance(chef,1);
            require(owner.level().getBlockState(center.above()).isAir(),"bounded native NPC completes obsidian mining beyond36 stationary ticks; position="+chef.position()+" removed="+chef.isRemoved());game.items().indirectKills().finishSupportRemoval();require(game.items().indirectKills().altitudeAuthor(supportVictim)==owner&&game.items().indirectKills().altitudeAuthor(newerVictim)==owner,"actual NPC mining captures native standing victims support-removal cause; first="+game.items().indirectKills().altitudeAuthor(supportVictim)+" second="+game.items().indirectKills().altitudeAuthor(newerVictim));
            supportVictim.setInvulnerableTime(0);supportVictim.damageCooldownTime=0;require(supportVictim.hurtServer(owner.level(),supportVictim.damageSources().fellOutOfWorld(),1000)&&supportVictim.isDeadOrDying()&&supportVictim.getKillCredit()==owner,"actual later native void death credits mining summoner");newerVictim.setOnGround(false);require(newerVictim.hurtServer(owner.level(),newerVictim.damageSources().playerAttack(other),1),"newer accepted native hit after mined support");newerVictim.setInvulnerableTime(0);newerVictim.damageCooldownTime=0;require(newerVictim.hurtServer(owner.level(),newerVictim.damageSources().fellOutOfWorld(),1000)&&newerVictim.isDeadOrDying()&&newerVictim.getKillCredit()==other,"later real player hit supersedes NPC terrain cause");remove(supportVictim);remove(newerVictim);clear();remove(victim);clearCorridor();
            corridor(true);victim=cow(at.add(0,0,6));chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));boolean built=false;
            for(int i=0;i<1000&&!chef.isRemoved()&&chef.getZ()<at.z+3;i++){advance(chef,1);for(int z=0;z<=2;z++)if(!owner.level().getBlockState(center.offset(0,-1,z)).isAir())built=true;}
            require(built&&chef.getZ()>=at.z+3,"native NPC constructs and actually crosses full ceiling-constrained bridge gap; position="+chef.position()+" "+chef.brain.diagnostics());clear();remove(victim);clearCorridor();
            for(int x=-1;x<=1;x++)for(int z=4;z<=6;z++)block(center.offset(x,3,z),Blocks.STONE);victim=cow(at.add(0,4,5));chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));double initial=chef.getY();
            // A completed climb reaches an actual accepted hit, including a valid
            // airborne strike; crossing the reach radius alone is insufficient.
            for(int i=0;i<700&&!chef.isRemoved()&&victim.getHealth()==20;i++)advance(chef,1);
            boolean columnBuilt=false;for(var p:blocks.keySet())if(p.getY()>=center.getY()&&blocks.containsKey(p.above())&&owner.level().getBlockState(p).is(Blocks.COBBLESTONE)&&owner.level().getBlockState(p.above()).is(Blocks.COBBLESTONE))columnBuilt=true;
            require(columnBuilt&&chef.getY()>=initial+2&&victim.getHealth()<20&&victim.getKillCredit()==owner,"bounded native NPC builds a real column, climbs and actually hits higher platform victim with summoner attribution; position="+chef.position()+" velocity="+chef.getDeltaMovement()+" grounded="+chef.onGround()+" "+chef.brain.diagnostics());clear();remove(victim);
            for(int x=-3;x<=3;x++)for(int z=-6;z<=8;z++)for(int y=0;y<=7;y++)block(center.offset(x,y,z),Blocks.AIR);
        }
        void longDetour(){
            for(int x=-12;x<=12;x++)for(int y=-3;y<=12;y++)block(center.offset(x,y,0),Blocks.BEDROCK);
            var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));boolean detoured=false;
            for(int i=0;i<1000&&!chef.isRemoved()&&chef.getZ()<at.z+2;i++){advance(chef,1);if(Math.abs(chef.getX()-at.x)>12)detoured=true;}
            require(detoured&&chef.getZ()>=at.z+2,"native pursuit retains search progress across slices and goes around a long unbreakable wall");clear();remove(victim);
            for(int x=-12;x<=12;x++)for(int y=-3;y<=12;y++)block(center.offset(x,y,0),Blocks.AIR);
        }
        void longParkour(){
            for(int gap:List.of(3,4)){
                corridor(false);
                for(int z=-5;z<=8;z++)block(center.offset(0,2,z),Blocks.AIR);
                for(int z=0;z<gap;z++)block(center.offset(0,-1,z),Blocks.AIR);
                var unchanged=terrain();var victim=cow(at.add(0,0,8));var chef=spawn(owner,victim);
                chef.setPos(at.add(0,0,-4));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
                boolean crossed=false,air=false;int elapsed;
                for(elapsed=0;elapsed<230&&!chef.isRemoved();elapsed++){
                    advance(chef,1);
                    if(chef.getZ()>at.z-.5&&chef.getZ()<at.z+gap-.4&&!chef.onGround()){
                        air=true;require(chef.getDeltaMovement().z>.08,gap+"-block gap flight keeps forward momentum instead of stopping in mid-air: "+chef.position()+" "+chef.brain.diagnostics());
                    }
                    if(chef.getZ()>=at.z+gap+.2&&chef.onGround()){crossed=true;break;}
                }
                require(crossed&&air&&terrain().equals(unchanged),"native "+gap+"-block gap is sprint-jumped promptly without bridge edits; ticks="+elapsed+" pos="+chef.position()+" "+chef.brain.diagnostics());
                clear();remove(victim);clearCorridor();
            }
        }
        void diagonalBuilding(){
            for(int kind=0;kind<3;kind++){
                boolean rising=kind>0,vertical=kind==2;
                // Only a small starting island and distant landing exist; cardinal
                // detours and terrain walking cannot masquerade as diagonal building.
                for(int x=-8;x<=10;x++)for(int z=-8;z<=10;z++)block(center.offset(x,-1,z),Blocks.AIR);
                for(int x=-8;x<=10;x++)for(int z=-8;z<=10;z++)if(x==-8||x==10||z==-8||z==10)
                    for(int y=-3;y<=12;y++)block(center.offset(x,y,z),Blocks.BEDROCK);
                for(int x=-5;x<=-3;x++)for(int z=-5;z<=-3;z++)block(center.offset(x,-1,z),Blocks.STONE);
                int height=vertical?6:rising?4:0;
                if(!vertical)for(int x=4;x<=6;x++)for(int z=4;z<=6;z++)block(center.offset(x,height-1,z),Blocks.STONE);
                var victim=cow(at.add(vertical?-4:5,height,vertical?-4:5));var chef=spawn(owner,victim);chef.setPos(at.add(-4,0,-4));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
                int firstBlock=-1,blocksPlaced=0;var seen=new HashSet<BlockPos>();
                Item previousItem=chef.getMainHandItem().getItem();int lastSwap=chef.tickCount;
                for(int tick=0;tick<460&&!chef.isRemoved()&&victim.getHealth()==20;tick++){
                    advance(chef,1);
                    Item current=chef.getMainHandItem().getItem();
                    if(current!=previousItem){
                        boolean bucketUse=(current==Items.BUCKET||current==Items.WATER_BUCKET)&&(previousItem==Items.BUCKET||previousItem==Items.WATER_BUCKET);
                        require(bucketUse||chef.tickCount-lastSwap>=LbcWtf.Chef.HAND_INTERVAL,"actual building/combat item changes respect hand dwell time: "+previousItem+" -> "+current+" dt="+(chef.tickCount-lastSwap));
                        if(!bucketUse)lastSwap=chef.tickCount;previousItem=current;
                    }
                    for(var p:blocks.keySet())if(owner.level().getBlockState(p).is(Blocks.COBBLESTONE)&&seen.add(p)){blocksPlaced++;if(firstBlock<0)firstBlock=tick;}
                }
                require(firstBlock>=0&&firstBlock<90&&blocksPlaced>=(vertical?3:4)&&victim.getHealth()<20,"completed "+(vertical?"vertical pillar to airborne target":rising?"rising diagonal bridge":"horizontal diagonal bridge")+" reaches fixed victim; first="+firstBlock+" blocks="+blocksPlaced+" pos="+chef.position()+" "+chef.brain.diagnostics());
                require(vertical||seen.stream().anyMatch(p->p.getX()>center.getX()-2&&p.getZ()>center.getZ()-2),"construction advances on both horizontal axes");
                clear();remove(victim);
                for(int x=-8;x<=10;x++)for(int z=-8;z<=10;z++)for(int y=-3;y<=12;y++)block(center.offset(x,y,z),y==-1?Blocks.STONE:Blocks.AIR);
            }
        }
        void stairCorner(){
            for(int x=-4;x<=7;x++)for(int z=-8;z<=3;z++)block(center.offset(x,-1,z),Blocks.AIR);
            for(int z=-6;z<=0;z++){
                double height=(z+6)*.5;int whole=(int)height;
                for(int y=-1;y<whole;y++)block(center.offset(0,y,z),Blocks.STONE);
                if(height!=whole)block(center.offset(0,whole,z),Blocks.STONE_SLAB);
            }
            for(int x=1;x<=5;x++)for(int y=-1;y<=2;y++)block(center.offset(x,y,0),Blocks.STONE);
            var unchanged=terrain();var victim=cow(at.add(5,3,0));var chef=spawn(owner,victim);
            chef.setPos(at.add(0,0,-6));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
            int ticks;for(ticks=0;ticks<300&&!chef.isRemoved()&&victim.getHealth()==20;ticks++)advance(chef,1);
            require(victim.getHealth()<20&&terrain().equals(unchanged),"continuous half-step staircase and right-angle detour completed without bridge/mining or a jump loop; ticks="+ticks+" pos="+chef.position()+" "+chef.brain.diagnostics());
            clear();remove(victim);
        }
        void parkourChain(){
            for(int z=-14;z<=14;z++)for(int x=-1;x<=1;x++)for(int y=0;y<=3;y++)
                block(center.offset(x,y,z),x==0?Blocks.AIR:Blocks.BEDROCK);
            for(int z=-14;z<=14;z++)block(center.offset(0,-1,z),
                    z>=-8&&z<=-6||z>=-2&&z<=1||z>=5&&z<=7?Blocks.AIR:Blocks.STONE);
            var unchanged=terrain();var victim=cow(at.add(0,0,14));var chef=spawn(owner,victim);
            chef.setPos(at.add(0,0,-12));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
            int ticks;for(ticks=0;ticks<400&&!chef.isRemoved()&&chef.getZ()<at.z+9; ticks++)advance(chef,1);
            require(chef.getZ()>=at.z+9&&terrain().equals(unchanged),"three consecutive 3/4/3-block gaps crossed by parkour without construction; ticks="+ticks+" pos="+chef.position()+" "+chef.brain.diagnostics());
            clear();remove(victim);
        }
        void rejectedSwingFeedback(){
            move(owner,at.add(8,0,0));
            var victim=cow(at.add(0,0,3));victim.setPermanentlyInvulnerable(true);
            var chef=spawn(owner,victim);chef.setPos(victim.position().add(0,0,-1.5));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
            boolean swung=false;
            try(var recording=new Recording(game,owner)){
                for(int tick=0;tick<60;tick++){int before=chef.actionSerial;advance(chef,1);if(chef.actionSerial>before)swung|=chef.isSwinging();}
                require(victim.getHealth()==20&&swung&&chef.actionSerial>=2,"invulnerable victim still receives visible native swing attempts");
                require(recording.packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound&&sound.getSound().value()==net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE),"rejected native attacks send no-damage sound");
                require(recording.packets.stream().anyMatch(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance),"rejected native attacks send their attack particles");
            }
            clear();remove(victim);
        }
        void voidRecovery(){
            var victim=cow(at.add(0,0,5));var chef=spawn(owner,victim);chef.setPos(at.x,owner.level().getMinY()-24,at.z);service.step(chef);
            require(!chef.isRemoved()&&owner.level().hasChunkAt(chef.blockPosition())&&owner.level().getBlockState(chef.blockPosition().below()).isSolidRender()&&chef.target==victim,"void Chef returns to real loaded solid surface retaining fixed victim");if(!blocks.containsKey(chef.blockPosition()))chef.setPos(at.add(0,0,-4));var pos=chef.position();advance(chef,30);require(chef.position().distanceToSqr(pos)>.01||victim.getHealth()<20,"after rescue live fixed-victim pursuit continues inside saved fixture");clear();remove(victim);
        }
        void knockedVoidAndLowCeiling(){
            var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));chef.setOnGround(true);
            require(chef.hurtServer(owner.level(),chef.damageSources().playerAttack(other),20)&&service.stasis(chef)==200,"native damage knocks rescue subject");
            chef.setPos(at.x,owner.level().getMinY()-24,at.z);service.step(chef);
            require(!chef.isRemoved()&&blocks.containsKey(chef.blockPosition())&&service.stasis(chef)==199&&chef.getPose()==Pose.SWIMMING&&chef.target==victim,"void rescue works while knocked without waking or retargeting; position="+chef.position());
            chef.setPos(at.add(0,0,-4));chef.setDeltaMovement(Vec3.ZERO);block(center.offset(0,1,-4),Blocks.STONE);
            advance(chef,199);require(service.stasis(chef)==0&&chef.getBoundingBox().getYsize()<=.61&&owner.level().noCollision(chef,chef.getBoundingBox()),"wake under one-block ceiling preserves safe prone box");
            block(center.offset(0,1,-4),Blocks.AIR);advance(chef,3);require(chef.getBoundingBox().getYsize()>1.5,"standing dimensions return when ceiling clears");clear();remove(victim);
        }
        void hungerAndConsumption(){
            var victim=cow(at.add(0,0,4));var chef=spawn(owner,victim);chef.setPos(at);chef.setOnGround(true);
            require(chef.survival.foodLevel()==20&&chef.survival.canSprint()&&chef.survival.supplies().stream().anyMatch(s->s.is(Items.COOKED_BEEF)),"fresh Chef owns actual food and can sprint");
            // Explicit accepted-action events isolate the food contract; this does not
            // claim that thousands of jumps occurred in elapsed gameplay.
            for(int tick=0;tick<50&&chef.survival.foodLevel()>6;tick++){
                chef.setSprinting(true);for(int event=0;event<25;event++)chef.survival.jumpExhaustion();chef.survival.tick();
            }
            require(chef.survival.foodLevel()<=6&&!chef.survival.canSprint()&&!chef.survival.eating(),"accepted exertion drains saturation/food and locks sprint; nearby threat forbids food use");
            victim.setPos(at.add(0,0,10));chef.setOnGround(true);advance(chef,LbcWtf.Chef.HAND_INTERVAL+1);
            require(chef.survival.eating()&&chef.isUsingItem()&&chef.getMainHandItem().is(Items.COOKED_BEEF),"safe hungry Chef starts real native timed food use");int hungry=chef.survival.foodLevel(),count=chef.getMainHandItem().getCount();
            // Exertion can still drain food while native use is in progress.
            advance(chef,10);require(chef.survival.foodLevel()<=hungry&&chef.getMainHandItem().getCount()==count,"partial native food use has no early nutrition");
            hungry=chef.survival.foodLevel();victim.setPos(chef.position().add(0,0,4));service.step(chef);require(!chef.survival.eating()&&!chef.isUsingItem()&&chef.survival.foodLevel()<=hungry&&chef.survival.supplies().stream().filter(s->s.is(Items.COOKED_BEEF)).findFirst().orElseThrow().getCount()==count,"nearby threat interrupts food without nutrition or consumption");
            victim.setPos(at.add(0,0,10));chef.setOnGround(true);service.step(chef);hungry=chef.survival.foodLevel();
            for(int tick=0;tick<50&&chef.survival.foodLevel()==hungry;tick++)advance(chef,1);
            require(chef.survival.foodLevel()>hungry&&chef.survival.supplies().stream().filter(s->s.is(Items.COOKED_BEEF)).findFirst().orElseThrow().getCount()==count-1,"completed native food use consumes exactly one retained food and restores nutrition");clear();remove(victim);
        }
        void fallWater(){
            var victim=cow(at.add(0,0,8));var chef=spawn(owner,victim);chef.setPos(at.add(0,7,-4));chef.setDeltaMovement(0,-.7,0);chef.fallDistance=4;chef.setOnGround(false);
            BlockPos water=null;boolean contact=false;
            for(int tick=0;tick<80&&!chef.isRemoved();tick++){
                advance(chef,1);if(chef.survival.ownedWater()!=null){water=chef.survival.ownedWater();require(blocks.containsKey(water),"fall-water source stays inside saved fixture");}
                contact|=chef.isInWater();if(contact&&chef.survival.ownedWater()==null)break;
            }
            require(water!=null&&contact,"falling Chef places reachable real water and physically lands in it");
            require(chef.survival.ownedWater()==null&&owner.level().getFluidState(water).isEmpty()&&chef.survival.supplies().stream().anyMatch(s->s.is(Items.WATER_BUCKET)),"safe landing recovers its actual source through native bucket pickup; pos="+chef.position()+" water="+chef.survival.ownedWater()+" velocity="+chef.getDeltaMovement());clear();remove(victim);
        }
        void fireWater(){
            var victim=cow(at.add(0,0,8));var chef=spawn(owner,victim);chef.setPos(at.add(0,0,-4));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(true);
            var fire=center.offset(0,0,-4);var unrelated=center.offset(8,0,-4);block(fire,Blocks.FIRE);block(unrelated,Blocks.WATER);
            boolean burning=false,source=false,contact=false;
            for(int tick=0;tick<80&&!chef.isRemoved();tick++){
                advance(chef,1);burning|=chef.isOnFire();source|=chef.survival.ownedWater()!=null;contact|=chef.isInWater();
                if(burning&&source&&contact&&!chef.isOnFire()&&chef.survival.ownedWater()==null)break;
            }
            require(burning&&source&&contact&&!chef.isOnFire(),"native fire contact triggers actual bucket water and extinguishes through physical water contact");
            require(chef.survival.ownedWater()==null&&chef.survival.supplies().stream().anyMatch(s->s.is(Items.WATER_BUCKET)),"extinguished Chef safely recovers its own actual source");
            require(owner.level().getBlockState(unrelated).is(Blocks.WATER)&&owner.level().getFluidState(unrelated).isSource(),"native bucket recovery preserves unrelated source water");clear();remove(victim);
        }
        void guards(){
            var victim=cow(at.add(0,0,6));var chef=spawn(owner,victim);var pos=chef.position();game.items().timeStop().begin(owner.level(),40);try{advance(chef,10);require(chef.position().distanceToSqr(pos)<1e-9&&victim.getHealth()==20,"actual dimension Time Stop prevents native movement and attacks");}finally{game.items().timeStop().end(owner.level());}clear();remove(victim);
            victim=cow(at.add(0,0,6));victim.entityTags().add("strict_map_object");chef=service.spawn(owner,victim);if(chef!=null)service.step(chef);require(service.count()==0&&victim.getHealth()==20,"technical target rejected or immediately retired");remove(victim);
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);chef=service.spawn(owner,other);if(chef!=null)service.step(chef);require(service.count()==0&&other.getHealth()==20,"allied-player fixed victim rejected");
            game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);move(other,at.add(0,0,4));other.entityTags().add("sin_greed_active");chef=service.spawn(owner,other);if(chef!=null){chef.setPos(other.position().add(0,0,-1));service.step(chef);require(other.getHealth()==20,"Greed victim rejects actual nearby Chef sword attempt");}clear();other.entityTags().remove("sin_greed_active");move(other,at.add(12,0,-12));
            game.spectators().enter(owner);victim=cow(at.add(0,0,6));chef=service.spawn(owner,victim);if(chef!=null)service.step(chef);require(game.isObserver(owner)&&service.count()==0&&victim.getHealth()==20,"observer cannot own active Chef");clear();remove(victim);
        }
    }
    private static final class Recording extends net.minecraft.network.Connection implements AutoCloseable {
        final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous;
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        Recording(GameController game,ServerPlayer player){
            super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);this.player=player;previous=player.connection;
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),this,player,
                    new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        }
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public net.minecraft.network.PacketListener getPacketListener(){return player.connection;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){}
        @Override public void close(){player.connection=previous;previous.resetPosition();}
    }
    private record Case(String name,java.util.function.Consumer<Fixture> action){}
    private static Natural natural;
    private static final class Natural implements AutoCloseable {
        final Fixture fixture;final Cow victim;final LbcWtf.Chef chef;
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> terrain;
        final int started;final double elevatedY;final Set<BlockPos> bridgeCells=new HashSet<>();
        int previousKnock=200;boolean fell,woke,built;float yaw,head,body;
        Natural(GameController game){
            fixture=new Fixture(game);
            try{
                fixture.init();fixture.corridor(false);
                // Leave descent headroom until the entire .6-wide body clears the
                // raised stone; the roof over gap cells2..3 still forbids a jump.
                for(int z=-5;z<=1;z++)fixture.block(fixture.center.offset(0,2,z),Blocks.AIR);
                fixture.block(fixture.center.offset(0,0,-3),Blocks.STONE_SLAB);
                fixture.block(fixture.center.offset(0,0,-2),Blocks.STONE_STAIRS);
                fixture.block(fixture.center.offset(0,0,-1),Blocks.STONE);
                for(int z=1;z<=3;z++){var p=fixture.center.offset(0,-1,z);fixture.block(p,Blocks.AIR);bridgeCells.add(p);}
                terrain=fixture.terrain();victim=fixture.cow(fixture.at.add(0,0,7));chef=fixture.spawn(fixture.owner,victim);
                chef.setPos(fixture.at.add(0,5,-4));chef.setDeltaMovement(Vec3.ZERO);chef.setOnGround(false);elevatedY=chef.getY();
                require(chef.hurtServer(fixture.owner.level(),chef.damageSources().playerAttack(fixture.other),20)&&fixture.service.stasis(chef)==200,"natural chase starts with native accepted20 damage knock");
                started=game.server().getTickCount();yaw=chef.getYRot();head=chef.getYHeadRot();body=chef.yBodyRot;
            }catch(RuntimeException|Error failure){fixture.close();throw failure;}
        }
        boolean observe(GameController game){
            int elapsed=game.server().getTickCount()-started;
            require(game.playing()&&!chef.isRemoved()&&chef.target==victim&&!chef.isNoGravity(),"natural chase retains live native actor, fixed victim and gravity");
            require(fixture.blocks.containsKey(chef.blockPosition()),"natural chase remains inside saved box: "+chef.position());
            int knock=fixture.service.stasis(chef);
            if(previousKnock>0){
                require(knock==previousKnock||knock==previousKnock-1,"natural knock clock advances at most once per real tick");
                fell|=chef.getY()<elevatedY-1;
                if(knock>0)require(chef.getPose()==Pose.SWIMMING&&Math.abs(chef.getBoundingBox().getYsize()-.6)<.001&&victim.getHealth()==20&&!chef.isUsingItem(),"natural knock stays prone and harmless while falling");
                else {require(fell&&elapsed>=199&&elapsed<=201,"natural knock falls and wakes after200 elapsed active ticks; elapsed="+elapsed);woke=true;}
            }
            previousKnock=knock;
            require(turn(yaw,chef.getYRot())<=25.01&&turn(head,chef.getYHeadRot())<=25.01&&turn(body,chef.yBodyRot)<=25.01,"natural consecutive body/head turns stay bounded; elapsed="+elapsed+" yaw="+turn(yaw,chef.getYRot())+" head="+turn(head,chef.getYHeadRot())+" body="+turn(body,chef.yBodyRot));
            yaw=chef.getYRot();head=chef.getYHeadRot();body=chef.yBodyRot;
            for(var p:bridgeCells)built|=fixture.owner.level().getBlockState(p).is(Blocks.COBBLESTONE);
            if(victim.getHealth()<20){
                require(woke&&built&&chef.getZ()>=fixture.at.z+4&&chef.distanceToSqr(victim)<=6.25&&victim.getKillCredit()==fixture.owner,"natural chase traverses shapes, builds/crosses bridge and actually hits fixed victim with ownership");
                for(var entry:terrain.entrySet())if(!bridgeCells.contains(entry.getKey()))require(fixture.owner.level().getBlockState(entry.getKey()).equals(entry.getValue()),"natural chase preserves terrain outside three bridge cells: "+entry.getKey());
                return true;
            }
            require(elapsed<500,"natural chase reaches and hits victim within500 real ticks; position="+chef.position()+" knock="+knock+" built="+built+" "+chef.brain.diagnostics());
            return false;
        }
        private static float turn(float before,float after){return Math.abs(net.minecraft.util.Mth.wrapDegrees(after-before));}
        @Override public void close(){fixture.close();}
    }
    /** Stages only; subsequent movement, physics, knock and attacks belong to real server ticks. */
    public static void startNatural(GameController game){require(natural==null,"no overlapping natural Chef fixture");natural=new Natural(game);SkyWars2.LOGGER.info("[check] START chef-natural (real server/world ticks)");}
    public static void tick(GameController game){
        if(natural==null||game==null)return;
        boolean done=false;
        try{done=natural.observe(game);if(done)SkyWars2.LOGGER.info("[check] PASS chef-natural (200 natural tick prone fall/wake, bounded rotations, slab/stair traversal, complete ceiling bridge crossing and attributed fixed-victim hit)");}
        catch(RuntimeException failure){done=true;SkyWars2.LOGGER.error("[check] FAIL chef-natural: {}",failure.getMessage(),failure);}
        finally{if(done){var finished=natural;natural=null;finished.close();}}
    }
    public static void run(GameController game){
        require(natural==null,"explicit checks cannot overlap natural Chef fixture");
        var failures=new ArrayList<String>();
        for(var test:List.of(new Case("material/ownership",Fixture::nativeMaterialAndOwnership),new Case("stasis",Fixture::infiniteHealthAndStasis),
                new Case("sword",Fixture::directSword),new Case("shapes",Fixture::shapeTraversal),new Case("terrain",Fixture::boundedTerrain),new Case("long detour",Fixture::longDetour),new Case("long parkour",Fixture::longParkour),new Case("parkour chain",Fixture::parkourChain),new Case("stair corner",Fixture::stairCorner),new Case("diagonal building",Fixture::diagonalBuilding),new Case("rejected feedback",Fixture::rejectedSwingFeedback),
                new Case("void",Fixture::voidRecovery),new Case("knocked void/ceiling",Fixture::knockedVoidAndLowCeiling),new Case("hunger/food",Fixture::hungerAndConsumption),new Case("fall water",Fixture::fallWater),new Case("fire water",Fixture::fireWater),new Case("guards",Fixture::guards))){
            try(var f=new Fixture(game)){f.init();test.action.accept(f);}
            catch(RuntimeException failure){failures.add(test.name+": "+failure.getMessage());}
        }
        require(failures.isEmpty(),String.join("; ",failures));
        SkyWars2.LOGGER.info("[check] PASS chef (native Mannequin, fixed-victim melee, infiniteHP/prone falling knock, custom sword critical combat and bounded native terrain traversal; explicit ticks)");
    }
    private static Saved stopSaved;private static String stopEpoch;private static LbcWtf.Chef stopChef;private static Cow stopTarget;private static Set<Entity> stopBefore;
    public static void stopProbe(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null&&owner!=null&&game.canUseGameplayItems(owner)&&game.items().wtf().pendingCount()==0&&cleanAttribution(game,owner),"exclusive clean real-stop Chef probe");stopSaved=new Saved(game,owner);stopBefore=Collections.newSetFromMap(new IdentityHashMap<>());owner.level().getAllEntities().forEach(stopBefore::add);
        try{stopTarget=new Cow(EntityTypes.COW,owner.level());stopTarget.setPos(owner.position().add(0,4,0));stopTarget.setNoAi(true);stopTarget.setNoGravity(true);stopTarget.entityTags().add("time_stopped");owner.level().addFreshEntity(stopTarget);stopChef=game.items().wtf().spawn(owner,stopTarget);require(stopChef!=null&&!stopChef.isRemoved(),"native Chef staged before real match stop");stopChef.entityTags().add("time_stopped");game.items().wtf().crafted(owner);require(game.items().wtf().count()==1&&game.items().wtf().fountainCount()==1&&game.items().wtf().pendingCount()==2,"native Chef and sixty-tick crafting fountain staged before actual stop");stopEpoch=game.items().epoch();}catch(RuntimeException|Error failure){restoreStop(game);throw failure;}SkyWars2.LOGGER.info("[check] PASS chef-stop-probe");
    }
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(stopEpoch)&&game.items().wtf().pendingCount()==0&&stopChef.isRemoved(),"actual match stop retires native Chef and epoch");}finally{restoreStop(game);}SkyWars2.LOGGER.info("[check] PASS chef-cleanup");}
    private static void restoreStop(GameController game){if(stopSaved==null)return;if(stopChef!=null)game.items().wtf().unloaded(stopChef);game.items().wtf().stop();if(stopTarget!=null){game.items().indirectKills().clear(stopTarget);stopTarget.discard();}stopSaved.restore(game);var fresh=new ArrayList<Entity>();stopSaved.player.level().getAllEntities().forEach(e->{if(!stopBefore.contains(e)&&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))fresh.add(e);});fresh.forEach(Entity::discard);game.players().save();game.items().magic().flushCleansing();stopSaved=null;stopChef=null;stopTarget=null;stopBefore=null;stopEpoch=null;}
}
