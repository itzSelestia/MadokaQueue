package dev.yae.madokaQueue.game.gamemode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// the catalogue of playable modes. add a new one here and it appears in /duel and its tab
// completion automatically. gamemodes hold no per-match state, so one shared instance is fine
public final class Gamemodes {
    // linked so the first registered stays the default and the order is stable in tab completion
    private static final Map<String, Gamemode> BY_NAME = new LinkedHashMap<>();

    static {
        register(new SwordGamemode());
    }

    private Gamemodes() {
    }

    private static void register(Gamemode gamemode) {
        BY_NAME.put(gamemode.getName().toLowerCase(Locale.ROOT), gamemode);
    }

    // null if no mode by that name
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
