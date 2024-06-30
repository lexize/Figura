package org.figuramc.figura.server.events;

import org.figuramc.figura.server.FiguraUser;

import java.util.UUID;

public class LoadPlayerDataEvent extends ReturnableEvent<FiguraUser> {
    private final UUID player;

    public LoadPlayerDataEvent(UUID player) {
        this.player = player;
    }

    public UUID player() {
        return player;
    }
}
