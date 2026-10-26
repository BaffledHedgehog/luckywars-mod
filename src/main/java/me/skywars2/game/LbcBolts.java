package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lightning Bolts/Boltbolt share native Electro lightning, block work and the rideable-arrow service. */
public final class LbcBolts {
    private static final net.minecraft.core.particles.DustParticleOptions REFORGE_DUST=
            new net.minecraft.core.particles.DustParticleOptions(0x00C3FF,.3f);
    private static final Identifier AIR=id("air_sphere_11x11x11"), SNOW=id("big_snowball");
    private static final TagKey<net.minecraft.world.level.block.Block> SNOW_REPLACE=
            TagKey.create(Registries.BLOCK,id("minecraft","tannerkivi"));
    private static final Vec3[] OFFSETS={new Vec3(10,0,0),new Vec3(0,0,10),new Vec3(-10,0,0),new Vec3(0,0,-10),new Vec3(0,10,0)};
    private record ReforgeRule(String category,String table,String... exclusions) {
        boolean matches(net.minecraft.nbt.CompoundTag data) {
            if(data.getIntOr(category,0)!=1) return false;
            for(String exclusion:exclusions) if(data.getIntOr(exclusion,0)==1) return false;
            return true;
        }
    }
    private static final ReforgeRule[] NORMAL_REFORGE={
            new ReforgeRule("accessory","transformables_accessory"),
            new ReforgeRule("axe","transformables_axe"),
            new ReforgeRule("bow","transformables_bow"),
            new ReforgeRule("hoe","transformables_hoe"),
            new ReforgeRule("pickaxe","transformables_pickaxe"),
            new ReforgeRule("potion","transformables_potion"),
            new ReforgeRule("shield","transformables_shield"),
            new ReforgeRule("shooter","transformables_shooter"),
            new ReforgeRule("shovel","transformables_shovel"),
            new ReforgeRule("staff","transformables_staff"),
            new ReforgeRule("sword","transformables_sword"),
            new ReforgeRule("totem","transformables_totem"),
            new ReforgeRule("spellitem","transformables_spells"),
            new ReforgeRule("summoner","transformables_summons"),
            new ReforgeRule("trident","transformables_trident"),
            new ReforgeRule("arcanum","transformables_arcanum"),
            new ReforgeRule("libspell","spells_library"),
            new ReforgeRule("helmet","transformables_helmet"),
            new ReforgeRule("chestplate","transformables_chestplate"),
            new ReforgeRule("leggings","transformables_leggings"),
            new ReforgeRule("boots","transformables_boots"),
            new ReforgeRule("metasola","halloween/metasola")};
    private static final ReforgeRule[] ULTRA_REFORGE={
            new ReforgeRule("accessory","transformables_accessory_all"),
            new ReforgeRule("axe","transformables_axe_all"),
            new ReforgeRule("bow","transformables_bow_all"),
            new ReforgeRule("hoe","transformables_hoe_all"),
            new ReforgeRule("pickaxe","transformables_pickaxe"),
            new ReforgeRule("potion","transformables_potion"),
            new ReforgeRule("shield","transformables_shield"),
            new ReforgeRule("shooter","transformables_shooter_all"),
            new ReforgeRule("shovel","transformables_shovel"),
            new ReforgeRule("staff","transformables_staff_all"),
            new ReforgeRule("sword","transformables_sword_all"),
            new ReforgeRule("totem","transformables_totem"),
            new ReforgeRule("spellitem","transformables_spells"),
            new ReforgeRule("summoner","transformables_summons_all"),
            new ReforgeRule("moisei","transformables_boots_all"),
            new ReforgeRule("trident","transformables_trident"),
            new ReforgeRule("arcanum","transformables_arcanum_all"),
            new ReforgeRule("libspell","spells_library"),
            new ReforgeRule("assasin","neko_set_assasin"),
            new ReforgeRule("berserk","neko_set_berserk"),
            new ReforgeRule("chaoschestplate","neko_set_chaos"),
            new ReforgeRule("mage","neko_set_mage","nyauwu"),
            new ReforgeRule("paladin","neko_set_paladin"),
            new ReforgeRule("phantom","neko_set_phantom"),
            new ReforgeRule("gownoleggings","neko_set_shitty"),
            new ReforgeRule("shulkshell","neko_set_shulker"),
            new ReforgeRule("reactive","neko_set_steampunk_andboots","technomk2"),
            new ReforgeRule("techno","neko_set_techno","technomk2"),
            new ReforgeRule("technomk2","neko_set_technomk2"),
            new ReforgeRule("nyauwu","neko_set_nyauwu"),
            new ReforgeRule("jumpboost","neko_set_traveller_andboots","reactive"),
            new ReforgeRule("randchanted","set_randchanted","sword","pickaxe","bow","axe"),
            new ReforgeRule("multiplying","set_multiplying"),
            new ReforgeRule("dynamite","transformables_chestplate"),
            new ReforgeRule("erosion","neko_set_erosion"),
            new ReforgeRule("nanobots","neko_set_nanobots"),
            new ReforgeRule("flesh","neko_set_flesh"),
            new ReforgeRule("glass","neko_set_glass"),
            new ReforgeRule("overload","neko_set_overload"),
            new ReforgeRule("metasola","halloween/metasola")};
    private static final net.minecraft.core.particles.DustColorTransitionOptions INFBOLT_DUST=
            new net.minecraft.core.particles.DustColorTransitionOptions(0xff0061,0xffffff,1);
    private final Map<LbcStonekivi.Work,ServerPlayer> reforgeJobs=new IdentityHashMap<>();

