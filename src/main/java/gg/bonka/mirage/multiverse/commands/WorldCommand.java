package gg.bonka.mirage.multiverse.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.misc.Chat;
import gg.bonka.mirage.misc.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.IOException;

@CommandAlias("world")
@CommandPermission("mirage.command.world")
public class WorldCommand extends BaseCommand {

    public WorldCommand() {
        Mirage.commandManager.getCommandCompletions().registerCompletion("world", c -> WorldsDirectoryManager.getInstance().getWorldNames());
    }

    @Default
    @CommandCompletion("@nothing")
    @Description("Shows mirage worlds data")
    public static void onCommand(Player player) {
        player.sendMessage(Chat.format(String.format("Available worlds: %s", WorldsDirectoryManager.getInstance().getWorldNames()), ChatColor.INFO));
    }

    @Subcommand("go")
    @CommandCompletion("@world")
    @Description("Teleport to the given world")
    public static void go(Player player, @Single String worldName) throws IOException {
        World world = Bukkit.getWorld(worldName);

        if(world == null) {
            if(!WorldsDirectoryManager.getInstance().getWorldNames().contains(worldName)) {
                player.sendMessage(Chat.format(String.format("%s does not exist! use /world create %s to create it", worldName, worldName), ChatColor.ERROR));
                return;
            }

            WorldsDirectoryManager.getInstance().loadWorld(worldName);
            world = Bukkit.createWorld(WorldCreator.name(worldName));
        }

        assert world != null;
        player.teleport(world.getSpawnLocation());
    }
}
