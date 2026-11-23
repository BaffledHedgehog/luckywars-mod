package me.skywars2.game.entities.mobs;

import java.util.*;

import me.skywars2.game.GameController;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.combat.LbcIndirectKills.Owner;
import me.skywars2.game.combat.LbcIndirectKills.SuspendedEffectCauses;
import me.skywars2.game.combat.MatchDamageSources.Ref;
import me.skywars2.game.effects.EffectLayers;
import me.skywars2.game.effects.FullInvisibility;
import me.skywars2.game.effects.LbcParasite;
import me.skywars2.game.effects.OwnedEffectLayers;
import me.skywars2.game.effects.ShadowAbsorption;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links;
import me.skywars2.game.match.events.LbcApocalypseEvents;
import me.skywars2.game.match.events.LbcApocalypseEvents.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.skywars2.game.entities.projectiles.ProjectileSteering;
import me.skywars2.game.match.checkpoints.MatchPolymorphCheckpoint;
import me.skywars2.game.skills.clone.LbcCloneSkills;

/** One reversible identity-preserving polymorph owner, shared by the wand and Magnesia. */
public final class LbcPolymorph {
    private static final String BODY="skywars2.polymorph.sheep", ORB="skywars2.polymorph.orb";
    static final int DURATION=200, ORB_LIFE=100, VOLLEYS=10, PER_VOLLEY=4, INTERVAL=10;
    static final double SPEED=.45, TURN=Math.toRadians(3);
    private final GameController game;
    private final Map<Entity,Form> forms=new IdentityHashMap<>();
    private final Map<Sheep,Form> bodies=new IdentityHashMap<>();
    private final List<Orb> orbs=new ArrayList<>();
    private final List<Volley> volleys=new ArrayList<>();
    private final Set<Entity> publishing=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Entity> detached=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Entity> nativeTransitions=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean restoring,rewinding;
    private int checkpointDepth;
    private final Map<Body,Boolean> heldBodies=new IdentityHashMap<>();
    private final Set<ServerPlayer> checkpointPublication=Collections.newSetFromMap(new IdentityHashMap<>());
    private long emitted;
    private int lastTick=Integer.MIN_VALUE;
    // These are status clocks, not resources, progress, match lives or ability ownership.
    private static final Set<String> STATUS=Set.of("cold","cold_cd","frozen","scared","lookback","twitching","piss","shock","target","delayed_death","nomagic","reverse_mana","void_resistance","gigaponos","all_seeing_eye","living_legend","dimensional_shifting","end_damage","nanobots_tier","overheaven","falltimer","blaze");
    private static final class Form {
        Entity original; Body sheep; final String epoch; final LbcIndirectKills.Owner cause;
        final Vec3 motion;
        final boolean invisible,noGravity,timeStopped; final float health,absorption; final int fire,air;
        final EnumMap<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        List<MobEffectInstance> effects; Object owned; CompoundTag absorptionState;
        LbcIndirectKills.SuspendedEffectCauses effectCauses;
        final Map<String,Integer> counters=new LinkedHashMap<>();
        net.minecraft.world.entity.ai.attributes.AttributeMap attributes;
        final List<ItemStack> inventory=new ArrayList<>(); final GameType mode;
        final List<Entity> companions=new ArrayList<>();
        int remaining=DURATION;boolean clonePending;
        Form(LbcPolymorph service,Entity original,Body sheep,LbcIndirectKills.Owner cause){
            this.original=original;this.sheep=sheep;this.cause=cause;epoch=service.game.items().epoch();
            invisible=original.isInvisible();noGravity=original.isNoGravity();timeStopped=original.entityTags().contains("time_stopped");motion=original.getDeltaMovement();
            fire=original.getRemainingFireTicks();air=original.getAirSupply();
            if(!(original instanceof ServerPlayer))companions.addAll(service.companions(original));
            if(original instanceof LivingEntity living){
                health=living.getHealth();absorption=living.getAbsorptionAmount();
                effectCauses=service.game.items().indirectKills().suspendEffectCauses(living);
                effects=living.getActiveEffects().stream().map(EffectLayers::copy).toList();
                owned=service.game.items().ownedEffects().suspend(living);
                var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,original.registryAccess());ShadowAbsorption.save(living,out);absorptionState=out.buildResult();
                for(var slot:EquipmentSlot.values())equipment.put(slot,living.getItemBySlot(slot).copy());
            }else{health=absorption=0;effects=List.of();absorptionState=new CompoundTag();}
            if(original instanceof ServerPlayer p){mode=p.gameMode();
                for(int n=0;n<p.getInventory().getContainerSize();n++)inventory.add(p.getInventory().getItem(n).copy());
                attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
                attributes.assignAllValues(p.getAttributes());
                for(var objective:service.game.server().getScoreboard().getObjectives()){
                    String name=objective.getName();if(!status(name))continue;
                    var score=service.game.server().getScoreboard().getPlayerScoreInfo(p,objective);
                    if(score!=null)counters.put(name,score.value());
                }
            }else {mode=null;attributes=null;}
        }
        /** Copy the saved pre-transform values; never resnapshot or suspend an already concealed body. */
        Form(Form source,Entity original,Body sheep){
            this.original=original;this.sheep=sheep;epoch=source.epoch;cause=source.cause;
            invisible=source.invisible;noGravity=source.noGravity;timeStopped=source.timeStopped;motion=source.motion;health=source.health;absorption=source.absorption;
            fire=source.fire;air=source.air;remaining=source.remaining;mode=source.mode;
            source.equipment.forEach((slot,stack)->equipment.put(slot,stack.copy()));
            effects=source.effects.stream().map(EffectLayers::copy).toList();owned=OwnedEffectLayers.copyCheckpoint(source.owned);effectCauses=source.effectCauses;
            absorptionState=source.absorptionState.copy();counters.putAll(source.counters);
            attributes=copyAttributes(source.attributes);source.inventory.forEach(stack->inventory.add(stack.copy()));
        }
    }
    private static boolean status(String name){return STATUS.contains(name)||name.startsWith("effect_");}
    private static final class Body extends Sheep {
        private boolean checkpointHeld;
        private DamageSource lastHit;private int lastHitTick=Integer.MIN_VALUE;private boolean redirected,lastRedirected;
        Body(ServerLevel level){super(EntityTypes.SHEEP,level);setPersistenceRequired();}
        @Override public void setHealth(float health){
            super.setHealth(health);
            var game=me.skywars2.SkyWars2.game();var form=game==null?null:game.items().polymorph().bodies.get(this);
            if(form!=null&&form.original instanceof ServerPlayer player)player.setHealth(getHealth());
        }
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            if(checkpointHeld)return false;
            int now=level.getServer().getTickCount();if(source==lastHit&&now==lastHitTick&&redirected!=lastRedirected)return false;
            var before=getDeltaMovement();boolean result=super.hurtServer(level,source,amount);
            if(result){lastHit=source;lastHitTick=now;lastRedirected=redirected;
                var game=me.skywars2.SkyWars2.game();var form=game==null?null:game.items().polymorph().bodies.get(this);
                if(form!=null&&form.original instanceof ServerPlayer player){var impulse=getDeltaMovement().subtract(before);
                    if(impulse.lengthSqr()>1e-12){player.addDeltaMovement(impulse);player.needsSync=true;player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));}
                }
                // Resolve native player death before returning to the attack caller. The
                // saved human HP is restored by finish before the existing death pipeline.
                if(form!=null&&!isAlive())game.items().polymorph().finish(form,true);
            }return result;
        }
        @Override public void tick(){if(!checkpointHeld)super.tick();}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushable(){return !checkpointHeld&&super.isPushable();}
    }
    private static final class Visual extends Display.ItemDisplay {
        Visual(ServerLevel level){super(EntityTypes.ITEM_DISPLAY,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    public static final class Orb {
        public final Display.ItemDisplay display;final LbcApocalypseEvents.Authority authority;public Vec3 motion;public int age;
        Orb(Visual display,LbcApocalypseEvents.Authority authority,Vec3 motion){this.display=display;this.authority=authority;this.motion=motion;}
    }
    private static final class Volley {
        final LbcApocalypseEvents.Authority authority;int left=VOLLEYS,wait;
        Volley(LbcApocalypseEvents.Authority authority){this.authority=authority;}
    }
    public LbcPolymorph(GameController game){this.game=game;}
    public boolean active(Entity entity){return forms.containsKey(entity);}
    public boolean concealed(Entity entity){return active(entity);}
    public boolean suspended(Entity entity){return !(entity instanceof ServerPlayer)&&active(entity);}
    public boolean nativeTransition(Entity entity){return nativeTransitions.contains(entity);}
    public boolean detached(Entity entity){return detached.contains(entity);}
    public List<Entity> detachedBodies(){return List.copyOf(detached);}
    public void releaseDetached(Entity entity){detached.remove(entity);}
    public void retainUnpublished(Entity entity){
        if(entity instanceof ServerPlayer||entity.isRemoved()||((ServerLevel)entity.level()).getEntity(entity.getUUID())!=null)
            throw new IllegalStateException("Only an absent, live nonplayer body may be retained by polymorph");
        detached.add(entity);game.items().lifetimes().polymorphVisibility(entity,false);
    }
    public void clearDetachedForCheckpoint(){detached.clear();}
    private List<Entity> companions(Entity original){
        var result=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        result.addAll(game.items().crocodildo().polymorphVisuals(original));
        result.addAll(game.items().helicopterEncounter().polymorphVisuals(original));
        result.addAll(game.items().transformator().polymorphVisuals(original));
        var queue=new ArrayDeque<Entity>(original.getPassengers());
        while(!queue.isEmpty()){var next=queue.removeFirst();if(next instanceof Display&&result.add(next))queue.addAll(next.getPassengers());}
        result.removeIf(e->e==original||e.isRemoved());return List.copyOf(result);
    }
    private void attach(Form form,Vec3 shift){
        form.companions.forEach(e->e.setPos(e.position().add(shift)));
        attach(form.original);for(var entity:form.companions)if(!entity.isRemoved())attach(entity);
    }
    private void dispose(Form form){
        detached.remove(form.original);game.items().mobgun().dispose(form.original);game.entityUnloaded(form.original);
        for(var entity:form.companions){detached.remove(entity);entity.discard();game.entityUnloaded(entity);}
    }
    private void detach(Entity entity){
        if(detached.contains(entity))return;
        nativeTransitions.add(entity);
        try{
            // Native removal callback retires section/UUID/ticking/tracking/navigation indexes.
            // The retained Java object is not dead: its private runtime resumes on registration.
            ((me.skywars2.mixin.entities.ActorCheckpointAccess.Body)entity).skywars2$levelCallback().onRemove(Entity.RemovalReason.CHANGED_DIMENSION);
            game.cancelQueuedEntityUnload(entity);retainUnpublished(entity);
        }finally{nativeTransitions.remove(entity);}
    }
    private void attach(Entity entity){
        if(!detached.contains(entity))return;
        nativeTransitions.add(entity);
        try{
            if(!((ServerLevel)entity.level()).addFreshEntity(entity)||entity.isRemoved())throw new IllegalStateException("Polymorph original registration rejected");
            detached.remove(entity);game.cancelQueuedEntityUnload(entity);
            game.items().lifetimes().polymorphVisibility(entity,true);
            game.items().timeStop().prepareFrozen(entity);
        }finally{nativeTransitions.remove(entity);}
    }
    public boolean body(Entity entity){return bodies.containsKey(entity);}
    public boolean masked(Entity entity){return active(entity);}
    public boolean restoring(){return restoring||rewinding;}
    public ServerPlayer owner(Entity entity){var f=bodies.get(entity);return f==null||f.cause==null?null:game.server().getPlayerList().getPlayer(f.cause.player());}
    public boolean eligible(Entity entity){return entity instanceof LivingEntity living&&!(entity instanceof Sheep)&&!active(entity)
            &&!LbcParasite.active(living)&&living.isAlive()&&!game.items().magic().suppressedAt((ServerLevel)entity.level(),entity.position())&&game.items().wands().validTarget(entity)&&!game.items().timeStop().frozen(entity)
            &&!game.items().timeStop().stopped((ServerLevel)entity.level());}
    public boolean gemEligible(Entity entity){return !(entity instanceof net.minecraft.world.entity.player.Player
            ||entity instanceof net.minecraft.world.entity.item.ItemEntity||entity instanceof Marker||entity instanceof Display||entity instanceof Interaction)
            &&!active(entity)&&!body(entity)&&!LbcSheep.managed(entity)&&!entity.entityTags().contains("magicalsheep")
            &&(!(entity instanceof LivingEntity living)||!LbcParasite.active(living))
            &&game.items().wands().validTarget(entity)&&!game.items().timeStop().frozen(entity)
            &&!game.items().timeStop().stopped((ServerLevel)entity.level())
            &&!game.items().magic().suppressedAt((ServerLevel)entity.level(),entity.position());}
    /** Operator timed grant uses the real reversible form and its authoritative countdown. */
    public boolean applyTimed(LivingEntity original,LbcIndirectKills.Owner cause,int ticks){
        if(ticks<=0)return false;
        var existing=forms.get(original);if(existing!=null){existing.remaining=ticks;return true;}
        if(transform(original,cause)==null)return false;
        forms.get(original).remaining=ticks;return true;
    }
    /** Edible Polymorph uses the same player/body/death owner, without timed expiry. */
    public boolean applyPermanent(ServerPlayer player,LbcIndirectKills.Owner cause){
        var existing=forms.get(player);
        if(existing!=null){existing.remaining=-1;return true;}
        if(transform(player,cause,true)==null)return false;
        forms.get(player).remaining=-1;return true;
    }
    public Sheep transform(LivingEntity original,LbcIndirectKills.Owner cause){return transform(original,cause,false);}
    Sheep transform(LivingEntity original,LbcIndirectKills.Owner cause,boolean rainbow){return transform(original,cause,rainbow,false);}
    /** Gem sources retain nonliving identity too; wand and Magnesia admission stay unchanged. */
    public Sheep transformGem(Entity original,LbcIndirectKills.Owner cause){return transform(original,cause,true,true);}
    /** Wand form shares the reversible entity owner, including native projectiles. */
    public void finishPlayers(){for(var form:List.copyOf(forms.values()))if(form.original instanceof ServerPlayer)finish(form,false);}
    Sheep transformWand(Entity original,LbcIndirectKills.Owner cause){
        if(original instanceof net.minecraft.world.entity.player.Player)return null;
        var sheep=transform(original,cause,true,true);
        if(sheep!=null)forms.get(original).remaining=60*20;
        return sheep;
    }
    private Sheep transform(Entity original,LbcIndirectKills.Owner cause,boolean rainbow,boolean gem){
        checkpointDepth++;try{
        if(!(gem?gemEligible(original):eligible(original))||!game.playing()||game.stage()==GameController.Stage.ENDING)return null;
        var level=(ServerLevel)original.level();if(!resident(level,original.position()))return null;
        var sheep=new Body(level);load(sheep,rainbow?"{DeathLootTable:'minecraft:empty',CustomName:'jeb_',CustomNameVisible:0b}":"{DeathLootTable:'minecraft:empty'}");
        float sheepHealth=game.balanceLegacy()?8:100;
        sheep.getAttribute(Attributes.MAX_HEALTH).setBaseValue(sheepHealth);sheep.setHealth(sheepHealth);
        if(rainbow&&game.balanceLegacy())sheep.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(20);
        sheep.setDeltaMovement(original.getDeltaMovement());
        if(rainbow){sheep.setCustomName(net.minecraft.network.chat.Component.literal("jeb_"));sheep.setCustomNameVisible(false);}
        sheep.setPos(original.position());sheep.setYRot(original.getYRot());sheep.setXRot(original.getXRot());sheep.entityTags().add(BODY);game.items().markCurrentItemEpoch(sheep);copyOnceFlags(original,sheep);
        if(original instanceof ServerPlayer p){p.closeContainer();game.items().fpvDrone().release(p);game.items().rifleZoom().forget(p);}
        var form=new Form(this,original,sheep,cause);forms.put(original,form);bodies.put(sheep,form);publishing.add(sheep);
        try{if(!level.addFreshEntity(sheep)||sheep.isRemoved()){forms.remove(original);bodies.remove(sheep);if(original instanceof LivingEntity living)game.items().ownedEffects().resume(living,form.owned);return null;}}
        finally{publishing.remove(sheep);}
        if(original.getTeam()!=null)game.server().getScoreboard().addPlayerToTeam(sheep.getScoreboardName(),original.getTeam());
        if(original instanceof LivingEntity living){
            game.items().frost().suspendForm(living);living.stopUsingItem();living.removeAllEffects();living.setAbsorptionAmount(0);
            for(var slot:EquipmentSlot.values())living.setItemSlot(slot,ItemStack.EMPTY);
        }
        original.stopRiding();for(var passenger:List.copyOf(original.getPassengers()))if(!form.companions.contains(passenger))passenger.stopRiding();
        original.clearFire();original.setAirSupply(original.getMaxAirSupply());
        if(original instanceof ServerPlayer p){
            p.setInvisible(true);
            p.getInventory().clearContent();p.setGameMode(GameType.ADVENTURE);
            for(var attribute:p.getAttributes().getSyncableAttributes())attribute.removeModifiers();
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(sheepHealth);p.setHealth(sheepHealth);
            p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.23);p.getAttribute(Attributes.SCALE).setBaseValue(.85);
            p.setSprinting(false);p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();
            form.counters.keySet().forEach(k->game.items().mana().score(p,k).set(0));
            sheep.setNoAi(true);sheep.setNoGravity(true);sheep.noPhysics=true;
            FullInvisibility.effectsChanged(p);p.inventoryMenu.sendAllDataToRemote();game.items().invalidateHud(p);
        }else{original.entityTags().add("time_stopped");for(var entity:form.companions)detach(entity);detach(original);}
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,sheep.getX(),sheep.getY()+.5,sheep.getZ(),20,.1,.1,.1,.5);
        return sheep;

        }finally{checkpointDepth--;}
    }
    public boolean redirectDamage(LivingEntity original,ServerLevel level,DamageSource source,float amount){
        checkpointDepth++;try{
        if(!(original instanceof ServerPlayer))return false;
        var form=forms.get(original);if(form==null)return false;
        boolean old=form.sheep.redirected;form.sheep.redirected=true;
        try{return form.sheep.hurtServer(level,source,amount);}finally{form.sheep.redirected=old;}

        }finally{checkpointDepth--;}
    }
    /** HP drains must resolve a killed sheep before another match phase observes its human body. */
    public void finishDirectDamage(LivingEntity target){
        var form=bodies.get(target);if(form!=null&&!target.isAlive())finish(form,true);
    }
    public ServerPlayer playerOfBody(LivingEntity target){
        var form=bodies.get(target);return form!=null&&form.original instanceof ServerPlayer player?player:null;
    }
    public void clearDebuffs(ServerPlayer player){var form=forms.get(player);if(form!=null)finish(form,false);}
    private void finish(Form form,boolean died){
        checkpointDepth++;try{
        if(!forms.remove(form.original,form))return;bodies.remove(form.sheep);
        var original=form.original;var at=form.sheep.position();var shift=at.subtract(original.position());var motion=form.sheep.getDeltaMovement();copyOnceFlags(form.sheep,original);
        if(form.sheep.getTeam()!=null)game.server().getScoreboard().removePlayerFromTeam(form.sheep.getScoreboardName(),form.sheep.getTeam());
        if(!form.sheep.isRemoved())form.sheep.discard();
        if(original.isRemoved()){if(!(original instanceof ServerPlayer))dispose(form);return;}
        restoring=true;
        try{
            if(!(original instanceof ServerPlayer)){original.setPos(at);original.setDeltaMovement(motion);}
            original.setInvisible(form.invisible);original.setNoGravity(form.noGravity);
            if(!form.timeStopped)original.entityTags().remove("time_stopped");
            original.setRemainingFireTicks(form.fire);original.setAirSupply(form.air);
            if(!(original instanceof LivingEntity living)){
                if(died)dispose(form);else attach(form,shift);return;
            }
            if(original instanceof ServerPlayer p){
                p.removeAllEffects();p.getInventory().clearContent();for(int n=0;n<form.inventory.size();n++)p.getInventory().setItem(n,form.inventory.get(n));
                p.getAttributes().assignAllValues(form.attributes);
                p.setGameMode(form.mode);form.counters.forEach((k,v)->game.items().mana().score(p,k).set(v));
            }
            form.equipment.forEach(living::setItemSlot);
            var restoredEffects=new HashMap<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,MobEffectInstance>();
            for(var effect:form.effects)if(living.addEffect(EffectLayers.copy(effect))){
                var active=living.getEffect(effect.getEffect());
                if(active!=null&&active.getAmplifier()==effect.getAmplifier())restoredEffects.put(effect.getEffect(),active);
            }
            ShadowAbsorption.beforeLoad(living);ShadowAbsorption.load(living,TagValueInput.create(ProblemReporter.DISCARDING,original.registryAccess(),form.absorptionState));
            game.items().ownedEffects().resume(living,form.owned);
            game.items().indirectKills().resumeEffectCauses(living,form.effectCauses,restoredEffects);
            game.items().frost().resumeForm(living);
            living.setHealth(Math.min(form.health,living.getMaxHealth()));living.setAbsorptionAmount(form.absorption);
            if(original instanceof ServerPlayer p){FullInvisibility.effectsChanged(p);p.inventoryMenu.sendAllDataToRemote();p.onUpdateAbilities();game.items().mana().inventoryChanged(p);game.items().magic().inventoryChanged(p);game.items().invalidateHud(p);}
        }finally{restoring=false;}
        if(!(original instanceof ServerPlayer)&&!died)attach(form,shift);
        if(died){
            if(original instanceof ServerPlayer p){var source=form.sheep.getLastDamageSource();
                if(source==null)game.items().indirectKills().inheritedExecution(p,form.cause);
                else {
                    var killer=form.sheep.getKillCredit();var inherited=game.items().indirectKills().babylonCause(source);
                    var cause=inherited!=null?inherited:killer instanceof ServerPlayer author?game.items().indirectKills().wildCause(author):form.cause;
                    game.items().indirectKills().withCause(p,cause,()->p.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE));
                }
            }else dispose(form);
        }

        }finally{checkpointDepth--;}
    }
    private static boolean resident(ServerLevel level,Vec3 at){var p=BlockPos.containing(at);return level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    private boolean live(LbcApocalypseEvents.Authority a){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(a.epoch());}
    public void magnesia(LbcApocalypseEvents.Authority authority){
        checkpointDepth++;try{volleys.add(new Volley(authority));
        }finally{checkpointDepth--;}
    }
    private List<LivingEntity> targets(){var result=new ArrayList<LivingEntity>();for(var level:game.server().getAllLevels())for(var e:level.getAllEntities())if(eligible(e))result.add((LivingEntity)e);return result;}
    public Orb orb(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at,Vec3 direction){
        checkpointDepth++;try{
        if(!live(authority)||!resident(level,at))return null;var display=new Visual(level);
        load(display,"{billboard:'center',brightness:{sky:15,block:15},shadow_radius:0f,Glowing:1b,glow_color_override:14619903,teleport_duration:1,transformation:{scale:[0.65f,0.65f,0.65f],translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}");
        var stack=new ItemStack(Items.STICK);stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:polymorph_sphere"));display.setItemStack(stack);display.setPos(at);
        display.entityTags().add(ORB);game.items().markCurrentItemEpoch(display);publishing.add(display);
        try{if(!level.addFreshEntity(display)||display.isRemoved())return null;}finally{publishing.remove(display);}
        var orb=new Orb(display,authority,direction.normalize().scale(SPEED));display.setDeltaMovement(orb.motion);orbs.add(orb);emitted++;return orb;

        }finally{checkpointDepth--;}
    }
    static Vec3 steer(Vec3 direction,Vec3 toward){
        return ProjectileSteering.turn(direction.normalize(),toward,TURN).scale(SPEED);
    }
    public void tick(){
        checkpointDepth++;try{
        if(rewinding)return;
        int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        for(var form:List.copyOf(forms.values())){
            var original=form.original;var sheep=form.sheep;
            if(original instanceof LivingEntity living&&LbcParasite.active(living)){finish(form,false);continue;}
            if(!game.items().currentEpoch(form.epoch)||original.isRemoved()||original.level()!=sheep.level()){finish(form,false);continue;}
            if(!sheep.isAlive()||sheep.isRemoved()){finish(form,sheep.getHealth()<=0||sheep.getRemovalReason()==Entity.RemovalReason.KILLED);continue;}
            if(game.items().magic().suppressedAt((ServerLevel)original.level(),sheep.position())){finish(form,false);continue;}
            if(game.items().timeStop().stopped((ServerLevel)original.level())||game.items().timeStop().frozen(sheep))continue;
            if(original instanceof ServerPlayer p){
                sheep.setPos(p.position());sheep.setYRot(p.getYRot());sheep.setXRot(p.getXRot());sheep.setYHeadRot(p.getYHeadRot());sheep.yBodyRot=p.yBodyRot;
                p.setHealth(sheep.getHealth());p.setSprinting(false);p.removeAllEffects();p.setInvisible(true);
                form.counters.keySet().forEach(k->game.items().mana().score(p,k).set(0));
            }
            if(form.remaining>=0&&--form.remaining<=0)finish(form,false);
        }
        if(game.items().wands().timeStopped())return;
        for(var volley:List.copyOf(volleys)){
            if(!live(volley.authority)){volleys.remove(volley);continue;}if(volley.wait-->0)continue;
            var candidates=targets();if(!candidates.isEmpty())for(int n=0;n<PER_VOLLEY;n++){
                var random=game.server().overworld().getRandom();var target=candidates.get(random.nextInt(candidates.size()));
                var direction=new Vec3(random.nextGaussian(),random.nextGaussian(),random.nextGaussian());
                orb(volley.authority,(ServerLevel)target.level(),target.position().add(random.nextDouble()*20-10,15,random.nextDouble()*20-10),direction);
            }
            volley.wait=INTERVAL-1;if(--volley.left==0)volleys.remove(volley);
        }
        for(var orb:List.copyOf(orbs)){
            var display=orb.display;var level=(ServerLevel)display.level();var from=display.position();
            if(!live(orb.authority)||display.isRemoved()||!resident(level,from)||orb.age>=ORB_LIFE){retire(orb);continue;}
            if(game.items().timeStop().stopped(level))continue;
            // Native motion mirrors the service flight vector so cloning can apply its normal source spread.
            orb.motion=display.getDeltaMovement();
            LivingEntity nearest=null;double distance=80*80;
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,from).inflate(80),this::eligible)){
                double d=entity.getEyePosition().distanceToSqr(from);if(d<distance){distance=d;nearest=entity;}
            }
            if(nearest!=null)orb.motion=steer(orb.motion,nearest.getEyePosition().subtract(from));display.setDeltaMovement(orb.motion);
            var to=from.add(orb.motion);LivingEntity hit=null;double closest=Double.POSITIVE_INFINITY;
            for(var candidate:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(.25),this::eligible)){
                var box=candidate.getBoundingBox().inflate(.2);var point=box.contains(from)?Optional.of(from):box.clip(from,to);
                if(point.isPresent()){double d=point.get().distanceToSqr(from);if(d<closest){closest=d;hit=candidate;}}
            }
            boolean suppressed=false;
            for(double d=0;d<=SPEED;d+=.1)if(game.items().magic().suppressedAt(level,from.add(orb.motion.normalize().scale(d)))){suppressed=true;break;}
            if(suppressed){retire(orb);continue;}
            if(hit!=null){transform(hit,orb.authority.cause());retire(orb);continue;}
            display.setPos(to);if(++orb.age>=ORB_LIFE)retire(orb);
        }

        }finally{checkpointDepth--;}
    }
    private void retire(Orb orb){orbs.remove(orb);if(!orb.display.isRemoved())orb.display.discard();}
    private static void load(Entity entity,String nbt){try{entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),TagParser.parseCompoundFully(nbt)));}catch(Exception e){throw new IllegalStateException(e);}}
    public void loaded(Entity e){
        checkpointDepth++;try{if(rewinding||heldBodies.containsKey(e))return;var copied=bodies.get(e);if(copied!=null)copied.clonePending=false;if((e.entityTags().contains(BODY)||e.entityTags().contains(ORB))&&!publishing.contains(e)&&!bodies.containsKey(e)&&orbs.stream().noneMatch(o->o.display==e))e.discard();
        }finally{checkpointDepth--;}
    }
    public void unloaded(Entity e){
        checkpointDepth++;try{if(rewinding||detached.contains(e))return;for(var form:List.copyOf(forms.values()))if(form.sheep==e||form.original==e){if(form.clonePending){forms.remove(form.original,form);bodies.remove(form.sheep,form);}else finish(form,false);}for(var orb:List.copyOf(orbs))if(orb.display==e)retire(orb);
        }finally{checkpointDepth--;}
    }
    public void forget(ServerPlayer p){
        checkpointDepth++;try{if(rewinding)return;var form=forms.get(p);if(form!=null)finish(form,false);
        }finally{checkpointDepth--;}
    }
    public void stop(){
        checkpointDepth++;try{rewinding=false;clearHeldBodies(true);for(var form:List.copyOf(forms.values()))finish(form,false);
        for(var entity:List.copyOf(detached)){detached.remove(entity);entity.discard();game.entityUnloaded(entity);}
        for(var orb:List.copyOf(orbs))retire(orb);volleys.clear();
        }finally{checkpointDepth--;}
    }
    public List<LbcSheep.SheepView> sheepViews(){return forms.values().stream().map(f->new LbcSheep.SheepView(f.sheep.getUUID(),f.cause==null?null:f.cause.player(),f.sheep.position())).toList();}
    public void forgetAuthor(UUID author){for(var f:List.copyOf(forms.values()))if(f.cause!=null&&author.equals(f.cause.player()))finish(f,false);}
    public int count(){return forms.size();}public int remaining(Entity entity){var form=forms.get(entity);return form==null?0:form.remaining;}
    public int remaining(LivingEntity entity){return remaining((Entity)entity);}
    public long emittedTotal(){return emitted;}
    public List<Orb> orbs(){return List.copyOf(orbs);}public int volleyCount(){return volleys.size();}
    /** Form images own the suspended human/mob values; current body physics belongs to native snapshots. */
    public void checkpointBoundary(){if(checkpointDepth!=0||restoring||!publishing.isEmpty())throw new IllegalStateException("Finish polymorph callbacks before checkpointing");}
    private static net.minecraft.world.entity.ai.attributes.AttributeMap copyAttributes(net.minecraft.world.entity.ai.attributes.AttributeMap source){
        if(source==null)return null;
        var result=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        result.assignAllValues(source);return result;
    }
    private void clearHeldBodies(boolean discard){
        for(var entry:List.copyOf(heldBodies.entrySet())){
            var body=entry.getKey();body.checkpointHeld=false;
            if(discard){if(!body.isRemoved())body.discard();}else body.setInvisible(entry.getValue());
        }
        heldBodies.clear();
    }
    /** Never finish(): it would refund suspended equipment/effects or execute the old death continuation. */
    public void prepareCheckpointFrozen(){
        checkpointBoundary();rewinding=true;checkpointPublication.clear();clearHeldBodies(false);forms.clear();bodies.clear();
        var old=List.copyOf(orbs);orbs.clear();volleys.clear();for(var orb:old)if(!orb.display.isRemoved())orb.display.discard();
    }
    public void publishCheckpointFrozen(){
        checkpointBoundary();
        for(var player:List.copyOf(checkpointPublication)){
            if(game.server().getPlayerList().getPlayer(player.getUUID())!=player)throw new IllegalStateException("Polymorph current human changed before publication");
            FullInvisibility.effectsChanged(player);player.inventoryMenu.sendAllDataToRemote();player.onUpdateAbilities();game.items().invalidateHud(player);
        }
        checkpointPublication.clear();rewinding=false;
    }
    public void cancelCheckpointFrozen(){checkpointBoundary();checkpointPublication.clear();clearHeldBodies(false);forms.clear();bodies.clear();rewinding=false;}
    public MatchPolymorphCheckpoint checkpoint(MatchDamageSources sources){return new MatchPolymorphCheckpoint(game,sources);}
    public List<FormCheckpoint> checkpointForms(MatchDamageSources sources){
        checkpointBoundary();if(rewinding)throw new IllegalStateException("Publish polymorph before capture");
        return forms.values().stream().map(form->new FormCheckpoint(form,sources)).toList();
    }
    public Set<UUID> transientCheckpointActors(){var result=new LinkedHashSet<UUID>();for(var orb:orbs)result.add(orb.display.getUUID());return Set.copyOf(result);}
    public final class FormCheckpoint {
        private final Form form;
        private final MatchSpellCheckpoint.Links links;
        private final MatchDamageSources.Ref original,sheep;
        private final List<MatchDamageSources.Ref> companions;
        private final int remaining;
        private final EnumMap<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        private final List<ItemStack> inventory;
        private final List<MobEffectInstance> effects;
        private final LbcIndirectKills.SuspendedEffectCauses effectCauses;
        private final Object owned;
        private final CompoundTag absorption;
        private final Map<String,Integer> counters;
        private final net.minecraft.world.entity.ai.attributes.AttributeMap attributes;
        private final boolean sheepInvisible;
        FormCheckpoint(Form form,MatchDamageSources sources){
            this.form=form;links=new MatchSpellCheckpoint.Links(sources);original=links.key(form.original);sheep=links.key(form.sheep);
            companions=form.companions.stream().map(links::key).toList();
            if(original.historical()!=null||sheep.historical()!=null)throw new IllegalStateException("Polymorph requires canonical current human/mob and sheep bodies");
            remaining=form.remaining;form.equipment.forEach((slot,stack)->equipment.put(slot,stack.copy()));
            inventory=form.inventory.stream().map(ItemStack::copy).toList();effects=form.effects.stream().map(EffectLayers::copy).toList();effectCauses=form.effectCauses;
            owned=OwnedEffectLayers.copyCheckpoint(form.owned);absorption=form.absorptionState.copy();counters=new LinkedHashMap<>(form.counters);attributes=copyAttributes(form.attributes);
            sheepInvisible=form.sheep.isInvisible();
        }
        public Set<UUID> actors(){var result=new LinkedHashSet<UUID>();result.add(original.id());result.add(sheep.id());companions.forEach(e->result.add(e.id()));return Set.copyOf(result);}
        public Set<UUID> resolve(java.util.function.Function<UUID,Entity> lookup){return links.resolve(lookup);}
        /** Hold only the real retained native sheep while its human is genuinely offline. */
        public void hold(java.util.function.Function<UUID,Entity> lookup){
            var body=lookup.apply(sheep.id());if(body==null)return;
            if(!(body instanceof Body current)||body.isRemoved()||body.level()!=form.sheep.level())throw new IllegalStateException("Restore the retained native polymorph sheep before holding an offline form");
            heldBodies.put(current,sheepInvisible);current.checkpointHeld=true;current.setInvisible(true);
        }
        public Runnable prepare(){
            var human=links.get(original);var body=(Body)links.get(sheep);
            var visuals=companions.stream().map(links::get).toList();
            if(human.level()!=body.level())throw new IllegalStateException("Restore polymorph bodies in their same captured dimension");
            var items=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);equipment.forEach((slot,stack)->items.put(slot,stack.copy()));
            var bag=inventory.stream().map(ItemStack::copy).toList();var statuses=effects.stream().map(EffectLayers::copy).toList();
            var leases=OwnedEffectLayers.copyCheckpoint(owned);var shadow=absorption.copy();var attrs=copyAttributes(attributes);
            return ()->{
                forms.remove(form.original,form);bodies.remove(form.sheep,form);heldBodies.remove(body);body.checkpointHeld=false;body.setInvisible(sheepInvisible);
                form.original=human;form.sheep=body;form.remaining=remaining;
                form.companions.clear();form.companions.addAll(visuals);
                form.equipment.clear();form.equipment.putAll(items);form.inventory.clear();form.inventory.addAll(bag);form.effects=statuses;form.effectCauses=effectCauses;
                form.owned=leases;form.absorptionState=shadow;form.counters.clear();form.counters.putAll(counters);form.attributes=attrs;
                // Same-tick duplicate-hit cache is transient; native health/motion/combat remain canonical.
                body.lastHit=null;body.lastHitTick=Integer.MIN_VALUE;body.redirected=false;body.lastRedirected=false;
                forms.put(human,form);bodies.put(body,form);if(human instanceof ServerPlayer player)checkpointPublication.add(player);
            };
        }
    }


    private static void copyOnceFlags(Entity source,Entity target){
        for(var flag:List.of("doubled","quadried"))if(source.entityTags().contains(flag))target.entityTags().add(flag);
    }
    public boolean cloneRecognizes(Entity actor){return actor instanceof Body || actor instanceof Visual || actor.entityTags().contains(BODY)||actor.entityTags().contains(ORB);}
    public boolean cloneGameplay(Entity actor){return actor.entityTags().contains(ORB)&&orbs.stream().anyMatch(orb->orb.display==actor);}
    public boolean cloneProtected(Entity actor){
        if(forms.containsKey(actor))return true;
        var form=bodies.get(actor);return form!=null&&form.original instanceof ServerPlayer;
    }
    public LbcCloneSkills.ActorImage cloneImage(Entity actor){
        if(cloneProtected(actor))return null;
        for(var orb:orbs)if(orb.display==actor){
            var authority=orb.authority;int age=orb.age;
            return new LbcCloneSkills.ActorImage(){
                public Entity create(Entity original,ServerLevel level){return new Visual(level);}
                public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                    var copied=new Orb((Visual)clone,authority,clone.getDeltaMovement());copied.age=age;orbs.add(copied);
                }
                public void rollback(Entity clone){orbs.removeIf(orb->orb.display==clone);}
            };
        }
        var source=bodies.get(actor);
        if(source==null||source.original instanceof ServerPlayer)return null;
        // Immutable operation-local saved values; every install copies collections again.
        var saved=new Form(source,source.original,source.sheep);var hidden=source.original;
        var rollbackAuxiliaries=new IdentityHashMap<Entity,Runnable>();
        return new LbcCloneSkills.ActorImage(){
            public Entity create(Entity original,ServerLevel level){return new Body(level);}
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                var sheep=(Body)clone;
                var auxiliary=game.items().cloneSkills().copyAuxiliary(hidden,body->{
                    if(body instanceof ServerPlayer)throw new IllegalStateException("Polymorph copy requires an independent nonplayer original");
                    var form=new Form(saved,body,sheep);form.clonePending=true;body.setPos(sheep.position());body.entityTags().add("time_stopped");copyOnceFlags(sheep,body);
                    forms.put(body,form);bodies.put(sheep,form);
                });
                var form=forms.get(auxiliary.root());form.companions.addAll(companions(auxiliary.root()));
                for(var entity:form.companions)detach(entity);
                rollbackAuxiliaries.put(clone,auxiliary::rollback);
            }
            public void rollback(Entity clone){
                var form=bodies.remove(clone);if(form!=null){forms.remove(form.original,form);form.companions.forEach(e->{detached.remove(e);e.discard();});}
                var rollback=rollbackAuxiliaries.remove(clone);if(rollback!=null)rollback.run();
            }
        };
    }
}
