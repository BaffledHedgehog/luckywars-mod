package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.Vec3;

/** Real cloud applications, native imbue recipe, actual double-bow publication and scheduled hits. */
public final class PotionCarrierChecks {
    private PotionCarrierChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Potion carriers: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final Set<UUID> bombs=new HashSet<>();final List<Cow> targets=new ArrayList<>();
        AreaEffectCloud cloud;Cow target;ItemStack potion;int began,when,phase;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        Cow target(){var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(f.owner.getEyePosition().add(0,-.6,5));cow.setNoAi(true);cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            require(f.level.addFreshEntity(cow),"native impact target");f.owned.add(cow.getUUID());targets.add(cow);return cow;}
        void begin(){f.prepareParticipants();require(game.items().babylonEffects().pendingCount()==0,"exclusive status fixture");
            f.move(f.owner,f.home.add(0,60,0));f.move(f.other,f.home.add(25,60,0));f.owner.setYRot(0);f.owner.setXRot(0);
            potion=f.loot("lbc:unstable_reactive");target=target();
            cloud=new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,f.level);cloud.setPos(target.position());cloud.setOwner(f.owner);
            cloud.setPotionContents(potion.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY));cloud.setPotionDurationScale(.25f);
            cloud.setWaitTime(0);cloud.setDuration(30);cloud.setRadius(1.5f);cloud.setRadiusOnUse(-.5f);
            require(f.level.addFreshEntity(cloud),"native unstable-reagent cloud");f.owned.add(cloud.getUUID());began=when=game.server().getTickCount();
        }
        void doubleBow(){cloud.discard();game.items().babylonEffects().clear(target);target.discard();
            for(var id:List.copyOf(bombs)){var entity=f.level.getEntity(id);if(entity!=null)entity.discard();}bombs.clear();
            target=target();var slots=new ArrayList<ItemStack>();for(int i=0;i<9;i++)slots.add(i==4?potion.copy():new ItemStack(Items.ARROW));
            var input=CraftingInput.of(3,3,slots);var recipe=f.level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,f.level).orElseThrow();
            var ammo=recipe.value().assemble(input);require(ammo.is(Items.TIPPED_ARROW)&&ammo.getCount()==8,"native crafted eight-arrow payload");
            var expected=ammo.get(DataComponents.POTION_CONTENTS);ammo.setCount(3);
            var weapon=f.loot("lbc:item_defs/lbc/doublebow__ce108b3ec6f0");
            f.owner.getInventory().clearContent();f.owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);f.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);
            require(weapon.getItem() instanceof BowItem,"canonical double-bow item");
            var before=new HashSet<UUID>();for(var e:f.level.getAllEntities())before.add(e.getUUID());
            require(f.owner.gameMode.useItem(f.owner,f.level,weapon,InteractionHand.MAIN_HAND).consumesAction()
                    &&weapon.getItem().releaseUsing(weapon,f.level,f.owner,71980),"real native double-bow charge/release");f.owner.stopUsingItem();
            int count=0;for(var e:f.level.getAllEntities())if(e instanceof Arrow arrow&&!e.isRemoved()&&!before.contains(e.getUUID())){
                count++;f.owned.add(e.getUUID());require(expected.equals(arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS))
                        &&arrow.getOwner()==f.owner,"both native double-bow arrows retain exact crafted effects and shooter");
            }
            require(count==2&&ammo.getCount()==2,"two native arrows spend exactly one selected ammunition");when=game.server().getTickCount();phase=1;
        }
        void chemicalVisuals(){
            var at=new Vec3(f.owner.getX(),f.level.getMaxY()+40,f.owner.getZ());f.move(f.owner,at);
            for(var item:List.of(LbcItems.TERMIT,LbcItems.SULFUR,LbcItems.PHOSPHIN,LbcItems.GREMUCH)){
                var body=new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,f.level);body.setPos(at);body.setOwner(f.owner);
                body.setPotionContents(new ItemStack(item).get(DataComponents.POTION_CONTENTS));
                require(f.level.addFreshEntity(body),"native chemical visual witness");f.owned.add(body.getUUID());
                require(((eu.pb4.polymer.core.api.entity.PolymerEntity)body).getPolymerEntityType(f.owner.connection.getPacketContext())==EntityTypes.MARKER,
                        "chemical bookkeeping cloud stays invisible; only authored particles render");
                f.client.packets().clear();game.items().clouds().tick();
                var particles=f.client.packets().stream().filter(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::isInstance)
                        .map(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class::cast).toList();
                if(item==LbcItems.TERMIT)require(particles.stream().anyMatch(p->p.particle()==net.minecraft.core.particles.ParticleTypes.FLAME
                        &&p.count()==5&&p.x()==at.x&&p.y()==at.y+.5&&p.z()==at.z&&p.xDist()==.2f&&p.yDist()==.5f&&p.zDist()==.2f
                        &&p.xMaxSpeed()==.5f&&p.overrideLimiter()),"thermite retains legacy five forced flame particles and exact position/spread/speed");
                if(item==LbcItems.SULFUR)require(particles.stream().anyMatch(p->p.particle()==net.minecraft.core.particles.ParticleTypes.SNEEZE
                        &&p.count()==10&&p.x()==at.x&&p.y()==at.y&&p.z()==at.z&&p.xDist()==4&&p.yDist()==4&&p.zDist()==4
                        &&p.xMaxSpeed()==0&&!p.overrideLimiter()),"sulfur retains legacy ten normal sneeze particles and exact position/spread/speed");
                if(item==LbcItems.PHOSPHIN)require(particles.isEmpty(),"phosphine retains source absence of extra custom particles");
                body.discard();
            }
            var ordinary=new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,f.level);
            require(((eu.pb4.polymer.core.api.entity.PolymerEntity)ordinary).getPolymerEntityType(f.owner.connection.getPacketContext())==EntityTypes.AREA_EFFECT_CLOUD,
                    "ordinary vanilla cloud presentation remains native");
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<80,"bounded native status contacts");
            for(var e:f.level.getAllEntities())if(e instanceof PrimedTnt tnt&&game.items().babylonEffects().managedSecondary(tnt)&&!bombs.contains(e.getUUID())){
                require(tnt.getOwner()==f.owner&&game.items().currentEpoch(tnt),"native status TNT retains true potion/arrow owner and epoch");bombs.add(tnt.getUUID());f.owned.add(tnt.getUUID());
            }
            if(bombs.isEmpty()){require(game.server().getTickCount()-when<20,"actual native carrier produces an explosion status");return false;}
            require(!target.hasEffect(MobEffects.RAID_OMEN),"encoded marker is consumed before its native effect tick");
            if(phase==0){doubleBow();return false;}
            target.setInvulnerableTime(0);target.damageCooldownTime=0;float before=target.getHealth();
            target.addEffect(ItemEffectLevels.create(MobEffects.RAID_OMEN,2,234),f.owner);
            require(target.getHealth()==before-4&&!target.hasEffect(MobEffects.RAID_OMEN),"termite carrier retains authored four lava damage and consumes its marker");
            target.addEffect(ItemEffectLevels.create(MobEffects.RAID_OMEN,2,235),f.owner);
            require(target.hasEffect(MobEffects.POISON)&&target.getEffect(MobEffects.POISON).getAmplifier()==4
                    &&target.getEffect(MobEffects.POISON).getDuration()==200&&!target.hasEffect(MobEffects.RAID_OMEN),"sulfur carrier retains authored Poison V ten seconds without marker leak");
            chemicalVisuals();
            return true;
        }
        public void close(){for(var target:targets)game.items().babylonEffects().clear(target);f.close();}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.begin();}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS potion-carriers (actual cloud, native imbue recipe, double-bow payload/debit, scheduled impacts, owned TNT and no native marker leak)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL potion-carriers",error);}}
}
