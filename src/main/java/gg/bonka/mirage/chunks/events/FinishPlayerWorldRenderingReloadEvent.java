package gg.bonka.mirage.chunks.events;

import org.bukkit.entity.Player;

/**
 * Called when a player's world rendering is altered by Mirage.
 * This event will be called after the world reload, use
 * @see StartPlayerWorldRenderingReloadEvent when you want to do stuff pre reload.
 */
public class FinishPlayerWorldRenderingReloadEvent extends WorldRenderingEvent {
    public FinishPlayerWorldRenderingReloadEvent(Player player) {
        super(player);
    }
}
