package net.minecraft.world.flag;

import com.mojang.serialization.Codec;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.MinecraftKey;

public class FeatureFlags {

    public static final FeatureFlag VANILLA;
    public static final FeatureFlag TRADE_REBALANCE;
    public static final FeatureFlag REDSTONE_EXPERIMENTS;
    public static final FeatureFlag MINECART_IMPROVEMENTS;
    public static final FeatureFlagRegistry REGISTRY;
    public static final Codec<FeatureFlagSet> CODEC;
    public static final FeatureFlagSet VANILLA_SET;
    public static final FeatureFlagSet DEFAULT_FLAGS;

    public FeatureFlags() {}

    public static String printMissingFlags(FeatureFlagSet featureflagset, FeatureFlagSet featureflagset1) {
        return printMissingFlags(FeatureFlags.REGISTRY, featureflagset, featureflagset1);
    }

    public static String printMissingFlags(FeatureFlagRegistry featureflagregistry, FeatureFlagSet featureflagset, FeatureFlagSet featureflagset1) {
        Set<MinecraftKey> set = featureflagregistry.toNames(featureflagset1);
        Set<MinecraftKey> set1 = featureflagregistry.toNames(featureflagset);

        return (String) set.stream().filter((minecraftkey) -> {
            return !set1.contains(minecraftkey);
        }).map(MinecraftKey::toString).collect(Collectors.joining(", "));
    }

    public static boolean isExperimental(FeatureFlagSet featureflagset) {
        return !featureflagset.isSubsetOf(FeatureFlags.VANILLA_SET);
    }

    static {
        FeatureFlagRegistry.a featureflagregistry_a = new FeatureFlagRegistry.a("main");

        VANILLA = featureflagregistry_a.createVanilla("vanilla");
        TRADE_REBALANCE = featureflagregistry_a.createVanilla("trade_rebalance");
        REDSTONE_EXPERIMENTS = featureflagregistry_a.createVanilla("redstone_experiments");
        MINECART_IMPROVEMENTS = featureflagregistry_a.createVanilla("minecart_improvements");
        REGISTRY = featureflagregistry_a.build();
        CODEC = FeatureFlags.REGISTRY.codec();
        VANILLA_SET = FeatureFlagSet.of(FeatureFlags.VANILLA);
        DEFAULT_FLAGS = FeatureFlags.VANILLA_SET;
    }
}
