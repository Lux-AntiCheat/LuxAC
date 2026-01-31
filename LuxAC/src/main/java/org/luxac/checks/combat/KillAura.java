package org.luxac.checks.combat;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.luxac.LuxAC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * KillAura - Erkennt Multi-Target-Angriffe und unmögliche Hit-Winkel
 * Prüft ob Spieler mehrere Entities gleichzeitig angreifen oder
 * Entities außerhalb ihres Sichtfelds treffen
 */
public class KillAura implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, Entity> lastHitEntity = new HashMap<>();

    // Maximaler Winkel für legitime Hits (in Grad)
    private static final double MAX_HIT_ANGLE = 90.0;

    public KillAura(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getAntiCheatConfig().isKillAuraEnabled()) return;

        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Entity victim = event.getEntity();
        UUID uuid = attacker.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Prüfe Blickwinkel zum Ziel
        double angle = getAngleToEntity(attacker, victim);

        if (angle > MAX_HIT_ANGLE) {
            int violationCount = violations.getOrDefault(uuid, 0) + 1;
            violations.put(uuid, violationCount);

            plugin.getLogger().warning(
                    attacker.getName() + " hat außerhalb des Sichtfelds getroffen! " +
                            "Verstoß #" + violationCount +
                            " (Winkel: " + String.format("%.1f", angle) + "°)"
            );

            if (violationCount >= plugin.getAntiCheatConfig().getKillAuraViolationLimit()) {
                handleViolation(attacker);
            }

            // Hit abbrechen
            event.setCancelled(true);
            return;
        }

        // Prüfe Multi-Target (schnelles Wechseln zwischen verschiedenen Entities)
        Entity lastVictim = lastHitEntity.get(uuid);
        Long lastHit = lastHitTime.get(uuid);

        if (lastVictim != null && lastHit != null && !lastVictim.equals(victim)) {
            long timeDiff = currentTime - lastHit;

            // Zu schnelles Wechseln zwischen Targets (unter 150ms)
            if (timeDiff < 150) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        attacker.getName() + " wechselt zu schnell zwischen Zielen! " +
                                "Verstoß #" + violationCount +
                                " (Zeit: " + timeDiff + "ms)"
                );

                if (violationCount >= plugin.getAntiCheatConfig().getKillAuraViolationLimit()) {
                    handleViolation(attacker);
                }

                event.setCancelled(true);
                return;
            }
        }

        // Reduziere Verstöße bei legitimen Hits
        if (violations.containsKey(uuid)) {
            int current = violations.get(uuid);
            if (current > 0) {
                violations.put(uuid, current - 1);
            }
        }

        lastHitTime.put(uuid, currentTime);
        lastHitEntity.put(uuid, victim);
    }

    /**
     * Berechnet den Winkel zwischen Spieler-Blickrichtung und Entity
     */
    private double getAngleToEntity(Player player, Entity entity) {
        // Vektor von Spieler zu Entity
        Vector toEntity = entity.getLocation().toVector()
                .subtract(player.getEyeLocation().toVector())
                .normalize();

        // Blickrichtung des Spielers
        Vector direction = player.getEyeLocation().getDirection().normalize();

        // Berechne Winkel mit Dot Product
        double dot = direction.dot(toEntity);
        double angle = Math.toDegrees(Math.acos(dot));

        return angle;
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: KillAura erkannt!");
        violations.remove(player.getUniqueId());
        lastHitTime.remove(player.getUniqueId());
        lastHitEntity.remove(player.getUniqueId());
    }
}