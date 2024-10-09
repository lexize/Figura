package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.avatars.UserdataAvatar;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.HashMap;

public class S2COwnedAvatarsPacket implements Packet {
    // TODO packet id
    private final HashMap<String, UserdataAvatar> ownedAvatars;

    public S2COwnedAvatarsPacket(HashMap<String, UserdataAvatar> ownedAvatars) {
        this.ownedAvatars = (HashMap<String, UserdataAvatar>) ownedAvatars.clone();
    }

    public S2COwnedAvatarsPacket(IFriendlyByteBuf byteBuf) {
        // TODO packet deserializer
        ownedAvatars = null;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        // TODO packet serializer
    }

    @Override
    public Identifier getId() {
        return null;
    }
}
