package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.Pair;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class S2CUserdataPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/userdata");

    private final int responseId;
    private final BitSet prideBadges;
    private final Pair<String, EHashPair> equippedAvatar;
    private final boolean loadFromFSBIfOffline;

    public S2CUserdataPacket(int responseId, BitSet prideBadges, Pair<String, EHashPair> avatar, boolean loadFromFSBIfOffline) {
        this.responseId = responseId;
        this.prideBadges = prideBadges;
        this.equippedAvatar = avatar;
        this.loadFromFSBIfOffline = loadFromFSBIfOffline;
    }

    public S2CUserdataPacket(IFriendlyByteBuf byteBuf) {
        this.responseId = byteBuf.readInt();
        this.prideBadges = BitSet.valueOf(byteBuf.readByteArray(Integer.MAX_VALUE));
        String avatarId = new String(byteBuf.readByteArray(Integer.MAX_VALUE), UTF_8);
        Hash hash = byteBuf.readHash();
        Hash ehash = byteBuf.readHash();
        equippedAvatar = new Pair<>(avatarId, new EHashPair(hash, ehash));
        loadFromFSBIfOffline = byteBuf.readByte() != 0;
    }

    public int responseId() {
        return responseId;
    }

    public BitSet prideBadges() {
        return prideBadges;
    }

    public Pair<String, EHashPair> avatar() {
        return equippedAvatar;
    }

    public boolean loadFromFSBIfOffline() {
        return loadFromFSBIfOffline;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeInt(responseId);
        byteBuf.writeByteArray(prideBadges.toByteArray());
        byteBuf.writeByteArray(equippedAvatar.left().getBytes(UTF_8));
        byteBuf.writeBytes(equippedAvatar.right().hash().get());
        byteBuf.writeBytes(equippedAvatar.right().ehash().get());
        byteBuf.writeByte(loadFromFSBIfOffline ? 1 : 0);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
