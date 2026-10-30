package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/** Zero-mana technological airstrike. The original scoreboard is the sole cooldown authority. */
public final class LbcHohlopitek {
    public static final Item ITEM=registerItem();
    private static final String COOLDOWN="coldownhohol";
    private static final CompoundTag FLAG=flag();
    // (user) No fixed range: the shared loaded terrain/target search owns termination.
    static final double RAY_RANGE=Double.POSITIVE_INFINITY;
    private final GameController game;
    private final LbcMana mana;
    // This stores only the sub-unit active-tick phase, never a second cooldown value.
    private final Map<ServerPlayer,Integer> phases=new IdentityHashMap<>();
    private final Map<LbcExasperato.RayTicket,ServerPlayer> rays=new LinkedHashMap<>();

    public LbcHohlopitek(GameController game,LbcMana mana) { this.game=game; this.mana=mana; }
    public static void register() { }
    private static CompoundTag flag() { var tag=new CompoundTag(); tag.putInt("hohlopitek",1); return tag; }
    private static Item registerItem() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:hohlopitek"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:hohlopitek"); }
        }.setId(key).durability(25).overrideDescription("hohlopitek"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null && player instanceof ServerPlayer owner
                        && controller.items().wands().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    public static boolean isItem(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty() && (stack.is(ITEM) || data!=null && data.matchedBy(FLAG));
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && mana.visible(owner) && game.items().wands().validTarget(owner);
    }
    private boolean admitted(ServerPlayer owner) {
        return valid(owner) && !game.items().actionsBlocked(owner)
                && !owner.entityTags().contains("time_stopped") && !game.items().timeStop().stopped(owner.level())
                && !game.items().magic().mechanismSuppressed(owner);
    }
    public boolean use(ServerPlayer owner,InteractionHand hand) {
        if(!isItem(owner.getItemInHand(hand)) || !admitted(owner) || cooldown(owner)>0
                || hand==InteractionHand.OFF_HAND && !game.config().offhandWands) return false;
        Vec3 origin=owner.getEyePosition();
        if(hand==InteractionHand.OFF_HAND) origin=origin.add(owner.getLookAngle().scale(.5));
        if(game.items().magic().mechanismSuppressedAt(owner.level(),origin)) return false;
        // Both hands, KFC, HUD, Envy and Vanity see exactly the same original raw score.
        mana.score(owner,COOLDOWN).set(200); phases.remove(owner);
        for(String sound:List.of("minecraft:entity.pig.ambient","minecraft:entity.pig.death","minecraft:entity.pig.hurt"))
            LbcOverload.sound(owner,origin,sound,3,1,0);
        game.items().exasperato().replayableLoaded(owner,origin,owner.getLookAngle(),
                LbcExasperato.RayMode.TECHNOLOGY,RAY_RANGE,new BomberAim(owner));
        return true;
    }
    private final class BomberAim implements LbcExasperato.ReplayableRay {
        final UUID author;final String epoch;final ServerLevel level;final float yaw;
        LbcExasperato.RayTicket ticket;double walked,nextSample;
        BomberAim(ServerPlayer owner){author=owner.getUUID();epoch=game.items().epoch();level=owner.level();yaw=owner.getYRot();}
        ServerPlayer owner(){return game.server().getPlayerList().getPlayer(author);}
        public void submitted(LbcExasperato.RayTicket value){ticket=value;rays.put(value,owner());}
        public boolean block(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return true;}
        public boolean target(net.minecraft.world.entity.Entity entity){var owner=owner();return owner!=null&&game.items().lifetimes().target(owner,entity);}
        public void trail(Vec3 from,Vec3 to){
            Vec3 delta=to.subtract(from);double length=delta.length(),end=walked+length;
            while(nextSample<=end+1e-7){
                Vec3 at=length<1e-9?from:from.add(delta.scale(Math.max(0,nextSample-walked)/length));
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.PORKCHOP),at.x,at.y,at.z,2,.5,.5,.5,0);nextSample++;
            }walked=end;
        }
        public void complete(HitResult hit){
            rays.remove(ticket);var owner=owner();
            if(hit==null||!game.items().currentEpoch(epoch)||owner==null||owner.level()!=level||!admitted(owner))return;
            completing++;try{game.items().droneDrops().spawnBomber(owner,hit.getLocation(),yaw);
                LbcOverload.sound(owner,hit.getLocation().add(0,20,0),"minecraft:lbcsounds.bambam",3,1,0);
            }finally{completing--;}
        }
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
            double distance=walked,sample=nextSample;return ignored->()->{walked=distance;nextSample=sample;};
        }
    }
    private int completing;private boolean advancing;
    void checkpointBoundary(){if(completing!=0||advancing)throw new IllegalStateException("Finish bomber callbacks before checkpoint");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        final MatchSpellCheckpoint.IdentityImage<ServerPlayer,Integer> phaseImage;
        final Map<LbcExasperato.RayTicket,MatchDamageSources.Ref> requests=new LinkedHashMap<>();
        @SuppressWarnings("unchecked") Checkpoint(MatchSpellCheckpoint.Links links){
            phaseImage=new MatchSpellCheckpoint.IdentityImage<>((IdentityHashMap<ServerPlayer,Integer>)phases,links);
            rays.forEach((ticket,owner)->requests.put(ticket,links.key(owner)));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var restored=phaseImage.restore(links);var pending=new LinkedHashMap<LbcExasperato.RayTicket,ServerPlayer>();
            requests.forEach((ticket,ref)->pending.put(ticket,(ServerPlayer)links.get(ref)));
            return ()->{phases.clear();phases.putAll(restored);rays.clear();rays.putAll(pending);};
        }
    }
    public int cooldown(ServerPlayer owner) { return Math.max(0,mana.score(owner,COOLDOWN).get()); }
    public int hudValue(ServerPlayer owner) { return cooldown(owner); }
    public void tick() {
        if(advancing)throw new IllegalStateException("Reentrant bomber tick");advancing=true;try {
        for(var owner:game.server().getPlayerList().getPlayers()) {
            if(!valid(owner)) { clear(owner); continue; }
            if(cooldown(owner)==0) { phases.remove(owner); continue; }
            if(game.items().timeStop().stopped(owner.level()) || owner.entityTags().contains("time_stopped")) continue;
            int phase=phases.getOrDefault(owner,0)+1;
            if(phase==5) { mana.score(owner,COOLDOWN).add(-1); phases.remove(owner); }
            else phases.put(owner,phase);
        }
        for(var entry:List.copyOf(rays.entrySet())) if(!valid(entry.getValue())) entry.getKey().cancel();
        }finally{advancing=false;}
    }
    public int pendingRayCount() { return rays.size(); }
    public int activeCount() {
        int count=rays.size();
        for(var player:game.server().getPlayerList().getPlayers()) if(cooldown(player)>0) count++;
        return count;
    }
    public void clear(ServerPlayer owner) {
        mana.score(owner,COOLDOWN).set(0); phases.remove(owner);
        for(var entry:List.copyOf(rays.entrySet())) if(entry.getValue()==owner) entry.getKey().cancel();
    }
    public void forget(ServerPlayer owner) { clear(owner); }
    public void stop() {
        for(var player:game.server().getPlayerList().getPlayers()) mana.score(player,COOLDOWN).set(0);
        phases.clear();
        for(var ray:List.copyOf(rays.keySet())) ray.cancel();
        rays.clear();
    }
}
