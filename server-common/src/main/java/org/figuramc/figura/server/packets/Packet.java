package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.s2c.*;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.Pair;

import java.util.List;

public interface Packet {
    void write(IFriendlyByteBuf buf);
    Identifier getId();


    interface Deserializer {
        Packet read(IFriendlyByteBuf buf);
    }

    List<Pair<Identifier, Deserializer>> PACKETS = List.of(
            Pair.of(C2SBackendHandshakePacket.PACKET_ID, (buf) -> new C2SBackendHandshakePacket()),
            Pair.of(C2SDeleteAvatarPacket.PACKET_ID, C2SDeleteAvatarPacket::new),
            Pair.of(C2SEquipAvatarsPacket.PACKET_ID, C2SEquipAvatarsPacket::new),
            Pair.of(C2SFetchAvatarPacket.PACKET_ID, C2SFetchAvatarPacket::new),
            Pair.of(C2SFetchOwnedAvatarsPacket.PACKET_ID, (buf) -> new C2SFetchOwnedAvatarsPacket()),
            Pair.of(C2SFetchUserdataPacket.PACKET_ID, C2SFetchUserdataPacket::new),
            Pair.of(C2SPingPacket.PACKET_ID, C2SPingPacket::new),
            Pair.of(C2SUploadAvatarPacket.PACKET_ID, C2SUploadAvatarPacket::new),

            Pair.of(S2CBackendHandshakePacket.PACKET_ID, S2CBackendHandshakePacket::new),
            Pair.of(S2CInitializeAvatarStreamPacket.PACKET_ID, S2CInitializeAvatarStreamPacket::new),
            Pair.of(S2COwnedAvatarsPacket.PACKET_ID, S2COwnedAvatarsPacket::new),
            Pair.of(S2CPingErrorPacket.PACKET_ID, S2CPingErrorPacket::new),
            Pair.of(S2CPingPacket.PACKET_ID, S2CPingPacket::new),
            Pair.of(S2CUserdataPacket.PACKET_ID, S2CUserdataPacket::new),

            Pair.of(AllowIncomingStreamPacket.PACKET_ID, AllowIncomingStreamPacket::new),
            Pair.of(AvatarDataPacket.PACKET_ID, AvatarDataPacket::new),
            Pair.of(CloseIncomingStreamPacket.PACKET_ID, CloseIncomingStreamPacket::new),
            Pair.of(CloseOutcomingStreamPacket.PACKET_ID, CloseOutcomingStreamPacket::new),
            Pair.of(CustomFSBPacket.PACKET_ID, CustomFSBPacket::new)
    );
}
