package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;

/** Four source table rolls per actual victim drop ENTITY, accepted by the shared death scope. */
public final class LbcHakureiSkill {
    private final GameController game;
    private final Predicate<ServerPlayer> admission;
    private final ArrayDeque<Reward> rewards=new ArrayDeque<>();
    private boolean restoring,publishing;
    private static final class Reward{
        final UUID owner;final String epoch;final ServerLevel level;final Vec3 at;
        int remaining;long random;final ArrayDeque<ItemStack> outputs=new ArrayDeque<>();boolean announced;
        Reward(UUID owner,String epoch,ServerLevel level,Vec3 at,int count,long seed){this.owner=owner;this.epoch=epoch;this.level=level;this.at=at;remaining=count;random=seed;}
        Reward(Reward old){this(old.owner,old.epoch,old.level,old.at,old.remaining,old.random);old.outputs.forEach(s->outputs.add(s.copy()));announced=old.announced;}
        long seed(){random+=0x9e3779b97f4a7c15L;long z=random;z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;z^=z>>>31;return z==0?1:z;}
    }
    public LbcHakureiSkill(GameController game,Predicate<ServerPlayer> admission){this.game=game;this.admission=Objects.requireNonNull(admission);}
    public boolean eligible(ServerPlayer owner){return game.lbc().hasSkill(owner,28)&&admission.test(owner);}
    public boolean publishing(){return publishing;}
    /** Called once per successfully published real ItemEntity/ExperienceOrb captured by CombatRewards. */
    public void reward(ServerPlayer owner,ServerLevel level,Vec3 at,int entityCount){
        if(restoring||publishing||entityCount<=0||!eligible(owner))return;
        // One record per actual entity bounds each record's transaction and preserves entity-count semantics.
        for(int n=0;n<entityCount;n++)rewards.add(new Reward(owner.getUUID(),game.items().epoch(),level,at,4,owner.getRandom().nextLong()));
    }
    private boolean paused(ServerPlayer player,ServerLevel level){return game.items().timeStop().stopped(level)||game.items().timeStop().frozen(player)||game.items().wands().inputTimeStopped(player);}
    public void tick(){
        if(restoring||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        int visits=Math.min(64,rewards.size()),budget=32;
        while(visits-->0&&budget>0){
            var reward=rewards.removeFirst();var owner=game.server().getPlayerList().getPlayer(reward.owner);
            // An accepted reward is independent of a subsequent skill selection. Epoch/lifecycle owns cancellation.
            if(!game.items().currentEpoch(reward.epoch)||owner==null||owner.isRemoved())continue;
            if(paused(owner,reward.level)||!reward.level.hasChunkAt(net.minecraft.core.BlockPos.containing(reward.at))){rewards.addLast(reward);continue;}
            if(!reward.announced){reward.level.sendParticles(ParticleTypes.END_ROD,reward.at.x,reward.at.y,reward.at.z,4,.1,.1,.1,0);LbcOverload.sound(reward.level,reward.at,"minecraft:block.amethyst_block.chime",net.minecraft.sounds.SoundSource.BLOCKS,.5f,2,0,0);reward.announced=true;}
            if(reward.outputs.isEmpty()&&reward.remaining>0){
                var table=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:touhou_item")));
                var params=new LootParams.Builder(reward.level).withOptionalParameter(LootContextParams.THIS_ENTITY,owner).withParameter(LootContextParams.ORIGIN,reward.at).create(LootContextParamSets.COMMAND);
                reward.outputs.addAll(table.getRandomItems(params,reward.seed()));reward.remaining--;budget--;
            }
            if(!reward.outputs.isEmpty()){
                var stack=reward.outputs.peekFirst();var child=new ItemEntity(reward.level,reward.at.x,reward.at.y,reward.at.z,stack.copy());child.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(child);
                publishing=true;boolean added;
                try{added=reward.level.addFreshEntity(child);}finally{publishing=false;}
                if(added)reward.outputs.removeFirst();
            }
            if(reward.remaining>0||!reward.outputs.isEmpty())rewards.addLast(reward);
        }
    }
    int pendingCount(){return rewards.stream().mapToInt(r->r.remaining+r.outputs.size()).sum();}
    public void forget(ServerPlayer player){if(!restoring&&!MatchPlayerCheckpoint.restoring(player))rewards.removeIf(r->r.owner.equals(player.getUUID()));}
    public void stop(){rewards.clear();}
    public void prepareFrozen(){if(publishing)throw new IllegalStateException("Hakurei rewind during native publication");restoring=true;rewards.clear();}
    public void publishFrozen(){restoring=false;}
    Checkpoint checkpoint(){if(publishing)throw new IllegalStateException("Hakurei checkpoint during native publication");return new Checkpoint();}
    final class Checkpoint{
        private final List<Reward> saved=rewards.stream().map(Reward::new).toList();
        MatchFieldCheckpoint.Plan prepare(Function<UUID,Entity> lookup){return new MatchFieldCheckpoint.Plan(Set.of(),()->{rewards.clear();saved.forEach(r->rewards.add(new Reward(r)));});}
    }
}
