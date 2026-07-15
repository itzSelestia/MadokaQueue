package dev.yae.madokaQueue.util;

import dev.yae.madokaQueue.MadokaQueue;
import dev.yae.madokaQueue.game.Match;
import dev.yae.madokaQueue.game.MatchManager;
import dev.yae.madokaQueue.game.gamemode.Gamemode;
import dev.yae.madokaQueue.game.gamemode.Gamemodes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class QueueManager implements Listener {
    private static final int ROUNDS = 3;
    private static final long PERIOD_TICKS = 20L;

    private final Map<Gamemode, Deque<UUID>> queues = new LinkedHashMap<>();
    private final MatchManager matchManager;
    private BukkitTask task;

    public QueueManager(MatchManager matchManager) {
        this.matchManager = matchManager;
        for (Gamemode gamemode : Gamemodes.all()) {
            queues.put(gamemode, new ArrayDeque<>());
        }
    }

    public boolean addToQueue(Gamemode gamemode, UUID uuid) {
        Deque<UUID> queue = queueFor(gamemode);
        if (queue == null) {
            return false;
        }
        if (matchManager.getMatch(uuid) != null || isQueued(uuid)) {
            return false;
        }

        queue.add(uuid);
        return true;
    }

    public boolean removeFromQueue(UUID uuid) {
        boolean removed = false;
        for (Deque<UUID> queue : queues.values()) {
            removed |= queue.remove(uuid);
        }
        return removed;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeFromQueue(event.getPlayer().getUniqueId());
    }

    public boolean isQueued(UUID uuid) {
        for (Deque<UUID> queue : queues.values()) {
            if (queue.contains(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(
                MadokaQueue.getInstance(), this::checkQueues, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        queues.values().forEach(Deque::clear);
    }

    private void checkQueues() {
        queues.forEach((gamemode, queue) -> {
            Player first;
            while ((first = pollPlayable(queue)) != null) {
                Player second = pollPlayable(queue);
                if (second == null) {
                    queue.addFirst(first.getUniqueId());
                    break;
                }

                Match match = new Match(first.getUniqueId(), second.getUniqueId(), ROUNDS, gamemode);
                matchManager.register(match);
                match.startMatch();
            }
        });
    }

    private Player pollPlayable(Deque<UUID> queue) {
        UUID uuid;
        while ((uuid = queue.poll()) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && matchManager.getMatch(uuid) == null) {
                return player;
            }
        }
        return null;
    }

    private Deque<UUID> queueFor(Gamemode gamemode) {
        Gamemode canonical = Gamemodes.get(gamemode.getName());
        return canonical == null ? null : queues.get(canonical);
    }
}
