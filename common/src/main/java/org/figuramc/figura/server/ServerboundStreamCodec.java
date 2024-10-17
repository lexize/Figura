package org.figuramc.figura.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class ServerboundStreamCodec implements StreamCodec<FriendlyByteBuf, CustomPacketPayload> {
    private final Packet.Deserializer deserializer;

    public ServerboundStreamCodec(Packet.Deserializer deserializer) {
        this.deserializer = deserializer;
    }

    @Override
    public CustomPacketPayload decode(FriendlyByteBuf object) {
        Packet packet = deserializer.read(new FriendlyByteBufWrapper(object));
        System.out.println("Deserialized %s".formatted(packet.getId()));
        return new PayloadWrapper(packet);
    }

    @Override
    public void encode(FriendlyByteBuf object, CustomPacketPayload object2) {
        if (object2 instanceof PayloadWrapper wrapper) {
            wrapper.source().write(new FriendlyByteBufWrapper(object));
        }
    }
}
