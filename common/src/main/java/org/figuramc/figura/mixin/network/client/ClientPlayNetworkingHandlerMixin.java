package org.figuramc.figura.mixin.network.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkingHandlerMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void onCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {

    }
}
