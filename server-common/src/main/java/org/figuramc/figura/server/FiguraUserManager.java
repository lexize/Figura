package org.figuramc.figura.server;

import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.LoadPlayerDataEvent;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public final class FiguraUserManager {
    private final FiguraServer parent;
    private final HashMap<UUID, FiguraUser> users = new HashMap<>();

    public FiguraUserManager(FiguraServer parent) {
        this.parent = parent;
    }

    public FiguraUser getUser(UUID playerUUID) {
        return users.get(playerUUID);
    }

    public void onPlayerJoin(UUID player) {
        parent.sendPacket(player, new S2CBackendHandshakePacket(
                parent.config().pings(),
                parent.config().pingsRateLimit(),
                parent.config().pingsSizeLimit(),
                parent.config().avatars(),
                parent.config().avatarSizeLimit(),
                parent.config().avatarsCountLimit()
        ));
    }

    public FiguraUser authorisePlayer(UUID player, boolean allowPings, boolean allowAvatars, int s2cChunkSize) {
        return users.computeIfAbsent(player, (k) -> loadPlayerData(k, allowPings, allowAvatars, s2cChunkSize));
    }

    private FiguraUser loadPlayerData(UUID player, boolean allowPings, boolean allowAvatars, int s2cChunkSize) {
        LoadPlayerDataEvent playerDataEvent = Events.call(new LoadPlayerDataEvent(player));
        if (playerDataEvent.returned()) return playerDataEvent.returnValue();
        Path dataFile = parent.getUserdataFile(player);
        return FiguraUser.load(player, allowPings, allowAvatars, s2cChunkSize, dataFile);
    }

    public void onPlayerLeave(UUID player) {
        users.computeIfPresent(player, (uuid, pl) -> {
            pl.save(parent.getUserdataFile(pl.player()));
            return null;
        });
    }

    public void close() {
        for (FiguraUser pl: users.values()) {
            pl.save(parent.getUserdataFile(pl.player()));
        }
        users.clear();
    }
}
