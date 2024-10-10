package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Closes outcoming stream opened by this side. Same on client and server side.
 */
public class CloseOutcomingStreamPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "stream/close_out");
    private final int streamId;
    private final short code;

    public CloseOutcomingStreamPacket(int streamId, short code) {
        this.streamId = streamId;
        this.code = code;
    }

    public CloseOutcomingStreamPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
        code = buf.readShort();
    }

    public int streamId() {
        return streamId;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
        buf.writeShort(code);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
