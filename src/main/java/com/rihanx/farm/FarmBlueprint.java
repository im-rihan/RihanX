package com.rihanx.farm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Farm generator contract: plan → validate → (build via existing BaseService paste).
 */
public interface FarmBlueprint {

    @NotNull String getId();

    @NotNull String getDescription();

    /**
     * Calculate every block placement. {@code context} may be null for dry-run planning.
     */
    @NotNull FarmPlan plan(@Nullable FarmBuildContext context);

    /**
     * Dry-run validation. Must not modify the world.
     */
    @NotNull List<String> validate(@Nullable FarmBuildContext context, @NotNull FarmPlan plan);
}
