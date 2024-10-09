package org.figuramc.figura.server.avatars;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.avatars.*;
import org.figuramc.figura.server.exceptions.HashNotMatchingException;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.s2c.S2CInitializeAvatarStreamPacket;
import org.figuramc.figura.server.packets.s2c.S2COwnedAvatarsPacket;
import org.figuramc.figura.server.utils.Either;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class FiguraServerAvatarManager {
    private final FiguraServer parent;
    private final IncomingAvatarHandler incomingAvatarHandler = new IncomingAvatarHandler();
    private final HashMap<Hash, AvatarHandle> avatars = new HashMap<>();

    public FiguraServerAvatarManager(FiguraServer parent) {
        this.parent = parent;
    }

    public void sendAvatar(Hash hash, UUID receiver, int streamId) {
        getAvatarHandle(hash).sendTo(receiver, streamId);
    }

    private AvatarHandle getAvatarHandle(Hash hash) {
        return avatars.computeIfAbsent(hash, AvatarHandle::new);
    }

    public synchronized void receiveAvatar(FiguraUser uploader, String avatarId, int streamId, Hash avatarHash, Hash avatarEHash) {
        incomingAvatarHandler.openStream(uploader.uuid(), avatarId, streamId, avatarHash, avatarEHash);
    }

    public void acceptAvatarChunk(FiguraUser uploader, int streamId, byte[] data, boolean finalChunk) {
        incomingAvatarHandler.acceptChunk(uploader.uuid(), streamId, data, finalChunk);
    }

    public CompletableFuture<Boolean> avatarExists(Hash hash) {
        // TODO
        return null;
    }

    public CompletableFuture<AvatarMetadata> getAvatarMetadata(Hash hash) {
        var either = getAvatarHandle(hash).getMetadata();
        if (either.isA()) return CompletableFuture.completedFuture(either.a());
        return either.b();
    }

    public void tick() {
        avatars.values().forEach(AvatarHandle::tick);
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
        private final HashMap<UUID, Hash> owners;
        private final HashMap<UUID, Hash> equipped;
        private boolean cleanupProtection = false;

        /**
         * Creates empty metadata
         */
        public AvatarMetadata() {
            this.owners = new HashMap<>();
            this.equipped = new HashMap<>();
        }

        /**
         * Creates metadata with avatar owners
         */
        public AvatarMetadata(HashMap<UUID, Hash> owners, HashMap<UUID, Hash> equipped) {
            this.owners = owners;
            this.equipped = equipped;
        }

        /**
         * Map of users who owns this avatar.
         * Avatar will have more than one owner in case if multiple people uploaded the same avatar, so avatar with same hash.
         * @return Map of UUID to EHash
         */
        public HashMap<UUID, Hash> owners() {
            return owners;
        }

        /**
         * Map of users who has this avatar equipped.
         * Avatar can have more than one user equipping it.
         * @return Map of UUID to EHash
         */
        public HashMap<UUID, Hash> equipped() {
            return equipped;
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

        public Hash getOwnerEHash(UUID owner) {
            return owners.get(owner);
        }

        public Hash getEquippedEHash(UUID owner) {
            return owners.get(owner);
        }

        private static AvatarMetadata fromJsonBytes(byte[] bytes) {
            return null; // TODO
        }
    }

    private static class AvatarOutcomingStream {
        private final UUID receiver;
        private final AvatarData source;
        private final int streamId;
        private final Hash hash;
        private final Hash ehash;
        private int streamPosition = 0;

        private AvatarOutcomingStream(UUID receiver, AvatarData source, int streamId, Hash hash, Hash ehash) {
            this.receiver = receiver;
            this.source = source;
            this.streamId = streamId;
            this.hash = hash;
            this.ehash = ehash;
        }

        private int getChunkSize() {
            var inst = FiguraServer.getInstance();
            var user = inst.userManager().getUser(receiver).join();
            int serverLimit = inst.config().s2cChunkSize();
            int maxServerLimit = serverLimit <= 0 ? AvatarDataPacket.MAX_CHUNK_SIZE : serverLimit;
            int clientLimit = user.s2cChunkSize();
            int maxClientLimit = clientLimit <= 0 ? AvatarDataPacket.MAX_CHUNK_SIZE : clientLimit;

            return Math.min(maxClientLimit, maxServerLimit);
        }

        public void tick() {
            var inst = FiguraServer.getInstance();
            if (streamPosition == 0) {
                inst.sendPacket(receiver, new S2CInitializeAvatarStreamPacket(streamId, hash, ehash));
            }
            int chunkSize = getChunkSize();
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
            return Objects.equals(receiver, stream.receiver) && hash.equals(stream.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiver, hash);
        }
    }

    private void writeAvatar(Hash avatarHash, byte[] avatarData) {
        var file = parent.getAvatar(avatarHash.get()).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(avatarData);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeMetadata(Hash avatarHash, AvatarMetadata metadata) {
        // TODO avatar metadata saving
    }

    private abstract class Awaiting {
        protected final UUID receiver;
        protected final AvatarHandle parent;

        private Awaiting(UUID receiver, AvatarHandle parent) {
            this.receiver = receiver;
            this.parent = parent;
        }

        public UUID receiver() {
            return receiver;
        }

        protected abstract boolean finish();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Awaiting awaiting)) return false;
            return Objects.equals(receiver, awaiting.receiver) && Objects.equals(parent, awaiting.parent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiver, parent);
        }

        protected abstract boolean acceptDataException(Exception error);
        protected abstract boolean acceptMetadataException(Exception error);
    }

    private class AwaitingOutcomingStream extends Awaiting {
        private final int streamId;

        private AwaitingOutcomingStream(UUID receiver, AvatarHandle parent, int streamId) {
            super(receiver, parent);
            this.streamId = streamId;
        }

        @Override
        protected boolean finish() {
            var data = parent.getAvatarData();
            var metadata = parent.getMetadata();
            if (data.isA() && metadata.isA()) {
                parent.streams.add(new AvatarOutcomingStream(receiver, data.a(), streamId,
                        parent.hash, metadata.a().getOwnerEHash(receiver)));
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

        @Override
        protected boolean acceptDataException(Exception error) {
            // Implement notifying user about an error in loading an avatar
            return true;
        }

        @Override
        protected boolean acceptMetadataException(Exception error) {
            // Implement notifying user about an error in loading avatar metadata
            return true;
        }
    }

    private class AvatarHandle {
        private final Hash hash;
        private Either<AvatarData, CompletableFuture<AvatarData>> data;
        private Either<AvatarMetadata, CompletableFuture<AvatarMetadata>> metadata;
        private final ArrayList<Awaiting> awaiting = new ArrayList<>();
        private final ArrayList<AvatarOutcomingStream> streams = new ArrayList<>();

        private AvatarHandle(Hash hash) {
            this.hash = hash;
        }

        private void sendTo(UUID receiver, int streamId) {
            awaiting.add(new AwaitingOutcomingStream(receiver, this, streamId));
        }

        private Either<AvatarData, CompletableFuture<AvatarData>> getAvatarData() {
            if (data == null) {
                data = Either.newB(startLoadingAvatar());
            }
            return data;
        }

        private CompletableFuture<AvatarData> startLoadingAvatar() {
            var event = Events.call(new StartLoadingAvatarEvent(hash));
            if (event.returned()) event.returnValue().thenApplyAsync(this::checkAndFinishLoadingAvatar);

            return CompletableFuture.supplyAsync(() -> {
                var inst = FiguraServer.getInstance();
                Path avatarFile = inst.getAvatar(hash.get());
                try {
                    FileInputStream fis = new FileInputStream(avatarFile.toFile());
                    byte[] data = fis.readAllBytes();
                    fis.close();
                    return data;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(this::checkAndFinishLoadingAvatar);
        }

        private AvatarData checkAndFinishLoadingAvatar(byte[] data) {
            Hash hash = Utils.getHash(data);
            if (!hash.equals(this.hash)) throw new HashNotMatchingException(this.hash, hash);
            return new AvatarData(data);
        }

        private Either<AvatarMetadata, CompletableFuture<AvatarMetadata>> getMetadata() {
            if (metadata == null) {
                metadata = Either.newB(startLoadingMetadata());
            }
            return metadata;
        }

        private CompletableFuture<AvatarMetadata> startLoadingMetadata() {
            var event = Events.call(new StartLoadingMetadataEvent(hash));
            if (event.returned()) return event.returnValue();

            return CompletableFuture.supplyAsync(() -> {
                var inst = FiguraServer.getInstance();
                Path avatarFile = inst.getAvatarMetadata(hash.get());
                try {
                    FileInputStream fis = new FileInputStream(avatarFile.toFile());
                    byte[] data = fis.readAllBytes();
                    fis.close();
                    return data;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(AvatarMetadata::fromJsonBytes);
        }

        private void tick() {
            var data = getAvatarData();
            var metadata = getMetadata();

            if (data.isB() && data.b().isDone()) {
                try {
                    this.data = Either.newA(data.b().join());
                } catch (Exception e) {
                    awaiting.removeIf(a -> a.acceptDataException(e));
                }
            }

            if (metadata.isB() && metadata.b().isDone()) {
                try {
                    this.metadata = Either.newA(metadata.b().join());
                } catch (Exception e) {
                    awaiting.removeIf(a -> a.acceptMetadataException(e));
                }
            }

            awaiting.removeIf(Awaiting::finish);
            data.a().tick();
            streams.forEach(AvatarOutcomingStream::tick);
        }
    }

    private class IncomingAvatarHandler {
        private final HashMap<Hash, ArrayList<IncomingAvatarKey>> hashesToUploads = new HashMap<>();
        private final HashMap<IncomingAvatarKey, AvatarIncomingStream> streams = new HashMap<>();

        private void openStream(UUID uploader, String avatarId, int streamId, Hash avatarHash, Hash avatarEHash) {
            var key = new IncomingAvatarKey(uploader, streamId);
            streams.put(new IncomingAvatarKey(uploader, streamId),
                    new AvatarIncomingStream(uploader, streamId, avatarId, avatarHash, avatarEHash)
            );
            hashesToUploads.computeIfAbsent(avatarHash, h -> new ArrayList<>()).add(key);
        }

        public void acceptChunk(UUID uuid, int streamId, byte[] data, boolean finalChunk) {
            var key = new IncomingAvatarKey(uuid, streamId);
            if (!streams.containsKey(key)) {
                parent.sendPacket(uuid, new CloseIncomingStreamPacket(streamId));
                return;
            }

            var s = streams.get(key);
            if (s.acceptDataChunk(data, finalChunk)) {
                streams.entrySet().removeIf(e -> e.getValue().isFinished());
            }
        }

        private class AvatarIncomingStream {
            private final LinkedList<byte[]> dataChunks = new LinkedList<>();
            private final UUID uploader;
            private final int streamId;
            private final String avatarId;
            private final Hash hash;
            private final Hash ehash;
            private int size = 0;
            private boolean finished;

            private AvatarIncomingStream(UUID uploader, int streamId, String avatarId, Hash hash, Hash ehash) {
                this.uploader = uploader;
                this.streamId = streamId;
                this.avatarId = avatarId;
                this.hash = hash;
                this.ehash = ehash;
            }

            // If this function returns true, it closes the stream and removes it from IncomingAvatarHandler
            private boolean acceptDataChunk(byte[] chunk, boolean finalChunk) {
                size += chunk.length;
                // In case if avatar size is exceeded - closing the stream and removing it from handler.
                if (size > parent.config().avatarSizeLimit() &&
                    !Events.call(new AvatarUploadSizeExceedEvent(uploader, size)).isCancelled()) {
                    close();
                    return true;
                }
                dataChunks.add(chunk);

                if (finalChunk) {
                    // Collecting all data chunks in one array
                    int offset = 0;
                    byte[] avatarData = new byte[size];
                    for (byte[] dataChunk: dataChunks) {
                        System.arraycopy(dataChunk, 0, avatarData, offset, dataChunk.length);
                    }
                    Hash dataHash = Utils.getHash(avatarData);
                    // In case if resulting data hash doesn't match - rejecting it.
                    // Closing this stream is not required as client should've done it by itself
                    if (!dataHash.equals(hash)) {
                        Events.call(new InvalidIncomingAvatarHashEvent(hash, dataHash));
                        return true;
                    }

                    // Writing avatar data
                    if (!Events.call(new StoreAvatarDataEvent(avatarData, hash)).isCancelled()) {
                        writeAvatar(hash, avatarData);
                    }

                    // Creating a new avatar handle
                    var avatarHandle = getAvatarHandle(hash);
                    avatarHandle.data = Either.newA(new AvatarData(avatarData));

                    // Creating empty metadata for this avatar with all the avatar owners
                    AvatarMetadata metadata = new AvatarMetadata();
                    for (IncomingAvatarKey key: hashesToUploads.get(hash)) {
                        var stream = streams.get(key);
                        metadata.owners.put(stream.uploader, stream.ehash);
                    }

                    // Writing avatar metadata
                    if (!Events.call(new StoreAvatarMetadataEvent(hash, metadata)).isCancelled()) {
                        writeMetadata(hash, metadata);
                    }

                    avatarHandle.metadata = Either.newA(metadata);

                    // Finishing work of all streams
                    var selfKey = new IncomingAvatarKey(uploader, streamId);
                    for (IncomingAvatarKey key: hashesToUploads.get(hash)) {
                        var stream = streams.get(key);
                        stream.finish(selfKey.equals(key));
                    }

                    // Removing keys from hash map
                    hashesToUploads.remove(hash);

                    return true;
                }

                return false;
            }

            private void close() {
                parent.sendPacket(uploader, new CloseIncomingStreamPacket(streamId));
            }

            private void finish(boolean uploadingFinisher) {
                // Immediately closing our stream if the user is not the one who finished uploading earlier
                if (!uploadingFinisher) close();
                finished = true;
                CompletableFuture<? extends Packet> future = parent.userManager().getUser(uploader)
                        .thenApplyAsync( u -> {
                            u.replaceOrAddOwnedAvatar(avatarId, hash, ehash);
                            return new S2COwnedAvatarsPacket(u.ownedAvatars());
                        });
                parent.sendDeferredPacket(uploader, future);
            }

            private boolean isFinished() {
                return finished;
            }
        }

    }

    private record IncomingAvatarKey(UUID uploader, int streamId) {}
}
