package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import me.skywars2.game.modes.bedwars.LbcBedwars;
import me.skywars2.game.modes.bedwars.LbcBedwarsArena;
import me.skywars2.game.modes.bedwars.LbcBedwarsAuxiliaries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.skywars2.game.effects.magic.LbcMana;

/** Operator-only scenes use the real mode3 owner, native players and normally ticking coordinator. */
public final class BedwarsChecks {
    private BedwarsChecks(){}
    private static Life life;
    private static Win win;
    private static BedwarsRoleChecks.Probe role;
    private record CeilingGlass(ServerLevel level,int team,BlockPos pos){}
    private static CeilingGlass ceilingGlass;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("BedWars mode3: "+why);}
    private static ServerPlayer player(GameController game,String name){return Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name)),name);}
    private static void pass(String suite,String why){SkyWars2.LOGGER.info("[check] PASS {} ({})",suite,why);}
    private static Villager merchant(GameController game){
        return game.bedwars().arena().topology().markers().stream().filter(m->m.kind()==LbcBedwarsArena.MarkerKind.SHOP)
                .map(m->game.maps().level().getEntity(m.id())).filter(Villager.class::isInstance).map(Villager.class::cast).findFirst().orElseThrow();
    }
    public static void lobby(GameController game){
        require(game.inLobby()&&game.bedwars().mode()&&game.selectedMapReady(),"real ready BedWars3/4 lobby");
        var observed=new ArrayList<String>();for(var entity:game.maps().level().getAllEntities())if(entity.entityTags().contains("bedwars_bed"))observed.add(entity.blockPosition()+"="+game.maps().level().getBlockState(entity.blockPosition())+" removed="+entity.isRemoved());
        SkyWars2.LOGGER.info("[check] bedwars placed map={} spawns={} teams={} bedCells={}",game.config().mapId,game.maps().spawns().size(),game.bedwars().availableTeams(),observed);
        var teams=game.bedwars().availableTeams().stream().sorted().toList();require(teams.size()>=2,"placed source bed identities");
        var names=List.of("TestAlpha","TestGamma","TestBeta","TestDelta");
        for(int n=0;n<names.size();n++){
            var p=GameChecks.createBot(game,names.get(n));var profile=game.players().get(p.getUUID());
            profile.lbcSkill=profile.lbcKit=profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            profile.lbcChallenge=profile.lbcChallengeKit=0;
            require(game.bedwars().selectTeam(p,teams.get(n/2)),"native lobby numeric team selection");p.setNoGravity(true);
        }
        require(game.canStart(null),"four native players can share two actual bed spawns");
        ceilingGlass=game.gameMode()==3&&game.config().mapId.equals("99991b")?stageCeilingGlass(game,teams.getFirst()):null;
        pass("bedwars-menu","actual mode"+game.gameMode()+"/four native participants/two selected teams/shared-spawn capacity");
    }
    private static CeilingGlass stageCeilingGlass(GameController game,int team){
        var level=game.maps().level();
        require(level.getMaxY()==319,"local mode3 fixture has the actual Y319 ceiling");
        var bed=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+LbcBedwars.colorName(team)+"_bed"));
        Entity marker=null;
        for(var entity:level.getAllEntities())if(entity.entityTags().contains("bedwars_bed")
                &&level.getBlockState(entity.blockPosition()).is(bed)){marker=entity;break;}
        require(marker!=null,"real selected team's placed bed marker");
        var bedPosition=marker.position();
        var spawn=game.maps().spawns().stream().min(Comparator.comparingDouble(s->s.position().distanceToSqr(bedPosition))).orElseThrow();
        var column=BlockPos.containing(spawn.position());
        var top=new BlockPos(column.getX(),level.getMaxY(),column.getZ());
        require(level.hasChunkAt(top)&&level.isInValidBounds(top)&&!level.isInValidBounds(top.above())
                        &&level.getBlockState(top).isAir()&&level.getBlockState(top.below()).isAir(),
                "empty resident ceiling column and one illegal cell above it");
        // The normal map reset owns cleanup of this otherwise empty fixture cell.
        game.maps().recordBlockChange(top);
        require(level.setBlock(top,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS),
                "stage real resident solid at Y319 before native BedWars setup");
        return new CeilingGlass(level,team,top);
    }
    private static void checkCeilingGlass(GameController game){
        var witness=ceilingGlass;
        require(witness!=null&&witness.level()==game.maps().level(),"pre-start ceiling witness belongs to this loaded arena");
        var level=witness.level();var pos=witness.pos();
        var glass=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+LbcBedwars.colorName(witness.team())+"_stained_glass"));
        require(game.bedwars().arena().topology().beds().stream().anyMatch(b->b.team()==witness.team()
                        &&b.spawn().getX()==pos.getX()&&b.spawn().getZ()==pos.getZ()),"the normal setup used the witnessed spawn column");
        require(level.getBlockState(pos).is(glass)&&game.bedwars().arena().originalTypeAt(level,pos)==glass,
                "actual setup replaces Y319 stone with team glass and protects that final type");
        require(level.getBlockState(pos.below()).isAir()&&!level.isInValidBounds(pos.above())
                        &&level.getBlockState(pos.above()).isAir()&&game.bedwars().arena().originalTypeAt(level,pos.above())==null,
                "column preserves AIR below and skips the outside cell at Y320");
    }
    public static void run(GameController game,String suite){
        require(game.playing()&&game.bedwars().mode()&&game.bedwars().active()&&game.bedwars().arena().active(),"admitted real BedWars match");
        var a=player(game,"TestAlpha");var b=player(game,"TestBeta");
        switch(suite){
            case "bedwars-basic", "bedwars-source"->{
                var c=player(game,"TestGamma");var d=player(game,"TestDelta");
                require(game.bedwars().team(a)==game.bedwars().team(c)&&game.bedwars().team(b)==game.bedwars().team(d)&&a.getTeam()!=b.getTeam(),"two actual native shared teams");
                require(a.position().distanceToSqr(c.position())<1&&b.position().distanceToSqr(d.position())<1,"same-team native shared spawns");
                require(game.bedwars().arena().topology().beds().size()==2&&game.bedwars().hasBed(a)&&game.bedwars().hasBed(b)&&game.bedwars().winnerTeam()==-1,"two owned physical beds prevent solo winner logic");
                require(!game.bedwars().auxiliaries().generators().isEmpty()&&LbcBedwars.shopRole(merchant(game)),"real marker topology/generators/protected merchant");
                if(game.gameMode()==3&&game.config().mapId.equals("99991b"))checkCeilingGlass(game);
                pass(suite,"actual mode"+game.gameMode()+"/shared spawns/two team beds/generators/merchant and team victory admission");
            }
            case "bedwars-shop"->{var npc=merchant(game);move(a,npc.position().add(0,0,2));BedwarsShopChecks.start(game,game.bedwars().shop(),npc);}
            case "bedwars-auxiliary"->BedwarsAuxiliaryChecks.start(game);
            case "bedwars-shop-role"->{
                require(role==null,"exclusive role scene");var npc=merchant(game);
                for(var p:game.server().getPlayerList().getPlayers())move(p,npc.position().add(12+game.bedwars().team(p),8,12));
                role=BedwarsRoleChecks.start(game,a,b,npc);
            }
            case "bedwars-protection"->{
                require(game.config().mapId.equals("99991b"),"actual locally placed finite protection fixture");
                move(a,new Vec3(0,82,3));
                BedwarsProtectionChecks.runNative(game,a,new BedwarsProtectionChecks.Cells(new BlockPos(0,81,0),new BlockPos(3,81,0),new BlockPos(6,81,0),new BlockPos(9,81,0),new BlockPos(0,81,5),new BlockPos(3,81,5),new BlockPos(-1,81,0)));
                var bed=game.bedwars().arena().topology().beds().stream().filter(x->x.team()==game.bedwars().team(a)).findFirst().orElseThrow();BedwarsProtectionChecks.ownBed(game,a,bed);
                pass(suite,"native type/loot/container/TNT/falling/explosion/fluid/piston/clone/template atomic denial, ordinary positive controls and own-bed halves");
            }
            case "bedwars-lifecycle"->{require(life==null,"exclusive life scene");life=new Life(game,a,b);}
            case "bedwars-victory"->{require(win==null,"exclusive winner scene");win=new Win(game,a,b);}
            case "bedwars-allies"->{BedwarsRoleChecks.allies(game,a,player(game,"TestGamma"),b);pass(suite,"actual allied native melee/mob/arrow/effects/impulse admission, enemy and accepted WILD/electrical positive controls");}
            default->throw new IllegalArgumentException(suite);
        }
    }
    private static void move(ServerPlayer p,Vec3 at){p.teleportTo(p.level(),at.x,at.y,at.z,Set.of(),0,0,true);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);}
    public static void tick(GameController game){
        if(win!=null)try{if(win.step()){win=null;pass("bedwars-victory","actual two-team elimination and rewards; native zombie and team chest survive fireworks with closed transactions, then retire in lobby");}}catch(Throwable error){win=null;SkyWars2.LOGGER.error("[check] FAIL bedwars-victory",error);}
        if(role!=null)try{if(role.finish()){role.close();role=null;}}catch(Throwable error){try{role.close();}finally{role=null;}SkyWars2.LOGGER.error("[check] FAIL bedwars-shop-role",error);}
        if(life!=null)try{if(life.step()){life.close();life=null;pass("bedwars-lifecycle","native death/body replacement/100 active ticks/+30 XP levels/TimeStop, pending bed-loss cancellation and nonempty whole-world team/bed/player rewind");}}catch(Throwable error){try{life.close();}finally{life=null;}SkyWars2.LOGGER.error("[check] FAIL bedwars-lifecycle",error);}
    }
    private static final class Win {
        final GameController game;final UUID owner,teammate;final int coins,wins,keys,teamCoins,teamWins,teamKeys;final int started;
        final ServerLevel level;final BlockPos chest;final Display.BlockDisplay chestVisual;final Zombie defender;
        final LbcBedwarsAuxiliaries.SharedContainer stock;
        boolean endingChecked;
        Win(GameController game,ServerPlayer a,ServerPlayer b){
            this.game=game;owner=a.getUUID();var c=player(game,"TestGamma");teammate=c.getUUID();
            var profile=game.players().get(owner);coins=profile.coins;wins=profile.wins;keys=Arrays.stream(profile.keys).sum();var other=game.players().get(teammate);teamCoins=other.coins;teamWins=other.wins;teamKeys=Arrays.stream(other.keys).sum();
            level=a.level();var aux=game.bedwars().auxiliaries();
            var look=BlockPos.containing(game.maps().look());
            level.getChunkAt(look);
            chest=BlockPos.betweenClosedStream(look.offset(-3,-1,-3),look.offset(3,3,3))
                    .filter(pos->!level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos)&&level.getBlockState(pos).isAir()
                            &&game.bedwars().arena().originalTypeAt(level,pos)==null).findFirst().orElseThrow().immutable();
            require(level.setBlock(chest,Blocks.ENDER_CHEST.defaultBlockState(),Block.UPDATE_ALL)
                    &&aux.placed(a,level,chest,aux.teamChest(a)),"actual marked chest and its native display placed near victory viewers");
            chestVisual=level.getEntitiesOfClass(Display.BlockDisplay.class,new AABB(chest).inflate(1),
                    display->display.blockPosition().equals(chest)&&display.entityTags().contains("skywars2.bedwars.chest_visual")).getFirst();
            stock=aux.container(game.bedwars().team(a));stock.setItem(0,new ItemStack(Items.DIAMOND,3));
            var held=a.getMainHandItem();
            var egg=aux.teamZombie(a);a.setItemInHand(InteractionHand.MAIN_HAND,egg);
            require(aux.useZombie(a,InteractionHand.MAIN_HAND,chest,Direction.UP)&&egg.isEmpty(),"actual native team zombie item accepts and debits before victory");
            a.setItemInHand(InteractionHand.MAIN_HAND,held);
            defender=level.getEntitiesOfClass(Zombie.class,new AABB(chest).inflate(3),zombie->aux.owner(zombie)==a).getFirst();
            defender.setNoGravity(true);defender.setNoAi(true);
            move(a,new Vec3(0,82,0));move(c,new Vec3(20,82,20));
            var bed=game.bedwars().arena().topology().beds().stream().filter(x->x.team()==game.bedwars().team(b)).findFirst().orElseThrow();BedwarsProtectionChecks.enemyBed(game,a,bed);
            for(var p:List.of(b,player(game,"TestDelta"))){
                p.setInvulnerableTime(0);p.damageCooldownTime=0;p.removeAllEffects();p.setHealth(p.getMaxHealth());
                require(p.hurtServer(p.level(),p.damageSources().playerAttack(a),10000)&&p.isDeadOrDying()&&game.lives(p.getUUID())==0,"actual final enemy participant elimination");
                // A native immediate-respawn client sends this before the next round; fixture sessions need the real packet too.
                p.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                var replacement=game.server().getPlayerList().getPlayer(p.getUUID());replacement.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                require(replacement!=p&&replacement.isAlive()&&game.isObserver(replacement),"actual eliminated spectator body survives later fixture rounds");
            }
            started=game.server().getTickCount();
        }
        boolean step(){
            int elapsed=game.server().getTickCount()-started;
            require(elapsed<game.config().endingSeconds*20+2400,"bounded native fireworks and lobby transition");
            var aux=game.bedwars().auxiliaries();
            if(endingChecked){
                if(game.stage()==GameController.Stage.ENDING){
                    require(level.getEntity(defender.getUUID())==defender&&level.getEntity(chestVisual.getUUID())==chestVisual,
                            "same published native bodies remain throughout idle fireworks");return false;
                }
                if(!game.inLobby())return false;
                require(defender.isRemoved()&&chestVisual.isRemoved()&&!aux.chest(level,chest)&&aux.containers().isEmpty(),"native lobby transition retires both original bodies and their auxiliary state");
                level.setBlock(chest,Blocks.AIR.defaultBlockState(),Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                return true;
            }
            require(elapsed<20,"winner resolved by next core tick");if(game.playing())return false;
            require(game.stage()==GameController.Stage.ENDING,"real team winner enters native ending");var a=game.server().getPlayerList().getPlayer(owner);var c=game.server().getPlayerList().getPlayer(teammate);var profile=game.players().get(owner);var other=game.players().get(teammate);
            require(profile.coins==coins+200&&profile.wins==wins+1&&Arrays.stream(profile.keys).sum()==keys+1,"two50coin kills plus sole100coin/key/win source recipient");
            require(other.coins==teamCoins+100&&other.wins==teamWins+1&&Arrays.stream(other.keys).sum()==teamKeys+1&&a.gameMode.getGameModeForPlayer()==GameType.CREATIVE&&c.gameMode.getGameModeForPlayer()==GameType.CREATIVE,"every winning teammate receives100coins/key/win while living winners retain creative");
            require(!game.bedwars().active()&&!game.bedwars().arena().active()&&aux.containers().contains(stock)&&stock.getItem(0).getCount()==3,
                    "native gameplay stops while the team chest graph remains intact");
            require(level.getEntity(defender.getUUID())==defender&&level.getEntity(chestVisual.getUUID())==chestVisual
                    &&aux.chest(level,chest)&&level.getBlockState(chest).is(Blocks.ENDER_CHEST),"native defender and chest visual survive the first victory fireworks");
            require(defender.getTarget()==null&&!aux.canAttack(defender,c),"retained native defender stays unable to attack");
            require(aux.open(a,InteractionHand.MAIN_HAND,level,chest)&&a.containerMenu==a.inventoryMenu,
                    "retained chest consumes interaction without reopening its shared store");
            var egg=aux.teamZombie(a);a.setItemInHand(InteractionHand.MAIN_HAND,egg);
            require(aux.useZombie(a,InteractionHand.MAIN_HAND,chest,Direction.UP)&&egg.getCount()==1,
                    "celebration never spawns another native defender or spends the item");
            a.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            endingChecked=true;
            return false;
        }
    }
    private static final class Life implements AutoCloseable {
        final GameController game;final UUID alpha,beta;final int lives;final LbcBedwarsArena.Bed bed;int phase,when,respawnLevel,savedTimer,ordinaryRespawns,expectedMaximum,expectedRegeneration;
        Life(GameController game,ServerPlayer a,ServerPlayer b){
            this.game=game;alpha=a.getUUID();beta=b.getUUID();lives=game.lives(beta);
            bed=game.bedwars().arena().topology().beds().stream().filter(x->x.team()==game.bedwars().team(b)).findFirst().orElseThrow();
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot(),"fresh real coordinator");
            require(game.vanity().requestCapture(a,false),"real mode3 capture request");begin(0);
        }
        void begin(int next){phase=next;when=game.server().getTickCount();}
        ServerPlayer a(){return game.server().getPlayerList().getPlayer(alpha);}ServerPlayer b(){return game.server().getPlayerList().getPlayer(beta);}
        void killAndReplace(){
            var previous=b();previous.setInvulnerableTime(0);previous.damageCooldownTime=0;previous.setHealth(previous.getMaxHealth());
            require(previous.hurtServer(previous.level(),previous.damageSources().playerAttack(a()),10000)&&previous.isDeadOrDying(),"real native attributed lethal hit");
            require(game.bedwars().pending(previous)&&game.awaitingRespawn(beta)&&game.lives(beta)==lives,"bed preserves sole core lives owner and schedules source100tick interval");
            previous.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            var current=b();current.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            require(current!=previous&&current.isAlive()&&current.gameMode.getGameModeForPlayer()==GameType.SPECTATOR&&game.awaitingRespawn(beta),"actual replacement waits in native spectator without generic viewer conversion");respawnLevel=current.experienceLevel;current.setNoGravity(true);
        }
        boolean step(){
            int elapsed=game.server().getTickCount()-when;require(elapsed<1800,"bounded real coordinator/life stage"+phase);
            if(game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            switch(phase){
                case 0->{if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;var mana=game.items().mana();mana.maximum(b(),mana.maximum(b())+2345);expectedMaximum=mana.maximum(b());expectedRegeneration=mana.baseRegen(b());mana.set(b(),expectedMaximum-1);killAndReplace();savedTimer=game.bedwars().checkpoint().savedRespawns.get(beta);game.items().timeStop().begin(game.maps().level(),1000);begin(1);}
                case 1->{require(game.bedwars().checkpoint().savedRespawns.get(beta)==savedTimer&&game.playing()&&game.bedwars().winnerTeam()==-1,"pending member counts as team and TimeStop pauses actual100tick clock");if(elapsed<25)return false;game.items().timeStop().end(game.maps().level());begin(2);}
                case 2->{if(game.awaitingRespawn(beta))return false;require(game.items().mana().maximum(b())==expectedMaximum&&game.items().mana().baseRegen(b())==expectedRegeneration&&game.items().mana().get(b())<=expectedMaximum,"real BedWars body replacement cannot accumulate mana, maximum or regeneration beyond the previous life");if(b().hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE))return false;require(elapsed>=savedTimer-1&&game.lives(beta)==lives&&b().isAlive()&&game.canUseGameplayItems(b())&&b().experienceLevel==respawnLevel+30,"bed respawn waits complete active clock, preserves lives and adds source30levels");if(++ordinaryRespawns<3){killAndReplace();savedTimer=game.bedwars().checkpoint().savedRespawns.get(beta);begin(2);return false;}killAndReplace();BedwarsProtectionChecks.enemyBed(game,a(),bed);require(!game.bedwars().hasBed(b()),"bed entitlement retired immediately");begin(3);}
                case 3->{if(game.awaitingRespawn(beta))return false;require(game.lives(beta)==0&&game.isObserver(b())&&game.playing(),"bed loss cancels pending member, surviving teammate keeps match alive");require(game.vanity().requestRestoreForCheck(),"nonempty mode3 whole-world replay");begin(4);}
                case 4->{if(game.vanity().busy())return false;require(game.lives(beta)==lives&&!game.awaitingRespawn(beta)&&game.canUseGameplayItems(b())&&game.bedwars().hasBed(b()),"native player/core/team entitlement restored together");require(game.maps().level().getBlockState(bed.head()).getBlock() instanceof net.minecraft.world.level.block.BedBlock&&game.maps().level().getBlockState(bed.spawn().below(2)).is(net.minecraft.world.level.block.Blocks.BEACON),"physical terrain and protection topology restored with bed");BedwarsProtectionChecks.ownBed(game,b(),bed);return true;}
            }return false;
        }
        public void close(){game.items().timeStop().end(game.maps().level());game.vanity().stop();}
    }
}
