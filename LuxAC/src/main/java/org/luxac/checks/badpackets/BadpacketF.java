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
 * BadpacketF - Erkennt Ground-Spoofing
 * Prüft ob der Spieler falschen Ground-Status (onGround) sendet
 * Dies wird oft von Cheats verwendet um NoFall, Fly oder andere Checks zu umgehen
 */
public class BadpacketF implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Boolean> lastGroundState = new HashMap<>();
    private final Map<UUID, Integer> airTicks = new HashMap<>();

    public BadpacketF(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isSpeedEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Ignoriere Spieler mit Fly-Permission
        if (player.isFlying() || player.getAllowFlight()) {
            return;
        }

        boolean clientGround = player.isOnGround();
        boolean serverGround = isActuallyOnGround(player);

        // Prüfe ob Client und Server Ground-Status übereinstimmen
        if (clientGround != serverGround) {
            // Client sendet falschen Ground-Status

            // Zähle aufeinanderfolgende falsche Ground-States
            int ticks = airTicks.getOrDefault(uuid, 0) + 1;
            airTicks.put(uuid, ticks);

            // Nach mehreren Ticks mit falschem Ground-Status flaggen
            if (ticks > 3) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                String status = clientGround ? "onGround=true" : "onGround=false";
                String actual = serverGround ? "tatsächlich auf Boden" : "tatsächlich in Luft";

                plugin.getLogger().warning(
                        player.getName() + " sendet falschen Ground-Status! " +
                                "Verstoß #" + violationCount +
                                " (Client: " + status + ", Server: " + actual + ")"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getSpeedViolationLimit()) {
                    handleViolation(player);
                }

                // Bei extremem Ground-Spoofing Bewegung abbrechen
                if (ticks > 10) {
                    event.setCancelled(true);
                }
            }
        } else {
            // Ground-Status stimmt überein, Counter zurücksetzen
            airTicks.remove(uuid);

            // Reduziere Verstöße bei korrektem Verhalten
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }

        lastGroundState.put(uuid, clientGround);
    }

    /**
     * Prüft ob der Spieler tatsächlich auf dem Boden ist
     * durch Überprüfung der Blöcke unter dem Spieler
     */
    private boolean isActuallyOnGround(Player player) {
        // Prüfe Block direkt unter den Füßen des Spielers
        double y = player.getLocation().getY();

        // Wenn Y-Koordinate auf einer Block-Grenze ist, ist Spieler auf dem Boden
        if (Math.abs(y - Math.floor(y)) < 0.001) {
            return player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();
        }

        return false;
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: Ungültige Packets (BadpacketF)!");
        violations.remove(player.getUniqueId());
        lastGroundState.remove(player.getUniqueId());
        airTicks.remove(player.getUniqueId());
    }
}