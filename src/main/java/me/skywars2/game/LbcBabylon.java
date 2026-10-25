package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Held Gate of Babylon: each funded hand emits one independent native projectile every two ticks. */
public final class LbcBabylon {
    public static final String PROJECTILE="skywars2.babylon";
    private static final ThreadLocal<Projectile> EFFECT_ARROW=new ThreadLocal<>();
    private static final CompoundTag FLAG=flag();
    public static final Item ITEM=registerItem();
    private static final double[][] ORIGINS={{2.5,2,0},{-2.5,2,0},{0,3,0},{4,0,0},{-4,0,0}};
    private static final double[][] MOTIONS={{-.2,.2,3},{.2,.2,3},{0,0,3},{-.5,.3,3},{.5,.3,3}};
    private static final DustColorTransitionOptions SPARK=new DustColorTransitionOptions(0xffff00,0xffffff,1);
    private static final CompoundTag[] ARROWS={arrow(16746496,101),arrow(16777215,109),arrow(16770560,110)};
    private static final CompoundTag ROCKET=parse("{LifeTime:100,ShotAtAngle:1b,FireworksItem:{id:\"minecraft:firework_rocket\",components:{\"minecraft:fireworks\":{explosions:[{shape:\"small_ball\",colors:[16754176],fade_colors:[7031040],has_trail:1b,has_twinkle:1b},{shape:\"large_ball\",colors:[16743168],fade_colors:[0],has_trail:1b,has_twinkle:1b},{shape:\"star\",colors:[16775936],fade_colors:[7955200],has_trail:1b,has_twinkle:1b},{shape:\"burst\",colors:[16748800],fade_colors:[8932608],has_trail:1b,has_twinkle:1b}]}},count:1}}");
    private final GameController game;
    private final LbcMana mana;
    private final Set<Projectile> projectiles=new LinkedHashSet<>();

