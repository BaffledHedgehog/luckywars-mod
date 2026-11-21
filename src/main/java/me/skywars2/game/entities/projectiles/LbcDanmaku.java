package me.skywars2.game.entities.projectiles;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.game.GameController;
import me.skywars2.game.GameController.Stage;
import me.skywars2.game.academy.LbcAcademy;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.combat.LbcIndirectKills.Owner;
import me.skywars2.game.combat.MatchDamageSources.Ref;
import me.skywars2.game.effects.magic.LbcMagic;
import me.skywars2.game.effects.magic.LbcMana;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.items.wands.LbcFartus;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint;
import me.skywars2.game.match.checkpoints.MatchSpellCheckpoint.Links;
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
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;
import me.skywars2.game.academy.SauvojenMeditation;
import me.skywars2.game.combat.LbcAcceptedHits;
import me.skywars2.game.combat.LbcDamage;
import me.skywars2.game.skills.LbcPlanetoidSkill;
import me.skywars2.game.skills.clone.LbcCloneSkills;
import me.skywars2.game.skills.witch.LbcGreed;
import me.skywars2.mixin.combat.BlackHoleDeathProtectionAccess;

/** Authored Danmaku display raycasters; no native projectile impulse or explosion. */
public final class LbcDanmaku {
    public static final Item ITEM=item(false), ACADEMY=item(true);
    public static final Item GRIMOIRE=grimoireItem();
    private static final String TAG="skywars2.danmaku";
    private static final TagKey<EntityType<?>> MOBS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:mobs"));
    private static final int BOMB_PULSE_INTERVAL = 10;
    private final GameController game;
    private final LbcMana mana;
    private final List<Card> cards=new ArrayList<>();
    private final List<Bomb> bombs=new ArrayList<>();
    public static final int SPHERE_SLOTS = 20;
    public static final int SPELLCARD_GLOW = 0xE7A100;
    public static final int HAKUREI_BOMB_GLOW = 0xE7A200;
    static final Identifier SPHERE_MODEL = Identifier.parse("lbc:hakurei_sphere");
    private final Set<Entity> spawning=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerPlayer,List<LivingEntity>> targets=new IdentityHashMap<>();
    private final Map<UUID,EnumMap<InteractionHand,External>> external=new HashMap<>();
    private record External(boolean academy,InteractionHand hand,int ticks) {}

    public enum Volley {
        STANDARD(InteractionHand.MAIN_HAND, false), STANDARD_OFFHAND(InteractionHand.OFF_HAND, false),
        ACADEMY(InteractionHand.MAIN_HAND, true), ACADEMY_OFFHAND(InteractionHand.OFF_HAND, true);

        final InteractionHand patternHand;
        final boolean academy;

        Volley(InteractionHand patternHand, boolean academy) {
            this.patternHand = patternHand;
            this.academy = academy;
        }
    }

    private record Aim(Vec3 eye, Vec3 look, float pitch, float yaw) {}
    private record DirectedVolley(ServerLevel level, Volley variant, Aim aim, LbcIndirectKills.Owner cause, int ticks) {}
    private final Map<UUID, EnumMap<InteractionHand, DirectedVolley>> directed = new HashMap<>();
    public LbcDanmaku(GameController game,LbcMana mana){this.game=game;this.mana=mana;var holder=ScoreHolder.forNameOnly("danmaku_staff_mana_use");var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");if(objective==null||board.getPlayerScoreInfo(holder,objective)==null)mana.score(holder,"lbc.math").set(1000);}
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
        if(!p.isShiftKeyDown())stack.consume(1,p);
        return true;
    }
    private boolean castBomb(ServerPlayer p,InteractionHand hand,ItemStack trialStack){
        int charge=mana.score(p,"danmaku_score").get();if(charge<50)return false;
        if (!publishBomb(p, p.position(), charge, false, null)) return false;
        mana.score(p,"danmaku_score").set(0);
        if(charge>=1500&&!trialStack.isEmpty()&&!isAcademy(trialStack)&&LbcAcademy.trial(trialStack))game.items().academy().ready(p,hand,trialStack);
        announceBomb(p, p.position(), false);
        game.items().wands().wandCasted(p);return true;
    }
    /** Source consumable: no staff charge, mana, wand bonus or local launch jammer. */
    public boolean useTouhouBomb(ServerPlayer player){
        if(!validOwner(player)||game.items().actionsBlocked(player)||game.items().wands().inputTimeStopped(player))return false;
        return launchBomb(player, player.position(), 300, true, null);
    }

