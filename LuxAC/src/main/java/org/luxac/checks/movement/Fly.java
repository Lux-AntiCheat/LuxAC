package org.luxac.checks.movement;

import org.luxac.LuxAC;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Fly implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Integer> airTicks = new HashMap<>();
    private final Map<UUID, Location> lastLocation = new HashMap<>();

    public Fly(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isFlyEnabled()) return;

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
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        // Prüfe ob sich Position vertikal oder horizontal geändert hat
        if (from.getX() == to.getX() &&
                from.getY() == to.getY() &&
                from.getZ() == to.getZ()) {
            return;
        }

        // Zähle Ticks in der Luft
        if (!player.isOnGround() && !isOnLadder(player) && !isInWater(player)) {
            int ticks = airTicks.getOrDefault(uuid, 0) + 1;
            airTicks.put(uuid, ticks);

            // Prüfe vertikale Bewegung
            double yDiff = to.getY() - from.getY();

            // Wenn Spieler sich nach oben bewegt oder auf gleicher Höhe bleibt in der Luft
            if (ticks > 20 && yDiff >= -0.1) {
                Location last = lastLocation.get(uuid);

                if (last != null) {
                    double lastYDiff = to.getY() - last.getY();

                    // Spieler schwebt oder fliegt nach oben
                    if (lastYDiff >= -0.2) {
                        int violationCount = violations.getOrDefault(uuid, 0) + 1;
                        violations.put(uuid, violationCount);

                        plugin.getLogger().warning(
                                player.getName() + " hat möglicherweise Fly benutzt! " +
                                        "Verstoß #" + violationCount + " (Y-Diff: " + yDiff + ")"
                        );

                        if (violationCount >= plugin.getAntiCheatConfig().getFlyViolationLimit()) {
                            handleViolation(player);
                        }

                        // Spieler zurücksetzen
                        event.setCancelled(true);
                        player.teleport(from);
                    }
                }
            }
        } else {
            // Spieler ist auf dem Boden, Counter zurücksetzen
            airTicks.remove(uuid);
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }

        lastLocation.put(uuid, to.clone());
    }

    private boolean isOnLadder(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        return blockType == Material.LADDER ||
                blockType == Material.VINE ||
                blockType == Material.SCAFFOLDING;
    }

    private boolean isInWater(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        return blockType == Material.WATER ||
                blockType == Material.BUBBLE_COLUMN;
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cAntiCheat: Fly erkannt!");
        violations.remove(player.getUniqueId());
        airTicks.remove(player.getUniqueId());
        lastLocation.remove(player.getUniqueId());
    }
}