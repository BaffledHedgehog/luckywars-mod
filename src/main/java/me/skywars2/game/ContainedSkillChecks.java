package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Bounded native checks for the source-menu skills 1, 12 and 13. */
public final class ContainedSkillChecks {
    private ContainedSkillChecks() {}
    private static DeathFixture death;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Contained skills: "+why);}
    private static void near(double got,double expected,String why){require(Math.abs(got-expected)<1e-5,why+" got="+got+" expected="+expected);}
    private static java.lang.reflect.Field field(Object target,String name){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{var result=type.getDeclaredField(name);result.setAccessible(true);return result;}catch(NoSuchFieldException ignored){}
        throw new IllegalStateException("Missing contained fixture field "+name);
    }
    private static Map<java.lang.reflect.Field,Object> attackers(ServerPlayer player){
        var result=new LinkedHashMap<java.lang.reflect.Field,Object>();
        try{for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime")){var entry=field(player,name);result.put(entry,entry.get(player));}}
        catch(IllegalAccessException error){throw new IllegalStateException("Capture native attacker credit",error);}return result;
    }
    private static void restoreAttackers(ServerPlayer player,Map<java.lang.reflect.Field,Object> values){
        try{for(var entry:values.entrySet())entry.getKey().set(player,entry.getValue());}
        catch(IllegalAccessException error){throw new IllegalStateException("Restore native attacker credit",error);}
    }
    private static void clearAttackers(ServerPlayer player){player.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);player.setLastHurtByMob(null);}
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.initialize();fixture.menu();fixture.heart();fixture.vip();fixture.suppression();fixture.actors();}
        SkyWars2.LOGGER.info("[check] PASS contained-skills (native effects/hidden layers, source menu, accepted damage accounting, own-position actor suppression)");
    }
    private static class Fixture implements AutoCloseable {
        final GameController game;ServerPlayer owner,other;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final Map<java.lang.reflect.Field,Object> ownerAttackers,otherAttackers;
        final boolean skills,ranked,originalFrozen;final int cooldown,otherCooldown;
        final int actorPhase;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final net.minecraft.server.level.ServerLevel level;
        RegenerationBeltChecks.ScheduledClient client,audience;Marker jammer;Vec3 origin;
        boolean closed;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two actual live participants");
            ownerAttackers=attackers(owner);otherAttackers=attackers(other);
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);level=owner.level();
            originalFrozen=game.server().tickRateManager().isFrozen();actorPhase=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#falling_hedgehogs"),"lbc.math").get();
            skills=game.config().lbcSkills;ranked=game.config().ranked;cooldown=owner.damageCooldownTime;otherCooldown=other.damageCooldownTime;
        }
        LbcContainedSkills service(){return game.items().containedSkills();}
        void select(int id){game.players().get(owner.getUUID()).lbcSkill=id;require(game.lbc().hasSkill(owner,id),"canonical selected skill"+id);}
        void initialize(){
            game.config().lbcSkills=true;game.config().ranked=true;game.server().tickRateManager().setFrozen(false);
            for(var p:List.of(owner,other)){
                clearAttackers(p);p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);
                p.getAttribute(Attributes.MAX_HEALTH).removeModifiers();p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);p.getAttribute(Attributes.ARMOR).removeModifiers();p.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifiers();p.setHealth(100);p.setAbsorptionAmount(0);
                p.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"));
                var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);
                game.items().magic().inventoryChanged(p);game.items().armorSets().inventoryChanged(p);service().clear(p);
            }
            game.items().magic().flushCleansing();
            int x=(owner.blockPosition().getX()>>4)*16+8,z=(owner.blockPosition().getZ()>>4)*16+8,y=Math.min(180,level.getMaxY()-110);
            for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++)for(int dy=0;dy<=3;dy++){
                var pos=new BlockPos(x+dx,y+dy,z+dz);require(level.hasChunkAt(pos),"bounded resident terrain");terrain.put(pos,game.maps().snapshot(level,pos));level.setBlock(pos,(dy==0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            }
            origin=new Vec3(x+.5,y+1,z+.5);move(owner,origin);move(other,origin.add(0,0,5));
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
        }
        void move(ServerPlayer p,Vec3 at){p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);if(p==owner&&client!=null)client.acknowledge();if(p==other&&audience!=null)audience.acknowledge();}
        void menu(){
            require(GameController.operator(owner),"native operator debug entry");game.config().ranked=false;var profile=game.players().get(owner.getUUID());
            for(var row:List.of(new Object[]{1,0,"lbc:true_advancements/accesories/jaakivi"},new Object[]{12,11,"lbc:true_advancements/axes/banhammer"},new Object[]{13,12,"lbc:true_advancements/axes/hammer"})){
                int id=(Integer)row[0],slot=(Integer)row[1];String gate=(String)row[2];game.menus().openDebug(owner);owner.entityTags().remove("skywars2.debug_perks");
                require(game.canUseMenus(owner)&&!game.debugPerks(owner),"real ordinary gate context");var advancement=game.server().getAdvancements().get(Identifier.parse(gate));require(advancement!=null,"source gate "+gate);
                profile.lbcSkill=0;profile.lbcUnlocks.remove(gate);for(var key:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,key);
                require(!game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot),"locked source slot"+slot);
                for(var key:advancement.value().criteria().keySet())owner.getAdvancements().award(advancement,key);
                require(game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot)&&profile.lbcSkill==id,"native completed gate selects"+id);
                game.menus().open(owner,GameMenus.Page.LBC_SKILLS);owner.entityTags().remove("skywars2.debug_perks");
                require(eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner) instanceof eu.pb4.sgui.api.gui.SimpleGui,"actual skill GUI");var gui=(eu.pb4.sgui.api.gui.SimpleGui)eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner);var button=gui.getGuiElement(slot);require(button!=null,"original slot button"+slot);profile.lbcSkill=0;
                button.getGuiCallback().click(slot,eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,gui);
                require(profile.lbcSkill==id&&game.lbc().selected(profile,LbcLoadouts.Kind.SKILLS,10000,slot),"actual GUI callback selects"+id);owner.closeContainer();
            }
            game.config().ranked=true;
        }
        void converted(int ticks,int amp,int expectedAmp){
            owner.removeAllEffects();owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,ticks,amp,true,false,false),other);
            require(!owner.hasEffect(MobEffects.SLOWNESS),"harmful native slowness consumed before first action");var resistance=owner.getEffect(MobEffects.RESISTANCE);
            require(resistance!=null&&resistance.getAmplifier()==expectedAmp&&resistance.getDuration()==ticks,"same real ticks/infinite and capped signed-positive level amp"+amp);
        }
        void heart(){
            select(1);converted(137,2,2);converted(-1,2,2);converted(211,10,3);converted(1,0,0);
            owner.removeAllEffects();require(owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,271,-2),other),"beneficial inverse slowness admitted");require(owner.getEffect(MobEffects.SLOWNESS).getAmplifier()==-2&&!owner.hasEffect(MobEffects.RESISTANCE),"beneficial signed layer retained without conversion");
            owner.removeAllEffects();game.players().get(owner.getUUID()).lbcSkill=0;
            require(owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,401,0),other)&&owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,111,2),other),"native restored chain admits two actual layers");
            require(((EffectLayers.Access)owner.getEffect(MobEffects.SLOWNESS)).skywars2$layerHidden()!=null,"native hidden chain exists before selection");
            select(1);service().beforeEffects(owner);require(!owner.hasEffect(MobEffects.SLOWNESS),"all harmful restored hidden layers consumed");
            var resistance=owner.getEffect(MobEffects.RESISTANCE);require(resistance!=null&&resistance.getAmplifier()==2&&resistance.getDuration()==111,"strongest converted chain retains own clock");
            var hidden=((EffectLayers.Access)resistance).skywars2$layerHidden();require(hidden!=null&&hidden.getAmplifier()==0&&hidden.getDuration()==401,"weaker converted hidden layer retains its independent clock");
            owner.removeAllEffects();game.players().get(owner.getUUID()).lbcSkill=0;
            owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,501,-2),other);owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,71,1),other);select(1);service().beforeEffects(owner);
            require(owner.hasEffect(MobEffects.SLOWNESS)&&owner.getEffect(MobEffects.SLOWNESS).getAmplifier()==-2&&owner.getEffect(MobEffects.SLOWNESS).getDuration()==501,"restored harmful head conversion preserves useful hidden inverse slowness");
            owner.removeAllEffects();game.config().lbcSkills=false;try{require(owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,137,2),other)&&!owner.hasEffect(MobEffects.RESISTANCE),"global disable retains ordinary harmful effect");}finally{owner.removeAllEffects();game.config().lbcSkills=true;}
            owner.entityTags().add("spectator");try{require(owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,137,2),other)&&!owner.hasEffect(MobEffects.RESISTANCE),"observer cannot acquire conversion");}finally{owner.entityTags().remove("spectator");owner.removeAllEffects();}
        }
        DamageSource playerDamage(){return owner.damageSources().playerAttack(other);}
        void vip(){
            select(12);require(game.items().specialTools().immuneToHammer(owner),"canonical VIP retains Banhammer immunity");var source=playerDamage();service().clear(owner);service().recordDamage(owner,source,40);service().recordDamage(owner,source,60);
            near(service().damageThisTick(owner),100,"same-tick accepted hits sum to maxHP");require(service().protectsDeath(owner,source),"exact >= maxHP threshold qualifies; actual max="+owner.getMaxHealth());require(!service().protectsDeath(owner,source),"one receipt cannot protect a second death");
            service().clear(owner);service().recordDamage(owner,source,200);near(service().damageThisTick(owner),200,"overkill accepted amount is not clamped to current HP");
            service().clear(owner);service().recordDamage(owner,source,99.5f);require(!service().protectsDeath(owner,source),"below maxHP never qualifies");
            service().clear(owner);clearAttackers(owner);require(owner.getKillCredit()==null,"unauthored environment control has no native kill credit");
            service().recordDamage(owner,owner.damageSources().generic(),200);service().recordDamage(owner,owner.damageSources().fall(),200);near(service().damageThisTick(owner),0,"unauthored environmental and fall damage excluded");require(!service().protectsDeath(owner,owner.damageSources().fall()),"unauthored fall cannot preserve life");
            owner.setLastHurtByPlayer(other,100);require(owner.getKillCredit()==other,"actual stored foreign-player credit established");
            service().recordDamage(owner,owner.damageSources().fall(),100);near(service().damageThisTick(owner),100,"fall with older native foreign-player credit counts");require(service().protectsDeath(owner,owner.damageSources().fall()),"stored foreign-player credit qualifies native fall death");
            service().clear(owner);service().recordDamage(owner,owner.damageSources().playerAttack(owner),200);near(service().damageThisTick(owner),0,"explicit self source excluded despite older foreign-player credit");require(!service().protectsDeath(owner,owner.damageSources().playerAttack(owner)),"explicit self death cannot use older foreign credit");clearAttackers(owner);
            var arrow=new Arrow(EntityTypes.ARROW,level);arrow.setOwner(other);arrow.setPos(origin.add(0,2,0));require(level.addFreshEntity(arrow),"native indirect projectile");actors.add(arrow);
            var indirect=owner.damageSources().arrow(arrow,other);service().clear(owner);service().recordDamage(owner,indirect,100);require(service().protectsDeath(owner,indirect),"native foreign player projectile counts");
            var captured=game.items().indirectKills().captureDamage(owner.damageSources().generic(),game.items().indirectKills().wildCause(other));service().clear(owner);service().recordDamage(owner,captured,100);require(service().protectsDeath(owner,captured),"true inherited player ownership counts even without direct entity");
            var npc=new net.minecraft.world.entity.monster.zombie.Zombie(EntityTypes.ZOMBIE,level);npc.setPos(origin.add(0,0,2));actors.add(npc);
            game.items().spellSources().unowned(npc);require(level.addFreshEntity(npc),"unowned native attacker admitted");
            owner.setLastHurtByPlayer(other,100);service().clear(owner);var npcHit=owner.damageSources().mobAttack(npc);
            service().recordDamage(owner,npcHit,200);near(service().damageThisTick(owner),0,"unowned Mob cannot borrow earlier PvP credit for VIP");
            require(!service().protectsDeath(owner,npcHit),"unowned Mob lethal receipt excluded");
            owner.setHealth(100);owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            require(owner.hurtServer(level,npcHit,3)&&owner.getKillCredit()==npc,"accepted native Mob supersedes older PvP credit before death");
            near(service().damageThisTick(owner),0,"accepted Mob damage records no player receipt");
            var ownerless=game.items().indirectKills().nativeMobDamage(owner.damageSources().magic(),null);
            owner.setLastHurtByPlayer(other,100);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            require(owner.hurtServer(level,ownerless,3)&&owner.getKillCredit()==null,"authorless scripted NPC damage clears stale native player credit");
            near(service().damageThisTick(owner),0,"authorless NPC source also excluded from VIP");
            clearAttackers(owner);npc.discard();actors.remove(npc);
            service().clear(owner);game.config().lbcSkills=false;try{service().recordDamage(owner,source,100);require(!service().protectsDeath(owner,source)&&!game.items().specialTools().immuneToHammer(owner),"globally disabled VIP cannot protect or reject Banhammer");}finally{game.config().lbcSkills=true;service().clear(owner);}
            owner.entityTags().add("spectator");try{service().recordDamage(owner,source,100);require(!service().protectsDeath(owner,source)&&!game.items().specialTools().immuneToHammer(owner),"observer cannot accumulate VIP or retain Banhammer immunity");}finally{owner.entityTags().remove("spectator");service().clear(owner);}
            owner.removeAllEffects();owner.setHealth(100);owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            require(owner.hurtServer(level,source,3),"actual nonlethal native damage accepted");near(service().damageThisTick(owner),3,"native accepted post-mitigation boundary records exactly once");
            service().clear(owner);owner.setHealth(100);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.getAbilities().invulnerable=true;
            try{require(!owner.hurtServer(level,source,3)&&service().damageThisTick(owner)==0,"native rejected damage records nothing");}finally{owner.getAbilities().invulnerable=false;}
            owner.setHealth(100);service().clear(owner);require(BlackHoleDamage.apply(game,owner,source,3),"accepted rawHP bridge");near(service().damageThisTick(owner),3,"rawHP accepted boundary records once");service().clear(owner);owner.setHealth(100);
        }
        void marker(String tag,Vec3 at){jammer=new Marker(EntityTypes.MARKER,level);jammer.setPos(at);jammer.entityTags().add(tag);require(level.addFreshEntity(jammer),"native suppression marker"+tag);actors.add(jammer);game.items().magic().loaded(jammer);}
        void unjam(){if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();actors.remove(jammer);jammer=null;}}
        void unjammed(){select(1);converted(137,2,2);owner.removeAllEffects();select(12);require(game.items().specialTools().immuneToHammer(owner),"Banhammer immunity remains unjammed");service().clear(owner);service().recordDamage(owner,playerDamage(),100);require(service().protectsDeath(owner,playerDamage()),"VIP unjammed positive control");service().clear(owner);}
        void suppression(){
            for(String tag:List.of("stopper_magic","stopper")){marker(tag,owner.position());try{if(tag.equals("stopper_magic"))require(game.items().magic().suppressed(owner),"native MAG field active");else require(game.items().magic().mechanismSuppressed(owner),"native TECH field active");unjammed();}finally{unjam();}}
            other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();
            try{require(game.items().magic().suppressed(owner),"native active strong-artifact control");unjammed();}finally{other.getInventory().clearContent();game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();}
        }
        Silverfish spawn(){select(13);Silverfish result=null;for(int attempt=0;attempt<128&&result==null;attempt++)result=game.items().fallingHedgehogs().spawn(owner);require(result!=null&&LbcFallingHedgehogs.managed(result),"identity-bound native falling carrier");actors.add(result);return result;}
        void actors(){
            var service=game.items().fallingHedgehogs();require(service.trackedCount()==0,"exclusive empty falling-actor fixture");
            var clock=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#falling_hedgehogs"),"lbc.math");clock.set(0);
            // Placement uses bounded random resident columns, so several tries may miss this tiny floor.
            select(13);Silverfish carrier=null;for(int attempt=0;attempt<128&&carrier==null;attempt++)carrier=service.spawn(owner);
            require(carrier!=null,"bounded source placement finds resident surface");actors.add(carrier);
            require(carrier.isInvulnerable()&&carrier.isNoAi()&&carrier.entityTags().containsAll(Set.of("angry_hedgehog","lucky_mob")),"source native carrier traits");
            var landing=BlockPos.containing(carrier.position().add(0,-101,0));require(level.hasChunkAt(landing)&&level.getBlockState(landing).is(net.minecraft.tags.BlockTags.ENTITIES_CAN_TELEPORT_TO),"source safe surface plus100 spawn");
            service.unloaded(carrier);require(!service.snapshot().contains(carrier),"unload detaches live reference");service.loaded(carrier);require(service.snapshot().contains(carrier),"load recovers exact native tagged identity");
            for(String tag:List.of("stopper_magic","stopper")){
                carrier.setPos(origin.add(0,20,0));marker(tag,carrier.position());try{require(tag.equals("stopper_magic")?game.items().magic().suppressedAt(level,carrier.position()):game.items().magic().mechanismSuppressedAt(level,carrier.position()),"actual field active at native actor");carrier.entityTags().add("time_stopped");service.tick();require(!carrier.isRemoved(),"paused actor defers own-position suppression");carrier.entityTags().remove("time_stopped");service.tick();require(carrier.isRemoved(),"existing actor destroyed at own "+tag+" position");}finally{unjam();}
                carrier=null;for(int attempt=0;attempt<128&&carrier==null;attempt++)carrier=service.spawn(owner);require(carrier!=null,"next independent resident carrier");actors.add(carrier);
            }
            // Owner suppression cannot erase a remote actor; both fields act at the carrier itself.
            marker("stopper_magic",owner.position());try{carrier.setPos(origin.add(0,40,0));service.tick();require(!carrier.isRemoved(),"owner MAG field cannot remotely erase outside carrier");}finally{unjam();}
            carrier.discard();service.unloaded(carrier);
            clock.set(0);for(int i=0;i<99;i++)service.tick();require(clock.get()==99&&service.trackedCount()==0,"source cadence has no emission before100 active ticks");
            service.tick();require(clock.get()==0&&service.trackedCount()==1,"source100th active tick emits exactly one selected-owner actor");
            for(var periodic:service.snapshot()){actors.add(periodic);periodic.discard();service.unloaded(periodic);}
            carrier=spawn();game.config().lbcSkills=false;try{service.tick();require(carrier.isRemoved(),"global disable retires existing native actor");require(service.spawn(owner)==null,"global disable closes actor creation");}finally{game.config().lbcSkills=true;}
            blast();
        }
        void blast(){
            // Preserve a conservative fifteen-block neighborhood of the native power7 blast.
            // Snapshot existing map blocks before dispatch; no extra chunks are loaded.
            var center=BlockPos.containing(origin);
            for(var pos:BlockPos.betweenClosed(center.offset(-15,-15,-15),center.offset(15,15,15))){
                require(level.hasChunkAt(pos),"native blast neighborhood already resident");var key=pos.immutable();terrain.computeIfAbsent(key,ignored->game.maps().snapshot(level,key));
            }
            var before=new HashSet<UUID>();for(var entity:level.getAllEntities()){before.add(entity.getUUID());require(entity instanceof ServerPlayer||actors.contains(entity)||entity.isRemoved()||entity.position().distanceToSqr(Vec3.atCenterOf(center))>16*16,"blast excludes unrelated native actors");}
            var board=game.server().getScoreboard();var oldOwner=owner.getTeam();var oldOther=other.getTeam();
            require(board.getPlayerTeam("contained_skill_checks")==null,"scoped team name unused");var team=board.addPlayerTeam("contained_skill_checks");
            try{
                board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
                for(var p:List.of(owner,other)){p.removeAllEffects();p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setInvulnerableTime(0);p.damageCooldownTime=0;}
                move(owner,origin.add(1,2,0));move(other,origin.add(3,2,0));
                var actor=spawn();actor.setPos(origin.add(0,1,0));actor.setOnGround(false);actor.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-2,0));require(actor.onGround(),"native swept block collision sets OnGround on source carrier");service().clear(owner);service().clear(other);
                float hp=owner.getHealth(),allyHp=other.getHealth();game.items().fallingHedgehogs().tick();
                require(actor.isRemoved(),"native onGround dispatch retires source actor");
                require(owner.getHealth()<hp&&other.getHealth()<allyHp,"native WILD radius7 blast harms owner and scoreboard ally");
                float after=owner.getHealth();game.items().fallingHedgehogs().impact(actor);near(owner.getHealth(),after,"removed actor cannot detonate twice");
            }finally{
                board.removePlayerTeam(team);if(oldOwner!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwner);if(oldOther!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOther);
                for(var entity:level.getAllEntities())if(!before.contains(entity.getUUID())&&!(entity instanceof ServerPlayer)&&!actors.contains(entity))entity.discard();
            }
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(this::unjam,()->{for(var actor:actors){game.items().fallingHedgehogs().unloaded(actor);actor.discard();}},()->{if(client!=null)client.close();if(audience!=null)audience.close();},()->terrain.forEach((pos,state)->game.maps().restoreSnapshot(level,pos,state)),()->{service().clear(owner);service().clear(other);game.config().lbcSkills=skills;game.config().ranked=ranked;saved.restore(game);otherSaved.restore(game);},()->{saved.restoreAttributes();saved.restoreUse();otherSaved.restoreAttributes();otherSaved.restoreUse();owner.damageCooldownTime=cooldown;other.damageCooldownTime=otherCooldown;},()->{game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();game.items().armorSets().inventoryChanged(owner);game.items().armorSets().inventoryChanged(other);game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#falling_hedgehogs"),"lbc.math").set(actorPhase);game.server().tickRateManager().setFrozen(originalFrozen);restoreAttackers(owner,ownerAttackers);restoreAttackers(other,otherAttackers);game.players().save();}))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Contained fixture cleanup",failure);
        }
    }
    private enum DeathMode {
        DIRECT("contained-vip-respawn"),SUM("contained-vip-sum-respawn"),FALL("contained-vip-fall-respawn");
        final String group;DeathMode(String group){this.group=group;}
    }
    public static void runVipDeath(GameController game){startVipDeath(game,DeathMode.DIRECT);}
    public static void runVipSumDeath(GameController game){startVipDeath(game,DeathMode.SUM);}
    public static void runVipFallDeath(GameController game){startVipDeath(game,DeathMode.FALL);}
    private static void startVipDeath(GameController game,DeathMode mode){
        require(death==null,"exclusive native VIP death fixture");var fixture=new DeathFixture(game,mode);death=fixture;
        try{fixture.start();}catch(RuntimeException|Error failure){death=null;fixture.close();throw failure;}
    }
    public static void tick(GameController game){
        if(death==null)return;var fixture=death;
        try{if(fixture.step()){death=null;fixture.close();SkyWars2.LOGGER.info("[check] PASS {} (real other-player lethal, native death, life retained, replacement body and twenty ticks)",fixture.mode.group);}}
        catch(RuntimeException|Error failure){death=null;fixture.close();throw failure;}
    }
    private static final class DeathFixture extends Fixture {
        final GameController.Checkpoint core;final boolean frozen,fallDamage;final DeathMode mode;int lives,deaths,kills,ticks,clock,pushTick;ServerPlayer retired;final int killedPlayer,killedByPlayer;boolean respawned,awaitingFall;
        DeathFixture(GameController game,DeathMode mode){super(game);this.mode=mode;require(!game.developerMode(),"VIP native death requires ordinary lives");core=game.checkpointCore();frozen=game.server().tickRateManager().isFrozen();fallDamage=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE);killedPlayer=other.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER));killedByPlayer=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER));}
        void start(){
            initialize();client.close();client=null;audience.close();audience=null;
            owner.connection.resetPosition();((MatchPlayerCheckpoint.SimulatedAcknowledgement)owner.connection).skywars2$acknowledgeRestore();
            other.connection.resetPosition();((MatchPlayerCheckpoint.SimulatedAcknowledgement)other.connection).skywars2$acknowledgeRestore();
            select(12);retired=owner;lives=game.lives(owner.getUUID());deaths=game.players().get(owner.getUUID()).deaths;kills=game.players().get(other.getUUID()).kills;require(lives>0,"ordinary finite life before death");
            owner.setExperienceLevels(0);owner.setExperiencePoints(0);owner.totalExperience=0;owner.setHealth(100);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            if(mode==DeathMode.FALL){
                require(owner.hurtServer(level,playerDamage(),1),"actual older native player hit before fall");require(owner.getKillCredit()==other,"actual accepted player hit establishes native fall credit");near(service().damageThisTick(owner),1,"only initial hit recorded in its own server tick");
                pushTick=game.server().getTickCount();awaitingFall=true;game.server().tickRateManager().setFrozen(false);return;
            }
            if(mode==DeathMode.SUM){
                require(owner.hurtServer(level,playerDamage(),40),"first real native accepted hit");
                near(service().damageThisTick(owner),40,"first real same-tick receipt");
                require(BlackHoleDamage.apply(game,owner,playerDamage(),60),"second actual same-tick rawHP hit reaches exact maxHP");
            }else require(owner.hurtServer(level,playerDamage(),200),"real foreign-player lethal native damage");
            verifyDeath();game.server().tickRateManager().setFrozen(false);
        }
        void verifyDeath(){
            require(owner.isDeadOrDying()&&game.awaitingRespawn(owner.getUUID()),"actual native death is retained");require(game.lives(owner.getUUID())==lives,"VIP preserves one native life");
            require(game.players().get(owner.getUUID()).deaths==deaths+1&&game.players().get(other.getUUID()).kills==kills+1,"ordinary death and killer accounting retained");
        }
        boolean step(){
            require(++ticks<200,"bounded native death/respawn");require(game.playing(),"ordinary match stays active");
            if(awaitingFall){
                if(game.server().getTickCount()<=pushTick)return false;
                require(service().damageThisTick(owner)==0,"older actual player-hit amount expired at native server tick boundary");require(owner.getKillCredit()==other,"older foreign attacker remains native fall credit across ticks");
                // The ordinary match deliberately disables fall damage during spawn grace.
                // Enable this scoped native contact probe and restore the exact rule in close.
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE,true,game.server());
                owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
                require(owner.hurtServer(level,owner.damageSources().fall(),200),"actual lethal native fall damage after older player hit");verifyDeath();awaitingFall=false;return false;
            }
            if(!respawned){
                owner=game.server().getPlayerList().respawn(retired,false,Entity.RemovalReason.KILLED);require(owner!=retired&&game.server().getPlayerList().getPlayer(owner.getUUID())==owner,"genuine registered replacement body");owner.connection.resetPosition();
                var acknowledgement=(MatchPlayerCheckpoint.SimulatedAcknowledgement)owner.connection;acknowledgement.skywars2$beginRespawn();acknowledgement.skywars2$acknowledgeRestore();
                require(owner.isAlive()&&!game.awaitingRespawn(owner.getUUID())&&game.allowDamage(owner)&&!game.isObserver(owner),"native AFTER_RESPAWN returns ordinary participant");
                require(game.lives(owner.getUUID())==lives,"actual respawn keeps preserved life");clock=game.matchTicks();respawned=true;return false;
            }
            require(owner==game.server().getPlayerList().getPlayer(owner.getUUID())&&owner.isAlive()&&game.allowDamage(owner),"same real replacement remains active");
            require(game.items().containedSkills().damageThisTick(owner)==0,"previous server tick receipt expires across real tick boundary");
            return game.matchTicks()-clock>=20;
        }
        @Override public void close(){
            // Existing full snapshots bind a body. Rebind their two body references after
            // actual PlayerList replacement so all original state is restored to the live body.
            try{if(owner!=retired&&retired!=null){field(saved,"player").set(saved,owner);field(saved.state,"player").set(saved.state,owner);}
                game.server().tickRateManager().setFrozen(true);game.restoreCore(core);
                owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED_BY.get(EntityTypes.PLAYER),killedByPlayer);other.getStats().setValue(other,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER),killedPlayer);
            }catch(ReflectiveOperationException error){throw new IllegalStateException("VIP replacement snapshot binding",error);}
            finally{try{super.close();}finally{level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.FALL_DAMAGE,fallDamage,game.server());game.server().tickRateManager().setFrozen(frozen);}}
        }
    }
}
