package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.*;

/** Native Academy producer checks; expectations are transcribed from the legacy helpers. */
public final class SauvojenAcademyChecks {
    private SauvojenAcademyChecks() { }
    private static final int[] CADENCES={10,1,5,10,5,5,40,5,5};
    private static final int[] MODES={10,10,6,13,10,5,9,8,9};
    private static final String[][] MODELS={
            {"st/ruby","consuming_wand","dublicating_wand"},
            {"st/terra","terra_razorpine","terrasword_true","fevgalea_star"},
            {"st/electro","erecto","electroblocker"},
            {"st/air","exasperato","homing"},
            {"st/acid","acidacidwand","acidcorrupt"},
            {"st/teleport","boltbolt_wand","infbolt"},
            {"st/stone","stonekivi","bedrock_wand"},
            {"st/fire","blazefire","the_sun"},
            {"st/blind","psychoblind","blindvex"}};
    private static void require(boolean condition,String detail) {
        if(!condition)throw new IllegalStateException("Sauvojen Academy: "+detail);
    }
    private static void near(Vec3 actual,Vec3 expected,String detail) {
        require(actual.distanceToSqr(expected)<1e-9,detail+" actual="+actual+" expected="+expected);
    }
    private static Object state(Object value,String field) {
        try{var f=value.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(value);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Native payload state "+field,e);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ElementalGrimoireChecks.Fixture base;
        final LbcSauvojen service;
        final List<Cow> cows=new ArrayList<>();
        final Map<AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> extraStats=new LinkedHashMap<>();
        final Map<net.minecraft.server.level.ServerPlayer,GameChecks.WandThirdPlayerState> peers=new LinkedHashMap<>();
        final Map<net.minecraft.server.level.ServerPlayer,Map<java.lang.reflect.Field,Object>> combat=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Map<net.minecraft.server.level.ServerPlayer,Object>> attribution=new LinkedHashMap<>();
        final Map<Object,Integer> fallGrace=new IdentityHashMap<>();
        final Map<net.minecraft.server.level.ServerPlayer,Map<String,Integer>> customScores=new LinkedHashMap<>();
        final int cost,stat;
        boolean retain;
        Fixture(GameController game){this(game,false);}
        Fixture(GameController game,boolean probes) {
            this.game=game;service=game.items().sauvojen();
            require(probes||service.count()==0,"exclusive empty drone service");
            base=new ElementalGrimoireChecks.Fixture(game,probes);
            var participants=game.server().getPlayerList().getPlayers().stream().filter(game::canUseGameplayItems).toList();
            try {
                for(var player:participants) {
                    if(player!=base.owner)peers.put(player,new GameChecks.WandThirdPlayerState(player,base.mana));
                    var scores=new LinkedHashMap<String,Integer>();customScores.put(player,scores);
                    for(String name:List.of("ray","acid_rifle","scared","lookback","twitching","piss","delayed_death"))scores.put(name,base.mana.score(player,name).get());
                    var fields=new LinkedHashMap<java.lang.reflect.Field,Object>();combat.put(player,fields);
                    for(String name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp")) {
                        var field=net.minecraft.world.entity.LivingEntity.class.getDeclaredField(name);field.setAccessible(true);fields.put(field,field.get(player));
                    }
                }
                for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);var map=(Map<?,?>)field.get(game.items().indirectKills());
                    var entries=new LinkedHashMap<net.minecraft.server.level.ServerPlayer,Object>();attribution.put(field,entries);
                    for(var player:participants) {
                        Object value=map.get(player);if(value instanceof Map<?,?> nested)value=new LinkedHashMap<>(nested);
                        entries.put(player,value);
                        if(value!=null&&value.getClass().getSimpleName().equals("Fall"))fallGrace.put(value,(Integer)state(value,"grace"));
                    }
                }
            } catch(ReflectiveOperationException error){throw new IllegalStateException("Academy exact participant snapshot",error);}
            cost=base.mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math").get();
            stat=base.owner.getStats().getValue(Stats.ITEM_USED.get(LbcWands.SAUVOJEN));
            for(var s:List.of(Stats.ITEM_USED.get(LbcWands.ACADEMY_SAUVOJEN),Stats.CUSTOM.get(Stats.MOB_KILLS),Stats.CUSTOM.get(Stats.DAMAGE_DEALT),Stats.CUSTOM.get(Stats.DAMAGE_TAKEN),Stats.ENTITY_KILLED.get(EntityTypes.COW)))extraStats.put(s,base.owner.getStats().getValue(s));
            for(var a:game.server().getAdvancements().getAllAdvancements()) {
                var done=new HashSet<String>();base.owner.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);
                advancements.put(a,done);
            }
            base.initialize();base.owner.removeAllEffects();base.mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math").set(2500);
        }
        ItemStack held(InteractionHand hand) {
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var loot=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/sauvojen_wand");
            require(loot.size()==1&&loot.getFirst().is(LbcWands.SAUVOJEN),"ordinary acquisition remains singleton native staff");
            var stack=loot.getFirst();var data=LbcFartus.data(stack);data.putInt("upgraded",1);
            data.remove("upgradable");data.remove("upgrade_available");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            base.owner.setItemInHand(hand,stack);return stack;
        }
        LbcSauvojen.Drone launch(LbcSauvojen.Kind kind) {
            int before=service.count();
            service.cast(base.owner,InteractionHand.MAIN_HAND,held(InteractionHand.MAIN_HAND),kind.ordinal());
            require(service.count()==before+1,"one Academy drone "+kind);
            var drone=service.snapshot().getLast();require(drone.academy&&drone.nextMode==0,"Academy identity starts first source mode "+kind);return drone;
        }
        Cow cow(Vec3 position) {
            var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(position);cow.setNoGravity(true);cow.setNoAi(true);
            require(base.owner.level().addFreshEntity(cow),"native target cow");cows.add(cow);return cow;
        }
        void removeCows(){
            for(var cow:cows){game.items().indirectKills().clear(cow);cow.discard();}cows.clear();
            var drops=new ArrayList<Entity>();for(var e:base.owner.level().getAllEntities())if(!base.before.contains(e)
                    &&(e instanceof net.minecraft.world.entity.item.ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))drops.add(e);
            drops.forEach(Entity::discard);
        }
        void clear(){
            service.stop();clearPayloads();
        }
        void clearPayloads(){
            base.clear();game.items().homing().stop();game.items().psycho().stop();
            game.items().bolts().stop();game.items().exasperato().stop();game.items().consuming().stop();
            game.items().duplicating().stop();game.items().blaze().stop();game.items().stonekivi().stop();
            game.items().acidCorruption().stop();game.items().awakenedAcid().stop();
            game.items().erecto().stop();game.items().summons().stop();service.stars().stop();
            var lightning=new ArrayList<Entity>();for(var e:base.owner.level().getAllEntities())if(!base.before.contains(e)&&e instanceof net.minecraft.world.entity.LightningBolt bolt&&LbcElectro.managedLightning(bolt)&&bolt.getCause()==base.owner)lightning.add(e);
            lightning.forEach(Entity::discard);
        }
        Object reboundStatus(Object value,net.minecraft.server.level.ServerPlayer player) {
            if(value==null||!value.getClass().getSimpleName().equals("Status"))return value;
            var prior=(net.minecraft.world.effect.MobEffectInstance)state(value,"effect");var active=player.getEffect(prior.getEffect());
            if(active==null)return value;
            try{var constructor=value.getClass().getDeclaredConstructors()[0];constructor.setAccessible(true);return constructor.newInstance(state(value,"owner"),active);}
            catch(ReflectiveOperationException error){throw new IllegalStateException("Academy exact active-effect attribution restore",error);}
        }
        @SuppressWarnings("unchecked")
        void restoreAttribution() {
            try {
                for(var field:attribution.entrySet()) {
                    var map=(Map<Object,Object>)field.getKey().get(game.items().indirectKills());
                    for(var entry:field.getValue().entrySet()) {
                        Object value=entry.getValue();var player=entry.getKey();
                        if(value instanceof Map<?,?> nested) {
                            var effects=new LinkedHashMap<Object,Object>();nested.forEach((effect,status)->effects.put(effect,reboundStatus(status,player)));value=effects;
                        } else value=reboundStatus(value,player);
                        if(value==null)map.remove(player);else map.put(player,value);
                    }
                }
                for(var entry:fallGrace.entrySet()){var field=entry.getKey().getClass().getDeclaredField("grace");field.setAccessible(true);field.setInt(entry.getKey(),entry.getValue());}
                for(var player:combat.entrySet())for(var field:player.getValue().entrySet())field.getKey().set(player.getKey(),field.getValue());
            } catch(ReflectiveOperationException error){throw new IllegalStateException("Academy exact participant attribution restore",error);}
        }
        void projectile(Projectile shot,Vec3 origin,Vec3 direction,double speed,String detail) {
            require(shot.getOwner()==base.owner&&game.items().currentEpoch(shot),"actual owner/epoch "+detail);
            near(shot.position(),origin,"detached launch position "+detail);
            near(shot.getDeltaMovement(),direction.scale(speed),"detached velocity "+detail);
        }
        void emit(ItemStack stack,LbcSauvojen.Kind kind,int mode,Vec3 origin,Vec3 direction) {
            var owner=base.owner;var position=owner.position();var velocity=owner.getDeltaMovement();
            float yaw=owner.getYRot(),pitch=owner.getXRot();var abilities=owner.getAbilities().pack();
            // Imported flag combinations must not let a detached donor producer ready or mutate its caller's stack.
            var trialData=LbcFartus.data(stack);trialData.putInt("upgradable",1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(trialData));
            var copy=stack.copy();int balance=base.mana.get(owner),maximum=base.mana.maximum(owner),packets=base.recording.packets.size();
            int used=owner.getStats().getValue(Stats.ITEM_USED.get(LbcWands.SAUVOJEN));
            service.emitAcademy(owner,InteractionHand.OFF_HAND,stack,kind,mode,origin,direction);
            near(owner.position(),position,"detached helper preserves owner position "+kind+"/"+mode);
            near(owner.getDeltaMovement(),velocity,"detached helper preserves owner velocity "+kind+"/"+mode);
            require(owner.getYRot()==yaw&&owner.getXRot()==pitch&&owner.getAbilities().pack().equals(abilities),"detached helper preserves camera and native abilities "+kind+"/"+mode);
            require(base.mana.get(owner)==balance&&base.mana.maximum(owner)==maximum&&owner.getStats().getValue(Stats.ITEM_USED.get(LbcWands.SAUVOJEN))==used,"detached helper has no paid admission/debit/use stat/Mage callback "+kind+"/"+mode);
            require(ItemStack.isSameItemSameComponents(stack,copy)&&stack.getCount()==copy.getCount(),"detached helper never changes owner stack/mode/trial/readiness "+kind+"/"+mode);
            require(base.recording.packets.subList(packets,base.recording.packets.size()).stream().noneMatch(p->p.getClass().getSimpleName().equals("ClientboundPlayerPositionPacket")||p.getClass().getSimpleName().equals("ClientboundPlayerAbilitiesPacket")),"no transient owner movement or abilities spoof packets "+kind+"/"+mode);
        }
        void drainRays() {
            for(int i=0;i<48&&game.items().exasperato().pendingSearches()>0;i++)game.items().exasperato().tick();
            require(game.items().exasperato().pendingSearches()==0&&game.items().homing().pendingRayCount()==0,"native detached ray completes in resident fixture");
        }
        void detachedAcidAndAir() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.3,.2,.9).normalize();
            Vec3 position=owner.position(),velocity=owner.getDeltaMovement();float yaw=owner.getYRot(),pitch=owner.getXRot();
            boolean flying=owner.getAbilities().flying,mayfly=owner.getAbilities().mayfly,invulnerable=owner.getAbilities().invulnerable;
            float walking=owner.getAbilities().getWalkingSpeed(),flight=owner.getAbilities().getFlyingSpeed();
            int maximum=base.mana.maximum(owner),rifle=game.items().awakenedAcid().pending(owner);
            base.mana.set(owner,4321);
            for(int mode=1;mode<=5;mode++) {
                emit(stack,LbcSauvojen.Kind.ACID,mode,origin,direction);
                int count=mode==4?8:1,variant=mode==2?1:mode>=4?2:0;
                var shots=game.items().clouds().acidSnapshot();require(shots.size()==count,"source detached acid count mode="+mode);
                for(var shot:shots) {
                    require(shot.getOwner()==owner&&game.items().currentEpoch(shot)&&shot.isNoGravity(),"native detached acid author/epoch/gravity mode="+mode);
                    near(shot.position(),origin,"actual detached acid origin mode="+mode);
                    require(LbcClouds.awakenedVariant(shot)==variant&&LbcClouds.acidLifetime(shot)==(variant==2?200:500),"source acid kind/lifetime mode="+mode);
                    require(!shot.entityTags().contains("on_fire"),"Academy drone does not infer ignited donor toggle mode="+mode);
                    if(mode!=4)near(shot.getDeltaMovement(),direction.scale(variant==2?3:.2),"actual acid direction/speed mode="+mode);
                    else require(Math.abs(shot.getDeltaMovement().length()-3)<1e-9&&shot.getDeltaMovement().normalize().dot(owner.getLookAngle())>.79,"source gatling helper explicitly uses owner camera for cone while retaining drone origin");
                }
                require(game.items().awakenedAcid().pending(owner)==rifle,"one source rifle release never creates a paid ten-shot queue");
                require(base.mana.get(owner)==4321&&base.mana.maximum(owner)==maximum&&stack.getCount()==1,"detached acid has no second debit/bonus/consumption");
                near(owner.position(),position,"acid preserves owner position");near(owner.getDeltaMovement(),velocity,"acid preserves owner velocity");
                require(owner.getYRot()==yaw&&owner.getXRot()==pitch&&owner.getAbilities().flying==flying&&owner.getAbilities().mayfly==mayfly&&owner.getAbilities().invulnerable==invulnerable
                        &&owner.getAbilities().getWalkingSpeed()==walking&&owner.getAbilities().getFlyingSpeed()==flight,"acid never spoofs owner camera/abilities");
                clear();
            }
            for(int mode=8;mode<=10;mode++) {
                emit(stack,LbcSauvojen.Kind.AIR,mode,origin,direction);
                var shots=game.items().exasperato().snapshot();int[] expected=mode==9?new int[]{200,100,50}:new int[]{200};
                require(shots.size()==expected.length,"source exasperato helper count mode="+mode);
                for(int i=0;i<shots.size();i++) {
                    var shot=shots.get(i);require(shot.mode==mode-8&&shot.lifetime==expected[i],"exact exasperato helper mode/lifetime "+mode+"/"+i);
                    projectile(shot,origin,mode==9?owner.getLookAngle():direction,mode==10?4.5:1,"exasperato "+mode+"/"+i);
                }
                require(base.mana.get(owner)==4321&&base.mana.maximum(owner)==maximum&&stack.getCount()==1,"detached exasperato no second debit/bonus/consumption");
                near(owner.position(),position,"exasperato preserves owner position");near(owner.getDeltaMovement(),velocity,"exasperato preserves owner velocity");
                require(owner.getYRot()==yaw&&owner.getXRot()==pitch,"exasperato preserves owner camera");clear();
            }
        }
        void detachedHomingAndBolts() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.6,0,.8);
            var wall=BlockPos.containing(origin.add(direction.scale(6)));var saved=game.maps().snapshot(owner.level(),wall);
            require(owner.level().hasChunkAt(wall)&&owner.level().getBlockState(wall).isAir(),"resident untouched detached ray wall");
            try {
                owner.level().setBlock(wall,Blocks.OBSIDIAN.defaultBlockState(),2|16|32);
                for(int mode=4;mode<=6;mode++) {
                    emit(stack,LbcSauvojen.Kind.AIR,mode,origin,direction);drainRays();
                    if(mode<6) {
                        var shots=game.items().homing().snapshot();require(shots.size()==(mode==4?10:1),"source homing fireball count "+mode);
                        for(var shot:shots)require(shot.getOwner()==owner&&game.items().currentEpoch(shot),"actual detached Homing owner/epoch "+mode);
                        require(base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                                .anyMatch(p->new Vec3(p.x(),p.y(),p.z()).distanceToSqr(origin)<1e-9),"actual detached Homing ray starts at drone "+mode);
                    } else {
                        require(game.items().homing().destinyCount()==1,"one detached Destiny center");
                        var destiny=game.items().homing().destinySnapshot().getFirst();
                        require(destiny.owner()==owner&&destiny.remaining()==600&&game.items().currentEpoch(destiny.epoch()),"actual Destiny author/600tick duration/epoch");
                        near(destiny.position(),origin,"Destiny retains drone center");
                    }
                    clear();
                }
                for(int mode=2;mode<=4;mode++) {
                    emit(stack,LbcSauvojen.Kind.TELEPORT,mode,origin,direction);
                    if(mode==2) {
                        var bolts=game.items().bolts().snapshot();require(bolts.size()==5,"detached Boltbolt terminal has five wandering bolts");
                        for(var bolt:bolts)require(bolt.owner==owner&&bolt.lifetime==200&&bolt.strikeInterval==40&&game.items().currentEpoch(bolt.epoch),"actual Boltbolt author/lifetime/interval/epoch");
                    } else if(mode==3) {
                        var meteors=game.items().bolts().meteorSnapshot();require(meteors.size()==1,"one actual detached meteor");
                        var meteor=meteors.getFirst();require(meteor.owner==owner&&!meteor.display.isRemoved()&&meteor.position.y>=origin.y+49,"meteor is native owned display fifty blocks above drone ray contact");
                    } else {
                        var waters=game.items().bolts().waterSnapshot();require(waters.size()==5,"five detached watering centers");
                        var expected=new HashSet<Vec3>(List.of(origin,origin.add(5,0,0),origin.add(-5,0,0),origin.add(0,0,5),origin.add(0,0,-5)));
                        for(var water:waters)require(water.owner==owner&&game.items().currentEpoch(water.epoch)&&expected.remove(water.position),"watering exact owner/epoch/drone±5 geometry");
                        require(expected.isEmpty()&&!owner.hasEffect(MobEffects.WATER_BREATHING)&&!owner.hasEffect(MobEffects.STRENGTH),"detached watering omits donor paid owner buffs");
                    }
                    clear();
                }
            } finally{clear();game.maps().restoreSnapshot(owner.level(),wall,saved);}
        }
        void detachedConsuming() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.6,0,.8);
            var wall=BlockPos.containing(origin.add(direction.scale(6)));var saved=game.maps().snapshot(owner.level(),wall);
            try {
                owner.level().setBlock(wall,Blocks.OBSIDIAN.defaultBlockState(),2|16|32);
                for(int mode=7;mode<=9;mode++) {
                    emit(stack,LbcSauvojen.Kind.RUBY,mode,origin,direction);
                    require(game.items().consuming().pendingWorkCount()>0,"real consuming terrain/displacement work submitted mode="+mode);
                    if(mode==9) {
                        var erasures=game.items().consuming().silenceSnapshot();require(erasures.size()==1,"single native Silence erasure guard");
                        var erasure=erasures.getFirst();require(erasure.owner()==owner&&game.items().currentEpoch(erasure.epoch())&&erasure.pending()==2&&erasure.settle()==40,"Silence source cause and gravity settling guard");
                        near(erasure.center(),origin,"Silence erases around drone rather than player");
                        var levitation=owner.getEffect(MobEffects.LEVITATION);var falling=owner.getEffect(MobEffects.SLOW_FALLING);
                        require(levitation!=null&&levitation.getAmplifier()==30&&levitation.getDuration()==60&&falling!=null&&falling.getDuration()==1200,"explicit upgraded Ruby9 source owner effects");
                    } else {
                        var particles=base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).toList();
                        require(!particles.isEmpty(),"actual consuming ray particles mode="+mode);
                        near(new Vec3(particles.getFirst().x(),particles.getFirst().y(),particles.getFirst().z()),origin,"consuming ray starts exactly at drone "+mode);
                        Vec3 ray=mode==8?Vec3.directionFromRotation((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())),(float)Math.toDegrees(Math.atan2(-direction.x,direction.z))):direction;
                        for(int i=0;i<particles.size();i++) {
                            var p=particles.get(i);
                            near(new Vec3(p.x(),p.y(),p.z()),origin.add(ray.scale(i*(mode==8?2:.7))),"one exact detached source ray sample, no donor five-ray fan "+mode+"/"+i);
                        }
                    }
                    clear();owner.removeEffect(MobEffects.LEVITATION);owner.removeEffect(MobEffects.SLOW_FALLING);
                }
            } finally{clear();game.maps().restoreSnapshot(owner.level(),wall,saved);owner.removeEffect(MobEffects.LEVITATION);owner.removeEffect(MobEffects.SLOW_FALLING);}
        }
        void firstSourceModes() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.3,.2,.9).normalize();
            for(var kind:LbcSauvojen.Kind.values()) {
                emit(stack,kind,0,origin,direction);
                if(kind==LbcSauvojen.Kind.ELECTRO) {
                    var arcs=game.items().wands().electroSnapshot();int[] lifetimes={40,45,50};require(arcs.size()==3,"Academy Electro helper has three source arcs");
                    for(int i=0;i<3;i++) {
                        var arc=arcs.get(i);require(arc.owner==owner&&arc.lifetime==lifetimes[i]&&arc.trialOwner==null&&!arc.statusWild,"first Academy Electro exact source lifetime/owner/no trial "+i);
                        near(arc.position,origin.add(direction.scale(-i*.01)),"actual Academy Electro local negative forward offsets");
                        require(Vec3.directionFromRotation(arc.pitch,arc.yaw).distanceToSqr(direction)<1e-8,"actual Academy Electro arc aim");
                    }
                } else if(kind==LbcSauvojen.Kind.BLIND) {
                    var charges=game.items().blind().snapshot();require(charges.size()==5,"source blind helper creates five charges");
                    for(var charge:charges)require(charge.getOwner()==owner&&game.items().currentEpoch(charge)&&charge.lifetime==30&&!charge.trial&&!charge.phantom&&charge.position().distanceToSqr(origin)<=.010000001,"actual blind source author/epoch/30t/lack trial/local offsets");
                } else if(kind==LbcSauvojen.Kind.FIRE) {
                    var shots=base.fire();require(shots.size()==3,"source fire_preloop makes three native small fireballs");
                    Vec3 left=new Vec3(direction.z,0,-direction.x).normalize();
                    var velocities=new HashSet<Vec3>(List.of(direction.scale(2.5),direction.scale(2.5).add(left.scale(.15)),direction.scale(2.5).add(left.scale(-.15))));
                    for(int i=0;i<3;i++) {
                        var shot=shots.get(i);require(shot.getOwner()==owner&&game.items().currentEpoch(shot),"source Fire author/epoch");near(shot.position(),origin.add(direction),"source Fire local forward1 origin");
                        var match=velocities.stream().filter(v->v.distanceToSqr(shot.getDeltaMovement())<1e-9).findFirst();require(match.isPresent(),"source Fire complete local side velocity set "+i);velocities.remove(match.orElseThrow());
                    }
                } else {
                    Projectile shot=switch(kind) {
                        case RUBY->game.items().ruby().snapshot().getFirst();
                        case TERRA->{LbcTerra.TerraProjectile found=null;for(var e:owner.level().getAllEntities())if(e instanceof LbcTerra.TerraProjectile p&&!p.isRemoved())found=p;require(found!=null,"native first Terra projectile");yield found;}
                        case AIR->game.items().air().snapshot().getFirst();case ACID->game.items().clouds().acidSnapshot().getFirst();
                        case TELEPORT->game.items().teleport().snapshot().getFirst();case STONE->game.items().stone().snapshot().getFirst();
                        case FIRE->base.fire().getFirst();case BLIND->game.items().blind().snapshot().getFirst();default->throw new IllegalStateException();
                    };
                    double speed=switch(kind){case AIR,STONE->1;case TELEPORT->1.8;case ACID->.2;case FIRE->2.5;default->1.8;};
                    if(kind==LbcSauvojen.Kind.ACID) {
                        require(shot.getOwner()==owner&&game.items().currentEpoch(shot)&&Math.abs(shot.getDeltaMovement().length()-.2)<1e-7&&shot.getDeltaMovement().normalize().dot(direction)>.79&&shot.entityTags().contains("shield_blockable2"),"source Academy big acid uses shield-blockable .2 initial speed and drone-angle spread");
                        near(shot.position(),origin,"source Academy big acid origin");
                    } else projectile(shot,origin,direction,speed,"first Academy "+kind);
                    require(base.count()==1,"one first source Academy projectile "+kind);
                    if(shot instanceof LbcRuby.Charge p)require(p.sourceLifetime==70,"Academy ruby helper70");
                    if(shot instanceof LbcTerra.TerraProjectile p)require(p.sourceLifetime==200,"Academy ordinary terra200");
                    if(shot instanceof LbcAir.Charge p)require(p.sourceLifetime==200&&!p.trial,"Academy Air helper200 with no donor trial");
                    if(shot instanceof LbcTeleport.Charge p)require(p.sourceLifetime==200,"Academy Teleport helper200");
                    if(shot instanceof LbcStone.Charge p)require(p.lifetime==200,"Academy Stone helper200");
                }
                clear();
            }
        }
        void detachedCorruption() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.6,0,.8);
            var wall=BlockPos.containing(origin.add(direction.scale(6)));var saved=game.maps().snapshot(owner.level(),wall);
            int previousReverse=base.mana.reverse(owner);
            try {
                owner.level().setBlock(wall,Blocks.OBSIDIAN.defaultBlockState(),2|16|32);
                emit(stack,LbcSauvojen.Kind.ACID,6,origin,direction);
                var balls=game.items().acidCorruption().snapshot();require(balls.size()==1,"one native hard acid ball at detached ray contact");
                var ball=balls.getFirst();require(ball.ball&&ball.getOwner()==owner&&game.items().currentEpoch(ball),"hard acid actual owner/epoch/ball mode");
                require(ball.position().distanceToSqr(origin)>16&&ball.position().distanceToSqr(origin.add(direction.scale(6)))<3,"hard acid actual drone ray terminal position");clear();
                emit(stack,LbcSauvojen.Kind.ACID,8,origin,direction);
                require(base.mana.reverse(owner)==previousReverse+30,"source Chuma terminal includes nearby owner with30seconds Reverse");clear();base.mana.reverse(owner,previousReverse);
                var victim=cow(origin.add(direction.scale(3)).add(0,-.5,0));
                emit(stack,LbcSauvojen.Kind.ACID,9,origin,direction);
                require(game.items().acidCorruption().deathRemaining(victim)==120,"source Titul terminal assigns actual120second delayed death to ray target");
                clear();removeCows();
                var board=game.server().getScoreboard();var previousTeam=owner.getTeam();
                require(board.getPlayerTeam("sw2_acad_sacrifice")==null,"exclusive sacrifice team");var team=board.addPlayerTeam("sw2_acad_sacrifice");
                try {
                    var allied=cow(origin.add(1,0,0));allied.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);allied.setHealth(20);
                    board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(allied.getScoreboardName(),team);
                    emit(stack,LbcSauvojen.Kind.ACID,7,origin,direction);
                    require(!allied.isAlive(),"source owner-context Reverse sacrifices its allied native mob");
                    var resistance=owner.getEffect(MobEffects.RESISTANCE);
                    require(resistance!=null&&resistance.getAmplifier()==4&&resistance.getDuration()==40,"twenty allied HP produce exact two-second ResistanceV at owner");
                } finally{board.removePlayerTeam(team);if(previousTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),previousTeam);owner.removeEffect(MobEffects.RESISTANCE);clear();removeCows();}
            } finally{clear();removeCows();base.mana.reverse(owner,previousReverse);game.maps().restoreSnapshot(owner.level(),wall,saved);}
        }
        void detachedPsychicCharges() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.3,.2,.9).normalize();
            for(int mode:new int[]{3,4,5,7}) {
                emit(stack,LbcSauvojen.Kind.BLIND,mode,origin,direction);
                var shots=game.items().psycho().snapshot();int expected=mode==4?4:mode==7?1:3;
                require(shots.size()==expected,"source Psychoblind helper charge count "+mode);
                for(int i=0;i<shots.size();i++) {
                    var shot=shots.get(i);require(shot.getOwner()==owner&&game.items().currentEpoch(shot)&&shot.lifetime==100&&shot.invisible==(mode==5)&&shot.spray==(mode==7),"actual psychic source mode/author/epoch/lifetime "+mode+"/"+i);
                    near(shot.position(),origin.add(direction.scale(mode==7?0:i*.01)),"source psychic local forward offsets "+mode+"/"+i);
                    if(mode!=7) {
                        var motion=shot.getDeltaMovement();float expectedYaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
                        float expectedPitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
                        float actualYaw=(float)Math.toDegrees(Math.atan2(-motion.x,motion.z));float actualPitch=(float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()));
                        // Native sin/cos table indices can differ by one 2*pi/65536 phase bin; two angle pairs bound speed2's norm error by2e-4.
                        require(Math.abs(motion.length()-2)<2e-4&&Math.abs(net.minecraft.util.Mth.wrapDegrees(actualYaw-expectedYaw))<=30.0101f&&Math.abs(actualPitch-expectedPitch)<=20.0101f,"psychic source independently bounded drone yaw30/pitch20 and speed2 with native LUT norm tolerance "+mode+"/"+i+" actual="+motion);
                    }
                }
                require(game.items().psycho().rayCount(owner)==0,"direct psychic charge helper never invents paid timed ray queue");clear();
            }
        }
        void detachedBlazeModes() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.6,0,.8);
            var victim=cow(origin.add(4,-1,4));
            try {
                for(int mode=3;mode<=7;mode++) {
                    final int branch=mode;
                    emit(stack,LbcSauvojen.Kind.FIRE,mode,origin,direction);
                    if(mode==4||mode==6) {
                        require(game.items().blaze().terrainCount()==1,"actual queued Fireblaze terrain producer "+mode);
                        var queue=(Collection<?>)state(game.items().blaze(),"terrain");var terrain=queue.iterator().next();
                        require(state(terrain,"owner")==owner,"Fireblaze terrain preserves actual author "+mode);
                        var areas=(Collection<?>)state(terrain,"areas");require(!areas.isEmpty(),"Fireblaze source queues real block work "+mode);
                        var center=victim.blockPosition();
                        if(mode==4) {
                            // Test cow is the only nearby high-altitude target; global source may also include real match actors.
                            require(areas.stream().map(a->(BlockAreaWork.Bounds)state(a,"bounds")).anyMatch(b->b.equals(new BlockAreaWork.Bounds(center.getX()-1,center.getY(),center.getZ()-1,center.getX()+1,center.getY()+1,center.getZ()+1))),"Kiuas real native queue contains exact source target3x2x3 footprint");
                            require(areas.stream().map(a->(BlockAreaWork.Bounds)state(a,"bounds")).anyMatch(b->b.equals(new BlockAreaWork.Bounds(center.getX()-15,center.getY()-15,center.getZ()-15,center.getX()+15,center.getY()+15,center.getZ()+15))),"Kiuas real queue contains exact source31cube");
                        } else require(areas.stream().map(a->(BlockAreaWork.Bounds)state(a,"bounds")).anyMatch(b->b.minY()==0&&b.maxY()==319&&b.minX()==b.maxX()&&b.minZ()==b.maxZ()&&Math.abs(b.minX()-center.getX())<=30&&Math.abs(b.minZ()-center.getZ())<=30),"Pillars real native queue contains random target±30 vertical0..319 lava column");
                        require(!owner.hasEffect(MobEffects.FIRE_RESISTANCE),"global detached Fireblaze does not award paid owner resistance");
                    } else {
                        var shots=new ArrayList<Projectile>();for(var e:owner.level().getAllEntities())if(!base.before.contains(e)&&!e.isRemoved()&&e instanceof Projectile p&&p.getOwner()==owner)shots.add(p);
                        require(!shots.isEmpty(),"actual Fireblaze native fireball payload "+mode);
                        if(mode==3) {
                            require(shots.size()==1&&shots.getFirst().getType()==EntityTypes.SMALL_FIREBALL,"direct at_random_fireball is one small fireball");
                            var shot=shots.getFirst();var offset=shot.position().subtract(origin);
                            boolean diagonal=Math.abs(offset.x)==4&&offset.y==0&&Math.abs(offset.z)==4;
                            boolean cardinal=offset.lengthSqr()==36&&(offset.x==0&&offset.z==0||offset.y==0&&(offset.x==0||offset.z==0));
                            require(diagonal||cardinal,"random direct Fireblaze offset belongs to exact nine-source set");
                            near(shot.getDeltaMovement(),offset.scale(diagonal?-.5:-.25),"random direct Fireblaze velocity points to drone center");
                        } else require(shots.stream().allMatch(p->p.getType()==(branch==5?EntityTypes.FIREBALL:EntityTypes.SMALL_FIREBALL)),"mega/world native fireball types "+mode);
                        for(var shot:shots)require(game.items().currentEpoch(shot),"Fireblaze native epoch "+mode);
                    }
                    clear();
                    var shots=new ArrayList<Entity>();for(var e:owner.level().getAllEntities())if(!base.before.contains(e)&&e instanceof Projectile p&&p.getOwner()==owner)shots.add(e);shots.forEach(Entity::discard);
                }
            } finally{clear();removeCows();}
        }
        void detachedElementalVariants() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.3,.2,.9).normalize();
            int[] terraLifetime={0,20,20,10,50};
            var terraKind=new LbcTerra.Kind[]{LbcTerra.Kind.REGULAR,LbcTerra.Kind.ACADEMY,LbcTerra.Kind.TRUE_CHARGE,LbcTerra.Kind.RAZOR,LbcTerra.Kind.TRUE_BEAM};
            for(int mode=1;mode<=4;mode++) {
                emit(stack,LbcSauvojen.Kind.TERRA,mode,origin,direction);
                var shots=new ArrayList<LbcTerra.TerraProjectile>();for(var e:owner.level().getAllEntities())if(e instanceof LbcTerra.TerraProjectile p&&!p.isRemoved())shots.add(p);
                require(shots.size()==1,"one actual detached Terra variant "+mode);var shot=shots.getFirst();
                require(state(shot,"kind")==terraKind[mode]&&shot.sourceLifetime==terraLifetime[mode],"exact Terra source producer kind/lifetime "+mode);
                require(shot.getOwner()==owner&&game.items().currentEpoch(shot),"Terra author/epoch "+mode);near(shot.position(),origin,"Terra source variant launch point "+mode);
                if(mode==3) {
                    float expectedYaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
                    float expectedPitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
                    require(Math.abs(net.minecraft.util.Mth.wrapDegrees(shot.getYRot()-expectedYaw))<=3.0101f
                            &&Math.abs(shot.getXRot()-expectedPitch)<=3.0101f,"Razor source independently randomizes drone yaw and pitch by at most three degrees");
                    require(Math.abs(shot.getDeltaMovement().length()-7.5)<2e-6,"Razor source speed7.5 within native float sine/cosine norm error, actual="+shot.getDeltaMovement().length());
                } else near(shot.getDeltaMovement(),direction.scale(mode==4?1.32:7.5),"Terra source variant exact velocity "+mode);clear();
            }
            for(int mode=1;mode<=3;mode++) {
                emit(stack,LbcSauvojen.Kind.AIR,mode,origin,direction);
                var shots=game.items().air().snapshot();require(shots.size()==(mode==1?4:1),"exact source Air helper count "+mode);
                for(var shot:shots) {
                    require(shot.mode==mode&&shot.sourceLifetime==200&&!shot.trial&&shot.getOwner()==owner&&game.items().currentEpoch(shot),"Air source mode/lifetime/author/epoch/no trial "+mode);
                    near(shot.position(),origin,"Air source detached origin "+mode);
                    if(mode==1)require(Math.abs(shot.getDeltaMovement().length()-1)<1e-8&&shot.getDeltaMovement().normalize().dot(owner.getLookAngle())>.79,"Air burst source explicitly spreads around owner camera");
                    else near(shot.getDeltaMovement(),direction.scale(mode==2?6.3:1.6),"Air source detached velocity "+mode);
                }
                clear();
            }
            emit(stack,LbcSauvojen.Kind.TELEPORT,1,origin,direction);
            var teleports=game.items().teleport().snapshot();require(teleports.size()==1&&teleports.getFirst().mode==2&&teleports.getFirst().sourceLifetime==200,"one source inverted Teleport200");
            projectile(teleports.getFirst(),origin,direction,3,"inverted Teleport");clear();
            emit(stack,LbcSauvojen.Kind.BLIND,1,origin,direction);
            var phantoms=game.items().blind().snapshot();require(phantoms.size()==4,"four source blind phantoms");
            for(var shot:phantoms)require(shot.getOwner()==owner&&game.items().currentEpoch(shot)&&shot.phantom&&!shot.trial&&shot.lifetime==30&&shot.position().distanceToSqr(origin)<=.010000001&&shot.getDeltaMovement().normalize().dot(owner.getLookAngle())>.79,"phantom source author/epoch/local offsets/owner-angle spread/no trial");clear();
        }
        void detachedStars() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.6,0,.8);
            emit(stack,LbcSauvojen.Kind.TERRA,5,origin,new Vec3(0,1,0));
            var beam=base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                    .filter(p->p.particle().getType()==net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK).toList();
            require(beam.size()==121,"direct Fevgalea source beam has121 samples including both endpoints");
            for(int i=0;i<121;i++) {
                var p=beam.get(i);near(new Vec3(p.x(),p.y(),p.z()),origin.add(0,i,0),"Fevgalea exact detached beam sample "+i);
                require(p.count()==10&&p.xDist()==1.5f&&p.yDist()==1.5f&&p.zDist()==1.5f,"Fevgalea source beam count/spread "+i);
            }
            require(service.stars().count()==0,"instant Fevgalea beam does not create a timed carrier");clear();
            for(int mode=6;mode<=9;mode++) {
                emit(stack,LbcSauvojen.Kind.TERRA,mode,origin,direction);
                var actors=service.stars().snapshot();require(actors.size()==1,"one actual Fevgalea source actor mode="+mode);
                var actor=actors.getFirst();require(actor.owner()==owner&&game.items().currentEpoch(actor.epoch())&&game.items().currentEpoch(actor.carrier())&&actor.age()==0,"Fevgalea actual author/epoch/new age "+mode);
                require(actor.kind()==(mode==6?SauvojenStars.Kind.RAIN:SauvojenStars.Kind.CHARGE)&&actor.lifetime()==(mode==6?100:40),"Fevgalea source kind/lifetime "+mode);
                require(mode==6?actor.flavour()==0:actor.flavour()>=1&&actor.flavour()<=3,"Fevgalea random charge flavour set "+mode);
                near(actor.position(),mode==6?origin.add(0,15,0):origin,"Fevgalea local-up15 rain versus charge origin "+mode);
                near(actor.direction(),direction,"Fevgalea actual captured drone direction "+mode);
                service.stars().tick();
                var next=service.stars().snapshot().stream().filter(a->a.carrier()==actor.carrier()).findFirst().orElseThrow();
                near(next.position(),actor.position().add(direction.scale(mode==6?1:1.8)),"Fevgalea actual native first active-tick movement "+mode);
                require(next.age()==1,"Fevgalea actual active age after native tick");clear();
            }
        }
        void detachedRockAndRubyRays() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(0,0,1);
            var center=BlockPos.containing(origin.add(0,0,6));
            var saved=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            for(var p:BlockPos.betweenClosed(BlockPos.containing(origin.add(-8,-8,-8)),BlockPos.containing(origin.add(8,12,68)))) {
                require(owner.level().hasChunkAt(p),"resident ray/bridge impact fixture");saved.put(p.immutable(),game.maps().snapshot(owner.level(),p));
            }
            Runnable restore=()->saved.forEach((p,s)->game.maps().restoreSnapshot(owner.level(),p,s));
            try {
                for(int mode=1;mode<=2;mode++) {
                    owner.level().setBlock(center,Blocks.STONE.defaultBlockState(),2|16|32);
                    emit(stack,LbcSauvojen.Kind.RUBY,mode,origin,direction);
                    require(mode==1?owner.level().getBlockState(center).is(Blocks.BEDROCK):owner.level().getBlockState(center).isAir(),"actual detached Ruby source hole/cutting ray terminal "+mode);
                    require(base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).anyMatch(p->new Vec3(p.x(),p.y(),p.z()).distanceToSqr(origin)<1e-9),"Ruby actual detached ray starts at drone "+mode);
                    clear();restore.run();
                }
                for(int mode=1;mode<=4;mode++) {
                    if(mode==1||mode==4)owner.level().setBlock(center,Blocks.STONE.defaultBlockState(),2|16|32);
                    emit(stack,LbcSauvojen.Kind.STONE,mode,origin,direction);
                    if(mode==3) {
                        var effect=owner.getEffect(MobEffects.RESISTANCE);require(effect!=null&&effect.getAmplifier()==4&&effect.getDuration()==100,"source shield explicitly resolves at owner for100ticks ResistanceV");owner.removeEffect(MobEffects.RESISTANCE);
                    } else if(mode==2) {
                        require(owner.level().getBlockState(BlockPos.containing(origin)).is(Blocks.CRYING_OBSIDIAN),"source bridge starts at drone exact Y without donor feet-1 adjustment");
                        require(owner.level().getBlockState(BlockPos.containing(origin.add(0,0,30))).is(Blocks.DIORITE),"source bridge retains entire fifteen-material length");
                    } else {
                        long changed=saved.entrySet().stream().filter(e->!owner.level().getBlockState(e.getKey()).equals(e.getValue().state())).count();
                        require(changed>8,"source rock/spike actual native template placement "+mode);
                        require(base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).anyMatch(p->new Vec3(p.x(),p.y(),p.z()).distanceToSqr(origin)<1e-9),"Stone actual detached ray starts at drone "+mode);
                    }
                    clear();restore.run();
                }
                var drone=launch(LbcSauvojen.Kind.RUBY);var victim=cow(origin.add(0,-.5,6));
                victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);
                for(int mode=0;mode<10;mode++) {
                    drone.position=origin;drone.carrier.setPos(origin);drone.lifetime=10;
                    require(drone.nextMode==mode,"approved Ruby cycle reaches each complete source mode in order "+mode);
                    service.tick();require(drone.nextMode==(mode+1)%10,"approved Ruby advances exactly once through all ten source modes "+mode);
                    require(mode==9?service.count()==0&&drone.carrier.isRemoved():service.count()==1&&!drone.carrier.isRemoved(),"source Ruby Silence intentionally consumes its own drone only on final mode "+mode);
                    clearPayloads();restore.run();
                }
                clear();removeCows();owner.removeEffect(MobEffects.LEVITATION);owner.removeEffect(MobEffects.SLOW_FALLING);
            } finally{clear();restore.run();}
        }
        void detachedDuplicating() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(.6,0,.8);
            for(int mode=3;mode<=6;mode++) {
                emit(stack,LbcSauvojen.Kind.RUBY,mode,origin,direction);
                if(mode==5) {
                    var jobs=(Collection<?>)state(game.items().duplicating(),"jobs");require(jobs.size()==1,"one actual Divide DDA work job");
                    var job=jobs.iterator().next();near((Vec3)state(job,"start"),origin,"Divide native DDA starts at drone");near((Vec3)state(job,"direction"),direction,"Divide native DDA captures drone direction");
                } else {
                    var markers=game.items().duplicating().markerSnapshot();require(markers.size()==1,"one actual duplicating moving center "+mode);
                    var marker=markers.getFirst();int mapped=mode==3?1:mode==4?4:2;
                    require(marker.owner()==owner&&game.items().currentEpoch(marker.epoch())&&marker.mode()==mapped&&marker.lifetime()==(mode==3?200:mode==4?300:100)&&marker.age()==0,"actual duplication source mode/lifetime/author/epoch "+mode);
                    near(marker.point(),origin.add(direction.scale(mode==3?7:mode==4?8:14)),"exact native duplication source offset "+mode);
                }
                clear();
            }
        }
        void detachedElectro() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(0,0,1);
            var wall=BlockPos.containing(origin.add(0,0,6));var saved=game.maps().snapshot(owner.level(),wall);
            try {
                owner.level().setBlock(wall,Blocks.OBSIDIAN.defaultBlockState(),2|16|32);
                for(int mode=1;mode<=5;mode++) {
                    Cow victim=mode==3?cow(origin.add(0,-.5,3)):null;
                    emit(stack,LbcSauvojen.Kind.ELECTRO,mode,origin,direction);
                    var arcs=game.items().wands().electroSnapshot();
                    if(mode==2||mode==4) {
                        require(arcs.size()==(mode==2?9:1),"actual Erecto volley/sniper trace count "+mode);
                        for(int i=0;i<arcs.size();i++) {
                            var arc=arcs.get(i);require(arc.owner==owner&&game.items().currentEpoch(arc.epoch)&&arc.lifetime==200&&arc.trialOwner==null&&arc.erecto!=null&&arc.erecto.stack.isEmpty(),"Erecto detached source author/lifetime/no trial/no paid stack "+mode+"/"+i);
                            near(arc.position,origin,"Erecto native detached launch point "+mode+"/"+i);near(arc.erecto.origin,origin,"Erecto native captured path origin");
                            require(arc.erecto.kind==(mode==4?LbcErecto.SNIPER:i==0?LbcErecto.CENTRAL:LbcErecto.SIDE),"Erecto actual source trace kind "+mode+"/"+i);
                        }
                        require(game.items().erecto().sniperWindupTicks(owner)==0,"detached sniper never applies paid windup");
                    } else if(mode==5) {
                        var spheres=game.items().erecto().sphereSnapshot();require(spheres.size()==1,"one native detached Erecto sphere");
                        var sphere=spheres.getFirst();require(sphere.owner==owner&&game.items().currentEpoch(sphere.epoch)&&sphere.age==0,"sphere actual source author/epoch/age");
                        require(sphere.position.distanceToSqr(origin.add(direction.scale(5)))<1,"sphere centers one unit before actual detached wall contact");
                    } else {
                        require(arcs.isEmpty(),"source Electro/Erecto ray is immediate, without timed traces "+mode);
                        require(base.recording.packets.stream().anyMatch(ClientboundLevelParticlesPacket.class::isInstance),"source instant ray native particles "+mode);
                        if(mode==3)require(victim!=null&&victim.getHealth()<victim.getMaxHealth()&&victim.getKillCredit()==owner&&victim.hasEffect(MobEffects.GLOWING)&&victim.getEffect(MobEffects.SLOWNESS).getAmplifier()==100,"actual one-shot Erecto ray damage/effects inherits owner");
                    }
                    require(base.mana.hudValue(owner,"ray")==0,"detached helper cannot create paid forty-tick ray counter");clear();removeCows();
                }
            } finally{clear();removeCows();game.maps().restoreSnapshot(owner.level(),wall,saved);}
        }
        void detachedStonekivi() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(0,0,1);
            var wall=BlockPos.containing(origin.add(0,0,6));var saved=game.maps().snapshot(owner.level(),wall);
            require(game.items().stonekivi().ready(),"prepared real Stonekivi source templates");
            try {
                owner.level().setBlock(wall,Blocks.STONE.defaultBlockState(),2|16|32);
                for(int mode=5;mode<=8;mode++) {
                    emit(stack,LbcSauvojen.Kind.STONE,mode,origin,direction);
                    var queue=(Collection<?>)state(game.items().stonekivi(),"work");require(queue.size()==1,"one actual queued Stonekivi producer "+mode);
                    var task=queue.iterator().next();
                    if(mode==8) {
                        require(task.getClass().getName().endsWith("LbcMagmify$Work")&&state(task,"owner")==owner,"Sun creates actual owned Magmify terrain state");
                        require(((it.unimi.dsi.fastutil.longs.LongSet)state(task,"discovered")).contains(wall.asLong()),"Sun native discovery starts at detached ray wall");
                    } else {
                        var children=(Collection<?>)state(task,"children");
                        require(mode==5?children.size()==5:mode==7?children.size()==1:!children.isEmpty(),"actual layered/Earth/comet source work structure, without donor trial task "+mode);
                    }
                    var particles=base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).toList();
                    require(!particles.isEmpty(),"actual Stonekivi native ray particles "+mode);near(new Vec3(particles.getFirst().x(),particles.getFirst().y(),particles.getFirst().z()),origin,"Stonekivi ray starts exactly at drone "+mode);
                    clear();
                }
            } finally{clear();game.maps().restoreSnapshot(owner.level(),wall,saved);}
        }
        void detachedRainPsychicAndFire() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(0,0,1);
            var wall=BlockPos.containing(origin.add(0,0,6));var saved=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            for(var p:BlockPos.betweenClosed(wall.offset(-2,-2,-2),wall.offset(2,2,2)))saved.put(p.immutable(),game.maps().snapshot(owner.level(),p));
            try {
                owner.level().setBlock(wall,Blocks.OBSIDIAN.defaultBlockState(),2|16|32);
                emit(stack,LbcSauvojen.Kind.BLIND,2,origin,direction);
                var clouds=(Collection<?>)state(game.items().blind(),"clouds");require(clouds.size()==1,"one actual native blind rain cloud");
                var cloud=clouds.iterator().next();require(state(cloud,"owner")==owner&&game.items().currentEpoch((String)state(cloud,"epoch")),"blind rain actual source author/epoch");
                require(((Vec3)state(cloud,"position")).distanceToSqr(origin.add(0,10,6))<2,"blind rain cloud ten blocks above detached ray contact");clear();
                var victim=cow(origin.add(0,-.5,3));
                emit(stack,LbcSauvojen.Kind.BLIND,6,origin,direction);
                require(game.items().psycho().rayCount(owner)==0&&game.items().psycho().count()==0,"source immediate mindblowing loop never creates paid forty-tick queue");
                require(game.items().psycho().effectValue(victim,"scared")==1&&game.items().psycho().effectValue(victim,"lookback")==1&&game.items().psycho().effectValue(victim,"piss")==1&&victim.hasEffect(MobEffects.BLINDNESS),"actual immediate psychic source ray affects each native target once");
                require(victim.getHealth()<victim.getMaxHealth()&&victim.getKillCredit()==owner,"native immediate psychic damage retains actual source owner");clear();removeCows();
                emit(stack,LbcSauvojen.Kind.FIRE,1,origin,direction);
                var fireballs=new ArrayList<Projectile>();for(var e:owner.level().getAllEntities())if(!base.before.contains(e)&&!e.isRemoved()&&e.getType()==EntityTypes.FIREBALL&&e instanceof Projectile p)fireballs.add(p);
                require(fireballs.size()==1,"one actual native large Fireball");projectile(fireballs.getFirst(),origin.add(direction),direction,2.5,"source big Fire localforward1");fireballs.forEach(Entity::discard);clear();
                emit(stack,LbcSauvojen.Kind.FIRE,2,origin,direction);
                require(saved.keySet().stream().anyMatch(p->owner.level().getBlockState(p).is(Blocks.LAVA)),"concentrated Fire creates actual owned terminal lava at detached wall");
                require(base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).anyMatch(p->p.particle().getType()==net.minecraft.core.particles.ParticleTypes.FALLING_LAVA&&new Vec3(p.x(),p.y(),p.z()).distanceToSqr(origin)<1e-9),"concentrated Fire source trail begins at drone");
            } finally{clear();removeCows();saved.forEach((p,s)->game.maps().restoreSnapshot(owner.level(),p,s));}
        }
        void curseAndCultist() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);
            Vec3 origin=owner.getEyePosition().add(2,1,-2),direction=new Vec3(0,0,1);
            var victim=cow(origin.add(0,-.5,3));emit(stack,LbcSauvojen.Kind.AIR,7,origin,direction);drainRays();
            require(game.items().homing().curseTicks(victim)==1200,"source AIR7 applies sixty-second Curse to actual detached ray target");clear();removeCows();
            var nearest=owner.level().players().stream().filter(p->p!=owner&&!p.isSpectator()&&!game.isObserver(p)&&p.isAlive())
                    .min(Comparator.comparingDouble(p->p.position().distanceToSqr(origin))).orElseThrow();
            emit(stack,LbcSauvojen.Kind.BLIND,8,origin,direction);
            var summons=game.items().summons().snapshot();require(summons.size()==1,"source final Blind helper creates one actual native cultist");
            var summon=summons.getFirst();require(game.items().summons().owner(summon)==owner&&game.items().currentEpoch(summon),"cultist retains drone owner's source and epoch at another player's origin");
            Vec3 offset=summon.position().subtract(nearest.position());require(offset.y==0&&offset.lengthSqr()==4&&(offset.x==0||offset.z==0),"cultist source is exactly two blocks along drone horizontal local axes around nearest noncaster player");clear();
        }
        void nativeAcquisition() {
            var owner=base.owner;
            var loot=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_sauvojen_wand");
            require(loot.size()==1&&loot.getFirst().is(LbcWands.ACADEMY_SAUVOJEN)&&loot.getFirst().getCount()==1,"narrow Academy acquisition native identity/count");
            var stack=loot.getFirst();var data=LbcFartus.data(stack);
            require(Identifier.parse("lbc:sauvojen_wand").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_x").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source Academy Sauvojen exact existing model and tierX");
            require(stack.get(DataComponents.LORE).lines().size()==5,"source four authored lore rows plus Academy suffix");
            var ordinary=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/sauvojen_wand").getFirst();
            require(stack.get(DataComponents.CUSTOM_NAME).equals(ordinary.get(DataComponents.CUSTOM_NAME))&&stack.get(DataComponents.LORE).lines().subList(0,4).equals(ordinary.get(DataComponents.LORE).lines()),"Academy preserves all exact source name/lore component structures and styles");
            var suffix=stack.get(DataComponents.LORE).lines().getLast();
            require(suffix.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("upgraded_item")&&suffix.getStyle().getColor()!=null&&suffix.getStyle().getColor().getValue()==0x55ff55&&!suffix.getStyle().isItalic(),"exact green nonitalic upgraded_item suffix");
            require(data.getIntOr("upgraded",0)==1&&data.getIntOr("mode",0)==1&&data.getIntOr("sauvojen_wand",0)==1&&data.getIntOr("staff",0)==1&&data.getIntOr("magicwand",0)==1&&data.getIntOr("craftusable",0)==1&&data.getIntOr("transformable",0)==1,"source native Academy five base flags and upgraded/mode metadata");
            require(stack.getHoverName().getString().equals(LbcWands.SAUVOJEN.getDefaultInstance().getHoverName().getString()),"Academy preserves authored base name");
            owner.setItemInHand(InteractionHand.MAIN_HAND,stack);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);base.mana.set(owner,2500);
            require(base.input(InteractionHand.MAIN_HAND)&&base.mana.get(owner)==0&&service.count()==1&&service.snapshot().getFirst().academy,"actual native Academy leaf routes paid input to upgraded engine");clear();
        }
        void chaosAndNuke() {
            var owner=base.owner;var stack=held(InteractionHand.OFF_HAND);var level=owner.level();
            Vec3 previous=owner.position();float yaw=owner.getYRot(),pitch=owner.getXRot();
            boolean tnt=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES);
            int protection=game.config().tntProtection;
            var bolts=new ArrayList<Entity>();
            var managed=(Collection<?>)state(game.items().electro(),"liveBolts");
            var priorManaged=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());priorManaged.addAll(managed);
            try {
                // Outside build height no terrain can be destroyed; the native blast and lightning paths still execute.
                owner.teleportTo(level,previous.x,level.getMaxY()+80,previous.z,Set.of(),yaw,pitch,true);owner.setDeltaMovement(Vec3.ZERO);
                for(int x=-35;x<=35;x+=7)for(int z=-35;z<=35;z+=7)require(level.hasChunkAt(BlockPos.containing(owner.position().add(x,0,z))),"resident source Chaos cube footprint");
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,true,game.server());
                game.config().tntProtection=0;
                Vec3 origin=owner.getEyePosition().add(0,50,-60),direction=new Vec3(0,0,1),center=owner.getEyePosition().add(0,50,0);
                base.recording.packets.clear();emit(stack,LbcSauvojen.Kind.AIR,11,origin,direction);
                // The service adds an actual bolt only after addFreshEntity succeeds; outside-height sections can be absent from visible entity iteration.
                for(var e:managed)if(!priorManaged.contains(e)&&e instanceof net.minecraft.world.entity.LightningBolt bolt&&!bolt.isRemoved()&&bolt.getCause()==owner)bolts.add(bolt);
                require(bolts.size()==40,"source Chaos creates forty successfully added actual native lightning bolts, actual="+bolts.size());
                Vec3 floored=new Vec3((int)center.x,(int)center.y,(int)center.z);
                for(var bolt:bolts) {
                    Vec3 offset=bolt.position().subtract(floored);require(Math.abs(offset.x)<=35&&Math.abs(offset.y)<=35&&Math.abs(offset.z)<=35,"Chaos lightning actual center is drone+direction60 with integer±35 offsets");
                    require(bolt instanceof net.minecraft.world.entity.LightningBolt lightning&&lightning.getCause()==owner&&LbcElectro.managedLightning(lightning),"Chaos lightning retains actual native cause and shared managed lifetime");
                }
                long packets=base.recording.packets.stream().filter(p->p.getClass().getSimpleName().equals("ClientboundExplodePacket")).count();
                require(packets>0&&packets<=80&&game.items().exasperato().snapshot().isEmpty(),"source Chaos runs native immediate blasts without donor missile carrier");
            } finally{
                var fresh=new ArrayList<Entity>();for(var e:managed)if(!priorManaged.contains(e)&&e instanceof net.minecraft.world.entity.LightningBolt bolt&&bolt.getCause()==owner)fresh.add(bolt);
                fresh.forEach(Entity::discard);managed.removeAll(fresh);
                clear();game.config().tntProtection=protection;level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,tnt,game.server());owner.teleportTo(level,previous.x,previous.y,previous.z,Set.of(),yaw,pitch,true);owner.setDeltaMovement(Vec3.ZERO);
            }
            var drone=launch(LbcSauvojen.Kind.AIR);Vec3 origin=drone.position;var target=cow(origin.add(0,0,6));
            drone.nextMode=12;drone.lifetime=10;base.recording.packets.clear();service.tick();
            Vec3 lowered=origin.add(0,-1,0),away=lowered.subtract(owner.getEyePosition()).multiply(1,0,1).normalize().scale(.1);
            require(service.count()==1&&drone.nextMode==0&&drone.lifetime==11,"source direct NUKETICK consumes final mode once and wraps to zero");
            near(drone.position,lowered.add(away).add(0,.04,0),"source NUKETICK descends original drone1 before normal horizontal motion/rise");
            var particles=base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast)
                    .filter(p->p.particle() instanceof net.minecraft.core.particles.DustColorTransitionOptions).toList();
            require(particles.size()==5,"direct NUKETICK emits five source downward samples");
            for(int i=0;i<5;i++)near(new Vec3(particles.get(i).x(),particles.get(i).y(),particles.get(i).z()),origin.add(0,-i*.25,0),"NUKETICK exact source sample "+i);
            require(game.items().exasperato().snapshot().isEmpty(),"direct NUKETICK never creates donor distant500tick missile");clear();removeCows();
        }
        void scheduling() {
            for(var kind:LbcSauvojen.Kind.values()) {
                int i=kind.ordinal();var drone=launch(kind);Vec3 origin=drone.position;
                require(kind.modes()==MODES[i]&&kind.academyCadence()==CADENCES[i],"independent source counter modulo/cadence "+kind);
                near(origin,base.owner.getEyePosition().add(base.owner.getLookAngle().scale(2)),"source initial eye+look2 "+kind);
                require(drone.holder.getWatchingPlayers().contains(base.owner.connection),"native manual viewer attached "+kind);
                require(drone.carrier.entityTags().contains(LbcSauvojen.DRONE)&&game.items().currentEpoch(drone.carrier),"owned current-epoch carrier "+kind);
                Set<Identifier> allowed=new HashSet<>();for(var name:MODELS[i])allowed.add(Identifier.parse("lbc:"+name));
                Set<Identifier> observed=new HashSet<>();
                // Source facing is mandatory. No target also prevents counter advance, but not the independent model lottery.
                for(int age=0;age<96;age++) {
                    drone.position=origin;drone.carrier.setPos(origin);service.tick();
                    require(drone.nextMode==0&&base.count()==0,"no-target cadence cannot emit or consume a mode "+kind);
                    var model=drone.display.getItem().get(DataComponents.ITEM_MODEL);
                    require(allowed.contains(model),"source per-tick model set "+kind+" "+model);observed.add(model);
                }
                require(observed.size()>1,"models reroll independently while no target can fire "+kind);
                var target=cow(origin.add(0,0,7));
                drone.position=origin;drone.carrier.setPos(origin);drone.lifetime=CADENCES[i]==1?2:CADENCES[i]-1;
                service.tick();require(drone.nextMode==(CADENCES[i]==1?1:0),"no early source modulo dispatch "+kind);
                if(CADENCES[i]!=1) {
                    drone.position=origin;drone.carrier.setPos(origin);service.tick();
                    require(drone.nextMode==1,"first exact source modulo dispatch advances once "+kind);
                }
                near(drone.direction,target.getEyePosition().subtract(origin).normalize(),"native nearest target direction "+kind);
                base.clear();removeCows();
                drone.lifetime=398;service.tick();require(service.count()==1&&drone.lifetime==399,"survives source age399 "+kind);
                service.tick();require(service.count()==0&&drone.carrier.isRemoved()&&drone.holder.getWatchingPlayers().isEmpty(),"source cutoff400 removes carrier and viewers before firing "+kind);
                base.clear();
            }
        }
        void lifecycle() {
            var drone=launch(LbcSauvojen.Kind.RUBY);var start=drone.position;
            var target=cow(start.add(-3,0,0));var role=cow(start.add(0,0,.5));role.entityTags().add("strict_map_object");
            var greed=cow(start.add(0,0,-.5));greed.entityTags().add("sin_greed_active");
            service.tick();near(drone.direction,target.getEyePosition().subtract(start).normalize(),"target skips protected role and active Greed");
            Vec3 prior=drone.position,aim=drone.direction;int age=drone.lifetime,mode=drone.nextMode;
            game.items().timeStop().begin(base.owner.level(),40);
            try{service.tick();require(drone.lifetime==age&&drone.nextMode==mode,"global stop freezes age/counter");near(drone.position,prior,"global stop freezes motion");near(drone.direction,aim,"global stop freezes aim");}
            finally{game.items().timeStop().end(base.owner.level());}
            base.owner.entityTags().add("time_stopped");
            try{service.tick();require(drone.lifetime==age&&drone.nextMode==mode,"personal stop freezes age/counter");near(drone.position,prior,"personal stop freezes motion");}
            finally{base.owner.entityTags().remove("time_stopped");}
            Vec3 away=prior.subtract(base.owner.getEyePosition()).multiply(1,0,1).normalize().scale(.1);
            service.tick();near(drone.position,prior.add(away).add(0,.04,0),"source negative local forward repels horizontally0.1 and rises0.04");
            removeCows();var cell=BlockPos.containing(drone.position);var saved=game.maps().snapshot(base.owner.level(),cell);
            try {
                base.owner.level().setBlock(cell,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SKIP_ON_PLACE);
                service.tick();require(service.count()==1&&!drone.carrier.isRemoved(),"Academy source survives solid central block");
            } finally{game.maps().restoreSnapshot(base.owner.level(),cell,saved);}
            service.unloaded(drone.carrier);require(service.count()==0&&drone.carrier.isRemoved(),"native unload retires Academy producer");
            drone=launch(LbcSauvojen.Kind.RUBY);service.forget(base.owner);require(service.count()==0&&drone.carrier.isRemoved(),"owner forget retires Academy identity");
            drone=launch(LbcSauvojen.Kind.RUBY);
            base.field=new Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(drone.position);base.field.entityTags().add("stopper_magic");
            require(base.owner.level().addFreshEntity(base.field),"native Academy antimagic marker");game.items().magic().loaded(base.field);
            try{service.tick();require(service.count()==0&&drone.carrier.isRemoved(),"antimagic at drone origin removes producer");}
            finally{base.removeField();}
            clear();
        }
        void payment() {
            for(var hand:InteractionHand.values()) {
                var stack=held(hand);base.mana.set(base.owner,2499);
                require(!base.input(hand)&&base.mana.get(base.owner)==2499&&service.count()==0,"Academy insufficient mana preserves input "+hand);
                base.mana.set(base.owner,2500);
                require(base.input(hand)&&base.mana.get(base.owner)==0&&stack.getCount()==1&&service.count()==1,"native Academy hand pays exactly2500 "+hand);
                var drone=service.snapshot().getFirst();require(drone.academy&&drone.hand==hand&&ItemStack.isSameItemSameComponents(drone.usedStack,stack),"Academy captures actual used hand/components "+hand);
                stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("changed after cast"));base.owner.setItemInHand(hand,ItemStack.EMPTY);
                require(!ItemStack.isSameItemSameComponents(drone.usedStack,stack),"Academy captured stack remains independent "+hand);clear();
            }
            held(InteractionHand.MAIN_HAND);var off=held(InteractionHand.OFF_HAND);var main=off.copy();base.owner.setItemInHand(InteractionHand.MAIN_HAND,main);
            base.mana.set(base.owner,5000);require(base.input(InteractionHand.MAIN_HAND)&&base.mana.get(base.owner)==0&&service.count()==2,"native Academy parallel hands debit independently");clear();
        }
        @Override public void close() {
            removeCows();if(!retain){
                clear();
                var fresh=new ArrayList<Entity>();for(var e:base.owner.level().getAllEntities())if(!base.before.contains(e))fresh.add(e);
                fresh.forEach(Entity::discard);
            }base.retain=retain;
            base.mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math").set(cost);
            advancements.forEach((a,done)->{for(String key:a.value().criteria().keySet())if(done.contains(key))base.owner.getAdvancements().award(a,key);else base.owner.getAdvancements().revoke(a,key);});
            base.owner.getStats().setValue(base.owner,Stats.ITEM_USED.get(LbcWands.SAUVOJEN),stat);base.close();
            extraStats.forEach((s,value)->base.owner.getStats().setValue(base.owner,s,value));
            peers.values().forEach(saved->saved.restore(game));restoreAttribution();
            customScores.forEach((player,scores)->scores.forEach((name,value)->base.mana.score(player,name).set(value)));
        }
    }
    private static List<LbcSauvojen.Drone> retained;
    private static String retainedEpoch;
    public static void run(GameController game) {
        record Step(String name,java.util.function.Consumer<Fixture> run){}
        var failures=new ArrayList<String>();
        for(var step:List.of(
                new Step("nativeAcquisition",Fixture::nativeAcquisition),
                new Step("firstSourceModes",Fixture::firstSourceModes),
                new Step("detachedElementalVariants",Fixture::detachedElementalVariants),
                new Step("detachedStars",Fixture::detachedStars),
                new Step("detachedRockAndRubyRays",Fixture::detachedRockAndRubyRays),
                new Step("detachedDuplicating",Fixture::detachedDuplicating),
                new Step("detachedElectro",Fixture::detachedElectro),
                new Step("detachedStonekivi",Fixture::detachedStonekivi),
                new Step("detachedRainPsychicAndFire",Fixture::detachedRainPsychicAndFire),
                new Step("detachedAcidAndAir",Fixture::detachedAcidAndAir),
                new Step("detachedHomingAndBolts",Fixture::detachedHomingAndBolts),
                new Step("detachedConsuming",Fixture::detachedConsuming),
                new Step("detachedCorruption",Fixture::detachedCorruption),
                new Step("detachedPsychicCharges",Fixture::detachedPsychicCharges),
                new Step("detachedBlazeModes",Fixture::detachedBlazeModes),
                new Step("curseAndCultist",Fixture::curseAndCultist),
                new Step("chaosAndNuke",Fixture::chaosAndNuke),
                new Step("scheduling",Fixture::scheduling),
                new Step("lifecycle",Fixture::lifecycle),
                new Step("payment",Fixture::payment))){
            // Fresh native state permits independent diagnostic groups; teardown failure is fatal.
            try(var f=new Fixture(game)){
                try{step.run().accept(f);}
                catch(IllegalStateException failure){
                    failures.add(step.name()+": "+failure.getMessage());
                    SkyWars2.LOGGER.info("Sauvojen Academy group rejected: {}",step.name(),failure);
                }
            }
        }
        require(failures.isEmpty(),"independent native groups: "+failures);
        SkyWars2.LOGGER.info("[check] PASS sauvojen-academy (80 source payload rows, native origins/owners, source cadences/target gate/model lottery,400tick lifetime, guards and paid hands)");
    }
    public static void stopProbe(GameController game) {
        require(retained==null,"exclusive Academy stop probe");
        try(var f=new Fixture(game,true)) {
            for(var kind:LbcSauvojen.Kind.values())f.launch(kind);
            retained=f.service.snapshot().stream().filter(d->d.academy).toList();require(retained.size()==9,"nine retained Academy drones");
            retainedEpoch=game.items().epoch();f.retain=true;
        }
        SkyWars2.LOGGER.info("[check] PASS sauvojen-academy-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(retained!=null,"actual Academy stop probe exists");
        try{require(!game.items().currentEpoch(retainedEpoch)&&game.items().sauvojen().count()==0&&retained.stream().allMatch(d->d.carrier.isRemoved()&&d.holder.getWatchingPlayers().isEmpty()),"actual stop clears Academy carriers, viewers and epoch");}
        finally{retained.forEach(d->d.carrier.discard());retained=null;retainedEpoch=null;}
        SkyWars2.LOGGER.info("[check] PASS sauvojen-academy-cleanup");
    }
}
