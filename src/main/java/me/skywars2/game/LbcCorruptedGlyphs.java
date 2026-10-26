package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** The four source dust leaves share a native placement item and match-owned display lifecycle. */
public final class LbcCorruptedGlyphs {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<EntityType<?>> UNHOLY=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:unholy_monsters"));
    private static final ResourceKey<Item> KEY=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:corrupted_dust"));
    public static final Item DUST=Registry.register(BuiltInRegistries.ITEM,KEY,new DustItem(new Item.Properties().setId(KEY)));
    private final GameController game;
    private final List<Glyph> glyphs=new ArrayList<>();
    private final Map<LivingEntity,Map<java.util.UUID,LbcIndirectKills.Owner>> belialDuels=new WeakHashMap<>();
    private final Map<net.minecraft.world.entity.Mob,LbcGlyphAi> ai=new java.util.HashMap<>();
    private final Map<net.minecraft.world.entity.Mob,Duel> fights=new java.util.HashMap<>();
    private record Duel(Glyph glyph, LivingEntity opponent){}
    public LbcCorruptedGlyphs(GameController game){this.game=game;}
    public static void register(){}
    public enum Kind {
        BAFOMET("corrupted_pentagram",0x5a2020), LAMASHTU("lamashtu",0x180c0c), BELIAL("belial",0xdd0000), AHRIMAN("ahriman",0x8723af);
        final String model;final int color;
        Kind(String model,int color){this.model=model;this.color=color;}
    }
    static Kind kind(net.minecraft.world.item.ItemStack stack){
        if(!stack.is(DUST))return null;
        var name=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("glyphkind","");
        for(var kind:Kind.values())if(kind.name().equalsIgnoreCase(name))return kind;
        return null;
    }
    private static final class DustItem extends SimplePolymerItem {
        DustItem(Item.Properties properties){super(properties,Items.PANDA_SPAWN_EGG,true);}
        @Override public InteractionResult useOn(UseOnContext context){
            var game=SkyWars2.game();
            return game==null?InteractionResult.FAIL:game.items().corruptedGlyphs().useOn(context);
        }
    }
    public InteractionResult useOn(UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer owner))return InteractionResult.FAIL;
        var kind=kind(context.getItemInHand());
        if(kind==null||!game.canUseGameplayItems(owner)||owner.level()!=game.maps().level()
                ||!game.playing()||game.stage()==GameController.Stage.ENDING||game.items().actionsBlocked(owner)
                ||game.items().wands().inputTimeStopped(owner)||game.items().magic().suppressed(owner))return InteractionResult.FAIL;
        var clicked=context.getClickedPos();
        var pos=owner.level().getBlockState(clicked).getCollisionShape(owner.level(),clicked).isEmpty()?clicked:clicked.relative(context.getClickedFace());
        var at=Vec3.atBottomCenterOf(pos);
        if(!owner.level().hasChunkAt(pos)||game.items().magic().suppressedAt(owner.level(),at))return InteractionResult.FAIL;
        if(spawn(owner,at,kind)==null)return InteractionResult.FAIL;
        context.getItemInHand().consume(1,owner);
        return InteractionResult.SUCCESS;
    }
    static final class Glyph {
        final ServerPlayer owner;final Display.ItemDisplay display;final Kind kind;final String epoch;
        int ticks;
        Glyph(ServerPlayer owner,Display.ItemDisplay display,Kind kind,String epoch){this.owner=owner;this.display=display;this.kind=kind;this.epoch=epoch;}
    }
    Glyph spawn(ServerPlayer owner,Vec3 at,Kind kind){
        var display=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,owner.level());
        try{
            // Initialize the entire pose before addFreshEntity sends the first tracking packet.
            var preset=TagParser.parseCompoundFully("{interpolation_duration:20,teleport_duration:20,shadow_radius:0f,shadow_strength:0f,item_display:'fixed',transformation:[2f,0f,0f,0f,0f,0f,-1f,0f,0f,2f,0f,0f,0f,0f,0f,1f],item:{id:'minecraft:panda_spawn_egg',count:1,components:{'minecraft:item_model':'lbc:"+kind.model+"'}}}");
            display.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),preset));
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new IllegalStateException(ex);}
        display.snapTo(at.x,at.y,at.z,0,0);
        display.entityTags().addAll(List.of("skywars2.corrupted_glyph","corrupted_dust",kind.name().toLowerCase(java.util.Locale.ROOT),"magic"));
        game.items().markCurrentItemEpoch(display);game.items().timeStop().prepareFrozen(display);
        var glyph=new Glyph(owner,display,kind,game.items().epoch());glyphs.add(glyph);
        // Entity-load callbacks run synchronously during publication and must already see ownership.
        if(!owner.level().addFreshEntity(display)||display.isRemoved()||!glyphs.contains(glyph)){remove(glyph);return null;}
        game.items().mana().award(owner,"lbc:true_advancements/staffs/corruption");
        return glyph;
    }
    public int count(){return glyphs.size();}
    /** Direct purification removes fields and AI leases without their terminal curses. */
    int dispelAt(net.minecraft.server.level.ServerLevel level,Vec3 at,double radius){
        int removed=0;
        for(var glyph:List.copyOf(glyphs))if(glyph.display.level()==level&&glyph.display.position().distanceToSqr(at)<=radius*radius){remove(glyph);removed++;}
        return removed;
    }
    public void loaded(net.minecraft.world.entity.Entity entity){
        if(entity.entityTags().contains("skywars2.corrupted_glyph")&&glyphs.stream().noneMatch(glyph->glyph.display==entity))entity.discard();
    }
    public void unloaded(net.minecraft.world.entity.Entity entity){for(var glyph:List.copyOf(glyphs))if(glyph.display==entity)remove(glyph);if(entity instanceof net.minecraft.world.entity.Mob mob)release(mob);if(entity instanceof LivingEntity living){removeFights(living);belialDuels.remove(living);for(var duels:belialDuels.values())duels.remove(living.getUUID());}}
    List<Glyph> snapshot(){return List.copyOf(glyphs);}
    public void forget(ServerPlayer owner){for(var glyph:List.copyOf(glyphs))if(glyph.owner==owner)remove(glyph);for(var duels:belialDuels.values())duels.values().removeIf(cause->cause.player().equals(owner.getUUID()));}
    public void stop(){for(var glyph:List.copyOf(glyphs))remove(glyph);for(var mob:List.copyOf(ai.keySet()))release(mob);fights.clear();belialDuels.clear();}
    private void remove(Glyph glyph){glyphs.remove(glyph);glyph.display.discard();for(var mob:List.copyOf(ai.keySet()))if(ai.get(mob).glyph==glyph)release(mob);fights.values().removeIf(duel->duel.glyph==glyph);}
    public void tick(){
        for(var entity:List.copyOf(belialDuels.keySet()))if(entity.isRemoved()||!entity.isAlive()){
            belialDuels.remove(entity);for(var duels:belialDuels.values())duels.remove(entity.getUUID());
        }
        belialDuels.values().removeIf(Map::isEmpty);
        // Intent holds strong references only while both native actors and their field remain live.
        for(var entry:List.copyOf(fights.entrySet()))if(!eligible(entry.getKey())||!eligibleOpponent(entry.getValue().opponent)
                ||!active(entry.getValue().glyph))removeFights(entry.getKey());
        for(var mob:List.copyOf(ai.keySet()))if(control(mob)==null)release(mob);
        for(var glyph:List.copyOf(glyphs)){
            var display=glyph.display;var owner=glyph.owner;
            if(display.isRemoved()||!game.playing()||game.stage()==GameController.Stage.ENDING
                    ||!game.items().currentEpoch(glyph.epoch)||!game.canUseGameplayItems(owner)
                    ||owner.level()!=display.level()||display.level()!=game.maps().level()){remove(glyph);continue;}
            if(!owner.level().hasChunkAt(display.blockPosition())){remove(glyph);continue;}
            if(game.items().magic().suppressedAt(owner.level(),display.position())){remove(glyph);continue;}
            if(game.items().wands().timeStopped())continue;
            if(++glyph.ticks%20==0)pulse(glyph);
        }
    }
    private List<LivingEntity> targets(Glyph glyph,double radius){
        var at=glyph.display.position();
        return glyph.owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),target->
                target.position().distanceToSqr(at)<=radius*radius&&target.getType().builtInRegistryHolder().is(MOBS)
                        &&game.items().wands().validTarget(target));
    }
    private boolean affect(Glyph glyph,LivingEntity target){return game.items().wands().projectileTarget(glyph.owner,target);}
    void pulse(Glyph glyph){
        if(!glyphs.contains(glyph))return;
        var at=glyph.display.position();var level=glyph.owner.level();
        var targets=targets(glyph,16);
        if(glyph.kind==Kind.BAFOMET){
            // Movement is leased in Mob.serverAiStep, where competing Goal/Brain writes are suspended.
        }else if(glyph.kind==Kind.LAMASHTU){
            for(var target:targets)if(affect(glyph,target)&&!target.getType().builtInRegistryHolder().is(UNHOLY))
                target.addEffect(ItemEffectLevels.create(MobEffects.WITHER,-1,0),glyph.owner);
        }else if(glyph.kind==Kind.BELIAL){
            for(var target:targets)if(!(target instanceof ServerPlayer)){
                leaveTeam(target);
                if(level.getRandom().nextInt(20)==0){
                    // Source execute-as changes the victim but retains the glyph's selector origin.
                    var candidates=targets(glyph,30).stream().filter(attacker->attacker!=target&&attacker!=glyph.owner).toList();
                    if(!candidates.isEmpty())belialAttack(glyph,target,candidates.get(level.getRandom().nextInt(candidates.size())));
                }
            }
            if(game.config().gameMode!=3&&game.config().gameMode!=4)
                for(var target:targets)if(target instanceof ServerPlayer&&affect(glyph,target))leaveTeam(target);
        }else if(glyph.kind==Kind.AHRIMAN){
            for(int i=0;i<6;i++)ahrimanCube(glyph,at.add(level.getRandom().nextInt(51)-25,level.getRandom().nextInt(51)-25,level.getRandom().nextInt(51)-25));
        }
        boolean contact=targets.stream().anyMatch(target->target.position().distanceToSqr(at)<=1
                &&(glyph.kind!=Kind.BAFOMET||!target.getType().builtInRegistryHolder().is(UNHOLY)));
        if(contact&&level.getRandom().nextInt(5)==0||level.getBlockState(BlockPos.containing(at.add(0,-.5,0))).isAir()
                ||!level.getBlockState(BlockPos.containing(at.add(0,.5,0))).isAir())explode(glyph);
        if(!glyph.display.isRemoved())glyph.display.setYRot(glyph.display.getYRot()+10);
    }
    void explode(Glyph glyph){
        if(!glyphs.contains(glyph))return;
        var at=glyph.display.position();
        if(glyph.kind==Kind.BAFOMET){
            for(var target:targets(glyph,1))if(!target.getType().builtInRegistryHolder().is(UNHOLY))
                target.addEffect(ItemEffectLevels.create(MobEffects.WITHER,100,4),glyph.owner);
        }else if(glyph.kind==Kind.LAMASHTU){
            for(var target:targets(glyph,16))target.removeEffect(MobEffects.WITHER);
        }else if(glyph.kind==Kind.AHRIMAN){
            for(var target:targets(glyph,16))randomAttributesDown(glyph,target);
        }
        LbcOverload.sound(glyph.owner,at,"minecraft:entity.cat.purr",1,0,0);
        glyph.owner.level().sendParticles(new DustParticleOptions(glyph.kind.color,1),at.x,at.y+.2,at.z,20,1.5,0,1.5,0);
        remove(glyph);
    }
    private void leaveTeam(LivingEntity target){
        var team=target.getTeam();if(team!=null)game.server().getScoreboard().removePlayerFromTeam(target.getScoreboardName(),team);
    }
    /** Starts a real two-way AI duel; admission never damages either mob. */
    boolean belialAttack(Glyph glyph,LivingEntity target,LivingEntity attacker){
        if(!glyphs.contains(glyph)||glyph.kind!=Kind.BELIAL||!(target instanceof net.minecraft.world.entity.Mob first)
                ||!(attacker instanceof net.minecraft.world.entity.Mob second)||first==second
                ||!eligible(first)||!eligible(second)||!first.canAttack(second)||!second.canAttack(first))return false;
        var cause=game.items().indirectKills().wildCause(glyph.owner);
        belialDuels.computeIfAbsent(target,key->new java.util.HashMap<>()).put(attacker.getUUID(),cause);
        belialDuels.computeIfAbsent(attacker,key->new java.util.HashMap<>()).put(target.getUUID(),cause);
        fights.put(first,new Duel(glyph,second));fights.put(second,new Duel(glyph,first));
        acquire(first,glyph).target(second);acquire(second,glyph).target(first);
        return true;
    }
    /** Pair-scoped attribution is installed before the first native AI melee swing. */
    LbcIndirectKills.Owner belialCause(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof LivingEntity attacker)||!game.items().wands().validTarget(victim,true)
                ||attacker instanceof ServerPlayer||!game.items().wands().validTarget(attacker))return null;
        var duels=belialDuels.get(victim);return duels==null?null:duels.get(attacker.getUUID());
    }
    private boolean eligible(net.minecraft.world.entity.Mob mob){
        return mob.isAlive()&&!mob.isRemoved()&&!mob.isNoAi()&&mob.getType().builtInRegistryHolder().is(MOBS)
                &&game.items().wands().validTarget(mob)&&!LbcGreed.active(mob)
                &&mob.level() instanceof net.minecraft.server.level.ServerLevel level&&!game.items().magic().suppressedAt(level,mob.position());
    }
    private Glyph control(net.minecraft.world.entity.Mob mob){
        if(!eligible(mob)||game.items().wands().timeStopped())return null;
        for(var glyph:glyphs)if(glyph.kind==Kind.BAFOMET&&active(glyph)&&mob.level()==glyph.display.level()
                &&mob.position().distanceToSqr(glyph.display.position())<=256)return glyph;
        var duel=fights.get(mob);
        return duel!=null&&active(duel.glyph)&&eligibleOpponent(duel.opponent)&&duel.opponent.level()==mob.level()&&mob.canAttack(duel.opponent)&&mob.level()==duel.glyph.display.level()
                &&mob.position().distanceToSqr(duel.glyph.display.position())<=900?duel.glyph:null;
    }
    private boolean eligibleOpponent(LivingEntity entity){return entity instanceof net.minecraft.world.entity.Mob mob&&eligible(mob);}
    private boolean active(Glyph glyph){return glyphs.contains(glyph)&&!glyph.display.isRemoved()&&game.playing()
            &&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(glyph.epoch)
            &&game.canUseGameplayItems(glyph.owner)&&glyph.owner.level()==glyph.display.level()
            &&glyph.display.level()==game.maps().level()&&!game.items().magic().suppressedAt(glyph.owner.level(),glyph.display.position());}
    private LbcGlyphAi acquire(net.minecraft.world.entity.Mob mob,Glyph glyph){
        var state=ai.get(mob);if(state==null){state=new LbcGlyphAi(mob,glyph);ai.put(mob,state);}else state.glyph=glyph;return state;
    }
    int fightCount(){return fights.size();}
    private void removeFights(LivingEntity entity){
        for(var entry:List.copyOf(fights.entrySet()))if(entry.getKey()==entity||entry.getValue().opponent==entity){
            var mob=entry.getKey();fights.remove(mob);
            var state=ai.get(mob);if(state!=null&&state.glyph.kind==Kind.BELIAL)release(mob);
        }
    }
    private void release(net.minecraft.world.entity.Mob mob){var state=ai.remove(mob);if(state!=null)state.restore();}
    /** Called only from the native mob AI boundary. True consumes that AI step. */
    public boolean aiStep(net.minecraft.world.entity.Mob mob){
        var glyph=control(mob);if(glyph==null){release(mob);return false;}
        var state=acquire(mob,glyph);
        if(glyph.kind==Kind.BAFOMET){
            state.field(glyph.display.position(),mob.getType().builtInRegistryHolder().is(UNHOLY));
            game.items().indirectKills().displaced(mob,glyph.owner,true);
        }else state.combat(fights.get(mob).opponent);
        mob.getSensing().tick();mob.getNavigation().tick();mob.getMoveControl().tick();mob.getLookControl().tick();mob.getJumpControl().tick();
        return true;
    }
    /** Native outside_border identity, with an explicit authored WILD marker for friendship policy. */
    static class WildSource extends DamageSource {
        WildSource(ServerPlayer owner){super(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.OUTSIDE_BORDER),owner);}
    }
    boolean ahrimanCube(Glyph glyph,Vec3 center){
        if(center.distanceToSqr(glyph.display.position())<=6.25)return false;
        var level=glyph.owner.level();var pos=BlockPos.containing(center);
        for(int x=(pos.getX()-1)>>4;x<=((pos.getX()+1)>>4);x++)for(int z=(pos.getZ()-1)>>4;z<=((pos.getZ()+1)>>4);z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        game.items().indirectKills().worldChanges(level,glyph.owner,true,()->{
            for(var block:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))
                if(!level.isOutsideBuildHeight(block)){
                    level.destroyBlock(block,true,glyph.owner);
                    // fill air destroy also replaces the fluid left behind by native destroyBlock.
                    level.setBlock(block,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                }
        });
        level.sendParticles(new DustParticleOptions(0,4),center.x,center.y,center.z,4,1,1,1,0);
        var damage=new WildSource(glyph.owner);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(2),target->
                target.position().distanceToSqr(center)<=4&&target.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validTarget(target)))
            target.hurtServer(level,damage,4);
        return true;
    }
    static final List<Holder<Attribute>> DOWN_ATTRIBUTES=List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE,Attributes.LUCK,Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.MOVEMENT_SPEED);
    private static final int[] DOWN_SCALE={1,1,10,1,1,1,5,100000},DOWN_DEBIT={1,1,10,1,2,1,1,2000};
    private void randomAttributesDown(Glyph glyph,LivingEntity target){
        for(int i=0;i<DOWN_ATTRIBUTES.size();i++)if(glyph.owner.level().getRandom().nextBoolean()){
            var attribute=target.getAttribute(DOWN_ATTRIBUTES.get(i));
            if(attribute!=null)attribute.setBaseValue((Math.floor(attribute.getBaseValue()*DOWN_SCALE[i])-DOWN_DEBIT[i])/(double)DOWN_SCALE[i]);
        }
    }
}
