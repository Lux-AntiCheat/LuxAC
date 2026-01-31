package org.luxac.checks.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.luxac.LuxAC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FastEat - Erkennt zu schnelles Essen/Trinken
 * Prüft ob Spieler Items schneller als normal konsumieren
 */
public class FastEat implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Long> lastEatTime = new HashMap<>();

    // Minimale Zeit zwischen Essen (in Millisekunden)
    // Normal: ~1600ms (32 Ticks)
    private static final long MIN_EAT_TIME = 1400;

    public FastEat(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getAntiCheatConfig().isFastEatEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Long lastEat = lastEatTime.get(uuid);

        if (lastEat != null) {
            long timeDiff = currentTime - lastEat;

            // Spieler hat zu schnell gegessen
            if (timeDiff < MIN_EAT_TIME) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        player.getName() + " hat zu schnell gegessen! " +
                                "Verstoß #" + violationCount +
                                " (Zeit: " + timeDiff + "ms < " + MIN_EAT_TIME + "ms)"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getFastEatViolationLimit()) {
                    handleViolation(player);
                }

                // Event abbrechen
                event.setCancelled(true);
            } else {
                // Reduziere Verstöße bei normalem Essen
                if (violations.containsKey(uuid)) {
                    int current = violations.get(uuid);
                    if (current > 0) {
                        violations.put(uuid, current - 1);
                    }
                }
            }
        }

        lastEatTime.put(uuid, currentTime);
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: FastEat erkannt!");
        violations.remove(player.getUniqueId());
        lastEatTime.remove(player.getUniqueId());
    }
}