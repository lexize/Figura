package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.avatars.FiguraServerAvatarManager;
import org.figuramc.figura.server.events.CancellableEvent;

public class StoreAvatarMetadataEvent extends CancellableEvent {
    private final byte[] hash;
    private final FiguraServerAvatarManager.AvatarMetadata metadata;

    public StoreAvatarMetadataEvent(byte[] hash, FiguraServerAvatarManager.AvatarMetadata metadata) {
        this.hash = hash;
        this.metadata = metadata;
    }

    public byte[] hash() {
        return hash;
    }

    public FiguraServerAvatarManager.AvatarMetadata metadata() {
        return metadata;
    }
}
