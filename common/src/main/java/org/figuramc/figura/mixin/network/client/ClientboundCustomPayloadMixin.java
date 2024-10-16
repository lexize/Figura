package org.figuramc.figura.mixin.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.s2c.Handlers;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundCustomPayloadPacket.class)
public class ClientboundCustomPayloadMixin {
    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
    private static void onReadPayload(ResourceLocation id, FriendlyByteBuf buf, CallbackInfoReturnable<PayloadWrapper> cir) {
        var handler = Handlers.getHandler(id);
        if (handler != null) {
            Packet packet = handler.serialize(new FriendlyByteBufWrapper(buf));
            cir.setReturnValue(new PayloadWrapper(packet));
        }
    }
}
