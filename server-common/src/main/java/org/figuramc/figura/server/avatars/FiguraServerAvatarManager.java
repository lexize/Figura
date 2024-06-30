package org.figuramc.figura.server.avatars;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.events.*;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.s2c.S2CInitializeAvatarStreamPacket;
import org.figuramc.figura.server.utils.Pair;
import org.figuramc.figura.server.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class FiguraServerAvatarManager {
    private final HashMap<byte[], AvatarData> data = new HashMap<>();
    private final HashMap<byte[], CompletableFuture<AvatarData>> loadingAvatarData = new HashMap<>();

    private final HashMap<byte[], AvatarMetadata> metadata = new HashMap<>();
    private final HashMap<byte[], CompletableFuture<AvatarMetadata>> loadingMetadata = new HashMap<>();

    private final HashMap<byte[], CompletableFuture<Pair<AvatarData, AvatarMetadata>>> loadingAvatars = new HashMap<>();

    private final ArrayList<AvatarOutcomingStream> openedOutcomingStreams = new ArrayList<>();

    private final HashMap<byte[], ArrayList<Awaiting>> awaiting = new HashMap<>();

    public void openOutcomingStream(UUID receiver, byte[] hash, int streamId) {
        if (data.containsKey(hash)) {
            if (!isOutcomingStreamOpen(receiver, hash))
                openedOutcomingStreams.add(new AvatarOutcomingStream(receiver, data.get(hash), streamId,
                    hash, getEHashForAvatar(receiver, hash).join()));
        }
        else {
            loadingAvatars.computeIfAbsent(hash, this::loadAvatar);
            var awaitingClients = getAwaiting(hash);
            var awaitingStream = new AwaitingOutcomingStream(receiver, hash, streamId);
            if (!awaitingClients.contains(awaitingStream)) awaitingClients.add(awaitingStream);
        }
    }

    private boolean isOutcomingStreamOpen(UUID receiver, byte[] hash) {
        int hashCode = Objects.hash(receiver, Arrays.hashCode(hash));
        return openedOutcomingStreams.stream().anyMatch(s -> s.hashCode() == hashCode);
    }

    private ArrayList<Awaiting> getAwaiting(byte[] hash) {
        return awaiting.computeIfAbsent(hash, k -> new ArrayList<>());
    }

    private CompletableFuture<AvatarData> startLoadingAvatarData(byte[] hash) {
        if (loadingAvatarData.containsKey(hash)) return loadingAvatarData.get(hash);
        if (data.containsKey(hash)) CompletableFuture.completedFuture(data.get(hash));
        var event = Events.call(new StartLoadingAvatarEvent(hash)); /* Handling an event for case if some of the
        plugins/mods has custom avatar loading implementation, for example loading from DB.
        P.S. Figura (both client and server side) will still check if hash of output data and requested hash matches,
        even if it is loading avatar with default way. If they don't, streams will be closed, and awaiting users will be
        notified about server's attempt to replace avatar. */
        if (event.returned()) return event.returnValue().thenApply(AvatarData::new);
        Path avatarPath = FiguraServer.getInstance().getAvatar(hash);
        var future =  CompletableFuture.supplyAsync(() -> {
            File f = avatarPath.toFile();
            try (FileInputStream fis = new FileInputStream(f)) {
                return fis.readAllBytes();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenApply(AvatarData::new);
        loadingAvatarData.put(hash, future);
        return future;
    }

    private CompletableFuture<AvatarMetadata> startLoadingAvatarMetadata(byte[] hash) {
        if (loadingMetadata.containsKey(hash)) return loadingMetadata.get(hash);
        if (metadata.containsKey(hash)) return CompletableFuture.completedFuture(metadata.get(hash));

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Pair<AvatarData, AvatarMetadata>> loadAvatar(byte[] hash) {
        return startLoadingAvatarData(hash).thenCombineAsync(startLoadingAvatarMetadata(hash), Pair::new);
    }

    public void tick() {
        // Finishing loading for avatars
        ArrayList<byte[]> loadingAvatarsToRemove = new ArrayList<>();
        for (var avatarEntry: loadingAvatarData.entrySet()) {
            var cf = avatarEntry.getValue();
            if (cf.isDone()) {
                var key = avatarEntry.getKey();
                var awaitingStreams = awaiting.get(key);
                if (cf.isCompletedExceptionally()) {
                    Events.call(new AvatarLoadingExceptionEvent( key, cf.exceptionNow(),
                            awaitingStreams.stream().map(Awaiting::receiver).toList()
                    )); // Invoking an event in case if avatar loading failed.
                }
                else if (!cf.isCancelled()) {
                    AvatarData avatarData = cf.join();
                    byte[] dataHash = Utils.getHash(avatarData.data);
                    if (Arrays.equals(dataHash, key)) {
                        this.data.put(key, avatarData);
                    }
                    else Events.call(new InvalidAvatarHashEvent(key, dataHash,
                            awaitingStreams.stream().map(Awaiting::receiver).toList()
                    )); // Invoking an event in case if avatar hash is wrong.

                }
                awaiting.remove(avatarEntry.getKey());
                loadingAvatarsToRemove.add(avatarEntry.getKey());
            }
        }
        for (byte[] key: loadingAvatarsToRemove) {
            loadingAvatarData.remove(key);
        }

        // Ticking avatars data GC counter
        for (AvatarData data : data.values()) {
            data.tick();
        }
        data.entrySet().removeIf(this::canAvatarDataBeRemoved);

        // Ticking streams, and closing finished ones
        for (AvatarOutcomingStream stream: openedOutcomingStreams) {
            stream.tick();
        }
        openedOutcomingStreams.removeIf(AvatarOutcomingStream::canBeClosed);
    }

    private boolean canAvatarDataBeRemoved(Map.Entry<byte[], AvatarData> entry) {
        var cfg = FiguraServer.getInstance().config();
        if (entry.getValue().timeWithoutFetching > cfg.garbageCollectionTicks()) {
            var event = Events.call(new AvatarDataGCEvent(entry.getKey()));
            return !event.isCancelled();
        }
        return false;
    }

    private CompletableFuture<byte[]> getEHashForAvatar(UUID receiver, byte[] hash) {
        return CompletableFuture.completedFuture(new byte[0]);
    }

    public static class AvatarData {
        private int timeWithoutFetching;
        private final byte[] data;

        public AvatarData(byte[] data) {
            this.data = data;
        }

        public byte[] data() {
            timeWithoutFetching = 0;
            return data;
        }

        public void tick() {
            timeWithoutFetching++;
        }
    }
    public static class AvatarMetadata {
        private final HashMap<UUID, byte[]> owners = new HashMap<>();
        private final ArrayList<UUID> equippedOn = new ArrayList<>();
        private boolean cleanupProtection = false;

        /**
         * Map of users who owns this avatar.
         * Avatar will have more than one owner in case if multiple people uploaded the same avatar, so avatar with same hash.
         * @return Map of UUID to EHash
         */
        public HashMap<UUID, byte[]> owners() {
            return owners;
        }

        /**
         * List of users who has this avatar equipped.
         * @return List of UUID
         */
        public ArrayList<UUID> equippedOn() {
            return equippedOn;
        }

        /**
         * If returned value is true, avatar won't be deleted from server cache,
         * even if count of owners and users who equips this avatar will be 0.
         * Otherwise, once avatar is not owned and equipped by anyone, file of this
         * avatar will be deleted from server.
         * @return True if protected from cleanup.
         */
        public boolean cleanupProtection() {
            return cleanupProtection;
        }

        public AvatarMetadata setCleanupProtection(boolean cleanupProtection) {
            this.cleanupProtection = cleanupProtection;
            return this;
        }
    }

    private static class AvatarOutcomingStream {
        private final UUID receiver;
        private final AvatarData source;
        private final int streamId;
        private final byte[] hash;
        private final byte[] ehash;
        private int streamPosition = 0;

        private AvatarOutcomingStream(UUID receiver, AvatarData source, int streamId, byte[] hash, byte[] ehash) {
            this.receiver = receiver;
            this.source = source;
            this.streamId = streamId;
            this.hash = hash;
            this.ehash = ehash;
        }

        public void tick() {
            var inst = FiguraServer.getInstance();
            if (streamPosition == 0) {
                inst.sendPacket(receiver, new S2CInitializeAvatarStreamPacket(streamId, ehash));
            }
            int maxOutcomingChunkSize = inst.config().maxOutcomingChunkSize();
            int chunkSize = maxOutcomingChunkSize > 0 ? Math.min(maxOutcomingChunkSize, AvatarDataPacket.MAX_CHUNK_SIZE)
                    : AvatarDataPacket.MAX_CHUNK_SIZE;
            byte[] data = source.data();
            byte[] chunk = new byte[Math.min(chunkSize, data.length - streamPosition)];
            System.arraycopy(source.data(), streamPosition, chunk, 0, chunk.length);
            streamPosition += chunkSize;
            inst.sendPacket(receiver, new AvatarDataPacket(streamId, canBeClosed(), chunk));
        }

        public boolean canBeClosed() {
            return streamPosition == source.data().length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AvatarOutcomingStream stream)) return false;
            return Objects.equals(receiver, stream.receiver) && Arrays.equals(hash, stream.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiver, Arrays.hashCode(hash));
        }
    }

    private abstract class Awaiting {
        protected final UUID receiver;
        protected final byte[] hash;

        private Awaiting(UUID receiver, byte[] hash) {
            this.receiver = receiver;
            this.hash = hash;
        }

        public UUID receiver() {
            return receiver;
        }

        protected abstract boolean finish();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Awaiting awaiting)) return false;
            return Objects.equals(receiver, awaiting.receiver) && Objects.deepEquals(hash, awaiting.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiver, Arrays.hashCode(hash));
        }
    }

    private class AwaitingOutcomingStream extends Awaiting {
        private final int streamId;

        private AwaitingOutcomingStream(UUID receiver, byte[] avatarHash, int streamId) {
            super(receiver, avatarHash);
            this.streamId = streamId;
        }

        @Override
        protected boolean finish() {
            if (data.containsKey(hash) && metadata.containsKey(hash)) {
                openedOutcomingStreams.add(new AvatarOutcomingStream(receiver, data.get(hash), streamId,
                        hash, getEHashForAvatar(receiver, hash).join()));
                return true;
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AwaitingOutcomingStream stream)) return false;
            if (!super.equals(o)) return false;
            return streamId == stream.streamId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), streamId);
        }
    }
}
