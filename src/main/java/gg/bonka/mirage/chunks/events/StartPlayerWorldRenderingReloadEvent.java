package gg.bonka.mirage.chunks.events;

import org.bukkit.entity.Player;

/**
 * Called when a player's world rendering is altered by Mirage.
 * This event will be called at the start of the world reload, use
 * @see FinishPlayerWorldRenderingReloadEvent when you want to do stuff after the reload.
 */
public class StartPlayerWorldRenderingReloadEvent extends WorldRenderingEvent {
    public StartPlayerWorldRenderingReloadEvent(Player player) {
        super(player);
    }
}
