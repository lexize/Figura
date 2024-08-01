package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class S2CConnected implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "handshake");

    @Override
    public void write(IFriendlyByteBuf buf) {

    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
