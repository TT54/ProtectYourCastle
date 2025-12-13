package fr.tt54.protectYourCastle;

import fr.tt54.protectYourCastle.cmd.CmdCastle;
import fr.tt54.protectYourCastle.cmd.CmdDrawbridge;
import fr.tt54.protectYourCastle.cmd.CmdStats;
import fr.tt54.protectYourCastle.game.*;
import fr.tt54.protectYourCastle.listeners.BannerListener;
import fr.tt54.protectYourCastle.listeners.GameListener;
import fr.tt54.protectYourCastle.mod_bridges.VoiceChatBridge;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProtectYourCastleMain extends JavaPlugin {

    private static ProtectYourCastleMain instance;
    public static VoiceChatBridge voiceChatBridge = new VoiceChatBridge();

    @Override
    public void onEnable() {
        instance = this;

        GameParameters.load();
        ResourceGenerator.load();
        Team.load();
        Trader.load();
        GameStatistics.load();

        this.getCommand("drawbridge").setExecutor(new CmdDrawbridge());
        this.getCommand("stats").setExecutor(new CmdStats());
        this.getCommand("castle").setExecutor(new CmdCastle());
        this.getCommand("castle").setTabCompleter(new CmdCastle());

        this.getServer().getPluginManager().registerEvents(new GameListener(), this);
        this.getServer().getPluginManager().registerEvents(new BannerListener(), this);

        try {
            voiceChatBridge.enable();
        } catch (Exception | Error e){
            System.err.println("Impossible d'activer la liaison avec voicechat");
        }
    }

    @Override
    public void onDisable() {
        ResourceGenerator.save();
        Team.save();
        Trader.save();
        GameStatistics.save();
        GameParameters.save();
    }

    public static ProtectYourCastleMain getInstance() {
        return instance;
    }
}
