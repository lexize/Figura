package org.figuramc.figura.server.exceptions;

import org.figuramc.figura.server.utils.Utils;

public class HashNotMatchingException extends RuntimeException {
    private final byte[] expectedHash;
    private final byte[] actualHash;

    public HashNotMatchingException(byte[] expectedHash, byte[] actualHash) {
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }

    @Override
    public String toString() {
        return "Expected hash %s for avatar data, got %s".formatted(
                Utils.hexFromBytes(expectedHash), Utils.hexFromBytes(actualHash));
    }

    public byte[] expectedHash() {
        return expectedHash;
    }

    public byte[] actualHash() {
        return actualHash;
    }
}
