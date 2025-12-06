package fr.tt54.protectYourCastle;

import fr.tt54.protectYourCastle.cmd.CmdCastle;
import fr.tt54.protectYourCastle.cmd.CmdDrawbridge;
import fr.tt54.protectYourCastle.game.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.listeners.BannerListener;
import fr.tt54.protectYourCastle.listeners.GameListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProtectYourCastleMain extends JavaPlugin {

    private static ProtectYourCastleMain instance;

    @Override
    public void onEnable() {
        instance = this;

        ResourceGenerator.load();
        Team.load();
        Trader.load();

        this.getCommand("drawbridge").setExecutor(new CmdDrawbridge());
        this.getCommand("castle").setExecutor(new CmdCastle());
        this.getCommand("castle").setTabCompleter(new CmdCastle());

        this.getServer().getPluginManager().registerEvents(new GameListener(), this);
        this.getServer().getPluginManager().registerEvents(new BannerListener(), this);
    }

    @Override
    public void onDisable() {
        ResourceGenerator.save();
        Team.save();
        Trader.save();
    }

    public static ProtectYourCastleMain getInstance() {
        return instance;
    }
}
