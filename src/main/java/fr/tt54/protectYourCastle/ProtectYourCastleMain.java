package fr.tt54.protectYourCastle;

import fr.tt54.protectYourCastle.cmd.CmdCastle;
import fr.tt54.protectYourCastle.cmd.CmdDrawbridge;
import fr.tt54.protectYourCastle.cmd.CmdStats;
import fr.tt54.protectYourCastle.game.*;
import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.protectYourCastle.inventories.trades.TradeTimingChatInputListener;
import fr.tt54.protectYourCastle.listeners.BannerListener;
import fr.tt54.protectYourCastle.listeners.GameListener;
import fr.tt54.protectYourCastle.listeners.ModEventListener;
import fr.tt54.protectYourCastle.mod_bridges.VoiceChatBridge;
import fr.tt54.pycmod.events.PYCBukkitEventRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public final class ProtectYourCastleMain extends JavaPlugin {

    private static ProtectYourCastleMain instance;
    public static VoiceChatBridge voiceChatBridge = new VoiceChatBridge();

    @Override
    public void onEnable() {
        instance = this;

        this.loadCommon();
        this.loadGame();

        this.getCommand("drawbridge").setExecutor(new CmdDrawbridge());
        this.getCommand("stats").setExecutor(new CmdStats());
        this.getCommand("castle").setExecutor(new CmdCastle());
        this.getCommand("castle").setTabCompleter(new CmdCastle());

        this.getServer().getPluginManager().registerEvents(new GameListener(), this);
        this.getServer().getPluginManager().registerEvents(new BannerListener(), this);
        this.getServer().getPluginManager().registerEvents(new TradeTimingChatInputListener(), this);

        Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        if(scoreboard.getObjective("health") == null){
            Objective objective = scoreboard.registerNewObjective("health", Criteria.HEALTH, "§c❤");
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            objective.setRenderType(RenderType.INTEGER);
        }

        try {
            voiceChatBridge.enable();
        } catch (NoClassDefFoundError e) {
            getLogger().warning("Impossible d'activer la liaison avec voicechat: dépendance manquante.");
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.WARNING, "Impossible d'activer la liaison avec voicechat", e);
        }
        try {
            PYCBukkitEventRegistry.registerPlayerDamagedByPlayerEvent(ModEventListener::onPlayerDamagedByPlayer);
        } catch (NoClassDefFoundError e) {
            getLogger().warning("Impossible d'enregistrer les événements personnalisés: dépendance PYCMod manquante.");
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.WARNING, "Impossible d'enregistrer les événements personnalisés", e);
        }
    }

    public void loadCommon(){
        GameParameters.load();
        GameStatistics.load();
        RankingDisplay.load();
    }

    public void loadGame(){
        ResourceGenerator.load();
        Team.load(Team.getPlayerTeamMapCopy());
        Trader.load();
    }

    @Override
    public void onDisable() {
        if(Game.getCurrentGame() != null){
            Game.getCurrentGame().stopMovementTraceRecorder();
        }
        this.saveCommon();
        this.saveGame();
    }

    public void saveCommon(){
        GameParameters.save();
        GameStatistics.save();
        RankingDisplay.save();
    }

    public void saveGame(){
        ResourceGenerator.save();
        Team.save();
        Trader.save();
    }

    public static ProtectYourCastleMain getInstance() {
        return instance;
    }
}
