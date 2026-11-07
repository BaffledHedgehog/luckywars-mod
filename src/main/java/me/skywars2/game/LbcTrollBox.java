package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;

/** Six authored Troll Box outcomes; native bodies and shared fair work retain the accepted source. */
public final class LbcTrollBox {
    public static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:troll_box"),new Token(
            LbcUtilityItemPrototypes.properties("skywars2:troll_box","/data/lbc/loot_table/troll_box.json",Items.SNOWBALL,16)));
    private static final String ACTOR="skywars2.troll_box",POWER="skywars2.troll_box.power",HUNTER="skywars2.troll_box.hunter";
    private static final Identifier WALLS=Identifier.parse("lbc:walls"),GRAVITY=Identifier.withDefaultNamespace("imprinted");
    private static final TagKey<Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.withDefaultNamespace("solid"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.withDefaultNamespace("mobs"));
    // Exact authored item arguments; parsed only as native item data, never executed as commands/functions.
    private static final String[] MASK_ARGS={
        "minecraft:leather_helmet[minecraft:unbreakable={},minecraft:custom_name={\"color\":\"dark_red\",\"italic\":false,\"translate\":\"mask_1\"},minecraft:equippable={slot:\"head\",asset_id:\"lbc:mask_1\"},minecraft:attribute_modifiers=[{type:\"minecraft:max_health\",slot:\"head\",id:\"dhfiue98pensudwyr893\",amount:4,operation:\"add_multiplied_base\"},{type:\"minecraft:movement_speed\",slot:\"head\",id:\"8fre90uiert09iuresf\",amount:4,operation:\"add_multiplied_base\"}],minecraft:custom_data={troll:1,trollmask:1,craftusable:1},minecraft:item_model=\"lbc:mask_1\",tooltip_style=\"lbc:tier_a\",lore=[{\"translate\":\"mask_1.lore\",\"color\":\"red\",italic:false},{\"translate\":\"mask_all.lore\",\"color\":\"dark_red\",italic:false}]]",
        "minecraft:leather_helmet[minecraft:unbreakable={},minecraft:custom_name={\"color\":\"dark_red\",\"italic\":false,\"translate\":\"mask_2\"},minecraft:equippable={slot:\"head\",asset_id:\"lbc:mask_2\"},minecraft:attribute_modifiers=[{type:\"minecraft:attack_damage\",slot:\"head\",id:\"iewi9ef9ifdgfsedf\",amount:0.5,operation:\"add_multiplied_base\"},{type:\"minecraft:attack_speed\",slot:\"head\",id:\"pojoijfrwepo9jewf9pi\",amount:4,operation:\"add_multiplied_base\"}],minecraft:custom_data={troll:1,trollmask:2,craftusable:1},minecraft:item_model=\"lbc:mask_2\",tooltip_style=\"lbc:tier_a\",lore=[{\"translate\":\"mask_2.lore\",\"color\":\"red\",italic:false},{\"translate\":\"mask_all.lore\",\"color\":\"dark_red\",italic:false}]]",
        "minecraft:leather_helmet[minecraft:unbreakable={},minecraft:custom_name={\"color\":\"dark_red\",\"italic\":false,\"translate\":\"mask_3\"},minecraft:equippable={slot:\"head\",asset_id:\"lbc:mask_3\"},minecraft:attribute_modifiers=[{type:\"minecraft:attack_damage\",slot:\"head\",id:\"isjdfqqwwwwwwww22eris\",amount:1,operation:\"add_multiplied_base\"},{type:\"minecraft:armor\",slot:\"head\",id:\"dfsai080842i3jhaashd\",amount:4,operation:\"add_multiplied_base\"}],minecraft:custom_data={troll:1,trollmask:3,craftusable:1},minecraft:item_model=\"lbc:mask_3\",tooltip_style=\"lbc:tier_s\",lore=[{\"translate\":\"mask_3.lore\",\"color\":\"red\",italic:false},{\"translate\":\"mask_all.lore\",\"color\":\"dark_red\",italic:false}]]",
        "minecraft:leather_helmet[minecraft:unbreakable={},minecraft:enchantments={\"minecraft:binding_curse\":1},tooltip_display={hidden_components:[\"minecraft:enchantments\"]},minecraft:custom_name={\"color\":\"dark_red\",\"italic\":false,\"translate\":\"mask_4\"},minecraft:equippable={slot:\"head\",asset_id:\"lbc:mask_4\"},minecraft:attribute_modifiers=[{type:\"minecraft:movement_speed\",slot:\"head\",id:\"kfgpwseur9834jvblde8\",amount:1,operation:\"add_multiplied_base\"},{type:\"minecraft:knockback_resistance\",slot:\"head\",id:\"kdjeruf32vsw7yh4bb9\",amount:2,operation:\"add_multiplied_base\"},{type:\"minecraft:attack_damage\",slot:\"head\",id:\"edbn2_kowluujf\",amount:2,operation:\"add_multiplied_base\"},{type:\"minecraft:armor\",slot:\"head\",id:\"iijijd2y777dhejvode\",amount:2,operation:\"add_multiplied_base\"},{type:\"minecraft:armor_toughness\",slot:\"head\",id:\"ndjnj2no98vhfn\",amount:2,operation:\"add_multiplied_base\"},{type:\"minecraft:luck\",slot:\"head\",id:\"mfmej88883hbdh\",amount:-1000,operation:\"add_value\"},{type:\"minecraft:max_health\",slot:\"head\",id:\"owoo0rre3iuwunya\",amount:2,operation:\"add_multiplied_base\"}],minecraft:custom_data={troll:1,trollmask:4,craftusable:1},minecraft:item_model=\"lbc:mask_4\",tooltip_style=\"lbc:tier_s\",lore=[{\"translate\":\"mask_4.lore\",\"color\":\"red\",italic:false},{\"translate\":\"mask_all.lore\",\"color\":\"dark_red\",italic:false}]]",
        "minecraft:leather_helmet[minecraft:unbreakable={},minecraft:custom_name={\"color\":\"dark_red\",\"italic\":false,\"translate\":\"mask_5\"},minecraft:equippable={slot:\"head\",asset_id:\"lbc:mask_5\"},minecraft:attribute_modifiers=[{type:\"minecraft:armor\",slot:\"head\",id:\"fkl92jnd8u8xcj\",amount:-0.8,operation:\"add_multiplied_base\"},{type:\"minecraft:armor_toughness\",slot:\"head\",id:\"jkskvskmklmskmc\",amount:-0.8,operation:\"add_multiplied_base\"},{type:\"minecraft:attack_speed\",slot:\"head\",id:\"dseo3pofpwr21rd\",amount:1,operation:\"add_multiplied_base\"},{type:\"minecraft:max_health\",slot:\"head\",id:\"mndfdf32ehhdf78\",amount:-0.1,operation:\"add_multiplied_base\"},{type:\"minecraft:attack_damage\",slot:\"head\",id:\"iimkw3ekiuudfscv\",amount:0.75,operation:\"add_multiplied_base\"}],minecraft:custom_data={troll:1,trollmask:5,craftusable:1},minecraft:item_model=\"lbc:mask_5\",tooltip_style=\"lbc:tier_c\",lore=[{\"translate\":\"mask_5.lore\",\"color\":\"red\",italic:false},{\"translate\":\"mask_all.lore\",\"color\":\"dark_red\",italic:false}]]",
        "minecraft:leather_helmet[minecraft:unbreakable={},minecraft:custom_name={\"color\":\"dark_red\",\"italic\":false,\"translate\":\"mask_6\"},minecraft:equippable={slot:\"head\",asset_id:\"lbc:mask_6\"},minecraft:attribute_modifiers=[{type:\"minecraft:movement_speed\",slot:\"head\",id:\"mdmvihuhec8a\",amount:-0.25,operation:\"add_multiplied_base\"}],minecraft:custom_data={troll:1,trollmask:6,craftusable:1},minecraft:item_model=\"lbc:mask_6\",tooltip_style=\"lbc:tier_s\",lore=[{\"translate\":\"mask_6.lore\",\"color\":\"red\",italic:false},{\"translate\":\"mask_all.lore\",\"color\":\"dark_red\",italic:false}]]"
    };
    private static final String HUNTER_DATA="{CustomNameVisible:0b,CustomName:{\"color\":\"dark_red\",\"italic\":false,\"text\":\"Trollge\"},Tags:[\"tp_skel_mask\",\"trollge_hunter\"],Silent:1b,DeathLootTable:\"empty\",PersistenceRequired:1b,CanPickUpLoot:0b,Health:20.0f,equipment:{feet:{id:\"minecraft:leather_boots\",components:{\"minecraft:equippable\":{\"slot\":\"feet\",\"asset_id\":\"lbc:mask_1\"}},count:1},chest:{id:\"minecraft:leather_chestplate\",components:{\"minecraft:equippable\":{\"slot\":\"chest\",\"asset_id\":\"lbc:mask_1\"}},count:1},head:{id:\"minecraft:leather_helmet\",components:{\"minecraft:equippable\":{\"slot\":\"head\",\"asset_id\":\"lbc:mask_1\"},\"minecraft:unbreakable\":{}},count:1}},drop_chances:{head:0.0f,chest:0.0f,legs:0.0f,feet:0.0f},attributes:[{id:\"minecraft:max_health\",base:20},{id:\"minecraft:attack_damage\",base:8}],active_effects:[{id:\"minecraft:speed\",amplifier:1b,duration:20000000,show_particles:0b},{id:\"minecraft:invisibility\",amplifier:1b,duration:20000000,show_particles:0b}]}";
    private static final int[][] WALL_OFFSETS={{-5,-5},{-16,-5},{6,-5},{-5,-16},{-5,6},{-16,-16},{6,6},{6,-16},{-16,6},{-27,-27},{-27,-16},{-27,-5},{-27,6},{-27,17},{17,-27},{17,-16},{17,-5},{17,6},{17,17},{-16,-27},{-5,-27},{6,-27},{-16,17},{-5,17},{6,17}};
    public static void register(){}
    public static boolean matches(ItemStack stack){return ItemData.intValue(stack,"trollge",0)==1;}
    public static boolean managed(Entity actor){return actor!=null&&actor.entityTags().contains(ACTOR);}
    public static int mask(ItemStack stack){return ItemData.intValue(stack,"troll",0)==1?ItemData.intValue(stack,"trollmask",0):0;}
    private final GameController game;
    private final ItemStack[] masks=new ItemStack[6];
    private volatile List<MapManager.TemplateSlice> walls=List.of();
    private Event hunter,darkness,powerfall,abyss;
    private int darkClock,gravityLeft=-1,hunterClock,powerClock,lastTick=Integer.MIN_VALUE,callbacks;
    private String epoch;
    private boolean restoring;
    private final Set<UUID> darknessPlayers=new LinkedHashSet<>(),gravityPlayers=new LinkedHashSet<>();
    private final Map<UUID,Hunter> hunters=new LinkedHashMap<>();
    private final Map<UUID,Power> powers=new LinkedHashMap<>();
    private final Map<UUID,Eye> eyes=new LinkedHashMap<>();
    private final Map<UUID,Integer> playerFrames=new HashMap<>();
    private final Set<Placement> placements=Collections.newSetFromMap(new IdentityHashMap<>());
    private PowerFrame powerFrame;
    private record Event(ServerLevel level,LbcSpellSource.Source source){}
    private static final class Hunter { LivingEntity body;final Event event;Hunter(LivingEntity body,Event event){this.body=body;this.event=event;} }
    private static final class Power { Entity body;final Event event;int age;Power(Entity body,Event event){this.body=body;this.event=event;} }
    public record View(boolean hunter,boolean darkness,boolean powerfall,boolean abyss,int darkClock,int gravityLeft,int hunters,int powers,int placements,int eyes){}
    public LbcTrollBox(GameController game){this.game=game;}
    public CompletableFuture<Void> prepare(){
        var parser=new ItemParser(game.server().registryAccess());
        try{for(int i=0;i<6;i++)masks[i]=parser.parse(new StringReader(MASK_ARGS[i])).createItemStack(1);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("Authored Troll Box mask data",error);}
        return game.maps().prepareSpellTemplates(List.of(WALLS)).thenAccept(ready->walls=List.copyOf(ready.get(WALLS)));
    }
    public ItemStack maskPrototype(int number){if(number<1||number>6||masks[number-1]==null)throw new IllegalStateException("Prepare all six authored masks before Troll Box use");return masks[number-1].copy();}
    public View view(){return new View(hunter!=null,darkness!=null,powerfall!=null,abyss!=null,darkClock,gravityLeft,hunters.size(),powers.size(),placements.size(),eyes.size());}
    private boolean running(){return !restoring&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()
            &&game.server().tickRateManager().runsNormally()&&!game.server().tickRateManager().isFrozen();}
    private boolean paused(Entity body){return body.isRemoved()||game.items().timeStop().stopped((ServerLevel)body.level())||game.items().timeStop().frozen(body)||body.entityTags().contains("time_stopped");}
    private boolean paused(Event event){return event==null||!game.items().currentEpoch(event.source.epoch())||game.items().timeStop().stopped(event.level);}
    private boolean role(ServerPlayer player){return player.isAlive()&&!player.isRemoved()&&!player.isSpectator()&&!game.isObserver(player)&&game.canUseGameplayItems(player)&&game.items().wands().validRoleTarget(player,false);}
    private boolean target(Event event,Entity target){return !paused(event)&&game.items().spellSources().target(event.source,target);}
    private ServerPlayer author(Event event){return event.source.author()==null?null:game.server().getPlayerList().getPlayer(event.source.author());}
    private LbcSpellSource.Source authority(ServerPlayer owner){
        var inherited=game.items().spellSources().current();return inherited!=null?inherited:new LbcSpellSource.Source(UUID.randomUUID(),owner.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(owner),Set.of(),true);
    }
    public static boolean use(GameController game,ServerPlayer owner,InteractionHand hand){
        var service=game.items().trollBox();var stack=owner.getItemInHand(hand);
        service.syncEpoch();
        if(!matches(stack)||game.vanity().capturePreparing()||!service.running()||!service.role(owner)||!LbcDimensionItems.ordinary(owner)||service.paused(owner)
                ||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner))return false;
        // Author: Troll Box RMB has no jammer gate. The separately worn mask6 retains its old MAG gate.
        for(int i=0;i<6;i++)if(service.masks[i]==null)throw new IllegalStateException("Prepare Troll Box native sources before admission");
        var event=new Event(owner.level(),service.authority(owner));var id=owner.getUUID();
        owner.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,owner);int outcome=owner.getRandom().nextInt(6)+1;
        LbcTmtShotCompletion.afterNativeAction(()->{if(game.items().currentEpoch(event.source.epoch())){
            var current=game.server().getPlayerList().getPlayer(id);if(current!=null)service.outcome(current,event,outcome);
        }});return true;
    }
    private static final class Token extends SimplePolymerItem {
        Token(Properties properties){super(properties,Items.SNOWBALL,true);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=SkyWars2.game();return game!=null&&player instanceof ServerPlayer owner&&LbcTrollBox.use(game,owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    }
    private void outcome(ServerPlayer owner,Event event,int number){callbacks++;try{
        var reward=maskPrototype(number);if(!owner.getInventory().add(reward)&&!reward.isEmpty())owner.drop(reward,false,net.minecraft.util.Prediction.SERVER_ONLY);
        if(number!=4)for(var player:game.server().getPlayerList().getPlayers()){
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("beware_"+number)));
        }
        switch(number){
            case 1->hunter=event;
            case 2->{darkness=event;darknessPlayers.add(owner.getUUID());darkClock=0;enterDarkness();}
            case 3->powerfall=event;
            case 4,5->{for(var player:game.server().getPlayerList().getPlayers())if(role(player)&&!player.level().dimension().equals(LbcHedgehogDimensions.NEXUS)&&target(event,player)){
                var job=new Placement(new Event(player.level(),event.source),player.position(),player.getUUID(),number==4,player.getRandom().nextLong());placements.add(job);game.items().stonekivi().queuedWork(job);
            }
                if(number==5)for(var player:game.server().getPlayerList().getPlayers())LbcOverload.sound(player,new Vec3(0,1000000,0),"minecraft:lbcsounds.backrooms",.5f,1,Double.MAX_VALUE,.5f);
            }
            case 6->abyss=event;
            default->throw new IllegalArgumentException("Troll Box source result");
        }
    }finally{callbacks--;}}
    private List<ServerPlayer> darknessTargets(){var out=new ArrayList<ServerPlayer>();if(darkness==null)return out;
        for(var player:game.server().getPlayerList().getPlayers())if(role(player)&&!player.level().dimension().equals(LbcHedgehogDimensions.NEXUS)
                &&(game.gameMode()<3||game.gameMode()>4||darknessPlayers.contains(player.getUUID()))&&!paused(player)&&target(darkness,player))out.add(player);
        return out;
    }
    private void transfer(ServerPlayer player,ServerLevel destination,Event event,String sound,float pitch){
        if(destination==null)return;
        try(var scope=game.items().spellSources().scope(event.source,player.level())){
            game.items().indirectKills().withCause(player,event.source.cause(),()->player.teleportTo(destination,player.getX(),player.getY(),player.getZ(),Set.of(),player.getYRot(),player.getXRot(),true));
        }
        LbcOverload.sound(player,sound,1,pitch,Double.MAX_VALUE);
    }
    private void enterDarkness(){
        var destination=game.server().getLevel(LbcHedgehogDimensions.IMPRINTED);if(destination==null)return;
        for(var player:darknessTargets()){
            transfer(player,destination,darkness,"minecraft:lbcsounds.imprinted_enter",0);
            var gravity=player.getAttribute(Attributes.GRAVITY);if(gravity!=null){gravity.removeModifier(GRAVITY);gravity.addTransientModifier(new AttributeModifier(GRAVITY,-1.01,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));gravityPlayers.add(player.getUUID());}
        }
        gravityLeft=100;
    }
    private void clearGravity(boolean force){for(var it=gravityPlayers.iterator();it.hasNext();){var player=game.server().getPlayerList().getPlayer(it.next());
        if(!force&&(player==null||paused(player)))continue;
        if(player!=null){var gravity=player.getAttribute(Attributes.GRAVITY);if(gravity!=null)gravity.removeModifier(GRAVITY);}it.remove();
    }gravityLeft=gravityPlayers.isEmpty()?-1:0;}
    private void syncEpoch(){if(!game.items().currentEpoch(epoch)){stop();epoch=game.items().epoch();}}
    public void tick(){
        syncEpoch();
        if(!running()||lastTick==game.server().getTickCount())return;lastTick=game.server().getTickCount();callbacks++;try{
            // The coordinator's existing quiescence gate parks only new work. Accepted
            // Placement/PowerFrame steps and shared rays keep running until their boundary.
            boolean admitting=!game.vanity().capturePreparing();
            if(admitting&&darkness!=null&&!paused(darkness)){
                if(gravityLeft>0)--gravityLeft;if(gravityLeft==0)clearGravity(false);
                ++darkClock; // Every Imprinted visitor now owns their own return deadline.
                if(darkClock>=1800){darkClock=0;enterDarkness();}
            }
            // The source one-second grid exists before these globals are enabled; activation
            // does not introduce a fresh twenty-tick delay or reset the shared phase.
            if(admitting&&!game.items().timeStop().stopped(hunter==null?game.maps().level():hunter.level))hunterClock=(hunterClock+1)%20;
            if(admitting&&!game.items().timeStop().stopped(powerfall==null?game.maps().level():powerfall.level))powerClock=(powerClock+1)%20;
            for(var entry:List.copyOf(hunters.values())){
                if(gone(entry.body)){hunters.remove(entry.body.getUUID());continue;}if(paused(entry.body)||paused(entry.event))continue;
                if(admitting&&entry.body.tickCount>0&&hunterClock==0&&entry.body.level().getRandom().nextFloat()<.2f){int sound=entry.body.level().getRandom().nextInt(9);LbcOverload.sound((ServerLevel)entry.body.level(),entry.body.position(),"minecraft:lbcsounds.scream_"+(sound%3+1),2,sound<3?1:sound<6?0:1.5f,32,0);}
            }
            if(admitting&&powerfall!=null&&!paused(powerfall)&&powerClock==0)spawnPower(powerfall);
            if(admitting&&hunter!=null&&!paused(hunter)&&hunterClock==0){if(hunter.level.getRandom().nextFloat()<.05f){
                var candidates=new ArrayList<ServerPlayer>();for(var player:game.server().getPlayerList().getPlayers())if(role(player)&&target(hunter,player))candidates.add(player);
                if(!candidates.isEmpty()){var chosen=candidates.get(hunter.level.getRandom().nextInt(candidates.size()));if(!paused(chosen))spawnHunter(hunter,chosen);}
            }}
            if(admitting&&powerFrame==null&&!powers.isEmpty()){powerFrame=new PowerFrame();game.items().stonekivi().queuedWork(powerFrame);}
        }finally{callbacks--;}
    }
    /** Attach to the existing native per-player runner, not another world/entity scan. */
    public void playerTick(ServerPlayer player){
        syncEpoch();
        if(!running()||!game.items().currentEpoch(epoch)||!role(player)||paused(player))return;
        int now=game.server().getTickCount();var previous=playerFrames.put(player.getUUID(),now);if(previous!=null&&previous==now)return;
        // Source @s[y=0,dy=-1000000] covers the whole block-zero column, not only feet at exactly0.
        if(abyss!=null&&!paused(abyss)&&player.blockPosition().getY()<=0&&target(abyss,player)){
            try(var scope=game.items().spellSources().scope(abyss.source,player.level())){game.items().indirectKills().withCause(player,abyss.source.cause(),()->player.teleportTo(player.level(),player.getX(),321,player.getZ(),Set.of(),player.getYRot(),player.getXRot(),true));}
        }
        if(!game.vanity().capturePreparing()&&mask(player.getItemBySlot(EquipmentSlot.HEAD))==6&&!game.items().magic().suppressed(player)&&!eyes.containsKey(player.getUUID())){
            var eye=new Eye(player,new Event(player.level(),new LbcSpellSource.Source(player.getUUID(),player.getUUID(),epoch,game.items().indirectKills().wildCause(player),Set.of(),true)));
            game.items().exasperato().replayableSamples(player,player.getEyePosition(),player.getLookAngle(),200,eye);
        }
    }
    private boolean gone(Entity body){return body.isRemoved()&&body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK||body instanceof LivingEntity living&&!living.isAlive();}
    private void spawnHunter(Event event,ServerPlayer target){
        var level=target.level();var body=EntityTypes.WITHER_SKELETON.create(level,EntitySpawnReason.COMMAND);if(body==null)return;
        try{body.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),TagParser.parseCompoundFully(HUNTER_DATA)));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
        body.setPos(target.position().add(0,6,0));var spread=LbcBloodConfuse.spreadDestination(level,target.position(),level.getRandom());if(spread!=null)body.setPos(spread);
        body.entityTags().addAll(List.of(ACTOR,HUNTER,"tp_skel_ar"));var source=event.source;
        var accepted=new Event(level,source);hunters.put(body.getUUID(),new Hunter(body,accepted));game.items().spellSources().bind(body,source);game.items().markCurrentItemEpoch(body);game.items().timeStop().prepareFrozen(body);
        try(var scope=game.items().spellSources().scope(source,level)){if(!level.addFreshEntity(body)){hunters.remove(body.getUUID());body.discard();}}
    }
    private void spawnPower(Event event){
        var level=event.level;var body=new Marker(EntityTypes.MARKER,level);body.setPos(level.getRandom().nextInt(201)-100,310,level.getRandom().nextInt(201)-100);body.setNoGravity(true);
        body.entityTags().addAll(List.of(ACTOR,POWER,"slowraycast","movingpower","can_deleted","magic","other"));
        var source=event.source;var accepted=new Event(level,source);
        powers.put(body.getUUID(),new Power(body,accepted));game.items().spellSources().bind(body,source);game.items().markCurrentItemEpoch(body);game.items().timeStop().prepareFrozen(body);
        try(var scope=game.items().spellSources().scope(source,level)){if(!level.addFreshEntity(body)){powers.remove(body.getUUID());body.discard();}}
    }
    private final class Visit {final Power power;int cell;Visit(Power power){this.power=power;}}
    private final class PowerFrame implements LbcStonekivi.Work {
        final ArrayDeque<Visit> todo=new ArrayDeque<>();boolean closed;
        PowerFrame(){for(var power:powers.values())todo.addLast(new Visit(power));}
        public boolean ready(){if(closed)return true;if(!running())return false;for(var visit:todo)if(gone(visit.power.body)||!paused(visit.power.body)&&!paused(visit.power.event))return true;return todo.isEmpty();}
        public boolean step(){
            if(closed)return true;if(!running())return false;callbacks++;try{
                int examined=todo.size();while(examined-->0&&!todo.isEmpty()){
                    var visit=todo.peekFirst();var p=visit.power;if(gone(p.body)){powers.remove(p.body.getUUID());todo.removeFirst();continue;}
                    if(paused(p.body)||paused(p.event)){todo.addLast(todo.removeFirst());continue;}
                    var level=(ServerLevel)p.body.level();var center=p.body.blockPosition();int budget=64;
                    while(visit.cell<343&&budget-->0){int cell=visit.cell++;var at=center.offset(cell%7-3,cell/49-3,cell/7%7-3);
                        if(level.hasChunkAt(at)&&level.getBlockState(at).is(SOLID))game.items().writeOwnedTerrain(level,at,Blocks.OBSIDIAN.defaultBlockState(),p.event.source.cause(),true);
                    }
                    if(visit.cell<343)return false;
                    level.sendParticles(new DustParticleOptions(0x080749,4),true,false,p.body.getX(),p.body.getY(),p.body.getZ(),1,.1,.1,.1,0);
                    for(var victim:level.getEntitiesOfClass(LivingEntity.class,p.body.getBoundingBox().inflate(4),e->e.distanceToSqr(p.body)<=16&&target(p.event,e)&&!paused(e)))
                        for(var effect:List.of(MobEffects.SLOWNESS,MobEffects.WEAKNESS,MobEffects.WITHER,MobEffects.BLINDNESS))game.items().indirectKills().wildEffect(victim,ItemEffectLevels.create(effect,100,100,false,false),p.event.source.cause());
                    p.body.setPos(p.body.position().add(0,-.5,0));if(++p.age>=800){powers.remove(p.body.getUUID());p.body.discard();}todo.removeFirst();return finishIfEmpty();
                }return finishIfEmpty();
            }finally{callbacks--;}
        }
        private boolean finishIfEmpty(){if(!todo.isEmpty())return false;closed=true;if(powerFrame==this)powerFrame=null;return true;}
        public void cancel(){closed=true;todo.clear();if(powerFrame==this)powerFrame=null;}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){if(!closed)throw new IllegalStateException("Drain Troll Box power terrain frame before capture");return links->()->{};}
    }
    private final class Placement implements LbcStonekivi.Work {
        final Event event;final Vec3 at;final UUID victim;final boolean cage;final long seed;int offset,slice;boolean closed;
        Placement(Event event,Vec3 at,UUID victim,boolean cage,long seed){this.event=event;this.at=at;this.victim=victim;this.cage=cage;this.seed=seed;}
        public boolean ready(){return closed||running()&&!paused(event)&&(cage||!walls.isEmpty());}
        public boolean step(){if(closed)return true;if(!ready())return false;callbacks++;try{
            if(!game.items().currentEpoch(event.source.epoch())){cancel();return true;}
            try(var scope=game.items().spellSources().scope(event.source,event.level)){
                if(cage){
                    var center=BlockPos.containing(at);for(int x=(center.getX()-2)>>4;x<=(center.getX()+2)>>4;x++)for(int z=(center.getZ()-2)>>4;z<=(center.getZ()+2)>>4;z++)
                        if(event.level.getChunkSource().getChunkNow(x,z)==null){game.maps().prepareChunk(event.level,new net.minecraft.world.level.ChunkPos(x,z));return false;}
                    // The shared empty-cage variant retains the causal author/source, never a fake token.
                    if(!game.items().cageDrops().spawnEmptyAnvilCage(event.level,at,author(event),event.source.cause()))return false;
                    var player=game.server().getPlayerList().getPlayer(victim);if(player!=null){player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("look_up")));}return finish();
                }
                var part=walls.get(slice);var origin=BlockPos.containing(at).offset(WALL_OFFSETS[offset][0],-1,WALL_OFFSETS[offset][1]);var bounds=part.bounds().moved(origin.getX(),origin.getY(),origin.getZ());
                for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)if(event.level.getChunkSource().getChunkNow(x,z)==null){game.maps().prepareChunk(event.level,new net.minecraft.world.level.ChunkPos(x,z));return false;}
                var settings=new StructurePlaceSettings().setKnownShape(true).setIgnoreEntities(false).setBoundingBox(bounds).addProcessor(new BlockRotProcessor(.995f));
                game.items().indirectKills().capturedWorldChanges(event.level,event.source.cause(),()->game.maps().placeMapSlice(event.level,part.template(),origin,settings,seed+offset));
                if(++slice==walls.size()){slice=0;if(++offset==WALL_OFFSETS.length)return finish();}return false;
            }
        }finally{callbacks--;}}
        private boolean finish(){closed=true;placements.remove(this);return true;}
        public void cancel(){finish();}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){if(!closed)throw new IllegalStateException("Drain Troll Box cages/wall placements before capture");return links->()->{};}
    }
    private final class Eye implements LbcExasperato.ReplayableRay {
        final UUID wearer;final Event event;LbcExasperato.RayTicket ticket;
        Eye(ServerPlayer wearer,Event event){this.wearer=wearer.getUUID();this.event=event;}
        public double sampleStep(){return .5;}public boolean block(BlockPos at,BlockState state){return !state.isAir();}public boolean target(Entity entity){return false;}public void trail(Vec3 from,Vec3 to){}
        public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;eyes.put(wearer,this);}
        public boolean readyAt(Vec3 point){var player=game.server().getPlayerList().getPlayer(wearer);return player==null||!role(player)||!restoring&&!paused(player)&&!paused(event);}
        public HitResult sampleAt(Vec3 point){callbacks++;try{
            var at=BlockPos.containing(point);var player=game.server().getPlayerList().getPlayer(wearer);
            if(player==null||!role(player)||!game.items().currentEpoch(event.source.epoch())||!event.level.hasChunkAt(at))return BlockHitResult.miss(point,Direction.UP,at);
            event.level.sendParticles(new DustParticleOptions(0xff0000,1),true,false,point.x,point.y,point.z,1,.05,.05,.05,0);
            var positive=new AABB(point,point.add(1,1,1));var negative=new AABB(point.add(-.99,-.99,-.99),point.add(.01,.01,.01));
            var candidates=event.level.getEntitiesOfClass(LivingEntity.class,positive,e->e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(point)<=49&&LbcTrollBox.this.target(event,e)&&!paused(e));
            if(!candidates.isEmpty()){var victim=candidates.getFirst();if(victim.getBoundingBox().intersects(negative))game.items().indirectKills().withCause(victim,event.source.cause(),()->victim.hurtServer(event.level,event.level.damageSources().generic(),.05f));}
            if(event.level.getRandom().nextFloat()<.1f)LbcOverload.sound(event.level,point,"minecraft:entity.creeper.primed",.2f,1.5f,16,0);
            return event.level.getBlockState(at).isAir()?null:new BlockHitResult(point,Direction.UP,at,false);
        }finally{callbacks--;}}
        public void complete(HitResult hit){eyes.remove(wearer,this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){return resolved->()->{eyes.put(wearer,this);};}
    }
    public LivingEntity owner(Entity actor){var h=hunters.get(actor.getUUID());if(h!=null)return author(h.event);var p=powers.get(actor.getUUID());return p==null?null:author(p.event);}
    public List<Entity> snapshot(){var result=new ArrayList<Entity>();for(var hunter:hunters.values())result.add(hunter.body);for(var power:powers.values())result.add(power.body);return List.copyOf(result);}
    boolean cloneRecognizes(Entity body){return hunters.containsKey(body.getUUID())||powers.containsKey(body.getUUID());}
    LbcCloneSkills.ActorImage cloneImage(Entity original){
        var hunter=hunters.get(original.getUUID());var power=powers.get(original.getUUID());if(hunter==null&&power==null)return null;
        var event=hunter!=null?hunter.event:power.event;int age=power==null?0:power.age;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity source,Entity clone,Map<Entity,Entity> family){
                var accepted=new Event((ServerLevel)clone.level(),event.source);game.items().spellSources().bind(clone,event.source);
                if(hunter!=null)hunters.put(clone.getUUID(),new Hunter((LivingEntity)clone,accepted));
                else{var copied=new Power(clone,accepted);copied.age=age;powers.put(clone.getUUID(),copied);}
            }
            public void rollback(Entity clone){hunters.remove(clone.getUUID());powers.remove(clone.getUUID());}
        };
    }
    public void loaded(Entity actor){if(restoring||MatchActorCheckpoint.restoring(actor)||!managed(actor))return;
        var hunter=hunters.get(actor.getUUID());if(hunter!=null&&actor instanceof LivingEntity living){hunter.body=living;return;}var power=powers.get(actor.getUUID());if(power!=null){power.body=actor;return;}actor.discard();
    }
    public void removed(Entity actor){if(restoring||MatchActorCheckpoint.restoring(actor)||actor.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)return;hunters.remove(actor.getUUID());powers.remove(actor.getUUID());playerFrames.remove(actor.getUUID());}
    public boolean captureReady(){return callbacks==0&&!restoring&&placements.isEmpty()&&powerFrame==null;}
    void checkpointBoundary(){if(!captureReady())throw new IllegalStateException("Finish Troll Box native callbacks and accepted terrain before capture");}
    public void prepareFrozen(){checkpointBoundary();restoring=true;hunters.clear();powers.clear();eyes.clear();playerFrames.clear();}
    public void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();playerFrames.clear();}
    public void cancelFrozen(){restoring=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        record Body(MatchDamageSources.Ref body,Event event,int age){}
        final Event savedHunter=hunter,savedDark=darkness,savedPower=powerfall,savedAbyss=abyss;
        final String savedEpoch=epoch;final int dark=darkClock,gravity=gravityLeft,hclock=hunterClock,pclock=powerClock;
        final Set<UUID> marked=Set.copyOf(darknessPlayers),floating=Set.copyOf(gravityPlayers);
        final List<Body> hunterBodies=new ArrayList<>(),powerBodies=new ArrayList<>();
        Checkpoint(MatchSpellCheckpoint.Links links){for(var value:hunters.values())hunterBodies.add(new Body(links.key(value.body),value.event,0));for(var value:powers.values())powerBodies.add(new Body(links.key(value.body),value.event,value.age));}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var nextHunters=new LinkedHashMap<UUID,Hunter>();for(var body:hunterBodies){var actor=(LivingEntity)links.get(body.body);nextHunters.put(actor.getUUID(),new Hunter(actor,body.event));}
            var nextPowers=new LinkedHashMap<UUID,Power>();for(var body:powerBodies){var actor=links.get(body.body);var value=new Power(actor,body.event);value.age=body.age;nextPowers.put(actor.getUUID(),value);}
            return ()->{epoch=savedEpoch;hunter=savedHunter;darkness=savedDark;powerfall=savedPower;abyss=savedAbyss;darkClock=dark;gravityLeft=gravity;hunterClock=hclock;powerClock=pclock;
                darknessPlayers.clear();darknessPlayers.addAll(marked);gravityPlayers.clear();gravityPlayers.addAll(floating);hunters.clear();hunters.putAll(nextHunters);powers.clear();powers.putAll(nextPowers);powerFrame=null;playerFrames.clear();};
        }
    }
    public void stop(){
        for(var job:List.copyOf(placements))job.cancel();if(powerFrame!=null)powerFrame.cancel();for(var eye:List.copyOf(eyes.values()))if(eye.ticket!=null)eye.ticket.cancel();eyes.clear();
        for(var value:List.copyOf(hunters.values()))value.body.discard();for(var value:List.copyOf(powers.values()))value.body.discard();hunters.clear();powers.clear();clearGravity(true);
        hunter=darkness=powerfall=abyss=null;darknessPlayers.clear();hunterClock=powerClock=darkClock=0;lastTick=Integer.MIN_VALUE;playerFrames.clear();restoring=false;
    }
}
