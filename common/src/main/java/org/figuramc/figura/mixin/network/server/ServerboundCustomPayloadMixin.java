package org.figuramc.figura.mixin.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomPayloadPacket.class)
public class ServerboundCustomPayloadMixin {
    @Inject(method = "readPayload", at = @At(value = "HEAD"), cancellable = true)
    private static void onReadPayload(ResourceLocation id, FriendlyByteBuf buf, CallbackInfoReturnable<PayloadWrapper> cir) {
        var ident = new Identifier(id.getNamespace(), id.getPath());
        var handler = FiguraModServer.getInstance().getPacketHandler(ident);
        if (handler != null) {
            Packet packet = handler.serialize(new FriendlyByteBufWrapper(buf));
            cir.setReturnValue(new PayloadWrapper(packet));
        }
    }
}
