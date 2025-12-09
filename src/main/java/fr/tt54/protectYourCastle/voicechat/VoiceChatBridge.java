package fr.tt54.protectYourCastle.voicechat;

import fr.tt54.protectYourCastle.game.Team;
import fr.tt54.pycmod.PYCVoiceChatPlugin;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VoiceChatBridge {

    private boolean enabled = false;
    private UUID globalGroupUUID;

    public void enable(){
        try{
            PYCVoiceChatPlugin.waitForEnabling().thenAccept(voicechatApi -> {
                this.enabled = true;
                this.globalGroupUUID = PYCVoiceChatPlugin.createOpenGroup("global", true);
            });
        } catch (NoClassDefFoundError e){
            this.enabled = false;
            System.err.println("Le mod PYCMod n'a pas été trouvé");
        }
    }

    public boolean isEnabled(){
        return this.enabled;
    }

    public void createTeamGroup(Team team){
        if(!this.enabled) return;
        UUID groupUUID = PYCVoiceChatPlugin.createOpenGroup(team.getColor().name(), true);
        team.setVoiceChatGroupUUID(groupUUID);
        for(UUID member : team.getMembers()){
            PYCVoiceChatPlugin.joinGroup(groupUUID, member);
        }
    }

    public void joinTeamGroup(Player player, Team team){
        if(!this.enabled || team.getVoiceChatGroupUUID() == null) return;
        PYCVoiceChatPlugin.joinGroup(team.getVoiceChatGroupUUID(), player.getUniqueId());
    }

    public void joinGlobalGroup(Player player){
        if(!this.enabled || this.globalGroupUUID == null) return;
        PYCVoiceChatPlugin.joinGroup(this.globalGroupUUID, player.getUniqueId());
    }

}
