# Mirage

Custom map loading system for minecraft servers.
It's able to reset maps to their saved state on server startup and during runtime.
The world tracking system also allows for runtime rollbacks without kicking players.

---

#### Version 1.0.0
The very first official version of Mirage contains:
- Mirage world loading
- Backup system
- Rollbacks
- Per world (in-game) configurability

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
    <version>1.0.0</version>
</dependency>
```

---

## API

Mirage uses 2 singletons from where you can easily save, load, backup and rollback mirage worlds!
These singletons are `WorldsDirectoryManager` and `WorldsTracker`.

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

---

## Server version Paper 1.20.1
[Download directly here](https://api.papermc.io/v2/projects/paper/versions/1.20.1/builds/196/downloads/paper-1.20.1-196.jar)
or [Browse paper builds](https://papermc.io/downloads/all)

---

## How to start:
- Download all maven dependecies
- Follow the steps described in [paper nms maven plugin's readme](https://github.com/Alvinn8/paper-nms-maven-plugin)
- Follow the steps described in the [Server Readme](server/README.md)
- Run jetbrains .run config and a test server automatically starts