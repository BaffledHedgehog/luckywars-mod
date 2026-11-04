package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** РќР°СЃС‚РѕСЏС‰РёРµ С‚РёРєРё РёСЃС‚РѕС‡РЅРёРєР°, РїРѕРґР°РІР»РµРЅРёРµ, WILD-РєРѕРЅС‚Р°РєС‚, РЅР°РіСЂР°РґС‹ Рё РІР»Р°РґРµР»СЊС†С‹ РїРµСЂРµРєСЂС‹С‚РёСЏ. */
public final class EternalWealthChecks {
    private static Fixture running;
    private EternalWealthChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Eternal Wealth: "+message);}
    static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable failure){running.close();running=null;throw failure;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS eternal-wealth (native Polymer/frame/actual intake; protected inventory, MAG/TECH, 20tick diamond, 180tick curse, source mask/fluid fix, raw mushrooms, WILD, target protection, owner-only overlap cleanup, per-player rewards, cold actor adoption and typed checkpoint)");}
        catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL eternal-wealth",failure);}}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcEternalWealth wealth;
        int phase,age,wait,prestige;Marker jammer,diamond;Cow hurt,raw;float ownerHp,hurtHp,rawHp;
        BlockPos curseCenter;boolean closed;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);wealth=game.items().eternalWealth();require(wealth.count()==0,"no preexisting wealth actors");}
        void init(){
            base.init();prestige=game.items().mana().score(base.owner,"greed_prestige").get();
            game.config().lbcSkills=true;game.players().get(base.owner.getUUID()).lbcSkill=2;
            for(var p:List.of(base.owner,base.other)){p.getAbilities().invulnerable=true;p.entityTags().remove("greed_cursed");p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);}
            int mask=0;for(int x=-5;x<=5;x++)for(int y=-5;y<=5;y++)for(int z=-5;z<=5;z++)if(LbcEternalWealth.inMask(x,y,z))mask++;
            require(mask==637,"exact source union, without corner expansion");
            for(var item:List.of(LbcEternalWealthItems.DROP,LbcEternalWealthItems.IMBA_DROP,LbcEternalWealthItems.STONE))
                require(item instanceof eu.pb4.polymer.core.api.item.PolymerItem,"one native Polymer identity per variant");
            var projection=((eu.pb4.polymer.core.api.block.PolymerBlock)LbcEternalWealthItems.CURSED_BLOCK).getPolymerBlockState(LbcEternalWealthItems.CURSED_BLOCK.defaultBlockState(),null);
            require(projection.is(Blocks.NOTE_BLOCK),"native cursed block uses the common full-cube palette without aliasing raw map mushrooms");
            require(projection.getCollisionShape(base.owner.level(),base.owner.blockPosition()).bounds().equals(new AABB(0,0,0,1,1,1)),"cursed client projection keeps full-cube collision");
            var token=new ItemEntity(base.owner.level(),base.at.x,base.at.y,base.at.z,new ItemStack(LbcEternalWealthItems.DROP));
            game.items().markCurrentItemEpoch(token);base.owner.level().addFreshEntity(token);
            require(game.items().specialDrops().spawn(token),"actual source dispatcher activates the airborne normal drop");
            var frames=base.owner.level().getEntitiesOfClass(ItemFrame.class,new AABB(base.at,base.at).inflate(3),f->LbcEternalWealthItems.isStone(f.getItem()));
            require(frames.size()==1&&frames.getFirst().isInvisible(),"source invisible glow frame contains actual Greedstone");
            frames.getFirst().setItem(ItemStack.EMPTY);frames.getFirst().discard();
            base.owner.getInventory().setItem(9,new ItemStack(LbcEternalWealthItems.STONE));game.items().mana().inventoryChanged(base.owner);
            curseCenter=base.owner.blockPosition();
            LbcDropBlocks.setBlock(base.owner.level(),curseCenter.offset(0,4,0),Blocks.WATER.defaultBlockState(),true);
            LbcDropBlocks.setBlock(base.owner.level(),curseCenter.offset(0,-4,0),Blocks.LAVA.defaultBlockState(),true);
            LbcDropBlocks.setBlock(base.owner.level(),curseCenter.offset(5,-1,0),Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(),true);
            SkyWars2.LOGGER.info("[check] START eternal-wealth");
        }
        void jam(String tag,Vec3 at){unjam();jammer=new Marker(EntityTypes.MARKER,base.owner.level());jammer.setPos(at);jammer.entityTags().add(tag);base.owner.level().addFreshEntity(jammer);game.items().magic().loaded(jammer);}
        void unjam(){if(jammer!=null){game.items().magic().unloaded(jammer);jammer.discard();jammer=null;}}
        Marker actor(String kind){return wealth.snapshot().stream().filter(m->m.entityTags().contains(LbcEternalWealth.TAG+".kind."+kind)).findFirst().orElse(null);}
        Marker adopt(String kind,ServerPlayer owner,Vec3 at,int ticks){
            var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);marker.entityTags().addAll(List.of(LbcEternalWealth.TAG,
                    LbcEternalWealth.TAG+".kind."+kind,LbcEternalWealth.TAG+".owner."+owner.getUUID(),LbcEternalWealth.TAG+".order.1",LbcEternalWealth.TAG+".age."+ticks));
            game.items().markCurrentItemEpoch(marker);owner.level().addFreshEntity(marker);return marker;
        }
        int rewards(String flag){return base.owner.level().getEntitiesOfClass(ItemEntity.class,new AABB(diamond.position(),diamond.position()).inflate(8),e->LbcFartus.data(e.getItem()).getIntOr(flag,0)==1)
                .stream().peek(e->require(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getNamespace().equals("skywars2"),"reward keeps native Polymer identity")).mapToInt(e->e.getItem().getCount()).sum();}
        boolean step(){
            require(++age<420,"bounded registered source ticks, phase="+phase);wait++;
            var p=base.owner;var level=p.level();
            if(phase==0&&wait>=3){
                require(p.getInventory().getItem(9).getCount()==1&&!p.entityTags().contains("greed_cursed"),"protected inventory blocks carried intake");
                p.getInventory().setItem(9,ItemStack.EMPTY);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(LbcEternalWealthItems.STONE));game.items().mana().inventoryChanged(p);phase=1;wait=0;
            }else if(phase==1&&wait>=3){
                require(!p.getOffhandItem().isEmpty(),"protected source requires main hand, not offhand");p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(LbcEternalWealthItems.STONE));game.items().mana().inventoryChanged(p);
                jam("stopper_magic",p.position());require(game.items().magic().suppressed(p),"native MAG positive control");phase=2;wait=0;
            }else if(phase==2&&wait>=3){
                require(!p.getMainHandItem().isEmpty()&&!p.entityTags().contains("greed_cursed"),"MAG prevents actual held intake");
                jam("stopper",p.position());require(game.items().magic().mechanismSuppressed(p)&&!game.items().magic().suppressed(p),"TECH only positive control");phase=3;wait=0;
            }else if(phase==3){
                var pending=actor("PENDING");if(pending==null)return false;
                require(p.getMainHandItem().isEmpty()&&p.entityTags().contains("greed_cursed")&&game.items().mana().score(p,"greed_prestige").get()==prestige+1,"one source debit and actual prestige/curse under TECH");
                require(pending.getX()>=-130&&pending.getX()<=130&&pending.getY()>=16&&pending.getY()<=300&&pending.getZ()>=-130&&pending.getZ()<=130,"source absolute random spawn coordinates");
                pending.setPos(net.minecraft.world.phys.Vec3.atLowerCornerOf(base.center.offset(0,3,10)));phase=4;wait=0;
            }else if(phase==4){
                diamond=actor("DIAMOND");if(diamond==null)return false;
                require(wait>=16,"diamond placement retains source twenty-tick delay");
                require(diamond.blockPosition().getY()==base.center.getY()+4,"actual template marker offset retained");phase=5;wait=0;
            }else if(phase==5){
                if(wealth.fields()==0)return false;
                require(wait>=150,"curse does not skip its nine-second source age");
                require(level.getBlockState(curseCenter.offset(0,4,0)).is(Blocks.GOLD_BLOCK)&&level.getBlockState(curseCenter.offset(0,-4,0)).is(Blocks.GOLD_BLOCK),"both formerly omitted fluid layers convert");
                require(level.getBlockState(curseCenter.below()).is(LbcEternalWealthItems.CURSED_BLOCK),"solid source cells use own server block");
                require(eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment.get(level,curseCenter.below())==null,"cursed native block model has no virtual holder");
                unjam();p.entityTags().remove("greed_cursed");
                for(var marker:wealth.snapshot())if(marker.entityTags().contains(LbcEternalWealth.TAG+".kind.SEED"))marker.discard();
                hurt=base.cow(Vec3.atBottomCenterOf(curseCenter.offset(4,0,0)));raw=base.cow(Vec3.atBottomCenterOf(curseCenter.offset(5,0,0)));
                game.server().getScoreboard().addPlayerToTeam(p.getScoreboardName(),base.team);game.server().getScoreboard().addPlayerToTeam(hurt.getScoreboardName(),base.team);
                p.getAbilities().invulnerable=false;p.setInvulnerableTime(0);p.damageCooldownTime=0;ownerHp=p.getHealth();hurtHp=hurt.getHealth();rawHp=raw.getHealth();phase=6;wait=0;
            }else if(phase==6&&wait>=6){
                require(p.getHealth()<ownerHp&&hurt.getHealth()<hurtHp,"native WILD hurts owner and allied mob");require(raw.getHealth()==rawHp,"raw mushroom outside conversion is harmless");
                var away=hurt.position().subtract(Vec3.atCenterOf(curseCenter)).normalize();
                jam("stopper_magic",hurt.position().add(away.scale(19)));require(game.items().magic().suppressedAt(level,hurt.position())&&!game.items().magic().suppressedAt(level,Vec3.atCenterOf(curseCenter)),"target-only MAG boundary positive control");
                hurtHp=hurt.getHealth();phase=7;wait=0;
            }else if(phase==7&&wait>=8){
                require(hurt.getHealth()==hurtHp&&wealth.fields()>0,"MAG protects target while source stays active outside");unjam();
                adopt("SEED",base.other,Vec3.atCenterOf(curseCenter.offset(6,0,0)),179);phase=8;wait=0;
            }else if(phase==8){
                if(wealth.fields()!=2)return false;
                var image=wealth.checkpoint();wealth.prepareFrozen();var plan=image.prepare(level::getEntity);require(plan.pending().isEmpty(),"native marker bodies resolve for typed checkpoint");plan.install().run();wealth.publishFrozen();
                require(wealth.fields()==2&&wealth.diamonds()==1,"nonempty typed field/diamond state resumes without replaying intake");
                for(var person:List.of(p,base.other)){person.getAbilities().invulnerable=true;person.entityTags().add("greed_cursed");}
                base.move(p,diamond.position());base.move(base.other,diamond.position().add(6,0,0));phase=9;wait=0;
            }else if(phase==9){
                if(!diamond.isRemoved())return false;
                require(!p.entityTags().contains("greed_cursed")&&!base.other.entityTags().contains("greed_cursed"),"all cursed players within ten lose the curse");
                require(rewards("greed_greed")==2,"one native Divider for each cleansed player");require(wealth.fields()==1,"only the directly touching owner's fields are removed");
                require(level.getBlockState(curseCenter.offset(-3,-1,0)).is(Blocks.STAINED_GLASS.pick(DyeColor.YELLOW)),"own unshared cells neutralize");
                require(level.getBlockState(curseCenter.offset(3,-1,0)).is(LbcEternalWealthItems.CURSED_BLOCK),"foreign overlapping field remains dangerous");
                var spot=base.at.add(0,2,4);diamond=adopt("DIAMOND",p,spot,0);base.move(p,spot);base.move(base.other,spot.add(1,0,0));phase=10;wait=0;
            }else if(phase==10){
                if(!diamond.isRemoved())return false;
                require(rewards("feather_greed")==2,"one native poor reward for each uncursed touching player");return true;
            }
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;unjam();wealth.stop();
            for(var p:List.of(base.owner,base.other)){p.getAbilities().invulnerable=false;p.entityTags().remove("greed_cursed");}
            game.items().mana().score(base.owner,"greed_prestige").set(prestige);base.close();}
    }
}
