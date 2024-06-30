package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class S2COwnedAvatarsPacket implements Packet {
    @Override
    public void write(IFriendlyByteBuf buf) {

    }

    @Override
    public Identifier getId() {
        return null;
    }
}
