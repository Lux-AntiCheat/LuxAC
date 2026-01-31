package org.luxac.checks.movement;

import org.luxac.LuxAC;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Speed implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();

    public Speed(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isSpeedEnabled()) return;

        Player player = event.getPlayer();

        // Spieler im Creative/Spectator ignorieren
        if (player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Fliegende Spieler ignorieren
        if (player.isFlying() || player.getAllowFlight()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Berechne horizontale Distanz
        double distance = Math.sqrt(
                Math.pow(to.getX() - from.getX(), 2) +
                        Math.pow(to.getZ() - from.getZ(), 2)
        );

        // Nur horizontale Bewegungen prüfen
        if (distance > 0) {
            Location last = lastLocation.get(uuid);
            Long lastTime = lastMoveTime.get(uuid);

            if (last != null && lastTime != null) {
                long timeDiff = currentTime - lastTime;

                // Mindestens 50ms zwischen Checks (1 Tick)
                if (timeDiff >= 50) {
                    // Berechne Geschwindigkeit in Blöcken pro Sekunde
                    double speed = (distance / timeDiff) * 1000;

                    // Maximale erlaubte Geschwindigkeit
                    double maxSpeed = getMaxSpeed(player);

                    if (speed > maxSpeed) {
                        int violationCount = violations.getOrDefault(uuid, 0) + 1;
                        violations.put(uuid, violationCount);

                        plugin.getLogger().warning(
                                player.getName() + " hat möglicherweise Speed benutzt! " +
                                        "Verstoß #" + violationCount +
                                        " (Speed: " + String.format("%.2f", speed) +
                                        " > Max: " + String.format("%.2f", maxSpeed) + ")"
                        );

                        if (violationCount >= plugin.getAntiCheatConfig().getSpeedViolationLimit()) {
                            handleViolation(player);
                        }

                        // Spieler zurücksetzen
                        event.setCancelled(true);
                        player.teleport(from);
                    } else {
                        // Reduziere Verstöße wenn Spieler normal läuft
                        if (violations.containsKey(uuid)) {
                            int current = violations.get(uuid);
                            if (current > 0) {
                                violations.put(uuid, current - 1);
                            }
                        }
                    }
                }
            }

            lastLocation.put(uuid, to.clone());
            lastMoveTime.put(uuid, currentTime);
        }
    }

    private double getMaxSpeed(Player player) {
        // Basis-Geschwindigkeit aus Config
        double maxSpeed = plugin.getAntiCheatConfig().getMaxSpeed();

        // Prüfe auf Speed-Effekte
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            // Jedes Level erhöht die Geschwindigkeit um ca. 20%
            maxSpeed += maxSpeed * 0.2 * (amplifier + 1);
        }

        // Sprinten berücksichtigen
        if (player.isSprinting()) {
            maxSpeed *= 1.3;
        }

        // Auf Eis/Blöcken
        if (isOnIce(player)) {
            maxSpeed *= 1.5;
        }

        // Flug-Boost (Elytra wird separat gehandhabt)
        if (player.isGliding()) {
            maxSpeed *= 3.0;
        }

        return maxSpeed;
    }

    private boolean isOnIce(Player player) {
        Location loc = player.getLocation().subtract(0, 1, 0);
        String blockType = loc.getBlock().getType().name();
        return blockType.contains("ICE");
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cAntiCheat: Speed erkannt!");
        violations.remove(player.getUniqueId());
        lastLocation.remove(player.getUniqueId());
        lastMoveTime.remove(player.getUniqueId());
    }
}