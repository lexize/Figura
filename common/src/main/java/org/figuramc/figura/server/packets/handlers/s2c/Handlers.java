package org.figuramc.figura.server.packets.handlers.s2c;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.server.packets.*;
import org.figuramc.figura.server.packets.s2c.*;
import org.figuramc.figura.server.utils.Identifier;

import java.util.HashMap;

public class Handlers {
    private static final HashMap<Identifier, S2CPacketHandler<?>> PACKET_HANDLERS = new HashMap<>() {{
        put(S2CBackendHandshakePacket.PACKET_ID, new S2CHandshakeHandler());
        put(S2CUserdataPacket.PACKET_ID, new S2CUserdataHandler());
        put(AvatarDataPacket.PACKET_ID, new S2CAvatarDataPacketHandler());
        put(CloseOutcomingStreamPacket.PACKET_ID, new CloseIncomingStreamPacketHandler());
        put(CloseIncomingStreamPacket.PACKET_ID, new CloseOutcomingStreamPacketHandler());
        put(AllowIncomingStreamPacket.PACKET_ID, new AllowOutcomingStreamPacketHandler());
        put(S2CPingPacket.PACKET_ID, new S2CPingPacketHandler());
        put(S2CPingErrorPacket.PACKET_ID, new S2CPingErrorPacketHandler());
        put(CustomFSBPacket.PACKET_ID, new S2CCustomFSBPacketHandler());
    }};

    public static S2CPacketHandler<Packet> getHandler(Identifier identifier) {
        return (S2CPacketHandler<Packet>) PACKET_HANDLERS.get(identifier);
    }

    public static S2CPacketHandler<Packet> getHandler(ResourceLocation resLoc) {
        Identifier identifier = new Identifier(resLoc.getNamespace(), resLoc.getPath());
        return getHandler(identifier);
    }
}
