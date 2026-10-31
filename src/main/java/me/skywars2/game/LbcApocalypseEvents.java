package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Original three global apocalypse phase graphs, with typed actions and native selectors/actors. */
public final class LbcApocalypseEvents {
    public enum Kind { END, BAFFLED, SHISH }
    public record Authority(UUID author,LbcIndirectKills.Owner cause,String epoch){}
    private record Key(Kind kind,int phase){}
    private static final class Pending {final Authority authority;int remaining;Pending(Authority authority,int ticks){this.authority=authority;remaining=ticks;}}
    private static final String TAG="skywars2.apocalypse.actor",AUTHOR="skywars2.apocalypse.author.",ORDER="skywars2.apocalypse.order.",EPOCH="skywars2.apocalypse.epoch.";
    private static final Identifier GOLD=Identifier.parse("lbc:gold48");
    private static final TagKey<net.minecraft.world.level.block.Block> SOLID=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:solid"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final Map<String,EntitySelector> SELECTORS=new HashMap<>();
    private static final CompoundTag SPAWNER=parse("""
{SpawnCount:1,SpawnRange:20,Delay:1,MinSpawnDelay:1,MaxSpawnDelay:2,MaxNearbyEntities:500,RequiredPlayerRange:60,SpawnData:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:8388608,custom_effects:[{id:"minecraft:instant_damage",amplifier:0b,duration:20}]}},count:1}}},SpawnPotentials:[{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:8388608,custom_effects:[{id:"minecraft:instant_damage",amplifier:0b,duration:20}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:2035284,custom_effects:[{id:"minecraft:slowness",amplifier:4b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:1935126,custom_effects:[{id:"minecraft:nausea",amplifier:1b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:0,custom_effects:[{id:"minecraft:blindness",amplifier:1b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:5476112,custom_effects:[{id:"minecraft:hunger",amplifier:127b,duration:400}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:4728399,custom_effects:[{id:"minecraft:weakness",amplifier:0b,duration:400}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:13433087,custom_effects:[{id:"minecraft:mining_fatigue",amplifier:2b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:6618974,custom_effects:[{id:"minecraft:poison",amplifier:2b,duration:300}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:10921638,custom_effects:[{id:"minecraft:wither",amplifier:2b,duration:300}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:16774925,custom_effects:[{id:"minecraft:glowing",amplifier:1b,duration:1200}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:16192767,custom_effects:[{id:"minecraft:levitation",amplifier:10b,duration:40}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:14417906,custom_effects:[{id:"minecraft:speed",amplifier:20b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:16756293,custom_effects:[{id:"minecraft:haste",amplifier:127b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:16384000,custom_effects:[{id:"minecraft:strength",amplifier:0b,duration:800}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:13762327,custom_effects:[{id:"minecraft:jump_boost",amplifier:20b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:3932152,custom_effects:[{id:"minecraft:slow_falling",amplifier:1b,duration:600}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:4521984,custom_effects:[{id:"minecraft:instant_damage",amplifier:1b,duration:20}]}},count:1}}}},{weight:1,data:{entity:{id:"minecraft:splash_potion",Item:{id:"minecraft:splash_potion",components:{"minecraft:potion_contents":{custom_color:12910585,custom_effects:[{id:"minecraft:levitation",amplifier:0b,duration:400}]}},count:1}}}}]}
""");
    private static final CompoundTag RIDER=parse("""
{CustomNameVisible:0b,Passengers:[{id:"minecraft:wither_skeleton",equipment:{mainhand:{id:"minecraft:iron_axe",components:{"minecraft:custom_name":{"italic":false,"translate":"hammer"},"minecraft:lore":[[{"translate":"type","color":"aqua","italic":false},{"translate":"axe","color":"aqua","italic":false}],{"translate":"hammer.lore","color":"white","italic":false}]},count:1}},drop_chances:{mainhand:1.0f}}],CustomName:"BaffledHegdehog"}
""");
    private static final String[] LUCKY_ITEMS={
        """
minecraft:red_mushroom_block[minecraft:custom_name={"bold":false,"color":"gold","italic":false,"text":"Lucky Block"},minecraft:lore=[{"text": "By BaffledHedgehog","color": "gray","italic": false}],minecraft:block_state={up:"false",down:"false",north:"false",south:"false",east:"false",west:"false"},minecraft:item_model="lbc:lucky_block",tooltip_style="lbc:tier_u"]
""",
        """
minecraft:brown_mushroom_block[minecraft:custom_name={"color":"light_purple","italic":false,"text":"Lucky Block Neko"},minecraft:lore=[{"text":"By BaffledHedgehog","color":"gray","italic":false}],minecraft:block_state={up:"false",down:"false",north:"false",south:"false",east:"false",west:"false"},minecraft:item_model="lbc:lucky_block_neko",tooltip_style="lbc:tier_u"]
""",
        """
minecraft:mushroom_stem[minecraft:custom_name={"color":"#FFB8E0","italic":false,"text":"❤ Lucky Block Loli ❤"},minecraft:lore=[{"text":"Community of the Lucky Wars ❤","color":"gray","italic":false}],minecraft:block_state={up:"false",down:"false",north:"false",south:"false",east:"false",west:"false"},minecraft:item_model="lbc:lucky_block_loli",tooltip_style="lbc:tier_u"]
"""
    };
    private static final int[] DELAY_END={0,2,2,2,2,2,2,2,2,2,2,2,2,2,2,40,5,5,5,5,5,5,5,100,15,15,15,40,10,10,10,40,60,15,15,15,40,10,10,10,10,10,10,10,10,10,10,10,60,10,10,10,70,140,100,140,300,440,0};
    private static final int[] DELAY_BAFFLED={0,80,120,160,160,200,80,240,0};
    private static final int[] DELAY_SHISH={0,100,100,100,200,100,400,400,400,100,100,0};
    private final GameController game;
    private final LinkedHashMap<Key,Pending> pending=new LinkedHashMap<>();
    private Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Terrain> terrain=Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile List<MapManager.TemplateSlice> gold=List.of();
    private int preparation,lastTick=Integer.MIN_VALUE;
    private int callbackDepth,preparationToken;private boolean rewinding;private volatile boolean preparing;
    private MatchAuthorityCheckpoint.Clock stagedClock;
    private Authority executingAuthority;private ServerLevel executingLevel;private Entity executingEmitter;
    private long starts,phases,outputs;
    public LbcApocalypseEvents(GameController game){this.game=game;}
    private static CompoundTag parse(String value){try{return TagParser.parseCompoundFully(value);}catch(CommandSyntaxException e){throw new ExceptionInInitializerError(e);}}
    public static List<Kind> kinds(ItemStack stack){if(stack.isEmpty())return List.of();var d=LbcFartus.data(stack);if(d.getIntOr("gadget",0)!=1)return List.of();var result=new ArrayList<Kind>();
        if(d.getIntOr("end_of_everything",0)==1)result.add(Kind.END);
        if(d.getIntOr("baffled_of_everything",0)==1)result.add(Kind.BAFFLED);
        if(d.getIntOr("shish_of_everything",0)==1)result.add(Kind.SHISH);return List.copyOf(result);}
    public static Kind kind(ItemStack stack){var all=kinds(stack);return all.isEmpty()?null:all.getFirst();}
    /** Native snowball caller owns its only debit/removal; all original independent raw branches run in order. */
    public boolean thrown(ServerPlayer player,ItemStack stack){
        if(rewinding)return false;
        callbackDepth++;try{if(!admitted(player))return false;var kinds=kinds(stack);if(kinds.isEmpty())return false;
        var authority=new Authority(player.getUUID(),game.items().indirectKills().wildCause(player),game.items().epoch());boolean activated=false;
        for(var kind:kinds)activated=beginAccepted(authority,player.level(),new Vec3(0,64,0),kind)||activated;return activated;
        }finally{callbackDepth--;}
    }
    public void prepare(){int generation=++preparationToken;preparation++;preparing=true;gold=List.of();game.maps().prepareSpellTemplates(List.of(GOLD)).whenComplete((value,error)->game.server().execute(()->{
        if(generation!=preparationToken)return;if(error!=null)SkyWars2.LOGGER.error("Apocalypse gold template preparation failed",error);else gold=value.getOrDefault(GOLD,List.of());preparing=false;}));}
    public boolean prepared(){return !gold.isEmpty();}
    private boolean live(Authority a){return a!=null&&a.cause()!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(a.epoch());}
    private boolean paused(ServerLevel level){return game.items().timeStop().stopped(level);}
    public ServerPlayer owner(Authority a){return a==null?null:game.server().getPlayerList().getPlayer(a.author());}
    private boolean admitted(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.items().wands().validRoleTarget(p,false)
            &&LbcDimensionItems.ordinary(p)&&!paused(p.level())&&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(p);}
    public boolean begin(ServerPlayer p,Kind kind){if(kind==null||!admitted(p))return false;
        var authority=new Authority(p.getUUID(),game.items().indirectKills().wildCause(p),game.items().epoch());
        return beginAccepted(authority,p.level(),new Vec3(0,64,0),kind);}
    /** Initial source position is0,64,0; scheduled phases recover the native SERVER context independently. */
    public boolean beginAccepted(Authority a,ServerLevel level,Vec3 at,Kind kind){
        if(rewinding)return false;
        callbackDepth++;try{if(kind==null||!live(a)||!LbcDimensionItems.ordinary(level)||paused(level))return false;
        if(!prepared())throw new IllegalStateException("Apocalypse template not prepared");
        var context=game.server().createCommandSourceStack().withLevel(level).withPosition(at).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
        var author=owner(a);if(author!=null)context=context.withEntity(author).withRotation(new net.minecraft.world.phys.Vec2(author.getXRot(),author.getYRot()));
        starts++;phase(a,kind,1,context);return true;
        }finally{callbackDepth--;}
    }
    private List<? extends Entity> select(CommandSourceStack context,String literal){try{
        var selector=SELECTORS.get(literal);if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();SELECTORS.put(literal,selector);}
        return List.copyOf(selector.findEntities(context));
    }catch(CommandSyntaxException e){throw new IllegalStateException("Native apocalypse selector "+literal,e);}}
    private static ServerLevel level(Entity entity){return (ServerLevel)entity.level();}
    private List<ServerPlayer> players(CommandSourceStack context){var result=new ArrayList<ServerPlayer>();for(var e:select(context,"@a[gamemode=!spectator]"))if(e instanceof ServerPlayer p&&game.canUseGameplayItems(p)&&!game.isObserver(p))result.add(p);return result;}
    private void title(Component text){for(var p:game.server().getPlayerList().getPlayers()){
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));p.connection.send(new ClientboundSetSubtitleTextPacket(text));}}
    private void translated(String key){title(Component.translatable(key));}
    private void schedule(Authority a,Kind kind,int phase,int delay){var key=new Key(kind,phase);pending.remove(key);pending.put(key,new Pending(a,delay));}
    private void phase(Authority a,Kind kind,int phase,CommandSourceStack context){
        callbackDepth++;try{if(!live(a))return;phases++;
        switch(kind){case END->end(a,phase,context);case BAFFLED->baffled(a,phase,context);case SHISH->shish(a,phase,context);}
        int[] delays=switch(kind){case END->DELAY_END;case BAFFLED->DELAY_BAFFLED;case SHISH->DELAY_SHISH;};
        if(delays[phase]>0)schedule(a,kind,phase+1,delays[phase]);
        }finally{callbackDepth--;}
    }
    public void tick(){
        if(rewinding)return;
        callbackDepth++;try{int now=game.server().getTickCount();if(now==lastTick||!game.server().tickRateManager().runsNormally())return;lastTick=now;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}
        for(var e:List.copyOf(actors))if(e.isRemoved())actors.remove(e);else if(!game.items().currentEpoch(e)){actors.remove(e);e.discard();}
        if(game.items().wands().timeStopped())return;
        var due=new ArrayList<Map.Entry<Key,Pending>>();for(var e:List.copyOf(pending.entrySet())){
            if(!live(e.getValue().authority)){pending.remove(e.getKey(),e.getValue());continue;}
            if(--e.getValue().remaining<=0)due.add(e);}
        for(var e:due)if(pending.remove(e.getKey(),e.getValue()))phase(e.getValue().authority,e.getKey().kind(),e.getKey().phase(),
                game.server().createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput());

        }finally{callbackDepth--;}
    }
    private void end(Authority a,int n,CommandSourceStack c){
        if(n<=15){if(n==1){var v=c.getPosition();c.getLevel().sendParticles(new DustParticleOptions(0xffffff,4),true,false,v.x,v.y,v.z,1000,2,100,2,0);translated("the_salt");}
            for(var e:select(c,"@e[tag=swrg.look]"))cube(a,level(e),e.position(),n,Blocks.AIR,true);return;}
        if(n<=23){if(n==16)translated("our_matter");for(var e:select(c,"@e[tag=swrg.look]"))cube(a,level(e),e.position(),n-15,Blocks.LAVA,false);return;}
        if(n<=27){if(n==24)translated("hermes_bird");for(int[] d:new int[][]{{5,5},{-5,5},{5,-5},{-5,-5},{0,-5},{0,5},{5,0},{-5,0},{0,0}})
            for(var e:select(c,"@e[tag=!spectator,sort=random,limit=10]"))spawn(a,level(e),e.position().add(d[0],4,d[1]),"minecraft:tnt",parse("{fuse:80}"));return;}
        if(n<=31){if(n==28)translated("white_eagle");int[][] offsets=n%2==0?new int[][]{{5,0},{-5,0},{0,5},{0,-5}}:new int[][]{{5,5},{-5,5},{5,-5},{-5,-5}};
            for(var d:offsets)for(var e:select(c,"@e[type=!minecraft:lightning_bolt,tag=!spectator,sort=random,limit="+(n==31?64:48)+"]"))
                spawn(a,level(e),e.position().add(d[0],0,d[1]),"minecraft:lightning_bolt",new CompoundTag());return;}
        switch(n){
            case 32->{translated("yellow_lion");for(var e:select(c,"@e[sort=random,limit=100]"))cube(a,level(e),e.position(),6,Blocks.EMERALD_BLOCK,true,state->state.is(SOLID));}
            case 33,34,35,36->{if(n==33)translated("red_lion");int[][] d={{20,20},{-20,-20},{-20,20},{20,-20}};var v=d[n-33];
                for(var e:select(c,"@e[type=#minecraft:mobs,tag=!spectator,sort=random,limit=64]"))fireball(a,level(e),e.position().add(v[0],20,v[1]),new Vec3(-Math.signum(v[0])*.5,-.5,-Math.signum(v[1])*.5),8);}
            case 37,38,39,40,41,42,43,44,45,46,47,48->{if(n==37)translated("celestial_ruby");int[][] d={{20,0},{0,20},{-20,0},{0,-20}};int index=(n-37)%4;var v=d[index];
                int count=n<=40?16:n<=44?8:4;for(var e:select(c,"@e[type=#minecraft:mobs,tag=!spectator,sort=random,limit="+count+"]"))
                    game.items().apocalypseActors().ruby(a,level(e),e.position().add(v[0],0,v[1]),net.minecraft.world.phys.Vec3.directionFromRotation(0,(index+1)*90));}
            case 49,50,51,52->{if(n==49){translated("magnesia");game.items().polymorph().magnesia(a);}}
            case 53->{translated("the_vessel_of_the_philosophers");for(var e:select(c,"@e[tag=!spectator,sort=random,limit=24]"))game.items().apocalypseActors().spawnCloud(a,level(e),e.position().add(0,30,0));}
            case 54->{translated("our_blessed_stone");for(var e:select(c,"@e[type=!minecraft:player,sort=random,limit=100]")){
                game.items().apocalypseActors().spawnStevari(a,level(e),e.position());
                if((e.getType().builtInRegistryHolder().is(MOBS)||LbcWtf.managed(e))&&!(e instanceof ServerPlayer)&&game.items().wands().validRoleTarget(e,false)){
                    if(e instanceof LivingEntity living)game.items().indirectKills().inheritedExecution(living,a.cause());else e.kill(level(e));}}}
            case 55->{translated("magnum_opus");for(var e:select(c,"@e[type=minecraft:marker,sort=random,limit=32]"))game.items().apocalypseActors().spawnWorm(a,level(e),e.position());}
            case 56->{translated("philosophers_stone");for(var e:select(c,"@e[tag=!spectator,sort=random,limit=20]"))gold(a,level(e),e.position().add(-24,-24,-24));
                for(var e:select(c,"@e[tag=!spectator,sort=random,limit=10]"))spawner(a,level(e),BlockPos.containing(e.position().add(0,10,0)));}
            case 57->{translated("sampo");for(var e:select(c,"@e[type=!minecraft:player,sort=random,limit=16]"))game.items().nukeEvent().startEoe(owner(a),level(e),e.position(),a.cause());}
            case 58->{for(var p:players(c))game.items().mana().award(p,"lbc:true_advancements/legends/end_of_everything");translated("true_knowledge");
                for(var e:select(c,"@e[tag=!spectator]")){var tag=new CompoundTag();tag.putString("LootTable","lbc:lucky_items_1");set(a,level(e),e.blockPosition(),Blocks.CHEST,tag,false);}}
            default->throw new IllegalArgumentException("END phase "+n);
        }
    }
    private void baffled(Authority a,int n,CommandSourceStack c){switch(n){
        case 1->{title(Component.literal("Baffled"));for(var e:select(c,"@e[type=#minecraft:mobs,tag=!spectator]"))if(e instanceof LivingEntity living&&game.items().wands().validRoleTarget(e,false))game.items().legacyEffects().snus(living,a.cause());}
        case 2->{title(Component.literal("Hedgehog"));for(int repeat=0;repeat<2;repeat++)for(var e:select(c,"@e[tag=!spectator]"))spawn(a,level(e),e.position(),"minecraft:silverfish",parse("{Invulnerable:1b,Tags:[minibaffled]}"));}
        case 3->{title(Component.literal("Slab"));for(var e:select(c,"@e[tag=!spectator]")){int y=e.blockPosition().getY();fill(a,level(e),new BlockPos(144,y,144),new BlockPos(-96,y,-96),Blocks.AIR,true,state->true);}}
        case 4->{title(Component.literal("Hedgehog Rider"));for(var e:select(c,"@e[tag=minibaffled]")){
            spawn(a,level(e),e.position(),"minecraft:silverfish",RIDER);
            level(e).sendParticles(ParticleTypes.CLOUD,e.getX(),e.getY()+1,e.getZ(),10,.5,1,.5,.5);
            globalSound(level(e),e.position(),"minecraft:block.fire.extinguish",.5f,1);
            e.teleportTo(level(e),0,-100,0,Set.of(),e.getYRot(),e.getXRot(),true);}}
        case 5->{title(Component.literal("Imbaffled"));String[] flags={"skeleton","bob","aimbob","sauvojen_tuntija"};int[] limits={50,25,25,1};
            for(int i=0;i<flags.length;i++)for(var e:select(c,"@e[type=!minecraft:player,sort=random,limit="+limits[i]+"]")){
                var item=parse("{id:repeating_command_block,count:1,components:{\"minecraft:custom_data\":{drop:1,"+flags[i]+":1}}}");
                var payload=new CompoundTag();payload.put("Item",item);spawn(a,level(e),e.position(),"minecraft:item",payload);}}
        case 6->{title(Component.literal("Yamete Hedgehog"));for(var e:select(c,"@e[tag=!spectator]"))loot(a,level(e),e.position(),"lbc:yameteluckyblock");}
        case 7->{title(Component.literal("SkyBaffled"));for(var e:select(c,"@e[tag=!spectator,limit=15]"))loot(a,level(e),e.position(),"lbc:drops/magic_bean");}
        case 8->{title(Component.literal("Lucky Hedgehog"));for(var p:players(c))if(game.items().wands().validRoleTarget(p,false)){
            p.removeAllEffects();game.items().indirectKills().wildEffect(p,ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.LUCK,-1,4),a.cause());
            for(var literal:LUCKY_ITEMS)try{var stack=new ItemParser(p.registryAccess()).parse(new StringReader(literal.strip())).createItemStack(64);
                if(!p.getInventory().add(stack)&&!stack.isEmpty())drop(a,p.level(),p.position(),stack);
            }catch(CommandSyntaxException error){throw new IllegalStateException("Native Baffled luckyblock source item",error);}
            game.items().mana().award(p,"lbc:true_advancements/legends/end_of_baffled");}}
        default->throw new IllegalArgumentException("BAFFLED phase "+n);
    }}
    private void shish(Authority a,int n,CommandSourceStack c){switch(n){
        case 1->{for(var p:players(c))game.items().chaosTrapActors().spawnForest(a,p.level(),p.position());title(Component.literal("Shish"));}
        case 2->{for(var p:players(c))if(game.items().wands().validRoleTarget(p,false)){
            game.items().reactive().setShishCounter(p,34);game.items().indirectKills().wildEffect(p,ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.LEVITATION,60,16,false,false,false),a.cause());}title(Component.literal("Jet Shish"));}
        case 3->{c.getLevel().getWorldBorder().setCenter(0,0);c.getLevel().getWorldBorder().setSize(382.75);title(Component.literal("The Great Wall"));}
        case 4->{for(int repeat=0;repeat<4;repeat++)for(var p:players(c))spawn(a,p.level(),p.position(),"minecraft:marker",parse("{Tags:[\"pre.sauv_electro\"]}"));
            for(var e:select(c,"@e[type=minecraft:marker,tag=pre.sauv_electro]")){var random=level(e).getRandom();e.setPos(Math.floor(e.getX())+random.nextInt(21)-10,e.getY(),Math.floor(e.getZ())+random.nextInt(21)-10);}
            for(var e:select(c,"@e[type=minecraft:marker,tag=pre.sauv_electro]"))e.setPos(e.position().add(0,8,0));
            for(var e:select(c,"@e[type=minecraft:marker,tag=pre.sauv_electro]"))game.items().apocalypseActors().spawnCloud(a,level(e),e.position(),LbcSauvojen.Kind.ELECTRO);
            for(var e:select(c,"@e[type=minecraft:marker,tag=pre.sauv_electro]"))e.kill(level(e));title(Component.literal("Blue Lightning"));}
        case 5->{for(var p:players(c))loot(a,p.level(),p.position(),"lbc:yellow_lightning");title(Component.literal("Yellow Lightning"));}
        case 6->{game.items().shishEncounter().riskOfRain(a,c.getLevel());title(Component.literal("Risk of Rain"));}
        case 7->title(Component.literal("The End has not come to an End.."));
        case 8->{game.items().shishEncounter().cyborgPhase(a,c);title(Component.literal("CyberShish"));}
        case 9,10->{String temporary=n==9?"pre.anti_mecha":"pre.anti_magic";String tag=n==9?"stopper":"stopper_magic";
            // The source's global temporary selectors may also include preexisting markers bearing these tags.
            for(int repeat=0;repeat<30;repeat++)for(var p:players(c))spawn(a,p.level(),p.position(),"minecraft:marker",parse("{Tags:[\""+temporary+"\"]}"));
            for(var e:select(c,"@e[type=minecraft:marker,tag="+temporary+"]")){var random=level(e).getRandom();
                e.setPos(Math.floor(e.getX())+random.nextInt(61)-30,Math.floor(e.getY())+random.nextInt(61)-30,Math.floor(e.getZ())+random.nextInt(61)-30);}
            for(var e:select(c,"@e[type=minecraft:marker,tag="+temporary+"]"))spawn(a,level(e),new Vec3(Math.floor(e.getX())+.5,Math.floor(e.getY()),Math.floor(e.getZ())+.5),"minecraft:marker",parse("{Tags:["+tag+",rc_1sec,marker_2t,not_block]}"));
            for(var e:select(c,"@e[type=minecraft:marker,tag="+temporary+"]"))e.kill(level(e));
            title(Component.literal(n==9?"End of Technology":"End of Magic"));
            for(var p:game.server().getPlayerList().getPlayers())selfSound(p,n==9?"minecraft:entity.iron_golem.death":"minecraft:block.beacon.deactivate");}
        case 11->{for(var p:players(c))loot(a,p.level(),p.position(),"lbc:source_of_energy");title(Component.literal("Source Of Energy"));
            for(var p:players(c))game.items().mana().award(p,"lbc:true_advancements/legends/end_of_technomagic");}
        default->throw new IllegalArgumentException("SHISH phase "+n);
    }}
    /** /playsound @a applies native distance, without an extra same-dimension recipient restriction. */
    private void globalSound(ServerLevel level,Vec3 at,String key,float volume,float pitch){var sound=net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse(key));
        double range=sound.getRange(volume);long seed=level.getRandom().nextLong();for(var p:game.server().getPlayerList().getPlayers())if(p.position().distanceToSqr(at)<=range*range)
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound),net.minecraft.sounds.SoundSource.MASTER,at.x,at.y,at.z,volume,pitch,seed));}
    private void selfSound(ServerPlayer p,String key){p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse(key))),net.minecraft.sounds.SoundSource.MASTER,
            p.getX(),p.getY(),p.getZ(),1,0,p.level().getRandom().nextLong()));}
    private void fireball(Authority a,ServerLevel level,Vec3 at,Vec3 motion,int power){
        var tag=new CompoundTag();tag.putInt("ExplosionPower",power);var velocity=new net.minecraft.nbt.ListTag();velocity.add(net.minecraft.nbt.DoubleTag.valueOf(motion.x));velocity.add(net.minecraft.nbt.DoubleTag.valueOf(motion.y));velocity.add(net.minecraft.nbt.DoubleTag.valueOf(motion.z));tag.put("Motion",velocity);
        spawn(a,level,at,"minecraft:fireball",tag);
    }
    private Entity spawn(Authority a,ServerLevel level,Vec3 at,String type,CompoundTag original){
        if(!live(a)||paused(level)||!resident(level,BlockPos.containing(at),BlockPos.containing(at)))return null;
        var tag=original.copy();tag.putString("id",type);
        var entity=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag),level,EntitySpawnReason.COMMAND,e->{e.setPos(at);stamp(e,a);return e;});
        if(entity==null)return null;
        var oldAuthority=executingAuthority;var oldLevel=executingLevel;var oldEmitter=executingEmitter;
        executingAuthority=a;executingLevel=level;executingEmitter=entity;
        try{if(!level.tryAddFreshEntityWithPassengers(entity)){entity.getSelfAndPassengers().forEach(e->{actors.remove(e);e.discard();});return null;}
            entity.getSelfAndPassengers().forEach(e->{actors.add(e);game.items().timeStop().prepareFrozen(e);});outputs++;return entity;
        }finally{executingAuthority=oldAuthority;executingLevel=oldLevel;executingEmitter=oldEmitter;}
    }
    private void loot(Authority a,ServerLevel level,Vec3 at,String table){if(!live(a)||paused(level)||!resident(level,BlockPos.containing(at),BlockPos.containing(at)))return;
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,at).create(LootContextParamSets.CHEST);
        var source=game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse(table)));
        for(var stack:source.getRandomItems(params))drop(a,level,at,stack);
    }
    private ItemEntity drop(Authority a,ServerLevel level,Vec3 at,ItemStack stack){var item=new ItemEntity(level,at.x,at.y,at.z,stack.copy());
        var author=owner(a);if(author!=null)item.setThrower(author);item.setDefaultPickUpDelay();stamp(item,a);
        if(level.addFreshEntity(item)){actors.add(item);game.items().timeStop().prepareFrozen(item);game.items().specialDrops().loaded(item);outputs++;return item;}item.discard();return null;
    }
    private void cube(Authority a,ServerLevel level,Vec3 at,int radius,net.minecraft.world.level.block.Block block,boolean strict){cube(a,level,at,radius,block,strict,state->true);}
    private void cube(Authority a,ServerLevel level,Vec3 at,int radius,net.minecraft.world.level.block.Block block,boolean strict,java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> filter){
        var center=BlockPos.containing(at);fill(a,level,center.offset(-radius,-radius,-radius),center.offset(radius,radius,radius),block,strict,filter);}
    private void fill(Authority a,ServerLevel level,BlockPos from,BlockPos to,net.minecraft.world.level.block.Block block,boolean strict,java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> filter){
        queue(a,level,from,to,()->LbcDropBlocks.fill(level,from,to,block,strict,filter));}
    private void set(Authority a,ServerLevel level,BlockPos pos,net.minecraft.world.level.block.Block block,CompoundTag tag,boolean strict){
        queue(a,level,pos,pos,()->{if(level.isOutsideBuildHeight(pos)||level.isDebug())return;var previous=level.getBlockState(pos);
            if(new BlockInput(block.defaultBlockState(),Set.of(),tag==null?null:tag.copy()).place(level,pos,strict?818:258)&&!strict)level.updateNeighboursOnBlockSet(pos,previous);});}
    private void spawner(Authority a,ServerLevel level,BlockPos pos){
        queue(a,level,pos,pos,()->{if(level.isOutsideBuildHeight(pos)||level.isDebug())return;var previous=level.getBlockState(pos);
            if(new BlockInput(Blocks.SPAWNER.defaultBlockState(),Set.of(),SPAWNER.copy()).place(level,pos,258)){
                level.updateNeighboursOnBlockSet(pos,previous);game.items().potionSpawnerDrops().admitEventSpawner(level,pos,a);}});
    }
    private record Operation(ServerLevel level,BlockPos from,BlockPos to,Runnable action){}
    private void queue(Authority a,ServerLevel level,BlockPos from,BlockPos to,Runnable action){var job=new Terrain(a);job.operations.add(new Operation(level,from.immutable(),to.immutable(),action));job.submit();}
    private final class Terrain implements LbcStonekivi.Work {
        final Authority authority;final ArrayDeque<Operation> operations=new ArrayDeque<>();boolean cancelled;
        Terrain(Authority authority){this.authority=authority;}
        void submit(){if(!operations.isEmpty()){terrain.add(this);game.items().stonekivi().queuedWork(this);}}
        public boolean ready(){return cancelled||!live(authority)||operations.isEmpty()||!paused(operations.peek().level());}
        public boolean step(){
        callbackDepth++;try{if(cancelled||!live(authority)){cancel();return true;}var op=operations.poll();if(op==null){cancel();return true;}
            // Source loaded-position failure drops this invocation; do not block the event forever on unloaded terrain.
            if(resident(op.level(),op.from(),op.to()))game.items().indirectKills().trapChanges(op.level(),authority.cause(),op.action());
            if(operations.isEmpty()){cancel();return true;}return false;

        }finally{callbackDepth--;}
    }
        public void cancel(){
        callbackDepth++;try{cancelled=true;operations.clear();terrain.remove(this);
        }finally{callbackDepth--;}
    }
    }
    private void gold(Authority a,ServerLevel level,Vec3 at){var origin=BlockPos.containing(at);var job=new Terrain(a);
        var processor=StructureBlockEntity.createRandom(0);var placement=StructureBlockEntity.createRandom(0);int maxX=0,maxY=0,maxZ=0;
        for(var part:gold){maxX=Math.max(maxX,part.bounds().maxX()+1);maxY=Math.max(maxY,part.bounds().maxY()+1);maxZ=Math.max(maxZ,part.bounds().maxZ()+1);}
        boolean[] accepted={false};job.operations.add(new Operation(level,origin,origin.offset(maxX,maxY,maxZ),()->accepted[0]=true));
        for(var part:gold){var b=part.bounds();int minX=origin.getX()+b.minX(),x=origin.getX()+b.maxX(),minZ=origin.getZ()+b.minZ(),z=origin.getZ()+b.maxZ();
            int y0=Math.max(level.getMinY(),origin.getY()+b.minY()),y1=Math.min(level.getMaxY(),origin.getY()+b.maxY());if(y0>y1)continue;
            var clip=new BoundingBox(minX,y0,minZ,x,y1,z);
            job.operations.add(new Operation(level,new BlockPos(minX,y0,minZ),new BlockPos(x,y1,z),()->{if(!accepted[0])return;
                var settings=new StructurePlaceSettings().setIgnoreEntities(false).setKnownShape(false).setBoundingBox(clip).addProcessor(new BlockRotProcessor(.05f)).setRandom(processor);
                part.template().placeInWorld(level,origin,origin,settings,placement,2);}));
        }job.submit();
    }
    private static boolean resident(ServerLevel level,BlockPos a,BlockPos b){for(int x=Math.min(a.getX(),b.getX())>>4;x<=Math.max(a.getX(),b.getX())>>4;x++)
        for(int z=Math.min(a.getZ(),b.getZ())>>4;z<=Math.max(a.getZ(),b.getZ())>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;return true;}
    private void stamp(Entity entity,Authority a){entity.entityTags().removeIf(t->t.startsWith(AUTHOR)||t.startsWith(ORDER)||t.startsWith(EPOCH));
        entity.entityTags().addAll(List.of(TAG,AUTHOR+a.author(),ORDER+a.cause().order(),EPOCH+a.epoch()));game.items().markCurrentItemEpoch(entity);
        game.items().spellSources().bind(entity,new LbcSpellSource.Source(entity.getUUID(),a.author(),a.epoch(),a.cause(),Set.of(),true));
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(TAG);}
    public static boolean wild(Entity entity){return managed(entity);}
    public Authority authority(Entity entity){if(!managed(entity))return null;UUID author=null;long order=0;String epoch=null;
        for(var tag:entity.entityTags())try{if(tag.startsWith(AUTHOR))author=UUID.fromString(tag.substring(AUTHOR.length()));else if(tag.startsWith(ORDER))order=Long.parseLong(tag.substring(ORDER.length()));else if(tag.startsWith(EPOCH))epoch=tag.substring(EPOCH.length());}catch(IllegalArgumentException ignored){}
        return author==null||order<=0||epoch==null?null:new Authority(author,new LbcIndirectKills.Owner(author,order,true,false),epoch);
    }
    public LbcIndirectKills.Owner cause(Entity entity){var a=authority(entity);return a==null?null:a.cause();}
    public ServerPlayer owner(Entity entity){return owner(authority(entity));}
    public boolean canAffect(Entity source,Entity target){var a=authority(source);return target!=null&&live(a)&&game.items().currentEpoch(source)
            &&game.items().wands().validRoleTarget(target,false)&&!LbcGreed.active(target);}
    public boolean canTarget(Entity source,Entity target){return canAffect(source,target);}
    public void inherit(Entity parent,Entity child){var a=authority(parent);if(live(a)&&game.items().currentEpoch(parent))stamp(child,a);}
    public void inheritChild(Entity child){if(executingAuthority!=null&&child.level()==executingLevel)stamp(child,executingAuthority);}
    public boolean executing(ServerLevel level){return executingAuthority!=null&&executingLevel==level;}
    public Entity currentEmitter(){return executingEmitter;}
    public void loaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;if(!managed(entity))return;var a=authority(entity);
        if(!live(a)||!game.items().currentEpoch(entity)){entity.discard();return;}actors.add(entity);game.items().spellSources().bind(entity,new LbcSpellSource.Source(entity.getUUID(),a.author(),a.epoch(),a.cause(),Set.of(),true));game.items().timeStop().prepareFrozen(entity);
        }finally{callbackDepth--;}
    }
    public void unloaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;actors.remove(entity);
        }finally{callbackDepth--;}
    }
    /** Accepted global scheduled consequences and physical children survive their author's death/disconnect. */
    public void forget(ServerPlayer player){}
    public void stop(){
        callbackDepth++;try{rewinding=false;stagedClock=null;pending.clear();for(var task:List.copyOf(terrain))task.cancel();for(var e:List.copyOf(actors))e.discard();actors.clear();lastTick=Integer.MIN_VALUE;
        }finally{callbackDepth--;}
    }
    public record PendingView(Kind kind,int phase,int ticks,Authority authority){}
    public List<PendingView> pendingSnapshot(){return pending.entrySet().stream().map(e->new PendingView(e.getKey().kind(),e.getKey().phase(),e.getValue().remaining,e.getValue().authority)).toList();}
    public List<Entity> snapshot(){return List.copyOf(actors);}public int terrainCount(){return terrain.size();}
    public long startCount(){return starts;}public long phaseCount(){return phases;}public long outputCount(){return outputs;}

    /** Terrain invocations finish before capture; no retained Runnable or template cursor is imaged. */
    boolean captureReady(){return callbackDepth==0&&executingAuthority==null&&!preparing&&terrain.isEmpty()&&!rewinding;}
    void checkpointBoundary(){if(callbackDepth!=0||executingAuthority!=null)throw new IllegalStateException("Finish apocalypse event callbacks/preparation before checkpointing");}
    void cancelFutureTerrainForRollback(){checkpointBoundary();preparationToken++;preparing=false;for(var work:List.copyOf(terrain))work.cancel();}
    void prepareFrozen(){checkpointBoundary();cancelFutureTerrainForRollback();rewinding=true;stagedClock=null;}
    void publishFrozen(){checkpointBoundary();if(stagedClock!=null)lastTick=stagedClock.last(game);stagedClock=null;rewinding=false;}
    void cancelFrozen(){checkpointBoundary();stagedClock=null;rewinding=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();if(!captureReady())throw new IllegalStateException("Complete apocalypse event terrain before capture");return new Checkpoint(links);}
    final class Checkpoint {
        private record Timer(Key key,Pending original,int remaining){}
        private final List<Timer> timers=pending.entrySet().stream().map(e->new Timer(e.getKey(),e.getValue(),e.getValue().remaining)).toList();
        private final List<MatchDamageSources.Ref> bodies;
        private final List<MapManager.TemplateSlice> preparedGold=gold;
        private final int preparedVersion=preparation;
        private final long savedStarts=starts,savedPhases=phases,savedOutputs=outputs;
        private final MatchAuthorityCheckpoint.Clock clock=new MatchAuthorityCheckpoint.Clock(game,lastTick,0);
        Checkpoint(MatchSpellCheckpoint.Links links){bodies=actors.stream().map(links::key).toList();for(var body:bodies)if(body.historical()!=null)throw new IllegalStateException("Capture live apocalypse child bodies");}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restored=new java.util.LinkedHashSet<Entity>();for(var body:bodies)restored.add(links.get(body));
            return ()->{pending.clear();for(var timer:timers){timer.original.remaining=timer.remaining;pending.put(timer.key,timer.original);}
                actors=restored;gold=preparedGold;preparation=preparedVersion;preparationToken++;preparing=false;
                starts=savedStarts;phases=savedPhases;outputs=savedOutputs;stagedClock=clock;};
        }
    }
}
