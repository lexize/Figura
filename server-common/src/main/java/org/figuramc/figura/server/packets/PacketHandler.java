package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public interface PacketHandler<P extends Packet> {
    P serialize(IFriendlyByteBuf byteBuf);
    void handle(P packet);
}
