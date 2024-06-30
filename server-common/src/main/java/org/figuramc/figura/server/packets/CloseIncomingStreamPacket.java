package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Closes incoming stream opened by other side. Same on client and server side.
 */
public class CloseIncomingStreamPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "stream/close_in");

    private final int streamId;

    public CloseIncomingStreamPacket(int streamId) {
        this.streamId = streamId;
    }

    public CloseIncomingStreamPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
