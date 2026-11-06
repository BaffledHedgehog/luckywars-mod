package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Bed entitlement and topology belong here; Participant remains the sole life/death owner. */
public final class LbcBedwars {
    private static final String[] COLORS={"light_blue","black","blue","cyan","purple","gray","green","magenta","brown","orange","light_gray","lime","pink","red","white","yellow"};
    private static final String[] GENERATORS={"copper_generator","iron_generator","gold_generator","diamond_generator","star_generator"};
    private static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    private record Site(LbcBedwarsArena.Bed bed,MapManager.Spawn spawn) { }
    private record Write(BlockPos position,BlockState state) { }
    private final GameController game;
    private final LbcBedwarsArena arena;
    private final LbcBedwarsAuxiliaries auxiliaries;
    private final LbcBedwarsShop shop;
    private List<Entity> placed=List.of();
    private List<LbcBedwarsArena.Bounds> bounds=List.of();
    private final Map<Integer,Site> sites=new LinkedHashMap<>();
    private final Map<UUID,Integer> assignments=new LinkedHashMap<>();
    private final Set<Integer> beds=new LinkedHashSet<>();
    private final Map<UUID,Integer> respawns=new LinkedHashMap<>();
    private final ArrayDeque<Write> setup=new ArrayDeque<>();
    private String epoch;
    private boolean running,activated,restoring,renewing,renewalPrepared;
    private Map<UUID,MapManager.Spawn> renewalSpawns=Map.of();
    private int presentTeams;

    public LbcBedwars(GameController game){
        this.game=game;
        arena=new LbcBedwarsArena(game,new LbcBedwarsArena.Policy(){
            public boolean active(){return LbcBedwars.this.active()&&activated;}
            public boolean canBreakBed(ServerPlayer p,int number){return canUse(p)&&team(p)!=number;}
            public void bedChanged(LbcBedwarsArena.Bed bed){lostBed(bed.team());}
            public boolean lifecycleAllowed(LbcBedwarsArena.Lifecycle reason){
                return switch(reason){
                    case MAP_PLACEMENT,MAP_RESET->game.maps().busy()||!game.playing();
                    case MAP_RENEWAL->game.supernovaMapTransition()&&game.maps().busy();
                    case COORDINATOR_REPLAY->game.vanity().busy()&&game.server().tickRateManager().isFrozen();
                };
            }
        });
        auxiliaries=new LbcBedwarsAuxiliaries(game,new LbcBedwarsAuxiliaries.Policy(){
            public String epoch(){return game.items().epoch();}
            public boolean active(){return LbcBedwars.this.active()&&activated&&game.playing();}
            public boolean canUse(ServerPlayer p){return LbcBedwars.this.canUse(p);}
            public int team(ServerPlayer p){return LbcBedwars.this.team(p);}
            public net.minecraft.world.scores.PlayerTeam playerTeam(ServerPlayer p){return p.getTeam();}
            public boolean hasBed(int number){return LbcBedwars.this.hasBed(number);}
            public void retiredGenerator(UUID id){var e=game.maps().level().getEntity(id);if(e!=null)e.discard();}
        });
        shop=new LbcBedwarsShop(new LbcBedwarsShop.Policy(){
            public String epoch(){return game.items().epoch();}
            public boolean canTrade(ServerPlayer p,Entity merchant){return canUse(p)&&shopRole(merchant)&&merchant.isAlive()&&!merchant.isRemoved()&&merchant.level()==p.level()&&p.distanceToSqr(merchant)<=64;}
            public int team(ServerPlayer p){return LbcBedwars.this.team(p);}
            public net.minecraft.world.item.ItemStack teamChest(ServerPlayer p){return auxiliaries.teamChest(p);}
            public net.minecraft.world.item.ItemStack teamZombie(ServerPlayer p){return auxiliaries.teamZombie(p);}
            public void beacon(ServerPlayer p,LbcBedwarsShop.Beacon buff,int seconds){auxiliaries.beacon(p,buff,seconds);}
        });
    }
    public static String colorName(int team){if(team<1||team>16)throw new IllegalArgumentException("Source team1..16");return COLORS[team-1];}
    public LbcBedwarsArena arena(){return arena;}
    public LbcBedwarsAuxiliaries auxiliaries(){return auxiliaries;}
    public LbcBedwarsShop shop(){return shop;}
    public boolean mode(){return (game.gameMode()==3||game.gameMode()==4)&&!game.balanceLegacy();}
    public boolean active(){return running&&mode()&&game.items().currentEpoch(epoch);}
    public boolean canUse(ServerPlayer p){return active()&&activated&&game.canUseGameplayItems(p)&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!game.items().actionsBlocked(p)&&team(p)>0;}
    public static boolean shopRole(Entity e){return LbcBedwarsRoleGuards.shop(e);}
    public int team(ServerPlayer p){
        var nativeTeam=p.getTeam();
        if(nativeTeam!=null)try{int value=Integer.parseInt(nativeTeam.getName());if(sites.containsKey(value))return value;}catch(NumberFormatException ignored){}
        return assignments.getOrDefault(p.getUUID(),0);
    }
    /** Stable causal team for accepted work, including an owner whose native player body is offline. */
    public int assignedTeam(UUID owner){return assignments.getOrDefault(owner,0);}
    public boolean hasBed(ServerPlayer p){return hasBed(team(p));}
    public boolean hasBed(int number){return active()&&number>0&&beds.contains(number)&&(presentTeams&(1<<number))!=0;}
    public boolean pending(ServerPlayer p){return active()&&respawns.containsKey(p.getUUID());}
    public void death(ServerPlayer p){if(hasBed(p)){respawns.put(p.getUUID(),100);p.entityTags().add("have_bed");respawnTitle(p,100);}}
    public boolean startReady(){return !active()||activated;}

