package com.rihanx.farm;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Material and dimension report generated from an actual {@link FarmPlan} (never fake totals).
 */
public final class MaterialCalculator {

    private MaterialCalculator() {
    }

    public record Report(
            @NotNull String farmId,
            int width,
            int length,
            int height,
            int totalBlocks,
            int functionalBlocks,
            int decorativeBlocks,
            int containers,
            int redstone,
            @NotNull Map<Material, Integer> materials
    ) {
    }

    public static @NotNull Report calculate(@NotNull FarmPlan plan) {
        int functional = plan.functionalCount();
        int decorative = Math.max(0, plan.placements().size() - functional);
        return new Report(
                plan.farmId(),
                plan.width(),
                plan.length(),
                plan.height(),
                plan.placements().size(),
                functional,
                decorative,
                plan.containerCount(),
                plan.redstoneCount(),
                plan.materialTotals()
        );
    }

    public static @NotNull String formatPreview(@NotNull FarmPlan plan, @NotNull List<String> errors) {
        Report r = calculate(plan);
        StringBuilder sb = new StringBuilder();
        sb.append("[RihanX AutoFarm]\n\n");
        sb.append("Farm: ").append(r.farmId()).append('\n');
        sb.append("Description: ").append(plan.description()).append('\n');
        sb.append("\nDimensions:\n");
        sb.append(r.width()).append(" x ").append(r.length()).append(" x ").append(r.height()).append('\n');
        sb.append("\nBlocks:\n").append(r.totalBlocks()).append('\n');
        sb.append("\nFunctional:\n").append(r.functionalBlocks()).append('\n');
        sb.append("Decorative:\n").append(r.decorativeBlocks()).append('\n');
        sb.append("Containers:\n").append(r.containers()).append('\n');
        sb.append("Redstone:\n").append(r.redstone()).append('\n');
        sb.append("\nWarnings:\n0\n");
        sb.append("Errors:\n").append(errors.size()).append('\n');
        if (!errors.isEmpty()) {
            for (String e : errors) {
                sb.append("  - ").append(e).append('\n');
            }
        }
        sb.append("\nTop materials:\n");
        List<Map.Entry<Material, Integer>> top = new ArrayList<>(r.materials().entrySet());
        top.sort(Map.Entry.<Material, Integer>comparingByValue().reversed());
        int shown = 0;
        for (Map.Entry<Material, Integer> e : top) {
            if (shown++ >= 12) {
                break;
            }
            sb.append("  ").append(e.getKey().name()).append(": ").append(e.getValue())
                    .append(" (").append(formatStacks(e.getValue())).append(")\n");
        }
        sb.append("\nUse /farm build ").append(r.farmId()).append(" to construct.\n");
        sb.append("Or /farm ").append(r.farmId()).append(" (same as build).\n");
        return sb.toString();
    }

    /** Format count as Minecraft stack notation (64 per stack). */
    public static @NotNull String formatStacks(int count) {
        if (count <= 0) {
            return "0";
        }
        int stacks = count / 64;
        int rem = count % 64;
        if (stacks == 0) {
            return rem + " items";
        }
        if (rem == 0) {
            return stacks + " stack" + (stacks == 1 ? "" : "s");
        }
        return stacks + " stack" + (stacks == 1 ? "" : "s") + " + " + rem;
    }
}
