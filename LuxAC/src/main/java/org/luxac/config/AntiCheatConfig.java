package org.luxac.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.luxac.LuxAC;

public class AntiCheatConfig {

    private final LuxAC plugin;
    private final FileConfiguration config;

    public AntiCheatConfig(LuxAC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public boolean isNoFallEnabled() {
        return config.getBoolean("checks.nofall.enabled", true);
    }

    public int getNoFallViolationLimit() {
        return config.getInt("checks.nofall.violation-limit", 5);
    }

    public boolean isFlyEnabled() {
        return config.getBoolean("checks.fly.enabled", true);
    }

    public int getFlyViolationLimit() {
        return config.getInt("checks.fly.violation-limit", 10);
    }

    public boolean isJesusEnabled() {
        return config.getBoolean("checks.jesus.enabled", true);
    }

    public int getJesusViolationLimit() {
        return config.getInt("checks.jesus.violation-limit", 5);
    }

    public boolean isSpeedEnabled() {
        return config.getBoolean("checks.speed.enabled", true);
    }

    public int getSpeedViolationLimit() {
        return config.getInt("checks.speed.violation-limit", 8);
    }

    public double getMaxSpeed() {
        return config.getDouble("checks.speed.max-speed", 0.7);
    }

    // BadPacket Checks
    public boolean isBadpacketAEnabled() {
        return config.getBoolean("checks.badpackets.badpacketA.enabled", true);
    }

    public int getBadpacketAViolationLimit() {
        return config.getInt("checks.badpackets.badpacketA.violation-limit", 5);
    }

    public boolean isBadpacketBEnabled() {
        return config.getBoolean("checks.badpackets.badpacketB.enabled", true);
    }

    public int getBadpacketBViolationLimit() {
        return config.getInt("checks.badpackets.badpacketB.violation-limit", 10);
    }

    public boolean isBadpacketCEnabled() {
        return config.getBoolean("checks.badpackets.badpacketC.enabled", true);
    }

    public int getBadpacketCViolationLimit() {
        return config.getInt("checks.badpackets.badpacketC.violation-limit", 5);
    }

    public boolean isBadpacketDEnabled() {
        return config.getBoolean("checks.badpackets.badpacketD.enabled", true);
    }

    public int getBadpacketDViolationLimit() {
        return config.getInt("checks.badpackets.badpacketD.violation-limit", 8);
    }

    public boolean isBadpacketEEnabled() {
        return config.getBoolean("checks.badpackets.badpacketE.enabled", true);
    }

    public int getBadpacketEViolationLimit() {
        return config.getInt("checks.badpackets.badpacketE.violation-limit", 5);
    }

    public boolean isBadpacketFEnabled() {
        return config.getBoolean("checks.badpackets.badpacketF.enabled", true);
    }

    public int getBadpacketFViolationLimit() {
        return config.getInt("checks.badpackets.badpacketF.violation-limit", 10);
    }

    // Combat Checks
    public boolean isAntiKockbackEnabled() {
        return config.getBoolean("checks.combat.antikockback.enabled", true);
    }

    public int getAntiKockbackViolationLimit() {
        return config.getInt("checks.combat.antikockback.violation-limit", 8);
    }

    public boolean isAutoTotemEnabled() {
        return config.getBoolean("checks.combat.autototem.enabled", true);
    }

    public int getAutoTotemViolationLimit() {
        return config.getInt("checks.combat.autototem.violation-limit", 5);
    }

    public boolean isKillAuraEnabled() {
        return config.getBoolean("checks.combat.killaura.enabled", true);
    }

    public int getKillAuraViolationLimit() {
        return config.getInt("checks.combat.killaura.violation-limit", 10);
    }

    public boolean isReachEnabled() {
        return config.getBoolean("checks.combat.reach.enabled", true);
    }

    public int getReachViolationLimit() {
        return config.getInt("checks.combat.reach.violation-limit", 5);
    }

    // Player Checks
    public boolean isFastEatEnabled() {
        return config.getBoolean("checks.player.fasteat.enabled", true);
    }

    public int getFastEatViolationLimit() {
        return config.getInt("checks.player.fasteat.violation-limit", 5);
    }

    // Place Checks
    public boolean isFastPlaceEnabled() {
        return config.getBoolean("checks.place.fastplace.enabled", true);
    }

    public int getFastPlaceViolationLimit() {
        return config.getInt("checks.place.fastplace.violation-limit", 8);
    }

    // Aim Checks
    public boolean isAutoAimEnabled() {
        return config.getBoolean("checks.aim.autoaim.enabled", true);
    }

    public int getAutoAimViolationLimit() {
        return config.getInt("checks.aim.autoaim.violation-limit", 5);
    }

    public boolean isBowAimBotEnabled() {
        return config.getBoolean("checks.aim.bowaimbot.enabled", true);
    }

    public int getBowAimBotViolationLimit() {
        return config.getInt("checks.aim.bowaimbot.violation-limit", 5);
    }
}