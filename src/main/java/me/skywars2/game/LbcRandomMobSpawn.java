package me.skywars2.game;

import java.util.List;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;

/** Exact native random_mob_spawn producer; COMMAND finalization follows presence of source NBT. */
final class LbcRandomMobSpawn {
    record Entry(String type,String payload,boolean finalizeSpawn){}
    static final List<Entry> ENTRIES=List.of(
        new Entry("minecraft:fox","{}",true),
        new Entry("minecraft:ghast","{}",true),
        new Entry("minecraft:axolotl","{}",true),
        new Entry("minecraft:bat","{}",true),
        new Entry("minecraft:bee","{}",true),
        new Entry("minecraft:blaze","{}",true),
        new Entry("minecraft:giant","{}",true),
        new Entry("minecraft:cat","{}",true),
        new Entry("minecraft:cave_spider","{}",true),
        new Entry("minecraft:glow_squid","{}",true),
        new Entry("minecraft:chicken","{}",true),
        new Entry("minecraft:cod","{}",true),
        new Entry("minecraft:goat","{}",true),
        new Entry("minecraft:cow","{}",true),
        new Entry("minecraft:creeper","{}",true),
        new Entry("minecraft:dolphin","{}",true),
        new Entry("minecraft:donkey","{}",true),
        new Entry("minecraft:guardian","{}",true),
        new Entry("minecraft:drowned","{equipment:{mainhand:{id:\"minecraft:trident\",count:1}}}",false),
        new Entry("minecraft:hoglin","{}",true),
        new Entry("minecraft:elder_guardian","{}",true),
        new Entry("minecraft:ender_dragon","{DragonPhase:4}",false),
        new Entry("minecraft:enderman","{}",true),
        new Entry("minecraft:endermite","{}",true),
        new Entry("minecraft:evoker","{}",true),
        new Entry("minecraft:horse","{}",true),
        new Entry("minecraft:husk","{}",true),
        new Entry("minecraft:illusioner","{equipment:{mainhand:{id:\"minecraft:bow\",count:1}}}",false),
        new Entry("minecraft:iron_golem","{}",true),
        new Entry("minecraft:llama","{}",true),
        new Entry("minecraft:magma_cube","{}",true),
        new Entry("minecraft:mooshroom","{}",true),
        new Entry("minecraft:mule","{}",true),
        new Entry("minecraft:ocelot","{}",true),
        new Entry("minecraft:panda","{}",true),
        new Entry("minecraft:parrot","{}",true),
        new Entry("minecraft:phantom","{}",true),
        new Entry("minecraft:pig","{}",true),
        new Entry("minecraft:piglin","{equipment:{mainhand:{id:\"minecraft:golden_sword\",count:1}}}",false),
        new Entry("minecraft:piglin_brute","{equipment:{mainhand:{id:\"minecraft:golden_axe\",count:1}}}",false),
        new Entry("minecraft:pillager","{equipment:{mainhand:{id:\"minecraft:crossbow\",count:1}}}",false),
        new Entry("minecraft:polar_bear","{}",true),
        new Entry("minecraft:pufferfish","{}",true),
        new Entry("minecraft:rabbit","{}",true),
        new Entry("minecraft:ravager","{}",true),
        new Entry("minecraft:salmon","{}",true),
        new Entry("minecraft:sheep","{}",true),
        new Entry("minecraft:shulker","{}",true),
        new Entry("minecraft:silverfish","{}",true),
        new Entry("minecraft:skeleton","{equipment:{mainhand:{id:\"minecraft:bow\",count:1}}}",false),
        new Entry("minecraft:skeleton_horse","{}",true),
        new Entry("minecraft:slime","{}",true),
        new Entry("minecraft:snow_golem","{}",true),
        new Entry("minecraft:spider","{}",true),
        new Entry("minecraft:squid","{}",true),
        new Entry("minecraft:stray","{equipment:{mainhand:{id:\"minecraft:bow\",count:1}}}",false),
        new Entry("minecraft:strider","{}",true),
        new Entry("minecraft:trader_llama","{}",true),
        new Entry("minecraft:tropical_fish","{}",true),
        new Entry("minecraft:turtle","{}",true),
        new Entry("minecraft:vex","{equipment:{mainhand:{id:\"minecraft:iron_sword\",count:1}}}",false),
        new Entry("minecraft:villager","{}",true),
        new Entry("minecraft:vindicator","{equipment:{mainhand:{id:\"minecraft:iron_axe\",count:1}}}",false),
        new Entry("minecraft:wandering_trader","{}",true),
        new Entry("minecraft:witch","{}",true),
        new Entry("minecraft:wither","{}",true),
        new Entry("minecraft:wither_skeleton","{equipment:{mainhand:{id:\"minecraft:stone_sword\",count:1}}}",false),
        new Entry("minecraft:wolf","{}",true),
        new Entry("minecraft:zoglin","{}",true),
        new Entry("minecraft:zombie","{}",true),
        new Entry("minecraft:zombie_horse","{}",true),
        new Entry("minecraft:zombie_villager","{}",true),
        new Entry("minecraft:zombified_piglin","{equipment:{mainhand:{id:\"minecraft:golden_sword\",count:1}}}",false),
        new Entry("minecraft:allay","{}",true),
        new Entry("minecraft:creeper","{powered:1b}",false),
        new Entry("minecraft:frog","{}",true),
        new Entry("minecraft:tadpole","{}",true),
        new Entry("minecraft:warden","{Brain:{memories:{\"minecraft:dig_cooldown\":{value:{},ttl:1200L}}}}",false),
        new Entry("minecraft:sniffer","{}",true),
        new Entry("minecraft:camel","{}",true),
        new Entry("minecraft:breeze","{}",true),
        new Entry("minecraft:armadillo","{}",true),
        new Entry("minecraft:bogged","{}",true),
        new Entry("minecraft:creaking","{Invulnerable:true}",false),
        new Entry("minecraft:happy_ghast","{}",true),
        new Entry("minecraft:copper_golem","{}",true),
        new Entry("minecraft:camel_husk","{}",true),
        new Entry("minecraft:parched","{}",true),
        new Entry("minecraft:nautilus","{}",true),
        new Entry("minecraft:zombie_nautilus","{}",true),
        new Entry("minecraft:sulfur_cube","{}",true) );
    private LbcRandomMobSpawn(){}
    static Entity spawn(GameController game,ServerPlayer player,int selected){
        if(selected<1||selected>ENTRIES.size())throw new IllegalArgumentException("Source mob index "+selected);
        var pos=player.blockPosition();var level=player.level();
        if(!level.getWorldBorder().isWithinBounds(pos)||level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)return null;
        var entry=ENTRIES.get(selected-1);
        try{return SummonCommand.createEntity(player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput(),
                BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entry.type())).orElseThrow(),player.position(),TagParser.parseCompoundFully(entry.payload()),entry.finalizeSpawn());}
        catch(CommandSyntaxException rejected){return null;}
    }
    static Entity random(GameController game,ServerPlayer player){
        int index=1+player.getRandom().nextInt(ENTRIES.size());
        var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");
        var modifier=objective==null?null:board.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("final_luck_modifier"),objective);
        if(modifier!=null&&modifier.value()==7)index=22;
        return spawn(game,player,index);
    }
}
