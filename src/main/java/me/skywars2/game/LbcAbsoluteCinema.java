package me.skywars2.game;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

/** One match choice per participant. Life counts, loot, acid, guns and controlled mobs keep their existing owners. */
public final class LbcAbsoluteCinema {
    public enum Role {
        PUTIN(""),HITLER(""),TRUMP("dtrump"),LENIN("lenin"),STALIN("stalin"),POZDMAN("pozdman"),NAHIDA("nahida");
        final String tag;Role(String tag){this.tag=tag;}
    }
    private record State(Role role,String epoch,boolean awakened,boolean pending,GameType previous,boolean modeInstalled){}
    private static final Identifier POZDMAN=Identifier.withDefaultNamespace("pozdman");
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.withDefaultNamespace("mobs"));
    private static final TagKey<net.minecraft.world.damagesource.DamageType> PLAYER_ATTACK=TagKey.create(Registries.DAMAGE_TYPE,Identifier.withDefaultNamespace("is_player_attack"));
    public static final String ACID="skywars2.cinema_acid";
    private final GameController game;
    private final LbcOreshnik rockets;
    private Map<UUID,State> roles=new LinkedHashMap<>();
    private int trumpRule,callbacks;private boolean retaliating,restoring;

    public LbcAbsoluteCinema(GameController game){this.game=game;rockets=new LbcOreshnik(game);}
    static Role roleFor(int roll){
        if(roll<1||roll>601)throw new IllegalArgumentException("Cinema roll must be in 1..601");
        return roll==601?Role.NAHIDA:Role.values()[(roll-1)/100];
    }
    public Role role(UUID id){var state=roles.get(id);return state==null?null:state.role;}
    public int trumpRule(){return trumpRule;}
    public LbcOreshnik rockets(){return rockets;}
    /** Only the canonical initial match-start caller invokes this after applying the loadout. */
    public boolean start(ServerPlayer player,List<Vec3> originalCenters){
        if(restoring||!game.lbc().hasSkill(player,35)||roles.containsKey(player.getUUID()))return false;
        startRoll(player,player.getRandom().nextInt(601)+1,originalCenters);return true;
    }
    void startRoll(ServerPlayer player,int roll,List<Vec3> originalCenters){
        if(roles.containsKey(player.getUUID()))throw new IllegalStateException("Cinema start already granted");
        var role=roleFor(roll);roles.put(player.getUUID(),new State(role,game.items().epoch(),false,false,null,false));
        if(!role.tag.isEmpty())player.entityTags().add(role.tag);
        switch(role){
            case PUTIN -> {
                if(!originalCenters.isEmpty())rockets.launch(player,originalCenters.get(player.getRandom().nextInt(originalCenters.size())).add(0,100,0));
                broadcast("putin",0xAA0000,true,null);
            }
            case HITLER -> {startAcid(player);broadcast("hitler",0xAA0000,true,null);identity(player,"hitler");}
            case TRUMP -> {
                trumpRule=player.getRandom().nextInt(2)+1;broadcast("dtrump_rule_"+trumpRule,0xFF5555,false,null);identity(player,"trump");
                if(player.getRandom().nextFloat()<.75f)trumpRule=0;
            }
            case LENIN -> {broadcast("lenin_spawn",0xFF5555,false,null);identity(player,"lenin");}
            case STALIN -> {give(player,"lbc:stalin_ak_finite",LbcAk47.ITEM);broadcast("stalin_spawn",0xFF5555,false,null);identity(player,"stalin");}
            case POZDMAN -> {modifiers(player,true);give(player,"lbc:pozdman_bot",LbcPozdmanBot.ITEM);broadcast("pozdman_spawn",0xFF5555,false,null);identity(player,"pozdman");}
            case NAHIDA -> {broadcast("lenin_spawn",0xFF5555,false,player);identity(player,"nahida");}
        }
    }
    private void give(ServerPlayer p,String table,Item item){
        var loot=game.lbc().rollTable(p.level(),p.position(),p,table);
        if(loot.size()!=1||loot.getFirst().getCount()!=1)throw new IllegalStateException("Cinema requires exactly one source item from "+table+"; got "+loot);
        for(var source:loot){
            var stack=source.transmuteCopy(item);
            stack.applyComponents(source.getComponents());
            if(item==LbcAk47.ITEM&&!LbcAk47.isFinite(stack))throw new IllegalStateException("Cinema requires exact finite AK loot: table="+table+" source="+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(source.getItem())+" x"+source.getCount()+" normalized="+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())+" finite="+LbcAk47.isFinite(stack));
            p.getInventory().placeItemBackInInventory(stack,net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
    private void identity(ServerPlayer p,String name){p.sendSystemMessage(Component.translatable("is_"+name).withStyle(s->s.withColor(0x55FFFF)));}
    private void broadcast(String key,int color,boolean bold,ServerPlayer exceptNear){
        var text=Component.translatable(key).withStyle(s->s.withColor(color).withBold(bold));
        for(var viewer:game.server().getPlayerList().getPlayers())if(exceptNear==null||viewer.level()==exceptNear.level()&&viewer.distanceToSqr(exceptNear)>=1)viewer.sendSystemMessage(text);
    }
    private void startAcid(ServerPlayer p){
        for(var spawn:game.maps().spawns()){
            var point=spawn.position();if(point.distanceToSqr(p.position())<25)continue;
            if(p.level().getPlayers(q->q.position().distanceToSqr(point)<=100,1).isEmpty())continue;
            for(int n=0;n<8;n++){
                float yaw=(p.getRandom().nextInt(360001)-180000)*.001f,pitch=(p.getRandom().nextInt(60001)+30000)*.001f;
                game.items().clouds().cinemaAcid(p,point.add(0,2+.01*(1+n%3),0),yaw,pitch);
            }
        }
    }
    private State current(ServerPlayer p){var state=roles.get(p.getUUID());return state!=null&&game.items().currentEpoch(state.epoch)?state:null;}
    private boolean active(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&game.playing()&&game.canUseGameplayItems(p)&&!game.isObserver(p)&&current(p)!=null;}
    /** Ordinary and BedWars body replacement calls this after its survival reset; no start rewards recur. */
    public void bodyReady(ServerPlayer p){
        var state=current(p);if(state==null)return;
        if(!state.role.tag.isEmpty())p.entityTags().add(state.role.tag);
        if(state.role==Role.POZDMAN)modifiers(p,true);
        if(state.awakened){
            GameType previous=state.previous==null?p.gameMode():state.previous;
            p.setGameMode(GameType.CREATIVE);
            roles.put(p.getUUID(),new State(state.role,state.epoch,true,false,previous,true));
            if(state.pending)p.sendSystemMessage(Component.translatable("nahida_awaken"));
        }
    }
    /** Return true exactly once. Caller grants one canonical life BEFORE its existing death debit. */
    public boolean beforeLastLifeLost(ServerPlayer p,int lives,boolean hasBed){
        var state=current(p);
        if(restoring||state==null||state.role!=Role.NAHIDA||state.awakened||lives!=1||hasBed)return false;
        roles.put(p.getUUID(),new State(state.role,state.epoch,true,true,p.gameMode(),false));return true;
    }
    public void tick(){
        if(restoring)return;
        for(var id:List.copyOf(roles.keySet())){
            var p=game.server().getPlayerList().getPlayer(id);if(!active(p)||game.items().timeStop().stopped(p.level()))continue;
            var state=current(p);if(state.role!=Role.LENIN)continue;
            for(var target:p.level().getEntities((Entity)null,p.getBoundingBox().inflate(1),e->!(e instanceof net.minecraft.world.entity.player.Player)
                    &&e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(p.position())<=1
                    &&game.items().wands().validRoleTarget(e,false,true)&&!LbcGreed.active(e)&&!game.items().polymorph().concealed(e)))
                game.items().communism().subjugate(p,target,true,true);
        }
        rockets.tick();
    }
    /** Call from the actual PlayerHurtEntityTrigger bridge, preserving its damage-type tag split. */
    public void outgoing(ServerPlayer attacker,Entity victim,DamageSource source,float original,float actual,boolean blocked){
        if(restoring||retaliating||blocked||trumpRule==0||!(victim instanceof ServerPlayer)||!LbcDimensionItems.ordinary(attacker))return;
        boolean melee=source.is(PLAYER_ATTACK);
        if(trumpRule!=(melee?1:2))return;
        var candidates=roles.entrySet().stream().filter(e->e.getValue().role==Role.TRUMP).map(e->game.server().getPlayerList().getPlayer(e.getKey())).filter(this::active).toList();
        if(candidates.isEmpty())return;var author=candidates.get(attacker.getRandom().nextInt(candidates.size()));
        retaliating=true;callbacks++;
        try{attacker.hurtServer(attacker.level(),attacker.damageSources().playerAttack(author),10);broadcast("trump_angry",0xFFFFFF,false,null);}
        finally{callbacks--;retaliating=false;}
    }
    /** One call per actual native victim death, before role/life cleanup; no synthetic/log-out deaths. */
    public void playerKilled(ServerPlayer killer,ServerPlayer victim,DamageSource source){
        var state=killer==null?null:current(killer);
        if(restoring||state==null||state.role!=Role.POZDMAN||source==null||!source.is(PLAYER_ATTACK)||source.getDirectEntity()!=killer||source.getEntity()!=killer)return;
        var wolf=EntityTypes.WOLF.create(victim.level(),EntitySpawnReason.COMMAND);if(wolf==null)throw new IllegalStateException("Cinema wolf construction");
        wolf.setPos(victim.position());wolf.setPersistenceRequired();wolf.setOwner(killer);wolf.setTame(true,false);wolf.setOrderedToSit(false);wolf.setComponent(DataComponents.WOLF_COLLAR,DyeColor.RED);
        wolf.setCustomName(Component.translatable("ebynavy_dog"));wolf.setCustomNameVisible(true);
        wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(9);wolf.setHealth(40);
        game.items().summons().track(killer,wolf);game.items().communism().subjugate(killer,wolf,true,true);
        game.items().timeStop().prepareFrozen(wolf);if(!victim.level().addFreshEntity(wolf))wolf.discard();
    }
    private static void modifiers(ServerPlayer p,boolean install){
        for(var pair:List.of(Map.entry(Attributes.ATTACK_DAMAGE,2d),Map.entry(Attributes.ARMOR,4d))){
            var attribute=p.getAttribute(pair.getKey());if(attribute==null)continue;attribute.removeModifier(POZDMAN);
            if(install)attribute.addTransientModifier(new AttributeModifier(POZDMAN,pair.getValue(),AttributeModifier.Operation.ADD_VALUE));
        }
    }
    public void forget(ServerPlayer p){
        var state=roles.remove(p.getUUID());if(state==null)return;
        if(!state.role.tag.isEmpty())p.entityTags().remove(state.role.tag);
        if(state.role==Role.POZDMAN)modifiers(p,false);
        if(state.modeInstalled&&p.gameMode()==GameType.CREATIVE&&state.previous!=null)p.setGameMode(state.previous);
    }
    public void stop(){
        for(var id:List.copyOf(roles.keySet())){var p=game.server().getPlayerList().getPlayer(id);if(p!=null)forget(p);}
        roles.clear();trumpRule=0;rockets.stop();
    }
    /** Clone only logical state; inventory/native actor adapters already own their actual copies. */
    public void copyPlayer(ServerPlayer original,ServerPlayer copy){var state=current(original);if(state!=null){roles.put(copy.getUUID(),state);bodyReady(copy);}}
    void boundary(){if(callbacks!=0||retaliating)throw new IllegalStateException("Finish Cinema criterion before snapshot");rockets.boundary();}
    void prepareFrozen(){boundary();restoring=true;rockets.prepareFrozen();}
    void publishFrozen(){boundary();restoring=false;rockets.publishFrozen();}
    void cancelFrozen(){boundary();restoring=false;rockets.publishFrozen();}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){boundary();return new Checkpoint(graph);}
    final class Checkpoint {
        final Map<UUID,State> image=Collections.unmodifiableMap(new LinkedHashMap<>(roles));final int rule=trumpRule;final LbcOreshnik.Checkpoint flight;
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){flight=rockets.checkpoint(graph);}
        Runnable prepare(MatchSpellCheckpoint.Links links){var install=flight.prepare(links);return ()->{roles=new LinkedHashMap<>(image);trumpRule=rule;install.run();};}
    }
}
