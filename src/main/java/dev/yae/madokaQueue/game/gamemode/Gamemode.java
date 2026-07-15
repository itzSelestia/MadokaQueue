package dev.yae.madokaQueue.game.gamemode;

import dev.yae.madokaQueue.ui.Palette;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public abstract class Gamemode {
    public abstract String getName();

    public abstract void equipKit(Player player);

    public Component displayName() {
        return Component.text(getName(), Palette.PETAL);
    }

    public void prepareGamemode() {
    }

}
