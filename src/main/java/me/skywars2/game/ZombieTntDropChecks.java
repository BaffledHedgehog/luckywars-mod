package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded native fixtures. Fatal attribution uses disposable mobs, never match participants. */
public final class ZombieTntDropChecks {
    private ZombieTntDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Zombie> stopActors=List.of();
    private static List<PrimedTnt> stopTnt=List.of();
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Zombie TNT drop: "+message);}
    private static net.minecraft.nbt.CompoundTag saved(Entity actor,ServerLevel level){
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        actor.saveWithoutId(out);return out.buildResult();
    }
    private static ItemEntity token(ServerLevel level,Vec3 at,ItemStack stack,LivingEntity author){
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);if(author!=null)item.setThrower(author);
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native item admission");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final net.minecraft.world.scores.PlayerTeam alphaTeam,betaTeam;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Set<Long> forced=new HashSet<>();final List<ItemEntity> tokens=new ArrayList<>();
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final Vec3 origin;final boolean griefing,tntExplodes,mobDrops;final int protection,alphaKills,betaKills;final double armor;
        ItemEntity stack,paused,unloaded;Zombie carrier;PrimedTnt passenger;
        Vec3 naturalStart,frozenPos,frozenTntPos,frozenMotion;int naturalFuse,frozenFuse;
        int phase,lastTick,wait;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two actual active match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            var board=game.server().getScoreboard();alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());betaTeam=board.getPlayersTeam(beta.getScoreboardName());
            armor=beta.getAttribute(Attributes.ARMOR).getBaseValue();protection=game.config().tntProtection;
            alphaKills=kills(alpha);betaKills=kills(beta);
            griefing=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
            tntExplodes=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES);
            mobDrops=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS);
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);
            level.getAllEntities().forEach(existing::add);
        }
        int kills(ServerPlayer p){return p.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));}
        void initialize(){
            var center=BlockPos.containing(origin);
            for(var p:BlockPos.betweenClosed(center.offset(-10,-10,-10),center.offset(10,10,10))){
                var pos=p.immutable();long key=net.minecraft.world.level.ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4);
                if(!level.getForceLoadedChunks().contains(key)){forced.add(key);level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}
                level.getChunkAt(pos);blocks.put(pos,game.maps().snapshot(pos));
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            }
            game.items().specialDrops().stop();clear();var board=game.server().getScoreboard();
            board.removePlayerFromTeam(alpha.getScoreboardName());board.removePlayerFromTeam(beta.getScoreboardName());
            for(var player:List.of(alpha,beta)){
                player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);
            }
            beta.getAttribute(Attributes.ARMOR).setBaseValue(0);game.config().tntProtection=0;
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,true,game.server());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,true,game.server());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,true,game.server());
            away();presentation();game.command("give TestAlpha skywars2:zombie_tnt_drop 1");lastTick=game.server().getTickCount();
        }
        void away(){alpha.teleportTo(level,origin.x,origin.y+25,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+25,origin.y,origin.z,Set.of(),0,0,true);}
        void presentation(){
            var p=new ItemStack(LbcZombieTntDropItem.TOKEN);var data=p.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr("zombietnt",0)==1 && data.getIntOr("drop",0)==1 && LbcZombieTntDropItem.matches(p),"exact native source flags");
            require(Identifier.parse("lbc:drop_trap").equals(p.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_ff").equals(p.get(DataComponents.TOOLTIP_STYLE)),"source model/tier");
            require(p.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals("trap_tnt_zombies")
                    && !p.getHoverName().getStyle().isItalic() && p.getHoverName().getStyle().getColor().getValue()==0xffff55,"source translated yellow name");
            var lore=p.getOrDefault(DataComponents.LORE,ItemLore.EMPTY).lines();
            require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc")
                    && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"source white lore");
            var loot=game.lbc().rollTable(level,origin,alpha,"lbc:ported/zombie_tnt_drop");
            require(loot.size()==1 && loot.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(p,loot.getFirst()),"complete ported/native presentation equality");
        }
        ItemEntity drop(int count,LivingEntity author){var t=token(level,origin,new ItemStack(LbcZombieTntDropItem.TOKEN,count),author);tokens.add(t);return t;}
        List<Zombie> live(){return game.items().zombieTntDrops().snapshot().stream().filter(e->!e.isRemoved()).toList();}
        List<PrimedTnt> explosives(){return game.items().zombieTntDrops().snapshotTnt().stream().filter(e->!e.isRemoved()).toList();}
        void clear(){game.items().zombieTntDrops().stop();}
        PrimedTnt mounted(Zombie zombie){require(zombie.getPassengers().size()==1 && zombie.getFirstPassenger() instanceof PrimedTnt,"one actual native TNT passenger");return (PrimedTnt)zombie.getFirstPassenger();}
        List<Zombie> batch(LivingEntity author){
            clear();var t=drop(1,author);t.setTarget(beta.getUUID());t.setYRot(37);t.setXRot(23);
            require(game.items().zombieTntDrops().ready(t) && game.items().zombieTntDrops().spawn(t),"native service admission");
            var actors=live();require(actors.size()==4 && explosives().size()==4,"four native baby zombie/TNT pairs");
            var expected=new ArrayList<>(List.of(origin.add(1,0,0),origin.add(-1,0,0),origin.add(0,0,1),origin.add(0,0,-1)));
            for(var zombie:actors){
                int index=-1;for(int i=0;i<expected.size();i++)if(zombie.position().distanceTo(expected.get(i))<1e-8){index=i;break;}
                require(index>=0,"exact cardinal fractional source position");expected.remove(index);
                require(zombie.isBaby() && !zombie.isNoAi() && !zombie.isNoGravity() && zombie.getYRot()==0 && zombie.getXRot()==0,"native baby, ordinary AI/gravity, source zero orientation");
                require(zombie.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==-10 && zombie.getAttributeValue(Attributes.ATTACK_DAMAGE)==-10,"source minus ten survives global finite attribute bounds override");
                require(zombie.getHealth()==20 && zombie.getDeltaMovement().equals(Vec3.ZERO),"native default health/motion");
                var tnt=mounted(zombie);
                require(tnt.getVehicle()==zombie && tnt.getFuse()==200 && !tnt.isNoGravity() && tnt.getDeltaMovement().equals(Vec3.ZERO),"native riding relation, fuse200, zero default motion/gravity");
                require(tnt.getOwner()==zombie && game.items().zombieTntDrops().owner(zombie)==zombie,"actual thrower independent of pickup owner");
                var tag=saved(zombie,level);var passengers=tag.getList("Passengers").orElseThrow();
                require(passengers.size()==1 && saved(tnt,level).getShortOr("fuse",(short)-1)==200,"native entity serialization retains mounted TNT/fuse200");
                require(Math.abs(tnt.getY()-zombie.getY()-.98)<1e-6 && Math.abs(tnt.getX()-zombie.getX())<1e-8 && Math.abs(tnt.getZ()-zombie.getZ())<1e-8,"native baby passenger attachment placement");
                require(game.items().currentEpoch(zombie) && game.items().currentEpoch(tnt),"carrier and passenger current epoch");
                zombie.setNoAi(true);zombie.setNoGravity(true);
            }
            require(t.getYRot()==37 && t.getXRot()==23,"token orientation unchanged");t.discard();return actors;
        }
        PrimedTnt single(){var actors=batch(alpha);var selected=actors.getFirst();var result=mounted(selected);
            for(var other:actors)if(other!=selected){mounted(other).discard();other.discard();}
            result.stopRiding();selected.setPos(origin.add(0,50,0));result.setPos(origin);result.setNoGravity(true);result.setDeltaMovement(Vec3.ZERO);return result;
        }
        float blast(int protection,boolean reflected){
            var tnt=single();var nativeOwner=tnt.getOwner();game.config().tntProtection=protection;if(reflected)game.items().electro().assignOwner(tnt,beta);
            beta.teleportTo(level,origin.x+1,origin.y,origin.z,Set.of(),0,0,true);beta.setHealth(1000);beta.setAbsorptionAmount(0);beta.setDeltaMovement(Vec3.ZERO);
            beta.setInvulnerableTime(0);beta.damageCooldownTime=0;tnt.setFuse(1);tnt.tick();
            require(tnt.isRemoved(),"actual native final fuse explosion");float loss=1000-beta.getHealth();
            if(loss>0)require(beta.getLastDamageSource()!=null && beta.getLastDamageSource().getDirectEntity()==tnt && beta.getLastDamageSource().getEntity()==(reflected?beta:nativeOwner),"native blast retains reflection-first thrower");return loss;
        }
        net.minecraft.world.entity.animal.cow.Cow cow(Vec3 at){
            var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,level);
            cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);require(level.addFreshEntity(cow),"disposable mob admitted");return cow;
        }
        void lethal(boolean reflected){
            away();var tnt=single();var owner=reflected?beta:alpha;var expected=reflected?beta:tnt.getOwner();if(reflected)game.items().electro().assignOwner(tnt,beta);
            var cow=cow(origin.add(1,0,0));int before=kills(owner);
            try{cow.setHealth(1);tnt.setFuse(1);tnt.tick();require(cow.isDeadOrDying() && cow.getKillCredit()==expected && kills(owner)==before+(reflected?1:0),"native TNT death credits causal/reflected player and builtin mob kill");}
            finally{game.items().indirectKills().clear(cow);cow.discard();clear();}
        }
        void roundTrip(){
            for(boolean reflected:List.of(false,true)){
                var original=single();LivingEntity expected=reflected?beta:original.getOwner();if(reflected)game.items().electro().assignOwner(original,beta);
                var tag=saved(original,level);require(tag.contains("owner"),"native serialized causal owner");
                var restored=new PrimedTnt(net.minecraft.world.entity.EntityTypes.TNT,level);
                try{restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
                    require(restored.getOwner()==expected && restored.getFuse()==200 && LbcZombieTntDrops.managed(restored) && game.items().currentEpoch(restored),"native TNT save/load retains original/reflected owner, fuse and epoch");
                }finally{restored.discard();clear();}
            }
        }
        void chain(){
            away();var parent=single();var nativeOwner=parent.getOwner();var pos=BlockPos.containing(origin).east();level.setBlock(pos,Blocks.TNT.defaultBlockState(),3);
            parent.setFuse(1);parent.tick();var children=explosives();
            require(parent.isRemoved() && level.getBlockState(pos).isAir() && children.size()==1,"actual native blast primes one native TNT block");
            var child=children.getFirst();require(child.getOwner()==nativeOwner && LbcZombieTntDrops.managed(child) && game.items().currentEpoch(child) && child.getFuse()>0 && child.getFuse()<80 && !child.isNoGravity(),"native chain inherits cause/epoch while retaining fuse/gravity");
            clear();require(child.isRemoved(),"quiet stop removes delayed native chain");
        }
        void melee(){
            var actors=batch(alpha);var actor=actors.getFirst();var victim=cow(actor.position().add(1,0,0));
            try{
                float health=victim.getHealth();victim.setDeltaMovement(Vec3.ZERO);boolean hit=actor.doHurtTarget(level,victim);
                require(victim.getHealth()==health,"source native zero attack never subtracts health");
                require(!hit && victim.getDeltaMovement().equals(Vec3.ZERO) && victim.getLastDamageSource()==null,
                        "author requires fully disabled melee: no native hit, knockback or damage-source side effects");
            }finally{game.items().indirectKills().clear(victim);victim.discard();clear();}
        }
        void carrierLifecycle(){
            away();var actors=batch(alpha);var source=actors.getFirst();var tnt=mounted(source);
            for(var other:actors)if(other!=source){mounted(other).discard();other.discard();}
            var drowned=source.convertTo(net.minecraft.world.entity.EntityTypes.DROWNED,
                    net.minecraft.world.entity.ConversionParams.single(source,true,true),net.minecraft.world.entity.EntitySpawnReason.CONVERSION,converted->{});
            require(drowned!=null && source.isRemoved() && drowned.isBaby() && LbcZombieTntDrops.managed(drowned)
                    && game.items().zombieTntDrops().owner(drowned)==drowned && game.items().currentEpoch(drowned),"actual native conversion retains baby carrier cause/epoch");
            require(tnt.getVehicle()==drowned && tnt.getFuse()==200 && tnt.getOwner()==drowned,"native conversion transfers the same live TNT passenger");
            clear();require(drowned.isRemoved() && tnt.isRemoved(),"quiet cleanup tracks converted carrier/passenger");
            actors=batch(alpha);source=actors.getFirst();tnt=mounted(source);
            for(var other:actors)if(other!=source){mounted(other).discard();other.discard();}
            source.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND));
            source.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,2);
            Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());level.getAllEntities().forEach(before::add);
            require(source.hurtServer(level,source.damageSources().generic(),1000) && source.isDeadOrDying(),"ordinary carrier combat death remains native");
            require(!tnt.isRemoved() && tnt.getFuse()==200,"carrier death does not disarm or prematurely detonate native passenger");
            require(java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).anyMatch(e->!before.contains(e) && e instanceof ItemEntity item && item.getItem().is(Items.DIAMOND)),"ordinary native death equipment loot is preserved");
            for(int i=0;i<25 && !source.isRemoved();i++)source.tick();
            require(source.isRemoved() && !tnt.isRemoved() && tnt.getVehicle()==null,"native death removal releases live TNT");
            tnt.tick();require(tnt.getFuse()==199,"released TNT continues its own native fuse");clear();
        }
        void rolesAndProtection(){
            float baseline=blast(0,false);require(baseline>0,"native unprotected TNT harms active participant");
            for(int i=1;i<=5;i++)require(Math.abs(blast(i,false)-baseline*(1-i/5f))<.03f,"native/global TNT protection exactly once at level "+i);
            game.config().tntProtection=0;
            for(String role:List.of("spectator","magic_academy","sauvojen_tuntija","strict_map_object")){
                boolean added=beta.entityTags().add(role);try{require(blast(0,false)==0,"native blast excludes role "+role);}finally{if(added)beta.entityTags().remove(role);}
            }
            var board=game.server().getScoreboard();require(board.getPlayerTeam("codex_zombie_tnt_probe")==null,"fixture team available");var team=board.addPlayerTeam("codex_zombie_tnt_probe");
            try{board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                float loss=blast(0,false);require(loss>0,"author-selected WILD native blast harms allied participants");
            }finally{board.removePlayerTeam(team);}
            require(blast(0,true)>0,"reflected native TNT credits and harms its new owner");clear();away();
        }
        boolean step(){
            require(game.playing(),"fixture preserves actual match");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{require(alpha.getInventory().getItem(0).is(LbcZombieTntDropItem.TOKEN) && live().isEmpty(),"give/fake pickup visual cannot activate");phase++;}
                case 1->{
                    require(live().isEmpty(),"held token cannot activate");alpha.getInventory().clearContent();
                    var fake=new ItemStack(Items.PAPER);fake.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:drop_trap"));var f=token(level,origin,fake,alpha);tokens.add(f);
                    require(!game.items().specialDrops().spawn(f),"plain visual fake rejected");f.discard();
                    var visual=drop(1,alpha);visual.makeFakeItem();require(!game.items().specialDrops().spawn(visual),"native give fake rejected");visual.discard();
                    stack=drop(3,alpha);phase++;
                }
                case 2->{require(!stack.isRemoved() && stack.getItem().getCount()==2 && live().size()==4,"stack3 one native unit per tick");require(!game.items().specialDrops().spawn(stack) && stack.getItem().getCount()==2,"same server tick dispatcher cannot debit twice");clear();phase++;}
                case 3->{require(!stack.isRemoved() && stack.getItem().getCount()==1 && live().size()==4,"stack2 one native unit next tick");clear();phase++;}
                case 4->{
                    require(stack.isRemoved() && live().size()==4,"last unit removes source token");batch(null);clear();
                    var actors=batch(alpha);carrier=actors.getFirst();passenger=mounted(carrier);
                    for(var other:actors)if(other!=carrier){mounted(other).discard();other.discard();}
                    carrier.setNoAi(false);carrier.setNoGravity(false);naturalStart=carrier.position();naturalFuse=passenger.getFuse();wait=0;phase++;
                }
                case 5->{
                    require(!carrier.isRemoved() && !passenger.isRemoved() && passenger.getVehicle()==carrier,"native server ticks retain TNT riding relation");
                    require(passenger.getFuse()<naturalFuse && passenger.getFuse()>190,"actual native server ticks decrement mounted fuse");
                    require(passenger.position().distanceTo(carrier.position())<2,"native passenger placement follows baby carrier");
                    if(++wait>=4){require(carrier.getY()<naturalStart.y,"natural server gravity moves carrier with native passenger");
                        frozenPos=carrier.position();frozenTntPos=passenger.position();frozenMotion=carrier.getDeltaMovement();frozenFuse=passenger.getFuse();
                        game.items().timeStop().begin(level,200);paused=drop(2,alpha);wait=0;phase++;}
                }
                case 6->{
                    require(!paused.isRemoved() && paused.getItem().getCount()==2,"time stop preserves pending stack");
                    require(passenger.getFuse()==frozenFuse && passenger.position().distanceTo(frozenTntPos)<1e-8 && carrier.position().distanceTo(frozenPos)<1e-8 && passenger.getVehicle()==carrier,"time stop freezes carrier, mounted position and fuse");
                    if(++wait>=3){game.items().timeStop().end(level);require(carrier.getDeltaMovement().distanceTo(frozenMotion)<1e-8,"time stop restores carrier native motion");paused.discard();clear();unloaded=drop(1,alpha);game.items().specialDrops().unloaded(unloaded);phase++;}
                }
                case 7->{
                    require(!unloaded.isRemoved() && live().isEmpty(),"unloaded pending token cannot activate");unloaded.discard();melee();rolesAndProtection();lethal(false);lethal(true);roundTrip();chain();carrierLifecycle();phase++;
                }
                case 8->{
                    var author=cow(origin.add(0,25,0));try{var t=drop(1,author);t.setTarget(beta.getUUID());author.discard();
                        require(game.items().zombieTntDrops().spawn(t),"departed captured actual author still activates");t.discard();
                        require(live().size()==4 && live().stream().allMatch(z->game.items().zombieTntDrops().owner(z)==z) && explosives().stream().allMatch(tnt->tnt.getOwner()==tnt.getVehicle()),"capture retains departed author for carriers/passengers");
                    }finally{author.discard();clear();}
                    var stale=drop(1,alpha);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().zombieTntDrops().ready(stale) && !game.items().zombieTntDrops().spawn(stale),"stale epoch rejects before actor admission");stale.discard();
                    var remote=drop(2,alpha);int x=(BlockPos.containing(origin).getX()>>4)+128,z=(BlockPos.containing(origin).getZ()>>4)+128;
                    while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;
                    remote.setPos(x*16+8.37,origin.y,z*16+8.81);
                    require(!game.items().zombieTntDrops().ready(remote) && !game.items().zombieTntDrops().spawn(remote) && level.getChunkSource().getChunkNow(x,z)==null && remote.getItem().getCount()==2 && live().isEmpty(),"unresident payload guard neither loads, partially emits nor debits");remote.discard();
                    var quiet=batch(alpha);var pending=explosives();Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());level.getAllEntities().forEach(before::add);
                    clear();require(quiet.stream().allMatch(actor->actor.isRemoved() && actor.getHealth()==20) && pending.stream().allMatch(Entity::isRemoved),"quiet cleanup discards living carriers/TNT without combat death");
                    for(var entity:level.getAllEntities())require(before.contains(entity),"quiet cleanup inserts no explosion loot, death items or XP");
                    game.items().specialDrops().stop();phase++;
                }
                case 9->{require(live().isEmpty() && explosives().isEmpty(),"no delayed source activation after dispatcher stop");return true;}
                default->throw new IllegalStateException("Unknown zombie TNT phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();clear();
            var reclaim=new ArrayList<Entity>();for(var entity:level.getEntities((Entity)null,new net.minecraft.world.phys.AABB(origin,origin).inflate(11)))if(!existing.contains(entity) && !(entity instanceof ServerPlayer))reclaim.add(entity);
            reclaim.forEach(Entity::discard);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            for(long key:forced)level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,griefing,game.server());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,mobDrops,game.server());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES,tntExplodes,game.server());game.config().tntProtection=protection;
            beta.getAttribute(Attributes.ARMOR).setBaseValue(armor);var board=game.server().getScoreboard();
            if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);
            alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaKills);
            beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaKills);
            alphaState.restore(game);betaState.restore(game);
        }
    }
    public static void run(GameController game){require(running==null,"already running");running=new State(game);
        try{running.initialize();}catch(RuntimeException|Error error){try{running.close();}finally{running=null;}throw error;}}
    public static void tick(GameController game){if(running==null)return;
        try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS zombie-tnt-drops");}}
        catch(Throwable error){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL zombie-tnt-drops",error);}}
    public static void stopProbe(GameController game){
        require(game.playing(),"stop probe requires actual match");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        game.items().timeStop().end(level);stopEpoch=game.items().epoch();stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),new ItemStack(LbcZombieTntDropItem.TOKEN,3),alpha);
        require(game.items().zombieTntDrops().spawn(stopToken),"live stop probe native batch");stopActors=game.items().zombieTntDrops().snapshot();stopTnt=game.items().zombieTntDrops().snapshotTnt();
        require(stopActors.size()==4 && stopTnt.size()==4,"four living carriers and mounted TNT before real match stop");
        for(var actor:stopActors){actor.setNoAi(true);actor.setNoGravity(true);}game.items().timeStop().begin(level,200);SkyWars2.LOGGER.info("[check] PASS zombie-tnt-drops-stop-probe");
    }
    public static void cleanup(GameController game){
        require(!game.items().currentEpoch(stopEpoch) && game.items().zombieTntDrops().trackedCount()==0 && game.items().zombieTntDrops().snapshotTnt().isEmpty() && game.items().specialDrops().pendingCount()==0,"actual stop invalidates epoch and clears actor/dispatcher services");
        require(stopActors.stream().allMatch(z->z.isRemoved() && z.getHealth()==20) && stopTnt.stream().allMatch(Entity::isRemoved),"actual stop quietly discards living carriers and native passengers");
        if(stopToken!=null)stopToken.discard();stopToken=null;stopActors=List.of();stopTnt=List.of();stopEpoch=null;SkyWars2.LOGGER.info("[check] PASS zombie-tnt-drops-cleanup");
    }
}
