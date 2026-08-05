package com.rihanx.protection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for region priority / containment helpers.
 */
class RegionResolveTest {

    @Test
    void higherPrioritySortsBeforeLower() {
        Region low = new Region("low", "world", 0, 0, 0, 10, 10, 10);
        low.setPriority(1);
        Region high = new Region("high", "world", 0, 0, 0, 5, 5, 5);
        high.setPriority(10);

        List<Region> list = new ArrayList<>(List.of(low, high));
        list.sort((a, b) -> {
            int byPriority = Integer.compare(b.getPriority(), a.getPriority());
            if (byPriority != 0) {
                return byPriority;
            }
            return Long.compare(a.volume(), b.volume());
        });

        assertEquals("high", list.getFirst().getName());
        assertEquals("low", list.get(1).getName());
    }

    @Test
    void samePriorityPrefersSmallerVolume() {
        Region big = new Region("big", "world", 0, 0, 0, 20, 20, 20);
        big.setPriority(5);
        Region small = new Region("small", "world", 0, 0, 0, 2, 2, 2);
        small.setPriority(5);

        List<Region> list = new ArrayList<>(List.of(big, small));
        list.sort(Comparator
                .comparingInt(Region::getPriority).reversed()
                .thenComparingLong(Region::volume));

        assertEquals("small", list.getFirst().getName());
    }

    @Test
    void containsInclusiveBounds() {
        Region region = new Region("plot", "world", 0, 64, 0, 15, 80, 15);
        assertTrue(region.contains(0, 64, 0));
        assertTrue(region.contains(15, 80, 15));
        assertTrue(region.contains(7, 70, 7));
    }

    @Test
    void ownerCountsAsMember() {
        java.util.UUID id = java.util.UUID.randomUUID();
        Region region = new Region("base", "world", 0, 0, 0, 1, 1, 1);
        region.addOwner(id);
        assertTrue(region.isOwner(id));
        assertTrue(region.isMember(id));
    }
}
