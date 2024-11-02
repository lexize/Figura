package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.Packet;

public abstract class ActivePacketHandler<T extends Packet> implements S2CPacketHandler<T> {
    @Override
    public void handle(T packet) {
        if (FSB.instance().active()) handlePacket(packet);
    }

    protected abstract void handlePacket(T packet);
}
