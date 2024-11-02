package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class C2SRequestVersion implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "c2s/version");
    @Override
    public void write(IFriendlyByteBuf buf) {

    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
