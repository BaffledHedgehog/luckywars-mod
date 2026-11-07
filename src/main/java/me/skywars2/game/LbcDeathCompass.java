package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

/** Carried Recovery Compass: source one-second check and a real owned, loot-bearing Warden. */
public final class LbcDeathCompass {
    private final GameController game;
    private int phase;
    public LbcDeathCompass(GameController game){this.game=game;}
    static boolean compass(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"death_compass",0)==1;}
    private static boolean carried(ServerPlayer player){
        for(int slot=0;slot<36;slot++)if(compass(player.getInventory().getItem(slot)))return true;
        return compass(player.getOffhandItem());
    }
    static Optional<GlobalPos> deathTarget(ServerPlayer player){
        var own=player.getLastDeathLocation();if(own.isPresent())return own;
        // Author: a never-dead holder borrows another online player's remembered death, never a fake origin.
        // Prefer this dimension, then the nearest death column; stable UUID order resolves equal distances.
        return player.level().getServer().getPlayerList().getPlayers().stream().filter(other->other!=player)
                .sorted(java.util.Comparator.comparing(other->other.getUUID().toString()))
                .map(ServerPlayer::getLastDeathLocation).flatMap(Optional::stream)
                .min(java.util.Comparator.comparingInt((GlobalPos pos)->pos.dimension()==player.level().dimension()?0:1)
                        .thenComparingDouble(pos->{double x=pos.pos().getX()+.5-player.getX(),z=pos.pos().getZ()+.5-player.getZ();return x*x+z*z;}));
    }
    static boolean matchingDeath(ServerPlayer player){
        // Native command integer results floor Pos; the source deliberately ignores death Y and dimension.
        var death=deathTarget(player);
        return death.isPresent()&&Mth.floor(player.getX())==death.get().pos().getX()
                &&Mth.floor(player.getZ())==death.get().pos().getZ();
    }
    private boolean admitted(ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
                &&game.canUseGameplayItems(player)&&!game.isObserver(player)&&game.stage()!=GameController.Stage.ENDING
                &&game.items().wands().validRoleTarget(player,false)
                &&!game.items().wands().inputTimeStopped(player)&&!game.items().timeStop().stopped(player.level())
                &&!player.level().dimension().identifier().getPath().equals("nexus")
                &&!player.level().dimension().identifier().getPath().equals("imprinted");
    }
    public void tick(){
        if(game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        phase=(phase+1)%20;if(phase!=0)return;
        for(var player:game.server().getPlayerList().getPlayers())
            if(admitted(player)&&carried(player)){
                var target=deathTarget(player);
                // A client item-model property reads this synchronized target without rewriting real death history.
                for(int i=0;i<=36;i++){
                    var stack=player.getInventory().getItem(i==36?40:i);if(!compass(stack))continue;
                    var tracker=new LodestoneTracker(target,false);
                    if(!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER)))stack.set(DataComponents.LODESTONE_TRACKER,tracker);
                    if(!LbcTmtItems.matches(stack))stack.set(DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.parse("lbc:death_compass"));
                }
                if(!game.items().magic().beaconSuppressed(player.position())&&target.isPresent()
                        &&Mth.floor(player.getX())==target.get().pos().getX()&&Mth.floor(player.getZ())==target.get().pos().getZ())trigger(player);
            }
    }
    private void trigger(ServerPlayer player){
        var level=player.level();var at=player.position();
        // /clear's real scope is wider than activation (equipment/crafting/cursor); keep its native order.
        player.getInventory().clearOrCountMatchingItems(LbcDeathCompass::compass,false,1,player.inventoryMenu.getCraftSlots());
        player.getInventory().setChanged();
        var spawn=at.add(0,15,0);
        net.minecraft.world.entity.monster.warden.Warden warden=null;
        if(Level.isInSpawnableBounds(BlockPos.containing(spawn))&&level.getChunkSource().getChunkNow(Mth.floor(spawn.x)>>4,Mth.floor(spawn.z)>>4)!=null){
            warden=EntityTypes.WARDEN.create(level,EntitySpawnReason.COMMAND);
            if(warden!=null){
                // Load through the native mob codec; 150 current HP intentionally keeps Warden's 500 maximum.
                var data=new net.minecraft.nbt.CompoundTag();data.putFloat("Health",150);
                data.putString("DeathLootTable","lbc:death_loot");
                warden.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
                warden.getBrain().setMemoryWithExpiry(net.minecraft.world.entity.ai.memory.MemoryModuleType.DIG_COOLDOWN,net.minecraft.util.Unit.INSTANCE,32767L);
                warden.setPos(spawn);warden.entityTags().add("death_mob");
                game.items().creatureDrops().adoptDeathWarden(warden,player);
                if(!level.addFreshEntity(warden)){warden.discard();warden=null;}
            }
        }
        // Independent source commands: a failed summon/spread does not refund the compass or cancel feedback.
        player.addEffect(ItemEffectLevels.create(MobEffects.DARKNESS,600,0),player);
        LbcOverload.sound(level,at,"minecraft:entity.warden.dig",1,1,0,0);
        if(warden!=null)spread(level,warden,level.dimension()==Level.NETHER?120:level.getMaxY()+1,RandomSource.createThreadLocalInstance(),true);
    }
    /** Single-entity vanilla spreadplayers: square samples, first two-air floor, tag/liquid checks and 10000 tries.
     * Cache resident column scans only within this synchronous command; no terrain loads or fallback location. */
    static boolean spread(ServerLevel level,Entity entity,int maxHeight,RandomSource random){
        return spread(level,entity,maxHeight,random,false);
    }
    private static boolean spread(ServerLevel level,Entity entity,int maxHeight,RandomSource random,boolean avoidPlayers){
        if(maxHeight<level.getMinY())return false;
        var center=entity.position();Map<Long,Integer> columns=new HashMap<>();
        int x=Mth.floor(Mth.nextDouble(random,(float)center.x-10,(float)center.x+10));
        int z=Mth.floor(Mth.nextDouble(random,(float)center.z-10,(float)center.z+10));
        for(int attempt=0;attempt<10000;attempt++){
            long key=BlockPos.asLong(x,0,z);
            var cached=columns.get(key);int y=cached==null?spawnY(level,x,z,maxHeight):cached;
            if(cached==null)columns.put(key,y);
            // Vanilla checks its exhausted iteration count before teleport, including a safe final sample.
            if(y!=Integer.MIN_VALUE){
                var box=entity.getBoundingBox().move(x+.5-entity.getX(),y-entity.getY(),z+.5-entity.getZ());
                boolean occupied=false;
                if(avoidPlayers)for(var player:level.players())if(player.isAlive()&&!player.isSpectator()&&player.getBoundingBox().intersects(box)){occupied=true;break;}
                if(!occupied)return attempt<9999&&entity.teleportTo(level,x+.5,y,z+.5,Set.of(),entity.getYRot(),entity.getXRot(),true);
            }
            x=Mth.floor(Mth.nextDouble(random,(float)center.x-10,(float)center.x+10));
            z=Mth.floor(Mth.nextDouble(random,(float)center.z-10,(float)center.z+10));
        }
        return false;
    }
    static int spawnY(ServerLevel level,int x,int z,int maxHeight){
        if(maxHeight<level.getMinY()||!Level.isInSpawnableBounds(new BlockPos(x,0,z))
                ||level.getChunkSource().getChunkNow(x>>4,z>>4)==null)return Integer.MIN_VALUE;
        var pos=new BlockPos.MutableBlockPos(x,maxHeight+1,z);
        boolean air2=level.getBlockState(pos).isAir();pos.move(Direction.DOWN);
        boolean air1=level.getBlockState(pos).isAir();
        while(pos.getY()>level.getMinY()){
            pos.move(Direction.DOWN);var block=level.getBlockState(pos);boolean air=block.isAir();
            if(!air&&air1&&air2)return pos.getY()<maxHeight&&!block.liquid()&&block.is(BlockTags.ENTITIES_CAN_TELEPORT_TO)?pos.getY()+1:Integer.MIN_VALUE;
            air2=air1;air1=air;
        }
        return Integer.MIN_VALUE;
    }
    public void stop(){phase=0;}
}
