package dev.yae.madokaQueue.game;

import dev.yae.madokaQueue.MadokaQueue;
import dev.yae.madokaQueue.util.InvincibleManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class Match {
    private final UUID player1;
    private final UUID player2;
    private final int rounds;
    private int currentRound = 1;
    private int player1score = 0;
    private int player2score = 0;
    private MatchState matchState = MatchState.WAITNG;
    private UUID winner;
    private boolean settingUpRound = false;
    private final int countdown = 5;
    private final MadokaQueue instance = MadokaQueue.getInstance();
    private BukkitTask countdownTask;

    public Match(UUID player1, UUID player2, int rounds) {
        if (rounds < 1 || rounds % 2 == 0) {
            throw new IllegalArgumentException("Invalid rounds amount");
        }
        this.player1 = player1;
        this.player2 = player2;
        this.rounds = rounds;
    }

    public void startMatch() {
        if (matchState != MatchState.WAITNG) {
            return;
        }
        matchState = MatchState.IN_PROGRESS;
        startRound();
    }

    private void startRound() {
        cancelCountDown();
        settingUpRound = true;
        rtp();
        // equip kit
        System.out.println("KIT");
        setGodMode(true);
        startCountDown();
    }


    public void onPlayerDeath(UUID deadPlayer) {
        // a death during the countdown is not a lost round, and must not start a second one
        if (matchState != MatchState.IN_PROGRESS || settingUpRound || !hasPlayer(deadPlayer)) {
            return;
        }

        if (deadPlayer.equals(player1)) {
            player2score++;
        } else {
            player1score++;
        }

        currentRound++;

        if (isMatchOver()) {
            endMatch();
        } else {
            startRound();
        }
    }


    public void forfeit(UUID offender) {
        if (matchState == MatchState.ENDED || !hasPlayer(offender)) {
            return;
        }
        winner = getOpponent(offender);
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
        countdownTask = new BukkitRunnable() {
            int secondsLeft = countdown;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    cancel();
                    countdownTask = null;
                    setGodMode(false);
                    settingUpRound = false;
                    return;
                }

                System.out.println(secondsLeft);

                secondsLeft--;
            }
        }.runTaskTimer(instance, 0L, 20L);

    }

    // a countdown left running after the match ends would flip god mode back on later
    private void cancelCountDown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void setGodMode(boolean invincible) {
        InvincibleManager.setInvincible(player1, invincible);
        InvincibleManager.setInvincible(player2, invincible);
    }

    private void endMatch() {
        if (matchState == MatchState.ENDED) {
            return;
        }
        matchState = MatchState.ENDED;
        cancelCountDown();
        setGodMode(false);
        settingUpRound = false;
        if (winner == null) {
            winner = player1score > player2score ? player1 : player2;
        }
        System.out.println(winner);
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
