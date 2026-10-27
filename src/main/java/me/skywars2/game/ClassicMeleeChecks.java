package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Source vanilla-item passives cross native damage/effect callbacks; no private runtime service state. */
public final class ClassicMeleeChecks {
    private ClassicMeleeChecks(){}
    private static ServerPlayer fixtureGamma;private static boolean createdGamma;
    public static void setup(GameController game){
        require(game.inLobby(),"classic native three-player setup requires ready lobby");require(fixtureGamma==null,"exclusive scoped third participant setup");
        var previous=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));fixtureGamma=GameChecks.createBot(game,"TestGamma");createdGamma=previous==null;
        require(fixtureGamma!=null&&fixtureGamma.getClass()==ServerPlayer.class,"third participant is a real vanilla ServerPlayer");
        SkyWars2.LOGGER.info("[check] PASS classic-melee-menu (scoped native third participant; created{})",createdGamma);
    }
    public static void cleanupLobby(GameController game){
        require(game.inLobby(),"classic third-player cleanup follows actual match stop");
        try{if(createdGamma&&fixtureGamma!=null){fixtureGamma.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(Component.literal("Classic melee fixture complete")));
            require(game.server().getPlayerList().getPlayer(fixtureGamma.getUUID())==null,"owned third participant left through actual native disconnect callback");}}
        finally{fixtureGamma=null;createdGamma=false;}
        SkyWars2.LOGGER.info("[check] PASS classic-melee-lobby-cleanup");
    }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Classic melee: "+why);}
    private static void near(double a,double b,String why){require(Math.abs(a-b)<1e-4,why+" actual="+a+" expected="+b);}
    private static void text(Component c,String key,Integer color){require(c!=null&&c.getContents() instanceof TranslatableContents t&&t.getKey().equals(key)&&!c.getStyle().isItalic()
            &&(color==null?c.getStyle().getColor()==null:c.getStyle().getColor()!=null&&c.getStyle().getColor().getValue()==color),"source text/style "+key);}
    private static void effect(LivingEntity entity,Holder<MobEffect> kind,int ticks,int amp,boolean visible){var e=entity.getEffect(kind);
        require(e!=null&&e.getDuration()==ticks&&e.getAmplifier()==amp&&e.isVisible()==visible,"exact native effect "+kind+" on "+entity+" expected="+ticks+"/"+amp+" visible="+visible+" actual="+e);}
    /** Records the actual native accepted-effect callback, including its author, without replacing effect application. */
    private static final class WitnessCow extends Cow {
        final Map<Holder<MobEffect>,Entity> authors=new HashMap<>();
        WitnessCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override protected void onEffectAdded(MobEffectInstance effect,Entity source){super.onEffectAdded(effect,source);authors.put(effect.getEffect(),source);}
        @Override protected void onEffectUpdated(MobEffectInstance effect,boolean refresh,Entity source){super.onEffectUpdated(effect,refresh,source);if(refresh)authors.put(effect.getEffect(),source);}
    }
    private static final class Audience implements AutoCloseable {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState saved;final ServerGamePacketListenerImpl connection;final ElementalGrimoireChecks.Recording recording;
        Audience(GameController game,ServerPlayer player){this.player=player;saved=new GameChecks.WandThirdPlayerState(player,game.items().mana());connection=player.connection;
            recording=new ElementalGrimoireChecks.Recording(player);player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());}
        void restore(GameController game){try{saved.restore(game);}finally{player.connection=connection;}}
        @Override public void close(){}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final Audience enemy,ally;final SauvojenMeditationChecks.Attribution attribution;
        final List<LivingEntity> mobs=new ArrayList<>();final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>(),combat=new LinkedHashMap<>();
        final Map<ServerPlayer,Map<net.minecraft.stats.Stat<?>,Integer>> sideStats=new LinkedHashMap<>();
        final Map<ServerPlayer,Map<net.minecraft.advancements.AdvancementHolder,Set<String>>> sideAdvancements=new LinkedHashMap<>();
        final Map<ServerPlayer,Map<java.lang.reflect.Field,Object>> sideProfiles=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam team,oldOwnerTeam,oldEnemyTeam,oldAllyTeam;final int food;final boolean drops;
        final java.lang.reflect.Field randomField;final RandomSource originalRandom;int roll,draws;final Vec3 at;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);
            var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));var gamma=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma"));require(beta!=null&&gamma!=null&&gamma==fixtureGamma&&!game.isObserver(gamma)&&game.canUseGameplayItems(gamma),"three real native participants admitted at match start; run classic-melee-menu in lobby first");
            enemy=new Audience(game,beta);ally=new Audience(game,gamma);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(base.owner,beta,gamma));
            food=base.mana.score(base.owner,"food_count").get();drops=base.owner.level().getGameRules().get(GameRules.MOB_DROPS);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,base.owner.getStats().getValue(stat));}
            for(var item:List.of(Items.WOODEN_SWORD,Items.IRON_SWORD))for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN)){var stat=type.get(item);stats.put(stat,base.owner.getStats().getValue(stat));}
            for(var type:List.of(EntityTypes.COW,EntityTypes.SKELETON)){var killed=Stats.ENTITY_KILLED.get(type);stats.put(killed,base.owner.getStats().getValue(killed));}
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){var completed=new HashSet<String>();base.owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(completed::add);advancements.put(advancement,completed);}
            for(var player:List.of(beta,gamma)){var values=new LinkedHashMap<net.minecraft.stats.Stat<?>,Integer>();for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);values.put(stat,player.getStats().getValue(stat));}for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN)){var stat=type.get(Items.SHIELD);values.put(stat,player.getStats().getValue(stat));}sideStats.put(player,values);
                var progress=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,Set<String>>();for(var adv:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);progress.put(adv,done);}sideAdvancements.put(player,progress);}
            try{for(var player:List.of(beta,gamma)){var p=game.players().get(player.getUUID());var values=new LinkedHashMap<java.lang.reflect.Field,Object>();for(var f:p.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var value=f.get(p);values.put(f,value instanceof int[] a?a.clone():value instanceof Set<?> set?new HashSet<>(set):value);}sideProfiles.put(player,values);}
                var p=game.players().get(base.owner.getUUID());for(var f:p.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var value=f.get(p);profile.put(f,value instanceof int[] a?a.clone():value instanceof Set<?> set?new HashSet<>(set):value);}
                for(var name:List.of("attackStrengthTicker","itemSwapTicker")){var f=field(base.owner,name);combat.put(f,f.get(base.owner));}
                randomField=field(base.owner,"random");originalRandom=(RandomSource)randomField.get(base.owner);
                var fixtureRandom=RandomSource.create(0x434c41535349434cL);
                randomField.set(base.owner,java.lang.reflect.Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                    if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(4)){draws++;return roll;}
                    try{return method.invoke(fixtureRandom,args);}catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}}));
            }catch(ReflectiveOperationException error){throw new IllegalStateException("native fixture snapshots/RNG",error);}
            var board=game.server().getScoreboard();oldOwnerTeam=base.owner.getTeam();oldEnemyTeam=beta.getTeam();oldAllyTeam=gamma.getTeam();require(board.getPlayerTeam("sw2_classic_guard")==null,"exclusive fixture team");team=board.addPlayerTeam("sw2_classic_guard");team.setAllowFriendlyFire(false);
            board.addPlayerToTeam(base.owner.getScoreboardName(),team);board.addPlayerToTeam(gamma.getScoreboardName(),team);if(beta.getTeam()!=null)board.removePlayerFromTeam(beta.getScoreboardName(),beta.getTeam());
            base.initialize();base.owner.removeAllEffects();base.owner.setSwimming(false);base.owner.setSprinting(false);base.owner.fallDistance=0;at=base.owner.position();
            var p=game.players().get(base.owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;Arrays.fill(p.trinkets,0);p.lbcChallenge=0;
            base.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);base.owner.setHealth(100);base.owner.setAbsorptionAmount(0);base.mana.set(base.owner,-50);
            for(var player:List.of(base.owner,beta,gamma)){for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))player.setItemSlot(slot,ItemStack.EMPTY);}
            ready(beta,at.add(0,0,3));ready(gamma,at.add(3,0,0));base.owner.level().getGameRules().set(GameRules.MOB_DROPS,false,game.server());
        }
        static java.lang.reflect.Field field(Object value,String name)throws NoSuchFieldException {Class<?> type=value.getClass();while(type!=null){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}throw new NoSuchFieldException(name);}
        void ready(ServerPlayer player,Vec3 pos){player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=false;player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);player.setHealth(100);player.setAbsorptionAmount(0);player.teleportTo(player.level(),pos.x,pos.y,pos.z,Set.of(),180,0,true);player.setInvulnerableTime(0);player.damageCooldownTime=0;}
        WitnessCow cow(Vec3 pos){var c=new WitnessCow(base.owner.level());c.setPos(pos);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);c.setHealth(100);require(base.owner.level().addFreshEntity(c),"native witness cow added");mobs.add(c);require(base.owner.level().getEntities(base.owner,c.getBoundingBox(),e->e==c).contains(c),"native spatial query immediately sees resident-map fixture cow");return c;}
        ItemStack loot(String leaf){var list=game.lbc().rollTable(base.owner.level(),at,base.owner,"lbc:ported/"+leaf);require(list.size()==1,"singleton native leaf "+leaf);return list.getFirst();}
        void held(String leaf){base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);base.owner.setItemInHand(InteractionHand.MAIN_HAND,loot(leaf));}
        boolean hit(LivingEntity target,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> kind){target.setInvulnerableTime(0);target.damageCooldownTime=0;return target.hurtServer(base.owner.level(),new DamageSource(base.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(kind),base.owner),1);}
        void strongAttack(LivingEntity target){try{for(var f:combat.keySet())f.setInt(base.owner,200);}catch(IllegalAccessException error){throw new IllegalStateException(error);}target.setInvulnerableTime(0);target.damageCooldownTime=0;base.owner.attack(target);}
        void melee(WitnessCow target){double damage=base.owner.getAttributeValue(Attributes.ATTACK_DAMAGE);strongAttack(target);near(target.getHealth(),100-damage,"full-strength native unenchanted melee damage");require(target.getLastHurtByPlayer()==base.owner,"real native Player.attack credited");}
        void clean(){base.owner.removeAllEffects();enemy.player.removeAllEffects();ally.player.removeAllEffects();for(var cow:mobs){game.items().indirectKills().clear(cow);cow.discard();}mobs.clear();base.removeField();base.owner.setSwimming(false);base.recording.packets.clear();enemy.recording.packets.clear();ally.recording.packets.clear();}
        void sourcePresentation(){
            for(var leaf:List.of("feeding_sword","legacy_feeding_sword","vampire_fang","legacy_vampire_fang","guardian_sword")){
                boolean feeding=leaf.contains("feeding"),vampire=leaf.contains("vampire"),legacy=leaf.startsWith("legacy_");var stack=loot(leaf);var vanilla=new ItemStack(feeding?Items.WOODEN_SWORD:Items.IRON_SWORD);
                require(stack.is(vanilla.getItem())&&stack.getMaxDamage()==vanilla.getMaxDamage(),"source ordinary vanilla item/default durability "+leaf);
                var flag=feeding?"feeding":vampire?"vampire":"seaguardian";var data=LbcFartus.data(stack);require(data.getIntOr(flag,0)==1&&data.getIntOr("transformable",0)==(legacy?0:1)&&data.getIntOr("sword",0)==(legacy?0:1),"exact legacy/current source flags "+leaf);
                require(Identifier.parse("lbc:"+(feeding?"steaksword":vampire?"vampiresword":"guardiansword")).equals(stack.get(DataComponents.ITEM_MODEL)),"source authored model "+leaf);
                require(legacy?stack.get(DataComponents.TOOLTIP_STYLE)==null:Identifier.parse("lbc:"+(feeding?"tier_d":vampire?"tier_c":"tier_b")).equals(stack.get(DataComponents.TOOLTIP_STYLE)),"source tooltip presence/style "+leaf);
                Integer nameColor=null;if(feeding)nameColor=0x780000;else if(vampire)nameColor=0xaa0000;text(stack.get(DataComponents.CUSTOM_NAME),feeding?"steak_sword":vampire?"vampire_fang":"guardian_sword",nameColor);
                var lore=stack.get(DataComponents.LORE).lines();require(lore.size()==(feeding?(legacy?1:2):(legacy?2:3)),"source exact lore row count "+leaf);int start=legacy?0:1;
                if(!legacy){var type=lore.getFirst();text(type,"type",0x55ffff);require(type.getSiblings().size()==1,"source type row has exactly one sword sibling");text(type.getSiblings().getFirst(),"sword",0x55ffff);}
                for(int i=start;i<lore.size();i++)text(lore.get(i),feeding?"steak_sword.lore":(vampire?"vampire_fang":"guardian_sword")+".lore."+(i-start+1),vampire?0xff5555:0xffffff);
                var attrs=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);if(!vampire)require(attrs.equals(vanilla.get(DataComponents.ATTRIBUTE_MODIFIERS)),"omitted source attributes preserve vanilla defaults "+leaf);
                else require(attrs.modifiers().size()==1&&attrs.modifiers().getFirst().modifier().amount()==1&&attrs.modifiers().getFirst().attribute().equals(Attributes.ATTACK_DAMAGE)
                        &&attrs.modifiers().getFirst().modifier().id().equals(Identifier.parse(legacy?"minecraft:vu89qjduxlsodkiogaisrepog":"minecraft:base_attack_damage")),"exact distinct legacy/current Vampire damage-only modifier");
            }
            try{var pool=json("transformables_sword").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
                for(var row:List.of(Map.entry("feeding_sword","feeding:1"),Map.entry("vampire_fang","vampire:1"),Map.entry("guardian_sword","seaguardian:1"))){com.google.gson.JsonObject original=null;
                    for(var entry:pool)if(entry.toString().contains(row.getValue())){require(original==null,"unique compiled original acquisition entry "+row.getKey());original=entry.getAsJsonObject().deepCopy();}
                    require(original!=null,"compiled original transform/reforge acquisition includes "+row.getKey());var leaf=json("ported/"+row.getKey()).getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().deepCopy();
                    original.remove("weight");original.remove("quality");leaf.remove("weight");leaf.remove("quality");require(original.equals(leaf),"compiled acquisition entry preserves exact source components/flags matching native leaf "+row.getKey());}
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
        }
        com.google.gson.JsonObject json(String leaf)throws java.io.IOException{try(var stream=ClassicMeleeChecks.class.getClassLoader().getResourceAsStream("data/lbc/loot_table/"+leaf+".json")){require(stream!=null,"compiled loot resource "+leaf);return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
        void feeding(){
            // Independent native command confirms instantaneous source duration conversion rather than copying service constants.
            try{game.server().getCommands().getDispatcher().execute("effect give "+base.owner.getUUID()+" minecraft:saturation 1 127 true",game.server().createCommandSourceStack().withLevel(base.owner.level()).withSuppressedOutput());}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}effect(base.owner,MobEffects.SATURATION,1,127,false);base.owner.removeAllEffects();
            for(var leaf:List.of("feeding_sword","legacy_feeding_sword")){held(leaf);var target=cow(at.add(0,0,2));base.owner.getFoodData().setFoodLevel(1);base.owner.getFoodData().setSaturation(0);int count=base.mana.score(base.owner,"food_count").get();base.recording.packets.clear();melee(target);
                effect(base.owner,MobEffects.SATURATION,1,127,false);require(base.mana.score(base.owner,"food_count").get()==count+1,"one native accepted-hit food_count increment "+leaf);
                var saturation=base.owner.getEffect(MobEffects.SATURATION);require(!saturation.tickServer(base.owner.level(),base.owner,()->{})&&base.owner.getFoodData().getFoodLevel()==20&&base.owner.getFoodData().getSaturationLevel()==20,"real native instant saturation fills actual food/saturation then expires");
                for(var id:List.of("minecraft:entity.generic.eat","minecraft:entity.player.burp"))sound(base.recording,id);clean();}
            held("feeding_sword");var target=cow(at.add(0,0,2));int count=base.mana.score(base.owner,"food_count").get();require(hit(target,DamageTypes.MAGIC),"accepted external nonarrow native hit");require(base.mana.score(base.owner,"food_count").get()==count+1,"source passives include attributed nonarrow indirect damage");base.owner.removeAllEffects();
            target.setHealth(1);target.setInvulnerableTime(0);target.damageCooldownTime=0;require(target.hurtServer(base.owner.level(),new DamageSource(base.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),base.owner),2)&&target.isDeadOrDying()&&base.mana.score(base.owner,"food_count").get()==count+2,"real fatal accepted damage triggers exactly once");clean();
        }
        void vampire(){for(var leaf:List.of("vampire_fang","legacy_vampire_fang")){held(leaf);base.owner.setHealth(10);base.owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,0),base.owner);var target=cow(at.add(0,0,2));melee(target);
                effect(base.owner,MobEffects.REGENERATION,20,4,false);near(base.owner.getHealth(),10,"source Vampire gives regeneration without direct instant health injection");var regen=base.owner.getEffect(MobEffects.REGENERATION);var hidden=((EffectLayers.Access)regen).skywars2$layerHidden();require(hidden!=null&&hidden.getAmplifier()==0&&hidden.getDuration()==100,"native lower regeneration layer preserved");
                for(int i=0;i<20;i++)require(regen.tickServer(base.owner.level(),base.owner,()->{}),"native regeneration advances into live lower layer");
                require(regen.getAmplifier()==0&&regen.getDuration()==80,"actual20 native effect ticks expire V and resume lower layer at80");near(base.owner.getHealth(),16,"native Regeneration V heals six times at its3-tick duration cadence");clean();}
            held("vampire_fang");base.owner.setHealth(50);base.owner.setInvulnerableTime(0);base.owner.damageCooldownTime=0;
            var self=game.items().indirectKills().captureDamage(new DamageSource(base.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),base.owner),game.items().indirectKills().wildCause(base.owner));
            require(base.owner.hurtServer(base.owner.level(),self,1),"accepted native attributed WILD self damage under protected team policy");effect(base.owner,MobEffects.REGENERATION,20,4,false);clean();}
        void sound(ElementalGrimoireChecks.Recording recording,String id){var sounds=recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(p->p.getSound().value().location().toString().equals(id)).toList();
            require(sounds.size()==1&&sounds.getFirst().getSource()==net.minecraft.sounds.SoundSource.MASTER&&sounds.getFirst().getVolume()==1&&sounds.getFirst().getPitch()==1,"source exact MASTER1/pitch1 sound "+id);}
        void guardian(){
            for(boolean swimming:new boolean[]{true,false}){
                held("guardian_sword");base.owner.setSwimming(swimming);roll=0;draws=0;WitnessCow first=cow(at.add(0,0,2)),second=cow(at.add(0,0,-6)),edge=cow(at.add(7,0,0)),outside=cow(at.add(7.01,0,0));
                var friendly=cow(at.add(-2,0,0));game.server().getScoreboard().addPlayerToTeam(friendly.getScoreboardName(),team);
                var greed=cow(at.add(0,0,-2));greed.entityTags().add("sin_greed_active");var technical=cow(at.add(-3,0,0));technical.entityTags().add("strict_map_object");var spectator=cow(at.add(0,0,-3));spectator.entityTags().add("spectator");var tooClose=cow(at.add(.005,0,0));
                base.recording.packets.clear();enemy.recording.packets.clear();ally.recording.packets.clear();require(hit(first,DamageTypes.MAGIC),"actual native Guardian trigger");require(draws==(swimming?0:1),"swimming branch consumes no random draw; dry branch exactly one native quarter draw");
                for(var target:List.of(first,second,edge,enemy.player)){effect(target,MobEffects.MINING_FATIGUE,swimming?1200:400,127,true);effect(target,swimming?MobEffects.WEAKNESS:MobEffects.WATER_BREATHING,swimming?200:40,1,true);}
                for(var target:List.of(first,second,edge))require(target.authors.get(MobEffects.MINING_FATIGUE)==base.owner&&target.authors.get(swimming?MobEffects.WEAKNESS:MobEffects.WATER_BREATHING)==base.owner,"actual accepted native effect callbacks retain Guardian source owner");
                for(var target:List.of(outside,friendly,greed,technical,spectator,tooClose,ally.player))require(!target.hasEffect(MobEffects.MINING_FATIGUE)&&!target.hasEffect(MobEffects.WEAKNESS)&&!target.hasEffect(MobEffects.WATER_BREATHING),"source exact radius/team/role/Greed/spectator/self-distance guard "+target);
                require(!base.owner.hasEffect(MobEffects.MINING_FATIGUE)&&!base.owner.hasEffect(MobEffects.WEAKNESS),"Guardian self excluded from enemy effects");if(swimming)effect(base.owner,MobEffects.WATER_BREATHING,200,2,true);else require(!base.owner.hasEffect(MobEffects.WATER_BREATHING),"dry Guardian gives beneficial source breath only to enemies");
                var packets=enemy.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(p->p.particle().getType()==ParticleTypes.ELDER_GUARDIAN).toList();require(packets.size()==1,"one targeted enemy native Elder packet");var particle=packets.getFirst();
                require(particle.overrideLimiter()&&!particle.alwaysShow()&&particle.count()==10&&particle.xDist()==5&&particle.yDist()==5&&particle.zDist()==5&&particle.xMaxSpeed()==1&&particle.yMaxSpeed()==1&&particle.zMaxSpeed()==1&&new Vec3(particle.x(),particle.y(),particle.z()).equals(at),"source forced10 elder particles exact origin/spread5/speed1");
                for(var recording:List.of(base.recording,ally.recording))require(recording.packets.stream().noneMatch(p->p instanceof ClientboundLevelParticlesPacket packet&&packet.particle().getType()==ParticleTypes.ELDER_GUARDIAN),"source excludes caster/allied particle recipients");sound(base.recording,"minecraft:entity.elder_guardian.curse");sound(ally.recording,"minecraft:entity.elder_guardian.curse");
                clean();}
            held("guardian_sword");for(int value:new int[]{1,3}){roll=value;draws=0;var target=cow(at.add(0,0,2));require(hit(target,DamageTypes.MAGIC),"accepted dry failed-proc control");require(draws==1&&!target.hasEffect(MobEffects.MINING_FATIGUE)&&!base.owner.hasEffect(MobEffects.WATER_BREATHING),"exact quarter predicate rejects nonzero native draw "+value);clean();}
        }
        void achievements(){
            var holders=new LinkedHashMap<String,net.minecraft.advancements.AdvancementHolder>();
            for(var key:List.of("steak","vampire","guardian")){
                var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/swords/"+key));require(advancement!=null,"actual imported "+key+" advancement loaded");holders.put(key,advancement);
                require(advancement.value().criteria().keySet().equals(Set.of("requirement")),"exact source single requirement criterion "+key);
                require(advancement.value().parent().orElseThrow().equals(Identifier.parse("lbc:true_advancements/swords/"+(key.equals("steak")?"epic":key.equals("vampire")?"devil":"gravity"))),"source advancement parent "+key);
                var display=advancement.value().display().orElseThrow();var icon=display.icon().create();
                require(display.type()==net.minecraft.advancements.AdvancementType.TASK&&icon.is(key.equals("steak")?Items.WOODEN_SWORD:Items.IRON_SWORD)
                        &&Identifier.parse("lbc:"+(key.equals("steak")?"steaksword":key.equals("vampire")?"vampiresword":"guardiansword")).equals(icon.get(DataComponents.ITEM_MODEL)),"source task frame and authored vanilla-backed advancement icon "+key);
                text(display.title(),"lbc.adv.sword."+key,null);text(display.description(),"lbc.adv.sword."+key+".lore",null);
                for(var criterion:advancement.value().criteria().keySet())base.owner.getAdvancements().revoke(advancement,criterion);
                require(!base.owner.getAdvancements().getOrStartProgress(advancement).isDone(),"native criterion starts unearned "+key);
            }
            held("feeding_sword");var feeding=base.owner.getMainHandItem();feeding.setDamageValue(feeding.getMaxDamage()-1);var target=cow(at.add(0,0,2));melee(target);
            require(feeding.isEmpty()&&base.owner.getAdvancements().getOrStartProgress(holders.get("steak")).isDone(),"actual native feeding sword melee durability break awards Steak criterion");clean();
            held("vampire_fang");var skeleton=new net.minecraft.world.entity.monster.skeleton.Skeleton(EntityTypes.SKELETON,base.owner.level());skeleton.setPos(at.add(0,0,2));skeleton.setNoAi(true);skeleton.setNoGravity(true);skeleton.setHealth(1);
            var noBlood=net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:no_blood"));require(skeleton.getType().builtInRegistryHolder().is(noBlood),"native Skeleton satisfies original no_blood victim tag");
            require(base.owner.level().addFreshEntity(skeleton),"actual native no_blood advancement victim");mobs.add(skeleton);strongAttack(skeleton);
            require(skeleton.isDeadOrDying()&&skeleton.getKillCredit()==base.owner&&base.owner.getAdvancements().getOrStartProgress(holders.get("vampire")).isDone(),"actual native Vampire Skeleton kill awards source no_blood criterion");clean();
            held("guardian_sword");base.owner.setSwimming(true);require(hit(enemy.player,DamageTypes.MAGIC),"accepted native swimming Guardian hit on actual player");
            require(base.owner.getAdvancements().getOrStartProgress(holders.get("guardian")).isDone(),"actual native swimming owner player-hurt criterion awards Guardian");clean();
        }
        void guards(){for(var leaf:List.of("feeding_sword","vampire_fang","guardian_sword")){held(leaf);base.owner.setSwimming(true);var target=cow(at.add(0,0,2));int count=base.mana.score(base.owner,"food_count").get();require(hit(target,DamageTypes.ARROW),"native arrow-only control");require(base.owner.getActiveEffects().isEmpty()&&base.mana.score(base.owner,"food_count").get()==count&&!target.hasEffect(MobEffects.MINING_FATIGUE),"source arrow-only rejects all passive families");
                target.setPermanentlyInvulnerable(true);require(!hit(target,DamageTypes.MAGIC)&&base.owner.getActiveEffects().isEmpty()&&base.mana.score(base.owner,"food_count").get()==count,"rejected native hurt produces no passive");target.setPermanentlyInvulnerable(false);
                enemy.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));enemy.player.startUsingItem(InteractionHand.MAIN_HAND);
                try{var remaining=field(enemy.player,"useItemRemaining");remaining.setInt(enemy.player,remaining.getInt(enemy.player)-6);require(enemy.player.isBlocking(),"actual native raised shield timing");
                    hit(enemy.player,DamageTypes.PLAYER_ATTACK);near(enemy.player.getHealth(),100,"native shield blocks all melee damage");require(base.owner.getActiveEffects().isEmpty()&&base.mana.score(base.owner,"food_count").get()==count&&!enemy.player.hasEffect(MobEffects.MINING_FATIGUE),"actual blocked native hit produces no sword passive "+leaf);
                }catch(ReflectiveOperationException error){throw new IllegalStateException("native shield-use fixture",error);}finally{enemy.player.stopUsingItem();enemy.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
                var offhand=base.owner.getMainHandItem();base.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));base.owner.setItemInHand(InteractionHand.OFF_HAND,offhand);require(hit(target,DamageTypes.MAGIC)&&base.owner.getActiveEffects().isEmpty()&&base.mana.score(base.owner,"food_count").get()==count,"passives inspect source mainhand only "+leaf);clean();}
            for(var leaf:List.of("feeding_sword","vampire_fang","guardian_sword")){held(leaf);base.owner.setSwimming(true);var target=cow(at.add(0,0,2));base.field=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,base.owner.level());base.field.setPos(at);base.field.entityTags().add("stopper_magic");base.owner.level().addFreshEntity(base.field);game.items().magic().loaded(base.field);int before=base.mana.score(base.owner,"food_count").get();require(hit(target,DamageTypes.MAGIC),"native hit remains accepted under suppression");
                if(leaf.equals("feeding_sword"))require(base.mana.score(base.owner,"food_count").get()==before+1&&base.owner.hasEffect(MobEffects.SATURATION),"source Feeding bypasses antimagic");else require(base.owner.getActiveEffects().isEmpty()&&!target.hasEffect(MobEffects.MINING_FATIGUE),"source Vampire/Guardian suppressed");clean();}
            require(base.mana.get(base.owner)==-50,"all classic accepted-hit passives are mana-free");
        }
        @Override public void close(){try{clean();base.mana.score(base.owner,"food_count").set(food);base.owner.level().getGameRules().set(GameRules.MOB_DROPS,drops,game.server());
                randomField.set(base.owner,originalRandom);for(var entry:combat.entrySet())entry.getKey().set(base.owner,entry.getValue());stats.forEach((stat,value)->base.owner.getStats().setValue(base.owner,stat,value));
                for(var entry:profile.entrySet())entry.getKey().set(game.players().get(base.owner.getUUID()),entry.getValue());
                for(var player:sideProfiles.keySet())for(var entry:sideProfiles.get(player).entrySet())entry.getKey().set(game.players().get(player.getUUID()),entry.getValue());
                sideStats.forEach((player,values)->values.forEach((stat,value)->player.getStats().setValue(player,stat,value)));
                sideAdvancements.forEach((player,values)->values.forEach((adv,done)->{for(var criterion:adv.value().criteria().keySet())if(done.contains(criterion))player.getAdvancements().award(adv,criterion);else player.getAdvancements().revoke(adv,criterion);}));
                advancements.forEach((adv,done)->{for(var criterion:adv.value().criteria().keySet())if(done.contains(criterion))base.owner.getAdvancements().award(adv,criterion);else base.owner.getAdvancements().revoke(adv,criterion);});
                var board=game.server().getScoreboard();board.removePlayerTeam(team);if(oldOwnerTeam!=null)board.addPlayerToTeam(base.owner.getScoreboardName(),oldOwnerTeam);if(oldEnemyTeam!=null)board.addPlayerToTeam(enemy.player.getScoreboardName(),oldEnemyTeam);if(oldAllyTeam!=null)board.addPlayerToTeam(ally.player.getScoreboardName(),oldAllyTeam);
            }catch(IllegalAccessException error){throw new IllegalStateException(error);}finally{try{enemy.restore(game);}finally{try{ally.restore(game);}finally{try{base.close();}finally{attribution.close();}}}}}
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.sourcePresentation();f.feeding();f.vampire();f.guardian();f.guards();f.achievements();}SkyWars2.LOGGER.info("[check] PASS classic-melee (five exact vanilla leaves/original reforge payloads; native attacks/nonarrow/fatal/self; instant saturation/food_count/sounds; regeneration hidden expiry; swimming/RNG quarter Guardian native area/team/role/Greed/author/particles; mainhand/arrow/rejected/suppression/mana guards; three actual source advancement criteria and exact teardown)");}
}
