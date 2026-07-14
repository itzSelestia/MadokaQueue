package dev.yae.madokaQueue.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HealManager {
    public static void healPlayer(UUID player) {
        Player player1 = Bukkit.getPlayer(player);
        assert player1 != null;
        player1.heal(player1.getMaxHealth());
        player1.setSaturation(20);
    }
}
