package dev.yae.madokaQueue.game;

import dev.yae.madokaQueue.MadokaQueue;
import dev.yae.madokaQueue.game.gamemode.Gamemode;
import dev.yae.madokaQueue.ui.MatchHud;
import dev.yae.madokaQueue.util.CorpseManager;
import dev.yae.madokaQueue.util.HealManager;
import dev.yae.madokaQueue.util.InvincibleManager;
import dev.yae.madokaQueue.util.SpectatorManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class Match {
    private final UUID player1;
    private final UUID player2;
    private final int rounds;
    private final Gamemode gamemode;
    private int currentRound = 1;
    private int player1score = 0;
    private int player2score = 0;
    private MatchState matchState = MatchState.WAITNG;
    private UUID winner;
    private boolean settingUpRound = false;
    private boolean celebrating = false;
    private final int countdown = 5;
    // the first round gives players longer to get ready
    private final int firstRoundCountdown = 10;
    private final int victoryCountdown = 3;
    // seconds the body stays up after a kill. independent of victoryCountdown, so it can outlive
    // the celebration and linger into the next round if you want it to
    private final int corpseLifetime = 3;
    private final MadokaQueue instance = MadokaQueue.getInstance();
    private final MatchHud hud;
    private BukkitTask countdownTask;
    private BukkitTask celebrationTask;

    public Match(UUID player1, UUID player2, int rounds, Gamemode gamemode) {
        if (rounds < 1 || rounds % 2 == 0) {
            throw new IllegalArgumentException("Invalid rounds amount");
        }
        this.player1 = player1;
        this.player2 = player2;
        this.rounds = rounds;
        this.gamemode = gamemode;
        this.hud = new MatchHud(player1, player2, gamemode.displayName());
    }

    public void startMatch() {
        if (matchState != MatchState.WAITNG) {
            return;
        }
        matchState = MatchState.IN_PROGRESS;
        hud.matchStart(rounds);
        startRound();
    }

    private void startRound() {
        cancelCountDown();
        settingUpRound = true;
        rtp();
        setGodMode(true);
        HealManager.healPlayer(player1);
        HealManager.healPlayer(player2);
        equipKit(player1);
        equipKit(player2);
        hud.roundStart(currentRound, rounds);
        startCountDown();
    }

    private void equipKit(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            gamemode.equipKit(player);
        }
    }


    public void onPlayerDeath(UUID deadPlayer, Component deathMessage) {
        if (matchState != MatchState.IN_PROGRESS || settingUpRound || celebrating || !hasPlayer(deadPlayer)) {
            return;
        }

        if (deadPlayer.equals(player1)) {
            player2score++;
        } else {
            player1score++;
        }

        currentRound++;

        celebrating = true;
        setGodMode(true);
        hud.deathMessage(deathMessage);
        hud.roundOver(deadPlayer, player1score, player2score);
        CorpseManager.spawn(deadPlayer, corpseLifetime);
        SpectatorManager.setSpectator(deadPlayer);
        startCelebration();
    }


    public void forfeit(UUID offender) {
        if (matchState == MatchState.ENDED || !hasPlayer(offender)) {
            return;
        }
        winner = getOpponent(offender);
        hud.forfeited(offender);
        endMatch();
    }

    private boolean isMatchOver() {
        if (currentRound > rounds) {
            return true;
        }
        int scoreToWin = rounds / 2 + 1;
        return player1score >= scoreToWin || player2score >= scoreToWin;
    }

    private void rtp() {
        // rtp logic here
        System.out.println("RTP");
    }

    private void startCountDown() {
        int seconds = currentRound == 1 ? firstRoundCountdown : countdown;
        countdownTask = new BukkitRunnable() {
            int secondsLeft = seconds;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    cancel();
                    countdownTask = null;
                    setGodMode(false);
                    settingUpRound = false;
                    hud.fight();
                    return;
                }

                hud.countdown(secondsLeft);

                secondsLeft--;
            }
        }.runTaskTimer(instance, 0L, 20L);

    }


    private void startCelebration() {
        celebrationTask = new BukkitRunnable() {
            int secondsLeft = victoryCountdown;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    cancel();
                    celebrationTask = null;
                    endCelebration();
                    return;
                }

                hud.nextRoundIn(secondsLeft);

                secondsLeft--;
            }
        }.runTaskTimer(instance, 0L, 20L);
    }

    private void endCelebration() {
        celebrating = false;
        restoreGameModes();

        if (isMatchOver()) {
            endMatch();
        } else {
            startRound();
        }
    }

    private void cancelCountDown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void cancelCelebration() {
        if (celebrationTask != null) {
            celebrationTask.cancel();
            celebrationTask = null;
        }
    }

    private void setGodMode(boolean invincible) {
        InvincibleManager.setInvincible(player1, invincible);
        InvincibleManager.setInvincible(player2, invincible);
    }

    private void restoreGameModes() {
        SpectatorManager.restore(player1);
        SpectatorManager.restore(player2);
    }

    private void endMatch() {
        if (matchState == MatchState.ENDED) {
            return;
        }
        matchState = MatchState.ENDED;
        cancelCountDown();
        cancelCelebration();
        setGodMode(false);
        restoreGameModes();
        settingUpRound = false;
        celebrating = false;
        if (winner == null) {
            winner = player1score > player2score ? player1 : player2;
        }
        hud.matchOver(winner, player1score, player2score);
        // a match that ends inside the celebration timer is not on any listener's code path,
        // so it has to drop itself from the registry or both players stay "already in a match"
        instance.getMatchManager().unregister(this);
        // end match logic
    }

    public boolean hasPlayer(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (player1.equals(uuid)) {
            return player2;
        }
        if (player2.equals(uuid)) {
            return player1;
        }
        return null;
    }

    public UUID getWinner() {
        return winner;
    }


    public boolean isSettingUpRound() {
        return settingUpRound;
    }

    public boolean isCelebrating() {
        return celebrating;
    }

    public MatchState getMatchState() {
        return matchState;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }
}
