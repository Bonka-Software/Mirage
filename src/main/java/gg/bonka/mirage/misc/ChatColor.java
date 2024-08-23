package gg.bonka.mirage.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;

@AllArgsConstructor
public enum ChatColor {

    INFO(TextColor.color(172, 196, 255), "\u001B[36m"),
    ERROR(TextColor.color(255, 75, 65), "\u001B[31m"),
    SUCCESS(TextColor.color(143, 255, 123), "\u001B[32m"),

    MIRAGE_THEME(TextColor.color(237, 87, 248), "\u001B[35m");

    @Getter
    private final TextColor textColor;

    private final String ansi;

    public String getAnsi(String text) {
        // u001B[0m = reset
        // More info about ansi escape codes: https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
        return String.format("%s%s\u001B[0m", ansi, text);
    }
}
