package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CProtocolVersion;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CProtocolVersionHandler implements S2CPacketHandler<S2CProtocolVersion> {
    @Override
    public S2CProtocolVersion serialize(IFriendlyByteBuf byteBuf) {
        return new S2CProtocolVersion(byteBuf);
    }

    @Override
    public void handle(S2CProtocolVersion packet) {
        FSB.instance().handleVersion(packet.version());
    }
}
