package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded real-server fixtures. No client rendering or persistence qualification is implied. */
public final class LuckyPigDropChecks {
    private LuckyPigDropChecks() { }
    private static State running;
    private static String stopEpoch;
    private static ItemEntity stopToken;
    private static List<Pig> stopPigs=List.of();
    private static List<net.minecraft.world.entity.monster.zombie.ZombifiedPiglin> stopConverted=List.of();
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Lucky pigs: "+text);}
    private static net.minecraft.nbt.CompoundTag saved(Entity actor,ServerLevel level){
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        actor.saveWithoutId(output);return output.buildResult();
    }
    private static ItemEntity token(ServerLevel level,Vec3 at,int count,LivingEntity author){
        var item=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(LbcLuckyPigDropItem.ITEM,count));
        if(author!=null)item.setThrower(author);item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setPickUpDelay(1000);
        require(level.addFreshEntity(item),"native token admission");return item;
    }
    private static final class State implements AutoCloseable {
        final GameController game;final ServerLevel level;final ServerPlayer alpha,beta;
        final GameChecks.WandThirdPlayerState alphaState,betaState;
        final Vec3 origin;final List<ItemEntity> tokens=new ArrayList<>();
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final int alphaKills,betaKills;final boolean mobDrops;
        ItemEntity stack,paused,unloaded;Pig frozen;Vec3 frozenPos;int phase,lastTick,wait;
        State(GameController game){
            this.game=game;level=game.maps().level();alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing() && alpha!=null && beta!=null && game.allowDamage(beta),"two active match participants");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            alphaKills=kills(alpha);betaKills=kills(beta);mobDrops=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS);
            origin=new Vec3(Math.floor(alpha.getX()/16)*16+8.37,270.7,Math.floor(alpha.getZ()/16)*16+8.81);
            level.getAllEntities().forEach(existing::add);
        }
        int kills(ServerPlayer player){return player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS));}
        void initialize(){
            game.items().specialDrops().stop();clear();level.getChunkAt(BlockPos.containing(origin));
            for(var player:List.of(alpha,beta)){player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);}
            alpha.teleportTo(level,origin.x,origin.y+25,origin.z,Set.of(),0,0,true);beta.teleportTo(level,origin.x+24,origin.y,origin.z,Set.of(),0,0,true);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,true,game.server());
            presentation();rewardTable();game.command("give TestAlpha skywars2:lucky_pigs_drop 1");lastTick=game.server().getTickCount();
        }
        void clear(){game.items().luckyPigs().stop();}
        List<Pig> live(){return game.items().luckyPigs().snapshot().stream().filter(p->!p.isRemoved()).toList();}
        ItemEntity drop(int count,LivingEntity author){var item=token(level,origin,count,author);tokens.add(item);return item;}
        void presentation(){
            var stack=new ItemStack(LbcLuckyPigDropItem.ITEM);var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            require(data.size()==2 && data.getIntOr("applepig",0)==1 && data.getIntOr("drop",0)==1 && LbcLuckyPigDropItem.matches(stack),"exact source flags");
            require(Identifier.parse("lbc:drop_not_trap").equals(stack.get(DataComponents.ITEM_MODEL)) && Identifier.parse("lbc:tier_c").equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source model/tier");
            require(stack.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name && name.getKey().equals("drop_apple_pigs") && !stack.getHoverName().getStyle().isItalic() && stack.getHoverName().getStyle().getColor().getValue()==0xffff55,"source yellow translated name");
            var lore=stack.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();
            require(lore.size()==1 && lore.getFirst().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents row && row.getKey().equals("trap_desc") && !lore.getFirst().getStyle().isItalic() && lore.getFirst().getStyle().getColor().getValue()==0xffffff,"source white lore");
            var rolled=game.lbc().rollTable(level,origin,alpha,"lbc:ported/lucky_pigs_drop");
            require(rolled.size()==1 && rolled.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(stack,rolled.getFirst()),"public loot/prototype equality");
            for(var vanilla:List.of(Items.BARRIER,Items.REPEATING_COMMAND_BLOCK,Items.PAPER))require(!LbcLuckyPigDropItem.matches(new ItemStack(vanilla)),"vanilla lookalike rejected");
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public net.minecraft.network.PacketListener getPacketListener(){return alpha.connection;}};
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)connection;
            net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),alpha.registryAccess());
                require(proxy.is(Items.REPEATING_COMMAND_BLOCK) && stack.get(DataComponents.ITEM_MODEL).equals(proxy.get(DataComponents.ITEM_MODEL)) && stack.get(DataComponents.TOOLTIP_STYLE).equals(proxy.get(DataComponents.TOOLTIP_STYLE)),"actual Polymer carrier/model/tier");
                require(ItemStack.isSameItemSameComponents(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),alpha.registryAccess())),"Polymer authoritative roundtrip");
            });
        }
        void rewardTable(){
            try(var input=LuckyPigDropChecks.class.getResourceAsStream("/data/lbc/loot_table/applepig.json")){
                require(input!=null,"applepig resource exists");var json=LootDefinitionChecks.expanded(com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
                var pools=json.getAsJsonArray("pools");require(pools.size()==1,"one apple pool");var pool=pools.get(0).getAsJsonObject();require(pool.get("rolls").getAsInt()==1,"one apple roll");var entries=pool.getAsJsonArray("entries");require(entries.size()==2,"two source apples");
                for(int i=0;i<2;i++){var e=entries.get(i).getAsJsonObject();require(e.get("name").getAsString().equals(i==0?"minecraft:enchanted_golden_apple":"minecraft:golden_apple") && e.get("weight").getAsInt()==(i==0?1:10) && e.get("quality").getAsInt()==(i==0?3:0),"exact source apple weight/quality "+i);}
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Identifier.parse("lbc:applepig")));
            for(float luck:new float[]{-1,0,1000000}){
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,alpha).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,alpha.damageSources().generic()).withLuck(luck).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
                boolean enchanted=false,golden=false;
                for(int i=1;i<=64;i++){var items=table.getRandomItems(params,i*0x9E3779B97F4A7C15L);require(items.size()==1 && items.getFirst().getCount()==1,"one native apple for luck "+luck);var apple=items.getFirst();require(apple.is(Items.GOLDEN_APPLE)||apple.is(Items.ENCHANTED_GOLDEN_APPLE),"only vanilla source apples");require(ItemStack.isSameItemSameComponents(apple,new ItemStack(apple.getItem())),"no invented apple components");enchanted|=apple.is(Items.ENCHANTED_GOLDEN_APPLE);golden|=apple.is(Items.GOLDEN_APPLE);}
                require(luck<0?!enchanted:luck>0?enchanted:enchanted&&golden,"native luck changes quality-weighted selection "+luck);
            }
        }
        List<Pig> batch(LivingEntity author){
            clear();var t=drop(1,author);t.setTarget(beta.getUUID());t.setYRot(37);t.setXRot(23);
            require(game.items().luckyPigs().ready(t) && game.items().luckyPigs().spawn(t),"native service spawn");var pigs=live();require(pigs.size()==4,"four pigs");
            var expected=List.of(origin.add(1,0,0),origin.add(0,0,1),origin.add(-1,0,0),origin.add(0,0,-1));
            // Native insertion ids retain source command order even if service map iteration does not.
            var ordered=pigs.stream().sorted(Comparator.comparingInt(Entity::getId)).toList();
            for(int i=0;i<4;i++){var p=ordered.get(i);require(p.position().distanceTo(expected.get(i))<1e-8,"fractional cardinal source position/order "+i);
                require(!p.isBaby() && !p.isNoAi() && !p.isNoGravity() && p.getHealth()==10 && p.getYRot()==0 && p.getXRot()==0 && p.getDeltaMovement().equals(Vec3.ZERO),"native adult health/AI/gravity/zero rotation/motion");
                require(p.getCustomName()!=null && p.getCustomName().getContents().equals(net.minecraft.network.chat.Component.literal("Lucky Pig").getContents()) && p.getCustomName().getString().equals("Lucky Pig") && !p.getCustomName().getStyle().isItalic() && p.getCustomName().getStyle().getColor().getValue()==0xffaa00 && p.isCustomNameVisible(),"literal gold visible nonitalic name");
                require(saved(p,level).getStringOr("DeathLootTable","").equals("lbc:applepig"),"native death loot table");require(LbcLuckyPigs.managed(p) && game.items().currentEpoch(p) && game.items().luckyPigs().owner(p)==p,"actual thrower independent of pickup owner");
                p.setNoAi(true);p.setNoGravity(true);
            }require(t.getYRot()==37 && t.getXRot()==23,"source token orientation retained");t.discard();return pigs;
        }
        void deathAndConversion(){
            var pigs=batch(alpha);var victim=pigs.getFirst();Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());level.getAllEntities().forEach(before::add);int prior=kills(beta);
            require(victim.hurtServer(level,victim.damageSources().playerAttack(beta),1000) && victim.isDeadOrDying() && victim.getKillCredit()==beta && kills(beta)==prior+1,"actual native death credits actual killer and builtin statistic");
            var loot=new ArrayList<ItemStack>();for(var e:level.getAllEntities())if(!before.contains(e) && e instanceof ItemEntity item)loot.add(item.getItem());
            require(loot.size()==1 && loot.getFirst().getCount()==1 && (loot.getFirst().is(Items.GOLDEN_APPLE)||loot.getFirst().is(Items.ENCHANTED_GOLDEN_APPLE)),"native combat death emits exactly one apple and no pork");clear();
            pigs=batch(alpha);var source=pigs.getFirst();var bolt=new net.minecraft.world.entity.LightningBolt(EntityTypes.LIGHTNING_BOLT,level);source.thunderHit(level,bolt);
            var converted=game.items().luckyPigs().snapshotConverted();require(source.isRemoved() && converted.size()==1,"native lightning converts one pig");var z=converted.getFirst();
            require(LbcLuckyPigs.managed(z) && game.items().currentEpoch(z) && game.items().luckyPigs().owner(z)==z,"native conversion retains cause/epoch");
            require(z.getCustomName()!=null && z.getCustomName().equals(source.getCustomName()) && z.isCustomNameVisible(),"native conversion preserves name");
            require(!saved(z,level).getStringOr("DeathLootTable","").equals("lbc:applepig"),"conversion restores vanilla zombified piglin loot");
            convertedCombat(z);
            before.clear();level.getAllEntities().forEach(before::add);
            z.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND));z.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,2);
            require(z.hurtServer(level,z.damageSources().generic(),1000) && z.isDeadOrDying(),"converted actor still uses native combat death");
            loot.clear();for(var e:level.getAllEntities())if(!before.contains(e) && e instanceof ItemEntity item)loot.add(item.getItem());
            require(loot.stream().anyMatch(s->s.is(Items.DIAMOND)) && loot.stream().noneMatch(s->s.is(Items.GOLDEN_APPLE)||s.is(Items.ENCHANTED_GOLDEN_APPLE)||s.is(Items.PORKCHOP)),"native conversion death preserves equipment/vanilla loot without apple reward");
            var offspring=pigs.get(1).getBreedOffspring(level,pigs.get(2));require(offspring!=null,"native managed-parent breeding factory");offspring.setBaby(true);offspring.setPos(origin);require(level.addFreshEntity(offspring),"ordinary offspring admission");
            try{require(!LbcLuckyPigs.managed(offspring) && game.items().luckyPigs().owner(offspring)==null && !saved(offspring,level).getStringOr("DeathLootTable","").equals("lbc:applepig"),"ordinary offspring has no managed cause/apple table");clear();require(!offspring.isRemoved() && z.isRemoved(),"quiet stop removes conversion and preserves ordinary offspring");}finally{offspring.discard();}
            var author=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);author.setPos(origin.add(0,25,0));require(level.addFreshEntity(author),"disposable author");
            try{var t=drop(1,author);author.discard();require(game.items().luckyPigs().spawn(t) && live().size()==4 && live().stream().allMatch(p->game.items().luckyPigs().owner(p)==p),"departed captured author does not cancel pigs");t.discard();}finally{author.discard();clear();}
        }
        void convertedCombat(net.minecraft.world.entity.monster.zombie.ZombifiedPiglin z){
            var service=game.items().luckyPigs();z.setNoAi(true);z.setNoGravity(true);
            var board=game.server().getScoreboard();var alphaTeam=board.getPlayersTeam(alpha.getScoreboardName());var betaTeam=board.getPlayersTeam(beta.getScoreboardName());
            require(board.getPlayerTeam("codex_lucky_pig_probe")==null,"fixture team available");var team=board.addPlayerTeam("codex_lucky_pig_probe");
            try{
                board.removePlayerFromTeam(alpha.getScoreboardName());board.removePlayerFromTeam(beta.getScoreboardName());
                require(service.canTarget(z,beta) && service.canAffect(z,beta) && service.canTarget(z,alpha),"native converted enemy and thrower self eligible");
                z.setTarget(beta);require(z.getTarget()==beta,"native target hook admits enemy");z.setTarget(alpha);require(z.getTarget()==alpha,"native target hook admits thrower self");z.setTarget(null);
                board.addPlayerToTeam(alpha.getScoreboardName(),team);board.addPlayerToTeam(beta.getScoreboardName(),team);
                float health=beta.getHealth();var motion=beta.getDeltaMovement();z.setTarget(beta);
                require(service.canAffect(z,beta) && service.canTarget(z,beta) && z.getTarget()==beta && z.doHurtTarget(level,beta) && beta.getHealth()<health,"unowned converted Mob attacks every player regardless of team");health=beta.getHealth();beta.setInvulnerableTime(0);beta.damageCooldownTime=0;
                require(service.canTarget(z,alpha),"self remains eligible despite same team");
                board.removePlayerFromTeam(beta.getScoreboardName());
                for(String tag:List.of("spectator","magic_academy","sauvojen_tuntija","strict_map_object")){
                    boolean added=beta.entityTags().add(tag);try{z.setTarget(beta);require(!service.canTarget(z,beta) && z.getTarget()!=beta && !z.doHurtTarget(level,beta) && beta.getHealth()==health,"native converted observer/service role rejected "+tag);}finally{if(added)beta.entityTags().remove(tag);}
                }
                beta.setGameMode(GameType.SPECTATOR);try{require(!service.canTarget(z,beta) && !z.doHurtTarget(level,beta),"native spectator rejected");}finally{beta.setGameMode(GameType.SURVIVAL);beta.setNoGravity(true);}
                var cow=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,level);cow.setPos(z.position().add(1,0,0));cow.setNoAi(true);cow.setNoGravity(true);require(level.addFreshEntity(cow),"disposable converted attack victim");
                int before=kills(alpha);try{cow.setHealth(1);require(z.doHurtTarget(level,cow) && cow.isDeadOrDying() && cow.getKillCredit()==z && kills(alpha)==before,"native converted melee keeps causal owner and builtin kill statistic");}finally{game.items().indirectKills().clear(cow);cow.discard();}
            }finally{
                z.setTarget(null);board.removePlayerTeam(team);if(alphaTeam!=null)board.addPlayerToTeam(alpha.getScoreboardName(),alphaTeam);if(betaTeam!=null)board.addPlayerToTeam(beta.getScoreboardName(),betaTeam);
            }
        }
        void guards(){
            var stale=drop(2,alpha);stale.entityTags().add("skywars2.items.stale-fixture");require(!game.items().specialDrops().spawn(stale) && stale.getItem().getCount()==2,"stale epoch has no debit");stale.discard();
            var fake=drop(2,alpha);fake.makeFakeItem();require(!game.items().specialDrops().spawn(fake) && fake.getItem().getCount()==2,"fake give visual has no debit");fake.discard();
            var remote=drop(2,alpha);int x=(BlockPos.containing(origin).getX()>>4)+128,z=(BlockPos.containing(origin).getZ()>>4)+128;while(level.getChunkSource().getChunkNow(x,z)!=null)x+=128;remote.setPos(x*16+8.37,origin.y,z*16+8.81);
            require(!game.items().specialDrops().spawn(remote) && remote.getItem().getCount()==2 && level.getChunkSource().getChunkNow(x,z)==null && live().isEmpty(),"unresident source neither loads/emits/debits");remote.discard();
            var both=drop(2,alpha);var flags=both.getItem().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();flags.putInt(LbcScareDropItem.Kind.NORMAL.flag(),1);both.getItem().set(DataComponents.CUSTOM_DATA,CustomData.of(flags));
            require(game.items().specialDrops().spawn(both) && both.getItem().getCount()==1 && live().size()==4,"multi flag dispatcher emits pigs with one unit debit");require(beta.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"other matching source flag executes");beta.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);both.discard();clear();
        }
        boolean step(){
            require(game.playing(),"actual match retained");int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            switch(phase){
                case 0->{require(ItemStack.isSameItemSameComponents(alpha.getInventory().getItem(0),new ItemStack(LbcLuckyPigDropItem.ITEM)),"native give prototype");alpha.getInventory().clearContent();batch(alpha);clear();guards();deathAndConversion();stack=drop(2,alpha);phase++;}
                case 1->{require(stack.getItem().getCount()==1 && live().size()==4,"first actual tick emits four and debits one");require(!game.items().specialDrops().spawn(stack) && stack.getItem().getCount()==1,"same actual tick duplicate rejected");live().forEach(p->{p.setNoAi(true);p.setNoGravity(true);});phase++;}
                case 2->{require(stack.isRemoved() && live().size()==8,"second actual tick emits next four/final debit");clear();frozen=batch(alpha).getFirst();frozen.setNoGravity(false);frozen.setDeltaMovement(new Vec3(.1,-.1,.2));frozenPos=frozen.position();game.items().timeStop().begin(level,200);paused=drop(2,alpha);wait=0;phase++;}
                case 3->{require(paused.getItem().getCount()==2 && frozen.position().equals(frozenPos),"time stop pauses pending debit and native pig motion");if(++wait==3){paused.discard();game.items().timeStop().end(level);clear();unloaded=drop(2,alpha);game.items().specialDrops().unloaded(unloaded);phase++;}}
                case 4->{require(!unloaded.isRemoved() && unloaded.getItem().getCount()==2 && live().isEmpty(),"unloaded pending token cannot replay");unloaded.discard();var actors=batch(alpha);var actor=actors.getFirst();game.items().luckyPigs().unloaded(actor);require(!game.items().luckyPigs().snapshot().contains(actor),"unloaded pig detached from resident service");game.items().luckyPigs().loaded(actor);require(game.items().luckyPigs().snapshot().contains(actor),"current epoch loaded pig restored");actor.entityTags().removeIf(t->t.startsWith("skywars2.items."));actor.entityTags().add("skywars2.items.stale-fixture");game.items().luckyPigs().unloaded(actor);game.items().luckyPigs().loaded(actor);require(actor.isRemoved(),"stale managed actor quietly removed on load");clear();game.items().specialDrops().stop();phase++;}
                case 5->{require(live().isEmpty() && game.items().luckyPigs().snapshotConverted().isEmpty(),"no delayed activation after quiet stop");return true;}
                default->throw new IllegalStateException("Unknown lucky pig phase "+phase);
            }return false;
        }
        public void close(){
            game.items().timeStop().end(level);tokens.forEach(Entity::discard);game.items().specialDrops().stop();clear();
            var leftovers=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!existing.contains(e) && !(e instanceof ServerPlayer) && Math.abs(e.getX()-origin.x)<8 && Math.abs(e.getZ()-origin.z)<8)leftovers.add(e);leftovers.forEach(Entity::discard);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS,mobDrops,game.server());alphaState.restore(game);betaState.restore(game);
            alpha.getStats().setValue(alpha,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),alphaKills);beta.getStats().setValue(beta,net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.MOB_KILLS),betaKills);
        }
    }
    private static void closeFailure(State state,Throwable error){try{state.close();}catch(Throwable cleanup){if(error!=cleanup)error.addSuppressed(cleanup);}finally{running=null;}}
    public static void run(GameController game){require(running==null,"already running");running=new State(game);try{running.initialize();}catch(RuntimeException|Error error){closeFailure(running,error);throw error;}}
    public static void tick(GameController game){if(running==null)return;var state=running;try{if(state.step()){state.close();running=null;SkyWars2.LOGGER.info("[check] PASS lucky-pigs");}}catch(Throwable error){closeFailure(state,error);SkyWars2.LOGGER.error("[check] FAIL lucky-pigs",error);}}
    public static void stopProbe(GameController game){
        require(game.playing(),"actual match stop probe");var level=game.maps().level();var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));game.items().timeStop().end(level);stopEpoch=game.items().epoch();
        stopToken=token(level,new Vec3(alpha.getX(),280,alpha.getZ()),3,alpha);require(game.items().luckyPigs().spawn(stopToken),"stop probe live pigs");
        var source=game.items().luckyPigs().snapshot().getFirst();source.thunderHit(level,new net.minecraft.world.entity.LightningBolt(EntityTypes.LIGHTNING_BOLT,level));stopPigs=game.items().luckyPigs().snapshot();stopConverted=game.items().luckyPigs().snapshotConverted();require(stopPigs.size()==3 && stopConverted.size()==1,"native conversion before real match stop");
        for(var p:stopPigs){p.setNoAi(true);p.setNoGravity(true);}for(var p:stopConverted){p.setNoAi(true);p.setNoGravity(true);}game.items().timeStop().begin(level,200);SkyWars2.LOGGER.info("[check] PASS lucky-pigs-stop-probe");
    }
    public static void cleanup(GameController game){
        require(!game.items().currentEpoch(stopEpoch) && game.items().luckyPigs().trackedCount()==0 && game.items().luckyPigs().convertedCount()==0,"real match stop invalidates epoch and clears native services");
        require(stopPigs.stream().allMatch(p->p.isRemoved() && p.getHealth()==10) && stopConverted.stream().allMatch(p->p.isRemoved() && p.getHealth()>0),"real stop discards living pig/conversion without combat death");
        if(stopToken!=null)stopToken.discard();stopToken=null;stopEpoch=null;stopPigs=List.of();stopConverted=List.of();SkyWars2.LOGGER.info("[check] PASS lucky-pigs-cleanup");
    }
}
