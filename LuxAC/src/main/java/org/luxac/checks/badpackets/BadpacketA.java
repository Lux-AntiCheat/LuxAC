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
 * BadpacketA - Erkennt ungültige Pitch-Werte
 * Prüft ob der Pitch (Blickwinkel vertikal) außerhalb des erlaubten Bereichs liegt
 * Erlaubter Bereich: -90.0 bis 90.0 Grad
 */
public class BadpacketA implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();

    public BadpacketA(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isSpeedEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        float pitch = player.getLocation().getPitch();

        // Prüfe ob Pitch außerhalb des gültigen Bereichs liegt
        if (pitch < -90.0f || pitch > 90.0f) {
            int violationCount = violations.getOrDefault(uuid, 0) + 1;
            violations.put(uuid, violationCount);

            plugin.getLogger().warning(
                    player.getName() + " hat ungültigen Pitch-Wert gesendet! " +
                            "Verstoß #" + violationCount + " (Pitch: " + pitch + ")"
            );

            if (violationCount >= plugin.getAntiCheatConfig().getSpeedViolationLimit()) {
                handleViolation(player);
            }

            // Bewegung abbrechen
            event.setCancelled(true);
        } else {
            // Reduziere Verstöße bei korrektem Verhalten
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: Ungültige Packets (BadpacketA)!");
        violations.remove(player.getUniqueId());
    }
}