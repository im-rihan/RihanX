package com.rihanx.farm;

import com.rihanx.base.BaseTemplates;
import com.rihanx.base.BlueprintValidator;
import com.rihanx.base.FarmTemplates;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Registry of farm blueprints. Adapts existing {@link FarmTemplates} generators into
 * {@link FarmPlan} for preview / validate / material calculation without rewriting farms.
 */
public final class FarmRegistry {

    private static final FarmRegistry INSTANCE = new FarmRegistry();

    private final @NotNull Map<String, FarmBlueprint> byId = new LinkedHashMap<>();

    private FarmRegistry() {
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : FarmTemplates.all().entrySet()) {
            byId.put(entry.getKey(), new LegacyFarmBlueprint(entry.getValue()));
        }
    }

    public static @NotNull FarmRegistry get() {
        return INSTANCE;
    }

    public @NotNull Set<String> ids() {
        return byId.keySet();
    }

    public @Nullable FarmBlueprint get(@NotNull String id) {
        return byId.get(id.toLowerCase(Locale.ROOT).trim());
    }

    public @NotNull FarmPlan plan(@NotNull String id) {
        FarmBlueprint bp = get(id);
        if (bp == null) {
            throw new IllegalArgumentException("Unknown farm: " + id);
        }
        return bp.plan(null);
    }

    public static final class LegacyFarmBlueprint implements FarmBlueprint {
        private final @NotNull BaseTemplates.BaseBlueprint source;

        public LegacyFarmBlueprint(@NotNull BaseTemplates.BaseBlueprint source) {
            this.source = source;
        }

        @Override
        public @NotNull String getId() {
            return source.id();
        }

        @Override
        public @NotNull String getDescription() {
            return source.description();
        }

        @Override
        public @NotNull FarmPlan plan(@Nullable FarmBuildContext context) {
            List<BlockPlacement> placements = new ArrayList<>(source.blocks().size());
            for (BaseTemplates.RelBlock rel : source.blocks()) {
                BlockPlacement.BuildStage stage = classify(rel.material());
                placements.add(new BlockPlacement(
                        rel.dx(), rel.dy(), rel.dz(), rel.material(),
                        rel.facing(), rel.slabType(), rel.hinge(),
                        rel.upperHalf(), rel.hanging(), stage
                ));
            }
            FarmPlan.Builder b = FarmPlan.builder(source.id(), source.description())
                    .spawn(source.spawnDx(), source.spawnDy(), source.spawnDz())
                    .meta("generation", FarmBuildContext.GENERATION_VERSION)
                    .meta("adapter", "LegacyFarmBlueprint")
                    .meta("mechanic-doc", "docs/FARM_MECHANICS.md");
            for (BlockPlacement p : placements) {
                b.add(p);
            }
            return b.build();
        }

        @Override
        public @NotNull List<String> validate(@Nullable FarmBuildContext context, @NotNull FarmPlan plan) {
            List<String> errors = new ArrayList<>();
            errors.addAll(FarmPlanValidator.validatePlan(plan));
            errors.addAll(BlueprintValidator.validateFarm(source));
            errors.addAll(BlueprintValidator.validateHopperOutputsReachStorage(source));
            return errors;
        }

        private static @NotNull BlockPlacement.BuildStage classify(@NotNull Material material) {
            String n = material.name();
            if (n.equals("WATER") || n.equals("LAVA")) {
                return BlockPlacement.BuildStage.FLUIDS;
            }
            if (n.equals("HOPPER") || n.contains("CHEST") || n.equals("BARREL")) {
                return BlockPlacement.BuildStage.COLLECTION;
            }
            if (n.contains("OBSERVER") || n.contains("PISTON") || n.contains("REDSTONE")
                    || n.contains("REPEATER") || n.contains("COMPARATOR") || n.equals("LEVER")
                    || n.contains("BUTTON") || n.equals("DAYLIGHT_DETECTOR")) {
                return BlockPlacement.BuildStage.REDSTONE;
            }
            if (n.equals("SPAWNER") || n.equals("IRON_BARS") || n.contains("BED")) {
                return BlockPlacement.BuildStage.MOB_SYSTEMS;
            }
            if (n.equals("LANTERN") || n.equals("SOUL_LANTERN") || n.equals("IRON_CHAIN")
                    || n.contains("SIGN") || n.contains("CARPET")) {
                return BlockPlacement.BuildStage.DECORATIVE;
            }
            if (n.contains("FARMLAND") || n.equals("SOUL_SAND") || n.equals("SAND")
                    || n.equals("PODZOL") || n.equals("DIRT")) {
                return BlockPlacement.BuildStage.TERRAIN;
            }
            return BlockPlacement.BuildStage.FUNCTIONAL;
        }
    }
}
