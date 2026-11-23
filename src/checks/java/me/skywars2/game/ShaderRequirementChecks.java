package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.presentation.ShaderRequirementGuard;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

final class ShaderRequirementChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Shader guard: "+why);}
    static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var player=f.owner();var other=f.other();
            ShaderRequirementGuard.forget(player);ShaderRequirementGuard.forget(other);
            ShaderRequirementGuard.synchronize(game,player);
            var displays=ShaderRequirementGuard.displays(player);var labels=ShaderRequirementGuard.labels(player);
            require(displays.size()==1&&labels.size()==3,"one cover and three camera labels");
            var cover=displays.getFirst();var holder=cover.getHolder();
            require(cover.getItem().get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:shader_guard")),"exact guarded material model");
            require(holder.getWatchingPlayers().size()==1&&holder.getWatchingPlayers().contains(player.connection),"private recipient only");
            require(ShaderRequirementGuard.displays(other).isEmpty(),"no second-recipient visual");
            for(var label:labels){
                require(label.getHolder()==holder&&label.getTextOpacity()==(byte)181,"text exact shader opacity");
                require(label.getBackground()==0&&!label.getDefaultBackground()&&!label.getShadow()&&label.getSeeThrough(),"no background or shadow, text visible over cover");
                require(label.getText().getStyle().getColor().getValue()==ShaderRequirementGuard.MARKER&&label.getText().getString().contains("\n"),"exact text marker and two lines");
            }
            int id=cover.getEntityId();player.setYRot(player.getYRot()+83);ShaderRequirementGuard.synchronize(game,player);
            require(ShaderRequirementGuard.displays(player).getFirst().getEntityId()==id,"reuse same cover while moving/turning");
            ShaderRequirementGuard.forget(player);
            require(ShaderRequirementGuard.displays(player).isEmpty()&&holder.getAttachment()==null&&holder.getWatchingPlayers().isEmpty(),"disconnect cleanup destroys attachment and watchers");
            SkyWars2.LOGGER.info("[check] PASS shader-requirement (native private virtual display model/tint contract, three two-line labels, no shadow/background, reuse and removal)");
        }
    }
}
