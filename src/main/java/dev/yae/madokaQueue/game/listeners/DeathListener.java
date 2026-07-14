package dev.yae.madokaQueue.game.listeners;

import dev.yae.madokaQueue.game.Match;
import dev.yae.madokaQueue.game.MatchManager;
import dev.yae.madokaQueue.ui.DeathMessages;
import dev.yae.madokaQueue.ui.Palette;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class DeathListener implements Listener {
    private final MatchManager matchManager;

    public DeathListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    // players in a match never really die: the killing blow is cancelled and they go to spectator,
    // so there is no death screen, no respawn, no dropped items
    // HIGH + ignoreCancelled so the god mode check during the countdown gets to cancel first
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Match match = matchManager.getMatch(player.getUniqueId());
        if (match == null) {
            return;
        }

        if (player.getHealth() - event.getFinalDamage() > 0) {
            return;
        }

        // a totem is the player's own answer to a killing blow. cancelling the damage here would
        // swallow it silently -- vanilla only checks for a totem *after* this event -- so step
        // aside and let it pop: nobody dies, nobody scores, the round carries on
        if (holdsTotem(player)) {
            return;
        }

        event.setCancelled(true);
        player.setFireTicks(0);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());

        match.onPlayerDeath(player.getUniqueId(), DeathMessages.of(player, event));
        matchManager.cleanUp(match);
    }

    private boolean holdsTotem(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }

    // backstop for anything that skips the damage event, like /kill or setHealth(0)
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID dead = event.getEntity().getUniqueId();
        Match match = matchManager.getMatch(dead);
        if (match == null) {
            return;
        }

        Component message = event.deathMessage() == null
                ? Palette.info(event.getEntity().getName() + " died")
                : Palette.prefix().append(event.deathMessage());

        match.onPlayerDeath(dead, message);
        matchManager.cleanUp(match);
    }
}
