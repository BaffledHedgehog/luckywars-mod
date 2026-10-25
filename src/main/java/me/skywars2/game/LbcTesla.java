package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Tesla input and its finite bent instant ray; existing Electro owns moving arcs and conduction. */
final class LbcTesla {
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> CONDUCTIVE=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:electroprovod"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final DustParticleOptions WHITE=new DustParticleOptions(0xffffff,.5f),PURPLE=new DustParticleOptions(0x6f4bb3,.75f);
    private LbcTesla(){}
    static boolean use(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int mode=Math.clamp(data.getIntOr("mode",1),1,3);
        if(academy&&owner.isShiftKeyDown()){
            mode=mode%3+1;data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();
            owner.sendSystemMessage(Component.translatable(switch(mode){case 2->"mode_instant";case 3->"mode_chain";default->"mode_default";}));return true;
        }
        int cost=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("lightning_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        if(academy&&mode==2)game.items().stonekivi().queuedWork(new Instant(game,owner));
        else game.items().wands().teslaVolley(owner,hand,stack,academy&&mode==3);
        game.items().wands().wandCasted(owner);return true;
    }
    /** A short source sample group is one cooperative work unit; no marker per sample or new queue. */
    static final class Instant implements LbcStonekivi.Work {
        final GameController game;final ServerPlayer owner;final net.minecraft.server.level.ServerLevel level;
        final Vec3 origin;final float yaw,pitch;final String epoch;
        final LbcWands.ElectroTrace trace;
        final DamageSource damage;
        Vec3 point;float currentYaw,currentPitch;int beam,sample;
        Instant(GameController game,ServerPlayer owner){
            this.game=game;this.owner=owner;level=owner.level();origin=owner.getEyePosition();yaw=owner.getYRot();pitch=owner.getXRot();epoch=game.items().epoch();
            trace=new LbcWands.ElectroTrace(owner,origin,epoch);
            damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),owner);reset();
        }
        private void reset(){point=origin;currentYaw=yaw;currentPitch=pitch;sample=0;}
        @Override public boolean step(){
            if(!game.items().currentEpoch(epoch)||!owner.isAlive()||owner.isRemoved()||owner.level()!=level||!game.canUseGameplayItems(owner))return true;
            Vec3 from=point,last=point,conductor=null;boolean terminal=false,emitted=false;
            for(int i=0;i<8;i++){
                var pos=BlockPos.containing(point);if(!level.hasChunkAt(pos)){terminal=true;break;}
                emitted=true;last=point;level.sendParticles(WHITE,true,false,point.x,point.y,point.z,1,0,0,0,2);
                level.sendParticles(PURPLE,true,false,point.x,point.y,point.z,1,0,0,0,1);
                var state=level.getBlockState(pos);
                if(state.is(CONDUCTIVE)){
                    if(conductor==null)conductor=point;
                }else if(!state.is(AIRS)){
                    terminal=true;break;
                }
                if(++sample>400){terminal=true;break;}
                boolean bend=owner.getRandom().nextFloat()<.05f;
                if(bend){currentYaw+=owner.getRandom().nextInt(7)-3;currentPitch+=owner.getRandom().nextInt(7)-3;LbcOverload.sound(owner,point,"minecraft:entity.firework_rocket.twinkle",.1f,2,0);}
                point=point.add(Vec3.directionFromRotation(currentPitch,currentYaw).scale(.25));
                if(bend)break;
            }
            // Groups have disjoint sample endpoints, so splitting work cannot duplicate bypass-cooldown hits.
            if(emitted)LbcDamage.hurtSegment(level,damage,from,last,Vec3.ZERO,.25,.5f,1,
                    entity->entity.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,entity),false);
            // Like moving Electro, one shared WILD conductor pulse per sampled segment.
            if(conductor!=null)game.items().wands().teslaConduction(trace,conductor);
            if(terminal){if(++beam>=5)return true;reset();}
            return false;
        }
    }
}
