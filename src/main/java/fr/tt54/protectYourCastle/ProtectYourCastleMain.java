package fr.tt54.protectYourCastle;

import org.bukkit.plugin.java.JavaPlugin;

public final class ProtectYourCastleMain extends JavaPlugin {

    private static ProtectYourCastleMain instance;

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {

    }

    public static ProtectYourCastleMain getInstance() {
        return instance;
    }
}