    /** Borrow the authored shell without spending the original staff's charge or unlocking upgrades. */
    public boolean scriptBomb(ServerPlayer player, Vec3 center, int charge, boolean fast) {
        if (!validOwner(player) || game.items().actionsBlocked(player) || game.items().wands().inputTimeStopped(player)) return false;
        var magic = game.items().magic();
        if (!fast && (magic.suppressed(player) || magic.suppressedAt(player.level(), player.getEyePosition())
                || magic.suppressedAt(player.level(), center))) return false;
        return launchBomb(player, center, charge, fast, game.items().indirectKills().coldCause(player, true));
    }

    private boolean launchBomb(ServerPlayer player, Vec3 center, int charge, boolean fast, LbcIndirectKills.Owner cause) {
        if (!publishBomb(player, center, charge, fast, cause)) return false;
        announceBomb(player, center, fast);
        return true;
    }

    private void announceBomb(ServerPlayer player, Vec3 center, boolean fast) {
        var announcement = fast
            ? Component.translatableWithFallback("lbc.hakurei.bomb.activate",
                "%s activated the bomb Fantasy Seal", player.getDisplayName())
            : Component.translatableWithFallback("lbc.hakurei.spellcard.activate",
                "%s activated the spell card Spirit Sign: Fantasy Seal", player.getDisplayName());
        game.server().getPlayerList().broadcastSystemMessage(announcement, false);
        LbcOverload.sound(player,center,"minecraft:lbcsounds.bomb",2,1,0);
    }
    public static boolean isItem(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||stack.is(ACADEMY)||LbcFartus.data(stack).getIntOr("danmaku_staff",0)==1);}
    public static boolean isAcademy(ItemStack stack){return isItem(stack)&&(stack.is(ACADEMY)||LbcFartus.data(stack).getIntOr("upgraded",0)==1);}
    public static boolean managed(Entity e){return e.entityTags().contains(TAG);}
    public static final class Card {
        public ServerPlayer owner;public final Display.ItemDisplay display;public final boolean homing;public Vec3 direction;public int age;
        public LbcIndirectKills.Owner cause;

        Card(ServerPlayer owner, Display.ItemDisplay display, Vec3 direction, boolean homing, LbcIndirectKills.Owner cause) {
            this.owner = owner;
            this.display = display;
            this.direction = direction.normalize();
            this.homing = homing;
            this.cause = cause;
        }
    }
    public static final class Bomb {
        public final ServerPlayer owner;
        public final Display.ItemDisplay display;
        public final int charge;
        public final boolean fast;
        public final LbcIndirectKills.Owner cause;
        public int age,empty,pulseTicks;

        Bomb(ServerPlayer owner, Display.ItemDisplay display, int charge, boolean fast, LbcIndirectKills.Owner cause) {
            this.owner = owner;
            this.display = display;
            this.charge = charge;
            this.fast = fast;
            this.cause = cause;
        }
        public int radius(){return (int)((int)(age*1.2)*.05);}
        public int sphereSlot() {
            int slot = display.getGlowColorOverride() - (fast ? HAKUREI_BOMB_GLOW : SPELLCARD_GLOW);
            return slot >= 0 && slot < SPHERE_SLOTS ? slot : -1;
        }
        void setSphereSlot(int slot) {
            display.setGlowColorOverride(slot < 0 ? -1 : (fast ? HAKUREI_BOMB_GLOW : SPELLCARD_GLOW) + slot);
            display.setGlowingTag(slot >= 0);
        }
    }
    public List<Card> cards(){return List.copyOf(cards);} public List<Bomb> bombs(){return List.copyOf(bombs);}
    public int count(){return cards.size()+bombs.size();}
    public int remaining(ServerPlayer p){return mana.score(p,"danmaku").get();}
    public ServerPlayer owner(Entity e){for(var c:cards)if(c.display==e)return c.owner;for(var b:bombs)if(b.display==e)return b.owner;return null;}
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
    public void queueExternal(ServerPlayer p,InteractionHand hand,boolean academy){
        var hands=external.computeIfAbsent(p.getUUID(),key->new EnumMap<>(InteractionHand.class));
        var prior=hands.get(hand);hands.put(hand,new External(academy,hand,prior==null?20:(int)Math.min(Integer.MAX_VALUE,(long)prior.ticks+20)));
    }
    /** Schedule one authored pattern with a fixed origin, independent of the physical wand slots. */
    public boolean scriptVolley(ServerPlayer player, Vec3 origin, Vec3 direction, Volley variant) {
        if (!validOwner(player)) return false;
        var patterns = directed.computeIfAbsent(player.getUUID(), ignored -> new EnumMap<>(InteractionHand.class));
        if (patterns.containsKey(variant.patternHand)) return false;
        float yaw = direction.horizontalDistanceSqr() < 1e-12 ? player.getYRot()
                : (float) -Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()));
        var aim = new Aim(origin, direction, pitch, yaw);
        var cause = game.items().indirectKills().coldCause(player, true);
        patterns.put(variant.patternHand, new DirectedVolley(player.level(), variant, aim, cause, 20));
        return true;
    }

    private void tickDirected(ServerPlayer player) {
        var patterns = directed.get(player.getUUID());
        if (patterns == null) return;
        var magic = game.items().magic();
        for (var iterator = patterns.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            var shot = entry.getValue();
            var origin = shot.aim.eye;
            if (player.level() != shot.level) {
                iterator.remove();
                continue;
            }
            if (game.items().timeStop().stopped(shot.level) || !shot.level.hasChunkAt(BlockPos.containing(origin))
                    || magic.suppressedAt(shot.level, origin)) continue;
            var hand = entry.getKey();
            if (((shot.ticks & 1) == 0) == (hand == InteractionHand.MAIN_HAND)) {
                emitPattern(player, hand, shot.variant.academy, phase(shot.ticks, hand), shot.aim, shot.cause, origin);
            }
            if (shot.ticks <= 1) iterator.remove();
            else entry.setValue(new DirectedVolley(shot.level, shot.variant, shot.aim, shot.cause, shot.ticks - 1));
        }
        if (patterns.isEmpty()) directed.remove(player.getUUID());
    }

    public int externalRemaining(ServerPlayer p){var hands=external.get(p.getUUID());if(hands==null)return 0;return (int)Math.min(Integer.MAX_VALUE,hands.values().stream().mapToLong(External::ticks).sum());}
    public void redirectCursedCards(){for(var c:List.copyOf(cards))game.items().homing().redirect(c);}
    public void tick(){
        targets.clear();
        for(var p:game.server().getPlayerList().getPlayers()){
            if(!validOwner(p)){if(remaining(p)>0||external.containsKey(p.getUUID())||directed.containsKey(p.getUUID()))forget(p);continue;}
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
            tickDirected(p);
        }
        if(!cards.isEmpty()||!bombs.isEmpty())game.items().cloneSkills().beforeOwnedStep(java.util.stream.Stream.concat(
                cards.stream().map(c->(Entity)c.display),bombs.stream().map(b->(Entity)b.display)).toList());
        for(var c:List.copyOf(cards))tick(c);
        for(var b:List.copyOf(bombs))tick(b);
    }
    private static int phase(int n,InteractionHand hand){return ((20-n%20)%20)/2;}
    private boolean valid(ServerPlayer p,Display.ItemDisplay d){return validOwner(p)&&!d.isRemoved()&&game.items().currentEpoch(d)&&p.level()==d.level()&&d.level().hasChunkAt(d.blockPosition())&&!game.items().magic().suppressedAt((ServerLevel)d.level(),d.position());}
    public Card card(ServerPlayer player, Vec3 position, Vec3 direction, boolean homing) {
        return publishCard(player, position, direction, homing, null);
    }

    public boolean scriptCard(ServerPlayer player, Vec3 position, Vec3 direction, boolean homing) {
        return publishCard(player, position, direction, homing, game.items().indirectKills().coldCause(player, true)) != null;
    }

    private Card publishCard(ServerPlayer p,Vec3 pos,Vec3 direction,boolean homing,LbcIndirectKills.Owner cause){
        if(!p.level().hasChunkAt(BlockPos.containing(pos)))return null;
        var d=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,p.level());
        try{var nbt=TagParser.parseCompoundFully("{teleport_duration:1,interpolation_duration:1,start_interpolation:0,shadow_radius:0f,shadow_strength:0f,item_display:'fixed',view_range:10f,transformation:{translation:[0f,0f,0f],left_rotation:[1f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[.5f,.5f,.5f]},item:{id:'minecraft:repeating_command_block',count:1,components:{'minecraft:item_model':'lbc:card_"+(homing?2:1)+"'}}}");d.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),nbt));}catch(Exception e){throw new IllegalStateException(e);}
        d.entityTags().add("danmaku");
        if (homing) d.entityTags().add("homing");
        if (!publishDisplay(p, d, pos)) return null;
        var c=new Card(p,d,direction,homing,cause);cards.add(c);orient(c);game.items().armorSets().phantom().activity(p);return c;
    }

    private boolean publishBomb(ServerPlayer owner, Vec3 center, int charge, boolean fast, LbcIndirectKills.Owner cause) {
        if (!owner.level().hasChunkAt(BlockPos.containing(center))) return false;
        var display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, owner.level());
        var item = new ItemStack(Items.PAPER);
        item.set(DataComponents.ITEM_MODEL, SPHERE_MODEL);
        display.setItemStack(item);
        display.setItemTransform(ItemDisplayContext.NONE);
        display.setViewRange(64);
        display.setTransformationInterpolationDuration(1);
        display.setPosRotInterpolationDuration(1);
        display.entityTags().add(fast ? "danmaku_bomb_fast" : "danmaku_bomb");
        display.entityTags().add("act");
        display.entityTags().add("cant_be_homing");
        var bomb = new Bomb(owner, display, charge, fast, cause);
        scaleSphere(bomb);
        assignSphereSlot(bomb);
        if (!publishDisplay(owner, display, center)) return false;
        bombs.add(bomb);
        return true;
    }

    private boolean publishDisplay(ServerPlayer owner, Display.ItemDisplay display, Vec3 position) {
        display.setPos(position);
        display.setNoGravity(true);
        display.entityTags().addAll(Set.of(TAG, "magic", "slowraycast", "player"));
        game.items().markCurrentItemEpoch(display);
        game.items().timeStop().prepareFrozen(display);
        spawning.add(display);
        try { return owner.level().addFreshEntity(display) && !display.isRemoved(); }
        finally { spawning.remove(display); }
    }

    private void assignSphereSlot(Bomb candidate) {
        int occupied = 0;
        for (var bomb : bombs) {
            int slot = bomb.sphereSlot();
            if (slot >= 0) occupied |= 1 << slot;
        }
        int firstFree = Integer.numberOfTrailingZeros(~occupied);
        candidate.setSphereSlot(firstFree < SPHERE_SLOTS ? firstFree : -1);
    }

    private static void scaleSphere(Bomb bomb) {
        float radius = bomb.age * .06f;
        bomb.display.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),
                new org.joml.Quaternionf(), new org.joml.Vector3f(radius), new org.joml.Quaternionf()));
        bomb.display.setTransformationInterpolationDelay(0);
    }
    private static void orient(Card c){var v=c.direction;c.display.setYRot((float)Math.toDegrees(Math.atan2(-v.x,v.z)));c.display.setXRot((float)-Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))));}
    public boolean reflectCard(Entity display, ServerPlayer reflector) {
        for (var card : cards) if (card.display == display) {
            card.owner = reflector;
            card.cause = null;
            card.direction = card.direction.reverse();
            orient(card);
            return true;
        }
        return false;
    }
    private void tick(Card c){
        if(!valid(c.owner,c.display)){remove(c);return;}if(game.items().timeStop().frozen(c.display)||c.display.entityTags().contains("time_stopped"))return;
        if(++c.age>60){remove(c);return;}
        var level = (ServerLevel)c.display.level();
        c.direction=game.items().homing().skillDirection(c.owner,level,c.display.position(),c.direction);orient(c);
        HomingTarget guide = null;
        for(int i=0;i<3;i++){
            var from = c.display.position();
            if(c.homing){
                if (guide == null || !guide.reusableAt(from)) guide = nearestHomingTarget(c.owner, level, from);
                if (guide == null) { if(c.age>=60)remove(c); return; }
                c.direction=ProjectileSteering.turn(c.direction,guide.entity.getEyePosition().subtract(from),Math.atan(.06));orient(c);
            }
            var to=from.add(c.direction.scale(.4));
            if(!residentSegment(level,from,to)){remove(c);return;}
            var block=net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,c.display,
                    (ignored,pos)->level.getBlockState(pos).isAir()?null:net.minecraft.world.phys.shapes.Shapes.block().clip(from,to,pos),
                    ignored->net.minecraft.world.phys.BlockHitResult.miss(to,net.minecraft.core.Direction.UP,BlockPos.containing(to)));
            double obstacle=block.getType()==net.minecraft.world.phys.HitResult.Type.MISS?Double.POSITIVE_INFINITY:from.distanceToSqr(block.getLocation());
            var greed=LbcGreed.firstHit(level,from,to,0);
            var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,c.display,from,to,new AABB(from,to).inflate(1),e->e instanceof LivingEntity&&game.items().wands().projectileTarget(c.owner,e),0);
            double impact=hit==null?Double.POSITIVE_INFINITY:from.distanceToSqr(hit.getLocation());
            if(greed!=null&&(c.owner!=greed.getEntity()||!greed.getEntity().getBoundingBox().inflate(.2).contains(from))&&from.distanceToSqr(greed.getLocation())<=Math.min(obstacle,impact)){
                if(greed.getEntity() instanceof ServerPlayer player){c.owner=player;c.cause=null;c.display.entityTags().add(LbcGreed.REFLECTED);c.direction=c.direction.scale(-1);c.display.setPos(greed.getLocation());orient(c);}else remove(c);return;
            }
            if(obstacle<=impact&&Double.isFinite(obstacle)){remove(c);return;}
            if(hit!=null){c.display.setPos(hit.getLocation());damageCard(c,(LivingEntity)hit.getEntity());mana.score(c.owner,"danmaku_score").set((int)Math.min(Integer.MAX_VALUE,(long)mana.score(c.owner,"danmaku_score").get()+3));remove(c);return;}
            c.display.setPos(to);
        }
        if(c.age>=60)remove(c);
    }

    private record HomingTarget(LivingEntity entity, Vec3 origin, double safeDistanceSquared) {
        boolean reusableAt(Vec3 position) {
            return origin.distanceToSqr(position) < safeDistanceSquared;
        }
    }

    private HomingTarget nearestHomingTarget(ServerPlayer owner, ServerLevel level, Vec3 origin) {
        var candidates = targets.computeIfAbsent(owner, player -> {
            var eligible = new ArrayList<LivingEntity>();
            for (var candidate : game.items().lifetimes().loadedTargets()) {
                if (candidate.level() == level && candidate.getType().builtInRegistryHolder().is(MOBS)
                        && game.items().wands().target(player, candidate)) eligible.add(candidate);
            }
            return eligible;
        });
        LivingEntity nearest = null;
        double best = Double.POSITIVE_INFINITY;
        double runnerUp = Double.POSITIVE_INFINITY;
        for (var candidate : candidates) {
            double squared = candidate.distanceToSqr(origin);
            if (!(squared < runnerUp) || candidate.level() != level || !candidate.isAlive() || candidate.isRemoved()
                    || !game.items().wands().target(owner, candidate)) continue;
            if (squared < best) {
                runnerUp = best;
                best = squared;
                nearest = candidate;
            } else {
                runnerUp = squared;
            }
        }
        if (nearest == null) return null;

        // Targets cannot independently move during one card's collision-free substeps.
        // Each distance can change by at most the card's displacement. A strict
        // half-gap therefore preserves the nearest target, including exact ties.
        double safeDistance = (Math.sqrt(runnerUp) - Math.sqrt(best)) * .5;
        return new HomingTarget(nearest, origin, safeDistance * safeDistance);
    }

    private static boolean residentSegment(ServerLevel level,Vec3 from,Vec3 to){
        int minX=BlockPos.containing(from).getX()>>4,maxX=BlockPos.containing(to).getX()>>4;
        int minZ=BlockPos.containing(from).getZ()>>4,maxZ=BlockPos.containing(to).getZ()>>4;
        for(int x=Math.min(minX,maxX);x<=Math.max(minX,maxX);x++)for(int z=Math.min(minZ,maxZ);z<=Math.max(minZ,maxZ);z++)if(!level.hasChunkAt(new BlockPos(x<<4,0,z<<4)))return false;
        return true;
    }
    private void damageCard(Card card, LivingEntity victim) {
        var owner = card.owner;
        var level = (ServerLevel) victim.level();
        if (game.items().magic().suppressedAt(level, victim.position())) return;
        // Cards omit the native attacker impulse, while retaining the cast's kill credit.
        var type = owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.GENERIC);
        var source = new DamageSource(type);
        var cause = card.cause != null ? card.cause : game.items().indirectKills().coldCause(owner, true);
        float dealt = game.balanceLegacy() ? .65f : .325f;
        game.items().indirectKills().withCause(victim, cause, () -> {
            if (victim.hurtServer(level, source, dealt)) LbcAcceptedHits.projectileFeedback(game, owner);
        });
    }

    private boolean damageBomb(Bomb bomb, LivingEntity victim, DamageSource source, float amount) {
        var level = (ServerLevel) victim.level();
        if (game.items().magic().suppressedAt(level, victim.position()) || game.items().timeStop().frozen(victim)
                || victim.isPermanentlyInvulnerable() || !game.items().allowDamage(victim, source)
                || EnchantmentHelper.isImmuneToDamage(level, victim, source)) return false;
        if (victim instanceof ServerPlayer player
                && (player.getAbilities().invulnerable || !player.canHarmPlayer(bomb.owner))) return false;

        float armor = (float) victim.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float damage = CombatRules.getDamageAfterAbsorb(victim, amount, source, armor, toughness);
        if (!(damage > 0) || !Float.isFinite(damage)) return false;
        var credit = (me.skywars2.mixin.combat.BlackHoleDeathProtectionAccess) victim;
        credit.skywars2$holeMobCredit(source);
        credit.skywars2$holePlayerCredit(source);
        game.items().indirectKills().damaged(victim, source);
        if (!LbcDamage.removeHealth(victim, damage, source)) return false;
        if (!victim.isAlive()) game.items().polymorph().finishDirectDamage(victim);
        else {
            // The explicit damage event already delivered the client's hurt response.
            // Publish health so the periodic player packet cannot infer a second one.
            var player = victim instanceof ServerPlayer direct ? direct : game.items().polymorph().playerOfBody(victim);
            if (player != null && player.isAlive())
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(player.getId(),
                    List.of(net.minecraft.network.syncher.SynchedEntityData.DataValue.create(
                        me.skywars2.mixin.combat.BlackHoleDeathProtectionAccess.skywars2$healthData(), player.getHealth()))));
        }
        return true;
    }

    private void tick(Bomb b){
        if(!valid(b.owner,b.display)){remove(b);return;}
        if(game.items().timeStop().frozen(b.display)||b.display.entityTags().contains("time_stopped"))return;
        int lastAge = b.age;
        if(b.fast){if(b.age<=b.charge)b.age+=10;}
        else b.age+=4;
        if (lastAge != b.age) scaleSphere(b);
        boolean pulse = ++b.pulseTicks == BOMB_PULSE_INTERVAL;
        if (pulse) b.pulseTicks = 0;
        var level = (ServerLevel)b.display.level();
        var center = b.display.position();
        double radius = b.radius();
        double radiusSquared = radius * radius;
        game.items().wands().clearDanmakuRays(b.owner, center, radius);
        game.items().sauvojen().meditation().clearTraces(level, center, radiusSquared);
        game.items().reactiveArtifacts().clearGrenades(b.owner, center, radiusSquared);
        DamageSource source = null;
        boolean hit = false;
        float amount = (b.fast ? 2f : 1f) * (game.balanceLegacy() ? 1f : .5f);
        for (var entity : level.getEntities(b.display, new AABB(center,center).inflate(radius),
                target -> target.distanceToSqr(center) <= radiusSquared)) {
            if (hostileProjectile(b.owner, entity)) {
                entity.discard();
                continue;
            }
            if (!pulse || !(entity instanceof LivingEntity living) || !game.items().wands().target(b.owner, entity)) continue;
            if (source == null) {
                var magic = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC);
                var cause = b.cause != null ? b.cause : game.items().indirectKills().coldCause(b.owner, true);
                source = game.items().indirectKills().captureDamage(new DamageSource(magic, b.display, b.owner), cause);
            }
            hit |= damageBomb(b, living, source, amount);
        }
        if (hit && !b.fast) LbcAcceptedHits.projectileFeedback(game, b.owner);
        if(b.age>b.charge&&(!b.fast||++b.empty>=60))remove(b);
    }
    private boolean hostileProjectile(ServerPlayer p,Entity e){
        if(managed(e)){var owner=owner(e);return owner!=null&&owner!=p&&!p.isAlliedTo(owner)&&cards.stream().anyMatch(c->c.display==e);}
        if(!game.items().wands().validRoleTarget(e,false)||LbcGreed.active(e)||p.isAlliedTo(e))return false;
        var tags=e.entityTags();
        boolean raycaster=e instanceof Display||e instanceof net.minecraft.world.entity.Marker||e instanceof net.minecraft.world.entity.AreaEffectCloud
                ||e instanceof net.minecraft.world.entity.Interaction&&tags.contains(SauvojenMeditation.TAG);
        if(!(e instanceof Projectile)&&!tags.contains("mob_projectile")&&!tags.contains("slowraycast")&&!tags.contains("raycast")
                &&!(e instanceof net.minecraft.world.entity.item.FallingBlockEntity)&&!(e instanceof net.minecraft.world.entity.item.PrimedTnt)
                &&!(raycaster&&(tags.contains("magic")||tags.contains("mecha"))))return false;
        var source=LbcIndirectKills.playerSource(e);return source!=p&&(source==null||!p.isAlliedTo(source));
    }
    private void remove(Card c){cards.remove(c);c.display.discard();}
    private void remove(Bomb bomb) {
        if (!bombs.remove(bomb)) return;
        bomb.display.discard();
        int freed = bomb.sphereSlot();
        if (freed < 0) return;
        for (var waiting : bombs) {
            if (waiting.sphereSlot() >= 0 || waiting.display.isRemoved()) continue;
            waiting.setSphereSlot(freed);
            break;
        }
    }
    public void loaded(Entity e){if(managed(e)&&!spawning.contains(e)&&owner(e)==null)e.discard();}
    public void unloaded(Entity e){for(var c:List.copyOf(cards))if(c.display==e)remove(c);for(var b:List.copyOf(bombs))if(b.display==e)remove(b);}
    public void forget(ServerPlayer p){for(var c:List.copyOf(cards))if(c.owner==p)remove(c);for(var b:List.copyOf(bombs))if(b.owner==p)remove(b);mana.score(p,"danmaku").set(0);mana.score(p,"danmaku_score").set(0);external.remove(p.getUUID());directed.remove(p.getUUID());}
    public void stop(){for(var c:List.copyOf(cards))remove(c);for(var b:List.copyOf(bombs))remove(b);for(var p:game.server().getPlayerList().getPlayers())forget(p);external.clear();directed.clear();targets.clear();}
    public MutableComponent hud(ServerPlayer p){
        var text=Component.empty();int ticks=mana.hudValue(p,"danmaku"),score=mana.hudValue(p,"danmaku_score");
        if(mana.visible(p)){var hands=external.get(p.getUUID());if(hands!=null)for(var extra:hands.values())ticks=Math.max(ticks,extra.ticks);}
        if(ticks>0)text.append(Component.literal("    \uE133 ").append(Component.literal(Integer.toString(ticks)).withColor(0xFFA1F2)));
        if(score>0)text.append(Component.literal("  \uE134 ").append(Component.literal(Integer.toString(score)).withColor(0xD494FF)));
        return text;
    }
    private record Pattern(double x,double y,double z,boolean local,float yaw,float pitch,boolean homing) {}
    private void emit(ServerPlayer p,InteractionHand hand,boolean academy,int phase){
        var aim = new Aim(p.getEyePosition(), p.getLookAngle(), p.getXRot(), p.getYRot());
        emitPattern(p, hand, academy, phase, aim, null, p.position());
    }

    private void emitPattern(ServerPlayer player, InteractionHand hand, boolean academy, int phase,
                             Aim aim, LbcIndirectKills.Owner cause, Vec3 soundOrigin) {
        boolean offhand = hand == InteractionHand.OFF_HAND;
        var base = aim.eye.add(0, academy && offhand ? 0 : -.5, 0);
        var left = Vec3.directionFromRotation(0, aim.yaw - 90);
        var up = aim.look.cross(left);
        for (var sample : PATTERNS[(academy ? 2 : 0) + (offhand ? 1 : 0)][phase]) {
            var pos = sample.local
                    ? base.add(left.scale(sample.x)).add(up.scale(sample.y)).add(aim.look.scale(sample.z))
                    : base.add(sample.x, sample.y, sample.z);
            var direction = Vec3.directionFromRotation(aim.pitch + sample.pitch, aim.yaw + sample.yaw);
            if (cause == null || !game.items().magic().suppressedAt(player.level(), pos)) {
                publishCard(player, pos, direction, sample.homing, cause);
            }
        }
        LbcOverload.sound(player, soundOrigin, "minecraft:ui.cartography_table.take_result", .5f, 2, 0);
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

    public void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform){
        for(var card:cards)if(card.display.level()==level&&!card.display.isRemoved())card.display.setPos(transform.apply(card.display.position()));
        for(var bomb:bombs)if(bomb.display.level()==level&&!bomb.display.isRemoved())
            bomb.display.setPos(transform.apply(bomb.display.position()));
    }
    public void planetoid(ServerPlayer holder) {
        for(var card:cards)if(!card.display.isRemoved()&&game.items().currentEpoch(card.display)
                &&!game.items().timeStop().frozen(card.display)&&!card.display.entityTags().contains("time_stopped")
                &&LbcPlanetoidSkill.near(game,holder,(ServerLevel)card.display.level(),card.display.position())){
            card.direction=LbcPlanetoidSkill.direction(holder,card.display.position(),82);orient(card);
        }
    }

    public LbcCloneSkills.ActorImage cloneImage(Entity original){
        var card=cards.stream().filter(c->c.display==original).findFirst().orElse(null);
        var bomb=bombs.stream().filter(b->b.display==original).findFirst().orElse(null);
        if(card==null&&bomb==null)return null;
        final ServerPlayer owner=card!=null?card.owner:bomb.owner;
        final int age=card!=null?card.age:bomb.age,charge=bomb==null?0:bomb.charge,empty=bomb==null?0:bomb.empty;
        final int pulseTicks=bomb==null?0:bomb.pulseTicks;
        final Vec3 direction=card==null?Vec3.ZERO:card.direction;
        final LbcIndirectKills.Owner cause = card != null ? card.cause : bomb.cause;
        final boolean homing=card!=null&&card.homing,fast=bomb!=null&&bomb.fast;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity source,Entity clone,Map<Entity,Entity> family){
                var display=(Display.ItemDisplay)clone;
                if(card!=null){var copied=new Card(owner,display,direction,homing,cause);copied.age=age;cards.add(copied);return;}
                var copied=new Bomb(owner,display,charge,fast,cause);
                copied.age=age;copied.empty=empty;copied.pulseTicks=pulseTicks;
                assignSphereSlot(copied);
                bombs.add(copied);
            }
            public void rollback(Entity clone){unloaded(clone);}
        };
    }
    public Checkpoint checkpoint(MatchSpellCheckpoint.Links links){if(!spawning.isEmpty())throw new IllegalStateException("Danmaku checkpoint during native publication");return new Checkpoint(links);}
    public final class Checkpoint{
        private record CardImage(MatchDamageSources.Ref owner,MatchDamageSources.Ref display,Vec3 direction,boolean homing,int age,LbcIndirectKills.Owner cause){}
        private record BombImage(MatchDamageSources.Ref owner,MatchDamageSources.Ref display,int charge,boolean fast,int age,int empty,int pulseTicks,LbcIndirectKills.Owner cause){}
        private final List<CardImage> savedCards;
        private final List<BombImage> savedBombs;
        private final Map<UUID,Map<InteractionHand,External>> savedExternal=new HashMap<>();
        private final Map<UUID,Map<InteractionHand,DirectedVolley>> savedDirected=new HashMap<>();
        Checkpoint(MatchSpellCheckpoint.Links links){
            savedCards=cards.stream().map(c->new CardImage(links.key(c.owner),links.key(c.display),c.direction,c.homing,c.age,c.cause)).toList();
            savedBombs=bombs.stream().map(b->new BombImage(links.key(b.owner),links.key(b.display),b.charge,b.fast,b.age,b.empty,b.pulseTicks,b.cause)).toList();
            external.forEach((id,hands)->savedExternal.put(id,Map.copyOf(hands)));
            directed.forEach((id,hands)->savedDirected.put(id,Map.copyOf(hands)));
        }
        public Runnable prepare(MatchSpellCheckpoint.Links links){
            var restoredCards=new ArrayList<Card>();for(var image:savedCards){var card=new Card((ServerPlayer)links.get(image.owner),(Display.ItemDisplay)links.get(image.display),image.direction,image.homing,image.cause);card.age=image.age;restoredCards.add(card);}
            var restoredBombs=new ArrayList<Bomb>();for(var image:savedBombs){var bomb=new Bomb((ServerPlayer)links.get(image.owner),(Display.ItemDisplay)links.get(image.display),image.charge,image.fast,image.cause);bomb.age=image.age;bomb.empty=image.empty;bomb.pulseTicks=image.pulseTicks;restoredBombs.add(bomb);}
            return ()->{
                cards.clear();cards.addAll(restoredCards);bombs.clear();bombs.addAll(restoredBombs);
                external.clear();savedExternal.forEach((id,hands)->external.put(id,new EnumMap<>(hands)));
                directed.clear();savedDirected.forEach((id,hands)->directed.put(id,new EnumMap<>(hands)));
                targets.clear();
            };
        }
    }

}
