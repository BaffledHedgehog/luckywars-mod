package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual accepted-hit Devil ladder and later native world effect updates, without manual player/world ticks. */
public final class DevilSwordChecks {
    private DevilSwordChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Devil sword: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-4,why+" actual="+actual+" expected="+expected);}
    private static java.lang.reflect.Field field(Object actor,String name)throws NoSuchFieldException{for(Class<?> type=actor.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static MobEffectInstance effect(LivingEntity target,Holder<MobEffect> kind,int duration,int amplifier){var value=target.getEffect(kind);
        require(value!=null&&value.getAmplifier()==amplifier&&value.getDuration()==duration&&value.isVisible()&&value.showIcon()&&!value.isAmbient(),"exact source effect "+kind+" expected="+duration+"/"+amplifier+" actual="+value);return value;}
    private static MobEffectInstance hidden(MobEffectInstance effect){return effect==null?null:((EffectLayers.Access)effect).skywars2$layerHidden();}
    private static void layer(MobEffectInstance value,int amp,int duration){require(value!=null&&value.getAmplifier()==amp&&value.getDuration()==duration,"independent native layer expected="+amp+"/"+duration+" actual="+value);}
    private static void text(Component text,String key,Integer color){require(text!=null&&text.getContents() instanceof TranslatableContents t&&t.getKey().equals(key)&&!text.getStyle().isItalic()
            &&(color==null?text.getStyle().getColor()==null:text.getStyle().getColor()!=null&&text.getStyle().getColor().getValue()==color),"source text/style "+key);}
    /** The real network scheduler owns this connection's native super.tick; the fixture never invokes it. */
    private static final class ScheduledConnection extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        ScheduledConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnecting(){return false;}
        @Override public boolean isConnected(){return true;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){}
    }
    private static final class ScheduledClient implements AutoCloseable {
        final GameController game;final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous,listener;
        final ScheduledConnection connection=new ScheduledConnection();final java.lang.reflect.Field updates;
        ScheduledClient(GameController game,ServerPlayer player){this.game=game;this.player=player;previous=player.connection;
            try{
                listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),connection,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
                // A fresh memory connection has no protocol channel pipeline; bind only its own native dispatch field.
                field(connection,"packetListener").set(connection,listener);updates=field(listener,"tickCount");
                listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                listener.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
                var teleport=connection.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
                listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
                require(listener.hasClientLoaded()&&field(listener,"awaitingPositionFromClient").get(listener)==null,"fresh native listener loaded and real teleport acknowledged");
                game.server().getConnection().getConnections().add(connection);
            }catch(ReflectiveOperationException|RuntimeException|Error error){game.server().getConnection().getConnections().remove(connection);player.connection=previous;throw new IllegalStateException("scoped native connection scheduler",error);}
        }
        int updates(){try{return updates.getInt(listener);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        @Override public void close(){try{game.server().getConnection().getConnections().remove(connection);}finally{player.connection=previous;}}
    }
    private static final class Snapshot {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final net.minecraft.nbt.CompoundTag food;
        final boolean ground,sprint,shift,swimming;final Vec3 knownMovement;final net.minecraft.world.entity.Pose pose;final Map<java.lang.reflect.Field,Object> tickers=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>();
        Snapshot(GameController game,ServerPlayer player){this.player=player;state=new GameChecks.WandThirdPlayerState(player,game.items().mana());ground=player.onGround();sprint=player.isSprinting();shift=player.isShiftKeyDown();swimming=player.isSwimming();knownMovement=player.getKnownMovement();pose=player.getPose();
            try{for(var name:List.of("attackStrengthTicker","itemSwapTicker")){var f=field(player,name);tickers.put(f,f.get(player));}}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess());player.getFoodData().addAdditionalSaveData(output);food=output.buildResult();
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
            for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN)){var stat=type.get(Items.WOODEN_SWORD);stats.put(stat,player.getStats().getValue(stat));}
            var killed=Stats.ENTITY_KILLED.get(EntityTypes.COW);stats.put(killed,player.getStats().getValue(killed));
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            try{var p=game.players().get(player.getUUID());for(var f:p.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(p);profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> set?new HashSet<>(set):v);}}
            catch(IllegalAccessException error){throw new IllegalStateException(error);}
        }
        void restore(GameController game){stats.forEach((s,v)->player.getStats().setValue(player,s,v));try{for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});state.restore(game);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setSwimming(swimming);player.setKnownMovement(knownMovement);player.setPose(pose);try{for(var entry:tickers.entrySet())entry.getKey().set(player,entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}player.getFoodData().readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess(),food));}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,other;final Snapshot ownerSnapshot,otherSnapshot;
        final SauvojenMeditationChecks.Attribution attribution;final Map<java.lang.reflect.Field,Object> combat=new LinkedHashMap<>();final List<Cow> cows=new ArrayList<>();
        final net.minecraft.world.scores.PlayerTeam team,ownerTeam,otherTeam;final boolean drops,regen;final Vec3 at;final String epoch;final int food;
        final net.minecraft.advancements.AdvancementHolder task;int phase,started,lastTick=-1,lastOwnerUpdate,lastOtherUpdate;boolean firstCurseTick;
        ScheduledClient ownerClient,otherClient;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null,"native second participant");
            ownerSnapshot=new Snapshot(game,owner);otherSnapshot=new Snapshot(game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));
            try{for(var key:List.of("attackStrengthTicker","itemSwapTicker")){var f=field(owner,key);combat.put(f,f.get(owner));}}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            drops=owner.level().getGameRules().get(GameRules.MOB_DROPS);regen=owner.level().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);food=base.mana.score(owner,"food_count").get();epoch=game.items().epoch();
            var board=game.server().getScoreboard();ownerTeam=owner.getTeam();otherTeam=other.getTeam();require(board.getPlayerTeam("sw2_devil_check")==null,"exclusive native fixture team");team=board.addPlayerTeam("sw2_devil_check");team.setAllowFriendlyFire(false);board.addPlayerToTeam(owner.getScoreboardName(),team);if(other.getTeam()!=null)board.removePlayerFromTeam(other.getScoreboardName(),other.getTeam());
            base.initialize();at=owner.position();for(var player:List.of(owner,other)){var p=game.players().get(player.getUUID());p.lbcSkill=0;p.lbcTrinket=0;p.lbcChallenge=0;Arrays.fill(p.trinkets,0);
                player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getAbilities().invulnerable=false;player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);player.setAbsorptionAmount(0);player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;player.setSprinting(false);player.setSwimming(false);player.setInvulnerableTime(0);player.damageCooldownTime=0;
                for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))player.setItemSlot(slot,ItemStack.EMPTY);}
            other.teleportTo(other.level(),at.x+4,at.y,at.z,Set.of(),180,0,true);owner.level().getGameRules().set(GameRules.MOB_DROPS,false,game.server());owner.level().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,game.server());base.mana.set(owner,-50);
            task=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/swords/devil"));require(task!=null,"source impossible-trigger Devil task loaded");
        }
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"actual singleton native leaf "+leaf);return list.getFirst();}
        ItemStack held(String leaf){owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var stack=loot(leaf);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);return stack;}
        Cow cow(){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(at.add(0,0,2));c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);require(owner.level().addFreshEntity(c),"real native target");cows.add(c);require(owner.level().getEntities(owner,c.getBoundingBox(),e->e==c).contains(c),"resident native collision target spatially visible");return c;}
        DamageSource source(ServerPlayer player,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> kind){return new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(kind),player);}
        boolean hit(LivingEntity target,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> kind){target.setInvulnerableTime(0);target.damageCooldownTime=0;return target.hurtServer(owner.level(),source(owner,kind),1);}
        void clean(){owner.removeAllEffects();other.removeAllEffects();base.removeField();owner.entityTags().remove("time_stopped");owner.entityTags().remove("nomagic_active");for(var c:cows){game.items().indirectKills().clear(c);c.discard();}cows.clear();owner.setHealth(1000);other.setHealth(1000);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;other.setInvulnerableTime(0);other.damageCooldownTime=0;}
        void revokeTask(){for(var key:task.value().criteria().keySet())owner.getAdvancements().revoke(task,key);}
        com.google.gson.JsonObject json(String path){try(var stream=DevilSwordChecks.class.getClassLoader().getResourceAsStream("data/lbc/"+path+".json")){require(stream!=null,"compiled source resource "+path);return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        void presentation(){
            for(var leaf:List.of("devil_sword","legacy_devil_sword")){boolean legacy=leaf.startsWith("legacy");var stack=loot(leaf);var vanilla=new ItemStack(Items.WOODEN_SWORD);require(stack.is(Items.WOODEN_SWORD)&&stack.getMaxDamage()==vanilla.getMaxDamage(),"source vanilla wooden sword/default durability");
                var data=LbcFartus.data(stack);require(data.getIntOr("heal",0)==1&&data.getIntOr("transformable",0)==(legacy?0:1)&&data.getIntOr("sword",0)==(legacy?0:1),"exact distinct current/legacy source flags");
                require(Identifier.parse("lbc:devilsword").equals(stack.get(DataComponents.ITEM_MODEL))&&(legacy?stack.get(DataComponents.TOOLTIP_STYLE)==null:Identifier.parse("lbc:tier_f").equals(stack.get(DataComponents.TOOLTIP_STYLE))),"source model/tier presence");text(stack.get(DataComponents.CUSTOM_NAME),"devil_sword",0xaa0000);
                var lore=stack.get(DataComponents.LORE).lines();require(lore.size()==(legacy?3:5),"source exact lore count");int from=legacy?0:1;if(!legacy){text(lore.getFirst(),"type",0x55ffff);require(lore.getFirst().getSiblings().size()==1,"single sword type sibling");text(lore.getFirst().getSiblings().getFirst(),"sword",0x55ffff);}
                for(int i=from;i<lore.size();i++)text(lore.get(i),(legacy?"devil_sword_old":"devil_sword")+".lore."+(i-from+1),0xff5555);
                var mods=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(mods.size()==1&&mods.getFirst().attribute().equals(Attributes.ATTACK_DAMAGE)&&mods.getFirst().modifier().amount()==0&&mods.getFirst().modifier().id().equals(Identifier.parse(legacy?"minecraft:edkoedkoedkodaerisaiwisjfijf":"minecraft:base_attack_damage")),"source damage0 rewrite with distinct legacy ID and absent invented speed modifier");
            }
            var pool=json("loot_table/transformables_sword").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");com.google.gson.JsonObject original=null;for(var entry:pool)if(entry.toString().contains("heal:1")){require(original==null,"single current source pool Devil entry");original=entry.getAsJsonObject().deepCopy();}require(original!=null,"original native transformation/reforge pool includes heal item");
            var leaf=json("loot_table/ported/devil_sword").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().deepCopy();original.remove("weight");original.remove("quality");leaf.remove("weight");leaf.remove("quality");require(original.equals(leaf),"actual compiled original acquisition payload equals current native leaf");
            var display=task.value().display().orElseThrow();var icon=display.icon().create();require(display.type()==net.minecraft.advancements.AdvancementType.TASK&&icon.is(Items.WOODEN_SWORD)&&Identifier.parse("lbc:devilsword").equals(icon.get(DataComponents.ITEM_MODEL))&&task.value().parent().orElseThrow().equals(Identifier.parse("lbc:true_advancements/swords/slimeball")),"source native Devil task icon/model/parent");text(display.title(),"lbc.adv.sword.devil",null);text(display.description(),"lbc.adv.sword.devil.lore",null);
            require(json("advancement/true_advancements/swords/devil").getAsJsonObject("criteria").getAsJsonObject("requirement").get("trigger").getAsString().equals("minecraft:impossible"),"source task requires actual helper6 explicit grant");
        }
        void ladder(){
            for(var leaf:List.of("devil_sword","legacy_devil_sword"))for(Integer initial:Arrays.asList(null,-2,0,1,2,3,4,5,6,Integer.MAX_VALUE)){
                clean();var stack=held(leaf);revokeTask();if(initial!=null)owner.addEffect(ItemEffectLevels.create(MobEffects.STRENGTH,80,initial),owner);var target=cow();int damage=stack.getDamageValue();float health=owner.getHealth();require(hit(target,DamageTypes.MAGIC),"real external attributed accepted nonarrow branch "+initial);
                near(owner.getHealth(),health,"source harmful effects remain scheduled rather than immediate");require(stack.getDamageValue()==damage&&base.mana.get(owner)==-50&&base.mana.score(owner,"food_count").get()==food,"passive changes neither durability/mana/food counter");require(target.getActiveEffects().isEmpty()&&other.getActiveEffects().isEmpty(),"all helper effects target self only");
                if(initial==null){effect(owner,MobEffects.STRENGTH,200,0);require(owner.getActiveEffects().size()==1,"no-strength reaches only final helper1, not earlier generic helper2");continue;}
                int upper=initial>=1&&initial<=5?Math.min(5,initial+1):1;
                if(initial>5){effect(owner,MobEffects.STRENGTH,80,initial);layer(hidden(owner.getEffect(MobEffects.STRENGTH)),1,200);}else{
                    var strength=effect(owner,MobEffects.STRENGTH,200,upper);if(initial==1||initial==5)layer(hidden(strength),1,200);else{layer(hidden(strength),initial,80);if(initial>=2)layer(hidden(hidden(strength)),1,200);}}
                var speed=effect(owner,MobEffects.SPEED,200,upper);effect(owner,MobEffects.NAUSEA,200,1);if(initial>=1&&initial<=5)layer(hidden(speed),1,200);else require(hidden(speed)==null,"generic helper2 has no fabricated upper speed layer");
                boolean harm=initial>=2&&initial<=5,wither=initial>=3&&initial<=5;if(harm)effect(owner,MobEffects.INSTANT_DAMAGE,1,initial==2?0:initial==3?1:2);else require(!owner.hasEffect(MobEffects.INSTANT_DAMAGE),"no harm outside exact helpers4..6");
                if(wither)effect(owner,MobEffects.WITHER,200,initial==3?3:5);else require(!owner.hasEffect(MobEffects.WITHER),"no wither outside helpers5/6");
                require(owner.getAdvancements().getOrStartProgress(task).isDone()==(initial==4||initial==5),"only real helper6 branch explicitly awards impossible task");
            }clean();
        }
        void acceptedAndGuards(){
            for(var leaf:List.of("devil_sword","legacy_devil_sword")){var stack=held(leaf);var target=cow();try{for(var f:combat.keySet())f.setInt(owner,200);}catch(IllegalAccessException e){throw new IllegalStateException(e);}double damage=owner.getAttributeValue(Attributes.ATTACK_DAMAGE);owner.attack(target);near(target.getHealth(),1000-damage,"real full-strength native melee damage");require(target.getLastHurtByPlayer()==owner,"real native melee owner");effect(owner,MobEffects.STRENGTH,200,0);require(stack.getDamageValue()==1,"only actual native melee spends ordinary sword durability");clean();}
            held("devil_sword");var target=cow();target.setHealth(1);require(hit(target,DamageTypes.MAGIC)&&target.isDeadOrDying(),"actual fatal accepted nonarrow hit");effect(owner,MobEffects.STRENGTH,200,0);clean();
            held("devil_sword");var self=game.items().indirectKills().captureDamage(source(owner,DamageTypes.MAGIC),game.items().indirectKills().wildCause(owner));require(owner.hurtServer(owner.level(),self,1),"actual accepted WILD attributed self damage under same-team protection");effect(owner,MobEffects.STRENGTH,200,0);clean();
            held("devil_sword");target=cow();require(hit(target,DamageTypes.ARROW)&&owner.getActiveEffects().isEmpty(),"arrow-only never activates ladder");target.setPermanentlyInvulnerable(true);require(!hit(target,DamageTypes.MAGIC)&&owner.getActiveEffects().isEmpty(),"rejected native hurt never activates ladder");target.setPermanentlyInvulnerable(false);
            var offhand=owner.getMainHandItem();owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));owner.setItemInHand(InteractionHand.OFF_HAND,offhand);require(hit(target,DamageTypes.MAGIC)&&owner.getActiveEffects().isEmpty(),"source selected mainhand only");clean();
            for(var tag:List.of("nomagic_active","time_stopped")){held("devil_sword");target=cow();owner.entityTags().add(tag);try{hit(target,DamageTypes.MAGIC);require(owner.getActiveEffects().isEmpty(),"native input/suppression guard "+tag);}finally{owner.entityTags().remove(tag);}clean();}
            held("devil_sword");target=cow();base.field=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());base.field.setPos(at);base.field.entityTags().add("stopper_magic");owner.level().addFreshEntity(base.field);game.items().magic().loaded(base.field);require(hit(target,DamageTypes.MAGIC)&&owner.getActiveEffects().isEmpty(),"source native stopper marker suppresses accepted hit");clean();
            held("devil_sword");target=cow();target.entityTags().add("strict_map_object");hit(target,DamageTypes.MAGIC);require(owner.getActiveEffects().isEmpty(),"native role guard prevents passive activation");clean();require(base.mana.get(owner)==-50,"all accepted source branches are mana-free");
        }
        void inventoryChallenge(){
            var challenge=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/swords/devil_full"));require(challenge!=null,"existing native five-item Devil inventory challenge");var display=challenge.value().display().orElseThrow();var icon=display.icon().create();
            require(display.type()==net.minecraft.advancements.AdvancementType.CHALLENGE&&challenge.value().parent().orElseThrow().equals(task.id())&&icon.is(Items.WOODEN_SWORD)&&Identifier.parse("lbc:devilsword").equals(icon.get(DataComponents.ITEM_MODEL))&&Boolean.TRUE.equals(icon.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)),"source Devil full challenge parent/frame/glint icon");text(display.title(),"lbc.adv.sword.devil_full",null);text(display.description(),"lbc.adv.sword.devil_full.lore",null);
            for(var key:challenge.value().criteria().keySet())owner.getAdvancements().revoke(challenge,key);owner.getInventory().clearContent();
            var sword=loot("devil_sword");var totem=loot("devil_totem");var blood=loot("blood_wand");var ghast=loot("summoner_ghasts");
            for(var entry:List.of(Map.entry(totem,"deviltotem"),Map.entry(blood,"bloodmagic"),Map.entry(ghast,"summonerghast")))require(LbcFartus.data(entry.getKey()).getIntOr(entry.getValue(),0)==1,"real source donor matches existing inventory predicate "+entry.getValue());
            var accessory=new ItemStack(Items.STICK);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("transformable",1);flags.putInt("accessory",1);flags.putInt("demonkishki",1);accessory.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(flags));
            int slot=9;for(var stack:List.of(sword,totem,blood,ghast))owner.getInventory().setItem(slot++,stack);owner.inventoryMenu.broadcastChanges();require(!owner.getAdvancements().getOrStartProgress(challenge).isDone(),"four matching source items cannot grant five-item challenge");
            owner.getInventory().setItem(slot,accessory);owner.inventoryMenu.broadcastChanges();require(owner.getAdvancements().getOrStartProgress(challenge).isDone(),"actual native inventory change grants exact fifth custom-data predicate; donor abilities are not invoked");owner.getInventory().clearContent();clean();
        }
        void command(ServerPlayer player,String effect){try{int result=game.server().getCommands().getDispatcher().execute("effect give "+player.getUUID()+" minecraft:"+effect,game.server().createCommandSourceStack().withLevel(player.level()).withSuppressedOutput());require(result==1,"independent native effect command control "+effect);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}}
        void normalizeFood(){for(var player:List.of(owner,other)){var food=new net.minecraft.nbt.CompoundTag();food.putInt("foodLevel",20);food.putInt("foodTickTimer",0);food.putFloat("foodSaturationLevel",20);food.putFloat("foodExhaustionLevel",0);player.getFoodData().readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess(),food));}}
        void beginScheduled(){
            clean();normalizeFood();ownerClient=new ScheduledClient(game,owner);otherClient=new ScheduledClient(game,other);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));require(owner.hurtServer(owner.level(),source(other,DamageTypes.MAGIC),1),"actual prior hostile credit control");require(owner.getKillCredit()==other,"native prior hostile owner established");owner.setHealth(1000);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
            held("devil_sword");owner.addEffect(ItemEffectLevels.create(MobEffects.STRENGTH,80,2),owner);require(hit(cow(),DamageTypes.MAGIC),"source helper4 scheduled self harm setup");effect(owner,MobEffects.INSTANT_DAMAGE,1,0);near(owner.getHealth(),1000,"instant damage is scheduled, not a synchronous synthetic hit");
            base.mana.set(owner,5000);base.mana.set(other,5000);started=game.server().getTickCount();phase=1;
        }
        boolean step(){require(game.playing()&&game.items().currentEpoch(epoch)&&!owner.isRemoved()&&owner.isAlive()&&owner.level()==game.maps().level()&&game.server().getPlayerList().getPlayer(other.getUUID())==other,"native asynchronous owner/match remains valid");int now=game.server().getTickCount();if(now==lastTick||now<=started)return false;lastTick=now;
            int ownerUpdates=ownerClient.updates(),otherUpdates=otherClient.updates();
            require(ownerUpdates>lastOwnerUpdate&&otherUpdates>lastOtherUpdate&&ownerUpdates==otherUpdates,"actual server connection scheduler processed both native player listeners exactly together; owner="+ownerUpdates+" other="+otherUpdates+" serverTick="+now);
            lastOwnerUpdate=ownerUpdates;lastOtherUpdate=otherUpdates;
            if(phase==1){require(!owner.hasEffect(MobEffects.INSTANT_DAMAGE),"one native scheduled instant effect expires after real owner update");near(owner.getHealth(),994,"native scheduled HarmI deals exact6HP without direct heal/hurt substitute");require(owner.getLastDamageSource()!=null&&owner.getLastDamageSource().is(DamageTypes.MAGIC)&&owner.getLastDamageSource().getEntity()==null&&owner.getKillCredit()==other,"native scheduled self harm preserves environment source and prior hostile credit");
                var strength=owner.getEffect(MobEffects.STRENGTH);require(strength!=null&&strength.getAmplifier()==3,"unowned native instant tick cannot recursively progress ladder");int elapsed=200-strength.getDuration();require(elapsed>=1&&elapsed<=3,"actual native first update timing");layer(hidden(strength),2,80-elapsed);layer(hidden(hidden(strength)),1,200-elapsed);layer(hidden(owner.getEffect(MobEffects.SPEED)),1,200-elapsed);
                clean();normalizeFood();held("devil_sword");owner.addEffect(ItemEffectLevels.create(MobEffects.STRENGTH,80,4),owner);require(hit(cow(),DamageTypes.MAGIC),"source helper6 native200tick curse setup");effect(owner,MobEffects.WITHER,200,5);effect(owner,MobEffects.INSTANT_DAMAGE,1,2);require(owner.getKillCredit()==other,"source self curse grant preserves existing hostile credit");
                command(other,"wither 10 5");command(other,"instant_damage 1 2");started=now;phase=2;return false;
            }
            require(phase==2&&now-started<=205,"native200tick curse probe completes without refresh/lifecycle stall");near(owner.getHealth(),other.getHealth(),"native source curse matches independent native WitherVI/HarmIII command control including cooldowns");
            var strength=owner.getEffect(MobEffects.STRENGTH);var wither=owner.getEffect(MobEffects.WITHER);
            if(!firstCurseTick){require(!owner.hasEffect(MobEffects.INSTANT_DAMAGE)&&!other.hasEffect(MobEffects.INSTANT_DAMAGE),"both one-tick native harm grants expire on actual scheduler");require(owner.getKillCredit()==other,"native instant self curse retains prior hostile credit rather than synthesizing self attacker");firstCurseTick=true;}
            if(wither!=null){require(strength!=null&&strength.getAmplifier()==5&&strength.getDuration()==wither.getDuration()&&wither.getAmplifier()==5,"native source clocks decline together without recursive recharge");int elapsed=200-wither.getDuration();require(elapsed>0,"real effect update occurred");var low=hidden(strength);if(elapsed<80){layer(low,4,80-elapsed);layer(hidden(low),1,200-elapsed);}else layer(low,1,200-elapsed);layer(hidden(owner.getEffect(MobEffects.SPEED)),1,200-elapsed);return false;}
            require(now-started>=200&&owner.isAlive()&&owner.getHealth()<994&&owner.getLastDamageSource()!=null&&owner.getLastDamageSource().is(DamageTypes.WITHER)&&owner.getLastDamageSource().getEntity()==null,"full200 native Wither ticks ran, damaged self with original environment source and expired");
            require(!owner.hasEffect(MobEffects.STRENGTH)&&!owner.hasEffect(MobEffects.SPEED)&&!owner.hasEffect(MobEffects.NAUSEA)&&!owner.hasEffect(MobEffects.INSTANT_DAMAGE)&&!other.hasEffect(MobEffects.WITHER),"all source and hidden200tick native effect layers expire without renewal");return true;
        }
        void closeClients(){try{if(otherClient!=null)otherClient.close();}finally{otherClient=null;try{if(ownerClient!=null)ownerClient.close();}finally{ownerClient=null;}}}
        @Override public void close(){try{closeClients();clean();owner.level().getGameRules().set(GameRules.MOB_DROPS,drops,game.server());owner.level().getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,regen,game.server());base.mana.score(owner,"food_count").set(food);for(var e:combat.entrySet())e.getKey().set(owner,e.getValue());var board=game.server().getScoreboard();board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(otherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),otherTeam);ownerSnapshot.restore(game);otherSnapshot.restore(game);}catch(IllegalAccessException e){throw new IllegalStateException(e);}finally{try{base.close();}finally{attribution.close();}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive native Devil fixture");running=new Fixture(game);try{running.presentation();running.ladder();running.acceptedAndGuards();running.inventoryChallenge();running.beginScheduled();}catch(Throwable e){try{running.close();}finally{running=null;}throw e;}}
    public static void tick(GameController game){if(game==null||running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS devil-sword (exact current/legacy vanilla payload and source pool; every signed/high strength branch/hidden layer; native melee/nonarrow/fatal/self/admission; helper6 task and real inventory challenge; scheduled Harm and full200native Wither expiry/credit/no recursion against independent native control)");}}catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL devil-sword",e);}}
}
