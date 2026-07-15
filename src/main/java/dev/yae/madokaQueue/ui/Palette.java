package dev.yae.madokaQueue.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class Palette {
    public static final TextColor PETAL = TextColor.fromHexString("#FFB7C5");
    public static final TextColor BLOSSOM = TextColor.fromHexString("#FF8FAB");
    public static final TextColor SOFT = TextColor.fromHexString("#FFE4EC");
    public static final TextColor SNOW = TextColor.fromHexString("#FFFFFF");

    public static final String FLOWER = "❀";

    private Palette() {
    }

    public static Component petals(Component text) {
        return Component.text()
                .append(Component.text(FLOWER + " ", PETAL))
                .append(text)
                .append(Component.text(" " + FLOWER, PETAL))
                .build();
    }

    public static Component prefix() {
        return Component.text()
                .append(Component.text(FLOWER + " ", PETAL))
                .append(Component.text("Duel", BLOSSOM, TextDecoration.BOLD))
                .append(Component.text(" ▸ ", SOFT))
                .build();
    }

    public static Component info(Component body) {
        return prefix().append(body);
    }

    public static Component info(String body) {
        return info(Component.text(body, SNOW));
    }

    public static Component warn(String body) {
        return prefix().append(Component.text(body, BLOSSOM));
    }
}
