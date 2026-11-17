package me.skywars2.game.genie;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real loopback HTTP, no provider credentials, Minecraft or external service required. */
public final class GenieContracts {
    static void require(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    static void rejected(Runnable action,String why){try{action.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){}}
    static JsonObject accepted(){return JsonParser.parseString("""
        {"granted":true,"answer":"Your sword is ready. Mind your pockets!","loophole":"The wish omitted a health cost.","item":{
        "name":"A curious sword","description":"A sword that heals on a hit.","icon":"iron_sword","count":1,
        "rules":[{"trigger":"HIT","target":"SELF","condition":"ALWAYS","effect":"HEAL","id":"","amount":2,"radius":0,"ticks":0,"cooldown":20,"chance":1}],
        "stats":[],"twist":{"kind":"HEALTH_PRICE","power":1}}}
        """).getAsJsonObject();}
    static String envelope(String reply){var root=new JsonObject();var choice=new JsonObject();var message=new JsonObject();message.addProperty("content",reply);choice.add("message",message);var choices=new JsonArray();choices.add(choice);root.add("choices",choices);return root.toString();}
    static void shape(){
        var original=accepted();var reply=GenieBlueprint.parseReply(original.toString());
        require(GenieBlueprint.decode(reply.item().encode()).equals(reply.item()),"immutable program roundtrip");
        var altered=original.deepCopy();altered.getAsJsonObject("item").getAsJsonArray("rules").get(0).getAsJsonObject().addProperty("command","kill @a");
        rejected(()->GenieBlueprint.parseReply(altered.toString()),"unknown nested executable field accepted");
        var fractional=original.deepCopy();fractional.getAsJsonObject("item").addProperty("count",1.5);rejected(()->GenieBlueprint.parseReply(fractional.toString()),"fractional count accepted");
        var huge=original.deepCopy();huge.getAsJsonObject("item").getAsJsonArray("rules").get(0).getAsJsonObject().addProperty("amount",999);rejected(()->GenieBlueprint.parseReply(huge.toString()),"global kill power accepted");
        var noTwist=original.deepCopy();noTwist.getAsJsonObject("item").add("twist",JsonNull.INSTANCE);rejected(()->GenieBlueprint.parseReply(noTwist.toString()),"missing drawback accepted");
        var darkness=original.deepCopy();var rule=darkness.getAsJsonObject("item").getAsJsonArray("rules").get(0).getAsJsonObject();rule.addProperty("effect","STATUS");rule.addProperty("id","darkness");rule.addProperty("amount",2);rejected(()->GenieBlueprint.parseReply(darkness.toString()),"darkness III accepted");
        var emoji=original.deepCopy();emoji.addProperty("answer","Done \uD83D\uDE00");rejected(()->GenieBlueprint.parseReply(emoji.toString()),"decorated speech accepted");
        rejected(()->new GenieModelClient.Options(URI.create("https:missing-host"),"model",10),"missing host accepted");
        rejected(()->new GenieModelClient.Options(URI.create("http://example.com"),"model",10),"remote plaintext accepted");
        try(var absent=new GenieModelClient(GenieModelClient.Options.defaults(),"")){
            require(!absent.configured(),"missing credentials reported configured");
            var error=failure(absent.request("Make a sword",List.of()).future());require(error instanceof GenieModelClient.ModelFailure f&&f.kind()==GenieModelClient.FailureKind.NOT_CONFIGURED,"missing credentials failure");
        }
    }
    static Throwable failure(CompletableFuture<?> future){try{future.get(10,TimeUnit.SECONDS);throw new AssertionError("failure expected");}catch(ExecutionException e){return e.getCause();}catch(Exception e){throw new AssertionError(e);}}
    public static void main(String[] args)throws Exception{
        shape();var executor=Executors.newCachedThreadPool(r->{var t=new Thread(r,"genie-contract-http");t.setDaemon(true);return t;});
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(executor);var calls=new AtomicInteger();var arrived=new CountDownLatch(1);
        server.createContext("/",exchange->{
            try{
                var request=JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
                var messages=request.getAsJsonArray("messages");require(messages.size()==2,"request reused chat history");require(messages.get(1).getAsJsonObject().get("role").getAsString().equals("user"),"fresh wish role");
                require(request.getAsJsonObject("response_format").getAsJsonObject("json_schema").get("strict").getAsBoolean(),"strict schema omitted");
                require("Bearer fake-secret".equals(exchange.getRequestHeaders().getFirst("Authorization")),"auth transport header");calls.incrementAndGet();
                String mode=exchange.getRequestURI().getPath();String response;int status=200;
                if(mode.equals("/deny"))response=envelope("{\"granted\":false,\"answer\":\"No.\",\"loophole\":\"\",\"item\":null}");
                else if(mode.equals("/bad"))response=envelope("{\"command\":\"kill @a\"}");
                else if(mode.equals("/large"))response="x".repeat(66000);
                else if(mode.equals("/limit")){status=429;exchange.getResponseHeaders().add("Retry-After","12");response="rate limited";}
                else if(mode.equals("/cancel")||mode.equals("/timeout")){exchange.sendResponseHeaders(200,0);exchange.getResponseBody().write('{');exchange.getResponseBody().flush();arrived.countDown();Thread.sleep(7000);return;}
                else response=envelope(accepted().toString());
                var bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);
            }catch(InterruptedException interrupted){Thread.currentThread().interrupt();}finally{exchange.close();}
        });server.start();
        String base="http://127.0.0.1:"+server.getAddress().getPort();
        try{
            for(String mode:List.of("ok","deny","bad","large","limit"))try(var client=new GenieModelClient(new GenieModelClient.Options(URI.create(base+"/"+mode),"test-model",5),"fake-secret")){
                var job=client.request("Make a healing sword "+mode,List.of(GenieBlueprint.TwistKind.RECOIL));
                if(mode.equals("ok"))require(job.future().get(5,TimeUnit.SECONDS).granted(),"valid item response");
                else if(mode.equals("deny"))require(!job.future().get(5,TimeUnit.SECONDS).granted(),"denial response");
                else {var error=failure(job.future());require(error instanceof GenieModelClient.ModelFailure,"typed HTTP failure");if(mode.equals("limit")){var f=(GenieModelClient.ModelFailure)error;require(f.retrySeconds()==12&&f.kind()==GenieModelClient.FailureKind.RATE_LIMIT,"rate limit metadata");}}
            }
            try(var client=new GenieModelClient(new GenieModelClient.Options(URI.create(base+"/cancel"),"test-model",5),"fake-secret")){
                var pending=client.request("Cancel this wish",List.of());require(arrived.await(5,TimeUnit.SECONDS),"cancellable streaming request actually arrived");pending.close();require(pending.future().isCancelled(),"cancellation did not complete");
            }
            try(var client=new GenieModelClient(new GenieModelClient.Options(URI.create(base+"/timeout"),"test-model",5),"fake-secret")){
                require(failure(client.request("Timeout this wish",List.of()).future())!=null,"streaming body timeout");
            }
            require(calls.get()==7,"unexpected retry or missing HTTP request");System.out.println("GENIE_CONTRACTS_PASS schema, bounds, speech, fresh HTTP contexts, deny, invalid/oversized replies, rate limit, stream cancellation, deadline");
        }finally{server.stop(0);executor.shutdownNow();}
    }
}
