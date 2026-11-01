package me.skywars2.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;

/** Native effect callbacks and complete hidden chains, without manual effect or player ticking. */
public final class GovnomancerChecks {
    private GovnomancerChecks(){}
    private static final List<Holder<MobEffect>> CLEARED=List.of(MobEffects.NAUSEA,MobEffects.POISON,MobEffects.WITHER);
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Govnomancer: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer p;final LbcGovnomancer service;
        final RegenerationBeltChecks.Snapshot saved;final Set<String> tags;
        final boolean skills,frozen;final List<Marker> fields=new ArrayList<>();
        Fixture(GameController game){
            this.game=game;p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(p!=null&&game.playing()&&game.canUseGameplayItems(p)&&p.level()==game.maps().level(),"real active participant");
            require(!game.items().timeStop().stopped(p.level()),"no borrowed Time Stop");
            saved=new RegenerationBeltChecks.Snapshot(game,p);tags=new HashSet<>(p.entityTags());
            skills=game.config().lbcSkills;frozen=game.server().tickRateManager().isFrozen();service=new LbcGovnomancer(game);
        }
        void initialize(){
            game.config().lbcSkills=false;
            p.removeAllEffects();p.getInventory().clearContent();
            var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcKit=0;profile.lbcChallengeKit=0;
            profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","sin_greed_active","nomagic_active","rcdenyalways"))p.entityTags().remove(tag);
            game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            game.server().tickRateManager().setFrozen(false);
        }
        void layers(){
            // Disable the selected passive while constructing genuine native weaker hidden layers.
            game.config().lbcSkills=false;p.removeAllEffects();
            for(var effect:CLEARED){
                p.addEffect(new MobEffectInstance(effect,400,0));p.addEffect(new MobEffectInstance(effect,50,2));
                var current=p.getEffect(effect);
                require(current!=null&&current.getAmplifier()==2&&((EffectLayers.Access)current).skywars2$layerHidden()!=null,"real active/hidden chain "+effect);
            }
            p.addEffect(new MobEffectInstance(MobEffects.SPEED,321,1));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,222,0));
            game.config().lbcSkills=true;game.players().get(p.getUUID()).lbcSkill=9;
        }
        void retained(String why){for(var effect:CLEARED)require(p.hasEffect(effect),why+" retains "+effect);}
        void cleared(String why){
            for(var effect:CLEARED)require(!p.hasEffect(effect)&&p.getEffect(effect)==null,why+" deletes complete native type "+effect);
            require(p.hasEffect(MobEffects.SPEED)&&p.getEffect(MobEffects.SPEED).getDuration()==321
                    &&p.getEffect(MobEffects.SPEED).getAmplifier()==1,"unrelated beneficial effect unchanged");
            require(p.hasEffect(MobEffects.BLINDNESS)&&p.getEffect(MobEffects.BLINDNESS).getDuration()==222,"unrelated harmful effect unchanged");
        }
        void cleanse(){game.items().effectsChanged(p);cleared("common effects_changed callback");}
        void field(String tag){
            var m=new Marker(EntityTypes.MARKER,p.level());m.setPos(p.position());m.entityTags().add(tag);
            require(p.level().addFreshEntity(m),"actual suppression marker "+tag);fields.add(m);game.items().magic().loaded(m);
        }
        void checks(){
            layers();service.effectsChanged(p);cleared("direct source payload");
            layers();cleanse();
            for(var effect:CLEARED){p.addEffect(new MobEffectInstance(effect,100,0));require(!p.hasEffect(effect),"actual native incoming effect callback "+effect);}
            layers();game.config().lbcSkills=false;service.effectsChanged(p);retained("disabled skills");game.config().lbcSkills=true;cleanse();
            layers();game.players().get(p.getUUID()).lbcSkill=0;service.effectsChanged(p);retained("unselected skill");game.players().get(p.getUUID()).lbcSkill=9;cleanse();
            for(var tag:List.of("time_stopped","strict_map_object")){
                layers();p.entityTags().add(tag);try{service.effectsChanged(p);retained("guard "+tag);}finally{p.entityTags().remove(tag);}cleanse();
            }
            layers();game.server().tickRateManager().setFrozen(true);
            try{service.effectsChanged(p);retained("global native freeze");}finally{game.server().tickRateManager().setFrozen(false);}cleanse();
            field("stopper_magic");field("stopper");
            require(game.items().magic().suppressed(p)&&game.items().magic().mechanismSuppressed(p),"actual MAG and TECH admission controls");
            layers();cleanse();
            for(var m:List.copyOf(fields)){game.items().magic().unloaded(m);m.discard();fields.remove(m);}
            p.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));
            game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            require(game.items().magic().artifactActive(p)&&game.items().magic().artifactSuppressedAt(p.level(),p.position()),"actual active antimagic artifact");
            layers();cleanse();
            for(var effect:CLEARED){p.addEffect(new MobEffectInstance(effect,100,0));require(!p.hasEffect(effect),"native protection remains physical under artifact "+effect);}
            require(!LbcDimensionItems.ordinary("minecraft:nexus")&&!LbcDimensionItems.ordinary("minecraft:imprinted")
                    &&LbcDimensionItems.ordinary(p),"shared dimension boundary");
        }
        @Override public void close(){
            game.config().lbcSkills=false;
            try{
                for(var m:List.copyOf(fields)){game.items().magic().unloaded(m);m.discard();}fields.clear();
                p.getInventory().clearContent();game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
                p.entityTags().clear();p.entityTags().addAll(tags);saved.restore(game);saved.restoreAttributes();saved.restoreUse();
            }finally{
                game.config().lbcSkills=skills;game.server().tickRateManager().setFrozen(frozen);
                game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            }
        }
    }
    public static void run(GameController game){
        try(var f=new Fixture(game)){f.initialize();f.checks();}
        SkyWars2.LOGGER.info("[check] PASS govnomancer (source skill9 native effects_changed; complete Nausea/Poison/Wither hidden chains; other buff/debuff retained; config/profile/role/TimeStop-tag/freeze guards; physical MAG/TECH/active-artifact admission)");
    }
}
