package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.Hash;
import com.mojang.datafixers.util.Pair;
import org.figuramc.figura.server.utils.StatusCode;
import org.figuramc.figura.server.utils.Utils;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.io.ByteArrayInputStream;
import java.util.*;

public class FSB {
    private static S2CHandshake s2CHandshake;
    private static boolean connected;
    private static final HashMap<UUID, UserData> awaitingUserdata = new HashMap<>();
    private static int nextOutputId;
    private static final HashMap<Integer, AvatarOutputStream> outputStreams = new HashMap<>();
    private static int nextInputId;
    private static final HashMap<Integer, AvatarInputStream> inputStreams = new HashMap<>();

    public static boolean connected() {
        return s2CHandshake != null && connected;
    }

    public static boolean avatars() {
        return connected() && s2CHandshake.allowAvatars();
    }

    public static boolean pings() {
        return connected() && s2CHandshake.allowPings();
    }

    public static void handleHandshake(S2CBackendHandshakePacket packet) {
        s2CHandshake =
                new S2CHandshake(packet.avatars(), packet.pings(), packet.maxAvatarSize(), packet.maxAvatarsCount(), packet.pingsRateLimit(), packet.pingsSizeLimit());
        sendPacket(new C2SBackendHandshakePacket(true, true));
        // TODO Make this function work properly when Paladin will make stuff
    }

    public static void getUser(UserData userData) {
        awaitingUserdata.put(userData.id, userData);
        sendPacket(new C2SFetchUserdataPacket(userData.id));
    }

    public static void uploadAvatar(String avatarId, byte[] avatarData) {
        outputStreams.put(nextOutputId, new AvatarOutputStream(avatarId, nextOutputId, avatarData));
        Hash hash = Utils.getHash(avatarData);
        Hash ehash = getEHash(hash);
        sendPacket(new C2SUploadAvatarPacket(nextOutputId, avatarId, hash, ehash));
        nextOutputId++;
    }

    public static void deleteAvatar(String avatarId) {
        sendPacket(new C2SDeleteAvatarPacket(avatarId));
    }

    public static void equipAvatar(List<Pair<String, Hash>> avatars) {
        HashMap<String, EHashPair> eHashPairs = new HashMap<>();
        for (Pair<String, Hash> pair: avatars) {
            eHashPairs.put(pair.getFirst(), new EHashPair(pair.getSecond(), getEHash(pair.getSecond())));
        }
        sendPacket(new C2SEquipAvatarsPacket(eHashPairs));
    }

    public static void onDisconnect() {
        s2CHandshake = null;
        connected = false;
        inputStreams.clear();
        outputStreams.clear();
        nextInputId = 0;
        nextOutputId = 0;
        // TODO: Handling disconnecting properly, closing all incoming/outcoming streams, etc.
    }

    public static void tick() {
        outputStreams.forEach((i, s) -> s.tick());
    }

    public static void getAvatar(UserData target, String hash) {
        Hash h = Utils.parseHash(hash);
        inputStreams.put(nextInputId, new AvatarInputStream(nextInputId, h, getEHash(h), target));
        nextInputId++;
    }

    public static void handleConnection() {
        connected = true;
    }

    public static void handleUserdata(S2CUserdataPacket packet) {
        UserData user = awaitingUserdata.get(packet.target());
        if (user != null) {
            boolean isHost = FiguraMod.isLocal(user.id);
            ArrayList<Pair<String, Pair<String, UUID>>> list = new ArrayList<>();
            packet.avatars().forEach((id, hashPair) -> {
                if (!isHost || getEHash(hashPair.hash()).equals(hashPair.ehash())) {
                    list.add(new Pair<>(hashPair.hash().toString(), new Pair<>(id, user.id)));
                }
            });
            user.loadData(list, new Pair<>(packet.prideBadges(), new BitSet()));
            awaitingUserdata.remove(packet.target());
        }
    }

    public static void handleAvatarData(int streamId, byte[] chunk, boolean finalChunk) {
        var inputStream = inputStreams.get(streamId);
        if (inputStream == null) {
            sendPacket(new CloseIncomingStreamPacket(streamId, StatusCode.INVALID_STREAM_ID));
            return;
        }
        inputStream.acceptDataChunk(chunk, finalChunk);
    }

    public static void handleAllow(int stream) {
        var outputStream = outputStreams.get(stream);
        if (outputStream != null) {
            outputStream.allow();
        }
    }

    public static void closeIncomingStream(int streamId, StatusCode code) {
        var inputStream = inputStreams.get(streamId);
        if (inputStream != null) {
            inputStream.close(code);
        }
    }

    public static void closeOutcomingStreamPacket(int streamId, StatusCode code) {
        var outputStream = outputStreams.get(streamId);
        if (outputStream != null) {
            outputStream.close(code);
        }
    }

    public static class S2CHandshake {
        private final boolean allowAvatars, allowPings;
        private final int maxAvatarSize, maxAvatarCount, pingsRateLimit, pingsSizeLimit;

