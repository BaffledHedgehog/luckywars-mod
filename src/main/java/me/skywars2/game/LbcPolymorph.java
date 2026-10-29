package me.skywars2.game;

import java.util.*;
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

/** One reversible identity-preserving polymorph owner, shared by the wand and Magnesia. */
public final class LbcPolymorph {
    private static final String BODY="skywars2.polymorph.sheep", ORB="skywars2.polymorph.orb";
    static final int DURATION=200, ORB_LIFE=100, VOLLEYS=10, PER_VOLLEY=4, INTERVAL=10;
    static final double SPEED=.45, TURN=Math.toRadians(3);
    private final GameController game;
    private final Map<LivingEntity,Form> forms=new IdentityHashMap<>();
    private final Map<Sheep,Form> bodies=new IdentityHashMap<>();
    private final List<Orb> orbs=new ArrayList<>();
    private final List<Volley> volleys=new ArrayList<>();
    private final Set<Entity> publishing=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean restoring;
    private long emitted;
    private int lastTick=Integer.MIN_VALUE;
    // These are status clocks, not resources, progress, match lives or ability ownership.
    private static final Set<String> STATUS=Set.of("cold","cold_cd","frozen","scared","lookback","twitching","piss","shock","target","delayed_death","nomagic","reverse_mana","void_resistance","gigaponos","all_seeing_eye","living_legend","dimensional_shifting","end_damage","nanobots_tier","overheaven","falltimer","blaze");
    private static final class Form {
        final LivingEntity original; final Body sheep; final String epoch; final LbcIndirectKills.Owner cause;
        final boolean invisible,noGravity; final float health,absorption; final int fire,air;
        final EnumMap<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        final List<MobEffectInstance> effects; final Object owned; final CompoundTag absorptionState;
        final Map<String,Integer> counters=new LinkedHashMap<>();
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes;
        final List<ItemStack> inventory=new ArrayList<>(); final GameType mode;
        int remaining=DURATION;
        Form(LbcPolymorph service,LivingEntity original,Body sheep,LbcIndirectKills.Owner cause){
            this.original=original;this.sheep=sheep;this.cause=cause;epoch=service.game.items().epoch();
            invisible=original.isInvisible();noGravity=original.isNoGravity();
            health=original.getHealth();absorption=original.getAbsorptionAmount();fire=original.getRemainingFireTicks();air=original.getAirSupply();
            effects=original.getActiveEffects().stream().map(EffectLayers::copy).toList();
            owned=service.game.items().ownedEffects().suspend(original);
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,original.registryAccess());ShadowAbsorption.save(original,out);absorptionState=out.buildResult();
            for(var slot:EquipmentSlot.values())equipment.put(slot,original.getItemBySlot(slot).copy());
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
    }
    private static boolean status(String name){return STATUS.contains(name)||name.startsWith("effect_");}
    private static final class Body extends Sheep {
        private DamageSource lastHit;private int lastHitTick=Integer.MIN_VALUE;private boolean redirected,lastRedirected;
        Body(ServerLevel level){super(EntityTypes.SHEEP,level);setPersistenceRequired();}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            int now=level.getServer().getTickCount();if(source==lastHit&&now==lastHitTick&&redirected!=lastRedirected)return false;
            var before=getDeltaMovement();boolean result=super.hurtServer(level,source,amount);
            if(result){lastHit=source;lastHitTick=now;lastRedirected=redirected;
                var game=me.skywars2.SkyWars2.game();var form=game==null?null:game.items().polymorph().bodies.get(this);
                if(form!=null&&form.original instanceof ServerPlayer player){var impulse=getDeltaMovement().subtract(before);
                    if(impulse.lengthSqr()>1e-12){player.addDeltaMovement(impulse);player.needsSync=true;player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));}
                }
            }return result;
        }
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushable(){return false;}
        @Override public void push(Entity other){}
        @Override public boolean canBeCollidedWith(Entity other){return false;}
    }
    private static final class Visual extends Display.ItemDisplay {
        Visual(ServerLevel level){super(EntityTypes.ITEM_DISPLAY,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    static final class Orb {
        final Display.ItemDisplay display;final LbcApocalypseEvents.Authority authority;Vec3 motion;int age;
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
    public boolean body(Entity entity){return bodies.containsKey(entity);}
    public boolean masked(Entity entity){return active(entity);}
    public boolean restoring(){return restoring;}
    public ServerPlayer owner(Entity entity){var f=bodies.get(entity);return f==null||f.cause==null?null:game.server().getPlayerList().getPlayer(f.cause.player());}
    public boolean eligible(Entity entity){return entity instanceof LivingEntity living&&!(entity instanceof Sheep)&&!active(entity)
            &&living.isAlive()&&!game.items().magic().suppressedAt((ServerLevel)entity.level(),entity.position())&&game.items().wands().validTarget(entity)&&!game.items().timeStop().frozen(entity)
            &&!game.items().timeStop().stopped((ServerLevel)entity.level());}
    Sheep transform(LivingEntity original,LbcIndirectKills.Owner cause){return transform(original,cause,false);}
    Sheep transform(LivingEntity original,LbcIndirectKills.Owner cause,boolean rainbow){
        if(!eligible(original)||!game.playing()||game.stage()==GameController.Stage.ENDING)return null;
        var level=(ServerLevel)original.level();if(!resident(level,original.position()))return null;
        var sheep=new Body(level);load(sheep,rainbow?"{DeathLootTable:'minecraft:empty',CustomName:'jeb_',CustomNameVisible:0b,attributes:[{id:'minecraft:knockback_resistance',base:20}]}":"{DeathLootTable:'minecraft:empty'}");
        if(rainbow){sheep.setCustomName(net.minecraft.network.chat.Component.literal("jeb_"));sheep.setCustomNameVisible(false);}
        sheep.setPos(original.position());sheep.setYRot(original.getYRot());sheep.setXRot(original.getXRot());sheep.entityTags().add(BODY);game.items().markCurrentItemEpoch(sheep);
        if(original instanceof ServerPlayer p){p.closeContainer();game.items().fpvDrone().release(p);game.items().rifleZoom().forget(p);}
        var form=new Form(this,original,sheep,cause);forms.put(original,form);bodies.put(sheep,form);publishing.add(sheep);
        try{if(!level.addFreshEntity(sheep)||sheep.isRemoved()){forms.remove(original);bodies.remove(sheep);game.items().ownedEffects().resume(original,form.owned);return null;}}
        finally{publishing.remove(sheep);}
        if(original.getTeam()!=null)game.server().getScoreboard().addPlayerToTeam(sheep.getScoreboardName(),original.getTeam());
        game.items().frost().suspendForm(original);
        original.stopUsingItem();original.stopRiding();original.removeAllEffects();original.setAbsorptionAmount(0);original.clearFire();original.setAirSupply(original.getMaxAirSupply());original.setInvisible(true);
        for(var slot:EquipmentSlot.values())original.setItemSlot(slot,ItemStack.EMPTY);
        if(original instanceof ServerPlayer p){
            p.getInventory().clearContent();p.setGameMode(GameType.ADVENTURE);
            for(var attribute:p.getAttributes().getSyncableAttributes())attribute.removeModifiers();
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8);p.setHealth(8);
            p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.23);p.getAttribute(Attributes.SCALE).setBaseValue(.85);
            p.setSprinting(false);p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();
            form.counters.keySet().forEach(k->game.items().mana().score(p,k).set(0));
            sheep.setNoAi(true);sheep.setNoGravity(true);sheep.noPhysics=true;
            FullInvisibility.effectsChanged(p);p.inventoryMenu.sendAllDataToRemote();game.items().invalidateHud(p);
        }else{original.setNoGravity(true);original.setDeltaMovement(Vec3.ZERO);}
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,sheep.getX(),sheep.getY()+.5,sheep.getZ(),20,.1,.1,.1,.5);
        return sheep;
    }
    public boolean redirectDamage(LivingEntity original,ServerLevel level,DamageSource source,float amount){
        var form=forms.get(original);if(form==null)return false;
        boolean old=form.sheep.redirected;form.sheep.redirected=true;
        try{return form.sheep.hurtServer(level,source,amount);}finally{form.sheep.redirected=old;}
    }
    private void finish(Form form,boolean died){
        if(!forms.remove(form.original,form))return;bodies.remove(form.sheep);
        var original=form.original;var at=form.sheep.position();var motion=form.sheep.getDeltaMovement();
        if(form.sheep.getTeam()!=null)game.server().getScoreboard().removePlayerFromTeam(form.sheep.getScoreboardName(),form.sheep.getTeam());
        if(!form.sheep.isRemoved())form.sheep.discard();
        if(original.isRemoved())return;
        restoring=true;
        try{
            if(!(original instanceof ServerPlayer)){original.setPos(at);original.setDeltaMovement(motion);}
            original.setInvisible(form.invisible);original.setNoGravity(form.noGravity);
            original.setRemainingFireTicks(form.fire);original.setAirSupply(form.air);
            if(original instanceof ServerPlayer p){
                p.removeAllEffects();p.getInventory().clearContent();for(int n=0;n<form.inventory.size();n++)p.getInventory().setItem(n,form.inventory.get(n));
                p.getAttributes().assignAllValues(form.attributes);
                p.setGameMode(form.mode);form.counters.forEach((k,v)->game.items().mana().score(p,k).set(v));
            }
            form.equipment.forEach(original::setItemSlot);
            for(var effect:form.effects)original.addEffect(EffectLayers.copy(effect));
            ShadowAbsorption.beforeLoad(original);ShadowAbsorption.load(original,TagValueInput.create(ProblemReporter.DISCARDING,original.registryAccess(),form.absorptionState));
            game.items().ownedEffects().resume(original,form.owned);
            game.items().frost().resumeForm(original);
            original.setHealth(Math.min(form.health,original.getMaxHealth()));original.setAbsorptionAmount(form.absorption);
            if(original instanceof ServerPlayer p){FullInvisibility.effectsChanged(p);p.inventoryMenu.sendAllDataToRemote();p.onUpdateAbilities();game.items().mana().inventoryChanged(p);game.items().magic().inventoryChanged(p);game.items().invalidateHud(p);}
        }finally{restoring=false;}
        if(died){
            if(original instanceof ServerPlayer p){var source=form.sheep.getLastDamageSource();
                if(source==null)game.items().indirectKills().inheritedExecution(p,form.cause);
                else {
                    var killer=form.sheep.getKillCredit();var inherited=game.items().indirectKills().babylonCause(source);
                    var cause=inherited!=null?inherited:killer instanceof ServerPlayer author?game.items().indirectKills().wildCause(author):form.cause;
                    game.items().indirectKills().withCause(p,cause,()->p.hurtServer(p.level(),p.damageSources().genericKill(),Float.MAX_VALUE));
                }
            }else game.items().mobgun().dispose(original);
        }
    }
    private static boolean resident(ServerLevel level,Vec3 at){var p=BlockPos.containing(at);return level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    private boolean live(LbcApocalypseEvents.Authority a){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(a.epoch());}
    public void magnesia(LbcApocalypseEvents.Authority authority){volleys.add(new Volley(authority));}
    private List<LivingEntity> targets(){var result=new ArrayList<LivingEntity>();for(var level:game.server().getAllLevels())for(var e:level.getAllEntities())if(eligible(e))result.add((LivingEntity)e);return result;}
    Orb orb(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at,Vec3 direction){
        if(!live(authority)||!resident(level,at))return null;var display=new Visual(level);
        load(display,"{billboard:'center',brightness:{sky:15,block:15},shadow_radius:0f,Glowing:1b,glow_color_override:14619903,teleport_duration:1,transformation:{scale:[0.65f,0.65f,0.65f]}}");
        var stack=new ItemStack(Items.STICK);stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:polymorph_sphere"));display.setItemStack(stack);display.setPos(at);
        display.entityTags().add(ORB);game.items().markCurrentItemEpoch(display);publishing.add(display);
        try{if(!level.addFreshEntity(display)||display.isRemoved())return null;}finally{publishing.remove(display);}
        var orb=new Orb(display,authority,direction.normalize().scale(SPEED));orbs.add(orb);emitted++;return orb;
    }
    static Vec3 steer(Vec3 direction,Vec3 toward){
        var a=direction.normalize();var b=toward.normalize();double dot=Math.max(-1,Math.min(1,a.dot(b))),angle=Math.acos(dot);
        if(angle<=TURN)return b.scale(SPEED);
        var perpendicular=b.subtract(a.scale(dot));if(perpendicular.lengthSqr()<1e-12)perpendicular=a.cross(Math.abs(a.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0));
        return a.scale(Math.cos(TURN)).add(perpendicular.normalize().scale(Math.sin(TURN))).normalize().scale(SPEED);
    }
    public void tick(){
        int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        for(var form:List.copyOf(forms.values())){
            var original=form.original;var sheep=form.sheep;
            if(!game.items().currentEpoch(form.epoch)||original.isRemoved()||original.level()!=sheep.level()){finish(form,false);continue;}
            if(!sheep.isAlive()||sheep.isRemoved()){finish(form,true);continue;}
            if(game.items().magic().suppressedAt((ServerLevel)original.level(),sheep.position())){finish(form,false);continue;}
            if(game.items().timeStop().stopped((ServerLevel)original.level())||game.items().timeStop().frozen(sheep))continue;
            if(original instanceof ServerPlayer p){
                sheep.setPos(p.position());sheep.setYRot(p.getYRot());sheep.setXRot(p.getXRot());sheep.setYHeadRot(p.getYHeadRot());sheep.yBodyRot=p.yBodyRot;
                p.setHealth(sheep.getHealth());p.setSprinting(false);p.removeAllEffects();p.setInvisible(true);
                form.counters.keySet().forEach(k->game.items().mana().score(p,k).set(0));
            }else original.setPos(sheep.position());
            if(--form.remaining<=0)finish(form,false);
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
            LivingEntity nearest=null;double distance=80*80;
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,from).inflate(80),this::eligible)){
                double d=entity.getEyePosition().distanceToSqr(from);if(d<distance){distance=d;nearest=entity;}
            }
            if(nearest!=null)orb.motion=steer(orb.motion,nearest.getEyePosition().subtract(from));
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
    }
    private void retire(Orb orb){orbs.remove(orb);if(!orb.display.isRemoved())orb.display.discard();}
    private static void load(Entity entity,String nbt){try{entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),TagParser.parseCompoundFully(nbt)));}catch(Exception e){throw new IllegalStateException(e);}}
    public void loaded(Entity e){if((e.entityTags().contains(BODY)||e.entityTags().contains(ORB))&&!publishing.contains(e)&&!bodies.containsKey(e)&&orbs.stream().noneMatch(o->o.display==e))e.discard();}
    public void unloaded(Entity e){for(var form:List.copyOf(forms.values()))if(form.sheep==e||form.original==e)finish(form,false);for(var orb:List.copyOf(orbs))if(orb.display==e)retire(orb);}
    public void forget(ServerPlayer p){var form=forms.get(p);if(form!=null)finish(form,false);}
    public void stop(){for(var form:List.copyOf(forms.values()))finish(form,false);for(var orb:List.copyOf(orbs))retire(orb);volleys.clear();}
    List<LbcSheep.SheepView> sheepViews(){return forms.values().stream().map(f->new LbcSheep.SheepView(f.sheep.getUUID(),f.cause==null?null:f.cause.player(),f.sheep.position())).toList();}
    void forgetAuthor(UUID author){for(var f:List.copyOf(forms.values()))if(f.cause!=null&&author.equals(f.cause.player()))finish(f,false);}
    int count(){return forms.size();}int remaining(LivingEntity entity){var form=forms.get(entity);return form==null?0:form.remaining;}
    long emittedTotal(){return emitted;}
    List<Orb> orbs(){return List.copyOf(orbs);}int volleyCount(){return volleys.size();}
}