    LbcBabylon(GameController game,LbcMana mana){
        this.game=game;this.mana=mana;
        // Construction precedes LbcItems assignment; only the supplied mana service is safe here.
        mana.score(ScoreHolder.forNameOnly("babylon_gate_mana_tick"),"lbc.math").set(300);
        mana.score(ScoreHolder.forNameOnly("babylon_gate_mana_tick_sec"),"lbc.math").set(3000);
    }
    private static CompoundTag flag(){var tag=new CompoundTag();tag.putInt("gate_of_babylon",1);return tag;}
    private static Item registerItem(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:babylon_gate"));
        var model=Identifier.parse("lbc:babylongate");
        var data=flag();data.putInt("transformable",1);data.putInt("arcanum",1);data.putInt("arcanum2t",1);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).overrideDescription("gate_of_babylon").component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_a"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.REPEATING_COMMAND_BLOCK,true));
    }
    public static void register(){}
    public static boolean isBabylon(ItemStack stack){
        if(stack.isEmpty())return false;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(ITEM)||data!=null&&data.matchedBy(FLAG);
    }
    public static boolean managed(Entity entity){return entity instanceof Projectile&&entity.entityTags().contains(PROJECTILE);}
    /** Only the native potion application stack can present a Babylon arrow to effect ingress. */
    public static Projectile effectArrow(){return EFFECT_ARROW.get();}
    private boolean active(ServerPlayer owner){return owner.isAlive()&&mana.visible(owner)&&!game.isObserver(owner);}
    public void tick(){
        for(var shot:List.copyOf(projectiles))if(!valid(shot)||!shot.level().hasChunkAt(shot.blockPosition()))unloaded(shot);
        if(!game.server().tickRateManager().runsNormally()||(game.server().getTickCount()&1)!=0)return;
        for(var owner:game.server().getPlayerList().getPlayers())tickPlayer(owner);
    }
    void tickPlayer(ServerPlayer owner){
        if(!game.server().tickRateManager().runsNormally()||!active(owner)||mana.noMagic(owner)>0
                ||game.items().timeStop().frozen(owner)||game.items().timeStop().stopped(owner.level())
                ||owner.entityTags().contains("time_stopped")||game.items().frost().isFrozen(owner)
                ||game.items().magic().suppressed(owner))return;
        if(isBabylon(owner.getMainHandItem()))release(owner);
        if(isBabylon(owner.getOffhandItem()))release(owner);
    }
    private void release(ServerPlayer owner){
        int price=mana.score(ScoreHolder.forNameOnly("babylon_gate_mana_tick"),"lbc.math").get();
        if(price<0||!mana.trySpend(owner,price))return;
        // Independent values replace the source's shared #random5 cascade (author ruling).
        emit(owner,owner.getRandom().nextInt(5)+1,owner.getRandom().nextInt(5)+1);
    }
    /** One-based source indices; deterministic native fixture entry point, without spending mana. */
    Projectile emit(ServerPlayer owner,int originIndex,int kind){
        if(originIndex<1||originIndex>5||kind<1||kind>5)throw new IllegalArgumentException("Babylon origin/kind must be 1..5");
        Vec3 pos=owner.getEyePosition().add(local(owner,ORIGINS[originIndex-1]));
        if(!owner.level().hasChunkAt(BlockPos.containing(pos)))return null;
        Projectile shot=switch(kind){
            case 1,2,3->{var arrow=new BabylonArrow(owner.level(),kind==1?101:kind==2?109:110);load(arrow,ARROWS[kind-1]);yield arrow;}
            case 4->new BabylonTrident(owner.level());
            case 5->{var rocket=new BabylonRocket(owner.level());load(rocket,ROCKET);yield rocket;}
            default->throw new IllegalStateException();
        };
        Vec3 velocity=local(owner,MOTIONS[originIndex-1]);
        // Source data-get/store scales to milliblocks and floors each component independently.
        shot.setPos(pos);shot.setDeltaMovement(new Vec3(Mth.floor(velocity.x*1000)*.001,Mth.floor(velocity.y*1000)*.001,Mth.floor(velocity.z*1000)*.001));
        shot.setOwner(owner);LbcShooters.orient(shot);shot.entityTags().add(PROJECTILE);
        game.items().markCurrentItemEpoch(shot);
        if(!owner.level().addFreshEntity(shot))return null;
        projectiles.add(shot);
        owner.level().sendParticles(SPARK,true,false,pos.x,pos.y,pos.z,10,.1,.1,.1,.4);
        return shot;
    }
    private static Vec3 local(ServerPlayer owner,double[] coords){
        Vec3 forward=owner.getLookAngle(),left=Vec3.directionFromRotation(0,owner.getYRot()-90);
        Vec3 up=forward.cross(left);
        return left.scale(coords[0]).add(up.scale(coords[1])).add(forward.scale(coords[2]));
    }
    private static CompoundTag arrow(int color,int amplifier){return parse("{item:{id:\"minecraft:tipped_arrow\",components:{\"minecraft:potion_contents\":{custom_color:"+color+",custom_effects:[{id:\"minecraft:raid_omen\",amplifier:"+amplifier+",duration:20}]}},count:1}}");}
    private static CompoundTag parse(String snbt){try{return TagParser.parseCompoundFully(snbt);}catch(Exception error){throw new ExceptionInInitializerError(error);}}
    private static void load(Entity entity,CompoundTag tag){entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),tag));}
    private boolean valid(Projectile shot){return !shot.isRemoved()&&game.items().currentEpoch(shot)
            &&shot.getOwner() instanceof ServerPlayer owner&&active(owner)&&owner.level()==shot.level();}
    public boolean canAffect(Projectile shot,Entity target){return valid(shot)
            &&game.items().wands().projectileTarget((ServerPlayer)shot.getOwner(),target);}
    public boolean loaded(Entity entity){
        if(!managed(entity))return false;
        // Only this live service's native actors may reload; plain disk entities cannot revive them.
        if((entity instanceof BabylonArrow arrow&&!arrow.retired||entity instanceof BabylonTrident trident&&!trident.retired
                ||entity instanceof BabylonRocket rocket&&!rocket.retired)&&valid((Projectile)entity)){
            projectiles.add((Projectile)entity);return true;
        }
        entity.discard();return true;
    }
    public void unloaded(Entity entity){if(managed(entity)){
        if(entity instanceof BabylonArrow arrow)arrow.retired=true;
        if(entity instanceof BabylonTrident trident)trident.retired=true;
        if(entity instanceof BabylonRocket rocket)rocket.retired=true;
        projectiles.remove(entity);if(!entity.isRemoved())entity.discard();
    }}
    public void forget(ServerPlayer owner){for(var shot:List.copyOf(projectiles))if(shot.getOwner()==owner)unloaded(shot);}
    public void stop(){for(var shot:List.copyOf(projectiles))unloaded(shot);}
    public int count(){return projectiles.size();}
    List<Projectile> snapshot(){return List.copyOf(projectiles);}
    private final class BabylonArrow extends Arrow {
        boolean retired,blockEffectDone;final int transportAmp;
        BabylonArrow(ServerLevel level,int transportAmp){super(EntityTypes.ARROW,level);this.transportAmp=transportAmp;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void tick(){
            if(!valid(this)){unloaded(this);return;}
            if(!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped((ServerLevel)level())
                    ||game.items().timeStop().frozen(this)||entityTags().contains("time_stopped"))return;
            super.tick();
        }
        @Override protected void onHitBlock(net.minecraft.world.phys.BlockHitResult hit){
            super.onHitBlock(hit);
            // Legacy prein -> arrows_f acts once at the embedded arrow position,
            // then kills the transport even when a ward suppresses its consequence.
            if(blockEffectDone||retired||!valid(this))return;
            blockEffectDone=true;
            game.items().babylonEffects().blockImpact(this,transportAmp,position());
            unloaded(this);
        }
        @Override protected void doPostHurtEffects(LivingEntity target){
            var previous=EFFECT_ARROW.get();
            EFFECT_ARROW.set(this);
            try{super.doPostHurtEffects(target);}
            finally{if(previous==null)EFFECT_ARROW.remove();else EFFECT_ARROW.set(previous);}
        }
    }
    private final class BabylonTrident extends ThrownTrident {
        boolean retired;
        BabylonTrident(ServerLevel level){super(EntityTypes.TRIDENT,level);}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void tick(){if(!valid(this)){unloaded(this);return;}super.tick();}
    }
    private final class BabylonRocket extends FireworkRocketEntity {
        boolean retired;
        BabylonRocket(ServerLevel level){super(EntityTypes.FIREWORK_ROCKET,level);}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void tick(){if(!valid(this)){unloaded(this);return;}super.tick();}
    }
}
