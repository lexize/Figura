package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class S2CBackendHandshakePacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "handshake");

    private final boolean pings;
    private final int pingsRateLimit;
    private final int pingsSizeLimit;

    private final boolean avatars;
    private final int maxAvatarSize;
    private final int maxAvatarsCount;

    public S2CBackendHandshakePacket(boolean pings, int pingsRateLimit, int pingsSizeLimit, boolean avatars, int maxAvatarSize, int maxAvatarsCount) {
        this.pings = pings;
        this.pingsRateLimit = pingsRateLimit;
        this.pingsSizeLimit = pingsSizeLimit;
        this.avatars = avatars;
        this.maxAvatarSize = maxAvatarSize;
        this.maxAvatarsCount = maxAvatarsCount;
    }

    public S2CBackendHandshakePacket(IFriendlyByteBuf source) {
        pings = source.readByte() != 0;
        if (pings) {
            pingsRateLimit = source.readInt();
            pingsSizeLimit = source.readInt();
        }
        else {
            pingsRateLimit = 0;
            pingsSizeLimit = 0;
        }

        avatars = source.readByte() != 0;
        if (avatars) {
            maxAvatarSize = source.readInt();
            maxAvatarsCount = source.readInt();
        }
        else {
            maxAvatarSize = 0;
            maxAvatarsCount = 0;
        }
    }

    public boolean pings() {
        return pings;
    }

    public int pingsRateLimit() {
        return pingsRateLimit;
    }

    public int pingsSizeLimit() {
        return pingsSizeLimit;
    }

    public boolean avatars() {
        return avatars;
    }

    public int maxAvatarSize() {
        return maxAvatarSize;
    }

    public int maxAvatarsCount() {
        return maxAvatarsCount;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        if (pings) {
            byteBuf.writeByte(1);
            byteBuf.writeInt(pingsRateLimit);
            byteBuf.writeInt(pingsSizeLimit);
        }
        else byteBuf.writeByte(0);

        if (avatars) {
            byteBuf.writeByte(1);
            byteBuf.writeInt(maxAvatarSize);
            byteBuf.writeInt(maxAvatarsCount);
        }
        else byteBuf.writeByte(0);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
