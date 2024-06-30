package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Closes outcoming stream opened by this side. Same on client and server side.
 */
public class CloseOutcomingStreamPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "stream/close_out");
    private final int streamId;

    public CloseOutcomingStreamPacket(int streamId) {
        this.streamId = streamId;
    }

    public CloseOutcomingStreamPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
    }

    public int streamId() {
        return streamId;
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
