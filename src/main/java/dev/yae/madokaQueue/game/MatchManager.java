package dev.yae.madokaQueue.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MatchManager {
    private final Map<UUID, Match> activeMatches = new HashMap<>();

    public void register(Match match) {
        activeMatches.put(match.getPlayer1(), match);
        activeMatches.put(match.getPlayer2(), match);
    }

    public void unregister(Match match) {
        activeMatches.remove(match.getPlayer1());
        activeMatches.remove(match.getPlayer2());
    }

    public Match getMatch(UUID uuid) {
        return activeMatches.get(uuid);
    }

    public void cleanUp(Match match) {
        if (match.getMatchState() == MatchState.ENDED) {
            unregister(match);
        }
    }
}
