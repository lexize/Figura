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

    @Override
    public void write(FriendlyByteBuf buf) {
        source.write(new FriendlyByteBufWrapper(buf));
    }

    public Packet source() {
        return source;
    }

    @Override
    public ResourceLocation id() {
        var id = source.getId();
        return new ResourceLocation(id.namespace(), id.path());
    }
}
