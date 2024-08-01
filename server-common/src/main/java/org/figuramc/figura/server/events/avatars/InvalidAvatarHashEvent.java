package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.Event;

import java.util.List;
import java.util.UUID;

public class InvalidAvatarHashEvent extends Event {
    private final byte[] expectedHash;
    private final byte[] receivedHash;
    private final List<UUID> awaitingUsers;

    public InvalidAvatarHashEvent(byte[] expectedHash, byte[] receivedHash, List<UUID> awaitingUsers) {
        this.expectedHash = expectedHash;
        this.receivedHash = receivedHash;
        this.awaitingUsers = awaitingUsers;
    }

    public byte[] expectedHash() {
        return expectedHash;
    }

    public byte[] receivedHash() {
        return receivedHash;
    }

    public List<UUID> awaitingUsers() {
        return awaitingUsers;
    }
}