    /** Called once before completed native placement; subsequent writes feed the existing type owner. */
    public void loading(ServerLevel level,List<BoundingBox> boxes,boolean bedwars){
        boolean sameMatch=bedwars&&active()&&game.supernovaMapTransition();
        if(!bedwars){stop();return;}
        shop.stop();renewing=sameMatch;renewalPrepared=false;renewalSpawns=Map.of();
        activated=false;setup.clear();placed=List.of();sites.clear();bounds=List.of();
        // Semantic bounds also contain source cameras/markers; block height is validated separately by MapManager.
        bounds=boxes.stream().map(b->new LbcBedwarsArena.Bounds(b.minX()-3,b.minY()-3,b.minZ()-3,b.maxX()+3,Math.max(319,b.maxY()),b.maxZ()+3)).toList();
        arena.beginCapture(level,bounds);
    }
    public void mapReady(List<Entity> markers){
        placed=List.copyOf(markers);
        for(var e:placed)if(shopRole(e)){LbcBedwarsRoleGuards.markShop(e);e.entityTags().add("strict_map_object");e.setPermanentlyInvulnerable(true);if(e instanceof Villager v){v.setNoAi(true);v.setPersistenceRequired();}}
        discover();
    }
    private static int color(BlockState state){
        if(!(state.getBlock() instanceof BedBlock))return 0;
        var name=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        for(int n=0;n<COLORS.length;n++)if(name.equals(COLORS[n]+"_bed"))return n+1;
        return 0;
    }
    private void discover(){
        sites.clear();var spawn=placed.stream().filter(e->!e.isRemoved()&&e.entityTags().contains("swrg.spawn")).toList();
        for(var marker:placed){
            if(marker.isRemoved()||!marker.entityTags().contains("bedwars_bed"))continue;
            var level=(ServerLevel)marker.level();var pos=marker.blockPosition();var state=level.getBlockState(pos);int number=color(state);
            if(number==0||spawn.isEmpty())continue;
            var foot=state.getValue(BedBlock.PART)==BedPart.FOOT?pos:pos.relative(state.getValue(BedBlock.FACING).getOpposite());
            var head=foot.relative(state.getValue(BedBlock.FACING));
            if(level.getBlockState(foot).getBlock()!=state.getBlock()||level.getBlockState(head).getBlock()!=state.getBlock())continue;
            var nearest=spawn.stream().min(Comparator.comparingDouble(e->e.position().distanceToSqr(marker.position()))).orElseThrow();
            var at=new MapManager.Spawn(nearest.position(),nearest.getYRot(),nearest.getXRot());
            sites.putIfAbsent(number,new Site(new LbcBedwarsArena.Bed(number,marker.getUUID(),foot,head,nearest.blockPosition()),at));
        }
    }
    public Set<Integer> availableTeams(){return Set.copyOf(sites.keySet());}
    public boolean selectTeam(ServerPlayer p,int number){
        if(!game.inLobby()||!game.canUseMenus(p)||number<0||number>16||number!=0&&!sites.containsKey(number))return false;
        var board=game.server().getScoreboard();if(p.getTeam()!=null)board.removePlayerFromTeam(p.getScoreboardName(),p.getTeam());
        if(number!=0){game.teamColors().ensurePalette();board.addPlayerToTeam(p.getScoreboardName(),board.getPlayerTeam(Integer.toString(number)));}
        return true;
    }
    /** Preserve valid selections; empty teams first, then minimum population, with the source split. */
    public Map<UUID,MapManager.Spawn> prepare(List<ServerPlayer> players){
        if(!mode()||sites.size()<(game.developerMode()?1:2))throw new IllegalStateException("BedWars requires placed team beds and spawns");
        shop.stop();auxiliaries.stop();assignments.clear();beds.clear();respawns.clear();setup.clear();
        renewing=renewalPrepared=false;renewalSpawns=Map.of();
        var counts=new LinkedHashMap<Integer,Integer>();sites.keySet().forEach(t->counts.put(t,0));
        for(var p:players){int selected=0;var t=p.getTeam();if(t!=null)try{selected=Integer.parseInt(t.getName());}catch(NumberFormatException ignored){}
            if(counts.containsKey(selected)){assignments.put(p.getUUID(),selected);counts.merge(selected,1,Integer::sum);}}
        for(var p:players)if(!assignments.containsKey(p.getUUID())){int min=counts.values().stream().mapToInt(Integer::intValue).min().orElseThrow();var options=counts.keySet().stream().filter(t->counts.get(t)==min).toList();int t=options.get(p.getRandom().nextInt(options.size()));assignments.put(p.getUUID(),t);counts.merge(t,1,Integer::sum);}
        if(players.size()>1&&sites.size()>1&&counts.values().stream().filter(n->n>0).count()==1){var shuffled=new ArrayList<>(players);Collections.shuffle(shuffled);int empty=counts.entrySet().stream().filter(e->e.getValue()==0).findFirst().orElseThrow().getKey();for(int n=0;n<players.size()/2;n++){var p=shuffled.get(n);counts.merge(assignments.put(p.getUUID(),empty),-1,Integer::sum);counts.merge(empty,1,Integer::sum);}}
        game.teamColors().beginBedwars(players,assignments);
        epoch=game.items().epoch();running=true;activated=false;presentTeams=0;
        assignments.values().forEach(t->{beds.add(t);presentTeams|=1<<t;});
        for(var entry:sites.entrySet())queueSite(entry.getKey(),entry.getValue());
        var output=new LinkedHashMap<UUID,MapManager.Spawn>();players.forEach(p->{p.entityTags().add("have_bed");output.put(p.getUUID(),sites.get(assignments.get(p.getUUID())).spawn);});
        return Map.copyOf(output);
    }
    /** Same match/epoch: map geometry changes, logical teams and native auxiliary roots do not. */
    public Map<UUID,MapManager.Spawn> prepareRenewal(Collection<UUID> retained){
        if(!active()||!game.supernovaMapTransition())throw new IllegalStateException("Prepare renewal inside the original active match");
        if(renewalPrepared){
            if(!renewalSpawns.keySet().containsAll(retained))throw new IllegalStateException("Retained team roster changed during prepared renewal");
            return renewalSpawns;
        }
        if(!renewing||placed.isEmpty())throw new IllegalStateException("Complete native BedWars replacement placement first");
        var logical=new TreeSet<Integer>();
        for(var id:retained){int number=assignedTeam(id);if(number<1||number>16)throw new IllegalStateException("Retained participant lacks its original team");logical.add(number);}
        if(sites.size()<logical.size())throw new IllegalStateException("Replacement map has insufficient team beds/spawns");
        // Reserve matching colors before assigning spare physical sites to different logical colors.
        var available=new TreeMap<Integer,Site>(sites);var selected=new TreeMap<Integer,Site>();
        for(int number:logical){var same=available.remove(number);if(same!=null)selected.put(number,same);}
        for(int number:logical)if(!selected.containsKey(number))selected.put(number,available.pollFirstEntry().getValue());
        var remapped=new LinkedHashMap<Integer,Site>();
        for(int number:logical){var physical=selected.get(number);var old=physical.bed;
            remapped.put(number,new Site(new LbcBedwarsArena.Bed(number,old.marker(),old.foot(),old.head(),old.spawn()),physical.spawn));}
        var output=new LinkedHashMap<UUID,MapManager.Spawn>();for(var id:retained)output.put(id,remapped.get(assignedTeam(id)).spawn);
        // All validation completes before the first queued native write. Existing lost rights stay lost.
        for(var entry:remapped.entrySet()){
            validatePair(entry.getValue());
            if(game.server().getScoreboard().getPlayerTeam(Integer.toString(entry.getKey()))==null)
                throw new IllegalStateException("Original native team object is absent during renewal");
        }
        setup.clear();
        for(var spare:available.values())queueRetiredSite(spare);
        sites.clear();sites.putAll(remapped);
        for(var entry:sites.entrySet()){
            int number=entry.getKey();var site=entry.getValue();queueSite(number,site);
            // These are NEW map marker identities, not participant/summon/shared-storage team reassignment.
            var board=game.server().getScoreboard();var team=board.getPlayerTeam(Integer.toString(number));
            for(var marker:placed)if(marker.getUUID().equals(site.bed.marker())
                    ||marker.entityTags().contains("swrg.spawn")&&marker.position().equals(site.spawn.position()))
                board.addPlayerToTeam(marker.getScoreboardName(),team);
        }
        renewalSpawns=Map.copyOf(output);renewalPrepared=true;return renewalSpawns;
    }
    private void validatePair(Site site){
        var level=game.maps().level();var foot=level.getBlockState(site.bed.foot());var head=level.getBlockState(site.bed.head());
        if(!(foot.getBlock() instanceof BedBlock)||head.getBlock()!=foot.getBlock()
                ||foot.getValue(BedBlock.PART)!=BedPart.FOOT||head.getValue(BedBlock.PART)!=BedPart.HEAD
                ||head.getValue(BedBlock.FACING)!=foot.getValue(BedBlock.FACING)
                ||!site.bed.foot().relative(foot.getValue(BedBlock.FACING)).equals(site.bed.head()))
            throw new IllegalStateException("Replacement map has a malformed native bed pair");
    }
    private void queueRetiredSite(Site site){
        setup.addLast(new Write(site.bed.foot(),Blocks.AIR.defaultBlockState()));setup.addLast(new Write(site.bed.head(),Blocks.AIR.defaultBlockState()));
        var beacon=BlockPos.containing(site.spawn.position()).below(2);
        if(game.maps().level().getBlockState(beacon).is(Blocks.BEACON))setup.addLast(new Write(beacon,Blocks.AIR.defaultBlockState()));
    }
    private void queueSite(int number,Site site){
        var level=game.maps().level();var bed=site.bed;
        if(!beds.contains(number)){queueRetiredSite(site);return;}
        var replacement=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+COLORS[number-1]+"_bed"));
        if(!(replacement instanceof BedBlock))throw new IllegalStateException("Missing current native bed color");
        setup.addLast(new Write(bed.foot(),replacement.withPropertiesOf(level.getBlockState(bed.foot()))));
        setup.addLast(new Write(bed.head(),replacement.withPropertiesOf(level.getBlockState(bed.head()))));
        var center=BlockPos.containing(site.spawn.position());var glass=BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:"+COLORS[number-1]+"_stained_glass")).defaultBlockState();
        for(var pos:BlockPos.betweenClosed(center.offset(-1,-3,-1),center.offset(1,-3,1)))setup.addLast(new Write(pos.immutable(),Blocks.DIAMOND_BLOCK.defaultBlockState()));
        setup.addLast(new Write(center.below(2),Blocks.BEACON.defaultBlockState()));setup.addLast(new Write(center.below(),glass));
        for(int y=center.getY();y<=319&&y<level.getMaxY();y++){var pos=new BlockPos(center.getX(),y,center.getZ());if(level.getBlockState(pos).is(SOLID))setup.addLast(new Write(pos,glass));}
    }
    /** Native terrain/body publication succeeded; keep every original team/bed/pending/economy owner. */
    public void renewalCommitted(){
        if(!renewing||!renewalPrepared||!activated)throw new IllegalStateException("Finish prepared native BedWars setup before renewal commit");
        renewing=renewalPrepared=false;renewalSpawns=Map.of();
    }
    private void activate(){
        var level=game.maps().level();var markers=new ArrayList<LbcBedwarsArena.Marker>();var generators=new ArrayList<LbcBedwarsAuxiliaries.Generator>();
        for(var e:placed){if(e.isRemoved())continue;var tags=e.entityTags();LbcBedwarsArena.MarkerKind kind=null;
            if(tags.contains("bedwars_bed"))kind=LbcBedwarsArena.MarkerKind.BED;else if(tags.contains("swrg.spawn"))kind=LbcBedwarsArena.MarkerKind.SPAWN;else if(shopRole(e))kind=LbcBedwarsArena.MarkerKind.SHOP;else if(tags.contains("swrg.look"))kind=LbcBedwarsArena.MarkerKind.LOOK;
            for(int n=0;n<GENERATORS.length;n++)if(tags.contains(GENERATORS[n])){kind=LbcBedwarsArena.MarkerKind.valueOf(LbcBedwarsAuxiliaries.Kind.values()[n].name());generators.add(new LbcBedwarsAuxiliaries.Generator(e.getUUID(),level,e.position(),LbcBedwarsAuxiliaries.Kind.values()[n]));break;}
            if(kind!=null)markers.add(new LbcBedwarsArena.Marker(e.getUUID(),level,e.blockPosition(),kind,kind==LbcBedwarsArena.MarkerKind.BED?color(level.getBlockState(e.blockPosition())):0));
        }
        arena.activate(new LbcBedwarsArena.Topology(level,bounds,markers,sites.entrySet().stream().filter(e->beds.contains(e.getKey())).map(e->e.getValue().bed).toList()));
        if(renewing)auxiliaries.replaceMap(generators);else auxiliaries.initialize(generators);
        activated=true;placed=List.of();
    }
    public void tick(){
        if(!active()||restoring||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(game.maps().level()))return;
        if(!activated){
            long end=System.nanoTime()+2_000_000;int budget=512;var level=game.maps().level();
            while(!setup.isEmpty()&&budget-->0&&System.nanoTime()<end){
                var w=setup.peekFirst();if(!level.hasChunkAt(w.position)){game.maps().prepareChunk(new net.minecraft.world.level.ChunkPos(w.position.getX()>>4,w.position.getZ()>>4));break;}
                boolean same=level.getBlockState(w.position)==w.state;
                if(!same&&!level.setBlock(w.position,w.state,Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS))
                    throw new IllegalStateException("Accepted BedWars setup write was rejected");
                // Include unchanged final cells in the canonical type capture as well.
                arena.captureCell(w.position,level.getBlockState(w.position));setup.removeFirst();
            }
            if(setup.isEmpty()&&(!renewing||renewalPrepared))activate();return;
        }
        presentTeams=0;for(var id:game.activeMembersForBedwars()){var p=game.server().getPlayerList().getPlayer(id);if(p!=null)presentTeams|=1<<team(p);}
        auxiliaries.tick();
        for(var id:List.copyOf(respawns.keySet())){var p=game.server().getPlayerList().getPlayer(id);if(p==null){respawns.remove(id);continue;}if(!hasBed(p)){respawns.remove(id);game.bedwarsCancelRespawn(id);continue;}int ticks=Math.max(0,respawns.get(id)-1);respawns.put(id,ticks);if(ticks>0&&ticks%20==0)respawnTitle(p,ticks);if(ticks==0&&p.isAlive()&&game.bedwarsRespawnReady(id))respawns.remove(id);}
    }
    public int winnerTeam(){
        if(!active()||!activated)return -1;var live=new LinkedHashSet<Integer>();
        for(var id:game.activeMembersForBedwars()){var p=game.server().getPlayerList().getPlayer(id);if(p!=null&&(hasBed(p)||p.isAlive()&&!p.isSpectator()&&!game.isObserver(p))){int team=team(p);if(team>0)live.add(team);}}
        return live.size()>1?-1:live.isEmpty()?0:live.iterator().next();
    }
    private void respawnTitle(ServerPlayer p,int ticks){
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable("you_died")));
        var subtitle=ticks==100?Component.translatable("respawn_in_5s"):Component.empty().append(Component.translatable("respawn_in")).append(" "+((ticks+19)/20));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));
    }
    private void lostBed(int number){if(!beds.remove(number))return;for(var p:game.server().getPlayerList().getPlayers())if(team(p)==number){p.entityTags().remove("have_bed");p.sendSystemMessage(Component.translatable("no_bed"));}}
    public void stop(){for(var p:game.server().getPlayerList().getPlayers())p.entityTags().remove("have_bed");shop.stop();auxiliaries.stop();arena.stop();running=activated=false;epoch=null;assignments.clear();beds.clear();respawns.clear();setup.clear();presentTeams=0;placed=List.of();sites.clear();bounds=List.of();restoring=false;renewing=renewalPrepared=false;renewalSpawns=Map.of();}
    final class Checkpoint {
        final String savedEpoch=epoch;final boolean savedRunning=running,savedActivated=activated,savedRenewing=renewing,savedRenewalPrepared=renewalPrepared;
        final Map<UUID,MapManager.Spawn> savedRenewalSpawns=Map.copyOf(renewalSpawns);
        final Map<Integer,Site> savedSites=Map.copyOf(sites);final Map<UUID,Integer> savedAssignments=Map.copyOf(assignments);
        final Set<Integer> savedBeds=Set.copyOf(beds);final Map<UUID,Integer> savedRespawns=Map.copyOf(respawns);
        final List<Write> savedSetup=List.copyOf(setup);final List<LbcBedwarsArena.Bounds> savedBounds=List.copyOf(bounds);final int savedPresent=presentTeams;
    }
    Checkpoint checkpoint(){return new Checkpoint();}
    void restore(Checkpoint c){shop.stop();epoch=c.savedEpoch;running=c.savedRunning;activated=c.savedActivated;renewing=c.savedRenewing;renewalPrepared=c.savedRenewalPrepared;renewalSpawns=c.savedRenewalSpawns;sites.clear();sites.putAll(c.savedSites);assignments.clear();assignments.putAll(c.savedAssignments);beds.clear();beds.addAll(c.savedBeds);respawns.clear();respawns.putAll(c.savedRespawns);setup.clear();setup.addAll(c.savedSetup);bounds=c.savedBounds;presentTeams=c.savedPresent;placed=List.of();}
}
