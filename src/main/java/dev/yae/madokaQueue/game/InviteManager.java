package dev.yae.madokaQueue.game;

import dev.yae.madokaQueue.game.gamemode.Gamemode;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InviteManager {
    public record Invite(UUID sender, UUID target, int rounds, Gamemode gamemode, BukkitTask expiry) {
    }

    private final Map<UUID, Map<UUID, Invite>> byTarget = new HashMap<>();

    public void add(Invite invite) {
        remove(invite.target(), invite.sender());
        byTarget.computeIfAbsent(invite.target(), key -> new HashMap<>())
                .put(invite.sender(), invite);
    }

    public Invite get(UUID target, UUID sender) {
        Map<UUID, Invite> invites = byTarget.get(target);
        return invites == null ? null : invites.get(sender);
    }

    public Invite remove(UUID target, UUID sender) {
        Map<UUID, Invite> invites = byTarget.get(target);
        if (invites == null) {
            return null;
        }

        Invite invite = invites.remove(sender);
        if (invite != null) {
            invite.expiry().cancel();
        }

        if (invites.isEmpty()) {
            byTarget.remove(target);
        }

        return invite;
    }

    public Map<UUID, Invite> invitesFor(UUID target) {
        return byTarget.getOrDefault(target, Map.of());
    }

    public void clear(UUID player) {
        Map<UUID, Invite> received = byTarget.remove(player);
        if (received != null) {
            received.values().forEach(invite -> invite.expiry().cancel());
        }

        for (Map<UUID, Invite> invites : byTarget.values()) {
            Invite sent = invites.remove(player);
            if (sent != null) {
                sent.expiry().cancel();
            }
        }

        byTarget.values().removeIf(Map::isEmpty);
    }
}
