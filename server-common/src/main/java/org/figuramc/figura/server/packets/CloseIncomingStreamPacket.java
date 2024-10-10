package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Closes incoming stream opened by other side. Same on client and server side.
 */
public class CloseIncomingStreamPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "stream/close_in");

    private final int streamId;
    /* KNOWN CODES
        100: Successful - Avatar finished uploading
        101: Successful - Avatar already exists

        301: Error - Invalid stream id
        302: Error - Max avatar size exceeded
        303: Error - Invalid hash
     */
    private final short code;

    public CloseIncomingStreamPacket(int streamId, short code) {
        this.streamId = streamId;
        this.code = code;
    }

    public CloseIncomingStreamPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
        code = buf.readShort();
    }

    public int streamId() {
        return streamId;
    }

    public short code() {
        return code;
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
