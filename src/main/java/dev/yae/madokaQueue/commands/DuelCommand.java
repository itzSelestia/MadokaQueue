package dev.yae.madokaQueue.commands;

import dev.yae.madokaQueue.game.Match;
import dev.yae.madokaQueue.game.MatchManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private static final int DEFAULT_ROUNDS = 3;

    private final MatchManager matchManager;

    public DuelCommand(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can start a duel.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            return false;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text(args[0] + " is not online.", NamedTextColor.RED));
            return true;
        }

        // two identical uuids would collide in the match registry
        if (target.equals(player)) {
            sender.sendMessage(Component.text("You cannot duel yourself.", NamedTextColor.RED));
            return true;
        }

        if (matchManager.getMatch(player.getUniqueId()) != null) {
            sender.sendMessage(Component.text("You are already in a match.", NamedTextColor.RED));
            return true;
        }

        if (matchManager.getMatch(target.getUniqueId()) != null) {
            sender.sendMessage(Component.text(target.getName() + " is already in a match.", NamedTextColor.RED));
            return true;
        }

        int rounds = DEFAULT_ROUNDS;
        if (args.length >= 2) {
            try {
                rounds = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                sender.sendMessage(Component.text("Rounds must be a number.", NamedTextColor.RED));
                return true;
            }

            if (rounds < 1 || rounds % 2 == 0) {
                sender.sendMessage(Component.text("Rounds must be a positive odd number.", NamedTextColor.RED));
                return true;
            }
        }

        Match match = new Match(player.getUniqueId(), target.getUniqueId(), rounds);
        matchManager.register(match);
        match.startMatch();

        Component message = Component.text(
                player.getName() + " vs " + target.getName() + " - best of " + rounds,
                NamedTextColor.GREEN);
        player.sendMessage(message);
        target.sendMessage(message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(sender.getName()))
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
