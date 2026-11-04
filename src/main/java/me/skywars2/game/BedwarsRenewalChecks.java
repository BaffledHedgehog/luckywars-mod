package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.minecraft.world.scores.PlayerTeam;

/** Disposable actual mode3 match: accepted native Supernova owns the complete renewal pipeline. */
public final class BedwarsRenewalChecks {
    private BedwarsRenewalChecks() { }
    private static Run running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("BedWars renewal: "+why);}
    public static void start(GameController game){
        Run r=null;
        try{require(running==null,"exclusive scene");r=new Run(game);r.init();running=r;SkyWars2.LOGGER.info("[check] START bedwars-renewal");}
        catch(Throwable error){if(r!=null)try{r.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL bedwars-renewal",error);}
    }
    public static void tick(GameController game){
        if(running==null)return;var r=running;
        try{if(r.step()){running=null;r.close();SkyWars2.LOGGER.info("[check] PASS bedwars-renewal (actual four-player/two-team mode3/native accepted Supernova, same epoch/inventory/XP/lives/team objects/shared54-slot root/beacon, lost bed and eliminated member retained, source logical bed color/new generators, native TimeStop)");}}
        catch(Throwable error){running=null;try{r.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL bedwars-renewal",error);}
    }
    private static ServerPlayer named(GameController game,String name){return Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name)),name);}
    private record PlayerImage(ServerPlayer body,int team,PlayerTeam nativeTeam,int lives,int level,float progress,int total,List<ItemStack> inventory,boolean observer) {
        PlayerImage(GameController game,ServerPlayer p){this(p,game.bedwars().team(p),p.getTeam(),game.lives(p.getUUID()),p.experienceLevel,p.experienceProgress,p.totalExperience,stacks(p),game.isObserver(p));}
        private static List<ItemStack> stacks(ServerPlayer p){var out=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)out.add(p.getInventory().getItem(i).copy());return List.copyOf(out);}
    }
    private static final class Run implements AutoCloseable {
        final GameController game;final LbcBedwarsAuxiliaries aux;final List<UUID> ids=new ArrayList<>();final String epoch;final boolean oldBorders;
        final Map<UUID,PlayerImage> images=new LinkedHashMap<>();final Map<Integer,PlayerTeam> teams=new LinkedHashMap<>();
        final List<RegenerationBeltChecks.ScheduledClient> clients=new ArrayList<>();final Set<UUID> oldGenerators=new HashSet<>();
        final BlockPos oldChest=new BlockPos(0,82,0);final int start;int stage,when,sequence,teamA,teamB,pauseClock,pauseBeacon,transitionBeacon;
        UUID event,oldDisplay;BlockEntity oldChestBody;LbcBedwarsAuxiliaries.SharedContainer root;boolean closed,sawTransition,sawMap;
        Run(GameController game){
            this.game=game;require(game.playing()&&game.config().gameMode==3&&game.config().mapId.equals("99991b")&&game.bedwars().active()&&game.bedwars().arena().active(),"fresh genuine local99991b mode3 match");
            require(!game.vanity().busy()&&!game.supernovaMapTransition()&&game.items().supernova().phase()==LbcSupernova.Phase.IDLE,"no unrelated accepted world transition");
            for(var name:List.of("TestAlpha","TestGamma","TestBeta","TestDelta")){var p=named(game,name);require(p.isAlive()&&!game.isObserver(p)&&game.canUseGameplayItems(p),"four actual active native participants");ids.add(p.getUUID());}
            var a=p(0);var c=p(1);var b=p(2);var d=p(3);teamA=game.bedwars().team(a);teamB=game.bedwars().team(b);
            require(teamA!=teamB&&teamA>0&&teamB>0&&game.bedwars().team(c)==teamA&&game.bedwars().team(d)==teamB&&a.getTeam()==c.getTeam()&&b.getTeam()==d.getTeam(),"actual Alpha/Gamma and Beta/Delta native teams");
            require(game.bedwars().arena().topology().beds().size()==2&&game.bedwars().hasBed(a)&&game.bedwars().hasBed(b)&&game.lives(d.getUUID())==1,"two real beds and source final-life elimination control");
            aux=game.bedwars().auxiliaries();epoch=game.items().epoch();oldBorders=game.config().noBorders;start=when=game.server().getTickCount();
        }
        ServerPlayer p(int n){return Objects.requireNonNull(game.server().getPlayerList().getPlayer(ids.get(n)),"retained actual player");}
        void move(ServerPlayer p,Vec3 at){p.teleportTo(game.maps().level(),at.x,at.y,at.z,Set.of(),0,0,true);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
        void init(){
            game.config().noBorders=true;
            var bed=game.bedwars().arena().topology().beds().stream().filter(b->b.team()==teamB).findFirst().orElseThrow();
            BedwarsProtectionChecks.enemyBed(game,p(0),bed);require(!game.bedwars().hasBed(p(2))&&!game.bedwars().hasBed(p(3)),"actual source bed right permanently lost before renewal");
            var d=p(3);d.setInvulnerableTime(0);d.damageCooldownTime=0;d.removeAllEffects();d.setHealth(d.getMaxHealth());
            require(d.hurtServer(d.level(),d.damageSources().playerAttack(p(0)),10000)&&d.isDeadOrDying()&&game.lives(d.getUUID())==0,"genuine final Delta elimination while Beta keeps second team alive");
            d.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));p(3).connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            require(p(3)!=d&&game.isObserver(p(3))&&game.lives(ids.get(3))==0,"native replacement is an actual eliminated observer");
            for(int n=0;n<4;n++){clients.add(new RegenerationBeltChecks.ScheduledClient(game,p(n)));p(n).closeContainer();p(n).inventoryMenu.getCraftSlots().clearContent();}
            require(game.maps().level().getBlockState(oldChest.below()).is(Blocks.STONE)&&game.maps().level().getBlockState(oldChest).isAir(),"actual local original support and unprotected chest air");
            require(game.bedwars().arena().topology().bounds().stream().anyMatch(b->b.contains(oldChest)),"physical chest lies inside actual old arena renewal footprint");
            move(p(0),new Vec3(.5,82,-2));clients.getFirst().acknowledge();p(0).setItemInHand(InteractionHand.MAIN_HAND,aux.teamChest(p(0)));
            var hit=new BlockHitResult(new Vec3(.5,82,.5),Direction.UP,oldChest.below(),false);
            p(0).connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,hit,++sequence));p(0).connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
            require(aux.chest(p(0).level(),oldChest)&&p(0).getMainHandItem().isEmpty(),"actual native marked chest placement/debit");
            oldChestBody=p(0).level().getBlockEntity(oldChest);require(oldChestBody!=null,"old physical native chest block entity");
            for(var body:p(0).level().getEntitiesOfClass(BlockDisplay.class,new AABB(oldChest).inflate(2),e->e.entityTags().contains("skywars2.bedwars.chest_visual")))oldDisplay=body.getUUID();
            require(oldDisplay!=null,"old native team-color child display");root=aux.container(teamA);root.setItem(7,new ItemStack(Items.DIAMOND,17));aux.beacon(p(0),LbcBedwarsShop.Beacon.HASTE2,600);
            for(var g:aux.generators())oldGenerators.add(g.id());require(!oldGenerators.isEmpty(),"real old source generators indexed");
            for(int n=0;n<4;n++){
                var player=p(n);move(player,new Vec3(1024.5+n*32,1000,1024.5));clients.get(n).acknowledge();
                if(n<3){player.setExperienceLevels(17+n);player.setExperiencePoints(7+n);var stack=new ItemStack(n==0?Items.DIAMOND:n==1?Items.EMERALD:Items.LAPIS_LAZULI,7+n);stack.set(DataComponents.CUSTOM_NAME,Component.literal("BedWars renewal retained "+n));player.getInventory().setItem(8,stack);}
            }
            var katana=loot("shit_katana");var gun=loot("shishgun");p(0).inventoryMenu.getCraftSlots().setItem(0,katana);p(0).inventoryMenu.getCraftSlots().setItem(1,gun);game.items().supernova().craftChanged(p(0));
            begin(0);
        }
        ItemStack loot(String leaf){var stacks=game.lbc().rollTable(p(0).level(),p(0).position(),p(0),"lbc:ported/"+leaf);require(stacks.size()==1,"canonical actual source "+leaf);return stacks.getFirst().copyWithCount(1);}
        void begin(int phase){stage=phase;when=game.server().getTickCount();}
        boolean step(){
            int now=game.server().getTickCount();require(now-start<30000,"bounded accepted source/async512-write renewal: stage="+stage+", event="+game.items().supernova().status()+", map="+game.config().mapId+", busy="+game.maps().busy());
            for(var client:clients)client.acknowledge();var service=game.items().supernova();
            if(stage==0){
                if(service.phase()==LbcSupernova.Phase.IDLE){require(now-when<100,"actual source crafting admission");return false;}
                require(p(0).inventoryMenu.getCraftSlots().getItem(0).isEmpty()&&p(0).inventoryMenu.getCraftSlots().getItem(1).isEmpty(),"one exact source Katana/Gun pair consumed once");
                event=service.eventId();require(event!=null&&service.owner().equals(ids.getFirst()),"real registered Supernova event owns map transition");
                for(int n=0;n<4;n++)images.put(ids.get(n),new PlayerImage(game,p(n)));
                teams.put(teamA,game.server().getScoreboard().getPlayerTeam(Integer.toString(teamA)));teams.put(teamB,game.server().getScoreboard().getPlayerTeam(Integer.toString(teamB)));
                require(teams.get(teamA)!=null&&teams.get(teamB)!=null,"actual original native team objects");begin(1);return false;
            }
            if(stage==1){
                if(!game.supernovaMapTransition())return false;sawTransition=true;transitionBeacon=aux.beaconSeconds(teamA,LbcBedwarsShop.Beacon.HASTE2);
                require(transitionBeacon>0&&root==aux.container(teamA),"accepted event retains existing beacon/root before map cycle");
                pauseClock=service.activeTicks();pauseBeacon=transitionBeacon;game.items().timeStop().begin(game.maps().level(),1000);begin(2);return false;
            }
            if(stage==2){
                require(game.supernovaMapTransition()&&Objects.equals(service.eventId(),event)&&service.activeTicks()==pauseClock&&aux.beaconSeconds(teamA,LbcBedwarsShop.Beacon.HASTE2)==pauseBeacon,"native TimeStop pauses actual accepted map-cycle clock and canonical beacon");
                require(game.config().mapId.equals("99991b"),"paused native map cycle cannot publish replacement");
                if(now-when<25)return false;game.items().timeStop().end(game.maps().level());begin(3);return false;
            }
            if(stage==3){
                if(service.phase()==LbcSupernova.Phase.MAP||game.maps().busy())sawMap=true;
                if(service.phase()!=LbcSupernova.Phase.IDLE||game.supernovaMapTransition()||game.maps().busy()||!game.bedwars().startReady())return false;
                verify();return true;
            }
            return false;
        }
        void verify(){
            require(sawTransition,"actual source map intent observed before native publication");
            require(game.playing()&&game.config().gameMode==3&&game.bedwars().active()&&game.bedwars().arena().active()&&epoch.equals(game.items().epoch()),"same actual mode3 match/item epoch after complete native renewal");
            require(game.maps().catalog(true).stream().anyMatch(m->m.id().equals(game.config().mapId)&&m.available()),"any actual eligible BedWars source/local destination accepted");
            for(var entry:teams.entrySet())require(game.server().getScoreboard().getPlayerTeam(Integer.toString(entry.getKey()))==entry.getValue(),"native team object identity retained");
            for(int n=0;n<4;n++){
                var current=p(n);var before=images.get(ids.get(n));require(current==before.body&&game.lives(ids.get(n))==before.lives&&game.bedwars().team(current)==before.team&&current.getTeam()==before.nativeTeam&&game.isObserver(current)==before.observer,"same body/team/lives/role without eliminated revival "+n);
                require(current.experienceLevel==before.level&&current.experienceProgress==before.progress&&current.totalExperience==before.total,"all native XP fields retained "+n);
                int balance=0;for(int slot=0;slot<before.inventory.size();slot++){var previous=before.inventory.get(slot);var actual=current.getInventory().getItem(slot);if(previous.isEmpty()&&n==0&&LbcBalanceItems.isBalance(actual)){balance+=actual.getCount();continue;}require(ItemStack.matches(previous,actual),"every native inventory slot retained without new kit/grant "+n+":"+slot);}
                if(n==0)require(balance==1,"exactly the actual source owner's canonical Balance reward");
                require(!game.awaitingRespawn(ids.get(n)),"renewal invents no player respawn "+n);
            }
            require(game.lives(ids.get(3))==0&&game.isObserver(p(3))&&!game.canUseGameplayItems(p(3)),"actual eliminated Delta remains eliminated");
            require(game.bedwars().hasBed(p(0))&&game.bedwars().hasBed(p(1))&&!game.bedwars().hasBed(p(2))&&!game.bedwars().hasBed(p(3)),"new map never restores the already-lost Beta/Delta bed right");
            var liveBeds=game.bedwars().arena().topology().beds();require(liveBeds.size()==1&&liveBeds.getFirst().team()==teamA,"new topology retains only actual surviving logical bed");
            var bed=liveBeds.getFirst();var expected=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+LbcBedwars.colorName(teamA)+"_bed"));
            require(p(0).level().getBlockState(bed.foot()).is(expected)&&p(0).level().getBlockState(bed.head()).is(expected),"new physical bed pair is retinted to original logical native team color");
            require(aux.container(teamA)==root&&root.getItem(7).is(Items.DIAMOND)&&root.getItem(7).getCount()==17,"identical canonical54slot root retains17diamonds");
            int after=aux.beaconSeconds(teamA,LbcBedwarsShop.Beacon.HASTE2);require(after==transitionBeacon||after==transitionBeacon-1,"existing beacon timer retained through map cycle, allowing one ordinary resumed boundary");
            require(!aux.chest(p(0).level(),oldChest)&&p(0).level().getBlockEntity(oldChest)!=oldChestBody&&p(0).level().getEntity(oldDisplay)==null,"old physical chest object/association/child display retired");
            require(!aux.generators().isEmpty()&&aux.generators().stream().noneMatch(g->oldGenerators.contains(g.id()))&&aux.generators().stream().allMatch(g->g.level().getEntity(g.id())!=null),"actual new-map native source generator bodies indexed");
            SkyWars2.LOGGER.info("[check] bedwars-renewal destination={} logicalTeams={}/{} beaconBefore={} after={} sampledMapPhase={}",game.config().mapId,teamA,teamB,transitionBeacon,after,sawMap);
        }
        public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            game.config().noBorders=oldBorders;game.items().timeStop().end(game.maps().level());
            for(var client:clients)try{client.close();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("BedWars renewal native client cleanup",failure);
            // The source map/bed/elimination transaction is intentionally irreversible in this disposable match.
        }
    }
}
