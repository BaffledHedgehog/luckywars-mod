package me.skywars2.game;

import com.mojang.serialization.MapCodec;
import me.skywars2.SkyWars2;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Общий native shapeless-контейнер: matches/preview не расходуют предметы и не запускают эффекты. */
public final class LbcCraftingRecipe extends CustomRecipe {
    public enum Kind {
        ROCKET("rocket_boots_transfer",null),
        TINY_REFORGE("tiny_reforge",null),
        REROLL_MACHINE("reroll_machine",null),
        TERRA("terra_combine","terra_wand"),
        ELECTRO("electro_combine","electro_wand"),
        FIRE("fire_combine","fire_wand"),
        BLIND("blind_combine","blind_wand"),
        AIR("air_combine","air_wand"),
        STONE("stone_combine","stone_wand"),
        TELEPORT("teleport_combine","teleport_wand"),
        RUBY("ruby_combine","ruby_wand"),
        ACID("acid_combine","acid_wand"),
        CYBERKATANA("electro_katana","cyberkatana"),
        SHIT_KATANA("shit_katana","shit_katana"),
        ELECTRO_SHIELD("electro_shield","electro_shield"),
        ELECTROBLOCKER("electroblocker","electroblocker"),
        EXASPERATO("exasperato_combine","exasperato_wand"),
        BEDROCK("bedrock_combine","bedrock_wand"),
        STONEKIVI("stonekivi_combine","stonekivi_wand"),
        SUN("sun_arcanum_combine","sun_arcanum"),
        FIREBLAZE("fireblaze_combine","fireblaze_wand"),
        PSYCHOBLIND("psychoblind_combine","psychoblind_wand"),
        PSYCHOVEX("psychovex_combine","psychovex_wand"),
        EXAHOMING("exahoming_combine","exahoming_wand"),
        BOLTBOLT("boltbolt_combine","boltbolt_wand"),
        INFBOLT("infbolt_combine","infbolt_wand"),
        CONSUMING("consuming_combine","consuming_wand"),
        DUPLICATING("duplicating_combine","duplicating_wand"),
        GAYCASTER("gaycaster_combine","gaycaster"),
        AWAKENED_ACID("awakened_acid_combine","awakened_acid_wand"),
        STAND_CLOCKS("stand_clocks","stand_clocks"),
        SHISH_WALL("shish_wall","shish_wall_wand"),
        SWORD("terra_sword_awakening","true_terra_sword"),
        RAZOR("terra_razorpine_awakening","terra_razorpine");
        final String id,loot;
        final LbcCraftingRecipe recipe;
        final RecipeSerializer<LbcCraftingRecipe> serializer;
        Kind(String id,String loot) {
            this.id=id; this.loot=loot; this.recipe=new LbcCraftingRecipe(this);
            this.serializer=new RecipeSerializer<>(MapCodec.unit(recipe),StreamCodec.unit(recipe));
        }
    }
    private final Kind kind;
    private LbcCraftingRecipe(Kind kind) { this.kind=kind; }

