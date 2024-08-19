package gg.bonka.mirage.multiverse.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.filesystem.eventhandlers.WorldInitHandler;
import gg.bonka.mirage.misc.Chat;
import gg.bonka.mirage.misc.ChatColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@CommandAlias("world")
@CommandPermission("mirage.command.world")
public class WorldCommand extends BaseCommand {

    public WorldCommand() {
        registerWorldArguments();
    }

    @Default
    @CommandCompletion("@nothing")
    @Description("Shows mirage worlds data")
    public void onCommand(Player player) {
        player.sendMessage(Chat.format(String.format("Available worlds: %s", WorldsDirectoryManager.getInstance().getWorldNames()), ChatColor.INFO));
    }

    @Subcommand("create")
    @CommandCompletion("@nothing @world")
    @Description("Creates the given world")
    @CommandPermission("mirage.command.world.create")
    public void create(Player player, @Single String worldName, @Optional String copyFromWorldName) {
        if(WorldsDirectoryManager.getInstance().getWorldNames().contains(worldName)) {
            player.sendMessage(Chat.format(String.format("World: %s already exists!", worldName), ChatColor.ERROR));
            return;
        }

        if (copyFromWorldName != null) {
            Component successMessage = Chat.format(String.format("Successfully copied: %s to %s world, use /world go %s to check it out!", copyFromWorldName, worldName, worldName), ChatColor.SUCCESS);
            WorldsDirectoryManager.getInstance().copyWorldAsync(copyFromWorldName, worldName, (success, message) ->
                    handleWorldCreationCallback(player, success, message, successMessage)
            );

            player.sendMessage(Chat.format(String.format("Copying: %s to the new %s world", copyFromWorldName, worldName), ChatColor.INFO));
            return;
        }

        new WorldInitHandler(worldName, () -> {
            Component successMessage = Chat.format(String.format("Successfully created: %s, use /world go %s to check it out!", worldName, worldName), ChatColor.SUCCESS);

            WorldsDirectoryManager.getInstance().saveWorldAsync(worldName, (success, message) ->
                    handleWorldCreationCallback(player, success, message, successMessage)
            );
        });

        player.sendMessage(Chat.format(String.format("Creating: %s", worldName), ChatColor.INFO));
        Bukkit.createWorld(new WorldCreator(worldName));
    }

    @Subcommand("remove")
    @CommandCompletion("@world @world @range:1")
    @Description("Deletes the given world, will send the players in the world to the destination world, will show them a message when you choose the show them!")
    @CommandPermission("mirage.command.world.remove")
    public void remove(Player player, @Single String worldName, @Single String destinationWorldName, @Single String showMessage) {
        if(!WorldsDirectoryManager.getInstance().getWorldNames().contains(worldName)) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", worldName), ChatColor.ERROR));
            return;
        }

        if(!WorldsDirectoryManager.getInstance().getWorldNames().contains(destinationWorldName)) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", destinationWorldName), ChatColor.ERROR));
            return;
        }

        int showMessageBoolean;

        try {
            showMessageBoolean = Integer.parseInt(showMessage);
        } catch (NumberFormatException e) {
            player.sendMessage(Chat.format(String.format("Invalid value for showMessage: %s, should either be 0 or 1", showMessage), ChatColor.ERROR));
            return;
        }

        player.sendMessage(Chat.format(String.format("Deleting world: %s", worldName), ChatColor.INFO));

        World world = Bukkit.getWorld(worldName);
        World destination = Bukkit.createWorld(new WorldCreator(destinationWorldName));

        if(world != null) {
            for(Player p : world.getPlayers()) {
                if(destination != null) {
                    p.teleport(destination.getSpawnLocation());

                    if(showMessageBoolean > 0)
                        p.sendMessage(Chat.format(String.format("The world %s is being removed!", worldName), ChatColor.ERROR));
                }
            }

            Bukkit.unloadWorld(world, false);
        }

        WorldsDirectoryManager.getInstance().removeWorldAsync(worldName, (success, message) -> {
            if (success) {
                player.sendMessage(Chat.format(String.format("World: %s has been removed successfully!", worldName), ChatColor.SUCCESS));
                registerWorldArguments();
            } else {
                player.sendMessage(Chat.format(message, ChatColor.ERROR));
            }
        });
    }

    @Subcommand("go")
    @CommandCompletion("@world")
    @Description("Teleport to the given world")
    @CommandPermission("mirage.command.world.teleport")
    public void go(Player player, @Single String worldName) {
        if(!WorldsDirectoryManager.getInstance().getWorldNames().contains(worldName)) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", worldName), ChatColor.ERROR));
            return;
        }

        World world = Bukkit.getWorld(worldName);

        if(world == null) {
            if(!WorldsDirectoryManager.getInstance().getWorldNames().contains(worldName)) {
                player.sendMessage(Chat.format(String.format("%s does not exist! use /world create %s to create it", worldName, worldName), ChatColor.ERROR));
                return;
            }

            WorldsDirectoryManager.getInstance().loadWorldAsync(worldName, (success, message) ->
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            World createdWorld = Bukkit.createWorld(WorldCreator.name(worldName));

                            if(success && createdWorld != null) {
                                player.teleport(createdWorld.getSpawnLocation());
                            } else {
                                player.sendMessage(Chat.format(message.isEmpty() ? "world = null, an error log should be printed to the console." : message, ChatColor.ERROR));
                            }

                            this.cancel();
                        }
                    }.runTask(Mirage.getInstance())
            );

            return;
        }

        player.teleport(world.getSpawnLocation());
    }

    /**
     * Handles the callback for world creation.
     *
     * @param player          The player sending the command
     * @param success         The success status of the world creation
     * @param message         The error message if the creation failed
     * @param successMessage  The success message to send if the creation succeeded
     */
    private void handleWorldCreationCallback(Player player, boolean success, String message, Component successMessage) {
        if(success) {
            player.sendMessage(successMessage);
            registerWorldArguments();
        } else {
            player.sendMessage(Chat.format(message, ChatColor.ERROR));
        }
    }

    /**
     * Registers the necessary command completions for the "world" argument.
     * Retrieves the list of world names from the WorldsDirectoryManager class
     * and registers it as a command completion with the PaperCommandManager.
     */
    private void registerWorldArguments() {
        Mirage.getInstance().getCommandManager().getCommandCompletions().registerCompletion("world", c -> WorldsDirectoryManager.getInstance().getWorldNames());
    }
}
