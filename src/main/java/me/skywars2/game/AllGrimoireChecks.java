package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Complete remaining catalog, actual consumed input and random-only dependency coverage. */
public final class AllGrimoireChecks {
    private AllGrimoireChecks() { }
    private static void require(boolean b,String why){if(!b)throw new IllegalStateException("All grimoires: "+why);}
    static final class Fixture implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture base;final net.minecraft.server.level.ServerLevel level;final Vec3 at;
        final net.minecraft.server.level.ServerPlayer beta;final GameChecks.WandThirdPlayerState betaSaved;
        final Map<BlockPos,MapManager.BlockSnapshot> cells=new LinkedHashMap<>();
        final Map<net.minecraft.world.item.Item,Integer> stats=new IdentityHashMap<>();
        final Map<String,Integer> scores=new LinkedHashMap<>();final Set<String> absent=new HashSet<>();
        final Set<Long> forced=new HashSet<>();boolean restoreWorld=true,retain;
        Fixture(GameController game){this(game,false);}
        Fixture(GameController game,boolean existingStopProbes){
            base=new ElementalGrimoireChecks.Fixture(game,existingStopProbes);base.initialize();level=base.owner.level();at=base.owner.position();
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(beta!=null,"second participant");betaSaved=new GameChecks.WandThirdPlayerState(beta,base.mana);
            beta.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);beta.setHealth(1_000_000);
            for(var objective:game.server().getScoreboard().getObjectives()){
                var info=game.server().getScoreboard().getPlayerScoreInfo(base.owner,objective);
                if(info==null)absent.add(objective.getName());else scores.put(objective.getName(),info.value());
            }
            for(var d:LbcLibrarySpells.definitions())stats.put(d.item(),base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(d.item())));
            var center=BlockPos.containing(at);
            for(int x=(center.getX()>>4)-2;x<=(center.getX()>>4)+2;x++)for(int z=(center.getZ()>>4)-2;z<=(center.getZ()>>4)+2;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(x,z,true);}level.getChunk(x,z);
            }
            for(var pos:BlockPos.betweenClosed(center.offset(-16,-8,-16),center.offset(16,12,20)))cells.put(pos.immutable(),game.maps().snapshot(level,pos));
        }
        void world(){
            for(var e:cells.entrySet())if(!level.getBlockState(e.getKey()).equals(e.getValue().state())||level.getBlockEntity(e.getKey())!=null)
                base.game.maps().restoreSnapshot(level,e.getKey(),e.getValue());
            level.setBlock(BlockPos.containing(at.add(0,base.owner.getEyeHeight(),8)),Blocks.STONE.defaultBlockState(),2|16|32);
        }
        void reset(){
            var i=base.game.items();i.libraryRandom().stop();i.bedrock().stop();i.evokerBass().stop();i.lifestealer().stop();i.advancedSnow().stop();i.neutral().stop();i.prism().stop();
            i.soulless().stop();i.halloweenWind().stop();i.fartus().stop();i.summons().stop();i.reactive().stop();
            i.hedgehog().stop();i.sheep().stop();i.homing().stop();i.bolts().stop();i.blaze().stop();i.psycho().stop();i.exoperso().stop();i.exasperato().stop();i.acidCorruption().stop();
            i.consuming().stop();i.duplicating().stop();i.awakenedAcid().stop();i.erecto().stop();i.danmaku().stop();i.iceWand().stop();i.vexSword().stop();i.stonekivi().stop();base.clear();
            var fresh=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!base.before.contains(e))fresh.add(e);fresh.forEach(Entity::discard);
            base.owner.removeAllEffects();base.owner.clearFire();base.owner.entityTags().remove("nomagic_active");i.bolts().clearChainStatus(base.owner);i.magic().colors().clearEffects(base.owner);
            base.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);base.owner.setHealth(1_000_000);base.owner.setAbsorptionAmount(0);base.owner.setInvulnerableTime(0);base.owner.damageCooldownTime=0;
            base.owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);base.owner.setDeltaMovement(Vec3.ZERO);base.owner.setNoGravity(true);base.owner.setShiftKeyDown(false);
            base.mana.noMagic(base.owner,0);base.mana.score(base.owner,"reverse_mana").set(0);base.mana.score(base.owner,"ray").set(0);
            if(restoreWorld)world();restoreWorld=false;base.recording.packets.clear();
        }
        ItemStack loot(LbcLibrarySpells.Definition d){var list=base.game.lbc().rollTable(level,at,base.owner,"lbc:ported/"+d.id());require(list.size()==1,"one narrow item "+d.id());return list.getFirst();}
        void presentation(LbcLibrarySpells.Definition d){
            var a=loot(d);var b=d.item().getDefaultInstance();require(a.is(d.item())&&a.getMaxStackSize()==99&&LbcLibrarySpells.kind(a)==d,"native identity "+d.id());
            for(var component:List.of(DataComponents.CUSTOM_NAME,DataComponents.LORE,DataComponents.CUSTOM_DATA,DataComponents.ITEM_MODEL,DataComponents.TOOLTIP_STYLE))
                require(Objects.equals(a.get(component),b.get(component)),"exact source/default component "+d.id()+" "+component);
        }
        void payload(int id){var i=base.game.items();
            if(id>=28&&id<=111){var effect=LbcLibrarySpells.fixedEffect(id);
                if(id>=109)require(base.mana.hudValue(base.owner,"effect_rtp_"+(id-108))==effect.ticks(),"native RTP tier counter "+id);
                else if(!Set.of("instant_health","instant_damage","saturation").contains(effect.id())){
                    var type=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(net.minecraft.resources.Identifier.withDefaultNamespace(effect.id())).orElseThrow();
                    var applied=base.owner.getEffect(type);require(applied!=null&&applied.getAmplifier()==effect.amplifier()&&applied.getDuration()==effect.ticks(),"exact fixed Badwiki effect/amplifier/duration "+id);
                }
            }
            if(id==3)require(i.evokerBass().snapshot().size()==36,"ring creates36 native fangs");
            if(id==4)require(i.evokerBass().snapshot().size()==51,"fan creates51 native fangs");
            if(id==19)require(i.lifestealer().count()==1,"one owned vampire charge");
            if(id>=129&&id<=133)require(i.hedgehog().helpers().size()>=1,"native contact hedgehog "+id);
            if(id==141||id==142)require(i.wands().electroCount()==(id==141?4:1),"source Tesla arc count "+id);
            if(id==144||id==145)require(i.clouds().acidSnapshot().size()==8&&i.clouds().acidSnapshot().stream().allMatch(p->p.entityTags().contains("on_fire")== (id==145)),"eight native acid charges and correct ignition");
            if(id>=149&&id<=153)require(i.neutral().snapshot().size()==(id==151?1:3),"native neutral mode count "+id);
            if(id==157||id==158)require(i.exoperso().count()==1,"one owned nuclear charge "+id);
            if(id==237||id==240)require(i.bedrock().count()==1,"one source bedrock room "+id);
            if(id==238)require(i.bedrock().cubeCount()==1,"one native bedrock cube");
        }
        void casts(){
            require(LbcLibrarySpells.definitions().size()==167,"complete remaining catalog");
            for(var d:LbcLibrarySpells.definitions()){
                presentation(d);
                for(var hand:InteractionHand.values())for(boolean shift:new boolean[]{false,true}){
                    reset();var stack=loot(d);base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                    base.owner.setItemInHand(hand,stack);base.owner.setShiftKeyDown(shift);base.mana.set(base.owner,shift?-500:0);int balance=base.mana.get(base.owner);
                    int count=base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(d.item()));
                    restoreWorld=d.spellId()==112||d.spellId()==113||d.spellId()==114||d.spellId()==115||d.spellId()>=237||d.spellId()==127||d.spellId()==128;
                    require(base.input(hand)&&stack.isEmpty()&&base.mana.get(base.owner)==balance,"actual final book/no mana gate/debit "+d.id()+" "+hand+" shift="+shift);
                    require(base.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(d.item()))==count+1,"one consumption stat "+d.id());payload(d.spellId());
                }
            }
            reset();
            for(int n=0;n<256;n++)for(var stack:base.game.lbc().rollTable(level,at,base.owner,"lbc:spells_library"))
                require(LbcLibrarySpells.kind(stack)!=null||LbcElementalGrimoires.kind(stack)!=null||LbcDanmaku.isGrimoire(stack),"complete broad library only returns registered playable books");
        }
        void random(){
            for(int roll=1;roll<=53;roll++){reset();base.mana.set(base.owner,-500);restoreWorld=true;LbcLibraryRandom.castRandom(base.game,base.owner,InteractionHand.MAIN_HAND,roll);require(base.mana.get(base.owner)==-500,"random branch without mana price "+roll);}
            for(int type:new int[]{13,16,17,18,19,20,21,22}){
                reset();base.game.items().libraryRandom().impact(base.owner,base.owner.getEyePosition().add(0,0,8),type);
                if(type==16){int count=0;for(var e:level.getAllEntities())if(!base.before.contains(e)&&e instanceof net.minecraft.world.entity.item.PrimedTnt t){require(t.getOwner()==base.owner&&t.getFuse()==56,"source delayed TNT owner/fuse");count++;}require(count==26,"26 source TNT velocities");}
            }
            reset();base.game.items().libraryRandom().impact(base.owner,base.owner.getEyePosition().add(0,0,8),22);
            for(int n=0;n<61;n++)base.game.items().libraryRandom().tick();
            int blocks=0;for(var e:level.getAllEntities())if(!base.before.contains(e)&&e instanceof net.minecraft.world.entity.item.FallingBlockEntity block){
                require(block.getBlockState().is(Blocks.STONE_BRICKS),"native falling stone state "+block.getBlockState());require(block.getDeltaMovement().y==-2,"native falling stone velocity "+block.getDeltaMovement());require(base.game.items().summons().owner(block)==base.owner,"native falling stone author");blocks++;
            }
            require(blocks==600,"falling-brick emitter releases ten per tick for source60 ticks");
            reset();float health=base.owner.getHealth();
            var wildArrow=base.game.items().vexSword().phantomShot(base.owner,base.owner,1,true);
            require(wildArrow!=null&&wildArrow.getOwner()==base.owner&&wildArrow.getPierceLevel()==100,"wild phantom selects its caster and retains owner/pierce");
            for(int n=0;n<10&&base.owner.getHealth()==health;n++)level.tickNonPassenger(wildArrow);
            require(base.owner.getHealth()<health,"native wild phantom collision damages its caster");
            reset();var board=base.game.server().getScoreboard();var ownTeam=base.owner.getTeam();var betaTeam=beta.getTeam();
            require(board.getPlayerTeam("library_wild_fixture")==null,"exclusive wild-arrow team fixture");var team=board.addPlayerTeam("library_wild_fixture");team.setAllowFriendlyFire(false);
            try{
                board.addPlayerToTeam(base.owner.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                beta.teleportTo(level,at.x,at.y,at.z+4,Set.of(),0,0,true);beta.setNoGravity(true);beta.setHealth(beta.getMaxHealth());beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
                float before=beta.getHealth();var arrow=base.game.items().vexSword().phantomShot(base.owner,beta,1,true);
                require(arrow!=null,"wild phantom selects allied player");for(int n=0;n<10&&beta.getHealth()==before;n++)level.tickNonPassenger(arrow);
                require(beta.getHealth()<before,"native wild phantom bypasses team arrow collision and friendly damage filters");
            }finally{
                board.removePlayerFromTeam(base.owner.getScoreboardName(),team);board.removePlayerFromTeam(beta.getScoreboardName(),team);board.removePlayerTeam(team);
                if(ownTeam!=null)board.addPlayerToTeam(base.owner.getScoreboardName(),ownTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);
            }
            reset();for(int kind=0;kind<9;kind++){base.game.items().libraryRandom().fly(base.owner,kind);base.game.items().libraryRandom().flyingPayload(base.owner,base.owner.getEyePosition().add(0,4,0),new Vec3(0,0,1),kind);}
            require(base.game.items().libraryRandom().count()>=9,"all9 native flying-wand models/payload factories");
        }
        void guards(){reset();for(var d:LbcLibrarySpells.definitions()){
            var stack=loot(d);stack.setCount(2);base.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            base.owner.entityTags().add("time_stopped");try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"personal stop preserves "+d.id());}finally{base.owner.entityTags().remove("time_stopped");}
            base.game.items().timeStop().begin(level,40);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"dimension stop preserves "+d.id());}finally{base.game.items().timeStop().end(level);}
            base.field=new Marker(EntityTypes.MARKER,level);base.field.setPos(at);base.field.entityTags().add("stopper_magic");
            require(level.addFreshEntity(base.field),"native antimagic fixture");base.game.items().magic().loaded(base.field);
            try{require(!base.input(InteractionHand.MAIN_HAND)&&stack.getCount()==2,"antimagic preserves "+d.id());}finally{base.removeField();}
            base.game.config().offhandWands=false;base.owner.setItemInHand(InteractionHand.OFF_HAND,stack);base.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            try{require(!base.input(InteractionHand.OFF_HAND)&&stack.getCount()==2,"disabled offhand preserves "+d.id());}finally{base.game.config().offhandWands=true;}
        }}
        @Override public void close(){if(!retain)reset();base.retain=retain;base.close();betaSaved.restore(base.game);
            for(var entry:stats.entrySet())base.owner.getStats().setValue(base.owner,net.minecraft.stats.Stats.ITEM_USED.get(entry.getKey()),entry.getValue());
            for(var objective:base.game.server().getScoreboard().getObjectives()){
                var value=scores.get(objective.getName());if(value!=null)base.mana.score(base.owner,objective.getName()).set(value);else base.game.server().getScoreboard().resetSinglePlayerScore(base.owner,objective);
            }
            if(!retain)restore();
        }
        void restore(){for(var e:cells.entrySet())base.game.maps().restoreSnapshot(level,e.getKey(),e.getValue());for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.casts();f.guards();}SkyWars2.LOGGER.info("[check] PASS all-grimoires");}
    public static void random(GameController game){try(var f=new Fixture(game)){f.random();}SkyWars2.LOGGER.info("[check] PASS all-grimoires-random");}
    private static Fixture retained;private static String epoch;
    public static void stopProbe(GameController game){require(retained==null,"exclusive retained fixture");var f=new Fixture(game,true);try{
        f.world();for(int id:new int[]{3,19,141,237,159})require(LbcLibrarySpells.cast(game,f.base.owner,InteractionHand.MAIN_HAND,id),"retained native cast "+id);
        game.items().libraryRandom().fly(f.base.owner,0);game.items().libraryRandom().impact(f.base.owner,f.at.add(0,5,8),17);
        epoch=game.items().epoch();f.retain=true;retained=f;
    }finally{f.close();}SkyWars2.LOGGER.info("[check] PASS all-grimoires-stop-probe");}
    public static void cleanup(GameController game){require(retained!=null,"retained fixture");try{
        require(!game.items().currentEpoch(epoch)&&game.items().libraryRandom().count()==0&&game.items().evokerBass().snapshot().isEmpty()
                &&game.items().lifestealer().count()==0&&game.items().bedrock().count()==0,"actual stop clears shared actors and random dependencies");
    }finally{retained.restore();retained=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS all-grimoires-cleanup");}
}
