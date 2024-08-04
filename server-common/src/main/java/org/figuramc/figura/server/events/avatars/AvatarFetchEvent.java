package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;

public class AvatarFetchEvent extends CancellableEvent {
    private final byte[] hash;

    public AvatarFetchEvent(byte[] hash) {
        this.hash = hash;
    }
}
