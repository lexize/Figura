package org.figuramc.figura.server.events;

import org.figuramc.figura.server.avatars.FiguraServerAvatarManager;

public class AvatarDataGCEvent extends CancellableEvent {
    private final byte[] hash;

    public AvatarDataGCEvent(byte[] hash) {
        this.hash = hash;
    }

    public byte[] hash() {
        return hash;
    }
}
