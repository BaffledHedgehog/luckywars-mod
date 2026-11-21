package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;

/** Actual commands before match start, source loot and native seasonal bodies/rewards after release. */
public final class SeasonalEventChecks {
    private SeasonalEventChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("Seasonal events: "+message);}
    private static ServerPlayer alpha(GameController game){return Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));}
    private static boolean key(Component component,String key){return component!=null&&(component.getContents() instanceof TranslatableContents t&&t.getKey().equals(key)||component.getSiblings().stream().anyMatch(c->key(c,key)));}
    private static void flag(GameController game,String name,boolean enabled){
        try{require(game.server().getCommands().getDispatcher().execute("skywars settings event "+name+" "+enabled,game.server().createCommandSourceStack())==1,"real event command "+name);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
    }
    public static void prepare(GameController game){
        for(var name:List.of("halloween","november","tournament"))flag(game,name,true);
        game.items().mana().score(alpha(game),"november").set(1);
        require(game.config().eventHalloween&&game.config().eventNovember&&game.config().eventTournament,"all independent manual flags");
        SkyWars2.LOGGER.info("[check] PASS seasonal-prepare");
    }
    private static boolean seasonal(ItemStack stack){
        return ItemData.intValue(stack,"halloween_food",0)==1||ItemData.intValue(stack,"pumpkin_king",0)==1
                ||stack.is(LbcHalloweenWind.WIND)||stack.is(LbcHalloweenWind.MACE)
                ||ItemData.intValue(stack,"jack_o_lantern",0)==1||ItemData.intValue(stack,"soulcorruption",0)==1;
    }
    private static Display.ItemDisplay circle(GameController game,ServerPlayer player,Vec3 position){
        var level=player.level();
        var actor=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);
        actor.entityTags().add(LbcSeasonalEvents.SCREAMER);
        actor.setPos(position);
        game.items().markCurrentItemEpoch(actor);
        require(level.addFreshEntity(actor),"native screamer circle added");
        return actor;
    }

    private static void overlap(GameController game,ServerPlayer player) throws ReflectiveOperationException {
        var level=player.level();
        var beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
        require(beta.level()==level&&beta.isAlive()&&!beta.isSpectator()&&!game.isObserver(beta),"independent living screamer visitor");

        var service=game.items().seasonal();
        var timeStop=game.items().timeStop();
        require(!timeStop.stopped(level),"exclusive stopped-clock test");
        var tickField=GameController.class.getDeclaredField("gameTicks");
        tickField.setAccessible(true);
        int savedTick=game.matchTicks();
        int pulse=(Math.floorDiv(savedTick,20)+2)*20;
        var savedPlayer=new GameChecks.WandThirdPlayerState(player,game.items().mana());
        var savedBeta=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
        var playerRandom=MatchRandomState.capture(player.getRandom());
        var betaRandom=MatchRandomState.capture(beta.getRandom());
        var levelRandom=MatchRandomState.capture(level.getRandom());
        var profile=game.players().get(player.getUUID());
        var betaProfile=game.players().get(beta.getUUID());
        int savedMemcoins=profile.memcoins, savedBetaMemcoins=betaProfile.memcoins;
        var original=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        level.getAllEntities().forEach(original::add);

        var block=player.blockPosition();
        // Both visitors and three test circles fit in the player's already loaded chunk.
        var near=new Vec3(((block.getX()>>4)<<4)+4.5,level.getMaxY()-8,((block.getZ()>>4)<<4)+8.5);
        var far=near.add(8,0,0);
        try{
            player.teleportTo(level,near.x,near.y,near.z,Set.of(),player.getYRot(),player.getXRot(),true);
            beta.teleportTo(level,far.x,far.y,far.z,Set.of(),beta.getYRot(),beta.getXRot(),true);
            var first=circle(game,player,near.add(.75,0,0));
            var second=circle(game,player,near.add(-.75,0,0));
            var separate=circle(game,beta,far.add(.75,0,0));
            require(service.count()==28,"three real circles registered by entity load");

            tickField.setInt(game,pulse-1);
            service.tick();
            require(!first.isRemoved()&&!second.isRemoved()&&!separate.isRemoved(),"visitors wait for the one-second pulse");

            tickField.setInt(game,pulse);
            timeStop.begin(level,20);
            try{
                service.tick();
                require(!first.isRemoved()&&!second.isRemoved()&&!separate.isRemoved(),"Time Stop pauses the actual screamer pulse");
            }finally{timeStop.end(level);}

            // The native RNG seed yields 17 coins and the harmless skeleton branch.
            player.getRandom().setSeed(12);
            beta.getRandom().setSeed(12);
            service.tick();
            require(first.isRemoved()&&!second.isRemoved()&&separate.isRemoved(),
                    "the first visitor consumes only one overlapping circle and the other visitor consumes its own on the same pulse");
            require(profile.memcoins==savedMemcoins+17&&betaProfile.memcoins==savedBetaMemcoins+17,
                    "one real reward per visitor on the shared pulse");

            tickField.setInt(game,pulse+20);
            player.getRandom().setSeed(12);
            service.tick();
            require(second.isRemoved()&&profile.memcoins==savedMemcoins+34&&betaProfile.memcoins==savedBetaMemcoins+17,
                    "the same visitor can consume its remaining circle on the next pulse");
        }finally{
            tickField.setInt(game,savedTick);
            var spawned=new ArrayList<Entity>();
            level.getAllEntities().forEach(actor->{if(!original.contains(actor))spawned.add(actor);});
            spawned.forEach(Entity::discard);
            savedPlayer.restore(game);
            savedBeta.restore(game);
            playerRandom.restore(player.getRandom());
            betaRandom.restore(beta.getRandom());
            levelRandom.restore(level.getRandom());
            profile.memcoins=savedMemcoins;
            betaProfile.memcoins=savedBetaMemcoins;
            game.players().save();
        }
    }

    public static void run(GameController game){
        require(game.playing(),"real released match");var player=alpha(game);var service=game.items().seasonal();var level=player.level();
        var persisted=GameConfig.load();require(persisted.eventHalloween&&persisted.eventNovember&&persisted.eventTournament,"three flags survive disk reload");
        for(var name:List.of("halloween","november","tournament"))require(game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly(name),"lbc.event").get()==1,"actual native loot holder "+name);
        var screamers=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(entity.entityTags().contains(LbcSeasonalEvents.SCREAMER))screamers.add(entity);
        require(screamers.size()==25&&service.count()==25,"actual match start places exactly source25 native screamers");
        var columns=new HashSet<Long>();
        for(var entity:screamers){
            require(entity instanceof Display.ItemDisplay d&&d.getItemStack().get(DataComponents.ITEM_MODEL).toString().equals("lbc:strangeness"),"source screamer model");
            require(Math.abs(entity.getX())<=190&&Math.abs(entity.getZ())<=190,"source scatter bounds");
            var floor=entity.blockPosition().below();var support=level.getBlockState(floor);
            require(!support.getCollisionShape(level,floor).isEmpty()&&support.getFluidState().isEmpty(),"each screamer actually stands on map terrain");
            require(Math.abs(entity.getY()-(floor.getY()+1.01))<.00001,"source .01 height above actual support");
            columns.add(net.minecraft.world.level.ChunkPos.pack(floor.getX(),floor.getZ()));
        }
        require(columns.size()>=5,"screamers are scattered across real map columns, not stacked at0,100,0");
        require(player.getTeam()!=null&&player.getTeam().getPlayerPrefix().getString().contains("\u0414\u0436\u0435\u0434\u0430\u0439"),"actual delayed November prefix");
        var caseButton=OriginalGuiDesign.button(player,"swrg",5000,4,game.players().get(player.getUUID()),game.config());
        require(caseButton.is(Items.JACK_O_LANTERN)&&key(caseButton.getHoverName(),"case_halloween"),"actual seasonal Jack case source GUI");
        for(int slot=12;slot<=14;slot++)require(key(OriginalGuiDesign.button(player,"swrg",6000,slot,game.players().get(player.getUUID()),game.config()).getHoverName(),"key_halloween"),"all three seasonal key bundles "+slot);
        int imba=game.config().imbaDropChance;game.config().imbaDropChance=6;game.luckyModifiers().syncConfig();
        int selected=0;for(int i=0;i<1000;i++)for(var stack:game.lbc().rollTable(level,player.position(),player,"lbc:lucky_items"))if(seasonal(stack))selected++;
        require(selected>0,"actual weighted Lucky loot admits Halloween entries");
        flag(game,"halloween",false);
        require(!GameConfig.load().eventHalloween&&GameConfig.load().eventNovember&&GameConfig.load().eventTournament,"one event off preserves other two across reload");
        require(!OriginalGuiDesign.button(player,"swrg",5000,4,game.players().get(player.getUUID()),game.config()).is(Items.JACK_O_LANTERN),"normal case restored immediately");
        for(int i=0;i<200;i++)for(var stack:game.lbc().rollTable(level,player.position(),player,"lbc:lucky_items"))require(!seasonal(stack),"disabled seasonal loot never selected");
        flag(game,"halloween",true);game.config().imbaDropChance=imba;game.luckyModifiers().syncConfig();
        var original=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(original::add);
        var at=player.position();int mem=game.players().get(player.getUUID()).memcoins;
        try{
            overlap(game,player);
            for(int outcome=1;outcome<=7;outcome++){
                service.screamer(player,outcome);var added=new ArrayList<Entity>();level.getAllEntities().forEach(e->{if(!original.contains(e))added.add(e);});
                if(outcome==1)require(added.stream().filter(Skeleton.class::isInstance).map(Skeleton.class::cast).anyMatch(m->m.hasEffect(MobEffects.INVISIBILITY)&&m.hasEffect(MobEffects.FIRE_RESISTANCE)&&LbcSpellSource.unownedHazard(m)),"invisible unowned native skeleton");
                if(outcome==2)require(added.stream().filter(PrimedTnt.class::isInstance).map(PrimedTnt.class::cast).anyMatch(t->t.getFuse()==170&&t.getY()==at.y+240&&t.getOwner()==null),"source170 fuse and240 height, no visitor ownership");
                if(outcome==3)require(game.items().psycho().effectValue(player,"scared")==60&&game.items().psycho().effectValue(player,"lookback")==10,"existing canonical Psycho clocks");
                if(outcome==4)require(added.stream().filter(Silverfish.class::isInstance).count()==8&&added.stream().filter(Silverfish.class::isInstance).map(Silverfish.class::cast).allMatch(m->m.getHealth()==1&&LbcSpellSource.unownedHazard(m)),"eight real unowned1HP silverfish");
                if(outcome==5)require(service.count()==31&&player.hasEffect(MobEffects.LEVITATION),"six source spiral markers and native levitation");
                if(outcome==6)require(added.stream().filter(WindCharge.class::isInstance).count()==4,"four native ownerless wind charges");
            }
            require(level.getBlockState(BlockPos.containing(at).below()).is(Blocks.JACK_O_LANTERN),"native solid-replace pumpkin trap");
            game.items().mana().score(player,"tournament_wins").set(2);
            // Invoke the real end-of-match owner, not the seasonal reward helper.
            var participants=GameController.class.getDeclaredField("participants");participants.setAccessible(true);
            for(var entry:((Map<UUID,?>)participants.get(game)).entrySet()){var lives=entry.getValue().getClass().getDeclaredField("lives");lives.setAccessible(true);lives.setInt(entry.getValue(),entry.getKey().equals(player.getUUID())?1:0);}
            var finish=GameController.class.getDeclaredMethod("finish");finish.setAccessible(true);finish.invoke(game);
            require(game.stage()==GameController.Stage.ENDING,"actual finish entered celebration");
            var beta=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta")));
            var participation=Objects.requireNonNull(game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse("swrg:rewards/turnir/trophy3")));
            require(game.isObserver(beta)&&beta.getAdvancements().getOrStartProgress(participation).isDone()
                    &&game.players().get(beta.getUUID()).legacyUnlocks.contains(participation.id().toString()),"losing observer receives native and durable source participation reward");
            var guarded=Objects.requireNonNull(game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse("swrg:rewards/turnir/trophy2")));
            for(var criterion:guarded.value().criteria().keySet())beta.getAdvancements().revoke(guarded,criterion);
            for(var criterion:guarded.value().criteria().keySet())require(!beta.getAdvancements().award(guarded,criterion),"ordinary spectator awards remain blocked after trusted projection returns");
            require(!beta.getAdvancements().getOrStartProgress(guarded).isDone(),"trusted award scope cannot leak into camera/inventory observation");
            require(game.items().mana().score(player,"tournament_wins").get()==3,"native tournament winner count");
            require(game.players().get(player.getUUID()).memcoins>mem,"real win Halloween bonus");
            require(game.server().getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR).getName().equals("tournament_kills")&&game.server().getScoreboard().getDisplayObjective(DisplaySlot.LIST).getName().equals("tournament_wins"),"real postgame tournament displays");
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        finally{game.items().psycho().clearStatus(player,"scared");game.items().psycho().clearStatus(player,"lookback");}
        SkyWars2.LOGGER.info("[check] PASS seasonal-events (real manual commands, persistence, source GUI, weighted loot, source25 actors, one circle per visitor and pulse,7 native outcomes, November and actual finish rewards)");
    }
}
