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
 * BadpacketE - Erkennt ungültige Yaw-Änderungen
 * Prüft ob der Yaw (Blickwinkel horizontal) sich zu schnell ändert
 * Menschliche Spieler können ihren Yaw nicht schneller als ~180° pro Tick ändern
 */
public class BadpacketE implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();

    // Maximale Yaw-Änderung pro Tick (in Grad)
    private static final float MAX_YAW_CHANGE = 180.0f;

    public BadpacketE(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isSpeedEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        float currentYaw = normalizeYaw(event.getTo().getYaw());
        Float previousYaw = lastYaw.get(uuid);

        if (previousYaw != null) {
            float yawDifference = Math.abs(getYawDifference(previousYaw, currentYaw));

            // Prüfe auf unmögliche Yaw-Änderung
            if (yawDifference > MAX_YAW_CHANGE) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        player.getName() + " hat unmögliche Yaw-Änderung durchgeführt! " +
                                "Verstoß #" + violationCount +
                                " (Yaw-Diff: " + String.format("%.2f", yawDifference) + "°)"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getSpeedViolationLimit()) {
                    handleViolation(player);
                }

                // Bewegung abbrechen
                event.setCancelled(true);
            } else {
                // Reduziere Verstöße bei normaler Rotation
                if (violations.containsKey(uuid)) {
                    int current = violations.get(uuid);
                    if (current > 0) {
                        violations.put(uuid, current - 1);
                    }
                }
            }
        }

        lastYaw.put(uuid, currentYaw);
    }

    /**
     * Normalisiert Yaw-Werte auf Bereich -180 bis 180
     */
    private float normalizeYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw > 180) {
            yaw -= 360;
        } else if (yaw < -180) {
            yaw += 360;
        }
        return yaw;
    }

    /**
     * Berechnet die kürzeste Distanz zwischen zwei Yaw-Werten
     */
    private float getYawDifference(float yaw1, float yaw2) {
        float diff = Math.abs(yaw1 - yaw2);
        if (diff > 180) {
            diff = 360 - diff;
        }
        return diff;
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: Ungültige Packets (BadpacketE)!");
        violations.remove(player.getUniqueId());
        lastYaw.remove(player.getUniqueId());
    }
}