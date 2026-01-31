package org.luxac;

import org.bukkit.plugin.java.JavaPlugin;
import org.luxac.checks.aim.AutoAim;
import org.luxac.checks.aim.BowAimBot;
import org.luxac.checks.badpackets.*;
import org.luxac.checks.combat.AntiKockback;
import org.luxac.checks.combat.AutoTotem;
import org.luxac.checks.combat.KillAura;
import org.luxac.checks.combat.Reach;
import org.luxac.checks.movement.*;
import org.luxac.checks.place.FastPlace;
import org.luxac.checks.player.FastEat;
import org.luxac.config.AntiCheatConfig;

public class LuxAC extends JavaPlugin {

    private AntiCheatConfig config;

    @Override
    public void onEnable() {
        // Config laden
        saveDefaultConfig();
        config = new AntiCheatConfig(this);

        // Movement Checks registrieren
        getServer().getPluginManager().registerEvents(new NoFall(this), this);
        getServer().getPluginManager().registerEvents(new Fly(this), this);
        getServer().getPluginManager().registerEvents(new Jesus(this), this);
        getServer().getPluginManager().registerEvents(new Speed(this), this);

        // BadPacket Checks registrieren
        getServer().getPluginManager().registerEvents(new BadpacketA(this), this);
        getServer().getPluginManager().registerEvents(new BadpacketB(this), this);
        getServer().getPluginManager().registerEvents(new BadpacketC(this), this);
        getServer().getPluginManager().registerEvents(new BadpacketD(this), this);
        getServer().getPluginManager().registerEvents(new BadpacketE(this), this);
        getServer().getPluginManager().registerEvents(new BadpacketF(this), this);

        // Combat Checks registrieren
        getServer().getPluginManager().registerEvents(new AntiKockback(this), this);
        getServer().getPluginManager().registerEvents(new AutoTotem(this), this);
        getServer().getPluginManager().registerEvents(new KillAura(this), this);
        getServer().getPluginManager().registerEvents(new Reach(this), this);

        // Player Checks registrieren
        getServer().getPluginManager().registerEvents(new FastEat(this), this);

        // Aim Checks registrieren
        getServer().getPluginManager().registerEvents(new AutoAim(this), this);
        getServer().getPluginManager().registerEvents(new BowAimBot(this), this);

        getLogger().info("LuxAC wurde aktiviert!");
        getLogger().info("Geladene Checks: Movement(4), BadPackets(6), Combat(4), Player(1), Place(1), Aim(2)");
    }

    @Override
    public void onDisable() {
        getLogger().info("LuxAC wurde deaktiviert!");
    }

    public AntiCheatConfig getAntiCheatConfig() {
        return config;
    }
}