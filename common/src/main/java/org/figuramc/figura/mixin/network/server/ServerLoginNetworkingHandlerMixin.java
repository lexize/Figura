package org.figuramc.figura.mixin.network.server;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.HandshakeEvent;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.C2SBackendHandshakePacket;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginNetworkingHandlerMixin {
    @Shadow @Final private Connection connection;
    @Shadow @Nullable
    public GameProfile gameProfile;
    @Unique
    private int figura$handshakeState = 0;
    private static final int packetId = 0xF1548731;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;handleAcceptedLogin()V"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (!canListen()) return;
        if (figura$handshakeState != 2) {
            UUID id = gameProfile.getId();
            if (Events.call(new HandshakeEvent(id)).isCancelled()) {
                figura$handshakeState = 2;
                return;
            }
            if (figura$handshakeState != 1) {
                var srv = FiguraServer.getInstance();
                S2CBackendHandshakePacket packet = srv.getHandshake();
                var res = new ResourceLocation(packet.getId().namespace(), packet.getId().path());
                if (packet != null) {
                    FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());
                    packet.write(new FriendlyByteBufWrapper(byteBuf));
                    srv.userManager().expect(id);
                    connection.send(new ClientboundCustomQueryPacket(packetId, res, byteBuf));
                    figura$handshakeState = 1;
                }
            }
            ci.cancel();
        }
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void handleResponse(ServerboundCustomQueryPacket packet, CallbackInfo ci) {
        if (!canListen()) return;
        if (figura$handshakeState == 1 && packet.getTransactionId() == packetId) {
            var data = packet.getData();
            if (data != null) {
                C2SPacketHandler<Packet> handler = FiguraServer.getInstance().getPacketHandler(C2SBackendHandshakePacket.PACKET_ID);
                Packet p = handler.serialize(new FriendlyByteBufWrapper(data));
                handler.handle(gameProfile.getId(), p);
            }
            figura$handshakeState = 2;
            ci.cancel();
        }
    }

    @Unique
    private static boolean canListen() {
        return FiguraServer.initialized();
    }
}
