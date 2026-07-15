package dev.yae.madokaQueue;

import dev.yae.madokaQueue.commands.DuelCommand;
import dev.yae.madokaQueue.commands.RtpqCommand;
import dev.yae.madokaQueue.game.listeners.DeathListener;
import dev.yae.madokaQueue.game.listeners.MatchListener;
import dev.yae.madokaQueue.game.MatchManager;
import com.github.retrooper.packetevents.PacketEvents;
import dev.yae.madokaQueue.util.CorpseManager;
import dev.yae.madokaQueue.util.InvincibleManager;
import dev.yae.madokaQueue.util.QueueManager;
import dev.yae.madokaQueue.util.SpectatorManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class MadokaQueue extends JavaPlugin {

    private final MatchManager matchManager = new MatchManager();

    public static MadokaQueue getInstance() {
        return instance;
    }

    private static MadokaQueue instance;

    private QueueManager queueManager = new QueueManager(matchManager);

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        PacketEvents.getAPI().init();

        getServer().getPluginManager().registerEvents(new DeathListener(matchManager), this);
        getServer().getPluginManager().registerEvents(new MatchListener(matchManager), this);
        getServer().getPluginManager().registerEvents(new InvincibleManager(), this);
        getServer().getPluginManager().registerEvents(new SpectatorManager(), this);
        getServer().getPluginManager().registerEvents(queueManager, this);
        queueManager.start();

        DuelCommand duelCommand = new DuelCommand(matchManager);
        PluginCommand duel = Objects.requireNonNull(getCommand("duel"), "duel command missing from plugin.yml");
        duel.setExecutor(duelCommand);
        duel.setTabCompleter(duelCommand);

        RtpqCommand rtpqCommand = new RtpqCommand(queueManager);
        PluginCommand rtpq = Objects.requireNonNull(getCommand("rtpq"), "rtpq command missing from plugin.yml");
        rtpq.setExecutor(rtpqCommand);
        rtpq.setTabCompleter(rtpqCommand);
    }

    @Override
    public void onDisable() {
        InvincibleManager.clearAll();
        SpectatorManager.restoreAll();
        CorpseManager.removeAll();
        queueManager.stop();

        PacketEvents.getAPI().terminate();
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public World getRtpWorld() {
        String name = getConfig().getString("rtp-world", "world");
        World world = getServer().getWorld(name);
        return world != null ? world : getServer().getWorlds().get(0);
    }
}
