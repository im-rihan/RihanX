package com.rihanx.protection;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionFlagTest {

    @Test
    void fromKeyResolvesCanonicalAndAliases() {
        assertEquals(ProtectionFlag.BUILD, ProtectionFlag.fromKey("build"));
        assertEquals(ProtectionFlag.CREEPER_EXPLOSION, ProtectionFlag.fromKey("creeper-explosion"));
        assertEquals(ProtectionFlag.CREEPER_EXPLOSION, ProtectionFlag.fromKey("creeper_explosion"));
        assertEquals(ProtectionFlag.CHEST_ACCESS, ProtectionFlag.fromKey("chest-access"));
        assertEquals(ProtectionFlag.ITEM_DROP, ProtectionFlag.fromKey("ITEM_DROP"));
    }

    @Test
    void fromKeyReturnsNullForUnknown() {
        assertNull(ProtectionFlag.fromKey("not-a-flag"));
        assertNull(ProtectionFlag.fromKey("   "));
    }

    @Test
    void keysCoversEveryFlag() {
        String[] keys = ProtectionFlag.keys();
        assertEquals(ProtectionFlag.values().length, keys.length);
        Set<String> unique = new HashSet<>(Arrays.asList(keys));
        assertEquals(keys.length, unique.size());
        for (ProtectionFlag flag : ProtectionFlag.values()) {
            assertTrue(unique.contains(flag.key()), "missing key " + flag.key());
            assertNotNull(ProtectionFlag.fromKey(flag.key()));
        }
    }
}
