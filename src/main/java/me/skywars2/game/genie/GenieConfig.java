package me.skywars2.game.genie;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;

/** The key is private server configuration, never a gamerule, command argument or model message. */
final class GenieConfig {
    private record FileOptions(String endpoint,String model,int timeoutSeconds){}
    static GenieModelClient load(){
        var folder=FabricLoader.getInstance().getConfigDir().resolve("skywars2/genie");
        var defaults=GenieModelClient.Options.defaults();
        try{
            Files.createDirectories(folder);var path=folder.resolve("api.json");
            if(!Files.exists(path))Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(
                    new FileOptions(defaults.endpoint().toString(),defaults.model(),defaults.timeoutSeconds())),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            var config=new Gson().fromJson(Files.readString(path,StandardCharsets.UTF_8),FileOptions.class);
            String key=System.getenv("SKYWARS2_GENIE_API_KEY");var keyFile=folder.resolve("api-key.txt");
            if((key==null||key.isBlank())&&Files.isRegularFile(keyFile))key=Files.readString(keyFile,StandardCharsets.UTF_8);
            return new GenieModelClient(new GenieModelClient.Options(URI.create(config.endpoint()),config.model(),config.timeoutSeconds()),key);
        }catch(IOException|RuntimeException failure){
            // Do not echo a file, endpoint credentials or provider error body into logs/chat.
            return new GenieModelClient(defaults,"");
        }
    }
}
