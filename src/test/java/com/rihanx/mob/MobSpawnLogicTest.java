package com.rihanx.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobSpawnLogicTest {

    @Test
    void parseVillagerDefaults() {
        MobSpawnService.SpawnRequest request = MobSpawnService.parse(new String[]{"villager"}, 16);
        assertNotNull(request);
        assertEquals(MobSpawnService.Kind.VILLAGER, request.kind());
        assertEquals(1, request.amount());
        assertEquals("none", request.professionId());
        assertTrue(request.nametag());
        assertFalse(request.minecart());
    }

    @Test
    void parseVillagerFarmerCount() {
        MobSpawnService.SpawnRequest request = MobSpawnService.parse(
                new String[]{"v", "farmer", "3"}, 16);
        assertNotNull(request);
        assertEquals(3, request.amount());
        assertEquals("farmer", request.professionId());
    }

    @Test
    void parseZombieMinecart() {
        MobSpawnService.SpawnRequest request = MobSpawnService.parse(
                new String[]{"zombie", "minecart", "2"}, 16);
        assertNotNull(request);
        assertEquals(MobSpawnService.Kind.ZOMBIE, request.kind());
        assertEquals(2, request.amount());
        assertTrue(request.minecart());
        assertTrue(request.nametag());
    }

    @Test
    void parseCapsAmountAtMax() {
        MobSpawnService.SpawnRequest request = MobSpawnService.parse(
                new String[]{"villager", "99"}, 8);
        assertNotNull(request);
        assertEquals(8, request.amount());
    }

    @Test
    void parseUnknownTypeOrFlagFails() {
        assertNull(MobSpawnService.parse(new String[]{"creeper"}, 16));
        assertNull(MobSpawnService.parse(new String[]{"villager", "dragon"}, 16));
        assertNull(MobSpawnService.parse(new String[]{}, 16));
    }
}
