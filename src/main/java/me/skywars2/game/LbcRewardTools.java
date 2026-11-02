package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Raw reward tools use the common resident ray queue and the real terminal hit. */
public final class LbcRewardTools {
    private LbcRewardTools(){}
    private static String kind(ItemStack stack){
        if(stack.isEmpty())return null;var data=LbcFartus.data(stack);
        if(data.getIntOr("raycastspell",0)!=1)return null;
        for(var key:List.of("krazha","shetka","lightning_bolt"))if(data.getIntOr(key,0)==1)return key;
        return null;
    }
    public static boolean matches(ItemStack stack){return kind(stack)!=null;}
    public static boolean use(GameController game,ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);var kind=kind(stack);
        if(kind==null||!game.playing()||game.stage()==GameController.Stage.ENDING||!game.canUseGameplayItems(player)||game.isObserver(player)||!player.isAlive()
                ||!LbcDimensionItems.ordinary(player)||game.items().actionsBlocked(player)
                ||!game.items().wands().validRoleTarget(player,false)||game.items().wands().inputTimeStopped(player)
                ||game.items().wands().timeStopped()||game.items().timeStop().stopped(player.level())
                ||game.items().magic().suppressed(player)||player.entityTags().contains("nomagic_active"))return false;
        var level=player.level();var epoch=game.items().epoch();var direction=player.getLookAngle();
        var from=player.getEyePosition().add(direction.scale(kind.equals("krazha")&&hand==InteractionHand.OFF_HAND?.5:0));
        double range=kind.equals("krazha")?65:kind.equals("shetka")?9:16;
        ParticleOptions dust=kind.equals("krazha")?new DustColorTransitionOptions(0x260049,0,1)
                :kind.equals("shetka")?new DustParticleOptions(0xffffff,.5f):ParticleTypes.END_ROD;
        game.items().exasperato().replayableTool(player,from,direction,range,kind.equals("shetka"),
                new ToolAim(game,player,hand,stack,kind,direction,dust),kind.equals("lightning_bolt"));
        return true;
    }
    private static final class ToolAim implements LbcExasperato.ReplayableRay {
        final GameController game;final java.util.UUID author;final net.minecraft.server.level.ServerLevel level;final String epoch,kind;
        final InteractionHand hand;final net.minecraft.world.phys.Vec3 direction;final ParticleOptions dust;ItemStack stack;
        ToolAim(GameController game,ServerPlayer player,InteractionHand hand,ItemStack stack,String kind,net.minecraft.world.phys.Vec3 direction,ParticleOptions dust){
            this.game=game;author=player.getUUID();level=player.level();epoch=game.items().epoch();this.hand=hand;this.stack=stack;this.kind=kind;this.direction=direction;this.dust=dust;
        }
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return kind.equals("shetka")?!state.isAir():!state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.withDefaultNamespace("airs")));}
        public boolean target(net.minecraft.world.entity.Entity e){return !e.getUUID().equals(author)&&e instanceof LivingEntity&&game.items().wands().validRoleTarget(e,false)&&!LbcGreed.active(e);}
        public void trail(net.minecraft.world.phys.Vec3 a,net.minecraft.world.phys.Vec3 b){double distance=a.distanceTo(b),step=kind.equals("shetka")?.15:kind.equals("krazha")?.5:.4;
            for(double d=0;d<=distance;d+=step){var p=a.add(direction.scale(d));level.sendParticles(dust,true,false,p.x,p.y,p.z,1,0,0,0,.01);}}
        public void complete(HitResult hit){var player=game.server().getPlayerList().getPlayer(author);if(player==null)return;
            if(hit==null||!game.items().currentEpoch(epoch)||!player.isAlive()||player.level()!=level
                    ||!game.canUseGameplayItems(player)||game.items().magic().suppressed(player))return;
            if(kind.equals("krazha")){
                if(player.getItemInHand(hand)!=stack||!(hit instanceof EntityHitResult entity)||!(entity.getEntity() instanceof LivingEntity target))return;
                steal(game,player,hand,target);
            }else if(kind.equals("shetka")&&hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK){
                game.items().indirectKills().trapChanges(level,game.items().indirectKills().wildCause(player),
                        ()->level.setBlock(block.getBlockPos(),Blocks.AIR.defaultBlockState(),3));
                var p=hit.getLocation();level.sendParticles(new DustParticleOptions(0xffffff,.5f),p.x,p.y,p.z,5,0,0,0,.2);
            }else if(kind.equals("lightning_bolt"))game.items().electro().lightning(player,hit.getLocation());

        }
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
            if(!kind.equals("krazha"))return ignored->()->{};var item=links.stack(game,stack);
            return current->{var rebound=item.get(current);return ()->stack=rebound;};
        }
    }
    /** Full stacks/components transfer exactly once. Failure never consumes the glove. */
    static boolean steal(GameController game,ServerPlayer player,InteractionHand hand,LivingEntity target){
        if(target==player||!target.isAlive()||!game.items().wands().validRoleTarget(target,false)||LbcGreed.active(target)
                ||game.items().magic().suppressedAt(player.level(),target.position())
                ||target instanceof ServerPlayer guarded&&game.items().inventoryHazards().protectedInventory(guarded))return false;
        SlotAccess chosen=null;
        for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))if(!target.getItemBySlot(slot).isEmpty()){
            chosen=SlotAccess.forEquipmentSlot(target,slot);break;
        }
        if(chosen==null&&!(target instanceof ServerPlayer)){
            var slots=new ArrayList<SlotAccess>();
            if(target instanceof InventoryCarrier carrier){var inv=carrier.getInventory();for(int i=0;i<inv.getContainerSize();i++)if(!inv.getItem(i).isEmpty())slots.add(inv.getSlot(i));}
            else if(target instanceof AbstractHorse horse)for(int i=0;i<horse.getInventorySize();i++){var slot=horse.getSlot(500+i);if(!slot.get().isEmpty())slots.add(slot);}
            if(slots.isEmpty())return false;chosen=slots.get(player.getRandom().nextInt(slots.size()));
        }
        ItemStack acquired;
        if(chosen==null)acquired=substitute();
        else{acquired=chosen.get().copy();if(!chosen.set(ItemStack.EMPTY))return false;}
        // Keep the independent original five-percent prank after a successful theft.
        if(chosen!=null&&player.getRandom().nextFloat()<.05f)acquired=substitute();
        var glove=player.getItemInHand(hand);glove.shrink(1);
        if(glove.isEmpty())player.setItemInHand(hand,acquired);
        else if(!player.getInventory().add(acquired))player.drop(acquired,false,net.minecraft.util.Prediction.SERVER_ONLY);
        changed(game,player);if(target instanceof ServerPlayer donor)changed(game,donor);
        LbcOverload.sound(player,target.position(),"minecraft:lbcsounds.wololo",1,1,0);
        return true;
    }
    private static void changed(GameController game,ServerPlayer p){p.getInventory().setChanged();game.items().magic().inventoryChanged(p);p.containerMenu.broadcastChanges();}
    static ItemStack substitute(){var result=new ItemStack(Items.REPEATING_COMMAND_BLOCK);
        result.set(DataComponents.CUSTOM_NAME,Component.translatable("what?").withStyle(s->s.withItalic(false)));
        result.set(DataComponents.LORE,new ItemLore(List.of(Component.translatable("what?.lore.1"),Component.translatable("what?.lore.2"))));
        result.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:trusi"));result.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_f"));return result;
    }
}
