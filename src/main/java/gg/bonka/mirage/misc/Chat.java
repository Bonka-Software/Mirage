package gg.bonka.mirage.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class Chat {

    private static final String prefix = "<color>[<gradient:#AB8EE4:#E24DC9>Mirage</gradient><color>]";

    public static Component format(String message, final ChatColor color) {
        message = String.format("%s <color>%s", prefix, message);
        return MiniMessage.miniMessage().deserialize(message, Placeholder.styling("color", color.getTextColor()));
    }
}
