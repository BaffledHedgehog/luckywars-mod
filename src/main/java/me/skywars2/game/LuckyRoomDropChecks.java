package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Synchronous native geometry, selection, loot and admission fixtures. */
public final class LuckyRoomDropChecks {
    private LuckyRoomDropChecks() { }
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static void require(boolean ok,String text) { if(!ok)throw new IllegalStateException("Lucky Room: "+text); }
    private static ItemEntity token(ServerLevel level,Vec3 at,LbcLuckyRoomDropItem.Kind kind,int count,ServerPlayer author) {
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(kind.item(),count));item.setThrower(author);
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static BlockState expected(int x,int y,int z) {
        if(y==2 && Math.abs(x)==2 && Math.abs(z)==2)return Blocks.GOLD_BLOCK.defaultBlockState();
        if(Math.abs(x)<=2 && Math.abs(z)<=2 && y>=-1 && y<=1) {
            if(y>=0 && Math.abs(x)<=1 && Math.abs(z)<=1) {
                if(y==0 && Math.abs(x)==1 && Math.abs(z)==1) {
                    var state=Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();
                    for(var property:state.getProperties())state=state.setValue((net.minecraft.world.level.block.state.properties.BooleanProperty)property,false);
                    return state;
                }
                return Blocks.AIR.defaultBlockState();
            }
            return Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.YELLOW).defaultBlockState();
        }
        return Blocks.DIAMOND_BLOCK.defaultBlockState();
    }
    public static void run(GameController game) {
        var level=game.maps().level();
        var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.playing() && alpha!=null && beta!=null,"two actual match participants");
        var aState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());
        var bState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
        var origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);
        var center=BlockPos.containing(origin);var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());level.getAllEntities().forEach(existing::add);
        var tokens=new ArrayList<ItemEntity>();
        try {
            game.items().specialDrops().stop();
            for(var p:BlockPos.betweenClosed(center.offset(-3,-2,-3),center.offset(3,3,3))) {
                var pos=p.immutable();level.getChunkAt(pos);blocks.put(pos,game.maps().snapshot(pos));
            }
            for(var p:List.of(alpha,beta)){p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.removeAllEffects();}
            alpha.teleportTo(level,origin.x+20,origin.y,origin.z,Set.of(),0,0,true);
            beta.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);
            for(var kind:LbcLuckyRoomDropItem.Kind.values()) {
                var source=new ItemStack(kind.item());var flags=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                require(flags.size()==2 && flags.getIntOr("lbh",0)==1 && flags.getIntOr("drop",0)==1 && LbcLuckyRoomDropItem.kind(source)==kind,"exact source flags/variant");
                String leaf=kind==LbcLuckyRoomDropItem.Kind.NORMAL?"lucky_room_drop":"old_lucky_room_drop";
                var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/"+leaf);
                require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(source,loot.getFirst()),"real public loot leaf");
                if(kind==LbcLuckyRoomDropItem.Kind.NORMAL) {
                    require(Identifier.parse("lbc:tier_c").equals(source.get(DataComponents.TOOLTIP_STYLE)) && Identifier.parse("lbc:drop_not_trap").equals(source.get(DataComponents.ITEM_MODEL)),"normal source frame/model");
                    var name=source.get(DataComponents.CUSTOM_NAME);require(name!=null && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text && text.getKey().equals("drop_lbc_room") && !name.getStyle().isItalic() && name.getStyle().getColor().getValue()==0xffff55,"normal translated yellow nonitalic name");
                    var lore=source.get(DataComponents.LORE).lines();require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"normal white nonitalic lore");
                } else require(!source.has(DataComponents.CUSTOM_NAME) && source.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines().isEmpty() && !source.has(DataComponents.TOOLTIP_STYLE) && Identifier.parse("minecraft:barrier").equals(source.get(DataComponents.ITEM_MODEL)),"old plain barrier");
                for(var pos:blocks.keySet()){if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),818);}
                level.setBlock(center,Blocks.CHEST.defaultBlockState(),818);
                ((net.minecraft.world.Container)level.getBlockEntity(center)).setItem(0,new ItemStack(Items.IRON_INGOT,7));
                var t=token(level,origin,kind,2,alpha);tokens.add(t);
                require(game.items().cageDrops().target(t)==beta && game.items().luckyRoomDrops().ready(t),"nearest fractional target ready");
                require(game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"shared one-unit debit");
                require(!game.items().specialDrops().spawn(t) && t.getItem().getCount()==1,"same-tick replay rejected");
                for(var pos:blocks.keySet()){var d=pos.subtract(center);require(level.getBlockState(pos).equals(expected(d.getX(),d.getY(),d.getZ())),"exact native shape and outside sentinel "+d);}
                int iron=0;for(var e:level.getAllEntities())if(!existing.contains(e) && e instanceof ItemEntity item && item.getItem().is(Items.IRON_INGOT))iron+=item.getItem().getCount();
                require(iron==7*(kind==LbcLuckyRoomDropItem.Kind.NORMAL?1:2),"native replaced-container contents preserved as drops");t.discard();
            }
            var t=token(level,origin,LbcLuckyRoomDropItem.Kind.NORMAL,2,alpha);tokens.add(t);
            alpha.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+5,origin.y,origin.z,Set.of(),0,0,true);
            require(game.items().cageDrops().target(t)==alpha,"caster is eligible WILD target");
            alpha.setGameMode(GameType.CREATIVE);require(game.items().cageDrops().target(t)==alpha,"Creative target remains eligible");
            alpha.setGameMode(GameType.SPECTATOR);require(game.items().cageDrops().target(t)==beta,"spectator excluded");alpha.setGameMode(GameType.SURVIVAL);
            for(String role:List.of("spectator","rcdenyalways","sauvojen_tuntija","strict_map_object","magic_academy","transformator")){alpha.entityTags().add(role);try{require(game.items().cageDrops().target(t)==beta,"service role excluded "+role);}finally{alpha.entityTags().remove(role);}}
            var modes=new LinkedHashMap<ServerPlayer,GameType>();for(var p:level.players())modes.put(p,p.gameMode.getGameModeForPlayer());
            try {
                for(var p:modes.keySet())p.setGameMode(GameType.SPECTATOR);
                var noTarget=token(level,origin,LbcLuckyRoomDropItem.Kind.OLD,2,alpha);tokens.add(noTarget);
                require(game.items().cageDrops().target(noTarget)==null && game.items().specialDrops().spawn(noTarget) && noTarget.getItem().getCount()==1,"empty selector still consumes one token");noTarget.discard();
            } finally {for(var entry:modes.entrySet()){entry.getKey().setGameMode(entry.getValue());entry.getKey().noPhysics=false;}}
            game.items().timeStop().begin(level,200);try{require(!game.items().luckyRoomDrops().ready(t) && !game.items().specialDrops().spawn(t) && t.getItem().getCount()==2,"frozen token preserves debit");}finally{game.items().timeStop().end(level);}
            t.entityTags().add("skywars2.items.stale-fixture");require(!game.items().luckyRoomDrops().ready(t),"stale epoch rejected");t.discard();
            require(!LbcLuckyRoomDropItem.matches(new ItemStack(Items.BARRIER)),"plain lookalike rejected");
            int limit=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS);
            try {
                level.setBlock(center,Blocks.CHEST.defaultBlockState(),818);
                ((net.minecraft.world.Container)level.getBlockEntity(center)).setItem(0,new ItemStack(Items.DIAMOND,3));
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,1,game.server());
                var limited=token(level,origin,LbcLuckyRoomDropItem.Kind.NORMAL,1,alpha);tokens.add(limited);
                require(game.items().specialDrops().spawn(limited),"per-command fill failure still consumes token");
                require(level.getBlockState(center).is(Blocks.CHEST) && ((net.minecraft.world.Container)level.getBlockEntity(center)).getItem(0).getCount()==3,"rejected fills preserve native container and contents");
                require(level.getBlockState(center.offset(2,2,2)).is(Blocks.GOLD_BLOCK),"independent source setblocks survive fill-limit failure");
            } finally {level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS,limit,game.server());}
            SkyWars2.LOGGER.info("[check] PASS lucky-room-drops");
        } finally {
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();
            var added=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e) && !(e instanceof ServerPlayer) && Math.abs(e.getX()-origin.x)<8 && Math.abs(e.getZ()-origin.z)<8)added.add(e);added.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(level.getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(e.getKey(),e.getValue());}
            game.items().indirectKills().clear(alpha);game.items().indirectKills().clear(beta);aState.restore(game);bState.restore(game);
        }
    }
    public static void stopProbe(GameController game) {
        require(game.playing(),"actual match stop probe");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        stopEpoch=game.items().epoch();game.items().timeStop().begin(level,200);stopToken=token(level,alpha.position(),LbcLuckyRoomDropItem.Kind.NORMAL,3,alpha);
        require(!game.items().specialDrops().spawn(stopToken) && stopToken.getItem().getCount()==3,"frozen pending token");SkyWars2.LOGGER.info("[check] PASS lucky-room-drops-stop-probe");
    }
    public static void cleanup(GameController game) {
        require(stopEpoch!=null && !game.items().currentEpoch(stopEpoch) && stopToken!=null && stopToken.getItem().getCount()==3,"actual stop epoch/debit");
        require(!game.items().specialDrops().spawn(stopToken),"stopped replay rejected");stopToken.discard();stopToken=null;stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS lucky-room-drops-cleanup");
    }
}
