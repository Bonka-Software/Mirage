package gg.bonka.mirage.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.bonka.mirage.Mirage;
import gg.bonka.mirage.chunks.ChunkRenderingSystem;
import gg.bonka.mirage.filesystem.WorldsDirectoryManager;
import gg.bonka.mirage.filesystem.eventhandlers.WorldInitHandler;
import gg.bonka.mirage.misc.Chat;
import gg.bonka.mirage.misc.ChatColor;
import gg.bonka.mirage.misc.ConfirmationScreen;
import gg.bonka.mirage.world.MirageWorld;
import gg.bonka.mirage.world.rollback.WorldsTracker;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.stream.Collectors;

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
        player.sendMessage(Chat.format(String.format("Available worlds: %s", WorldsDirectoryManager.getInstance().getWorlds()), ChatColor.INFO));
    }

    @Subcommand("create")
    @CommandCompletion("@nothing @range:1 @world")
    @Description("Creates the given world")
    @CommandPermission("mirage.command.world.create")
    public void create(Player player, @Single String worldName, @Single String persistent, @Optional String copyFromWorldName) {
        if(WorldsDirectoryManager.getInstance().getWorlds().stream().anyMatch(mirageWorld -> mirageWorld.getWorldName().equals(worldName))) {
            player.sendMessage(Chat.format(String.format("World: %s already exists!", worldName), ChatColor.ERROR));
            return;
        }

        boolean isPersistent;

        try {
            isPersistent = Integer.parseInt(persistent) > 0;
        } catch (NumberFormatException e) {
            player.sendMessage(Chat.format(String.format("Invalid value for isPersistent: %s, should either be 0 or 1", persistent), ChatColor.ERROR));
            return;
        }

        if (copyFromWorldName != null) {
            Component successMessage = Chat.format(String.format("Successfully copied: %s to %s world, use /world go %s to check it out!", copyFromWorldName, worldName, worldName), ChatColor.SUCCESS);

            MirageWorld copyFormWorld = WorldsDirectoryManager.getMirageWorld(copyFromWorldName);
            WorldsDirectoryManager.getInstance().copyWorldAsync(copyFormWorld, worldName, isPersistent, (success, message) ->
                    handleWorldCreationCallback(player, success, message, successMessage)
            );

            player.sendMessage(Chat.format(String.format("Copying: %s to the new %s world", copyFromWorldName, worldName), ChatColor.INFO));
            return;
        }

        new WorldInitHandler(worldName, () -> {
            Component successMessage = Chat.format(String.format("Successfully created: %s, use /world go %s to check it out!", worldName, worldName), ChatColor.SUCCESS);

            MirageWorld world = WorldsDirectoryManager.getMirageWorld(worldName);
            world.setPersistent(isPersistent);

            WorldsDirectoryManager.getInstance().saveWorldAsync(world, (success, message) ->
                    handleWorldCreationCallback(player, success, message, successMessage)
            );
        });

        player.sendMessage(Chat.format(String.format("Creating: %s", worldName), ChatColor.INFO));
        //This is very CPU heavy, but doing it async doesn't work because: WorldBorderCenterChangeEvent may only be triggered synchronously.
        //TODO: Try to find a fix for this!
        Bukkit.createWorld(new WorldCreator(worldName));
    }

    @Subcommand("remove")
    @CommandCompletion("@world @world @range:1")
    @Description("Deletes the given world, will send the players in the world to the destination world, will show them a message when you choose the show them!")
    @CommandPermission("mirage.command.world.remove")
    public void remove(Player player, @Single String worldName, @Single String destinationWorldName, @Single String showMessage) {
        if(WorldsDirectoryManager.getInstance().getWorlds().stream().noneMatch(mirageWorld -> mirageWorld.getWorldName().equals(worldName))) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", worldName), ChatColor.ERROR));
            return;
        }

        if(WorldsDirectoryManager.getInstance().getWorlds().stream().noneMatch(mirageWorld -> mirageWorld.getWorldName().equals(destinationWorldName))) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", destinationWorldName), ChatColor.ERROR));
            return;
        }

        boolean showMessageBoolean;

        try {
            showMessageBoolean = Integer.parseInt(showMessage) > 0;
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

                    if(showMessageBoolean)
                        p.sendMessage(Chat.format(String.format("The world %s is being removed!", worldName), ChatColor.ERROR));
                }
            }

            Bukkit.unloadWorld(world, false);
        }

        WorldsDirectoryManager.getInstance().removeWorldAsync(WorldsDirectoryManager.getMirageWorld(worldName), (success, message) -> {
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
        if(WorldsDirectoryManager.getInstance().getWorlds().stream().noneMatch(mirageWorld -> mirageWorld.getWorldName().equals(worldName))) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", worldName), ChatColor.ERROR));
            return;
        }

        World world = Bukkit.getWorld(worldName);

        if(world == null) {
            WorldsDirectoryManager.getInstance().loadWorldAsync(WorldsDirectoryManager.getMirageWorld(worldName), (success, message) ->
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

    @Subcommand("save")
    @CommandCompletion("@world")
    @Description("Saves the world")
    @CommandPermission("mirage.command.world.save")
    public void save(Player player, @Single String worldName) {
        if(WorldsDirectoryManager.getInstance().getWorlds().stream().noneMatch(mirageWorld -> mirageWorld.getWorldName().equals(worldName))) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", worldName), ChatColor.ERROR));
            return;
        }

        MirageWorld world = WorldsDirectoryManager.getMirageWorld(worldName);
        new ConfirmationScreen(player, String.format("Saving %s will override the current save, are you sure?", worldName), confirmed -> handleSaveCallback(player, world, confirmed));
    }

    @Subcommand("reset")
    @CommandCompletion("@world")
    @Description("Resets the world to it's saved stage")
    @CommandPermission("mirage.command.world.reset")
    public void reset(Player player, @Single String worldName) {
        if(WorldsDirectoryManager.getInstance().getWorlds().stream().noneMatch(mirageWorld -> mirageWorld.getWorldName().equals(worldName))) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", worldName), ChatColor.ERROR));
            return;
        }

        MirageWorld world = WorldsDirectoryManager.getMirageWorld(worldName);

        new ConfirmationScreen(player, String.format("Any current changes to %s won't be saved or backed up! Are you sure?", worldName), confirmed -> {
            if(!confirmed) {
                player.sendMessage(Chat.format("Canceled reset!", ChatColor.ERROR));
                return;
            }

            player.sendMessage(Chat.format("Resetting world...", ChatColor.SUCCESS));

            if(world.getUseRollback()) {
                WorldsTracker.getInstance().getTrackedWorld(worldName).reset();
            } else {
                if(!resetWorld(player, world)) {
                    player.sendMessage(Chat.format(String.format("was not able to reset %s", worldName), ChatColor.ERROR));
                    return;
                }
            }

            player.sendMessage(Chat.format(String.format("%s was successfully reset!", worldName), ChatColor.SUCCESS));
        });
    }

    @Subcommand("renderAs")
    @CommandCompletion("@players @world @world")
    @Description("Shows mirage worlds data")
    @CommandPermission("mirage.command.rendering")
    public void renderAs(Player player, @Single String receiverName, @Single String worldName, @Single String visualizerName) {
        Player receiver = Bukkit.getPlayer(receiverName);
        World world = Bukkit.getWorld(worldName);
        World visualizer = Bukkit.getWorld(visualizerName);

        if(receiver == null) {
            player.sendMessage(Chat.format(String.format("%s is not online!", receiverName), ChatColor.ERROR));
            return;
        }

        if(world == null) {
            throwWorldError(player, worldName);
            return;
        }

        if(visualizer == null) {
            throwWorldError(player, visualizerName);
            return;
        }

        if(world.getEnvironment() != visualizer.getEnvironment()) {
            player.sendMessage(Chat.format("The worlds need to be the same dimension!", ChatColor.ERROR));
            return;
        }

        ChunkRenderingSystem.getInstance().renderWorldAs(receiver, world, visualizer);
        ChunkRenderingSystem.getInstance().updateChunks(receiver);
    }

    @Subcommand("settings")
    @CommandCompletion("@world")
    @Description("Opens the world settings GUI")
    @CommandPermission("mirage.command.world.settings")
    public void settings(Player player, @Single String worldName) {
        if(WorldsDirectoryManager.getInstance().getWorlds().stream().noneMatch(mirageWorld -> mirageWorld.getWorldName().equals(worldName))) {
            player.sendMessage(Chat.format(String.format("World: %s doesn't exist!", worldName), ChatColor.ERROR));
            return;
        }

        openSettingsScreen(player, WorldsDirectoryManager.getMirageWorld(worldName));
    }

    private void openSettingsScreen(Player player, MirageWorld world) {
        TagResolver persistentTag = Placeholder.styling("persistent", getColor(world.getPersistent()), ClickEvent.callback(audience -> {
            world.setPersistent(!world.getPersistent());
            refreshSettingsScreen(player, world);
        }));

        TagResolver backupTag = Placeholder.styling("backup", getColor(world.getBackup()), ClickEvent.callback(audience -> {
            world.setBackup(!world.getBackup());
            refreshSettingsScreen(player, world);
        }));

        TagResolver loadOnStartTag = Placeholder.styling("loadonstart", getColor(world.getLoadOnStart()), ClickEvent.callback(audience -> {
            world.setLoadOnStart(!world.getLoadOnStart());
            refreshSettingsScreen(player, world);
        }));

        TagResolver keepInMemoryTag = Placeholder.styling("keepinmemory", getColor(world.getKeepInMemory()), ClickEvent.callback(audience -> {
            world.setKeepInMemory(!world.getKeepInMemory());
            refreshSettingsScreen(player, world);
        }));

        TagResolver useRollbackTag = Placeholder.styling("userollback", getColor(world.getUseRollback()), ClickEvent.callback(audience -> {
            world.setUseRollback(!world.getUseRollback());
            WorldsTracker.getInstance().checkForMirageWorldsUpdate();

            refreshSettingsScreen(player, world);
        }));

        String persistentText = String.format("<persistent>[%s]</persistent>", world.getPersistent());
        String backupText = String.format("<backup>[%s]</backup><newline>", world.getBackup());
        String loadOnStartText = String.format("<loadonstart>[%s]</loadonstart>", world.getLoadOnStart());
        String keepInMemoryText = String.format("<keepinmemory>[%s]</keepinmemory>", world.getKeepInMemory());
        String useRollbackText = String.format("<userollback>[%s]</userollback>", world.getUseRollback());

        Component content = MiniMessage.miniMessage().deserialize(String.format(
                                "Persistent world: %s<br><br>" +
                                "Backup world after unload: %s<br><br>" +
                                "Load on start: %s<br><br>" +
                                "Keep in memory: %s<br><br>" +
                                "Use rollback: %s",
                        persistentText, backupText, loadOnStartText, keepInMemoryText, useRollbackText),
                persistentTag, backupTag, loadOnStartTag, keepInMemoryTag, useRollbackTag);

        Book book = Book.book(Component.text("World settings"), Component.text("Server"), content);
        player.openBook(book);
    }

    private void refreshSettingsScreen(Player player, MirageWorld world) {
        world.save();
        openSettingsScreen(player, world);
    }

    private TextColor getColor(boolean value) {
        return value ? ChatColor.SUCCESS.getTextColor() : ChatColor.ERROR.getTextColor();
    }

    private boolean resetWorld(Player initializer, MirageWorld mirageWorld) {
        World bukkitWorld = Bukkit.getWorld(mirageWorld.getWorldName());

        if(Bukkit.getWorlds().get(0).equals(bukkitWorld)) {
            initializer.sendMessage(Chat.format("It's not possible to reset the main world without rollback enabled!", ChatColor.ERROR));
            return false;
        }

        if(bukkitWorld == null)
            return false;

        for(Player p : bukkitWorld.getPlayers()) {
            p.kick(Chat.format("World resetting", ChatColor.ERROR));
        }

        Bukkit.unloadWorld(bukkitWorld, false);
        WorldsDirectoryManager.getInstance().loadWorldAsync(mirageWorld, (success, message) -> {
            if(!success)
                initializer.sendMessage(Chat.format(message, ChatColor.ERROR));

            Bukkit.createWorld(new WorldCreator(mirageWorld.getWorldName()));
        });

        return true;
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

    private void handleSaveCallback(Player player, MirageWorld world, boolean confirmed) {
        if(!confirmed) {
            player.sendMessage(Chat.format("Canceled world save!", ChatColor.ERROR));
            return;
        }

        player.sendMessage(Chat.format("Saving world...", ChatColor.SUCCESS));
        Objects.requireNonNull(Bukkit.getWorld(world.getWorldName())).save();

        //There is no way to check if the world has finished saving...
        new BukkitRunnable() {
            public void run() {
                WorldsDirectoryManager.getInstance().saveWorldAsync(world, (success, message) -> {
                    if (!success) {
                        player.sendMessage(Chat.format(message, ChatColor.ERROR));
                        return;
                    }

                    WorldsTracker.getInstance().getTrackedWorld(world.getWorldName()).updateSave();
                    player.sendMessage(Chat.format(String.format("%s was saved!", world.getWorldName()), ChatColor.SUCCESS));
                });

                this.cancel();
            }
        }.runTaskLater(Mirage.getInstance(), 40L);
    }

    private void throwWorldError(Player player, String worldName) {
        player.sendMessage(Chat.format(String.format("%s is not a valid world!", worldName), ChatColor.ERROR));
    }

    /**
     * Registers the necessary command completions for the "world" argument.
     * Retrieves the list of world names from the WorldsDirectoryManager class
     * and registers it as a command completion with the PaperCommandManager.
     */
    private void registerWorldArguments() {
        Mirage.getInstance().getCommandManager().getCommandCompletions().registerCompletion("world", c ->
                WorldsDirectoryManager.getInstance().getWorlds().stream().map(MirageWorld::getWorldName).collect(Collectors.toList())
        );
    }
}
