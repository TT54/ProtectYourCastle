package fr.tt54.protectYourCastle.mod_bridges;

import fr.tt54.pycmod.PYCCuriosBridge;
import org.bukkit.entity.Player;

public class CuriosBridge {

    public static void clearPlayerCuriosInventory(Player player){
        try {
            PYCCuriosBridge.clearCuriosItems(player.getUniqueId());
        } catch (NoClassDefFoundError ignore){}
    }

}
