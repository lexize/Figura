package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.packets.Packet;

import java.util.UUID;

public abstract class AuthorizedC2SPacketHandler<P extends Packet> implements C2SPacketHandler<P> {
    private final FiguraServer parent;

    protected AuthorizedC2SPacketHandler(FiguraServer parent) {
        this.parent = parent;
    }

    @Override
    public final void handle(UUID sender, P packet) {
        FiguraUser user = parent.userManager().getUser(sender);
        if (user == null) return;
        handle(user, packet);
    }

    protected abstract void handle(FiguraUser sender, P packet);
}
