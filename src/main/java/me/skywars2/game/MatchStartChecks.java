package me.skywars2.game;

import java.nio.file.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Source startup phases and deferred configuration, exercised with the real controller/players. */
public final class MatchStartChecks {
    private MatchStartChecks(){}
    private static final Path CONFIG=Path.of("config/luckywars/game.json");
    private static Probe probe;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Match mode start: "+why);}
    private static byte[] bytes(){try{return Files.exists(CONFIG)?Files.readAllBytes(CONFIG):null;}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private static void file(byte[] saved){try{if(saved==null)Files.deleteIfExists(CONFIG);else Files.write(CONFIG,saved);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    public static void menu(GameController game){
        require(game.inLobby()&&game.selectedMapReady(),"actual ready lobby");int old=game.config().gameMode;String map=game.config().mapId;byte[] saved=bytes();
        try{
            for(int mode:new int[]{1,2,3,4}){
                game.config().gameMode=mode;boolean bed=mode>=3;
                var wrong=game.maps().catalog(!bed).stream().filter(MapManager.MapInfo::available).findFirst().orElseThrow();
                require(!game.mapMatchesMode(wrong.id()),"source map family rejected "+mode);
                require(!game.selectMap(null,wrong.id(),false)&&game.config().mapId.equals(map)&&!game.maps().busy(),"wrong-family selection cannot mutate/paste "+mode);
                require(game.selectedMapReady()==(game.maps().bedwars(map)==bed),"cached readiness follows requested map family "+mode);
                require(game.modeReady(mode),"ready complete source mode family "+mode);
            }
        }finally{game.config().gameMode=old;}
        require(game.selectedMapReady()&&game.config().mapId.equals(map)&&Arrays.equals(saved,bytes()),"exact lobby selection/configuration retained");
        require(!game.modeReady(0)&&!game.modeReady(5),"invalid source modes remain rejected");
        SkyWars2.LOGGER.info("[check] PASS match-mode-menu (four ready source modes, stale readiness/wrong selection rejection, no paste/config mutation)");
    }
    public static void run(GameController game){byte[] saved=bytes();int original=game.gameMode();
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            int level=f.owner.experienceLevel,total=f.owner.totalExperience;float progress=f.owner.experienceProgress;
            try{
                f.prepareParticipants();int lives=game.lives(f.owner.getUUID());boolean bed=game.bedwars().mode();
                var at=f.owner.blockPosition().offset(0,5,5);for(var actor:f.level.getAllEntities())if(actor instanceof Marker&&actor.entityTags().stream().anyMatch(t->Set.of("swrg.default","swrg.midle","swrg.center").contains(t)))f.remember(actor.blockPosition());
                f.set(at,Blocks.BARREL.defaultBlockState());var chest=(RandomizableContainerBlockEntity)f.level.getBlockEntity(at);chest.setItem(0,new ItemStack(Items.ECHO_SHARD,13));var marker=new Marker(EntityTypes.MARKER,f.level);marker.setPos(Vec3.atCenterOf(at));marker.entityTags().add("swrg.default");f.own(marker);require(f.level.addFreshEntity(marker),"actual arena chest anchor");
                int next=original>=3?1:3;game.configureLobbySetting(null,"gamemode",Integer.toString(next));
                require(game.config().gameMode==next&&GameConfig.load().gameMode==next&&game.gameMode()==original&&game.bedwars().mode()==bed,"real persisted setting does not reclassify active match");
                game.maps().fillChests(false);require(bed?chest.getItem(0).is(Items.ECHO_SHARD):!chest.getItem(0).is(Items.ECHO_SHARD),"real arena refill uses live mode rather than next-match configuration");
                var tags=Set.copyOf(f.owner.entityTags());
                for(int mode:new int[]{1,2,3,4}){
                    game.beginMatchMode(mode);f.owner.experienceLevel=7;f.owner.totalExperience=123;f.owner.experienceProgress=.25f;
                    game.grantStartExperience(f.owner);require(f.owner.experienceLevel==7+(mode==2?20:0)&&f.owner.totalExperience==123&&f.owner.experienceProgress==.25f,"native source initial XP levels only in mode2 "+mode);
                    require(f.owner.entityTags().equals(tags),"XP initialization never manufactures Stand entitlement");
                }
                game.beginMatchMode(2);f.owner.experienceLevel=0;game.grantStartExperience(f.owner);game.players().get(f.owner.getUUID()).kit=11;
                boolean kits=game.config().vanillaKits,ranked=game.config().ranked;try{
                    game.config().vanillaKits=false;game.events().giveKit(f.owner);require(f.owner.experienceLevel==20,"disabled vanilla kit cannot add XP");
                    game.config().vanillaKits=true;game.config().ranked=true;game.events().giveKit(f.owner);require(f.owner.experienceLevel==40,"source20 levels precede available native kit11 twenty-level grant");
                }finally{game.config().vanillaKits=kits;game.config().ranked=ranked;}
                for(int mode:new int[]{1,2}){game.beginMatchMode(mode);game.prepareSoloTeams(List.of(f.owner,f.other));require(f.owner.getTeam()!=null&&f.other.getTeam()!=null&&f.owner.getTeam()!=f.other.getTeam()&&!f.owner.isAlliedTo(f.other),"real distinct native solo memberships "+mode);}
                var team=f.owner.getTeam();for(int mode:new int[]{3,4}){game.beginMatchMode(mode);game.prepareSoloTeams(List.of(f.owner,f.other));require(f.owner.getTeam()==team,"BedWars does not receive a solo reassignment "+mode);}
                require(game.lives(f.owner.getUUID())==lives,"configured/additional life count unchanged by mode phases");
                game.beginMatchMode(original);var image=game.checkpointCore();game.beginMatchMode(next);boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
                try{game.restoreCore(image);require(game.gameMode()==original&&game.config().gameMode==next,"typed core replay restores captured live mode independently of future configuration");}finally{game.server().tickRateManager().setFrozen(frozen);}
            }finally{f.owner.experienceLevel=level;f.owner.totalExperience=total;f.owner.experienceProgress=progress;}
        }finally{file(saved);}
        require(game.gameMode()==original,"canonical fixture restores exact original mode");
        SkyWars2.LOGGER.info("[check] PASS match-mode-start (actual persistent setting/current-vs-next chest/BedWars split; native four-mode source XP and kit order, real solo teams, life preservation and typed core replay)");
    }
    private record Probe(MatchValueSnapshot config,byte[] file,int next){}
    public static void stopProbe(GameController game){
        require(probe==null&&game.playing(),"exclusive actual stop probe");int active=game.gameMode(),next=active>=3?1:3;probe=new Probe(new MatchValueSnapshot(game.config()),bytes(),next);
        game.configureLobbySetting(null,"gamemode",Integer.toString(next));require(game.gameMode()==active&&game.config().gameMode==next,"pending setting leaves live family intact until real stop");
        SkyWars2.LOGGER.info("[check] PASS match-mode-stop-probe (next-mode request retained for actual controller stop)");
    }
    public static void cleanup(GameController game){var saved=Objects.requireNonNull(probe,"pending mode probe");try{
        require(game.inLobby()&&game.gameMode()==saved.next&&game.config().gameMode==saved.next&&!game.bedwars().active()&&!game.teamColors().active(),"actual lobby clears latched mode and bed/solo ownership before exposing next setting");
    }finally{saved.config.restore();file(saved.file);probe=null;}
        SkyWars2.LOGGER.info("[check] PASS match-mode-cleanup (actual stop/lobby mode handoff and exact configuration restoration)");
    }
}
