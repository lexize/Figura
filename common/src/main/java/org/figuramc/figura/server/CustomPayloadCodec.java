package org.figuramc.figura.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.Pair;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class CustomPayloadCodec implements StreamCodec<FriendlyByteBuf, PayloadWrapper> {
    private final Packet.Deserializer deserializer;

    public CustomPayloadCodec(Packet.Deserializer deserializer) {
        this.deserializer = deserializer;
    }

    @Override
    public PayloadWrapper decode(FriendlyByteBuf object) {
        return new PayloadWrapper(deserializer.read(new FriendlyByteBufWrapper(object)));
    }

    @Override
    public void encode(FriendlyByteBuf object, PayloadWrapper object2) {
        object2.source().write(new FriendlyByteBufWrapper(object));
    }
}
