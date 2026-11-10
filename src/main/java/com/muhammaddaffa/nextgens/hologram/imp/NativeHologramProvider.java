package com.muhammaddaffa.nextgens.hologram.imp;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.nextgens.hologram.HologramProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in hologram provider that uses vanilla text display entities, so no hologram plugin is needed.
 * The entities are not persistent: they are never saved to the world and vanish on chunk unload or
 * restart, after which the generator task spawns them again.
 */
public class NativeHologramProvider implements HologramProvider {

    private final Map<String, TextDisplay> entities = new ConcurrentHashMap<>();
    private final Set<String> pending = ConcurrentHashMap.newKeySet();

    @Override
    public String id() {
        return "Native";
    }

    @Override
    public String pluginName() {
        return "NextGens";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void spawn(String name, Location location, List<String> lines) {
        World world = location.getWorld();
        if (world == null) return;
        String text = String.join("\n", lines);
        this.pending.add(name);
        ExecutorManager.getProvider().region(world, location.getBlockX() >> 4, location.getBlockZ() >> 4, () -> {
            // destroyed before the spawn task ran
            if (!this.pending.remove(name)) return;
            removeEntity(this.entities.remove(name));
            TextDisplay display = world.spawn(location, TextDisplay.class, entity -> {
                entity.setPersistent(false);
                entity.setText(text);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setShadowed(true);
            });
            this.entities.put(name, display);
        });
    }

    @Override
    public void destroy(String name, Location location) {
        this.pending.remove(name);
        TextDisplay display = this.entities.remove(name);
        if (display == null) return;
        World world = location.getWorld();
        if (world == null) return;
        ExecutorManager.getProvider().region(world, location.getBlockX() >> 4, location.getBlockZ() >> 4,
                () -> removeEntity(display));
    }

    @Override
    public boolean isSpawned(String name) {
        if (this.pending.contains(name)) return true;
        TextDisplay display = this.entities.get(name);
        return display != null && display.isValid();
    }

    private static void removeEntity(Entity entity) {
        if (entity != null) entity.remove();
    }

}
