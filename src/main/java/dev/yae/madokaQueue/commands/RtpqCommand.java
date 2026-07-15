package dev.yae.madokaQueue.commands;

import dev.yae.madokaQueue.game.gamemode.Gamemode;
import dev.yae.madokaQueue.game.gamemode.Gamemodes;
import dev.yae.madokaQueue.ui.Palette;
import dev.yae.madokaQueue.util.QueueManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class RtpqCommand implements CommandExecutor, TabCompleter {
    private final QueueManager queueManager;

    public RtpqCommand(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Palette.warn("Players only."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Palette.info(Component.text("/rtpq <gamemode>", Palette.SOFT)));
            player.sendMessage(Palette.info(Component.text(
                    "modes: " + String.join(", ", Gamemodes.names()), Palette.SOFT)));
            return true;
        }

        Gamemode gamemode = Gamemodes.get(args[0]);
        if (gamemode == null) {
            player.sendMessage(Palette.warn("Unknown gamemode: " + args[0]));
            return true;
        }

        if (queueManager.addToQueue(gamemode, player.getUniqueId())) {
            player.sendMessage(Palette.info(Component.text()
                    .append(Component.text("Queued for ", Palette.SNOW))
                    .append(gamemode.displayName())
                    .append(Component.text(". Waiting for an opponent...", Palette.SNOW))
                    .build()));
        } else {
            player.sendMessage(Palette.warn("You are already queued or in a duel."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Gamemodes.names().stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