    private final GameController game;
    private final Set<Bolt> bolts=new LinkedHashSet<>();
    private final Set<Water> waters=new LinkedHashSet<>();
    private final Set<Meteor> meteors=new LinkedHashSet<>();
    private final Set<Chain> chains=new LinkedHashSet<>();
    private volatile Map<Identifier,List<MapManager.TemplateSlice>> templates=Map.of();
    private volatile int preparation;
    private int ticks;
    private int workGeneration;
    private final Map<ServerPlayer,Integer> ownerWorkGeneration=new IdentityHashMap<>();
    private static final net.minecraft.nbt.CompoundTag METEOR_PRESET=meteorPreset();
    // User: ten seconds, free Reforge without an entry threshold, repeating chains.
    private static final int EVERLASTING_TICKS=200;

    static class Owned {
        final ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        Vec3 position;
        int age;
        Owned(ServerPlayer owner,Vec3 position,String epoch) {
            this.owner=owner; this.level=owner.level(); this.position=position; this.epoch=epoch;
        }
    }
    static final class Bolt extends Owned {
        final int lifetime;
        final int strikeInterval;
        Bolt(ServerPlayer owner,Vec3 position,String epoch) {
            this(owner,position,epoch,EVERLASTING_TICKS,40);
        }
        Bolt(ServerPlayer owner,Vec3 position,String epoch,int lifetime,int strikeInterval) {
            super(owner,position,epoch); this.lifetime=lifetime; this.strikeInterval=strikeInterval;
        }
    }
    static final class Water extends Owned {
        Water(ServerPlayer owner,Vec3 position,String epoch) { super(owner,position,epoch); }
    }
    static final class Meteor extends Owned {
        final Display.ItemDisplay display;
        int impactVisualTicks;
        Meteor(ServerPlayer owner,Vec3 position,String epoch,Display.ItemDisplay display) {
            super(owner,position,epoch); this.display=display;
        }
    }
    static final class Chain extends Owned {
        Set<LivingEntity> current=Collections.newSetFromMap(new IdentityHashMap<>());
        int untilNext=40;
        Chain(ServerPlayer owner,Vec3 position,String epoch) { super(owner,position,epoch); }
    }
    private final LbcChainLightningStatus chainStatus;
    LbcBolts(GameController game) { this.game=game;chainStatus=new LbcChainLightningStatus(game); }
    public boolean acceptStatus(LivingEntity target,MobEffectInstance effect,Entity source){return chainStatus.accept(target,effect,source);}
    void observeStatus(LivingEntity target){chainStatus.observe(target);}
    int statusCount(LivingEntity target){return chainStatus.queued(target);}
    public void clearChainStatus(LivingEntity target){chainStatus.clear(target);}
    static void register() { }
    private static Identifier id(String path) { return id("lbc",path); }
    private static Identifier id(String namespace,String path) { return Identifier.fromNamespaceAndPath(namespace,path); }
    private static net.minecraft.nbt.CompoundTag meteorPreset() {
        try {
            return net.minecraft.nbt.TagParser.parseCompoundFully("{view_range:10f,teleport_duration:2,interpolation_duration:2,Rotation:[-90f,-90f],transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],scale:[3f,3f,3f],right_rotation:[0f,0f,0f,1f]},item:{id:\"minecraft:repeating_command_block\",count:1,components:{\"minecraft:item_model\":\"lbc:big_snowball\"}}}");
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new ExceptionInInitializerError(error); }
    }
    public void prepare() {
        int generation=++preparation; templates=Map.of();
        game.maps().prepareSpellTemplates(List.of(AIR,SNOW)).whenComplete((value,error)-> {
            if(generation!=preparation) return;
            if(error!=null) SkyWars2.LOGGER.error("Boltbolt template preparation failed",error);
            else templates=value;
        });
    }
    public boolean ready() { return templates.size()==2; }
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform) {
        // Chain is attached to living targets; it has no independent spell origin.
        transformOrigins(bolts,level,transform);
        transformOrigins(waters,level,transform);
        // Remove before discard: the display unload callback also updates meteors.
        for(var meteor:List.copyOf(meteors)) if(meteor.level==level) {
            Vec3 next=transform.apply(meteor.position);
            if(next==null) {
                meteors.remove(meteor);
                if(!meteor.display.isRemoved()) meteor.display.discard();
            } else {
                meteor.position=next;
                if(!meteor.display.isRemoved()) meteor.display.setPos(next);
            }
        }
    }
    private static <T extends Owned> void transformOrigins(Set<T> states,ServerLevel level,
            java.util.function.UnaryOperator<Vec3> transform) {
        states.removeIf(state->{
            if(state.level!=level) return false;
            Vec3 next=transform.apply(state.position);
            if(next==null) return true;
            state.position=next; return false;
        });
    }

    public int count() { return bolts.size()+waters.size()+meteors.size()+chains.size()+reforgeJobs.size(); }
    List<Bolt> snapshot() { return List.copyOf(bolts); }
    List<Water> waterSnapshot() { return List.copyOf(waters); }
    List<Meteor> meteorSnapshot() { return List.copyOf(meteors); }
    int chainCount() { return chains.size(); }
    List<Chain> chainSnapshot() { return List.copyOf(chains); }
    boolean hasChain(LivingEntity target) { return chains.stream().anyMatch(chain->chain.current.contains(target)); }
    int waterCount() { return waters.size(); }
    int meteorCount() { return meteors.size(); }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && owner.isAlive() && !owner.isRemoved() && game.items().mana().visible(owner)
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && owner.level()==game.maps().level();
    }
    private boolean valid(Owned state) { return valid(state.owner) && state.level==state.owner.level() && state.epoch.equals(game.items().epoch()); }
    private boolean castBlocked(ServerPlayer owner) {
        return paused(owner) || game.items().frost().isFrozen(owner) || game.items().magic().suppressed(owner);
    }
    private boolean paused(ServerPlayer owner) {
        // Frost blocks new casts; already released projectiles and passive effects continue (user).
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped");
    }
    private boolean target(ServerPlayer owner,Entity target) {
        return target.level()==owner.level() && !target.entityTags().contains("strict_map_object")
                && game.items().wands().target(owner,target)
                && !game.items().magic().suppressedAt(owner.level(),target.position())
                && !target.entityTags().contains("nomagic_active");
    }
    private boolean projectileTarget(ServerPlayer owner,Entity target) {
        return target.level()==owner.level() && game.items().wands().projectileTarget(owner,target)
                && !game.items().magic().suppressedAt(owner.level(),target.position())
                && !target.entityTags().contains("nomagic_active");
    }
    private List<LivingEntity> projectileTargets(ServerPlayer owner,Vec3 at,double radius) {
        var result=new ArrayList<LivingEntity>();
        for(var target:game.items().lifetimes().loadedTargets())
            if(projectileTarget(owner,target) && target.position().distanceToSqr(at)<=radius*radius) result.add(target);
        return result;
    }
    private boolean chainTarget(ServerPlayer owner,LivingEntity target) {
        return target.level()==owner.level() && game.items().wands().validTarget(target)
                && !game.items().magic().suppressedAt(owner.level(),target.position())
                && !target.entityTags().contains("nomagic_active")
                && (!(target instanceof ServerPlayer player) || !game.items().magic().rejectCustomEffect(player));
    }
    /** Chain lightning is wild: allies and caster share the same magic/role checks. */
    private List<LivingEntity> chainTargets(ServerPlayer owner,Vec3 at,double radius) {
        var result=new ArrayList<LivingEntity>();
        for(var target:game.items().lifetimes().loadedTargets())
            if(chainTarget(owner,target) && target.position().distanceToSqr(at)<=radius*radius) result.add(target);
        return result;
    }
    private List<LivingEntity> targets(ServerPlayer owner,Vec3 at,double radius) {
        var result=new ArrayList<LivingEntity>();
        for(var target:game.items().lifetimes().loadedTargets())
            if(target.level()==owner.level() && target(owner,target) && target.position().distanceToSqr(at)<=radius*radius) result.add(target);
        return result;
    }
    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean donor,boolean upgraded) {
        if(!valid(owner) || castBlocked(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        var mana=game.items().mana();
        int mode=upgraded?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,donor?3:4):1;
        if(upgraded && owner.isShiftKeyDown()) {
            // (user) Любой режим доступен при пустой или отрицательной мане, не только Reforge.
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int nextMode=Math.floorMod(data.getIntOr("mode",1),donor?3:4)+1;
            data.putInt("mode",nextMode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            owner.sendSystemMessage(Component.translatable(donor?switch(nextMode) {
                case 2->"mode_reforge"; case 3->"mode_chain"; default->"mode_default";
            }:switch(nextMode) {
                case 2->"mode_wind_racer"; case 3->"mode_snow_meteor"; case 4->"mode_watering"; default->"mode_default";
            })); return true;
        }
        if(!donor && mode==3 && !ready()) return false;
        int cost=mana.score(ScoreHolder.forNameOnly(donor?"lightning_bolt_wand_mana_use":"boltbolt_wand_mana_use"),"lbc.math").get();
        if(donor && mode==2) cost=0;
        if(!mana.trySpendWand(owner,cost,cost)) return false;
        if(donor) donor(owner,hand,stack,mode,upgraded);
        else boltbolt(owner,hand,stack,mode);
        LbcOverload.sound(owner,"minecraft:entity.lightning_bolt.thunder",1,2,0);
        if(!(donor && mode==2)) game.items().wands().wandCasted(owner);
        else game.items().trinkets().castWand(owner);
        return true;
    }
    boolean grimoire(ServerPlayer owner,int mode) {
        if(mode==3&&!ready())return false;
        // Library carriers are anchored at the eyes regardless of the consumed hand.
        boltbolt(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,mode);
        if(mode==1)LbcOverload.sound(owner,"minecraft:entity.lightning_bolt.thunder",1,2,0);
        return true;
    }
    private void boltbolt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode) {
        if(mode==2) game.items().teleport().castRideableArrow(owner,2,true);
        else if(mode==4) watering(owner);
        else {
            Vec3 at=ray(owner,hand,150,mode==3?ParticleTypes.SNOWFLAKE:ParticleTypes.CLOUD,false);
            if(at!=null) {
                if(mode==3) meteor(owner,at.add(0,50,0)); else terminal(owner,hand,stack,at);
            }
        }
    }

    /** The shared collision helper yields exactly one terminal and never probes an unloaded chunk. */
    private Vec3 ray(ServerPlayer owner,InteractionHand hand,double range,net.minecraft.core.particles.ParticleOptions particle,boolean items) {
        return ray(owner,hand,range,particle,items,false);
    }
    private Vec3 ray(ServerPlayer owner,InteractionHand hand,double range,net.minecraft.core.particles.ParticleOptions particle,boolean items,boolean wild) {
        Vec3 from=owner.getEyePosition();
        if(hand==InteractionHand.OFF_HAND) from=from.add(owner.getLookAngle().scale(.5));
        Vec3 direction=owner.getLookAngle().normalize(),to=from.add(direction.scale(range));
        double suppressed=game.items().magic().suppressionFraction(owner.level(),from,to);
        if(Double.isFinite(suppressed)) to=from.lerp(to,suppressed);
        var hit=wild ? game.items().wands().firstHit(owner,from,to,entity->entity instanceof LivingEntity living && chainTarget(owner,living),true)
                : game.items().wands().firstHit(owner,from,to);
        Vec3 point=hit==null?to:hit.getLocation();
        if(items) {
            double nearest=from.distanceToSqr(point);
            for(var item:owner.level().getEntitiesOfClass(ItemEntity.class,new AABB(from,to).inflate(1),e->target(owner,e))) {
                var clipped=item.getBoundingBox().inflate(.35).clip(from,to);
                if(clipped.isPresent() && clipped.get().distanceToSqr(from)<nearest) { point=clipped.get(); nearest=point.distanceToSqr(from); }
            }
        }
        double distance=from.distanceTo(point);
        for(double d=0;d<=distance;d+=items?.35:.5) {
            Vec3 p=from.add(direction.scale(d));
            if(!owner.level().hasChunkAt(BlockPos.containing(p))) return null;
            double spread=particle==REFORGE_DUST?0:particle==ParticleTypes.END_ROD?.3:.1;
            owner.level().sendParticles(particle,particle!=ParticleTypes.END_ROD&&particle!=REFORGE_DUST,false,p.x,p.y,p.z,1,spread,spread,spread,0);
            if(particle==ParticleTypes.CLOUD) owner.level().sendParticles(ParticleTypes.SCULK_SOUL,p.x,p.y,p.z,1,.1,.1,.1,0);
        }
        if(Double.isFinite(suppressed) && distance>=from.distanceTo(to)-1e-6) return null;
        if(hit==null && !items || !owner.level().hasChunkAt(BlockPos.containing(point))) return null;
        return point;
    }
    void libraryChainArea(ServerPlayer owner,Vec3 at,double radius){
        var chain=new Chain(owner,at,game.items().epoch());chain.current.addAll(chainTargets(owner,at,radius));
        if(!chain.current.isEmpty())chains.add(chain);
    }
    void libraryDonor(ServerPlayer owner,int mode){
        donor(owner,InteractionHand.MAIN_HAND,ItemStack.EMPTY,mode,mode!=1);
        LbcOverload.sound(owner,"minecraft:entity.lightning_bolt.thunder",1,2,0);
    }
    private void donor(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,boolean upgraded) {
        Vec3 at=ray(owner,hand,mode==2?7:100,mode==2?REFORGE_DUST:ParticleTypes.END_ROD,mode==2,mode==3);
        if(at==null) return;
        if(mode==2) { reforge(owner,at); return; }
        game.items().electro().lightning(owner,at);
        if(mode==3) {
            var chain=new Chain(owner,at,game.items().epoch());
            chain.current.addAll(chainTargets(owner,at,3));
            if(!chain.current.isEmpty()) chains.add(chain);
        } else {
            double radius=upgraded?5:3;
            for(var entity:owner.level().getEntities(owner,new AABB(at,at).inflate(radius),e->projectileTarget(owner,e)
                    && !(e instanceof net.minecraft.world.entity.LightningBolt) && e.position().distanceToSqr(at)<=radius*radius)) {
                game.items().electro().lightning(owner,entity.position());
                if(!entity.onGround()) game.items().mana().award(owner,"lbc:true_advancements/staffs/lightning");
            }
            if(!upgraded && LbcAcademy.trial(stack)) {
                long count=owner.level().getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(2),
                        item->target(owner,item) && item.position().distanceToSqr(at)<=4 && transformable(item.getItem())).size();
                if(count>=4) game.items().academy().ready(owner,hand,stack);
            }
        }
    }
    private static boolean transformable(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("transformable",0)==1;
    }
    /** Ordinary Infinite Bolt is a free, short ray; living entities never terminate it. */
    public boolean useInfbolt(ServerPlayer owner,InteractionHand hand,ItemStack stack) {
        if(!valid(owner) || castBlocked(owner) || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        Vec3 from=owner.getEyePosition(),direction=owner.getLookAngle().normalize();
        Vec3 far=from.add(direction.scale(7));
        double suppression=game.items().magic().suppressionFraction(owner.level(),from,far);
        Vec3 limit=Double.isFinite(suppression)?from.lerp(far,suppression):far;
        var wall=LbcTerra.wallHit(owner.level(),from,limit,state->!state.isAir());
        Vec3 end=wall==null?limit:wall.getLocation();
        var item=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(owner.level(),owner,
                from,end,new AABB(from,end).inflate(1),entity->entity instanceof ItemEntity
                        && entity.isAlive() && target(owner,entity),0);
        Vec3 point=item==null?end:item.getLocation();
        boolean terminal=item!=null || wall!=null && owner.level().hasChunkAt(wall.getBlockPos());
        if(Double.isFinite(suppression) && point.distanceToSqr(from)>=limit.distanceToSqr(from)-1e-9) terminal=false;
        double distance=from.distanceTo(point);
        for(int sample=0;sample<=20 && sample*.35<=distance+1e-9;sample++) {
            Vec3 particle=from.add(direction.scale(sample*.35));
            if(!owner.level().hasChunkAt(BlockPos.containing(particle))) break;
            owner.level().sendParticles(INFBOLT_DUST,particle.x,particle.y,particle.z,1,.01,.01,.01,0);
        }
        if(terminal) {
            reforge(owner,point,true);
            LbcOverload.sound(owner,point,"minecraft:entity.firework_rocket.blast_far",1,1,0);
            LbcOverload.sound(owner,point,"minecraft:block.anvil.use",.4f,2,0);
            owner.level().sendParticles(ParticleTypes.END_ROD,point.x,point.y,point.z,30,.1,.1,.1,.25);
        }
        LbcOverload.sound(owner,"minecraft:entity.lightning_bolt.thunder",1,2,0);
        return true;
    }
    private void reforge(ServerPlayer owner,Vec3 at) {
        reforge(owner,at,false);
        owner.level().sendParticles(ParticleTypes.FIREWORK,at.x,at.y,at.z,20,.5,.5,.5,.1);
        LbcOverload.sound(owner,at,"minecraft:block.anvil.use",1,1,0);
    }
    private static boolean arrow64(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.ARROW) && stack.getCount()==64;
    }
    static List<String> reforgeTables(ItemStack stack,boolean ultra) {
        var result=new ArrayList<String>();
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        for(var rule:ultra?ULTRA_REFORGE:NORMAL_REFORGE) if(rule.matches(data)) result.add(rule.table());
        if(ultra && arrow64(stack)) result.add("magnum_ammo");
        return result;
    }
    enum ReforgeFamily { NORMAL, ULTRA, LIGHTNING }
    // Natural lightning's independent flags and order are distinct from the wand donor.
    private static final ReforgeRule[] LIGHTNING_REFORGE={
            new ReforgeRule("accessory","transformables_accessory"),
            new ReforgeRule("axe","transformables_axe"),
            new ReforgeRule("bow","transformables_bow"),
            new ReforgeRule("chestplate","transformables_chestplate"),
            new ReforgeRule("hoe","transformables_hoe"),
            new ReforgeRule("pickaxe","transformables_pickaxe"),
            new ReforgeRule("potion","transformables_potion"),
            new ReforgeRule("shield","transformables_shield"),
            new ReforgeRule("shooter","transformables_shooter"),
            new ReforgeRule("shovel","transformables_shovel"),
            new ReforgeRule("staff","transformables_staff"),
            new ReforgeRule("sword","transformables_sword"),
            new ReforgeRule("totem","transformables_totem"),
            new ReforgeRule("spellitem","transformables_spells"),
            new ReforgeRule("summoner","transformables_summons"),
            new ReforgeRule("boots","transformables_boots"),
            new ReforgeRule("leggings","transformables_leggings"),
            new ReforgeRule("helmet","transformables_helmet"),
            new ReforgeRule("trident","transformables_trident"),
            new ReforgeRule("arcanum","transformables_arcanum"),
            new ReforgeRule("metasola","halloween/metasola")};
    static List<String> lightningReforgeTables(ItemStack stack) {
        var result=new ArrayList<String>();
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        for(var rule:LIGHTNING_REFORGE) if(rule.matches(data)) result.add(rule.table());
        return result;
    }
    private static List<String> reforgeTables(ItemStack stack,ReforgeFamily family) {
        return family==ReforgeFamily.LIGHTNING?lightningReforgeTables(stack):reforgeTables(stack,family==ReforgeFamily.ULTRA);
    }
    /** One normalized loot bundle shared by dropped-item and committed crafting transactions. */
    List<ItemStack> rollReforge(Entity context,List<String> tables) {
        return rollReforge(context,context.position(),tables);
    }
    List<ItemStack> rollReforge(Entity context,Vec3 origin,List<String> tables) {
        var output=new ArrayList<ItemStack>();
        for(String table:tables)
            output.addAll(game.lbc().rollTable((ServerLevel)context.level(),origin,context,"lbc:"+table));
        output.removeIf(ItemStack::isEmpty);
        return output;
    }
    /** Exact donor, explicit unit budget and delivery context; never captures nearby inputs. */
    private ReforgeInput reforgeInput(ItemEntity item,ReforgeFamily family,int units,ServerPlayer recipient,
                                      java.util.function.Consumer<ItemStack> delivery) {
        if(units<=0 || reforgeTables(item.getItem(),family).isEmpty()) return null;
        return new ReforgeInput(item,family,units,recipient,delivery);
    }
    ReforgeInput lightningReforgeInput(ItemEntity item) {
        return reforgeInput(item,ReforgeFamily.LIGHTNING,item.getItem().getCount(),null,null);
    }
    final class ReforgeInput implements LbcStonekivi.Work {
        final ItemEntity item;
        final ItemStack input,snapshot;
        final ServerLevel level;
        final String epoch;
        final ReforgeFamily family;
        final ServerPlayer recipient;
        final java.util.function.Consumer<ItemStack> delivery;
        int remaining,units,converted;
        boolean convertedArrowStack;
        ReforgeInput(ItemEntity item,ReforgeFamily family,int units,ServerPlayer recipient,
                     java.util.function.Consumer<ItemStack> delivery) {
            this.item=item; this.input=item.getItem(); this.snapshot=input.copy(); this.remaining=input.getCount();
            this.level=(ServerLevel)item.level(); this.epoch=game.items().epoch(); this.family=family;
            this.units=units; this.recipient=recipient; this.delivery=delivery;
        }
        public boolean step() { return step(recipient,delivery); }
        boolean step(ServerPlayer awardRecipient,java.util.function.Consumer<ItemStack> sink) {
            if(item.isRemoved() || item.level()!=level || item.getItem()!=input || !epoch.equals(game.items().epoch())
                    || input.getCount()!=remaining || !ItemStack.isSameItemSameComponents(input,snapshot)
                    || !level.hasChunkAt(item.blockPosition())) return true;
            if(sink!=null && !game.items().lightningAcquisition().eligibleRecipient(awardRecipient,level)) return true;
            // Exact64 is retested each recursion: Ultra rolls ammo once, then consumes the other 63.
            var output=rollReforge(item,reforgeTables(input,family));
            if(output.isEmpty() && !convertedArrowStack) return true;
            if(family==ReforgeFamily.ULTRA && arrow64(input)) convertedArrowStack=true;
            Vec3 position=item.position();
            input.shrink(1); remaining--; units--; converted++;
            if(remaining==0) item.discard(); else item.setItem(input);
            for(var result:output) {
                if(sink!=null) sink.accept(result);
                else level.addFreshEntity(new ItemEntity(level,position.x,position.y,position.z,result));
            }
            if(awardRecipient!=null) game.items().mana().award(awardRecipient,"lbc:true_advancements/"
                    +(family==ReforgeFamily.ULTRA?"lightning_smith_ultra":"lightning_smith"));
            return remaining==0 || units==0;
        }
    }
    /** Capture inputs once so this cast never reforges the outputs it creates. */
    private void reforge(ServerPlayer owner,Vec3 at,boolean ultra) {
        var jobs=new ArrayDeque<LbcStonekivi.Work>();
        var state=new Owned(owner,at,game.items().epoch());
        for(var item:owner.level().getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(1),
                entity->entity.isAlive() && target(owner,entity) && entity.position().distanceToSqr(at)<=1
                        && (transformable(entity.getItem()) || ultra && arrow64(entity.getItem())))) {
            var input=reforgeInput(item,ultra?ReforgeFamily.ULTRA:ReforgeFamily.NORMAL,
                    item.getItem().getCount(),owner,null);
            if(input!=null) jobs.add(input);
        }
        if(jobs.isEmpty()) return;
        int generation=workGeneration,ownerGeneration=ownerWorkGeneration.getOrDefault(owner,0);
        var job=new LbcStonekivi.Work() {
            public boolean step() {
                if(!valid(state) || generation!=workGeneration || ownerGeneration!=ownerWorkGeneration.getOrDefault(owner,0)) { cancel(); return true; }
                if(paused(owner)) return false;
                long deadline=System.nanoTime()+1_000_000;
                for(int units=0;units<8 && !jobs.isEmpty() && System.nanoTime()<deadline;units++)
                    if(jobs.peek().step()) jobs.remove();
                if(jobs.isEmpty()) { reforgeJobs.remove(this); return true; }
                return false;
            }
            public void cancel() { jobs.clear(); reforgeJobs.remove(this); }
        };
        reforgeJobs.put(job,owner);
        game.items().stonekivi().queuedWork(job);
    }
    int pendingReforgeCount() { return reforgeJobs.size(); }
    void flushReforgeJobs() { game.items().stonekivi().tick(); }
    private void terminal(ServerPlayer owner,InteractionHand hand,ItemStack stack,Vec3 at) {
        game.items().electro().lightning(owner,at);
        for(var offset:OFFSETS) bolts.add(new Bolt(owner,at.add(offset),game.items().epoch()));
        for(var target:projectileTargets(owner,at,5)) {
            game.items().electro().shock(target,owner,10);
            if(LbcAcademy.trial(stack) && game.items().electro().shocks(target)>=60) game.items().academy().ready(owner,hand,stack);
        }
        game.server().setWeatherParameters(0,1200,true,true);
    }
    /** Field of Lightning shares Boltbolt's steering, strikes and lifecycle. */
    void homingField(ServerPlayer owner,int lifetime) {
        Vec3 origin=owner.position();
        String epoch=game.items().epoch();
        for(int i=0;i<20;i++) {
            Vec3 at=origin.add(owner.getRandom().nextInt(201)-100,0,owner.getRandom().nextInt(201)-100);
            bolts.add(new Bolt(owner,at,epoch,lifetime,20));
        }
    }
    private void watering(ServerPlayer owner) {
        owner.addEffect(ItemEffectLevels.create(MobEffects.WATER_BREATHING,4800,0));
        owner.addEffect(ItemEffectLevels.create(MobEffects.STRENGTH,4800,2));
        waters.add(new Water(owner,owner.position(),game.items().epoch()));
        for(int i=0;i<4;i++) waters.add(new Water(owner,owner.position().add(OFFSETS[i].scale(.5)),game.items().epoch()));
    }
    private void meteor(ServerPlayer owner,Vec3 at) {
        if(!owner.level().hasChunkAt(BlockPos.containing(at))) return;
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());
        display.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess(),METEOR_PRESET.copy()));
        display.setPos(at); display.entityTags().add("skywars2.boltbolt.meteor"); game.items().markCurrentItemEpoch(display);
        var meteor=new Meteor(owner,at,game.items().epoch(),display); meteors.add(meteor);
        if(!owner.level().addFreshEntity(display)) { meteors.remove(meteor); display.discard(); }
        LbcOverload.sound(owner,at,"minecraft:entity.lightning_bolt.thunder",19,0,0);
    }
    void loaded(Entity entity) {
        if(entity instanceof LivingEntity living)chainStatus.loaded(living);
        if(entity.entityTags().contains("skywars2.boltbolt.meteor")
                && meteors.stream().noneMatch(meteor->meteor.display==entity)) entity.discard();
    }
    void unloaded(Entity entity) {
        for(var meteor:List.copyOf(meteors)) if(meteor.display==entity) {
            meteors.remove(meteor);
            if(!entity.isRemoved()) entity.discard();
        }
        if(entity instanceof LivingEntity living){chainStatus.unloaded(living);for(var chain:chains)chain.current.remove(living);chains.removeIf(chain->chain.current.isEmpty());}
    }
    public void clearEffects(LivingEntity target) {
        chainStatus.clear(target);
        for(var chain:chains) chain.current.remove(target);
        chains.removeIf(chain->chain.current.isEmpty());
    }
    public void forget(ServerPlayer owner) {
        chainStatus.forget(owner);
        ownerWorkGeneration.merge(owner,1,Integer::sum);
        for(var entry:List.copyOf(reforgeJobs.entrySet())) if(entry.getValue()==owner) entry.getKey().cancel();
        bolts.removeIf(state->state.owner==owner); waters.removeIf(state->state.owner==owner);
        chains.removeIf(state->state.owner==owner); clearEffects(owner);
        for(var state:List.copyOf(meteors)) if(state.owner==owner) {
            meteors.remove(state);
            if(!state.display.isRemoved()) state.display.discard();
        }
    }
    public void stop() {
        chainStatus.stop();
        workGeneration++; ownerWorkGeneration.clear();
        for(var job:List.copyOf(reforgeJobs.keySet())) job.cancel();
        bolts.clear(); waters.clear(); chains.clear();
        for(var meteor:List.copyOf(meteors)) {
            meteors.remove(meteor);
            if(!meteor.display.isRemoved()) meteor.display.discard();
        }
    }
    public void tick() {
        if(game.stage()==GameController.Stage.LOADING || game.stage()==GameController.Stage.ENDING) { stop(); return; }
        if(game.items().wands().timeStopped()) return;
        ticks++;chainStatus.tick();
        for(var bolt:List.copyOf(bolts)) {
            if(!valid(bolt) || !bolt.level.hasChunkAt(BlockPos.containing(bolt.position))
                    || game.items().magic().suppressedAt(bolt.level,bolt.position)) { bolts.remove(bolt); continue; }
            if(paused(bolt.owner)) continue;
            if(++bolt.age>bolt.lifetime) { bolts.remove(bolt); continue; }
            {
                var nearby=targets(bolt.owner,bolt.position,25);
                boolean near=nearby.stream().anyMatch(target->target.position().distanceToSqr(bolt.position)<=225);
                if(near) bolt.position=nearby.get(bolt.owner.getRandom().nextInt(nearby.size())).position();
                else {
                    LivingEntity nearest=null; double distance=Double.POSITIVE_INFINITY;
                    for(var target:game.items().lifetimes().loadedTargets()) if(target.level()==bolt.level && target(bolt.owner,target)) {
                        double d=target.position().distanceToSqr(bolt.position); if(d<distance) { nearest=target; distance=d; }
                    }
                    if(nearest!=null) bolt.position=bolt.position.add(nearest.getEyePosition().subtract(bolt.position).normalize().scale(.5));
                }
                if(bolt.age%bolt.strikeInterval==0) game.items().electro().lightning(bolt.owner,bolt.position);
            }
            if(bolt.age>=bolt.lifetime) bolts.remove(bolt);
            bolt.level.sendParticles(ParticleTypes.CLOUD,true,false,bolt.position.x,bolt.position.y,bolt.position.z,1,0,0,0,0);
            bolt.level.sendParticles(ParticleTypes.SCULK_SOUL,bolt.position.x,bolt.position.y,bolt.position.z,1,0,0,0,0);
        }
        tickChains(); tickWater(); tickMeteors();
    }
    private void tickChains() {
        for(var chain:List.copyOf(chains)) {
            if(!valid(chain)) { chains.remove(chain); continue; }
            chain.current.removeIf(target->!chainTarget(chain.owner,target));
            if(chain.current.isEmpty()) { chains.remove(chain); continue; }
            if(paused(chain.owner)) continue;
            if(ticks%5==0) for(var target:chain.current) chain.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,target.getX(),target.getY()+1,target.getZ(),3,.3,.5,.3,0);
            if(--chain.untilNext>0) continue; chain.untilNext=40;
            Set<LivingEntity> next=Collections.newSetFromMap(new IdentityHashMap<>());
            for(var target:List.copyOf(chain.current)) {
                game.items().electro().lightning(chain.owner,target.position());
                for(var nearby:chainTargets(chain.owner,target.position(),16))
                    if(nearby!=target && chainTarget(chain.owner,nearby)
                            && nearby.position().distanceToSqr(target.position())>=.000001) next.add(nearby);
            }
            chain.current=next;
            if(next.isEmpty()) chains.remove(chain);
        }
    }
    private void tickWater() {
        for(var water:List.copyOf(waters)) {
            if(!valid(water) || water.age>=200) { waters.remove(water); continue; }
            if(paused(water.owner)) continue;
            water.age++; if(water.age%2!=0) continue;
            BlockPos at=BlockPos.containing(water.position);
            var work=new BlockAreaWork(water.level,state->state.isAir(),(pos,state)->Blocks.WATER.defaultBlockState(),3);
            work.move(new BlockAreaWork.Bounds(at.getX()-2,at.getY()-1,at.getZ()-2,at.getX()+2,at.getY()+1,at.getZ()+2),true);
            queue(water,work);
            water.position=Vec3.atLowerCornerOf(at.offset(water.owner.getRandom().nextInt(15)-7,water.owner.getRandom().nextInt(15)-7,water.owner.getRandom().nextInt(15)-7));
            if(water.age>=200) waters.remove(water);
        }
    }
    private void tickMeteors() {
        for(var meteor:List.copyOf(meteors)) {
            if(!valid(meteor) || meteor.display.isRemoved() || !meteor.level.hasChunkAt(BlockPos.containing(meteor.position))) {
                meteors.remove(meteor); meteor.display.discard(); continue;
            }
            if(paused(meteor.owner)) continue;
            if(meteor.impactVisualTicks>0) {
                if(--meteor.impactVisualTicks==0) { meteors.remove(meteor); meteor.display.discard(); }
                continue;
            }
            meteor.age++;
            // The first legacy 2-tick callback only initializes. Its relative tp commands
            // share one execution origin: the last matching offset wins, they never sum.
            if(meteor.age<=2 || meteor.age%2!=0) continue;
            double fall=.5+.5*Math.min(meteor.age/5,5);
            Vec3 next=meteor.position.add(0,-fall,0);
            // Source model spans -16..32 pixels: scale3 gives a 4.5-block body radius
            // before spin. Sweep the lower centerline to contact rather than exploding
            // from a block sampled five blocks ahead of an undersized display.
            final double radius=4.5;
            // Include exact endpoint contact: native shape clipping otherwise waits until inside.
            var hit=LbcTerra.wallHit(meteor.level,meteor.position.add(0,-radius,0),next.add(0,-radius-1e-6,0));
            boolean impact=hit!=null && meteor.level.hasChunkAt(hit.getBlockPos());
            if(impact) next=new Vec3(next.x,hit.getLocation().y+radius,next.z);
            meteor.position=next; meteor.display.setPos(next); meteor.display.setYRot(meteor.display.getYRot()+6); meteor.display.setXRot(meteor.display.getXRot()+6);
            meteor.display.needsSync=true;
            meteor.level.sendParticles(ParticleTypes.SNOWFLAKE,next.x,next.y,next.z,20,2,2,2,.5);
            if(impact) {
                explode(meteor);
                // Let the client's two-tick interpolation reach the contact point before removal.
                meteor.impactVisualTicks=2;
            } else if(hit!=null || next.y<meteor.level.getMinY()-16) { meteors.remove(meteor); meteor.display.discard(); }
        }
    }
    private void explode(Meteor meteor) {
        var owner=meteor.owner; Vec3 at=meteor.position; BlockPos center=BlockPos.containing(at);
        meteor.level.sendParticles(ParticleTypes.SNOWFLAKE,at.x,at.y,at.z,1000,3,3,3,1.5);
        for(var target:projectileTargets(owner,at,10)) {
            target.hurtServer(meteor.level,new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FREEZE),owner),35);
            for(int i=0;i<5;i++) game.items().frost().add(target,owner);
        }
        var jobs=new ArrayDeque<LbcStonekivi.Work>();
        jobs.add(template(meteor,AIR,center.offset(-5,-6,-5)));
        int x=center.getX(),y=center.getY(),z=center.getZ();
        // The three overlapping source fills form one cross-shaped volume; inspect every cell once.
        var snow=new BlockAreaWork(meteor.level,state->state.is(SNOW_REPLACE),(pos,state)->
                Math.abs(pos.getX()-x)<=7 || Math.abs(pos.getY()-y)<=7 || Math.abs(pos.getZ()-z)<=7
                        ? Blocks.SNOW_BLOCK.defaultBlockState() : null,3);
        snow.move(new BlockAreaWork.Bounds(x-10,y-10,z-10,x+10,y+10,z+10),true);
        jobs.add(blockWork(meteor,snow)); jobs.add(template(meteor,SNOW,center.offset(-3,-7,-3)));
        game.items().stonekivi().queuedWork(sequence(meteor,jobs));
        for(float pitch:new float[]{0,1,2}) LbcOverload.sound(owner,at,"minecraft:block.snow.fall",2,pitch,0);
        LbcOverload.sound(owner,at,"minecraft:entity.dragon_fireball.explode",2,0,0);
    }
    private void queue(Owned state,BlockAreaWork work) { game.items().stonekivi().queuedWork(blockWork(state,work)); }
    private LbcStonekivi.Work blockWork(Owned state,BlockAreaWork work) {
        int generation=workGeneration,ownerGeneration=ownerWorkGeneration.getOrDefault(state.owner,0);
        return new LbcStonekivi.Work() {
            public boolean step() {
                if(!valid(state) || generation!=workGeneration || ownerGeneration!=ownerWorkGeneration.getOrDefault(state.owner,0)) { work.clear(); return true; }
                if(paused(state.owner)) return false;
                work.step(game,state.owner,512,System.nanoTime()+1_000_000); return !work.hasWork();
            }
            public void cancel() { work.clear(); }
        };
    }
    private LbcStonekivi.Work sequence(Owned state,ArrayDeque<LbcStonekivi.Work> jobs) {
        int generation=workGeneration,ownerGeneration=ownerWorkGeneration.getOrDefault(state.owner,0);
        return new LbcStonekivi.Work() {
            public boolean step() {
                if(!valid(state) || generation!=workGeneration || ownerGeneration!=ownerWorkGeneration.getOrDefault(state.owner,0)) { cancel(); return true; }
                if(paused(state.owner)) return false;
                if(!jobs.isEmpty() && jobs.peek().step()) jobs.remove(); return jobs.isEmpty();
            }
            public void cancel() { jobs.forEach(LbcStonekivi.Work::cancel); jobs.clear(); }
        };
    }
    private LbcStonekivi.Work template(Owned state,Identifier id,BlockPos origin) {
        var jobs=new ArrayDeque<LbcStonekivi.Work>(); long seed=state.owner.getRandom().nextLong();
        for(var part:templates.getOrDefault(id,List.of())) {
            var b=part.bounds(); int minX=origin.getX()+b.minX(),maxX=origin.getX()+b.maxX(),minZ=origin.getZ()+b.minZ(),maxZ=origin.getZ()+b.maxZ();
            int minY=Math.max(state.level.getMinY(),origin.getY()+b.minY()),maxY=Math.min(state.level.getMaxY(),origin.getY()+b.maxY());
            if(minY>maxY) continue;
            for(int cx=minX>>4;cx<=maxX>>4;cx++) for(int cz=minZ>>4;cz<=maxZ>>4;cz++) {
                var bounds=new BoundingBox(Math.max(minX,cx*16),minY,Math.max(minZ,cz*16),Math.min(maxX,cx*16+15),maxY,Math.min(maxZ,cz*16+15));
                jobs.add(()-> {
                    if(!valid(state) || !state.level.hasChunkAt(new BlockPos(bounds.minX(),minY,bounds.minZ()))) return true;
                    game.items().indirectKills().trapChanges(state.owner,()->game.maps().placeMapSlice(state.level,part.template(),origin,
                            new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true).setBoundingBox(bounds),seed)); return true;
                });
            }
        }
        return sequence(state,jobs);
    }
}
