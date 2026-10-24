package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Native ownership/reflection and source prism samples; immediate beams share a per-tick CPU budget. */
public final class LbcPrism {
    static final EntityType<Charge> TYPE=registerType();
    private static final TagKey<Block> AIRS=tag("airs"),GLASS=tag("glasses_2"),PASS=tag("airs_or_glass");
    private static final DustColorTransitionOptions UNSTABLE=new DustColorTransitionOptions(0x00b266,0x00ffff,2);
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final List<Color> COLORS=colors();
    private record Color(TagKey<Block> tag,DustColorTransitionOptions particle){}
    private static TagKey<Block> tag(String name){return TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:"+name));}
    private static List<Color> colors(){
        try(var stream=LbcPrism.class.getResourceAsStream("/skywars2/prism_colors.json")){
            var list=new ArrayList<Color>();
            for(var row:com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray()){
                var c=row.getAsJsonObject();list.add(new Color(tag(c.get("tag").getAsString()),new DustColorTransitionOptions(c.get("from").getAsInt(),c.get("to").getAsInt(),2)));
            }return List.copyOf(list);
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    static void register(){}
    private static EntityType<Charge> registerType(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:prism_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(4).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);return type;
    }
    public static final class Charge extends Projectile implements PolymerEntity {
        enum Phase { UNSTABLE,STABLE,ACADEMY }
        Phase phase=Phase.UNSTABLE;
        int age,steps,bounces;Vec3 origin=Vec3.ZERO;
        InteractionHand hand;ItemStack stack;UUID caster;
        DustColorTransitionOptions particle=UNSTABLE;
        final Set<UUID> hit=new HashSet<>();
        Charge(EntityType<? extends Charge> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushedByFluid(){return false;}
        @Override public void tick(){
            var game=SkyWars2.game();if(game==null){discard();return;}
            if(game.items().timeStop().frozen(this))return;
            baseTick();game.items().prism().flight(this);
        }
    }
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final ArrayDeque<Charge> immediate=new ArrayDeque<>();
    private int budgetTick=Integer.MIN_VALUE;
    private long spent;
    LbcPrism(GameController game){this.game=game;}
    List<Charge> snapshot(){return List.copyOf(charges);}
    int pendingRays(){return immediate.size();}
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("unstable_prism_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        var charge=new Charge(TYPE,owner.level());charge.setOwner(owner);charge.hand=hand;charge.stack=stack;charge.caster=owner.getUUID();
        charge.origin=owner.getEyePosition().add(owner.getLookAngle().scale(hand==InteractionHand.OFF_HAND?.5:0));charge.setPos(charge.origin);charge.setDeltaMovement(owner.getLookAngle());
        charge.entityTags().addAll(List.of("prism","player","slowraycast","magic",game.items().epoch()));
        if(academy){
            charge.phase=Charge.Phase.ACADEMY;var mana=game.items().mana();int h=Math.floorMod(mana.score(owner,"h").get()+20,360);
            mana.score(owner,"h").set(h);mana.score(owner,"s").set(80);mana.score(owner,"v").set(100);
            charge.particle=new DustColorTransitionOptions(Mth.hsvToRgb(h/360f,.8f,1),0xffffff,2);
        }else charge.entityTags().add("shield_blockable");
        game.items().timeStop().prepareFrozen(charge);
        if(owner.level().addFreshEntity(charge)&&academy)immediate.addLast(charge);
        game.items().wands().wandCasted(owner);pump();return true;
    }
    private boolean active(Charge c){return !c.isRemoved()&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&c.level()==game.maps().level()&&game.items().currentEpoch(c)&&c.getOwner() instanceof ServerPlayer owner
            &&owner.isAlive()&&!owner.isRemoved()&&game.canUseGameplayItems(owner)
            &&c.level().hasChunkAt(c.blockPosition())&&!game.items().magic().suppressedAt((ServerLevel)c.level(),c.position());}
    private boolean paused(Charge c){return game.items().timeStop().stopped((ServerLevel)c.level())||game.items().timeStop().frozen(c);}
    private Vec3 direction(Charge c){return c.getDeltaMovement().lengthSqr()<1e-12?Vec3.directionFromRotation(c.getXRot(),c.getYRot()):c.getDeltaMovement().normalize();}
    private void rotate(Charge c,float yaw,float pitch){
        var d=direction(c);float y=(float)Math.toDegrees(Math.atan2(-d.x,d.z))+yaw;
        float p=(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance()))+pitch;
        c.setDeltaMovement(Vec3.directionFromRotation(p,y));LbcShooters.orient(c);
    }
    void flight(Charge c){
        if(!active(c)){c.discard();return;}if(paused(c)||c.phase!=Charge.Phase.UNSTABLE)return;
        if(++c.age>=150){c.discard();return;}
        for(int step=0;step<9&&!c.isRemoved();step++){
            if(!active(c)){c.discard();return;}
            var level=(ServerLevel)c.level();var point=c.position();var state=level.getBlockState(c.blockPosition());
            if(state.is(GLASS)){
                c.phase=Charge.Phase.STABLE;c.steps=0;c.particle=palette(state);immediate.addLast(c);pump();return;
            }
            sample(c);damage(c);if(c.isRemoved())return;
            if(level.getRandom().nextFloat()<.00125f){crystal(c);for(int i=0;i<6;i++)if(level.getRandom().nextBoolean())rotate(c,i<2||i>3?(i%2==0?33:-33):0,i==2?33:i==3?-33:0);}
            // Preserve the source six near-wall checks and 45 degree deflections.
            if(step<6){int[][] offsets={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
                for(int[] o:offsets){var pos=BlockPos.containing(point.add(o[0]*.26,o[1]*.26,o[2]*.26));
                    if(level.hasChunkAt(pos)&&!level.getBlockState(pos).is(PASS))rotate(c,o[1]==0?(o[0]+o[2])*45:0,o[1]*45);
                }
            }
            erode(c,.15f);
            if(!level.getBlockState(c.blockPosition()).is(AIRS))return;
            if(step<8&&!move(c,.25,false))return;
        }
    }
    private DustColorTransitionOptions palette(BlockState state){
        DustColorTransitionOptions selected=new DustColorTransitionOptions(0xffffff,0xd2ffff,2);
        for(var color:COLORS)if(state.is(color.tag))selected=color.particle;
        if(state.is(net.minecraft.world.level.block.Blocks.TINTED_GLASS))selected=new DustColorTransitionOptions(0x040404,0,2);
        return selected;
    }
    private void sample(Charge c){var p=c.position();var level=(ServerLevel)c.level();
        level.sendParticles(c.particle,true,false,p.x,p.y,p.z,1,c.phase==Charge.Phase.ACADEMY?0:.1,c.phase==Charge.Phase.ACADEMY?0:.1,c.phase==Charge.Phase.ACADEMY?0:.1,.01);
        if(c.phase!=Charge.Phase.ACADEMY)LbcOverload.sound((ServerPlayer)c.getOwner(),p,"minecraft:item.trident.return",.1f,0,0);
        else if(level.getRandom().nextFloat()<.1f)LbcOverload.sound((ServerPlayer)c.getOwner(),p,"minecraft:block.amethyst_cluster.break",.3f,1,0);
    }
    private void damage(Charge c){
        var owner=(ServerPlayer)c.getOwner();var p=c.position();var level=(ServerLevel)c.level();
        var source=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(c.phase==Charge.Phase.ACADEMY?DamageTypes.GENERIC:DamageTypes.CACTUS),c,owner);
        int count=0,limit=c.phase==Charge.Phase.ACADEMY?1:2;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(p,p.add(1,1,1)),e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,e)
                &&e.position().distanceToSqr(p)<=225&&!game.items().magic().suppressedAt(level,e.position()))){
            if(c.phase==Charge.Phase.ACADEMY&&!c.hit.add(target.getUUID()))continue;
            boolean applied=target.hurtServer(level,source,c.phase==Charge.Phase.ACADEMY?7:8);
            if(applied&&c.phase==Charge.Phase.STABLE)game.items().mana().award(owner,"lbc:true_advancements/staffs/prism");
            if(applied&&c.phase!=Charge.Phase.ACADEMY&&c.caster.equals(owner.getUUID())&&owner.getItemInHand(c.hand)==c.stack&&LbcAcademy.trial(c.stack))trial(c,owner);
            if(++count>=limit)break;
        }
    }
    private void trial(Charge c,ServerPlayer owner){
        if(c.position().distanceToSqr(c.origin)>75*75)game.items().academy().ready(owner,c.hand,c.stack);
    }
    private void erode(Charge c,float chance){
        var level=(ServerLevel)c.level();var center=c.blockPosition();var owner=(ServerPlayer)c.getOwner();
        game.items().indirectKills().trapChanges(level,owner,false,()->{
            for(var pos:List.of(center,center.east(),center.west(),center.above(),center.below(),center.south(),center.north())){
                if(level.getRandom().nextFloat()<chance&&level.hasChunkAt(pos)&&!level.getBlockState(pos).is(GLASS))level.destroyBlock(pos,true,owner);
            }
        });
    }
    void crystal(Charge c){
        var level=(ServerLevel)c.level();var crystal=new EndCrystal(EntityTypes.END_CRYSTAL,level);
        crystal.setPos(c.position().add(0,-.5,0));crystal.setShowBottom(false);
        // Reuse the existing native actor/child/explosion owner, including after crystal removal.
        game.items().mobgun().registerChild((ServerPlayer)c.getOwner(),crystal);
        game.items().timeStop().prepareFrozen(crystal);if(!level.addFreshEntity(crystal))crystal.discard();
    }
    private boolean move(Charge c,double distance,boolean bounce){
        Vec3 from=c.position(),d=direction(c),to=from.add(d.scale(distance));var level=(ServerLevel)c.level();
        if(!level.hasChunkAt(BlockPos.containing(to))){c.discard();return false;}
        if(c.phase!=Charge.Phase.ACADEMY){var shield=game.items().wands().shieldHit((ServerPlayer)c.getOwner(),from,to);
            if(shield!=null){LbcOverload.sound((ServerPlayer)c.getOwner(),shield.getLocation(),"minecraft:item.shield.block",1,1,0);c.discard();return false;}}
        if(game.items().magic().suppressedAt(level,to)){c.discard();return false;}
        if(bounce&&!level.getBlockState(BlockPos.containing(to)).is(AIRS)){
            // Native block face supplies an unambiguous normal at corners/negative coordinates.
            var wall=LbcTerra.wallHit(level,from,to,s->!s.is(AIRS));
            if(wall==null){c.discard();return false;}
            var face=wall.getDirection();var n=new Vec3(face.getStepX(),face.getStepY(),face.getStepZ());
            c.setDeltaMovement(d.subtract(n.scale(2*d.dot(n))).normalize());c.hit.clear();c.bounces++;
            c.setPos(wall.getLocation().add(n.scale(.001)));return true;
        }
        c.setPos(to);return true;
    }
    private void instantStep(Charge c){
        sample(c);damage(c);if(c.isRemoved())return;
        if(c.phase==Charge.Phase.STABLE){
            erode(c,.35f);
            if(++c.steps>=451||!((ServerLevel)c.level()).getBlockState(c.blockPosition()).is(PASS)){c.discard();return;}
            move(c,.33,false);
        }else{
            if(++c.steps>=301){c.discard();return;}move(c,.3,true);
        }
    }
    /** No queue cap: every surviving cast completes; the same budget covers casts and end-of-tick work. */
    private void pump(){
        int tick=game.server().getTickCount();if(budgetTick!=tick){budgetTick=tick;spent=0;}
        if(spent>=3_000_000)return;long start=System.nanoTime(),deadline=start+3_000_000-spent;
        try{while(!immediate.isEmpty()&&System.nanoTime()<deadline){
            int round=immediate.size();boolean progressed=false;
            while(round-->0&&!immediate.isEmpty()&&System.nanoTime()<deadline){
                var c=immediate.removeFirst();if(!active(c)){c.discard();continue;}
                if(paused(c)){immediate.addLast(c);continue;}
                progressed=true;for(int i=0;i<8&&!c.isRemoved()&&System.nanoTime()<deadline;i++){
                    if(!active(c)){c.discard();break;}instantStep(c);
                }
                if(!c.isRemoved())immediate.addLast(c);
            }
            if(!progressed)break;
        }}finally{spent+=System.nanoTime()-start;}
    }
    void tick(){pump();}
    void loaded(Entity e){if(e instanceof Charge c){if(game.playing()&&game.items().currentEpoch(c))charges.add(c);else c.discard();}}
    void unloaded(Entity e){if(e instanceof Charge c){charges.remove(c);immediate.remove(c);}}
    void forget(ServerPlayer p){for(var c:List.copyOf(charges))if(c.getOwner()==p)c.discard();}
    void stop(){var old=List.copyOf(charges);charges.clear();immediate.clear();old.forEach(Entity::discard);spent=0;}
}
