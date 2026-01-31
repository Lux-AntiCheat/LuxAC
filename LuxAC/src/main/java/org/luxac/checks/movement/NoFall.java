package org.luxac.checks.movement;

import org.luxac.LuxAC;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoFall implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Double> lastFallDistance = new HashMap<>();

    public NoFall(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isNoFallEnabled()) return;

        Player player = event.getPlayer();

        // Spieler im Creative/Spectator ignorieren
        if (player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Spieler mit Fly-Permission ignorieren
        if (player.getAllowFlight()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        double fallDistance = player.getFallDistance();

        // Speichere die letzte Fall-Distanz
        if (fallDistance > 3.0) {
            lastFallDistance.put(uuid, fallDistance);
        }

        // Prüfe ob Spieler auf dem Boden ist
        if (player.isOnGround() && fallDistance == 0) {
            Double lastDistance = lastFallDistance.get(uuid);

            if (lastDistance != null && lastDistance > 3.0) {
                // Spieler sollte Fall-Schaden bekommen haben
                // Wird im EntityDamageEvent überprüft
                lastFallDistance.remove(uuid);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!plugin.getAntiCheatConfig().isNoFallEnabled()) return;

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Fallschaden wurde korrekt registriert
            lastFallDistance.remove(uuid);
            violations.remove(uuid);
        } else {
            // Prüfe ob Spieler fallen sollte aber keinen Schaden bekommt
            Double lastDistance = lastFallDistance.get(uuid);

            if (lastDistance != null && lastDistance > 3.0 && player.isOnGround()) {
                int violationCount = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, violationCount);

                plugin.getLogger().warning(
                        player.getName() + " hat möglicherweise NoFall benutzt! " +
                                "Verstoß #" + violationCount
                );

                if (violationCount >= plugin.getAntiCheatConfig().getNoFallViolationLimit()) {
                    handleViolation(player);
                }

                lastFallDistance.remove(uuid);
            }
        }
    }

    private void handleViolation(Player player) {
        // Spieler kicken oder bannen
        player.kickPlayer("§cAntiCheat: NoFall erkannt!");
        violations.remove(player.getUniqueId());
    }
}