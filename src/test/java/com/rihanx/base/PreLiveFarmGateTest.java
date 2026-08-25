package com.rihanx.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Go-live gate: run this (or {@code scripts/pre-live-test.ps1}) before deploying.
 * Validates all 24 farm blueprints — structure, hoppers, playability, and recent layout fixes.
 */
class PreLiveFarmGateTest {

    @Test
    @DisplayName("PRE-LIVE: all 24 farms pass full validation gate")
    void allFarmsPassPreLiveGate() {
        Map<String, BaseTemplates.BaseBlueprint> farms = FarmTemplates.all();
        assertEquals(24, farms.size(), "farm catalog size changed — update gate tests");

        List<String> failures = new ArrayList<>();
        Map<String, Set<String>> report = new LinkedHashMap<>();

        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : farms.entrySet()) {
            String id = entry.getKey();
            Set<String> errors = BlueprintValidator.validatePreLive(entry.getValue());
            if (!errors.isEmpty()) {
                failures.add(id);
                report.put(id, errors);
            }
        }

        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder("PRE-LIVE GATE FAILED — do not deploy:\n");
            report.forEach((id, errs) -> sb.append("  ").append(id).append(": ").append(errs).append('\n'));
            assertTrue(failures.isEmpty(), sb.toString());
        }
    }

    @Test
    @DisplayName("PRE-LIVE: XP natural pads are 24+ blocks from AFK window (3D sqrt)")
    void xpPadDistanceUsesCorrect3dRange() {
        BaseTemplates.BaseBlueprint xp = FarmTemplates.all().get("xp");
        double closest = BlueprintValidator.closestXpPadDistance(xp);
        assertTrue(closest >= 24.0,
                "closest pad must be ≥24 blocks (3D), was " + closest);
        // Closest dry pad is on the north ring (south cells are non-spawn slabs near AFK)
        assertTrue(closest <= 30.0, "sanity: closest pad should be near the 24-block ring, was " + closest);
    }

    @Test
    @DisplayName("PRE-LIVE: XP spawner farms load player within spawner range")
    void xpSpawnerFarmsWithinActivationRange() {
        for (String id : List.of("xp-zombie", "xp-skeleton", "xp-spider")) {
            BaseTemplates.BaseBlueprint bp = FarmTemplates.all().get(id);
            double maxDist = 0;
            for (BaseTemplates.RelBlock block : bp.blocks()) {
                if (block.material() != org.bukkit.Material.SPAWNER) {
                    continue;
                }
                double dist = Math.sqrt(
                        (double) block.dx() * block.dx()
                                + Math.pow(block.dy() - 1, 2)
                                + Math.pow(block.dz() - XpFarmTemplates.AFK_WINDOW_Z, 2)
                );
                maxDist = Math.max(maxDist, dist);
            }
            assertTrue(maxDist <= 32.0, id + " spawner too far from window: " + maxDist);
            assertTrue(maxDist >= 5.0, id + " spawner suspiciously close: " + maxDist);
        }
    }

    @Test
    @DisplayName("PRE-LIVE: chicken/cow/pig have lava cooker + water canal")
    void livestockCookersWired() {
        for (String id : List.of("chicken", "cow", "pig")) {
            Set<String> errors = BlueprintValidator.validatePreLive(FarmTemplates.all().get(id));
            assertTrue(errors.stream().noneMatch(e -> e.contains("no-lava-cooker")),
                    id + " missing lava cooker");
            assertTrue(errors.stream().noneMatch(e -> e.contains("no-water-canal")),
                    id + " missing water canal");
            assertTrue(errors.isEmpty(), id + " pre-live errors: " + errors);
        }
    }

    @Test
    @DisplayName("PRE-LIVE: bamboo collects from both sides of every stalk")
    void bambooDualSideHoppers() {
        Set<String> errors = BlueprintValidator.validatePreLive(FarmTemplates.all().get("bamboo"));
        assertTrue(errors.stream().noneMatch(e -> e.startsWith("bamboo-missing")),
                "bamboo dual hoppers: " + errors);
        assertTrue(errors.isEmpty(), "bamboo pre-live: " + errors);
    }

    @Test
    @DisplayName("PRE-LIVE: slime/redstone kill boxes are sealed")
    void slimeAndWitchKillBoxesSealed() {
        for (String id : List.of("slime", "redstone")) {
            Set<String> errors = BlueprintValidator.validatePreLive(FarmTemplates.all().get(id));
            assertTrue(errors.stream().noneMatch(e -> e.startsWith("slime-kill")),
                    id + " escape gap: " + errors);
            assertTrue(errors.isEmpty(), id + " pre-live: " + errors);
        }
    }

    @Test
    @DisplayName("PRE-LIVE: every farm hopper chain reaches storage")
    void everyFarmHopperReachStorage() {
        for (Map.Entry<String, BaseTemplates.BaseBlueprint> entry : FarmTemplates.all().entrySet()) {
            if ("animal".equals(entry.getKey())) {
                continue; // feed chests only
            }
            Set<String> dead = BlueprintValidator.validateHopperOutputsReachStorage(entry.getValue());
            assertTrue(dead.isEmpty(), entry.getKey() + " hopper dead-ends: " + dead);
        }
    }
}
