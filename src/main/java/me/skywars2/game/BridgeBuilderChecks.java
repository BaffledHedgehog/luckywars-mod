package me.skywars2.game;

import com.google.gson.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.Vec3;

/** DRAFT ONLY. Native input/whole-owner checks plus explicitly separate source-kernel fixtures.
 * Independent oracle is decoded from read-only source NBT by Python, never the service's prepared cells. */
public final class BridgeBuilderChecks {
    private BridgeBuilderChecks() { }
    private static Fixture running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Bridge Builder: "+why);}
    private static Field field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing fixture field "+name);}
    private static Object get(Object value,String name){try{return field(value,name).get(value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object value,String name,Object next){try{field(value,name).set(value,next);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    public static void run(GameController game){start(game,0);}
    public static void runRewind(GameController game){start(game,1);}
    public static void runBedwars(GameController game){start(game,2);}
    private static void start(GameController game,int kind){require(running==null,"exclusive fixture");var f=new Fixture(game,kind);running=f;try{f.begin();}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{require(game==f.game,"same native controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} (actual typed native snowball/thrower, exact source templates and resumable native terrain owner; scoped source-kernel cases are labeled separately)",f.name());}catch(Throwable error){running=null;try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+f.name(),error);}}
    private static final String ORACLE_GZIP_BASE64="H4sIAAAAAAAC/72Z3W7bOBCF3yXXviBnhqS2QJ4kCAI3dROjTpy1XfRnse++EkV+kh050bZJkZsTaeZoDn04Iqmrfy4elw+riw8XD9v94WJxsV//bP+7+mvR/l0vLp6Wm9Xh0F25eFg/rm53y8+HD0/bzXp/v/p082m93a3bu4vRzb+/LneHnzdP681mubtafl/vL39cH0Xc3q/3q02bXkI/bra3X6Y4Pu7Wt1/2r7H/vH4t4vtkxPPHnuq62W+WH68OP55Wlx+3h8P2YfFteVjtNtu7u9Wny8/LzX51/QrBYbne7a8+L2/Xj3eXq+X+sLhfbj5Xuv39suXeH3bL9d394TfZ99uvh/v3o3/c7t6T/tvqPcdmGPnD9undhv3NuUdj/ubcw4DPo26bwe1qs9m3reDKL9zCFu56cSUtElDMSFukIFv4gkK+1uYtPEjzXSPOclxFKccF+AJxAb5IBZEKEvX59ll9hrRIFlJQzKh9OpV2cVpQrbTL9gVpvmvwWY6r11KpylNp5QvwRSqIVJDg6+5ZqU+oVKhUqFSoVKg0qyuoVipUKlQqbaWSqxIqFSoVKhUqFSqt9XUaBRRLpVoqEO7K0d1UdCg6FG8oOhRHKNVrcUQgN3A3kBupIFJBpIJIBYm7ibvdbyCgWrOhyFBkZazkKK5qM7QZ2gxt+bcpqKo0VBq+75j738PQa+g19Bp6Db21vkh9kfoScYm7nTcF1Od2qGoLjEFgDMZ3q/KA8oDygPI8LwqqygPKA8o7vl55QHlAeUB5QHlAeUBvQG9Ab9WWiMszEFSVR7RFlEeUj+9W5RHlEeUR5RHlEeUR5RHlEeUR5RHlEeUR5RHlEeUR5RHlEeU1ruurvRM7pCADBVAs45KKDp9zrSAFGSiAIiiVOdP15YpalxXUjmZBCjJQAEVQIrcpnbAyK8wKs8KsMCvMCrPCrDAbzAazwWwwW2b2BQWuRVAitym/dGUOMAeYA8yBmgPMAeYAc4A5whxhjjBHmCPMEeYIc4Q5wpzwQcIHCR8kfJDwQcIHCR80+K/Bfw3+a/Bfg/9c+3wBKchAAVQd2+DYmutzrhRkWZvPufXakFsd2+DYyieZTwpSrlnxZK1FMl+NG/iqYxsc25Q5rVSqMCvMCrNSqWbmmlsd2+DYjtkKEpCCrKwZmvJrWWauKIGasmZocGytOVBzoOZAzYGaAzUHag7UHGGOjHNknCPMEeYIc2ScI+McYU7wJSpN+CDBl/BBotIEX0NuQy0N/muopSHX5TVrKshAISOf18AVCUhBQ1wEpYyEXMkZTUEGCqA+o1t7O5AHSbfDz6h9q9Tl+pDSZGQkGyk2pBiBgcDAUwIpYUgJPCWQHEmJ/abAF2gDDAPs0xNJiVFMjGJiFBOjmMhtiGv4fZrRuHfrXy8Fxh7mawuvBbat0MoKuVyNw9XUM4QCYwev/10cnbbc3G43Xx8eb/xw7KK9kaaPXd7iZGXO0U05dhlv+Fz+cV0xcb8pcmy8fL7rMbbD2L4Y0bFF9FyTwpcjC/Jcq0szX3I9d32+68pGzrGl81yTwicwS2kCjo1N5ijtXmCWMs2FXCF32Jbl7Sio1jzeFNWaFWalZoVZqVlhNsbZGGdjnI1xNpiNcTbG2RjnPvec9WSwXl5fv6f1Zh/MvX64N3Ew+MzYp+cVp7b0nCpM2VI4m1ByldzhhOPYqqcG9ZwH9FbVCatWNMQN5xCeswkBKWcTg5F9WWl3Bg0F9W/53r4B+7qCtKwpaq6Sq+QeW/rUyJ7deG9pnbB0RUNc1aZoU7Qp2nS0Gz+1uWdX/NzmPVJ23p7duHBNX5gO+lvT4SXHuQnHuQnHuQnHHZ+pve6z8+562VOnjdDhJIeT3ISTHE5yOMkdnVm97p/zrnnZK6cOcRMOcRMOcRMOcTMcYn++YZ55HQ+e8vSzsc+mXr3nHCeclyp8p97zLAGHvudm9j03s++5mX3PPXuVe9DYtzLxKj/nYOF8U+E79XI9YR2WATLT326G0/v+6Gb2R/dsaeDpj+N5MLU0ODcj+vPNYW74ibnhOcs8P0vCm82S8y/81+fPcr17Nms8i1gB6WgR6yYWsce9+/jbx/+fXW5Gj1cyjr+WyOi7iZtYMvuChjeFmzFHPXOvn61+5mz1ZWa6GfNWeYYRZ9RsxDnmqBvNMxm9odyMd1X/LcX98ux3M95pyjOMuKE3GHGnWwRf0PBmdDN6iKc39N3Ez+wm9fuKm9FXlGcYcUbNRpyjhwz9x9FDPL3m5Xezp9f8Wndyr77D63eY+vXFj77DOL7D1C7gOLexcnrmOEdznDR64jxxnjhPnBAnxAlxQpwSp8QpcUqcEWfEGXH9tXPdOP7BNcsvni+83watb31C6xNa33BuIBNtTmhpQksbPhnLRAuSieWyTLQbmWgtMrFc1mefWnW0yZKJaS9McWWKDx9ddWJKysRy+fknT+Xz5niqydFy+fo/pwcMhEokAAA=";
    private static JsonArray oracle(){try(var input=new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(ORACLE_GZIP_BASE64)))){return JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private record Template(String name,int x,int y,int z,Map<BlockPos,BlockState> cells) { }
    private static <T extends Comparable<T>> BlockState property(BlockState state,Property<T> property,String value){return state.setValue(property,property.getValue(value).orElseThrow());}
    private static BlockState state(String text){var parts=text.split("\\[",2);var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(parts[0]));require(block!=null,"oracle vanilla block "+parts[0]);var state=block.defaultBlockState();if(parts.length==2)for(var entry:parts[1].substring(0,parts[1].length()-1).split(",")){var pair=entry.split("=",2);var property=block.getStateDefinition().getProperty(pair[0]);require(property!=null,"oracle native property "+entry);state=property(state,property,pair[1]);}return state;}
    private static List<Template> templates(){
        var hashes=List.of("dc4b95c6f0564cbe00e34d0c9b4168c88673e3223c7397258f318f535182c8cf","34163540f5c90e1ae4fe86679c2e56cb99f5ce0e5b82f601f21b0ee0ec4d30f0","68a7b44f26a9d946f33911587822f086aa04d091eb53ccf9c5a2dc865147b399","33ff9bfbef14dfcef4ae2917bb5d4e503b39c62b6f71d5bcd1a44ddc1a3bbf59","5b572748ee556a6bcdf0046f0f0710eabebf1cef745b26b44d456617e1fd7892","f319b2f35307dac210e807755bac85c8b75b920feea61816004752b04d505fc7","a31ea4fa77cf07793f145a5f613d406a87224f61599d2b48a40f11e8b2bdb1d2");
        var result=new ArrayList<Template>();int n=0;
        for(var value:oracle()){
            var row=value.getAsJsonObject();String name=row.get("name").getAsString();try(var in=BridgeBuilderChecks.class.getResourceAsStream("/data/lbc/structure/"+name+".nbt")){require(in!=null&&HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())).equals(hashes.get(n++)),"exact independently hashed source template "+name);}catch(Exception e){throw new IllegalStateException(e);}
            var palette=row.getAsJsonArray("palette").asList().stream().map(p->state(p.getAsString())).toList();var cells=new LinkedHashMap<BlockPos,BlockState>();
            for(var item:row.getAsJsonArray("cells")){var p=item.getAsJsonArray();var old=cells.put(new BlockPos(p.get(0).getAsInt(),p.get(1).getAsInt(),p.get(2).getAsInt()),palette.get(p.get(3).getAsInt()));require(old==null,"independent source cell unique");}
            var size=row.getAsJsonArray("size");result.add(new Template(name,size.get(0).getAsInt(),size.get(1).getAsInt(),size.get(2).getAsInt(),Map.copyOf(cells)));
        }
        require(result.size()==7&&result.getFirst().cells.size()==337&&result.get(1).cells.size()==45&&result.get(5).cells.size()==125&&result.get(5).cells.values().stream().filter(BlockState::isAir).count()==20,"independent seven-template/cardinality/explicitAIR oracle");return List.copyOf(result);
    }
    /** Only source-kernel fixtures install this finite draw tape; never enrolled in coordinator capture. */
    private static final class Tape extends LegacyRandomSource {
        final ArrayDeque<Float> chances=new ArrayDeque<>();final ArrayDeque<Integer> choices=new ArrayDeque<>();
        Tape(float[] supplied,int... values){super(1);for(float f:supplied)chances.add(f);for(int i:values)choices.add(i);}
        @Override public float nextFloat(){require(!chances.isEmpty(),"unexpected source predicate draw");return chances.removeFirst();}
        @Override public int nextInt(int bound){require(!choices.isEmpty(),"unexpected source integer draw bound="+bound);int value=choices.removeFirst();require(value>=0&&value<bound,"source integer remains inside exact inclusive range");return value;}
        void done(){require(chances.isEmpty()&&choices.isEmpty(),"all and only expected independent source draws consumed");}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final int kind;final ServerPlayer owner,other;final ServerLevel level;
        final LbcBridgeBuilder service;final LbcBridgeBuilder.Policy policy;final List<Template> source;
        final MatchCoreCheckpoint core;final MatchWorldCheckpoint world;final MatchWorldRandomCheckpoint random;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;final RegenerationBeltChecks.ScheduledClient client,audience;
        final Map<ServerPlayer,ExtraPlayer> extraPlayers=new LinkedHashMap<>();
        record ExtraPlayer(ServerPlayer player,ServerLevel level,Vec3 position,float yaw,float pitch,boolean noGravity,Vec3 movement,RegenerationBeltChecks.Snapshot saved,RegenerationBeltChecks.ScheduledClient client) { }
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final Set<UUID> before=new HashSet<>();
        final Vec3 origin;final int began;final boolean frozen;final int usedBefore,otherDanmakuUsed;final MatchRandomState ownerRandom,otherRandom;boolean closed,stopped,restoring;int stage,when,peer;
        Entity marker;Snowball ball;Object actor,generation;Tape tape;Map<BlockPos,BlockState> expected=new LinkedHashMap<>();
        List<Object> pendingOps;long pendingRng;LbcBridgeBuilder.View captured;Map<BlockPos,BlockState> capturedTerrain;Set<UUID> family;
        Fixture(GameController game,int kind){
            this.game=game;this.kind=kind;owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));other=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));level=owner.level();
            require(game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other)&&!game.vanity().busy()&&!game.vanity().hasSnapshot(),"actual two-player match and exclusive coordinator");
            service=game.items().bridgeBuilder();require(service.ready()&&service.count()==0&&game.items().stonekivi().queuedSnapshot().isEmpty(),"prepared exact seven templates and exclusive empty owned queue");policy=(LbcBridgeBuilder.Policy)get(service,"policy");source=templates();
            frozen=game.server().tickRateManager().isFrozen();core=new MatchCoreCheckpoint(game);world=new MatchWorldCheckpoint(game);random=new MatchWorldRandomCheckpoint(game);saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);usedBefore=owner.getStats().getValue(Stats.ITEM_USED.get(LbcBridgeBuilderItem.TOKEN));otherDanmakuUsed=other.getStats().getValue(Stats.ITEM_USED.get(LbcDanmaku.ITEM));ownerRandom=MatchRandomState.capture(owner.getRandom());otherRandom=MatchRandomState.capture(other.getRandom());
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);level.getAllEntities().forEach(e->before.add(e.getUUID()));origin=new Vec3(owner.getX(),34,owner.getZ());began=game.server().getTickCount();
        }
        String name(){return kind==0?"bridge-builder-imba":kind==1?"bridge-builder-rewind":"bridge-builder-bedwars";}
        void next(int n){stage=n;when=game.server().getTickCount();}
        void move(ServerPlayer player,Vec3 at){require(player.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual native fixture move");player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);if(player==owner)client.acknowledge();else audience.acknowledge();}
        void pause(){game.items().timeStop().begin(level,10000);stopped=true;}
        void thaw(){game.items().timeStop().end(level);stopped=false;}
        void remember(BlockPos at){terrain.putIfAbsent(at.immutable(),game.maps().snapshot(level,at));}
        void block(BlockPos at,BlockState state){require(level.hasChunkAt(at),"fixture only writes prepared resident cells");remember(at);if(level.getBlockEntity(at) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(at,state,818);}
        void begin(){
            game.server().tickRateManager().setFrozen(false);game.config().lbcSkills=false;game.config().ranked=false;
            for(var p:List.of(owner,other)){game.lbc().clear(p);p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;}
            move(owner,origin);move(other,origin.add(.1,0,.1));
            set(service,"policy",new LbcBridgeBuilder.Policy(){
                public boolean launch(LivingEntity author,ServerLevel l,Vec3 at){return policy.launch(author,l,at);}
                public boolean write(LivingEntity author,ServerLevel l,BlockPos at,MapManager.BlockSnapshot state,LbcIndirectKills.Owner cause){require(l==level,"same source world");remember(at);return policy.write(author,l,at,state,cause);}
            });
            input();if(kind==1){prepareReplay();return;}if(kind==2){bedwars();return;}
            // Independent phase7 kernel: exact source Y counter34,4 decrement vs5 physical descent.
            kernelGeneration(marker.position().add(0,0,4),7);tape=new Tape(new float[]{.999f,.999f},0,1,2,3,4,5,4);set(generation,"random",tape);
            expectedTemplate(0,BlockPos.containing(marker.position().add(-4,-10,-4)),true);
            int[] variants={1,2,3,4,5,6,5};for(int i=0;i<variants.length;i++){int variant=variants[i],offset=variant==1?-1:-2;expectedTemplate(variant,BlockPos.containing(marker.position().add(offset,-15-5*i,offset)),true);}
            var air=expected.entrySet().stream().filter(e->e.getValue().isAir()).map(Map.Entry::getKey).findFirst().orElseThrow();block(air,Blocks.CHEST.defaultBlockState());((net.minecraft.world.Container)level.getBlockEntity(air)).setItem(0,new ItemStack(Items.DIAMOND,7));
            dormantPlayers();next(0);
        }
        void input(){
            var loot=game.lbc().rollTable(level,owner.position(),owner,"lbc:ported/bridge_builder_imba");require(loot.size()==1&&loot.getFirst().is(LbcBridgeBuilderItem.TOKEN)&&loot.getFirst().getCount()==1,"actual canonical typed source item");var item=loot.getFirst();
            require(Identifier.parse("lbc:bridgebuilder").equals(item.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_a").equals(item.get(DataComponents.TOOLTIP_STYLE)),"authored model/tier");
            require(LbcBridgeBuilder.recognized(item)&&ItemData.intValue(item,"gadget",0)==1&&ItemData.intValue(item,"imba",0)==1&&ItemData.intValue(item,"most",0)==0&&ItemData.intValue(item,"projegadget",0)==0,"new IMBA is distinct from old wool-trail Most");
            owner.setYRot(90);owner.setXRot(15);other.setYRot(-90);other.setXRot(-30);owner.setItemInHand(InteractionHand.MAIN_HAND,item.copyWithCount(2));int used=owner.getStats().getValue(Stats.ITEM_USED.get(LbcBridgeBuilderItem.TOKEN));
            require(owner.gameMode.useItem(owner,level,owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"actual native SnowballItem use");
            require(owner.getMainHandItem().getCount()==1&&owner.getStats().getValue(Stats.ITEM_USED.get(LbcBridgeBuilderItem.TOKEN))==used+1,"single native debit and item statistic");
            require(service.count()==1,"one logical source marker per actual input");marker=service.snapshot().getFirst();
            require(marker.position().equals(owner.position())&&marker.getYRot()==90&&marker.getXRot()==15&&service.owner(marker)==owner,"actual thrower FEET/aim wins over nearer differently facing player");
            ball=null;for(var e:level.getAllEntities())if(!before.contains(e.getUUID())&&e instanceof Snowball shot&&!shot.isRemoved()&&shot.getOwner()==owner&&LbcBridgeBuilder.recognized(shot.getItem())){require(ball==null,"one native transport projectile");ball=shot;}
            require(ball!=null&&!ball.isRemoved()&&service.protectedCloneActor(ball),"vanilla snowball continues independently; logical marker is sole clone boundary");
            require(!service.launch(ball,owner)&&service.count()==1,"same actual projectile insertion cannot create duplicate marker");
            actor=((Map<?,?>)get(service,"actors")).get(marker.getUUID());
        }
        void dormantPlayers(){
            move(owner,origin.add(0,220,0));move(other,origin.add(12,225,0));
            // The actual BedWars fixture has four participants; every nonspectator is a source observer.
            for(var player:List.copyOf(level.players()))if(player!=owner&&player!=other&&!player.isSpectator()){
                var savedExtra=extraPlayers.computeIfAbsent(player,p->new ExtraPlayer(p,p.level(),p.position(),p.getYRot(),p.getXRot(),p.isNoGravity(),p.getDeltaMovement(),new RegenerationBeltChecks.Snapshot(game,p),new RegenerationBeltChecks.ScheduledClient(game,p)));
                var at=origin.add(3*extraPlayers.size(),230,0);require(player.teleportTo(level,at.x,at.y,at.z,Set.of(),player.getYRot(),player.getXRot(),true),"native additional participant dormancy move");player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);savedExtra.client.acknowledge();
            }
            require(service.views().getFirst().dormant(),"positive native200-block dormancy gate for all nonspectators within the same XZ map border");
        }
        void expectedTemplate(int kind,BlockPos at,boolean prefill){
            var template=source.get(kind);if(prefill)for(var p:BlockPos.betweenClosed(at,at.offset(template.x-1,template.y-1,template.z-1)))block(p,Blocks.DIRT.defaultBlockState());
            template.cells.forEach((pos,state)->expected.put(at.offset(pos),state));
        }
        Object op(String name,Object... args){try{var type=Class.forName("me.skywars2.game.LbcBridgeBuilder$"+name);var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);return ctor.newInstance(args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Scoped source node "+name,e);}}
        /** Supporting generation only. Real input/debit is checked separately; the source phase is controlled explicitly. */
        void kernelGeneration(Vec3 at,int lifetime){
            require(get(actor,"pending")==null,"no accepted generation replaced by kernel fixture");marker.setPos(at);set(actor,"lifetime",lifetime);
            try{var type=Class.forName("me.skywars2.game.LbcBridgeBuilder$Generation");var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);generation=ctor.newInstance(service,actor,at,lifetime);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            set(actor,"pending",generation);game.items().stonekivi().queuedWork((LbcStonekivi.Work)generation);
        }
        @SuppressWarnings("unchecked") ArrayDeque<Object> ops(){return (ArrayDeque<Object>)get(generation,"ops");}
        boolean done(){return (boolean)get(generation,"closed");}
        void verifyExpected(){for(var entry:expected.entrySet())require(level.getBlockState(entry.getKey()).equals(entry.getValue()),"source oracle actual terrain "+entry.getKey()+" expected="+entry.getValue()+" actual="+level.getBlockState(entry.getKey()));}
        void supportKernel(){
            expected.clear();var at=BlockPos.containing(origin.add(20,0,0));
            for(var p:List.of(at.west(),at,at.east(),at.north(),at.south())){block(p,Blocks.STONE.defaultBlockState());block(p.below(),Blocks.DIRT.defaultBlockState());}
            block(at.north(),Blocks.QUARTZ_PILLAR.defaultBlockState());block(at.south(),Blocks.QUARTZ_PILLAR.defaultBlockState());
            kernelGeneration(marker.position(),1);ops().clear();ops().add(op("Support",at,0,false));
            game.items().stonekivi().queuedWork(new LbcStonekivi.Work(){public boolean step(){peer++;return true;}});next(2);
        }
        void decorationKernel(){
            kernelGeneration(marker.position(),1);ops().clear();ops().add(op("Branches",0));
            // Source:50% entry, positive8 lateral, vertical0, first90% rise accepted then rejected,
            // remaining four90% rejected, side5% rejected. Exact draw count is independent source data.
            tape=new Tape(new float[]{0,0,.999f,.999f,.999f,.999f,.999f,.999f},0,0,20);set(generation,"random",tape);
            expected.clear();var base=marker.position().add(0,0,8);var starts=List.of(base.add(1,0,0),base.add(0,2,1),base.add(-1,4,0),base.add(0,2,-1),base.add(0,-5,0));
            for(int i=0;i<starts.size();i++){var p=BlockPos.containing(starts.get(i));expected.put(p.below(),Blocks.QUARTZ_PILLAR.defaultBlockState());expected.put(p, i==0?Blocks.QUARTZ_PILLAR.defaultBlockState():Blocks.SEA_LANTERN.defaultBlockState());if(i==0)expected.put(p.above(),Blocks.SEA_LANTERN.defaultBlockState());}
            expected.keySet().forEach(p->block(p,Blocks.STONE.defaultBlockState()));next(3);
        }
        int axis;Map<BlockPos,BlockState> sideExpected;Vec3 sideOrigin;
        Vec3 sourceLocal(Vec3 at,int yaw,double left,double up,double forwards){
            // Independent source command-coordinate resolver, not the service's private local() helper.
            var context=game.server().createCommandSourceStack().withLevel(level).withPosition(at).withRotation(new net.minecraft.world.phys.Vec2(0,yaw));
            return new net.minecraft.commands.arguments.coordinates.LocalCoordinates(left,up,forwards).getPosition(context);
        }
        void axisKernel(int yaw){
            axis=yaw;sideOrigin=origin.add(40,12,0);kernelGeneration(sideOrigin,1);ops().clear();ops().add(op("Side",sideOrigin,yaw,0,0));ops().add(op("Side",sideOrigin,yaw+180,0,0));
            // Source checks current-position radius AFTER each deck. Resolve its exact floating
            // command coordinates, including the first body after an admitted <=200 continuation.
            var draws=new ArrayList<Float>();sideExpected=new LinkedHashMap<>();
            for(int direction:List.of(yaw,yaw+180)){
                var at=sideOrigin;int count=0;boolean again;
                do{
                var center=BlockPos.containing(at);draws.add(.999f);if(count++>0)draws.add(.999f);
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)sideExpected.put(center.offset(x,0,z),Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)sideExpected.put(center.offset(x,-1,z),Blocks.QUARTZ_BRICKS.defaultBlockState());
                for(int a:List.of(-2,2))for(int b:List.of(-1,1))sideExpected.put(BlockPos.containing(sourceLocal(at,direction,a,1,b)),Blocks.QUARTZ_BRICKS.defaultBlockState());
                again=at.distanceToSqr(sideOrigin)<=40000;at=sourceLocal(at,direction,0,0,5);
                }while(again);
                require(count>=41,"independent source loop reaches the full200 radius");
            }
            float[] values=new float[draws.size()];for(int i=0;i<values.length;i++)values[i]=draws.get(i);tape=new Tape(values);set(generation,"random",tape);
            // Expected source cells may span unresident chunks; only native manager tickets prepare them.
            next(yaw==0?4:5);
        }
        void unloadedKernel(){
            int cx=60000,cz=60000;while(level.getChunkSource().getChunkNow(cx,cz)!=null)cx+=128;
            var at=new BlockPos(cx*16+8,34,cz*16+8);kernelGeneration(marker.position(),1);ops().clear();ops().add(op("Emit",at,new MapManager.BlockSnapshot(Blocks.QUARTZ_BRICKS.defaultBlockState(),null)));
            ((LbcStonekivi.Work)generation).step();require(!done()&&ops().size()==1&&level.getChunkSource().getChunkNow(cx,cz)==null,"unresident accepted native cell retained and chunk requested without synchronous load");
            expected.clear();expected.put(at,Blocks.QUARTZ_BRICKS.defaultBlockState());peer=0;game.items().stonekivi().queuedWork(new LbcStonekivi.Work(){public boolean step(){peer++;return true;}});next(6);
        }
        void selectClone(int id){
            String key=id==15?"lbc:true_advancements/bows/double":"lbc:true_advancements/hoes/sickle";game.config().lbcSkills=true;game.config().ranked=false;game.items().mana().award(owner,key);game.menus().openDebug(owner);owner.entityTags().remove("skywars2.debug_perks");require(game.canUseMenus(owner)&&!game.debugPerks(owner),"actual owned skill selector GUI without bypass");require(game.lbc().select(owner,LbcLoadouts.Kind.SKILLS,10000,id-1)&&game.lbc().hasSkill(owner,id),"actual source15/17 clone selection");owner.closeContainer();
        }
        void cloneStart(int id){
            service.stop();if(ball!=null)ball.discard();game.items().stonekivi().tick();move(owner,origin);move(other,origin.add(12,0,0));selectClone(id);input();next(id==15?10:11);
        }
        BlockPos beacon;MapManager.BlockSnapshot beaconBefore;int fieldKind;Snowball friendly;BlockPos bombRemainder;
        void removeBeacon(){if(beacon==null)return;game.maps().restoreSnapshot(level,beacon,beaconBefore,818);game.items().magic().blockChanged(beacon,level.getBlockState(beacon));beacon=null;}
        long noBranchSeed(){for(long seed=1;seed<100000;seed++){var body=new java.util.Random(seed);boolean good=true;for(int n=0;n<3;n++){var r=new java.util.Random(body.nextLong());if(r.nextFloat()<.5f||r.nextFloat()<.05f){good=false;break;}}if(good)return seed;}throw new IllegalStateException("No finite no-branch fixture seed");}
        void suppression(int index){
            service.stop();if(ball!=null)ball.discard();game.items().stonekivi().tick();game.lbc().clear(owner);game.config().lbcSkills=false;move(owner,origin);move(other,origin.add(12,0,0));
            fieldKind=index;beacon=BlockPos.containing(origin).below(2);beaconBefore=game.maps().snapshot(level,beacon);block(beacon,(index==0?LbcMagic.MAGIC_BEACON_BLOCK:LbcMagic.MECHANISM_BEACON_BLOCK).defaultBlockState());game.items().magic().blockChanged(beacon,level.getBlockState(beacon));
            require(index==0?game.items().magic().suppressed(owner):game.items().magic().mechanismSuppressed(owner),"actual native block beacon positive control");
            if(index==1){
                var item=LbcBridgeBuilderItem.TOKEN.getDefaultInstance();owner.setItemInHand(InteractionHand.MAIN_HAND,item);
                require(!item.getItem().use(level,owner,InteractionHand.MAIN_HAND).consumesAction()&&item.getCount()==1&&service.count()==0,"TECH rejects native launch without consumption");
                removeBeacon();input();marker.getRandom().setSeed(noBranchSeed());
                beacon=BlockPos.containing(origin).below(2);beaconBefore=game.maps().snapshot(level,beacon);block(beacon,LbcMagic.MECHANISM_BEACON_BLOCK.defaultBlockState());game.items().magic().blockChanged(beacon,level.getBlockState(beacon));next(16);
            }else{input();marker.getRandom().setSeed(noBranchSeed());next(12);}
        }
        void bomb(){
            removeBeacon();require(game.items().danmaku().count()==0,"exclusive native bomb positive control");
            kernelGeneration(marker.position(),1);ops().clear();bombRemainder=BlockPos.containing(origin.add(20,3,0));ops().add(op("Emit",bombRemainder,new MapManager.BlockSnapshot(Blocks.QUARTZ_PILLAR.defaultBlockState(),null)));
            move(other,marker.position());other.setShiftKeyDown(true);other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcDanmaku.ITEM));game.items().mana().set(other,0);game.items().mana().score(other,"danmaku_score").set(50);
            require(other.gameMode.useItem(other,level,other.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&game.items().danmaku().bombs().size()==1,"actual hostile native Danmaku staff bomb input");
            // A controlled source bomb age avoids waiting while the bridge moves; next real tick applies age20.
            game.items().danmaku().bombs().getFirst().age=16;friendly=new Snowball(EntityTypes.SNOWBALL,level);friendly.setOwner(other);friendly.setItem(new ItemStack(Items.SNOWBALL));friendly.setPos(marker.position());friendly.setNoGravity(true);friendly.setDeltaMovement(Vec3.ZERO);require(level.addFreshEntity(friendly),"actual bomb-owner projectile positive control");next(13);
        }
        int dormantAge;Vec3 dormantAt;
        boolean ordinaryStep(int elapsed){
            switch(stage){
                case 0->{if(!done()){require(elapsed<100,"finite source main/seven column generations drain");return false;}tape.done();verifyExpected();
                    var main=BlockPos.containing(marker.position().add(-4,-10,-4));require(!source.getFirst().cells.containsKey(BlockPos.ZERO)&&level.getBlockState(main).is(Blocks.DIRT),"source sparse template does not clear unlisted cells");
                    for(var e:level.getAllEntities())require(before.contains(e.getUUID())||!(e instanceof ItemEntity),"quiet source placement/carving emits no block/container loot");
                    dormantAt=marker.position();set(actor,"lifetime",10000);dormantAge=10000;next(1);
                }
                case 1->{var view=service.views().getFirst();require(view.dormant()&&!view.pending()&&view.position().equals(dormantAt)&&view.lifetime()>dormantAge,"actual dormant ticks increment source Lifetime without movement or invented lifetime expiry");if(elapsed<4)return false;supportKernel();}
                case 2->{if(!done()){require(elapsed<10,"source fill-success support quantum drains");return false;}var at=BlockPos.containing(origin.add(20,0,0));require((long)get(generation,"writes")==3&&peer==1,"X-arm writes commit before Z-success decision and shared peer progresses");for(var p:List.of(at.west(),at,at.east()))require(level.getBlockState(p).is(Blocks.QUARTZ_PILLAR)&&level.getBlockState(p.below()).is(Blocks.DIRT),"second Z-fill success0 stops descent despite old centerSTONE");decorationKernel();}
                case 3->{if(!done()){require(elapsed<15,"source decorative recursive paths drain");return false;}tape.done();verifyExpected();require((long)get(generation,"writes")==12,"five source decorative starts plus one accepted90percent rise");axisKernel(0);}
                case 4,5->{if(!done()){require(elapsed<180,"complete native two-sided source200 deck loops drain axis="+axis);return false;}tape.done();for(var entry:sideExpected.entrySet())require(level.getBlockState(entry.getKey()).equals(entry.getValue()),"all source side deck/lower/rail cells axis="+axis+" at="+entry.getKey());
                    if(axis==0)axisKernel(90);else unloadedKernel();}
                case 6->{require(peer==1,"unloaded accepted work yields to peer instead of monopolizing queue");if(!done()){require(elapsed<100,"native tickets eventually publish accepted cell");return false;}verifyExpected();cloneStart(15);}
                case 10,11->{int id=stage==10?15:17,count=id==15?2:4;if(service.count()<count){require(elapsed<12,"registered clone aura sees native logical marker");return false;}require(service.count()==count,"exact actual x2/x4 logical family, transport not multiplied");
                    for(var e:service.snapshot())require(service.owner(e)==owner&&e.entityTags().contains(id==15?"doubled":"quadried"),"cloned logical owner/copy-once state");
                    if(service.views().stream().anyMatch(v->v.lifetime()==0)){require(elapsed<12,"every actual cloned logical actor receives registered native ticks");return false;}
                    long transports=0;for(var e:level.getAllEntities())if(e instanceof Snowball shot&&!shot.isRemoved()&&shot.getOwner()==owner&&LbcBridgeBuilder.recognized(shot.getItem()))transports++;require(transports<=1,"native transport is not a second logical clone producer");
                    if(id==15)cloneStart(17);else suppression(0);}
                case 12->{
                    require(service.count()==1&&!marker.isRemoved(),"mechanical source actor survives native MAG erase ticks");require(game.items().magic().suppressed(owner),"native MAG beacon remains active during source generation");
                    if(marker.position().distanceToSqr(origin)<=16.1){require(elapsed<15,"source keeps moving/generating under actual active beacon");return false;}
                    require(service.views().getFirst().lifetime()>=2,"registered source Lifetime and future generations progress under native field");
                    removeBeacon();if(fieldKind==0)suppression(1);else{
                        // The final generation may still be accepted; wait for it at this exact pose before bomb fixture.
                        dormantPlayers();next(14);
                    }
                }
                case 14->{if(service.views().getFirst().pending()){require(elapsed<30,"last native beacon generation drains before hostile bomb fixture");return false;}bomb();}
                case 13->{require(marker.isRemoved()&&!friendly.isRemoved(),"actual native hostile bomb clears logical marker while preserving caster-owned projectile");require(done()&&level.getBlockState(bombRemainder).is(Blocks.QUARTZ_PILLAR),"bomb removal still drains already accepted source cell");next(15);}
                // ENTITY_UNLOAD queues retirement for the next native GameController tick.
                case 15->{require(marker.isRemoved()&&service.count()==0&&!friendly.isRemoved(),"canonical queued unload retires bomb-cleared producer while preserving caster-owned projectile");game.items().danmaku().forget(other);other.setShiftKeyDown(false);return true;}
                case 16->{require(marker.isRemoved(),"TECH removes an already launched mechanical bridge producer");if(service.count()!=0){require(elapsed<8,"queued native unload retires TECH producer");return false;}removeBeacon();game.items().stonekivi().tick();if(ball!=null)ball.discard();input();marker.getRandom().setSeed(noBranchSeed());dormantPlayers();next(14);}
                default->throw new IllegalStateException("Bridge ordinary stage "+stage);
            }return false;
        }
        Vec3 firstMove;int replayRound;boolean futureAdvanced;List<BlockPos> beds;Map<BlockPos,MapManager.BlockSnapshot> protectedImages;BlockPos admitted;
        /** Independent java.util.Random reference for the native Legacy source stream, only to choose a finite fixture seed. */
        long heavySeed(){for(long seed=1;seed<100000;seed++){var body=new java.util.Random(seed);var source=new java.util.Random(body.nextLong());if(source.nextFloat()>=.5f&&source.nextFloat()<.05f)return seed;}throw new IllegalStateException("No finite native fixture seed");}
        void prepareReplay(){marker.getRandom().setSeed(heavySeed());firstMove=marker.position().add(Vec3.directionFromRotation(marker.getXRot(),marker.getYRot()).scale(4));replayRound=0;next(50);}
        void capture(){
            @SuppressWarnings("unchecked") var active=(Map<UUID,Object>)get(service,"actors");if(replayRound==0){actor=active.get(marker.getUUID());generation=get(actor,"pending");captured=service.views().getFirst();}
            require(generation!=null&&!done(),"real accepted source generation has nonempty work");pendingOps=List.copyOf(ops());require(!pendingOps.isEmpty(),"actual partial immutable operation deque");
            var rng=(net.minecraft.util.RandomSource)get(generation,"random");var image=MatchRandomState.capture(rng);pendingRng=rng.nextLong();image.restore(rng);
            capturedTerrain=new LinkedHashMap<>();for(var at:terrain.keySet())capturedTerrain.put(at,level.getBlockState(at));pause();require(game.vanity().requestCapture(owner,false),"actual whole coordinator captures source native partial work");next(51);
        }
        void verifyReplay(){
            require(List.copyOf(ops()).equals(pendingOps),"typed queued generation restores exact source operation order/cursors");var rng=(net.minecraft.util.RandomSource)get(generation,"random");var image=MatchRandomState.capture(rng);require(rng.nextLong()==pendingRng,"native private generation next RNG draw restored");image.restore(rng);
            if(replayRound==0){require(service.count()==1&&service.views().getFirst().equals(captured),"whole service restores native marker/Lifetime/dormancy/pending/write counters");marker=service.snapshot().getFirst();require(service.owner(marker)==owner,"current native causal thrower rebound");}
            else require(service.count()==0&&level.getEntity(marker.getUUID())==null,"rewind accepted work without resurrecting removed/bomb-cleared logical marker");
            for(var entry:capturedTerrain.entrySet())require(level.getBlockState(entry.getKey()).equals(entry.getValue()),"whole coordinator restores exact painted prefix");
            for(var entry:terrain.entrySet())if(!capturedTerrain.containsKey(entry.getKey()))require(level.getBlockState(entry.getKey()).equals(entry.getValue().state()),"whole coordinator removes future-only native terrain");
        }
        boolean rewindStep(int elapsed){
            switch(stage){
                case 50->{
                    require(marker.position().distanceToSqr(firstMove)<1e-9,"actual first scheduled source tick moves local-forward4 using causal thrower aim");
                    var current=((Map<?,?>)get(service,"actors")).get(marker.getUUID());generation=current==null?null:get(current,"pending");require(generation!=null&&!done(),"finite seeded source side branch leaves real bounded accepted backlog");dormantPlayers();capture();
                }
                case 51->{
                    require(List.copyOf(ops()).equals(pendingOps),"actual TimeStop pauses accepted private operation cursor");if(replayRound==0)require(service.views().getFirst().equals(captured),"actual TimeStop pauses source Lifetime/marker and writes");
                    if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;thaw();next(52);
                }
                case 52->{
                    if(List.copyOf(ops()).equals(pendingOps)){require(elapsed<10,"registered queue advances accepted source work after thaw");return false;}
                    require(!done(),"fixture restores a truly partial generation, not an empty finished queue");pause();require(game.vanity().requestRestoreForCheck(),"whole coordinator restores real future terrain/RNG/private branch cursor");restoring=true;next(53);
                }
                case 53->{
                    if(game.vanity().busy())return false;verifyReplay();require(game.items().timeStop().stopped(level),"full source pause boundary restored");game.vanity().stop();
                    if(replayRound==0){
                        // Source removal/bomb clearing retires production, but accepted terrain still owns its remainder.
                        marker.discard();require(marker.isRemoved()&&!done(),"actual marker removal retains accepted source generation");replayRound=1;next(55);
                    }else{thaw();next(54);}
                }
                case 55->{
                    // Keep the accepted cursor frozen across the real queued native unload boundary.
                    require(game.items().timeStop().stopped(level)&&!done()&&List.copyOf(ops()).equals(pendingOps),"native unload boundary preserves TimeStop-paused accepted source cursor");
                    require(marker.isRemoved()&&service.count()==0,"canonical queued unload retires removed producer before whole capture");capture();
                }
                case 54->{if(!done()){require(elapsed<300,"all accepted source branches drain after marker removal and rewind");return false;}require(service.count()==0,"completed removed-marker generation does not restart producer");return true;}
                default->throw new IllegalStateException("Bridge rewind stage "+stage);
            }return false;
        }
        @SuppressWarnings("unchecked") void bedwars(){
            var arena=game.bedwars().arena();require(game.config().gameMode==3&&arena.active()&&arena.topology().level()==level,"actual initialized ordinary BedWars3 topology");require(!arena.topology().beds().isEmpty(),"actual protected native beds");
            protectedImages=new LinkedHashMap<>();var bed=arena.topology().beds().getFirst();beds=List.of(bed.foot(),bed.head());for(var at:beds)protectedImages.put(at,game.maps().snapshot(level,at));
            var original=(Map<Long,net.minecraft.world.level.block.Block>)get(arena,"original");for(var entry:original.entrySet()){
                var at=BlockPos.of(entry.getKey());if(level.hasChunkAt(at)&&!beds.contains(at)&&!arena.allowsType(level,at,Blocks.QUARTZ_BRICKS.defaultBlockState())){protectedImages.put(at,game.maps().snapshot(level,at));break;}
            }
            require(protectedImages.size()>2,"actual original nonbed terrain exists beside protected bed halves");
            admitted=BlockPos.containing(origin.add(5,0,0));require(arena.originalTypeAt(level,admitted)==null&&level.getBlockState(admitted).isAir(),"positive source added cell was originally air in the actual arena");
            kernelGeneration(marker.position(),1);ops().clear();for(var at:protectedImages.keySet())ops().add(op("Emit",at,new MapManager.BlockSnapshot(Blocks.QUARTZ_BRICKS.defaultBlockState(),null)));ops().add(op("Emit",admitted,new MapManager.BlockSnapshot(Blocks.QUARTZ_BRICKS.defaultBlockState(),null)));dormantPlayers();next(70);
        }
        boolean bedwarsStep(int elapsed){
            if(!done()){require(elapsed<30,"native protected/admitted writes finish as one source generation");return false;}
            require((long)get(generation,"writes")==1&&level.getBlockState(admitted).is(Blocks.QUARTZ_BRICKS),"actual canonical BedWars allows added terrain while rejecting originals");
            for(var entry:protectedImages.entrySet()){var now=game.maps().snapshot(level,entry.getKey());require(now.state().equals(entry.getValue().state())&&Objects.equals(now.entity(),entry.getValue().entity()),"original terrain/bed state and complete container data preserved");}
            service.stop();if(ball!=null)ball.discard();game.items().stonekivi().tick();move(owner,origin);game.spectators().enter(owner);var held=new ItemStack(LbcBridgeBuilderItem.TOKEN,2);owner.setItemInHand(InteractionHand.MAIN_HAND,held);
            try{owner.gameMode.useItem(owner,level,held,InteractionHand.MAIN_HAND);require(service.count()==0&&held.getCount()==2,"actual observer native item admission produces no marker or debit");}
            finally{game.spectators().remove(owner);owner.setGameMode(GameType.SURVIVAL);}return true;
        }
        boolean step(){
            client.acknowledge();audience.acknowledge();for(var extra:extraPlayers.values())extra.client.acknowledge();require(game.server().getTickCount()-began<1400,"bounded source fixture "+name()+" stage="+stage);int elapsed=game.server().getTickCount()-when;
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            return kind==0?ordinaryStep(elapsed):kind==1?rewindStep(elapsed):bedwarsStep(elapsed);
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->{if(kind==1)game.vanity().stop();},()->{removeBeacon();if(kind==0)game.items().danmaku().forget(other);service.stop();if(stopped){game.items().timeStop().end(level);stopped=false;}game.items().stonekivi().stop();},
                    ()->{var entities=new ArrayList<Entity>();for(var e:level.getAllEntities())entities.add(e);for(var e:entities)if(!before.contains(e.getUUID())&&!(e instanceof ServerPlayer)){if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}},
                    ()->{for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),818);},
                    ()->{game.server().tickRateManager().setFrozen(true);core.restoreFrozen();world.restoreFrozen();random.restoreFrozen();},
                    ()->{saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();ownerRandom.restore(owner.getRandom());otherRandom.restore(other.getRandom());owner.getStats().setValue(owner,Stats.ITEM_USED.get(LbcBridgeBuilderItem.TOKEN),usedBefore);other.getStats().setValue(other,Stats.ITEM_USED.get(LbcDanmaku.ITEM),otherDanmakuUsed);},
                    ()->{for(var extra:extraPlayers.values()){extra.saved.restoreAttributes();extra.saved.restore(game);extra.saved.restoreUse();require(extra.player.teleportTo(extra.level,extra.position.x,extra.position.y,extra.position.z,Set.of(),extra.yaw,extra.pitch,true),"native additional participant exact position restore");extra.client.acknowledge();extra.player.setNoGravity(extra.noGravity);extra.player.setDeltaMovement(extra.movement);}},
                    ()->{for(var extra:extraPlayers.values())extra.client.close();},
                    ()->{set(service,"policy",policy);game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);game.items().magic().flushCleansing();},client::close,audience::close,()->game.server().tickRateManager().setFrozen(frozen)))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Bridge Builder scoped native restoration",failure);
        }
    }
}
