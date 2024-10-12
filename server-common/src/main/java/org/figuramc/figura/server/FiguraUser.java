package org.figuramc.figura.server;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.InputStreamByteBuf;
import org.figuramc.figura.server.utils.OutputStreamByteBuf;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class FiguraUser {
    private final UUID player;
    private boolean online;
    private boolean pings;
    private boolean avatars;
    private final PingCounter pingCounter = new PingCounter();
    private final BitSet prideBadges;
    private final HashMap<String, EHashPair> equippedAvatars;

    private final HashMap<String, EHashPair> ownedAvatars;

    public FiguraUser(UUID player, boolean online, boolean allowPings, boolean allowAvatars, BitSet prideBadges, HashMap<String, EHashPair> equippedAvatars, HashMap<String, EHashPair> ownedAvatars) {
        this.player = player;
        this.online = online;
        this.pings = allowPings;
        this.avatars = allowAvatars;
        this.prideBadges = prideBadges;
        this.equippedAvatars = equippedAvatars;
        this.ownedAvatars = ownedAvatars;
    }

    public UUID uuid() {
        return player;
    }

    public boolean online() {
        return online;
    }

    public boolean offline() {
        return !online;
    }

    public boolean avatarsAllowed() {
        return avatars;
    }

    public boolean pingsAllowed() {
        return pings;
    }

    public PingCounter pingCounter() {
        return pingCounter;
    }

    public BitSet prideBadges() {
        return prideBadges;
    }

    public HashMap<String, EHashPair> equippedAvatars() {
        return equippedAvatars;
    }

    public HashMap<String, EHashPair> ownedAvatars() {
        return ownedAvatars;
    }

    public void sendPacket(Packet packet) {
        FiguraServer.getInstance().sendPacket(player, packet);
    }

    public void sendDeferredPacket(CompletableFuture<? extends Packet> packet) {
        FiguraServer.getInstance().sendDeferredPacket(player, packet);
    }

    public void save(Path file) {
        file.getParent().toFile().mkdirs();
        File playerFile = file.toFile();
        try {
            FileOutputStream fos = new FileOutputStream(playerFile);
            OutputStreamByteBuf buf = new OutputStreamByteBuf(fos);
            save(buf);
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(IFriendlyByteBuf buf) {
        byte[] badges = prideBadges.toByteArray();
        buf.writeVarInt(badges.length);
        buf.writeBytes(badges);
        buf.writeVarInt(equippedAvatars.size());
        for (var equippedAvatar : equippedAvatars.entrySet()) {
            buf.writeByteArray(equippedAvatar.getKey().getBytes(UTF_8));
            buf.writeBytes(equippedAvatar.getValue().hash().get());
            buf.writeBytes(equippedAvatar.getValue().ehash().get());
        }
        buf.writeVarInt(ownedAvatars.size());
        for (var ownedAvatar : ownedAvatars.entrySet()) {
            buf.writeByteArray(ownedAvatar.getKey().getBytes(UTF_8));
            buf.writeBytes(ownedAvatar.getValue().hash().get());
            buf.writeBytes(ownedAvatar.getValue().ehash().get());
        }
    }

    public static FiguraUser load(UUID player, Path playerFile) {
        try (FileInputStream fis = new FileInputStream(playerFile.toFile())) {
            InputStreamByteBuf buf = new InputStreamByteBuf(fis);
            return load(player, buf);
        } catch (FileNotFoundException e) {
            return new FiguraUser(player, true, false, false, new BitSet(), new HashMap<>(), new HashMap<>());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FiguraUser load(UUID player, IFriendlyByteBuf buf) {
        int length = buf.readVarInt();
        byte[] arr = buf.readBytes(length);
        BitSet prideBadges = BitSet.valueOf(arr);
        int equippedAvatarsCount = buf.readVarInt();
        HashMap<String, EHashPair> equippedAvatars = new HashMap<>();
        for (int i = 0; i < equippedAvatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            Hash hash = buf.readHash();
            Hash ehash = buf.readHash();
            equippedAvatars.put(id, new EHashPair(hash, ehash));
        }
        HashMap<String, EHashPair> ownedAvatars = new HashMap<>();
        int ownedAvatarsCount = buf.readVarInt();
        for (int i = 0; i < ownedAvatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            Hash hash = buf.readHash();
            Hash ehash = buf.readHash();
            ownedAvatars.put(id, new EHashPair(hash, ehash));
        }
        return new FiguraUser(player, false, false, false, prideBadges, equippedAvatars, ownedAvatars);
    }

    public Hash findEHash(Hash hash) {
        for (EHashPair pair: equippedAvatars.values()) {
            if (pair.hash().equals(hash)) return pair.ehash();
        }
        for (EHashPair pair: ownedAvatars.values()) {
            if (pair.hash().equals(hash)) return pair.ehash();
        }
        return null;
    }

    public void update(boolean allowPings, boolean allowAvatars) {
        this.pings = allowPings;
        this.avatars = allowAvatars;
    }

    public void setOnline() {
        online = true;
    }

    public void setOffline() {
        online = false;
    }

    public CompletableFuture<Void> removeOwnedAvatar(String avatarId) {
        if (!ownedAvatars.containsKey(avatarId)) {
            return CompletableFuture.completedFuture(null);
        }
        else {
            EHashPair avatar = ownedAvatars.remove(avatarId);
            return FiguraServer.getInstance().avatarManager().getAvatarMetadata(avatar.hash()).thenAcceptAsync(m -> {
                 m.owners().remove(this.uuid());
            });
        }
    }

    public CompletableFuture<Void> removeEquippedAvatar(String avatarId) {
        if (!equippedAvatars.containsKey(avatarId)) {
            return CompletableFuture.completedFuture(null);
        }
        else {
            EHashPair avatar = equippedAvatars.remove(avatarId);
            return FiguraServer.getInstance().avatarManager().getAvatarMetadata(avatar.hash()).thenAcceptAsync(m -> {
                m.equipped().remove(uuid());
            });
        }
    }

    public CompletableFuture<Void> replaceOrAddOwnedAvatar(String avatarId, Hash hash, Hash ehash) {
        return removeOwnedAvatar(avatarId).thenRunAsync(() -> {
            ownedAvatars.put(avatarId, new EHashPair(hash, ehash));
            FiguraServer.getInstance().avatarManager().getAvatarMetadata(hash).join().owners().put(uuid(), ehash);
        });
    }

    public CompletableFuture<Void> replaceOrAddEquippedAvatar(String avatarId, Hash hash, Hash ehash) {
        return removeEquippedAvatar(avatarId).thenRunAsync(() -> {
            equippedAvatars.put(avatarId, new EHashPair(hash, ehash));
            FiguraServer.getInstance().avatarManager().getAvatarMetadata(hash).join().equipped().put(uuid(), ehash);
        });
    }

    private static class PingCounter {
        private int bytesSent; // Amount of total bytes sent in last 20 ticks
        private int pingsSent; // Amount of pings sent in last 20 ticks

        public int bytesSent() {
            return bytesSent;
        }

        public int pingsSent() {
            return pingsSent;
        }

        public void addPing(int size) {
            pingsSent++;
            bytesSent += size;
        }

        public void reset() {
            bytesSent = 0;
            pingsSent = 0;
        }
    }
}
