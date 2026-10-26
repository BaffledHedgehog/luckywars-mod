package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.mixin.SyringeMobAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Three owned summons selected once on equip; reversible native modifiers and real mob attacks. */
public final class LbcSyringe {
    public static final Item ITEM=registerItem();
    static final Identifier MODIFIER=Identifier.parse("lbc:syringe_buff");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String SAVED="skywars2.syringe:", ACTIVE="skywars2.syringe";
    private static final String[] SOUNDS={"minecraft:lbcsounds.fuwafuwa","minecraft:lbcsounds.awaawa","minecraft:lbcsounds.yurayura"};
    private static Item registerItem(){
        try(var input=LbcSyringe.class.getResourceAsStream("/skywars2/syringe.json")){
            if(input==null)throw new IllegalStateException("Missing syringe source");
            var row=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:siggewinne_syringe"));
            var model=Identifier.parse(row.get("model").getAsString());
            var lore=new ArrayList<net.minecraft.network.chat.Component>();
            for(var line:row.getAsJsonArray("lore"))lore.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,line).getOrThrow());
            var props=new Item.Properties(){@Override public Identifier effectiveModel(){return model;}}.setId(key).stacksTo(1)
                    .component(DataComponents.ITEM_MODEL,model).component(DataComponents.TOOLTIP_STYLE,Identifier.parse(row.get("tooltip").getAsString()))
                    .component(DataComponents.CUSTOM_NAME,ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,row.get("name")).getOrThrow())
                    .component(DataComponents.LORE,new ItemLore(lore)).component(DataComponents.CUSTOM_DATA,CustomData.of(TagParser.parseCompoundFully(row.get("data").getAsString())));
            return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(props,Items.CARROT_ON_A_STICK,true));
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    public static void register(){ }
    static boolean matches(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||LbcFartus.data(stack).getIntOr("siggewinne_syringe",0)==1);}
    private final GameController game;
    private final Map<UUID,Set<LivingEntity>> holding=new HashMap<>();
    private final Map<LivingEntity,Buff> buffs=new IdentityHashMap<>();
    private int ticks;private boolean guardianDamage;
    private record Buff(UUID owner,ItemStack weapon,int power,int ghast,int creeper,Set<String> previousTags) { }
    LbcSyringe(GameController game){this.game=game;}
    int count(){return buffs.size();}
    boolean buffed(LivingEntity mob){return buffs.containsKey(mob);}
    private boolean active(ServerPlayer p){return game.items().mana().visible(p)&&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level();}
    private boolean held(ServerPlayer p){return matches(p.getMainHandItem())||matches(p.getOffhandItem());}
    private boolean eligible(ServerPlayer p,LivingEntity mob){return !(mob instanceof ServerPlayer)&&mob.getType().builtInRegistryHolder().is(MOBS)&&mob.isAlive()&&!mob.isRemoved()&&mob.level()==p.level()
            &&game.items().wands().validTarget(mob)&&LbcIndirectKills.playerSource(mob)==p;}
    private int power(LivingEntity mob,ItemStack stack){return EnchantmentHelper.getItemEnchantmentLevel(mob.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),stack);}
    private void power(LivingEntity mob,ItemStack stack,int value){if(!stack.isEmpty()&&power(mob,stack)!=value)EnchantmentHelper.updateEnchantments(stack,m->m.set(mob.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),value));}
    private static int triple(int value){return (int)Math.clamp((long)value*3,Integer.MIN_VALUE,Integer.MAX_VALUE);}
    private static void modifier(LivingEntity mob,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,double amount){
        var instance=mob.getAttribute(attr);if(instance!=null){instance.removeModifier(MODIFIER);instance.addPermanentModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    }
    private void begin(ServerPlayer p,LivingEntity mob){
        if(buffs.containsKey(mob))return;
        var weapon=mob.getMainHandItem();int power=power(mob,weapon);
        int ghast=mob instanceof SyringeMobAccess.GhastPower a?a.skywars2$syringePower():0;
        int creeper=mob instanceof SyringeMobAccess.CreeperRadius a?a.skywars2$syringeRadius():0;
        var tags=new HashSet<String>();for(String t:List.of("syringe_buffed","super_blaze","super_guardian"))if(mob.entityTags().contains(t))tags.add(t);
        buffs.put(mob,new Buff(p.getUUID(),weapon,power,ghast,creeper,Set.copyOf(tags)));
        float hp=mob.getHealth();modifier(mob,Attributes.MAX_HEALTH,5);modifier(mob,Attributes.ATTACK_DAMAGE,2);modifier(mob,Attributes.MOVEMENT_SPEED,.25);
        mob.setHealth(hp*3);if(power>0)power(mob,weapon,triple(power));
        if(mob instanceof SyringeMobAccess.GhastPower a)a.skywars2$syringePower(triple(ghast));
        if(mob instanceof SyringeMobAccess.CreeperRadius a)a.skywars2$syringeRadius(triple(creeper));
        mob.entityTags().add(ACTIVE);mob.entityTags().add(SAVED+power+","+ghast+","+creeper+","+(tags.contains("syringe_buffed")?1:0)+","+(tags.contains("super_blaze")?1:0)+","+(tags.contains("super_guardian")?1:0));
        mob.entityTags().add("syringe_buffed");if(mob instanceof Blaze)mob.entityTags().add("super_blaze");if(mob instanceof Guardian)mob.entityTags().add("super_guardian");
    }
    private void attributesOff(LivingEntity mob){float hp=mob.getHealth();
        for(var a:List.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.MOVEMENT_SPEED)){var v=mob.getAttribute(a);if(v!=null)v.removeModifier(MODIFIER);}
        mob.setHealth(hp/3);
    }
    private void tagsOff(LivingEntity mob,Set<String> previous){mob.entityTags().removeIf(t->t.equals(ACTIVE)||t.startsWith(SAVED)
            ||Set.of("syringe_buffed","super_blaze","super_guardian").contains(t)&&!previous.contains(t));}
    private void end(LivingEntity mob){var b=buffs.remove(mob);if(b==null)return;
        attributesOff(mob);if(b.power>0)power(mob,b.weapon,b.power);
        if(mob instanceof SyringeMobAccess.GhastPower a)a.skywars2$syringePower(b.ghast);
        if(mob instanceof SyringeMobAccess.CreeperRadius a)a.skywars2$syringeRadius(b.creeper);
        tagsOff(mob,b.previousTags);var set=holding.get(b.owner);if(set!=null)set.remove(mob);
    }
    void refresh(){
        for(var id:List.copyOf(holding.keySet())){var p=game.server().getPlayerList().getPlayer(id);if(p==null||!active(p)||!held(p))forget(id);}
        for(var p:game.server().getPlayerList().getPlayers())if(active(p)&&held(p)&&!holding.containsKey(p.getUUID())){
            var set=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());holding.put(p.getUUID(),set);p.entityTags().add("holding_syringe");
            game.items().lifetimes().loadedTargets().stream().filter(e->eligible(p,e))
                    .sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(3).forEach(m->{begin(p,m);set.add(m);});
            LbcOverload.sound(p,p.position(),SOUNDS[p.getRandom().nextInt(SOUNDS.length)],1.5f,1,0);
        }
        for(var mob:List.copyOf(buffs.keySet())){var b=buffs.get(mob);var p=game.server().getPlayerList().getPlayer(b.owner);
            if(p==null||!eligible(p,mob)||mob.level().getEntity(mob.getUUID())!=mob){end(mob);continue;}
            if(mob.entityTags().contains("time_stopped")||game.items().timeStop().stopped(p.level()))continue;
            ((net.minecraft.server.level.ServerLevel)mob.level()).sendParticles(ParticleTypes.NOTE,mob.getX(),mob.getY()+1,mob.getZ(),10,.3,.6,.3,0);
            mob.addEffect(ItemEffectLevels.create(MobEffects.GLOWING,20,0),p);
            if(mob instanceof Blaze blaze&&mob.getRandom().nextFloat()<.1f)blaze(blaze);
        }
    }
    public void blaze(Mob mob){var b=buffs.get(mob);if(b==null||!(mob instanceof Blaze))return;
        var p=game.server().getPlayerList().getPlayer(b.owner);if(p==null||!active(p))return;
        var target=game.items().summons().orders().assigned(mob);
        if(target==null)target=game.items().lifetimes().loadedTargets().stream().filter(e->e.level()==mob.level()&&game.items().wands().target(p,e)
                &&!game.items().magic().suppressedAt(p.level(),e.position())).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(target==null)return;var at=mob.getEyePosition();var direction=target.getEyePosition().subtract(at).normalize();
        var shot=new BonusFireball(p,mob,direction);shot.accelerationPower=.05;shot.setPos(at);shot.setDeltaMovement(direction.scale(.7));
        game.items().summons().track(p,shot);p.level().addFreshEntity(shot);
        LbcOverload.sound(p,mob.position(),"minecraft:entity.blaze.shoot",1,1,0);
        p.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xfff235,4),true,false,mob.getX(),mob.getY(),mob.getZ(),3,.1,.1,.1,0);
    }
    /** Keep vanilla launch-unit collision immunity while attribution belongs to the summoner. */
    private static final class BonusFireball extends LargeFireball {
        private final Mob origin;private boolean leftOrigin;
        BonusFireball(ServerPlayer owner,Mob origin,net.minecraft.world.phys.Vec3 direction){super(owner.level(),owner,direction,2);this.origin=origin;}
        @Override public void tick(){if(!leftOrigin)leftOrigin=origin.isRemoved()||origin.getRootVehicle().getSelfAndPassengers()
                .noneMatch(e->e.getBoundingBox().inflate(.3).intersects(getBoundingBox()));super.tick();}
        @Override protected boolean canHitEntity(Entity entity){return (leftOrigin||entity.getRootVehicle()!=origin.getRootVehicle())&&super.canHitEntity(entity);}
    }
    public void damaged(LivingEntity victim,DamageSource source){
        if(guardianDamage||!(victim instanceof ServerPlayer p)||!(source.getDirectEntity() instanceof Guardian guardian)||!buffs.containsKey(guardian)
                ||p.level().dimension().identifier().getPath().equals("nexus")||p.level().dimension().identifier().getPath().equals("imprinted"))return;
        var b=buffs.get(guardian);var owner=game.server().getPlayerList().getPlayer(b.owner);if(owner==null)return;
        guardianDamage=true;try{p.hurtServer(p.level(),new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),guardian,owner),10);}finally{guardianDamage=false;}
    }
    public void converted(Entity from,Entity to){if(!(from instanceof Mob old)||!(to instanceof LivingEntity mob))return;var b=buffs.get(old);if(b==null)return;
        var p=game.server().getPlayerList().getPlayer(b.owner);var type=b.weapon.getItem();end(old);tagsOff(mob,b.previousTags);
        if(b.power>0&&(b.weapon.isEmpty()||mob.getMainHandItem().is(type)))power(mob,mob.getMainHandItem(),b.power);
        if(p!=null&&holding.containsKey(b.owner)){begin(p,mob);holding.get(b.owner).add(mob);}
    }
    void tick(){if(++ticks%5==0)refresh();}
    void loaded(Entity entity){if(!(entity instanceof LivingEntity mob)||buffs.containsKey(mob)||!mob.entityTags().contains(ACTIVE))return;
        var saved=mob.entityTags().stream().filter(t->t.startsWith(SAVED)).findFirst().orElse(null);
        if(saved!=null){var parts=saved.substring(SAVED.length()).split(",");if(parts.length==6){
            attributesOff(mob);power(mob,mob.getMainHandItem(),Integer.parseInt(parts[0]));
            if(mob instanceof SyringeMobAccess.GhastPower a)a.skywars2$syringePower(Integer.parseInt(parts[1]));
            if(mob instanceof SyringeMobAccess.CreeperRadius a)a.skywars2$syringeRadius(Integer.parseInt(parts[2]));
            var previous=new HashSet<String>();for(int i=3;i<6;i++)if(parts[i].equals("1"))previous.add(List.of("syringe_buffed","super_blaze","super_guardian").get(i-3));tagsOff(mob,previous);
        }}
    }
    void unloaded(Entity entity){if(entity instanceof LivingEntity mob)end(mob);}
    public void beforeDeathLoot(LivingEntity entity){end(entity);}
    private void forget(UUID id){var set=holding.remove(id);if(set!=null)for(var mob:List.copyOf(set))end(mob);
        var p=game.server().getPlayerList().getPlayer(id);if(p!=null)p.entityTags().remove("holding_syringe");}
    void forget(ServerPlayer p){forget(p.getUUID());p.entityTags().remove("holding_syringe");}
    void stop(){for(var id:List.copyOf(holding.keySet()))forget(id);for(var m:List.copyOf(buffs.keySet()))end(m);}
}
