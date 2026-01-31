package org.luxac.checks.combat;

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
 * AntiKockback - Erkennt reduzierten oder fehlenden Knockback
 * Prüft ob Spieler nach einem Hit den normalen Knockback erhalten
 */
public class AntiKockback implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Vector> lastVelocity = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();

    // Minimaler erwarteter Knockback
    private static final double MIN_KNOCKBACK = 0.1;

    public AntiKockback(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getAntiCheatConfig().isAntiKockbackEnabled()) return;

        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        UUID uuid = victim.getUniqueId();

        // Speichere aktuelle Velocity vor dem Hit
        Vector velocityBefore = victim.getVelocity().clone();
        lastVelocity.put(uuid, velocityBefore);
        lastHitTime.put(uuid, System.currentTimeMillis());

        // Prüfe Velocity nach 2 Ticks (100ms)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkKnockback(victim, velocityBefore);
        }, 2L);
    }

    private void checkKnockback(Player player, Vector velocityBefore) {
        UUID uuid = player.getUniqueId();
        Long hitTime = lastHitTime.get(uuid);

        if (hitTime == null) return;

        // Prüfe ob der Hit kürzlich war
        if (System.currentTimeMillis() - hitTime > 200) return;

        Vector velocityAfter = player.getVelocity();

        // Berechne Geschwindigkeitsänderung
        double velocityChange = velocityAfter.length() - velocityBefore.length();

        // Wenn Velocity-Änderung zu gering ist -> Kein Knockback
        if (velocityChange < MIN_KNOCKBACK) {
            int violationCount = violations.getOrDefault(uuid, 0) + 1;
            violations.put(uuid, violationCount);

            plugin.getLogger().warning(
                    player.getName() + " hat möglicherweise AntiKnockback benutzt! " +
                            "Verstoß #" + violationCount +
                            " (Velocity-Änderung: " + String.format("%.3f", velocityChange) + ")"
            );

            if (violationCount >= plugin.getAntiCheatConfig().getAntiKockbackViolationLimit()) {
                handleViolation(player);
            }
        } else {
            // Reduziere Verstöße bei normalem Knockback
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: AntiKnockback erkannt!");
        violations.remove(player.getUniqueId());
        lastVelocity.remove(player.getUniqueId());
        lastHitTime.remove(player.getUniqueId());
    }
}