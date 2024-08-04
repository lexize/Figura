package org.figuramc.figura.server.avatars;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.avatars.StartLoadingAvatarEvent;
import org.figuramc.figura.server.events.avatars.StartLoadingMetadataEvent;
import org.figuramc.figura.server.exceptions.HashNotMatchingException;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.s2c.S2CInitializeAvatarStreamPacket;
import org.figuramc.figura.server.utils.Either;
import org.figuramc.figura.server.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.figuramc.figura.server.utils.Utils.copyBytes;

public final class FiguraServerAvatarManager {
    private final FiguraServer parent;
    private final HashMap<byte[], AvatarHandle> avatars = new HashMap<>();

    public FiguraServerAvatarManager(FiguraServer parent) {
        this.parent = parent;
    }

    public void sendAvatar(byte[] hash, UUID receiver, int streamId) {
        avatars.computeIfAbsent(copyBytes(hash), AvatarHandle::new).sendTo(receiver, streamId);
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
        private final HashMap<UUID, byte[]> owners;
        private boolean cleanupProtection = false;

        /**
         * Creates empty metadata
         */
        public AvatarMetadata() {
            this.owners = new HashMap<>();
        }

        /**
         * Creates metadata with avatar owners
         */
        public AvatarMetadata(HashMap<UUID, byte[]> owners) {
            this.owners = owners;
        }

        /**
         * Map of users who owns this avatar.
         * Avatar will have more than one owner in case if multiple people uploaded the same avatar, so avatar with same hash.
         * @return Map of UUID to EHash
         */
        public HashMap<UUID, byte[]> owners() {
            return owners;
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

        public byte[] getEHash(UUID owner) {
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

        private int getChunkSize() {
            var inst = FiguraServer.getInstance();
            var user = inst.userManager().getUser(receiver);
            int serverLimit = inst.config().s2cChunkSize();
            int maxServerLimit = serverLimit <= 0 ? AvatarDataPacket.MAX_CHUNK_SIZE : serverLimit;
            int clientLimit = user.s2cChunkSize();
            int maxClientLimit = clientLimit <= 0 ? AvatarDataPacket.MAX_CHUNK_SIZE : clientLimit;

            return Math.min(maxClientLimit, maxServerLimit);
        }

        public void tick() {
            var inst = FiguraServer.getInstance();
            if (streamPosition == 0) {
                inst.sendPacket(receiver, new S2CInitializeAvatarStreamPacket(streamId, ehash));
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
            return Objects.equals(receiver, stream.receiver) && Arrays.equals(hash, stream.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiver, Arrays.hashCode(hash));
        }
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
                        parent.hash, metadata.a().getEHash(receiver)));
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
        private final byte[] hash;
        private Either<AvatarData, CompletableFuture<AvatarData>> data;
        private Either<AvatarMetadata, CompletableFuture<AvatarMetadata>> metadata;
        private final ArrayList<Awaiting> awaiting = new ArrayList<>();
        private final ArrayList<AvatarOutcomingStream> streams = new ArrayList<>();

        private AvatarHandle(byte[] hash) {
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
            var event = Events.call(new StartLoadingAvatarEvent(copyBytes(hash)));
            if (event.returned()) event.returnValue().thenApplyAsync(this::checkAndFinishLoadingAvatar);

            return CompletableFuture.supplyAsync(() -> {
                var inst = FiguraServer.getInstance();
                Path avatarFile = inst.getAvatar(hash);
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
            byte[] hash = Utils.getHash(data);
            if (!Arrays.equals(hash, this.hash)) throw new HashNotMatchingException(copyBytes(this.hash), hash);
            return new AvatarData(data);
        }

        private Either<AvatarMetadata, CompletableFuture<AvatarMetadata>> getMetadata() {
            if (metadata == null) {
                metadata = Either.newB(startLoadingMetadata());
            }
            return metadata;
        }

        private CompletableFuture<AvatarMetadata> startLoadingMetadata() {
            var event = Events.call(new StartLoadingMetadataEvent(copyBytes(hash)));
            if (event.returned()) return event.returnValue();

            return CompletableFuture.supplyAsync(() -> {
                var inst = FiguraServer.getInstance();
                Path avatarFile = inst.getAvatarMetadata(hash);
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
}
