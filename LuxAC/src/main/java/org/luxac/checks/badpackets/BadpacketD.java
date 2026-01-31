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
 * BadpacketD - Erkennt Position-Look Packet ohne Bewegung
 * Prüft ob der Spieler Position Packets sendet obwohl er sich nicht bewegt hat
 * Dies deutet auf modifizierte Clients oder Packet-Spamming hin
 */
public class BadpacketD implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Long> lastPacketTime = new HashMap<>();
    private final Map<UUID, Integer> packetCount = new HashMap<>();

    // Maximale Anzahl von Packets pro Sekunde ohne Bewegung
    private static final int MAX_PACKETS_PER_SECOND = 30;

    public BadpacketD(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isSpeedEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Prüfe ob sich Position geändert hat
        boolean positionChanged =
                event.getFrom().getX() != event.getTo().getX() ||
                        event.getFrom().getY() != event.getTo().getY() ||
                        event.getFrom().getZ() != event.getTo().getZ();

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastPacketTime.get(uuid);

        if (!positionChanged) {
            // Keine Bewegung, aber Packet wurde gesendet

            if (lastTime != null) {
                long timeDiff = currentTime - lastTime;

                // Zähle Packets innerhalb von 1 Sekunde
                if (timeDiff < 1000) {
                    int count = packetCount.getOrDefault(uuid, 0) + 1;
                    packetCount.put(uuid, count);

                    // Zu viele Packets ohne Bewegung
                    if (count > MAX_PACKETS_PER_SECOND) {
                        int violationCount = violations.getOrDefault(uuid, 0) + 1;
                        violations.put(uuid, violationCount);

                        plugin.getLogger().warning(
                                player.getName() + " sendet zu viele Packets ohne Bewegung! " +
                                        "Verstoß #" + violationCount +
                                        " (Packets: " + count + "/s)"
                        );

                        if (violationCount >= plugin.getAntiCheatConfig().getSpeedViolationLimit()) {
                            handleViolation(player);
                        }

                        // Event abbrechen um Spam zu stoppen
                        event.setCancelled(true);
                    }
                } else {
                    // Neue Sekunde beginnt, Counter zurücksetzen
                    packetCount.put(uuid, 1);
                }
            }
        } else {
            // Position hat sich geändert, legitime Bewegung
            packetCount.remove(uuid);

            // Reduziere Verstöße
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }

        lastPacketTime.put(uuid, currentTime);
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: Ungültige Packets (BadpacketD)!");
        violations.remove(player.getUniqueId());
        lastPacketTime.remove(player.getUniqueId());
        packetCount.remove(player.getUniqueId());
    }
}