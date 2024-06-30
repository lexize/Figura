package org.figuramc.figura.server.events;

import java.util.concurrent.CompletableFuture;

public class StartLoadingAvatarEvent extends ReturnableEvent<CompletableFuture<byte[]>> {
    private final byte[] hash;
    public StartLoadingAvatarEvent(byte[] hash) {
        this.hash = hash;
    }

    public byte[] hash() {
        return hash;
    }
}
