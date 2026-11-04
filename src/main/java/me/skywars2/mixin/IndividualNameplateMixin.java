package me.skywars2.mixin;

import java.util.*;
import me.skywars2.game.LbcNameplates;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

public final class IndividualNameplateMixin {
    private IndividualNameplateMixin(){}
    @Mixin(ClientboundSetPlayerTeamPacket.class)
    public interface PacketAccess {
        @Invoker("<init>")
        static ClientboundSetPlayerTeamPacket skywars2$create(String name,int method,Optional<ClientboundSetPlayerTeamPacket.Parameters> parameters,Collection<String> players){throw new AssertionError();}
    }
    @Mixin(ServerScoreboard.class)
    public abstract static class Teams {
        @Inject(method={"onTeamAdded","onTeamChanged","onTeamRemoved"},at=@At("HEAD"))
        private void skywars2$teamImage(PlayerTeam team,CallbackInfo ci){LbcNameplates.changed((Scoreboard)(Object)this);}
    }
    @Mixin(Scoreboard.class)
    public abstract static class Membership {
        @Inject(method="addPlayerToTeam",at=@At("RETURN"))
        private void skywars2$joined(String name,PlayerTeam team,CallbackInfoReturnable<Boolean> ci){LbcNameplates.changed((Scoreboard)(Object)this);}
        @Inject(method="removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V",at=@At("RETURN"))
        private void skywars2$left(String name,PlayerTeam team,CallbackInfo ci){LbcNameplates.changed((Scoreboard)(Object)this);}
    }
}
