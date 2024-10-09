package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class C2SAvatarDataPacket extends AuthorizedC2SPacketHandler<AvatarDataPacket>{
    protected C2SAvatarDataPacket(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, AvatarDataPacket packet) {
        // TODO
        //parent.avatarManager().acceptAvatarDataChunk(sender.uuid(), packet.streamId(), packet.avatarData(), packet.finalChunk());
    }

    @Override
    public AvatarDataPacket serialize(IFriendlyByteBuf byteBuf) {
        return new AvatarDataPacket(byteBuf);
    }
}
