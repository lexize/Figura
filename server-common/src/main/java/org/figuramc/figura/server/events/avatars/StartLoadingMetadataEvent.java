package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.avatars.FiguraServerAvatarManager;
import org.figuramc.figura.server.events.ReturnableEvent;

import java.util.concurrent.CompletableFuture;

public class StartLoadingMetadataEvent extends ReturnableEvent<CompletableFuture<FiguraServerAvatarManager.AvatarMetadata>> {
    private final byte[] hash;

    public StartLoadingMetadataEvent(byte[] hash) {
        this.hash = hash;
    }

    public byte[] hash() {
        return hash;
    }
}
