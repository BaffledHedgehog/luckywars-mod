package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** The registered ClassicMelee clock and native player attack own every gameplay mutation. */
public final class OnePunchGloveChecks {
    private OnePunchGloveChecks(){}
    private static final Identifier BASE=Identifier.withDefaultNamespace("base_attack_damage");
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("One Punch Glove: "+why);}
    private static double damage(ItemStack stack){
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().stream()
                .filter(e->e.attribute().equals(Attributes.ATTACK_DAMAGE)&&e.modifier().id().equals(BASE))
                .mapToDouble(e->e.modifier().amount()).findFirst().orElseThrow();
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        ServerPlayer owner;ServerLevel level;Vec3 at;RegenerationBeltChecks.Snapshot saved;
        RegenerationBeltChecks.ScheduledClient client;SauvojenMeditationChecks.Attribution attribution;
        ItemStack token,expected,offhand,stored,stick;Cow cow;
        final Map<Marker,Map<BlockPos,MapManager.BlockSnapshot>> fieldCells=new IdentityHashMap<>();
        Objective hp;boolean objectiveAdded,regen,ruleCaptured,skills,levelups,configCaptured,closed;
        int stage=-1,started,when,updates;
        Fixture(GameController game){this.game=game;}
        String diagnostics(){return "stage="+stage+" elapsed="+(game.server().getTickCount()-when)+" totalTicks="+(game.server().getTickCount()-started)
                +" player="+(owner==null?"null":owner.getScoreboardName()+" dimension="+owner.level().dimension().identifier()+" hp="+owner.getHealth()+" absorption="+owner.getAbsorptionAmount()+" selected="+owner.getInventory().getSelectedSlot()+" held="+owner.getMainHandItem()+" modifiers="+owner.getMainHandItem().get(DataComponents.ATTRIBUTE_MODIFIERS))
                +" listener="+(client==null?"null":client.diagnostics());}
        ItemStack loot(){
            try{
                var source=game.server().createCommandSourceStack().withEntity(owner).withLevel(owner.level()).withPosition(owner.position()).withSuppressedOutput();
                require(game.server().getCommands().getDispatcher().execute("loot replace entity @s weapon.mainhand loot lbc:one_punch_glove",source)==1,"actual native canonical /loot command");
            }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("Glove native loot",error);}
            var result=owner.getMainHandItem();require(result.is(LbcOnePunchGloveItem.ITEM)&&result.getCount()==1,"actual canonical native loot carrier");
            return result;
        }
        void health(float value,float absorption){owner.setHealth(value);owner.setAbsorptionAmount(absorption);require(Math.abs(owner.getHealth()-value)<.0001&&Math.abs(owner.getAbsorptionAmount()-absorption)<.0001,"native HP/absorption fixture positive control");}
        void stale(){game.server().getScoreboard().getOrCreatePlayerScore(owner,hp,true).set(97);}
        ItemStack decorated(boolean raw){
            var source=loot();var result=raw?source.transmuteCopy(Items.IRON_SWORD):source;
            if(raw){result.set(DataComponents.CUSTOM_DATA,source.get(DataComponents.CUSTOM_DATA));result.set(DataComponents.ITEM_MODEL,source.get(DataComponents.ITEM_MODEL));result.set(DataComponents.LORE,source.get(DataComponents.LORE));result.set(DataComponents.TOOLTIP_STYLE,source.get(DataComponents.TOOLTIP_STYLE));}

            result.set(DataComponents.CUSTOM_NAME,Component.literal("scoped glove name"));
            result.set(DataComponents.REPAIR_COST,17);result.set(DataComponents.MAX_DAMAGE,7);result.setDamageValue(3);
            result.enchant(owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),2);
            var data=LbcFartus.data(result).copy();data.putString("kept_fixture_data","glove");result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            var entries=new ArrayList<>(result.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers());
            entries.add(new ItemAttributeModifiers.Entry(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Identifier.parse("skywars2:glove_fixture_added"),.125,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND));
            result.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));
            require(LbcOnePunchGloveItem.matches(result),"native and raw source flag admitted");return result;
        }
        void equip(ItemStack stack){token=stack;owner.setItemInHand(InteractionHand.MAIN_HAND,stack);expected=stack.copy();}
        void preserved(int amount,String why){
            require(owner.getMainHandItem()==token,why+" keeps physical held stack");
            var modifiers=token.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            var base=modifiers.stream().filter(e->e.attribute().equals(Attributes.ATTACK_DAMAGE)&&e.modifier().id().equals(BASE)).toList();
            require(base.size()==1&&base.getFirst().modifier().amount()==amount
                    &&base.getFirst().modifier().operation()==AttributeModifier.Operation.ADD_VALUE
                    &&base.getFirst().slot()==EquipmentSlotGroup.MAINHAND,why+" exact current health canonical damage");
            var prior=expected.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            require(prior.stream().filter(e->!e.modifier().id().equals(BASE)).toList()
                    .equals(modifiers.stream().filter(e->!e.modifier().id().equals(BASE)).toList()),why+" independent modifiers unchanged");
            var left=expected.copy();var right=token.copy();left.remove(DataComponents.ATTRIBUTE_MODIFIERS);right.remove(DataComponents.ATTRIBUTE_MODIFIERS);
            require(ItemStack.matches(left,right),why+" name/lore/model/tier/custom data/enchantment/damage/all other components unchanged");
            require(game.server().getScoreboard().getPlayerScoreInfo(owner,hp).value()==97,why+" ignores stale dummy hplvl score");
        }
        void begin(int next){stage=next;when=game.server().getTickCount();updates=client.updates();}
        void transfer(String id){
            var destination=game.server().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(id)));
            require(destination!=null,"native destination "+id);
            owner.teleportTo(destination,at.x,Math.min(at.y,destination.getMaxY()-20),at.z,Set.of(),0,0,true);client.acknowledge();
        }
        void field(String tag){
            var marker=new Marker(EntityTypes.MARKER,level);marker.setPos(owner.position());marker.entityTags().add(tag);
            var cells=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
            for(var pos:List.of(marker.blockPosition(),marker.blockPosition().above()))cells.put(pos,game.maps().snapshot(level,pos));
            fieldCells.put(marker,cells);require(level.addFreshEntity(marker),"owned native "+tag+" field");game.items().magic().loaded(marker);
        }
        void removeFields(){
            for(var marker:List.copyOf(fieldCells.keySet())){
                game.items().magic().unloaded(marker);marker.discard();
                fieldCells.remove(marker).forEach((pos,snapshot)->{game.maps().restoreSnapshot(level,pos,snapshot);game.items().magic().blockChanged(pos,level.getBlockState(pos));});
            }
        }
        void artifact(boolean active){
            owner.getInventory().setItem(9,active?new ItemStack(LbcMagic.MYSTICAL_ARTIFACT):ItemStack.EMPTY);
            game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();
        }
        void initialize(){
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&owner!=null&&game.canUseGameplayItems(owner)&&owner.level()==game.maps().level()
                    &&!game.items().timeStop().stopped(owner.level()),"actual playing TestAlpha in ordinary active match");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner));
            level=owner.level();at=new Vec3(owner.getX(),Math.min(240,level.getMaxY()-40),owner.getZ());
            for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(LbcOnePunchGloveItem.ITEM,Items.IRON_SWORD)){
                var stat=type.get(item);saved.stats.put(stat,owner.getStats().getValue(stat));
            }
            skills=game.config().lbcSkills;levelups=game.config().lbcLevelups;configCaptured=true;game.config().lbcSkills=false;game.config().lbcLevelups=false;
            regen=level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);ruleCaptured=true;level.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,game.server());
            owner.stopUsingItem();owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);
            owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.getAbilities().invulnerable=false;owner.getAbilities().flying=false;
            owner.noPhysics=false;owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);owner.setOnGround(true);
            for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))owner.entityTags().remove(tag);
            var profile=game.players().get(owner.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(1000);
            owner.setSprinting(false);owner.setShiftKeyDown(false);owner.getFoodData().setFoodLevel(8);owner.getFoodData().setSaturation(0);
            owner.getInventory().setSelectedSlot(0);owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);game.items().magic().inventoryChanged(owner);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);
            hp=game.server().getScoreboard().getObjective("hplvl");if(hp==null){objectiveAdded=true;hp=game.server().getScoreboard().addObjective("hplvl",ObjectiveCriteria.DUMMY,Component.literal("glove stale score"),ObjectiveCriteria.RenderType.INTEGER,false,null);}
            require(hp.getCriteria()==ObjectiveCriteria.DUMMY,"stale source score is deliberately dummy");stale();
            var fresh=loot();require(damage(fresh)==97&&fresh.getMaxDamage()==1&&fresh.getDamageValue()==0,"canonical source loot reads stale97 and carries one durability");
            require(Identifier.parse("lbc:one_punch_glove").equals(fresh.get(DataComponents.ITEM_MODEL))
                    &&Identifier.parse("lbc:tier_b").equals(fresh.get(DataComponents.TOOLTIP_STYLE))&&fresh.get(DataComponents.LORE).lines().size()==2,"source model/tier/two lore rows");
            var effective=fresh.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
            require(effective.size()==1&&effective.getFirst().attribute().equals(Attributes.ATTACK_DAMAGE)
                    &&effective.getFirst().modifier().id().equals(BASE)&&effective.getFirst().modifier().operation()==AttributeModifier.Operation.ADD_VALUE
                    &&effective.getFirst().slot()==EquipmentSlotGroup.MAINHAND,"source final set_attributes replaces complete list with canonical mainhand damage only");
            health(13.2f,2.1f);equip(decorated(false));started=game.server().getTickCount();begin(-1);
        }
        boolean step(){
            client.acknowledge();int elapsed=game.server().getTickCount()-when;
            require(game.server().getTickCount()-started<180,"bounded real native scheduler");
            if(stage==-1){if(client.updates()<=updates)return false;health(13.2f,2.1f);stale();begin(0);return false;}
            require(client.updates()-updates==elapsed,"one native scheduled listener update per measured server tick");
            if(elapsed<3)return false;
            switch(stage){
                case 0->{preserved(16,"native HP13.2+absorption2.1 rounds up16");health(10.2f,2.1f);begin(1);}
                case 1->{preserved(13,"live health change");health(10.2f,5.1f);begin(2);}
                case 2->{preserved(16,"live absorption change");health(6.2f,1.1f);equip(decorated(true));begin(3);}
                case 3->{preserved(8,"raw iron sword source flag");offhand=token.copy();owner.setItemInHand(InteractionHand.OFF_HAND,offhand);stored=token.copy();owner.getInventory().setItem(0,stored);owner.getInventory().setSelectedSlot(4);stick=new ItemStack(Items.STICK);owner.setItemInHand(InteractionHand.MAIN_HAND,stick);expected=offhand.copy();health(21.2f,0);begin(4);}
                case 4->{require(owner.getMainHandItem()==stick&&stick.is(Items.STICK)&&ItemStack.matches(offhand,expected)&&ItemStack.matches(stored,expected),"offhand/inventory glove and replacement selected stack remain untouched");owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);health(8.2f,0);equip(decorated(false));begin(5);}
                case 5->{preserved(9,"current selected slot4 receives refresh");owner.entityTags().add("time_stopped");health(31.2f,0);begin(6);}
                case 6->{preserved(9,"per-player Time Stop pauses refresh");owner.entityTags().remove("time_stopped");begin(7);}
                case 7->{preserved(32,"refresh resumes after Time Stop");owner.entityTags().add("spectator");health(41.2f,0);begin(8);}
                case 8->{preserved(32,"spectator role excludes refresh");owner.entityTags().remove("spectator");owner.entityTags().add("strict_map_object");begin(9);}
                case 9->{preserved(32,"technical map role excludes refresh");owner.entityTags().remove("strict_map_object");begin(10);}
                case 10->{preserved(42,"ordinary participant resumes refresh");health(51.2f,0);transfer("minecraft:imprinted");begin(11);}
                case 11->{preserved(42,"Imprinted excludes refresh");transfer("minecraft:nexus");begin(12);}
                case 12->{preserved(42,"Nexus excludes refresh");owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);client.acknowledge();begin(13);}
                case 13->{preserved(52,"ordinary match dimension resumes refresh");field("stopper_magic");require(game.items().magic().suppressed(owner),"real MAG field suppression positive control");health(61.2f,0);begin(15);}
                case 15->{preserved(62,"MAG field permits physical health refresh");require(game.items().magic().suppressed(owner),"MAG control remains active across real ticks");removeFields();field("stopper");require(game.items().magic().mechanismSuppressed(owner),"real TECH suppression positive control");health(71.2f,0);begin(16);}
                case 16->{preserved(72,"TECH field permits physical health refresh");require(game.items().magic().mechanismSuppressed(owner),"TECH control remains active after native marker migration");removeFields();artifact(true);require(game.items().magic().artifactActive(owner)&&game.items().magic().suppressed(owner),"strong artifact active suppression positive control");health(81.2f,0);begin(17);}
                case 17->{preserved(82,"strong artifact permits physical health refresh");require(game.items().magic().artifactActive(owner)&&game.items().magic().suppressed(owner),"strong artifact remains active across real ticks");artifact(false);health(13.2f,0);owner.getInventory().clearContent();equip(loot());begin(18);}
                case 18->{
                    preserved(14,"fresh one-durability native weapon ready for attack");
                    cow=new Cow(EntityTypes.COW,level);cow.setPos(owner.position().add(0,0,2));cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(level.addFreshEntity(cow),"actual owned stationary native melee target");
                    var broken=Stats.ITEM_BROKEN.get(LbcOnePunchGloveItem.ITEM);int before=owner.getStats().getValue(broken);int dealt=owner.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT));
                    owner.attack(cow);
                    require(cow.getHealth()<1000&&cow.getLastHurtByMob()==owner,"actual native mainhand attack damages and credits its player");
                    require(owner.getMainHandItem().isEmpty()&&owner.getStats().getValue(broken)==before+1,"native one-durability attack breaks glove and increments real ITEM_BROKEN");
                    require(owner.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT))>dealt,"native attack records actual dealt damage stat");return true;
                }
                default->throw new IllegalStateException("glove fixture stage "+stage);
            }return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->{if(cow!=null){game.items().indirectKills().clear(cow);cow.discard();}},
                    this::removeFields,
                    ()->{if(owner!=null){owner.entityTags().remove("time_stopped");owner.entityTags().remove("spectator");owner.entityTags().remove("strict_map_object");}},
                    ()->{if(owner!=null&&level!=null&&owner.level()!=level){owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);if(client!=null)client.acknowledge();}},
                    ()->{if(ruleCaptured)level.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,regen,game.server());},
                    ()->{if(configCaptured){game.config().lbcSkills=skills;game.config().lbcLevelups=levelups;}if(saved!=null)saved.restore(game);},
                    ()->{if(saved!=null){saved.restoreAttributes();saved.restoreUse();game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();}},
                    ()->{if(attribution!=null)attribution.close();},
                    ()->{if(client!=null)client.close();},
                    ()->{if(objectiveAdded)game.server().getScoreboard().removeObjective(hp);}
            ))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Glove scoped restore",failure);
        }
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(game==null||running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS one-punch-glove (canonical native/raw loot; registered actual scheduled health+absorption ceil refresh independent of stale dummy score; selected mainhand only; unrelated components/modifiers preserved; roles/TimeStop/Nexus/Imprinted; MAG/TECH/strong artifact unjammed; native player attack one-durability break and real stats; scoped restoration)");}}catch(Throwable error){var diagnostics=running.diagnostics();try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL one-punch-glove "+diagnostics,error);}}
}
