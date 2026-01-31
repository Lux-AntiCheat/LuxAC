package org.luxac.checks.combat;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.luxac.LuxAC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reach - Erkennt erhöhte Reichweite bei Angriffen
 * Prüft ob Spieler Entities aus zu großer Entfernung treffen
 */
public class Reach implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();

    // Maximale erlaubte Reichweite (in Blöcken)
    private static final double MAX_REACH_SURVIVAL = 3.1;
    private static final double MAX_REACH_CREATIVE = 6.1;

    public Reach(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getAntiCheatConfig().isReachEnabled()) return;

        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Entity victim = event.getEntity();
        UUID uuid = attacker.getUniqueId();

        // Berechne Distanz zwischen Spieler und Ziel
        double distance = attacker.getLocation().distance(victim.getLocation());

        // Bestimme maximale erlaubte Reichweite basierend auf GameMode
        double maxReach = attacker.getGameMode() == GameMode.CREATIVE
                ? MAX_REACH_CREATIVE
                : MAX_REACH_SURVIVAL;

        // Prüfe ob Reichweite überschritten wurde
        if (distance > maxReach) {
            int violationCount = violations.getOrDefault(uuid, 0) + 1;
            violations.put(uuid, violationCount);

            plugin.getLogger().warning(
                    attacker.getName() + " hat mit erhöhter Reichweite getroffen! " +
                            "Verstoß #" + violationCount +
                            " (Distanz: " + String.format("%.2f", distance) +
                            " > Max: " + String.format("%.2f", maxReach) + ")"
            );

            if (violationCount >= plugin.getAntiCheatConfig().getReachViolationLimit()) {
                handleViolation(attacker);
            }

            // Hit abbrechen
            event.setCancelled(true);
        } else {
            // Reduziere Verstöße bei legitimer Reichweite
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: Reach (erhöhte Reichweite) erkannt!");
        violations.remove(player.getUniqueId());
    }
}