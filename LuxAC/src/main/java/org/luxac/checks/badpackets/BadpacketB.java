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
 * BadpacketB - Erkennt doppelte Position Packets
 * Prüft ob der Spieler mehrmals dieselbe Position sendet ohne sich zu bewegen
 * Dies kann auf modifizierte Clients oder Packet-Manipulation hindeuten
 */
public class BadpacketB implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, String> lastPosition = new HashMap<>();
    private final Map<UUID, Integer> duplicateCount = new HashMap<>();

    public BadpacketB(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isSpeedEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Position als String speichern (gerundet auf 3 Dezimalstellen)
        String currentPos = String.format("%.3f,%.3f,%.3f",
                event.getTo().getX(),
                event.getTo().getY(),
                event.getTo().getZ()
        );

        String lastPos = lastPosition.get(uuid);

        if (lastPos != null && lastPos.equals(currentPos)) {
            // Spieler sendet dieselbe Position mehrmals
            int duplicates = duplicateCount.getOrDefault(uuid, 0) + 1;
            duplicateCount.put(uuid, duplicates);

            // Erst nach mehreren aufeinanderfolgenden Duplikaten flaggen
            if (duplicates > 5) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        player.getName() + " sendet doppelte Position Packets! " +
                                "Verstoß #" + violationCount + " (Duplikate: " + duplicates + ")"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getSpeedViolationLimit()) {
                    handleViolation(player);
                }
            }
        } else {
            // Position hat sich geändert, Counter zurücksetzen
            duplicateCount.remove(uuid);

            // Reduziere Verstöße bei korrektem Verhalten
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }

        lastPosition.put(uuid, currentPos);
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: Ungültige Packets (BadpacketB)!");
        violations.remove(player.getUniqueId());
        lastPosition.remove(player.getUniqueId());
        duplicateCount.remove(player.getUniqueId());
    }
}