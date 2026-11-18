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
    static GenieModelClient offline(GenieModelClient.Options options){
        return new GenieModelClient(options,"fake-secret",(wish,recent)->"Offline transport contract. Return only constructor JSON.");
    }
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
    static void openAiTransport() throws Exception {
        var api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> paths = Collections.synchronizedList(new ArrayList<>());
        api.createContext("/v1/", exchange -> {
            try (exchange) {
                require("Bearer fake-secret".equals(exchange.getRequestHeaders().getFirst("Authorization")), "OpenAI project credential");
                String path = exchange.getRequestURI().getPath();
                String operation = exchange.getRequestMethod() + " " + path;
                paths.add(operation);
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject reply = new JsonObject();
                if (operation.equals("POST /v1/containers")) reply.addProperty("id", "cntr_fixture");
                else if (operation.equals("POST /v1/containers/cntr_fixture/files")) {
                    var filename = body.split("filename=\"", 2)[1].split("\"", 2)[0];
                    require(body.contains("fixture-" + filename), "exact verifier or context upload " + filename);
                    reply.addProperty("path", "/mnt/data/hosted-" + filename);
                } else if (operation.equals("POST /v1/responses")) {
                    var request = JsonParser.parseString(body).getAsJsonObject();
                    require(request.get("model").getAsString().equals("fixture-model")
                            && request.getAsJsonObject("reasoning").get("effort").getAsString().equals("medium")
                            && !request.get("store").getAsBoolean(), "configured model, medium reasoning, stateless request");
                    require(request.get("instructions").getAsString().contains("/mnt/data/hosted-verify_genie_program.py")
                            && request.getAsJsonArray("tools").get(0).getAsJsonObject().get("container").getAsString().equals("cntr_fixture"),
                            "run the mounted verifier in this wish's container");
                    require(request.getAsJsonArray("include").get(0).getAsString().equals("code_interpreter_call.outputs"),
                            "request provider-observed tool output");
                    boolean fail = request.get("input").getAsString().equals("missing final output");
                    reply = JsonParser.parseString("""
                        {"status":"completed","output":[
                        {"type":"message","phase":"commentary","role":"assistant","content":[{"type":"output_text","text":"ignored commentary"}]},
                        {"type":"code_interpreter_call","container_id":"cntr_other","status":"completed","outputs":[{"type":"logs","logs":"{\\"ok\\":true,\\"source\\":\\"wrong\\"}"}]},
                        {"type":"code_interpreter_call","container_id":"cntr_fixture","status":"completed","outputs":[{"type":"logs","logs":"{\\"ok\\":true,\\"source\\":\\"actual\\"}"}]},
                        {"type":"message","phase":"final_answer","role":"assistant","content":[{"type":"output_text","text":"{\\"decision\\":\\"reject\\"}"}]}]}
                        """).getAsJsonObject();
                    if (fail) reply.getAsJsonArray("output").remove(3);
                } else require(operation.equals("DELETE /v1/containers/cntr_fixture"), "unexpected OpenAI request");
                byte[] json = reply.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, json.length);
                exchange.getResponseBody().write(json);
            }
        });
        api.start();
        try (var http = java.net.http.HttpClient.newHttpClient()) {
            var uri = URI.create("http://127.0.0.1:" + api.getAddress().getPort() + "/v1/");
            var transport = new GenieOpenAiTransport(http, uri, "fixture-model", "fake-secret");
            byte[] verifier = "fixture-verify_genie_program.py".getBytes(StandardCharsets.UTF_8);
            var result = transport.run("wish", "fixture-game_knowledge.json", "fixture-contract.json", verifier);
            require(result.reply().equals("{\"decision\":\"reject\"}") && result.executions().size() == 1
                    && result.executions().getFirst().get("source").getAsString().equals("actual"),
                    "only final text and same-container tool logs are accepted");
            try {
                transport.run("missing final output", "fixture-game_knowledge.json", "fixture-contract.json", verifier);
                throw new AssertionError("missing final output accepted");
            } catch (java.io.IOException expected) {}
            require(paths.size() == 12 && paths.get(5).equals("DELETE /v1/containers/cntr_fixture")
                    && paths.getLast().equals(paths.get(5)), "temporary container deleted on success and failed response");
            System.out.println("GENIE_OPENAI_TRANSPORT_PASS Responses and Code Interpreter HTTP, final message, same-container receipts and cleanup");
        } finally {
            api.stop(0);
        }
    }
    static void geminiTransport() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] verifier = java.nio.file.Files.readAllBytes(java.nio.file.Path.of("tools/verify_genie_program.py"));
        String contract = "{\"events\":[\"HIT\"]}";
        var script = JsonParser.parseString("{\"functions\":{\"hit\":{\"parameters\":[\"event\"],\"code\":[]}},\"handlers\":{\"HIT\":\"hit\"}}").getAsJsonObject();
        var receipt = new JsonObject();
        receipt.addProperty("ok", true);
        receipt.addProperty("verifier", me.skywars2.game.genie.script.GenieScriptReply.VERIFIER_VERSION);
        receipt.addProperty("program_sha256", me.skywars2.game.genie.script.ScriptJson.digest(script));
        receipt.addProperty("contract_sha256", me.skywars2.game.genie.script.ScriptJson.digest(JsonParser.parseString(contract)));
        receipt.addProperty("instructions", 0);
        receipt.addProperty("functions", 1);
        var grant = JsonParser.parseString("""
            {"decision":"grant","item":{"icon":"minecraft:iron_sword","name":"Wish Sword","description":"A sword."},
             "answer":"Here, watch your toes.","twist":"toe crunch"}
            """).getAsJsonObject();
        grant.add("program", script);
        grant.add("receipt", receipt);
        var errors = new ConcurrentLinkedQueue<Throwable>();
        server.createContext("/generate", exchange -> {
            try (exchange) {
                require("fake-gemini-key".equals(exchange.getRequestHeaders().getFirst("x-goog-api-key"))
                        && exchange.getRequestURI().getQuery() == null, "Gemini key stays in auth header");
                var body = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                require(body.getAsJsonArray("contents").size() == 1
                        && body.getAsJsonArray("tools").get(0).getAsJsonObject().has("codeExecution"), "separate wish with cloud Python");
                var sent = body.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts");
                require(sent.get(1).getAsJsonObject().get("text").getAsString().contains("fixture game index"), "full game index supplied");
                var attachment = sent.get(0).getAsJsonObject().getAsJsonObject("inlineData");
                require(attachment.get("mimeType").getAsString().equals("text/csv"), "native file attachment type");
                var decoder = Base64.getDecoder();
                var rows = new String(decoder.decode(attachment.get("data").getAsString()), StandardCharsets.UTF_8).lines().toList();
                require(rows.get(0).equals("name,data") && rows.get(1).startsWith("verify_genie_program.py,")
                        && Arrays.equals(verifier, decoder.decode(rows.get(1).split(",", 2)[1]))
                        && rows.get(2).startsWith("contract.json,")
                        && contract.equals(new String(decoder.decode(rows.get(2).split(",", 2)[1]), StandardCharsets.UTF_8)),
                        "verifier and contract survive file transport byte for byte");
                String mode = sent.get(3).getAsJsonObject().get("text").getAsString();
                var response = JsonParser.parseString("""
                    {"candidates":[{"finishReason":"STOP","content":{"parts":[
                      {"thought":true,"text":"internal model text"},
                      {"text":"checking"},
                      {"executableCode":{"language":"PYTHON","code":"print(receipt)"}},
                      {"codeExecutionResult":{"outcome":"OUTCOME_OK","output":""}},
                      {"text":""}]}}]}
                    """).getAsJsonObject();
                var candidate = response.getAsJsonArray("candidates").get(0).getAsJsonObject();
                var parts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                var execution = parts.get(3).getAsJsonObject().getAsJsonObject("codeExecutionResult");
                execution.addProperty("output", receipt + "\n");
                if (mode.equals("failed validation")) execution.addProperty("outcome", "OUTCOME_FAILED");
                if (mode.equals("truncated")) candidate.addProperty("finishReason", "MAX_TOKENS");
                parts.get(4).getAsJsonObject().addProperty("text", grant.toString());
                byte[] data = response.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);
            } catch (Throwable error) {
                errors.add(error);
            }
        });
        server.start();
        try (var http = java.net.http.HttpClient.newHttpClient()) {
            var transport = new GenieGeminiTransport(http, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/generate"), "fake-gemini-key");
            var shape = JsonParser.parseString(contract).getAsJsonObject();
            var granted = transport.run("grant", "fixture game index", contract, verifier);
            require(me.skywars2.game.genie.script.GenieScriptReply.read(granted.reply(), granted.executions(), shape)
                    instanceof me.skywars2.game.genie.script.GenieScriptReply.Grant, "accept the final exact checked program");
            var failed = transport.run("failed validation", "fixture game index", contract, verifier);
            rejected(() -> me.skywars2.game.genie.script.GenieScriptReply.read(failed.reply(), failed.executions(), shape),
                    "final text alone must not grant an unverified program");
            try {
                transport.run("truncated", "fixture game index", contract, verifier);
                throw new AssertionError("truncated provider response accepted");
            } catch (java.io.IOException expected) {}
            require(errors.isEmpty(), "Gemini test HTTP endpoint failed");
            System.out.println("GENIE_GEMINI_TRANSPORT_PASS exact sandbox file, final JSON, genuine tool results and incomplete replies");
        } finally {
            server.stop(0);
        }
    }

    public static void main(String[] args)throws Exception{
        openAiTransport();
        geminiTransport();
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
            for(String mode:List.of("ok","deny","bad","large","limit"))try(var client=offline(new GenieModelClient.Options(URI.create(base+"/"+mode),"test-model",5))){
                var job=client.request("Make a healing sword "+mode,List.of(GenieBlueprint.TwistKind.RECOIL));
                if(mode.equals("ok"))require(job.future().get(5,TimeUnit.SECONDS).granted(),"valid item response");
                else if(mode.equals("deny"))require(!job.future().get(5,TimeUnit.SECONDS).granted(),"denial response");
                else {var error=failure(job.future());require(error instanceof GenieModelClient.ModelFailure,"typed HTTP failure");if(mode.equals("limit")){var f=(GenieModelClient.ModelFailure)error;require(f.retrySeconds()==12&&f.kind()==GenieModelClient.FailureKind.RATE_LIMIT,"rate limit metadata");}}
            }
            try(var client=offline(new GenieModelClient.Options(URI.create(base+"/cancel"),"test-model",5))){
                var pending=client.request("Cancel this wish",List.of());require(arrived.await(5,TimeUnit.SECONDS),"cancellable streaming request actually arrived");pending.close();require(pending.future().isCancelled(),"cancellation did not complete");
            }
            try(var client=offline(new GenieModelClient.Options(URI.create(base+"/timeout"),"test-model",5))){
                require(failure(client.request("Timeout this wish",List.of()).future())!=null,"streaming body timeout");
            }
            require(calls.get()==7,"unexpected retry or missing HTTP request");System.out.println("GENIE_CONTRACTS_PASS schema, bounds, speech, fresh HTTP contexts, deny, invalid/oversized replies, rate limit, stream cancellation, deadline");
        }finally{server.stop(0);executor.shutdownNow();}
    }
}
