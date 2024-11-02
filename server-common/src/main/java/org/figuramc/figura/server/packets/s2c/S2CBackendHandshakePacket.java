package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

public class S2CBackendHandshakePacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/handshake");

    private final int pingsRateLimit;
    private final int pingsSizeLimit;

    private final int maxAvatarSize;
    private final int maxAvatarsCount;

    private final ArrayList<UUID> connectedUsers;

    public S2CBackendHandshakePacket(int pingsRateLimit, int pingsSizeLimit, int maxAvatarSize, int maxAvatarsCount, ArrayList<UUID> connectedUsers) {
        this.pingsRateLimit = pingsRateLimit;
        this.pingsSizeLimit = pingsSizeLimit;
        this.maxAvatarSize = maxAvatarSize;
        this.maxAvatarsCount = maxAvatarsCount;
        this.connectedUsers = connectedUsers;
    }

    public S2CBackendHandshakePacket(IFriendlyByteBuf source) {
        pingsRateLimit = source.readInt();
        pingsSizeLimit = source.readInt();

        maxAvatarSize = source.readInt();
        maxAvatarsCount = source.readInt();
        int usersCount = source.readInt();
        ArrayList<UUID> users = new ArrayList<>();
        for (int i = 0; i < usersCount; i++) {
            users.add(source.readUUID());
        }
        this.connectedUsers = users;
    }

    public int pingsRateLimit() {
        return pingsRateLimit;
    }

    public int pingsSizeLimit() {
        return pingsSizeLimit;
    }

    public int maxAvatarSize() {
        return maxAvatarSize;
    }

    public int maxAvatarsCount() {
        return maxAvatarsCount;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeInt(pingsRateLimit);
        byteBuf.writeInt(pingsSizeLimit);

        byteBuf.writeInt(maxAvatarSize);
        byteBuf.writeInt(maxAvatarsCount);

        byteBuf.writeInt(connectedUsers.size());
        for (UUID user: connectedUsers) {
            byteBuf.writeUUID(user);
        }
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }

    public void forEachConnectedUser(Consumer<UUID> handler) {
        connectedUsers.forEach(handler);
    }
}
