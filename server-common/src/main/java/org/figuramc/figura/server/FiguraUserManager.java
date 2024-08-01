package org.figuramc.figura.server;

import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.users.LoadPlayerDataEvent;
import org.figuramc.figura.server.packets.s2c.S2CConnected;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public final class FiguraUserManager {
    private final FiguraServer parent;
    private final HashMap<UUID, FiguraUser> users = new HashMap<>();
    private final LinkedList<UUID> expectedUsers = new LinkedList<>();

    public FiguraUserManager(FiguraServer parent) {
        this.parent = parent;
    }

    public FiguraUser getUser(UUID playerUUID) {
        return users.get(playerUUID);
    }

    public void onPlayerJoin(UUID player) {
        parent.sendHandshake(player);
    }

    public void updateOrAuthPlayer(UUID player, boolean allowPings, boolean allowAvatars, int s2cChunkSize) {
        users.compute(player, (k, p) -> {
            if (p != null) {
                p.update(allowPings, allowAvatars, s2cChunkSize);
                return p;
            } else if (expectedUsers.contains(player)) {
                FiguraUser user = loadPlayerData(player, allowPings, allowAvatars, s2cChunkSize);
                expectedUsers.remove(player);
                return user;
            }
            return null;
        });
        users.computeIfPresent(player, (k, p) -> {
            p.sendPacket(new S2CConnected());
            return p;
        });
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

    public void expect(UUID user) {
        if (!expectedUsers.contains(user)) {
            expectedUsers.add(user);
        }
    }
}
