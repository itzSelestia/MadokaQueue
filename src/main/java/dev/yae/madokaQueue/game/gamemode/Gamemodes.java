package dev.yae.madokaQueue.game.gamemode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Gamemodes {
    private static final Map<String, Gamemode> BY_NAME = new LinkedHashMap<>();

    static {
        register(new SwordGamemode());
        register(new VanillaGamemode());
        register(new MaceGamemode());
    }

    private Gamemodes() {
    }

    private static void register(Gamemode gamemode) {
        BY_NAME.put(gamemode.getName().toLowerCase(Locale.ROOT), gamemode);
    }

    public static Gamemode get(String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static Gamemode getDefault() {
        return BY_NAME.values().iterator().next();
    }

    public static Collection<Gamemode> all() {
        return BY_NAME.values();
    }

    public static List<String> names() {
        return all().stream().map(Gamemode::getName).toList();
    }
}
