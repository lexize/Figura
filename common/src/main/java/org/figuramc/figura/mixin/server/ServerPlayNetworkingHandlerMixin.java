package org.figuramc.figura.mixin.server;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.figuramc.figura.FiguraModServer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkingHandlerMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onPlayerPlayInit(MinecraftServer server, Connection connection, ServerPlayer player, CallbackInfo ci) {
        FiguraModServer.getInstance().userManager().onUserJoin(player.getUUID());
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        var srv = FiguraModServer.getInstance();
        var resLoc = packet.getIdentifier();
        var id = new Identifier(resLoc.getNamespace(), resLoc.getPath());
        C2SPacketHandler<Packet> handler = srv.getPacketHandler(id);
        if (handler != null) {
            handler.handle(this.player.getUUID(), handler.serialize(new FriendlyByteBufWrapper(packet.getData())));
            ci.cancel();
        }
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayerDisconnect(Component reason, CallbackInfo ci) {
        FiguraModServer.getInstance().userManager().onUserLeave(player.getUUID());
    }
}
