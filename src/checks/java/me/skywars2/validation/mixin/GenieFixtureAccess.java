package me.skywars2.validation.mixin;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import me.skywars2.game.genie.GenieModelClient;
import me.skywars2.game.genie.GenieScriptModel;
import me.skywars2.game.genie.GenieService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class GenieFixtureAccess {
    private GenieFixtureAccess() {}

    @Mixin(value = GenieService.class, remap = false)
    public interface Service {
        @Accessor("model") GenieModelClient validation$model();
        @Accessor("model") void validation$model(GenieModelClient model);
        @Accessor("scriptModel") GenieScriptModel validation$scriptModel();
        @Accessor("scriptModel") void validation$scriptModel(GenieScriptModel model);
        @Accessor("wishes") Map<UUID, ?> validation$wishes();
    }

    @Mixin(value = GenieScriptModel.class, remap = false)
    public interface Model {
        @Accessor("transport") GenieScriptModel.Transport validation$transport();
    }

    @Mixin(targets = "me.skywars2.game.genie.GenieGeminiTransport", remap = false)
    public interface Gemini {
        @Accessor("endpoint") URI validation$endpoint();
    }

}
