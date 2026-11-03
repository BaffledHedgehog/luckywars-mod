package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Kaname's source behavior on the exact native arrow, independent of its ammo/components. */
public final class LbcMadokaBow {
    private static final String MARK="madoka_arrow",JOB="skywars2.madoka.restore",AUTHOR="skywars2.madoka.author:",ORDER="skywars2.madoka.order:";
    private final GameController game;
    private final Set<AbstractArrow> arrows=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Marker> restoreJobs=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcMadokaBow(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"madoka_bow",0)==1;}
    public static boolean recognizes(Entity entity){return entity instanceof AbstractArrow&&entity.entityTags().contains(MARK);}
    public static boolean restoreJob(Entity entity){return entity instanceof Marker&&entity.entityTags().contains(JOB);}
    public void augment(ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        if(!matches(weapon)||game.items().magic().suppressed(owner))return;
        var cause=game.items().indirectKills().coldCause(owner,true);
        for(var projectile:family)if(projectile instanceof AbstractArrow arrow&&!arrow.isRemoved()
                &&arrow.getOwner()==owner&&arrow.level()==owner.level()){
            arrow.entityTags().add(MARK);arrow.entityTags().add(AUTHOR+cause.player());arrow.entityTags().add(ORDER+cause.order());
            game.items().markCurrentItemEpoch(arrow);arrows.add(arrow);
        }
    }
    /** Rebind only the live carrier; a previously accepted terrain job retains its own author. */
    public void ownerChanged(Entity actor,ServerPlayer owner){
        if(!recognizes(actor))return;
        var cause=game.items().indirectKills().coldCause(owner,true);
        actor.entityTags().removeIf(tag->tag.startsWith(AUTHOR)||tag.startsWith(ORDER));
        actor.entityTags().add(AUTHOR+cause.player());actor.entityTags().add(ORDER+cause.order());
    }
    private boolean current(Entity arrow){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&game.items().currentEpoch(arrow)&&arrow.level()==game.maps().level()&&LbcDimensionItems.ordinary(arrow.level());}
    private boolean paused(Entity arrow){return !game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().stopped((ServerLevel)arrow.level())||game.items().timeStop().frozen(arrow)
            ||arrow.entityTags().contains("time_stopped");}
    /** Tick HEAD; native movement pauses alongside the effect while frozen. */
    public boolean beforeProjectile(Projectile projectile){
        if(!recognizes(projectile))return true;
        var arrow=(AbstractArrow)projectile;
        if(arrow.isRemoved())return false;
        if(!current(arrow)){arrows.remove(arrow);arrow.discard();return false;}
        if(paused(arrow))return false;
        var level=(ServerLevel)arrow.level();var at=arrow.position();
        if(level.getRandom().nextFloat()<.2f)level.sendParticles(ParticleTypes.HEART,at.x,at.y,at.z,1,0,0,0,0);
        if(((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)arrow).skywars2$isInGround()){
            requestRestore(arrow,at);return false;
        }
        return true;
    }
    /** Accept before other block-contact traits retire their shared native carrier. */
    public boolean blockHit(Projectile arrow,BlockHitResult hit){
        return recognizes(arrow)&&current(arrow)&&!paused(arrow)&&requestRestore((AbstractArrow)arrow,hit.getLocation());
    }
    private boolean requestRestore(AbstractArrow arrow,Vec3 at){
        if(!recognizes(arrow)||arrow.isRemoved())return false;
        var job=new Marker(EntityTypes.MARKER,(ServerLevel)arrow.level());job.setPos(at);
        job.entityTags().add(JOB);
        // MapRotation transports every native actor, including strict markers. This tag also
        // keeps queued terrain authority out of nearby clone-skill families.
        job.entityTags().add("strict_map_object");
        for(var tag:arrow.entityTags())if(tag.startsWith(AUTHOR)||tag.startsWith(ORDER))job.entityTags().add(tag);
        game.items().markCurrentItemEpoch(job);
        arrow.entityTags().remove(MARK);arrows.remove(arrow);
        if(!arrow.level().addFreshEntity(job)){
            arrow.entityTags().add(MARK);arrows.add(arrow);return false;
        }
        restoreJobs.add(job);arrow.discard();return true;
    }
    private static String value(Entity actor,String prefix){
        for(var tag:actor.entityTags())if(tag.startsWith(prefix))return tag.substring(prefix.length());return null;
    }
    private LbcIndirectKills.Owner cause(Entity actor){try{
        return new LbcIndirectKills.Owner(UUID.fromString(value(actor,AUTHOR)),Long.parseLong(value(actor,ORDER)),false,true);
    }catch(RuntimeException invalid){return null;}}
    private void restore(Marker job){
        if(job.isRemoved()||!current(job)){restoreJobs.remove(job);if(!job.isRemoved())job.discard();return;}
        if(paused(job))return;
        var owner=cause(job);if(owner==null){restoreJobs.remove(job);job.discard();return;}
        boolean[] restored={false};var level=(ServerLevel)job.level();var at=job.position();
        game.items().indirectKills().trapChanges(level,owner,()->restored[0]=game.maps().restoreMadokaRegion(level,at));
        if(!restored[0])return; // Busy/unresident clips remain native checkpointed jobs, without forcing chunks.
        level.sendParticles(ParticleTypes.HEART,at.x,at.y,at.z,100,6,6,6,0);
        LbcOverload.sound(level,at,"minecraft:entity.player.levelup",1,2,0,0);
        restoreJobs.remove(job);job.discard();
    }
    /** Invoke only after vanilla accepted an entity hit (doPostHurtEffects), before curse mutation.
     * Native arrow damage, potion delivery, spectral glow, piercing and pickup remain vanilla. */
    public void beamContact(ServerPlayer owner,ItemStack weapon,net.minecraft.world.entity.projectile.arrow.Arrow context,LivingEntity victim){
        if(!matches(weapon))return;
        var cause=game.items().indirectKills().coldCause(owner,true);
        context.entityTags().add(MARK);context.entityTags().add(AUTHOR+cause.player());context.entityTags().add(ORDER+cause.order());
        game.items().markCurrentItemEpoch(context);entityHit(context,victim);
    }
    public void entityHit(Projectile arrow,LivingEntity victim){
        if(!recognizes(arrow)||!current(arrow)||paused(arrow))return;
        if(game.items().magic().suppressedAt((ServerLevel)victim.level(),victim.position())
                ||victim instanceof ServerPlayer guarded&&game.items().inventoryHazards().protectedInventory(guarded))return;
        if(!(victim instanceof ServerPlayer player)){
            victim.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,curse(victim,victim.getOffhandItem()));return;
        }
        // The advancement's Inventory NBT gate intentionally accepts ANY upgraded item.
        boolean reward=false;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)
            if(ItemData.intValue(player.getInventory().getItem(slot),"upgraded",0)==1){reward=true;break;}
        var author=author(arrow);
        if(reward&&author!=null){
            var mana=game.items().mana();mana.set(author,mana.get(author)-10000);
            author.level().sendParticles(ParticleTypes.WITCH,author.getX(),author.getY()+1,author.getZ(),150,.5,1,.5,.5);
            mana.award(author,"lbc:true_advancements/bows/madoka_bow");
        }
        // The author explicitly protects the target with MAG and the inventory-protection skill.
        for(int slot=0;slot<Math.min(36,player.getInventory().getContainerSize());slot++)
            player.getInventory().setItem(slot,curse(player,player.getInventory().getItem(slot)));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,curse(player,player.getOffhandItem()));
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
    }
    /** Alternate accepted-damage hook for integrations which cannot call doPostHurtEffects.
     * Use exactly one of damaged/entityHit for each successful native contact. */
    public void damaged(ServerPlayer victim,DamageSource damage){
        if(damage.getDirectEntity() instanceof Projectile projectile)entityHit(projectile,victim);
    }
    private ItemStack curse(LivingEntity victim,ItemStack stack){
        if(stack.isEmpty()||ItemData.intValue(stack,"upgraded",0)!=1)return stack;
        var base=base(stack);
        // Imported native Academy carriers need not retain legacy magicwand custom_data.
        // The authoritative reverse mapping establishes their identity without broadening
        // this curse to arbitrary upgraded inventory items (the advancement has a wider gate).
        if(base==stack.getItem()&&!stack.is(Items.CARROT_ON_A_STICK)
                &&ItemData.intValue(stack,"magicwand",0)!=1)return stack;
        var data=LbcFartus.data(stack);
        if(base!=stack.getItem()){
            var original=stack;stack=original.transmuteCopy(base);
            // transmuteCopy carries only patches; preserve resolved original defaults/removals too.
            for(var type:List.copyOf(stack.getComponents().keySet()))if(!original.has(type))stack.remove(type);
            stack.applyComponents(original.getComponents());
        }
        var changed=data.copy();changed.remove("upgraded");changed.remove("upgradable");changed.remove("upgrade_available");
        changed.putInt("pissed_by_madoka_sob_sob_sob_sob_sob",1);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(changed));
        var lore=new ArrayList<>(stack.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines());
        // NBT [-1] only replaces an existing last row; it does not invent lore on empty stacks.
        if(!lore.isEmpty()){lore.set(lore.size()-1,Component.translatable("madoka_cursed").withStyle(ChatFormatting.RED));stack.set(DataComponents.LORE,new ItemLore(lore));}
        if(victim instanceof ServerPlayer player)player.sendSystemMessage(Component.translatable("lost_upgrade"));
        return stack;
    }
    private static net.minecraft.world.item.Item base(ItemStack stack){return LbcAcademy.baseWandItem(stack.getItem());}
    private ServerPlayer author(Entity arrow){
        for(var tag:arrow.entityTags())if(tag.startsWith(AUTHOR))try{
            return game.server().getPlayerList().getPlayer(UUID.fromString(tag.substring(AUTHOR.length())));
        }catch(IllegalArgumentException invalid){return null;}
        return null;
    }
    public void loaded(Entity entity){if((recognizes(entity)||restoreJob(entity))&&!entity.isRemoved()){
        if(!current(entity))entity.discard();
        else if(restoreJob(entity))restoreJobs.add((Marker)entity);else arrows.add((AbstractArrow)entity);
    }}
    public void unloaded(Entity entity){arrows.remove(entity);restoreJobs.remove(entity);}
    public void tick(){for(var job:List.copyOf(restoreJobs))restore(job);
        for(var arrow:List.copyOf(arrows))if(arrow.isRemoved()||!current(arrow)){
        arrows.remove(arrow);if(!arrow.isRemoved())arrow.discard();
    }}
    public void clear(){for(var job:List.copyOf(restoreJobs))if(!job.isRemoved())job.discard();restoreJobs.clear();
        for(var arrow:List.copyOf(arrows))if(!arrow.isRemoved())arrow.discard();arrows.clear();}
    public void reindexRestored(Iterable<Entity> restored){arrows.clear();restoreJobs.clear();for(var entity:restored)loaded(entity);}
    static boolean cloneRecognizes(Entity entity){return recognizes(entity);}
    LbcCloneSkills.ActorImage cloneImage(Entity original){
        if(!recognizes(original)||!current(original))return null;
        // Native snapshot copies all tags/arrow components. Only this owner's identity index is rebuilt.
        return CloneComplexProjectileStates.image(this::loaded,this::unloaded);
    }
}

