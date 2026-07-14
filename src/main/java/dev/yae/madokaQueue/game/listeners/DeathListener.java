package dev.yae.madokaQueue.game.listeners;

import dev.yae.madokaQueue.game.Match;
import dev.yae.madokaQueue.game.MatchManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class DeathListener implements Listener {
    private final MatchManager matchManager;

    public DeathListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    // players in a match never really die: the killing blow is cancelled and they go to spectator,
    // so there is no death screen, no respawn, no dropped items
    // HIGH + ignoreCancelled so the god mode check during the countdown gets to cancel first
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Match match = matchManager.getMatch(player.getUniqueId());
        if (match == null) {
            return;
        }

        if (player.getHealth() - event.getFinalDamage() > 0) {
            return;
        }

        event.setCancelled(true);
        player.setFireTicks(0);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());

        match.onPlayerDeath(player.getUniqueId());
        matchManager.cleanUp(match);
    }

    // backstop for anything that skips the damage event, like /kill or setHealth(0)
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID dead = event.getEntity().getUniqueId();
        Match match = matchManager.getMatch(dead);
        if (match == null) {
            return;
        }

        match.onPlayerDeath(dead);
        matchManager.cleanUp(match);
    }
}
