package net.minecraft.core.registries;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.IRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.Particle;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.numbers.NumberFormatType;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.StatisticWrapper;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.EnumBannerPatternType;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GeneratorSettingBase;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverAbstract;
import net.minecraft.world.level.levelgen.carver.WorldGenCarverWrapper;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacers;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProviders;
import net.minecraft.world.level.levelgen.feature.treedecorators.WorldGenFeatureTrees;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacers;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.WorldGenFeatureDefinedStructurePoolTemplate;
import net.minecraft.world.level.levelgen.structure.pools.WorldGenFeatureDefinedStructurePools;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureStructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.score.LootScoreProviderType;

public class Registries {

    public static final MinecraftKey ROOT_REGISTRY_NAME = MinecraftKey.withDefaultNamespace("root");
    public static final ResourceKey<IRegistry<Activity>> ACTIVITY = createRegistryKey("activity");
    public static final ResourceKey<IRegistry<AttributeBase>> ATTRIBUTE = createRegistryKey("attribute");
    public static final ResourceKey<IRegistry<EnumBannerPatternType>> BANNER_PATTERN = createRegistryKey("banner_pattern");
    public static final ResourceKey<IRegistry<MapCodec<? extends WorldChunkManager>>> BIOME_SOURCE = createRegistryKey("worldgen/biome_source");
    public static final ResourceKey<IRegistry<Block>> BLOCK = createRegistryKey("block");
    public static final ResourceKey<IRegistry<MapCodec<? extends Block>>> BLOCK_TYPE = createRegistryKey("block_type");
    public static final ResourceKey<IRegistry<TileEntityTypes<?>>> BLOCK_ENTITY_TYPE = createRegistryKey("block_entity_type");
    public static final ResourceKey<IRegistry<BlockPredicateType<?>>> BLOCK_PREDICATE_TYPE = createRegistryKey("block_predicate_type");
    public static final ResourceKey<IRegistry<WorldGenFeatureStateProviders<?>>> BLOCK_STATE_PROVIDER_TYPE = createRegistryKey("worldgen/block_state_provider_type");
    public static final ResourceKey<IRegistry<WorldGenCarverAbstract<?>>> CARVER = createRegistryKey("worldgen/carver");
    public static final ResourceKey<IRegistry<CatVariant>> CAT_VARIANT = createRegistryKey("cat_variant");
    public static final ResourceKey<IRegistry<WolfVariant>> WOLF_VARIANT = createRegistryKey("wolf_variant");
    public static final ResourceKey<IRegistry<MapCodec<? extends ChunkGenerator>>> CHUNK_GENERATOR = createRegistryKey("worldgen/chunk_generator");
    public static final ResourceKey<IRegistry<ChunkStatus>> CHUNK_STATUS = createRegistryKey("chunk_status");
    public static final ResourceKey<IRegistry<ArgumentTypeInfo<?, ?>>> COMMAND_ARGUMENT_TYPE = createRegistryKey("command_argument_type");
    public static final ResourceKey<IRegistry<CreativeModeTab>> CREATIVE_MODE_TAB = createRegistryKey("creative_mode_tab");
    public static final ResourceKey<IRegistry<MinecraftKey>> CUSTOM_STAT = createRegistryKey("custom_stat");
    public static final ResourceKey<IRegistry<DamageType>> DAMAGE_TYPE = createRegistryKey("damage_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends DensityFunction>>> DENSITY_FUNCTION_TYPE = createRegistryKey("worldgen/density_function_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends EnchantmentEntityEffect>>> ENCHANTMENT_ENTITY_EFFECT_TYPE = createRegistryKey("enchantment_entity_effect_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends LevelBasedValue>>> ENCHANTMENT_LEVEL_BASED_VALUE_TYPE = createRegistryKey("enchantment_level_based_value_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends EnchantmentLocationBasedEffect>>> ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE = createRegistryKey("enchantment_location_based_effect_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends EnchantmentProvider>>> ENCHANTMENT_PROVIDER_TYPE = createRegistryKey("enchantment_provider_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends EnchantmentValueEffect>>> ENCHANTMENT_VALUE_EFFECT_TYPE = createRegistryKey("enchantment_value_effect_type");
    public static final ResourceKey<IRegistry<EntityTypes<?>>> ENTITY_TYPE = createRegistryKey("entity_type");
    public static final ResourceKey<IRegistry<WorldGenerator<?>>> FEATURE = createRegistryKey("worldgen/feature");
    public static final ResourceKey<IRegistry<FeatureSizeType<?>>> FEATURE_SIZE_TYPE = createRegistryKey("worldgen/feature_size_type");
    public static final ResourceKey<IRegistry<FloatProviderType<?>>> FLOAT_PROVIDER_TYPE = createRegistryKey("float_provider_type");
    public static final ResourceKey<IRegistry<FluidType>> FLUID = createRegistryKey("fluid");
    public static final ResourceKey<IRegistry<WorldGenFoilagePlacers<?>>> FOLIAGE_PLACER_TYPE = createRegistryKey("worldgen/foliage_placer_type");
    public static final ResourceKey<IRegistry<FrogVariant>> FROG_VARIANT = createRegistryKey("frog_variant");
    public static final ResourceKey<IRegistry<GameEvent>> GAME_EVENT = createRegistryKey("game_event");
    public static final ResourceKey<IRegistry<HeightProviderType<?>>> HEIGHT_PROVIDER_TYPE = createRegistryKey("height_provider_type");
    public static final ResourceKey<IRegistry<Instrument>> INSTRUMENT = createRegistryKey("instrument");
    public static final ResourceKey<IRegistry<IntProviderType<?>>> INT_PROVIDER_TYPE = createRegistryKey("int_provider_type");
    public static final ResourceKey<IRegistry<Item>> ITEM = createRegistryKey("item");
    public static final ResourceKey<IRegistry<JukeboxSong>> JUKEBOX_SONG = createRegistryKey("jukebox_song");
    public static final ResourceKey<IRegistry<LootItemConditionType>> LOOT_CONDITION_TYPE = createRegistryKey("loot_condition_type");
    public static final ResourceKey<IRegistry<LootItemFunctionType<?>>> LOOT_FUNCTION_TYPE = createRegistryKey("loot_function_type");
    public static final ResourceKey<IRegistry<LootNbtProviderType>> LOOT_NBT_PROVIDER_TYPE = createRegistryKey("loot_nbt_provider_type");
    public static final ResourceKey<IRegistry<LootNumberProviderType>> LOOT_NUMBER_PROVIDER_TYPE = createRegistryKey("loot_number_provider_type");
    public static final ResourceKey<IRegistry<LootEntryType>> LOOT_POOL_ENTRY_TYPE = createRegistryKey("loot_pool_entry_type");
    public static final ResourceKey<IRegistry<LootScoreProviderType>> LOOT_SCORE_PROVIDER_TYPE = createRegistryKey("loot_score_provider_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends SurfaceRules.f>>> MATERIAL_CONDITION = createRegistryKey("worldgen/material_condition");
    public static final ResourceKey<IRegistry<MapCodec<? extends SurfaceRules.o>>> MATERIAL_RULE = createRegistryKey("worldgen/material_rule");
    public static final ResourceKey<IRegistry<MemoryModuleType<?>>> MEMORY_MODULE_TYPE = createRegistryKey("memory_module_type");
    public static final ResourceKey<IRegistry<Containers<?>>> MENU = createRegistryKey("menu");
    public static final ResourceKey<IRegistry<MobEffectList>> MOB_EFFECT = createRegistryKey("mob_effect");
    public static final ResourceKey<IRegistry<PaintingVariant>> PAINTING_VARIANT = createRegistryKey("painting_variant");
    public static final ResourceKey<IRegistry<Particle<?>>> PARTICLE_TYPE = createRegistryKey("particle_type");
    public static final ResourceKey<IRegistry<PlacementModifierType<?>>> PLACEMENT_MODIFIER_TYPE = createRegistryKey("worldgen/placement_modifier_type");
    public static final ResourceKey<IRegistry<VillagePlaceType>> POINT_OF_INTEREST_TYPE = createRegistryKey("point_of_interest_type");
    public static final ResourceKey<IRegistry<PositionSourceType<?>>> POSITION_SOURCE_TYPE = createRegistryKey("position_source_type");
    public static final ResourceKey<IRegistry<PosRuleTestType<?>>> POS_RULE_TEST = createRegistryKey("pos_rule_test");
    public static final ResourceKey<IRegistry<PotionRegistry>> POTION = createRegistryKey("potion");
    public static final ResourceKey<IRegistry<RecipeSerializer<?>>> RECIPE_SERIALIZER = createRegistryKey("recipe_serializer");
    public static final ResourceKey<IRegistry<Recipes<?>>> RECIPE_TYPE = createRegistryKey("recipe_type");
    public static final ResourceKey<IRegistry<RootPlacerType<?>>> ROOT_PLACER_TYPE = createRegistryKey("worldgen/root_placer_type");
    public static final ResourceKey<IRegistry<DefinedStructureRuleTestType<?>>> RULE_TEST = createRegistryKey("rule_test");
    public static final ResourceKey<IRegistry<RuleBlockEntityModifierType<?>>> RULE_BLOCK_ENTITY_MODIFIER = createRegistryKey("rule_block_entity_modifier");
    public static final ResourceKey<IRegistry<Schedule>> SCHEDULE = createRegistryKey("schedule");
    public static final ResourceKey<IRegistry<SensorType<?>>> SENSOR_TYPE = createRegistryKey("sensor_type");
    public static final ResourceKey<IRegistry<SoundEffect>> SOUND_EVENT = createRegistryKey("sound_event");
    public static final ResourceKey<IRegistry<StatisticWrapper<?>>> STAT_TYPE = createRegistryKey("stat_type");
    public static final ResourceKey<IRegistry<WorldGenFeatureStructurePieceType>> STRUCTURE_PIECE = createRegistryKey("worldgen/structure_piece");
    public static final ResourceKey<IRegistry<StructurePlacementType<?>>> STRUCTURE_PLACEMENT = createRegistryKey("worldgen/structure_placement");
    public static final ResourceKey<IRegistry<WorldGenFeatureDefinedStructurePools<?>>> STRUCTURE_POOL_ELEMENT = createRegistryKey("worldgen/structure_pool_element");
    public static final ResourceKey<IRegistry<MapCodec<? extends PoolAliasBinding>>> POOL_ALIAS_BINDING = createRegistryKey("worldgen/pool_alias_binding");
    public static final ResourceKey<IRegistry<DefinedStructureStructureProcessorType<?>>> STRUCTURE_PROCESSOR = createRegistryKey("worldgen/structure_processor");
    public static final ResourceKey<IRegistry<StructureType<?>>> STRUCTURE_TYPE = createRegistryKey("worldgen/structure_type");
    public static final ResourceKey<IRegistry<WorldGenFeatureTrees<?>>> TREE_DECORATOR_TYPE = createRegistryKey("worldgen/tree_decorator_type");
    public static final ResourceKey<IRegistry<TrunkPlacers<?>>> TRUNK_PLACER_TYPE = createRegistryKey("worldgen/trunk_placer_type");
    public static final ResourceKey<IRegistry<VillagerProfession>> VILLAGER_PROFESSION = createRegistryKey("villager_profession");
    public static final ResourceKey<IRegistry<VillagerType>> VILLAGER_TYPE = createRegistryKey("villager_type");
    public static final ResourceKey<IRegistry<DecoratedPotPattern>> DECORATED_POT_PATTERN = createRegistryKey("decorated_pot_pattern");
    public static final ResourceKey<IRegistry<NumberFormatType<?>>> NUMBER_FORMAT_TYPE = createRegistryKey("number_format_type");
    public static final ResourceKey<IRegistry<DataComponentType<?>>> DATA_COMPONENT_TYPE = createRegistryKey("data_component_type");
    public static final ResourceKey<IRegistry<MapCodec<? extends EntitySubPredicate>>> ENTITY_SUB_PREDICATE_TYPE = createRegistryKey("entity_sub_predicate_type");
    public static final ResourceKey<IRegistry<ItemSubPredicate.a<?>>> ITEM_SUB_PREDICATE_TYPE = createRegistryKey("item_sub_predicate_type");
    public static final ResourceKey<IRegistry<MapDecorationType>> MAP_DECORATION_TYPE = createRegistryKey("map_decoration_type");
    public static final ResourceKey<IRegistry<DataComponentType<?>>> ENCHANTMENT_EFFECT_COMPONENT_TYPE = createRegistryKey("enchantment_effect_component_type");
    public static final ResourceKey<IRegistry<ConsumeEffect.a<?>>> CONSUME_EFFECT_TYPE = createRegistryKey("consume_effect_type");
    public static final ResourceKey<IRegistry<RecipeDisplay.a<?>>> RECIPE_DISPLAY = createRegistryKey("recipe_display");
    public static final ResourceKey<IRegistry<SlotDisplay.i<?>>> SLOT_DISPLAY = createRegistryKey("slot_display");
    public static final ResourceKey<IRegistry<RecipeBookCategory>> RECIPE_BOOK_CATEGORY = createRegistryKey("recipe_book_category");
    public static final ResourceKey<IRegistry<BiomeBase>> BIOME = createRegistryKey("worldgen/biome");
    public static final ResourceKey<IRegistry<ChatMessageType>> CHAT_TYPE = createRegistryKey("chat_type");
    public static final ResourceKey<IRegistry<WorldGenCarverWrapper<?>>> CONFIGURED_CARVER = createRegistryKey("worldgen/configured_carver");
    public static final ResourceKey<IRegistry<WorldGenFeatureConfigured<?, ?>>> CONFIGURED_FEATURE = createRegistryKey("worldgen/configured_feature");
    public static final ResourceKey<IRegistry<DensityFunction>> DENSITY_FUNCTION = createRegistryKey("worldgen/density_function");
    public static final ResourceKey<IRegistry<DimensionManager>> DIMENSION_TYPE = createRegistryKey("dimension_type");
    public static final ResourceKey<IRegistry<Enchantment>> ENCHANTMENT = createRegistryKey("enchantment");
    public static final ResourceKey<IRegistry<EnchantmentProvider>> ENCHANTMENT_PROVIDER = createRegistryKey("enchantment_provider");
    public static final ResourceKey<IRegistry<FlatLevelGeneratorPreset>> FLAT_LEVEL_GENERATOR_PRESET = createRegistryKey("worldgen/flat_level_generator_preset");
    public static final ResourceKey<IRegistry<GeneratorSettingBase>> NOISE_SETTINGS = createRegistryKey("worldgen/noise_settings");
    public static final ResourceKey<IRegistry<NoiseGeneratorNormal.a>> NOISE = createRegistryKey("worldgen/noise");
    public static final ResourceKey<IRegistry<PlacedFeature>> PLACED_FEATURE = createRegistryKey("worldgen/placed_feature");
    public static final ResourceKey<IRegistry<Structure>> STRUCTURE = createRegistryKey("worldgen/structure");
    public static final ResourceKey<IRegistry<ProcessorList>> PROCESSOR_LIST = createRegistryKey("worldgen/processor_list");
    public static final ResourceKey<IRegistry<StructureSet>> STRUCTURE_SET = createRegistryKey("worldgen/structure_set");
    public static final ResourceKey<IRegistry<WorldGenFeatureDefinedStructurePoolTemplate>> TEMPLATE_POOL = createRegistryKey("worldgen/template_pool");
    public static final ResourceKey<IRegistry<CriterionTrigger<?>>> TRIGGER_TYPE = createRegistryKey("trigger_type");
    public static final ResourceKey<IRegistry<TrimMaterial>> TRIM_MATERIAL = createRegistryKey("trim_material");
    public static final ResourceKey<IRegistry<TrimPattern>> TRIM_PATTERN = createRegistryKey("trim_pattern");
    public static final ResourceKey<IRegistry<WorldPreset>> WORLD_PRESET = createRegistryKey("worldgen/world_preset");
    public static final ResourceKey<IRegistry<MultiNoiseBiomeSourceParameterList>> MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST = createRegistryKey("worldgen/multi_noise_biome_source_parameter_list");
    public static final ResourceKey<IRegistry<TrialSpawnerConfig>> TRIAL_SPAWNER_CONFIG = createRegistryKey("trial_spawner");
    public static final ResourceKey<IRegistry<World>> DIMENSION = createRegistryKey("dimension");
    public static final ResourceKey<IRegistry<WorldDimension>> LEVEL_STEM = createRegistryKey("dimension");
    public static final ResourceKey<IRegistry<LootTable>> LOOT_TABLE = createRegistryKey("loot_table");
    public static final ResourceKey<IRegistry<LootItemFunction>> ITEM_MODIFIER = createRegistryKey("item_modifier");
    public static final ResourceKey<IRegistry<LootItemCondition>> PREDICATE = createRegistryKey("predicate");
    public static final ResourceKey<IRegistry<Advancement>> ADVANCEMENT = createRegistryKey("advancement");
    public static final ResourceKey<IRegistry<IRecipe<?>>> RECIPE = createRegistryKey("recipe");

    public Registries() {}

    public static ResourceKey<World> levelStemToLevel(ResourceKey<WorldDimension> resourcekey) {
        return ResourceKey.create(Registries.DIMENSION, resourcekey.location());
    }

    public static ResourceKey<WorldDimension> levelToLevelStem(ResourceKey<World> resourcekey) {
        return ResourceKey.create(Registries.LEVEL_STEM, resourcekey.location());
    }

    private static <T> ResourceKey<IRegistry<T>> createRegistryKey(String s) {
        return ResourceKey.createRegistryKey(MinecraftKey.withDefaultNamespace(s));
    }

    public static String elementsDirPath(ResourceKey<? extends IRegistry<?>> resourcekey) {
        return resourcekey.location().getPath();
    }

    public static String tagsDirPath(ResourceKey<? extends IRegistry<?>> resourcekey) {
        return "tags/" + resourcekey.location().getPath();
    }
}
