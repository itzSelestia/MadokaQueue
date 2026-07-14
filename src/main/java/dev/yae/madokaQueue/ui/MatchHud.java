package dev.yae.madokaQueue.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

// everything the two duellists see and hear. kept out of Match so the game logic stays readable
public class MatchHud {
    private static final TextColor PETAL = Palette.PETAL;
    private static final TextColor BLOSSOM = Palette.BLOSSOM;
    private static final TextColor SOFT = Palette.SOFT;
    private static final TextColor SNOW = Palette.SNOW;

    private static final String FLOWER = Palette.FLOWER;

    private static final Title.Times QUICK = Title.Times.times(
            Duration.ofMillis(150), Duration.ofMillis(900), Duration.ofMillis(350));
    private static final Title.Times LINGER = Title.Times.times(
            Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700));

    private final UUID player1;
    private final UUID player2;
    private final Component gamemode;

    public MatchHud(UUID player1, UUID player2, Component gamemode) {
        this.player1 = player1;
        this.player2 = player2;
        this.gamemode = gamemode;
    }

    public void matchStart(int rounds) {
        both(player -> {
            player.showTitle(Title.title(
                    petals(Component.text("DUEL", PETAL, TextDecoration.BOLD)),
                    Component.text(nameOf(player1) + "  vs  " + nameOf(player2), SNOW),
                    QUICK));
            player.sendMessage(Palette.info(Component.text()
                    .append(gamemode)
                    .append(Component.text("  ·  best of " + rounds, SOFT))
                    .build()));
            play(player, "block.amethyst_block.chime", 1.2f);
        });
    }

    public void roundStart(int round, int rounds) {
        both(player -> player.showTitle(Title.title(
                Component.text("Round " + round, PETAL, TextDecoration.BOLD),
                Component.text()
                        .append(gamemode)
                        .append(Component.text("  ·  of " + rounds, SOFT))
                        .build(),
                QUICK)));
    }

    // the pitch climbs as the count falls, so the last tick is the tensest
    public void countdown(int secondsLeft) {
        Component bar = Component.text()
                .append(Component.text(FLOWER + "  ", PETAL))
                .append(Component.text("starting in ", SNOW))
                .append(Component.text(secondsLeft, BLOSSOM, TextDecoration.BOLD))
                .append(Component.text("  " + FLOWER, PETAL))
                .build();

        both(player -> {
            player.sendActionBar(bar);
            play(player, "block.note_block.hat", 1.6f - (secondsLeft * 0.1f));
        });
    }

    public void fight() {
        both(player -> {
            player.showTitle(Title.title(
                    petals(Component.text("FIGHT", BLOSSOM, TextDecoration.BOLD)),
                    Component.empty(),
                    QUICK));
            player.sendActionBar(Component.empty());
            play(player, "block.note_block.pling", 1.8f);
        });
    }

    // vanilla never gets to print one, because the killing blow is cancelled
    public void deathMessage(Component message) {
        both(player -> player.sendMessage(message));
    }

    // eliminated is whoever just died, so the other one took the round
    public void roundOver(UUID eliminated, int player1score, int player2score) {
        UUID roundWinner = eliminated.equals(player1) ? player2 : player1;

        each(roundWinner, player -> {
            player.showTitle(Title.title(
                    petals(Component.text("ROUND WON", PETAL, TextDecoration.BOLD)),
                    score(roundWinner, player1score, player2score),
                    QUICK));
            play(player, "entity.player.levelup", 1.4f);
        });

        each(eliminated, player -> {
            player.showTitle(Title.title(
                    Component.text("ELIMINATED", SOFT, TextDecoration.BOLD),
                    score(eliminated, player1score, player2score),
                    QUICK));
            play(player, "block.note_block.bass", 0.7f);
        });
    }

    public void nextRoundIn(int secondsLeft) {
        Component bar = Component.text()
                .append(Component.text(FLOWER + "  ", PETAL))
                .append(Component.text("next round in ", SNOW))
                .append(Component.text(secondsLeft, BLOSSOM, TextDecoration.BOLD))
                .append(Component.text("  " + FLOWER, PETAL))
                .build();

        both(player -> player.sendActionBar(bar));
    }

    public void matchOver(UUID winner, int player1score, int player2score) {
        both(player -> player.sendActionBar(Component.empty()));
        broadcastResult(winner, player1score, player2score);

        each(winner, player -> {
            player.showTitle(Title.title(
                    petals(Component.text("VICTORY", PETAL, TextDecoration.BOLD)),
                    score(winner, player1score, player2score),
                    LINGER));
            play(player, "ui.toast.challenge_complete", 1.0f);
            play(player, "block.amethyst_block.chime", 1.5f);
        });

        UUID loser = winner.equals(player1) ? player2 : player1;
        each(loser, player -> {
            player.showTitle(Title.title(
                    Component.text("DEFEAT", SOFT, TextDecoration.BOLD),
                    score(loser, player1score, player2score),
                    LINGER));
            play(player, "block.note_block.bass", 0.6f);
        });
    }

    // the whole server hears about a finished duel, not just the two who fought
    private void broadcastResult(UUID winner, int player1score, int player2score) {
        UUID loser = winner.equals(player1) ? player2 : player1;
        int winnerScore = Math.max(player1score, player2score);
        int loserScore = Math.min(player1score, player2score);

        Bukkit.broadcast(Palette.info(Component.text()
                .append(Component.text(nameOf(winner), PETAL, TextDecoration.BOLD))
                .append(Component.text(" defeated ", SNOW))
                .append(Component.text(nameOf(loser), SOFT, TextDecoration.BOLD))
                .append(Component.text("  " + winnerScore + " - " + loserScore + "  ·  ", SNOW))
                .append(gamemode)
                .build()));
    }

    public void forfeited(UUID offender) {
        UUID other = offender.equals(player1) ? player2 : player1;
        each(other, player -> player.sendMessage(
                petals(Component.text(nameOf(offender) + " left the duel", SOFT))));
    }

    // always from the reader's own point of view: your score first
    private Component score(UUID viewer, int player1score, int player2score) {
        int mine = viewer.equals(player1) ? player1score : player2score;
        int theirs = viewer.equals(player1) ? player2score : player1score;

        return Component.text()
                .append(Component.text(mine, PETAL, TextDecoration.BOLD))
                .append(Component.text("  -  ", SNOW))
                .append(Component.text(theirs, SNOW))
                .build();
    }

    private Component petals(Component text) {
        return Component.text()
                .append(Component.text(FLOWER + " ", PETAL))
                .append(text)
                .append(Component.text(" " + FLOWER, PETAL))
                .build();
    }

    private void play(Player player, String key, float pitch) {
        player.playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, 1.0f, pitch));
    }

    private String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? "?" : player.getName();
    }

    private void both(java.util.function.Consumer<Player> action) {
        each(player1, action);
        each(player2, action);
    }

    private void each(UUID uuid, java.util.function.Consumer<Player> action) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            action.accept(player);
        }
    }
}
