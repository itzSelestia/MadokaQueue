package dev.yae.madokaQueue.util;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectatorManager implements Listener {
    private static final Map<UUID, GameMode> previousModes = new HashMap<>();

    public static void setSpectator(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        previousModes.putIfAbsent(uuid, player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
    }

    public static void restore(UUID uuid) {
        GameMode previous = previousModes.get(uuid);
        if (previous == null) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        player.setGameMode(previous);
        previousModes.remove(uuid);
    }

    public static void restoreAll() {
        for (Map.Entry<UUID, GameMode> entry : previousModes.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.setGameMode(entry.getValue());
            }
        }

        previousModes.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameMode previous = previousModes.remove(player.getUniqueId());
        if (previous != null) {
            player.setGameMode(previous);
        }
    }
}
