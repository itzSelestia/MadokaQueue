package dev.yae.madokaQueue;

import dev.yae.madokaQueue.commands.DuelCommand;
import dev.yae.madokaQueue.game.listeners.DeathListener;
import dev.yae.madokaQueue.game.listeners.MatchListener;
import dev.yae.madokaQueue.game.MatchManager;
import dev.yae.madokaQueue.util.InvincibleManager;
import dev.yae.madokaQueue.util.SpectatorManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class MadokaQueue extends JavaPlugin {

    private final MatchManager matchManager = new MatchManager();

    public static MadokaQueue getInstance() {
        return instance;
    }

    private static MadokaQueue instance;

    @Override
    public void onEnable() {
        instance = this;

        getServer().getPluginManager().registerEvents(new DeathListener(matchManager), this);
        getServer().getPluginManager().registerEvents(new MatchListener(matchManager), this);
        getServer().getPluginManager().registerEvents(new InvincibleManager(), this);
        getServer().getPluginManager().registerEvents(new SpectatorManager(), this);

        DuelCommand duelCommand = new DuelCommand(matchManager);
        PluginCommand duel = Objects.requireNonNull(getCommand("duel"), "duel command missing from plugin.yml");
        duel.setExecutor(duelCommand);
        duel.setTabCompleter(duelCommand);
    }

    @Override
    public void onDisable() {
        // on /reload the players stay online, and their countdown tasks do not
        InvincibleManager.clearAll();
        SpectatorManager.restoreAll();
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }
}
