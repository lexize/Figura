package org.figuramc.figura.server;

import org.figuramc.figura.server.avatars.FiguraServerAvatarManager;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.HandshakeEvent;
import org.figuramc.figura.server.events.packets.OutcomingPacketEvent;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.handlers.c2s.*;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.Utils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class FiguraServer {
    private static FiguraServer INSTANCE;
    private final FiguraUserManager userManager = new FiguraUserManager(this);
    private final FiguraServerAvatarManager avatarManager = new FiguraServerAvatarManager(this);
    private FiguraServerConfig config;
    private final DeferredPacketsQueue deferredPacketsQueue = new DeferredPacketsQueue(this);
    protected FiguraServer() {
        if (INSTANCE != null) throw new IllegalStateException("Can't create more than one instance of FiguraServer");
        INSTANCE = this;
    }

    private final HashMap<Identifier, C2SPacketHandler<?>> PACKET_HANDLERS = new HashMap<>() {{
        put(C2SBackendHandshakePacket.PACKET_ID, new C2SHandshakeHandler(FiguraServer.this));
        put(C2SFetchAvatarPacket.PACKET_ID, new C2SFetchAvatarPacketHandler(FiguraServer.this));
        put(C2SFetchUserdataPacket.PACKET_ID, new C2SFetchUserdataPacketHandler(FiguraServer.this));
        put(C2SUploadAvatarPacket.PACKET_ID, new C2SUploadAvatarPacketHandler(FiguraServer.this));
        // TODO: FetchOwnedAvatarsPacketHandler
        put(C2SDeleteAvatarPacket.PACKET_ID, new C2SDeleteAvatarPacketHandler(FiguraServer.this));

        put(AvatarDataPacket.PACKET_ID, new C2SAvatarDataPacketHandler(FiguraServer.this));
    }};

    public static FiguraServer getInstance() {
        return INSTANCE;
    }

    public abstract Path getFiguraFolder();

    public abstract void registerHandler(Identifier packetId, C2SPacketHandler<?> handler);
    public abstract void unregisterHandler(Identifier packetId);

    public Path getUsersFolder() {
        return getFiguraFolder().resolve("users");
    }

    public Path getAvatarsFolder() {
        return getFiguraFolder().resolve("avatars");
    }

    public Path getAvatar(byte[] hash) {
        return getAvatarsFolder().resolve("%s.nbt".formatted(Utils.hexFromBytes(hash)));
    }

    public Path getAvatarMetadata(byte[] hash) {
        return getAvatarsFolder().resolve("%s.mtd".formatted(Utils.hexFromBytes(hash)));
    }

    public Path getUserdataFile(UUID user) {
        return getUsersFolder().resolve("%s.pl".formatted(Utils.uuidToHex(user)));
    }

    public final void init() {
        getUsersFolder().toFile().mkdirs();
        getAvatarsFolder().toFile().mkdirs();
    }

    public final void close() {
        avatarManager.close();
        userManager.close();
        INSTANCE = null;
    }

    protected final void tick() {
        deferredPacketsQueue.tick();
        avatarManager.tick();
    }

    public final void sendHandshake(UUID receiver) {
        var event = Events.call(new HandshakeEvent(receiver));
        if (!event.isCancelled()) {
            userManager.expect(receiver);
            sendPacket(receiver, new S2CBackendHandshakePacket(
                    config.pings(),
                    config.pingsRateLimit(),
                    config.pingsSizeLimit(),
                    config.avatars(),
                    config.avatarSizeLimit(),
                    config.avatarsCountLimit()
            ));
        }
    }

    public FiguraServerConfig config() {
        return config;
    }

    public FiguraUserManager userManager() {
        return userManager;
    }

    public final void sendPacket(UUID receiver, Packet packet) {
        OutcomingPacketEvent event = new OutcomingPacketEvent(receiver, packet);
        Events.call(event);
        if (!event.isCancelled()) {
            sendPacketInternal(receiver, packet);
        }
    }

    public final void sendDeferredPacket(UUID receiver, CompletableFuture<? extends Packet> packet) {
        deferredPacketsQueue.sendPacket(receiver, packet);
    }

    protected abstract void sendPacketInternal(UUID receiver, Packet packet);

    public FiguraServerAvatarManager avatarManager() {
        return avatarManager;
    }
}
