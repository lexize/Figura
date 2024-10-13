package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public interface Packet {
    void write(IFriendlyByteBuf buf);
    Identifier getId();

    enum Stage {
        Login,
        Play
    }
}
