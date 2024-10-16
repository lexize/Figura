package org.figuramc.figura.server;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

public class FiguraModServer extends FiguraServer {
    public static final Logger LOGGER = LoggerFactory.getLogger("FSB");
    private final MinecraftServer parent;
    public FiguraModServer(MinecraftServer parent) {
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

    @Override
    public void logInfo(String text) {
        LOGGER.info(text);
    }

    @Override
    public void logError(String text) {
        LOGGER.error(text);
    }

    @Override
    public void logError(String text, Throwable err) {
        LOGGER.error(text, err);
    }

    @Override
    public void logDebug(String text) {
        LOGGER.debug(text);
    }

    public static FiguraModServer getInstance() {
        return (FiguraModServer) INSTANCE;
    }


}
