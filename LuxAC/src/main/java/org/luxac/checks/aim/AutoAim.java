package org.luxac.checks.aim;

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
 * AutoAim - Erkennt automatisches Zielen (Aimbot)
 * Prüft auf perfekte Kopftreffer-Quote und unnatürliche Präzision
 */
public class AutoAim implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Integer> totalHits = new HashMap<>();
    private final Map<UUID, Integer> perfectHits = new HashMap<>();

    // Maximale erlaubte perfekte Treffer-Quote (in Prozent)
    private static final double MAX_PERFECT_HIT_RATIO = 80.0;

    public AutoAim(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getAntiCheatConfig().isAutoAimEnabled()) return;

        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Entity victim = event.getEntity();
        UUID uuid = attacker.getUniqueId();

        // Zähle totale Treffer
        int hits = totalHits.getOrDefault(uuid, 0) + 1;
        totalHits.put(uuid, hits);

        // Prüfe ob Treffer "perfekt" war (genau auf Kopfhöhe)
        double attackerEyeHeight = attacker.getEyeLocation().getY();
        double victimEyeHeight = victim.getLocation().getY() + ((Player) victim).getEyeHeight();
        double heightDiff = Math.abs(attackerEyeHeight - victimEyeHeight);

        // Perfekter Treffer wenn Höhenunterschied minimal ist
        if (heightDiff < 0.1) {
            int perfect = perfectHits.getOrDefault(uuid, 0) + 1;
            perfectHits.put(uuid, perfect);
        }

        // Prüfe Treffer-Quote nach mindestens 10 Treffern
        if (hits >= 10) {
            int perfect = perfectHits.getOrDefault(uuid, 0);
            double ratio = (perfect * 100.0) / hits;

            if (ratio > MAX_PERFECT_HIT_RATIO) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        attacker.getName() + " hat verdächtige Treffer-Präzision! " +
                                "Verstoß #" + violationCount +
                                " (Perfekte Treffer: " + String.format("%.1f", ratio) + "%)"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getAutoAimViolationLimit()) {
                    handleViolation(attacker);
                }

                // Counter zurücksetzen für neue Analyse
                totalHits.put(uuid, 0);
                perfectHits.put(uuid, 0);
            }
        }
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: AutoAim erkannt!");
        violations.remove(player.getUniqueId());
        totalHits.remove(player.getUniqueId());
        perfectHits.remove(player.getUniqueId());
    }
}