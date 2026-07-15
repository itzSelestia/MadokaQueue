package dev.yae.madokaQueue.game.listeners;

import dev.yae.madokaQueue.game.Match;
import dev.yae.madokaQueue.game.MatchManager;
import dev.yae.madokaQueue.game.MatchState;
import dev.yae.madokaQueue.ui.Palette;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.w3c.dom.Text;

import java.util.UUID;

public class MatchListener implements Listener {
    private static final double MAX_DISTANCE = 800.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;

    private final MatchManager matchManager;

    public MatchListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        forfeit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        forfeit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        checkDistance(event.getPlayer(), to);
    }

    @EventHandler
    public void onCommandExecute(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (matchManager.getMatch(player.getUniqueId()) == null) {
            return;
        }
        player.sendMessage(Palette.info(Component.text("Do not use commands while fighting", Palette.BLOSSOM)));
        event.setCancelled(true);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        checkDistance(event.getPlayer(), event.getTo());
    }

    private void checkDistance(Player player, Location to) {
        Match match = matchManager.getMatch(player.getUniqueId());
        if (match == null || match.getMatchState() != MatchState.IN_PROGRESS) {
            return;
        }

        if (match.isSettingUpRound() || match.isCelebrating()) {
            return;
        }

        Player opponent = Bukkit.getPlayer(match.getOpponent(player.getUniqueId()));
        if (opponent == null) {
            return;
        }

        if (!opponent.getWorld().equals(to.getWorld())) {
            return;
        }

        if (to.distanceSquared(opponent.getLocation()) > MAX_DISTANCE_SQUARED) {
            forfeit(player.getUniqueId());
        }
    }

    private void forfeit(UUID uuid) {
        Match match = matchManager.getMatch(uuid);
        if (match == null) {
            return;
        }

        match.forfeit(uuid);
        matchManager.cleanUp(match);
    }
}
