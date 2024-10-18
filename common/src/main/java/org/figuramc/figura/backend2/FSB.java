package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.ducks.ServerDataAccessor;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.packets.s2c.S2CPingPacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.Hash;
import com.mojang.datafixers.util.Pair;
import org.figuramc.figura.server.utils.StatusCode;
import org.figuramc.figura.server.utils.Utils;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

public class FSB {
    private static byte[] key;
    private static S2CBackendHandshakePacket s2CHandshake;
    private static State state = State.Uninitialized;
    private static final HashMap<UUID, UserData> awaitingUserdata = new HashMap<>();
    private static int nextOutputId;
    private static final HashMap<Integer, AvatarOutputStream> outputStreams = new HashMap<>();
    private static int nextInputId;
    private static final HashMap<Integer, AvatarInputStream> inputStreams = new HashMap<>();

    public static boolean connected() {
        return s2CHandshake != null && state == State.Connected;
    }

    public static S2CBackendHandshakePacket handshake() {
        return s2CHandshake;
    }

    public static boolean handleHandshake(S2CBackendHandshakePacket packet) {
        ServerDataAccessor data = (ServerDataAccessor) Minecraft.getInstance().getCurrentServer();
        if (data != null && data.figura$allowFigura()) {
            s2CHandshake = packet;
            state = State.Connected;
            sendPacket(new C2SBackendHandshakePacket());
            FiguraToast.sendToast(FiguraText.of("backend.fsb_connected"));
            AvatarManager.clearAllAvatars(); // Used to make FSB fetch avatars of all players from FSB
            return true;
        }
        return false;
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
        state = State.Uninitialized;
        inputStreams.clear();
        outputStreams.clear();
        nextInputId = 0;
        nextOutputId = 0;
        NetworkStuff.backendStatus = 1;
        // TODO: Handling disconnecting properly, closing all incoming/outcoming streams, etc.
    }

    public static void tick() {
        if (FSB.connected()) {
            if (NetworkStuff.backendStatus != 4) NetworkStuff.backendStatus = 4;
            outputStreams.forEach((i, s) -> s.tick());
        }
    }

    public static void getAvatar(UserData target, String hash) {
        Hash h = Utils.parseHash(hash);
        inputStreams.put(nextInputId, new AvatarInputStream(nextInputId, h, getEHash(h), target));
        sendPacket(new C2SFetchAvatarPacket(nextInputId, h));
        nextInputId++;
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

    public static void handlePing(S2CPingPacket packet) {
        Avatar avatar = AvatarManager.getLoadedAvatar(packet.sender());
        if (avatar == null)
            return;
        avatar.runPing(packet.id(), packet.data());
    }

    public static void sendPacket(Packet packet) {
        Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new PayloadWrapper(packet)));
    }

    public static Hash getEHash(Hash hash) {
        byte[] hashBytes = hash.get();
        byte[] key = getKey();
        byte[] ehashBytes = new byte[hashBytes.length + key.length];
        System.arraycopy(hashBytes, 0, ehashBytes, 0, hashBytes.length);
        System.arraycopy(key, 0, ehashBytes, hashBytes.length, key.length);
        return Utils.getHash(ehashBytes);
    }

    private static File keyFile() {
        return FiguraMod.getFiguraDirectory().resolve(".fsbkey").toFile();
    }

    public static byte[] getKey() {
        if (key == null) {
            var f = keyFile();
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    key = fis.readAllBytes();
                }
                catch (IOException e) {
                    FiguraMod.LOGGER.error("Error occured while getting a key for FSB: ", e);
                    key = new byte[16];;
                }
            }
            else {
                regenerateKey();
            }
        }
        return key;
    }

    public static void regenerateKey() {
        Random rnd = new Random();
        key = new byte[16];
        rnd.nextBytes(key);
        try (FileOutputStream fos = new FileOutputStream(keyFile())) {
            fos.write(key);
        }
        catch (IOException e) {
            FiguraMod.LOGGER.error("Error occured while writing a key for FSB: ", e);
        }
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
                byte[] chunk = new byte[size];
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

    public enum State {
        Uninitialized,
        HandshakeSent,
        Connected
    }
}
