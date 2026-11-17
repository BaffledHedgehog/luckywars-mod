package me.skywars2.game.genie;

import com.google.gson.*;
import java.util.*;

/** A model supplies data, never executable code, commands or arbitrary item NBT. */
public record GenieBlueprint(String name,String description,String icon,int count,
                             List<Rule> rules,List<Stat> stats,Twist twist) {
    public enum Trigger { USE,HIT,HURT,JUMP,SHOT,EAT,SPRINT,TICK,MINING }
    public enum Target { SELF,HIT_TARGET,AIMED_TARGET,NEARBY_ENEMIES,NEARBY_ALL,LOOK_POINT }
    public enum Condition { ALWAYS,WET,DRY,ON_GROUND,IN_AIR,DAY,NIGHT,SNEAKING,SPRINTING,LOW_HEALTH,FULL_HEALTH }
    public enum Effect { DAMAGE,HEAL,STATUS,FIRE,PUSH,PULL,TELEPORT,EXPLOSION,LIGHTNING,PROJECTILE,ENTITY_LAUNCH,REPAIR,MANA,FOOD,CLEANSE,SOUND,PARTICLES,BLOCK }
    public enum Slot { AUTO,MAIN_HAND,OFF_HAND,HAND,HEAD,CHEST,LEGS,FEET }
    public enum TwistKind { HEALTH_PRICE,MANA_PRICE,HUNGER_PRICE,RECOIL,SELF_POISON,SELF_FIRE,
        SELF_GLOW,SELF_LAUNCH,SELF_TELEPORT,SHUFFLE_POCKETS,DROP_ITEM,BRITTLE,
        HEAL_ENEMY,CRUMBLE_FLOOR,SLOW_OWNER,WET_ONLY,DRY_ONLY,NIGHT_ONLY,AIR_ONLY }
    public record Rule(Trigger trigger,Target target,Condition condition,Effect effect,String id,
                       double amount,double radius,int ticks,int cooldown,double chance) { }
    public record Stat(String id,double amount,Slot slot) { }
    public record Twist(TwistKind kind,double power) { }
    public record Reply(boolean granted,String answer,String loophole,GenieBlueprint item) { }

    private static final Gson JSON=new Gson();
    private static final Set<String> STATS=Set.of("attack_damage","attack_speed","armor","armor_toughness",
            "max_health","movement_speed","jump_strength","knockback_resistance","luck","safe_fall_distance");
    public GenieBlueprint {
        rules=rules==null?List.of():List.copyOf(rules);
        stats=stats==null?List.of():List.copyOf(stats);
    }
    private static void require(boolean valid,String message){if(!valid)throw new IllegalArgumentException(message);}
    private static boolean finite(double number){return Double.isFinite(number);}
    private static String identifier(String input){
        require(input!=null&&input.length()<=96&&input.matches("(?:minecraft:)?[a-z0-9_./-]+"),"invalid built-in identifier");
        return input.startsWith("minecraft:")?input.substring(10):input;
    }
    public static String plain(String value,int maximum){
        require(value!=null&&!value.isBlank()&&value.length()<=maximum,"missing or excessive text");
        require(value.codePoints().noneMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\t'),"control characters in text");
        require(value.indexOf('\u00a7')<0,"formatting codes in text");return value.strip();
    }
    /** Validate before resolving native registries or mutating a lamp/player. */
    public void validate(){validatedCost();}
    public double validatedCost(){
        plain(name,64);plain(description,240);identifier(icon);
        require(count>=1&&count<=64,"item quantity outside constructor bounds");
        require(rules.size()<=6&&stats.size()<=5&&(!rules.isEmpty()||!stats.isEmpty()),"empty or excessive item program");
        require(twist!=null&&twist.kind()!=null&&finite(twist.power())&&twist.power()>=1&&twist.power()<=3,"a meaningful drawback is required");
        double cost=count*.15;
        for(var rule:rules){
            require(rule!=null&&rule.trigger()!=null&&rule.target()!=null&&rule.condition()!=null&&rule.effect()!=null,"incomplete rule");
            require(rule.id()!=null&&rule.id().length()<=96,"excessive effect identifier");
            require(finite(rule.amount())&&rule.amount()>=0&&rule.amount()<=16,"effect power outside constructor bounds");
            require(finite(rule.radius())&&rule.radius()>=0&&rule.radius()<=6,"effect radius outside constructor bounds");
            require(rule.ticks()>=0&&rule.ticks()<=600&&rule.cooldown()>=5&&rule.cooldown()<=1200,"invalid effect timing");
            require(finite(rule.chance())&&rule.chance()>0&&rule.chance()<=1,"invalid trigger probability");
            if(rule.trigger()==Trigger.TICK)require(rule.cooldown()>=20,"continuous effects need a one-second cadence");
            if(rule.effect()==Effect.EXPLOSION)require(rule.amount()<=4&&rule.cooldown()>=40,"explosion exceeds constructor bounds");
            if(rule.effect()==Effect.PROJECTILE)require(Set.of("arrow","snowball","small_fireball").contains(identifier(rule.id())),"unsupported projectile primitive");
            if(rule.effect()==Effect.ENTITY_LAUNCH){identifier(rule.id());require(rule.amount()<=3&&rule.cooldown()>=40,"native entity launch needs speed<=3 and at least40 ticks between bodies");}
            if(Set.of(Effect.STATUS,Effect.SOUND,Effect.PARTICLES,Effect.BLOCK).contains(rule.effect()))identifier(rule.id());
            if(rule.effect()==Effect.STATUS){
                require(rule.amount()<=4,"status level exceeds constructor bounds");
                if(Set.of("blindness","darkness").contains(identifier(rule.id())))require(rule.amount()<=1,"visual darkness is limited to level II");
            }
            if(rule.effect()==Effect.BLOCK)require(rule.radius()<=2&&rule.cooldown()>=20,"block work exceeds constructor bounds");
            if(rule.effect()==Effect.TELEPORT)require(rule.radius()<=6,"teleport exceeds constructor bounds");
            double area=rule.target()==Target.NEARBY_ALL||rule.target()==Target.NEARBY_ENEMIES?1+rule.radius()*.4:1;
            double power=switch(rule.effect()){
                case DAMAGE,HEAL,EXPLOSION,LIGHTNING,PROJECTILE,ENTITY_LAUNCH -> 2+rule.amount();
                case STATUS -> 2+rule.amount()*2+rule.ticks()/100.;
                case BLOCK -> 4+rule.radius()*3;
                default -> 2+rule.amount()*.5;
            };
            cost+=power*area*rule.chance()*Math.max(.25,20./rule.cooldown());
        }
        var seen=new HashSet<String>();
        for(var stat:stats){
            require(stat!=null&&stat.slot()!=null,"incomplete attribute");String id=identifier(stat.id());
            require(STATS.contains(id)&&seen.add(id+":"+stat.slot()),"unsupported or repeated attribute");
            require(finite(stat.amount()),"nonfinite attribute");
            double maximum=switch(id){case "movement_speed"->.05;case "jump_strength"->.5;case "knockback_resistance"->.5;case "attack_speed"->2;default->12;};
            require(Math.abs(stat.amount())<=maximum,"attribute exceeds constructor bounds");
            cost+=Math.abs(stat.amount())/maximum*8;
        }
        require(cost<=48,"requested item exceeds constructor power budget");
        return cost;
    }
    public static Reply parseReply(String content){
        require(content!=null&&content.length()<=32768,"excessive model reply");
        var root=JsonParser.parseString(content);require(root.isJsonObject(),"reply must be an object");
        require(root.getAsJsonObject().keySet().equals(Set.of("granted","answer","loophole","item")),"unknown model reply fields");
        validateShape(root,schema());
        var reply=JSON.fromJson(root,Reply.class);speech(reply.answer());
        require(reply.answer().indexOf('\u2014')<0&&reply.answer().indexOf('\u2013')<0,"long dashes in genie speech");
        if(reply.granted()){
            require(reply.item()!=null,"accepted wish lacks an item");plain(reply.loophole(),400);reply.item().validate();
        }else require(reply.item()==null,"rejected wish must not include an item");
        return reply;
    }
    public String encode(){validate();return JSON.toJson(this);}
    public static GenieBlueprint decode(String content){
        require(content!=null&&content.length()<=16384,"excessive item program");var tree=JsonParser.parseString(content);validateShape(tree,schema().getAsJsonObject("properties").getAsJsonObject("item").getAsJsonArray("anyOf").get(0).getAsJsonObject());var result=JSON.fromJson(tree,GenieBlueprint.class);require(result!=null,"missing item program");result.validate();return result;
    }

    private static void speech(String text){
        plain(text,320);
        require(text.codePoints().allMatch(c->Character.isLetterOrDigit(c)||Character.getType(c)==Character.NON_SPACING_MARK||c==' '||c>=33&&c<=126),"non-keyboard decorations in genie speech");
    }
    /** Check every object and primitive too: Gson alone silently ignores unknown nested fields. */
    private static void validateShape(JsonElement value,JsonObject shape){
        if(shape.has("anyOf")){
            for(var variant:shape.getAsJsonArray("anyOf"))try{validateShape(value,variant.getAsJsonObject());return;}catch(IllegalArgumentException ignored){}
            throw new IllegalArgumentException("invalid nullable item");
        }
        String type=shape.get("type").getAsString();
        switch(type){
            case "null" -> require(value!=null&&value.isJsonNull(),"expected null");
            case "object" -> {
                require(value!=null&&value.isJsonObject(),"expected object");
                var fields=shape.getAsJsonObject("properties");require(value.getAsJsonObject().keySet().equals(fields.keySet()),"unexpected or missing constructor fields");
                fields.entrySet().forEach(e->validateShape(value.getAsJsonObject().get(e.getKey()),e.getValue().getAsJsonObject()));
            }
            case "array" -> {require(value!=null&&value.isJsonArray(),"expected array");for(var element:value.getAsJsonArray())validateShape(element,shape.getAsJsonObject("items"));}
            case "string" -> require(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString(),"expected string");
            case "boolean" -> require(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean(),"expected boolean");
            case "number","integer" -> {
                require(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber(),"expected number");
                double number=value.getAsDouble();require(Double.isFinite(number)&&(!type.equals("integer")||number==Math.rint(number)),"invalid number");
            }
            default -> throw new IllegalArgumentException("unsupported schema type");
        }
        if(shape.has("enum"))require(shape.getAsJsonArray("enum").contains(value),"unknown constructor enum");
    }

    private static JsonObject type(String type){var result=new JsonObject();result.addProperty("type",type);return result;}
    private static JsonObject enumeration(Class<? extends Enum<?>> kind){
        var result=type("string");var choices=new JsonArray();for(var value:kind.getEnumConstants())choices.add(value.name());result.add("enum",choices);return result;
    }
    private static JsonObject object(Map<String,JsonObject> fields){
        var result=type("object");var properties=new JsonObject();var required=new JsonArray();
        fields.forEach((key,value)->{properties.add(key,value);required.add(key);});result.add("properties",properties);result.add("required",required);result.addProperty("additionalProperties",false);return result;
    }
    private static JsonObject array(JsonObject item){var result=type("array");result.add("items",item);return result;}
    /** Only basic supported JSON-schema constructs; all semantic limits are checked above too. */
    public static JsonObject schema(){
        var rule=object(Map.of("trigger",enumeration(Trigger.class),"target",enumeration(Target.class),"condition",enumeration(Condition.class),
                "effect",enumeration(Effect.class),"id",type("string"),"amount",type("number"),"radius",type("number"),
                "ticks",type("integer"),"cooldown",type("integer"),"chance",type("number")));
        var stat=object(Map.of("id",type("string"),"amount",type("number"),"slot",enumeration(Slot.class)));
        var twist=object(Map.of("kind",enumeration(TwistKind.class),"power",type("number")));
        var item=object(Map.of("name",type("string"),"description",type("string"),"icon",type("string"),"count",type("integer"),
                "rules",array(rule),"stats",array(stat),"twist",twist));
        var nullable=new JsonObject();var choices=new JsonArray();choices.add(item);choices.add(type("null"));nullable.add("anyOf",choices);
        return object(Map.of("granted",type("boolean"),"answer",type("string"),"loophole",type("string"),"item",nullable));
    }
}
