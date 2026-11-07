package me.skywars2.game;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/** Gownomancer's physical effects_changed protection; native effect types own all hidden layers. */
public final class LbcGovnomancer {
    private final GameController game;
    // Present only during a synchronous native callback, never retained as match state.
    private final Set<UUID> cleansing=new HashSet<>();
    public LbcGovnomancer(GameController game){this.game=game;}
    public void effectsChanged(ServerPlayer player){
        if(cleansing.contains(player.getUUID())||!game.lbc().skillsEnabled()
                ||game.players().get(player.getUUID()).lbcSkill!=9
                ||!game.playing()||game.stage()==GameController.Stage.ENDING||!player.isAlive()||player.isRemoved()
                ||player.level()!=game.maps().level()||!game.canUseGameplayItems(player)||game.isObserver(player)
                ||!LbcDimensionItems.ordinary(player)||!game.items().wands().validRoleTarget(player,false)
                ||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.items().timeStop().stopped(player.level())||game.items().wands().inputTimeStopped(player))return;
        if(!player.hasEffect(MobEffects.NAUSEA)&&!player.hasEffect(MobEffects.POISON)&&!player.hasEffect(MobEffects.WITHER))return;
        cleansing.add(player.getUUID());
        try{
            player.removeEffect(MobEffects.NAUSEA);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WITHER);
        }finally{cleansing.remove(player.getUUID());}
    }
}
