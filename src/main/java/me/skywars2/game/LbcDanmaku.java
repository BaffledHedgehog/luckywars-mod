package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Authored Danmaku display raycasters; no native projectile impulse or explosion. */
public final class LbcDanmaku {
    public static final Item ITEM=item(false), ACADEMY=item(true);
    public static final Item GRIMOIRE=grimoireItem();
    private static final String TAG="skywars2.danmaku";
    private final GameController game;
    private final LbcMana mana;
    private final List<Card> cards=new ArrayList<>();
    private final List<Bomb> bombs=new ArrayList<>();
    private final Set<Entity> spawning=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerPlayer,List<LivingEntity>> targets=new IdentityHashMap<>();
    private final Map<UUID,EnumMap<InteractionHand,External>> external=new HashMap<>();
    private record External(boolean academy,InteractionHand hand,int ticks) {}
    LbcDanmaku(GameController game,LbcMana mana){this.game=game;this.mana=mana;var holder=ScoreHolder.forNameOnly("danmaku_staff_mana_use");var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");if(objective==null||board.getPlayerScoreInfo(holder,objective)==null)mana.score(holder,"lbc.math").set(1000);}
    public static void register(){}
    private static Item item(boolean academy){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(academy?"skywars2:academy_danmaku_staff":"skywars2:danmaku_staff"));
        var tag=new CompoundTag();for(var flag:List.of("transformable","spellwithcooldown","staff","magicwand","danmaku_staff"))tag.putInt(flag,1);
        if(academy){tag.putInt("upgraded",1);tag.putInt("mode",1);tag.putInt("upgradable",0);tag.putInt("upgrade_available",1);}
        var model=Identifier.parse("lbc:danmaku");
        var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(1).overrideDescription("danmaku_wand")
                .component(DataComponents.ITEM_MODEL,model).component(DataComponents.CUSTOM_DATA,CustomData.of(tag))
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(academy?"lbc:tier_s":"lbc:tier_a"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
        });
    }
    private static Item grimoireItem(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:danmaku_grimoire"));
        var model=Identifier.parse("lbc:spelllib/danmaku");
        var tag=new CompoundTag();for(var flag:List.of("transformable","gadget","libspell","unresolved_grimoire","spellwithcooldown"))tag.putInt(flag,1);
        tag.putInt("mana",1200);tag.putInt("spellid",159);
        var lore=new ArrayList<Component>();
        lore.add(Component.translatable("type").withStyle(style->style.withColor(0x55ffff).withItalic(false)).append(Component.translatable("libspell").withStyle(style->style.withColor(0x55ffff).withItalic(false))));
        lore.add(Component.empty());
        for(var keyName:List.of("danmaku_wand.lore.1","danmaku_wand.lore.2","danmaku_wand.lore.3","danmaku_wand.lore.4","danmaku_wand.lore.5","danmaku_wand.lore.6","danmaku_wand.lore.7"))lore.add(Component.translatable(keyName).withStyle(style->style.withColor(0xffffff).withItalic(false)));
        lore.add(Component.translatable("rarity").withStyle(style->style.withColor(0xffffff).withItalic(false))
                .append(Component.translatable("rare").withStyle(style->style.withColor(0xffff55).withItalic(false))));
        var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(99).overrideDescription("libspell_danmaku")
                .component(DataComponents.ITEM_MODEL,model).component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_c"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(tag))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("libspell_danmaku").withStyle(style->style.withColor(0xffffff).withItalic(false)))
                .component(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.SNOWBALL,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer p&&game.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isGrimoire(ItemStack stack){
        var data=LbcFartus.data(stack);return !stack.isEmpty()&&(stack.is(GRIMOIRE)||data.getIntOr("libspell",0)==1&&data.getIntOr("spellid",0)==159);
    }
    /** The grimoire stores mana metadata for other mechanics; its consumed cast never spends mana. */
    public boolean useGrimoire(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!isGrimoire(stack)||!validOwner(p)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||game.items().actionsBlocked(p)||game.items().wands().inputTimeStopped(p)||game.items().magic().suppressed(p)
                ||game.items().magic().suppressedAt(p.level(),p.getEyePosition()))return false;
        if(p.isShiftKeyDown())castBomb(p,hand,ItemStack.EMPTY);
        else queueExternal(p,hand,false);
        p.level().playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.SNOWBALL_THROW,
                net.minecraft.sounds.SoundSource.NEUTRAL,.5f,.4f/(p.level().getRandom().nextFloat()*.4f+.8f));
        p.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(GRIMOIRE));
        stack.consume(1,p);
        return true;
    }
    private boolean castBomb(ServerPlayer p,InteractionHand hand,ItemStack trialStack){
        int charge=mana.score(p,"danmaku_score").get();if(charge<50)return false;
        var a=display(p,p.position(),false,true);var b=display(p,p.position(),false,true);
        if(a==null||b==null){if(a!=null)a.discard();if(b!=null)b.discard();return false;}
        a.entityTags().add("act");scale(b,-1);bombs.add(new Bomb(p,a,b,charge));mana.score(p,"danmaku_score").set(0);
        if(charge>=1500&&!trialStack.isEmpty()&&!isAcademy(trialStack)&&LbcAcademy.trial(trialStack))game.items().academy().ready(p,hand,trialStack);
        LbcOverload.sound(p,p.position(),"minecraft:lbcsounds.bomb",2,1,0);game.items().wands().wandCasted(p);return true;
    }
    public static boolean isItem(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||stack.is(ACADEMY)||LbcFartus.data(stack).getIntOr("danmaku_staff",0)==1);}
    public static boolean isAcademy(ItemStack stack){return isItem(stack)&&(stack.is(ACADEMY)||LbcFartus.data(stack).getIntOr("upgraded",0)==1);}
    public static boolean managed(Entity e){return e.entityTags().contains(TAG);}
    static final class Card {
        ServerPlayer owner;final Display.ItemDisplay display;final boolean homing;Vec3 direction;int age;
        Card(ServerPlayer owner,Display.ItemDisplay display,Vec3 direction,boolean homing){this.owner=owner;this.display=display;this.direction=direction.normalize();this.homing=homing;}
    }
    static final class Bomb {
        final ServerPlayer owner;final Display.ItemDisplay positive,negative;final int charge;int age;
        Bomb(ServerPlayer owner,Display.ItemDisplay positive,Display.ItemDisplay negative,int charge){this.owner=owner;this.positive=positive;this.negative=negative;this.charge=charge;}
        int radius(){return (int)((int)(age*1.2)*.05);}
    }
    List<Card> cards(){return List.copyOf(cards);} List<Bomb> bombs(){return List.copyOf(bombs);}
    int count(){return cards.size()+bombs.size();}
    int remaining(ServerPlayer p){return mana.score(p,"danmaku").get();}
    public ServerPlayer owner(Entity e){for(var c:cards)if(c.display==e)return c.owner;for(var b:bombs)if(b.positive==e||b.negative==e)return b.owner;return null;}
    private boolean validOwner(ServerPlayer p){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()&&game.canUseGameplayItems(p)&&p.level()==game.maps().level();}
    public boolean use(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!isItem(stack)||!validOwner(p)||game.items().actionsBlocked(p)||game.items().wands().inputTimeStopped(p)||game.items().magic().suppressed(p)||game.items().magic().suppressedAt(p.level(),p.getEyePosition()))return false;
        if(p.isShiftKeyDown())return castBomb(p,hand,stack);
        int cost=mana.score(ScoreHolder.forNameOnly("danmaku_staff_mana_use"),"lbc.math").get();
        if(!mana.trySpendWand(p,cost))return false;
        mana.score(p,"danmaku").set((int)Math.min(Integer.MAX_VALUE,(long)remaining(p)+20));game.items().wands().wandCasted(p);return true;
    }
    /** Consumed casts retain their actual hand and continue independently of held staff input. */
    void queueExternal(ServerPlayer p,InteractionHand hand,boolean academy){
        var hands=external.computeIfAbsent(p.getUUID(),key->new EnumMap<>(InteractionHand.class));
        var prior=hands.get(hand);hands.put(hand,new External(academy,hand,prior==null?20:(int)Math.min(Integer.MAX_VALUE,(long)prior.ticks+20)));
    }
    int externalRemaining(ServerPlayer p){var hands=external.get(p.getUUID());if(hands==null)return 0;return (int)Math.min(Integer.MAX_VALUE,hands.values().stream().mapToLong(External::ticks).sum());}
    public void redirectCursedCards(){for(var c:List.copyOf(cards))game.items().homing().redirect(c);}
    public void tick(){
        targets.clear();
        for(var p:game.server().getPlayerList().getPlayers()){
            if(!validOwner(p)){if(remaining(p)>0||external.containsKey(p.getUUID()))forget(p);continue;}
            int n=remaining(p);if(game.items().wands().inputTimeStopped(p)||game.items().magic().suppressed(p)||game.items().magic().suppressedAt(p.level(),p.getEyePosition()))continue;
            var hand=(n&1)==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;var stack=p.getItemInHand(hand);
            if(n>0){if(isItem(stack))emit(p,hand,isAcademy(stack),phase(n,hand));mana.score(p,"danmaku").set(n-1);}
            var extras=external.get(p.getUUID());if(extras!=null){
                for(var extra:List.copyOf(extras.values())){
                    if(((extra.ticks&1)==0)==(extra.hand==InteractionHand.MAIN_HAND))emit(p,extra.hand,extra.academy,phase(extra.ticks,extra.hand));
                    if(extra.ticks<=1)extras.remove(extra.hand);else extras.put(extra.hand,new External(extra.academy,extra.hand,extra.ticks-1));
                }
                if(extras.isEmpty())external.remove(p.getUUID());
            }
        }
        for(var c:List.copyOf(cards))tick(c);
        for(var b:List.copyOf(bombs))tick(b);
    }
    private static int phase(int n,InteractionHand hand){return ((20-n%20)%20)/2;}
    private boolean valid(ServerPlayer p,Display.ItemDisplay d){return validOwner(p)&&!d.isRemoved()&&game.items().currentEpoch(d)&&p.level()==d.level()&&d.level().hasChunkAt(d.blockPosition())&&!game.items().magic().suppressedAt((ServerLevel)d.level(),d.position());}
    Card card(ServerPlayer p,Vec3 pos,Vec3 direction,boolean homing){var d=display(p,pos,homing,false);if(d==null)return null;var c=new Card(p,d,direction,homing);cards.add(c);orient(c);return c;}
    private Display.ItemDisplay display(ServerPlayer p,Vec3 pos,boolean homing,boolean bomb){
        if(!p.level().hasChunkAt(BlockPos.containing(pos)))return null;
        var d=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,p.level());
        try{var nbt=TagParser.parseCompoundFully("{teleport_duration:"+(bomb?30:1)+",interpolation_duration:"+(bomb?30:1)+",start_interpolation:0,shadow_radius:0f,shadow_strength:0f,item_display:'"+(bomb?"none":"fixed")+"',view_range:"+(bomb?"500000f":"10f")+",transformation:{translation:[0f,0f,0f],left_rotation:"+(bomb?"[0f,0f,0f,1f]":"[1f,0f,0f,1f]")+",right_rotation:[0f,0f,0f,1f],scale:"+(bomb?"[1f,1f,1f]":"[.5f,.5f,.5f]")+"},item:{id:'"+(bomb?"minecraft:red_stained_glass":"minecraft:repeating_command_block")+"',count:1"+(bomb?"":",components:{'minecraft:item_model':'lbc:card_"+(homing?2:1)+"'}")+"}}");d.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),nbt));}catch(Exception e){throw new IllegalStateException(e);}
        d.setPos(pos);d.setNoGravity(true);d.entityTags().add(TAG);d.entityTags().add("magic");d.entityTags().add("slowraycast");d.entityTags().add("player");d.entityTags().add(bomb?"danmaku_bomb":"danmaku");if(bomb)d.entityTags().add("cant_be_homing");if(homing)d.entityTags().add("homing");game.items().markCurrentItemEpoch(d);game.items().timeStop().prepareFrozen(d);spawning.add(d);try{return p.level().addFreshEntity(d)&&!d.isRemoved()?d:null;}finally{spawning.remove(d);}
    }
    private static void scale(Display.ItemDisplay d,float value){d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f(value),new org.joml.Quaternionf()));}
    private static void orient(Card c){var v=c.direction;c.display.setYRot((float)Math.toDegrees(Math.atan2(-v.x,v.z)));c.display.setXRot((float)-Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))));}
    private void tick(Card c){
        if(!valid(c.owner,c.display)){remove(c);return;}if(game.items().timeStop().frozen(c.display)||c.display.entityTags().contains("time_stopped"))return;
        if(++c.age>60){remove(c);return;}
        for(int i=0;i<3;i++){
            if(c.homing){
                var candidates=targets.computeIfAbsent(c.owner,p->{var out=new ArrayList<LivingEntity>();for(var e:((ServerLevel)p.level()).getAllEntities())if(e instanceof LivingEntity living&&game.items().wands().target(p,e))out.add(living);return out;});
                LivingEntity nearest=null;double ds=Double.POSITIVE_INFINITY;
                for(var e:candidates)if(e.isAlive()&&!e.isRemoved()&&game.items().wands().target(c.owner,e)){double d=e.distanceToSqr(c.display);if(d<ds){ds=d;nearest=e;}}
                if(nearest==null){if(c.age>=60)remove(c);return;}
                c.direction=c.direction.add(nearest.getEyePosition().subtract(c.display.position()).normalize().scale(.06)).normalize();orient(c);
            }
            var from=c.display.position();var to=from.add(c.direction.scale(.4));var level=(ServerLevel)c.display.level();
            if(!residentSegment(level,from,to)){remove(c);return;}
            var block=net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,c.display,
                    (ignored,pos)->level.getBlockState(pos).isAir()?null:net.minecraft.world.phys.shapes.Shapes.block().clip(from,to,pos),
                    ignored->net.minecraft.world.phys.BlockHitResult.miss(to,net.minecraft.core.Direction.UP,BlockPos.containing(to)));
            double obstacle=block.getType()==net.minecraft.world.phys.HitResult.Type.MISS?Double.POSITIVE_INFINITY:from.distanceToSqr(block.getLocation());
            var greed=LbcGreed.firstHit(level,from,to,0);
            var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,c.display,from,to,new AABB(from,to).inflate(1),e->e instanceof LivingEntity&&game.items().wands().projectileTarget(c.owner,e),0);
            double impact=hit==null?Double.POSITIVE_INFINITY:from.distanceToSqr(hit.getLocation());
            if(greed!=null&&(c.owner!=greed.getEntity()||!greed.getEntity().getBoundingBox().inflate(.2).contains(from))&&from.distanceToSqr(greed.getLocation())<=Math.min(obstacle,impact)){
                if(greed.getEntity() instanceof ServerPlayer player){c.owner=player;c.display.entityTags().add(LbcGreed.REFLECTED);c.direction=c.direction.scale(-1);c.display.setPos(greed.getLocation());orient(c);}else remove(c);return;
            }
            if(obstacle<=impact&&Double.isFinite(obstacle)){remove(c);return;}
            if(hit!=null){c.display.setPos(hit.getLocation());damage(c.owner,(LivingEntity)hit.getEntity(),c.display,false,.65f);mana.score(c.owner,"danmaku_score").set((int)Math.min(Integer.MAX_VALUE,(long)mana.score(c.owner,"danmaku_score").get()+3));remove(c);return;}
            c.display.setPos(to);
        }
        if(c.age>=60)remove(c);
    }

    private static boolean residentSegment(ServerLevel level,Vec3 from,Vec3 to){
        int minX=BlockPos.containing(from).getX()>>4,maxX=BlockPos.containing(to).getX()>>4;
        int minZ=BlockPos.containing(from).getZ()>>4,maxZ=BlockPos.containing(to).getZ()>>4;
        for(int x=Math.min(minX,maxX);x<=Math.max(minX,maxX);x++)for(int z=Math.min(minZ,maxZ);z<=Math.max(minZ,maxZ);z++)if(!level.hasChunkAt(new BlockPos(x<<4,0,z<<4)))return false;
        return true;
    }
    private void damage(ServerPlayer owner,LivingEntity victim,Entity direct,boolean magic,float amount){
        if(game.items().magic().suppressedAt((ServerLevel)victim.level(),victim.position()))return;
        var type=owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(magic?DamageTypes.MAGIC:DamageTypes.GENERIC);
        // Source cards omit an attacker: captured credit must not introduce an attacker impulse.
        // Bomb magic explicitly names the caster in the original damage command.
        var source=magic?new DamageSource(type,direct,owner):new DamageSource(type);
        var cause=game.items().indirectKills().coldCause(owner,true);
        game.items().indirectKills().withCause(victim,cause,()->victim.hurtServer((ServerLevel)victim.level(),source,amount));
    }

    private void tick(Bomb b){
        if(!valid(b.owner,b.positive)||!valid(b.owner,b.negative)){remove(b);return;}if(game.items().timeStop().frozen(b.positive)||b.positive.entityTags().contains("time_stopped"))return;
        b.age+=4;scale(b.positive,b.age*.2f);scale(b.negative,-b.age*.2f);
        int radius=b.radius();game.items().wands().clearDanmakuRays(b.owner,b.positive.position(),radius);for(var e:b.positive.level().getEntities(b.positive,b.positive.getBoundingBox().inflate(radius),e->e!=b.negative&&e.distanceToSqr(b.positive)<=radius*(double)radius)){
            if(hostileProjectile(b.owner,e)){e.discard();continue;}
            if(e instanceof LivingEntity living&&game.items().wands().target(b.owner,e))damage(b.owner,living,b.positive,true,1);
        }
        if(b.age>b.charge)remove(b);
    }
    private boolean hostileProjectile(ServerPlayer p,Entity e){
        if(managed(e)){var owner=owner(e);return owner!=null&&owner!=p&&!p.isAlliedTo(owner)&&cards.stream().anyMatch(c->c.display==e);}
        if(!game.items().wands().validRoleTarget(e,false)||LbcGreed.active(e)||p.isAlliedTo(e))return false;
        if(!(e instanceof Projectile)&&!e.entityTags().contains("mob_projectile")&&!e.entityTags().contains("slowraycast")&&!e.entityTags().contains("raycast")&&!(e instanceof net.minecraft.world.entity.item.FallingBlockEntity)&&!(e instanceof net.minecraft.world.entity.item.PrimedTnt)&&!((e.entityTags().contains("magic")||e.entityTags().contains("mecha"))&&(e instanceof Display||e instanceof net.minecraft.world.entity.Marker||e instanceof net.minecraft.world.entity.AreaEffectCloud)))return false;
        var source=LbcIndirectKills.playerSource(e);return source!=p&&(source==null||!p.isAlliedTo(source));
    }
    private void remove(Card c){cards.remove(c);c.display.discard();}
    private void remove(Bomb b){bombs.remove(b);b.positive.discard();b.negative.discard();}
    public void loaded(Entity e){if(managed(e)&&!spawning.contains(e)&&owner(e)==null)e.discard();}
    public void unloaded(Entity e){for(var c:List.copyOf(cards))if(c.display==e)remove(c);for(var b:List.copyOf(bombs))if(b.positive==e||b.negative==e)remove(b);}
    public void forget(ServerPlayer p){for(var c:List.copyOf(cards))if(c.owner==p)remove(c);for(var b:List.copyOf(bombs))if(b.owner==p)remove(b);mana.score(p,"danmaku").set(0);mana.score(p,"danmaku_score").set(0);external.remove(p.getUUID());}
    public void stop(){for(var c:List.copyOf(cards))remove(c);for(var b:List.copyOf(bombs))remove(b);for(var p:game.server().getPlayerList().getPlayers())forget(p);external.clear();targets.clear();}
    public MutableComponent hud(ServerPlayer p){
        var text=Component.empty();int ticks=mana.hudValue(p,"danmaku"),score=mana.hudValue(p,"danmaku_score");
        if(mana.visible(p)){var hands=external.get(p.getUUID());if(hands!=null)for(var extra:hands.values())ticks=Math.max(ticks,extra.ticks);}
        if(ticks>0)text.append(Component.literal("    \uE133 ").append(Component.literal(Integer.toString(ticks)).withColor(0xFFA1F2)));
        if(score>0)text.append(Component.literal("  \uE134 ").append(Component.literal(Integer.toString(score)).withColor(0xD494FF)));
        return text;
    }
    private record Pattern(double x,double y,double z,boolean local,float yaw,float pitch,boolean homing) {}
    private void emit(ServerPlayer p,InteractionHand hand,boolean academy,int phase){
        var base=p.getEyePosition().add(0,academy&&hand==InteractionHand.OFF_HAND?0:-.5,0);var look=p.getLookAngle();var left=Vec3.directionFromRotation(0,p.getYRot()-90);var up=look.cross(left);
        for(var s:PATTERNS[(academy?2:0)+(hand==InteractionHand.OFF_HAND?1:0)][phase]){
            var pos=s.local?base.add(left.scale(s.x)).add(up.scale(s.y)).add(look.scale(s.z)):base.add(s.x,s.y,s.z);
            card(p,pos,Vec3.directionFromRotation(p.getXRot()+s.pitch,p.getYRot()+s.yaw),s.homing);
        }
        LbcOverload.sound(p,p.position(),"minecraft:ui.cartography_table.take_result",.5f,2,0);
    }
    private static final Pattern[][][] PATTERNS={{{new Pattern(0.0,0.0,0.0,false,30.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,15.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,0.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-15.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-30.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,60.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-60.0f,-20.0f,true)},
{new Pattern(0.0,0.35,0.0,false,-20.0f,0.0f,false),new Pattern(0.0,0.35,0.0,false,-15.0f,0.0f,false)},
{new Pattern(0.0,0.35,0.0,false,-10.0f,0.0f,false),new Pattern(0.0,0.35,0.0,false,-5.0f,0.0f,false)},
{new Pattern(0.0,0.35,0.0,false,0.0f,0.0f,false),new Pattern(0.0,0.35,0.0,false,5.0f,0.0f,false)},
{new Pattern(0.0,0.35,0.0,false,10.0f,0.0f,false),new Pattern(0.0,0.35,0.0,false,15.0f,0.0f,false)},
{new Pattern(0.0,0.35,0.0,false,20.0f,0.0f,false),new Pattern(0.0,0.35,0.0,false,25.0f,0.0f,false)},
{new Pattern(0.0,0.2,0.0,false,30.0f,0.0f,false),new Pattern(0.0,0.2,0.0,false,15.0f,0.0f,false),new Pattern(0.0,0.2,0.0,false,0.0f,0.0f,false),new Pattern(0.0,0.2,0.0,false,-15.0f,0.0f,false),new Pattern(0.0,0.2,0.0,false,-30.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,60.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-60.0f,-20.0f,true)},
{new Pattern(0.0,0.1,0.0,false,23.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,13.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,-7.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,-17.0f,0.0f,false)},
{new Pattern(0.0,0.1,0.0,false,20.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,10.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,-10.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,-20.0f,0.0f,false)},
{new Pattern(0.0,0.1,0.0,false,17.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,7.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,-13.0f,0.0f,false),new Pattern(0.0,0.1,0.0,false,-23.0f,0.0f,false)}},
{{new Pattern(0.3,0.0,0.0,true,0.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,2.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,2.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,4.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,4.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,2.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,2.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,0.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,-2.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,-2.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,-4.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,-4.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,-2.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,-2.0f,0.0f,false)},
{new Pattern(0.3,0.0,0.0,true,0.0f,0.0f,false),new Pattern(-0.3,0.0,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.0,0.0,0.0,false,30.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,15.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,0.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-15.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-30.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,60.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-60.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,50.0f,-15.0f,true),new Pattern(0.0,0.0,0.0,false,-50.0f,-15.0f,true),new Pattern(0.0,0.0,0.0,false,40.0f,-15.0f,true),new Pattern(0.0,0.0,0.0,false,-40.0f,-15.0f,true),new Pattern(0.0,0.0,0.0,false,30.0f,-10.0f,true),new Pattern(0.0,0.0,0.0,false,-30.0f,-10.0f,true),new Pattern(0.0,0.0,0.0,false,20.0f,-10.0f,true),new Pattern(0.0,0.0,0.0,false,-20.0f,-10.0f,true)}},
{{new Pattern(0.0,0.0,0.0,false,0.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,90.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-90.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,180.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,45.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-45.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,135.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-135.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,10.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,100.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-80.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-170.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,35.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-55.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,125.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-145.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,20.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,110.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-160.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,25.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-65.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,115.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-155.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,30.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,120.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-60.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-150.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,15.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-75.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,105.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-165.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,40.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,130.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-50.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-140.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,5.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-85.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,95.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-175.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,50.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,140.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-40.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-130.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-5.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-95.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,85.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,175.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,60.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,150.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-30.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-120.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-15.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-105.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,75.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,165.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,70.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,160.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-20.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-110.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-25.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-115.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,65.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,155.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,80.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,170.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-10.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-100.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-35.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,-125.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,55.0f,-20.0f,true),new Pattern(0.0,0.0,0.0,false,145.0f,-20.0f,true)},
{new Pattern(0.0,0.0,0.0,false,40.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,35.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,30.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,25.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,20.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,15.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,10.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,5.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,0.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-40.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-35.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-30.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-25.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-20.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-15.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-10.0f,0.0f,false),new Pattern(0.0,0.0,0.0,false,-5.0f,0.0f,false)}},
{{new Pattern(0.0,0.547723,0.0,true,0.0f,0.0f,false),new Pattern(0.474342,0.273861,0.0,true,0.0f,0.0f,false),new Pattern(0.474342,-0.273861,0.0,true,0.0f,0.0f,false),new Pattern(0.0,-0.547723,0.0,true,0.0f,0.0f,false),new Pattern(-0.474342,-0.273861,0.0,true,0.0f,0.0f,false),new Pattern(-0.474342,0.273861,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.057253,0.544722,0.0,true,0.0f,0.0f,false),new Pattern(0.500369,0.222779,0.0,true,0.0f,0.0f,false),new Pattern(0.443117,-0.321943,0.0,true,0.0f,0.0f,false),new Pattern(-0.057253,-0.544722,0.0,true,0.0f,0.0f,false),new Pattern(-0.500369,-0.222779,0.0,true,0.0f,0.0f,false),new Pattern(-0.443117,0.321943,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.113878,0.535754,0.0,true,0.0f,0.0f,false),new Pattern(0.520915,0.169256,0.0,true,0.0f,0.0f,false),new Pattern(0.407037,-0.366498,0.0,true,0.0f,0.0f,false),new Pattern(-0.113878,-0.535754,0.0,true,0.0f,0.0f,false),new Pattern(-0.520915,-0.169256,0.0,true,0.0f,0.0f,false),new Pattern(-0.407037,0.366498,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.169256,0.520915,0.0,true,0.0f,0.0f,false),new Pattern(0.535754,0.113878,0.0,true,0.0f,0.0f,false),new Pattern(0.366498,-0.407037,0.0,true,0.0f,0.0f,false),new Pattern(-0.169256,-0.520915,0.0,true,0.0f,0.0f,false),new Pattern(-0.535754,-0.113878,0.0,true,0.0f,0.0f,false),new Pattern(-0.366498,0.407037,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.222779,0.500369,0.0,true,0.0f,0.0f,false),new Pattern(0.544722,0.057253,0.0,true,0.0f,0.0f,false),new Pattern(0.321943,-0.443117,0.0,true,0.0f,0.0f,false),new Pattern(-0.222779,-0.500369,0.0,true,0.0f,0.0f,false),new Pattern(-0.544722,-0.057253,0.0,true,0.0f,0.0f,false),new Pattern(-0.321943,0.443117,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.547723,0.0,0.0,true,0.0f,0.0f,false),new Pattern(0.273861,0.474342,0.0,true,0.0f,0.0f,false),new Pattern(-0.273861,0.474342,0.0,true,0.0f,0.0f,false),new Pattern(-0.547723,0.0,0.0,true,0.0f,0.0f,false),new Pattern(-0.273861,-0.474342,0.0,true,0.0f,0.0f,false),new Pattern(0.273861,-0.474342,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.321943,0.443117,0.0,true,0.0f,0.0f,false),new Pattern(0.544722,-0.057253,0.0,true,0.0f,0.0f,false),new Pattern(0.222779,-0.500369,0.0,true,0.0f,0.0f,false),new Pattern(-0.321943,-0.443117,0.0,true,0.0f,0.0f,false),new Pattern(-0.544722,0.057253,0.0,true,0.0f,0.0f,false),new Pattern(-0.222779,0.500369,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.366498,0.407037,0.0,true,0.0f,0.0f,false),new Pattern(0.535754,-0.113878,0.0,true,0.0f,0.0f,false),new Pattern(0.169256,-0.520915,0.0,true,0.0f,0.0f,false),new Pattern(-0.366498,-0.407037,0.0,true,0.0f,0.0f,false),new Pattern(-0.535754,0.113878,0.0,true,0.0f,0.0f,false),new Pattern(-0.169256,0.520915,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.407037,0.366498,0.0,true,0.0f,0.0f,false),new Pattern(0.520915,-0.169256,0.0,true,0.0f,0.0f,false),new Pattern(0.113878,-0.535754,0.0,true,0.0f,0.0f,false),new Pattern(-0.407037,-0.366498,0.0,true,0.0f,0.0f,false),new Pattern(-0.520915,0.169256,0.0,true,0.0f,0.0f,false),new Pattern(-0.113878,0.535754,0.0,true,0.0f,0.0f,false)},
{new Pattern(0.443117,0.321943,0.0,true,0.0f,0.0f,false),new Pattern(0.500369,-0.222779,0.0,true,0.0f,0.0f,false),new Pattern(0.057253,-0.544722,0.0,true,0.0f,0.0f,false),new Pattern(-0.443117,-0.321943,0.0,true,0.0f,0.0f,false),new Pattern(-0.500369,0.222779,0.0,true,0.0f,0.0f,false),new Pattern(-0.057253,0.544722,0.0,true,0.0f,0.0f,false)}}};
}
