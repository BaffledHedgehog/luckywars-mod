package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Soulless wand, its native Vex consequence and the six deployable rewards. */
public final class LbcSoulless {
    private static final String BASE="skywars2.metasola_base",DISPLAY="skywars2.metasola_display",VEX="skywars2.ghost_vex",TRIAL="skywars2.soulless_trial_vex";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    public static final Item WAND=item(0);
    public static final Item ACADEMY_WAND=item(-1);
    public static final Item METASOLA_1=item(1),METASOLA_2=item(2),METASOLA_3=item(3),METASOLA_4=item(4),METASOLA_5=item(5),METASOLA_6=item(6);
    public static final Item[] MATERIALS={METASOLA_1,METASOLA_2,METASOLA_3,METASOLA_4,METASOLA_5,METASOLA_6};
    private static final List<Holder<Attribute>> ATTRIBUTES=List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_DAMAGE,Attributes.GRAVITY,Attributes.FALL_DAMAGE_MULTIPLIER);
    private static final String[] IDS={"89shg89emh89gseghs89d98ghsdg","47568678ghs89d98ghsdg","47565hytyktyktysdg","wge5gd6j7hn8kus89d98ghsdg","wge5gh45h45h54h5sdg"};
    private static final double[] AMOUNTS={-100,-100,-100,-.85,-1};
    private static final int[] DELAYS={200,80,100,40,200,100},HP={25,50,20,15,500,40};
    private final GameController game;
    private final LbcMana mana;
    private int checkpointDepth;
    private boolean rewinding;
    private final Map<Entity,Boolean> checkpointHeld=new java.util.IdentityHashMap<>();
    private final Map<LivingEntity,Ghosting> ghosts=new HashMap<>();
    private final List<Field> fields=new ArrayList<>();
    private final Map<LivingEntity,Integer> incarnationCooldowns=new HashMap<>();
    private final List<GhostVex> vexes=new ArrayList<>();
    public LbcSoulless(GameController game,LbcMana mana){this.game=game;this.mana=mana;}
    public static void register(){}
    private static Item item(int type){
        boolean wand=type<=0,academy=type<0;
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(academy?"skywars2:academy_soulcorruption_wand":wand?"skywars2:soulcorruption_wand":"skywars2:metasola_"+type));
        var model=Identifier.parse(wand?"lbc:soulcorruptwand":"lbc:metasola_"+type);
        var tag=new CompoundTag();
        if(wand){for(var name:List.of("craftusable","transformable","staff","magicwand","soulcorrupter"))tag.putInt(name,1);
            if(academy){tag.putInt("upgraded",1);tag.putInt("mode",1);}}
        else{tag.putInt("metasola",1);tag.putInt("type",type);tag.putInt("gadget",1);tag.putInt("transformable",1);}
        var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(wand?1:16)
                .overrideDescription(wand?"soulcorruption_wand":"metasola_"+type).component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        props.component(DataComponents.TOOLTIP_STYLE,Identifier.parse(type==0||type==1?"lbc:tier_b":type<5?"lbc:tier_a":"lbc:tier_s"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,wand?Items.CARROT_ON_A_STICK:Items.SNOWBALL,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static boolean isWand(ItemStack stack){return !stack.isEmpty()&&(stack.is(WAND)||stack.is(ACADEMY_WAND)||data(stack).getIntOr("soulcorrupter",0)==1);}
    public static boolean isAcademy(ItemStack stack){return stack.is(ACADEMY_WAND)||isWand(stack)&&data(stack).getIntOr("upgraded",0)==1;}
    public static int material(ItemStack stack){for(int i=0;i<MATERIALS.length;i++)if(stack.is(MATERIALS[i]))return i+1;return data(stack).getIntOr("metasola",0)==1?data(stack).getIntOr("type",0):0;}
    private boolean ownerValid(ServerPlayer p){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()&&game.canUseGameplayItems(p)&&p.level()==game.maps().level();}
    private boolean paused(Entity e){return !game.server().tickRateManager().runsNormally()||e.entityTags().contains("time_stopped")||game.items().timeStop().frozen(e)||e.level() instanceof ServerLevel l&&game.items().timeStop().stopped(l);}
    private boolean allowed(ServerPlayer p){return ownerValid(p)&&!paused(p)&&!game.items().actionsBlocked(p)&&!game.items().frost().isFrozen(p)&&mana.noMagic(p)<=0&&!game.items().magic().suppressed(p)&&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());}
    public boolean use(ServerPlayer p,InteractionHand hand){
        checkpointDepth++;try{
        var stack=p.getItemInHand(hand);int type=material(stack);
        if(!isWand(stack)&&type==0||!allowed(p))return false;
        if(type>=1&&type<=6){
            var transport=new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(p.level(),p,stack);
            transport.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.5f,1);
            if(spawn(p,transport.position(),transport.getDeltaMovement(),type)==null)return false;
            LbcOverload.sound(p,p.position(),"minecraft:entity.snowball.throw",.5f,.4f/(p.getRandom().nextFloat()*.4f+.8f),0);
            stack.consume(1,p);return true;
        }
        boolean academy=isAcademy(stack);
        int mode=Math.clamp(data(stack).getIntOr("mode",1),1,2);
        if(academy&&p.isShiftKeyDown()){
            // Mode selection changes only the actual used stack and never enters the paid cast path.
            var tag=data(stack);mode=mode==1?2:1;tag.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            p.getInventory().setChanged();
            p.sendSystemMessage(net.minecraft.network.chat.Component.translatable(mode==1?"mode_default":"mode_create_metasola"));
            return true;
        }
        int cost=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("soulcorruption_wand_mana_use"),"lbc.math").get();
        if(!isWand(stack)||cost<0||!mana.trySpendWand(p,cost))return false;
        if(academy&&mode==2){
            // (user) Creation has the full 1500-mana admission/debit, without the legacy 1000 refund.
            if(!generate(p)){mana.set(p,mana.get(p)+cost);return false;}
        }else if(!academy&&p.isShiftKeyDown()&&LbcAcademy.trial(stack)){
            // These are ordinary hostile Vex, not the ghosting consequence with a wind-charge reward.
            for(int i=0;i<16;i++)spawnVex(p,p.position().add(0,1.6,0),true);
            game.items().academy().ready(p,hand,stack);
        }else cast(p);
        game.items().wands().wandCasted(p);return true;

        }finally{checkpointDepth--;}
    }
    boolean generate(ServerPlayer p){
        checkpointDepth++;try{
        var loot=game.lbc().rollTable(p.level(),p.position(),p,"lbc:metasola");
        if(loot.isEmpty())return false;
        for(var stack:loot){
            var drop=new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),stack);drop.setDefaultPickUpDelay();
            if(!p.level().addFreshEntity(drop))return false;
        }
        return true;

        }finally{checkpointDepth--;}
    }
    void cast(ServerPlayer p){
        checkpointDepth++;try{for(var target:targets(p,p.getEyePosition(),25))apply(p,target);
        }finally{checkpointDepth--;}
    }
    private List<LivingEntity> targets(ServerPlayer p,Vec3 at,double radius){return p.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),e->e.position().distanceToSqr(at)<=radius*radius&&e.getType().builtInRegistryHolder().is(MOBS)&&eligible(e));}
    private boolean eligible(LivingEntity target){return game.items().wands().validTarget(target)&&!paused(target)&&!game.items().magic().suppressedAt((ServerLevel)target.level(),target.position());}
    private static final class Ghosting {
        ServerPlayer owner;final LbcIndirectKills.Owner cause;int frame;
        Ghosting(ServerPlayer owner,LbcIndirectKills.Owner cause){this.owner=owner;this.cause=cause;}
    }
    void apply(ServerPlayer p,LivingEntity target){
        checkpointDepth++;try{
        if(!eligible(target)||target instanceof ServerPlayer player&&game.items().magic().rejectCustomEffect(player))return;
        if(target instanceof Vex){convert(p,target);return;}
        var previous=ghosts.get(target);var status=new Ghosting(p,game.items().indirectKills().wildCause(p));
        if(previous!=null)status.frame=previous.frame;ghosts.put(target,status);
        mana.score(target,"effect_ghosting").add(5);
        for(int i=0;i<ATTRIBUTES.size();i++){
            var attr=target.getAttribute(ATTRIBUTES.get(i));var id=Identifier.withDefaultNamespace(IDS[i]);
            if(attr!=null&&!attr.hasModifier(id))attr.addTransientModifier(new AttributeModifier(id,AMOUNTS[i],AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        }finally{checkpointDepth--;}
    }
    private void convert(ServerPlayer p,LivingEntity vex){
        var drop=new ItemEntity(p.level(),vex.getX(),vex.getY(),vex.getZ(),game.lbc().rollTable(p.level(),vex.position(),p,"lbc:metasola").getFirst());
        drop.setNoGravity(true);drop.setDeltaMovement(0,-.06,0);drop.setDefaultPickUpDelay();
        p.level().addFreshEntity(drop);feedback(p,vex.position(),"minecraft:entity.vex.charge",1,.3f);
        game.items().indirectKills().withCause(vex,game.items().indirectKills().wildCause(p),()->vex.kill(p.level()));
    }
    private void feedback(ServerPlayer p,Vec3 at,String sound,float volume,float pitch){p.level().sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,10,0,0,0,.5);LbcOverload.sound(p,at,sound,volume,pitch,0);}
    void tickGhost(LivingEntity target,Ghosting status){
        checkpointDepth++;try{
        if(!ownerValid(status.owner)||target.isRemoved()||!target.isAlive()||target.level()!=status.owner.level()){clear(target);return;}
        if(paused(target)||LbcGreed.active(target))return;
        if(!game.items().wands().validTarget(target)||target instanceof ServerPlayer player&&game.items().magic().rejectCustomEffect(player)||game.items().magic().suppressedAt((ServerLevel)target.level(),target.position())){clear(target);return;}
        if(mana.score(target,"effect_ghosting").get()<=0){clear(target);return;}
        target.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,20,1,false,false),status.owner);
        if(target.isSprinting()&&!target.onGround()){
            double yaw=Math.toRadians(target.getYRot());move(target,new Vec3(-Math.sin(yaw)*.2,0,Math.cos(yaw)*.2),status.cause);
            particles(status.owner,target.position(),ParticleTypes.GUST,1);particles(status.owner,target.position().add(0,.5,0),ParticleTypes.SOUL,1);
            LbcOverload.sound(status.owner,target.position(),"minecraft:block.wool.step",.5f,2,0);
        }
        if(target.hurtTime==10)spawnVex(status.owner,target.position());
        if(++status.frame%20==0){mana.score(target,"effect_ghosting").add(-1);particles(status.owner,target.position().add(0,1,0),ParticleTypes.SOUL,5);if(mana.score(target,"effect_ghosting").get()<=0)clear(target);}

        }finally{checkpointDepth--;}
    }
    public void clear(LivingEntity target){
        checkpointDepth++;try{
        ghosts.remove(target);mana.score(target,"effect_ghosting").set(0);
        for(int i=0;i<ATTRIBUTES.size();i++){var attr=target.getAttribute(ATTRIBUTES.get(i));if(attr!=null)attr.removeModifier(Identifier.withDefaultNamespace(IDS[i]));}

        }finally{checkpointDepth--;}
    }
    public void purify(ServerPlayer target){clear(target);}
    private final class GhostVex extends Vex {
        ServerPlayer caster;final boolean trial;boolean retired;
        GhostVex(ServerPlayer caster,boolean trial){super(EntityTypes.VEX,caster.level());this.caster=caster;this.trial=trial;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void tick(){if(rewinding||checkpointHeld.containsKey(this))return;if(!validVex(this)){removeVex(this);return;}if(!paused(this))super.tick();}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return !checkpointHeld.containsKey(this)&&super.hurtServer(level,source,amount);}
        @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource damage,boolean byPlayer){
            if(retired)return;
            if(trial)super.dropCustomDeathLoot(level,damage,byPlayer);
            else spawnAtLocation(level,new ItemStack(Items.WIND_CHARGE));
        }
    }
    GhostVex spawnVex(ServerPlayer p,Vec3 at){return spawnVex(p,at,false);}
    private GhostVex spawnVex(ServerPlayer p,Vec3 at,boolean trial){
        checkpointDepth++;try{
        if(!p.level().hasChunkAt(BlockPos.containing(at)))return null;
        var vex=new GhostVex(p,trial);vex.setPos(at);vex.entityTags().add(trial?TRIAL:VEX);game.items().markCurrentItemEpoch(vex);vexes.add(vex);game.items().timeStop().prepareFrozen(vex);
        if(!p.level().addFreshEntity(vex)||vex.isRemoved()){removeVex(vex);return null;}
        if(!trial)feedback(p,at,"minecraft:item.dye.use",1,0);return vex;

        }finally{checkpointDepth--;}
    }
    private boolean validVex(GhostVex v){return !v.retired&&!v.isRemoved()&&ownerValid(v.caster)&&v.level()==v.caster.level()&&game.items().currentEpoch(v)&&v.caster.level().hasChunkAt(v.blockPosition());}
    private void removeVex(GhostVex v){v.retired=true;vexes.remove(v);v.discard();}
    static final class Field {
        ServerPlayer owner;Pig pig;Display.ItemDisplay display;final int type;final LbcIndirectKills.Owner cause;int age,frame;
        Field(ServerPlayer owner,Pig pig,Display.ItemDisplay display,int type,LbcIndirectKills.Owner cause){this.owner=owner;this.pig=pig;this.display=display;this.type=type;this.cause=cause;}
    }
    private final class Body extends Pig {
        Body(ServerLevel level){super(EntityTypes.PIG,level);}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void tick(){if(rewinding||checkpointHeld.containsKey(this))return;var field=find(this);if(field==null||!valid(field)){if(field!=null)LbcSoulless.this.remove(field);else discard();return;}if(!paused(this))super.tick();}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return !checkpointHeld.containsKey(this)&&super.hurtServer(level,source,amount);}
        @Override protected void dropFromLootTable(ServerLevel level,DamageSource source,boolean byPlayer){}
        @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean byPlayer){}
    }
    private final class Visual extends Display.ItemDisplay {
        Visual(ServerLevel level){super(EntityTypes.ITEM_DISPLAY,level);}
        @Override public boolean shouldBeSaved(){return false;}
    }
    Field spawn(ServerPlayer p,Vec3 at,Vec3 itemMotion,int type){
        checkpointDepth++;try{
        if(type<1||type>6||!ownerValid(p)||!p.level().hasChunkAt(BlockPos.containing(at)))return null;
        var pig=new Body(p.level());var display=new Visual(p.level());
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HP[type-1]);pig.setHealth(pig.getMaxHealth());
        pig.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(-100000);pig.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER).setBaseValue(0);
        pig.setSilent(true);pig.setNoAi(false);pig.setNoGravity(false);pig.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,-1,0,false,false));pig.setPos(at);pig.setDeltaMovement(new Vec3(Math.floor(itemMotion.x*2000)*.001,Math.floor(itemMotion.y*2000)*.001,Math.floor(itemMotion.z*2000)*.001));
        try{display.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),TagParser.parseCompoundFully("{item_display:'fixed',brightness:{block:15,sky:15},teleport_duration:1,interpolation_duration:1,transformation:{translation:[0f,.6f,0f],scale:[1f,1f,1f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},item:{id:'minecraft:carrot_on_a_stick',count:1,components:{'minecraft:item_model':'lbc:metasola_"+type+"'}}}")));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
        display.setPos(at);pig.entityTags().addAll(List.of(BASE,"metasola_base"));display.entityTags().addAll(List.of(DISPLAY,"metasola","metasola_"+type,"magic"));
        game.items().markCurrentItemEpoch(pig);game.items().markCurrentItemEpoch(display);
        var field=new Field(p,pig,display,type,game.items().indirectKills().wildCause(p));fields.add(field);
        game.items().timeStop().prepareFrozen(pig);game.items().timeStop().prepareFrozen(display);
        if(!p.level().addFreshEntity(pig)||pig.isRemoved()||!p.level().addFreshEntity(display)||display.isRemoved()){remove(field);return null;}return field;

        }finally{checkpointDepth--;}
    }
    private Field find(Entity e){for(var f:fields)if(f.pig==e||f.display==e)return f;return null;}
    private boolean valid(Field f){return ownerValid(f.owner)&&f.owner.level()==f.pig.level()&&!f.pig.isRemoved()&&f.pig.isAlive()&&!f.display.isRemoved()&&game.items().currentEpoch(f.pig)&&game.items().currentEpoch(f.display)&&f.owner.level().hasChunkAt(f.pig.blockPosition())&&!game.items().magic().suppressedAt(f.owner.level(),f.pig.position());}
    private void remove(Field f){fields.remove(f);f.pig.discard();f.display.discard();}
    private boolean friendly(Field f,LivingEntity e){return e==f.owner||f.owner.isAlliedTo(e);}
    private List<LivingEntity> fieldTargets(Field f,double r){return targets(f.owner,f.display.position(),r).stream().filter(e->!e.entityTags().contains("metasola_base")).toList();}
    void tickField(Field f){
        checkpointDepth++;try{
        if(!valid(f)){remove(f);return;}if(paused(f.pig)||paused(f.display))return;
        f.display.snapTo(f.pig.getX(),f.pig.getY(),f.pig.getZ(),f.pig.getYRot(),0);
        f.age++;if(++f.frame%20==0)second(f);if(f.age<DELAYS[f.type-1])return;
        var at=f.display.position();var p=f.owner;
        switch(f.type){
            case 1->particles(p,at.add(0,.5,0),ParticleTypes.DAMAGE_INDICATOR,1);
            case 2->particles(p,at.add(0,.5,0),ParticleTypes.HEART,2);
            case 3->{particles(p,at.add(0,.5,0),ParticleTypes.SNOWFLAKE,1);for(var e:fieldTargets(f,20)){var d=at.subtract(e.getEyePosition());double yaw=Math.atan2(-d.x,d.z);moveVortex(e,new Vec3(-Math.sin(yaw)*.08+Math.cos(yaw)*.074,.08,Math.cos(yaw)*.08+Math.sin(yaw)*.074).scale(.7),f.cause);particles(p,e.position(),ParticleTypes.GUST,1);particles(p,e.position().add(0,.5,0),ParticleTypes.SOUL,1);if(p.getRandom().nextInt(10)==0)LbcOverload.sound(p,e.position(),"minecraft:block.wool.step",.7f,1.5f,0);}}
            case 4->{particles(p,at.add(0,.5,0),ParticleTypes.WITCH,1);for(var e:fieldTargets(f,10))if(e instanceof ServerPlayer&&friendly(f,e)&&mana.score(e,"mana").get()<=500)mana.score(e,"mana").set(500);}
            // Author requests the full pattern (legacy used integrity 0.1); strict keeps unsupported fire/lava.
            case 5->{p.level().sendParticles(ParticleTypes.LAVA,at.x,at.y,at.z,6,7,7,7,0);if(p.getRandom().nextBoolean())LbcOverload.sound(p,at,"minecraft:block.lava.extinguish",1,1,0);for(var e:targets(p,at,10))if(e.position().distanceToSqr(at)>=.0001)damage(f,e,DamageTypes.IN_FIRE,20);if(p.level().hasChunkAt(BlockPos.containing(f.pig.position().add(0,-.2,0))))f.pig.setPos(f.pig.position().add(0,-.2,0));game.items().indirectKills().trapChanges(p.level(),f.cause,()->game.maps().placeLoadedTemplateStrict(p,Identifier.parse("lbc:light_sphere_21x21"),BlockPos.containing(at.add(-10,-10,-10)),1.0f));}
            case 6->{particles(p,at.add(0,.5,0),new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()),1);for(var e:fieldTargets(f,10))if(friendly(f,e)){e.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,false),p);if(mana.score(e,"void_resistance").get()<2)mana.score(e,"void_resistance").set(2);particles(p,e.position().add(0,1,0),new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()),1);}if(p.getRandom().nextInt(100)==0)LbcOverload.sound(p,at,"minecraft:block.beacon.ambient",1,1,0);}
            default->throw new IllegalStateException();
        }

        }finally{checkpointDepth--;}
    }
    void second(Field f){
        checkpointDepth++;try{
        if(!fields.contains(f)||!valid(f)||paused(f.pig)||paused(f.display)||f.type>2)return;
        f.age+=20;if(f.age<DELAYS[f.type-1])return;
        if(f.type==1){LbcOverload.sound(f.owner,f.display.position(),"minecraft:entity.ravager.attack",.8f,2,0);for(var e:fieldTargets(f,10))if(!friendly(f,e)){damage(f,e,DamageTypes.MAGIC,4);particles(f.owner,e.position().add(0,1,0),ParticleTypes.SWEEP_ATTACK,1);}}
        else for(var e:fieldTargets(f,10))if(friendly(f,e)&&!incarnationCooldowns.containsKey(e)){incarnationCooldowns.put(e,20);heal(f,e);}

        }finally{checkpointDepth--;}
    }
    private void heal(Field f,LivingEntity target){game.items().indirectKills().withCause(target,f.cause,()->MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(f.owner.level(),f.display,f.owner,target,2,1));particles(f.owner,target.position().add(0,2,0),ParticleTypes.HEART,1);LbcOverload.sound(f.owner,target.position(),"minecraft:entity.experience_orb.pickup",.5f,2,0);}
    private void damage(Field f,LivingEntity target,ResourceKey<net.minecraft.world.damagesource.DamageType> type,float amount){game.items().indirectKills().withCause(target,f.cause,()->target.hurtServer(f.owner.level(),new DamageSource(f.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),f.owner),amount));}
    private void move(LivingEntity target,Vec3 add,LbcIndirectKills.Owner cause){var motion=target.getDeltaMovement().add(add);double max=Math.max(Math.abs(motion.x),Math.max(Math.abs(motion.y),Math.abs(motion.z)));if(max>10)motion=motion.scale(10/max);target.setDeltaMovement(motion);target.needsSync=true;target.syncVelocity=true;game.items().indirectKills().displaced(target,cause);}
    private void moveVortex(LivingEntity target,Vec3 add,LbcIndirectKills.Owner cause){
        if(!(target instanceof ServerPlayer player)){move(target,add,cause);return;}
        var before=player.getDeltaMovement();var motion=before.add(add);
        double max=Math.max(Math.abs(motion.x),Math.max(Math.abs(motion.y),Math.abs(motion.z)));
        if(max>10)motion=motion.scale(10/max);
        PlayerForces.add(player,motion.subtract(before));
        game.items().indirectKills().displaced(player,cause);
    }
    private void particles(ServerPlayer p,Vec3 at,net.minecraft.core.particles.ParticleOptions type,int n){p.level().sendParticles(type,at.x,at.y,at.z,n,.3,.3,.3,0);}
    public void tick(){
        checkpointDepth++;try{
        if(rewinding)return;
        // All Incarnations share one native16HP pulse per target per20 active ticks.
        // Keep the interval when a field disappears so replacing its source cannot stack it.
        var cooldowns=incarnationCooldowns.entrySet().iterator();
        while(cooldowns.hasNext()){
            var entry=cooldowns.next();var target=entry.getKey();
            if(target.isRemoved()||!target.isAlive()||target.level()!=game.maps().level()){cooldowns.remove();continue;}
            if(paused(target))continue;
            if(entry.getValue()<=1)cooldowns.remove();else entry.setValue(entry.getValue()-1);
        }
        for(var e:List.copyOf(ghosts.entrySet()))tickGhost(e.getKey(),e.getValue());for(var f:List.copyOf(fields))tickField(f);for(var v:List.copyOf(vexes))if(!validVex(v))removeVex(v);

        }finally{checkpointDepth--;}
    }
    public ServerPlayer owner(Entity e){if(e instanceof GhostVex v)return v.caster;var f=find(e);return f==null?null:f.owner;}
    public net.minecraft.network.chat.Component hud(ServerPlayer p){
        int value=mana.hudValue(p,"effect_ghosting");
        return value<=0?net.minecraft.network.chat.Component.empty():net.minecraft.network.chat.Component.literal("  \uE196 ")
                .append(net.minecraft.network.chat.Component.literal(Integer.toString(value)).withStyle(net.minecraft.ChatFormatting.WHITE));
    }
    public int count(){return fields.size();}
    public int ghostCount(){return ghosts.size();}
    List<Field> snapshot(){return List.copyOf(fields);}
    List<? extends Vex> vexSnapshot(){return vexes.stream().filter(v->!v.trial).toList();}
    List<? extends Vex> trialSnapshot(){return vexes.stream().filter(v->v.trial).toList();}
    public boolean loaded(Entity e){
        checkpointDepth++;try{if(rewinding||checkpointHeld.containsKey(e))return managed(e);if(!managed(e))return false;if(find(e)==null&&!vexes.contains(e))e.discard();return true;
        }finally{checkpointDepth--;}
    }
    public static boolean managed(Entity e){return e!=null&&(e.entityTags().contains(BASE)||e.entityTags().contains(DISPLAY)||e.entityTags().contains(VEX)||e.entityTags().contains(TRIAL));}
    public void unloaded(Entity e){
        checkpointDepth++;try{if(rewinding||checkpointHeld.containsKey(e))return;incarnationCooldowns.remove(e);if(e instanceof LivingEntity living&&ghosts.containsKey(living))clear(living);var f=find(e);if(f!=null)remove(f);if(e instanceof GhostVex v)removeVex(v);
        }finally{checkpointDepth--;}
    }
    public void forget(ServerPlayer p){
        checkpointDepth++;try{if(rewinding)return;incarnationCooldowns.remove(p);for(var e:List.copyOf(ghosts.entrySet()))if(e.getKey()==p||e.getValue().owner==p)clear(e.getKey());for(var f:List.copyOf(fields))if(f.owner==p)remove(f);for(var v:List.copyOf(vexes))if(v.caster==p)removeVex(v);
        }finally{checkpointDepth--;}
    }
    public void stop(){
        checkpointDepth++;try{rewinding=false;releaseHeld(true);incarnationCooldowns.clear();for(var e:List.copyOf(ghosts.keySet()))clear(e);for(var f:List.copyOf(fields))remove(f);for(var v:List.copyOf(vexes))removeVex(v);
        }finally{checkpointDepth--;}
    }
    void checkpointBoundary(){if(checkpointDepth!=0)throw new IllegalStateException("Finish Soulless field/ghost/native callbacks before checkpointing");}
    boolean captureReady(){return checkpointDepth==0;}
    private void hold(Entity entity,boolean invisible){checkpointHeld.put(entity,invisible);entity.setInvisible(true);}
    private void release(Entity entity){var visible=checkpointHeld.remove(entity);if(visible!=null)entity.setInvisible(visible);}
    private void releaseHeld(boolean discard){for(var entry:List.copyOf(checkpointHeld.entrySet())){if(discard){if(!entry.getKey().isRemoved())entry.getKey().discard();}else entry.getKey().setInvisible(entry.getValue());}checkpointHeld.clear();}
    void prepareFrozen(){
        checkpointBoundary();rewinding=true;releaseHeld(false);
        // Native snapshots own score/modifier/effect values. Never clear()/remove()/convert() here.
        ghosts.clear();fields.clear();vexes.clear();incarnationCooldowns.clear();
    }
    void publishFrozen(){checkpointBoundary();rewinding=false;}
    void cancelFrozen(){checkpointBoundary();releaseHeld(false);ghosts.clear();fields.clear();vexes.clear();incarnationCooldowns.clear();rewinding=false;}
    Checkpoint checkpoint(MatchDamageSources sources){checkpointBoundary();if(rewinding)throw new IllegalStateException("Publish Soulless before capture");return new Checkpoint(sources);}
    private abstract class Entry {
        final MatchSpellCheckpoint.Links links;
        final java.util.Set<java.util.UUID> actors=new java.util.LinkedHashSet<>();
        Entry(MatchDamageSources sources){links=new MatchSpellCheckpoint.Links(sources);}
        MatchDamageSources.Ref key(Entity entity){var ref=links.key(entity);if(ref!=null){if(ref.historical()!=null)throw new IllegalStateException("Soulless essential state requires canonical current native bodies");actors.add(ref.id());}return ref;}
        abstract Runnable prepare();
        void holdPending(java.util.function.Function<java.util.UUID,Entity> lookup){}
    }
    private final class GhostImage extends Entry {
        final Ghosting ghost;final MatchDamageSources.Ref target,owner;final int frame;
        GhostImage(LivingEntity target,Ghosting ghost,MatchDamageSources sources){super(sources);this.ghost=ghost;this.target=key(target);owner=key(ghost.owner);frame=ghost.frame;}
        @Override Runnable prepare(){var living=(LivingEntity)links.get(target);var player=(ServerPlayer)links.get(owner);return ()->{ghost.owner=player;ghost.frame=frame;ghosts.put(living,ghost);};}
    }
    private final class FieldImage extends Entry {
        final Field field;final MatchDamageSources.Ref owner,pig,display;final int age,frame;final boolean pigInvisible,displayInvisible;
        FieldImage(Field field,MatchDamageSources sources){super(sources);this.field=field;owner=key(field.owner);pig=key(field.pig);display=key(field.display);age=field.age;frame=field.frame;pigInvisible=field.pig.isInvisible();displayInvisible=field.display.isInvisible();}
        @Override Runnable prepare(){var player=(ServerPlayer)links.get(owner);var body=(Pig)links.get(pig);var visual=(Display.ItemDisplay)links.get(display);
            return ()->{release(body);release(visual);field.owner=player;field.pig=body;field.display=visual;field.age=age;field.frame=frame;fields.add(field);};}
        @Override void holdPending(java.util.function.Function<java.util.UUID,Entity> lookup){var body=lookup.apply(pig.id());var visual=lookup.apply(display.id());
            if(body!=null){if(!(body instanceof Body))throw new IllegalStateException("Restore native Metasola pig before holding its offline field");hold(body,pigInvisible);}
            if(visual!=null){if(!(visual instanceof Display.ItemDisplay))throw new IllegalStateException("Restore native Metasola display before holding its offline field");hold(visual,displayInvisible);}}
    }
    private final class VexImage extends Entry {
        final GhostVex vex;final MatchDamageSources.Ref body,owner;final boolean retired,invisible;
        VexImage(GhostVex vex,MatchDamageSources sources){super(sources);this.vex=vex;body=key(vex);owner=key(vex.caster);retired=vex.retired;invisible=vex.isInvisible();}
        @Override Runnable prepare(){var current=(GhostVex)links.get(body);var player=(ServerPlayer)links.get(owner);return ()->{release(current);current.caster=player;current.retired=retired;vexes.add(current);};}
        @Override void holdPending(java.util.function.Function<java.util.UUID,Entity> lookup){var current=lookup.apply(body.id());if(current!=null){if(!(current instanceof GhostVex))throw new IllegalStateException("Restore native GhostVex before holding its offline caster");hold(current,invisible);}}
    }
    private final class CooldownImage extends Entry {
        final MatchDamageSources.Ref target;final int remaining;
        CooldownImage(LivingEntity target,int remaining,MatchDamageSources sources){super(sources);this.target=key(target);this.remaining=remaining;}
        @Override Runnable prepare(){var current=(LivingEntity)links.get(target);return ()->incarnationCooldowns.put(current,remaining);}
    }
    final class Checkpoint {
        private final List<Entry> entries;
        private final java.util.Set<Entry> installed=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Checkpoint(MatchDamageSources sources){
            var all=new ArrayList<Entry>();ghosts.forEach((target,ghost)->all.add(new GhostImage(target,ghost,sources)));
            for(var field:fields)all.add(new FieldImage(field,sources));for(var vex:vexes)all.add(new VexImage(vex,sources));
            incarnationCooldowns.forEach((target,remaining)->all.add(new CooldownImage(target,remaining,sources)));entries=List.copyOf(all);installed.addAll(entries);
        }
        private Checkpoint(List<Entry> entries){this.entries=List.copyOf(entries);}
        Checkpoint withPending(Checkpoint previous){
            var all=new ArrayList<>(entries);for(var entry:previous.entries)if(!previous.installed.contains(entry))all.add(entry);
            var result=new Checkpoint(all);result.installed.addAll(installed);return result;
        }
        Checkpoint pendingOnly(){return new Checkpoint(entries.stream().filter(entry->!installed.contains(entry)).toList());}
        java.util.Set<java.util.UUID> actorIds(){var all=new java.util.LinkedHashSet<java.util.UUID>();for(var entry:entries)all.addAll(entry.actors);return java.util.Set.copyOf(all);}
        void reset(){installed.clear();}
        MatchFieldCheckpoint.Plan prepareAvailable(java.util.function.Function<java.util.UUID,Entity> lookup){
            checkpointBoundary();var pending=new java.util.LinkedHashSet<java.util.UUID>();var ready=new ArrayList<Entry>();var waiting=new ArrayList<Entry>();var plans=new ArrayList<Runnable>();
            for(var entry:entries)if(!installed.contains(entry)){
                var missing=entry.links.resolve(lookup);if(missing.isEmpty()){ready.add(entry);plans.add(entry.prepare());}else{pending.addAll(missing);waiting.add(entry);}}
            return new MatchFieldCheckpoint.Plan(java.util.Set.copyOf(pending),()->{for(var entry:waiting)entry.holdPending(lookup);plans.forEach(Runnable::run);installed.addAll(ready);});
        }
        boolean hasPending(){return installed.size()!=entries.size();}
    }

}
