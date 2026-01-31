package org.luxac.checks.combat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.luxac.LuxAC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AutoTotem - Erkennt automatisches Totem-Switching
 * Prüft ob Spieler Totems zu schnell in die Offhand bewegen
 */
public class AutoTotem implements Listener {

    private final LuxAC plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Long> lastTotemUse = new HashMap<>();
    private final Map<UUID, Integer> quickTotemCount = new HashMap<>();

    // Minimale Zeit zwischen Totem-Uses (in Millisekunden)
    private static final long MIN_TOTEM_DELAY = 250;

    public AutoTotem(LuxAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!plugin.getAntiCheatConfig().isAutoTotemEnabled()) return;

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Prüfe ob Spieler Totem in Offhand hat
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.TOTEM_OF_UNDYING) {
            return;
        }

        Long lastUse = lastTotemUse.get(uuid);

        if (lastUse != null) {
            long timeDiff = currentTime - lastUse;

            // Totem wurde zu schnell nach dem letzten verwendet
            if (timeDiff < MIN_TOTEM_DELAY) {
                int quickCount = quickTotemCount.getOrDefault(uuid, 0) + 1;
                quickTotemCount.put(uuid, quickCount);

                // Nach mehreren schnellen Totem-Uses flaggen
                if (quickCount >= 2) {
                    int violationCount = violations.getOrDefault(uuid, 0) + 1;
                    violations.put(uuid, violationCount);

                    plugin.getLogger().warning(
                            player.getName() + " hat möglicherweise AutoTotem benutzt! " +
                                    "Verstoß #" + violationCount +
                                    " (Zeit: " + timeDiff + "ms, Schnelle Uses: " + quickCount + ")"
                    );

                    if (violationCount >= plugin.getAntiCheatConfig().getAutoTotemViolationLimit()) {
                        handleViolation(player);
                    }
                }
            } else {
                // Normale Geschwindigkeit, Counter zurücksetzen
                quickTotemCount.remove(uuid);

                // Reduziere Verstöße
                if (violations.containsKey(uuid)) {
                    int current = violations.get(uuid);
                    if (current > 0) {
                        violations.put(uuid, current - 1);
                    }
                }
            }
        }

        lastTotemUse.put(uuid, currentTime);

        // Prüfe nach 1 Tick ob neues Totem in Offhand ist
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkTotemRefill(player);
        }, 1L);
    }

    private void checkTotemRefill(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        // Wenn sofort ein neues Totem in der Offhand ist (innerhalb 1 Tick)
        if (offhand.getType() == Material.TOTEM_OF_UNDYING) {
            int violationCount = violations.getOrDefault(uuid, 0) + 1;
            violations.put(uuid, violationCount);

            plugin.getLogger().warning(
                    player.getName() + " hat Totem zu schnell nachgefüllt! " +
                            "Verstoß #" + violationCount
            );

            if (violationCount >= plugin.getAntiCheatConfig().getAutoTotemViolationLimit()) {
                handleViolation(player);
            }
        }
    }

    private void handleViolation(Player player) {
        player.kickPlayer("§cLuxAC: AutoTotem erkannt!");
        violations.remove(player.getUniqueId());
        lastTotemUse.remove(player.getUniqueId());
        quickTotemCount.remove(player.getUniqueId());
    }
}