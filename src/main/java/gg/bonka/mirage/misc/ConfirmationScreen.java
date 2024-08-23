package gg.bonka.mirage.misc;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

public record ConfirmationScreen(Player player, String info, ConfirmationCallback callback) {
    public ConfirmationScreen {
        TagResolver confirmTag = Placeholder.styling("confirm", ChatColor.SUCCESS.getTextColor(), ClickEvent.callback(audience -> callback.callback(true)));
        TagResolver cancelTag = Placeholder.styling("cancel", ChatColor.ERROR.getTextColor(), ClickEvent.callback(audience -> callback.callback(false)));

        String text = String.format("%s<br><br><confirm>[CONFIRM]</confirm>    <cancel>[CANCEL]</cancel>", info);
        Component content = MiniMessage.miniMessage().deserialize(text, confirmTag, cancelTag);

        Book book = Book.book(Component.text("World settings"), Component.text("Server"), content);
        player.openBook(book);
    }

    public interface ConfirmationCallback {
        void callback(boolean confirmed);
    }
}