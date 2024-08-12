package gg.bonka.mirage.multiverse.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.misc.Chat;
import gg.bonka.mirage.misc.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("world")
@CommandPermission("mirage.command.world")
public class WorldCommand extends BaseCommand {

    @Default
    @CommandCompletion("@nothing")
    @Description("Shows mirage worlds data")
    public static void onCommand(Player player) {
        player.sendMessage(Chat.format(String.format("Available worlds: %s", WorldsDirectoryManager.getInstance().getWorlds()), ChatColor.INFO));
    }
}
