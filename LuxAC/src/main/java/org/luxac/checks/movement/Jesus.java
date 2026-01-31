package org.luxac.checks.movement;

import org.luxac.LuxAC;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Jesus implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Integer> waterWalkTicks = new HashMap<>();

    public Jesus(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getAntiCheatConfig().isJesusEnabled()) return;

        Player player = event.getPlayer();

        // Spieler im Creative/Spectator ignorieren
        if (player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Location loc = player.getLocation();
        UUID uuid = player.getUniqueId();

        // Prüfe ob Spieler über Wasser ist
        Block blockBelow = loc.clone().subtract(0, 0.5, 0).getBlock();
        Block blockAt = loc.getBlock();

        // Spieler steht/läuft auf Wasser
        if (isWater(blockBelow) && !isWater(blockAt)) {
            // Prüfe ob Spieler wirklich auf dem Wasser läuft (nicht schwimmt)
            if (!player.isSwimming() && !player.isInWater()) {
                int ticks = waterWalkTicks.getOrDefault(uuid, 0) + 1;
                waterWalkTicks.put(uuid, ticks);

                // Nach mehreren Ticks auf Wasser -> Verstoß
                if (ticks > 5) {
                    // Prüfe ob Spieler spezielle Blöcke hat (Lily Pad, etc.)
                    if (!hasValidBlockAboveWater(player)) {
                        int violationCount = violations.getOrDefault(uuid, 0) + 1;
                        violations.put(uuid, violationCount);

                        plugin.getLogger().warning(
                                player.getName() + " hat möglicherweise Jesus benutzt! " +
                                        "Verstoß #" + violationCount
                        );

                        if (violationCount >= plugin.getAntiCheatConfig().getJesusViolationLimit()) {
                            handleViolation(player);
                        }

                        // Spieler ins Wasser fallen lassen
                        Location teleportLoc = loc.clone().subtract(0, 1, 0);
                        player.teleport(teleportLoc);
                    }
                }
            } else {
                waterWalkTicks.remove(uuid);
            }
        } else {
            // Spieler ist nicht über Wasser
            waterWalkTicks.remove(uuid);

            // Reduziere Verstöße wenn Spieler normal läuft
            if (violations.containsKey(uuid)) {
                int current = violations.get(uuid);
                if (current > 0) {
                    violations.put(uuid, current - 1);
                }
            }
        }
    }

    private boolean isWater(Block block) {
        Material type = block.getType();
        return type == Material.WATER ||
                type == Material.BUBBLE_COLUMN;
    }

    private boolean hasValidBlockAboveWater(Player player) {
        Location loc = player.getLocation();

        // Prüfe alle Blöcke in der Nähe der Füße des Spielers
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block block = loc.clone().add(x, -0.1, z).getBlock();
                Material type = block.getType();

                // Lily Pad oder andere valide Blöcke
                if (type == Material.LILY_PAD ||
                        type.isSolid() && type != Material.WATER) {
                    return true;
                }
            }
        }

        return false;
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cAntiCheat: Jesus (Water Walk) erkannt!");
        violations.remove(player.getUniqueId());
        waterWalkTicks.remove(player.getUniqueId());
    }
}