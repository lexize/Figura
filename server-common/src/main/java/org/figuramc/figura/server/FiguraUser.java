package org.figuramc.figura.server;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.CustomFSBPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class FiguraUser {
    private final UUID player;
    private boolean online;
    private final PingCounter pingCounter = new PingCounter();
    private final BitSet prideBadges;
    private Pair<String, EHashPair> equippedAvatar;
    private boolean loadFromFSBIfOffline;

    private final HashMap<String, EHashPair> ownedAvatars;

    public FiguraUser(UUID player, BitSet prideBadges, Pair<String, EHashPair> equippedAvatar, HashMap<String, EHashPair> ownedAvatars, boolean loadFromFSBIfOffline) {
        this.player = player;
        this.online = false;
        this.prideBadges = prideBadges;
        this.equippedAvatar = equippedAvatar;
        this.ownedAvatars = ownedAvatars;
        this.loadFromFSBIfOffline = loadFromFSBIfOffline;
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

    public PingCounter pingCounter() {
        return pingCounter;
    }

    public BitSet prideBadges() {
        return prideBadges;
    }

    public Pair<String, EHashPair> equippedAvatar() {
        return equippedAvatar;
    }

    public HashMap<String, EHashPair> ownedAvatars() {
        return ownedAvatars;
    }

    public void sendPacket(Packet packet) {
        FiguraServer.getInstance().sendPacket(player, packet);
    }

    @Deprecated(since = "0", forRemoval = true)
    public void saveToByteBuf(Path file) {
        file.getParent().toFile().mkdirs();
        File playerFile = file.toFile();
        try {
            FileOutputStream fos = new FileOutputStream(playerFile);
            OutputStreamByteBuf buf = new OutputStreamByteBuf(fos);
            saveToByteBuf(buf);
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated(since = "0", forRemoval = true)
    public void saveToByteBuf(IFriendlyByteBuf buf) {
        byte[] badges = prideBadges.toByteArray();
        buf.writeVarInt(badges.length);
        buf.writeBytes(badges);
        buf.writeByteArray(equippedAvatar.left().getBytes(UTF_8));
        buf.writeBytes(equippedAvatar.right().hash().get());
        buf.writeBytes(equippedAvatar.right().ehash().get());
        buf.writeVarInt(ownedAvatars.size());
        for (var ownedAvatar : ownedAvatars.entrySet()) {
            buf.writeByteArray(ownedAvatar.getKey().getBytes(UTF_8));
            buf.writeBytes(ownedAvatar.getValue().hash().get());
            buf.writeBytes(ownedAvatar.getValue().ehash().get());
        }
    }

    @Deprecated(since = "0", forRemoval = true)
    public static FiguraUser loadFromByteBuf(UUID player, Path playerFile) {
        try (FileInputStream fis = new FileInputStream(playerFile.toFile())) {
            InputStreamByteBuf buf = new InputStreamByteBuf(fis);
            return loadFromByteBuf(player, buf);
        } catch (FileNotFoundException e) {
            return new FiguraUser(player, new BitSet(), null, new HashMap<>(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated(since = "0", forRemoval = true)
    public static FiguraUser loadFromByteBuf(UUID player, IFriendlyByteBuf buf) {
        int length = buf.readVarInt();
        byte[] arr = buf.readBytes(length);
        BitSet prideBadges = BitSet.valueOf(arr);
        String equippedAvatarId = new String(buf.readByteArray(256), UTF_8);
        Hash equippedAvatarHash = buf.readHash();
        Hash equippedAvatarEhash = buf.readHash();
        HashMap<String, EHashPair> ownedAvatars = new HashMap<>();
        int ownedAvatarsCount = buf.readVarInt();
        for (int i = 0; i < ownedAvatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            Hash hash = buf.readHash();
            Hash ehash = buf.readHash();
            ownedAvatars.put(id, new EHashPair(hash, ehash));
        }
        return new FiguraUser(player, prideBadges, new Pair<>(equippedAvatarId, new EHashPair(equippedAvatarHash, equippedAvatarEhash)), ownedAvatars, true);
    }

    public Hash findEHash(Hash hash) {
        if (equippedAvatar.right().hash().equals(hash)) return equippedAvatar.right().ehash();
        for (EHashPair pair: ownedAvatars.values()) {
            if (pair.hash().equals(hash)) return pair.ehash();
        }
        return null;
    }

    public void update() {

    }

    public void setOnline() {
        online = true;
    }

    public void setOffline() {
        online = false;
    }

    public void removeOwnedAvatar(String avatarId) {
        if (ownedAvatars.containsKey(avatarId)) {
            EHashPair avatar = ownedAvatars.remove(avatarId);
            FiguraServer.getInstance().avatarManager().getAvatarMetadata(avatar.hash()).owners().remove(uuid());
        }
    }

    public void removeEquippedAvatar(String avatarId) {
        if (equippedAvatar.left().equals(avatarId)) {
            FiguraServer.getInstance().avatarManager().getAvatarMetadata(equippedAvatar.right().hash()).equipped().remove(uuid());
        }
    }

    public void replaceOrAddOwnedAvatar(String avatarId, Hash hash, Hash ehash) {
        ownedAvatars.put(avatarId, new EHashPair(hash, ehash));
        FiguraServer.getInstance().avatarManager().getAvatarMetadata(hash).owners().put(uuid(), ehash);
    }

    public void replaceOrAddEquippedAvatar(String avatarId, Hash hash, Hash ehash) {
        equippedAvatar = new Pair<>(avatarId, new EHashPair(hash, ehash));
        FiguraServer.getInstance().avatarManager().getAvatarMetadata(hash).equipped().put(uuid(), ehash);
    }

    public int getAvatarsCountWithId(String avatarId) {
        return ownedAvatars().size() + (ownedAvatars().containsKey(avatarId) ? 0 : 1);
    }

    public void sendFSBPacket(String id, byte[] data) {
        sendPacket(new CustomFSBPacket(id.hashCode(), data));
    }

    public boolean loadFromFSBIfOffline() {
        return false;
    }

    public static class PingCounter {
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
