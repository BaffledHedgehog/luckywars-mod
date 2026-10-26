package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real Mortis melee victims and the charged, immediate Death Scythe ray. */
public final class LbcSpecialTools {
    public static final Item SCYTHE=createScythe();
    public static final Item BANHAMMER=createHammer();
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String CHARGING="swordcharging3",CHARGED="swordcharged3";
    private final GameController game;
    private final Set<Erase> erasures=new LinkedHashSet<>();
    private final Set<Hammer> hammers=new LinkedHashSet<>();
    LbcSpecialTools(GameController game){this.game=game;}
    public static void register(){}
    private static Item createScythe(){
        var id=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:death_scythe"));
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return Identifier.parse("lbc:death_skythe");}}
                .setId(id).durability(25).overrideDescription("death_skythe");
        return Registry.register(BuiltInRegistries.ITEM,id,new SimplePolymerItem(properties,Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var g=SkyWars2.game();return g!=null&&player instanceof ServerPlayer p&&g.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    private static Item createHammer(){
        var id=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:banhammer"));
        return Registry.register(BuiltInRegistries.ITEM,id,new SimplePolymerItem(new Item.Properties(){
            @Override public Identifier effectiveModel(){return Identifier.parse("lbc:banhammer");}
        }.setId(id).durability(25).overrideDescription("banhammer"),Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var g=SkyWars2.game();return g!=null&&player instanceof ServerPlayer p&&g.items().wands().use(p,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    static boolean scythe(ItemStack stack){return !stack.isEmpty()&&(stack.is(SCYTHE)||LbcFartus.data(stack).getIntOr("death_skythe",0)==1);}
    public static boolean isLegacyScythe(ItemStack stack){return !stack.is(SCYTHE)&&scythe(stack);}
    static boolean hammer(ItemStack stack){return !stack.isEmpty()&&(stack.is(BANHAMMER)||LbcFartus.data(stack).getIntOr("banhammer",0)==1);}
    public static boolean isLegacyHammer(ItemStack stack){return !stack.is(BANHAMMER)&&hammer(stack);}
    static boolean mortis(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("mortis",0)==1;}
    private boolean active(ServerPlayer p){return game.items().mana().visible(p)&&game.items().wands().validTarget(p)
            &&!game.items().actionsBlocked(p)&&!p.entityTags().contains("time_stopped")&&!game.items().timeStop().stopped(p.level());}
    private boolean magic(ServerPlayer p){return active(p)&&!game.items().magic().suppressed(p)
            &&!p.entityTags().contains("nomagic_active")&&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());}
    private static boolean resident(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    int charges(ServerPlayer p){return game.items().mana().hudValue(p,CHARGED);}
    /** The source kill criterion intentionally counts credited kills while the scythe is held. */
    public void died(LivingEntity victim,DamageSource source){
        var p=LbcIndirectKills.playerSource(source.getEntity());
        if(p==null&&victim.getKillCredit() instanceof ServerPlayer credited)p=credited;
        if(p==null||p==victim||!scythe(p.getMainHandItem())||!active(p))return;
        var dimension=p.level().dimension().identifier().getPath();
        if(dimension.equals("nexus")||dimension.equals("imprinted"))return;
        var mana=game.items().mana();var progress=mana.score(p,CHARGING);progress.add(1);
        if(progress.get()>=2){mana.score(p,CHARGED).add(1);progress.set(0);}
        p.level().sendParticles(ParticleTypes.SQUID_INK,p.getX(),p.getY()+1,p.getZ(),3,.5,1,.5,0);
    }
    /** Called after accepted native player melee, never by searching nearby HurtTime entities. */
    public void melee(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer p)||!LbcDamage.isMeleeAttack(source,p)||!active(p)
                ||!game.items().wands().projectileTarget(p,victim,true)||LbcGreed.active(victim))return;
        if(hammer(p.getMainHandItem())){
            var mana=game.items().mana();var score=mana.score(p,"swordcharging7");score.add(1);
            if(score.get()>=15){mana.score(p,"swordcharged7").add(1);score.set(0);}return;
        }
        if(!mortis(p.getMainHandItem()))return;
        var level=p.level();var forward=Vec3.directionFromRotation(0,p.getYRot());var destination=victim.position().add(forward.scale(4));
        var start=p.position();
        // The old /tp lunges through the actual victim, ending four blocks beyond it.
        for(int i=0;i<=16;i++)if(!resident(level,BlockPos.containing(start.lerp(destination,i/16.0))))return;
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20,2,true,false),p);
        p.teleportTo(destination.x,destination.y,destination.z);
        var dust=new DustParticleOptions(0x4c0000,4);
        for(int n=-4;n<=4;n++){
            var at=destination.add(forward.scale(-n));level.sendParticles(dust,at.x,at.y,at.z,5,.5,1,.5,0);
        }
        for(float pitch:new float[]{0,1,2})LbcOverload.sound(p,destination,"minecraft:entity.ender_dragon.flap",.33f,pitch,0);
        var back=victim.getEyePosition().subtract(destination).multiply(1,0,1).normalize();
        if(back.lengthSqr()<1e-10)back=forward.scale(-1);
        var direction=back;var positions=new LinkedHashSet<BlockPos>();var floors=new LinkedHashSet<BlockPos>();
        // Legacy increments before <=40: 41 quarter-block samples, not forty whole blocks.
        for(int n=0;n<=40;n++){
            var at=destination.add(direction.scale(n*.25));
            if(!resident(level,BlockPos.containing(at)))break;
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-.5,0,-.5)),BlockPos.containing(at.add(.5,1,.5))))if(resident(level,pos))positions.add(pos.immutable());
            var floor=BlockPos.containing(at.add(0,-1,0));if(resident(level,floor))floors.add(floor);
        }
        game.items().indirectKills().trapChanges(p,()->{
            for(var pos:positions)if(!level.getBlockState(pos).isAir())level.destroyBlock(pos,true,p);
            for(var floor:floors)if(level.getBlockState(floor).is(AIRS))level.setBlock(floor,Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState(),Block.UPDATE_ALL);
        });
    }
    boolean use(ServerPlayer p,InteractionHand hand){
        if(hammer(p.getItemInHand(hand)))return useHammer(p,hand);
        if(!scythe(p.getItemInHand(hand))||!magic(p)||charges(p)<=0||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var level=p.level();var direction=p.getLookAngle();var start=p.getEyePosition().add(direction.scale(hand==InteractionHand.OFF_HAND?.5:0));
        if(!resident(level,BlockPos.containing(start)))return false;
        var points=new ArrayList<Vec3>();
        for(int n=0;n<=70;n++){
            var at=start.add(direction.scale(n));if(!resident(level,BlockPos.containing(at))||game.items().magic().suppressedAt(level,at))break;
            points.add(at);
        }
        if(points.isEmpty())return false;
        game.items().mana().score(p,CHARGED).add(-1);
        for(int n=0;n<=5;n++)LbcOverload.sound(p,start,"minecraft:entity.warden.attack_impact",1,n*.2f,0);
        game.items().mana().award(p,"lbc:true_advancements/hoes/death_skytche");
        var last=points.getLast();var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(start,last).inflate(6),e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(p,e,true));
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),p);
        var erased=new LinkedHashSet<BlockPos>();
        for(int sample=0;sample<points.size();sample++){
            var at=points.get(sample);level.sendParticles(ParticleTypes.SONIC_BOOM,at.x,at.y,at.z,15,1,1,1,0);
            // Cactus keeps the original cooldown. A blocked earlier sample can still be followed by a real hit.
            for(var target:targets)if(target.isAlive()&&target.position().distanceToSqr(at)>.0001&&target.position().distanceToSqr(at)<=36
                    &&!target.entityTags().contains("nomagic_active")&&!game.items().magic().suppressedAt(level,target.position()))target.hurtServer(level,damage,15);
            if(sample<6)continue;
            var center=BlockPos.containing(at);
            for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5)))
                if(level.getRandom().nextFloat()<.08f&&resident(level,pos)&&!level.getBlockState(pos).isAir())erased.add(pos.immutable());
        }
        if(!erased.isEmpty()){
            var task=new Erase(p,erased);erasures.add(task);
            if(!task.step())game.items().stonekivi().queuedWork(task);
        }
        game.items().wands().wandCasted(p);return true;
    }
    /** Coalesced source integrity rolls: immediate damage/visuals, visible terrain batches, no structure placement. */
    private final class Erase implements LbcStonekivi.Work {
        final ServerPlayer owner;final ServerLevel level;final String epoch;final Iterator<BlockPos> positions;final LbcIndirectKills.Owner cause;boolean cancelled;
        Erase(ServerPlayer p,Set<BlockPos> positions){owner=p;level=p.level();epoch=game.items().epoch();this.positions=positions.iterator();cause=game.items().indirectKills().coldCause(p,true);}
        @Override public boolean step(){
            if(cancelled||!game.items().currentEpoch(epoch)||!game.playing()||game.stage()==GameController.Stage.ENDING){cancel();return true;}
            if(game.items().timeStop().stopped(level))return false;
            long deadline=System.nanoTime()+1_000_000;
            game.items().indirectKills().trapChanges(level,cause,()->{
                for(int n=0;n<512&&positions.hasNext()&&(n==0||System.nanoTime()<deadline);n++){
                    var pos=positions.next();if(resident(level,pos)&&!level.getBlockState(pos).isAir())
                        level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                }
            });
            if(!positions.hasNext()){erasures.remove(this);return true;}return false;
        }
        @Override public void cancel(){cancelled=true;erasures.remove(this);}
    }
    int pending(){return erasures.size();}
    int hammerCount(){return hammers.size();}
    List<Hammer> hammerSnapshot(){return List.copyOf(hammers);}
    /** First resident hit only; the old recursive command counter represented201 samples at0.4 blocks. */
    private boolean useHammer(ServerPlayer p,InteractionHand hand){
        if(!magic(p)||game.items().mana().hudValue(p,"swordcharged7")<=0||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var level=p.level();var start=p.getEyePosition();var direction=p.getLookAngle();Vec3 hit=null;
        var candidates=level.getEntitiesOfClass(LivingEntity.class,new AABB(start,start.add(direction.scale(80.4))).inflate(2),e->e!=p&&e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validTarget(e));
        for(int n=0;n<=200;n++){
            var at=start.add(direction.scale(n*.4));var pos=BlockPos.containing(at);
            if(!resident(level,pos)||game.items().magic().suppressedAt(level,at))break;
            var bounds=new AABB(at.x,at.y,at.z,at.x+1,at.y+1,at.z+1);
            var victim=candidates.stream().filter(e->e.getBoundingBox().intersects(bounds)).min(Comparator.comparingDouble(e->e.distanceToSqr(at))).orElse(null);
            if(victim!=null){hit=victim.position();break;}
            if(!level.getBlockState(pos).is(AIRS)){hit=at.subtract(direction.scale(.4));break;}
        }
        if(hit==null)return false;
        game.items().mana().score(p,"swordcharged7").add(-1);
        level.sendParticles(new DustParticleOptions(0xff0000,2),hit.x,hit.y,hit.z,200,4,4,4,0);
        LbcOverload.sound(p,hit,"minecraft:lbcsounds.supply_fall",5,0,0);
        hammers.add(new Hammer(p,hit));game.items().wands().wandCasted(p);return true;
    }
    final class Hammer {
        final ServerPlayer author;final ServerLevel level;final String epoch;final Vec3 impact;
        final eu.pb4.polymer.virtualentity.api.ElementHolder holder=new eu.pb4.polymer.virtualentity.api.ElementHolder();
        final eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement display;
        Vec3 point;int age;boolean impacted;
        Hammer(ServerPlayer p,Vec3 at){author=p;level=p.level();epoch=game.items().epoch();impact=new Vec3(at.x,Math.floor(at.y),at.z);point=impact.add(0,200,0);
            var stack=new ItemStack(Items.CARROT_ON_A_STICK);stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:banhammer3d"));
            display=new eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement(stack);display.setItemDisplayContext(net.minecraft.world.item.ItemDisplayContext.NONE);
            display.setScale(new org.joml.Vector3f(4));display.setTeleportDuration(1);display.setViewRange(10);holder.addElement(display);
            game.visualTracking().attach(holder,level,()->point);viewers();holder.tick();
        }
        void viewers(){var players=game.server().getPlayerList().getPlayers();
            for(var c:List.copyOf(holder.getWatchingPlayers()))if(c.player.connection!=c||!players.contains(c.player)||c.player.level()!=level||c.player.position().distanceToSqr(point)>409600)holder.stopWatching(c);
            for(var p:players)if(p.level()==level&&p.position().distanceToSqr(point)<=409600)holder.startWatching(p);
        }
        void destroy(){holder.destroy();hammers.remove(this);}
    }
    boolean immuneToHammer(ServerPlayer p){return LbcGreed.active(p)||game.config().lbcSkills&&game.players().get(p.getUUID()).lbcSkill==12;}
    /** One native disconnect per affected player; the source's client-hanging particle flood is deliberately gone. */
    void impact(Hammer hammer){
        if(hammer.impacted)return;hammer.impacted=true;
        var targets=hammer.level.players().stream().filter(p->p.position().distanceToSqr(hammer.impact)<=30.25&&game.items().wands().validRoleTarget(p,false)).toList();
        if(!targets.isEmpty()&&game.server().getPlayerList().getPlayer(hammer.author.getUUID())==hammer.author)
            game.items().mana().award(hammer.author,"lbc:true_advancements/axes/banhammer");
        for(var p:targets)if(!immuneToHammer(p))p.connection.disconnect(Component.translatable("skywars2.banhammer.kick"));
        hammer.level.sendParticles(ParticleTypes.EXPLOSION,hammer.impact.x,hammer.impact.y,hammer.impact.z,1000,5,.5,5,0);
        hammer.level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xffff1919),hammer.impact.x,hammer.impact.y,hammer.impact.z,1000,1,1,1,0);
    }
    void tick(){for(var h:List.copyOf(hammers)){
        if(!game.items().currentEpoch(h.epoch)||!game.playing()||game.stage()==GameController.Stage.ENDING){h.destroy();continue;}
        h.viewers();if(game.items().timeStop().stopped(h.level)||!resident(h.level,BlockPos.containing(h.impact)))continue;
        if(h.age<100)h.point=h.impact.add(0,200-2*(h.age+1),0);
        if(++h.age==100)impact(h);h.holder.tick();if(h.age>=200)h.destroy();
    }}
    Component hud(ServerPlayer p){
        var hud=Component.empty();
        if(hammer(p.getMainHandItem())||hammer(p.getOffhandItem()))hud.append(Component.literal("    \uE103 \u00a7fx"))
                .append(Component.literal(Integer.toString(game.items().mana().hudValue(p,"swordcharged7"))).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" [")).append(Component.literal(Integer.toString(game.items().mana().hudValue(p,"swordcharging7"))).withStyle(ChatFormatting.AQUA)).append(Component.literal("/\u00a7b15\u00a7f]"));
        if(!scythe(p.getMainHandItem())&&!scythe(p.getOffhandItem()))return hud;
        return hud.append(Component.literal("    \uE103 \u00a7fx")).append(Component.literal(Integer.toString(charges(p))).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" [")).append(Component.literal(Integer.toString(game.items().mana().hudValue(p,CHARGING))).withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal("/\u00a752\u00a7f]"));
    }
    void clear(ServerPlayer p){for(var name:new String[]{CHARGING,CHARGED,"swordcharging7","swordcharged7"})game.items().mana().score(p,name).set(0);}
    void stop(){for(var task:List.copyOf(erasures))task.cancel();for(var hammer:List.copyOf(hammers))hammer.destroy();for(var p:game.server().getPlayerList().getPlayers())clear(p);}
}
