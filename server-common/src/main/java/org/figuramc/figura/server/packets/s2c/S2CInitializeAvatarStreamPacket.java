package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class S2CInitializeAvatarStreamPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "stream/init");

    private final int streamId;
    private final byte[] ehash;

    public S2CInitializeAvatarStreamPacket(int streamId, byte[] ehash) {
        this.streamId = streamId;
        if (ehash.length != 32) throw new IllegalArgumentException("Invalid ehash length");
        this.ehash = ehash;
    }

    public S2CInitializeAvatarStreamPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
        ehash = buf.readHash();
    }

    public int streamId() {
        return streamId;
    }

    public byte[] ehash() {
        return ehash;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
        buf.writeBytes(ehash);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
