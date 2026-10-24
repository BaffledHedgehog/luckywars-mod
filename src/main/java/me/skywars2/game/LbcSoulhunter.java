package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

/** Soulhunter keeps native mace smashing; its four source counters belong to the player. */
public final class LbcSoulhunter {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=net.minecraft.tags.TagKey.create(
            Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    public static final Item MACE=registerMace();
    private final GameController game;
    private final LbcMana mana;
    private final Map<ServerPlayer,Integer> seconds=new HashMap<>();
    LbcSoulhunter(GameController game,LbcMana mana){this.game=game;this.mana=mana;}
    public static void register(){}
    private static Item registerMace(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:soulhunter_mace"));
        var model=Identifier.parse("lbc:soulhunter_mace");
        var tag=new CompoundTag();for(var name:new String[]{"transformable","shovel","soulhunter_mace","spellwithcooldown"})tag.putInt(name,1);
        var properties=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}
                .setId(key).durability(500).attributes(MaceItem.createAttributes())
                .component(DataComponents.TOOL,MaceItem.createToolProperties()).component(DataComponents.WEAPON,new Weapon(1))
                .repairable(Items.BREEZE_ROD).enchantable(15).overrideDescription("soulhunter_mace")
                .component(DataComponents.ITEM_MODEL,model).component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_ss"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        return Registry.register(BuiltInRegistries.ITEM,key,new NativeMace(properties));
    }
    private static final class NativeMace extends MaceItem implements PolymerItem {
        NativeMace(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.MACE;}
        @Override public int getUseDuration(ItemStack stack,LivingEntity user){return Integer.MAX_VALUE;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var game=me.skywars2.SkyWars2.game();
            if(game==null||!(player instanceof ServerPlayer p)||!game.items().soulhunter().allowed(p))return InteractionResult.FAIL;
            game.items().soulhunter().use(p,hand);
            if(hand==InteractionHand.MAIN_HAND&&game.config().offhandWands)game.items().wands().use(p,InteractionHand.OFF_HAND);
            p.startUsingItem(hand);return InteractionResult.CONSUME;
        }
        @Override public void onUseTick(Level level,LivingEntity living,ItemStack stack,int remaining){
            var game=me.skywars2.SkyWars2.game();
            if(game!=null&&living instanceof ServerPlayer p&&p.getUseItem()==stack){
                if(!game.items().soulhunter().allowed(p)){p.stopUsingItem();return;}
                game.items().soulhunter().use(p,p.getUsedItemHand());
            }
        }
    }
    public static boolean isMace(ItemStack stack){return !stack.isEmpty()&&(stack.is(MACE)
            ||stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("soulhunter_mace",0)==1);}
    private boolean holding(ServerPlayer p){return isMace(p.getMainHandItem())||isMace(p.getOffhandItem());}
    private boolean owner(ServerPlayer p){return game.playing()&&mana.visible(p)&&game.canUseGameplayItems(p);}
    private boolean paused(LivingEntity entity){return !game.server().tickRateManager().runsNormally()
            ||entity.entityTags().contains("time_stopped")||game.items().timeStop().frozen(entity)
            ||entity.level() instanceof net.minecraft.server.level.ServerLevel level&&game.items().timeStop().stopped(level);}
    private boolean allowed(ServerPlayer p){return owner(p)&&!paused(p)&&!game.items().actionsBlocked(p)
            &&!game.items().frost().isFrozen(p)&&!LbcGreed.active(p);}
    public boolean use(ServerPlayer p,InteractionHand hand){
        if(!isMace(p.getItemInHand(hand))||!allowed(p))return false;
        String dimension=p.level().dimension().identifier().toString();
        if(dimension.equals("minecraft:nexus")||dimension.equals("minecraft:imprinted"))return false;
        // (user) Posture selects exactly one ability, with an independent cooldown.
        String counter=p.isShiftKeyDown()?"mace_4":"mace_2";
        if(mana.hudValue(p,counter)>0)return false;
        if(p.isShiftKeyDown())splash(p);else dash(p);return true;
    }
    void dash(ServerPlayer p){
        mana.score(p,"mace_2").set(5);motion(p,p.getLookAngle().scale(3),true);
        var at=p.position();p.level().sendParticles(ParticleTypes.GUST_EMITTER_SMALL,at.x,at.y,at.z,1,0,0,0,0);
        souls(p);LbcOverload.sound(p,at,"minecraft:entity.phantom.flap",1,1.3f,0);
    }
    void splash(ServerPlayer p){
        mana.score(p,"mace_4").set(20);var at=p.position();
        for(var viewer:game.server().getPlayerList().getPlayers())if(viewer.level()==p.level()&&viewer.position().distanceToSqr(at)>.0001)
            p.level().sendParticles(viewer,ParticleTypes.GUST_EMITTER_LARGE,true,false,at.x,at.y,at.z,10,5,5,5,0);
        souls(p);LbcOverload.sound(p,at,"minecraft:entity.zombie.break_wooden_door",3,.8f,0);
        var cause=game.items().indirectKills().coldCause(p,true);
        for(var target:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),
                e->e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(at)<=100&&!paused(e)&&game.items().wands().target(p,e)))
            game.items().indirectKills().withCause(target,cause,()->target.hurtServer(p.level(),p.damageSources().playerAttack(p),15));
        // Seven overlapping source fills form one stepped disk, y=0..5; each block drops once.
        BlockPos center=p.blockPosition();
        game.items().indirectKills().trapChanges(p,()->{
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)if(inSplash(x,z))for(int y=0;y<=5;y++){
                var pos=center.offset(x,y,z);
                if(p.level().isOutsideBuildHeight(pos)||!p.level().hasChunkAt(pos)||p.level().getBlockState(pos).isAir())continue;
                p.level().destroyBlock(pos,true,p);
            }
        });
    }
    static boolean inSplash(int x,int z){
        int ax=Math.abs(x),az=Math.abs(z);
        return ax<=3&&az<=10||ax<=5&&az<=9||ax<=6&&az<=8||ax<=7&&az<=7
                ||ax<=8&&az<=6||ax<=9&&az<=5||ax<=10&&az<=3;
    }
    private void souls(ServerPlayer p){var at=p.position();p.level().sendParticles(ParticleTypes.SOUL,at.x,at.y+.5,at.z,100,3,3,3,0);}
    private void motion(ServerPlayer p,Vec3 motion,boolean set){
        Vec3 next=set?motion:p.getDeltaMovement().add(motion);
        double max=Math.max(Math.abs(next.x),Math.max(Math.abs(next.y),Math.abs(next.z)));
        if(max>10)next=next.scale(10/max);
        p.setDeltaMovement(next);p.needsSync=true;p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }
    /** Invoked only after a real native jump, rather than by polling the look pitch. */
    public void jump(ServerPlayer p){
        if(!allowed(p)||!holding(p)||p.getXRot()<80||p.getXRot()>90||mana.hudValue(p,"mace_3")>0)return;
        mana.score(p,"mace_3").set(5);var at=p.position();
        p.level().sendParticles(ParticleTypes.GUST_EMITTER_LARGE,true,false,at.x,at.y,at.z,1,0,0,0,0);
        LbcOverload.sound(p,at,"minecraft:entity.breeze.wind_burst",1,1,0);motion(p,new Vec3(0,1.2,0),false);
    }
    void tickPlayer(ServerPlayer p){
        if(!allowed(p))return;
        int stamina=Math.max(0,Math.min(60,mana.hudValue(p,"mace_1")));
        if(p.isSprinting()&&!p.onGround()&&holding(p)&&!p.entityTags().contains("unstamined")){
            mana.score(p,"mace_1").set(Math.max(0,stamina-1));motion(p,p.getLookAngle().scale(.2),false);
            var at=p.position();p.level().sendParticles(ParticleTypes.GUST,at.x,at.y,at.z,1,0,0,0,0);
            p.level().sendParticles(ParticleTypes.SOUL,at.x,at.y+.5,at.z,1,0,0,0,0);
            LbcOverload.sound(p,at,"minecraft:block.wool.step",.5f,2,0);
            if(stamina<=1){p.entityTags().add("unstamined");LbcOverload.sound(p,at,"minecraft:block.glass.break",1,.8f,0);}
        }else if(!p.isSprinting()&&p.onGround()&&stamina<60){
            mana.score(p,"mace_1").set(stamina+1);if(stamina+1==60)p.entityTags().remove("unstamined");
        }
    }
    void second(ServerPlayer p){if(owner(p)&&!paused(p))for(var name:new String[]{"mace_2","mace_3","mace_4"}){
        var score=mana.score(p,name);if(score.get()>0)score.set(score.get()-1);
    }}
    public void tick(){for(var p:game.server().getPlayerList().getPlayers())if(owner(p)&&!paused(p)){
        tickPlayer(p);int value=seconds.getOrDefault(p,0)+1;
        if(value>=20){second(p);seconds.put(p,0);}else seconds.put(p,value);
    }}
    public Component hud(ServerPlayer p){
        if(!holding(p))return Component.empty();
        boolean exhausted=p.entityTags().contains("unstamined");
        var out=Component.literal(exhausted?"    \uE201 ":"    \uE197 ")
                .append(Component.literal(Integer.toString(mana.hudValue(p,"mace_1"))).withStyle(exhausted?ChatFormatting.RED:ChatFormatting.WHITE));
        var icons=new String[]{"    \uE198 ","    \uE199 ","    \uE200 "};
        for(int i=0;i<3;i++){int value=mana.hudValue(p,"mace_"+(i+2));out.append(icons[i]).append(value>0
                ?Component.literal(Integer.toString(value)).withStyle(ChatFormatting.WHITE):Component.translatable("ready").withStyle(ChatFormatting.GREEN));}
        return out;
    }
    public void crafted(ServerPlayer p){p.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,p.getX(),p.getY()+1,p.getZ(),100,1,1,1,1);
        LbcOverload.sound(p,p.position(),"minecraft:block.enchantment_table.use",1,0,0);}
    public void purify(ServerPlayer p){seconds.remove(p);p.entityTags().remove("unstamined");mana.score(p,"mace_1").set(60);
        for(var name:new String[]{"mace_2","mace_3","mace_4"})mana.score(p,name).set(0);}
    public void forget(ServerPlayer p){purify(p);}
    public void stop(){for(var p:game.server().getPlayerList().getPlayers())purify(p);seconds.clear();}
}