    public static void register() {
        for(var kind:Kind.values()) Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Identifier.parse("skywars2:"+kind.id),kind.serializer);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        var game = SkyWars2.game();
        return game != null && game.playing() && level == game.maps().level()
                && (kind==Kind.ROCKET ? !LbcItems.rocketCraftTarget(input).isEmpty() : craftingKind(input)==kind);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        var game = SkyWars2.game();
        if(game==null) return ItemStack.EMPTY;
        if(kind==Kind.ROCKET) return game.items().rocketCraftResult(input);
        if(kind==Kind.TINY_REFORGE) return craftingKind(input)==kind ? LbcTinyReforge.placeholder() : ItemStack.EMPTY;
        if(kind==Kind.REROLL_MACHINE) return craftingKind(input)==kind ? LbcRerollCrafting.placeholder() : ItemStack.EMPTY;
        if(craftingKind(input)!=kind) return ItemStack.EMPTY;
        var result=game.lbc().rollTable(game.maps().level(),net.minecraft.world.phys.Vec3.ZERO,null,"lbc:ported/"+kind.loot);
        if(result.isEmpty()) return ItemStack.EMPTY;
        var stack=result.getFirst().copyWithCount(1);
        var lore=stack.get(net.minecraft.core.component.DataComponents.LORE);
        if(lore!=null) {
            // У recipe preview нет THIS_ENTITY: разрешаем исходные mana-score строки через сервер,
            // сохраняя компоненты переводов и стили, как при выдаче того же loot игроку.
            var context=net.minecraft.network.chat.ResolutionContext.create(
                    game.server().createCommandSourceStack().withLevel(game.maps().level()));
            var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>(lore.lines().size());
            for(var line:lore.lines()) {
                try { lines.add(net.minecraft.network.chat.ComponentUtils.resolve(context,line)); }
                catch(com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                    SkyWars2.LOGGER.warn("Cannot resolve crafting lore for {}",kind.id,exception);
                    lines.add(line);
                }
            }
            stack.set(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(lines));
        }
        return stack;
    }

    @Override
    public RecipeSerializer<LbcCraftingRecipe> getSerializer() { return kind.serializer; }

    /** Общее распознавание семейств: ровно два занятых слота, без расхода и случайности в preview. */
    public static Kind craftingKind(CraftingInput input) {
        if(input.ingredientCount()!=2) return LbcRerollCrafting.matches(input) ? Kind.REROLL_MACHINE : null;
        int basicAcid=0,caustic=0;
        for(var stack:input.items())if(!stack.isEmpty()){
            var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if(stack.is(LbcWands.ACID_WAND)||data.getIntOr("stacid",0)==1)basicAcid++;
            else if(stack.is(LbcWands.CAUSTIC)||stack.is(LbcWands.ACADEMY_CAUSTIC)||data.getIntOr("acidwand",0)==1)caustic++;
        }
        if(basicAcid==1&&caustic==1)return Kind.AWAKENED_ACID;
        int prism=0,omega=0;
        for(var stack:input.items())if(!stack.isEmpty()){
            var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if(stack.is(LbcWands.PRISM)||stack.is(LbcWands.ACADEMY_PRISM)||data.getIntOr("prisma",0)==1)prism++;
            else if(stack.is(LbcWands.OMEGA)||stack.is(LbcWands.ACADEMY_OMEGA)||data.getIntOr("omegaflag",0)==1)omega++;
        }
        if(prism==1&&omega==1)return Kind.GAYCASTER;
        int reactiveShish=0,upgradedWall=0;
        for(var stack:input.items()) if(!stack.isEmpty()) {
            if(LbcReactiveItems.isReactive(stack)) reactiveShish++;
            else if(stack.is(LbcWands.ACADEMY_WALL) || stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).matchedBy(UPGRADED_WALL_DONOR)) upgradedWall++;
        }
        if(reactiveShish==1 && upgradedWall==1) return Kind.SHISH_WALL;
        int standArrow=0,timeStopper=0;
        for(var stack:input.items()) if(!stack.isEmpty()) {
            if(LbcStand.isArrow(stack)) standArrow++;
            else if(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).matchedBy(TIME_STOPPER_DONOR)) timeStopper++;
        }
        if(standArrow==1 && timeStopper==1) return Kind.STAND_CLOCKS;
        var tiny=tinyPair(input);
        if(tiny!=null) return tiny;
        if(LbcTinyReforge.matches(input)) return Kind.TINY_REFORGE;
        if(LbcRerollCrafting.matches(input)) return Kind.REROLL_MACHINE;
        var terra=LbcTerra.craftingKind(input);
        if(terra!=null) return terra;
        int air=0,exoperso=0,stone=0,wall=0,tanner=0,fire=0,kiuas=0,blaze=0,blind=0,psycho=0,vex=0,stars=0,teleport=0,bolts=0,material=0,ruby=0,consuming=0,divider=0;
        for(var stack:input.items()) if(!stack.isEmpty()) {
            var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if(stack.is(LbcWands.AIR_WAND) || stack.is(LbcWands.ACADEMY_AIR)
                    || data!=null && data.matchedBy(AIR_DONOR)) air++;
            else if(stack.is(LbcWands.EXOPERSO) || stack.is(LbcWands.ACADEMY_EXOPERSO)
                    || data!=null && data.matchedBy(EXOPERSO_DONOR)) exoperso++;
            else if(stack.is(LbcWands.FALLING_STARS_WAND) || stack.is(LbcWands.ACADEMY_FALLING_STARS)
                    || data!=null && data.matchedBy(STARS_DONOR)) stars++;
            else if(stack.is(LbcWands.TELEPORT_WAND) || stack.is(LbcWands.ACADEMY_TELEPORT)
                    || data!=null && data.matchedBy(TELEPORT_DONOR)) teleport++;
            else if(LbcWands.isBoltMaterial(stack)) material++;
            else if(stack.is(LbcWands.RUBY_WAND) || stack.is(LbcWands.ACADEMY_RUBY)
                    || data!=null && data.matchedBy(RUBY_DONOR)) ruby++;
            else if(LbcWands.isConsumingMaterial(stack)) consuming++;
            else if(LbcWands.isDivider(stack)) divider++;
            else if(stack.is(LbcWands.LIGHTNING_BOLTS_WAND) || stack.is(LbcWands.ACADEMY_LIGHTNING_BOLTS)
                    || data!=null && data.matchedBy(BOLTS_DONOR)) bolts++;
            else if(stack.is(LbcWands.STONE_WAND) || stack.is(LbcWands.ACADEMY_STONE)
                    || data!=null && data.matchedBy(STONE_DONOR)) stone++;
            else if(stack.is(LbcWands.WALL) || stack.is(LbcWands.ACADEMY_WALL)
                    || data!=null && data.matchedBy(WALL_DONOR)) wall++;
            else if(LbcTanner.isTanner(stack)) tanner++;
            else if(stack.is(LbcWands.FIRE_WAND) || stack.is(LbcWands.ACADEMY_FIRE)
                    || data!=null && data.matchedBy(FIRE_DONOR)) fire++;
            else if(LbcKiuas.isKiuaskivi(stack)) kiuas++;
            else if(stack.is(LbcWands.BLAZE_WAND) || stack.is(LbcWands.ACADEMY_BLAZE)
                    || data!=null && data.matchedBy(BLAZE_DONOR)) blaze++;
        }
        if(input.ingredientCount()==2) for(var stack:input.items()) if(!stack.isEmpty()) {
            var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if(stack.is(LbcWands.BLIND_WAND) || stack.is(LbcWands.ACADEMY_BLIND)
                    || data!=null && data.matchedBy(BLIND_DONOR)) blind++;
            else if(stack.is(LbcWands.SUMMONER_VEXES) || data!=null && data.matchedBy(VEX_DONOR)) vex++;
            else if(stack.is(LbcWands.PSYCHOWAND) || stack.is(LbcWands.ACADEMY_PSYCHOWAND)
                    || data!=null && data.matchedBy(PSYCHO_DONOR)) psycho++;
        }
        if(blind==1 && vex==1) return Kind.PSYCHOVEX;
        if(blind==1 && psycho==1) return Kind.PSYCHOBLIND;
        if(air==1 && exoperso==1) return Kind.EXASPERATO;
        if(air==1 && stars==1) return Kind.EXAHOMING;
        if(teleport==1 && bolts==1) return Kind.BOLTBOLT;
        if(teleport==1 && material==1) return Kind.INFBOLT;
        if(ruby==1 && consuming==1) return Kind.CONSUMING;
        if(ruby==1 && divider==1) return Kind.DUPLICATING;
        if(stone==1 && wall==1) return Kind.BEDROCK;
        if(stone==1 && tanner==1) return Kind.STONEKIVI;
        if(fire==1 && kiuas==1) return Kind.SUN;
        if(fire==1 && blaze==1) return Kind.FIREBLAZE;
        int katana=0,donor=0;
        for(var stack:input.items()) if(!stack.isEmpty()) {
            if(LbcElectro.isKatana(stack) && !LbcElectro.isShitKatana(stack)) katana++;
            else if(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("govnoq",0)==1) donor++;
        }
        if(katana==1 && donor==1) return Kind.SHIT_KATANA;
        int tinyElectro=0,sword=0,shield=0,electro=0,energyShield=0;
        for(var stack:input.items()) if(!stack.isEmpty()) {
            if(tinyElectro(stack)) tinyElectro++;
            else if(LbcElectro.isShield(stack)) energyShield++;
            else if(stack.is(LbcWands.ELECTRO) || stack.is(LbcWands.ACADEMY_ELECTRO)
                    || stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("stelectro",0)==1) electro++;
            else if(stack.is(net.minecraft.world.item.Items.IRON_SWORD)) sword++;
            else if(stack.is(net.minecraft.world.item.Items.SHIELD) && !LbcElectro.isShield(stack)) shield++;
            else return null;
        }
        if(electro==1 && energyShield==1) return Kind.ELECTROBLOCKER;
        if(tinyElectro==1 && sword==1) return Kind.CYBERKATANA;
        return tinyElectro==1 && shield==1 ? Kind.ELECTRO_SHIELD : null;
    }

    /** Два занятых слота одного Tiny-семейства, включая старые доноры и смешанную пару. */
    private static Kind tinyPair(CraftingInput input) {
        Kind first=null;
        for(var stack:input.items()) if(!stack.isEmpty()) {
            var current=tinyKind(stack);
            if(current==null || first!=null && first!=current) return null;
            first=current;
        }
        return first;
    }

    private static Kind tinyKind(ItemStack stack) {
        if(stack.is(LbcWands.TINY_FIRE)) return Kind.FIRE;
        if(stack.is(LbcWands.TINY_BLIND)) return Kind.BLIND;
        if(stack.is(LbcWands.TINY_AIR)) return Kind.AIR;
        if(stack.is(LbcWands.TINY_STONE)) return Kind.STONE;
        if(stack.is(LbcWands.TINY_TELEPORT)) return Kind.TELEPORT;
        if(stack.is(LbcWands.TINY_RUBY)) return Kind.RUBY;
        if(stack.is(LbcWands.TINY_TERRA)) return Kind.TERRA;
        if(stack.is(LbcWands.TINY_ACID)) return Kind.ACID;
        if(stack.is(LbcWands.TINY_ELECTRO)) return Kind.ELECTRO;
        var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(data==null) return null;
        if(data.matchedBy(TINY_FIRE_DONOR)) return Kind.FIRE;
        if(data.matchedBy(TINY_BLIND_DONOR)) return Kind.BLIND;
        if(data.matchedBy(TINY_AIR_DONOR)) return Kind.AIR;
        if(data.matchedBy(TINY_STONE_DONOR)) return Kind.STONE;
        if(data.matchedBy(TINY_TELEPORT_DONOR)) return Kind.TELEPORT;
        if(data.matchedBy(TINY_RUBY_DONOR)) return Kind.RUBY;
        if(data.matchedBy(TINY_TERRA_DONOR)) return Kind.TERRA;
        if(data.matchedBy(TINY_ACID_DONOR)) return Kind.ACID;
        return data.matchedBy(TINY_ELECTRO_DONOR) ? Kind.ELECTRO : null;
    }

    private static boolean tinyElectro(ItemStack stack) {
        var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return stack.is(LbcWands.TINY_ELECTRO) || data!=null && data.copyTag().getIntOr("stelectro_tiny",0)==1;
    }

    private static final net.minecraft.nbt.CompoundTag AIR_DONOR=donorFlag("stair"),
            UPGRADED_WALL_DONOR=upgradedWallDonor(),
            TIME_STOPPER_DONOR=donorFlag("time_stopper"),
            RUBY_DONOR=donorFlag("struby"),
            EXOPERSO_DONOR=donorFlag("exoperso"), STONE_DONOR=donorFlag("ststone"), WALL_DONOR=donorFlag("wall"), FIRE_DONOR=donorFlag("stfire"), BLAZE_DONOR=donorFlag("fallingstars"), BLIND_DONOR=donorFlag("stblind"), PSYCHO_DONOR=donorFlag("psychowand"), VEX_DONOR=donorFlag("summonervexes"), STARS_DONOR=donorFlag("truefallingstars"), TELEPORT_DONOR=donorFlag("stteleport"), BOLTS_DONOR=donorFlag("lightning_bolts");
    private static final net.minecraft.nbt.CompoundTag TINY_FIRE_DONOR=donorFlag("stfire_tiny"),
            TINY_BLIND_DONOR=donorFlag("stblind_tiny"), TINY_AIR_DONOR=donorFlag("stair_tiny"),
            TINY_STONE_DONOR=donorFlag("ststone_tiny"), TINY_TELEPORT_DONOR=donorFlag("stteleport_tiny"),
            TINY_RUBY_DONOR=donorFlag("struby_tiny"), TINY_TERRA_DONOR=donorFlag("stterra_tiny"),
            TINY_ACID_DONOR=donorFlag("stacid_tiny"), TINY_ELECTRO_DONOR=donorFlag("stelectro_tiny");
    private static net.minecraft.nbt.CompoundTag donorFlag(String name) {
        var tag=new net.minecraft.nbt.CompoundTag(); tag.putInt(name,1); return tag;
    }

    private static net.minecraft.nbt.CompoundTag upgradedWallDonor() {
        var tag=donorFlag("wall"); tag.putInt("upgraded",1); return tag;
    }

    /** (user) 60% сохранения донора: случайность только при настоящем расходе, не при preview. */
    @Override
    public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result=net.minecraft.world.item.crafting.CraftingRecipe.defaultCraftingReminder(input);
        var game=SkyWars2.game();
        if((kind==Kind.CYBERKATANA || kind==Kind.ELECTRO_SHIELD) && game!=null
                && craftingKind(input)==kind && game.maps().level().getRandom().nextFloat()<.6f)
            for(int i=0;i<input.size();i++) if(tinyElectro(input.getItem(i))) {
                result.set(i,input.getItem(i).copyWithCount(1));
                break;
            }
        return result;
    }

    /** Только настоящий ResultSlot.onTake: preview не вызывает звуки, частицы или изменение мира. */
    public static void craftTaken(net.minecraft.server.level.ServerPlayer player,CraftingInput input) {
        var game=SkyWars2.game();
        if(game==null || !game.canUseGameplayItems(player)) return;
        game.items().rocketCraftTaken(player,input);
        var kind=craftingKind(input);
        if(kind==null) return;
        if(kind==Kind.SHISH_WALL) {
            player.level().sendParticles(new net.minecraft.core.particles.ItemParticleOption(
                            net.minecraft.core.particles.ParticleTypes.ITEM,net.minecraft.world.item.Items.SPRUCE_SAPLING),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,.15);
            LbcOverload.sound(player,player.position(),"minecraft:block.grass.break",1,1,0);
            return;
        }
        if(kind==Kind.STAND_CLOCKS) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,.38f,.133f,.388f)&0xffffff,2),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.CONSUMING || kind==Kind.DUPLICATING) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,kind==Kind.DUPLICATING?.812f:.553f,kind==Kind.DUPLICATING?.643f:.129f,kind==Kind.DUPLICATING?.761f:.427f)&0xffffff,2),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.BOLTBOLT || kind==Kind.INFBOLT) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,kind==Kind.INFBOLT?.933f:.643f,kind==Kind.INFBOLT?.969f:.765f,kind==Kind.INFBOLT?.98532f:.812f)&0xffffff,kind==Kind.INFBOLT?4:2),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.EXAHOMING) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,.886f,.843f,.729f)&0xffffff,2),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.PSYCHOBLIND || kind==Kind.PSYCHOVEX) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            (kind==Kind.PSYCHOVEX ? net.minecraft.util.ARGB.colorFromFloat(1,.001f,0,.001f)
                                    : net.minecraft.util.ARGB.colorFromFloat(1,.384f,.549f,.561f))&0xffffff,2),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.SUN || kind==Kind.FIREBLAZE) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,kind==Kind.SUN?.937f:.988f,kind==Kind.SUN?.988f:.733f,.263f)&0xffffff,2),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.STONEKIVI) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,.345f,.235f,.173f)&0xffffff,4),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.BEDROCK) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,.125f,.141f,.145f)&0xffffff,4),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.EXASPERATO) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            net.minecraft.util.ARGB.colorFromFloat(1,.988f,.804f,.294f)&0xffffff,2),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
            return;
        }
        if(kind==Kind.SHIT_KATANA) {
            player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xffff00,1),
                    true,false,player.getX(),player.getY()+1,player.getZ(),100,1,1,1,1);
            LbcOverload.sound(player,"minecraft:entity.zombie_villager.cure",2,1,0);
            LbcOverload.sound(player,"minecraft:entity.player.breath",2,0,0);
            return;
        }
        boolean enchant=switch(kind) {
            case FIRE,BLIND,AIR,STONE,TELEPORT,RUBY,TERRA,ACID,ELECTRO,CYBERKATANA,ELECTRO_SHIELD -> true;
            default -> false;
        };
        var dust=new net.minecraft.core.particles.DustParticleOptions(kind==Kind.ELECTROBLOCKER
                ? net.minecraft.util.ARGB.colorFromFloat(1,.196f,.514f,.592f)&0xffffff : kind==Kind.SWORD
                ? net.minecraft.util.ARGB.colorFromFloat(1,.475f,.761f,.435f)&0xffffff
                : net.minecraft.util.ARGB.colorFromFloat(1,.263f,.427f,.239f)&0xffffff,2);
        player.level().sendParticles(enchant ? net.minecraft.core.particles.ParticleTypes.ENCHANT : dust,
                true,false,player.getX(),player.getY()+(enchant ? .2 : 1),player.getZ(),100,1,1,1,1);
        LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
    }
}
