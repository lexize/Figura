package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public interface S2CPacketHandler<P extends Packet> {
    P serialize(IFriendlyByteBuf byteBuf);
    void handle(P packet);
}
