package org.figuramc.figura.server;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.InputStreamByteBuf;
import org.figuramc.figura.server.utils.OutputStreamByteBuf;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class FiguraUser {
    private final UUID player;
    private boolean online;
    private boolean pings;
    private boolean avatars;
    private int s2cChunkSize;
    private final PingCounter pingCounter = new PingCounter();
    private final BitSet prideBadges;
    private final HashMap<String, byte[]> equippedAvatars;

    private final HashMap<String, byte[]> ownedAvatars;

    public FiguraUser(UUID player, boolean online, boolean allowPings, boolean allowAvatars, int s2cChunkSize, BitSet prideBadges, HashMap<String, byte[]> equippedAvatars, HashMap<String, byte[]> ownedAvatars) {
        this.player = player;
        this.online = online;
        this.pings = allowPings;
        this.avatars = allowAvatars;
        this.s2cChunkSize = s2cChunkSize;
        this.prideBadges = prideBadges;
        this.equippedAvatars = equippedAvatars;
        this.ownedAvatars = ownedAvatars;
    }

    public UUID player() {
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

    public int s2cChunkSize() {
        return s2cChunkSize;
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

    public HashMap<String, byte[]> equippedAvatars() {
        return equippedAvatars;
    }

    public HashMap<String, byte[]> ownedAvatars() {
        return ownedAvatars;
    }

    public void sendPacket(Packet packet) {
        FiguraServer.getInstance().sendPacket(player, packet);
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
        buf.writeBytes(prideBadges.toByteArray());
        buf.writeVarInt(equippedAvatars.size());
        for (var equippedAvatar : equippedAvatars.entrySet()) {
            buf.writeByteArray(equippedAvatar.getKey().getBytes(UTF_8));
            buf.writeBytes(equippedAvatar.getValue());
        }
        buf.writeVarInt(ownedAvatars.size());
        for (var ownedAvatar : ownedAvatars.entrySet()) {
            buf.writeByteArray(ownedAvatar.getKey().getBytes(UTF_8));
            buf.writeBytes(ownedAvatar.getValue());
        }
    }

    public static FiguraUser load(UUID player, Path playerFile) {
        try (FileInputStream fis = new FileInputStream(playerFile.toFile())) {
            InputStreamByteBuf buf = new InputStreamByteBuf(fis);
            return load(player, buf);
        } catch (FileNotFoundException e) {
            return new FiguraUser(player, true, false, false, 0, new BitSet(), new HashMap<>(), new HashMap<>());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FiguraUser load(UUID player, IFriendlyByteBuf buf) {
        BitSet prideBadges = BitSet.valueOf(buf.readByteArray(256));
        int equippedAvatarsCount = buf.readVarInt();
        HashMap<String, byte[]> equippedAvatars = new HashMap<>();
        for (int i = 0; i < equippedAvatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            byte[] hash = buf.readHash();
            equippedAvatars.put(id, hash);
        }
        HashMap<String, byte[]> ownedAvatars = new HashMap<>();
        int ownedAvatarsCount = buf.readVarInt();
        for (int i = 0; i < ownedAvatarsCount; i++) {
            String id = new String(buf.readByteArray(256), UTF_8);
            byte[] hash = buf.readHash();
            ownedAvatars.put(id, hash);
        }
        return new FiguraUser(player, false, false, false, 0, prideBadges, equippedAvatars, ownedAvatars);
    }

    public byte[] findEHash(byte[] hash) {
        return null;
    }

    public void update(boolean allowPings, boolean allowAvatars, int s2cChunkSize) {
        this.pings = allowPings;
        this.avatars = allowAvatars;
        this.s2cChunkSize = s2cChunkSize;
    }

    public void setOnline() {
        online = true;
    }

    public void setOffline() {
        online = false;
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
