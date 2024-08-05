package org.figuramc.figura.server;

import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.users.LoadPlayerDataEvent;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// TODO: Make FiguraUserManager also use CompletedFutures
public final class FiguraUserManager {
    private final FiguraServer parent;
    private final HashMap<UUID, FiguraUser> users = new HashMap<>();
    private final LinkedList<UUID> expectedUsers = new LinkedList<>();

    public FiguraUserManager(FiguraServer parent) {
        this.parent = parent;
    }

    public FiguraUser getUserOrNull(UUID playerUUID) {
        return users.get(playerUUID);
    }

    public void onUserJoin(UUID player) {
        parent.sendHandshake(player);
    }

    public FiguraUser getUser(UUID player) {
        return users.computeIfAbsent(player, (p) -> loadPlayerData(player));
    }

    public void setupOnlinePlayer(UUID uuid, boolean allowPings, boolean allowAvatars, int s2cChunkSize) {
        FiguraUser user = getUser(uuid);
        user.setOnline();
        user.update(allowPings, allowAvatars, s2cChunkSize);
        expectedUsers.remove(uuid); // This is called either way just to remove it in case if it was first time initialization
    }


    private FiguraUser loadPlayerData(UUID player) {
        LoadPlayerDataEvent playerDataEvent = Events.call(new LoadPlayerDataEvent(player));
        if (playerDataEvent.returned()) return playerDataEvent.returnValue();
        Path dataFile = parent.getUserdataFile(player);
        return FiguraUser.load(player, dataFile);
    }

    public void onUserLeave(UUID player) {
        users.computeIfPresent(player, (uuid, pl) -> {
            pl.save(parent.getUserdataFile(pl.player()));
            pl.setOffline();
            return pl;
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

    public boolean isExpected(UUID user) {
        return expectedUsers.contains(user);
    }
}
