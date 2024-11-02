package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class S2CProtocolVersion implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/version");

    private final int version;

    public S2CProtocolVersion(int version) {
        this.version = version;
    }

    public S2CProtocolVersion(IFriendlyByteBuf buf) {
        version = buf.readInt();
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(version);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }

    public int version() {
        return version;
    }
}
