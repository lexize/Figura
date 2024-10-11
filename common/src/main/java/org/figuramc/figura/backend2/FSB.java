package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.C2SDeleteAvatarPacket;
import org.figuramc.figura.server.packets.c2s.C2SEquipAvatarsPacket;
import org.figuramc.figura.server.packets.c2s.C2SFetchUserdataPacket;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class FSB {
    private static S2CHandshake s2CHandshake;
    private static final HashMap<UUID, UserData> awaitingUserdata = new HashMap<>();

    public static boolean connected() {
        return s2CHandshake != null;
    }

    public static boolean avatars() {
        return connected() && s2CHandshake.allowAvatars();
    }

    public static void handleHandshake(S2CBackendHandshakePacket packet) {
        s2CHandshake =
                new S2CHandshake(packet.avatars(), packet.pings(), packet.maxAvatarSize(), packet.maxAvatarsCount(), packet.pingsRateLimit(), packet.pingsSizeLimit());
        // TODO Make this function work when Paladin will make stuff
    }

    public static void getUser(UserData userData) {
        // TODO
        sendPacket(new C2SFetchUserdataPacket(userData.id));
        awaitingUserdata.put(userData.id, userData);
    }

    public static void uploadAvatar(String avatarId, byte[] avatarData) {
        // TODO Avatar streaming
    }

    public static void deleteAvatar(String avatarId) {
        sendPacket(new C2SDeleteAvatarPacket(avatarId));
    }

    public static void equipAvatar(List<Hash> avatars) {
        ArrayList<EHashPair> eHashPairs = new ArrayList<>();
        for (Hash hash: avatars) {
            eHashPairs.add(new EHashPair(hash, getEHash(hash)));
        }
        sendPacket(new C2SEquipAvatarsPacket(eHashPairs));
    }

    public static void onDisconnect() {
        s2CHandshake = null;
        // TODO: Handling disconnecting properly, closing all incoming/outcoming streams, etc.
    }

    public static void getAvatar(UserData target, String hash) {
        // TODO
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
        return null; // TODO Make this function work when Paladin will make stuff
    }
}
