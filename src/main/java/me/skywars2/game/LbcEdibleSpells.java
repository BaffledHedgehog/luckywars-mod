package me.skywars2.game;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

/** Native consequences of ordinary eat_* criteria; native consumption owns the only item debit. */
public final class LbcEdibleSpells {
    private static final String TAG="skywars2.edible_spell";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<net.minecraft.world.level.block.Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final String[] FLAGS={"eat_damage","eat_fire","eat_explode","eat_big_explode","eat_chaos_explode",
            "eat_haste","eat_strength","eat_weakness","eat_poison","eat_wither","eat_regeneration","eat_buffs",
            "eat_lightning","eat_psycho","eat_polymorph","eat_teleport","eat_teleport2","eat_slab","eat_book",
            "eat_evoker","eat_cum","gain_bucket","eat_danmaku","yellow_lightings_bottle","eat_glowing","eat_damage_all"};
    private static final String FIREWORK="{FireworksItem:{id:'minecraft:firework_rocket',components:{'minecraft:fireworks':{explosions:[{shape:'small_ball',colors:[16745510],fade_colors:[16767065],has_trail:1b,has_twinkle:1b},{shape:'large_ball',colors:[7405414],fade_colors:[7733201],has_trail:1b,has_twinkle:1b},{shape:'star',colors:[5857279],fade_colors:[10769663],has_trail:1b,has_twinkle:1b},{shape:'creeper',colors:[16728793],fade_colors:[16724828],has_trail:1b,has_twinkle:1b},{shape:'burst',colors:[12069375],fade_colors:[13303741],has_trail:1b,has_twinkle:1b}]}},count:1}}";
    private static final class WildArrow extends DamageSource {
        WildArrow(ServerPlayer p){super(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),p);}
    }
    private record Body(ServerPlayer player,Sheep sheep,GameType previous,String epoch){}
    private final GameController game;
    private final List<Body> bodies=new ArrayList<>();
    private final Set<Entity> nativeChildren=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity,LbcIndirectKills.Owner> authors=new IdentityHashMap<>();
    private final Set<Entity> publishing=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcEdibleSpells(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){if(stack.isEmpty())return false;var d=LbcFartus.data(stack);for(var flag:FLAGS)if(d.getIntOr(flag,0)==1)return true;return false;}
    public static boolean matchesUsing(ItemStack stack){var d=LbcFartus.data(stack);return !stack.isEmpty()&&(d.getIntOr("lava_bucket",0)==1||d.getIntOr("snow_bucket",0)==1);}
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}
    public static boolean wild(Entity entity){return managed(entity)&&(entity instanceof FireworkRocketEntity||entity instanceof EvokerFangs);}
    public static boolean wild(DamageSource source){return source instanceof WildArrow||wild(source.getDirectEntity());}
    public LbcIndirectKills.Owner cause(Entity entity){return authors.get(entity);}
    public Entity owner(Entity entity){var accepted=cause(entity);return accepted==null?null:game.server().getPlayerList().getPlayer(accepted.player());}
    private boolean active(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&LbcDimensionItems.ordinary(p)&&game.canUseGameplayItems(p)&&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()
            &&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)
            &&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(p)
            &&!p.entityTags().contains("time_stopped")&&!game.items().timeStop().stopped(p.level());}
    /** Call once for the completed native consumption, with the original stack/components. */
    public void consumed(ServerPlayer p,ItemStack stack){
        if(!active(p)||!matches(stack))return;
        var d=LbcFartus.data(stack);
        // Independent advancement criteria can coexist on a single authored item.
        for(var flag:FLAGS)if(d.getIntOr(flag,0)==1)switch(flag){
            case "eat_damage"->game.items().indirectKills().withCause(p,game.items().indirectKills().wildCause(p),()->p.hurtServer(p.level(),new WildArrow(p),15));
            case "eat_fire"->fire(p);
            case "eat_explode"->blast(p);
            case "eat_big_explode"->game.items().exoperso().edibleBurst(p,p.position());
            case "eat_chaos_explode"->chaos(p);
            case "eat_haste"->effect(p,MobEffects.HASTE,1200,127,false);
            case "eat_strength"->effect(p,MobEffects.STRENGTH,1200,2,false);
            case "eat_weakness"->effect(p,MobEffects.WEAKNESS,600,9,true);
            case "eat_poison"->effect(p,MobEffects.POISON,200,9,false);
            case "eat_wither"->effect(p,MobEffects.WITHER,600,0,true);
            case "eat_regeneration"->effect(p,MobEffects.REGENERATION,200,10,true);
            case "eat_buffs"->{effect(p,MobEffects.INVISIBILITY,200,1,true);effect(p,MobEffects.SPEED,200,1,true);effect(p,MobEffects.JUMP_BOOST,200,1,true);}
            case "eat_lightning"->{for(int n=0;n<52;n++)lightning(p);}
            case "eat_psycho"->game.items().psycho().afflict(p,p,60,0,0,0);
            case "eat_polymorph"->polymorph(p);
            case "eat_teleport"->teleport(p,1000);
            case "eat_teleport2"->teleport(p,300);
            case "eat_slab"->slab(p);
            case "eat_book"->book(p);
            case "eat_evoker"->{var fangs=new EvokerFangs(p.level(),p.getX(),p.getY(),p.getZ(),0,0,p);insert(p,fangs);}
            case "eat_cum"->cum(p);
            case "gain_bucket"->drop(p,p,"lbc:imba/bucket_rofl");
            case "eat_danmaku"->danmaku(p);
            case "yellow_lightings_bottle"->{game.items().psycho().afflict(p,p,0,0,25000,100000);var mana=game.items().mana();mana.add(p,100000);mana.baseRegen(p,mana.baseRegen(p)+20);mana.maximum(p,mana.maximum(p)+10000);p.entityTags().add("have_lucky_effects");}
            case "eat_glowing"->{effect(p,MobEffects.GLOWING,600,0,false);effect(p,MobEffects.NIGHT_VISION,600,0,false);}
            case "eat_damage_all"->LbcCausticOmega.omega(game,p,InteractionHand.MAIN_HAND,p.getMainHandItem(),false);
            default->throw new IllegalStateException(flag);
        }
    }
    /** Native using_item criteria fire during use, independently of finishing or cancelling. */
    public void using(ServerPlayer p,ItemStack stack){if(!active(p)||!matchesUsing(stack))return;var d=LbcFartus.data(stack);
        if(d.getIntOr("lava_bucket",0)==1)p.hurtServer(p.level(),p.damageSources().lava(),4);
        if(d.getIntOr("snow_bucket",0)==1){game.items().frost().add(p,p);LbcOverload.sound(p,p.position(),"minecraft:block.snow.fall",1,0,0);}
    }
    private void effect(LivingEntity target,Holder<MobEffect> effect,int ticks,int amplifier,boolean hidden){
        target.addEffect(ItemEffectLevels.create(effect,ticks,amplifier,false,!hidden,!hidden),target);
    }
    private void fire(ServerPlayer p){game.items().indirectKills().trapChanges(p.level(),p,true,()->{
        var center=p.blockPosition();for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))
            if(p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).is(AIRS))p.level().setBlock(pos,Blocks.FIRE.defaultBlockState(),3);
    });}
    private void blast(ServerPlayer p){game.items().exasperato().instantBlast(p,p.level(),p.position(),4,game.items().epoch());}
    private void lightning(ServerPlayer p){var bolt=EntityTypes.LIGHTNING_BOLT.create(p.level(),EntitySpawnReason.TRIGGERED);if(bolt!=null){bolt.setCause(p);bolt.setPos(p.position());insert(p,bolt);}}
    private void chaos(ServerPlayer p){
        lightning(p);
        for(var e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),e->e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(p.position())<=100&&game.items().wands().validTarget(e)))
            e.addEffect(ItemEffectLevels.create(MobEffects.WITHER,80,9),p);
        var rocket=new FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET,p.level());load(rocket,FIREWORK);rocket.setOwner(p);rocket.setPos(p.position());insert(p,rocket);blast(p);
    }
    private void cum(ServerPlayer p){for(float pitch:new float[]{0,.2f,.3f,.5f,.8f,1})LbcOverload.sound(p,p.position(),"minecraft:lbcsounds.agggght",.75f,pitch,0);
        if(!game.items().magic().rejectCustomDebuff(p))p.removeAllEffects();for(var e:List.of(MobEffects.POISON,MobEffects.WITHER,MobEffects.NAUSEA,MobEffects.SLOWNESS))effect(p,e,200,0,true);
        effect(p,MobEffects.SATURATION,100,0,true);effect(p,MobEffects.REGENERATION,2000,10,true);effect(p,MobEffects.SPEED,2000,1,true);effect(p,MobEffects.STRENGTH,2000,1,true);
    }
    private void book(ServerPlayer p){LbcOverload.sound(p,p.position(),"minecraft:lbcsounds.agggght",1,1,10);
        for(int n=0;n<10;n++)drop(p,p,"lbc:transformables_spells");drop(p,p,"lbc:transformables_staff_all");
        if(!game.items().magic().rejectCustomDebuff(p))p.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()-40);
        for(var e:List.of(MobEffects.NAUSEA,MobEffects.BLINDNESS,MobEffects.DARKNESS))effect(p,e,20,0,true);
    }
    private void danmaku(ServerPlayer p){var at=p.getEyePosition().add(0,-.5,0);
        for(int n=1;n<=360;n++){game.items().danmaku().card(p,at,Vec3.directionFromRotation(p.getXRot()-10,p.getYRot()+n),true);LbcOverload.sound(p,at,"minecraft:ui.cartography_table.take_result",.5f,2,0);}
        for(int n=1;n<=360;n++){game.items().danmaku().card(p,at,Vec3.directionFromRotation(p.getXRot(),p.getYRot()+n),false);LbcOverload.sound(p,at,"minecraft:ui.cartography_table.take_result",.5f,2,0);}
    }
    private void teleport(ServerPlayer p,int radius){if(game.items().magic().rejectCustomDebuff(p))return;var board=game.server().getScoreboard();var objective=board.getObjective("swrg.math");var score=objective==null?null:board.getPlayerScoreInfo(ScoreHolder.forNameOnly("no_borders"),objective);
        boolean bounded=score!=null&&score.value()==0;execute(p,"spreadplayers "+(bounded?"0 0 0 190":p.getX()+" "+p.getZ()+" 0 "+radius)+" true @s");
    }
    private void polymorph(ServerPlayer p){if(game.items().magic().rejectCustomDebuff(p))return;var sheep=EntityTypes.SHEEP.create(p.level(),EntitySpawnReason.TRIGGERED);if(sheep==null)return;
        load(sheep,"{DeathLootTable:'minecraft:empty',CustomName:'jeb_',CustomNameVisible:0b,attributes:[{id:'minecraft:knockback_resistance',base:10}]}");sheep.setCustomName(Component.literal("jeb_"));sheep.setCustomNameVisible(false);sheep.entityTags().add("magicalsheep");sheep.setPos(p.position());
        if(!insert(p,sheep))return;var body=new Body(p,sheep,p.gameMode(),game.items().epoch());bodies.add(body);
        p.level().sendParticles(ParticleTypes.POOF,p.getX(),p.getY()+.5,p.getZ(),20,.1,.1,.1,.5);LbcOverload.sound(p,p.position(),"minecraft:block.fire.extinguish",1,2,0);
        p.setGameMode(GameType.SPECTATOR);p.setCamera(sheep);
    }
    /** Invoked only by the native fang's actual attack tick, before vanilla's owner/allies shortcut. */
    public void fangHit(EvokerFangs fang,LivingEntity victim){
        if(!(owner(fang) instanceof ServerPlayer p)||!game.items().currentEpoch(fang)||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||p.level()!=fang.level()||victim.level()!=fang.level()||!game.items().wands().validTarget(victim)||victim.isInvulnerable())return;
        var source=p.damageSources().indirectMagic(fang,p);
        game.items().indirectKills().withCause(victim,cause(fang),()->{
            if(victim.hurtServer(p.level(),source,6))net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(p.level(),victim,source);
        });
    }
    private boolean insert(ServerPlayer p,Entity entity){if(!(entity instanceof ItemEntity))entity.entityTags().add(TAG);game.items().markCurrentItemEpoch(entity);authors.put(entity,game.items().indirectKills().wildCause(p));nativeChildren.add(entity);publishing.add(entity);
        try{if(p.level().addFreshEntity(entity)&&!entity.isRemoved()){game.items().timeStop().prepareFrozen(entity);return true;}nativeChildren.remove(entity);authors.remove(entity);return false;}finally{publishing.remove(entity);}
    }
    private void drop(ServerPlayer author,Entity target,String table){for(var stack:game.lbc().rollTable((net.minecraft.server.level.ServerLevel)target.level(),target.position(),target,table)){
        var item=new ItemEntity(target.level(),target.getX(),target.getY(),target.getZ(),stack);item.setDefaultPickUpDelay();item.setThrower(author);insert(author,item);
    }}
    private ItemStack rollSlab(Entity e){var out=game.lbc().rollTable((net.minecraft.server.level.ServerLevel)e.level(),e.position(),e,"lbc:random_eatable_slab_random_count");return out.isEmpty()?ItemStack.EMPTY:out.getFirst();}
    private void slab(ServerPlayer author){
        var entities=new ArrayList<Entity>();author.level().getAllEntities().forEach(entities::add);
        for(var p:game.alivePlayers())if(p.level()==author.level()&&game.items().wands().validTarget(p)){
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND))p.setItemSlot(slot,rollSlab(p));
            p.containerMenu.setCarried(rollSlab(p));var craft=p.inventoryMenu.getCraftSlots();for(int n=0;n<craft.getContainerSize();n++)craft.setItem(n,rollSlab(p));
            for(int n=0;n<36;n++)p.getInventory().setItem(n,rollSlab(p));p.getInventory().setChanged();p.containerMenu.broadcastChanges();LbcOverload.sound(p,p.position(),"minecraft:lbcsounds.shitting_1",1,1,0);
        }
        for(var e:entities)if(!(e instanceof ServerPlayer)&&!e.isRemoved()&&game.items().wands().validTarget(e)){
            if(e instanceof ItemEntity item)item.setItem(item.getItem().transmuteCopy(Items.BRICK_SLAB));
            else if(e.getType().builtInRegistryHolder().is(MOBS)&&e instanceof LivingEntity living){
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))living.setItemSlot(slot,rollSlab(e));
                execute(e,"data modify entity @s DeathLootTable set value 'lbc:random_eatable_slab_random_count_64'");LbcOverload.sound(author,e.position(),"minecraft:lbcsounds.shitting_1",1,1,0);
            }else{drop(author,e,"lbc:random_eatable_slab_random_count");if(e instanceof Marker)execute(e,"loot insert ~ ~ ~ loot lbc:random_eatable_slab_random_count_27");e.kill((net.minecraft.server.level.ServerLevel)e.level());}
        }
    }
    private static void load(Entity entity,String source){try{entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),TagParser.parseCompoundFully(source)));}catch(Exception e){throw new IllegalStateException("Invalid frozen edible source",e);}}
    private int execute(Entity actor,String command){var source=game.server().createCommandSourceStack().withSource(CommandSource.NULL).withLevel((net.minecraft.server.level.ServerLevel)actor.level()).withEntity(actor).withPosition(actor.position()).withRotation(actor.getRotationVector()).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
        try{return game.server().getCommands().getDispatcher().execute(command,source);}catch(CommandSyntaxException expectedSourceFailure){return 0;}
    }
    /** Native game tick only maintains accepted polymorphs and native child ownership. */
    public void tick(){
        for(var body:List.copyOf(bodies))if(LbcParasite.active(body.player)||!game.items().currentEpoch(body.epoch)||!game.playing()||game.stage()==GameController.Stage.ENDING||body.player.isRemoved())release(body);
        for(var e:List.copyOf(nativeChildren))if(e.isRemoved()||!game.items().currentEpoch(e)||!game.playing()||game.stage()==GameController.Stage.ENDING){nativeChildren.remove(e);authors.remove(e);if(!e.isRemoved())e.discard();}
    }
    public boolean polymorphed(ServerPlayer player){return bodies.stream().anyMatch(body->body.player==player);}
    public void clearDebuffs(ServerPlayer player){for(var body:List.copyOf(bodies))if(body.player==player)release(body);}
    private void release(Body body){bodies.remove(body);if(body.player.getCamera()==body.sheep){body.player.setCamera(body.player);body.player.setGameMode(body.previous);}nativeChildren.remove(body.sheep);authors.remove(body.sheep);if(!body.sheep.isRemoved())body.sheep.discard();}
    public void loaded(Entity entity){if(managed(entity)&&!publishing.contains(entity)&&!nativeChildren.contains(entity))entity.discard();}
    public void unloaded(Entity entity){nativeChildren.remove(entity);authors.remove(entity);}
    public void forget(ServerPlayer p){for(var body:List.copyOf(bodies))if(body.player==p)release(body);for(var e:List.copyOf(nativeChildren))if(authors.get(e)!=null&&p.getUUID().equals(authors.get(e).player())){nativeChildren.remove(e);authors.remove(e);e.discard();}}
    public void stop(){for(var body:List.copyOf(bodies))release(body);for(var e:List.copyOf(nativeChildren))e.discard();nativeChildren.clear();authors.clear();}
    public int polymorphCount(){return bodies.size();}
    public List<Entity> snapshot(){return List.copyOf(nativeChildren);}
}
