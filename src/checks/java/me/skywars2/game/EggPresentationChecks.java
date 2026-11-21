package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Source names cross real native placement and wire codecs; ant physics retain native feet. */
public final class EggPresentationChecks {
    private EggPresentationChecks() { }
    private static void require(boolean ok,String message) {
        if(!ok) throw new IllegalStateException("Egg presentation: "+message);
    }
    static void name(Entity actor,ItemStack stack) {
        var data=stack.get(DataComponents.ENTITY_DATA);
        require(data!=null && actor.getType()==data.type(),"source actor type");
        if(!data.contains("CustomName")) return;
        var expected=TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
                actor.registryAccess(),data.copyTagWithoutId()).read("CustomName",ComponentSerialization.CODEC).orElseThrow();
        require(expected.equals(actor.getCustomName()),"exact authored entity name replaces recursively inherited egg styles: "+LbcSpecialEggs.kind(stack));
        var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),actor.registryAccess());
        try {
            ComponentSerialization.STREAM_CODEC.encode(buffer,actor.getCustomName());
            require(expected.equals(ComponentSerialization.STREAM_CODEC.decode(buffer)) && !buffer.isReadable(),
                    "native component wire codec retains exact authored name");
        } finally { buffer.release(); }
        if(LbcSpecialEggs.kind(stack)==LbcSpecialEggs.Kind.FAT_SKELETON)
            require(actor.getCustomName().getStyle().getColor()==null && !actor.getCustomName().getStyle().isItalic(),
                    "fat skeleton retains source default white and nonitalic style");
    }
    private static void poseMath() {
        // Exact old/new client traces and author model are documented in ANT_APPEARANCE_AND_EGG_NAMES.md.
        // Model vertices are centered at.5 by native ItemTransform after its display transform.
        for(double x:new double[]{5,6,10,11}) for(double y:new double[]{0,3}) for(double z:new double[]{4,6,8,10,12,14}) {
            double oldX=.75*.625*2*(x/16-.5);
            double oldY=12.0/16-.75*.25-.75*.625*(-14.75/16+2*(y/16-.5));
            double oldZ=-.75*.625*(2.0/16+2*(z/16-.5));
            double newX=.625*1.5*(x/16-.5);
            double newY=15.25/16-.25-.625*(-7.4625/16+1.5*(y/16-.5));
            double newZ=-.625*(1.5/16+1.5*(z/16-.5));
            require(Math.abs(oldX-newX)<1e-12 && Math.abs(oldY-newY)<1e-12 && Math.abs(oldZ-newZ)<1e-12,
                    "derived26.3 RP correction matches old neutral ant geometry");
        }
        double oldFoot=1.501-(12.0/16-.75*.25-.75*.625*(-14.75/16-1));
        double unpatched=1.501-(15.25/16-.25-.625*(-14.75/16-1));
        require(Math.abs(oldFoot-.03762109375)<1e-12 && Math.abs(unpatched+.403296875)<1e-12,
                "exact source transform explains ant foot sinking");
    }
    public static void run(GameController game) {
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing() && owner!=null && game.allowDamage(owner),"actual match participant");
        var level=game.maps().level();
        var state=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var pad=new BlockPos((int)Math.floor(owner.getX()/16)*16+8,Math.min(270,level.getMaxY()-10),(int)Math.floor(owner.getZ()/16)*16+8);
        require(level.getChunkSource().getChunkNow(pad.getX()>>4,pad.getZ()>>4)!=null,"resident test column");
        var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        level.getAllEntities().forEach(before::add);
        try {
            for(var mutable:BlockPos.betweenClosed(pad.offset(-1,0,-1),pad.offset(1,5,1))) {
                var pos=mutable.immutable();
                require(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"resident test halo");
                blocks.put(pos,game.maps().snapshot(level,pos));
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),818);
            }
            level.setBlock(pad,Blocks.STONE.defaultBlockState(),818);
            owner.closeContainer(); owner.getInventory().clearContent(); owner.removeAllEffects();
            owner.setGameMode(GameType.SURVIVAL); owner.setNoGravity(true);
            var at=Vec3.atLowerCornerOf(pad).add(.5,1,.5);
            owner.teleportTo(level,at.x,at.y+2,at.z-3,Set.of(),0,0,true);
            var hit=new BlockHitResult(Vec3.atCenterOf(pad).add(0,.5,0),Direction.UP,pad,false);
            for(var kind:LbcSpecialEggs.Kind.values()) for(boolean renamed:new boolean[]{false,true}) {
                var stack=new ItemStack(kind.item(),2);
                if(renamed) stack.set(DataComponents.CUSTOM_NAME,Component.literal("Renamed probe")
                        .withStyle(ChatFormatting.LIGHT_PURPLE,ChatFormatting.BOLD,ChatFormatting.ITALIC,ChatFormatting.UNDERLINE));
                itemProxy(owner,stack,kind);
                var prior=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
                level.getAllEntities().forEach(prior::add);
                owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                require(owner.gameMode.useItemOn(owner,level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()
                        && stack.getCount()==1,"actual block placement and one-unit debit "+kind);
                var fresh=new ArrayList<Entity>();
                for(var e:level.getAllEntities()) if(!prior.contains(e))fresh.add(e);
                var data=stack.get(DataComponents.ENTITY_DATA);
                var actors=fresh.stream().filter(e->e.getType()==data.type()).toList();
                require(actors.size()==1,"one configured actor from real item "+kind);
                var actor=actors.getFirst(); name(actor,stack);
                if(!data.contains("CustomName"))require(Objects.equals(actor.getCustomName(),stack.get(DataComponents.CUSTOM_NAME)),
                        "unnamed source retains native egg title/rename behavior "+kind);
                if(kind==LbcSpecialEggs.Kind.ANT) antBody((Zombie)actor,at);
                fresh.forEach(Entity::discard);
            }
            var token=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(LbcAntDropItem.Kind.ANTS.item()));
            token.setThrower(owner); token.setNoGravity(true);token.setPickUpDelay(1000);
            require(level.addFreshEntity(token) && game.items().specialDrops().spawn(token),"actual native ant drop dispatch");
            var ants=new ArrayList<Zombie>();
            for(var e:level.getAllEntities())if(!before.contains(e) && e instanceof Zombie z && !z.isRemoved() && z.entityTags().contains("muravei"))ants.add(z);
            require(ants.size()==5,"five original native ant bodies");
            for(var ant:ants)antBody(ant,at);
            poseMath();
        } finally {
            var cleanup=new ArrayList<Entity>();for(var actor:level.getAllEntities())if(!before.contains(actor))cleanup.add(actor);
            for(var actor:cleanup){if(actor instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);actor.discard();}
            for(var entry:blocks.entrySet()) {
                if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();
                game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());
            }
            state.restore(game);
        }
        SkyWars2.LOGGER.info("[check] PASS egg-presentation");
    }
    private static void itemProxy(net.minecraft.server.level.ServerPlayer owner,ItemStack stack,LbcSpecialEggs.Kind kind) {
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
            @Override public net.minecraft.network.PacketListener getPacketListener(){return owner.connection;}
        };
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)connection;
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()-> {
            var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,
                    net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),owner.registryAccess());
            var expected=((eu.pb4.polymer.core.api.item.PolymerItem)kind.item()).getPolymerItem(stack,provider.getPacketContext());
            require(proxy.is(expected) && proxy.getCount()==stack.getCount()
                    && Objects.equals(stack.get(DataComponents.CUSTOM_NAME),proxy.get(DataComponents.CUSTOM_NAME))
                    && ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(
                            proxy,provider.getPacketContext(),owner.registryAccess())),
                    "egg recipient proxy/name and authoritative restoration "+kind);
        });
    }
    private static void antBody(Zombie ant,Vec3 feet) {
        require(ant.isBaby() && ant.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)
                && Math.abs(ant.getBoundingBox().minY-feet.y)<1e-9 && Math.abs(ant.getY()-feet.y)<1e-9,
                "ant feet use native support surface without server cosmetic offset");
        var ordinary=EntityTypes.ZOMBIE.create((ServerLevel)ant.level(),EntitySpawnReason.COMMAND);
        require(ordinary!=null,"native reference zombie"); ordinary.setBaby(true);
        require(ant.getBbHeight()==ordinary.getBbHeight() && ant.getBbWidth()==ordinary.getBbWidth(),
                "ant keeps exact native baby zombie physical dimensions");ordinary.discard();
        require(net.minecraft.resources.Identifier.parse("lbc:muravei").equals(
                ant.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.ITEM_MODEL)),"original shared RP ant head model");
    }
}
