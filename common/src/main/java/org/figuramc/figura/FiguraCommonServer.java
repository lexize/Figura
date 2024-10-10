package org.figuramc.figura;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

public class FiguraCommonServer extends FiguraServer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Figura Server");
    private final MinecraftServer parent;
    public FiguraCommonServer(MinecraftServer parent) {
        this.parent = parent;
    }

    @Override
    public Path getFiguraFolder() {
        return Path.of("fsb");
    }

    @Override
    protected void sendPacketInternal(UUID receiver, Packet packet) {
        ServerPlayer player = parent.getPlayerList().getPlayer(receiver);
        if (player != null) {
            var byteBuf = new FriendlyByteBuf(Unpooled.buffer());
            packet.write(new FriendlyByteBufWrapper(byteBuf));
            var id = packet.getId();
            var path = new ResourceLocation(id.namespace(), id.path());
            player.connection.send(new ClientboundCustomPayloadPacket(path, byteBuf));
        }
    }

    public static FiguraCommonServer getInstance() {
        return (FiguraCommonServer) INSTANCE;
    }
}
