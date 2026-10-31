package me.skywars2.game;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** All ten original Imba leaves. Native tags, attributes, scores and the shared ray queue own state. */
public final class LbcImbaRunes {
    public enum Kind { FEHU,URUZ,THURISAZ,ANSUZ,RAIDO,KENAZ,GIFU,WUNJO,HAGALAZ,NAUTIZ;
        String flag(){return "rune_"+name().toLowerCase(java.util.Locale.ROOT);}
    }
    /** Match/map ownership stays with GameController and MapManager. */
    public interface GlobalActions {
        void clearMap(ServerPlayer author);
        void restart(ServerPlayer author);
        void addLives(ServerPlayer author,int amount);
    }
    static final Identifier POSITIVE=Identifier.parse("minecraft:rune_thurisaz"),NEGATIVE=Identifier.parse("minecraft:rune_thurisaz_negative");
    private static final List<String> POS=List.of("scale","attack_damage","attack_knockback","explosion_knockback_resistance","fall_damage_multiplier","jump_strength","knockback_resistance","max_health","movement_efficiency","movement_speed","safe_fall_distance","step_height","water_movement_efficiency","block_break_speed","block_interaction_range","entity_interaction_range","mining_efficiency","gravity");
    private static final List<String> NEG=List.of("scale","attack_damage","jump_strength","max_health","movement_speed","step_height","fall_damage_multiplier","block_break_speed","block_interaction_range","entity_interaction_range","gravity");
    private final GameController game;private final GlobalActions globals;private final boolean artifactSuppressesAll;
    public LbcImbaRunes(GameController game,GlobalActions globals,boolean artifactSuppressesAll){this.game=game;this.globals=globals;this.artifactSuppressesAll=artifactSuppressesAll;}
    public static Kind kind(ItemStack stack){if(stack.isEmpty())return null;var d=LbcFartus.data(stack);
        if(d.getIntOr("spell",0)!=1||d.getIntOr("raycastspell",0)!=1)return null;
        for(var k:Kind.values())if(d.getIntOr(k.flag(),0)==1)return k;return null;}
    public static boolean matches(ItemStack stack){return kind(stack)!=null;}
    private boolean admitted(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
        &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&LbcDimensionItems.ordinary(p)&&!game.items().actionsBlocked(p)
        &&game.items().wands().validRoleTarget(p,false)&&!game.items().wands().inputTimeStopped(p)&&!game.items().wands().timeStopped()
        &&!game.items().timeStop().stopped(p.level())&&!beacon(p.level(),p.position())
        &&(!artifactSuppressesAll||!game.items().magic().suppressed(p));}
    private boolean beacon(ServerLevel level,Vec3 point){return level==game.maps().level()&&game.items().magic().beaconSuppressed(point);}
    public boolean use(ServerPlayer p,InteractionHand hand){var stack=p.getItemInHand(hand);var k=kind(stack);if(k==null||!admitted(p))return false;
        if(k==Kind.FEHU||k==Kind.GIFU){var direction=p.getLookAngle();var from=p.getEyePosition().add(direction.scale(hand==InteractionHand.OFF_HAND?.5:0));
            game.items().exasperato().replayableSamples(p,from,direction,200,new RuneRay(p,hand,stack,k,direction));return true;}
        debit(p,stack);
        switch(k){
            case URUZ->{p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1e100);p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,-1,4,false,false),p);}
            case THURISAZ->giant(p);
            case ANSUZ->p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            case RAIDO->{game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("bottom_nexus"),"lbc.math").set(1);title("bottom_now_nexus",false);}
            case KENAZ->{nether(p);title("kysinlava",false);}
            case WUNJO->{globals.addLives(p,16);p.level().getGameRules().set(GameRules.KEEP_INVENTORY,true,game.server());title("keepinventory",true);}
            case HAGALAZ->{globals.clearMap(p);for(var e:entities(p.level()))if(e instanceof ItemEntity)e.discard();title("hagalaz",false);}
            case NAUTIZ->{title("new_game",true);for(var target:List.copyOf(game.server().getPlayerList().getPlayers())){target.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,200,4,false,false),p);target.teleportTo(p.level(),0,1000,0,TeleportMomentum.positionFlags(Set.of()),target.getYRot(),target.getXRot(),true);}
                p.level().getGameRules().set(GameRules.FALL_DAMAGE,false,game.server());globals.restart(p);
                for(int n=0;n<4;n++)for(var e:List.copyOf(entities(p.level())))if(!(e instanceof ServerPlayer)&&!(e instanceof net.minecraft.world.entity.Marker)){
                    if(e instanceof LivingEntity living)game.items().indirectKills().inheritedExecution(living,game.items().indirectKills().wildCause(p));else e.kill(p.level());}}
            default->throw new IllegalStateException("Unexpected immediate rune "+k);
        }return true;
    }
    private static List<Entity> entities(ServerLevel level){var out=new java.util.ArrayList<Entity>();level.getAllEntities().forEach(out::add);return out;}
    private void debit(ServerPlayer p,ItemStack stack){stack.shrink(1);p.getInventory().setChanged();game.items().magic().inventoryChanged(p);p.containerMenu.broadcastChanges();}
    private void title(String key,boolean positive){for(var p:game.server().getPlayerList().getPlayers()){
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(key).withStyle(positive?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.DARK_RED)));}}
    private void giant(ServerPlayer p){for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(50),e->e!=p&&e.distanceToSqr(p)>=.0001&&e.distanceToSqr(p)<=2500&&harmful(p,e)))modifiers(target,false);
        modifiers(p,true);}
    /** The cast alone has the source same-team predicate; Greed is not an attribute-modifier gate. */
    private boolean harmful(ServerPlayer owner,LivingEntity target){return game.items().wands().validRoleTarget(target,false)
        &&!target.entityTags().contains("strict_map_object")&&!sameTeam(owner,target);}
    private boolean sameTeam(ServerPlayer owner,LivingEntity target){var board=game.server().getScoreboard();var objective=board.getObjective("team_number");
        if(objective!=null){var a=board.getPlayerScoreInfo(owner,objective);var b=board.getPlayerScoreInfo(target,objective);if(a!=null&&b!=null&&a.value()==b.value())return true;}
        return target instanceof ServerPlayer p&&owner.isAlliedTo(p);}
    static void modifiers(LivingEntity target,boolean positive){for(String name:positive?POS:NEG){var a=target.getAttribute(BuiltInRegistries.ATTRIBUTE.get(Identifier.withDefaultNamespace(name)).orElseThrow());if(a==null)continue;
        var id=positive?POSITIVE:NEGATIVE;if(a.hasModifier(id))continue;
        double amount=positive?(name.equals("fall_damage_multiplier")?-.5:name.endsWith("interaction_range")?2:1)
            :switch(name){case "jump_strength","gravity"->-.875;case "movement_speed"->-.8;case "fall_damage_multiplier"->-1;default->-.9375;};
        a.addPermanentModifier(new AttributeModifier(id,amount,positive&&name.equals("attack_knockback")?AttributeModifier.Operation.ADD_VALUE:AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
        target.entityTags().add(positive?"gigantic":"submissive");}
    private void nether(ServerPlayer p){var destination=game.server().getLevel(Level.NETHER);if(destination==null)return;
        for(var e:entities(p.level()))if(e!=p&&e.position().distanceToSqr(p.position())>=.0001&&!e.entityTags().contains("core_marker")&&!e.entityTags().contains("strict_map_object")
            &&(!(e instanceof LivingEntity living)||game.items().wands().validRoleTarget(living,false))){var moved=e.teleport(new TeleportTransition(destination,e.position(),e.getDeltaMovement(),e.getYRot(),e.getXRot(),TeleportTransition.PLACE_PORTAL_TICKET));
            if(moved instanceof LivingEntity living)game.items().indirectKills().teleported(living,p,true);}}
    /** Original players_tick crush and players_1s bottom-Nexus consumer; native tags/scores are canonical. */
    public void tick(){if(!game.playing()||game.items().wands().timeStopped())return;
        boolean bottom=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("bottom_nexus"),"lbc.math").get()==1;
        for(var p:List.copyOf(game.server().getPlayerList().getPlayers())){if(!p.isAlive()||game.isObserver(p)||!game.canUseGameplayItems(p)||game.items().timeStop().stopped(p.level())||game.items().wands().inputTimeStopped(p))continue;
            if(p.entityTags().contains("gigantic"))for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(1),e->e.entityTags().contains("submissive")&&e.distanceToSqr(p)<=1&&game.items().wands().validRoleTarget(e,false)))
                target.hurtServer(p.level(),game.items().indirectKills().captureDamage(new net.minecraft.world.damagesource.DamageSource(p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.FALLING_ANVIL),p),game.items().indirectKills().wildCause(p)),100);
            if(bottom&&game.server().getTickCount()%20==0&&p.getY()<=-100&&LbcDimensionItems.ordinary(p))game.items().hedgehogDimensions().nexus(p,p,p.getRandom());
        }}
    public void stop(){for(var level:game.server().getAllLevels())for(var e:entities(level))if(e instanceof LivingEntity living)reset(living);
        game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("bottom_nexus"),"lbc.math").set(0);}
    static void reset(LivingEntity target){for(String name:POS){var a=target.getAttribute(BuiltInRegistries.ATTRIBUTE.get(Identifier.withDefaultNamespace(name)).orElseThrow());if(a!=null){a.removeModifier(POSITIVE);a.removeModifier(NEGATIVE);}}
        target.entityTags().remove("gigantic");target.entityTags().remove("submissive");}
    private final class RuneRay implements LbcExasperato.ReplayableRay {
        final UUID owner;final ServerLevel level;final String epoch;final InteractionHand hand;final Kind kind;final Vec3 direction;ItemStack stack;
        RuneRay(ServerPlayer p,InteractionHand hand,ItemStack stack,Kind kind,Vec3 direction){owner=p.getUUID();level=p.level();epoch=game.items().epoch();this.hand=hand;this.stack=stack;this.kind=kind;this.direction=direction;}
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return !state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse("minecraft:airs")));}
        public boolean target(Entity e){return e instanceof LivingEntity&&!e.getUUID().equals(owner)&&game.items().wands().validRoleTarget(e,false);}
        public void trail(Vec3 from,Vec3 to){}
        public double sampleStep(){return kind==Kind.FEHU?1:.33;}
        public HitResult sampleAt(Vec3 at){
            if(artifactSuppressesAll&&game.items().magic().artifactSuppressedAt(level,at))return null;
            if(kind==Kind.GIFU)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,true,false,at.x,at.y,at.z,2,.04,.04,.04,0);
            else level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR,net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN.defaultBlockState()),true,false,at.x,at.y,at.z,2,.04,.04,.04,0);
            if(block(BlockPos.containing(at),level.getBlockState(BlockPos.containing(at))))return terminal(kind==Kind.GIFU?at.subtract(direction.scale(.33)):at);
            double radius=kind==Kind.FEHU?15:7;
            if(!level.getEntities((Entity)null,new AABB(at,at.add(1,1,1)),e->target(e)&&e.position().distanceToSqr(at)<=radius*radius).isEmpty())return terminal(at);
            return null;
        }
        private HitResult terminal(Vec3 at){return new BlockHitResult(at,net.minecraft.core.Direction.getApproximateNearest(direction),BlockPos.containing(at),false);}
        public boolean sampleContinues(Vec3 at){var p=game.server().getPlayerList().getPlayer(owner);if(p==null||artifactSuppressesAll&&(game.items().magic().suppressed(p)||game.items().magic().artifactSuppressedAt(level,at)))return false;
            double radius=kind==Kind.FEHU?16:7;
            for(var holder:level.players())if(!holder.isSpectator()&&(holder.entityTags().contains("nomagic_active")||game.items().magic().artifactActive(holder))&&holder.position().distanceToSqr(at)<=radius*radius)return false;
            var next=at.add(direction.scale(sampleStep()));return !beacon(level,next)&&(!artifactSuppressesAll||!game.items().magic().artifactSuppressedAt(level,next));
        }
        public void complete(HitResult hit){var p=game.server().getPlayerList().getPlayer(owner);
            if(hit==null||p==null||!game.items().currentEpoch(epoch)||p.level()!=level||!admitted(p)||p.getItemInHand(hand)!=stack||LbcImbaRunes.kind(stack)!=kind)return;
            debit(p,stack);var at=hit.getLocation();
            if(kind==Kind.GIFU){for(int n=0;n<20;n++)for(var loot:game.lbc().rollTable(level,at,p,"lbc:random_glack_1"))level.addFreshEntity(new ItemEntity(level,at.x,at.y,at.z,loot));}
            else game.server().getStructureTemplateManager().get(Identifier.parse("lbc:item_gallery")).ifPresent(template->{var origin=BlockPos.containing(at.add(-19,0,-19));
                game.items().indirectKills().trapChanges(level,game.items().indirectKills().wildCause(p),()->game.maps().placeMapSlice(level,template,origin,new StructurePlaceSettings().setIgnoreEntities(false),level.getRandom().nextLong()));});
        }
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){var ref=links.stack(game,stack);return current->{var rebound=ref.get(current);return ()->stack=rebound;};}
    }
}
