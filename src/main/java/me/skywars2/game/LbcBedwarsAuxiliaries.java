package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** Indexed source generators, team beacon leases and native shared chest/zombie ownership. */
public final class LbcBedwarsAuxiliaries {
    public interface Policy {
        String epoch(); boolean active(); boolean canUse(ServerPlayer player);
        int team(ServerPlayer player); PlayerTeam playerTeam(ServerPlayer player); boolean hasBed(int team);
        default void retiredGenerator(UUID id) { }
    }
    public enum Kind {
        COPPER(10,Items.COPPER_INGOT),IRON(100,Items.IRON_INGOT),GOLD(200,Items.GOLD_INGOT),DIAMOND(300,Items.DIAMOND),STAR(300,Items.NETHER_STAR);
        final int interval;final Item item;
        Kind(int interval,Item item){this.interval=interval;this.item=item;}
    }
    public record Generator(UUID id,ServerLevel level,Vec3 at,Kind kind) { }
    private static final class Generation {
        final Generator marker;long due;boolean retired;
        Generation(Generator marker,long due){this.marker=marker;this.due=due;}
        Generation(Generation old){this(old.marker,old.due);retired=old.retired;}
    }
    private record Site(ServerLevel level,BlockPos pos) { Site{pos=pos.immutable();} }
    private record Chest(int team,UUID display) { }
    private record Child(UUID owner,int team) { }
    private static final class Output {
        final ServerLevel level;Vec3 at;final UUID recipient;final UUID prune;ItemStack stack;
        Output(ServerLevel level,Vec3 at,UUID recipient,UUID prune,ItemStack stack){this.level=level;this.at=at;this.recipient=recipient;this.prune=prune;this.stack=stack;}
        Output(Output old){this(old.level,old.at,old.recipient,old.prune,old.stack.copy());}
    }
    /** A canonical native inventory root. All viewers hold this same object. */
    public final class SharedContainer extends SimpleContainer {
        final int team;final String epoch;
        SharedContainer(int team){super(54);this.team=team;epoch=policy.epoch();}
        boolean current(){return Objects.equals(epoch,policy.epoch())&&storage.get(team)==this;}
        void validate(ServerPlayer player,boolean current){
            if(current&&(!current()||policy.team(player)!=team))throw new IllegalStateException("Restore canonical BedWars team storage before its viewers");
        }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return player instanceof ServerPlayer p&&current()&&policy.active()&&policy.team(p)==team;}
    }
    private final GameController game;private final Policy policy;
    private final List<Generation> generators=new ArrayList<>();private final Map<UUID,Generation> byId=new LinkedHashMap<>();
    private final Map<ServerLevel,Long> clocks=new IdentityHashMap<>();
    private final ArrayDeque<Output> outputs=new ArrayDeque<>();
    private final Map<Integer,SharedContainer> storage=new LinkedHashMap<>();
    private final Map<Site,Chest> chests=new LinkedHashMap<>();
    private final Deque<Site> playerBreaks=new ArrayDeque<>();
    private final ArrayDeque<Site> pendingDisplays=new ArrayDeque<>();
    private final Map<UUID,Child> zombies=new LinkedHashMap<>();
    private final Map<Integer,EnumMap<LbcBedwarsShop.Beacon,Integer>> beacons=new LinkedHashMap<>();
    private final Set<UUID> effectRecipients=new LinkedHashSet<>();
    private String epoch;private int cursor,seconds;private boolean restoring,publishing;
    public LbcBedwarsAuxiliaries(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);epoch=policy.epoch();}
    public void initialize(Collection<Generator> markers){
        stop();epoch=policy.epoch();
        for(var marker:markers){if(byId.containsKey(marker.id()))throw new IllegalArgumentException("Duplicate native generator UUID");var g=new Generation(marker,marker.kind.interval);generators.add(g);byId.put(marker.id,g);clocks.putIfAbsent(marker.level,0L);}
    }
    /** Same-match map renewal retains canonical player-owned resources and accepted work. */
    public void replaceMap(Collection<Generator> markers){
        if(!Objects.equals(epoch,policy.epoch()))throw new IllegalStateException("BedWars map renewal must retain the canonical match epoch");
        for(var entry:chests.entrySet())if(entry.getValue().display!=null){var e=entry.getKey().level.getEntity(entry.getValue().display);if(e!=null)e.discard();}
        chests.clear();pendingDisplays.clear();generators.clear();byId.clear();cursor=0;
        for(var marker:markers){
            if(byId.containsKey(marker.id()))throw new IllegalArgumentException("Duplicate native generator UUID");
            long now=clocks.computeIfAbsent(marker.level,ignored->0L);
            var g=new Generation(marker,now-now%marker.kind.interval+marker.kind.interval);
            generators.add(g);byId.put(marker.id,g);
        }
        // Native permanent-removal callbacks retire purged zombie IDs. Unloaded or
        // surviving bodies retain their same immutable owner/team metadata.
    }
    public List<SharedContainer> containers(){return List.copyOf(storage.values());}
    public SharedContainer container(int team){if(team<1||team>16)throw new IllegalArgumentException("BedWars team1..16");return storage.computeIfAbsent(team,SharedContainer::new);}
    private boolean live(ServerPlayer player){return !restoring&&policy.active()&&Objects.equals(epoch,policy.epoch())&&policy.canUse(player)&&!game.isObserver(player)&&policy.team(player)>=1&&policy.team(player)<=16;}
    private boolean paused(ServerLevel level){return game.items().timeStop().stopped(level);}
    public static ItemStack teamChestItem(){return marked(Items.ENDER_CHEST,"skywars2.bedwars.team_chest","team_chest");}
    public static ItemStack teamZombieItem(){return marked(Items.ZOMBIE_SPAWN_EGG,"skywars2.bedwars.team_zombie","team_zombie");}
    public ItemStack teamChest(ServerPlayer player){return teamChestItem();}
    public ItemStack teamZombie(ServerPlayer player){return teamZombieItem();}
    private static ItemStack marked(Item item,String flag,String key){var s=new ItemStack(item);var tag=new CompoundTag();tag.putInt(flag,1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));s.set(DataComponents.ITEM_NAME,Component.translatable(key));return s;}
    public static boolean chestItem(ItemStack stack){return stack.is(Items.ENDER_CHEST)&&ItemData.intValue(stack,"skywars2.bedwars.team_chest",0)==1;}
    public static boolean zombieItem(ItemStack stack){return stack.is(Items.ZOMBIE_SPAWN_EGG)&&(ItemData.intValue(stack,"skywars2.bedwars.team_zombie",0)==1||stack.get(DataComponents.ITEM_NAME)!=null&&stack.get(DataComponents.ITEM_NAME).getContents() instanceof TranslatableContents t&&t.getKey().equals("team_zombie"));}
    public void tick(){
        if(restoring||!policy.active()||!Objects.equals(epoch,policy.epoch())||!game.server().tickRateManager().runsNormally())return;
        for(var level:clocks.keySet())if(!paused(level))clocks.put(level,clocks.get(level)+1);
        int visits=Math.min(128,generators.size());
        while(visits-->0&&!generators.isEmpty()){
            if(cursor>=generators.size())cursor=0;var g=generators.get(cursor++);var marker=g.marker;
            if(g.retired||paused(marker.level)||!marker.level.hasChunkAt(BlockPos.containing(marker.at))||clocks.get(marker.level)<g.due)continue;
            g.due+=marker.kind.interval;generate(g);
        }
        int budget=Math.min(128,outputs.size());while(budget-->0){var output=outputs.removeFirst();if(paused(output.level)){outputs.addLast(output);continue;}if(!deliver(output))outputs.addLast(output);}
        // Purchases/timers belong to the active match world, sharing its native TimeStop.
        if(!paused(game.maps().level())&&++seconds>=20){seconds=0;beaconSecond();}
        int displays=Math.min(32,pendingDisplays.size());while(displays-->0){var site=pendingDisplays.removeFirst();var chest=chests.get(site);if(chest!=null&&chest.display==null){if(paused(site.level)||!display(site,chest.team))pendingDisplays.addLast(site);}}
    }
    private void generate(Generation g){
        var m=g.marker;var players=m.level.getEntitiesOfClass(ServerPlayer.class,new net.minecraft.world.phys.AABB(m.at,m.at).inflate(1.5),p->live(p)&&p.position().distanceToSqr(m.at)<=2.25);
        if(!players.isEmpty()){for(var player:players)outputs.addLast(new Output(m.level,m.at,player.getUUID(),null,new ItemStack(m.kind.item)));return;}
        outputs.addLast(new Output(m.level,m.at.add(0,.5,0),null,m.id,new ItemStack(m.kind.item)));
    }
    private boolean deliver(Output output){
        if(!output.level.hasChunkAt(BlockPos.containing(output.at)))return false;
        if(output.recipient!=null){var player=game.server().getPlayerList().getPlayer(output.recipient);
            if(player!=null&&live(player)&&player.level()==output.level){player.getInventory().add(output.stack);if(output.stack.isEmpty())return true;output.at=player.position();}}
        if(output.stack.isEmpty())return true;
        var item=new ItemEntity(output.level,output.at.x,output.at.y,output.at.z,output.stack.copy());item.setDefaultPickUpDelay();game.items().markCurrentItemEpoch(item);
        publishing=true;boolean added;try{added=output.level.addFreshEntity(item);}finally{publishing=false;}
        if(!added)return false;output.stack=ItemStack.EMPTY;
        if(output.prune!=null)prune(output.prune);return true;
    }
    private void prune(UUID id){
        var g=byId.get(id);if(g==null||g.retired)return;int same=0;
        for(var other:generators)if(!other.retired&&other.marker.level==g.marker.level&&other.marker.kind==g.marker.kind&&other.marker.at.distanceToSqr(g.marker.at)<=1)same++;
        if(same>=2){g.retired=true;policy.retiredGenerator(id);}
    }
    public void beacon(ServerPlayer player,LbcBedwarsShop.Beacon beacon,int duration){
        if(!live(player)||duration<=0)return;
        var team=beacons.computeIfAbsent(policy.team(player),ignored->new EnumMap<>(LbcBedwarsShop.Beacon.class));
        team.merge(beacon,duration,(old,next)->Math.addExact(old,next));
    }
    private static Holder<MobEffect> effect(LbcBedwarsShop.Beacon b){return switch(b){case SPEED2->MobEffects.SPEED;case JUMP2->MobEffects.JUMP_BOOST;case FIRE_RES->MobEffects.FIRE_RESISTANCE;case STRENGTH2->MobEffects.STRENGTH;case HASTE2->MobEffects.HASTE;case REGEN2->MobEffects.REGENERATION;case RESISTANCE1->MobEffects.RESISTANCE;};}
    private static int amplifier(LbcBedwarsShop.Beacon b){return switch(b){case JUMP2->2;case FIRE_RES,RESISTANCE1->0;default->1;};}
    private static String effectKey(int team){return "bedwars.beacon."+team;}
    private void beaconSecond(){
        for(var team:beacons.entrySet()){
            // Native source manager decrements remaining time before refreshing effects.
            team.getValue().replaceAll((kind,left)->Math.max(0,left-1));
            if(!policy.hasBed(team.getKey()))continue;
            for(var player:game.server().getPlayerList().getPlayers())if(policy.team(player)==team.getKey()&&!game.isObserver(player)){
                effectRecipients.add(player.getUUID());for(var buff:team.getValue().entrySet())if(buff.getValue()>0)game.items().ownedEffects().grant(player,effectKey(team.getKey()),new MobEffectInstance(effect(buff.getKey()),Math.multiplyExact(buff.getValue(),20),amplifier(buff.getKey()),true,false,true),null);
            }
        }
    }
    /** Called only after native marked chest placement succeeded; vanilla owns the used-hand debit. */
    public boolean placed(ServerPlayer player,ServerLevel level,BlockPos pos,ItemStack placed){
        if(!chestItem(placed))return false;
        if(!live(player)||!level.getBlockState(pos).is(Blocks.ENDER_CHEST))return false;
        var site=new Site(level,pos);if(chests.containsKey(site))return true;int team=policy.team(player);container(team);chests.put(site,new Chest(team,null));if(!display(site,team))pendingDisplays.addLast(site);return true;
    }
    private boolean display(Site site,int team){
        if(!site.level.hasChunkAt(site.pos)||!site.level.getBlockState(site.pos).is(Blocks.ENDER_CHEST))return false;
        var body=new BlockDisplay(EntityTypes.BLOCK_DISPLAY,site.level);body.setPos(site.pos.getX(),site.pos.getY(),site.pos.getZ());body.setBlockState(Blocks.STAINED_GLASS.pick(dye(team)).defaultBlockState());body.entityTags().add("skywars2.bedwars.chest_visual");game.items().markCurrentItemEpoch(body);
        if(site.level.addFreshEntity(body)){chests.put(site,new Chest(team,body.getUUID()));return true;}body.discard();return false;
    }
    private static DyeColor dye(int team){return List.of(DyeColor.LIGHT_BLUE,DyeColor.BLACK,DyeColor.BLUE,DyeColor.CYAN,DyeColor.PURPLE,DyeColor.GRAY,DyeColor.GREEN,DyeColor.MAGENTA,DyeColor.BROWN,DyeColor.ORANGE,DyeColor.LIGHT_GRAY,DyeColor.LIME,DyeColor.PINK,DyeColor.RED,DyeColor.WHITE,DyeColor.YELLOW).get(team-1);}
    /** True consumes an owned chest interaction, including enemy denial. */
    public boolean open(ServerPlayer player,InteractionHand hand,ServerLevel level,BlockPos pos){
        var chest=chests.get(new Site(level,pos));if(chest==null)return false;
        if(hand!=InteractionHand.MAIN_HAND||!live(player)||paused(level)||policy.team(player)!=chest.team)return true;
        player.openMenu(new SimpleMenuProvider((id,inv,p)->ChestMenu.sixRows(id,inv,container(chest.team)),Component.translatable("team_chest")));return true;
    }
    public boolean chest(ServerLevel level,BlockPos pos){return chests.containsKey(new Site(level,pos));}
    /** The original site remains scoped after Level has already removed its association. */
    public boolean withPlayerBreak(ServerPlayer player,BlockPos pos,Supplier<Boolean> action){
        var site=new Site(player.level(),pos);
        if(!chests.containsKey(site)||!site.level.getBlockState(pos).is(Blocks.ENDER_CHEST))return action.get();
        playerBreaks.push(site);
        try{return action.get();}finally{playerBreaks.pop();}
    }
    public boolean suppressMiningDrops(ServerLevel level,BlockPos pos){return playerBreaks.contains(new Site(level,pos));}
    /** Called only for accepted native material writes, including environment writers. */
    public void blockChanged(ServerLevel level,BlockPos pos,BlockState state){
        if(restoring||state.is(Blocks.ENDER_CHEST))return;
        var site=new Site(level,pos);if(!chests.containsKey(site))return;
        removed(level,pos,playerBreaks.contains(site));
    }

    /** Native successful break calls this once while suppressing ordinary obsidian/ender-chest drops. */
    public void removed(ServerLevel level,BlockPos pos,boolean playerBreak){
        var site=new Site(level,pos);var old=chests.remove(site);if(old==null)return;
        if(old.display!=null){var e=level.getEntity(old.display);if(e!=null)e.discard();}
        if(playerBreak){var stack=teamChestItem();stack.remove(DataComponents.ITEM_NAME);stack.set(DataComponents.CUSTOM_NAME,Component.translatable("team_chest"));var output=new Output(level,Vec3.atCenterOf(pos),null,null,stack);if(!deliver(output))outputs.addLast(output);}
    }
    /** Handles rejected marked eggs too: callers must not fall through to vanilla spawn. */
    public boolean useZombie(ServerPlayer player,InteractionHand hand,BlockPos clicked,Direction face){
        var held=player.getItemInHand(hand);if(!zombieItem(held))return false;
        if(!live(player)||paused(player.level())||game.items().wands().inputTimeStopped(player))return true;
        Vec3 at=clicked==null?player.position().add(player.getLookAngle().scale(1.5)):Vec3.atBottomCenterOf(clicked.relative(face));
        if(!player.level().hasChunkAt(BlockPos.containing(at)))return true;
        var zombie=new Zombie(EntityTypes.ZOMBIE,player.level());zombie.setPos(at);zombie.setPersistenceRequired();zombie.setCanPickUpLoot(false);
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND)){
            Item item=switch(slot){case HEAD->Items.IRON_HELMET;case CHEST->Items.IRON_CHESTPLATE;case LEGS->Items.IRON_LEGGINGS;case FEET->Items.IRON_BOOTS;default->Items.IRON_SWORD;};
            var stack=new ItemStack(item);stack.set(DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE);zombie.setItemSlot(slot,stack);zombie.setDropChance(slot,0);
        }
        var team=policy.playerTeam(player);if(team==null)return true;
        zombie.setCustomName(Component.translatable("entity.minecraft.zombie").withColor(LbcTeamColors.color(policy.team(player)).rgb()));zombie.setCustomNameVisible(true);zombie.entityTags().add("bw-owner:"+player.getUUID());zombie.entityTags().add("skywars2.bedwars.zombie");zombie.entityTags().add("skywars2.bedwars.team:"+policy.team(player));game.items().markCurrentItemEpoch(zombie);
        if(!player.level().addFreshEntity(zombie)){zombie.discard();return true;}
        game.server().getScoreboard().addPlayerToTeam(zombie.getScoreboardName(),team);zombies.put(zombie.getUUID(),new Child(player.getUUID(),policy.team(player)));held.shrink(1);return true;
    }
    /** Derived load/clone index; persistent semantic tags remain the native source of truth. */
    public void loaded(Entity body){
        if(restoring||MatchActorCheckpoint.restoring(body)||!(body instanceof Zombie)||!body.entityTags().contains("skywars2.bedwars.zombie"))return;
        if(!game.items().currentEpoch(body)||!Objects.equals(epoch,policy.epoch())){zombies.remove(body.getUUID());body.discard();return;}
        UUID owner=null;int team=0;
        for(var tag:body.entityTags())try{
            if(tag.startsWith("bw-owner:"))owner=UUID.fromString(tag.substring("bw-owner:".length()));
            else if(tag.startsWith("skywars2.bedwars.team:"))team=Integer.parseInt(tag.substring("skywars2.bedwars.team:".length()));
        }catch(IllegalArgumentException invalid){return;}
        if(owner==null)return;
        if(team<1||team>16){var player=game.server().getPlayerList().getPlayer(owner);if(player!=null)team=policy.team(player);}
        if(team<1||team>16)return;
        zombies.put(body.getUUID(),new Child(owner,team));
        // Reattach actual native team after an NBT clone/load, without inventing AI.
        var player=game.server().getPlayerList().getPlayer(owner);var nativeTeam=player==null?null:policy.playerTeam(player);
        if(nativeTeam!=null)game.server().getScoreboard().addPlayerToTeam(body.getScoreboardName(),nativeTeam);
    }
    public void unloaded(Entity body){
        if(restoring||MatchActorCheckpoint.restoring(body))return;
        var reason=body.getRemovalReason();if(reason!=null&&reason.shouldDestroy())zombies.remove(body.getUUID());
    }
    public ServerPlayer owner(Entity body){var child=zombies.get(body.getUUID());return child==null||!game.items().currentEpoch(body)||!Objects.equals(epoch,policy.epoch())?null:game.server().getPlayerList().getPlayer(child.owner);}
    public boolean canAttack(Entity actor,Entity target){var child=zombies.get(actor.getUUID());if(child==null)return true;var owner=owner(actor);return owner!=null&&policy.active()&&!paused((ServerLevel)actor.level())&&game.items().wands().target(owner,target);}
    public void targetChanged(Mob mob,LivingEntity target){if(target!=null&&zombies.containsKey(mob.getUUID())&&!canAttack(mob,target))mob.setTarget(null);}
    public void stop(){
        for(var player:game.server().getPlayerList().getPlayers())if(player.containerMenu instanceof ChestMenu menu&&menu.getContainer() instanceof LbcBedwarsAuxiliaries.SharedContainer c&&storage.containsValue(c))player.closeContainer();
        for(var entry:chests.entrySet())if(entry.getValue().display!=null){var e=entry.getKey().level.getEntity(entry.getValue().display);if(e!=null)e.discard();}
        for(var id:zombies.keySet())for(var level:game.server().getAllLevels()){var e=level.getEntity(id);if(e!=null)e.discard();}
        for(var id:effectRecipients){var p=game.server().getPlayerList().getPlayer(id);if(p!=null)for(int team=1;team<=16;team++)game.items().ownedEffects().withdraw(p,effectKey(team));}
        storage.values().forEach(SimpleContainer::clearContent);storage.clear();generators.clear();byId.clear();clocks.clear();outputs.clear();chests.clear();pendingDisplays.clear();zombies.clear();beacons.clear();effectRecipients.clear();cursor=seconds=0;restoring=false;
    }
    /** Canonical container values are captured once, independently of zero/one/multiple open viewers. */
    public StorageImage storageImage(){return new StorageImage();}
    public final class StorageImage {
        private final Map<Integer,SharedContainer> roots=new LinkedHashMap<>(storage);
        private final Map<Integer,List<ItemStack>> values=new LinkedHashMap<>();
        private StorageImage(){roots.forEach((team,c)->{var stacks=new ArrayList<ItemStack>();for(int i=0;i<54;i++)stacks.add(c.getItem(i).copy());values.put(team,List.copyOf(stacks));});}
        public void restoreStorageFrozen(){
            for(var c:storage.values())if(!roots.containsValue(c))c.clearContent();storage.clear();storage.putAll(roots);
            roots.forEach((team,c)->{for(int i=0;i<54;i++)c.setItem(i,values.get(team).get(i).copy());});
        }
    }
    public void prepareFrozen(){if(publishing||!playerBreaks.isEmpty())throw new IllegalStateException("BedWars checkpoint during native publication/break");restoring=true;}
    public void publishFrozen(){restoring=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){if(publishing||!playerBreaks.isEmpty())throw new IllegalStateException("BedWars checkpoint during native publication/break");return new Checkpoint(links);}
    final class Checkpoint {
        final List<Generation> gens=generators.stream().map(Generation::new).toList();final Map<ServerLevel,Long> time=new IdentityHashMap<>(clocks);
        final List<Output> queued=outputs.stream().map(Output::new).toList();final Map<Site,Chest> sites=new LinkedHashMap<>(chests);final Map<UUID,Child> children=new LinkedHashMap<>(zombies);
        final List<Site> displayQueue=List.copyOf(pendingDisplays);final Map<Integer,EnumMap<LbcBedwarsShop.Beacon,Integer>> buffs=new LinkedHashMap<>();final Set<UUID> recipients=Set.copyOf(effectRecipients);
        final Map<UUID,MatchDamageSources.Ref> bodies=new LinkedHashMap<>();final String savedEpoch=epoch;final int savedCursor=cursor,savedSeconds=seconds;
        Checkpoint(MatchSpellCheckpoint.Links links){beacons.forEach((team,map)->buffs.put(team,new EnumMap<>(map)));for(var site:sites.entrySet())if(site.getValue().display!=null){var e=site.getKey().level.getEntity(site.getValue().display);if(e!=null)bodies.put(e.getUUID(),links.key(e));}for(var id:children.keySet())for(var level:game.server().getAllLevels()){var e=level.getEntity(id);if(e!=null)bodies.put(id,links.key(e));}}
        Runnable prepare(MatchSpellCheckpoint.Links links){for(var body:bodies.values())links.get(body);return ()->{generators.clear();byId.clear();for(var old:gens){var g=new Generation(old);generators.add(g);byId.put(g.marker.id,g);}clocks.clear();clocks.putAll(time);outputs.clear();queued.forEach(q->outputs.addLast(new Output(q)));chests.clear();chests.putAll(sites);pendingDisplays.clear();pendingDisplays.addAll(displayQueue);zombies.clear();zombies.putAll(children);beacons.clear();buffs.forEach((team,map)->beacons.put(team,new EnumMap<>(map)));effectRecipients.clear();effectRecipients.addAll(recipients);epoch=savedEpoch;cursor=savedCursor;seconds=savedSeconds;};}
    }
    public List<Generator> generators(){return generators.stream().filter(g->!g.retired).map(g->g.marker).toList();}
    int team(ServerPlayer player){return policy.team(player);}
    int pendingOutputs(){return outputs.size();}
    int beaconSeconds(int team,LbcBedwarsShop.Beacon kind){return beacons.getOrDefault(team,new EnumMap<>(LbcBedwarsShop.Beacon.class)).getOrDefault(kind,0);}
}
