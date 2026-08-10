package com.rihanx.portal;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * File-backed portal link persistence without a live Bukkit world.
 */
class PortalStoreLinkTest {

    @TempDir
    Path tempDir;

    @Test
    void bidirectionalLinkAndUnlinkPersist() {
        File file = tempDir.resolve("portals.yml").toFile();
        PortalStore store = new PortalStore(file, null);

        assertTrue(store.putRaw("north", new PortalStore.StoredPortal(
                "world", 10, 64, 20, 0f, 0f, null)));
        assertTrue(store.putRaw("south", new PortalStore.StoredPortal(
                "world", 100, 64, 200, 180f, 0f, null)));

        assertTrue(store.setLink("north", "south"));
        assertTrue(store.setLink("south", "north"));

        assertEquals("south", store.get("north").link());
        assertEquals("north", store.get("south").link());

        // Reload from disk
        PortalStore reloaded = new PortalStore(file, null);
        assertEquals(Set.of("north", "south"), Set.copyOf(reloaded.list()));
        assertEquals("south", reloaded.get("north").link());
        assertEquals("north", reloaded.get("south").link());

        assertTrue(reloaded.delete("north"));
        assertNull(reloaded.get("north"));
        assertNotNull(reloaded.get("south"));
        assertNull(reloaded.get("south").link(), "delete should clear reverse links");
    }

    @Test
    void yamlContainsLinkKeys() {
        File file = tempDir.resolve("portals.yml").toFile();
        PortalStore store = new PortalStore(file, null);
        store.putRaw("a", new PortalStore.StoredPortal("world", 1, 2, 3, 0, 0, null));
        store.putRaw("b", new PortalStore.StoredPortal("world", 4, 5, 6, 0, 0, "a"));
        store.setLink("a", "b");

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        assertEquals("b", yaml.getString("portals.a.link"));
        assertEquals("a", yaml.getString("portals.b.link"));
        assertFalse(yaml.getString("portals.a.world", "").isBlank());
    }

    @Test
    void listIsSortedCaseInsensitive() {
        File file = tempDir.resolve("portals.yml").toFile();
        PortalStore store = new PortalStore(file, null);
        store.putRaw("zeta", new PortalStore.StoredPortal("w", 0, 0, 0, 0, 0, null));
        store.putRaw("alpha", new PortalStore.StoredPortal("w", 0, 0, 0, 0, 0, null));
        assertEquals(
                java.util.List.of("alpha", "zeta"),
                store.list().stream().collect(Collectors.toList())
        );
    }
}
