package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class C2SEquipAvatarsPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "avatars/equip");
    private final ArrayList<EHashPair> avatars;

    public C2SEquipAvatarsPacket(ArrayList<EHashPair> pairs) {
        avatars = pairs;
    }

    public C2SEquipAvatarsPacket(IFriendlyByteBuf byteBuf) {
        int count = byteBuf.readByte();
        ArrayList<EHashPair> pairs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pairs.add(new EHashPair(byteBuf.readHash(), byteBuf.readHash()));
        }
        avatars = pairs;
    }

    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeByte(avatars.size());
        for (EHashPair pair: avatars) {
            byteBuf.writeBytes(pair.hash().get());
            byteBuf.writeBytes(pair.ehash().get());
        }
    }

    public Identifier getId() {
        return PACKET_ID;
    }
}
