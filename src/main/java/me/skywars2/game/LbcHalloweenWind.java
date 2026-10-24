package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Halloween Vex wind wand (distinct from Vex Soul) and native Antichrist mace. */
public final class LbcHalloweenWind {
    public static final String PROJECTILE="skywars2.halloween_wind_charge";
    static final Identifier AIR=Identifier.parse("lbc:air_sphere_11x11x11");
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    public static final Item WIND=registerItem("skywars2:vex_wind_wand",false,false);
    public static final Item ACADEMY_WIND=registerItem("skywars2:academy_vex_wind_wand",true,false);
    public static final Item MACE=registerItem("skywars2:halloween_mace",false,true);
    private final GameController game;
    private final LbcMana mana;
    private final Set<WindCharge> shots=new LinkedHashSet<>();
    private final Map<ServerPlayer,Integer> seconds=new HashMap<>();

    LbcHalloweenWind(GameController game,LbcMana mana){this.game=game;this.mana=mana;}
    public static void register(){}
    private static Item registerItem(String id,boolean academy,boolean mace){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        var model=Identifier.parse(mace?"lbc:halloween_mace":"lbc:vex_wand");
        var tag=new CompoundTag();tag.putInt("transformable",1);
        if(mace){tag.putInt("shovel",1);tag.putInt("halloween_mace",1);tag.putInt("spellwithcooldown",1);tag.putInt("craftusable",1);}
        else{tag.putInt("staff",1);tag.putInt("magicwand",1);tag.putInt("vex_wand",1);}
        if(academy){tag.putInt("upgraded",1);tag.putInt("mode",1);}
        var p=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).stacksTo(1).overrideDescription(mace?"halloween_mace":"vex_wand")
                .component(DataComponents.ITEM_MODEL,model)
                .component(DataComponents.TOOLTIP_STYLE,Identifier.parse(mace?"lbc:tier_s":academy?"lbc:tier_a":"lbc:tier_b"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        Item item;
        if(mace)item=new NativeMace(p.durability(500).attributes(MaceItem.createAttributes())
                .component(DataComponents.TOOL,MaceItem.createToolProperties()).component(DataComponents.WEAPON,new Weapon(1))
                .repairable(Items.BREEZE_ROD).enchantable(15));
        else item=new SimplePolymerItem(p,Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=me.skywars2.SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer owner&&game.items().wands().use(owner,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        };
        return Registry.register(BuiltInRegistries.ITEM,key,item);
    }
    private static final class NativeMace extends MaceItem implements PolymerItem {
        NativeMace(Properties p){super(p);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.MACE;}
    }
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static boolean isWind(ItemStack stack){return !stack.isEmpty()&&(stack.is(WIND)||stack.is(ACADEMY_WIND)||data(stack).getIntOr("vex_wand",0)==1);}
    public static boolean isMace(ItemStack stack){return !stack.isEmpty()&&(stack.is(MACE)||data(stack).getIntOr("halloween_mace",0)==1);}
    public static boolean academy(ItemStack stack){return isWind(stack)&&(stack.is(ACADEMY_WIND)||data(stack).getIntOr("upgraded",0)==1);}
    public static boolean managed(Entity entity){return entity instanceof WindCharge&&entity.entityTags().contains(PROJECTILE);}
    private boolean owner(ServerPlayer p){return game.playing()&&mana.visible(p)&&!game.isObserver(p);}
    private boolean paused(Entity e){return !game.server().tickRateManager().runsNormally()||e.entityTags().contains("time_stopped")
            ||game.items().timeStop().frozen(e)||e.level() instanceof net.minecraft.server.level.ServerLevel l&&game.items().timeStop().stopped(l);}
    private boolean castAllowed(ServerPlayer p){return owner(p)&&!paused(p)&&!game.items().actionsBlocked(p)
            &&mana.noMagic(p)<=0&&!game.items().frost().isFrozen(p)&&!game.items().magic().suppressed(p)
            &&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());}
    public boolean use(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!isWind(stack)||!owner(p)||paused(p)||game.items().actionsBlocked(p)||game.items().frost().isFrozen(p))return false;
        // Family mode changes are free, including an empty mana pool/personal nomagic.
        if(academy(stack)&&p.isShiftKeyDown()){
            var tag=data(stack);int mode=Math.max(1,Math.min(4,tag.getIntOr("mode",1)))%4+1;
            tag.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));p.getInventory().setChanged();
            p.sendSystemMessage(Component.translatable(switch(mode){case 1->"mode_default";case 2->"mode_user";case 3->"mode_not_user";default->"mode_windball";}));return true;
        }
        if(!castAllowed(p))return false;
        int cost=mana.score(ScoreHolder.forNameOnly("vex_wand_mana_use"),"lbc.math").get();
        if(cost<0||!mana.trySpendWand(p,cost))return false;
        if(!academy(stack)&&LbcAcademy.trial(stack)&&p.isShiftKeyDown())meditate(p,hand,stack);
        else cast(p,academy(stack)?Math.max(1,Math.min(4,data(stack).getIntOr("mode",1))):1);
        game.items().wands().wandCasted(p);return true;
    }
    /** Source origin is the eye-positioned spell execution context. */
    void cast(ServerPlayer p,int mode){
        if(mode<1||mode>4)throw new IllegalArgumentException("Wind mode must be 1..4");
        Vec3 origin=p.getEyePosition();
        LbcOverload.sound(p,origin,"minecraft:entity.breeze.wind_burst",1,mode==4?2:1,0);
        if(mode==4){for(int i=0;i<5;i++)spawn(p);return;}
        p.level().sendParticles(ParticleTypes.GUST,origin.x,origin.y+1,origin.z,40,5,5,5,0);
        var cause=game.items().indirectKills().wildCause(p);
        if(mode==2){push(p,p,p.getLookAngle(),false,cause);return;}
        for(var target:nearby(p,origin,5))if(mode!=3||target!=p)push(p,target,p.getLookAngle(),false,cause);
    }
    private List<LivingEntity> nearby(ServerPlayer p,Vec3 origin,double radius){
        return p.level().getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(radius),
                target->target.getType().builtInRegistryHolder().is(MOBS)&&target.position().distanceToSqr(origin)<=radius*radius&&eligible(target));
    }
    private boolean eligible(Entity target){return target instanceof LivingEntity&&game.items().wands().validTarget(target)&&!paused(target);}
    void meditate(ServerPlayer p,InteractionHand hand,ItemStack stack){
        if(p.getItemInHand(hand)!=stack||!LbcAcademy.trial(stack))return;
        // Completion is immediate and belongs only to the actual used stack.
        game.items().academy().ready(p,hand,stack);
        Vec3 origin=p.getEyePosition();
        game.items().indirectKills().trapChanges(p.level(),p,true,()->game.maps().placeLoadedTemplate(p,AIR,BlockPos.containing(origin.add(-5,-1,-5)),1));
        p.level().sendParticles(ParticleTypes.GUST_EMITTER_LARGE,true,false,origin.x,origin.y,origin.z,20,6,6,6,0);
        var cause=game.items().indirectKills().wildCause(p);
        // Source faces TARGET eyes from caster feet before moving the execution position to TARGET.
        for(var target:nearby(p,p.position(),11))push(p,target,target.getEyePosition().subtract(p.position()).normalize().scale(10),true,cause);
    }
    void push(ServerPlayer p,LivingEntity target,Vec3 motion,boolean set){push(p,target,motion,set,game.items().indirectKills().wildCause(p));}
    private void push(ServerPlayer p,LivingEntity target,Vec3 motion,boolean set,LbcIndirectKills.Owner cause){
        if(!eligible(target)||target.level()!=p.level())return;
        Vec3 next=set?motion:target.getDeltaMovement().add(motion);
        // Paper MotionBrigadier caps the largest component at ten, preserving direction.
        double max=Math.max(Math.abs(next.x),Math.max(Math.abs(next.y),Math.abs(next.z)));
        if(max>10)next=next.scale(10/max);
        target.setDeltaMovement(next);target.needsSync=true;
        if(target instanceof ServerPlayer player)player.connection.send(new ClientboundSetEntityMotionPacket(player));
        if(target!=p)game.items().indirectKills().displaced(target,cause);
    }
    private WindCharge spawn(ServerPlayer p){
        Vec3 origin=p.getEyePosition();if(!p.level().hasChunkAt(BlockPos.containing(origin)))return null;
        var shot=new NativeCharge(p.level());shot.setOwner(p);shot.setPos(origin);
        var look=p.getLookAngle();var random=p.getRandom();
        shot.setDeltaMovement(new Vec3((Math.floor(look.x*10000)+random.nextInt(2001)-1000)*.0001,
                (Math.floor(look.y*10000)+random.nextInt(401)-200)*.0001,
                (Math.floor(look.z*10000)+random.nextInt(2001)-1000)*.0001));
        LbcShooters.orient(shot);shot.entityTags().add(PROJECTILE);shot.entityTags().add("magic");game.items().markCurrentItemEpoch(shot);
        if(!p.level().addFreshEntity(shot)){shot.discard();return null;}
        shots.add(shot);game.items().timeStop().prepareFrozen(shot);return shot;
    }
    private boolean valid(WindCharge shot){return !shot.isRemoved()&&game.items().currentEpoch(shot)&&shot instanceof NativeCharge n&&!n.retired
            &&shot.getOwner() instanceof ServerPlayer p&&owner(p)&&p.level()==shot.level()
            &&p.level().hasChunkAt(shot.blockPosition())&&!game.items().magic().suppressedAt(p.level(),shot.position());}
    public boolean canAffect(Projectile shot,Entity target){return shot instanceof WindCharge charge&&valid(charge)&&eligible(target)
            &&shot.getOwner() instanceof ServerPlayer p&&game.items().wands().projectileTarget(p,target);}
    public boolean explosionTarget(Entity source,Entity target){
        if(!managed(source))return true;
        return source instanceof WindCharge charge&&valid(charge)&&eligible(target)&&charge.getOwner() instanceof ServerPlayer p
                &&(target==p||game.items().wands().projectileTarget(p,target));
    }
    private final class NativeCharge extends WindCharge {
        boolean retired;
        NativeCharge(Level level){super(EntityTypes.WIND_CHARGE,level);}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public void tick(){if(!valid(this)){unloaded(this);return;}if(paused(this))return;super.tick();}
        @Override protected void explode(Vec3 pos){
            if(!valid(this)||paused(this))return;
            var p=(ServerPlayer)getOwner();var cause=game.items().indirectKills().wildCause(p);
            Map<LivingEntity,Vec3> before=new LinkedHashMap<>();
            for(var target:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(pos,pos).inflate(3),e->explosionTarget(this,e)))before.put(target,target.getDeltaMovement());
            // The native calculator retains radius, exposure, block triggers, particles and jump scaling.
            super.explode(pos);
            for(var entry:before.entrySet())if(entry.getKey()!=p&&!entry.getValue().equals(entry.getKey().getDeltaMovement()))game.items().indirectKills().displaced(entry.getKey(),cause);
        }
    }
    public void tick(){
        for(var shot:List.copyOf(shots))if(!valid(shot))unloaded(shot);
        for(var p:game.server().getPlayerList().getPlayers()){
            if(!owner(p)||paused(p))continue;
            int ticks=seconds.getOrDefault(p,0)+1;
            if(ticks>=20){second(p);seconds.put(p,0);}else seconds.put(p,ticks);
        }
    }
    void second(ServerPlayer p){if(owner(p)&&!paused(p)){var score=mana.score(p,"cooldown_wind_charge");if(score.get()>0)score.set(score.get()-1);}}
    /** Native jumpFromGround tail; the source armor pass consumes the real jump statistic. */
    public void jump(ServerPlayer p){tickPlayer(p);}
    void tickPlayer(ServerPlayer p){
        // Source jump ability has no mana/antimagic gate; ordinary action/role/freeze rules still apply.
        if(!owner(p)||paused(p)||game.items().actionsBlocked(p)||p.getXRot()<80||p.getXRot()>90||(!isMace(p.getMainHandItem())&&!isMace(p.getOffhandItem()))
                ||mana.hudValue(p,"cooldown_wind_charge")>0||!eligible(p))return;
        mana.score(p,"cooldown_wind_charge").set(5);seconds.remove(p);
        Vec3 at=p.position();p.level().sendParticles(ParticleTypes.GUST_EMITTER_LARGE,true,false,at.x,at.y,at.z,1,0,0,0,0);
        LbcOverload.sound(p,at,"minecraft:entity.breeze.wind_burst",1,1,0);
        // Source motion set ~ ~1 ~ resolves relative to current velocity, not world position.
        push(p,p,new Vec3(0,1,0),false);
    }
    public Component hud(ServerPlayer p){
        if(!isMace(p.getMainHandItem())&&!isMace(p.getOffhandItem()))return Component.empty();
        int value=mana.hudValue(p,"cooldown_wind_charge");
        return value>0?Component.literal("    \uE105 ").append(Component.literal(Integer.toString(value)).withStyle(ChatFormatting.WHITE))
                :Component.literal("    \uE104 ").append(Component.translatable("ready").withStyle(ChatFormatting.GREEN));
    }
    public void purify(ServerPlayer p){mana.score(p,"cooldown_wind_charge").set(0);seconds.remove(p);}
    public int count(){return shots.size();}
    List<WindCharge> snapshot(){return List.copyOf(shots);}
    public boolean loaded(Entity e){if(!managed(e))return false;if(e instanceof NativeCharge charge&&valid(charge))shots.add(charge);else unloaded(e);return true;}
    public void unloaded(Entity e){if(managed(e)){if(e instanceof NativeCharge charge)charge.retired=true;shots.remove(e);if(!e.isRemoved())e.discard();}}
    public void forget(ServerPlayer p){for(var shot:List.copyOf(shots))if(shot.getOwner()==p)unloaded(shot);purify(p);}
    public void stop(){for(var shot:List.copyOf(shots))unloaded(shot);for(var p:game.server().getPlayerList().getPlayers())purify(p);seconds.clear();}
}
