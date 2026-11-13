package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real native criterion inputs, source loot/recipient projection and normally ticking typed actors. */
public final class ReactiveTrinketChecks {
    private ReactiveTrinketChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Reactive trinkets: "+why);}
    private static final List<String> NAMES=List.of("drojjevoe_odealo","mandoahuet","nether_plug","banangay","twilight_sparkle","hedgehog_gauntlet","poly_gem","glass_shard","psycho_soul");
    private static List<Item> items(){return List.of(LbcTrinkets.DROJJI,LbcTrinkets.MANDOAHUET,LbcTrinkets.NETHER_PLUG,LbcTrinkets.BANANGAY,LbcTrinkets.TWILIGHT_SPARKLE,LbcTrinkets.HEDGEHOG_GAUNTLET,LbcTrinkets.POLY_GEM,LbcTrinkets.GLASS_SHARD,LbcTrinkets.PSYCHO_SOUL);}
    public static void run(GameController game){start(game,false);}
    public static void runRewind(GameController game){start(game,true);}
    private static void start(GameController game,boolean rewind){require(running==null,"exclusive fixture");var next=new Fixture(game,rewind);running=next;try{next.begin();}catch(Throwable e){running=null;try{next.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}throw e;}}
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(game==f.game,"same controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} ({})",f.rewind?"reactive-trinkets-rewind":"reactive-trinkets",f.rewind?"nonempty actual coordinator native Prism/horse/spike/soul state replay":"nine source/default/recipient roundtrips; actual accepted/blocked/rejected/arrow damage, physical count provenance; temporary gem/Chef/projectile/vehicle identity, sequential Prism and fixed-duration horse/gravity spike outputs");}
        catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL "+(f.rewind?"reactive-trinkets-rewind":"reactive-trinkets"),e);}
    }
    /** Observes the actual native blocking primitive; preserves all vanilla behavior. */
    private static final class ShieldWitness extends Cow {
        float lastBlocked=Float.NaN;
        ShieldWitness(ServerLevel level){super(EntityTypes.COW,level);}
        @Override public float applyItemBlocking(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
            return lastBlocked=super.applyItemBlocking(level,source,amount);
        }
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean rewind;final ServerPlayer owner;final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved;final RegenerationBeltChecks.ScheduledClient client;
        final SauvojenMeditationChecks.Attribution attribution;final int limit;final Vec3 origin;
        final List<Entity> bodies=new ArrayList<>();final Set<UUID> original=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final Map<UUID,Integer> savedSteps=new LinkedHashMap<>(),savedRemaining=new LinkedHashMap<>();
        List<LbcReactiveOffense.Visual> savedSouls;Set<UUID> savedRays;Cow target,nearby,gemOriginal;LivingEntity gemBody;ShieldWitness shield;
        boolean gemRemovalReported;
        int phase,when,started;boolean stopped,closed,restoring;int expectedHue;UUID sheep;
        Fixture(GameController game,boolean rewind){
            this.game=game;this.rewind=rewind;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&owner!=null&&game.canUseGameplayItems(owner),"actual active TestAlpha");level=owner.level();
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot(),"exclusive real coordinator");
            require(game.items().clouds().acidCount()==0&&game.items().reactiveTrinkets().offense().visualCount()==0&&game.items().reactiveTrinkets().defense().snapshot().isEmpty()&&game.items().prism().pendingRays()==0,"exclusive reactive families");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner));limit=game.config().lbcTrinketLimit;
            level.getAllEntities().forEach(e->original.add(e.getUUID()));
            origin=new Vec3(owner.getX(),level.getMaxY()-8,owner.getZ());
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-4,-4,-4)),BlockPos.containing(origin.add(4,4,4))))require(level.hasChunkAt(pos)&&level.getBlockState(pos).isAir(),"resident air fixture volume "+pos);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);game.config().lbcTrinketLimit=99;
            owner.stopRiding();owner.stopUsingItem();owner.setGameMode(GameType.SURVIVAL);owner.removeAllEffects();
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=profile.lbcKit=profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            empty();move(origin);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,0);game.items().magic().inventoryChanged(owner);game.items().magic().flushCleansing();started=game.server().getTickCount();
        }
        void move(Vec3 p){require(owner.teleportTo(level,p.x,p.y,p.z,Set.of(),0,0,true),"actual native teleport");owner.setDeltaMovement(Vec3.ZERO);client.acknowledge();}
        void empty(){owner.stopUsingItem();owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);owner.getInventory().setSelectedSlot(0);game.items().reactiveTrinkets().inventoryChanged(owner);}
        ItemStack loot(int n){var result=game.lbc().rollTable(level,owner.position(),owner,"lbc:trinket/"+NAMES.get(n));require(result.size()==1&&result.getFirst().is(items().get(n)),"canonical native item "+NAMES.get(n));return result.getFirst();}
        void equip(Item item,int count){var stack=new ItemStack(item,count);owner.getInventory().setItem(9,stack);game.items().reactiveTrinkets().inventoryChanged(owner);}
        Cow cow(Vec3 at,boolean noAi){var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(noAi);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);require(level.addFreshEntity(c),"native witness publication");bodies.add(c);return c;}
        void health(LivingEntity e){e.setHealth(e.getMaxHealth());e.setInvulnerableTime(0);e.damageCooldownTime=0;e.hurtTime=0;e.hurtDuration=0;}
        void sourcePresentation(){
            var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)(Object)client.connection;
            for(int n=0;n<9;n++){
                var stack=loot(n);var defaults=items().get(n).getDefaultInstance();require(stack.getMaxStackSize()==99,"source max99 "+NAMES.get(n));
                for(var component:List.of(DataComponents.ITEM_MODEL,DataComponents.CUSTOM_NAME,DataComponents.LORE,DataComponents.CUSTOM_DATA,DataComponents.TOOLTIP_STYLE))require(Objects.equals(stack.get(component),defaults.get(component)),"source/default exact component "+NAMES.get(n)+" "+component);
                var name=stack.get(DataComponents.CUSTOM_NAME);require(name!=null&&!name.getStyle().isItalic()&&name.getStyle().getColor()!=null&&name.getStyle().getColor().getValue()==0xffffff,"source white nonitalic name");
                for(var line:stack.get(DataComponents.LORE).lines())require(!line.getStyle().isItalic()&&line.getStyle().getColor()!=null&&line.getStyle().getColor().getValue()==0xaaaaaa,"source gray nonitalic lore");
                net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
                    var proxy=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,provider.getPacketContext(),owner.registryAccess());
                    require(proxy.is(Items.REPEATING_COMMAND_BLOCK)&&Objects.equals(proxy.get(DataComponents.ITEM_MODEL),stack.get(DataComponents.ITEM_MODEL)),"recipient proxy and exact model");
                    require(ItemStack.matches(stack,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(proxy,provider.getPacketContext(),owner.registryAccess())),"authoritative recipient roundtrip");
                });
            }
            empty();var raw=new ItemStack(Items.PAPER,99);var tag=new net.minecraft.nbt.CompoundTag();tag.putString("trinket","drojji");raw.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            owner.getInventory().setItem(9,raw);owner.inventoryMenu.getCraftSlots().setItem(0,new ItemStack(LbcTrinkets.DROJJI,14));owner.containerMenu.setCarried(new ItemStack(LbcTrinkets.DROJJI,2));
            require(LbcReactiveTrinkets.count(owner,LbcTrinkets.DROJJI,"drojji")==115,"native physical raw99+craft14+cursor2 count-only provenance");
            require(raw.getCount()==99&&owner.inventoryMenu.getCraftSlots().getItem(0).getCount()==14&&owner.containerMenu.getCarried().getCount()==2,"count-only leaves all stacks intact");empty();
        }
        void offenseInventory(){empty();owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_SWORD));owner.getInventory().setItem(9,new ItemStack(LbcTrinkets.DROJJI,99));owner.getInventory().setItem(10,new ItemStack(LbcTrinkets.MANDOAHUET,99));owner.getInventory().setItem(11,new ItemStack(LbcTrinkets.PSYCHO_SOUL,99));}
        void begin(){
            sourcePresentation();
            if(rewind){beginRewind();return;}
            offenseInventory();shield=new ShieldWitness(level);shield.setPos(origin.add(0,0,2));shield.setNoAi(true);shield.setNoGravity(true);shield.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);shield.setHealth(1000);require(level.addFreshEntity(shield),"native blocking witness publication");bodies.add(shield);shield.setYRot(180);shield.setYHeadRot(180);shield.setYBodyRot(180);shield.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));shield.startUsingItem(InteractionHand.OFF_HAND);phase=0;when=game.server().getTickCount();
        }
        void actualOffense(){
            // Native26.3 applyItemBlocking uses HEAD yaw, independently of the body's YRot.
            shield.setPos(origin.add(0,0,2));shield.setYRot(180);shield.setYHeadRot(180);shield.setYBodyRot(180);move(origin);
            require(shield.isBlocking(),"actual native held shield reached blocking delay");
            var source=owner.damageSources().playerAttack(owner);var item=shield.getItemBlockingWith();
            require(item!=null&&item.is(Items.SHIELD),"native blocking item established");
            var facing=Vec3.directionFromRotation(0,shield.getYHeadRot());var toward=owner.position().subtract(shield.position()).multiply(1,0,1).normalize();
            double angle=Math.acos(Math.clamp(facing.dot(toward),-1d,1d));
            require(item.get(DataComponents.BLOCKS_ATTACKS).resolveBlockedDamage(source,4,angle)==4,"native shield component proves actual source is in blocking arc");
            // Independent positive control uses a real native damage attempt with no trinkets enrolled.
            empty();float before=shield.getHealth();int wear=item.getDamageValue();health(shield);
            boolean accepted=shield.hurtServer(level,source,4);
            String control="accepted="+accepted+" hp="+shield.getHealth()+" before="+before+" nativeBlocked="+shield.lastBlocked+" shieldWear="+item.getDamageValue()+" beforeWear="+wear+" cooldown="+shield.damageCooldownTime+" hurtTime="+shield.hurtTime+" headYaw="+shield.getYHeadRot()+" angle="+angle;
            SkyWars2.LOGGER.info("[check] reactive-trinkets native shield control {}",control);
            // BlocksAttacks.hurtBlockingItem damages equipment only for Player; a Cow has zero wear.
            require(!accepted&&shield.getHealth()==before&&shield.lastBlocked==4&&shield.damageCooldownTime==20&&shield.hurtTime==10&&item.getDamageValue()==wear,"native shield control fully blocks actual damage (vanilla nonplayer wear unchanged): "+control);
            offenseInventory();health(shield);owner.setDeltaMovement(Vec3.ZERO);shield.lastBlocked=Float.NaN;
            require(shield.getInvulnerableTime()==0&&shield.damageCooldownTime==0&&shield.hurtTime==0,"actual trinket shield attack starts outside native hit cooldown");
            owner.attack(shield);
            require(shield.lastBlocked>0&&shield.getHealth()==before&&game.items().clouds().acidCount()==0&&game.items().reactiveTrinkets().offense().visualCount()==0,"fully shield-blocked actual attack emits no outgoing trinket: nativeBlocked="+shield.lastBlocked+" hp="+shield.getHealth()+" before="+before+" acid="+game.items().clouds().acidCount()+" soul="+game.items().reactiveTrinkets().offense().visualCount()+" headYaw="+shield.getYHeadRot());
            shield.stopUsingItem();shield.discard();target=cow(origin.add(0,0,2),false);nearby=cow(origin.add(2,0,2),false);health(target);
            // The actual native hit/criterion invokes the service once; no direct outgoing call here.
            require(target.hurtServer(level,owner.damageSources().playerAttack(owner),2),"actual accepted player-authored native hit");
            require(target.getHealth()<1000&&target.isNoAi()&&!nearby.isNoAi(),"Psycho Soul disables only causal victim, never unrelated nearby mob");
            require(game.items().clouds().acidCount()==1&&game.items().reactiveTrinkets().offense().visualCount()==1,"one guaranteed acid and one soul visual from one native callback");
            require(!target.hurtServer(level,owner.damageSources().playerAttack(owner),1)&&game.items().clouds().acidCount()==1&&game.items().reactiveTrinkets().offense().visualCount()==1,"rejected lower cooldown hit cannot double-proc");
            var arrow=new Arrow(EntityTypes.ARROW,level);arrow.setOwner(owner);health(nearby);
            try{require(nearby.hurtServer(level,owner.damageSources().arrow(arrow,owner),1),"actual native arrow control accepted");require(!nearby.isNoAi()&&game.items().clouds().acidCount()==1,"arrow damage excluded from sword reactions");}finally{arrow.discard();}
            target.discard();nearby.discard();game.items().clouds().stop();game.items().reactiveTrinkets().offense().stop();empty();move(origin);
        }
        void beginGem(){
            var service=game.items().polymorph();
            require(!service.gemEligible(owner),"gem excludes player source");
            var dropped=new net.minecraft.world.entity.item.ItemEntity(level,origin.x,origin.y,origin.z,new ItemStack(Items.DIAMOND));
            require(!service.gemEligible(dropped),"gem excludes dropped-item source");dropped.discard();
            for(var type:List.of(EntityTypes.ARROW,EntityTypes.OAK_BOAT)){
                var source=type.create(level,EntitySpawnReason.COMMAND);require(source!=null,"native gem source construction");
                source.setPos(origin);source.setNoGravity(true);var motion=new Vec3(.3,.2,.1);source.setDeltaMovement(motion);
                require(level.addFreshEntity(source),"native gem source publication");bodies.add(source);
                var form=service.transformGem(source,game.items().indirectKills().coldCause(owner,false));
                require(form!=null&&service.suspended(source)&&service.remaining(source)==200&&!source.isRemoved(),"gem suspends projectile/vehicle for200ticks");
                service.forgetAuthor(owner.getUUID());
                require(!service.active(source)&&!source.isRemoved()&&!source.isInvisible()&&source.isNoGravity()&&source.getDeltaMovement().equals(motion),"gem restoration preserves nonliving identity/gravity/trajectory");source.discard();
            }
            target=cow(origin.add(0,-2,0),true);gemOriginal=target;
            var chef=game.items().wtf().spawn(owner,target);
            require(chef!=null,"native Chef source for temporary gem");bodies.add(chef);
            require(service.transformGem(chef,game.items().indirectKills().coldCause(owner,false))!=null&&service.suspended(chef),"gem includes Chef in reversible form");
            service.forgetAuthor(owner.getUUID());require(!chef.isRemoved()&&!service.active(chef),"temporary gem restores Chef without permanent erasure");chef.discard();
            // No wand is needed; the temporary gem must not enter Academy's gender branch.
            for(int slot=9;slot<20;slot++)owner.getInventory().setItem(slot,new ItemStack(LbcTrinkets.POLY_GEM,99));game.items().reactiveTrinkets().inventoryChanged(owner);
            phase=1;when=game.server().getTickCount();
        }
        void beginPrism(){
            empty();move(origin);target=cow(origin.add(0,-2,0),true);game.items().mana().score(owner,"h").set(0);int mana=game.items().mana().get(owner);
            require(level.canSeeSky(owner.blockPosition())&&game.items().prism().trinket(owner),"actual source feet+3 sky/below producer accepts");
            expectedHue=40;require(game.items().mana().get(owner)==mana,"free producer has no mana debit");phase=2;when=game.server().getTickCount();
        }
        void platform(){
            int y=level.getMaxY()-12;
            for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
                var pos=new BlockPos(x,y,z);require(level.hasChunkAt(pos),"source absolute-zero teleport columns resident");terrain.putIfAbsent(pos,game.maps().snapshot(level,pos));require(level.setBlock(pos,Blocks.STONE.defaultBlockState(),2)||level.getBlockState(pos).is(Blocks.STONE),"actual native source surface fixture");
            }
        }
        Vec3 flightStart,lastFlight;double flightDistance;UUID expiryHorse;int expiryPackets;
        void flatten(Packet<?> packet,List<Packet<?>> out){if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flatten(child,out);else out.add(packet);}
        void beginDefense(){
            target.discard();empty();move(origin);platform();equip(LbcTrinkets.BANANGAY,99);health(owner);
            require(owner.hurtServer(level,owner.damageSources().generic(),1),"actual pre-hit incoming banana damage accepted");client.acknowledge();
            require(Math.abs(owner.getX())<=6.5&&Math.abs(owner.getZ())<=6.5&&Math.abs(owner.getY()-(level.getMaxY()-10.5))<.001,"source absolute-zero six-range actual surface plus half block");
            empty();move(origin);equip(LbcTrinkets.HEDGEHOG_GAUNTLET,99);health(owner);var feet=owner.position();
            require(owner.hurtServer(level,owner.damageSources().generic(),1),"actual pre-hit incoming spike damage accepted");
            var spike=game.items().reactiveTrinkets().defense().snapshot().stream().filter(v->!v.horse()).findFirst().orElseThrow();require(spike.owner().equals(owner.getUUID())&&spike.body().position().distanceTo(feet)<1e-6,"native spike uses captured pre-hit feet and actual owner");
            empty();owner.setOnGround(false);game.items().reactiveTrinkets().defense().fall(owner,49,-150);
            var horse=game.items().reactiveTrinkets().defense().snapshot().stream().filter(LbcReactiveDefense.BodyView::horse).findFirst().orElseThrow();
            require(horse.body() instanceof Horse h&&!h.isBaby()&&Math.abs(h.getScale()-.5)<1e-6&&h.getHealth()==8&&h.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)&&owner.getRootVehicle()==h&&horse.remaining()==100,"small old-style armored horse/rider/effect/100 source counter");
            var mount=(Horse)horse.body();
            require(mount.getBodyArmorItem().is(net.minecraft.world.item.Items.DIAMOND_HORSE_ARMOR)&&mount.isSaddled(),"native armor and saddle retained");
            require(mount.getControllingPassenger()==null&&mount.isLocalInstanceAuthoritative()&&mount.getPassengers().size()==1&&owner.getVehicle()==mount.getFirstPassenger(),"server-authored flight, no conflicting vanilla rider prediction");
            owner.setYRot(90);owner.setXRot(0);flightStart=mount.position();lastFlight=flightStart;
            phase=3;when=game.server().getTickCount();
        }
        void beginRewind(){
            target=cow(origin.add(0,-2,0),false);offenseInventory();require(target.hurtServer(level,owner.damageSources().playerAttack(owner),1),"native soul queued before whole replay");empty();
            owner.setOnGround(false);game.items().reactiveTrinkets().defense().fall(owner,49,-150);game.items().reactiveTrinkets().defense().received(owner,0,99);
            game.items().mana().score(owner,"h").set(0);
            for(int n=0;n<128;n++)require(game.items().prism().trinket(owner),"accepted real queued Prism source "+n);
            require(game.items().prism().pendingRays()>0,"actual fair-budget Prism queue is nonempty");
            var ray=game.items().prism().snapshot().stream().filter(c->c.trinket&&!c.isRemoved()).findFirst().orElseThrow();
            int count=(int)game.items().prism().snapshot().stream().filter(c->c.trinket&&!c.isRemoved()).count();
            require(game.items().cloneSkills().copyNow(ray,15),"real installed x2 clone accepts typed trinket Prism");
            require(game.items().cloneSkills().copyNow(ray,17),"real installed x4 clone accepts typed trinket Prism");
            require(game.items().prism().snapshot().stream().filter(c->c.trinket&&!c.isRemoved()).count()==count+4,"one x2 plus three x4 native bodies");
            require(!game.items().cloneSkills().copyNow(ray,15)&&!game.items().cloneSkills().copyNow(ray,17),"same variant clone recursion rejected");
            game.items().timeStop().begin(level,10000);stopped=true;
            require(game.vanity().requestCapture(owner,false),"actual coordinator accepts nonempty reactive graph");phase=10;when=game.server().getTickCount();
        }
        boolean step(){
            client.acknowledge();require(game.server().getTickCount()-started<(rewind?500:750),"bounded normal server fixture including two full ten-second horse lifetimes");int elapsed=game.server().getTickCount()-when;
            if(gemBody!=null&&!gemRemovalReported&&(!gemBody.isAlive()||gemBody.isRemoved())){gemRemovalReported=true;SkyWars2.LOGGER.info("[check] reactive-trinkets gem retirement {}",gemDiagnostic());}
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            switch(phase){
                case 0->{if(elapsed<8)return false;actualOffense();beginGem();}
                case 1->{
                    if(!game.items().polymorph().active(target)){require(elapsed<30,"guaranteed legal eleven99-stack gem enrollment");return false;}
                    sheep=game.items().sheep().sheepViews().stream().filter(v->v.owner().equals(owner.getUUID())&&!original.contains(v.entity())).map(LbcSheep.SheepView::entity).findFirst().orElseThrow();
                    gemBody=(LivingEntity)level.getEntity(sheep);
                    require(level.getEntity(sheep)!=null&&game.items().polymorph().body(level.getEntity(sheep))&&!target.isRemoved()
                            &&game.items().polymorph().remaining(target)<=200,"gem keeps actual original suspended behind ten-second native sheep");
                    // Restoration is conditional on the sheep surviving. This timer witness shares
                    // the scene with a7HP Prism and4HP spike; isolate its survival, not production damage.
                    gemBody.setNoGravity(true);gemBody.setDeltaMovement(Vec3.ZERO);gemBody.setPermanentlyInvulnerable(true);
                    require(gemBody.isAlive()&&game.items().polymorph().remaining(gemOriginal)>0,"living timed restoration witness isolated from unrelated combat and falling");empty();beginPrism();
                }
                case 2->{
                    if(game.items().prism().pendingRays()>0){require(elapsed<60,"bounded source Prism completion");return false;}
                    require(game.items().mana().score(owner,"h").get()==expectedHue,"first complete ray precedes conditional upward hue+20 continuation");
                    require(level.getEntity(sheep)!=null&&!level.getEntity(sheep).isRemoved()&&game.items().polymorph().remaining(gemOriginal)>0,"gem sheep remains until timed restoration");beginDefense();
                }
                case 3->{
                    var horse=game.items().reactiveTrinkets().defense().snapshot().stream().filter(LbcReactiveDefense.BodyView::horse).findFirst().orElseThrow();
                    double moved=horse.body().position().distanceTo(lastFlight);flightDistance+=moved;lastFlight=horse.body().position();
                    require(((Horse)horse.body()).getControllingPassenger()==null,"client never owns flight movement");
                    if(elapsed>=3)require(moved>.5&&moved<1.3,"steady native flight displacement each tick: "+moved);
                    if(elapsed==8){owner.setYRot(0);owner.setXRot(-20);}
                    if(elapsed<16)return false;
                    require(flightDistance>12&&horse.body().getZ()>flightStart.z+4,"source-speed flight turns toward rider gaze without teleporting");
                    require(horse.remaining()<100&&horse.remaining()>=90,"fixed active two-tick horse clock");
                    owner.stopRiding();horse.body().setDeltaMovement(Vec3.ZERO);phase=4;when=game.server().getTickCount();
                }
                case 4->{if(elapsed<205)return false;
                    require(!game.items().polymorph().active(gemOriginal)&&!gemOriginal.isRemoved()&&!gemOriginal.isInvisible()&&level.getEntity(sheep)==null,"gem restores original identity after ten active seconds: "+gemDiagnostic());
                    var horse=game.items().reactiveTrinkets().defense().snapshot().stream().filter(LbcReactiveDefense.BodyView::horse).findFirst().orElseThrow();
                    require(horse.remaining()==0&&!horse.body().isRemoved()&&horse.body().getPassengers().isEmpty(),"unridden horse boost expires after ten seconds and removes control seat while horse remains");
                    move(origin);owner.setOnGround(false);game.items().reactiveTrinkets().defense().fall(owner,49,-150);
                    var occupied=game.items().reactiveTrinkets().defense().snapshot().stream().filter(v->v.horse()&&v.remaining()>0).findFirst().orElseThrow();
                    expiryHorse=occupied.body().getUUID();require(owner.getRootVehicle()==occupied.body(),"second native flight begins with the real rider");
                    owner.setXRot(-90);owner.setYRot(0);expiryPackets=client.packets().size();phase=5;when=game.server().getTickCount();return false;
                }
                case 5->{
                    require(elapsed<230,"natural occupied flight expires without artificial clock changes");
                    var horse=game.items().reactiveTrinkets().defense().snapshot().stream().filter(v->v.body().getUUID().equals(expiryHorse)).findFirst().orElseThrow();
                    if(horse.remaining()>0)return false;
                    require(!horse.body().isRemoved()&&owner.getVehicle()==horse.body()&&horse.body().getControllingPassenger()==owner,"native client riding controller receives the same surviving horse");
                    var packets=new ArrayList<Packet<?>>();for(var packet:client.packets().subList(expiryPackets,client.packets().size()))flatten(packet,packets);
                    int passenger=-1,motion=-1;Vec3 momentum=Vec3.ZERO;
                    for(int i=0;i<packets.size();i++){
                        var packet=packets.get(i);
                        if(packet instanceof ClientboundSetPassengersPacket changed&&changed.getVehicle()==horse.body().getId()
                                &&Arrays.equals(changed.getPassengers(),new int[]{owner.getId()}))passenger=i;
                        if(passenger>=0&&packet instanceof ClientboundSetEntityMotionPacket changed&&changed.id()==horse.body().getId()){
                            motion=i;momentum=changed.movement();break;
                        }
                    }
                    require(passenger>=0&&motion>passenger&&momentum.y>.5&&momentum.length()<1.2,"actual wire transfers passenger ownership before preserving the existing upward impulse: "+momentum);
                    return true;
                }
                case 10->{
                    if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;
                    var rays=game.items().prism().snapshot().stream().filter(c->c.trinket&&!c.isRemoved()).toList();require(!rays.isEmpty(),"captured nonempty typed Prism bodies");
                    savedRays=new LinkedHashSet<>();for(var ray:rays){savedRays.add(ray.getUUID());savedSteps.put(ray.getUUID(),ray.steps);}
                    for(var body:game.items().reactiveTrinkets().defense().snapshot())savedRemaining.put(body.body().getUUID(),body.remaining());require(savedRemaining.size()>=2,"captured actual horse and spike");
                    savedSouls=game.items().reactiveTrinkets().offense().visualSnapshot();require(!savedSouls.isEmpty(),"captured nonempty private soul queue");
                    game.items().prism().stop();game.items().reactiveTrinkets().defense().stop();game.items().reactiveTrinkets().offense().stop();
                    game.items().mana().set(owner,777);require(game.vanity().requestRestoreForCheck(),"actual nonempty coordinator restore request");restoring=true;phase=11;when=game.server().getTickCount();
                }
                case 11->{
                    if(game.vanity().busy())return false;
                    for(var id:savedRays){require(level.getEntity(id) instanceof LbcPrism.Charge,"native queued body restored");var ray=(LbcPrism.Charge)level.getEntity(id);require(ray.trinket&&ray.steps==savedSteps.get(id)&&ray.producer.equals(owner.getUUID()),"typed sample cursor/continuation owner restored");}
                    var restored=new LinkedHashMap<UUID,Integer>();for(var body:game.items().reactiveTrinkets().defense().snapshot())restored.put(body.body().getUUID(),body.remaining());require(restored.equals(savedRemaining),"native horse/spike private lifetime map restored");
                    require(game.items().reactiveTrinkets().offense().visualSnapshot().equals(savedSouls),"actual queued soul position/yaw/age restored");require(game.items().timeStop().stopped(level),"whole coordinator restored source pause boundary");return true;
                }
                default->throw new IllegalStateException("Unknown reactive fixture phase "+phase);
            }
            return false;
        }
        String gemDiagnostic(){return "active="+game.items().polymorph().active(gemOriginal)+" remaining="+game.items().polymorph().remaining(gemOriginal)
                +" originalRemoved="+gemOriginal.isRemoved()+" originalInvisible="+gemOriginal.isInvisible()+" originalPos="+gemOriginal.position()
                +" sheepAlive="+(gemBody!=null&&gemBody.isAlive())+" sheepRemoved="+(gemBody!=null&&gemBody.isRemoved())
                +" sheepHP="+(gemBody==null?null:gemBody.getHealth())+" sheepPos="+(gemBody==null?null:gemBody.position())
                +" sheepReason="+(gemBody==null?null:gemBody.getRemovalReason())+" sheepLastDamage="+(gemBody==null?null:gemBody.getLastDamageSource());}
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{if(rewind)game.vanity().stop();},()->{if(stopped)game.items().timeStop().end(level);},()->{owner.stopRiding();game.items().polymorph().forgetAuthor(owner.getUUID());game.items().reactiveTrinkets().stop();game.items().prism().stop();game.items().clouds().stop();},()->{var remove=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!original.contains(entity.getUUID())&&(bodies.contains(entity)||entity instanceof LbcPrism.Charge||LbcReactiveDefense.managed(entity)||entity.entityTags().contains("magicalsheep")))remove.add(entity);remove.forEach(Entity::discard);},()->terrain.forEach((pos,state)->game.maps().restoreSnapshot(level,pos,state,2)),()->{game.config().lbcTrinketLimit=limit;saved.restoreAttributes();saved.restore(game);saved.restoreUse();game.items().reactiveTrinkets().inventoryChanged(owner);},client::close,attribution::close))try{action.run();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
            if(failure!=null)throw new IllegalStateException("Reactive fixture cleanup",failure);
        }
    }
}
