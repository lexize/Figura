package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.C2SRequestVersion;
import org.figuramc.figura.server.packets.s2c.S2CProtocolVersion;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.UUID;

public class C2SVersionRequestHandler implements C2SPacketHandler<C2SRequestVersion> {

    private final FiguraServer parent;

    public C2SVersionRequestHandler(FiguraServer parent) {
        this.parent = parent;
    }

    @Override
    public C2SRequestVersion serialize(IFriendlyByteBuf byteBuf) {
        return new C2SRequestVersion();
    }

    @Override
    public void handle(UUID sender, C2SRequestVersion packet) {
        parent.sendPacket(sender, new S2CProtocolVersion(Packet.PROTOCOL_VERSION));
    }
}
