package org.figuramc.figura.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class PayloadWrapper implements CustomPacketPayload {
    private final Packet source;

    public PayloadWrapper(Packet source) {
        this.source = source;
    }

    public Packet source() {
        return source;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        var id = source.getId();
        return new Type<>(new ResourceLocation(id.namespace(), id.path()));
    }
}
