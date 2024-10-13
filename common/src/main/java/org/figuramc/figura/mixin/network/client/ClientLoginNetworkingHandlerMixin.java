package org.figuramc.figura.mixin.network.client;

import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.s2c.Handlers;
import org.figuramc.figura.server.packets.handlers.s2c.S2CHandshakeHandler;
import org.figuramc.figura.server.packets.handlers.s2c.S2CPacketHandler;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientLoginNetworkingHandlerMixin {
    @Shadow @Final private Connection connection;

    @Shadow @Final private ServerData serverData;

    @Inject(method = "handleCustomQuery", at = @At("HEAD"), cancellable = true)
    private void onCustomQuery(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
        var resLoc = packet.getIdentifier();
        var ident = new Identifier(resLoc.getNamespace(), resLoc.getPath());
        if (ident.equals(S2CBackendHandshakePacket.PACKET_ID)) {
            S2CPacketHandler<Packet> handler = Handlers.getHandler(resLoc);
            S2CBackendHandshakePacket p = (S2CBackendHandshakePacket) handler.serialize(new FriendlyByteBufWrapper(packet.getData()));
            if (FSB.handleHandshake(p, true, serverData)) {
                connection.send(new ServerboundCustomQueryPacket(packet.getTransactionId(), new FriendlyByteBuf(Unpooled.buffer())));
                ci.cancel();
            }
        }
    }
}
