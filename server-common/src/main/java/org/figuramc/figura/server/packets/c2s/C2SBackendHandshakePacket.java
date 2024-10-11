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

    public C2SBackendHandshakePacket(boolean pings, boolean avatars, int incomingChunkSize) {
        this.pings = pings;
        this.avatars = avatars;
    }

    public C2SBackendHandshakePacket(IFriendlyByteBuf byteBuf) {
        this.pings = byteBuf.readByte() != 0;
        this.avatars = byteBuf.readByte() != 0;
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

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeByte(pings ? 1 : 0);
        byteBuf.writeByte(avatars ? 1 : 0);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}