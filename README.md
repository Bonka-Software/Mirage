# Mirage

[Available on Spigot](https://www.spigotmc.org/resources/mirage.119105/)

Custom map loading system for minecraft servers.
It's able to reset maps to their saved state on server startup and during runtime.
The world tracking system also allows for runtime rollbacks without kicking players.

![WorldReset-optimize](https://github.com/user-attachments/assets/35efe830-a17c-4dc8-9181-56f070bf9401)

---

#### Version 1.1.7
This version of Mirage contains:
- Mirage world loading
- Backup system
- Rollbacks
- Per world (in-game) configurability
- Per player chunk rendering (Mainly available through the API)

![WorldSwitching-ezgif com-optimize](https://github.com/user-attachments/assets/b2a496a7-3b4b-4d18-8eb5-2f7d5fd5ea2a)

These are all the same world, just rendering as different mirage worlds! Using /world renderas <name> <world> <visualizer>

---

## Maven

```xml
<repository>
    <id>bonka</id>
    <url>https://nexus.bonka.gg/repository/maven-public/</url>
</repository>
```

```xml
<dependency>
    <groupId>gg.bonka</groupId>
    <artifactId>Mirage</artifactId>
    <version>1.1.7</version>
    <scope>provided</scope>
</dependency>
```

---

## API

Mirage uses 2 singletons from where you can easily save, load, backup and rollback mirage worlds!
These singletons are `WorldsDirectoryManager` and `WorldsTracker`.

Mirage 1.1 also introduces a new Singleton `ChunkRenderingSystem` for rendering fake chunks.

Also, don't forget to add mirage as a dependency in you `plugin.yml`:<br>
`depend: [Mirage]` or `softdepend: [Mirage]`

### WorldsDirectoryManager

The worlds directory manager is what makes Mirage tick.
Use `WorldsDirectoryManager.getMirageWorld(String worldName)` to convert any world into a mirage world!
You can then use `WorldsDirectoryManager.getInstance()` to save, load, remove, or backup the world.
Although it's important to note that the WorldsDirectoryManager is not responsible for runtime worlds!
So make sure you unload your worlds before doing anything with them.

example: 
```java
void deleteWorld(String worldName) {
    World world = Bukkit.getWorld(worldName);

    for(Player player : world.getPlayers()) {
        player.kick(Component.text("World is being deleted!"));
    }

    Bukkit.unloadWorld(world, false);

    WorldsDirectoryManager.getInstance().removeWorldAsync(WorldsDirectoryManager.getMirageWorld(worldName), (success, message) -> {
        if (success) {
            player.sendMessage(Chat.format(String.format("World: %s has been removed successfully!", worldName), ChatColor.SUCCESS));
        } else {
            player.sendMessage(Chat.format(message, ChatColor.ERROR));
        }
    });
}
```

I generally recommend using the Async WorldsDirectoryManager methods, keep in mind that the callback is also on that async though!

### WorldsTracker

Only relevant for worlds that have use-rollback enabled!
Stores the initial state of chunks in memory and actively keeps track of changed blocks of the worlds it is tracking.

Use `WorldsTracker.getInstance().getTrackedWorld(String worldName)` to get a tracked world.
You can either update the tracked world, meaning you save the current state of the world to memory (Not to disk, you can only do that through the WorldsDirectoryManager).
Or you can reset the world to its previous state (again, memory only, the world won't get reloaded from disk, this is impossible without kicking players from the world).

These function are: `trackedWorld.updateSave()` and `trackedWorld.reset()`.

![WorldSave](https://github.com/user-attachments/assets/bf5177c4-435f-4ced-88e5-f7dd992fc19c)

### ChunksRenderingSystem

Used to render different chunks or worlds for different players.
This system may be useful if you run a RPG server, or other very linear games.
Keep in mind that this is rendering only, players can't really interact with fake chunks out of the box.
You'll have to implement some other programming trickery to support interactions. 
But this system is perfect for rendering out of bounds chunks!

This system can be accessed via the `ChunksRenderingSystem` singleton.

```java
void RenderWorldAs(Player player, World world, World visualizer) {
    //Renders the world as the visualizer for the given player
    ChunkRenderingSystem.getInstance().renderWorldAs(player, world, visualizer);

    //Don't forget to reload the player's chunks when you change their chunks
    ChunkRenderingSystem.getInstance().updateChunks(receiver);   
}
```

You can subscribe to the `StartPlayerWorldRenderingReloadEvent` & `FinishPlayerWorldRenderingReloadEvent` these are called when a player's world rendering changes.

---

## Server version Paper 1.21.4
[Download directly here](https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/134/downloads/paper-1.21.4-134.jar)
or [Browse paper builds](https://papermc.io/downloads/all)

---

## How to start:
- Download all maven dependencies
- Follow the steps described in [paper nms maven plugin's readme](https://github.com/Alvinn8/paper-nms-maven-plugin)
- Follow the steps described in the [Server Readme](server/README.md)
- Run jetbrains .run config and a test server automatically starts
