package org.figuramc.figura.backend2;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.ducks.ServerDataAccessor;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.packets.s2c.S2CConnectedPacket;
import org.figuramc.figura.server.packets.s2c.S2CPingPacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.Hash;
import com.mojang.datafixers.util.Pair;
import org.figuramc.figura.server.utils.Result;
import org.figuramc.figura.server.utils.StatusCode;
import org.figuramc.figura.server.utils.Utils;
import org.figuramc.figura.utils.FiguraText;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public abstract class FSB {
    private static final int MIN_PROTOCOL_VERSION_SUPPORTED = 0;

    private static FSB instance;
    private byte[] key;
    private S2CBackendHandshakePacket s2CHandshake;
    private State state = State.WaitingForProtocolVersion;
    private final HashMap<Integer, Consumer<S2CUserdataPacket>> awaitingUserdata = new HashMap<>();
    private final HashMap<Integer, AvatarOutputStream> outputStreams = new HashMap<>();
    private final HashMap<Integer, AvatarInputStream> inputStreams = new HashMap<>();
    private final ArrayList<UUID> playersConnectedToFSB = new ArrayList<>();
    private final ArrayList<UUID> fetchedByFSB = new ArrayList<>();
    private int nextRequestId = 0;
    private int serverProtocolVersion;

    private int initFetchTick = 0;
    private int handshakeAttempts = 0;
    private static final int HANDSHAKE_SEND_DELAY = 40;
    private static final int MAX_ATTEMPTS_TO_CONNECT = 10;

    protected FSB() {
        if (instance != null) throw new IllegalStateException("Unable to create more than one FSB instance");
        instance = this;
    }

    public static FSB instance() {
        return instance;
    }

    public State state() {
        return state;
    }

    public boolean active() {
        return s2CHandshake != null && state == State.Connected;
    }

    public S2CBackendHandshakePacket handshake() {
        return s2CHandshake;
    }

    private static boolean fsbAllowed() {
        ServerDataAccessor data = (ServerDataAccessor) Minecraft.getInstance().getCurrentServer();
        return data != null && data.figura$allowFigura();
    }

    public void handleHandshake(S2CBackendHandshakePacket packet) {
        if (fsbAllowed() && state == State.HandshakeSent) {
            s2CHandshake = packet;
            state = State.Connected;
            FiguraToast.sendToast(FiguraText.of("backend.fsb_connected"));
            AvatarManager.clearAllAvatars();
            packet.forEachConnectedUser((user) -> {
                if (!playersConnectedToFSB.contains(user))
                    playersConnectedToFSB.add(user);
            });
        }
    }

    public void handleConnectionRefusal() {
        state = State.Refused;
    }

    public void handleConnected(S2CConnectedPacket p) {
        if (!playersConnectedToFSB.contains(p.user()))
            playersConnectedToFSB.add(p.user());
    }

    public void handleVersion(int version) {
        if (version < MIN_PROTOCOL_VERSION_SUPPORTED || version > Packet.PROTOCOL_VERSION) {
            state = State.Incompatible;
        }
        else {
            serverProtocolVersion = version;
            state = State.Uninitialized;
        }
    }

    public void handleUserdata(S2CUserdataPacket packet) {
        Consumer<S2CUserdataPacket> handler = awaitingUserdata.get(packet.responseId());
        if (handler != null) handler.accept(packet);
    }

    public void handleAllow(int stream) {
        var outputStream = outputStreams.get(stream);
        if (outputStream != null) {
            outputStream.allow();
        }
    }

    public void handleAvatarData(int streamId, byte[] chunk, boolean finalChunk) {
        var inputStream = inputStreams.get(streamId);
        if (inputStream == null) {
            sendPacket(new CloseIncomingStreamPacket(streamId, StatusCode.INVALID_STREAM_ID));
            return;
        }
        inputStream.acceptDataChunk(chunk, finalChunk);
    }

    public void handlePing(S2CPingPacket packet) {
        Avatar avatar = AvatarManager.getLoadedAvatar(packet.sender());
        if (avatar == null)
            return;
        avatar.runPing(packet.id(), packet.data());
    }

    private int getNextRequestId() {
        int v = nextRequestId;
        nextRequestId++;
        return v;
    }

    public void getUserAndApply(UserData userData) {
        getUser(userData.id, (packet) -> applyUserdata(userData, packet));
    }

    public void getUserAndApplyOffline(UserData userData) {
        getUser(userData.id, (packet) -> applyUserdataOffline(userData, packet));
    }

    public void getUser(UUID uuid, Consumer<S2CUserdataPacket> handler) {
        int id = getNextRequestId();
        awaitingUserdata.put(id, handler);
        sendPacket(new C2SFetchUserdataPacket(uuid, id));
    }

    public void uploadAvatar(String avatarId, byte[] avatarData) {
        int id = getNextRequestId();
        outputStreams.put(id, new AvatarOutputStream(this, avatarId, id, avatarData));
        Hash hash = Utils.getHash(avatarData);
        Hash ehash = getEHash(hash);
        sendPacket(new C2SUploadAvatarPacket(id, avatarId, hash, ehash));
    }

    public void deleteAvatar(String avatarId) {
        sendPacket(new C2SDeleteAvatarPacket(avatarId));
    }

    public void equipAvatar(List<Pair<String, Hash>> avatars) {
        HashMap<String, EHashPair> eHashPairs = new HashMap<>();
        for (Pair<String, Hash> pair: avatars) {
            eHashPairs.put(pair.getFirst(), new EHashPair(pair.getSecond(), getEHash(pair.getSecond())));
        }
        sendPacket(new C2SEquipAvatarsPacket(eHashPairs));
    }

    public void onDisconnect() {
        s2CHandshake = null;
        state = State.WaitingForProtocolVersion;
        initFetchTick = 0;
        handshakeAttempts = 0;
        inputStreams.clear();
        outputStreams.clear();
        playersConnectedToFSB.clear();
        fetchedByFSB.clear();
    }

    public boolean connectedToFSB(UUID user) {
        return playersConnectedToFSB.contains(user);
    }

    public boolean allowedToFetch(UUID user) {
        return fetchedByFSB.contains(user);
    }

    public void tick() {
        if (!fsbAllowed()) return;
        if (!active()) {
            if (handshakeAttempts < MAX_ATTEMPTS_TO_CONNECT) return;
            if (state != State.Refused) {
                initFetchTick++;
                if (initFetchTick == HANDSHAKE_SEND_DELAY) {
                    switch (state) {
                        case WaitingForProtocolVersion -> sendPacket(new C2SRequestVersion());
                        case Uninitialized, HandshakeSent -> {
                            sendPacket(new C2SBackendHandshakePacket());
                            state = State.HandshakeSent;
                            handshakeAttempts++;
                        }
                    }
                    initFetchTick = 0;
                }
            }
        }
        else outputStreams.forEach((i, s) -> s.tick());
    }

    public void getAvatar(Hash hash, @Nullable Hash ehash, Consumer<Result<byte[]>> avatarDataConsumer) {
        int id = getNextRequestId();
        inputStreams.put(id, new AvatarInputStream(this, id, hash, ehash, avatarDataConsumer));
        sendPacket(new C2SFetchAvatarPacket(id, hash));
    }

    public void getAvatarAndApply(UserData target, String h) {
        Hash hash = Utils.parseHash(h);
        getAvatar(hash, FiguraMod.isLocal(target.id) ? getEHash(hash) : null, (data) -> applyAvatar(target, hash, data));
    }

    void applyAvatar(UserData target, Hash hash, Result<byte[]> data) {
        try {
            byte[] avatarData = data.get();
            ByteArrayInputStream bais = new ByteArrayInputStream(avatarData);
            CompoundTag tag = NbtIo.readCompressed(bais);
            CacheAvatarLoader.save(hash.toString(), tag);
            target.loadAvatar(tag);
        } catch (Throwable e) {
            FiguraMod.LOGGER.error("Failed to load avatar for " + target.id, e);
        }
    }

    void applyUserdata(UserData user, S2CUserdataPacket packet) {
        boolean isHost = FiguraMod.isLocal(user.id);
        ArrayList<Pair<String, Pair<String, UUID>>> list = new ArrayList<>();
        var id = packet.avatar().left();
        var hashPair = packet.avatar().right();
        if (!isHost || getEHash(hashPair.hash()).equals(hashPair.ehash())) {
            list.add(new Pair<>(hashPair.hash().toString(), new Pair<>(id, user.id)));
        }
        user.loadData(list, new Pair<>(packet.prideBadges(), new BitSet()));
        awaitingUserdata.remove(packet.responseId());
    }

    void applyUserdataOffline(UserData user, S2CUserdataPacket packet) {
        if (packet.loadFromFSBIfOffline()) {
            applyUserdata(user, packet);
        }
        else {
            fetchedByFSB.add(user.id);
            AvatarManager.clearAvatars(user.id);
        }
    }

    public void closeIncomingStream(int streamId, StatusCode code) {
        var inputStream = inputStreams.get(streamId);
        if (inputStream != null) {
            inputStream.close(code);
        }
    }

    public void closeOutcomingStreamPacket(int streamId, StatusCode code) {
        var outputStream = outputStreams.get(streamId);
        if (outputStream != null) {
            outputStream.close(code);
        }
    }

    public abstract void sendPacket(Packet packet);

    public Hash getEHash(Hash hash) {
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

    public byte[] getKey() {
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

    public void regenerateKey() {
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
        private final FSB parent;
        private final int id;
        private final Hash hash;
        private final Hash ehash;
        private final Consumer<Result<byte[]>> avatarDataConsumer;
        private final LinkedList<byte[]> dataChunks = new LinkedList<>();
        private int size = 0;

        private AvatarInputStream(FSB parent, int id, Hash hash, Hash ehash, Consumer<Result<byte[]>> avatarDataConsumer) {
            this.parent = parent;
            this.id = id;
            this.hash = hash;
            this.ehash = ehash;
            this.avatarDataConsumer = avatarDataConsumer;
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
                    parent.sendPacket(new CloseIncomingStreamPacket(id, StatusCode.INVALID_HASH));
                }
                if (ehash != null && !parent.getEHash(hash).equals(ehash)) {
                    parent.sendPacket(new CloseIncomingStreamPacket(id, StatusCode.OWNERSHIP_CHECK_ERROR));
                }

                try {
                    avatarDataConsumer.accept(new Result<>(avatarData));
                }
                catch (Exception e) {
                    avatarDataConsumer.accept(new Result<>(e));
                }
                parent.inputStreams.remove(id);
            }
        }

        private void close(StatusCode code) {
            switch (code) {
                case AVATAR_DOES_NOT_EXIST -> FiguraMod.LOGGER.info("Avatar with hash %s does not exist on this server".formatted(hash));
                default -> FiguraMod.LOGGER.error("Incoming stream was closed by unexpected reason: %s".formatted(code.name()));
            }
            parent.inputStreams.remove(id);
        }
    }

    private static class AvatarOutputStream {
        private final FSB parent;
        private final String avatarId;
        private final int id;
        private final byte[] data;
        private int position;
        private boolean upload;

        private AvatarOutputStream(FSB parent, String avatarId, int id, byte[] data) {
            this.parent = parent;
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
                parent.sendPacket(new AvatarDataPacket(id, finalChunk, chunk));
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
                    parent.equipAvatar(List.of(Pair.of(avatarId, Utils.getHash(data))));
                    AvatarManager.localUploaded = true;
                }
                case MAX_AVATAR_SIZE_EXCEEDED -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_too_big"), FiguraToast.ToastType.ERROR);
                }
                default -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_error"), code, FiguraToast.ToastType.ERROR);
                }
            }
            parent.outputStreams.remove(id);
        }
    }

    public enum State {
        WaitingForProtocolVersion,
        Uninitialized,
        HandshakeSent,
        Connected,
        Refused,
        Incompatible
    }
}
