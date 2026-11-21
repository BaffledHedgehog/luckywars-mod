package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Scoped native admission, health accounting, source selection and teleport wire checks. */
public final class ReactiveSkillChecks {
    private ReactiveSkillChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Reactive skills: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-5,why+" actual="+actual+" expected="+expected);}
    private static java.lang.reflect.Field field(Object actor,String name){
        for(Class<?> type=actor.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
        throw new IllegalStateException("Missing scoped fixture field "+name);
    }
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.initialize();fixture.menu();fixture.mana();fixture.suppression();fixture.evasion();}
        // Log only after the connection, authored terrain, profiles and resource state are restored.
        SkyWars2.LOGGER.info("[check] PASS reactive-skills (native HP, nested attempts, MAG/TECH/artifact, ordinary menu gates, enemy sphere and teleport momentum)");
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final boolean skills,ranked;final int cooldown,otherCooldown;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<java.lang.reflect.Field,Long> counters=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam ownerTeam,otherTeam;
        RegenerationBeltChecks.ScheduledClient client,audience;Marker jammer;
        net.minecraft.world.scores.PlayerTeam team;Vec3 origin;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two real active participants");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            skills=game.config().lbcSkills;ranked=game.config().ranked;cooldown=owner.damageCooldownTime;otherCooldown=other.damageCooldownTime;
            ownerTeam=owner.getTeam();otherTeam=other.getTeam();
            for(String name:List.of("payouts","damageRolls","damageTeleports","projectileTeleports","projectileQueries"))try{var f=field(service(),name);counters.put(f,f.getLong(service()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        LbcReactiveSkills service(){return game.items().reactiveSkills();}
        void select(int id){game.players().get(owner.getUUID()).lbcSkill=id;require(game.lbc().hasSkill(owner,id),"selected ordinary live owner skill"+id);}
        void initialize(){
            game.config().lbcSkills=true;game.config().ranked=true;
            for(var p:List.of(owner,other)){
                p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.setNoGravity(true);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.ARMOR).removeModifiers();p.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifiers();p.setHealth(100);p.setAbsorptionAmount(0);
                p.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"));
                var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);
                game.items().magic().inventoryChanged(p);game.items().armorSets().inventoryChanged(p);
            }
            game.items().magic().flushCleansing();
            int x=(owner.blockPosition().getX()>>4)*16+8,z=(owner.blockPosition().getZ()>>4)*16+8;
            int y=Math.min(240,owner.level().getMaxY()-8);
            for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)for(int dy=0;dy<=2;dy++){
                var pos=new BlockPos(x+dx,y+dy,z+dz);require(owner.level().hasChunkAt(pos),"fixture uses resident chunk only");terrain.put(pos,game.maps().snapshot(owner.level(),pos));
                owner.level().setBlock(pos,(dy==0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            }
            origin=new Vec3(x+.5,y+1,z+.5);move(owner,origin);move(other,origin.add(0,0,6));
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);
            require(!game.items().magic().suppressed(owner),"unaffected native fixture positive control");
        }
        void move(ServerPlayer p,Vec3 at){p.teleportTo(p.level(),at.x,at.y,at.z,Set.of(),31,-17,true);if(p==owner&&client!=null)client.acknowledge();if(p==other&&audience!=null)audience.acknowledge();}
        void menu(){
            require(GameController.operator(owner),"actual operator menu entry");
            var profile=game.players().get(owner.getUUID());game.config().ranked=false;
            for(var row:List.of(new Object[]{6,5,"lbc:true_advancements/armor/nyauwu"},new Object[]{10,9,"lbc:true_advancements/armor/shulker_full"})){
                int id=(Integer)row[0],slot=(Integer)row[1];String gate=(String)row[2];
                // The previous row closes its real GUI, which also ends match-time menu access.
                // Reenter through the actual operator action, then remove only the unlock bypass.
                game.menus().openDebug(owner);owner.entityTags().remove("skywars2.debug_perks");
                require(game.canUseMenus(owner)&&!game.debugPerks(owner),"real menu context with ordinary achievement policy skill"+id);
                var advancement=game.server().getAdvancements().get(Identifier.parse(gate));require(advancement!=null,"original native source gate "+gate);
                profile.lbcSkill=0;profile.lbcUnlocks.remove(gate);for(var key:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,key);
                require(!game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot),"ordinary missing achievement rejects skill"+id);
                for(var key:advancement.value().criteria().keySet())owner.getAdvancements().award(advancement,key);
                require(game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,slot)&&profile.lbcSkill==id,"original completed achievement admits skill"+id);
                game.menus().open(owner,GameMenus.Page.LBC_SKILLS);owner.entityTags().remove("skywars2.debug_perks");
                require(eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner) instanceof eu.pb4.sgui.api.gui.SimpleGui,"real skill menu opened");
                var menu=(eu.pb4.sgui.api.gui.SimpleGui)eu.pb4.sgui.api.SguiUtils.getCurrentGui(owner);var button=menu.getGuiElement(slot);require(button!=null,"source slot button");profile.lbcSkill=0;
                button.getGuiCallback().click(slot,eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,menu);
                require(profile.lbcSkill==id&&game.lbc().selected(profile,LbcLoadouts.Kind.SKILLS,10000,slot),"ordinary actual menu callback selects"+id);owner.closeContainer();
            }
            game.config().ranked=true;
        }
        void reset(int mana,int regeneration){owner.setHealth(100);owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;game.items().mana().set(owner,mana);game.items().mana().baseRegen(owner,regeneration);}
        boolean hit(float amount){client.acknowledge();return owner.hurtServer(owner.level(),owner.damageSources().generic(),amount);}
        void mana(){
            select(6);var mana=game.items().mana();mana.maximum(owner,1);
            for(int rate:new int[]{16,-16}){reset(0,rate);require(hit(2),"native accepted hit");near(owner.getHealth(),98,"native actual loss2");require(mana.get(owner)==3200,"positive actual lostHP100absBASE despite max1/sign"+rate);}
            reset(-5000,16);require(hit(2)&&mana.get(owner)==-1800,"negative mana receives signed additive positive payout");
            reset(0,Integer.MIN_VALUE);require(hit(2)&&mana.get(owner)==Integer.MAX_VALUE,"MIN regeneration and huge gain saturate without wrapping");
            reset(Integer.MAX_VALUE-1,16);require(hit(2)&&mana.get(owner)==Integer.MAX_VALUE,"already overmax addition saturates");
            reset(77,16);
            require(owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION,100,2),owner),"native positive absorption layer admitted");
            try{
                require(owner.getAbsorptionAmount()>=2,"actual absorption resource exists before native hit");
                hit(2);near(owner.getHealth(),100,"absorption absorbs all HP loss");require(mana.get(owner)==77,"absorption only never pays");
            }finally{owner.removeEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);owner.setAbsorptionAmount(0);}
            reset(88,16);owner.getAbilities().invulnerable=true;try{require(!hit(2)&&mana.get(owner)==88,"native rejected damage pays nothing");}finally{owner.getAbilities().invulnerable=false;}
            reset(99,16);require(!service().withIncomingAttempt(owner,()->false)&&mana.get(owner)==99,"no actual health loss pays nothing");
            reset(0,16);require(service().withIncomingAttempt(owner,()->{owner.setHealth(99);return service().withIncomingAttempt(owner,()->{owner.setHealth(97);return true;});}),"actual nested wrapper action accepted");require(mana.get(owner)==4800,"nested3HP pays exactly once instead of5HP");
            reset(0,16);require(BlackHoleDamage.apply(game,owner,owner.damageSources().generic(),2),"real rawHP route accepts");near(owner.getHealth(),98,"actual rawHP loss2");require(mana.get(owner)==3200,"rawHP shares actual incoming wrapper payout");
            reset(0,16);game.config().lbcSkills=false;try{require(hit(2)&&mana.get(owner)==0,"global disable closes hit payout");}finally{game.config().lbcSkills=true;}
            reset(0,16);owner.entityTags().add("time_stopped");try{service().withIncomingAttempt(owner,()->{owner.setHealth(98);return true;});require(mana.get(owner)==0,"manual pause closes actual wrapper payout");}finally{owner.entityTags().remove("time_stopped");}
        }
        void marker(String tag){jammer=new Marker(EntityTypes.MARKER,owner.level());jammer.setPos(owner.position());jammer.entityTags().add(tag);require(owner.level().addFreshEntity(jammer),"native suppression marker");actors.add(jammer);game.items().magic().loaded(jammer);}
        void unjam(){if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();actors.remove(jammer);jammer=null;}}
        void suppression(){
            for(String tag:List.of("stopper_magic","stopper_tech")){
                marker(tag);boolean magic=tag.equals("stopper_magic");require(game.items().magic().suppressed(owner)==magic,"native field positive control "+tag);
                try{select(6);reset(0,16);require(hit(2),"field hit accepted");require(game.items().mana().get(owner)==(magic?0:3200),"MAG suppression/TECH independence6");
                    select(10);long rolls=service().damageRollCount();service().damaged(owner,owner.damageSources().generic());require(service().damageRollCount()==rolls+(magic?0:1),"MAG suppression/TECH independence10");
                }finally{unjam();}
            }
            other.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();
            try{require(game.items().magic().suppressed(owner),"nearby active strong artifact positive control");select(6);reset(0,16);require(hit(2)&&game.items().mana().get(owner)==0,"artifact suppresses6");select(10);long rolls=service().damageRollCount();service().damaged(owner,owner.damageSources().generic());require(service().damageRollCount()==rolls,"artifact suppresses10");}
            finally{other.getInventory().clearContent();game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();}
        }
        Arrow arrow(Entity source,Vec3 at){var arrow=new Arrow(EntityTypes.ARROW,owner.level());arrow.setOwner(source);arrow.setPos(at);arrow.setNoGravity(true);require(owner.level().addFreshEntity(arrow),"real resident native projectile");actors.add(arrow);return arrow;}
        void evasion(){
            select(10);move(owner,origin);reset(0,0);long rolls=service().damageRollCount();require(hit(1)&&service().damageRollCount()==rolls+1,"accepted hurtServer dispatches one source20percent roll");move(owner,origin);
            var board=game.server().getScoreboard();require(board.getPlayerTeam("reactive_skill_checks")==null,"fixture team name unused");team=board.addPlayerTeam("reactive_skill_checks");board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
            var own=arrow(owner,origin.add(.1,1,0));var friendly=arrow(other,origin.add(.2,1,0));
            require(!service().airborneEnemyProjectile(owner,own)&&!service().airborneEnemyProjectile(owner,friendly),"own and native allied projectiles excluded");
            var skeleton=new net.minecraft.world.entity.monster.skeleton.Skeleton(EntityTypes.SKELETON,owner.level());skeleton.setPos(origin.add(0,0,5));skeleton.setNoAi(true);skeleton.setNoGravity(true);require(owner.level().addFreshEntity(skeleton),"real resident natural skeleton owner");actors.add(skeleton);
            var mobArrow=arrow(skeleton,origin.add(1,1,0));require(LbcIndirectKills.playerSource(mobArrow)==null&&mobArrow.getOwner()==skeleton,"natural mob projectile has native ownership without player attribution");
            require(service().airborneEnemyProjectile(owner,mobArrow),"foreign enemy mob arrow needs no legacy player IDs");long mobTeleports=service().projectileTeleportCount();service().tick();require(service().projectileTeleportCount()==mobTeleports+1,"native hostile mob arrow triggers spread5 after own and allied arrows");move(owner,origin);
            board.addPlayerToTeam(skeleton.getScoreboardName(),team);require(!service().airborneEnemyProjectile(owner,mobArrow),"native scoreboard-allied mob arrow excluded");mobTeleports=service().projectileTeleportCount();service().tick();require(service().projectileTeleportCount()==mobTeleports,"only own/player-allied/mob-allied arrows do not evade");
            board.removePlayerFromTeam(skeleton.getScoreboardName(),team);mobArrow.discard();actors.remove(mobArrow);skeleton.discard();actors.remove(skeleton);
            board.removePlayerFromTeam(other.getScoreboardName(),team);require(service().airborneEnemyProjectile(owner,friendly),"same airborne foreign enemy now admitted");
            friendly.setPos(origin.add(3.9,0,3.9));require(!service().airborneEnemyProjectile(owner,friendly),"exact sphere rejects AABB corner");friendly.setPos(origin.add(4.001,0,0));require(!service().airborneEnemyProjectile(owner,friendly),"outside4 excluded");friendly.setPos(origin.add(4,0,0));require(service().airborneEnemyProjectile(owner,friendly),"exact radius4 included");
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,friendly.registryAccess());friendly.saveWithoutId(output);var data=output.buildResult();data.putBoolean("inGround",true);friendly.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,friendly.registryAccess(),data));friendly.setOwner(other);require(!service().airborneEnemyProjectile(owner,friendly),"native inGround arrow excluded");friendly.discard();actors.remove(friendly);
            var hostile=arrow(other,origin.add(1,1,0));long teleports=service().projectileTeleportCount();service().tick();require(service().projectileTeleportCount()==teleports+1,"any enemy after first self projectile triggers one safe spread5");near(owner.getY(),origin.y,"real teleport lands above safe platform");
            hostile.discard();actors.remove(hostile);own.discard();actors.remove(own);move(owner,origin);var velocity=new Vec3(.37,-.23,.19);var known=new Vec3(-.13,.41,.29);owner.setDeltaMovement(velocity);owner.setKnownMovement(known);owner.setYRot(73);owner.setXRot(-29);
            require(service().teleport(owner,3),"real bounded resident spread3 finds safe column");require(owner.getDeltaMovement().equals(velocity)&&owner.getKnownMovement().equals(known),"independent XYZ velocity and known movement retained");near(owner.getYRot(),73,"yaw retained");near(owner.getXRot(),-29,"pitch retained");
            net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket packet;
            try{Object connection=field(client,"connection").get(client);packet=(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket)field(connection,"latestPosition").get(connection);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            require(packet!=null&&packet.relatives().containsAll(Set.of(Relative.DELTA_X,Relative.DELTA_Y,Relative.DELTA_Z))&&!packet.relatives().contains(Relative.ROTATE_DELTA)&&packet.change().deltaMovement().equals(Vec3.ZERO),"native teleport wire preserves each client momentum axis");client.acknowledge();
            long queries=service().projectileQueryCount();rolls=service().damageRollCount();owner.entityTags().add("time_stopped");
            try{service().tick();service().damaged(owner,owner.damageSources().generic());require(service().projectileQueryCount()==queries&&service().damageRollCount()==rolls,"manual pause closes evasion work");}finally{owner.entityTags().remove("time_stopped");}
            var far=new Vec3(1000000,240,1000000);var pos=BlockPos.containing(far);require(!owner.level().hasChunkAt(pos),"negative search control starts unloaded");require(LbcSurfaceTeleport.destination(owner.level(),far,5,owner.level().getMaxY()+1,owner.getRandom())==null&&!owner.level().hasChunkAt(pos),"bounded search does not load or generate missing columns");
        }
        @Override public void close(){
            Throwable failure=null;
            for(Runnable action:List.<Runnable>of(this::unjam,()->actors.forEach(Entity::discard),()->{if(client!=null)client.close();if(audience!=null)audience.close();},()->{
                var board=game.server().getScoreboard();if(team!=null)board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(otherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),otherTeam);
            },()->terrain.forEach((pos,state)->game.maps().restoreSnapshot(owner.level(),pos,state)),()->{game.config().lbcSkills=skills;game.config().ranked=ranked;saved.restore(game);otherSaved.restore(game);},()->{saved.restoreAttributes();saved.restoreUse();otherSaved.restoreAttributes();otherSaved.restoreUse();owner.damageCooldownTime=cooldown;other.damageCooldownTime=otherCooldown;},()->{
                game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();game.items().armorSets().inventoryChanged(owner);game.items().armorSets().inventoryChanged(other);
                try{for(var entry:counters.entrySet())entry.getKey().setLong(service(),entry.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
            }))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Reactive fixture cleanup",failure);
        }
    }
}
