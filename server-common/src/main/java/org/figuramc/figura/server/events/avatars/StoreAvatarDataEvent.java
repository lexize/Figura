package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;

import java.util.UUID;

public class StoreAvatarDataEvent extends CancellableEvent {
    private final UUID avatarUploader;
    private final byte[] avatarData;
    private final byte[] avatarHash;
    private final byte[] avatarEHash;
    private final String avatarId;

    public StoreAvatarDataEvent(UUID avatarUploader, byte[] avatarData, byte[] avatarHash, byte[] avatarEHash, String avatarId) {
        this.avatarUploader = avatarUploader;
        this.avatarData = avatarData;
        this.avatarHash = avatarHash;
        this.avatarEHash = avatarEHash;
        this.avatarId = avatarId;
    }

    public UUID avatarUploader() {
        return avatarUploader;
    }

    public byte[] avatarData() {
        return avatarData;
    }

    public byte[] avatarHash() {
        return avatarHash;
    }

    public byte[] avatarEHash() {
        return avatarEHash;
    }

    public String avatarId() {
        return avatarId;
    }
}
