package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The actual silverfish family: cast helpers, native melee tanks and passenger wall trial. */
public final class LbcHedgehog {
    private static final String TAG="skywars2.hedgehog", ARROW="skywars2.hedgehog_arrow", CHILD="skywars2.hedgehog_sneeze_child";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    public enum Kind { ORDINARY, DEFAULT, HEALER, TANK, SNIPER, LASER, WALL }
    private static final String[] MODES={"mode_default","mode_healer","mode_tank","mode_far","mode_laser"};
    private final GameController game;
    private final List<Fish> fish=new ArrayList<>();
    private final List<HelperArrow> arrows=new ArrayList<>();
    private final java.util.Set<Silverfish> silverfish=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final java.util.ArrayDeque<BlastBatch> blasts=new java.util.ArrayDeque<>();
    private Entity blastSource;
    private LbcIndirectKills.Owner blastCause;
    private ServerLevel blastLevel;
    private long completedBlasts;
    private record Child(ServerPlayer author,LbcIndirectKills.Owner cause,String epoch){}
    private final java.util.Map<Entity,Child> children=new java.util.IdentityHashMap<>();
    private ServerPlayer blastAuthor;
    private static final class BlastBatch {
        final ServerLevel level;final Vec3 origin;final Silverfish source;final ServerPlayer author;final LbcIndirectKills.Owner cause;final String epoch;int next;
        BlastBatch(ServerLevel level,Vec3 origin,Silverfish source,ServerPlayer author,LbcIndirectKills.Owner cause,String epoch){this.level=level;this.origin=origin;this.source=source;this.author=author;this.cause=cause;this.epoch=epoch;}
    }
    // Raw source offsets, preserving all32 simultaneous radius20 creepers.
    private static final int[][] SNEEZE_OFFSETS={{0,0,0},{5,0,0},{-5,0,0},{0,0,5},{0,0,-5},{5,0,5},{-5,0,-5},{5,0,-5},{-5,0,5},
        {0,5,0},{0,10,0},{0,15,0},{0,20,0},{0,-5,0},{0,-10,0},{0,-15,0},
        {7,5,0},{-7,5,0},{0,5,7},{0,5,-7},{7,5,7},{-7,5,-7},{7,5,-7},{-7,5,7},
        {7,-5,0},{-7,-5,0},{0,-5,7},{0,-5,-7},{7,-5,7},{-7,-5,-7},{7,-5,-7},{-7,-5,7}};
    public LbcHedgehog(GameController game,LbcMana mana){
        this.game=game;
        var holder=ScoreHolder.forNameOnly("baffled_wand_mana_use");var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");
        if(objective==null||board.getPlayerScoreInfo(holder,objective)==null)mana.score(holder,"lbc.math").set(2500);
    }
    public static void register(){}
    public static boolean managed(Entity e){return e!=null&&(e.entityTags().contains(TAG)||e.entityTags().contains(ARROW)||e.entityTags().contains(CHILD));}
    private ServerPlayer player(UUID id){return game.server().getPlayerList().getPlayer(id);}
    private boolean ownerValid(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
            &&p.level()==game.maps().level()&&game.canUseGameplayItems(p)&&!game.isObserver(p);}
    private boolean resident(ServerLevel l,Vec3 p){var b=BlockPos.containing(p);return l.getChunkSource().getChunkNow(b.getX()>>4,b.getZ()>>4)!=null;}
    private boolean frozen(Entity e){return !game.server().tickRateManager().runsNormally()||game.items().wands().timeStopped()
            ||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped")||e instanceof LivingEntity living&&game.items().frost().isFrozen(living);}
    private boolean suppressed(ServerLevel l,Vec3 p){return game.items().magic().suppressedAt(l,p);}
    private boolean valid(Fish f){var p=player(f.author);return !f.retired&&!f.isRemoved()&&(f.kind==Kind.WALL?game.playing()&&game.stage()!=GameController.Stage.ENDING:ownerValid(p)&&p.level()==f.level())
            &&game.items().currentEpoch(f)&&resident((ServerLevel)f.level(),f.position())&&!suppressed((ServerLevel)f.level(),f.position());}
    private boolean eligible(LivingEntity e){return e.isAlive()&&game.items().wands().validTarget(e)&&e.getType().builtInRegistryHolder().is(MOBS)
            &&!frozen(e)&&!suppressed((ServerLevel)e.level(),e.position());}
    private boolean allied(Fish f,LivingEntity e){var p=player(f.author);return e==p||e==f||p!=null&&p.isAlliedTo(e)
            ||e instanceof Fish other&&other.kind!=Kind.WALL&&other.author.equals(f.author);}
    private boolean enemy(Fish f,LivingEntity e){return e!=f&&eligible(e)&&(f.kind==Kind.WALL?!(e instanceof Fish other&&other.kind==Kind.WALL)&&!e.entityTags().contains("baffled_wall"):!allied(f,e))&&!e.entityTags().contains("strict_map_object")
            &&!e.entityTags().contains("nomagic_active")&&!LbcGreed.active(e);}
    private boolean impact(Fish f,LivingEntity e){var p=player(f.author);return e!=f&&eligible(e)&&!LbcGreed.active(e)
            &&(f.kind==Kind.WALL||p==null||!(e instanceof ServerPlayer)||game.items().wands().projectileTarget(p,e));}
    public boolean use(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!LbcHedgehogItems.isWand(stack)||!ownerValid(p)||game.items().godHedgehog().blocksSilverfish(p.level())||frozen(p)||game.items().actionsBlocked(p)
                ||game.items().wands().inputTimeStopped(p)||game.items().magic().suppressed(p)||suppressed(p.level(),p.getEyePosition()))return false;
        boolean academy=LbcHedgehogItems.isAcademy(stack);var tag=LbcFartus.data(stack);int mode=Math.clamp(tag.getIntOr("mode",1),1,5);
        if(academy&&p.isShiftKeyDown()){
            mode=mode%5+1;tag.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));p.getInventory().setChanged();
            p.sendSystemMessage(Component.translatable(MODES[mode-1]));return true;
        }
        int cost=academy?1000:game.items().mana().score(ScoreHolder.forNameOnly("baffled_wand_mana_use"),"lbc.math").get();
        if(cost<0||!game.items().mana().trySpendWand(p,cost))return false;
        cast(p,academy?Kind.values()[mode]:Kind.ORDINARY);
        long helpers=fish.stream().filter(f->f.isAlive()&&!f.isRemoved()&&f.level()==p.level()&&f.position().distanceToSqr(p.position())<=100
                &&(academy?f.kind!=Kind.ORDINARY&&f.kind!=Kind.WALL:f.kind==Kind.ORDINARY)).count();
        if(helpers>=40)game.items().mana().award(p,"lbc:true_advancements/staffs/hedgehog");
        if(!academy&&p.isShiftKeyDown()&&LbcAcademy.trial(stack)){
            spawnWall(p.level(),p.position(),p);game.items().academy().ready(p,hand,stack);
        }
        game.items().wands().wandCasted(p);return true;
    }
    void cast(ServerPlayer p,Kind kind){
        boolean academy=kind!=Kind.ORDINARY;var direction=p.getLookAngle().normalize();var origin=p.getEyePosition();double step=academy?.6:1;
        for(int sample=0;sample<=(academy?400:200);sample++){
            var at=origin.add(direction.scale(sample*step));if(!resident(p.level(),at)||suppressed(p.level(),at))return;
            if(kind==Kind.ORDINARY||kind==Kind.DEFAULT)p.level().sendParticles(ParticleTypes.WHITE_ASH,at.x,at.y,at.z,2,.5,.5,.5,0);
            else {int color=switch(kind){case HEALER->0xff9591;case TANK->0;case SNIPER->0x0084ff;default->0x8dffd3;};
                p.level().sendParticles(new DustColorTransitionOptions(color,0x999999,1),at.x,at.y,at.z,1,.5,.5,.5,0);}
            // Source uses one selector cell, not a radius around every sample.
            boolean entity=!p.level().getEntitiesOfClass(LivingEntity.class,cell(at),e->e!=p&&eligible(e)).isEmpty();
            boolean block=!p.level().getBlockState(BlockPos.containing(at)).isAir();
            if(entity)spawn(p,p.level(),academy?at:at.subtract(direction),kind);
            if(block){var b=BlockPos.containing(at.subtract(direction));spawn(p,p.level(),new Vec3(b.getX()+.5,b.getY(),b.getZ()+.5),kind);}
            if(entity||block)return;
        }
    }
    private static AABB cell(Vec3 at){return new AABB(at.x,at.y,at.z,at.x+1,at.y+1,at.z+1);}
    private final class Fish extends Silverfish {
        final UUID author;final Kind kind;final LbcIndirectKills.Owner cause;int timer=8;boolean retired;
        Fish(ServerLevel level,ServerPlayer p,Kind kind){super(EntityTypes.SILVERFISH,level);this.author=p.getUUID();this.kind=kind;
            cause=kind==Kind.WALL?game.items().indirectKills().wildCause(p):game.items().indirectKills().coldCause(p,false);}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void setTarget(LivingEntity target){if(target!=null&&author!=null&&!enemy(this,target))target=null;super.setTarget(target);}
        @Override public void tick(){if(!valid(this)){retire(this);return;}if(!frozen(this))super.tick();}
        @Override public boolean doHurtTarget(ServerLevel l,Entity target){
            if(!(target instanceof LivingEntity living)||!impact(this,living))return false;
            // Same native Mob melee operations, with captured cause before ServerPlayer's admission.
            var weapon=getWeaponItem();var damage=game.items().indirectKills().captureDamage(weapon.getDamageSource(this),cause);
            float amount=(float)getAttributeValue(Attributes.ATTACK_DAMAGE);
            amount=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(l,weapon,target,damage,amount);
            amount+=weapon.getItem().getAttackDamageBonus(target,amount,damage);var oldMovement=target.getDeltaMovement();
            boolean hit=target.hurtServer(l,damage,amount);
            if(hit){causeExtraKnockback(target,getKnockback(target,damage),oldMovement,damage,amount,true);
                weapon.hurtEnemy(living,this);net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(l,target,damage);setLastHurtMob(target);playAttackSound();}
            postPiercingAttack();return hit;
        }
        @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean killedByPlayer){
            if(retired)return;
            if(kind==Kind.WALL){for(var stack:game.lbc().rollTable(level,position(),this,"lbc:hedgehog_wall"))spawnAtLocation(level,stack);}
            else super.dropCustomDeathLoot(level,source,killedByPlayer);
        }
    }
    public Silverfish spawnHelper(ServerPlayer p,ServerLevel level,Vec3 at,Kind kind){return spawn(p,level,at,kind);}
    private Fish spawn(ServerPlayer p,ServerLevel level,Vec3 at,Kind kind){
        if(!(kind==Kind.WALL?wallAuthorValid(p):ownerValid(p))||game.items().godHedgehog().blocksSilverfish(level)||!resident(level,at)||suppressed(level,at))return null;
        var f=new Fish(level,p,kind);f.setPos(at);f.entityTags().add(TAG);
        String legacy=switch(kind){case ORDINARY->"hedgehog";case WALL->"baffled_wall";default->"hedgehog_"+kind.name().toLowerCase(java.util.Locale.ROOT);};
        f.entityTags().add(legacy);if(kind!=Kind.ORDINARY&&kind!=Kind.WALL)f.entityTags().add("hedgehog_upgraded");
        if(kind!=Kind.TANK)f.entityTags().add("lucky_mob");
        f.setNoAi(kind!=Kind.TANK&&kind!=Kind.WALL);f.setPersistenceRequired();
        f.setCustomName(kind==Kind.WALL?Component.translatable("grand_baffled_wall").withStyle(ChatFormatting.DARK_RED).withStyle(s->s.withItalic(false)):
                Component.literal(switch(kind){case HEALER->"HealingHedgehog";case TANK->"HeavyHedgehog";case SNIPER->"SniperHedgehog";case LASER->"LaseringHedgehog";default->"BaffledHedgehog";}));
        if(kind==Kind.TANK){attribute(f,Attributes.MAX_HEALTH,100);attribute(f,Attributes.ATTACK_DAMAGE,15);attribute(f,Attributes.KNOCKBACK_RESISTANCE,1000);attribute(f,Attributes.ATTACK_KNOCKBACK,1);f.setHealth(100);}
        if(kind==Kind.WALL){attribute(f,Attributes.MAX_HEALTH,30);attribute(f,Attributes.FOLLOW_RANGE,200);attribute(f,Attributes.KNOCKBACK_RESISTANCE,100);
            attribute(f,Attributes.ATTACK_DAMAGE,10);attribute(f,Attributes.ARMOR,10);attribute(f,Attributes.ARMOR_TOUGHNESS,10);f.setHealth(30);}
        fish.add(f);game.items().markCurrentItemEpoch(f);game.items().timeStop().prepareFrozen(f);
        if(!level.addFreshEntity(f)||f.isRemoved()){retire(f);return null;}
        if(kind!=Kind.WALL)LbcOverload.sound(p,at,"minecraft:entity.silverfish.ambient",1,1.3f,0);return f;
    }
    private static void attribute(LivingEntity e,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> key,double amount){var a=e.getAttribute(key);if(a!=null)a.setBaseValue(amount);}
    /** Source wall towers retain native passenger relationships and gravity. */
    public int spawnWall(ServerLevel level,Vec3 at,ServerPlayer author){return spawnWall(level,at,author,author.getLookAngle());}
    private boolean wallAuthorValid(ServerPlayer author){return author!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(author)&&!game.isObserver(author);}
    public int spawnWall(ServerLevel level,Vec3 at,ServerPlayer author,Vec3 direction){
        if(!wallAuthorValid(author)||!resident(level,at)||suppressed(level,at))return 0;
        var forward=direction.normalize();var left=new Vec3(forward.z,0,-forward.x).normalize();int count=0;
        for(int side:new int[]{1,2,-1,-2,0}){
            var point=at.add(left.scale(side)).add(forward);Fish previous=null;
            for(int i=0;i<13;i++){
                var next=spawn(author,level,point,Kind.WALL);if(next==null)break;count++;
                if(previous==null)next.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,-1,1,false,false),author);
                else next.startRiding(previous,true,true);previous=next;
            }
        }
        for(var p:game.server().getPlayerList().getPlayers())p.sendSystemMessage(Component.translatable("baffled_awakened").withStyle(ChatFormatting.DARK_RED));
        if(wallCount(level)>=150)game.items().godHedgehog().summon(level,at,author);
        return count;
    }
    public int wallCount(ServerLevel level){int n=0;for(var e:level.getAllEntities())if(e instanceof Silverfish&&e.isAlive()&&e.entityTags().contains("baffled_wall"))n++;return n;}
    private List<LivingEntity> nearby(Fish f,double r){return ((ServerLevel)f.level()).getEntitiesOfClass(LivingEntity.class,f.getBoundingBox().inflate(r),e->e!=f&&e.position().distanceToSqr(f.position())<=r*r&&eligible(e));}
    private LivingEntity nearest(Fish f,List<LivingEntity> candidates,boolean friendly){return candidates.stream().filter(e->friendly?allied(f,e):enemy(f,e)).min(java.util.Comparator.comparingDouble(e->e.distanceToSqr(f))).orElse(null);}
    private void active(Fish f){
        if(f.kind==Kind.WALL||f.kind==Kind.TANK){
            var target=f.getTarget();if(target!=null&&!enemy(f,target))f.setTarget(null);
            if(f.getTarget()==null)f.setTarget(nearest(f,nearby(f,f.kind==Kind.WALL?200:16),false));
        }
        if(f.kind==Kind.TANK)return;
        if(f.kind==Kind.WALL){if(f.getRandom().nextFloat()<.005f){var target=nearest(f,nearby(f,10),false);if(target!=null)beam(f,target,Kind.WALL);}return;}
        if(f.kind!=Kind.LASER){if(f.timer==0)f.timer=40;f.timer--;if(f.timer>0)return;if(f.kind==Kind.SNIPER&&f.getRandom().nextFloat()>=.2f)return;}
        var candidates=nearby(f,switch(f.kind){case SNIPER->100;case HEALER->19;case LASER->13;default->20;});
        var p=player(f.author);LivingEntity target;
        if(f.kind==Kind.HEALER){
            if(candidates.stream().noneMatch(e->!(e instanceof Silverfish)&&allied(f,e)&&e.distanceToSqr(f)<=19*19))return;
            target=nearest(f,candidates,true);
        }else{
            double range=switch(f.kind){case ORDINARY->15;case DEFAULT->17;case SNIPER->95;default->10;};
            if(f.kind==Kind.DEFAULT&&(p==null||p.distanceToSqr(f)>17*17))return;
            if(f.kind==Kind.SNIPER&&candidates.stream().noneMatch(e->allied(f,e)&&e.distanceToSqr(f)<=95*95))return;
            target=nearest(f,candidates.stream().filter(e->e.distanceToSqr(f)<=range*range).toList(),false);
        }
        if(target==null)return;if(f.kind==Kind.ORDINARY)shoot(f,target);else beam(f,target,f.kind);
    }
    private void damage(Fish f,LivingEntity target,float amount,boolean laser){var p=player(f.author);if(p==null&&f.kind!=Kind.WALL||!impact(f,target))return;
        var level=(ServerLevel)f.level();var type=laser?DamageTypes.GENERIC:DamageTypes.ARROW;
        var source=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),f,p==null?f:p);
        target.hurtServer(level,game.items().indirectKills().captureDamage(source,f.cause),amount);}
    private void beam(Fish f,LivingEntity target,Kind kind){
        var p=player(f.author);if(p==null&&f.kind!=Kind.WALL)return;var level=(ServerLevel)f.level();var from=f.getEyePosition();var direction=target.getEyePosition().subtract(from).normalize();
        LbcOverload.sound(level,from,"minecraft:entity.silverfish.hurt",1,1,0,0);
        if(kind==Kind.SNIPER)LbcOverload.sound(p,from,"minecraft:item.trident.return",5,1,0);
        int max=kind==Kind.SNIPER?300:kind==Kind.LASER?30:50;int color=kind==Kind.HEALER?0xffdcdc:kind==Kind.SNIPER?0xbeb3ff:kind==Kind.LASER?0x3fffbf:0xbdbdbd;
        for(int i=0;i<=max;i++){
            var at=from.add(direction.scale(i*.3));if(!resident(level,at)||suppressed(level,at))return;
            level.sendParticles(new DustParticleOptions(color,kind==Kind.SNIPER?3:1),at.x,at.y,at.z,1,0,0,0,0);
            var targets=level.getEntitiesOfClass(LivingEntity.class,cell(at),e->kind==Kind.HEALER?e!=f&&eligible(e)&&allied(f,e):impact(f,e));
            if(!targets.isEmpty()){
                var hit=targets.getFirst();
                if(kind==Kind.HEALER)game.items().indirectKills().withCause(hit,f.cause,()->MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(level,f,p,hit,0,1));
                else{damage(f,hit,kind==Kind.SNIPER?20:kind==Kind.LASER?.02f:4,kind==Kind.LASER);if(kind!=Kind.LASER)return;}
            }
            if(kind!=Kind.LASER&&!level.getBlockState(BlockPos.containing(at)).isAir())return;
        }
    }
    /** The same attack boundary used after the source random wall/Sniper admission. */
    public void fireBeam(Silverfish helper,LivingEntity target){if(helper instanceof LbcHedgehog.Fish f&&valid(f)&&!frozen(f))beam(f,target,f.kind);}
    private final class HelperArrow extends Arrow {
        final UUID author,helper;final LbcIndirectKills.Owner cause;
        HelperArrow(ServerLevel level,ServerPlayer p,Fish helper){super(level,helper,new ItemStack(Items.ARROW),null);author=p.getUUID();this.helper=helper.getUUID();cause=helper.cause;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override protected boolean canHitEntity(Entity e){var p=getOwner() instanceof ServerPlayer current?current:player(author);if(e.getUUID().equals(helper)||p==null)return false;
            return super.canHitEntity(e)&&(LbcGreed.active(e)||game.items().wands().validTarget(e)&&(!(e instanceof ServerPlayer)||game.items().wands().projectileTarget(p,e)));}
        @Override protected void onHitBlock(BlockHitResult hit){super.onHitBlock(hit);discard();}
        @Override public void tick(){if(!game.playing()||!ownerValid(player(author))||!game.items().currentEpoch(this)||!resident((ServerLevel)level(),position())
                ||suppressed((ServerLevel)level(),position())){discard();return;}if(!frozen(this))super.tick();}
    }
    private void shoot(Fish f,LivingEntity target){var p=player(f.author);if(p==null)return;
        var arrow=new HelperArrow((ServerLevel)f.level(),p,f);var arrowTag=new CompoundTag();arrowTag.putByte("PierceLevel",(byte)100);
        arrow.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),arrowTag));arrow.entityTags().add(ARROW);arrow.entityTags().add("magic");
        arrow.setPos(f.getEyePosition());arrow.setDeltaMovement(target.getEyePosition().subtract(f.getEyePosition()).normalize().scale(2));
        // Native projectile owner is the accepted caster; helper remains the direct firing body.
        arrow.setOwner(p);game.items().markCurrentItemEpoch(arrow);arrows.add(arrow);game.items().timeStop().prepareFrozen(arrow);
        if(!p.level().addFreshEntity(arrow))arrow.discard();
        var at=f.position();p.level().sendParticles(new DustParticleOptions(0xcccccc,1),at.x,at.y,at.z,30,.2,.2,.2,1);
        LbcOverload.sound(p,at,"minecraft:entity.arrow.shoot",1,1,0);
    }
    public void tick(){
        if(!game.server().tickRateManager().runsNormally())return;
        for(var s:List.copyOf(silverfish)){
            if(s.isRemoved()||!s.isAlive()){silverfish.remove(s);continue;}
            if(game.playing()&&!frozen(s)&&resident((ServerLevel)s.level(),s.position())&&s.getRandom().nextDouble()<1e-13)sneeze(s);
        }
        for(var f:List.copyOf(fish)){if(!valid(f)){retire(f);continue;}if(!f.isAlive())continue;if(!frozen(f))active(f);}
        arrows.removeIf(Entity::isRemoved);
        for(var e:List.copyOf(children.keySet())){
            var child=children.get(e);if(e.isRemoved()){children.remove(e);continue;}
            if(!game.playing()||!game.items().currentEpoch(child.epoch)||!resident((ServerLevel)e.level(),e.position())){children.remove(e);e.discard();}
        }
        tickBlasts();
    }
    private void retire(Fish f){f.retired=true;fish.remove(f);f.discard();}
    public void retireSilverfish(ServerLevel level,boolean all){
        var snapshot=new ArrayList<Silverfish>();for(var e:level.getAllEntities())if(e instanceof Silverfish s)snapshot.add(s);
        for(var e:snapshot)if(all||managed(e)){if(e instanceof LbcHedgehog.Fish f)retire(f);else e.discard();}
    }
    /** (user) Boss supremacy removes every other silverfish in its dimension. */
    public void eraseForBoss(ServerLevel level,Entity boss){
        var snapshot=new ArrayList<Silverfish>();for(var e:level.getAllEntities())if(e instanceof Silverfish s&&e!=boss)snapshot.add(s);
        for(var e:snapshot){if(e instanceof LbcHedgehog.Fish f)retire(f);else e.discard();}
    }
    public ServerPlayer owner(Entity e){if(e==blastSource)return blastAuthor;var child=children.get(e);if(child!=null)return child.author;if(e instanceof LbcHedgehog.Fish f)return player(f.author);if(e instanceof LbcHedgehog.HelperArrow a)return player(a.author);return null;}
    public void loaded(Entity e){if(managed(e)&&!fish.contains(e)&&!arrows.contains(e)&&!children.containsKey(e)){e.discard();return;}if(e instanceof Silverfish s)silverfish.add(s);}
    public void unloaded(Entity e){silverfish.remove(e);
        // Native TNT discards itself immediately before its synchronous explosion; keep that cause
        // until the next service tick so blast and chained-block callbacks still see its lineage.
        if(children.containsKey(e)&&e.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK){children.remove(e);e.discard();}
        if(e instanceof LbcHedgehog.Fish f)retire(f);if(e instanceof LbcHedgehog.HelperArrow a){arrows.remove(a);a.discard();}}
    public void forget(ServerPlayer p){forget(p.getUUID());}
    public void forget(UUID id){retireOwned(id,false);}
    public void retireOwned(UUID id,boolean includeWalls){for(var f:List.copyOf(fish))if(f.author.equals(id)&&(includeWalls||f.kind!=Kind.WALL))retire(f);for(var a:List.copyOf(arrows))if(a.author.equals(id)){a.discard();arrows.remove(a);}}
    public void stop(){for(var f:List.copyOf(fish))retire(f);for(var a:arrows)a.discard();arrows.clear();blasts.clear();for(var child:children.keySet())child.discard();children.clear();}
    /** Public deterministic boundary lets checks trigger the real astronomically rare consequence. */
    public void sneeze(Silverfish source){
        if(!game.playing()||source.isRemoved()||!(source.level() instanceof ServerLevel level)||frozen(source))return;
        var at=source.position();var causal=owner(source);if(causal==null)causal=game.items().godHedgehog().owner(source);
        var acceptedCause=causal==null?game.items().godHedgehog().cause(source):game.items().indirectKills().wildCause(causal);
        for(var p:game.server().getPlayerList().getPlayers())p.sendSystemMessage(Component.translatableWithFallback("skywars2.hedgehog.sneeze","Ёжик чихнул.").withStyle(ChatFormatting.DARK_RED));
        for(var sound:List.of("minecraft:entity.dragon_fireball.explode","minecraft:entity.generic.explode","minecraft:item.firecharge.use","minecraft:entity.firework_rocket.large_blast","minecraft:entity.firework_rocket.large_blast_far"))
            LbcOverload.sound(level,at,sound,100,0,0,0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,at.x,at.y+10,at.z,500,15,10,15,0);
        game.items().nuclearVisuals().show(level,at,12,false);
        blasts.addLast(new BlastBatch(level,at,source,causal,acceptedCause,game.items().epoch()));
        tickBlasts();
    }
    /** Native explosion callbacks retain the original accepted event even after its fish has died. */
    public LbcIndirectKills.Owner cause(Entity source){var child=children.get(source);return source==blastSource?blastCause:child==null?null:child.cause;}
    public boolean executingSneeze(ServerLevel level){return blastLevel==level;}
    public boolean explosionTarget(ServerLevel level,Entity victim){return !executingSneeze(level)||game.items().wands().validTarget(victim);}
    public boolean explosionTarget(Entity source,Entity victim){return source==null||source!=blastSource&&!children.containsKey(source)||game.items().wands().validTarget(victim);}
    public void inheritSneezeChild(ServerLevel level,Entity child){
        if(!executingSneeze(level))return;child.entityTags().add(CHILD);game.items().markCurrentItemEpoch(child);
        children.put(child,new Child(blastAuthor,blastCause,game.items().epoch()));game.items().timeStop().prepareFrozen(child);
    }
    public void inheritSneezeChild(Entity parent,Entity child){
        var lineage=children.get(parent);if(lineage==null)return;child.entityTags().add(CHILD);game.items().markCurrentItemEpoch(child);
        children.put(child,lineage);game.items().timeStop().prepareFrozen(child);
    }
    public List<Entity> children(){return List.copyOf(children.keySet());}
    public int pendingBlasts(){int total=0;for(var b:blasts)total+=SNEEZE_OFFSETS.length-b.next;return total;}
    public long completedBlasts(){return completedBlasts;}
    public record BlastView(Vec3 origin,UUID owner,int remaining,String epoch){}
    public List<BlastView> blastViews(){return blasts.stream().map(b->new BlastView(b.origin,b.cause==null?null:b.cause.player(),SNEEZE_OFFSETS.length-b.next,b.epoch)).toList();}
    public void tickBlasts(){
        if(!game.server().tickRateManager().runsNormally()||blasts.isEmpty())return;
        long deadline=System.nanoTime()+4_000_000;int processed=0,examined=0,available=blasts.size();
        while(!blasts.isEmpty()&&processed<4&&(processed==0||System.nanoTime()<deadline)&&examined<Math.max(available,4)){
            var batch=blasts.removeFirst();examined++;
            if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(batch.epoch))continue;
            if(game.items().timeStop().stopped(batch.level)){blasts.addLast(batch);continue;}
            var shift=SNEEZE_OFFSETS[batch.next];var point=batch.origin.add(shift[0],shift[1],shift[2]);
            if(!resident(batch.level,point)){blasts.addLast(batch);continue;}
            if(resident(batch.level,point)){
                var oldSource=blastSource;var oldCause=blastCause;var oldLevel=blastLevel;var oldAuthor=blastAuthor;
                blastSource=batch.source;blastCause=batch.cause;blastLevel=batch.level;blastAuthor=batch.author;
                try{
                    var nativeDamage=batch.level.damageSources().explosion(batch.source,batch.author);
                    var damage=game.items().indirectKills().captureDamage(nativeDamage,batch.cause);
                    // Null explosion actor prevents vanilla from excluding the fish itself from exposure.
                    game.items().indirectKills().trapChanges(batch.level,batch.cause,()->batch.level.explode(null,damage,null,point.x,point.y,point.z,20,false,Level.ExplosionInteraction.MOB));
                    completedBlasts++;batch.next++;
                }finally{blastSource=oldSource;blastCause=oldCause;blastLevel=oldLevel;blastAuthor=oldAuthor;}
            }
            processed++;if(batch.next<SNEEZE_OFFSETS.length)blasts.addLast(batch);
        }
    }
    public int trackedCount(){return fish.size()+arrows.size()+children.size();}
    public record View(UUID entity,UUID owner,Kind kind,Vec3 point,int timer,boolean passenger){}
    public List<View> views(){return fish.stream().map(f->new View(f.getUUID(),f.author,f.kind,f.position(),f.timer,f.isPassenger())).toList();}
    public List<Silverfish> helpers(){return List.copyOf(fish);}
    public List<Arrow> projectiles(){return List.copyOf(arrows);}
}
