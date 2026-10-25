package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual game-mode use of the nine narrow library leaves and their shared native payloads. */
public final class ElementalGrimoireChecks {
    private ElementalGrimoireChecks() { }
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Elemental grimoire: "+why);
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){ }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcMana mana;final ServerPlayer owner;
        final GameChecks.WandThirdPlayerState saved;final boolean offhand,shift,sprint,ground;
        final ServerGamePacketListenerImpl connection;final Recording recording;
        final Map<LbcElementalGrimoires.Kind,Integer> stats=new EnumMap<>(LbcElementalGrimoires.Kind.class);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        Marker field;boolean retain;
        Fixture(GameController game) {
            this.game=game;mana=game.items().mana();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(owner!=null&&game.playing()&&game.canUseGameplayItems(owner),"active participant");
            require(owner.containerMenu==owner.inventoryMenu&&count()==0,"closed inventory and exclusive empty payload services");
            saved=new GameChecks.WandThirdPlayerState(owner,mana);offhand=game.config().offhandWands;
            shift=owner.isShiftKeyDown();sprint=owner.isSprinting();ground=owner.onGround();
            owner.level().getAllEntities().forEach(before::add);
            for(var kind:LbcElementalGrimoires.Kind.values())stats.put(kind,owner.getStats().getValue(Stats.ITEM_USED.get(kind.item)));
            connection=owner.connection;recording=new Recording(owner);
            owner.connection=new ServerGamePacketListenerImpl(game.server(),recording,owner,
                    new CommonListenerCookie(owner.getGameProfile(),0,owner.clientInformation(),false));
            owner.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        void initialize() {
            var at=new Vec3(owner.getX(),Math.min(240,owner.level().getMaxY()-40),owner.getZ());
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-8,-3,-8)),BlockPos.containing(at.add(8,5,8))))
                require(owner.level().hasChunkAt(pos)&&owner.level().getBlockState(pos).isAir(),"resident air launch volume");
            game.config().offhandWands=true;owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);
            owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setShiftKeyDown(false);
            owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);
            owner.getInventory().setSelectedSlot(0);mana.set(owner,0);
            game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
        }
        int count() {
            return game.items().ruby().count()+game.items().terra().count()+game.items().wands().electroCount()
                    +game.items().air().count()+game.items().clouds().acidCount()+game.items().teleport().count()
                    +game.items().stone().count()+game.items().blind().count()+fire().size();
        }
        List<Projectile> fire() {
            var out=new ArrayList<Projectile>();
            for(var entity:owner.level().getAllEntities())if(!before.contains(entity)&&!entity.isRemoved()
                    &&entity.getType()==EntityTypes.SMALL_FIREBALL&&entity instanceof Projectile projectile)out.add(projectile);
            return out;
        }
        ItemStack loot(LbcElementalGrimoires.Kind kind) {
            var result=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/"+kind.id);
            require(result.size()==1&&result.getFirst().getCount()==1,"singleton narrow loot "+kind);
            return result.getFirst();
        }
        ItemStack held(LbcElementalGrimoires.Kind kind,InteractionHand hand,int count) {
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var stack=loot(kind);stack.setCount(count);owner.setItemInHand(hand,stack);return stack;
        }
        boolean input(InteractionHand hand) {
            return owner.gameMode.useItem(owner,owner.level(),owner.getItemInHand(hand),hand).consumesAction();
        }
        void presentation(LbcElementalGrimoires.Kind kind) {
            var stack=loot(kind);var defaults=kind.item.getDefaultInstance();
            require(stack.is(kind.item)&&LbcElementalGrimoires.kind(stack)==kind&&stack.getMaxStackSize()==99,"native identity "+kind);
            require(stack.get(DataComponents.ITEM_MODEL).equals(defaults.get(DataComponents.ITEM_MODEL))
                    &&stack.get(DataComponents.TOOLTIP_STYLE).equals(defaults.get(DataComponents.TOOLTIP_STYLE))
                    &&stack.get(DataComponents.CUSTOM_NAME).equals(defaults.get(DataComponents.CUSTOM_NAME))
                    &&stack.get(DataComponents.LORE).equals(defaults.get(DataComponents.LORE))
                    &&stack.get(DataComponents.CUSTOM_DATA).equals(defaults.get(DataComponents.CUSTOM_DATA)),"give and exact imported source components agree "+kind);
            var data=LbcFartus.data(stack);
            require(data.getIntOr("spellid",0)==kind.spellId&&data.getIntOr("mana",0)==kind.metadataMana
                    &&data.getIntOr("spellwithcooldown",0)==0,"source metadata without invented cooldown "+kind);
        }
        void assertPayload(LbcElementalGrimoires.Kind kind) {
            List<? extends Projectile> payload=switch(kind) {
                case RUBY -> game.items().ruby().snapshot();
                case TERRA -> {var out=new ArrayList<Projectile>();for(var e:owner.level().getAllEntities())if(e instanceof LbcTerra.TerraProjectile p&&!p.isRemoved())out.add(p);yield out;}
                case ELECTRO -> List.of();
                case AIR -> game.items().air().snapshot();
                case ACID -> game.items().clouds().acidSnapshot();
                case TELEPORT -> game.items().teleport().snapshot();
                case STONE -> game.items().stone().snapshot();
                case FIRE -> fire();
                case BLIND -> game.items().blind().snapshot();
            };
            int expected=switch(kind){case ELECTRO,FIRE -> 3;case BLIND -> 5;default -> 1;};
            require(count()==expected,"exact source charge count "+kind+" actual="+count());
            if(kind==LbcElementalGrimoires.Kind.ELECTRO) {
                var traces=game.items().wands().electroSnapshot();
                require(traces.size()==3,"three electrical traces");
                for(int i=0;i<3;i++)require(traces.get(i).owner==owner&&traces.get(i).lifetime==40+i*5
                        &&traces.get(i).trialOwner==null,"captured electrical owner/lifetime/no trial");
            } else {
                require(payload.size()==expected,"native payload count "+kind);
                for(var projectile:payload)require(projectile.getOwner()==owner&&game.items().currentEpoch(projectile)
                        &&projectile.getDeltaMovement().lengthSqr()>0,"captured owner/epoch/motion "+kind);
            }
            require(game.items().air().snapshot().stream().noneMatch(p->p.trial)
                    &&game.items().blind().snapshot().stream().noneMatch(p->p.trial),"consumed cast has no forged Academy trial");
            for(var e:owner.level().getAllEntities())require(before.contains(e)||e.getType()!=EntityTypes.SNOWBALL,"no snowball carrier");
            String sound=switch(kind) {
                case RUBY,TERRA,STONE -> "minecraft:block.amethyst_cluster.break";
                case ELECTRO -> "minecraft:entity.firework_rocket.twinkle";
                case AIR -> "minecraft:entity.generic.explode";
                case ACID -> "minecraft:entity.zombie_villager.cure";
                case TELEPORT -> "minecraft:entity.lightning_bolt.thunder";
                case FIRE -> "minecraft:item.firecharge.use";
                case BLIND -> "minecraft:entity.squid.squirt";
            };
            require(recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance)
                    .map(ClientboundSoundPacket.class::cast).anyMatch(p->p.getSound().value().location().toString().equals(sound)),"native source sound "+kind);
        }
        void clear() {
            game.items().ruby().stop();game.items().terra().stop();game.items().wands().stop();game.items().air().stop();
            game.items().clouds().stop();game.items().teleport().stop();game.items().stone().stop();game.items().blind().stop();
            fire().forEach(Entity::discard);recording.packets.clear();
        }
        void casts() {
            for(var kind:LbcElementalGrimoires.Kind.values()) {
                presentation(kind);
                for(var hand:InteractionHand.values())for(boolean sneak:new boolean[]{false,true}) {
                    var stack=held(kind,hand,2);owner.setShiftKeyDown(sneak);mana.set(owner,sneak?-200:0);
                    var data=LbcFartus.data(stack);data.putInt("upgradable",1);data.putInt("mode",3);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
                    int balance=mana.get(owner),stat=owner.getStats().getValue(Stats.ITEM_USED.get(kind.item));
                    require(input(hand)&&stack.getCount()==1&&mana.get(owner)==balance,"one consumption/no mana admission or debit "+kind+" "+hand+" shift="+sneak);
                    require(owner.getStats().getValue(Stats.ITEM_USED.get(kind.item))==stat+1,"one native use stat "+kind);
                    require(LbcFartus.data(stack).getIntOr("upgrade_available",0)==0,"never readies grimoire trial");
                    assertPayload(kind);clear();
                }
            }
            owner.setShiftKeyDown(false);
            var main=held(LbcElementalGrimoires.Kind.RUBY,InteractionHand.MAIN_HAND,1);
            var off=loot(LbcElementalGrimoires.Kind.BLIND);owner.setItemInHand(InteractionHand.OFF_HAND,off);
            mana.set(owner,-500);require(input(InteractionHand.MAIN_HAND)&&main.isEmpty()&&off.isEmpty()&&mana.get(owner)==-500
                    &&game.items().ruby().count()==1&&game.items().blind().count()==5,"parallel mixed final-stack hands");clear();
        }
        void guards() {
            for(var kind:LbcElementalGrimoires.Kind.values()) {
                var stack=held(kind,InteractionHand.MAIN_HAND,2);
                owner.entityTags().add("time_stopped");
                try{require(!input(InteractionHand.MAIN_HAND)&&stack.getCount()==2&&count()==0,"personal stop preserves "+kind);}
                finally{owner.entityTags().remove("time_stopped");}
                game.items().timeStop().begin(owner.level(),40);
                try{require(!input(InteractionHand.MAIN_HAND)&&stack.getCount()==2&&count()==0,"dimension stop preserves "+kind);}
                finally{game.items().timeStop().end(owner.level());}
                field=new Marker(EntityTypes.MARKER,owner.level());field.setPos(owner.position());field.entityTags().add("stopper_magic");
                require(owner.level().addFreshEntity(field),"native antimagic fixture");game.items().magic().loaded(field);
                try{require(!input(InteractionHand.MAIN_HAND)&&stack.getCount()==2&&count()==0,"antimagic preserves "+kind);}
                finally{removeField();}
                stack=held(kind,InteractionHand.OFF_HAND,2);game.config().offhandWands=false;
                try{require(!input(InteractionHand.OFF_HAND)&&stack.getCount()==2&&count()==0,"disabled offhand preserves "+kind);}
                finally{game.config().offhandWands=true;}
            }
            game.spectators().enter(owner);
            for(var kind:LbcElementalGrimoires.Kind.values()) {
                var stack=held(kind,InteractionHand.MAIN_HAND,2);
                require(!input(InteractionHand.MAIN_HAND)&&stack.getCount()==2&&count()==0,"observer preserves "+kind);
            }
        }
        void removeField(){if(field!=null){game.items().magic().unloaded(field);field.discard();field=null;game.items().magic().flushCleansing();}}
        @Override public void close() {
            removeField();game.items().timeStop().end(owner.level());if(!retain)clear();
            game.config().offhandWands=offhand;saved.restore(game);owner.setShiftKeyDown(shift);owner.setSprinting(sprint);owner.setOnGround(ground);
            for(var entry:stats.entrySet())owner.getStats().setValue(owner,Stats.ITEM_USED.get(entry.getKey().item),entry.getValue());
            owner.connection=connection;connection.resetPosition();game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
        }
    }
    private static List<Entity> retained;
    private static String retainedEpoch;
    public static void run(GameController game) {
        try(var fixture=new Fixture(game)){fixture.initialize();fixture.casts();fixture.guards();}
        SkyWars2.LOGGER.info("[check] PASS elemental-grimoires");
    }
    public static void stopProbe(GameController game) {
        require(retained==null,"exclusive retained fixture");
        try(var fixture=new Fixture(game)) {
            fixture.initialize();
            for(var kind:LbcElementalGrimoires.Kind.values()) {
                fixture.held(kind,InteractionHand.MAIN_HAND,1);
                require(fixture.input(InteractionHand.MAIN_HAND),"retained actual input "+kind);
            }
            var actors=new ArrayList<Entity>();
            for(var e:fixture.owner.level().getAllEntities())if(!fixture.before.contains(e)&&e instanceof Projectile p&&p.getOwner()==fixture.owner){actors.add(e);e.entityTags().add("time_stopped");}
            require(actors.size()==14&&game.items().wands().electroCount()==3,"14 native projectiles and three nonentity arcs retained");
            retained=List.copyOf(actors);retainedEpoch=game.items().epoch();fixture.retain=true;
        }
        SkyWars2.LOGGER.info("[check] PASS elemental-grimoires-stop-probe");
    }
    public static void cleanup(GameController game) {
        var actors=retained;require(actors!=null&&!actors.isEmpty(),"retained consumed payloads");
        try{require(!game.items().currentEpoch(retainedEpoch)&&actors.stream().allMatch(Entity::isRemoved)
                    &&game.items().wands().electroCount()==0&&game.items().ruby().count()==0&&game.items().terra().count()==0
                    &&game.items().air().count()==0&&game.items().teleport().count()==0&&game.items().stone().count()==0
                    &&game.items().blind().count()==0&&game.items().clouds().acidCount()==0,"actual stop retires all nine engines and epoch");}
        finally{actors.forEach(Entity::discard);retained=null;retainedEpoch=null;}
        SkyWars2.LOGGER.info("[check] PASS elemental-grimoires-cleanup");
    }
}
