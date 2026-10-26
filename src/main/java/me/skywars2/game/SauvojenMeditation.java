package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** Combined staff's destructive nine-stage trial. Every helper belongs to one owner/stack admission. */
public final class SauvojenMeditation {
    static final String TAG="skywars2.sauvojen.meditation";
    static final Identifier STONE=Identifier.parse("lbc:stone_cylinder");
    private static final TagKey<Block> AIRS=tag("airs"),TO_LIME=tag("to_lime");
    private static final int STRICT=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    enum Kind { TERRA,TERRA_RAY,WEB,AIR,ACID_RING,ACID_BALLER,ACID_BALL,SCARE }
    static final class Hazard {
        final Kind kind; Vec3 at,direction; int age; float yaw;
        Entity actor; UUID uuid;
        Hazard(Kind kind,Vec3 at,Vec3 direction){this.kind=kind;this.at=at;this.direction=direction;}
        void close(){if(actor!=null)actor.discard();actor=null;}
    }
    static final class Trial {
        final ServerPlayer owner; final ItemStack stack; final ServerLevel level; final String epoch;
        final LbcIndirectKills.Owner cause; final List<Hazard> hazards=new ArrayList<>();
        int remaining=620; int ticks;
        TerrainJob terrain;long terrainNanos,maxTerrainNanos;int terrainTurns;
        BlockPos stoneOrigin;int stoneRow;long stoneSeed;
        Trial(ServerPlayer p,ItemStack stack,String epoch,LbcIndirectKills.Owner cause){
            owner=p;this.stack=stack;level=p.level();this.epoch=epoch;this.cause=cause;
        }
    }
    private static final class TerrainJob {
        final int phase;final BlockPos center,lo,hi;int cursor;
        TerrainJob(int phase,BlockPos center){this.phase=phase;this.center=center;
            lo=center.offset(phase==620?-10:-60,phase==620?0:-50,phase==620?-10:-60);
            hi=center.offset(phase==620?10:60,phase==620?24:0,phase==620?10:60);}
        int size(){return phase==620?8725:63652;}
        BlockPos position(){
            if(phase==620){int column=cursor/25;return center.offset(RUBY_COLUMNS[column*2],cursor%25,RUBY_COLUMNS[column*2+1]);}
            if(cursor<20181)return center.offset(cursor%31-15,cursor/961-20,(cursor/31)%31-15);
            int i=cursor-20181;if(i<28830)return center.offset(i%31-15,i/961-50,(i/31)%31-15);
            i-=28830;return center.offset(i%121-60,-21,i/121-60);
        }
    }
    private static final int[] RUBY_COLUMNS=rubyColumns();
    private static int[] rubyColumns(){int[] cells=new int[349*2];int i=0;
        for(int x=-10;x<=10;x++)for(int z=-rubyHalfWidth(x);z<=rubyHalfWidth(x);z++){cells[i++]=x;cells[i++]=z;}return cells;}
    /** Native TNT keeps its author through direct damage, terrain changes and chain callbacks. */
    static final class TrialTnt extends PrimedTnt {
        final ServerPlayer author;final LbcIndirectKills.Owner cause;
        TrialTnt(Trial t,Vec3 at){super(t.level,at.x,at.y,at.z,t.owner);author=t.owner;cause=t.cause;
            setFuse(0);setDeltaMovement(Vec3.ZERO);entityTags().add(LbcDamage.UNPROTECTED_TNT);entityTags().add(t.epoch);}
        @Override public LivingEntity getOwner(){return author;}
    }
    public static boolean managed(Entity source){return source instanceof TrialTnt;}
    public static LbcIndirectKills.Owner cause(Entity source){return source instanceof TrialTnt t?t.cause:null;}
    private final GameController game;
    private final Map<ServerPlayer,Trial> trials=new IdentityHashMap<>();
    public SauvojenMeditation(GameController game){this.game=game;}
    private static TagKey<Block> tag(String id){return TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:"+id));}
    static boolean participating(ItemStack stack){return LbcAcademy.trial(stack)
            &&(stack.is(LbcWands.SAUVOJEN)||stack.getOrDefault(DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("sauvojen_wand",0)==1);}
    public boolean start(ServerPlayer owner,InteractionHand hand,ItemStack stack){
        if(hand!=InteractionHand.MAIN_HAND||owner.getMainHandItem()!=stack||!owner.isShiftKeyDown()
                ||!participating(stack)||!valid(owner)||game.items().magic().suppressed(owner)
                ||game.items().timeStop().stopped(owner.level()))return false;
        if(trials.containsKey(owner))return true;
        var t=new Trial(owner,stack,game.items().epoch(),game.items().indirectKills().wildCause(owner));
        trials.put(owner,t);counter(t);title(owner,"meditation_gaycaster",false);return true;
    }
    private boolean valid(ServerPlayer owner){return owner.isAlive()&&!owner.isRemoved()
            &&owner.level()==game.maps().level()&&game.canUseGameplayItems(owner)&&!game.isObserver(owner)
            &&game.stage()!=GameController.Stage.ENDING;}
    public int count(){return trials.size();}
    int remaining(ServerPlayer owner){var t=trials.get(owner);return t==null?0:t.remaining;}
    List<Hazard> snapshot(ServerPlayer owner){var t=trials.get(owner);return t==null?List.of():List.copyOf(t.hazards);}
    Trial trial(ServerPlayer owner){return trials.get(owner);}
    private void counter(Trial t){game.items().mana().score(t.owner,"sauvojen_wand_upgrade").set(t.remaining);}
    public void tick(){
        for(var t:List.copyOf(trials.values())){
            if(!valid(t.owner)||t.owner.level()!=t.level||!game.items().currentEpoch(t.epoch)) {finish(t,null);continue;}
            if(t.owner.getMainHandItem()!=t.stack||!participating(t.stack)){finish(t,"sauvojen_wand_meditate_no_wand");continue;}
            if(game.maps().busy()||game.items().timeStop().stopped(t.level)||t.owner.entityTags().contains("time_stopped"))continue;
            // Legacy stopper pauses countdown; already emitted helpers retain their own local suppression.
            if(!game.items().magic().suppressed(t.owner)){
                if(!phase(t))continue;
                if(trials.get(t.owner)!=t)continue;
                if(t.remaining==1){game.items().academy().ready(t.owner,InteractionHand.MAIN_HAND,t.stack);finish(t,null);continue;}
                t.remaining--;counter(t);
            }
            for(var h:List.copyOf(t.hazards)){
                if(trials.get(t.owner)!=t)break;
                if(!resident(t.level,h.at)||game.items().magic().suppressedAt(t.level,h.at)||step(t,h)){
                    t.hazards.remove(h);h.close();
                }
            }
            t.ticks++;
        }
    }
    /** False means a required resident footprint/template is unavailable: retry without losing the phase. */
    boolean phase(Trial t){
        int n=t.remaining;Vec3 at=t.owner.position();var r=t.owner.getRandom();
        if(n==620&&!ruby(t))return false;
        if(n==320&&!stone(t))return false;
        if(n==20&&!fire(t))return false;
        int stage=switch(n){case 620->1;case 560->2;case 520->3;case 420->4;case 310->5;case 300->6;case 160->7;case 140->8;case 30->9;default->0;};
        if(stage>0)title(t.owner,"sauvojen_wand_meditate_"+stage,stage==9);
        if(n>=550&&n<=600){
            int ticker=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("ticker"),"lbc.math").get();
            for(int i=1;i<=3;i++){
                Vec3 point=at.add(0,1,0).add(Vec3.directionFromRotation(0,ticker*i).scale(10));
                Hazard h=add(t,Kind.TERRA,point,t.owner.getEyePosition().subtract(point).multiply(1,0,1).normalize());
                model(t,h,"st/terra",1.5f);sound(t,point,"minecraft:block.beacon.activate",.5f,2);
                line(t,point,h.direction,80,.25,0xcc0000);
            }
        }
        if(n==520||n==500||n==480||n==460||n==440){
            if(n!=520)webDamage(t);
            if(trials.get(t.owner)!=t)return true;
            int count=n==520?4:n==500?8:n==480?12:n==460?14:16;
            for(int i=0;i<count;i++){
                // Source spreadplayers uses a square, minimum range4, maximum12, then motion-blocking height.
                Vec3 point=webPosition(t,at);
                var h=add(t,Kind.WEB,point,Vec3.ZERO);model(t,h,"st/electro",2);h.actor.setXRot(-90);
                h.actor.setYRot(yaw(t.owner.getEyePosition().subtract(point))+90);
                sound(t,h.at,"minecraft:block.beacon.activate",.5f,2);
            }
            webLines(t,false);
        }
        if(n==420)webDamage(t);
        if(n>=383&&n<=419){
            var h=add(t,Kind.AIR,at.add(Vec3.directionFromRotation(0,t.owner.getYRot()).scale(401-n)),Vec3.ZERO);
            model(t,h,null,0);rings(t,h.at.add(0,.5,0),7,0xff0000,false);
            rings(t,h.at.add(0,1.5,0),7,0xff0000,false);rings(t,h.at.add(0,2.5,0),7,0xff0000,false);
        }
        if(n==300)add(t,Kind.ACID_RING,at.add(-5,0,0),Vec3.directionFromRotation(0,0));
        if(n>=200&&n<=290&&r.nextBoolean()){
            Vec3 point=new Vec3(Math.floor(at.x)+r.nextInt(21)-10,at.y+9,Math.floor(at.z)+r.nextInt(21)-10);
            if(resident(t.level,point)){var h=add(t,Kind.ACID_BALLER,point,new Vec3(0,-1,0));model(t,h,"st/acid",2.5f);
                h.actor.setXRot(90);acidWarning(t,point);}
        }
        if(n==160){effect(t,t.owner,MobEffects.LEVITATION,300,0);effect(t,t.owner,MobEffects.LEVITATION,20,20);}
        if(n==140)effect(t,t.owner,MobEffects.DARKNESS,300,0);
        // The source increments before evaluating the following blind range.
        if(n>=40&&n<=120&&r.nextFloat()<.7f)t.remaining++;
        if(t.remaining>=40&&t.remaining<=140&&r.nextBoolean())blind(t);
        return true;
    }
    static int rubyHalfWidth(int x){int a=Math.abs(x);return a<=3?10:a<=5?9:a==6?8:a==7?7:a==8?6:a==9?5:3;}
    private boolean ruby(Trial t){
        if(!terrainWork(t,620))return false;BlockPos c=t.owner.blockPosition();
        sound(t,t.owner.position(),"minecraft:block.amethyst_cluster.break",3,.8f);
        t.level.sendParticles(new DustParticleOptions(0xd400ff,2),c.getX(),c.getY(),c.getZ(),100,10,0,10,0);return true;
    }
    private boolean stone(Trial t){
        var template=game.server().getStructureTemplateManager().get(STONE).orElse(null);
        if(template==null)throw new IllegalStateException("Missing packaged meditation template data/lbc/structure/stone_cylinder.nbt (lbc:stone_cylinder)");
        if(template.getSize().getX()!=47||template.getSize().getY()!=19||template.getSize().getZ()!=47)
            throw new IllegalStateException("Meditation stone_cylinder must retain source47x19x47 dimensions");
        BlockPos p=t.stoneOrigin==null?t.owner.blockPosition().offset(-23,-4,-23):t.stoneOrigin,end=p.offset(46,18,46);
        if(!resident(t.level,p,end))return false;
        if(t.stoneOrigin==null){t.stoneOrigin=p;t.stoneSeed=t.owner.getRandom().nextLong();t.stoneRow=0;}
        // Preserve every explicit AIR/STONE and absent cell. Native row slices bound the observed full-placement spike.
        long started=System.nanoTime();
        long deadline=EffectTerrain.deadlineNanos();boolean worked=false;
        while(t.stoneRow<19&&System.nanoTime()<deadline){
            int y=p.getY()+t.stoneRow;worked=true;
            terrain(t,p,end,()->game.maps().placeMapSlice(t.level,template,p,new StructurePlaceSettings()
                    .setBoundingBox(new BoundingBox(p.getX(),y,p.getZ(),end.getX(),y,end.getZ())),t.stoneSeed));
            t.stoneRow++;
        }
        if(worked){long elapsed=System.nanoTime()-started;t.terrainNanos+=elapsed;t.maxTerrainNanos=Math.max(t.maxTerrainNanos,elapsed);t.terrainTurns++;}
        if(t.stoneRow<19)return false;t.stoneOrigin=null;t.stoneRow=0;return true;
    }
    private boolean fire(Trial t){
        if(t.terrain==null){
            BlockPos c=t.owner.blockPosition();if(!resident(t.level,c.offset(-60,-50,-60),c.offset(60,0,60)))return false;
            t.owner.removeEffect(MobEffects.LEVITATION);effect(t,t.owner,MobEffects.SLOW_FALLING,80,0);
            sound(t,t.owner.position(),"minecraft:lbcsounds.scream",1,0);
        }
        if(!terrainWork(t,20))return false;
        game.items().mana().score(t.owner,"scared").set(0);
        t.level.sendParticles(ParticleTypes.FLAME,t.owner.getX(),t.owner.getY(),t.owner.getZ(),100,2,2,2,0);return true;
    }
    /** Entire authored volumes, resumed only at small native-write boundaries under the shared52ms deadline. */
    private boolean terrainWork(Trial t,int phase){
        if(t.terrain==null)t.terrain=new TerrainJob(phase,t.owner.blockPosition());
        var job=t.terrain;if(job.phase!=phase)throw new IllegalStateException("Sauvojen terrain phase changed while pending");
        if(!resident(t.level,job.lo,job.hi))return false;
        long started=System.nanoTime(),deadline=EffectTerrain.deadlineNanos();boolean worked=false;
        while(job.cursor<job.size()&&System.nanoTime()<deadline){
            int until=Math.min(job.size(),job.cursor+1024);worked=true;
            terrain(t,job.lo,job.hi,()->{
                while(job.cursor<until){BlockPos cell=job.position();
                    if(phase==620)t.level.destroyBlock(cell,true,t.owner);
                    else {boolean air=job.cursor<20181;t.level.setBlock(cell,(air?Blocks.AIR:Blocks.LAVA).defaultBlockState(),
                            air?STRICT:Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}
                    job.cursor++;
                }
            });
        }
        if(worked){long elapsed=System.nanoTime()-started;t.terrainNanos+=elapsed;t.maxTerrainNanos=Math.max(t.maxTerrainNanos,elapsed);t.terrainTurns++;}
        if(job.cursor<job.size())return false;t.terrain=null;return true;
    }
    private void terrain(Trial t,BlockPos lo,BlockPos hi,Runnable action){
        game.items().indirectKills().withSupportCandidates(t.level,new AABB(Vec3.atLowerCornerOf(lo),Vec3.atLowerCornerOf(hi.offset(1,1,1))),
                ()->game.items().indirectKills().trapChanges(t.level,t.cause,action));
        // Developer/lobby changes also have to participate in the match restoration journal.
        for(int x=lo.getX()>>4;x<=hi.getX()>>4;x++)for(int z=lo.getZ()>>4;z<=hi.getZ()>>4;z++){
            game.maps().recordBlockChange(new BlockPos(Math.max(lo.getX(),x*16),lo.getY(),Math.max(lo.getZ(),z*16)));
            game.maps().recordBlockChange(new BlockPos(Math.min(hi.getX(),x*16+15),hi.getY(),Math.min(hi.getZ(),z*16+15)));
        }
    }
    private Hazard add(Trial t,Kind kind,Vec3 at,Vec3 direction){var h=new Hazard(kind,at,direction);t.hazards.add(h);return h;}
    private void model(Trial t,Hazard h,String model,float scale){
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,t.level);
        var stack=new ItemStack(model==null?Items.WOOL.pick(net.minecraft.world.item.DyeColor.RED):Items.CARROT_ON_A_STICK);
        if(model!=null)stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:"+model));
        display.setItemStack(stack);display.setItemTransform(ItemDisplayContext.FIXED);
        if(h.kind==Kind.AIR||h.kind==Kind.ACID_BALLER||h.kind==Kind.SCARE)
            display.setBrightnessOverride(new net.minecraft.util.Brightness(15,15));
        if(h.kind==Kind.AIR)display.setViewRange(2000);
        display.setPosRotInterpolationDuration(1);display.setTransformationInterpolationDuration(1);
        display.setTransformation(new com.mojang.math.Transformation(new Vector3f(),
                model==null?new Quaternionf():new Quaternionf(.6532815f,.27059805f,.6532815f,.27059805f),
                model==null?new Vector3f(0,200,0):new Vector3f(scale),new Quaternionf()));
        display.setPos(h.at);display.setYRot(yaw(h.direction));display.setXRot(pitch(h.direction));
        own(t,h,display);
    }
    private void own(Trial t,Hazard h,Entity actor){
        actor.entityTags().add(TAG);actor.entityTags().add("magic");game.items().markCurrentItemEpoch(actor);
        h.actor=actor;h.uuid=actor.getUUID();if(!t.level.addFreshEntity(actor)){t.hazards.remove(h);actor.discard();}
    }
    private void blind(Trial t){
        if(looking(t)){
            // The source score deliberately survives admission/failure; only the fire stage resets it.
            int scared=game.items().mana().score(t.owner,"scared").get()+10;
            game.items().mana().score(t.owner,"scared").set(scared);
            float[] volumes={.01f,.05f,.1f,.2f,.3f,.4f,.5f,.6f,.7f,.8f,.9f,2,.5f,.6f,.7f,.8f,.9f,2,.9f,.9f,2};
            float[] pitches={2,1.9f,1.8f,1.7f,1.6f,1.5f,1.4f,1.3f,1.2f,1.1f,1,.9f,1.4f,1.3f,1.2f,1.1f,1,.9f,1,1,.9f};
            if(scared<=105){int index=Math.clamp((scared-1)/5,0,20);sound(t,t.owner.position(),"minecraft:entity.enderman.scream",volumes[index],pitches[index]);}
            if(scared>105){hurt(t,t.owner,8,DamageTypes.MAGIC,false);return;}
        }
        var r=t.owner.getRandom();Vec3 direction=Vec3.directionFromRotation(r.nextInt(181)-90,r.nextInt(361)-180);
        var h=add(t,Kind.SCARE,t.owner.position().add(direction.scale(10)),Vec3.ZERO);
        var interaction=new Interaction(EntityTypes.INTERACTION,t.level);interaction.setPos(h.at);interaction.setWidth(4);interaction.setHeight(4);
        interaction.entityTags().add("scare_picture");own(t,h,interaction);
        var face=add(t,Kind.SCARE,h.at,Vec3.ZERO);model(t,face,"scare_face_"+(r.nextInt(10)+1),5);
        face.actor.setGlowingTag(true);((Display)face.actor).setGlowColorOverride(0);
        ((Display)face.actor).setTransformation(new com.mojang.math.Transformation(new Vector3f(0,1.25f,0),new Quaternionf(),new Vector3f(5),new Quaternionf()));
        face.actor.setYRot(yaw(t.owner.getEyePosition().subtract(h.at)));face.actor.setXRot(pitch(t.owner.getEyePosition().subtract(h.at)));
        effect(t,t.owner,MobEffects.DARKNESS,300,0);effect(t,t.owner,MobEffects.LEVITATION,20,0);
    }
    /** Vanilla looking_at: closest interaction on the sight ray (100 blocks), with unobstructed native block clip. */
    boolean looking(Trial t){
        Vec3 from=t.owner.getEyePosition(),to=from.add(t.owner.getLookAngle().scale(100));
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(t.level,t.owner,from,to,
                new AABB(from,to).inflate(1),e->!e.isSpectator(),0);
        return hit!=null&&hit.getEntity() instanceof Interaction&&hit.getEntity().entityTags().contains("scare_picture")
                &&t.owner.hasLineOfSight(hit.getEntity());
    }
    private boolean step(Trial t,Hazard h){
        h.age++;
        switch(h.kind){
            case TERRA->{if(h.age>=40){var ray=add(t,Kind.TERRA_RAY,h.at,h.direction);
                ray.yaw=yaw(h.direction);sound(t,h.at,"minecraft:block.amethyst_cluster.break",.5f,1);
                t.level.sendParticles(ParticleTypes.WITCH,h.at.x,h.at.y,h.at.z,10,.2,.2,.2,2);return true;}}
            case TERRA_RAY->{
                Vec3 end=h.at.add(h.direction.scale(1.4));
                var shield=game.items().wands().shieldHit(t.owner,h.at,end,true);
                var greed=LbcGreed.firstHit(t.level,h.at,end,.15);
                if(shield!=null||greed!=null)return true;
                for(int i=0;i<8;i++){
                    if(!resident(t.level,h.at))return true;
                    t.level.sendParticles(new net.minecraft.core.particles.DustColorTransitionOptions(0x66ff66,0x00ff00,1),
                            true,false,h.at.x,h.at.y,h.at.z,1,.1,.1,.1,0);
                    contact(t,h.at,false);
                    if(!t.level.getBlockState(BlockPos.containing(h.at)).is(AIRS)){
                        BlockPos p=BlockPos.containing(h.at.subtract(h.direction.scale(.2)));
                        terrain(t,p.offset(-7,-3,-7),p.offset(7,3,7),()->LbcTerra.growVegetation(t.level,p,LbcTerra.Kind.REGULAR));return true;
                    }
                    if(i<7)h.at=h.at.add(h.direction.scale(.2));
                }return h.age>=20;
            }
            case AIR->{
                if(h.actor instanceof Display d){d.setYRot(d.getYRot()+40);d.setTransformation(new com.mojang.math.Transformation(
                        new Vector3f(),new Quaternionf(),new Vector3f(h.age*.05f,200,h.age*.05f),new Quaternionf()));}
                if(h.age>=60){
                    for(int i=0;i<4;i++)blast(t,h.at);
                    for(var e:targets(t,h.at,7))if(e.position().distanceToSqr(h.at)<=49)hurt(t,e,8,DamageTypes.MAGIC,false);
                    sound(t,h.at,"minecraft:entity.generic.explode",1,1.3f);return true;
                }
            }
            case ACID_RING->{rings(t,h.at.add(0,1,0),10,0x00ff00,true);h.at=h.at.add(Vec3.directionFromRotation(0,h.yaw).scale(.25));h.yaw+=5;return h.age>=130;}
            case ACID_BALLER->{
                if(h.age<40){if(t.owner.getRandom().nextFloat()<.15f){
                    acidWarning(t,h.at);var nearest=trials.values().stream().filter(other->other.level==t.level)
                            .min(Comparator.comparingDouble(other->other.owner.position().distanceToSqr(h.at))).orElse(t);
                    Vec3 horizontal=nearest.owner.position().subtract(h.at).multiply(1,0,1).normalize();
                    h.at=h.at.add(horizontal.scale(.1));if(h.actor!=null){h.actor.setPos(h.at);h.actor.setYRot(yaw(horizontal));h.actor.setXRot(90);}
                }}
                else {if(t.owner.getRandom().nextBoolean()){
                    add(t,Kind.ACID_BALL,h.at,Vec3.directionFromRotation(80+t.owner.getRandom().nextInt(101)*.1f,-180+t.owner.getRandom().nextInt(3601)*.1f));}
                    if(t.owner.getRandom().nextBoolean())sound(t,h.at,"minecraft:entity.zombie_villager.cure",.7f,2);
                }return h.age>=50;
            }
            case ACID_BALL->{
                for(int i=0;i<3;i++){
                    if(t.owner.getRandom().nextFloat()<.2f)t.level.sendParticles(new DustParticleOptions(0x00ff00,1.5f),h.at.x,h.at.y,h.at.z,1,0,0,0,0);
                    contact(t,h.at,true);Vec3 next=h.at.add(h.direction.scale(.4));if(!resident(t.level,next))return true;
                    if(!t.level.getBlockState(BlockPos.containing(next)).is(AIRS)){corrupt(t,BlockPos.containing(next));break;}
                    if(i<2)h.at=next;
                }return h.age>=40;
            }
            case SCARE->{
                if(h.actor instanceof Interaction){
                    if(t.owner.getRandom().nextFloat()<.01f)sound(t,h.at,"minecraft:ambient.underwater.loop.additions.ultra_rare",2,0);
                    if(t.owner.getRandom().nextFloat()<.005f)sound(t,h.at,"minecraft:ambient.underwater.loop.additions.ultra_rare",2,1);
                    if(t.owner.getRandom().nextFloat()<.0025f)sound(t,h.at,"minecraft:ambient.underwater.loop.additions.ultra_rare",2,1.5f);
                }
                if(h.actor instanceof Display&&t.owner.getRandom().nextFloat()<.05f){
                    var nearest=trials.values().stream().filter(other->other.level==t.level).min(Comparator.comparingDouble(other->other.owner.position().distanceToSqr(h.at))).orElse(t);
                    h.actor.setYRot(yaw(nearest.owner.getEyePosition().subtract(h.at)));h.actor.setXRot(pitch(nearest.owner.getEyePosition().subtract(h.at)));
                }h.at=h.at.add(0,.1,0);if(h.actor!=null)h.actor.setPos(h.at);return h.age>=100;
            }
            case WEB->{}
        }return false;
    }
    void blast(Trial t,Vec3 at){
        if(!t.level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES))return;
        var source=new TrialTnt(t,at);game.items().markCurrentItemEpoch(source);
        game.items().indirectKills().trapChanges(t.level,t.cause,()->t.level.explode(source,at.x,at.y,at.z,4,Level.ExplosionInteraction.TNT));
    }
    private Vec3 webPosition(Trial t,Vec3 center){
        // Source invokes spreadplayers separately for each sole @s, so its spreadDistance4 has no pair constraint.
        // Native spreadplayers centres successful cells and rejects unsafe support before the final height projection.
        var r=t.owner.getRandom();Vec3 chosen=center;
        for(int attempt=0;attempt<10000;attempt++){
            double x=Math.floor(center.x+r.nextDouble()*24-12)+.5,z=Math.floor(center.z+r.nextDouble()*24-12)+.5;
            var cell=BlockPos.containing(x,center.y,z);if(!resident(t.level,new Vec3(x,center.y,z)))continue;
            int surface=t.level.getHeight(Heightmap.Types.MOTION_BLOCKING,cell.getX(),cell.getZ());
            var ground=t.level.getBlockState(new BlockPos(cell.getX(),surface-1,cell.getZ()));
            if(ground.liquid()||!ground.is(net.minecraft.tags.BlockTags.ENTITIES_CAN_TELEPORT_TO))continue;
            chosen=new Vec3(x,surface+1,z);break;
        }
        return chosen;
    }
    private void corrupt(Trial t,BlockPos p){
        var old=t.level.getBlockState(p);Block next;
        if(old.is(Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.BLACK)))next=Blocks.AIR;
        else if(old.is(Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.GRAY)))next=Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.BLACK);
        else if(old.is(Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY)))next=Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.GRAY);
        else if(old.is(Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.YELLOW)))next=Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY);
        else if(old.is(Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.GREEN)))next=Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.YELLOW);
        else if(old.is(Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.LIME)))next=Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.GREEN);
        else {if(old.is(TO_LIME))return;next=Blocks.CONCRETE_POWDER.pick(net.minecraft.world.item.DyeColor.LIME);}
        Block replacement=next;terrain(t,p,p,()->t.level.setBlockAndUpdate(p,replacement.defaultBlockState()));
    }
    private void webDamage(Trial t){webLines(t,true);for(var h:List.copyOf(t.hazards))if(h.kind==Kind.WEB){t.hazards.remove(h);h.close();}}
    /** Source facing entity selector forks to every previously active node, giving n(n-1)/2 rays. */
    int webLines(Trial t,boolean damage){
        var webs=t.hazards.stream().filter(h->h.kind==Kind.WEB).toList();if(webs.isEmpty())return 0;
        int edges=0;
        var active=new ArrayList<Hazard>();active.add(webs.getFirst());
        for(int i=1;i<webs.size();i++){
            var h=webs.get(i);
            for(var target:active){edges++;
            Vec3 aim=target.at.subtract(h.at);double distance=aim.length();Vec3 direction=aim.normalize();
            double step=damage?.5:.2;int limit=damage?201:1000;
            for(int j=0;j<limit&&j*step<=distance;j++){
                Vec3 p=h.at.add(direction.scale(j*step));if(!resident(t.level,p)||game.items().magic().suppressedAt(t.level,p))break;
                if(LbcGreed.firstHit(t.level,p,p.add(direction.scale(step)),.15)!=null)break;
                if(damage){t.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,p.x,p.y,p.z,4,.1,.1,.1,1);
                    t.level.sendParticles(new net.minecraft.core.particles.DustColorTransitionOptions(0x00ccff,0x003333,.8f),p.x,p.y,p.z,1,.1,.1,.1,0);}
                else t.level.sendParticles(new DustParticleOptions(0xcc0000,1),p.x,p.y,p.z,1,0,0,0,0);
                if(damage&&contact(t,p,false))break; // source return run damaged terminates this fork's ray.
                boolean terminal=false;for(var node:active)if(node.at.distanceToSqr(p)<=(damage?.25:.09)){terminal=true;break;}
                if(terminal)break;
            }}active.add(h);
        }return edges;
    }
    private List<LivingEntity> targets(Trial t,Vec3 at,double radius){return t.level.getEntitiesOfClass(LivingEntity.class,
            new AABB(at,at).inflate(radius),e->e.isAlive()&&game.items().wands().validTarget(e)&&!LbcGreed.active(e)
                    &&!e.entityTags().contains("time_stopped"));}
    private boolean contact(Trial t,Vec3 point,boolean acid){
        // Legacy two dx=dy=dz=0 selectors intersect a .99-wide native bounding-box sampling cube.
        var first=new AABB(point,point.add(1,1,1));
        var second=new AABB(point.add(-.99,-.99,-.99),point.add(.01,.01,.01));
        for(var e:targets(t,point,5))if(e.getBoundingBox().intersects(first)&&e.getBoundingBox().intersects(second)){
            hurt(t,e,acid?4:8,acid?DamageTypes.IN_FIRE:DamageTypes.MAGIC,acid);return true;
        }return false;
    }
    void hurt(Trial t,LivingEntity e,float amount,ResourceKey<DamageType> type,boolean acid){
        var damage=new DamageSource(t.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),t.owner);
        e.hurtServer(t.level,game.items().indirectKills().captureDamage(damage,t.cause),amount);
        if(acid)effect(t,e,MobEffects.POISON,200,0);
        // Source calls fail irrespective of damage acceptance; close the victim's own trial only.
        if(e instanceof ServerPlayer p){var own=trials.get(p);if(own!=null)finish(own,"sauvojen_wand_meditate_fail");}
    }
    private void effect(Trial t,LivingEntity e,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,int ticks,int amp){
        game.items().indirectKills().wildEffect(e,ItemEffectLevels.create(effect,ticks,amp,false,false,false),t.cause);
        if(effect==MobEffects.LEVITATION)game.items().indirectKills().displaced(e,t.cause);
    }
    private void rings(Trial t,Vec3 at,double radius,int color,boolean damage){
        for(int i=0;i<26;i++)for(int axis=0;axis<4;axis++){double angle=Math.toRadians(i*4+axis*90);Vec3 p=at.add(Math.sin(angle)*radius,0,Math.cos(angle)*radius);
            if(resident(t.level,p))t.level.sendParticles(damage?new DustParticleOptions(color,1):
                    new net.minecraft.core.particles.DustColorTransitionOptions(0x000000,0xff0000,1),p.x,p.y,p.z,1,0,0,0,0);}
        if(damage)for(var e:targets(t,at,15)){double d=e.position().distanceToSqr(at);if(d>=100&&d<=225)hurt(t,e,8,DamageTypes.MAGIC,false);}
        sound(t,at,"minecraft:block.beacon.activate",1,.5f);
    }
    private void acidWarning(Trial t,Vec3 at){
        line(t,at,new Vec3(0,-1,0),40,.25,0xcc0000);
        sound(t,at,"minecraft:block.beacon.activate",1,1);
        t.level.sendParticles(ParticleTypes.WITCH,at.x,at.y,at.z,2,.4,.4,.4,0);
    }
    private void line(Trial t,Vec3 at,Vec3 direction,int samples,double step,int color){
        for(int i=1;i<=samples;i++){Vec3 p=at.add(direction.scale(i*step));if(!resident(t.level,p))break;
            t.level.sendParticles(new DustParticleOptions(color,.8f),p.x,p.y,p.z,1,0,0,0,0);}
    }
    private static float yaw(Vec3 direction){return (float)Math.toDegrees(Math.atan2(-direction.x,direction.z));}
    private static float pitch(Vec3 direction){return (float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));}
    private static boolean resident(ServerLevel level,Vec3 at){var p=BlockPos.containing(at);return !level.isOutsideBuildHeight(p)
            &&level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null;}
    private static boolean resident(ServerLevel level,BlockPos lo,BlockPos hi){
        if(level.isOutsideBuildHeight(lo)||level.isOutsideBuildHeight(hi))return false;
        for(int x=(lo.getX()-1)>>4;x<=(hi.getX()+1)>>4;x++)for(int z=(lo.getZ()-1)>>4;z<=(hi.getZ()+1)>>4;z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;return true;
    }
    private static void title(ServerPlayer owner,String key,boolean warning){
        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(warning?
                Component.translatable("sauvojen_wand_meditate_91").withStyle(ChatFormatting.DARK_RED):Component.literal(" ")));
        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(warning?
                Component.translatable(key).withStyle(ChatFormatting.RED):Component.translatable(key)));
    }
    private static void sound(Trial t,Vec3 at,String id,float volume,float pitch){LbcOverload.sound(t.owner,at,id,volume,pitch,0);}
    private void finish(Trial t,String key){
        if(trials.get(t.owner)!=t)return;trials.remove(t.owner);t.remaining=0;t.terrain=null;t.stoneOrigin=null;t.stoneRow=0;counter(t);
        for(var h:t.hazards)h.close();t.hazards.clear();
        if(key!=null)t.owner.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }
    public void forget(ServerPlayer owner){var t=trials.get(owner);if(t!=null)finish(t,null);}
    /** Parent damage callback: native TNT is also a trial hit, even when another participant authored it. */
    public void damaged(LivingEntity victim,DamageSource damage){
        if(managed(damage.getDirectEntity())&&victim instanceof ServerPlayer owner){
            var t=trials.get(owner);if(t!=null)finish(t,"sauvojen_wand_meditate_fail");
        }
    }
    public void loaded(Entity actor){
        if(!actor.entityTags().contains(TAG))return;
        for(var t:trials.values())for(var h:t.hazards)if(actor.getUUID().equals(h.uuid)&&game.items().currentEpoch(t.epoch)){
            h.actor=actor;return;}
        actor.discard();
    }
    public void unloaded(Entity actor){
        if(!actor.entityTags().contains(TAG))return;
        for(var t:trials.values())for(var h:List.copyOf(t.hazards))if(h.actor==actor){
            // Never keep an invisible damaging helper after its actual actor is removed/unloaded.
            t.hazards.remove(h);h.close();
        }
    }
    public void stop(){for(var t:List.copyOf(trials.values()))finish(t,null);}
}
