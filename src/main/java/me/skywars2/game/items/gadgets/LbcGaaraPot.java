package me.skywars2.game.items.gadgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import me.skywars2.game.GameController;
import me.skywars2.game.GameController.Stage;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.effects.ItemEffectLevels;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.items.tmt.LbcTmtInventory;
import me.skywars2.game.items.wands.LbcStonekivi;
import me.skywars2.game.world.maps.NativeChunkLoad;
import me.skywars2.game.world.terrain.LbcDropBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** One inventory pot reaction per admitted native incoming criterion. */
public final class LbcGaaraPot {
    private static final TagKey<Block> REPLACE=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:tannerkivi"));
    private final GameController game;
    private final Set<Terrain> pending=Collections.newSetFromMap(new IdentityHashMap<>());
    public LbcGaaraPot(GameController game){this.game=game;}
    public int count(){return pending.size();}
    public static boolean matches(ItemStack stack){var pattern=new net.minecraft.nbt.CompoundTag();pattern.putInt("gaara_tikva",1);return !stack.isEmpty()&&LbcTmtInventory.matches(stack,pattern);}

    /** The incoming owner has already passed pre-hit alive/role admission, including fatal hits. */
    public void incoming(ServerPlayer owner){
        ItemStack pot=null;
        for(int slot=0;slot<=40;slot++)if(matches(owner.getInventory().getItem(slot))){pot=owner.getInventory().getItem(slot);break;}
        if(pot!=null)propertyIncoming(owner,pot);
    }
    public void propertyIncoming(ServerPlayer owner,ItemStack pot){
        if(owner==null||owner.isRemoved()||owner.level()!=game.maps().level()||!game.playing()
                ||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.stage()==GameController.Stage.ENDING||game.items().actionsBlocked(owner)
                ||game.items().wands().inputTimeStopped(owner)||game.items().timeStop().stopped(owner.level()))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted"))
                ||owner.entityTags().contains("nomagic_active")||game.items().magic().suppressed(owner))return;
        if(pot.isEmpty()||!matches(pot))return;
        // Debit before any payload can cause another native damage callback.
        pot.shrink(1);owner.getInventory().setChanged();owner.containerMenu.broadcastChanges();
        var task=new Terrain(owner);pending.add(task);game.items().stonekivi().queuedWork(task);
        owner.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,40,15),owner);
        owner.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,400,0),owner);
        LbcOverload.sound(owner,owner.position().add(0,1.5,0),"minecraft:entity.item.break",1,1,0);
        var model=new ItemStack(Items.REPEATING_COMMAND_BLOCK);model.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:sand_pot"));
        owner.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM,ItemStackTemplate.fromNonEmptyStack(model)),
                owner.getX(),owner.getY()+1.3,owner.getZ(),30,.3,.3,.3,0);
    }

    /** Accepted construction settles through the common queue before Vanity captures terrain. */
    private final class Terrain implements LbcStonekivi.Work {
        final ServerLevel level;final LbcIndirectKills.Owner cause;final String epoch;final BlockPos from,to;boolean cancelled;
        final List<ChunkPos> footprint=new ArrayList<>();
        List<CompletableFuture<?>> loading;

        Terrain(ServerPlayer owner){
            level=owner.level();cause=game.items().indirectKills().coldCause(owner,false);epoch=game.items().epoch();
            var center=owner.blockPosition();from=center.offset(-10,-10,-10);to=center.offset(10,10,10);
            // The native fill also updates immediate neighbors of the volume.
            for(int x=(from.getX()-1)>>4;x<=((to.getX()+1)>>4);x++)
                for(int z=(from.getZ()-1)>>4;z<=((to.getZ()+1)>>4);z++)
                    footprint.add(new ChunkPos(x,z));
        }
        boolean live(){return !cancelled&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch);}
        private boolean resident(ChunkPos chunk){
            return level.getChunkSource().getChunkNow(chunk.x(),chunk.z())!=null;
        }
        @Override public boolean ready(){
            if(!live())return true;
            if(game.items().timeStop().stopped(level))return false;
            if(loading==null) {
                loading=new ArrayList<>();
                for(var chunk:footprint) if(!resident(chunk)) loading.add(game.maps().prepareChunk(level,chunk));
            }
            return loading.stream().anyMatch(NativeChunkLoad::failed) || loading.stream().allMatch(CompletableFuture::isDone);
        }
        @Override public boolean step(){
            if(!ready())return false;
            if(live()&&loading.stream().noneMatch(NativeChunkLoad::failed)&&footprint.stream().allMatch(this::resident))
                game.items().indirectKills().trapChanges(level,cause,
                        ()->LbcDropBlocks.fill(level,from,to,Blocks.SAND,false,state->state.is(REPLACE)));
            cancel();return true;
        }
        @Override public void cancel(){cancelled=true;pending.remove(this);}
    }
}
