package dev.yae.madokaQueue.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InvincibleManager implements Listener {
    private static final Set<UUID> immortalPlayers = new HashSet<>();

    public static void setInvincible(UUID player, boolean invincible) {
        if (invincible) {
            immortalPlayers.add(player);
        } else {
            immortalPlayers.remove(player);
        }
    }

    public static boolean isInvincible(UUID player) {
        return immortalPlayers.contains(player);
    }

    public static void clearAll() {
        immortalPlayers.clear();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && immortalPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        immortalPlayers.remove(event.getPlayer().getUniqueId());
    }
}
