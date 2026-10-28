package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Chaos uses the original native loot table, then the ordinary causal dropped-item lifecycle. */
public final class LbcChaosTraps {
    static final ResourceKey<LootTable> TABLE=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:traps"));
    private final GameController game;

    public LbcChaosTraps(GameController game){this.game=game;}

    /** /loot spawn has a chest context and default luck zero, independent of the wearer's Luck. */
    public List<ItemEntity> spawn(ServerPlayer owner,Vec3 pos){
        if(owner==null||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||owner.level()!=game.maps().level()||game.isObserver(owner)
                ||game.items().timeStop().stopped(owner.level())
                ||game.items().magic().beaconSuppressed(owner.position()))return List.of();
        var level=owner.level();
        if(level.getChunkSource().getChunkNow(((int)Math.floor(pos.x))>>4,((int)Math.floor(pos.z))>>4)==null)return List.of();
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,pos)
                .withParameter(LootContextParams.THIS_ENTITY,owner).create(LootContextParamSets.CHEST);
        var table=game.server().reloadableRegistries().getLootTable(TABLE);
        var spawned=new ArrayList<ItemEntity>();
        for(var stack:table.getRandomItems(params))if(!stack.isEmpty()){
            var token=new ItemEntity(level,pos.x,pos.y,pos.z,stack.copy());
            // Thrower is set before ENTITY_LOAD; pickup target must never become the trap's author.
            token.setThrower(owner);
            token.setDefaultPickUpDelay();
            game.items().markCurrentItemEpoch(token);
            game.items().timeStop().prepareFrozen(token);
            if(level.addFreshEntity(token)){
                game.items().specialDrops().loaded(token);
                spawned.add(token);
            }else token.discard();
        }
        return List.copyOf(spawned);
    }
}
