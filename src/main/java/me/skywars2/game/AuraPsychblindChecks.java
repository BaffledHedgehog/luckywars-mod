package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Operator checks use real selection, native incoming criteria and the ordinary server scheduler. */
public final class AuraPsychblindChecks {
    private AuraPsychblindChecks(){}
    private static Fixture running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Aura/Psychblind: "+why);}
    private static void near(double a,double b,String why){require(Math.abs(a-b)<.0001,why+" actual="+a+" expected="+b);}
    public static void runAura(GameController game){start(game,0);}
    public static void runPsychblind(GameController game){start(game,1);}
    public static void runRewind(GameController game){start(game,2);}
    private static void start(GameController game,int kind){
        require(running==null,"exclusive scheduled fixture");var f=new Fixture(game,kind);running=f;
        try{f.begin();}catch(Throwable e){running=null;try{f.close();}catch(Throwable c){e.addSuppressed(c);}throw e;}
    }
    public static void tick(GameController game){
        if(running==null)return;var f=running;
        try{require(game==f.game,"same native controller");if(!f.step())return;running=null;f.close();SkyWars2.LOGGER.info("[check] PASS {} ({})",f.name(),f.kind==0?"native shield/resisted criteria, seven source colors, real payloads and selection cleanup":f.kind==1?"native selection/global claims, nondamaging ally gathering, clone/TimeStop and ordinary Psycho terminal":"nonempty actual whole-coordinator aura/doppel identity and phase replay");}
        catch(Throwable e){running=null;try{f.close();}catch(Throwable c){e.addSuppressed(c);}SkyWars2.LOGGER.error("[check] FAIL "+f.name(),e);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final int kind;final ServerPlayer owner,other;final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved,otherSaved;
        final SauvojenMeditationChecks.Attribution attribution;
        final RegenerationBeltChecks.ScheduledClient client,audience;
        final boolean skills,ranked,ownerClaim,otherClaim;final Vec3 origin;final int started;
        final Set<UUID> original=new HashSet<>();final List<Cow> actors=new ArrayList<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final net.minecraft.world.scores.PlayerTeam oldTeam,otherTeam,team;
        double attack,armor,toughness,knockback;int manaRegen,stage,when;boolean closed,stopped,restoring;
        Cow target,ally,shop,observer;BlockPos support;float health;UUID doppelId;Vec3 frozenPosition;
        Map<UUID,Vec3> replayBodies=new LinkedHashMap<>();
        final Set<UUID> removedGrams=new LinkedHashSet<>();
        Fixture(GameController game,int kind){
            this.game=game;this.kind=kind;
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two real active participants");level=owner.level();
            require(!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(level),"exclusive coordinator and source clock");
            require(game.items().psychblindSkill().count()==0&&game.items().psycho().snapshot().isEmpty(),"exclusive native doppel/Psycho families");
            saved=new RegenerationBeltChecks.Snapshot(game,owner);otherSaved=new RegenerationBeltChecks.Snapshot(game,other);
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));skills=game.config().lbcSkills;ranked=game.config().ranked;ownerClaim=owner.entityTags().contains(LbcPsychblindSkill.CLAIM);otherClaim=other.entityTags().contains(LbcPsychblindSkill.CLAIM);
            level.getAllEntities().forEach(e->original.add(e.getUUID()));origin=new Vec3(owner.getX(),level.getMaxY()-12,owner.getZ());
            for(var p:BlockPos.betweenClosed(BlockPos.containing(origin.add(-4,-2,-4)),BlockPos.containing(origin.add(4,4,4))))require(level.hasChunkAt(p)&&level.getBlockState(p).isAir(),"resident air fixture "+p);
            oldTeam=owner.getTeam();otherTeam=other.getTeam();team=game.server().getScoreboard().addPlayerTeam("aura_psych_qa");team.setAllowFriendlyFire(false);
            client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,other);started=game.server().getTickCount();
        }
        String name(){return kind==0?"rainbow-aura":kind==1?"psychblind-skill":"aura-psychblind-rewind";}
        void begin(){
            game.config().lbcSkills=true;game.config().ranked=false;
            for(var p:List.of(owner,other)){
                p.stopUsingItem();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
                p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAbilities().flying=false;p.noPhysics=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setOnGround(false);p.removeAllEffects();
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(500);p.setAbsorptionAmount(0);p.getFoodData().setFoodLevel(8);p.getFoodData().setSaturation(0);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=profile.lbcKit=profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);
                game.items().mana().noMagic(p,0);game.items().mana().baseRegen(p,0);game.items().magic().inventoryChanged(p);
                // Both players are source mobs too. Reserve their claims to isolate one finite cow source.
                p.entityTags().add(LbcPsychblindSkill.CLAIM);
                game.server().getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);
            }
            move(owner,origin);move(other,origin.add(20,0,0));game.items().magic().flushCleansing();
            attack=owner.getAttributeValue(Attributes.ATTACK_DAMAGE);armor=owner.getAttributeValue(Attributes.ARMOR);toughness=owner.getAttributeValue(Attributes.ARMOR_TOUGHNESS);knockback=owner.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);manaRegen=game.items().mana().baseRegen(owner);
            if(kind==0){select(owner,32);near(owner.getAttributeValue(Attributes.ATTACK_DAMAGE),attack+5,"real selection enters red");target=cow(origin.add(0,0,2));owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));owner.startUsingItem(InteractionHand.OFF_HAND);next(0);}
            else if(kind==1){move(other,origin.add(12,0,0));select(owner,36);select(other,36);ally=cow(origin.add(0,0,16));game.server().getScoreboard().addPlayerToTeam(ally.getScoreboardName(),team);shop=cow(origin.add(1,0,16));LbcBedwarsRoleGuards.markShop(shop);observer=cow(origin.add(-1,0,16));observer.entityTags().add("spectator");health=ally.getHealth();next(20);}
            else {select(owner,32);hit();hit();hit();hit();require(color()==LbcRainbowAura.Color.AQUA,"rewind has active aqua ownership");select(other,36);ally=cow(origin.add(20,0,16));next(40);}
        }
        void select(ServerPlayer p,int id){
            String gate=id==32?"gaycaster":"psychoblind";
            String path="lbc:true_advancements/legends/academy/"+gate;
            game.items().mana().award(p,path);
            var advancement=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(path));
            require(advancement!=null&&p.getAdvancements().getOrStartProgress(advancement).isDone(),"actual completed source advancement "+gate);
            require(GameController.operator(p),"real operator opens native debug menu for in-match selection");
            boolean hadDebug=p.entityTags().contains("skywars2.debug_perks");
            try{
                game.menus().openDebug(p);
                require(game.menus().debugAccess(p)&&game.canUseMenus(p),"actual open debug GUI admits in-match menu input");
                // The GUI grants menu access, but this selector still exercises the completed source gate.
                p.entityTags().remove("skywars2.debug_perks");
                require(!game.debugPerks(p),"source selection has no debug achievement bypass");
                require(game.lbc().select(p,LbcLoadouts.Kind.SKILLS,10100,id==32?4:8),"real achievement-gated source selector "+id);
                p.closeContainer();
                require(!game.menus().debugAccess(p)&&!game.debugPerks(p)&&game.lbc().hasSkill(p,id),"actual selected native skill survives closed GUI and removed bypass "+id);
            }finally{
                try{p.closeContainer();}finally{if(hadDebug)p.entityTags().add("skywars2.debug_perks");else p.entityTags().remove("skywars2.debug_perks");}
            }
        }
        void move(ServerPlayer p,Vec3 at){require(p.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"native participant teleport");p.setDeltaMovement(Vec3.ZERO);if(p==owner)client.acknowledge();else audience.acknowledge();}
        Cow cow(Vec3 at){var c=new Cow(EntityTypes.COW,level);c.setPos(at);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);require(level.addFreshEntity(c),"native source body publication");actors.add(c);return c;}
        void resetHit(){owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.hurtTime=0;owner.hurtDuration=0;owner.setDeltaMovement(Vec3.ZERO);}
        LbcRainbowAura.Color color(){return game.items().rainbowAura().color(owner);}
        void hit(){var before=color();resetHit();require(owner.hurtServer(level,owner.damageSources().generic(),1),"real native incoming damage admitted");require(color()==LbcRainbowAura.Color.values()[(before.ordinal()+1)%7],"exactly one source color transition");}
        void next(int n){stage=n;when=game.server().getTickCount();}
        List<LbcPsychblindSkill.Doppel> doppels(){var result=new ArrayList<LbcPsychblindSkill.Doppel>();for(var e:level.getAllEntities())if(e instanceof LbcPsychblindSkill.Doppel d&&!original.contains(e.getUUID()))result.add(d);return result;}
        void block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){terrain.putIfAbsent(pos.immutable(),game.maps().snapshot(level,pos));level.setBlock(pos,state,2);}
        boolean step(){
            client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<500,"finite scheduled fixture, stage="+stage);int elapsed=game.server().getTickCount()-when;
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            switch(stage){
                case 0->{
                    if(elapsed<8)return false;require(owner.isBlocking(),"native shield delay elapsed");owner.setYRot(0);owner.setYHeadRot(0);owner.setYBodyRot(0);resetHit();
                    var source=owner.damageSources().mobAttack(target);var facing=Vec3.directionFromRotation(0,owner.getYHeadRot());var toward=target.position().subtract(owner.position()).multiply(1,0,1).normalize();double angle=Math.acos(Math.clamp(facing.dot(toward),-1d,1d));
                    require(owner.getOffhandItem().get(DataComponents.BLOCKS_ATTACKS).resolveBlockedDamage(source,4,angle)==4,"native actual source inside shield arc");
                    int blocked=owner.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_BLOCKED_BY_SHIELD));float hp=owner.getHealth();
                    require(!owner.hurtServer(level,source,4)&&owner.getHealth()==hp,"real fully blocked incoming control");
                    require(owner.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_BLOCKED_BY_SHIELD))>blocked&&color()==LbcRainbowAura.Color.ORANGE,"positive native shield stat and criterion advance");
                    near(owner.getAttributeValue(Attributes.ATTACK_DAMAGE),attack,"orange removes only aura red delta");owner.stopUsingItem();owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);health=target.getHealth();next(1);
                }
                case 1->{
                    if(target.getHealth()==health){require(elapsed<30,"ordinary orange source cadence");return false;}
                    require(target.getHealth()==health-1&&!target.isOnFire()&&owner.hasEffect(MobEffects.FIRE_RESISTANCE),"actual orange1 damage without ignition plus source resistance");target.discard();
                    owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40,4,false,false));resetHit();float hp=owner.getHealth();owner.hurtServer(level,owner.damageSources().generic(),4);
                    require(owner.getHealth()==hp&&color()==LbcRainbowAura.Color.YELLOW,"actual resistance-zero incoming criterion advances once");owner.removeAllEffects();
                    near(owner.getAttributeValue(Attributes.ARMOR),armor+40,"yellow armor40");near(owner.getAttributeValue(Attributes.ARMOR_TOUGHNESS),toughness+40,"yellow toughness40");near(owner.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),knockback+100,"yellow raw resistance100");hit();
                    near(owner.getAttributeValue(Attributes.ARMOR),armor,"lime removes yellow armor");near(owner.getAttributeValue(Attributes.ARMOR_TOUGHNESS),toughness,"lime removes yellow toughness");near(owner.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),knockback,"lime removes yellow resistance");near(game.items().regenerationBelt().rate(owner),.025,"lime enters real shared command regen base");
                    var belt=game.lbc().rollTable(level,owner.position(),owner,"lbc:ported/regeneration_belt").getFirst();var flags=LbcFartus.data(belt);var regen=flags.getCompound("PublicBukkitValues").orElseThrow().getCompound("lws:regen").orElseThrow();regen.putString("lws:operation","multiple");regen.putDouble("lws:value",2);belt.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));owner.getInventory().setItem(9,belt);near(game.items().regenerationBelt().rate(owner),.05,"lime base precedes actual item multiplier");owner.getInventory().setItem(9,ItemStack.EMPTY);health=owner.getHealth();next(2);
                }
                case 2->{if(elapsed<6)return false;require(owner.getHealth()>health,"normal native regen owner heals lime");hit();near(game.items().mana().baseRegen(owner),manaRegen+5,"aqua adds source5");near(game.items().regenerationBelt().rate(owner),0,"aqua removes lime base");game.items().mana().set(owner,9499);next(3);}
                case 3->{
                    if(game.items().mana().get(owner)<9999){require(elapsed<120,"source100 cadence raw mana pulse");return false;}
                    require(game.items().mana().get(owner)==10000,"aqua9499 receives raw500 then exact source10000 ceiling");
                    var checkpoint=game.items().rainbowAura().checkpoint();
                    for(var id:checkpoint.actorIds())if(!original.contains(id)&&level.getEntity(id) instanceof Display.ItemDisplay gram&&gram.entityTags().contains("skywars2.rainbow_visual"))removedGrams.add(id);
                    require(!removedGrams.isEmpty(),"real source aqua pulse published owned native managram displays");
                    // Actual native removal must retire cosmetic links without altering source color/resource ownership.
                    for(var id:removedGrams)level.getEntity(id).discard();next(8);
                }
                case 8->{
                    if(elapsed<2)return false;var checkpoint=game.items().rainbowAura().checkpoint();
                    for(var id:removedGrams)require(level.getEntity(id)==null&&!checkpoint.actorIds().contains(id),"native visual unload retires removed managram UUID "+id);
                    require(checkpoint.prepare(level::getEntity).pending().isEmpty(),"future native checkpoint has no dead visual links");
                    require(color()==LbcRainbowAura.Color.AQUA&&game.items().mana().baseRegen(owner)==manaRegen+5&&game.items().mana().get(owner)==10000,"visual removal preserves active aqua color and resource ownership");
                    hit();near(game.items().mana().baseRegen(owner),manaRegen,"blue reverses source5");game.items().mana().score(owner,"nanobots_tier").set(217);next(4);
                }
                case 4->{if(elapsed<25)return false;require(game.items().mana().score(owner,"nanobots_tier").get()==217,"blue preserves charge already above120");game.items().mana().score(owner,"nanobots_tier").set(118);next(5);}
                case 5->{if(game.items().mana().score(owner,"nanobots_tier").get()==118){require(elapsed<40,"second blue source20 cadence");return false;}require(game.items().mana().score(owner,"nanobots_tier").get()==120,"blue118 saturates at120");hit();support=BlockPos.containing(origin).below();block(support,Blocks.CHEST.defaultBlockState());((ChestBlockEntity)level.getBlockEntity(support)).setItem(0,new ItemStack(Items.DIAMOND,3));for(var d:List.of(support.north(),support.south(),support.east(),support.west()))block(d,Blocks.AIR.defaultBlockState());move(owner,Vec3.atBottomCenterOf(support.above()));owner.setOnGround(true);next(6);}
                case 6->{
                    var neighbors=List.of(support.north(),support.south(),support.east(),support.west());if(neighbors.stream().anyMatch(p->!level.getBlockState(p).is(Blocks.CHEST))){require(elapsed<20,"purple native support copy finishes");owner.setOnGround(true);return false;}
                    require(((ChestBlockEntity)level.getBlockEntity(support)).getItem(0).is(Items.DIAMOND)&&((ChestBlockEntity)level.getBlockEntity(support)).getItem(0).getCount()==3,"purple preserves original container inventory");for(var p:neighbors)require(((ChestBlockEntity)level.getBlockEntity(p)).isEmpty(),"only copied container Items stripped");
                    ((ChestBlockEntity)level.getBlockEntity(support)).clearContent();var mushroom=Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();for(var property:List.of(net.minecraft.world.level.block.HugeMushroomBlock.UP,net.minecraft.world.level.block.HugeMushroomBlock.DOWN,net.minecraft.world.level.block.HugeMushroomBlock.NORTH,net.minecraft.world.level.block.HugeMushroomBlock.SOUTH,net.minecraft.world.level.block.HugeMushroomBlock.EAST,net.minecraft.world.level.block.HugeMushroomBlock.WEST))mushroom=mushroom.setValue(property,false);block(support,mushroom);block(support.north(),Blocks.OBSIDIAN.defaultBlockState());for(var p:List.of(support.south(),support.east(),support.west()))block(p,Blocks.AIR.defaultBlockState());owner.setOnGround(true);next(7);
                }
                case 7->{
                    if(!level.getBlockState(support.south()).is(Blocks.RED_MUSHROOM_BLOCK)){require(elapsed<20,"purple mushroom copy finishes");owner.setOnGround(true);return false;}
                    var copied=level.getBlockState(support.south());for(var property:List.of(net.minecraft.world.level.block.HugeMushroomBlock.UP,net.minecraft.world.level.block.HugeMushroomBlock.DOWN,net.minecraft.world.level.block.HugeMushroomBlock.NORTH,net.minecraft.world.level.block.HugeMushroomBlock.SOUTH,net.minecraft.world.level.block.HugeMushroomBlock.EAST,net.minecraft.world.level.block.HugeMushroomBlock.WEST))require(copied.getValue(property),"copied source mushroom normalizes all six faces");require(level.getBlockState(support.north()).is(Blocks.OBSIDIAN),"purple admission preserves nonair neighbor");
                    owner.setOnGround(false);hit();require(color()==LbcRainbowAura.Color.RED,"complete seven-state source cycle");near(owner.getAttributeValue(Attributes.ATTACK_DAMAGE),attack+5,"red reentry is single owned delta");
                    block(BlockPos.containing(owner.position()).offset(2,0,0),LbcMagic.MAGIC_BEACON_BLOCK.defaultBlockState());next(90);
                }
                case 90->{
                    if(elapsed<3)return false;require(game.items().magic().suppressed(owner),"MAG aura positive control");
                    near(owner.getAttributeValue(Attributes.ATTACK_DAMAGE),attack,"MAG withdraws active red bonus");
                    resetHit();owner.hurtServer(level,owner.damageSources().generic(),1);require(color()==LbcRainbowAura.Color.RED,"MAG prevents aura color transition");
                    block(BlockPos.containing(owner.position()).offset(2,0,0),Blocks.AIR.defaultBlockState());next(91);
                }
                case 91->{
                    if(elapsed<3)return false;require(!game.items().magic().suppressed(owner),"MAG aura removed");
                    near(owner.getAttributeValue(Attributes.ATTACK_DAMAGE),attack+5,"red resumes with one owned bonus");
                    select(owner,36);require(color()==null,"real selection loss clears aura");near(owner.getAttributeValue(Attributes.ATTACK_DAMAGE),attack,"selection loss removes red only");return true;
                }
                case 20->{
                    if(!ally.entityTags().contains(LbcPsychblindSkill.CLAIM)){require(elapsed<30,"selected-event enrollment extracts actual source");return false;}
                    require(ally.getHealth()==health&&!shop.entityTags().contains(LbcPsychblindSkill.CLAIM)&&!observer.entityTags().contains(LbcPsychblindSkill.CLAIM),"nondamaging ally intake excludes protected shop/observer");
                    var ds=doppels();require(ds.size()==1,"one global source claim across two actual selected casters");var d=ds.getFirst();doppelId=d.getUUID();require(d.gathering,"distant source starts gathering");
                    require(game.items().cloneSkills().copyNow(d,15)&&game.items().cloneSkills().copyNow(d,17),"installed x2/x4 accept actual typed doppel");require(doppels().size()==5,"x2 adds one and x4 adds three");require(!game.items().cloneSkills().copyNow(d,15)&&!game.items().cloneSkills().copyNow(d,17),"same variant recursion denied");
                    // Isolate the original source soul for an exact ONE terminal, after testing both clone adapters.
                    for(var copy:doppels())if(copy!=d)copy.discard();frozenPosition=d.position();game.items().timeStop().begin(level,1000);stopped=true;next(21);
                }
                case 21->{require(level.getEntity(doppelId)!=null&&level.getEntity(doppelId).position().equals(frozenPosition),"TimeStop freezes actual gathering body");if(elapsed<5)return false;game.items().timeStop().end(level);stopped=false;next(22);}
                case 22->{
                    var d=(LbcPsychblindSkill.Doppel)level.getEntity(doppelId);require(d!=null,"source soul remains without hostile target");
                    if(d.gathering){require(elapsed<35,"eight .25 native gathering samples converge");return false;}
                    require(ally.getHealth()==health&&game.items().psycho().snapshot().isEmpty(),"ally never becomes harmful terminal target");frozenPosition=d.position();next(25);
                }
                case 25->{
                    if(elapsed<3)return false;var d=(LbcPsychblindSkill.Doppel)level.getEntity(doppelId);require(d!=null,"unfired source remains in orbit");var caster=(ServerPlayer)d.getOwner();require(caster!=null,"real doppel caster retained");
                    var radial=d.position().subtract(caster.position());near(radial.y,3,"source orbit height3");near(Math.sqrt(radial.x*radial.x+radial.z*radial.z),4,"source orbit radius4");require(!d.position().equals(frozenPosition),"ordinary source orbit advances");
                    var outward=radial.multiply(1,0,1).normalize().scale(-1);if(caster==owner)move(other,origin.add(35,0,0));
                    target=cow(caster.position().add(outward.scale(9)));target.entityTags().add(LbcPsychblindSkill.CLAIM);var farther=cow(caster.position().add(outward.scale(10)));farther.entityTags().add(LbcPsychblindSkill.CLAIM);next(23);
                }
                case 23->{
                    var charges=game.items().psycho().snapshot();if(charges.isEmpty()){require(elapsed<20,"ordinary Psycho terminal appears");return false;}
                    require(charges.size()==1&&charges.getFirst().lifetime==100&&!charges.getFirst().invisible&&!charges.getFirst().spray,"exactly one existing visible100tick Psycho charge");var shot=charges.getFirst();require(shot.getDeltaMovement().normalize().dot(target.getEyePosition().subtract(shot.position()).normalize())>.999,"terminal aims at actual nearest hostile9 ahead of farther10");require(level.getEntity(doppelId)==null,"terminal consumes the source soul");health=target.getHealth();next(24);
                }
                case 24->{if(target.getHealth()==health){require(elapsed<30,"actual shared Psycho flight hits nearest hostile");return false;}require(target.getHealth()==health-4,"existing Psycho collision deals source4");require(target.hasEffect(MobEffects.DARKNESS)&&game.items().psycho().effectValue(target,"scared")>0&&game.items().psycho().effectValue(target,"lookback")>0&&game.items().psycho().effectValue(target,"twitching")>0,"existing shared Psycho status owner receives collision");require(ally.getHealth()==1000,"borrowed ally remains unharmed");require(ally.entityTags().contains(LbcPsychblindSkill.CLAIM),"global source claim survives visual consumption");game.items().psychblindSkill().clear(owner.getUUID());game.items().psychblindSkill().clear(other.getUUID());require(game.items().psychblindSkill().count()==0,"source owner cleanup removes bodies");return true;}
                case 40->{
                    if(doppels().isEmpty()){require(elapsed<30,"nonempty actual native rewind graph");return false;}
                    game.items().timeStop().begin(level,10000);stopped=true;require(game.vanity().requestCapture(owner,false),"actual whole coordinator accepts nonempty aura/doppel graph");next(41);
                }
                case 41->{
                    if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;for(var d:doppels())replayBodies.put(d.getUUID(),d.position());require(!replayBodies.isEmpty(),"captured actual typed bodies");
                    game.items().rainbowAura().clear(owner);game.items().psychblindSkill().clear(other.getUUID());require(doppels().isEmpty(),"future typed bodies really removed");game.items().mana().set(owner,777);require(game.vanity().requestRestoreForCheck(),"actual whole coordinator restore");restoring=true;next(42);
                }
                case 42->{
                    if(game.vanity().busy())return false;require(color()==LbcRainbowAura.Color.AQUA&&game.items().mana().baseRegen(owner)==manaRegen+5,"actual coordinator restores aqua ownership without duplicated entry delta");
                    for(var e:replayBodies.entrySet())require(level.getEntity(e.getKey()) instanceof LbcPsychblindSkill.Doppel d&&d.position().equals(e.getValue())&&d.getOwner()==other,"actual native identity/position/caster rebound");require(game.items().timeStop().stopped(level),"actual source pause boundary restored");require(level.getEntity(ally.getUUID()) instanceof Cow source&&source.entityTags().contains(LbcPsychblindSkill.CLAIM),"native rebound source global claim restored");return true;
                }
                default->throw new IllegalStateException("unknown scheduled stage "+stage);
            }
            return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                ()->{if(kind==2)game.vanity().stop();},()->{if(stopped)game.items().timeStop().end(level);},
                ()->{game.items().rainbowAura().clear(owner);game.items().psychblindSkill().clear(owner.getUUID());game.items().psychblindSkill().clear(other.getUUID());game.items().psycho().stop();},
                ()->{var remove=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!original.contains(e.getUUID())&&(actors.stream().anyMatch(a->a.getUUID().equals(e.getUUID()))||e instanceof LbcPsychblindSkill.Doppel||e instanceof LbcPsycho.Charge))remove.add(e);remove.forEach(Entity::discard);},
                ()->terrain.forEach((p,s)->game.maps().restoreSnapshot(level,p,s,2)),
                ()->{var board=game.server().getScoreboard();board.removePlayerFromTeam(owner.getScoreboardName(),team);board.removePlayerFromTeam(other.getScoreboardName(),team);board.removePlayerTeam(team);if(oldTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldTeam);if(otherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),otherTeam);},
                ()->{game.config().lbcSkills=skills;game.config().ranked=ranked;saved.restoreAttributes();otherSaved.restoreAttributes();saved.restore(game);otherSaved.restore(game);saved.restoreUse();otherSaved.restoreUse();if(!ownerClaim)owner.entityTags().remove(LbcPsychblindSkill.CLAIM);if(!otherClaim)other.entityTags().remove(LbcPsychblindSkill.CLAIM);game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(other);},
                client::close,audience::close,attribution::close))try{action.run();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
            if(failure!=null)throw new IllegalStateException("Aura/Psychblind scoped cleanup",failure);
        }
    }
}