        public S2CHandshake(boolean allowAvatars, boolean allowPings, int maxAvatarSize, int maxAvatarCount, int pingsRateLimit, int pingsSizeLimit) {
            this.allowAvatars = allowAvatars;
            this.allowPings = allowPings;
            this.maxAvatarSize = maxAvatarSize;
            this.maxAvatarCount = maxAvatarCount;
            this.pingsRateLimit = pingsRateLimit;
            this.pingsSizeLimit = pingsSizeLimit;
        }

        public boolean allowAvatars() {
            return allowAvatars;
        }

        public boolean allowPings() {
            return allowPings;
        }

        public int maxAvatarSize() {
            return maxAvatarSize;
        }

        public int maxAvatarCount() {
            return maxAvatarCount;
        }

        public int pingsRateLimit() {
            return pingsRateLimit;
        }

        public int pingsSizeLimit() {
            return pingsSizeLimit;
        }
    }

    public static void sendPacket(Packet packet) {
        var byteBuf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(new FriendlyByteBufWrapper(byteBuf));
        var resPath = new ResourceLocation(packet.getId().namespace(), packet.getId().path());
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(resPath, byteBuf));
    }

    public static Hash getEHash(Hash hash) {
        return hash; /* VERY IMPORTANT NOTE: THIS IS TEMPORARY!!!! SOLUTION, IT IS **EXTREMELY UNSAFE** AND ***MUST NOT!!*** GET IN RELEASE VERSION.
        IT IS MADE JUST SO I CAN CONTINUE WORKING ON FSB AND TESTING OTHER FEATURES WHILE WAITING FOR PALADIN TO FINISH SECRET KEY OPTION IN CONFIG. */
        // TODO Make this function work properly when Paladin will make stuff
    }

    private static class AvatarInputStream {
        private final int id;
        private final Hash hash;
        private final Hash ehash;
        private final UserData target;
        private final LinkedList<byte[]> dataChunks = new LinkedList<>();
        private int size = 0;

        private AvatarInputStream(int id, Hash hash, Hash ehash, UserData target) {
            this.id = id;
            this.hash = hash;
            this.ehash = ehash;
            this.target = target;
        }

        private void acceptDataChunk(byte[] chunk, boolean finalChunk) {
            dataChunks.add(chunk);
            size += chunk.length;
            if (finalChunk) {
                byte[] avatarData = new byte[size];
                int offset = 0;
                for (byte[] dataChunk : dataChunks) {
                    System.arraycopy(dataChunk, 0, avatarData, offset, dataChunk.length);
                    offset += dataChunk.length;
                }
                Hash resultHash = Utils.getHash(avatarData);
                if (!resultHash.equals(hash)) {
                    sendPacket(new CloseIncomingStreamPacket(id, StatusCode.INVALID_HASH));
                }
                if (FiguraMod.isLocal(target.id) && !getEHash(hash).equals(ehash)) {
                    sendPacket(new CloseIncomingStreamPacket(id, StatusCode.OWNERSHIP_CHECK_ERROR));
                }

                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(avatarData);
                    CompoundTag tag = NbtIo.readCompressed(bais);
                    CacheAvatarLoader.save(hash.toString(), tag);
                    target.loadAvatar(tag);
                }
                catch (Exception e) {
                    FiguraMod.LOGGER.error("Failed to load avatar for " + target.id, e);
                }
                inputStreams.remove(id);
            }
        }

        private void close(StatusCode code) {
            switch (code) {
                case AVATAR_DOES_NOT_EXIST -> FiguraMod.LOGGER.info("Avatar with hash %s does not exist on this server".formatted(hash));
                default -> FiguraMod.LOGGER.error("Incoming stream was closed by unexpected reason: %s".formatted(code.name()));
            }
            inputStreams.remove(id);
        }
    }

    private static class AvatarOutputStream {
        private final String avatarId;
        private final int id;
        private final byte[] data;
        private int position;
        private boolean upload;

        private AvatarOutputStream(String avatarId, int id, byte[] data) {
            this.avatarId = avatarId;
            this.id = id;
            this.data = data;
        }

        private void tick() {
            if (upload) {
                int size = nextChunkSize();
                byte[] chunk = new byte[nextChunkSize()];
                System.arraycopy(data, position, chunk, 0, chunk.length);
                position += size;
                boolean finalChunk = data.length == position;
                sendPacket(new AvatarDataPacket(id, finalChunk, chunk));
                if (finalChunk) upload = false;
            }
        }

        private int nextChunkSize() {
            return Math.min(AvatarDataPacket.MAX_CHUNK_SIZE, data.length - position);
        }

        private void allow() {
            upload = true;
        }

        private void close(StatusCode code) {
            switch (code) {
                case FINISHED, ALREADY_EXISTS -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_success"));
                    equipAvatar(List.of(Pair.of(avatarId, Utils.getHash(data))));
                    AvatarManager.localUploaded = true;
                }
                case MAX_AVATAR_SIZE_EXCEEDED -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_too_big"), FiguraToast.ToastType.ERROR);
                }
                default -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_error"), code, FiguraToast.ToastType.ERROR);
                }
            }
            outputStreams.remove(id);
        }
    }
}
