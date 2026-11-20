package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native Tesla input, shared electric geometry, cooperative beams, trial and match lifecycle. */
public final class TeslaChecks {
    private TeslaChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Tesla: "+why);}
    private static Fixture running;
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;
        final GameChecks.WandThirdPlayerState saved,otherSaved;
        final Vec3 at=new Vec3(-240.5,240,-240.5);
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Long> forced=new HashSet<>();
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final Map<net.minecraft.stats.Stat<Identifier>,Integer> stats=new LinkedHashMap<>();
        final Map<Identifier,Set<String>> advancements=new LinkedHashMap<>();
        final Set<String> unlocks;final boolean offhand;final int skill,trinket,challengeProgress;
        final net.minecraft.world.scores.PlayerTeam team;
        boolean warmed;int warmTicks,phase;long last=Long.MIN_VALUE;
        ItemStack trial;net.minecraft.world.entity.Interaction panel;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null,"two active participants");
            saved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());otherSaved=new GameChecks.WandThirdPlayerState(other,game.items().mana());offhand=game.config().offhandWands;
            var p=game.players().get(owner.getUUID());skill=p.lbcSkill;trinket=p.lbcTrinket;challengeProgress=p.lbcChallengeProgress;unlocks=new HashSet<>(p.lbcUnlocks);
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=net.minecraft.stats.Stats.CUSTOM.get(id);stats.put(s,owner.getStats().getValue(s));}
            for(String key:List.of("lbc:true_advancements/staffs/tesla","lbc:true_advancements/legends/academy/tesla")){
                var id=Identifier.parse(key);var advancement=game.server().getAdvancements().get(id);if(advancement!=null){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(id,done);}
            }
            owner.level().getAllEntities().forEach(before::add);team=game.server().getScoreboard().addPlayerTeam("tesla_fixture");team.setAllowFriendlyFire(false);
        }
        void init(){
            for(int x=-16;x<=-14;x++)for(int z=-16;z<=-8;z++){long k=net.minecraft.world.level.ChunkPos.pack(x,z);if(!owner.level().getForceLoadedChunks().contains(k)){forced.add(k);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);}
            owner.closeContainer();owner.getInventory().clearContent();owner.removeAllEffects();owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);owner.setShiftKeyDown(false);
            owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);other.setNoGravity(true);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;game.config().offhandWands=true;
            game.items().mana().set(owner,100000);game.items().mana().noMagic(owner,0);
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);owner.getFoodData().setFoodLevel(17);
            game.server().getScoreboard().addPlayerToTeam(owner.getScoreboardName(),team);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
        }
        boolean warm(){if(warmed)return true;require(++warmTicks<=600,"resident entity ticking fixture");for(long k:forced)if(!owner.level().areEntitiesLoaded(k))return false;warmed=true;return true;}
        ItemStack loot(String leaf){var list=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/"+leaf);require(list.size()==1,"one item "+leaf);return list.getFirst();}
        int flag(ItemStack s,String key){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(key,0);}
        void flag(ItemStack s,String key,int value){var d=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();d.putInt(key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
        boolean use(ItemStack s,InteractionHand hand){owner.setItemInHand(hand,s);return owner.gameMode.useItem(owner,owner.level(),s,hand).consumesAction();}

        List<LbcWands.ElectroTrace> shots(){return game.items().wands().electroSnapshot().stream().filter(t->t.owner==owner&&t.age<t.lifetime).toList();}
        void clear(){for(var t:shots())t.age=t.lifetime;game.items().wands().tick();}
        static final class ProbeCow extends Cow {
            int direct,conducting;
            ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
                if(source.is(LbcDamage.ELECTRO_BOLT))direct++;if(source.is(LbcDamage.ELECTRO_CONDUCTION))conducting++;
                return super.hurtServer(level,source,amount);
            }
        }
        ProbeCow cow(Vec3 point){var c=new ProbeCow(owner.level());c.setPos(point);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);owner.level().addFreshEntity(c);return c;}
        void block(BlockPos p,net.minecraft.world.level.block.Block type){blocks.putIfAbsent(p,game.maps().snapshot(owner.level(),p));owner.level().setBlock(p,type.defaultBlockState(),2);}
        void presentation(){
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            for(boolean upgraded:new boolean[]{false,true}){
                var s=loot(upgraded?"academy_tesla_wand":"tesla_wand");
                require(s.is(upgraded?LbcWands.ACADEMY_TESLA:LbcWands.TESLA)&&flag(s,"lightningwand")==1&&flag(s,"staff")==1&&flag(s,"cyberset")==1,"native Tesla identity/source flags");
                require(Identifier.parse("lbc:lightningwand").equals(s.get(DataComponents.ITEM_MODEL))&&Identifier.parse(upgraded?"lbc:tier_a":"lbc:tier_b").equals(s.get(DataComponents.TOOLTIP_STYLE))&&s.get(DataComponents.LORE).lines().size()==(upgraded?12:11),"B/A source model/lore");
                require(s.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("lightning_wand")&&!s.getHoverName().getStyle().isItalic(),"source translated name/style");
                require(flag(s,"upgraded")== (upgraded?1:0),"source Academy flag");
            }
            game.items().mana().set(owner,1199);require(!LbcTesla.use(game,owner,InteractionHand.MAIN_HAND,loot("tesla_wand"),false)&&game.items().mana().get(owner)==1199,"insufficient mana unchanged");
            var a=loot("tesla_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,a);owner.setItemInHand(InteractionHand.OFF_HAND,a.copy());game.items().mana().set(owner,100000);
            require(owner.gameMode.useItem(owner,owner.level(),a,InteractionHand.MAIN_HAND).consumesAction()&&shots().size()==8&&game.items().mana().get(owner)==97600,"native simultaneous hands, four arcs per hand and2400 mana");
            var lifetimes=shots().stream().map(t->t.lifetime).sorted().toList();require(lifetimes.equals(List.of(30,30,35,35,40,40,45,45)),"source four lifetimes per volley");
            var offsets=shots().stream().map(t->Math.round(t.position.subtract(owner.getEyePosition()).dot(owner.getLookAngle())*100)).sorted().toList();
            require(offsets.equals(List.of(-3L,-2L,-1L,0L,47L,48L,49L,50L)),"ordinary offhand source origin half-block forward");
            clear();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var s=loot("academy_tesla_wand");game.items().mana().set(owner,-10000);owner.setShiftKeyDown(true);
            for(int mode:new int[]{2,3,1})require(use(s,InteractionHand.MAIN_HAND)&&flag(s,"mode")==mode&&game.items().mana().get(owner)==-10000,"free mode changes at negative mana");
            owner.setShiftKeyDown(false);game.items().mana().set(owner,100000);
        }
        void geometry(){
            var center=at.add(0,3,8);var victim=cow(center.add(0,-.6,1));game.server().getScoreboard().addPlayerToTeam(victim.getScoreboardName(),team);
            other.teleportTo(other.level(),center.x,center.y-.6,center.z+1,Set.of(),0,0,true);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);other.setHealth(1000);other.setAbsorptionAmount(0);
            require(use(loot("tesla_wand"),InteractionHand.OFF_HAND),"actual offhand Tesla volley");var trace=shots().getFirst();shots().stream().filter(t->t!=trace).forEach(t->t.age=t.lifetime);trace.position=center;trace.yaw=0;trace.pitch=0;
            try (var caster = new RegenerationBeltChecks.ScheduledClient(game, owner);
                 var reflector = new RegenerationBeltChecks.ScheduledClient(game, other)) {
                game.items().wands().tick();
                require(Math.abs(trace.position.distanceTo(center)-2)<1e-6&&victim.getHealth()<1000
                                &&victim.direct>=2&&victim.getKillCredit()==owner&&other.getHealth()==1000,
                        "2-block native sampled arc hits friendly mob repeatedly, protects allied player and credits caster");
                require(contactSounds(caster)==1&&contactSounds(reflector)==0,
                        "sampled Tesla contact sends one private native hit sound to the caster");
                victim.discard();
                var reflectedVictim=cow(center.add(0,-.6,1));
                try {
                    game.items().wands().reflectElectro(other);
                    require(trace.owner==other,"electro shield reflection transfers owner on shared trace");
                    trace.yaw=180;trace.pitch=0;
                    game.items().wands().tick();
                    require(reflectedVictim.getHealth()<1000&&contactSounds(caster)==1&&contactSounds(reflector)==1,
                            "reflected Tesla hit sends one sound to the current shooter and none to the original caster");
                } finally { reflectedVictim.discard(); }
            }
            trace.owner=owner;trace.position=center;trace.age=29;game.items().wands().tick();require(!shots().contains(trace),"ordinary arc expires at30 source ticks");clear();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            // A real vanilla shield clips Tesla at the same shared sphere as other shield_blockable arcs.
            game.server().getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);other.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));other.setYRot(180);other.startUsingItem(InteractionHand.MAIN_HAND);for(int i=0;i<6;i++)other.doTick();require(other.isBlocking(),"native shield blocking delay");
            require(use(loot("tesla_wand"),InteractionHand.MAIN_HAND),"shield volley");var blocked=shots().getFirst();blocked.position=center.add(0,0,-2);blocked.yaw=0;blocked.pitch=0;game.items().wands().tick();require(!shots().contains(blocked)&&blocked.position.distanceTo(center.add(0,0,-2))<2,"native shield clips moving Tesla arc");clear();other.stopUsingItem();other.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);game.server().getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            other.teleportTo(other.level(),owner.getX()+1,owner.getY(),owner.getZ(),Set.of(),0,0,true);owner.setHealth(1000);other.setHealth(1000);owner.setInvulnerableTime(0);other.setInvulnerableTime(0);
            game.items().wands().teslaConduction(new LbcWands.ElectroTrace(owner,owner.position(),game.items().epoch()),owner.position());
            require(owner.getHealth()==999&&other.getHealth()==999,"conduction reaches caster and allied player even with team friendly fire disabled; owner="+owner.getHealth()+" ally="+other.getHealth()+" armor="+owner.getAttributeValue(Attributes.ARMOR)+"/"+other.getAttributeValue(Attributes.ARMOR)+" cooldown="+owner.damageCooldownTime+"/"+other.damageCooldownTime+" invulnerable="+owner.getInvulnerableTime()+"/"+other.getInvulnerableTime()+" loaded="+owner.connection.hasClientLoaded()+"/"+other.connection.hasClientLoaded()+" changing="+owner.isChangingDimension()+"/"+other.isChangingDimension()+" effects="+owner.getActiveEffects()+"/"+other.getActiveEffects());
            other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);
        }
        private static long contactSounds(RegenerationBeltChecks.ScheduledClient client) {
            return client.packets().stream().filter(packet ->
                    packet instanceof net.minecraft.network.protocol.game.ClientboundGameEventPacket event
                            && event.getEvent() == net.minecraft.network.protocol.game.ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND)
                    .count();
        }
        void cadence(){
            clear();
            var start=at.add(0,6,30);var victim=cow(start.add(0,-.6,1));
            var beside=cow(start.add(.8,-.6,1));
            try{
                var bolt=new net.minecraft.world.damagesource.DamageSource(owner.level().registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),owner);
                require(bolt.is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN),"loaded direct electric type bypasses native hurt cooldown");
                int samples=0,lineSamples=0;
                for(int i=0;i<=10;i++){
                    var point=start.add(0,0,i*.2);
                    if(victim.getBoundingBox().contains(point))lineSamples++;
                    if(victim.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(point,point.add(1,1,1))))samples++;
                }
                require(samples>lineSamples&&lineSamples>1,"legacy pulse count exceeds precise body samples");
                require(beside.getBoundingBox().clip(start,start.add(0,0,2)).isEmpty(),"nearby legacy cell target misses exact line");
                game.items().wands().teslaVolley(owner,InteractionHand.MAIN_HAND,loot("tesla_wand"),false);
                var volley=shots();require(volley.size()==4,"four independent moving electric sources");
                for(int pass=1;pass<=2;pass++){
                    for(var trace:volley){trace.position=start;trace.yaw=0;trace.pitch=0;}
                    game.items().wands().tick();
                    int expected=pass*4*samples;
                    require(victim.direct==expected&&victim.getHealth()==1000-expected*.5f,
                            "each exact-intersected arc retains every legacy pulse through native cooldown: pass="+pass+" attempts="+victim.direct+" expected="+expected+" hp="+victim.getHealth());
                    require(victim.damageCooldownTime>10,"regression exercises active native hurt cooldown");
                    require(beside.direct==0&&beside.getHealth()==1000,"legacy pulse count never widens the admission line");
                    int expectedAge=pass;
                    require(volley.stream().allMatch(t->t.age==expectedAge&&Math.abs(t.position.distanceTo(start)-2)<1e-6),
                            "all moving electric sources advance two blocks each working tick");
                }
            }finally{clear();victim.discard();beside.discard();}
        }
        void precisePulseGeometry(){
            var center=at.add(0,8,34);var victim=cow(center.add(0,-.6,0));
            try{
                var damage=new net.minecraft.world.damagesource.DamageSource(owner.level().registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.ELECTRO_BOLT),owner);
                var box=victim.getBoundingBox();
                for(var direction:List.of(new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(0,1,0),new Vec3(0,-1,0),
                        new Vec3(0,0,1),new Vec3(0,0,-1),new Vec3(1,.2,1).normalize(),new Vec3(-1,-.2,-1).normalize())){
                    var from=center.subtract(direction.scale(2));var to=center.add(direction.scale(2));int expected=0;
                    require(box.clip(from,to).isPresent(),"precise cross-body fixture");
                    for(int i=0;i<=20;i++){
                        var point=from.lerp(to,i*.2/from.distanceTo(to));
                        if(box.intersects(new net.minecraft.world.phys.AABB(point,point.add(1,1,1))))expected++;
                    }
                    victim.setHealth(1000);victim.direct=0;
                    LbcDamage.hurtSegment(owner.level(),damage,from,to,Vec3.ZERO,.2,.5f,2,e->e==victim,false);
                    require(victim.direct==expected&&victim.getHealth()==1000-expected*.5f,"directional legacy pulse multiplicity after exact contact: "+direction);
                }
                // A finite segment stops short of the body, even if its old unit cells overlap it.
                victim.direct=0;victim.setHealth(1000);
                LbcDamage.hurtSegment(owner.level(),damage,center.add(0,0,-2),center.add(0,0,-.6),Vec3.ZERO,.2,.5f,2,e->e==victim,false);
                require(victim.direct==0&&victim.getHealth()==1000,"no damage beyond the exact segment endpoint");
            }finally{victim.discard();}
        }
        void instantConduction(){
            owner.setYRot(0);owner.setXRot(0);var eye=owner.getEyePosition();var pos=BlockPos.containing(eye);
            var victim=cow(eye.add(4,-.6,0));var technical=cow(eye.add(-4,-.6,0));technical.entityTags().add("strict_map_object");
            var invulnerable=cow(eye.add(3,-.6,0));invulnerable.setPermanentlyInvulnerable(true);
            other.teleportTo(other.level(),eye.x-3,at.y,eye.z,Set.of(),0,0,true);other.setHealth(1000);other.setAbsorptionAmount(0);owner.setHealth(1000);
            block(pos,Blocks.IRON_BLOCK);var work=new LbcTesla.Instant(game,owner);Vec3 start=work.point;float ownHp=owner.getHealth(),allyHp=other.getHealth();
            require(!work.step()&&work.beam==0&&work.point.distanceToSqr(start)>0,"instant ray passes tagged metal without terminating");
            require(victim.conducting==1&&victim.getHealth()==999&&victim.getKillCredit()==owner,"one shared conduction pulse per instant segment with native owner");
            require(owner.getHealth()==ownHp-1&&other.getHealth()==allyHp-1,"instant conduction remains WILD for caster/allied player");
            require(technical.getHealth()==1000&&invulnerable.getHealth()==1000,"instant shared conduction preserves protected roles/native rejected damage");
            block(pos,Blocks.WATER);work=new LbcTesla.Instant(game,owner);int pulses=victim.conducting;require(!work.step()&&work.beam==0&&victim.conducting==pulses+1,"water shares conductor continuation and one segment pulse");
            block(pos,Blocks.STONE);work=new LbcTesla.Instant(game,owner);pulses=victim.conducting;require(!work.step()&&work.beam==1&&victim.conducting==pulses,"nonconductor terminates beam without conduction");
            block(pos,Blocks.AIR);victim.discard();technical.discard();invulnerable.discard();other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);
        }
        void instantContacts(){
            owner.setYRot(0);owner.setXRot(0);
            var eye=owner.getEyePosition();
            var start=BlockPos.containing(eye);
            for(var cell:BlockPos.betweenClosed(start.offset(-1,-1,0),start.offset(1,1,2)))
                block(cell.immutable(),Blocks.AIR);
            var observerPosition=other.position();
            other.setPos(eye.add(0,-.6,-2));
            try(var caster=new RegenerationBeltChecks.ScheduledClient(game,owner);
                var observer=new RegenerationBeltChecks.ScheduledClient(game,other)){
                instantContact(eye,0,true,caster,observer);
                instantContact(eye,.20,true,caster,observer);
                instantContact(eye,.30,false,caster,observer);
                // Move the centerline next to the block edge without crossing it.
                var edge=new Vec3(start.getX()+.9,eye.y,eye.z);
                block(start.east(),Blocks.OBSIDIAN);
                block(start.east().south(),Blocks.OBSIDIAN);
                instantContact(edge,.20,false,caster,observer);
                block(start.east(),Blocks.AIR);
                block(start.east().south(),Blocks.AIR);
            }finally{other.setPos(observerPosition);}
        }
        void instantContact(Vec3 origin,double gap,boolean hit,RegenerationBeltChecks.ScheduledClient caster,
                RegenerationBeltChecks.ScheduledClient observer){
            var victim=cow(origin.add(0,-.6,.85));
            if(gap>0)victim.setPos(victim.position().add(victim.getBbWidth()*.5+gap,0,0));
            var random=owner.getRandom();var savedRandom=MatchRandomState.capture(random);
            try{
                random.setSeed(0);
                var work=new LbcTesla.Instant(game,owner);work.point=origin;
                long casterBefore=contactSounds(caster),observerBefore=contactSounds(observer);
                require(!work.step()&&work.beam==0&&work.sample==8
                                &&work.point.distanceToSqr(origin.add(0,0,2))<1e-9,
                        "one straight native Academy segment ran at gap "+gap);
                if(gap==0){
                    int expected=0;var box=victim.getBoundingBox();
                    for(int i=0;i<work.sample;i++){
                        var point=origin.add(0,0,i*.25);
                        if(box.intersects(new net.minecraft.world.phys.AABB(point,point.add(1,1,1))))expected++;
                    }
                    require(expected>1&&victim.direct==expected&&victim.getHealth()==1000-expected*.5f,
                            "instant centerline retains exact legacy sample multiplicity: "+expected+" versus "+victim.direct);
                }else require((victim.direct>0)==hit&&(victim.getHealth()<1000)==hit,
                        "instant arrow-width contact at gap "+gap+" expected hit="+hit+", pulses="+victim.direct);
                require(contactSounds(caster)==casterBefore+(hit?1:0)&&contactSounds(observer)==observerBefore,
                        "instant segment sends one accepted-contact cue only to the shooter at gap "+gap);
            }finally{savedRandom.restore(random);victim.discard();}
        }
        void chain(){
            var s=loot("academy_tesla_wand");flag(s,"mode",3);require(use(s,InteractionHand.MAIN_HAND)&&shots().size()==1,"one native chain carrier");var trace=shots().getFirst();require(trace.teslaChain&&trace.lifetime==150,"source chain flag/lifetime150");
            var center=at.add(0,4,12);trace.position=center;trace.yaw=0;trace.pitch=0;
            var near=cow(center.add(1,0,4));var ring=cow(center.add(7,0,4));
            game.items().wands().tick();require(Math.abs(trace.position.distanceTo(center)-4)<1e-6&&trace.yaw==0,"chain moves four blocks and near target suppresses turn");
            Vec3 p=trace.position;for(int i=0;i<4;i++)game.items().wands().tick();require(trace.position.equals(p),"source flight only every5 ticks");near.discard();var friendly=cow(center.add(2,0,4));game.server().getScoreboard().addPlayerToTeam(friendly.getScoreboardName(),team);
            trace.position=center;trace.yaw=0;trace.pitch=0;other.teleportTo(other.level(),center.x-6,center.y,center.z+4,Set.of(),0,0,true);
            game.items().wands().tick();require(trace.yaw< -80&&trace.yaw> -100,"chain seeks enemy ring and ignores closer allied player/friendly mob");
            ring.discard();friendly.discard();trace.age=149;game.items().wands().tick();require(!shots().contains(trace),"chain expires at150 actual ticks");clear();other.teleportTo(other.level(),at.x+30,at.y,at.z,Set.of(),0,0,true);
        }
        void familyArcContacts() {
            var level = owner.level();
            BlockPos cell = BlockPos.containing(at).offset(2, 5, 24);
            Vec3 from = Vec3.atCenterOf(cell).add(0, 0, -1.25);
            var aim = new Vec3(0, 0, 1);
            block(cell, LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI));
            block(cell.north(), Blocks.STONE);
            try {
                game.items().wands().electroRayAt(owner, from, aim, null);
                require(yellowDrops(from).isEmpty(), "a solid block prevents a reaction behind it");
                block(cell.north(), Blocks.AIR);
                var trace = new LbcWands.ElectroTrace(owner, from, game.items().epoch());
                trace.yaw=0;trace.pitch=0;
                game.items().wands().higherElectro(trace);
                game.items().wands().tick();
                var loot = yellowDrops(from);
                require(!shots().contains(trace) && loot.size()==1 && loot.getFirst().getItem().getCount()==1,
                        "moving electrical arc stops and drops exactly one source Yellow Lightning");
                loot.forEach(Entity::discard);
                require(LbcLuckyBlocks.kind(level.getBlockState(cell))==LbcLuckyBlocks.Kind.LOLI,
                        "arc contact preserves the actual Lucky Block Loli");

                block(cell, Blocks.MUSHROOM_STEM);
                game.items().wands().electroRayAt(owner, from, aim, null);
                require(yellowDrops(from).isEmpty(), "ordinary mushroom stem has no arc reaction");
                block(cell, LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI));
                var work = new LbcTesla.Instant(game, owner);
                work.currentYaw=0;work.currentPitch=0;work.point = from;
                for(int attempt=0;attempt<100&&work.beam==0;attempt++)work.step();
                require(work.beam==1 && yellowDrops(from).size()==1,
                        "Academy instant Tesla shares native block reaction");
                yellowDrops(from).forEach(Entity::discard);

                var mother = EntityTypes.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                require(mother != null, "native Mother actor");
                mother.entityTags().addAll(Set.of("mom","skywars2.family"));
                game.items().markCurrentItemEpoch(mother);
                mother.setPos(from.add(0, -.5, .45));
                require(level.addFreshEntity(mother) && game.items().familyDrops().snapshot().contains(mother),
                        "resident native Mother uses the family actor index");
                try {
                    game.items().wands().electroRayAt(owner, from, aim, null);
                    loot = yellowDrops(from);
                    require(loot.size()==1 && loot.getFirst().getZ() < cell.getZ(),
                            "first Mother intercepts instant Electro before the Loli block");
                    loot.forEach(Entity::discard);
                    require(mother.isAlive(), "arc preserves Mother");
                } finally {
                    mother.discard();
                }
            } finally {
                yellowDrops(from).forEach(Entity::discard);
                block(cell, Blocks.AIR);
            }
        }

        private LbcWands.ElectroTrace fireTowardGreed(Vec3 from, List<LbcWands.ElectroTrace> fixtureShots) {
            require(use(loot("tesla_wand"), InteractionHand.MAIN_HAND), "actual moving Tesla cast");
            var volley = shots();
            require(volley.size() == 4, "each test press creates the source four-arc volley");
            fixtureShots.addAll(volley);
            var shot = volley.getFirst();
            for (var sibling : volley) if (sibling != shot) sibling.age = sibling.lifetime;
            shot.position = from;
            shot.yaw = -45;
            shot.pitch = 0;
            game.items().wands().tick();
            return shot;
        }

        void greedTerrainOrder() {
            var level = owner.level();
            var wands = game.items().wands();
            var cell = BlockPos.containing(at).offset(3, 7, 28);
            var wallFace = new Vec3(cell.getX(), cell.getY() + .5, cell.getZ() + .94);
            var direction = Vec3.directionFromRotation(0, -45);
            var from = wallFace.subtract(direction.scale(.65));
            var reflector = wallFace.add(direction.scale(1.1)).add(0, -.55, 0);
            var oldPosition = other.position();
            float oldYaw = other.getYRot(), oldPitch = other.getXRot();
            var fixtureShots = new ArrayList<LbcWands.ElectroTrace>();
            var loli = LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI);
            var mother = EntityTypes.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(mother != null && !LbcGreed.active(other) && yellowDrops(from).isEmpty(),
                    "isolated actual family outputs and reflector");
            require(java.util.stream.IntStream.rangeClosed(0, 10)
                            .noneMatch(i -> BlockPos.containing(from.add(direction.scale(i * .2))).equals(cell)),
                    "terrain contact lies between the original .2-block particle samples");
            try {
                block(cell, Blocks.AIR);
                other.teleportTo(level, reflector.x, reflector.y, reflector.z, Set.of(), 0, 0, true);
                other.entityTags().add("sin_greed_active");
                var open = fireTowardGreed(from, fixtureShots);
                require(open.owner == other && wands.hasElectroTrace(open)
                                && Math.abs(open.yaw - 135) < 1e-5 && yellowDrops(from).isEmpty(),
                        "unobstructed Greed reverses the original moving arc");
                open.age = open.lifetime;
                wands.tick();

                block(cell, Blocks.STONE);
                var stopped = fireTowardGreed(from, fixtureShots);
                require(!wands.hasElectroTrace(stopped) && stopped.owner == owner
                                && stopped.position.distanceToSqr(wallFace) < 1e-10 && yellowDrops(from).isEmpty(),
                        "the exact solid face terminates the arc before Greed behind it, even between samples");

                block(cell, loli);
                var reward = fireTowardGreed(from, fixtureShots);
                var items = yellowDrops(from);
                require(!wands.hasElectroTrace(reward) && reward.owner == owner
                                && reward.position.distanceToSqr(wallFace) < 1e-10
                                && items.size() == 1 && items.getFirst().getItem().getCount() == 1
                                && items.getFirst().position().distanceToSqr(wallFace) < 1e-10
                                && level.getBlockState(cell).is(loli),
                        "nearer native Loli wins before Greed, drops once and stays in the world");
                items.forEach(Entity::discard);

                mother.entityTags().addAll(Set.of("mom", "skywars2.family"));
                game.items().markCurrentItemEpoch(mother);
                mother.setNoGravity(true);
                mother.setPermanentlyInvulnerable(true);
                mother.setPos(from.add(direction.scale(.58)).add(0, -.5, 0));
                require(level.addFreshEntity(mother) && game.items().familyDrops().snapshot().contains(mother),
                        "registered native Mother before the reflector");
                block(cell, Blocks.AIR);
                var motherFace = mother.getBoundingBox().inflate(.12)
                        .clip(from, from.add(direction.scale(2))).orElseThrow();
                var intercepted = fireTowardGreed(from, fixtureShots);
                items = yellowDrops(from);
                require(!wands.hasElectroTrace(intercepted) && intercepted.owner == owner
                                && intercepted.position.distanceToSqr(motherFace) < 1e-10
                                && items.size() == 1 && items.getFirst().getItem().getCount() == 1
                                && items.getFirst().position().distanceToSqr(motherFace) < 1e-10 && mother.isAlive(),
                        "nearer Mother has the same priority over Greed as a terrain contact");
            } finally {
                other.entityTags().remove("sin_greed_active");
                other.teleportTo(level, oldPosition.x, oldPosition.y, oldPosition.z, Set.of(), oldYaw, oldPitch, true);
                mother.discard();
                yellowDrops(from).forEach(Entity::discard);
                for (var shot : fixtureShots) shot.age = shot.lifetime;
                wands.tick();
                game.maps().restoreSnapshot(level, cell, blocks.get(cell));
            }
        }

        void yellowKatanaFamilyContacts() {
            var level = owner.level();
            var electro = game.items().electro();
            var cell = BlockPos.containing(at).offset(2, 5, 24);
            var from = Vec3.atCenterOf(cell).add(0, 0, -1);
            var meleeTarget = cow(owner.position().add(1, 0, 0));
            var mother = EntityTypes.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(mother != null, "native Yellow Katana Mother fixture");
            var tesla = game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/staffs/tesla"));
            require(tesla != null, "native Tesla advancement for unrelated award control");
            var tracker = owner.getAdvancements();
            var oldCriteria = new HashSet<String>();
            tracker.getOrStartProgress(tesla).getCompletedCriteria().forEach(oldCriteria::add);
            var main = owner.getMainHandItem();
            var oldPitch = owner.getXRot();
            var oldYaw = owner.getYRot();
            try {
                oldCriteria.forEach(criterion -> tracker.revoke(tesla, criterion));
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LbcElectro.SHIT_KATANA));
                owner.setXRot(0);
                owner.setYRot(0);
                block(cell.north(), Blocks.AIR);
                block(cell, LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI));
                require(yellowDrops(from).isEmpty(), "empty native Yellow Katana loot corridor");
                var loliHit = fireYellowKatanaArc(electro, meleeTarget, from);
                var loot = yellowDrops(from);
                SkyWars2.LOGGER.info("[check] native Yellow Katana Loli contact: {} drops, endpoint {}",
                        loot.size(), loliHit.position);
                require(loot.size()==1 && loot.getFirst().getItem().getCount()==1
                                && Math.abs(loliHit.position.z-cell.getZ())<1e-6
                                && LbcLuckyBlocks.kind(level.getBlockState(cell))==LbcLuckyBlocks.Kind.LOLI,
                        "a real Yellow Katana arc stops on Loli and drops one Yellow Lightning");
                loot.forEach(Entity::discard);

                mother.entityTags().addAll(Set.of("mom", "skywars2.family"));
                game.items().markCurrentItemEpoch(mother);
                mother.setNoGravity(true);
                mother.setPos(from.add(0, -.5, 1.05));
                require(level.addFreshEntity(mother) && game.items().familyDrops().snapshot().contains(mother),
                        "native Yellow Katana Mother enters the actor index");
                block(cell, Blocks.STONE);
                var wallHit = fireYellowKatanaArc(electro, meleeTarget, from);
                require(Math.abs(wallHit.position.z-cell.getZ())<1e-6 && yellowDrops(from).isEmpty(),
                        "natural stone collision prevents a Mother reaction behind the wall");

                block(cell, LbcLuckyBlocks.block(LbcLuckyBlocks.Kind.LOLI));
                mother.setPos(from.add(0, -.5, .75));
                var motherHit = fireYellowKatanaArc(electro, meleeTarget, from);
                loot = yellowDrops(from);
                require(loot.size()==1 && loot.getFirst().getItem().getCount()==1
                                && motherHit.position.z>from.z && motherHit.position.z<cell.getZ()
                                && Math.abs(loot.getFirst().getZ()-motherHit.position.z)<1e-6
                                && mother.isAlive() && LbcLuckyBlocks.kind(level.getBlockState(cell))==LbcLuckyBlocks.Kind.LOLI,
                        "closest native Mother stops the arc early and reacts once while leaving the Loli block intact");
                loot.forEach(Entity::discard);

                checkYellowKatanaShield(electro, meleeTarget, from);
                require(!tracker.getOrStartProgress(tesla).isDone(),
                        "Yellow Katana family contact does not award the unrelated Tesla advancement");
            } finally {
                electro.transformOrigins(level, point -> null);
                electro.reset(owner);
                yellowDrops(from).forEach(Entity::discard);
                block(cell, Blocks.AIR);
                mother.discard();
                meleeTarget.discard();
                owner.setItemInHand(InteractionHand.MAIN_HAND, main);
                owner.setXRot(oldPitch);
                owner.setYRot(oldYaw);
                oldCriteria.forEach(criterion -> tracker.award(tesla, criterion));
            }
        }

        private LbcElectro.YellowTrace fireYellowKatanaArc(LbcElectro electro, ProbeCow target, Vec3 from) {
            require(electro.yellowSnapshot().isEmpty(), "fresh native Yellow Katana volley");
            target.setInvulnerableTime(0);
            target.damageCooldownTime = 0;
            float health = target.getHealth();
            owner.resetAttackStrengthTicker();
            owner.attack(target);
            var volley = electro.yellowSnapshot();
            require(target.getHealth()<health && volley.size()==7,
                    "native accepted melee launches seven ordinary Yellow Katana arcs");
            var witness = volley.getFirst();
            var eye = witness.position;
            // Route one actual melee bolt into the bounded corridor so the drop count is exact.
            electro.transformOrigins(owner.level(), point -> point.equals(eye) ? from : null);
            require(electro.yellowSnapshot().equals(List.of(witness)), "one real Yellow Katana trace enters the corridor");
            electro.tick();
            require(electro.yellowSnapshot().isEmpty(), "terminal native Yellow Katana contact retires the trace");
            return witness;
        }

        private void checkYellowKatanaShield(LbcElectro electro, ProbeCow target, Vec3 from) {
            var offhand = other.getOffhandItem();
            var position = other.position();
            var yaw = other.getYRot();
            var pitch = other.getXRot();
            var scoreboard = game.server().getScoreboard();
            scoreboard.removePlayerFromTeam(other.getScoreboardName(), team);
            try {
                var blocker = from.add(0, -1, 2.95);
                other.teleportTo(other.level(), blocker.x, blocker.y, blocker.z, Set.of(), 180, 0, true);
                other.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
                other.startUsingItem(InteractionHand.OFF_HAND);
                for (int i=0;i<6;i++) other.doTick();
                var clip = game.items().wands().shieldHit(owner, from, from.add(0, 0, .5));
                require(other.isBlocking() && clip!=null && clip.getEntity()==other
                                && clip.getLocation().z>from.z && clip.getLocation().z<from.z+.4,
                        "active native shield is ahead of both family targets");
                var blocked = fireYellowKatanaArc(electro, target, from);
                require(blocked.position.distanceToSqr(clip.getLocation())<1e-8 && yellowDrops(from).isEmpty(),
                        "the native shield stops Yellow Katana before either family target can react");
            } finally {
                other.stopUsingItem();
                other.setItemInHand(InteractionHand.OFF_HAND, offhand);
                other.teleportTo(other.level(), position.x, position.y, position.z, Set.of(), yaw, pitch, true);
                scoreboard.addPlayerToTeam(other.getScoreboardName(), team);
            }
        }

        private List<net.minecraft.world.entity.item.ItemEntity> yellowDrops(Vec3 point) {
            return owner.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(point, point).inflate(3),
                    e -> flag(e.getItem(), "yellow_lightning") == 1);
        }

        void boundaries(){
            var mark=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,owner.level());mark.setPos(at);mark.entityTags().add("stopper_magic");owner.level().addFreshEntity(mark);game.items().magic().loaded(mark);
            try{int mana=game.items().mana().get(owner);require(!use(loot("tesla_wand"),InteractionHand.MAIN_HAND)&&game.items().mana().get(owner)==mana,"mana wand activation blocked by real antimagic");
                // Already launched electricity remains nonmagical inside that same beacon.
                game.items().wands().teslaVolley(owner,InteractionHand.MAIN_HAND,loot("tesla_wand"),false);var trace=shots().getFirst();var from=trace.position;game.items().wands().tick();require(trace.position.distanceToSqr(from)>1,"existing arcs keep moving inside antimagic");clear();
            }finally{game.items().magic().unloaded(mark);mark.discard();}
        }
        int waits;ProbeCow instantVictim;
        void beginInstant(){
            owner.setYRot(0);owner.setXRot(0);var s=loot("academy_tesla_wand");flag(s,"mode",2);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);instantVictim=cow(owner.getEyePosition().add(0,-.6,2));
            for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)block(BlockPos.containing(owner.getEyePosition()).offset(x,y,4),Blocks.STONE);
            int before=game.items().stonekivi().count(),mana=game.items().mana().get(owner);require(use(s,InteractionHand.MAIN_HAND)&&game.items().stonekivi().count()==before+1&&game.items().mana().get(owner)==mana-1200,"native instant queues one five-beam work item for1200");
            game.items().timeStop().begin(owner.level(),200);
        }
        void conductionTrial(){
            var center=at.add(0,4,20);var pos=BlockPos.containing(center);block(pos,Blocks.IRON_BLOCK);
            var cows=new ArrayList<ProbeCow>();for(int i=0;i<10;i++){var c=cow(center.add(4,0,(i-5)*.3));cows.add(c);if(i%2==0)game.server().getScoreboard().addPlayerToTeam(c.getScoreboardName(),team);}
            var technical=cow(center.add(-4,0,0));technical.entityTags().add("strict_map_object");cows.getLast().setPermanentlyInvulnerable(true);
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);require(use(trial,InteractionHand.OFF_HAND),"actual trial offhand volley");var trace=shots().getFirst();shots().stream().filter(t->t!=trace).forEach(t->t.age=t.lifetime);trace.position=center;trace.yaw=0;trace.pitch=0;
            game.items().wands().tick();require(flag(trial,"upgrade_available")==0&&cows.stream().limit(9).allMatch(c->c.getHealth()==999)&&technical.getHealth()==1000,"nine accepted conductor targets insufficient, friendly mobs hit and technical protected");
            cows.getLast().setPermanentlyInvulnerable(false);trace.position=center;game.items().wands().tick();require(flag(trial,"upgrade_available")==1&&cows.getLast().getHealth()==999&&cows.getFirst().getKillCredit()==owner,"ten actual conductor hits qualify used offhand stack with native credit");
            clear();cows.forEach(Entity::discard);technical.discard();
        }
        boolean step(){long now=owner.level().getGameTime();if(last==now)return false;last=now;
            if(phase==0){presentation();geometry();cadence();precisePulseGeometry();instantConduction();instantContacts();chain();yellowKatanaFamilyContacts();familyArcContacts();greedTerrainOrder();boundaries();beginInstant();phase++;return false;}
            if(phase==1){require(game.items().stonekivi().count()>0&&instantVictim.direct==0,"actual Time Stop freezes queued instant work");if(++waits<3)return false;game.items().timeStop().end(owner.level());waits=0;phase++;return false;}
            if(phase==2){if(game.items().stonekivi().count()>0){require(++waits<200,"queued five rays complete");return false;}require(instantVictim.getHealth()<1000&&instantVictim.direct>=5&&instantVictim.getKillCredit()==owner,"all five native instant beams hit and credit real caster");instantVictim.discard();
                panel=new net.minecraft.world.entity.Interaction(EntityTypes.INTERACTION,owner.level());panel.setPos(at);panel.entityTags().add("magic_academy");owner.level().addFreshEntity(panel);trial=loot("tesla_wand");owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&LbcAcademy.trial(trial),"actual Tesla Academy admission");phase++;return false;}
            if(phase==3){conductionTrial();phase++;return false;}
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.MAIN_HAND,trial);require(game.items().academy().interact(owner,InteractionHand.MAIN_HAND,panel)&&owner.getMainHandItem().is(LbcWands.ACADEMY_TESLA)&&Identifier.parse("lbc:tier_a").equals(owner.getMainHandItem().get(DataComponents.TOOLTIP_STYLE)),"real Tesla Academy A promotion");return true;
        }
        public void close(){
            game.items().timeStop().end(owner.level());var fresh=new ArrayList<Entity>();owner.level().getAllEntities().forEach(e->{if(!before.contains(e)&&e.position().distanceToSqr(at)<40000)fresh.add(e);});fresh.forEach(Entity::discard);
            for(var e:blocks.entrySet()){if(owner.level().getBlockEntity(e.getKey()) instanceof net.minecraft.world.Container c)c.clearContent();game.maps().restoreSnapshot(owner.level(),e.getKey(),e.getValue());}
            game.server().getScoreboard().removePlayerTeam(team);saved.restore(game);otherSaved.restore(game);stats.forEach((s,v)->owner.getStats().setValue(owner,s,v));
            var p=game.players().get(owner.getUUID());p.lbcSkill=skill;p.lbcTrinket=trinket;p.lbcChallengeProgress=challengeProgress;p.lbcUnlocks.clear();p.lbcUnlocks.addAll(unlocks);game.config().offhandWands=offhand;
            for(var entry:advancements.entrySet()){var advancement=game.server().getAdvancements().get(entry.getKey());for(String criterion:advancement.value().criteria().keySet())if(!entry.getValue().contains(criterion))owner.getAdvancements().revoke(advancement,criterion);}
            game.items().indirectKills().clear(owner);game.items().indirectKills().clear(other);for(long k:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(k),net.minecraft.world.level.ChunkPos.getZ(k),false);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.init();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(!f.warm())return;if(f.step()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS tesla");}}catch(Throwable e){running=null;try{f.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tesla",e);}}

    private static GameChecks.WandThirdPlayerState stopSaved;private static String epoch;
    public static void stopProbe(GameController game){var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(stopSaved==null,"exclusive stop probe");stopSaved=new GameChecks.WandThirdPlayerState(owner,game.items().mana());owner.setShiftKeyDown(false);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);game.items().mana().noMagic(owner,0);game.items().mana().set(owner,100000);var s=game.lbc().rollTable(owner.level(),owner.position(),owner,"lbc:ported/academy_tesla_wand").getFirst();var d=s.get(DataComponents.CUSTOM_DATA).copyTag();d.putInt("mode",2);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));owner.setItemInHand(InteractionHand.MAIN_HAND,s);require(game.items().wands().use(owner,InteractionHand.MAIN_HAND)&&game.items().stonekivi().count()>0,"instant work pending before actual stop");d.putInt("mode",3);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));require(game.items().wands().use(owner,InteractionHand.MAIN_HAND),"live chain before stop");epoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS tesla-stop-probe");}
    public static void cleanup(GameController game){try{require(stopSaved!=null&&!game.items().currentEpoch(epoch)&&game.items().stonekivi().count()==0&&game.items().wands().electroSnapshot().isEmpty(),"actual match stop clears instant work and moving Tesla arcs");}finally{if(stopSaved!=null)stopSaved.restore(game);stopSaved=null;epoch=null;}SkyWars2.LOGGER.info("[check] PASS tesla-cleanup");}
}
