package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.util.*;

/** Native Player.attack, Mob.doHurtTarget and actual outbound PvP motion packets. */
public final class AttackKnockbackChecks {
    private AttackKnockbackChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Attack knockback: "+message);}
    private static Field fieldInHierarchy(Class<?> type,String name)throws NoSuchFieldException{
        for(Class<?> current=type;current!=null;current=current.getSuperclass())try{var field=current.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(type.getName()+" hierarchy: "+name);
    }
    private static boolean clean(GameController game,ServerPlayer player){
        try{for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){field.setAccessible(true);if(((Map<?,?>)field.get(game.items().indirectKills())).containsKey(player))return false;}}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}return true;
    }
    private record AttributeState(double base,List<AttributeModifier> modifiers,Set<Identifier> permanent){
        AttributeState(AttributeInstance attribute){this(attribute.getBaseValue(),List.copyOf(attribute.getModifiers()),attribute.getPermanentModifiers().stream().map(AttributeModifier::id).collect(java.util.stream.Collectors.toSet()));}
        void restore(AttributeInstance attribute){attribute.removeModifiers();attribute.setBaseValue(base);for(var modifier:modifiers)if(permanent.contains(modifier.id()))attribute.addPermanentModifier(modifier);else attribute.addTransientModifier(modifier);}
    }
    private static final class Saved {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;
        final Map<Holder<Attribute>,AttributeState> attributes=new LinkedHashMap<>();
        final Map<Field,Object> fields=new LinkedHashMap<>(),profileFields=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team;final int fallStat;final PlayerStore.Profile profile;
        Saved(GameController game,ServerPlayer player){
            this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());team=player.getTeam();profile=game.players().get(player.getUUID());
            for(var type:List.of(Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.ATTACK_KNOCKBACK,Attributes.KNOCKBACK_RESISTANCE,Attributes.ENTITY_INTERACTION_RANGE))attributes.put(type,new AttributeState(player.getAttribute(type)));
            fallStat=player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FALL_ONE_CM));
            try{
                for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp","lastHurt","lastDamageSource","lastDamageStamp","hurtTime","hurtDuration")){var field=LivingEntity.class.getDeclaredField(name);field.setAccessible(true);fields.put(field,field.get(player));}
                for(String name:List.of("attackStrengthTicker","itemSwapTicker")){var field=fieldInHierarchy(player.getClass(),name);fields.put(field,field.get(player));}
                for(var field:profile.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){var value=field.get(profile);if(value instanceof int[] array)value=array.clone();else if(value instanceof Set<?> set)value=new HashSet<>(set);profileFields.put(field,value);}
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void restore(GameController game){
            state.restore(game);attributes.forEach((type,value)->value.restore(player.getAttribute(type)));
            var board=game.server().getScoreboard();if(player.getTeam()!=null)board.removePlayerFromTeam(player.getScoreboardName(),player.getTeam());if(team!=null)board.addPlayerToTeam(player.getScoreboardName(),team);
            try{for(var entry:fields.entrySet())entry.getKey().set(player,entry.getValue());for(var entry:profileFields.entrySet())entry.getKey().set(profile,entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            player.getStats().setValue(player,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.FALL_ONE_CM),fallStat);
            game.items().magic().inventoryChanged(player);
        }
    }
    private static final class Recording extends net.minecraft.network.Connection {
        final ServerPlayer player;final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public net.minecraft.network.PacketListener getPacketListener(){return player.connection;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;final Saved saved,otherSaved;final Vec3 at;
        final List<Entity> actors=new ArrayList<>();final boolean ranked,skills;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.allowDamage(owner)&&game.allowDamage(other),"two active native match participants");
            for(var player:List.of(owner,other))require(player.containerMenu==player.inventoryMenu&&clean(game,player)&&!game.items().actionsBlocked(player),"fixture owns clean menu/actions/combat boundary");
            require(!game.items().timeStop().stopped(owner.level()),"fixture starts outside Time Stop");
            saved=new Saved(game,owner);otherSaved=new Saved(game,other);at=owner.position().add(0,24,0);
            ranked=game.config().ranked;skills=game.config().lbcSkills;
        }
        void set(LivingEntity player,Holder<Attribute> type,double value){var attribute=player.getAttribute(type);attribute.removeModifiers();attribute.setBaseValue(value);require(attribute.getValue()==value,"actual native attribute "+type+"="+value);}
        void move(ServerPlayer player,Vec3 point){player.teleportTo(player.level(),point.x,point.y,point.z,Set.of(),0,0,true);player.setDeltaMovement(Vec3.ZERO);}
        void initialize(){
            game.config().ranked=false;game.config().lbcSkills=false;
            var board=game.server().getScoreboard();
            for(var player:List.of(owner,other)){
                if(player.getTeam()!=null)board.removePlayerFromTeam(player.getScoreboardName(),player.getTeam());
                player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setShiftKeyDown(false);player.setRemainingFireTicks(0);player.setInvulnerableTime(0);player.damageCooldownTime=0;player.hurtTime=0;player.hurtDuration=0;player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);player.setHealth(20);player.setAbsorptionAmount(0);player.fallDistance=0;
                player.entityTags().remove("sin_greed_active");player.entityTags().remove("time_stopped");
                var profile=game.players().get(player.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().mana().noMagic(player,0);game.items().magic().inventoryChanged(player);
                set(player,Attributes.ATTACK_DAMAGE,2);set(player,Attributes.ATTACK_SPEED,4);set(player,Attributes.ATTACK_KNOCKBACK,0);set(player,Attributes.KNOCKBACK_RESISTANCE,0);
            }
            move(owner,at);move(other,at.add(7,0,0));game.items().magic().flushCleansing();
        }
        Cow cow(){var target=new Cow(EntityTypes.COW,owner.level());target.setPos(at.add(0,0,1.5));target.setNoAi(true);target.setNoGravity(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);owner.level().addFreshEntity(target);actors.add(target);return target;}
        void ready(LivingEntity target,float yaw,float pitch){
            try{fieldInHierarchy(owner.getClass(),"attackStrengthTicker").setInt(owner,200);fieldInHierarchy(owner.getClass(),"itemSwapTicker").setInt(owner,200);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            owner.setYRot(yaw);owner.setXRot(pitch);owner.setYHeadRot(yaw);owner.setOnGround(true);owner.fallDistance=0;owner.setSprinting(false);owner.setDeltaMovement(Vec3.ZERO);
            target.setDeltaMovement(Vec3.ZERO);target.setOnGround(true);target.setHealth(target.getMaxHealth());target.setInvulnerableTime(0);target.damageCooldownTime=0;target.hurtTime=0;target.hurtDuration=0;target.syncVelocity=false;
        }
        Vec3 hit(Cow target,double attribute,float yaw,float pitch,boolean grounded){
            ready(target,yaw,pitch);set(owner,Attributes.ATTACK_KNOCKBACK,attribute);target.setOnGround(grounded);owner.attack(target);
            require(target.getHealth()<100,"real native Player.attack accepted damage");return target.getDeltaMovement();
        }
        void directions(){
            var target=cow();
            for(boolean grounded:new boolean[]{true,false})for(float pitch:new float[]{-90,90,-45,0}){
                var base=hit(target,0,90,pitch,grounded);
                near(base,owner.getLookAngle().scale(.4),"zero-attribute melee base follows yaw/pitch both grounded and airborne");
            }
            ready(target,0,-90);target.setDeltaMovement(new Vec3(.6,.8,1));
            owner.causeExtraKnockback(target,.4f,Vec3.ZERO,owner.damageSources().playerAttack(owner),2,false);
            near(target.getDeltaMovement(),new Vec3(.3,.8,.5),"directed melee damps pre-existing motion equally across axes");
            var horizontal=hit(target,2,0,0,true);require(Math.abs(horizontal.z-1.2)<1e-5&&Math.abs(horizontal.y)<1e-5,"horizontal melee directs base hurt and attribute without artificial lift: "+horizontal);
            var up2=hit(target,2,0,-90,true);var up4=hit(target,4,0,-90,true);
            near(up4.subtract(up2),new Vec3(0,1,0),"two more attribute points add one upward impulse");require(up4.y>2,"vertical attribute is not capped to native .4");
            var down=hit(target,4,0,90,true);require(down.y<-1.5&&Math.abs(down.z)<.3,"downward look drives the victim downward: "+down);
            var a=hit(target,2,90,-45,false);var b=hit(target,4,90,-45,false);
            near(b.subtract(a),new Vec3(-Math.sqrt(.5),Math.sqrt(.5),0),"airborne diagonal victim follows yaw and pitch");
            var maximum=hit(target,10,0,-90,false);require(maximum.y>4.99,"attribute10 provides full five-block/tick vertical impulse in air");
            for(double resistance:new double[]{.5,-.5,-10,-100,1,2}){
                set(target,Attributes.KNOCKBACK_RESISTANCE,resistance);
                a=hit(target,2,0,-90,false);b=hit(target,4,0,-90,false);
                if(resistance>=1)require(a.lengthSqr()==0&&b.lengthSqr()==0,"native full/super resistance blocks all impulse");
                else near(b.subtract(a),new Vec3(0,1-resistance,0),"signed resistance scales directed contribution");
            }
            set(target,Attributes.KNOCKBACK_RESISTANCE,0);
            for(double attribute:new double[]{-4,-100,100,2_000_000_000d}){
                var motion=hit(target,attribute,0,-90,false);
                require(Math.abs(motion.y-(.2+attribute/2))<1e-5,"full signed uncapped native attack attribute="+attribute+": "+motion);
            }
            for(double attribute:new double[]{Double.MAX_VALUE,-Double.MAX_VALUE}){
                set(target,Attributes.KNOCKBACK_RESISTANCE,-100);
                var motion=hit(target,attribute,0,-90,false);
                require(Double.isFinite(motion.x)&&Double.isFinite(motion.y)&&Double.isFinite(motion.z)
                        &&Math.signum(motion.y)==Math.signum(attribute)&&Math.abs(motion.y)>1e300,
                        "float-overflow attribute produces finite directed motion: "+motion);
            }
            set(target,Attributes.KNOCKBACK_RESISTANCE,0);target.discard();
        }
        void nativeRemainder(){
            var target=cow();set(owner,Attributes.ATTACK_KNOCKBACK,2);
            ready(target,0,-90);owner.attack(target);var normal=target.getDeltaMovement();
            ready(target,0,-90);owner.setSprinting(true);owner.setDeltaMovement(new Vec3(.5,.25,.75));owner.attack(target);
            near(target.getDeltaMovement().subtract(normal),new Vec3(0,.5,0),"native sprint contribution follows full look");
            near(owner.getDeltaMovement(),new Vec3(.3,.25,.45),"native attacker slowdown remains once");require(!owner.isSprinting(),"native attack clears sprint once");
            var sword=new ItemStack(net.minecraft.world.item.Items.WOODEN_SWORD);
            sword.enchant(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK),2);
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);ready(target,0,-90);owner.attack(target);
            near(target.getDeltaMovement().subtract(normal),new Vec3(0,1,0),"real KnockbackII follows full look");owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);ready(target,0,-90);set(owner,Attributes.ATTACK_KNOCKBACK,-2);owner.attack(target);
            near(target.getDeltaMovement(),new Vec3(0,.4,0),"negative attribute plus enchantment cancels extra force without damping base hurt");
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            ready(target,0,-90);set(owner,Attributes.ATTACK_KNOCKBACK,0);owner.attack(target);near(target.getDeltaMovement(),new Vec3(0,.4,0),"zero attribute still directs ordinary native hurt push");
            ready(target,0,-90);set(owner,Attributes.ATTACK_KNOCKBACK,10);owner.causeExtraKnockback(target,.4f,Vec3.ZERO,owner.damageSources().playerAttack(owner),2,false);
            near(target.getDeltaMovement(),new Vec3(0,.4,0),"nonattribute native base/sweep calls follow full look");
            ready(target,0,-90);
            var magic=owner.damageSources().indirectMagic(owner,owner);
            require(magic.getEntity()==owner&&magic.getDirectEntity()==owner,"non-melee spell retains direct player attribution");
            require(target.hurtServer(owner.level(),magic,2),"actual direct-attributed magic hit accepted damage");
            near(target.getDeltaMovement(),new Vec3(0,.4,.4),"direct-attributed magic keeps native hurt geometry");
            ready(target,0,-90);
            var projectile=owner.damageSources().source(net.minecraft.world.damagesource.DamageTypes.ARROW,owner,owner);
            target.knockback(.4,0,-1,projectile,2,false);
            near(target.getDeltaMovement(),new Vec3(0,.4,.4),"projectile source with direct owner stays outside melee direction");
            ready(target,0,-90);
            var spear=owner.damageSources().source(net.minecraft.world.damagesource.DamageTypes.SPEAR,owner,owner);
            target.knockback(.4,0,-1,spear,2,false);
            near(target.getDeltaMovement(),new Vec3(0,.4,0),"native direct spear melee also follows pitch");
            ready(target,0,-90);target.entityTags().add("sin_greed_active");owner.attack(target);require(target.getHealth()==100&&target.getDeltaMovement().lengthSqr()==0,"active Greed rejects actual native hit and impulse");
            owner.causeExtraKnockback(target,5,Vec3.ZERO,owner.damageSources().playerAttack(owner),2,true);require(target.getDeltaMovement().lengthSqr()==0,"direct extra path cannot bypass Greed guard");target.entityTags().remove("sin_greed_active");
            ready(target,0,-90);target.setPermanentlyInvulnerable(true);owner.attack(target);require(target.getDeltaMovement().lengthSqr()==0,"rejected native hurt never adds directed attribute force");target.setPermanentlyInvulnerable(false);target.discard();
        }
        @SuppressWarnings("unchecked")
        void mobsAndObjects(){
            var target=cow();var mob=new Zombie(EntityTypes.ZOMBIE,owner.level());mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);set(mob,Attributes.ATTACK_DAMAGE,2);owner.level().addFreshEntity(mob);actors.add(mob);
            Vec3 first=null;
            for(int attribute:new int[]{2,4}){
                ready(target,0,0);target.setOnGround(false);mob.setYRot(0);mob.setXRot(-45);mob.setYHeadRot(0);set(mob,Attributes.ATTACK_KNOCKBACK,attribute);
                require(mob.doHurtTarget(owner.level(),target),"native Mob.doHurtTarget accepted damage");
                if(first==null)first=target.getDeltaMovement();else near(target.getDeltaMovement().subtract(first),new Vec3(0,Math.sqrt(.5),Math.sqrt(.5)),"native mob melee also gains diagonal vertical impulse");
            }
            ready(target,0,0);target.setOnGround(false);mob.setYRot(0);mob.setXRot(-90);set(mob,Attributes.ATTACK_KNOCKBACK,-4);
            require(mob.doHurtTarget(owner.level(),target),"native signed mob melee accepted damage");
            require(Math.abs(target.getDeltaMovement().y+1.8)<1e-4,"negative native mob attribute reverses look-directed force");
            mob.discard();target.discard();
            var immune=new net.minecraft.world.entity.monster.creaking.Creaking(EntityTypes.CREAKING,owner.level());immune.setPos(at.add(0,0,1));actors.add(immune);owner.level().addFreshEntity(immune);
            try{var field=immune.getClass().getDeclaredField("CAN_MOVE");field.setAccessible(true);immune.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Boolean>)field.get(null),false);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            require(!immune.canMove(),"fixture uses native watched/nonmoving Creaking immunity");
            owner.setYRot(0);owner.setXRot(-90);set(owner,Attributes.ATTACK_KNOCKBACK,10);immune.setDeltaMovement(Vec3.ZERO);
            owner.causeExtraKnockback(immune,5,Vec3.ZERO,owner.damageSources().playerAttack(owner),2,true);
            require(immune.getDeltaMovement().lengthSqr()==0,"native knockback-immune mob override remains authoritative");immune.discard();
            var item=new net.minecraft.world.entity.item.ItemEntity(owner.level(),at.x,at.y,at.z,new ItemStack(net.minecraft.world.item.Items.STONE));actors.add(item);owner.level().addFreshEntity(item);item.setDeltaMovement(Vec3.ZERO);
            owner.setYRot(0);owner.setXRot(-90);set(owner,Attributes.ATTACK_KNOCKBACK,4);owner.causeExtraKnockback(item,2,Vec3.ZERO,owner.damageSources().playerAttack(owner),2,true);
            near(item.getDeltaMovement(),new Vec3(0,2,0),"native nonliving extra branch follows full look");item.discard();
        }
        void frozenTarget(){
            var target=cow();var old=new Vec3(.1,.2,.3);target.setDeltaMovement(old);var time=game.items().timeStop();
            boolean stand=owner.entityTags().contains("have_stand");owner.entityTags().add("have_stand");
            try{
                time.begin(owner.level(),20);require(time.frozen(target)&&target.getDeltaMovement().lengthSqr()==0,"native Time Stop freezes fixture victim");
                owner.setYRot(0);owner.setXRot(-90);set(owner,Attributes.ATTACK_KNOCKBACK,10);owner.causeExtraKnockback(target,5,Vec3.ZERO,owner.damageSources().playerAttack(owner),2,true);
                require(target.getDeltaMovement().lengthSqr()==0,"direct attribute extra cannot move a frozen victim");
            }finally{time.end(owner.level());if(!stand)owner.entityTags().remove("have_stand");}
            near(target.getDeltaMovement(),old,"native time resume restores original velocity without attribute contamination");target.discard();
        }
        void pvpPacket(){
            move(other,at.add(0,0,1.5));var previous=other.connection;var recording=new Recording(other);
            try{
                other.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),recording,other,new net.minecraft.server.network.CommonListenerCookie(other.getGameProfile(),0,other.clientInformation(),false));
                other.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                ready(other,0,-90);set(owner,Attributes.ATTACK_KNOCKBACK,4);recording.packets.clear();owner.attack(other);
                var motion=recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket.class::cast).filter(p->p.id()==other.getId()).toList();
                require(motion.size()==1&&motion.getFirst().movement().y>2,"actual native PvP sends exactly one upward motion packet: "+motion);
                near(other.getDeltaMovement(),Vec3.ZERO,"native PvP restores old server movement after sending");require(!other.syncVelocity,"native PvP clears sent velocity flag");
            }finally{other.connection=previous;previous.resetPosition();move(other,at.add(7,0,0));}
        }
        void carpetPlayers(){
            ServerPlayer fake=null;
            UUID uuid=GameChecks.offlineUuid("TestKnockback");
            require(game.server().getPlayerList().getPlayer(uuid)==null,"fixed Carpet fixture identity is offline");
            Map<UUID,Object> participants=uuidMap(game,"participants"),profiles=uuidMap(game.players(),"profiles");
            Object priorParticipant=participants.remove(uuid),priorProfile=profiles.remove(uuid);
            int priorLastAlive;
            try{priorLastAlive=fieldInHierarchy(game.getClass(),"lastAliveTicks").getInt(game);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            try{
                var profile=new com.mojang.authlib.GameProfile(uuid,"TestKnockback");
                var type=Class.forName("carpet.patches.EntityPlayerMPFake");
                fake=(ServerPlayer)type.getMethod("respawnFake",net.minecraft.server.MinecraftServer.class,net.minecraft.server.level.ServerLevel.class,com.mojang.authlib.GameProfile.class,net.minecraft.server.level.ClientInformation.class)
                        .invoke(null,game.server(),owner.level(),profile,owner.clientInformation());
                var connection=(net.minecraft.network.Connection)Class.forName("carpet.patches.FakeClientConnection").getConstructor(net.minecraft.network.protocol.PacketFlow.class).newInstance(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
                game.server().getPlayerList().placeNewPlayer(connection,fake,new net.minecraft.server.network.CommonListenerCookie(profile,0,fake.clientInformation(),false));
                require(game.returnFromGameModeCommand(fake,GameType.SURVIVAL),"actual Carpet fake enrolls in running match");
                fake.setGameMode(GameType.SURVIVAL);fake.setNoGravity(true);fake.getInventory().clearContent();fake.removeAllEffects();
                fake.hasChangedDimension();fake.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                require(fake.getClass()==type&&fake.connection.getClass().getName().equals("carpet.patches.NetHandlerPlayServerFake"),"real Carpet victim and listener, without recording substitute");
                move(fake,at.add(0,0,1.5));set(fake,Attributes.KNOCKBACK_RESISTANCE,-100);
                for(float pitch:new float[]{-90,90,-45}){
                    ready(fake,0,pitch);set(owner,Attributes.ATTACK_KNOCKBACK,4);owner.attack(fake);
                    var motion=fake.getDeltaMovement();
                    require(fake.getHealth()<fake.getMaxHealth(),"native melee damages actual fake victim");
                    double expectedPower=222.2;
                    near(motion,owner.getLookAngle().scale(expectedPower),"actual fake retains complete pitched melee impulse at resistance -100");
                }
                var target=cow();ready(target,0,0);target.setOnGround(false);
                set(fake,Attributes.ATTACK_DAMAGE,2);set(fake,Attributes.ATTACK_SPEED,4);set(fake,Attributes.ATTACK_KNOCKBACK,100);
                fake.setYRot(0);fake.setXRot(-90);fake.setYHeadRot(0);fake.setSprinting(false);fake.setOnGround(true);fake.fallDistance=0;
                fieldInHierarchy(fake.getClass(),"attackStrengthTicker").setInt(fake,200);fieldInHierarchy(fake.getClass(),"itemSwapTicker").setInt(fake,200);
                fake.attack(target);
                require(target.getHealth()<100&&Math.abs(target.getDeltaMovement().y-50.2)<1e-4,"real Carpet attacker gains uncapped vertical native melee impulse");
                game.items().indirectKills().clear(target);target.discard();
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native Carpet knockback fixture",error);}
            finally{
                if(fake!=null){
                    game.items().indirectKills().clear(fake);game.items().shooters().clearStun(fake);
                    if(game.server().getPlayerList().getPlayer(fake.getUUID())==fake)fake.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("Knockback fixture cleanup")));
                    fake.discard();
                }
                participants.remove(uuid);profiles.remove(uuid);
                if(priorParticipant!=null)participants.put(uuid,priorParticipant);
                if(priorProfile!=null)profiles.put(uuid,priorProfile);
                game.players().save();
                try{fieldInHierarchy(game.getClass(),"lastAliveTicks").setInt(game,priorLastAlive);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
                require(game.server().getPlayerList().getPlayer(uuid)==null,"actual Carpet fixture fully disconnected");
            }
        }
        @SuppressWarnings("unchecked")
        private Map<UUID,Object> uuidMap(Object object,String name){
            try{return (Map<UUID,Object>)fieldInHierarchy(object.getClass(),name).get(object);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        public void close(){
            for(var actor:actors){if(actor instanceof LivingEntity living)game.items().indirectKills().clear(living);actor.discard();}
            for(var player:List.of(owner,other)){game.items().shooters().clearStun(player);game.items().indirectKills().clear(player);}
            game.config().ranked=ranked;game.config().lbcSkills=skills;saved.restore(game);otherSaved.restore(game);game.items().magic().flushCleansing();
        }
    }
    private static void near(Vec3 actual,Vec3 expected,String message){require(actual.distanceTo(expected)<1e-4,message+": actual="+actual+" expected="+expected);}
    public static void run(GameController game){
        try(var fixture=new Fixture(game)){fixture.initialize();fixture.directions();fixture.nativeRemainder();fixture.mobsAndObjects();fixture.frozenTarget();fixture.pvpPacket();fixture.carpetPlayers();}
        SkyWars2.LOGGER.info("[check] PASS attack-knockback (native player/mob melee, pitch/yaw/airborne/unbounded signed attribute, strong negative resistance, look-directed sprint/enchantment/base force, immunity, nonliving push and actual PvP motion packet and real Carpet attacker/victim)");
    }
}
