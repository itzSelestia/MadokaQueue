package dev.yae.madokaQueue.game;

import dev.yae.madokaQueue.MadokaQueue;
import dev.yae.madokaQueue.game.gamemode.Gamemode;
import dev.yae.madokaQueue.game.gamemode.VanillaGamemode;
import dev.yae.madokaQueue.ui.MatchHud;
import dev.yae.madokaQueue.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
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
    private final int firstRoundCountdown = 10;
    private final int victoryCountdown = 3;
    private final int corpseLifetime = 2;
    private static final int RTP_RANGE = 5000;
    private static final int RTP_MIN_Y = 60;
    private static final int RTP_MAX_Y = 250;
    private static final int RTP_ATTEMPTS = 60;
    private static final Set<Material> RTP_UNSAFE = EnumSet.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS);
    private static final Random RTP_RANDOM = new Random();
    private final MadokaQueue instance = MadokaQueue.getInstance();
    private final MatchHud hud;
    private BukkitTask countdownTask;
    private BukkitTask celebrationTask;
    private ItemStack[] player1Layout = null;
    private ItemStack[] player2Layout = null;

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
        if (gamemode instanceof VanillaGamemode) {
            return;
        }

        if (uuid.equals(player1) && player1Layout != null) {
            assert player != null;
            player.getInventory().setContents(deepClone(player1Layout));
            player.updateInventory();
            return;
        }
        else if (uuid.equals(player2) && player2Layout != null) {
            assert player != null;
            player.getInventory().setContents(deepClone(player2Layout));
            player.updateInventory();
            return;
        }

        if (player != null) {
            gamemode.equipKit(player);
        }
    }

    private ItemStack[] deepClone(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
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
        World world = instance.getRtpWorld();
        if (world == null) {
            return;
        }

        Location location = findSafeLocation(world);
        if (location == null) {
            location = world.getSpawnLocation();
        }

        teleport(player1, location);
        teleport(player2, location);
    }

    private void teleport(UUID uuid, Location location) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.teleport(location);
        }
    }

    private Location findSafeLocation(World world) {
        for (int attempt = 0; attempt < RTP_ATTEMPTS; attempt++) {
            int x = RTP_RANDOM.nextInt(RTP_RANGE * 2 + 1) - RTP_RANGE;
            int z = RTP_RANDOM.nextInt(RTP_RANGE * 2 + 1) - RTP_RANGE;
            int y = findSafeY(world, x, z);
            if (y != -1) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    private int findSafeY(World world, int x, int z) {
        int startY = Math.min(RTP_MAX_Y, world.getHighestBlockYAt(x, z));
        for (int y = startY; y >= RTP_MIN_Y; y--) {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (ground.getType().isSolid()
                    && feet.getType().isAir()
                    && head.getType().isAir()
                    && !RTP_UNSAFE.contains(ground.getType())) {
                return y;
            }
        }
        return -1;
    }

    private void startCountDown() {
        int seconds = currentRound == 1 ? firstRoundCountdown : countdown;
        countdownTask = new BukkitRunnable() {
            int secondsLeft = seconds;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    if (currentRound == 1) {
                        player1Layout = getPlayerLayout(player1);
                        player2Layout = getPlayerLayout(player2);
                    }
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

    private ItemStack[] getPlayerLayout(UUID player) {
        Player player1 = Bukkit.getPlayer(player);
        if (player1 == null) {
            return null;
        }
        return deepClone(player1.getInventory().getContents());
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
        instance.getMatchManager().unregister(this);
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
