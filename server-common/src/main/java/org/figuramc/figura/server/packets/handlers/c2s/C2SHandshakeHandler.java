package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUserManager;
import org.figuramc.figura.server.packets.c2s.C2SBackendHandshakePacket;
import org.figuramc.figura.server.packets.s2c.S2CConnected;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.UUID;

public class C2SHandshakeHandler implements C2SPacketHandler<C2SBackendHandshakePacket> {
    private final FiguraServer parent;

    public C2SHandshakeHandler(FiguraServer parent) {
        this.parent = parent;
    }

    @Override
    public C2SBackendHandshakePacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SBackendHandshakePacket(byteBuf);
    }

    @Override
    public void handle(UUID sender, C2SBackendHandshakePacket packet) {
        FiguraUserManager manager = parent.userManager();
        if (!manager.isExpected(sender)) return;
        parent.sendDeferredPacket(sender, manager
                .setupOnlinePlayer(sender, packet.pings(), packet.avatars(), packet.maxAvatarChunkSize())
                .thenApplyAsync(
                v -> new S2CConnected()
        ));
    }
}
