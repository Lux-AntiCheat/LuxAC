package org.luxac.checks.badpackets;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.luxac.LuxAC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BadpacketC - Erkennt unmögliche Bewegungen (Teleport ohne Grund)
 * Prüft ob der Spieler sich zu schnell über große Distanzen bewegt
 * ohne legitime Gründe (Teleport, Enderpearl, etc.)
 */
public class BadpacketC implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new HashMap<>();

    // Maximale erlaubte Distanz pro Tick (in Blöcken)
    private static final double MAX_DISTANCE_PER_TICK = 10.0;

    public BadpacketC(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isSpeedEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Ignoriere wenn Spieler fliegen darf oder im Creative ist
        if (player.isFlying() || player.getAllowFlight()) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());

        // Prüfe auf unmögliche Bewegung
        if (distance > MAX_DISTANCE_PER_TICK) {
            // Prüfe ob kürzlich teleportiert wurde (Gnade-Periode von 500ms)
            Long lastTeleport = lastTeleportTime.get(uuid);
            long currentTime = System.currentTimeMillis();

            if (lastTeleport != null && (currentTime - lastTeleport) < 500) {
                // Legitimer Teleport, ignorieren
                return;
            }

            int violationCount = violations.getOrDefault(uuid, 0) + 1;
            violations.put(uuid, violationCount);

            plugin.getLogger().warning(
                    player.getName() + " hat unmögliche Bewegung durchgeführt! " +
                            "Verstoß #" + violationCount +
                            " (Distanz: " + String.format("%.2f", distance) + " Blöcke)"
            );

            if (violationCount >= plugin.getAntiCheatConfig().getSpeedViolationLimit()) {
                handleViolation(player);
            }

            // Spieler zurücksetzen
            event.setCancelled(true);
            player.teleport(event.getFrom());
            lastTeleportTime.put(uuid, currentTime);
        } else {
            // Reduziere Verstöße bei normaler Bewegung
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: Ungültige Packets (BadpacketC)!");
        violations.remove(player.getUniqueId());
        lastTeleportTime.remove(player.getUniqueId());
    }
}