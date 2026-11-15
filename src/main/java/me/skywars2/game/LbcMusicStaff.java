package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.*;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Native WILD arrows and a single server-wide held-item music session. */
public final class LbcMusicStaff {
    public static final String FLAG="music_staff";
    public static final Identifier MUSIC=Identifier.parse("skywars2:music_staff");
    public static final Identifier ARROW_MODEL=Identifier.parse("lbc:music_staff/arrow");
    static final int STUDY_TICKS=52*20;
    static final String BASE_MANA="music_staff_mana_use",ACADEMY_MANA="academy_music_staff_mana_use";
    public static final Item ITEM=item(false),ACADEMY=item(true);
    public static final EntityType<Shot> TYPE=type();
    private final GameController game;
    private final Set<Shot> arrows=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerPlayer,Listening> students=new IdentityHashMap<>();
    private final Set<ServerPlayer> audience=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean playing;
    private static final class Listening {final ItemStack stack;int ticks;Listening(ItemStack stack){this.stack=stack;}}
    LbcMusicStaff(GameController game){this.game=game;}
    public static void register(){}
    private static Item item(boolean academy){
        String id=academy?"academy_music_staff":"music_staff";
        var key=Identifier.parse("skywars2:"+id);
        var properties=LbcUtilityItemPrototypes.deferredProperties(key.toString(),"/data/lbc/loot_table/ported/"+id+".json",Items.CARROT_ON_A_STICK,1);
        return Registry.register(BuiltInRegistries.ITEM,key,LbcWands.nativeTool(properties,Items.CARROT_ON_A_STICK));
    }
    private static EntityType<Shot> type(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:music_staff_arrow"));
        var value=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Shot>of(Shot::new,MobCategory.MISC)
                .sized(.5f,.5f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(value);return value;
    }
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&(stack.is(ITEM)||stack.is(ACADEMY)||ItemData.intValue(stack,FLAG,0)==1);}
    public static boolean upgraded(ItemStack stack){return stack.is(ACADEMY)||ItemData.intValue(stack,"upgraded",0)==1;}
    private boolean canHold(ServerPlayer player){return player.isAlive()&&!player.isRemoved()&&!player.isDeadOrDying();}
    private List<Map.Entry<InteractionHand,ItemStack>> held(ServerPlayer player){
        var result=new ArrayList<Map.Entry<InteractionHand,ItemStack>>(2);if(!canHold(player))return result;
        for(var hand:InteractionHand.values()){var stack=player.getItemInHand(hand);if(matches(stack))result.add(Map.entry(hand,stack));}
        return result;
    }
    private void start(ServerPlayer listener){
        listener.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(MUSIC)),SoundSource.MASTER,
                listener.getX(),1_000_000,listener.getZ(),Float.MAX_VALUE,1,0));
    }
    private void stopMusic(){
        var packet=new ClientboundStopSoundPacket(MUSIC,SoundSource.MASTER);
        for(var listener:game.server().getPlayerList().getPlayers())listener.connection.send(packet);
        audience.clear();playing=false;
    }
    /** Held music is independent of casting, mana and the arena's Time Stop. */
    public void tick(){
        var online=game.server().getPlayerList().getPlayers();boolean any=false;
        var active=Collections.newSetFromMap(new IdentityHashMap<ServerPlayer,Boolean>());
        for(var player:online){
            var held=held(player);if(held.isEmpty()){students.remove(player);continue;}any=true;active.add(player);
            var prior=students.get(player);Map.Entry<InteractionHand,ItemStack> candidate=null;
            for(var entry:held)if(LbcAcademy.trial(entry.getValue())&&!upgraded(entry.getValue())
                    &&(candidate==null||prior!=null&&prior.stack==entry.getValue()))candidate=entry;
            if(candidate==null){students.remove(player);continue;}
            var progress=prior!=null&&prior.stack==candidate.getValue()?prior:new Listening(candidate.getValue());
            students.put(player,progress);
            if(++progress.ticks>=STUDY_TICKS){game.items().academy().ready(player,candidate.getKey(),candidate.getValue());students.remove(player);}
        }
        students.keySet().removeIf(player->!active.contains(player));
        if(!any){if(playing)stopMusic();return;}
        if(!playing){playing=true;audience.clear();}
        audience.removeIf(listener->!online.contains(listener));
        for(var listener:online)if(audience.add(listener))start(listener);
    }
    int studyTicks(ServerPlayer player){var state=students.get(player);return state==null?0:state.ticks;}
    boolean playing(){return playing;}
    public boolean use(ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!matches(stack)||!game.playing()||!canHold(player)||game.isObserver(player)||!game.canUseGameplayItems(player)
                ||game.items().actionsBlocked(player)||game.items().wands().inputTimeStopped(player)
                ||game.items().magic().suppressed(player)||game.items().magic().suppressedAt(player.level(),player.getEyePosition())
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands||player.getCooldowns().isOnCooldown(stack))return false;
        var cell=net.minecraft.core.BlockPos.containing(player.getEyePosition());
        if(player.level().isOutsideBuildHeight(cell)||!player.level().hasChunkAt(cell))return false;
        boolean improved=upgraded(stack);
        int cost=Math.max(0,game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly(improved?ACADEMY_MANA:BASE_MANA),"lbc.math").get());
        if(!game.items().mana().trySpendWand(player,cost))return false;
        int count=improved&&player.getRandom().nextInt(100)<52?2:1;
        int spawned=0;
        try(var use=WeaponEnchantments.using(player,stack)){
            var wild=game.items().indirectKills().wildCause(player);
            var cause=new LbcIndirectKills.Owner(wild.player(),wild.order(),true,true,wild.enchantmentWeapon(),true);
            for(int i=0;i<count;i++){
                var shot=new Shot(TYPE,player.level());shot.damage=improved?16:8;shot.weapon=stack.copyWithCount(1);
                shot.setOwner(player);shot.setPos(count==2?LbcBalanceExtras.doubleArrowOrigin(player,i==0?-1:1,true):player.getEyePosition());shot.getSlot(0).set(new ItemStack(Items.ARROW));shot.pickup=Shot.Pickup.DISALLOWED;
                var direction=player.getLookAngle();shot.shoot(direction.x,direction.y,direction.z,3,1);shot.setCritArrow(false);
                game.items().markCurrentItemEpoch(shot);game.items().spellSources().bind(shot,new LbcSpellSource.Source(player.getUUID(),player.getUUID(),game.items().epoch(),cause,Set.of()));
                game.items().timeStop().prepareFrozen(shot);
                if(player.level().addFreshEntity(shot)&&!shot.isRemoved())spawned++;else shot.discard();
            }
        }
        if(spawned==0){game.items().mana().add(player,cost);return false;}
        game.items().wands().wandCasted(player);return true;
    }
    public static final class Shot extends Arrow implements PolymerEntity,FixedArrowDamage {
        int damage=8;ItemStack weapon=ItemStack.EMPTY;ProjectileModel model;
        Shot(EntityType<? extends Arrow> type,Level level){super(type,level);}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public ItemStack getWeaponItem(){return weapon;}
        @Override public float fixedArrowDamage(){return damage;}
        @Override public void addAdditionalSaveData(ValueOutput output){super.addAdditionalSaveData(output);output.putInt("MusicStaffDamage",damage);output.store("MusicStaffWeapon",ItemStack.OPTIONAL_CODEC,weapon);}
        @Override public void readAdditionalSaveData(ValueInput input){super.readAdditionalSaveData(input);damage=input.getIntOr("MusicStaffDamage",8);weapon=input.read("MusicStaffWeapon",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);}
        @Override public void tick(){
            var game=me.skywars2.SkyWars2.game();
            if(game==null||!game.items().currentEpoch(this)){discard();return;}
            if(game.items().timeStop().frozen(this)||game.items().timeStop().stopped((ServerLevel)level())||entityTags().contains("time_stopped"))return;
            super.tick();if(isRemoved())return;
            if(tickCount>=1200){discard();return;}
            if(model==null)model=new ProjectileModel(this,ARROW_MODEL);
            Vec3 visible=position();if(!isInGround()){
                visible=visible.add(getDeltaMovement());var wall=LbcTerra.wallHit((ServerLevel)level(),position(),visible);if(wall!=null)visible=wall.getLocation();
            }
            model.sync(visible);
        }
    }
    public void loaded(Entity entity){if(entity instanceof Shot shot){arrows.add(shot);if(shot.model==null)shot.model=new ProjectileModel(shot,ARROW_MODEL);}}
    public void unloaded(Entity entity){if(entity instanceof Shot shot){arrows.remove(shot);if(shot.model!=null){shot.model.close();shot.model=null;}}}
    List<Shot> arrows(){return List.copyOf(arrows);}
    public void forget(ServerPlayer player){students.remove(player);audience.remove(player);if(game.server().getPlayerList().getPlayers().stream().noneMatch(p->p!=player&&!held(p).isEmpty())&&playing)stopMusic();}
    public void stop(){if(playing)stopMusic();students.clear();for(var shot:List.copyOf(arrows)){unloaded(shot);shot.discard();}}
}
