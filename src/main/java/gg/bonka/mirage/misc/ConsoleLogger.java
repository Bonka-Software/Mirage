package gg.bonka.mirage.misc;

import org.bukkit.Bukkit;

public final class ConsoleLogger {

    // [Mirage] with color codes!
    private static final String prefix = String.format("%s%s%s", ChatColor.INFO.getAnsi("["), ChatColor.MIRAGE_THEME.getAnsi("Mirage"), ChatColor.INFO.getAnsi("]"));

    public static void info(final String message) {
        print(message, ChatColor.INFO);
    }

    public static void error(final String message) {
        print(message, ChatColor.ERROR);
    }

    public static void success(final String message) {
        print(message, ChatColor.SUCCESS);
    }

    private static void print(String message, ChatColor chatColor) {
        Bukkit.getConsoleSender().sendMessage(String.format("%s %s", prefix, chatColor.getAnsi(message)));
    }
}
