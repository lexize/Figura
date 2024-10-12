package org.figuramc.figura.server;

public final class FiguraServerConfig {
    private int pingsRateLimit = 32;
    private int pingsSizeLimit = 1024;

    private int avatarSizeLimit = 102400;
    private int avatarsCountLimit = 1;

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
}
