package fr.tt54.protectYourCastle;

import fr.skytasul.glowingentities.GlowingEntities;
import fr.tt54.protectYourCastle.cmd.CmdCastle;
import fr.tt54.protectYourCastle.cmd.CmdDrawbridge;
import fr.tt54.protectYourCastle.game.ResourceGenerator;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.protectYourCastle.game.Trader;
import fr.tt54.protectYourCastle.listeners.BannerListener;
import fr.tt54.protectYourCastle.listeners.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public final class ProtectYourCastleMain extends JavaPlugin {

    private static ProtectYourCastleMain instance;
    public GlowingEntities glowingEntities;

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

        glowingEntities = new GlowingEntities(this);

        enableHealthInfo();
    }

    @Override
    public void onDisable() {
        ResourceGenerator.save();
        Team.save();
        Trader.save();

        glowingEntities.disable();
    }

    public static ProtectYourCastleMain getInstance() {
        return instance;
    }

    private void enableHealthInfo(){
        Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        if(scoreboard.getObjective("health") == null) {
            Objective objective = Bukkit.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("health", Criteria.HEALTH, "§c❤");
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            objective.setRenderType(RenderType.INTEGER);
        }
    }
}
