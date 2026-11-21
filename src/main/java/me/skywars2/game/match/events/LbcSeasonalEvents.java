package me.skywars2.game.match.events;

import com.mojang.math.Transformation;

import me.skywars2.game.GameController;
import me.skywars2.game.effects.ItemEffectLevels;
import me.skywars2.game.effects.magic.LbcOverload;
import me.skywars2.game.gui.loadouts.LbcSourceUnlocks;
import me.skywars2.game.match.MatchSettings;
import me.skywars2.game.players.PlayerStore;
import me.skywars2.game.players.PlayerStore.Profile;
import me.skywars2.game.world.terrain.LbcDropBlocks;
import me.skywars2.game.world.terrain.LbcSurfaceTeleport;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import me.skywars2.game.effects.magic.LbcMana;

/** Manual seasonal lifecycle. Native bodies/scores own state; this loaded-body set is derived only. */
public final class LbcSeasonalEvents {
    public static final String SCREAMER="screamer_halloween";
    static final String WIND="particle_wind";
    private final GameController game;
    private final Set<Entity> actors=new LinkedHashSet<>();
    public LbcSeasonalEvents(GameController game){this.game=game;}
    public void loaded(Entity actor){
        if((actor instanceof Display.ItemDisplay&&actor.entityTags().contains(SCREAMER)
                ||actor instanceof Marker&&actor.entityTags().contains(WIND))&&game.items().currentEpoch(actor))actors.add(actor);
    }
    public void unloaded(Entity actor){actors.remove(actor);}
    public void reindexRestored(Collection<Entity> bodies){actors.clear();for(var actor:bodies)loaded(actor);}
    public void stop(){for(var actor:List.copyOf(actors))actor.discard();actors.clear();}
    public int count(){return actors.size();}
    private void insert(Entity actor,Vec3 at){
        actor.setPos(at);game.items().markCurrentItemEpoch(actor);
        if(actor instanceof Mob){actor.entityTags().add("skywars2.players_only_trap");game.items().spellSources().unowned(actor);}
        else if(actor instanceof Projectile||actor instanceof PrimedTnt)game.items().spellSources().unowned(actor);
        game.items().timeStop().prepareFrozen(actor);
        if(((net.minecraft.server.level.ServerLevel)actor.level()).addFreshEntity(actor))loaded(actor);
    }
    public void started(){
        if(game.config().eventTournament){objectives();game.server().getScoreboard().setDisplayObjective(DisplaySlot.SIDEBAR,null);}
        if(!game.config().eventHalloween)return;
        var level=game.maps().level();
        for(var player:game.server().getPlayerList().getPlayers()){
            game.items().mana().award(player,"lbc:true_advancements/event/halloween");
            LbcOverload.notifySound(player,"lbcsounds.halloween",SoundSource.MASTER,.5f,1);
        }
        for(int i=0;i<25;i++){
            // game.command executes later. Moving the initial actors by .01 before
            // spreadplayers ran excluded them from its distance=..0.01 selector.
            // Resolve the real resident terrain first; never leave a trap at Y100.
            var at=LbcSurfaceTeleport.destination(level,Vec3.ZERO,190,level.getMaxY(),level.getRandom());
            if(at==null)break;
            var actor=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);actor.entityTags().add(SCREAMER);
            var item=new ItemStack(Items.STICK);item.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:strangeness"));actor.setItemStack(item);
            actor.setShadowRadius(0);actor.setShadowStrength(0);actor.setViewRange(1000);
            actor.setBrightnessOverride(new net.minecraft.util.Brightness(15,15));
            actor.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(2,1,2),new Quaternionf()));
            insert(actor,at.add(0,.01,0));
        }
    }
    public void tick(){
        if(!game.playing()||!game.server().tickRateManager().runsNormally())return;
        var level=game.maps().level();if(game.items().timeStop().stopped(level))return;
        if(game.config().eventNovember&&game.matchTicks()==20)november();
        boolean halloweenPulse=game.matchTicks()%20==0&&game.config().eventHalloween;
        var screamers=halloweenPulse?new ArrayList<Entity>():null;
        for(var actor:List.copyOf(actors)){
            if(actor.isRemoved()||!game.items().currentEpoch(actor)){actors.remove(actor);continue;}
            if(actor.level()!=level||game.items().timeStop().frozen(actor)||!level.hasChunkAt(actor.blockPosition()))continue;
            if(actor.entityTags().contains(WIND)){
                var at=actor.position();level.sendParticles(ParticleTypes.GUST,at.x,at.y,at.z,1,0,0,0,0);
                actor.setPos(at.add(actor.getLookAngle().scale(.2)));actor.setYRot(actor.getYRot()+10);
                if(level.getRandom().nextFloat()<.04f)actor.discard();
            }else if(screamers!=null){
                screamers.add(actor);
            }
        }
        if(screamers==null||screamers.isEmpty())return;
        // The legacy second selects at most one circle for each eligible player.
        for(var player:level.players()){
            if(player.isSpectator()||game.isObserver(player)||!player.isAlive()||game.items().timeStop().frozen(player))continue;
            for(var remaining=screamers.iterator();remaining.hasNext();){
                var actor=remaining.next();
                if(actor.isRemoved()||player.position().distanceToSqr(actor.position())>4)continue;
                remaining.remove();
                actor.discard();
                actors.remove(actor);
                activate(player);
                break;
            }
        }
    }
    private void activate(ServerPlayer player){
        int amount=1+player.getRandom().nextInt(50);game.players().get(player.getUUID()).memcoins+=amount;
        player.sendSystemMessage(Component.translatable("you_got").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(Integer.toString(amount)).withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("memcoins").withStyle(ChatFormatting.GOLD)));
        game.players().save();screamer(player,1+player.getRandom().nextInt(7));
    }
    /** Exactly the seven reachable source outcomes, not the unused 8..16 functions. No ownership from the visitor. */
    public void screamer(ServerPlayer player,int outcome){
        if(outcome<1||outcome>7)throw new IllegalArgumentException("Halloween outcome 1..7");
        var level=player.level();var at=player.position();var random=player.getRandom();
        LbcOverload.notifySound(player,"lbcsounds.s"+outcome,SoundSource.MASTER,1,1);
        switch(outcome){
            case 1->{var mob=new Skeleton(EntityTypes.SKELETON,level);mob.addEffect(ItemEffectLevels.create(MobEffects.INVISIBILITY,-1,0,false,false,false));
                mob.addEffect(ItemEffectLevels.create(MobEffects.FIRE_RESISTANCE,-1,0,false,false,false));insert(mob,at);}
            case 2->{player.addEffect(ItemEffectLevels.create(MobEffects.DARKNESS,200,0));var tnt=new PrimedTnt(EntityTypes.TNT,level);tnt.setFuse(170);insert(tnt,at.add(0,240,0));}
            case 3->game.items().psycho().afflict(player,null,60,10,0,0);
            case 4->{for(int i=0;i<8;i++){var mob=new Silverfish(EntityTypes.SILVERFISH,level);mob.setHealth(1);
                mob.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER).setBaseValue(0);insert(mob,at);
                mob.setDeltaMovement((random.nextInt(12001)-6000)*.0001,random.nextInt(10001)*.0001,(random.nextInt(12001)-6000)*.0001);}}
            case 5->{for(int yaw:new int[]{0,60,120,180,-60,-120}){var wind=new Marker(EntityTypes.MARKER,level);wind.entityTags().addAll(List.of(WIND,"slowraycast","rare"));wind.setYRot(yaw);insert(wind,at);}
                player.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,6));}
            case 6->{for(var offset:List.of(new Vec3(2,2,0),new Vec3(-2,2,0),new Vec3(0,2,2),new Vec3(0,2,-2)))insert(new WindCharge(EntityTypes.WIND_CHARGE,level),at.add(offset));}
            case 7->{var pos=BlockPos.containing(at);var solid=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse("minecraft:solid"));
                LbcDropBlocks.fill(level,pos.offset(-5,-5,-5),pos.offset(5,5,5),Blocks.JACK_O_LANTERN,false,state->state.is(solid));}
        }
    }
    private void november(){
        for(var player:game.server().getPlayerList().getPlayers()){
            int faction=game.items().mana().score(player,"november").get();if(faction!=1&&faction!=2)continue;
            // The source changes the existing team's prefix, not its color or membership.
            var team=player.getTeam();if(team!=null)team.setPlayerPrefix(Component.literal(faction==1?"\u0414\u0436\u0435\u0434\u0430\u0439 ":"\u0421\u0438\u0442\u0445 ").withStyle(faction==1?ChatFormatting.AQUA:ChatFormatting.RED));
            player.sendSystemMessage(Component.translatable("november_stat").withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable("jedi").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(Integer.toString(game.items().lightsabers().jediKills())))
                    .append(Component.translatable("sith").withStyle(ChatFormatting.RED))
                    .append(Component.literal(Integer.toString(game.items().lightsabers().sithKills()))));
        }
    }
    private void objectives(){
        var board=game.server().getScoreboard();
        if(board.getObjective("tournament_kills")==null)board.addObjective("tournament_kills",ObjectiveCriteria.KILL_COUNT_PLAYERS,Component.translatable("kills").withStyle(ChatFormatting.RED),ObjectiveCriteria.RenderType.INTEGER,true,null);
        if(board.getObjective("tournament_wins")==null)board.addObjective("tournament_wins",ObjectiveCriteria.DUMMY,Component.translatable("wins").withStyle(ChatFormatting.GOLD),ObjectiveCriteria.RenderType.INTEGER,false,null);
    }
    public void settingsChanged(){
        var board=game.server().getScoreboard();
        if(game.config().eventTournament){objectives();if(!game.playing()){board.setDisplayObjective(DisplaySlot.SIDEBAR,board.getObjective("tournament_kills"));board.setDisplayObjective(DisplaySlot.LIST,board.getObjective("tournament_wins"));}}
        else{for(var slot:List.of(DisplaySlot.SIDEBAR,DisplaySlot.LIST)){var shown=board.getDisplayObjective(slot);if(shown!=null&&shown.getName().startsWith("tournament_"))board.setDisplayObjective(slot,null);}MatchSettings.sync(game);}
    }
    private void reward(ServerPlayer player,String key){
        var profile=game.players().get(player.getUUID());
        profile.revokedLegacyUnlocks.remove(key);profile.legacyUnlocks.add(key);
        // The common finish transaction saves profiles after all seasonal recipients.
        LbcSourceUnlocks.publish(player,profile);
    }
    public void finished(List<UUID> winners){
        if(game.config().eventHalloween&&!winners.isEmpty()){
            int roll=1+game.maps().level().getRandom().nextInt(100),amount=roll<=70?100:roll<=99?250:1500;
            for(var id:winners){game.players().get(id).memcoins+=amount;var player=game.server().getPlayerList().getPlayer(id);
                if(player!=null)player.sendSystemMessage(Component.translatable(roll<=70?"get_memcoin1":roll<=99?"get_memcoin2":"get_memcoin3").withStyle(ChatFormatting.RED));}
        }
        if(!game.config().eventTournament)return;
        objectives();var mana=game.items().mana();
        for(var id:winners){var profile=game.players().get(id);if(profile.username.equals("BaffledHedgehog"))continue;
            var holder=ScoreHolder.forNameOnly(profile.username);var wins=mana.score(holder,"tournament_wins");wins.add(1);
            var player=game.server().getPlayerList().getPlayer(id);
            if(wins.get()>=3&&player!=null){game.announce(player.getDisplayName().copy().append(Component.translatable("win_tour").withStyle(ChatFormatting.GOLD)));reward(player,"swrg:rewards/turnir/trophy1");}}
        for(var player:game.server().getPlayerList().getPlayers()){
            if(mana.score(player,"tournament_wins").get()>=2)reward(player,"swrg:rewards/turnir/trophy2");
            reward(player,"swrg:rewards/turnir/trophy3");
        }
        // Called before Stage.ENDING; explicitly publish the source post-match displays here.
        var board=game.server().getScoreboard();board.setDisplayObjective(DisplaySlot.SIDEBAR,board.getObjective("tournament_kills"));board.setDisplayObjective(DisplaySlot.LIST,board.getObjective("tournament_wins"));
    }
}
