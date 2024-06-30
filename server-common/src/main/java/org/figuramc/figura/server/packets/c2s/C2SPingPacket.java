package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class C2SPingPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "ping");
    public static final int MAX_PING_SIZE = 1048576 - 5; // Max size of ping that is possible to send
    // in default minecraft client

    private final int id;
    private final boolean sync;
    private final byte[] data;

    public C2SPingPacket(int id, boolean sync, byte[] data) {
        this.id = id;
        this.sync = sync;
        this.data = data;
    }

    public C2SPingPacket(IFriendlyByteBuf buf) {
        this.id = buf.readInt();
        this.sync = buf.readByte() != 0;
        this.data = buf.readBytes();
    }

    public int id() {
        return id;
    }

    public boolean sync() {
        return sync;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeByte(sync ? 1 : 0);
        buf.writeBytes(data);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
