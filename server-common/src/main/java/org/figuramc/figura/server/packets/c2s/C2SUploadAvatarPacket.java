package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import static java.nio.charset.StandardCharsets.UTF_8;

public class C2SUploadAvatarPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "avatars/upload");

    private final int streamId;
    private final String avatarId;
    private final byte[] ehash;

    public C2SUploadAvatarPacket(int streamId, String avatarId, byte[] ehash) {
        this.streamId = streamId;
        this.avatarId = avatarId;
        if (ehash.length != 32) throw new IllegalArgumentException("Invalid ehash length");
        this.ehash = ehash;
    }

    public C2SUploadAvatarPacket(IFriendlyByteBuf buf) {
        this.streamId = buf.readInt();
        this.avatarId = new String(buf.readByteArray(256), UTF_8);
        this.ehash = buf.readHash();
    }

    public int streamId() {
        return streamId;
    }

    public String avatarId() {
        return avatarId;
    }

    public byte[] ehash() {
        return ehash;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
        buf.writeByteArray(avatarId.getBytes(UTF_8));
        buf.writeBytes(ehash);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
