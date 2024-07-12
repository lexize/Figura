package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Packet sent to server to acknowledge that client allows server to work as Figura backend.
 */
public class C2SBackendHandshakePacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "handshake");

    private final boolean pings;
    private final boolean avatars;
    private final int incomingChunkSize;

    public C2SBackendHandshakePacket(boolean pings, boolean avatars, int incomingChunkSize) {
        this.pings = pings;
        this.avatars = avatars;
        this.incomingChunkSize = incomingChunkSize;
    }

    public C2SBackendHandshakePacket(IFriendlyByteBuf byteBuf) {
        this.pings = byteBuf.readByte() != 0;
        this.avatars = byteBuf.readByte() != 0;
        this.incomingChunkSize = this.avatars ? byteBuf.readInt() :  0;
    }

    /**
     * Does client allow server to send pings?
     * @return true if yes
     */
    public boolean pings() {
        return pings;
    }

    /**
     * Does client allow server to send userdata and avatars to it?
     * @return true if yes
     */
    public boolean avatars() {
        return avatars;
    }

    /**
     * Max incoming chunk size allowed by client, in case if it allows backend work with avatars.
     * @return Max incoming chunk size
     */
    public int maxAvatarChunkSize() {
        return incomingChunkSize;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeByte(pings ? 1 : 0);
        byteBuf.writeByte(avatars ? 1 : 0);
        if (avatars) {
            byteBuf.writeInt(incomingChunkSize);
        }
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}