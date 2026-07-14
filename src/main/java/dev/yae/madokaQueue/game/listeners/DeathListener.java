package dev.yae.madokaQueue.game.listeners;

import dev.yae.madokaQueue.game.Match;
import dev.yae.madokaQueue.game.MatchManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class DeathListener implements Listener {
    private final MatchManager matchManager;

    public DeathListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

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
