package org.figuramc.figura.server.packets.handlers.s2c;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.server.packets.*;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.packets.s2c.S2CConnectedPacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.Identifier;

import java.util.HashMap;

public class Handlers {
    private static final HashMap<Identifier, S2CPacketHandler<?>> PACKET_HANDLERS = new HashMap<>() {{
        put(S2CBackendHandshakePacket.PACKET_ID, new S2CHandshakeHandler());
        put(S2CConnectedPacket.PACKET_ID, new S2CConnectedHandler());
        put(S2CUserdataPacket.PACKET_ID, new S2CUserdataHandler());
        put(AvatarDataPacket.PACKET_ID, new S2CAvatarDataPacketHandler());
        put(CloseOutcomingStreamPacket.PACKET_ID, new CloseIncomingStreamPacketHandler());
        put(CloseIncomingStreamPacket.PACKET_ID, new CloseOutcomingStreamPacketHandler());
        put(AllowIncomingStreamPacket.PACKET_ID, new AllowOutcomingStreamPacketHandler());
    }};

    public static S2CPacketHandler<Packet> getHandler(ResourceLocation resLoc) {
        Identifier identifier = new Identifier(resLoc.getNamespace(), resLoc.getPath());
        FiguraMod.LOGGER.info(identifier.toString());
        return (S2CPacketHandler<Packet>) PACKET_HANDLERS.get(identifier);
    }
}
