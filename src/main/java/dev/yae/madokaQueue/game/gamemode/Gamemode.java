package dev.yae.madokaQueue.game.gamemode;

import dev.yae.madokaQueue.ui.Palette;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

// one duel ruleset: what the fighters are handed at the start of every round. subclasses vary the
// kit; the match loop, hud and everything else stay the same
public abstract class Gamemode {
    // shown in the hud, chat and tab completion, so keep it short
    public abstract String getName();

    // called for both players at the start of each round, after their inventory is cleared
    public abstract void equipKit(Player player);

    // styled for the sakura hud. override if a mode wants its own accent
    public Component displayName() {
        return Component.text(getName(), Palette.PETAL);
    }
}
