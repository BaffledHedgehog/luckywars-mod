package me.skywars2.game;

import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Source Ender and Bad Wiki casts. Native effects and the existing RTP service own timers. */
public final class LbcEnderWiki {
    record Effect(String id,int ticks,int amplifier,boolean hidden){}
    static final Effect[] RANDOM=new Effect[84],ACADEMY=new Effect[28];
    static {
        try(var input=LbcEnderWiki.class.getResourceAsStream("/skywars2/bad_wiki_effects.json")){
            if(input==null)throw new IllegalStateException("Missing Bad Wiki presets");
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var row:json.getAsJsonArray("random")){var r=row.getAsJsonObject();RANDOM[r.get("index").getAsInt()-1]=new Effect(r.get("effect").getAsString(),r.get("ticks").getAsInt(),r.get("amplifier").getAsInt(),r.get("hidden").getAsBoolean());}
            for(var row:json.getAsJsonArray("academy")){var r=row.getAsJsonObject();ACADEMY[r.get("mode").getAsInt()-1]=new Effect(r.get("effect").getAsString(),r.get("ticks").getAsInt(),0,r.get("hidden").getAsBoolean());}
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    private final GameController game;
    LbcEnderWiki(GameController game){this.game=game;}
    boolean use(ServerPlayer p,InteractionHand hand,ItemStack stack,boolean wiki,boolean academy){
        if(!game.items().mana().visible(p)||game.items().actionsBlocked(p)||p.entityTags().contains("time_stopped")
                ||game.items().timeStop().stopped(p.level())||game.items().magic().suppressed(p)
                ||game.items().magic().suppressedAt(p.level(),p.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        int mode=Math.max(1,Math.min(28,data.getIntOr("mode",1))),tier=Math.max(1,Math.min(3,data.getIntOr("tier",1)));
        if(wiki&&academy&&p.isShiftKeyDown()){
            // Disjoint pitch ranges fix the legacy double switch on the exact horizon/boundaries.
            if(p.getXRot()<=-89)tier=tier%3+1;
            else if(p.getXRot()>=89)tier=(tier+1)%3+1;
            else if(p.getXRot()<=0)mode=mode%28+1;
            else mode=(mode+26)%28+1;
            data.putInt("mode",mode);data.putInt("tier",tier);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));p.getInventory().setChanged();
            p.sendSystemMessage(Component.translatable("lbc_"+(mode==28?"random_teleport":ACADEMY[mode-1].id())).append(Integer.toString(tier)));return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly(wiki?"bad_wiki_mana_use":"ender_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(p,cost))return false;
        if(wiki){
            int random=academy?0:p.getRandom().nextInt(84);
            var e=academy?ACADEMY[mode-1]:RANDOM[random];
            int amplifier=academy?tier-1+(mode==28?40:0):e.amplifier();
            apply(p,e,amplifier);
            if(!academy&&random==62)game.items().mana().award(p,"lbc:true_advancements/staffs/badwiki");
            if(!academy&&LbcAcademy.trial(stack)&&java.util.Arrays.stream(ACADEMY).filter(v->!Set.of("instant_health","instant_damage","saturation","raid_omen").contains(v.id()))
                    .allMatch(v->p.hasEffect(BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(v.id())).orElseThrow())))
                game.items().academy().ready(p,hand,stack);
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN),net.minecraft.sounds.SoundSource.MASTER,p.getX(),p.getY(),p.getZ(),10,1,p.getRandom().nextLong()));
        }else ender(p,hand,stack,academy);
        game.items().wands().wandCasted(p);return true;
    }
    void apply(ServerPlayer p,Effect e,int amplifier){
        var type=BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(e.id())).orElseThrow();
        p.addEffect(ItemEffectLevels.create(type,e.ticks(),amplifier,false,!e.hidden()),p);
    }
    void libraryEffect(ServerPlayer p,Effect effect){
        apply(p,effect,effect.amplifier());
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN),net.minecraft.sounds.SoundSource.MASTER,p.getX(),p.getY(),p.getZ(),10,1,p.getRandom().nextLong()));
    }
    void ender(ServerPlayer p,InteractionHand hand,ItemStack stack,boolean academy){
        Vec3 origin=p.position();
        if(academy)p.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,60,0,false,false),p);
        LbcOverload.sound(p,origin,"minecraft:entity.enderman.teleport",2,1,16);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT),net.minecraft.sounds.SoundSource.MASTER,origin.x,origin.y,origin.z,200,1,p.getRandom().nextLong()));
        p.level().sendParticles(ParticleTypes.PORTAL,true,false,origin.x,origin.y,origin.z+1,100,.2,.5,.2,.5);
        var destination=LbcSurfaceTeleport.destination(p.level(),game.config().noBorders?origin:Vec3.ZERO,100,
                p.level().dimension()==Level.NETHER?125:p.level().getMaxY()+1,p.getRandom());
        if(destination!=null)p.teleportTo(p.level(),destination.x,destination.y+.5,destination.z,Set.of(),p.getYRot(),p.getXRot(),true);
        p.level().sendParticles(ParticleTypes.REVERSE_PORTAL,true,false,origin.x,origin.y,origin.z+1,100,.2,.5,.2,.5);
        LbcOverload.sound(p,origin,"minecraft:entity.enderman.teleport",2,1,16);
        if(p.level().players().stream().anyMatch(e->e!=p&&!e.isSpectator()&&e.distanceToSqr(p)>=.0001&&e.distanceToSqr(p)<=64))
            game.items().mana().award(p,"lbc:true_advancements/staffs/ender_random");
        if(!academy&&LbcAcademy.trial(stack)&&game.items().mana().score(p,"falltimer").get()>=100)game.items().academy().ready(p,hand,stack);
    }
    /** Legacy airborne counter is a raw tick score, including the existing KFC +1 bridge. */
    void tick(){for(var p:game.server().getPlayerList().getPlayers())if(game.items().mana().visible(p)){
        var counter=game.items().mana().score(p,"falltimer");if(p.onGround())counter.set(0);else counter.add(1);
    }}
}
