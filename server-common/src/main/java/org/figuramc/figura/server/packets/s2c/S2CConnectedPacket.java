package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.UUID;

public class S2CConnectedPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "connected");

    private final UUID user;

    public S2CConnectedPacket(UUID user) {
        this.user = user;
    }

    public S2CConnectedPacket(IFriendlyByteBuf buf) {
        this.user = buf.readUUID();
    }


    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeUUID(user);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }

    public UUID user() {
        return user;
    }
}
