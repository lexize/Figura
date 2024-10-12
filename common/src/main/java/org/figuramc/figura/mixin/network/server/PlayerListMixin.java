package org.figuramc.figura.mixin.network.server;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.figuramc.figura.server.FiguraModServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void onPlayerConnect(Connection connection, ServerPlayer player, CallbackInfo ci) {
        FiguraModServer.getInstance().logInfo("Connection for %s established".formatted(player.getUUID()));
        FiguraModServer.getInstance().userManager().onUserJoin(player.getUUID());
    }
}
