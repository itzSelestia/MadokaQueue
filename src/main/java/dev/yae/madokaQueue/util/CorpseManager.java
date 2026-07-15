package dev.yae.madokaQueue.util;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import dev.yae.madokaQueue.MadokaQueue;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class CorpseManager {
    private static final AtomicInteger entityIds = new AtomicInteger(Integer.MAX_VALUE - 10_000);
    private static final Map<UUID, Corpse> corpses = new HashMap<>();

    private static final int SKIN_LAYERS_INDEX = 16;
    private static final byte ALL_SKIN_LAYERS = 0x7F;

    private record Corpse(int entityId, UUID profileId, String name, BukkitTask expiry) {
    }

    public static void spawn(UUID uuid, int secondsAlive) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        remove(uuid);

        int entityId = entityIds.decrementAndGet();
        UUID profileId = UUID.randomUUID();
        String name = profileId.toString().substring(0, 16);

        List<TextureProperty> skin = skinOf(player);
        org.bukkit.Location at = player.getLocation();

        broadcast(() -> hideNameTag(name));
        broadcast(() -> addToTab(new UserProfile(profileId, name, skin)));
        broadcast(() -> spawnCorpse(entityId, profileId, at));
        broadcast(() -> showSkinLayers(entityId));

        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(
                MadokaQueue.getInstance(),
                () -> remove(uuid),
                secondsAlive * 20L);

        corpses.put(uuid, new Corpse(entityId, profileId, name, expiry));
    }

    public static void remove(UUID uuid) {
        Corpse corpse = corpses.remove(uuid);
        if (corpse == null) {
            return;
        }

        corpse.expiry().cancel();
        despawn(corpse);
    }

    public static void removeAll() {
        for (Corpse corpse : corpses.values()) {
            corpse.expiry().cancel();
            despawn(corpse);
        }

        corpses.clear();
    }

    private static void despawn(Corpse corpse) {
        broadcast(() -> new WrapperPlayServerDestroyEntities(corpse.entityId()));
        broadcast(() -> new WrapperPlayServerPlayerInfoRemove(corpse.profileId()));
        broadcast(() -> new WrapperPlayServerTeams(
                corpse.name(),
                WrapperPlayServerTeams.TeamMode.REMOVE,
                (WrapperPlayServerTeams.ScoreBoardTeamInfo) null));
    }

    private static WrapperPlayServerTeams hideNameTag(String name) {
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(),
                null,
                null,
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                NamedTextColor.WHITE,
                WrapperPlayServerTeams.OptionData.NONE);

        return new WrapperPlayServerTeams(name, WrapperPlayServerTeams.TeamMode.CREATE, info, name);
    }

    private static WrapperPlayServerPlayerInfoUpdate addToTab(UserProfile profile) {
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(profile);
        info.setGameMode(GameMode.SURVIVAL);
        info.setLatency(0);
        info.setListed(false);

        return new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                        WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
                info);
    }

    private static WrapperPlayServerSpawnEntity spawnCorpse(int entityId, UUID profileId, org.bukkit.Location at) {
        Location location = new Location(
                new Vector3d(at.getX(), at.getY(), at.getZ()),
                at.getYaw(),
                at.getPitch());

        return new WrapperPlayServerSpawnEntity(
                entityId,
                profileId,
                EntityTypes.PLAYER,
                location,
                at.getYaw(),
                0,
                null);
    }

    private static WrapperPlayServerEntityMetadata showSkinLayers(int entityId) {
        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(SKIN_LAYERS_INDEX, EntityDataTypes.BYTE, ALL_SKIN_LAYERS));

        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }

    private static ItemStack toPacketItem(org.bukkit.inventory.ItemStack item) {
        if (item == null) {
            return ItemStack.EMPTY;
        }

        return SpigotConversionUtil.fromBukkitItemStack(item);
    }

    private static List<TextureProperty> skinOf(Player player) {
        List<TextureProperty> textures = new ArrayList<>();
        for (ProfileProperty property : player.getPlayerProfile().getProperties()) {
            textures.add(new TextureProperty(
                    property.getName(),
                    property.getValue(),
                    property.getSignature()));
        }

        return textures;
    }

    private static void broadcast(Supplier<PacketWrapper<?>> packet) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(online, packet.get());
        }
    }
}
