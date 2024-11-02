package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.UUID;

/**
 * Packet sent to server in order to get player's userdata
 */
public class C2SFetchUserdataPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "c2s/userdata");

    private final UUID target;
    private final int responseCode;

    public C2SFetchUserdataPacket(UUID target, int responseCode) {
        this.target = target;
        this.responseCode = responseCode;
    }

    public C2SFetchUserdataPacket(IFriendlyByteBuf byteBuf) {
        target = byteBuf.readUUID();
        responseCode = byteBuf.readInt();;
    }

    public int requestId() {
        return responseCode;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeUUID(target);
    }

    public UUID target() {
        return target;
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
