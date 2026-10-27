package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Synchronous native trial checks. Terrain is snapshotted and restored, including explicit template air. */
public final class SauvojenMeditationChecks {
    private SauvojenMeditationChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("Sauvojen meditation: "+message);}
    /** Per-participant entries only: teardown cannot erase another actor's ambient attribution. */
    static final class Attribution implements AutoCloseable {
        final LbcIndirectKills service;
        final Map<java.lang.reflect.Field,Map<net.minecraft.world.entity.LivingEntity,Object>> saved=new LinkedHashMap<>();
        final List<? extends net.minecraft.world.entity.LivingEntity> targets;
        final Map<net.minecraft.world.entity.LivingEntity,Map<java.lang.reflect.Field,Object>> combat=new IdentityHashMap<>();
        Attribution(GameController game,List<? extends net.minecraft.world.entity.LivingEntity> targets){
            service=game.items().indirectKills();this.targets=List.copyOf(targets);
            try{for(var field:LbcIndirectKills.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){
                field.setAccessible(true);var map=(Map<?,?>)field.get(service);var values=new IdentityHashMap<net.minecraft.world.entity.LivingEntity,Object>();
                for(var target:targets)if(map.containsKey(target)){var value=map.get(target);values.put(target,value instanceof Map<?,?> nested?new HashMap<>(nested):value);}
                saved.put(field,values);
            }
                for(var target:targets){var fields=new LinkedHashMap<java.lang.reflect.Field,Object>();combat.put(target,fields);
                    for(var name:List.of("lastHurtByMob","lastHurtByMobTimestamp","lastHurtByPlayer","lastHurtByPlayerMemoryTime","lastHurtMob","lastHurtMobTimestamp")){
                        var field=net.minecraft.world.entity.LivingEntity.class.getDeclaredField(name);field.setAccessible(true);fields.put(field,field.get(target));
                    }
                }
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Meditation attribution snapshot",error);}
        }
        private Object rebound(Object value,net.minecraft.world.entity.LivingEntity target)throws ReflectiveOperationException{
            if(value instanceof Map<?,?> nested){var result=new HashMap<Object,Object>();for(var entry:nested.entrySet())result.put(entry.getKey(),rebound(entry.getValue(),target));return result;}
            if(value!=null&&value.getClass().isRecord()&&value.getClass().getSimpleName().equals("Status")){
                var effect=value.getClass().getDeclaredMethod("effect");var owner=value.getClass().getDeclaredMethod("owner");effect.setAccessible(true);owner.setAccessible(true);
                var previous=(net.minecraft.world.effect.MobEffectInstance)effect.invoke(value);var current=target.getEffect(previous.getEffect());
                var constructor=value.getClass().getDeclaredConstructor(LbcIndirectKills.Owner.class,net.minecraft.world.effect.MobEffectInstance.class);constructor.setAccessible(true);
                return constructor.newInstance(owner.invoke(value),current==null?previous:current);
            }return value;
        }
        @SuppressWarnings("unchecked") public void close(){
            try{for(var entry:saved.entrySet()){
                var map=(Map<Object,Object>)entry.getKey().get(service);for(var target:targets){
                    if(entry.getValue().containsKey(target))map.put(target,rebound(entry.getValue().get(target),target));else map.remove(target);
                }
            }for(var actor:combat.entrySet())for(var field:actor.getValue().entrySet())field.getKey().set(actor.getKey(),field.getValue());
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Meditation attribution restore",error);}
        }
    }
    private static final class Recording extends net.minecraft.network.Connection {
        final net.minecraft.server.level.ServerPlayer player;
        final List<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket> particles=new ArrayList<>();
        Recording(net.minecraft.server.level.ServerPlayer player){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public net.minecraft.network.PacketListener getPacketListener(){return player.connection;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){
            if(packet instanceof net.minecraft.network.protocol.game.ClientboundBundlePacket bundle)bundle.subPackets().forEach(p->send(p,null,false));
            else if(packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket particle)particles.add(particle);
        }
        @Override public void flushChannel(){}
    }
    private static final class Swap implements AutoCloseable {
        final net.minecraft.server.level.ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous;final Recording recording;
        Swap(GameController game,net.minecraft.server.level.ServerPlayer player){this.player=player;previous=player.connection;recording=new Recording(player);
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),recording,player,
                    new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));}
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final SauvojenMeditation service;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final List<Entity> spawned=new ArrayList<>();
        final Set<net.minecraft.world.level.ChunkPos> forced=new LinkedHashSet<>();
        final int savedCounter,savedFear;
        final Attribution attribution;
        Fixture(GameController game){this.game=game;service=game.items().sauvojen().meditation();
            require(service.count()==0,"exclusive actual meditation lifecycle service");base=new ElementalGrimoireChecks.Fixture(game);
            savedCounter=base.mana.score(base.owner,"sauvojen_wand_upgrade").get();savedFear=base.mana.score(base.owner,"scared").get();
            attribution=new Attribution(game,List.of(base.owner));base.initialize();
            try{warmFootprint();}catch(RuntimeException|Error error){close();throw error;}}
        void warmFootprint(){
            var center=base.owner.blockPosition();var level=base.owner.level();
            // The full source121x121 lava plane and native neighbour guards require these chunks.
            // These are test-only force tickets. Production admission still never acquires terrain.
            for(int x=(center.getX()-61)>>4;x<=(center.getX()+61)>>4;x++)
                for(int z=(center.getZ()-61)>>4;z<=(center.getZ()+61)>>4;z++){
                    if(level.setChunkForced(x,z,true))forced.add(new net.minecraft.world.level.ChunkPos(x,z));
                    level.getChunkAt(new BlockPos(x*16,center.getY(),z*16));
                    require(level.getChunkSource().getChunkNow(x,z)!=null,"native warmed full meditation chunk "+x+","+z);
                }
        }
        ItemStack held(){
            var loot=game.lbc().rollTable(base.owner.level(),base.owner.position(),base.owner,"lbc:ported/sauvojen_wand");
            require(loot.size()==1,"ordinary source loot");var stack=loot.getFirst();
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("upgradable",1);tag.putInt("upgraded",0);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));base.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
            base.owner.setShiftKeyDown(true);return stack;
        }
        SauvojenMeditation.Trial admit(){service.stop();var stack=held();require(service.start(base.owner,InteractionHand.MAIN_HAND,stack),"native mainhand admission");return service.trial(base.owner);}
        void save(BlockPos lo,BlockPos hi){
            for(var p:BlockPos.betweenClosed(lo,hi)){
                require(base.owner.level().getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)!=null,"resident full footprint "+p);
                var cell=p.immutable();terrain.computeIfAbsent(cell,key->game.maps().snapshot(base.owner.level(),key));
            }
        }
        void restore(){terrain.forEach((p,state)->game.maps().restoreSnapshot(base.owner.level(),p,state));terrain.clear();}
        boolean complete(SauvojenMeditation.Trial t){
            long started=System.nanoTime();int turns=0;
            do{EffectTerrain.beginTick();if(service.phase(t)){
                SkyWars2.LOGGER.info("[check] Sauvojen terrain phase{} turns{} totalCPUms{} maxTurnCPUms{}",t.remaining,turns+1,
                        (System.nanoTime()-started)/1_000_000d,t.maxTerrainNanos/1_000_000d);return true;
            }}while(++turns<128);return false;
        }
        void admission(){
            var t=admit();require(service.remaining(base.owner)==620,"620 initial countdown");
            require(!service.start(base.owner,InteractionHand.OFF_HAND,t.stack),"offhand cannot admit");
            require(service.start(base.owner,InteractionHand.MAIN_HAND,t.stack)&&service.count()==1,"held repeat is idempotent");
            // A component-identical replacement is a different participating stack.
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,t.stack.copy());service.tick();require(service.count()==0,"exact stack holding guard");
            t=admit();t.remaining=160;service.tick();
            require(base.owner.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION).getAmplifier()==20,"native levitation amplifier20");
            require(service.remaining(base.owner)==159,"one manual active tick");
            service.forget(base.owner);require(service.count()==0,"UUID player lifecycle cleanup");
            t=admit();t.remaining=1;var exact=t.stack;service.tick();
            var data=exact.get(DataComponents.CUSTOM_DATA).copyTag();
            require(base.owner.getMainHandItem()==exact&&data.getIntOr("upgrade_available",0)==1&&data.getIntOr("upgradable",1)==0,
                    "readiness mutates actual participating stack without granting upgraded item");
            require(service.count()==0,"ready closes trial");
            t=admit();t.remaining=310;base.owner.entityTags().add("time_stopped");
            try{service.tick();require(service.remaining(base.owner)==310,"owner Time Stop pauses countdown");}
            finally{base.owner.entityTags().remove("time_stopped");}
            game.items().timeStop().begin(base.owner.level(),40);
            try{service.tick();require(service.remaining(base.owner)==310,"dimension Time Stop pauses countdown");}
            finally{game.items().timeStop().end(base.owner.level());}
            service.stop();
        }
        void ruby(){
            var t=admit();var c=base.owner.blockPosition();save(c.offset(-11,0,-11),c.offset(11,24,11));
            try{
                // Interior edge, excluded corner and destroy drops use real world writes.
                base.owner.level().setBlock(c.offset(10,24,3),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
                base.owner.level().setBlock(c.offset(10,24,4),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
                require(complete(t),"ruby resident phase");
                require(base.owner.level().getBlockState(c.offset(10,24,3)).isAir(),"ruby inclusive upper narrow edge");
                require(base.owner.level().getBlockState(c.offset(10,24,4)).is(Blocks.STONE),"ruby excluded corner retained");
                int cells=0;for(int x=-10;x<=10;x++)cells+=(SauvojenMeditation.rubyHalfWidth(x)*2+1)*25;
                require(cells==8725,"source ruby 349×25 cells");
            }finally{service.stop();restore();}
        }
        void stone(){
            var t=admit();var c=base.owner.blockPosition();save(c.offset(-23,-4,-23),c.offset(23,14,23));
            try{
                var template=game.server().getStructureTemplateManager().get(SauvojenMeditation.STONE).orElseThrow(()->
                        new IllegalStateException("Candidate JAR must package data/lbc/structure/stone_cylinder.nbt"));
                require(template.getSize().getX()==47&&template.getSize().getY()==19&&template.getSize().getZ()==47,"full source stone template dimensions");
                // Fill the volume so explicit air and absent template entries remain distinguishable.
                for(var p:terrain.keySet())base.owner.level().setBlock(p,Blocks.GLASS.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                t.remaining=320;require(complete(t),"native template placement");
                int stone=0,air=0,retained=0;
                for(var p:terrain.keySet()){
                    var state=base.owner.level().getBlockState(p);if(state.is(Blocks.STONE))stone++;else if(state.isAir())air++;else if(state.is(Blocks.GLASS))retained++;
                }
                require(stone==6964&&air==26115&&retained==8892,"exact source explicit stone/air and untouched absent cells "+stone+"/"+air+"/"+retained);
            }finally{service.stop();restore();}
        }
        void fire(){
            var t=admit();var c=base.owner.blockPosition();
            save(c.offset(-15,-50,-15),c.offset(15,0,15));save(c.offset(-60,-21,-60),c.offset(60,-21,60));
            try{
                t.remaining=20;require(complete(t),"full resident fire phase");int lava=0,air=0;
                for(var p:terrain.keySet()){
                    var state=base.owner.level().getBlockState(p);if(state.is(Blocks.LAVA))lava++;else if(state.isAir())air++;
                }
                require(lava==42510&&air==20181,"full source lava union and air pit "+lava+"/"+air);
                require(base.owner.getEffect(net.minecraft.world.effect.MobEffects.LEVITATION)==null,"fire removes levitation");
                require(base.owner.getEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING).getDuration()==80,"source four-second slow falling");
            }finally{service.stop();restore();}
        }
        void hazards(){
            var t=admit();t.remaining=601;service.tick();t.remaining=600;require(service.phase(t),"terra phase");
            require(service.terraTicker()==1,"native non-stopped global angular clock advances without legacy scoreboard writer");
            var rods=service.snapshot(base.owner);
            require(rods.stream().map(h->h.at).distinct().count()==3,"three distinct terra positions");
            for(int i=0;i<3;i++)require(rods.get(i).at.distanceToSqr(base.owner.position().add(0,1,0).add(Vec3.directionFromRotation(0,i+1).scale(10)))<1e-10,"source ticker multiplied by1/2/3 exact coordinates");
            var prior=rods.getFirst().at;t.remaining=599;service.tick();
            require(service.snapshot(base.owner).stream().filter(h->h.kind==SauvojenMeditation.Kind.TERRA).map(h->h.at).anyMatch(p->p.distanceToSqr(prior)>1e-4),"advancing ring has different emission coordinates");
            for(var h:List.copyOf(t.hazards))if(!rods.contains(h)){t.hazards.remove(h);if(h.actor!=null)h.actor.discard();}
            require(service.snapshot(base.owner).size()==3,"three terra emitters");
            for(var h:service.snapshot(base.owner))require(Math.abs(h.at.subtract(base.owner.position()).horizontalDistance()-10)<1e-5,
                    "native radius10 source geometry");
            var actors=service.snapshot(base.owner).stream().map(h->h.actor).toList();service.forget(base.owner);
            require(actors.stream().allMatch(Entity::isRemoved),"scoped actual helper cleanup");
            t=admit();t.remaining=419;require(service.phase(t),"first air column");
            require(service.snapshot(base.owner).getFirst().at.distanceToSqr(base.owner.position().add(Vec3.directionFromRotation(0,base.owner.getYRot()).scale(-18)))<1e-8,
                    "first air column at exact local-18");service.stop();
            // Source damage fails an active trial even if native damage is rejected.
            t=admit();boolean invulnerable=base.owner.getAbilities().invulnerable;base.owner.getAbilities().invulnerable=true;
            try{service.hurt(t,base.owner,8,net.minecraft.world.damagesource.DamageTypes.MAGIC,false);require(service.count()==0,"rejected native damage still fails trial");}
            finally{base.owner.getAbilities().invulnerable=invulnerable;}
            t=admit();var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(base.owner.position().add(0,0,5));cow.setNoAi(true);
            base.owner.level().addFreshEntity(cow);spawned.add(cow);float before=cow.getHealth();
            service.hurt(t,cow,4,net.minecraft.world.damagesource.DamageTypes.IN_FIRE,true);
            require(cow.getHealth()<before&&cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"native acid in_fire and poison");
            require(cow.getEffect(net.minecraft.world.effect.MobEffects.POISON).getAmplifier()==0&&cow.getEffect(net.minecraft.world.effect.MobEffects.POISON).getDuration()==200,
                    "source poison10 seconds amplifier0");require(service.count()==1,"foreign victim does not end author's trial");
            var source=new SauvojenMeditation.TrialTnt(t,base.owner.position());
            require(SauvojenMeditation.managed(source)&&SauvojenMeditation.cause(source)==t.cause&&source.getOwner()==base.owner,
                    "ephemeral native TNT preserves exact wild cause");
            service.stop();require(service.count()==0,"service stop closes all trials");
        }
        void independent(){
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(other!=null&&game.canUseGameplayItems(other),"second actual participant for concurrent trial ownership");
            var saved=new GameChecks.WandThirdPlayerState(other,base.mana);var indirect=new Attribution(game,List.of(other));boolean shift=other.isShiftKeyDown();
            int counter=base.mana.score(other,"sauvojen_wand_upgrade").get();
            try{
                var first=admit();first.remaining=600;service.phase(first);
                other.setItemInHand(InteractionHand.MAIN_HAND,first.stack.copy());other.setShiftKeyDown(true);
                require(service.start(other,InteractionHand.MAIN_HAND,other.getMainHandItem()),"second independent admission");
                var second=service.trial(other);second.remaining=600;service.phase(second);
                var secondActors=service.snapshot(other).stream().map(h->h.actor).toList();
                require(service.count()==2&&secondActors.size()==3,"simultaneous native helper families");
                require(secondActors.stream().noneMatch(Entity::isRemoved),"second native helper family is live before foreign cleanup: "+
                        secondActors.stream().map(e->e.getUUID()+" removed="+e.isRemoved()+" reason="+e.getRemovalReason()).toList());
                service.forget(base.owner);
                require(service.count()==1&&service.remaining(other)==600&&secondActors.stream().noneMatch(Entity::isRemoved),
                        "first cleanup leaves other trial counters and all native actors intact: trials="+service.count()+
                                " remaining="+service.remaining(other)+" actors="+secondActors.stream().map(e->e.getUUID()+" removed="+e.isRemoved()+" reason="+e.getRemovalReason()).toList());
                service.stop();require(secondActors.stream().allMatch(Entity::isRemoved),"stop closes remaining exact helpers");
            }finally{service.forget(other);saved.restore(game);indirect.close();other.setShiftKeyDown(shift);base.mana.score(other,"sauvojen_wand_upgrade").set(counter);}
        }
        void webGraph(){
            var t=admit();var center=base.owner.position().add(0,.5,12);
            for(var offset:List.of(new Vec3(-4,0,-4),new Vec3(4,0,-4),new Vec3(4,0,4),new Vec3(-4,0,4)))
                t.hazards.add(new SauvojenMeditation.Hazard(SauvojenMeditation.Kind.WEB,center.add(offset),Vec3.ZERO));
            require(service.webLines(t,false)==6,"four native web nodes issue six warning rays");
            var cow=new Cow(EntityTypes.COW,base.owner.level());cow.setPos(center.add(0,-.5,0));cow.setNoAi(true);cow.setNoGravity(true);
            require(base.owner.level().addFreshEntity(cow),"actual diagonal web victim");spawned.add(cow);float health=cow.getHealth();
            require(service.webLines(t,true)==6&&cow.getHealth()<health,"complete web graph hits central victim absent from nearest-node tree");
            t.hazards.clear();
            for(int i=0;i<16;i++)t.hazards.add(new SauvojenMeditation.Hazard(SauvojenMeditation.Kind.WEB,
                    center.add(Math.cos(i*Math.PI/8)*8,0,Math.sin(i*Math.PI/8)*8),Vec3.ZERO));
            try(var packets=new Swap(game,base.owner)){
                require(service.webLines(t,false)==120,"sixteen native web nodes issue120warning rays");
                require(t.webQueries==0&&t.webEdges==120&&t.webSamples>120,"warning traces complete graph without world entity queries");
                require(!packets.recording.particles.isEmpty()&&packets.recording.particles.size()<=2048,"bounded actual full-graph warning packet workload");
                for(var packet:packets.recording.particles){
                    var point=new Vec3(packet.x(),packet.y(),packet.z());boolean onRay=false;
                    for(int i=1;i<t.hazards.size();i++)for(int j=0;j<i;j++){
                        var from=t.hazards.get(i).at;var direction=t.hazards.get(j).at.subtract(from).normalize();
                        var delta=point.subtract(from);double along=delta.dot(direction);
                        if(along>=-1e-6&&delta.subtract(direction.scale(along)).lengthSqr()<1e-8&&Math.abs(along/.2-Math.rint(along/.2))<1e-5)onRay=true;
                    }
                    require(onRay,"bounded warning retains exact native .2-step source coordinates");
                }
                packets.recording.particles.clear();require(service.webLines(t,true)==120,"full damage graph retains all120 edges");
                require(t.webQueries==1&&t.webEdges==120&&t.webSamples>120,"full damage graph uses one native spatial query for all source collision samples");
                require(packets.recording.particles.size()<=1600&&packets.recording.particles.stream().mapToInt(p->p.count()).sum()<=4000,"bounded actual full-graph damage packets and client particle instances");
            }
            t.hazards.clear();cow.setHealth(cow.getMaxHealth());cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            var coincident=cow.position().add(0,.5,0);
            for(int i=0;i<2;i++)t.hazards.add(new SauvojenMeditation.Hazard(SauvojenMeditation.Kind.WEB,coincident,Vec3.ZERO));
            try(var packets=new Swap(game,base.owner)){
                require(service.webLines(t,false)==1&&t.webSamples==1&&packets.recording.particles.size()==1,"coincident native nodes retain their sole warning source sample");
                var warning=packets.recording.particles.getFirst();
                require(new Vec3(warning.x(),warning.y(),warning.z()).distanceToSqr(coincident)<1e-12,"coincident warning uses exact shared source coordinate");
                packets.recording.particles.clear();float before=cow.getHealth();
                require(service.webLines(t,true)==1&&t.webSamples==1&&cow.getHealth()<before,"coincident native nodes retain initial exact collision and damage");
                require(packets.recording.particles.size()==2&&packets.recording.particles.stream().allMatch(packet->new Vec3(packet.x(),packet.y(),packet.z()).distanceToSqr(coincident)<1e-12),"coincident damage retains both native particle types at shared coordinate");
            }service.stop();
        }
        void fear(){
            var psycho=game.items().psycho();
            require(psycho.effectValue(base.owner,"scared")==0,"fixture starts with clean canonical fear");
            var t=admit();t.remaining=160;float health=base.owner.getHealth();require(service.phase(t),"initial levitation phase");
            require(base.owner.getHealth()==health&&service.count()==1&&psycho.effectValue(base.owner,"scared")==0,"initial levitation neither damages nor afflicts fear");
            var picture=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,base.owner.level());
            picture.setWidth(4);picture.setHeight(4);picture.entityTags().add("scare_picture");
            picture.setPos(base.owner.getEyePosition().add(base.owner.getLookAngle().scale(5)).add(0,-2,0));
            require(base.owner.level().addFreshEntity(picture),"actual looked-at scare interaction");spawned.add(picture);
            try{
                require(service.looking(t),"actual native scare sight ray");
                base.mana.score(base.owner,"scared").set(10000);
                service.blind(t);require(psycho.effectValue(base.owner,"scared")==10&&base.owner.getHealth()==health&&service.count()==1,"first sight uses canonical fear10 despite obsolete raw score");
                for(int i=0;i<5;i++)psycho.tick();
                require(psycho.effectValue(base.owner,"scared")==5,"real Psycho five-tick decay applies to meditation fear");
                // Scared movement is native and random: restore only this fixture's actual sight geometry.
                picture.setPos(base.owner.getEyePosition().add(base.owner.getLookAngle().scale(5)).add(0,-2,0));
                psycho.afflict(base.owner,base.owner,90,7,0,0);
                service.blind(t);require(psycho.effectValue(base.owner,"scared")==105&&base.owner.getHealth()==health&&service.count()==1,"inclusive105 boundary remains harmless");
                base.owner.entityTags().add("sin_greed_active");
                try{service.blind(t);require(psycho.effectValue(base.owner,"scared")==105&&base.owner.getHealth()==health&&service.count()==1,"Greed rejects fear and threshold damage");}
                finally{base.owner.entityTags().remove("sin_greed_active");}
                boolean invulnerable=base.owner.getAbilities().invulnerable;base.owner.getAbilities().invulnerable=true;
                try{service.blind(t);require(psycho.effectValue(base.owner,"scared")==115&&service.count()==0,"only above105 source damage fails trial even when native hit is rejected");}
                finally{base.owner.getAbilities().invulnerable=invulnerable;}
                require(psycho.effectValue(base.owner,"lookback")==7,"fear lifecycle preserves unrelated custom status");
            }finally{picture.discard();psycho.clearStatus(base.owner,"scared");psycho.clearStatus(base.owner,"lookback");service.stop();}
        }
        void alliedBlast(){
            var other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null,"actual allied native blast participant");
            var state=new GameChecks.WandThirdPlayerState(other,base.mana);var indirect=new Attribution(game,List.of(other));
            var board=game.server().getScoreboard();var oldOwnerTeam=base.owner.getTeam();var oldOtherTeam=other.getTeam();
            require(board.getPlayerTeam("sw2_medit_tnt")==null,"exclusive native TNT team");var team=board.addPlayerTeam("sw2_medit_tnt");team.setAllowFriendlyFire(false);
            try{
                var t=admit();var at=base.owner.position().add(12,0,0);var c=BlockPos.containing(at);save(c.offset(-9,-9,-9),c.offset(9,9,9));
                board.addPlayerToTeam(base.owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
                other.getInventory().clearContent();other.removeAllEffects();other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                other.getAbilities().invulnerable=false;other.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
                other.setHealth(200);other.setAbsorptionAmount(0);other.setInvulnerableTime(0);other.damageCooldownTime=0;other.setNoGravity(true);
                other.teleportTo(other.level(),at.x+1,at.y,at.z,Set.of(),0,0,true);
                require(!base.owner.canHarmPlayer(other),"ordinary native team damage is disabled before trial TNT");
                float health=other.getHealth();service.blast(t,at);
                require(other.getHealth()<health,"actual native TrialTnt explosion damages allied player through WILD admission");
                require(other.getLastHurtByPlayer()==base.owner,"native allied TNT retains actual player author");
                try{var field=LbcIndirectKills.class.getDeclaredField("laterAttacks");field.setAccessible(true);
                    require(t.cause.equals(((Map<?,?>)field.get(game.items().indirectKills())).get(other)),
                            "actual allied TNT retains exact captured WILD indirect fall author/order");
                }catch(ReflectiveOperationException error){throw new IllegalStateException("Native trial TNT attribution",error);}
            }finally{
                service.stop();restore();board.removePlayerTeam(team);
                if(oldOwnerTeam!=null)board.addPlayerToTeam(base.owner.getScoreboardName(),oldOwnerTeam);
                if(oldOtherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOtherTeam);
                state.restore(game);indirect.close();
            }
        }
        @Override public void close(){
            try{service.stop();restore();for(var e:spawned){if(e instanceof net.minecraft.world.entity.LivingEntity living)game.items().indirectKills().clear(living);e.discard();}
                base.mana.score(base.owner,"sauvojen_wand_upgrade").set(savedCounter);base.mana.score(base.owner,"scared").set(savedFear);
            }finally{try{base.close();}finally{try{attribution.close();}finally{
                for(var chunk:forced)base.owner.level().setChunkForced(chunk.x(),chunk.z(),false);forced.clear();
            }}}
        }
    }
    public static void run(GameController game){
        try(var f=new Fixture(game)){f.admission();f.ruby();f.stone();f.fire();f.hazards();f.independent();f.webGraph();f.fear();f.alliedBlast();}
        SkyWars2.LOGGER.info("[check] PASS sauvojen-meditation (native stack/readiness,8725ruby,47x19x47 explicit stone,42510lava/20181air, complete6/120web rays+diagonal damage+bounded packets, canonical fear/decay/105 threshold/Greed, real allied WILD TNT damage/exact author, per-participant state and cleanup)");
    }
}
