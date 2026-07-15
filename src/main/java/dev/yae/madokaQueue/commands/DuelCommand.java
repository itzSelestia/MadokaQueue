package dev.yae.madokaQueue.commands;

import dev.yae.madokaQueue.MadokaQueue;
import dev.yae.madokaQueue.game.InviteManager;
import dev.yae.madokaQueue.game.Match;
import dev.yae.madokaQueue.game.MatchManager;
import dev.yae.madokaQueue.game.gamemode.Gamemode;
import dev.yae.madokaQueue.game.gamemode.Gamemodes;
import dev.yae.madokaQueue.ui.Palette;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private static final int DEFAULT_ROUNDS = 3;
    private static final int INVITE_SECONDS = 60;

    private final MatchManager matchManager;
    private final InviteManager inviteManager = new InviteManager();

    public DuelCommand(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Palette.warn("Only players can duel."));
            return true;
        }

        if (args.length < 1) {
            usage(player);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "accept" -> respond(player, args, true);
            case "decline" -> respond(player, args, false);
            default -> invite(player, args);
        };
    }

    private boolean invite(Player player, String[] args) {
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Palette.warn(args[0] + " is not online."));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(Palette.warn("You cannot duel yourself."));
            return true;
        }

        if (matchManager.getMatch(player.getUniqueId()) != null) {
            player.sendMessage(Palette.warn("You are already in a duel."));
            return true;
        }

        if (matchManager.getMatch(target.getUniqueId()) != null) {
            player.sendMessage(Palette.warn(target.getName() + " is already in a duel."));
            return true;
        }

        int rounds = DEFAULT_ROUNDS;
        Gamemode gamemode = Gamemodes.getDefault();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (isInteger(arg)) {
                Integer parsed = parseRounds(player, arg);
                if (parsed == null) {
                    return true;
                }
                rounds = parsed;
            } else {
                Gamemode found = Gamemodes.get(arg);
                if (found == null) {
                    player.sendMessage(Palette.warn("Unknown gamemode: " + arg
                            + ". Try: " + String.join(", ", Gamemodes.names())));
                    return true;
                }
                gamemode = found;
            }
        }

        if (inviteManager.get(player.getUniqueId(), target.getUniqueId()) != null) {
            return respond(player, new String[]{"accept", target.getName()}, true);
        }

        int finalRounds = rounds;
        Gamemode finalGamemode = gamemode;
        InviteManager.Invite invite = new InviteManager.Invite(
                player.getUniqueId(),
                target.getUniqueId(),
                rounds,
                gamemode,
                Bukkit.getScheduler().runTaskLater(
                        MadokaQueue.getInstance(),
                        () -> expire(player.getUniqueId(), target.getUniqueId()),
                        INVITE_SECONDS * 20L));
        inviteManager.add(invite);

        player.sendMessage(Palette.info(Component.text()
                .append(Component.text("Invited ", Palette.SNOW))
                .append(Component.text(target.getName(), Palette.PETAL, TextDecoration.BOLD))
                .append(Component.text(" to ", Palette.SNOW))
                .append(finalGamemode.displayName())
                .append(Component.text(", best of " + finalRounds + ".", Palette.SNOW))
                .build()));
        player.sendMessage(Palette.info(Component.text(
                "It expires in " + INVITE_SECONDS + " seconds.", Palette.SOFT)));

        sendInviteCard(target, player, finalRounds, finalGamemode);
        return true;
    }

    private void sendInviteCard(Player target, Player sender, int rounds, Gamemode gamemode) {
        target.sendMessage(Component.empty());
        target.sendMessage(Palette.petals(Component.text()
                .append(Component.text(sender.getName(), Palette.BLOSSOM, TextDecoration.BOLD))
                .append(Component.text(" challenges you", Palette.SNOW))
                .build()));
        target.sendMessage(Palette.info(Component.text()
                .append(gamemode.displayName())
                .append(Component.text("  ·  best of " + rounds, Palette.SOFT))
                .build()));

        Component accept = Component.text("[ ACCEPT ]", Palette.PETAL, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/duel accept " + sender.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Fight " + sender.getName(), Palette.SOFT)));

        Component decline = Component.text("[ DECLINE ]", Palette.SOFT)
                .clickEvent(ClickEvent.runCommand("/duel decline " + sender.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Turn it down", Palette.SOFT)));

        target.sendMessage(Component.text()
                .append(Component.text("   "))
                .append(accept)
                .append(Component.text("   "))
                .append(decline)
                .build());
        target.sendMessage(Component.empty());

        target.playSound(Sound.sound(
                Key.key("block.amethyst_block.chime"), Sound.Source.MASTER, 1.0f, 1.4f));
    }

    private boolean respond(Player player, String[] args, boolean accepted) {
        if (args.length < 2) {
            player.sendMessage(Palette.warn("Usage: /duel " + (accepted ? "accept" : "decline") + " <player>"));
            return true;
        }

        Player sender = Bukkit.getPlayerExact(args[1]);
        if (sender == null) {
            player.sendMessage(Palette.warn(args[1] + " is not online."));
            return true;
        }

        InviteManager.Invite invite = inviteManager.get(player.getUniqueId(), sender.getUniqueId());
        if (invite == null) {
            player.sendMessage(Palette.warn(sender.getName() + " has not invited you."));
            return true;
        }

        inviteManager.remove(player.getUniqueId(), sender.getUniqueId());

        if (!accepted) {
            player.sendMessage(Palette.info(Component.text("Declined.", Palette.SOFT)));
            sender.sendMessage(Palette.info(Component.text()
                    .append(Component.text(player.getName(), Palette.PETAL, TextDecoration.BOLD))
                    .append(Component.text(" declined your duel.", Palette.SNOW))
                    .build()));
            return true;
        }

        if (matchManager.getMatch(player.getUniqueId()) != null
                || matchManager.getMatch(sender.getUniqueId()) != null) {
            player.sendMessage(Palette.warn("One of you is already in a duel."));
            return true;
        }

        inviteManager.clear(player.getUniqueId());
        inviteManager.clear(sender.getUniqueId());

        Match match = new Match(sender.getUniqueId(), player.getUniqueId(), invite.rounds(), invite.gamemode());
        matchManager.register(match);
        match.startMatch();
        return true;
    }

    private void expire(UUID senderId, UUID targetId) {
        if (inviteManager.get(targetId, senderId) == null) {
            return;
        }

        inviteManager.remove(targetId, senderId);

        Player sender = Bukkit.getPlayer(senderId);
        Player target = Bukkit.getPlayer(targetId);
        if (sender != null && target != null) {
            sender.sendMessage(Palette.info(Component.text(
                    "Your invite to " + target.getName() + " expired.", Palette.SOFT)));
        }
    }

    private boolean isInteger(String raw) {
        if (raw.isEmpty()) {
            return false;
        }
        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isDigit(raw.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private Integer parseRounds(Player player, String raw) {
        int rounds;
        try {
            rounds = Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            player.sendMessage(Palette.warn("Rounds must be a number."));
            return null;
        }

        if (rounds < 1 || rounds % 2 == 0) {
            player.sendMessage(Palette.warn("Rounds must be a positive odd number, so nobody draws."));
            return null;
        }

        return rounds;
    }

    private void usage(Player player) {
        player.sendMessage(Palette.petals(Component.text("Duel", Palette.BLOSSOM, TextDecoration.BOLD)));
        player.sendMessage(Palette.info(Component.text("/duel <player> [gamemode] [rounds]", Palette.SOFT)));
        player.sendMessage(Palette.info(Component.text("/duel accept <player>", Palette.SOFT)));
        player.sendMessage(Palette.info(Component.text("/duel decline <player>", Palette.SOFT)));
        player.sendMessage(Palette.info(Component.text(
                "modes: " + String.join(", ", Gamemodes.names()), Palette.SOFT)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("accept", "decline"));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    options.add(online.getName());
                }
            }
            return matching(options, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean responding = sub.equals("accept") || sub.equals("decline");

        if (args.length == 2 && responding) {
            List<String> inviters = new ArrayList<>();
            for (UUID senderId : inviteManager.invitesFor(player.getUniqueId()).keySet()) {
                Player inviter = Bukkit.getPlayer(senderId);
                if (inviter != null) {
                    inviters.add(inviter.getName());
                }
            }
            return matching(inviters, args[1]);
        }

        if ((args.length == 2 || args.length == 3) && !responding) {
            List<String> options = new ArrayList<>(Gamemodes.names());
            options.addAll(List.of("1", "3", "5"));
            return matching(options, args[args.length - 1]);
        }

        return List.of();
    }

    private List<String> matching(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
