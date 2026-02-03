package dev.mynny.deathChest.manager;

import dev.mynny.deathChest.Main;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BodyManager {

    private final Main plugin;

    public BodyManager(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, BodyData> bodies = new HashMap<>();

    public void registerBody(ArmorStand stand, Inventory inventory, Location location) {
        bodies.put(stand.getUniqueId(), new BodyData(stand, inventory, location));
    }

    public BodyData getBody(UUID standId) {
        return bodies.get(standId);
    }

    public BodyData getBodyByInventory(Inventory inventory) {
        for (BodyData data : bodies.values()) {
            if (data.inventory.equals(inventory)) {
                return data;
            }
        }
        return null;
    }

    public BodyData findNearest(Location location, double radius) {
        double radiusSquared = radius * radius;
        BodyData closest = null;
        double closestDistance = radiusSquared + 1.0;
        for (BodyData data : bodies.values()) {
            if (!data.location.getWorld().equals(location.getWorld())) {
                continue;
            }
            double distance = data.location.distanceSquared(location);
            if (distance <= radiusSquared && distance < closestDistance) {
                closest = data;
                closestDistance = distance;
            }
        }
        return closest;
    }

    public void removeBody(UUID standId) {
        BodyData data = bodies.remove(standId);
        if (data != null && !data.stand.isDead()) {
            data.stand.remove();
        }
    }

    public void startBodyChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Set<UUID> toRemove = new HashSet<>();
                for (Map.Entry<UUID, BodyData> entry : bodies.entrySet()) {
                    BodyData data = entry.getValue();
                    if (data.stand.isDead() || data.inventory.isEmpty()) {
                        data.stand.remove();
                        toRemove.add(entry.getKey());
                    }
                }
                toRemove.forEach(bodies::remove);
            }
        }.runTaskTimer(plugin, 20 * 10L, 20 * 10L);
    }

    public void removeAllBodies() {
        for (BodyData data : bodies.values()) {
            if (!data.stand.isDead()) {
                data.stand.remove();
            }
        }
        bodies.clear();
    }

    public static class BodyData {
        private final ArmorStand stand;
        private final Inventory inventory;
        private final Location location;

        public BodyData(ArmorStand stand, Inventory inventory, Location location) {
            this.stand = stand;
            this.inventory = inventory;
            this.location = location.clone();
        }

        public ArmorStand getStand() {
            return stand;
        }

        public Inventory getInventory() {
            return inventory;
        }

        public Location getLocation() {
            return location.clone();
        }
    }
}
