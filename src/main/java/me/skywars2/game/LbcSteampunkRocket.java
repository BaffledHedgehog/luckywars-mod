package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.*;
import java.util.function.Function;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.WandProjectileMixin.ArrowAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Immediate source rocket input; a real passenger arrow owns movement and collision. */
public final class LbcSteampunkRocket {
    public static final String TAG="skywars2.steampunk_rocket";
    private static final String TERMINAL=TAG+".terminal";
    private static final int LIFETIME=40;
    public static final Item TOKEN=registerItem();
    private static Item registerItem(){
        var id=Identifier.parse("skywars2:steampunk_rocket");var model=Identifier.parse("lbc:steampunk_rocket");
        var flags=new CompoundTag();flags.putInt("steampunk_rocket",1);flags.putInt("gadget",1);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(16).overrideDescription("steampunk_rocket")
                .component(DataComponents.ITEM_MODEL,model).component(DataComponents.CUSTOM_DATA,CustomData.of(flags))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("steampunk_rocket").withStyle(s->s.withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("steampunk_rocket.lore").withStyle(s->s.withItalic(false)))));
        return Registry.register(BuiltInRegistries.ITEM,id,new Token(properties));
    }
    public static void register(){}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"steampunk_rocket",0)==1;}
    public static boolean managed(Entity entity){return entity instanceof Arrow&&entity.entityTags().contains(TAG);}
    private final GameController game;
    private final Map<UUID,Flight> flights=new LinkedHashMap<>();
    private final Set<UUID> retired=new HashSet<>();
    private String retiredEpoch;
    private boolean restoring;
    private static final class Flight {
        final UUID id,rider;
        final LbcSpellSource.Source source;
        Arrow body;
        int age,lastTick=Integer.MIN_VALUE;
        Flight(UUID id,UUID rider,LbcSpellSource.Source source,int age,Arrow body){this.id=id;this.rider=rider;this.source=source;this.age=age;this.body=body;}
        Image image(){return new Image(id,rider,source,age);}
    }
    record Image(UUID id,UUID rider,LbcSpellSource.Source source,int age){}
    public LbcSteampunkRocket(GameController game){this.game=Objects.requireNonNull(game);}
    private boolean admitted(ServerPlayer owner){
        return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()
                &&owner.isAlive()&&!owner.isRemoved()&&!owner.isSpectator()&&!game.isObserver(owner)
                &&game.canUseGameplayItems(owner)&&LbcDimensionItems.ordinary(owner)
                &&game.items().wands().validRoleTarget(owner,false)&&!game.items().actionsBlocked(owner)
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level())
                &&game.server().tickRateManager().runsNormally();
    }
    public boolean use(ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);if(!matches(stack)||!admitted(owner))return false;
        // Author-added launch TECH suppression follows ordinary rejected-use
        // admission: no stat/debit. Kill Button's explicit source exception differs.
        if(game.items().magic().mechanismSuppressed(owner))return false;
        var origin=owner.position().add(0,.01,0);var level=owner.level();float yaw=owner.getYRot(),pitch=owner.getXRot();
        UUID rider=owner.getUUID();String epoch=game.items().epoch();var inherited=game.items().spellSources().current();
        // The existing native source adapter owns TMT debit/completion.
        owner.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,owner);
        var cause=inherited!=null&&inherited.cause()!=null?inherited.cause():game.items().indirectKills().wildCause(owner);
        // Ordinary RMB is immediate; an accepted native shot completes its criterion
        // and restores private components in the same outer action before mounting.
        LbcTmtShotCompletion.afterNativeAction(()->{
            if(!game.items().currentEpoch(epoch))return;
            var actual=game.server().getPlayerList().getPlayer(rider);
            if(actual==null||actual.isRemoved()||!actual.isAlive()||actual.level()!=level)return;
            launch(actual,level,origin,yaw,pitch,epoch,inherited,cause);
        });
        return true;
    }
    private void launch(ServerPlayer rider,ServerLevel level,Vec3 origin,float yaw,float pitch,String epoch,
                        LbcSpellSource.Source inherited,LbcIndirectKills.Owner cause){
        var arrow=new Arrow(EntityTypes.ARROW,level);
        UUID author=inherited!=null&&inherited.author()!=null?inherited.author():rider.getUUID();
        // EntityReference's native Owner UUID survives logout and replacement bodies.
        var owner=game.server().getPlayerList().getPlayer(author);
        if(owner!=null)arrow.setOwner(owner);
        else {var ownerTag=new CompoundTag();ownerTag.store("Owner",net.minecraft.core.UUIDUtil.CODEC,author);arrow.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),ownerTag));}
        arrow.setPos(origin);arrow.setYRot(yaw);arrow.setXRot(pitch);arrow.setDeltaMovement(Vec3.ZERO);
        arrow.setNoGravity(true);arrow.setBaseDamage(0);arrow.setSoundEvent(SoundEvents.WOOL_STEP);
        arrow.pickup=AbstractArrow.Pickup.DISALLOWED;((ArrowAccess)(Object)arrow).skywars2$setPierceLevel((byte)127);
        arrow.entityTags().addAll(List.of(TAG,"rided_steampunk","killedarrow","exploding","actived"));
        var source=new LbcSpellSource.Source(inherited==null?arrow.getUUID():inherited.emitter(),author,epoch,cause,
                inherited==null?Set.of():inherited.excluded(),inherited!=null&&inherited.persistent());
        var flight=new Flight(arrow.getUUID(),rider.getUUID(),source,0,arrow);flights.put(flight.id,flight);
        save(flight);game.items().markCurrentItemEpoch(arrow);game.items().spellSources().bind(arrow,source);
        try(var scope=game.items().spellSources().scope(source,level)){
            if(!level.addFreshEntity(arrow)){remove(flight);return;}
        }
        rider.stopRiding();rider.startRiding(arrow,true,true);
    }
    private boolean paused(Arrow arrow){return restoring||!game.server().tickRateManager().runsNormally()
            ||arrow.entityTags().contains("time_stopped")||game.items().timeStop().frozen(arrow)
            ||game.items().timeStop().stopped((ServerLevel)arrow.level());}
    private boolean current(Flight flight){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.items().currentEpoch(flight.source.epoch());}
    /** Wire beside Teleport.beforeCloudTick at actual AbstractArrow.tick HEAD. */
    public boolean beforeArrowTick(Arrow arrow){
        if(!managed(arrow))return true;
        if(!flights.containsKey(arrow.getUUID()))loaded(arrow);
        var flight=flights.get(arrow.getUUID());
        if(flight==null||arrow.isRemoved())return false;
        if(!current(flight)||!LbcDimensionItems.ordinary(arrow.level())){remove(flight);return false;}
        if(paused(arrow))return false;
        if(((ArrowAccess)(Object)arrow).skywars2$isInGround()){landed(arrow);return false;}
        int now=game.server().getTickCount();if(flight.lastTick==now)return false;
        var rider=game.server().getPlayerList().getPlayer(flight.rider);
        boolean nearby=rider!=null&&!rider.isRemoved()&&rider.isAlive()&&!rider.isSpectator()&&!game.isObserver(rider)
                &&rider.level()==arrow.level()&&rider.position().distanceToSqr(arrow.position())<=9
                &&game.items().wands().validRoleTarget(rider,false);
        // Legacy selects the matching player within3, even after dismount. Losing
        // that player enables gravity; later proximity does not disable it again.
        Vec3 motion=nearby?steer(arrow.getDeltaMovement(),rider.getLookAngle()):arrow.getDeltaMovement();
        var level=(ServerLevel)arrow.level();
        boolean resident=BlockGetter.traverseBlocks(arrow.position(),arrow.position().add(motion),level,
                (world,pos)->world.hasChunkAt(pos)?null:Boolean.FALSE,world->Boolean.TRUE);
        if(!resident)return false;
        flight.lastTick=now;
        if(!nearby)arrow.setNoGravity(false);
        else {arrow.setDeltaMovement(motion);arrow.needsSync=true;arrow.syncVelocity=true;}
        level.sendParticles(ParticleTypes.FLAME,arrow.getX(),arrow.getY(),arrow.getZ(),30,.5,.5,.5,.3);
        flight.age++;saveAge(flight);
        if(flight.age>=LIFETIME){remove(flight);return false;}
        return true;
    }
    /** Exact source score arithmetic: floor initial scales and scoreboard floorDiv. */
    static Vec3 steer(Vec3 motion,Vec3 look){
        int x=Mth.floor(motion.x*10000)+Mth.floor(look.x*1000);
        int y=Mth.floor(motion.y*10000)+Mth.floor(look.y*1000);
        int z=Mth.floor(motion.z*10000)+Mth.floor(look.z*1000);
        while(x>100000||x< -100000||y>100000||y< -100000||z>100000||z< -100000){
            x=Math.floorDiv(x*3,4);y=Math.floorDiv(y*3,4);z=Math.floorDiv(z*3,4);
        }
        return new Vec3(x*.0001,y*.0001,z*.0001);
    }
    /** Wire at AbstractArrow.onHitBlock TAIL, after native inGround becomes true. */
    public boolean landed(Arrow arrow){
        if(!managed(arrow)||arrow.isRemoved()||!((ArrowAccess)(Object)arrow).skywars2$isInGround())return false;
        var flight=flights.get(arrow.getUUID());if(flight==null||paused(arrow))return false;
        if(!current(flight)||flight.age>=LIFETIME){remove(flight);return false;}
        flights.remove(flight.id);retire(flight.id);arrow.entityTags().add(TERMINAL);
        var level=(ServerLevel)arrow.level();var source=game.items().spellSources().source(arrow);
        if(source==null){
            // A native reflection transfers ownership. Never reattach the old
            // accepted source over the shared reflection owner's new authority.
            if(arrow.getOwner() instanceof ServerPlayer changed&&!changed.getUUID().equals(flight.source.author()))
                source=new LbcSpellSource.Source(arrow.getUUID(),changed.getUUID(),flight.source.epoch(),
                        game.items().indirectKills().wildCause(changed),Set.of());
            else source=flight.source;
        }
        var author=source.author()==null?null:game.server().getPlayerList().getPlayer(source.author());
        var tnt=new PrimedTnt(level,arrow.getX(),arrow.getY(),arrow.getZ(),author);tnt.setFuse(0);tnt.setDeltaMovement(Vec3.ZERO);
        game.items().markCurrentItemEpoch(tnt);game.items().spellSources().bind(tnt,source);
        try(var scope=game.items().spellSources().scope(source,level)){level.addFreshEntity(tnt);}
        finally{arrow.discard();}
        return true;
    }
    /** Indexed cleanup only; native arrow hooks own active age, steering and physics. */
    public void tick(){
        if(restoring)return;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        for(var flight:List.copyOf(flights.values())){
            if(!current(flight)){remove(flight);continue;}
            var arrow=flight.body;if(arrow==null)continue;
            if(arrow.isRemoved()){
                if(arrow.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)flight.body=null;
                else {flights.remove(flight.id);retire(flight.id);}
            }else if(!paused(arrow)&&((ArrowAccess)(Object)arrow).skywars2$isInGround())landed(arrow);
        }
    }
    private void retire(UUID id){if(!Objects.equals(retiredEpoch,game.items().epoch())){retired.clear();retiredEpoch=game.items().epoch();}retired.add(id);}
    private void remove(Flight flight){flights.remove(flight.id);retire(flight.id);if(flight.body!=null){flight.body.entityTags().add(TERMINAL);flight.body.discard();}}
    private static void put(Entity body,String key,String value){String prefix=TAG+"."+key+".";body.entityTags().removeIf(tag->tag.startsWith(prefix));body.entityTags().add(prefix+value);}
    private static String get(Entity body,String key,String fallback){String prefix=TAG+"."+key+".";for(var tag:body.entityTags())if(tag.startsWith(prefix))return tag.substring(prefix.length());return fallback;}
    private void saveAge(Flight f){if(f.body!=null)put(f.body,"age",Integer.toString(f.age));}
    private void save(Flight f){
        var body=f.body;if(body==null)return;saveAge(f);put(body,"rider",f.rider.toString());put(body,"epoch",f.source.epoch());
        put(body,"emitter",f.source.emitter().toString());if(f.source.author()!=null)put(body,"author",f.source.author().toString());
        put(body,"persistent",Boolean.toString(f.source.persistent()));
        var cause=f.source.cause();if(cause!=null){put(body,"cause",cause.player().toString());put(body,"order",Long.toString(cause.order()));put(body,"friendly",Boolean.toString(cause.friendlyFire()));put(body,"projectile",Boolean.toString(cause.projectile()));}
        String prefix=TAG+".excluded.";body.entityTags().removeIf(tag->tag.startsWith(prefix));for(var excluded:f.source.excluded())body.entityTags().add(prefix+excluded);
    }
    public void loaded(Entity entity){
        if(restoring||!managed(entity))return;var arrow=(Arrow)entity;
        if(entity.entityTags().contains(TERMINAL)||Objects.equals(retiredEpoch,game.items().epoch())&&retired.contains(entity.getUUID())){entity.discard();return;}
        var old=flights.get(entity.getUUID());if(old!=null){old.body=arrow;game.items().spellSources().bind(arrow,old.source);return;}
        try{
            String epoch=get(entity,"epoch","");int age=Integer.parseInt(get(entity,"age","0"));
            if(!game.items().currentEpoch(epoch)||age<0||age>=LIFETIME||!game.playing()){entity.discard();return;}
            UUID rider=UUID.fromString(get(entity,"rider",""));String author=get(entity,"author","");String causeId=get(entity,"cause","");
            var cause=causeId.isEmpty()?null:new LbcIndirectKills.Owner(UUID.fromString(causeId),Long.parseLong(get(entity,"order","0")),Boolean.parseBoolean(get(entity,"friendly","true")),Boolean.parseBoolean(get(entity,"projectile","false")));
            var excluded=new HashSet<String>();String prefix=TAG+".excluded.";for(var tag:entity.entityTags())if(tag.startsWith(prefix))excluded.add(tag.substring(prefix.length()));
            var source=new LbcSpellSource.Source(UUID.fromString(get(entity,"emitter",entity.getUUID().toString())),author.isEmpty()?null:UUID.fromString(author),epoch,cause,excluded,Boolean.parseBoolean(get(entity,"persistent","false")));
            flights.put(entity.getUUID(),new Flight(entity.getUUID(),rider,source,age,arrow));game.items().spellSources().bind(arrow,source);
        }catch(RuntimeException malformed){entity.discard();}
    }
    public void unloaded(Entity entity){var flight=flights.get(entity.getUUID());if(flight==null||flight.body!=entity)return;
        if(entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)flight.body=null;
        else {flights.remove(flight.id);retire(flight.id);}}
    public int count(){return flights.size();}
    int age(Arrow arrow){var flight=flights.get(arrow.getUUID());return flight==null?-1:flight.age;}
    List<Arrow> snapshot(){return flights.values().stream().map(f->f.body).filter(Objects::nonNull).toList();}
    public void stop(){for(var flight:List.copyOf(flights.values()))remove(flight);flights.clear();restoring=false;}
    void prepareFrozen(){restoring=true;flights.clear();}
    void publishFrozen(){restoring=false;for(var flight:flights.values())flight.lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        final List<Image> images=flights.values().stream().map(Flight::image).toList();
        final Set<UUID> retiredImage=Set.copyOf(retired);final String retiredEpochImage=retiredEpoch;
        Set<UUID> actorIds(){var ids=new HashSet<UUID>();images.forEach(image->ids.add(image.id()));return Set.copyOf(ids);}
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){
            var missing=new HashSet<UUID>();var rebound=new ArrayList<Flight>();
            for(var image:images){var entity=lookup.apply(image.id());if(entity==null)missing.add(image.id());
                else if(entity instanceof Arrow arrow)rebound.add(new Flight(image.id(),image.rider(),image.source(),image.age(),arrow));
                else throw new IllegalStateException("Steampunk rocket checkpoint actor is not an Arrow: "+image.id());}
            return new MatchFieldCheckpoint.Plan(Set.copyOf(missing),()->{flights.clear();retired.clear();retired.addAll(retiredImage);retiredEpoch=retiredEpochImage;
                for(var flight:rebound){flights.put(flight.id,flight);save(flight);game.items().spellSources().bind(flight.body,flight.source);}});
        }
    }
    private static final class Token extends SimplePolymerItem {
        Token(Properties properties){super(properties,Items.SNOWBALL,true);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=SkyWars2.game();
            return game!=null&&player instanceof ServerPlayer owner&&game.items().steampunkRocket().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    }
}
