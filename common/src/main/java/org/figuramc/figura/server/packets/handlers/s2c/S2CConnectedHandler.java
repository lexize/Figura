package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CConnectedPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CConnectedHandler implements S2CPacketHandler<S2CConnectedPacket> {
    @Override
    public S2CConnectedPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CConnectedPacket();
    }

    @Override
    public void handle(S2CConnectedPacket packet) {
        FSB.handleConnection();
    }
}
