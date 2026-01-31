package org.luxac.checks.aim;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;
import org.luxac.LuxAC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BowAimBot - Erkennt automatisches Zielen mit Bögen
 * Prüft auf perfekte Bögen-Treffer und unnatürliche Präzision
 */
public class BowAimBot implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Integer> totalShots = new HashMap<>();
    private final Map<UUID, Integer> perfectShots = new HashMap<>();
    private final Map<UUID, Vector> lastShotDirection = new HashMap<>();

    // Maximale erlaubte perfekte Schuss-Quote (in Prozent)
    private static final double MAX_PERFECT_SHOT_RATIO = 70.0;
    // Minimaler Winkel-Unterschied zwischen aufeinanderfolgenden Schüssen
    private static final double MIN_AIM_CHANGE = 2.0;

    public BowAimBot(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!plugin.getAntiCheatConfig().isBowAimBotEnabled()) return;

        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        Player shooter = (Player) event.getEntity();
        UUID uuid = shooter.getUniqueId();
        Arrow arrow = (Arrow) event.getProjectile();

        Vector shotDirection = arrow.getVelocity().normalize();
        Vector lastDirection = lastShotDirection.get(uuid);

        if (lastDirection != null) {
            // Berechne Winkeländerung zwischen Schüssen
            double angle = Math.toDegrees(Math.acos(shotDirection.dot(lastDirection)));

            // Zu wenig Änderung deutet auf Aimbot hin
            if (angle < MIN_AIM_CHANGE) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        shooter.getName() + " hat verdächtig konstante Zielrichtung! " +
                                "Verstoß #" + violationCount +
                                " (Winkeländerung: " + String.format("%.2f", angle) + "°)"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getBowAimBotViolationLimit()) {
                    handleViolation(shooter);
                }
            }
        }

        lastShotDirection.put(uuid, shotDirection);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getAntiCheatConfig().isBowAimBotEnabled()) return;

        if (!(event.getDamager() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getDamager();

        if (!(arrow.getShooter() instanceof Player)) return;

        Player shooter = (Player) arrow.getShooter();
        UUID uuid = shooter.getUniqueId();

        // Zähle Schüsse
        int shots = totalShots.getOrDefault(uuid, 0) + 1;
        totalShots.put(uuid, shots);

        // Prüfe ob Treffer "perfekt" war (direkter Kopftreffer)
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            double hitHeight = arrow.getLocation().getY();
            double headHeight = victim.getEyeLocation().getY();

            // Perfekter Kopftreffer
            if (Math.abs(hitHeight - headHeight) < 0.3) {
                int perfect = perfectShots.getOrDefault(uuid, 0) + 1;
                perfectShots.put(uuid, perfect);
            }
        }

        // Prüfe Treffer-Quote nach mindestens 5 Schüssen
        if (shots >= 5) {
            int perfect = perfectShots.getOrDefault(uuid, 0);
            double ratio = (perfect * 100.0) / shots;

            if (ratio > MAX_PERFECT_SHOT_RATIO) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        shooter.getName() + " hat verdächtige Bogen-Präzision! " +
                                "Verstoß #" + violationCount +
                                " (Kopftreffer: " + String.format("%.1f", ratio) + "%)"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getBowAimBotViolationLimit()) {
                    handleViolation(shooter);
                }

                // Counter zurücksetzen
                totalShots.put(uuid, 0);
                perfectShots.put(uuid, 0);
            }
        }
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: BowAimBot erkannt!");
        violations.remove(player.getUniqueId());
        totalShots.remove(player.getUniqueId());
        perfectShots.remove(player.getUniqueId());
        lastShotDirection.remove(player.getUniqueId());
    }
}