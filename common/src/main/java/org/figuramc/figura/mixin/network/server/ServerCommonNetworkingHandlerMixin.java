package org.figuramc.figura.mixin.network.server;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class)
public abstract class ServerCommonNetworkingHandlerMixin {
    @Shadow protected abstract GameProfile playerProfile();

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (!FiguraServer.initialized()) return;
        var srv = FiguraModServer.getInstance();
        var payload = packet.payload();
        if (payload instanceof PayloadWrapper wrapper) {
            var gameProfile = this.playerProfile();
            var source = wrapper.source();
            C2SPacketHandler<Packet> handler = srv.getPacketHandler(source.getId());
            if (handler != null) {
                try {
                    handler.handle(gameProfile.getId(), source);
                }
                catch (Exception e) {
                    srv.logError("", e);
                }
                ci.cancel();
            }
        }

    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayerDisconnect(Component reason, CallbackInfo ci) {
        if (!FiguraServer.initialized()) return;
        FiguraModServer.getInstance().userManager().onUserLeave(playerProfile().getId());
    }
}
