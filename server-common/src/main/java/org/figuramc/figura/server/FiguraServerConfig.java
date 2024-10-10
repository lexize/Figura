package org.figuramc.figura.server;

public final class FiguraServerConfig {
    private boolean pings = true;
    private boolean avatars = true;

    private int pingsRateLimit = 20;
    private int pingsSizeLimit = 10240;

    private int avatarSizeLimit = 102400;
    private int avatarsCountLimit = 1;

    private int maxOutcomingChunkSize = 50000;

    public boolean pings() {
        return pings;
    }

    public boolean avatars() {
        return avatars;
    }

    public int pingsRateLimit() {
        return pingsRateLimit;
    }

    public int pingsSizeLimit() {
        return pingsSizeLimit;
    }

    public int avatarSizeLimit() {
        return avatarSizeLimit;
    }

    public int avatarsCountLimit() {
        return avatarsCountLimit;
    }

    public int s2cChunkSize() {
        return maxOutcomingChunkSize;
    }
}
