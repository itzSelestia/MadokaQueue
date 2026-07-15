package dev.yae.madokaQueue.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class DeathMessages {
    private DeathMessages() {
    }

    public static Component of(Player victim, EntityDamageEvent event) {
        Player killer = killerOf(event);
        if (killer != null) {
            String verb = event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE
                    ? " was shot by "
                    : " was slain by ";

            return line(Component.text()
                    .append(name(victim.getName(), Palette.SOFT))
                    .append(Component.text(verb, Palette.SNOW))
                    .append(name(killer.getName(), Palette.PETAL))
                    .build());
        }

        return line(Component.text()
                .append(name(victim.getName(), Palette.SOFT))
                .append(Component.text(" " + describe(event.getCause()), Palette.SNOW))
                .build());
    }

    private static String describe(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FALL -> "fell from a high place";
            case FIRE, FIRE_TICK, CAMPFIRE -> "burned to death";
            case LAVA -> "tried to swim in lava";
            case DROWNING -> "drowned";
            case VOID -> "fell out of the world";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "blew up";
            case SUFFOCATION -> "suffocated";
            case STARVATION -> "starved to death";
            case MAGIC, POISON, WITHER -> "withered away";
            case FLY_INTO_WALL -> "experienced kinetic energy";
            case HOT_FLOOR -> "discovered the floor was lava";
            case FREEZE -> "froze to death";
            default -> "died";
        };
    }

    private static Player killerOf(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return null;
        }

        if (byEntity.getDamager() instanceof Player player) {
            return player;
        }

        if (byEntity.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    private static Component name(String name, net.kyori.adventure.text.format.TextColor color) {
        return Component.text(name, color, TextDecoration.BOLD);
    }

    private static Component line(Component body) {
        return Palette.prefix().append(body);
    }
}
