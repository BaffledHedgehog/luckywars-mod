package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Durable shop admission; all other actor allegiance remains owned by each existing mechanic. */
public final class LbcBedwarsRoleGuards {
    private LbcBedwarsRoleGuards(){}
    public static final String ROLE="skywars2.bedwars.shop";
    private static LbcBedwarsArena.Topology indexed;
    private static Set<UUID> shops=Set.of();
    private static void index(){
        var game=SkyWars2.game();var next=game==null||game.bedwars()==null?null:game.bedwars().arena().topology();
        if(next==indexed)return;indexed=next;
        if(next==null){shops=Set.of();return;}
        var ids=new HashSet<UUID>();for(var marker:next.markers())if(marker.kind()==LbcBedwarsArena.MarkerKind.SHOP)ids.add(marker.id());shops=Set.copyOf(ids);
    }
    public static boolean shop(Entity entity){
        if(entity==null)return false;
        var tags=entity.entityTags();if(tags.contains("bedwars_shop")||tags.contains(ROLE))return true;
        if(entity instanceof net.minecraft.server.level.ServerPlayer)return false;
        index();return indexed!=null&&entity.level()==indexed.level()&&shops.contains(entity.getUUID());
    }
    /** Call at real placed/load admission; these native tags are retained by NBT and canonical body replay. */
    public static void markShop(Entity entity){entity.entityTags().add("bedwars_shop");entity.entityTags().add(ROLE);entity.entityTags().add("strict_map_object");}
    public static boolean affectable(Entity entity){return !shop(entity)||MatchActorCheckpoint.restoring(entity);}
    public static boolean canEmitFrom(Entity entity){return !shop(entity);}
    public static boolean canEmit(LbcSpellSource.Source source,ServerLevel level){
        if(source==null)return true;index();
        if(shops.contains(source.emitter()))return false;
        return level==null||canEmitFrom(level.getEntity(source.emitter()));
    }
    /** Reject before native addFreshEntity publishes a child, without changing the typed source factory contract. */
    public static boolean publication(ServerLevel level){
        var game=SkyWars2.game();return game==null||canEmit(game.items().spellSources().current(),level);
    }
}
